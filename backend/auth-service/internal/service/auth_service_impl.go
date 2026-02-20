package service

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"math/big"
	"time"

	"github.com/rs/zerolog"
	"golang.org/x/crypto/bcrypt"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	"github.com/whatsapp-clone/backend/auth-service/internal/repository"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/jwt"
)

type authServiceImpl struct {
	userRepo     repository.UserRepository
	otpRepo      repository.OTPRepository
	refreshRepo  repository.RefreshTokenRepository
	jwtManager   *jwt.Manager
	refreshTTL   time.Duration
	otpLength    int
	maxAttempts  int
	log          zerolog.Logger
}

func NewAuthService(
	userRepo repository.UserRepository,
	otpRepo repository.OTPRepository,
	refreshRepo repository.RefreshTokenRepository,
	jwtManager *jwt.Manager,
	refreshTTL time.Duration,
	otpLength int,
	maxAttempts int,
	log zerolog.Logger,
) AuthService {
	return &authServiceImpl{
		userRepo:    userRepo,
		otpRepo:     otpRepo,
		refreshRepo: refreshRepo,
		jwtManager:  jwtManager,
		refreshTTL:  refreshTTL,
		otpLength:   otpLength,
		maxAttempts: maxAttempts,
		log:         log,
	}
}

func (s *authServiceImpl) SendOTP(ctx context.Context, phone string) (string, error) {
	otp, err := generateOTP(s.otpLength)
	if err != nil {
		return "", apperr.NewInternal("failed to generate OTP", err)
	}

	hashed, err := bcrypt.GenerateFromPassword([]byte(otp), bcrypt.DefaultCost)
	if err != nil {
		return "", apperr.NewInternal("failed to hash OTP", err)
	}

	entry := &model.OTPEntry{
		HashedOTP: string(hashed),
		Attempts:  0,
	}

	if err := s.otpRepo.Store(ctx, phone, entry); err != nil {
		return "", err
	}

	s.log.Info().Str("phone", maskPhone(phone)).Msg("OTP generated and stored")

	return otp, nil
}

func (s *authServiceImpl) VerifyOTP(ctx context.Context, phone, code string) (*model.AuthResult, error) {
	entry, err := s.otpRepo.Get(ctx, phone)
	if err != nil {
		return nil, err
	}
	if entry == nil {
		return nil, apperr.Wrap(apperr.CodeOTPExpired, 400, "OTP expired or not found", nil)
	}

	if entry.Attempts >= s.maxAttempts {
		_ = s.otpRepo.Delete(ctx, phone)
		return nil, apperr.Wrap(apperr.CodeOTPMaxAttempts, 429, "too many OTP attempts", nil)
	}

	if err := bcrypt.CompareHashAndPassword([]byte(entry.HashedOTP), []byte(code)); err != nil {
		_, _ = s.otpRepo.IncrementAttempts(ctx, phone)
		return nil, apperr.Wrap(apperr.CodeOTPInvalid, 400, "invalid OTP code", nil)
	}

	_ = s.otpRepo.Delete(ctx, phone)

	user, err := s.userRepo.UpsertByPhone(ctx, phone)
	if err != nil {
		return nil, err
	}

	// Detect new user: if created_at == updated_at (within 1 second), user was just created
	isNewUser := user.UpdatedAt.Sub(user.CreatedAt) < time.Second

	pair, err := s.issueTokenPair(ctx, user.ID, user.Phone)
	if err != nil {
		return nil, err
	}

	s.log.Info().Str("user_id", user.ID).Str("phone", maskPhone(phone)).Bool("is_new_user", isNewUser).Msg("OTP verified, tokens issued")

	return &model.AuthResult{
		AccessToken:      pair.AccessToken,
		RefreshToken:     pair.RefreshToken,
		ExpiresIn:        pair.ExpiresIn,
		ExpiresInSeconds: pair.ExpiresIn,
		User:             user,
		IsNewUser:        isNewUser,
	}, nil
}

func (s *authServiceImpl) RefreshTokens(ctx context.Context, refreshToken string) (*model.TokenPair, error) {
	tokenHash := sha256Hash(refreshToken)

	stored, err := s.refreshRepo.GetByTokenHash(ctx, tokenHash)
	if err != nil {
		return nil, err
	}
	if stored == nil {
		return nil, apperr.Wrap(apperr.CodeTokenInvalid, 401, "invalid or expired refresh token", nil)
	}

	if err := s.refreshRepo.RevokeByID(ctx, stored.ID); err != nil {
		return nil, err
	}

	user, err := s.userRepo.GetByID(ctx, stored.UserID)
	if err != nil {
		return nil, err
	}

	pair, err := s.issueTokenPair(ctx, user.ID, user.Phone)
	if err != nil {
		return nil, err
	}

	s.log.Info().Str("user_id", user.ID).Msg("tokens refreshed")

	return pair, nil
}

func (s *authServiceImpl) Logout(ctx context.Context, refreshToken string) error {
	tokenHash := sha256Hash(refreshToken)

	stored, err := s.refreshRepo.GetByTokenHash(ctx, tokenHash)
	if err != nil {
		return err
	}
	if stored == nil {
		return nil
	}

	if err := s.refreshRepo.RevokeByID(ctx, stored.ID); err != nil {
		return err
	}

	s.log.Info().Str("user_id", stored.UserID).Msg("user logged out")

	return nil
}

func (s *authServiceImpl) ValidateToken(ctx context.Context, token string) (string, string, error) {
	claims, err := s.jwtManager.ValidateToken(token)
	if err != nil {
		return "", "", apperr.Wrap(apperr.CodeTokenInvalid, 401, "invalid token", err)
	}
	return claims.UserID, claims.Phone, nil
}

func (s *authServiceImpl) issueTokenPair(ctx context.Context, userID, phone string) (*model.TokenPair, error) {
	accessToken, err := s.jwtManager.CreateAccessToken(userID, phone)
	if err != nil {
		return nil, apperr.NewInternal("failed to create access token", err)
	}

	opaqueToken, err := generateOpaqueToken(32)
	if err != nil {
		return nil, apperr.NewInternal("failed to generate refresh token", err)
	}

	refreshTokenHash := sha256Hash(opaqueToken)
	rt := &model.RefreshToken{
		UserID:    userID,
		TokenHash: refreshTokenHash,
		ExpiresAt: time.Now().Add(s.refreshTTL),
	}

	if err := s.refreshRepo.Create(ctx, rt); err != nil {
		return nil, err
	}

	return &model.TokenPair{
		AccessToken:  accessToken,
		RefreshToken: opaqueToken,
		ExpiresIn:    int64(s.refreshTTL.Seconds()),
	}, nil
}

func generateOTP(length int) (string, error) {
	max := new(big.Int)
	max.SetString(fmt.Sprintf("%s", pow10(length)), 10)

	n, err := rand.Int(rand.Reader, max)
	if err != nil {
		return "", err
	}

	format := fmt.Sprintf("%%0%dd", length)
	return fmt.Sprintf(format, n), nil
}

func pow10(n int) string {
	result := "1"
	for i := 0; i < n; i++ {
		result += "0"
	}
	return result
}

func generateOpaqueToken(byteLen int) (string, error) {
	b := make([]byte, byteLen)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b), nil
}

func sha256Hash(data string) string {
	h := sha256.Sum256([]byte(data))
	return hex.EncodeToString(h[:])
}

func maskPhone(phone string) string {
	if len(phone) <= 4 {
		return "****"
	}
	return phone[:4] + "****"
}
