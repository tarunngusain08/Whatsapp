package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/chat-service/internal/model"
	"github.com/whatsapp-clone/backend/chat-service/internal/service"
	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"
)

type HTTPHandler struct {
	chatSvc service.ChatService
	log     zerolog.Logger
}

func NewHTTPHandler(chatSvc service.ChatService, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{chatSvc: chatSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(rg *gin.RouterGroup) {
	chats := rg.Group("/chats")
	{
		chats.POST("", h.CreateChat)
		chats.GET("", h.ListChats)
		chats.GET("/:id", h.GetChat)
		chats.PATCH("/:id", h.UpdateGroup)
		chats.POST("/:id/participants", h.AddMember)
		chats.DELETE("/:id/participants/:userId", h.RemoveMember)
		chats.PATCH("/:id/participants/:userId/role", h.ChangeRole)
		chats.PUT("/:id/mute", h.MuteChat)
		chats.PUT("/:id/pin", h.PinChat)
		chats.PUT("/:id/avatar", h.UploadGroupAvatar)
		chats.PUT("/:id/disappearing", h.SetDisappearingMessages)
	}
}

func getUserID(c *gin.Context) string {
	return c.GetHeader("X-User-ID")
}

func (h *HTTPHandler) CreateChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	// Peek at the body to determine if this is a direct chat or group creation.
	// If "name" is present, it's a group; otherwise it's a direct chat.
	var raw map[string]interface{}
	if err := c.ShouldBindJSON(&raw); err != nil {
		response.Error(c, err)
		return
	}

	if _, hasName := raw["name"]; hasName {
		var req model.CreateGroupRequest
		req.Name, _ = raw["name"].(string)
		req.Description, _ = raw["description"].(string)
		if memberIDsRaw, ok := raw["member_ids"].([]interface{}); ok {
			for _, v := range memberIDsRaw {
				if s, ok := v.(string); ok {
					req.MemberIDs = append(req.MemberIDs, s)
				}
			}
		}
		if req.Name == "" {
			response.Error(c, &validationErr{msg: "name is required"})
			return
		}
		if len(req.MemberIDs) == 0 {
			response.Error(c, &validationErr{msg: "member_ids is required"})
			return
		}

		chat, group, err := h.chatSvc.CreateGroup(c.Request.Context(), userID, &req)
		if err != nil {
			response.Error(c, err)
			return
		}
		response.Created(c, gin.H{"chat": chat, "group": group})
	} else {
		var req model.CreateDirectChatRequest
		if otherID, ok := raw["other_user_id"].(string); ok {
			req.OtherUserID = otherID
		}
		if req.OtherUserID == "" {
			response.Error(c, &validationErr{msg: "other_user_id is required"})
			return
		}

		chat, err := h.chatSvc.CreateDirectChat(c.Request.Context(), userID, &req)
		if err != nil {
			response.Error(c, err)
			return
		}
		response.Created(c, gin.H{"chat": chat})
	}
}

func (h *HTTPHandler) ListChats(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	items, err := h.chatSvc.ListChats(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, items)
}

func (h *HTTPHandler) GetChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")
	item, err := h.chatSvc.GetChat(c.Request.Context(), userID, chatID)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, item)
}

func (h *HTTPHandler) UpdateGroup(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")
	var req model.UpdateGroupRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	if err := h.chatSvc.UpdateGroup(c.Request.Context(), userID, chatID, &req); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) AddMember(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")
	var req model.AddMemberRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, err)
		return
	}

	if err := h.chatSvc.AddMember(c.Request.Context(), userID, chatID, req.UserID); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) RemoveMember(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")
	targetUserID := c.Param("userId")

	if err := h.chatSvc.RemoveMember(c.Request.Context(), userID, chatID, targetUserID); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) ChangeRole(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")
	targetUserID := c.Param("userId")

	var body struct {
		Role string `json:"role" binding:"required"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, err)
		return
	}

	var err error
	switch body.Role {
	case "admin":
		err = h.chatSvc.PromoteMember(c.Request.Context(), userID, chatID, targetUserID)
	case "member":
		err = h.chatSvc.DemoteMember(c.Request.Context(), userID, chatID, targetUserID)
	default:
		response.Error(c, &validationErr{msg: "role must be 'admin' or 'member'"})
		return
	}

	if err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) MuteChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")

	var body struct {
		Muted     bool    `json:"muted"`
		MuteUntil *string `json:"mute_until"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, err)
		return
	}

	var muteUntil *time.Time
	if body.MuteUntil != nil {
		t, err := time.Parse(time.RFC3339, *body.MuteUntil)
		if err != nil {
			response.Error(c, &validationErr{msg: "mute_until must be RFC3339 format"})
			return
		}
		muteUntil = &t
	}

	if err := h.chatSvc.MuteChat(c.Request.Context(), userID, chatID, body.Muted, muteUntil); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) PinChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "missing X-User-ID header"})
		return
	}

	chatID := c.Param("id")

	var body struct {
		Pinned bool `json:"pinned"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, err)
		return
	}

	if err := h.chatSvc.PinChat(c.Request.Context(), userID, chatID, body.Pinned); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) UploadGroupAvatar(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}
	chatID := c.Param("id")

	file, header, err := c.Request.FormFile("avatar")
	if err != nil {
		response.Error(c, apperr.NewBadRequest("avatar file is required"))
		return
	}
	defer file.Close()

	if header.Size > 5*1024*1024 {
		response.Error(c, apperr.NewBadRequest("avatar file too large, max 5MB"))
		return
	}

	contentType := header.Header.Get("Content-Type")
	if contentType != "image/jpeg" && contentType != "image/png" && contentType != "image/webp" {
		response.Error(c, apperr.NewBadRequest("invalid format, must be JPEG, PNG, or WebP"))
		return
	}

	avatarURL, err := h.chatSvc.UploadGroupAvatar(c.Request.Context(), chatID, userID)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, gin.H{"avatar_url": avatarURL})
}

func (h *HTTPHandler) SetDisappearingMessages(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}
	chatID := c.Param("id")

	var body struct {
		Timer string `json:"timer"` // "off", "24h", "7d", "90d"
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}

	var duration *time.Duration
	switch body.Timer {
	case "off", "":
		duration = nil
	case "24h":
		d := 24 * time.Hour
		duration = &d
	case "7d":
		d := 7 * 24 * time.Hour
		duration = &d
	case "90d":
		d := 90 * 24 * time.Hour
		duration = &d
	default:
		response.Error(c, apperr.NewBadRequest("invalid timer value, must be 'off', '24h', '7d', or '90d'"))
		return
	}

	if err := h.chatSvc.SetDisappearingMessages(c.Request.Context(), chatID, userID, duration); err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, gin.H{"timer": body.Timer})
}

type validationErr struct {
	msg string
}

func (e *validationErr) Error() string {
	return e.msg
}
