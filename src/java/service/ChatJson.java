package service;

import java.math.BigDecimal;
import java.util.*;

/** Small JSON codec for the REST adapter; no runtime dependency is required. */
public final class ChatJson {
    private ChatJson() {}

    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : text.toCharArray())
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 32) out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            return out.append('"').toString();
        }
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((key, item) -> entries.add(encode(key.toString()) + ":" + encode(item)));
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Collection<?> list)
            return "[" + String.join(",", list.stream().map(ChatJson::encode).toList()) + "]";
        throw new IllegalArgumentException("Unsupported JSON value");
    }

    public static Object parse(String text) {
        if (text == null || text.length() > 262144)
            throw new IllegalArgumentException("Invalid JSON size");
        Parser parser = new Parser(text);
        Object result = parser.value(0);
        parser.space();
        if (parser.index != text.length())
            throw new IllegalArgumentException("Trailing JSON content");
        return result;
    }

    private static final class Parser {
        final String text;
        int index;

        Parser(String text) {
            this.text = text;
        }

        void space() {
            while (index < text.length() && " \t\r\n".indexOf(text.charAt(index)) >= 0) index++;
        }

        boolean take(char c) {
            space();
            if (index < text.length() && text.charAt(index) == c) {
                index++;
                return true;
            }
            return false;
        }

        void expect(char c) {
            if (!take(c)) throw new IllegalArgumentException("Invalid JSON syntax");
        }

        Object value(int depth) {
            space();
            if (depth > 32 || index >= text.length())
                throw new IllegalArgumentException("Invalid JSON nesting");
            char c = text.charAt(index);
            if (c == '"') return string();
            if (c == '{') {
                index++;
                Map<String, Object> map = new LinkedHashMap<>();
                if (take('}')) return map;
                do {
                    space();
                    String key = string();
                    expect(':');
                    if (map.containsKey(key))
                        throw new IllegalArgumentException("Duplicate JSON key");
                    map.put(key, value(depth + 1));
                } while (take(','));
                expect('}');
                return map;
            }
            if (c == '[') {
                index++;
                List<Object> list = new ArrayList<>();
                if (take(']')) return list;
                do {
                    list.add(value(depth + 1));
                } while (take(','));
                expect(']');
                return list;
            }
            for (String literal : List.of("true", "false", "null"))
                if (text.startsWith(literal, index)) {
                    index += literal.length();
                    return literal.equals("null") ? null : Boolean.valueOf(literal);
                }
            int from = index;
            while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0)
                index++;
            String number = text.substring(from, index);
            if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))
                throw new IllegalArgumentException("Invalid JSON number");
            return new BigDecimal(number);
        }

        String string() {
            if (index >= text.length() || text.charAt(index++) != '"')
                throw new IllegalArgumentException("Expected JSON string");
            StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') return out.toString();
                if (c < 32) throw new IllegalArgumentException("JSON control character");
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (index >= text.length()) break;
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        if (index + 4 > text.length())
                            throw new IllegalArgumentException("JSON unicode escape");
                        out.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }
    }
}
