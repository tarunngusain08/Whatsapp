package service

import (
	"fmt"
	"io"
	"net/http"
	"strings"
)

// AllowedImageTypes lists accepted image MIME types.
var AllowedImageTypes = map[string]bool{
	"image/jpeg": true,
	"image/png":  true,
	"image/gif":  true,
	"image/webp": true,
}

// AllowedVideoTypes lists accepted video MIME types.
var AllowedVideoTypes = map[string]bool{
	"video/mp4":        true,
	"video/quicktime":  true,
	"video/webm":       true,
}

// AllowedAudioTypes lists accepted audio MIME types.
var AllowedAudioTypes = map[string]bool{
	"audio/mpeg": true,
	"audio/ogg":  true,
	"audio/wav":  true,
	"audio/aac":  true,
	"audio/mp4":  true,
}

// AllowedDocTypes lists accepted document MIME types.
var AllowedDocTypes = map[string]bool{
	"application/pdf":   true,
	"application/msword": true,
	"text/plain":        true,
	"application/zip":   true,
}

// MIMEValidationResult holds the detected MIME type and file type category.
type MIMEValidationResult struct {
	MIMEType string
	FileType string
}

// ValidateMIME reads the first 512 bytes from the reader, detects the content type,
// and validates it against the allowed types. Returns the detected MIME type and
// file type category. The reader must be seekable to reset after detection.
func ValidateMIME(reader io.ReadSeeker) (*MIMEValidationResult, error) {
	buf := make([]byte, 512)
	n, err := reader.Read(buf)
	if err != nil && err != io.EOF {
		return nil, fmt.Errorf("failed to read file header: %w", err)
	}

	mimeType := http.DetectContentType(buf[:n])

	// Reset reader to the beginning.
	if _, err := reader.Seek(0, io.SeekStart); err != nil {
		return nil, fmt.Errorf("failed to reset reader: %w", err)
	}

	fileType := classifyMIMEType(mimeType)
	if fileType == "" {
		return nil, fmt.Errorf("unsupported file type: %s", mimeType)
	}

	return &MIMEValidationResult{
		MIMEType: mimeType,
		FileType: fileType,
	}, nil
}

// ValidateSize checks if the file size is within the allowed limits for the category.
func ValidateSize(fileType string, size int64, maxImage, maxVideo, maxAudio, maxDoc int64) error {
	var maxSize int64
	switch fileType {
	case "image":
		maxSize = maxImage
	case "video":
		maxSize = maxVideo
	case "audio":
		maxSize = maxAudio
	case "document":
		maxSize = maxDoc
	default:
		return fmt.Errorf("unknown file type: %s", fileType)
	}

	if size > maxSize {
		return fmt.Errorf("file too large: %d bytes exceeds %d byte limit for %s", size, maxSize, fileType)
	}
	return nil
}

// classifyMIMEType returns the file type category for a given MIME type.
func classifyMIMEType(mimeType string) string {
	if AllowedImageTypes[mimeType] {
		return "image"
	}
	if AllowedVideoTypes[mimeType] {
		return "video"
	}
	if AllowedAudioTypes[mimeType] {
		return "audio"
	}
	if AllowedDocTypes[mimeType] {
		return "document"
	}
	// Handle openxmlformats document types with prefix matching.
	if strings.HasPrefix(mimeType, "application/vnd.openxmlformats-") {
		return "document"
	}
	if strings.HasPrefix(mimeType, "application/vnd.ms-") {
		return "document"
	}
	return ""
}
