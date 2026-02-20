package handler

import (
	"io"
	"net/http"
	"net/url"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var wsUpgrader = websocket.Upgrader{
	CheckOrigin:     wsCheckOrigin,
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
}

// wsCheckOrigin validates WebSocket Origin header.
// TODO: load allowed origins from config for production.
func wsCheckOrigin(r *http.Request) bool {
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

func WSProxyHandler(wsTargetURL string) gin.HandlerFunc {
	target, err := url.Parse(wsTargetURL)
	if err != nil {
		panic("invalid ws target URL: " + wsTargetURL)
	}

	return func(c *gin.Context) {
		clientConn, err := wsUpgrader.Upgrade(c.Writer, c.Request, nil)
		if err != nil {
			return
		}
		defer clientConn.Close()

		header := http.Header{}
		if uid, ok := c.Get("user_id"); ok {
			if uidStr, ok := uid.(string); ok {
				header.Set("X-User-ID", uidStr)
			}
		}
		if phone, ok := c.Get("phone"); ok {
			if phoneStr, ok := phone.(string); ok {
				header.Set("X-User-Phone", phoneStr)
			}
		}
		header.Set("Authorization", c.GetHeader("Authorization"))

		backendConn, _, err := websocket.DefaultDialer.Dial(target.String(), header)
		if err != nil {
			clientConn.WriteMessage(websocket.CloseMessage,
				websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "backend unavailable"))
			return
		}
		defer backendConn.Close()

		errCh := make(chan error, 2)
		go pumpWS(clientConn, backendConn, errCh)
		go pumpWS(backendConn, clientConn, errCh)
		<-errCh
	}
}

func pumpWS(src, dst *websocket.Conn, errCh chan<- error) {
	for {
		msgType, reader, err := src.NextReader()
		if err != nil {
			errCh <- err
			return
		}
		writer, err := dst.NextWriter(msgType)
		if err != nil {
			errCh <- err
			return
		}
		if _, err := io.Copy(writer, reader); err != nil {
			errCh <- err
			return
		}
		writer.Close()
	}
}
