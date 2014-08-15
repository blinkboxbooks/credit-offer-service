package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import com.blinkbox.books.clients.UnauthorizedException
import com.blinkbox.books.messaging.ErrorHandler
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.ReliableEventHandler
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import com.blinkbox.books.clients.accountcreditservice.AdminAccountCreditService
import com.blinkbox.books.clients.authservice.{UserProfile, AuthService}
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
class DeviceRegistrationHandler(offerDao: OfferHistoryService, adminAccountCreditService: AdminAccountCreditService, authService: AuthService,
                                mailEventOutput: ActorRef, reportingEventOutput: ActorRef, errorHandler: ErrorHandler,
                                retryInterval: FiniteDuration) extends ReliableEventHandler(errorHandler, retryInterval) with Logging {

  import ZuulTokenProvider._

  val tokenProvider = new DummyProvider

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    for (
      deviceRegistration <- Future(DeviceRegistrationEvent.fromXML(event.body.content)) if isHudl2(deviceRegistration);
      userId = deviceRegistration.userId;
      userProfile <- withAuthRetry(tokenProvider, authService.userProfile(userId));
      granted = offerDao.isGranted(userId, "HUDL2");
      _ = offerDao.grant(userId, "HUDL2") if !granted;
      accountCredit = withAuthRetry(tokenProvider, adminAccountCreditService.addCredit(userId, BigDecimal("10.0"), "GBP"));
      _ = sendMessages(userProfile, BigDecimal("10.0"), "GBP", "HUDL2")
    ) yield ()


  override def isTemporaryFailure(e: Throwable): Boolean = true

  def isHudl2(event: DeviceRegistrationEvent): Boolean = ???
  def sendMessages(userDetails: UserProfile, credited: BigDecimal, currency: String, offer: String) = ???
}


class DummyProvider extends TokenProvider {
  override def accessToken: Future[AccessToken] = ???
  override def refreshedAccessToken: Future[AccessToken] = ???
}