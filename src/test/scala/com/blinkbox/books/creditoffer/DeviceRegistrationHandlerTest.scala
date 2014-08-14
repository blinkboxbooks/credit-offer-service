package com.blinkbox.books.creditoffer

import akka.actor.{ActorRef,ActorSystem,Props}
import akka.testkit.{ImplicitSender,TestKit}
import com.blinkbox.books.clients.accountcreditservice.AdminAccountCreditService
import com.blinkbox.books.clients.authservice.AuthService
import com.blinkbox.books.messaging._
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter,FunSuiteLike}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DeviceRegistrationHandlerTest extends TestKit(ActorSystem("test-system")) with ImplicitSender
  with FunSuiteLike with BeforeAndAfter with MockitoSugar {

  val retryInterval = 5.seconds
  
  private var offerDao: OfferHistoryService = _
  private var accountCreditService: AdminAccountCreditService = _
  private var authService: AuthService = _
  private var errorHandler: ErrorHandler = _
  private var eventSender: EventSender = _

  private var handler: ActorRef = _

  val eventHeader = EventHeader("test")

  before {
    eventSender = mock[EventSender]
    errorHandler = mock[ErrorHandler]
    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])

    handler = system.actorOf(Props(
      new DeviceRegistrationHandler(offerDao, accountCreditService, authService, eventSender, errorHandler, retryInterval)))
  }

  //
  // Normal operation.
  //

  ignore("Hudl2 registration for user that's granted offer") {
    fail("TODO")
  }

  ignore("Hudl2 registration for user that has already had the offer") {
    fail("TODO")
  }

  ignore("Hudl2 registration for user after offer has hit the cap") {
    fail("TODO")
  }

  ignore("Non-Hudl2 registration") {
    fail("TODO")
  }

  ignore("Needing re-authentication to add credit for user") {
    fail("TODO")
  }

  //
  // Failure scenarios.
  //

  ignore("Invalid registration message") {
    fail("TODO")
  }

  ignore("Temporary failure when getting user details") {
    fail("TODO")
  }

  ignore("Registration message for unknown user") {
    fail("TODO")
  }

  ignore("Temporary failure when adding credit to user") {
    fail("TODO")
  }

}
