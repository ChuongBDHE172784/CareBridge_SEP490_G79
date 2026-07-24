package com.carebridge.backend.masterdata.dto.response;

import com.carebridge.backend.masterdata.entity.District;
import com.carebridge.backend.masterdata.entity.Hospital;
import com.carebridge.backend.masterdata.entity.Province;
import com.carebridge.backend.masterdata.entity.Specialty;
import com.carebridge.backend.masterdata.entity.Ward;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {

 public ProvinceResponse toProvinceResponse(Province entity) {
  if (entity == null) return null;
  return ProvinceResponse.builder()
   .provinceId(entity.getProvinceId())
   .name(entity.getName())
   .nameEn(entity.getNameEn())
   .region(entity.getRegion())
   .build();
 }

 public DistrictResponse toDistrictResponse(District entity) {
  if (entity == null) return null;
  return DistrictResponse.builder()
   .districtId(entity.getDistrictId())
   .provinceId(entity.getProvinceId())
   .name(entity.getName())
   .nameEn(entity.getNameEn())
   .build();
 }

 public SpecialtyResponse toSpecialtyResponse(Specialty entity) {
  if (entity == null) return null;
  return SpecialtyResponse.builder()
   .specialtyId(entity.getSpecialtyId())
   .name(entity.getName())
   .description(entity.getDescription())
   .category(entity.getCategory())
   .build();
 }

 public HospitalResponse toHospitalResponse(Hospital entity) {
  if (entity == null) return null;
  return HospitalResponse.builder()
   .hospitalId(entity.getHospitalId())
   .name(entity.getName())
   .provinceId(entity.getProvinceId())
   .districtId(entity.getDistrictId())
   .address(entity.getAddress())
   .level(entity.getLevel())
   .type(entity.getType())
   .phone(entity.getPhone())
   .build();
 }

 public WardResponse toWardResponse(Ward entity) {
  if (entity == null) return null;
  return WardResponse.builder()
   .wardId(entity.getWardId())
   .districtId(entity.getDistrictId())
   .provinceId(entity.getProvinceId())
   .name(entity.getName())
   .nameEn(entity.getNameEn())
   .build();
 }
}
