package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence.cake.{TestDatabaseTypes, TestRepositoriesComponent, TestDatabaseComponent}
import com.blinkbox.books.creditoffer.persistence.models._
import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateTime
import org.scalatest.{Matchers, BeforeAndAfter, FlatSpec}

import scala.slick.jdbc.meta.MTable._

class OfferHistoryServiceTest extends FlatSpec with BeforeAndAfter with Matchers
  with TestDatabaseComponent with TestRepositoriesComponent {

  import tables.driver.simple._

  var testDatabase : tables.driver.backend.Database = _
  var historyDao : DefaultOfferHistoryService[TestDatabaseTypes] = _
  val firstOffer = "promotion_1"
  val secondOffer = "promotion_2"
  val firstUserId = 1
  val secondUserId = 2
  val creditedAmount = Money.of(CurrencyUnit.of("GBP"), 20.0f)
  val creditLimit = Money.of(CurrencyUnit.of("GBP"), 90.0f)

  def populateDatabase = {
    // Two different users and two different offers
    db.withSession { implicit session =>
      tables.promotions += new Promotion(PromotionId.Invalid, firstUserId, firstOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(PromotionId.Invalid, firstUserId, secondOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(PromotionId.Invalid, secondUserId, firstOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(PromotionId.Invalid, secondUserId, secondOffer, DateTime.now, creditedAmount)
    }
  }

  before {

    historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository, creditedAmount, creditLimit)
    populateDatabase
  }

  after {
    db.withSession { implicit session =>
      tables.promotions.mutate(_.delete)
    }
  }

  "The database connection" should "have the schema created successfully" in {
    db.withSession { implicit session =>
      val databaseTables = getTables(Some(""), Some(""), None, None).list
      databaseTables.size shouldBe 1
      databaseTables.count(_.name.name.equalsIgnoreCase("promotions")) shouldBe 1
    }
  }

  it should "be able to create Promotion entries successfully" in {
    db.withSession { implicit session =>
      val size = tables.promotions += new Promotion(PromotionId(1), 101, "sample_promotion", DateTime.now, creditedAmount)
      size shouldBe 1
    }
  }

  "OfferHistoryService" should "be able grant a user an offer by adding a promotion to the database" in {
    db.withSession { implicit session =>
      tables.promotions.mutate(_.delete)
      tables.promotions.list.size shouldBe 0
      historyDao.grant(firstUserId, firstOffer)
      tables.promotions.list.size shouldBe 1
    }
  }

  it should "find out if a user has been granted an offer correctly" in {
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
    val offersOfFirstUser = historyDao.listGrantedOffersForUser(firstUserId)
    offersOfFirstUser.size shouldBe 2
    offersOfFirstUser.exists(o => o.offerId == firstOffer && o.userId == firstUserId) shouldBe true
    offersOfFirstUser.exists(o => o.offerId == secondOffer && o.userId == firstUserId) shouldBe true

    val offersOfSecondUser = historyDao.listGrantedOffersForUser(secondUserId)
    offersOfSecondUser.size shouldBe 2
    offersOfSecondUser.exists(o => o.offerId == firstOffer && o.userId == secondUserId) shouldBe true
    offersOfSecondUser.exists(o => o.offerId == secondOffer && o.userId == secondUserId) shouldBe true
  }

  it should "list all users under a single promotional offer" in {
    val usersUsingFirstOffer = historyDao.listAllGrantedUsersForOffer(firstOffer)
    usersUsingFirstOffer.size shouldBe 2
    usersUsingFirstOffer.exists(o => o.offerId == firstOffer && o.userId == firstUserId) shouldBe true
    usersUsingFirstOffer.exists(o => o.offerId == firstOffer && o.userId == secondUserId) shouldBe true

    val usersUsingSecondOffer = historyDao.listAllGrantedUsersForOffer(secondOffer)
    usersUsingSecondOffer.size shouldBe 2
    usersUsingSecondOffer.exists(o => o.offerId == secondOffer && o.userId == firstUserId) shouldBe true
    usersUsingSecondOffer.exists(o => o.offerId == secondOffer && o.userId == secondUserId) shouldBe true
  }
}