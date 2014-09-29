package com.blinkbox.books.creditoffer.clients

import akka.actor.{ActorRefFactory, ActorSystem}
import com.blinkbox.books.clients.SendAndReceive
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.spray.v1._
import org.joda.money.{CurrencyUnit, Money}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import spray.http.ContentType.apply
import spray.http._

import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class AdminAccountCreditClientTests extends FunSuite with ScalaFutures with Configuration {

  val system = ActorSystem("test", config)
  val cfg = AdminAccountCreditClientConfig(config)
  // Settings for whenReady/Waiter. We override the default values because the first call to the mock
  // Feature service takes longer than the default values.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(25, Millis)))

  test("add credit to user account") {
    val client = new AdminAccountCreditServiceClient(cfg, system, system.dispatcher) with OkAddSendReceiveMock

    val amount = Money.of(CurrencyUnit.GBP, BigDecimal("10.0").bigDecimal)
    whenReady(client.addCredit(123, amount, "someaccesstoken")) { result =>
      assert(result == AccountCredit(amount))
    }
  }

  test("get current credit") {
    val client = new AdminAccountCreditServiceClient(cfg, system, system.dispatcher) with AccountListSendReceiveMock

    whenReady(client.currentCredit(123, "someaccesstoken")) { result =>
      assert(result == AccountCreditList(List(
        AccountCredit(Money.of(CurrencyUnit.GBP, BigDecimal("10.00").bigDecimal)),
        AccountCredit(Money.of(CurrencyUnit.EUR, BigDecimal("30.19").bigDecimal))))
      )
    }
  }

  trait OkAddSendReceiveMock extends SendAndReceive {

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

  trait AccountListSendReceiveMock extends SendAndReceive {

    MediaTypes.register(`application/vnd.blinkboxbooks.data.v1+json`)
    val resp =
      """{
        |"type":"urn:blinkboxbooks:schema:list",
        |"items":[
        |   {"type":"urn:blinkboxbooks:schema:admin:credit","amount":"10.00","currency":"GBP"},
        |   {"type":"urn:blinkboxbooks:schema:admin:credit","amount":"30.19","currency":"EUR"}
        |]}""".stripMargin

    override def sendAndReceive(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext) = {
      (req: HttpRequest) =>
      {
        val response = HttpResponse(StatusCodes.OK, HttpEntity(`application/vnd.blinkboxbooks.data.v1+json`, resp))
        Future.successful(response)
      }
    }
  }
}