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
        String currentUser;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT current_user")) {
            if (!result.next()) {
                throw new IllegalStateException("Cannot resolve Flyway fixture user");
            }
            currentUser = result.getString(1);
        }
        provisionForFlywayRunner(connection, currentUser);
    }

    public static void provisionForFlywayRunner(Connection connection, String flywayRunner)
            throws Exception {
        if (flywayRunner == null || !flywayRunner.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe Flyway runner role: " + flywayRunner);
        }
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
            // The deployment runner is separate from runtime roles. V1 itself requires
            // SUPERUSER/CREATEROLE for the final ownership handoff, grants the temporary
            // retention membership, and revokes it before completing.
            statement.execute("GRANT carebridge_checklist_schema_owner TO " + flywayRunner);
            statement.execute("GRANT USAGE, CREATE ON SCHEMA public TO " + flywayRunner);
            // Spring Boot may create a fresh Flyway connection after the
            // datasource has been initialised, so spring.flyway.init-sql is
            // not guaranteed to survive into every migration transaction.
            // Model the production runner's pre-set session contract at the
            // embedded role level; this is test-only and never changes the
            // application role's privileges.
            statement.execute("ALTER ROLE " + flywayRunner
                    + " SET carebridge.checklist_v1_writes_frozen = 'true'");
            statement.execute("ALTER ROLE " + flywayRunner
                    + " SET carebridge.checklist_p1_p2_role = 'MIGRATION'");
        }
    }
}
