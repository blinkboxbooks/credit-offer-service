package com.blinkbox.books.creditoffer

import akka.actor.{ ActorRef, ActorSystem, Props, Status }
import akka.testkit.{ ImplicitSender, TestKit }
import com.blinkbox.books.clients.accountcreditservice.AdminAccountCreditService
import com.blinkbox.books.clients.accountcreditservice.AccountCredit
import com.blinkbox.books.clients.authservice.AuthService
import com.blinkbox.books.clients.authservice.UserProfile
import com.blinkbox.books.clients.NotFoundException
import com.blinkbox.books.messaging._
import com.blinkbox.books.schemas.events.user.v2
import java.io.IOException
import org.joda.money.Money
import org.joda.money.CurrencyUnit
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => eql }
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfter, FlatSpecLike, StreamlinedXmlEquality }
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.xml.sax.SAXException
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.xml.{ Elem, XML, Node }
import scala.xml.Utility.trim

@RunWith(classOf[JUnitRunner])
class DeviceRegistrationHandlerTest extends TestKit(ActorSystem("test-system")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfter with MockitoSugar with StreamlinedXmlEquality {

  val user1 = 101
  val user2 = 102
  val authToken = "test auth token"
  val retryInterval = 100.millis

  /**
   * A fixture for the mutable values used in device registration handler tests.
   */
  trait TestFixture extends DeviceRegistrationFixture {
    // Define mocks and initialise them with default behaviour.
    val offerDao: OfferHistoryService = mock[OfferHistoryService]
    val accountCreditService: AdminAccountCreditService = mock[AdminAccountCreditService]
    val authService: AuthService = mock[AuthService]
    val errorHandler: ErrorHandler = mock[ErrorHandler]
    val eventSender: EventSender = mock[EventSender]

    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])

    // Make these users known user.
    when(authService.userProfile(eql(user1))(anyString)).thenReturn(Future.successful(userProfile(user1)))
    when(authService.userProfile(eql(user2))(anyString)).thenReturn(Future.successful(userProfile(user2)))

    // This user has no previous grants hence is valid for the offer once.
    when(offerDao.grant(user1, offerId)).thenReturn(Some(grantedOffer(user1))).thenReturn(None)

    // This user is not eligible for the offer at all.
    when(offerDao.grant(user2, offerId)).thenReturn(None)

    // Make crediting this user succeed.
    when(accountCreditService.addCredit(user1, offerAmount.getAmount, offerAmount.getCurrencyUnit.getCurrencyCode)(authToken))
      .thenReturn(Future.successful(AccountCredit(offerAmount.getAmount, offerAmount.getCurrencyUnit.getCurrencyCode)))

    // The default object under test.
    val handler = createHandler()

    /** Create actor under test. */
    def createHandler(): ActorRef = system.actorOf(Props(
      new DeviceRegistrationHandler(offerDao, accountCreditService, authService, eventSender, errorHandler, retryInterval)))

    /** Check that the event was processed successfully by checking the various outputs. */
    def checkSuccessfulResult(userId: Int) = {
      // Check that the user was credited - once and only once.
      verify(accountCreditService, times(1)).addCredit(user1, offerAmount.getAmount, offerAmount.getCurrencyUnit.getCurrencyCode)(authToken)

      // Check output events were triggered.
      verify(eventSender).sendEvent(v2.User(v2.UserId(user1), username(user1), firstName(user1), lastName(user1)), offerAmount, offerId)

      // Check no errors were sent.
      verify(errorHandler, times(0)).handleError(any[Event], any[Throwable])
    }

    /** Check that event processing failed and was treated correctly. */
    def checkFailure[T <: Throwable](event: Event)(implicit manifest: Manifest[T]) {
      // Check no user was credited.
      verify(accountCreditService, times(0)).addCredit(anyInt, any[BigDecimal], anyString)(anyString)

      // Check no events were sent.
      verify(eventSender, times(0)).sendEvent(any[v2.User], any[Money], anyString)

      // Check event was passed on to error handler, along with the expected exception.
      val expectedExceptionClass = manifest.runtimeClass.asInstanceOf[Class[T]]
      verify(errorHandler).handleError(eql(event), isA(expectedExceptionClass))
    }

    /** Check that event was processed but ignored. */
    def checkIgnored() = {
      // Check that no user was credited.
      verify(accountCreditService, times(0)).addCredit(anyInt, any[BigDecimal], anyString)(anyString)

      // Check no events were sent.
      verify(eventSender, times(0)).sendEvent(any[v2.User], any[Money], anyString)

      // Should not have posted an error.
      verify(errorHandler, times(0)).handleError(any[Event], any[Throwable])
    }

  }

  //
  // Tests for normal operation.
  //

  "A registration handler" should "handle registration for new Hudl 2 user that is eligible for offer" in new TestFixture {
    // Send a Hudl2 registration for a new user.
    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)

    // Check the result of the overall flow.
    expectMsgType[Status.Success]

    // Check it requested the right user details.
    verify(authService).userProfile(eql(user1))(any[String])

    checkSuccessfulResult(user1)
  }

  it should "ignore registration for user that isn't eligible for the offer" in new TestFixture {
    handler ! deviceRegistrationEvent(user2, deviceMatchesOffer = true)

    expectMsgType[Status.Success]

    checkIgnored()
  }

  it should "ignore registrations for devices that aren't covered by the offer" in new TestFixture {
    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = false)

    expectMsgType[Status.Success]

    checkIgnored()
  }

  it should "process a sequence of events" in new TestFixture {
    val invalidEvent = Event.xml("Invalid XML", EventHeader("test"))
    handler ! invalidEvent

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)

    handler ! deviceRegistrationEvent(user2, deviceMatchesOffer = true)

    expectMsgType[Status.Failure]
    checkFailure[SAXException](invalidEvent)

    expectMsgType[Status.Success]
    expectMsgType[Status.Success]

    checkSuccessfulResult(user1)
    checkSuccessfulResult(user2)
  }

  //
  // Tests for failure scenarios.
  //

  it should "fail invalid registration messages" in new TestFixture {
    val event = Event.xml("Invalid XML", EventHeader("test"))
    handler ! event

    expectMsgType[Status.Failure]
    checkFailure[SAXException](event)
  }

  it should "fail a registration message for an unknown user" in new TestFixture {
    val event = deviceRegistrationEvent(999, deviceMatchesOffer = true)
    handler ! event

    expectMsgType[Status.Failure]
    checkFailure[NotFoundException](event)
  }

  it should "recover from a temporary failure when getting user details" in new TestFixture {
    // Force a temporary exception to happen when getting user details.
    val tempException = new IOException("Test exception")
    when(authService.userProfile(eql(user1))(anyString))
      .thenThrow(tempException)
      .thenThrow(tempException)
      .thenReturn(Future.successful(userProfile(user1)))

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    checkSuccessfulResult(user1)
  }

  it should "recover from a temporary failure when adding credit to user" in new TestFixture {
    val tempException = new IOException("Test exception")
    when(accountCreditService.addCredit(user1, offerAmount.getAmount, offerAmount.getCurrencyUnit.getCurrencyCode)(authToken))
      .thenThrow(tempException)
      .thenThrow(tempException)
      .thenReturn(Future.successful(AccountCredit(offerAmount.getAmount, offerAmount.getCurrencyUnit.getCurrencyCode)))

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    checkSuccessfulResult(user1)
  }

  it should "not retry any part of the workflow when a failure happens on sending events" in new TestFixture {
    when(eventSender.sendEvent(any[v2.User], any[Money], any[String]))
      .thenThrow(new RuntimeException("Test exception"))

    handler ! deviceRegistrationEvent(user1, deviceMatchesOffer = true)
    expectMsgType[Status.Success]

    checkSuccessfulResult(user1)
  }

}
