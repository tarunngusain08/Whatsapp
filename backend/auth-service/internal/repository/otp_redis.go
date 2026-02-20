package repository

import (
	"context"
	"encoding/json"
	"time"

	"github.com/redis/go-redis/v9"
	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

type redisOTPRepository struct {
	rdb *redis.Client
	ttl time.Duration
}

func NewRedisOTPRepository(rdb *redis.Client, ttl time.Duration) OTPRepository {
	return &redisOTPRepository{rdb: rdb, ttl: ttl}
}

func (r *redisOTPRepository) key(phone string) string {
	return "otp:" + phone
}

func (r *redisOTPRepository) Store(ctx context.Context, phone string, entry *model.OTPEntry) error {
	data, err := json.Marshal(entry)
	if err != nil {
		return apperr.NewInternal("failed to marshal OTP entry", err)
	}
	if err := r.rdb.Set(ctx, r.key(phone), data, r.ttl).Err(); err != nil {
		return apperr.NewInternal("failed to store OTP", err)
	}
	return nil
}

func (r *redisOTPRepository) Get(ctx context.Context, phone string) (*model.OTPEntry, error) {
	data, err := r.rdb.Get(ctx, r.key(phone)).Bytes()
	if err != nil {
		if err == redis.Nil {
			return nil, nil
		}
		return nil, apperr.NewInternal("failed to get OTP", err)
	}
	var entry model.OTPEntry
	if err := json.Unmarshal(data, &entry); err != nil {
		return nil, apperr.NewInternal("failed to unmarshal OTP entry", err)
	}
	return &entry, nil
}

func (r *redisOTPRepository) IncrementAttempts(ctx context.Context, phone string) (int, error) {
	entry, err := r.Get(ctx, phone)
	if err != nil {
		return 0, err
	}
	if entry == nil {
		return 0, nil
	}
	entry.Attempts++
	data, err := json.Marshal(entry)
	if err != nil {
		return 0, apperr.NewInternal("failed to marshal OTP entry", err)
	}
	ttl := r.rdb.TTL(ctx, r.key(phone)).Val()
	if ttl <= 0 {
		ttl = r.ttl
	}
	if err := r.rdb.Set(ctx, r.key(phone), data, ttl).Err(); err != nil {
		return 0, apperr.NewInternal("failed to update OTP attempts", err)
	}
	return entry.Attempts, nil
}

func (r *redisOTPRepository) Delete(ctx context.Context, phone string) error {
	return r.rdb.Del(ctx, r.key(phone)).Err()
}
