package com.blinkbox.books.creditoffer

import org.joda.time.DateTime

/**
 * Interface for accessing persistently stored data about offers that have been
 * granted to users.
 */
trait OfferHistoryDao {

  // This is just a sketch of what the API may look like.

  def granted(userId: Int, offerId: String)

  def isGranted(userId: Int, offerId: String)

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer]

  def listUserGrantedOffer(offerId: String): Seq[GrantedOffer]

}

case class GrantedOffer(
  userId: Int,
  offerId: String,
  createdAt: DateTime)

// TODO: Implement this...
class DbOfferHistoryDao extends OfferHistoryDao {

  def granted(userId: Int, offerId: String) = ???

  def isGranted(userId: Int, offerId: String) = ???

  def listGrantedOffersForUser(userId: Int): Seq[GrantedOffer] = ???

  def listUserGrantedOffer(offerId: String): Seq[GrantedOffer] = ???

}
