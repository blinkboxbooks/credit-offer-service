package com.blinkbox.books.creditoffer

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConsumer
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.typesafe.config.Config
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher

case class AppConfig(
  retryTime: FiniteDuration,
  requestTimeout: FiniteDuration,
  accountCreditService: URL,
  input: RabbitMqConsumer.QueueConfiguration,
  exactTargetOutput: PublisherConfiguration,
  reportingOutput: PublisherConfiguration,
  error: PublisherConfiguration,
  db: DbConfig)

case class DbConfig(
  url: URI,
  username: String,
  password: String)

object AppConfig {
  def apply(config: Config): AppConfig = {
    val serviceConfig = config.getConfig("service.creditOffer")
    AppConfig(
      serviceConfig.getDuration("retryTime", TimeUnit.MILLISECONDS).millis,
      serviceConfig.getDuration("requestTimeout", TimeUnit.MILLISECONDS).millis,
      serviceConfig.getUrl("accountCreditService.url"),
      RabbitMqConsumer.QueueConfiguration(serviceConfig.getConfig("registrationListener.input")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("registrationListener.exactTargetOutput")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("registrationListener.reportingOutput")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("registrationListener.error")),
      DbConfig(serviceConfig.getConfig("db")))
  }
}

object DbConfig {
  def apply(config: Config): DbConfig =
    DbConfig(
      config.getUri("url"),
      config.getString("username"),
      config.getString("password"))
}
