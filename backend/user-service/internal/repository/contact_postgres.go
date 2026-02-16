package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type postgresContactRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresContactRepository(pool *pgxpool.Pool) ContactRepository {
	return &postgresContactRepository{pool: pool}
}

func (r *postgresContactRepository) Upsert(ctx context.Context, contact *model.Contact) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO contacts (user_id, contact_id, nickname)
		 VALUES ($1, $2, $3)
		 ON CONFLICT (user_id, contact_id) DO UPDATE SET nickname = EXCLUDED.nickname`,
		contact.UserID, contact.ContactID, contact.Nickname,
	)
	if err != nil {
		return fmt.Errorf("upsert contact: %w", err)
	}
	return nil
}

func (r *postgresContactRepository) GetByUserID(ctx context.Context, userID string) ([]*model.Contact, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, user_id, contact_id, nickname, is_blocked, created_at
		 FROM contacts WHERE user_id = $1
		 ORDER BY created_at DESC`, userID,
	)
	if err != nil {
		return nil, fmt.Errorf("get contacts by user id: %w", err)
	}
	defer rows.Close()

	var contacts []*model.Contact
	for rows.Next() {
		var c model.Contact
		if err := rows.Scan(&c.ID, &c.UserID, &c.ContactID, &c.Nickname, &c.IsBlocked, &c.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan contact: %w", err)
		}
		contacts = append(contacts, &c)
	}
	return contacts, rows.Err()
}

func (r *postgresContactRepository) IsContact(ctx context.Context, userID, contactID string) (bool, error) {
	var exists bool
	err := r.pool.QueryRow(ctx,
		`SELECT EXISTS(SELECT 1 FROM contacts WHERE user_id = $1 AND contact_id = $2)`,
		userID, contactID,
	).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check is contact: %w", err)
	}
	return exists, nil
}

func (r *postgresContactRepository) Block(ctx context.Context, userID, contactID string) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO contacts (user_id, contact_id, is_blocked)
		 VALUES ($1, $2, true)
		 ON CONFLICT (user_id, contact_id) DO UPDATE SET is_blocked = true`,
		userID, contactID,
	)
	if err != nil {
		return fmt.Errorf("block contact: %w", err)
	}
	return nil
}

func (r *postgresContactRepository) Unblock(ctx context.Context, userID, contactID string) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE contacts SET is_blocked = false WHERE user_id = $1 AND contact_id = $2`,
		userID, contactID,
	)
	if err != nil {
		return fmt.Errorf("unblock contact: %w", err)
	}
	return nil
}

func (r *postgresContactRepository) IsBlocked(ctx context.Context, userID, contactID string) (bool, error) {
	var blocked bool
	err := r.pool.QueryRow(ctx,
		`SELECT COALESCE(is_blocked, false) FROM contacts WHERE user_id = $1 AND contact_id = $2`,
		userID, contactID,
	).Scan(&blocked)
	if err == pgx.ErrNoRows {
		return false, nil
	}
	if err != nil {
		return false, fmt.Errorf("check is blocked: %w", err)
	}
	return blocked, nil
}

func (r *postgresContactRepository) Delete(ctx context.Context, userID, contactID string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM contacts WHERE user_id = $1 AND contact_id = $2`,
		userID, contactID,
	)
	if err != nil {
		return fmt.Errorf("delete contact: %w", err)
	}
	return nil
}
