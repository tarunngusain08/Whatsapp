package service

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/media-service/internal/repository"
)

// ThumbnailGenerator handles thumbnail creation using FFmpeg.
type ThumbnailGenerator struct {
	ffmpegPath  string
	storageRepo repository.StorageRepository
	log         zerolog.Logger
}

func NewThumbnailGenerator(ffmpegPath string, storageRepo repository.StorageRepository, log zerolog.Logger) *ThumbnailGenerator {
	return &ThumbnailGenerator{
		ffmpegPath:  ffmpegPath,
		storageRepo: storageRepo,
		log:         log,
	}
}

// Generate creates a thumbnail for images and videos. Returns the storage key
// of the generated thumbnail, or empty string if not applicable.
func (t *ThumbnailGenerator) Generate(ctx context.Context, storageKey, fileType, mediaID string) (string, error) {
	if fileType != "image" && fileType != "video" {
		return "", nil
	}

	// Check if ffmpeg is available.
	if _, err := exec.LookPath(t.ffmpegPath); err != nil {
		t.log.Warn().Msg("ffmpeg not found, skipping thumbnail generation")
		return "", nil
	}

	tmpDir := os.TempDir()
	originalPath := filepath.Join(tmpDir, "orig-"+mediaID)
	thumbPath := filepath.Join(tmpDir, "thumb-"+mediaID+".jpg")
	defer os.Remove(originalPath)
	defer os.Remove(thumbPath)

	// Download original from MinIO to a temp file.
	downloadURL, err := t.storageRepo.PresignedURL(ctx, storageKey, 5*time.Minute)
	if err != nil {
		return "", fmt.Errorf("failed to get presigned URL for thumbnail: %w", err)
	}

	if err := downloadFile(downloadURL, originalPath); err != nil {
		return "", fmt.Errorf("failed to download original for thumbnail: %w", err)
	}

	var cmd *exec.Cmd
	switch fileType {
	case "image":
		cmd = exec.CommandContext(ctx, t.ffmpegPath,
			"-i", originalPath,
			"-vf", "scale=200:-1",
			"-frames:v", "1",
			"-y", thumbPath,
		)
	case "video":
		cmd = exec.CommandContext(ctx, t.ffmpegPath,
			"-i", originalPath,
			"-vf", "scale=200:-1",
			"-frames:v", "1",
			"-y", thumbPath,
		)
	default:
		return "", nil
	}

	var stderr bytes.Buffer
	cmd.Stderr = &stderr
	if err := cmd.Run(); err != nil {
		return "", fmt.Errorf("ffmpeg failed: %s: %w", stderr.String(), err)
	}

	// Upload thumbnail to MinIO.
	thumbKey := "thumbnails/" + mediaID + ".jpg"
	thumbFile, err := os.Open(thumbPath)
	if err != nil {
		return "", fmt.Errorf("failed to open thumbnail: %w", err)
	}
	defer thumbFile.Close()

	stat, err := thumbFile.Stat()
	if err != nil {
		return "", fmt.Errorf("failed to stat thumbnail: %w", err)
	}

	if err := t.storageRepo.Upload(ctx, thumbKey, thumbFile, stat.Size(), "image/jpeg"); err != nil {
		return "", fmt.Errorf("failed to upload thumbnail: %w", err)
	}

	return thumbKey, nil
}

var downloadClient = &http.Client{Timeout: 30 * time.Second}

// downloadFile fetches a URL and writes it to the given file path.
func downloadFile(fileURL, dest string) error {
	req, err := http.NewRequest(http.MethodGet, fileURL, nil)
	if err != nil {
		return err
	}
	resp, err := downloadClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("download returned status %d", resp.StatusCode)
	}

	out, err := os.Create(dest)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	return err
}
