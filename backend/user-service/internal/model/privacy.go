package model

import "time"

type PrivacyVisibility string

const (
	VisibilityEveryone PrivacyVisibility = "everyone"
	VisibilityContacts PrivacyVisibility = "contacts"
	VisibilityNobody   PrivacyVisibility = "nobody"
)

type PrivacySettings struct {
	UserID       string            `json:"user_id"        db:"user_id"`
	LastSeen     PrivacyVisibility `json:"last_seen"      db:"last_seen"`
	ProfilePhoto PrivacyVisibility `json:"profile_photo"  db:"profile_photo"`
	About        PrivacyVisibility `json:"about"          db:"about"`
	ReadReceipts bool              `json:"read_receipts"  db:"read_receipts"`
	UpdatedAt    time.Time         `json:"updated_at"     db:"updated_at"`
}
