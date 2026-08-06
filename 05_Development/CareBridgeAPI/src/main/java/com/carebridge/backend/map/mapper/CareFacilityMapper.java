package com.carebridge.backend.map.mapper;

import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.entity.CareFacility;
import org.springframework.stereotype.Component;

@Component
public class CareFacilityMapper {

    public FacilityResponse toResponse(CareFacility entity, Integer distanceMeters) {
        return FacilityResponse.builder()
                .facilityId(entity.getFacilityId())
                .name(entity.getName())
                .facilityType(entity.getFacilityType())
                .facilityLevel(entity.getFacilityLevel())
                .ownershipType(entity.getOwnershipType())
                .address(entity.getAddress())
                .provinceId(entity.getProvinceId())
                .districtId(entity.getDistrictId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .phone(entity.getPhone())
                .openingHoursJson(entity.getOpeningHoursJson())
                .sourceType(entity.getSourceType())
                .externalSourceId(entity.getExternalSourceId())
                .verificationStatus(entity.getVerificationStatus() != null
                        ? entity.getVerificationStatus().name() : "UNVERIFIED")
                .distanceMeters(distanceMeters)
                .active(entity.getActive())
                .searchable(entity.getSearchable())
                .build();
    }

    public FacilityResponse toResponse(CareFacility entity) {
        return toResponse(entity, null);
    }
}
