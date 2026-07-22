package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.District;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, String> {
 List<District> findByProvinceIdOrderByName(String provinceId);
 List<District> findByProvinceIdAndIsActiveTrueOrderByName(String provinceId);
 boolean existsByProvinceIdAndDistrictId(String provinceId, String districtId);
}
