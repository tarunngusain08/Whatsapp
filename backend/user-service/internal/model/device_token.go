package model

import "time"

type DeviceToken struct {
	ID        string    `json:"id"         db:"id"`
	UserID    string    `json:"user_id"    db:"user_id"`
	Token     string    `json:"token"      db:"token"`
	Platform  string    `json:"platform"   db:"platform"` // "android", "ios", "web"
	CreatedAt time.Time `json:"created_at" db:"created_at"`
	UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}
