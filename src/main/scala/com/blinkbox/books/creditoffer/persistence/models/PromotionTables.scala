package com.blinkbox.books.creditoffer.persistence.models

import com.blinkbox.books.creditoffer.persistence.support.JdbcSupport
import org.joda.time.DateTime

import scala.slick.driver.JdbcProfile

trait PromotionTables extends JdbcSupport {
  import driver.simple._

  lazy val promotions = TableQuery[Promotions]
  lazy val reports = TableQuery[Reports]

  implicit lazy val promotionIdColumnType = MappedColumnType.base[PromotionId, Int](_.value, PromotionId(_))
  implicit lazy val reportIdColumnType = MappedColumnType.base[ReportId, Int](_.value, ReportId(_))

  class Promotions(tag: Tag) extends Table[Promotion](tag, "promotions") {
    def id = column[PromotionId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def promotionId = column[String]("promotion_id", O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)

    override def * = (id, userId, promotionId, createdAt) <> (Promotion.tupled, Promotion.unapply)
  }

  class Reports(tag: Tag) extends Table[Report](tag, "reports") {
    def id = column[ReportId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def promotionId = column[String]("promotion_id", O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)

    override def * = (id, userId, promotionId, createdAt) <> (Report.tupled, Report.unapply)
  }
}

object PromotionTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new PromotionTables {
    override val driver: JdbcProfile = _driver
  }
}

trait PromotionTablesSupport extends JdbcSupport {
  val tables: PromotionTables
  lazy val driver = tables.driver
}