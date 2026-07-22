package com.carebridge.backend.map.repository;

import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareFacilityRepository extends JpaRepository<CareFacility, UUID> {

    List<CareFacility> findByVerificationStatus(FacilityStatus status);

    List<CareFacility> findByActiveTrueOrderByNameAsc();

    Optional<CareFacility> findByFacilityIdAndActiveTrue(UUID facilityId);

    @Query(value = "SELECT * FROM care_facilities " +
            "WHERE is_active = true AND is_searchable = true AND verification_status = 'VERIFIED' " +
            "AND latitude IS NOT NULL AND longitude IS NOT NULL " +
            "AND (:type IS NULL OR upper(facility_type) = upper(:type)) " +
            "AND (:provinceId IS NULL OR province_id = :provinceId) " +
            "AND (:districtId IS NULL OR district_id = :districtId) " +
            "AND earth_distance(ll_to_earth(:lat, :lng), ll_to_earth(latitude::double precision, longitude::double precision)) <= :radiusMeters " +
            "ORDER BY earth_distance(ll_to_earth(:lat, :lng), ll_to_earth(latitude::double precision, longitude::double precision))",
            nativeQuery = true)
    List<CareFacility> findNearby(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("type") String type,
            @Param("provinceId") String provinceId,
            @Param("districtId") String districtId);
}
