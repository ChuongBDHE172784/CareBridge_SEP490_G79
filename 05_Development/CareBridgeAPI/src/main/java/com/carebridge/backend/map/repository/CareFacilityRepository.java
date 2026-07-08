package com.carebridge.backend.map.repository;

import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CareFacilityRepository extends JpaRepository<CareFacility, UUID> {

    List<CareFacility> findByVerificationStatus(FacilityStatus status);

    @Query(value = "SELECT * FROM care_facilities " +
            "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
            "AND earth_distance(ll_to_earth(:lat, :lng), ll_to_earth(latitude::double precision, longitude::double precision)) <= :radiusMeters * 1000",
            nativeQuery = true)
    List<CareFacility> findNearby(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radiusMeters") double radiusMeters);
}
