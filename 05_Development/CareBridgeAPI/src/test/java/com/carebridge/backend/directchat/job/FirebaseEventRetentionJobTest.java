package com.carebridge.backend.directchat.job;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carebridge.backend.integration.firebase.IFirebaseRealtimeGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FirebaseEventRetentionJobTest {

    @Mock private IFirebaseRealtimeGateway gateway;

    private final Instant now = Instant.parse("2026-07-15T08:00:00Z");
    private FirebaseEventRetentionJob job;

    @BeforeEach
    void setUp() {
        job = new FirebaseEventRetentionJob(gateway, Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(job, "retentionHours", 24L);
    }

    @Test
    void purgeExpiredEvents_whenEnabled_usesConfiguredCutoff() {
        ReflectionTestUtils.setField(job, "firestoreEnabled", true);

        job.purgeExpiredEvents();

        verify(gateway).purgeEventsOlderThan(now.minusSeconds(24L * 60 * 60));
    }

    @Test
    void purgeExpiredEvents_whenDisabled_doesNothing() {
        ReflectionTestUtils.setField(job, "firestoreEnabled", false);

        job.purgeExpiredEvents();

        verify(gateway, never()).purgeEventsOlderThan(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void purgeExpiredEvents_whenFirestoreFails_doesNotEscapeScheduler() {
        ReflectionTestUtils.setField(job, "firestoreEnabled", true);
        org.mockito.Mockito.doThrow(new IllegalStateException("Firestore unavailable"))
                .when(gateway).purgeEventsOlderThan(org.mockito.ArgumentMatchers.any());

        job.purgeExpiredEvents();

        verify(gateway).purgeEventsOlderThan(now.minusSeconds(24L * 60 * 60));
    }

    @Test
    void purgeExpiredEvents_whenRetentionIsNotPositive_doesNothing() {
        ReflectionTestUtils.setField(job, "firestoreEnabled", true);
        ReflectionTestUtils.setField(job, "retentionHours", 0L);

        job.purgeExpiredEvents();

        verify(gateway, never()).purgeEventsOlderThan(org.mockito.ArgumentMatchers.any());
    }
}
