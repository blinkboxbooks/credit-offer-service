package com.blinkbox.books.creditoffer.persistence

import org.joda.time.{DateTime, DateTimeZone}
import scala.slick.driver.JdbcProfile
import scala.slick.profile._

//
// Common traits for database access with Slick.
// NOTE: This should be replaced with a common DB library when this is available.
//

trait SlickSupport[Profile <: BasicProfile] {
  val driver: Profile
  type Session = Profile#Backend#Session
}

trait JdbcSupport extends SlickSupport[JdbcProfile] {
  import driver.simple._

  protected implicit def jodaDateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime, DateTimeZone.UTC))
}
