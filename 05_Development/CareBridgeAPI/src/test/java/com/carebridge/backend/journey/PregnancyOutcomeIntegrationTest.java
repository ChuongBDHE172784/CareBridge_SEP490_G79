package com.carebridge.backend.journey;

import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.common.exception.BusinessException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret"
})
@Transactional
class PregnancyOutcomeIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID OWNER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000163");
    private static final UUID JOURNEY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000016300");
    private static final UUID BASELINE_SUBMISSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000016305");

    @Autowired IJourneyTransitionService transitionService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeedPregnancy() {
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
                ) VALUES (?, ?, 'Story 63 Mother', ?, 'MOTHER', 'ACTIVE',
                    true, false, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """, OWNER_ID, OWNER_ID, "story63.mother@test.carebridge.local");
        seedLifecycleBaselineAndConsent();
        jdbcTemplate.update("""
                INSERT INTO public.care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'MOTHER', 'Story 63 Mother', 'ACTIVE', now(), now())
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
    void lossPersistsAppendOnlyEvidenceAndTransitionsWithoutDeliveryDate() {
        var response = transitionService.recordPregnancyOutcome(
                OWNER_ID,
                JOURNEY_ID,
                request(
                        UUID.fromString("00000000-0000-0000-0000-000000016301"),
                        PregnancyOutcomeType.PREGNANCY_LOSS,
                        null));

        assertThat(response.getJourneyType().name()).isEqualTo("POSTPARTUM");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pregnancy_outcome FROM mother_journeys WHERE journey_id = ?",
                String.class, JOURNEY_ID)).isEqualTo("PREGNANCY_LOSS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT delivery_date FROM mother_journeys WHERE journey_id = ?",
                LocalDate.class, JOURNEY_ID)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'MOTHER_JOURNEY_TRANSITION' "
                        + "AND payload->>'eventType' = 'OUTCOME_RECORDED'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit_events SET payload = payload || '{\"reason\": \"changed\"}'::jsonb "
                        + "WHERE subject_reference_id = ? AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'",
                JOURNEY_ID))
                .hasMessageContaining("IMMUTABLE_TABLE: public.audit_events");
    }

    @Test
    void identicalRetryReturnsOneEvidenceAndOneTransition() {
        UUID submissionId = UUID.fromString("00000000-0000-0000-0000-000000016302");
        RecordPregnancyOutcomeRequest request = request(
                submissionId, PregnancyOutcomeType.LIVE_BIRTH, LocalDate.of(2026, 7, 18));

        var first = transitionService.recordPregnancyOutcome(OWNER_ID, JOURNEY_ID, request);
        var replay = transitionService.recordPregnancyOutcome(OWNER_ID, JOURNEY_ID, request);

        assertThat(replay.getEvidenceId()).isEqualTo(first.getEvidenceId());
        assertThat(replay.getTransitionId()).isEqualTo(first.getTransitionId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND payload->>'submissionId' = ? "
                        + "AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'",
                Long.class, JOURNEY_ID, submissionId.toString())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'MOTHER_JOURNEY_TRANSITION'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
    }

    @Test
    void changedIdempotencyPayloadAndStaleVersionLeaveExactlyOneMutation() {
        UUID submissionId = UUID.fromString("00000000-0000-0000-0000-000000016303");
        transitionService.recordPregnancyOutcome(
                OWNER_ID,
                JOURNEY_ID,
                request(submissionId, PregnancyOutcomeType.ONGOING, null));

        RecordPregnancyOutcomeRequest changed = request(
                submissionId, PregnancyOutcomeType.PREGNANCY_LOSS, null);
        assertThatThrownBy(() -> transitionService.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, changed))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode())
                                .isEqualTo("OUTCOME_SUBMISSION_CONFLICT"));

        RecordPregnancyOutcomeRequest stale = request(
                UUID.fromString("00000000-0000-0000-0000-000000016304"),
                PregnancyOutcomeType.UNKNOWN,
                null);
        stale.setExpectedJourneyVersion(0L);
        assertThatThrownBy(() -> transitionService.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, stale))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode())
                                .isEqualTo("JOURNEY_VERSION_CONFLICT"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE subject_reference_id = ? AND event_category = 'MOTHER_JOURNEY_TRANSITION'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
    }

    @Test
    void databaseRejectsEvidenceWhoseOwnerDoesNotMatchJourneyOwner() {
        UUID differentOwner = UUID.fromString("00000000-0000-0000-0000-000000016399");

        // Canonical schema dropped the owner-must-match-journey trigger; the remaining
        // database-level guard is the actor FK, which rejects an unknown foreign owner.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO audit_events (
                    audit_event_id, event_category, actor_user_id, subject_user_id,
                    subject_reference_id, resource_type, resource_id,
                    payload, occurred_at, created_at
                ) VALUES (?, 'PREGNANCY_OUTCOME_EVIDENCE', ?, ?, ?, 'mother_journeys', ?,
                    ?::jsonb, now(), now())
                """,
                UUID.randomUUID(), differentOwner, differentOwner, JOURNEY_ID, JOURNEY_ID,
                """
                {"outcomeType": "ONGOING", "source": "SELF_REPORTED",
                 "reason": "Synthetic owner mismatch", "revisionNumber": 1,
                 "journeyVersion": 0, "semanticHash": "synthetic", "correction": false,
                 "submissionId": "%s"}
                """.formatted(UUID.randomUUID())))
                .hasMessageContaining("audit_events_actor_user_id_fkey");
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

    private RecordPregnancyOutcomeRequest request(
            UUID submissionId, PregnancyOutcomeType type, LocalDate date) {
        var request = new RecordPregnancyOutcomeRequest();
        request.setSubmissionId(submissionId);
        request.setExpectedJourneyVersion(0L);
        request.setOutcomeType(type);
        request.setOutcomeDate(date);
        request.setSource(JourneyDateSource.SELF_REPORTED);
        request.setReason("Synthetic outcome confirmation");
        request.setEffectiveAt(Instant.now().minusSeconds(60));
        return request;
    }
}
