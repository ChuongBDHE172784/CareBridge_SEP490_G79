package com.carebridge.backend.directchat.repository;

import com.carebridge.backend.directchat.entity.ConversationCall;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConversationCallRepository extends JpaRepository<ConversationCall, UUID>, JpaSpecificationExecutor<ConversationCall> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConversationCall c WHERE c.id = :callId")
    Optional<ConversationCall> findByIdForUpdate(@Param("callId") UUID callId);

    // ADR-DCC-005: conditional UPDATE, never load-then-save — rowsAffected is the sole
    // race oracle between PATCH /answer and CallTimeoutReconciliationJob.
    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.ANSWERED, "
            + "c.answeredAt = :answeredAt WHERE c.id = :callId AND c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.RINGING")
    int conditionallyAnswer(@Param("callId") UUID callId, @Param("answeredAt") Instant answeredAt);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.MISSED, c.endedAt = :endedAt "
            + "WHERE c.id = :callId AND c.callStatus IN (com.carebridge.backend.directchat.entity.CallStatus.INITIATED, com.carebridge.backend.directchat.entity.CallStatus.RINGING)")
    int conditionallyMarkMissed(@Param("callId") UUID callId, @Param("endedAt") Instant endedAt);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.RINGING "
            + "WHERE c.id = :callId AND c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.INITIATED")
    int conditionallyMarkRinging(@Param("callId") UUID callId);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.DECLINED, c.endedAt = :endedAt "
            + "WHERE c.id = :callId AND c.callStatus IN (com.carebridge.backend.directchat.entity.CallStatus.INITIATED, "
            + "com.carebridge.backend.directchat.entity.CallStatus.RINGING)")
    int conditionallyDecline(@Param("callId") UUID callId, @Param("endedAt") Instant endedAt);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.CANCELLED, c.endedAt = :endedAt "
            + "WHERE c.id = :callId AND c.callStatus IN (com.carebridge.backend.directchat.entity.CallStatus.INITIATED, com.carebridge.backend.directchat.entity.CallStatus.RINGING)")
    int conditionallyCancel(@Param("callId") UUID callId, @Param("endedAt") Instant endedAt);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationCall c SET c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.ENDED, "
            + "c.endedAt = :endedAt, c.durationSeconds = :durationSeconds "
            + "WHERE c.id = :callId AND c.callStatus = com.carebridge.backend.directchat.entity.CallStatus.ANSWERED")
    int conditionallyEndAnswered(@Param("callId") UUID callId, @Param("endedAt") Instant endedAt,
            @Param("durationSeconds") int durationSeconds);

    @Query("SELECT c FROM ConversationCall c WHERE c.callStatus IN (com.carebridge.backend.directchat.entity.CallStatus.INITIATED, com.carebridge.backend.directchat.entity.CallStatus.RINGING) "
            + "AND c.initiatedAt < :cutoff")
    List<ConversationCall> findRingingCallsInitiatedBefore(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT c
            FROM ConversationCall c, DirectConversation d
            WHERE c.conversationId = d.id
              AND (:currentUserId = d.motherUserId OR :currentUserId = d.expertUserId)
              AND c.callStatus IN (
                  com.carebridge.backend.directchat.entity.CallStatus.INITIATED,
                  com.carebridge.backend.directchat.entity.CallStatus.RINGING,
                  com.carebridge.backend.directchat.entity.CallStatus.ANSWERED
              )
            ORDER BY c.initiatedAt DESC
            """)
    List<ConversationCall> findActiveForParticipant(@Param("currentUserId") UUID currentUserId);
}
