package repository

import (
	"context"
	"time"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
)

type MessageRepository interface {
	// Insert creates a new message. Uses client_msg_id unique index for idempotency.
	// Returns the existing message if client_msg_id already exists.
	Insert(ctx context.Context, msg *model.Message) (*model.Message, error)

	// GetByID retrieves a single message by message_id.
	GetByID(ctx context.Context, messageID string) (*model.Message, error)

	// ListByChatID returns messages for a chat using cursor-based pagination.
	// Cursor is (created_at, message_id) for deterministic ordering.
	ListByChatID(ctx context.Context, chatID string, cursorTime *time.Time, cursorID string, limit int) ([]*model.Message, error)

	// UpdateStatus updates the status map entry for a specific recipient.
	UpdateStatus(ctx context.Context, messageID, userID string, status model.RecipientStatus) error

	// SoftDelete marks a message as deleted (sets is_deleted=true, clears payload).
	SoftDelete(ctx context.Context, messageID, senderID string) error

	// SoftDeleteForUser adds a user to the deleted_for_users list for per-user deletion.
	SoftDeleteForUser(ctx context.Context, messageID, userID string) error

	// StarMessage adds userID to is_starred_by array.
	StarMessage(ctx context.Context, messageID, userID string) error

	// UnstarMessage removes userID from is_starred_by array.
	UnstarMessage(ctx context.Context, messageID, userID string) error

	// AddReaction adds or replaces a user's reaction on a message (one reaction per user).
	AddReaction(ctx context.Context, messageID, userID, emoji string) error

	// RemoveReaction removes a user's reaction from a message.
	RemoveReaction(ctx context.Context, messageID, userID string) error

	// Search performs a full-text search within a chat using MongoDB $text index.
	Search(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error)

	// SearchGlobal performs a full-text search across multiple chats.
	SearchGlobal(ctx context.Context, chatIDs []string, query string, limit int) ([]*model.Message, error)

	// GetLastPerChat returns the latest message for each given chat ID.
	GetLastPerChat(ctx context.Context, chatIDs []string) (map[string]*model.Message, error)

	// CountUnread returns the count of unread messages per chat for the given user.
	CountUnread(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error)

	// DeleteExpiredMessages soft-deletes messages older than the given cutoff time.
	// Used by the disappearing messages cleanup job.
	DeleteExpiredMessages(ctx context.Context, olderThan time.Time) (int64, error)
}
