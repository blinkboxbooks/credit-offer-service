package com.blinkbox.books.creditoffer.persistence.models

import org.joda.time.DateTime

case class Promotion(id: PromotionId, userId: Int, promotionId: String, createdAt: DateTime)
case class PromotionId(value: Int) extends AnyVal
object PromotionId { val Invalid = PromotionId(-1) }
