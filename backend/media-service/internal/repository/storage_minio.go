package repository

import (
	"context"
	"io"
	"net/url"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/rs/zerolog"
)

type storageMinIORepo struct {
	client *minio.Client
	bucket string
	log    zerolog.Logger
}

func NewStorageMinIORepository(client *minio.Client, bucket string, log zerolog.Logger) StorageRepository {
	return &storageMinIORepo{
		client: client,
		bucket: bucket,
		log:    log,
	}
}

func (r *storageMinIORepo) Upload(ctx context.Context, key string, reader io.Reader, size int64, contentType string) error {
	_, err := r.client.PutObject(ctx, r.bucket, key, reader, size, minio.PutObjectOptions{
		ContentType: contentType,
	})
	if err != nil {
		r.log.Error().Err(err).Str("key", key).Msg("failed to upload object to MinIO")
		return err
	}
	return nil
}

func (r *storageMinIORepo) Delete(ctx context.Context, key string) error {
	err := r.client.RemoveObject(ctx, r.bucket, key, minio.RemoveObjectOptions{})
	if err != nil {
		r.log.Error().Err(err).Str("key", key).Msg("failed to delete object from MinIO")
		return err
	}
	return nil
}

func (r *storageMinIORepo) PresignedURL(ctx context.Context, key string, expiry time.Duration) (string, error) {
	reqParams := make(url.Values)
	presignedURL, err := r.client.PresignedGetObject(ctx, r.bucket, key, expiry, reqParams)
	if err != nil {
		r.log.Error().Err(err).Str("key", key).Msg("failed to generate presigned URL")
		return "", err
	}
	return presignedURL.String(), nil
}

func (r *storageMinIORepo) GetObject(ctx context.Context, key string) (io.ReadCloser, string, int64, error) {
	obj, err := r.client.GetObject(ctx, r.bucket, key, minio.GetObjectOptions{})
	if err != nil {
		r.log.Error().Err(err).Str("key", key).Msg("failed to get object from MinIO")
		return nil, "", 0, err
	}
	info, err := obj.Stat()
	if err != nil {
		obj.Close()
		r.log.Error().Err(err).Str("key", key).Msg("failed to stat object from MinIO")
		return nil, "", 0, err
	}
	return obj, info.ContentType, info.Size, nil
}
