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
      val canOffer = !isGranted(userId, offerId) && ! exceedsCreditLimit(newTotalCreditAmount)
      if (canOffer) {
        val createdTime = DateTime.now(DateTimeZone.UTC)
        promotionRepo.insert(new Promotion(userId, offerId, createdTime, creditAmount))
        Some(GrantedOffer(userId, offerId, creditAmount, createdTime))
      } else {
        None
      }
    }

  def isGranted(userId: Int, offerId: String)(implicit session: DbTypes#Session): Boolean = {
    val granted = promotionRepo.findByUserIdAndOfferId(userId, offerId).isDefined
    logger.info(s"Already granted offer $offerId for user with id $userId: $granted")
    granted
  }

  private def exceedsCreditLimit(amount: Money) = {
    val limitReached = amount.isGreaterThan(creditLimit)
    if (limitReached) logger.info("Credit limit reached")
    limitReached
  }

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer] =
    db.withSession { implicit session =>
      promotionRepo.findGrantedOffersForUser(userId).map(promotion =>
        new GrantedOffer(userId, promotion.offerId, promotion.creditedAmount, promotion.createdAt))
    }
}
