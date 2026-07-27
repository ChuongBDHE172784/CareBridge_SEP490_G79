package com.carebridge.backend.journey;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalAuditFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret"
})
class PregnancyOutcomeConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID OWNER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000164");
    private static final UUID JOURNEY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000016400");
    private static final UUID BASELINE_SUBMISSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000016405");

    @Autowired IJourneyTransitionService transitionService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedCommittedPregnancy() {
        CanonicalAuditFixture.deleteByActor(jdbcTemplate, OWNER_ID);
        deleteCanonicalEvents();
        jdbcTemplate.update("""
                DELETE FROM public.data_permissions
                WHERE owner_user_id = ? AND permission_kind = 'CONSENT_GRANT'
                  AND scope_type = 'MOTHER_BASELINE'
                """, OWNER_ID);
        jdbcTemplate.update("DELETE FROM public.mother_journeys WHERE journey_id = ?", JOURNEY_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, person_id, display_name, email, role, account_status,
                    enabled, locked, must_change_password, created_at, updated_at
                ) VALUES (?, ?, 'Story 63 Concurrent Mother', ?, 'MOTHER', 'ACTIVE',
                    true, false, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """, OWNER_ID, OWNER_ID, "story63.concurrent@test.carebridge.local");
        seedLifecycleBaselineAndConsent();
        jdbcTemplate.update("""
                INSERT INTO public.care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'MOTHER', 'Story 63 Concurrent Mother', 'ACTIVE', now(), now())
                ON CONFLICT (care_subject_id) DO NOTHING
                """, JOURNEY_ID, OWNER_ID, OWNER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.mother_journeys (
                    journey_id, care_subject_id, owner_user_id, journey_type, status, version,
                    date_source, date_confidence, created_at, updated_at
                ) VALUES (?, ?, ?, 'PREGNANCY', 'ACTIVE', 0,
                    'SELF_REPORTED', 'ESTIMATED', now(), now())
                """, JOURNEY_ID, JOURNEY_ID, OWNER_ID);
    }

    @Test
    void conflictingConcurrentOutcomesCommitExactlyOnce() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> ongoing = () -> submitAfterBarrier(
                    barrier,
                    UUID.fromString("00000000-0000-0000-0000-000000016401"),
                    PregnancyOutcomeType.ONGOING);
            Callable<String> loss = () -> submitAfterBarrier(
                    barrier,
                    UUID.fromString("00000000-0000-0000-0000-000000016402"),
                    PregnancyOutcomeType.PREGNANCY_LOSS);

            var results = executor.invokeAll(List.of(ongoing, loss)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertThat(results).containsExactlyInAnyOrder("COMMITTED", "JOURNEY_VERSION_CONFLICT");
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'MOTHER_JOURNEY_TRANSITION'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
    }

    private void deleteCanonicalEvents() {
        jdbcTemplate.execute(
                "ALTER TABLE public.audit_events DISABLE TRIGGER audit_events_immutable_trg");
        try {
            jdbcTemplate.update(
                    "DELETE FROM public.audit_events "
                            + "WHERE subject_reference_id = ? OR "
                            + "(actor_user_id = ? AND event_category = 'BASELINE_CONTEXT')",
                    JOURNEY_ID, OWNER_ID);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE public.audit_events ENABLE TRIGGER audit_events_immutable_trg");
        }
    }

    private void seedLifecycleBaselineAndConsent() {
        jdbcTemplate.update("""
                INSERT INTO public.audit_events (
                    audit_event_id, event_category, actor_user_id, resource_type,
                    payload, occurred_at, created_at
                ) VALUES (?, 'BASELINE_CONTEXT', ?, 'mother_journeys', ?::jsonb, now(), now())
                """, UUID.randomUUID(), OWNER_ID,
                """
                {"revision": 1, "schemaVersion": "MOTHER_BASELINE_V1",
                 "lifecycleGoal": "CURRENTLY_PREGNANT", "locale": "vi-VN",
                 "timeZone": "Asia/Ho_Chi_Minh", "preferences": "NUTRITION",
                 "source": "SELF_REPORTED", "submissionId": "%s"}
                """.formatted(BASELINE_SUBMISSION_ID));
        jdbcTemplate.update("""
                INSERT INTO public.data_permissions (
                    permission_kind, owner_user_id, scope_type, purpose, scope_text,
                    policy_version, evidence_key, locale, granted_at, expires_at,
                    version_number, status, created_at, updated_at
                ) VALUES ('CONSENT_GRANT', ?, 'MOTHER_BASELINE', 'PERSONALIZE',
                    'STORE_BASELINE_AND_PERSONALIZE_MOTHER_LIFECYCLE',
                    'MOTHER_LIFECYCLE_V1', ?, 'vi-VN', now(),
                    now() + interval '30 days', 1, 'ACTIVE', now(), now())
                """, OWNER_ID, BASELINE_SUBMISSION_ID);
    }

    private String submitAfterBarrier(
            CyclicBarrier barrier, UUID submissionId, PregnancyOutcomeType outcome)
            throws Exception {
        barrier.await();
        try {
            transitionService.recordPregnancyOutcome(
                    OWNER_ID, JOURNEY_ID, request(submissionId, outcome));
            return "COMMITTED";
        } catch (BusinessException exception) {
            return exception.getCode();
        }
    }

    private RecordPregnancyOutcomeRequest request(
            UUID submissionId, PregnancyOutcomeType outcome) {
        var request = new RecordPregnancyOutcomeRequest();
        request.setSubmissionId(submissionId);
        request.setExpectedJourneyVersion(0L);
        request.setOutcomeType(outcome);
        request.setSource(JourneyDateSource.SELF_REPORTED);
        request.setReason("Synthetic concurrent outcome");
        request.setEffectiveAt(Instant.now().minusSeconds(60));
        return request;
    }
}
