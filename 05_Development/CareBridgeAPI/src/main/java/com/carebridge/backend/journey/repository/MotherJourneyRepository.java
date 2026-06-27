package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MotherJourneyRepository extends JpaRepository<MotherJourney, UUID> {

    boolean existsByOwnerUserIdAndJourneyTypeAndStatus(UUID ownerUserId, JourneyType type, JourneyStatus status);

    long countByOwnerUserIdAndStatus(UUID ownerUserId, JourneyStatus status);

    /** Returns the most recently created ACTIVE journey for the user (LIMIT 1) */
    @Query("SELECT j FROM MotherJourney j WHERE j.ownerUserId = :ownerUserId AND j.status = :status ORDER BY j.createdAt DESC")
    Optional<MotherJourney> findByOwnerUserIdAndStatus(@Param("ownerUserId") UUID ownerUserId,
                                                        @Param("status") JourneyStatus status);
}
