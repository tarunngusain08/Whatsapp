package service

import (
	"context"
	"time"

	"github.com/redis/go-redis/v9"
)

type redisRateLimiter struct {
	rdb *redis.Client
}

func NewRedisRateLimiter(rdb *redis.Client) RateLimiter {
	return &redisRateLimiter{rdb: rdb}
}

func (r *redisRateLimiter) Allow(ctx context.Context, key string, limit int, windowSec int) (bool, error) {
	count, err := r.rdb.Incr(ctx, key).Result()
	if err != nil {
		return false, err
	}
	// Only set TTL when the key is new (count == 1) to create a fixed window
	if count == 1 {
		r.rdb.Expire(ctx, key, time.Duration(windowSec)*time.Second)
	}
	return count <= int64(limit), nil
}
