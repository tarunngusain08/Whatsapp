package model

import "time"

type User struct {
	ID          string    `json:"id"           db:"id"`
	Phone       string    `json:"phone"        db:"phone"`
	DisplayName string    `json:"display_name" db:"display_name"`
	AvatarURL   string    `json:"avatar_url"   db:"avatar_url"`
	StatusText  string    `json:"status_text"  db:"status_text"`
	CreatedAt   time.Time `json:"created_at"   db:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"   db:"updated_at"`
}
