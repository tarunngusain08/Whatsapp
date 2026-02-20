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
		if uid := c.GetString("user_id"); uid != "" {
			key = "rate:user:" + uid
		}
		allowed, err := limiter.Allow(c.Request.Context(), key, rps, 60)
		if err != nil {
			// Fail-closed: reject requests when rate limiter is unavailable
			response.Error(c, apperr.NewInternal("rate limiter unavailable", err))
			c.Abort()
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
