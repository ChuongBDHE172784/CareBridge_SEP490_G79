package com.carebridge.backend.triage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.HealthMemoryWriteHandler;
import com.carebridge.backend.triage.service.ITriageService;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.journey.service.ILifecycleSafetyOutcomeProjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.BABY_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_A;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeAiOneShotGreenJson;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeRunIntakeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — THMC-TC-INT-01 / THMC-TC-INT-02.
 * Testcontainers PostgreSQL (Flyway canonical baseline); ChildTriageAiClient mocked —
 * no live Gemini/Python service. Persistence oracle: baseline :999-1012.
 */
class TriageHealthMemoryContextIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID PERSON_A = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    @Autowired private ITriageService triageService;
    @Autowired private HealthMemoryEntryRepository healthMemoryEntryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private GeminiTriageClient geminiTriageClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;
    @MockitoBean private IEmergencyService emergencyService;
    @MockitoBean private ILifecycleSafetyOutcomeProjector lifecycleSafetyOutcomeProjector;
    // §6-sanctioned consent fixture (same as TriageIntegrationTest): the sibling
    // TriageDisclaimerConsent gate is out of scope here — the default mock's
    // ensureActiveConsent() is a no-op, letting elective intake proceed.
    @MockitoBean private com.carebridge.backend.triage.service.ITriageConsentService triageConsentService;

    @MockitoSpyBean private HealthMemoryEntryRepository healthMemoryEntryRepositorySpy;

    private final List<List<HealthMemoryContextItem>> recordedContexts = new ArrayList<>();

    @BeforeEach
    void seedOwner() {
        jdbcTemplate.update(
                "INSERT INTO persons (person_id) VALUES (?) ON CONFLICT DO NOTHING", PERSON_A);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, created_at, updated_at, person_id) "
                        + "VALUES (?, now(), now(), ?) ON CONFLICT DO NOTHING",
                USER_A, PERSON_A);
        recordedContexts.clear();
        // BR-THMC-004: an empty/absent context keeps the pre-feature ONE-ARG call
        // byte-for-byte; only a non-empty server-loaded context switches to the
        // two-arg overload. Record both dispatches so the context assertions see
        // exactly what the service passed (empty list for the one-arg path).
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class)))
                .thenAnswer(invocation -> {
                    recordedContexts.add(List.of());
                    return makeAiOneShotGreenJson();
                });
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class), anyList()))
                .thenAnswer(invocation -> {
                    recordedContexts.add(List.copyOf(invocation.getArgument(1)));
                    return makeAiOneShotGreenJson();
                });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM health_context_memories WHERE user_id = ?", USER_A);
        jdbcTemplate.update("DELETE FROM triage_session_evidence WHERE triage_session_id IN "
                + "(SELECT triage_session_id FROM triage_sessions WHERE user_id = ?)", USER_A);
        // NOTE: triage_sessions rows are intentionally NOT deleted — the canonical
        // triage_completed_snapshot_guard_trg trigger (V20260724211000) makes COMPLETED
        // snapshots immutable on real PostgreSQL (DELETE raises). Assertions in this class
        // are keyed by per-run generated session ids, so leftover rows are harmless.
    }

    @Test
    void thmcTcInt01_fullLoop_completedIntakeWritesMemory_nextIntakeInjectsIt() {
        // Intake #1 — the service method commits its own transaction, so AFTER_COMMIT fires
        IntakeSessionResponse first = triageService.runIntake(makeRunIntakeRequest(), USER_A);
        assertThat(first.getStatus()).isEqualTo("COMPLETED");
        UUID session1Id = first.getSessionId();

        // DB oracle (baseline :999-1012): exactly 1 active memory row for session #1
        List<HealthMemoryEntry> rows = healthMemoryEntryRepository.findAll().stream()
                .filter(e -> session1Id.equals(e.getSourceSessionId()))
                .toList();
        assertThat(rows).hasSize(1);
        HealthMemoryEntry row = rows.get(0);
        assertThat(row.getUserId()).isEqualTo(USER_A);
        assertThat(row.getBabyProfileId()).isEqualTo(BABY_1);
        assertThat(row.getRelatedStage()).isEqualTo(TriageStage.INFANT);
        assertThat(row.getDeletedAt()).isNull();
        String schemaVersion = jdbcTemplate.queryForObject(
                "SELECT memory_payload_jsonb ->> 'schemaVersion' FROM health_context_memories "
                        + "WHERE triage_session_id = ?", String.class, session1Id);
        assertThat(schemaVersion).isEqualTo("1.0");
        // TTL correctness: expires_at = completed_at + 30 days (§14.1 oracle query)
        Boolean ttlOk = jdbcTemplate.queryForObject(
                "SELECT (m.expires_at = s.completed_at + INTERVAL '30 days') "
                        + "FROM health_context_memories m "
                        + "JOIN triage_sessions s ON s.triage_session_id = m.triage_session_id "
                        + "WHERE m.triage_session_id = ?", Boolean.class, session1Id);
        assertThat(ttlOk).isTrue();
        assertThat(row.getExpiresAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(row.getExpiresAt()); // timestamptz round-trip sanity

        // Intake #1 saw no prior context; intake #2 receives the memory written by #1
        assertThat(recordedContexts.get(0)).isEmpty();
        triageService.runIntake(makeRunIntakeRequest(), USER_A);
        assertThat(recordedContexts.get(1))
                .extracting(HealthMemoryContextItem::summaryText)
                .anyMatch(summary -> summary.equals(row.getSummaryText()));

        // Strict `expires_at > now` boundary re-check at SQL level (repository oracle :19)
        jdbcTemplate.update(
                "UPDATE health_context_memories SET expires_at = now() - interval '1 second' "
                        + "WHERE user_id = ?", USER_A);
        triageService.runIntake(makeRunIntakeRequest(), USER_A);
        assertThat(recordedContexts.get(2)).isEmpty();
    }

    @Test
    void thmcTcInt02_afterCommitIsolation_memorySaveFailureDoesNotRollBackSession() {
        doThrow(new RuntimeException("SYNTHETIC save failure"))
                .when(healthMemoryEntryRepositorySpy).save(any(HealthMemoryEntry.class));
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger handlerLogger = (Logger) LoggerFactory.getLogger(HealthMemoryWriteHandler.class);
        handlerLogger.addAppender(appender);

        IntakeSessionResponse response;
        try {
            response = assertDoesNotThrow(
                    () -> triageService.runIntake(makeRunIntakeRequest(), USER_A));
        } finally {
            handlerLogger.detachAppender(appender);
        }

        // No exception surfaced; session commit survived (ADR-THMC-001 / BR-SAFETY)
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM triage_sessions WHERE triage_session_id = ?",
                String.class, response.getSessionId());
        assertThat(status).isEqualTo("COMPLETED");
        Boolean completedAtPresent = jdbcTemplate.queryForObject(
                "SELECT completed_at IS NOT NULL FROM triage_sessions WHERE triage_session_id = ?",
                Boolean.class, response.getSessionId());
        assertThat(completedAtPresent).isTrue();
        // Write genuinely failed (not skipped-by-guard): zero memory rows for this session
        Integer memoryCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM health_context_memories WHERE triage_session_id = ?",
                Integer.class, response.getSessionId());
        assertThat(memoryCount).isZero();
        // Failure observable in logs only (catch-and-log, no PII)
        assertThat(appender.list).anyMatch(event -> event.getLevel() == Level.WARN);
    }
}
