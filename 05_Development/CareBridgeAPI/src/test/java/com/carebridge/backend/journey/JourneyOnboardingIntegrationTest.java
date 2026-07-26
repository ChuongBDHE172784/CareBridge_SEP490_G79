package com.carebridge.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.SubmitJourneyOnboardingRequest;
import com.carebridge.backend.journey.entity.LifecycleGoal;
import com.carebridge.backend.journey.entity.SupportPreference;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalAuditFixture;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = {
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret"
})
class JourneyOnboardingIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID OWNER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000162");
    private static final UUID SUBMISSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000016200");

    @Autowired IJourneyOnboardingService onboardingService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanAndSeedOwner() {
        CanonicalAuditFixture.deleteByActor(jdbcTemplate, OWNER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.data_permissions WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ?",
                OWNER_ID);
        deleteCanonicalEvents(OWNER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.persons (person_id, display_name, created_at, updated_at)
                VALUES (?, 'Story 62 Mother', now(), now())
                ON CONFLICT (person_id) DO NOTHING
                """, OWNER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, person_id, email, role, account_status, enabled, locked,
                    must_change_password, created_at, updated_at
                ) VALUES (?, ?, ?, 'MOTHER', 'ACTIVE', true, false, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """, OWNER_ID, OWNER_ID, "story62.mother@test.carebridge.local");
    }

    @Test
    void concurrentSameSubmissionProducesOneRevisionAndOneEvidence() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> submitAfterBarrier(ready, start));
            var second = executor.submit(() -> submitAfterBarrier(ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).getBaselineRevision()).isEqualTo(1L);
            assertThat(second.get(10, TimeUnit.SECONDS).getBaselineRevision()).isEqualTo(1L);
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE owner_user_id = ? AND legacy_source = 'MOTHER_BASELINE'",
                Long.class, OWNER_ID)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM data_permissions WHERE permission_kind='CONSENT_GRANT' "
                        + "AND owner_user_id = ? AND evidence_key = ?",
                Long.class, OWNER_ID, SUBMISSION_ID)).isEqualTo(1L);
    }

    @Test
    void deniedConsentRollsBackWithoutBaselineConsentOrAuditSideEffects() {
        SubmitJourneyOnboardingRequest request = validRequest();
        request.setConsentAccepted(false);

        assertThatThrownBy(() -> onboardingService.submit(OWNER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("LIFECYCLE_CONSENT_REQUIRED");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE owner_user_id = ? AND legacy_source = 'MOTHER_BASELINE'",
                Long.class, OWNER_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM data_permissions WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ?",
                Long.class, OWNER_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_origin='AUDIT_LOG' AND actor_user_id = ? "
                        + "AND event_category IN ('MOTHER_BASELINE_SUBMITTED', 'CONSENT_GRANTED')",
                Long.class, OWNER_ID)).isZero();
    }

    @Test
    void eligibilityWaitsForConcurrentRevocationAndThenFailsClosed() throws Exception {
        onboardingService.submit(OWNER_ID, validRequest());
        CountDownLatch revocationUpdated = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var revoke = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.queryForObject("""
                        SELECT 1 FROM pg_advisory_xact_lock(
                            hashtextextended(CAST(? AS text), 0))
                        """, Integer.class, OWNER_ID);
                jdbcTemplate.update("""
                        UPDATE data_permissions
                           SET revoked_at = now(), revoked_by = owner_user_id
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """, OWNER_ID, SUBMISSION_ID);
                revocationUpdated.countDown();
                try {
                    assertThat(allowCommit.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));

            assertThat(revocationUpdated.await(5, TimeUnit.SECONDS)).isTrue();
            var eligibility = executor.submit(() -> {
                try {
                    onboardingService.ensureEligible(OWNER_ID);
                    return null;
                } catch (BusinessException exception) {
                    return exception;
                }
            });

            assertThatThrownBy(() -> eligibility.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            allowCommit.countDown();
            revoke.get(5, TimeUnit.SECONDS);
            assertThat(eligibility.get(5, TimeUnit.SECONDS))
                    .isNotNull()
                    .extracting("code")
                    .isEqualTo("LIFECYCLE_CONSENT_INVALID");
        }
    }

    @ParameterizedTest(name = "persisted lifecycle consent fails closed when {0}")
    @MethodSource("invalidPersistedConsentMutations")
    void persistedInvalidConsentIsExcludedByPostgresQuery(
            String fixture, String mutationSql) {
        onboardingService.submit(OWNER_ID, validRequest());
        var validStatus = onboardingService.getStatus(OWNER_ID);
        assertThat(validStatus.isBaselineComplete()).isTrue();
        assertThat(validStatus.isConsentValid()).isTrue();

        jdbcTemplate.update(mutationSql, OWNER_ID, SUBMISSION_ID);

        assertThatThrownBy(() -> onboardingService.ensureEligible(OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("LIFECYCLE_CONSENT_INVALID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mother_journey_events "
                        + "WHERE owner_user_id = ? AND legacy_source = 'MOTHER_BASELINE'",
                Long.class, OWNER_ID)).isEqualTo(1L);
    }

    private void deleteCanonicalEvents(UUID ownerId) {
        jdbcTemplate.execute(
                "ALTER TABLE public.mother_journey_events DISABLE TRIGGER mother_journey_events_immutable_trg");
        try {
            jdbcTemplate.update(
                    "DELETE FROM public.mother_journey_events WHERE owner_user_id = ?", ownerId);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE public.mother_journey_events ENABLE TRIGGER mother_journey_events_immutable_trg");
        }
    }

    private static java.util.stream.Stream<Arguments> invalidPersistedConsentMutations() {
        return java.util.stream.Stream.of(
                Arguments.of("expired", """
                        UPDATE data_permissions SET expires_at = now() - interval '1 second'
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("at exact expiry boundary", """
                        UPDATE data_permissions SET expires_at = now()
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("revoked", """
                        UPDATE data_permissions SET revoked_at = now(), revoked_by = owner_user_id
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("wrong purpose", """
                        UPDATE data_permissions SET purpose = 'VIEW'
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("wrong scope", """
                        UPDATE data_permissions SET scope_text = 'journey:other'
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("wrong policy version", """
                        UPDATE data_permissions SET policy_version = 'MOTHER_LIFECYCLE_V0'
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """),
                Arguments.of("wrong data type", """
                        UPDATE data_permissions SET scope_type = 'HEALTH_RECORD'
                         WHERE permission_kind='CONSENT_GRANT' AND owner_user_id = ? AND evidence_key = ?
                        """));
    }

    private com.carebridge.backend.journey.dto.JourneyOnboardingStatusResponse submitAfterBarrier(
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return onboardingService.submit(OWNER_ID, validRequest());
    }

    private SubmitJourneyOnboardingRequest validRequest() {
        SubmitJourneyOnboardingRequest request = new SubmitJourneyOnboardingRequest();
        request.setSubmissionId(SUBMISSION_ID);
        request.setLifecycleGoal(LifecycleGoal.CURRENTLY_PREGNANT);
        request.setLocale("vi-VN");
        request.setTimeZone("Asia/Ho_Chi_Minh");
        request.setPreferences(List.of(SupportPreference.NUTRITION));
        request.setConsentAccepted(true);
        request.setPolicyVersion("MOTHER_LIFECYCLE_V1");
        return request;
    }
}
