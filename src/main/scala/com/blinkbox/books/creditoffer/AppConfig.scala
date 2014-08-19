package com.blinkbox.books.creditoffer

import com.blinkbox.books.config._
import com.blinkbox.books.creditoffer.clients.Account
import com.blinkbox.books.creditoffer.clients.AdminAccountCreditClientConfig
import com.blinkbox.books.rabbitmq.RabbitMqConsumer
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher
import com.typesafe.config.Config
import java.net.{URI, URL}
import java.util.concurrent.TimeUnit
import org.joda.money.{CurrencyUnit, Money}
import scala.concurrent.duration._

case class AppConfig(
  creditLimit: Money,
  creditAmount: Money,
  retryTime: FiniteDuration,
  requestTimeout: FiniteDuration,
  accountCreditService: URL,
  input: RabbitMqConsumer.QueueConfiguration,
  exactTarget: ExactTargetConfig,
  mailer: MailerConfig,
  reportingOutput: PublisherConfiguration,
  error: PublisherConfiguration,
  db: DbConfig,
  useExactTarget: Boolean,
  account: Account)

case class DbConfig(
  url: URI,
  username: String,
  password: String)

case class MailerConfig(output: PublisherConfiguration, templateName: String, routingId: String)
case class ExactTargetConfig(output: PublisherConfiguration, templateName: String)

object AppConfig {
  def apply(config: Config): AppConfig = {
    val serviceConfig = config.getConfig("service.creditOffer")
    AppConfig(
      Money.of(CurrencyUnit.of("GBP"), serviceConfig.getDouble("creditLimit")),
      Money.of(CurrencyUnit.of("GBP"), serviceConfig.getDouble("creditAmount")),
      serviceConfig.getDuration("retryTime", TimeUnit.MILLISECONDS).millis,
      serviceConfig.getDuration("requestTimeout", TimeUnit.MILLISECONDS).millis,
      serviceConfig.getUrl("accountCreditService.url"),
      RabbitMqConsumer.QueueConfiguration(serviceConfig.getConfig("input")),
      ExactTargetConfig(serviceConfig.getConfig("exactTarget")),
      MailerConfig(serviceConfig.getConfig("mailer")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("reportingOutput")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("error")),
      DbConfig(serviceConfig.getConfig("db")),
      serviceConfig.getBoolean("useExactTarget"),
      Account(serviceConfig.getString("account.username"), serviceConfig.getString("account.password"))
    )
  }
}

object DbConfig {
  def apply(config: Config): DbConfig =
    DbConfig(
      config.getUri("url"),
      config.getString("username"),
      config.getString("password"))
}


object ExactTargetConfig {
  def apply(config: Config): ExactTargetConfig = ExactTargetConfig(
    PublisherConfiguration(config.getConfig("output")), config.getString("templateName"))
}

object MailerConfig {
  def apply(config: Config): MailerConfig = MailerConfig(
    PublisherConfiguration(config.getConfig("output")),
    config.getString("templateName"), config.getString("routingId"))
}
