package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Consultation dispute repository.
 */
@Repository
public interface ConsultationDisputeRepository extends JpaRepository<ConsultationDispute, Long> {

    /**
     * Find dispute by booking ID.
     *
     * @param bookingId the booking ID
     * @return Optional of dispute
     */
    ConsultationDispute findByBookingId(Long bookingId);

    /**
     * Find disputes by submitter.
     *
     * @param submittedBy the user who submitted
     * @return list of disputes
     */
    List<ConsultationDispute> findBySubmittedBy(Long submittedBy);

    /**
     * Find disputes by status.
     *
     * @param status the dispute status
     * @return list of disputes
     */
    List<ConsultationDispute> findByStatus(ConsultationDispute.DisputeStatus status);
}
