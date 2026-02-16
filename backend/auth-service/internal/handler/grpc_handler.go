package handler

import (
	"context"

	"github.com/rs/zerolog"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/whatsapp-clone/backend/auth-service/internal/service"
	authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
)

type GRPCHandler struct {
	authv1.UnimplementedAuthServiceServer
	authSvc service.AuthService
	log     zerolog.Logger
}

func NewGRPCHandler(authSvc service.AuthService, log zerolog.Logger) *GRPCHandler {
	return &GRPCHandler{authSvc: authSvc, log: log}
}

func (h *GRPCHandler) ValidateToken(ctx context.Context, req *authv1.ValidateTokenRequest) (*authv1.ValidateTokenResponse, error) {
	if req.Token == "" {
		return &authv1.ValidateTokenResponse{Valid: false}, nil
	}

	userID, phone, err := h.authSvc.ValidateToken(ctx, req.Token)
	if err != nil {
		h.log.Debug().Err(err).Msg("token validation failed")
		return &authv1.ValidateTokenResponse{Valid: false}, nil
	}

	if userID == "" {
		return nil, status.Error(codes.Internal, "user_id is empty after validation")
	}

	return &authv1.ValidateTokenResponse{
		Valid:  true,
		UserId: userID,
		Phone:  phone,
	}, nil
}
