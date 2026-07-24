package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class Postgresql18CanonicalSchemaIntegrationTest {

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine");

    @Test
    void cleanBootstrapKeepsCanonicalTableCountAndPassesHibernateValidation() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().success).isTrue();
        assertThat(tableCount()).isEqualTo(70);
        assertThat(legacyExpertProfileColumnCount()).isZero();
        assertThat(canonicalProfessionalProfileColumnCount()).isEqualTo(3);

        validateJpaMappings();
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

    private int legacyExpertProfileColumnCount() throws Exception {
        return profileColumnCount("expert_profile_id");
    }

    private int canonicalProfessionalProfileColumnCount() throws Exception {
        return profileColumnCount("professional_profile_id");
    }

    private int profileColumnCount(String columnName) throws Exception {
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
