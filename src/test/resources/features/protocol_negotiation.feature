Feature: Protocol Negotiation
  As a subscriber
  I want to negotiate a protocol version with the NewsFeed application
  So that I can establish a session

  Scenario: Successful protocol negotiation with version 3.0
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "3.0" and CorrelationId 1
    Then I should receive a ProtocolResponse within 1 second
    And the ProtocolResponse should have ProtocolVersion "3.0"
    And the ProtocolResponse CorrelationId should be 1

  Scenario: Protocol negotiation with unsupported version
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "9.9" and CorrelationId 2
    Then I should receive a ProtocolResponse within 1 second
    And the ProtocolResponse should have ProtocolVersion ""

  Scenario: First supported version is selected when multiple are offered
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "9.9, 2.0, 1.0" and CorrelationId 3
    Then I should receive a ProtocolResponse within 1 second
    And the ProtocolResponse should have ProtocolVersion "2.0"
    And the ProtocolResponse CorrelationId should be 3

  Scenario: CorrelationId is correctly echoed in ProtocolResponse
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "3.0" and CorrelationId 42
    Then I should receive a ProtocolResponse within 1 second
    And the ProtocolResponse CorrelationId should be 42

  Scenario: AvailableTopics is sent after version 2.0 negotiation
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "2.0" and CorrelationId 5
    Then I should receive a ProtocolResponse within 1 second
    And I should receive an AvailableTopics message

  Scenario: AvailableTopics contains a non-empty list of topics
    Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
    When I send a ProtocolRequest with versions "2.0" and CorrelationId 6
    And I receive the ProtocolResponse
    Then I should receive an AvailableTopics message with a non-empty Topics list
