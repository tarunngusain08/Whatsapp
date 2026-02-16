package config

import "time"

type Config struct {
	HTTPPort       string        `env:"WS_PORT"              envDefault:":8087"`
	RedisAddr      string        `env:"WS_REDIS_ADDR"        envDefault:"redis:6379"`
	RedisPassword  string        `env:"WS_REDIS_PASSWORD"    envDefault:""`
	NATSUrl        string        `env:"WS_NATS_URL"          envDefault:"nats://nats:4222"`
	AuthGRPCAddr   string        `env:"WS_AUTH_GRPC_ADDR"    envDefault:"auth-service:9081"`
	MessageGRPCAddr string       `env:"WS_MSG_GRPC_ADDR"     envDefault:"message-service:9084"`
	ChatGRPCAddr   string        `env:"WS_CHAT_GRPC_ADDR"    envDefault:"chat-service:9083"`
	PingInterval   time.Duration `env:"WS_PING_INTERVAL"     envDefault:"25s"`
	PongTimeout    time.Duration `env:"WS_PONG_TIMEOUT"      envDefault:"35s"`
	WriteTimeout   time.Duration `env:"WS_WRITE_TIMEOUT"     envDefault:"10s"`
	MaxMessageSize int64         `env:"WS_MAX_MSG_SIZE"      envDefault:"65536"`
	PresenceTTL    time.Duration `env:"WS_PRESENCE_TTL"      envDefault:"60s"`
	TypingTTL      time.Duration `env:"WS_TYPING_TTL"        envDefault:"5s"`
	LogLevel       string        `env:"WS_LOG_LEVEL"         envDefault:"info"`
	OTLPEndpoint   string        `env:"OTLP_ENDPOINT"        envDefault:""`
}
