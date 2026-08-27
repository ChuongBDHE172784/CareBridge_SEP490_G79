package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.HandoffPreviewResponse;
import com.carebridge.backend.consultation.context.dto.SharedCitationResponse;
import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.entity.ConsultationContextCitation;
import com.carebridge.backend.consultation.context.entity.ConsultationContextShare;
import com.carebridge.backend.consultation.context.exception.TriageExpertHandoffException;
import com.carebridge.backend.consultation.context.policy.TriageExpertHandoffPolicy;
import com.carebridge.backend.consultation.context.repository.ConsultationContextCitationRepository;
import com.carebridge.backend.consultation.context.repository.ConsultationContextShareRepository;
import com.carebridge.backend.consultation.context.service.TriageCitationResolver;
import com.carebridge.backend.consultation.context.service.TriageExpertHandoffService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.service.CreateConsultationRequestResult;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ITriageService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TriageExpertHandoffServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID INTAKE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID KEY = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SHARE_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID BABY_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000702");
    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-07-25T00:00:00Z");

    @Mock private IIntakeSessionRepository intakeRepository;
    @Mock private ITriageService triageService;
    @Mock private IConsultationRequestService consultationRequestService;
    @Mock private ConsultationRequestRepository consultationRequestRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConsentGrantRepository consentGrantRepository;
    @Mock private ConsultationContextShareRepository contextShareRepository;
    @Mock private ConsultationContextCitationRepository citationRepository;
    @Mock private TriageCitationResolver citationResolver;
    @Mock private AuditService auditService;

    private TriageExpertHandoffService service;

    @BeforeEach
    void setUp() {
        service = new TriageExpertHandoffService(
                intakeRepository,
                triageService,
                consultationRequestService,
                consultationRequestRepository,
                expertProfileRepository,
                userRepository,
                consentGrantRepository,
                contextShareRepository,
                citationRepository,
                citationResolver,
                auditService,
                new TriageExpertHandoffPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void previewReturnsOnlySanitizedServerOwnedAllowlist() {
        when(intakeRepository.findByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID)).thenReturn(result("  Safe\t summary "));
        when(citationResolver.resolveForPreview(anyList(), any())).thenReturn(List.of());

        HandoffPreviewResponse response = service.preview(INTAKE_ID, OWNER_ID);

        assertThat(response.intakeSessionId()).isEqualTo(INTAKE_ID);
        assertThat(response.riskLevel()).isEqualTo("YELLOW");
        assertThat(response.stage()).isEqualTo("POSTPARTUM");
        assertThat(response.riskSummary()).isEqualTo("Safe summary");
        assertThat(response.sharedFields()).containsExactlyElementsOf(
                new TriageExpertHandoffPolicy().sharedFields());
        assertThat(response.excludedFields()).containsExactlyElementsOf(
                new TriageExpertHandoffPolicy().excludedFields());
    }

    @Test
    void previewUsesNeutralNotFoundForForeignOrMissingIntake() {
        when(intakeRepository.findByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(INTAKE_ID, OWNER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-002"));
        verify(triageService, never()).getResult(any(), any());
    }

    @Test
    void previewAcceptsOwnedCompletedYellowBabyProfileIntakeWithoutJourney() {
        when(intakeRepository.findByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleBabyProfileIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID))
                .thenReturn(babyProfileResult("Baby safe summary"));
        when(citationResolver.resolveForPreview(anyList(), any())).thenReturn(List.of());

        HandoffPreviewResponse response = service.preview(INTAKE_ID, OWNER_ID);

        assertThat(response.intakeSessionId()).isEqualTo(INTAKE_ID);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.YELLOW.name());
        assertThat(response.stage()).isEqualTo(TriageStage.INFANT.name());
        assertThat(response.riskSummary()).isEqualTo("Baby safe summary");
    }

    @Test
    void createPersistsExactConsentRequestAndImmutableSnapshot() {
        TriageExpertHandoffCreateRequest request = createRequest();
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty());
        when(intakeRepository.findForUpdateByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID)).thenReturn(result("Safe summary"));
        when(consultationRequestService.create(any(), any())).thenReturn(new CreateConsultationRequestResult(
                consultationResponse(), true));
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());
        stubEligibleExpertBoundary();
        when(citationResolver.resolveForCreate(anyList(), any())).thenReturn(List.of());
        when(consentGrantRepository.save(any())).thenAnswer(invocation -> {
            ConsentGrant consent = invocation.getArgument(0);
            consent.setId(91L);
            return consent;
        });
        when(contextShareRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HandoffCreateResponse response = service.create(INTAKE_ID, request, OWNER_ID);

        assertThat(response.consultationRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.replayed()).isFalse();
        assertThat(response.sharedAt()).isEqualTo(NOW);
        assertThat(response.context().riskSummary()).isEqualTo("Safe summary");

        ArgumentCaptor<CreateConsultationRequestRequest> genericRequest =
                ArgumentCaptor.forClass(CreateConsultationRequestRequest.class);
        verify(consultationRequestService).create(genericRequest.capture(), org.mockito.ArgumentMatchers.eq(OWNER_ID));
        assertThat(genericRequest.getValue().getClientRequestId()).isEqualTo(KEY);
        assertThat(genericRequest.getValue().getExpertProfileId()).isEqualTo(EXPERT_PROFILE_ID);
        assertThat(genericRequest.getValue().getTopic()).isEqualTo(TriageExpertHandoffPolicy.TOPIC);
        assertThat(genericRequest.getValue().getDescription()).isEqualTo(TriageExpertHandoffPolicy.DESCRIPTION);

        ArgumentCaptor<ConsentGrant> consent = ArgumentCaptor.forClass(ConsentGrant.class);
        verify(consentGrantRepository).save(consent.capture());
        assertThat(consent.getValue().getUserId()).isEqualTo(OWNER_ID);
        assertThat(consent.getValue().getDataType()).isEqualTo(ConsentDataType.EXPERT_SHARED_DATA);
        assertThat(consent.getValue().getPurpose()).isEqualTo(ConsentPurpose.SHARE);
        assertThat(consent.getValue().getRecipient()).isEqualTo(EXPERT_PROFILE_ID.toString());
        assertThat(consent.getValue().getScope()).isEqualTo(TriageExpertHandoffPolicy.CONSENT_SCOPE);
        assertThat(consent.getValue().getPolicyVersion()).isEqualTo(TriageExpertHandoffPolicy.POLICY_VERSION);
        assertThat(consent.getValue().getEvidenceKey()).isEqualTo(KEY);
        assertThat(consent.getValue().getExpiryAt()).isEqualTo(EXPIRES);

        ArgumentCaptor<Object> auditDetails = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.MODERATION_ACTION),
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.eq("TRIAGE_EXPERT_HANDOFF"),
                any(),
                auditDetails.capture());
        assertThat(auditDetails.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auditDetails.getValue();
        assertThat(details).containsOnlyKeys(
                "eventType",
                "policyVersion",
                "consultationRequestId",
                "intakeSessionId",
                "expertProfileId");
        assertThat(details.toString()).doesNotContain(
                "Safe summary", "riskSummary", "citations", "symptoms", "continuationToken");
    }

    @Test
    void createPreservesNullJourneyAndExactBabyOriginInImmutableSnapshot() {
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty());
        when(intakeRepository.findForUpdateByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleBabyProfileIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID))
                .thenReturn(babyProfileResult("Baby safe summary"));
        when(consultationRequestService.create(any(), any())).thenReturn(
                new CreateConsultationRequestResult(consultationResponse(), true));
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());
        stubEligibleExpertBoundary();
        when(citationResolver.resolveForCreate(anyList(), any())).thenReturn(List.of());
        when(consentGrantRepository.save(any())).thenAnswer(invocation -> {
            ConsentGrant consent = invocation.getArgument(0);
            consent.setId(91L);
            return consent;
        });
        when(contextShareRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HandoffCreateResponse response = service.create(INTAKE_ID, createRequest(), OWNER_ID);

        assertThat(response.consultationRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.replayed()).isFalse();
        assertThat(response.context().riskLevel()).isEqualTo(RiskLevel.YELLOW.name());
        assertThat(response.context().stage()).isEqualTo(TriageStage.INFANT.name());

        ArgumentCaptor<ConsultationContextShare> share =
                ArgumentCaptor.forClass(ConsultationContextShare.class);
        verify(contextShareRepository).save(share.capture());
        assertThat(share.getValue().getIntakeSessionId()).isEqualTo(INTAKE_ID);
        assertThat(share.getValue().getJourneyId()).isNull();
        assertThat(share.getValue().getOriginDashboard())
                .isEqualTo(OriginDashboard.BABY_PROFILE.name());
        assertThat(share.getValue().getOriginReferenceId()).isEqualTo(BABY_PROFILE_ID);
        assertThat(share.getValue().getTriageStage()).isEqualTo(TriageStage.INFANT.name());
        assertThat(share.getValue().getRiskLevel()).isEqualTo(RiskLevel.YELLOW.name());
        assertThat(share.getValue().getIntakeStatus()).isEqualTo(IntakeStatus.COMPLETED.name());
    }

    @Test
    void sameIntentFastReplayReturnsOriginalAggregateWithoutWrites() {
        ConsultationContextShare existing = contextShare();
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.of(existing));
        when(citationRepository.findByContextShareIdOrderByOrdinalAsc(SHARE_ID))
                .thenReturn(List.of());
        when(consultationRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(consultationRequest()));

        HandoffCreateResponse response = service.create(INTAKE_ID, createRequest(), OWNER_ID);

        assertThat(response.replayed()).isTrue();
        assertThat(response.sharedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(intakeRepository, never()).findForUpdateByIdAndUserId(any(), any());
        verify(consultationRequestService, never()).create(any(), any());
        verify(consentGrantRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    @Test
    void changedIntentOnSameKeyFailsBeforeAnyLockOrWrite() {
        ConsultationContextShare existing = ConsultationContextShare.builder()
                .id(SHARE_ID)
                .consultationRequestId(REQUEST_ID)
                .ownerUserId(OWNER_ID)
                .intakeSessionId(INTAKE_ID)
                .expertProfileId(UUID.randomUUID())
                .idempotencyKey(KEY)
                .sharePolicyVersion(TriageExpertHandoffPolicy.POLICY_VERSION)
                .build();
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(INTAKE_ID, createRequest(), OWNER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-009"));
        verify(intakeRepository, never()).findForUpdateByIdAndUserId(any(), any());
        verify(consultationRequestService, never()).create(any(), any());
    }

    @Test
    void genericRequestCollisionWithoutContextFailsClosed() {
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty());
        when(intakeRepository.findForUpdateByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID)).thenReturn(result("Safe summary"));
        when(consultationRequestService.create(any(), any())).thenReturn(new CreateConsultationRequestResult(
                consultationResponse(), false));
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(INTAKE_ID, createRequest(), OWNER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-009"));
        verify(consentGrantRepository, never()).save(any());
        verify(contextShareRepository, never()).save(any());
    }

    @Test
    void persistenceFailureIsFeatureScopedAndLeavesLaterWritesUnattempted() {
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty());
        when(intakeRepository.findForUpdateByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID)).thenReturn(result("Safe summary"));
        when(consultationRequestService.create(any(), any())).thenReturn(new CreateConsultationRequestResult(
                consultationResponse(), true));
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());
        stubEligibleExpertBoundary();
        when(citationResolver.resolveForCreate(anyList(), any())).thenReturn(List.of());
        when(consentGrantRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("synthetic persistence failure"));

        assertThatThrownBy(() -> service.create(INTAKE_ID, createRequest(), OWNER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-010"));
        verify(contextShareRepository, never()).save(any());
        verify(citationRepository, never()).saveAll(any());
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    @Test
    void featureRevalidatesDisabledExpertAccountAfterGenericCreateAndRollsBackBeforeConsent() {
        when(contextShareRepository.findByOwnerUserIdAndIdempotencyKey(OWNER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty());
        when(intakeRepository.findForUpdateByIdAndUserId(INTAKE_ID, OWNER_ID))
                .thenReturn(Optional.of(eligibleIntake()));
        when(triageService.getResult(INTAKE_ID, OWNER_ID)).thenReturn(result("Safe summary"));
        when(consultationRequestService.create(any(), any())).thenReturn(new CreateConsultationRequestResult(
                consultationResponse(), true));
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(eligibleExpert()));
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID)).thenReturn(Optional.of(User.builder()
                .id(EXPERT_USER_ID)
                .enabled(false)
                .locked(false)
                .build()));

        assertThatThrownBy(() -> service.create(INTAKE_ID, createRequest(), OWNER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-004"));
        verify(consentGrantRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    @Test
    void ownerCanReadImmutableContextAfterConsentExpiry() {
        stubContextRead();

        HandoffParticipantResponse response = service.read(REQUEST_ID, OWNER_ID);

        assertThat(response.consultationRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.context().riskLevel()).isEqualTo("YELLOW");
        verify(consentGrantRepository, never()).findById(any());
    }

    @Test
    void outsiderIsNeutralNotFoundAndExpertWithRevokedConsentIsForbidden() {
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.of(contextShare()));
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(eligibleExpert()));

        assertThatThrownBy(() -> service.read(REQUEST_ID, UUID.randomUUID()))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-006"));

        ConsentGrant revoked = validConsent();
        revoked.setRevokedAt(NOW.minusSeconds(1));
        when(consentGrantRepository.findById(91L)).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> service.read(REQUEST_ID, EXPERT_USER_ID))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-007"));
    }

    @Test
    void assignedExpertReadsOnlyWithExactActiveConsentAndEligibleAccount() {
        stubContextRead();
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(eligibleExpert()));
        when(consentGrantRepository.findById(91L)).thenReturn(Optional.of(validConsent()));
        when(userRepository.findById(EXPERT_USER_ID)).thenReturn(Optional.of(User.builder()
                .id(EXPERT_USER_ID)
                .enabled(true)
                .locked(false)
                .build()));

        HandoffParticipantResponse response = service.read(REQUEST_ID, EXPERT_USER_ID);

        assertThat(response.consultationRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.context().riskSummary()).isEqualTo("Original safe summary");
    }

    private void stubContextRead() {
        when(contextShareRepository.findByConsultationRequestId(REQUEST_ID))
                .thenReturn(Optional.of(contextShare()));
        when(citationRepository.findByContextShareIdOrderByOrdinalAsc(SHARE_ID))
                .thenReturn(List.of());
        when(consultationRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(consultationRequest()));
    }

    private void stubEligibleExpertBoundary() {
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(eligibleExpert()));
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID)).thenReturn(Optional.of(User.builder()
                .id(EXPERT_USER_ID)
                .enabled(true)
                .locked(false)
                .build()));
    }

    private static TriageExpertHandoffCreateRequest createRequest() {
        return new TriageExpertHandoffCreateRequest(
                KEY, EXPERT_PROFILE_ID, true, TriageExpertHandoffPolicy.POLICY_VERSION);
    }

    private static IntakeSession eligibleIntake() {
        return IntakeSession.builder()
                .id(INTAKE_ID)
                .userId(OWNER_ID)
                .journeyId(UUID.fromString("00000000-0000-0000-0000-000000000701"))
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(UUID.fromString("00000000-0000-0000-0000-000000000702"))
                .stage(TriageStage.POSTPARTUM)
                .riskLevel(RiskLevel.YELLOW)
                .status(IntakeStatus.COMPLETED)
                .build();
    }

    private static IntakeSession eligibleBabyProfileIntake() {
        return IntakeSession.builder()
                .id(INTAKE_ID)
                .userId(OWNER_ID)
                .journeyId(null)
                .originDashboard(OriginDashboard.BABY_PROFILE)
                .originReferenceId(BABY_PROFILE_ID)
                .stage(TriageStage.INFANT)
                .riskLevel(RiskLevel.YELLOW)
                .status(IntakeStatus.COMPLETED)
                .build();
    }

    private static TriageResultResponse result(String summary) {
        return TriageResultResponse.builder()
                .sessionId(INTAKE_ID)
                .stage("POSTPARTUM")
                .riskLevel("YELLOW")
                .summary(summary)
                .citations(List.of())
                .build();
    }

    private static TriageResultResponse babyProfileResult(String summary) {
        return TriageResultResponse.builder()
                .sessionId(INTAKE_ID)
                .stage(TriageStage.INFANT.name())
                .riskLevel(RiskLevel.YELLOW.name())
                .summary(summary)
                .citations(List.of())
                .build();
    }

    private static ConsultationRequestResponse consultationResponse() {
        return ConsultationRequestResponse.builder()
                .id(REQUEST_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .status("PENDING")
                .expiresAt(EXPIRES)
                .createdAt(NOW)
                .build();
    }

    private static ConsultationRequest consultationRequest() {
        return ConsultationRequest.builder()
                .id(REQUEST_ID)
                .requesterUserId(OWNER_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .clientRequestId(KEY)
                .status(ConsultationRequestStatus.PENDING)
                .build();
    }

    private static ConsultationContextShare contextShare() {
        IntakeSession intake = eligibleIntake();
        return ConsultationContextShare.builder()
                .id(SHARE_ID)
                .consultationRequestId(REQUEST_ID)
                .ownerUserId(OWNER_ID)
                .intakeSessionId(INTAKE_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .consentGrantId(91L)
                .idempotencyKey(KEY)
                .journeyId(intake.getJourneyId())
                .originDashboard(intake.getOriginDashboard().name())
                .originReferenceId(intake.getOriginReferenceId())
                .triageStage("POSTPARTUM")
                .riskLevel("YELLOW")
                .intakeStatus("COMPLETED")
                .riskSummary("Original safe summary")
                .sharePolicyVersion(TriageExpertHandoffPolicy.POLICY_VERSION)
                .createdAt(NOW.minusSeconds(60))
                .build();
    }

    private static ExpertProfile eligibleExpert() {
        return ExpertProfile.builder()
                .expertProfileId(EXPERT_PROFILE_ID)
                .userId(EXPERT_USER_ID)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    private static ConsentGrant validConsent() {
        return ConsentGrant.builder()
                .id(91L)
                .userId(OWNER_ID)
                .dataType(ConsentDataType.EXPERT_SHARED_DATA)
                .purpose(ConsentPurpose.SHARE)
                .recipient(EXPERT_PROFILE_ID.toString())
                .scope(TriageExpertHandoffPolicy.CONSENT_SCOPE)
                .policyVersion(TriageExpertHandoffPolicy.POLICY_VERSION)
                .evidenceKey(KEY)
                .consentGivenAt(NOW.minusSeconds(60))
                .expiryAt(EXPIRES)
                .build();
    }
}
