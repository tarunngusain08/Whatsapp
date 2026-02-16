package service

import "context"

type AuthValidator interface {
	ValidateToken(ctx context.Context, token string) (userID string, phone string, err error)
}
