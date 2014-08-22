# Change log

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


