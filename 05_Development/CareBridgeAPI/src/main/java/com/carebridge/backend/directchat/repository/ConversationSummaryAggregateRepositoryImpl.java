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
                LEFT JOIN direct_messages dm
                       ON dm.conversation_id = dc.conversation_id
                      AND dm.sender_user_id <> :currentUserId
                      AND (dm.created_at, dm.message_id) > (
                            COALESCE(CASE WHEN dc.mother_user_id = :currentUserId THEN dc.mother_last_read_at
                                          ELSE dc.expert_last_read_at END, '-infinity'::timestamptz),
                            COALESCE(CASE WHEN dc.mother_user_id = :currentUserId THEN dc.mother_last_read_message_id
                                          ELSE dc.expert_last_read_message_id END,
                                     '00000000-0000-0000-0000-000000000000'::uuid))
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
    public ReadCursor advanceReadCursor(UUID conversationId, UUID currentUserId, boolean mother,
            java.time.Instant createdAt, UUID messageId) {
        String timeColumn = mother ? "mother_last_read_at" : "expert_last_read_at";
        String idColumn = mother ? "mother_last_read_message_id" : "expert_last_read_message_id";
        String userColumn = mother ? "mother_user_id" : "expert_user_id";
        String sql = """
                UPDATE direct_conversations
                SET %1$s = CASE WHEN (COALESCE(%1$s, '-infinity'::timestamptz),
                                           COALESCE(%2$s, '00000000-0000-0000-0000-000000000000'::uuid))
                                      < (?, ?)
                                   THEN ? ELSE %1$s END,
                    %2$s = CASE WHEN (COALESCE(%1$s, '-infinity'::timestamptz),
                                           COALESCE(%2$s, '00000000-0000-0000-0000-000000000000'::uuid))
                                      < (?, ?)
                                   THEN ? ELSE %2$s END
                WHERE conversation_id = ? AND %3$s = ?
                RETURNING %1$s, %2$s
                """.formatted(timeColumn, idColumn, userColumn);
        return jdbcTemplate.getJdbcTemplate().queryForObject(sql,
                (rs, rowNum) -> new ReadCursor(rs.getTimestamp(1).toInstant(), UUID.fromString(rs.getString(2))),
                java.sql.Timestamp.from(createdAt), messageId, java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(createdAt), messageId, messageId, conversationId, currentUserId);
    }
}
