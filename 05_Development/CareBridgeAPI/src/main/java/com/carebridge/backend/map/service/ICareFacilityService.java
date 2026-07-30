package com.carebridge.backend.map.service;

import com.carebridge.backend.map.dto.request.NearbySearchRequest;
import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.dto.response.NearbyResponse;
import com.carebridge.backend.map.dto.response.RouteResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ICareFacilityService {

    NearbyResponse searchNearby(BigDecimal lat, BigDecimal lng, Integer radiusMeters, String type,
                                String provinceId, String districtId);

    List<FacilityResponse> getAllFacilities();

    FacilityResponse getFacilityById(UUID id);

    RouteResponse getRoute(RouteRequest request);

    List<FacilityResponse> getPendingFacilities();

    void verifyFacility(UUID facilityId, com.carebridge.backend.map.facilitystatus.FacilityStatus status, UUID adminId);
}
