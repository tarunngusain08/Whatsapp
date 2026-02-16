package config

import "time"

type Config struct {
	HTTPPort          string        `env:"MEDIA_HTTP_PORT"          envDefault:":8086"`
	GRPCPort          string        `env:"MEDIA_GRPC_PORT"          envDefault:":9086"`
	MongoURI          string        `env:"MEDIA_MONGO_URI"          envRequired:"true"`
	MongoDB           string        `env:"MEDIA_MONGO_DB"           envDefault:"whatsapp"`
	MinIOEndpoint     string        `env:"MEDIA_MINIO_ENDPOINT"     envDefault:"minio:9000"`
	MinIOAccessKey    string        `env:"MEDIA_MINIO_ACCESS_KEY"   envRequired:"true"`
	MinIOSecretKey    string        `env:"MEDIA_MINIO_SECRET_KEY"   envRequired:"true"`
	MinIOBucket       string        `env:"MEDIA_MINIO_BUCKET"       envDefault:"whatsapp-media"`
	MinIOUseSSL       bool          `env:"MEDIA_MINIO_USE_SSL"      envDefault:"false"`
	PresignedURLTTL   time.Duration `env:"MEDIA_PRESIGNED_TTL"      envDefault:"1h"`
	FFmpegPath        string        `env:"MEDIA_FFMPEG_PATH"        envDefault:"/usr/bin/ffmpeg"`
	MaxImageSize      int64         `env:"MEDIA_MAX_IMAGE_SIZE"     envDefault:"16777216"`
	MaxVideoSize      int64         `env:"MEDIA_MAX_VIDEO_SIZE"     envDefault:"67108864"`
	MaxAudioSize      int64         `env:"MEDIA_MAX_AUDIO_SIZE"     envDefault:"16777216"`
	MaxDocSize        int64         `env:"MEDIA_MAX_DOC_SIZE"       envDefault:"104857600"`
	ThumbnailMaxWidth int           `env:"MEDIA_THUMB_MAX_WIDTH"    envDefault:"200"`
	CleanupInterval   time.Duration `env:"MEDIA_CLEANUP_INTERVAL"   envDefault:"6h"`
	LogLevel          string        `env:"MEDIA_LOG_LEVEL"          envDefault:"info"`
	OTLPEndpoint      string        `env:"OTLP_ENDPOINT"            envDefault:""`
}
