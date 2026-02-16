package handler

import (
	"context"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
	"github.com/whatsapp-clone/backend/message-service/internal/service"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
)

type GRPCHandler struct {
	messagev1.UnimplementedMessageServiceServer
	msgSvc service.MessageService
}

func NewGRPCHandler(msgSvc service.MessageService) *GRPCHandler {
	return &GRPCHandler{msgSvc: msgSvc}
}

func (h *GRPCHandler) SendMessage(ctx context.Context, req *messagev1.SendMessageRequest) (*messagev1.SendMessageResponse, error) {
	sendReq := &model.SendMessageRequest{
		ChatID:           req.ChatId,
		Type:             model.MessageType(req.Type),
		ClientMsgID:      req.ClientMsgId,
		ReplyToMessageID: req.ReplyToMessageId,
		Payload: model.MessagePayload{
			Body:       req.Payload.GetBody(),
			MediaID:    req.Payload.GetMediaId(),
			Caption:    req.Payload.GetCaption(),
			Filename:   req.Payload.GetFilename(),
			DurationMs: req.Payload.GetDurationMs(),
		},
	}
	if req.ForwardedFrom != nil {
		sendReq.ForwardedFrom = &model.ForwardedFrom{
			ChatID:    req.ForwardedFrom.ChatId,
			MessageID: req.ForwardedFrom.MessageId,
		}
	}

	msg, err := h.msgSvc.SendMessage(ctx, req.SenderId, sendReq)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}

	return &messagev1.SendMessageResponse{
		MessageId: msg.MessageID,
		CreatedAt: timestamppb.New(msg.CreatedAt),
	}, nil
}

func (h *GRPCHandler) UpdateMessageStatus(ctx context.Context, req *messagev1.UpdateMessageStatusRequest) (*messagev1.UpdateMessageStatusResponse, error) {
	err := h.msgSvc.UpdateStatus(ctx, req.MessageId, req.UserId, req.Status)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	return &messagev1.UpdateMessageStatusResponse{Success: true}, nil
}

func (h *GRPCHandler) GetLastMessages(ctx context.Context, req *messagev1.GetLastMessagesRequest) (*messagev1.GetLastMessagesResponse, error) {
	msgs, err := h.msgSvc.GetLastMessages(ctx, req.ChatIds)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}

	result := make(map[string]*messagev1.MessagePreview, len(msgs))
	for chatID, msg := range msgs {
		body := msg.Payload.Body
		if len(body) > 100 {
			body = body[:100]
		}
		if body == "" {
			body = "[" + string(msg.Type) + "]"
		}
		result[chatID] = &messagev1.MessagePreview{
			MessageId: msg.MessageID,
			SenderId:  msg.SenderID,
			Type:      string(msg.Type),
			Body:      body,
			CreatedAt: timestamppb.New(msg.CreatedAt),
		}
	}

	return &messagev1.GetLastMessagesResponse{Messages: result}, nil
}

func (h *GRPCHandler) GetUnreadCounts(ctx context.Context, req *messagev1.GetUnreadCountsRequest) (*messagev1.GetUnreadCountsResponse, error) {
	counts, err := h.msgSvc.GetUnreadCounts(ctx, req.UserId, req.ChatIds)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	return &messagev1.GetUnreadCountsResponse{Counts: counts}, nil
}
