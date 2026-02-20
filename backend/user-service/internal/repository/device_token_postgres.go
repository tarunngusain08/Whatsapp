package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type postgresDeviceTokenRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresDeviceTokenRepository(pool *pgxpool.Pool) DeviceTokenRepository {
	return &postgresDeviceTokenRepository{pool: pool}
}

func (r *postgresDeviceTokenRepository) Upsert(ctx context.Context, token *model.DeviceToken) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO device_tokens (user_id, token, platform, updated_at)
		 VALUES ($1, $2, $3, NOW())
		 ON CONFLICT (token) DO UPDATE SET
		   user_id = EXCLUDED.user_id,
		   platform = EXCLUDED.platform,
		   updated_at = NOW()`,
		token.UserID, token.Token, token.Platform,
	)
	if err != nil {
		return fmt.Errorf("upsert device token: %w", err)
	}
	return nil
}

func (r *postgresDeviceTokenRepository) GetByUserID(ctx context.Context, userID string) ([]*model.DeviceToken, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, user_id, token, platform, created_at, updated_at
		 FROM device_tokens WHERE user_id = $1
		 ORDER BY updated_at DESC`, userID,
	)
	if err != nil {
		return nil, fmt.Errorf("get device tokens by user id: %w", err)
	}
	defer rows.Close()

	var tokens []*model.DeviceToken
	for rows.Next() {
		var t model.DeviceToken
		if err := rows.Scan(&t.ID, &t.UserID, &t.Token, &t.Platform, &t.CreatedAt, &t.UpdatedAt); err != nil {
			return nil, fmt.Errorf("scan device token: %w", err)
		}
		tokens = append(tokens, &t)
	}
	return tokens, rows.Err()
}

func (r *postgresDeviceTokenRepository) DeleteByToken(ctx context.Context, token string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM device_tokens WHERE token = $1`, token,
	)
	if err != nil {
		return fmt.Errorf("delete device token: %w", err)
	}
	return nil
}

func (r *postgresDeviceTokenRepository) DeleteByTokenAndUser(ctx context.Context, userID, token string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM device_tokens WHERE token = $1 AND user_id = $2`, token, userID,
	)
	if err != nil {
		return fmt.Errorf("delete device token for user: %w", err)
	}
	return nil
}

func (r *postgresDeviceTokenRepository) DeleteByUserID(ctx context.Context, userID string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM device_tokens WHERE user_id = $1`, userID,
	)
	if err != nil {
		return fmt.Errorf("delete device tokens by user id: %w", err)
	}
	return nil
}
