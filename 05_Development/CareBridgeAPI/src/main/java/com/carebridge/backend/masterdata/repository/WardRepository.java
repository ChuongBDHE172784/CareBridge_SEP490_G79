package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByDistrictId(String districtId);
    List<Ward> findByDistrictIdAndIsActiveTrue(String districtId);
}