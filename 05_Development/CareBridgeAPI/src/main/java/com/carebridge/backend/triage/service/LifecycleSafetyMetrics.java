package com.carebridge.backend.triage.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Story 6.7 observability without identifiers, tokens, routes, or health content. */
@Component
public class LifecycleSafetyMetrics {
    private static final Logger log = LoggerFactory.getLogger(LifecycleSafetyMetrics.class);

    public enum Boundary {
        CONTINUATION,
        PROJECTION
    }

    public enum Outcome {
        CREATED,
        ACKNOWLEDGED,
        REPLAYED,
        REJECTED,
        FAILED,
        RECOVERED
    }

    private final Map<Boundary, Map<Outcome, AtomicLong>> counts = new EnumMap<>(Boundary.class);

    public LifecycleSafetyMetrics() {
        for (Boundary boundary : Boundary.values()) {
            Map<Outcome, AtomicLong> outcomes = new EnumMap<>(Outcome.class);
            for (Outcome outcome : Outcome.values()) {
                outcomes.put(outcome, new AtomicLong());
            }
            counts.put(boundary, outcomes);
        }
    }

    public void record(Boundary boundary, Outcome outcome) {
        long count = counts.get(boundary).get(outcome).incrementAndGet();
        log.info("story67_safety boundary={} outcome={} count={}", boundary, outcome, count);
    }

    public long count(Boundary boundary, Outcome outcome) {
        return counts.get(boundary).get(outcome).get();
    }
}
