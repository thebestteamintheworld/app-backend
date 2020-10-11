import com.dxfeed.api.*
import com.dxfeed.event.market.Quote
import com.dxfeed.promise.Promise
import com.google.gson.Gson
import kotlin.system.exitProcess

data class QuoteJSON(
        val symbol: String,
        val bid: Double,
        val ask: Double,
        val spread: Double
)

class QuoteExtended : Quote {
    var markup: Double
        get() = field
        set(value) {
            field = value
        }

    fun getFixedMarkedupBid(): Double {
        return this.bidPrice * (1.0 - markup)
    }

    fun getFixedMarkedupAsk(): Double {
        return this.askPrice * (1.0 + markup)
    }

    fun getFloatMarkedupBid(): Double {
        return (1.0 - markup*(this.askPrice-this.bidPrice)/(this.askPrice+this.bidPrice))*this.bidPrice
    }

    fun getFloatMarkedupAsk(): Double {
        return (1.0 + markup*(this.askPrice-this.bidPrice)/(this.askPrice+this.bidPrice))*this.askPrice
    }

    fun getFloatMarkedUpSpread(): Double {
        return getFloatMarkedupAsk() - getFloatMarkedupBid()
    }

    fun getFixedMarkedUpSpread(): Double {
        return getFixedMarkedupAsk() - getFixedMarkedupBid()
    }

    constructor(q: Quote, markup: Double = 0.0) : super(q.eventSymbol) {
        super.setAskPrice(q.askPrice)
        super.setBidPrice(q.bidPrice)
        this.markup = markup
    }

}


class ApiSubscriptor {


    private var apiAddress: String
    private lateinit var gson: Gson
    private var feed: DXFeed
    private var markupPercent: Double = 0.01

    private var originalSymbols: List<String>

    private lateinit var listener: DXFeedEventListener<Quote>


    private lateinit var sub: DXFeedSubscription<Quote>

    private val sleep: Long = 1000L

    private var lastQuotes: MutableList<QuoteExtended>
    private lateinit var lastQuotesCopy: MutableList<QuoteExtended>

    private var crossSymbolsList: List<String>

    private var pauseFlag: Boolean = false

    constructor(apiAddress: String) {
        this.apiAddress = apiAddress
        this.originalSymbols = listOf("USD/CNH:AFX{mm=CFH2}", "EUR/USD:AFX{mm=CFH2}",
                "AUD/USD:AFX{mm=CFH2}", "USD/HKD:AFX{mm=CFH2}", "USD/CHF:AFX{mm=CFH2}",
                "EUR/GBP:AFX{mm=CFH2}", "GBP/USD:AFX{mm=CFH2}", "USD/CAD:AFX{mm=CFH2}")

        this.lastQuotes = mutableListOf<QuoteExtended>()
        feed = DXEndpoint.create().connect(apiAddress).feed
        val result: List<Promise<Quote>> = feed.getLastEventsPromises<Quote>(Quote::class.java, this.originalSymbols)
        for (r in result) {
            this.lastQuotes.add(QuoteExtended(r.await()))
        }

        var crossQuotes = mutableListOf<QuoteExtended>()

        crossSymbolsList = listOf("CNH", "EUR", "AUD", "HKD", "CHF", "GBP", "CAD")
        crossSymbolsList.forEach { symbol ->
            run {
                for (symbol2 in crossSymbolsList) {
                    if (symbol.equals(symbol2)) continue
                    val quote = Quote()
                    quote.eventSymbol = "$symbol/$symbol2:AFX{mm=CFH2}"

                    var usdBAsk: Double
                    var quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol:AFX{mm=CFH2}") }
                    if (quoteUSDB == null) usdBAsk = this.lastQuotes.find { it.eventSymbol.equals("$symbol/USD:AFX{mm=CFH2}") }!!.askPrice
                    else usdBAsk = 1 / quoteUSDB.askPrice

                    var usdCAsk: Double
                    quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol2:AFX{mm=CFH2}") }
                    if (quoteUSDB == null) usdCAsk = this.lastQuotes.find { it.eventSymbol.equals("$symbol2/USD:AFX{mm=CFH2}") }!!.askPrice
                    else usdCAsk = 1 / quoteUSDB.askPrice


                    var usdBBid: Double
                    quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol:AFX{mm=CFH2}") }
                    if (quoteUSDB == null) usdBBid = this.lastQuotes.find { it.eventSymbol.equals("$symbol/USD:AFX{mm=CFH2}") }!!.bidPrice
                    else usdBBid = 1 / quoteUSDB.bidPrice

                    var usdCBid: Double
                    quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol2:AFX{mm=CFH2}") }
                    if (quoteUSDB == null) usdCBid = this.lastQuotes.find { it.eventSymbol.equals("$symbol2/USD:AFX{mm=CFH2}") }!!.bidPrice
                    else usdCBid = 1 / quoteUSDB.bidPrice


                    quote.bidPrice = usdCBid / usdBBid
                    quote.askPrice = usdCAsk / usdBAsk

                    crossQuotes.add(QuoteExtended(quote))
                }
            }
        }

        this.lastQuotes.addAll(crossQuotes)

    }

    fun startUpdating() {

        listener = DXFeedEventListener { events ->

            lastQuotesCopy = this.lastQuotes.toMutableList()

            while (pauseFlag) {
                Thread.sleep(sleep)
            }

            for (quote in events) {
                var markup: Double = 0.0
                lastQuotesCopy.removeIf { it ->
                    run {
                        markup = it.markup
                        it.eventSymbol.equals(quote.eventSymbol)
                    }
                }

                lastQuotesCopy.add(QuoteExtended(quote, markup))
            }


            lastQuotesCopy.removeIf { it -> !originalSymbols.contains(it.eventSymbol) }

            var crossQuotes = mutableListOf<QuoteExtended>()

            crossSymbolsList.forEach { symbol ->
                run {
                    for (symbol2 in crossSymbolsList) {
                        if (symbol.equals(symbol2)) continue
                        val quote = Quote()
                        quote.eventSymbol = "$symbol/$symbol2:AFX{mm=CFH2}"

                        var usdBAsk: Double
                        var quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol:AFX{mm=CFH2}") }
                        if (quoteUSDB == null) usdBAsk = this.lastQuotes.find { it.eventSymbol.equals("$symbol/USD:AFX{mm=CFH2}") }!!.askPrice
                        else usdBAsk = 1 / quoteUSDB.askPrice

                        var usdCAsk: Double
                        quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol2:AFX{mm=CFH2}") }
                        if (quoteUSDB == null) usdCAsk = this.lastQuotes.find { it.eventSymbol.equals("$symbol2/USD:AFX{mm=CFH2}") }!!.askPrice
                        else usdCAsk = 1 / quoteUSDB.askPrice


                        var usdBBid: Double
                        quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol:AFX{mm=CFH2}") }
                        if (quoteUSDB == null) usdBBid = this.lastQuotes.find { it.eventSymbol.equals("$symbol/USD:AFX{mm=CFH2}") }!!.bidPrice
                        else usdBBid = 1 / quoteUSDB.bidPrice

                        var usdCBid: Double
                        quoteUSDB = this.lastQuotes.find { it.eventSymbol.equals("USD/$symbol2:AFX{mm=CFH2}") }
                        if (quoteUSDB == null) usdCBid = this.lastQuotes.find { it.eventSymbol.equals("$symbol2/USD:AFX{mm=CFH2}") }!!.bidPrice
                        else usdCBid = 1 / quoteUSDB.bidPrice

                        quote.bidPrice = usdCBid / usdBBid
                        quote.askPrice = usdCAsk / usdBAsk




                        crossQuotes.add(QuoteExtended(quote))
                    }
                }
            }

            lastQuotesCopy.addAll(crossQuotes)
//            println(this.lastQuotes.toString())
            Thread.sleep(sleep)
            while (pauseFlag) {
                Thread.sleep(sleep)
            }

            lastQuotesCopy.forEach { i -> i.markup = this.lastQuotes.find { q -> q.eventSymbol.equals(i.eventSymbol) }!!.markup }

            this.lastQuotes = lastQuotesCopy.toMutableList()
        }


        sub = feed.createSubscription(Quote::class.java).apply {
            addEventListener(listener)
            addSymbols(originalSymbols)
        }
    }


    fun stopListening() {
        sub.removeEventListener(listener)
    }

    fun getQuotesBySymbols(symbols: List<String>): List<QuoteExtended> {
        this.pauseFlag = true
        var list = this.lastQuotes.toList()
        list = list.filter { symbols.contains(it.eventSymbol) }
        this.pauseFlag = false
        return list
    }

    fun setMarkupToSymbols(value: Double, symbols: List<String>) {
        this.pauseFlag = true
        this.lastQuotes.forEach {
            if (symbols.contains(it.eventSymbol)) it.markup = value
        }
        this.pauseFlag = false
    }

}