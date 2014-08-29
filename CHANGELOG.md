# Change log

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


