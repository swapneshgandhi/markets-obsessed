package marketsobsessed.sentiment

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.trees.Tree

object SentimentAnalyzer {
  private[sentiment] var pipeline: StanfordCoreNLP = null

  def init {
    pipeline = new StanfordCoreNLP("StanfordCoreNLP.properties")
  }

  // scalastyle:off
  def findSentiment(tweet: String): Double = {
    if (tweet != null && tweet.length > 0) {
      val annotation: Annotation = pipeline.process(tweet)
      import scala.collection.JavaConversions._
      var sentiments = annotation.get(classOf[CoreAnnotations.SentencesAnnotation]).map {
        sentence =>
          val tree: Tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
          val sentiment: Int = RNNCoreAnnotations.getPredictedClass(tree)
          sentiment
      }
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
      (sentiments.foldLeft(0.0)(_ + _) / sentiments.foldLeft(0)((r, c) => r + 1)).toFloat
    }
    else {
      2.0
    }
  }

  // scalastyle:on
}
