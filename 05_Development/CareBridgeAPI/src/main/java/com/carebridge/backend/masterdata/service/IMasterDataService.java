package com.carebridge.backend.masterdata.service;

import com.carebridge.backend.masterdata.dto.response.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMasterDataService {
    List<ProvinceResponse> getProvinces();
    List<DistrictResponse> getDistrictsByProvince(String provinceId);
    List<SpecialtyResponse> getSpecialties();
    List<HospitalResponse> getHospitals(String provinceId, String districtId, String query);
    Optional<HospitalResponse> getHospitalById(String hospitalId);
}
