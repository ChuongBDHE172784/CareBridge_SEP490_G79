package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.constants.ConsentConstants;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.journey.dto.JourneyOnboardingStatusResponse;
import com.carebridge.backend.journey.dto.SubmitJourneyOnboardingRequest;
import com.carebridge.backend.journey.entity.MotherBaselineContext;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JourneyOnboardingServiceImpl implements IJourneyOnboardingService {

    public static final String SCHEMA_VERSION = "MOTHER_BASELINE_V1";
    public static final String BASELINE_SOURCE = "SELF_REPORTED";
    public static final String POLICY_VERSION = "MOTHER_LIFECYCLE_V1";
    public static final String LIFECYCLE_SCOPE =
            "STORE_BASELINE_AND_PERSONALIZE_MOTHER_LIFECYCLE";

    private final MotherBaselineContextRepository baselineRepository;
    private final ConsentGrantRepository consentRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public JourneyOnboardingServiceImpl(
            MotherBaselineContextRepository baselineRepository,
            ConsentGrantRepository consentRepository,
            AuditService auditService) {
        this(baselineRepository, consentRepository, auditService, Clock.systemUTC());
    }

    public JourneyOnboardingServiceImpl(
            MotherBaselineContextRepository baselineRepository,
            ConsentGrantRepository consentRepository,
            AuditService auditService,
            Clock clock) {
        this.baselineRepository = baselineRepository;
        this.consentRepository = consentRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public JourneyOnboardingStatusResponse submit(
            UUID userId, SubmitJourneyOnboardingRequest request) {
        if (!request.isConsentAccepted()) {
            throw error(HttpStatus.CONFLICT, "LIFECYCLE_CONSENT_REQUIRED",
                    "Lifecycle consent is required before continuing");
        }
        if (!POLICY_VERSION.equals(request.getPolicyVersion())) {
            throw error(HttpStatus.CONFLICT, "LIFECYCLE_CONSENT_INVALID",
                    "Lifecycle consent needs to be reviewed");
        }
        if (new HashSet<>(request.getPreferences()).size() != request.getPreferences().size()) {
            throw error(HttpStatus.BAD_REQUEST, "DUPLICATE_SUPPORT_PREFERENCE",
                    "Support preferences must be unique");
        }

        // Serialize onboarding revisions per owner. This makes both same-key
        // ambiguous retries and distinct concurrent submissions deterministic.
        baselineRepository.acquireOwnerLock(userId);
        var existingBaseline = baselineRepository.findByOwnerUserIdAndSubmissionId(
                userId, request.getSubmissionId());
        var existingConsent = consentRepository.findLifecycleEvidenceByKey(
                userId, request.getSubmissionId());
        if (existingBaseline.isPresent() && existingConsent.isPresent()) {
            if (!matches(existingBaseline.get(), existingConsent.get(), request)) {
                throw error(HttpStatus.CONFLICT, "ONBOARDING_SUBMISSION_CONFLICT",
                        "Submission id was already used with different onboarding data");
            }
            return response(existingBaseline.get(), existingConsent.get(), Instant.now(clock));
        }
        if (existingBaseline.isPresent() || existingConsent.isPresent()) {
            throw error(HttpStatus.CONFLICT, "ONBOARDING_SUBMISSION_CONFLICT",
                    "Onboarding submission state is inconsistent; retry with a new submission");
        }

        Instant now = Instant.now(clock);
        long revision = baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(userId)
                .map(value -> value.getRevision() + 1)
                .orElse(1L);
        String preferences = request.getPreferences().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        MotherBaselineContext baseline = baselineRepository.save(
                MotherBaselineContext.builder()
                        .id(UUID.randomUUID())
                        .ownerUserId(userId)
                        .submissionId(request.getSubmissionId())
                        .revision(revision)
                        .schemaVersion(SCHEMA_VERSION)
                        .source(BASELINE_SOURCE)
                        .lifecycleGoal(request.getLifecycleGoal())
                        .locale(request.getLocale())
                        .timeZone(request.getTimeZone())
                        .preferences(preferences)
                        .recordedAt(now)
                        .build());
        ConsentGrant evidence = consentRepository.save(
                ConsentGrant.builder()
                        .userId(userId)
                        .dataType(ConsentDataType.MOTHER_BASELINE)
                        .purpose(ConsentPurpose.PERSONALIZE)
                        .scope(LIFECYCLE_SCOPE)
                        .policyVersion(POLICY_VERSION)
                        .evidenceKey(request.getSubmissionId())
                        .locale(request.getLocale())
                        .consentGivenAt(now)
                        .expiryAt(now.plus(ConsentConstants.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS))
                        .build());
        auditService.log(AuditAction.MOTHER_BASELINE_SUBMITTED, userId,
                "MotherBaselineContext", baseline.getId().toString(),
                Map.of("schemaVersion", SCHEMA_VERSION, "revision", revision));
        auditService.log(AuditAction.CONSENT_GRANTED, userId,
                "ConsentGrant", evidence.getId().toString(),
                Map.of("policyVersion", POLICY_VERSION, "scope", LIFECYCLE_SCOPE));
        return response(baseline, evidence, now);
    }

    @Override
    @Transactional(readOnly = true)
    public JourneyOnboardingStatusResponse getStatus(UUID userId) {
        var baseline = baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(userId);
        var consent = baseline.flatMap(value -> consentRepository.findLifecycleEvidenceByKey(
                userId, value.getSubmissionId()));
        boolean consentValid = consent.filter(value -> isValidEvidence(value, Instant.now(clock)))
                .isPresent();
        return JourneyOnboardingStatusResponse.builder()
                .baselineComplete(baseline.isPresent())
                .consentValid(consentValid)
                .baselineRevision(baseline.map(MotherBaselineContext::getRevision).orElse(0L))
                .baselineId(baseline.map(MotherBaselineContext::getId).orElse(null))
                .consentEvidenceId(consent.map(ConsentGrant::getId).orElse(null))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureEligible(UUID userId) {
        JourneyOnboardingStatusResponse status;
        try {
            // Share the owner-scoped transaction lock with onboarding writes and
            // revocation so consent cannot change between this check and journey creation.
            consentRepository.acquireLifecycleOwnerLock(userId);
            status = getStatus(userId);
        } catch (DataAccessException exception) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE,
                    "ONBOARDING_ELIGIBILITY_UNAVAILABLE",
                    "Onboarding eligibility cannot be verified right now");
        }
        if (!status.isBaselineComplete()) {
            throw error(HttpStatus.CONFLICT, "BASELINE_REQUIRED",
                    "Complete your baseline before starting a journey");
        }
        if (!status.isConsentValid()) {
            if (!consentRepository.existsLifecycleEvidence(userId)) {
                throw error(HttpStatus.CONFLICT, "LIFECYCLE_CONSENT_REQUIRED",
                        "Lifecycle consent is required before continuing");
            }
            throw error(HttpStatus.CONFLICT, "LIFECYCLE_CONSENT_INVALID",
                    "Your lifecycle consent needs to be reviewed");
        }
    }

    private JourneyOnboardingStatusResponse response(
            MotherBaselineContext baseline, ConsentGrant evidence, Instant now) {
        boolean valid = evidence.getRevokedAt() == null
                && evidence.getExpiryAt() != null
                && evidence.getExpiryAt().isAfter(now);
        return JourneyOnboardingStatusResponse.builder()
                .baselineComplete(true)
                .consentValid(valid)
                .baselineRevision(baseline.getRevision())
                .baselineId(baseline.getId())
                .consentEvidenceId(evidence.getId())
                .build();
    }

    private boolean matches(MotherBaselineContext baseline, ConsentGrant evidence,
            SubmitJourneyOnboardingRequest request) {
        String preferences = request.getPreferences().stream().map(Enum::name).sorted()
                .collect(Collectors.joining(","));
        return baseline.getLifecycleGoal() == request.getLifecycleGoal()
                && baseline.getLocale().equals(request.getLocale())
                && baseline.getTimeZone().equals(request.getTimeZone())
                && baseline.getPreferences().equals(preferences)
                && POLICY_VERSION.equals(evidence.getPolicyVersion())
                && LIFECYCLE_SCOPE.equals(evidence.getScope())
                && request.getLocale().equals(evidence.getLocale());
    }

    private boolean isValidEvidence(ConsentGrant evidence, Instant now) {
        return evidence.getDataType() == ConsentDataType.MOTHER_BASELINE
                && evidence.getPurpose() == ConsentPurpose.PERSONALIZE
                && POLICY_VERSION.equals(evidence.getPolicyVersion())
                && LIFECYCLE_SCOPE.equals(evidence.getScope())
                && evidence.getRevokedAt() == null
                && evidence.getExpiryAt() != null
                && evidence.getExpiryAt().isAfter(now);
    }

    private BusinessException error(HttpStatus status, String code, String message) {
        return new BusinessException(status, code, message);
    }
}
