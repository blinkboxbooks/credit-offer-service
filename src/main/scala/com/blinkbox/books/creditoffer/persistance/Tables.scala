package com.blinkbox.books.creditoffer.persistance

import com.blinkbox.books.creditoffer.persistance.models._
import com.blinkbox.books.creditoffer.persistance.support.JdbcSupport
import org.joda.time.DateTime

import scala.slick.driver.JdbcProfile
import scala.slick.lifted.ProvenShape

trait Tables extends JdbcSupport {

  import driver.simple._;

  lazy val promotions = TableQuery[Promotions];
  lazy val reports = TableQuery[Reports];

  implicit lazy val promotionIdColumnType = MappedColumnType.base[PromotionId, Int](_.value, PromotionId(_))
  implicit lazy val reportIdColumnType = MappedColumnType.base[ReportId, Int](_.value, ReportId(_))

  class Promotions(tag: Tag) extends Table[Promotion](tag, "promotions") {
    def id = column[PromotionId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def promotionId = column[String]("promotion_id", O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)

    override def * : ProvenShape[Promotion] = (id, userId, promotionId, createdAt) <> (Promotion.tupled, Promotion.unapply)
  }

  class Reports(tag: Tag) extends Table[Report](tag, "reports") {
    def id = column[ReportId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def promotionId = column[String]("promotion_id", O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)

    override def * : ProvenShape[Report] = (id, userId, promotionId, createdAt) <> (Report.tupled, Report.unapply)
  }
}

object Tables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new Tables {
    override val driver: JdbcProfile = _driver
  }
}

trait TablesSupport extends JdbcSupport {
  val tables: Tables
  lazy val driver = tables.driver
}