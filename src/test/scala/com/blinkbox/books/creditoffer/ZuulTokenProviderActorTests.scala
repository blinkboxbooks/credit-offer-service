package com.blinkbox.books.creditoffer

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.blinkbox.books.clients.RequestTimeoutException
import com.blinkbox.books.clients.authservice.{AuthService, AuthTokens}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.test.MockitoSyrup
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuiteLike}
import org.scalatest.junit.JUnitRunner
import scala.concurrent.Future
import scala.concurrent.duration._
import org.mockito.Mockito._


@RunWith(classOf[JUnitRunner])
class ZuulTokenProviderActorTests extends TestKit(ActorSystem("test-system")) with ImplicitSender with FunSuiteLike
  with BeforeAndAfter with MockitoSyrup with Configuration {

  implicit val askTimeout = Timeout(5.seconds)

  val account = Account("credit-offer-service@blinkbox.com", "abc123")
  private var authService: AuthService = _
  private var tokenProvider: TestActorRef[ZuulTokenProviderActor] = _


  before {
    authService = mock[AuthService]
    tokenProvider = TestActorRef(new ZuulTokenProviderActor(account, authService))
  }

  test("TokenProvider initialises tokens on first GetAccessToken message") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.successful(AuthTokens("accessToken", "refreshToken")))

    assert(tokenProvider.underlyingActor.authTokens == None)

    tokenProvider ! GetAccessToken

    expectMsg(AccessToken("accessToken"))
    assert(tokenProvider.underlyingActor.authTokens == Some(AuthTokens("accessToken", "refreshToken")))
    verify(authService).authenticate(account.username, account.password)
    verifyNoMoreInteractions(authService)
  }

  test("TokenProvider initialises tokens on first RefreshedAccessToken message") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.successful(AuthTokens("accessToken", "refreshToken")))

    assert(tokenProvider.underlyingActor.authTokens == None)

    tokenProvider ! RefreshAccessToken

    expectMsg(AccessToken("accessToken"))
    assert(tokenProvider.underlyingActor.authTokens == Some(AuthTokens("accessToken", "refreshToken")))
    verify(authService).authenticate(account.username, account.password)
    verifyNoMoreInteractions(authService)
  }

  test("TokenProvider initialises access tokens once") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.successful(AuthTokens("accessToken", "refreshToken")))

    assert(tokenProvider.underlyingActor.authTokens == None)

    tokenProvider ! GetAccessToken
    tokenProvider ! GetAccessToken

    expectMsg(AccessToken("accessToken"))
    expectMsg(AccessToken("accessToken"))

    verify(authService, times(1)).authenticate(account.username, account.password)
    verifyNoMoreInteractions(authService)
  }

  test("TokenProvider refreshes token on RefreshedAccessToken message") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.successful(AuthTokens("accessToken", "refreshToken")))
    when(authService.authenticate("refreshToken"))
      .thenReturn(Future.successful(AuthTokens("newAccessToken", "refreshToken")))

    tokenProvider ! GetAccessToken
    tokenProvider ! RefreshAccessToken

    expectMsg(AccessToken("accessToken"))
    expectMsg(AccessToken("newAccessToken"))

    verify(authService).authenticate(account.username, account.password)
    verify(authService).authenticate("refreshToken")
    verifyNoMoreInteractions(authService)
  }

  test("TokenProvider does not get successful response from Auth service") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.failed(RequestTimeoutException("Zuul does not like me")))

    assert(tokenProvider.underlyingActor.authTokens == None)

    tokenProvider ! GetAccessToken

    expectMsg(RequestTimeoutException("Zuul does not like me"))

    assert(tokenProvider.underlyingActor.authTokens == None)
    verify(authService).authenticate(account.username, account.password)
    verifyNoMoreInteractions(authService)
  }

  test("TokenProvider recovers from failed AuthService requests") {
    when(authService.authenticate(account.username, account.password))
      .thenReturn(Future.failed(RequestTimeoutException("Zuul does not like me")),
                  Future.successful(AuthTokens("accessToken", "refreshToken")))

    assert(tokenProvider.underlyingActor.authTokens == None)

    tokenProvider ! GetAccessToken
    tokenProvider ! GetAccessToken

    expectMsg(RequestTimeoutException("Zuul does not like me"))
    expectMsg(AccessToken("accessToken"))

    assert(tokenProvider.underlyingActor.authTokens == Some(AuthTokens("accessToken", "refreshToken")))
    verify(authService, times(2)).authenticate(account.username, account.password)
    verifyNoMoreInteractions(authService)
  }
}
