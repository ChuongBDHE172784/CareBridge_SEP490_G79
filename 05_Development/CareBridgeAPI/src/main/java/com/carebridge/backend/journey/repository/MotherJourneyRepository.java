package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotherJourneyRepository extends JpaRepository<MotherJourney, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO care_subjects (
                care_subject_id, person_id, owner_user_id, subject_type,
                nickname, status, created_at, updated_at)
            SELECT :subjectId, u.user_id, u.user_id, 'MOTHER',
                   u.display_name, 'ACTIVE', now(), now()
              FROM users u
             WHERE u.user_id = :ownerUserId
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int ensureMotherCareSubject(
            @Param("subjectId") UUID subjectId,
            @Param("ownerUserId") UUID ownerUserId);

    @Query(value = """
            SELECT care_subject_id FROM care_subjects
             WHERE owner_user_id = :ownerUserId AND subject_type = 'MOTHER'
               AND mother_journey_id IS NULL
             ORDER BY created_at DESC
             LIMIT 1
            """, nativeQuery = true)
    UUID findMotherCareSubjectId(@Param("ownerUserId") UUID ownerUserId);

    @Modifying
    @Query(value = """
            UPDATE care_subjects SET mother_journey_id = :journeyId, updated_at = now()
             WHERE care_subject_id = :subjectId AND mother_journey_id IS NULL
            """, nativeQuery = true)
    int linkMotherCareSubject(
            @Param("subjectId") UUID subjectId,
            @Param("journeyId") UUID journeyId);

    /**
     * Legacy compatibility helper for old postpartum records. New maternal metrics
     * must use the journey's persisted careSubjectId directly.
     */
    @Modifying
    @Query(value = """
            INSERT INTO care_subjects (
                care_subject_id, person_id, owner_user_id, mother_journey_id,
                subject_type, nickname, status, created_at, updated_at)
            SELECT j.journey_id, u.user_id, u.user_id, j.journey_id, 'MOTHER',
                   u.display_name, 'ACTIVE', now(), now()
              FROM mother_journeys j
              JOIN users u ON u.user_id = j.owner_user_id
             WHERE j.journey_id = :journeyId
            ON CONFLICT (care_subject_id) DO NOTHING
            """, nativeQuery = true)
    int ensureJourneyObservationSubject(@Param("journeyId") UUID journeyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from MotherJourney j where j.id = :journeyId")
    Optional<MotherJourney> findByIdForUpdate(@Param("journeyId") UUID journeyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select journey from MotherJourney journey
             where journey.id = :journeyId
               and journey.ownerUserId = :ownerUserId
            """)
    Optional<MotherJourney> findByIdAndOwnerUserIdForUpdate(
            @Param("journeyId") UUID journeyId,
            @Param("ownerUserId") UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from MotherJourney j where j.ownerUserId=:owner and j.status=com.carebridge.backend.journey.entity.JourneyStatus.ACTIVE and j.journeyType in (com.carebridge.backend.journey.entity.JourneyType.PRE_PREGNANCY, com.carebridge.backend.journey.entity.JourneyType.PREGNANCY, com.carebridge.backend.journey.entity.JourneyType.POSTPARTUM)")
    Optional<MotherJourney> findCanonicalForUpdate(@Param("owner") UUID owner);

    @Query("select j from MotherJourney j where j.ownerUserId=:owner and j.status=com.carebridge.backend.journey.entity.JourneyStatus.ACTIVE and j.journeyType in (com.carebridge.backend.journey.entity.JourneyType.PRE_PREGNANCY, com.carebridge.backend.journey.entity.JourneyType.PREGNANCY, com.carebridge.backend.journey.entity.JourneyType.POSTPARTUM)")
    Optional<MotherJourney> findCanonical(@Param("owner") UUID owner);

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

    List<MotherJourney> findByStatus(JourneyStatus status);

    boolean existsByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    boolean existsByIdAndOwnerUserIdAndStatus(UUID id, UUID ownerUserId, JourneyStatus status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<MotherJourney> findByIdAndOwnerUserIdAndStatus(
            UUID id, UUID ownerUserId, JourneyStatus status);

    /** Returns the most recently created ACTIVE journey for the user (LIMIT 1). */
    Optional<MotherJourney> findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(UUID ownerUserId,
                                                                                JourneyStatus status);
}
