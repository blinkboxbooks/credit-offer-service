package com.blinkbox.books.creditoffer

import java.io.ByteArrayInputStream
import javax.xml.transform.stream.StreamSource
import org.joda.time.DateTime
import scala.xml.{Node, XML}

import com.blinkbox.books.messaging.Xml.NodeSeqWrapper
import com.blinkbox.books.messaging.Xml.validatorFor

case class DeviceDetails(id: Int, name: String, brand: String, model: String, os: String)

object DeviceDetails {
  def fromXML(device: Node): DeviceDetails =
    if (device.size == 0) throw new IllegalArgumentException(s"Cannot parse device details from empty node")
    else DeviceDetails(
      device.stringValue("id").toInt,
      device.stringValue("name"),
      device.stringValue("brand"),
      device.stringValue("model"),
      device.stringValue("os")
    )
}

case class DeviceRegistrationEvent(userId: Int, timestamp: DateTime, device: DeviceDetails)

object DeviceRegistrationEvent {
  def fromXML(bytes: Array[Byte]): DeviceRegistrationEvent = {
    try {
      val validator = validatorFor("/routing.xsd", "/versioning.xsd", "/clients.xsd")
      validator.validate(new StreamSource(new ByteArrayInputStream(bytes)))
    } catch {
      case ex: Throwable => throw new IllegalArgumentException(s"Invalid XML message: ${new String(bytes)}", ex)
    }
    val xml = XML.load(new ByteArrayInputStream(bytes))
    DeviceRegistrationEvent(
      xml.stringValue("userId").toInt,
      xml.dateTimeValue("timestamp"),
      DeviceDetails.fromXML((xml \ "client")(0))
    )
  }
}