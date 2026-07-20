package com.carebridge.backend.journey;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
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

    @Autowired IJourneyTransitionService transitionService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedCommittedPregnancy() {
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
                """, OWNER_ID, "story63.concurrent@test.carebridge.local");
        jdbcTemplate.update("""
                INSERT INTO public.mother_journeys (
                    journey_id, owner_user_id, journey_type, status, version,
                    date_source, date_confidence, created_at, updated_at
                ) VALUES (?, ?, 'PREGNANCY', 'ACTIVE', 0,
                    'SELF_REPORTED', 'ESTIMATED', now(), now())
                """, JOURNEY_ID, OWNER_ID);
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
                "SELECT count(*) FROM pregnancy_outcome_evidence WHERE journey_id = ?",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_transitions WHERE journey_id = ?",
                Long.class, JOURNEY_ID)).isEqualTo(1L);
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
