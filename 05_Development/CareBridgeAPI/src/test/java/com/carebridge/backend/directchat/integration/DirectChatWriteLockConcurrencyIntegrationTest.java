package com.carebridge.backend.directchat.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.service.IConversationCallService;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(DirectChatWriteLockConcurrencyIntegrationTest.BarrierConfig.class)
class DirectChatWriteLockConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private IDirectConversationService conversationService;
    @Autowired private IDirectMessageService messageService;
    @Autowired private IConversationCallService callService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private WriteBarrier writeBarrier;
    @MockitoBean private IZegoCloudService zegoCloudService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        writeBarrier.disableAndRelease();
        executor.shutdownNow();
    }

    @Test
    void findOrCreate_trustLockFirstFailsAfterCommittedTrustLoss() throws Exception {
        Fixture fixture = seedFixture(false);
        LockOwner trust = holdTrustLock(fixture.expertProfileId(), TrustStatus.REVOKED);
        Future<?> operation = executor.submit(() -> conversationService.findOrCreate(
                fixture.motherId(), fixture.expertProfileId()));
        assertBlocked(operation);

        trust.release();
        trust.future().get(10, TimeUnit.SECONDS);
        assertDirectChatFailure(operation, "DCC-002");
        assertThat(count("direct_conversations", fixture)).isZero();
    }

    @Test
    void findOrCreate_interactionLockFirstCommitsBeforeTrustMutation() throws Exception {
        Fixture fixture = seedFixture(false);
        writeBarrier.enable(WriteKind.CONVERSATION);
        Future<?> operation = executor.submit(() -> conversationService.findOrCreate(
                fixture.motherId(), fixture.expertProfileId()));
        assertThat(writeBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> trust = revokeInBackground(fixture.expertProfileId());
        assertBlocked(trust);
        writeBarrier.releaseWriter.countDown();
        operation.get(10, TimeUnit.SECONDS);
        trust.get(10, TimeUnit.SECONDS);

        assertThat(count("direct_conversations", fixture)).isEqualTo(1);
        assertTrust(fixture.expertProfileId(), TrustStatus.REVOKED);
    }

    @Test
    void sendMessage_trustLockFirstFailsWithoutMessageOrAudit() throws Exception {
        Fixture fixture = seedFixture(true);
        LockOwner trust = holdTrustLock(fixture.expertProfileId(), TrustStatus.REVOKED);
        Future<?> operation = executor.submit(() -> messageService.sendMessage(
                fixture.conversationId(), fixture.motherId(), messageRequest()));
        assertBlocked(operation);

        trust.release();
        trust.future().get(10, TimeUnit.SECONDS);
        assertDirectChatFailure(operation, "DCC-010");
        assertThat(count("direct_messages", fixture)).isZero();
    }

    @Test
    void sendMessage_interactionLockFirstCommitsBeforeTrustMutation() throws Exception {
        Fixture fixture = seedFixture(true);
        writeBarrier.enable(WriteKind.MESSAGE);
        Future<?> operation = executor.submit(() -> messageService.sendMessage(
                fixture.conversationId(), fixture.motherId(), messageRequest()));
        assertThat(writeBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> trust = revokeInBackground(fixture.expertProfileId());
        assertBlocked(trust);
        writeBarrier.releaseWriter.countDown();
        operation.get(10, TimeUnit.SECONDS);
        trust.get(10, TimeUnit.SECONDS);

        assertThat(count("direct_messages", fixture)).isEqualTo(1);
        assertTrust(fixture.expertProfileId(), TrustStatus.REVOKED);
    }

    @Test
    void initiateCall_trustLockFirstFailsWithoutCall() throws Exception {
        Fixture fixture = seedFixture(true);
        LockOwner trust = holdTrustLock(fixture.expertProfileId(), TrustStatus.REVOKED);
        Future<?> operation = executor.submit(() -> callService.initiateCall(
                fixture.conversationId(), fixture.motherId(), CallType.VOICE));
        assertBlocked(operation);

        trust.release();
        trust.future().get(10, TimeUnit.SECONDS);
        assertDirectChatFailure(operation, "DCC-010");
        assertThat(count("conversation_calls", fixture)).isZero();
    }

    @Test
    void initiateCall_interactionLockFirstCommitsBeforeTrustMutation() throws Exception {
        Fixture fixture = seedFixture(true);
        writeBarrier.enable(WriteKind.CALL);
        Future<?> operation = executor.submit(() -> callService.initiateCall(
                fixture.conversationId(), fixture.motherId(), CallType.VOICE));
        assertThat(writeBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> trust = revokeInBackground(fixture.expertProfileId());
        assertBlocked(trust);
        writeBarrier.releaseWriter.countDown();
        operation.get(10, TimeUnit.SECONDS);
        trust.get(10, TimeUnit.SECONDS);

        assertThat(count("conversation_calls", fixture)).isEqualTo(1);
        assertTrust(fixture.expertProfileId(), TrustStatus.REVOKED);
    }

    private LockOwner holdTrustLock(UUID expertProfileId, TrustStatus target) throws Exception {
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> future = executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    ExpertProfile locked = expertProfileRepository
                            .findByIdForUpdate(expertProfileId)
                            .orElseThrow();
                    locked.setTrustStatus(target);
                    expertProfileRepository.save(locked);
                    lockAcquired.countDown();
                    await(release);
                }));
        assertThat(lockAcquired.await(10, TimeUnit.SECONDS)).isTrue();
        return new LockOwner(future, release);
    }

    private Future<?> revokeInBackground(UUID expertProfileId) {
        return executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    ExpertProfile locked = expertProfileRepository
                            .findByIdForUpdate(expertProfileId)
                            .orElseThrow();
                    locked.setTrustStatus(TrustStatus.REVOKED);
                    expertProfileRepository.save(locked);
                }));
    }

    private void assertBlocked(Future<?> future) {
        assertThatThrownBy(() -> future.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
    }

    private void assertDirectChatFailure(Future<?> future, String code) {
        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(
                        ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(
                                        DirectChatException.class,
                                        business -> assertThat(business.getCode()).isEqualTo(code)));
    }

    private Fixture seedFixture(boolean withConversation) {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        UUID conversationId = withConversation ? UUID.randomUUID() : null;
        seedUser(motherId, "Lock Mother", "MOTHER");
        seedUser(expertUserId, "Lock Expert", "EXPERT");
        jdbcTemplate.update("""
                INSERT INTO professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        if (withConversation) {
            jdbcTemplate.update("""
                    INSERT INTO archived_realtime_records
                        (archive_id, legacy_table, legacy_id, mother_user_id, expert_user_id,
                         status, original_created_at, last_activity_at)
                    VALUES (?, 'direct_conversations', ?, ?, ?, 'ACTIVE', now(), now())
                    """, conversationId, conversationId.toString(), motherId, expertUserId);
        }
        return new Fixture(motherId, expertUserId, expertProfileId, conversationId);
    }

    private void seedUser(UUID id, String name, String role) {
        CanonicalUserFixture.insertUser(jdbcTemplate, id, name, uniquePhone(), role);
    }

    private int count(String table, Fixture fixture) {
        String sql = switch (table) {
            case "direct_conversations" ->
                    "SELECT COUNT(*) FROM archived_realtime_records "
                            + "WHERE legacy_table='direct_conversations' AND mother_user_id=? AND expert_user_id=?";
            case "direct_messages" ->
                    "SELECT COUNT(*) FROM archived_realtime_records "
                            + "WHERE legacy_table='direct_messages' AND conversation_id=?";
            case "conversation_calls" ->
                    "SELECT COUNT(*) FROM archived_realtime_records "
                            + "WHERE legacy_table='conversation_calls' AND conversation_id=?";
            default -> throw new IllegalArgumentException(table);
        };
        if ("direct_conversations".equals(table)) {
            return jdbcTemplate.queryForObject(
                    sql, Integer.class, fixture.motherId(), fixture.expertUserId());
        }
        return jdbcTemplate.queryForObject(sql, Integer.class, fixture.conversationId());
    }

    private void assertTrust(UUID expertProfileId, TrustStatus expected) {
        assertThat(expertProfileRepository.findById(expertProfileId).orElseThrow().getTrustStatus())
                .isEqualTo(expected);
    }

    private static SendDirectMessageRequest messageRequest() {
        SendDirectMessageRequest request = new SendDirectMessageRequest();
        request.setClientMessageId(UUID.randomUUID());
        request.setMessageBody("Hello expert");
        return request;
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for synchronization");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private enum WriteKind {
        CONVERSATION,
        MESSAGE,
        CALL
    }

    private record Fixture(
            UUID motherId,
            UUID expertUserId,
            UUID expertProfileId,
            UUID conversationId) {
    }

    private record LockOwner(Future<?> future, CountDownLatch releaseLatch) {
        void release() {
            releaseLatch.countDown();
        }
    }

    @TestConfiguration
    static class BarrierConfig {
        @Bean
        WriteBarrier directChatWriteBarrier() {
            return new WriteBarrier();
        }
    }

    @Aspect
    static class WriteBarrier {
        volatile WriteKind enabledKind;
        volatile CountDownLatch writerReached = new CountDownLatch(1);
        volatile CountDownLatch releaseWriter = new CountDownLatch(1);

        void enable(WriteKind kind) {
            enabledKind = kind;
            writerReached = new CountDownLatch(1);
            releaseWriter = new CountDownLatch(1);
        }

        void disableAndRelease() {
            enabledKind = null;
            releaseWriter.countDown();
        }

        @Before("execution(* com.carebridge.backend.directchat.service.impl.DirectConversationWriter.insertIfAbsent(..))")
        public void beforeConversationInsert() {
            awaitIfEnabled(WriteKind.CONVERSATION);
        }

        @Before("execution(* com.carebridge.backend.directchat.service.impl.DirectMessageWriter.insertIfAbsent(..))")
        public void beforeMessageInsert() {
            awaitIfEnabled(WriteKind.MESSAGE);
        }

        @Before("execution(* com.carebridge.backend.directchat.repository.ConversationCallRepository.save(..))")
        public void beforeCallInsert() {
            awaitIfEnabled(WriteKind.CALL);
        }

        private void awaitIfEnabled(WriteKind kind) {
            if (enabledKind == kind) {
                writerReached.countDown();
                await(releaseWriter);
            }
        }
    }
}
