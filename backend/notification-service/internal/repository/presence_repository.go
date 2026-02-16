package repository

import (
	"context"
	"fmt"

	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog"
)

// PresenceRepository checks whether a user is currently online.
type PresenceRepository interface {
	IsOnline(ctx context.Context, userID string) (bool, error)
}

type presenceRedis struct {
	client *redis.Client
	log    zerolog.Logger
}

func NewPresenceRepository(client *redis.Client, log zerolog.Logger) PresenceRepository {
	return &presenceRedis{client: client, log: log}
}

func (r *presenceRedis) IsOnline(ctx context.Context, userID string) (bool, error) {
	key := fmt.Sprintf("presence:%s", userID)
	exists, err := r.client.Exists(ctx, key).Result()
	if err != nil {
		return false, fmt.Errorf("check presence: %w", err)
	}
	return exists > 0, nil
}
