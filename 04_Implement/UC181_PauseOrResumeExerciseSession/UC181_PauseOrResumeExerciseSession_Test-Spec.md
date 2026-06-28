# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# SRS 3.3.2.7 — Pause or Resume Exercise Session

**Document ID:** `CB-EXERCISE-TS-004`
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
- `04_Implement/UC181_PauseOrResumeExerciseSession/UC181_PauseOrResumeExerciseSession_TDS.md` (CB-EXERCISE-IMP-004)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.2.7`

> **TDD Convention:** Tests MUST be written BEFORE production code. Run → confirm 🔴 FAIL → implement → 🟢 PASS → refactor 🔵.
> Never mark a test ✅ unless `./mvnw test` (backend) or `flutter test` (mobile) is green.
> Use SYNTHETIC data only — no real PII.

---

## CHANGELOG

| Date | Author | Change |
|------|--------|--------|
| `2026-06-28` | `AI Agent` | Initial creation — TDD spec for UC181 Pause or Resume Exercise Session |

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
| **Feature ID** | `SRS 3.3.2.7 / UC-181` |
| **Module** | `exercise — Pause or Resume Exercise Session` |
| **Spec source** | `CB-EXERCISE-IMP-004` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S2 (Sprint 2)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY` |
| **Upstream Dependencies** | `IAM (JWT), UC179 StartExerciseSession (exercise_sessions table)` |
| **Downstream Consumers** | `Posture Analysis (SRS 3.3.2.6), UC182 CompleteExerciseSession` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-004 §17`, `ADR-PR-001 through ADR-PR-004` |
| **Constraints Injected** | `C1 (state guard), C2 (owner check), C3 (paused_seconds accumulation), C4 (soft warning), C5 (JWT auth)` |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Oracle for DB facts: `V1__init_schema.sql` and approved Flyway migrations.

| # | Spec Issue | Reality | Fix in Test |
|---|------------|---------|-------------|
| L1 | TDS references `paused_at` column added by V2 migration; V1 does not have this column | V1 `exercise_sessions` has no `paused_at` — TDS uses `updated_at` as implicit pause-start proxy via ADR-PR-003 | Tests must verify `paused_seconds` accumulation via `updated_at` delta, NOT a dedicated `paused_at` column |
| L2 | `warning_count` described as "pause counter" but in V1 it is `DEFAULT 0 NOT NULL` | V1 column exists; incremented each pause, never decremented | Tests verify `warning_count` increments from 0→1→2→3 with each pause |
| L3 | "3-pause threshold is soft warning, not hard block" | BR-SESSION-014 and ADR-PR-004 confirm soft-only warning | Tests must confirm the 4th pause is NOT rejected (i.e., still returns 200, not 429/400) |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
exercise module — Pause/Resume flows:
├── Domain (state machine policy — pure logic, no deps)
│   └── ExerciseSessionStatePolicy — assertCanPause / assertCanResume
├── Service (mock repositories — Mockito)
│   └── ExerciseSessionServiceImpl.pauseSession() / resumeSession()
├── Controller (mock service — @WebMvcTest)
│   └── ExerciseSessionController — PATCH /sessions/{id}/pause+resume
└── Integration (Testcontainers PostgreSQL)
    └── Full PATCH flow with real DB state transitions
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.7 UC-181` | Mother can pause/resume timing and posture feedback |
| `BR-SESSION-010` | Pause only valid from IN_PROGRESS |
| `BR-SESSION-011` | Resume only valid from PAUSED |
| `BR-SESSION-012` | Only session owner (user_id == JWT sub) may pause/resume |
| `BR-SESSION-013` | `paused_seconds += (now - updated_at)` on resume |
| `BR-SESSION-014` | `warning_count` incremented each pause; soft warning when ≥ 3 |
| `ADR-PR-001` | State transition validated server-side; client state not trusted |
| `ADR-PR-002` | IDOR prevention via ownership check |
| `ADR-PR-003` | `updated_at` used as pause-start proxy |
| `ADR-PR-004` | 3-pause threshold returns `pauseWarning: true` in response |
| `V1__init_schema.sql exercise_sessions` | `session_status VARCHAR(20)`, `paused_seconds INT DEFAULT 0`, `warning_count INT DEFAULT 0` |
| `CB-EXERCISE-IMP-004 §9` | API contract: PATCH `/pause` → 200, PATCH `/resume` → 200 |
| `CB-EXERCISE-IMP-004 §10` | Error codes: EXPR-002 (404), EXPR-003 (403), EXPR-004 (409) |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|------------|
| TC-COND-PR-001 | Mother pauses an IN_PROGRESS session | Happy path — pause | PR-TC-001 |
| TC-COND-PR-002 | System increments warning_count on each pause | Business rule BR-SESSION-014 | PR-TC-001, PR-TC-004 |
| TC-COND-PR-003 | Response includes `pauseWarning: true` when warning_count ≥ 3 | Soft warning ADR-PR-004 | PR-TC-004 |
| TC-COND-PR-004 | Mother resumes a PAUSED session | Happy path — resume | PR-TC-002 |
| TC-COND-PR-005 | `paused_seconds` accumulates correctly on resume | BR-SESSION-013 | PR-TC-002 |
| TC-COND-PR-006 | Pause on already-PAUSED session returns 409 EXPR-004 | Invalid state — pause | PR-TC-005 |
| TC-COND-PR-007 | Resume on IN_PROGRESS session returns 409 EXPR-004 | Invalid state — resume | PR-TC-006 |
| TC-COND-PR-008 | Pause/resume by non-owner returns 403 EXPR-003 | IDOR / authorization | PR-TC-007, PR-TC-008 |
| TC-COND-PR-009 | Pause/resume with non-existent sessionId returns 404 EXPR-002 | Not found | PR-TC-009 |
| TC-COND-PR-010 | 4th pause still succeeds (soft warning, not hard block) | ADR-PR-004 | PR-TC-010 |
| TC-COND-PR-011 | Unauthenticated request returns 401 | IAM gate | PR-TC-011 |
| TC-COND-PR-012 | Full pause→resume cycle persists correctly in DB | Integration coverage | PR-TC-INT-001 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | session_status FSM (IN_PROGRESS ↔ PAUSED) | Verify all valid and invalid transitions |
| Error Guessing | IDOR via sessionId path variable | Ownership bypass is a critical security risk |
| Boundary Value Analysis | warning_count = 2 (no warning), 3 (first warning), 4+ (persistent warning) | Soft warning boundary |
| Equivalence Partitioning | pausedSeconds accumulation (single pause vs. multiple) | Verify addition logic |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-PR-001` | DB seed | `exercise_sessions` row: `session_status='IN_PROGRESS'`, `user_id=USER_A_ID`, `paused_seconds=0`, `warning_count=0` | Happy path pause base |
| `FX-PR-002` | DB seed | `exercise_sessions` row: `session_status='PAUSED'`, `user_id=USER_A_ID`, `paused_seconds=120`, `warning_count=1` | Happy path resume base |
| `FX-PR-003` | DB seed | `exercise_sessions` row: `session_status='IN_PROGRESS'`, `user_id=USER_A_ID`, `warning_count=2` | Soft warning boundary (2 pauses already) |
| `FX-PR-004` | DB seed | `exercise_sessions` row: `session_status='IN_PROGRESS'`, `user_id=USER_B_ID` | Cross-user IDOR test |
| `FX-PR-005` | JWT | `{ sub: USER_A_ID, role: 'MOTHER' }` | Authorized user |
| `FX-PR-006` | JWT | `{ sub: USER_B_ID, role: 'MOTHER' }` | Different user for IDOR |
| `FX-PR-007` | UUID | `NON_EXISTENT_SESSION_ID` | 404 test |

---

## 4. Test Case Specification

> **TC ID format:** `PR-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

```java
// ExerciseSessionPauseResumeTestFactory.java
// All test data MUST come from this factory — no shared mutable state
class ExerciseSessionPauseResumeTestFactory {

    static final UUID USER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID SESSION_IN_PROGRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID SESSION_PAUSED_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID NON_EXISTENT_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    static ExerciseSession makeInProgressSession() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(SESSION_IN_PROGRESS_ID);
        s.setUserId(USER_A_ID);
        s.setExerciseId(EXERCISE_ID);
        s.setSessionStatus(SessionStatus.IN_PROGRESS);
        s.setPausedSeconds(0);
        s.setWarningCount(0);
        s.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        return s;
    }

    static ExerciseSession makeInProgressSession(Consumer<ExerciseSession> overrides) {
        ExerciseSession s = makeInProgressSession();
        overrides.accept(s);
        return s;
    }

    static ExerciseSession makePausedSession() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(SESSION_PAUSED_ID);
        s.setUserId(USER_A_ID);
        s.setExerciseId(EXERCISE_ID);
        s.setSessionStatus(SessionStatus.PAUSED);
        s.setPausedSeconds(120);
        s.setWarningCount(1);
        s.setStartedAt(Instant.now().minus(15, ChronoUnit.MINUTES));
        s.setUpdatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return s;
    }
}
```

---

### PR-TC-001 — Pause an IN_PROGRESS session (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionService.pauseSession()` / `PATCH /api/v1/exercises/sessions/{sessionId}/pause`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-001, TC-COND-PR-002`
**Oracle Source:** `BR-SESSION-010`, `CB-EXERCISE-IMP-004 §9 Response 200`

**Preconditions:**
- Session exists with `session_status = 'IN_PROGRESS'`, `warning_count = 0` (FX-PR-001)
- JWT belongs to session owner (USER_A_ID)

**Test Steps:**
1. Arrange: mock `ExerciseSessionRepository.findById(SESSION_IN_PROGRESS_ID)` → return `makeInProgressSession()`; mock `save()` → capture argument
2. Act: call `service.pauseSession(SESSION_IN_PROGRESS_ID, USER_A_ID)`
3. Assert:
   - Saved session has `sessionStatus = PAUSED`
   - Saved session has `warningCount = 1`
   - Response DTO has `sessionStatus = "PAUSED"`, `warningCount = 1`, `pauseWarning = false`

**Expected Result (PASS):**
- `sessionStatus = PAUSED`, `warningCount = 1`, `pauseWarning = false`

**Expected Result (FAIL):**
- Status not changed, or `warningCount` not incremented, or `pauseWarning` field absent

**Current Status:** 🔴 Not written

---

### PR-TC-002 — Resume a PAUSED session (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionService.resumeSession()` / `PATCH /api/v1/exercises/sessions/{sessionId}/resume`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionResumeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-004, TC-COND-PR-005`
**Oracle Source:** `BR-SESSION-011, BR-SESSION-013`, `CB-EXERCISE-IMP-004 §9 Response 200 resume`

**Preconditions:**
- Session exists: `session_status = 'PAUSED'`, `paused_seconds = 120`, `updated_at = 5 minutes ago` (FX-PR-002)
- JWT belongs to session owner (USER_A_ID)

**Test Steps:**
1. Arrange: fixed clock at T+5min; mock repository to return `makePausedSession()` (updated_at = T); capture save argument
2. Act: call `service.resumeSession(SESSION_PAUSED_ID, USER_A_ID)` at T+5min
3. Assert:
   - Saved `sessionStatus = IN_PROGRESS`
   - Saved `pausedSeconds >= 120 + 300` (i.e., original 120s + ~300s elapsed pause)
   - Response has `sessionStatus = "IN_PROGRESS"`, `pausedSeconds` ≥ 420

**Expected Result (PASS):**
- `sessionStatus = IN_PROGRESS`, `pausedSeconds` correctly accumulated

**Expected Result (FAIL):**
- `pausedSeconds` unchanged, or `sessionStatus` still PAUSED

**Current Status:** 🔴 Not written

---

### PR-TC-003 — Controller layer pause returns 200 OK

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionController.PATCH /api/v1/exercises/sessions/{sessionId}/pause`
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerPauseTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-001`
**Oracle Source:** `CB-EXERCISE-IMP-004 §9 — 200 OK response shape`

**Preconditions:**
- Mock service returns `PauseSessionResponse` with `sessionStatus=PAUSED`, `warningCount=1`
- JWT has role `MOTHER`

**Test Steps:**
1. Arrange: `@WebMvcTest` with mock `ExerciseSessionService`; mock `pauseSession()` to return valid response
2. Act: `PATCH /api/v1/exercises/sessions/{id}/pause` with `Authorization: Bearer [MOTHER_JWT]`
3. Assert: HTTP 200, response body has `sessionStatus = "PAUSED"`, `pauseWarning = false`

**Expected Result (PASS):** 200 OK with correct shape

**Current Status:** 🔴 Not written

---

### PR-TC-004 — Soft warning when warning_count reaches 3

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionService.pauseSession()` soft warning
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-003`
**Oracle Source:** `ADR-PR-004`, `BR-SESSION-014`

**Preconditions:**
- Session: `session_status = IN_PROGRESS`, `warning_count = 2` (FX-PR-003)

**Test Steps:**
1. Arrange: mock repository to return session with `warningCount=2`
2. Act: `service.pauseSession(SESSION_ID, USER_A_ID)`
3. Assert:
   - Saved `warningCount = 3`
   - Response `pauseWarning = true`
   - HTTP status still 200 (NOT 400/429) — soft warning only

**Expected Result (PASS):** `warningCount=3`, `pauseWarning=true`, HTTP 200

**Current Status:** 🔴 Not written

---

### PR-TC-005 — Pause on PAUSED session returns 409 EXPR-004

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionStatePolicy.assertCanPause()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-006`
**Oracle Source:** `BR-SESSION-010`, `CB-EXERCISE-IMP-004 §10 EXPR-004`

**Preconditions:**
- Session: `session_status = 'PAUSED'` (FX-PR-002)

**Test Steps:**
1. Arrange: mock repo to return `makePausedSession()`
2. Act: call `service.pauseSession(SESSION_PAUSED_ID, USER_A_ID)`
3. Assert: throws `InvalidSessionStateException` with error code `EXPR-004`

**Expected Result (PASS):** Exception with code `EXPR-004`; no DB write

**Current Status:** 🔴 Not written

---

### PR-TC-006 — Resume on IN_PROGRESS session returns 409 EXPR-004

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionStatePolicy.assertCanResume()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionResumeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-007`
**Oracle Source:** `BR-SESSION-011`, `CB-EXERCISE-IMP-004 §10 EXPR-004`

**Preconditions:**
- Session: `session_status = 'IN_PROGRESS'` (FX-PR-001)

**Test Steps:**
1. Arrange: mock repo to return `makeInProgressSession()`
2. Act: call `service.resumeSession(SESSION_IN_PROGRESS_ID, USER_A_ID)`
3. Assert: throws `InvalidSessionStateException` with code `EXPR-004`

**Expected Result (PASS):** Exception `EXPR-004`, no DB write

**Current Status:** 🔴 Not written

---

### PR-TC-007 — Pause by non-owner returns 403 EXPR-003 (IDOR prevention)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExerciseSessionService — ownership check`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-008`
**Oracle Source:** `ADR-PR-002`, `BR-SESSION-012`, `CB-EXERCISE-IMP-004 §10 EXPR-003`

**Preconditions:**
- Session owned by USER_A_ID (FX-PR-001); attacker JWT is USER_B_ID (FX-PR-006)

**Test Steps:**
1. Arrange: mock repo to return session with `userId = USER_A_ID`
2. Act: call `service.pauseSession(SESSION_IN_PROGRESS_ID, USER_B_ID)` — USER_B_ID is the attacker
3. Assert: throws `SessionAccessDeniedException` with code `EXPR-003`; no DB save called

**Expected Result (PASS):** `EXPR-003` thrown, `save()` never called

**Expected Result (FAIL):** Session paused for attacker — critical IDOR vulnerability

**Current Status:** 🔴 Not written

---

### PR-TC-008 — Resume by non-owner returns 403 EXPR-003

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Feature Under Test:** `ExerciseSessionService.resumeSession()` ownership check
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionResumeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-008`
**Oracle Source:** `ADR-PR-002`, `CB-EXERCISE-IMP-004 §10 EXPR-003`

**Preconditions:**
- Session owned by USER_A_ID (FX-PR-002); attacker JWT is USER_B_ID

**Test Steps:**
1. Arrange: mock repo returns session with `userId = USER_A_ID`
2. Act: call `service.resumeSession(SESSION_PAUSED_ID, USER_B_ID)`
3. Assert: throws `SessionAccessDeniedException` code `EXPR-003`

**Expected Result (PASS):** Exception `EXPR-003`, no DB write

**Current Status:** 🔴 Not written

---

### PR-TC-009 — Non-existent sessionId returns 404 EXPR-002

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionService — session not found handling`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-009`
**Oracle Source:** `CB-EXERCISE-IMP-004 §10 EXPR-002`

**Preconditions:**
- `NON_EXISTENT_SESSION_ID` does not exist in DB

**Test Steps:**
1. Arrange: mock `repository.findById(NON_EXISTENT_SESSION_ID)` → `Optional.empty()`
2. Act: call `service.pauseSession(NON_EXISTENT_SESSION_ID, USER_A_ID)`
3. Assert: throws `SessionNotFoundException` code `EXPR-002`

**Expected Result (PASS):** `EXPR-002` thrown

**Current Status:** 🔴 Not written

---

### PR-TC-010 — 4th pause still returns 200 (not hard-blocked)

**Severity:** `LOW`
**Feature Under Test:** `ExerciseSessionService.pauseSession()` — soft-only warning
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-010`
**Oracle Source:** `ADR-PR-004 — "soft warning only, not hard block"`

**Preconditions:**
- Session: `warning_count = 3`, `session_status = IN_PROGRESS`

**Test Steps:**
1. Arrange: session with `warningCount=3`
2. Act: call `service.pauseSession(SESSION_ID, USER_A_ID)`
3. Assert: no exception thrown; response has `warningCount=4`, `pauseWarning=true`, HTTP 200

**Expected Result (PASS):** 4th pause succeeds; `warningCount=4`, `pauseWarning=true`

**Current Status:** 🔴 Not written

---

### PR-TC-011 — Unauthenticated request returns 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security authentication gate
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerPauseTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-011`
**Oracle Source:** `CB-EXERCISE-IMP-004 §16 Auth Matrix — GUEST → ❌`

**Test Steps:**
1. Arrange: no Authorization header
2. Act: `PATCH /api/v1/exercises/sessions/{id}/pause`
3. Assert: HTTP 401

**Expected Result (PASS):** 401 Unauthorized

**Current Status:** 🔴 Not written

---

### PR-TC-INT-001 — Full pause → resume cycle (Integration)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: start → pause → resume → verify DB state
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionPauseResumeIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-PR-012`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied (exercise_sessions table from V1)
- Seed: `pregnancy_exercises` row (PUBLISHED), `exercise_safety_checks` row (CLEARED), `exercise_sessions` row (IN_PROGRESS)

**Test Steps:**
1. Seed: insert IN_PROGRESS session via repository
2. `PATCH /api/v1/exercises/sessions/{sessionId}/pause` (USER_A JWT) → assert 200, `sessionStatus=PAUSED`
3. Sleep 2 seconds
4. `PATCH /api/v1/exercises/sessions/{sessionId}/resume` (USER_A JWT) → assert 200, `sessionStatus=IN_PROGRESS`
5. Fetch session from DB; assert `paused_seconds >= 2`, `warning_count = 1`

**Expected Result (PASS):**
- `session_status = 'IN_PROGRESS'`
- `paused_seconds >= 2`
- `warning_count = 1`

**DB Assertion:**
```java
ExerciseSession session = repo.findById(sessionId).orElseThrow();
assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
assertThat(session.getPausedSeconds()).isGreaterThanOrEqualTo(2);
assertThat(session.getWarningCount()).isEqualTo(1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PR-TC-001` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-002` | `ExerciseSessionResumeServiceTest.java` | `[ ]` | — | — |
| `PR-TC-003` | `ExerciseSessionControllerPauseTest.java` | `[ ]` | — | — |
| `PR-TC-004` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-005` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-006` | `ExerciseSessionResumeServiceTest.java` | `[ ]` | — | — |
| `PR-TC-007` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-008` | `ExerciseSessionResumeServiceTest.java` | `[ ]` | — | — |
| `PR-TC-009` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-010` | `ExerciseSessionPauseServiceTest.java` | `[ ]` | — | — |
| `PR-TC-011` | `ExerciseSessionControllerPauseTest.java` | `[ ]` | — | — |
| `PR-TC-INT-001` | `ExerciseSessionPauseResumeIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Red Phase stub:**
```java
@Service
public class ExerciseSessionServiceImpl implements IExerciseSessionService {

    @Override
    public PauseSessionResponse pauseSession(UUID sessionId, UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ResumeSessionResponse resumeSession(UUID sessionId, UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (if PASS) |
|-------|-------------|----------|--------|----------------------|
| `PR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PR-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PR-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PR-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- All FAIL? ☐ Yes → **GATE-2 PASS** → proceed to implement
- Log file: `test-evidence/UC181_red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] `CB-EXERCISE-IMP-004` TDS reviewed and approved
- [ ] UC179 StartExerciseSession implemented (exercise_sessions table populated)
- [ ] V1 migration applied and `exercise_sessions` table exists with all required columns
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=ExerciseSessionPauseServiceTest,ExerciseSessionResumeServiceTest,ExerciseSessionControllerPauseTest` — all green
- [ ] `./mvnw verify -Dtest=ExerciseSessionPauseResumeIntegrationTest` — integration green
- [ ] All 12 TCs green
- [ ] PR-TC-007 and PR-TC-008 (IDOR) MUST pass
- [ ] No business logic in controller (validation + delegation only)
- [ ] No PII in logs

**Exit Criteria — CASE 2.0:**
- [ ] Red Gate (§5.1) — all tests FAIL with stub before implementation
- [ ] Contract Existence — all injected classes exist: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] Props Isolation — no shared mutable state between tests
- [ ] Oracle Source — every assert references a BR/ADR

---

## 7. Rollback Plan

```bash
# Revert implementation files only (no migration to rollback — V1 schema unchanged)
git checkout -- src/main/java/com/carebridge/backend/exercise/service/impl/ExerciseSessionServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java
git checkout -- src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionPauseServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionResumeServiceTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Warning Sign | Check | Gate |
|-------|-------------|--------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any BR or ADR | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC passes with `throw new UnsupportedOperationException()` stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `paused_at` column exists in V1 (it does not) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test verifies business logic in controller class | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExerciseSessionStatePolicy` before it exists in codebase | ☐ | G-3 |

**Review result:**
- [ ] No anti-patterns detected → spec approved
- [ ] Anti-pattern detected → record below and fix before implementation

---

*TDD Template v2.0 — CB-EXERCISE-TS-004 — UC181 Pause or Resume Exercise Session*
*Status: Draft — awaiting Tech Lead and Principal Architect approval before implementation begins.*
