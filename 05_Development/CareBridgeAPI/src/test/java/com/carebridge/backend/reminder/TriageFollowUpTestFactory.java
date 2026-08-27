package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * CB-TYFU-TDD-001 — Props Isolation Pattern (CASE 2.0, anti AP-AI-002).
 * Every test creates fresh instances through this factory; no shared mutable state.
 * All data SYNTHETIC — no real PII.
 */
final class TriageFollowUpTestFactory {

    static final UUID MOTHER_A  = UUID.fromString("00000000-0000-0000-0000-000000000001"); // FX-009
    static final UUID MOTHER_B  = UUID.fromString("00000000-0000-0000-0000-000000000002"); // FX-009
    static final UUID SESSION_1 = UUID.fromString("00000000-0000-0000-0000-0000000000a1"); // FX-009
    static final UUID JOURNEY_1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BABY_1    = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final Instant T0     = Instant.parse("2026-07-26T10:00:00Z");                   // FX-001

    private TriageFollowUpTestFactory() {
    }

    /** FX-001 — deterministic time for every TC. */
    static Clock fixedClock() {
        return Clock.fixed(T0, ZoneOffset.UTC);
    }

    /** FX-002 — fresh eventId per call (Logic Issue L3: eventId is never a dedupe key). */
    static IntakeSessionCompleted makeYellowEvent() {
        return new IntakeSessionCompleted(UUID.randomUUID(), SESSION_1, MOTHER_A,
                RiskLevel.YELLOW, T0);
    }

    /** FX-003/FX-008/FX-010 — riskLevel / completedAt variants over the same SESSION_1. */
    static IntakeSessionCompleted makeEvent(RiskLevel riskLevel, Instant completedAt) {
        return new IntakeSessionCompleted(UUID.randomUUID(), SESSION_1, MOTHER_A,
                riskLevel, completedAt);
    }

    /** FX-005 — committed YELLOW session; symptoms text matches canonical code "fever". */
    static IntakeSession makeYellowSession() {
        IntakeSession session = new IntakeSession();
        session.setId(SESSION_1);
        session.setUserId(MOTHER_A);
        session.setSymptoms("bé sốt 38.5 độ"); // SYNTHETIC — no real PII
        session.setRiskLevel(RiskLevel.YELLOW);
        session.setStatus(IntakeStatus.COMPLETED);
        session.setCreatedAt(T0.minusSeconds(600));
        session.setCompletedAt(T0);
        session.setCreatedBy(MOTHER_A);
        session.setJourneyId(JOURNEY_1);
        session.setBabyProfileId(BABY_1);
        return session;
    }

    /** Dedupe scenarios — a follow-up already persisted for SESSION_1. */
    static Reminder makeExistingFollowUp() {
        Reminder reminder = new Reminder();
        reminder.setOwnerUserId(MOTHER_A);
        reminder.setReminderType(ReminderType.TRIAGE_FOLLOW_UP);
        reminder.setSourceReferenceType("TRIAGE_SESSION");
        reminder.setSourceReferenceId(SESSION_1);
        reminder.setStatus(ReminderStatus.PENDING);
        return reminder;
    }
}
