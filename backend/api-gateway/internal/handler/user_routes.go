package handler

import (
	"fmt"
	"net/http/httputil"
	"net/url"
	"regexp"

	"github.com/gin-gonic/gin"
)

// Pattern for client's block/unblock path: /api/v1/users/:userId/block
var userBlockPattern = regexp.MustCompile(
	`^/api/v1/users/([^/]+)/block$`,
)

// RegisterUserRoutes registers a smart proxy for /api/v1/users/* that rewrites
// client-specific paths (e.g., /users/:id/block -> /users/contacts/:id/block).
func RegisterUserRoutes(
	engine *gin.Engine,
	userHTTPAddr string,
	authMW, rateLimitMW gin.HandlerFunc,
) {
	target, err := url.Parse(userHTTPAddr)
	if err != nil {
		panic("invalid user target URL: " + userHTTPAddr)
	}

	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.ErrorHandler = proxyErrorHandler

	mws := buildMiddlewareChain(authMW, rateLimitMW)

	smartProxy := func(c *gin.Context) {
		setUserHeaders(c)
		fullPath := c.Request.URL.Path

		// Check if this is a block/unblock path from the client
		// Client: POST/DELETE /api/v1/users/:userId/block
		// Backend: POST/DELETE /api/v1/users/contacts/:id/block
		if matches := userBlockPattern.FindStringSubmatch(fullPath); matches != nil {
			userID := matches[1]
			// Don't rewrite if the path already includes "contacts"
			if userID != "contacts" {
				c.Request.URL.Path = fmt.Sprintf("/api/v1/users/contacts/%s/block", userID)
			}
		}

		proxy.ServeHTTP(c.Writer, c.Request)
	}

	handlers := append(mws, smartProxy)
	engine.Any("/api/v1/users", handlers...)
	engine.Any("/api/v1/users/*path", handlers...)
}
