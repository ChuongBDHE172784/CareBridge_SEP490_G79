package com.carebridge.backend.masterdata.repository;

import com.carebridge.backend.masterdata.entity.Specialty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, String> {
 List<Specialty> findByIsActiveTrueOrderByName();
 List<Specialty> findByCategoryAndIsActiveTrueOrderByName(String category);
}
