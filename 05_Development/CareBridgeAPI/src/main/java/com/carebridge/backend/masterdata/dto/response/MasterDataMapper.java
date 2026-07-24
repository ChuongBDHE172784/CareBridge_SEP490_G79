package com.carebridge.backend.masterdata.dto.response;

import com.carebridge.backend.masterdata.entity.AdministrativeArea;
import com.carebridge.backend.masterdata.entity.Specialty;
import com.carebridge.backend.map.entity.CareFacility;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {

 public ProvinceResponse toProvinceResponse(AdministrativeArea entity) {
  if (entity == null) return null;
  return ProvinceResponse.builder()
   .provinceId(entity.getLegacyCode())
   .name(entity.getName())
   .build();
 }

 public DistrictResponse toDistrictResponse(AdministrativeArea entity) {
  if (entity == null) return null;
  return DistrictResponse.builder()
   .districtId(entity.getLegacyCode())
   .name(entity.getName())
   .build();
 }

 public SpecialtyResponse toSpecialtyResponse(Specialty entity) {
  if (entity == null) return null;
  return SpecialtyResponse.builder()
   .specialtyId(entity.getSpecialtyId().toString())
   .name(entity.getName())
   .description(entity.getDescription())
   .category(entity.getCode())
   .build();
 }

 public HospitalResponse toHospitalResponse(CareFacility entity) {
  if (entity == null) return null;
  return HospitalResponse.builder()
   .hospitalId(entity.getFacilityId().toString())
   .name(entity.getName())
   .provinceId(entity.getProvinceId())
   .districtId(entity.getDistrictId())
   .address(entity.getAddress())
   .level(entity.getFacilityLevel())
   .type(entity.getFacilityType())
   .phone(entity.getPhone())
   .build();
 }
}
