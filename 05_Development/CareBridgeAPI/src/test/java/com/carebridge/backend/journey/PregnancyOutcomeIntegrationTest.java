package com.carebridge.backend.journey;

import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
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

    @Autowired IJourneyTransitionService transitionService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAndSeedPregnancy() {
        jdbcTemplate.update("DELETE FROM public.audit_logs WHERE actor_user_id = ?", OWNER_ID);
        jdbcTemplate.update("DELETE FROM public.pregnancy_outcome_evidence WHERE journey_id = ?", JOURNEY_ID);
        jdbcTemplate.update("DELETE FROM public.mother_journey_transitions WHERE journey_id = ?", JOURNEY_ID);
        jdbcTemplate.update("DELETE FROM public.mother_journeys WHERE journey_id = ?", JOURNEY_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, email, role, account_status, enabled, locked,
                    must_change_password, created_at, updated_at
                ) VALUES (?, ?, 'MOTHER', 'ACTIVE', true, false, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """, OWNER_ID, "story63.mother@test.carebridge.local");
        jdbcTemplate.update("""
                INSERT INTO public.mother_journeys (
                    journey_id, owner_user_id, journey_type, status, version,
                    date_source, date_confidence, created_at, updated_at
                ) VALUES (?, ?, 'PREGNANCY', 'ACTIVE', 0,
                    'SELF_REPORTED', 'ESTIMATED', now(), now())
                """, JOURNEY_ID, OWNER_ID);
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
                "SELECT count(*) FROM pregnancy_outcome_evidence WHERE journey_id = ?",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_transitions "
                        + "WHERE journey_id = ? AND event_type = 'OUTCOME_RECORDED'",
                Long.class, JOURNEY_ID)).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE pregnancy_outcome_evidence SET reason = 'changed' WHERE journey_id = ?",
                JOURNEY_ID))
                .hasMessageContaining("pregnancy outcome evidence is append-only");
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
                "SELECT count(*) FROM pregnancy_outcome_evidence "
                        + "WHERE journey_id = ? AND submission_id = ?",
                Long.class, JOURNEY_ID, submissionId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_transitions WHERE journey_id = ?",
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
                "SELECT count(*) FROM pregnancy_outcome_evidence WHERE journey_id = ?",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_transitions WHERE journey_id = ?",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
    }

    @Test
    void databaseRejectsEvidenceWhoseOwnerDoesNotMatchJourneyOwner() {
        UUID differentOwner = UUID.fromString("00000000-0000-0000-0000-000000016399");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO pregnancy_outcome_evidence (
                    evidence_id, journey_id, owner_user_id, submission_id, outcome_type,
                    source, actor_user_id, reason, effective_at, revision_number,
                    journey_version, semantic_hash, correction
                ) VALUES (?, ?, ?, ?, 'ONGOING', 'SELF_REPORTED', ?,
                    'Synthetic owner mismatch', now(), 1, 0, 'synthetic', false)
                """,
                UUID.randomUUID(), JOURNEY_ID, differentOwner, UUID.randomUUID(), differentOwner))
                .hasMessageContaining("pregnancy outcome evidence owner must match journey owner");
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
