package tests

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/gorilla/websocket"
	"github.com/stretchr/testify/require"
)

var baseURL string

func init() {
	baseURL = os.Getenv("API_BASE_URL")
	if baseURL == "" {
		baseURL = "http://localhost:8080"
	}
}

// uniqueID returns a unique ID for test deduplication across runs.
func uniqueID(prefix string) string {
	return fmt.Sprintf("%s-%d-%d", prefix, time.Now().UnixNano(), rand.Intn(100000))
}

// doRequest builds and executes an HTTP request against the API gateway.
func doRequest(t *testing.T, method, path string, body interface{}, token string) *http.Response {
	t.Helper()

	var bodyReader io.Reader
	if body != nil {
		data, err := json.Marshal(body)
		require.NoError(t, err)
		bodyReader = bytes.NewReader(data)
	}

	req, err := http.NewRequest(method, baseURL+path, bodyReader)
	require.NoError(t, err)
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	require.NoError(t, err)
	return resp
}

// parseResponse reads the JSON body of a response into a generic map.
func parseResponse(t *testing.T, resp *http.Response) map[string]interface{} {
	t.Helper()
	defer resp.Body.Close()
	var result map[string]interface{}
	err := json.NewDecoder(resp.Body).Decode(&result)
	require.NoError(t, err)
	return result
}

// parseResponseRaw reads the raw bytes of a response body.
func parseResponseRaw(t *testing.T, resp *http.Response) []byte {
	t.Helper()
	defer resp.Body.Close()
	data, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	return data
}

// registerUser registers a user via the OTP flow and returns tokens and user ID.
// The flow: request OTP -> verify OTP -> get profile to extract user ID.
func registerUser(t *testing.T, phone string) (accessToken, refreshToken, userID string) {
	t.Helper()

	// Step 1: Request OTP
	resp := doRequest(t, "POST", "/api/v1/auth/request-otp", map[string]string{
		"phone": phone,
	}, "")
	require.Equal(t, http.StatusOK, resp.StatusCode, "request-otp should return 200")
	body := parseResponse(t, resp)
	require.True(t, body["success"].(bool), "request-otp should succeed")
	data := body["data"].(map[string]interface{})
	otp := data["otp"].(string)
	require.NotEmpty(t, otp, "OTP should not be empty")

	// Step 2: Verify OTP
	resp = doRequest(t, "POST", "/api/v1/auth/verify-otp", map[string]string{
		"phone": phone,
		"code":  otp,
	}, "")
	require.Equal(t, http.StatusOK, resp.StatusCode, "verify-otp should return 200")
	body = parseResponse(t, resp)
	require.True(t, body["success"].(bool), "verify-otp should succeed")
	data = body["data"].(map[string]interface{})
	accessToken = data["access_token"].(string)
	refreshToken = data["refresh_token"].(string)
	require.NotEmpty(t, accessToken)
	require.NotEmpty(t, refreshToken)

	// Step 3: Get user ID from profile
	resp = doRequest(t, "GET", "/api/v1/users/me", nil, accessToken)
	require.Equal(t, http.StatusOK, resp.StatusCode, "get profile should return 200")
	body = parseResponse(t, resp)
	require.True(t, body["success"].(bool))
	userID = body["data"].(map[string]interface{})["id"].(string)
	require.NotEmpty(t, userID)

	return accessToken, refreshToken, userID
}

// extractChatInfo extracts chat_id and type from either flattened or nested format.
func extractChatInfo(data map[string]interface{}) (chatID, chatType string) {
	// Flattened format: { chat_id, type, ... }
	if id, ok := data["chat_id"].(string); ok {
		t, _ := data["type"].(string)
		return id, t
	}
	// Nested format: { chat: { id, type } }
	if chat, ok := data["chat"].(map[string]interface{}); ok {
		id, _ := chat["id"].(string)
		t, _ := chat["type"].(string)
		return id, t
	}
	return "", ""
}

// extractChatList extracts the chat array from either PaginatedData format
// (data.items) or legacy format (data as array directly).
func extractChatList(t *testing.T, data interface{}) []interface{} {
	t.Helper()

	if dataMap, ok := data.(map[string]interface{}); ok {
		if items, ok := dataMap["items"].([]interface{}); ok {
			return items
		}
	}
	if arr, ok := data.([]interface{}); ok {
		return arr
	}
	t.Fatal("chat data should be either PaginatedData or array")
	return nil
}

// extractMessageList extracts the message array from either PaginatedData format
// (data.items) or the legacy format (data as array directly).
func extractMessageList(t *testing.T, data interface{}) []interface{} {
	t.Helper()

	// New PaginatedData format: { items: [...], nextCursor, hasMore }
	if dataMap, ok := data.(map[string]interface{}); ok {
		if items, ok := dataMap["items"].([]interface{}); ok {
			return items
		}
	}

	// Legacy format: data is the array directly
	if arr, ok := data.([]interface{}); ok {
		return arr
	}

	t.Fatal("data should be either PaginatedData or array")
	return nil
}

// createDirectChat creates a direct chat between the caller and otherUserID.
// Returns the chat ID.
func createDirectChat(t *testing.T, token, otherUserID string) string {
	t.Helper()

	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"other_user_id": otherUserID,
	}, token)
	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode, "create direct chat should return 200 or 201")
	body := parseResponse(t, resp)
	data := body["data"].(map[string]interface{})

	// Support both flattened format (chat_id) and nested format (chat.id)
	if chatID, ok := data["chat_id"].(string); ok {
		require.NotEmpty(t, chatID)
		return chatID
	}
	chat := data["chat"].(map[string]interface{})
	chatID := chat["id"].(string)
	require.NotEmpty(t, chatID)
	return chatID
}

// sendMessage sends a text message in a chat and returns the message ID.
func sendMessage(t *testing.T, token, chatID, text, clientMsgID string) string {
	t.Helper()

	resp := doRequest(t, "POST", "/api/v1/messages", map[string]interface{}{
		"chat_id": chatID,
		"type":    "text",
		"payload": map[string]interface{}{
			"body": text,
		},
		"client_msg_id": clientMsgID,
	}, token)
	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode, "send message should return 200 or 201")
	body := parseResponse(t, resp)
	data := body["data"].(map[string]interface{})
	msgID := data["message_id"].(string)
	require.NotEmpty(t, msgID)
	return msgID
}

// connectWS opens a WebSocket connection via the API gateway.
func connectWS(t *testing.T, token string) *websocket.Conn {
	t.Helper()

	wsURL := os.Getenv("WS_URL")
	if wsURL == "" {
		wsURL = "ws://localhost:8080/ws"
	}

	header := http.Header{}
	header.Set("Authorization", "Bearer "+token)

	dialer := websocket.Dialer{
		HandshakeTimeout: 10 * time.Second,
	}
	conn, _, err := dialer.Dial(wsURL, header)
	require.NoError(t, err, "websocket dial should succeed")
	return conn
}

// readWSEvent reads one JSON event from a WebSocket connection with a timeout.
func readWSEvent(t *testing.T, conn *websocket.Conn, timeout time.Duration) map[string]interface{} {
	t.Helper()

	_ = conn.SetReadDeadline(time.Now().Add(timeout))
	_, message, err := conn.ReadMessage()
	require.NoError(t, err, "should read ws message")

	var event map[string]interface{}
	err = json.Unmarshal(message, &event)
	require.NoError(t, err, "ws message should be valid JSON")
	return event
}
