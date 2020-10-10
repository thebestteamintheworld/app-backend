@file:JvmName("Start")


fun main(apiAddress: String = "demo.dxfeed.com:7306", serverPort: Int = 7777) {

    val ws = WebSocket(serverPort, apiAddress)

    ws.run()

}