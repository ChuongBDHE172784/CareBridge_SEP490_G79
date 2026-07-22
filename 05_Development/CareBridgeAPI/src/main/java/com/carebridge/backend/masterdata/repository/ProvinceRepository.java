package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.Province;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProvinceRepository extends JpaRepository<Province, String> {
 Optional<Province> findByProvinceId(String provinceId);
 List<Province> findByIsActiveTrueOrderByName();

 @Query("SELECT p FROM Province p WHERE p.isActive = true AND p.region = :region ORDER BY p.name")
 List<Province> findByRegionOrderByName(String region);
}
