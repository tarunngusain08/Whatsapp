package repository

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

type postgresRefreshTokenRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresRefreshTokenRepository(pool *pgxpool.Pool) RefreshTokenRepository {
	return &postgresRefreshTokenRepository{pool: pool}
}

func (r *postgresRefreshTokenRepository) Create(ctx context.Context, token *model.RefreshToken) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
		VALUES ($1, $2, $3)
	`, token.UserID, token.TokenHash, token.ExpiresAt)
	if err != nil {
		return apperr.NewInternal("failed to create refresh token", err)
	}
	return nil
}

func (r *postgresRefreshTokenRepository) GetByTokenHash(ctx context.Context, tokenHash string) (*model.RefreshToken, error) {
	var token model.RefreshToken
	err := r.pool.QueryRow(ctx, `
		SELECT id, user_id, token_hash, expires_at, revoked, created_at
		FROM refresh_tokens
		WHERE token_hash = $1 AND revoked = FALSE AND expires_at > NOW()
	`, tokenHash).Scan(
		&token.ID, &token.UserID, &token.TokenHash,
		&token.ExpiresAt, &token.Revoked, &token.CreatedAt,
	)
	if err != nil {
		if err == pgx.ErrNoRows {
			return nil, nil
		}
		return nil, apperr.NewInternal("failed to get refresh token", err)
	}
	return &token, nil
}

func (r *postgresRefreshTokenRepository) RevokeByID(ctx context.Context, id string) error {
	_, err := r.pool.Exec(ctx, `UPDATE refresh_tokens SET revoked = TRUE WHERE id = $1`, id)
	if err != nil {
		return apperr.NewInternal("failed to revoke refresh token", err)
	}
	return nil
}

func (r *postgresRefreshTokenRepository) RevokeAllByUserID(ctx context.Context, userID string) error {
	_, err := r.pool.Exec(ctx, `UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = $1`, userID)
	if err != nil {
		return apperr.NewInternal("failed to revoke all refresh tokens", err)
	}
	return nil
}

func (r *postgresRefreshTokenRepository) ReplaceToken(ctx context.Context, oldID string, newToken *model.RefreshToken) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return apperr.NewInternal("failed to begin transaction", err)
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `UPDATE refresh_tokens SET revoked = TRUE WHERE id = $1`, oldID); err != nil {
		return apperr.NewInternal("failed to revoke old refresh token", err)
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
		VALUES ($1, $2, $3)
	`, newToken.UserID, newToken.TokenHash, newToken.ExpiresAt); err != nil {
		return apperr.NewInternal("failed to create new refresh token", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return apperr.NewInternal("failed to commit token replacement", err)
	}
	return nil
}
