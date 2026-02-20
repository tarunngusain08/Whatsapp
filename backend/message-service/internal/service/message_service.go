package service

import (
	"context"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
)

type MessageService interface {
	SendMessage(ctx context.Context, senderID string, req *model.SendMessageRequest) (*model.Message, error)
	GetMessages(ctx context.Context, query *model.ListMessagesQuery) ([]*model.Message, error)
	GetMessageByID(ctx context.Context, messageID string) (*model.Message, error)
	UpdateStatus(ctx context.Context, messageID, userID, status string) error
	DeleteMessage(ctx context.Context, messageID, senderID string) error
	SoftDeleteForUser(ctx context.Context, messageID, userID string) error
	StarMessage(ctx context.Context, messageID, userID string) error
	UnstarMessage(ctx context.Context, messageID, userID string) error
	ReactToMessage(ctx context.Context, messageID, userID, emoji string) error
	RemoveReaction(ctx context.Context, messageID, userID string) error
	ForwardMessage(ctx context.Context, senderID, targetChatID, sourceMessageID string) (*model.Message, error)
	SearchMessages(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error)
	SearchGlobal(ctx context.Context, userID, query string, chatIDs []string, limit int) ([]*model.Message, error)
	GetLastMessages(ctx context.Context, chatIDs []string) (map[string]*model.Message, error)
	GetUnreadCounts(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error)
}
