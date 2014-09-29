package com.blinkbox.books.creditoffer.clients

import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.clients._
import com.blinkbox.books.config._
import com.blinkbox.books.spray.JsonFormats._
import com.blinkbox.books.spray.v1.Version1JsonSupport
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.joda.money.{CurrencyUnit, Money}
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, Formats}
import spray.http.HttpResponse
import spray.http.StatusCodes._
import spray.httpx.RequestBuilding.{Get, Post}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

case class AccountCredit(amount: BigDecimal, currency: String) {
  def asMoney = Money.of(CurrencyUnit.of(currency), amount.bigDecimal)
}
object AccountCredit {
  def apply(amount: Money) = new AccountCredit(amount.getAmount, amount.getCurrencyUnit.getCurrencyCode)
}

case class AccountCreditList(items: List[AccountCredit])

case class AccountCreditReq(amount: BigDecimal, currency: String, reason: String)
object AccountCreditReq {
  def apply(amount: Money, reason: String) = new AccountCreditReq(amount.getAmount, amount.getCurrencyUnit.getCurrencyCode, reason)
}

case class AdminAccountCreditClientConfig(url: URL, timeout: FiniteDuration)

object AdminAccountCreditClientConfig {
  def apply(config: Config): AdminAccountCreditClientConfig = AdminAccountCreditClientConfig(
    config.getHttpUrl("service.adminaccountcredit.api.admin.internalUrl"),
    config.getDuration("service.adminaccountcredit.api.admin.timeout", TimeUnit.MILLISECONDS).millis)
}

trait AdminAccountCreditService {
  def addCredit(userId: Int, amount: Money, authToken: String): Future[AccountCredit]
  def currentCredit(userId: Int, authToken: String): Future[AccountCreditList]
}

trait AccountCreditService {
  def addCredit(userId: Int, amount: Money): Future[AccountCredit]
  def currentCredit(userId: Int): Future[AccountCreditList]
}

class AdminAccountCreditServiceClient(cfg: AdminAccountCreditClientConfig, val system: ActorSystem, val ec: ExecutionContext)
  extends AdminAccountCreditService with ClientPlumbing with StrictLogging with Version1JsonSupport {
  this: SendAndReceive =>

  import com.blinkbox.books.creditoffer.clients.AdminAccountCreditServiceClient._

  override protected val timeout: Timeout = Timeout(cfg.timeout)
  implicit override def version1JsonFormats: Formats = blinkboxFormat() + BigDecimalSerializer

  private val serviceUrl = cfg.url.toString

  override def addCredit(userId: Int, amount: Money, authToken: String): Future[AccountCredit] = {

    val credit = AccountCreditReq(amount, "customer") // Pre-defined value in admin account credit service.
    call(Post(s"$serviceUrl/admin/users/$userId/credit", credit), okPF[AccountCredit], Some(authToken))
  }

  override def currentCredit(userId: Int, authToken: String): Future[AccountCreditList] =
    call(Get(s"$serviceUrl/admin/users/$userId/credit"), okPF[AccountCreditList], Some(authToken))

  private def okPF[T: Manifest]: PartialFunction[HttpResponse, T] = {
    case resp if resp.status == OK => unmarshal(version1JsonUnmarshaller[T])(resp.entity)
  }
}

object AdminAccountCreditServiceClient {
  def apply(cfg: AdminAccountCreditClientConfig, system: ActorSystem, ec: ExecutionContext) =
    new AdminAccountCreditServiceClient(cfg, system, ec) with SendAndReceive

  /**
   *  Custom serializer for BigDecimals because:
   *  - Admin-account-credit service returns numbers in strings.
   *  - All our JSON-related helper libraries expect numbers to be JSON numbers
   */
  case object BigDecimalSerializer extends CustomSerializer[BigDecimal](_ => ({
    case JString(s) => BigDecimal(s)
    case JNull => null
  }, {
    case d: BigDecimal => JString(d.toString())
  }))
}

class RetryingAccountCreditServiceClient(cfg: AdminAccountCreditClientConfig, val tokenProvider: TokenProvider, system: ActorSystem, ec: ExecutionContext)
  extends AdminAccountCreditServiceClient(cfg, system, ec) with AccountCreditService with AuthRetry {
  this: SendAndReceive =>

  override def addCredit(userId: Int, amount: Money): Future[AccountCredit] = {
    logger.info(s"Adding credit ($amount) for user $userId")
    withAuthRetry(super.addCredit(userId, amount, _))(ec)
  }

  override def currentCredit(userId: Int): Future[AccountCreditList] = {
    logger.info(s"Retrieving current credit for user $userId")
    withAuthRetry(super.currentCredit(userId, _))(ec)
  }
}

object RetryingAdminAccountCreditServiceClient {
  def apply(cfg: Config, tokenProvider: TokenProvider, system: ActorSystem, ec: ExecutionContext) =
    new RetryingAccountCreditServiceClient(AdminAccountCreditClientConfig(cfg), tokenProvider, system, ec) with SendAndReceive
}