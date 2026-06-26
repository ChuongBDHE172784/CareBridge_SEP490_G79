# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC29 — View and Select Pregnancy Exercise — Test Specification

**Document ID:** `CB-EXERCISE-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Developer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC29_ViewAndSelectPregnancyExercise/UC29_ViewAndSelectPregnancyExercise_TDS.md` (CB-EXERCISE-IMP-001 v1.0) — Technical Design Specification
- `01_Requirements/SRS.md` — SRS 3.3.2.1 Functional requirements
- `08_References/Template/PHASE-4_Test-Spec.md` — Test-Spec Template

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là PASS nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Developer | Khởi tạo tài liệu — TDD spec cho UC29 View and Select Pregnancy Exercise |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC29` |
| **Module** | `View and Select Pregnancy Exercise — exercise` |
| **Spec gốc** | `CB-EXERCISE-IMP-001` |
| **Priority** | 🟠 P1 |
| **Sprint** | `Current Sprint (2026-06-26)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `IAM (JWT authentication), pregnancy_exercises table` |
| **Downstream Consumers** | `UC30 — Analyze Exercise Posture` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-001 S17, ADR-EXERCISE-001/002/003` |
| **Constraints Injected** | C1 (PUBLISHED only), C2 (safety_warning always present), C3 (guidance only), C4 (MOTHER role via JWT), C5 (no business logic in controller), C6 (PaginatedResponse), C7 (optional filters) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec does not explicitly state DRAFT/ARCHIVED filtering | DB column `status` has values DRAFT, PUBLISHED, ARCHIVED. Only PUBLISHED must be visible. | Tests assert DRAFT and ARCHIVED exercises are NEVER returned by list or detail endpoints. Test seeds mixed-status data and verifies filtering. |
| L2 | Trimester filter described as "based on Mother's journey week" | Trimester filter is an optional query param sent by client (ADR-EXERCISE-002). Server does NOT auto-detect from journey. | Tests verify: omitting trimester param returns ALL PUBLISHED exercises. Passing trimester param filters correctly. |
| L3 | safety_warning field nullable in DB schema | BR-EXERCISE-003 requires safety_warning always present in API response, never null | Tests verify: when DB safety_warning is null, API response maps it to empty string `""`. Field is NEVER null in response DTO. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC29 View and Select Pregnancy Exercise bao gồm các layer:
├── Service (mock JPA Repository với Mockito)
│   └── ExerciseQueryService — list + detail logic
├── Controller (mock Service với @WebMvcTest)
│   └── ExerciseController — REST endpoints, validation
├── Mapper (unit test, no mocking)
│   └── ExerciseMapper — entity → DTO conversion
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
    └── Full flow: HTTP → Controller → Service → Repository → DB
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.1` | Mother views list of suitable exercises, selects one for detail view |
| `ADR-EXERCISE-001` | Paginated list with filter, no full-text search |
| `ADR-EXERCISE-002` | Trimester filter is optional client-side param |
| `ADR-EXERCISE-003` | Safety check required before session start (not in UC29 scope) |
| `BR-EXERCISE-001` | Only PUBLISHED exercises visible |
| `BR-EXERCISE-002` | Filter by trimester scope |
| `BR-EXERCISE-003` | safety_warning always shown prominently |
| `BR-EXERCISE-004` | No auto-start without safety check |
| `BR-RBAC` | MOTHER role required, JWT authentication |
| `CB-EXERCISE-IMP-001 S8` | Interface specification — service and repository contracts |
| `CB-EXERCISE-IMP-001 S10` | Error codes EX-001, EX-002 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | List exercises returns only PUBLISHED items | `ExerciseQueryService.listPublishedExercises()` | `EX-TC-029-001`, `EX-TC-029-004` |
| TC-COND-002 | Trimester filter works correctly | `ExerciseRepository.findByStatusAndFilters()` | `EX-TC-029-001` |
| TC-COND-003 | Difficulty filter works correctly | `ExerciseRepository.findByStatusAndFilters()` | `EX-TC-029-002` |
| TC-COND-004 | Exercise detail returns full content | `ExerciseQueryService.getExerciseDetail()` | `EX-TC-029-003` |
| TC-COND-005 | Non-existent exercise returns 404 | `ExerciseQueryService.getExerciseDetail()` | `EX-TC-029-005` |
| TC-COND-006 | Authentication required | `ExerciseController` | `EX-TC-029-006` |
| TC-COND-007 | safety_warning never null in response | `ExerciseMapper` | `EX-TC-029-001`, `EX-TC-029-003` |
| TC-COND-008 | DRAFT exercises filtered from list | `ExerciseQueryService.listPublishedExercises()` | `EX-TC-029-004`, `EX-TC-029-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Trimester filter values (FIRST/SECOND/THIRD/ALL/null) | Verify each valid partition and null (no filter) |
| Equivalence Partitioning | Difficulty filter values (EASY/MEDIUM/HARD/null) | Verify each valid partition and null |
| Boundary Value Analysis | Pagination (page=0, size=1, size=50, size=51) | Verify boundary of page size limits |
| State Transition Testing | Exercise status (DRAFT/PUBLISHED/ARCHIVED) | Verify only PUBLISHED state is visible |
| Error Guessing | Invalid UUID, missing JWT, expired JWT | Common API error scenarios |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-029-001` | DB seed | `{ exerciseId: UUID-1, title: "Prenatal Yoga T1", trimesterScope: FIRST, difficultyLevel: EASY, status: PUBLISHED, safetyWarning: "Stop if dizzy" }` | Happy path — published exercise with safety warning |
| `FX-029-002` | DB seed | `{ exerciseId: UUID-2, title: "Strength Training T2", trimesterScope: SECOND, difficultyLevel: MEDIUM, status: PUBLISHED, safetyWarning: "" }` | Published exercise with empty safety warning |
| `FX-029-003` | DB seed | `{ exerciseId: UUID-3, title: "Draft Exercise", status: DRAFT, safetyWarning: null }` | DRAFT exercise — must NOT appear in API response |
| `FX-029-004` | DB seed | `{ exerciseId: UUID-4, title: "Archived Exercise", status: ARCHIVED }` | ARCHIVED exercise — must NOT appear |
| `FX-029-005` | JWT | `{ sub: "mother-001", role: "MOTHER" }` | Valid MOTHER auth token |
| `FX-029-006` | JWT | `(none)` | Missing JWT — 401 scenario |

---

## 4. Test Case Specification

> **TC ID format:** `EX-TC-029-NNN`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeExercise()
// ═══════════════════════════════════════════════════════════

// ExerciseTestFactory.java
class ExerciseTestFactory {

    static final UUID EXERCISE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EXERCISE_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-100000000001");

    // Giá trị baseline hợp lệ — PUBLISHED exercise
    static PregnancyExercise makePublishedExercise() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(EXERCISE_ID_1);
        e.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-200000000001"));
        e.setTitle("Prenatal Yoga - First Trimester");
        e.setDescription("Gentle yoga poses suitable for early pregnancy");
        e.setTrimesterScope(TrimesterScope.FIRST);
        e.setDifficultyLevel(DifficultyLevel.EASY);
        e.setDurationMinutes((short) 20);
        e.setInstructionContent("Step 1: Start in a comfortable seated position...");
        e.setMediaUrl("https://cdn.carebridge.com/exercises/prenatal-yoga-t1.mp4");
        e.setSafetyWarning("Stop immediately if you feel dizzy or experience pain.");
        e.setSupportsPostureAnalysis(true);
        e.setStatus(ExerciseStatus.PUBLISHED);
        e.setVersionNo(1);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    // Overload để override specific fields
    static PregnancyExercise makePublishedExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise e = makePublishedExercise();
        overrides.accept(e);
        return e;
    }

    // DRAFT exercise — should never appear in UC29 results
    static PregnancyExercise makeDraftExercise() {
        return makePublishedExercise(e -> {
            e.setExerciseId(EXERCISE_ID_3);
            e.setTitle("Draft Exercise - Not Visible");
            e.setStatus(ExerciseStatus.DRAFT);
        });
    }
}
```

---

### EX-TC-029-001 — Happy path: List exercises filtered by trimester

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseController.listExercises() → ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001, TC-COND-002, TC-COND-007`
**Oracle Source:** `BR-EXERCISE-001` (only PUBLISHED), `BR-EXERCISE-002` (trimester filter), `BR-EXERCISE-003` (safety_warning present)

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001, FX-029-002 as mock return data

**Test Steps:**
1. Arrange: Mock `exerciseRepository.findByStatusAndFilters(PUBLISHED, FIRST, null, PageRequest(0,20))` to return Page with FX-029-001
2. Act: Call `exerciseQueryService.listPublishedExercises(FIRST, null, 0, 20)`
3. Assert: Result contains 1 item with trimesterScope=FIRST, safetyWarning is not null and not empty

**Expected Result (PASS):**
- PaginatedResponse with 1 item
- Item has `exerciseId` matching FX-029-001
- Item has `trimesterScope = "FIRST"`
- Item has `safetyWarning = "Stop immediately if you feel dizzy or experience pain."`
- Item has `supportsPostureAnalysis = true`

**Expected Result (FAIL):**
- Service throws exception (method not implemented)
- Response is null or empty
- safetyWarning is null

**Current Status:** 🔴 Not written
**Implementation Note:** Ensure repository query uses `status = PUBLISHED` filter. Mapper must never set safetyWarning to null.

---

### EX-TC-029-002 — Filter by difficulty=EASY returns only easy exercises

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-EXERCISE-001`, `ADR-EXERCISE-001` (filter by difficulty)

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001 (EASY), FX-029-002 (MEDIUM)

**Test Steps:**
1. Arrange: Mock `findByStatusAndFilters(PUBLISHED, null, EASY, PageRequest(0,20))` to return Page with FX-029-001 only
2. Act: Call `exerciseQueryService.listPublishedExercises(null, EASY, 0, 20)`
3. Assert: Result contains 1 item, difficultyLevel=EASY

**Expected Result (PASS):**
- PaginatedResponse with 1 item
- Item has `difficultyLevel = "EASY"`

**Expected Result (FAIL):**
- Returns items with non-EASY difficulty
- Service throws exception

**Current Status:** 🔴 Not written

---

### EX-TC-029-003 — Exercise detail by ID returns full content

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseQueryService.getExerciseDetail()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004, TC-COND-007`
**Oracle Source:** `US-EXERCISE-002` (detail view includes instruction_content), `BR-EXERCISE-003` (safety_warning present)

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001

**Test Steps:**
1. Arrange: Mock `findByExerciseIdAndStatus(EXERCISE_ID_1, PUBLISHED)` to return Optional.of(FX-029-001)
2. Act: Call `exerciseQueryService.getExerciseDetail(EXERCISE_ID_1)`
3. Assert: Response contains full exercise detail including instructionContent and safetyWarning

**Expected Result (PASS):**
- ApiResponse with data containing `instructionContent = "Step 1: Start in a comfortable seated position..."`
- `safetyWarning` is populated, not null
- `versionNo` is present
- `createdAt` is present

**Expected Result (FAIL):**
- instructionContent is null or missing
- safetyWarning is null
- Service throws exception

**Current Status:** 🔴 Not written
**Implementation Note:** Detail response MUST include `instructionContent` which is NOT in the summary DTO.

---

### EX-TC-029-004 — DRAFT exercise not visible in list

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001, TC-COND-008`
**Oracle Source:** `BR-EXERCISE-001` (only PUBLISHED visible)

**Preconditions:**
- ExerciseRepository mocked to return only PUBLISHED exercises (DRAFT filtered at query level)

**Test Steps:**
1. Arrange: Mock `findByStatusAndFilters(PUBLISHED, null, null, PageRequest(0,20))` to return Page with FX-029-001 only (DRAFT exercise FX-029-003 not included because query filters by status)
2. Act: Call `exerciseQueryService.listPublishedExercises(null, null, 0, 20)`
3. Assert: Result does not contain any exercise with status DRAFT

**Expected Result (PASS):**
- Response contains only PUBLISHED exercises
- No DRAFT or ARCHIVED exercises in response

**Expected Result (FAIL):**
- DRAFT exercise appears in response list
- Service does not pass status=PUBLISHED filter to repository

**Current Status:** 🔴 Not written
**Implementation Note:** This is CRITICAL — the service MUST always pass `status=PUBLISHED` to the repository query. Never fetch all and filter in memory.

---

### EX-TC-029-005 — Exercise ID not found returns 404 with EX-001

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseQueryService.getExerciseDetail()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-EXERCISE-IMP-001 S10` (error code EX-001)

**Preconditions:**
- ExerciseRepository mocked
- Non-existent UUID

**Test Steps:**
1. Arrange: Mock `findByExerciseIdAndStatus(nonExistentId, PUBLISHED)` to return Optional.empty()
2. Act: Call `exerciseQueryService.getExerciseDetail(nonExistentId)`
3. Assert: ExerciseNotFoundException thrown with error code EX-001

**Expected Result (PASS):**
- ExerciseNotFoundException thrown
- Error code = "EX-001"
- HTTP status = 404

**Expected Result (FAIL):**
- Returns null instead of throwing exception
- Wrong error code
- Returns 500 instead of 404

**Current Status:** 🔴 Not written

---

### EX-TC-029-006 — No JWT returns 401

**Severity:** `HIGH`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `ExerciseController` — Spring Security configuration
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC` (JWT required)

**Preconditions:**
- Spring Security configured
- No JWT token provided

**Test Steps:**
1. Arrange: No Authorization header
2. Act: Call `GET /api/v1/exercises` without JWT
3. Assert: HTTP 401 Unauthorized

**Expected Result (PASS):**
- HTTP 401 response
- No exercise data leaked

**Expected Result (FAIL):**
- HTTP 200 with exercise data (security vulnerability)
- HTTP 403 instead of 401

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EX-TC-029-SEC-001 — Non-MOTHER role cannot access exercises

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ExerciseController` — Role-based access control
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- JWT token with EXPERT role (not MOTHER)

**Test Steps (Attack Simulation):**
1. Arrange: Create JWT with role=EXPERT
2. Act: Call `GET /api/v1/exercises` with EXPERT JWT
3. Assert: HTTP 403 Forbidden

**Expected Result (PASS = system secure):**
- HTTP 403 Forbidden

**Expected Result (FAIL = vulnerability):**
- HTTP 200 with exercise data (unauthorized access)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EX-TC-029-INT-001 — Integration: Seed 3 exercises (2 PUBLISHED, 1 DRAFT), list returns 2

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → Repository → DB`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-008`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically
- Seed: FX-029-001 (PUBLISHED, FIRST, EASY), FX-029-002 (PUBLISHED, SECOND, MEDIUM), FX-029-003 (DRAFT, FIRST, EASY)

**Test Steps:**
1. Seed 3 exercises into database: 2 PUBLISHED + 1 DRAFT
2. Call `GET /api/v1/exercises` with valid MOTHER JWT
3. Assert response contains exactly 2 exercises
4. Assert DRAFT exercise (FX-029-003) is NOT in response
5. Assert each returned exercise has `safetyWarning` field populated (not null)

**Expected Result (PASS):**
- HTTP 200
- Response body `items` array has length 2
- All items have `status` attribute absent (not exposed) or implied PUBLISHED
- FX-029-003 title "Draft Exercise - Not Visible" does NOT appear
- Each item has non-null `safetyWarning`

**Expected Result (FAIL):**
- Response returns 3 exercises (DRAFT leaked)
- Any item has null `safetyWarning`
- HTTP error

**DB Assertion:**
```java
// Verify DB has 3 total exercises but API returns only 2
long totalInDb = exerciseRepository.count();
assertThat(totalInDb).isEqualTo(3);

// Verify API list
var response = mockMvc.perform(get("/api/v1/exercises")
    .header("Authorization", "Bearer " + motherJwt))
    .andExpect(status().isOk())
    .andReturn();

var items = objectMapper.readTree(response.getResponse().getContentAsString())
    .get("items");
assertThat(items.size()).isEqualTo(2);
```

**Current Status:** 🔴 Not written

---

### EX-TC-029-INT-002 — Integration: Exercise detail with null safety_warning in DB returns empty string

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: detail endpoint with null safety_warning mapping`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- PostgreSQL container running
- Seed: 1 PUBLISHED exercise with `safety_warning = NULL` in DB

**Test Steps:**
1. Seed 1 PUBLISHED exercise with safety_warning = NULL
2. Call `GET /api/v1/exercises/{exerciseId}` with valid MOTHER JWT
3. Assert response has `safetyWarning` = `""` (empty string, NOT null)

**Expected Result (PASS):**
- HTTP 200
- `safetyWarning` field = `""` (empty string)

**Expected Result (FAIL):**
- `safetyWarning` is null in JSON response
- `safetyWarning` field missing from response

**Current Status:** 🔴 Not written
**Implementation Note:** L3 logic issue — ExerciseMapper must convert null DB value to empty string.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EX-TC-029-001` | `ExerciseQueryServiceTest.java` | `[ ]` | `___` | |
| `EX-TC-029-002` | `ExerciseQueryServiceTest.java` | `[ ]` | `___` | |
| `EX-TC-029-003` | `ExerciseQueryServiceTest.java` | `[ ]` | `___` | |
| `EX-TC-029-004` | `ExerciseQueryServiceTest.java` | `[ ]` | `___` | |
| `EX-TC-029-005` | `ExerciseQueryServiceTest.java` | `[ ]` | `___` | |
| `EX-TC-029-006` | `ExerciseControllerSecurityTest.java` | `[ ]` | `___` | |
| `EX-TC-029-SEC-001` | `ExerciseControllerSecurityTest.java` | `[ ]` | `___` | |
| `EX-TC-029-INT-001` | `ExerciseIntegrationTest.java` | `[ ]` | `___` | |
| `EX-TC-029-INT-002` | `ExerciseIntegrationTest.java` | `[ ]` | `___` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub.
> Mọi test PHẢI FAIL. Nếu test PASS ngay → AP-AI-002 detected → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ExerciseQueryService implements IExerciseQueryService {

    @Override
    public PaginatedResponse<ExerciseSummaryResponse> listPublishedExercises(
            TrimesterScope trimester, DifficultyLevel difficulty, int page, int size) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ApiResponse<ExerciseDetailResponse> getExerciseDetail(UUID exerciseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EX-TC-029-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `EX-TC-029-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXERCISE-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] V1 migration with pregnancy_exercises table đã chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho ExerciseQueryService
- [ ] Không có business logic trong ExerciseController (chỉ có validation + mapping)
- [ ] DRAFT/ARCHIVED exercises never appear in API responses (verified by EX-TC-029-004, EX-TC-029-INT-001)
- [ ] safety_warning never null in response (verified by EX-TC-029-INT-002)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (S5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- V1 migration chưa applied
- IAM/Security module chưa sẵn sàng cho JWT validation
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert test and implementation files
git checkout -- src/main/java/com/carebridge/backend/exercise/
git checkout -- src/test/java/com/carebridge/backend/exercise/

# No migration to revert — UC29 uses existing V1 schema
# Feature remains not-implemented — no harm to existing functionality
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (S5.1) | ☐ | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*UC29 — View and Select Pregnancy Exercise — 9 test cases (6 unit + 1 security + 2 integration)*
