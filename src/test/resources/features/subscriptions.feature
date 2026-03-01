Feature: Subscriptions
  As a subscriber
  I want to subscribe to topics and receive news
  So that I can get updates on topics I care about

  Background:
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    And I have negotiated protocol version "3.0" with CorrelationId 1

  Scenario: Subscribe to valid topics and receive SubscribeResponse
    When I send a SubscribeRequest with topics "sport, weather" and CorrelationId 10
    Then I should receive a SubscribeResponse within 1 second
    And the SubscribeResponse should contain topic "sport"
    And the SubscribeResponse should contain topic "weather"
    And the SubscribeResponse CorrelationId should be 10

  Scenario: Subscribing with invalid topics results in filtered response
    When I send a SubscribeRequest with topics "invalidtopic123" and CorrelationId 11
    Then I should receive a SubscribeResponse within 1 second
    And the SubscribeResponse Topics list should not contain "invalidtopic123"

  Scenario: Unsubscribing stops notifications
    When I send a SubscribeRequest with topics "sport" and CorrelationId 12
    And I receive the SubscribeResponse
    And I wait for at least one Notify message
    And I send an Unsubscribe message
    Then I should not receive any new Notify messages for 4 seconds

  Scenario: Starting new subscription replaces the previous one
    When I send a SubscribeRequest with topics "sport" and CorrelationId 13
    And I receive the SubscribeResponse
    When I send a SubscribeRequest with topics "weather" and CorrelationId 14
    Then I should receive a SubscribeResponse within 1 second
    And the SubscribeResponse should contain topic "weather"
    And the SubscribeResponse CorrelationId should be 14
