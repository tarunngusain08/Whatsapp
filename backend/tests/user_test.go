package tests

import (
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestUserProfile_GetMe(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552001")

	resp := doRequest(t, "GET", "/api/v1/users/me", nil, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	user := body["data"].(map[string]interface{})
	assert.Equal(t, "+14155552001", user["phone"])
	assert.NotEmpty(t, user["id"])
	assert.NotEmpty(t, user["created_at"])
}

func TestUserProfile_UpdateDisplayName(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552002")

	resp := doRequest(t, "PUT", "/api/v1/users/me", map[string]interface{}{
		"display_name": "Test User",
	}, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	user := body["data"].(map[string]interface{})
	assert.Equal(t, "Test User", user["display_name"])
}

func TestUserProfile_UpdateStatusText(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552010")

	resp := doRequest(t, "PUT", "/api/v1/users/me", map[string]interface{}{
		"status_text": "Busy coding",
	}, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	user := body["data"].(map[string]interface{})
	assert.Equal(t, "Busy coding", user["status_text"])
}

func TestUserProfile_GetOtherUser(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155552011")
	_, _, userB := registerUser(t, "+14155552012")

	resp := doRequest(t, "GET", "/api/v1/users/"+userB, nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	user := body["data"].(map[string]interface{})
	assert.Equal(t, userB, user["id"])
}

func TestUserProfile_ContactSync(t *testing.T) {
	// Ensure a user exists with a known phone
	registerUser(t, "+14155552003")
	token, _, _ := registerUser(t, "+14155552004")

	resp := doRequest(t, "POST", "/api/v1/users/contacts/sync", map[string]interface{}{
		"phones": []string{"+14155552003", "+14155559999"},
	}, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))

	// Support both formats: new { registered_users: [...] } or legacy [...]
	data := body["data"]
	var results []interface{}
	if dataMap, ok := data.(map[string]interface{}); ok {
		if ru, ok := dataMap["registered_users"].([]interface{}); ok {
			results = ru
		}
	} else if arr, ok := data.([]interface{}); ok {
		results = arr
	}
	require.NotNil(t, results, "contact sync results should not be nil")
	assert.GreaterOrEqual(t, len(results), 1, "at least one contact should be found")

	found := false
	for _, r := range results {
		contact := r.(map[string]interface{})
		if contact["phone"] == "+14155552003" {
			found = true
			assert.NotEmpty(t, contact["user_id"])
			break
		}
	}
	assert.True(t, found, "synced user should appear in results")
}

func TestUserProfile_GetContacts(t *testing.T) {
	registerUser(t, "+14155552013")
	token, _, _ := registerUser(t, "+14155552014")

	// Sync a contact first
	resp := doRequest(t, "POST", "/api/v1/users/contacts/sync", map[string]interface{}{
		"phones": []string{"+14155552013"},
	}, token)
	require.Equal(t, http.StatusOK, resp.StatusCode)

	// Get contacts list
	resp = doRequest(t, "GET", "/api/v1/users/contacts", nil, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
}

func TestUserProfile_BlockAndUnblock(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155552015")
	_, _, userB := registerUser(t, "+14155552016")

	// Block user B
	resp := doRequest(t, "POST", "/api/v1/users/contacts/"+userB+"/block", nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Unblock user B
	resp = doRequest(t, "DELETE", "/api/v1/users/contacts/"+userB+"/block", nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestUserProfile_Privacy_UpdateAndGet(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552005")

	// Update privacy settings
	resp := doRequest(t, "PUT", "/api/v1/users/privacy", map[string]interface{}{
		"last_seen":     "nobody",
		"profile_photo": "contacts",
		"about":         "everyone",
		"read_receipts": false,
	}, token)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Get privacy settings
	resp = doRequest(t, "GET", "/api/v1/users/privacy", nil, token)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	settings := body["data"].(map[string]interface{})
	assert.Equal(t, "nobody", settings["last_seen"])
	assert.Equal(t, "contacts", settings["profile_photo"])
	assert.Equal(t, "everyone", settings["about"])
	assert.Equal(t, false, settings["read_receipts"])
}

func TestUserProfile_Presence(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155552017")
	_, _, userB := registerUser(t, "+14155552018")

	resp := doRequest(t, "GET", "/api/v1/users/"+userB+"/presence", nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	data := body["data"].(map[string]interface{})
	// online should be a boolean (probably false since user B is not connected via WS)
	_, hasOnline := data["online"]
	assert.True(t, hasOnline, "presence response should contain 'online' field")
}

func TestUserProfile_RegisterDevice(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552019")

	resp := doRequest(t, "POST", "/api/v1/users/devices", map[string]interface{}{
		"token":    "fake-fcm-token-12345",
		"platform": "android",
	}, token)
	assert.Equal(t, http.StatusCreated, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
}

func TestUserProfile_RemoveDevice(t *testing.T) {
	token, _, _ := registerUser(t, "+14155552020")

	// Register device first
	resp := doRequest(t, "POST", "/api/v1/users/devices", map[string]interface{}{
		"token":    "fake-fcm-token-to-remove",
		"platform": "ios",
	}, token)
	require.Equal(t, http.StatusCreated, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Remove device
	resp = doRequest(t, "DELETE", "/api/v1/users/devices/fake-fcm-token-to-remove", nil, token)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}
