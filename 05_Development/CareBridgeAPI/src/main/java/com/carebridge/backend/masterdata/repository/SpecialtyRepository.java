package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.Specialty;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
 List<Specialty> findByIsActiveTrueOrderByName();

 @Query(value = "SELECT * FROM specialties WHERE specialty_id::text = :identifier "
     + "OR code = :identifier LIMIT 1", nativeQuery = true)
 Optional<Specialty> findByIdentifier(@Param("identifier") String identifier);
}
