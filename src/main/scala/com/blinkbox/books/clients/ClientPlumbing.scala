package com.blinkbox.books.clients

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import spray.can.Http
import spray.client.pipelining._
import spray.http.StatusCodes._
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http._
import spray.httpx.unmarshalling._

import scala.concurrent.{ExecutionContext, Future}

/**
 * NOTE: This is code that should move into a common HTTP client library.
 */
trait ClientPlumbing extends StrictLogging {
  this: SendAndReceive =>

  protected implicit val system: ActorSystem
  protected implicit val ec: ExecutionContext

  protected val timeout: Timeout

  protected def pipeline(userToken: Option[String]): HttpRequest => Future[HttpResponse] = userToken match {
    case Some(token) => addCredentials(OAuth2BearerToken(token)) ~> sendAndReceive
    case None        => sendAndReceive
  }

  protected def unmarshal[T](unmarshaller: Unmarshaller[T])(entity: HttpEntity): T = unmarshaller(entity) match {
    case Right(result) => result
    case Left(error)   => throw new UnmarshallingException(error.toString)
  }

  protected def call[T](req: HttpRequest, pf: PartialFunction[HttpResponse, T], token: Option[String] = None): Future[T] = {
    val rawResponse = pipeline(token)(req)
    val unmarshalledResponse = rawResponse.map { resp => (pf orElse errors)(resp) }

    // we care about some spray exceptions, but don't want the clients to rely on them directly, hence the transformation
    unmarshalledResponse.transform(identity, exceptionTransformer)
  }

  private def exceptionTransformer(ex: Throwable) = ex match {
    case e: spray.can.Http.ConnectionAttemptFailedException => new ConnectionAttemptFailedException(e.getMessage, e)
    case e: spray.can.Http.RequestTimeoutException          => new RequestTimeoutException(e.getMessage, e)
    case e: spray.can.Http.ConnectionException              => new ConnectionException(e.getMessage, e)
    case other                                              => other
  }

  private def errors[T]: PartialFunction[HttpResponse, T] = {
    case r if r.status == NotFound                      => throw new NotFoundException(r.entity.data.asString)
    case r if r.status == RequestTimeout                => throw new RequestTimeoutException(r.entity.data.asString)
    case r if r.status == Unauthorized                  => throw new UnauthorizedException(r.entity.asString, extractHttpChallegeParams(r))
    case r if r.status == InternalServerError           => throw new ServiceErrorException(r.entity.data.asString)
    case r if r.status == BadGateway                    => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == ServiceUnavailable            => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == GatewayTimeout                => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == BandwidthLimitExceeded        => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == NetworkAuthenticationRequired => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == NetworkReadTimeout            => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case r if r.status == NetworkConnectTimeout         => throw new TemporaryConnectionException(r.status, r.entity.data.asString)
    case other                                          => throw new UnsupportedResponseException(s"Unsupported response from service: " +
                                                                              s"'${other.status}'. Message: ${other.entity.data.asString}")
  }

  // extracts information needed by the caller to rebuild the WWW-Authenticate header in case of 401 response
  private def extractHttpChallegeParams(response: HttpResponse): Map[String, String] = {
    import spray.util.pimpSeq
    val challenges = response.headers.findByType[`WWW-Authenticate`] match {
      case Some(`WWW-Authenticate`(cs)) => cs
      case _ =>
        logger.error("No 'WWW-Authenticate' header in the response")
        List.empty
    }
    if (challenges.size > 1) logger.warn(s"Expected 1 Http challenge, got ${challenges.size}. Using the first one")
    challenges.headOption.map(_.params).getOrElse(Map.empty)
  }

  def close(): Unit = {
    IO(Http).ask(Http.CloseAll)(timeout)
    system.shutdown()
  }
}

trait SendAndReceive {
  def sendAndReceive(implicit refFactory: ActorRefFactory, ec: ExecutionContext): SendReceive = sendReceive(refFactory, ec)
}

class ConnectionException(message: String, cause: Throwable = null) extends Exception(message, cause)

case class TemporaryConnectionException(statusCode: StatusCode, message: String) extends ConnectionException(s"status code: $statusCode. $message")
case class ConnectionAttemptFailedException(message: String, cause: Throwable = null) extends ConnectionException(message, cause)
case class RequestTimeoutException(message: String, cause: Throwable = null) extends ConnectionException(message, cause)
case class ThrottledException(message: String, cause: Throwable = null) extends ConnectionException(message, cause)

case class NotFoundException(message: String, cause: Throwable = null) extends Exception(message, cause)
case class UnmarshallingException(message: String, cause: Throwable = null) extends Exception(message, cause)
case class UnauthorizedException(message: String, challengeParams: Map[String, String], cause: Throwable = null) extends Exception(message, cause)
case class ServiceErrorException(message: String, cause: Throwable = null) extends Exception(message, cause)
case class UnsupportedResponseException(message: String, cause: Throwable = null) extends Exception(message, cause)