package config

type Config struct {
	HTTPPort    string `env:"CHAT_HTTP_PORT"         envDefault:":8083"`
	GRPCPort    string `env:"CHAT_GRPC_PORT"         envDefault:":9083"`
	PostgresDSN string `env:"CHAT_POSTGRES_DSN"      envRequired:"true"`
	NATSUrl     string `env:"CHAT_NATS_URL"          envDefault:"nats://nats:4222"`
	MessageGRPC string `env:"CHAT_MESSAGE_GRPC_ADDR" envDefault:"message-service:9084"`
	LogLevel    string `env:"CHAT_LOG_LEVEL"         envDefault:"info"`
	OTLPEndpoint string `env:"OTLP_ENDPOINT"          envDefault:""`
}
