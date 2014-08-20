package com.blinkbox.books.creditoffer

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.creditoffer.clients._
import com.blinkbox.books.creditoffer.persistence._
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * The main entry point of the credit offer service.
 */
object CreditOfferService extends App with Configuration with StrictLogging with Loggers
  with DefaultDatabaseComponent with DefaultRepositoriesComponent {

  logger.info(s"Starting Credit Offer service with config: $config")

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
  implicit val system = ActorSystem("credit-offer-service")
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

  val authClient = AuthServiceClient(AuthServiceClientConfig(config))
  val tokenProviderActor = system.actorOf(Props(classOf[ZuulTokenProviderActor], appConfig.account, authClient),
    name = "zuul-token-provider-actor")
  val authTokenProvider = new ZuulTokenProvider(tokenProviderActor)

  val adminAccountCreditService = RetryingAdminAccountCreditServiceClient(authTokenProvider, AdminAccountCreditClientConfig(config))
  val userService = RetryingUserServiceClient(authTokenProvider, AuthServiceClientConfig(config))

  val deviceRegistrationHandler = system.actorOf(Props(
    new DeviceRegistrationHandler(offerDao, adminAccountCreditService, userService, eventSender,
      deviceRegErrorHandler, appConfig.retryTime)), name = "device-registration-event-handler")

  // Create the actor that consumes messages from RabbitMQ, and kick it off.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.input,
    "credit-offer-consumer", deviceRegistrationHandler)),
    name = "device-registration-event-listener")
    .tell(RabbitMqConsumer.Init, null)

  logger.info("Started")
}
