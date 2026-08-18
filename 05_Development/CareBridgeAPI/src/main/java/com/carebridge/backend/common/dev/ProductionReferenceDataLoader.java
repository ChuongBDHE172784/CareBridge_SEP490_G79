package com.carebridge.backend.common.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the canonical baseline reference data (health metrics, knowledge sources,
 * red flag rules, community topics, ai moderation policies, vaccination schedules,
 * care item templates) from db/data_seed/production_reference_data.sql.
 *
 * Runs automatically on startup across all profiles (idempotent ON CONFLICT clauses).
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(
        prefix = "carebridge.reference-data",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class ProductionReferenceDataLoader implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        seedProductionReferenceData();
    }

    /**
     * Executes the idempotent SQL statements in db/data_seed/production_reference_data.sql.
     */
    public void seedProductionReferenceData() {
        ClassPathResource resource = new ClassPathResource(
                "db/data_seed/production_reference_data.sql");
        if (!resource.exists()) {
            log.warn("Production reference data script is not available on the classpath");
            return;
        }

        try (var inputStream = resource.getInputStream()) {
            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            int executed = 0;
            for (String statement : splitSqlStatements(script)) {
                if (!statement.isBlank()) {
                    jdbcTemplate.execute(statement);
                    executed++;
                }
            }
            log.info("Loaded {} statements from production reference data script", executed);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read production reference data script", ex);
        }
    }

    /** Split PostgreSQL SQL while preserving semicolons inside strings and dollar-quoted blocks. */
    static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String dollarTag = null;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';
            if (lineComment) {
                current.append(ch);
                if (ch == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                current.append(ch);
                if (ch == '*' && next == '/') {
                    current.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }
            if (dollarTag != null) {
                if (script.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (singleQuoted) {
                current.append(ch);
                if (ch == '\'' && next == '\'') {
                    current.append(next);
                    i++;
                } else if (ch == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                current.append(ch);
                if (ch == '"' && next == '"') {
                    current.append(next);
                    i++;
                } else if (ch == '"') {
                    doubleQuoted = false;
                }
                continue;
            }
            if (ch == '-' && next == '-') {
                current.append(ch).append(next);
                i++;
                lineComment = true;
            } else if (ch == '/' && next == '*') {
                current.append(ch).append(next);
                i++;
                blockComment = true;
            } else if (ch == '\'') {
                current.append(ch);
                singleQuoted = true;
            } else if (ch == '"') {
                current.append(ch);
                doubleQuoted = true;
            } else if (ch == '$') {
                int end = script.indexOf('$', i + 1);
                if (end > i && script.substring(i + 1, end).matches("[A-Za-z_][A-Za-z0-9_]*|")) {
                    dollarTag = script.substring(i, end + 1);
                    current.append(dollarTag);
                    i = end;
                } else {
                    current.append(ch);
                }
            } else if (ch == ';') {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString());
        }
        return statements;
    }
}
