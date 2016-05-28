package marketsobsessed.quotes

import java.util.Date
import org.apache.http.HttpResponse
import org.apache.http.client.{ClientProtocolException, ResponseHandler}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

trait FinApi {

  val realTimeQuoteUrl: String

  val historicalQuoteUrl: String

  def getLatestQuote: RealTimeQuote

  def getHistoricalQuotes: List[HistoricalQuote]

  var highOfTheDay: Float = 0

  var lowOfTheDay: Float = 99999.99.toFloat

  val DEFAULT_SENTIMENT = 0
  val DATE_COLUMN = 0
  val OPEN_PRICE_COLUMN = 1
  val HIGH_PRICE_COLUMN = 2
  val LOW_PRICE_COLUMN = 3
  val CLOSE_PRICE_COLUMN = 4

  def updateHighLows(latestQuote: RealTimeQuote): Unit = {
    highOfTheDay = latestQuote.currentPrice.max(latestQuote.openPrice).max(highOfTheDay)
    lowOfTheDay = latestQuote.currentPrice.min(latestQuote.openPrice).min(lowOfTheDay)
  }

  def updateHistoricalQuoteFromLatest(historicalQuote: HistoricalQuote, realTimeQuote: RealTimeQuote): HistoricalQuote = {
    new HistoricalQuote(realTimeQuote.id, realTimeQuote.dateTime, realTimeQuote.openPrice,
      highOfTheDay, lowOfTheDay, realTimeQuote.currentPrice, realTimeQuote.sentimentScore)
  }

  def getResponse(url: String): String = {

    val httpclient = HttpClients.createDefault()
    try {
      val httpGet = new HttpGet(url)
      val responseHandler = new ResponseHandler[String]() {
        override def handleResponse(response: HttpResponse): String = {
          val status = response.getStatusLine.getStatusCode
          if (status >= 200 && status < 300) {
            val entity = response.getEntity
            if (entity != null) EntityUtils.toString(entity) else null
          } else {
            throw new ClientProtocolException("Unexpected response: " + EntityUtils.toString(response.getEntity))
          }
        }
      }
      httpclient.execute(httpGet, responseHandler)
    }
    finally {
      httpclient.close()
    }
  }
}

case class HistoricalQuote(id: String, dateTime: Date, openPrice: Float, highOfTheDay: Float, lowOfTheDay: Float, closePrice: Float, sentimentScore: Float)

case class RealTimeQuote(id: String, dateTime: Date, openPrice: Float, currentPrice: Float, var sentimentScore: Float)
