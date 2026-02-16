package jwt

import (
	"errors"
	"time"

	jwtgo "github.com/golang-jwt/jwt/v5"
)

var (
	ErrTokenExpired  = errors.New("token has expired")
	ErrTokenInvalid  = errors.New("token is invalid")
	ErrTokenMalformed = errors.New("token is malformed")
)

type Claims struct {
	jwtgo.RegisteredClaims
	UserID string `json:"user_id"`
	Phone  string `json:"phone"`
}

type Manager struct {
	signingKey []byte
	accessTTL  time.Duration
}

func NewManager(signingKey string, accessTTL time.Duration) *Manager {
	return &Manager{
		signingKey: []byte(signingKey),
		accessTTL:  accessTTL,
	}
}

func (m *Manager) CreateAccessToken(userID, phone string) (string, error) {
	now := time.Now()
	claims := Claims{
		RegisteredClaims: jwtgo.RegisteredClaims{
			ExpiresAt: jwtgo.NewNumericDate(now.Add(m.accessTTL)),
			IssuedAt:  jwtgo.NewNumericDate(now),
			Issuer:    "whatsapp-auth",
		},
		UserID: userID,
		Phone:  phone,
	}
	token := jwtgo.NewWithClaims(jwtgo.SigningMethodHS256, claims)
	return token.SignedString(m.signingKey)
}

func (m *Manager) ValidateToken(tokenStr string) (*Claims, error) {
	token, err := jwtgo.ParseWithClaims(tokenStr, &Claims{}, func(t *jwtgo.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwtgo.SigningMethodHMAC); !ok {
			return nil, ErrTokenInvalid
		}
		return m.signingKey, nil
	})
	if err != nil {
		if errors.Is(err, jwtgo.ErrTokenExpired) {
			return nil, ErrTokenExpired
		}
		return nil, ErrTokenInvalid
	}
	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return nil, ErrTokenInvalid
	}
	return claims, nil
}
