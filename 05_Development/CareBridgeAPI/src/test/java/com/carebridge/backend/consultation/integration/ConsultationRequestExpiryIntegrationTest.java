package com.carebridge.backend.consultation.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consultation.event.ConsultationRequestDomainEvent;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
class ConsultationRequestExpiryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private IConsultationRequestService service;
    @Autowired private ApplicationEvents applicationEvents;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void expiresOnlyOverduePendingRowsAndPublishesSystemActor() {
        Fixture fixture = seedFixture();
        UUID overduePending = insertRequest(fixture, "PENDING", "-1 hour", false);
        UUID futurePending = insertRequest(fixture, "PENDING", "1 hour", false);
        UUID alreadyAccepted = insertRequest(fixture, "ACCEPTED", "-1 hour", true);

        assertThat(service.expireOverdueRequests()).isEqualTo(1);

        assertThat(status(overduePending)).isEqualTo("EXPIRED");
        assertThat(respondedAt(overduePending)).isNotNull();
        assertThat(status(futurePending)).isEqualTo("PENDING");
        assertThat(status(alreadyAccepted)).isEqualTo("ACCEPTED");
        assertThat(applicationEvents.stream(ConsultationRequestDomainEvent.class)
                        .filter(event -> event.requestId().equals(overduePending))
                        .anyMatch(event -> "REQUEST_EXPIRED".equals(event.eventType())
                                && event.actorUserId() == null
                                && "SYSTEM".equals(event.actorType())))
                .isTrue();
    }

    private Fixture seedFixture() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        // Canonical model: the expert profile IS the users row, so the profile id is the user id.
        UUID expertProfileId = expertUserId;
        seedUser(motherId, "Expiry Mother", "MOTHER");
        seedUser(expertUserId, "Expiry Expert", "EXPERT");
        jdbcTemplate.update("""
                UPDATE users
                   SET specialty='Sản khoa', verification_status='APPROVED', trust_status='ACTIVE'
                 WHERE user_id=?
                """, expertUserId);
        return new Fixture(motherId, expertProfileId, expertUserId);
    }

    private UUID insertRequest(
            Fixture fixture, String status, String expiryOffset, boolean responded) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO expert_consultation_requests
                    (id, requester_user_id, expert_profile_id, client_request_id,
                     topic, description, status, responded_at, responded_by,
                     expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Expiry', 'Description', ?,
                        CASE WHEN ? THEN now() ELSE NULL END,
                        CASE WHEN ? THEN ? ELSE NULL END,
                        now() + (?::interval), now() - interval '2 hours', now())
                """,
                id,
                fixture.motherId(),
                fixture.expertProfileId(),
                UUID.randomUUID(),
                status,
                responded,
                responded,
                fixture.expertUserId(),
                expiryOffset);
        return id;
    }

    private void seedUser(UUID id, String name, String role) {
        CanonicalUserFixture.insertUser(jdbcTemplate, id, name, uniquePhone(), role);
    }

    private String status(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM expert_consultation_requests WHERE id=?", String.class, id);
    }

    private java.time.Instant respondedAt(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT responded_at FROM expert_consultation_requests WHERE id=?",
                java.time.Instant.class,
                id);
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private record Fixture(UUID motherId, UUID expertProfileId, UUID expertUserId) {
    }
}
