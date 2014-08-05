package com.blinkbox.books.creditoffer

import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.rabbitmq._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq.RabbitMqConsumer.QueueConfiguration
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

/**
 * The main entry point of the credit offer service.
 */
object CreditOfferService extends App with Configuration with Logging with Loggers {

  logger.info(s"Starting Credit Offer service with config: $config")

  // Get configuration
  val appConfig = AppConfig(config)
  val rabbitMqConfig = RabbitMqConfig(config)

  val consumerConnection = RabbitMq.reliableConnection(RabbitMqConfig(config))
  val publisherConnection = RabbitMq.recoveredConnection(RabbitMqConfig(config))

  private def publisher(config: PublisherConfiguration, actorName: String) =
    system.actorOf(Props(new RabbitMqConfirmedPublisher(publisherConnection, config)), name = actorName)

  // Initialise the actor system.
  implicit val system = ActorSystem("credit-offer-service")
  implicit val ec = system.dispatcher
  implicit val requestTimeout = Timeout(appConfig.requestTimeout)

  // Connect to DB.
  val dbConfig = appConfig.db
  // TODO: Pass in the necessary parameters.
  val offerDao = new DbOfferHistoryDao()

  logger.debug("Initialising actors")

  // Create actors that handle email messages.
  val exactTargetPublisher = publisher(appConfig.exactTargetOutput, "exact-target-publisher")
  val reportingPublisher = publisher(appConfig.reportingOutput, "reporting-publisher")
  val deviceRegErrorHandler = new ActorErrorHandler(publisher(appConfig.error, "registration-error-publisher"))
  val deviceRegistrationHandler = system.actorOf(Props(
    new DeviceRegistrationHandler(offerDao, exactTargetPublisher, reportingPublisher, deviceRegErrorHandler, appConfig.retryTime)),
    name = "device-registration-event-handler")

  // Create the actor that consumes messages from RabbitMQ, and kick it off.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.input,
    "credit-offer-consumer", deviceRegistrationHandler)),
    name = "device-registration-event-listener")
    .tell(RabbitMqConsumer.Init, null)

  logger.info("Started")
}
