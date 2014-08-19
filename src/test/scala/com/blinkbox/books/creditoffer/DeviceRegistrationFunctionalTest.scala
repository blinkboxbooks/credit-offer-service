package com.blinkbox.books.creditoffer

import akka.actor.{ ActorRef, ActorSystem, Props, Status }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import akka.util.Timeout
import com.blinkbox.books.clients.accountcreditservice.AdminAccountCreditService
import com.blinkbox.books.clients.authservice.AuthService
import com.blinkbox.books.creditoffer.persistence.cake.TestRepositoriesComponent
import com.blinkbox.books.creditoffer.persistence.cake.TestDatabaseComponent
import com.blinkbox.books.creditoffer.persistence.cake.TestDatabaseTypes
import com.blinkbox.books.creditoffer.persistence.models.Promotion
import com.blinkbox.books.messaging.ErrorHandler
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.runner.RunWith
import org.scalatest.{ BeforeAndAfter, FlatSpecLike, StreamlinedXmlEquality }
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._
import akka.actor.Status.Success
import com.blinkbox.books.clients.accountcreditservice.AccountCreditService
import com.blinkbox.books.clients.authservice.UserService

@RunWith(classOf[JUnitRunner])
class DeviceRegistrationFunctionalTest extends FlatSpecLike with BeforeAndAfter with MockitoSugar with StreamlinedXmlEquality
  with TestDatabaseComponent with TestRepositoriesComponent {

  import tables.driver.simple._

  val creditedAmount = Money.of(CurrencyUnit.of("GBP"), 20.0f)
  val creditLimit = Money.of(CurrencyUnit.of("GBP"), 100.0f)
  val retryInterval = 100.millis

  val user1 = 101
  val user2 = 102

  after {
    db.withSession { implicit session =>
      tables.promotions.mutate(_.delete)
    }
  }

  /**
   * A fixture that provides the various part of the service used in the test.
   */
  class TestFixture(useExactTarget: Boolean = true) extends TestKit(ActorSystem("test-system"))
    with DeviceRegistrationFixture with ImplicitSender {

    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(10.seconds)

    // A offer history service backed by an in-memory DB.
    val historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository, creditedAmount, creditLimit)

    // TODO: An account credit service (backed by something that takes auth config and may fail).
    val accountCreditService = mock[AccountCreditService]

    // TODO: A real user service that sits on top of the auth service.
    val userService = mock[UserService]

    val emailPublisher = TestProbe()
    val mailEventSender = if (useExactTarget) {
      new EmailEventSender(emailPublisher.ref, "exactTarget.templateName")
    } else {
      new MailerEventSender(emailPublisher.ref, "mailer.templateName", "mailer.routingId")
    }

    val reportingPublisher = TestProbe()
    val reportingSender = new ReportingEventSender(reportingPublisher.ref)

    val eventSender = new CompoundEventSender(reportingSender, mailEventSender)

    val errorHandler = mock[ErrorHandler]

    val handler = system.actorOf(Props(
      new DeviceRegistrationHandler(historyDao, accountCreditService, userService, eventSender, errorHandler, retryInterval)))
  }

  //
  // Test cases.
  //

  "An event handler with real services" should "handle valid registration by new user by assigning credit" ignore new TestFixture {

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    // TODO: Check that user was given credit.
    fail("TODO")
  }

  ignore should "Should ignore Hudl 2 registration for user that has received the offer already" in new TestFixture {
    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    // TODO: Check that user was only given credit once.
    fail("TODO")
  }

  ignore should "ignore Hudl2 registration for user after offer has hit the cap" in new TestFixture {
    fail("TODO")
  }

  ignore should ("handle case when needing re-authentication to get user details") in new TestFixture {
    fail("TODO")
  }

  ignore should ("handle case when needing re-authentication to add credit for user") in new TestFixture {
    fail("TODO")
  }

  "A handler configured to use Exact Target" should "send Exact Target events on succesful user credit" ignore new TestFixture(useExactTarget = true) {
    fail("TODO")
  }

  "A handler configured to use the Mailer" should "send Mailer events on succesful user credit" ignore new TestFixture(useExactTarget = false) {
    fail("TODO")
  }

}
