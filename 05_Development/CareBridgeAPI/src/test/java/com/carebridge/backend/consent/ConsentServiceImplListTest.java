package com.carebridge.backend.consent;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.mapper.ConsentGrantMapper;
import com.carebridge.backend.consent.policy.ConsentCheckPolicy;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.consent.service.impl.ConsentServiceImpl;
import com.carebridge.backend.expertavailability.repository.ExpertLocationShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConsentServiceImplListTest {

    private ConsentService consentService;
    private ConsentGrantRepository consentGrantRepository;
    private ConsentGrantMapper consentGrantMapper;
    private ConsentCheckPolicy consentCheckPolicy;
    private AuditService auditService;
    private ExpertLocationShareRepository expertLocationShareRepository;

    @BeforeEach
    void setUp() {
        consentGrantRepository = mock(ConsentGrantRepository.class);
        consentGrantMapper = mock(ConsentGrantMapper.class);
        consentCheckPolicy = mock(ConsentCheckPolicy.class);
        auditService = mock(AuditService.class);
        expertLocationShareRepository = mock(ExpertLocationShareRepository.class);
        consentService = new ConsentServiceImpl(
                consentGrantRepository,
                consentGrantMapper,
                consentCheckPolicy,
                auditService,
                expertLocationShareRepository);
    }

    @Test
    @DisplayName("CONSENT-TC-020: listConsents returns all consents for user")
    void listConsents_returnsAllForUser() {
        UUID userId = UUID.randomUUID();

        ConsentGrant grant1 = ConsentGrant.builder()
                .id(1L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now().minusSeconds(7200))
                .expiryAt(Instant.now().plusSeconds(86400))
                .build();

        ConsentGrant grant2 = ConsentGrant.builder()
                .id(2L)
                .userId(userId)
                .dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE)
                .consentGivenAt(Instant.now().minusSeconds(3600))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(Instant.now().minusSeconds(1800))
                .build();

        ConsentGrantResponse r1 = ConsentGrantResponse.builder()
                .id(1L).userId(userId).dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW).revokedAt(null).build();
        ConsentGrantResponse r2 = ConsentGrantResponse.builder()
                .id(2L).userId(userId).dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE).revokedAt(Instant.now().minusSeconds(1800)).build();

        when(consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId))
                .thenReturn(List.of(grant1, grant2));
        when(consentGrantMapper.toResponse(grant1)).thenReturn(r1);
        when(consentGrantMapper.toResponse(grant2)).thenReturn(r2);

        List<ConsentGrantResponse> result = consentService.listConsents(userId);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("CONSENT-TC-021: listConsents active consents have revokedAt == null")
    void listConsents_activeGrant_hasNullRevokedAt() {
        UUID userId = UUID.randomUUID();

        ConsentGrant activeGrant = ConsentGrant.builder()
                .id(1L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now().minusSeconds(3600))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(null)
                .build();

        ConsentGrantResponse response = ConsentGrantResponse.builder()
                .id(1L).userId(userId).revokedAt(null).build();

        when(consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId))
                .thenReturn(List.of(activeGrant));
        when(consentGrantMapper.toResponse(activeGrant)).thenReturn(response);

        List<ConsentGrantResponse> result = consentService.listConsents(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("CONSENT-TC-022: listConsents revoked consents have revokedAt != null")
    void listConsents_revokedGrant_hasRevokedAt() {
        UUID userId = UUID.randomUUID();

        ConsentGrant revokedGrant = ConsentGrant.builder()
                .id(2L)
                .userId(userId)
                .dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE)
                .consentGivenAt(Instant.now().minusSeconds(7200))
                .expiryAt(Instant.now().plusSeconds(86400))
                .revokedAt(Instant.now().minusSeconds(3600))
                .revokedBy(userId)
                .build();

        ConsentGrantResponse response = ConsentGrantResponse.builder()
                .id(2L).userId(userId).revokedAt(Instant.now().minusSeconds(3600)).build();

        when(consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId))
                .thenReturn(List.of(revokedGrant));
        when(consentGrantMapper.toResponse(revokedGrant)).thenReturn(response);

        List<ConsentGrantResponse> result = consentService.listConsents(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("CONSENT-TC-023: listConsents returns empty list when no consents")
    void listConsents_noConsents_returnsEmptyList() {
        UUID userId = UUID.randomUUID();

        when(consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId))
                .thenReturn(List.of());

        List<ConsentGrantResponse> result = consentService.listConsents(userId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CONSENT-TC-024: listConsents uses correct repository method")
    void listConsents_usesCorrectRepositoryMethod() {
        UUID userId = UUID.randomUUID();

        when(consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId))
                .thenReturn(List.of());

        consentService.listConsents(userId);

        verify(consentGrantRepository).findByUserIdOrderByConsentGivenAtDesc(userId);
        verifyNoMoreInteractions(consentGrantRepository);
    }
}
