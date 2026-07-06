package com.carebridge.backend.expertavailability.mapper;

import com.carebridge.backend.expertavailability.dto.request.ShareLocationRequest;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ExpertLocationShareMapper {

    public ExpertLocationShare toEntity(UUID expertProfileId, ShareLocationRequest request) {
        return ExpertLocationShare.builder()
                .expertProfileId(expertProfileId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .availabilityStatus(request.getAvailabilityStatus())
                .expiresAt(request.getExpiresAt())
                .consentReference(request.getConsentReference())
                .build();
    }

    public LocationShareResponse toResponse(ExpertLocationShare entity) {
        return LocationShareResponse.builder()
                .locationShareId(entity.getLocationShareId())
                .expertProfileId(entity.getExpertProfileId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .accuracyMeters(entity.getAccuracyMeters())
                .availabilityStatus(entity.getAvailabilityStatus())
                .sharedAt(entity.getSharedAt    ())
                .expiresAt(entity.getExpiresAt())
                .consentReference(entity.getConsentReference())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
