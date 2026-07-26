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
                INSERT INTO public.persons (person_id, display_name, created_at, updated_at)
                VALUES (?, 'Story 63 Mother', now(), now())
                ON CONFLICT (person_id) DO NOTHING
                """, OWNER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, person_id, email, role, account_status, enabled, locked,
                    must_change_password, created_at, updated_at
                ) VALUES (?, ?, ?, 'MOTHER', 'ACTIVE', true, false, false, now(), now())
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
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'PREGNANCY_OUTCOME'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'JOURNEY_TRANSITION' "
                        + "AND event_type = 'OUTCOME_RECORDED'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE mother_journey_events SET reason = 'changed' "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'PREGNANCY_OUTCOME'",
                JOURNEY_ID))
                .hasMessageContaining("IMMUTABLE_TABLE: public.mother_journey_events");
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
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND submission_id = ? "
                        + "AND legacy_source = 'PREGNANCY_OUTCOME'",
                Long.class, JOURNEY_ID, submissionId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'JOURNEY_TRANSITION'",
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
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'PREGNANCY_OUTCOME'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE mother_journey_id = ? AND legacy_source = 'JOURNEY_TRANSITION'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
    }

    @Test
    void databaseRejectsEvidenceWhoseOwnerDoesNotMatchJourneyOwner() {
        UUID differentOwner = UUID.fromString("00000000-0000-0000-0000-000000016399");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO mother_journey_events (
                    event_id, mother_journey_id, owner_user_id, event_type,
                    event_payload_jsonb, schema_version, submission_id, outcome_type,
                    event_source, actor_user_id, reason, effective_at, recorded_at,
                    revision_number, journey_version, semantic_hash, correction,
                    legacy_source, legacy_id
                ) VALUES (?, ?, ?, 'PREGNANCY_OUTCOME_EVIDENCE', '{}'::jsonb, '1', ?,
                    'ONGOING', 'SELF_REPORTED', ?, 'Synthetic owner mismatch', now(), now(),
                    1, 0, 'synthetic', false, 'PREGNANCY_OUTCOME', ?)
                """,
                UUID.randomUUID(), JOURNEY_ID, differentOwner, UUID.randomUUID(), differentOwner,
                UUID.randomUUID().toString()))
                .hasMessageContaining("mother journey event owner must match journey owner");
    }

    private void deleteCanonicalEvents() {
        jdbcTemplate.execute(
                "ALTER TABLE public.mother_journey_events DISABLE TRIGGER mother_journey_events_immutable_trg");
        try {
            jdbcTemplate.update(
                    "DELETE FROM public.mother_journey_events "
                            + "WHERE mother_journey_id = ? OR "
                            + "(owner_user_id = ? AND legacy_source = 'MOTHER_BASELINE')",
                    JOURNEY_ID, OWNER_ID);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE public.mother_journey_events ENABLE TRIGGER mother_journey_events_immutable_trg");
        }
    }

    private void seedLifecycleBaselineAndConsent() {
        jdbcTemplate.update("""
                INSERT INTO public.mother_journey_events (
                    event_id, owner_user_id, submission_id, journey_version,
                    schema_version, lifecycle_goal, locale, time_zone,
                    preferences, recorded_at, event_source, event_type,
                    event_payload_jsonb, effective_at, legacy_source, legacy_id
                ) VALUES (?, ?, ?, 1, 'MOTHER_BASELINE_V1',
                    'CURRENTLY_PREGNANT', 'vi-VN', 'Asia/Ho_Chi_Minh',
                    'NUTRITION', now(), 'SELF_REPORTED', 'BASELINE_CONTEXT',
                    '{}'::jsonb, now(), 'MOTHER_BASELINE', ?)
                """, UUID.randomUUID(), OWNER_ID, BASELINE_SUBMISSION_ID,
                BASELINE_SUBMISSION_ID.toString());
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
