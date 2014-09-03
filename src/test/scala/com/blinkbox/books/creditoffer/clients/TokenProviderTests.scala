package com.blinkbox.books.creditoffer.clients

import akka.actor.ActorSystem
import akka.pattern.AskTimeoutException
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.blinkbox.books.clients.UnauthorizedException
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.scalatest.FunSuiteLike
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.concurrent.Future

class TokenProviderTests extends TestKit(ActorSystem("test-system")) with ImplicitSender with FunSuiteLike with ScalaFutures with FailHelper with MockitoSyrup {

  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  class AuthRetryTester(val tokenProvider: TokenProvider) extends StrictLogging with AuthRetry {
    def getToken(token: String): Future[String] = Future.successful(token)

    def triggerRetry(): Future[String] = withAuthRetry(getToken)
  }

  test("AuthRetry refreshes expired token") {
    val tokenProvider = mock[TokenProvider]

    when(tokenProvider.accessToken).thenReturn(Future.failed(UnauthorizedException("test 401", Map.empty)))
    when(tokenProvider.refreshedAccessToken).thenReturn(Future.successful(AccessToken("refreshedToken")))

    val tester = new AuthRetryTester(tokenProvider)

    whenReady(tester.triggerRetry()) { res =>
      assert(res == "refreshedToken")
    }

    verify(tokenProvider).accessToken
    verify(tokenProvider).refreshedAccessToken
  }

  test("AuthRetry does not retry if the first attempt fails with something other than UnauthorizedException") {
    val tokenProvider = mock[TokenProvider]

    when(tokenProvider.accessToken).thenReturn(Future.failed(new RuntimeException("test exception")))

    val tester = new AuthRetryTester(tokenProvider)

    failingWith[RuntimeException](tester.triggerRetry())

    verify(tokenProvider).accessToken
    verifyNoMoreInteractions(tokenProvider)
  }

  test("ZuulTokenProvider throws AskTimeoutException if TokenProvider actor does not return in time") {
    val probe = TestProbe()
    val zuulTokenProvider = new ZuulTokenProvider(probe.ref, Timeout(1.second))

    failingWith[AskTimeoutException](zuulTokenProvider.accessToken)
  }

  test("AuthRetry does not retry if the first attempt times out") {
    val probe = TestProbe()
    val zuulTokenProvider = new ZuulTokenProvider(probe.ref, Timeout(1.second))
    val tester = new AuthRetryTester(zuulTokenProvider)

    failingWith[AskTimeoutException](tester.triggerRetry())
  }
}
