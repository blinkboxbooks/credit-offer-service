package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import com.blinkbox.books.clients.ConnectionException
import com.blinkbox.books.creditoffer.clients._
import com.blinkbox.books.messaging._
import com.blinkbox.books.schemas.events.user.v2.{User, UserId}
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.joda.money.Money
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{blocking, Future}

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
  accountCreditService: AccountCreditService, userService: UserService,
  eventSender: EventSender, errorHandler: ErrorHandler, retryInterval: FiniteDuration, delay: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  import DeviceRegistrationHandler._

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    val deviceRegistration = DeviceRegistrationEvent.fromXML(event.body.content)
    val userId = deviceRegistration.userId
    if (!isHudl2(deviceRegistration.device))
      Future.successful(())
    else {
      logger.info(s"Handling Hudl2 registration event. User id: $userId, device id: ${deviceRegistration.device.id}")
      for (
        _ <- Future(blocking(Thread.sleep(delay.toMillis))); // Temporary fix for CP-1998
        userProfile <- userService.userProfile(userId);
        _ <- accountCreditService.currentCredit(userId); // don't proceed if account credit service is not available
        grantedOption <- Future(offerDao.grant(userId, offerCode));
        creditedOption <- optionallyCredit(userId, grantedOption);
        creditedAmountOption = creditedOption.map(_.asMoney);
        _ = sendIfCredited(creditedAmountOption, grantedOption, UserId(userId), userProfile, offerCode)
      ) yield ()
    }
  }

  override def isTemporaryFailure(e: Throwable) =
    e.isInstanceOf[IOException] || e.isInstanceOf[TimeoutException] || e.isInstanceOf[ConnectionException] ||
      Option(e.getCause).exists(isTemporaryFailure)

  /** Decide if the given device registration is for a Hudl2. */
  private def isHudl2(device: DeviceDetails): Boolean = device.brand == Hudl2Brand && device.model == Hudl2Model

  private def optionallyCredit(userId: Int, granted: Option[GrantedOffer]): Future[Option[AccountCredit]] = granted match {
    case Some(grant) => accountCreditService.addCredit(userId, grant.creditedAmount).map(res => Some(res))
    case None => Future.successful(None)
  }

  private def sendIfCredited(creditAmount: Option[Money], granted: Option[GrantedOffer], userId: UserId, userProfile: UserProfile, offer: String): Future[Unit] =
    (creditAmount, granted) match {
      case (Some(amount), Some(grant)) => Future(eventSender.sendEvent(
        User(userId, userProfile.user_username, userProfile.user_first_name, userProfile.user_last_name), amount, grant.createdAt, offer))
      case _ => Future.successful(None)
    }

}

object DeviceRegistrationHandler {

  /** The fields used to recognise Hudl 2 devices. */
  val Hudl2Brand = "Hudl"
  val Hudl2Model = "Hudl 2"

  /** The unique ID for the Hudl2 free credit offer. */
  val offerCode = "account_credit_hudl2"
}
