package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MotherJourneyRepository extends JpaRepository<MotherJourney, UUID> {

    boolean existsByOwnerUserIdAndJourneyTypeAndStatus(UUID ownerUserId, JourneyType type, JourneyStatus status);

    long countByOwnerUserIdAndStatus(UUID ownerUserId, JourneyStatus status);
}
