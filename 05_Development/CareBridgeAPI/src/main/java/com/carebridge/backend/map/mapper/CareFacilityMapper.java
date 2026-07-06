package com.carebridge.backend.map.mapper;

import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.entity.CareFacility;
import org.springframework.stereotype.Component;

@Component
public class CareFacilityMapper {

    public FacilityResponse toResponse(CareFacility entity, Integer distanceMeters) {
        return FacilityResponse.builder()
                .facilityId(entity.getFacilityId())
                .partnerId(entity.getPartnerId())
                .name(entity.getName())
                .facilityType(entity.getFacilityType())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .phone(entity.getPhone())
                .openingHoursJson(entity.getOpeningHoursJson())
                .sourceType(entity.getSourceType())
                .verificationStatus(entity.getVerificationStatus() != null
                        ? entity.getVerificationStatus().name() : "UNVERIFIED")
                .distanceMeters(distanceMeters)
                .build();
    }

    public FacilityResponse toResponse(CareFacility entity) {
        return toResponse(entity, null);
    }
}
