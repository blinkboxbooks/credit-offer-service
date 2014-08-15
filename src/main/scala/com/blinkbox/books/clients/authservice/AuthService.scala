package com.blinkbox.books.clients.authservice

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.clients.{SendAndReceive, ClientPlumbing}
import com.blinkbox.books.creditoffer.AuthServiceClientConfig
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.{DefaultFormats, Formats}
import spray.http.{FormData, HttpResponse}
import spray.http.StatusCodes._
import spray.httpx.Json4sJacksonSupport
import spray.httpx.RequestBuilding.{Get, Post}
import spray.httpx.marshalling.BasicMarshallers.FormDataMarshaller

import scala.concurrent.Future

trait AuthService {
  def authenticate(userName: String, password: String): Future[AuthTokens]
  def authenticate(refreshToken: String): Future[AuthTokens]
  def userProfile(userId: Int)(authToken: String): Future[UserProfile]
}

case class AuthTokens(access_token: String, refresh_token: String)
case class UserProfile(user_username: String, user_first_name: String, user_last_name: String)

class AuthServiceClient(cfg: AuthServiceClientConfig) extends AuthService
  with ClientPlumbing with StrictLogging with Json4sJacksonSupport {
    this: SendAndReceive =>

  override protected val timeout: Timeout = Timeout(cfg.timeout)
  override protected implicit val system: ActorSystem = ActorSystem("auth-service-client")
  override implicit def json4sJacksonFormats: Formats = DefaultFormats

  override def authenticate(username: String, password: String): Future[AuthTokens] = {
    val reqData = Map("grant_type" -> "password", "username" -> username, "password" -> password)
    // Since we mix in JsonSupport in this class, we need to provide a FormData marshaller explicitly to send the request
    // with correct content type (application/x-www-form-urlencoded). Otherwise FormData is marshaled to JSON.
    call(Post(s"${cfg.url}/oauth2/token", FormData(reqData))(FormDataMarshaller), authTokensResponseHandler)
  }

  override def authenticate(refreshToken: String): Future[AuthTokens] = {
    val reqData = Map("grant_type" -> "refresh_token", "refresh_token" -> refreshToken)
    call(Post(s"${cfg.url}/oauth2/token", FormData(reqData))(FormDataMarshaller), authTokensResponseHandler)
  }


  override def userProfile(userId: Int)(authToken: String): Future[UserProfile] = {
    call(Get(s"${cfg.url}/admin/users/$userId"), userProfileResponseHandler, Some(authToken))
  }

  private def userProfileResponseHandler: PartialFunction[HttpResponse, UserProfile] = {
    case resp if resp.status == OK => unmarshal(json4sUnmarshaller[UserProfile])(resp.entity)
  }

  private def authTokensResponseHandler: PartialFunction[HttpResponse, AuthTokens] = {
    case resp if resp.status == OK => unmarshal(json4sUnmarshaller[AuthTokens])(resp.entity)
    case resp if resp.status == TooManyRequests => throw ThrottledException(resp.entity.data.asString) // TODO: include Retry-After header info
  }
}

object AuthServiceClient {
  def apply(cfg: AuthServiceClientConfig) = new AuthServiceClient(cfg) with SendAndReceive
}

case class ThrottledException(message: String, cause: Throwable = null) extends Exception(message, cause)