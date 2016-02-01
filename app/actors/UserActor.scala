package actors

import akka.actor.UntypedActor
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import play.Play
import play.libs.Json
import play.mvc.WebSocket
import java.util.List

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
    QuotesActor.quotesActor.tell(IAmHere, getSelf())
  }

  @throws[Exception](classOf[Exception])
  override def onReceive(message: Any) {
    message match {
      case quote: Quote =>
        val quoteMessage: ObjectNode = Json.newObject
        quoteMessage.put("type", "quoteUpdate")
        quoteMessage.put("symbol", quote.symbol)
        quoteMessage.put("openPrice", quote.openPrice)
        quoteMessage.put("currentPrice", quote.currentPrice)
        quoteMessage.put("sentimentScore", quote.sentimentScore)
        out.write(quoteMessage)
      case _ =>
    }
  }
}
