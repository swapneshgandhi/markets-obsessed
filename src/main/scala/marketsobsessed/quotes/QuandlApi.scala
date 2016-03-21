package marketsobsessed.quotes

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import marketsobsessed.utils.DateUtils
import play.Logger

/**
  * Created by sgandhi on 2/20/16.
  */
class QuandlApi(ticker: String, tickerId: String) extends FinApi {

  val url = s"https://www.quandl.com/api/v3/datasets/$ticker.csv&exclude_column_names=true&start_date="

  override val realTimeQuoteUrl: String = url

  override val historicalQuoteUrl: String = url

  override def getHistoricalQuotes: List[HistoricalQuote] = {

    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.setTimeZone(TimeZone.getDefault)
    val startDate = sdf.format(DateUtils.toLastYearTime(DateUtils.getCurrentTime))

    val historicalQuotes = getResponse(historicalQuoteUrl + startDate).split("\n").map {
      line =>
        val row = line.split(",")
        new HistoricalQuote(tickerId, sdf.parse(row(DATE_COLUMN)), row(OPEN_PRICE_COLUMN).toFloat, row(HIGH_PRICE_COLUMN).toFloat,
          row(LOW_PRICE_COLUMN).toFloat, row(CLOSE_PRICE_COLUMN).toFloat, DEFAULT_SENTIMENT)
    }

    val lastHistoricQuote = historicalQuotes.last

    if (DateUtils.isSameDay(lastHistoricQuote.dateTime, new Date())) {
      historicalQuotes.dropRight(1).toList :+ updateHistoricalQuoteFromLatest(lastHistoricQuote, getLatestQuote)
    }
    else {
      historicalQuotes.toList :+ updateHistoricalQuoteFromLatest(lastHistoricQuote, getLatestQuote)
    }

  }

  override def getLatestQuote: RealTimeQuote = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.setTimeZone(TimeZone.getDefault)
    val startDate = sdf.format(DateUtils.getCurrentTime)
    try {
      val line = getResponse(realTimeQuoteUrl + startDate)
      val row = line.split(",")
      new RealTimeQuote(tickerId, sdf.parse(row(DATE_COLUMN)), row(OPEN_PRICE_COLUMN).toFloat,
        row(CLOSE_PRICE_COLUMN).toFloat, DEFAULT_SENTIMENT)
    }
    catch {
      case e: Exception => Logger.error("cannot retrive quote")
        new RealTimeQuote(ticker, new Date(), 0, 0, 0)
    }
  }

  override var highOfTheDay: Float = _

  override def updateHighLows(realTimeQuote: RealTimeQuote): Unit = ???

  override var lowOfTheDay: Float = _
}
