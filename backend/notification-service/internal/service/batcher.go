package service

import (
	"context"
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/notification-service/internal/model"
	"github.com/whatsapp-clone/backend/notification-service/internal/repository"
)

// groupBuffer accumulates notifications for a single (userID, chatID) pair.
type groupBuffer struct {
	payloads []map[string]string
	chatName string
	timer    *time.Timer
}

// NotificationBatcher batches group chat notifications per (user, chat) with
// a configurable debounce window. When the timer fires, it collapses N messages
// into a single summary push notification.
type NotificationBatcher struct {
	mu             sync.Mutex
	buffers        map[string]*groupBuffer
	debounceWindow time.Duration
	fcmClient      FCMClient
	tokenRepo      repository.DeviceTokenRepository
	log            zerolog.Logger
}

func NewNotificationBatcher(
	debounceWindow time.Duration,
	fcmClient FCMClient,
	tokenRepo repository.DeviceTokenRepository,
	log zerolog.Logger,
) *NotificationBatcher {
	return &NotificationBatcher{
		buffers:        make(map[string]*groupBuffer),
		debounceWindow: debounceWindow,
		fcmClient:      fcmClient,
		tokenRepo:      tokenRepo,
		log:            log,
	}
}

// Add buffers a group notification for the given user and chat. On the first
// message the debounce timer starts; subsequent messages reset the timer. When
// the timer fires, all buffered messages are collapsed into one push.
func (b *NotificationBatcher) Add(userID, chatID, chatName string, payload map[string]string) {
	key := userID + ":" + chatID

	b.mu.Lock()
	defer b.mu.Unlock()

	buf, exists := b.buffers[key]
	if !exists {
		buf = &groupBuffer{
			payloads: []map[string]string{},
			chatName: chatName,
		}
		b.buffers[key] = buf
	}

	buf.payloads = append(buf.payloads, payload)

	// Reset timer on each new message.
	if buf.timer != nil {
		buf.timer.Stop()
	}
	buf.timer = time.AfterFunc(b.debounceWindow, func() {
		b.flush(userID, chatID, key)
	})
}

// flush collapses all buffered messages for a key into a single push notification.
func (b *NotificationBatcher) flush(userID, chatID, key string) {
	b.mu.Lock()
	buf, exists := b.buffers[key]
	if !exists {
		b.mu.Unlock()
		return
	}
	delete(b.buffers, key)
	b.mu.Unlock()

	count := len(buf.payloads)
	body := fmt.Sprintf("%d new messages", count)
	if count == 1 {
		body = "1 new message"
	}

	title := buf.chatName
	if title == "" {
		title = "Group Chat"
	}

	tokens, err := b.tokenRepo.GetByUserID(context.Background(), userID)
	if err != nil {
		b.log.Error().Err(err).Str("user_id", userID).Msg("failed to get device tokens for batch flush")
		return
	}

	for _, token := range tokens {
		payload := &model.NotificationPayload{
			Title: title,
			Body:  body,
			Token: token,
			Data: map[string]string{
				"type":    "message",
				"chat_id": chatID,
				"body":    body,
			},
		}

		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		if err := b.fcmClient.Send(ctx, payload); err != nil {
			b.log.Error().Err(err).
				Str("user_id", userID).
				Str("chat_id", chatID).
				Msg("failed to send batched group notification")
		}
		cancel()
	}

	b.log.Info().
		Str("user_id", userID).
		Str("chat_id", chatID).
		Int("count", count).
		Msg("flushed batched group notification")
}

// Shutdown flushes pending notifications and stops all timers.
func (b *NotificationBatcher) Shutdown() {
	b.mu.Lock()
	keys := make([]string, 0, len(b.buffers))
	for key, buf := range b.buffers {
		if buf.timer != nil {
			buf.timer.Stop()
		}
		keys = append(keys, key)
	}
	b.mu.Unlock()

	for _, key := range keys {
		parts := strings.SplitN(key, ":", 2)
		if len(parts) == 2 {
			b.flush(parts[0], parts[1], key)
		}
	}
}
