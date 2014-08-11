package com.blinkbox.books.creditoffer.persistence.cake

import java.sql.SQLIntegrityConstraintViolationException

import com.blinkbox.books.creditoffer.DbConfig
import com.blinkbox.books.creditoffer.persistence.data._
import com.blinkbox.books.creditoffer.persistence.models.PromotionTables

import scala.reflect._
import scala.slick.driver.{JdbcProfile, MySQLDriver}
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.profile.BasicProfile

trait DatabaseTypes {
  type Profile <: BasicProfile
  type ConstraintException <: Throwable
  type Database = Profile#Backend#Database
  lazy implicit val constraintExceptionTag: ClassTag[ConstraintException] = classTag[ConstraintException]
}

trait DatabaseComponent[DbTypes <: DatabaseTypes] {
  def driver: DbTypes#Profile
  def db: DbTypes#Database
  def tables: PromotionTables
}

trait RepositoriesComponent[DbTypes <: DatabaseTypes] {
  def promotionRepository: PromotionRepository[DbTypes#Profile]
  def reportRepository: ReportRepository[DbTypes#Profile]
}

trait DefaultDatabaseTypes extends DatabaseTypes {
  override type Profile = JdbcProfile
  override type ConstraintException = SQLIntegrityConstraintViolationException
}

trait DefaultDatabaseComponent extends DatabaseComponent[DefaultDatabaseTypes] {
  override val driver = MySQLDriver
  override val tables = PromotionTables(driver)
  override val db = Database.forURL(
    dbSettings.url.toString,
    driver = "com.mysql.jdbc.Driver",
    user = dbSettings.username,
    password = dbSettings.password)
  def dbSettings: DbConfig
}

trait DefaultRepositoriesComponent extends RepositoriesComponent[DefaultDatabaseTypes] {
  this: DatabaseComponent[DefaultDatabaseTypes] =>

  override val promotionRepository = new DefaultPromotionRepository(tables)
  override val reportRepository = new DefaultReportRepository(tables)
}

