package com.carebridge.backend.consent;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.service.IExpertAvailabilityService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ConsentRevocationLocationShareConcurrencyPostgresTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private ConsentService consentService;
    @Autowired private IExpertAvailabilityService expertAvailabilityService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void onlineThenRevokeUsesOneLockOrderAndEndsWithNoSharedLocation() throws Exception {
        Fixture fixture = seedFixture();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch locationRowLocked = new CountDownLatch(1);
        CountDownLatch releaseLocationRow = new CountDownLatch(1);
        CountDownLatch onlineTransactionStarted = new CountDownLatch(1);
        CountDownLatch revokeTransactionStarted = new CountDownLatch(1);
        AtomicInteger onlineBackendPid = new AtomicInteger(-1);
        AtomicInteger revokeBackendPid = new AtomicInteger(-1);

        Future<?> blocker = executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    jdbcTemplate.queryForObject("""
                            SELECT location_share_id
                              FROM expert_location_shares
                             WHERE location_share_id = ?
                             FOR UPDATE
                            """, UUID.class, fixture.locationShareId());
                    locationRowLocked.countDown();
                    await(releaseLocationRow);
                }));

        Future<LocationShareResponse> online = null;
        Future<ConsentGrantResponse> revoke = null;
        try {
            assertThat(locationRowLocked.await(10, TimeUnit.SECONDS))
                    .as("location blocker acquired its row lock")
                    .isTrue();

            online = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        onlineBackendPid.set(currentBackendPid());
                        onlineTransactionStarted.countDown();
                        return expertAvailabilityService.setOnlineStatus(fixture.profileId(), true);
                    }));
            assertThat(onlineTransactionStarted.await(10, TimeUnit.SECONDS))
                    .as("ONLINE transaction exposed its backend pid")
                    .isTrue();
            assertThat(waitUntilOwnerAdvisoryLockIsHeldBy(
                    fixture.userId(), onlineBackendPid.get(), Duration.ofSeconds(10)))
                    .as("ONLINE acquired owner advisory lock before waiting for location row")
                    .isTrue();

            revoke = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        revokeBackendPid.set(currentBackendPid());
                        revokeTransactionStarted.countDown();
                        return consentService.revokeConsent(
                                fixture.userId(), fixture.legacyConsentId());
                    }));
            assertThat(revokeTransactionStarted.await(10, TimeUnit.SECONDS))
                    .as("revoke transaction exposed its backend pid")
                    .isTrue();
            assertThat(waitUntilRevokeTransactionWaitsForOwnerAdvisoryLock(
                    fixture.userId(),
                    revokeBackendPid.get(),
                    onlineBackendPid.get(),
                    Duration.ofSeconds(10)))
                    .as("exact revoke transaction waits for ONLINE on the same owner advisory key")
                    .isTrue();

            releaseLocationRow.countDown();
            blocker.get(10, TimeUnit.SECONDS);

            assertThat(online.get(10, TimeUnit.SECONDS).getAvailabilityStatus())
                    .isEqualTo("ONLINE");
            assertThat(revoke.get(10, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("REVOKED");
        } finally {
            releaseLocationRow.countDown();
            if (online != null && !online.isDone()) {
                online.cancel(true);
            }
            if (revoke != null && !revoke.isDone()) {
                revoke.cancel(true);
            }
            if (!blocker.isDone()) {
                blocker.cancel(true);
            }
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM expert_location_shares
                 WHERE consent_reference = ?
                """, Long.class, fixture.permissionId())).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM data_permissions
                 WHERE permission_id = ?
                   AND status = 'REVOKED'
                   AND revoked_at IS NOT NULL
                   AND revoked_by = ?
                """, Long.class, fixture.permissionId(), fixture.userId())).isOne();
    }

    private Fixture seedFixture() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        UUID locationShareId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO persons (person_id, display_name, created_at, updated_at)
                VALUES (?, 'Consent concurrency expert', now(), now())
                """, userId);
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, person_id, email, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, 'EXPERT', true, false, now(), now())
                """, userId, userId, "consent-race-" + userId + "@test.invalid");
        jdbcTemplate.update("""
                INSERT INTO professional_profiles (
                    professional_profile_id, user_id, specialty, verification_status,
                    trust_status, created_at, updated_at)
                VALUES (?, ?, 'OBSTETRICS', 'APPROVED', 'ACTIVE', now(), now())
                """, profileId, userId);
        Long legacyConsentId = jdbcTemplate.queryForObject("""
                INSERT INTO data_permissions (
                    permission_id, owner_user_id, permission_kind, scope_type, purpose,
                    status, granted_at, expires_at, version_number, created_at, updated_at)
                VALUES (?, ?, 'CONSENT_GRANT', 'LOCATION', 'SHARE', 'ACTIVE',
                        now() - interval '1 minute', now() + interval '1 hour',
                        1, now(), now())
                RETURNING legacy_consent_id
                """, Long.class, permissionId, userId);
        jdbcTemplate.update("""
                INSERT INTO expert_location_shares (
                    location_share_id, professional_profile_id, latitude, longitude,
                    availability_status, shared_at, expires_at, consent_reference,
                    created_at, updated_at)
                VALUES (?, ?, 10.7769, 106.7009, 'OFFLINE', now(),
                        now() + interval '30 minutes', ?, now(), now())
                """, locationShareId, profileId, permissionId);

        return new Fixture(userId, profileId, permissionId, locationShareId, legacyConsentId);
    }

    private boolean waitUntilOwnerAdvisoryLockIsHeldBy(
            UUID userId, int holderBackendPid, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Boolean held = jdbcTemplate.queryForObject("""
                    WITH expected_lock AS (
                        SELECT hashtextextended(CAST(? AS text), 0) AS lock_key
                    )
                    SELECT EXISTS (
                        SELECT 1
                          FROM pg_locks lock_row
                          CROSS JOIN expected_lock expected
                         WHERE lock_row.pid = ?
                           AND lock_row.locktype = 'advisory'
                           AND lock_row.database = (
                               SELECT oid FROM pg_database WHERE datname = current_database())
                           AND lock_row.classid::bigint =
                               ((expected.lock_key >> 32) & 4294967295::bigint)
                           AND lock_row.objid::bigint =
                               (expected.lock_key & 4294967295::bigint)
                           AND lock_row.objsubid = 1
                           AND lock_row.granted)
                    """, Boolean.class, userId.toString(), holderBackendPid);
            if (Boolean.TRUE.equals(held)) {
                return true;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        return false;
    }

    private boolean waitUntilRevokeTransactionWaitsForOwnerAdvisoryLock(
            UUID userId, int revokeBackendPid, int onlineBackendPid, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Boolean waiting = jdbcTemplate.queryForObject("""
                    WITH expected_lock AS (
                        SELECT hashtextextended(CAST(? AS text), 0) AS lock_key
                    )
                    SELECT EXISTS (
                        SELECT 1
                          FROM pg_locks lock_row
                          CROSS JOIN expected_lock expected
                         WHERE lock_row.pid = ?
                           AND lock_row.locktype = 'advisory'
                           AND lock_row.database = (
                               SELECT oid FROM pg_database WHERE datname = current_database())
                           AND lock_row.classid::bigint =
                               ((expected.lock_key >> 32) & 4294967295::bigint)
                           AND lock_row.objid::bigint =
                               (expected.lock_key & 4294967295::bigint)
                           AND lock_row.objsubid = 1
                           AND NOT lock_row.granted
                           AND ? = ANY(pg_blocking_pids(lock_row.pid)))
                    """, Boolean.class, userId.toString(), revokeBackendPid, onlineBackendPid);
            if (Boolean.TRUE.equals(waiting)) {
                return true;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        return false;
    }

    private int currentBackendPid() {
        return jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release location row lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding location row lock", exception);
        }
    }

    private record Fixture(
            UUID userId,
            UUID profileId,
            UUID permissionId,
            UUID locationShareId,
            Long legacyConsentId) {}
}
