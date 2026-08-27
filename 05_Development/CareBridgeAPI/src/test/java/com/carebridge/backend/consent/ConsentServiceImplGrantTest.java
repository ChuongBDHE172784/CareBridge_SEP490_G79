package com.carebridge.backend.consent;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
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
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsentServiceImplGrantTest {

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
    @DisplayName("CONSENT-TC-001: grantConsent saves grant with correct fields")
    void grantConsent_validRequest_savesAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        GrantConsentRequest request = new GrantConsentRequest();
        request.setDataType(ConsentDataType.HEALTH_RECORD);
        request.setPurpose(ConsentPurpose.VIEW);
        request.setRecipient("Dr. Smith");
        request.setScope("blood_pressure,heart_rate");
        request.setExpiryDays(30);

        ConsentGrant savedGrant = ConsentGrant.builder()
                .id(1L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .recipient("Dr. Smith")
                .scope("blood_pressure,heart_rate")
                .consentGivenAt(Instant.now())
                .expiryAt(Instant.now().plusSeconds(30 * 86400L))
                .build();

        ConsentGrantResponse expectedResponse = ConsentGrantResponse.builder()
                .id(1L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .recipient("Dr. Smith")
                .scope("blood_pressure,heart_rate")
                .consentGivenAt(savedGrant.getConsentGivenAt())
                .expiryAt(savedGrant.getExpiryAt())
                .build();

        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(savedGrant);
        when(consentGrantMapper.toResponse(savedGrant)).thenReturn(expectedResponse);

        ConsentGrantResponse response = consentService.grantConsent(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDataType()).isEqualTo(ConsentDataType.HEALTH_RECORD);
        verify(consentGrantRepository).save(any(ConsentGrant.class));
    }

    @Test
    @DisplayName("CONSENT-TC-002: grantConsent uses default expiry days when not provided")
    void grantConsent_noExpiryDays_usesDefault() {
        UUID userId = UUID.randomUUID();
        GrantConsentRequest request = new GrantConsentRequest();
        request.setDataType(ConsentDataType.HEALTH_RECORD);
        request.setPurpose(ConsentPurpose.VIEW);
        request.setExpiryDays(null); // no expiry specified

        ConsentGrant savedGrant = ConsentGrant.builder()
                .id(2L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now())
                .expiryAt(Instant.now().plusSeconds(90 * 86400L))
                .build();

        ConsentGrantResponse expectedResponse = ConsentGrantResponse.builder()
                .id(2L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(savedGrant.getConsentGivenAt())
                .expiryAt(savedGrant.getExpiryAt())
                .build();

        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(savedGrant);
        when(consentGrantMapper.toResponse(savedGrant)).thenReturn(expectedResponse);

        ConsentGrantResponse response = consentService.grantConsent(userId, request);

        assertThat(response).isNotNull();

        // Verify the saved grant has expiryAt set
        ArgumentCaptor<ConsentGrant> captor = ArgumentCaptor.forClass(ConsentGrant.class);
        verify(consentGrantRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiryAt()).isNotNull();
        assertThat(captor.getValue().getExpiryAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("CONSENT-TC-003: grantConsent logs CONSENT_GRANTED audit")
    void grantConsent_validRequest_logsAudit() {
        UUID userId = UUID.randomUUID();
        GrantConsentRequest request = new GrantConsentRequest();
        request.setDataType(ConsentDataType.LOCATION);
        request.setPurpose(ConsentPurpose.SHARE);
        request.setExpiryDays(7);

        ConsentGrant savedGrant = ConsentGrant.builder()
                .id(3L)
                .userId(userId)
                .dataType(ConsentDataType.LOCATION)
                .purpose(ConsentPurpose.SHARE)
                .consentGivenAt(Instant.now())
                .expiryAt(Instant.now().plusSeconds(7 * 86400L))
                .build();

        ConsentGrantResponse response = ConsentGrantResponse.builder()
                .id(3L)
                .build();

        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(savedGrant);
        when(consentGrantMapper.toResponse(savedGrant)).thenReturn(response);

        consentService.grantConsent(userId, request);

        verify(auditService).log(
                eq(AuditAction.CONSENT_GRANTED),
                eq(userId),
                eq("ConsentGrant"),
                eq("3"),
                any(Map.class));
    }

    @Test
    @DisplayName("CONSENT-TC-004: grantConsent sets consentGivenAt to now")
    void grantConsent_setsConsentGivenAtToNow() {
        UUID userId = UUID.randomUUID();
        GrantConsentRequest request = new GrantConsentRequest();
        request.setDataType(ConsentDataType.HEALTH_RECORD);
        request.setPurpose(ConsentPurpose.VIEW);
        request.setExpiryDays(14);

        Instant beforeCall = Instant.now();

        ConsentGrant savedGrant = ConsentGrant.builder()
                .id(4L)
                .userId(userId)
                .dataType(ConsentDataType.HEALTH_RECORD)
                .purpose(ConsentPurpose.VIEW)
                .consentGivenAt(Instant.now())
                .expiryAt(Instant.now().plusSeconds(14 * 86400L))
                .build();

        when(consentGrantRepository.save(any(ConsentGrant.class))).thenReturn(savedGrant);
        when(consentGrantMapper.toResponse(savedGrant)).thenReturn(ConsentGrantResponse.builder().build());

        consentService.grantConsent(userId, request);

        ArgumentCaptor<ConsentGrant> captor = ArgumentCaptor.forClass(ConsentGrant.class);
        verify(consentGrantRepository).save(captor.capture());
        assertThat(captor.getValue().getConsentGivenAt()).isNotNull();
        assertThat(captor.getValue().getConsentGivenAt()).isAfterOrEqualTo(beforeCall);
    }
}
