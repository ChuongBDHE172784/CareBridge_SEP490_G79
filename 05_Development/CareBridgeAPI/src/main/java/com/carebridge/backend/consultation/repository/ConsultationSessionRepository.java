package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Consultation session repository.
 */
@Repository
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, Long> {

    /**
     * Find session by booking ID.
     *
     * @param bookingId the booking ID
     * @return Optional of session
     */
    Optional<ConsultationSession> findByBookingId(Long bookingId);

    /**
     * Find session by session token.
     *
     * @param sessionToken the session token
     * @return Optional of session
     */
    Optional<ConsultationSession> findBySessionToken(String sessionToken);
}
