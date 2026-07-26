package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.SecurityEvent;
import com.carebridge.backend.audit.entity.SecurityEventType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Compatibility repository over the canonical, immutable audit_events ledger.
 *
 * <p>The HTTP API historically exposed a Long id. We retain that contract by
 * persisting a stable publicEventId in payload for new rows and deriving one
 * from the canonical UUID for migrated rows.</p>
 */
@Repository
@RequiredArgsConstructor
public class SecurityEventRepository {

    private static final String SECURITY_CATEGORIES =
            "('LOGIN_FAILED','PERMISSION_DENIED','SUSPICIOUS_ACTIVITY','TOKEN_REVOKED','OTP_ATTEMPT_LIMIT_EXCEEDED')";
    private static final String EFFECTIVE_SELECT = """
            SELECT base.*,
                   coalesce(latest.status, base.status) AS effective_status,
                   coalesce(latest.reviewed_by, base.reviewed_by) AS effective_reviewed_by,
                   coalesce(latest.reviewed_at, base.reviewed_at) AS effective_reviewed_at
            """;
    private static final String EFFECTIVE_FROM = """
             FROM audit_events base
             LEFT JOIN LATERAL (
                 SELECT review.status, review.reviewed_by, review.reviewed_at
                   FROM audit_events review
                  WHERE review.event_category = 'SECURITY_EVENT_REVIEWED'
                    AND review.security_event_id = base.audit_event_id
                  ORDER BY review.occurred_at DESC, review.audit_event_id DESC
                  LIMIT 1
             ) latest ON true
            """;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SecurityEvent save(SecurityEvent event) {
        UUID canonicalId = event.getAuditEventId() == null ? UUID.randomUUID() : event.getAuditEventId();
        long publicId = event.getId() == null ? publicId(canonicalId) : event.getId();
        Optional<SecurityEvent> collision = findById(publicId);
        if (collision.isPresent() && !collision.get().getAuditEventId().equals(canonicalId)) {
            throw new IllegalStateException("Security event public id collision: " + publicId);
        }
        Instant occurredAt = event.getOccurredAt() == null ? Instant.now() : event.getOccurredAt();
        Map<String, Object> payload = readPayload(event.getPayload());
        payload.put("publicEventId", publicId);
        if (event.getDetails() != null) {
            payload.put("details", event.getDetails());
        }

        jdbcTemplate.update("""
                INSERT INTO audit_events
                    (audit_event_id, actor_user_id, event_category, resource_type,
                     payload, ip_address, user_agent, correlation_id, severity, status,
                     occurred_at, created_at, event_origin)
                VALUES (?, ?, ?, 'SECURITY_EVENT', ?::jsonb, ?, ?, ?, ?, ?, ?, ?, 'SECURITY_EVENT')
                """,
                canonicalId, event.getUserId(), event.getEventType().name(), writeJson(payload),
                event.getIpAddress(), event.getUserAgent(), event.getCorrelationId(),
                defaultText(event.getSeverity(), "MEDIUM"), defaultText(event.getStatus(), "OPEN"),
                Timestamp.from(occurredAt), Timestamp.from(occurredAt));

        event.setAuditEventId(canonicalId);
        event.setId(publicId);
        event.setOccurredAt(occurredAt);
        event.setPayload(writeJson(payload));
        return event;
    }

    public Page<SecurityEvent> search(
            UUID userId, SecurityEventType eventType, String severity, String status,
            String ipAddress, Instant from, Instant to, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE base.event_category IN " + SECURITY_CATEGORIES);
        List<Object> args = new ArrayList<>();
        add(where, args, "base.actor_user_id = ?", userId);
        add(where, args, "base.event_category = ?", eventType == null ? null : eventType.name());
        add(where, args, "base.severity = ?", severity);
        add(where, args, "coalesce(latest.status, base.status) = ?", status);
        add(where, args, "base.ip_address = ?", ipAddress);
        add(where, args, "base.occurred_at >= ?", from == null ? null : Timestamp.from(from));
        add(where, args, "base.occurred_at <= ?", to == null ? null : Timestamp.from(to));

        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*)" + EFFECTIVE_FROM + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<SecurityEvent> content = jdbcTemplate.query(
                EFFECTIVE_SELECT + EFFECTIVE_FROM + where
                        + " ORDER BY base.occurred_at DESC, base.audit_event_id DESC LIMIT ? OFFSET ?",
                this::mapEvent, pageArgs.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    public List<SecurityEvent> findByCorrelationIdOrderByOccurredAtAsc(UUID correlationId) {
        return jdbcTemplate.query(
                EFFECTIVE_SELECT + EFFECTIVE_FROM
                        + " WHERE base.event_category IN " + SECURITY_CATEGORIES
                        + " AND base.correlation_id = ?"
                        + " ORDER BY base.occurred_at ASC, base.audit_event_id ASC",
                this::mapEvent, correlationId);
    }

    public Optional<SecurityEvent> findById(Long id) {
        List<SecurityEvent> direct = jdbcTemplate.query(
                EFFECTIVE_SELECT + EFFECTIVE_FROM
                        + " WHERE base.event_category IN " + SECURITY_CATEGORIES
                        + " AND base.payload->>'publicEventId' = ?"
                        + " ORDER BY base.occurred_at DESC, base.audit_event_id DESC",
                this::mapEvent, id.toString());
        if (!direct.isEmpty()) {
            return unique(id, direct);
        }
        List<SecurityEvent> derived = jdbcTemplate.query(
                EFFECTIVE_SELECT + EFFECTIVE_FROM
                        + " WHERE base.event_category IN " + SECURITY_CATEGORIES,
                this::mapEvent).stream().filter(e -> e.getId().equals(id)).toList();
        return unique(id, derived);
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public int updateStatus(Long id, String status, UUID reviewedBy, Instant reviewedAt) {
        Optional<SecurityEvent> existing = findById(id);
        if (existing.isEmpty()) {
            return 0;
        }
        SecurityEvent base = existing.get();
        Map<String, Object> payload = new HashMap<>();
        payload.put("publicEventId", id);
        payload.put("status", status);
        jdbcTemplate.update("""
                INSERT INTO audit_events
                    (actor_user_id, event_category, resource_type, security_event_id,
                     payload, status, reviewed_by, reviewed_at, occurred_at, created_at, event_origin)
                VALUES (?, 'SECURITY_EVENT_REVIEWED', 'SECURITY_EVENT', ?, ?::jsonb,
                        ?, ?, ?, ?, ?, 'SECURITY_EVENT_REVIEW')
                """,
                reviewedBy, base.getAuditEventId(), writeJson(payload), status, reviewedBy,
                Timestamp.from(reviewedAt), Timestamp.from(reviewedAt), Timestamp.from(reviewedAt));
        return 1;
    }

    UUID canonicalId(Long publicEventId) {
        return findById(publicEventId).map(SecurityEvent::getAuditEventId).orElse(null);
    }

    private SecurityEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
        UUID canonicalId = rs.getObject("audit_event_id", UUID.class);
        Map<String, Object> payload = readPayload(rs.getString("payload"));
        Object persistedId = payload.get("publicEventId");
        long id = persistedId instanceof Number number
                ? number.longValue()
                : persistedId != null ? Long.parseLong(persistedId.toString()) : publicId(canonicalId);
        return SecurityEvent.builder()
                .id(id)
                .auditEventId(canonicalId)
                .occurredAt(toInstant(rs.getTimestamp("occurred_at")))
                .eventType(SecurityEventType.valueOf(rs.getString("event_category")))
                .userId(rs.getObject("actor_user_id", UUID.class))
                .ipAddress(rs.getString("ip_address"))
                .details(payload.get("details") == null ? null : payload.get("details").toString())
                .userAgent(rs.getString("user_agent"))
                .payload(rs.getString("payload"))
                .correlationId(rs.getObject("correlation_id", UUID.class))
                .severity(rs.getString("severity"))
                .status(rs.getString("effective_status"))
                .reviewedBy(rs.getObject("effective_reviewed_by", UUID.class))
                .reviewedAt(toInstant(rs.getTimestamp("effective_reviewed_at")))
                .build();
    }

    private Optional<SecurityEvent> unique(Long publicId, List<SecurityEvent> matches) {
        if (matches.size() > 1) {
            throw new IllegalStateException("Security event public id collision: " + publicId);
        }
        return matches.stream().findFirst();
    }

    private void add(StringBuilder where, List<Object> args, String predicate, Object value) {
        if (value != null && (!(value instanceof String s) || !s.isBlank())) {
            where.append(" AND ").append(predicate);
            args.add(value);
        }
    }

    private Map<String, Object> readPayload(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(json, MAP_TYPE));
        } catch (Exception ignored) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("legacyPayload", json);
            return fallback;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize security audit payload", exception);
        }
    }

    private static long publicId(UUID id) {
        return (id.getMostSignificantBits() ^ id.getLeastSignificantBits()) & Long.MAX_VALUE;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
