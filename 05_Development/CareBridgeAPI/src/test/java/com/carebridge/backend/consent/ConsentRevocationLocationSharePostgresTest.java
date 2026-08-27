package com.carebridge.backend.consent;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ConsentRevocationLocationSharePostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private ConsentService consentService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    void revokingCanonicalPermissionDeletesOnlyItsLocationShares() {
        UUID userId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        UUID unrelatedPermissionId = UUID.randomUUID();
        insertExpert(userId);

        Long legacyConsentId = jdbcTemplate.queryForObject("""
                INSERT INTO data_permissions (
                    permission_id, owner_user_id, permission_kind, scope_type, purpose,
                    status, granted_at, expires_at, version_number, created_at, updated_at)
                VALUES (?, ?, 'CONSENT_GRANT', 'LOCATION', 'SHARE',
                        'ACTIVE', now() - interval '1 minute', now() + interval '1 hour',
                        1, now(), now())
                RETURNING legacy_consent_id
                """, Long.class, permissionId, userId);

        insertLocationShare(userId, permissionId);
        insertLocationShare(userId, unrelatedPermissionId);

        ConsentGrantResponse response = consentService.revokeConsent(userId, legacyConsentId);
        entityManager.flush();

        assertThat(response.getPermissionId()).isEqualTo(permissionId);
        assertThat(countShares(permissionId)).isZero();
        assertThat(countShares(unrelatedPermissionId)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM data_permissions
                 WHERE permission_id = ?
                   AND revoked_at IS NOT NULL
                   AND revoked_by = ?
                   AND status = 'REVOKED'
                """, Long.class, permissionId, userId)).isOne();
    }

    private void insertExpert(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, person_id, display_name, email, role, specialty,
                    verification_status, trust_status, enabled, locked, created_at, updated_at)
                VALUES (?, ?, 'Consent revoke expert', ?, 'EXPERT', 'OBSTETRICS',
                        'APPROVED', 'ACTIVE', true, false, now(), now())
                """, userId, userId, "consent-revoke-" + userId + "@test.invalid");
    }

    private void insertLocationShare(UUID expertUserId, UUID permissionId) {
        jdbcTemplate.update("""
                INSERT INTO expert_location_shares (
                    location_share_id, professional_profile_id, user_id, latitude, longitude,
                    availability_status, shared_at, expires_at, consent_reference,
                    created_at, updated_at)
                VALUES (?, ?, ?, 10.7769, 106.7009, 'ONLINE', now(),
                        now() + interval '30 minutes', ?, now(), now())
                """, UUID.randomUUID(), expertUserId, expertUserId, permissionId);
    }

    private long countShares(UUID permissionId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM expert_location_shares WHERE consent_reference = ?",
                Long.class,
                permissionId);
    }
}
