package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ProfessionalSpecialty;
import com.carebridge.backend.expert.entity.ProfessionalSpecialtyId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessionalSpecialtyRepository
        extends JpaRepository<ProfessionalSpecialty, ProfessionalSpecialtyId> {

    List<ProfessionalSpecialty> findByProfessionalProfileIdOrderByPrimaryDesc(UUID professionalProfileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProfessionalSpecialty ps "
            + "WHERE ps.professionalProfileId = :professionalProfileId")
    int deleteByProfessionalProfileId(@Param("professionalProfileId") UUID professionalProfileId);
}
