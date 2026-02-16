package repository

import (
	"context"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
)

type OTPRepository interface {
	Store(ctx context.Context, phone string, entry *model.OTPEntry) error
	Get(ctx context.Context, phone string) (*model.OTPEntry, error)
	IncrementAttempts(ctx context.Context, phone string) (int, error)
	Delete(ctx context.Context, phone string) error
}
