package com.blinkbox.books.creditoffer

import com.blinkbox.books.creditoffer.persistence._
import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, BeforeAndAfter, FlatSpec}
import scala.slick.jdbc.meta.MTable._

@RunWith(classOf[JUnitRunner])
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
      tables.promotions += new Promotion(firstUserId, firstOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(firstUserId, secondOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(secondUserId, firstOffer, DateTime.now, creditedAmount)
      tables.promotions += new Promotion(secondUserId, secondOffer, DateTime.now, creditedAmount)
    }
  }

  def resetDatabase() = db.withSession { implicit session => tables.promotions.mutate(_.delete)}

  before {
    historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository, creditedAmount, creditLimit)
    populateDatabase
  }

  after {
    resetDatabase
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
      val size = tables.promotions += new Promotion(101, "sample_promotion", DateTime.now, creditedAmount)
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
    db.withSession { implicit session =>
      historyDao.isGranted(firstUserId, firstOffer) shouldBe true
      historyDao.isGranted(firstUserId, secondOffer) shouldBe true
      historyDao.isGranted(secondUserId, firstOffer) shouldBe true
      historyDao.isGranted(secondUserId, secondOffer) shouldBe true

      // Check that no other offers are returned true
      historyDao.isGranted(firstUserId, "unknown_offer") shouldBe false
      historyDao.isGranted(10, firstOffer) shouldBe false
    }
  }

  it should "not give a promotion when the Credit Limit has been reached " in {
    val newOffer = "Super Duper Mighty Morphin Offer Time!"
    db.withSession { implicit session =>
      historyDao.isGranted(firstUserId, newOffer) shouldBe false // Make sure the offer has not been granted
    }
    historyDao.grant(firstUserId, newOffer) shouldBe None
    val offersOfFirstUser = historyDao.listGrantedOffersForUser(firstUserId)

    // Make sure the new offer is not in the DB and that the offers that are there already are not effected
    offersOfFirstUser.exists(o => o.offerId == newOffer && o.userId == firstUserId) shouldBe false
    offersOfFirstUser.exists(o => o.offerId == firstOffer && o.userId == firstUserId) shouldBe true
    offersOfFirstUser.exists(o => o.offerId == secondOffer && o.userId == firstUserId) shouldBe true
  }

  it should "not give a promotion to a user if the user has received it already" in {
    // Create a service with a much higher credit limit and repopulate it
    resetDatabase
    val newCreditLimit = creditLimit.plus(1000.0)
    historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository, creditedAmount, newCreditLimit)
    populateDatabase

    // Make sure that the offer has already been granted before attempting to grant it again
    db.withSession { implicit session =>
      historyDao.isGranted(firstUserId, firstOffer) shouldBe true
    }
    historyDao.grant(firstUserId, firstOffer) shouldBe None

    // Make sure that the offer did not get written twice into the database by accident
    val offersOfFirstUser = historyDao.listGrantedOffersForUser(firstUserId)
    offersOfFirstUser.count(o => o.offerId == firstOffer && o.userId == firstUserId) shouldBe 1
  }

  it should "allow a promotion to be granted to a user if it culminates to the exact credit limit at the end" in {
    // Testing the edge case of the totalCreditedAmount + newOfferAmount equals the creditLimit
    resetDatabase
    val newCreditLimit = creditLimit.plus(10.0)
    historyDao = new DefaultOfferHistoryService[TestDatabaseTypes](db, promotionRepository, creditedAmount, newCreditLimit)
    populateDatabase

    val newOffer = "Super Duper Mighty Morphin Offer Time!"
    db.withSession { implicit session =>
      historyDao.isGranted(firstUserId, newOffer) shouldBe false // Make sure the offer has not been granted
    }
    val granted = historyDao.grant(firstUserId, newOffer)
    granted.get.userId shouldBe firstUserId
    granted.get.offerId shouldBe newOffer
    granted.get.creditedAmount shouldBe creditedAmount

    val offersOfFirstUser = historyDao.listGrantedOffersForUser(firstUserId)
    offersOfFirstUser.exists(o => o.offerId == newOffer && o.userId == firstUserId) shouldBe true
  }

}