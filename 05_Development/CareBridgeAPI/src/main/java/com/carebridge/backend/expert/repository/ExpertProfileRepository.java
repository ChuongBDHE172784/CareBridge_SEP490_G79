package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {

    Optional<ExpertProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<ExpertProfile> findByVerificationStatus(VerificationStatus status);

    @Query("SELECT ep FROM ExpertProfile ep WHERE ep.verificationStatus = 'APPROVED' ORDER BY ep.ratingAvg DESC NULLS LAST")
    List<ExpertProfile> findVerifiedPublic();

    @Query("SELECT ep FROM ExpertProfile ep " +
           "WHERE ep.verificationStatus = 'APPROVED' " +
           "AND (:specialty IS NULL OR ep.specialty = :specialty)")
    List<ExpertProfile> findVerifiedBySpecialty(@Param("specialty") String specialty);
}
