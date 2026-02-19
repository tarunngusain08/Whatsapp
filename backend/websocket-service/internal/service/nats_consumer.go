package service

import (
	"context"
	"encoding/json"
	"strings"
	"time"

	"github.com/nats-io/nats.go"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

// ensureStreams creates MESSAGES and CHATS JetStream streams if they do not exist.
// This makes the websocket-service resilient to startup ordering.
func (s *wsServiceImpl) ensureStreams() error {
	streams := []struct {
		name     string
		subjects []string
	}{
		{name: "MESSAGES", subjects: []string{"msg.>"}},
		{name: "CHATS", subjects: []string{"chat.>", "group.>"}},
	}
	for _, st := range streams {
		info, _ := s.js.StreamInfo(st.name)
		if info != nil {
			continue
		}
		_, err := s.js.AddStream(&nats.StreamConfig{
			Name:     st.name,
			Subjects: st.subjects,
		})
		if err != nil {
			return err
		}
		s.log.Info().Str("stream", st.name).Msg("created JetStream stream")
	}
	return nil
}

// StartNATSConsumers subscribes to NATS JetStream subjects for real-time delivery.
func (s *wsServiceImpl) StartNATSConsumers(ctx context.Context) error {
	if err := s.ensureStreams(); err != nil {
		return err
	}
	if err := s.subscribeNewMessages(ctx); err != nil {
		return err
	}
	if err := s.subscribeStatusUpdates(ctx); err != nil {
		return err
	}
	if err := s.subscribeDeletedMessages(ctx); err != nil {
		return err
	}
	if err := s.subscribeChatAndGroupEvents(ctx); err != nil {
		return err
	}
	return nil
}

// subscribeNewMessages handles msg.new — delivers new messages to chat participants.
func (s *wsServiceImpl) subscribeNewMessages(ctx context.Context) error {
	_, err := s.js.Subscribe("msg.new", func(m *nats.Msg) {
		var event struct {
			MessageID string `json:"message_id"`
			ChatID    string `json:"chat_id"`
			SenderID  string `json:"sender_id"`
			Type      string `json:"type"`
			Payload   struct {
				Body       string `json:"body"`
				MediaID    string `json:"media_id"`
				Caption    string `json:"caption"`
				Filename   string `json:"filename"`
				DurationMs int64  `json:"duration_ms"`
			} `json:"payload"`
			CreatedAt time.Time `json:"created_at"`
		}
		if err := json.Unmarshal(m.Data, &event); err != nil {
			s.log.Error().Err(err).Msg("failed to unmarshal msg.new")
			_ = m.Nak()
			return
		}

		wsEvent := model.WSEvent{Type: "message.new"}
		wsEvent.Payload, _ = json.Marshal(model.MessageNewPayload{
			MessageID: event.MessageID,
			ChatID:    event.ChatID,
			SenderID:  event.SenderID,
			Type:      event.Type,
			Payload: model.MessageContent{
				Body:       event.Payload.Body,
				MediaID:    event.Payload.MediaID,
				Caption:    event.Payload.Caption,
				Filename:   event.Payload.Filename,
				DurationMs: event.Payload.DurationMs,
			},
			CreatedAt: event.CreatedAt.UnixMilli(),
		})

		data, _ := json.Marshal(wsEvent)
		participantIDs := s.getChatParticipants(ctx, event.ChatID)
		for _, uid := range participantIDs {
			s.rdb.Publish(ctx, "user:channel:"+uid, data)
		}

		_ = m.Ack()
	}, nats.Durable("ws-msg-consumer"), nats.ManualAck())

	if err != nil {
		return err
	}
	s.log.Info().Msg("subscribed to msg.new")
	return nil
}

// subscribeStatusUpdates handles msg.status.updated — notifies the original sender.
func (s *wsServiceImpl) subscribeStatusUpdates(ctx context.Context) error {
	_, err := s.js.Subscribe("msg.status.updated", func(m *nats.Msg) {
		var event struct {
			MessageID string `json:"message_id"`
			ChatID    string `json:"chat_id"`
			UserID    string `json:"user_id"`
			SenderID  string `json:"sender_id"`
			Status    string `json:"status"`
		}
		if err := json.Unmarshal(m.Data, &event); err != nil {
			s.log.Error().Err(err).Msg("failed to unmarshal msg.status.updated")
			_ = m.Nak()
			return
		}

		wsEvent := model.WSEvent{Type: "message.status"}
		wsEvent.Payload, _ = json.Marshal(map[string]string{
			"message_id": event.MessageID,
			"chat_id":    event.ChatID,
			"user_id":    event.UserID,
			"status":     event.Status,
		})
		data, _ := json.Marshal(wsEvent)
		s.rdb.Publish(context.Background(), "user:channel:"+event.SenderID, data)

		_ = m.Ack()
	}, nats.Durable("ws-status-consumer"), nats.ManualAck())

	if err != nil {
		return err
	}
	s.log.Info().Msg("subscribed to msg.status.updated")
	return nil
}

// subscribeDeletedMessages handles msg.deleted — routes deletion event to chat participants.
func (s *wsServiceImpl) subscribeDeletedMessages(ctx context.Context) error {
	_, err := s.js.Subscribe("msg.deleted", func(m *nats.Msg) {
		var event struct {
			MessageID   string `json:"message_id"`
			ChatID      string `json:"chat_id"`
			UserID      string `json:"user_id"`
			ForEveryone bool   `json:"for_everyone"`
		}
		if err := json.Unmarshal(m.Data, &event); err != nil {
			s.log.Error().Err(err).Msg("failed to unmarshal msg.deleted")
			_ = m.Nak()
			return
		}

		wsEvent := model.WSEvent{Type: "message.deleted"}
		wsEvent.Payload, _ = json.Marshal(map[string]string{
			"message_id": event.MessageID,
			"user_id":    event.UserID,
		})
		data, _ := json.Marshal(wsEvent)

		if event.ForEveryone {
			participantIDs := s.getChatParticipants(ctx, event.ChatID)
			for _, uid := range participantIDs {
				s.rdb.Publish(context.Background(), "user:channel:"+uid, data)
			}
		} else {
			s.rdb.Publish(context.Background(), "user:channel:"+event.UserID, data)
		}

		_ = m.Ack()
	}, nats.Durable("ws-delete-consumer"), nats.ManualAck())

	if err != nil {
		return err
	}
	s.log.Info().Msg("subscribed to msg.deleted")
	return nil
}

// subscribeChatAndGroupEvents handles chat.created, chat.updated, group.member.added/removed.
func (s *wsServiceImpl) subscribeChatAndGroupEvents(_ context.Context) error {
	subjects := []string{"chat.created", "chat.updated", "group.member.added", "group.member.removed"}
	for _, subj := range subjects {
		subject := subj
		_, err := s.js.Subscribe(subject, func(m *nats.Msg) {
			var event map[string]interface{}
			if err := json.Unmarshal(m.Data, &event); err != nil {
				s.log.Error().Err(err).Str("subject", subject).Msg("failed to unmarshal event")
				_ = m.Nak()
				return
			}

			wsEvent := model.WSEvent{Type: subject}
			wsEvent.Payload = m.Data

			if members, ok := event["participants"].([]interface{}); ok {
				data, _ := json.Marshal(wsEvent)
				for _, mid := range members {
					if uid, ok := mid.(string); ok {
						s.rdb.Publish(context.Background(), "user:channel:"+uid, data)
					}
				}
			}

			_ = m.Ack()
		}, nats.Durable("ws-"+strings.ReplaceAll(subject, ".", "-")+"-consumer"), nats.ManualAck())

		if err != nil {
			return err
		}
		s.log.Info().Str("subject", subject).Msg("subscribed to NATS subject")
	}

	return nil
}
