package repository

import (
	"context"
	"time"

	"github.com/whatsapp-clone/backend/chat-service/internal/model"
)

type ChatRepository interface {
	// CreateDirect creates a direct chat with two participants in a single transaction.
	CreateDirect(ctx context.Context, chat *model.Chat, participants [2]model.ChatParticipant) error

	// FindDirectChat finds an existing direct chat between two users.
	FindDirectChat(ctx context.Context, userID1, userID2 string) (*model.Chat, error)

	// CreateGroup creates a group chat with initial participants and group metadata atomically.
	CreateGroup(ctx context.Context, chat *model.Chat, group *model.Group, participants []model.ChatParticipant) error

	// GetByID retrieves a chat by ID.
	GetByID(ctx context.Context, chatID string) (*model.Chat, error)

	// GetUserChats retrieves all chat IDs that a user is a participant of.
	GetUserChats(ctx context.Context, userID string) ([]string, error)

	// GetParticipants returns all participants for a chat.
	GetParticipants(ctx context.Context, chatID string) ([]model.ChatParticipant, error)

	// GetParticipant returns a specific participant in a chat.
	GetParticipant(ctx context.Context, chatID, userID string) (*model.ChatParticipant, error)

	// AddParticipant adds a user to a chat.
	AddParticipant(ctx context.Context, p *model.ChatParticipant) error

	// RemoveParticipant removes a user from a chat.
	RemoveParticipant(ctx context.Context, chatID, userID string) error

	// UpdateParticipantRole changes a participant's role (admin/member).
	UpdateParticipantRole(ctx context.Context, chatID, userID, role string) error

	// UpdateMute sets mute status for a participant.
	UpdateMute(ctx context.Context, chatID, userID string, isMuted bool, muteUntil *time.Time) error

	// UpdatePin sets pin status for a participant.
	UpdatePin(ctx context.Context, chatID, userID string, isPinned bool) error

	// GetGroup returns group metadata.
	GetGroup(ctx context.Context, chatID string) (*model.Group, error)

	// UpdateGroup updates group metadata.
	UpdateGroup(ctx context.Context, chatID string, req *model.UpdateGroupRequest) error

	// IsAdmin checks if a user is an admin of a chat.
	IsAdmin(ctx context.Context, chatID, userID string) (bool, error)

	// IsMember checks if a user is a member of a chat.
	IsMember(ctx context.Context, chatID, userID string) (bool, error)

	// UpdateGroupRaw updates group fields using a raw map (for avatar updates etc.).
	UpdateGroupRaw(ctx context.Context, chatID string, fields map[string]interface{}) error

	// UpdateAutoDeleteTimer sets the auto-delete timer for a participant.
	UpdateAutoDeleteTimer(ctx context.Context, chatID, userID string, timer *time.Duration) error
}
