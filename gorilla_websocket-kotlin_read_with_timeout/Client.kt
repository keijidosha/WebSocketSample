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
    val mode = 2

    println("start")
    val uri: URI = URI.create( "ws://127.0.0.1:8080/hoge" )
    val container: WebSocketContainer = ContainerProvider.getWebSocketContainer()
    val wsClient = WebSocketClientMain()
    var session: Session = container.connectToServer( wsClient, uri )
    /*
    wsClient.textMessageObserver = object: TextMessageObserver {
        override fun onReceive(msg: String) {
            println("message: " + msg)
        }
    }
    */

    delay(1000L)

    // タイムアウトなし版
    fun waitMessage() = async(Dispatchers.Default) {
        // val msg = suspendCoroutine<String> { continuation ->
        val msg = suspendCancellableCoroutine<String> { continuation ->
            wsClient.textMessageObserver = object: TextMessageObserver {
                override fun onReceive(msg: String) {
                    continuation.resume(msg)
                }
            }
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

    session.basicRemote.sendText( "hoge" )
    println("sended message")
    when(mode) {
        1 -> {
            println("wait response by mode 1")
            val msg = waitMessageWithTimeout(900L).await()
            println("The Received Message is: " + msg)
        }
        else -> {
            println("wait response by mode 2")
            val job = waitMessage()
            val msg = try {
                withTimeout(900L) {
                    job.await()
                }
            } catch(ex: TimeoutCancellationException){
                println("canceling")
                job.cancel()
                "timeout!"
            }
            println("The Received Message is: " + msg)
        }
    }

    wsClient.close()
    // System.exit(0)
}