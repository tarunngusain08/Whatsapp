package tests

import (
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMessage_SendAndRetrieve(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554001")
	tokenB, _, userB := registerUser(t, "+14155554002")

	chatID := createDirectChat(t, tokenA, userB)

	// Send message
	clientMsgID := uniqueID("test-msg")
	msgID := sendMessage(t, tokenA, chatID, "Hello, World!", clientMsgID)
	assert.NotEmpty(t, msgID)

	// Allow async persistence
	time.Sleep(500 * time.Millisecond)

	// Retrieve messages as user B
	resp := doRequest(t, "GET", fmt.Sprintf("/api/v1/messages?chat_id=%s", chatID), nil, tokenB)
	require.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	require.True(t, body["success"].(bool))

	dataRaw := body["data"]
	require.NotNil(t, dataRaw, "data should not be nil, chatID=%s", chatID)

	messages := extractMessageList(t, dataRaw)
	assert.GreaterOrEqual(t, len(messages), 1, "should have at least one message")

	// Verify message content
	found := false
	for _, m := range messages {
		msg := m.(map[string]interface{})
		if msg["client_msg_id"] == clientMsgID {
			found = true
			payload := msg["payload"].(map[string]interface{})
			assert.Equal(t, "Hello, World!", payload["body"])
			assert.Equal(t, "text", msg["type"])
			break
		}
	}
	assert.True(t, found, "sent message should be retrievable")
}

func TestMessage_Idempotent(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554003")
	_, _, userB := registerUser(t, "+14155554004")

	chatID := createDirectChat(t, tokenA, userB)

	// Send same message twice with the same client_msg_id
	dedupID := uniqueID("dedup")
	for i := 0; i < 2; i++ {
		resp := doRequest(t, "POST", "/api/v1/messages", map[string]interface{}{
			"chat_id":       chatID,
			"type":          "text",
			"payload":       map[string]interface{}{"body": "Dedup test"},
			"client_msg_id": dedupID,
		}, tokenA)
		assert.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)
		_ = parseResponse(t, resp)
	}

	time.Sleep(500 * time.Millisecond)

	// Retrieve and count messages with that client_msg_id
	resp := doRequest(t, "GET", fmt.Sprintf("/api/v1/messages?chat_id=%s", chatID), nil, tokenA)
	require.Equal(t, http.StatusOK, resp.StatusCode)
	body := parseResponse(t, resp)
	require.NotNil(t, body["data"], "data should not be nil")
	messages := extractMessageList(t, body["data"])

	count := 0
	for _, m := range messages {
		msg := m.(map[string]interface{})
		if msg["client_msg_id"] == dedupID {
			count++
		}
	}
	assert.Equal(t, 1, count, "duplicate client_msg_id should result in only one message")
}

func TestMessage_Delete_ForEveryone(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554005")
	_, _, userB := registerUser(t, "+14155554006")

	chatID := createDirectChat(t, tokenA, userB)

	// Send message
	msgID := sendMessage(t, tokenA, chatID, "Delete me", uniqueID("delete"))

	// Delete for everyone
	resp := doRequest(t, "DELETE", fmt.Sprintf("/api/v1/messages/%s?for=everyone", msgID), nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestMessage_Delete_ForMe(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554007")
	_, _, userB := registerUser(t, "+14155554008")

	chatID := createDirectChat(t, tokenA, userB)

	msgID := sendMessage(t, tokenA, chatID, "Delete for me only", uniqueID("delete-me"))

	// Delete for me (default)
	resp := doRequest(t, "DELETE", fmt.Sprintf("/api/v1/messages/%s?for=me", msgID), nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestMessage_Pagination(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554009")
	_, _, userB := registerUser(t, "+14155554010")

	chatID := createDirectChat(t, tokenA, userB)

	// Send multiple messages
	for i := 0; i < 5; i++ {
		sendMessage(t, tokenA, chatID, fmt.Sprintf("Pagination msg %d", i), uniqueID(fmt.Sprintf("page-msg-%d", i)))
	}

	time.Sleep(500 * time.Millisecond)

	// Retrieve with limit
	resp := doRequest(t, "GET", fmt.Sprintf("/api/v1/messages?chat_id=%s&limit=3", chatID), nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	require.NotNil(t, body["data"], "data should not be nil")
	messages := extractMessageList(t, body["data"])
	assert.LessOrEqual(t, len(messages), 3, "limit should be respected")

	// Check pagination info in PaginatedData format or legacy meta
	data := body["data"].(map[string]interface{})
	if data["hasMore"] != nil || data["nextCursor"] != nil {
		// New PaginatedData format
		assert.NotNil(t, data["hasMore"], "hasMore should be present in PaginatedData")
	} else if meta, ok := body["meta"].(map[string]interface{}); ok {
		_, hasNextCursor := meta["next_cursor"]
		assert.True(t, hasNextCursor || meta["has_more"] != nil, "pagination metadata should be present")
	}
}

func TestMessage_Star(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554011")
	_, _, userB := registerUser(t, "+14155554012")

	chatID := createDirectChat(t, tokenA, userB)
	msgID := sendMessage(t, tokenA, chatID, "Star me", uniqueID("star"))

	// Star message
	resp := doRequest(t, "POST", fmt.Sprintf("/api/v1/messages/%s/star", msgID), nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)

	// Unstar message
	resp = doRequest(t, "DELETE", fmt.Sprintf("/api/v1/messages/%s/star", msgID), nil, tokenA)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestMessage_Forward(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554013")
	_, _, userB := registerUser(t, "+14155554014")
	_, _, userC := registerUser(t, "+14155554015")

	chatAB := createDirectChat(t, tokenA, userB)
	chatAC := createDirectChat(t, tokenA, userC)

	// Send original message
	msgID := sendMessage(t, tokenA, chatAB, "Forward this", uniqueID("forward"))

	// Forward to another chat
	resp := doRequest(t, "POST", fmt.Sprintf("/api/v1/messages/%s/forward", msgID), map[string]interface{}{
		"target_chat_ids": []string{chatAC},
	}, tokenA)
	assert.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
}

func TestMessage_MarkAsRead(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554016")
	tokenB, _, userB := registerUser(t, "+14155554017")

	chatID := createDirectChat(t, tokenA, userB)
	sendMessage(t, tokenA, chatID, "Read me", uniqueID("read"))

	time.Sleep(500 * time.Millisecond)

	// User B marks messages as read
	resp := doRequest(t, "POST", "/api/v1/messages/read", map[string]interface{}{
		"chat_id": chatID,
	}, tokenB)
	assert.Equal(t, http.StatusNoContent, resp.StatusCode)
	_ = parseResponseRaw(t, resp)
}

func TestMessage_Search(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155554018")
	_, _, userB := registerUser(t, "+14155554019")

	chatID := createDirectChat(t, tokenA, userB)
	sendMessage(t, tokenA, chatID, "searchable keyword alpha", uniqueID("search-1"))
	sendMessage(t, tokenA, chatID, "another message beta", uniqueID("search-2"))

	time.Sleep(500 * time.Millisecond)

	// Search for "alpha"
	resp := doRequest(t, "GET", fmt.Sprintf("/api/v1/messages/search?chat_id=%s&q=alpha", chatID), nil, tokenA)
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	body := parseResponse(t, resp)
	assert.True(t, body["success"].(bool))
	require.NotNil(t, body["data"], "search data should not be nil")
	results := body["data"].([]interface{})
	assert.GreaterOrEqual(t, len(results), 1, "search should find at least one matching message")
}
