package com.blinkbox.books.creditoffer

import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.config._
import com.blinkbox.books.creditoffer.clients._
import com.blinkbox.books.creditoffer.persistence._
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.rabbitmq._
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool

/**
 * The main entry point of the credit offer service.
 */
object CreditOfferService extends App with Configuration with StrictLogging with Loggers
  with DefaultDatabaseComponent with DefaultRepositoriesComponent {

  logger.info(s"Starting Credit Offer service")

  // Get configuration
  val appConfig = AppConfig(config)
  val rabbitMqConfig = RabbitMqConfig(config)
  val consumerConnection = RabbitMq.reliableConnection(RabbitMqConfig(config))
  val publisherConnection = RabbitMq.recoveredConnection(RabbitMqConfig(config))

  // Initialise database access.
  override def dbSettings = appConfig.db
  val offerDao = new DefaultOfferHistoryService[DefaultDatabaseTypes](
    db, promotionRepository, appConfig.creditAmount, appConfig.creditLimit)

  // Initialise the actor system.
  // Actor system needs a config instance we load to pick up the settings from external application.conf (CP-1879)
  implicit val system = ActorSystem("credit-offer-service", config)
  implicit val ec = system.dispatcher
  implicit val requestTimeout = Timeout(appConfig.requestTimeout)

  logger.debug("Initialising actors")

  private def publisher(config: PublisherConfiguration, actorName: String) =
    system.actorOf(Props(new RabbitMqConfirmedPublisher(publisherConnection, config)), name = actorName)

  val reportingPublisher = publisher(appConfig.reportingOutput, "reporting-publisher")
  val deviceRegErrorHandler = new ActorErrorHandler(publisher(appConfig.error, "registration-error-publisher"))
  val mailEventSender = if (appConfig.useExactTarget) {
    val exactTargetPublisher = publisher(appConfig.exactTarget.output, "exact-target-publisher")
    new EmailEventSender(exactTargetPublisher, appConfig.exactTarget.templateName)
  } else {
    val mailerPublisher = publisher(appConfig.mailer.output, "mailer-publisher")
    new MailerEventSender(mailerPublisher, appConfig.mailer.templateName, appConfig.mailer.routingId)
  }
  val eventSender = new CompoundEventSender(new ReportingEventSender(reportingPublisher), mailEventSender)

  // Create clients to auth and admin account credit services
  val clientEc = ExecutionContext.fromExecutorService(new ForkJoinPool)

  val authClient = AuthServiceClient(config, system, clientEc)
  val tokenProviderActor = system.actorOf(Props(classOf[ZuulTokenProviderActor], appConfig.account, authClient),
    name = "zuul-token-provider-actor")
  val authTokenProvider = new ZuulTokenProvider(tokenProviderActor, appConfig.requestTimeout)

  val adminAccountCreditService = RetryingAdminAccountCreditServiceClient(config, authTokenProvider, system, clientEc)
  val userService = RetryingUserServiceClient(AuthServiceClientConfig(config), authTokenProvider, system, clientEc)

  // Temporary fix for CP-1998. Remove when the auth server fix for this has been verified and deployed.
  val delay = config.getFiniteDuration("service.creditOffer.messageProcessingDelay")

  val deviceRegistrationHandler = system.actorOf(Props(
    new DeviceRegistrationHandler(offerDao, adminAccountCreditService, userService, eventSender,
      deviceRegErrorHandler, appConfig.retryTime, delay)), name = "device-registration-event-handler")

  // Create the actor that consumes messages from RabbitMQ, and kick it off.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.input,
    "credit-offer-consumer", deviceRegistrationHandler)),
    name = "device-registration-event-listener") ! RabbitMqConsumer.Init

  logger.info("Started")
}
