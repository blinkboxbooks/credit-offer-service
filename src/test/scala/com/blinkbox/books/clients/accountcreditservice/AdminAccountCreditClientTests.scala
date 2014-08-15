package com.blinkbox.books.clients.accountcreditservice

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.clients.SendAndReceive
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.creditoffer.AdminAccountCreditClientConfig
import com.blinkbox.books.spray.v1._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Seconds, Span, Millis}
import spray.http._

import scala.concurrent.{Future, ExecutionContext}

@RunWith(classOf[JUnitRunner])
class AdminAccountCreditClientTests extends FunSuite with ScalaFutures with Configuration {

  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("add credit to user account") {
    val client = new AdminAccountCreditServiceClient(AdminAccountCreditClientConfig(config)) with OkSendReceiveMock

    whenReady(client.addCredit(123, BigDecimal("10.0"), "GBP")("someaccesstoken")) { result =>
      assert(result == AccountCredit(BigDecimal("10.0"), "GBP"))
    }
  }
}

trait OkSendReceiveMock extends SendAndReceive {
  import scala.concurrent.duration._

  MediaTypes.register(`application/vnd.blinkboxbooks.data.v1+json`)
  val resp = """{"type":"urn:blinkboxbooks:schema:admin:credit","amount":"10.00","currency":"GBP"}""".stripMargin

  override def sendAndReceive(implicit refFactory: ActorRefFactory,
                              executionContext: ExecutionContext, futureTimeout: Timeout = 60.seconds) = {
    (req: HttpRequest) => {
      val response = HttpResponse(StatusCodes.OK, HttpEntity(`application/vnd.blinkboxbooks.data.v1+json`, resp))
      Future.successful(response)
    }
  }
}