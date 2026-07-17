package com.carebridge.backend.consultation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
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

@Import(ConsultationRequestAcceptLockConcurrencyIntegrationTest.BarrierConfig.class)
class ConsultationRequestAcceptLockConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private IConsultationRequestService service;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AcceptBarrier acceptBarrier;
    @MockitoBean private IZegoCloudService zegoCloudService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        acceptBarrier.disableAndRelease();
        executor.shutdownNow();
    }

    @Test
    void trustLockFirstMakesAcceptFailWithoutConversationOrTransition() throws Exception {
        Fixture fixture = seedFixture();
        CountDownLatch trustLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseTrust = new CountDownLatch(1);
        Future<?> trust = executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    ExpertProfile locked = expertProfileRepository
                            .findByIdForUpdate(fixture.expertProfileId())
                            .orElseThrow();
                    locked.setTrustStatus(TrustStatus.REVOKED);
                    expertProfileRepository.save(locked);
                    trustLockAcquired.countDown();
                    await(releaseTrust);
                }));
        assertThat(trustLockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> accept = executor.submit(
                () -> service.accept(fixture.requestId(), fixture.expertUserId()));
        assertThatThrownBy(() -> accept.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        releaseTrust.countDown();
        trust.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> accept.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(
                        ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(
                                        ConsultationRequestException.class,
                                        business -> assertThat(business.getCode())
                                                .isEqualTo("CONREQ-004")));
        assertThat(requestStatus(fixture.requestId())).isEqualTo("PENDING");
        assertThat(conversationCount(fixture)).isZero();
    }

    @Test
    void acceptLockFirstCommitsBeforeTrustMutationContinues() throws Exception {
        Fixture fixture = seedFixture();
        int bookingsBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consultation_bookings", Integer.class);
        acceptBarrier.enable();
        Future<?> accept = executor.submit(
                () -> service.accept(fixture.requestId(), fixture.expertUserId()));
        assertThat(acceptBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> trust = executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    ExpertProfile locked = expertProfileRepository
                            .findByIdForUpdate(fixture.expertProfileId())
                            .orElseThrow();
                    locked.setTrustStatus(TrustStatus.REVOKED);
                    expertProfileRepository.save(locked);
                }));
        assertThatThrownBy(() -> trust.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        acceptBarrier.releaseWriter.countDown();
        accept.get(10, TimeUnit.SECONDS);
        trust.get(10, TimeUnit.SECONDS);

        assertThat(requestStatus(fixture.requestId())).isEqualTo("ACCEPTED");
        assertThat(conversationCount(fixture)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM consultation_bookings", Integer.class))
                .isEqualTo(bookingsBefore);
        assertThat(expertProfileRepository
                        .findById(fixture.expertProfileId())
                        .orElseThrow()
                        .getTrustStatus())
                .isEqualTo(TrustStatus.REVOKED);
    }

    @Test
    void concurrentAcceptsProduceOneSuccessAndOneConreq005() throws Exception {
        Fixture fixture = seedFixture();
        acceptBarrier.enable();
        Future<?> winner = executor.submit(
                () -> service.accept(fixture.requestId(), fixture.expertUserId()));
        assertThat(acceptBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> loser = executor.submit(
                () -> service.accept(fixture.requestId(), fixture.expertUserId()));
        assertThatThrownBy(() -> loser.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        acceptBarrier.releaseWriter.countDown();
        winner.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> loser.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(
                        ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(
                                        ConsultationRequestException.class,
                                        business -> assertThat(business.getCode())
                                                .isEqualTo("CONREQ-005")));
        assertThat(requestStatus(fixture.requestId())).isEqualTo("ACCEPTED");
        assertThat(conversationCount(fixture)).isEqualTo(1);
    }

    @Test
    void cancelWinningAgainstPausedAcceptRollsBackConversationInsert() throws Exception {
        Fixture fixture = seedFixture();
        acceptBarrier.enable();
        Future<?> accept = executor.submit(
                () -> service.accept(fixture.requestId(), fixture.expertUserId()));
        assertThat(acceptBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> cancel = executor.submit(
                () -> service.cancel(fixture.requestId(), fixture.motherId()));
        cancel.get(10, TimeUnit.SECONDS);
        assertThat(requestStatus(fixture.requestId())).isEqualTo("CANCELLED");

        acceptBarrier.releaseWriter.countDown();
        assertThatThrownBy(() -> accept.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(
                        ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(
                                        ConsultationRequestException.class,
                                        business -> assertThat(business.getCode())
                                                .isEqualTo("CONREQ-005")));
        assertThat(requestStatus(fixture.requestId())).isEqualTo("CANCELLED");
        assertThat(conversationCount(fixture)).isZero();
    }

    private Fixture seedFixture() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        seedUser(motherId, "Accept Mother", "MOTHER");
        seedUser(expertUserId, "Accept Expert", "EXPERT");
        jdbcTemplate.update("""
                INSERT INTO expert_profiles
                    (expert_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        jdbcTemplate.update("""
                INSERT INTO consultation_requests
                    (id, requester_user_id, expert_profile_id, client_request_id,
                     topic, description, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Nutrition', 'Description', 'PENDING',
                        now() + interval '48 hours', now(), now())
                """, requestId, motherId, expertProfileId, UUID.randomUUID());
        return new Fixture(motherId, expertUserId, expertProfileId, requestId);
    }

    private void seedUser(UUID id, String name, String role) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, false, now(), now())
                """, id, name, uniquePhone(), role);
    }

    private String requestStatus(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM consultation_requests WHERE id=?",
                String.class,
                requestId);
    }

    private int conversationCount(Fixture fixture) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM direct_conversations
                 WHERE mother_user_id=? AND expert_user_id=?
                """,
                Integer.class,
                fixture.motherId(),
                fixture.expertUserId());
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

    private record Fixture(
            UUID motherId,
            UUID expertUserId,
            UUID expertProfileId,
            UUID requestId) {
    }

    @TestConfiguration
    static class BarrierConfig {
        @Bean
        AcceptBarrier consultationAcceptBarrier() {
            return new AcceptBarrier();
        }
    }

    @Aspect
    static class AcceptBarrier {
        volatile boolean enabled;
        volatile CountDownLatch writerReached = new CountDownLatch(1);
        volatile CountDownLatch releaseWriter = new CountDownLatch(1);

        void enable() {
            enabled = true;
            writerReached = new CountDownLatch(1);
            releaseWriter = new CountDownLatch(1);
        }

        void disableAndRelease() {
            enabled = false;
            releaseWriter.countDown();
        }

        @Before("execution(* com.carebridge.backend.directchat.service.impl.DirectConversationWriter.insertIfAbsent(..))")
        public void beforeConversationInsert() {
            if (enabled) {
                writerReached.countDown();
                await(releaseWriter);
            }
        }
    }
}
