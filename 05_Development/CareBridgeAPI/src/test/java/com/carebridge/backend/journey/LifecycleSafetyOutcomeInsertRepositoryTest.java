package com.carebridge.backend.journey;

import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import com.carebridge.backend.journey.repository.LifecycleSafetyOutcomeInsertRepository;
import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LifecycleSafetyOutcomeInsertRepositoryTest {

    @Test
    void insertsCanonicalEventAndTreatsConflictAsIdempotentReplay() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1, 0);
        LifecycleSafetyOutcomeInsertRepository repository =
                new LifecycleSafetyOutcomeInsertRepository(jdbcTemplate);
        LifecycleSafetyOutcome outcome = outcome(null);

        UUID created = repository.insertIfAbsent(outcome);
        UUID replayed = repository.insertIfAbsent(outcome);

        assertThat(created).isEqualTo(outcome.getId());
        assertThat(replayed).isNull();
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), arguments.capture());
        assertThat(sql.getValue())
                .contains("INSERT INTO audit_events")
                .contains("'SAFETY_OUTCOME'")
                .contains("audit_event_id, event_category, actor_user_id, subject_user_id")
                .contains("subject_reference_id, resource_type, resource_id")
                .contains("'mother_journeys'")
                .contains("'intakeSessionId'")
                .contains("'triageSessionId'")
                .contains("'emergencySessionId'")
                .contains("'riskLevel'")
                .contains("'stage'")
                .contains("'originDashboard'")
                .contains("'originReferenceId'")
                .contains("'originAction'")
                .contains("ON CONFLICT (audit_event_id) DO NOTHING")
                .doesNotContain("lifecycle_safety_outcomes")
                .doesNotContain("mother_journey_events");
        assertThat(arguments.getAllValues().get(0)[7])
                .as("nullable emergencySessionId remains a scalar SQL null")
                .isNull();
    }

    private LifecycleSafetyOutcome outcome(UUID emergencySessionId) {
        UUID journeyId = UUID.randomUUID();
        return LifecycleSafetyOutcome.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .journeyId(journeyId)
                .intakeSessionId(UUID.randomUUID())
                .emergencySessionId(emergencySessionId)
                .riskLevel(RiskLevel.GREEN)
                .stage(TriageStage.PREGNANCY)
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(journeyId)
                .originAction(OriginAction.RETURN_TO_MOTHER_JOURNEY)
                .occurredAt(Instant.now())
                .recordedAt(Instant.now())
                .build();
    }
}
