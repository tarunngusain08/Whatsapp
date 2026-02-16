package service

import (
	"context"
	"fmt"

	authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
	"google.golang.org/grpc"
)

type authGRPCValidator struct {
	client authv1.AuthServiceClient
}

func NewAuthValidator(conn *grpc.ClientConn) AuthValidator {
	return &authGRPCValidator{client: authv1.NewAuthServiceClient(conn)}
}

func (v *authGRPCValidator) ValidateToken(ctx context.Context, token string) (string, string, error) {
	resp, err := v.client.ValidateToken(ctx, &authv1.ValidateTokenRequest{Token: token})
	if err != nil {
		return "", "", fmt.Errorf("auth grpc: %w", err)
	}
	if !resp.Valid {
		return "", "", fmt.Errorf("token invalid")
	}
	return resp.UserId, resp.Phone, nil
}
