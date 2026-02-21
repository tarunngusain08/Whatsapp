package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
)

type RefreshTokenRepository interface {
	Create(ctx context.Context, token *model.RefreshToken) error
	GetByTokenHash(ctx context.Context, tokenHash string) (*model.RefreshToken, error)
	RevokeByID(ctx context.Context, id string) error
	RevokeAllByUserID(ctx context.Context, userID string) error
	// ReplaceToken atomically revokes the old token and creates a new one
	// within a single database transaction.
	ReplaceToken(ctx context.Context, oldID string, newToken *model.RefreshToken) error
}
