package com.carebridge.backend.consultation.context.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.context.dto.HandoffContextResponse;
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
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.service.CreateConsultationRequestResult;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ITriageService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TriageExpertHandoffService implements ITriageExpertHandoffService {

    private final IIntakeSessionRepository intakeRepository;
    private final ITriageService triageService;
    private final IConsultationRequestService consultationRequestService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final ConsentGrantRepository consentGrantRepository;
    private final ConsultationContextShareRepository contextShareRepository;
    private final ConsultationContextCitationRepository citationRepository;
    private final TriageCitationResolver citationResolver;
    private final AuditService auditService;
    private final TriageExpertHandoffPolicy policy;
    private final Clock clock;

    @Autowired
    public TriageExpertHandoffService(
            IIntakeSessionRepository intakeRepository,
            ITriageService triageService,
            IConsultationRequestService consultationRequestService,
            ConsultationRequestRepository consultationRequestRepository,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            ConsentGrantRepository consentGrantRepository,
            ConsultationContextShareRepository contextShareRepository,
            ConsultationContextCitationRepository citationRepository,
            TriageCitationResolver citationResolver,
            AuditService auditService,
            TriageExpertHandoffPolicy policy) {
        this(
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
                policy,
                Clock.systemUTC());
    }

    public TriageExpertHandoffService(
            IIntakeSessionRepository intakeRepository,
            ITriageService triageService,
            IConsultationRequestService consultationRequestService,
            ConsultationRequestRepository consultationRequestRepository,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            ConsentGrantRepository consentGrantRepository,
            ConsultationContextShareRepository contextShareRepository,
            ConsultationContextCitationRepository citationRepository,
            TriageCitationResolver citationResolver,
            AuditService auditService,
            TriageExpertHandoffPolicy policy,
            Clock clock) {
        this.intakeRepository = intakeRepository;
        this.triageService = triageService;
        this.consultationRequestService = consultationRequestService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.expertProfileRepository = expertProfileRepository;
        this.userRepository = userRepository;
        this.consentGrantRepository = consentGrantRepository;
        this.contextShareRepository = contextShareRepository;
        this.citationRepository = citationRepository;
        this.citationResolver = citationResolver;
        this.auditService = auditService;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public HandoffPreviewResponse preview(UUID intakeSessionId, UUID ownerUserId) {
        IntakeSession intake = intakeRepository.findByIdAndUserId(intakeSessionId, ownerUserId)
                .orElseThrow(TriageExpertHandoffException::sourceNotFound);
        TriageResultResponse result = verifiedResult(intake, ownerUserId);
        String summary = policy.sanitizeSummary(result.getSummary());
        List<SharedCitationResponse> citations = citationResolver.resolveForPreview(
                result.getCitations(), intake.getStage().name());
        return new HandoffPreviewResponse(
                intake.getId(),
                TriageExpertHandoffPolicy.POLICY_VERSION,
                RiskLevel.YELLOW.name(),
                intake.getStage().name(),
                summary,
                citations,
                policy.sharedFields(),
                policy.excludedFields());
    }

    @Override
    public HandoffCreateResponse create(
            UUID intakeSessionId,
            TriageExpertHandoffCreateRequest request,
            UUID ownerUserId) {
        policy.assertCreateRequest(request);

        Optional<ConsultationContextShare> fastExisting =
                contextShareRepository.findByOwnerUserIdAndIdempotencyKey(
                        ownerUserId, request.getClientRequestId());
        if (fastExisting.isPresent()) {
            return replay(fastExisting.get(), intakeSessionId, request);
        }

        IntakeSession intake = intakeRepository.findForUpdateByIdAndUserId(
                        intakeSessionId, ownerUserId)
                .orElseThrow(TriageExpertHandoffException::sourceNotFound);
        TriageResultResponse result = verifiedResult(intake, ownerUserId);
        String summary = policy.sanitizeSummary(result.getSummary());

        Optional<ConsultationContextShare> afterIntakeLock =
                contextShareRepository.findByOwnerUserIdAndIdempotencyKey(
                        ownerUserId, request.getClientRequestId());
        if (afterIntakeLock.isPresent()) {
            return replay(afterIntakeLock.get(), intakeSessionId, request);
        }

        CreateConsultationRequestResult genericResult = createGenericRequest(request, ownerUserId);
        ConsultationRequestResponse generic = genericResult.response();
        if (generic == null || generic.getId() == null) {
            throw TriageExpertHandoffException.completionFailed();
        }

        Optional<ConsultationContextShare> winnerByKey =
                contextShareRepository.findByOwnerUserIdAndIdempotencyKey(
                        ownerUserId, request.getClientRequestId());
        if (winnerByKey.isPresent()) {
            return replay(winnerByKey.get(), intakeSessionId, request);
        }
        Optional<ConsultationContextShare> winnerByRequest =
                contextShareRepository.findByConsultationRequestId(generic.getId());
        if (winnerByRequest.isPresent()) {
            return replay(winnerByRequest.get(), intakeSessionId, request);
        }
        if (!genericResult.created()) {
            throw TriageExpertHandoffException.idempotencyConflict();
        }
        revalidateLockedExpertBoundary(request.getExpertProfileId());

        List<SharedCitationResponse> safeCitations = citationResolver.resolveForCreate(
                result.getCitations(), intake.getStage().name());
        Instant now = clock.instant();
        try {
            ConsentGrant consent = consentGrantRepository.save(ConsentGrant.builder()
                .userId(ownerUserId)
                .dataType(ConsentDataType.EXPERT_SHARED_DATA)
                .purpose(ConsentPurpose.SHARE)
                .recipient(request.getExpertProfileId().toString())
                .scope(TriageExpertHandoffPolicy.CONSENT_SCOPE)
                .policyVersion(TriageExpertHandoffPolicy.POLICY_VERSION)
                .evidenceKey(request.getClientRequestId())
                .consentGivenAt(now)
                .expiryAt(generic.getExpiresAt())
                .version(1)
                .build());
            if (consent.getId() == null || generic.getExpiresAt() == null) {
                throw TriageExpertHandoffException.completionFailed();
            }

            UUID shareId = UUID.randomUUID();
            ConsultationContextShare share = contextShareRepository.save(
                ConsultationContextShare.builder()
                        .id(shareId)
                        .consultationRequestId(generic.getId())
                        .ownerUserId(ownerUserId)
                        .intakeSessionId(intake.getId())
                        .expertProfileId(request.getExpertProfileId())
                        .consentGrantId(consent.getId())
                        .idempotencyKey(request.getClientRequestId())
                        .journeyId(intake.getJourneyId())
                        .originDashboard(intake.getOriginDashboard().name())
                        .originReferenceId(intake.getOriginReferenceId())
                        .triageStage(intake.getStage().name())
                        .riskLevel(RiskLevel.YELLOW.name())
                        .intakeStatus(IntakeStatus.COMPLETED.name())
                        .riskSummary(summary)
                        .sharePolicyVersion(TriageExpertHandoffPolicy.POLICY_VERSION)
                        .createdAt(now)
                        .build());

            List<ConsultationContextCitation> citationSnapshots = new ArrayList<>();
            for (int index = 0; index < safeCitations.size(); index++) {
                SharedCitationResponse citation = safeCitations.get(index);
                citationSnapshots.add(ConsultationContextCitation.builder()
                    .id(UUID.randomUUID())
                    .contextShareId(share.getId())
                    .evidenceSourceId(citation.evidenceSourceId())
                    .organization(citation.organization())
                    .sourceUrl(citation.baseUrl())
                    .sourceStatusAtShare("APPROVED")
                    .reviewedAt(citation.reviewedAt())
                    .ordinal((short) index)
                    .createdAt(now)
                        .build());
            }
            if (!citationSnapshots.isEmpty()) {
                citationRepository.saveAll(citationSnapshots);
            }
            consentGrantRepository.flush();
            contextShareRepository.flush();
            citationRepository.flush();
            auditService.log(
                    AuditAction.MODERATION_ACTION,
                    ownerUserId,
                    "TRIAGE_EXPERT_HANDOFF",
                    shareId.toString(),
                    Map.of(
                            "eventType", "TRIAGE_CONTEXT_SHARED",
                            "policyVersion", TriageExpertHandoffPolicy.POLICY_VERSION,
                            "consultationRequestId", generic.getId().toString(),
                            "intakeSessionId", intake.getId().toString(),
                            "expertProfileId", request.getExpertProfileId().toString()));

            return new HandoffCreateResponse(
                    generic.getId(),
                    requireStatus(generic.getStatus()),
                    false,
                    now,
                    new HandoffContextResponse(
                            RiskLevel.YELLOW.name(),
                            intake.getStage().name(),
                            summary,
                            safeCitations));
        } catch (DataAccessException persistenceFailure) {
            throw TriageExpertHandoffException.completionFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HandoffParticipantResponse read(UUID consultationRequestId, UUID currentUserId) {
        ConsultationContextShare share = contextShareRepository
                .findByConsultationRequestId(consultationRequestId)
                .orElseThrow(TriageExpertHandoffException::sharedContextNotFound);

        if (!currentUserId.equals(share.getOwnerUserId())) {
            ExpertProfile expert = expertProfileRepository.findById(share.getExpertProfileId())
                    .orElseThrow(TriageExpertHandoffException::sharedContextNotFound);
            if (!currentUserId.equals(expert.getUserId())) {
                throw TriageExpertHandoffException.sharedContextNotFound();
            }
            ConsentGrant consent = consentGrantRepository.findById(share.getConsentGrantId())
                    .orElseThrow(TriageExpertHandoffException::sharedContextUnavailable);
            User account = userRepository.findById(expert.getUserId())
                    .orElseThrow(TriageExpertHandoffException::sharedContextUnavailable);
            if (!validExpertRead(share, expert, account, consent, clock.instant())) {
                throw TriageExpertHandoffException.sharedContextUnavailable();
            }
        }

        ConsultationRequest request = consultationRequestRepository
                .findById(consultationRequestId)
                .orElseThrow(TriageExpertHandoffException::sharedContextNotFound);
        HandoffContextResponse context = contextResponse(share);
        return new HandoffParticipantResponse(
                consultationRequestId,
                request.getStatus().name(),
                share.getCreatedAt(),
                context);
    }

    private void revalidateLockedExpertBoundary(UUID expertProfileId) {
        ExpertProfile expert = expertProfileRepository.findByIdForUpdate(expertProfileId)
                .orElseThrow(TriageExpertHandoffException::expertNoLongerAvailable);
        User account = userRepository.findByIdForUpdate(expert.getUserId())
                .orElseThrow(TriageExpertHandoffException::expertNoLongerAvailable);
        Instant now = clock.instant();
        if (!expert.isEligibleForConsultation()
                || !account.isEnabled()
                || account.isLocked()
                || (account.getSuspendedUntil() != null
                        && account.getSuspendedUntil().isAfter(now))) {
            throw TriageExpertHandoffException.expertNoLongerAvailable();
        }
    }

    private TriageResultResponse verifiedResult(IntakeSession intake, UUID ownerUserId) {
        assertEligibleIntake(intake);
        TriageResultResponse result = triageService.getResult(intake.getId(), ownerUserId);
        if (result == null
                || !Objects.equals(result.getSessionId(), intake.getId())
                || !RiskLevel.YELLOW.name().equals(result.getRiskLevel())
                || !intake.getStage().name().equals(result.getStage())) {
            throw TriageExpertHandoffException.intakeNotEligible();
        }
        return result;
    }

    private static void assertEligibleIntake(IntakeSession intake) {
        if (intake.getStatus() != IntakeStatus.COMPLETED
                || intake.getRiskLevel() != RiskLevel.YELLOW
                || intake.getStage() == null
                || intake.getOriginDashboard() == null
                || (intake.getOriginDashboard() != OriginDashboard.MOTHER_JOURNEY
                        && intake.getOriginDashboard() != OriginDashboard.BABY_PROFILE)
                || intake.getOriginReferenceId() == null
                || (intake.getOriginDashboard() == OriginDashboard.MOTHER_JOURNEY
                    && intake.getJourneyId() == null)
                || (intake.getOriginDashboard() == OriginDashboard.BABY_PROFILE
                    && intake.getJourneyId() != null)) {
            throw TriageExpertHandoffException.intakeNotEligible();
        }
    }

    private CreateConsultationRequestResult createGenericRequest(
            TriageExpertHandoffCreateRequest request, UUID ownerUserId) {
        try {
            return consultationRequestService.create(
                    CreateConsultationRequestRequest.builder()
                            .clientRequestId(request.getClientRequestId())
                            .expertProfileId(request.getExpertProfileId())
                            .topic(TriageExpertHandoffPolicy.TOPIC)
                            .description(TriageExpertHandoffPolicy.DESCRIPTION)
                            .build(),
                    ownerUserId);
        } catch (ConsultationRequestException error) {
            if ("CONREQ-002".equals(error.getCode())
                    || "CONREQ-004".equals(error.getCode())
                    || "CONREQ-006".equals(error.getCode())) {
                throw TriageExpertHandoffException.expertNoLongerAvailable();
            }
            if ("CONREQ-009".equals(error.getCode())) {
                throw TriageExpertHandoffException.idempotencyConflict();
            }
            throw TriageExpertHandoffException.completionFailed();
        }
    }

    private HandoffCreateResponse replay(
            ConsultationContextShare share,
            UUID intakeSessionId,
            TriageExpertHandoffCreateRequest request) {
        if (!sameIntent(share, intakeSessionId, request)) {
            throw TriageExpertHandoffException.idempotencyConflict();
        }
        ConsultationRequest consultationRequest = consultationRequestRepository
                .findById(share.getConsultationRequestId())
                .orElseThrow(TriageExpertHandoffException::sharedContextNotFound);
        return new HandoffCreateResponse(
                share.getConsultationRequestId(),
                consultationRequest.getStatus().name(),
                true,
                share.getCreatedAt(),
                contextResponse(share));
    }

    private static boolean sameIntent(
            ConsultationContextShare share,
            UUID intakeSessionId,
            TriageExpertHandoffCreateRequest request) {
        return Objects.equals(share.getIntakeSessionId(), intakeSessionId)
                && Objects.equals(share.getExpertProfileId(), request.getExpertProfileId())
                && Objects.equals(
                        share.getSharePolicyVersion(), request.getConsentPolicyVersion());
    }

    private HandoffContextResponse contextResponse(ConsultationContextShare share) {
        List<SharedCitationResponse> citations = citationRepository
                .findByContextShareIdOrderByOrdinalAsc(share.getId())
                .stream()
                .map(citation -> new SharedCitationResponse(
                        citation.getEvidenceSourceId(),
                        citation.getOrganization(),
                        citation.getSourceUrl(),
                        citation.getReviewedAt()))
                .toList();
        return new HandoffContextResponse(
                share.getRiskLevel(),
                share.getTriageStage(),
                share.getRiskSummary(),
                citations);
    }

    private static boolean validExpertRead(
            ConsultationContextShare share,
            ExpertProfile expert,
            User account,
            ConsentGrant consent,
            Instant now) {
        return expert.isEligibleForConsultation()
                && account.isEnabled()
                && !account.isLocked()
                && (account.getSuspendedUntil() == null
                        || !account.getSuspendedUntil().isAfter(now))
                && consent.getRevokedAt() == null
                && consent.getExpiryAt() != null
                && consent.getExpiryAt().isAfter(now)
                && consent.getDataType() == ConsentDataType.EXPERT_SHARED_DATA
                && consent.getPurpose() == ConsentPurpose.SHARE
                && Objects.equals(consent.getUserId(), share.getOwnerUserId())
                && Objects.equals(consent.getRecipient(), share.getExpertProfileId().toString())
                && Objects.equals(consent.getScope(), TriageExpertHandoffPolicy.CONSENT_SCOPE)
                && Objects.equals(
                        consent.getPolicyVersion(), TriageExpertHandoffPolicy.POLICY_VERSION)
                && Objects.equals(consent.getEvidenceKey(), share.getIdempotencyKey());
    }

    private static String requireStatus(String status) {
        if (status == null || status.isBlank()) {
            throw TriageExpertHandoffException.completionFailed();
        }
        return status;
    }
}
