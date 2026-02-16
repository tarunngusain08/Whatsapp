package handler

import (
	"context"

	"github.com/rs/zerolog"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/whatsapp-clone/backend/chat-service/internal/repository"
	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
)

type GRPCHandler struct {
	chatv1.UnimplementedChatServiceServer
	chatRepo repository.ChatRepository
	log      zerolog.Logger
}

func NewGRPCHandler(chatRepo repository.ChatRepository, log zerolog.Logger) *GRPCHandler {
	return &GRPCHandler{chatRepo: chatRepo, log: log}
}

func (h *GRPCHandler) GetChatParticipants(ctx context.Context, req *chatv1.GetChatParticipantsRequest) (*chatv1.GetChatParticipantsResponse, error) {
	participants, err := h.chatRepo.GetParticipants(ctx, req.ChatId)
	if err != nil {
		h.log.Error().Err(err).Str("chat_id", req.ChatId).Msg("failed to get participants")
		return nil, status.Error(codes.Internal, "failed to get participants")
	}

	userIDs := make([]string, 0, len(participants))
	for _, p := range participants {
		userIDs = append(userIDs, p.UserID)
	}

	return &chatv1.GetChatParticipantsResponse{UserIds: userIDs}, nil
}

func (h *GRPCHandler) IsMember(ctx context.Context, req *chatv1.IsMemberRequest) (*chatv1.IsMemberResponse, error) {
	participant, err := h.chatRepo.GetParticipant(ctx, req.ChatId, req.UserId)
	if err != nil {
		h.log.Error().Err(err).
			Str("chat_id", req.ChatId).
			Str("user_id", req.UserId).
			Msg("failed to check membership")
		return nil, status.Error(codes.Internal, "failed to check membership")
	}

	return &chatv1.IsMemberResponse{IsMember: participant != nil}, nil
}

func (h *GRPCHandler) CheckChatPermission(ctx context.Context, req *chatv1.CheckChatPermissionRequest) (*chatv1.CheckChatPermissionResponse, error) {
	resp := &chatv1.CheckChatPermissionResponse{}

	// Check membership
	isMember, err := h.chatRepo.IsMember(ctx, req.ChatId, req.UserId)
	if err != nil {
		h.log.Error().Err(err).
			Str("chat_id", req.ChatId).
			Str("user_id", req.UserId).
			Msg("failed to check membership")
		return nil, status.Errorf(codes.Internal, "failed to check membership: %v", err)
	}
	resp.IsMember = isMember

	if !isMember {
		return resp, nil
	}

	// Get chat details
	chat, err := h.chatRepo.GetByID(ctx, req.ChatId)
	if err != nil {
		h.log.Error().Err(err).Str("chat_id", req.ChatId).Msg("failed to get chat")
		return nil, status.Errorf(codes.Internal, "failed to get chat: %v", err)
	}

	resp.ChatType = string(chat.Type)

	if chat.Type == "group" {
		group, err := h.chatRepo.GetGroup(ctx, req.ChatId)
		if err == nil && group != nil {
			resp.IsAdminOnly = group.IsAdminOnly
		}
	}

	// Check admin status
	isAdmin, err := h.chatRepo.IsAdmin(ctx, req.ChatId, req.UserId)
	if err != nil {
		h.log.Warn().Err(err).
			Str("chat_id", req.ChatId).
			Str("user_id", req.UserId).
			Msg("failed to check admin status, defaulting to false")
		resp.IsAdmin = false
	} else {
		resp.IsAdmin = isAdmin
	}

	return resp, nil
}
