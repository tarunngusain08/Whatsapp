package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

type redisPresenceRepository struct {
	client *redis.Client
}

func NewRedisPresenceRepository(client *redis.Client) PresenceRepository {
	return &redisPresenceRepository{client: client}
}

func presenceKey(userID string) string {
	return "presence:" + userID
}

func lastSeenKey(userID string) string {
	return "last_seen:" + userID
}

func (r *redisPresenceRepository) SetOnline(ctx context.Context, userID string, ttl time.Duration) error {
	err := r.client.SetEx(ctx, presenceKey(userID), "online", ttl).Err()
	if err != nil {
		return fmt.Errorf("set online: %w", err)
	}
	return nil
}

func (r *redisPresenceRepository) IsOnline(ctx context.Context, userID string) (bool, error) {
	result, err := r.client.Exists(ctx, presenceKey(userID)).Result()
	if err != nil {
		return false, fmt.Errorf("check online: %w", err)
	}
	return result > 0, nil
}

func (r *redisPresenceRepository) SetLastSeen(ctx context.Context, userID string, t time.Time) error {
	err := r.client.Set(ctx, lastSeenKey(userID), t.Format(time.RFC3339), 0).Err()
	if err != nil {
		return fmt.Errorf("set last seen: %w", err)
	}
	return nil
}

func (r *redisPresenceRepository) GetLastSeen(ctx context.Context, userID string) (time.Time, error) {
	val, err := r.client.Get(ctx, lastSeenKey(userID)).Result()
	if err == redis.Nil {
		return time.Time{}, nil
	}
	if err != nil {
		return time.Time{}, fmt.Errorf("get last seen: %w", err)
	}

	t, err := time.Parse(time.RFC3339, val)
	if err != nil {
		return time.Time{}, fmt.Errorf("parse last seen time: %w", err)
	}
	return t, nil
}

func (r *redisPresenceRepository) Remove(ctx context.Context, userID string) error {
	err := r.client.Del(ctx, presenceKey(userID)).Err()
	if err != nil {
		return fmt.Errorf("remove presence: %w", err)
	}
	return nil
}
