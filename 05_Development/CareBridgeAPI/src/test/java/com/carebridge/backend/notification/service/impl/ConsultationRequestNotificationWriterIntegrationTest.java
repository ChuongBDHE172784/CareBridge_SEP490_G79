package com.carebridge.backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
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
        jdbcTemplate.update("""
                insert into users
                    (user_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                values (?, 'Notification Recipient', ?, 'MOTHER', true, false, now(), now())
                """, recipientId, uniquePhone());

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
