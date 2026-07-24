package com.carebridge.backend.masterdata.service;

import com.carebridge.backend.masterdata.dto.MasterDataDTO;
import com.carebridge.backend.masterdata.entity.*;
import com.carebridge.backend.masterdata.repository.*;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterDataService {
    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CareFacilityRepository careFacilityRepository;

    public List<MasterDataDTO> getProvinces() {
        return administrativeAreaRepository.findByAreaTypeOrderByNameAsc("PROVINCE").stream()
                .map(p -> MasterDataDTO.builder().id(p.getLegacyCode()).name(p.getName()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getDistricts(String provinceId) {
        return administrativeAreaRepository.findDistrictsByProvinceCode(provinceId).stream()
                .map(d -> MasterDataDTO.builder().id(d.getLegacyCode()).name(d.getName()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getSpecialties() {
        return specialtyRepository.findByIsActiveTrueOrderByName().stream()
                .map(s -> MasterDataDTO.builder().id(s.getSpecialtyId().toString()).name(s.getName())
                        .description(s.getDescription()).category(s.getCode()).build())
                .collect(Collectors.toList());
    }

    public List<MasterDataDTO> getHospitals(String provinceId) {
        return careFacilityRepository.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId).stream()
                .map(h -> MasterDataDTO.builder().id(h.getFacilityId().toString()).name(h.getName()).build())
                .collect(Collectors.toList());
    }
}
