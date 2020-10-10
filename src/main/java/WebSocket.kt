import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress


class WebSocket(port: Int, private var apiAddress: String) : WebSocketServer(InetSocketAddress(port)) {

    private lateinit var api: ApiSubscriptor


    private val gson: Gson = Gson()

    override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
        api = ApiSubscriptor(this, apiAddress, gson)
        api.startListening()
        println("NEW CONNECTION")
    }

    override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
        api.stopListening()
        println("CONNECTION CLOSED")
    }

    override fun onMessage(p0: WebSocket?, p1: String?) {
        if (p1?.length!! > 200) stop()
        println(p1)
        processMessage(p1)
    }

    override fun onStart() {
        println("WEBSOCKET STARTED")
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {
        println("WEBSOCKET ERROR!")
        p1?.printStackTrace()
    }

    private fun processMessage(message: String) {
        try {
            val request: WebSocketRequest = gson.fromJson<WebSocketRequest>(message, WebSocketRequest::class.java)
            when (request.type) {
                "change" -> {
                    api.changeSymbols(request.symbols!!)
                }
                "close" -> {
                    println("Event: Stop")
                    api.stopListening()
                    this.stop()
                }
                "markup" -> {
                    println("Event: markup")
                    api.setMarkup(request.new!!)
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}