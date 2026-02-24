package service

import (
	"context"
	"encoding/json"

	"github.com/nats-io/nats.go"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
)

type EventPublisher struct {
	js  nats.JetStreamContext
	log zerolog.Logger
}

func NewEventPublisher(js nats.JetStreamContext, log zerolog.Logger) *EventPublisher {
	return &EventPublisher{js: js, log: log}
}

// EnsureStream creates the MESSAGES stream if it does not already exist.
func (p *EventPublisher) EnsureStream() error {
	stream, _ := p.js.StreamInfo("MESSAGES")
	if stream != nil {
		return nil
	}
	_, err := p.js.AddStream(&nats.StreamConfig{
		Name:     "MESSAGES",
		Subjects: []string{"msg.>"},
	})
	return err
}

// MessageEnrichment holds additional metadata resolved at send time so
// downstream consumers (notification-service, websocket-service) can
// build rich push notifications without extra lookups.
type MessageEnrichment struct {
	SenderName     string   `json:"sender_name"`
	SenderAvatar   string   `json:"sender_avatar"`
	ChatName       string   `json:"chat_name"`
	IsGroup        bool     `json:"is_group"`
	ParticipantIDs []string `json:"participant_ids"`
}

// PublishNewMessage publishes a msg.new event for real-time delivery.
func (p *EventPublisher) PublishNewMessage(ctx context.Context, msg *model.Message, enrichment *MessageEnrichment) error {
	payload := map[string]interface{}{
		"message_id": msg.MessageID,
		"chat_id":    msg.ChatID,
		"sender_id":  msg.SenderID,
		"type":       msg.Type,
		"payload":    msg.Payload,
		"created_at": msg.CreatedAt.UnixMilli(),
	}
	if enrichment != nil {
		payload["sender_name"] = enrichment.SenderName
		payload["sender_avatar"] = enrichment.SenderAvatar
		payload["chat_name"] = enrichment.ChatName
		payload["is_group"] = enrichment.IsGroup
		payload["participant_ids"] = enrichment.ParticipantIDs
	}
	data, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	_, err = p.js.Publish("msg.new", data)
	return err
}

// PublishStatusUpdate publishes a msg.status.updated event.
func (p *EventPublisher) PublishStatusUpdate(ctx context.Context, msgID, chatID, userID, status, senderID string) error {
	data, err := json.Marshal(map[string]string{
		"message_id": msgID,
		"chat_id":    chatID,
		"user_id":    userID,
		"status":     status,
		"sender_id":  senderID,
	})
	if err != nil {
		return err
	}
	_, err = p.js.Publish("msg.status.updated", data)
	return err
}

// PublishReaction publishes a msg.reaction event for real-time delivery.
func (p *EventPublisher) PublishReaction(ctx context.Context, messageID, chatID, userID, emoji string, removed bool) error {
	data, err := json.Marshal(map[string]interface{}{
		"message_id": messageID,
		"chat_id":    chatID,
		"user_id":    userID,
		"emoji":      emoji,
		"removed":    removed,
	})
	if err != nil {
		return err
	}
	_, err = p.js.Publish("msg.reaction", data)
	return err
}

// PublishMessageDeleted publishes a msg.deleted event.
func (p *EventPublisher) PublishMessageDeleted(ctx context.Context, msgID, chatID, userID string, forEveryone bool) error {
	data, err := json.Marshal(map[string]interface{}{
		"message_id":   msgID,
		"chat_id":      chatID,
		"user_id":      userID,
		"for_everyone": forEveryone,
	})
	if err != nil {
		return err
	}
	_, err = p.js.Publish("msg.deleted", data)
	return err
}
