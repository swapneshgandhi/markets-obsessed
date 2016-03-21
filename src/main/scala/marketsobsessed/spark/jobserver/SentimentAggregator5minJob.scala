package marketsobsessed.spark.jobserver

import com.datastax.spark.connector.toSparkContextFunctions
import com.typesafe.config.{Config, ConfigFactory}
import marketsobsessed.utils.{ApplicationConstants, DateUtils}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.functions._
import org.apache.spark.{SparkConf, SparkContext}
import spark.jobserver.{SparkJob, SparkJobInvalid, SparkJobValid, SparkJobValidation}
import scala.util.Try

/**
  * Created by sgandhi on 2/14/16.
  */
object SentimentAggregator5minJob extends SparkJob {

  val diffBetweenStartAndEndDate = 5

  def main(args: Array[String]) {

    val appConf = ConfigFactory.load()
    val conf = new SparkConf().setAppName(this.getClass.getSimpleName)
      .set("spark.cassandra.connection.host", appConf.getString("cassandra.host"))
      .set("spark.cassandra.connection.port", appConf.getString("cassandra.port"))
    val sc = new SparkContext(conf)

    val configString = if (args.length == 2) s"${ApplicationConstants.START_TIME} = ${args(0)}\n${ApplicationConstants.END_TIME} = ${args(1)}" else ""
    runJob(sc, ConfigFactory.parseString(configString))
  }

  override def runJob(sc: SparkContext, jobConfig: Config): Any = {
    sc.cassandraTable(ApplicationConstants.KEYSPACE_NAME, ApplicationConstants.TWEETS_TABLE_NAME)

    //val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    val csc = new CassandraSQLContext(sc)

    val endTime = if (jobConfig.hasPath(ApplicationConstants.END_TIME)) {
      jobConfig.getString(ApplicationConstants.END_TIME)
    }
    else {
      DateUtils.to5MinIntervalTimestamp(DateUtils.getCurrentTime).toString
    }

    val startTime = if (jobConfig.hasPath(ApplicationConstants.START_TIME)) {
      jobConfig.getString(ApplicationConstants.START_TIME)
    }
    else {
      DateUtils.to5MinIntervalTimestamp(DateUtils.getCurrentTime - diffBetweenStartAndEndDate * 60 * 1000).toString
    }

    val tweetsData: DataFrame = csc.sql(s"SELECT * from ${ApplicationConstants.KEYSPACE_NAME}.${ApplicationConstants.TWEETS_TABLE_NAME}" +
      s" WHERE ${ApplicationConstants.TIMESTAMP} <= $endTime AND ${ApplicationConstants.TIMESTAMP} >= $startTime")

    val to5MinTimestampSqlFunc = udf(DateUtils.to5MinIntervalTimestamp)
    val aggregatedDF = tweetsData.groupBy("tickerId")
      .agg(expr("sum(score) as scoreSum"), expr("count(score) as scoreCount"))
      .withColumn("timestamp5Min", to5MinTimestampSqlFunc(col("timestamp")))
      .drop("timestamp")
    //sum(tweetsData("score")))
    //, count(tweetsData("score")))

    //tweetsData.createCassandraTable()

    aggregatedDF.write.format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> ApplicationConstants.MIN5_AGGREGATE_TABLE_NAME,
        "keyspace" -> ApplicationConstants.KEYSPACE_NAME))
      .save()

  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = {
    Try(config.getString("input.string"))
      .map(x => SparkJobValid)
      .getOrElse(SparkJobInvalid("No input.string config param"))
  }

}
