package config

import "time"

type Config struct {
	HTTPPort        string        `env:"AUTH_HTTP_PORT"          envDefault:":8081"`
	GRPCPort        string        `env:"AUTH_GRPC_PORT"          envDefault:":9081"`
	PostgresDSN     string        `env:"AUTH_POSTGRES_DSN"       envRequired:"true"`
	RedisAddr       string        `env:"AUTH_REDIS_ADDR"         envDefault:"redis:6379"`
	RedisPassword   string        `env:"AUTH_REDIS_PASSWORD"     envDefault:""`
	JWTSecret       string        `env:"AUTH_JWT_SECRET"         envRequired:"true"`
	AccessTokenTTL  time.Duration `env:"AUTH_ACCESS_TOKEN_TTL"   envDefault:"15m"`
	RefreshTokenTTL time.Duration `env:"AUTH_REFRESH_TOKEN_TTL"  envDefault:"720h"`
	OTPLength       int           `env:"AUTH_OTP_LENGTH"         envDefault:"6"`
	OTPTTL          time.Duration `env:"AUTH_OTP_TTL"            envDefault:"5m"`
	OTPMaxAttempts  int           `env:"AUTH_OTP_MAX_ATTEMPTS"   envDefault:"5"`
	DevMode         bool          `env:"AUTH_DEV_MODE"           envDefault:"false"`
	LogLevel        string        `env:"AUTH_LOG_LEVEL"          envDefault:"info"`
	OTLPEndpoint    string        `env:"OTLP_ENDPOINT"           envDefault:""`
}
