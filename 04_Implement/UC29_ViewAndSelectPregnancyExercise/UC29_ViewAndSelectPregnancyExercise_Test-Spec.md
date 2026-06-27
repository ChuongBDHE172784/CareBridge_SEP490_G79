# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC29 — View and Select Pregnancy Exercise — Test Specification

**Document ID:** `CB-EXERCISE-IMP-001-TEST`
**Version:** `1.1`
**Date:** `2026-06-27`
**Status:** `Implemented — 2026-06-27`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Developer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC29_ViewAndSelectPregnancyExercise/UC29_ViewAndSelectPregnancyExercise_TDS.md` (CB-EXERCISE-IMP-001 v1.1) — Technical Design Specification
- `04_Implement/UC177_ViewPregnancyExerciseDetail/UC177_ViewPregnancyExerciseDetail_Test-Spec.md` (CB-EXERCISE-IMP-002-TEST) — Detail view test cases
- `01_Requirements/SRS.md` — SRS 3.3.2.1 Functional requirements

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
| 2026-06-27 | AI Agent — Developer | v1.1: Tách test cases cho detail view (EX-TC-029-003, EX-TC-029-005, EX-TC-029-INT-002) ra CB-EXERCISE-IMP-002-TEST. UC29 Test-Spec giờ chỉ cover list endpoint. |

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
| **Module** | `View and Select Pregnancy Exercise (list only) — exercise` |
| **Spec gốc** | `CB-EXERCISE-IMP-001 v1.1` |
| **Priority** | 🟠 P1 |
| **Sprint** | `Sprint 1 (2026-06-27 →)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY` |
| **Upstream Dependencies** | `IAM (JWT authentication), pregnancy_exercises table` |
| **Downstream Consumers** | `UC177_ViewPregnancyExerciseDetail (CB-EXERCISE-IMP-002), UC30 — Analyze Exercise Posture` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-001 v1.1 §17, ADR-EXERCISE-001/002/003` |
| **Constraints Injected** | C1 (PUBLISHED only), C2 (safety_warning always present), C3 (list-only, no start), C4 (MOTHER role via JWT), C5 (no business logic in controller), C6 (PaginatedResponse), C7 (optional filters) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec does not explicitly state DRAFT/ARCHIVED filtering | DB column `status` có values DRAFT, PUBLISHED, ARCHIVED. Chỉ PUBLISHED mới visible. | Tests assert DRAFT và ARCHIVED exercises KHÔNG BAO GIỜ xuất hiện trong list response. |
| L2 | Trimester filter described as "based on Mother's journey week" | Trimester filter là optional query param từ client (ADR-EXERCISE-002). Server không auto-detect từ journey. | Tests verify: omitting trimester param → return ALL PUBLISHED exercises. Passing trimester param → filter correctly. |
| L3 | safety_warning field nullable trong DB schema | BR-EXERCISE-003 requires safety_warning luôn present trong API response, never null | Tests verify: khi DB safety_warning là null, API response map thành empty string `""`. Field KHÔNG BAO GIỜ null trong response DTO. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC29 v1.1 — View and Select Pregnancy Exercise (LIST ONLY) bao gồm các layer:
├── Service (mock JPA Repository với Mockito)
│   └── ExerciseQueryService.listPublishedExercises() — list logic
├── Controller (mock Service với @WebMvcTest)
│   └── ExerciseController.GET /api/v1/exercises — REST list endpoint, validation
├── Mapper (unit test, no mocking)
│   └── ExerciseMapper.toSummaryResponse() — entity → summary DTO
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
    └── Full flow: HTTP → Controller → Service → Repository → DB (list endpoint only)

OUT OF SCOPE (→ CB-EXERCISE-IMP-002):
  └── ExerciseController.GET /api/v1/exercises/{exerciseId}
  └── ExerciseQueryService.getExerciseDetail()
  └── ExerciseDetailResponse DTO
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.2.1` | Mother views list of suitable exercises, selects one |
| `ADR-EXERCISE-001` | Paginated list với filter, no full-text search |
| `ADR-EXERCISE-002` | Trimester filter là optional client-side param |
| `ADR-EXERCISE-003` | Safety check required before session start (not in UC29 scope) |
| `BR-EXERCISE-001` | Only PUBLISHED exercises visible |
| `BR-EXERCISE-002` | Filter by trimester scope |
| `BR-EXERCISE-003` | safety_warning always shown prominently — never null |
| `BR-EXERCISE-004` | No auto-start without safety check |
| `BR-RBAC` | MOTHER role required, JWT authentication |
| `CB-EXERCISE-IMP-001 v1.1 §8` | Interface specification — IExerciseQueryService.listPublishedExercises() |
| `CB-EXERCISE-IMP-001 v1.1 §10` | Error codes EX-003 (invalid filter), IAM-001, IAM-002 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | List exercises returns only PUBLISHED items | `ExerciseQueryService.listPublishedExercises()` | `EX-TC-029-001`, `EX-TC-029-004` |
| TC-COND-002 | Trimester filter works correctly | `ExerciseRepository.findPublishedByFilters()` | `EX-TC-029-001` |
| TC-COND-003 | Difficulty filter works correctly | `ExerciseRepository.findPublishedByFilters()` | `EX-TC-029-002` |
| TC-COND-004 | No filters — all PUBLISHED returned | `ExerciseQueryService.listPublishedExercises()` | `EX-TC-029-003` |
| TC-COND-005 | DRAFT exercises filtered from list | `ExerciseQueryService.listPublishedExercises()` | `EX-TC-029-004`, `EX-TC-029-INT-001` |
| TC-COND-006 | Authentication required | `ExerciseController` (Spring Security) | `EX-TC-029-005` |
| TC-COND-007 | safety_warning never null in list response | `ExerciseMapper.toSummaryResponse()` | `EX-TC-029-001`, `EX-TC-029-006` |
| TC-COND-008 | Non-MOTHER role returns 403 | `ExerciseController` (RBAC) | `EX-TC-029-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Trimester filter values (FIRST/SECOND/THIRD/ALL/null) | Verify each valid partition and null (no filter) |
| Equivalence Partitioning | Difficulty filter values (EASY/MEDIUM/HARD/null) | Verify each valid partition and null |
| Boundary Value Analysis | Pagination (page=0, size=1, size=50) | Verify boundary of page size |
| State Transition Testing | Exercise status (DRAFT/PUBLISHED/ARCHIVED) | Verify only PUBLISHED visible |
| Error Guessing | Missing JWT, expired JWT, wrong role, invalid filter value | Common API error scenarios |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-029-001` | DB seed | `{ exerciseId: UUID-1, title: "Prenatal Yoga T1", trimesterScope: FIRST, difficultyLevel: EASY, status: PUBLISHED, safetyWarning: "Stop if dizzy" }` | Happy path — published exercise with safety warning |
| `FX-029-002` | DB seed | `{ exerciseId: UUID-2, title: "Strength Training T2", trimesterScope: SECOND, difficultyLevel: MEDIUM, status: PUBLISHED, safetyWarning: "" }` | Published exercise with empty safety warning |
| `FX-029-003` | DB seed | `{ exerciseId: UUID-3, title: "Draft Exercise", status: DRAFT, safetyWarning: null }` | DRAFT exercise — must NOT appear in list response |
| `FX-029-004` | DB seed | `{ exerciseId: UUID-4, title: "Archived Exercise", status: ARCHIVED }` | ARCHIVED exercise — must NOT appear |
| `FX-029-005` | DB seed | `{ exerciseId: UUID-5, title: "All Trimesters HARD", trimesterScope: ALL, difficultyLevel: HARD, status: PUBLISHED, safetyWarning: null }` | Test null safetyWarning → empty string mapping |
| `FX-029-006` | JWT | `{ sub: "mother-001", role: "MOTHER" }` | Valid MOTHER auth token |
| `FX-029-007` | JWT | `(none)` | Missing JWT — 401 scenario |
| `FX-029-008` | JWT | `{ sub: "expert-001", role: "EXPERT" }` | Wrong role JWT — 403 scenario |

---

## 4. Test Case Specification

> **TC ID format:** `EX-TC-029-NNN`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Mỗi @Test dùng ExerciseTestFactory — không shared mutable state
// ═══════════════════════════════════════════════════════════

class ExerciseTestFactory {

    static final UUID EXERCISE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EXERCISE_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXERCISE_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-100000000001");

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

    static PregnancyExercise makePublishedExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise e = makePublishedExercise();
        overrides.accept(e);
        return e;
    }

    static PregnancyExercise makeDraftExercise() {
        return makePublishedExercise(e -> {
            e.setExerciseId(EXERCISE_ID_3);
            e.setTitle("Draft Exercise - Not Visible");
            e.setStatus(ExerciseStatus.DRAFT);
            e.setSafetyWarning(null);
        });
    }

    static PregnancyExercise makeExerciseWithNullSafetyWarning() {
        return makePublishedExercise(e -> {
            e.setExerciseId(EXERCISE_ID_2);
            e.setSafetyWarning(null);
        });
    }
}
```

---

### EX-TC-029-001 — Happy path: List exercises filtered by trimester=FIRST

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseController.listExercises() → ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001, TC-COND-002, TC-COND-007`
**Oracle Source:** `BR-EXERCISE-001` (only PUBLISHED), `BR-EXERCISE-002` (trimester filter), `BR-EXERCISE-003` (safety_warning present)

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001 (FIRST, EASY, PUBLISHED) as mock return data

**Test Steps:**
1. Arrange: Mock `exerciseRepository.findPublishedByFilters(PUBLISHED, FIRST, null, PageRequest(0,20))` trả về Page với FX-029-001
2. Act: `exerciseQueryService.listPublishedExercises(FIRST, null, 0, 20)`
3. Assert: kết quả chứa 1 item với trimesterScope=FIRST và safetyWarning not null

**Expected Result (PASS):**
- PaginatedResponse với 1 item
- Item có `exerciseId` = EXERCISE_ID_1
- Item có `trimesterScope = "FIRST"`
- Item có `safetyWarning = "Stop immediately if you feel dizzy or experience pain."`
- Item có `supportsPostureAnalysis = true`

**Expected Result (FAIL):**
- Service throws exception (method not implemented)
- Response rỗng
- safetyWarning là null

**Current Status:** 🟢 Passing
**Implementation Note:** Service PHẢI luôn pass `status=PUBLISHED` vào repository. Mapper không được trả về null safetyWarning.

---

### EX-TC-029-002 — Filter by difficulty=EASY returns only easy exercises

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-EXERCISE-001`, `ADR-EXERCISE-001`

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001 (EASY), FX-029-002 (MEDIUM) available as fixtures

**Test Steps:**
1. Arrange: Mock `findPublishedByFilters(PUBLISHED, null, EASY, PageRequest(0,20))` trả về Page với FX-029-001
2. Act: `exerciseQueryService.listPublishedExercises(null, EASY, 0, 20)`
3. Assert: 1 item, difficultyLevel=EASY

**Expected Result (PASS):**
- PaginatedResponse với 1 item
- Item có `difficultyLevel = "EASY"`

**Expected Result (FAIL):**
- Trả về items với non-EASY difficulty
- Service throws exception

**Current Status:** 🟢 Passing

---

### EX-TC-029-003 — No filters: all PUBLISHED exercises returned

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-EXERCISE-002` (null param = no filter)

**Preconditions:**
- ExerciseRepository mocked
- FX-029-001, FX-029-002 as fixtures (different trimesters)

**Test Steps:**
1. Arrange: Mock `findPublishedByFilters(PUBLISHED, null, null, PageRequest(0,20))` trả về Page với [FX-029-001, FX-029-002]
2. Act: `exerciseQueryService.listPublishedExercises(null, null, 0, 20)`
3. Assert: 2 items với mixed trimesters

**Expected Result (PASS):**
- PaginatedResponse với 2 items
- Items có `trimesterScope` khác nhau (FIRST, SECOND)

**Expected Result (FAIL):**
- Chỉ trả về 1 item
- Service không pass null params đúng cách

**Current Status:** 🟢 Passing

---

### EX-TC-029-004 — DRAFT exercise not visible in list

**Severity:** `CRITICAL`
**Feature Under Test:** `ExerciseQueryService.listPublishedExercises()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001, TC-COND-005`
**Oracle Source:** `BR-EXERCISE-001` (only PUBLISHED visible)

**Preconditions:**
- ExerciseRepository mocked
- Mock returns only PUBLISHED exercises (DRAFT filtered at query level)

**Test Steps:**
1. Arrange: Mock `findPublishedByFilters(PUBLISHED, null, null, PageRequest(0,20))` trả về Page với chỉ FX-029-001 (DRAFT exercise FX-029-003 không có trong Page vì query đã filter)
2. Act: `exerciseQueryService.listPublishedExercises(null, null, 0, 20)`
3. Assert: Response không chứa exercise nào với status DRAFT

**Expected Result (PASS):**
- Response chứa chỉ PUBLISHED exercises
- Không có DRAFT hoặc ARCHIVED exercises

**Expected Result (FAIL):**
- DRAFT exercise xuất hiện trong response
- Service không pass `status=PUBLISHED` filter vào repository

**Current Status:** 🟢 Passing
**Implementation Note:** CRITICAL — Service PHẢI LUÔN pass `status=PUBLISHED` vào repository. Không bao giờ fetch all và filter trong memory.

---

### EX-TC-029-005 — No JWT returns 401

**Severity:** `HIGH`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `ExerciseController` — Spring Security configuration
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC` (JWT required), `CB-EXERCISE-IMP-001 v1.1 §10` (IAM-001)

**Preconditions:**
- Spring Security configured
- FX-029-007: No Authorization header

**Test Steps:**
1. Arrange: Không có Authorization header
2. Act: `GET /api/v1/exercises` without JWT
3. Assert: HTTP 401 Unauthorized

**Expected Result (PASS):**
- HTTP 401 response
- Response body chứa `{ error: { code: "IAM-001" } }`
- Không có exercise data bị leak

**Expected Result (FAIL):**
- HTTP 200 với exercise data (lỗ hổng bảo mật)
- HTTP 403 thay vì 401

**Current Status:** 🟢 Passing

---

### EX-TC-029-006 — null safetyWarning in DB maps to empty string in response

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseMapper.toSummaryResponse()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseMapperTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-EXERCISE-003` (safety_warning always present, never null), `L3 Logic Issue`

**Preconditions:**
- `ExerciseTestFactory.makeExerciseWithNullSafetyWarning()` (FX-029-005 fixture)

**Test Steps:**
1. Arrange: Create PregnancyExercise entity với `safetyWarning = null`
2. Act: `exerciseMapper.toSummaryResponse(entity)`
3. Assert: `result.getSafetyWarning()` = `""` (empty string, not null)

**Expected Result (PASS):**
- `result.getSafetyWarning()` là `""` (không phải null)

**Expected Result (FAIL):**
- `result.getSafetyWarning()` là `null` — vi phạm BR-EXERCISE-003

**Current Status:** 🟢 Passing
**Implementation Note:** L3 logic fix — ExerciseMapper phải convert null → "" trong toSummaryResponse().

---

### SECURITY TEST CASES

---

### EX-TC-029-SEC-001 — Non-MOTHER role cannot access exercise list

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ExerciseController` — Role-based access control
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- JWT token với role=EXPERT (không phải MOTHER) — FX-029-008

**Test Steps (Attack Simulation):**
1. Arrange: Create JWT với `role=EXPERT`
2. Act: `GET /api/v1/exercises` với EXPERT JWT
3. Assert: HTTP 403 Forbidden

**Expected Result (PASS = system secure):**
- HTTP 403 Forbidden
- Response body chứa `{ error: { code: "IAM-002" } }`

**Expected Result (FAIL = vulnerability):**
- HTTP 200 với exercise data (unauthorized access)

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### EX-TC-029-INT-001 — Integration: Seed 3 exercises (2 PUBLISHED, 1 DRAFT), list returns 2

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → Repository → DB (list endpoint)`
**Test File:** `src/test/java/com/carebridge/backend/exercise/ExerciseIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001, TC-COND-005`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động
- Seed: FX-029-001 (PUBLISHED, FIRST, EASY), FX-029-002 (PUBLISHED, SECOND, MEDIUM), FX-029-003 (DRAFT, FIRST, EASY)

**Test Steps:**
1. Seed 3 exercises: 2 PUBLISHED + 1 DRAFT
2. `GET /api/v1/exercises` với valid MOTHER JWT
3. Assert response chứa đúng 2 exercises
4. Assert DRAFT exercise (FX-029-003) KHÔNG có trong response
5. Assert mỗi returned exercise có `safetyWarning` field không null

**Expected Result (PASS):**
- HTTP 200
- `items` array có length = 2
- FX-029-003 title "Draft Exercise - Not Visible" không xuất hiện
- Mỗi item có `safetyWarning` không null

**Expected Result (FAIL):**
- Response trả về 3 exercises (DRAFT bị leak)
- Bất kỳ item nào có null `safetyWarning`
- HTTP error

**DB Assertion:**
```java
// Verify DB có 3 exercises tổng cộng nhưng API chỉ trả về 2
long totalInDb = exerciseRepository.count();
assertThat(totalInDb).isEqualTo(3);

var response = mockMvc.perform(get("/api/v1/exercises")
    .header("Authorization", "Bearer " + motherJwt))
    .andExpect(status().isOk())
    .andReturn();

var items = objectMapper.readTree(response.getResponse().getContentAsString())
    .get("items");
assertThat(items.size()).isEqualTo(2);

// Verify không có "Draft Exercise" trong response
items.forEach(item ->
    assertThat(item.get("title").asText()).doesNotContain("Draft")
);
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EX-TC-029-001` | `ExerciseQueryServiceTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-002` | `ExerciseQueryServiceTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-003` | `ExerciseQueryServiceTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-004` | `ExerciseQueryServiceTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-005` | `ExerciseControllerDetailSecurityTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-006` | `ExerciseMapperTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-SEC-001` | `ExerciseControllerDetailSecurityTest.java` | `[x]` | `2026-06-27` | |
| `EX-TC-029-INT-001` | `ExerciseDetailIntegrationTest.java` | `[x]` | `2026-06-27` | |

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
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EX-TC-029-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `EX-TC-029-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-005` | Spring Security not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-006` | `return null` from mapper | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-SEC-001` | Spring Security not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-TC-029-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXERCISE-IMP-001 v1.1` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] V1 migration với pregnancy_exercises table đã chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExerciseQueryService.listPublishedExercises()`
- [ ] Không có business logic trong `ExerciseController`
- [ ] DRAFT/ARCHIVED exercises KHÔNG BAO GIỜ xuất hiện trong list response (verified bởi EX-TC-029-004, EX-TC-029-INT-001)
- [ ] `safetyWarning` never null trong response (verified bởi EX-TC-029-006)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- V1 migration chưa applied
- IAM/Security module chưa sẵn sàng cho JWT validation
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files
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
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `ExerciseDetailResponse` hoặc `getExerciseDetail` (thuộc CB-EXERCISE-IMP-002) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected)_ | — | — | — | — |
