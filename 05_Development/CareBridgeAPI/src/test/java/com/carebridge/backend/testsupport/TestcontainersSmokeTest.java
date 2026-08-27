package com.carebridge.backend.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal smoke test: proves the Spring context boots against a real PostgreSQL
 * Testcontainer with Flyway migrations applied. If this is green, the INT-* suites
 * can rely on the same infrastructure.
 */
class TestcontainersSmokeTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAgainstRealPostgres() {
        assertThat(POSTGRES.isRunning()).isTrue();

        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);

        // Flyway ran the real migrations, so the core users table must exist.
        Integer userTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
                Integer.class);
        assertThat(userTableCount).isEqualTo(1);
    }
}
