package model

import "time"

// FileType represents the category of an uploaded file.
type FileType string

const (
	FileTypeImage    FileType = "image"
	FileTypeVideo    FileType = "video"
	FileTypeAudio    FileType = "audio"
	FileTypeDocument FileType = "document"
)

// Media is the MongoDB document for uploaded media metadata.
type Media struct {
	MediaID          string    `json:"media_id"           bson:"media_id"`
	UploaderID       string    `json:"uploader_id"        bson:"uploader_id"`
	FileType         string    `json:"file_type"          bson:"file_type"`
	MIMEType         string    `json:"mime_type"          bson:"mime_type"`
	OriginalFilename string    `json:"original_filename"  bson:"original_filename"`
	SizeBytes        int64     `json:"size_bytes"         bson:"size_bytes"`
	StorageKey       string    `json:"storage_key"        bson:"storage_key"`
	ThumbnailKey     string    `json:"thumbnail_key"      bson:"thumbnail_key"`
	ChecksumSHA256   string    `json:"checksum_sha256"    bson:"checksum_sha256"`
	Width            int       `json:"width"              bson:"width"`
	Height           int       `json:"height"             bson:"height"`
	DurationMs       int64     `json:"duration_ms"        bson:"duration_ms"`
	CreatedAt        time.Time `json:"created_at"         bson:"created_at"`
	UpdatedAt        time.Time `json:"updated_at"         bson:"updated_at"`
}

// UploadResult is returned after a successful upload.
type UploadResult struct {
	MediaID      string `json:"media_id"`
	URL          string `json:"url"`
	ThumbnailURL string `json:"thumbnail_url,omitempty"`
	Width        int    `json:"width,omitempty"`
	Height       int    `json:"height,omitempty"`
	DurationMs   int64  `json:"duration_ms,omitempty"`
	SizeBytes    int64  `json:"size_bytes"`
	MIMEType     string `json:"mime_type"`
	FileType     string `json:"file_type"`
}

// MediaMetadata is returned for metadata retrieval.
type MediaMetadata struct {
	MediaID      string `json:"media_id"`
	URL          string `json:"url"`
	ThumbnailURL string `json:"thumbnail_url,omitempty"`
	Width        int    `json:"width,omitempty"`
	Height       int    `json:"height,omitempty"`
	DurationMs   int64  `json:"duration_ms,omitempty"`
	SizeBytes    int64  `json:"size_bytes"`
	MIMEType     string `json:"mime_type"`
	FileType     string `json:"file_type"`
}
