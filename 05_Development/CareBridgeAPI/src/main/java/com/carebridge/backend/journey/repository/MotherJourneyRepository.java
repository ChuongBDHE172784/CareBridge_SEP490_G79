package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotherJourneyRepository extends JpaRepository<MotherJourney, UUID> {

    boolean existsByOwnerUserIdAndJourneyTypeAndStatus(UUID ownerUserId, JourneyType type, JourneyStatus status);

    boolean existsByOwnerUserIdAndStatusAndJourneyTypeIn(
            UUID ownerUserId, JourneyStatus status, List<JourneyType> journeyTypes);

    Optional<MotherJourney> findByOwnerUserIdAndStatusAndJourneyTypeIn(
            UUID ownerUserId, JourneyStatus status, List<JourneyType> journeyTypes);

    Optional<MotherJourney> findFirstByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtDesc(
            UUID ownerUserId, JourneyType journeyType, JourneyStatus status);

    List<MotherJourney> findByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtAsc(
        UUID ownerUserId, JourneyType type, JourneyStatus status);

    long countByOwnerUserIdAndStatus(UUID ownerUserId, JourneyStatus status);

    boolean existsByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    /** Returns the most recently created ACTIVE journey for the user (LIMIT 1). */
    Optional<MotherJourney> findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(UUID ownerUserId,
                                                                                JourneyStatus status);
}
