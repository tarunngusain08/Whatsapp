package service

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/nats-io/nats.go"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/chat-service/internal/model"
	"github.com/whatsapp-clone/backend/chat-service/internal/repository"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
)

type chatServiceImpl struct {
	chatRepo      repository.ChatRepository
	messageClient messagev1.MessageServiceClient
	eventPublisher
}

func NewChatService(
	chatRepo repository.ChatRepository,
	messageClient messagev1.MessageServiceClient,
	js nats.JetStreamContext,
	log zerolog.Logger,
) ChatService {
	return &chatServiceImpl{
		chatRepo:      chatRepo,
		messageClient: messageClient,
		eventPublisher: eventPublisher{
			js:  js,
			log: log,
		},
	}
}

func (s *chatServiceImpl) CreateDirectChat(ctx context.Context, callerID string, req *model.CreateDirectChatRequest) (*model.Chat, error) {
	if callerID == req.OtherUserID {
		return nil, apperr.NewBadRequest("cannot create chat with yourself")
	}

	existing, err := s.chatRepo.FindDirectChat(ctx, callerID, req.OtherUserID)
	if err != nil {
		return nil, apperr.NewInternal("failed to check existing chat", err)
	}
	if existing != nil {
		return existing, nil
	}

	chatID := uuid.New().String()
	now := time.Now()

	chat := &model.Chat{
		ID:        chatID,
		Type:      model.ChatTypeDirect,
		CreatedAt: now,
		UpdatedAt: now,
	}

	participants := [2]model.ChatParticipant{
		{ID: uuid.New().String(), ChatID: chatID, UserID: callerID, Role: "member", JoinedAt: now},
		{ID: uuid.New().String(), ChatID: chatID, UserID: req.OtherUserID, Role: "member", JoinedAt: now},
	}

	if err := s.chatRepo.CreateDirect(ctx, chat, participants); err != nil {
		return nil, apperr.NewInternal("failed to create direct chat", err)
	}

	s.publishEvent("chat.created", map[string]interface{}{
		"chat_id":      chatID,
		"type":         "direct",
		"participants": []string{callerID, req.OtherUserID},
	})

	return chat, nil
}

func (s *chatServiceImpl) CreateGroup(ctx context.Context, callerID string, req *model.CreateGroupRequest) (*model.Chat, *model.Group, error) {
	chatID := uuid.New().String()
	now := time.Now()

	chat := &model.Chat{
		ID:        chatID,
		Type:      model.ChatTypeGroup,
		CreatedAt: now,
		UpdatedAt: now,
	}

	group := &model.Group{
		ChatID:      chatID,
		Name:        req.Name,
		Description: req.Description,
		CreatedBy:   callerID,
		CreatedAt:   now,
		UpdatedAt:   now,
	}

	// Build participant list: caller is admin, all others are members.
	seen := map[string]bool{callerID: true}
	participants := make([]model.ChatParticipant, 0, len(req.MemberIDs)+1)
	participants = append(participants, model.ChatParticipant{
		ID: uuid.New().String(), ChatID: chatID, UserID: callerID, Role: "admin", JoinedAt: now,
	})
	for _, memberID := range req.MemberIDs {
		if seen[memberID] {
			continue
		}
		seen[memberID] = true
		participants = append(participants, model.ChatParticipant{
			ID: uuid.New().String(), ChatID: chatID, UserID: memberID, Role: "member", JoinedAt: now,
		})
	}

	if err := s.chatRepo.CreateGroup(ctx, chat, group, participants); err != nil {
		return nil, nil, apperr.NewInternal("failed to create group", err)
	}

	s.publishEvent("chat.created", map[string]interface{}{
		"chat_id": chatID,
		"type":    "group",
		"name":    req.Name,
		"members": append(req.MemberIDs, callerID),
	})

	for _, memberID := range req.MemberIDs {
		if memberID == callerID {
			continue
		}
		s.publishEvent("group.member.added", map[string]interface{}{
			"chat_id":  chatID,
			"user_id":  memberID,
			"added_by": callerID,
		})
	}

	return chat, group, nil
}

func (s *chatServiceImpl) ListChats(ctx context.Context, userID string) ([]*model.ChatListItem, error) {
	chatIDs, err := s.chatRepo.GetUserChats(ctx, userID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get user chats", err)
	}
	if len(chatIDs) == 0 {
		return []*model.ChatListItem{}, nil
	}

	// Fetch last messages from message-service via gRPC (batch). Non-fatal if fails.
	lastMsgsResp, err := s.messageClient.GetLastMessages(ctx, &messagev1.GetLastMessagesRequest{
		ChatIds: chatIDs,
	})
	if err != nil {
		lastMsgsResp = &messagev1.GetLastMessagesResponse{Messages: map[string]*messagev1.MessagePreview{}}
	}

	// Fetch unread counts from message-service via gRPC (batch). Non-fatal if fails.
	unreadResp, err := s.messageClient.GetUnreadCounts(ctx, &messagev1.GetUnreadCountsRequest{
		UserId:  userID,
		ChatIds: chatIDs,
	})
	if err != nil {
		unreadResp = &messagev1.GetUnreadCountsResponse{Counts: map[string]int64{}}
	}

	items := make([]*model.ChatListItem, 0, len(chatIDs))
	for _, chatID := range chatIDs {
		chat, err := s.chatRepo.GetByID(ctx, chatID)
		if err != nil || chat == nil {
			continue
		}

		participants, _ := s.chatRepo.GetParticipants(ctx, chatID)

		item := &model.ChatListItem{
			Chat:         *chat,
			Participants: participants,
			UnreadCount:  unreadResp.Counts[chatID],
		}

		if chat.Type == model.ChatTypeGroup {
			group, _ := s.chatRepo.GetGroup(ctx, chatID)
			item.Group = group
		}

		if preview, ok := lastMsgsResp.Messages[chatID]; ok {
			item.LastMessage = &model.MessagePreview{
				MessageID: preview.MessageId,
				SenderID:  preview.SenderId,
				Type:      preview.Type,
				Body:      preview.Body,
				CreatedAt: preview.CreatedAt.AsTime().UnixMilli(),
			}
		}

		items = append(items, item)
	}

	return items, nil
}

func (s *chatServiceImpl) GetChat(ctx context.Context, callerID, chatID string) (*model.ChatListItem, error) {
	// Verify membership.
	participant, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return nil, apperr.NewInternal("failed to check membership", err)
	}
	if participant == nil {
		return nil, apperr.Wrap(apperr.CodeNotChatMember, 403, "you are not a member of this chat", nil)
	}

	chat, err := s.chatRepo.GetByID(ctx, chatID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get chat", err)
	}
	if chat == nil {
		return nil, apperr.Wrap(apperr.CodeChatNotFound, 404, "chat not found", nil)
	}

	participants, _ := s.chatRepo.GetParticipants(ctx, chatID)

	item := &model.ChatListItem{
		Chat:         *chat,
		Participants: participants,
	}

	if chat.Type == model.ChatTypeGroup {
		group, _ := s.chatRepo.GetGroup(ctx, chatID)
		item.Group = group
	}

	// Enrich with last message.
	lastMsgsResp, err := s.messageClient.GetLastMessages(ctx, &messagev1.GetLastMessagesRequest{
		ChatIds: []string{chatID},
	})
	if err == nil {
		if preview, ok := lastMsgsResp.Messages[chatID]; ok {
			item.LastMessage = &model.MessagePreview{
				MessageID: preview.MessageId,
				SenderID:  preview.SenderId,
				Type:      preview.Type,
				Body:      preview.Body,
				CreatedAt: preview.CreatedAt.AsTime().UnixMilli(),
			}
		}
	}

	// Enrich with unread count.
	unreadResp, err := s.messageClient.GetUnreadCounts(ctx, &messagev1.GetUnreadCountsRequest{
		UserId:  callerID,
		ChatIds: []string{chatID},
	})
	if err == nil {
		item.UnreadCount = unreadResp.Counts[chatID]
	}

	return item, nil
}

const maxGroupMembers = 1024

func (s *chatServiceImpl) AddMember(ctx context.Context, callerID, chatID, targetUserID string) error {
	caller, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return apperr.NewInternal("failed to check caller membership", err)
	}
	if caller == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 403, "you are not a member of this chat", nil)
	}
	if caller.Role != "admin" {
		return apperr.Wrap(apperr.CodeNotAdmin, 403, "only admins can add members", nil)
	}

	participants, err := s.chatRepo.GetParticipants(ctx, chatID)
	if err != nil {
		return apperr.NewInternal("failed to get participants", err)
	}
	if len(participants) >= maxGroupMembers {
		return apperr.NewBadRequest(
			fmt.Sprintf("group cannot exceed %d members", maxGroupMembers),
		)
	}

	existing, err := s.chatRepo.GetParticipant(ctx, chatID, targetUserID)
	if err != nil {
		return apperr.NewInternal("failed to check target membership", err)
	}
	if existing != nil {
		return apperr.Wrap(apperr.CodeAlreadyMember, 409, "user is already a member", nil)
	}

	p := &model.ChatParticipant{
		ID:       uuid.New().String(),
		ChatID:   chatID,
		UserID:   targetUserID,
		Role:     "member",
		JoinedAt: time.Now(),
	}

	if err := s.chatRepo.AddParticipant(ctx, p); err != nil {
		return apperr.NewInternal("failed to add member", err)
	}

	s.publishEvent("group.member.added", map[string]interface{}{
		"chat_id":  chatID,
		"user_id":  targetUserID,
		"added_by": callerID,
	})

	return nil
}

func (s *chatServiceImpl) RemoveMember(ctx context.Context, callerID, chatID, targetUserID string) error {
	caller, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return apperr.NewInternal("failed to check caller membership", err)
	}
	if caller == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 403, "you are not a member of this chat", nil)
	}

	isSelfRemoval := callerID == targetUserID

	if !isSelfRemoval && caller.Role != "admin" {
		return apperr.Wrap(apperr.CodeNotAdmin, 403, "only admins can remove members", nil)
	}

	// If self-removal and caller is admin, check if they're the last admin.
	if isSelfRemoval && caller.Role == "admin" {
		participants, err := s.chatRepo.GetParticipants(ctx, chatID)
		if err != nil {
			return apperr.NewInternal("failed to get participants", err)
		}

		adminCount := 0
		for _, p := range participants {
			if p.Role == "admin" {
				adminCount++
			}
		}

		// If the last admin is leaving, promote the oldest remaining member.
		if adminCount == 1 && len(participants) > 1 {
			var oldest *model.ChatParticipant
			for i := range participants {
				p := &participants[i]
				if p.UserID == callerID {
					continue
				}
				if oldest == nil || p.JoinedAt.Before(oldest.JoinedAt) {
					oldest = p
				}
			}
			if oldest != nil {
				if err := s.chatRepo.UpdateParticipantRole(ctx, chatID, oldest.UserID, "admin"); err != nil {
					return apperr.NewInternal("failed to promote oldest member", err)
				}
			}
		}
	}

	if err := s.chatRepo.RemoveParticipant(ctx, chatID, targetUserID); err != nil {
		return apperr.NewInternal("failed to remove member", err)
	}

	s.publishEvent("group.member.removed", map[string]interface{}{
		"chat_id":    chatID,
		"user_id":    targetUserID,
		"removed_by": callerID,
	})

	return nil
}

func (s *chatServiceImpl) PromoteMember(ctx context.Context, callerID, chatID, targetUserID string) error {
	caller, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return apperr.NewInternal("failed to check caller membership", err)
	}
	if caller == nil || caller.Role != "admin" {
		return apperr.Wrap(apperr.CodeNotAdmin, 403, "only admins can promote members", nil)
	}

	target, err := s.chatRepo.GetParticipant(ctx, chatID, targetUserID)
	if err != nil {
		return apperr.NewInternal("failed to check target membership", err)
	}
	if target == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 404, "target user is not a member", nil)
	}

	if err := s.chatRepo.UpdateParticipantRole(ctx, chatID, targetUserID, "admin"); err != nil {
		return apperr.NewInternal("failed to promote member", err)
	}

	s.publishEvent("chat.updated", map[string]interface{}{
		"chat_id":  chatID,
		"user_id":  targetUserID,
		"new_role": "admin",
	})

	return nil
}

func (s *chatServiceImpl) DemoteMember(ctx context.Context, callerID, chatID, targetUserID string) error {
	caller, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return apperr.NewInternal("failed to check caller membership", err)
	}
	if caller == nil || caller.Role != "admin" {
		return apperr.Wrap(apperr.CodeNotAdmin, 403, "only admins can demote members", nil)
	}

	target, err := s.chatRepo.GetParticipant(ctx, chatID, targetUserID)
	if err != nil {
		return apperr.NewInternal("failed to check target membership", err)
	}
	if target == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 404, "target user is not a member", nil)
	}

	if err := s.chatRepo.UpdateParticipantRole(ctx, chatID, targetUserID, "member"); err != nil {
		return apperr.NewInternal("failed to demote member", err)
	}

	s.publishEvent("chat.updated", map[string]interface{}{
		"chat_id":  chatID,
		"user_id":  targetUserID,
		"new_role": "member",
	})

	return nil
}

func (s *chatServiceImpl) UpdateGroup(ctx context.Context, callerID, chatID string, req *model.UpdateGroupRequest) error {
	caller, err := s.chatRepo.GetParticipant(ctx, chatID, callerID)
	if err != nil {
		return apperr.NewInternal("failed to check caller membership", err)
	}
	if caller == nil || caller.Role != "admin" {
		return apperr.Wrap(apperr.CodeNotAdmin, 403, "only admins can update group settings", nil)
	}

	if err := s.chatRepo.UpdateGroup(ctx, chatID, req); err != nil {
		return apperr.NewInternal("failed to update group", err)
	}

	s.publishEvent("chat.updated", map[string]interface{}{
		"chat_id": chatID,
		"action":  "group_updated",
	})

	return nil
}

func (s *chatServiceImpl) MuteChat(ctx context.Context, userID, chatID string, mute bool, muteUntil *time.Time) error {
	participant, err := s.chatRepo.GetParticipant(ctx, chatID, userID)
	if err != nil {
		return apperr.NewInternal("failed to check membership", err)
	}
	if participant == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 403, "you are not a member of this chat", nil)
	}

	if err := s.chatRepo.UpdateMute(ctx, chatID, userID, mute, muteUntil); err != nil {
		return apperr.NewInternal("failed to update mute", err)
	}

	return nil
}

func (s *chatServiceImpl) PinChat(ctx context.Context, userID, chatID string, pin bool) error {
	participant, err := s.chatRepo.GetParticipant(ctx, chatID, userID)
	if err != nil {
		return apperr.NewInternal("failed to check membership", err)
	}
	if participant == nil {
		return apperr.Wrap(apperr.CodeNotChatMember, 403, "you are not a member of this chat", nil)
	}

	if err := s.chatRepo.UpdatePin(ctx, chatID, userID, pin); err != nil {
		return apperr.NewInternal("failed to update pin", err)
	}

	return nil
}

func (s *chatServiceImpl) UploadGroupAvatar(ctx context.Context, chatID, userID string) (string, error) {
	// Verify user is admin of the group
	isAdmin, err := s.chatRepo.IsAdmin(ctx, chatID, userID)
	if err != nil {
		return "", apperr.NewInternal("failed to check admin status", err)
	}
	if !isAdmin {
		return "", apperr.NewForbidden("only group admins can update the avatar")
	}

	// In production, delegate to media-service for storage
	avatarURL := fmt.Sprintf("/api/v1/media/group-avatars/%s", chatID)

	err = s.chatRepo.UpdateGroupRaw(ctx, chatID, map[string]interface{}{
		"avatar_url": avatarURL,
	})
	if err != nil {
		return "", apperr.NewInternal("failed to update group avatar", err)
	}
	return avatarURL, nil
}

func (s *chatServiceImpl) SetDisappearingMessages(ctx context.Context, chatID, userID string, timer *time.Duration) error {
	// Verify user is a member of the chat
	isMember, err := s.chatRepo.IsMember(ctx, chatID, userID)
	if err != nil {
		return apperr.NewInternal("failed to check membership", err)
	}
	if !isMember {
		return apperr.NewForbidden("not a member of this chat")
	}

	err = s.chatRepo.UpdateAutoDeleteTimer(ctx, chatID, userID, timer)
	if err != nil {
		return apperr.NewInternal("failed to set disappearing messages", err)
	}
	return nil
}
