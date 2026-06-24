package com.carebridge.backend.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.mapper.PartnerProfileMapper;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.partner.service.PartnerProfileServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PartnerProfileServiceImplTest {

    @Mock
    private PartnerOrganizationRepository partnerOrganizationRepository;

    @Spy
    private PartnerProfileMapper partnerProfileMapper = new PartnerProfileMapper();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PartnerProfileServiceImpl partnerProfileService;

    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CreatePartnerProfileRequest makeValidRequest() {
        return CreatePartnerProfileRequest.builder()
                .name("Phòng khám Test")
                .type(OrganizationType.CLINIC)
                .address("123 Đường Test")
                .city("Hà Nội")
                .phone("0901234567")
                .email("test@clinic.vn")
                .website(null)
                .description(null)
                .build();
    }

    private PartnerOrganization makeSavedEntity(UUID id) {
        return PartnerOrganization.builder()
                .id(id)
                .name("Phòng khám Test")
                .type(OrganizationType.CLINIC)
                .address("123 Đường Test")
                .city("Hà Nội")
                .phone("0901234567")
                .email("test@clinic.vn")
                .status(OrganizationStatus.PENDING_APPROVAL)
                .representativeUserId(ACTOR_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // PTR-TC-001: createProfile tạo entity với đúng fields — Happy Path (TC-COND-001)
    @Test
    void createProfile_validRequest_returnsResponseWithPendingApprovalStatus() {
        CreatePartnerProfileRequest request = makeValidRequest();
        UUID savedId = UUID.randomUUID();
        PartnerOrganization saved = makeSavedEntity(savedId);

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail("test@clinic.vn")).thenReturn(false);
        when(partnerOrganizationRepository.save(any(PartnerOrganization.class))).thenReturn(saved);

        CreatePartnerProfileResponse response = partnerProfileService.createProfile(request, ACTOR_ID);

        // Oracle: ADR-003 — status must be PENDING_APPROVAL
        assertNotNull(response.getId());
        assertEquals("Phòng khám Test", response.getName());
        assertEquals(OrganizationType.CLINIC, response.getType());
        assertEquals(OrganizationStatus.PENDING_APPROVAL, response.getStatus());
        assertNotNull(response.getCreatedAt());
        verify(partnerOrganizationRepository).save(any(PartnerOrganization.class));
    }

    // PTR-TC-002: status luôn là PENDING_APPROVAL — invariant (TC-COND-002)
    @Test
    void createProfile_entityPassedToSave_alwaysHasPendingApprovalStatus() {
        CreatePartnerProfileRequest request = makeValidRequest();
        UUID savedId = UUID.randomUUID();

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail(any())).thenReturn(false);
        when(partnerOrganizationRepository.save(any(PartnerOrganization.class))).thenReturn(makeSavedEntity(savedId));

        partnerProfileService.createProfile(request, ACTOR_ID);

        ArgumentCaptor<PartnerOrganization> captor = forClass(PartnerOrganization.class);
        verify(partnerOrganizationRepository).save(captor.capture());
        // Oracle: ADR-003 — MUST be PENDING_APPROVAL, never APPROVED
        assertEquals(OrganizationStatus.PENDING_APPROVAL, captor.getValue().getStatus());
    }

    // PTR-TC-002 sub: representativeUserId dari actorId param, tidak dari request body
    @Test
    void createProfile_representativeUserIdSetFromActorId_notFromRequest() {
        CreatePartnerProfileRequest request = makeValidRequest();
        UUID savedId = UUID.randomUUID();

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail(any())).thenReturn(false);
        when(partnerOrganizationRepository.save(any(PartnerOrganization.class))).thenReturn(makeSavedEntity(savedId));

        partnerProfileService.createProfile(request, ACTOR_ID);

        ArgumentCaptor<PartnerOrganization> captor = forClass(PartnerOrganization.class);
        verify(partnerOrganizationRepository).save(captor.capture());
        // Oracle: ADR-002 — representativeUserId MUST equal actorId (from SecurityContext)
        assertEquals(ACTOR_ID, captor.getValue().getRepresentativeUserId());
    }

    // PTR-TC-004: Duplicate profile → PTR-002 (TC-COND-004)
    @Test
    void createProfile_duplicateProfile_throwsPartnerExceptionPtr002() {
        CreatePartnerProfileRequest request = makeValidRequest();
        PartnerOrganization existing = makeSavedEntity(UUID.randomUUID());

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID))
                .thenReturn(Optional.of(existing));

        PartnerException ex = assertThrows(PartnerException.class,
                () -> partnerProfileService.createProfile(request, ACTOR_ID));

        // Oracle: ADR-001, BR-PTR-001 — 1 profile per user
        assertEquals("PTR-002", ex.getCode());
        verify(partnerOrganizationRepository, never()).save(any());
        // Oracle: ADR-004 — no audit for failed operations
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // PTR-TC-005: Email trùng → PTR-003 (TC-COND-005)
    @Test
    void createProfile_duplicateEmail_throwsPartnerExceptionPtr003() {
        CreatePartnerProfileRequest request = makeValidRequest();

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail("test@clinic.vn")).thenReturn(true);

        PartnerException ex = assertThrows(PartnerException.class,
                () -> partnerProfileService.createProfile(request, ACTOR_ID));

        // Oracle: ADR-001 — email must be unique
        assertEquals("PTR-003", ex.getCode());
        verify(partnerOrganizationRepository, never()).save(any());
    }

    // PTR-TC-008: AuditService.log() được gọi sau save thành công (TC-COND-008)
    @Test
    void createProfile_onSuccess_auditServiceLogCalledOnce() {
        CreatePartnerProfileRequest request = makeValidRequest();
        UUID savedId = UUID.randomUUID();
        PartnerOrganization saved = makeSavedEntity(savedId);

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail(any())).thenReturn(false);
        when(partnerOrganizationRepository.save(any(PartnerOrganization.class))).thenReturn(saved);

        partnerProfileService.createProfile(request, ACTOR_ID);

        // Oracle: ADR-004, BR-AUDIT-002 — audit AFTER successful save
        verify(auditService).log(
                eq(AuditAction.PARTNER_PROFILE_CREATED),
                eq(ACTOR_ID),
                eq("PartnerOrganization"),
                eq(savedId.toString()),
                eq("created"));
    }

    // PTR-TC-SEC-004: AuditService KHÔNG được gọi nếu save thất bại (TC-COND-008 reverse)
    @Test
    void createProfile_saveThrowsDataIntegrityViolation_auditServiceNotCalled() {
        CreatePartnerProfileRequest request = makeValidRequest();

        when(partnerOrganizationRepository.findByRepresentativeUserId(ACTOR_ID)).thenReturn(Optional.empty());
        when(partnerOrganizationRepository.existsByEmail(any())).thenReturn(false);
        when(partnerOrganizationRepository.save(any(PartnerOrganization.class)))
                .thenThrow(new DataIntegrityViolationException("uk_partner_representative"));

        PartnerException ex = assertThrows(PartnerException.class,
                () -> partnerProfileService.createProfile(request, ACTOR_ID));

        // Oracle: ADR-004 — no audit for failed save (race condition caught by DB constraint)
        assertEquals("PTR-002", ex.getCode());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }
}
