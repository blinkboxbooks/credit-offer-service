package com.blinkbox.books.creditoffer

import akka.actor.ActorRef
import com.blinkbox.books.clients.accountcreditservice.AccountCredit
import com.blinkbox.books.clients.accountcreditservice.AccountCreditService
import com.blinkbox.books.clients.authservice.{ UserService, UserProfile }
import com.blinkbox.books.messaging._
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.UserId
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.IOException
import java.util.concurrent.TimeoutException
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import spray.can.Http.ConnectionException

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
  eventSender: EventSender, errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  import DeviceRegistrationHandler._

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {

    val deviceRegistration = DeviceRegistrationEvent.fromXML(event.body.content)
    val userId = deviceRegistration.userId;
    if (!isHudl2(deviceRegistration.device))
      Future.successful()
    else
      for (
        userProfile <- userService.userProfile(userId);
        granted <- Future(offerDao.grant(userId, offerCode));
        creditedOption <- optionallyCredit(userId, granted);
        creditedAmountOption = creditedOption.map(c => Money.of(CurrencyUnit.of(c.currency), c.amount.bigDecimal)); // TODO: Make client APIs return Money instead.
        _ = sendIfCredited(creditedAmountOption, UserId(userId), userProfile, offerCode)
      ) yield ()
  }

  @tailrec
  final override def isTemporaryFailure(e: Throwable) =
    e.isInstanceOf[IOException] || e.isInstanceOf[TimeoutException] || e.isInstanceOf[ConnectionException] ||
      Option(e.getCause).isDefined && isTemporaryFailure(e.getCause)

  /** Decide if the given device registration is for a Hudl2. */
  private def isHudl2(device: DeviceDetails): Boolean = device.brand == Hudl2Brand && device.model == Hudl2Model

  private def optionallyCredit(userId: Int, granted: Option[GrantedOffer]): Future[Option[AccountCredit]] = granted match {
    case Some(grant) =>
      accountCreditService.addCredit(userId, grant.creditedAmount.getAmount, grant.creditedAmount.getCurrencyUnit.getCode)
        .map(res => Some(res))
    case None => Future.successful(None)
  }

  private def sendIfCredited(creditAmount: Option[Money], userId: UserId, userProfile: UserProfile, offer: String): Future[Unit] = creditAmount match {
    case Some(amount) => Future(eventSender.sendEvent(User(userId, userProfile.user_username, userProfile.user_first_name, userProfile.user_last_name), amount, offer))
    case None => Future.successful(None)
  }

}

object DeviceRegistrationHandler {

  /** The fields used to recognise Hudl 2 registrations. */
  val Hudl2Brand = "Hudl"
  val Hudl2Model = "Hudl 2"

  /** The unique ID for the Hudl2 free credit offer. */
  val offerCode = "account_credit_hudl2"
}
