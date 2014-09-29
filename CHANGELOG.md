# Change log

## 0.1.15 ([#39](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/39) 2014-09-29 15:11:59)

Fixed failing build

### Bugfix

- Fixed builds failing because of logger initialisation timeouts

## 0.1.14 ([#37](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/37) 2014-09-25 15:19:28)

Fixed Akka logging

### Bugfix

- Akka log messages now go to Graylog ([CP-1879](http://jira.blinkbox.local/jira/browse/CP-1879))

## 0.1.13 ([#38](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/38) 2014-09-26 10:20:23)

Bump common-config to support substitution

### Patch

Bump config library

## 0.1.12 ([#36](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/36) 2014-09-10 15:08:24)

CP-1774  Update RabbitMQ library.

### Improvements

- Retry connecting to RabbitMQ on authentication error (fix in upstream library).


## 0.1.11 ([#35](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/35) 2014-09-01 08:40:54)

Updated tests to use failingWith from common-test

### Improvement

- Updated tests to use `failingWith` from common-test library instead of using a variant of it defined in this project.

## 0.1.10 ([#34](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/34) 2014-08-29 14:17:07)

Bugfix: Logging self-type in AuthRetry trait 

### Bugfix

- Changed the self-type of `AuthRetry` trait to `StrictLogging`. For some reason using `Logging` caused `scala.NotImplementedError` errors at runtime.

## 0.1.9 ([#33](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/33) 2014-08-29 13:30:01)

Use Mailer by default instead of Exact Target.

### Improvements.

- Switch to use Mailer by default for sending emails, instead of Exact Target.


## 0.1.8 ([#30](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/30) 2014-08-28 15:51:09)

Missing the udp port for gelf

Patch to add logging port for gelf

## 0.1.7 ([#28](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/28) 2014-08-26 10:32:35)

Acceptance tests

Test Improvement: Adding acceptance tests for credit-offer-service. 


## 0.1.6 ([#32](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/32) 2014-08-29 11:49:04)

Fixed content-type for email message.

### Bug fix:

- Fixed content type for Exact Target email message to match latest schema.


## 0.1.5 ([#31](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/31) 2014-08-28 17:12:56)

CP-1780 Handle ConnectionExceptions

### Bug fix:

- Correctly handle Spray ConnectionExceptions, translating them to the exceptions specified in the client API.


## 0.1.4 ([#29](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/29) 2014-08-27 09:26:02)

No `id` in table and primary keys are `user_id` and `promotion_id`

A patch to make the database better

## 0.1.3 ([#27](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/27) 2014-08-22 16:01:22)

Admin account credit service check

### Improvement

- `DeviceRegistrationHandler` retrieves user's account credit balance before granting an offer and retries processing the event if that operation fails.

## 0.1.2 ([#26](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/26) 2014-08-22 15:07:07)

Update RabbitMQ library

### Improvements

- Got latest rabbitmq-ha library version to get bugfix for cancelling timeouts.


## 0.1.1 ([#25](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/25) 2014-08-22 13:30:20)

Bugfix: retry processing the event when experiencing connectivity issues

### Bugfix

- `isTemporaryFailure()` now checks correct type of `ConnectionException`.

## 0.1.0 ([#22](https://git.mobcastdev.com/Hermes/credit-offer-service/pull/22) 2014-08-20 15:48:03)

First version of Hudl 2 credit offer service

### New features

First version of the Hudl 2 credit offer service which:

- Consumes device registration events.
- Gives users £10 credit if they haven't been given this offer before.
- Sends events to trigger emails (via Mailer or Exact Target), and for reporting.


