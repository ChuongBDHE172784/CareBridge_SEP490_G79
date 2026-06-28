# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# SRS 3.3.2.5 — Start Exercise Session — Test Specification

**Document ID:** `CB-EXERCISE-IMP-003-TEST`
**Version:** `1.0`
**Date:** `2026-06-28`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Developer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC179_StartExerciseSession/UC179_StartExerciseSession_TDS.md` (CB-EXERCISE-IMP-003 v1.0) — Technical Design Specification
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (oracle)
- `01_Requirements/SRS.md` — SRS 3.3.2.5 Functional requirements

> **TDD Convention:** This document describes test cases BEFORE production code is written.
> Mandatory order: write test (`.java`) → run → confirm FAIL (RED) → implement → PASS (GREEN) → refactor (BLUE).
> Never mark a test as PASS before `./mvnw test` is green.
> Never use real PII in test data — use SYNTHETIC data only.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information.

| Date | Author | Change Description |
|------|--------|--------------------|
| 2026-06-28 | AI Agent — Developer | Initial document — TDD spec for SRS 3.3.2.5 Start Exercise Session |

---

## TABLE OF CONTENTS

1. [Module Info](#1-module-info)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `SRS-3.3.2.5` |
| **Module** | `Start Exercise Session — exercise` |
| **Spec source** | `CB-EXERCISE-IMP-003 v1.0` |
| **Priority** | P1 (Medium, safety-gated) |
| **Sprint** | `Sprint 2 (2026-06-28 →)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY` |
| **Upstream Dependencies** | `CB-EXERCISE-IMP-001/002 (shared entity/repo), exercise_safety_checks (V1), IAM (JWT)` |
| **Downstream Consumers** | `UC181 Pause/Resume (CB-EXERCISE-IMP-004), UC183 Complete Session` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-003 §17, ADR-SES-001, ADR-SES-002, ADR-SES-003, ADR-SES-004` |
| **Constraints Injected** | C1 (exercise re-validated PUBLISHED server-side), C2 (safetyCheckId FK validation, CLEARED status), C3 (UTC-day duplicate check), C4 (userId from JWT principal), C5 (no business logic in controller), C6 (sessionStatus=IN_PROGRESS, pausedSeconds=0 on create), C7 (no new migration) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, use `V1__init_schema.sql` and approved migrations as the final persistence oracle; ERD is only supporting evidence.

| # | Original Spec (issue) | Actual Reality (schema/policy) | Fix Applied in Tests |
|---|----------------------|---------------------------------|----------------------|
| L1 | Spec ambiguous on whether safety check validation checks userId ownership | V1 schema: `exercise_safety_checks.user_id NOT NULL` — safety check belongs to a specific user. JWT `sub` must equal `safety_check.user_id` | Tests assert EXSESS-003 when userId does not match |
| L2 | Spec did not specify what "duplicate session today" means (rolling 24h vs UTC day) | ADR-SES-003: UTC-day window — `started_at >= CURRENT_DATE` | Tests use UTC midnight as boundary; fixed to avoid flakiness |
| L3 | Spec says "at most 3 pauses recommended" — could be interpreted as a session-start rule | UC179 scope: duplicate check is for session-start only (IN_PROGRESS or PAUSED today). Pause count limit is a UC181 concern, not a session-start block | Tests do NOT check pause count at session start |
| L4 | V1 schema: `safety_check_id` on `exercise_sessions` is nullable (FK to exercise_safety_checks) | This is intentional — future sessions created by ADMIN or SYSTEM may skip safety check. For MOTHER-initiated sessions (UC179), safety_check_id is required and validated | Tests enforce non-null safetyCheckId in StartSessionRequest; service saves the FK |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
exercise (Start Session) layers under test:
├── Service (ExerciseSessionServiceImpl) — Mockito mocks for all repos
├── Controller (@WebMvcTest ExerciseSessionController) — mock Service
└── Integration (Testcontainers PostgreSQL + Spring Boot — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.5` | Session creation post safety-check clearance; IN_PROGRESS initial state |
| `ADR-SES-001` | Exercise re-validated PUBLISHED at session start |
| `ADR-SES-002` | Safety check FK validated: userId, exerciseId, resultStatus = CLEARED |
| `ADR-SES-003` | UTC-day duplicate session check |
| `ADR-SES-004` | paused_seconds aggregated column; no event log |
| `BR-SAFETY` | No session allowed when red_flag_detected = true |
| `BR-RBAC` | Only MOTHER role can create sessions |
| `V1__init_schema.sql` | exercise_sessions table — column names, types, defaults |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Exercise is PUBLISHED and valid | `ExerciseSessionServiceImpl.startSession()` happy path | `SESS-TC-001` |
| TC-COND-002 | Exercise is not found or not PUBLISHED | `validateExercisePublished()` → EXSESS-002 | `SESS-TC-002` |
| TC-COND-003 | Safety check ID not found in DB | `validateSafetyCheck()` → EXSESS-003 | `SESS-TC-003` |
| TC-COND-004 | Safety check userId does not match JWT userId | `validateSafetyCheck()` → EXSESS-003 | `SESS-TC-004` |
| TC-COND-005 | Safety check exerciseId does not match path exerciseId | `validateSafetyCheck()` → EXSESS-003 | `SESS-TC-005` |
| TC-COND-006 | Safety check resultStatus = BLOCKED (red flag) | `validateSafetyCheck()` → EXSESS-003 | `SESS-TC-006` |
| TC-COND-007 | Duplicate IN_PROGRESS session today | `checkNoDuplicateSession()` → EXSESS-004 | `SESS-TC-007` |
| TC-COND-008 | Duplicate PAUSED session today | `checkNoDuplicateSession()` → EXSESS-004 | `SESS-TC-008` |
| TC-COND-009 | safetyCheckId missing from request | `@Valid StartSessionRequest` → EXSESS-001 | `SESS-TC-009` |
| TC-COND-010 | No JWT / invalid JWT | `@PreAuthorize` → 401 | `SESS-TC-010` |
| TC-COND-011 | Wrong role (not MOTHER) | `@PreAuthorize` → 403 | `SESS-TC-011` |
| TC-COND-012 | Full integration — DB state after creation | Integration test | `SESS-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | resultStatus (CLEARED vs BLOCKED vs PENDING), sessionStatus (IN_PROGRESS vs PAUSED) | Cover each valid/invalid partition |
| Boundary Value Analysis | UTC-day boundary for duplicate check (session started at CURRENT_DATE 00:00:00 vs previous day) | Avoid flaky off-by-one errors |
| State Transition Testing | SessionStatus FSM — only IN_PROGRESS valid at creation | Ensure no other initial status is accepted |
| Error Guessing | userId mismatch on safety check, wrong role injection | Security attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-SESS-001` | SYNTHETIC entity | `PregnancyExercise { exerciseId=EX_ID_1, status=PUBLISHED, supportsPostureAnalysis=true }` | Happy path exercise |
| `FX-SESS-002` | SYNTHETIC entity | `PregnancyExercise { exerciseId=EX_ID_1, status=DRAFT }` | Rejected exercise |
| `FX-SESS-003` | SYNTHETIC entity | `ExerciseSafetyCheck { safetyCheckId=SC_ID_1, userId=USER_ID_1, exerciseId=EX_ID_1, resultStatus=CLEARED, redFlagDetected=false }` | Valid safety check |
| `FX-SESS-004` | SYNTHETIC entity | `ExerciseSafetyCheck { resultStatus=BLOCKED, redFlagDetected=true }` | Red-flag safety check |
| `FX-SESS-005` | SYNTHETIC entity | `ExerciseSafetyCheck { userId=DIFFERENT_USER_ID }` | Ownership mismatch |
| `FX-SESS-006` | SYNTHETIC entity | `ExerciseSession { sessionStatus=IN_PROGRESS, startedAt=today UTC }` | Duplicate session |
| `FX-SESS-007` | JWT | `{ sub: USER_ID_1, role: 'MOTHER' }` | Valid MOTHER token |
| `FX-SESS-008` | JWT | `{ sub: ADMIN_ID, role: 'ADMIN' }` | Wrong role token |

---

## 4. Test Case Specification

> **TC ID format:** `SESS-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** RED (not written) → YELLOW (written, failing) → GREEN (passing)

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

> CASE 2.0 Rule: Every test MUST create a fresh instance via factory. No shared mutable state between test cases. Prevents AP-AI-002 (Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Place at top of test file — every @Test uses factory methods
// ═══════════════════════════════════════════════════════════

class ExerciseSessionTestFactory {

    static final UUID EX_ID_1   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SC_ID_1   = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID SESS_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000030");

    /** Valid PUBLISHED exercise — synced with FX-SESS-001 */
    static PregnancyExercise makePublishedExercise() {
        PregnancyExercise ex = new PregnancyExercise();
        ex.setExerciseId(EX_ID_1);
        ex.setStatus(ExerciseStatus.PUBLISHED);
        ex.setSupportsPostureAnalysis(true);
        ex.setTitle("Prenatal Yoga - Trimester 2");
        return ex;
    }

    /** CLEARED safety check — synced with FX-SESS-003 */
    static ExerciseSafetyCheck makeClearedSafetyCheck() {
        ExerciseSafetyCheck sc = new ExerciseSafetyCheck();
        sc.setSafetyCheckId(SC_ID_1);
        sc.setExerciseId(EX_ID_1);
        sc.setUserId(USER_ID_1);
        sc.setResultStatus(SafetyCheckStatus.CLEARED);
        sc.setRedFlagDetected(false);
        sc.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        return sc;
    }

    /** Override specific fields */
    static ExerciseSafetyCheck makeClearedSafetyCheck(Consumer<ExerciseSafetyCheck> overrides) {
        ExerciseSafetyCheck sc = makeClearedSafetyCheck();
        overrides.accept(sc);
        return sc;
    }

    /** Valid StartSessionRequest */
    static StartSessionRequest makeStartRequest() {
        StartSessionRequest req = new StartSessionRequest();
        req.setSafetyCheckId(SC_ID_1);
        return req;
    }

    /** Existing active session (today) — simulates duplicate */
    static ExerciseSession makeActiveSessionToday() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(SESS_ID_1);
        s.setExerciseId(EX_ID_1);
        s.setUserId(USER_ID_1);
        s.setSessionStatus(SessionStatus.IN_PROGRESS);
        s.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));
        s.setPausedSeconds(0);
        return s;
    }
}
```

---

### SESS-TC-001 — Happy Path: Valid Exercise + Cleared Safety Check + No Duplicate

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseSessionServiceImpl.startSession()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED — not implemented
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SES-001, ADR-SES-002, ADR-SES-003, V1__init_schema.sql (session_status DEFAULT 'IN_PROGRESS')`

**Preconditions:**
- `ExerciseRepository` mock returns `FX-SESS-001` (PUBLISHED exercise) for `findByExerciseIdAndStatus(EX_ID_1, PUBLISHED)`
- `ExerciseSafetyCheckRepository` mock returns `FX-SESS-003` (CLEARED check) for `findById(SC_ID_1)`
- `ExerciseSessionRepository` mock returns empty for `findActiveSessionToday(...)`
- `ExerciseSessionRepository` mock captures and returns the saved session

**Test Steps:**
1. Arrange: create mocks using `ExerciseSessionTestFactory`; mock `sessionRepository.save()` to return session with generated UUID
2. Act: `service.startSession(EX_ID_1, makeStartRequest(), USER_ID_1)`
3. Assert: response has non-null `exerciseSessionId`, `sessionStatus == "IN_PROGRESS"`, `startedAt` is not null

**Expected Result (PASS):**
- `response.getSessionStatus()` equals `"IN_PROGRESS"`
- `response.getExerciseSessionId()` is not null
- `sessionRepository.save()` called exactly once
- Saved entity has `pausedSeconds == 0`, `warningCount == 0`, `endedAt == null`

**Expected Result (FAIL — implementation error):**
- Any non-IN_PROGRESS status on creation
- `save()` not called or called multiple times
- `pausedSeconds != 0`

**Current Status:** RED — not written
**Implementation Note:** In service: call `validateExercisePublished()`, then `validateSafetyCheck()`, then `checkNoDuplicateSession()`, then build entity with `sessionStatus=IN_PROGRESS` and `startedAt=OffsetDateTime.now(ZoneOffset.UTC)`, then `save()`.

---

### SESS-TC-002 — Exercise Not PUBLISHED (or Not Found) → EXSESS-002

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseSessionServiceImpl.validateExercisePublished()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SES-001`

**Preconditions:**
- `ExerciseRepository` mock returns empty for `findByExerciseIdAndStatus(EX_ID_1, PUBLISHED)`

**Test Steps:**
1. Arrange: exercise repo returns empty (exercise doesn't exist or not PUBLISHED)
2. Act: `service.startSession(EX_ID_1, makeStartRequest(), USER_ID_1)`
3. Assert: `ExerciseNotFoundException` thrown

**Expected Result (PASS):**
- `ExerciseNotFoundException` is thrown with error code `EXSESS-002`
- `safetyCheckRepository.findById()` is NEVER called (short-circuit)
- `sessionRepository.save()` is NEVER called

**Expected Result (FAIL):**
- Exception not thrown → session created for non-PUBLISHED exercise
- Safety check or session calls made after exercise validation fails

**Current Status:** RED — not written

---

### SESS-TC-003 — Safety Check Not Found → EXSESS-003

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseSessionServiceImpl.validateSafetyCheck()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SES-002, BR-SAFETY`

**Preconditions:**
- Exercise repo returns PUBLISHED exercise (FX-SESS-001)
- Safety check repo returns empty for `findById(SC_ID_1)`

**Test Steps:**
1. Arrange: set up mocks
2. Act: `service.startSession(EX_ID_1, makeStartRequest(), USER_ID_1)`
3. Assert: `SafetyCheckNotClearedException` thrown

**Expected Result (PASS):**
- `SafetyCheckNotClearedException` thrown with code `EXSESS-003`
- `sessionRepository.save()` never called

**Current Status:** RED — not written

---

### SESS-TC-004 — Safety Check UserId Mismatch → EXSESS-003

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseSessionServiceImpl.validateSafetyCheck()` — userId ownership check
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SES-002, BR-SAFETY`

**Preconditions:**
- Exercise repo returns PUBLISHED exercise
- Safety check repo returns a check where `userId = DIFFERENT_USER_ID` (using `makeClearedSafetyCheck(sc -> sc.setUserId(UUID.randomUUID()))`)

**Test Steps:**
1. Arrange: safety check with different userId
2. Act: `service.startSession(EX_ID_1, makeStartRequest(), USER_ID_1)`
3. Assert: `SafetyCheckNotClearedException` thrown

**Expected Result (PASS):**
- Exception thrown even though safety check exists and is CLEARED (ownership check fails)

**Current Status:** RED — not written
**Implementation Note:** Service must assert `check.getUserId().equals(userId)` explicitly. A different user's CLEARED check cannot be used.

---

### SESS-TC-005 — Safety Check ExerciseId Mismatch → EXSESS-003

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionServiceImpl.validateSafetyCheck()` — exerciseId cross-check
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SES-002`

**Preconditions:**
- Exercise repo returns PUBLISHED exercise for EX_ID_1
- Safety check repo returns a check where `exerciseId = DIFFERENT_EXERCISE_ID`

**Expected Result (PASS):**
- `SafetyCheckNotClearedException` thrown with code `EXSESS-003`
- Guards against safety check for exercise A being reused for exercise B

**Current Status:** RED — not written

---

### SESS-TC-006 — Safety Check Is BLOCKED (Red Flag) → EXSESS-003

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseSessionServiceImpl.validateSafetyCheck()` — CLEARED status gate
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SES-002, BR-SAFETY`

**Preconditions:**
- Exercise repo returns PUBLISHED exercise
- Safety check repo returns `FX-SESS-004` (BLOCKED, redFlagDetected = true)

**Expected Result (PASS):**
- `SafetyCheckNotClearedException` thrown with code `EXSESS-003`
- No session created — this is the core safety gate of UC179

**Current Status:** RED — not written
**Implementation Note:** This is the most safety-critical test. Must fail clearly if implementation allows BLOCKED checks through.

---

### SESS-TC-007 — Duplicate IN_PROGRESS Session Today → EXSESS-004

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionServiceImpl.checkNoDuplicateSession()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SES-003`

**Preconditions:**
- Exercise PUBLISHED, safety check CLEARED (pass prior validations)
- Session repo mock returns `FX-SESS-006` (IN_PROGRESS session from today)

**Expected Result (PASS):**
- `DuplicateSessionException` thrown with code `EXSESS-004`
- `sessionRepository.save()` never called

**Current Status:** RED — not written

---

### SESS-TC-008 — Duplicate PAUSED Session Today → EXSESS-004

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionServiceImpl.checkNoDuplicateSession()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-SES-003`

**Preconditions:**
- Exercise PUBLISHED, safety check CLEARED
- Session repo returns a PAUSED session from today (using `makeActiveSessionToday()` with `sessionStatus = PAUSED`)

**Expected Result (PASS):**
- `DuplicateSessionException` thrown
- PAUSED sessions block re-start, same as IN_PROGRESS

**Current Status:** RED — not written

---

### SESS-TC-009 — Missing safetyCheckId → 400 EXSESS-001 (Controller Test)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionController` — `@Valid StartSessionRequest`
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-EXERCISE-IMP-003 §10 — EXSESS-001`

**Preconditions:**
- `@WebMvcTest(ExerciseSessionController.class)` with mocked service
- Request body: `{}`  (no safetyCheckId)
- Auth: MOTHER JWT

**Test Steps:**
1. Arrange: mock Spring Security with MOTHER role; mock service (should not be called)
2. Act: `mockMvc.perform(post("/api/v1/exercises/{exerciseId}/sessions", EX_ID_1).content("{}").contentType(APPLICATION_JSON).header("Authorization", "Bearer MOTHER_TOKEN"))`
3. Assert: status 400, response body contains `code = "EXSESS-001"`, `field = "safetyCheckId"`

**Expected Result (PASS):**
- HTTP 400
- `service.startSession()` never called
- Response contains field-level validation error for `safetyCheckId`

**Current Status:** RED — not written

---

### SESS-TC-010 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionController` — Spring Security authentication filter
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. Act: POST without Authorization header
2. Assert: HTTP 401

**Expected Result (PASS):**
- 401 returned; service never invoked

**Current Status:** RED — not written

---

### SESS-TC-011 — Wrong Role (ADMIN) → 403

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionController` — `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-RBAC, §16 Authorization Matrix`

**Test Steps:**
1. Arrange: authenticate with ADMIN role JWT
2. Act: POST with valid body
3. Assert: HTTP 403

**Expected Result (PASS):**
- 403 returned; service never invoked

**Current Status:** RED — not written

---

### INTEGRATION TEST CASES

> Uses Testcontainers (`PostgreSQLContainer`). Flyway migration auto-applied. Timeout: 120s.

---

### SESS-TC-INT-001 — Full Session Start Flow: DB State After Creation

**Severity:** `HIGH`
**Feature Under Test:** Full flow: POST /sessions → ExerciseSessionServiceImpl → ExerciseSessionRepository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001, TC-COND-012`

**Preconditions:**
- `@SpringBootTest` with Testcontainers PostgreSQL
- Flyway migration V1 applied automatically on context startup
- Seed via JPA `@BeforeEach`:
  - Insert PUBLISHED exercise (EX_ID_1) into `pregnancy_exercises`
  - Insert CLEARED safety check (SC_ID_1, userId=USER_ID_1, exerciseId=EX_ID_1) into `exercise_safety_checks`
- Authenticate as MOTHER with valid JWT for USER_ID_1

**Test Steps:**
1. Seed data into DB
2. `POST /api/v1/exercises/{EX_ID_1}/sessions` with `{ "safetyCheckId": SC_ID_1 }`
3. Assert HTTP 201
4. Parse `exerciseSessionId` from response
5. Query DB directly for the created session

**Expected Result (PASS):**
- HTTP 201 with `sessionStatus = "IN_PROGRESS"`, `exerciseSessionId` non-null
- DB: `SELECT * FROM exercise_sessions WHERE exercise_session_id = '<returned-id>'` returns exactly 1 row
- DB row: `session_status = 'IN_PROGRESS'`, `paused_seconds = 0`, `ended_at IS NULL`, `user_id = USER_ID_1`, `safety_check_id = SC_ID_1`
- DB row: `started_at` is within 2 seconds of test start time (UTC)

**DB Assertion:**
```java
ExerciseSession saved = sessionRepository.findById(responseId).orElseThrow();
assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
assertThat(saved.getPausedSeconds()).isEqualTo(0);
assertThat(saved.getEndedAt()).isNull();
assertThat(saved.getUserId()).isEqualTo(USER_ID_1);
assertThat(saved.getSafetyCheckId()).isEqualTo(SC_ID_1);
assertThat(saved.getStartedAt()).isBeforeOrEqualTo(OffsetDateTime.now(ZoneOffset.UTC));
```

**Expected Result (FAIL):**
- Session not found in DB
- `session_status` has any value other than `IN_PROGRESS`
- `paused_seconds != 0`

**Current Status:** RED — not written

---

### SESS-TC-INT-002 — Duplicate Session Rejected at DB Level

**Severity:** `HIGH`
**Feature Under Test:** Duplicate session guard — EXSESS-004 returned on second POST
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionIntegrationTest.java`
**TDD Phase:** RED

**Preconditions:**
- Seed: PUBLISHED exercise, CLEARED safety check, existing IN_PROGRESS session from today

**Test Steps:**
1. Seed data including an active session
2. `POST /api/v1/exercises/{EX_ID_1}/sessions` (attempt second session)
3. Assert 409

**DB Assertion:**
```java
long sessionCount = sessionRepository.findAll().stream()
    .filter(s -> s.getExerciseId().equals(EX_ID_1) && s.getUserId().equals(USER_ID_1))
    .count();
assertThat(sessionCount).isEqualTo(1); // no new session created
```

**Current Status:** RED — not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit hash) | REFACTOR note |
|-------|-----------|---------------|---------------------|---------------|
| `SESS-TC-001` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-002` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-003` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-004` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-005` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-006` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | RED CRITICAL — safety gate |
| `SESS-TC-007` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-008` | `ExerciseSessionServiceImplTest.java` | `[ ]` | — | — |
| `SESS-TC-009` | `ExerciseSessionControllerTest.java` | `[ ]` | — | — |
| `SESS-TC-010` | `ExerciseSessionControllerTest.java` | `[ ]` | — | — |
| `SESS-TC-011` | `ExerciseSessionControllerTest.java` | `[ ]` | — | — |
| `SESS-TC-INT-001` | `ExerciseSessionIntegrationTest.java` | `[ ]` | — | Testcontainers |
| `SESS-TC-INT-002` | `ExerciseSessionIntegrationTest.java` | `[ ]` | — | Testcontainers |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Before implementing, run the full test suite with an empty/throw stub. Every test MUST FAIL.
> If any test PASSes with the stub → AP-AI-002 detected → reject and rewrite.

**Stub for Red Phase:**

```java
// ExerciseSessionServiceImpl.java — RED PHASE STUB (must throw)
@Service
@RequiredArgsConstructor
public class ExerciseSessionServiceImpl implements IExerciseSessionService {

    @Override
    public StartSessionResponse startSession(UUID exerciseId, StartSessionRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause if PASS unexpectedly |
|-------|-------------|----------|--------|----------------------------------|
| `SESS-TC-001` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state |
| `SESS-TC-002` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-003` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-004` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-005` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-006` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | ☐ Safety gate bypassed |
| `SESS-TC-007` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-008` | `throw UnsupportedOperationException` | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-009` | Bean not yet registered | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-010` | Spring Security blocks request | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-011` | Spring Security blocks request | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-INT-001` | Table exists but stub throws | RED FAIL | ☐ FAIL ☐ PASS | — |
| `SESS-TC-INT-002` | Table exists but stub throws | RED FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- All FAILs? ☐ Yes → **GATE-2 PASS** (T2 → T3) → proceed to implementation
- Evidence log: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Start Conditions)

- [ ] TDS `CB-EXERCISE-IMP-003` reviewed and status changed to `Approved`
- [ ] Logic Issues (Section 2) confirmed with Principal Architect
- [ ] `exercise_sessions` and `exercise_safety_checks` tables confirmed in staging DB (no new migration needed)
- [ ] Test fixtures (Section 3 TDS-05) prepared
- [ ] Existing exercise entities (`PregnancyExercise`, `ExerciseRepository`) confirmed reusable

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — all unit tests pass (no skips)
- [ ] `./mvnw verify` — all integration tests pass (Testcontainers)
- [ ] Test coverage ≥ 80% lines for `ExerciseSessionServiceImpl`
- [ ] No business logic in `ExerciseSessionController` (only `@Valid`, `@PreAuthorize`, delegate to service)
- [ ] No PII/secret in plaintext logs
- [ ] BR-SAFETY gate: SESS-TC-006 passes (BLOCKED safety check rejected)
- [ ] BR-RBAC gate: SESS-TC-010 and SESS-TC-011 pass

**Exit Criteria — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — all injected classes exist in codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — no shared mutable state between tests:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " src/test/java/com/carebridge/backend/exercise/service/ExerciseSessionServiceImplTest.java
  # Every instance must be inside @Test or use factory method
  ```
- [ ] **Oracle Source** — every `assertThat()` expected value has a traceable source (ADR/BR/V1 schema)

### Suspension Criteria

- Dependency `exercise_safety_checks` entity not yet created by another branch
- Integration conflict with parallel feature branch
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

No DB migration to rollback (tables exist in V1):

```bash
# Revert implementation files only
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/

# Revert test files
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/

# Feature gap remains OPEN — keep entry in sprint tracker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Warning Sign in Spec | Check | Gate |
|-------|-------------|----------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any ADR or BR constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS with empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes separate `exercise_pause_events` table exists | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies business logic inside `ExerciseSessionController` | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExerciseSafetyCheckService` (not in §8 interfaces) | ☐ | G-3 |

**Review Result:**
- [ ] No anti-patterns detected → TDD spec approved for implementation
- [ ] Anti-patterns detected → log below → fix before implementing

| AP detected | TC ID | Description | Fix Action | Fixed? |
|------------|-------|-------------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — CB-EXERCISE-IMP-003-TEST — UC179 Start Exercise Session*
*Status: Draft — awaiting review and Red Gate execution before implementation.*
