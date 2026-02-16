package model

import (
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// Client represents a single WebSocket connection.
type Client struct {
	Conn     *websocket.Conn
	UserID   string
	Phone    string
	Send     chan []byte
	JoinedAt time.Time
}

// Hub maintains the set of active clients and routes messages.
type Hub struct {
	mu      sync.RWMutex
	clients map[string][]*Client // userID -> list of connections (multi-device)
}

func NewHub() *Hub {
	return &Hub{clients: make(map[string][]*Client)}
}

// Register adds a client to the hub.
func (h *Hub) Register(client *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.clients[client.UserID] = append(h.clients[client.UserID], client)
}

// Unregister removes a specific client from the hub.
func (h *Hub) Unregister(client *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	conns := h.clients[client.UserID]
	for i, c := range conns {
		if c == client {
			h.clients[client.UserID] = append(conns[:i], conns[i+1:]...)
			break
		}
	}
	if len(h.clients[client.UserID]) == 0 {
		delete(h.clients, client.UserID)
	}
}

// GetClients returns all connections for a user.
func (h *Hub) GetClients(userID string) []*Client {
	h.mu.RLock()
	defer h.mu.RUnlock()
	clients := h.clients[userID]
	out := make([]*Client, len(clients))
	copy(out, clients)
	return out
}

// IsConnected returns true if the user has at least one active connection.
func (h *Hub) IsConnected(userID string) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients[userID]) > 0
}

// AllUserIDs returns all currently connected user IDs.
func (h *Hub) AllUserIDs() []string {
	h.mu.RLock()
	defer h.mu.RUnlock()
	ids := make([]string, 0, len(h.clients))
	for id := range h.clients {
		ids = append(ids, id)
	}
	return ids
}
