package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import akka.util.Timeout
import com.blinkbox.books.logging.RichLogger
import com.blinkbox.books.messaging.{Event, EventHeader}
import com.blinkbox.books.schemas.actions.email
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.User.Credited
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.joda.money.Money
import org.joda.time.DateTime

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

/**
 * EventSender that publishes Email XML message to Mailer's exchange.
 */  
class MailerEventSender(delegate: ActorRef, templateName: String, routingId: String)(implicit ec: ExecutionContext, timeout: Timeout)
  extends EventSender with StrictLogging {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val content = buildEmailContent(user, creditedAmount, offer)
    logger.withContext("userId" -> user.id, "emailTemplateName" -> templateName) {
      _.info(s"Sending credit event for Mailer")
    }
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

/**
 * Publishes new-style JSON message to the shop's header exchange.
 */
class EmailEventSender(delegate: ActorRef, templateName: String)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender with StrictLogging {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val attributes = Map("firstName" -> user.firstName, "lastName" -> user.lastName)
    val emailTrigger = email.v2.Email.Send(timestamp, email.v2.User(email.v2.UserId(user.id.value), user.username), templateName, attributes)
    logger.withContext("userId" -> user.id, "emailTemplateName" -> templateName) {
      _.info("Sending credit event for ExactTarget")
    }
    delegate ! Event.json(EventHeader(EventSender.Originator), emailTrigger)
  }

}

/**
 * Publishes User.Credited JSON message to the shop's header exchange. 
 */
class ReportingEventSender(delegate: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventSender
  with StrictLogging {

  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) = {
    val creditEvent = User.Credited(timestamp, user,
      creditedAmount.getAmount, creditedAmount.getCurrencyUnit.getCurrencyCode, offer)
    logger.withContext("userId" -> user.id, "creditOffer" -> offer, "creditAmount" -> creditedAmount) {
      _.info("Sending credit event for Reporting service")
    }
    delegate ! Event.json(EventHeader(EventSender.Originator), creditEvent)
  }

}

/** Pass on event to a number of event senders. */
class CompoundEventSender(delegates: EventSender*) extends EventSender {
  override def sendEvent(user: User, creditedAmount: Money, timestamp: DateTime, offer: String) =
    delegates.foreach(delegate => Try(delegate.sendEvent(user, creditedAmount, timestamp, offer)))
}
