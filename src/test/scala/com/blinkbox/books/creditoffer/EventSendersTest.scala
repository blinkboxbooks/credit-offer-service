package com.blinkbox.books.creditoffer

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.util.Timeout
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.schemas.events.user.v2.User
import com.blinkbox.books.schemas.events.user.v2.UserId
import org.joda.money.Money
import org.joda.money.CurrencyUnit
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.FlatSpec
import org.scalatest.StreamlinedXmlEquality
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._
import scala.xml._

class EventSendersTest extends FlatSpec with MockitoSugar {

  val offer = "test-offer"
  val user = User(UserId(42), "bob@blinkbox.com", "Bob", "Builder")
  val creditedValue = BigDecimal(4.20)
  val creditAmount = Money.of(CurrencyUnit.GBP, creditedValue.bigDecimal)
  val timestamp = DateTime.now

  trait SenderFixture {
    val eventSender1 = mock[EventSender]
    val eventSender2 = mock[EventSender]
    val eventSender3 = mock[EventSender]
    val eventSenders = Seq(eventSender1, eventSender2, eventSender3)
  }

  class PublisherFixture extends TestKit(ActorSystem("test-system")) with ImplicitSender with StreamlinedXmlEquality {
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5.seconds)
    val publisher = TestProbe()
  }

  "A compound sender" should "do nothing when empty" in new SenderFixture {
    new CompoundEventSender().sendEvent(user, creditAmount, timestamp, offer)
  }

  it should "pass on event to all delegates" in new SenderFixture {
    new CompoundEventSender(eventSenders: _*).sendEvent(user, creditAmount, timestamp, offer)
    eventSenders.foreach(sender => verify(sender).sendEvent(user, creditAmount, timestamp, offer))
  }

  it should "pass on event to all other delegates when one fails" in new SenderFixture {
    val ex = new RuntimeException("Test exception")
    doThrow(ex).when(eventSender2).sendEvent(any[User], any[Money], any[DateTime], anyString)

    new CompoundEventSender(eventSenders: _*).sendEvent(user, creditAmount, timestamp, offer)

    eventSenders.foreach(sender => verify(sender).sendEvent(user, creditAmount, timestamp, offer))
  }

  "A reporting event sender" should "publish 'user credit' events on its output" in new PublisherFixture {
    // Send a message.
    val sender = new ReportingEventSender(publisher.ref)
    sender.sendEvent(user, creditAmount, timestamp, offer)

    // Wait for the output.
    val published = publisher.expectMsgType[Event](3.seconds)

    // Check the content of the published message.
    val (_, eventUser, eventAmount, eventCurrency, eventReason) = published.body match {
      case User.Credited(timestamp, user, amount, currency, reason) => (timestamp, user, amount, currency, reason)
    }
    assert(eventUser == user && eventAmount == BigDecimal(creditAmount.getAmount) &&
      eventCurrency == creditAmount.getCurrencyUnit.getCurrencyCode && eventReason == offer)
  }

  "An Exact Target event sender" should "publish 'send email' events on its output" in new PublisherFixture {
    val testTemplate = "test_template"
    val sender = new EmailEventSender(publisher.ref, testTemplate)
    sender.sendEvent(user, creditAmount, timestamp, offer)

    val published = publisher.expectMsgType[Event](3.seconds)

    published.body match {
      case Email.Send(timestamp, recipient, templateName, attributes) =>
        (timestamp, recipient, templateName, attributes)
        assert(recipient.emailAddress == user.username &&
          recipient.id == user.id.value.toString &&
          templateName == testTemplate)
        assert(attributes == Map("firstName" -> user.firstName, "lastName" -> user.lastName))
    }
  }

  "A Mailer event sender" should "publish events in the old Mailer XML format on its output" in new PublisherFixture {
    val sender = new MailerEventSender(publisher.ref, "test_template", "test instance")
    sender.sendEvent(user, creditAmount, timestamp, offer)

    val published = publisher.expectMsgType[Event](3.seconds)

    val xml = XML.loadString(published.body.asString)
    assert(xml === expectedXml, "Should produce the expected XML")
  }

  private val expectedXml =
    <sendEmail r:messageId={ "user-42-credited-test-offer" } r:instance="test instance" r:originator="bookStore" xmlns="http://schemas.blinkbox.com/books/emails/sending/v1" xmlns:r="http://schemas.blinkbox.com/books/routing/v1">
      <template>test_template</template>
      <to>
        <recipient>
          <name>Bob</name>
          <email>bob@blinkbox.com</email>
        </recipient>
      </to>
      <templateVariables>
        <templateVariable>
          <key>salutation</key>
          <value>Bob</value>
        </templateVariable>
        <templateVariable>
          <key>amountCredited</key>
          <value>4.20</value>
        </templateVariable>
      </templateVariables>
    </sendEmail>

}
