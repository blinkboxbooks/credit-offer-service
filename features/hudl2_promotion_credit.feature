Feature: Hudl2 Promotion Service
  As a Marketing Director
  I want users who register a Hudl2 to receive promotional credit of £10
  So that users are incentivised to use blinkbox books on a Hudl2

  @one
  Scenario: Existing user registers a Hudl2
    Given an existing user registers a Hudl2
    When the event is processed
    Then the user has £10 credited to their account
    And the user receives an e-mail confirming that their account has been deposited with £10 credit
    And the credit event was reported

  Scenario: User registers two Hudl2s to their account
    Given an existing user has registered a Hudl2
    And the user registers another Hudl2
    When the event is processed
    Then the user does not receive any additional credit
    And the user does not receive an e-mail about promotional credit
    And the credit event was not reported

  Scenario: Promotion limit already met
    Given the amount of credit given out has met the promotion limit
    And an existing user registers a Hudl2
    When the event is processed
    Then the user does not receive any promotional credit
    And the user does not receive an e-mail about promotional credit
    And the credit event was not reported

  @negative
  Scenario: User registers a Hudl1
    Given an existing user registers a Hudl1
    When the event is processed
    Then the user does not receive any additional credit
    And the user does not receive an e-mail about promotional credit
    And the credit event was not reported

  @negative
  Scenario: Event message comes in an incorrect format
    Given a user triggers a Hudl2 registration event in an invalid format
    When the event is processed
    Then the user does not receive any promotional credit
    And the user does not receive an e-mail about promotional credit
    And the event message is stored for later processing

  @negative
  Scenario: Event message with missing mandatory fields
    Given a user triggers a Hudl2 registration event with missing mandatory information
    When the event is processed
    Then the user does not receive any promotional credit
    And the user does not receive an e-mail about promotional credit
    And the event message is stored for later processing

#  @manual @edge_case
#  Scenario: account credit service unavailable
#    Given the account credit service is unavailable
#    When an event is received for processing
#    Then the account credit service is retried until available
#    And the original message should not be placed on the DLQ
#
#  @manual @edge_case
#  Scenario: account credit service is returns user unknown
#    Given account credit service does not know the user in the event
#    When the event is sent for processing
#    Then the message is placed on the DLQ
#
#  @manual @edge_case
#  Scenario: agora exchange unavailable
#    Given reporting service is unavailable
#    When the event is sent for processing
#    Then the message is placed on the DLQ
#
#  @manual @edge_case
#  Scenario: credit offer queue unavailable
#    Given exact target queue is unavailable
#    Then the promotion service continues trying to connect
