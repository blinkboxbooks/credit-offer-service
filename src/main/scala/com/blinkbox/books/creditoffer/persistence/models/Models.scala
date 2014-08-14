package com.blinkbox.books.creditoffer.persistence.models

import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateTime


object Promotion {
  def applyTuple(id: PromotionId,
                 userId: Int,
                 promotionId: String,
                 createdAt: DateTime,
                 creditedAmount: BigDecimal) = {
    val money = Money.of(CurrencyUnit.of("GBP"), creditedAmount.bigDecimal) // Using Java BigDecimal
    new Promotion(id, userId, promotionId, createdAt, money)
  }

  def unapplyTuple(promotion: Promotion) = {
    Some(promotion.id, promotion.userId, promotion.offerId, promotion.createdAt, BigDecimal(promotion.creditedAmount.getAmount))
  }
}

case class Promotion(id: PromotionId, userId: Int, offerId: String, createdAt: DateTime, creditedAmount: Money)

case class PromotionId(value: Int) extends AnyVal

object PromotionId {
  val Invalid = PromotionId(-1)
}
