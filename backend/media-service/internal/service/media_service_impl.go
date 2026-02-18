package service

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/media-service/config"
	"github.com/whatsapp-clone/backend/media-service/internal/model"
	"github.com/whatsapp-clone/backend/media-service/internal/repository"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

type mediaServiceImpl struct {
	cfg          *config.Config
	mediaRepo    repository.MediaRepository
	storageRepo  repository.StorageRepository
	thumbGen     *ThumbnailGenerator
	presignedTTL time.Duration
	log          zerolog.Logger
}

func NewMediaService(
	cfg *config.Config,
	mediaRepo repository.MediaRepository,
	storageRepo repository.StorageRepository,
	thumbGen *ThumbnailGenerator,
	log zerolog.Logger,
) MediaService {
	return &mediaServiceImpl{
		cfg:          cfg,
		mediaRepo:    mediaRepo,
		storageRepo:  storageRepo,
		thumbGen:     thumbGen,
		presignedTTL: cfg.PresignedURLTTL,
		log:          log,
	}
}

func (s *mediaServiceImpl) Upload(ctx context.Context, uploaderID string, fh *multipart.FileHeader) (*model.UploadResult, error) {
	file, err := fh.Open()
	if err != nil {
		return nil, apperr.NewBadRequest("failed to open uploaded file")
	}
	defer file.Close()

	// Detect MIME type from file content.
	buf := make([]byte, 512)
	n, err := file.Read(buf)
	if err != nil {
		return nil, apperr.NewBadRequest("failed to read file")
	}

	mimeType := detectContentType(buf[:n])

	// Reset reader position.
	if _, err := file.Seek(0, io.SeekStart); err != nil {
		return nil, apperr.NewInternal("failed to reset file reader", err)
	}

	// Determine file type category and validate.
	fileType, maxSize := s.classifyMIME(mimeType)
	if fileType == "" {
		return nil, &apperr.AppError{
			Code:       apperr.CodeMediaInvalidType,
			Message:    "unsupported file type: " + mimeType,
			HTTPStatus: 400,
		}
	}
	if fh.Size > maxSize {
		return nil, &apperr.AppError{
			Code:       apperr.CodeMediaTooLarge,
			Message:    fmt.Sprintf("file too large, max %d bytes", maxSize),
			HTTPStatus: 413,
		}
	}

	// Compute SHA-256 checksum while reading.
	hasher := sha256.New()
	teeReader := io.TeeReader(file, hasher)

	mediaID := uuid.New().String()
	storageKey := fmt.Sprintf("%s/%s/%s", fileType, time.Now().Format("2006/01/02"), mediaID)

	// Upload original to MinIO.
	if err := s.storageRepo.Upload(ctx, storageKey, teeReader, fh.Size, mimeType); err != nil {
		return nil, apperr.NewInternal("failed to upload to storage", err)
	}
	checksum := hex.EncodeToString(hasher.Sum(nil))

	// Generate thumbnail for images and videos.
	var thumbnailKey string
	if fileType == "image" || fileType == "video" {
		thumbKey, err := s.thumbGen.Generate(ctx, storageKey, fileType, mediaID)
		if err != nil {
			s.log.Warn().Err(err).Msg("thumbnail generation failed, continuing without")
		} else {
			thumbnailKey = thumbKey
		}
	}

	now := time.Now()
	media := &model.Media{
		MediaID:          mediaID,
		UploaderID:       uploaderID,
		FileType:         fileType,
		MIMEType:         mimeType,
		OriginalFilename: fh.Filename,
		SizeBytes:        fh.Size,
		ChecksumSHA256:   checksum,
		StorageKey:       storageKey,
		ThumbnailKey:     thumbnailKey,
		CreatedAt:        now,
		UpdatedAt:        now,
	}

	if err := s.mediaRepo.Insert(ctx, media); err != nil {
		return nil, apperr.NewInternal("failed to save media metadata", err)
	}

	// Generate presigned URLs.
	downloadURL, _ := s.storageRepo.PresignedURL(ctx, storageKey, s.presignedTTL)
	var thumbURL string
	if thumbnailKey != "" {
		thumbURL, _ = s.storageRepo.PresignedURL(ctx, thumbnailKey, s.presignedTTL)
	}

	return &model.UploadResult{
		MediaID:      mediaID,
		URL:          downloadURL,
		ThumbnailURL: thumbURL,
		SizeBytes:    fh.Size,
		MIMEType:     mimeType,
		FileType:     fileType,
	}, nil
}

func (s *mediaServiceImpl) GetMetadata(ctx context.Context, mediaID string) (*model.Media, string, string, error) {
	media, err := s.mediaRepo.GetByID(ctx, mediaID)
	if err != nil {
		return nil, "", "", apperr.NewInternal("failed to get media", err)
	}
	if media == nil {
		return nil, "", "", apperr.NewNotFound("media not found")
	}

	url, _ := s.storageRepo.PresignedURL(ctx, media.StorageKey, s.presignedTTL)
	var thumbURL string
	if media.ThumbnailKey != "" {
		thumbURL, _ = s.storageRepo.PresignedURL(ctx, media.ThumbnailKey, s.presignedTTL)
	}

	return media, url, thumbURL, nil
}

func (s *mediaServiceImpl) GetDownloadURL(ctx context.Context, mediaID string, expiry time.Duration) (string, error) {
	media, err := s.mediaRepo.GetByID(ctx, mediaID)
	if err != nil {
		return "", apperr.NewInternal("failed to get media", err)
	}
	if media == nil {
		return "", apperr.NewNotFound("media not found")
	}

	url, err := s.storageRepo.PresignedURL(ctx, media.StorageKey, expiry)
	if err != nil {
		return "", apperr.NewInternal("failed to generate download URL", err)
	}
	return url, nil
}

func (s *mediaServiceImpl) StreamFile(ctx context.Context, mediaID string) (io.ReadCloser, string, int64, error) {
	media, err := s.mediaRepo.GetByID(ctx, mediaID)
	if err != nil {
		return nil, "", 0, apperr.NewInternal("failed to get media", err)
	}
	if media == nil {
		return nil, "", 0, apperr.NewNotFound("media not found")
	}

	reader, contentType, size, err := s.storageRepo.GetObject(ctx, media.StorageKey)
	if err != nil {
		return nil, "", 0, apperr.NewInternal("failed to stream file from storage", err)
	}
	return reader, contentType, size, nil
}

func (s *mediaServiceImpl) DeleteMedia(ctx context.Context, mediaID string) error {
	media, err := s.mediaRepo.GetByID(ctx, mediaID)
	if err != nil {
		return apperr.NewInternal("failed to get media", err)
	}
	if media == nil {
		return apperr.NewNotFound("media not found")
	}

	// Delete from storage.
	if err := s.storageRepo.Delete(ctx, media.StorageKey); err != nil {
		s.log.Error().Err(err).Str("key", media.StorageKey).Msg("failed to delete from storage")
	}
	if media.ThumbnailKey != "" {
		if err := s.storageRepo.Delete(ctx, media.ThumbnailKey); err != nil {
			s.log.Error().Err(err).Str("key", media.ThumbnailKey).Msg("failed to delete thumbnail from storage")
		}
	}

	// Delete metadata from MongoDB.
	if err := s.mediaRepo.Delete(ctx, mediaID); err != nil {
		return apperr.NewInternal("failed to delete media metadata", err)
	}

	return nil
}

// classifyMIME returns the file type category and max allowed size.
func (s *mediaServiceImpl) classifyMIME(mimeType string) (string, int64) {
	switch {
	case strings.HasPrefix(mimeType, "image/"):
		return "image", s.cfg.MaxImageSize
	case strings.HasPrefix(mimeType, "video/"):
		return "video", s.cfg.MaxVideoSize
	case strings.HasPrefix(mimeType, "audio/"):
		return "audio", s.cfg.MaxAudioSize
	case mimeType == "application/pdf",
		mimeType == "application/msword",
		mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		mimeType == "application/vnd.ms-excel",
		mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		mimeType == "text/plain",
		mimeType == "application/zip":
		return "document", s.cfg.MaxDocSize
	default:
		return "", 0
	}
}

// detectContentType wraps http.DetectContentType.
func detectContentType(data []byte) string {
	return http.DetectContentType(data)
}
