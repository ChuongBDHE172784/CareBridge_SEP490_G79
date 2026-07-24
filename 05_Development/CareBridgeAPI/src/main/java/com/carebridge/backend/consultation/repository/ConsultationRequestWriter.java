package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultationRequestWriter {

    private final JdbcTemplate jdbcTemplate;

    public ConsultationRequestWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InsertResult insertIfAbsent(ConsultationRequest candidate) {
        List<UUID> insertedIds = jdbcTemplate.query(
                """
                INSERT INTO expert_consultation_requests (
                    id, requester_user_id, expert_profile_id, client_request_id,
                    topic, description, preferred_window_start, preferred_window_end,
                    status, reject_reason, direct_conversation_id, responded_at,
                    responded_by, expires_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (requester_user_id, client_request_id) DO NOTHING
                RETURNING id
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                candidate.getId(),
                candidate.getRequesterUserId(),
                candidate.getExpertProfileId(),
                candidate.getClientRequestId(),
                candidate.getTopic(),
                candidate.getDescription(),
                timestamp(candidate.getPreferredWindowStart()),
                timestamp(candidate.getPreferredWindowEnd()),
                candidate.getStatus().name(),
                candidate.getRejectReason(),
                candidate.getDirectConversationId(),
                timestamp(candidate.getRespondedAt()),
                candidate.getRespondedBy(),
                Timestamp.from(candidate.getExpiresAt()),
                Timestamp.from(candidate.getCreatedAt()),
                Timestamp.from(candidate.getUpdatedAt()));
        return insertedIds.isEmpty()
                ? new InsertResult(candidate.getId(), false)
                : new InsertResult(insertedIds.getFirst(), true);
    }

    private static Timestamp timestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    public record InsertResult(UUID requestId, boolean created) {
    }
}
