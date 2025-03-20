Feature: Recovery of orders in Created state
  As a system administrator
  I want orders in Created state to be properly recovered
  So that the system can maintain consistent state after restart

  Background:
    Given the system is in recovery mode
    And the transaction logs are available

  Scenario: Order in Created state is rebuilt
    Given an order with ID "ORD-123" exists in transaction logs
    And the order has state "Created" in RECALL/TICKET/HISTORY topic
    When the recovery process runs for this order
    Then the CreatedStateStrategy should handle the order
    And the recovery action should be REBUILD
    And the order should be rebuilt in the cache with state "New"
    And no DFD request should be sent

  Scenario: Order in Created state with request and response entries is rebuilt
    Given an order with ID "ORD-456" exists in transaction logs
    And the order has state "Created" in RECALL/TICKET/HISTORY topic
    And the order has state "New" in RECALL/TO/OMS topic
    And the order has state "New" in OMS/TO/RECALL topic
    When the recovery process runs for this order
    Then the CreatedStateStrategy should handle the order
    And the recovery action should be REBUILD
    And the order should be rebuilt in the cache with state "New"
    And no DFD request should be sent
