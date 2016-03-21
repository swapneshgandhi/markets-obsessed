package marketsobsessed.quotes

import com.typesafe.config.ConfigFactory
import marketsobsessed.utils.ApplicationConstants
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import scala.collection.JavaConversions._

/**
  * Created by sgandhi on 3/20/16.
  */
class FinApisTests extends FeatureSpec with GivenWhenThen with Matchers {
  val tickers = ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS).map {
    tickerInfo =>
      tickerInfo.getString(ApplicationConstants.ID)
  }.toSet
  val totalTickers = tickers.size

  ignore("QuotesManager should return realtime quotes and historical quotes for all 8 tickers") {

    When("QuotesManager.fetchLatestQuotes is called.")
    val realTimeQuotes = QuotesManager.fetchLatestQuotes
    Then("It should query Finance APIs for latest Quotes, find their sentiments")
    realTimeQuotes.length should equal(totalTickers)

    realTimeQuotes.foreach {
      realTimeQuote =>
        And("realTimeQuote.id should match tickerIds in application.conf")
        tickers should contain(realTimeQuote.id)
        And("realTimeQuote.currentPrice should be non zero positive number.")
        realTimeQuote.currentPrice.toDouble should be > 0.0
        And("realTimeQuote.openPrice should be non zero positive number.")
        realTimeQuote.openPrice.toDouble should be > 0.0
        And("realTimeQuote.dateTime should not be null")
        realTimeQuote.dateTime should not be null
        And("realTimeQuote.score should be between 0 and 100")
        realTimeQuote.sentimentScore.toDouble should (be >= 0.0 and be <= 100.0)
    }
  }


  ignore("historic tests ignored.") {
    When("QuotesManager.fetchHistoricQuotes is called.")
    val historicalQuotesMap = QuotesManager.fetchHistoricQuotes
    Then("It should query Finance APIs for Historical Quotes, find their sentiments")
    historicalQuotesMap.size should equal(totalTickers)

    historicalQuotesMap.foreach {
      case (id, historicalQuotesList) =>
        And("historicalQuotes.id should match tickerIds in application.conf")
        val oneIdFromList = historicalQuotesList.head.id
        tickers should contain(oneIdFromList)
        And("historicalQuote.id should match with ids of other historical quotes in the same list.")
        historicalQuotesList.forall(historicalQuote => historicalQuote.id.equals(oneIdFromList))
        And("historicalQuote.closePrice should be non zero positive number.")
        historicalQuotesList.forall(_.closePrice > 0.0) should be(true)
        And("historicalQuote.highOfTheDay should be non zero positive number.")
        historicalQuotesList.forall(_.highOfTheDay > 0.0) should be(true)
        And("historicalQuote.lowOfTheDay should be non zero positive number.")
        historicalQuotesList.forall(_.lowOfTheDay > 0.0) should be(true)
        And("historicalQuote.openPrice should be non zero positive number.")
        historicalQuotesList.forall(_.openPrice > 0.0) should be(true)
        And("historicalQuote.dateTime should not be null")
        historicalQuotesList.forall(_.dateTime != null) should be(true)
        And("realTimeQuote.sentimentScore should be between 0 and 100")
        historicalQuotesList.forall(historicalQuote => historicalQuote.sentimentScore >= 0.0
          && historicalQuote.sentimentScore <= 100.0) should be(true)
    }
  }
}

