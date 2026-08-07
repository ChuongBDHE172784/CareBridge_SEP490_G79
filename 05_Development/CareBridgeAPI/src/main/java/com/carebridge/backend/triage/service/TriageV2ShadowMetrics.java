package com.carebridge.backend.triage.service;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Shadow comparison counters. No session, user, message, rule, or exception data is accepted. */
@Component
public class TriageV2ShadowMetrics {
    private static final Logger log = LoggerFactory.getLogger(TriageV2ShadowMetrics.class);
    private final AtomicLong matches = new AtomicLong();
    private final AtomicLong mismatches = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    public void recordMatch() {
        log.info("triage_v2_shadow comparison=MATCH total={}", matches.incrementAndGet());
    }

    public void recordMismatch() {
        log.warn("triage_v2_shadow comparison=MISMATCH total={}", mismatches.incrementAndGet());
    }

    public void recordError() {
        log.warn("triage_v2_shadow comparison=ERROR total={}", errors.incrementAndGet());
    }

    public long matchCount() { return matches.get(); }
    public long mismatchCount() { return mismatches.get(); }
    public long errorCount() { return errors.get(); }
}
