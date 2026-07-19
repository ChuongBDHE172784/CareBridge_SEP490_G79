package com.carebridge.backend.masterdata.service.impl;

import com.carebridge.backend.masterdata.dto.response.*;
import com.carebridge.backend.masterdata.entity.*;
import com.carebridge.backend.masterdata.repository.*;
import com.carebridge.backend.masterdata.service.IMasterDataService;
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

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SpecialtyRepository specialtyRepository;
    private final HospitalRepository hospitalRepository;

    @Override
    public List<ProvinceResponse> getProvinces() {
        return provinceRepository.findByIsActiveTrueOrderByName().stream()
                .map(p -> ProvinceResponse.builder()
                        .provinceId(p.getProvinceId())
                        .name(p.getName())
                        .nameEn(p.getNameEn())
                        .region(p.getRegion())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvince(String provinceId) {
        return districtRepository.findByProvinceIdAndIsActiveTrueOrderByName(provinceId).stream()
                .map(d -> DistrictResponse.builder()
                        .districtId(d.getDistrictId())
                        .provinceId(d.getProvinceId())
                        .name(d.getName())
                        .nameEn(d.getNameEn())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecialtyResponse> getSpecialties() {
        return specialtyRepository.findByIsActiveTrueOrderByName().stream()
                .map(s -> SpecialtyResponse.builder()
                        .specialtyId(s.getSpecialtyId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .category(s.getCategory())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalResponse> getHospitals(String provinceId, String districtId, String query) {
        List<Hospital> hospitals;
        if (provinceId == null) {
            hospitals = hospitalRepository.findByIsActiveTrueOrderByName();
        } else if (districtId == null) {
            if (query == null || query.isBlank()) {
                hospitals = hospitalRepository.findByProvinceIdAndIsActiveTrueOrderByName(provinceId);
            } else {
                hospitals = hospitalRepository.searchInProvince(provinceId, query);
            }
        } else {
            hospitals = hospitalRepository.findByProvinceIdAndDistrictIdAndIsActiveTrueOrderByName(provinceId, districtId);
        }

        return hospitals.stream()
                .map(h -> HospitalResponse.builder()
                        .hospitalId(h.getHospitalId())
                        .name(h.getName())
                        .provinceId(h.getProvinceId())
                        .districtId(h.getDistrictId())
                        .address(h.getAddress())
                        .level(h.getLevel())
                        .type(h.getType())
                        .phone(h.getPhone())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalResponse> getHospitalById(String hospitalId) {
        return hospitalRepository.findById(hospitalId).map(h -> HospitalResponse.builder()
                .hospitalId(h.getHospitalId())
                .name(h.getName())
                .provinceId(h.getProvinceId())
                .districtId(h.getDistrictId())
                .address(h.getAddress())
                .level(h.getLevel())
                .type(h.getType())
                .phone(h.getPhone())
                .build());
    }
}
