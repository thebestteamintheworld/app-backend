import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress


class WebSocket(port: Int, private var apiAddress: String) : WebSocketServer(InetSocketAddress(port)) {

    private lateinit var api: ApiSubscriptor



    val gson: Gson = Gson()

    override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
        api = ApiSubscriptor(this, apiAddress)
        api.startListening()
        println("connected")
    }

    override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
        api.stopListening()
        println("closed")
    }

    override fun onMessage(p0: WebSocket?, p1: String?) {
        if (p1?.length!! > 100) stop()
        println(p1)
        processMessage(p1)
    }

    override fun onStart() {
        println("started")
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {
        api.stopListening()
        println("ERROR!")
        p1?.printStackTrace()
    }

    private fun processMessage(message: String) {
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
//                stop()
            }
        }
    }
}