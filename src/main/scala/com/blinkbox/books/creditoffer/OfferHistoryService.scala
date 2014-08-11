package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence.cake.DatabaseTypes
import com.blinkbox.books.creditoffer.persistence.data.PromotionRepository
import com.blinkbox.books.creditoffer.persistence.models.{Promotion, PromotionId}
import org.joda.time.DateTime

/**
 * Interface for accessing persistently stored data about offers that have been
 * granted to users.
 */
trait OfferHistoryService {

  def grant(userId: Int, offerId: String): Unit

  def isGranted(userId: Int, offerId: String): Boolean

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer]

  def listAllGrantedUsersForOffer(offerId: String): Seq[GrantedOffer]

}

case class GrantedOffer(userId: Int, offerId: String, createdAt: DateTime)

class DefaultOfferHistoryService[DbTypes <: DatabaseTypes](
    db: DbTypes#Database,
    promotionRepo: PromotionRepository[DbTypes#Profile]) extends OfferHistoryService {

  def grant(userId: Int, offerId: String) {
    db.withSession { implicit session =>
      promotionRepo.insert(new Promotion(PromotionId.Invalid, userId, offerId, DateTime.now))
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
      promotionRepo.findGrantedOffersForUser(userId).map(promotion => new GrantedOffer(userId, promotion.promotionId, promotion.createdAt))
    }
  }

  def listAllGrantedUsersForOffer(offerId: String): Seq[GrantedOffer] = {
    db.withSession { implicit session =>
      promotionRepo.findAllUsersUsingOffer(offerId).map(promotion => new GrantedOffer(promotion.userId, offerId, promotion.createdAt))
    }
  }
}
