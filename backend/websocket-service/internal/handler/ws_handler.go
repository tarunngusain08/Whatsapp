package handler

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"

	"github.com/whatsapp-clone/backend/websocket-service/config"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
	"github.com/whatsapp-clone/backend/websocket-service/internal/service"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     checkOrigin,
}

// checkOrigin validates the WebSocket Origin header.
// TODO: load allowed origins from config for production.
func checkOrigin(r *http.Request) bool {
	origin := r.Header.Get("Origin")
	if origin == "" {
		return true
	}
	allowedOrigins := []string{
		"http://localhost", "https://localhost",
		"http://127.0.0.1", "https://127.0.0.1",
	}
	for _, allowed := range allowedOrigins {
		if strings.HasPrefix(origin, allowed) {
			return true
		}
	}
	return false
}

// WSHandler handles WebSocket upgrade requests and manages read/write pumps.
type WSHandler struct {
	hub     *model.Hub
	wsSvc   service.WebSocketService
	authVal service.AuthValidator
	cfg     *config.Config
	log     zerolog.Logger
}

// NewWSHandler creates a new WSHandler.
func NewWSHandler(
	hub *model.Hub,
	wsSvc service.WebSocketService,
	authVal service.AuthValidator,
	cfg *config.Config,
	log zerolog.Logger,
) *WSHandler {
	return &WSHandler{
		hub:     hub,
		wsSvc:   wsSvc,
		authVal: authVal,
		cfg:     cfg,
		log:     log,
	}
}

// ServeWS handles the HTTP -> WebSocket upgrade and starts the read/write pumps.
func (h *WSHandler) ServeWS(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("X-User-ID")
	phone := r.Header.Get("X-User-Phone")

	// If no identity headers from api-gateway, fall back to token validation.
	if userID == "" {
		token := r.URL.Query().Get("token")
		if token == "" {
			token = r.Header.Get("Authorization")
			if len(token) > 7 && token[:7] == "Bearer " {
				token = token[7:]
			}
		}

		if token == "" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		var err error
		userID, phone, err = h.authVal.ValidateToken(r.Context(), token)
		if err != nil {
			h.log.Warn().Err(err).Msg("token validation failed")
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		h.log.Error().Err(err).Msg("websocket upgrade failed")
		return
	}

	client := &model.Client{
		Conn:     conn,
		UserID:   userID,
		Phone:    phone,
		Send:     make(chan []byte, 256),
		JoinedAt: time.Now(),
	}

	h.hub.Register(client)
	h.log.Info().Str("user_id", userID).Msg("client connected")

	ctx := context.Background()
	_ = h.wsSvc.SetPresence(ctx, userID, true)
	h.wsSvc.NotifyPresenceChange(userID, true)
	_ = h.wsSvc.StartRedisSubscriber(ctx, client)

	go h.writePump(client)
	go h.readPump(client)
}

// readPump reads messages from the WebSocket connection and routes them to the service layer.
func (h *WSHandler) readPump(client *model.Client) {
	defer func() {
		h.hub.Unregister(client)
		_ = h.wsSvc.StopRedisSubscriber(client)

		if !h.hub.IsConnected(client.UserID) {
			_ = h.wsSvc.SetPresence(context.Background(), client.UserID, false)
			h.wsSvc.NotifyPresenceChange(client.UserID, false)
			h.wsSvc.CleanupPresenceSubscriptions(client.UserID)
		}

		client.Conn.Close()
		close(client.Send)
		h.log.Info().Str("user_id", client.UserID).Msg("client disconnected")
	}()

	client.Conn.SetReadLimit(h.cfg.MaxMessageSize)
	_ = client.Conn.SetReadDeadline(time.Now().Add(h.cfg.PongTimeout))
	client.Conn.SetPongHandler(func(string) error {
		_ = client.Conn.SetReadDeadline(time.Now().Add(h.cfg.PongTimeout))
		_ = h.wsSvc.SetPresence(context.Background(), client.UserID, true)
		return nil
	})

	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseNormalClosure) {
				h.log.Warn().Err(err).Str("user_id", client.UserID).Msg("websocket read error")
			}
			return
		}

		var event model.WSEvent
		if err := json.Unmarshal(message, &event); err != nil {
			h.sendError(client, "invalid event format")
			continue
		}

		if err := h.wsSvc.HandleEvent(context.Background(), client, &event); err != nil {
			h.log.Error().Err(err).Str("type", event.Type).Str("user_id", client.UserID).Msg("event handling failed")
			h.sendError(client, err.Error())
		}
	}
}

// writePump writes messages to the WebSocket connection and sends periodic pings.
func (h *WSHandler) writePump(client *model.Client) {
	ticker := time.NewTicker(h.cfg.PingInterval)
	defer func() {
		ticker.Stop()
		client.Conn.Close()
	}()

	for {
		select {
		case message, ok := <-client.Send:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(h.cfg.WriteTimeout))
			if !ok {
				_ = client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := client.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
		case <-ticker.C:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(h.cfg.WriteTimeout))
			if err := client.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// sendError sends an error event to a client.
func (h *WSHandler) sendError(client *model.Client, msg string) {
	event := model.WSEvent{Type: "error"}
	event.Payload, _ = json.Marshal(map[string]string{"message": msg})
	data, _ := json.Marshal(event)
	select {
	case client.Send <- data:
	default:
	}
}
