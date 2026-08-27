package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.MOTHER_A;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.MOTHER_B;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.SESSION_1;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.T0;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeYellowEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * CB-TYFU-TDD-001 — TYFU-TC-INT-01 (+ TYFU-TC-13 read-isolation part).
 * Real PostgreSQL (Testcontainers, Flyway canonical baseline B20260724111500);
 * INotificationService mocked (L4 — no real push delivery exists).
 * Persistence oracle: baseline :1587-1610.
 */
class TriageFollowUpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private INotificationService notificationService;

    private TransactionTemplate tx;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(transactionManager);
        when(notificationService.scheduleFcmPush(any(), anyString(), anyString(), any()))
                .thenReturn("fcm-int-1");
        // Canonical schema (V20260727010000): persons is folded into users;
        // users.person_id is NOT NULL UNIQUE and always equals user_id.
        jdbcTemplate.update(
                "INSERT INTO users (user_id, created_at, updated_at, person_id) "
                        + "VALUES (?, now(), now(), ?) ON CONFLICT DO NOTHING",
                MOTHER_A, MOTHER_A);
        // FX-005 — committed YELLOW session (journey/baby omitted: FK-heavy, not asserted here).
        // Raw SQL, not intakeSessionRepository.save(): IntakeSession has a @GeneratedValue id,
        // so save() with the pre-assigned SESSION_1 takes Hibernate's detached-merge path and
        // throws StaleObjectStateException when the row does not exist yet. ON CONFLICT keeps
        // re-seeding idempotent (COMPLETED snapshots are immutable and cannot be deleted).
        // The canonical BEFORE INSERT trigger (V20260724211000) backfills result_jsonb /
        // schema_version / content_hash for COMPLETED rows.
        jdbcTemplate.update(
                "INSERT INTO triage_sessions (triage_session_id, user_id, status, risk_level, "
                        + "symptoms, emergency, created_at, completed_at, created_by, stage) "
                        + "VALUES (?, ?, 'COMPLETED', 'YELLOW', ?, false, ?, ?, ?, 'INFANT') "
                        + "ON CONFLICT (triage_session_id) DO NOTHING",
                SESSION_1, MOTHER_A, "bé sốt 38.5 độ", // SYNTHETIC — no real PII
                java.sql.Timestamp.from(T0.minusSeconds(600)), java.sql.Timestamp.from(T0),
                MOTHER_A);
    }

    @AfterEach
    void cleanUp() {
        // Canonical schema: scheduled_care_items -> care_tasks discriminated by
        // task_type = 'SCHEDULED_REMINDER' (Reminder entity @SQLRestriction).
        jdbcTemplate.update(
                "DELETE FROM care_tasks WHERE owner_user_id = ? AND task_type = 'SCHEDULED_REMINDER'",
                MOTHER_A);
        // audit_events is append-only (IMMUTABLE_TABLE trigger) and COMPLETED triage_sessions
        // are delete-protected on real PostgreSQL — neither is deleted. The audit assertion is
        // keyed by the per-run reminder id, so leftover rows never interfere.
    }

    @Test
    void tyfuTcInt01_publishCommit_createsOneRowAndAudit_duplicatePublishStillOneRow() {
        // 1) publish inside a committed transaction → AFTER_COMMIT handler fires (ADR-TYFU-002)
        tx.executeWithoutResult(status -> eventPublisher.publishEvent(makeYellowEvent()));

        // 2) duplicate publish — fresh eventId, same SESSION_1 (Logic Issue L3 / BR-TYFU-002)
        tx.executeWithoutResult(status -> eventPublisher.publishEvent(makeYellowEvent()));

        List<Reminder> rows = reminderRepository.findByOwnerUserIdOrderByScheduledAtDesc(MOTHER_A)
                .stream()
                .filter(r -> r.getReminderType() == ReminderType.TRIAGE_FOLLOW_UP)
                .toList();
        assertThat(rows).hasSize(1); // dedupe (ADR-TYFU-003)
        Reminder saved = rows.getFirst();
        assertThat(saved.getSourceReferenceId()).isEqualTo(SESSION_1);
        assertThat(saved.getSourceReferenceType()).isEqualTo("TRIAGE_SESSION");
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(saved.getScheduledAt())
                .isEqualTo(Instant.parse("2026-07-26T14:00:00Z")); // completedAt + 4h (ADR-TYFU-005)
        assertThat(saved.getFcmJobId()).isEqualTo("fcm-int-1");    // ADR-TYFU-004

        // resource_id is uuid in the canonical baseline — bind the UUID directly
        // (a String parameter fails on PostgreSQL: no uuid = varchar operator).
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_category = 'REMINDER_CREATED' "
                        + "AND resource_id = ?", Integer.class, saved.getId());
        assertThat(auditCount).isEqualTo(1); // AuditAction.REMINDER_CREATED, exactly once
    }

    @Test
    void tyfuTc13_readIsolation_otherMotherCannotSeeTheFollowUp() {
        tx.executeWithoutResult(status -> eventPublisher.publishEvent(makeYellowEvent()));

        UUID careItemId = reminderRepository.findByOwnerUserIdOrderByScheduledAtDesc(MOTHER_A)
                .stream()
                .filter(r -> r.getReminderType() == ReminderType.TRIAGE_FOLLOW_UP)
                .findFirst().orElseThrow().getId();

        // Existing ownership contract (findByIdAndOwnerUserId → 404 at HTTP level)
        assertThat(reminderRepository.findByIdAndOwnerUserId(careItemId, MOTHER_B)).isEmpty();
        assertThat(reminderRepository.findByIdAndOwnerUserId(careItemId, MOTHER_A)).isPresent();
    }
}
