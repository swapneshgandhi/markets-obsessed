package marketsobsessed.spark.jobserver

import com.datastax.spark.connector.toSparkContextFunctions
import com.typesafe.config.{Config, ConfigFactory}
import marketsobsessed.utils.ApplicationConstants
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.{SparkConf, SparkContext}
import spark.jobserver.{SparkJob, SparkJobInvalid, SparkJobValid, SparkJobValidation}

import scala.util.Try

/**
  * Created by sgandhi on 2/14/16.
  */
object SentimentAggregatorDailyJob extends SparkJob {

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
    sc.cassandraTable("keyspace name", "table name")
    //val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    val csc = new CassandraSQLContext(sc)
    val minute5DataFrame: DataFrame = csc.sql("SELECT * from keyspace.table WHERE ...")

    minute5DataFrame.select("ticker", "timestamp", "x" * 3)

    val partialDailyDataFrame = csc.sql("SELECT * from keyspace.table WHERE ...")

    minute5DataFrame.unionAll(partialDailyDataFrame).groupBy("")


    //    partialDailyDataFrame.createCassandraTable(
    //      "testKey",
    //      "testTableName",
    //      partitionKeyColumns = Some(Seq("user")),
    //      clusteringKeyColumns = Some(Seq("newcolumnname"))
    //    )

    //    tweetsData.write.format("org.apache.spark.sql.cassandra")
    //      .options(Map("table" -> "words", "keyspace" -> "test"))
    //      .save()

  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = {
    Try(config.getString("input.string"))
      .map(x => SparkJobValid)
      .getOrElse(SparkJobInvalid("No input.string config param"))
  }

}
