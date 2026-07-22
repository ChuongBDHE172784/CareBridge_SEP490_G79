package com.carebridge.backend.directchat.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.service.IConversationCallService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AnsweredCallCleanupLockIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private IConversationCallService callService;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private IZegoCloudService zegoCloudService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void answeredEndCompletesWhileTrustTransactionStillOwnsExpertLock() throws Exception {
        Fixture fixture = seedFixture(CallStatus.ANSWERED);
        CountDownLatch trustLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseTrust = new CountDownLatch(1);

        Future<?> trust = holdTrustLock(fixture, trustLockAcquired, releaseTrust);
        assertThat(trustLockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> end = executor.submit(() -> callService.end(
                fixture.conversationId(), fixture.callId(), fixture.motherId()));
        var response = end.get(10, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT call_status FROM conversation_calls WHERE call_id=?",
                String.class, fixture.callId())).isEqualTo("ENDED");

        releaseTrust.countDown();
        trust.get(10, TimeUnit.SECONDS);
    }

    @Test
    void cancellableEndWaitsForTrustLockThenReadsCommittedIneligibleState() throws Exception {
        Fixture fixture = seedFixture(CallStatus.RINGING);
        CountDownLatch trustLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseTrust = new CountDownLatch(1);

        Future<?> trust = holdTrustLock(fixture, trustLockAcquired, releaseTrust);
        assertThat(trustLockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> cancel = executor.submit(() -> callService.end(
                fixture.conversationId(), fixture.callId(), fixture.motherId()));
        assertThatThrownBy(() -> cancel.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT call_status FROM conversation_calls WHERE call_id=?",
                String.class, fixture.callId())).isEqualTo("RINGING");

        releaseTrust.countDown();
        trust.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> cancel.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(DirectChatException.class,
                                        business -> assertThat(business.getCode())
                                                .isEqualTo("DCC-010")));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT call_status FROM conversation_calls WHERE call_id=?",
                String.class, fixture.callId())).isEqualTo("RINGING");
    }

    private Future<?> holdTrustLock(
            Fixture fixture,
            CountDownLatch trustLockAcquired,
            CountDownLatch releaseTrust) {
        return executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ExpertProfile locked = expertProfileRepository
                    .findByIdForUpdate(fixture.expertProfileId())
                    .orElseThrow();
            locked.setTrustStatus(TrustStatus.REVOKED);
            expertProfileRepository.save(locked);
            trustLockAcquired.countDown();
            await(releaseTrust);
        }));
    }

    private Fixture seedFixture(CallStatus callStatus) {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                VALUES (?, 'Cleanup Mother', ?, 'MOTHER', true, false, now(), now())
                """, motherId, uniquePhone());
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                VALUES (?, 'Cleanup Expert', ?, 'EXPERT', true, false, now(), now())
                """, expertUserId, uniquePhone());
        jdbcTemplate.update("""
                INSERT INTO professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        jdbcTemplate.update("""
                INSERT INTO direct_conversations
                    (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at)
                VALUES (?, ?, ?, 'ACTIVE', now(), now())
                """, conversationId, motherId, expertUserId);
        jdbcTemplate.update("""
                INSERT INTO conversation_calls
                    (call_id, conversation_id, initiated_by_user_id, call_type, call_status,
                     zego_room_id, initiated_at, answered_at, created_at)
                VALUES (?, ?, ?, 'VOICE', ?, ?, now() - interval '2 minutes',
                        CASE WHEN ? = 'ANSWERED' THEN now() - interval '1 minute' ELSE NULL END,
                        now() - interval '2 minutes')
                """, callId, conversationId, motherId, callStatus.name(),
                callId.toString(), callStatus.name());
        return new Fixture(motherId, expertProfileId, conversationId, callId);
    }

    private static String uniquePhone() {
        return "08" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test synchronization");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record Fixture(
            UUID motherId,
            UUID expertProfileId,
            UUID conversationId,
            UUID callId) {
    }
}
