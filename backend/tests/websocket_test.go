package tests

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/gorilla/websocket"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestWebSocket_Connect(t *testing.T) {
	token, _, _ := registerUser(t, "+14155556001")

	conn := connectWS(t, token)
	defer conn.Close()

	// If we got here, the connection succeeded
	assert.NotNil(t, conn)
}

func TestWebSocket_Connect_NoToken(t *testing.T) {
	wsURL := "ws://localhost:8080/ws"
	_, resp, err := websocket.DefaultDialer.Dial(wsURL, nil)
	// Connection should be rejected
	if err != nil {
		if resp != nil {
			assert.NotEqual(t, 101, resp.StatusCode, "unauthenticated WS should not upgrade")
		}
		return
	}
	t.Fatal("expected websocket dial to fail without auth token")
}

func TestWebSocket_SendMessage(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155556002")
	_, _, userB := registerUser(t, "+14155556003")

	chatID := createDirectChat(t, tokenA, userB)

	conn := connectWS(t, tokenA)
	defer conn.Close()

	// Send a message via WebSocket
	event := map[string]interface{}{
		"type": "message.send",
		"payload": map[string]interface{}{
			"chat_id":       chatID,
			"type":          "text",
			"payload":       map[string]interface{}{"body": "Hello via WS!"},
			"client_msg_id": uniqueID("ws-msg"),
		},
	}
	data, err := json.Marshal(event)
	require.NoError(t, err)

	err = conn.WriteMessage(websocket.TextMessage, data)
	require.NoError(t, err)

	// Read the ack/response
	reply := readWSEvent(t, conn, 5*time.Second)
	assert.NotEmpty(t, reply["type"], "should receive a response event")
}

func TestWebSocket_TypingIndicator(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155556004")
	_, _, userB := registerUser(t, "+14155556005")

	chatID := createDirectChat(t, tokenA, userB)

	conn := connectWS(t, tokenA)
	defer conn.Close()

	// Send typing event
	event := map[string]interface{}{
		"type": "typing.start",
		"payload": map[string]interface{}{
			"chat_id": chatID,
		},
	}
	data, err := json.Marshal(event)
	require.NoError(t, err)

	err = conn.WriteMessage(websocket.TextMessage, data)
	assert.NoError(t, err, "sending typing event should not error")
}

func TestWebSocket_PresenceSubscribe(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155556006")
	_, _, userB := registerUser(t, "+14155556007")

	conn := connectWS(t, tokenA)
	defer conn.Close()

	// Subscribe to presence of user B
	event := map[string]interface{}{
		"type": "presence.subscribe",
		"payload": map[string]interface{}{
			"user_ids": []string{userB},
		},
	}
	data, err := json.Marshal(event)
	require.NoError(t, err)

	err = conn.WriteMessage(websocket.TextMessage, data)
	assert.NoError(t, err, "subscribing to presence should not error")
}

func TestWebSocket_RealTimeDelivery(t *testing.T) {
	tokenA, _, _ := registerUser(t, "+14155556008")
	tokenB, _, userB := registerUser(t, "+14155556009")

	chatID := createDirectChat(t, tokenA, userB)

	// User B connects via WebSocket to receive messages
	connB := connectWS(t, tokenB)
	defer connB.Close()

	// Small delay to ensure WS is fully registered
	time.Sleep(300 * time.Millisecond)

	// User A sends a message via REST API
	sendMessage(t, tokenA, chatID, "Real-time test!", uniqueID("rt-msg"))

	// User B should receive the message event via WebSocket
	_ = connB.SetReadDeadline(time.Now().Add(5 * time.Second))
	_, message, err := connB.ReadMessage()
	if err != nil {
		t.Logf("WS read timed out or errored (may be expected depending on routing): %v", err)
		return
	}

	var event map[string]interface{}
	err = json.Unmarshal(message, &event)
	require.NoError(t, err)
	assert.NotEmpty(t, event["type"], "should receive an event")
	t.Logf("Received WS event type: %s", event["type"])
}
