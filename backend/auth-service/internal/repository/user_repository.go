package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
)

type UserRepository interface {
	UpsertByPhone(ctx context.Context, phone string) (*model.User, error)
	GetByID(ctx context.Context, id string) (*model.User, error)
}
