package com.carebridge.backend.triage.service;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observability counters for the intake red-flag pre-screen (CB-TRIAGE-IMP-003 §5.1).
 * Mirrors the {@link TriageFallbackMetrics} pattern — never logs intake content or user data.
 *
 * @version 1.0
 */
@Component
public class TriagePreScreenMetrics {

    private static final Logger log = LoggerFactory.getLogger(TriagePreScreenMetrics.class);

    private final AtomicLong shortCircuitTotal = new AtomicLong();
    private final AtomicLong annotationTotal = new AtomicLong();
    private final AtomicLong degradedTotal = new AtomicLong();

    public void recordShortCircuit(String flow) {
        log.info("triage_prescreen_short_circuit flow={} total={}",
                flow, shortCircuitTotal.incrementAndGet());
    }

    public void recordAnnotation(String flow) {
        log.info("triage_prescreen_annotation flow={} total={}",
                flow, annotationTotal.incrementAndGet());
    }

    public void recordDegraded(String flow) {
        log.warn("triage_prescreen_degraded flow={} total={}",
                flow, degradedTotal.incrementAndGet());
    }

    public long shortCircuitCount() {
        return shortCircuitTotal.get();
    }

    public long annotationCount() {
        return annotationTotal.get();
    }

    public long degradedCount() {
        return degradedTotal.get();
    }
}
