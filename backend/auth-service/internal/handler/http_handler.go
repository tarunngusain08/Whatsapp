package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/auth-service/internal/model"
	"github.com/whatsapp-clone/backend/auth-service/internal/service"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"
)

type HTTPHandler struct {
	authSvc service.AuthService
	devMode bool
	log     zerolog.Logger
}

func NewHTTPHandler(authSvc service.AuthService, devMode bool, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{authSvc: authSvc, devMode: devMode, log: log}
}

func (h *HTTPHandler) RegisterRoutes(rg *gin.RouterGroup) {
	auth := rg.Group("/auth")
	{
		// Original backend routes
		auth.POST("/request-otp", h.RequestOTP)
		auth.POST("/verify-otp", h.VerifyOTP)
		auth.POST("/refresh", h.Refresh)
		auth.POST("/logout", h.Logout)

		// Client-compatible aliases
		auth.POST("/otp/send", h.RequestOTP)
		auth.POST("/otp/verify", h.VerifyOTP)
		auth.POST("/token/refresh", h.Refresh)
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

	resp := gin.H{
		"message":            "OTP sent successfully",
		"expires_in_seconds": 300,
	}
	if h.devMode {
		resp["otp"] = otp
	}

	response.OK(c, resp)
}

func (h *HTTPHandler) VerifyOTP(c *gin.Context) {
	var req model.VerifyOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	// Coalesce: client sends "otp", backend expects "code"
	if req.Code == "" {
		req.Code = req.OTP
	}

	if req.Code == "" {
		response.Error(c, apperr.NewBadRequest("code or otp is required"))
		return
	}

	result, err := h.authSvc.VerifyOTP(c.Request.Context(), req.Phone, req.Code)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, result)
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

	// Return both expires_in and expires_in_seconds for client compatibility
	response.OK(c, gin.H{
		"access_token":       tokens.AccessToken,
		"refresh_token":      tokens.RefreshToken,
		"expires_in":         tokens.ExpiresIn,
		"expires_in_seconds": tokens.ExpiresIn,
	})
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
