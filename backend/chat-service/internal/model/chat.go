package model

import "time"

type ChatType string

const (
	ChatTypeDirect ChatType = "direct"
	ChatTypeGroup  ChatType = "group"
)

type Chat struct {
	ID        string    `json:"id"         db:"id"`
	Type      ChatType  `json:"type"       db:"type"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
	UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}

type ChatParticipant struct {
	ID              string         `json:"id"                        db:"id"`
	ChatID          string         `json:"chat_id"                   db:"chat_id"`
	UserID          string         `json:"user_id"                   db:"user_id"`
	Role            string         `json:"role"                      db:"role"`
	IsMuted         bool           `json:"is_muted"                  db:"is_muted"`
	MuteUntil       *time.Time     `json:"mute_until"                db:"mute_until"`
	IsPinned        bool           `json:"is_pinned"                 db:"is_pinned"`
	AutoDeleteTimer *time.Duration `json:"auto_delete_timer,omitempty" db:"auto_delete_timer"`
	JoinedAt        time.Time      `json:"joined_at"                 db:"joined_at"`
}

type Group struct {
	ChatID      string    `json:"chat_id"       db:"chat_id"`
	Name        string    `json:"name"          db:"name"`
	Description string    `json:"description"   db:"description"`
	AvatarURL   string    `json:"avatar_url"    db:"avatar_url"`
	CreatedBy   string    `json:"created_by"    db:"created_by"`
	IsAdminOnly bool      `json:"is_admin_only" db:"is_admin_only"`
	CreatedAt   time.Time `json:"created_at"    db:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"    db:"updated_at"`
}
