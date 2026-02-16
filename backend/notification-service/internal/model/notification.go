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
	MessageID      string   `json:"message_id"`
	ChatID         string   `json:"chat_id"`
	SenderID       string   `json:"sender_id"`
	SenderName     string   `json:"sender_name"`
	Type           string   `json:"type"`
	Body           string   `json:"body"`
	ChatName       string   `json:"chat_name"`
	IsGroup        bool     `json:"is_group"`
	ParticipantIDs []string `json:"participant_ids"`
}

// MemberEvent is the NATS event received when a user is added to a group.
type MemberEvent struct {
	ChatID    string `json:"chat_id"`
	UserID    string `json:"user_id"`
	AddedBy   string `json:"added_by"`
	GroupName string `json:"group_name"`
}
