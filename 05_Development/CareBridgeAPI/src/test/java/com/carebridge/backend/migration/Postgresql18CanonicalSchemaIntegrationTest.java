package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Canonical fresh-bootstrap contract: the full Flyway chain builds the schema
 * from an empty PostgreSQL 18 database — 57 base tables (including
 * flyway_schema_history), the three surviving compatibility views, and a schema
 * Hibernate validates against every mapped entity.
 *
 * <p>The count dropped from 62 when the consolidation programme retired
 * partner_organizations, account_deletion_requests, account_lock_appeals,
 * device_connections and archived_records, and the nearby_support_interactions
 * view went with them.
 */
@Testcontainers(disabledWithoutDocker = true)
class Postgresql18CanonicalSchemaIntegrationTest {

    private static final int CANONICAL_BASE_TABLE_COUNT = 57;

    private static final List<String> COMPATIBILITY_VIEWS = List.of(
            "care_logs",
            "emergency_contacts",
            "expert_credentials");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine");

    @Test
    void cleanBootstrapKeepsCanonicalTableCountAndPassesHibernateValidation() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().success).isTrue();
        assertThat(tableCount()).isEqualTo(CANONICAL_BASE_TABLE_COUNT);
        assertThat(publicViews()).containsExactlyElementsOf(COMPATIBILITY_VIEWS);
        assertThat(baseTableExists("direct_conversations")).isTrue();
        assertThat(baseTableExists("ai_moderation_policies")).isTrue();
        assertThat(baseTableExists("persons")).isFalse();
        assertThat(baseTableExists("professional_profiles")).isFalse();

        // Retired by the database consolidation programme (V3 §3.1-§3.4).
        assertThat(baseTableExists("partner_organizations")).isFalse();
        assertThat(baseTableExists("account_deletion_requests")).isFalse();
        assertThat(baseTableExists("account_lock_appeals")).isFalse();
        assertThat(baseTableExists("device_connections")).isFalse();
        assertThat(baseTableExists("archived_records")).isFalse();

        // Explicitly retained by the same programme (plan §4.14) — these look
        // similar to the objects above and must not be swept up with them.
        assertThat(baseTableExists("device_tokens")).isTrue();
        assertThat(baseTableExists("direct_conversation_read_cursors")).isTrue();
        assertThat(baseTableExists("reminder_occurrence_aliases")).isTrue();
        assertThat(legacyExpertProfileColumnCount()).isZero();
        assertThat(canonicalExpertUserIdColumnCount()).isEqualTo(3);
        assertThat(canonicalProfessionalProfileMirrorColumnCount()).isEqualTo(2);

        validateJpaMappings();
    }

    private List<String> publicViews() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT table_name
                          FROM information_schema.views
                         WHERE table_schema = 'public'
                         ORDER BY table_name
                        """)) {
            List<String> views = new ArrayList<>();
            while (result.next()) {
                views.add(result.getString(1));
            }
            return List.copyOf(views);
        }
    }

    private boolean baseTableExists(String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("""
                        SELECT count(*)
                          FROM information_schema.tables
                         WHERE table_schema = 'public'
                           AND table_type = 'BASE TABLE'
                           AND table_name = ?
                        """)) {
            statement.setString(1, tableName);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1) == 1;
            }
        }
    }

    private int tableCount() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT count(*)
                          FROM information_schema.tables
                         WHERE table_schema = 'public'
                           AND table_type = 'BASE TABLE'
                        """)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    /** The retired expert_profile_id namespace must not resurface anywhere. */
    private int legacyExpertProfileColumnCount() throws Exception {
        return expertRuntimeColumnCount("expert_profile_id");
    }

    /** All three expert runtime relations expose the canonical user_id owner column. */
    private int canonicalExpertUserIdColumnCount() throws Exception {
        return expertRuntimeColumnCount("user_id");
    }

    /**
     * Only the two base tables keep the professional_profile_id mirror column
     * (its value is the owning user_id); the expert_credentials compatibility
     * view exposes user_id alone.
     */
    private int canonicalProfessionalProfileMirrorColumnCount() throws Exception {
        return expertRuntimeColumnCount("professional_profile_id");
    }

    private int expertRuntimeColumnCount(String columnName) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("""
                        SELECT count(*)
                          FROM information_schema.columns
                         WHERE table_schema = 'public'
                           AND table_name IN (
                               'expert_credentials',
                               'expert_availability',
                               'expert_location_shares')
                           AND column_name = ?
                        """)) {
            statement.setString(1, columnName);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }

    private void validateJpaMappings() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.carebridge.backend");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        var properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        factory.setJpaProperties(properties);

        try {
            factory.afterPropertiesSet();
            assertThat(factory.getObject().isOpen()).isTrue();
        } finally {
            factory.destroy();
        }
    }
}
