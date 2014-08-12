package com.blinkbox.books.creditoffer.persistence.cake

import javax.sql.DataSource

import com.blinkbox.books.creditoffer.persistence.data.DefaultPromotionRepository
import com.blinkbox.books.creditoffer.persistence.models.PromotionTables
import org.apache.commons.dbcp.BasicDataSource
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
    val database = Database.forDataSource(dataSource)
    import tables.driver.simple._
    database.withSession { implicit session =>
      val ddl = tables.promotions.ddl
      try {
        ddl.drop
      } catch { case _: JdbcSQLException => /* Do nothing */ }
      ddl.create
    }
    database
  }
  lazy val dataSource : DataSource = {
    val ds = new BasicDataSource
    ds.setDriverClassName("org.h2.Driver")
    ds.setMaxActive(20)
    ds.setMaxIdle(10)
    ds.setUrl("jdbc:h2:mem:features;DB_CLOSE_DELAY=-1")
    ds
  }
}

trait TestRepositoriesComponent extends RepositoriesComponent[TestDatabaseTypes] {
  this: DatabaseComponent[TestDatabaseTypes] =>

  override val promotionRepository = new DefaultPromotionRepository(tables)
}
