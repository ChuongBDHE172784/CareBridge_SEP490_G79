package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.AdministrativeArea;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdministrativeAreaRepository
        extends JpaRepository<AdministrativeArea, UUID> {

    List<AdministrativeArea> findByAreaTypeOrderByNameAsc(String areaType);

    Optional<AdministrativeArea> findByCode(String code);

    List<AdministrativeArea> findByAreaTypeAndParentAreaIdOrderByNameAsc(
            String areaType, UUID parentAreaId);

    @Query("""
        SELECT district FROM AdministrativeArea district
         WHERE district.areaType = 'DISTRICT'
           AND district.parentAreaId = (
               SELECT province.id FROM AdministrativeArea province
                WHERE province.areaType = 'PROVINCE'
                  AND province.legacyCode = :provinceCode
           )
         ORDER BY district.name
        """)
    List<AdministrativeArea> findDistrictsByProvinceCode(
            @Param("provinceCode") String provinceCode);
}
