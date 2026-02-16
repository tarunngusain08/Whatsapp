package config

import "time"

type Config struct {
	Port            string        `env:"GATEWAY_PORT"              envDefault:":8080"`
	AuthGRPCAddr    string        `env:"AUTH_GRPC_ADDR"            envDefault:"auth-service:9081"`
	AuthHTTPAddr    string        `env:"AUTH_HTTP_ADDR"            envDefault:"http://auth-service:8081"`
	UserHTTPAddr    string        `env:"USER_HTTP_ADDR"            envDefault:"http://user-service:8082"`
	ChatHTTPAddr    string        `env:"CHAT_HTTP_ADDR"            envDefault:"http://chat-service:8083"`
	MessageHTTPAddr string        `env:"MESSAGE_HTTP_ADDR"         envDefault:"http://message-service:8084"`
	MediaHTTPAddr   string        `env:"MEDIA_HTTP_ADDR"           envDefault:"http://media-service:8086"`
	WSAddr          string        `env:"WS_ADDR"                   envDefault:"ws://ws-service:8087/ws"`
	RedisAddr       string        `env:"GATEWAY_REDIS_ADDR"        envDefault:"redis:6379"`
	RedisPassword   string        `env:"GATEWAY_REDIS_PASSWORD"    envDefault:""`
	RateLimitRPS    int           `env:"GATEWAY_RATE_LIMIT_RPS"    envDefault:"60"`
	RateLimitBurst  int           `env:"GATEWAY_RATE_LIMIT_BURST"  envDefault:"10"`
	CORSOrigins     string        `env:"GATEWAY_CORS_ORIGINS"      envDefault:"*"`
	LogLevel        string        `env:"GATEWAY_LOG_LEVEL"         envDefault:"info"`
	GRPCTimeout     time.Duration `env:"GATEWAY_GRPC_TIMEOUT"      envDefault:"5s"`
	MaxBodySize     int64         `env:"GATEWAY_MAX_BODY_SIZE"     envDefault:"104857600"`
	OTLPEndpoint    string        `env:"OTLP_ENDPOINT"             envDefault:""`
}
