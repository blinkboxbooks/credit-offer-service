package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import com.blinkbox.books.messaging.ErrorHandler
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.ReliableEventHandler
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

/**
 * Actor that processes device registrations.
 *
 * On each event, this actor will:
 *
 * - See if it's a device type that qualifies for an offer.
 * - See if the user has received the offer already.
 * - If the event and user qualifies:
 *   Credit the user, update their tracked state, send a message for reporting, and send a message for a confirmation email.
 */
class DeviceRegistrationHandler(offerDao: OfferHistoryDao, mailEventOutput: ActorRef, reportingEventOutput: ActorRef,
  errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with Logging {

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    for (
      deviceRegistration <- Future(DeviceRegistrationEvent.fromXML(event.body.content))
    ) yield()

  override def isTemporaryFailure(e: Throwable): Boolean = true

}
