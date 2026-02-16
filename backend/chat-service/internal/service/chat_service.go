package service

import (
	"context"
	"time"

	"github.com/whatsapp-clone/backend/chat-service/internal/model"
)

type ChatService interface {
	// CreateDirectChat creates or returns an existing direct chat between two users.
	CreateDirectChat(ctx context.Context, callerID string, req *model.CreateDirectChatRequest) (*model.Chat, error)

	// CreateGroup creates a new group chat with the caller as admin.
	CreateGroup(ctx context.Context, callerID string, req *model.CreateGroupRequest) (*model.Chat, *model.Group, error)

	// ListChats returns all chats for a user with last message previews and unread counts.
	ListChats(ctx context.Context, userID string) ([]*model.ChatListItem, error)

	// GetChat retrieves a single chat by ID with full details.
	GetChat(ctx context.Context, callerID, chatID string) (*model.ChatListItem, error)

	// AddMember adds a user to a group chat (admin only).
	AddMember(ctx context.Context, callerID, chatID, targetUserID string) error

	// RemoveMember removes a user from a group chat (admin only, or self-removal).
	RemoveMember(ctx context.Context, callerID, chatID, targetUserID string) error

	// PromoteMember promotes a member to admin (admin only).
	PromoteMember(ctx context.Context, callerID, chatID, targetUserID string) error

	// DemoteMember demotes an admin to member (admin only).
	DemoteMember(ctx context.Context, callerID, chatID, targetUserID string) error

	// UpdateGroup updates group metadata (admin only).
	UpdateGroup(ctx context.Context, callerID, chatID string, req *model.UpdateGroupRequest) error

	// MuteChat mutes/unmutes a chat for the caller.
	MuteChat(ctx context.Context, userID, chatID string, mute bool, muteUntil *time.Time) error

	// PinChat pins/unpins a chat for the caller.
	PinChat(ctx context.Context, userID, chatID string, pin bool) error

	// UploadGroupAvatar updates the avatar for a group chat (admin only).
	UploadGroupAvatar(ctx context.Context, chatID, userID string) (string, error)

	// SetDisappearingMessages sets the auto-delete timer for a chat participant.
	SetDisappearingMessages(ctx context.Context, chatID, userID string, timer *time.Duration) error
}
