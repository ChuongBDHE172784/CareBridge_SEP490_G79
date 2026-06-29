# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# SRS 3.3.2.4 — Complete Pre-exercise Safety Check — Test Specification

**Document ID:** `CB-EXERCISE-IMP-003-TEST`
**Version:** `1.0`
**Date:** `2026-06-28`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Developer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC178_CompletePreExerciseSafetyCheck/UC178_CompletePreExerciseSafetyCheck_TDS.md` (CB-EXERCISE-IMP-003 v1.0) — Technical Design Specification
- `04_Implement/UC177_ViewPregnancyExerciseDetail/UC177_ViewPregnancyExerciseDetail_TDS.md` (CB-EXERCISE-IMP-002) — Upstream: shared ExerciseRepository
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — Primary schema oracle
- `01_Requirements/SRS.md` — SRS 3.3.2.4 Functional requirements
- `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/pre_exercise_safety_check_screen.dart` — Existing mobile UI

> **TDD Convention:** This document describes test cases BEFORE writing production code.
> Required order: write test (`.java` / Dart) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test as PASS until `./mvnw test` (backend) or `flutter test` (mobile) is green.
> Never use real PII in test data — SYNTHETIC data only.
> Oracle source for every expected value must be cited (BR/ADR/schema).

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information.

| Date | Author | Change Description |
|------|--------|--------------------|
| 2026-06-28 | AI Agent — Developer | Initial document — TDD spec for SRS 3.3.2.4 Complete Pre-exercise Safety Check (UC-178) |

---

## TABLE OF CONTENTS

1. [Module Info](#1-module-info)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
   - Props Isolation Boilerplate
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `SRS-3.3.2.4` |
| **Module** | `Complete Pre-exercise Safety Check — exercise` |
| **Spec source** | `CB-EXERCISE-IMP-003 v1.0` |
| **Priority** | 🔴 P0 (Critical — BR-SAFETY gate for exercise session start) |
| **Sprint** | `Sprint 1 (2026-06-28 →)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` (answers represent pregnancy health status) |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY, PDPA` |
| **Upstream Dependencies** | `CB-EXERCISE-IMP-002 (shared ExerciseRepository, ExerciseNotFoundException), IAM (JWT)` |
| **Downstream Consumers** | `CB-153 — Start Exercise Session (reads safetyCheckId to verify CLEARED gate)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-003 §17, ADR-PSC-001, ADR-PSC-002, ADR-PSC-003` |
| **Constraints Injected** | C1 (verify PUBLISHED exercise first), C2 (any false → BLOCKED; all true → CLEARED), C3 (non-diagnostic blocked_reason with escalation), C4 (userId from JWT only), C5 (answer_json not in logs), C6 (no logic in controller), C7 (SafetyCheckPolicy is sole evaluator), C8 (both outcomes return 201), C9 (no session-start coupling) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For schema disputes, `V1__init_schema.sql` and approved migrations are the persistence oracle.
> Test cases encode the **corrected** behavior.

| # | Original Spec (incorrect/missing) | Actual (schema / policy) | Fix applied in tests |
|---|-----------------------------------|--------------------------|----------------------|
| L1 | Safety check might allow exercise if 3 of 4 questions pass | ADR-PSC-001: BLOCKED if ANY answer is false. ALL four must be true for CLEARED. No partial clearance. | Tests assert: Q1=false with Q2/Q3/Q4=true → BLOCKED (not partial clearance) |
| L2 | blocked_reason could potentially be generated dynamically or left null when BLOCKED | ADR-PSC-003: blocked_reason MUST be non-null pre-authored text with escalation prompt when status=BLOCKED. Never null. | Tests assert: blocked_reason not null, not empty, contains "doctor or midwife" when BLOCKED |
| L3 | UI passes `Navigator.pop(true)` when all checked — no backend call | Feature spec: backend `POST /api/v1/exercises/{exerciseId}/safety-check` MUST be called to persist the record and obtain `safetyCheckId` | Tests assert: API call made on submit; result contains safetyCheckId |
| L4 | `completed_at` field purpose unclear | V1 schema: `completed_at timestamptz` — set to `now()` when CLEARED, remains NULL when BLOCKED (invariant from §6.4) | Tests assert: completedAt not null for CLEARED; completedAt is null for BLOCKED |
| L5 | BLOCKED result might return 4xx (client error) | ADR-PSC-001 + §10: BLOCKED is a valid processed response — returns HTTP 201 not 4xx | Tests assert: BLOCKED submission returns 201 (not 422 or 400) |
| L6 | `result_status` default is 'PENDING' in V1 schema | Service must always resolve to CLEARED or BLOCKED before persistence. PENDING is an internal transient state — never returned to client. | Tests assert: response.resultStatus is always "CLEARED" or "BLOCKED", never "PENDING" |
| L7 | `exercise_safety_checks` has no secondary index for GET /latest query | §5.2 migration: `idx_safety_checks_exercise_user_created (exercise_id, user_id, created_at DESC)` must exist | Integration tests seed index existence before running GET /latest |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
CB-EXERCISE-IMP-003 — Complete Pre-exercise Safety Check covers these layers:
├── Policy (pure domain logic — no Spring dependencies)
│   └── SafetyCheckPolicy.evaluate() — red-flag evaluation
│   └── SafetyCheckPolicy.buildBlockedReason() — guidance text generation
├── Service (mock JPA Repositories with Mockito)
│   └── ExerciseSafetyCheckService.submitSafetyCheck() — orchestration
│   └── ExerciseSafetyCheckService.getLatestSafetyCheck() — query
├── Mapper (unit test, no mocking)
│   └── SafetyCheckMapper.toEntity() — request → entity
│   └── SafetyCheckMapper.toResponse() — entity → DTO
├── Controller (mock Service with @WebMvcTest)
│   └── POST /api/v1/exercises/{exerciseId}/safety-check
│   └── GET /api/v1/exercises/{exerciseId}/safety-check/latest
├── Integration (Testcontainers PostgreSQL + @SpringBootTest)
│   └── Full submit flow with real DB
│   └── GET /latest with real DB
│   └── Verify answer_json not in logs
└── Mobile Widget (flutter_test)
    └── PreExerciseSafetyCheckScreen API call wiring
    └── BLOCKED state shows blockedReason

OUT OF SCOPE:
  └── Exercise session start → CB-153
  └── Posture analysis → UC-30
  └── Exercise list/detail → CB-EXERCISE-IMP-001/002
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.4` | Mother answers 4 health questions; system blocks if any red flag detected |
| `ADR-PSC-001` | Any red flag → BLOCKED absolute; CLEARED = all 4 pass |
| `ADR-PSC-002` | userId from JWT SecurityContext only; answer_json not in logs |
| `ADR-PSC-003` | blocked_reason: non-diagnostic, non-null, includes escalation prompt |
| `ADR-PSC-004` | No new table migration — exercise_safety_checks from V1 |
| `BR-SAFETY-001` | System guidance must be non-diagnostic |
| `BR-SAFETY-002` | Red flag detected → block continuation (no override) |
| `BR-SAFETY-003` | blocked_reason must include escalation prompt |
| `BR-RBAC-001` | MOTHER role required via JWT |
| `BR-EXERCISE-001` | Only PUBLISHED exercise can have safety check submitted |
| `CB-EXERCISE-IMP-003 §8` | IExerciseSafetyCheckService contract |
| `CB-EXERCISE-IMP-003 §10` | Error codes: PSC-001, PSC-002, PSC-003, EX-001, IAM-001, IAM-002 |
| `V1__init_schema.sql` | exercise_safety_checks columns: safety_check_id, result_status, red_flag_detected, blocked_reason, completed_at, answer_json |
| `pre_exercise_safety_check_screen.dart` | Existing mobile UI — 4 checkboxes, notes field, _allChecked gate |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | All 4 answers true → result_status = CLEARED | `SafetyCheckPolicy.evaluate()` | `PSC-TC-001` |
| TC-COND-002 | Q1=false (dizziness) → BLOCKED with guidance for Q1 | `SafetyCheckPolicy.evaluate()`, `buildBlockedReason()` | `PSC-TC-002` |
| TC-COND-003 | Q2=false (contractions) → BLOCKED | `SafetyCheckPolicy.evaluate()` | `PSC-TC-003` |
| TC-COND-004 | Q3=false (bleeding/fluid) → BLOCKED | `SafetyCheckPolicy.evaluate()` | `PSC-TC-003` |
| TC-COND-005 | Q4=false (not hydrated/fed) → BLOCKED | `SafetyCheckPolicy.evaluate()` | `PSC-TC-003` |
| TC-COND-006 | Multiple false answers → single BLOCKED record | `SafetyCheckPolicy.evaluate()`, `ExerciseSafetyCheckService` | `PSC-TC-004` |
| TC-COND-007 | blocked_reason contains escalation prompt | `SafetyCheckPolicy.buildBlockedReason()` | `PSC-TC-005` |
| TC-COND-008 | blocked_reason does NOT contain diagnostic language | `SafetyCheckPolicy.buildBlockedReason()` | `PSC-TC-006` |
| TC-COND-009 | CLEARED → completedAt set, redFlagDetected=false, blockedReason=null | `SafetyCheckMapper.toEntity()` | `PSC-TC-001`, `PSC-TC-INT-001` |
| TC-COND-010 | BLOCKED → completedAt null, redFlagDetected=true, blockedReason not null | `SafetyCheckMapper.toEntity()` | `PSC-TC-002`, `PSC-TC-INT-002` |
| TC-COND-011 | Exercise not PUBLISHED → 404 EX-001 | `ExerciseSafetyCheckService.submitSafetyCheck()` | `PSC-TC-007` |
| TC-COND-012 | Missing q1 answer → 400 PSC-001 | `ExerciseController` (@Valid) | `PSC-TC-008` |
| TC-COND-013 | Both CLEARED and BLOCKED return HTTP 201 | `ExerciseController` | `PSC-TC-INT-001`, `PSC-TC-INT-002` |
| TC-COND-014 | GET /latest returns most recent submission | `ExerciseSafetyCheckService.getLatestSafetyCheck()` | `PSC-TC-INT-003` |
| TC-COND-015 | GET /latest with no prior submission → 404 PSC-002 | `ExerciseSafetyCheckService.getLatestSafetyCheck()` | `PSC-TC-009` |
| TC-COND-016 | result_status is never "PENDING" in response | `ExerciseSafetyCheckService` | `PSC-TC-001`, `PSC-TC-002` |
| TC-COND-017 | answer_json absent from application logs | `ExerciseSafetyCheckService` | `PSC-TC-SEC-003` |
| TC-COND-018 | Mother cannot access another user's safety checks | `ExerciseController` + Service userId isolation | `PSC-TC-SEC-004` |
| TC-COND-019 | No JWT → 401 | Spring Security | `PSC-TC-SEC-001` |
| TC-COND-020 | Non-MOTHER role → 403 | Spring Security RBAC | `PSC-TC-SEC-002` |
| TC-COND-021 | Mobile: all 4 checked → API call triggered | `PreExerciseSafetyCheckScreen` | `PSC-TC-MOB-001` |
| TC-COND-022 | Mobile: BLOCKED result shows blockedReason text | `PreExerciseSafetyCheckScreen` | `PSC-TC-MOB-002` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Answer combinations (all-true, any-one-false, all-false) | Cover all distinct outcome classes |
| Boundary Value Analysis | 0 red flags (CLEARED) vs. 1 red flag (BLOCKED) — boundary of block trigger | ADR-PSC-001: any=1 is the critical boundary |
| State Transition Testing | result_status: PENDING → CLEARED / PENDING → BLOCKED | §6.4 state machine compliance |
| Error Guessing | Missing fields, wrong types, no JWT, wrong role, stale exercise | Common API misuse vectors |
| Security Testing | PII log scan, user isolation, BLOCKED override attempt | BR-SAFETY + PDPA |
| Specification-based Testing | blocked_reason text content validation | ADR-PSC-003 compliance |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-PSC-001` | DB seed | `{ exerciseId: UUID-EX-P1, status: PUBLISHED, title: "Prenatal Yoga T1", createdBy: UUID-ADMIN }` | PUBLISHED exercise for happy path submissions |
| `FX-PSC-002` | DB seed | `{ exerciseId: UUID-EX-D1, status: DRAFT, title: "Draft Exercise" }` | DRAFT exercise — must reject safety check |
| `FX-PSC-003` | Request body | `{ q1: true, q2: true, q3: true, q4: true }` | All-clear request → CLEARED |
| `FX-PSC-004` | Request body | `{ q1: false, q2: true, q3: true, q4: true }` | Dizziness reported (Q1=false) → BLOCKED |
| `FX-PSC-005` | Request body | `{ q1: true, q2: false, q3: true, q4: true }` | Contractions reported (Q2=false) → BLOCKED |
| `FX-PSC-006` | Request body | `{ q1: true, q2: true, q3: false, q4: true }` | Bleeding/fluid reported (Q3=false) → BLOCKED |
| `FX-PSC-007` | Request body | `{ q1: true, q2: true, q3: true, q4: false }` | Not hydrated/fed (Q4=false) → BLOCKED |
| `FX-PSC-008` | Request body | `{ q1: false, q2: false, q3: false, q4: false }` | All false → BLOCKED, multiple flags |
| `FX-PSC-009` | Request body (invalid) | `{ q2: true, q3: true, q4: true }` | Missing q1 → 400 PSC-001 |
| `FX-PSC-010` | JWT | `{ sub: "mother-001", role: "MOTHER" }` | Valid MOTHER JWT |
| `FX-PSC-011` | JWT | `(none)` | Missing JWT — 401 scenario |
| `FX-PSC-012` | JWT | `{ sub: "expert-001", role: "EXPERT" }` | Wrong role — 403 scenario |
| `FX-PSC-013` | JWT | `{ sub: "mother-002", role: "MOTHER" }` | Different Mother — user isolation |
| `FX-PSC-014` | UUID | `00000000-0000-0000-0000-999999999999` | Non-existent exerciseId → 404 EX-001 |
| `FX-PSC-015` | path param | `"not-a-uuid"` | Invalid UUID format → 400 EX-003 |

---

## 4. Test Case Specification

> **TC ID format:** `PSC-TC-NNN`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

> CASE 2.0 Rule: Every test MUST create fresh instances via factory. No shared mutable state between test cases. This guards against AP-AI-002 (Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (CB-EXERCISE-IMP-003)
// Place at top of each test file — every @Test uses factory methods.
// ═══════════════════════════════════════════════════════════════════════

class SafetyCheckTestFactory {

    // === Fixed UUIDs for deterministic tests ===
    static final UUID EXERCISE_ID_PUBLISHED = UUID.fromString("00000000-0000-0000-0001-000000000001");
    static final UUID EXERCISE_ID_DRAFT     = UUID.fromString("00000000-0000-0000-0002-000000000001");
    static final UUID EXERCISE_ID_NOT_EXIST = UUID.fromString("00000000-0000-0000-0000-999999999999");
    static final UUID MOTHER_USER_ID        = UUID.fromString("00000000-0000-0000-0003-000000000001");
    static final UUID MOTHER_USER_ID_2      = UUID.fromString("00000000-0000-0000-0003-000000000002");
    static final UUID ADMIN_CREATOR_ID      = UUID.fromString("00000000-0000-0000-0000-200000000001");
    static final UUID SAFETY_CHECK_ID_1     = UUID.fromString("00000000-0000-0000-0004-000000000001");
    static final UUID JOURNEY_ID_1          = UUID.fromString("00000000-0000-0000-0005-000000000001");

    // === Baseline PUBLISHED exercise ===
    static PregnancyExercise makePublishedExercise() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(EXERCISE_ID_PUBLISHED);
        e.setCreatedBy(ADMIN_CREATOR_ID);
        e.setTitle("Prenatal Yoga - First Trimester");
        e.setDescription("Gentle yoga poses suitable for early pregnancy");
        e.setTrimesterScope(TrimesterScope.FIRST);
        e.setDifficultyLevel(DifficultyLevel.EASY);
        e.setDurationMinutes((short) 20);
        e.setInstructionContent("Step 1: Start in comfortable position.");
        e.setSafetyWarning("Stop if dizzy.");
        e.setSupportsPostureAnalysis(false);
        e.setStatus(ExerciseStatus.PUBLISHED);
        e.setVersionNo(1);
        e.setCreatedAt(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        e.setUpdatedAt(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        return e;
    }

    // === All-clear request ===
    static SubmitSafetyCheckRequest makeAllClearRequest() {
        SubmitSafetyCheckRequest req = new SubmitSafetyCheckRequest();
        req.setQ1NoDizziness(true);
        req.setQ2NoContractions(true);
        req.setQ3NoBleeding(true);
        req.setQ4HydratedAndFed(true);
        req.setJourneyId(JOURNEY_ID_1);
        req.setNotes("Feeling good today.");
        return req;
    }

    // === Request with specific false answer ===
    static SubmitSafetyCheckRequest makeRequestWithQ1False() {
        SubmitSafetyCheckRequest req = makeAllClearRequest();
        req.setQ1NoDizziness(false);  // Dizziness present — RED FLAG
        return req;
    }

    static SubmitSafetyCheckRequest makeRequestWithQ2False() {
        SubmitSafetyCheckRequest req = makeAllClearRequest();
        req.setQ2NoContractions(false);  // Contractions present — RED FLAG
        return req;
    }

    static SubmitSafetyCheckRequest makeRequestWithQ3False() {
        SubmitSafetyCheckRequest req = makeAllClearRequest();
        req.setQ3NoBleeding(false);  // Bleeding/fluid present — RED FLAG
        return req;
    }

    static SubmitSafetyCheckRequest makeRequestWithQ4False() {
        SubmitSafetyCheckRequest req = makeAllClearRequest();
        req.setQ4HydratedAndFed(false);  // Not hydrated/fed — RED FLAG
        return req;
    }

    static SubmitSafetyCheckRequest makeAllFalseRequest() {
        SubmitSafetyCheckRequest req = new SubmitSafetyCheckRequest();
        req.setQ1NoDizziness(false);
        req.setQ2NoContractions(false);
        req.setQ3NoBleeding(false);
        req.setQ4HydratedAndFed(false);
        return req;
    }

    // === Pre-built CLEARED safety check entity ===
    static ExerciseSafetyCheck makeClearedSafetyCheck() {
        ExerciseSafetyCheck check = new ExerciseSafetyCheck();
        check.setSafetyCheckId(SAFETY_CHECK_ID_1);
        check.setExerciseId(EXERCISE_ID_PUBLISHED);
        check.setUserId(MOTHER_USER_ID);
        check.setJourneyId(JOURNEY_ID_1);
        check.setAnswerJson(Map.of("Q1", true, "Q2", true, "Q3", true, "Q4", true));
        check.setRedFlagDetected(false);
        check.setResultStatus(SafetyCheckStatus.CLEARED);
        check.setBlockedReason(null);
        check.setCompletedAt(OffsetDateTime.of(2026, 6, 28, 8, 30, 0, 0, ZoneOffset.UTC));
        check.setCreatedAt(OffsetDateTime.of(2026, 6, 28, 8, 30, 0, 0, ZoneOffset.UTC));
        return check;
    }

    // === Pre-built BLOCKED safety check entity ===
    static ExerciseSafetyCheck makeBlockedSafetyCheck() {
        ExerciseSafetyCheck check = new ExerciseSafetyCheck();
        check.setSafetyCheckId(SAFETY_CHECK_ID_1);
        check.setExerciseId(EXERCISE_ID_PUBLISHED);
        check.setUserId(MOTHER_USER_ID);
        check.setAnswerJson(Map.of("Q1", false, "Q2", true, "Q3", true, "Q4", true));
        check.setRedFlagDetected(true);
        check.setResultStatus(SafetyCheckStatus.BLOCKED);
        check.setBlockedReason("Dizziness or faintness was reported. It is not recommended to exercise while experiencing dizziness as it may indicate reduced blood flow. Please rest and consult your doctor or midwife before proceeding with physical activity.");
        check.setCompletedAt(null);  // null when BLOCKED — L4 logic fix
        check.setCreatedAt(OffsetDateTime.of(2026, 6, 28, 8, 31, 0, 0, ZoneOffset.UTC));
        return check;
    }
}
```

---

### PSC-TC-001 — All 4 answers true → CLEARED result

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckPolicy.evaluate()` + `ExerciseSafetyCheckService.submitSafetyCheck()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-009, TC-COND-016`
**Oracle Source:** `ADR-PSC-001` (all four true → CLEARED), `V1__init_schema.sql` (result_status, completed_at columns), `L4` (completedAt set when CLEARED), `L6` (never PENDING in response)

**Preconditions:**
- `SafetyCheckPolicy` instantiated (no dependencies — pure domain class)
- FX-PSC-003: all-true answer set

**Test Steps:**
1. Arrange: Create `Map<SafetyQuestion, Boolean>` with all values = `true`
2. Act: `policy.evaluate(allTrueAnswers)`
3. Assert: `EvaluationResult.isCleared() == true` and `flaggedQuestions.isEmpty()`

**Expected Result (PASS):**
- `result.isCleared()` = `true`
- `result.getFlaggedQuestions()` = empty list
- No exception thrown

**Expected Result (FAIL):**
- `result.isCleared()` = `false` (policy incorrectly blocks all-clear submission)
- Any question incorrectly flagged
- Exception thrown from policy

**Current Status:** 🔴 Not written
**Implementation Note:** `SafetyCheckPolicy.evaluate()` must iterate all 4 questions and only return cleared=true when zero red flags detected.

---

### PSC-TC-002 — Q1=false (dizziness) → BLOCKED, blocked_reason references Q1

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckPolicy.evaluate()` + `SafetyCheckPolicy.buildBlockedReason()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002, TC-COND-010`
**Oracle Source:** `ADR-PSC-001` (any false → BLOCKED), `ADR-PSC-003` (blocked_reason must name the concern), `L4` (completedAt null when BLOCKED)

**Preconditions:**
- FX-PSC-004: `{ Q1: false, Q2: true, Q3: true, Q4: true }`

**Test Steps:**
1. Arrange: answers = `{Q1_NO_DIZZINESS: false, Q2: true, Q3: true, Q4: true}`
2. Act: `result = policy.evaluate(answers)`; `reason = policy.buildBlockedReason(result.getFlaggedQuestions())`
3. Assert:
   - `result.isCleared()` = `false`
   - `result.getFlaggedQuestions()` contains `SafetyQuestion.Q1_NO_DIZZINESS`
   - `reason` is not null, not empty
   - `reason` references dizziness/faintness concern

**Expected Result (PASS):**
- `result.isCleared()` = `false`
- `result.getFlaggedQuestions()` = `[Q1_NO_DIZZINESS]`
- `reason` contains "dizziness" or "faintness" (case-insensitive)

**Expected Result (FAIL):**
- Policy returns cleared=true when Q1=false (safety bypass — critical vulnerability)
- `reason` is null or empty
- `reason` does not reference the actual symptom

**Current Status:** 🔴 Not written

---

### PSC-TC-003 — Each individual red-flag question independently blocks

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckPolicy.evaluate()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003, TC-COND-004, TC-COND-005`
**Oracle Source:** `ADR-PSC-001` (ANY false → BLOCKED)

**Preconditions:**
- FX-PSC-005 (Q2=false), FX-PSC-006 (Q3=false), FX-PSC-007 (Q4=false)

**Test Steps (parameterized — run for Q2, Q3, Q4 each):**
1. Arrange: Build request with exactly ONE answer false (Q2, then Q3, then Q4 in separate test runs)
2. Act: `policy.evaluate(answers)`
3. Assert: `result.isCleared()` = `false` for all three

**Expected Result (PASS):**
- Q2=false → BLOCKED (contractions)
- Q3=false → BLOCKED (bleeding/fluid)
- Q4=false → BLOCKED (not hydrated/fed)
- Each independently produces cleared=false

**Expected Result (FAIL):**
- Any single false answer produces cleared=true (safety bypass — CRITICAL)

**Current Status:** 🔴 Not written
**Implementation Note:** Use `@ParameterizedTest` with `@MethodSource` for Q2, Q3, Q4 cases.

---

### PSC-TC-004 — Multiple false answers → single BLOCKED record, all flagged questions listed

**Severity:** `HIGH`
**Feature Under Test:** `SafetyCheckPolicy.evaluate()` + `buildBlockedReason()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-PSC-001` (any false → BLOCKED; blocked_reason consolidates all flagged items)

**Preconditions:**
- FX-PSC-008: all four answers false

**Test Steps:**
1. Arrange: `{ Q1: false, Q2: false, Q3: false, Q4: false }`
2. Act: `result = policy.evaluate(allFalseAnswers)`; `reason = policy.buildBlockedReason(result.getFlaggedQuestions())`
3. Assert: `result.getFlaggedQuestions().size()` = 4; result is BLOCKED; `reason` is not null and not empty

**Expected Result (PASS):**
- `result.getFlaggedQuestions()` = all 4 questions
- `result.isCleared()` = false
- `reason` is non-null, non-empty (may consolidate or list all concerns)

**Expected Result (FAIL):**
- Only partial set of flagged questions returned
- Exception thrown due to multiple flags

**Current Status:** 🔴 Not written

---

### PSC-TC-005 — blocked_reason contains mandatory escalation prompt

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckPolicy.buildBlockedReason()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-SAFETY-003` (escalation guidance mandatory), `ADR-PSC-003` (must include "consult your doctor or midwife")

**Preconditions:**
- FX-PSC-004 (Q1=false)

**Test Steps:**
1. Arrange: Trigger Q1 red flag
2. Act: `reason = policy.buildBlockedReason([Q1_NO_DIZZINESS])`
3. Assert: `reason.toLowerCase()` contains "doctor" AND "midwife"

**Expected Result (PASS):**
- `reason` contains both "doctor" and "midwife" (case-insensitive match)

**Expected Result (FAIL):**
- `reason` missing "doctor" or "midwife" → BR-SAFETY-003 violation
- `reason` is null or empty

**Current Status:** 🔴 Not written
**Implementation Note:** The pre-authored blocked_reason constant must include the exact phrase "consult your doctor or midwife" or equivalent. This constraint is enforced by the test.

---

### PSC-TC-006 — blocked_reason does NOT contain diagnostic language

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckPolicy.buildBlockedReason()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-SAFETY-001` (non-diagnostic guidance only), `ADR-PSC-003`

**Preconditions:**
- Run for all 4 question red-flag cases

**Test Steps (parameterized for all 4 questions):**
1. Arrange: Each question false individually
2. Act: `reason = policy.buildBlockedReason([questionX])`
3. Assert:
   - `reason` does NOT contain: "you have [condition]"
   - `reason` does NOT contain: "you are suffering"
   - `reason` does NOT contain: "diagnosed"
   - `reason` does NOT contain: "prescribed"

**Expected Result (PASS):**
- None of the forbidden phrases present in reason text for any question variant

**Expected Result (FAIL):**
- Any reason text contains diagnostic language → BR-SAFETY-001 violation

**Current Status:** 🔴 Not written
**Implementation Note:** Assert using `assertThat(reason.toLowerCase()).doesNotContain("you have", "you are suffering", "diagnos", "prescri")`. Text must use observational language: "was reported", "was not confirmed", "may indicate".

---

### PSC-TC-007 — Exercise not PUBLISHED → 404 EX-001

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSafetyCheckService.submitSafetyCheck()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-EXERCISE-001` (PUBLISHED only), `CB-EXERCISE-IMP-003 §10` (EX-001), `ADR-PSC-002` (verify exercise before processing)

**Preconditions:**
- `ExerciseRepository` mocked
- FX-PSC-002: DRAFT exercise; `findByExerciseIdAndStatus(id, PUBLISHED)` returns empty
- FX-PSC-010: valid MOTHER JWT userId

**Test Steps:**
1. Arrange: Mock `exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID_DRAFT, PUBLISHED)` → `Optional.empty()`
2. Act: `safetyCheckService.submitSafetyCheck(EXERCISE_ID_DRAFT, allClearRequest, MOTHER_USER_ID)`
3. Assert: `ExerciseNotFoundException` thrown with code `"EX-001"`

**Expected Result (PASS):**
- `ExerciseNotFoundException` thrown
- `exception.getCode()` = `"EX-001"`
- HTTP mapping = 404
- `ExerciseSafetyCheckRepository.save()` was NEVER called (no phantom record creation)

**Expected Result (FAIL):**
- Service proceeds to evaluate answers despite unpublished exercise
- Repository save is called for invalid exercise
- Wrong exception or error code

**Current Status:** 🔴 Not written
**Implementation Note:** Verify with `verify(safetyCheckRepository, never()).save(any())` after exception assertion.

---

### PSC-TC-008 — Missing required answer → 400 PSC-001

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseController` — @Valid DTO validation
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-EXERCISE-IMP-003 §10` (PSC-001), `SubmitSafetyCheckRequest` @NotNull constraints (§8.1)

**Preconditions:**
- @WebMvcTest with mocked service
- FX-PSC-009: body missing `q1NoDizziness` and `q3NoBleeding`
- FX-PSC-010: valid MOTHER JWT

**Test Steps:**
1. Arrange: Request body with only `q2NoContractions: true` and `q4HydratedAndFed: true` (missing q1 and q3)
2. Act: `POST /api/v1/exercises/{UUID}/safety-check` with partial body
3. Assert: HTTP 400 with error code `PSC-001`

**Expected Result (PASS):**
- HTTP 400 Bad Request
- Response body: `{ "error": { "code": "PSC-001", "message": "Safety check validation failed", "details": [...] } }`
- `details` array contains entries for `q1NoDizziness` and `q3NoBleeding`

**Expected Result (FAIL):**
- HTTP 500 (unhandled validation exception)
- Missing required field silently treated as `null` and evaluated as red flag without error

**Current Status:** 🔴 Not written

---

### PSC-TC-009 — GET /latest with no prior submission → 404 PSC-002

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSafetyCheckService.getLatestSafetyCheck()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-EXERCISE-IMP-003 §10` (PSC-002)

**Preconditions:**
- `ExerciseSafetyCheckRepository` mocked
- `findTopByExerciseIdAndUserIdOrderByCreatedAtDesc()` returns `Optional.empty()`

**Test Steps:**
1. Arrange: Mock repository to return `Optional.empty()`
2. Act: `safetyCheckService.getLatestSafetyCheck(EXERCISE_ID_PUBLISHED, MOTHER_USER_ID)`
3. Assert: `SafetyCheckNotFoundException` (or equivalent) thrown with code `"PSC-002"`

**Expected Result (PASS):**
- `SafetyCheckNotFoundException` thrown with code `"PSC-002"`
- HTTP mapping = 404

**Expected Result (FAIL):**
- Returns null instead of throwing exception
- Returns empty SafetyCheckResponse instead of 404

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### PSC-TC-SEC-001 — No JWT returns 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `ExerciseController` — Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `BR-RBAC-001` (JWT required), `CB-EXERCISE-IMP-003 §10` (IAM-001)

**Preconditions:**
- FX-PSC-011: No Authorization header

**Test Steps (Attack Simulation):**
1. Arrange: No Authorization header
2. Act: `POST /api/v1/exercises/{UUID}/safety-check` without JWT
3. Assert: HTTP 401, error code IAM-001

**Expected Result (PASS = system secure):**
- HTTP 401
- Response: `{ "error": { "code": "IAM-001" } }`
- No safety check record created

**Expected Result (FAIL = vulnerability):**
- HTTP 200 or 201 without authentication (critical security breach)

**Current Status:** 🔴 Not written

---

### PSC-TC-SEC-002 — Non-MOTHER role → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ExerciseController` — Role-based access control
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `BR-RBAC-001`, `§16 Authorization Matrix` (EXPERT → ❌)

**Preconditions:**
- FX-PSC-012: JWT with `role=EXPERT`

**Test Steps (Attack Simulation):**
1. Arrange: JWT with `role=EXPERT`
2. Act: `POST /api/v1/exercises/{UUID}/safety-check` with EXPERT JWT
3. Assert: HTTP 403

**Expected Result (PASS = system secure):**
- HTTP 403 Forbidden
- Response: `{ "error": { "code": "IAM-002" } }`

**Expected Result (FAIL = vulnerability):**
- HTTP 201 allowing EXPERT to submit safety checks on behalf of Mothers

**Current Status:** 🔴 Not written

---

### PSC-TC-SEC-003 — answer_json does NOT appear in application logs

**Severity:** `CRITICAL`
**OWASP:** `A09:2021 — Security Logging and Monitoring Failures`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File`
**Legal:** `PDPA — health data must not be logged`
**Feature Under Test:** `ExerciseSafetyCheckService` — logging behavior
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckLogAuditTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `ADR-PSC-002` (answer_json not in logs), `PDPA` (health data protection)

**Preconditions:**
- Log appender captured (using `@CaptureSystemOutput` or TestLogCaptor)
- FX-PSC-003 (all-clear request with distinct values)

**Test Steps:**
1. Arrange: Capture log output; mock repository to return saved entity
2. Act: `safetyCheckService.submitSafetyCheck(...)` with all-clear request
3. Assert: Log output does NOT contain any of: `"answer_json"`, `"q1NoDizziness"`, `"q2NoContractions"`, `"q3NoBleeding"`, `"q4HydratedAndFed"`, `"Q1"`, `"Q2"`, `"Q3"`, `"Q4"` (when followed by `: ` and a boolean in context)

**Expected Result (PASS = secure):**
- No health answer data in log output
- Log contains only `safetyCheckId` and `resultStatus` at INFO level

**Expected Result (FAIL = PDPA violation):**
- Answer content appears in log output
- `answer_json` JSONB representation logged

**Current Status:** 🔴 Not written

---

### PSC-TC-SEC-004 — Mother cannot access another Mother's safety check

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExerciseSafetyCheckService.getLatestSafetyCheck()` — user isolation
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-PSC-002` (userId from JWT only — enforces user isolation), `BR-RBAC-001`, `§16 Auth Matrix`

**Preconditions:**
- FX-PSC-013: `MOTHER_USER_ID_2` JWT (different Mother)
- Safety check previously created by `MOTHER_USER_ID` for `EXERCISE_ID_PUBLISHED`

**Test Steps:**
1. Arrange: Seed 1 safety check for `MOTHER_USER_ID`. Create JWT for `MOTHER_USER_ID_2`.
2. Act: `GET /api/v1/exercises/{EXERCISE_ID_PUBLISHED}/safety-check/latest` with MOTHER_USER_ID_2 JWT
3. Assert: HTTP 404 (no record found for MOTHER_USER_ID_2 + EXERCISE_ID_PUBLISHED pair)

**Expected Result (PASS = user isolation enforced):**
- HTTP 404 PSC-002 (no safety check found for MOTHER_USER_ID_2)
- MOTHER_USER_ID_2 cannot see MOTHER_USER_ID's health records

**Expected Result (FAIL = data leak):**
- HTTP 200 returning MOTHER_USER_ID's safety check to MOTHER_USER_ID_2 (cross-user health data leak)

**Current Status:** 🔴 Not written
**Implementation Note:** The userId parameter passed to `getLatestSafetyCheck()` MUST be extracted from JWT only. If the repository correctly queries by `userId` from JWT, this test passes automatically. Never accept `userId` from request params.

---

### INTEGRATION TEST CASES

---

### PSC-TC-INT-001 — Full flow: CLEARED submission persisted correctly in DB

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: Controller → Service → Policy → Repository → DB (CLEARED path)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-009, TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migration applied automatically (V1 + index migration)
- Seed: FX-PSC-001 (PUBLISHED exercise)

**Test Steps:**
1. Seed 1 PUBLISHED exercise (FX-PSC-001)
2. `POST /api/v1/exercises/{EXERCISE_ID_PUBLISHED}/safety-check` with FX-PSC-003 (all true) and MOTHER JWT
3. Assert HTTP 201 with CLEARED result
4. Assert DB state

**Expected Result (PASS):**
- HTTP 201
- `data.resultStatus` = `"CLEARED"`
- `data.redFlagDetected` = `false`
- `data.blockedReason` = null
- `data.completedAt` not null
- `data.safetyCheckId` is a valid UUID

**Expected Result (FAIL):**
- HTTP error
- `data.resultStatus` = `"BLOCKED"` despite all-clear answers
- `data.completedAt` = null despite CLEARED

**DB Assertion:**
```java
// Verify record persisted with correct state
ExerciseSafetyCheck fromDb = safetyCheckRepository
    .findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(EXERCISE_ID_PUBLISHED, MOTHER_USER_ID)
    .orElseThrow(() -> new AssertionError("Safety check not found in DB"));

// Oracle: V1__init_schema.sql columns + ADR-PSC-001 invariants
assertThat(fromDb.getResultStatus()).isEqualTo(SafetyCheckStatus.CLEARED);
assertThat(fromDb.getRedFlagDetected()).isFalse();
assertThat(fromDb.getBlockedReason()).isNull();
assertThat(fromDb.getCompletedAt()).isNotNull();   // L4: set when CLEARED
assertThat(fromDb.getAnswerJson()).containsEntry("Q1", true)
    .containsEntry("Q2", true)
    .containsEntry("Q3", true)
    .containsEntry("Q4", true);
```

**Current Status:** 🔴 Not written

---

### PSC-TC-INT-002 — Full flow: BLOCKED submission persisted correctly in DB

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: Controller → Service → Policy → Repository → DB (BLOCKED path)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002, TC-COND-010, TC-COND-013`

**Preconditions:**
- Testcontainer running; PUBLISHED exercise seeded
- FX-PSC-004 (Q1=false)

**Test Steps:**
1. Seed PUBLISHED exercise
2. `POST /api/v1/exercises/{EXERCISE_ID_PUBLISHED}/safety-check` with Q1=false
3. Assert HTTP 201 (not 4xx!) with BLOCKED result
4. Assert DB state

**Expected Result (PASS):**
- HTTP 201
- `data.resultStatus` = `"BLOCKED"`
- `data.redFlagDetected` = `true`
- `data.blockedReason` not null, not empty
- `data.blockedReason` contains "doctor" and "midwife"
- `data.completedAt` = null (L4: null when BLOCKED)

**Expected Result (FAIL):**
- HTTP 422 or 400 for BLOCKED result (L5 logic issue — BLOCKED is 201)
- `data.completedAt` not null when BLOCKED (invariant violation)

**DB Assertion:**
```java
ExerciseSafetyCheck fromDb = safetyCheckRepository
    .findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(EXERCISE_ID_PUBLISHED, MOTHER_USER_ID)
    .orElseThrow();

// Oracle: V1__init_schema.sql + ADR-PSC-001 + ADR-PSC-003
assertThat(fromDb.getResultStatus()).isEqualTo(SafetyCheckStatus.BLOCKED);
assertThat(fromDb.getRedFlagDetected()).isTrue();
assertThat(fromDb.getBlockedReason()).isNotNull().isNotBlank();
assertThat(fromDb.getBlockedReason().toLowerCase()).contains("doctor");
assertThat(fromDb.getBlockedReason().toLowerCase()).contains("midwife");
assertThat(fromDb.getCompletedAt()).isNull();  // L4: null when BLOCKED
assertThat(fromDb.getAnswerJson()).containsEntry("Q1", false);
```

**Current Status:** 🔴 Not written

---

### PSC-TC-INT-003 — GET /latest returns most recent check after multiple submissions

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSafetyCheckService.getLatestSafetyCheck()` ordering
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ExerciseSafetyCheckRepository.findTopByExerciseIdAndUserIdOrderByCreatedAtDesc()` contract (§8.4)

**Preconditions:**
- Testcontainer running; PUBLISHED exercise seeded

**Test Steps:**
1. Submit safety check with Q1=false (BLOCKED) — first submission
2. Submit safety check with all-true (CLEARED) — second submission (more recent)
3. `GET /api/v1/exercises/{EXERCISE_ID_PUBLISHED}/safety-check/latest`
4. Assert: response reflects the CLEARED check (most recent)

**Expected Result (PASS):**
- HTTP 200
- `data.resultStatus` = `"CLEARED"` (most recent submission was CLEARED)
- `data.safetyCheckId` matches the second submission's ID

**Expected Result (FAIL):**
- Returns the first BLOCKED check instead of the most recent CLEARED one

**DB Assertion:**
```java
// Verify 2 records exist
assertThat(safetyCheckRepository.findAll().size()).isEqualTo(2);

// Verify GET /latest returns the most recent
MvcResult result = mockMvc.perform(get("/api/v1/exercises/{id}/safety-check/latest",
        EXERCISE_ID_PUBLISHED)
    .header("Authorization", "Bearer " + motherJwt))
    .andExpect(status().isOk())
    .andReturn();

JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
assertThat(data.get("resultStatus").asText()).isEqualTo("CLEARED");
```

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES

---

### PSC-TC-MOB-001 — All 4 checkboxes checked → API call submitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `PreExerciseSafetyCheckScreen` — API wiring on submit
**Test File:** `test/features/exercise/pre_exercise_safety_check_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `L3` (Navigator.pop(true) must be replaced by API call), `pre_exercise_safety_check_screen.dart` (existing 4 checkboxes), `§9.2 API Spec`

**Preconditions:**
- Flutter test environment with mock HTTP client
- `exerciseId` (UUID) provided to screen constructor
- All 4 check items tapped to checked state

**Test Steps:**
1. Arrange: Provide `exerciseId` and mock HTTP client returning CLEARED response
2. Act: Tap all 4 checkboxes; tap "Bắt đầu buổi tập" button
3. Assert: HTTP mock received exactly 1 POST request to `/api/v1/exercises/{exerciseId}/safety-check`

**Expected Result (PASS):**
- Mock HTTP client receives 1 POST to correct endpoint
- Request body contains `{ "q1NoDizziness": true, "q2NoContractions": true, "q3NoBleeding": true, "q4HydratedAndFed": true }`
- Authorization header present with Bearer token

**Expected Result (FAIL):**
- `Navigator.pop(true)` called without API call (old behavior — L3 issue)
- Wrong endpoint called
- Answers missing from request body

**Current Status:** 🔴 Not written
**Implementation Note:** Screen must accept `exerciseId: String` as required constructor parameter. Replace `Navigator.of(context).pop(true)` with `_submitSafetyCheck()` async method calling the API.

---

### PSC-TC-MOB-002 — BLOCKED API response displays blockedReason to Mother

**Severity:** `CRITICAL`
**Feature Under Test:** `PreExerciseSafetyCheckScreen` — BLOCKED state UI rendering
**Test File:** `test/features/exercise/pre_exercise_safety_check_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `ADR-PSC-003` (blocked_reason must be shown to Mother), `§9.2 API Spec` (BLOCKED response shape)

**Preconditions:**
- Mock HTTP client returns BLOCKED response:
  ```json
  { "data": { "resultStatus": "BLOCKED", "redFlagDetected": true, "blockedReason": "Dizziness or faintness was reported. Please rest and consult your doctor or midwife." } }
  ```

**Test Steps:**
1. Arrange: Mock HTTP returns BLOCKED response; all 4 checkboxes ticked
2. Act: Tap "Bắt đầu buổi tập" button
3. Assert: `blockedReason` text visible on screen

**Expected Result (PASS):**
- UI shows the `blockedReason` text (or a localized version)
- "Bắt đầu buổi tập" button is still disabled or redirects to a blocked state
- Mother is NOT navigated to exercise session

**Expected Result (FAIL):**
- Screen navigates to exercise session despite BLOCKED result
- `blockedReason` not displayed to Mother

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PSC-TC-001` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-002` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-003` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | Use @ParameterizedTest |
| `PSC-TC-004` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-005` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-006` | `SafetyCheckPolicyTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-007` | `ExerciseSafetyCheckServiceTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-008` | `ExerciseSafetyCheckControllerTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-009` | `ExerciseSafetyCheckServiceTest.java` | `[ ]` | _(pending)_ | |
| `PSC-TC-SEC-001` | `ExerciseSafetyCheckControllerSecurityTest.java` | `[ ]` | _(pending)_ | Spring Security layer |
| `PSC-TC-SEC-002` | `ExerciseSafetyCheckControllerSecurityTest.java` | `[ ]` | _(pending)_ | RBAC layer |
| `PSC-TC-SEC-003` | `ExerciseSafetyCheckLogAuditTest.java` | `[ ]` | _(pending)_ | Log capture |
| `PSC-TC-SEC-004` | `ExerciseSafetyCheckControllerSecurityTest.java` | `[ ]` | _(pending)_ | User isolation |
| `PSC-TC-INT-001` | `ExerciseSafetyCheckIntegrationTest.java` | `[ ]` | _(pending)_ | Testcontainers |
| `PSC-TC-INT-002` | `ExerciseSafetyCheckIntegrationTest.java` | `[ ]` | _(pending)_ | Testcontainers |
| `PSC-TC-INT-003` | `ExerciseSafetyCheckIntegrationTest.java` | `[ ]` | _(pending)_ | Testcontainers |
| `PSC-TC-MOB-001` | `pre_exercise_safety_check_screen_test.dart` | `[ ]` | _(pending)_ | Flutter widget test |
| `PSC-TC-MOB-002` | `pre_exercise_safety_check_screen_test.dart` | `[ ]` | _(pending)_ | Flutter widget test |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Before implementing production code, run the full test suite against empty/throw stubs.
> ALL tests MUST FAIL. If any test PASSes immediately → AP-AI-002 detected → reject and rewrite.

**Stub for Red Phase (Backend):**

```java
// Red Phase — service stub (MUST throw on all methods)
@Service
public class ExerciseSafetyCheckService implements IExerciseSafetyCheckService {

    @Override
    public ApiResponse<SafetyCheckResponse> submitSafetyCheck(
            UUID exerciseId, SubmitSafetyCheckRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ApiResponse<SafetyCheckResponse> getLatestSafetyCheck(UUID exerciseId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Red Phase — policy stub (MUST throw on all methods)
public class SafetyCheckPolicy {

    public EvaluationResult evaluate(Map<SafetyQuestion, Boolean> answers) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    public String buildBlockedReason(List<SafetyQuestion> flaggedQuestions) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Stub for Red Phase (Mobile):**

```dart
// Red Phase stub — submitSafetyCheck is not connected
// PreExerciseSafetyCheckScreen still calls Navigator.pop(true) (old behavior)
// All mobile tests that expect API call will FAIL
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause if PASS unexpectedly |
|-------|-------------|----------|--------|----------------------------------|
| `PSC-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | _(fill on execution)_ |
| `PSC-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-008` | DTO @NotNull validation | 🔴 FAIL | ☐ FAIL ☐ PASS | Controller infra — validate separately |
| `PSC-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-SEC-001` | Spring Security config | Depends on Spring Security setup | ☐ FAIL ☐ PASS | Security infra |
| `PSC-TC-SEC-002` | Spring Security RBAC | Depends on Security setup | ☐ FAIL ☐ PASS | Security infra |
| `PSC-TC-SEC-003` | Stub service — log audit | 🔴 FAIL (stub throws) | ☐ FAIL ☐ PASS | |
| `PSC-TC-SEC-004` | Stub service | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-INT-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PSC-TC-MOB-001` | Old Navigator.pop(true) behavior | 🔴 FAIL (API call not made) | ☐ FAIL ☐ PASS | |
| `PSC-TC-MOB-002` | No blockedReason UI | 🔴 FAIL (widget not found) | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` _(fill before implementation)_
- All service/policy tests FAIL? `☐ Yes → GATE-2 PASS (T2→T3) → proceed to implementation`
- Log file: `[path to red-gate-evidence.log]` _(fill after stub run)_

> **If any test PASSes unexpectedly:** Stop. Identify root cause from the table above. Common causes: tautological assertion, shared state from another test, Spring infra test that doesn't touch the stub. Rewrite test using Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Pre-conditions to Start)

- [ ] TDS `CB-EXERCISE-IMP-003 v1.0` has been reviewed and this Test-Spec is approved
- [ ] Logic Issues (Section 2, L1–L7) confirmed with Principal Architect
- [ ] `CB-EXERCISE-IMP-002` (UC-177 View Exercise Detail) implemented and passing — `ExerciseRepository` and `ExerciseNotFoundException` must exist in `com.carebridge.backend.exercise`
- [ ] V1 migration with `exercise_safety_checks` table applied on dev environment
- [ ] Flyway migration for index (`V{n}__add_safety_check_query_index.sql`) ready and tested on staging
- [ ] Test fixtures (§3 TDS-05) prepared in `SafetyCheckTestFactory`
- [ ] Flutter dev environment configured (`flutter test` runs without error on existing tests)

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — all unit tests green (no skips)
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] `flutter test` — all widget tests green
- [ ] Test coverage ≥ 80% lines for `ExerciseSafetyCheckService` and `SafetyCheckPolicy`
- [ ] No business logic in `ExerciseController` safety-check methods (only DTO validation + userId extraction + delegate)
- [ ] `SafetyCheckPolicy` is the ONLY class with red-flag evaluation logic
- [ ] All-true submission returns CLEARED with `completedAt` set — verified by PSC-TC-001, PSC-TC-INT-001
- [ ] Any-false submission returns BLOCKED with `completedAt` null — verified by PSC-TC-002, PSC-TC-INT-002
- [ ] `blockedReason` never null when BLOCKED — verified by PSC-TC-002, PSC-TC-INT-002
- [ ] `blockedReason` contains "doctor" and "midwife" — verified by PSC-TC-005
- [ ] `blockedReason` free of diagnostic language — verified by PSC-TC-006
- [ ] BLOCKED result returns HTTP 201 (not 4xx) — verified by PSC-TC-INT-002
- [ ] `answer_json` absent from logs — verified by PSC-TC-SEC-003
- [ ] User isolation: Mother cannot access another Mother's check — verified by PSC-TC-SEC-004
- [ ] Mobile screen calls API on submit — verified by PSC-TC-MOB-001
- [ ] Mobile screen shows blockedReason when BLOCKED — verified by PSC-TC-MOB-002

**CASE 2.0 Exit Criteria:**

- [ ] **Red Gate (§5.1)** — ALL service/policy tests FAIL with throw stubs before implementation
- [ ] **Contract Existence** — all injected classes compile:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — no shared mutable state between tests:
  ```bash
  # Every test class uses SafetyCheckTestFactory — no @BeforeEach mutable setup
  grep -n "private.*= new\|private static.*= new" src/test/java/com/carebridge/backend/exercise/SafetyCheck*Test.java
  # All instances should be inside @Test or factory methods
  ```
- [ ] **Oracle Source** — every `assertThat` expected value has a BR/ADR/schema citation in this document

### Suspension Criteria (Pause Conditions)

- `CB-EXERCISE-IMP-002` shared repository/entity not yet implemented
- V1 migration not applied on test environment
- IAM/JWT module not ready for security test configuration
- Architectural concern raised by Principal Architect about policy placement or PDPA handling

---

## 7. Rollback Plan

```bash
# Rollback: Revert test and implementation files for CB-EXERCISE-IMP-003 only.
# No table structure to revert (ADR-PSC-004: existing V1 table used).
# Only the query index migration needs revert.

# Step 1: Revert query index migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_safety_checks_exercise_user_created;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

# Step 2: Revert backend implementation files
git checkout -- src/main/java/com/carebridge/backend/exercise/entity/ExerciseSafetyCheck.java
git checkout -- src/main/java/com/carebridge/backend/exercise/entity/SafetyCheckStatus.java
git checkout -- src/main/java/com/carebridge/backend/exercise/entity/SafetyQuestion.java
git checkout -- src/main/java/com/carebridge/backend/exercise/policy/SafetyCheckPolicy.java
git checkout -- src/main/java/com/carebridge/backend/exercise/dto/SubmitSafetyCheckRequest.java
git checkout -- src/main/java/com/carebridge/backend/exercise/dto/SafetyCheckResponse.java
git checkout -- src/main/java/com/carebridge/backend/exercise/mapper/SafetyCheckMapper.java
git checkout -- src/main/java/com/carebridge/backend/exercise/repository/ExerciseSafetyCheckRepository.java
git checkout -- src/main/java/com/carebridge/backend/exercise/service/IExerciseSafetyCheckService.java
git checkout -- src/main/java/com/carebridge/backend/exercise/service/ExerciseSafetyCheckService.java
git checkout -- src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java

# Step 3: Revert test files
git checkout -- src/test/java/com/carebridge/backend/exercise/SafetyCheckPolicyTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckControllerSecurityTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckLogAuditTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckIntegrationTest.java

# Step 4: Revert mobile files
git checkout -- lib/features/exercise/screens/pre_exercise_safety_check_screen.dart
git checkout -- test/features/exercise/pre_exercise_safety_check_screen_test.dart

# Note: Data in exercise_safety_checks table is NOT rolled back (append-only audit records).
# CB-EXERCISE-IMP-002 (UC-177 detail) is NOT affected by this rollback.
# Gap remains OPEN — mark CB-EXERCISE-IMP-003 as reverted in sprint tracking.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> Checklist for reviewer when test cases were AI-assisted. Skip if tests were 100% human-written.

| AP-ID | Anti-Pattern | Indicator in This TDD Spec | Check | Gate |
|-------|-------------|---------------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASSes with empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test allows partial clearance (e.g., 3 of 4 pass → CLEARED) without ADR for this | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies red-flag logic in Controller or Service instead of SafetyCheckPolicy | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExerciseSessionService`, `SessionStartService`, or any CB-153 class | ☐ | G-3 |
| AP-AI-006 | PII Logging Blindspot | Test does not include PSC-TC-SEC-003 (log audit) — health data logging unchecked | ☐ | G-0 |
| AP-AI-007 | Diagnostic Language Blindspot | Test does not include PSC-TC-006 (blocked_reason language check) | ☐ | G-0 |

**Review result:**

- [ ] No anti-patterns detected → TDD spec approved for implementation
- [ ] Anti-pattern detected → record below → fix before implementation

| AP detected | TC ID | Description | Fix action | Fixed? |
|------------|-------|-------------|------------|--------|
| _(none detected at spec authoring time)_ | — | — | — | — |

---

*TDD Spec CB-EXERCISE-IMP-003-TEST v1.0 — Integrates CASE 2.0 Anti-Pattern Detection, Red Gate Protocol, and Props Isolation.*
*Sections marked with ⭐ are CASE 2.0 additions.*
*Oracle source for every expected value is cited (BR/ADR/schema).*
*Status: Draft — pending Principal Architect and DPO review before implementation begins.*
