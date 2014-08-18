package com.blinkbox.books.clients.accountcreditservice

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.clients.{SendAndReceive, ClientPlumbing}
import com.blinkbox.books.creditoffer.{AuthRetry, TokenProvider, AdminAccountCreditClientConfig}
import com.blinkbox.books.spray.JsonFormats._
import com.blinkbox.books.spray.v1.Version1JsonSupport
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.{Formats, CustomSerializer}
import org.json4s.JsonAST.{JNull, JString}
import spray.http.{HttpEntity, HttpResponse}
import spray.http.StatusCodes._
import spray.httpx.RequestBuilding.Post

import scala.concurrent.Future

case class AccountCredit(amount: BigDecimal, currency: String)
case class AccountCreditReq(amount: BigDecimal, currency: String, reason: String)

trait AdminAccountCreditService {
  def addCredit(userId: Int, amount: BigDecimal, currency: String, authToken: String): Future[AccountCredit]
}

trait AccountCreditService {
  def addCredit(userId: Int, amount: BigDecimal, currency: String): Future[AccountCredit]
}

class AdminAccountCreditServiceClient(cfg: AdminAccountCreditClientConfig)
  extends AdminAccountCreditService with ClientPlumbing with StrictLogging with Version1JsonSupport {
    this: SendAndReceive =>

  import AdminAccountCreditServiceClient._

  override protected val timeout: Timeout = Timeout(cfg.timeout)
  override protected implicit val system: ActorSystem = ActorSystem("admin-account-credit-service-client")
  implicit override def version1JsonFormats: Formats = blinkboxFormat() + BigDecimalSerializer

  private val serviceUrl = cfg.url.toString

  override def addCredit(userId: Int, amount: BigDecimal, currency: String, authToken: String): Future[AccountCredit] = {

    val credit = AccountCreditReq(amount, currency, "customer") // TODO: use more specific reason
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

  def addCredit(userId: Int, amount: BigDecimal, currency: String): Future[AccountCredit] =
    withAuthRetry(super.addCredit(userId, amount, currency, _))
}

object RetryingAdminAccountCreditServiceClient {
  def apply(tokenProvider: TokenProvider, cfg: AdminAccountCreditClientConfig) =
    new RetryingAccountCreditServiceClient(tokenProvider, cfg) with SendAndReceive
}