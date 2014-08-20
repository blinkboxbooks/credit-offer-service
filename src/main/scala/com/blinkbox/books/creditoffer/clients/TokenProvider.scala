package com.blinkbox.books.creditoffer.clients

import akka.actor.{ActorLogging, ActorRef, Stash, Actor}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.blinkbox.books.clients.UnauthorizedException
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}

trait TokenProvider {
  def accessToken: Future[AccessToken]
  def refreshedAccessToken: Future[AccessToken]
}

case class Account(username: String, password: String)

case object GetAccessToken
case object RefreshAccessToken
case class AccessToken(value: String) extends AnyVal


class ZuulTokenProviderActor(acc: Account, authService: AuthService) extends Actor with Stash with ActorLogging {
  import context.become

  implicit val ec = context.dispatcher // TODO: consider which futures need to run where.

  var authTokens: Option[AuthTokens] = None

  override def receive: Receive = serveAuthTokens

  private def serveAuthTokens: Receive = {
    case GetAccessToken =>
      authTokens match {
        case Some(tokens) => sender ! AccessToken(tokens.access_token)
        case None =>
          become(refreshingTokens)
          authService.authenticate(acc.username, acc.password).pipeTo(self)(sender())
      }
    case RefreshAccessToken =>
      become(refreshingTokens)
      authTokens match {
        case Some(tokens) => authService.authenticate(tokens.refresh_token).pipeTo(self)(sender())
        case None => authService.authenticate(acc.username, acc.password).pipeTo(self)(sender())
      }
  }

  private def refreshingTokens: Receive = {
    case GetAccessToken => stash()
    case RefreshAccessToken => stash()
    case newTokens: AuthTokens =>
      log.debug(s"Got new tokens: $newTokens")
      authTokens = Some(newTokens)
      sender ! AccessToken(newTokens.access_token)
      unstashAll()
      become(serveAuthTokens)
    case akka.actor.Status.Failure(ex) =>
      sender ! ex
      unstashAll()
      become(serveAuthTokens)
  }
}

class ZuulTokenProvider(providerActor: ActorRef) extends TokenProvider {

  implicit val askTimeout = Timeout(5.minutes)

  override def accessToken: Future[AccessToken] = (providerActor ? GetAccessToken).mapTo[AccessToken]
  override def refreshedAccessToken: Future[AccessToken] = (providerActor ? RefreshAccessToken).mapTo[AccessToken]
}

trait AuthRetry {
  this: StrictLogging =>

  val tokenProvider: TokenProvider

  // TODO: Add tests for this
  def withAuthRetry[T](f: (String) => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    tokenProvider.accessToken.flatMap(accessToken => f(accessToken.value)) recoverWith {
      case ex: UnauthorizedException =>
        logger.info("Access token is invalid, getting a refreshed token and trying again...")
        tokenProvider.refreshedAccessToken.flatMap(accessToken => f(accessToken.value))
    }
  }
}