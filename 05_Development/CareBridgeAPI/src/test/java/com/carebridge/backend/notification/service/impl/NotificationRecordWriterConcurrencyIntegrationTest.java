package com.carebridge.backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * MEDI-TC-021 — two threads racing NotificationRecordWriter.insertIfAbsent for the same
 * (user_id, reference_id) must be settled by the real Postgres partial unique index
 * (uq_notification_records_direct_message via ON CONFLICT ... DO NOTHING), not by any
 * application-level lock — there isn't one by design (ADR-MEDI-004 mục 3).
 */
class NotificationRecordWriterConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private NotificationRecordWriter notificationRecordWriter;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void insertIfAbsent_twoThreadsSamePair_exactlyOneRowWins_noExceptionEscapes() throws Exception {
        UUID recipientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String phoneSuffix = String.valueOf(System.nanoTime()).substring(3, 11);
        CanonicalUserFixture.insertUser(
                jdbcTemplate, recipientId, "Concurrency Recipient", "05" + phoneSuffix, "EXPERT");

        NotificationRecord candidateA = candidate(recipientId, messageId, "from A");
        NotificationRecord candidateB = candidate(recipientId, messageId, "from B");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<Boolean> resultA = new AtomicReference<>();
        AtomicReference<Boolean> resultB = new AtomicReference<>();
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();

        pool.submit(() -> {
            ready.countDown();
            try {
                go.await();
                resultA.set(notificationRecordWriter.insertIfAbsent(candidateA));
            } catch (Throwable t) {
                errorA.set(t);
            }
        });
        pool.submit(() -> {
            ready.countDown();
            try {
                go.await();
                resultB.set(notificationRecordWriter.insertIfAbsent(candidateB));
            } catch (Throwable t) {
                errorB.set(t);
            }
        });

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(errorA.get()).as("thread A must not throw").isNull();
        assertThat(errorB.get()).as("thread B must not throw").isNull();
        assertThat(List.of(resultA.get(), resultB.get()))
                .as("exactly one insertIfAbsent call wins (true), the other loses (false)")
                .containsExactlyInAnyOrder(true, false);

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_records WHERE user_id = ? AND reference_id = ?",
                Integer.class, recipientId, messageId);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void claim_oldPendingRow_usesFreshLease_andCannotBeClaimedAgainImmediately() {
        UUID recipientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String phoneSuffix = String.valueOf(System.nanoTime()).substring(3, 11);
        CanonicalUserFixture.insertUser(
                jdbcTemplate, recipientId, "Lease Recipient", "04" + phoneSuffix, "EXPERT");

        NotificationRecord oldPending = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(recipientId)
                .type(NotificationType.MESSAGE)
                .title("Tin nhắn mới")
                .body("Old pending row")
                .referenceId(messageId)
                .referenceType("DIRECT_MESSAGE")
                .status(NotificationRecordStatus.PENDING)
                .attemptCount(0)
                .createdAt(Instant.now().minusSeconds(600))
                .metadata(Map.of())
                .build();

        assertThat(notificationRecordWriter.insertIfAbsent(oldPending)).isTrue();
        assertThat(notificationRecordWriter.claim(oldPending.getId())).isTrue();
        assertThat(notificationRecordWriter.claim(oldPending.getId()))
                .as("a fresh PROCESSING lease must not be reclaimed based on the old created_at")
                .isFalse();

        Instant processingStartedAt = jdbcTemplate.queryForObject(
                "SELECT processing_started_at FROM notification_records WHERE id = ?",
                (rs, rowNum) -> rs.getTimestamp(1).toInstant(), oldPending.getId());
        assertThat(processingStartedAt).isAfter(Instant.now().minusSeconds(10));
    }

    private static NotificationRecord candidate(UUID recipientId, UUID messageId, String bodySuffix) {
        return NotificationRecord.builder()
                .id(UUID.randomUUID()) // different id per candidate — mirrors 2 independent listener runs
                .userId(recipientId)
                .type(NotificationType.MESSAGE)
                .title("Tin nhắn mới")
                .body("Bạn có tin nhắn mới " + bodySuffix)
                .referenceId(messageId)
                .referenceType("DIRECT_MESSAGE")
                .status(NotificationRecordStatus.FAILED)
                .attemptCount(0)
                .createdAt(Instant.now())
                .metadata(Map.of())
                .build();
    }
}
