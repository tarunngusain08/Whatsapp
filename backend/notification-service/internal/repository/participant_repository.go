package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rs/zerolog"
)

// ParticipantRepository provides access to chat participant mute status.
type ParticipantRepository interface {
	IsMuted(ctx context.Context, chatID, userID string) (bool, error)
}

type participantPostgres struct {
	pool *pgxpool.Pool
	log  zerolog.Logger
}

func NewParticipantRepository(pool *pgxpool.Pool, log zerolog.Logger) ParticipantRepository {
	return &participantPostgres{pool: pool, log: log}
}

func (r *participantPostgres) IsMuted(ctx context.Context, chatID, userID string) (bool, error) {
	var muted bool
	err := r.pool.QueryRow(ctx,
		`SELECT EXISTS (
			SELECT 1 FROM chat_participants
			WHERE chat_id = $1 AND user_id = $2 AND is_muted = true
			  AND (mute_until IS NULL OR mute_until > NOW())
		)`, chatID, userID).Scan(&muted)
	if err != nil {
		return false, fmt.Errorf("check mute status: %w", err)
	}
	return muted, nil
}
