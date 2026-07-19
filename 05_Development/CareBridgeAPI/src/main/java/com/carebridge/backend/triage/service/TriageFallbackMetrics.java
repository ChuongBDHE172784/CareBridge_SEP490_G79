package com.carebridge.backend.triage.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Counts Java fallback usage without logging intake content or user data. */
@Component
public class TriageFallbackMetrics {
    private static final Logger log = LoggerFactory.getLogger(TriageFallbackMetrics.class);

    public enum Reason {
        TIMEOUT,
        PYTHON_5XX,
        MALFORMED_RESPONSE,
        NETWORK_ERROR,
        OTHER
    }

    private final AtomicLong total = new AtomicLong();
    private final Map<Reason, AtomicLong> byReason = new EnumMap<>(Reason.class);

    public TriageFallbackMetrics() {
        for (Reason reason : Reason.values()) {
            byReason.put(reason, new AtomicLong());
        }
    }

    public void record(Reason reason, String flow) {
        long totalCount = total.incrementAndGet();
        long reasonCount = byReason.get(reason).incrementAndGet();
        log.info("triage_java_fallback flow={} reason={} total={} reasonTotal={}",
                flow, reason, totalCount, reasonCount);
    }

    public long totalCount() {
        return total.get();
    }

    public long count(Reason reason) {
        return byReason.get(reason).get();
    }
}
