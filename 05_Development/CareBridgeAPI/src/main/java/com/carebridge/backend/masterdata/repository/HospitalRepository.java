package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.Hospital;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HospitalRepository extends JpaRepository<Hospital, String> {
 List<Hospital> findByProvinceIdAndIsActiveTrueOrderByName(String provinceId);
 List<Hospital> findByProvinceIdAndDistrictIdAndIsActiveTrueOrderByName(String provinceId, String districtId);
 List<Hospital> findByIsActiveTrueOrderByName();

 @Query("SELECT h FROM Hospital h WHERE h.isActive = true AND h.provinceId = :provinceId AND LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY h.name")
 List<Hospital> searchInProvince(String provinceId, String query);
}
