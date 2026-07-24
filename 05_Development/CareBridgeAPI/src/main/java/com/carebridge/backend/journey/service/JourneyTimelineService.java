package com.carebridge.backend.journey.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.JourneyTimelineItemResponse;
import com.carebridge.backend.journey.dto.JourneyTimelinePageResponse;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JourneyTimelineService implements IJourneyTimelineService {
    private final MotherJourneyRepository journeyRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public JourneyTimelinePageResponse getTimeline(
            UUID ownerUserId, UUID journeyId, Pageable pageable) {
        if (!journeyRepository.existsByIdAndOwnerUserId(journeyId, ownerUserId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY-002", "Journey not found");
        }
        List<JourneyTimelineItemResponse> items = jdbcTemplate.query("""
                WITH timeline AS (
                    SELECT CASE WHEN event_type = 'SAFETY_OUTCOME'
                                THEN 'SAFETY_OUTCOME' ELSE 'LIFECYCLE_TRANSITION' END AS item_type,
                           event_id AS item_id, effective_at AS occurred_at, recorded_at,
                           event_type::text, from_stage::text, to_stage::text,
                           risk_level,
                           stage,
                           triage_session_id AS source_intake_id,
                           emergency_session_id AS source_emergency_id,
                           origin_action
                    FROM mother_journey_events
                    WHERE mother_journey_id = ?
                      AND (legacy_source = 'JOURNEY_TRANSITION' OR event_type = 'SAFETY_OUTCOME')
                )
                SELECT * FROM timeline
                ORDER BY occurred_at DESC, recorded_at DESC, item_id DESC
                LIMIT ? OFFSET ?
                """, (rs, row) -> mapItem(rs), journeyId,
                pageable.getPageSize(), pageable.getOffset());
        Long total = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM mother_journey_events
                WHERE mother_journey_id = ?
                  AND (legacy_source = 'JOURNEY_TRANSITION' OR event_type = 'SAFETY_OUTCOME')
                """, Long.class, journeyId);
        long count = total == null ? 0 : total;
        int totalPages = count == 0 ? 0
                : (int) ((count + pageable.getPageSize() - 1) / pageable.getPageSize());
        return JourneyTimelinePageResponse.builder()
                .items(items).page(pageable.getPageNumber()).size(pageable.getPageSize())
                .totalElements(count).totalPages(totalPages).build();
    }

    private JourneyTimelineItemResponse mapItem(ResultSet rs) throws SQLException {
        return JourneyTimelineItemResponse.builder()
                .itemType(rs.getString("item_type"))
                .itemId(rs.getObject("item_id", UUID.class))
                .occurredAt(rs.getTimestamp("occurred_at").toInstant())
                .recordedAt(rs.getTimestamp("recorded_at").toInstant())
                .eventType(rs.getString("event_type"))
                .fromStage(rs.getString("from_stage"))
                .toStage(rs.getString("to_stage"))
                .riskLevel(rs.getString("risk_level"))
                .stage(rs.getString("stage"))
                .sourceIntakeId(rs.getObject("source_intake_id", UUID.class))
                .sourceEmergencyId(rs.getObject("source_emergency_id", UUID.class))
                .originAction(rs.getString("origin_action"))
                .build();
    }
}
