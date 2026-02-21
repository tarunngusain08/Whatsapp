package service

import (
	"context"
	"fmt"
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
	if count == 1 {
		if err := r.rdb.Expire(ctx, key, time.Duration(windowSec)*time.Second).Err(); err != nil {
			r.rdb.Del(ctx, key)
			return false, fmt.Errorf("rate limiter: set TTL: %w", err)
		}
	}
	return count <= int64(limit), nil
}
