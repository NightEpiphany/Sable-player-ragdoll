package com.moigferdsrte.ragdoll.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

public final class Json5Parser {
    private Json5Parser() {
    }

    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    public static JsonElement parse(String source) {
        Cursor cursor = new Cursor(source);
        JsonElement result = cursor.value(0);
        cursor.space();
        if (cursor.index != source.length()) throw cursor.error("Unexpected trailing input");
        return result;
    }

    /** Linear scan, with bounded nesting to reject malformed configuration safely. */
    private static final class Cursor {
        private final String source;
        private int index;
        Cursor(String source) { this.source = source; }
        IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at offset " + index); }
        void space() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\uFEFF') { index++; continue; }
                if (c != '/' || index + 1 >= source.length()) return;
                char next = source.charAt(index + 1);
                if (next == '/') {
                    index += 2;
                    while (index < source.length() && !newline(source.charAt(index))) index++;
                } else if (next == '*') {
                    int end = source.indexOf("*/", index + 2);
                    if (end < 0) throw error("Unterminated comment");
                    index = end + 2;
                } else return;
            }
        }
        boolean take(char c) {
            space();
            if (index < source.length() && source.charAt(index) == c) { index++; return true; }
            return false;
        }
        void require(char c) { if (!take(c)) throw error("Expected '" + c + "'"); }
        JsonElement value(int depth) {
            if (depth > 64) throw error("Nesting too deep");
            space();
            if (index == source.length()) throw error("Expected value");
            char c = source.charAt(index);
            if (c == '{') {
                index++;
                JsonObject object = new JsonObject();
                if (take('}')) return object;
                do {
                    space();
                    if (index == source.length()) throw error("Expected key");
                    char quote = source.charAt(index);
                    String key = quote == '\'' || quote == '"' ? string() : identifier();
                    require(':');
                    object.add(key, value(depth + 1));
                    if (take('}')) return object;
                    require(',');
                    if (take('}')) return object;
                } while (true);
            }
            if (c == '[') {
                index++;
                JsonArray array = new JsonArray();
                if (take(']')) return array;
                do {
                    array.add(value(depth + 1));
                    if (take(']')) return array;
                    require(',');
                    if (take(']')) return array;
                } while (true);
            }
            if (c == '\'' || c == '"') return new JsonPrimitive(string());
            int start = index;
            while (index < source.length()) {
                c = source.charAt(index);
                if (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == ',' || c == ']' || c == '}' || c == '/') break;
                index++;
            }
            String token = source.substring(start, index);
            return switch (token) {
                case "true" -> new JsonPrimitive(true);
                case "false" -> new JsonPrimitive(false);
                case "null" -> JsonNull.INSTANCE;
                case "Infinity", "+Infinity" -> new JsonPrimitive(Double.POSITIVE_INFINITY);
                case "-Infinity" -> new JsonPrimitive(Double.NEGATIVE_INFINITY);
                case "NaN", "+NaN", "-NaN" -> new JsonPrimitive(Double.NaN);
                default -> number(token);
            };
        }
        JsonElement number(String token) {
            try {
                String unsigned = token.startsWith("-") || token.startsWith("+") ? token.substring(1) : token;
                if (unsigned.startsWith("0x") || unsigned.startsWith("0X")) {
                    String digits = unsigned.substring(2);
                    if (digits.isEmpty() || !digits.chars().allMatch(c -> Character.digit(c, 16) >= 0)) throw error("Invalid hex number");
                    BigInteger value = new BigInteger(digits, 16);
                    return new JsonPrimitive(token.startsWith("-") ? value.negate() : value);
                }
                if (!DECIMAL.matcher(token).matches()) throw error("Invalid value '" + token + "'");
                return new JsonPrimitive(new BigDecimal(token));
            } catch (NumberFormatException exception) { throw error("Invalid number"); }
        }
        String identifier() {
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index);
                if (c == '\\') {
                    index++;
                    if (index >= source.length() || source.charAt(index++) != 'u') throw error("Invalid identifier escape");
                    c = hex(4);
                } else {
                    if (!(c == '$' || c == '_' || (result.isEmpty() ? Character.isUnicodeIdentifierStart(c) : Character.isUnicodeIdentifierPart(c)))) break;
                    index++;
                }
                if (!(c == '$' || c == '_' || (result.isEmpty() ? Character.isUnicodeIdentifierStart(c) : Character.isUnicodeIdentifierPart(c)))) throw error("Invalid identifier");
                result.append(c);
            }
            if (result.isEmpty()) throw error("Expected identifier");
            return result.toString();
        }
        String string() {
            char quote = source.charAt(index++);
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == quote) return result.toString();
                if (newline(c)) throw error("Unescaped line break");
                if (c != '\\') { result.append(c); continue; }
                if (index == source.length()) throw error("Unterminated escape");
                c = source.charAt(index++);
                if (newline(c)) {
                    if (c == '\r' && index < source.length() && source.charAt(index) == '\n') index++;
                    continue;
                }
                switch (c) {
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'v' -> result.append('\u000B');
                    case '0' -> {
                        if (index < source.length() && Character.isDigit(source.charAt(index))) throw error("Octal escape is not supported");
                        result.append('\0');
                    }
                    case 'x' -> result.append(hex(2));
                    case 'u' -> result.append(hex(4));
                    default -> {
                        if (Character.isDigit(c)) throw error("Invalid numeric escape");
                        result.append(c);
                    }
                }
            }
            throw error("Unterminated string");
        }
        char hex(int digits) {
            if (index + digits > source.length()) throw error("Incomplete hex escape");
            int value = 0;
            for (int i = 0; i < digits; i++) {
                int digit = Character.digit(source.charAt(index++), 16);
                if (digit < 0) throw error("Invalid hex escape");
                value = value * 16 + digit;
            }
            return (char) value;
        }
        static boolean newline(char c) { return c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029'; }
    }
}
