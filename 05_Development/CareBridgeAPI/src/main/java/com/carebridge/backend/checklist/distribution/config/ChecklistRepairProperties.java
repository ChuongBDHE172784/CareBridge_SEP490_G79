package com.carebridge.backend.checklist.distribution.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime limits for the app-independent checklist occurrence repair sweep.
 *
 * <p>The sweep is intentionally bounded: a temporary database backlog must not turn one
 * scheduler tick into an unbounded replay.  The values are configuration rather than
 * clinical rules; changing them does not alter the checklist contract or add persistence.
 */
@Component
@ConfigurationProperties(prefix = "carebridge.checklist.repair")
public class ChecklistRepairProperties {

    private boolean enabled = true;
    private String cron = "0 */15 * * * *";
    private ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private int catchUpWeeks = 12;
    private int maxJourneysPerRun = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        if (cron != null && !cron.isBlank()) {
            this.cron = cron;
        }
    }

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        if (zone != null) {
            this.zone = zone;
        }
    }

    public int getCatchUpWeeks() {
        return catchUpWeeks;
    }

    public void setCatchUpWeeks(int catchUpWeeks) {
        this.catchUpWeeks = catchUpWeeks;
    }

    public int getMaxJourneysPerRun() {
        return maxJourneysPerRun;
    }

    public void setMaxJourneysPerRun(int maxJourneysPerRun) {
        this.maxJourneysPerRun = maxJourneysPerRun;
    }

    /** Effective replay horizon consumed by the catch-up service. */
    public int boundedCatchUpWeeks() {
        return Math.max(0, Math.min(catchUpWeeks, 12));
    }

    /** Effective owner bound; non-positive values disable the sweep rather than surprise it. */
    public int boundedMaxJourneysPerRun() {
        return Math.max(0, Math.min(maxJourneysPerRun, 1_000));
    }
}
