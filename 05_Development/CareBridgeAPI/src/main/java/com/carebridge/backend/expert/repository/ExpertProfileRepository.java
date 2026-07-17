package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {

 Optional<ExpertProfile> findByUserId(UUID userId);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.expertProfileId = :id")
 Optional<ExpertProfile> findByIdForUpdate(@Param("id") UUID id);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.userId = :userId")
 Optional<ExpertProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

 boolean existsByUserId(UUID userId);

 List<ExpertProfile> findByVerificationStatus(VerificationStatus status);

 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.verificationStatus = 'APPROVED' "
     + "AND ep.trustStatus = com.carebridge.backend.expert.truststatus.TrustStatus.ACTIVE "
     + "ORDER BY ep.ratingAvg DESC NULLS LAST")
 List<ExpertProfile> findVerifiedPublic();

 @Query("SELECT ep FROM ExpertProfile ep " +
 "WHERE ep.verificationStatus = 'APPROVED' " +
 "AND ep.trustStatus = com.carebridge.backend.expert.truststatus.TrustStatus.ACTIVE " +
 "AND (:specialty IS NULL OR ep.specialty = :specialty)")
 List<ExpertProfile> findVerifiedBySpecialty(@Param("specialty") String specialty);

 List<ExpertProfile> findByUserIdIn(Set<UUID> userIds);

 // ADR-MEDI-001 mục 1-2 — real pagination (Pageable actually applied, unlike
 // findVerifiedPublic/findVerifiedBySpecialty above) + optional case-insensitive text search
 // across users.full_name / expert_profiles.professional_title / expert_profiles.workplace.
 @Query(value = """
     SELECT ep.* FROM expert_profiles ep JOIN users u ON u.user_id = ep.user_id
     WHERE ep.verification_status = 'APPROVED'
       AND ep.trust_status = 'ACTIVE'
       AND (:specialty IS NULL OR ep.specialty = :specialty)
       AND (:q IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(ep.professional_title) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(ep.workplace) LIKE LOWER(CONCAT('%', :q, '%')))
     ORDER BY ep.rating_avg DESC NULLS LAST, ep.expert_profile_id ASC
     """,
     countQuery = """
     SELECT COUNT(*) FROM expert_profiles ep JOIN users u ON u.user_id = ep.user_id
     WHERE ep.verification_status = 'APPROVED'
       AND ep.trust_status = 'ACTIVE'
       AND (:specialty IS NULL OR ep.specialty = :specialty)
       AND (:q IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(ep.professional_title) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(ep.workplace) LIKE LOWER(CONCAT('%', :q, '%')))
     """,
     nativeQuery = true)
 Page<ExpertProfile> searchDirectory(@Param("specialty") String specialty, @Param("q") String q, Pageable pageable);

 @Query("SELECT DISTINCT TRIM(ep.specialty) FROM ExpertProfile ep "
     + "WHERE ep.verificationStatus = 'APPROVED' "
     + "AND ep.trustStatus = com.carebridge.backend.expert.truststatus.TrustStatus.ACTIVE "
     + "AND ep.specialty IS NOT NULL "
     + "AND TRIM(ep.specialty) <> '' ORDER BY TRIM(ep.specialty)")
 List<String> findApprovedSpecialties();
}
