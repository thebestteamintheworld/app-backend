import com.dxfeed.api.DXEndpoint
import com.dxfeed.api.DXFeedEventListener
import com.dxfeed.api.DXFeedSubscription
import com.dxfeed.event.market.Quote


class ApiSubscriptor(private var ws: WebSocket, private var apiAddress: String) {

    private var symbols: List<String> = listOf("USD/CNH:AFX{mm=CFH2}")
    private var symbol: String = "USD/CNH:AFX{mm=CFH2}"
    private var keepListening: Boolean = true

    private lateinit var sub: DXFeedSubscription<Quote>
    private lateinit var listener: DXFeedEventListener<Quote>

    private var markup: Double = 1.0

    public fun startListening() {
        val feed = DXEndpoint.create().connect(apiAddress).feed
        listener = DXFeedEventListener {
            fun eventsReceived(events: List<Quote?>?) {
                for (quote in events!!) {
                    ws.broadcast(quote.toString())
                }
            }
        }
        sub = feed.createSubscription(Quote::class.java).apply {
            addEventListener(listener)
        }
        sub.setSymbols(symbols)
    }

    public fun stopListening() {
        sub.removeEventListener(listener)
    }

    public fun changeSymbols(symbols: Array<String>) {
        sub.setSymbols(symbols)
    }

    public fun setMarkup(new: Double) {
        markup = new
    }

}