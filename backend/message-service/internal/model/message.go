package model

import "time"

type MessageType string

const (
	MessageTypeText     MessageType = "text"
	MessageTypeImage    MessageType = "image"
	MessageTypeVideo    MessageType = "video"
	MessageTypeAudio    MessageType = "audio"
	MessageTypeDocument MessageType = "document"
	MessageTypeLocation MessageType = "location"
)

type MessageStatus string

const (
	StatusSent      MessageStatus = "sent"
	StatusDelivered MessageStatus = "delivered"
	StatusRead      MessageStatus = "read"
)

type Reaction struct {
	Emoji     string    `json:"emoji"      bson:"emoji"`
	UserID    string    `json:"user_id"    bson:"user_id"`
	CreatedAt time.Time `json:"created_at" bson:"created_at"`
}

type Message struct {
	MessageID        string                     `json:"message_id"                    bson:"message_id"`
	ChatID           string                     `json:"chat_id"                       bson:"chat_id"`
	SenderID         string                     `json:"sender_id"                     bson:"sender_id"`
	ClientMsgID      string                     `json:"client_msg_id"                 bson:"client_msg_id"`
	Type             MessageType                `json:"type"                          bson:"type"`
	ReplyToMessageID string                     `json:"reply_to_message_id,omitempty" bson:"reply_to_message_id,omitempty"`
	ForwardedFrom    *ForwardedFrom             `json:"forwarded_from,omitempty"      bson:"forwarded_from,omitempty"`
	Payload          MessagePayload             `json:"payload"                       bson:"payload"`
	Status           map[string]RecipientStatus `json:"status"                        bson:"status"`
	Reactions        []Reaction                 `json:"reactions,omitempty"           bson:"reactions,omitempty"`
	IsDeleted        bool                       `json:"is_deleted"                    bson:"is_deleted"`
	IsStarredBy      []string                   `json:"is_starred_by"                 bson:"is_starred_by"`
	ReplyToPreview   *ReplyPreview              `json:"reply_to_preview,omitempty"    bson:"-"`
	CreatedAt        time.Time                  `json:"created_at"                    bson:"created_at"`
	UpdatedAt        time.Time                  `json:"updated_at"                    bson:"updated_at"`
}

type ReplyPreview struct {
	MessageID string      `json:"message_id"`
	SenderID  string      `json:"sender_id"`
	Type      MessageType `json:"type"`
	Body      string      `json:"body"`
}

type ForwardedFrom struct {
	ChatID    string `json:"chat_id"    bson:"chat_id"`
	MessageID string `json:"message_id" bson:"message_id"`
}

type MessagePayload struct {
	Body       string `json:"body,omitempty"        bson:"body,omitempty"`
	MediaID    string `json:"media_id,omitempty"    bson:"media_id,omitempty"`
	Caption    string `json:"caption,omitempty"     bson:"caption,omitempty"`
	Filename   string `json:"filename,omitempty"    bson:"filename,omitempty"`
	DurationMs int64  `json:"duration_ms,omitempty" bson:"duration_ms,omitempty"`
}

type RecipientStatus struct {
	Status    MessageStatus `json:"status"     bson:"status"`
	UpdatedAt time.Time     `json:"updated_at" bson:"updated_at"`
}
