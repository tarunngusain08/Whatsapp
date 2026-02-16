package handler

import (
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/whatsapp-clone/backend/api-gateway/internal/service"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"
)

func AuthMiddleware(validator service.AuthValidator) gin.HandlerFunc {
	return func(c *gin.Context) {
		auth := c.GetHeader("Authorization")
		if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
			response.Error(c, apperr.NewUnauthorized("missing or invalid Authorization header"))
			c.Abort()
			return
		}
		token := strings.TrimPrefix(auth, "Bearer ")

		userID, phone, err := validator.ValidateToken(c.Request.Context(), token)
		if err != nil {
			response.Error(c, apperr.NewUnauthorized("invalid token"))
			c.Abort()
			return
		}

		c.Set("user_id", userID)
		c.Set("phone", phone)
		c.Next()
	}
}
