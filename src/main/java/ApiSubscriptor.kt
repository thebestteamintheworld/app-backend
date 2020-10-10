import com.dxfeed.api.DXEndpoint
import com.dxfeed.api.DXFeedEventListener
import com.dxfeed.api.DXFeedSubscription
import com.dxfeed.event.market.Quote
import com.google.gson.Gson


class ApiSubscriptor(private var ws: WebSocket, private var apiAddress: String, private var gson: Gson) {

    private var symbols: List<String> = listOf("USD/HKD:AFX{mm=CFH2}")

    private lateinit var listener: DXFeedEventListener<Quote>

    private var markup: Double = 1.0

    private lateinit var sub: DXFeedSubscription<Quote>

    private val sleep: Long = 1000L

    fun startListening() {
        listener = DXFeedEventListener { events ->
            val quotesToSend: MutableList<Quote> = mutableListOf<Quote>()

            for (quote in events) {
                println(this.symbols)
                if (!quotesToSend.any { q -> q.eventSymbol == quote.eventSymbol })
                    quotesToSend.add(quote)
            }

            for (quote in quotesToSend) {
                quote.bidPrice *= markup
                quote.askPrice *= markup
                ws.broadcast(gson.toJson(quote))
                println(gson.toJson(quote))
            }

            Thread.sleep(sleep)
        }
        val feed = DXEndpoint.create().connect(apiAddress).feed
        sub = feed.createSubscription(Quote::class.java).apply {
            addEventListener(listener)
            addSymbols(symbols)
        }
    }

    fun stopListening() {
        sub.removeEventListener(listener)
    }

    fun changeSymbols(symbols: List<String>) {
        sub.removeSymbols(this.symbols)
        this.symbols = symbols.map { symbol -> "$symbol:AFX{mm=CFH2}" }
        sub.setSymbols(this.symbols)
        println(sub.symbols)
    }

    fun setMarkup(new: Double) {
        this.markup = new
    }

}