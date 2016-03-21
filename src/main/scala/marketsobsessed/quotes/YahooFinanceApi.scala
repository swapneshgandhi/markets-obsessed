package marketsobsessed.quotes

import java.text.SimpleDateFormat
import java.util.TimeZone

/**
  * Created by sgandhi on 2/20/16.
  */
class YahooFinanceApi(rTicker: String, hTicker: String, tickerId: String) extends QuandlApi(rTicker, tickerId) {

  override val realTimeQuoteUrl = s"http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1c1p2&s=$rTicker"

  override def getLatestQuote: RealTimeQuote = {

    val response = getResponse(realTimeQuoteUrl).split(",").map(_.trim)

    val DATE_COLUMN = 2
    val TIME_COLUMN = 3
    val PRICE_CHANGE_COLUMN = 4
    val CLOSE_PRICE_COLUMN = 1

    val sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm")
    val tz = TimeZone.getDefault
    sdf.setTimeZone(tz)
    val date = sdf.parse(response(DATE_COLUMN) + " " + response(TIME_COLUMN))

    val latestQuote = new RealTimeQuote(tickerId, date, response(CLOSE_PRICE_COLUMN).toFloat - response(PRICE_CHANGE_COLUMN).toFloat,
      response(CLOSE_PRICE_COLUMN).toFloat, DEFAULT_SENTIMENT)

    updateHighLows(latestQuote)

    latestQuote
  }

}
