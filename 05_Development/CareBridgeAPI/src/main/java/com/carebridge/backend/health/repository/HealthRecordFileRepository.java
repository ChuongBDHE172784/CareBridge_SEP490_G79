package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthRecordFile;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HealthRecordFileRepository {
    private final JdbcTemplate jdbcTemplate;

    public HealthRecordFile save(HealthRecordFile link) {
        int changed = jdbcTemplate.update(
                """
                UPDATE attachments
                   SET health_record_id=?, updated_at=now()
                 WHERE attachment_id=?
                   AND (health_record_id IS NULL OR health_record_id=?)
                """,
                link.getHealthRecordId(), link.getFileId(), link.getHealthRecordId());
        if (changed != 1) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM attachments WHERE attachment_id=?)",
                    Boolean.class, link.getFileId());
            if (Boolean.TRUE.equals(exists)) {
                throw new IllegalStateException("Attachment already belongs to another health record");
            }
            throw new IllegalArgumentException("Attachment not found: " + link.getFileId());
        }
        link.setId(link.getFileId());
        return link;
    }

    public List<HealthRecordFile> findByHealthRecordIdOrderByDisplayOrderAsc(UUID healthRecordId) {
        return jdbcTemplate.query("""
                SELECT attachment_id, health_record_id, created_at,
                       row_number() over (order by created_at, attachment_id) - 1 AS display_order
                  FROM attachments
                 WHERE health_record_id=? AND status='ACTIVE'
                 ORDER BY created_at, attachment_id
                """, (rs, rowNum) -> HealthRecordFile.builder()
                .id(rs.getObject("attachment_id", UUID.class))
                .fileId(rs.getObject("attachment_id", UUID.class))
                .healthRecordId(rs.getObject("health_record_id", UUID.class))
                .displayOrder(rs.getInt("display_order"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build(), healthRecordId);
    }

    public List<HealthRecordFile> findByFileId(UUID fileId) {
        return jdbcTemplate.query("""
                SELECT attachment_id, health_record_id, created_at
                  FROM attachments WHERE attachment_id=? AND health_record_id IS NOT NULL
                """, (rs, rowNum) -> HealthRecordFile.builder()
                .id(fileId).fileId(fileId)
                .healthRecordId(rs.getObject("health_record_id", UUID.class))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build(), fileId);
    }
}
