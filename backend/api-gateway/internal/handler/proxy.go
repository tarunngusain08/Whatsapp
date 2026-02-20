package handler

import (
	"net/http"
	"net/http/httputil"
	"net/url"

	"github.com/gin-gonic/gin"
	"github.com/whatsapp-clone/backend/api-gateway/internal/model"
)

func RegisterProxyRoutes(engine *gin.Engine, routes []model.RouteTarget, authMW, rateLimitMW gin.HandlerFunc) {
	for _, route := range routes {
		rt := route // capture loop variable
		target, err := url.Parse(rt.TargetURL)
		if err != nil {
			panic("invalid target URL: " + rt.TargetURL)
		}

		proxy := httputil.NewSingleHostReverseProxy(target)
		proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
			w.WriteHeader(http.StatusBadGateway)
			w.Write([]byte(`{"success":false,"error":{"code":"BAD_GATEWAY","message":"service unavailable"}}`))
		}

		handlers := []gin.HandlerFunc{}
		if rt.RequireAuth {
			if rateLimitMW != nil {
				handlers = append(handlers, rateLimitMW)
			}
			if authMW != nil {
				handlers = append(handlers, authMW)
			}
		}
		proxyHandler := func(c *gin.Context) {
			if uid := c.GetString("user_id"); uid != "" {
				c.Request.Header.Set("X-User-ID", uid)
			}
			if phone := c.GetString("phone"); phone != "" {
				c.Request.Header.Set("X-User-Phone", phone)
			}
			if rid := c.GetString("request_id"); rid != "" {
				c.Request.Header.Set("X-Request-ID", rid)
			}
			proxy.ServeHTTP(c.Writer, c.Request)
		}

		// Register both the exact prefix and the catch-all wildcard
		// so that requests like POST /api/v1/chats and GET /api/v1/chats/:id both work.
		exactHandlers := make([]gin.HandlerFunc, len(handlers), len(handlers)+1)
		copy(exactHandlers, handlers)
		exactHandlers = append(exactHandlers, proxyHandler)

		wildcardHandlers := make([]gin.HandlerFunc, len(handlers), len(handlers)+1)
		copy(wildcardHandlers, handlers)
		wildcardHandlers = append(wildcardHandlers, proxyHandler)

		engine.Any(rt.PathPrefix, exactHandlers...)
		engine.Any(rt.PathPrefix+"/*path", wildcardHandlers...)
	}
}
