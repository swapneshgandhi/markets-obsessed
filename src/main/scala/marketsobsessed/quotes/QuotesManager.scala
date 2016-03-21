package marketsobsessed.quotes

import java.text.SimpleDateFormat
import java.util.Date
import com.typesafe.config.ConfigFactory
import marketsobsessed.cassandra.CassandraManager
import marketsobsessed.tweet.TweetReceiver
import marketsobsessed.utils.{ApplicationConstants, DateUtils}
import scala.collection.JavaConversions._

/**
  * Created by sgandhi on 2/20/16.
  */

case class Fraction(sum: Double, count: Int) {

  def evaluate: Float = {
    sum / count
  }.toFloat

  def add(otherFraction: Fraction): Fraction = {
    new Fraction(this.sum + otherFraction.sum, this.count + otherFraction.count)
  }

}

object QuotesManager extends App {

  val tickers = ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS)
  //Play.application.configuration.getStringList("tickers")
  var riskOnNumbers = Fraction(0, 0)
  var riskOffNumbers = Fraction(0, 0)

  val finApiMap: collection.mutable.Map[String, FinApi] = collection.mutable.Map() ++ tickers.map {
    tickerInfo =>
      tickerInfo.getString(ApplicationConstants.SOURCE) match {
        case ApplicationConstants.GOOGLE => (tickerInfo.getString(ApplicationConstants.ID),
          new GoogleFinanceApi(tickerInfo.getString(ApplicationConstants.TICKER), tickerInfo.getString(ApplicationConstants.ID)))
        case ApplicationConstants.YAHOO => (tickerInfo.getString(ApplicationConstants.ID),
          new YahooFinanceApi(tickerInfo.getString(ApplicationConstants.TICKER), tickerInfo.getString(ApplicationConstants.HISTORICAL),
            tickerInfo.getString(ApplicationConstants.ID)))
        case ApplicationConstants.YAHOO_FUTURES => (tickerInfo.getString(ApplicationConstants.ID),
          new YahooFinanceFuturesApi(tickerInfo.getString(ApplicationConstants.TICKER), tickerInfo.getString(ApplicationConstants.HISTORICAL),
            tickerInfo.getString(ApplicationConstants.ID)))
        case _ =>
      }
  }.toMap

  val riskOnOffMap = tickers.map {
    tickerInfo =>
      tickerInfo.getString(ApplicationConstants.TYPE) match {
        case ApplicationConstants.RISK_ON => (tickerInfo.getString(ApplicationConstants.ID), ApplicationConstants.RISK_ON)
        case _ => (tickerInfo.getString(ApplicationConstants.ID), ApplicationConstants.RISK_OFF)
      }
  }.toMap

  def isRiskOn(tickerId: String): Boolean = {
    riskOnOffMap(tickerId).equals(ApplicationConstants.RISK_ON)
  }

  def updatedFuturesTicker(partialTicker: String): String = {
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(partialTicker + "_contracts.csv")).getLines.dropWhile {
      line =>
        val contractArray = line.split(",")
        val sdf = new SimpleDateFormat("dd-MMM-yy")
        sdf.parse(contractArray(2)).getTime < DateUtils.getLastWeekDate
    }.toList.head
  }

  def updateFuturesTicker: Unit = {
    finApiMap.filter(item => item._2.isInstanceOf[YahooFinanceFuturesApi]).foreach {
      item =>
        val yahooFinanceFuturesApi = item._2.asInstanceOf[YahooFinanceFuturesApi]
        finApiMap(item._1) = new YahooFinanceFuturesApi(updatedFuturesTicker(yahooFinanceFuturesApi.rTicker),
          yahooFinanceFuturesApi.hTicker, yahooFinanceFuturesApi.tickerId)
    }
  }

  def fetchHistoricQuotes: Map[String, List[HistoricalQuote]] = {
    finApiMap.map {
      case (tickerId, finApi) =>
        val quotes = finApi.getHistoricalQuotes
        val currentTime = DateUtils.getCurrentTime
        val sentimentList = CassandraManager.getHistoricSentiment(tickerId, new Date(currentTime),
          new Date(DateUtils.toLastYearTime(currentTime))).map(_.toFloat)
        (tickerId, quotes.zip(sentimentList).map { case (quote, score) => new HistoricalQuote(quote.id, quote.dateTime,
          quote.openPrice, quote.highOfTheDay, quote.lowOfTheDay, quote.closePrice, score)
        })
    }.toMap
  }

  def getRiskOnOFFNumbers: Seq[Float] = {
    Seq(riskOnNumbers.evaluate, riskOffNumbers.evaluate)
  }

  def fetchLatestQuotes: Seq[RealTimeQuote] = {
    finApiMap.map {
      case (tickerId, finApi) =>
        val quote = finApi.getLatestQuote
        val sentimentList = TweetReceiver.getTweetSentiment(quote.id).filter(tS => tS.dateTickerCompositeKey.tickerId.equals(quote.id))
        val sentimentAvgFraction = new Fraction(sentimentList.foldLeft(0.0)((res, ts) => res + ts.score), sentimentList.foldLeft(0)((res, curr) => res + 1))
        if (isRiskOn(quote.id)) {
          riskOnNumbers = sentimentAvgFraction.add(riskOnNumbers)
        } else {
          riskOffNumbers = sentimentAvgFraction.add(riskOffNumbers)
        }
        val avgScore = sentimentAvgFraction.evaluate
        new RealTimeQuote(quote.id, quote.dateTime, quote.openPrice, quote.currentPrice, avgScore)
    }.toSeq
  }
}
