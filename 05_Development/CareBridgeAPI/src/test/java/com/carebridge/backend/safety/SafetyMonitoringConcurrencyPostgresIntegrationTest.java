package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Execution(ExecutionMode.SAME_THREAD)
class SafetyMonitoringConcurrencyPostgresIntegrationTest
        extends AbstractPostgresIntegrationTest {

    private static final UUID OWNER =
            UUID.fromString("72600000-0000-0000-0000-000000000001");

    @Autowired private IFallDetectionService fallDetectionService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private SafetyConsentPolicy consentPolicy;
    @MockitoBean private AuditService auditService;

    @BeforeEach
    void seedOwnerAndConfig() {
        cleanFixtures();
        jdbcTemplate.update("""
                INSERT INTO users(
                    user_id,person_id,display_name,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES (?, ?, 'Concurrent Safety Owner', 'monitoring.concurrent@test',
                        'MOTHER', 'ACTIVE', true, false, true, false, now(), now())
                """, OWNER, OWNER);
        jdbcTemplate.update("""
                INSERT INTO safety_configs(
                    safety_config_id,user_id,fall_detection_enabled,
                    sensitivity_level,emergency_auto_alert,countdown_seconds,
                    sensor_permission_granted,sensor_permission_recorded_at,
                    updated_at,updated_by)
                VALUES (gen_random_uuid(), ?, true, 'MEDIUM', true, 30,
                        true, now(), now(), ?)
                """, OWNER, OWNER);
    }

    @AfterEach
    void cleanUp() {
        cleanFixtures();
    }

    @Test
    void concurrentEnableReturnsOneCanonicalActiveSession() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<ImuMonitoringSessionResponse> responses = new ArrayList<>();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> enableAfterBarrier(ready, start));
            var second = executor.submit(() -> enableAfterBarrier(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            responses.add(first.get(30, TimeUnit.SECONDS));
            responses.add(second.get(30, TimeUnit.SECONDS));
        }

        assertThat(responses)
                .extracting(ImuMonitoringSessionResponse::getSessionId)
                .containsOnly(responses.getFirst().getSessionId());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_monitoring_sessions
                 WHERE user_id = ? AND status = 'ACTIVE'
                """, Long.class, OWNER)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_monitoring_sessions
                 WHERE user_id = ?
                """, Long.class, OWNER)).isOne();
    }

    private ImuMonitoringSessionResponse enableAfterBarrier(
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        return fallDetectionService.enable(OWNER, "MEDIUM");
    }

    private void cleanFixtures() {
        jdbcTemplate.update(
                "DELETE FROM safety_monitoring_sessions WHERE user_id = ?", OWNER);
        jdbcTemplate.update("DELETE FROM safety_configs WHERE user_id = ?", OWNER);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", OWNER);
    }
}
