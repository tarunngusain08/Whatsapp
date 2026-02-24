package service

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nats-io/nats.go"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/notification-service/internal/model"
	"github.com/whatsapp-clone/backend/notification-service/internal/repository"
)

// Consumer listens on NATS JetStream subjects and orchestrates push notification delivery.
type Consumer struct {
	js           nats.JetStreamContext
	presenceRepo repository.PresenceRepository
	muteRepo     repository.ParticipantRepository
	tokenRepo    repository.DeviceTokenRepository
	userRepo     repository.UserRepository
	fcmClient    FCMClient
	batcher      *NotificationBatcher
	log          zerolog.Logger
}

func NewConsumer(
	js nats.JetStreamContext,
	presenceRepo repository.PresenceRepository,
	muteRepo repository.ParticipantRepository,
	tokenRepo repository.DeviceTokenRepository,
	userRepo repository.UserRepository,
	fcmClient FCMClient,
	batcher *NotificationBatcher,
	log zerolog.Logger,
) *Consumer {
	return &Consumer{
		js:           js,
		presenceRepo: presenceRepo,
		muteRepo:     muteRepo,
		tokenRepo:    tokenRepo,
		userRepo:     userRepo,
		fcmClient:    fcmClient,
		batcher:      batcher,
		log:          log,
	}
}

// ensureStreams creates MESSAGES and CHATS JetStream streams if they do not exist.
func (c *Consumer) ensureStreams() error {
	streams := []struct {
		name     string
		subjects []string
	}{
		{name: "MESSAGES", subjects: []string{"msg.>"}},
		{name: "CHATS", subjects: []string{"chat.>", "group.>"}},
		{name: "CALLS", subjects: []string{"call.>"}},
	}
	for _, st := range streams {
		info, _ := c.js.StreamInfo(st.name)
		if info != nil {
			continue
		}
		_, err := c.js.AddStream(&nats.StreamConfig{
			Name:     st.name,
			Subjects: st.subjects,
		})
		if err != nil {
			return err
		}
		c.log.Info().Str("stream", st.name).Msg("created JetStream stream")
	}
	return nil
}

// Start subscribes to NATS subjects and blocks until context is cancelled.
func (c *Consumer) Start(ctx context.Context) error {
	if err := c.ensureStreams(); err != nil {
		return err
	}
	if err := c.subscribeMessageEvents(ctx); err != nil {
		return err
	}
	if err := c.subscribeMemberEvents(ctx); err != nil {
		return err
	}
	if err := c.subscribeCallEvents(ctx); err != nil {
		return err
	}

	c.log.Info().Msg("NATS consumers started")
	<-ctx.Done()
	return nil
}

// subscribeMessageEvents sets up a durable consumer for msg.new events.
func (c *Consumer) subscribeMessageEvents(ctx context.Context) error {
	_, err := c.js.Subscribe("msg.new", func(natsMsg *nats.Msg) {
		var event model.MessageEvent
		if err := json.Unmarshal(natsMsg.Data, &event); err != nil {
			c.log.Error().Err(err).Msg("failed to unmarshal msg.new event")
			_ = natsMsg.Nak()
			return
		}

		if err := c.handleNewMessage(ctx, &event); err != nil {
			c.log.Error().Err(err).
				Str("message_id", event.MessageID).
				Msg("failed to handle msg.new notification")
			_ = natsMsg.Nak()
			return
		}
		_ = natsMsg.Ack()
	}, nats.Durable("notif-msg-consumer"), nats.ManualAck(), nats.AckWait(30*time.Second))

	if err != nil {
		return fmt.Errorf("subscribe to msg.new: %w", err)
	}
	return nil
}

// subscribeMemberEvents sets up a durable consumer for group.member.added events.
func (c *Consumer) subscribeMemberEvents(ctx context.Context) error {
	_, err := c.js.Subscribe("group.member.added", func(natsMsg *nats.Msg) {
		var event model.MemberEvent
		if err := json.Unmarshal(natsMsg.Data, &event); err != nil {
			c.log.Error().Err(err).Msg("failed to unmarshal group.member.added event")
			_ = natsMsg.Nak()
			return
		}

		if err := c.handleGroupMemberAdded(ctx, &event); err != nil {
			c.log.Error().Err(err).
				Str("chat_id", event.ChatID).
				Str("user_id", event.UserID).
				Msg("failed to handle group.member.added notification")
			_ = natsMsg.Nak()
			return
		}
		_ = natsMsg.Ack()
	}, nats.Durable("notif-group-consumer"), nats.ManualAck(), nats.AckWait(30*time.Second))

	if err != nil {
		return fmt.Errorf("subscribe to group.member.added: %w", err)
	}
	return nil
}

// handleNewMessage processes a single message event and sends push notifications
// to all offline, non-muted recipients.
func (c *Consumer) handleNewMessage(ctx context.Context, event *model.MessageEvent) error {
	body := event.Payload.Body
	if body == "" && event.Payload.Caption != "" {
		body = event.Payload.Caption
	}
	chatType := "direct"
	if event.IsGroup {
		chatType = "group"
	}
	chatName := event.ChatName
	if chatName == "" {
		chatName = event.SenderName
	}

	for _, recipientID := range event.ParticipantIDs {
		if recipientID == event.SenderID {
			continue
		}

		online, err := c.presenceRepo.IsOnline(ctx, recipientID)
		if err != nil {
			c.log.Warn().Err(err).Str("user_id", recipientID).Msg("presence check failed, proceeding with push")
		} else if online {
			continue
		}

		muted, err := c.muteRepo.IsMuted(ctx, event.ChatID, recipientID)
		if err != nil {
			c.log.Warn().Err(err).Str("user_id", recipientID).Msg("mute check failed, proceeding with push")
		} else if muted {
			continue
		}

		payload := map[string]string{
			"type":        "message",
			"chatId":      event.ChatID,
			"messageId":   event.MessageID,
			"senderId":    event.SenderID,
			"senderName":  event.SenderName,
			"content":     truncate(body, 200),
			"messageType": event.Type,
			"chatType":    chatType,
			"chatName":    chatName,
			"avatarUrl":   event.SenderAvatar,
			"timestamp":   fmt.Sprintf("%d", event.CreatedAt),
		}

		if event.IsGroup {
			c.batcher.Add(recipientID, event.ChatID, chatName, payload)
		} else {
			c.sendPushToUser(ctx, recipientID, event.SenderName, truncate(body, 200), payload)
		}
	}
	return nil
}

// handleGroupMemberAdded sends a "You were added to GroupName" push to the new member.
func (c *Consumer) handleGroupMemberAdded(ctx context.Context, event *model.MemberEvent) error {
	// Skip if user is online.
	online, err := c.presenceRepo.IsOnline(ctx, event.UserID)
	if err == nil && online {
		return nil
	}

	title := event.GroupName
	if title == "" {
		title = "Group Chat"
	}
	body := fmt.Sprintf("You were added to %s", title)

	data := map[string]string{
		"type":    "group_invite",
		"chat_id": event.ChatID,
		"body":    body,
	}

	c.sendPushToUser(ctx, event.UserID, title, body, data)
	return nil
}

// subscribeCallEvents sets up a durable consumer for call.new events.
func (c *Consumer) subscribeCallEvents(ctx context.Context) error {
	_, err := c.js.Subscribe("call.new", func(natsMsg *nats.Msg) {
		var event model.CallEvent
		if err := json.Unmarshal(natsMsg.Data, &event); err != nil {
			c.log.Error().Err(err).Msg("failed to unmarshal call.new event")
			_ = natsMsg.Nak()
			return
		}

		if err := c.handleNewCall(ctx, &event); err != nil {
			c.log.Error().Err(err).
				Str("call_id", event.CallID).
				Msg("failed to handle call.new notification")
			_ = natsMsg.Nak()
			return
		}
		_ = natsMsg.Ack()
	}, nats.Durable("notif-call-consumer"), nats.ManualAck(), nats.AckWait(30*time.Second))

	if err != nil {
		return fmt.Errorf("subscribe to call.new: %w", err)
	}
	return nil
}

// handleNewCall sends a push notification for an incoming call to an offline user.
func (c *Consumer) handleNewCall(ctx context.Context, event *model.CallEvent) error {
	callerName := event.CallerName
	avatarURL := event.AvatarURL

	if callerName == "" {
		name, avatar, err := c.userRepo.GetDisplayName(ctx, event.CallerID)
		if err != nil {
			c.log.Warn().Err(err).Str("caller_id", event.CallerID).Msg("failed to resolve caller name")
			callerName = "Unknown"
		} else {
			callerName = name
			if avatarURL == "" {
				avatarURL = avatar
			}
		}
	}

	callLabel := "Incoming voice call"
	if event.CallType == "video" {
		callLabel = "Incoming video call"
	}

	payload := map[string]string{
		"type":       "call",
		"callId":     event.CallID,
		"callerId":   event.CallerID,
		"callerName": callerName,
		"callType":   event.CallType,
		"avatarUrl":  avatarURL,
	}

	c.sendPushToUser(ctx, event.TargetID, callerName, callLabel, payload)
	return nil
}

// sendPushToUser retrieves device tokens for a user and sends push notifications.
func (c *Consumer) sendPushToUser(ctx context.Context, userID, title, body string, data map[string]string) {
	tokens, err := c.tokenRepo.GetByUserID(ctx, userID)
	if err != nil {
		c.log.Error().Err(err).Str("user_id", userID).Msg("failed to get device tokens")
		return
	}

	if len(tokens) == 0 {
		c.log.Debug().Str("user_id", userID).Msg("no device tokens found, skipping push")
		return
	}

	for _, token := range tokens {
		payload := &model.NotificationPayload{
			Title: title,
			Body:  body,
			Data:  data,
			Token: token,
		}

		if err := c.fcmClient.Send(ctx, payload); err != nil {
			c.log.Error().Err(err).
				Str("user_id", userID).
				Str("token_prefix", token[:min(len(token), 12)]).
				Msg("failed to send push notification")
		}
	}
}

// truncate shortens s to maxLen, appending "..." if truncated.
func truncate(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	return s[:maxLen-3] + "..."
}
