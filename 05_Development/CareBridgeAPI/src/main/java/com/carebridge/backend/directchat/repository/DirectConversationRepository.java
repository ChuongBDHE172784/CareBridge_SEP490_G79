package com.carebridge.backend.directchat.repository;

import com.carebridge.backend.directchat.entity.DirectConversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    Optional<DirectConversation> findByMotherUserIdAndExpertUserId(UUID motherUserId, UUID expertUserId);

    // called as findByMotherUserIdOrExpertUserId(currentUserId, currentUserId) — "my conversations"
    // regardless of which role the current user plays in each row.
    List<DirectConversation> findByMotherUserIdOrExpertUserId(UUID motherUserId, UUID expertUserId);

    // ADR-MEDI-002: same "my conversations" query, sorted for the inbox list (no ORDER BY before).
    List<DirectConversation> findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(
            UUID motherUserId, UUID expertUserId);

    // BR-DCC-014: touched from both message send and every call transition, not just message send.
    @Modifying
    @Transactional
    @Query("UPDATE DirectConversation c SET c.lastActivityAt = :timestamp WHERE c.id = :conversationId")
    void touchActivity(@Param("conversationId") UUID conversationId, @Param("timestamp") Instant timestamp);

}
