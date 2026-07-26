package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import com.carebridge.backend.emergency.repository.EmergencyAlertDeliveryRepository;
import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import com.carebridge.backend.emergency.service.EmergencyAlertProviderFence;
import com.carebridge.backend.emergency.service.FencedAlertDelivery;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalSafetyActionInvariantMigrationIntegrationTest {

    private static final MigrationVersion PRE_STORY66_BRIDGE =
            MigrationVersion.fromVersion("20260722231350");
    private static final MigrationVersion STORY66_BRIDGE =
            MigrationVersion.fromVersion("20260722231360");
    private static final MigrationVersion LEGACY_PRE_STORY66 =
            MigrationVersion.fromVersion("20260722020400");
    private static final MigrationVersion BASELINE =
            MigrationVersion.fromVersion("20260724111500");
    private static final MigrationVersion BASELINE_COMPATIBILITY =
            MigrationVersion.fromVersion("20260724120000");
    private static final MigrationVersion CANONICAL = MigrationVersion.fromVersion("20260724210000");
    private static final Set<String> STORY66_BRANCH_FILES = Set.of(
            "V20260722119950__bridge_story66_out_of_order_parents.sql",
            "V20260722120000__guarantee_triage_emergency_idempotency.sql",
            "V20260722210000__persist_lifecycle_safety_outcomes_and_continuations.sql",
            "V20260722231350__preserve_epic6_lifecycle_bindings.sql");
    private static final String OWNER = "72000000-0000-0000-0000-000000000001";
    private static final String INTAKE_ONE = "72000000-0000-0000-0000-000000000011";
    private static final String INTAKE_TWO = "72000000-0000-0000-0000-000000000012";
    private static final String EMERGENCY = "72000000-0000-0000-0000-000000000021";
    private static final String DUPLICATE = "72000000-0000-0000-0000-000000000022";
    private static final String RECIPIENT = "72000000-0000-0000-0000-000000000002";
    private static final String DEVICE = "72000000-0000-0000-0000-000000000041";
    private static final String NOTIFICATION = "72000000-0000-0000-0000-000000000051";

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @TempDir
    Path tempDirectory;

    @Test
    void migrationReconcilesActiveRowsBackfillsEveryIntakeAndFreezesActions() throws Exception {
        migrate(BASELINE_COMPATIBILITY);
        seedOwnerAndIntakes();
        execute("""
                INSERT INTO safety_events (
                    safety_event_id,user_id,source_event_id,detected_at,event_type,status,
                    record_type,created_at,updated_at)
                VALUES
                  ('72000000-0000-0000-0000-000000000021',
                   '72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000011',now()-interval '2 minutes',
                   'AUTO_TRIAGE','ACTIVE','EMERGENCY_SESSION',now()-interval '2 minutes',now()),
                  ('72000000-0000-0000-0000-000000000022',
                   '72000000-0000-0000-0000-000000000001',NULL,now()-interval '1 minute',
                   'MANUAL','ACTIVE','EMERGENCY_SESSION',now()-interval '1 minute',now());
                INSERT INTO carebridge_migration_bridge.story66_triage_escalation_bridge
                    (intake_session_id,emergency_session_id,owner_user_id,
                     triggered_at,captured_at)
                VALUES ('72000000-0000-0000-0000-000000000012',
                        '72000000-0000-0000-0000-000000000021',
                        '72000000-0000-0000-0000-000000000001',now(),now());
                """);

        migrate(CANONICAL);

        assertThat(number("""
                SELECT count(*) FROM safety_events
                WHERE user_id='72000000-0000-0000-0000-000000000001'
                  AND record_type='EMERGENCY_SESSION' AND status='ACTIVE'
                """)).isOne();
        assertThat(number("SELECT count(*) FROM safety_events WHERE safety_event_id='"
                + DUPLICATE + "' AND status='CANCELLED' AND resolved_at IS NOT NULL"))
                .isOne();
        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                WHERE action_type='TRIAGE_ESCALATION'
                  AND safety_event_id='72000000-0000-0000-0000-000000000021'
                  AND owner_user_id='72000000-0000-0000-0000-000000000001'
                  AND triage_handoff_id IN (
                    '72000000-0000-0000-0000-000000000011',
                    '72000000-0000-0000-0000-000000000012')
                """)).isEqualTo(2);

        assertThatThrownBy(() -> execute("""
                INSERT INTO safety_events (
                    safety_event_id,user_id,detected_at,event_type,status,record_type,
                    created_at,updated_at)
                VALUES (gen_random_uuid(),'72000000-0000-0000-0000-000000000001',
                        now(),'MANUAL','ACTIVE','EMERGENCY_SESSION',now(),now())
                """)).hasMessageContaining("safety_events_one_active_emergency_user_uk");
        assertThatThrownBy(() -> execute("""
                UPDATE safety_event_actions SET summary='tampered'
                WHERE action_type='TRIAGE_ESCALATION'
                """)).hasMessageContaining("SAFETY_EVENT_ACTION_IMMUTABLE");
        assertThatThrownBy(() -> execute("""
                DELETE FROM safety_event_actions WHERE action_type='TRIAGE_ESCALATION'
                """)).hasMessageContaining("SAFETY_EVENT_ACTION_IMMUTABLE");
    }

    @Test
    void story66BridgeWaitsForConcurrentWriterThenCapturesAndDropsEveryLegacyLeaf()
            throws Exception {
        migrateHistoricalStory66Branch();
        seedLegacyStory66State();

        try (Connection writer = connection()) {
            writer.setAutoCommit(false);
            try (var insert = writer.prepareStatement("""
                    INSERT INTO triage_emergency_escalations (
                        intake_session_id,emergency_session_id,user_id,triggered_at)
                    VALUES (?,?,?, '2026-07-24T01:00:00Z')
                    """)) {
                insert.setObject(1, java.util.UUID.fromString(INTAKE_TWO));
                insert.setObject(2, java.util.UUID.fromString(EMERGENCY));
                insert.setObject(3, java.util.UUID.fromString(OWNER));
                assertThat(insert.executeUpdate()).isOne();
            }

            try (var executor = Executors.newSingleThreadExecutor()) {
                var migration = executor.submit(() -> migrate(STORY66_BRIDGE, true));
                assertThatThrownBy(() -> migration.get(300, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                writer.commit();
                migration.get(15, TimeUnit.SECONDS);
            }
        }

        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.story66_triage_escalation_bridge
                 WHERE emergency_session_id =
                       '72000000-0000-0000-0000-000000000021'
                   AND owner_user_id =
                       '72000000-0000-0000-0000-000000000001'
                   AND intake_session_id IN (
                       '72000000-0000-0000-0000-000000000011',
                       '72000000-0000-0000-0000-000000000012')
                """)).isEqualTo(2);
        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.story66_notification_outbox_bridge
                 WHERE emergency_session_id =
                       '72000000-0000-0000-0000-000000000021'
                   AND owner_user_id =
                       '72000000-0000-0000-0000-000000000001'
                   AND status = 'DELIVERED' AND attempt_count = 2
                   AND delivered_at = '2026-07-24T00:30:00Z'
                   AND terminal_at = '2026-07-24T00:30:00Z'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM pg_class relation
                  JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                 WHERE namespace.nspname='public'
                   AND relation.relname IN (
                       'triage_emergency_escalations',
                       'emergency_notification_outbox')
                """)).isZero();

        migrate(CANONICAL);

        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                 WHERE action_type='TRIAGE_ESCALATION'
                   AND safety_event_id=
                       '72000000-0000-0000-0000-000000000021'
                   AND owner_user_id=
                       '72000000-0000-0000-0000-000000000001'
                   AND triage_handoff_id IN (
                       '72000000-0000-0000-0000-000000000011',
                       '72000000-0000-0000-0000-000000000012')
                """)).isEqualTo(2);
        assertThat(number("""
                SELECT count(*) FROM safety_events
                 WHERE safety_event_id=
                       '72000000-0000-0000-0000-000000000021'
                   AND user_id='72000000-0000-0000-0000-000000000001'
                   AND alert_generation=2 AND alert_status='SENT'
                   AND alert_completed_at='2026-07-24T00:30:00Z'
                   AND alert_successful_recipient_count >= 1
                   AND alert_claim_token IS NULL
                   AND alert_lease_expires_at IS NULL
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                 WHERE safety_event_id=
                       '72000000-0000-0000-0000-000000000021'
                   AND action_type='ALERT_ATTEMPT' AND action_phase='RESULT'
                   AND alert_generation=2 AND attempt_number=2
                   AND attempt_status='SENT'
                   AND completed_at='2026-07-24T00:30:00Z'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM pg_class relation
                  JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                 WHERE namespace.nspname='carebridge_migration_bridge'
                   AND relation.relname IN (
                       'story66_triage_escalation_bridge',
                       'story66_notification_outbox_bridge')
                """)).isZero();
    }

    @Test
    void atomicClaimsKeepGenerationHistoryAndRejectStaleOrResolvedFences() throws Exception {
        migrate(CANONICAL);
        seedOwnerAndIntakes();
        execute("""
                INSERT INTO safety_events (
                    safety_event_id,user_id,detected_at,event_type,status,record_type,
                    created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000021',
                        '72000000-0000-0000-0000-000000000001',now(),'MANUAL',
                        'ACTIVE','EMERGENCY_SESSION',now(),now())
                ;
                INSERT INTO device_tokens(id,user_id,token,platform,active,created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000041',
                        '72000000-0000-0000-0000-000000000002',
                        'safety-fence-device','ANDROID',true,now(),now());
                INSERT INTO notification_records (
                    id,user_id,type,title,body,status,attempt_count,channel,is_read,
                    created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000051',
                        '72000000-0000-0000-0000-000000000002','EMERGENCY',
                        'Safety fence','Delivery snapshot','PENDING',0,'PUSH',false,now(),now())
                """);
        EmergencyAlertAttemptRepository repository = new EmergencyAlertAttemptRepository(jdbc());
        EmergencyAlertDeliveryRepository deliveryRepository =
                new EmergencyAlertDeliveryRepository(jdbc());

        var executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                return repository.claim(java.util.UUID.fromString(EMERGENCY),
                        Instant.now().plusSeconds(120));
            });
            var second = executor.submit(() -> {
                start.await();
                return repository.claim(java.util.UUID.fromString(EMERGENCY),
                        Instant.now().plusSeconds(120));
            });
            start.countDown();
            List<Optional<EmergencyAlertClaim>> claims = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(claims).filteredOn(Optional::isPresent).hasSize(1);
            EmergencyAlertClaim generationOne = claims.stream()
                    .flatMap(Optional::stream).findFirst().orElseThrow();

            assertThat(repository.complete(
                    generationOne, "NO_RECIPIENTS", 0, 0, false)).isTrue();
            EmergencyAlertClaim generationTwo = repository.claim(
                    java.util.UUID.fromString(EMERGENCY), Instant.now().plusSeconds(120))
                    .orElseThrow();
            assertThat(generationTwo.generation()).isEqualTo(generationOne.generation() + 1);
            assertThat(generationTwo.fenceToken()).isNotEqualTo(generationOne.fenceToken());
            assertThat(repository.complete(generationOne, "SENT", 1, 0, false)).isFalse();
            var intent = deliveryRepository.insertIntent(
                    generationTwo, java.util.UUID.fromString(RECIPIENT),
                    java.util.UUID.fromString(DEVICE), java.util.UUID.fromString(NOTIFICATION));
            assertThat(deliveryRepository.appendResult(
                    intent.actionId(), generationTwo, true, 1, "fcm-1", null)).isTrue();
            assertThat(deliveryRepository.appendResult(
                    intent.actionId(), generationTwo, true, 1, "fcm-1", null)).isFalse();

            execute("""
                    UPDATE safety_events
                       SET status='RESOLVED', alert_status='SUPPRESSED',
                           alert_lease_expires_at=NULL, resolved_at=now()
                     WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                    """);
            assertThat(repository.renew(
                    generationTwo, Instant.now().plusSeconds(120))).isFalse();
            assertThat(repository.complete(generationTwo, "SENT", 1, 0, false)).isFalse();
        } finally {
            executor.shutdownNow();
        }

        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                  AND action_type='ALERT_ATTEMPT'
                """)).isEqualTo(3);
        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                  AND action_type='ALERT_ATTEMPT' AND action_phase='RESULT'
                  AND attempt_status='NO_RECIPIENTS'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                  AND action_type='DELIVERY' AND alert_generation=2
                  AND action_phase IN ('INTENT','RESULT')
                """)).isEqualTo(2);
        assertThat(number("""
                SELECT alert_generation FROM safety_events
                WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                """)).isEqualTo(2);
    }

    @Test
    void resolutionCommittedBeforeProviderFencePreventsProviderInvocation() throws Exception {
        migrate(CANONICAL);
        seedOwnerAndIntakes();
        seedEmergencyAndDeliveryPrerequisites();
        DriverManagerDataSource dataSource = dataSource();
        EmergencyAlertAttemptRepository attempts =
                new EmergencyAlertAttemptRepository(new JdbcTemplate(dataSource));
        EmergencyAlertProviderFence fence = new EmergencyAlertProviderFence(
                new DataSourceTransactionManager(dataSource), attempts, 1000);
        EmergencyAlertClaim claim = attempts.claim(
                java.util.UUID.fromString(EMERGENCY), Instant.now().plusSeconds(5))
                .orElseThrow();
        execute("""
                UPDATE safety_events
                   SET status='RESOLVED', resolved_at=now(),
                       alert_status='SUPPRESSED', alert_lease_expires_at=NULL
                 WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                """);
        AtomicInteger providerCalls = new AtomicInteger();

        var result = fence.execute(claim, () -> {
            providerCalls.incrementAndGet();
            return new FencedAlertDelivery(FcmDeliveryResult.success("unexpected", 1), true);
        });

        assertThat(result).isEmpty();
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void providerFencePastLeaseBlocksReclaimUntilSuccessSnapshotCommits() throws Exception {
        migrate(CANONICAL);
        seedOwnerAndIntakes();
        seedEmergencyAndDeliveryPrerequisites();
        DriverManagerDataSource dataSource = dataSource();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        EmergencyAlertAttemptRepository attempts =
                new EmergencyAlertAttemptRepository(template);
        EmergencyAlertDeliveryRepository deliveries =
                new EmergencyAlertDeliveryRepository(template);
        EmergencyAlertProviderFence fence = new EmergencyAlertProviderFence(
                new DataSourceTransactionManager(dataSource), attempts, 50);
        EmergencyAlertClaim claim = attempts.claim(
                java.util.UUID.fromString(EMERGENCY), Instant.now().plusSeconds(2))
                .orElseThrow();
        var intent = deliveries.insertIntent(
                claim, java.util.UUID.fromString(RECIPIENT),
                java.util.UUID.fromString(DEVICE), java.util.UUID.fromString(NOTIFICATION));
        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        AtomicInteger providerCalls = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var provider = executor.submit(() -> fence.execute(claim, () -> {
                providerCalls.incrementAndGet();
                providerEntered.countDown();
                await(releaseProvider);
                boolean recorded = deliveries.appendResult(
                        intent.actionId(), claim, true, 1, "fcm-fenced", null);
                return new FencedAlertDelivery(
                        FcmDeliveryResult.success("fcm-fenced", 1), recorded);
            }));
            assertThat(providerEntered.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(2200);
            var reclaimer = executor.submit(() -> attempts.claim(
                    java.util.UUID.fromString(EMERGENCY), Instant.now().plusSeconds(5)));
            assertThatThrownBy(() -> reclaimer.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseProvider.countDown();
            assertThat(provider.get(5, TimeUnit.SECONDS)).isPresent()
                    .get().extracting(FencedAlertDelivery::recorded).isEqualTo(true);
            assertThat(reclaimer.get(5, TimeUnit.SECONDS)).isPresent();
        }

        assertThat(deliveries.findSuccessful(
                java.util.UUID.fromString(EMERGENCY), java.util.UUID.fromString(DEVICE)))
                .isPresent();
        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void providerFenceMakesResolutionWaitForDeliveryResultCommit() throws Exception {
        migrate(CANONICAL);
        seedOwnerAndIntakes();
        seedEmergencyAndDeliveryPrerequisites();
        DriverManagerDataSource dataSource = dataSource();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        EmergencyAlertAttemptRepository attempts =
                new EmergencyAlertAttemptRepository(template);
        EmergencyAlertDeliveryRepository deliveries =
                new EmergencyAlertDeliveryRepository(template);
        EmergencyAlertProviderFence fence = new EmergencyAlertProviderFence(
                new DataSourceTransactionManager(dataSource), attempts, 5000);
        EmergencyAlertClaim claim = attempts.claim(
                java.util.UUID.fromString(EMERGENCY), Instant.now().plusSeconds(5))
                .orElseThrow();
        var intent = deliveries.insertIntent(
                claim, java.util.UUID.fromString(RECIPIENT),
                java.util.UUID.fromString(DEVICE), java.util.UUID.fromString(NOTIFICATION));
        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var provider = executor.submit(() -> fence.execute(claim, () -> {
                providerEntered.countDown();
                await(releaseProvider);
                boolean recorded = deliveries.appendResult(
                        intent.actionId(), claim, true, 1, "fcm-before-resolve", null);
                return new FencedAlertDelivery(
                        FcmDeliveryResult.success("fcm-before-resolve", 1), recorded);
            }));
            assertThat(providerEntered.await(5, TimeUnit.SECONDS)).isTrue();
            var resolver = executor.submit(this::resolveEmergency);
            assertThatThrownBy(() -> resolver.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseProvider.countDown();
            assertThat(provider.get(5, TimeUnit.SECONDS)).isPresent()
                    .get().extracting(FencedAlertDelivery::recorded).isEqualTo(true);
            assertThat(resolver.get(5, TimeUnit.SECONDS)).isOne();
        }

        assertThat(number("""
                SELECT count(*) FROM safety_event_actions
                 WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                   AND action_type='DELIVERY' AND action_phase='RESULT'
                   AND delivery_status='SENT'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM safety_events
                 WHERE safety_event_id='72000000-0000-0000-0000-000000000021'
                   AND status='RESOLVED'
                """)).isOne();
    }

    @Test
    void monitoringDisableAndIngestionSerializeOnTheSameSessionRow() throws Exception {
        migrate(CANONICAL);
        seedOwnerAndIntakes();
        String sessionOne = "72000000-0000-0000-0000-000000000031";
        String sessionTwo = "72000000-0000-0000-0000-000000000032";
        execute("""
                INSERT INTO safety_monitoring_sessions (
                    monitoring_session_id,user_id,status,sensitivity_level,started_at,created_by)
                VALUES ('72000000-0000-0000-0000-000000000031',
                        '72000000-0000-0000-0000-000000000001','ACTIVE','MEDIUM',now(),
                        '72000000-0000-0000-0000-000000000001')
                """);

        var executor = Executors.newSingleThreadExecutor();
        try (Connection ingestion = connection()) {
            ingestion.setAutoCommit(false);
            try (var lock = ingestion.prepareStatement("""
                    SELECT monitoring_session_id FROM safety_monitoring_sessions
                    WHERE monitoring_session_id=? AND status='ACTIVE' FOR UPDATE
                    """)) {
                lock.setObject(1, java.util.UUID.fromString(sessionOne));
                try (ResultSet result = lock.executeQuery()) {
                    assertThat(result.next()).isTrue();
                }
            }

            var disable = executor.submit(() -> updateSessionStatus(sessionOne, "STOPPED"));
            assertThatThrownBy(() -> disable.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            try (Statement statement = ingestion.createStatement()) {
                statement.execute("""
                        INSERT INTO safety_events (
                            safety_event_id,user_id,monitoring_session_id,detected_at,
                            event_type,status,record_type,created_at,updated_at)
                        VALUES (gen_random_uuid(),
                            '72000000-0000-0000-0000-000000000001',
                            '72000000-0000-0000-0000-000000000031',now(),
                            'SUSPECTED_FALL','OPEN','IMU_EVENT',now(),now())
                        """);
            }
            ingestion.commit();
            assertThat(disable.get(5, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(number("SELECT count(*) FROM safety_events WHERE monitoring_session_id='"
                + sessionOne + "' AND record_type='IMU_EVENT'"))
                .isOne();

        execute("""
                INSERT INTO safety_monitoring_sessions (
                    monitoring_session_id,user_id,status,sensitivity_level,started_at,created_by)
                VALUES ('72000000-0000-0000-0000-000000000032',
                        '72000000-0000-0000-0000-000000000001','ACTIVE','MEDIUM',now(),
                        '72000000-0000-0000-0000-000000000001')
                """);
        try (Connection disabling = connection()) {
            disabling.setAutoCommit(false);
            try (var update = disabling.prepareStatement("""
                    UPDATE safety_monitoring_sessions SET status='STOPPED',ended_at=now()
                    WHERE monitoring_session_id=? AND status='ACTIVE'
                    """)) {
                update.setObject(1, java.util.UUID.fromString(sessionTwo));
                assertThat(update.executeUpdate()).isOne();
            }
            var secondExecutor = Executors.newSingleThreadExecutor();
            try {
                var ingestion = secondExecutor.submit(() -> hasActiveSessionForUpdate(sessionTwo));
                assertThatThrownBy(() -> ingestion.get(200, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                disabling.commit();
                assertThat(ingestion.get(5, TimeUnit.SECONDS)).isFalse();
            } finally {
                secondExecutor.shutdownNow();
            }
        }
    }

    @Test
    void migrationReconcilesDuplicateActiveMonitoringAndEnforcesPartialUniqueness()
            throws Exception {
        migrate(BASELINE);
        seedOwnerAndIntakes();
        execute("""
                INSERT INTO safety_monitoring_sessions (
                    monitoring_session_id,user_id,status,sensitivity_level,
                    started_at,created_by)
                VALUES
                  ('72000000-0000-0000-0000-000000000031',
                   '72000000-0000-0000-0000-000000000001','ACTIVE','MEDIUM',
                   '2026-07-24T00:00:00Z',
                   '72000000-0000-0000-0000-000000000001'),
                  ('72000000-0000-0000-0000-000000000032',
                   '72000000-0000-0000-0000-000000000001','ACTIVE','HIGH',
                   '2026-07-24T00:01:00Z',
                   '72000000-0000-0000-0000-000000000001')
                """);

        migrate(CANONICAL);

        assertThat(number("""
                SELECT count(*) FROM safety_monitoring_sessions
                 WHERE monitoring_session_id =
                       '72000000-0000-0000-0000-000000000031'
                   AND status = 'ACTIVE'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM safety_monitoring_sessions
                 WHERE monitoring_session_id =
                       '72000000-0000-0000-0000-000000000032'
                   AND status = 'STOPPED'
                   AND ended_at = started_at
                """)).isOne();
        assertThatThrownBy(() -> execute("""
                INSERT INTO safety_monitoring_sessions (
                    monitoring_session_id,user_id,status,sensitivity_level,
                    started_at,created_by)
                VALUES (gen_random_uuid(),
                        '72000000-0000-0000-0000-000000000001','ACTIVE',
                        'MEDIUM',now(),
                        '72000000-0000-0000-0000-000000000001')
                """)).hasMessageContaining(
                        "safety_monitoring_sessions_one_active_user_uk");
    }

    private void seedLegacyStory66State() throws Exception {
        execute("""
                INSERT INTO users(
                    user_id,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES
                  ('72000000-0000-0000-0000-000000000001',
                   'legacy.safety.owner@test','MOTHER','ACTIVE',true,false,
                   true,false,now(),now())
                ON CONFLICT (user_id) DO NOTHING;
                INSERT INTO intake_sessions (
                    id,user_id,stage,symptoms,risk_level,status,created_at,
                    completed_at,created_by)
                VALUES
                  ('72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000001','PREGNANCY',
                   'legacy red one','RED','COMPLETED',now(),now(),
                   '72000000-0000-0000-0000-000000000001'),
                  ('72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000001','PREGNANCY',
                   'legacy red two','RED','COMPLETED',now(),now(),
                   '72000000-0000-0000-0000-000000000001');
                INSERT INTO emergency_sessions (
                    id,user_id,status,trigger_source,created_at,created_by)
                VALUES ('72000000-0000-0000-0000-000000000021',
                        '72000000-0000-0000-0000-000000000001','ACTIVE',
                        'AUTO_TRIAGE','2026-07-24T00:00:00Z',
                        '72000000-0000-0000-0000-000000000001');
                INSERT INTO triage_emergency_escalations (
                    intake_session_id,emergency_session_id,user_id,triggered_at)
                VALUES ('72000000-0000-0000-0000-000000000011',
                        '72000000-0000-0000-0000-000000000021',
                        '72000000-0000-0000-0000-000000000001',
                        '2026-07-24T00:10:00Z');
                INSERT INTO emergency_notification_outbox (
                    emergency_session_id,status,attempt_count,next_attempt_at,
                    last_error_code,claim_token,created_at,delivered_at,terminal_at)
                VALUES ('72000000-0000-0000-0000-000000000021','DELIVERED',2,
                        '2026-07-24T00:30:00Z',NULL,NULL,
                        '2026-07-24T00:00:00Z','2026-07-24T00:30:00Z',
                        '2026-07-24T00:30:00Z');
                """);
    }

    private void seedOwnerAndIntakes() throws Exception {
        execute("""
                INSERT INTO persons(person_id,display_name,created_at,updated_at)
                VALUES
                  ('72000000-0000-0000-0000-000000000001','Safety Owner',now(),now()),
                  ('72000000-0000-0000-0000-000000000002','Safety Recipient',now(),now())
                ON CONFLICT (person_id) DO NOTHING;
                INSERT INTO users(user_id,person_id,email,role,account_status,enabled,locked,
                                  email_verified,phone_verified,created_at,updated_at)
                VALUES
                  ('72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000001','safety.owner@test','MOTHER',
                   'ACTIVE',true,false,true,false,now(),now()),
                  ('72000000-0000-0000-0000-000000000002',
                   '72000000-0000-0000-0000-000000000002','safety.recipient@test','FAMILY',
                   'ACTIVE',true,false,true,false,now(),now())
                ON CONFLICT (user_id) DO NOTHING;
                INSERT INTO triage_sessions (
                    triage_session_id,user_id,stage,symptoms,risk_level,status,emergency,
                    created_at,completed_at,created_by)
                VALUES
                  ('72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000001','PREGNANCY','red one','RED',
                   'COMPLETED',true,now(),now(),
                   '72000000-0000-0000-0000-000000000001'),
                  ('72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000001','PREGNANCY','red two','RED',
                   'COMPLETED',true,now(),now(),
                   '72000000-0000-0000-0000-000000000001')
                ON CONFLICT (triage_session_id) DO NOTHING;
                """);
    }

    private void seedEmergencyAndDeliveryPrerequisites() throws Exception {
        execute("""
                INSERT INTO safety_events (
                    safety_event_id,user_id,detected_at,event_type,status,
                    record_type,created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000021',
                        '72000000-0000-0000-0000-000000000001',now(),
                        'MANUAL','ACTIVE','EMERGENCY_SESSION',now(),now());
                INSERT INTO device_tokens(
                    id,user_id,token,platform,active,created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000041',
                        '72000000-0000-0000-0000-000000000002',
                        'provider-fence-device','ANDROID',true,now(),now());
                INSERT INTO notification_records (
                    id,user_id,type,title,body,status,attempt_count,channel,
                    is_read,created_at,updated_at)
                VALUES ('72000000-0000-0000-0000-000000000051',
                        '72000000-0000-0000-0000-000000000002','EMERGENCY',
                        'Provider fence','Immutable delivery','PENDING',0,
                        'PUSH',false,now(),now());
                """);
    }

    private int resolveEmergency() throws Exception {
        try (Connection connection = connection();
             var update = connection.prepareStatement("""
                     UPDATE safety_events
                        SET status='RESOLVED', resolved_at=now(),
                            alert_status='SUPPRESSED',
                            alert_lease_expires_at=NULL, updated_at=now()
                      WHERE safety_event_id=? AND status='ACTIVE'
                     """)) {
            update.setObject(1, java.util.UUID.fromString(EMERGENCY));
            return update.executeUpdate();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for provider test barrier");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provider test barrier interrupted", exception);
        }
    }

    private int updateSessionStatus(String sessionId, String status) throws Exception {
        try (Connection connection = connection(); var update = connection.prepareStatement("""
                UPDATE safety_monitoring_sessions SET status=?,ended_at=now()
                WHERE monitoring_session_id=? AND status='ACTIVE'
                """)) {
            update.setString(1, status);
            update.setObject(2, java.util.UUID.fromString(sessionId));
            return update.executeUpdate();
        }
    }

    private boolean hasActiveSessionForUpdate(String sessionId) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (var query = connection.prepareStatement("""
                    SELECT 1 FROM safety_monitoring_sessions
                    WHERE monitoring_session_id=? AND status='ACTIVE' FOR UPDATE
                    """)) {
                query.setObject(1, java.util.UUID.fromString(sessionId));
                try (ResultSet result = query.executeQuery()) {
                    boolean active = result.next();
                    connection.commit();
                    return active;
                }
            }
        }
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource());
    }

    private DriverManagerDataSource dataSource() {
        return new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void migrate(MigrationVersion target) {
        migrate(target, false);
    }

    private void migrate(MigrationVersion target, boolean outOfOrder) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .outOfOrder(outOfOrder)
                .load()
                .migrate();
    }

    private void migrateHistoricalStory66Branch() throws IOException {
        Path legacy = copyMigrations(
                tempDirectory.resolve("legacy-through-story66-predecessor"),
                LEGACY_PRE_STORY66,
                Set.of(),
                true);
        migrate(legacy, LEGACY_PRE_STORY66, false);

        Path story66 = copyMigrations(
                tempDirectory.resolve("story66-branch"),
                null,
                STORY66_BRANCH_FILES,
                false);
        migrate(story66, PRE_STORY66_BRIDGE, false);
    }

    private Path copyMigrations(
            Path destination,
            MigrationVersion target,
            Set<String> names,
            boolean namesAreExclusions) throws IOException {
        Path source = migrationRoot();
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                String name = path.getFileName().toString();
                MigrationVersion version = migrationVersion(name);
                boolean named = names.contains(name);
                boolean include = namesAreExclusions
                        ? version != null && version.compareTo(target) <= 0 && !named
                        : named;
                if (!include) {
                    continue;
                }
                Path output = destination.resolve(source.relativize(path));
                Files.createDirectories(output.getParent());
                Files.copy(path, output, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return destination;
    }

    private MigrationVersion migrationVersion(String fileName) {
        if (!fileName.startsWith("V") || !fileName.endsWith(".sql")) {
            return null;
        }
        int separator = fileName.indexOf("__");
        if (separator < 2) {
            return null;
        }
        return MigrationVersion.fromVersion(
                fileName.substring(1, separator).replace('_', '.'));
    }

    private Path migrationRoot() {
        try {
            var resource = Thread.currentThread().getContextClassLoader().getResource("db/migration");
            if (resource == null || !"file".equals(resource.getProtocol())) {
                throw new IllegalStateException("exploded db/migration test resource is required");
            }
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("invalid migration resource path", exception);
        }
    }

    private void migrate(Path location, MigrationVersion target, boolean outOfOrder) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:" + location.toAbsolutePath().toString().replace('\\', '/'))
                .target(target)
                .outOfOrder(outOfOrder)
                .validateOnMigrate(false)
                .load()
                .migrate();
    }

    private long number(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
