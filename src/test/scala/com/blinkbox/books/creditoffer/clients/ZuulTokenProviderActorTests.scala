package com.blinkbox.books.creditoffer.clients

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.blinkbox.books.clients.RequestTimeoutException
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.test.MockitoSyrup
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.concurrent.{AsyncAssertions, ScalaFutures}
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSuiteLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ZuulTokenProviderActorTests extends TestKit(ActorSystem("test-system")) with ImplicitSender with FunSuiteLike
   with BeforeAndAfter with MockitoSyrup with ScalaFutures with AsyncAssertions with Configuration {

   // Settings for whenReady/Waiter. We override the default values because the first call to the mock
   // Feature service takes longer than the default values.
   implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

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

     expectMsg(Failure(RequestTimeoutException("Zuul does not like me")))

     assert(tokenProvider.underlyingActor.authTokens == None)
     verify(authService).authenticate(account.username, account.password)
     verifyNoMoreInteractions(authService)
   }

   test("mapTo works correctly when TokenProvider returns a failure") {
     when(authService.authenticate(account.username, account.password))
       .thenReturn(Future.failed(RequestTimeoutException("Zuul does not like me")))

     val w = new Waiter

     (tokenProvider ? GetAccessToken).mapTo[AccessToken] onComplete {
       case Success(_) => w.dismiss()
       case scala.util.Failure(e) => w(throw e); w.dismiss()
     }

     intercept[RequestTimeoutException] {
       w.await
     }
   }

   test("TokenProvider recovers from failed AuthService requests") {
     when(authService.authenticate(account.username, account.password))
       .thenReturn(Future.failed(RequestTimeoutException("Zuul does not like me")),
                   Future.successful(AuthTokens("accessToken", "refreshToken")))

     assert(tokenProvider.underlyingActor.authTokens == None)

     tokenProvider ! GetAccessToken
     tokenProvider ! GetAccessToken

     expectMsg(Failure(RequestTimeoutException("Zuul does not like me")))
     expectMsg(AccessToken("accessToken"))

     assert(tokenProvider.underlyingActor.authTokens == Some(AuthTokens("accessToken", "refreshToken")))
     verify(authService, times(2)).authenticate(account.username, account.password)
     verifyNoMoreInteractions(authService)
   }
 }
