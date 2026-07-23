package com.carebridge.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class CanonicalRoleMigrationIntegrationTest {

    private static final String PRE_CONSOLIDATION_TARGET = "20260722020300";
    private static final String CONSOLIDATION_TARGET = "20260722020400";

    @Test
    void emptyLegacyTablesPreserveAssignedAndUnassignedUsersAndDropExactlyTwoTables()
            throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            long tableCountBefore;
            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("""
                        SELECT count(*)
                          FROM information_schema.tables
                         WHERE table_schema = 'public'
                           AND table_type = 'BASE TABLE'
                        """)) {
                    result.next();
                    tableCountBefore = result.getLong(1);
                }
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, role, created_at, updated_at)
                        SELECT gen_random_uuid(), 'role-user-' || n || '@carebridge.dev',
                               CASE WHEN n <= 12 THEN NULL ELSE 'MOTHER' END,
                               now(), now()
                          FROM generate_series(1, 52) AS n
                        """);
            }

            flyway(postgres, CONSOLIDATION_TARGET).migrate();

            try (var connection = connection(postgres);
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT count(*) AS user_count,
                                count(*) FILTER (WHERE role IS NULL) AS null_role_count,
                                to_regclass('public.roles') AS roles_table,
                                to_regclass('public.user_roles') AS user_roles_table,
                                (SELECT count(*)
                                   FROM information_schema.tables
                                  WHERE table_schema = 'public'
                                    AND table_type = 'BASE TABLE') AS table_count_after
                           FROM users
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("user_count")).isEqualTo(52);
                assertThat(result.getLong("null_role_count")).isEqualTo(12);
                assertThat(result.getString("roles_table")).isNull();
                assertThat(result.getString("user_roles_table")).isNull();
                assertThat(result.getLong("table_count_after")).isEqualTo(tableCountBefore - 2);
            }
        }
    }

    @Test
    void oneToOneActiveMappingBackfillsNullRoleAndPreservesMatchingRole() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID roleId = UUID.randomUUID();
            UUID nullRoleUserId = UUID.randomUUID();
            UUID matchingUserId = UUID.randomUUID();
            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO roles (role_id, role_code, role_name, is_active, created_at)
                        VALUES ('%s', 'FAMILY', 'Family', true, now())
                        """.formatted(roleId));
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, role, created_at, updated_at) VALUES
                        ('%s', 'role-null@carebridge.dev', NULL, now(), now()),
                        ('%s', 'role-match@carebridge.dev', 'FAMILY', now(), now())
                        """.formatted(nullRoleUserId, matchingUserId));
                statement.executeUpdate("""
                        INSERT INTO user_roles (
                            user_role_id, user_id, role_id, status, expires_at, created_at
                        ) VALUES
                        (gen_random_uuid(), '%s', '%s', 'ACTIVE', NULL, now()),
                        (gen_random_uuid(), '%s', '%s', 'ACTIVE', NULL, now())
                        """.formatted(nullRoleUserId, roleId, matchingUserId, roleId));
            }

            flyway(postgres, CONSOLIDATION_TARGET).migrate();

            try (var connection = connection(postgres);
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT count(*) FILTER (WHERE role = 'FAMILY') AS family_count,
                                to_regclass('public.roles') AS roles_table,
                                to_regclass('public.user_roles') AS user_roles_table
                           FROM users
                          WHERE user_id IN ('%s', '%s')
                         """.formatted(nullRoleUserId, matchingUserId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("family_count")).isEqualTo(2);
                assertThat(result.getString("roles_table")).isNull();
                assertThat(result.getString("user_roles_table")).isNull();
            }
        }
    }

    @Test
    void futureExpiringMappingRollsBackInsteadOfMakingTemporaryRolePermanent() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID roleId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO roles (role_id, role_code, role_name, is_active, created_at)
                        VALUES ('%s', 'FAMILY', 'Family', true, now())
                        """.formatted(roleId));
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, role, created_at, updated_at)
                        VALUES ('%s', 'role-expiring@carebridge.dev', NULL, now(), now())
                        """.formatted(userId));
                statement.executeUpdate("""
                        INSERT INTO user_roles (
                            user_role_id, user_id, role_id, status, expires_at, created_at
                        ) VALUES (
                            gen_random_uuid(), '%s', '%s', 'ACTIVE', now() + interval '30 days', now()
                        )
                        """.formatted(userId, roleId));
            }

            assertThatThrownBy(() -> flyway(postgres, CONSOLIDATION_TARGET).migrate())
                    .hasMessageContaining("inactive, expiring, or historical role mapping");
            assertNullRoleRollbackState(postgres, userId);
        }
    }

    @Test
    void multipleMappingsForOneUserRollBackWithoutChoosingARole() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID userId = UUID.randomUUID();
            UUID motherRoleId = UUID.randomUUID();
            UUID familyRoleId = UUID.randomUUID();
            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO roles (role_id, role_code, role_name, is_active, created_at) VALUES
                        ('%s', 'MOTHER', 'Mother', true, now()),
                        ('%s', 'FAMILY', 'Family', true, now())
                        """.formatted(motherRoleId, familyRoleId));
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, role, created_at, updated_at)
                        VALUES ('%s', 'role-multiple@carebridge.dev', NULL, now(), now())
                        """.formatted(userId));
                statement.executeUpdate("""
                        INSERT INTO user_roles (user_role_id, user_id, role_id, status, created_at) VALUES
                        (gen_random_uuid(), '%s', '%s', 'ACTIVE', now()),
                        (gen_random_uuid(), '%s', '%s', 'ACTIVE', now())
                        """.formatted(userId, motherRoleId, userId, familyRoleId));
            }

            assertThatThrownBy(() -> flyway(postgres, CONSOLIDATION_TARGET).migrate())
                    .hasMessageContaining("multi-role mapping cannot be represented");
            assertNullRoleRollbackState(postgres, userId);
        }
    }

    @Test
    void conflictingMappingRollsBackBackfillAndKeepsBothLegacyTables() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID roleId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            insertMapping(postgres, roleId, userId, "MOTHER", "FAMILY");

            assertThatThrownBy(() -> flyway(postgres, CONSOLIDATION_TARGET).migrate())
                    .hasMessageContaining("users.role conflicts with legacy mapping");

            assertRollbackState(postgres, userId, "MOTHER");
        }
    }

    @Test
    void publicFunctionDependencyFailsBeforeDropAndKeepsBothLegacyTables() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE FUNCTION public.count_legacy_roles() RETURNS bigint
                        LANGUAGE sql AS 'SELECT count(*) FROM public."roles"'
                        """);
            }

            assertThatThrownBy(() -> flyway(postgres, CONSOLIDATION_TARGET).migrate())
                    .hasMessageContaining("function/procedure reference");

            try (var connection = connection(postgres);
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT to_regclass('public.roles'),
                                to_regclass('public.user_roles')
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("roles");
                assertThat(result.getString(2)).isEqualTo("user_roles");
            }
        }
    }

    @Test
    void crossSchemaFunctionDependencyFailsBeforeDropAndKeepsBothLegacyTables() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA app_security");
                statement.execute("""
                        CREATE FUNCTION app_security.count_legacy_roles() RETURNS bigint
                        LANGUAGE plpgsql AS $$
                        DECLARE legacy_count bigint;
                        BEGIN
                            EXECUTE 'SELECT count(*) FROM public.roles' INTO legacy_count;
                            RETURN legacy_count;
                        END
                        $$
                        """);
            }

            assertThatThrownBy(() -> flyway(postgres, CONSOLIDATION_TARGET).migrate())
                    .hasMessageContaining("function/procedure reference");

            try (var connection = connection(postgres);
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT to_regclass('public.roles'),
                                to_regclass('public.user_roles')
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("roles");
                assertThat(result.getString(2)).isEqualTo("user_roles");
            }
        }
    }

    private static void insertMapping(
            PostgreSQLContainer postgres, UUID roleId, UUID userId,
            String userRole, String legacyRole) throws Exception {
        try (var connection = connection(postgres);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO roles (role_id, role_code, role_name, is_active, created_at)
                    VALUES ('%s', '%s', 'Legacy role', true, now())
                    """.formatted(roleId, legacyRole));
            statement.executeUpdate("""
                    INSERT INTO users (user_id, email, role, created_at, updated_at)
                    VALUES ('%s', 'role-conflict@carebridge.dev', '%s', now(), now())
                    """.formatted(userId, userRole));
            statement.executeUpdate("""
                    INSERT INTO user_roles (user_role_id, user_id, role_id, status, created_at)
                    VALUES (gen_random_uuid(), '%s', '%s', 'ACTIVE', now())
                    """.formatted(userId, roleId));
        }
    }

    private static void assertRollbackState(
            PostgreSQLContainer postgres, UUID userId, String expectedRole) throws Exception {
        try (var connection = connection(postgres);
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT role, to_regclass('public.roles'),
                            to_regclass('public.user_roles')
                       FROM users WHERE user_id = '%s'
                     """.formatted(userId))) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo(expectedRole);
            assertThat(result.getString(2)).isEqualTo("roles");
            assertThat(result.getString(3)).isEqualTo("user_roles");
        }
    }

    private static void assertNullRoleRollbackState(
            PostgreSQLContainer postgres, UUID userId) throws Exception {
        try (var connection = connection(postgres);
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT role, to_regclass('public.roles'),
                            to_regclass('public.user_roles')
                       FROM users WHERE user_id = '%s'
                     """.formatted(userId))) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isNull();
            assertThat(result.getString(2)).isEqualTo("roles");
            assertThat(result.getString(3)).isEqualTo("user_roles");
        }
    }

    private static java.sql.Connection connection(PostgreSQLContainer postgres) throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static Flyway flyway(PostgreSQLContainer postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
