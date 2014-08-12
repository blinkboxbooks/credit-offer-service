package com.blinkbox.books.creditoffer.persistence.data

import com.blinkbox.books.creditoffer.persistence.support.SlickSupport
import com.blinkbox.books.creditoffer.persistence.models._

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait PromotionRepository[Profile <: BasicProfile] extends SlickSupport[Profile] {
  def findById(id: PromotionId)(implicit session: Session): Option[Promotion]
  def findByUserIdAndOfferId(userId: Int, offerId: String)(implicit session: Session) : Option[Promotion]
  def findGrantedOffersForUser(userId: Int)(implicit session: Session): Seq[Promotion]
  def findAllUsersUsingOffer(offerId: String)(implicit session: Session) : Seq[Promotion]
  def list(implicit session: Session): List[Promotion]
  def insert(feature: Promotion)(implicit session: Session): Int
  def update(feature: Promotion)(implicit session: Session): Int
  def delete(id: PromotionId)(implicit session: Session): Int
}

trait JdbcPromotionRepository extends PromotionRepository[JdbcProfile] with PromotionTables {
  import driver.simple._

  override def findById(id: PromotionId)(implicit session: Session) =
    promotions.filter(_.id === id).firstOption

  override def findByUserIdAndOfferId(userId: Int, offerId: String)(implicit session: Session) =
    promotions.filter(entry => entry.userId === userId && entry.promotionId === offerId).firstOption

  override def findGrantedOffersForUser(userId: Int)(implicit session: Session)=
    promotions.filter(_.userId === userId).list

  override def findAllUsersUsingOffer(offerId: String)(implicit session: Session) =
    promotions.filter(_.promotionId === offerId).list

  override def list(implicit session: Session) =
    promotions.list

  override def insert(promotion: Promotion)(implicit session: Session) =
    promotions.insert(promotion)

  override def update(promotion: Promotion)(implicit session: Session) =
    promotions.filter(_.id === promotion.id).update(promotion)

  override def delete(id: PromotionId)(implicit session: Session) =
    promotions.filter(_.id === id).delete
}

class DefaultPromotionRepository(val tables: PromotionTables) extends PromotionTablesSupport with JdbcPromotionRepository
