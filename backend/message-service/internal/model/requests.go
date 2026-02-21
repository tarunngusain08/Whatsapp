package model

type SendMessageRequest struct {
	ChatID           string         `json:"chat_id"            binding:"required"`
	Type             MessageType    `json:"type"               binding:"required"`
	Payload          MessagePayload `json:"payload"            binding:"required"`
	ClientMsgID      string         `json:"client_msg_id"      binding:"required"`
	ReplyToMessageID string         `json:"reply_to_message_id"`
	ForwardedFrom    *ForwardedFrom `json:"forwarded_from"`
}

type UpdateStatusRequest struct {
	Status string `json:"status" binding:"required"`
}

type ListMessagesQuery struct {
	ChatID   string `form:"chat_id"   binding:"required"`
	UserID   string `form:"-"`
	Cursor   string `form:"cursor"`
	CursorID string `form:"cursor_id"`
	Limit    int    `form:"limit"`
}

type SearchMessagesQuery struct {
	ChatID string `form:"chat_id" binding:"required"`
	Query  string `form:"q"       binding:"required"`
	Limit  int    `form:"limit"`
}

type ForwardRequest struct {
	TargetChatIDs []string `json:"target_chat_ids" binding:"required"`
}
