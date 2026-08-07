package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.rules.TriageRuleRegistry;
import com.carebridge.backend.triage.rules.TriageV2ReadinessService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Default-off, side-effect-free V2 shadow runner. It calls only the Python workflow transport and
 * records a categorical comparison; it never writes sessions, citations, messages, or identifiers.
 */
@Component
public class TriageV2ShadowService {
    private final TriageV2WorkflowClient workflow;
    private final TriageV2ReadinessService readiness;
    private final TriageV2ShadowMetrics metrics;
    private final TaskExecutor executor;
    private final boolean enabled;

    public TriageV2ShadowService(
            TriageV2WorkflowClient workflow,
            TriageV2ReadinessService readiness,
            TriageV2ShadowMetrics metrics,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            @Value("${carebridge.triage.v2.shadow-enabled:false}") boolean enabled) {
        this.workflow = workflow;
        this.readiness = readiness;
        this.metrics = metrics;
        this.executor = executor;
        this.enabled = enabled;
    }

    public void submit(String message, String v1Outcome) {
        if (!enabled || message == null || message.isBlank()) return;
        String bounded = message.length() <= 2_000 ? message : message.substring(0, 2_000);
        try {
            executor.execute(() -> compare(bounded, v1Outcome));
        } catch (RuntimeException rejected) {
            metrics.recordError();
        }
    }

    void compare(String message, String v1Outcome) {
        try {
            Optional<TriageRuleRegistry> registry = readiness.registry();
            if (!readiness.isReady() || registry.isEmpty()) {
                metrics.recordError();
                return;
            }
            String id = UUID.randomUUID().toString();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("sessionId", id);
            request.put("stateVersion", 0);
            request.put("expectedStateVersion", 0);
            request.put("requestId", "shadow_request_" + id.replace("-", ""));
            request.put("messageId", "shadow_message_" + id.replace("-", ""));
            request.put("latestUserMessage", message);
            request.put("activeProfileId", null);
            request.put("previousState", null);
            request.put("signals", Map.of());
            request.put("measurements", Map.of());
            request.put("expectedRulesetHash", registry.get().rulesetSha256());
            TriageV2WorkflowClient.WorkflowResult result = workflow.executeTurn(request);
            String v2Outcome = text(result.state().get("triageOutcome"));
            if (normalize(v1Outcome).equals(normalize(v2Outcome))) metrics.recordMatch();
            else metrics.recordMismatch();
        } catch (RuntimeException failure) {
            metrics.recordError();
        }
    }

    private static String normalize(String outcome) {
        if (outcome == null || outcome.isBlank()) return "NEEDS_MORE_INFO";
        return switch (outcome) {
            case "NEED_MORE_INFO" -> "NEEDS_MORE_INFO";
            default -> outcome;
        };
    }

    private static String text(Object value) {
        return value == null ? "NEEDS_MORE_INFO" : String.valueOf(value);
    }
}
