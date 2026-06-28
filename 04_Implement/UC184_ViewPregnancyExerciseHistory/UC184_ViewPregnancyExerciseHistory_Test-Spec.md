# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC184 — View Pregnancy Exercise History

**Document ID:** `CB-EXERCISE-TDD-184`
**Version:** `1.0`
**Date:** `2026-06-28`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Tech Lead Role`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending — endpoint is read-only; no PII written`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `04_Implement/UC184_ViewPregnancyExerciseHistory/UC184_ViewPregnancyExerciseHistory_TDS.md` — TDS CB-EXERCISE-IMP-184
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/` — existing exercise module
- `BR-RBAC` — Business Rule: Role-Based Access Control

> **TDD Convention:** This document describes test cases BEFORE any production code is written.
> Required order: write test (.java) → run → confirm FAIL (RED) → implement → PASS (GREEN) → refactor (BLUE).
> Do NOT mark a test as passing until `./mvnw test` is green.
> Do NOT use real PII in test data — SYNTHETIC data only.

---

## CHANGELOG

| Date | Author | Change Description |
|------|--------|--------------------|
| `2026-06-28` | `AI Agent` | Initial TDD spec — UC-184 View Pregnancy Exercise History |

---

## TABLE OF CONTENTS

1. [Module Information](#1-module-information)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Information

| Field | Value |
|-------|-------|
| **Feature / UC ID** | `UC-184 — View Pregnancy Exercise History` |
| **Function ID** | `3.3.2.10` |
| **Module** | `ExerciseSessionHistory — Exercise Bounded Context` |
| **TDS Reference** | `CB-EXERCISE-IMP-184` |
| **Priority** | Medium (P2) |
| **Sprint** | `Current Sprint (2026-06-28 onward)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC` |
| **Upstream Dependencies** | `IAM module (JWT)`, `exercise_sessions table (V1)`, `pregnancy_exercises table (V1)` |
| **Downstream Consumers** | `Mobile App — Exercise History Screen (Flutter)` |

### 1.1. AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `TDS CB-EXERCISE-IMP-184 §17`, `ADR-001`, `ADR-002`, `ADR-003` |
| **Constraints Injected** | C1 (Principal injection), C2 (user_id WHERE), C3 (COMPLETED filter), C4 (JOIN FETCH), C5 (size clamp), C6 (PreAuthorize MOTHER), C7 (readOnly transaction), C8 (effectiveDuration computation) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate §5.1)` |

---

## 2. Logic Issues Resolved

> These issues were identified by reconciling the UC-184 feature description against the actual `V1__init_schema.sql` and codebase.

| # | Original spec assumption (incorrect) | Actual state (schema / codebase) | Fix applied in tests |
|---|--------------------------------------|----------------------------------|----------------------|
| L1 | "duration" can be read from `exercise_sessions.duration` | No `duration` column exists in `exercise_sessions`. Effective duration must be computed: `(ended_at - started_at)` in seconds minus `paused_seconds` | Tests assert `effectiveDurationSeconds` is computed, not a raw column value |
| L2 | Filter by "exercise title" (free text search) | No full-text index on `pregnancy_exercises.title`. Free-text title filter was deprioritized in API design; only `trimester`, `from`, `to` filters are supported | Tests cover only the three supported filter parameters; title filter test is explicitly excluded |
| L3 | "All sessions" including IN_PROGRESS | Only `session_status = 'COMPLETED'` sessions are shown per FR-184-001 design decision | Tests assert IN_PROGRESS / PAUSED / ABANDONED sessions are excluded from results |
| L4 | Group-by-month is a backend concern | Month grouping is a mobile client UX concern; backend returns flat paginated list sorted by `started_at DESC`. The mobile client groups by month locally. | No group-by-month logic is tested in backend test cases |
| L5 | `ExerciseSession` JPA entity exists | No `ExerciseSession.java` entity exists yet in the codebase. It must be created. Tests reference `ExerciseSession` — confirm class must be created before tests can compile. | Tests include compilation guard (`./mvnw compile`) as a required pre-step |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
ExerciseSessionHistory module covers these layers:
├── Entity (ExerciseSession.java — new, maps exercise_sessions table)
├── DTO (ExerciseSessionHistorySummary.java — new)
├── Repository (ExerciseSessionHistoryRepository.java — new, JPQL query)
├── Mapper (ExerciseSessionHistoryMapper.java — new, effectiveDuration logic)
├── Service (ExerciseSessionHistoryServiceImpl.java — new, clamping + orchestration)
└── Controller (ExerciseController.java — add getSessionHistory endpoint)
```

**Out of scope:**
- `exercise_sessions` write operations (covered by UC-179, UC-181, UC-182)
- Mobile Flutter widget tests for month grouping (Flutter responsibility)
- Admin-scoped session history view (not part of UC-184)

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `UC-184` | Mother views own completed exercise history; paginated list; filterable |
| `TDS CB-EXERCISE-IMP-184 §9` | API contract: GET `/api/v1/exercises/sessions/history`, query params, response shape |
| `ADR-001` | Principal injection pattern for ownership enforcement |
| `ADR-002` | JOIN FETCH must prevent N+1; single SQL query per page |
| `ADR-003` | Offset pagination; size clamped to [1,50] |
| `BR-RBAC-001` | Only ROLE_MOTHER may call this endpoint |
| `BR-RBAC-002` | Mother sees only her own sessions (user_id scoping) |
| `FR-184-001` | Only COMPLETED sessions returned |
| `FR-184-002` | Optional filters: trimester, from, to |
| `FR-184-003` | Response fields: exerciseTitle, startedAt, effectiveDurationSeconds, completionPercent, postureScore, warningCount |
| `V1__init_schema.sql` | exercise_sessions columns; no duration column; paused_seconds exists |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother has completed sessions — returns paginated list | `ExerciseSessionHistoryServiceImpl.getHistory()` | EXH-TC-001 |
| TC-COND-002 | Page size > 50 → clamped to 50 | `ExerciseSessionHistoryServiceImpl` size clamping | EXH-TC-002 |
| TC-COND-003 | Page size < 1 → clamped to 1 | `ExerciseSessionHistoryServiceImpl` size clamping | EXH-TC-003 |
| TC-COND-004 | `from` and `to` date params converted to OffsetDateTime at day boundaries | `ExerciseSessionHistoryServiceImpl` date conversion | EXH-TC-004 |
| TC-COND-005 | Mapper computes effectiveDurationSeconds correctly | `ExerciseSessionHistoryMapper.toSummary()` | EXH-TC-005 |
| TC-COND-006 | Mapper handles null postureScore | `ExerciseSessionHistoryMapper.toSummary()` | EXH-TC-006 |
| TC-COND-007 | Mapper handles null endedAt (COMPLETED but no endedAt — defensive) | `ExerciseSessionHistoryMapper.toSummary()` | EXH-TC-007 |
| TC-COND-008 | Mother with no completed sessions → empty page, HTTP 200 | Full stack | EXH-TC-INT-002 |
| TC-COND-009 | Filter by trimester — only matching sessions returned | Repository JPQL + full stack | EXH-TC-INT-003 |
| TC-COND-010 | Filter by date range — only sessions within range returned | Repository JPQL + full stack | EXH-TC-INT-004 |
| TC-COND-011 | IN_PROGRESS sessions not included in response | Repository JPQL filter | EXH-TC-INT-005 |
| TC-COND-012 | Request without JWT → 401 | Spring Security filter | EXH-TC-SEC-001 |
| TC-COND-013 | Request with non-MOTHER role → 403 | `@PreAuthorize` | EXH-TC-SEC-002 |
| TC-COND-014 | Mother A cannot see Mother B's sessions | Ownership WHERE clause | EXH-TC-SEC-003 |
| TC-COND-015 | SQL injection in `from` param → 400 (type conversion rejects) | Spring param binding | EXH-TC-SEC-004 |
| TC-COND-016 | Repository issues single SQL query per request (no N+1) | JPQL JOIN FETCH verification | EXH-TC-INT-006 |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | size param: valid [1,50], clamped (<1 or >50) | Ensure clamping logic covers all partitions |
| Boundary Value Analysis | size=0, size=1, size=50, size=51; page=0, page=MAX_INT | Edge of valid ranges |
| State Transition Testing | session_status: COMPLETED (included), IN_PROGRESS, PAUSED, ABANDONED (excluded) | Filter correctness |
| Date Boundary Testing | from/to at midnight UTC, from=to, from=null, to=null | Date conversion and inclusive boundaries |
| Error Guessing | Null userId injection, SQL injection in date param, invalid enum value | Security + robustness |

### TDS-05 — Test Data Requirements (SYNTHETIC)

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | Mother user: `userId = UUID("00000000-0000-0000-0000-000000000001")`, role = MOTHER | Authenticated test user |
| `FX-002` | DB seed | Other Mother: `userId = UUID("00000000-0000-0000-0000-000000000002")` | Data isolation check |
| `FX-003` | DB seed | `pregnancy_exercises`: id=`ex-001`, title="Prenatal Yoga", trimesterScope=SECOND, status=PUBLISHED | Exercise to join against |
| `FX-004` | DB seed | `exercise_sessions`: `session_status=COMPLETED`, userId=FX-001, exerciseId=FX-003, startedAt=2026-06-01T08:00:00+07:00, endedAt=2026-06-01T08:30:00+07:00, pausedSeconds=120, completionPercent=95.5, postureScore=87.3, warningCount=1 | Primary happy path session |
| `FX-005` | DB seed | `exercise_sessions`: `session_status=IN_PROGRESS`, userId=FX-001, exerciseId=FX-003, startedAt=2026-06-28T08:00:00+07:00 | Must be excluded from results |
| `FX-006` | DB seed | `exercise_sessions`: `session_status=COMPLETED`, userId=FX-002, exerciseId=FX-003, startedAt=2026-06-15T10:00:00+07:00 | Other Mother's session — must not appear for FX-001 |
| `FX-007` | DB seed | `pregnancy_exercises`: id=`ex-002`, title="First Trimester Walk", trimesterScope=FIRST, status=PUBLISHED | For trimester filter test |
| `FX-008` | DB seed | `exercise_sessions`: COMPLETED, userId=FX-001, exerciseId=FX-007 (FIRST trimester exercise), startedAt=2026-03-01 | For trimester filter exclusion test |
| `FX-009` | JWT | `{ sub: "00000000-0000-0000-0000-000000000001", roles: ["ROLE_MOTHER"] }` | Valid MOTHER JWT |
| `FX-010` | JWT | `{ sub: "admin-uuid", roles: ["ROLE_ADMIN"] }` | Admin JWT for 403 test |
| `FX-011` | DB seed | 55 COMPLETED sessions for FX-001, same exerciseId FX-003 | Pagination boundary test (size=50 clamp) |

---

## 4. Test Case Specification

> **TC ID format:** `EXH-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** All tests start at RED (not yet written)

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

> Every test MUST use fresh instances via factory. No shared mutable state between tests.

```java
// ═══════════════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// File: src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryTestFactory.java
// ═══════════════════════════════════════════════════════════════════
package com.carebridge.backend.exercise;

import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

class ExerciseSessionHistoryTestFactory {

    static final UUID MOTHER_USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID      = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID SESSION_ID       = UUID.fromString("00000000-0000-0000-0000-000000000004");

    static final OffsetDateTime STARTED_AT =
        OffsetDateTime.of(2026, 6, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7));
    static final OffsetDateTime ENDED_AT =
        OffsetDateTime.of(2026, 6, 1, 8, 30, 0, 0, ZoneOffset.ofHours(7));

    /** Baseline valid COMPLETED session owned by MOTHER_USER_ID */
    static ExerciseSession makeSession() {
        ExerciseSession s = new ExerciseSession();
        s.setExerciseSessionId(SESSION_ID);
        s.setExerciseId(EXERCISE_ID);
        s.setUserId(MOTHER_USER_ID);
        s.setStartedAt(STARTED_AT);
        s.setEndedAt(ENDED_AT);
        s.setPausedSeconds(120);                         // 2 minutes paused
        s.setCompletionPercent(new BigDecimal("95.5"));
        s.setPostureScore(new BigDecimal("87.3"));
        s.setSessionStatus("COMPLETED");
        s.setWarningCount(1);
        s.setExercise(makeExercise());
        return s;
    }

    static ExerciseSession makeSession(Consumer<ExerciseSession> overrides) {
        ExerciseSession s = makeSession();
        overrides.accept(s);
        return s;
    }

    static PregnancyExercise makeExercise() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(EXERCISE_ID);
        e.setTitle("Prenatal Yoga — Hip Opener");
        e.setTrimesterScope(TrimesterScope.SECOND);
        e.setDurationMinutes((short) 30);
        e.setStatus(ExerciseStatus.PUBLISHED);
        return e;
    }

    static PregnancyExercise makeExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise e = makeExercise();
        overrides.accept(e);
        return e;
    }
}
```

---

### EXH-TC-001 — Service returns paginated history for valid userId

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryServiceTest.java`
**TDD Phase:** RED — not yet implemented
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-184 AC-001`, `TDS CB-EXERCISE-IMP-184 §8.2`

**Preconditions:**
- `ExerciseSessionHistoryRepository` is mocked with Mockito
- `ExerciseSessionHistoryMapper` is mocked with Mockito
- Fixture `FX-004` session available as return value from repository mock
- Fixture `FX-009` userId available as `MOTHER_USER_ID`

**Test Steps:**
1. Arrange: create mock `Page<ExerciseSession>` containing one `FX-004` session. Stub `repository.findCompletedByUserIdAndFilters(MOTHER_USER_ID, "COMPLETED", null, null, null, PageRequest.of(0, 20, Sort.by("startedAt").descending()))` to return the mock page. Stub `mapper.toSummary(session)` to return a valid `ExerciseSessionHistorySummary`.
2. Act: call `service.getHistory(MOTHER_USER_ID, null, null, null, 0, 20)`.
3. Assert:
   - Result is a `PaginatedResponse<ExerciseSessionHistorySummary>`
   - `result.getData()` has size 1
   - `result.getPage()` == 0
   - `result.getSize()` == 20
   - `result.getTotalElements()` == 1
   - Repository was called exactly once with `status = "COMPLETED"` and `userId = MOTHER_USER_ID`

**Expected Result (PASS):**
- `PaginatedResponse` correctly populated; repository called once with correct params.

**Expected Result (FAIL — indication of broken implementation):**
- Repository called with wrong userId (IDOR)
- Repository called with status != "COMPLETED"
- `PaginatedResponse` fields do not match page metadata

**Current Status:** RED — not written
**Implementation Note:** Service must call `SecurityUtils` NOT here — userId is already passed in as parameter (Principal resolution is in the controller, ADR-001).

---

### EXH-TC-002 — Service clamps page size > 50 to 50

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl.getHistory()` — size clamping
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-003`, `TDS CB-EXERCISE-IMP-184 §8.2 (C5)`

**Preconditions:**
- Repository and mapper mocked.

**Test Steps:**
1. Arrange: stub repository to return empty page for any pageable.
2. Act: call `service.getHistory(MOTHER_USER_ID, null, null, null, 0, 100)`.
3. Assert: repository was called with `PageRequest` where `getPageSize() == 50`.

**Expected Result (PASS):** Repository receives `size = 50` not `size = 100`.
**Expected Result (FAIL):** Repository called with `size = 100` — clamping not applied.

**Current Status:** RED — not written
**Implementation Note:** `int clampedSize = Math.min(Math.max(size, 1), 50);`

---

### EXH-TC-003 — Service clamps page size < 1 to 1

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl.getHistory()` — size clamping lower bound
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-003`

**Test Steps:**
1. Arrange: stub repository to return empty page.
2. Act: call `service.getHistory(MOTHER_USER_ID, null, null, null, 0, 0)`.
3. Assert: repository called with `PageRequest` where `getPageSize() == 1`.

**Expected Result (PASS):** size clamped to 1.
**Expected Result (FAIL):** size passed as 0 → repository throws or returns 0-size page.

**Current Status:** RED — not written

---

### EXH-TC-004 — Service converts LocalDate to OffsetDateTime at day boundaries

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl.getHistory()` — date conversion
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS CB-EXERCISE-IMP-184 §8.2`, `FR-184-002`

**Test Steps:**
1. Arrange: stub repository for any pageable and dates. Capture the `from` and `to` OffsetDateTime arguments using Mockito `ArgumentCaptor`.
2. Act: call `service.getHistory(MOTHER_USER_ID, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 28), 0, 20)`.
3. Assert:
   - Captured `from` is `2026-01-01T00:00:00` at server zone (or UTC+7)
   - Captured `to` is `2026-06-28T23:59:59` at server zone (or start of next day)

**Expected Result (PASS):** Dates expanded to full day boundaries; sessions at day edges are included.
**Expected Result (FAIL):** Dates passed as-is at midnight only → sessions started at 23:30 on `to` date missed.

**Current Status:** RED — not written
**Implementation Note:** Service converts `from` → `from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime()` and `to` → `to.plusDays(1).atStartOfDay(...).toOffsetDateTime()` (exclusive upper bound) OR `to.atTime(LocalTime.MAX).atOffset(...)` (inclusive). Chosen approach must be documented and consistent.

---

### EXH-TC-005 — Mapper computes effectiveDurationSeconds correctly

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionHistoryMapper.toSummary()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS CB-EXERCISE-IMP-184 §8.4 (C8)`

**Preconditions:**
- Session: startedAt = `2026-06-01T08:00:00+07:00`, endedAt = `2026-06-01T08:30:00+07:00`, pausedSeconds = 120
- Expected: rawSeconds = 1800, effectiveDurationSeconds = 1800 - 120 = 1680

**Test Steps:**
1. Arrange: `ExerciseSession session = ExerciseSessionHistoryTestFactory.makeSession()` (has startedAt, endedAt, pausedSeconds=120, and joined exercise).
2. Act: `ExerciseSessionHistorySummary dto = mapper.toSummary(session)`.
3. Assert: `dto.getEffectiveDurationSeconds() == 1680L`.

**Expected Result (PASS):** `effectiveDurationSeconds = 1680` (1800 raw - 120 paused).
**Expected Result (FAIL):** returns `1800` (pausedSeconds not subtracted) or `0` (wrong computation).

**Current Status:** RED — not written

---

### EXH-TC-006 — Mapper handles null postureScore

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryMapper.toSummary()` — null field handling
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `V1__init_schema.sql` — `posture_score numeric` (nullable, no NOT NULL constraint)

**Test Steps:**
1. Arrange: `ExerciseSession session = ExerciseSessionHistoryTestFactory.makeSession(s -> s.setPostureScore(null))`.
2. Act: `ExerciseSessionHistorySummary dto = mapper.toSummary(session)`.
3. Assert:
   - No `NullPointerException` thrown
   - `dto.getPostureScore()` is `null`
   - Other fields mapped correctly

**Expected Result (PASS):** DTO has `postureScore = null`; no exception.
**Expected Result (FAIL):** `NullPointerException` thrown → mapper does not handle nullable postureScore.

**Current Status:** RED — not written

---

### EXH-TC-007 — Mapper handles null endedAt

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryMapper.toSummary()` — defensive null endedAt
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `V1__init_schema.sql` — `ended_at timestamptz` (nullable); defensive guard per TDS §8.4

**Test Steps:**
1. Arrange: `ExerciseSession session = ExerciseSessionHistoryTestFactory.makeSession(s -> s.setEndedAt(null))`.
2. Act: `ExerciseSessionHistorySummary dto = mapper.toSummary(session)`.
3. Assert:
   - No `NullPointerException`
   - `dto.getEffectiveDurationSeconds() == 0L`
   - `dto.getEndedAt()` is `null`

**Expected Result (PASS):** `effectiveDurationSeconds = 0`; no crash.
**Expected Result (FAIL):** `NullPointerException` on `endedAt` subtraction.

**Current Status:** RED — not written

---

### EXH-TC-008 — Mapper maps all required fields from joined exercise

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionHistoryMapper.toSummary()` — JOIN FETCH field mapping
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS CB-EXERCISE-IMP-184 §8.1 DTO spec`, `FR-184-003`

**Test Steps:**
1. Arrange: `ExerciseSession session = ExerciseSessionHistoryTestFactory.makeSession()` with exercise having `title = "Prenatal Yoga — Hip Opener"`, `trimesterScope = SECOND`.
2. Act: `ExerciseSessionHistorySummary dto = mapper.toSummary(session)`.
3. Assert all fields:
   - `dto.getExerciseSessionId()` == `SESSION_ID`
   - `dto.getExerciseId()` == `EXERCISE_ID`
   - `dto.getExerciseTitle()` == `"Prenatal Yoga — Hip Opener"`
   - `dto.getTrimesterScope()` == `"SECOND"`
   - `dto.getStartedAt()` == `STARTED_AT`
   - `dto.getEndedAt()` == `ENDED_AT`
   - `dto.getCompletionPercent()` == `new BigDecimal("95.5")`
   - `dto.getPostureScore()` == `new BigDecimal("87.3")`
   - `dto.getWarningCount()` == `1`
   - `dto.getSessionStatus()` == `"COMPLETED"`

**Expected Result (PASS):** All fields correctly mapped from session and session.exercise.
**Expected Result (FAIL):** Any field is `null` when it should have a value, or mismatched value.

**Current Status:** RED — not written

---

### SECURITY TEST CASES

---

### EXH-TC-SEC-001 — Request without JWT returns 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `GET /api/v1/exercises/sessions/history` — Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `BR-RBAC-001`, `TDS §9.2 Response 401`

**Preconditions:**
- `@WebMvcTest(ExerciseController.class)` with Spring Security enabled
- No Authorization header in request

**Test Steps:**
1. Arrange: `MockMvc` configured with Spring Security.
2. Act: `mockMvc.perform(get("/api/v1/exercises/sessions/history"))`.
3. Assert: response status is `401 Unauthorized`.

**Expected Result (PASS = system is secure):** HTTP 401 returned; no data leaked.
**Expected Result (FAIL = vulnerability):** HTTP 200 returned with session data — authentication bypass.

**Current Status:** RED — not written

---

### EXH-TC-SEC-002 — ADMIN JWT returns 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-284 — Improper Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on `ExerciseController.getSessionHistory()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §16 Authorization Matrix`, `BR-RBAC-001`

**Test Steps:**
1. Arrange: `@WithMockUser(roles = {"ADMIN"})` or JWT with `ROLE_ADMIN` claim.
2. Act: `mockMvc.perform(get("/api/v1/exercises/sessions/history").with(authentication(adminToken)))`.
3. Assert: response status is `403 Forbidden`.

**Expected Result (PASS):** HTTP 403.
**Expected Result (FAIL):** HTTP 200 — Admin can access Mother's history (authorization bypass).

**Current Status:** RED — not written

---

### EXH-TC-SEC-003 — Mother A cannot see Mother B's sessions (IDOR prevention)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC-002`
**Feature Under Test:** `ExerciseSessionHistoryRepository` — `WHERE es.userId = :userId`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §3 ADR-001`, `BR-RBAC-002`

**Preconditions:**
- Testcontainers PostgreSQL running
- DB seeded with `FX-004` (MOTHER A's session) and `FX-006` (MOTHER B's session — same exercise)
- JWT for MOTHER A (FX-009)

**Test Steps:**
1. Seed `FX-004` (userId = MOTHER_USER_ID) and `FX-006` (userId = OTHER_USER_ID) into DB.
2. Call `GET /api/v1/exercises/sessions/history` authenticated as MOTHER A (FX-009).
3. Assert:
   - HTTP 200
   - `data` array contains only session owned by `MOTHER_USER_ID`
   - `data` does NOT contain `OTHER_USER_ID`'s session
   - `totalElements == 1` (only MOTHER A's session)

**Expected Result (PASS):** Response contains 1 item — MOTHER A's session only.
**Expected Result (FAIL):** Response contains MOTHER B's session — IDOR vulnerability active.

**Current Status:** RED — not written
**Implementation Note:** This test validates ADR-001 (C2) — the `WHERE es.userId = :userId` clause in the JPQL query.

---

### EXH-TC-SEC-004 — SQL injection in `from` parameter is safely rejected

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** Spring `@RequestParam` type binding for `LocalDate from`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-015`

**Test Steps (Attack Simulation):**
1. Send request: `GET /api/v1/exercises/sessions/history?from=2026-01-01'; DROP TABLE exercise_sessions; --` with valid MOTHER JWT.
2. Assert:
   - Response status is `400 Bad Request` (Spring `MethodArgumentTypeMismatchException`)
   - No database tables are dropped
   - Error body contains `EXH-001` or equivalent bad param error

**Expected Result (PASS = system is safe):** HTTP 400; no DB mutation.
**Expected Result (FAIL = vulnerable):** Query executed with injected SQL, or unexpected 500 error leaking stack trace.

**Current Status:** RED — not written

---

### INTEGRATION TEST CASES

---

### EXH-TC-INT-001 — Happy path full flow: seed COMPLETED session, call endpoint, assert response

**Severity:** `HIGH`
**Feature Under Test:** Full stack: Controller → Service → Repository → DB → Mapper → DTO
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`

**Preconditions:**
- `@SpringBootTest` + `@Testcontainers` with PostgreSQL container
- Flyway migrations applied automatically on Spring context start
- Seed `FX-003` (pregnancy_exercises) and `FX-004` (exercise_sessions COMPLETED for MOTHER_USER_ID)
- JWT for MOTHER_USER_ID (FX-009)

**Test Steps:**
1. Seed: insert `FX-003` exercise, then `FX-004` COMPLETED session referencing it.
2. Call `GET /api/v1/exercises/sessions/history?page=0&size=20` with MOTHER JWT.
3. Parse JSON response.
4. Assert:
   - HTTP 200
   - `success = true`
   - `data.length == 1`
   - `data[0].exerciseTitle == "Prenatal Yoga — Hip Opener"`
   - `data[0].trimesterScope == "SECOND"`
   - `data[0].sessionStatus == "COMPLETED"`
   - `data[0].completionPercent == 95.5`
   - `data[0].warningCount == 1`
   - `data[0].effectiveDurationSeconds == 1680` (1800 raw - 120 paused)
   - `totalElements == 1`
   - `totalPages == 1`

**Expected Result (PASS):** All assertions green.
**Expected Result (FAIL):** Any assertion mismatch — data missing, wrong field values, or effectiveDuration miscalculated.

**DB Assertion:**
```java
List<ExerciseSession> dbSessions = exerciseSessionRepo.findAll();
assertThat(dbSessions).anyMatch(s ->
    s.getUserId().equals(MOTHER_USER_ID) &&
    s.getSessionStatus().equals("COMPLETED"));
```

**Current Status:** RED — not written

---

### EXH-TC-INT-002 — Empty history returns HTTP 200 with empty data array

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl.getHistory()` — empty result
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §9.2 — Empty History Response`

**Preconditions:**
- DB has no `exercise_sessions` rows for `MOTHER_USER_ID`
- JWT for MOTHER_USER_ID valid

**Test Steps:**
1. Ensure DB has NO sessions for MOTHER_USER_ID.
2. Call `GET /api/v1/exercises/sessions/history?page=0&size=20` with MOTHER JWT.
3. Assert:
   - HTTP 200 (not 404)
   - `data == []`
   - `totalElements == 0`
   - `totalPages == 0`

**Expected Result (PASS):** Empty paginated response; no error thrown.
**Expected Result (FAIL):** HTTP 404 or 500 for empty history; empty collection treated as "not found".

**Current Status:** RED — not written

---

### EXH-TC-INT-003 — Trimester filter returns only matching sessions

**Severity:** `MEDIUM`
**Feature Under Test:** Repository JPQL `(:trimester IS NULL OR e.trimesterScope = :trimester)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `FR-184-002`, `TDS §9.1 query params`

**Preconditions:**
- DB seeded with `FX-004` (SECOND trimester session for MOTHER) and `FX-008` (FIRST trimester session for MOTHER)

**Test Steps:**
1. Seed both sessions.
2. Call `GET /api/v1/exercises/sessions/history?trimester=SECOND`.
3. Assert:
   - HTTP 200
   - `data.length == 1`
   - `data[0].trimesterScope == "SECOND"`
   - FIRST trimester session NOT in `data`

**Expected Result (PASS):** Only SECOND trimester session returned.
**Expected Result (FAIL):** Both sessions returned (trimester filter not applied), or wrong session returned.

**Current Status:** RED — not written

---

### EXH-TC-INT-004 — Date range filter returns only sessions within range

**Severity:** `MEDIUM`
**Feature Under Test:** Repository JPQL `(:from IS NULL OR es.startedAt >= :from) AND (:to IS NULL OR es.startedAt <= :to)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `FR-184-002`

**Preconditions:**
- DB seeded with COMPLETED session on `2026-06-01` (FX-004) and another on `2026-05-01` (out-of-range session)
- Both for MOTHER_USER_ID

**Test Steps:**
1. Seed two sessions: one on 2026-06-01, one on 2026-05-01.
2. Call `GET /api/v1/exercises/sessions/history?from=2026-06-01&to=2026-06-30`.
3. Assert:
   - HTTP 200
   - `data.length == 1`
   - `data[0].startedAt` is within `[2026-06-01, 2026-06-30]`
   - The 2026-05-01 session is NOT in `data`

**Expected Result (PASS):** Only the June session returned.
**Expected Result (FAIL):** May session appears in results (date filter not applied).

**Current Status:** RED — not written

---

### EXH-TC-INT-005 — IN_PROGRESS sessions excluded from history

**Severity:** `HIGH`
**Feature Under Test:** Repository JPQL `AND es.sessionStatus = :status` where status = "COMPLETED"
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `FR-184-001`, `TDS §3 ADR`, `C3 constraint`

**Preconditions:**
- DB seeded with `FX-004` (COMPLETED) and `FX-005` (IN_PROGRESS) for MOTHER_USER_ID

**Test Steps:**
1. Seed `FX-004` (COMPLETED) and `FX-005` (IN_PROGRESS).
2. Call `GET /api/v1/exercises/sessions/history` with MOTHER JWT.
3. Assert:
   - HTTP 200
   - `data.length == 1` (only COMPLETED session)
   - `data[0].sessionStatus == "COMPLETED"`
   - The IN_PROGRESS session (FX-005) is NOT present in `data`

**Expected Result (PASS):** Only COMPLETED session appears.
**Expected Result (FAIL):** IN_PROGRESS session appears in history (status filter not applied).

**Current Status:** RED — not written

---

### EXH-TC-INT-006 — Single SQL query issued per page request (N+1 prevention)

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionHistoryRepository` JPQL JOIN FETCH (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS §3 ADR-002`

**Preconditions:**
- DB seeded with 5 COMPLETED sessions for MOTHER_USER_ID, each referencing a different exercise
- Hibernate SQL logging enabled (`spring.jpa.show-sql=true` or `@Sql` in test)

**Test Steps:**
1. Enable Hibernate statistics: `entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics()` — enable and reset.
2. Seed 5 sessions with 5 different exercises.
3. Call service `getHistory(MOTHER_USER_ID, null, null, null, 0, 10)`.
4. Assert: `statistics.getQueryExecutionCount() == 1` (single query for the page; no per-session exercise lookups).

**Expected Result (PASS):** Exactly 1 SQL query executed.
**Expected Result (FAIL):** 6 queries executed (1 + 5 for each exercise lookup) — N+1 problem not solved.

**DB Assertion:**
```java
SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
sf.getStatistics().setStatisticsEnabled(true);
sf.getStatistics().clear();

service.getHistory(MOTHER_USER_ID, null, null, null, 0, 10);

assertThat(sf.getStatistics().getQueryExecutionCount()).isEqualTo(1L);
```

**Current Status:** RED — not written

---

### EXH-TC-INT-007 — Pagination: second page returns correct offset

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseSessionHistoryServiceImpl` + `ExerciseController` pagination
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`

**Preconditions:**
- DB seeded with 25 COMPLETED sessions for MOTHER_USER_ID, all with distinct `started_at` timestamps in descending order

**Test Steps:**
1. Seed 25 sessions.
2. Call `GET /api/v1/exercises/sessions/history?page=1&size=20`.
3. Assert:
   - HTTP 200
   - `data.length == 5` (page 1 of 25 sessions with size 20 = 5 remaining)
   - `page == 1`
   - `totalElements == 25`
   - `totalPages == 2`

**Expected Result (PASS):** Second page returns 5 items correctly.
**Expected Result (FAIL):** Second page returns 20 items (duplicates from first page) or 0 items.

**Current Status:** RED — not written

---

### EXH-TC-INT-008 — Invalid trimester enum returns 400

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseController` `@RequestParam` conversion for `TrimesterScope`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-012` (input validation)
**Oracle Source:** `TDS §10 Error Code EXH-001`

**Test Steps:**
1. Arrange: MOTHER JWT (FX-009).
2. Act: `GET /api/v1/exercises/sessions/history?trimester=INVALID_VALUE` with MOTHER JWT.
3. Assert:
   - HTTP 400
   - Response contains error code `EXH-001` (or framework equivalent)
   - Service is never called

**Expected Result (PASS):** HTTP 400 with informative error message.
**Expected Result (FAIL):** HTTP 500 (unhandled exception), or query proceeds with null trimester ignoring the invalid value.

**Current Status:** RED — not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit hash) | REFACTOR note |
|-------|-----------|--------------|---------------------|---------------|
| `EXH-TC-001` | `ExerciseSessionHistoryServiceTest.java` | `[ ]` | — | — |
| `EXH-TC-002` | `ExerciseSessionHistoryServiceTest.java` | `[ ]` | — | — |
| `EXH-TC-003` | `ExerciseSessionHistoryServiceTest.java` | `[ ]` | — | — |
| `EXH-TC-004` | `ExerciseSessionHistoryServiceTest.java` | `[ ]` | — | — |
| `EXH-TC-005` | `ExerciseSessionHistoryMapperTest.java` | `[ ]` | — | — |
| `EXH-TC-006` | `ExerciseSessionHistoryMapperTest.java` | `[ ]` | — | — |
| `EXH-TC-007` | `ExerciseSessionHistoryMapperTest.java` | `[ ]` | — | — |
| `EXH-TC-008` | `ExerciseSessionHistoryMapperTest.java` | `[ ]` | — | — |
| `EXH-TC-SEC-001` | `ExerciseControllerSecurityTest.java` | `[ ]` | — | — |
| `EXH-TC-SEC-002` | `ExerciseControllerSecurityTest.java` | `[ ]` | — | — |
| `EXH-TC-SEC-003` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-SEC-004` | `ExerciseControllerSecurityTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-001` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-002` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-003` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-004` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-005` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-006` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-007` | `ExerciseSessionHistoryIntegrationTest.java` | `[ ]` | — | — |
| `EXH-TC-INT-008` | `ExerciseControllerSecurityTest.java` | `[ ]` | — | — |

### 5.1. Red Gate Protocol (CASE 2.0 — GATE-2)

> All tests MUST FAIL before any implementation. If a test PASSES with the stub → AP-AI-002 detected → rewrite the test.

**Stub for Red Phase:**

```java
// ExerciseSessionHistoryServiceImpl.java — RED PHASE STUB
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseSessionHistoryServiceImpl implements IExerciseSessionHistoryService {

    private final ExerciseSessionHistoryRepository repository;
    private final ExerciseSessionHistoryMapper mapper;

    @Override
    public PaginatedResponse<ExerciseSessionHistorySummary> getHistory(
            UUID userId,
            TrimesterScope trimester,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// ExerciseSessionHistoryMapper.java — RED PHASE STUB
@Component
public class ExerciseSessionHistoryMapper {
    public ExerciseSessionHistorySummary toSummary(ExerciseSession session) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification Table:**

| TC ID | Stub Result | Expected | Actual (fill in) | Root Cause if PASS |
|-------|-------------|----------|------------------|--------------------|
| `EXH-TC-001` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-002` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-003` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-004` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-005` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-006` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-007` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-008` | `throw UnsupportedOperationException` | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-SEC-001` | Endpoint does not exist | FAIL (404) | `[ ]` FAIL / `[ ]` PASS | If PASS: security misconfigured |
| `EXH-TC-SEC-002` | Endpoint does not exist | FAIL (404 or 403) | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-SEC-003` | No data; 404 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-SEC-004` | No endpoint; 404 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-001` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-002` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-003` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-004` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-005` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-006` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-007` | `throw` → 500 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |
| `EXH-TC-INT-008` | No endpoint → 404 | FAIL | `[ ]` FAIL / `[ ]` PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___ (fill in after committing stubs)`
- All FAIL? `[ ]` Yes → **GATE-2 PASS** (T2→T3) → proceed to implementation
- Log file: `target/surefire-reports/` (fill in after running `./mvnw test`)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EXERCISE-IMP-184` reviewed and approved by Principal Architect
- [ ] Logic Issues (§2 above) confirmed with Tech Lead
- [ ] `V1__init_schema.sql` confirmed applied on target DB (both `exercise_sessions` and `pregnancy_exercises` tables exist)
- [ ] Test fixtures `FX-001` through `FX-011` (§3 TDS-05) defined and ready to seed
- [ ] `ExerciseSession` JPA entity class planned for creation (L5 from §2)
- [ ] Testcontainers PostgreSQL dependency available in test scope (`pom.xml`)

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — all unit tests green (ExerciseSessionHistoryServiceTest, ExerciseSessionHistoryMapperTest)
- [ ] `./mvnw verify` — all integration tests green (ExerciseSessionHistoryIntegrationTest, ExerciseControllerSecurityTest)
- [ ] Test coverage ≥ 80% lines for `ExerciseSessionHistoryServiceImpl` and `ExerciseSessionHistoryMapper`
- [ ] `ExerciseController.getSessionHistory()` has no business logic — only param extraction, Principal resolution, and delegation to service
- [ ] No PII (phone, name, email) appears in application logs during integration test run
- [ ] Security tests EXH-TC-SEC-001 through EXH-TC-SEC-004 all green
- [ ] N+1 test (EXH-TC-INT-006) confirms single query per page
- [ ] `./mvnw compile` outputs zero errors

**CASE 2.0 Exit Criteria:**

- [ ] **Red Gate (§5.1)** — all 20 tests FAILED with stub before implementation began
- [ ] **Contract Existence** — all imported classes (`ExerciseSession`, `IExerciseSessionHistoryService`, etc.) compile:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — `ExerciseSessionHistoryTestFactory` used in every test; no shared mutable static state
- [ ] **Oracle Source** — every `assertThat()` in tests has a comment referencing the BR/AC/ADR that defines the expected value

### Suspension Criteria

- `ExerciseSession` JPA entity blocked (schema dispute, naming conflict with other branch)
- Testcontainers not available in CI environment
- `V1__init_schema.sql` not applied on staging (migration runner issue)
- Conflicting changes to `ExerciseController.java` from another branch causing merge conflicts

---

## 7. Rollback Plan

```bash
# Revert all implementation files (no new migration was applied in default deploy)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/ExerciseSession.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/dto/ExerciseSessionHistorySummary.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/repository/ExerciseSessionHistoryRepository.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/mapper/ExerciseSessionHistoryMapper.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/IExerciseSessionHistoryService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/ExerciseSessionHistoryServiceImpl.java
# Revert the getSessionHistory() addition to ExerciseController (partial revert — keep existing endpoints)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java

# Revert test files
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/

# If optional composite index migration was applied:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_exercise_sessions_user_status_started;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE script LIKE '%add_exercise_session_history_composite_index%';"

# UC-184 remains OPEN — feature not shipped
```

---

## 8. CASE 2.0 Anti-Pattern Detection

> Checklist for reviewer when these test cases were AI-assisted. Complete before implementation begins.

| AP-ID | Anti-Pattern | Sign in this TDD spec | Check | Gate |
|-------|-------------|----------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any ADR/BR/UC | `[ ]` | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASSES with `throw UnsupportedOperationException` stub (§5.1) | `[ ]` | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `JOIN FETCH` works without `@ManyToOne` on `ExerciseSession` entity — this must be verified | `[ ]` | G-1 |
| AP-AI-004 | Layer Violation | Test asserts that Controller contains business logic (IDOR check, clamping) | `[ ]` | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExerciseSessionRepository` (wrong name) instead of `ExerciseSessionHistoryRepository` | `[ ]` | G-3 |

**Review Result:**

- [ ] No anti-patterns detected → TDD spec approved for implementation
- [ ] Anti-pattern detected → record below and fix before implementation

| AP detected | TC ID | Description | Fix action | Fixed? |
|------------|-------|-------------|------------|--------|
| `AP-AI-003` | `EXH-TC-INT-006` | `JOIN FETCH` requires `ExerciseSession.exercise` field to be a JPA `@ManyToOne` — verify before writing repository query | Create `ExerciseSession` entity with `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="exercise_id") private PregnancyExercise exercise;` | `[ ]` |

---

## Appendix — Suggested Test File Structure

```
src/test/java/com/carebridge/backend/exercise/
├── ExerciseSessionHistoryTestFactory.java          -- Props isolation factory (§4 boilerplate)
├── ExerciseSessionHistoryMapperTest.java           -- EXH-TC-005 through EXH-TC-008
├── ExerciseSessionHistoryServiceTest.java          -- EXH-TC-001 through EXH-TC-004
├── ExerciseControllerSecurityTest.java             -- EXH-TC-SEC-001, SEC-002, SEC-004, INT-008
│   (uses @WebMvcTest + Spring Security test support)
└── ExerciseSessionHistoryIntegrationTest.java      -- EXH-TC-SEC-003, INT-001 through INT-007
    (uses @SpringBootTest + @Testcontainers + PostgreSQLContainer)
```

**Key dependencies needed in `pom.xml` (if not already present):**
```xml
<!-- Testcontainers for integration tests -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

*TDD Spec v1.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol integrated.*
*Status: Draft — awaiting Principal Architect review and approval before implementation begins.*
