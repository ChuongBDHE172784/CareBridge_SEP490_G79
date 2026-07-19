package com.carebridge.backend.masterdata.service;

import com.carebridge.backend.masterdata.dto.MasterDataDTO;
import com.carebridge.backend.masterdata.entity.*;
import com.carebridge.backend.masterdata.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterDataService {
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SpecialtyRepository specialtyRepository;
    private final HospitalRepository hospitalRepository;

    public List<MasterDataDTO> getProvinces() {
        return provinceRepository.findByIsActiveTrueOrderByName().stream()
                .map(p -> MasterDataDTO.builder().id(p.getProvinceId()).name(p.getName()).region(p.getRegion()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getDistricts(String provinceId) {
        return districtRepository.findByProvinceIdAndIsActiveTrueOrderByName(provinceId).stream()
                .map(d -> MasterDataDTO.builder().id(d.getDistrictId()).name(d.getName()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getSpecialties() {
        return specialtyRepository.findByIsActiveTrueOrderByName().stream()
                .map(s -> MasterDataDTO.builder().id(s.getSpecialtyId()).name(s.getName()).description(s.getDescription()).category(s.getCategory()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getHospitals(String provinceId) {
        return hospitalRepository.findByProvinceIdAndIsActiveTrueOrderByName(provinceId).stream()
                .map(h -> MasterDataDTO.builder().id(h.getHospitalId()).name(h.getName()).build())
                .collect(Collectors.toList());
    }
}
