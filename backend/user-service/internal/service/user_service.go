package service

import (
	"context"
	"io"
	"time"

	"github.com/whatsapp-clone/backend/user-service/internal/model"
)

type UserService interface {
	GetProfile(ctx context.Context, callerID, targetID string) (*model.User, error)
	GetUsersByIDs(ctx context.Context, ids []string) ([]*model.User, error)
	UpdateProfile(ctx context.Context, userID string, req *model.UpdateProfileRequest) (*model.User, error)
	UploadAvatar(ctx context.Context, userID string, file io.Reader, size int64, contentType string) (string, error)
	ContactSync(ctx context.Context, userID string, phones []string) ([]*model.ContactSyncResult, error)
	GetContacts(ctx context.Context, userID string) ([]*model.Contact, error)
	BlockUser(ctx context.Context, userID, targetID string) error
	UnblockUser(ctx context.Context, userID, targetID string) error
	GetPrivacySettings(ctx context.Context, userID string) (*model.PrivacySettings, error)
	UpdatePrivacySettings(ctx context.Context, settings *model.PrivacySettings) error
	RegisterDeviceToken(ctx context.Context, token *model.DeviceToken) error
	RemoveDeviceToken(ctx context.Context, token string) error
	RemoveDeviceTokenForUser(ctx context.Context, userID, token string) error
	SetPresence(ctx context.Context, userID string, online bool) error
	CheckPresence(ctx context.Context, userID string) (online bool, lastSeen *time.Time, err error)
	CreateStatus(ctx context.Context, userID string, req *model.CreateStatusRequest) (*model.Status, error)
	GetMyStatuses(ctx context.Context, userID string) ([]*model.Status, error)
	GetContactStatuses(ctx context.Context, userID string) ([]*model.Status, error)
	DeleteStatus(ctx context.Context, statusID, userID string) error
	ViewStatus(ctx context.Context, statusID, viewerID string) error
}
