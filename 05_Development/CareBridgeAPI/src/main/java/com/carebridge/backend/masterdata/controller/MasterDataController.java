package com.carebridge.backend.masterdata.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.masterdata.dto.response.*;
import com.carebridge.backend.masterdata.service.IMasterDataService;
import com.carebridge.backend.masterdata.service.ProvinceCacheService;
import com.carebridge.backend.integration.trackasia.TrackAsiaService;
import com.carebridge.backend.integration.trackasia.TrackAsiaPlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/master-data")
@RequiredArgsConstructor
public class MasterDataController {

    private final IMasterDataService service;
    private final ProvinceCacheService provinceCacheService;
    private final TrackAsiaService trackAsiaService;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(provinceCacheService.getProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistricts(
            @RequestParam String provinceId) {
        return ResponseEntity.ok(ApiResponse.success(service.getDistrictsByProvince(provinceId)));
    }

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getSpecialties() {
        return ResponseEntity.ok(ApiResponse.success(service.getSpecialties()));
    }

    @GetMapping("/hospitals")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> getHospitals(
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(service.getHospitals(provinceId, districtId, q)));
    }

    @GetMapping("/hospitals/search/trackasia")
    public ResponseEntity<ApiResponse<List<TrackAsiaPlaceDto>>> searchTrackAsiaHospitals(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String provinceId) {
        // provinceId is resolved to a province name here rather than trusted as text, so
        // the caller cannot push arbitrary strings into the upstream query.
        String provinceName = provinceId == null || provinceId.isBlank()
                ? null
                : provinceCacheService.getProvinces().stream()
                        .filter(p -> provinceId.equals(p.getProvinceId()))
                        .map(ProvinceResponse::getName)
                        .findFirst()
                        .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                trackAsiaService.searchHospitals(q, provinceName)));
    }

    @GetMapping("/hospitals/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospital(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getHospitalById(id).orElseThrow()));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String provinceId) {
        if (provinceId != null && !provinceId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(provinceCacheService.getWardsByProvince(provinceId)));
        }
        return ResponseEntity.ok(ApiResponse.success(service.getWardsByDistrict(districtId)));
    }
}
