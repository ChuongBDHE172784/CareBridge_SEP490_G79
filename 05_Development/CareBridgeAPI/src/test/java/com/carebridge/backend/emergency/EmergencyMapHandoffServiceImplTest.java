package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import com.carebridge.backend.emergency.repository.EmergencyMapHandoffRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.impl.EmergencyMapHandoffServiceImpl;
import com.carebridge.backend.map.mapper.EmergencyMapHandoffMapper;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyMapHandoffServiceImplTest {

    @Mock private EmergencyMapHandoffRepository handoffRepository;
    @Mock private EmergencyMapHandoffMapper handoffMapper;
    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private CareFacilityRepository careFacilityRepository;
    @Mock private TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    @Mock private FamilyMemberPort familyMemberPort;
    @Mock private LocationConsentPort locationConsentPort;
    @InjectMocks private EmergencyMapHandoffServiceImpl service;

    @Test
    void rejectsTriageContextNotOwnedByCaller() {
        UUID ownerId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        CreateEmergencyHandoffRequest request = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(intakeId)
                .riskLevel("RED")
                .build();
        when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createHandoff(ownerId, request))
                .isInstanceOf(EmergencyException.class)
                .extracting("code").isEqualTo("EMER-006");
        verify(handoffRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validatesFacilityAndLinksCanonicalEmergencyWhenAvailable() {
        UUID ownerId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID emergencyId = UUID.randomUUID();
        IntakeSession intake = IntakeSession.builder()
                .id(intakeId)
                .userId(ownerId)
                .status(IntakeStatus.COMPLETED)
                .riskLevel(RiskLevel.RED)
                .build();
        CreateEmergencyHandoffRequest request = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(intakeId)
                .riskLevel("RED")
                .selectedFacilityId(facilityId)
                .build();
        EmergencyMapHandoff handoff = EmergencyMapHandoff.builder()
                .userId(ownerId)
                .triageHandoffId(intakeId)
                .selectedFacilityId(facilityId)
                .status(HandoffStatus.OPEN)
                .build();
        EmergencyHandoffResponse expected = EmergencyHandoffResponse.builder()
                .handoffId(UUID.randomUUID())
                .userId(ownerId)
                .build();
        when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                .thenReturn(Optional.of(intake));
        when(careFacilityRepository.existsById(facilityId)).thenReturn(true);
        when(triageEscalationLinkRepository.findEmergencySessionId(intakeId, ownerId))
                .thenReturn(Optional.of(emergencyId));
        when(handoffMapper.toEntity(ownerId, request)).thenReturn(handoff);
        when(handoffRepository.insert(handoff)).thenReturn(handoff);
        when(handoffMapper.toResponse(handoff)).thenReturn(expected);

        EmergencyHandoffResponse response = service.createHandoff(ownerId, request);

        assertThat(response).isSameAs(expected);
        assertThat(handoff.getSafetyEventId()).isEqualTo(emergencyId);
        assertThat(handoff.getRiskLevel()).isEqualTo("RED");
        assertThat(handoff.getStatus()).isEqualTo(HandoffStatus.ACCEPTED);
        verify(handoffRepository).insert(handoff);
    }

    @Test
    void redactsCoordinatesBeforePersistenceWithoutLocationConsent() {
        UUID ownerId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        IntakeSession intake = IntakeSession.builder()
                .id(intakeId)
                .userId(ownerId)
                .status(IntakeStatus.COMPLETED)
                .riskLevel(RiskLevel.RED)
                .build();
        CreateEmergencyHandoffRequest request = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(intakeId)
                .riskLevel("RED")
                .userLatitude(new BigDecimal("10.762622"))
                .userLongitude(new BigDecimal("106.660172"))
                .build();
        EmergencyMapHandoff handoff = EmergencyMapHandoff.builder()
                .userId(ownerId)
                .triageHandoffId(intakeId)
                .userLatitude(request.getUserLatitude())
                .userLongitude(request.getUserLongitude())
                .build();
        when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                .thenReturn(Optional.of(intake));
        when(handoffMapper.toEntity(ownerId, request)).thenReturn(handoff);
        when(locationConsentPort.hasLocationConsent(ownerId)).thenReturn(false);
        when(handoffRepository.insert(handoff)).thenReturn(handoff);

        service.createHandoff(ownerId, request);

        assertThat(handoff.getUserLatitude()).isNull();
        assertThat(handoff.getUserLongitude()).isNull();
        verify(handoffRepository).insert(handoff);
    }

    @Test
    void retainsValidCoordinatesWhenLocationConsentExists() {
        UUID ownerId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        IntakeSession intake = IntakeSession.builder()
                .id(intakeId)
                .userId(ownerId)
                .riskLevel(RiskLevel.RED)
                .build();
        CreateEmergencyHandoffRequest request = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(intakeId)
                .riskLevel("RED")
                .userLatitude(new BigDecimal("-90"))
                .userLongitude(new BigDecimal("180"))
                .build();
        EmergencyMapHandoff handoff = EmergencyMapHandoff.builder()
                .userId(ownerId)
                .userLatitude(request.getUserLatitude())
                .userLongitude(request.getUserLongitude())
                .build();
        when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                .thenReturn(Optional.of(intake));
        when(handoffMapper.toEntity(ownerId, request)).thenReturn(handoff);
        when(locationConsentPort.hasLocationConsent(ownerId)).thenReturn(true);
        when(handoffRepository.insert(handoff)).thenReturn(handoff);

        service.createHandoff(ownerId, request);

        assertThat(handoff.getUserLatitude()).isEqualByComparingTo("-90");
        assertThat(handoff.getUserLongitude()).isEqualByComparingTo("180");
    }

    @Test
    void rejectsIncompleteOrOutOfRangeCoordinatesBeforePersistence() {
        UUID ownerId = UUID.randomUUID();
        CreateEmergencyHandoffRequest incomplete = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(UUID.randomUUID())
                .riskLevel("RED")
                .userLatitude(BigDecimal.ZERO)
                .build();
        CreateEmergencyHandoffRequest outOfRange = CreateEmergencyHandoffRequest.builder()
                .triageHandoffId(UUID.randomUUID())
                .riskLevel("RED")
                .userLatitude(new BigDecimal("90.0000001"))
                .userLongitude(new BigDecimal("1E1000"))
                .build();

        assertThatThrownBy(() -> service.createHandoff(ownerId, incomplete))
                .isInstanceOf(EmergencyException.class)
                .extracting("code").isEqualTo("EMER-010");
        assertThatThrownBy(() -> service.createHandoff(ownerId, outOfRange))
                .isInstanceOf(EmergencyException.class)
                .extracting("code").isEqualTo("EMER-010");
        verify(handoffRepository, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(locationConsentPort, never()).hasLocationConsent(ownerId);
    }

    @Test
    void crossOwnerWhoIsNotAuthorizedFamilyCannotReadSensitiveHandoff() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        UUID handoffId = UUID.randomUUID();
        EmergencyMapHandoff handoff = EmergencyMapHandoff.builder()
                .handoffId(handoffId)
                .userId(ownerId)
                .summary("Sensitive symptom summary")
                .build();
        when(handoffRepository.findById(handoffId)).thenReturn(Optional.of(handoff));
        when(familyMemberPort.isFamilyMember(ownerId, callerId)).thenReturn(false);

        assertThatThrownBy(() -> service.getHandoff(handoffId, callerId, false))
                .isInstanceOf(EmergencyException.class)
                .extracting("code").isEqualTo("EMER-009");
        verify(handoffMapper, never()).toResponse(handoff);
    }

    @Test
    void systemAdminCanReadCrossOwnerHandoffWithoutFamilyMembership() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        UUID handoffId = UUID.randomUUID();
        EmergencyMapHandoff handoff = EmergencyMapHandoff.builder()
                .handoffId(handoffId)
                .userId(ownerId)
                .build();
        EmergencyHandoffResponse response = EmergencyHandoffResponse.builder()
                .handoffId(handoffId)
                .userId(ownerId)
                .build();
        when(handoffRepository.findById(handoffId)).thenReturn(Optional.of(handoff));
        when(handoffMapper.toResponse(handoff)).thenReturn(response);

        assertThat(service.getHandoff(handoffId, callerId, true)).isSameAs(response);
    }
}
