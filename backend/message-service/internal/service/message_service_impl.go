package service

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/rs/zerolog"
	"go.mongodb.org/mongo-driver/mongo"

	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
	userv1 "github.com/whatsapp-clone/backend/proto/user/v1"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
	"github.com/whatsapp-clone/backend/message-service/internal/repository"
)

type messageServiceImpl struct {
	messageRepo repository.MessageRepository
	publisher   *EventPublisher
	userClient  userv1.UserServiceClient
	chatClient  chatv1.ChatServiceClient
	log         zerolog.Logger
}

func NewMessageService(repo repository.MessageRepository, pub *EventPublisher, userClient userv1.UserServiceClient, chatClient chatv1.ChatServiceClient, log zerolog.Logger) MessageService {
	return &messageServiceImpl{
		messageRepo: repo,
		publisher:   pub,
		userClient:  userClient,
		chatClient:  chatClient,
		log:         log,
	}
}

// SendMessage validates, persists (with client_msg_id dedup), and publishes a new message.
func (s *messageServiceImpl) SendMessage(ctx context.Context, senderID string, req *model.SendMessageRequest) (*model.Message, error) {
	// P5-06: Enforce admin-only messaging via chat-service gRPC.
	permResp, err := s.chatClient.CheckChatPermission(ctx, &chatv1.CheckChatPermissionRequest{
		ChatId: req.ChatID,
		UserId: senderID,
	})
	if err != nil {
		s.log.Warn().Err(err).Str("chat_id", req.ChatID).Msg("failed to check chat permission, allowing message")
	} else {
		if !permResp.IsMember {
			return nil, apperr.NewForbidden("not a member of this chat")
		}
		if permResp.IsAdminOnly && !permResp.IsAdmin {
			return nil, apperr.NewForbidden("only admins can send messages in this chat")
		}
	}

	if err := validateMessagePayload(req.Type, req.Payload); err != nil {
		return nil, err
	}

	now := time.Now()
	msgID := uuid.New().String()

	msg := &model.Message{
		MessageID:        msgID,
		ChatID:           req.ChatID,
		SenderID:         senderID,
		ClientMsgID:      req.ClientMsgID,
		Type:             req.Type,
		ReplyToMessageID: req.ReplyToMessageID,
		ForwardedFrom:    req.ForwardedFrom,
		Payload:          req.Payload,
		Status:           make(map[string]model.RecipientStatus),
		IsDeleted:        false,
		IsStarredBy:      []string{},
		CreatedAt:        now,
		UpdatedAt:        now,
	}

	result, err := s.messageRepo.Insert(ctx, msg)
	if err != nil {
		return nil, apperr.NewInternal("failed to insert message", err)
	}

	if pubErr := s.publisher.PublishNewMessage(ctx, result); pubErr != nil {
		s.log.Error().Err(pubErr).Str("message_id", result.MessageID).Msg("failed to publish msg.new event")
	}

	return result, nil
}

// GetMessages returns messages for a chat with cursor-based pagination.
func (s *messageServiceImpl) GetMessages(ctx context.Context, query *model.ListMessagesQuery) ([]*model.Message, error) {
	limit := query.Limit
	if limit <= 0 || limit > 100 {
		limit = 50
	}

	var cursorTime *time.Time
	if query.Cursor != "" {
		t, err := time.Parse(time.RFC3339Nano, query.Cursor)
		if err != nil {
			return nil, apperr.NewBadRequest("invalid cursor format, expected RFC3339Nano")
		}
		cursorTime = &t
	}

	msgs, err := s.messageRepo.ListByChatID(ctx, query.ChatID, cursorTime, query.CursorID, limit)
	if err != nil {
		return nil, apperr.NewInternal("failed to list messages", err)
	}

	// Populate reply previews
	for _, msg := range msgs {
		if msg.ReplyToMessageID != "" {
			original, err := s.messageRepo.GetByID(ctx, msg.ReplyToMessageID)
			if err == nil && original != nil {
				body := original.Payload.Body
				if len(body) > 100 {
					body = body[:97] + "..."
				}
				msg.ReplyToPreview = &model.ReplyPreview{
					MessageID: original.MessageID,
					SenderID:  original.SenderID,
					Type:      original.Type,
					Body:      body,
				}
			}
		}
	}

	return msgs, nil
}

// UpdateStatus validates the status transition, updates the repo, and publishes an event.
func (s *messageServiceImpl) UpdateStatus(ctx context.Context, messageID, userID, status string) error {
	msgStatus := model.MessageStatus(status)
	if msgStatus != model.StatusDelivered && msgStatus != model.StatusRead {
		return apperr.NewBadRequest("invalid status, must be 'delivered' or 'read'")
	}

	recipientStatus := model.RecipientStatus{
		Status:    msgStatus,
		UpdatedAt: time.Now(),
	}

	err := s.messageRepo.UpdateStatus(ctx, messageID, userID, recipientStatus)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperr.NewNotFound("message not found")
		}
		return apperr.NewInternal("failed to update status", err)
	}

	// P4-09: Check reading user's read_receipts privacy setting.
	if msgStatus == model.StatusRead {
		privacyResp, err := s.userClient.GetPrivacySettings(ctx, &userv1.GetPrivacySettingsRequest{
			UserId: userID,
		})
		if err != nil {
			s.log.Warn().Err(err).Str("user_id", userID).Msg("failed to check read receipt privacy, publishing anyway")
		} else if !privacyResp.ReadReceipts {
			s.log.Debug().Str("user_id", userID).Msg("user has read receipts disabled, skipping publish")
			return nil
		}
	}

	// Fetch the message to get sender_id and chat_id for routing the status update
	msg, msgErr := s.messageRepo.GetByID(ctx, messageID)
	senderID := ""
	chatID := ""
	if msgErr == nil && msg != nil {
		senderID = msg.SenderID
		chatID = msg.ChatID
	}

	if pubErr := s.publisher.PublishStatusUpdate(ctx, messageID, chatID, userID, status, senderID); pubErr != nil {
		s.log.Error().Err(pubErr).Str("message_id", messageID).Msg("failed to publish msg.status.updated event")
	}

	return nil
}

// DeleteMessage verifies sender ownership, soft-deletes, and publishes a delete event.
func (s *messageServiceImpl) DeleteMessage(ctx context.Context, messageID, senderID string) error {
	msg, err := s.messageRepo.GetByID(ctx, messageID)
	if err != nil {
		return apperr.NewInternal("failed to get message", err)
	}
	if msg == nil {
		return apperr.NewNotFound("message not found")
	}
	if msg.SenderID != senderID {
		return apperr.NewForbidden("only the sender can delete a message for everyone")
	}

	if err := s.messageRepo.SoftDelete(ctx, messageID, senderID); err != nil {
		return apperr.NewInternal("failed to delete message", err)
	}

	if pubErr := s.publisher.PublishMessageDeleted(ctx, messageID, msg.ChatID, senderID, true); pubErr != nil {
		s.log.Error().Err(pubErr).Str("message_id", messageID).Msg("failed to publish msg.deleted event")
	}

	return nil
}

func (s *messageServiceImpl) SoftDeleteForUser(ctx context.Context, messageID, userID string) error {
	msg, err := s.messageRepo.GetByID(ctx, messageID)
	if err != nil {
		return apperr.NewInternal("failed to get message", err)
	}
	if msg == nil {
		return apperr.NewNotFound("message not found")
	}

	if err := s.messageRepo.SoftDeleteForUser(ctx, messageID, userID); err != nil {
		return apperr.NewInternal("failed to soft delete message for user", err)
	}

	return nil
}

// ForwardMessage fetches the original message and re-sends it to the target chat.
func (s *messageServiceImpl) ForwardMessage(ctx context.Context, senderID, targetChatID, sourceMessageID string) (*model.Message, error) {
	original, err := s.messageRepo.GetByID(ctx, sourceMessageID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get source message", err)
	}
	if original == nil {
		return nil, apperr.NewNotFound("source message not found")
	}

	forwardReq := &model.SendMessageRequest{
		ChatID:      targetChatID,
		Type:        original.Type,
		Payload:     original.Payload,
		ClientMsgID: uuid.New().String(),
		ForwardedFrom: &model.ForwardedFrom{
			ChatID:    original.ChatID,
			MessageID: original.MessageID,
		},
	}

	return s.SendMessage(ctx, senderID, forwardReq)
}

// StarMessage adds the user to the message's starred list.
func (s *messageServiceImpl) StarMessage(ctx context.Context, messageID, userID string) error {
	err := s.messageRepo.StarMessage(ctx, messageID, userID)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperr.NewNotFound("message not found")
		}
		return apperr.NewInternal("failed to star message", err)
	}
	return nil
}

// UnstarMessage removes the user from the message's starred list.
func (s *messageServiceImpl) UnstarMessage(ctx context.Context, messageID, userID string) error {
	err := s.messageRepo.UnstarMessage(ctx, messageID, userID)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperr.NewNotFound("message not found")
		}
		return apperr.NewInternal("failed to unstar message", err)
	}
	return nil
}

// GetMessageByID retrieves a single message by its ID.
func (s *messageServiceImpl) GetMessageByID(ctx context.Context, messageID string) (*model.Message, error) {
	msg, err := s.messageRepo.GetByID(ctx, messageID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get message", err)
	}
	if msg == nil {
		return nil, apperr.NewNotFound("message not found")
	}
	return msg, nil
}

// ReactToMessage adds or replaces a user's reaction on a message.
func (s *messageServiceImpl) ReactToMessage(ctx context.Context, messageID, userID, emoji string) error {
	if emoji == "" {
		return apperr.NewBadRequest("emoji is required")
	}
	// Limit reaction length to prevent abuse (standard emoji are 1-12 bytes)
	if len(emoji) > 32 {
		return apperr.NewBadRequest("invalid emoji: too long")
	}
	err := s.messageRepo.AddReaction(ctx, messageID, userID, emoji)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperr.NewNotFound("message not found")
		}
		return apperr.NewInternal("failed to add reaction", err)
	}

	msg, msgErr := s.messageRepo.GetByID(ctx, messageID)
	chatID := ""
	if msgErr == nil && msg != nil {
		chatID = msg.ChatID
	}

	if pubErr := s.publisher.PublishReaction(ctx, messageID, chatID, userID, emoji, false); pubErr != nil {
		s.log.Error().Err(pubErr).Str("message_id", messageID).Msg("failed to publish msg.reaction event")
	}

	return nil
}

// RemoveReaction removes a user's reaction from a message.
func (s *messageServiceImpl) RemoveReaction(ctx context.Context, messageID, userID string) error {
	err := s.messageRepo.RemoveReaction(ctx, messageID, userID)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperr.NewNotFound("message not found")
		}
		return apperr.NewInternal("failed to remove reaction", err)
	}

	msg, msgErr := s.messageRepo.GetByID(ctx, messageID)
	chatID := ""
	if msgErr == nil && msg != nil {
		chatID = msg.ChatID
	}

	if pubErr := s.publisher.PublishReaction(ctx, messageID, chatID, userID, "", true); pubErr != nil {
		s.log.Error().Err(pubErr).Str("message_id", messageID).Msg("failed to publish msg.reaction removed event")
	}

	return nil
}

// SearchMessages delegates full-text search to the repository.
func (s *messageServiceImpl) SearchMessages(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error) {
	msgs, err := s.messageRepo.Search(ctx, chatID, query, limit)
	if err != nil {
		return nil, apperr.NewInternal("failed to search messages", err)
	}
	return msgs, nil
}

// SearchGlobal searches messages across multiple chats.
// NOTE: In a full implementation, this would call chat-service gRPC to resolve
// the user's chat IDs. For now, chatIDs must be provided by the caller (HTTP handler / gateway).
func (s *messageServiceImpl) SearchGlobal(ctx context.Context, userID, query string, chatIDs []string, limit int) ([]*model.Message, error) {
	if limit <= 0 || limit > 100 {
		limit = 20
	}

	msgs, err := s.messageRepo.SearchGlobal(ctx, chatIDs, query, limit)
	if err != nil {
		return nil, apperr.NewInternal("failed to search messages globally", err)
	}
	return msgs, nil
}

// GetLastMessages delegates to the repository aggregation.
func (s *messageServiceImpl) GetLastMessages(ctx context.Context, chatIDs []string) (map[string]*model.Message, error) {
	msgs, err := s.messageRepo.GetLastPerChat(ctx, chatIDs)
	if err != nil {
		return nil, apperr.NewInternal("failed to get last messages", err)
	}
	return msgs, nil
}

// GetUnreadCounts delegates to the repository aggregation.
func (s *messageServiceImpl) GetUnreadCounts(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error) {
	counts, err := s.messageRepo.CountUnread(ctx, userID, chatIDs)
	if err != nil {
		return nil, apperr.NewInternal("failed to get unread counts", err)
	}
	return counts, nil
}

// validateMessagePayload checks that the payload contains required fields for the given type.
func validateMessagePayload(msgType model.MessageType, payload model.MessagePayload) error {
	switch msgType {
	case model.MessageTypeText:
		if payload.Body == "" {
			return apperr.NewBadRequest("text message requires a non-empty body")
		}
	case model.MessageTypeImage, model.MessageTypeVideo, model.MessageTypeAudio, model.MessageTypeDocument:
		if payload.MediaID == "" {
			return apperr.NewBadRequest(string(msgType) + " message requires media_id")
		}
	case model.MessageTypeLocation:
		if payload.Body == "" {
			return apperr.NewBadRequest("location message requires body with coordinates")
		}
	default:
		return apperr.NewBadRequest("unsupported message type: " + string(msgType))
	}
	return nil
}
