package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (CB-TRIAGE-TEST-003 §4)
// Each @Test calls makeXxx() — never reuses instances across tests.
// All fixture data is SYNTHETIC; keywords deliberately absent from every
// hardcoded rule list (TriageRedFlagPolicy.FLOOR_KEYWORDS, SymptomNormalizer.KEYWORDS,
// PediatricRiskRules, app/risk_rules.py) — verified in Test-Spec TDS-05.
// ═══════════════════════════════════════════════════════════
class TriagePreScreenTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BABY_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    // FX-001 — RED + ESCALATE + active
    static RedFlagRule makeRedEscalateRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        rule.setKeyword("ngã đập đầu");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(false);
        return rule;
    }

    // Generic override-based factory — FX-002..FX-006 are expressed as overrides
    static RedFlagRule makeRule(Consumer<RedFlagRule> overrides) {
        RedFlagRule rule = makeRedEscalateRule();
        overrides.accept(rule);
        return rule;
    }

    // FX-011 — one-shot request whose only riskable content is the pre-screen keyword
    static RunIntakeRequest makeOneShotRequest() {
        RunIntakeRequest request = new RunIntakeRequest();
        request.setStage(TriageStage.INFANT);
        request.setBabyProfileId(BABY_PROFILE_ID);
        request.setSymptoms("bé bị nga dap dau xuống sàn");   // FX-008
        return request;
    }

    // Overload to override specific fields — never share mutated instances across tests
    static RunIntakeRequest makeOneShotRequest(Consumer<RunIntakeRequest> overrides) {
        RunIntakeRequest request = makeOneShotRequest();
        overrides.accept(request);
        return request;
    }

    static RunIntakeRequest makeNeutralOneShotRequest() {
        return makeOneShotRequest(r -> r.setSymptoms("bé hơi quấy khóc nhẹ")); // FX-010
    }

    // FX-012
    static StartIntakeConversationRequest makeStartRequest() {
        StartIntakeConversationRequest request = new StartIntakeConversationRequest();
        request.setInitialText("bé bị nga dap dau xuống sàn"); // FX-008
        request.setClientRequestId("trfp-cr-001");
        request.setStage(TriageStage.INFANT);
        request.setBabyProfileId(BABY_PROFILE_ID);
        return request;
    }

    static StartIntakeConversationRequest makeStartRequest(
            Consumer<StartIntakeConversationRequest> overrides) {
        StartIntakeConversationRequest request = makeStartRequest();
        overrides.accept(request);
        return request;
    }

    // Conversation session in NEED_MORE_INFO awaiting a parentFreeText answer (TC-014 harness)
    static IntakeSession makeNeedMoreInfoConversationSession() {
        return IntakeSession.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000201"))
                .userId(MOTHER_ID)
                .symptoms("CONVERSATION_INTAKE")
                .stage(TriageStage.INFANT)
                .babyProfileId(BABY_PROFILE_ID)
                .status(IntakeStatus.NEED_MORE_INFO)
                .createdAt(Instant.parse("2026-07-26T00:00:00Z"))
                .createdBy(MOTHER_ID)
                .rawAiResponse("""
                        {"status":"ASK_MORE","stage":"INFANT","mergedIntake":{"stage":"INFANT"},
                         "questions":[{"questionKey":"parentFreeText",
                                       "text":"Bạn hãy mô tả thêm dấu hiệu của bé.",
                                       "answerType":"TEXT","options":[]}],
                         "round":1}
                        """)
                .build();
    }

    // FX-013 — minimal valid non-RED AI payloads (recorded mock)
    static String makeAiGreenOneShotJson() {
        return """
               {"status":"COMPLETED","riskLevel":"GREEN","riskColor":"#22C55E",
                "emergencyActionRequired":false,"recommendationCode":"MONITOR_AT_HOME",
                "matchedRules":[],"redFlags":[],"disclaimer":"synthetic"}""";
    }

    static String makeAiAskMoreEnvelopeJson() {
        return """
               {"status":"ASK_MORE","mergedIntake":{},"round":1,
                "questions":[{"questionKey":"duration","text":"Bao lâu rồi?",
                              "answerType":"TEXT","options":[]}]}""";
    }

    // FX-015 — event recorder (fresh per test)
    static List<Object> makeEventSink() {
        return new ArrayList<>();
    }
}
