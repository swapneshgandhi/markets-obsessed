package marketsobsessed.sentiment

import scala.collection.mutable

/**
 * Created by sgandhi on 5/27/16.
 */
object SentimentHelper {

  // scalastyle:off
  def updateSentiment(tweet: String): mutable.Buffer[Int] = {

    val sentiments = mutable.Buffer[Int]()

    if ((tweet.contains("long") || tweet.contains("buy") || tweet.contains("bought")) && tweet.contains("put")) {
      sentiments += 0
    }
    else if ((tweet.contains("short") || tweet.contains("sell") || tweet.contains("sold")) && tweet.contains("call")) {
      sentiments += 0
    }
    else if (tweet.contains("long") || tweet.contains("buy") || tweet.contains("bought") || tweet.contains("call")) {
      sentiments += 4
    }
    else if (tweet.contains("short") || tweet.contains("sell") || tweet.contains("sold") || tweet.contains("put")) {
      sentiments += 0
    }

    if (tweet.contains("+")) {
      sentiments += 4
    }
    else if (tweet.contains("-")) {
      sentiments += 0
    }
    if (tweet.contains("up") || tweet.contains("high")) {
      sentiments += 4
    }
    else if (tweet.contains("down") || tweet.contains("low")) {
      sentiments += 0
    }
    sentiments
  }
  // scalastyle:on
}

