package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rs/zerolog"
)

// DeviceTokenRepository provides access to FCM device tokens.
type DeviceTokenRepository interface {
	GetByUserID(ctx context.Context, userID string) ([]string, error)
	DeleteByToken(ctx context.Context, token string) error
}

type deviceTokenPostgres struct {
	pool *pgxpool.Pool
	log  zerolog.Logger
}

func NewDeviceTokenRepository(pool *pgxpool.Pool, log zerolog.Logger) DeviceTokenRepository {
	return &deviceTokenPostgres{pool: pool, log: log}
}

func (r *deviceTokenPostgres) GetByUserID(ctx context.Context, userID string) ([]string, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT token FROM device_tokens WHERE user_id = $1`, userID)
	if err != nil {
		return nil, fmt.Errorf("query device tokens: %w", err)
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var token string
		if err := rows.Scan(&token); err != nil {
			return nil, fmt.Errorf("scan device token: %w", err)
		}
		tokens = append(tokens, token)
	}
	return tokens, rows.Err()
}

func (r *deviceTokenPostgres) DeleteByToken(ctx context.Context, token string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM device_tokens WHERE token = $1`, token)
	if err != nil {
		return fmt.Errorf("delete device token: %w", err)
	}
	r.log.Info().Str("token", token[:min(len(token), 12)]+"...").Msg("deleted stale FCM token")
	return nil
}
