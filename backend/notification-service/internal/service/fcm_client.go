package service

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"math"
	"net/http"
	"time"

	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/notification-service/internal/model"
	"github.com/whatsapp-clone/backend/notification-service/internal/repository"
)

// FCMClient sends push notifications to device tokens.
type FCMClient interface {
	Send(ctx context.Context, payload *model.NotificationPayload) error
}

// ---------------------------------------------------------------------------
// MockFCMClient — logs notifications instead of sending (for dev / missing creds)
// ---------------------------------------------------------------------------

type MockFCMClient struct {
	log zerolog.Logger
}

func NewMockFCMClient(log zerolog.Logger) *MockFCMClient {
	return &MockFCMClient{log: log}
}

func (m *MockFCMClient) Send(_ context.Context, payload *model.NotificationPayload) error {
	m.log.Info().
		Str("token", payload.Token[:min(len(payload.Token), 12)]+"...").
		Str("title", payload.Title).
		Str("body", payload.Body).
		Interface("data", payload.Data).
		Msg("[MOCK FCM] push notification sent")
	return nil
}

// ---------------------------------------------------------------------------
// HTTPFCMClient — real FCM HTTP v1 API client with retry and stale-token cleanup
// ---------------------------------------------------------------------------

type HTTPFCMClient struct {
	httpClient *http.Client
	projectID  string
	apiKey     string
	tokenRepo  repository.DeviceTokenRepository
	maxRetries int
	log        zerolog.Logger
}

func NewHTTPFCMClient(projectID, apiKey string, tokenRepo repository.DeviceTokenRepository, maxRetries int, log zerolog.Logger) *HTTPFCMClient {
	return &HTTPFCMClient{
		httpClient: &http.Client{Timeout: 10 * time.Second},
		projectID:  projectID,
		apiKey:     apiKey,
		tokenRepo:  tokenRepo,
		maxRetries: maxRetries,
		log:        log,
	}
}

func (f *HTTPFCMClient) Send(ctx context.Context, payload *model.NotificationPayload) error {
	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", f.projectID)

	body := map[string]interface{}{
		"message": map[string]interface{}{
			"token": payload.Token,
			"data":  payload.Data,
		},
	}

	jsonBody, err := json.Marshal(body)
	if err != nil {
		return fmt.Errorf("marshal FCM payload: %w", err)
	}

	var lastErr error
	for attempt := 0; attempt <= f.maxRetries; attempt++ {
		if attempt > 0 {
			backoff := time.Duration(math.Pow(2, float64(attempt-1))) * 500 * time.Millisecond
			select {
			case <-ctx.Done():
				return ctx.Err()
			case <-time.After(backoff):
			}
		}

		req, reqErr := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(jsonBody))
		if reqErr != nil {
			return fmt.Errorf("create FCM request: %w", reqErr)
		}
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("Authorization", "Bearer "+f.apiKey)

		resp, doErr := f.httpClient.Do(req)
		if doErr != nil {
			lastErr = fmt.Errorf("FCM request failed: %w", doErr)
			continue
		}

		respBody, _ := io.ReadAll(resp.Body)
		resp.Body.Close()

		switch {
		case resp.StatusCode == http.StatusOK:
			return nil
		case resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusGone:
			// Stale token — remove from database.
			if delErr := f.tokenRepo.DeleteByToken(ctx, payload.Token); delErr != nil {
				f.log.Error().Err(delErr).Msg("failed to delete stale FCM token")
			}
			return fmt.Errorf("stale FCM token %s, removed", payload.Token[:min(len(payload.Token), 12)])
		case resp.StatusCode >= 500:
			lastErr = fmt.Errorf("FCM server error %d: %s", resp.StatusCode, string(respBody))
			continue
		default:
			return fmt.Errorf("FCM error %d: %s", resp.StatusCode, string(respBody))
		}
	}

	return fmt.Errorf("FCM send failed after %d retries: %w", f.maxRetries, lastErr)
}
