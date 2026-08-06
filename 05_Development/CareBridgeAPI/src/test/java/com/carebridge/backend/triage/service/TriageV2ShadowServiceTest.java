package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.rules.TriageRuleRegistry;
import com.carebridge.backend.triage.rules.TriageV2ReadinessService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageV2ShadowServiceTest {
    private static final String HASH = "a".repeat(64);
    private TriageV2WorkflowClient workflow;
    private TriageV2ReadinessService readiness;
    private TriageV2ShadowMetrics metrics;
    private final TaskExecutor direct = Runnable::run;

    @BeforeEach
    void setUp() {
        workflow = mock(TriageV2WorkflowClient.class);
        readiness = mock(TriageV2ReadinessService.class);
        metrics = new TriageV2ShadowMetrics();
        TriageRuleRegistry registry = mock(TriageRuleRegistry.class);
        when(registry.rulesetSha256()).thenReturn(HASH);
        when(readiness.isReady()).thenReturn(true);
        when(readiness.registry()).thenReturn(Optional.of(registry));
    }

    @Test
    void disabledShadowHasNoCallOrSideEffect() {
        TriageV2ShadowService service = new TriageV2ShadowService(
                workflow, readiness, metrics, direct, false);

        service.submit("private message", "YELLOW");

        verify(workflow, never()).executeTurn(any());
        assertThat(metrics.matchCount() + metrics.mismatchCount() + metrics.errorCount()).isZero();
    }

    @Test
    void enabledShadowSendsEphemeralStateAndRecordsOnlyMatchCategory() {
        AtomicReference<Map<String, Object>> payload = new AtomicReference<>();
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            payload.set(invocation.getArgument(0));
            return new TriageV2WorkflowClient.WorkflowResult(
                    Map.of("triageOutcome", "YELLOW"), "READY", "2.2.0", HASH);
        });
        TriageV2ShadowService service = new TriageV2ShadowService(
                workflow, readiness, metrics, direct, true);

        service.submit("private health message", "YELLOW");

        assertThat(metrics.matchCount()).isEqualTo(1);
        assertThat(metrics.mismatchCount()).isZero();
        assertThat(payload.get()).doesNotContainKeys("userId", "persist", "citations");
        assertThat(payload.get().get("previousState")).isNull();
        assertThat(payload.get().get("expectedRulesetHash")).isEqualTo(HASH);
    }

    @Test
    void mismatchAndPythonFailureNeverEscapeShadowBoundary() {
        when(workflow.executeTurn(any()))
                .thenReturn(new TriageV2WorkflowClient.WorkflowResult(
                        Map.of("triageOutcome", "RED"), "READY", "2.2.0", HASH))
                .thenThrow(new IllegalStateException("private remote details"));
        TriageV2ShadowService service = new TriageV2ShadowService(
                workflow, readiness, metrics, direct, true);

        service.submit("message one", "YELLOW");
        service.submit("message two", "YELLOW");

        assertThat(metrics.mismatchCount()).isEqualTo(1);
        assertThat(metrics.errorCount()).isEqualTo(1);
    }
}
