package health

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

type Checker func(ctx context.Context) error

type Handler struct {
	mu       sync.RWMutex
	checkers map[string]Checker
}

func NewHandler() *Handler {
	return &Handler{checkers: make(map[string]Checker)}
}

func (h *Handler) AddChecker(name string, fn Checker) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.checkers[name] = fn
}

func (h *Handler) RegisterRoutes(r *gin.Engine) {
	r.GET("/health", h.liveness)
	r.GET("/ready", h.readiness)
}

func (h *Handler) liveness(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"status": "alive"})
}

func (h *Handler) readiness(c *gin.Context) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	ctx, cancel := context.WithTimeout(c.Request.Context(), 5*time.Second)
	defer cancel()

	results := make(map[string]string, len(h.checkers))
	allOK := true
	for name, check := range h.checkers {
		if err := check(ctx); err != nil {
			results[name] = err.Error()
			allOK = false
		} else {
			results[name] = "ok"
		}
	}

	status := http.StatusOK
	if !allOK {
		status = http.StatusServiceUnavailable
	}
	c.JSON(status, gin.H{"status": results})
}
