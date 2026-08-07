package com.carebridge.backend.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.sql.DataSource;

/** Pre-provisions deployment-owned database roles required by checklist migrations. */
public final class EmbeddedPostgresRoleFixture {
    private EmbeddedPostgresRoleFixture() {
    }

    public static void provision(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            provision(connection);
        }
    }

    /** For callers that only have raw connection details, such as a Testcontainers container. */
    public static void provision(String jdbcUrl, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            provision(connection);
        }
    }

    public static void provision(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    DO $$ BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles
                                       WHERE rolname = 'carebridge_checklist_schema_owner') THEN
                            CREATE ROLE carebridge_checklist_schema_owner
                            NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
                            NOREPLICATION NOBYPASSRLS;
                        END IF;
                    END $$
                    """);
            statement.execute("""
                    DO $$ BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles
                                       WHERE rolname = 'carebridge_checklist_retention_owner') THEN
                            CREATE ROLE carebridge_checklist_retention_owner
                            NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
                            NOREPLICATION NOBYPASSRLS;
                        END IF;
                    END $$
                    """);
            statement.execute("""
                    DO $$ BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles
                                       WHERE rolname = 'checklist_operations') THEN
                            CREATE ROLE checklist_operations
                            LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
                            NOREPLICATION NOBYPASSRLS;
                        END IF;
                    END $$
                    """);
            statement.execute("""
                    DO $$ BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles
                                       WHERE rolname = 'carebridge_application') THEN
                            CREATE ROLE carebridge_application
                            LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
                            NOREPLICATION NOBYPASSRLS;
                        END IF;
                    END $$
                    """);
        }
    }
}
