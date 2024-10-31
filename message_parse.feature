  // message_parser.feature
Feature: Message Parser Tests with Order Class
  Test MessageParser functionality for Order messages in different formats

  Background:
    Given a message parser is initialized for Order class

  Scenario: Parse JSON Order message successfully
    Given a JSON Order message:
      """
      {
        "orderID": "ORD001",
        "currentState": "New",
        "account": "ACC123",
        "ordQty": 1000,
        "orderType": "1",
        "onBehalfOfCompID": "TRADING",
        "tradeAllocIndicator": 0,
        "riskFillCategory": "Parent",
        "bookingReportingInstruction": "I-AB",
        "senderCompID": "CBRECALL",
        "senderSubID": "CB",
        "targetCompID": "CBMSRECALL",
        "targetSubID": "SWAP",
        "symbol": "AAPL",
        "side": "1",
        "currency": "USD"
      }
      """
    When the message is parsed as JSON
    Then the parsed Order should have orderID "ORD001"
    And the parsed Order should have state "New"
    And the parsed Order should have quantity 1000
    And the parsed Order should have account "ACC123"

  Scenario: Parse NVFIX Order message successfully
    Given a NVFIX Order message:
      """
      11=ORD001\u000138=1000\u00011=ACC123\u000140=1\u0001115=TRADING\u0001826=0\u000173980=Parent\u000111555=I-AB\u000149=CBRECALL\u000150=CB\u000156=CBMSRECALL\u000157=SWAP\u000155=AAPL\u000154=1\u000115=USD\u0001
      """
    When the message is parsed as NVFIX
    Then the parsed Order should have orderID "ORD001"
    And the parsed Order should have quantity 1000
    And the parsed Order should have account "ACC123"
    And the parsed Order should have orderType "1"

  Scenario: Convert Order to JSON string
    Given an Order with values:
      | orderID              | ORD001    |
      | account             | ACC123    |
      | ordQty             | 1000      |
      | orderType          | 1         |
      | onBehalfOfCompID   | TRADING   |
      | tradeAllocIndicator| 0         |
      | riskFillCategory   | Parent    |
      | currentState       | New       |
    When the Order is converted to JSON string
    Then the JSON string should contain "orderID":"ORD001"
    And the JSON string should contain "account":"ACC123"
    And the JSON string should contain "ordQty":1000
    And the JSON string should contain "currentState":"New"

  Scenario: Convert Order to NVFIX string
    Given an Order with values:
      | orderID              | ORD001    |
      | account             | ACC123    |
      | ordQty             | 1000      |
      | orderType          | 1         |
      | onBehalfOfCompID   | TRADING   |
      | tradeAllocIndicator| 0         |
    When the Order is converted to NVFIX string
    Then the NVFIX string should contain "11=ORD001"
    And the NVFIX string should contain "1=ACC123"
    And the NVFIX string should contain "38=1000"
    And the NVFIX string should contain "40=1"

  Scenario: Handle invalid JSON Order message
    Given an invalid JSON Order message:
      """
      {
        "orderID": "ORD001",
        "account": "ACC123",
        ordQty: 1000,
      """
    When the message is parsed as JSON
    Then a ParsingErrorException should be thrown

  Scenario: Handle invalid NVFIX Order message
    Given an invalid NVFIX Order message "11=ORD001\u00011=ACC123\u0001invalid\u0001"
    When the message is parsed as NVFIX
    Then a ParsingErrorException should be thrown

// MessageParserSteps.java
@SpringBootTest
public class MessageParserSteps {
    private MessageParser<Order> jsonParser;
    private MessageParser<Order> nvfixParser;
    private String inputMessage;
    private Order parsedOrder;
    private String outputMessage;
    private Exception thrownException;

    @Before
    public void setup() {
        jsonParser = new MessageParser<>(MessageType.JSON, Order.class);
        nvfixParser = new MessageParser<>(MessageType.NVFIX, Order.class);
    }

    @Given("a message parser is initialized for Order class")
    public void messageParserIsInitialized() {
        assertNotNull(jsonParser);
        assertNotNull(nvfixParser);
    }

    @Given("a JSON Order message:")
    public void givenJsonOrderMessage(String message) {
        inputMessage = message;
    }

    @Given("a NVFIX Order message:")
    public void givenNvfixOrderMessage(String message) {
        inputMessage = message;
    }

    @Given("an Order with values:")
    public void givenOrder(Map<String, String> values) {
        Order order = new Order();
        order.setOrderID(values.get("orderID"));
        order.setAccount(values.get("account"));
        order.setOrdQty(Long.parseLong(values.get("ordQty")));
        order.setOrderType(values.get("orderType"));
        order.setOnBehalfOfCompID(values.get("onBehalfOfCompID"));
        order.setTradeAllocIndicator(Integer.parseInt(values.get("tradeAllocIndicator")));
        if (values.containsKey("currentState")) {
            order.setCurrentState(OrderState.valueOf(values.get("currentState")));
        }
        if (values.containsKey("riskFillCategory")) {
            order.setRiskFillCategory(values.get("riskFillCategory"));
        }
        parsedOrder = order;
    }

    @Given("an invalid JSON Order message:")
    public void givenInvalidJsonOrderMessage(String message) {
        inputMessage = message;
    }

    @Given("an invalid NVFIX Order message {string}")
    public void givenInvalidNvfixOrderMessage(String message) {
        inputMessage = message;
    }

    @When("the message is parsed as JSON")
    public void parseJsonMessage() {
        try {
            parsedOrder = jsonParser.parse(inputMessage);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the message is parsed as NVFIX")
    public void parseNvfixMessage() {
        try {
            parsedOrder = nvfixParser.parse(inputMessage);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("the Order is converted to JSON string")
    public void convertToJsonString() {
        outputMessage = jsonParser.toString(parsedOrder);
    }

    @When("the Order is converted to NVFIX string")
    public void convertToNvfixString() {
        outputMessage = nvfixParser.toString(parsedOrder);
    }

    @Then("the parsed Order should have orderID {string}")
    public void verifyParsedOrderId(String expectedId) {
        assertEquals(expectedId, parsedOrder.getOrderID());
    }

    @Then("the parsed Order should have state {string}")
    public void verifyParsedOrderState(String expectedState) {
        assertEquals(OrderState.valueOf(expectedState), parsedOrder.getCurrentState());
    }

    @Then("the parsed Order should have quantity {int}")
    public void verifyParsedOrderQuantity(int expectedQuantity) {
        assertEquals(expectedQuantity, parsedOrder.getOrdQty());
    }

    @Then("the parsed Order should have account {string}")
    public void verifyParsedOrderAccount(String expectedAccount) {
        assertEquals(expectedAccount, parsedOrder.getAccount());
    }

    @Then("the parsed Order should have orderType {string}")
    public void verifyParsedOrderType(String expectedType) {
        assertEquals(expectedType, parsedOrder.getOrderType());
    }

    @Then("the JSON string should contain {string}")
    public void verifyJsonStringContains(String expectedContent) {
        assertTrue(outputMessage.contains(expectedContent));
    }

    @Then("the NVFIX string should contain {string}")
    public void verifyNvfixStringContains(String expectedContent) {
        assertTrue(outputMessage.contains(expectedContent));
    }

    @Then("a ParsingErrorException should be thrown")
    public void verifyParsingErrorException() {
        assertNotNull(thrownException);
        assertTrue(thrownException instanceof ParsingErrorException);
    }
}
