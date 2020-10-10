@file:JvmName("Start")


fun main(apiAddress: String = "demo.dxfeed.com:7306", serverPort: Int = 7777) {

    val ws = WebSocket(serverPort, apiAddress)

    ws.run();

}


//    fun startAPI() {
//        val address = "demo.dxfeed.com:7300"
//        val symbol = "IBM"
//        val feed = DXEndpoint.create().connect(address).feed
//        val sub: DXFeedSubscription<*> = feed.createSubscription(Trade::class.java).apply {
//            addEventListener(DXFeedEventListener<Trade> { events ->
//                for (quote in events) {
////                    println(quote)
////                    print(Gson().toJson(quote))
//
//                    ws?.broadcast(Gson().toJson(quote))
//
//                }
//            })
//            addSymbols(symbol)
//        }
//        while (!Thread.interrupted()) {
//
//        }
//        Thread.sleep(1000)
//    }


