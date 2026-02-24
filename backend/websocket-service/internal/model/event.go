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
	Body         string `json:"body,omitempty"`
	MediaID      string `json:"media_id,omitempty"`
	Caption      string `json:"caption,omitempty"`
	Filename     string `json:"filename,omitempty"`
	DurationMs   int64  `json:"duration_ms,omitempty"`
	MediaURL     string `json:"media_url,omitempty"`
	ThumbnailURL string `json:"thumbnail_url,omitempty"`
	MimeType     string `json:"mime_type,omitempty"`
	FileSize     int64  `json:"file_size,omitempty"`
}

type MessageStatusPayload struct {
	MessageID string `json:"message_id"`
	ChatID    string `json:"chat_id,omitempty"`
	SenderID  string `json:"sender_id,omitempty"`
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
	Status    string         `json:"status"`    // "sent" for client MessageDto
	CreatedAt string         `json:"created_at"` // ISO8601 for client parseTimestamp
}

type MessageSentAckPayload struct {
	ClientMsgID string `json:"client_msg_id"`
	MessageID   string `json:"message_id"`
	ChatID      string `json:"chat_id"`
	Timestamp   string `json:"timestamp"` // ISO8601 for client parseTimestamp
	CreatedAt   int64  `json:"created_at"` // Unix ms, kept for backwards compat
}

type PresenceEventPayload struct {
	UserID   string `json:"user_id"`
	Online   bool   `json:"online"`
	LastSeen string `json:"last_seen,omitempty"`
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
