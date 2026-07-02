package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.integration.gemini.filter.TriageRedFlagSafetyFilter;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.response.RedFlagRuleResponse;
import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.RedFlagRuleService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// RFR-TC-INT-001
// Note: this codebase has no Testcontainers/real-Postgres integration harness (verified project-wide —
// same finding already documented in ModerateContentIntegrationTest); the test datasource is H2 in
// PostgreSQL mode with Flyway disabled and Hibernate ddl-auto=create-drop
// (src/test/resources/application.yaml), so no Flyway-seeded rows exist in this context. This test
// verifies the actual contract under test — read-through, no cache (ADR-004): create a rule through the
// real Spring-managed service, then confirm TriageRedFlagSafetyFilter sees it on the very next call,
// with no restart/cache-bust step — using real beans end-to-end (matches TriageIntegrationTest pattern).
@SpringBootTest
@Transactional
class RedFlagRuleIntegrationTest {

    @Autowired
    private RedFlagRuleService redFlagRuleService;

    @Autowired
    private RedFlagRuleRepository redFlagRuleRepository;

    @Autowired
    private TriageRedFlagSafetyFilter triageRedFlagSafetyFilter;

    // Unrelated to this test's subject — GmailEmailService requires a JavaMailSender bean not
    // configured in the test datasource profile; mocking here matches TriageIntegrationTest's
    // established pattern for avoiding that dependency chain during full-context @SpringBootTest.
    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    private static final UUID SYSTEM_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void createRule_thenCheckSafetyFilter_detectsRuleOnNextCallWithoutRestart() {
        CreateRedFlagRuleRequest request = new CreateRedFlagRuleRequest(
                "tự làm đau bản thân", RedFlagSeverity.RED, RedFlagAction.ESCALATE);

        RedFlagRuleResponse created = redFlagRuleService.createRule(request, SYSTEM_ADMIN_ID);

        RagSafetyResult result = triageRedFlagSafetyFilter.check("tôi có suy nghĩ tự làm đau bản thân");

        assertThat(result.isRedFlag()).isTrue();
        assertThat(result.getEmergencyGuidance()).isNotBlank();

        RedFlagRule persisted = redFlagRuleRepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getSeverity()).isEqualTo(RedFlagSeverity.RED);
        assertThat(persisted.isActive()).isTrue();
        assertThat(persisted.isSystemDefault()).isFalse();
    }
}
