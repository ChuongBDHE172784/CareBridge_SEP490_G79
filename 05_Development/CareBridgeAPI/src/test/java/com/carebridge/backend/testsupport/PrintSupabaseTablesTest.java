package com.carebridge.backend.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PrintSupabaseTablesTest {

    @Test
    void printTables() throws Exception {
        Assumptions.assumeTrue(
            "true".equalsIgnoreCase(System.getenv("SUPABASE_SCHEMA_RESET_TEST_ENABLED")),
            "Set SUPABASE_SCHEMA_RESET_TEST_ENABLED=true to allow this destructive database test"
        );

        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        Assumptions.assumeTrue(
            url != null && !url.isBlank()
                && username != null && !username.isBlank()
                && password != null && !password.isBlank(),
            "Supabase database environment variables are required"
        );

        // 1. Drop and recreate public schema
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS public CASCADE");
            stmt.execute("CREATE SCHEMA public");
        }

        // 2. Run Flyway migration
        Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .load()
            .migrate();

        // 3. Print tables list
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            try (ResultSet rs = conn.getMetaData().getTables("postgres", "public", "%", new String[]{"TABLE"})) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
                Collections.sort(tables);
                System.out.println("TOTAL_TABLES_COUNT: " + tables.size());
                for (String table : tables) {
                    System.out.println("TABLE_NAME: " + table);
                }
            }
        }
    }
}
