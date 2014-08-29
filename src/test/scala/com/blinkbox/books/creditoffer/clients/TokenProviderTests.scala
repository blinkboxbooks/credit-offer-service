package com.blinkbox.books.creditoffer.clients

import com.blinkbox.books.clients.UnauthorizedException
import com.blinkbox.books.test.MockitoSyrup
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.scalatest.FunSuite
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class TokenProviderTests extends FunSuite with ScalaFutures with MockitoSyrup {


  class WhenRetryTester(val tokenProvider: TokenProvider) extends StrictLogging with AuthRetry {
    def getToken(token: String): Future[String] = Future.successful(token)

    def triggerRetry(): Future[String] = withAuthRetry(getToken)
  }

  test("AuthRetry refreshes expired token") {
    val tokenProvider = mock[TokenProvider]

    when(tokenProvider.accessToken).thenReturn(Future.failed(UnauthorizedException("test 401", Map.empty)))
    when(tokenProvider.refreshedAccessToken).thenReturn(Future.successful(AccessToken("refreshedToken")))

    val tester = new WhenRetryTester(tokenProvider)

    whenReady(tester.triggerRetry()) { res =>
      assert(res == "refreshedToken")
    }
  }
}
