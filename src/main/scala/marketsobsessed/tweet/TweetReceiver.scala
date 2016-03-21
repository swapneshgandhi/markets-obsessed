package marketsobsessed.tweet

import marketsobsessed.cassandra.{DateTickerCompositeKey, TweetSentiment}
import marketsobsessed.sentiment._
import play.Play
import twitter4j._
import scala.collection.JavaConversions._
import scala.collection._

object TweetReceiver {

  val ID = "id"
  val MAX_TWEETS = 100
  val sinceIdMaps: mutable.Map[String, Long] = Play.application.configuration.getConfigList("tickers").map {
    config =>
      (config.getString(ID), 0.toLong)
  }(collection.breakOut)

  val tickerQueryMap = Play.application.configuration.getConfigList("tickers").
    map { config =>
      (config.getString(ID), config.getString("twitterQuery"))
    } toMap

  def getTickers(hashTagsList: Array[HashtagEntity]): Array[String] = {
    hashTagsList.dropWhile(tag => !tickerQueryMap.containsKey(tag.getText)).map(_.getText)
  }

  def getTweetSentiment: List[TweetSentiment] = {
    tickerQueryMap.keys.flatMap(getTweetSentiment(_)).toList
  }

  def getTweetSentiment(id: String): List[TweetSentiment] = {
    val twitter: Twitter = new TwitterFactory().getInstance
    var tweetList: List[TweetSentiment] = List()
    try {
      val query = new Query(tickerQueryMap(id))
      query.setSinceId(sinceIdMaps(id))
      query.setCount(MAX_TWEETS)
      val queryResult = twitter.search(query)
      sinceIdMaps(id) = queryResult.getSinceId
      tweetList = queryResult.getTweets.flatMap(getTweetSentiment(_)).toList
    }
    catch {
      case te: TwitterException =>
        play.Logger.error("Failed to search tweets: " + te.getMessage)
    }
    tweetList
  }

  private def getTweetSentiment(tweet: Status): List[TweetSentiment] = {
    val score = (SentimentAnalyzer.findSentiment(tweet.getText) * 25).toInt
    getTickers(tweet.getHashtagEntities).map {
      ticker =>
        new TweetSentiment(new DateTickerCompositeKey(tweet.getCreatedAt, ticker), score)
    }.toList
  }

}

