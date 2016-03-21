package marketsobsessed.tweet

import com.typesafe.config.ConfigFactory
import marketsobsessed.cassandra.{DateTickerCompositeKey, TweetSentiment}
import marketsobsessed.sentiment._
import marketsobsessed.utils.ApplicationConstants
import twitter4j._
import scala.collection.JavaConversions._
import scala.collection._

object TweetReceiver {

  val ID = "id"
  SentimentAnalyzer.init
  val MAX_TWEETS = 100
  val sinceIdMaps: mutable.Map[String, Long] = mutable.Map() ++ ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS).map {
    config =>
      (config.getString(ID), 0.toLong)
  }.toMap

  val tickerQueryMap = ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS).map {
    config =>
      (config.getString(ID), config.getString(ApplicationConstants.TWITTER_QUERY))
  } toMap

  //  def getTickers(hashTagsList: Array[HashtagEntity]): Array[String] = {
  //    hashTagsList.dropWhile(tag => !tickerQueryMap.containsKey(tag.getText)).map(_.getText)
  //  }

  def getTweetSentiment: List[TweetSentiment] = {
    tickerQueryMap.keys.flatMap(getTweetSentiment(_)).toList
  }

  def getTweetSentiment(tickerId: String): List[TweetSentiment] = {
    val twitter: Twitter = new TwitterFactory().getInstance
    var tweetList: List[TweetSentiment] = List()
    try {
      val query = new Query(tickerQueryMap(tickerId))
      query.setLang("en")
      query.setSinceId(sinceIdMaps(tickerId))
      query.setCount(MAX_TWEETS)
      val queryResult = twitter.search(query)
      sinceIdMaps(tickerId) = queryResult.getSinceId
      tweetList = queryResult.getTweets.map(getTweetSentiment(_, tickerId)).toList
    }
    catch {
      case te: TwitterException =>
        play.Logger.error("Failed to search tweets: " + te.getMessage)
    }
    tweetList
  }

  private def getTweetSentiment(tweet: Status, tickerId: String): TweetSentiment = {
    val score = (SentimentAnalyzer.findSentiment(tweet.getText) * 25).toInt
    println(tweet.getText, score)
    new TweetSentiment(new DateTickerCompositeKey(tweet.getCreatedAt, tickerId), score)
  }


}

