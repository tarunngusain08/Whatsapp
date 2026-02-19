package model

import "time"

type Status struct {
	ID        string    `json:"id"         db:"id"`
	UserID    string    `json:"user_id"    db:"user_id"`
	Type      string    `json:"type"       db:"type"`
	Content   string    `json:"content"    db:"content"`
	Caption   string    `json:"caption,omitempty"  db:"caption"`
	BgColor   string    `json:"bg_color,omitempty" db:"bg_color"`
	Viewers   []string  `json:"viewers"    db:"-"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
	ExpiresAt time.Time `json:"expires_at" db:"expires_at"`
}

type CreateStatusRequest struct {
	Type    string `json:"type"    binding:"required"`
	Content string `json:"content" binding:"required"`
	Caption string `json:"caption,omitempty"`
	BgColor string `json:"bg_color,omitempty"`
}
