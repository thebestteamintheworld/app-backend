import com.dxfeed.api.DXEndpoint
import com.dxfeed.api.DXFeedEventListener
import com.dxfeed.api.DXFeedSubscription
import com.dxfeed.event.market.Quote


class ApiSubscriptor(private var ws: WebSocket, private var apiAddress: String) {

    private var symbols: List<String> = listOf("USD/HKD:AFX{mm=CFH2}")
    private var symbol: String = "EUR/USD:AFX{mm=CFH2}"
    private var keepListening: Boolean = true

    private lateinit var sub: DXFeedSubscription<Quote>
    private lateinit var listener: DXFeedEventListener<Quote>

    private var markup: Double = 1.0

    private lateinit var pointer: DXFeedSubscription<Quote>

     fun startListening() {
        val feed = DXEndpoint.create().connect(apiAddress).feed
        val sub: DXFeedSubscription<Quote> = feed.createSubscription(Quote::class.java).apply {
            addEventListener(DXFeedEventListener<Quote> { events -> for (quote in events) println(quote.toString()) })
            addSymbols(symbol)
        }
        pointer = sub
    }

     fun stopListening() {
        pointer.removeEventListener(listener)
    }

     fun changeSymbols(symbols: List<String>) {
        println("${symbols[0]} ${symbols[1]}")
        pointer.setSymbols(symbols.map { symbol -> "$symbol:AFX{mm=CFH2}" })
        println(pointer.symbols)
    }

     fun setMarkup(new: Double) {
        markup = new
    }

}