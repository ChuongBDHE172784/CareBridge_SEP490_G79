# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# SRS 3.3.2.9 — View Exercise Session Result

**Document ID:** `CB-EXERCISE-TS-006`
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
- `04_Implement/UC183_ViewExerciseSessionResult/UC183_ViewExerciseSessionResult_TDS.md` (CB-EXERCISE-IMP-006)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.2.9`

> **TDD Convention:** Tests BEFORE production code. 🔴 FAIL with stub → implement → 🟢 PASS → refactor 🔵.
> SYNTHETIC data only — no real PII.

---

## CHANGELOG

| Date | Author | Change |
|------|--------|--------|
| `2026-06-28` | `AI Agent` | Initial creation — TDD spec for UC183 View Exercise Session Result |

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
| **Feature ID** | `SRS 3.3.2.9 / UC-183` |
| **Module** | `exercise — View Exercise Session Result` |
| **Spec source** | `CB-EXERCISE-IMP-006` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S2 (Sprint 2)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY, BR-PRIVACY` |
| **Upstream Dependencies** | `IAM (JWT), UC182 (exercise_sessions COMPLETED), posture_feedback_events (V1), pregnancy_exercises (V1)` |
| **Downstream Consumers** | `Mobile App result screen (read-only display)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-006 §17`, `ADR-VER-001 through ADR-VER-004` |
| **Constraints Injected** | `C1 (owner-only access), C2 (COMPLETED sessions only), C3 (exercise title via JOIN), C4 (posture feedback summary), C5 (JWT auth MOTHER role)` |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec Issue | Reality | Fix in Test |
|---|------------|---------|-------------|
| L1 | Result endpoint may tempt implementation to return data for non-COMPLETED sessions | `ADR-VER-004` requires specific EXSESS-007 error when session is not COMPLETED (not 403 or 404) | Test: GET result for IN_PROGRESS session → assert 422/409 with code EXSESS-007, not 404 or generic 500 |
| L2 | Exercise title fetched via JOIN — if exercise deleted (soft-deleted/DRAFT), title may be unavailable | `ADR-VER-002` uses Option A (JOIN); if exercise not found, use cached title or fallback string | Test: assert title is not null in result response |
| L3 | `posture_score` and `summary_json` computed and stored by UC182 — this module only reads them | V1 column `posture_score numeric` is nullable; response must handle null gracefully | Test: complete session with no posture events → GET result → assert `postureScore=null` field is present (not absent) |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
exercise module — View Session Result (read-only):
├── Service (mock repos — Mockito)
│   └── ExerciseSessionResultService.getSessionResult()
│       ├── assertIsCompleted() — state guard
│       ├── fetchExercise() — JOIN exercise title
│       └── fetchPostureSummary() — posture_feedback_events
├── Controller (mock service — @WebMvcTest)
│   └── ExerciseSessionController.GET /sessions/{id}/result
└── Integration (Testcontainers PostgreSQL — read-only after UC182 seeds)
    └── Full GET after completion
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.9 UC-183` | View post-session results: duration, completion level, posture score, warnings |
| `BR-EXRES-001` | Only session owner may view result |
| `BR-EXRES-002` | Data comes from UC182-populated exercise_sessions record |
| `BR-EXRES-003` | Exercise title fetched from pregnancy_exercises |
| `BR-EXRES-004` | Posture feedback event summary (top-N by severity) |
| `BR-EXRES-005` | Non-COMPLETED session → EXSESS-007 |
| `ADR-VER-001` | Owner-only access; 403 EXSESS-004 on mismatch |
| `ADR-VER-002` | Exercise title via JOIN at query time (no denormalization) |
| `ADR-VER-004` | Non-COMPLETED → EXSESS-007 (not 403 or 404) |
| `V1__init_schema.sql` | `exercise_sessions`, `posture_feedback_events`, `pregnancy_exercises` |
| `CB-EXERCISE-IMP-006 §9` | `GET /api/v1/exercises/sessions/{sessionId}/result` → 200 |
| `CB-EXERCISE-IMP-006 §10` | Errors: EXSESS-004 (403), EXSESS-007 (422), EXSESS-008 (404) |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|------------|
| TC-COND-VER-001 | View result of COMPLETED session by owner — happy path | Full read flow | VER-TC-001 |
| TC-COND-VER-002 | Response includes exercise title from pregnancy_exercises | BR-EXRES-003 (JOIN) | VER-TC-001 |
| TC-COND-VER-003 | Response includes posture feedback summary | BR-EXRES-004 | VER-TC-002 |
| TC-COND-VER-004 | postureScore = null when no posture events (session completed without camera) | L3 edge case | VER-TC-003 |
| TC-COND-VER-005 | View result of IN_PROGRESS session → 422 EXSESS-007 | ADR-VER-004 | VER-TC-004 |
| TC-COND-VER-006 | View result of PAUSED session → 422 EXSESS-007 | ADR-VER-004 | VER-TC-005 |
| TC-COND-VER-007 | View result by non-owner → 403 EXSESS-004 | ADR-VER-001 (IDOR) | VER-TC-006 |
| TC-COND-VER-008 | Non-existent sessionId → 404 EXSESS-008 | Not found | VER-TC-007 |
| TC-COND-VER-009 | Unauthenticated request → 401 | IAM gate | VER-TC-008 |
| TC-COND-VER-010 | Full integration: UC182 → UC183 read result | End-to-end persistence | VER-TC-INT-001 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | COMPLETED vs. non-COMPLETED session | Verify EXSESS-007 gate |
| Error Guessing | IDOR via sessionId path variable | Critical security risk |
| Equivalence Partitioning | With/without posture events | Null vs. present postureScore |
| Boundary Value Analysis | N/A for read-only | — |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Purpose |
|-----------|------|-------|---------|
| `FX-VER-001` | DB seed | `exercise_sessions`: `status=COMPLETED`, `user_id=USER_A`, `completion_percent=90.0`, `posture_score=0.75`, `warning_count=1`, `summary_json={issues:["FORWARD_LEAN"], highlights:["GOOD_POSTURE"]}` | Happy path |
| `FX-VER-002` | DB seed | `pregnancy_exercises`: `exercise_id=EXERCISE_ID`, `title="Yoga Prenatal Stretch"`, `duration_minutes=20` | Exercise title JOIN |
| `FX-VER-003` | DB seed | 2 `posture_feedback_events` for SESSION_ID with HIGH and LOW severity | Posture summary |
| `FX-VER-004` | DB seed | `exercise_sessions`: `status=IN_PROGRESS`, `user_id=USER_A` | EXSESS-007 test |
| `FX-VER-005` | DB seed | `exercise_sessions`: `status=PAUSED`, `user_id=USER_A` | EXSESS-007 test |
| `FX-VER-006` | DB seed | `exercise_sessions`: `status=COMPLETED`, `user_id=USER_B` | IDOR test |
| `FX-VER-007` | DB seed | COMPLETED session with `posture_score=null` (no posture events) | Null posture score |
| `FX-VER-008` | JWT | `{ sub: USER_A_ID, role: 'MOTHER' }` | Owner JWT |
| `FX-VER-009` | JWT | `{ sub: USER_B_ID, role: 'MOTHER' }` | Non-owner JWT |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

```java
// ViewExerciseSessionResultTestFactory.java
class ViewExerciseSessionResultTestFactory {

    static final UUID USER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID COMPLETED_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID IN_PROGRESS_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID NON_EXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    static ExerciseSession makeCompletedSession() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(COMPLETED_SESSION_ID);
        s.setUserId(USER_A_ID);
        s.setExerciseId(EXERCISE_ID);
        s.setSessionStatus(SessionStatus.COMPLETED);
        s.setStartedAt(Instant.now().minus(25, ChronoUnit.MINUTES));
        s.setEndedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        s.setPausedSeconds(0);
        s.setCompletionPercent(new BigDecimal("90.00"));
        s.setPostureScore(new BigDecimal("0.75"));
        s.setWarningCount(1);
        s.setSummaryJson("""
            {"issues":["FORWARD_LEAN"],"highlights":["GOOD_POSTURE"]}
            """);
        return s;
    }

    static ExerciseSession makeCompletedSession(Consumer<ExerciseSession> overrides) {
        ExerciseSession s = makeCompletedSession();
        overrides.accept(s);
        return s;
    }

    static PregnancyExercise makeExercise() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(EXERCISE_ID);
        e.setTitle("Yoga Prenatal Stretch");
        e.setDurationMinutes((short) 20);
        return e;
    }

    static PostureFeedbackEvent makeHighSeverityEvent() {
        PostureFeedbackEvent ev = new PostureFeedbackEvent();
        ev.setFeedbackEventId(UUID.randomUUID());
        ev.setExerciseSessionId(COMPLETED_SESSION_ID);
        ev.setPostureCode("FORWARD_LEAN");
        ev.setSeverity("HIGH");
        ev.setConfidenceScore(new BigDecimal("0.85"));
        ev.setEventTimeMs(5000L);
        return ev;
    }
}
```

---

### VER-TC-001 — View COMPLETED session result — happy path (service layer)

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionResultService.getSessionResult()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-001, TC-COND-VER-002`
**Oracle Source:** `BR-EXRES-001, BR-EXRES-002, BR-EXRES-003, CB-EXERCISE-IMP-006 §9 200 response`

**Preconditions:**
- Completed session with `user_id=USER_A`, `completion_percent=90.00`, `posture_score=0.75` (FX-VER-001)
- Exercise title="Yoga Prenatal Stretch" (FX-VER-002)
- JWT is USER_A

**Test Steps:**
1. Arrange: mock `ExerciseSessionRepository.findById()` → `makeCompletedSession()`; mock `ExerciseRepository.findById(EXERCISE_ID)` → `makeExercise()`; mock posture repo → 1 HIGH event
2. Act: `service.getSessionResult(COMPLETED_SESSION_ID, USER_A_ID)`
3. Assert:
   - Response contains `exerciseName = "Yoga Prenatal Stretch"`
   - Response contains `completionPercent = 90.00`
   - Response contains `postureScore = 0.75`
   - Response contains `warningCount = 1`
   - Response contains `summaryJson.issues = ["FORWARD_LEAN"]`

**Expected Result (PASS):** Full result DTO with all fields correctly mapped

**Current Status:** 🔴 Not written

---

### VER-TC-002 — Posture feedback event summary included in response

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionResultService` — posture feedback summary
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-003`
**Oracle Source:** `BR-EXRES-004 — "include posture feedback event summary (top-N events per severity)"`

**Preconditions:**
- 2 posture_feedback_events: 1 HIGH severity + 1 LOW severity (FX-VER-003)

**Test Steps:**
1. Arrange: mock posture repo returns HIGH and LOW events
2. Act: `service.getSessionResult(COMPLETED_SESSION_ID, USER_A_ID)`
3. Assert: response `postureFeedbackSummary` is not null; contains at least 2 entries with `posture_code` and `severity`

**Expected Result (PASS):** `postureFeedbackSummary` non-empty list with severity breakdown

**Current Status:** 🔴 Not written

---

### VER-TC-003 — postureScore is null when session completed without camera

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionResultService` — null posture score handling
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-004`
**Oracle Source:** `L3 (Logic Issues) — nullable posture_score in V1 schema`

**Test Steps:**
1. Arrange: COMPLETED session with `postureScore = null` (FX-VER-007); posture event repo returns empty list
2. Act: `service.getSessionResult(SESSION_ID, USER_A_ID)`
3. Assert: response field `postureScore` is `null` (field present but null, not absent from JSON)

**Expected Result (PASS):** `"postureScore": null` in response JSON

**Current Status:** 🔴 Not written

---

### VER-TC-004 — View result of IN_PROGRESS session → 422 EXSESS-007

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionResultService.assertIsCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-005`
**Oracle Source:** `ADR-VER-004 — EXSESS-007 for non-COMPLETED sessions`, `BR-EXRES-005`

**Preconditions:**
- Session: `status = IN_PROGRESS` (FX-VER-004)

**Test Steps:**
1. Mock repo returns IN_PROGRESS session owned by USER_A
2. Act: `service.getSessionResult(IN_PROGRESS_SESSION_ID, USER_A_ID)`
3. Assert: throws `SessionNotCompletedException` with code `EXSESS-007`

**Expected Result (PASS):** `EXSESS-007` exception; NOT 403 or 404

**Current Status:** 🔴 Not written

---

### VER-TC-005 — View result of PAUSED session → 422 EXSESS-007

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionResultService.assertIsCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-006`
**Oracle Source:** `ADR-VER-004`

**Test Steps:**
1. Mock repo returns PAUSED session owned by USER_A (FX-VER-005)
2. Act: `service.getSessionResult(SESSION_ID, USER_A_ID)`
3. Assert: throws `SessionNotCompletedException` with code `EXSESS-007`

**Expected Result (PASS):** `EXSESS-007` exception

**Current Status:** 🔴 Not written

---

### VER-TC-006 — View result by non-owner → 403 EXSESS-004 (IDOR prevention)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExerciseSessionResultService` — owner check
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-007`
**Oracle Source:** `ADR-VER-001, CB-EXERCISE-IMP-006 §10 EXSESS-004`

**Preconditions:**
- COMPLETED session owned by USER_B (FX-VER-006); attacker JWT is USER_A

**Test Steps:**
1. Mock repo returns session with `userId = USER_B`; authenticated user = USER_A
2. Act: `service.getSessionResult(COMPLETED_SESSION_ID, USER_A_ID)` — USER_A is NOT the owner
3. Assert: throws `AccessDeniedBusinessException` code `EXSESS-004`

**Expected Result (PASS):** `EXSESS-004` thrown; no data returned

**Expected Result (FAIL):** Exercise result returned to unauthorized user — critical IDOR

**Current Status:** 🔴 Not written

---

### VER-TC-007 — Non-existent sessionId → 404 EXSESS-008

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionResultService` — not found path
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-008`
**Oracle Source:** `CB-EXERCISE-IMP-006 §10 EXSESS-008`

**Test Steps:**
1. Mock repo returns `Optional.empty()`
2. Act: `service.getSessionResult(NON_EXISTENT_ID, USER_A_ID)`
3. Assert: throws `ResourceNotFoundException` code `EXSESS-008`

**Expected Result (PASS):** 404 with `EXSESS-008`

**Current Status:** 🔴 Not written

---

### VER-TC-008 — Unauthenticated request → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security auth gate
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ViewExerciseSessionResultControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-009`
**Oracle Source:** `CB-EXERCISE-IMP-006 §16 Auth Matrix — GUEST → ❌`

**Test Steps:**
1. `@WebMvcTest` — no Authorization header
2. `GET /api/v1/exercises/sessions/{id}/result`
3. Assert: HTTP 401

**Current Status:** 🔴 Not written

---

### VER-TC-INT-001 — Full integration: complete (UC182) then view result (UC183)

**Severity:** `HIGH`
**Feature Under Test:** End-to-end — UC182 → UC183 read result from real DB
**Test File:** `src/test/java/com/carebridge/backend/exercise/ViewExerciseSessionResultIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-VER-010`

**Preconditions:**
- PostgreSQL Testcontainer; V1 Flyway applied
- Seed: pregnancy_exercise (PUBLISHED, duration=15min), exercise_safety_check (CLEARED), exercise_session (IN_PROGRESS, started 15min ago)
- Seed: 1 posture_feedback_event with HIGH severity

**Test Steps:**
1. `PATCH /api/v1/exercises/sessions/{sessionId}/complete` (USER_A JWT) → assert 200
2. `GET /api/v1/exercises/sessions/{sessionId}/result` (USER_A JWT) → assert 200
3. Validate response fields:
   - `sessionStatus = "COMPLETED"`
   - `completionPercent` is set and > 0
   - `postureScore` is not null (posture event exists)
   - `exerciseName` is not null (JOIN succeeded)
   - `summaryJson.issues` is array (may be empty or populated)

**Expected Result (PASS):**
```json
{
  "sessionStatus": "COMPLETED",
  "completionPercent": "...",
  "postureScore": "...",
  "exerciseName": "...",
  "summaryJson": { "issues": [...], "highlights": [...] }
}
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VER-TC-001` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-002` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-003` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-004` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-005` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-006` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-007` | `ViewExerciseSessionResultServiceTest.java` | `[ ]` | — | — |
| `VER-TC-008` | `ViewExerciseSessionResultControllerTest.java` | `[ ]` | — | — |
| `VER-TC-INT-001` | `ViewExerciseSessionResultIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Red Phase stub:**
```java
@Service
public class ExerciseSessionResultServiceImpl implements IExerciseSessionResultService {

    @Override
    public ExerciseSessionResultResponse getSessionResult(UUID sessionId, UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (if unexpected PASS) |
|-------|----------|--------|----------------------------------|
| `VER-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VER-TC-004` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VER-TC-006` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VER-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- All FAIL? ☐ Yes → **GATE-2 PASS** → proceed to implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] `CB-EXERCISE-IMP-006` TDS reviewed and approved
- [ ] UC182 (CompleteExerciseSession) implemented — `exercise_sessions.session_status = COMPLETED` achievable
- [ ] V1 migration applied: `exercise_sessions`, `posture_feedback_events`, `pregnancy_exercises`
- [ ] Test fixtures prepared

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=ViewExerciseSessionResultServiceTest,ViewExerciseSessionResultControllerTest` — all 8 TCs green
- [ ] `./mvnw verify -Dtest=ViewExerciseSessionResultIntegrationTest` — integration green
- [ ] VER-TC-006 (IDOR) MUST pass — blocking
- [ ] VER-TC-004 + VER-TC-005 (EXSESS-007 gate) MUST pass — safety gate
- [ ] No PII in logs

**CASE 2.0 Exit:**
- [ ] Red Gate all FAIL with stub before implementation
- [ ] `./mvnw compile 2>&1 | grep "error:"` → empty
- [ ] Props Isolation enforced in all test classes
- [ ] Oracle source cited for every assert

---

## 7. Rollback Plan

```bash
# No migration to rollback — read-only module, no schema changes
git checkout -- src/main/java/com/carebridge/backend/exercise/service/impl/ExerciseSessionResultServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java
git checkout -- src/test/java/com/carebridge/backend/exercise/service/ViewExerciseSessionResultServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/ViewExerciseSessionResultIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Warning Sign | Check | Gate |
|-------|-------------|--------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any BR or ADR | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test passes with `throw UnsupportedOperationException` stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test returns EXSESS-007 only for COMPLETED (wrong — should return data for COMPLETED, error for non-COMPLETED) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test verifies JOIN logic or posture summary building | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExerciseSessionResultService` before it exists in codebase | ☐ | G-3 |

---

*TDD Template v2.0 — CB-EXERCISE-TS-006 — UC183 View Exercise Session Result*
*Status: Draft — awaiting Tech Lead and Principal Architect approval before implementation begins.*
