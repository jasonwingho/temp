Feature: Message Parser Tests
  As a developer
  I want to parse and format Order messages
  So that I can handle different message formats correctly

  Background:
    Given a message parser is initialized for Order class

  # JSON scenarios remain the same since they don't use the field separator
  Scenario: Parse simple JSON Order message
    Given a JSON Order message:
    """
    {
      "orderID": "ORD001",
      "account": "ACC123",
      "ordQty": 1000,
      "orderType": "1",
      "onBehalfOfCompID": "TRADING"
    }
    """
    When the message is parsed as JSON
    Then the parsed Order should have orderID "ORD001"
    And the parsed Order should have quantity 1000
    And the parsed Order should have account "ACC123"

  Scenario: Parse simple NVFIX Order message
    Given a NVFIX Order message:
    """
    11=ORD001␁1=ACC123␁38=1000␁40=1␁115=TRADING␁49=CBRECALL␁50=CB␁56=CBMSRECALL␁57=SWAP␁
    """
    When the message is parsed as NVFIX
    Then the parsed Order should have orderID "ORD001"
    And the parsed Order should have account "ACC123"
    And the parsed Order should have quantity 1000

  Scenario: Format Order to NVFIX
    Given an Order with values:
      | orderID              | ORD001    |
      | account             | ACC123    |
      | ordQty             | 1000      |
      | orderType          | 1         |
      | onBehalfOfCompID   | TRADING   |
      | senderCompID       | CBRECALL  |
      | senderSubID        | CB        |
      | targetCompID       | CBMSRECALL|
      | targetSubID        | SWAP      |
    When the Order is converted to NVFIX string
    Then the NVFIX string should contain "11=ORD001"
    And the NVFIX string should contain "1=ACC123"
    And the NVFIX string should contain "38=1000"
    And the NVFIX string should contain "40=1"
    And the NVFIX string should contain "115=TRADING"

  Scenario: Handle invalid NVFIX message
    Given a NVFIX Order message "11=ORD001␁INVALID␁1=ACC123"
    When the message is parsed as NVFIX
    Then a ParsingErrorException should be thrown
