package com.carebridge.backend.nearbycare.mapper;

import com.carebridge.backend.nearbycare.dto.request.CreateNearbySupportRequest;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportRequestResponse;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportResponseResponse;
import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class NearbySupportMapper {

    public NearbySupportRequest toEntity(UUID requesterUserId, CreateNearbySupportRequest request) {
        return NearbySupportRequest.builder()
                .requesterUserId(requesterUserId)
                .supportType(request.getSupportType())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .consentStatus(request.getConsentStatus())
                .build();
    }

    public NearbySupportRequestResponse toRequestResponse(NearbySupportRequest entity) {
        return NearbySupportRequestResponse.builder()
                .requestId(entity.getRequestId())
                .requesterUserId(entity.getRequesterUserId())
                .supportType(entity.getSupportType())
                .description(entity.getDescription())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .consentStatus(entity.getConsentStatus())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .respondedAt(entity.getRespondedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NearbySupportResponseResponse toResponseResponse(NearbySupportResponse entity) {
        return NearbySupportResponseResponse.builder()
                .responseId(entity.getResponseId())
                .requestId(entity.getRequestId())
                .expertProfileId(entity.getExpertProfileId())
                .action(entity.getAction() != null ? entity.getAction().name() : null)
                .note(entity.getNote())
                .respondedAt(entity.getRespondedAt())
                .build();
    }
}
