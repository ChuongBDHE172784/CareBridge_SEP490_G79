package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.IntakeSession;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Performs the database-arbitrated insert for conversation idempotency keys. */
@Component
public class IntakeSessionWriter {

    private final JdbcTemplate jdbcTemplate;

    public IntakeSessionWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InsertResult insertConversationIfAbsent(IntakeSession candidate) {
        List<UUID> insertedIds = jdbcTemplate.query(
                """
                INSERT INTO triage_sessions (
                    triage_session_id, user_id, baby_profile_id, mother_profile_id, stage,
                    client_request_id, journey_id, origin_dashboard, origin_reference_id,
                    continuation_token, continuation_expires_at, symptoms, status,
                    created_at, created_by, disclaimer_version, content_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, client_request_id)
                    WHERE client_request_id IS NOT NULL
                    DO NOTHING
                RETURNING triage_session_id
                """,
                (rs, rowNum) -> rs.getObject("triage_session_id", UUID.class),
                candidate.getId(),
                candidate.getUserId(),
                candidate.getBabyProfileId(),
                candidate.getMotherProfileId(),
                candidate.getStage() == null ? null : candidate.getStage().name(),
                candidate.getClientRequestId(),
                candidate.getJourneyId(),
                candidate.getOriginDashboard() == null ? null : candidate.getOriginDashboard().name(),
                candidate.getOriginReferenceId(),
                candidate.getContinuationToken(),
                timestamp(candidate.getContinuationExpiresAt()),
                candidate.getSymptoms(),
                candidate.getStatus().name(),
                Timestamp.from(candidate.getCreatedAt()),
                candidate.getCreatedBy(),
                // CB-TRIAGE-CONSENT-IMP-001 (ADR-TDC-003): keep the DB-arbitrated insert path
                // consistent with the repository.save path — sessions carry the stamped version.
                candidate.getDisclaimerVersion(),
                candidate.getContentHash());
        return new InsertResult(!insertedIds.isEmpty());
    }

    private static Timestamp timestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    public record InsertResult(boolean created) {
    }
}
