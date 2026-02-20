package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"

	"github.com/whatsapp-clone/backend/media-service/internal/service"
)

type HTTPHandler struct {
	mediaSvc       service.MediaService
	presignedExpiry time.Duration
	log            zerolog.Logger
}

func NewHTTPHandler(mediaSvc service.MediaService, presignedExpiry time.Duration, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{
		mediaSvc:       mediaSvc,
		presignedExpiry: presignedExpiry,
		log:            log,
	}
}

func (h *HTTPHandler) RegisterRoutes(rg *gin.RouterGroup) {
	media := rg.Group("/media")
	{
		media.POST("/upload", h.Upload)
		media.GET("/:mediaId", h.GetMetadata)
		media.GET("/:mediaId/download", h.Download)
	}
}

// Upload handles multipart file upload and returns media metadata with presigned URLs.
func (h *HTTPHandler) Upload(c *gin.Context) {
	uploaderID := c.GetHeader("X-User-ID")
	if uploaderID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	fh, err := c.FormFile("file")
	if err != nil {
		response.Error(c, apperr.NewBadRequest("file is required: "+err.Error()))
		return
	}

	result, err := h.mediaSvc.Upload(c.Request.Context(), uploaderID, fh)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.Created(c, result)
}

// GetMetadata returns media metadata with fresh presigned URLs.
func (h *HTTPHandler) GetMetadata(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	mediaID := c.Param("mediaId")
	if mediaID == "" {
		response.Error(c, apperr.NewBadRequest("media_id is required"))
		return
	}

	media, url, thumbURL, err := h.mediaSvc.GetMetadata(c.Request.Context(), mediaID)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, gin.H{
		"media_id":      media.MediaID,
		"file_type":     media.FileType,
		"mime_type":     media.MIMEType,
		"size_bytes":    media.SizeBytes,
		"url":           url,
		"thumbnail_url": thumbURL,
		"width":         media.Width,
		"height":        media.Height,
		"duration_ms":   media.DurationMs,
		"created_at":    media.CreatedAt,
	})
}

// Download streams the file content directly from object storage so that
// clients never need to reach the internal MinIO hostname.
func (h *HTTPHandler) Download(c *gin.Context) {
	mediaID := c.Param("mediaId")
	if mediaID == "" {
		response.Error(c, apperr.NewBadRequest("media_id is required"))
		return
	}

	reader, contentType, size, err := h.mediaSvc.StreamFile(c.Request.Context(), mediaID)
	if err != nil {
		response.Error(c, err)
		return
	}
	defer reader.Close()

	if contentType == "" {
		contentType = "application/octet-stream"
	}

	c.Header("Cache-Control", "public, max-age=86400")
	c.DataFromReader(http.StatusOK, size, contentType, reader, nil)
}
