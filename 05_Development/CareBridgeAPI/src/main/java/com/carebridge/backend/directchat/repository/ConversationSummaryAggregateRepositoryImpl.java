package com.carebridge.backend.directchat.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ConversationSummaryAggregateRepositoryImpl implements ConversationSummaryAggregateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    ConversationSummaryAggregateRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<UUID, LastMessageRow> fetchLastMessages(List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, LastMessageRow> result = new HashMap<>();
        jdbcTemplate.query("""
                SELECT DISTINCT ON (conversation_id) conversation_id, message_id,
                       message_body, created_at
                FROM direct_messages
                WHERE conversation_id IN (:ids)
                ORDER BY conversation_id, created_at DESC, message_id DESC
                """,
                new MapSqlParameterSource("ids", conversationIds),
                rs -> {
                    result.put(
                            UUID.fromString(rs.getString("conversation_id")),
                            new LastMessageRow(UUID.fromString(rs.getString("message_id")), rs.getString("message_body"),
                                    rs.getTimestamp("created_at").toInstant()));
                });
        return result;
    }

    @Override
    public Map<UUID, Integer> fetchUnreadCounts(List<UUID> conversationIds, UUID currentUserId) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> result = new HashMap<>();
        for (UUID id : conversationIds) {
            result.put(id, 0);
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", conversationIds)
                .addValue("currentUserId", currentUserId);
        jdbcTemplate.query("""
                SELECT dc.conversation_id AS conversation_id, COUNT(dm.message_id) AS unread_count
                FROM direct_conversations dc
                LEFT JOIN direct_conversation_read_cursors rc
                       ON rc.conversation_id = dc.conversation_id
                      AND rc.reader_user_id = :currentUserId
                LEFT JOIN direct_messages dm
                       ON dm.conversation_id = dc.conversation_id
                      AND dm.sender_user_id <> :currentUserId
                      AND (dm.created_at, dm.message_id) > (COALESCE(rc.last_read_at, '-infinity'::timestamptz),
                            COALESCE(rc.last_read_message_id, '00000000-0000-0000-0000-000000000000'::uuid))
                WHERE dc.conversation_id IN (:ids)
                GROUP BY dc.conversation_id
                """,
                params,
                rs -> {
                    result.put(UUID.fromString(rs.getString("conversation_id")), rs.getInt("unread_count"));
                });
        return result;
    }

    @Override
    public ReadCursor advanceReadCursor(UUID conversationId, UUID currentUserId,
            java.time.Instant createdAt, UUID messageId) {
        String sql = """
                INSERT INTO direct_conversation_read_cursors (
                    conversation_id, reader_user_id, last_read_at, last_read_message_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, now(), now())
                ON CONFLICT (conversation_id, reader_user_id) DO UPDATE
                   SET last_read_at = CASE WHEN (direct_conversation_read_cursors.last_read_at,
                                                direct_conversation_read_cursors.last_read_message_id)
                                              < (EXCLUDED.last_read_at, EXCLUDED.last_read_message_id)
                                            THEN EXCLUDED.last_read_at
                                            ELSE direct_conversation_read_cursors.last_read_at END,
                       last_read_message_id = CASE WHEN (direct_conversation_read_cursors.last_read_at,
                                                        direct_conversation_read_cursors.last_read_message_id)
                                                      < (EXCLUDED.last_read_at, EXCLUDED.last_read_message_id)
                                                    THEN EXCLUDED.last_read_message_id
                                                    ELSE direct_conversation_read_cursors.last_read_message_id END,
                       updated_at = now()
                RETURNING last_read_at, last_read_message_id
                """;
        return jdbcTemplate.getJdbcTemplate().queryForObject(sql,
                (rs, rowNum) -> new ReadCursor(rs.getTimestamp(1).toInstant(), UUID.fromString(rs.getString(2))),
                conversationId, currentUserId, java.sql.Timestamp.from(createdAt), messageId);
    }
}
