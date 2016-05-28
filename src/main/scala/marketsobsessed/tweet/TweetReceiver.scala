package marketsobsessed.tweet

import java.util.Date

import com.typesafe.config.ConfigFactory
import marketsobsessed.cassandra.{DateTickerCompositeKey, TweetSentiment}
import marketsobsessed.sentiment._
import marketsobsessed.utils.ApplicationConstants
import twitter4j._
import scala.collection.JavaConversions._
import scala.collection.mutable

object TweetReceiver {

  val ID = "id"
  SentimentAnalyzer.init
  val MAX_TWEETS = 100
  val sinceIdMaps: scala.collection.mutable.Map[String, Long] = mutable.Map() ++ ConfigFactory.load().getConfigList(ApplicationConstants.TICKERS).map {
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

  def getTweetSentiment(): Map[String, TweetSentiment] = {
    tickerQueryMap.keys.map{
      tickerId =>
        (tickerId, getTweetSentiment(tickerId))
    }.toMap
  }

  def getTweetSentiment(tickerId: String): TweetSentiment = {
    val twitter: Twitter = new TwitterFactory().getInstance
    var tweetSentiment = new TweetSentiment(new DateTickerCompositeKey(new Date(System.currentTimeMillis()), tickerId), 0, 0)
    try {
      val query = new Query(tickerQueryMap(tickerId))
      query.setLang("en")
      query.setSinceId(sinceIdMaps(tickerId))
      query.setCount(MAX_TWEETS)
      val queryResult = twitter.search(query)
      sinceIdMaps(tickerId) = queryResult.getSinceId
      val sentimentScoreList = queryResult.getTweets.map(getTweetSentiment(_, tickerId))
      tweetSentiment = new TweetSentiment(tweetSentiment.dateTickerCompositeKey,
        sentimentScoreList.foldLeft(0.0)((res, ts) => res + ts).toInt, sentimentScoreList.length)
    }
    catch {
      case te: TwitterException =>
        play.Logger.error("Failed to search tweets: " + te.getMessage)
    }
    tweetSentiment
  }

  private def getTweetSentiment(tweet: Status, tickerId: String): Int = {
    (SentimentAnalyzer.findSentiment(tweet.getText) * 25).toInt
  }


}

