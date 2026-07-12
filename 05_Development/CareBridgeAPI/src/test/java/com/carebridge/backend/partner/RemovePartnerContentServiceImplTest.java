package com.carebridge.backend.partner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.RemovalRequest;
import com.carebridge.backend.partner.entity.*;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.repository.*;
import com.carebridge.backend.partner.service.PartnerContentRemovalServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemovePartnerContentServiceImplTest {
    static final UUID ADMIN = UUID.fromString("f0100000-0000-0000-0000-0000000000ad");
    static final UUID SERVICE = UUID.fromString("f0200000-0000-0000-0000-00000000000d");
    static final UUID CAMPAIGN = UUID.fromString("f0200000-0000-0000-0000-00000000000c");
    @Mock PartnerServiceRepository serviceRepository;
    @Mock SponsoredCampaignRepository campaignRepository;
    @Mock AuditService auditService;
    @InjectMocks PartnerContentRemovalServiceImpl service;

    @Test void prcTc801_removesServiceWithoutChangingApproval() {
        PartnerService target = PartnerService.builder().id(SERVICE).approvalStatus(ServiceApprovalStatus.APPROVED).build();
        when(serviceRepository.findById(SERVICE)).thenReturn(Optional.of(target));
        var response = service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest("policy"), ADMIN);
        assertTrue(response.isRemoved()); assertTrue(target.isRemoved()); assertEquals(ADMIN, target.getRemovedBy());
        assertEquals("policy", target.getRemovalReason()); assertNotNull(target.getRemovedAt());
        assertEquals(ServiceApprovalStatus.APPROVED, target.getApprovalStatus());
    }

    @Test void prcTc802_removesCampaign() {
        SponsoredCampaign target = SponsoredCampaign.builder().id(CAMPAIGN).approvalStatus(CampaignApprovalStatus.APPROVED).build();
        when(campaignRepository.findById(CAMPAIGN)).thenReturn(Optional.of(target));
        service.remove(PartnerContentTargetType.CAMPAIGN, CAMPAIGN, new RemovalRequest("policy"), ADMIN);
        assertTrue(target.isRemoved()); assertEquals(ADMIN, target.getRemovedBy());
    }

    @Test void prcTc803_rejectsAlreadyRemoved() {
        PartnerService target = PartnerService.builder().id(SERVICE).removed(true).build();
        when(serviceRepository.findById(SERVICE)).thenReturn(Optional.of(target));
        PartnerException error = assertThrows(PartnerException.class,
                () -> service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest("again"), ADMIN));
        assertEquals("PTR-028", error.getCode()); verify(serviceRepository, never()).save(any());
    }

    @Test void prcTc804_rejectsBlankReason() {
        PartnerException error = assertThrows(PartnerException.class,
                () -> service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest(" "), ADMIN));
        assertEquals("PTR-029", error.getCode()); verifyNoInteractions(serviceRepository, campaignRepository);
    }

    @Test void prcTc805_neverHardDeletes() {
        when(serviceRepository.findById(SERVICE)).thenReturn(Optional.of(PartnerService.builder().id(SERVICE).build()));
        service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest("policy"), ADMIN);
        verify(serviceRepository, never()).delete(any()); verify(serviceRepository, never()).deleteById(any());
        verify(campaignRepository, never()).delete(any()); verify(campaignRepository, never()).deleteById(any());
    }

    @Test void prcTc807_missingTarget() {
        when(serviceRepository.findById(SERVICE)).thenReturn(Optional.empty());
        PartnerException error = assertThrows(PartnerException.class,
                () -> service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest("policy"), ADMIN));
        assertEquals("PTR-026", error.getCode());
    }

    @Test void prcTc808_auditsOnce() {
        when(serviceRepository.findById(SERVICE)).thenReturn(Optional.of(PartnerService.builder().id(SERVICE).build()));
        service.remove(PartnerContentTargetType.SERVICE, SERVICE, new RemovalRequest("policy"), ADMIN);
        verify(auditService).log(AuditAction.PARTNER_CONTENT_REMOVED, ADMIN, "SERVICE", SERVICE.toString(), "reason=policy");
    }
}
