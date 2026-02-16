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
	pipe := r.rdb.Pipeline()
	incrCmd := pipe.Incr(ctx, key)
	pipe.Expire(ctx, key, time.Duration(windowSec)*time.Second)
	_, err := pipe.Exec(ctx)
	if err != nil {
		return false, err
	}
	count := incrCmd.Val()
	return count <= int64(limit), nil
}
