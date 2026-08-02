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

    /** Conversations delegated to an accepted Family member of the Mother's active care group. */
    @Query(value = """
            SELECT dc.*
              FROM direct_conversations dc
             WHERE dc.mother_user_id <> :userId
               AND dc.expert_user_id <> :userId
               AND EXISTS (
                    SELECT 1
                      FROM care_groups cg
                      JOIN care_group_members cgm ON cgm.care_group_id = cg.care_group_id
                     WHERE cg.owner_user_id = dc.mother_user_id
                       AND cg.status = 'ACTIVE'
                       AND cgm.user_id = :userId
                       AND cgm.invitation_status = 'ACCEPTED'
               )
             ORDER BY dc.last_activity_at DESC NULLS LAST
            """, nativeQuery = true)
    List<DirectConversation> findDelegatedToFamilyUserOrderByLastActivityAtDesc(@Param("userId") UUID userId);

    // BR-DCC-014: touched from both message send and every call transition, not just message send.
    @Modifying
    @Transactional
    @Query("UPDATE DirectConversation c SET c.lastActivityAt = :timestamp WHERE c.id = :conversationId")
    void touchActivity(@Param("conversationId") UUID conversationId, @Param("timestamp") Instant timestamp);

    // ADR-MEDI-003 mục 3-5 — native: GREATEST()/'-infinity'::timestamptz are Postgres-specific,
    // not reliably portable through HQL. cursorAt MUST be resolvedMessage.getCreatedAt() from the
    // caller — never Instant.now() (see TDS §6.4/§6.5, constraint C2).
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE direct_conversations
            SET mother_last_read_at = GREATEST(COALESCE(mother_last_read_at, '-infinity'::timestamptz), :cursorAt)
            WHERE conversation_id = :id AND mother_user_id = :currentUserId
            """, nativeQuery = true)
    int markMotherRead(@Param("id") UUID id, @Param("currentUserId") UUID currentUserId, @Param("cursorAt") Instant cursorAt);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE direct_conversations
            SET expert_last_read_at = GREATEST(COALESCE(expert_last_read_at, '-infinity'::timestamptz), :cursorAt)
            WHERE conversation_id = :id AND expert_user_id = :currentUserId
            """, nativeQuery = true)
    int markExpertRead(@Param("id") UUID id, @Param("currentUserId") UUID currentUserId, @Param("cursorAt") Instant cursorAt);
}
