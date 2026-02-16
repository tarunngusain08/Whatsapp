package handler

import (
	"io"
	"net/http"
	"net/url"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var wsUpgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
}

func WSProxyHandler(wsTargetURL string) gin.HandlerFunc {
	return func(c *gin.Context) {
		clientConn, err := wsUpgrader.Upgrade(c.Writer, c.Request, nil)
		if err != nil {
			return
		}
		defer clientConn.Close()

		target, _ := url.Parse(wsTargetURL)
		header := http.Header{}
		if uid, exists := c.Get("user_id"); exists {
			header.Set("X-User-ID", uid.(string))
		}
		if phone, exists := c.Get("phone"); exists {
			header.Set("X-User-Phone", phone.(string))
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
