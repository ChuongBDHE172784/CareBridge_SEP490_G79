package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.triage.repository.TriageSessionEvidenceWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class TriageSessionEvidenceWriterTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void writesCitationAndLinkedClaimWithStableHashesAndIdempotentConflictSql() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UUID sourceId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class))).thenReturn(List.of(sourceId));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        TriageSessionEvidenceWriter writer =
                new TriageSessionEvidenceWriter(jdbcTemplate, new ObjectMapper());
        Map<String, Object> citation = Map.of(
                "sourceId", sourceId.toString(),
                "title", "MOH guidance",
                "excerpt", "Seek care now",
                "url", "https://moh.gov.vn/guide",
                "domain", "moh.gov.vn",
                "sourceVersion", "2026");
        Map<String, Object> claim = Map.of(
                "claimId", "CLAIM-1",
                "text", "Seek care now",
                "evidenceIds", List.of(sourceId.toString()));

        int inserted = writer.writeValidated(
                UUID.randomUUID(), List.of(citation), List.of(claim));

        assertThat(inserted).isEqualTo(2);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), arguments.capture());
        assertThat(sql.getAllValues()).allSatisfy(statement -> assertThat(statement)
                .contains("INSERT INTO triage_session_evidence")
                .contains("ON CONFLICT (triage_session_id, evidence_type, content_hash) DO NOTHING"));
        assertThat(arguments.getAllValues())
                .allSatisfy(values -> assertThat((String) values[10]).hasSize(64));
        assertThat(arguments.getAllValues().get(0)[5]).isEqualTo(sourceId);
        assertThat(arguments.getAllValues().get(1)[5]).isEqualTo(sourceId);
    }
}
