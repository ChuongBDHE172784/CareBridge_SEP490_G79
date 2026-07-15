package com.carebridge.backend.directchat.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * TDS §9.2: unified MESSAGE/CALL_EVENT timeline via native UNION ALL + Postgres
 * row-value comparison on (sort_ts, kind, resource_id). Not expressible as JPQL —
 * a plain JdbcTemplate component, not a Spring Data interface.
 */
@Repository
@RequiredArgsConstructor
public class ConversationTimelineRepository {

    private static final String UNION_SQL =
            "SELECT 'MESSAGE' AS kind, message_id AS resource_id, created_at AS sort_ts "
                    + "FROM direct_messages WHERE conversation_id = ? "
                    + "UNION ALL "
                    + "SELECT 'CALL_EVENT' AS kind, call_id AS resource_id, initiated_at AS sort_ts "
                    + "FROM conversation_calls WHERE conversation_id = ?";

    private final JdbcTemplate jdbcTemplate;

    /** after=cursor: strictly-newer items, ASC, for reconnect sync. */
    public List<TimelineRow> fetchAfter(UUID conversationId, Instant afterTs, String afterKind, UUID afterResourceId, int limit) {
        String sql = "SELECT * FROM (" + UNION_SQL + ") t "
                + "WHERE (sort_ts, kind, resource_id) > (?, ?, ?) "
                + "ORDER BY sort_ts ASC, kind ASC, resource_id ASC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER,
                conversationId, conversationId, Timestamp.from(afterTs), afterKind, afterResourceId, limit);
    }

    /** before=cursor: strictly-older items, DESC — caller must reverse before returning to client. */
    public List<TimelineRow> fetchBefore(UUID conversationId, Instant beforeTs, String beforeKind, UUID beforeResourceId, int limit) {
        String sql = "SELECT * FROM (" + UNION_SQL + ") t "
                + "WHERE (sort_ts, kind, resource_id) < (?, ?, ?) "
                + "ORDER BY sort_ts DESC, kind DESC, resource_id DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER,
                conversationId, conversationId, Timestamp.from(beforeTs), beforeKind, beforeResourceId, limit);
    }

    /** No cursor (reopen conversation): latest page, DESC — caller must reverse before returning to client. */
    public List<TimelineRow> fetchLatest(UUID conversationId, int limit) {
        String sql = "SELECT * FROM (" + UNION_SQL + ") t "
                + "ORDER BY sort_ts DESC, kind DESC, resource_id DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, conversationId, conversationId, limit);
    }

    private static final org.springframework.jdbc.core.RowMapper<TimelineRow> ROW_MAPPER = (rs, rowNum) -> new TimelineRow(
            rs.getString("kind"),
            UUID.fromString(rs.getString("resource_id")),
            rs.getTimestamp("sort_ts").toInstant());

    public record TimelineRow(String kind, UUID resourceId, Instant sortTs) {
    }
}
