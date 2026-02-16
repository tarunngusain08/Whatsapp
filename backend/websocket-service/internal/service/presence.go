package service

import (
	"context"
	"fmt"
	"time"

	"encoding/json"

	"github.com/gorilla/websocket"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

// SetPresence updates a user's online presence in Redis.
func (s *wsServiceImpl) SetPresence(ctx context.Context, userID string, online bool) error {
	key := "presence:" + userID

	if online {
		return s.rdb.SetEx(ctx, key, "online", s.cfg.PresenceTTL).Err()
	}

	pipe := s.rdb.Pipeline()
	pipe.Del(ctx, key)
	pipe.Set(ctx, fmt.Sprintf("last_seen:%s", userID), time.Now().UnixMilli(), 0)
	_, err := pipe.Exec(ctx)
	return err
}

// NotifyPresenceChange notifies all subscribers when a user's presence changes.
func (s *wsServiceImpl) NotifyPresenceChange(userID string, online bool) {
	subscribers := s.presenceTracker.GetSubscribers(userID)
	if len(subscribers) == 0 {
		return
	}

	event := model.WSEvent{Type: "presence"}
	event.Payload, _ = json.Marshal(model.PresenceEventPayload{
		UserID: userID,
		Online: online,
	})

	for _, subID := range subscribers {
		s.SendToUser(subID, &event)
	}
}

// CleanupPresenceSubscriptions removes all presence subscriptions for a user (e.g., on disconnect).
func (s *wsServiceImpl) CleanupPresenceSubscriptions(userID string) {
	s.presenceTracker.Unsubscribe(userID)
}

// GracefulShutdown closes all active WebSocket connections and Redis pub-sub subscriptions.
func (s *wsServiceImpl) GracefulShutdown() {
	for _, uid := range s.hub.AllUserIDs() {
		for _, client := range s.hub.GetClients(uid) {
			_ = client.Conn.WriteMessage(
				websocket.CloseMessage,
				websocket.FormatCloseMessage(websocket.CloseGoingAway, "server shutting down"),
			)
			client.Conn.Close()
		}
	}

	s.subRegistry.mu.Lock()
	for _, pubsub := range s.subRegistry.subs {
		pubsub.Close()
	}
	s.subRegistry.mu.Unlock()

	s.log.Info().Msg("websocket service shut down gracefully")
}
