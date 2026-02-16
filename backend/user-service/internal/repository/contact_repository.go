package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type ContactRepository interface {
	Upsert(ctx context.Context, contact *model.Contact) error
	GetByUserID(ctx context.Context, userID string) ([]*model.Contact, error)
	IsContact(ctx context.Context, userID, contactID string) (bool, error)
	Block(ctx context.Context, userID, contactID string) error
	Unblock(ctx context.Context, userID, contactID string) error
	IsBlocked(ctx context.Context, userID, contactID string) (bool, error)
	Delete(ctx context.Context, userID, contactID string) error
}
