package middleware

import (
	"net/url"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"
)

var sensitivePathPrefixes = []string{"/auth", "/otp", "/token"}
var sensitiveQueryKeys = map[string]bool{
	"token": true, "otp": true, "code": true, "secret": true,
}

func Logger(log zerolog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		query := redactQuery(path, c.Request.URL.RawQuery)

		c.Next()

		latency := time.Since(start)
		status := c.Writer.Status()
		rid, _ := c.Get("request_id")

		evt := log.Info()
		if status >= 500 {
			evt = log.Error()
		} else if status >= 400 {
			evt = log.Warn()
		}

		ridStr := ""
		if rid != nil {
			ridStr = rid.(string)
		}

		evt.
			Str("request_id", ridStr).
			Str("method", c.Request.Method).
			Str("path", path).
			Str("query", query).
			Int("status", status).
			Dur("latency", latency).
			Str("client_ip", c.ClientIP()).
			Str("user_agent", c.Request.UserAgent()).
			Msg("request completed")
	}
}

func redactQuery(path, rawQuery string) string {
	if rawQuery == "" {
		return ""
	}
	for _, prefix := range sensitivePathPrefixes {
		if strings.Contains(path, prefix) {
			return "[REDACTED]"
		}
	}
	parsed, err := url.ParseQuery(rawQuery)
	if err != nil {
		return "[REDACTED]"
	}
	for key := range parsed {
		if sensitiveQueryKeys[strings.ToLower(key)] {
			parsed.Set(key, "[REDACTED]")
		}
	}
	return parsed.Encode()
}
