package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.service.HealthMemoryProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (CB-TRIAGE-THMC-IMP-001-TEST §4)
// Each @Test calls makeXxx() — never reuses instances across tests.
// All fixture data is SYNTHETIC (FX-THMC-001…013).
// ═══════════════════════════════════════════════════════════
class HealthMemoryContextTestFactory {

    static final UUID USER_A   = UUID.fromString("00000000-0000-0000-0000-0000000000a1"); // FX-THMC-001
    static final UUID USER_B   = UUID.fromString("00000000-0000-0000-0000-0000000000b2"); // FX-THMC-002
    static final UUID BABY_1   = UUID.fromString("00000000-0000-0000-0000-00000000c001"); // FX-THMC-003
    static final UUID BABY_2   = UUID.fromString("00000000-0000-0000-0000-00000000c002"); // FX-THMC-003
    static final UUID MOTHER_1 = UUID.fromString("00000000-0000-0000-0000-00000000d001"); // FX-THMC-004
    static final UUID SESSION_1 = UUID.fromString("00000000-0000-0000-0000-00000000e001");
    static final Instant NOW_FIXED     = Instant.parse("2026-07-26T10:00:00Z");           // FX-THMC-012
    static final Instant COMPLETED_AT  = Instant.parse("2026-07-26T08:00:00Z");           // FX-THMC-005
    static final String RAW_MARKER     = "RAW_FREETEXT_MARKER_7f3a";                      // FX-THMC-005

    /** FX-THMC-005 — COMPLETED INFANT session, risk YELLOW, canonical result snapshot present. */
    static IntakeSession makeCompletedSession() {
        return IntakeSession.builder()
                .id(SESSION_1)
                .userId(USER_A)
                .babyProfileId(BABY_1)
                .stage(TriageStage.INFANT)
                .status(IntakeStatus.COMPLETED)
                .riskLevel(RiskLevel.YELLOW)
                .symptoms("{\"hasFreeText\":true,\"parentFreeTextSnapshot\":\"" + RAW_MARKER + "\"}")
                .rawAiResponse("{\"status\":\"COMPLETED\",\"riskLevel\":\"YELLOW\","
                        + "\"normalizedSymptoms\":[\"fever\",\"cough\"],"
                        + "\"recommendationCode\":\"CONTACT_HEALTHCARE_PROVIDER\","
                        + "\"fallbackUsed\":false,"
                        + "\"parentFreeText\":\"" + RAW_MARKER + "\"}")
                .createdAt(COMPLETED_AT.minusSeconds(120))
                .completedAt(COMPLETED_AT)
                .createdBy(USER_A)
                .build();
    }

    static IntakeSession makeCompletedSession(Consumer<IntakeSession> overrides) {
        IntakeSession s = makeCompletedSession();
        overrides.accept(s);
        return s;
    }

    /** FX-THMC-006 — active memory for (USER_A, BABY_1, INFANT). */
    static HealthMemoryEntry makeActiveMemory() {
        return HealthMemoryEntry.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-00000000f001"))
                .userId(USER_A)
                .babyProfileId(BABY_1)
                .relatedStage(TriageStage.INFANT)
                .summaryText("SYNTHETIC prior triage: risk YELLOW; fever, cough")
                .sourceSessionId(SESSION_1)
                .createdAt(Instant.parse("2026-07-20T09:00:00Z"))
                .expiresAt(Instant.parse("2026-08-19T09:00:00Z"))
                .build();
    }

    static HealthMemoryEntry makeActiveMemory(Consumer<HealthMemoryEntry> overrides) {
        HealthMemoryEntry e = makeActiveMemory();
        overrides.accept(e);
        return e;
    }

    /** Context item matching makeActiveMemory() (FX-THMC-006). */
    static HealthMemoryContextItem makeContextItem() {
        HealthMemoryEntry entry = makeActiveMemory();
        return new HealthMemoryContextItem(
                entry.getSummaryText(),
                entry.getRelatedStage().name(),
                entry.getCreatedAt(),
                entry.getExpiresAt());
    }

    /** FX-THMC-005-event — completion event matching makeCompletedSession(). */
    static IntakeSessionCompleted makeCompletedEvent() {
        return new IntakeSessionCompleted(
                UUID.fromString("00000000-0000-0000-0000-00000000ee01"),
                SESSION_1, USER_A, RiskLevel.YELLOW, COMPLETED_AT);
    }

    /** One-shot request for (USER_A, BABY_1, INFANT) — public contract fields only. */
    static RunIntakeRequest makeRunIntakeRequest() {
        return RunIntakeRequest.builder()
                .stage(TriageStage.INFANT)
                .babyProfileId(BABY_1)
                .symptomList(List.of("ho", "sốt nhẹ"))
                .childAgeMonths(7)
                .build();
    }

    static StartIntakeConversationRequest makeStartConversationRequest() {
        StartIntakeConversationRequest r = new StartIntakeConversationRequest();
        r.setStage(TriageStage.INFANT);
        r.setBabyProfileId(BABY_1);
        r.setInitialText("SYNTHETIC initial text");
        return r;
    }

    /** FX-THMC-009 — fresh properties per test; override for TTL/bounding variants. */
    static HealthMemoryProperties makeProperties() {
        HealthMemoryProperties p = new HealthMemoryProperties();
        p.setTtlDays(30);
        p.setMaxContextEntries(5);
        p.setMaxSummaryChars(500);
        return p;
    }

    static HealthMemoryProperties makeProperties(Consumer<HealthMemoryProperties> overrides) {
        HealthMemoryProperties p = makeProperties();
        overrides.accept(p);
        return p;
    }

    /** FX-THMC-010 — valid canonical one-shot AI response body. */
    static String makeAiOneShotGreenJson() {
        return "{\"status\":\"COMPLETED\",\"riskLevel\":\"GREEN\",\"stage\":\"INFANT\","
                + "\"disclaimer\":\"SYNTHETIC disclaimer\"}";
    }

    /** Valid ASK_MORE start envelope (one renderable INFANT question, round 1). */
    static String makeAiAskMoreEnvelopeJson() {
        return """
               {"status":"ASK_MORE","mergedIntake":{},"round":1,
                "questions":[{"questionKey":"duration","text":"Bao lâu rồi?",
                              "answerType":"TEXT","options":[]}]}""";
    }
}
