package repository

import (
	"context"
	"fmt"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type postgresUserRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresUserRepository(pool *pgxpool.Pool) UserRepository {
	return &postgresUserRepository{pool: pool}
}

func (r *postgresUserRepository) GetByID(ctx context.Context, id string) (*model.User, error) {
	var u model.User
	err := r.pool.QueryRow(ctx,
		`SELECT id, phone, display_name, avatar_url, status_text, created_at, updated_at
		 FROM users WHERE id = $1`, id,
	).Scan(&u.ID, &u.Phone, &u.DisplayName, &u.AvatarURL, &u.StatusText, &u.CreatedAt, &u.UpdatedAt)

	if err == pgx.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get user by id: %w", err)
	}
	return &u, nil
}

func (r *postgresUserRepository) GetByIDs(ctx context.Context, ids []string) ([]*model.User, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, phone, display_name, avatar_url, status_text, created_at, updated_at
		 FROM users WHERE id = ANY($1::uuid[])`, ids,
	)
	if err != nil {
		return nil, fmt.Errorf("get users by ids: %w", err)
	}
	defer rows.Close()

	var users []*model.User
	for rows.Next() {
		var u model.User
		if err := rows.Scan(&u.ID, &u.Phone, &u.DisplayName, &u.AvatarURL, &u.StatusText, &u.CreatedAt, &u.UpdatedAt); err != nil {
			return nil, fmt.Errorf("scan user: %w", err)
		}
		users = append(users, &u)
	}
	return users, rows.Err()
}

func (r *postgresUserRepository) GetByPhone(ctx context.Context, phone string) (*model.User, error) {
	var u model.User
	err := r.pool.QueryRow(ctx,
		`SELECT id, phone, display_name, avatar_url, status_text, created_at, updated_at
		 FROM users WHERE phone = $1`, phone,
	).Scan(&u.ID, &u.Phone, &u.DisplayName, &u.AvatarURL, &u.StatusText, &u.CreatedAt, &u.UpdatedAt)

	if err == pgx.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get user by phone: %w", err)
	}
	return &u, nil
}

func (r *postgresUserRepository) GetByPhones(ctx context.Context, phones []string) ([]*model.ContactSyncResult, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, phone, display_name, avatar_url
		 FROM users WHERE phone = ANY($1::varchar[])`, phones,
	)
	if err != nil {
		return nil, fmt.Errorf("get users by phones: %w", err)
	}
	defer rows.Close()

	var results []*model.ContactSyncResult
	for rows.Next() {
		var r model.ContactSyncResult
		if err := rows.Scan(&r.UserID, &r.Phone, &r.DisplayName, &r.AvatarURL); err != nil {
			return nil, fmt.Errorf("scan contact sync result: %w", err)
		}
		results = append(results, &r)
	}
	return results, rows.Err()
}

func (r *postgresUserRepository) Update(ctx context.Context, id string, req *model.UpdateProfileRequest) (*model.User, error) {
	setClauses := make([]string, 0, 3)
	args := make([]interface{}, 0, 4)
	argIdx := 1

	if req.DisplayName != nil {
		setClauses = append(setClauses, fmt.Sprintf("display_name = $%d", argIdx))
		args = append(args, *req.DisplayName)
		argIdx++
	}
	if req.AvatarURL != nil {
		setClauses = append(setClauses, fmt.Sprintf("avatar_url = $%d", argIdx))
		args = append(args, *req.AvatarURL)
		argIdx++
	}
	if req.StatusText != nil {
		setClauses = append(setClauses, fmt.Sprintf("status_text = $%d", argIdx))
		args = append(args, *req.StatusText)
		argIdx++
	}

	if len(setClauses) == 0 {
		return r.GetByID(ctx, id)
	}

	setClauses = append(setClauses, "updated_at = NOW()")
	args = append(args, id)

	query := fmt.Sprintf(
		`UPDATE users SET %s WHERE id = $%d
		 RETURNING id, phone, display_name, avatar_url, status_text, created_at, updated_at`,
		strings.Join(setClauses, ", "), argIdx,
	)

	var u model.User
	err := r.pool.QueryRow(ctx, query, args...).
		Scan(&u.ID, &u.Phone, &u.DisplayName, &u.AvatarURL, &u.StatusText, &u.CreatedAt, &u.UpdatedAt)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("update user: %w", err)
	}
	return &u, nil
}
