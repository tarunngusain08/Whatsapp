package model

type CreateDirectChatRequest struct {
	OtherUserID string `json:"other_user_id" binding:"required"`
}

type CreateGroupRequest struct {
	Name        string   `json:"name"        binding:"required"`
	Description string   `json:"description"`
	MemberIDs   []string `json:"member_ids"  binding:"required"`
}

type UpdateGroupRequest struct {
	Name        *string `json:"name"`
	Description *string `json:"description"`
	AvatarURL   *string `json:"avatar_url"`
	IsAdminOnly *bool   `json:"is_admin_only"`
}

type AddMemberRequest struct {
	UserID string `json:"user_id" binding:"required"`
}

type ChatListItem struct {
	Chat         Chat              `json:"chat"`
	Participants []ChatParticipant `json:"participants"`
	Group        *Group            `json:"group,omitempty"`
	LastMessage  *MessagePreview   `json:"last_message,omitempty"`
	UnreadCount  int64             `json:"unread_count"`
}

type MessagePreview struct {
	MessageID string `json:"message_id"`
	SenderID  string `json:"sender_id"`
	Type      string `json:"type"`
	Body      string `json:"body"`
	CreatedAt int64  `json:"created_at"`
}
