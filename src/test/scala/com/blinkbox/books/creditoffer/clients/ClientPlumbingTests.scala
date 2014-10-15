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

  val tempFailureCodes = List(BadGateway, ServiceUnavailable, GatewayTimeout, BandwidthLimitExceeded,
    NetworkAuthenticationRequired, NetworkReadTimeout, NetworkConnectTimeout)

  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("Throws RequestTimeoutException when server returns 408 Request Timeout") {
    val client = clientWithMockResponse(HttpResponse(RequestTimeout, HttpEntity.Empty))

    failingWith[RequestTimeoutException](client.testMethod)
  }

  test("Throws TemporaryConnectionException when server has a temporary problem") {
    tempFailureCodes.foreach { code =>
      val client = clientWithMockResponse(HttpResponse(code, HttpEntity.Empty))

      val ex = failingWith[TemporaryConnectionException](client.testMethod)
      assert(ex.statusCode == code)
    }
  }

  test("Throws TemporaryConnectionException with a message when server has a temporary problem") {
    tempFailureCodes.foreach { code =>
      val client = clientWithMockResponse(HttpResponse(code, HttpEntity("problem description")))

      val ex = failingWith[TemporaryConnectionException](client.testMethod)
      assert(ex.statusCode == code)
      assert(ex.message == "problem description")
    }
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
