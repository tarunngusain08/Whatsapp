package service

import (
	"context"

	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

// WebSocketService defines the contract for real-time WebSocket event handling.
type WebSocketService interface {
	// HandleEvent processes a client->server WebSocket event.
	HandleEvent(ctx context.Context, client *model.Client, event *model.WSEvent) error

	// SendToUser sends an event to all connected clients for a user on this instance.
	SendToUser(userID string, event *model.WSEvent) error

	// SetPresence updates the user's online presence in Redis.
	SetPresence(ctx context.Context, userID string, online bool) error

	// StartNATSConsumers starts consuming events from NATS JetStream for real-time delivery.
	StartNATSConsumers(ctx context.Context) error

	// StartRedisSubscriber starts a Redis pub-sub subscriber for the given client.
	StartRedisSubscriber(ctx context.Context, client *model.Client) error

	// StopRedisSubscriber stops the Redis pub-sub subscriber for the given client.
	StopRedisSubscriber(client *model.Client) error

	// NotifyPresenceChange notifies all subscribers when a user's presence changes.
	NotifyPresenceChange(userID string, online bool)

	// CleanupPresenceSubscriptions removes all presence subscriptions for a user.
	CleanupPresenceSubscriptions(userID string)

	// GracefulShutdown closes all WebSocket connections and Redis subscriptions.
	GracefulShutdown()
}

// AuthValidator validates tokens via the auth-service gRPC.
type AuthValidator interface {
	ValidateToken(ctx context.Context, token string) (userID, phone string, err error)
}
