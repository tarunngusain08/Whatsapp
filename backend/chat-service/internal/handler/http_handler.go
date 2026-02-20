package handler

import (
	"strings"
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

func requireChatID(c *gin.Context) (string, bool) {
	chatID := strings.TrimSpace(c.Param("id"))
	if chatID == "" {
		response.Error(c, apperr.NewBadRequest("chat ID is required"))
		return "", false
	}
	return chatID, true
}

func (h *HTTPHandler) CreateChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	// Peek at the body to determine if this is a direct chat or group creation.
	// Supports both backend format (name + member_ids / other_user_id) and
	// client format (type + participant_ids + optional name).
	var raw map[string]interface{}
	if err := c.ShouldBindJSON(&raw); err != nil {
		response.Error(c, err)
		return
	}

	// Extract participant_ids (client format) and member_ids (backend format)
	participantIDs := extractStringSlice(raw, "participant_ids")
	memberIDs := extractStringSlice(raw, "member_ids")

	chatType, _ := raw["type"].(string)
	name, _ := raw["name"].(string)
	description, _ := raw["description"].(string)
	otherUserID, _ := raw["other_user_id"].(string)

	// Determine whether this is a direct or group chat:
	// - Explicit "type": "group" or "type": "direct" from client
	// - Presence of "name" (without other_user_id) implies group
	// - Presence of "other_user_id" implies direct
	isGroup := chatType == "group" || (name != "" && chatType != "direct" && otherUserID == "")
	isDirect := chatType == "direct" || otherUserID != "" || (!isGroup && len(participantIDs) == 1)

	if isDirect {
		if otherUserID == "" && len(participantIDs) > 0 {
			otherUserID = participantIDs[0]
		}
		if otherUserID == "" {
			response.Error(c, &validationErr{msg: "other_user_id or participant_ids is required"})
			return
		}

		req := model.CreateDirectChatRequest{OtherUserID: otherUserID}
		chat, err := h.chatSvc.CreateDirectChat(c.Request.Context(), userID, &req)
		if err != nil {
			response.Error(c, err)
			return
		}

		// Include both participants so the client can persist them locally
		participants := []model.ChatParticipant{
			{ChatID: chat.ID, UserID: userID, Role: "member", JoinedAt: chat.CreatedAt},
			{ChatID: chat.ID, UserID: otherUserID, Role: "member", JoinedAt: chat.CreatedAt},
		}

		// Return flattened ChatDto for client compatibility
		response.Created(c, flattenChat(&model.ChatListItem{
			Chat:         *chat,
			Participants: participants,
		}))
	} else {
		// Group chat
		if len(memberIDs) == 0 {
			memberIDs = participantIDs
		}
		if name == "" {
			response.Error(c, &validationErr{msg: "name is required for group chats"})
			return
		}
		if len(memberIDs) == 0 {
			response.Error(c, &validationErr{msg: "member_ids or participant_ids is required"})
			return
		}

		req := model.CreateGroupRequest{
			Name:        name,
			Description: description,
			MemberIDs:   memberIDs,
		}
		chat, group, err := h.chatSvc.CreateGroup(c.Request.Context(), userID, &req)
		if err != nil {
			response.Error(c, err)
			return
		}

		response.Created(c, flattenChat(&model.ChatListItem{
			Chat:  *chat,
			Group: group,
		}))
	}
}

// extractStringSlice extracts a []string from a map value that may be []interface{}.
func extractStringSlice(raw map[string]interface{}, key string) []string {
	var result []string
	if arr, ok := raw[key].([]interface{}); ok {
		for _, v := range arr {
			if s, ok := v.(string); ok {
				result = append(result, s)
			}
		}
	}
	return result
}

func (h *HTTPHandler) ListChats(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	items, err := h.chatSvc.ListChats(c.Request.Context(), userID)
	if err != nil {
		response.Error(c, err)
		return
	}

	// Return in PaginatedData format the client expects:
	// { success: true, data: { items: [...], hasMore: false } }
	flat := make([]gin.H, 0, len(items))
	for _, item := range items {
		flat = append(flat, flattenChat(item))
	}

	response.OK(c, gin.H{
		"items":      flat,
		"nextCursor": nil,
		"hasMore":    false,
	})
}

func (h *HTTPHandler) GetChat(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}
	item, err := h.chatSvc.GetChat(c.Request.Context(), userID, chatID)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, flattenChat(item))
}

func (h *HTTPHandler) UpdateGroup(c *gin.Context) {
	userID := getUserID(c)
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}
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
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}
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
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}
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
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}
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
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}

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
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	chatID, ok := requireChatID(c)
	if !ok {
		return
	}

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
	chatID, ok := requireChatID(c)
	if !ok {
		return
	}

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
	chatID, ok := requireChatID(c)
	if !ok {
		return
	}

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

// flattenChat transforms a ChatListItem into a flat ChatDto-like map
// matching the client's expected shape.
func flattenChat(item *model.ChatListItem) gin.H {
	name := ""
	description := ""
	avatarURL := ""

	if item.Group != nil {
		name = item.Group.Name
		description = item.Group.Description
		avatarURL = item.Group.AvatarURL
	}

	// Build flat participant list -- only include fields the chat-service
	// actually knows.  display_name / avatar_url are NOT sent so the client
	// won't overwrite existing local user records with empty values.
	participants := make([]gin.H, 0, len(item.Participants))
	for _, p := range item.Participants {
		participants = append(participants, gin.H{
			"user_id": p.UserID,
			"role":    p.Role,
		})
	}

	// Build flat last_message
	var lastMessage interface{}
	if item.LastMessage != nil {
		lastMessage = gin.H{
			"message_id": item.LastMessage.MessageID,
			"preview":    item.LastMessage.Body,
			"sender_id":  item.LastMessage.SenderID,
			"type":       item.LastMessage.Type,
			"timestamp":  item.LastMessage.CreatedAt,
		}
	}

	// Determine muted status from participants
	isMuted := false
	for _, p := range item.Participants {
		if p.IsMuted {
			isMuted = true
			break
		}
	}

	return gin.H{
		"chat_id":      item.Chat.ID,
		"type":         string(item.Chat.Type),
		"name":         name,
		"description":  description,
		"avatar_url":   avatarURL,
		"participants": participants,
		"last_message": lastMessage,
		"unread_count": item.UnreadCount,
		"is_muted":     isMuted,
		"created_at":   item.Chat.CreatedAt.Format(time.RFC3339),
		"updated_at":   item.Chat.UpdatedAt.Format(time.RFC3339),
	}
}
