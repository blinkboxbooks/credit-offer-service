package com.blinkbox.books.creditoffer.persistence

import java.sql.SQLIntegrityConstraintViolationException
import javax.sql.DataSource
import com.blinkbox.books.config.DatabaseConfig
import org.apache.commons.dbcp2.BasicDataSource
import scala.reflect._
import scala.slick.driver.{JdbcProfile, MySQLDriver}
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.profile.BasicProfile

//
// Common traits for database access with Slick.
// NOTE: This should be replaced with a common DB library when this is available.
//

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
}

trait DefaultDatabaseTypes extends DatabaseTypes {
  override type Profile = JdbcProfile
  override type ConstraintException = SQLIntegrityConstraintViolationException
}

trait DefaultDatabaseComponent extends DatabaseComponent[DefaultDatabaseTypes] {
  override val driver = MySQLDriver
  override val tables = PromotionTables(driver)
  override lazy val db = Database.forDataSource(dataSource)
  lazy val dataSource : DataSource = {
    val ds = new BasicDataSource
    ds.setDriverClassName("com.mysql.jdbc.Driver")
    ds.setUsername(dbSettings.user)
    ds.setPassword(dbSettings.pass)
    ds.setMaxTotal(20)
    ds.setMaxIdle(10)
    ds.setUrl(dbSettings.jdbcUrl)
    ds
  }
  def dbSettings: DatabaseConfig
}

trait DefaultRepositoriesComponent extends RepositoriesComponent[DefaultDatabaseTypes] {
  this: DatabaseComponent[DefaultDatabaseTypes] =>

  override val promotionRepository = new DefaultPromotionRepository(tables)
}

