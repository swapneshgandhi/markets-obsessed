package actors

import java.text.SimpleDateFormat
import akka.actor.UntypedActor
import com.fasterxml.jackson.databind.JsonNode
import marketsobsessed.quotes.{HistoricalQuote, RealTimeQuote}
import play.Logger
import play.libs.Json
import play.mvc.WebSocket

/**
  * Created by sgandhi on 1/31/16.
  */

/**
  * The broker between the WebSocket and the StockActor(s).  The UserActor holds the connection and sends serialized
  * JSON data to the client.
  */
class UserActor extends UntypedActor {
  private final var out: WebSocket.Out[JsonNode] = null

  def this(out: WebSocket.Out[JsonNode]) {
    this()
    this.out = out
    Logger.info("in UserActor")
    QuotesActor.quotesActor.tell(IAmHere, getSelf())
  }

  @throws[Exception](classOf[Exception])
  override def onReceive(message: Any) {

    val sdf = new SimpleDateFormat("yyyy-MM-dd")

    message match {
      case quotes: Seq[_] =>
        val quoteMessage = Json.newObject()
        quoteMessage.put("type", "quotes")
        val array = quoteMessage.putArray("quotes")

        quotes.asInstanceOf[Seq[RealTimeQuote]].foreach {
          quote =>
            val quoteObjectNode = Json.newObject()
            quoteObjectNode.put("symbol", quote.id)
            quoteObjectNode.put("openPrice", quote.openPrice)
            quoteObjectNode.put("currentPrice", quote.currentPrice)
            quoteObjectNode.put("sentimentScore", quote.sentimentScore)
            array.add(quoteObjectNode)
        }
        Logger.info(quoteMessage.toString)
        out.write(quoteMessage)

      case historicQuotes: Map[_, _] =>
        val historicQuoteMessage = Json.newObject()
        historicQuoteMessage.put("type", "historicQuotes")

        historicQuotes.asInstanceOf[Map[String, HistoricalQuote]].foreach {
          case (tickerId, realTimeQuote) =>
            val array = historicQuoteMessage.putArray(realTimeQuote.id)
            val quoteObjectNode = Json.newObject()
            quoteObjectNode.put("symbol", realTimeQuote.id)
            quoteObjectNode.put("date", sdf.format(realTimeQuote.dateTime))
            quoteObjectNode.put("open", realTimeQuote.openPrice)
            quoteObjectNode.put("high", realTimeQuote.highOfTheDay)
            quoteObjectNode.put("low", realTimeQuote.lowOfTheDay)
            quoteObjectNode.put("close", realTimeQuote.closePrice)
            quoteObjectNode.put("sentimentScore", realTimeQuote.sentimentScore)
            array.add(quoteObjectNode)
        }
      case _ =>

    }
  }
}
