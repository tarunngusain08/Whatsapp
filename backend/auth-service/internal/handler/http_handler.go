package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	"github.com/whatsapp-clone/backend/auth-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/response"
)

type HTTPHandler struct {
	authSvc service.AuthService
	log     zerolog.Logger
}

func NewHTTPHandler(authSvc service.AuthService, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{authSvc: authSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(rg *gin.RouterGroup) {
	auth := rg.Group("/auth")
	{
		auth.POST("/request-otp", h.RequestOTP)
		auth.POST("/verify-otp", h.VerifyOTP)
		auth.POST("/refresh", h.Refresh)
		auth.POST("/logout", h.Logout)
	}
}

func (h *HTTPHandler) RequestOTP(c *gin.Context) {
	var req model.SendOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	otp, err := h.authSvc.SendOTP(c.Request.Context(), req.Phone)
	if err != nil {
		response.Error(c, err)
		return
	}

	// In production, OTP would be sent via SMS; returning it here for dev/testing
	response.OK(c, gin.H{
		"message": "OTP sent successfully",
		"otp":     otp,
	})
}

func (h *HTTPHandler) VerifyOTP(c *gin.Context) {
	var req model.VerifyOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	tokens, err := h.authSvc.VerifyOTP(c.Request.Context(), req.Phone, req.Code)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, tokens)
}

func (h *HTTPHandler) Refresh(c *gin.Context) {
	var req model.RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	tokens, err := h.authSvc.RefreshTokens(c.Request.Context(), req.RefreshToken)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, tokens)
}

func (h *HTTPHandler) Logout(c *gin.Context) {
	var req model.RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	if err := h.authSvc.Logout(c.Request.Context(), req.RefreshToken); err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, gin.H{"message": "logged out successfully"})
}
