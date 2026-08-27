package com.carebridge.backend.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Splits PostgreSQL scripts without treating quoted/commented semicolons as boundaries. */
final class SqlTopLevelStatementParser {

    private static final Pattern LEADING_KEYWORD =
            Pattern.compile("^\\s*([A-Za-z]+)", Pattern.DOTALL);
    private static final Pattern INSERT_TARGET = Pattern.compile(
            "(?is)^\\s*INSERT\\s+INTO\\s+(?:\\\"?public\\\"?\\.)?\\\"?([a-z_][a-z0-9_]*)\\\"?");
    private static final Pattern DATA_MUTATION_KEYWORD =
            Pattern.compile("(?i)\\b(INSERT|UPDATE|DELETE|MERGE|COPY|TRUNCATE)\\b");
    private static final Pattern DYNAMIC_EXECUTE_KEYWORD =
            Pattern.compile("(?i)\\bEXECUTE\\b");
    private static final Pattern SAFE_DYNAMIC_ACL_LEADER =
            Pattern.compile("(?i)^\\s*(GRANT|REVOKE)\\b");

    private SqlTopLevelStatementParser() {}

    static List<SqlStatement> parse(String script) {
        List<SqlStatement> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        State state = State.CODE;
        String dollarDelimiter = null;
        int blockDepth = 0;

        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            char next = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (state == State.LINE_COMMENT) {
                if (character == '\n') {
                    state = State.CODE;
                    current.append(character);
                }
                continue;
            }
            if (state == State.BLOCK_COMMENT) {
                if (character == '/' && next == '*') {
                    blockDepth++;
                    index++;
                } else if (character == '*' && next == '/') {
                    blockDepth--;
                    index++;
                    if (blockDepth == 0) {
                        state = State.CODE;
                    }
                }
                continue;
            }

            current.append(character);
            if (state == State.SINGLE_QUOTE) {
                if (character == '\'' && next == '\'') {
                    current.append(next);
                    index++;
                } else if (character == '\'') {
                    state = State.CODE;
                }
                continue;
            }
            if (state == State.DOUBLE_QUOTE) {
                if (character == '"' && next == '"') {
                    current.append(next);
                    index++;
                } else if (character == '"') {
                    state = State.CODE;
                }
                continue;
            }
            if (state == State.DOLLAR_QUOTE) {
                if (script.startsWith(dollarDelimiter, index)) {
                    current.append(dollarDelimiter, 1, dollarDelimiter.length());
                    index += dollarDelimiter.length() - 1;
                    state = State.CODE;
                }
                continue;
            }

            if (character == '-' && next == '-') {
                current.setLength(current.length() - 1);
                state = State.LINE_COMMENT;
                index++;
            } else if (character == '/' && next == '*') {
                current.setLength(current.length() - 1);
                state = State.BLOCK_COMMENT;
                blockDepth = 1;
                index++;
            } else if (character == '\'') {
                state = State.SINGLE_QUOTE;
            } else if (character == '"') {
                state = State.DOUBLE_QUOTE;
            } else if (character == '$') {
                String candidate = dollarDelimiterAt(script, index);
                if (candidate != null) {
                    current.append(candidate, 1, candidate.length());
                    index += candidate.length() - 1;
                    dollarDelimiter = candidate;
                    state = State.DOLLAR_QUOTE;
                }
            } else if (character == ';') {
                current.setLength(current.length() - 1);
                addStatement(statements, current);
                current.setLength(0);
            }
        }
        if (state != State.CODE && state != State.LINE_COMMENT) {
            throw new IllegalArgumentException("Unterminated SQL lexical state: " + state);
        }
        addStatement(statements, current);
        return List.copyOf(statements);
    }

    static boolean containsExecutableDataMutation(String code) {
        String scrubbed = scrubQuotedTextAndComments(code);
        if (DATA_MUTATION_KEYWORD.matcher(scrubbed).find()) {
            return true;
        }
        Matcher execute = DYNAMIC_EXECUTE_KEYWORD.matcher(scrubbed);
        while (execute.find()) {
            int statementEnd = scrubbed.indexOf(';', execute.end());
            if (statementEnd < 0) {
                statementEnd = code.length();
            }
            String dynamicExpression = code.substring(execute.end(), statementEnd);
            String dynamicSql = scrubQuotedTextAndComments(quotedText(dynamicExpression));
            if (!SAFE_DYNAMIC_ACL_LEADER.matcher(dynamicSql).find()
                    && DATA_MUTATION_KEYWORD.matcher(dynamicSql).find()) {
                return true;
            }
        }
        return false;
    }

    private static String scrubQuotedTextAndComments(String code) {
        StringBuilder scrubbed = new StringBuilder(code.length());
        State state = State.CODE;
        String dollarDelimiter = null;
        int blockDepth = 0;
        for (int index = 0; index < code.length(); index++) {
            char character = code.charAt(index);
            char next = index + 1 < code.length() ? code.charAt(index + 1) : '\0';
            if (state == State.LINE_COMMENT) {
                if (character == '\n') {
                    state = State.CODE;
                    scrubbed.append(character);
                } else {
                    scrubbed.append(' ');
                }
                continue;
            }
            if (state == State.BLOCK_COMMENT) {
                scrubbed.append(' ');
                if (character == '/' && next == '*') {
                    blockDepth++;
                    scrubbed.append(' ');
                    index++;
                } else if (character == '*' && next == '/') {
                    blockDepth--;
                    scrubbed.append(' ');
                    index++;
                    if (blockDepth == 0) {
                        state = State.CODE;
                    }
                }
                continue;
            }
            if (state == State.SINGLE_QUOTE) {
                scrubbed.append(' ');
                if (character == '\'' && next == '\'') {
                    scrubbed.append(' ');
                    index++;
                } else if (character == '\'') {
                    state = State.CODE;
                }
                continue;
            }
            if (state == State.DOUBLE_QUOTE) {
                scrubbed.append(' ');
                if (character == '"' && next == '"') {
                    scrubbed.append(' ');
                    index++;
                } else if (character == '"') {
                    state = State.CODE;
                }
                continue;
            }
            if (state == State.DOLLAR_QUOTE) {
                if (code.startsWith(dollarDelimiter, index)) {
                    scrubbed.append(" ".repeat(dollarDelimiter.length()));
                    index += dollarDelimiter.length() - 1;
                    state = State.CODE;
                } else {
                    scrubbed.append(' ');
                }
                continue;
            }
            if (character == '-' && next == '-') {
                scrubbed.append("  ");
                state = State.LINE_COMMENT;
                index++;
            } else if (character == '/' && next == '*') {
                scrubbed.append("  ");
                state = State.BLOCK_COMMENT;
                blockDepth = 1;
                index++;
            } else if (character == '\'') {
                scrubbed.append(' ');
                state = State.SINGLE_QUOTE;
            } else if (character == '"') {
                scrubbed.append(' ');
                state = State.DOUBLE_QUOTE;
            } else if (character == '$') {
                String candidate = dollarDelimiterAt(code, index);
                if (candidate == null) {
                    scrubbed.append(character);
                } else {
                    scrubbed.append(" ".repeat(candidate.length()));
                    index += candidate.length() - 1;
                    dollarDelimiter = candidate;
                    state = State.DOLLAR_QUOTE;
                }
            } else {
                scrubbed.append(character);
            }
        }
        return scrubbed.toString();
    }

    private static String quotedText(String expression) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < expression.length(); index++) {
            char character = expression.charAt(index);
            char next = index + 1 < expression.length() ? expression.charAt(index + 1) : '\0';
            if (character == '-' && next == '-') {
                int newline = expression.indexOf('\n', index + 2);
                index = newline < 0 ? expression.length() : newline;
            } else if (character == '/' && next == '*') {
                int depth = 1;
                index += 2;
                while (index < expression.length() && depth > 0) {
                    char current = expression.charAt(index);
                    char following = index + 1 < expression.length()
                            ? expression.charAt(index + 1) : '\0';
                    if (current == '/' && following == '*') {
                        depth++;
                        index += 2;
                    } else if (current == '*' && following == '/') {
                        depth--;
                        index += 2;
                    } else {
                        index++;
                    }
                }
                index--;
            } else if (character == '\'') {
                index++;
                while (index < expression.length()) {
                    char current = expression.charAt(index);
                    char following = index + 1 < expression.length()
                            ? expression.charAt(index + 1) : '\0';
                    if (current == '\'' && following == '\'') {
                        text.append('\'');
                        index += 2;
                    } else if (current == '\'') {
                        break;
                    } else {
                        text.append(current);
                        index++;
                    }
                }
            } else if (character == '"') {
                index++;
                while (index < expression.length()) {
                    if (expression.charAt(index) == '"') {
                        if (index + 1 < expression.length()
                                && expression.charAt(index + 1) == '"') {
                            index += 2;
                        } else {
                            break;
                        }
                    } else {
                        index++;
                    }
                }
            } else if (character == '$') {
                String delimiter = dollarDelimiterAt(expression, index);
                if (delimiter != null) {
                    int bodyStart = index + delimiter.length();
                    int bodyEnd = expression.indexOf(delimiter, bodyStart);
                    if (bodyEnd < 0) {
                        throw new IllegalArgumentException("Unterminated dynamic SQL dollar quote");
                    }
                    text.append(expression, bodyStart, bodyEnd);
                    index = bodyEnd + delimiter.length() - 1;
                }
            }
        }
        return text.toString();
    }

    private static String dollarDelimiterAt(String script, int index) {
        int closing = script.indexOf('$', index + 1);
        if (closing < 0) {
            return null;
        }
        String tag = script.substring(index + 1, closing);
        return tag.matches("([A-Za-z_][A-Za-z0-9_]*)?")
                ? script.substring(index, closing + 1)
                : null;
    }

    private static void addStatement(List<SqlStatement> statements, StringBuilder raw) {
        String sql = raw.toString().trim();
        if (sql.isEmpty()) {
            return;
        }
        Matcher matcher = LEADING_KEYWORD.matcher(sql);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Statement has no leading keyword: " + sql);
        }
        statements.add(new SqlStatement(matcher.group(1).toUpperCase(Locale.ROOT), sql));
    }

    record SqlStatement(String type, String sql) {
        String insertTarget() {
            Matcher matcher = INSERT_TARGET.matcher(sql);
            if (!matcher.find()) {
                throw new IllegalStateException("Not a canonical INSERT statement: " + sql);
            }
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }

        String dollarQuotedBody() {
            int first = sql.indexOf('$');
            String delimiter = first < 0 ? null : dollarDelimiterAt(sql, first);
            if (delimiter == null) {
                return "";
            }
            int end = sql.lastIndexOf(delimiter);
            if (end <= first) {
                throw new IllegalStateException("Unterminated dollar body");
            }
            return sql.substring(first + delimiter.length(), end);
        }
    }

    private enum State {
        CODE,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        DOLLAR_QUOTE,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
