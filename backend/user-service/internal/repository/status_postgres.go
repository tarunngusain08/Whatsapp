package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type postgresStatusRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresStatusRepository(pool *pgxpool.Pool) StatusRepository {
	return &postgresStatusRepository{pool: pool}
}

func (r *postgresStatusRepository) Create(ctx context.Context, status *model.Status) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO statuses (id, user_id, type, content, caption, bg_color, created_at, expires_at)
		 VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
		status.ID, status.UserID, status.Type, status.Content,
		status.Caption, status.BgColor, status.CreatedAt, status.ExpiresAt,
	)
	if err != nil {
		return fmt.Errorf("create status: %w", err)
	}
	return nil
}

func (r *postgresStatusRepository) GetByUserID(ctx context.Context, userID string) ([]*model.Status, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT s.id, s.user_id, s.type, s.content, s.caption, s.bg_color, s.created_at, s.expires_at,
		        COALESCE(array_agg(sv.viewer_id) FILTER (WHERE sv.viewer_id IS NOT NULL), '{}')
		 FROM statuses s
		 LEFT JOIN status_viewers sv ON sv.status_id = s.id
		 WHERE s.user_id = $1 AND s.expires_at > NOW()
		 GROUP BY s.id
		 ORDER BY s.created_at DESC`, userID,
	)
	if err != nil {
		return nil, fmt.Errorf("get statuses by user id: %w", err)
	}
	defer rows.Close()

	return scanStatuses(rows)
}

func (r *postgresStatusRepository) GetByUserIDs(ctx context.Context, userIDs []string) ([]*model.Status, error) {
	now := time.Now()
	rows, err := r.pool.Query(ctx,
		`SELECT s.id, s.user_id, s.type, s.content, s.caption, s.bg_color, s.created_at, s.expires_at,
		        COALESCE(array_agg(sv.viewer_id) FILTER (WHERE sv.viewer_id IS NOT NULL), '{}')
		 FROM statuses s
		 LEFT JOIN status_viewers sv ON sv.status_id = s.id
		 WHERE s.user_id = ANY($1::uuid[]) AND s.expires_at > $2
		 GROUP BY s.id
		 ORDER BY s.created_at DESC`, userIDs, now,
	)
	if err != nil {
		return nil, fmt.Errorf("get statuses by user ids: %w", err)
	}
	defer rows.Close()

	return scanStatuses(rows)
}

func (r *postgresStatusRepository) Delete(ctx context.Context, id, userID string) error {
	tag, err := r.pool.Exec(ctx,
		`DELETE FROM statuses WHERE id = $1 AND user_id = $2`, id, userID,
	)
	if err != nil {
		return fmt.Errorf("delete status: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("status not found")
	}
	return nil
}

func (r *postgresStatusRepository) DeleteExpired(ctx context.Context) (int64, error) {
	tag, err := r.pool.Exec(ctx,
		`DELETE FROM statuses WHERE expires_at < NOW()`,
	)
	if err != nil {
		return 0, fmt.Errorf("delete expired statuses: %w", err)
	}
	return tag.RowsAffected(), nil
}

func (r *postgresStatusRepository) AddViewer(ctx context.Context, statusID, viewerID string) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO status_viewers (status_id, viewer_id)
		 VALUES ($1, $2)
		 ON CONFLICT (status_id, viewer_id) DO NOTHING`,
		statusID, viewerID,
	)
	if err != nil {
		return fmt.Errorf("add status viewer: %w", err)
	}
	return nil
}

func scanStatuses(rows pgx.Rows) ([]*model.Status, error) {
	var statuses []*model.Status
	for rows.Next() {
		var s model.Status
		if err := rows.Scan(
			&s.ID, &s.UserID, &s.Type, &s.Content, &s.Caption,
			&s.BgColor, &s.CreatedAt, &s.ExpiresAt, &s.Viewers,
		); err != nil {
			return nil, fmt.Errorf("scan status: %w", err)
		}
		statuses = append(statuses, &s)
	}
	return statuses, rows.Err()
}
