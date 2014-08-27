package com.blinkbox.books.creditoffer.persistence

import org.joda.time.DateTime
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.ProvenShape.proveShapeOf

/**
 * Schema definition for promotions table.
 */
trait PromotionTables extends JdbcSupport {

  import driver.simple._

  lazy val promotions = TableQuery[Promotions]

  class Promotions(tag: Tag) extends Table[Promotion](tag, "promotions") {

    def userId = column[Int]("user_id", O.NotNull)
    def promotionId = column[String]("promotion_id", O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def creditedAmount = column[BigDecimal]("credited_amount", O.NotNull)
    
    override def * = (userId, promotionId, createdAt, creditedAmount) <> ((Promotion.applyTuple _).tupled, Promotion.unapplyTuple)

    def pk = primaryKey("pk_a", (userId, promotionId))
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