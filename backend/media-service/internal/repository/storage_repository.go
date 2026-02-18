package repository

import (
	"context"
	"io"
	"time"
)

// StorageRepository defines object storage operations.
type StorageRepository interface {
	Upload(ctx context.Context, key string, reader io.Reader, size int64, contentType string) error
	Delete(ctx context.Context, key string) error
	PresignedURL(ctx context.Context, key string, expiry time.Duration) (string, error)
	GetObject(ctx context.Context, key string) (io.ReadCloser, string, int64, error)
}
