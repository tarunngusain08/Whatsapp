package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type DeviceTokenRepository interface {
	Upsert(ctx context.Context, token *model.DeviceToken) error
	GetByUserID(ctx context.Context, userID string) ([]*model.DeviceToken, error)
	DeleteByToken(ctx context.Context, token string) error
	DeleteByTokenAndUser(ctx context.Context, userID, token string) error
	DeleteByUserID(ctx context.Context, userID string) error
}
