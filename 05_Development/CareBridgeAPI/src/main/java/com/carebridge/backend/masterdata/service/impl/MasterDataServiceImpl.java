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

    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CareFacilityRepository careFacilityRepository;

    @Override
    public List<ProvinceResponse> getProvinces() {
        return administrativeAreaRepository.findByAreaTypeOrderByNameAsc("PROVINCE").stream()
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

    private Optional<CareFacility> findActiveFacility(String identifier) {
        try {
            return careFacilityRepository.findByFacilityIdAndActiveTrue(
                    java.util.UUID.fromString(identifier));
        } catch (IllegalArgumentException ignored) {
            return careFacilityRepository.findByExternalSourceIdAndActiveTrue(identifier);
        }
    }
}
