package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence.cake.DatabaseTypes
import com.blinkbox.books.creditoffer.persistence.data.PromotionRepository
import com.blinkbox.books.creditoffer.persistence.models.{Promotion, PromotionId}
import org.joda.money.Money
import org.joda.time.{DateTimeZone, DateTime}

/**
 * Interface for accessing persistently stored data about offers that have been
 * granted to users.
 */
trait OfferHistoryService {

  def grant(userId: Int, offerId: String): Boolean

  def isGranted(userId: Int, offerId: String): Boolean

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer]

  def listAllGrantedUsersForOffer(offerId: String): Seq[GrantedOffer]

}

case class GrantedOffer(userId: Int, offerId: String, createdAt: DateTime)

class DefaultOfferHistoryService[DbTypes <: DatabaseTypes](
    db: DbTypes#Database,
    promotionRepo: PromotionRepository[DbTypes#Profile],
    creditAmount: Money,
    creditLimit: Money ) extends OfferHistoryService {

  def grant(userId: Int, offerId: String) : Boolean = {
    db.withSession { implicit session =>
      session.withTransaction {
        val newTotalCreditAmount = promotionRepo.totalCreditedAmount.plus(creditAmount)
        // Check the offer has not been given beforehand and that adding it will not exceed the credit limits
        val canOffer = !isGranted(userId, offerId) &&
          (newTotalCreditAmount.isLessThan(creditLimit) || newTotalCreditAmount.isEqual(creditLimit))
        if (canOffer) {
          promotionRepo.insert(new Promotion(PromotionId.Invalid, userId, offerId, DateTime.now(DateTimeZone.UTC), creditAmount))
        }
        canOffer
      }
    }
  }

  def isGranted(userId: Int, offerId: String): Boolean = {
    db.withSession { implicit session =>
      promotionRepo.findByUserIdAndOfferId(userId, offerId) match {
        case Some(_) => true
        case None => false
      }
    }
  }

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer] = {
    db.withSession { implicit session =>
      promotionRepo.findGrantedOffersForUser(userId).map(promotion => new GrantedOffer(userId, promotion.offerId, promotion.createdAt))
    }
  }

  def listAllGrantedUsersForOffer(offerId: String): Seq[GrantedOffer] = {
    db.withSession { implicit session =>
      promotionRepo.findAllUsersUsingOffer(offerId).map(promotion => new GrantedOffer(promotion.userId, offerId, promotion.createdAt))
    }
  }
}
