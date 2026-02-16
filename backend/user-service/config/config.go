package config

import "time"

type Config struct {
	HTTPPort    string        `env:"USER_HTTP_PORT"      envDefault:":8082"`
	GRPCPort    string        `env:"USER_GRPC_PORT"      envDefault:":9082"`
	PostgresDSN string        `env:"USER_POSTGRES_DSN"   envRequired:"true"`
	RedisAddr   string        `env:"USER_REDIS_ADDR"     envDefault:"redis:6379"`
	RedisPassword string      `env:"USER_REDIS_PASSWORD" envDefault:""`
	PresenceTTL time.Duration `env:"USER_PRESENCE_TTL"   envDefault:"60s"`
	LogLevel     string        `env:"USER_LOG_LEVEL"      envDefault:"info"`
	OTLPEndpoint string        `env:"OTLP_ENDPOINT"       envDefault:""`
}
