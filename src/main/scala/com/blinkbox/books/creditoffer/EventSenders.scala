package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.EventHeader
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.User.Credited
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.money.Money
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Common interface for objects that post messages when a user has received credit from an offer.
 */
trait EventSender {

  /**
   *  Send event fire-and-forget style, the implementations will handle retrying on failure.
   */
  def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String): Unit

}

object EventSender {
  val Originator = "credit-offer-service"
}

// Publish Email XML message to Mailer's exchange. 
class MailerEventSender(delegate: ActorRef, templateName: String, routingId: String)(implicit ec: ExecutionContext, timeout: Timeout)
  extends EventSender {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val content = buildEmailContent(user, creditedAmount, offer)
    delegate ! Event.xml(content, EventHeader(EventSender.Originator))
  }

  /** Generate output message in the old Mailer format. */
  private def buildEmailContent(user: User, creditedAmount: Money, offer: String): String = {
    val XmlDeclaration = """<?xml version="1.0" encoding="UTF-8"?>""" + "\n"
    val xml =
      <sendEmail r:messageId={ s"user-${user.id.value}-credited-$offer" } r:instance={ routingId } r:originator="bookStore" xmlns="http://schemas.blinkbox.com/books/emails/sending/v1" xmlns:r="http://schemas.blinkbox.com/books/routing/v1">
        <template>{ templateName }</template>
        <to>
          <recipient>
            <name>{ user.firstName }</name>
            <email>{ user.username }</email>
          </recipient>
        </to>
        <templateVariables>
          <templateVariable>
            <key>salutation</key>
            <value>{ user.firstName }</value>
          </templateVariable>
          <templateVariable>
            <key>amountCredited</key>
            <value>{ creditedAmount.getAmount }</value>
          </templateVariable>
        </templateVariables>
      </sendEmail>

    XmlDeclaration + xml.toString
  }

}

// Publish new-style JSON message to Agora header exchange.
class EmailEventSender(delegate: ActorRef, templateName: String)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val attributes = Map("name" -> user.username, "amount" -> creditedAmount.getAmount.toString)
    val emailTrigger = Email.Send(timestamp, Email.User(user.username, user.id.value.toString), templateName, attributes)
    delegate ! Event.json(EventHeader(EventSender.Originator), emailTrigger)
  }

}

// -------------- TODO - TEMPORARY: replace this with actual message schemas when available.
object Email {
  import com.blinkbox.books.messaging._

  case class User(emailAddress: String, id: String)
  case class Send(timestamp: DateTime, to: User, emailTemplateName: String, attributes: Map[String, String])

  implicit object Send extends JsonEventBody[Send] {
    val jsonMediaType = MediaType("application/vnd.blinkbox.books.events.email.send.v2+json")
    def unapply(body: EventBody): Option[(DateTime, User, String, Map[String, String])] =
      JsonEventBody.unapply[Send](body).flatMap(Send.unapply)
  }

  implicit object User extends JsonEventBody[User] {
    val jsonMediaType = MediaType("application/vnd.blinkbox.books.events.email.common.v2+json")
    def unapply(body: EventBody): Option[Any] = JsonEventBody.unapply[User](body).flatMap(User.unapply)
  }
}
// --------------

// Publish User.Credited JSON message to Agora header exchange.
class ReportingEventSender(delegate: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val creditEvent = User.Credited(timestamp, user,
      creditedAmount.getAmount, creditedAmount.getCurrencyUnit.getCurrencyCode, offer)
    delegate ! Event.json(EventHeader(EventSender.Originator), creditEvent)
  }

}

/** Pass on event to a number of event senders. */
class CompoundEventSender(delegates: EventSender*) extends EventSender {
  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) =
    delegates.foreach(delegate => Try(delegate.sendEvent(user, creditedAmount, timestamp, offer)))
}
