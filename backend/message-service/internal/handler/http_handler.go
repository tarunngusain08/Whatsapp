package handler

import (
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"

	apperr "github.com/whatsapp-clone/backend/pkg/errors"
	"github.com/whatsapp-clone/backend/pkg/response"

	"github.com/whatsapp-clone/backend/message-service/internal/model"
	"github.com/whatsapp-clone/backend/message-service/internal/service"
)

type HTTPHandler struct {
	msgSvc service.MessageService
	log    zerolog.Logger
}

func NewHTTPHandler(msgSvc service.MessageService, log zerolog.Logger) *HTTPHandler {
	return &HTTPHandler{msgSvc: msgSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(rg *gin.RouterGroup) {
	msgs := rg.Group("/messages")
	{
		msgs.GET("", h.ListMessages)
		msgs.POST("", h.SendMessage)
		msgs.POST("/read", h.MarkAsRead)
		msgs.GET("/search", h.SearchMessages)
		msgs.GET("/search-global", h.SearchGlobal)
		msgs.DELETE("/:messageId", h.DeleteMessage)
		msgs.POST("/:messageId/forward", h.ForwardMessage)
		msgs.POST("/:messageId/star", h.StarMessage)
		msgs.DELETE("/:messageId/star", h.UnstarMessage)
		msgs.POST("/:messageId/react", h.ReactToMessage)
		msgs.DELETE("/:messageId/react", h.RemoveReaction)
		msgs.GET("/:messageId/receipts", h.GetMessageReceipts)
	}
}

func (h *HTTPHandler) ListMessages(c *gin.Context) {
	chatID := c.Query("chat_id")
	if chatID == "" {
		response.Error(c, apperr.NewBadRequest("chat_id query parameter is required"))
		return
	}

	limit := 50
	if limitStr := c.Query("limit"); limitStr != "" {
		if v, err := strconv.Atoi(limitStr); err == nil && v > 0 {
			limit = v
		}
	}
	if limit > 100 {
		limit = 100
	}

	query := &model.ListMessagesQuery{
		ChatID:   chatID,
		Cursor:   c.Query("cursor"),
		CursorID: c.Query("cursor_id"),
		Limit:    limit,
	}

	msgs, err := h.msgSvc.GetMessages(c.Request.Context(), query)
	if err != nil {
		response.Error(c, err)
		return
	}

	var meta *response.Meta
	if len(msgs) > 0 {
		last := msgs[len(msgs)-1]
		meta = &response.Meta{
			NextCursor: last.CreatedAt.Format(time.RFC3339Nano),
			HasMore:    len(msgs) == limit,
		}
	} else {
		meta = &response.Meta{HasMore: false}
	}

	response.OKWithMeta(c, msgs, meta)
}

func (h *HTTPHandler) SendMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	var req model.SendMessageRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body: "+err.Error()))
		return
	}
	if req.ChatID == "" {
		response.Error(c, apperr.NewBadRequest("chat_id is required"))
		return
	}

	msg, err := h.msgSvc.SendMessage(c.Request.Context(), userID, &req)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.Created(c, msg)
}

func (h *HTTPHandler) DeleteMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	messageID := c.Param("messageId")
	forParam := c.DefaultQuery("for", "me")

	if forParam == "everyone" {
		if err := h.msgSvc.DeleteMessage(c.Request.Context(), messageID, userID); err != nil {
			response.Error(c, err)
			return
		}
	} else {
		_ = h.msgSvc.UnstarMessage(c.Request.Context(), messageID, userID)
	}

	response.NoContent(c)
}

func (h *HTTPHandler) ForwardMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	messageID := c.Param("messageId")

	var req model.ForwardRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body: "+err.Error()))
		return
	}

	var forwarded []*model.Message
	for _, targetChatID := range req.TargetChatIDs {
		msg, err := h.msgSvc.ForwardMessage(c.Request.Context(), userID, targetChatID, messageID)
		if err != nil {
			response.Error(c, err)
			return
		}
		forwarded = append(forwarded, msg)
	}

	response.Created(c, forwarded)
}

func (h *HTTPHandler) StarMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	messageID := c.Param("messageId")
	if err := h.msgSvc.StarMessage(c.Request.Context(), messageID, userID); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

func (h *HTTPHandler) UnstarMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	messageID := c.Param("messageId")
	if err := h.msgSvc.UnstarMessage(c.Request.Context(), messageID, userID); err != nil {
		response.Error(c, err)
		return
	}

	response.NoContent(c)
}

// MarkAsRead marks all recent unread messages in a chat as read for the user.
func (h *HTTPHandler) MarkAsRead(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}

	var body struct {
		ChatID string `json:"chat_id"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, apperr.NewBadRequest("invalid request body"))
		return
	}
	chatID := body.ChatID
	if chatID == "" {
		response.Error(c, apperr.NewBadRequest("chat_id is required"))
		return
	}

	msgs, err := h.msgSvc.GetMessages(c.Request.Context(), &model.ListMessagesQuery{
		ChatID: chatID,
		Limit:  100,
	})
	if err != nil {
		response.Error(c, err)
		return
	}

	for _, msg := range msgs {
		if msg.SenderID == userID {
			continue
		}
		if rs, ok := msg.Status[userID]; ok && rs.Status == model.StatusRead {
			continue
		}
		_ = h.msgSvc.UpdateStatus(c.Request.Context(), msg.MessageID, userID, string(model.StatusRead))
	}

	response.NoContent(c)
}

func (h *HTTPHandler) SearchMessages(c *gin.Context) {
	chatID := c.Query("chat_id")
	if chatID == "" {
		response.Error(c, apperr.NewBadRequest("chat_id query parameter is required"))
		return
	}
	q := c.Query("q")
	if q == "" {
		response.Error(c, apperr.NewBadRequest("query parameter 'q' is required"))
		return
	}

	limit := 20
	if limitStr := c.Query("limit"); limitStr != "" {
		if v, err := strconv.Atoi(limitStr); err == nil && v > 0 {
			limit = v
		}
	}

	msgs, err := h.msgSvc.SearchMessages(c.Request.Context(), chatID, q, limit)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, msgs)
}

// ReactToMessage adds or replaces a user's reaction on a message.
func (h *HTTPHandler) ReactToMessage(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}
	messageID := c.Param("messageId")
	var body struct {
		Emoji string `json:"emoji" binding:"required"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		response.Error(c, apperr.NewBadRequest("emoji is required"))
		return
	}
	if err := h.msgSvc.ReactToMessage(c.Request.Context(), messageID, userID, body.Emoji); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

// RemoveReaction removes the calling user's reaction from a message.
func (h *HTTPHandler) RemoveReaction(c *gin.Context) {
	userID := c.GetHeader("X-User-ID")
	if userID == "" {
		response.Error(c, apperr.NewUnauthorized("missing X-User-ID header"))
		return
	}
	messageID := c.Param("messageId")
	if err := h.msgSvc.RemoveReaction(c.Request.Context(), messageID, userID); err != nil {
		response.Error(c, err)
		return
	}
	response.NoContent(c)
}

// GetMessageReceipts returns per-recipient read/delivery status breakdown for a message.
func (h *HTTPHandler) GetMessageReceipts(c *gin.Context) {
	messageID := c.Param("messageId")
	msg, err := h.msgSvc.GetMessageByID(c.Request.Context(), messageID)
	if err != nil {
		response.Error(c, err)
		return
	}
	type receipt struct {
		UserID    string    `json:"user_id"`
		Status    string    `json:"status"`
		UpdatedAt time.Time `json:"updated_at"`
	}
	receipts := make([]receipt, 0, len(msg.Status))
	for uid, rs := range msg.Status {
		receipts = append(receipts, receipt{
			UserID:    uid,
			Status:    string(rs.Status),
			UpdatedAt: rs.UpdatedAt,
		})
	}
	response.OK(c, receipts)
}

// SearchGlobal searches messages across all chats a user participates in.
// Requires chat_ids as a comma-separated query parameter (provided by the gateway
// or a prior /chats list call) since message-service does not have a chat-service gRPC client.
func (h *HTTPHandler) SearchGlobal(c *gin.Context) {
	q := c.Query("q")
	if q == "" {
		response.Error(c, apperr.NewBadRequest("query parameter 'q' is required"))
		return
	}

	chatIDsParam := c.Query("chat_ids")
	if chatIDsParam == "" {
		response.Error(c, apperr.NewBadRequest("query parameter 'chat_ids' is required"))
		return
	}

	chatIDs := strings.Split(chatIDsParam, ",")
	for i := range chatIDs {
		chatIDs[i] = strings.TrimSpace(chatIDs[i])
	}

	limit := 20
	if limitStr := c.Query("limit"); limitStr != "" {
		if v, err := strconv.Atoi(limitStr); err == nil && v > 0 {
			limit = v
		}
	}

	userID := c.GetHeader("X-User-ID")
	msgs, err := h.msgSvc.SearchGlobal(c.Request.Context(), userID, q, chatIDs, limit)
	if err != nil {
		response.Error(c, err)
		return
	}

	response.OK(c, msgs)
}
