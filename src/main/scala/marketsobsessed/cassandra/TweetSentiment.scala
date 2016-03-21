package marketsobsessed.cassandra

import java.util
import javax.persistence._
import marketsobsessed.utils.ApplicationConstants
import play.Logger

@Entity
@Table(name = ApplicationConstants.TWEETS_TABLE_NAME, schema = ApplicationConstants.KUNDERA_SCHEMA)
case class TweetSentiment(@EmbeddedId dateTickerCompositeKey: DateTickerCompositeKey,
                          @Column score: Int) {

}

@Entity
@Table(name = ApplicationConstants.MIN5_AGGREGATE_TABLE_NAME, schema = ApplicationConstants.KUNDERA_SCHEMA)
case class AggregatedMin5TweetSentiment(@EmbeddedId dateTickerCompositeKey: DateTickerCompositeKey,
                                        @Column score: Int) {

}

@Entity
@Table(name = ApplicationConstants.MIN5_AGGREGATE_TABLE_NAME, schema = ApplicationConstants.KUNDERA_SCHEMA)
case class AggregatedDayTweetSentiment(@EmbeddedId dateTickerCompositeKey: DateTickerCompositeKey,
                                       @Column score: Int) {

}

@Embeddable
class DateTickerCompositeKey(@Column
                             @Temporal(TemporalType.TIMESTAMP) val timestamp: util.Date,
                             @Column val tickerId: String) {
}

object CassandraManager {

  val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("MO_PU")

  def insert(tweetSentimentList: List[TweetSentiment]): Unit = {
    tweetSentimentList.foreach(insert)
  }

  def insert(tweetSentiment: TweetSentiment): Unit = {
    try {
      val entityManager = emf.createEntityManager()
      entityManager.persist(tweetSentiment)
      entityManager.close()
    }
    catch {
      case e: Exception =>
        Logger.error("An error occurred during cassandra insert operation" + e.getMessage)
    }
  }

  def getHistoricSentiment(ticket: String, startDate: util.Date, endDate: util.Date): util.List[Int] = {
    val entityManager = emf.createEntityManager()
    try {
      entityManager.createQuery(s"select score from aggregated_day where ticker = $ticket" +
        s" and date between $startDate and $endDate").getResultList.asInstanceOf[util.List[Int]]
    }
    catch {
      case e: Exception =>
        Logger.error("An error occurred during cassandra insert operation" + e.getMessage)
        new util.ArrayList[Int]()
    }
    finally {
      entityManager.close()
    }
  }
}


