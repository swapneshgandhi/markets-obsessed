import java.util.Date
import com.jimmoores.quandl.{Frequency, DataSetRequest, QuandlSession}
import com.typesafe.config.ConfigFactory
import org.threeten.bp.{ZoneId, Instant}
import scala.collection.JavaConversions._


val x = "sd"

//val tickers = ConfigFactory.load().getConfigList("tickers") //Play.application.configuration.getStringList("tickers")
val GOOGLE = "google"
val session = QuandlSession.create("7BS7k1_dnKCP3h2FefxA")
def getTime = new Date().getTime
val tabularResult = session.getDataSet(
  DataSetRequest.Builder
    .of("WIKI/AAPL")
    .withFrequency(Frequency.DAILY)
    .withStartDate(Instant.ofEpochMilli(getTime).atZone(ZoneId.systemDefault()).toLocalDate)
    .withEndDate(Instant.ofEpochMilli(getTime).atZone(ZoneId.systemDefault()).toLocalDate.minusYears(1))
    .build())
System.out.println("Header definition: " + tabularResult.getHeaderDefinition())
tabularResult.iterator().foreach {
  println(_)
}
