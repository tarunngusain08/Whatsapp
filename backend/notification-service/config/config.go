package config

import "time"

type Config struct {
	HTTPPort           string        `env:"NOTIF_HTTP_PORT"            envDefault:":8085"`
	PostgresDSN        string        `env:"NOTIF_POSTGRES_DSN"         envRequired:"true"`
	RedisAddr          string        `env:"NOTIF_REDIS_ADDR"           envDefault:"redis:6379"`
	RedisPassword      string        `env:"NOTIF_REDIS_PASSWORD"       envDefault:""`
	NATSUrl            string        `env:"NOTIF_NATS_URL"             envDefault:"nats://nats:4222"`
	FCMCredentialsPath string        `env:"NOTIF_FCM_CREDENTIALS_PATH" envDefault:""`
	GroupBatchWindow   time.Duration `env:"NOTIF_GROUP_BATCH_WINDOW"   envDefault:"3s"`
	MaxRetries         int           `env:"NOTIF_MAX_RETRIES"          envDefault:"3"`
	LogLevel           string        `env:"NOTIF_LOG_LEVEL"            envDefault:"info"`
	OTLPEndpoint       string        `env:"OTLP_ENDPOINT"              envDefault:""`
}
