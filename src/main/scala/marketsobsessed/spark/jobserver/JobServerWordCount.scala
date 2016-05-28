package marketsobsessed.spark.jobserver

import com.typesafe.config.{Config, ConfigFactory}
import marketsobsessed.sentiment.SentimentAnalyzer
import org.apache.spark.SparkContext
import spark.jobserver.{SparkJobInvalid, SparkJobValid, SparkJobValidation, SparkJob}
import scala.util.Try

/**
 * A super-simple Spark job example that implements the SparkJob trait and can be submitted to the job server.
 *
 * Set the config with the sentence to split or count:
 * input.string = "adsfasdf asdkf  safksf a sdfa"
 *
 * validate() returns SparkJobInvalid if there is no input.string
 */
object WordCountExample extends SparkJob {
  def main(args: Array[String]) {
    val sc = new SparkContext("local[4]", "WordCountExample")
    val config = ConfigFactory.parseString("")
    val results = runJob(sc, config)
    println("Result is " + results)
  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = {
    Try(config.getString("input.string"))
      .map(x => SparkJobValid)
      .getOrElse(SparkJobInvalid("No input.string config param"))
  }

  override def runJob(sc: SparkContext, config: Config): Any = {
    val dd = config.getString("input.string")
    //dd.map((_, 1)).reduceByKey(_ + _).collect().toMap
    Map(dd -> SentimentAnalyzer.findSentiment(dd))
  }
}