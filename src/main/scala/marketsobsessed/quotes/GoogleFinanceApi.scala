package marketsobsessed.quotes

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import com.fasterxml.jackson.databind.ObjectMapper
import marketsobsessed.utils.DateUtils

/**
  * Created by sgandhi on 2/20/16.
  *
  * @param ticker   is used for fetching the quotes
  * @param tickerId is used as the symbol/id to identify Quotes everywhere in the application - in C* keyspace and in UI.
  */
class GoogleFinanceApi(ticker: String, tickerId: String) extends FinApi {

  override val realTimeQuoteUrl = s"http://finance.google.com/finance/info?client=ig&q=$ticker"

  override val historicalQuoteUrl: String = s"http://www.google.com/finance/historical?q=$ticker&output=csv"

  val mapper = new ObjectMapper()

  val CDT = "America/Chicago"

  override def getLatestQuote: RealTimeQuote = {

    val jsonResult = getResponse(realTimeQuoteUrl)
    //val firstPart = jsonResult.split("[")(1).split("]")(0)
    val rootNode = mapper.readTree(jsonResult.split('[')(1).split(']')(0))
    //val timeZone = rootNode.get("ltt").asText.split(" ")(1)

    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    sdf.setTimeZone(TimeZone.getTimeZone(CDT))
    val openPrice = rootNode.get("l_fix").asDouble - rootNode.get("c").asDouble

    val latestQuote = new RealTimeQuote(tickerId, sdf.parse(rootNode.get("lt_dts").asText),
      openPrice.toFloat, rootNode.get("l_fix").asDouble.toFloat, 0)

    updateHighLows(latestQuote)

    latestQuote
  }

  override def getHistoricalQuotes: List[HistoricalQuote] = {

    val sdf = new SimpleDateFormat("dd-MMM-yy")

    val historicalQuotes = getResponse(historicalQuoteUrl).split("\n").drop(0).map(_.split("\n")).map {
      row =>
        new HistoricalQuote(tickerId, sdf.parse(row(DATE_COLUMN)), row(OPEN_PRICE_COLUMN).toFloat,
          row(HIGH_PRICE_COLUMN).toFloat, row(LOW_PRICE_COLUMN).toFloat, row(CLOSE_PRICE_COLUMN).toFloat, DEFAULT_SENTIMENT)
    }

    val lastHistoricQuote = historicalQuotes.last

    if (DateUtils.isSameDay(lastHistoricQuote.dateTime, new Date())) {
      historicalQuotes.dropRight(1).toList :+ updateHistoricalQuoteFromLatest(lastHistoricQuote, getLatestQuote)
    }
    else {
      historicalQuotes.toList :+ updateHistoricalQuoteFromLatest(lastHistoricQuote, getLatestQuote)
    }

  }

}
