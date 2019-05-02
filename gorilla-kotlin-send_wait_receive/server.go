package main

import (
	"log"
	"net/http"

	"./mylog"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func answerToStart(conn *websocket.Conn) {
	// time.Sleep(1 * time.Second)
	conn.WriteMessage(websocket.TextMessage, []byte("started"))
}

func answerToStop(conn *websocket.Conn) {
	// time.Sleep(1 * time.Second)
	conn.WriteMessage(websocket.TextMessage, []byte("stopped"))
}

func hogeHandler(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		mylog.Error(err)
		return
	}
	mylog.Infof("Connected from %s", conn.RemoteAddr().String())

	for {
		// Read message from browser
		msgType, msg, err := conn.ReadMessage()
		if err != nil {
			if _, ok := err.(*websocket.CloseError); ok {
				_err := err.(*websocket.CloseError)
				mylog.Infof("Websocket was closed: %s, code=%d, text=%s", conn.RemoteAddr().String(), _err.Code, _err.Text)
			} else {
				mylog.Errorf("WebSocket read error type=%T, error=%v", err, err)
			}
			return
		} else if msgType == websocket.TextMessage {
			mylog.Infof("WebSocket received message: message type=TextMessage, message=%v", string(msg))
			switch string(msg) {
			case "start":
				go answerToStart(conn)
			case "stop":
				go answerToStop(conn)
			case "close":
				// time.Sleep(1 * time.Second)
				conn.WriteMessage(websocket.TextMessage, []byte("closed"))
				conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(1000, "closed"))
				conn.Close()
				break
			}
		}

	}
}

func main() {
	log.SetFlags(log.Ldate | log.Ltime | log.Lshortfile)
	mylog.LogLevel = mylog.INFO
	http.HandleFunc("/hoge/", hogeHandler)

	mylog.Infof("start")

	http.ListenAndServe(":8080", nil)
}
