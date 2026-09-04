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

    /**
     * Buổi tư vấn của cuộc trò chuyện này đã hết giờ chưa.
     *
     * <p>Chat mở ra khi chuyên gia nhận yêu cầu và trước đây không có gì đóng lại, nên
     * một khung giờ trôi qua từ lâu vẫn nhắn tiếp được. Mẹ đặt khung giờ nào thì nói
     * chuyện trong khung giờ đó.
     *
     * <p>Suy ra lúc đọc chứ không lưu vào cột status: cột đó mang
     * {@code CHECK (status = 'ACTIVE')} nên không nhận được giá trị nào khác, và nới
     * constraint là đổi schema. Suy ra cũng không có độ trễ như quét định kỳ.
     *
     * <p>Yêu cầu không chọn khung giờ thì không bao giờ hết giờ — không có hạn thì
     * không có gì để hết.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM expert_consultation_requests r
                 WHERE r.direct_conversation_id = :conversationId
                   AND r.status = 'ACCEPTED'
                   AND r.preferred_window_end IS NOT NULL
                   AND r.preferred_window_end < CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    boolean isConsultationWindowEnded(@Param("conversationId") UUID conversationId);

    /** Cùng câu hỏi cho cả danh sách hội thoại, một truy vấn thay vì hỏi từng cái. */
    @Query(value = """
            SELECT DISTINCT r.direct_conversation_id FROM expert_consultation_requests r
             WHERE r.direct_conversation_id IN (:conversationIds)
               AND r.status = 'ACCEPTED'
               AND r.preferred_window_end IS NOT NULL
               AND r.preferred_window_end < CURRENT_TIMESTAMP
            """, nativeQuery = true)
    List<UUID> findIdsWithEndedConsultationWindow(
            @Param("conversationIds") java.util.Collection<UUID> conversationIds);

}
