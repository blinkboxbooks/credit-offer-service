package com.blinkbox.books.clients.authservice

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.clients.{UnauthorizedException, SendAndReceive}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.creditoffer.AuthServiceClientConfig
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.{AsyncAssertions, ScalaFutures}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import spray.http.ContentTypes.`application/json`
import spray.http._

import scala.concurrent.duration._
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import com.blinkbox.books.creditoffer.TokenProvider
import com.blinkbox.books.creditoffer.ZuulTokenProvider.withAuthRetry

@RunWith(classOf[JUnitRunner])
class AuthClientTests extends FunSuite with ScalaFutures with AsyncAssertions with Configuration with MockitoSugar {

  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("Authenticate with password") {
    val client = new AuthServiceClient(AuthServiceClientConfig(config)) with OkSendReceiveMock

    whenReady(client.authenticate("someuser", "somepassword")) { result =>
      assert(result == AuthTokens("someaccesstoken", "somerefreshtoken"))
    }
  }

  test("Throws ThrottledException when Auth server returns '429 Too Many Requests'") {
    val client = new AuthServiceClient(AuthServiceClientConfig(config)) with ThrottledSendReceiveMock
    val w = new Waiter

    client.authenticate("someuser", "somepassword") onComplete {
      case Success(_) => w.dismiss()
      case Failure(e) => w(throw e); w.dismiss()
    }

    intercept[ThrottledException] {
      w.await
    }
  }

  test("Authenticate with refresh token") {
    val client = new AuthServiceClient(AuthServiceClientConfig(config)) with OkSendReceiveMock

    whenReady(client.authenticate("somerefreshtoken")) { result =>
      assert(result == AuthTokens("someaccesstoken", "somerefreshtoken"))
    }
  }

  test("Get user profile") {
    val client = new AuthServiceClient(AuthServiceClientConfig(config)) with UserProfileSendReceiveMock

    whenReady(client.userProfile(1926)("someToken")) { result =>
      assert(result == UserProfile("credit-offer-service@blinkbox.com", "credit-offer", "service"))
    }
  }
}


trait OkSendReceiveMock extends SendAndReceive {
  val resp = """{
      |"access_token":"someaccesstoken",
      |"token_type":"bearer",
      |"expires_in":1800,
      |"refresh_token":"somerefreshtoken",
      |"user_id":"urn:blinkbox:zuul:user:1926",
      |"user_uri":"/users/1926",
      |"user_username":"credit-offer-service@blinkbox.com",
      |"user_first_name":"credit-offer",
      |"user_last_name":"service"}""".stripMargin

  override def sendAndReceive(implicit refFactory: ActorRefFactory,
                              executionContext: ExecutionContext, futureTimeout: Timeout = 60.seconds) = {
    (req: HttpRequest) => Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(`application/json`, resp)))
  }
}

trait ThrottledSendReceiveMock extends SendAndReceive {
  val resp = """{
               |  "error": "throttled",
               |  "message": "Too many requests"
               |}""".stripMargin

  override def sendAndReceive(implicit refFactory: ActorRefFactory,
                              executionContext: ExecutionContext, futureTimeout: Timeout = 60.seconds) = {
    (req: HttpRequest) =>
      Future.successful(HttpResponse(StatusCodes.TooManyRequests, HttpEntity(`application/json`, resp)))
  }
}

trait UserProfileSendReceiveMock extends SendAndReceive {
  val resp =
    """{
      |"user_id":"urn:blinkbox:zuul:user:1926",
      |"user_uri":"/users/1926",
      |"user_username":"credit-offer-service@blinkbox.com",
      |"user_first_name":"credit-offer",
      |"user_last_name":"service",
      |"user_allow_marketing_communications":false,
      |"user_previous_usernames":[]
      |}""".stripMargin

  override def sendAndReceive(implicit refFactory: ActorRefFactory,
                              executionContext: ExecutionContext, futureTimeout: Timeout = 60.seconds) = {
    (req: HttpRequest) =>
      Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(`application/json`, resp)))
  }
}
