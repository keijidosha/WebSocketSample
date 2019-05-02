import java.net.URI;

import javax.websocket.ClientEndpoint
import javax.websocket.ContainerProvider
import javax.websocket.OnClose
import javax.websocket.OnError
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.WebSocketContainer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine

interface TextMessageObserver {
    fun onReceive(msg: String)
}

@ClientEndpoint
class WebSocketClientMain {
    var session: Session? = null
    var textMessageObserver: TextMessageObserver? = null

    @OnOpen
    fun onOpen( @Suppress("UNUSED_PARAMETER") session: Session ) {
        this.session = session
        /* セッション確立時の処理 */
        println("[セッション確立]")
    }

    @OnMessage
    fun onMessage( message: String ) {
        /* メッセージ受信時の処理 */
        println("[受信]:" + message)
        textMessageObserver?.onReceive(message)
    }

    @OnError
    fun onError( th: Throwable ) {
        /* エラー発生時の処理 */
        println( th.toString())
    }

    @OnClose
    fun onClose( @Suppress("UNUSED_PARAMETER") session: Session ) {
        this.session = null
        /* セッション解放時の処理 */
        println( "[クローズ]")
    }

    fun sendText( msg: String ) {
        session?.basicRemote?.sendText( msg )
    }

    fun close() {
        session?.close()
    }
}

fun main() = runBlocking<Unit> {
    println("start")
    val uri: URI = URI.create( "ws://127.0.0.1:8080/hoge" )
    val container: WebSocketContainer = ContainerProvider.getWebSocketContainer()
    val wsClient = WebSocketClientMain()
    var session: Session = container.connectToServer( wsClient, uri )

    delay(500L)

    // タイムアウトなし版
    fun sendMessageAndWait(sendMsg: String) = async(Dispatchers.Default) {
        // val msg = suspendCoroutine<String> { continuation ->
        val msg = suspendCancellableCoroutine<String> { continuation ->
            wsClient.textMessageObserver = object: TextMessageObserver {
                override fun onReceive(msg: String) {
                    continuation.resume(msg)
                }
            }
            session.basicRemote.sendText(sendMsg)
        }
        return@async msg
    }


    // タイムアウトあり版
    fun waitMessageWithTimeout(timeout: Long) = async(Dispatchers.Default) {
        try {
            val msg = withTimeout(timeout) {
                suspendCancellableCoroutine<String> { continuation ->
                    wsClient.textMessageObserver = object: TextMessageObserver {
                        override fun onReceive(msg: String) {
                            continuation.resume(msg)
                        }
                    }
                }
            }
            return@async msg
        } catch( ex: TimeoutCancellationException) {
            "timeout!"
        }
    }

    println("sended start")
    var msg = sendMessageAndWait("start").await()
    println("message received: " + msg)

    println("sended stop")
    msg = sendMessageAndWait("stop").await()
    println("message received: " + msg)

    println("sended close")
    msg = sendMessageAndWait("close").await()
    println("message received: " + msg)

    wsClient.close()
    session.close()
    // System.exit(0)
}