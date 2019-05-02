package main

import (
	"log"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
)

// コネクションを管理するリスト
var connections = make(map[*websocket.Conn]int16)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func notifyTime() {
	for {
		time.Sleep(1 * time.Second)
		now := time.Now().Format("2006-01-02 15:04:05")
		for conn, count := range connections {
			if count == 10 {
				if err := conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(1000, "10 times")); err != nil {
					log.Printf("[ERROR]close message error: %s", err)
				} else {
					if err := conn.Close(); err != nil {
						log.Printf("[ERROR]close error: %s", err)
					} else {
						log.Printf("[INFO]connection closed %s", conn.RemoteAddr().String())
					}
				}
			} else {
				if err := conn.WriteMessage(websocket.TextMessage, []byte(now)); err != nil {
					log.Printf("[ERROR]%s", err)
				} else {
					connections[conn] = count + 1
				}
			}
		}
	}
}

func hogeHandler(w http.ResponseWriter, r *http.Request) {
	conn, _ := upgrader.Upgrade(w, r, nil) // error ignored for sake of simplicity
	log.Printf("[INFO]Connected from %s", conn.RemoteAddr().String())
	// connections = append(connections, conn)
	connections[conn] = 0

	for {
		// Read message from browser
		msgType, msg, err := conn.ReadMessage()
		if err != nil {
			if _, ok := err.(*websocket.CloseError); ok {
				_err := err.(*websocket.CloseError)
				log.Printf("Websocket was closed: %s, code=%d, text=%s", conn.RemoteAddr().String(), _err.Code, _err.Text)
			} else {
				log.Printf("WebSocket read error type=%T, error=%v", err, err)
			}
			delete(connections, conn)
			return
		}

		log.Printf("WebSocket received message: message type=%v, message=%v", msgType, msg)

	}
}

func main() {
	http.HandleFunc("/hoge/", hogeHandler)

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "index.html")
	})

	log.Print("[INFO]start")

	go notifyTime()

	http.ListenAndServe(":8080", nil)
}
