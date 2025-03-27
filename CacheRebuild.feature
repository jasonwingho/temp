Feature: Rebuild Cache Functionality

  Background: 
    Given the application is starting up
    And AMPS clients are configured for the following topics:
      | Topic Name       | Message Type |
      | RECALL/TICKET    | JSON         |
      | RECALL/TO/OMS    | NVFIX        |
      | OMS/TO/RECALL    | NVFIX        |

  Scenario: Successfully rebuild cache from transaction logs with all necessary data
    Given transaction logs exist for order "ORD-001" in the following topics:
      | Topic          | Message Type     | State          | Timestamp            |
      | RECALL/TICKET  | RecallTicket     | New            | 2025-01-01T10:00:00Z |
      | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T10:01:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | New            | 2025-01-01T10:02:00Z |
      | RECALL/TO/OMS  | Order            | PendingFill    | 2025-01-01T10:03:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | PartiallyFilled| 2025-01-01T10:04:00Z |
    And bookmark timestamps are:
      | Topic         | Timestamp             |
      | RECALL/TICKET | 2025-01-01T10:05:00Z  |
      | OMS/TO/RECALL | 2025-01-01T10:05:00Z  |
    When the cache initialization process runs
    Then the cache should contain a RecallTicket for "ORD-001" with state "PartiallyFilled"
    And the cache should contain an Order for "ORD-001" with state "PartiallyFilled"
    And the recovery action determined should be "REBUILD"
    And no messages should be published to any topic

  Scenario: Rebuild cache with inconsistent state requiring republish
    Given transaction logs exist for order "ORD-002" in the following topics:
      | Topic          | Message Type     | State          | Timestamp            |
      | RECALL/TICKET  | RecallTicket     | New            | 2025-01-01T10:00:00Z |
      | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T10:01:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | New            | 2025-01-01T10:02:00Z |
      | RECALL/TO/OMS  | Order            | PendingFill    | 2025-01-01T10:03:00Z |
    And bookmark timestamps are:
      | Topic         | Timestamp             |
      | RECALL/TICKET | 2025-01-01T09:59:00Z  |
      | OMS/TO/RECALL | 2025-01-01T10:05:00Z  |
    When the cache initialization process runs
    Then the cache should contain a RecallTicket for "ORD-002" with state "New"
    And the cache should contain an Order for "ORD-002" with state "PendingFill"
    And the recovery action determined should be "REPUBLISH"
    And a message should be published to topic "RECALL/TICKET" for order "ORD-002" with state "PendingFill"

  Scenario: Rebuild cache with orders in monitored states requiring timeout monitoring
    Given transaction logs exist for order "ORD-003" in the following topics:
      | Topic          | Message Type     | State          | Timestamp            |
      | RECALL/TICKET  | RecallTicket     | PendingNew     | 2025-01-01T10:00:00Z |
      | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T10:01:00Z |
    When the cache initialization process runs
    Then the cache should contain a RecallTicket for "ORD-003" with state "PendingNew"
    And the cache should contain an Order for "ORD-003" with state "PendingNew"
    And a timeout task should be scheduled for order "ORD-003"

  Scenario: Rebuild cache for order in terminal state
    Given transaction logs exist for order "ORD-004" in the following topics:
      | Topic          | Message Type     | State          | Timestamp            |
      | RECALL/TICKET  | RecallTicket     | Filled         | 2025-01-01T10:00:00Z |
      | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T09:50:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | New            | 2025-01-01T09:51:00Z |
      | RECALL/TO/OMS  | Order            | PendingFill    | 2025-01-01T09:52:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | Filled         | 2025-01-01T09:53:00Z |
    When the cache initialization process runs
    Then the cache should contain a RecallTicket for "ORD-004" with state "Filled"
    And the cache should contain an Order for "ORD-004" with state "Filled"
    And no timeout task should be scheduled for order "ORD-004"
    And the recovery action determined should be "IGNORE"

  Scenario: Rebuild cache handling discarded entries based on bookmark timestamps
    Given transaction logs exist for order "ORD-005" in the following topics:
      | Topic          | Message Type     | State          | Timestamp            |
      | RECALL/TICKET  | RecallTicket     | New            | 2025-01-01T10:00:00Z |
      | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T10:01:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | New            | 2025-01-01T10:02:00Z |
      | RECALL/TO/OMS  | Order            | PendingFill    | 2025-01-01T10:08:00Z |
      | OMS/TO/RECALL  | ExecutionReport  | PartiallyFilled| 2025-01-01T10:09:00Z |
    And bookmark timestamps are:
      | Topic         | Timestamp             |
      | RECALL/TICKET | 2025-01-01T10:05:00Z  |
      | OMS/TO/RECALL | 2025-01-01T10:05:00Z  |
    When the cache initialization process runs
    Then the cache should contain a RecallTicket for "ORD-005" with state "New"
    And the cache should contain an Order for "ORD-005" with state "New"
    And 2 entries should be discarded as newer than the bookmark timestamps

  Scenario: Handle recovery when no transaction logs exist
    When the cache initialization process runs
    Then the cache should be empty
    And no recovery actions should be performed
    And a warning message should be logged

  Scenario: Handle recovery with missing topics
    Given AMPS client for topic "RECALL/TICKET" is unavailable
    When the cache initialization process runs
    Then the initialization should wait for up to 15 minutes for all topics
    And a warning message should be logged
    And the application should continue with partial transaction logs

  Scenario: Handle multiple orders with different states
    Given transaction logs exist for multiple orders:
      | Order ID | Topic          | Message Type     | State          | Timestamp            |
      | ORD-A    | RECALL/TICKET  | RecallTicket     | New            | 2025-01-01T10:00:00Z |
      | ORD-A    | RECALL/TO/OMS  | Order            | PendingNew     | 2025-01-01T10:01:00Z |
      | ORD-A    | OMS/TO/RECALL  | ExecutionReport  | New            | 2025-01-01T10:02:00Z |
      | ORD-B    | RECALL/TICKET  | RecallTicket     | Canceled       | 2025-01-01T09:00:00Z |
      | ORD-B    | RECALL/TO/OMS  | Order            | PendingCancel  | 2025-01-01T08:55:00Z |
      | ORD-B    | OMS/TO/RECALL  | ExecutionReport  | Canceled       | 2025-01-01T08:56:00Z |
      | ORD-C    | RECALL/TICKET  | RecallTicket     | PendingFill    | 2025-01-01T11:00:00Z |
      | ORD-C    | RECALL/TO/OMS  | Order            | PendingFill    | 2025-01-01T11:01:00Z |
    When the cache initialization process runs
    Then the cache should contain 3 orders
    And the cache should contain 3 recall tickets
    And a timeout task should be scheduled only for order "ORD-C"
