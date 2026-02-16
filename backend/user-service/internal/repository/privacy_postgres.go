package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type postgresPrivacyRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresPrivacyRepository(pool *pgxpool.Pool) PrivacyRepository {
	return &postgresPrivacyRepository{pool: pool}
}

func (r *postgresPrivacyRepository) Get(ctx context.Context, userID string) (*model.PrivacySettings, error) {
	var p model.PrivacySettings
	err := r.pool.QueryRow(ctx,
		`SELECT user_id, last_seen, profile_photo, about, read_receipts, updated_at
		 FROM privacy_settings WHERE user_id = $1`, userID,
	).Scan(&p.UserID, &p.LastSeen, &p.ProfilePhoto, &p.About, &p.ReadReceipts, &p.UpdatedAt)

	if err == pgx.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get privacy settings: %w", err)
	}
	return &p, nil
}

func (r *postgresPrivacyRepository) Upsert(ctx context.Context, settings *model.PrivacySettings) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO privacy_settings (user_id, last_seen, profile_photo, about, read_receipts, updated_at)
		 VALUES ($1, $2, $3, $4, $5, NOW())
		 ON CONFLICT (user_id) DO UPDATE SET
		   last_seen = EXCLUDED.last_seen,
		   profile_photo = EXCLUDED.profile_photo,
		   about = EXCLUDED.about,
		   read_receipts = EXCLUDED.read_receipts,
		   updated_at = NOW()`,
		settings.UserID, settings.LastSeen, settings.ProfilePhoto, settings.About, settings.ReadReceipts,
	)
	if err != nil {
		return fmt.Errorf("upsert privacy settings: %w", err)
	}
	return nil
}
