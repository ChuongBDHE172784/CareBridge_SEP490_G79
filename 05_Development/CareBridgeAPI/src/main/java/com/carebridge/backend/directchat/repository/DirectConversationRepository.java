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
     * Đóng những cuộc trò chuyện mà buổi tư vấn sinh ra chúng đã hết giờ.
     *
     * <p>Chat mở ra khi chuyên gia nhận yêu cầu, và trước đây không có gì đóng nó lại,
     * nên một khung giờ đã trôi qua từ lâu vẫn nhắn tiếp được. Mẹ đặt khung giờ nào thì
     * nói chuyện trong khung giờ đó; hết giờ, cuộc trò chuyện thành chỉ đọc.
     *
     * <p>Một câu lệnh thay vì vòng lặp, và chỉ chạm vào hàng còn ACTIVE nên chạy lại
     * bao nhiêu lần cũng không đổi thêm gì. Yêu cầu không chọn khung giờ (preferred
     * window để trống) không bị đóng — không có hạn thì không có gì để hết.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE direct_conversations c
               SET status = 'CLOSED'
             WHERE c.status = 'ACTIVE'
               AND EXISTS (
                   SELECT 1 FROM expert_consultation_requests r
                    WHERE r.direct_conversation_id = c.conversation_id
                      AND r.status = 'ACCEPTED'
                      AND r.preferred_window_end IS NOT NULL
                      AND r.preferred_window_end < CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int closeConversationsPastConsultationWindow();

}
