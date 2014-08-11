package com.blinkbox.books.creditoffer.persistence.cake

import com.blinkbox.books.creditoffer.persistence.data.{DefaultReportRepository, DefaultPromotionRepository}
import com.blinkbox.books.creditoffer.persistence.models.PromotionTables
import org.h2.jdbc.JdbcSQLException

import scala.slick.driver.{H2Driver, JdbcProfile}
import scala.slick.jdbc.JdbcBackend._

trait TestDatabaseTypes extends DatabaseTypes {
  override type Profile = JdbcProfile
  override type ConstraintException = JdbcSQLException
}

trait TestDatabaseComponent extends DatabaseComponent[TestDatabaseTypes] {
  override val driver = H2Driver
  override val tables = PromotionTables(H2Driver)
  override val db = {
    val database = Database.forURL("jdbc:h2:mem:features;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    import tables.driver.simple._
    database.withSession { implicit session =>
      val ddl = tables.promotions.ddl ++ tables.reports.ddl
      try {
        ddl.drop
      } catch { case _: JdbcSQLException => /* Do nothing */ }
      ddl.create
    }
    database
  }
}

trait TestRepositoriesComponent extends RepositoriesComponent[TestDatabaseTypes] {
  this: DatabaseComponent[TestDatabaseTypes] =>

  override val promotionRepository = new DefaultPromotionRepository(tables)
  override val reportRepository = new DefaultReportRepository(tables)
}
