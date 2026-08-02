package com.carebridge.backend.map.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.dto.response.NearbyResponse;
import com.carebridge.backend.map.dto.response.RouteResponse;
import com.carebridge.backend.map.service.ICareFacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class CareFacilityController {

    private final ICareFacilityService careFacilityService;

    @GetMapping("/nearby-facilities")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<NearbyResponse>> searchNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "5000") Integer radiusMeters,
            @RequestParam(defaultValue = "hospital") String type) {
        return ResponseEntity.ok(ApiResponse.success(
                careFacilityService.searchNearby(lat, lng, radiusMeters, type)));
    }

    @GetMapping("/facilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getAllFacilities() {
        return ResponseEntity.ok(ApiResponse.success(careFacilityService.getAllFacilities()));
    }

    @GetMapping("/facilities/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FacilityResponse>> getFacility(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(careFacilityService.getFacilityById(id)));
    }

    // UC-79: View Route, ETA and Quick Call/Navigate
    @PostMapping("/route")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(careFacilityService.getRoute(request)));
    }
}
