package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.EventHeader
import com.blinkbox.books.schemas.actions.email
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
    val attributes = Map("firstName" -> user.firstName, "lastName" -> user.lastName)
    val emailTrigger = email.v2.Email.Send(timestamp, email.v2.User(email.v2.UserId(user.id.value), user.username), templateName, attributes)
    delegate ! Event.json(EventHeader(EventSender.Originator), emailTrigger)
  }

}

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
