# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# SRS 3.3.2.8 — Complete Exercise Session

**Document ID:** `CB-EXERCISE-TS-005`
**Version:** `1.0`
**Date:** `2026-06-28`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Developer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC182_CompleteExerciseSession/UC182_CompleteExerciseSession_TDS.md` (CB-EXERCISE-IMP-005)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.2.8`

> **TDD Convention:** Tests written BEFORE production code. Run → confirm 🔴 FAIL → implement → 🟢 PASS → refactor 🔵.
> SYNTHETIC data only — no real PII.

---

## CHANGELOG

| Date | Author | Change |
|------|--------|--------|
| `2026-06-28` | `AI Agent` | Initial creation — TDD spec for UC182 Complete Exercise Session |

---

## TABLE OF CONTENTS

1. [Module Info](#1-module-info)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature ID** | `SRS 3.3.2.8 / UC-182` |
| **Module** | `exercise — Complete Exercise Session` |
| **Spec source** | `CB-EXERCISE-IMP-005` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S2 (Sprint 2)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY, BR-PRIVACY` |
| **Upstream Dependencies** | `IAM (JWT), UC179 (exercise_sessions), UC181 (pause/resume), posture_feedback_events (V1)` |
| **Downstream Consumers** | `UC183 ViewExerciseSessionResult, NotificationService (ExerciseSessionCompleted event)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-005 §17`, `ADR-CES-001 through ADR-CES-004` |
| **Constraints Injected** | `C1 (state policy class), C2 (owner check 403), C3 (duration calc), C4 (posture score AVG), C5 (summary_json HIGH/LOW), C6 (event emission), C7 (completion_percent capped 100)` |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec Issue | Reality | Fix in Test |
|---|------------|---------|-------------|
| L1 | `completion_percent` described as capped at 100 | BR-EXSESS-004 confirms cap at `100.00`; sessions running longer than planned should not show >100% | Tests must include case where actual_duration > planned_duration and assert completion_percent = 100.00 |
| L2 | `posture_score` described as AVG(confidence_score) | V1 column is `posture_score numeric` — nullable; when no posture_feedback_events exist, posture_score should be null (not 0) | Test: complete session with zero posture events → assert posture_score IS NULL |
| L3 | `summary_json.issues` uses severity = HIGH | `posture_feedback_events.severity` is `VARCHAR(20)` in V1 — case-sensitive; code must use exact case | Tests must seed events with `severity='HIGH'` and `severity='LOW'` in exact uppercase |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
exercise module — Complete Session flow:
├── Domain (policy — pure logic)
│   └── ExerciseSessionStatePolicy.assertCanComplete()
├── Service (mock repos — Mockito)
│   └── ExerciseSessionServiceImpl.completeSession()
│       ├── calculateDuration()
│       ├── calculateCompletionPercent()
│       ├── calculatePostureScore()
│       └── buildSummaryJson()
├── Controller (mock service — @WebMvcTest)
│   └── ExerciseSessionController.PATCH /sessions/{id}/complete
└── Integration (Testcontainers PostgreSQL)
    └── Full PATCH /complete with real DB + posture_feedback_events
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.8 UC-182` | End session, save duration/completion/posture/issues |
| `BR-EXSESS-001` | Only IN_PROGRESS or PAUSED sessions can be completed |
| `BR-EXSESS-002` | Only session owner may complete |
| `BR-EXSESS-003` | `actual_duration_seconds = (ended_at − started_at) − paused_seconds` |
| `BR-EXSESS-004` | `completion_percent = min(actual_duration / planned_duration * 100, 100.00)` |
| `BR-EXSESS-005` | `posture_score = AVG(confidence_score)` from `posture_feedback_events` |
| `BR-EXSESS-006` | `summary_json.issues` = HIGH severity posture_codes; `highlights` = LOW severity |
| `BR-EXSESS-007` | Emit `ExerciseSessionCompleted` Spring application event on success |
| `ADR-CES-001` | State guard in dedicated policy class |
| `ADR-CES-002` | Owner check before mutation → 403 on mismatch |
| `ADR-CES-004` | Synchronous Spring event via `ApplicationEventPublisher` |
| `V1__init_schema.sql` | `exercise_sessions`, `posture_feedback_events`, `pregnancy_exercises` table structure |
| `CB-EXERCISE-IMP-005 §9` | `PATCH /api/v1/exercises/sessions/{sessionId}/complete` → 200 |
| `CB-EXERCISE-IMP-005 §10` | Errors: EXSESS-001 (404), EXSESS-002 (409), EXSESS-003 (400), EXSESS-004 (403) |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|------------|
| TC-COND-CES-001 | Complete IN_PROGRESS session — happy path | Full completion flow | CES-TC-001 |
| TC-COND-CES-002 | Complete PAUSED session — also valid | State: PAUSED → COMPLETED | CES-TC-002 |
| TC-COND-CES-003 | Duration calculated correctly | BR-EXSESS-003 | CES-TC-001, CES-TC-003 |
| TC-COND-CES-004 | completion_percent capped at 100.00 | BR-EXSESS-004 boundary | CES-TC-003 |
| TC-COND-CES-005 | posture_score = AVG(confidence_score) when events exist | BR-EXSESS-005 | CES-TC-001 |
| TC-COND-CES-006 | posture_score = null when no events | BR-EXSESS-005 edge case | CES-TC-004 |
| TC-COND-CES-007 | summary_json.issues = HIGH severity posture_codes | BR-EXSESS-006 | CES-TC-005 |
| TC-COND-CES-008 | ExerciseSessionCompleted event published on success | BR-EXSESS-007 | CES-TC-006 |
| TC-COND-CES-009 | Complete already-COMPLETED session → 409 EXSESS-002 | State guard | CES-TC-007 |
| TC-COND-CES-010 | Complete ABANDONED session → 409 EXSESS-002 | State guard | CES-TC-008 |
| TC-COND-CES-011 | Complete by non-owner → 403 EXSESS-004 | IDOR prevention | CES-TC-009 |
| TC-COND-CES-012 | Non-existent sessionId → 404 EXSESS-001 | Not found | CES-TC-010 |
| TC-COND-CES-013 | Full completion cycle integration test | DB persistence + event | CES-TC-INT-001 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | IN_PROGRESS→COMPLETED, PAUSED→COMPLETED, COMPLETED→COMPLETED (invalid) | Exhaustive state machine coverage |
| Boundary Value Analysis | completion_percent: exactly 100% (at-target) and >100% (over-target) | Cap-at-100 boundary |
| Error Guessing | IDOR via sessionId in path | Critical security risk |
| Equivalence Partitioning | Posture events: 0 events (null score), 1+ events (computed score) | Null vs. computed branch |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Purpose |
|-----------|------|-------|---------|
| `FX-CES-001` | DB seed | `exercise_sessions`: `status=IN_PROGRESS`, `user_id=USER_A`, `started_at=T-20min`, `paused_seconds=0`, exercise `duration_minutes=20` | Happy path — 100% completion |
| `FX-CES-002` | DB seed | Same as FX-CES-001 but `status=PAUSED` | PAUSED→COMPLETED path |
| `FX-CES-003` | DB seed | `exercise_sessions`: `status=IN_PROGRESS`, `started_at=T-30min`, `paused_seconds=0`, exercise `duration_minutes=20` | Over-target → capped at 100% |
| `FX-CES-004` | DB seed | Session with no associated `posture_feedback_events` | Null posture score test |
| `FX-CES-005` | DB seed | `posture_feedback_events`: 3 rows for session — confidences [0.8, 0.6, 0.7], severities ['LOW','HIGH','LOW'] | AVG = 0.7; issues=['HIGH_CODE']; highlights=['LOW_CODE1','LOW_CODE2'] |
| `FX-CES-006` | DB seed | `exercise_sessions`: `status=COMPLETED` | Double-complete → 409 |
| `FX-CES-007` | DB seed | `exercise_sessions`: `status=ABANDONED` | Abandoned → 409 |
| `FX-CES-008` | DB seed | Session owned by USER_B | IDOR test |
| `FX-CES-009` | JWT | `{ sub: USER_A_ID, role: 'MOTHER' }` | Owner auth |
| `FX-CES-010` | JWT | `{ sub: USER_B_ID, role: 'MOTHER' }` | Attacker auth |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

```java
// CompleteExerciseSessionTestFactory.java
class CompleteExerciseSessionTestFactory {

    static final UUID USER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final Instant SESSION_START = Instant.now().minus(20, ChronoUnit.MINUTES);
    static final int PLANNED_DURATION_MINUTES = 20;

    static ExerciseSession makeInProgressSession() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(SESSION_ID);
        s.setUserId(USER_A_ID);
        s.setExerciseId(EXERCISE_ID);
        s.setSessionStatus(SessionStatus.IN_PROGRESS);
        s.setStartedAt(SESSION_START);
        s.setPausedSeconds(0);
        s.setWarningCount(0);
        return s;
    }

    static ExerciseSession makeInProgressSession(Consumer<ExerciseSession> overrides) {
        ExerciseSession s = makeInProgressSession();
        overrides.accept(s);
        return s;
    }

    static PregnancyExercise makeExercise() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(EXERCISE_ID);
        e.setDurationMinutes((short) PLANNED_DURATION_MINUTES);
        e.setTitle("Yoga Prenatal");
        return e;
    }

    static PostureFeedbackEvent makePostureEvent(UUID sessionId, String posture, float confidence, String severity) {
        PostureFeedbackEvent ev = new PostureFeedbackEvent();
        ev.setFeedbackEventId(UUID.randomUUID());
        ev.setExerciseSessionId(sessionId);
        ev.setPostureCode(posture);
        ev.setConfidenceScore(new BigDecimal(String.valueOf(confidence)));
        ev.setSeverity(severity);
        ev.setEventTimeMs(1000L);
        return ev;
    }
}
```

---

### CES-TC-001 — Complete IN_PROGRESS session — duration and posture score calculated

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-001, TC-COND-CES-003, TC-COND-CES-005`
**Oracle Source:** `BR-EXSESS-003, BR-EXSESS-004, BR-EXSESS-005`

**Preconditions:**
- Session: IN_PROGRESS, started 20 min ago, paused_seconds=0 (FX-CES-001)
- 3 posture events with confidence scores [0.8, 0.6, 0.7] (FX-CES-005)
- Planned duration = 20 min

**Test Steps:**
1. Arrange: mock repo returns session; mock posture repo returns events; mock exercise repo returns exercise; fix clock at T+20min from start
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert:
   - Saved session: `sessionStatus = COMPLETED`, `endedAt` set
   - `completionPercent = 100.00` (20min actual / 20min planned * 100, capped at 100)
   - `postureScore` ≈ 0.70 (average of 0.8, 0.6, 0.7)
   - Response contains all computed fields

**Expected Result (PASS):**
- `sessionStatus=COMPLETED`, `completionPercent=100.00`, `postureScore` ≈ 0.70

**Current Status:** 🔴 Not written

---

### CES-TC-002 — Complete PAUSED session — also valid

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionStatePolicy.assertCanComplete()` — PAUSED path
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-002`
**Oracle Source:** `BR-EXSESS-001 — "IN_PROGRESS or PAUSED sessions can be completed"`

**Preconditions:**
- Session: PAUSED, paused_seconds=300 (FX-CES-002)

**Test Steps:**
1. Arrange: mock repo returns PAUSED session
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: no `ExerciseSessionStateException` thrown; saved `sessionStatus = COMPLETED`

**Expected Result (PASS):** `sessionStatus = COMPLETED`

**Current Status:** 🔴 Not written

---

### CES-TC-003 — completion_percent capped at 100.00 when session runs over

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionService.calculateCompletionPercent()` boundary
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-004`
**Oracle Source:** `BR-EXSESS-004 — "capped at 100"`

**Preconditions:**
- Session: started 30 min ago, paused_seconds=0, planned_duration=20min (FX-CES-003)

**Test Steps:**
1. Arrange: session started 30 min ago; exercise duration=20 min; paused_seconds=0
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: `completionPercent` is exactly `100.00` — NOT `150.00`

**Expected Result (PASS):** `completionPercent = 100.00`

**Current Status:** 🔴 Not written

---

### CES-TC-004 — posture_score is null when no posture feedback events exist

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionService.calculatePostureScore()` — empty events
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-006`
**Oracle Source:** `L2 (Logic Issues) — null when no events per V1 schema nullable posture_score`

**Preconditions:**
- Session: IN_PROGRESS with NO posture_feedback_events (FX-CES-004)

**Test Steps:**
1. Arrange: mock posture repo returns empty list
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: saved `postureScore IS NULL`; response `postureScore = null` (not 0)

**Expected Result (PASS):** `postureScore = null`

**Current Status:** 🔴 Not written

---

### CES-TC-005 — summary_json issues = HIGH severity posture codes

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionService.buildSummaryJson()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-007`
**Oracle Source:** `BR-EXSESS-006 — "issues = posture_codes with severity = HIGH"`

**Preconditions:**
- 3 posture events: posture_code=[A(HIGH), B(LOW), C(HIGH)] (FX-CES-005 variant)

**Test Steps:**
1. Arrange: 3 posture events with different severities
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert:
   - `summaryJson.issues` contains `["A", "C"]` (HIGH severity codes only)
   - `summaryJson.highlights` contains `["B"]` (LOW severity codes only)

**Expected Result (PASS):** `issues=["A","C"]`, `highlights=["B"]`

**Current Status:** 🔴 Not written

---

### CES-TC-006 — ExerciseSessionCompleted event published on success

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionService` → `ApplicationEventPublisher.publishEvent()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-008`
**Oracle Source:** `BR-EXSESS-007, ADR-CES-004`

**Preconditions:**
- Valid IN_PROGRESS session

**Test Steps:**
1. Arrange: mock `ApplicationEventPublisher`; inject into service
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: `publisher.publishEvent(any(ExerciseSessionCompletedEvent.class))` called exactly once

**Expected Result (PASS):** Event published exactly once with `sessionId` in payload

**Current Status:** 🔴 Not written

---

### CES-TC-007 — Complete already-COMPLETED session → 409 EXSESS-002

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionStatePolicy.assertCanComplete()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-009`
**Oracle Source:** `BR-EXSESS-001, CB-EXERCISE-IMP-005 §10 EXSESS-002`

**Test Steps:**
1. Arrange: session with `sessionStatus = COMPLETED` (FX-CES-006)
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: throws `ExerciseSessionStateException` with code `EXSESS-002`; no DB write

**Expected Result (PASS):** Exception `EXSESS-002`

**Current Status:** 🔴 Not written

---

### CES-TC-008 — Complete ABANDONED session → 409 EXSESS-002

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionStatePolicy.assertCanComplete()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-010`
**Oracle Source:** `BR-EXSESS-001`

**Test Steps:**
1. Session with `sessionStatus = ABANDONED` (FX-CES-007)
2. Act: `service.completeSession(SESSION_ID, USER_A_ID)`
3. Assert: throws `ExerciseSessionStateException` code `EXSESS-002`

**Expected Result (PASS):** Exception `EXSESS-002`

**Current Status:** 🔴 Not written

---

### CES-TC-009 — Complete by non-owner → 403 EXSESS-004 (IDOR prevention)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExerciseSessionService` ownership check
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-011`
**Oracle Source:** `ADR-CES-002, CB-EXERCISE-IMP-005 §10 EXSESS-004`

**Test Steps:**
1. Session owned by USER_A; authenticatedUserId = USER_B (FX-CES-008, FX-CES-010)
2. Act: `service.completeSession(SESSION_ID, USER_B_ID)`
3. Assert: throws `AccessDeniedBusinessException` code `EXSESS-004`; `save()` never called

**Expected Result (PASS):** `EXSESS-004` thrown, no mutation

**Expected Result (FAIL):** Session completed by attacker — critical vulnerability

**Current Status:** 🔴 Not written

---

### CES-TC-010 — Non-existent sessionId → 404 EXSESS-001

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionRepository.findById()` not-found path
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-012`
**Oracle Source:** `CB-EXERCISE-IMP-005 §10 EXSESS-001`

**Test Steps:**
1. Mock repo → `Optional.empty()`
2. Act: `service.completeSession(RANDOM_UUID, USER_A_ID)`
3. Assert: throws `ResourceNotFoundException` code `EXSESS-001`

**Current Status:** 🔴 Not written

---

### CES-TC-INT-001 — Full completion integration with real DB and posture events

**Severity:** `HIGH`
**Feature Under Test:** Complete exercise session end-to-end with Testcontainers
**Test File:** `src/test/java/com/carebridge/backend/exercise/CompleteExerciseSessionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CES-013`

**Preconditions:**
- PostgreSQL Testcontainer running; Flyway V1 applied
- Seed: pregnancy_exercise (PUBLISHED, duration_minutes=15), exercise_safety_check (CLEARED), exercise_session (IN_PROGRESS, started 15min ago)
- Seed: 2 posture_feedback_events with confidence [0.9, 0.8], severity ['HIGH','LOW']

**Test Steps:**
1. Seed all required rows
2. `PATCH /api/v1/exercises/sessions/{sessionId}/complete` with USER_A JWT
3. Assert response 200, `sessionStatus = "COMPLETED"`, `completionPercent = 100.00`, `postureScore ≈ 0.85`
4. Query DB: `SELECT * FROM exercise_sessions WHERE exercise_session_id = ?` → verify `session_status = 'COMPLETED'`, `ended_at IS NOT NULL`, `posture_score IS NOT NULL`

**DB Assertion:**
```java
ExerciseSession saved = repo.findById(sessionId).orElseThrow();
assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
assertThat(saved.getEndedAt()).isNotNull();
assertThat(saved.getPostureScore()).isNotNull();
assertThat(saved.getCompletionPercent()).isEqualByComparingTo("100.00");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CES-TC-001` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-002` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-003` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-004` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-005` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-006` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-007` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-008` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-009` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-010` | `CompleteExerciseSessionServiceTest.java` | `[ ]` | — | — |
| `CES-TC-INT-001` | `CompleteExerciseSessionIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Red Phase stub:**
```java
@Service
public class ExerciseSessionServiceImpl implements IExerciseSessionService {

    @Override
    public CompleteSessionResponse completeSession(UUID sessionId, UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (if unexpected PASS) |
|-------|----------|--------|----------------------------------|
| `CES-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `CES-TC-003` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `CES-TC-007` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `CES-TC-009` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `CES-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- All FAIL? ☐ Yes → **GATE-2 PASS** → proceed to implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] `CB-EXERCISE-IMP-005` TDS reviewed and approved
- [ ] UC179 (StartExerciseSession) implemented — `exercise_sessions` table populated
- [ ] UC178 (SafetyCheck) implemented — `exercise_safety_checks` table populated
- [ ] V1 Flyway migration applied (`exercise_sessions`, `posture_feedback_events`)
- [ ] Test fixtures prepared

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=CompleteExerciseSessionServiceTest` — all 10 TCs green
- [ ] `./mvnw verify -Dtest=CompleteExerciseSessionIntegrationTest` — integration green
- [ ] CES-TC-009 (IDOR) MUST pass
- [ ] CES-TC-006 (event emission) MUST pass
- [ ] `completionPercent` cap behavior verified (CES-TC-003)
- [ ] No PII in logs

**CASE 2.0 Exit:**
- [ ] Red Gate all FAIL with stub
- [ ] `./mvnw compile 2>&1 | grep "error:"` → empty
- [ ] Props Isolation enforced in all test files
- [ ] Oracle source cited for every `assertThat`

---

## 7. Rollback Plan

```bash
# No migration to rollback — V1 schema unchanged
git checkout -- src/main/java/com/carebridge/backend/exercise/service/impl/ExerciseSessionServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/exercise/policy/ExerciseSessionStatePolicy.java
git checkout -- src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java
git checkout -- src/test/java/com/carebridge/backend/exercise/service/CompleteExerciseSessionServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/CompleteExerciseSessionIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Warning Sign | Check | Gate |
|-------|-------------|--------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any BR or ADR | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test passes with `throw UnsupportedOperationException` stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `posture_score = 0` when no events (should be null) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test verifies duration/completion calculations | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test uses `ExerciseSessionStatePolicy` before it exists in codebase | ☐ | G-3 |

---

*TDD Template v2.0 — CB-EXERCISE-TS-005 — UC182 Complete Exercise Session*
*Status: Draft — awaiting approval before implementation begins.*
