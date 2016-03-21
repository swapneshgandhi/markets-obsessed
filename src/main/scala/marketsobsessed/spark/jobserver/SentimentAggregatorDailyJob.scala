package marketsobsessed.spark.jobserver

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
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
object SentimentAggregatorDailyJob extends SparkJob {

  val sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm")
  val tz = TimeZone.getDefault
  sdf.setTimeZone(tz)

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
    val csc = new CassandraSQLContext(sc)

    val endTime = if (jobConfig.hasPath(ApplicationConstants.END_TIME)) {
      sdf.parse(jobConfig.getString(ApplicationConstants.END_TIME))
    }
    else {
      new Date(DateUtils.toTopOfTheHourTimestamp(DateUtils.getCurrentTime))
    }

    val startTime = if (jobConfig.hasPath(ApplicationConstants.START_TIME)) {
      sdf.parse(jobConfig.getString(ApplicationConstants.START_TIME))
    }
    else {
      new Date(DateUtils.toTopOfTheHourTimestamp(DateUtils.toLastHourTime(DateUtils.getCurrentTime)))
    }

    val minute5DataFrame: DataFrame = csc.sql(s"SELECT * from ${ApplicationConstants.KEYSPACE_NAME}.${ApplicationConstants.MIN5_AGGREGATE_TABLE_NAME}" +
      s" WHERE ${ApplicationConstants.TIMESTAMP} <= $endTime AND ${ApplicationConstants.TIMESTAMP} > $startTime")

    //minute5DataFrame.select("ticker", "timestamp", "x" * 3)
    var partialDailyDataFrame: DataFrame = csc.emptyDataFrame
    if (DateUtils.isSameDay(startTime, endTime)) {
      partialDailyDataFrame = csc.sql(s"SELECT * from ${ApplicationConstants.KEYSPACE_NAME}.${ApplicationConstants.DAILY_AGGREGATE_TABLE_NAME}" +
        s" WHERE ${ApplicationConstants.TIMESTAMP} == $startTime")
    }

    val topOfTheHourSqlFunc = udf(DateUtils.toTopOfTheHourTimestamp)

    val aggregatedDF = minute5DataFrame.withColumn("timestampHour", topOfTheHourSqlFunc(col("timestamp5min"))).drop("timestamp5min")
      .unionAll(partialDailyDataFrame).groupBy("tickerId")
      .agg(expr("sum(score_sum) as score_sum"), expr("count(score_count) as score_count"))

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
