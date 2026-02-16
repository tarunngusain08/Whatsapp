package handler

import (
	"context"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/whatsapp-clone/backend/media-service/internal/service"
	mediav1 "github.com/whatsapp-clone/backend/proto/media/v1"
)

type GRPCHandler struct {
	mediav1.UnimplementedMediaServiceServer
	mediaSvc service.MediaService
}

func NewGRPCHandler(mediaSvc service.MediaService) *GRPCHandler {
	return &GRPCHandler{mediaSvc: mediaSvc}
}

func (h *GRPCHandler) GetMediaMetadata(ctx context.Context, req *mediav1.GetMediaMetadataRequest) (*mediav1.GetMediaMetadataResponse, error) {
	media, url, thumbURL, err := h.mediaSvc.GetMetadata(ctx, req.GetMediaId())
	if err != nil {
		return nil, status.Error(codes.NotFound, err.Error())
	}

	return &mediav1.GetMediaMetadataResponse{
		MediaId:      media.MediaID,
		FileType:     media.FileType,
		MimeType:     media.MIMEType,
		SizeBytes:    media.SizeBytes,
		Url:          url,
		ThumbnailUrl: thumbURL,
		Width:        int32(media.Width),
		Height:       int32(media.Height),
		DurationMs:   media.DurationMs,
	}, nil
}
