package com.carebridge.backend.consultation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.event.ConsultationRequestDomainEvent;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(ConsultationRequestConcurrencyIntegrationTest.BarrierConfig.class)
@RecordApplicationEvents
class ConsultationRequestConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private IConsultationRequestService service;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CreateInsertBarrier insertBarrier;
    @Autowired private ApplicationEvents applicationEvents;
    @MockitoBean private IZegoCloudService zegoCloudService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        insertBarrier.disableAndRelease();
        executor.shutdownNow();
    }

    @Test
    void trustLockFirst_newCreateSeesCommittedTrustLossAndHasZeroCreateSideEffects()
            throws Exception {
        Fixture fixture = seedFixture();
        CountDownLatch trustLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseTrust = new CountDownLatch(1);

        Future<?> trust = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ExpertProfile locked = expertProfileRepository
                    .findByIdForUpdate(fixture.expertProfileId())
                    .orElseThrow();
            locked.setTrustStatus(TrustStatus.REVOKED);
            expertProfileRepository.save(locked);
            trustLockAcquired.countDown();
            await(releaseTrust);
        }));
        assertThat(trustLockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

        UUID clientRequestId = UUID.randomUUID();
        Future<?> create = executor.submit(() -> service.create(
                request(fixture.expertProfileId(), clientRequestId), fixture.motherId()));
        assertThatThrownBy(() -> create.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isZero();

        releaseTrust.countDown();
        trust.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> create.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(ExecutionException.class,
                        ex -> assertThat(ex.getCause())
                                .isInstanceOfSatisfying(ConsultationRequestException.class,
                                        business -> assertThat(business.getCode())
                                                .isEqualTo("CONREQ-002")));
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isZero();
        assertThat(createdEventCount(clientRequestId)).isZero();
    }

    @Test
    void createLockFirst_commitsPendingBeforeTrustMutationContinues() throws Exception {
        Fixture fixture = seedFixture();
        insertBarrier.enable();
        UUID clientRequestId = UUID.randomUUID();

        Future<?> create = executor.submit(() -> service.create(
                request(fixture.expertProfileId(), clientRequestId), fixture.motherId()));
        assertThat(insertBarrier.writerReached.await(10, TimeUnit.SECONDS)).isTrue();

        CountDownLatch trustStarted = new CountDownLatch(1);
        Future<?> trust = executor.submit(() -> {
            trustStarted.countDown();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                ExpertProfile locked = expertProfileRepository
                        .findByIdForUpdate(fixture.expertProfileId())
                        .orElseThrow();
                locked.setTrustStatus(TrustStatus.REVOKED);
                expertProfileRepository.save(locked);
            });
        });
        assertThat(trustStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> trust.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        insertBarrier.releaseWriter.countDown();
        var result = create.get(10, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        trust.get(10, TimeUnit.SECONDS);

        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM expert_consultation_requests WHERE requester_user_id=? AND client_request_id=?",
                String.class, fixture.motherId(), clientRequestId)).isEqualTo("PENDING");
        assertThat(expertProfileRepository.findById(fixture.expertProfileId()).orElseThrow().getTrustStatus())
                .isEqualTo(TrustStatus.REVOKED);
    }

    @Test
    void retryAfterTrustLossReturnsExistingWithoutExpertLockOrNewSideEffects() {
        Fixture fixture = seedFixture();
        UUID clientRequestId = UUID.randomUUID();
        CreateConsultationRequestRequest request =
                request(fixture.expertProfileId(), clientRequestId);

        var first = service.create(request, fixture.motherId());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ExpertProfile locked = expertProfileRepository
                    .findByIdForUpdate(fixture.expertProfileId())
                    .orElseThrow();
            locked.setTrustStatus(TrustStatus.SUSPENDED);
            expertProfileRepository.save(locked);
        });
        long eventsBefore = applicationEvents.stream(ConsultationRequestDomainEvent.class)
                .filter(event -> event.requestId().equals(first.response().getId()))
                .count();

        var retry = service.create(request, fixture.motherId());

        assertThat(retry.created()).isFalse();
        assertThat(retry.response().getId()).isEqualTo(first.response().getId());
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
        assertThat(applicationEvents.stream(ConsultationRequestDomainEvent.class)
                .filter(event -> event.requestId().equals(first.response().getId()))
                .count()).isEqualTo(eventsBefore);
    }

    private Fixture seedFixture() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, motherId, "Concurrency Mother", uniquePhone(), "MOTHER");
        CanonicalUserFixture.insertUser(
                jdbcTemplate, expertUserId, "Concurrency Expert", uniquePhone(), "EXPERT");
        jdbcTemplate.update("""
                INSERT INTO professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        return new Fixture(motherId, expertUserId, expertProfileId);
    }

    private static CreateConsultationRequestRequest request(
            UUID expertProfileId, UUID clientRequestId) {
        return CreateConsultationRequestRequest.builder()
                .clientRequestId(clientRequestId)
                .expertProfileId(expertProfileId)
                .topic("Nutrition")
                .description("Please advise on feeding.")
                .build();
    }

    private int countRequests(UUID motherId, UUID clientRequestId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expert_consultation_requests WHERE requester_user_id=? AND client_request_id=?",
                Integer.class, motherId, clientRequestId);
    }

    private long createdEventCount(UUID ignoredClientRequestId) {
        return applicationEvents.stream(ConsultationRequestDomainEvent.class)
                .filter(event -> "REQUEST_CREATED".equals(event.eventType()))
                .count();
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
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

    private record Fixture(UUID motherId, UUID expertUserId, UUID expertProfileId) {
    }

    @TestConfiguration
    static class BarrierConfig {
        @Bean
        CreateInsertBarrier createInsertBarrier() {
            return new CreateInsertBarrier();
        }
    }

    @Aspect
    static class CreateInsertBarrier {
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

        @Before("execution(* com.carebridge.backend.consultation.repository.ConsultationRequestWriter.insertIfAbsent(..))")
        public void beforeInsert() {
            if (enabled) {
                writerReached.countDown();
                await(releaseWriter);
            }
        }
    }
}
