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
                SELECT to_regclass('public.triage_sessions') IS NOT NULL
                   -- V2__drop_legacy_triage_evidence drops triage_session_evidence as
                   -- legacy, so it belongs with the tables that must be gone, not with
                   -- the canonical ones. This assertion still listed it as required.
                   AND to_regclass('public.triage_session_evidence') IS NULL
                   AND to_regclass('public.intake_sessions') IS NULL
                   AND to_regclass('public.structured_intake_data') IS NULL
                   AND to_regclass('public.triage_answers') IS NULL
                   AND to_regclass('public.triage_assessments') IS NULL
                """, Boolean.class);

        assertThat(canonicalTriageOnly).isTrue();
    }
}
