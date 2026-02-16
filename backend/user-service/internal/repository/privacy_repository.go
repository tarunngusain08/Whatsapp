package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type PrivacyRepository interface {
	Get(ctx context.Context, userID string) (*model.PrivacySettings, error)
	Upsert(ctx context.Context, settings *model.PrivacySettings) error
}
