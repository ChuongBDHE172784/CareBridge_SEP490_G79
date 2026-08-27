package com.carebridge.backend.triage.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CB-TRIAGE-THMC-IMP-001 §8.1 — tunable retention/bounding knobs for triage-written
 * health-context memories (ADR-THMC-002 / ADR-THMC-003).
 *
 * <p>Defaults: ttlDays=30 (Open — ADR-THMC-002 pending DPO confirmation),
 * maxContextEntries=5, maxSummaryChars=500 (Open O4).
 *
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "carebridge.triage.health-memory")
public class HealthMemoryProperties {

    private int ttlDays = 30;
    private int maxContextEntries = 5;
    private int maxSummaryChars = 500;

    public int getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(int ttlDays) {
        this.ttlDays = ttlDays;
    }

    public int getMaxContextEntries() {
        return maxContextEntries;
    }

    public void setMaxContextEntries(int maxContextEntries) {
        this.maxContextEntries = maxContextEntries;
    }

    public int getMaxSummaryChars() {
        return maxSummaryChars;
    }

    public void setMaxSummaryChars(int maxSummaryChars) {
        this.maxSummaryChars = maxSummaryChars;
    }
}
