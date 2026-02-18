package handler

import (
	"net/http/httputil"
	"net/url"
	"strings"

	"github.com/gin-gonic/gin"
)

// RegisterNotificationRoutes adds proxy routes for client notification device
// management. The client calls /api/v1/notifications/devices/* while the backend
// serves these under /api/v1/users/devices/*.
func RegisterNotificationRoutes(
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

	rewriteProxy := func(c *gin.Context) {
		setUserHeaders(c)
		// Rewrite /api/v1/notifications/devices/... -> /api/v1/users/devices/...
		c.Request.URL.Path = strings.Replace(
			c.Request.URL.Path,
			"/api/v1/notifications/devices",
			"/api/v1/users/devices",
			1,
		)
		proxy.ServeHTTP(c.Writer, c.Request)
	}

	handlers := append(mws, rewriteProxy)
	engine.Any("/api/v1/notifications/devices", handlers...)
	engine.Any("/api/v1/notifications/devices/*path", handlers...)
}
