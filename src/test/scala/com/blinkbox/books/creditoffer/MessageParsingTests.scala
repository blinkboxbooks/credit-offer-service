package com.blinkbox.books.creditoffer

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class MessageParsingTests extends FunSuite {

  test("Parses valid device registration message") {
    val validXML =
      <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
                  xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
                  xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
                  r:originator="zuul"
                  v:version="1.0">

        <userId>123</userId>
        <timestamp>2013-12-30T19:15:23Z</timestamp>

        <client>
          <id>19384</id>
          <name>My New Phone</name>
          <brand>Apple</brand>
          <model>iPhone 5S</model>
          <os>iOS 7</os>
        </client>
      </registered>

    val parsedMsg = DeviceRegistrationEvent.fromXML(validXML.toString().getBytes)

    assert(parsedMsg.userId == 123)
    assert(parsedMsg.timestamp == DateTime.parse("2013-12-30T19:15:23Z"))
    assert(parsedMsg.device == DeviceDetails(19384, "My New Phone", "Apple", "iPhone 5S", "iOS 7"))
  }

  test("Throws exception when parsing invalid device registration message") {
    val invalidXML =
      <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
                  xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
                  xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
                  r:originator="zuul"
                  v:version="1.0">
        <userId>123</userId>
      </registered>

   val ex = intercept[IllegalArgumentException] {
     DeviceRegistrationEvent.fromXML(invalidXML.toString().getBytes)
   }
   assert(ex.getCause.isInstanceOf[SAXParseException])
  }
}