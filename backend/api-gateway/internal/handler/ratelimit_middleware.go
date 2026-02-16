package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/whatsapp-clone/backend/api-gateway/internal/service"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"
)

func RateLimitMiddleware(limiter service.RateLimiter, rps int) gin.HandlerFunc {
	return func(c *gin.Context) {
		key := "rate:" + c.ClientIP()
		if uid, exists := c.Get("user_id"); exists {
			key = "rate:user:" + uid.(string)
		}
		allowed, err := limiter.Allow(c.Request.Context(), key, rps, 60)
		if err != nil {
			c.Next()
			return
		}
		if !allowed {
			response.Error(c, apperr.NewTooManyRequests("rate limit exceeded"))
			c.Abort()
			return
		}
		c.Next()
	}
}
