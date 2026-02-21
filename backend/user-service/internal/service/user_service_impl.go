package service

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/google/uuid"
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
	mediaServiceURL string
	httpClient      *http.Client
	log             zerolog.Logger
}

var e164Regex = regexp.MustCompile(`^\+[1-9]\d{1,14}$`)

func NewUserService(
	userRepo repository.UserRepository,
	contactRepo repository.ContactRepository,
	privacyRepo repository.PrivacyRepository,
	deviceTokenRepo repository.DeviceTokenRepository,
	presenceRepo repository.PresenceRepository,
	statusRepo repository.StatusRepository,
	presenceTTL time.Duration,
	mediaServiceURL string,
	log zerolog.Logger,
) UserService {
	if mediaServiceURL == "" {
		mediaServiceURL = "http://media-service:8080"
	}
	return &userServiceImpl{
		userRepo:        userRepo,
		contactRepo:     contactRepo,
		privacyRepo:     privacyRepo,
		deviceTokenRepo: deviceTokenRepo,
		presenceRepo:    presenceRepo,
		statusRepo:      statusRepo,
		presenceTTL:     presenceTTL,
		mediaServiceURL: mediaServiceURL,
		httpClient:      &http.Client{Timeout: 30 * time.Second},
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

const maxAvatarSize = 5 * 1024 * 1024 // 5 MB

func (s *userServiceImpl) UploadAvatar(ctx context.Context, userID string, file io.Reader, size int64, contentType string) (string, error) {
	if size > maxAvatarSize {
		return "", apperr.NewBadRequest("avatar file too large (max 5 MB)")
	}

	avatarURL, err := s.uploadToMediaService(ctx, userID, file, contentType)
	if err != nil {
		s.log.Warn().Err(err).Msg("media-service upload failed, using placeholder URL")
		avatarURL = fmt.Sprintf("/api/v1/media/avatars/%s", userID)
	}

	_, err = s.userRepo.Update(ctx, userID, &model.UpdateProfileRequest{
		AvatarURL: &avatarURL,
	})
	if err != nil {
		return "", apperr.NewInternal("failed to update avatar URL", err)
	}
	return avatarURL, nil
}

func (s *userServiceImpl) uploadToMediaService(ctx context.Context, userID string, file io.Reader, contentType string) (string, error) {
	var buf bytes.Buffer
	writer := multipart.NewWriter(&buf)

	ext := "jpg"
	switch contentType {
	case "image/png":
		ext = "png"
	case "image/webp":
		ext = "webp"
	}

	part, err := writer.CreateFormFile("file", fmt.Sprintf("avatar_%s.%s", userID, ext))
	if err != nil {
		return "", fmt.Errorf("create form file: %w", err)
	}
	if _, err := io.Copy(part, file); err != nil {
		return "", fmt.Errorf("copy file data: %w", err)
	}
	if err := writer.Close(); err != nil {
		return "", fmt.Errorf("close multipart writer: %w", err)
	}

	url := fmt.Sprintf("%s/api/v1/media/upload", s.mediaServiceURL)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, &buf)
	if err != nil {
		return "", fmt.Errorf("create request: %w", err)
	}
	req.Header.Set("Content-Type", writer.FormDataContentType())
	req.Header.Set("X-User-ID", userID)

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("http call to media-service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return "", fmt.Errorf("media-service returned status %d", resp.StatusCode)
	}

	var envelope struct {
		Data struct {
			URL string `json:"url"`
		} `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		return "", fmt.Errorf("decode media-service response: %w", err)
	}

	if envelope.Data.URL == "" {
		return "", fmt.Errorf("media-service returned empty URL")
	}
	return envelope.Data.URL, nil
}

func (s *userServiceImpl) ContactSync(ctx context.Context, userID string, phones []string) ([]*model.ContactSyncResult, error) {
	if len(phones) == 0 {
		return nil, nil
	}
	if len(phones) > 1000 {
		return nil, apperr.NewBadRequest("max 1000 phones per sync request")
	}

	validPhones := make([]string, 0, len(phones))
	for _, p := range phones {
		if e164Regex.MatchString(p) {
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
			if err := s.contactRepo.Upsert(ctx, &model.Contact{
				UserID:    userID,
				ContactID: r.UserID,
			}); err != nil {
				s.log.Warn().Err(err).
					Str("user_id", userID).
					Str("contact_id", r.UserID).
					Msg("failed to auto-add contact during sync")
			}
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

func (s *userServiceImpl) RemoveDeviceTokenForUser(ctx context.Context, userID, token string) error {
	if err := s.deviceTokenRepo.DeleteByTokenAndUser(ctx, userID, token); err != nil {
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
		ID:        uuid.New().String(),
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
