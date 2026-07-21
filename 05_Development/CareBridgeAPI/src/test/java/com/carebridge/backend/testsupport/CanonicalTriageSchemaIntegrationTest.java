package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CanonicalTriageSchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void cleanBootstrapAndHibernateValidationKeepOnlyCanonicalTriagePersistence() {
        Boolean canonicalTriageOnly = jdbcTemplate.queryForObject("""
                SELECT to_regclass('public.intake_sessions') IS NOT NULL
                   AND to_regclass('public.structured_intake_data') IS NOT NULL
                   AND to_regclass('public.triage_answers') IS NULL
                   AND to_regclass('public.triage_assessments') IS NULL
                """, Boolean.class);

        assertThat(canonicalTriageOnly).isTrue();
    }
}
