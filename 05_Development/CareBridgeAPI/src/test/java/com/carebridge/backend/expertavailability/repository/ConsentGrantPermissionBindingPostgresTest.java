package com.carebridge.backend.expertavailability.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ConsentGrantPermissionBindingPostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private ConsentGrantRepository consentGrantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void exactPermissionBindingRequiresMatchingOwnerScopePurposeAndActiveLifetime() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-24T12:00:00Z");
        insertUser(ownerId, "location-owner@test.invalid");
        insertUser(otherOwnerId, "other-location-owner@test.invalid");
        jdbcTemplate.update("""
                INSERT INTO data_permissions (
                    permission_id, owner_user_id, permission_kind, scope_type, purpose,
                    status, granted_at, expires_at, version_number, created_at, updated_at)
                VALUES (?, ?, 'CONSENT_GRANT', 'LOCATION', 'SHARE',
                        'ACTIVE', ?, ?, 1, ?, ?)
                """,
                permissionId,
                ownerId,
                java.sql.Timestamp.from(now.minusSeconds(60)),
                java.sql.Timestamp.from(now.plusSeconds(300)),
                java.sql.Timestamp.from(now.minusSeconds(60)),
                java.sql.Timestamp.from(now.minusSeconds(60)));

        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(240))).isTrue();
        assertThat(exists(UUID.randomUUID(), ownerId, now, now.plusSeconds(240))).isFalse();
        assertThat(exists(permissionId, otherOwnerId, now, now.plusSeconds(240))).isFalse();
        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(301))).isFalse();

        jdbcTemplate.update(
                "UPDATE data_permissions SET status='SUSPENDED' WHERE permission_id=?",
                permissionId);
        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(240))).isFalse();

        jdbcTemplate.update(
                "UPDATE data_permissions SET status='ACTIVE', granted_at=? WHERE permission_id=?",
                java.sql.Timestamp.from(now.plusSeconds(1)),
                permissionId);
        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(240))).isFalse();

        jdbcTemplate.update(
                "UPDATE data_permissions SET granted_at=? WHERE permission_id=?",
                java.sql.Timestamp.from(now.minusSeconds(60)),
                permissionId);
        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(240))).isTrue();

        jdbcTemplate.update(
                "UPDATE data_permissions SET revoked_at=? WHERE permission_id=?",
                java.sql.Timestamp.from(now),
                permissionId);
        assertThat(exists(permissionId, ownerId, now, now.plusSeconds(240))).isFalse();
    }

    private boolean exists(
            UUID permissionId,
            UUID ownerId,
            Instant now,
            Instant requiredUntil) {
        return consentGrantRepository.existsValidConsentByPermissionIdCoveringInterval(
                permissionId,
                ownerId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                now,
                requiredUntil);
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, person_id, display_name, email, enabled, locked, created_at, updated_at)
                VALUES (?, ?, 'Location consent test user', ?, true, false, now(), now())
                """, userId, userId, email);
    }
}
