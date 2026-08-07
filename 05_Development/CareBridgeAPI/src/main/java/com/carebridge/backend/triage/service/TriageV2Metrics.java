package com.carebridge.backend.triage.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Low-cardinality V2 telemetry. It never accepts IDs, messages, prompts, or exception text. */
@Component
public class TriageV2Metrics {
    private static final Logger log = LoggerFactory.getLogger(TriageV2Metrics.class);

    public enum Failure {
        FALLBACK, HASH_MISMATCH, CITATION_REJECTED, STATE_CONFLICT, MALFORMED_RESPONSE
    }

    private final Map<String, AtomicLong> outcomes = new ConcurrentHashMap<>();
    private final Map<Failure, AtomicLong> failures = new EnumMap<>(Failure.class);
    private final AtomicLong turns = new AtomicLong();
    private final AtomicLong latencyTotalMs = new AtomicLong();
    private final AtomicLong questionTotal = new AtomicLong();
    private final AtomicLong targetConflicts = new AtomicLong();

    public TriageV2Metrics() {
        for (Failure failure : Failure.values()) failures.put(failure, new AtomicLong());
    }

    public void recordTurn(String outcome, long latencyMs, int questionCount, boolean targetConflict) {
        String safeOutcome = switch (outcome == null ? "NONE" : outcome) {
            case "RED", "YELLOW", "NEEDS_MORE_INFO", "OUT_OF_SCOPE" -> outcome;
            default -> "NONE";
        };
        long total = turns.incrementAndGet();
        outcomes.computeIfAbsent(safeOutcome, ignored -> new AtomicLong()).incrementAndGet();
        latencyTotalMs.addAndGet(Math.max(0, Math.min(latencyMs, 60_000)));
        questionTotal.addAndGet(Math.max(0, Math.min(questionCount, 3)));
        if (targetConflict) targetConflicts.incrementAndGet();
        log.info("triage_v2_turn outcome={} total={}", safeOutcome, total);
    }

    public void recordFailure(Failure failure) {
        long count = failures.get(failure).incrementAndGet();
        log.warn("triage_v2_failure category={} total={}", failure, count);
    }

    public long turnCount() { return turns.get(); }
    public long outcomeCount(String outcome) {
        AtomicLong count = outcomes.get(outcome);
        return count == null ? 0 : count.get();
    }
    public long failureCount(Failure failure) { return failures.get(failure).get(); }
    public long targetConflictCount() { return targetConflicts.get(); }
    public double averageLatencyMs() {
        long count = turns.get();
        return count == 0 ? 0 : (double) latencyTotalMs.get() / count;
    }
    public double averageQuestionCount() {
        long count = turns.get();
        return count == 0 ? 0 : (double) questionTotal.get() / count;
    }
}
