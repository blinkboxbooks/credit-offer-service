package com.blinkbox.books.creditoffer.persistance

import com.blinkbox.books.creditoffer.persistance.models.{Report, ReportId, PromotionId, Promotion}
import org.h2.jdbc.JdbcSQLException
import org.joda.time.DateTime
import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta._
import scala.slick.lifted
import scala.slick.lifted._
import org.scalatest._

import scala.slick.lifted.TableQuery

class TablesSuite extends FlatSpec with BeforeAndAfter with Matchers {

  val tables = Tables(H2Driver)
  import tables.driver.simple._

  implicit var session : Session = _

  var databaseTables : List[MTable] = _

  def refreshSession(database: tables.driver.backend.DatabaseDef) = {

    session = database.createSession
    val ddl = (tables.promotions.ddl ++ tables.reports.ddl)

    try {
      ddl.drop
    } catch { case _ : JdbcSQLException => /* Do nothing */ }

    ddl.create
    databaseTables = MTable.getTables().list()

    database
  }

  val db = {
    val threadId = Thread.currentThread().getId()
    Database.forURL(s"jdbc:h2:mem:auth$threadId;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  }

  var d : tables.driver.backend.DatabaseDef = _

  before {
    d = refreshSession(db)
  }

  "The database connection" should "have the schema created successfully" in {
    databaseTables.size shouldBe 2
    databaseTables.count(_.name.name.equalsIgnoreCase("promotions")) shouldBe 1
    databaseTables.count(_.name.name.equalsIgnoreCase("reports")) shouldBe 1
  }

  it should "be able to create Promotion entries successfully" in {
    val size = tables.promotions += new Promotion(PromotionId(1), 101, "sample_promotion", DateTime.now)
    size shouldBe 1
  }

  it should "be able to create Result entries successfully" in {
    val size = tables.reports += new Report(ReportId(1), 101, "sample_report", DateTime.now)
    size shouldBe 1
  }

}
