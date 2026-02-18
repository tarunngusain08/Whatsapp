package service

import (
	"context"
	"io"
	"mime/multipart"
	"time"

	"github.com/whatsapp-clone/backend/media-service/internal/model"
)

// MediaService defines business operations for media management.
type MediaService interface {
	Upload(ctx context.Context, uploaderID string, fh *multipart.FileHeader) (*model.UploadResult, error)
	GetMetadata(ctx context.Context, mediaID string) (*model.Media, string, string, error)
	GetDownloadURL(ctx context.Context, mediaID string, expiry time.Duration) (string, error)
	StreamFile(ctx context.Context, mediaID string) (io.ReadCloser, string, int64, error)
	DeleteMedia(ctx context.Context, mediaID string) error
	StartCleanupJob(ctx context.Context)
}
