package com.carebridge.backend.journey;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalAuditFixture;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret"
})
@Execution(ExecutionMode.SAME_THREAD)
class JourneyConsentMutationPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID OWNER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000610210");
    private static final UUID SUBMISSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000610211");

    @Autowired IJourneyTransitionService transitionService;
    @Autowired MotherJourneyRepository journeyRepository;
    @Autowired MotherJourneyTransitionRepository transitionRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ConsentService consentService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedEligibleMother() {
        CanonicalAuditFixture.deleteByActor(jdbcTemplate, OWNER_ID);
        jdbcTemplate.update(
                "UPDATE public.care_subjects SET mother_journey_id = NULL "
                        + "WHERE owner_user_id = ? AND subject_type = 'MOTHER'",
                OWNER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.mother_journeys WHERE owner_user_id = ?",
                OWNER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.care_subjects WHERE owner_user_id = ? AND subject_type = 'MOTHER'",
                OWNER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.data_permissions WHERE owner_user_id = ? "
                        + "AND permission_kind = 'CONSENT_GRANT' AND scope_type = 'MOTHER_BASELINE'",
                OWNER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, person_id, display_name, email, role, account_status,
                    enabled, locked, email_verified, phone_verified, created_at, updated_at
                ) VALUES (?, ?, 'Consent Mutation Mother', ?, 'MOTHER', 'ACTIVE',
                    true, false, true, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """, OWNER_ID, OWNER_ID, "consent.mutation@test.carebridge.local");
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
                """.formatted(SUBMISSION_ID));
        jdbcTemplate.update("""
                INSERT INTO public.data_permissions (
                    permission_kind, owner_user_id, scope_type, purpose, scope_text,
                    policy_version, evidence_key, locale, granted_at, expires_at,
                    version_number, status, created_at, updated_at
                ) VALUES ('CONSENT_GRANT', ?, 'MOTHER_BASELINE', 'PERSONALIZE',
                    'STORE_BASELINE_AND_PERSONALIZE_MOTHER_LIFECYCLE',
                    'MOTHER_LIFECYCLE_V1', ?, 'vi-VN', now(),
                    now() + interval '30 days', 1, 'ACTIVE', now(), now())
                """, OWNER_ID, SUBMISSION_ID);
    }

    @Test
    void revokedConsentRejectsJourneyUpdateWithoutCurrentHistoryOrAuditMutation() {
        var created = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(), OWNER_ID);
        long versionBefore = created.getVersion();
        JourneyType stageBefore = JourneyType.valueOf(created.getJourneyType());
        long transitionsBefore = transitionRepository.countByJourneyId(created.getId());
        Long consentId = lifecycleConsentId();
        consentService.revokeConsent(OWNER_ID, consentId);

        assertConsentInvalid(() -> transitionService.updateJourney(
                OWNER_ID, created.getId(), JourneyLifecycleTestFactory.dateCorrection()));

        assertThat(journeyRepository.findById(created.getId()).orElseThrow())
                .satisfies(journey -> {
                    assertThat(journey.getVersion()).isEqualTo(versionBefore);
                    assertThat(journey.getJourneyType()).isEqualTo(stageBefore);
                });
        assertThat(transitionRepository.countByJourneyId(created.getId()))
                .isEqualTo(transitionsBefore);
        assertThat(auditLogRepository.findByEntityIdAndAction(
                created.getId(), AuditAction.JOURNEY_UPDATED)).isEmpty();
    }

    @Test
    void expiredConsentRejectsPregnancyOutcomeWithoutCurrentHistoryEvidenceOrAuditMutation() {
        var created = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(), OWNER_ID);
        long versionBefore = created.getVersion();
        JourneyType stageBefore = JourneyType.valueOf(created.getJourneyType());
        long transitionsBefore = transitionRepository.countByJourneyId(created.getId());
        jdbcTemplate.update("""
                UPDATE public.data_permissions
                SET expires_at = now() - interval '1 minute', status = 'EXPIRED', updated_at = now()
                WHERE owner_user_id = ? AND evidence_key = ?
                  AND permission_kind = 'CONSENT_GRANT'
                """, OWNER_ID, SUBMISSION_ID);

        assertConsentInvalid(() -> transitionService.recordPregnancyOutcome(
                OWNER_ID, created.getId(), liveBirthRequest(created.getVersion())));

        assertThat(journeyRepository.findById(created.getId()).orElseThrow())
                .satisfies(journey -> {
                    assertThat(journey.getVersion()).isEqualTo(versionBefore);
                    assertThat(journey.getJourneyType()).isEqualTo(stageBefore);
                    assertThat(journey.getPregnancyOutcome()).isNull();
                });
        assertThat(transitionRepository.countByJourneyId(created.getId()))
                .isEqualTo(transitionsBefore);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.audit_events
                WHERE subject_reference_id = ? AND event_category = 'PREGNANCY_OUTCOME_EVIDENCE'
                """, Long.class, created.getId())).isZero();
        assertThat(auditLogRepository.findByEntityIdAndAction(
                created.getId(), AuditAction.PREGNANCY_OUTCOME_RECORDED)).isEmpty();
    }

    private Long lifecycleConsentId() {
        return jdbcTemplate.queryForObject("""
                SELECT legacy_consent_id FROM public.data_permissions
                WHERE permission_kind = 'CONSENT_GRANT'
                  AND owner_user_id = ? AND evidence_key = ?
                """, Long.class, OWNER_ID, SUBMISSION_ID);
    }

    private RecordPregnancyOutcomeRequest liveBirthRequest(long expectedVersion) {
        var request = new RecordPregnancyOutcomeRequest();
        request.setSubmissionId(UUID.fromString("00000000-0000-0000-0000-000000610212"));
        request.setExpectedJourneyVersion(expectedVersion);
        request.setOutcomeType(PregnancyOutcomeType.LIVE_BIRTH);
        request.setOutcomeDate(LocalDate.now().minusDays(1));
        request.setSource(JourneyDateSource.SELF_REPORTED);
        request.setReason("Synthetic consent mutation regression");
        request.setEffectiveAt(Instant.now());
        return request;
    }

    private void assertConsentInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("LIFECYCLE_CONSENT_INVALID");
                });
    }
}
