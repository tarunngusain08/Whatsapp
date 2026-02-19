package model

import "encoding/json"

// WSEvent represents a WebSocket message envelope (both client->server and server->client).
type WSEvent struct {
	Type    string          `json:"event"`
	Payload json.RawMessage `json:"data"`
}

// --- Client -> Server event payloads ---

type MessageSendPayload struct {
	ChatID           string         `json:"chat_id"`
	Type             string         `json:"type"`
	Payload          MessageContent `json:"payload"`
	ClientMsgID      string         `json:"client_msg_id"`
	ReplyToMessageID string         `json:"reply_to_message_id,omitempty"`
}

type MessageContent struct {
	Body       string `json:"body,omitempty"`
	MediaID    string `json:"media_id,omitempty"`
	Caption    string `json:"caption,omitempty"`
	Filename   string `json:"filename,omitempty"`
	DurationMs int64  `json:"duration_ms,omitempty"`
}

type MessageStatusPayload struct {
	MessageID string `json:"message_id"`
	ChatID    string `json:"chat_id,omitempty"`
	Status    string `json:"status"` // "delivered" or "read"
}

type MessageDeletePayload struct {
	MessageID   string `json:"message_id"`
	ChatID      string `json:"chat_id"`
	ForEveryone bool   `json:"for_everyone"`
}

type TypingPayload struct {
	ChatID string `json:"chat_id"`
}

type PresenceSubscribePayload struct {
	UserIDs []string `json:"user_ids"`
}

// --- Server -> Client event payloads ---

type MessageReactionPayload struct {
	MessageID string `json:"message_id"`
	ChatID    string `json:"chat_id"`
	UserID    string `json:"user_id"`
	Emoji     string `json:"emoji"`
	Removed   bool   `json:"removed"`
}

type MessageNewPayload struct {
	MessageID string         `json:"message_id"`
	ChatID    string         `json:"chat_id"`
	SenderID  string         `json:"sender_id"`
	Type      string         `json:"type"`
	Payload   MessageContent `json:"payload"`
	CreatedAt int64          `json:"created_at"`
}

type MessageSentAckPayload struct {
	ClientMsgID string `json:"client_msg_id"`
	MessageID   string `json:"message_id"`
	CreatedAt   int64  `json:"created_at"`
}

type PresenceEventPayload struct {
	UserID string `json:"user_id"`
	Online bool   `json:"online"`
}

// --- Call signaling payloads ---

type CallOfferPayload struct {
	CallID       string `json:"call_id"`
	TargetUserID string `json:"target_user_id"`
	SDP          string `json:"sdp"`
	CallType     string `json:"call_type"` // "audio" or "video"
}

type CallAnswerPayload struct {
	CallID       string `json:"call_id"`
	TargetUserID string `json:"target_user_id"`
	SDP          string `json:"sdp"`
}

type CallIceCandidatePayload struct {
	CallID       string `json:"call_id"`
	TargetUserID string `json:"target_user_id"`
	Candidate    string `json:"candidate"`
}

type CallEndPayload struct {
	CallID       string `json:"call_id"`
	TargetUserID string `json:"target_user_id"`
	Reason       string `json:"reason,omitempty"` // "hangup", "declined", "timeout"
}
