
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ByteParseUtil {
    private static final int DECIMAL_MAX_LENGTH = 9;

    /**
     * Parses a boolean from a byte array. 'Y' is true, anything else is false.
     */
    public static boolean parseBoolean(byte[] buffer, int valStart, int vallen) {
        validateInput(buffer, valStart, vallen);
        return buffer[valStart] == 'Y';
    }

    /**
     * Parses an integer from a byte array.
     */
    public static int parseInt(byte[] b, int idx, int len) throws ParseException {
        // Input validation
        validateInput(b, idx, len);

        int to = idx + len;
        boolean isNegative = false;
        
        // Handle sign
        if (b[idx] == '-') {
            isNegative = true;
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after minus sign", idx);
            }
        } else if (b[idx] == '+') {
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after plus sign", idx);
            }
        }

        int value = 0;
        while (idx < to) {
            int prevValue = value;
            value *= 10;
            
            // Check for overflow
            if (value / 10 != prevValue) {
                throw new ParseException("Integer overflow", idx);
            }
            
            byte digit = b[idx++];
            if (digit < '0' || digit > '9') {
                throw new ParseException("Invalid character in number", idx - 1);
            }
            
            value += (digit - '0');
            // Check for overflow after addition
            if (value < 0) {
                throw new ParseException("Integer overflow", idx);
            }
        }
        
        if (isNegative) {
            if (value == Integer.MIN_VALUE) {
                return value;
            }
            value = -value;
            if (value > 0) {
                throw new ParseException("Integer overflow", idx);
            }
        }
        
        return value;
    }

    /**
     * Parses a long from a byte array.
     */
    public static long parseLong(byte[] b, int idx, int len) throws ParseException {
        validateInput(b, idx, len);

        int to = idx + len;
        boolean isNegative = false;
        
        // Handle sign
        if (b[idx] == '-') {
            isNegative = true;
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after minus sign", idx);
            }
        } else if (b[idx] == '+') {
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after plus sign", idx);
            }
        }

        long value = 0;
        while (idx < to) {
            long prevValue = value;
            value *= 10;
            
            // Check for overflow
            if (value / 10 != prevValue) {
                throw new ParseException("Long overflow", idx);
            }
            
            byte digit = b[idx++];
            if (digit < '0' || digit > '9') {
                throw new ParseException("Invalid character in number", idx - 1);
            }
            
            value += (digit - '0');
            // Check for overflow after addition
            if (value < 0) {
                throw new ParseException("Long overflow", idx);
            }
        }
        
        if (isNegative) {
            if (value == Long.MIN_VALUE) {
                return value;
            }
            value = -value;
            if (value > 0) {
                throw new ParseException("Long overflow", idx);
            }
        }
        
        return value;
    }

    /**
     * Parses a double from a byte array.
     */
    public static double parseDouble(byte[] b, int idx, int len) throws ParseException {
        validateInput(b, idx, len);
        
        int to = idx + len;
        
        // Front trim whitespace
        while (idx < to && b[idx] == ' ') {
            idx++;
        }
        
        // Back trim whitespace
        while (idx < to && b[to - 1] == ' ') {
            to--;
        }
        
        if (idx >= to) {
            throw new ParseException("Empty number after trimming whitespace", idx);
        }

        // Handle sign
        int signMultiplier = 1;
        if (b[idx] == '-') {
            signMultiplier = -1;
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after minus sign", idx);
            }
        } else if (b[idx] == '+') {
            idx++;
            if (idx >= to) {
                throw new ParseException("Empty number after plus sign", idx);
            }
        }

        // Parse integer part
        long integerPart = 0;
        boolean hasDot = false;
        
        while (idx < to && !hasDot) {
            byte digit = b[idx];
            
            if (digit == '.') {
                hasDot = true;
                idx++;
                continue;
            }
            
            if (digit == 'E' || digit == 'e') {
                break;
            }
            
            if (digit < '0' || digit > '9') {
                throw new ParseException("Invalid character in integer part", idx);
            }
            
            long prevValue = integerPart;
            integerPart = integerPart * 10 + (digit - '0');
            if (integerPart / 10 != prevValue) {
                throw new ParseException("Number too large", idx);
            }
            
            idx++;
        }

        // Parse decimal part
        double decimalPart = 0;
        int decimalPosition = 0;
        
        if (hasDot && idx < to) {
            while (idx < to && decimalPosition < DECIMAL_MAX_LENGTH) {
                byte digit = b[idx];
                
                if (digit == 'E' || digit == 'e') {
                    break;
                }
                
                if (digit < '0' || digit > '9') {
                    throw new ParseException("Invalid character in decimal part", idx);
                }
                
                decimalPart = decimalPart * 10 + (digit - '0');
                decimalPosition++;
                idx++;
            }
            
            // Skip remaining decimal places but check they're valid
            while (idx < to && b[idx] != 'E' && b[idx] != 'e') {
                if (b[idx] < '0' || b[idx] > '9') {
                    throw new ParseException("Invalid character in decimal part", idx);
                }
                idx++;
            }
            
            decimalPart = decimalPart / Math.pow(10, decimalPosition);
        }

        // Parse exponent if present
        int exponent = 0;
        if (idx < to && (b[idx] == 'E' || b[idx] == 'e')) {
            idx++;
            if (idx >= to) {
                throw new ParseException("Missing exponent", idx);
            }
            
            boolean negativeExponent = false;
            if (b[idx] == '-') {
                negativeExponent = true;
                idx++;
            } else if (b[idx] == '+') {
                idx++;
            }
            
            if (idx >= to) {
                throw new ParseException("Empty exponent", idx);
            }
            
            while (idx < to) {
                byte digit = b[idx];
                if (digit < '0' || digit > '9') {
                    throw new ParseException("Invalid character in exponent", idx);
                }
                
                int prevExponent = exponent;
                exponent = exponent * 10 + (digit - '0');
                if (exponent / 10 != prevExponent || exponent < 0) {
                    throw new ParseException("Exponent too large", idx);
                }
                
                idx++;
            }
            
            if (negativeExponent) {
                exponent = -exponent;
            }
        }

        // Combine all parts
        double result = (integerPart + decimalPart) * Math.pow(10, exponent);
        return result * signMultiplier;
    }

    /**
     * Parses a date from a byte array using the specified format.
     */
    public static Date parseDate(byte[] b, int idx, int len, String format) throws ParseException {
        validateInput(b, idx, len);
        String dateStr = new String(b, idx, len);
        return new SimpleDateFormat(format).parse(dateStr);
    }

    /**
     * Validates input parameters for all parse methods.
     */
    private static void validateInput(byte[] b, int idx, int len) {
        if (b == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (len <= 0) {
            throw new IllegalArgumentException("Cannot parse empty string");
        }
        if (idx < 0 || idx >= b.length) {
            throw new IllegalArgumentException("Invalid start index");
        }
        if (idx + len > b.length) {
            throw new IllegalArgumentException("Length exceeds array bounds");
        }
    }
}
