package com.carebridge.backend.map.mapper;

import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class EmergencyMapHandoffMapper {

    public EmergencyMapHandoff toEntity(UUID userId, CreateEmergencyHandoffRequest request) {
        return EmergencyMapHandoff.builder()
                .userId(userId)
                .triageHandoffId(request.getTriageHandoffId())
                .riskLevel(request.getRiskLevel())
                .userLatitude(request.getUserLatitude())
                .userLongitude(request.getUserLongitude())
                .selectedFacilityId(request.getSelectedFacilityId())
                .summary(request.getSymptomSummary())
                .status(HandoffStatus.OPEN)
                .build();
    }

    public EmergencyHandoffResponse toResponse(EmergencyMapHandoff entity) {
        return EmergencyHandoffResponse.builder()
                .handoffId(entity.getHandoffId())
                .userId(entity.getUserId())
                .triageHandoffId(entity.getTriageHandoffId())
                .riskLevel(entity.getRiskLevel())
                .userLatitude(entity.getUserLatitude())
                .userLongitude(entity.getUserLongitude())
                .selectedFacilityId(entity.getSelectedFacilityId())
                .summary(entity.getSummary())
                .status(entity.getStatus() != null ? entity.getStatus().name() : "OPEN")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
