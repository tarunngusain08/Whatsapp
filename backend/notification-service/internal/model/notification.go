package model

// NotificationPayload represents a single FCM push notification.
type NotificationPayload struct {
	Title string            `json:"title"`
	Body  string            `json:"body"`
	Data  map[string]string `json:"data"`
	Token string            `json:"token"`
}

// MessageEvent is the NATS event received when a new message is created.
type MessageEvent struct {
	MessageID      string         `json:"message_id"`
	ChatID         string         `json:"chat_id"`
	SenderID       string         `json:"sender_id"`
	SenderName     string         `json:"sender_name"`
	SenderAvatar   string         `json:"sender_avatar"`
	Type           string         `json:"type"`
	Payload        MessagePayload `json:"payload"`
	ChatName       string         `json:"chat_name"`
	IsGroup        bool           `json:"is_group"`
	ParticipantIDs []string       `json:"participant_ids"`
	CreatedAt      int64          `json:"created_at"`
}

// MessagePayload holds the message content fields from the NATS event.
type MessagePayload struct {
	Body     string `json:"body"`
	MediaID  string `json:"media_id"`
	Caption  string `json:"caption"`
	Filename string `json:"filename"`
}

// CallEvent is the NATS event received when a call is initiated to an offline user.
type CallEvent struct {
	CallID     string `json:"call_id"`
	CallerID   string `json:"caller_id"`
	CallerName string `json:"caller_name"`
	CallType   string `json:"call_type"`
	TargetID   string `json:"target_user_id"`
	AvatarURL  string `json:"avatar_url"`
}

// MemberEvent is the NATS event received when a user is added to a group.
type MemberEvent struct {
	ChatID    string `json:"chat_id"`
	UserID    string `json:"user_id"`
	AddedBy   string `json:"added_by"`
	GroupName string `json:"group_name"`
}
