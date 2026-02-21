package handler

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"regexp"
	"strings"

	"github.com/gin-gonic/gin"
)

// Patterns for chat-scoped message paths
var (
	chatMessagesPattern = regexp.MustCompile(
		`^/api/v1/chats/([^/]+)/messages(?:/(.*))?$`,
	)
)

// RegisterChatRoutes registers a smart proxy under /api/v1/chats that inspects
// the full path and routes chat-scoped message requests to message-service,
// while all other chat paths go to chat-service.
func RegisterChatRoutes(
	engine *gin.Engine,
	chatHTTPAddr string,
	messageHTTPAddr string,
	authMW, rateLimitMW gin.HandlerFunc,
) {
	chatTarget, err := url.Parse(chatHTTPAddr)
	if err != nil {
		panic("invalid chat target URL: " + chatHTTPAddr)
	}
	msgTarget, err := url.Parse(messageHTTPAddr)
	if err != nil {
		panic("invalid message target URL: " + messageHTTPAddr)
	}

	chatProxy := httputil.NewSingleHostReverseProxy(chatTarget)
	chatProxy.ErrorHandler = proxyErrorHandler

	msgProxy := httputil.NewSingleHostReverseProxy(msgTarget)
	msgProxy.ErrorHandler = proxyErrorHandler

	mws := buildMiddlewareChain(authMW, rateLimitMW)

	smartProxy := func(c *gin.Context) {
		setUserHeaders(c)
		fullPath := c.Request.URL.Path

		// Check if this is a chat-scoped message path
		if matches := chatMessagesPattern.FindStringSubmatch(fullPath); matches != nil {
			chatID := matches[1]
			remainder := matches[2] // e.g., "", "read", ":messageId", ":messageId/star"

			rewriteChatScopedMessage(c, chatID, remainder)
			msgProxy.ServeHTTP(c.Writer, c.Request)
			return
		}

		// All other chat paths go to chat-service as-is
		chatProxy.ServeHTTP(c.Writer, c.Request)
	}

	handlers := append(mws, smartProxy)
	engine.Any("/api/v1/chats", handlers...)
	engine.Any("/api/v1/chats/*path", handlers...)
}

// rewriteChatScopedMessage rewrites chat-scoped message paths into
// the message-service's flat format.
func rewriteChatScopedMessage(c *gin.Context, chatID, remainder string) {
	method := c.Request.Method

	switch {
	case remainder == "" && method == http.MethodGet:
		// GET /api/v1/chats/:chatId/messages -> GET /api/v1/messages?chat_id=chatId
		q := c.Request.URL.Query()
		q.Set("chat_id", chatID)
		c.Request.URL.Path = "/api/v1/messages"
		c.Request.URL.RawQuery = q.Encode()

	case remainder == "" && method == http.MethodPost:
		// POST /api/v1/chats/:chatId/messages -> POST /api/v1/messages (inject chat_id)
		c.Request.URL.Path = "/api/v1/messages"
		injectChatIDIntoBody(c, chatID)

	case remainder == "read" && method == http.MethodPost:
		// POST /api/v1/chats/:chatId/messages/read -> POST /api/v1/messages/read
		c.Request.URL.Path = "/api/v1/messages/read"
		injectChatIDIntoBody(c, chatID)

	case strings.Contains(remainder, "/star"):
		// PUT|DELETE /api/v1/chats/:chatId/messages/:messageId/star
		messageID := strings.Split(remainder, "/")[0]
		c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s/star", messageID)

	case strings.Contains(remainder, "/react"):
		// POST|DELETE /api/v1/chats/:chatId/messages/:messageId/react
		messageID := strings.Split(remainder, "/")[0]
		c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s/react", messageID)

	case strings.Contains(remainder, "/forward"):
		messageID := strings.Split(remainder, "/")[0]
		c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s/forward", messageID)

	case strings.Contains(remainder, "/receipts"):
		messageID := strings.Split(remainder, "/")[0]
		c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s/receipts", messageID)

	default:
		// DELETE /api/v1/chats/:chatId/messages/:messageId -> DELETE /api/v1/messages/:messageId
		if method == http.MethodDelete && remainder != "" && !strings.Contains(remainder, "/") {
			c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s", remainder)
		} else {
			// Fallback: pass through to message-service
			c.Request.URL.Path = fmt.Sprintf("/api/v1/messages/%s", remainder)
		}
	}
}

func proxyErrorHandler(w http.ResponseWriter, r *http.Request, err error) {
	log.Printf("proxy error for %s %s: %v", r.Method, r.URL.Path, err)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusBadGateway)
	w.Write([]byte(`{"success":false,"error":{"code":"BAD_GATEWAY","message":"service unavailable"}}`))
}

func setUserHeaders(c *gin.Context) {
	if uid := c.GetString("user_id"); uid != "" {
		c.Request.Header.Set("X-User-ID", uid)
	}
	if phone := c.GetString("phone"); phone != "" {
		c.Request.Header.Set("X-User-Phone", phone)
	}
	if rid := c.GetString("request_id"); rid != "" {
		c.Request.Header.Set("X-Request-ID", rid)
	}
}

func buildMiddlewareChain(authMW, rateLimitMW gin.HandlerFunc) []gin.HandlerFunc {
	var mws []gin.HandlerFunc
	if rateLimitMW != nil {
		mws = append(mws, rateLimitMW)
	}
	if authMW != nil {
		mws = append(mws, authMW)
	}
	return mws
}

// injectChatIDIntoBody reads the request body, injects "chat_id" into the JSON,
// and replaces the body with the modified version using proper JSON marshaling.
func injectChatIDIntoBody(c *gin.Context, chatID string) {
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"success": false,
			"error":   gin.H{"code": "BAD_REQUEST", "message": "failed to read request body"},
		})
		return
	}
	defer c.Request.Body.Close()

	var data map[string]interface{}
	if len(body) == 0 || string(body) == "{}" {
		data = make(map[string]interface{})
	} else {
		if err := json.Unmarshal(body, &data); err != nil {
			c.AbortWithStatusJSON(400, gin.H{
				"success": false,
				"error":   gin.H{"code": "BAD_REQUEST", "message": "invalid JSON body"},
			})
			return
		}
	}
	data["chat_id"] = chatID

	newBody, err := json.Marshal(data)
	if err != nil {
		c.AbortWithStatusJSON(500, gin.H{
			"success": false,
			"error":   gin.H{"code": "INTERNAL", "message": "failed to marshal request body"},
		})
		return
	}

	c.Request.Body = io.NopCloser(strings.NewReader(string(newBody)))
	c.Request.ContentLength = int64(len(newBody))
}
