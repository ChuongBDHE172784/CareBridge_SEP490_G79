package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.EmergencyAlertAcknowledgementRepository;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.EmergencyAlertAcknowledgementService;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyAlertAcknowledgementServiceTest {

    @Mock private IEmergencySessionRepository emergencySessionRepository;
    @Mock private FamilyMemberPort familyMemberPort;
    @Mock private EmergencyAlertAcknowledgementRepository acknowledgementRepository;
    @Mock private AuditService auditService;
    @InjectMocks private EmergencyAlertAcknowledgementService service;

    private final UUID motherId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @Test
    void familyMemberCanPersistAcknowledgementOnExistingNotificationRecords() {
        when(emergencySessionRepository.findById(sessionId)).thenReturn(Optional.of(session()));
        when(familyMemberPort.isFamilyMember(motherId, familyId)).thenReturn(true);
        when(acknowledgementRepository.find(sessionId, familyId))
                .thenReturn(new EmergencyAlertAcknowledgementRepository.AcknowledgementState(
                        true, false, null));
        when(acknowledgementRepository.acknowledge(eq(sessionId), eq(familyId), any(Instant.class)))
                .thenReturn(1);

        service.acknowledge(sessionId, familyId);

        verify(acknowledgementRepository).acknowledge(
                eq(sessionId), eq(familyId), any(Instant.class));
        verify(auditService).log(any(), eq(familyId), eq("EmergencySession"),
                eq(sessionId.toString()), any(java.util.Map.class));
    }

    @Test
    void nonFamilyCallerCannotAcknowledgeAlert() {
        when(emergencySessionRepository.findById(sessionId)).thenReturn(Optional.of(session()));
        when(familyMemberPort.isFamilyMember(motherId, familyId)).thenReturn(false);

        assertThatThrownBy(() -> service.acknowledge(sessionId, familyId))
                .isInstanceOf(EmergencyException.class);

        verify(acknowledgementRepository, never()).acknowledge(any(), any(), any());
    }

    private EmergencySession session() {
        return EmergencySession.builder()
                .id(sessionId)
                .userId(motherId)
                .status(EmergencyStatus.ACTIVE)
                .triggerSource("FALL_DETECTION")
                .createdAt(Instant.now())
                .build();
    }
}
