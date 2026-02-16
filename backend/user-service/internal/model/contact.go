package model

import "time"

type Contact struct {
	ID        string    `json:"id"         db:"id"`
	UserID    string    `json:"user_id"    db:"user_id"`
	ContactID string    `json:"contact_id" db:"contact_id"`
	Nickname  string    `json:"nickname"   db:"nickname"`
	IsBlocked bool      `json:"is_blocked" db:"is_blocked"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
}
