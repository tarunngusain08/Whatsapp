package middleware

import (
	"net/http"
	"runtime/debug"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"
)

func Recovery(log zerolog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				rid, _ := c.Get("request_id")
				ridStr := ""
				if rid != nil {
					ridStr = rid.(string)
				}
				log.Error().
					Str("request_id", ridStr).
					Interface("error", err).
					Str("stack", string(debug.Stack())).
					Msg("panic recovered")
				c.AbortWithStatusJSON(http.StatusInternalServerError, gin.H{
					"success": false,
					"error":   gin.H{"code": "INTERNAL_ERROR", "message": "internal server error"},
				})
			}
		}()
		c.Next()
	}
}
