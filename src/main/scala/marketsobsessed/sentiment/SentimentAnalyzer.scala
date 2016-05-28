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

      sentiments ++= SentimentHelper.updateSentiment(tweet)

      (sentiments.foldLeft(0.0)(_ + _) / sentiments.foldLeft(0)((r, c) => r + 1)).toFloat
    }
    else {
      2.0
    }
  }

}
