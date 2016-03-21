package marketsobsessed.tweets

import marketsobsessed.tweet.TweetReceiver
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

/**
  * Created by sgandhi on 3/20/16.
  */
class TweetReceiverTests extends FeatureSpec with GivenWhenThen with Matchers {

  feature("Tweets Receiver should return tweets for all 8 tickers") {

    When("TweetReceiver.getTweetSentiment is called.")
    val tweetSentiments = TweetReceiver.getTweetSentiment
    Then("It should query twitter for latest tweets, find their sentiments")
    tweetSentiments.length should be > 0

    tweetSentiments.foreach {
      tweetSentiment =>
        And("tweetSentiment.dateTickerCompositeKey should not be null")
        tweetSentiment.dateTickerCompositeKey should not be null
        And("tweetSentiment.dateTickerCompositeKey.tickerId should not be empty")
        tweetSentiment.dateTickerCompositeKey.tickerId.length should be > 0
        And("tweetSentiment.dateTickerCompositeKey.timestamp should not be null")
        tweetSentiment.dateTickerCompositeKey.timestamp should not be null
        And("tweetSentiment.score should be between 0 and 100")
        tweetSentiment.score should (be >= 0 and be <= 100)
    }

  }

}

