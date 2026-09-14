package com.sinh.backend.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility parser that validates boolean condition expression strings (with parentheses, &&, ||, ==, !=, >, <, etc.)
 * and converts them into a nested JSON condition tree with Value-Based DataType Inference.
 */
public class ConditionExpressionParser {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\"?\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2})?\"?$");

    /**
     * Parse an expression string into a nested Map representation of the condition tree.
     * Throws IllegalArgumentException if the expression syntax is invalid.
     */
    public static Map<String, Object> parseToTree(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String trimmed = expression.trim();
        List<Token> tokens = tokenize(trimmed);
        if (tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        Parser parser = new Parser(tokens);
        Map<String, Object> result = parser.parseExpression();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Cú pháp biểu thức không hợp lệ gần '" + parser.peek().text + "'");
        }
        return result;
    }

    // ==========================================================
    // TOKENIZER
    // ==========================================================
    private enum TokenType {
        LPAREN, RPAREN, AND, OR, OP, IDENTIFIER, LITERAL
    }

    private static class Token {
        final TokenType type;
        final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = input.length();

        while (i < len) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
            } else if (c == '&' && i + 1 < len && input.charAt(i + 1) == '&') {
                tokens.add(new Token(TokenType.AND, "&&"));
                i += 2;
            } else if (c == '|' && i + 1 < len && input.charAt(i + 1) == '|') {
                tokens.add(new Token(TokenType.OR, "||"));
                i += 2;
            } else if (c == '=' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.OP, "=="));
                i += 2;
            } else if (c == '!' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.OP, "!="));
                i += 2;
            } else if (c == '>' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.OP, ">="));
                i += 2;
            } else if (c == '<' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.OP, "<="));
                i += 2;
            } else if (c == '<' && i + 1 < len && input.charAt(i + 1) == '>') {
                tokens.add(new Token(TokenType.OP, "<>"));
                i += 2;
            } else if (c == '>') {
                tokens.add(new Token(TokenType.OP, ">"));
                i++;
            } else if (c == '<') {
                tokens.add(new Token(TokenType.OP, "<"));
                i++;
            } else if (c == '=') {
                tokens.add(new Token(TokenType.OP, "="));
                i++;
            } else if (c == '"' || c == '\'') {
                char quote = c;
                int start = i;
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < len && input.charAt(i) != quote) {
                    sb.append(input.charAt(i));
                    i++;
                }
                if (i >= len) {
                    throw new IllegalArgumentException("Thiếu dấu ngoặc báo chuỗi " + quote);
                }
                i++; // skip quote
                tokens.add(new Token(TokenType.LITERAL, sb.toString()));
            } else {
                // Word or Number
                int start = i;
                while (i < len && !Character.isWhitespace(input.charAt(i))
                        && "()&|=!><,'\"".indexOf(input.charAt(i)) == -1) {
                    i++;
                }
                String word = input.substring(start, i);
                if ("&&".equals(word) || "AND".equalsIgnoreCase(word)) {
                    tokens.add(new Token(TokenType.AND, "AND"));
                } else if ("||".equals(word) || "OR".equalsIgnoreCase(word)) {
                    tokens.add(new Token(TokenType.OR, "OR"));
                } else if ("CONTAINS".equalsIgnoreCase(word) || "contains".equalsIgnoreCase(word)) {
                    tokens.add(new Token(TokenType.OP, "CONTAINS"));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, word));
                }
            }
        }
        return tokens;
    }

    // ==========================================================
    // RECURSIVE DESCENT PARSER
    // ==========================================================
    private static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean isAtEnd() {
            return pos >= tokens.size();
        }

        Token peek() {
            return isAtEnd() ? new Token(TokenType.IDENTIFIER, "") : tokens.get(pos);
        }

        Token advance() {
            return tokens.get(pos++);
        }

        boolean match(TokenType type) {
            if (!isAtEnd() && peek().type == type) {
                advance();
                return true;
            }
            return false;
        }

        // Expression -> OrExpr
        Map<String, Object> parseExpression() {
            return parseOr();
        }

        // OrExpr -> AndExpr ( '||' AndExpr )*
        Map<String, Object> parseOr() {
            List<Map<String, Object>> children = new ArrayList<>();
            children.add(parseAnd());

            while (match(TokenType.OR)) {
                children.add(parseAnd());
            }

            if (children.size() == 1) {
                return children.get(0);
            }

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("logicalOperator", "OR");
            node.put("children", children);
            return node;
        }

        // AndExpr -> PrimaryExpr ( '&&' PrimaryExpr )*
        Map<String, Object> parseAnd() {
            List<Map<String, Object>> children = new ArrayList<>();
            children.add(parsePrimary());

            while (match(TokenType.AND)) {
                children.add(parsePrimary());
            }

            if (children.size() == 1) {
                return children.get(0);
            }

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("logicalOperator", "AND");
            node.put("children", children);
            return node;
        }

        // PrimaryExpr -> '(' Expression ')' | Comparison
        Map<String, Object> parsePrimary() {
            if (match(TokenType.LPAREN)) {
                Map<String, Object> expr = parseExpression();
                if (!match(TokenType.RPAREN)) {
                    throw new IllegalArgumentException("Thiếu dấu đóng ngoặc ')' trong biểu thức");
                }
                return expr;
            }
            return parseComparison();
        }

        // Comparison -> field OP value
        Map<String, Object> parseComparison() {
            if (isAtEnd()) {
                throw new IllegalArgumentException("Biểu thức chưa hoàn chỉnh");
            }

            Token fieldToken = advance();
            if (fieldToken.type != TokenType.IDENTIFIER && fieldToken.type != TokenType.LITERAL) {
                throw new IllegalArgumentException("Kỳ vọng tên trường tại '" + fieldToken.text + "'");
            }

            if (isAtEnd() || peek().type != TokenType.OP) {
                throw new IllegalArgumentException("Kỳ vọng toán tử so sánh (==, !=, >, <,...) sau '" + fieldToken.text + "'");
            }

            Token opToken = advance();

            if (isAtEnd()) {
                throw new IllegalArgumentException("Kỳ vọng giá trị so sánh sau toán tử '" + opToken.text + "'");
            }

            Token valToken = advance();

            String field = fieldToken.text;
            String rawOp = opToken.text;
            String rawVal = valToken.text;

            String operator = normalizeOperator(rawOp);
            Object value = parseTypedValue(rawVal);

            String valueType = "LITERAL";
            String dataType = inferDataType(rawVal, value);

            // If value token is an unquoted IDENTIFIER and not boolean/number, it represents a Field/Variable reference!
            if (valToken.type == TokenType.IDENTIFIER && !(value instanceof Boolean) && !(value instanceof Number)) {
                valueType = "FIELD_REF";
                dataType = "VARIABLE";
            }

            Map<String, Object> leaf = new LinkedHashMap<>();
            leaf.put("field", field);
            leaf.put("operator", operator);
            leaf.put("value", value);
            leaf.put("valueType", valueType);
            leaf.put("dataType", dataType);

            return leaf;
        }
    }

    // ==========================================================
    // HELPERS & VALUE INFERENCE
    // ==========================================================
    private static String normalizeOperator(String op) {
        switch (op) {
            case "=":
            case "==":
                return "EQUALS";
            case "!=":
            case "<>":
                return "NOT_EQUALS";
            case ">":
                return "GREATER_THAN";
            case ">=":
                return "GREATER_THAN_OR_EQUAL";
            case "<":
                return "LESS_THAN";
            case "<=":
                return "LESS_THAN_OR_EQUAL";
            case "CONTAINS":
            case "contains":
                return "CONTAINS";
            default:
                return op.toUpperCase();
        }
    }

    private static Object parseTypedValue(String val) {
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
            return Boolean.parseBoolean(val);
        }

        try {
            if (val.contains(".")) {
                return Double.parseDouble(val);
            } else {
                return Long.parseLong(val);
            }
        } catch (NumberFormatException ignored) {
        }

        return val;
    }

    private static String inferDataType(String rawVal, Object parsedVal) {
        if (parsedVal instanceof Boolean) {
            return "BOOLEAN";
        }
        if (parsedVal instanceof Number) {
            return "NUMBER";
        }
        if (rawVal != null && DATE_PATTERN.matcher(rawVal.trim()).matches()) {
            return "DATE";
        }
        return "STRING";
    }
}
