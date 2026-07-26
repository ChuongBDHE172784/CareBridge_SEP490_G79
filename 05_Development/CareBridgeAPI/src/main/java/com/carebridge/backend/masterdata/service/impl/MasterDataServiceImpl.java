package com.carebridge.backend.masterdata.service.impl;

import com.carebridge.backend.masterdata.dto.response.*;
import com.carebridge.backend.masterdata.entity.*;
import com.carebridge.backend.masterdata.repository.*;
import com.carebridge.backend.masterdata.service.IMasterDataService;
import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MasterDataServiceImpl implements IMasterDataService {

    private static final String PROVINCE = "PROVINCE";
    private static final String DISTRICT = "DISTRICT";
    private static final String WARD = "WARD";

    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CareFacilityRepository careFacilityRepository;
    private final MasterDataMapper masterDataMapper;

    @Override
    public List<ProvinceResponse> getProvinces() {
        return administrativeAreaRepository.findByAreaTypeOrderByNameAsc(PROVINCE).stream()
                .map(p -> ProvinceResponse.builder()
                        .provinceId(p.getLegacyCode())
                        .name(p.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvince(String provinceId) {
        return administrativeAreaRepository.findDistrictsByProvinceCode(provinceId).stream()
                .map(d -> DistrictResponse.builder()
                        .districtId(d.getLegacyCode())
                        .provinceId(provinceId)
                        .name(d.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecialtyResponse> getSpecialties() {
        return specialtyRepository.findByIsActiveTrueOrderByName().stream()
                .map(s -> SpecialtyResponse.builder()
                        .specialtyId(s.getSpecialtyId().toString())
                        .name(s.getName())
                        .description(s.getDescription())
                        .category(s.getCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalResponse> getHospitals(String provinceId, String districtId, String query) {
        List<CareFacility> hospitals;
        if (provinceId == null) {
            hospitals = careFacilityRepository.findByActiveTrueOrderByNameAsc();
        } else if (districtId == null) {
            if (query == null || query.isBlank()) {
                hospitals = careFacilityRepository.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId);
            } else {
                hospitals = careFacilityRepository.searchActiveInProvince(provinceId, query);
            }
        } else {
            hospitals = careFacilityRepository
                    .findByProvinceIdAndDistrictIdAndActiveTrueOrderByNameAsc(provinceId, districtId);
        }

        return hospitals.stream()
                .map(h -> HospitalResponse.builder()
                        .hospitalId(h.getFacilityId().toString())
                        .name(h.getName())
                        .provinceId(h.getProvinceId())
                        .districtId(h.getDistrictId())
                        .address(h.getAddress())
                        .level(h.getFacilityLevel())
                        .type(h.getFacilityType())
                        .phone(h.getPhone())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalResponse> getHospitalById(String hospitalId) {
        return findActiveFacility(hospitalId).map(h -> HospitalResponse.builder()
                .hospitalId(h.getFacilityId().toString())
                .name(h.getName())
                .provinceId(h.getProvinceId())
                .districtId(h.getDistrictId())
                .address(h.getAddress())
                .level(h.getFacilityLevel())
                .type(h.getFacilityType())
                .phone(h.getPhone())
                .build());
    }

    @Override
    public List<WardResponse> getWardsByDistrict(String districtId) {
        if (districtId == null || districtId.isBlank()) {
            return List.of();
        }

        var district = administrativeAreaRepository.findByCode(DISTRICT + ":" + districtId)
                .filter(area -> DISTRICT.equals(area.getAreaType()))
                .filter(area -> districtId.equals(area.getLegacyCode()))
                .orElse(null);
        if (district == null || district.getParentAreaId() == null) {
            return List.of();
        }

        var province = administrativeAreaRepository.findById(district.getParentAreaId())
                .filter(area -> PROVINCE.equals(area.getAreaType()))
                .orElse(null);
        if (province == null || province.getLegacyCode() == null) {
            return List.of();
        }

        return administrativeAreaRepository
                .findByAreaTypeAndParentAreaIdOrderByNameAsc(WARD, district.getId()).stream()
                .map(ward -> masterDataMapper.toWardResponse(
                        ward, district.getLegacyCode(), province.getLegacyCode()))
                .collect(Collectors.toList());
    }

    private Optional<CareFacility> findActiveFacility(String identifier) {
        try {
            return careFacilityRepository.findByFacilityIdAndActiveTrue(
                    java.util.UUID.fromString(identifier));
        } catch (IllegalArgumentException ignored) {
            return careFacilityRepository.findByExternalSourceIdAndActiveTrue(identifier);
        }
    }
}
