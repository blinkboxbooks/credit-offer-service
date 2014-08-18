package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import com.blinkbox.books.clients.accountcreditservice.AccountCreditService
import com.blinkbox.books.clients.authservice.{UserService, UserProfile}
import com.blinkbox.books.messaging._
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import org.joda.money.Money
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.UserId

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
class DeviceRegistrationHandler(offerDao: OfferHistoryService,
  adminAccountCreditService: AccountCreditService, userService: UserService,
  eventSender: EventSender, errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  val offerCode = "hudl2credit"

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    for (
      deviceRegistration <- Future(DeviceRegistrationEvent.fromXML(event.body.content)) if isHudl2(deviceRegistration);
      userId = deviceRegistration.userId;
      userProfile <- userService.userProfile(userId);
      grantedOptional <- Future(offerDao.grant(userId, offerCode));
      Some(granted) = grantedOptional if grantedOptional.isDefined;
      credited = granted.creditedAmount;
      accountCredit = adminAccountCreditService.addCredit(userId, credited.getAmount, credited.getCurrencyUnit.getCode);
      _ = sendMessages(userProfile, credited, offerCode)
    ) yield ()

  override def isTemporaryFailure(e: Throwable): Boolean = ???

  def isHudl2(event: DeviceRegistrationEvent): Boolean = ???

  def sendMessages(userProfile: UserProfile, creditAmount: Money, offer: String) =
    eventSender.sendEvent(User(UserId(42), userProfile.user_username, userProfile.user_first_name, userProfile.user_last_name), creditAmount, offer)
}
