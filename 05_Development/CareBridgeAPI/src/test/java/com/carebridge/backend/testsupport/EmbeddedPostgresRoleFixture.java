package com.carebridge.backend.testsupport;

import java.sql.Connection;
import javax.sql.DataSource;

/** Pre-provisions deployment-owned database roles required by checklist migrations. */
public final class EmbeddedPostgresRoleFixture {
    private EmbeddedPostgresRoleFixture() {
    }

    public static void provision(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
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
