package repository

import (
	"context"
	"time"
)

type PresenceRepository interface {
	// SetOnline marks the user as online with the given TTL.
	SetOnline(ctx context.Context, userID string, ttl time.Duration) error

	// IsOnline returns true if the user has an active presence key.
	IsOnline(ctx context.Context, userID string) (bool, error)

	// SetLastSeen stores the last-seen timestamp (called when presence expires or user disconnects).
	SetLastSeen(ctx context.Context, userID string, t time.Time) error

	// GetLastSeen retrieves the last-seen timestamp for a user.
	GetLastSeen(ctx context.Context, userID string) (time.Time, error)

	// Remove deletes the presence key (explicit offline).
	Remove(ctx context.Context, userID string) error
}
