package com.blinkbox.books.creditoffer.clients

import akka.actor.ActorRefFactory
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import com.blinkbox.books.clients.SendAndReceive
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.spray.v1._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{ Seconds, Span, Millis }
import org.joda.money.Money
import org.joda.money.CurrencyUnit
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration.DurationInt
import spray.http._
import spray.http.ContentType.apply

@RunWith(classOf[JUnitRunner])
class AdminAccountCreditClientTests extends FunSuite with ScalaFutures with Configuration {

  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("add credit to user account") {
    val client = new AdminAccountCreditServiceClient(AdminAccountCreditClientConfig(config)) with OkSendReceiveMock

    val amount = Money.of(CurrencyUnit.GBP, BigDecimal("10.0").bigDecimal)
    whenReady(client.addCredit(123, amount, "someaccesstoken")) { result =>
      assert(result == AccountCredit(amount))
    }
  }

  trait OkSendReceiveMock extends SendAndReceive {

    MediaTypes.register(`application/vnd.blinkboxbooks.data.v1+json`)
    val resp = """{"type":"urn:blinkboxbooks:schema:admin:credit","amount":"10.00","currency":"GBP"}""".stripMargin

    override def sendAndReceive(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext) = {
      (req: HttpRequest) =>
        {
          val response = HttpResponse(StatusCodes.OK, HttpEntity(`application/vnd.blinkboxbooks.data.v1+json`, resp))
          Future.successful(response)
        }
    }
  }

}