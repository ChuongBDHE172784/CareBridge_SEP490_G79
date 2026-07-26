package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.SecurityEventNote;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SecurityEventNoteRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityEventRepository eventRepository;

    public SecurityEventNote save(SecurityEventNote note) {
        UUID baseId = eventRepository.canonicalId(note.getEventId());
        if (baseId == null) {
            throw new IllegalArgumentException("Unknown security event: " + note.getEventId());
        }
        UUID noteId = note.getNoteId() == null ? UUID.randomUUID() : note.getNoteId();
        Instant createdAt = note.getCreatedAt() == null ? Instant.now() : note.getCreatedAt();
        jdbcTemplate.update("""
                INSERT INTO audit_events
                    (audit_event_id, actor_user_id, event_category, resource_type, purpose,
                     security_event_id, note_text, occurred_at, created_at, event_origin)
                VALUES (?, ?, 'SECURITY_INVESTIGATION_NOTE', 'SECURITY_EVENT',
                        'SECURITY_INVESTIGATION', ?, ?, ?, ?, 'SECURITY_INVESTIGATION_NOTE')
                """, noteId, note.getAuthorId(), baseId, note.getNoteText(),
                Timestamp.from(createdAt), Timestamp.from(createdAt));
        note.setNoteId(noteId);
        note.setSecurityEventId(baseId);
        note.setCreatedAt(createdAt);
        return note;
    }

    public List<SecurityEventNote> findByEventIdOrderByCreatedAtAsc(Long eventId) {
        UUID baseId = eventRepository.canonicalId(eventId);
        if (baseId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT audit_event_id, actor_user_id, security_event_id, note_text, occurred_at
                  FROM audit_events
                 WHERE event_category = 'SECURITY_INVESTIGATION_NOTE'
                   AND security_event_id = ?
                 ORDER BY occurred_at ASC
                """, (rs, rowNum) -> SecurityEventNote.builder()
                .noteId(rs.getObject("audit_event_id", UUID.class))
                .eventId(eventId)
                .securityEventId(baseId)
                .authorId(rs.getObject("actor_user_id", UUID.class))
                .noteText(rs.getString("note_text"))
                .createdAt(rs.getTimestamp("occurred_at").toInstant())
                .build(), baseId);
    }
}
