package com.carebridge.backend.expertavailability.mapper;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.response.AvailabilityResponse;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ExpertAvailabilityMapper {

    public ExpertAvailability toEntity(UUID expertProfileId, CreateAvailabilityRequest request) {
        return ExpertAvailability.builder()
                .expertProfileId(expertProfileId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .channelType(request.getChannelType())
                .status(AvailabilityStatus.AVAILABLE)
                .build();
    }

    public AvailabilityResponse toResponse(ExpertAvailability entity) {
        return AvailabilityResponse.builder()
                .availabilityId(entity.getAvailabilityId())
                .expertProfileId(entity.getExpertProfileId())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .channelType(entity.getChannelType())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
