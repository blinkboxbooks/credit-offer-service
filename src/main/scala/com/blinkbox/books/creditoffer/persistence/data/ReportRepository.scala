package com.blinkbox.books.creditoffer.persistence.data

import com.blinkbox.books.creditoffer.persistence.support.SlickSupport
import com.blinkbox.books.creditoffer.persistence.models._

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait ReportRepository[Profile <: BasicProfile] extends SlickSupport[Profile] {
  def findById(id: ReportId)(implicit session: Session): Option[Report]
  def list(implicit session: Session): List[Report]
  def insert(feature: Report)(implicit session: Session): Int
  def update(feature: Report)(implicit session: Session): Int
  def delete(id: ReportId)(implicit session: Session): Int
}

trait JdbcReportRepository extends ReportRepository[JdbcProfile] with PromotionTables {
  import driver.simple._

  override def findById(id: ReportId)(implicit session: Session) =
    reports.filter(_.id === id).firstOption

  override def list(implicit session: Session) =
    reports.list

  override def insert(report: Report)(implicit session: Session) =
    reports.insert(report)

  override def update(report: Report)(implicit session: Session) =
    reports.filter(_.id === report.id).update(report)

  override def delete(id: ReportId)(implicit session: Session) =
    reports.filter(_.id === id).delete
}

class DefaultReportRepository(val tables: PromotionTables) extends PromotionTablesSupport with JdbcReportRepository
