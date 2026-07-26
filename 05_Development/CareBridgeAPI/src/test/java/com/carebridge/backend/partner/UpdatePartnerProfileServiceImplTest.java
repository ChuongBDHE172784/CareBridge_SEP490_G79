package com.carebridge.backend.partner;

import static com.carebridge.backend.partner.UpdatePartnerProfileTestFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.mapper.PartnerProfileMapper;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.partner.service.PartnerProfileServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdatePartnerProfileServiceImplTest {
    @Mock PartnerOrganizationRepository repository;
    @Spy PartnerProfileMapper mapper = new PartnerProfileMapper();
    @Mock AuditService auditService;
    @InjectMocks PartnerProfileServiceImpl service;

    private void editable(PartnerOrganization entity) {
        when(repository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void pupTc201_updatesEditableFieldsAndPreservesProtectedFields() {
        PartnerOrganization entity = partner(OrganizationStatus.APPROVED); editable(entity);
        var response = service.updateProfile(request(), OWNER_ID);
        assertAll(() -> assertEquals("Updated Clinic", entity.getName()),
                () -> assertEquals("+84907654321", entity.getPhone()),
                () -> assertEquals(OrganizationStatus.APPROVED, entity.getStatus()),
                () -> assertEquals(OWNER_ID, entity.getRepresentativeUserId()),
                () -> assertEquals(OrganizationStatus.APPROVED, response.status()));
    }

    @Test void pupTc202_missingProfileThrowsPtr007() {
        when(repository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.empty());
        PartnerException exception = assertThrows(PartnerException.class, () -> service.updateProfile(request(), OWNER_ID));
        assertEquals("PTR-007", exception.getCode()); verify(repository, never()).save(any());
    }

    @Test void pupTc203_suspendedAndRejectedProfilesThrowPtr009() {
        for (OrganizationStatus status : new OrganizationStatus[]{OrganizationStatus.SUSPENDED, OrganizationStatus.REJECTED}) {
            reset(repository); when(repository.findByRepresentativeUserId(OWNER_ID)).thenReturn(Optional.of(partner(status)));
            PartnerException exception = assertThrows(PartnerException.class, () -> service.updateProfile(request(), OWNER_ID));
            assertEquals("PTR-009", exception.getCode());
        }
    }

    @Test void pupTc205_successAuditedExactlyOnce() {
        PartnerOrganization entity = partner(OrganizationStatus.APPROVED); editable(entity);
        service.updateProfile(request(), OWNER_ID);
        verify(auditService, times(1)).log(eq(AuditAction.PARTNER_PROFILE_UPDATED), eq(OWNER_ID),
                eq("PartnerOrganization"), eq(PARTNER_ID.toString()), any());
    }

    @Test void pupTc206_resolvesOnlyByCurrentUserId() {
        PartnerOrganization entity = partner(OrganizationStatus.APPROVED); editable(entity);
        service.updateProfile(request(), OWNER_ID);
        verify(repository).findByRepresentativeUserId(OWNER_ID);
        ArgumentCaptor<PartnerOrganization> captor = ArgumentCaptor.forClass(PartnerOrganization.class);
        verify(repository).save(captor.capture()); assertEquals(PARTNER_ID, captor.getValue().getId());
    }

    @Test void pupTc207_statusCannotBeChangedByUpdate() {
        PartnerOrganization entity = partner(OrganizationStatus.APPROVED); editable(entity);
        service.updateProfile(request(), OWNER_ID); assertEquals(OrganizationStatus.APPROVED, entity.getStatus());
    }
}
