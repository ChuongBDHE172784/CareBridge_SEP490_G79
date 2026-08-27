package com.carebridge.backend.expertavailability.repository;

import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ExpertLocationShareRepository extends JpaRepository<ExpertLocationShare, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpertLocationShare> findTopByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);

    @Modifying
    @Query("DELETE FROM ExpertLocationShare share WHERE share.expertProfileId = :expertProfileId")
    int deleteAllByExpertProfileId(@Param("expertProfileId") UUID expertProfileId);

    @Modifying
    @Query("DELETE FROM ExpertLocationShare share WHERE share.consentReference = :consentReference")
    int deleteAllByConsentReference(@Param("consentReference") UUID consentReference);
}
