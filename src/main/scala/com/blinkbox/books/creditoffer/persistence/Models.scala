package com.blinkbox.books.creditoffer.persistence

import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateTime

case class Promotion(userId: Int, offerId: String, createdAt: DateTime, creditedAmount: Money)

object Promotion {
  def applyTuple(userId: Int,
                 promotionId: String,
                 createdAt: DateTime,
                 creditedAmount: BigDecimal) = {
    val money = Money.of(CurrencyUnit.of("GBP"), creditedAmount.bigDecimal) // Using Java BigDecimal
    new Promotion(userId, promotionId, createdAt, money)
  }

  def unapplyTuple(promotion: Promotion) = {
    Some((promotion.userId, promotion.offerId, promotion.createdAt, BigDecimal(promotion.creditedAmount.getAmount)))
  }
}