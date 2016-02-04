package actors

import akka.actor.{Props, ActorRef, Actor}
import play.api.libs.json._
import play.{Logger, Play}
import utils.{StockQuote, FakeStockQuote}
import java.util.Random
import scala.collection.immutable.{HashSet, Queue}
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.libs.Akka

/**
 * Created by sgandhi on 1/31/16.
 */
class QuotesActor extends Actor {

  lazy val stockQuote: StockQuote = new FakeStockQuote

  val quotes = Play.application.configuration.getStringList("tickers").map {
    ticker =>
      getInitialQuote(ticker)
  }.toSeq

  def getInitialQuote(ticker: String): Quote = {
    new Quote(ticker, 100, 100, 0)
  }

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  // Fetch the latest stock value every 75ms
  val nextTick = context.system.scheduler.schedule(Duration.Zero, 1200.millis, self, FetchLatestQuote)

  val sendLatestQuote = context.system.scheduler.schedule(Duration.Zero, 1200.millis, self, SendLatestQuote)

  def receive = {

    case FetchLatestQuote =>
      //Logger.info("creating new Quote")
      quotes.foreach { quote =>
        quote.currentPrice = stockQuote.newPrice(quote.currentPrice)
      }
    case SendLatestQuote =>
      //Logger.info("sending new Quote")
      // notify watchers
      watchers.foreach(_ ! quotes)
    case IAmHere =>
      //Logger.info("in IamHere message received")
      // send the stock history to the user
      sender ! quotes
      // add the watcher to the list
      watchers = watchers + sender
    case IAmAway =>
      watchers = watchers - sender
  }
}

object QuotesActor {
  lazy val quotesActor: ActorRef = Akka.system.actorOf(Props(classOf[QuotesActor]))
}

case object SendLatestQuote

case object FetchLatestQuote

case class Quote(symbol: String, var openPrice: Float, var currentPrice: Float, var sentimentScore: Float)

object Quote {
  implicit val implicitQuoteWrites = new Writes[Quote] {
    def writes(quote: Quote): JsValue = {
      Json.obj(
        "symbol" -> quote.symbol,
        "openPrice" -> quote.openPrice,
        "currentPrice" -> quote.currentPrice,
        "sentimentScore" -> quote.sentimentScore)
    }
  }
}


case class IAmHere()

case class IAmAway()