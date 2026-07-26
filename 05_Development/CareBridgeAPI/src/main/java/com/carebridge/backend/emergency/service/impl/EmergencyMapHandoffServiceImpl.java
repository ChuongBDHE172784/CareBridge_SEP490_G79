package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import com.carebridge.backend.map.mapper.EmergencyMapHandoffMapper;
import com.carebridge.backend.emergency.repository.EmergencyMapHandoffRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.emergency.service.IEmergencyMapHandoffService;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EmergencyMapHandoffServiceImpl implements IEmergencyMapHandoffService {

    private final EmergencyMapHandoffRepository handoffRepository;
    private final EmergencyMapHandoffMapper handoffMapper;
    private final IIntakeSessionRepository intakeSessionRepository;
    private final CareFacilityRepository careFacilityRepository;
    private final TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    private final FamilyMemberPort familyMemberPort;
    private final LocationConsentPort locationConsentPort;

    @Override
    public EmergencyHandoffResponse createHandoff(UUID userId, CreateEmergencyHandoffRequest request) {
        validateCoordinates(request.getUserLatitude(), request.getUserLongitude());
        var intake = intakeSessionRepository.findByIdAndUserId(request.getTriageHandoffId(), userId)
                .orElseThrow(() -> new com.carebridge.backend.emergency.exception.EmergencyException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "EMER-006",
                        "Owned triage context was not found"));
        if (intake.getRiskLevel() == null
                || !intake.getRiskLevel().name().equalsIgnoreCase(request.getRiskLevel())) {
            throw new com.carebridge.backend.emergency.exception.EmergencyException(
                    org.springframework.http.HttpStatus.CONFLICT, "EMER-007",
                    "Handoff risk level does not match the canonical triage result");
        }
        if (request.getSelectedFacilityId() != null
                && !careFacilityRepository.existsById(request.getSelectedFacilityId())) {
            throw new com.carebridge.backend.emergency.exception.EmergencyException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "EMER-008",
                    "Selected care facility was not found");
        }

        var handoff = handoffMapper.toEntity(userId, request);
        boolean hasCoordinates = request.getUserLatitude() != null;
        if (hasCoordinates && !locationConsentPort.hasLocationConsent(userId)) {
            handoff.setUserLatitude(null);
            handoff.setUserLongitude(null);
        }
        handoff.setRiskLevel(intake.getRiskLevel().name());
        triageEscalationLinkRepository
                .findEmergencySessionId(intake.getId(), userId)
                .ifPresent(handoff::setSafetyEventId);

        // Auto-accept if risk level is RED (emergency protocol)
        if ("RED".equalsIgnoreCase(request.getRiskLevel())) {
            handoff.setStatus(HandoffStatus.ACCEPTED);
        }
        var saved = handoffRepository.insert(handoff);
        return handoffMapper.toResponse(saved);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)
                || latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                    || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                    || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                    || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new com.carebridge.backend.emergency.exception.EmergencyException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "EMER-010",
                    "Location coordinates must be a complete valid latitude/longitude pair");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyHandoffResponse getHandoff(UUID handoffId, UUID callerId, boolean systemAdmin) {
        EmergencyMapHandoff handoff = handoffRepository.findById(handoffId)
                .orElseThrow(() -> new com.carebridge.backend.emergency.exception.EmergencyException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "EMER-001", "Emergency handoff not found"));
        boolean owner = handoff.getUserId().equals(callerId);
        boolean authorizedFamily = !systemAdmin && !owner
                && familyMemberPort.isFamilyMember(handoff.getUserId(), callerId);
        if (!systemAdmin && !owner && !authorizedFamily) {
            throw new com.carebridge.backend.emergency.exception.EmergencyException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "EMER-009",
                    "You are not authorized to view this emergency handoff");
        }
        return handoffMapper.toResponse(handoff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyHandoffResponse> getMyHandoffs(UUID userId) {
        return handoffRepository.findByUserId(userId).stream()
                .map(handoffMapper::toResponse)
                .collect(Collectors.toList());
    }
}
