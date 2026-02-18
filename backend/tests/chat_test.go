package tests

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestChat_CreateDirect(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553001")
	_, _, userB := registerUser(t, "+14155553002")

	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"other_user_id": userB,
	}, tokenA)
	assert.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	data := body["data"].(map[string]interface{})

	chatID, chatType := extractChatInfo(data)
	assert.Equal(t, "direct", chatType)
	assert.NotEmpty(t, chatID)
}

func TestChat_CreateDirect_Idempotent(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553003")
	_, _, userB := registerUser(t, "+14155553004")

	// Create chat first time
	chatID1 := createDirectChat(t, tokenA, userB)

	// Create same chat again — should return the same chat
	chatID2 := createDirectChat(t, tokenA, userB)

	assert.Equal(t, chatID1, chatID2, "creating the same direct chat twice should return the same chat ID")
}

func TestChat_CreateGroup(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553005")
	_, _, userB := registerUser(t, "+14155553006")
	_, _, userC := registerUser(t, "+14155553007")

	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "Test Group",
		"member_ids": []string{userB, userC},
	}, tokenA)
	assert.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	data := body["data"].(map[string]interface{})

	chatID, chatType := extractChatInfo(data)
	assert.Equal(t, "group", chatType)
	assert.NotEmpty(t, chatID)

	// In flat format, name is at the top level; in nested format it's in "group"
	if name, ok := data["name"].(string); ok {
		assert.Equal(t, "Test Group", name)
	} else if group, ok := data["group"].(map[string]interface{}); ok {
		assert.Equal(t, "Test Group", group["name"])
	}
}

func TestChat_CreateGroup_MissingName(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553020")
	_, _, userB := registerUser(t, "+14155553021")

	// Send "name" as empty -> should fail validation
	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "",
		"member_ids": []string{userB},
	}, tokenA)
	assert.NotEqual(t, http.StatusOK, resp.StatusCode)
	assert.NotEqual(t, http.StatusCreated, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestChat_CreateGroup_NoMembers(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553022")

	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "Empty Group",
		"member_ids": []string{},
	}, tokenA)
	assert.NotEqual(t, http.StatusOK, resp.StatusCode)
	assert.NotEqual(t, http.StatusCreated, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestChat_ListChats(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553008")
	_, _, userB := registerUser(t, "+14155553009")

	// Create a chat
	createDirectChat(t, tokenA, userB)

	// List chats
	resp := doRequest(t, "GET", "/api/v1/chats", nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	chats := extractChatList(t, body["data"])
	assert.GreaterOrEqual(t, len(chats), 1, "user should have at least one chat")
}

func TestChat_GetChat(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553010")
	_, _, userB := registerUser(t, "+14155553011")

	chatID := createDirectChat(t, tokenA, userB)

	resp := doRequest(t, "GET", fmt.Sprintf("/api/v1/chats/%s", chatID), nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
}

func TestChat_UpdateGroup(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553012")
	_, _, userB := registerUser(t, "+14155553013")

	// Create group
	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "Original Name",
		"member_ids": []string{userB},
	}, tokenA)
	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)
	body := parseResponse(t, resp)
	data := body["data"].(map[string]interface{})
	chatID, _ := extractChatInfo(data)

	// Update group name
	resp = doRequest(t, "PATCH", fmt.Sprintf("/api/v1/chats/%s", chatID), map[string]interface{}{
		"name": "Updated Name",
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestChat_AddAndRemoveMember(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553014")
	_, _, userB := registerUser(t, "+14155553015")
	_, _, userC := registerUser(t, "+14155553016")

	// Create group with user B
	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "Member Test Group",
		"member_ids": []string{userB},
	}, tokenA)
	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)
	body := parseResponse(t, resp)
	data := body["data"].(map[string]interface{})
	chatID, _ := extractChatInfo(data)

	// Add user C
	resp = doRequest(t, "POST", fmt.Sprintf("/api/v1/chats/%s/participants", chatID), map[string]interface{}{
		"user_id": userC,
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Remove user C
	resp = doRequest(t, "DELETE", fmt.Sprintf("/api/v1/chats/%s/participants/%s", chatID, userC), nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestChat_ChangeRole(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553017")
	_, _, userB := registerUser(t, "+14155553018")

	// Create group
	resp := doRequest(t, "POST", "/api/v1/chats", map[string]interface{}{
		"name":       "Role Test Group",
		"member_ids": []string{userB},
	}, tokenA)
	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)
	body := parseResponse(t, resp)
	data := body["data"].(map[string]interface{})
	chatID, _ := extractChatInfo(data)

	// Promote user B to admin
	resp = doRequest(t, "PATCH", fmt.Sprintf("/api/v1/chats/%s/participants/%s/role", chatID, userB), map[string]interface{}{
		"role": "admin",
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Demote user B back to member
	resp = doRequest(t, "PATCH", fmt.Sprintf("/api/v1/chats/%s/participants/%s/role", chatID, userB), map[string]interface{}{
		"role": "member",
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestChat_MuteAndPin(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155553023")
	_, _, userB := registerUser(t, "+14155553024")

	chatID := createDirectChat(t, tokenA, userB)

	// Mute chat
	resp := doRequest(t, "PUT", fmt.Sprintf("/api/v1/chats/%s/mute", chatID), map[string]interface{}{
		"muted": true,
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Unmute chat
	resp = doRequest(t, "PUT", fmt.Sprintf("/api/v1/chats/%s/mute", chatID), map[string]interface{}{
		"muted": false,
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Pin chat
	resp = doRequest(t, "PUT", fmt.Sprintf("/api/v1/chats/%s/pin", chatID), map[string]interface{}{
		"pinned": true,
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Unpin chat
	resp = doRequest(t, "PUT", fmt.Sprintf("/api/v1/chats/%s/pin", chatID), map[string]interface{}{
		"pinned": false,
	}, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}
