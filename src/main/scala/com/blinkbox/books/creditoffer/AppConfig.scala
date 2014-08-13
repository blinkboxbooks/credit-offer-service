package com.blinkbox.books.creditoffer

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConsumer
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.typesafe.config.Config
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import org.joda.money.{CurrencyUnit, Money}

import scala.concurrent.duration._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher

case class AppConfig(
  creditLimit: Money,
  creditAmount: Money,
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
      Money.of(CurrencyUnit.of("GBP"), serviceConfig.getDouble("creditLimit")),
      Money.of(CurrencyUnit.of("GBP"), serviceConfig.getDouble("creditAmount")),
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
