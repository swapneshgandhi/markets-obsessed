package actors

import akka.actor.{Actor, ActorRef, Props}
import marketsobsessed.quotes.{QuotesManager, RealTimeQuote}
import play.libs.Akka
import utils.{FakeStockQuote, StockQuote}
import scala.collection.immutable.HashSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by sgandhi on 1/31/16.
  */
//if google api doesn't work http://www.quantatrisk.com/2014/01/14/hacking-google-finance-in-real-time-for-algorithmic-traders/
class QuotesActor extends Actor {

  lazy val stockQuote: StockQuote = new FakeStockQuote

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val nextTick = context.system.scheduler.schedule(Duration.Zero, 1200.millis, self, FetchLatestQuote)

  val sendLatestQuote = context.system.scheduler.schedule(Duration.Zero, 1200.millis, self, SendLatestQuote)

  var quotes: Seq[RealTimeQuote] = QuotesManager.fetchLatestQuotes

  def receive = {
    case FetchLatestQuote =>
      //Logger.info("creating new Quote")
      quotes = QuotesManager.fetchLatestQuotes
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

case class Quote(symbol: String, var openPrice: Float, var currentPrice: Float, var sentimentScore: Float)

case object SendLatestQuote

case object FetchLatestQuote

case class IAmHere()

case class IAmAway()