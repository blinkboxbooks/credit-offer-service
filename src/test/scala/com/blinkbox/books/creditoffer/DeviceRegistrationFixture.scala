package com.blinkbox.books.creditoffer
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import com.blinkbox.books.clients.authservice.UserProfile
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.EventHeader

/**
 * Helper code for generating test data.
 */
trait DeviceRegistrationFixture {

  val offerId = "test-offer"
  val offerAmount = Money.of(CurrencyUnit.GBP, 10.0)
  val offerTimestamp = DateTime.now(DateTimeZone.UTC)

  def username(userId: Int) = s"username $userId"
  def firstName(userId: Int) = s"first name $userId"
  def lastName(userId: Int) = s"last name $userId"
  def userProfile(userId: Int) = UserProfile(username(userId), firstName(userId), lastName(userId))
  def grantedOffer(userId: Int) = GrantedOffer(userId, offerId, offerAmount, offerTimestamp)

  def deviceRegistrationEvent(userId: Int, deviceMatchesOffer: Boolean) = {
    // TODO: Replace this with a constant on the impl with the real Hudl 2 device string.
    val (model, brand) = if (deviceMatchesOffer) ("Hudl2", "Tesco Hudl") else ("Generic Tablet", "OEM")
    val content =
      <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1" xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1" xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning" r:originator="zuul" v:version="1.0">
        <userId>{ userId }</userId>
        <timestamp>2013-12-30T19:15:23Z</timestamp>
        <client>
          <id>19384</id>
          <name>My New Tablet</name>
          <brand>{ brand }</brand>
          <model>{ model }</model>
          <os>Android Stick of Rock</os>
        </client>
      </registered>
    Event.xml(content.toString, EventHeader("test"))
  }

}
