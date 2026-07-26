package com.carebridge.backend.consultation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ConsultationRequestRepositoryIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private ConsultationRequestRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void maliciousTextIsPersistedAsDataAndDoesNotAlterSchema() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        seedUser(motherId, "MOTHER");
        seedUser(expertUserId, "EXPERT");
        jdbcTemplate.update("""
                insert into professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status,
                     trust_status, created_at, updated_at)
                values (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        String injection = "'; DROP TABLE expert_consultation_requests; --";
        Instant now = Instant.now();
        ConsultationRequest request = ConsultationRequest.builder()
                .id(UUID.randomUUID())
                .requesterUserId(motherId)
                .expertProfileId(expertProfileId)
                .clientRequestId(UUID.randomUUID())
                .topic(injection)
                .description(injection)
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(now.plusSeconds(3600))
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.saveAndFlush(request);

        ConsultationRequest stored = repository.findById(request.getId()).orElseThrow();
        assertThat(stored.getTopic()).isEqualTo(injection);
        assertThat(stored.getDescription()).isEqualTo(injection);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from expert_consultation_requests where id = ?",
                Integer.class,
                request.getId())).isEqualTo(1);
    }

    private void seedUser(UUID id, String role) {
        CanonicalUserFixture.insertUser(
                jdbcTemplate, id, "Repository Test User", uniquePhone(), role);
    }

    private static String uniquePhone() {
        return "07" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }
}
