package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type StatusRepository interface {
	Create(ctx context.Context, status *model.Status) error
	GetByUserID(ctx context.Context, userID string) ([]*model.Status, error)
	GetByUserIDs(ctx context.Context, userIDs []string) ([]*model.Status, error)
	Delete(ctx context.Context, id, userID string) error
	AddViewer(ctx context.Context, statusID, viewerID string) error
}
