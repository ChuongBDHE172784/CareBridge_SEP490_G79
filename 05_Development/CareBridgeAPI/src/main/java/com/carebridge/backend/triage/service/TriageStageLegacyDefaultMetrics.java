package com.carebridge.backend.triage.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Observes backward-compatible stage defaults without recording intake content or raw identifiers. */
@Component
public class TriageStageLegacyDefaultMetrics {
    private static final Logger log = LoggerFactory.getLogger(TriageStageLegacyDefaultMetrics.class);

    private final AtomicLong legacyDefaultTotal = new AtomicLong();

    public void record(UUID userId, boolean hasBabyProfileId, boolean hasMotherProfileId) {
        long total = legacyDefaultTotal.incrementAndGet();
        String ownerHash = userId == null ? "none" : Integer.toHexString(userId.hashCode());
        log.info("triage_stage_legacy_default_total={} timestamp={} ownerHash={} hasBabyProfileId={} hasMotherProfileId={}",
                total, Instant.now(), ownerHash, hasBabyProfileId, hasMotherProfileId);
    }

    public long legacyDefaultTotal() {
        return legacyDefaultTotal.get();
    }
}
