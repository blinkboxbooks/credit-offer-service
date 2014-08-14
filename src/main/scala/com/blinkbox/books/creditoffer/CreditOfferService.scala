package com.blinkbox.books.creditoffer

import akka.actor.{ ActorSystem, Props }
import akka.util.Timeout
import com.blinkbox.books.clients.accountcreditservice.AdminAccountCreditServiceClient
import com.blinkbox.books.clients.authservice.AuthServiceClient
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.creditoffer.persistence.cake._
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.rabbitmq._
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
  // Initialise the actor system.
  implicit val system = ActorSystem("credit-offer-service")
  implicit val ec = system.dispatcher
  implicit val requestTimeout = Timeout(appConfig.requestTimeout)
  // Connect to DB.
  val offerDao = new DefaultOfferHistoryService[DefaultDatabaseTypes](db, promotionRepository, appConfig.creditAmount, appConfig.creditLimit)
  // Create actors that handle email messages.
  val exactTargetPublisher = publisher(appConfig.exactTargetOutput, "exact-target-publisher")
  val reportingPublisher = publisher(appConfig.reportingOutput, "reporting-publisher")

  logger.debug("Initialising actors")
  val deviceRegErrorHandler = new ActorErrorHandler(publisher(appConfig.error, "registration-error-publisher"))

  val eventSender = new CompoundEventSender(Seq(new ReportingEventSender(reportingPublisher))) // TODO: Add email sender.
  val adminAccountCreditService = AdminAccountCreditServiceClient(AdminAccountCreditClientConfig(config))
  val authService = AuthServiceClient(AuthServiceClientConfig(config))
  
  val deviceRegistrationHandler = system.actorOf(Props(
    new DeviceRegistrationHandler(offerDao, adminAccountCreditService, authService, eventSender,
      deviceRegErrorHandler, appConfig.retryTime)), name = "device-registration-event-handler")

  override def dbSettings = appConfig.db

  private def publisher(config: PublisherConfiguration, actorName: String) =
    system.actorOf(Props(new RabbitMqConfirmedPublisher(publisherConnection, config)), name = actorName)

  // Create the actor that consumes messages from RabbitMQ, and kick it off.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.input,
    "credit-offer-consumer", deviceRegistrationHandler)),
    name = "device-registration-event-listener")
    .tell(RabbitMqConsumer.Init, null)

  logger.info("Started")
}
