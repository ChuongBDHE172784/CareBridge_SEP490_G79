package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Consultation message repository.
 */
@Repository
public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    /**
     * Find messages by session ID ordered by send time.
     *
     * @param sessionId the session ID
     * @return list of messages
     */
    List<ConsultationMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);

    /**
     * Find unread messages for a user.
     *
     * @param sessionId the session ID
     * @param userId the user ID
     * @return list of unread messages
     */
    @Query("SELECT m FROM ConsultationMessage m WHERE m.sessionId = :sessionId " +
            "AND m.senderUserId <> :userId AND m.readAt IS NULL ORDER BY m.sentAt ASC")
    List<ConsultationMessage> findUnreadBySessionIdAndUserId(@Param("sessionId") Long sessionId,
                                                             @Param("userId") Long userId);
}
