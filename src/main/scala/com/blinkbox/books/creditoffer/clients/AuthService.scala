package com.blinkbox.books.creditoffer.clients

import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.clients._
import com.blinkbox.books.config._
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.{DefaultFormats, Formats}
import spray.http.MediaTypes.`application/x-www-form-urlencoded`
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.Json4sJacksonSupport
import spray.httpx.RequestBuilding.{Get, Post}
import spray.httpx.marshalling.Marshaller

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

//
// Client API for auth service.
//
// NOTE: This should be replaced be a client library for the Auth service when that's available.
//
trait AuthService {
  def authenticate(userName: String, password: String): Future[AuthTokens]
  def authenticate(refreshToken: String): Future[AuthTokens]
  def userProfile(userId: Int, authToken: String): Future[UserProfile]
}

trait UserService {
  def userProfile(userId: Int): Future[UserProfile]
}

case class AuthTokens(access_token: String, refresh_token: String)
case class UserProfile(user_username: String, user_first_name: String, user_last_name: String)

class AuthServiceClient(cfg: AuthServiceClientConfig, val system: ActorSystem, val ec: ExecutionContext) extends AuthService
  with ClientPlumbing with StrictLogging with Json4sJacksonSupport {
    this: SendAndReceive =>

  import AuthServiceClient.FormDataMarshaller

  private val serviceUrl = cfg.url.toString

  override protected val timeout: Timeout = Timeout(cfg.timeout)
  override implicit def json4sJacksonFormats: Formats = DefaultFormats

  override def authenticate(username: String, password: String): Future[AuthTokens] = {
    val reqData = Map("grant_type" -> "password", "username" -> username, "password" -> password)
    // Since we mix in JsonSupport in this class, we need to provide a FormData marshaller explicitly to send the request
    // with correct content type (application/x-www-form-urlencoded). Otherwise FormData is marshaled to JSON.
    call(Post(s"$serviceUrl/oauth2/token", FormData(reqData))(FormDataMarshaller), authTokensResponseHandler)
  }

  override def authenticate(refreshToken: String): Future[AuthTokens] = {
    val reqData = Map("grant_type" -> "refresh_token", "refresh_token" -> refreshToken)
    call(Post(s"$serviceUrl/oauth2/token", FormData(reqData))(FormDataMarshaller), authTokensResponseHandler)
  }

  override def userProfile(userId: Int, authToken: String): Future[UserProfile] = {
    call(Get(s"$serviceUrl/admin/users/$userId"), userProfileResponseHandler, Some(authToken))
  }

  private def userProfileResponseHandler: PartialFunction[HttpResponse, UserProfile] = {
    case resp if resp.status == OK => unmarshal(json4sUnmarshaller[UserProfile])(resp.entity)
  }

  private def authTokensResponseHandler: PartialFunction[HttpResponse, AuthTokens] = {
    case resp if resp.status == OK => unmarshal(json4sUnmarshaller[AuthTokens])(resp.entity)
    case resp if resp.status == TooManyRequests =>
      val retryInterval = resp.headers.find(_.is("retry-after")).map(_.value)
      throw ThrottledException(retryInterval.fold("")(seconds => s"Retry after ${seconds}s"))
  }
}

object AuthServiceClient {
  def apply(cfg: Config, system: ActorSystem, ec: ExecutionContext) =
    new AuthServiceClient(AuthServiceClientConfig(cfg), system, ec) with SendAndReceive

  implicit val FormDataMarshaller: Marshaller[FormData] =
    Marshaller.delegate[FormData, String](`application/x-www-form-urlencoded`) { (formData, contentType) ⇒
      Uri.Query(formData.fields: _*).render(new StringRendering, HttpCharsets.`UTF-8`.nioCharset).get
    }
}

class RetryingUserServiceClient(cfg: AuthServiceClientConfig, val tokenProvider: TokenProvider, system: ActorSystem, ec: ExecutionContext)
  extends AuthServiceClient(cfg, system, ec) with UserService with AuthRetry {
  this: SendAndReceive =>

  override def userProfile(userId: Int): Future[UserProfile] = {
    logger.info(s"Retrieving user details for user with id: $userId")
    withAuthRetry(super.userProfile(userId, _))(ec)
  }
}

object RetryingUserServiceClient {
  def apply(cfg: AuthServiceClientConfig, tokenProvider: TokenProvider, system: ActorSystem, ec: ExecutionContext) =
    new RetryingUserServiceClient(cfg, tokenProvider, system, ec) with SendAndReceive
}

case class AuthServiceClientConfig(url: URL, timeout: FiniteDuration)

object AuthServiceClientConfig {
  def apply(config: Config): AuthServiceClientConfig = AuthServiceClientConfig(
    config.getHttpUrl("service.auth.api.public.internalUrl"),
    config.getDuration("service.auth.api.public.timeout", TimeUnit.MILLISECONDS).millis)
}
