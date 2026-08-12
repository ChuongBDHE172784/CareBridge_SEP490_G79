package com.carebridge.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.JourneyTimelineService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JourneyTimelineServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void timelineGuardsPayloadUuidCastsAgainstMalformedLegacyValues() {
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UUID ownerId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        when(journeys.existsByIdAndOwnerUserId(journeyId, ownerId)).thenReturn(true);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        JourneyTimelineService service = new JourneyTimelineService(journeys, jdbcTemplate);

        assertThat(service.getTimeline(ownerId, journeyId, PageRequest.of(0, 20)).getItems())
                .isEmpty();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sql.getValue())
                .contains("CASE WHEN payload->>'triageSessionId' ~*")
                .contains("THEN CAST(payload->>'triageSessionId' AS uuid)")
                .contains("CASE WHEN payload->>'emergencySessionId' ~*")
                .contains("THEN CAST(payload->>'emergencySessionId' AS uuid)")
                .doesNotContain("CAST(payload->>'triageSessionId' AS uuid) AS source_intake_id")
                .doesNotContain("CAST(payload->>'emergencySessionId' AS uuid) AS source_emergency_id");
    }
}
