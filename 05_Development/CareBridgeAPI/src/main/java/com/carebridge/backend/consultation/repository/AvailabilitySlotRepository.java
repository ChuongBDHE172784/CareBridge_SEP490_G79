package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Availability slot repository.
 * Provides slot management and conflict detection.
 */
@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    /**
     * Find available slots by expert ID.
     *
     * @param expertId the expert ID
     * @return list of available slots
     */
    List<AvailabilitySlot> findByExpertIdAndStatus(Long expertId, AvailabilitySlot.SlotStatus status);

    /**
     * Find slots by expert ID within a date range.
     *
     * @param expertId the expert ID
     * @param from start date
     * @param to end date
     * @return list of slots
     */
    List<AvailabilitySlot> findByExpertIdAndSlotStartBetween(Long expertId, Instant from, Instant to);

    /**
     * Check for slot conflicts.
     * Finds slots that overlap with the given time range.
     * ADR-EXP-002: Efficient conflict detection.
     *
     * @param expertId the expert ID
     * @param start the proposed start time
     * @param end the proposed end time
     * @return count of conflicting slots
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s WHERE s.expertId = :expertId " +
            "AND s.status IN ('AVAILABLE', 'BOOKED') " +
            "AND s.slotStart < :end AND s.slotEnd > :start")
    long countOverlappingSlots(@Param("expertId") Long expertId,
                               @Param("start") Instant start,
                               @Param("end") Instant end);

    /**
     * Find available slots in a time range.
     *
     * @param expertId the expert ID
     * @param start the start time
     * @param end the end time
     * @param status the slot status
     * @return list of available slots
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.expertId = :expertId " +
            "AND s.slotStart >= :start AND s.slotEnd <= :end " +
            "AND s.status = :status")
    List<AvailabilitySlot> findAvailableInRange(@Param("expertId") Long expertId,
                                                @Param("start") Instant start,
                                                @Param("end") Instant end,
                                                @Param("status") AvailabilitySlot.SlotStatus status);
}
