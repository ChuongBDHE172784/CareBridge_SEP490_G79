package com.carebridge.backend.triage;

import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.ITriageConsentService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CB-TRIAGE-FDBB-IMP-001-TEST — TFBF-TC-INT-01 / TFBF-TC-INT-02 (D3).
 *
 * <p>End-to-end over real PostgreSQL (Testcontainers, real Flyway chain) through the real
 * controller/service/repository stack. External boundaries mocked exactly like the sibling
 * triage INT suites: {@code ChildTriageAiClient} (no Python/Gemini call) and the
 * §6-sanctioned {@code ITriageConsentService} no-op mock (consent gate covered by
 * {@code TriageConsentIntegrationTest}, out of scope here).
 *
 * <p>TFBF-TC-INT-01 pins the endpoint contract (200 envelope) for the NO-clientRequestId
 * path, which pre-fix dies with Hibernate {@code StaleObjectStateException} (pre-assigned id
 * on a {@code @GeneratedValue} entity → detached merge). TFBF-TC-INT-02 pins the
 * DB-arbitrated idempotent path that must remain byte-identical (BR-FDBB-006 / C5).
 */
class TriageConversationStartIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // Same full-context stubbing pattern as TriageConsentIntegrationTest.
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private GeminiTriageClient geminiTriageClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;
    @MockitoBean private IEmergencyService emergencyService;
    // FX-TFBF-002: default mock's ensureActiveConsent() is a no-op → elective intake proceeds.
    @MockitoBean private ITriageConsentService triageConsentService;

    private User mother;
    private String motherJwt;

    /** FX-TFBF-003 — SYNTHETIC ASK_MORE start envelope (sibling ASK_MORE_START shape). */
    private static final String ASK_MORE_START = """
            {"status":"ASK_MORE","mergedIntake":{},"assistantMessage":"Need age",
             "questions":[{"questionKey":"childAgeMonths","text":"Bé bao nhiêu tháng tuổi?",
                           "answerType":"NUMBER","options":[]}],"round":1}
            """;

    @BeforeEach
    void seedMother() {
        // Unique email per test: users are intentionally never deleted (audit/session FKs);
        // triage_sessions rows are keyed per-user so leftovers are harmless.
        mother = userRepository.save(User.builder()
                .email("tfbf.mother+" + UUID.randomUUID() + "@test.local")
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        motherJwt = jwtTokenProvider.generateAccessToken(mother);
        when(childTriageAiClient.startIntake(any())).thenReturn(ASK_MORE_START);
    }

    private long countSessions(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions WHERE user_id = ?", Long.class, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TFBF-TC-INT-01 — start WITHOUT clientRequestId → 200 envelope + persisted row
    // Oracle: IntakeController :61-68 (ResponseEntity.ok + ApiResponse.success) / BR-FDBB-006
    // Pre-fix: 500 — StaleObjectStateException (merge of pre-assigned @GeneratedValue id)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tfbfTcInt01_startWithoutClientRequestId_shouldReturn200AndPersistSession() throws Exception {
        String body = mockMvc.perform(post("/api/v1/triage/intake/conversation/start")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialText\":\"Bé sốt nhẹ — SYNTHETIC\",\"stage\":\"INFANT\",\"currentIntake\":{\"stage\":\"INFANT\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASK_MORE"))
                .andExpect(jsonPath("$.data.intakeSessionId").exists())
                .andReturn().getResponse().getContentAsString();
        String sessionId = com.jayway.jsonpath.JsonPath.read(body, "$.data.intakeSessionId");
        UUID persistedId = UUID.fromString(sessionId); // must be the DB-generated UUID

        assertThat(countSessions(mother.getId())).isEqualTo(1);
        String clientRequestId = jdbcTemplate.queryForObject(
                "SELECT client_request_id FROM triage_sessions WHERE triage_session_id = ?",
                String.class, persistedId);
        assertThat(clientRequestId).isNull();
        UUID rowUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM triage_sessions WHERE triage_session_id = ?",
                UUID.class, persistedId);
        assertThat(rowUserId).isEqualTo(mother.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TFBF-TC-INT-02 — start WITH clientRequestId stays idempotent (regression pin, C5)
    // Oracle: IntakeSessionWriter :20-54 + TriageService replay contract :339-354
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tfbfTcInt02_startWithClientRequestId_replayReturnsSameSessionSingleRow() throws Exception {
        String clientRequestId = UUID.randomUUID().toString(); // FX-TFBF-004
        String request = "{\"initialText\":\"Bé sốt nhẹ — SYNTHETIC\","
                + "\"stage\":\"INFANT\",\"currentIntake\":{\"stage\":\"INFANT\"},"
                + "\"clientRequestId\":\"" + clientRequestId + "\"}";

        String first = mockMvc.perform(post("/api/v1/triage/intake/conversation/start")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASK_MORE"))
                .andReturn().getResponse().getContentAsString();
        String firstSessionId = com.jayway.jsonpath.JsonPath.read(first, "$.data.intakeSessionId");

        String second = mockMvc.perform(post("/api/v1/triage/intake/conversation/start")
                        .header("Authorization", "Bearer " + motherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASK_MORE"))
                .andReturn().getResponse().getContentAsString();
        String secondSessionId = com.jayway.jsonpath.JsonPath.read(second, "$.data.intakeSessionId");

        assertThat(secondSessionId).isEqualTo(firstSessionId);
        Long rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions WHERE user_id = ? AND client_request_id = ?",
                Long.class, mother.getId(), clientRequestId);
        assertThat(rows).isEqualTo(1);
    }
}
