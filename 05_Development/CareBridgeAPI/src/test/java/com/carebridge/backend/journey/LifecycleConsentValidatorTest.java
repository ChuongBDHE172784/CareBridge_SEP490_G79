package com.carebridge.backend.journey;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.journey.entity.MotherBaselineContext;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LifecycleConsentValidatorTest {
    private static final UUID OWNER = UUID.fromString("10000000-0000-4000-8000-000000000064");
    private static final UUID KEY = UUID.fromString("20000000-0000-4000-8000-000000000064");
    private static final Instant NOW = Instant.parse("2026-07-19T05:00:00Z");

    @Mock MotherBaselineContextRepository baselineRepository;
    @Mock ConsentGrantRepository consentRepository;
    private LifecycleConsentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LifecycleConsentValidator(
                baselineRepository, consentRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void exactEvidencePredicate_allowsReadWithoutTakingMutationLock() {
        stub(ConsentGrant.builder()
                .userId(OWNER)
                .evidenceKey(KEY)
                .dataType(ConsentDataType.MOTHER_BASELINE)
                .purpose(ConsentPurpose.PERSONALIZE)
                .policyVersion(LifecycleConsentValidator.POLICY_VERSION)
                .scope(LifecycleConsentValidator.LIFECYCLE_SCOPE)
                .expiryAt(NOW.plusSeconds(60))
                .build());

        validator.ensureEligibleForRead(OWNER);

        verify(baselineRepository).findTopByOwnerUserIdOrderByRevisionDesc(OWNER);
    }

    @Test
    void revokedEvidence_isRejectedAndMutationIsSerialized() {
        stub(ConsentGrant.builder()
                .userId(OWNER)
                .evidenceKey(KEY)
                .dataType(ConsentDataType.MOTHER_BASELINE)
                .purpose(ConsentPurpose.PERSONALIZE)
                .policyVersion(LifecycleConsentValidator.POLICY_VERSION)
                .scope(LifecycleConsentValidator.LIFECYCLE_SCOPE)
                .expiryAt(NOW.plusSeconds(60))
                .revokedAt(NOW.minusSeconds(1))
                .build());

        assertThatThrownBy(() -> validator.ensureEligibleForMutation(OWNER))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(
                        ((BusinessException) error).getCode())
                        .isEqualTo("LIFECYCLE_CONSENT_INVALID"));
        verify(consentRepository).acquireLifecycleOwnerLock(OWNER);
    }

    private void stub(ConsentGrant evidence) {
        var baseline = MotherBaselineContext.builder()
                .ownerUserId(OWNER)
                .submissionId(KEY)
                .build();
        when(baselineRepository.findTopByOwnerUserIdOrderByRevisionDesc(OWNER))
                .thenReturn(Optional.of(baseline));
        when(consentRepository.findLifecycleEvidenceByKey(OWNER, KEY))
                .thenReturn(Optional.of(evidence));
    }
}
