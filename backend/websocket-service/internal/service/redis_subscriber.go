package service

import (
	"context"
	"sync"

	"github.com/redis/go-redis/v9"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

// subscriberRegistry tracks per-client Redis pub-sub subscriptions.
type subscriberRegistry struct {
	mu   sync.Mutex
	subs map[*model.Client]*redis.PubSub
}

func newSubscriberRegistry() *subscriberRegistry {
	return &subscriberRegistry{subs: make(map[*model.Client]*redis.PubSub)}
}

// StartRedisSubscriber starts a Redis pub-sub subscriber for the given client's user channel.
func (s *wsServiceImpl) StartRedisSubscriber(ctx context.Context, client *model.Client) error {
	channel := "user:channel:" + client.UserID
	pubsub := s.rdb.Subscribe(ctx, channel)

	s.subRegistry.mu.Lock()
	s.subRegistry.subs[client] = pubsub
	s.subRegistry.mu.Unlock()

	go func() {
		ch := pubsub.Channel()
		for msg := range ch {
			select {
			case client.Send <- []byte(msg.Payload):
			default:
				s.log.Warn().
					Str("user_id", client.UserID).
					Msg("redis subscriber: client send buffer full, dropping message")
			}
		}
	}()

	s.log.Debug().Str("user_id", client.UserID).Str("channel", channel).Msg("redis subscriber started")
	return nil
}

// StopRedisSubscriber stops the Redis pub-sub subscriber for the given client.
func (s *wsServiceImpl) StopRedisSubscriber(client *model.Client) error {
	s.subRegistry.mu.Lock()
	pubsub, ok := s.subRegistry.subs[client]
	if ok {
		delete(s.subRegistry.subs, client)
	}
	s.subRegistry.mu.Unlock()

	if pubsub != nil {
		s.log.Debug().Str("user_id", client.UserID).Msg("redis subscriber stopped")
		return pubsub.Close()
	}
	return nil
}
