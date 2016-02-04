package actors

import akka.actor.UntypedActor
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import play.{Logger, Play}
import play.mvc.WebSocket
import scala.collection.JavaConverters._
import play.libs.Json

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

    message match {
      case quotes: Seq[_] =>
        val quoteMessage = Json.newObject()
        quoteMessage.put("type","quotes")
        val array = quoteMessage.putArray("quotes")

        quotes.asInstanceOf[Seq[Quote]].foreach {
          quote =>
            val quoteObjectNode = Json.newObject()
            quoteObjectNode.put("symbol", quote.symbol)
            quoteObjectNode.put("openPrice", quote.openPrice)
            quoteObjectNode.put("currentPrice", quote.currentPrice)
            quoteObjectNode.put("sentimentScore", quote.sentimentScore)
            array.add(quoteObjectNode)
        }

        out.write(quoteMessage)
      case _ =>
        
    }
  }
}
