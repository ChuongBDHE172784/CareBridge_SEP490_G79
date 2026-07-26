package com.carebridge.backend.masterdata.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.masterdata.dto.response.*;
import com.carebridge.backend.masterdata.service.IMasterDataService;
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

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(service.getProvinces()));
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

    @GetMapping("/hospitals/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospital(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getHospitalById(id).orElseThrow()));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(
            @RequestParam String districtId) {
        return ResponseEntity.ok(ApiResponse.success(service.getWardsByDistrict(districtId)));
    }
}
