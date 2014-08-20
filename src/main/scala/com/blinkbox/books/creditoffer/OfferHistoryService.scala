package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.joda.money.Money
import org.joda.time.{DateTimeZone, DateTime}

/**
 * Interface for accessing persistently stored data about offers that have been
 * granted to users.
 */
trait OfferHistoryService {

  def grant(userId: Int, offerId: String): Option[GrantedOffer]

  def isGranted(userId: Int, offerId: String): Boolean

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer]

}

case class GrantedOffer(userId: Int, offerId: String, creditedAmount: Money, createdAt: DateTime)

class DefaultOfferHistoryService[DbTypes <: DatabaseTypes](
  db: DbTypes#Database,
  promotionRepo: PromotionRepository[DbTypes#Profile],
  creditAmount: Money,
  creditLimit: Money) extends OfferHistoryService with StrictLogging {

  def grant(userId: Int, offerId: String): Option[GrantedOffer] =
    db.withTransaction { implicit session =>
      val newTotalCreditAmount = promotionRepo.totalCreditedAmount.plus(creditAmount)
      // Check the offer has not been given beforehand and that adding it will not exceed the credit limits
      val canOffer = !isGranted(userId, offerId) &&
        (newTotalCreditAmount.isLessThan(creditLimit) || newTotalCreditAmount.isEqual(creditLimit))
      val grantResult = if (canOffer) {
        val createdTime = DateTime.now(DateTimeZone.UTC)
        promotionRepo.insert(new Promotion(PromotionId.Invalid, userId, offerId, createdTime, creditAmount))
        Some(GrantedOffer(userId, offerId, creditAmount, createdTime))
      } else {
        None
      }
      logger.info(s"Granted offer $offerId for user with id $userId: ${grantResult.nonEmpty}")
      grantResult
    }

  def isGranted(userId: Int, offerId: String): Boolean =
    db.withSession(implicit session => promotionRepo.findByUserIdAndOfferId(userId, offerId).isDefined)

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer] =
    db.withSession { implicit session =>
      promotionRepo.findGrantedOffersForUser(userId).map(promotion =>
        new GrantedOffer(userId, promotion.offerId, promotion.creditedAmount, promotion.createdAt))
    }

}
