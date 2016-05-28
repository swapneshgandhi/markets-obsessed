package marketsobsessed.quotes

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import marketsobsessed.cassandra.CassandraManager
import marketsobsessed.tweet.TweetReceiver
import marketsobsessed.utils.{ApplicationConstants, DateUtils}
import play.api.{Play, Logger}
import play.api.Play.current

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

object QuotesManager {

  val tickers = ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS)
  //Play.application.configuration.getStringList("tickers")
  var riskOnNumbers = Fraction(0, 0)
  var riskOffNumbers = Fraction(0, 0)

  val finApiMap: collection.mutable.Map[String, FinApi] = collection.mutable.Map[String, FinApi]() ++ tickers.map {
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

  private def updateFuturesTicker(partialTicker: String): String = {
    Logger.info(partialTicker)
    scala.io.Source.fromInputStream(Option(ClassLoader.getSystemResourceAsStream(partialTicker + "_contracts.csv")).getOrElse(
      Play.classloader.getResourceAsStream(partialTicker + "_contracts.csv"))).getLines.dropWhile {
      line =>
        val contractArray = line.split(",")
        val sdf = new SimpleDateFormat("dd-MMM-yy")
        sdf.parse(contractArray(2)).getTime < DateUtils.getNextWeekDate
    }.toList.head.split(",")(1)
  }

  def updateFuturesTicker: Unit = {
    finApiMap.filter(item => item._2.isInstanceOf[YahooFinanceFuturesApi]).foreach {
      item =>
        val yahooFinanceFuturesApi = item._2.asInstanceOf[YahooFinanceFuturesApi]
        finApiMap(item._1) = new YahooFinanceFuturesApi(updateFuturesTicker(yahooFinanceFuturesApi.rTicker),
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
    val tweetSentimentsMap = TweetReceiver.getTweetSentiment()
    //CassandraManager.insert(tweetSentimentsMap)
    finApiMap.map {
      case (tickerId, finApi) =>
        val quote = finApi.getLatestQuote

        val sentimentAvgFraction = new Fraction(tweetSentimentsMap(tickerId).sumScore, tweetSentimentsMap(tickerId).count)
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
