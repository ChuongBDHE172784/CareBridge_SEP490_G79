package com.carebridge.backend.triage;

import com.carebridge.backend.triage.service.TriageV2Metrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriageV2MetricsTest {
    @Test
    void recordsOnlyClosedOutcomesFailuresAndAggregates() {
        TriageV2Metrics metrics = new TriageV2Metrics();

        metrics.recordTurn("RED", 75, 0, true);
        metrics.recordTurn("attacker-private-label", 200_000, 99, false);
        metrics.recordFailure(TriageV2Metrics.Failure.HASH_MISMATCH);

        assertThat(metrics.turnCount()).isEqualTo(2);
        assertThat(metrics.outcomeCount("RED")).isEqualTo(1);
        assertThat(metrics.outcomeCount("NONE")).isEqualTo(1);
        assertThat(metrics.outcomeCount("attacker-private-label")).isZero();
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.HASH_MISMATCH)).isEqualTo(1);
        assertThat(metrics.targetConflictCount()).isEqualTo(1);
        assertThat(metrics.averageQuestionCount()).isEqualTo(1.5);
        assertThat(metrics.averageLatencyMs()).isEqualTo(30_037.5);
    }
}
