package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type UserRepository interface {
	GetByID(ctx context.Context, id string) (*model.User, error)
	GetByIDs(ctx context.Context, ids []string) ([]*model.User, error)
	GetByPhone(ctx context.Context, phone string) (*model.User, error)
	GetByPhones(ctx context.Context, phones []string) ([]*model.ContactSyncResult, error)
	Update(ctx context.Context, id string, req *model.UpdateProfileRequest) (*model.User, error)
}
