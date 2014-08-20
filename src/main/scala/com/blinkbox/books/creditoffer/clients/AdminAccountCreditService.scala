package com.blinkbox.books.creditoffer.clients

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.clients._
import com.blinkbox.books.config._
import com.blinkbox.books.spray.JsonFormats._
import com.blinkbox.books.spray.v1.Version1JsonSupport
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.net.URL
import java.util.concurrent.TimeUnit
import org.joda.money.Money
import org.joda.money.CurrencyUnit
import org.json4s.{ Formats, CustomSerializer }
import org.json4s.JsonAST.{ JNull, JString }
import spray.http.{ HttpEntity, HttpResponse }
import spray.http.StatusCodes._
import spray.httpx.RequestBuilding.Post
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

case class AccountCredit(amount: BigDecimal, currency: String) {
  def asMoney = Money.of(CurrencyUnit.of(currency), amount.bigDecimal)
}
object AccountCredit {
  def apply(amount: Money) = new AccountCredit(amount.getAmount, amount.getCurrencyUnit.getCurrencyCode)
}
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
}

trait AccountCreditService {
  def addCredit(userId: Int, amount: Money): Future[AccountCredit]
}

class AdminAccountCreditServiceClient(cfg: AdminAccountCreditClientConfig)
  extends AdminAccountCreditService with ClientPlumbing with StrictLogging with Version1JsonSupport {
  this: SendAndReceive =>

  import AdminAccountCreditServiceClient._

  override protected val timeout: Timeout = Timeout(cfg.timeout)
  override protected implicit val system: ActorSystem = ActorSystem("admin-account-credit-service-client")
  implicit override def version1JsonFormats: Formats = blinkboxFormat() + BigDecimalSerializer

  private val serviceUrl = cfg.url.toString

  override def addCredit(userId: Int, amount: Money, authToken: String): Future[AccountCredit] = {

    val credit = AccountCreditReq(amount, "customer") // TODO: use more specific reason
    call(Post(s"$serviceUrl/admin/users/$userId/credit", credit), okPF, Some(authToken))
  }

  private val unmarshalResponse: HttpEntity => AccountCredit = unmarshal(version1JsonUnmarshaller[AccountCredit])

  private def okPF: PartialFunction[HttpResponse, AccountCredit] = {
    case resp if resp.status == OK => unmarshalResponse(resp.entity)
  }
}

object AdminAccountCreditServiceClient {
  def apply(cfg: AdminAccountCreditClientConfig) = new AdminAccountCreditServiceClient(cfg) with SendAndReceive

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

class RetryingAccountCreditServiceClient(override val tokenProvider: TokenProvider, cfg: AdminAccountCreditClientConfig)
  extends AdminAccountCreditServiceClient(cfg) with AccountCreditService with AuthRetry {
  this: SendAndReceive =>

  import scala.concurrent.ExecutionContext.Implicits.global // TODO: review this

  def addCredit(userId: Int, amount: Money): Future[AccountCredit] = {
    logger.info(s"Adding credit ($amount) for user $userId")
    withAuthRetry(super.addCredit(userId, amount, _))
  }
}

object RetryingAdminAccountCreditServiceClient {
  def apply(tokenProvider: TokenProvider, cfg: AdminAccountCreditClientConfig) =
    new RetryingAccountCreditServiceClient(tokenProvider, cfg) with SendAndReceive
}