Feature: Byte Parsing Utility
  As a developer
  I want to parse different data types from byte arrays
  So that I can process raw data formats efficiently

  Scenario Outline: Parse boolean values
    When I parse boolean value "<input>"
    Then the boolean result should be <expected>

    Examples:
      | input | expected |
      | Y     | true     |
      | N     | false    |

  Scenario Outline: Parse valid integer values
    When I parse integer value "<input>"
    Then the integer result should be <expected>

    Examples:
      | input | expected |
      | 123   | 123      |
      | 0     | 0        |
      | -123  | -123     |
      | -1    | -1       |

  Scenario Outline: Parse invalid integer values
    When I attempt to parse invalid integer "<input>"
    Then a ParseException should be thrown

    Examples:
      | input |
      | 12a34 |
      | -     |
      | abc   |

  Scenario Outline: Parse valid long values
    When I parse long value "<input>"
    Then the long result should be <expected>

    Examples:
      | input | expected |
      | 123   | 123      |
      | 0     | 0        |
      | -123  | -123     |
      | -1    | -1       |
      | 9223372036854775807 | 9223372036854775807 |

  Scenario Outline: Parse invalid long values
    When I attempt to parse invalid long "<input>"
    Then a ParseException should be thrown

    Examples:
      | input |
      | 12a34 |
      | -     |
      | abc   |

  Scenario Outline: Parse valid double values
    When I parse double value "<input>"
    Then the double result should be <expected> with delta <delta>

    Examples:
      | input    | expected | delta  |
      | 123      | 123.0    | 0.0001 |
      | 0        | 0.0      | 0.0001 |
      | 123.45   | 123.45   | 0.0001 |
      | 0.123    | 0.123    | 0.0001 |
      | -123.45  | -123.45  | 0.0001 |
      | -0.123   | -0.123   | 0.0001 |
      | 1.23E2   | 123.0    | 0.0001 |
      | 1.23E-2  | 0.0123   | 0.0001 |
      | 1.23E30  | 1.23E30  | 1.0E25 |
      | 1.23E-30 | 1.23E-30 | 1.0E-35|

  Scenario Outline: Parse double values with whitespace
    When I parse double value "<input>"
    Then the double result should be <expected> with delta <delta>

    Examples:
      | input     | expected | delta  |
      | 123.45    | 123.45   | 0.0001 |
      |  123.45   | 123.45   | 0.0001 |
      | 123.45    | 123.45   | 0.0001 |

  Scenario Outline: Parse invalid double values
    When I attempt to parse invalid double "<input>"
    Then a ParseException should be thrown

    Examples:
      | input  |
      | 12.3x4 |
      | -      |
      | 1.23E  |
      | abc    |

  Scenario: Parse double with high precision
    When I parse double value "1.123456789"
    Then the double result should be 1.123456789 with delta 0.000000001
