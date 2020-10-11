@file:JvmName("Start")

package hackathon

import ApiSubscriptor
import QuoteExtended
import QuoteJSON
import RequestModel
import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress


fun main(apiAddress: String = "demo.dxfeed.com:7306", serverPort: Int = 11600) {

    val apiSubscriptor = ApiSubscriptor(apiAddress)
    apiSubscriptor.startUpdating()

    ApiServer(serverPort, apiSubscriptor, Gson()).start()
}

class ApiServer(private val serverPort: Int, private val apiSubscriptor: ApiSubscriptor, private val gson: Gson) {
    fun start() {
        val server: HttpServer = HttpServer.create(InetSocketAddress(serverPort), 0)
        server.createContext("/api", Handler(apiSubscriptor, gson))
        server.setExecutor(null)
        server.start()
    }


    private class Handler(private val apiSubscriptor: ApiSubscriptor, private val gson: Gson) : HttpHandler {
        override fun handle(exchange: HttpExchange?) {
            val response = processMessage(getContent(exchange))
            val bytes = response.toByteArray()
            exchange!!.responseHeaders.set("Access-Control-Allow-Origin", "*")
            exchange!!.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(bytes)
            os.close()
        }

        private fun getContent(exchange: HttpExchange?): String {
            val httpInput = BufferedReader(InputStreamReader(
                    exchange!!.requestBody, "UTF-8"))
            val `in` = StringBuilder()
            var input: String?
            while (httpInput.readLine().also { input = it } != null) {
                `in`.append(input).append(" ")
            }
            httpInput.close()
            return `in`.toString().trim { it <= ' ' }
        }

        private fun processMessage(mes: String): String {
            val request: RequestModel = gson.fromJson(mes, RequestModel::class.java)
            println(request.toString())
            when (request.type) {
                "get" -> {
                    val symbols: List<String> = request.symbols!!.map { "$it:AFX{mm=CFH2}" }
                    var b = apiSubscriptor.getQuotesBySymbols(symbols)

                    val a1 = gson.toJson(b.map { QuoteJSON(it.eventSymbol, it.getFixedMarkedupBid(), it.getFixedMarkedupAsk(), it.getFixedMarkedUpSpread()) })
                    val a2 = gson.toJson(b.map { QuoteJSON(it.eventSymbol, it.getFloatMarkedupBid(), it.getFloatMarkedupAsk(), it.getFloatMarkedUpSpread()) })
                    val a3 = gson.toJson(b.map { QuoteJSON(it.eventSymbol, it.bidPrice, it.askPrice, it.askPrice - it.bidPrice) })

                    val result = ("{\"fixed\":$a1,\"float\":$a2,\"none\":$a3}")

//                    println("Result:$result")

                   return result
                }
                "set" -> {
                    val value = request.value
                    val symbols = request.symbols!!.map { "$it:AFX{mm=CFH2}" }
                    apiSubscriptor.setMarkupToSymbols(value!!, symbols!!)
                    println("$value markup set")
                    return "{'set': 'ok'}"
                }
                else -> {
                    return "{error: 123}"
                }
            }
        }
    }
}