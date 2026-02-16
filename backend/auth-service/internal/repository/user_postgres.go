package repository

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

type postgresUserRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresUserRepository(pool *pgxpool.Pool) UserRepository {
	return &postgresUserRepository{pool: pool}
}

func (r *postgresUserRepository) UpsertByPhone(ctx context.Context, phone string) (*model.User, error) {
	var user model.User
	err := r.pool.QueryRow(ctx, `
		INSERT INTO users (phone) VALUES ($1)
		ON CONFLICT (phone) DO UPDATE SET updated_at = NOW()
		RETURNING id, phone, display_name, avatar_url, status_text, created_at, updated_at
	`, phone).Scan(
		&user.ID, &user.Phone, &user.DisplayName, &user.AvatarURL,
		&user.StatusText, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		return nil, apperr.NewInternal("failed to upsert user", err)
	}
	return &user, nil
}

func (r *postgresUserRepository) GetByID(ctx context.Context, id string) (*model.User, error) {
	var user model.User
	err := r.pool.QueryRow(ctx, `
		SELECT id, phone, display_name, avatar_url, status_text, created_at, updated_at
		FROM users WHERE id = $1
	`, id).Scan(
		&user.ID, &user.Phone, &user.DisplayName, &user.AvatarURL,
		&user.StatusText, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		return nil, apperr.NewNotFound("user not found")
	}
	return &user, nil
}
