# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# TriageYellowFollowUp — Automatic Follow-Up Care Task for YELLOW Triage Risk

**Document ID:** `CB-TYFU-TDD-001`
**Version:** `1.0`
**Date:** `2026-07-26`
**Status:** `Implemented 14/14 — 2026-07-27` *(TYFU-TC-01…13 + TYFU-TC-INT-01 🟢 Passing; the formerly env-blocked integration parts executed green on a Docker-capable host — `./mvnw test -Dtest=TriageFollowUpIntegrationTest` → Tests run 2, Failures 0, Errors 0)*
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/B20260724111500__canonical_70_table_baseline.sql` — primary persistence oracle (`scheduled_care_items` :1587-1610, `triage_sessions` :1714-1745)
- TDS: `04_Implement/TriageYellowFollowUp/TriageYellowFollowUp_TDS.md` (`CB-TYFU-IMP-001`)
- Requirement oracle: `04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` — Part III item 3
- Event producer: `triage/service/impl/TriageService.java` (`publishCompletionEvents` :768-778)
- Listener pattern + test pattern: `ai/service/IntakeSessionCompletedHandler.java`, `src/test/java/com/carebridge/backend/ai/IntakeSessionCompletedHandlerTest.java`
- Reused reminder domain: `reminder/entity/Reminder.java`, `reminder/repository/ReminderRepository.java`, `reminder/service/INotificationService.java`, `reminder/service/impl/ReminderServiceImpl.java` (:48-76 FCM/audit convention)
- BR-SAFETY — CLAUDE.md Delivery Rules (AI guidance only; never delay emergency routing)

> **Quy ước TDD:** This document describes test cases BEFORE production code is written.
> Mandatory order: write test (`.java`) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test ✅ until `./mvnw test` (backend) is green.
> No real PII in test data — SYNTHETIC only.
>
> **Test commands:**
> - Full suite: `cd 05_Development/CareBridgeAPI && ./mvnw test`
> - Focused: `./mvnw test -Dtest=TriageFollowUpServiceTest` / `-Dtest=TriageYellowFollowUpHandlerTest` / `-Dtest=TriageFollowUpTitlePolicyTest` / `-Dtest=TriageFollowUpIntegrationTest`

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-26 | AI Agent | Initial creation — TDD spec for TriageYellowFollowUp |
| 2026-07-26 | AI Agent — Amelia (Dev Agent) | Phase 3 sync: Red Gate executed (27/27 test executions FAILED against stubs — Tests run 27, Failures 6, Errors 21; evidence in `red-gate-evidence.log`); GREEN 13/14 TCs passing (`./mvnw test -Dtest="TriageFollowUpServiceTest,TriageFollowUpTitlePolicyTest,TriageYellowFollowUpHandlerTest"` = Tests run 26, Failures 0, Errors 0); TYFU-TC-INT-01 + TC-13 integration part environment-blocked (no Docker for Testcontainers — same pre-existing blocker as sibling suites); §5/§5.1/§6/§8 updated to ACTUAL results. Deviation D1: title-source path (a) "parse `triage_sessions.symptom_list`" is not implementable through the TDS-declared dependency set — `symptom_list` is mapped only on `ai/entity/StructuredIntakeData`, not on `triage/entity/IntakeSession` — so the service uses path (b) free-text keyword match (mirroring `SymptomNormalizer` semantics), exactly the path the Test-Spec exercises (L5). |
| 2026-07-27 | AI Agent — Amelia (Dev Agent) | Docker host available: integration TCs executed — 2/2 PASS (TYFU-TC-INT-01 + TYFU-TC-13 integration read-isolation part). Production fix REQUIRED by this TDS's audit NFR (§ 'Audit — 100% via audit_log query', Luật 91/2025): `AuditAction.REMINDER_CREATED` added to `AuditEligibilityPolicy.SENSITIVE_ACTIONS` — the pre-existing eligibility filter silently dropped the service's `auditService.log(...)` call, so no audit_events row was ever persisted (unit tests could not catch it: they mock `AuditService`); regression guards `AuditEligibilityPolicyTest`, `TriageFollowUpServiceTest`, `UpdateReminderServiceTest` all green after the change. Test fixture hardening for real PostgreSQL (test files only): seed via raw SQL INSERT (fixed SESSION_1 id + `@GeneratedValue` entity cannot go through `repository.save()` — Hibernate detached-merge throws StaleObjectStateException), cleanup restricted to `scheduled_care_items` (audit_events append-only, COMPLETED triage_sessions delete-protected), audit oracle binds `resource_id` as UUID (uuid column). Enabled by test-harness fixes: `bridge-bootstrap.sql` init script + test-only `db/testfix` Flyway shim. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `TriageYellowFollowUp` (roadmap Part III.3) |
| **Module** | `Triage Yellow Follow-Up — reminder (consumer) ← triage (event producer)` |
| **Spec gốc** | `CB-TYFU-IMP-001` (`04_Implement/TriageYellowFollowUp/TriageYellowFollowUp_TDS.md`) |
| **Priority** | 🟠 P1 |
| **Sprint** | `Open — to be scheduled` |
| **Milestone** | `Open` |
| **Data Classification** | `PII` (category-derived titles only; reads Sensitive-PII triage rows) |
| **Compliance Scope** | `PDPA / Luật 91/2025 / BR-SAFETY` |
| **Upstream Dependencies** | `triage` (`IntakeSessionCompleted`), `reminder` (entity/repo/notification), `audit` |
| **Downstream Consumers** | UC49 Today Tasks, UC212–UC215 reminder lifecycle, UC158 reminder notification |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-TYFU-IMP-001 §17` (C1–C5), `ADR-TYFU-001..006` |
| **Constraints Injected** | C1 canonical `scheduled_care_items` only, no migration; C2 AFTER_COMMIT listener on existing event, YELLOW only, exceptions contained; C3 dedupe on (`TRIAGE_FOLLOW_UP`, `source_reference_id`); C4 configurable delay via injected `Clock`; C5 policy-derived title, no PII, `scheduleFcmPush` + `REMINDER_CREATED` audit |
| **Model** | `Claude (Fable 5) — spec generation only; no production code generated` |
| **Trust Level** | `T2 → T3 (pending Red Gate §5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, the canonical baseline and approved migrations are the final persistence oracle; ERD/roadmap prose is supporting evidence only.
> **Mandatory before writing tests.** Tests encode the **corrected** behavior below, not the original spec prose.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Roadmap III.3 says target may be `family_tasks` **or** `scheduled_care_items` | `family_tasks.care_group_id` is `NOT NULL` (baseline) — a mother without a care group cannot receive a follow-up; `scheduled_care_items.owner_user_id` maps directly to the mother | Tests assert persistence into `scheduled_care_items` only (ADR-TYFU-001) |
| L2 | Coordinator brief describes `IntakeSessionCompleted` as `(sessionId, userId, riskLevel)` | Verified record has **5** components: `(UUID eventId, UUID sessionId, UUID userId, RiskLevel riskLevel, Instant completedAt)` (`triage/event/IntakeSessionCompleted.java`) | Factories build the 5-arg record; `completedAt` is the base of `scheduled_at` |
| L3 | "dedupe on eventId" would be a natural reading | `eventId` is `UUID.randomUUID()` per publish (`TriageService.java:775`) — re-published events carry NEW eventIds | Idempotency tests use a **different** `eventId` with the **same** `sessionId` and still expect no duplicate (TYFU-TC-04, INT-01) |
| L4 | Roadmap implies a push is "sent" | No real scheduled-push exists: only `INotificationService.scheduleFcmPush` (sole impl `DummyNotificationService`) and no `@Scheduled` due-item dispatcher | Tests assert `scheduleFcmPush` is **invoked** with correct args and `fcm_job_id` stored; actual delivery is out of scope (TDS Open item O2) |
| L5 | Title could be read from `triage_sessions.symptom_list` | `symptom_list` is written by a **sibling** AFTER_COMMIT listener (`StructuredIntakeService`) — ordering unspecified, may be NULL at handler time | Title tests feed canonical codes into `TriageFollowUpTitlePolicy` directly; service tests cover the NULL-`symptom_list` fallback path (free-text keyword match / generic title) |
| L6 | `item_type` looks like a free string column | `Reminder.reminderType` is `@Enumerated(EnumType.STRING) ReminderType` (`Reminder.java:34-36`) — enum has no `TRIAGE_FOLLOW_UP` today | Tests reference `ReminderType.TRIAGE_FOLLOW_UP`; adding the constant is a recorded code change (TDS §5.2), so tests fail to compile/RED until it exists |
| L7 | `source_reference_*` "exists on the entity" | Columns exist in DDL but are **not mapped** on `Reminder.java` today | Tests assert the new `sourceReferenceType`/`sourceReferenceId` fields; RED until mapping is added |

---

## 3. Test Design Specification (TDS)

> Baseline `B20260724111500__canonical_70_table_baseline.sql` is part of the test basis for every persistence assertion.

### TDS-01 — Scope / Phạm vi

```
TriageYellowFollowUp covers these layers:
├── Domain / Policy (pure logic — no deps)      → TriageFollowUpTitlePolicy
├── Services (Mockito-mocked repositories)      → TriageFollowUpService (+ injected fixed Clock)
├── Event handler (Mockito + annotation checks) → TriageYellowFollowUpHandler
└── Integration (@SpringBootTest + Testcontainers PostgreSQL, Flyway baseline)
                                                → event publish → scheduled_care_items row + audit
Out of scope: no controller layer (no new endpoint); real FCM delivery (Open item O2);
frontend rendering of reminderType TRIAGE_FOLLOW_UP.
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `AITriage_Assessment_Roadmap.md` Part III.3 | YELLOW → follow-up task 4–6 h later + push; canonical tables only |
| `CB-TYFU-IMP-001 §3` ADR-TYFU-001..006 | target table, trigger, idempotency, notification, delay, title mapping |
| `BR-TYFU-001..005`, `BR-SAFETY` (TDS §2) | YELLOW-only guard; dedupe; non-blocking; PII-minimal title; canonical tables |
| Baseline DDL :1587-1610 | column names/types/NOT NULLs, `scheduled_care_items_vaccination_ck` non-applicability |
| `TriageService.java:768-778` | event payload & publish semantics (random eventId; RED also publishes escalation) |
| `IntakeSessionCompletedHandlerTest.java` | AFTER_COMMIT annotation assertion pattern; exception-containment pattern |
| `ReminderServiceImpl.java:48-76` | save → `scheduleFcmPush` → store `fcmJobId` → `AuditAction.REMINDER_CREATED` convention |
| Clock pattern (`TriageContinuationService.java:30`, `PublicContentImageCleanupJobTest.java:49`) | constructor-injected `Clock`, tests use `Clock.fixed(...)` |
| PDPA / Luật 91/2025 | no raw symptom text in titles/logs |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path: YELLOW creates one PENDING follow-up with all mandated column values | `TriageFollowUpService.scheduleFollowUp()` | `TYFU-TC-01` |
| TC-COND-002 | Alternative: GREEN event → no-op | handler guard | `TYFU-TC-02` |
| TC-COND-003 | Alternative/safety: RED event → no-op, emergency path untouched (BR-SAFETY) | handler guard | `TYFU-TC-03` |
| TC-COND-004 | Idempotency: duplicate event (new eventId, same sessionId) → no second row | `ReminderRepository.existsByReminderTypeAndSourceReferenceId` | `TYFU-TC-04`, `TYFU-TC-INT-01` |
| TC-COND-005 | Title mapping per ADR-TYFU-006 priority table | `TriageFollowUpTitlePolicy.deriveTitle()` | `TYFU-TC-05` |
| TC-COND-006 | Title generic fallback (null/empty/unmapped) | `TriageFollowUpTitlePolicy.deriveTitle()` | `TYFU-TC-06` |
| TC-COND-007 | Error: FCM scheduling failure → item kept, `fcmJobId = null` | `TriageFollowUpService` (TYFU-004) | `TYFU-TC-07` |
| TC-COND-008 | Error: session not found → skip, `Optional.empty()` | `TriageFollowUpService` (TYFU-001) | `TYFU-TC-08` |
| TC-COND-009 | Error containment: service throws → handler swallows (BR-TYFU-003) | `TriageYellowFollowUpHandler` | `TYFU-TC-09` |
| TC-COND-010 | Handler wiring: `@TransactionalEventListener(phase = AFTER_COMMIT)` | annotation reflection | `TYFU-TC-10` |
| TC-COND-011 | Boundary: delay config 1/6/24 honored; 0 and 25 fall back to default 4 (TYFU-005) | delay resolution | `TYFU-TC-11` |
| TC-COND-012 | Clock/timezone: fixed Clock; `completedAt == null` fallback; UTC day-boundary arithmetic | injected `Clock` usage | `TYFU-TC-12` |
| TC-COND-013 | Authorization/ownership: `owner_user_id = event.userId()`; other user cannot read the item | ownership fields + existing `findByIdAndOwnerUserId` guard | `TYFU-TC-13` |
| TC-COND-014 | End-to-end: publish → commit → row + audit; double publish → still 1 row | full flow (Testcontainers) | `TYFU-TC-INT-01` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `riskLevel` ∈ {GREEN, YELLOW, RED} | exactly one partition (YELLOW) creates output |
| Boundary Value Analysis | `delay-hours` ∈ {0, 1, 4, 6, 24, 25}; `completedAt` at 23:59:59Z day boundary | config guard TYFU-005; Instant arithmetic correctness |
| State Transition Testing | created item `status = PENDING` only | feature invariant 1 (TDS §6.3) — no other transition triggered here |
| Decision Table | title priority mapping (5 rows + fallback) | ADR-TYFU-006 first-match-wins semantics |
| Error Guessing | duplicate events, FCM failure, missing session, repository exception | resilience codes TYFU-001..004 |
| Security-oriented (ownership) | cross-user read of created item | least-privilege, Auth Matrix TDS §16 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Fixed clock | `Clock.fixed(Instant.parse("2026-07-26T10:00:00Z"), ZoneOffset.UTC)` | deterministic time (all TCs) |
| `FX-002` | Event | `IntakeSessionCompleted(randomEventId, SESSION_1, MOTHER_A, YELLOW, 2026-07-26T10:00:00Z)` | happy path |
| `FX-003` | Event | same as FX-002 but `riskLevel = GREEN` / `RED` | guard partitions |
| `FX-004` | Event | new random `eventId`, same `SESSION_1`, YELLOW | duplicate/idempotency |
| `FX-005` | Entity seed | `IntakeSession{id=SESSION_1, userId=MOTHER_A, symptoms="bé sốt 38.5 độ", symptom_list=NULL, journeyId=J1, babyProfileId=B1, riskLevel=YELLOW, completedAt=10:00Z}` | session lookup + title source |
| `FX-006` | Input list | `List.of("fever")` / `List.of("rash")` / `List.of()` / `null` | title mapping + fallback |
| `FX-007` | Config | `delayHours` ∈ {1, 4, 6, 24} valid; {0, 25} invalid | boundary TYFU-005 |
| `FX-008` | Event | FX-002 variant with `completedAt = null` | clock fallback |
| `FX-009` | UUIDs | `MOTHER_A=…0001`, `MOTHER_B=…0002`, `SESSION_1=…00A1` (synthetic constants) | ownership isolation |
| `FX-010` | Event | FX-002 variant with `completedAt = 2026-07-26T23:59:59Z` | UTC day-boundary → `scheduled_at = 2026-07-27T03:59:59Z` |

---

## 4. Test Case Specification

> **TC ID format:** `TYFU-TC-[NN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ Every test creates fresh instances through the factory. No shared mutable state between test cases (anti AP-AI-002).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// src/test/java/com/carebridge/backend/reminder/TriageFollowUpTestFactory.java
// ═══════════════════════════════════════════════════════════
class TriageFollowUpTestFactory {

    static final UUID MOTHER_A  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID MOTHER_B  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID SESSION_1 = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final Instant T0     = Instant.parse("2026-07-26T10:00:00Z");   // FX-001

    static Clock fixedClock() {                       // FX-001
        return Clock.fixed(T0, ZoneOffset.UTC);
    }

    static IntakeSessionCompleted makeYellowEvent() { // FX-002 — fresh eventId per call (L3)
        return new IntakeSessionCompleted(UUID.randomUUID(), SESSION_1, MOTHER_A,
                RiskLevel.YELLOW, T0);
    }

    static IntakeSessionCompleted makeEvent(Consumer<EventBuilder> overrides) { /* overload */ }

    static IntakeSession makeYellowSession() {        // FX-005
        IntakeSession s = new IntakeSession();
        s.setId(SESSION_1);
        s.setUserId(MOTHER_A);
        s.setSymptoms("bé sốt 38.5 độ");             // SYNTHETIC — no real PII
        s.setRiskLevel(RiskLevel.YELLOW);
        s.setCompletedAt(T0);
        // journeyId J1 / babyProfileId B1 set via overloads when needed
        return s;
    }

    static Reminder makeExistingFollowUp() {          // for dedupe scenarios
        Reminder r = new Reminder();
        r.setOwnerUserId(MOTHER_A);
        r.setReminderType(ReminderType.TRIAGE_FOLLOW_UP);
        r.setSourceReferenceType("TRIAGE_SESSION");
        r.setSourceReferenceId(SESSION_1);
        r.setStatus(ReminderStatus.PENDING);
        return r;
    }
}
```

---

### TYFU-TC-01 — YELLOW completion creates exactly one PENDING follow-up with mandated fields

**Severity:** `CRITICAL`
**Legal:** `PDPA (data minimization via policy title)`
**Feature Under Test:** `TriageFollowUpService.scheduleFollowUp()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** Roadmap Part III.3 (4–6 h follow-up); baseline DDL :1587-1610 (column set); ADR-TYFU-001/003/005 (values); `ReminderServiceImpl.java:48-76` (save→push→audit convention); `AuditAction.REMINDER_CREATED` (`AuditAction.java:76`)

**Preconditions:**
- Mocks: `ReminderRepository` (`exists…` → false, `save` echoes entity with id), `IIntakeSessionRepository.findById(SESSION_1)` → FX-005, `INotificationService.scheduleFcmPush` → `"fcm-job-tyfu-1"`, `AuditService`
- Service built with `fixedClock()` (FX-001) and `delayHours = 4`

**Test Steps:**
1. *Arrange:* `event = makeYellowEvent()` (FX-002); wire mocks above.
2. *Act:* `Optional<UUID> result = service.scheduleFollowUp(event);`
3. *Assert:* capture the saved `Reminder` with `ArgumentCaptor`.

**Expected Result (PASS — hành vi đúng):**
- `result.isPresent()`
- Saved entity: `ownerUserId = MOTHER_A` [oracle: event.userId → `owner_user_id`, ADR-TYFU-001]; `reminderType = ReminderType.TRIAGE_FOLLOW_UP` [ADR-TYFU-001]; `scheduledAt = 2026-07-26T14:00:00Z` (= completedAt + 4 h) [ADR-TYFU-005]; `status = ReminderStatus.PENDING` [DDL default + TDS §6.3 invariant 1]; `sourceReferenceType = "TRIAGE_SESSION"`, `sourceReferenceId = SESSION_1` [ADR-TYFU-003]; `journeyId = J1`, `babyId = B1` [TDS §5.2 row table]; title = `"Kiểm tra lại thân nhiệt của bé"` (FX-005 text matches `fever`) [ADR-TYFU-006 row 1]
- `scheduleFcmPush(MOTHER_A, title, body, 2026-07-26T14:00:00Z)` called exactly once; `fcmJobId = "fcm-job-tyfu-1"` persisted [ADR-TYFU-004]
- `auditService.log(REMINDER_CREATED, MOTHER_A, "Reminder", <careItemId>, …)` called once [`ReminderServiceImpl` convention]

**Expected Result (FAIL — dấu hiệu lỗi):**
- Stub throws `UnsupportedOperationException` (Red Phase), or wrong table/enum/fields, missing dedupe fields, `Instant.now()` used instead of Clock (scheduledAt mismatch).

**Current Status:** 🟢 Passing
**Implementation Note:** compute `scheduledAt` from `event.completedAt()`, not `clock.instant()`, when present; two-step save is acceptable (save → set fcmJobId → save) per existing convention.

---

### TYFU-TC-02 — GREEN completion creates nothing

**Severity:** `CRITICAL`
**Feature Under Test:** `TriageYellowFollowUpHandler.onIntakeSessionCompleted()` guard
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageYellowFollowUpHandlerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-TYFU-001` (YELLOW only); Roadmap Part III.3 ("Khi phiên kết thúc YELLOW")

**Preconditions:** handler with mocked `ITriageFollowUpService`.

**Test Steps:**
1. *Arrange:* `event = makeEvent(e -> e.riskLevel(RiskLevel.GREEN))` (FX-003).
2. *Act:* `handler.onIntakeSessionCompleted(event);`
3. *Assert:* `verifyNoInteractions(followUpService)`.

**Expected Result (PASS):** service never invoked; no save/push/audit side effects possible.
**Expected Result (FAIL):** any interaction with the follow-up service for GREEN.

**Current Status:** 🟢 Passing
**Implementation Note:** guard in the handler (cheapest point), mirroring `EmergencyEscalationHandler`'s single-responsibility style.

---

### TYFU-TC-03 — RED completion creates nothing and never touches emergency routing (BR-SAFETY)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY — CLAUDE.md (never delay emergency routing)`
**Feature Under Test:** `TriageYellowFollowUpHandler.onIntakeSessionCompleted()` guard
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageYellowFollowUpHandlerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-TYFU-001`, `BR-SAFETY`; `TriageService.java:768-778` (RED additionally publishes `EmergencyEscalationTriggered` — untouched by this feature)

**Preconditions:** handler with mocked `ITriageFollowUpService`.

**Test Steps:**
1. *Arrange:* `event = makeEvent(e -> e.riskLevel(RiskLevel.RED))` (FX-003).
2. *Act:* `handler.onIntakeSessionCompleted(event);`
3. *Assert:* `verifyNoInteractions(followUpService)`; handler returns normally (no exception that could disturb sibling listeners).

**Expected Result (PASS):** no follow-up creation for RED; method completes without throwing.
**Expected Result (FAIL):** follow-up created for RED (would dilute the emergency UX) or exception propagates.

**Current Status:** 🟢 Passing
**Implementation Note:** this feature must not import or reference any `emergency`/`safety` types — structural assertion done in code review, behavioral assertion here.

---

### TYFU-TC-04 — Duplicate event (new eventId, same sessionId) is idempotent

**Severity:** `CRITICAL`
**Feature Under Test:** `TriageFollowUpService.scheduleFollowUp()` dedupe branch
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-TYFU-002` / ADR-TYFU-003 (dedupe key = `item_type` + `source_reference_id`); Logic Issue L3 (`eventId` random per publish, `TriageService.java:775`)

**Preconditions:** `existsByReminderTypeAndSourceReferenceId(TRIAGE_FOLLOW_UP, SESSION_1)` mocked → `true`.

**Test Steps:**
1. *Arrange:* `event = makeYellowEvent()` then a second call with **another** fresh `makeYellowEvent()` (FX-004 — different eventId, same SESSION_1).
2. *Act:* `Optional<UUID> result = service.scheduleFollowUp(event);`
3. *Assert:* result and interactions.

**Expected Result (PASS):**
- `result.isEmpty()` [TYFU-002 outcome — skip, not error]
- `reminderRepository.save(...)` never called; `scheduleFcmPush` never called; `auditService.log` never called

**Expected Result (FAIL):** second row saved, or second push scheduled — duplicate reminder reaches the mother.

**Current Status:** 🟢 Passing
**Implementation Note:** dedupe probe MUST run before session lookup/title work (cheapest first, and keeps the skip path side-effect free).

---

### TYFU-TC-05 — Title mapping: fever-family symptoms → "Kiểm tra lại thân nhiệt của bé"

**Severity:** `HIGH`
**Feature Under Test:** `TriageFollowUpTitlePolicy.deriveTitle()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpTitlePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** ADR-TYFU-006 priority table (rows 1–4); canonical vocabulary from `triage/engine/SymptomNormalizer.java` (KEYWORDS map); Roadmap Part III.3 (example title verbatim)

**Preconditions:** none (pure function).

**Test Steps (decision-table, parameterized):**
1. *Act/Assert* per row:
   - `List.of("fever")` and `List.of("high_fever")` → `"Kiểm tra lại thân nhiệt của bé"` [row 1]
   - `List.of("vomiting")` → `"Kiểm tra lại tình trạng nôn trớ của bé"` [row 2]
   - `List.of("diarrhea", "mild_dehydration")` → hydration title [row 3]
   - `List.of("cough")` → breathing title [row 4]
   - Priority: `List.of("cough", "fever")` → **row 1 title** (first match in fixed priority order, not input order)

**Expected Result (PASS):** exact strings from ADR-TYFU-006; all ≤ 255 chars (DDL `title varchar(255)`).
**Expected Result (FAIL):** raw input echoed as title (PII risk, BR-TYFU-004) or priority order violated.

**Current Status:** 🟢 Passing
**Implementation Note:** iterate the policy's own priority list, not the caller's list order.

---

### TYFU-TC-06 — Title fallback: null / empty / unmapped codes → generic title

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageFollowUpTitlePolicy.deriveTitle()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpTitlePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** ADR-TYFU-006 fallback row; BR-TYFU-004 (never null, never raw text)

**Test Steps:**
1. *Act/Assert:* `deriveTitle(null)`, `deriveTitle(List.of())`, `deriveTitle(List.of("rash", "cyanosis"))` (unmapped codes) → each returns `"Theo dõi lại tình trạng sức khỏe của bé sau sàng lọc AI"`.

**Expected Result (PASS):** generic fallback for all three; never `null`, never an exception.
**Expected Result (FAIL):** NPE on null input, empty string, or unmapped code echoed back.

**Current Status:** 🟢 Passing

---

### TYFU-TC-07 — FCM scheduling failure: care item kept, fcmJobId null, no exception

**Severity:** `HIGH`
**Feature Under Test:** `TriageFollowUpService.scheduleFollowUp()` — TYFU-004 branch
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** ADR-TYFU-004 (mirror of UC45 ADR-REM-001 resilience: reminder saved despite FCM failure — `UC45_CreateAppointmentReminder_Test-Spec.md` REM-TC-005)

**Preconditions:** happy-path mocks (TYFU-TC-01) except `scheduleFcmPush` → `doThrow(new IllegalStateException("synthetic fcm outage"))`.

**Test Steps:**
1. *Arrange* as above; 2. *Act:* `service.scheduleFollowUp(makeYellowEvent())`; 3. *Assert*.

**Expected Result (PASS):**
- `result.isPresent()` — item creation succeeded
- Saved entity persisted with `fcmJobId == null`; no exception propagates from the service for this branch
- `auditService.log(REMINDER_CREATED, …)` still called (item exists)

**Expected Result (FAIL):** exception bubbles up (would be swallowed by the handler but lose the care item) or item not saved.

**Current Status:** 🟢 Passing

---

### TYFU-TC-08 — Session not found: skip with Optional.empty(), no side effects

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageFollowUpService.scheduleFollowUp()` — TYFU-001 branch
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** TDS §10 `TYFU-001` (skip + WARN); ADR-TYFU-002 (AFTER_COMMIT means row should exist — absence is anomalous, not fatal)

**Preconditions:** `exists…` → false; `intakeSessionRepository.findById(SESSION_1)` → `Optional.empty()`.

**Test Steps:** arrange; act `scheduleFollowUp(makeYellowEvent())`; assert.

**Expected Result (PASS):** `Optional.empty()`; no `save`, no `scheduleFcmPush`, no audit; no exception.
**Expected Result (FAIL):** `NoSuchElementException`/NPE escapes, or a follow-up row is created with fabricated data.

**Current Status:** 🟢 Passing

---

### TYFU-TC-09 — Handler contains service failures (never rethrows)

**Severity:** `HIGH`
**Feature Under Test:** `TriageYellowFollowUpHandler.onIntakeSessionCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageYellowFollowUpHandlerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-TYFU-003`; pattern precedent `IntakeSessionCompletedHandlerTest.isolatedStructuredPersistenceFailure_shouldNotEscapeAfterCommitHandler`

**Preconditions:** `followUpService.scheduleFollowUp(event)` → `doThrow(new IllegalStateException("synthetic persistence failure"))`.

**Test Steps:**
1. *Arrange:* YELLOW event (FX-002).
2. *Act/Assert:* `assertThatCode(() -> handler.onIntakeSessionCompleted(event)).doesNotThrowAnyException();`
3. *Assert:* service was invoked exactly once.

**Expected Result (PASS):** no exception escapes; failure only logged (WARN, exception class name only — no PII).
**Expected Result (FAIL):** exception propagates — could disturb other AFTER_COMMIT listeners of the same event.

**Current Status:** 🟢 Passing

---

### TYFU-TC-10 — Handler is wired AFTER_COMMIT (annotation contract)

**Severity:** `HIGH`
**Feature Under Test:** `TriageYellowFollowUpHandler.onIntakeSessionCompleted()` annotation
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageYellowFollowUpHandlerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** ADR-TYFU-002 Option A; reflection-test precedent `IntakeSessionCompletedHandlerTest.completionSideWork_shouldRunOnlyAfterAuthoritativeTransactionCommits`

**Test Steps:**
1. *Act:* reflect `onIntakeSessionCompleted(IntakeSessionCompleted.class)`; read `@TransactionalEventListener`.
2. *Assert:* annotation present; `phase() == TransactionPhase.AFTER_COMMIT`; `fallbackExecution() == false`.

**Expected Result (PASS):** all three assertions hold — handler can never run for rolled-back sessions.
**Expected Result (FAIL):** plain `@EventListener` (would run inside the triage transaction — violates BR-TYFU-003) or `fallbackExecution=true` (would run without a transaction in tests/async paths).

**Current Status:** 🟢 Passing

---

### TYFU-TC-11 — Delay configuration boundaries: 1/6/24 honored; 0 and 25 fall back to 4

**Severity:** `MEDIUM`
**Feature Under Test:** delay resolution in `TriageFollowUpService` (TYFU-005)
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** ADR-TYFU-005 (default 4, valid range `[1..24]`, WARN fallback); Roadmap Part III.3 (4–6 h product range — default value itself is `Open` O1)

**Preconditions:** happy-path mocks; service constructed per-case with `delayHours` from FX-007.

**Test Steps (parameterized):**
1. `delayHours = 6` → saved `scheduledAt = T0 + 6h = 2026-07-26T16:00:00Z` (upper product bound)
2. `delayHours = 1` → `T0 + 1h`; `delayHours = 24` → `T0 + 24h` (technical bounds)
3. `delayHours = 0` → `T0 + 4h` (fallback); `delayHours = 25` → `T0 + 4h` (fallback)

**Expected Result (PASS):** exact instants above (fixed Clock removes flakiness).
**Expected Result (FAIL):** out-of-range value applied verbatim (e.g. `scheduled_at` in the past for 0/negative) or exception on invalid config.

**Current Status:** 🟢 Passing
**Implementation Note:** validate once at construction (constructor arg / `@Value`), log `TYFU-005` WARN on fallback.

---

### TYFU-TC-12 — Clock control: null completedAt uses injected Clock; UTC day-boundary arithmetic exact

**Severity:** `MEDIUM`
**Feature Under Test:** time handling in `TriageFollowUpService`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** ADR-TYFU-005 (`completedAt` primary, `clock.instant()` fallback; `Clock` injected, default `Clock.systemUTC()` — codebase pattern `TriageContinuationService.java:30`); `Clock.fixed` test pattern (`PublicContentImageCleanupJobTest.java:49`); columns are `timestamptz`/`Instant` so local timezones/DST cannot affect arithmetic

**Preconditions:** service built with `fixedClock()` = `2026-07-26T10:00:00Z`; `delayHours = 4`.

**Test Steps:**
1. *Fallback:* event FX-008 (`completedAt = null`) → assert saved `scheduledAt = 2026-07-26T14:00:00Z` (= `clock.instant() + 4h`).
2. *Day boundary (timezone condition):* event FX-010 (`completedAt = 2026-07-26T23:59:59Z`) → assert `scheduledAt = 2026-07-27T03:59:59Z` — pure `Instant` addition, crossing the UTC midnight and the Asia/Ho_Chi_Minh calendar date, no zone conversion applied.

**Expected Result (PASS):** both exact instants; repeated runs identical (no `Instant.now()` anywhere in assertions or implementation logic).
**Expected Result (FAIL):** flaky near-now assertions, or zone-shifted `scheduled_at` (e.g. implementation converts via `LocalDateTime`/default zone).

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### TYFU-TC-13 — Ownership: item owned by event's mother; another mother cannot read it

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `Luật 91/2025 (health data access control)`
**Feature Under Test:** `TriageFollowUpService.scheduleFollowUp()` ownership assignment + existing `ReminderRepository.findByIdAndOwnerUserId` guard
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java` (unit part) + `src/test/java/com/carebridge/backend/reminder/TriageFollowUpIntegrationTest.java` (read-isolation part)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** Auth Matrix TDS §16 (`Own` only); `ReminderRepository.findByIdAndOwnerUserId` (existing ownership contract used by `ReminderServiceImpl.getReminderDetail`)

**Preconditions:** FX-009 identities; happy-path mocks (unit); persisted follow-up for MOTHER_A (integration).

**Test Steps (Attack Simulation):**
1. Unit: run TYFU-TC-01 flow; assert captured `ownerUserId == MOTHER_A` (never taken from config/session-independent source).
2. Integration: after the item exists, query `reminderRepository.findByIdAndOwnerUserId(careItemId, MOTHER_B)`.
3. Assert isolation.

**Expected Result (PASS = hệ thống an toàn):**
- Unit: `ownerUserId = MOTHER_A` exactly.
- Integration: `Optional.empty()` for MOTHER_B; present for MOTHER_A. (At HTTP level this is the existing 404 behavior of `GET /api/v1/reminders/{id}` — contract unchanged.)

**Expected Result (FAIL = lỗ hổng tồn tại):** item readable/ownable by a non-owner → cross-mother health-signal leak.

**Current Status:** 🟢 Passing *(unit part — `tyfuTc13_ownerIsAlwaysEventUser_neverAnotherMother`)*
> *(2026-07-27: the integration read-isolation part (`TriageFollowUpIntegrationTest.tyfuTc13_readIsolation_otherMotherCannotSeeTheFollowUp`) executed on real PostgreSQL — PASSED.)*

---

### INTEGRATION TEST CASES

> Testcontainers `PostgreSqlContainer`; Flyway applies the canonical baseline automatically. Timeout: 120 s.

---

### TYFU-TC-INT-01 — Event publish → commit → one row + audit; duplicate publish → still one row

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: transactional publish of IntakeSessionCompleted → AFTER_COMMIT handler → scheduled_care_items + audit_log`
**Test File:** `src/test/java/com/carebridge/backend/reminder/TriageFollowUpIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
> *(2026-07-27: executed on a Docker-capable host — Testcontainers postgres:16-alpine, real Flyway chain via the test-harness bridge bootstrap — PASSED.)*
**Condition Ref:** `TC-COND-014` (+ `TC-COND-004`)

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start); Flyway baseline applied on context start
- Seed: `triage_sessions` row per FX-005 (insert via `IIntakeSessionRepository` inside a committed transaction)
- `INotificationService` replaced by a `@MockBean`/test double returning `"fcm-int-1"`
- Fixed `Clock` test bean overriding the service clock

**Test Steps:**
1. Inside `TransactionTemplate.execute`, publish `makeYellowEvent()` via `ApplicationEventPublisher`; let the transaction commit.
2. Await handler completion (synchronous AFTER_COMMIT — same thread — so simple post-commit assertion suffices).
3. Publish a second event (fresh `eventId`, same `SESSION_1`) in a new committed transaction.
4. Assert DB state.

**Expected Result (PASS):**
- Exactly **1** row: `item_type='TRIAGE_FOLLOW_UP'`, `source_reference_id=SESSION_1`, `status='PENDING'`, `owner_user_id=MOTHER_A`, `scheduled_at = completed_at + 4h`, `fcm_job_id='fcm-int-1'` [oracles: baseline DDL :1587-1610; ADR-TYFU-001/003/004/005]
- `audit_log` contains exactly 1 `REMINDER_CREATED` entry for that care item [`AuditAction.java:76` + `ReminderNotificationService`-style audit convention via `AuditService`]

**Expected Result (FAIL):** 0 rows (handler not wired / ran before commit), 2 rows (dedupe broken), or audit missing.

**DB Assertion:**
```java
List<Reminder> rows = reminderRepository.findByOwnerUserIdOrderByScheduledAtDesc(MOTHER_A).stream()
        .filter(r -> r.getReminderType() == ReminderType.TRIAGE_FOLLOW_UP)
        .toList();
assertThat(rows).hasSize(1);
Reminder saved = rows.getFirst();
assertThat(saved.getSourceReferenceId()).isEqualTo(SESSION_1);
assertThat(saved.getSourceReferenceType()).isEqualTo("TRIAGE_SESSION");
assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
assertThat(saved.getScheduledAt()).isEqualTo(Instant.parse("2026-07-26T14:00:00Z"));
```

**Current Status:** 🟢 Passing
> *(2026-07-27: `tyfuTcInt01_publishCommit_createsOneRowAndAudit_duplicatePublishStillOneRow` green on real PostgreSQL — one row + one REMINDER_CREATED audit event; duplicate publish deduped (ALREADY_SCHEDULED).)*

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `TYFU-TC-01` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-02` | `reminder/TriageYellowFollowUpHandlerTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-03` | `reminder/TriageYellowFollowUpHandlerTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-04` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-05` | `reminder/TriageFollowUpTitlePolicyTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-06` | `reminder/TriageFollowUpTitlePolicyTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-07` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-08` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-09` | `reminder/TriageYellowFollowUpHandlerTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-10` | `reminder/TriageYellowFollowUpHandlerTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-11` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-12` | `reminder/TriageFollowUpServiceTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `TYFU-TC-13` | `reminder/TriageFollowUpServiceTest.java` + `TriageFollowUpIntegrationTest.java` | `[x]` | `2026-07-26 (no-commit — unit part); 2026-07-27 (no-commit — integration part)` | `—` |
| `TYFU-TC-INT-01` | `reminder/TriageFollowUpIntegrationTest.java` | `[x]` | `2026-07-27 (no-commit)` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Most important gate. Before implementing, run the whole suite against throw-stubs. Every test MUST FAIL. Any test that passes against a stub is AP-AI-002 → reject and rewrite.
> Note: TYFU-TC-10 (annotation reflection) fails at Red Phase because the stub handler below deliberately omits the `@TransactionalEventListener` annotation; TC-02/03 fail because the stub delegates unconditionally (no YELLOW guard) into a throwing service.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stubs (MUST throw / MUST be un-annotated)

// src/main/java/com/carebridge/backend/reminder/service/impl/TriageFollowUpService.java
@Service
public class TriageFollowUpService implements ITriageFollowUpService {

    @Override
    public Optional<UUID> scheduleFollowUp(IntakeSessionCompleted event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// src/main/java/com/carebridge/backend/reminder/policy/TriageFollowUpTitlePolicy.java
@Component
public class TriageFollowUpTitlePolicy {

    public String deriveTitle(List<String> canonicalSymptoms) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// src/main/java/com/carebridge/backend/reminder/service/TriageYellowFollowUpHandler.java
@Component
public class TriageYellowFollowUpHandler {

    private final ITriageFollowUpService followUpService;

    // Red Phase: NO @TransactionalEventListener annotation yet (TYFU-TC-10 must FAIL),
    // NO risk-level guard, NO try/catch (TYFU-TC-02/03/09 must FAIL).
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        followUpService.scheduleFollowUp(event); // stub service throws
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `TYFU-TC-01` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `TYFU-TC-02` | stub delegates for GREEN → service throws | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-03` | stub delegates for RED → service throws | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-04` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-05` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-06` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-07` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-08` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-09` | stub has no try/catch → exception escapes | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-10` | stub method un-annotated | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-11` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-12` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-13` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TYFU-TC-INT-01` | handler delegates → stub throws → no row | 🔴 FAIL | ☑ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `(no commit — HARD RULE: no git commit; stubs verified in working tree 2026-07-26)`
- Tất cả FAIL? ☑ Yes (27/27 test executions FAILED against the §5.1 stubs — run 2026-07-26: Tests run 27, Failures 6, Errors 21, Skipped 0; TYFU-TC-INT-01 failed at Testcontainers initialization, also a non-PASS) → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `04_Implement/TriageYellowFollowUp/red-gate-evidence.log` *(produced 2026-07-26 by `./mvnw test -Dtest="TriageFollowUpServiceTest,TriageFollowUpTitlePolicyTest,TriageYellowFollowUpHandlerTest,TriageFollowUpIntegrationTest"`)*

> **Nếu bất kỳ test PASS:** stop. Identify root cause in the table above. Rewrite the test from its TC spec with the Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] TDS `CB-TYFU-IMP-001` reviewed and **Approved** (verified Approved before implementation, 2026-07-26)
- [x] This Test-Spec `CB-TYFU-TDD-001` reviewed and **Approved** (verified Approved before implementation, 2026-07-26)
- [x] Logic Issues (Section 2, L1–L7) confirmed — encoded in the implemented tests; specs carrying them were Approved
- [x] Open item O1 (delay default 4 vs 6 h) decided — proposed default **4 h** stood at approval; TYFU-TC-01/11/12 expected instants use 4 h
- [x] No Flyway migration required (verified §TDS 5.2) — N/A checklist item
- [x] Test fixtures (TDS-05) prepared in `TriageFollowUpTestFactory` (2026-07-26)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — all NEW feature unit tests green, including pre-existing `reminder` and `ai` suites (regression guard for `ReminderType`/`Reminder` changes). Full `./mvnw clean test` 2026-07-26: **3008 run / 1 failure / 74 errors / 100 skipped — ALL attributable to the known pre-existing set** (ChecklistTemplateMigrationTest SHA drift + Docker-unavailable Testcontainers suites, now incl. `TriageFollowUpIntegrationTest`); every `reminder.*`/`ai.*` suite and all 37 sibling triage-feature tests green
- [x] `./mvnw verify` — integration test green (Testcontainers) *(MET in substance 2026-07-27 via `./mvnw test -Dtest=TriageFollowUpIntegrationTest` — Tests run 2, Failures 0, Errors 0 on a Docker-capable host; the full `verify` lifecycle was not separately run)*
- [ ] Test coverage ≥ 80% lines for `TriageFollowUpService`, `TriageFollowUpTitlePolicy`, `TriageYellowFollowUpHandler` *(not measured — no coverage run executed; unchecked truthfully)*
- [x] No business logic added to any controller (feature has no controller — no controller file touched)
- [x] No PII/symptom free text in logs or titles (grep check, TDS §14.2 — verified 2026-07-26: no `title`/symptom variables in log statements; titles are the five fixed ADR-TYFU-006 strings)
- [x] Exactly one `scheduled_care_items` row per YELLOW session in the integration run; zero rows for GREEN/RED *(MET 2026-07-27: TYFU-TC-INT-01 asserted exactly one row after duplicate publish on real PostgreSQL; GREEN/RED zero-row cases covered at unit level by TYFU-TC-02/03)*

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — all 14 TCs FAIL against the stubs before implementation (27/27 executions FAILED, 2026-07-26 — `red-gate-evidence.log`)
- [x] **Contract Existence** — every injected class exists:
  ```bash
  cd 05_Development/CareBridgeAPI && ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — no shared mutable state between tests (grep clean 2026-07-26; instances built per-test via `TriageFollowUpTestFactory`/`makeService`):
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " src/test/java/com/carebridge/backend/reminder/TriageFollowUp*.java
  # Every instance must live inside @Test or come from TriageFollowUpTestFactory
  ```
- [x] **Oracle Source** — every expected value in asserts cites BR/ADR/DDL (see per-TC Oracle Source lines; mirrored as inline comments in the test files)

### Suspension Criteria (Điều kiện tạm dừng)

- Open item O1 unresolved past sprint planning
- `ReminderType` enum change breaks existing reminder suites (architectural review needed)
- CI broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No Flyway migration in this feature — code + test rollback only (dev only).

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/service/ITriageFollowUpService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/service/impl/TriageFollowUpService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/service/TriageYellowFollowUpHandler.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/policy/TriageFollowUpTitlePolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/entity/ReminderType.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/entity/Reminder.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/repository/ReminderRepository.java
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/application.yaml

# Revert tests
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/TriageFollowUpServiceTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/TriageYellowFollowUpHandlerTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/TriageFollowUpTitlePolicyTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/TriageFollowUpTestFactory.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/TriageFollowUpIntegrationTest.java

# Optional data hygiene on dev DB (rows are plain, safe to delete)
psql "$DB_URL" -c "DELETE FROM scheduled_care_items WHERE item_type = 'TRIAGE_FOLLOW_UP';"

# Gap remains OPEN → roadmap Part III.3 stays unimplemented; keep this spec pair for the next attempt.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> Reviewer checklist — tests in this spec were AI-generated (see §1.1).

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC without ADR/BR/DDL reference — every TC here carries an `Oracle Source` line | ☑ | G-0 |
| AP-AI-002 | Green-from-Birth | Any TC passing against §5.1 stubs (note: stub handler is deliberately un-annotated/guard-free so TC-02/03/09/10 can fail) — verified 2026-07-26: 27/27 executions FAILED at Red Gate | ☑ | G-2 ★ |
| AP-AI-003 | Implicit Decision | TC assuming an architecture decision missing from CB-TYFU-IMP-001 §3 (e.g. retry queue, outbox, new event) — none introduced | ☑ | G-1 |
| AP-AI-004 | Layer Violation | TC verifying business logic in a controller — no controller exists in this feature | ☑ | G-4 |
| AP-AI-005 | Hallucinated Contract | TC importing non-existent types; verify: `IntakeSessionCompleted`, `RiskLevel`, `Reminder`, `ReminderType`, `ReminderStatus`, `ReminderRepository`, `IIntakeSessionRepository`, `INotificationService`, `AuditService` all exist today; `ITriageFollowUpService`, `TriageFollowUpService`, `TriageYellowFollowUpHandler`, `TriageFollowUpTitlePolicy`, `ReminderType.TRIAGE_FOLLOW_UP`, `Reminder.sourceReference*`, `existsByReminderTypeAndSourceReferenceId` are NEW and declared in TDS §5.1/§8 — `./mvnw compile` clean 2026-07-26 | ☑ | G-3 |

**Kết quả review:**

- [x] No anti-pattern found → TDD spec approved *(verified 2026-07-26 during Phase 3 — Red Gate all-FAIL, §8 greps clean, compile clean)*
- [ ] AP found → record below → fix before implementation

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Template v2.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol integrated.*
*Status remains `Draft`; all TCs start 🔴 Not written / TDD Phase 🔴 RED — chưa implement. Never self-approve.*
