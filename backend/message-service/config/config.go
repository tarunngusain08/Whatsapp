package config

type Config struct {
	HTTPPort string `env:"MESSAGE_HTTP_PORT" envDefault:":8084"`
	GRPCPort string `env:"MESSAGE_GRPC_PORT" envDefault:":9084"`
	MongoURI string `env:"MESSAGE_MONGO_URI" envRequired:"true"`
	MongoDB  string `env:"MESSAGE_MONGO_DB"  envDefault:"whatsapp"`
	NATSUrl  string `env:"MESSAGE_NATS_URL"  envDefault:"nats://nats:4222"`
	UserServiceGRPC string `env:"MESSAGE_USER_GRPC_ADDR" envDefault:"user-service:9082"`
	ChatServiceGRPC string `env:"MESSAGE_CHAT_GRPC_ADDR" envDefault:"chat-service:9083"`
	LogLevel        string `env:"MESSAGE_LOG_LEVEL"      envDefault:"info"`
	OTLPEndpoint    string `env:"OTLP_ENDPOINT"          envDefault:""`
}
