package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence.cake.{TestDatabaseTypes, TestRepositoriesComponent, TestDatabaseComponent}
import com.blinkbox.books.creditoffer.persistence.models._
import org.h2.jdbc.JdbcSQLException
import org.joda.time.DateTime
import org.scalatest.{Matchers, BeforeAndAfter, FlatSpec}

import scala.slick.driver.H2Driver
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.meta.MTable._

class OfferHistoryServiceTest extends FlatSpec with BeforeAndAfter with Matchers
  with TestDatabaseComponent with TestRepositoriesComponent {

  import tables.driver.simple._

  var databaseTables : List[MTable] = _

  var testDatabase : tables.driver.backend.Database = _
  var historyDao : DefaultOfferHistoryService[TestDatabaseTypes] = _
  val firstOffer = "promotion_1"
  val secondOffer = "promotion_2"
  val firstUserId = 1
  val secondUserId = 2

  def populateDatabase = {
    // Two different users and two different offers
    db.withSession { implicit session =>
      tables.promotions += new Promotion(PromotionId.Invalid, firstUserId, firstOffer, DateTime.now)
      tables.promotions += new Promotion(PromotionId.Invalid, firstUserId, secondOffer, DateTime.now)
      tables.promotions += new Promotion(PromotionId.Invalid, secondUserId, firstOffer, DateTime.now)
      tables.promotions += new Promotion(PromotionId.Invalid, secondUserId, secondOffer, DateTime.now)
    }
  }

  before {
    db.withSession { implicit session =>
      databaseTables = getTables(Some(""), Some(""), None, None).list
    }
    historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository)
    populateDatabase
  }

  after {
    db.withSession { implicit session =>
      tables.promotions.mutate(_.delete)
    }
  }

  "The database connection" should "have the schema created successfully" in {
    databaseTables.size shouldBe 1
    databaseTables.count(_.name.name.equalsIgnoreCase("promotions")) shouldBe 1
  }

  it should "be able to create Promotion entries successfully" in {
    db.withSession { implicit session =>
      val size = tables.promotions += new Promotion(PromotionId(1), 101, "sample_promotion", DateTime.now)
      size shouldBe 1
    }
  }

  "DbOfferHistoryDao" should "be able grant a user an offer by adding a promotion to the database" in {
    db.withSession { implicit session =>
      tables.promotions.mutate(_.delete)
      tables.promotions.list.size shouldBe 0
      historyDao.grant(firstUserId, firstOffer)
      tables.promotions.list.size shouldBe 1
    }
  }

  it should "find out if a user has been granted an offer correctly" in {
    populateDatabase

    // Check all the offers we have return true
    historyDao.isGranted(firstUserId, firstOffer) shouldBe true
    historyDao.isGranted(firstUserId, secondOffer) shouldBe true
    historyDao.isGranted(secondUserId, firstOffer) shouldBe true
    historyDao.isGranted(secondUserId, secondOffer) shouldBe true

    // Check that no other offers are returned true
    historyDao.isGranted(firstUserId, "unknown_offer") shouldBe false
    historyDao.isGranted(10, firstOffer) shouldBe false
  }

  it should "list all offers granted to a single user correctly" in {
    populateDatabase
  }

  it should "list all users under a single promotional offer" in {
    populateDatabase
  }
}