package com.carebridge.backend.directchat.job;

import com.carebridge.backend.integration.firebase.IFirebaseRealtimeGateway;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** ADR-DCC-006: purges RTDB signal nodes older than the retention window — transport-layer only. */
@Component
public class FirebaseEventRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(FirebaseEventRetentionJob.class);

    private final IFirebaseRealtimeGateway gateway;
    private final Clock clock;

    @Value("${carebridge.firebase.realtime.enabled:false}")
    private boolean realtimeEnabled;

    @Value("${carebridge.directchat.firebase-event-retention-hours:24}")
    private long retentionHours;

    @Autowired
    public FirebaseEventRetentionJob(IFirebaseRealtimeGateway gateway) {
        this(gateway, Clock.systemDefaultZone());
    }

    /** Test constructor. */
    public FirebaseEventRetentionJob(IFirebaseRealtimeGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredEvents() {
        if (!realtimeEnabled) {
            return;
        }
        Instant cutoff = Instant.now(clock).minus(java.time.Duration.ofHours(retentionHours));
        try {
            gateway.purgeEventsOlderThan(cutoff);
        } catch (Exception ex) {
            // Best-effort hygiene job — never let a Firebase outage break the scheduler.
            log.error("Firebase event retention purge failed", ex);
        }
    }
}
