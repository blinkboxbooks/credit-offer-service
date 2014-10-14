package com.blinkbox.books.creditoffer.clients

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.util.Timeout
import com.blinkbox.books.clients.{ClientPlumbing, RequestTimeoutException, SendAndReceive, TemporaryConnectionException}
import com.blinkbox.books.test.FailHelper
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.{AsyncAssertions, ScalaFutures}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import spray.http.StatusCodes._
import spray.http.{HttpEntity, HttpRequest, HttpResponse}
import spray.httpx.RequestBuilding.Get

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class ClientPlumbingTests extends FunSuite with ScalaFutures with AsyncAssertions with MockitoSugar with FailHelper {

  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("Throws RequestTimeoutException when Auth server returns 408 Request Timeout") {
    val client = clientWithMockResponse(HttpResponse(RequestTimeout, HttpEntity.Empty))

    failingWith[RequestTimeoutException](client.testMethod)
  }

  test("Throws TemporaryConnectionException when Auth server returns 502 Bad Gateway") {
    val client = clientWithMockResponse(HttpResponse(BadGateway, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == BadGateway)
  }

  test("Throws TemporaryConnectionException when Auth server returns 503 Service Unavailable") {
    val client = clientWithMockResponse(HttpResponse(ServiceUnavailable, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == ServiceUnavailable)
  }

  test("Throws TemporaryConnectionException when Auth server returns 504 Gateway Timeout") {
    val client = clientWithMockResponse(HttpResponse(GatewayTimeout, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == GatewayTimeout)
  }

  test("Throws TemporaryConnectionException when Auth server returns 509 Bandwidth Limit Exceeded") {
    val client = clientWithMockResponse(HttpResponse(BandwidthLimitExceeded, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == BandwidthLimitExceeded)
  }

  test("Throws TemporaryConnectionException when Auth server returns 511 Network Authentication Required") {
    val client = clientWithMockResponse(HttpResponse(NetworkAuthenticationRequired, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == NetworkAuthenticationRequired)
  }

  test("Throws TemporaryConnectionException when Auth server returns 598 Network Read Timeout") {
    val client = clientWithMockResponse(HttpResponse(NetworkReadTimeout, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == NetworkReadTimeout)
  }

  test("Throws TemporaryConnectionException when Auth server returns 599 Network Connect Timeout") {
    val client = clientWithMockResponse(HttpResponse(NetworkConnectTimeout, HttpEntity.Empty))

    val ex = failingWith[TemporaryConnectionException](client.testMethod)
    assert(ex.statusCode == NetworkConnectTimeout)
  }

  def clientWithMockResponse(resp: HttpResponse) =
    new TestClient with SendAndReceive {
      override def sendAndReceive(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext) = {
        (req: HttpRequest) => Future.successful(resp)
      }
    }

  class TestClient extends ClientPlumbing {
    this: SendAndReceive =>

    override protected implicit val system: ActorSystem = ActorSystem("test-system")
    override protected implicit val ec: ExecutionContext = global
    override protected val timeout: Timeout = 5.seconds

    private def okHandler: PartialFunction[HttpResponse, Unit] = {
      case resp if resp.status == OK => ()
    }

    def testMethod = call(Get(), okHandler, token = None)
  }
}
