package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Consultation repository.
 * Provides booking management and lookup.
 */
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    /**
     * Find consultation by booking reference.
     *
     * @param bookingRef the booking reference
     * @return Optional of consultation
     */
    Optional<Consultation> findByBookingRef(String bookingRef);

    /**
     * Find consultations by expert ID.
     *
     * @param expertId the expert ID
     * @return list of consultations
     */
    List<Consultation> findByExpertId(Long expertId);

    /**
     * Find consultations by requester user ID.
     *
     * @param requesterUserId the requester user ID
     * @return list of consultations
     */
    List<Consultation> findByRequesterUserId(Long requesterUserId);

    /**
     * Find consultations by status.
     *
     * @param status the consultation status
     * @return list of consultations
     */
    List<Consultation> findByStatus(Consultation.ConsultationStatus status);

    /**
     * Find upcoming consultations (scheduled in the future).
     *
     * @param now current time
     * @return list of upcoming consultations
     */
    @Query("SELECT c FROM Consultation c WHERE c.scheduledStart > :now ORDER BY c.scheduledStart ASC")
    List<Consultation> findUpcoming(@Param("now") Instant now);

    /**
     * Check for double booking conflict.
     * Returns true if expert has another booking at the given time.
     *
     * @param expertId the expert ID
     * @param start the start time
     * @param end the end time
     * @param excludeBookingId booking to exclude (for rescheduling)
     * @return true if conflict exists
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Consultation c WHERE c.expertId = :expertId " +
            "AND c.status IN ('CONFIRMED', 'IN_PROGRESS') " +
            "AND c.scheduledStart < :end AND c.scheduledEnd > :start " +
            "AND (:excludeBookingId IS NULL OR c.bookingId <> :excludeBookingId)")
    boolean existsConflict(@Param("expertId") Long expertId,
                           @Param("start") Instant start,
                           @Param("end") Instant end,
                           @Param("excludeBookingId") Long excludeBookingId);

    /**
     * Find consultations by status in date range.
     *
     * @param status the status filter
     * @param from start date
     * @param to end date
     * @return list of consultations
     */
    @Query("SELECT c FROM Consultation c WHERE c.status = :status AND c.scheduledStart BETWEEN :from AND :to")
    List<Consultation> findByStatusAndScheduledStartBetween(@Param("status") Consultation.ConsultationStatus status,
                                                            @Param("from") Instant from,
                                                            @Param("to") Instant to);
}
