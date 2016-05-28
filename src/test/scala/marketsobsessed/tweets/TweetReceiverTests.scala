package marketsobsessed.tweets

import com.typesafe.config.ConfigFactory
import marketsobsessed.quotes.Fraction
import marketsobsessed.tweet.TweetReceiver
import marketsobsessed.utils.ApplicationConstants
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import scala.collection.JavaConversions._

/**
  * Created by sgandhi on 3/20/16.
  */
class TweetReceiverTests extends FeatureSpec with GivenWhenThen with Matchers {

  val tickers = ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS).map {
    tickerInfo =>
      tickerInfo.getString(ApplicationConstants.ID)
  }.toSet
  val totalTickers = tickers.size


  feature("Tweets Receiver should return tweets for all 8 tickers") {
    scenario("TweetReceiver.getTweetSentiment is called.") {
      val tweetSentiments = TweetReceiver.getTweetSentiment()
      Then("It should query twitter for latest tweets, find their sentiments")
      tweetSentiments.size should equal(totalTickers)

      tweetSentiments.values.foreach {
        tweetSentiment =>
          And("tweetSentiment.dateTickerCompositeKey should not be null")
          tweetSentiment.dateTickerCompositeKey should not be null
          val tickerId = tweetSentiment.dateTickerCompositeKey.tickerId
          And(s"$tickerId should not be empty")
          tickerId.length should be > 0
          And(s"$tickerId should be in tickers list in application.conf")
          tickers should contain(tweetSentiment.dateTickerCompositeKey.tickerId)
          val timeStamp = tweetSentiment.dateTickerCompositeKey.timestamp
          And(s"$timeStamp should not be null")
          timeStamp should not be null

          val scoreFraction = new Fraction(tweetSentiment.sumScore, tweetSentiment.count)
          And(s"${scoreFraction.evaluate} should be between 0 and 100")
          scoreFraction.evaluate.toDouble should (be >= 0.0 and be <= 100.0)
      }
    }
  }
}

