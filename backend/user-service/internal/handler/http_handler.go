package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"
	"github.com/whatsapp-clone/backend/user-service/internal/model"
	"github.com/whatsapp-clone/backend/user-service/internal/service"
)

type HTTPHandler struct {
	userSvc service.UserService
	log     zerolog.Logger
}

func NewHTTPHandler(userSvc service.UserService, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{userSvc: userSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(r *gin.RouterGroup) {
	r.GET("/me", h.GetMyProfile)
	r.PUT("/me", h.UpdateMyProfile)
	r.PATCH("/me", h.UpdateMyProfile) // Client-compatible alias
	r.PUT("/me/avatar", h.UploadAvatar)
	r.GET("/:id", h.GetUserProfile)
	r.POST("/contacts/sync", h.ContactSync)
	r.GET("/contacts", h.GetContacts)
	r.POST("/contacts/:id/block", h.BlockUser)
	r.DELETE("/contacts/:id/block", h.UnblockUser)
	r.GET("/privacy", h.GetPrivacySettings)
	r.PUT("/privacy", h.UpdatePrivacySettings)
	r.POST("/devices", h.RegisterDevice)
	r.DELETE("/devices/:token", h.RemoveDevice)
	r.GET("/:id/presence", h.GetPresence)

	statuses := r.Group("/statuses")
	statuses.POST("", h.CreateStatus)
	statuses.GET("", h.GetContactStatuses)
	statuses.GET("/me", h.GetMyStatuses)
	statuses.DELETE("/:id", h.DeleteStatus)
	statuses.POST("/:id/view", h.ViewStatus)
}

// extractUserID reads the user ID set by the api-gateway via X-User-ID header.
func extractUserID(c *gin.Context) string {
	if uid := c.GetString("user_id"); uid != "" {
		return uid
	}
	return c.GetHeader("X-User-ID")
}

func (h *HTTPHandler) GetMyProfile(c *gin.Context) {
	userID := extractUserID(c)
	user, err := h.userSvc.GetProfile(c.Request.Context(), userID, userID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, user)
}

func (h *HTTPHandler) UpdateMyProfile(c *gin.Context) {
	userID := extractUserID(c)
	var req model.UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	user, err := h.userSvc.UpdateProfile(c.Request.Context(), userID, &req)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, user)
}

func (h *HTTPHandler) UploadAvatar(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	file, header, err := c.Request.FormFile("avatar")
	if err != nil {
		response.Error(c, apperr.NewBadRequest("avatar file is required"))
		return
	}
	defer file.Close()

	// Validate file size (max 5MB for avatars)
	if header.Size > 5*1024*1024 {
		response.Error(c, apperr.NewBadRequest("avatar file too large, max 5MB"))
		return
	}

	// Validate content type
	contentType := header.Header.Get("Content-Type")
	if contentType != "image/jpeg" && contentType != "image/png" && contentType != "image/webp" {
		response.Error(c, apperr.NewBadRequest("invalid avatar format, must be JPEG, PNG, or WebP"))
		return
	}

	avatarURL, err := h.userSvc.UploadAvatar(c.Request.Context(), userID, file, header.Size, contentType)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, gin.H{"avatar_url": avatarURL})
}

func (h *HTTPHandler) GetUserProfile(c *gin.Context) {
	callerID := extractUserID(c)
	targetID := c.Param("id")
	user, err := h.userSvc.GetProfile(c.Request.Context(), callerID, targetID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, user)
}

func (h *HTTPHandler) ContactSync(c *gin.Context) {
	userID := extractUserID(c)
	var req model.ContactSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	// Coalesce: client sends "phone_numbers", backend expects "phones"
	phones := req.Phones
	if len(phones) == 0 {
		phones = req.PhoneNumbers
	}
	if len(phones) == 0 {
		response.Error(c, apperr.NewBadRequest("phones or phone_numbers is required"))
		return
	}

	results, err := h.userSvc.ContactSync(c.Request.Context(), userID, phones)
	if err != nil {
		response.Error(c, err)
		return
	}

	// Wrap in registered_users for client compatibility
	response.OK(c, gin.H{
		"registered_users": results,
	})
}

func (h *HTTPHandler) GetContacts(c *gin.Context) {
	userID := extractUserID(c)
	contacts, err := h.userSvc.GetContacts(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, contacts)
}

func (h *HTTPHandler) BlockUser(c *gin.Context) {
	userID := extractUserID(c)
	targetID := c.Param("id")
	if err := h.userSvc.BlockUser(c.Request.Context(), userID, targetID); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

func (h *HTTPHandler) UnblockUser(c *gin.Context) {
	userID := extractUserID(c)
	targetID := c.Param("id")
	if err := h.userSvc.UnblockUser(c.Request.Context(), userID, targetID); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

func (h *HTTPHandler) GetPrivacySettings(c *gin.Context) {
	userID := extractUserID(c)
	settings, err := h.userSvc.GetPrivacySettings(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, settings)
}

func (h *HTTPHandler) UpdatePrivacySettings(c *gin.Context) {
	userID := extractUserID(c)
	var settings model.PrivacySettings
	if err := c.ShouldBindJSON(&settings); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	settings.UserID = userID
	if err := h.userSvc.UpdatePrivacySettings(c.Request.Context(), &settings); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

func (h *HTTPHandler) RegisterDevice(c *gin.Context) {
	userID := extractUserID(c)
	var dt model.DeviceToken
	if err := c.ShouldBindJSON(&dt); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	dt.UserID = userID
	if err := h.userSvc.RegisterDeviceToken(c.Request.Context(), &dt); err != nil {
		response.Error(c, err)
		return
	}
	response.Created(c, dt)
}

func (h *HTTPHandler) RemoveDevice(c *gin.Context) {
	userID := extractUserID(c)
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing user identity"))
		return
	}
	token := c.Param("token")
	if err := h.userSvc.RemoveDeviceTokenForUser(c.Request.Context(), userID, token); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

func (h *HTTPHandler) GetPresence(c *gin.Context) {
	targetID := c.Param("id")
	online, lastSeen, err := h.userSvc.CheckPresence(c.Request.Context(), targetID)
	if err != nil {
		response.Error(c, err)
		return
	}
	resp := gin.H{"online": online}
	if lastSeen != nil {
		resp["last_seen"] = lastSeen
	}
	response.OK(c, resp)
}

func (h *HTTPHandler) CreateStatus(c *gin.Context) {
	userID := extractUserID(c)
	var req model.CreateStatusRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	status, err := h.userSvc.CreateStatus(c.Request.Context(), userID, &req)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.Created(c, status)
}

func (h *HTTPHandler) GetContactStatuses(c *gin.Context) {
	userID := extractUserID(c)
	statuses, err := h.userSvc.GetContactStatuses(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, statuses)
}

func (h *HTTPHandler) GetMyStatuses(c *gin.Context) {
	userID := extractUserID(c)
	statuses, err := h.userSvc.GetMyStatuses(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}
	response.OK(c, statuses)
}

func (h *HTTPHandler) DeleteStatus(c *gin.Context) {
	userID := extractUserID(c)
	statusID := c.Param("id")
	if err := h.userSvc.DeleteStatus(c.Request.Context(), statusID, userID); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

func (h *HTTPHandler) ViewStatus(c *gin.Context) {
	viewerID := extractUserID(c)
	statusID := c.Param("id")
	if err := h.userSvc.ViewStatus(c.Request.Context(), statusID, viewerID); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}
