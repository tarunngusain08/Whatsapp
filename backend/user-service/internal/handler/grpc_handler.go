package handler

import (
	"context"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"

	userv1 "github.com/whatsapp-clone/backend/proto/user/v1"
	"github.com/whatsapp-clone/backend/user-service/internal/service"
)

type GRPCHandler struct {
	userv1.UnimplementedUserServiceServer
	userSvc service.UserService
}

func NewGRPCHandler(userSvc service.UserService) *GRPCHandler {
	return &GRPCHandler{userSvc: userSvc}
}

func (h *GRPCHandler) GetUser(ctx context.Context, req *userv1.GetUserRequest) (*userv1.GetUserResponse, error) {
	if req.UserId == "" {
		return nil, status.Error(codes.InvalidArgument, "user_id required")
	}

	user, err := h.userSvc.GetProfile(ctx, "", req.UserId)
	if err != nil {
		return nil, status.Error(codes.NotFound, err.Error())
	}

	return &userv1.GetUserResponse{
		User: &userv1.UserProfile{
			UserId:      user.ID,
			Phone:       user.Phone,
			DisplayName: user.DisplayName,
			AvatarUrl:   user.AvatarURL,
			StatusText:  user.StatusText,
			CreatedAt:   timestamppb.New(user.CreatedAt),
			UpdatedAt:   timestamppb.New(user.UpdatedAt),
		},
	}, nil
}

func (h *GRPCHandler) GetUsers(ctx context.Context, req *userv1.GetUsersRequest) (*userv1.GetUsersResponse, error) {
	if len(req.UserIds) == 0 {
		return &userv1.GetUsersResponse{}, nil
	}

	users, err := h.userSvc.GetUsersByIDs(ctx, req.UserIds)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}

	var profiles []*userv1.UserProfile
	for _, u := range users {
		profiles = append(profiles, &userv1.UserProfile{
			UserId:      u.ID,
			Phone:       u.Phone,
			DisplayName: u.DisplayName,
			AvatarUrl:   u.AvatarURL,
			StatusText:  u.StatusText,
			CreatedAt:   timestamppb.New(u.CreatedAt),
			UpdatedAt:   timestamppb.New(u.UpdatedAt),
		})
	}

	return &userv1.GetUsersResponse{Users: profiles}, nil
}

func (h *GRPCHandler) CheckPresence(ctx context.Context, req *userv1.CheckPresenceRequest) (*userv1.CheckPresenceResponse, error) {
	online, lastSeen, err := h.userSvc.CheckPresence(ctx, req.UserId)
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}

	resp := &userv1.CheckPresenceResponse{Online: online}
	if lastSeen != nil {
		resp.LastSeen = timestamppb.New(*lastSeen)
	}
	return resp, nil
}

func (h *GRPCHandler) GetPrivacySettings(ctx context.Context, req *userv1.GetPrivacySettingsRequest) (*userv1.GetPrivacySettingsResponse, error) {
	settings, err := h.userSvc.GetPrivacySettings(ctx, req.UserId)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "failed to get privacy settings: %v", err)
	}
	return &userv1.GetPrivacySettingsResponse{
		LastSeen:     string(settings.LastSeen),
		ProfilePhoto: string(settings.ProfilePhoto),
		About:        string(settings.About),
		ReadReceipts: settings.ReadReceipts,
	}, nil
}
