package com.carebridge.backend.consent;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.mapper.ConsentGrantMapper;
import com.carebridge.backend.consent.policy.ConsentCheckPolicy;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.consent.service.impl.ConsentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsentServiceImplRevokeTest {

    private ConsentService consentService;
    private ConsentGrantRepository consentGrantRepository;
    private ConsentGrantMapper consentGrantMapper;
    private ConsentCheckPolicy consentCheckPolicy;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        consentGrantRepository = mock(ConsentGrantRepository.class);
        consentGrantMapper = mock(ConsentGrantMapper.class);
        consentCheckPolicy = mock(ConsentCheckPolicy.class);
        auditService = mock(AuditService.class);
        consentService = new ConsentServiceImpl(
                consentGrantRepository, consentGrantMapper, consentCheckPolicy, auditService);
    }

    @Test
    @DisplayName("CONSENT-TC-010: revokeConsent sets revokedAt and revokedBy")
    void revokeConsent_activeGrant_setsRevokedFields() {
        UUID userId = UUID.randomUUID();
        Long consentId = 1L;

        ConsentGrant grant = ConsentGrant.builder()
                .id(consentId)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now().minusSeconds(3600))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(null)
                .build();

        ConsentGrantResponse revokedResponse = ConsentGrantResponse.builder()
                .id(consentId)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .revokedAt(Instant.now())
                .build();

        when(consentGrantRepository.findByIdAndUserId(consentId, userId)).thenReturn(Optional.of(grant));
        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(grant);
        when(consentGrantMapper.toResponse(grant)).thenReturn(revokedResponse);

        ConsentGrantResponse response = consentService.revokeConsent(userId, consentId);

        assertThat(response).isNotNull();
        assertThat(response.getRevokedAt()).isNotNull();

        ArgumentCaptor<ConsentGrant> captor = ArgumentCaptor.forClass(ConsentGrant.class);
        verify(consentGrantRepository).save(captor.capture());
        assertThat(captor.getValue().getRevokedAt()).isNotNull();
        assertThat(captor.getValue().getRevokedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("CONSENT-TC-011: revokeConsent throws ResourceNotFoundException when not found")
    void revokeConsent_grantNotFound_throwsNotFoundException() {
        UUID userId = UUID.randomUUID();
        Long consentId = 999L;

        when(consentGrantRepository.findByIdAndUserId(consentId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consentService.revokeConsent(userId, consentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Consent grant not found");
    }

    @Test
    @DisplayName("CONSENT-TC-012: revokeConsent throws ValidationException when already revoked")
    void revokeConsent_alreadyRevoked_throwsValidationException() {
        UUID userId = UUID.randomUUID();
        Long consentId = 2L;

        ConsentGrant alreadyRevokedGrant = ConsentGrant.builder()
                .id(consentId)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now().minusSeconds(7200))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(Instant.now().minusSeconds(3600)) // already revoked!
                .revokedBy(userId)
                .build();

        when(consentGrantRepository.findByIdAndUserId(consentId, userId))
                .thenReturn(Optional.of(alreadyRevokedGrant));

        assertThatThrownBy(() -> consentService.revokeConsent(userId, consentId))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("CONSENT-TC-013: revokeConsent logs CONSENT_REVOKED audit")
    void revokeConsent_activeGrant_logsAudit() {
        UUID userId = UUID.randomUUID();
        Long consentId = 3L;

        ConsentGrant grant = ConsentGrant.builder()
                .id(consentId)
                .userId(userId)
                .dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE)
                .consentGivenAt(Instant.now().minusSeconds(3600))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(null)
                .build();

        ConsentGrantResponse revokedResponse = ConsentGrantResponse.builder()
                .id(consentId)
                .userId(userId)
                .dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE)
                .revokedAt(Instant.now())
                .build();

        when(consentGrantRepository.findByIdAndUserId(consentId, userId)).thenReturn(Optional.of(grant));
        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(grant);
        when(consentGrantMapper.toResponse(grant)).thenReturn(revokedResponse);

        consentService.revokeConsent(userId, consentId);

        verify(auditService).log(
                eq(AuditAction.CONSENT_REVOKED),
                eq(userId),
                eq("ConsentGrant"),
                eq("3"),
                any(Map.class));
    }
}
