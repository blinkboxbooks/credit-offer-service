package com.blinkbox.books.creditoffer

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.util.Timeout
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.schemas.events.user.v2
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.UserId
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._
import org.joda.money.Money
import org.joda.money.CurrencyUnit

class EventSendersTest extends FlatSpec with MockitoSugar {

  val offer = "test-offer"
  val user = User(UserId(42), "bob@blinkbox.com", "Bob", "Builder")
  val creditAmount = Money.of(CurrencyUnit.GBP, BigDecimal(3.99).bigDecimal)

  trait SenderFixture {
    val eventSender1 = mock[EventSender]
    val eventSender2 = mock[EventSender]
    val eventSender3 = mock[EventSender]
    val eventSenders = Seq(eventSender1, eventSender2, eventSender3)
  }

  class PublisherFixture extends TestKit(ActorSystem("test-system")) with ImplicitSender {
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5.seconds)
    val publisher = TestProbe()
  }

  "A compound sender" should "do nothing when empty" in new SenderFixture {
    new CompoundEventSender(Nil).sendEvent(user, creditAmount, offer)
  }

  it should "pass on event to all delegates" in new SenderFixture {
    new CompoundEventSender(eventSenders).sendEvent(user, creditAmount, offer)
    eventSenders.foreach(sender => verify(sender).sendEvent(user, creditAmount, offer))
  }

  it should "pass on event to all other delegates when one fails" in new SenderFixture {
    val ex = new RuntimeException("Test exception")
    doThrow(ex).when(eventSender2).sendEvent(any[User], any[Money], anyString)

    new CompoundEventSender(eventSenders).sendEvent(user, creditAmount, offer)

    eventSenders.foreach(sender => verify(sender).sendEvent(user, creditAmount, offer))
  }

  "A reporting event sender" should "publish 'user credit' events on its output" in new PublisherFixture {
    // Send a message.
    val sender = new ReportingEventSender(publisher.ref)
    sender.sendEvent(user, creditAmount, offer)

    // Wait for the output.
    val published = publisher.expectMsgType[Event](3.seconds)

    // Check the content of the published message.
    val (_, eventUser, eventAmount, eventCurrency, eventReason) = published.body match {
      case User.Credited(timestamp, user, amount, currency, reason) => (timestamp, user, amount, currency, reason)
    }
    assert(eventUser == user && eventAmount == BigDecimal(creditAmount.getAmount) && 
        eventCurrency == creditAmount.getCurrencyUnit.getCurrencyCode && eventReason == offer)
  }

}
