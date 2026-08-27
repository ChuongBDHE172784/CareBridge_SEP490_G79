package com.carebridge.backend.journey.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Single source of truth for Story 6.2 lifecycle-consent evidence checks. */
@Component
public class LifecycleConsentValidator {
    public static final String POLICY_VERSION = "MOTHER_LIFECYCLE_V1";
    public static final String LIFECYCLE_SCOPE =
            "STORE_BASELINE_AND_PERSONALIZE_MOTHER_LIFECYCLE";

    private final MotherBaselineContextRepository baselineRepository;
    private final ConsentGrantRepository consentRepository;
    private final Clock clock;

    @Autowired
    public LifecycleConsentValidator(
            MotherBaselineContextRepository baselineRepository,
            ConsentGrantRepository consentRepository) {
        this(baselineRepository, consentRepository, Clock.systemUTC());
    }

    public LifecycleConsentValidator(
            MotherBaselineContextRepository baselineRepository,
            ConsentGrantRepository consentRepository,
            Clock clock) {
        this.baselineRepository = baselineRepository;
        this.consentRepository = consentRepository;
        this.clock = clock;
    }

    public void ensureEligibleForRead(UUID ownerUserId) {
        ensureEligible(ownerUserId, false);
    }

    public void ensureEligibleForMutation(UUID ownerUserId) {
        ensureEligible(ownerUserId, true);
    }

    private void ensureEligible(UUID ownerUserId, boolean serialize) {
        if (serialize) {
            consentRepository.acquireLifecycleOwnerLock(ownerUserId);
        }
        var baseline = baselineRepository
                .findTopByOwnerUserIdOrderByRevisionDesc(ownerUserId)
                .orElseThrow(() -> error("BASELINE_REQUIRED",
                        "Complete your baseline before using maternal lifecycle features"));
        var evidence = consentRepository
                .findLifecycleEvidenceByKey(ownerUserId, baseline.getSubmissionId())
                .orElseThrow(() -> error("LIFECYCLE_CONSENT_REQUIRED",
                        "Lifecycle consent is required"));
        Instant now = Instant.now(clock);
        boolean valid = evidence.getDataType() == ConsentDataType.MOTHER_BASELINE
                && evidence.getPurpose() == ConsentPurpose.PERSONALIZE
                && POLICY_VERSION.equals(evidence.getPolicyVersion())
                && LIFECYCLE_SCOPE.equals(evidence.getScope())
                && evidence.getRevokedAt() == null
                && evidence.getExpiryAt() != null
                && evidence.getExpiryAt().isAfter(now);
        if (!valid) {
            throw error("LIFECYCLE_CONSENT_INVALID",
                    "Lifecycle consent needs to be reviewed");
        }
    }

    private BusinessException error(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
