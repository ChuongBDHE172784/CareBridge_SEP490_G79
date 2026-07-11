package com.carebridge.backend.partner;

import static com.carebridge.backend.partner.SubmitServiceListingTestFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.entity.*;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.repository.*;
import com.carebridge.backend.partner.service.PartnerServiceServiceImpl;
import com.carebridge.backend.partner.mapper.PartnerServiceMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitServiceListingServiceImplTest {
    @Mock PartnerOrganizationRepository orgRepository;
    @Mock PartnerServiceRepository serviceRepository;
    @Mock AuditService auditService;
    @Spy PartnerServiceMapper mapper = new PartnerServiceMapper();
    @InjectMocks PartnerServiceServiceImpl service;
    private void approved() {
        when(orgRepository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.of(org(OrganizationStatus.APPROVED)));
        when(serviceRepository.save(any())).thenReturn(saved());
    }
    @Test void pslTc301_approvedOrgCreatesPendingListing() {
        approved(); var response = service.submitService(request("VND"), OWNER_ID);
        ArgumentCaptor<PartnerService> captor = ArgumentCaptor.forClass(PartnerService.class);
        verify(serviceRepository).save(captor.capture());
        assertAll(() -> assertEquals(ORG_ID, captor.getValue().getPartnerId()),
                () -> assertEquals(ServiceApprovalStatus.PENDING, captor.getValue().getApprovalStatus()),
                () -> assertEquals(ServiceApprovalStatus.PENDING, response.approvalStatus()));
    }
    @Test void pslTc302_missingOrgThrowsPtr010() {
        when(orgRepository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.empty());
        PartnerException ex = assertThrows(PartnerException.class, () -> service.submitService(request("VND"), OWNER_ID));
        assertEquals("PTR-010", ex.getCode()); verify(serviceRepository, never()).save(any());
    }
    @Test void pslTc303_nonApprovedStatusesThrowPtr011() {
        for (OrganizationStatus status : new OrganizationStatus[]{OrganizationStatus.PENDING_APPROVAL, OrganizationStatus.SUSPENDED, OrganizationStatus.REJECTED}) {
            reset(orgRepository, serviceRepository); when(orgRepository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.of(org(status)));
            assertEquals("PTR-011", assertThrows(PartnerException.class, () -> service.submitService(request("VND"), OWNER_ID)).getCode());
        }
    }
    @Test void pslTc305_statusIsAlwaysPending() { approved(); service.submitService(request("VND"), OWNER_ID); ArgumentCaptor<PartnerService> c=ArgumentCaptor.forClass(PartnerService.class); verify(serviceRepository).save(c.capture()); assertEquals(ServiceApprovalStatus.PENDING,c.getValue().getApprovalStatus()); }
    @Test void pslTc306_partnerIdComesFromResolvedOrg() { approved(); service.submitService(request("VND"), OWNER_ID); ArgumentCaptor<PartnerService> c=ArgumentCaptor.forClass(PartnerService.class); verify(serviceRepository).save(c.capture()); assertEquals(ORG_ID,c.getValue().getPartnerId()); }
    @Test void pslTc307_nullCurrencyDefaultsToVnd() { approved(); service.submitService(request(null), OWNER_ID); ArgumentCaptor<PartnerService> c=ArgumentCaptor.forClass(PartnerService.class); verify(serviceRepository).save(c.capture()); assertEquals("VND",c.getValue().getCurrency()); }
    @Test void pslTc308_successAuditedOnce() { approved(); service.submitService(request("VND"), OWNER_ID); verify(auditService).log(eq(AuditAction.PARTNER_SERVICE_SUBMITTED),eq(OWNER_ID),eq("PartnerService"),eq(SERVICE_ID.toString()),any()); }
}
