package repository

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/whatsapp-clone/backend/chat-service/internal/model"
)

type chatPostgres struct {
	pool *pgxpool.Pool
}

func NewChatPostgres(pool *pgxpool.Pool) ChatRepository {
	return &chatPostgres{pool: pool}
}

func (r *chatPostgres) CreateDirect(ctx context.Context, chat *model.Chat, participants [2]model.ChatParticipant) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx,
		`INSERT INTO chats (id, type, created_at, updated_at) VALUES ($1, $2, $3, $4)`,
		chat.ID, chat.Type, chat.CreatedAt, chat.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("insert chat: %w", err)
	}

	for _, p := range participants {
		_, err = tx.Exec(ctx,
			`INSERT INTO chat_participants (id, chat_id, user_id, role, is_muted, is_pinned, joined_at) VALUES ($1, $2, $3, $4, $5, $6, $7)`,
			p.ID, p.ChatID, p.UserID, p.Role, p.IsMuted, p.IsPinned, p.JoinedAt,
		)
		if err != nil {
			return fmt.Errorf("insert participant: %w", err)
		}
	}

	return tx.Commit(ctx)
}

func (r *chatPostgres) FindDirectChat(ctx context.Context, userID1, userID2 string) (*model.Chat, error) {
	query := `
		SELECT c.id, c.type, c.created_at, c.updated_at
		FROM chats c
		JOIN chat_participants cp1 ON c.id = cp1.chat_id AND cp1.user_id = $1
		JOIN chat_participants cp2 ON c.id = cp2.chat_id AND cp2.user_id = $2
		WHERE c.type = 'direct'
		GROUP BY c.id
		HAVING COUNT(DISTINCT cp1.user_id) + COUNT(DISTINCT cp2.user_id) = 2
		LIMIT 1`

	var chat model.Chat
	err := r.pool.QueryRow(ctx, query, userID1, userID2).
		Scan(&chat.ID, &chat.Type, &chat.CreatedAt, &chat.UpdatedAt)
	if err != nil {
		if err == pgx.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("find direct chat: %w", err)
	}
	return &chat, nil
}

func (r *chatPostgres) CreateGroup(ctx context.Context, chat *model.Chat, group *model.Group, participants []model.ChatParticipant) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx,
		`INSERT INTO chats (id, type, created_at, updated_at) VALUES ($1, $2, $3, $4)`,
		chat.ID, chat.Type, chat.CreatedAt, chat.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("insert chat: %w", err)
	}

	_, err = tx.Exec(ctx,
		`INSERT INTO groups (chat_id, name, description, avatar_url, created_by, is_admin_only, created_at, updated_at)
		 VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
		group.ChatID, group.Name, group.Description, group.AvatarURL,
		group.CreatedBy, group.IsAdminOnly, group.CreatedAt, group.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("insert group: %w", err)
	}

	for _, p := range participants {
		_, err = tx.Exec(ctx,
			`INSERT INTO chat_participants (id, chat_id, user_id, role, is_muted, is_pinned, joined_at) VALUES ($1, $2, $3, $4, $5, $6, $7)`,
			p.ID, p.ChatID, p.UserID, p.Role, p.IsMuted, p.IsPinned, p.JoinedAt,
		)
		if err != nil {
			return fmt.Errorf("insert participant: %w", err)
		}
	}

	return tx.Commit(ctx)
}

func (r *chatPostgres) GetByID(ctx context.Context, chatID string) (*model.Chat, error) {
	var chat model.Chat
	err := r.pool.QueryRow(ctx,
		`SELECT id, type, created_at, updated_at FROM chats WHERE id = $1`, chatID,
	).Scan(&chat.ID, &chat.Type, &chat.CreatedAt, &chat.UpdatedAt)
	if err != nil {
		if err == pgx.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("get chat by id: %w", err)
	}
	return &chat, nil
}

func (r *chatPostgres) GetUserChats(ctx context.Context, userID string) ([]string, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT chat_id FROM chat_participants WHERE user_id = $1`, userID,
	)
	if err != nil {
		return nil, fmt.Errorf("get user chats: %w", err)
	}
	defer rows.Close()

	var chatIDs []string
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return nil, fmt.Errorf("scan chat_id: %w", err)
		}
		chatIDs = append(chatIDs, id)
	}
	return chatIDs, rows.Err()
}

func (r *chatPostgres) GetParticipants(ctx context.Context, chatID string) ([]model.ChatParticipant, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, chat_id, user_id, role, is_muted, mute_until, is_pinned, joined_at
		 FROM chat_participants WHERE chat_id = $1`, chatID,
	)
	if err != nil {
		return nil, fmt.Errorf("get participants: %w", err)
	}
	defer rows.Close()

	var participants []model.ChatParticipant
	for rows.Next() {
		var p model.ChatParticipant
		if err := rows.Scan(&p.ID, &p.ChatID, &p.UserID, &p.Role, &p.IsMuted, &p.MuteUntil, &p.IsPinned, &p.JoinedAt); err != nil {
			return nil, fmt.Errorf("scan participant: %w", err)
		}
		participants = append(participants, p)
	}
	return participants, rows.Err()
}

func (r *chatPostgres) GetParticipant(ctx context.Context, chatID, userID string) (*model.ChatParticipant, error) {
	var p model.ChatParticipant
	err := r.pool.QueryRow(ctx,
		`SELECT id, chat_id, user_id, role, is_muted, mute_until, is_pinned, joined_at
		 FROM chat_participants WHERE chat_id = $1 AND user_id = $2`, chatID, userID,
	).Scan(&p.ID, &p.ChatID, &p.UserID, &p.Role, &p.IsMuted, &p.MuteUntil, &p.IsPinned, &p.JoinedAt)
	if err != nil {
		if err == pgx.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("get participant: %w", err)
	}
	return &p, nil
}

func (r *chatPostgres) AddParticipant(ctx context.Context, p *model.ChatParticipant) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO chat_participants (id, chat_id, user_id, role, is_muted, is_pinned, joined_at)
		 VALUES ($1, $2, $3, $4, $5, $6, $7)`,
		p.ID, p.ChatID, p.UserID, p.Role, p.IsMuted, p.IsPinned, p.JoinedAt,
	)
	if err != nil {
		return fmt.Errorf("add participant: %w", err)
	}
	return nil
}

func (r *chatPostgres) RemoveParticipant(ctx context.Context, chatID, userID string) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM chat_participants WHERE chat_id = $1 AND user_id = $2`, chatID, userID,
	)
	if err != nil {
		return fmt.Errorf("remove participant: %w", err)
	}
	return nil
}

func (r *chatPostgres) UpdateParticipantRole(ctx context.Context, chatID, userID, role string) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE chat_participants SET role = $3 WHERE chat_id = $1 AND user_id = $2`,
		chatID, userID, role,
	)
	if err != nil {
		return fmt.Errorf("update participant role: %w", err)
	}
	return nil
}

func (r *chatPostgres) UpdateMute(ctx context.Context, chatID, userID string, isMuted bool, muteUntil *time.Time) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE chat_participants SET is_muted = $3, mute_until = $4 WHERE chat_id = $1 AND user_id = $2`,
		chatID, userID, isMuted, muteUntil,
	)
	if err != nil {
		return fmt.Errorf("update mute: %w", err)
	}
	return nil
}

func (r *chatPostgres) UpdatePin(ctx context.Context, chatID, userID string, isPinned bool) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE chat_participants SET is_pinned = $3 WHERE chat_id = $1 AND user_id = $2`,
		chatID, userID, isPinned,
	)
	if err != nil {
		return fmt.Errorf("update pin: %w", err)
	}
	return nil
}

func (r *chatPostgres) GetGroup(ctx context.Context, chatID string) (*model.Group, error) {
	var g model.Group
	err := r.pool.QueryRow(ctx,
		`SELECT chat_id, name, description, avatar_url, created_by, is_admin_only, created_at, updated_at
		 FROM groups WHERE chat_id = $1`, chatID,
	).Scan(&g.ChatID, &g.Name, &g.Description, &g.AvatarURL, &g.CreatedBy, &g.IsAdminOnly, &g.CreatedAt, &g.UpdatedAt)
	if err != nil {
		if err == pgx.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("get group: %w", err)
	}
	return &g, nil
}

func (r *chatPostgres) IsAdmin(ctx context.Context, chatID, userID string) (bool, error) {
	var role string
	err := r.pool.QueryRow(ctx,
		`SELECT role FROM chat_participants WHERE chat_id = $1 AND user_id = $2`, chatID, userID,
	).Scan(&role)
	if err != nil {
		if err == pgx.ErrNoRows {
			return false, nil
		}
		return false, fmt.Errorf("check admin: %w", err)
	}
	return role == "admin", nil
}

func (r *chatPostgres) IsMember(ctx context.Context, chatID, userID string) (bool, error) {
	var exists bool
	err := r.pool.QueryRow(ctx,
		`SELECT EXISTS(SELECT 1 FROM chat_participants WHERE chat_id = $1 AND user_id = $2)`, chatID, userID,
	).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("check membership: %w", err)
	}
	return exists, nil
}

var allowedGroupFields = map[string]bool{
	"name": true, "description": true, "avatar_url": true, "is_admin_only": true,
}

func (r *chatPostgres) UpdateGroupRaw(ctx context.Context, chatID string, fields map[string]interface{}) error {
	if len(fields) == 0 {
		return nil
	}

	setClauses := make([]string, 0, len(fields)+1)
	args := make([]interface{}, 0, len(fields)+2)
	argIdx := 1

	for key, val := range fields {
		if !allowedGroupFields[key] {
			return fmt.Errorf("disallowed field: %s", key)
		}
		setClauses = append(setClauses, fmt.Sprintf("%s = $%d", key, argIdx))
		args = append(args, val)
		argIdx++
	}

	setClauses = append(setClauses, fmt.Sprintf("updated_at = $%d", argIdx))
	args = append(args, time.Now())
	argIdx++

	args = append(args, chatID)
	query := fmt.Sprintf("UPDATE groups SET %s WHERE chat_id = $%d",
		strings.Join(setClauses, ", "), argIdx)

	_, err := r.pool.Exec(ctx, query, args...)
	if err != nil {
		return fmt.Errorf("update group raw: %w", err)
	}
	return nil
}

func (r *chatPostgres) UpdateAutoDeleteTimer(ctx context.Context, chatID, userID string, timer *time.Duration) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE chat_participants SET auto_delete_timer = $1 WHERE chat_id = $2 AND user_id = $3`,
		timer, chatID, userID,
	)
	if err != nil {
		return fmt.Errorf("update auto delete timer: %w", err)
	}
	return nil
}

func (r *chatPostgres) UpdateGroup(ctx context.Context, chatID string, req *model.UpdateGroupRequest) error {
	setClauses := make([]string, 0, 4)
	args := make([]interface{}, 0, 5)
	argIdx := 1

	if req.Name != nil {
		setClauses = append(setClauses, fmt.Sprintf("name = $%d", argIdx))
		args = append(args, *req.Name)
		argIdx++
	}
	if req.Description != nil {
		setClauses = append(setClauses, fmt.Sprintf("description = $%d", argIdx))
		args = append(args, *req.Description)
		argIdx++
	}
	if req.AvatarURL != nil {
		setClauses = append(setClauses, fmt.Sprintf("avatar_url = $%d", argIdx))
		args = append(args, *req.AvatarURL)
		argIdx++
	}
	if req.IsAdminOnly != nil {
		setClauses = append(setClauses, fmt.Sprintf("is_admin_only = $%d", argIdx))
		args = append(args, *req.IsAdminOnly)
		argIdx++
	}

	if len(setClauses) == 0 {
		return nil
	}

	setClauses = append(setClauses, fmt.Sprintf("updated_at = $%d", argIdx))
	args = append(args, time.Now())
	argIdx++

	args = append(args, chatID)
	query := fmt.Sprintf("UPDATE groups SET %s WHERE chat_id = $%d",
		strings.Join(setClauses, ", "), argIdx)

	_, err := r.pool.Exec(ctx, query, args...)
	if err != nil {
		return fmt.Errorf("update group: %w", err)
	}
	return nil
}
