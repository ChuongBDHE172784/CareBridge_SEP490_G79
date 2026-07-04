package com.carebridge.backend.location.mapper;

import com.carebridge.backend.location.dto.request.LocationSnapshotRequest;
import com.carebridge.backend.location.dto.response.LocationSnapshotResponse;
import com.carebridge.backend.location.entity.LocationSnapshot;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class LocationSnapshotMapper {

    public LocationSnapshot toEntity(UUID userId, LocationSnapshotRequest request) {
        LocationSnapshot entity = new LocationSnapshot();
        entity.setUserId(userId);
        entity.setContextType(request.getContextType());
        entity.setContextId(request.getContextId());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setAccuracyMeters(request.getAccuracyMeters());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setConsentStatus("GRANTED");
        return entity;
    }

    public LocationSnapshotResponse toResponse(LocationSnapshot entity) {
        return LocationSnapshotResponse.builder()
                .locationSnapshotId(entity.getLocationSnapshotId())
                .userId(entity.getUserId())
                .contextType(entity.getContextType())
                .contextId(entity.getContextId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .accuracyMeters(entity.getAccuracyMeters())
                .capturedAt(entity.getCapturedAt())
                .expiresAt(entity.getExpiresAt())
                .consentStatus(entity.getConsentStatus())
                .build();
    }
}
