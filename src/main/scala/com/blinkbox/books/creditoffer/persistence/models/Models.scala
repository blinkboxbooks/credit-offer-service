package com.blinkbox.books.creditoffer.persistence.models

import org.joda.time.DateTime

case class Promotion(id: PromotionId, userId: Int, promotionId: String, createdAt: DateTime)
case class PromotionId(value: Int) extends AnyVal
object PromotionId { val Invalid = PromotionId(-1) }

case class Report(id: ReportId, userId: Int, promotionId: String, createdAt: DateTime)
case class ReportId(value: Int) extends AnyVal
object ReportId { val Invalid = ReportId(-1) }