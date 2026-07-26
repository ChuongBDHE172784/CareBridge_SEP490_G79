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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ConsultationRequestNotificationWriterIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private ConsultationRequestNotificationWriter writer;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void idempotencyIsPerRecipientRequestAndEventType() {
        UUID recipientId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        seedUser(recipientId);

        NotificationRecord created = candidate(
                recipientId, requestId, "REQUEST_CREATED");
        NotificationRecord createdRedelivery = candidate(
                recipientId, requestId, "REQUEST_CREATED");
        NotificationRecord accepted = candidate(
                recipientId, requestId, "REQUEST_ACCEPTED");

        assertThat(writer.insertIfAbsent(created)).isTrue();
        assertThat(writer.insertIfAbsent(createdRedelivery)).isFalse();
        assertThat(writer.insertIfAbsent(accepted)).isTrue();

        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from notification_records
                 where user_id = ? and reference_id = ?
                   and type = 'CONSULTATION'
                   and reference_type = 'CONSULTATION_REQUEST'
                """, Integer.class, recipientId, requestId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void concurrentRedeliveryCreatesExactlyOneRecord() throws Exception {
        UUID recipientId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        seedUser(recipientId);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return writer.insertIfAbsent(candidate(recipientId, requestId, "REQUEST_CREATED"));
            });
            Future<Boolean> second = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return writer.insertIfAbsent(candidate(recipientId, requestId, "REQUEST_CREATED"));
            });

            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from notification_records
                 where user_id = ? and reference_id = ?
                   and type = 'CONSULTATION'
                   and reference_type = 'CONSULTATION_REQUEST'
                   and metadata ->> 'eventType' = 'REQUEST_CREATED'
                """, Integer.class, recipientId, requestId);
        assertThat(count).isOne();
    }

    @Test
    void staleClaimCannotCompleteAfterNewerClaim() {
        UUID recipientId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        seedUser(recipientId);
        NotificationRecord record = candidate(recipientId, requestId, "REQUEST_CREATED");
        assertThat(writer.insertIfAbsent(record)).isTrue();

        UUID staleToken = writer.claim(record.getId());
        assertThat(staleToken).isNotNull();
        jdbcTemplate.update("""
                update notification_records
                   set processing_started_at = now() - interval '2 minutes'
                 where id = ?
                """, record.getId());
        UUID currentToken = writer.claim(record.getId());
        assertThat(currentToken).isNotNull().isNotEqualTo(staleToken);

        record.setStatus(NotificationRecordStatus.SENT);
        record.setAttemptCount(1);
        record.setSentAt(Instant.now());
        assertThat(writer.complete(record, staleToken)).isFalse();
        assertThat(writer.complete(record, currentToken)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                  from notification_records
                 where id = ? and status = 'SENT' and claim_token is null
                """, Integer.class, record.getId())).isOne();
    }

    private void seedUser(UUID recipientId) {
        CanonicalUserFixture.insertUser(
                jdbcTemplate,
                recipientId,
                "Notification Recipient",
                uniquePhone(),
                "MOTHER");
    }

    private static NotificationRecord candidate(
            UUID recipientId, UUID requestId, String eventType) {
        return NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(recipientId)
                .type(NotificationType.CONSULTATION)
                .title("Consultation update")
                .body("A consultation request changed")
                .referenceId(requestId)
                .referenceType("CONSULTATION_REQUEST")
                .status(NotificationRecordStatus.PENDING)
                .attemptCount(0)
                .createdAt(Instant.now())
                .metadata(Map.of("eventType", eventType))
                .build();
    }

    private static String uniquePhone() {
        return "08" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }
}
