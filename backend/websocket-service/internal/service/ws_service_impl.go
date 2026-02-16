package service

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nats-io/nats.go"
	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog"

	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
	"github.com/whatsapp-clone/backend/websocket-service/config"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

type wsServiceImpl struct {
	hub           *model.Hub
	rdb           *redis.Client
	nc            *nats.Conn
	js            nats.JetStreamContext
	messageClient messagev1.MessageServiceClient
	chatClient    chatv1.ChatServiceClient
	cfg           *config.Config
	log             zerolog.Logger
	subRegistry     *subscriberRegistry
	presenceTracker *presenceTracker
}

// NewWebSocketService creates a new WebSocketService implementation.
func NewWebSocketService(
	hub *model.Hub,
	rdb *redis.Client,
	nc *nats.Conn,
	js nats.JetStreamContext,
	messageClient messagev1.MessageServiceClient,
	chatClient chatv1.ChatServiceClient,
	cfg *config.Config,
	log zerolog.Logger,
) WebSocketService {
	return &wsServiceImpl{
		hub:           hub,
		rdb:           rdb,
		nc:            nc,
		js:            js,
		messageClient: messageClient,
		chatClient:    chatClient,
		cfg:           cfg,
		log:           log,
		subRegistry:     newSubscriberRegistry(),
		presenceTracker: newPresenceTracker(),
	}
}

// SendToUser sends an event to all local connections for a given user.
func (s *wsServiceImpl) SendToUser(userID string, event *model.WSEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("marshal event: %w", err)
	}

	clients := s.hub.GetClients(userID)
	for _, c := range clients {
		select {
		case c.Send <- data:
		default:
			s.log.Warn().Str("user_id", userID).Msg("client send buffer full, dropping message")
		}
	}
	return nil
}

// getChatParticipants resolves chat participants via chat-service gRPC with Redis caching.
func (s *wsServiceImpl) getChatParticipants(ctx context.Context, chatID string) []string {
	cacheKey := "chat:participants:" + chatID

	cached, err := s.rdb.Get(ctx, cacheKey).Result()
	if err == nil {
		var ids []string
		if json.Unmarshal([]byte(cached), &ids) == nil && len(ids) > 0 {
			return ids
		}
	}

	resp, err := s.chatClient.GetChatParticipants(ctx, &chatv1.GetChatParticipantsRequest{
		ChatId: chatID,
	})
	if err != nil {
		s.log.Error().Err(err).Str("chat_id", chatID).Msg("failed to get chat participants")
		return nil
	}

	if len(resp.UserIds) > 0 {
		if data, err := json.Marshal(resp.UserIds); err == nil {
			s.rdb.Set(ctx, cacheKey, data, 5*time.Minute)
		}
	}

	return resp.UserIds
}
