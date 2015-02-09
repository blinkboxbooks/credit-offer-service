Credit Offer Service
====================

This is a message-driven service that listens for events that qualify for users to receive additional account credit, and adds credits to user accounts accordingly.

Currently, this service only listens for Hudl 2 device registrations and credits a users account upon registering that specific device, for users that haven't already received this offer.

## Build and run

The resource server builds as a standalone Jar file using `sbt`.

It uses the common Blinkbox Books conventions and approaches to configuration, metrics, health endpoints etc., see [the common-config library](https://github.com/blinkboxbooks/common-config.scala) for details.


## Configuration

See the [application.conf](/src/main/resources/application.conf) file for properties that need to be provided, and [reference.conf](/src/main/resources/reference.conf) for settings that can optionally be overridden.

In order to be able to add credit to user accounts, the service needs to be configured with credentials (username and role) that has the necessary privilieges. We assume the username in this case is `credit-offer-service@blinkbox.com`, and this needs to be configured with the `CSR` role.

The service will interact with the Blinkbox Books authentication service ("Zuul") in order to get the necessary tokens for making requests on the account credit service.

## Messaging

The service connects to RabbitMQ to receive and publish events.

The service subscribes to events from the `Events` exchange, and stores these in the `Credit.Offer.DeviceRegistration` and `Credit.Offer.DeviceRegistration.DLQ` queues, which are automatically declared.

It publishes messages to the `Mail.Sender.Exchange` and `Agora` exchanges.

## Database

As the current account credit service doesn't allow querying for past transactions of a given type, this service has to track the state of allocated offers locally.

The schema for the database are held within the `schema` directory.
