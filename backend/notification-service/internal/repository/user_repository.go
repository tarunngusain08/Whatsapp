package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rs/zerolog"
)

// UserRepository provides access to basic user profile data for
// enriching push notification payloads.
type UserRepository interface {
	GetDisplayName(ctx context.Context, userID string) (string, string, error)
}

type userPostgres struct {
	pool *pgxpool.Pool
	log  zerolog.Logger
}

func NewUserRepository(pool *pgxpool.Pool, log zerolog.Logger) UserRepository {
	return &userPostgres{pool: pool, log: log}
}

// GetDisplayName returns (display_name, avatar_url, error) for a user.
func (r *userPostgres) GetDisplayName(ctx context.Context, userID string) (string, string, error) {
	var name, avatar string
	err := r.pool.QueryRow(ctx,
		`SELECT display_name, avatar_url FROM users WHERE id = $1`, userID,
	).Scan(&name, &avatar)
	if err != nil {
		return "", "", fmt.Errorf("get user display name: %w", err)
	}
	return name, avatar, nil
}
