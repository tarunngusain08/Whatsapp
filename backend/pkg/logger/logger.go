package logger

import (
	"io"
	"os"
	"time"

	"github.com/rs/zerolog"
)

func New(serviceName, level string) zerolog.Logger {
	var lvl zerolog.Level
	switch level {
	case "debug":
		lvl = zerolog.DebugLevel
	case "warn":
		lvl = zerolog.WarnLevel
	case "error":
		lvl = zerolog.ErrorLevel
	default:
		lvl = zerolog.InfoLevel
	}

	var w io.Writer = os.Stdout
	if os.Getenv("LOG_FORMAT") == "pretty" {
		w = zerolog.ConsoleWriter{Out: os.Stdout, TimeFormat: time.RFC3339}
	}

	return zerolog.New(w).
		Level(lvl).
		With().
		Timestamp().
		Str("service", serviceName).
		Logger()
}

func WithRequestID(log zerolog.Logger, requestID string) zerolog.Logger {
	return log.With().Str("request_id", requestID).Logger()
}

func WithUserID(log zerolog.Logger, userID string) zerolog.Logger {
	return log.With().Str("user_id", userID).Logger()
}
