package com.blinkbox.books.creditoffer.persistence.models

import com.blinkbox.books.creditoffer.persistence.support.JdbcSupport
import org.joda.time.DateTime
import scala.slick.driver.JdbcProfile

trait PromotionTables extends JdbcSupport {

  import driver.simple._

  lazy val promotions = TableQuery[Promotions]

  implicit lazy val promotionIdColumnType = MappedColumnType.base[PromotionId, Int](_.value, PromotionId(_))

  class Promotions(tag: Tag) extends Table[Promotion](tag, "promotions") {
    override def * = (id, userId, promotionId, createdAt, creditedAmount) <> ((Promotion.applyTuple _).tupled, Promotion.unapplyTuple)

    def id = column[PromotionId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)

    def userId = column[Int]("user_id", O.NotNull)

    def promotionId = column[String]("promotion_id", O.NotNull)

    def createdAt = column[DateTime]("created_at", O.NotNull)

    def creditedAmount = column[BigDecimal]("credited_amount", O.NotNull)

  }

}

object PromotionTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new PromotionTables {
    override val driver: JdbcProfile = _driver
  }
}

trait PromotionTablesSupport extends JdbcSupport {
  lazy val driver = tables.driver
  val tables: PromotionTables
}