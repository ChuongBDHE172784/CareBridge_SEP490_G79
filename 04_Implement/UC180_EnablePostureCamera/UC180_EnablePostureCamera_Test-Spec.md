# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC180 — Enable Posture Camera — Test Specification

**Document ID:** `CB-EXERCISE-TEST-006`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC180_EnablePostureCamera/UC180_EnablePostureCamera_TDS.md` — Technical Design Specification (CB-EXERCISE-IMP-006)
- `04_Implement/UC30_AnalyzeExercisePosture/UC30_AnalyzeExercisePosture_TDS.md` — Related posture analysis UC
- `01_Requirements/SRS.md` — SRS 3.3.2.6 (Function ID 3.3.2.6)

> **TDD Convention:** This document defines test cases BEFORE production code is written.
> Required order: write tests (.java / .dart) → run → confirm FAIL (Red) → implement → PASS (Green) → refactor (Blue).
> Do NOT mark a test green unless `./mvnw test` (backend) or `flutter test` (mobile) is actually passing.
> Use SYNTHETIC test data only — no real user PII in tests.

---

## CHANGELOG

| Date | Author | Change Description |
|------|--------|--------------------|
| 2026-06-28 | AI Agent — Developer | Initial document — TDD spec for UC180 Enable Posture Camera |

---

## TABLE OF CONTENTS

1. [Module Information](#1-module-information)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Information

| Field | Value |
|-------|-------|
| **Feature / UC ID** | `UC180 — Enable Posture Camera` |
| **Module** | `exercise — Posture Camera Enablement` |
| **TDS Reference** | `CB-EXERCISE-IMP-006` |
| **Priority** | Medium |
| **Sprint** | `Current sprint` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `UC29 (exercise selection), UC177 (exercise detail), UC30 (posture analysis), IAM, exercise_sessions table, posture_analysis_configs table` |
| **Downstream Consumers** | `UC30 PostureFeedbackEventController (receives posture events once camera is active)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `TDS CB-EXERCISE-IMP-006 §17`, `ADR-EXERCISE-006-001 through 003` |
| **Constraints Injected** | `C1 (no video to backend), C2 (privacy notice first), C3 (deny non-fatal), C4 (RBAC + session ownership), C5 (supportsPostureAnalysis gate), C6 (dispose camera), C7 (no business logic in controller)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate verification)` |

---

## 2. Logic Issues Resolved

> **Oracle rule:** Use `V1__init_schema.sql` and approved Flyway migrations as the persistence oracle. ERD is supporting evidence only.

| # | Original Spec / Potential Ambiguity | Actual Reality (schema / policy) | Fix Applied in Tests |
|---|-------------------------------------|----------------------------------|----------------------|
| L1 | TDS mentions `userId` field on `exercise_sessions` | V1 schema: `exercise_sessions` does not have an explicit `user_id` column in the provided DDL — session ownership must be inferred via `exercise_id` → linked booking/journey entity. **Resolution:** UC30 TDS clarifies that session ownership check is done via the exercise_id belonging to the mother's journey. Tests must mock repository accordingly. If UC30 adds a `user_id` column to exercise_sessions, tests use that. Until confirmed, tests mock the session ownership lookup. | Tests mock `ExerciseSessionRepository.findBySessionIdAndUserIdAndSessionStatus()` with the session model that includes userId |
| L2 | `posture_analysis_configs` `analysis_mode` column name vs Java field name | V1 schema: `analysis_mode VARCHAR(30)`. Java entity field: `analysisMode` (camelCase). Tests must use the Java field name in assertions. | Tests assert on `PostureConfigResponse.analysisMode`, not `analysis_mode` |
| L3 | What happens when `configJson` is null in `posture_analysis_configs` | `config_json JSONB` is nullable in schema. `PostureConfigResponse.configJson` should be null (not empty map) when DB value is null. | TC-UNIT-006 specifically tests null configJson scenario |
| L4 | Mobile: `CameraController` initialization may fail on emulator / missing camera hardware | Flutter plugin throws `CameraException` if no camera is available (e.g., web platform without media devices) | TC-MOB-004 covers camera init failure; TC-MOB-009 covers web platform graceful degradation |

---

## 3. Test Design Specification

> Oracle rule: Include `V1__init_schema.sql` and approved Flyway migrations in the test basis whenever database schema facts, constraints, or persistence side effects are part of the oracle.

### TDS-01 — Scope

```
UC180 — Enable Posture Camera covers these layers:
├── Backend
│   ├── Domain (entity: PostureAnalysisConfig, ExerciseSession — no business logic)
│   ├── Repository (PostureAnalysisConfigRepository, ExerciseSessionRepository — mock with Mockito)
│   ├── Service (PostureConfigServiceImpl — unit tested with mocked repos)
│   ├── Controller (PostureConfigController — @WebMvcTest with mocked service)
│   └── Integration (Testcontainers PostgreSQL with @SpringBootTest — full round-trip)
│
└── Mobile (Flutter)
    ├── BLoC unit tests (PostureCameraBloc — mocked deps)
    ├── Widget tests (PosturePrivacyNoticeWidget, "Enable Camera" toggle)
    └── Privacy integration test (verify no camera frames reach HTTP layer)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-180 (3.3.2.6)` | Camera permission request flow, privacy notice, deny non-fatal |
| `ADR-EXERCISE-006-001` | Backend config endpoint; config scoped to IN_PROGRESS session |
| `ADR-EXERCISE-006-002` | Permission deny = non-blocking; session continues |
| `ADR-EXERCISE-006-003` | No video data to backend; only keypoints from UC30 |
| `BR-POSTURE-CAM-001` | Camera only enabled if `supports_posture_analysis = true` |
| `BR-POSTURE-CAM-002` | Permission explicitly requested; deny is non-fatal |
| `BR-POSTURE-CAM-003` | Privacy notice before permission dialog |
| `BR-POSTURE-CAM-004` | Camera frames never transmitted to backend |
| `BR-POSTURE-CAM-005` | Session continues without posture analysis if camera denied |
| `BR-POSTURE-CAM-006` | Config endpoint requires active IN_PROGRESS session |
| `BR-POSTURE-CAM-007` | Only MOTHER role can call posture config endpoint |
| `V1__init_schema.sql` | `posture_analysis_configs.analysis_mode, confidence_threshold, feedback_level, status, effective_from, effective_to` |
| `V1__init_schema.sql` | `exercise_sessions.session_status` (IN_PROGRESS, COMPLETED, ABORTED) |
| `V1__init_schema.sql` | `pregnancy_exercises.supports_posture_analysis BOOLEAN NOT NULL DEFAULT false` |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid auth + active session + supported exercise + active config → 200 + config | `PostureConfigServiceImpl.getActiveConfig()` | `PCM-TC-001`, `PCM-TC-INT-001` |
| TC-COND-002 | Exercise `supports_posture_analysis = false` → PCM-003 | `PostureConfigServiceImpl` guard | `PCM-TC-002` |
| TC-COND-003 | Session not found / wrong owner / COMPLETED → PCM-004 | `PostureConfigServiceImpl` session ownership check | `PCM-TC-003`, `PCM-TC-004` |
| TC-COND-004 | No ACTIVE posture config record for exercise → PCM-003 | `PostureAnalysisConfigRepository.findActiveConfigForExercise()` | `PCM-TC-005` |
| TC-COND-005 | No JWT → 401 | Spring Security filter | `PCM-TC-INT-002` |
| TC-COND-006 | ADMIN role → 403 | `@PreAuthorize("hasRole('MOTHER')")` | `PCM-TC-INT-003` |
| TC-COND-007 | Missing sessionId param → 400 PCM-001 | Controller validation | `PCM-TC-CON-001` |
| TC-COND-008 | Camera permission granted → active state | `PostureCameraBloc` happy path | `PCM-TC-MOB-001` |
| TC-COND-009 | Camera permission denied → denied state, session continues | `PostureCameraBloc` deny path | `PCM-TC-MOB-002` |
| TC-COND-010 | Camera permission permanently denied → permanent denied state | `PostureCameraBloc` permanent deny | `PCM-TC-MOB-003` |
| TC-COND-011 | Config fetch network error → error state, fallback | `PostureCameraBloc` error handling | `PCM-TC-MOB-004` |
| TC-COND-012 | Mother declines privacy notice → no permission request | `PosturePrivacyNoticeWidget` | `PCM-TC-MOB-005` |
| TC-COND-013 | Exercise `supportsPostureAnalysis = false` → camera toggle hidden | `ExerciseSessionScreen` widget | `PCM-TC-MOB-006` |
| TC-COND-014 | DisableCameraRequested → CameraController.dispose() called | `PostureCameraBloc` cleanup | `PCM-TC-MOB-007` |
| TC-COND-015 | No camera frames transmitted to backend | Privacy integration test | `PCM-TC-MOB-008` |
| TC-COND-016 | configJson null in DB → null in response | `PostureConfigServiceImpl` mapping | `PCM-TC-006` |
| TC-COND-017 | CameraController init failure (hardware error) | `PostureCameraBloc` error path | `PCM-TC-MOB-009` |
| TC-COND-018 | Config returned is scoped to effective date range | `PostureAnalysisConfigRepository` query | `PCM-TC-007` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `sessionId` (valid owned IN_PROGRESS / valid not-owned / completed / null) | Covers session ownership domain clearly |
| Equivalence Partitioning | Permission result (granted / denied / permanentlyDenied) | Covers all OS permission states |
| Boundary Value Analysis | `effective_from / effective_to` dates | Config at exactly the boundary must be returned / excluded correctly |
| State Transition Testing | `PostureCameraState` FSM (disabled → requesting → granted → active → disabled) | Critical state machine; each transition must be verified |
| Error Guessing | ADMIN role calling MOTHER-only endpoint; sending request without sessionId | RBAC bypass and missing param attacks |
| Privacy Test (Domain-Specific) | Camera frames in HTTP request body | Ensures no video data leaks — specific to healthcare privacy context |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `pregnancy_exercises { exerciseId: ex-001, supports_posture_analysis: true, status: PUBLISHED }` | Happy path exercise |
| `FX-002` | DB seed | `pregnancy_exercises { exerciseId: ex-002, supports_posture_analysis: false, status: PUBLISHED }` | Unsupported exercise |
| `FX-003` | DB seed | `posture_analysis_configs { postureConfigId: cfg-001, exerciseId: ex-001, status: ACTIVE, analysis_mode: RULE_BASED, confidence_threshold: 0.75, feedback_level: STANDARD, effective_from: now()-1h, effective_to: null }` | Active config for happy path |
| `FX-004` | DB seed | `exercise_sessions { exerciseSessionId: sess-001, exerciseId: ex-001, userId: mother-001, session_status: IN_PROGRESS }` | Active session owned by mother-001 |
| `FX-005` | DB seed | `exercise_sessions { exerciseSessionId: sess-002, exerciseId: ex-001, userId: mother-999, session_status: IN_PROGRESS }` | Session owned by different mother |
| `FX-006` | DB seed | `exercise_sessions { exerciseSessionId: sess-003, exerciseId: ex-001, userId: mother-001, session_status: COMPLETED }` | Completed session |
| `FX-007` | JWT | `{ sub: "mother-001", role: "MOTHER" }` | Valid MOTHER JWT |
| `FX-008` | JWT | `{ sub: "admin-001", role: "ADMIN" }` | ADMIN JWT (should be rejected) |
| `FX-009` | DB seed | `posture_analysis_configs { configJson: null }` | Null configJson scenario |
| `FX-010` | DB seed | `posture_analysis_configs { status: INACTIVE }` | Inactive config (should not be returned) |
| `FX-011` | DB seed | `posture_analysis_configs { effective_from: now()+1h }` | Future-effective config (should not be returned) |
| `FX-012` | DB seed | `posture_analysis_configs { effective_to: now()-1m }` | Expired config (should not be returned) |

---

## 4. Test Case Specification

> **TC ID format:** `PCM-TC-[NNN]` (backend unit), `PCM-TC-INT-[NNN]` (backend integration), `PCM-TC-CON-[NNN]` (controller), `PCM-TC-MOB-[NNN]` (mobile)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** Not written (will be 🔴 Red at start)

### Props Isolation Boilerplate (CASE 2.0 — REQUIRED)

> CASE 2.0 Rule: Every test MUST create a fresh instance via factory. No shared mutable state between test cases.

```java
// === BACKEND TEST FACTORY ===
// PostureCameraTestFactory.java
// com.carebridge.backend.exercise.

class PostureCameraTestFactory {

    static final UUID EXERCISE_ID_SUPPORTED =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EXERCISE_ID_UNSUPPORTED =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID CONFIG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID SESSION_ID_ACTIVE =
            UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID SESSION_ID_COMPLETED =
            UUID.fromString("00000000-0000-0000-0000-000000000021");
    static final UUID SESSION_ID_OTHER_MOTHER =
            UUID.fromString("00000000-0000-0000-0000-000000000022");
    static final UUID MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID OTHER_MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    static PregnancyExercise makeSupportedExercise() {
        PregnancyExercise ex = new PregnancyExercise();
        ex.setExerciseId(EXERCISE_ID_SUPPORTED);
        ex.setTitle("Prenatal Yoga — Supported");
        ex.setSupportsPostureAnalysis(true);
        ex.setStatus(ExerciseStatus.PUBLISHED);
        ex.setCreatedAt(OffsetDateTime.now().minusDays(1));
        ex.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        ex.setCreatedBy(UUID.randomUUID());
        return ex;
    }

    static PregnancyExercise makeUnsupportedExercise() {
        PregnancyExercise ex = makeSupportedExercise();
        ex.setExerciseId(EXERCISE_ID_UNSUPPORTED);
        ex.setTitle("Breathing Exercise — Not Supported");
        ex.setSupportsPostureAnalysis(false);
        return ex;
    }

    static PostureAnalysisConfig makeActiveConfig() {
        PostureAnalysisConfig cfg = new PostureAnalysisConfig();
        cfg.setPostureConfigId(CONFIG_ID);
        cfg.setExerciseId(EXERCISE_ID_SUPPORTED);
        cfg.setAnalysisMode("RULE_BASED");
        cfg.setRuleOrModelVersion("v1.2.0-rules");
        cfg.setConfidenceThreshold(new BigDecimal("0.75"));
        cfg.setFeedbackLevel("STANDARD");
        cfg.setConfigJson(null);
        cfg.setEffectiveFrom(OffsetDateTime.now().minusHours(1));
        cfg.setEffectiveTo(null);
        cfg.setStatus("ACTIVE");
        cfg.setCreatedAt(OffsetDateTime.now().minusHours(1));
        cfg.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        return cfg;
    }

    static ExerciseSession makeActiveSession() {
        ExerciseSession session = new ExerciseSession();
        session.setExerciseSessionId(SESSION_ID_ACTIVE);
        session.setExerciseId(EXERCISE_ID_SUPPORTED);
        session.setUserId(MOTHER_ID);
        session.setSessionStatus("IN_PROGRESS");
        return session;
    }

    static ExerciseSession makeCompletedSession() {
        ExerciseSession session = makeActiveSession();
        session.setExerciseSessionId(SESSION_ID_COMPLETED);
        session.setSessionStatus("COMPLETED");
        return session;
    }
}
```

```dart
// === MOBILE TEST FACTORY ===
// posture_camera_test_factory.dart

class PostureCameraTestFactory {
  static const exerciseIdSupported = '00000000-0000-0000-0000-000000000001';
  static const exerciseIdUnsupported = '00000000-0000-0000-0000-000000000002';
  static const sessionId = '00000000-0000-0000-0000-000000000020';

  static PostureConfigModel makeConfig() => PostureConfigModel(
    exerciseId: exerciseIdSupported,
    postureConfigId: '00000000-0000-0000-0000-000000000010',
    analysisMode: 'RULE_BASED',
    ruleOrModelVersion: 'v1.2.0-rules',
    confidenceThreshold: 0.75,
    feedbackLevel: 'STANDARD',
    configJson: null,
  );
}
```

---

### PCM-TC-001 — getActiveConfig: happy path returns config

**Severity:** `CRITICAL`
**Feature Under Test:** `PostureConfigServiceImpl.getActiveConfig(exerciseId, sessionId, motherId)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED — not yet implemented
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-EXERCISE-006-001 §Decision` — config returned for supported exercise with active owned session

**Preconditions:**
- Exercise FX-001 exists with `supportsPostureAnalysis = true`, `status = PUBLISHED`
- Config FX-003 exists with `status = ACTIVE`, `effectiveFrom = now()-1h`, `effectiveTo = null`
- Session FX-004 exists with `sessionStatus = IN_PROGRESS`, `userId = mother-001`

**Test Steps:**
1. Arrange: mock `ExerciseRepository.findById(EXERCISE_ID_SUPPORTED)` → `makeSupportedExercise()`
2. Arrange: mock `ExerciseSessionRepository.findByExerciseSessionIdAndUserIdAndSessionStatus(SESSION_ID_ACTIVE, MOTHER_ID, "IN_PROGRESS")` → `Optional.of(makeActiveSession())`
3. Arrange: mock `PostureAnalysisConfigRepository.findActiveConfigForExercise(EXERCISE_ID_SUPPORTED, any(OffsetDateTime.class))` → `Optional.of(makeActiveConfig())`
4. Act: `postureConfigService.getActiveConfig(EXERCISE_ID_SUPPORTED, SESSION_ID_ACTIVE, MOTHER_ID)`
5. Assert: response is not null
6. Assert: `response.getExerciseId()` equals `EXERCISE_ID_SUPPORTED`
7. Assert: `response.getPostureConfigId()` equals `CONFIG_ID`
8. Assert: `response.getAnalysisMode()` equals `"RULE_BASED"`
9. Assert: `response.getConfidenceThreshold()` equals `0.75`
10. Assert: `response.getFeedbackLevel()` equals `"STANDARD"`
11. Assert: `response.getConfigJson()` is null

**Expected Result (PASS):**
- `PostureConfigResponse` returned with all fields populated from FX-003.

**Expected Result (FAIL — sign of wrong implementation):**
- Method returns null, or throws unexpectedly, or returns wrong analysisMode.

**Current Status:** 🔴 Not written

---

### PCM-TC-002 — getActiveConfig: exercise does not support posture analysis → PCM-003

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigServiceImpl.getActiveConfig()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-POSTURE-CAM-001` — config must not be returned for unsupported exercises

**Preconditions:**
- Exercise FX-002: `supportsPostureAnalysis = false`

**Test Steps:**
1. Arrange: mock `ExerciseRepository.findById(EXERCISE_ID_UNSUPPORTED)` → `makeUnsupportedExercise()`
2. Act + Assert: `assertThrows(ResourceNotFoundException.class, () -> service.getActiveConfig(EXERCISE_ID_UNSUPPORTED, SESSION_ID_ACTIVE, MOTHER_ID))`
3. Assert: exception message contains `"PCM-003"` or error code matches

**Expected Result (PASS):**
- `ResourceNotFoundException` thrown with code `PCM-003`.

**Expected Result (FAIL):**
- Method returns a config or throws wrong exception.

**Current Status:** 🔴 Not written

---

### PCM-TC-003 — getActiveConfig: session belongs to different mother → PCM-004

**Severity:** `CRITICAL`
**Feature Under Test:** `PostureConfigServiceImpl.getActiveConfig()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-POSTURE-CAM-006` + `BR-RBAC` — session ownership enforced

**Preconditions:**
- Session FX-005: `exerciseSessionId = SESSION_ID_OTHER_MOTHER`, `userId = OTHER_MOTHER_ID`, `sessionStatus = IN_PROGRESS`

**Test Steps:**
1. Arrange: mock `ExerciseRepository` → `makeSupportedExercise()`
2. Arrange: mock `ExerciseSessionRepository.findByExerciseSessionIdAndUserIdAndSessionStatus(SESSION_ID_OTHER_MOTHER, MOTHER_ID, "IN_PROGRESS")` → `Optional.empty()`
3. Act + Assert: `assertThrows(ResourceNotFoundException.class, () -> service.getActiveConfig(EXERCISE_ID_SUPPORTED, SESSION_ID_OTHER_MOTHER, MOTHER_ID))`
4. Assert: exception code is `PCM-004`

**Expected Result (PASS):**
- `ResourceNotFoundException` with code `PCM-004` — session not found for this mother.

**Expected Result (FAIL):**
- Method returns config for another mother's session (RBAC violation).

**Current Status:** 🔴 Not written

---

### PCM-TC-004 — getActiveConfig: session is COMPLETED → PCM-004

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigServiceImpl.getActiveConfig()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-POSTURE-CAM-006` — config only returned for IN_PROGRESS sessions

**Test Steps:**
1. Arrange: mock exercise → `makeSupportedExercise()`
2. Arrange: mock session repo for `(SESSION_ID_COMPLETED, MOTHER_ID, "IN_PROGRESS")` → `Optional.empty()`
3. Act + Assert: `assertThrows(ResourceNotFoundException.class, ...)`
4. Assert: exception code is `PCM-004`

**Expected Result (PASS):**
- Exception with PCM-004.

**Current Status:** 🔴 Not written

---

### PCM-TC-005 — getActiveConfig: no ACTIVE config in DB → PCM-003

**Severity:** `HIGH`
**Feature Under Test:** `PostureAnalysisConfigRepository.findActiveConfigForExercise()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-POSTURE-CAM-001` — exercise supports posture but no current config configured

**Test Steps:**
1. Arrange: exercise → `makeSupportedExercise()`
2. Arrange: session → `makeActiveSession()`
3. Arrange: config repo `findActiveConfigForExercise(EXERCISE_ID_SUPPORTED, any)` → `Optional.empty()`
4. Act + Assert: `assertThrows(ResourceNotFoundException.class, ...)`
5. Assert: code is `PCM-003`

**Expected Result (PASS):**
- `ResourceNotFoundException(PCM-003)` when no active config exists.

**Current Status:** 🔴 Not written

---

### PCM-TC-006 — getActiveConfig: configJson null in DB → null in response (not empty map)

**Severity:** `MEDIUM`
**Feature Under Test:** `PostureConfigServiceImpl` mapper
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `L3 (Logic Issues Resolved §2)` — null JSONB maps to null DTO field

**Test Steps:**
1. Arrange: config `makeActiveConfig()` with `configJson = null`
2. Arrange: all other mocks return valid data (FX-001, FX-004, FX-003 but configJson=null)
3. Act: call `getActiveConfig(...)`
4. Assert: `response.getConfigJson() == null` (not empty map, not NPE)

**Expected Result (PASS):**
- `getConfigJson()` returns null.

**Current Status:** 🔴 Not written

---

### PCM-TC-007 — Repository: expired config not returned (effective_to in past)

**Severity:** `HIGH`
**Feature Under Test:** `PostureAnalysisConfigRepository.findActiveConfigForExercise()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureAnalysisConfigRepositoryTest.java` (or integration test)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `V1__init_schema.sql` — `effective_to` field semantics

**Preconditions (using Testcontainers or @DataJpaTest):**
- Config FX-012: `effective_to = now() - 1 minute`, `status = ACTIVE`
- Config FX-011: `effective_from = now() + 1 hour`, `status = ACTIVE`

**Test Steps:**
1. Seed FX-012 (expired) and FX-011 (future) into test DB
2. Call `findActiveConfigForExercise(exerciseId, now())`
3. Assert: `Optional.isEmpty()` — neither expired nor future config is returned

**Expected Result (PASS):**
- Empty Optional returned for both expired and future configs.

**Current Status:** 🔴 Not written

---

### PCM-TC-CON-001 — Controller: missing sessionId → 400 PCM-001

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigController GET /api/v1/exercises/{id}/posture-config`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §10 PCM-001`

**Test Steps (@WebMvcTest):**
1. Arrange: mock service, valid MOTHER JWT
2. Act: `GET /api/v1/exercises/{exerciseId}/posture-config` (no sessionId param)
3. Assert: response status 400
4. Assert: response body contains `"code": "PCM-001"` or Spring validation error

**Expected Result (PASS):**
- 400 Bad Request, PCM-001.

**Current Status:** 🔴 Not written

---

### PCM-TC-CON-002 — Controller: no business logic in controller layer

**Severity:** `MEDIUM`
**Feature Under Test:** `PostureConfigController`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `C7 (CASE 2.0 constraint)`
**Oracle Source:** `CLAUDE.md Architecture rules — controller: validation + request/response mapping only`

**Test Steps:**
1. Arrange: mock `IPostureConfigService.getActiveConfig()` → `PostureConfigResponse`
2. Act: call controller endpoint
3. Assert: controller calls `service.getActiveConfig()` exactly once
4. Assert: no conditional logic, no DB calls, no business decisions in controller class
5. Verify: controller is annotated with `@RestController` only (no `@Transactional` or repository injection)

**Expected Result (PASS):**
- Controller delegates entirely to service.

**Current Status:** 🔴 Not written

---

### PCM-TC-INT-001 — Integration: authenticated MOTHER retrieves posture config

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: JWT → Controller → Service → Repository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`

**Preconditions:**
- `@Testcontainers` with `PostgreSqlContainer` auto-start
- Flyway migrations applied (includes `posture_analysis_configs`, `exercise_sessions`, `pregnancy_exercises` tables from V1)
- Seed: FX-001, FX-003, FX-004 inserted via `@Sql` or JPA repository in `@BeforeEach`
- JWT generated with `sub=mother-001, role=MOTHER`

**Test Steps:**
1. Seed exercise, session, config via test setup
2. `GET /api/v1/exercises/{exerciseId}/posture-config?sessionId={sessionId}` with JWT
3. Assert status 200
4. Assert `data.analysisMode = "RULE_BASED"`
5. Assert `data.postureConfigId` matches seeded config ID
6. Assert `data.confidenceThreshold = 0.75`

**DB Assertion:**
```java
PostureAnalysisConfig fromDb = configRepo.findById(CONFIG_ID).orElseThrow();
assertThat(fromDb.getStatus()).isEqualTo("ACTIVE");
assertThat(fromDb.getAnalysisMode()).isEqualTo("RULE_BASED");
```

**Current Status:** 🔴 Not written

---

### PCM-TC-INT-002 — Integration: no JWT → 401

**Severity:** `CRITICAL`
**Feature Under Test:** Spring Security authentication filter
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. `GET /api/v1/exercises/{exerciseId}/posture-config?sessionId={sessionId}` — no Authorization header
2. Assert status 401

**Current Status:** 🔴 Not written

---

### PCM-TC-INT-003 — Integration: ADMIN role → 403

**Severity:** `HIGH`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on endpoint
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-POSTURE-CAM-007`, `§16 Authorization Matrix`

**Test Steps:**
1. JWT with `role = ADMIN` (FX-008)
2. `GET /api/v1/exercises/{exerciseId}/posture-config?sessionId={sessionId}` with ADMIN JWT
3. Assert status 403

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### PCM-TC-SEC-001 — Session ownership: cannot access other mother's session config

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-284 — Improper Access Control`
**Legal:** `BR-RBAC — MOTHER may only access own session resources`
**Feature Under Test:** `PostureConfigServiceImpl` ownership check
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Session `sess-005` belongs to `mother-999`
- Authenticated caller is `mother-001`

**Test Steps (Attack Simulation):**
1. MOTHER-001 calls `getActiveConfig(exerciseId, sess-005, MOTHER_ID)`
2. Assert: `ResourceNotFoundException` with PCM-004 thrown
3. Assert: service NEVER calls `configRepository.findActiveConfigForExercise()` for cross-mother access

**Expected Result (PASS = system is secure):**
- `ResourceNotFoundException(PCM-004)` raised before config is queried.

**Expected Result (FAIL = vulnerability exists):**
- Config from another mother's session is returned to the attacker.

**Current Status:** 🔴 Not written

---

### PCM-TC-SEC-002 — RBAC: EXPERT role cannot access posture config endpoint

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-284`
**Feature Under Test:** `@PreAuthorize` on controller endpoint
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. JWT with `role = EXPERT`
2. `GET /posture-config` endpoint call
3. Assert: 403 Forbidden

**Expected Result (PASS):** 403 returned.
**Expected Result (FAIL):** Config data returned to EXPERT.

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES

---

### PCM-TC-MOB-001 — PostureCameraBloc: permission granted → active state

**Severity:** `CRITICAL`
**Feature Under Test:** `PostureCameraBloc` — full happy path
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-EXERCISE-006-001`, `ADR-EXERCISE-006-002 §Decision`

**Preconditions:**
- Mock `PermissionHandler` returns `PermissionStatus.granted`
- Mock `PostureConfigRepository` returns `PostureCameraTestFactory.makeConfig()`
- Mock `CameraController` initializes without error

**Test Steps:**
1. Create `PostureCameraBloc` with mocks
2. `bloc.add(EnableCameraRequested())`
3. `await bloc.stream.take(4).toList()` (wait for 4 state transitions)
4. Assert states in order: `requestingPermission`, `fetchingConfig`, `initializing`, `active`
5. Assert final state is `PostureCameraActive` with non-null `controller` and `config`
6. Assert `config.analysisMode == 'RULE_BASED'`

**Expected Result (PASS):**
- Bloc transitions through all states correctly; final state is `active`.

**Expected Result (FAIL):**
- Wrong order of states, or stuck in intermediate state, or config not set.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-002 — PostureCameraBloc: permission denied → denied (non-fatal)

**Severity:** `CRITICAL`
**Feature Under Test:** `PostureCameraBloc` — deny path
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-EXERCISE-006-002` — deny is NON-FATAL

**Test Steps:**
1. Mock `PermissionHandler` returns `PermissionStatus.denied`
2. `bloc.add(EnableCameraRequested())`
3. Assert states: `requestingPermission`, `permissionDenied`
4. Assert final state is `PostureCameraPermissionDenied(isPermanent: false)`
5. Assert mock `CameraController.initialize()` was NEVER called
6. Assert mock `PostureConfigRepository.fetchConfig()` was NEVER called

**Expected Result (PASS):**
- State is `permissionDenied`. Camera never initialized. Config never fetched.

**Expected Result (FAIL):**
- Exception thrown (session disrupted), or camera initialized despite denial.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-003 — PostureCameraBloc: permanently denied → permanent denied state

**Severity:** `HIGH`
**Feature Under Test:** `PostureCameraBloc`
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. Mock `PermissionHandler` returns `PermissionStatus.permanentlyDenied`
2. Add `EnableCameraRequested`
3. Assert final state is `PostureCameraPermissionDenied(isPermanent: true)`

**Expected Result (PASS):**
- `isPermanent: true` in denied state.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-004 — PostureCameraBloc: config fetch network error → error state

**Severity:** `HIGH`
**Feature Under Test:** `PostureCameraBloc` error handling
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. Mock `PermissionHandler` returns `granted`
2. Mock `PostureConfigRepository` throws `NetworkException`
3. Add `EnableCameraRequested`
4. Assert states include `fetchingConfig` then `error`
5. Assert final state is `PostureCameraError(errorCode: 'PCM-M-003')`
6. Assert `CameraController.initialize()` was NEVER called

**Expected Result (PASS):**
- Error state with `PCM-M-003`. Camera not initialized.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-005 — PosturePrivacyNoticeWidget: onDeclined → no permission request

**Severity:** `CRITICAL`
**Feature Under Test:** `PosturePrivacyNoticeWidget` + `PostureCameraBloc`
**Test File:** `test/exercise/posture_camera/posture_privacy_notice_widget_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `BR-POSTURE-CAM-003` — privacy notice must be displayed; `BR-PRIVACY` — no coercion

**Test Steps:**
1. Pump `PosturePrivacyNoticeWidget` with `onAccepted: ...` and `onDeclined: ...` callbacks
2. Find and tap "Decline" button
3. Assert: `onDeclined` callback was called once
4. Assert: `onAccepted` callback was NOT called
5. Assert: `PermissionHandler.request` was NEVER called
6. Assert: `PostureCameraBloc` state remains `disabled`

**Expected Result (PASS):**
- User can decline; no permission requested.

**Expected Result (FAIL):**
- Permission dialog shown despite decline.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-006 — ExerciseSessionScreen: camera toggle hidden when not supported

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseSessionScreen` widget conditional rendering
**Test File:** `test/exercise/exercise_session_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `BR-POSTURE-CAM-001`, `C5 (CASE 2.0)`

**Test Steps:**
1. Create `ExerciseDetailModel(supportsPostureAnalysis: false)`
2. Pump `ExerciseSessionScreen(exercise: unsupportedExercise)`
3. Assert: widget with key `Key('enableCameraButton')` or text "Enable Posture Camera" is NOT in widget tree
4. `expect(find.text('Enable Posture Camera'), findsNothing)`

**Expected Result (PASS):**
- Camera toggle completely absent from widget tree.

**Expected Result (FAIL):**
- Camera button visible for unsupported exercise.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-007 — CameraController disposed on DisableCameraRequested

**Severity:** `CRITICAL`
**Feature Under Test:** `PostureCameraBloc` cleanup
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `C6 (CASE 2.0)` — resource leak prevention

**Preconditions:**
- `PostureCameraBloc` is in `active` state (previously granted + initialized)

**Test Steps:**
1. Drive bloc to `active` state (via permission grant + config fetch + camera init)
2. `bloc.add(DisableCameraRequested())`
3. Await state transition
4. Assert: `mockCameraController.dispose()` called exactly once
5. Assert: final state is `PostureCameraDisabled`

**Expected Result (PASS):**
- `dispose()` called. Bloc in disabled state.

**Expected Result (FAIL):**
- `dispose()` not called (camera resource leak).

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-008 — Privacy: no camera frames sent to HTTP layer

**Severity:** `CRITICAL`
**OWASP:** `A02:2021 — Cryptographic Failures` (data leakage)
**CWE:** `CWE-311 — Missing Encryption of Sensitive Data` / Privacy violation
**Feature Under Test:** `PostureCameraBloc` + camera image stream
**Test File:** `test/exercise/posture_camera/posture_camera_privacy_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `ADR-EXERCISE-006-003` — all video stays on-device

**Test Steps:**
1. Mock HTTP client (intercept all network calls) — record all request bodies
2. Drive `PostureCameraBloc` to `active` state
3. Simulate 100 `CameraImage` frames from mock camera stream
4. Assert: mock HTTP client recorded ZERO requests with:
   - `Content-Type: multipart/form-data`
   - `Content-Type: video/*`
   - body length > 50KB (heuristic for raw frame data)
5. Assert: the only outbound HTTP calls are the config fetch (GET /posture-config, small JSON) and any UC30 keypoint events

**Expected Result (PASS = privacy is maintained):**
- No large binary payloads in HTTP calls. Only small JSON.

**Expected Result (FAIL = privacy violation):**
- Raw camera frames or video streams detected in outbound HTTP traffic.

**Current Status:** 🔴 Not written

---

### PCM-TC-MOB-009 — CameraController init fails (hardware error) → error state

**Severity:** `HIGH`
**Feature Under Test:** `PostureCameraBloc` hardware failure handling
**Test File:** `test/exercise/posture_camera/posture_camera_bloc_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Mock `PermissionHandler` → `granted`
2. Mock `PostureConfigRepository` → `makeConfig()`
3. Mock `CameraController.initialize()` → throws `CameraException('Camera hardware unavailable', 'cameraHardwareUnavailable')`
4. Add `EnableCameraRequested`
5. Assert final state is `PostureCameraError(errorCode: 'PCM-M-001')`

**Expected Result (PASS):**
- Error state with code `PCM-M-001`. No crash.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit hash) | 🔵 REFACTOR note |
|-------|-----------|-----------------|------------------------|-----------------|
| `PCM-TC-001` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-002` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-003` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-004` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-005` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-006` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-007` | `PostureAnalysisConfigRepositoryTest.java` | `[ ]` | `—` | — |
| `PCM-TC-CON-001` | `PostureConfigControllerTest.java` | `[ ]` | `—` | — |
| `PCM-TC-CON-002` | `PostureConfigControllerTest.java` | `[ ]` | `—` | — |
| `PCM-TC-INT-001` | `PostureConfigIntegrationTest.java` | `[ ]` | `—` | — |
| `PCM-TC-INT-002` | `PostureConfigIntegrationTest.java` | `[ ]` | `—` | — |
| `PCM-TC-INT-003` | `PostureConfigIntegrationTest.java` | `[ ]` | `—` | — |
| `PCM-TC-SEC-001` | `PostureConfigServiceTest.java` | `[ ]` | `—` | — |
| `PCM-TC-SEC-002` | `PostureConfigIntegrationTest.java` | `[ ]` | `—` | — |
| `PCM-TC-MOB-001` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-002` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-003` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-004` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-005` | `posture_privacy_notice_widget_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-006` | `exercise_session_screen_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-007` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-008` | `posture_camera_privacy_test.dart` | `[ ]` | `—` | — |
| `PCM-TC-MOB-009` | `posture_camera_bloc_test.dart` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ **CASE 2.0 Critical Gate.** Before implementing, run all tests with empty/throw stubs.
> ALL tests MUST FAIL. If any PASS → AP-AI-002 detected → rewrite.

**Backend Stub for Red Phase:**

```java
// PostureConfigServiceImpl.java — Red Phase stub
@Service
public class PostureConfigServiceImpl implements IPostureConfigService {

    @Override
    public PostureConfigResponse getActiveConfig(UUID exerciseId, UUID sessionId, UUID motherId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub [PCM]");
    }
}
```

**Mobile Stub for Red Phase:**

```dart
// posture_camera_bloc.dart — Red Phase stub
class PostureCameraBloc extends Bloc<PostureCameraEvent, PostureCameraState> {
  PostureCameraBloc() : super(const PostureCameraDisabled()) {
    on<EnableCameraRequested>((event, emit) {
      throw UnimplementedError('Not implemented — Red Phase stub [PCM]');
    });
    on<DisableCameraRequested>((event, emit) {
      throw UnimplementedError('Not implemented — Red Phase stub [PCM]');
    });
  }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (if PASS unexpectedly) |
|-------|-------------|----------|--------|-----------------------------------|
| `PCM-TC-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `PCM-TC-002` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PCM-TC-003` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PCM-TC-MOB-001` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PCM-TC-MOB-002` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PCM-TC-MOB-007` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PCM-TC-MOB-008` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- All FAIL? ☐ Yes → **GATE-2 PASS** (T2 → T3) → proceed to implementation
- Log file: `logs/red-gate-uc180-evidence.log`

> **If any test PASSES with the stub:** Stop immediately. Identify root cause from the table above. Rewrite the test using `PostureCameraTestFactory` Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Start Conditions)

- [ ] TDS `CB-EXERCISE-IMP-006` reviewed and approved by Tech Lead
- [ ] Logic Issues (Section 2) confirmed with Principal Architect
- [ ] UC30 tables (`posture_analysis_configs`, `exercise_sessions`) confirmed to exist in staging DB (created by UC30 Flyway migration)
- [ ] Test fixtures (Section 3 TDS-05) prepared
- [ ] Flutter packages `permission_handler` and `camera` added to `pubspec.yaml`
- [ ] Android/iOS permission manifest entries reviewed by DPO
- [ ] Privacy notice copy approved by DPO

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — all backend unit tests green (no skips)
- [ ] `./mvnw verify` — all integration tests green (Testcontainers running)
- [ ] `flutter test` — all mobile widget tests green (no skips)
- [ ] Backend test coverage ≥ 80% lines on `PostureConfigServiceImpl`
- [ ] No business logic in `PostureConfigController` (only validation + service call + response mapping)
- [ ] No camera frames in outbound HTTP traffic (PCM-TC-MOB-008 passing)
- [ ] `PosturePrivacyNoticeWidget` displayed before permission dialog (PCM-TC-MOB-005 passing)
- [ ] Camera denial is non-fatal — session continues (PCM-TC-MOB-002 passing)
- [ ] Camera toggle hidden for unsupported exercises (PCM-TC-MOB-006 passing)
- [ ] `CameraController.dispose()` called on exit (PCM-TC-MOB-007 passing)
- [ ] RBAC enforced: MOTHER only, session ownership validated (PCM-TC-SEC-001 + PCM-TC-INT-003 passing)

**Exit Criteria — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — all injected classes exist in codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — no shared mutable state between tests:
  ```bash
  # All instances created via PostureCameraTestFactory — not shared across @Test methods
  ```
- [ ] **Oracle Source** — all `assert` expected values trace to BR / ADR / schema (documented in each TC above)

### Suspension Criteria (Pause Conditions)

- UC30 Flyway migration not yet deployed (tables missing — integration tests cannot run)
- `permission_handler` or `camera` plugin compatibility issue with Flutter version
- DPO has not approved privacy notice copy
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No database migration for UC180 — code rollback only

# Backend: revert implementation files
git checkout -- src/main/java/com/carebridge/backend/exercise/service/PostureConfigServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java
git checkout -- src/main/java/com/carebridge/backend/exercise/dto/PostureConfigResponse.java
git checkout -- src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/exercise/PostureConfigIntegrationTest.java

# Mobile: revert implementation files
git checkout -- lib/features/exercise/posture_camera/
git checkout -- test/exercise/posture_camera/

# UC180 remains OPEN in backlog — mark as reverted
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Sign in TDD Spec | Check | Gate |
|-------|-------------|-----------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC does not reference any ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASSES with empty/throw stub (§5.1 Red Gate) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes architecture decision not in any ADR (e.g., video streaming to backend) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic (ownership check in controller) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `CameraStreamService` or `BiometricUploadService` (does not exist) | ☐ | G-3 |

**Review Result:**

- [ ] No anti-patterns detected → Test-Spec approved for implementation
- [ ] Anti-pattern detected → record below, fix before implementing

| AP Detected | TC ID | Description | Fix Action | Fixed? |
|------------|-------|-------------|------------|--------|
| `—` | `—` | — | — | ☐ |

---

*TDD Test-Spec v2.0 — Integrated CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Sections marked ⭐ are CASE 2.0 additions.*
*Status: Draft — awaiting Tech Lead review and DPO sign-off on privacy notice copy.*
