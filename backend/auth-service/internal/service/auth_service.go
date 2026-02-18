package service

import (
	"context"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
)

type AuthService interface {
	SendOTP(ctx context.Context, phone string) (otp string, err error)
	VerifyOTP(ctx context.Context, phone, code string) (*model.AuthResult, error)
	RefreshTokens(ctx context.Context, refreshToken string) (*model.TokenPair, error)
	Logout(ctx context.Context, refreshToken string) error
	ValidateToken(ctx context.Context, token string) (userID string, phone string, err error)
}
