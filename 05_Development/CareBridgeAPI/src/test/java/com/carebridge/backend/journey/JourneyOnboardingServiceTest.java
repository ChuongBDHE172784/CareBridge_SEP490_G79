package com.carebridge.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.journey.dto.SubmitJourneyOnboardingRequest;
import com.carebridge.backend.journey.entity.LifecycleGoal;
import com.carebridge.backend.journey.entity.MotherBaselineContext;
import com.carebridge.backend.journey.entity.SupportPreference;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import com.carebridge.backend.journey.service.impl.JourneyOnboardingServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class JourneyOnboardingServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000062");
    private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000006200");
    private static final Instant NOW = Instant.parse("2026-07-18T10:00:00Z");

    @Mock private MotherBaselineContextRepository baselineRepository;
    @Mock private ConsentGrantRepository consentRepository;
    @Mock private AuditService auditService;

    private JourneyOnboardingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JourneyOnboardingServiceImpl(
                baselineRepository,
                consentRepository,
                auditService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void submitPersistsVersionedBaselineAndScopedConsent() {
        when(baselineRepository.findByOwnerUserIdAndSubmissionId(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(USER_ID))
                .thenReturn(Optional.empty());
        when(baselineRepository.save(any())).thenAnswer(invocation -> {
            MotherBaselineContext context = invocation.getArgument(0);
            context.setId(UUID.randomUUID());
            return context;
        });
        when(consentRepository.findLifecycleEvidenceByKey(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(consentRepository.save(any())).thenAnswer(invocation -> {
            ConsentGrant grant = invocation.getArgument(0);
            grant.setId(62L);
            return grant;
        });

        var result = service.submit(USER_ID, validRequest());

        assertThat(result.isBaselineComplete()).isTrue();
        assertThat(result.isConsentValid()).isTrue();
        assertThat(result.getBaselineRevision()).isEqualTo(1L);
        verify(baselineRepository).save(org.mockito.ArgumentMatchers.argThat(
                baseline -> "SELF_REPORTED".equals(baseline.getSource())));
        verify(baselineRepository).save(any(MotherBaselineContext.class));
        verify(consentRepository).save(any(ConsentGrant.class));
        verify(baselineRepository).acquireOwnerLock(USER_ID);
    }

    @Test
    void submitIsIdempotentForSameAccountAndSubmissionId() {
        MotherBaselineContext existing = MotherBaselineContext.builder()
                .id(UUID.randomUUID())
                .ownerUserId(USER_ID)
                .submissionId(SUBMISSION_ID)
                .revision(3L)
                .schemaVersion("MOTHER_BASELINE_V1")
                .source("SELF_REPORTED")
                .lifecycleGoal(LifecycleGoal.PREPARING_FOR_PREGNANCY)
                .locale("vi-VN")
                .timeZone("Asia/Ho_Chi_Minh")
                .preferences("NUTRITION")
                .recordedAt(NOW)
                .build();
        ConsentGrant evidence = ConsentGrant.builder()
                .id(62L)
                .userId(USER_ID)
                .dataType(ConsentDataType.MOTHER_BASELINE)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(JourneyOnboardingServiceImpl.LIFECYCLE_SCOPE)
                .policyVersion(JourneyOnboardingServiceImpl.POLICY_VERSION)
                .locale("vi-VN")
                .consentGivenAt(NOW)
                .expiryAt(NOW.plusSeconds(3600))
                .build();
        when(baselineRepository.findByOwnerUserIdAndSubmissionId(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(existing));
        when(consentRepository.findLifecycleEvidenceByKey(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(evidence));

        var result = service.submit(USER_ID, validRequest());

        assertThat(result.getBaselineRevision()).isEqualTo(3L);
        verify(baselineRepository, never()).save(any());
        verify(consentRepository, never()).save(any());
        verify(baselineRepository).acquireOwnerLock(USER_ID);
    }

    @Test
    void changedPayloadForExistingSubmissionReturnsConflict() {
        MotherBaselineContext existing = MotherBaselineContext.builder()
                .id(UUID.randomUUID()).ownerUserId(USER_ID).submissionId(SUBMISSION_ID)
                .revision(1L).schemaVersion("MOTHER_BASELINE_V1").source("SELF_REPORTED")
                .lifecycleGoal(LifecycleGoal.CURRENTLY_PREGNANT).locale("vi-VN")
                .timeZone("Asia/Ho_Chi_Minh").preferences("NUTRITION").recordedAt(NOW).build();
        ConsentGrant evidence = ConsentGrant.builder().id(62L).userId(USER_ID)
                .dataType(ConsentDataType.MOTHER_BASELINE).purpose(ConsentPurpose.PERSONALIZE)
                .scope(JourneyOnboardingServiceImpl.LIFECYCLE_SCOPE)
                .policyVersion(JourneyOnboardingServiceImpl.POLICY_VERSION).locale("vi-VN")
                .consentGivenAt(NOW).expiryAt(NOW.plusSeconds(3600)).build();
        when(baselineRepository.findByOwnerUserIdAndSubmissionId(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(existing));
        when(consentRepository.findLifecycleEvidenceByKey(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(evidence));

        assertThatThrownBy(() -> service.submit(USER_ID, validRequest()))
                .isInstanceOf(BusinessException.class).extracting("code")
                .isEqualTo("ONBOARDING_SUBMISSION_CONFLICT");
    }

    @Test
    void duplicatePreferencesAreRejected() {
        SubmitJourneyOnboardingRequest request = validRequest();
        request.setPreferences(List.of(SupportPreference.NUTRITION, SupportPreference.NUTRITION));

        assertThatThrownBy(() -> service.submit(USER_ID, request))
                .isInstanceOf(BusinessException.class).extracting("code")
                .isEqualTo("DUPLICATE_SUPPORT_PREFERENCE");
        verify(baselineRepository, never()).save(any());
    }

    @Test
    void deniedConsentLeavesNoPartialBaseline() {
        SubmitJourneyOnboardingRequest request = validRequest();
        request.setConsentAccepted(false);

        assertThatThrownBy(() -> service.submit(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("LIFECYCLE_CONSENT_REQUIRED");
        verify(baselineRepository, never()).save(any());
        verify(consentRepository, never()).save(any());
    }

    @Test
    void missingBaselineFailsJourneyEligibilityWithoutWriting() {
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureEligible(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("BASELINE_REQUIRED");
        verify(baselineRepository, never()).save(any());
        verify(consentRepository, never()).save(any());
    }

    @Test
    void revokedOrExpiredConsentFailsJourneyEligibility() {
        MotherBaselineContext baseline = MotherBaselineContext.builder()
                .id(UUID.randomUUID())
                .ownerUserId(USER_ID)
                .revision(1L)
                .submissionId(SUBMISSION_ID)
                .build();
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(USER_ID))
                .thenReturn(Optional.of(baseline));
        ConsentGrant latestEvidence = ConsentGrant.builder()
                .id(63L)
                .userId(USER_ID)
                .dataType(ConsentDataType.MOTHER_BASELINE)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(JourneyOnboardingServiceImpl.LIFECYCLE_SCOPE)
                .policyVersion(JourneyOnboardingServiceImpl.POLICY_VERSION)
                .consentGivenAt(NOW.minusSeconds(120))
                .expiryAt(NOW.plusSeconds(3600))
                .revokedAt(NOW.minusSeconds(1))
                .build();
        when(consentRepository.findLifecycleEvidenceByKey(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(latestEvidence));
        when(consentRepository.existsLifecycleEvidence(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureEligible(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("LIFECYCLE_CONSENT_INVALID");
    }

    @Test
    void missingConsentUsesRequiredErrorCode() {
        MotherBaselineContext baseline = MotherBaselineContext.builder()
                .id(UUID.randomUUID())
                .ownerUserId(USER_ID)
                .revision(1L)
                .submissionId(SUBMISSION_ID)
                .build();
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(USER_ID))
                .thenReturn(Optional.of(baseline));
        when(consentRepository.findLifecycleEvidenceByKey(USER_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(consentRepository.existsLifecycleEvidence(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.ensureEligible(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("LIFECYCLE_CONSENT_REQUIRED");
    }

    @Test
    void consentStoreFailureFailsClosedWithStableUnavailableCode() {
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(USER_ID))
                .thenThrow(new DataAccessResourceFailureException("synthetic outage"));

        assertThatThrownBy(() -> service.ensureEligible(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ONBOARDING_ELIGIBILITY_UNAVAILABLE");
    }

    private SubmitJourneyOnboardingRequest validRequest() {
        SubmitJourneyOnboardingRequest request = new SubmitJourneyOnboardingRequest();
        request.setSubmissionId(SUBMISSION_ID);
        request.setLifecycleGoal(LifecycleGoal.PREPARING_FOR_PREGNANCY);
        request.setLocale("vi-VN");
        request.setTimeZone("Asia/Ho_Chi_Minh");
        request.setPreferences(List.of(SupportPreference.NUTRITION));
        request.setConsentAccepted(true);
        request.setPolicyVersion("MOTHER_LIFECYCLE_V1");
        return request;
    }
}
