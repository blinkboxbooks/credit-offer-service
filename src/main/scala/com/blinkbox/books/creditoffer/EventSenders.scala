package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import akka.util.Timeout
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.EventHeader
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.User.Credited
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Common interface for objects that post messages when a user has received credit from an offer.
 */
trait EventSender {

  /**
   *  Send event fire-and-forget style, the implementations will handle retrying on failure.
   */
  def sendEvent(user: User, credited: BigDecimal, currency: String, offer: String): Unit

}

object EventSender {
  val Originator = "credit-offer-service"
}

// Publish Email XML message to Mailer's exchange. 
class MailerEventSender(delegate: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender {

  override def sendEvent(user: User, credited: BigDecimal, currency: String, offer: String) = ???

}

// Publish Exact Target JSON message to Agora header exchange. 
class ExactTargetEventSender(delegate: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender {

  override def sendEvent(user: User, credited: BigDecimal, currency: String, offer: String) = ???

}

// Publish User.Credited JSON message to Agora header exchange.
class ReportingEventSender(delegate: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender {

  override def sendEvent(user: User, credited: BigDecimal, currency: String, offer: String) = {
    val creditEvent = User.Credited(DateTime.now(DateTimeZone.UTC), user, credited, currency, offer)
    val header = EventHeader(EventSender.Originator)
    delegate ! Event.json(header, creditEvent)
  }

}

/** Pass on event to a number of event senders. */
class CompoundEventSender(delegates: Seq[EventSender]) extends EventSender {
  override def sendEvent(user: User, credited: BigDecimal, currency: String, offer: String) =
    delegates.foreach(delegate => Try(delegate.sendEvent(user, credited, currency, offer)))
}
