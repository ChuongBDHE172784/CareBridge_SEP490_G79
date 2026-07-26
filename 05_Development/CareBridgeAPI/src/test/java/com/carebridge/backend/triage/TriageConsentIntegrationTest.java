package com.carebridge.backend.triage;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.entity.TriageDisclaimerConsent;
import com.carebridge.backend.triage.repository.TriageDisclaimerConsentRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.ITriageConsentService;
import com.carebridge.backend.triage.service.ITriageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.carebridge.backend.triage.TriageConsentTestFactory.V1;
import static com.carebridge.backend.triage.TriageConsentTestFactory.makeAcceptRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CB-TRIAGE-CONSENT-IMP-001-TEST — end-to-end over real PostgreSQL (Testcontainers,
 * Flyway canonical baseline {@code B20260724111500}) covering TDC-TC-06/07/08/16/17/18/INT-01.
 *
 * <p>NOT {@code @Transactional}: TDC-TC-17 runs parallel accepts in independent transactions
 * (pg_advisory_xact_lock only exists on real PostgreSQL — ADR-TDC-004); each test cleans up its
 * own rows in {@link #cleanUp()} instead.
 */
@TestPropertySource(properties = {
        "carebridge.triage.disclaimer.version=AI_TRIAGE_DISCLAIMER_V1",
        "carebridge.triage.disclaimer.text=SYNTHETIC DISCLAIMER TEXT V1"
})
class TriageConsentIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ITriageConsentService consentService;
    @Autowired private ITriageService triageService;
    @Autowired private TriageDisclaimerConsentRepository consentRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    // Same full-context stubbing pattern as TriageIntegrationTest / sibling triage INT tests.
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private GeminiTriageClient geminiTriageClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;
    // TDC-TC-18 observes the emergency escalation boundary (EmergencyEscalationHandler is a
    // synchronous @EventListener calling openOrReuseFromTriage) — same pattern as
    // TriageRedFlagPreScreenIntegrationTest.
    @MockitoBean private IEmergencyService emergencyService;

    private User mother;
    private User mother2;
    private String motherJwt;
    private String mother2Jwt;

    private static final String GREEN_ONE_SHOT = """
            {"status":"COMPLETED","riskLevel":"GREEN","emergencyActionRequired":false,
             "matchedRules":["GREEN_MILD_NO_RED_FLAGS"],"recommendationCode":"MONITOR_AT_HOME",
             "disclaimer":"AI guidance only — SYNTHETIC","summary":"SYNTHETIC mild"}
            """;

    private static final String ASK_MORE_START = """
            {"status":"ASK_MORE","mergedIntake":{},"assistantMessage":"Need age",
             "questions":[{"questionKey":"childAgeMonths","text":"Bé bao nhiêu tháng tuổi?",
                           "answerType":"NUMBER","options":[]}],"round":1}
            """;

    private static final String RED_COMPLETE_CONTINUE = """
            {"status":"TRIAGE_COMPLETE","mergedIntake":{"childAgeMonths":12},"round":2,
             "assistantMessage":"Emergency guidance — SYNTHETIC",
             "triageResult":{"status":"COMPLETED","riskLevel":"RED","emergencyActionRequired":true,
                             "recommendationCode":"SEEK_EMERGENCY_CARE",
                             "matchedRules":["SYNTHETIC_RED_RULE"],"redFlags":["synthetic"],
                             "summary":"SYNTHETIC emergency","disclaimer":"AI guidance only — SYNTHETIC",
                             "citations":[],"claims":[],"evidenceIds":[]}}
            """;

    @BeforeEach
    void seedUsers() {
        // Unique emails per test: users are intentionally never deleted (see cleanUp note),
        // so fixed addresses would collide with the users unique-email constraint.
        String unique = UUID.randomUUID().toString();
        mother = saveUser("tdc.mother1+" + unique + "@test.local", Role.MOTHER);
        mother2 = saveUser("tdc.mother2+" + unique + "@test.local", Role.MOTHER);
        motherJwt = jwtTokenProvider.generateAccessToken(mother);
        mother2Jwt = jwtTokenProvider.generateAccessToken(mother2);
    }

    @AfterEach
    void cleanUp() {
        // Only the consent rows are cleaned: tc16/tcInt01 run GLOBAL queries over
        // permission_kind = 'AI_TRIAGE_DISCLAIMER'. Everything else is left in place because
        // the canonical schema forbids it on real PostgreSQL: audit_events is append-only
        // (IMMUTABLE_TABLE trigger) and COMPLETED triage_sessions are delete-protected
        // (triage_completed_snapshot_guard_trg, V20260724211000); users stay because audit
        // rows reference them and every test seeds fresh unique users anyway. All remaining
        // assertions are keyed by per-test user/session/permission ids.
        jdbcTemplate.update(
                "DELETE FROM data_permissions WHERE permission_kind = 'AI_TRIAGE_DISCLAIMER'");
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .role(role)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
    }

    /** FX-007 — seed an effective ACTIVE consent row for the given user (current version V1). */
    private TriageDisclaimerConsent seedActiveConsent(UUID userId) {
        TriageDisclaimerConsent consent = new TriageDisclaimerConsent();
        consent.setPermissionId(UUID.randomUUID());
        consent.setOwnerUserId(userId);
        consent.setScopeType("TRIAGE");
        consent.setScopeText("ELECTIVE_AI_TRIAGE_INTAKE_ONLY");
        consent.setPurpose("AI_TRIAGE_GUIDANCE");
        consent.setPolicyVersion(V1);
        consent.setStatus("ACTIVE");
        consent.setGrantedAt(Instant.now());
        consent.setPermissionSeriesId(consent.getPermissionId());
        consent.setVersionNumber(1);
        consent.setLocale("vi");
        consent.setConsentEvidenceKey("synthetic-evidence-key");
        return consentRepository.save(consent);
    }

    private long countSessions(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions WHERE user_id = ?", Long.class, userId);
    }

    private long countActiveConsents(UUID userId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM data_permissions
                WHERE owner_user_id = ? AND permission_kind = 'AI_TRIAGE_DISCLAIMER'
                  AND policy_version = ? AND status = 'ACTIVE'
                """, Long.class, userId, V1);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-06 — Gate: runIntake without effective consent → 409, no session row
    // Oracle: TDS §9.2 gated-endpoint schema + §10 / ADR-TDC-002 (L3 flat error shape)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc06_runIntake_withoutConsent_shouldReturn409_andCreateNoSessionRow() throws Exception {
        mockMvc.perform(post("/api/v1/triage/intake")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Đau đầu nhẹ — SYNTHETIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("TRIAGE_CONSENT_REQUIRED"));

        assertThat(countSessions(mother.getId())).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-07 — Gate: startConversation without effective consent → 409
    // Oracle: TDS §9.1 (both elective entries gated) / BR-TDC-004
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc07_startConversation_withoutConsent_shouldReturn409_andCreateNoSessionRow() throws Exception {
        mockMvc.perform(post("/api/v1/triage/intake/conversation/start")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialText\":\"Sốt nhẹ — SYNTHETIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIAGE_CONSENT_REQUIRED"));

        assertThat(countSessions(mother.getId())).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-08 — Gate passes with effective consent; intake proceeds unchanged
    // Oracle: BR-TDC-001 / existing UC60 contract (201 + sessionId)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc08_runIntake_withEffectiveConsent_shouldProceedUnchanged() throws Exception {
        seedActiveConsent(mother.getId());
        when(childTriageAiClient.triageChild(any())).thenReturn(GREEN_ONE_SHOT);
        when(childTriageAiClient.triageChild(any(), any())).thenReturn(GREEN_ONE_SHOT);

        mockMvc.perform(post("/api/v1/triage/intake")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Đau đầu nhẹ — SYNTHETIC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").exists());

        assertThat(countSessions(mother.getId())).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-16 — Ownership from JWT only; body cannot designate another owner (CWE-639)
    // Oracle: BR-TDC-006 / AcceptTriageConsentRequest schema (no owner field by design)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc16_ownership_bodyOwnerFieldsIgnored_rowsKeyedToJwtUser() throws Exception {
        // Attack simulation: MOTHER2 accepts, smuggling MOTHER's id as extra JSON fields.
        mockMvc.perform(post("/api/v1/triage/consent/accept")
                        .header("Authorization", "Bearer " + mother2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\",\"locale\":\"vi\","
                                + "\"ownerUserId\":\"" + mother.getId() + "\","
                                + "\"userId\":\"" + mother.getId() + "\"}"))
                .andExpect(status().isCreated());

        UUID owner = jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM data_permissions WHERE permission_kind = 'AI_TRIAGE_DISCLAIMER'",
                UUID.class);
        assertThat(owner).isEqualTo(mother2.getId());   // never MOTHER's id

        // B's acceptance did not leak to A.
        mockMvc.perform(get("/api/v1/triage/consent")
                        .header("Authorization", "Bearer " + motherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUIRED"));

        mockMvc.perform(post("/api/v1/triage/intake")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"SYNTHETIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIAGE_CONSENT_REQUIRED"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-17 — Concurrency: N parallel accepts → exactly one ACTIVE row
    // Oracle: ADR-TDC-004 / NFR §4.2 uniqueness invariant (real advisory lock — no mocks)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc17_parallelAccepts_shouldCollapseToSingleActiveRow() throws Exception {
        int concurrency = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch release = new CountDownLatch(1);
        List<Future<TriageConsentAcceptOutcome>> futures = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            futures.add(executor.submit(() -> {
                release.await(10, TimeUnit.SECONDS);
                return consentService.accept(makeAcceptRequest(), mother.getId());
            }));
        }
        release.countDown();
        List<TriageConsentAcceptOutcome> outcomes = new ArrayList<>();
        for (Future<TriageConsentAcceptOutcome> future : futures) {
            outcomes.add(future.get(30, TimeUnit.SECONDS));    // zero exceptions expected
        }
        executor.shutdownNow();

        assertThat(countActiveConsents(mother.getId())).isEqualTo(1L);
        assertThat(outcomes.stream().filter(TriageConsentAcceptOutcome::created).count()).isEqualTo(1L);
        assertThat(outcomes.stream().filter(o -> !o.created()).count()).isEqualTo(concurrency - 1L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-18 — BR-SAFETY: mid-flight revocation never blocks continueConversation
    // or RED escalation; only the NEXT elective entry is gated.  ★ Release blocker
    // Oracle: BR-TDC-004 / TDS §6.2 note / roadmap Part II.5 (RED → escalation)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc18_midFlightRevocation_neverBlocksContinueOrRedEscalation() throws Exception {
        seedActiveConsent(mother.getId());
        when(childTriageAiClient.startIntake(any())).thenReturn(ASK_MORE_START);
        when(childTriageAiClient.continueIntake(any())).thenReturn(RED_COMPLETE_CONTINUE);

        // Conversation started while consent was ACTIVE. clientRequestId routes session
        // creation through the database-arbitrated native-insert path (the idempotent path
        // real clients use). KNOWN LATENT PRODUCTION BUG (out of scope for this safety TC,
        // reported separately): without clientRequestId, TriageService.startConversation
        // builds the IntakeSession with a pre-assigned id on a @GeneratedValue entity and
        // calls repository.save(), which Hibernate treats as a detached merge and throws
        // StaleObjectStateException on real PostgreSQL.
        IntakeConversationResponse started = triageService.startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("Sốt — SYNTHETIC")
                        .clientRequestId(UUID.randomUUID().toString())
                        .build(),
                mother.getId());
        assertThat(started.getStatus()).isEqualTo("ASK_MORE");
        UUID sessionId = UUID.fromString(started.getIntakeSessionId());

        // 1) Revoke mid-flight.
        mockMvc.perform(post("/api/v1/triage/consent/revoke")
                        .header("Authorization", "Bearer " + motherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUIRED"))
                .andExpect(jsonPath("$.data.reason").value("REVOKED"));

        // 2) + 3) Continue the in-flight session with a RED result → 200, fully processed.
        mockMvc.perform(post("/api/v1/triage/intake/conversation/continue")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intakeSessionId\":\"" + sessionId + "\","
                                + "\"newAnswers\":{\"childAgeMonths\":12}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRIAGE_COMPLETE"));

        // 4) RED outcome still triggered the emergency escalation boundary.
        verify(emergencyService).openOrReuseFromTriage(sessionId, mother.getId());

        // 5) The NEXT elective entry is gated again.
        mockMvc.perform(post("/api/v1/triage/intake")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"SYNTHETIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIAGE_CONSENT_REQUIRED"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-INT-01 — Full flow: accept → gate passes → session stamped
    // Oracle: TDS §6.1 happy path / ADR-TDC-003 / baseline DDL (disclaimer_version varchar(80))
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tcInt01_fullFlow_acceptGateSessionStamp_auditOnce() throws Exception {
        when(childTriageAiClient.triageChild(any())).thenReturn(GREEN_ONE_SHOT);
        when(childTriageAiClient.triageChild(any(), any())).thenReturn(GREEN_ONE_SHOT);

        // 1) status REQUIRED
        mockMvc.perform(get("/api/v1/triage/consent")
                        .header("Authorization", "Bearer " + motherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUIRED"))
                .andExpect(jsonPath("$.data.reason").value("NOT_ACCEPTED"))
                .andExpect(jsonPath("$.data.disclaimerText").value("SYNTHETIC DISCLAIMER TEXT V1"));

        // 2) accept → 201
        mockMvc.perform(post("/api/v1/triage/consent/accept")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\",\"locale\":\"vi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 3) idempotent repeat → 200, still exactly 1 row
        mockMvc.perform(post("/api/v1/triage/consent/accept")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\",\"locale\":\"vi\"}"))
                .andExpect(status().isOk());
        assertThat(countActiveConsents(mother.getId())).isEqualTo(1L);

        // 4) gated intake now passes → 201 + sessionId
        String body = mockMvc.perform(post("/api/v1/triage/intake")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Đau đầu nhẹ — SYNTHETIC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andReturn().getResponse().getContentAsString();
        String sessionId = com.jayway.jsonpath.JsonPath.read(body, "$.data.sessionId");

        // 5) exact data_permissions column values (raw SQL — proves real column names)
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT permission_kind, scope_type, scope_text, purpose, policy_version, status,
                       expires_at, granted_at, revoked_at, revoked_by, version_number, locale,
                       supersedes_permission_id, permission_series_id, consent_evidence_key,
                       grantee_user_id, scope_reference_id, recipient, evidence_key, permission_id
                FROM data_permissions
                WHERE owner_user_id = ? AND permission_kind = 'AI_TRIAGE_DISCLAIMER'
                """, mother.getId());
        assertThat(row.get("permission_kind")).isEqualTo("AI_TRIAGE_DISCLAIMER");
        assertThat(row.get("scope_type")).isEqualTo("TRIAGE");
        assertThat(row.get("scope_text")).isEqualTo("ELECTIVE_AI_TRIAGE_INTAKE_ONLY");
        assertThat(row.get("purpose")).isEqualTo("AI_TRIAGE_GUIDANCE");
        assertThat(row.get("policy_version")).isEqualTo("AI_TRIAGE_DISCLAIMER_V1");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        assertThat(row.get("expires_at")).isNull();
        assertThat(row.get("granted_at")).isNotNull();
        assertThat(row.get("revoked_at")).isNull();
        assertThat(row.get("revoked_by")).isNull();
        assertThat(row.get("version_number")).isEqualTo(1);
        assertThat(row.get("locale")).isEqualTo("vi");
        assertThat(row.get("supersedes_permission_id")).isNull();
        assertThat(row.get("permission_series_id")).isNotNull();
        assertThat(row.get("grantee_user_id")).isNull();
        assertThat(row.get("scope_reference_id")).isNull();
        assertThat(row.get("recipient")).isNull();
        assertThat(row.get("evidence_key")).isNull();

        // 6) session stamped with the disclaimer version + text populated (UC60 behaviour)
        String stamped = jdbcTemplate.queryForObject(
                "SELECT disclaimer_version FROM triage_sessions WHERE triage_session_id = ?::uuid",
                String.class, sessionId);
        assertThat(stamped).isEqualTo("AI_TRIAGE_DISCLAIMER_V1");
        String disclaimerText = jdbcTemplate.queryForObject(
                "SELECT disclaimer_text FROM triage_sessions WHERE triage_session_id = ?::uuid",
                String.class, sessionId);
        assertThat(disclaimerText).isNotBlank();

        // 7) audit: exactly 1 CONSENT_GRANTED entry for the consent row
        UUID permissionId = (UUID) row.get("permission_id");
        assertThat(auditLogRepository.findByEntityIdAndAction(permissionId, AuditAction.CONSENT_GRANTED))
                .hasSize(1);
    }
}
