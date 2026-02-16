package repository

import (
	"context"
	"time"

	"github.com/whatsapp-clone/backend/media-service/internal/model"
)

// MediaRepository defines persistence operations for media metadata.
type MediaRepository interface {
	Insert(ctx context.Context, media *model.Media) error
	GetByID(ctx context.Context, mediaID string) (*model.Media, error)
	Delete(ctx context.Context, mediaID string) error
	FindOrphaned(ctx context.Context, olderThan time.Time) ([]*model.Media, error)
}
