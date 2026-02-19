package service

import (
	"context"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/rs/zerolog"

	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/user-service/internal/model"
	"github.com/whatsapp-clone/backend/user-service/internal/repository"
)

type userServiceImpl struct {
	userRepo        repository.UserRepository
	contactRepo     repository.ContactRepository
	privacyRepo     repository.PrivacyRepository
	deviceTokenRepo repository.DeviceTokenRepository
	presenceRepo    repository.PresenceRepository
	statusRepo      repository.StatusRepository
	presenceTTL     time.Duration
	log             zerolog.Logger
}

func NewUserService(
	userRepo repository.UserRepository,
	contactRepo repository.ContactRepository,
	privacyRepo repository.PrivacyRepository,
	deviceTokenRepo repository.DeviceTokenRepository,
	presenceRepo repository.PresenceRepository,
	statusRepo repository.StatusRepository,
	presenceTTL time.Duration,
	log zerolog.Logger,
) UserService {
	return &userServiceImpl{
		userRepo:        userRepo,
		contactRepo:     contactRepo,
		privacyRepo:     privacyRepo,
		deviceTokenRepo: deviceTokenRepo,
		presenceRepo:    presenceRepo,
		statusRepo:      statusRepo,
		presenceTTL:     presenceTTL,
		log:             log,
	}
}

func (s *userServiceImpl) GetProfile(ctx context.Context, callerID, targetID string) (*model.User, error) {
	// For internal gRPC calls callerID may be empty — skip block check.
	if callerID != "" && callerID != targetID {
		blocked, err := s.contactRepo.IsBlocked(ctx, targetID, callerID)
		if err != nil {
			return nil, apperr.NewInternal("failed to check block status", err)
		}
		if blocked {
			return nil, apperr.NewForbidden("user not available")
		}
	}

	user, err := s.userRepo.GetByID(ctx, targetID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get user", err)
	}
	if user == nil {
		return nil, apperr.NewNotFound("user not found")
	}

	// If caller is viewing their own profile, return everything.
	if callerID == targetID || callerID == "" {
		return user, nil
	}

	// Load privacy settings of the target.
	privacy, err := s.privacyRepo.Get(ctx, targetID)
	if err != nil {
		return nil, apperr.NewInternal("failed to load privacy settings", err)
	}
	if privacy == nil {
		return user, nil
	}

	// Determine relationship: is the caller in the target's contacts?
	isContact, err := s.contactRepo.IsContact(ctx, targetID, callerID)
	if err != nil {
		return nil, apperr.NewInternal("failed to check contact status", err)
	}

	// Apply privacy filters.
	if !canSee(privacy.ProfilePhoto, isContact, callerID == targetID) {
		user.AvatarURL = ""
	}
	if !canSee(privacy.About, isContact, callerID == targetID) {
		user.StatusText = ""
	}

	return user, nil
}

func (s *userServiceImpl) GetUsersByIDs(ctx context.Context, ids []string) ([]*model.User, error) {
	users, err := s.userRepo.GetByIDs(ctx, ids)
	if err != nil {
		return nil, apperr.NewInternal("failed to get users", err)
	}
	return users, nil
}

func (s *userServiceImpl) UpdateProfile(ctx context.Context, userID string, req *model.UpdateProfileRequest) (*model.User, error) {
	if req.DisplayName == nil && req.AvatarURL == nil && req.StatusText == nil {
		return nil, apperr.NewBadRequest("at least one field must be provided")
	}

	user, err := s.userRepo.Update(ctx, userID, req)
	if err != nil {
		return nil, apperr.NewInternal("failed to update profile", err)
	}
	if user == nil {
		return nil, apperr.NewNotFound("user not found")
	}
	return user, nil
}

func (s *userServiceImpl) UploadAvatar(ctx context.Context, userID string, file io.Reader, size int64, contentType string) (string, error) {
	// In a production system, this would delegate to media-service for storage.
	// For now, generate a reference URL. The media-service would be called via gRPC.
	avatarURL := fmt.Sprintf("/api/v1/media/avatars/%s", userID)

	_, err := s.userRepo.Update(ctx, userID, &model.UpdateProfileRequest{
		AvatarURL: &avatarURL,
	})
	if err != nil {
		return "", apperr.NewInternal("failed to update avatar URL", err)
	}
	return avatarURL, nil
}

func (s *userServiceImpl) ContactSync(ctx context.Context, userID string, phones []string) ([]*model.ContactSyncResult, error) {
	if len(phones) == 0 {
		return nil, nil
	}
	if len(phones) > 1000 {
		return nil, apperr.NewBadRequest("max 1000 phones per sync request")
	}

	// Normalize and validate phone numbers (E.164)
	validPhones := make([]string, 0, len(phones))
	for _, p := range phones {
		if strings.HasPrefix(p, "+") && len(p) >= 8 && len(p) <= 16 {
			validPhones = append(validPhones, p)
		}
	}
	if len(validPhones) == 0 {
		return nil, nil
	}

	results, err := s.userRepo.GetByPhones(ctx, validPhones)
	if err != nil {
		return nil, apperr.NewInternal("failed to sync contacts", err)
	}

	// Auto-add matched users as contacts.
	for _, r := range results {
		if r.UserID != userID {
			_ = s.contactRepo.Upsert(ctx, &model.Contact{
				UserID:    userID,
				ContactID: r.UserID,
			})
		}
	}

	return results, nil
}

func (s *userServiceImpl) GetContacts(ctx context.Context, userID string) ([]*model.Contact, error) {
	contacts, err := s.contactRepo.GetByUserID(ctx, userID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get contacts", err)
	}
	return contacts, nil
}

func (s *userServiceImpl) BlockUser(ctx context.Context, userID, targetID string) error {
	if err := s.contactRepo.Block(ctx, userID, targetID); err != nil {
		return apperr.NewInternal("failed to block user", err)
	}
	return nil
}

func (s *userServiceImpl) UnblockUser(ctx context.Context, userID, targetID string) error {
	if err := s.contactRepo.Unblock(ctx, userID, targetID); err != nil {
		return apperr.NewInternal("failed to unblock user", err)
	}
	return nil
}

func (s *userServiceImpl) GetPrivacySettings(ctx context.Context, userID string) (*model.PrivacySettings, error) {
	settings, err := s.privacyRepo.Get(ctx, userID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get privacy settings", err)
	}
	if settings == nil {
		// Return defaults when no settings exist yet.
		return &model.PrivacySettings{
			UserID:       userID,
			LastSeen:     model.VisibilityEveryone,
			ProfilePhoto: model.VisibilityEveryone,
			About:        model.VisibilityEveryone,
			ReadReceipts: true,
		}, nil
	}
	return settings, nil
}

func (s *userServiceImpl) UpdatePrivacySettings(ctx context.Context, settings *model.PrivacySettings) error {
	if err := s.privacyRepo.Upsert(ctx, settings); err != nil {
		return apperr.NewInternal("failed to update privacy settings", err)
	}
	return nil
}

func (s *userServiceImpl) RegisterDeviceToken(ctx context.Context, token *model.DeviceToken) error {
	if err := s.deviceTokenRepo.Upsert(ctx, token); err != nil {
		return apperr.NewInternal("failed to register device token", err)
	}
	return nil
}

func (s *userServiceImpl) RemoveDeviceToken(ctx context.Context, token string) error {
	if err := s.deviceTokenRepo.DeleteByToken(ctx, token); err != nil {
		return apperr.NewInternal("failed to remove device token", err)
	}
	return nil
}

func (s *userServiceImpl) SetPresence(ctx context.Context, userID string, online bool) error {
	if online {
		if err := s.presenceRepo.SetOnline(ctx, userID, s.presenceTTL); err != nil {
			return apperr.NewInternal("failed to set online", err)
		}
	} else {
		if err := s.presenceRepo.Remove(ctx, userID); err != nil {
			return apperr.NewInternal("failed to set offline", err)
		}
		if err := s.presenceRepo.SetLastSeen(ctx, userID, time.Now()); err != nil {
			return apperr.NewInternal("failed to set last seen", err)
		}
	}
	return nil
}

func (s *userServiceImpl) CheckPresence(ctx context.Context, userID string) (bool, *time.Time, error) {
	online, err := s.presenceRepo.IsOnline(ctx, userID)
	if err != nil {
		return false, nil, apperr.NewInternal("failed to check presence", err)
	}
	if online {
		return true, nil, nil
	}

	lastSeen, err := s.presenceRepo.GetLastSeen(ctx, userID)
	if err != nil {
		return false, nil, apperr.NewInternal("failed to get last seen", err)
	}
	if lastSeen.IsZero() {
		return false, nil, nil
	}
	return false, &lastSeen, nil
}

func (s *userServiceImpl) CreateStatus(ctx context.Context, userID string, req *model.CreateStatusRequest) (*model.Status, error) {
	if req.Type != "text" && req.Type != "image" {
		return nil, apperr.NewBadRequest("type must be 'text' or 'image'")
	}

	status := &model.Status{
		ID:        fmt.Sprintf("%d", time.Now().UnixNano()),
		UserID:    userID,
		Type:      req.Type,
		Content:   req.Content,
		Caption:   req.Caption,
		BgColor:   req.BgColor,
		Viewers:   []string{},
		CreatedAt: time.Now(),
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := s.statusRepo.Create(ctx, status); err != nil {
		return nil, apperr.NewInternal("failed to create status", err)
	}
	return status, nil
}

func (s *userServiceImpl) GetMyStatuses(ctx context.Context, userID string) ([]*model.Status, error) {
	statuses, err := s.statusRepo.GetByUserID(ctx, userID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get statuses", err)
	}
	return statuses, nil
}

func (s *userServiceImpl) GetContactStatuses(ctx context.Context, userID string) ([]*model.Status, error) {
	contacts, err := s.contactRepo.GetByUserID(ctx, userID)
	if err != nil {
		return nil, apperr.NewInternal("failed to get contacts", err)
	}

	if len(contacts) == 0 {
		return nil, nil
	}

	contactIDs := make([]string, 0, len(contacts))
	for _, c := range contacts {
		if !c.IsBlocked {
			contactIDs = append(contactIDs, c.ContactID)
		}
	}

	if len(contactIDs) == 0 {
		return nil, nil
	}

	statuses, err := s.statusRepo.GetByUserIDs(ctx, contactIDs)
	if err != nil {
		return nil, apperr.NewInternal("failed to get contact statuses", err)
	}
	return statuses, nil
}

func (s *userServiceImpl) DeleteStatus(ctx context.Context, statusID, userID string) error {
	if err := s.statusRepo.Delete(ctx, statusID, userID); err != nil {
		return apperr.NewNotFound("status not found")
	}
	return nil
}

func (s *userServiceImpl) ViewStatus(ctx context.Context, statusID, viewerID string) error {
	if err := s.statusRepo.AddViewer(ctx, statusID, viewerID); err != nil {
		return apperr.NewInternal("failed to mark status as viewed", err)
	}
	return nil
}

// canSee determines if a field is visible based on privacy setting and relationship.
func canSee(vis model.PrivacyVisibility, isContact, isSelf bool) bool {
	if isSelf {
		return true
	}
	switch vis {
	case model.VisibilityEveryone:
		return true
	case model.VisibilityContacts:
		return isContact
	case model.VisibilityNobody:
		return false
	default:
		return true
	}
}
