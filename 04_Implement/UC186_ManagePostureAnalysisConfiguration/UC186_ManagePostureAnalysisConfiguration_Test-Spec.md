# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC186 — Manage Posture Analysis Configuration — Test Specification

**Document ID:** `CB-EXERCISE-IMP-ADMIN-002-TDD`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `N/A — no PII`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`posture_analysis_configs`, lines 1245-1259)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260704103000__add_posture_config_confidence_threshold_check.sql` — actual migration applied (BR-SAFETY `CHECK` constraint; real timestamp used per CLAUDE.md instead of the TDS's placeholder date)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.6.2 (UC-186, lines 1688-1707)
- `04_Implement/UC186_ManagePostureAnalysisConfiguration/UC186_ManagePostureAnalysisConfiguration_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC185_ManagePregnancyExercises/UC185_ManagePregnancyExercises_TDS.md` — sibling admin-CRUD convention reference

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm test` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC186 Manage Posture Analysis Configuration |
| `2026-07-04` | `AI Agent` | Approved by user — proceeding to implementation |
| `2026-07-04` | `AI Agent` | Implemented and verified: 30/30 backend tests GREEN via `./mvnw test` (PostureConfigServiceTest x11, AdminPostureConfigControllerSecurityTest x11, AdminPostureConfigControllerTest x3, CreatePostureConfigRequestValidationTest x5). `PostureConfigLifecycleIntegrationTest` (PAC-TC-INT-001/002, PAC-TC-THRESH-007) written and compiles but NOT executed — Docker daemon unavailable this session. Web component tests (PAC-TC-WEB-001..003) not started — Admin Portal UI out of scope this session. See §5 RGR tracker for per-TC detail. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
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
| **Feature / Gap ID** | `GAP-UC186` |
| **Module** | `Posture Analysis Configuration — exercise bounded context` |
| **Spec gốc** | `CB-EXERCISE-IMP-ADMIN-002` (UC186 TDS) |
| **Priority** | 🟠 P1 (BR-SAFETY relevant — gates AI feedback shown to pregnant Mothers) |
| **Sprint** | `S3 & S4` (per task Owner: TV1-Phương) |
| **Milestone** | Admin Portal exercise-management feature set |
| **Data Classification** | `Internal` (no PII) |
| **Compliance Scope** | `N/A` (PDPA/GDPR); `BR-RBAC`, `BR-SAFETY` |
| **Upstream Dependencies** | `pregnancy_exercises` (exercise existence + `supportsPostureAnalysis`), IAM/RBAC |
| **Downstream Consumers** | UC180 Enable Posture Camera (`getActiveConfig()`, **untouched** by this feature), `posture_feedback_events` (FK to `posture_config_id`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC186_TDS §17.1` (C1-C6), `ADR-PAC-001` through `ADR-PAC-005` |
| **Constraints Injected** | C1 (do not touch `getActiveConfig()`), C2 (threshold range BR-SAFETY), C3 (SYSTEM_ADMIN only), C4 (append-only versioning), C5 (system-set `effectiveFrom`), C6 (audit every mutation) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T1 (spec-only, no code written yet) → T2 pending implementation → T3 pending Red Gate` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS wording "manages... rule or model version" is ambiguous between single-mutable-row vs. version-history — naive implementation could `UPDATE` a row's `confidence_threshold` in place | `posture_analysis_configs` has **no unique constraint on `exercise_id`** and a `status`/`effective_from`/`effective_to` shape (`V1__init_schema.sql` L1245-1259) — confirms append-only multi-version design (ADR-PAC-002) | `PAC-TC-VER-*` tests assert that `createNewVersion()` **inserts a new row** and leaves the prior row's `analysis_mode`/`confidence_threshold`/etc. byte-for-byte unchanged (only `status`/`effective_to` flip) |
| L2 | Existing `findActiveConfigByExerciseId` query does not filter `effectiveFrom <= now` — a naive "admin schedules a future config" feature would silently activate early | ADR-PAC-003: `effectiveFrom` is always system-set to `now()`, never client-supplied | `PAC-TC-CREATE-006` asserts `CreatePostureConfigRequest`/`UpdatePostureConfigRequest` have **no** `effectiveFrom` field exposed in the request DTO/JSON contract; service always computes it |
| L3 | `confidence_threshold numeric` has no DB-level range bound; a naive implementation might rely on DTO validation alone | ADR-PAC-004: two-layer defense — DTO `@DecimalMin/@DecimalMax` **and** DB `CHECK` constraint (`V20260707130000`) | `PAC-TC-THRESH-*` boundary tests (CRITICAL) cover both layers; `PAC-TC-INT-003` verifies the DB `CHECK` constraint directly via raw insert bypassing the service |
| L4 | Sibling UC185 uses `CONTENT_ADMIN`; a naive copy-paste implementation of UC186 could reuse the same role annotation | SRS UC-186 explicitly names Primary Actor "System Admin", distinct from UC-185's "Content Admin" (ADR-PAC-001) | `PAC-TC-SEC-001` explicitly asserts `CONTENT_ADMIN` gets `403` on all four new endpoints — this is the single most important regression guard against a copy-paste role mistake |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Posture Analysis Configuration (UC186) bao gồm các layer:
├── Domain (enums: AnalysisMode, PostureFeedbackLevel, PostureConfigStatus — pure, no deps)
├── Application / Service (PostureConfigServiceImpl — NEW methods only; mock JPA Repositories with Mockito)
├── Controller (AdminPostureConfigController — mock IPostureConfigService with @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — full create/new-version/activate/list lifecycle + DB CHECK constraint)
└── Web (Vitest — postureConfiguration feature: API client, version-history list, create/new-version form)

OUT OF SCOPE for this Test-Spec: PostureConfigServiceImpl.getActiveConfig() — already implemented,
already has its own coverage; not re-tested here except as a regression guard (§4, PAC-TC-REGRESSION-001).
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-186` | Create/manage analysis mode, rule/model version, confidence threshold, feedback level; System Admin actor; BR-RBAC, BR-SAFETY |
| `ADR-PAC-001` | `SYSTEM_ADMIN`-only authorization, explicitly excluding `CONTENT_ADMIN` |
| `ADR-PAC-002` | Append-only versioning: create=first row, new-version=insert+supersede, activate=flip target+supersede current |
| `ADR-PAC-003` | `effectiveFrom` always system-set, never client-supplied |
| `ADR-PAC-004` | `confidenceThreshold` bounded `[0.0, 1.0]` at DTO + DB layer (BR-SAFETY, CRITICAL) |
| `ADR-PAC-005` | Every mutation emits exactly 1 `audit_logs` row via `AuditService` |
| `V1__init_schema.sql` L1245-1259, L1979-1989 | Column types/nullability, FK targets (`pregnancy_exercises`, `users`), existing index |
| `V20260707130000` (new) | `CHECK` constraint on `confidence_threshold` |
| BR-RBAC / BR-SAFETY | Compliance scope for this project |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin creates the first config for an exercise that supports posture analysis | `PostureConfigServiceImpl.createConfig()` | `PAC-TC-CREATE-001` |
| TC-COND-002 | Admin attempts to create a config for a nonexistent exercise | `createConfig()` | `PAC-TC-CREATE-002` |
| TC-COND-003 | Admin attempts to create a config for an exercise with `supportsPostureAnalysis=false` | `createConfig()` | `PAC-TC-CREATE-003` |
| TC-COND-004 | Admin attempts to create a second initial config for an exercise that already has one | `createConfig()` | `PAC-TC-CREATE-004` |
| TC-COND-005 | Confidence threshold boundary values (CRITICAL, BR-SAFETY) | `CreatePostureConfigRequest`/`UpdatePostureConfigRequest` validation + DB `CHECK` | `PAC-TC-THRESH-001..007` |
| TC-COND-006 | Admin creates a new version, superseding the current active one | `createNewVersion()` | `PAC-TC-VER-001` |
| TC-COND-007 | Admin creates a new version when no active config exists yet | `createNewVersion()` | `PAC-TC-VER-002` |
| TC-COND-008 | Admin activates a `SUPERSEDED` version (rollback) | `activateVersion()` | `PAC-TC-ACT-001` |
| TC-COND-009 | Admin activates an already-`ACTIVE` version (idempotent no-op) | `activateVersion()` | `PAC-TC-ACT-002` |
| TC-COND-010 | Admin activates a nonexistent version | `activateVersion()` | `PAC-TC-ACT-003` |
| TC-COND-011 | Admin lists version history for an exercise | `listVersions()` | `PAC-TC-LIST-001` |
| TC-COND-012 | Non-`SYSTEM_ADMIN` roles are denied on all 4 new endpoints | `AdminPostureConfigController` `@PreAuthorize` | `PAC-TC-SEC-001..004` |
| TC-COND-013 | `getActiveConfig()` (existing) is untouched by this feature | Regression guard | `PAC-TC-REGRESSION-001` |
| TC-COND-014 | Full lifecycle end-to-end via real DB | Integration | `PAC-TC-INT-001..003` |
| TC-COND-015 | Web: version-history list renders, create/new-version form validates threshold client-side | React component tests | `PAC-TC-WEB-001..003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `analysisMode`/`feedbackLevel` enum values, exercise existence/support flag | Valid vs. invalid enum values, exercise found/not-found/unsupported partitions |
| Boundary Value Analysis | `confidenceThreshold` | CRITICAL BR-SAFETY field — exact boundaries `0.0`, `1.0`, and just-outside (`-0.01`, `1.01`) must be tested |
| State Transition Testing | `status` (`ACTIVE`/`SUPERSEDED`) | ADR-PAC-002 versioning state machine (§6.5 of TDS) |
| Error Guessing | RBAC bypass, DB `CHECK` bypass via raw SQL, concurrent version creation | Security + defense-in-depth verification |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `PregnancyExercise{status=PUBLISHED, supportsPostureAnalysis=true}` | Happy path exercise |
| `FX-002` | DB seed | `PregnancyExercise{supportsPostureAnalysis=false}` | PAC-005 negative case |
| `FX-003` | DB seed | `PostureAnalysisConfig{status=ACTIVE, confidenceThreshold=0.75}` | Baseline active config for update/activate tests |
| `FX-004` | DB seed | `PostureAnalysisConfig{status=SUPERSEDED, confidenceThreshold=0.6}` | Rollback/activate target |
| `FX-005` | JWT | `{sub: 'admin-001', role: 'SYSTEM_ADMIN'}` | Auth context — happy path |
| `FX-006` | JWT | `{sub: 'content-admin-001', role: 'CONTENT_ADMIN'}` | Auth context — 403 regression guard (L4) |
| `FX-007` | JWT | `{sub: 'mother-001', role: 'MOTHER'}` | Auth context — 403 on admin endpoints |

---

## 4. Test Case Specification

> **TC ID format:** `PAC-TC-[GROUP]-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

// PostureConfigTestFactory.java
class PostureConfigTestFactory {

    static PregnancyExercise makeExercise() {
        return makeExercise(e -> {});
    }

    static PregnancyExercise makeExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise exercise = PregnancyExercise.builder()
                .exerciseId(UUID.fromString("00000000-0000-0000-0000-0000000000e1"))
                .createdBy(UUID.fromString("00000000-0000-0000-0000-0000000000a1"))
                .title("Pelvic Tilt Stretch")
                .status(ExerciseStatus.PUBLISHED)
                .supportsPostureAnalysis(true)
                .versionNo(1)
                .build();
        overrides.accept(exercise);
        return exercise;
    }

    // Baseline ACTIVE config — synced with FX-003
    static PostureAnalysisConfig makeActiveConfig() {
        return makeActiveConfig(c -> {});
    }

    static PostureAnalysisConfig makeActiveConfig(Consumer<PostureAnalysisConfig> overrides) {
        PostureAnalysisConfig config = PostureAnalysisConfig.builder()
                .postureConfigId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"))
                .exerciseId(UUID.fromString("00000000-0000-0000-0000-0000000000e1"))
                .configuredBy(UUID.fromString("00000000-0000-0000-0000-0000000000a1"))
                .analysisMode("MODEL_BASED")
                .ruleOrModelVersion("posenet-v2.1.0")
                .confidenceThreshold(new BigDecimal("0.75"))
                .feedbackLevel("DETAILED")
                .effectiveFrom(OffsetDateTime.parse("2026-07-01T00:00:00Z"))
                .effectiveTo(null)
                .status("ACTIVE")
                .build();
        overrides.accept(config);
        return config;
    }

    // SUPERSEDED config — synced with FX-004
    static PostureAnalysisConfig makeSupersededConfig() {
        return makeActiveConfig(c -> {
            c.setPostureConfigId(UUID.fromString("00000000-0000-0000-0000-0000000000c0"));
            c.setStatus("SUPERSEDED");
            c.setConfidenceThreshold(new BigDecimal("0.60"));
            c.setEffectiveTo(OffsetDateTime.parse("2026-07-01T00:00:00Z"));
        });
    }

    static CreatePostureConfigRequest makeCreateRequest() {
        return makeCreateRequest(r -> {});
    }

    static CreatePostureConfigRequest makeCreateRequest(Consumer<CreatePostureConfigRequest> overrides) {
        CreatePostureConfigRequest request = new CreatePostureConfigRequest();
        request.setExerciseId(UUID.fromString("00000000-0000-0000-0000-0000000000e1"));
        request.setAnalysisMode(AnalysisMode.MODEL_BASED);
        request.setRuleOrModelVersion("posenet-v2.1.0");
        request.setConfidenceThreshold(new BigDecimal("0.75"));
        request.setFeedbackLevel(PostureFeedbackLevel.DETAILED);
        overrides.accept(request);
        return request;
    }

    static UpdatePostureConfigRequest makeUpdateRequest() {
        return makeUpdateRequest(r -> {});
    }

    static UpdatePostureConfigRequest makeUpdateRequest(Consumer<UpdatePostureConfigRequest> overrides) {
        UpdatePostureConfigRequest request = new UpdatePostureConfigRequest();
        request.setAnalysisMode(AnalysisMode.HYBRID);
        request.setRuleOrModelVersion("posenet-v2.2.0");
        request.setConfidenceThreshold(new BigDecimal("0.80"));
        request.setFeedbackLevel(PostureFeedbackLevel.DETAILED);
        overrides.accept(request);
        return request;
    }
}
```

---

### PAC-TC-CREATE-001 — Create initial config: happy path

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigServiceImpl.createConfig()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-PAC-002 §Decision`, TDS §6.1 sequence diagram

**Preconditions:**
- `FX-001` (published exercise, `supportsPostureAnalysis=true`), no existing config for it (`existsByExerciseId` mocked `false`)

**Test Steps:**
1. Arrange: mock `exerciseRepository.findById(exerciseId)` → `FX-001`; mock `postureConfigRepository.existsByExerciseId(exerciseId)` → `false`
2. Act: call `createConfig(PostureConfigTestFactory.makeCreateRequest(), adminUserId)`
3. Assert: `postureConfigRepository.save()` invoked once with `status="ACTIVE"`, `effectiveFrom` ≈ now, `effectiveTo=null`; `auditService.log(POSTURE_CONFIG_CREATED, ...)` invoked once; response `status="ACTIVE"`

**Expected Result (PASS):** `ApiResponse<AdminPostureConfigResponse>` with `status=ACTIVE`, all request fields echoed.
**Expected Result (FAIL):** Wrong status, missing audit call, or `effectiveFrom` taken from request instead of system clock.

**Current Status:** 🔴 Not written
**Implementation Note:** `effectiveFrom` must be set via `OffsetDateTime.now()` inside the service (ADR-PAC-003) — never `request.getEffectiveFrom()` (field does not even exist on the DTO).

---

### PAC-TC-CREATE-002 — Create config: exercise not found

**Severity:** `MEDIUM`
**Feature Under Test:** `createConfig()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §6.1`, existing `ExerciseNotFoundException.notFound()`

**Preconditions:** `exerciseRepository.findById(unknownId)` → `Optional.empty()`

**Test Steps:**
1. Act: `createConfig(makeCreateRequest(r -> r.setExerciseId(unknownId)), adminUserId)`
2. Assert: throws `ExerciseNotFoundException` with code `EX-001`

**Expected Result (PASS):** `ExerciseNotFoundException` thrown, `postureConfigRepository.save()` never called.
**Expected Result (FAIL):** NPE, or silent creation of an orphan config row.

**Current Status:** 🔴 Not written

---

### PAC-TC-CREATE-003 — Create config: exercise does not support posture analysis

**Severity:** `HIGH`
**Feature Under Test:** `createConfig()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-PAC` §1 In-Scope, TDS §6.1 alt branch

**Preconditions:** `FX-002` (`supportsPostureAnalysis=false`)

**Test Steps:**
1. Act: `createConfig(makeCreateRequest(), adminUserId)` where target exercise is `FX-002`
2. Assert: throws `InvalidPostureConfigException` code `PAC-005`, HTTP 409

**Expected Result (PASS):** `PAC-005` thrown, no row persisted.
**Expected Result (FAIL):** Config silently created for an exercise that never offers posture analysis to Mothers.

**Current Status:** 🔴 Not written

---

### PAC-TC-CREATE-004 — Create config: already exists (must use new-version endpoint)

**Severity:** `HIGH`
**Feature Under Test:** `createConfig()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-PAC-002`, error code `PAC-006`

**Preconditions:** `postureConfigRepository.existsByExerciseId(exerciseId)` → `true`

**Test Steps:**
1. Act: `createConfig(makeCreateRequest(), adminUserId)`
2. Assert: throws `InvalidPostureConfigException` code `PAC-006`, HTTP 409

**Expected Result (PASS):** `PAC-006` thrown; guards the append-only invariant (no duplicate "first" rows).
**Expected Result (FAIL):** A second unrelated `ACTIVE` row created for the same exercise, violating "exactly one ACTIVE row" invariant.

**Current Status:** 🔴 Not written

---

### THRESHOLD BOUNDARY TEST CASES (CRITICAL — BR-SAFETY)

> Boundary Value Analysis on `confidenceThreshold` per TDS ADR-PAC-004. These are the highest-priority tests in this spec — an out-of-range threshold can cause the downstream AI posture-detection pipeline to silently mis-trigger safety feedback for a pregnant Mother.

### PAC-TC-THRESH-001 — confidenceThreshold = 0.0 (lower boundary, valid)

**Severity:** `CRITICAL`
**CWE:** `N/A (data integrity, not injection)`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `CreatePostureConfigRequest` Bean Validation`
**Test File:** `src/test/java/com/carebridge/backend/exercise/CreatePostureConfigRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-PAC-004 §Decision — [0.0, 1.0] inclusive`

**Preconditions:** none

**Test Steps:**
1. Build `makeCreateRequest(r -> r.setConfidenceThreshold(new BigDecimal("0.0")))`
2. Run `jakarta.validation.Validator.validate(request)`
3. Assert: zero violations

**Expected Result (PASS):** No validation violations — `0.0` is a valid inclusive boundary.
**Expected Result (FAIL):** A violation is raised (over-restrictive `@DecimalMin`), incorrectly rejecting a legitimate "always trigger feedback" threshold.

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-002 — confidenceThreshold = 1.0 (upper boundary, valid)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `CreatePostureConfigRequest` Bean Validation
**Test File:** `CreatePostureConfigRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-PAC-004`

**Test Steps:** same as above with `1.0` → zero violations.

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-003 — confidenceThreshold = -0.01 (just below lower boundary, INVALID)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `CreatePostureConfigRequest` Bean Validation
**Test File:** `CreatePostureConfigRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-PAC-004`

**Test Steps:**
1. Build `makeCreateRequest(r -> r.setConfidenceThreshold(new BigDecimal("-0.01")))`
2. Validate
3. Assert: exactly 1 violation on field `confidenceThreshold`, message references the `[0.0, 1.0]` bound

**Expected Result (PASS):** Rejected with a field-level violation.
**Expected Result (FAIL — dangerous):** Value accepted, propagated to the AI posture pipeline as a negative confidence gate (undefined/dangerous downstream behavior).

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-004 — confidenceThreshold = 1.01 (just above upper boundary, INVALID)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `CreatePostureConfigRequest` Bean Validation
**Test File:** `CreatePostureConfigRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:** same pattern with `1.01` → exactly 1 violation.

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-005 — confidenceThreshold = null (INVALID — required field)

**Severity:** `HIGH`
**Feature Under Test:** `CreatePostureConfigRequest` Bean Validation (`@NotNull`)
**Test File:** `CreatePostureConfigRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:** `makeCreateRequest(r -> r.setConfidenceThreshold(null))` → 1 violation (`@NotNull`).

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-006 — HTTP-level: confidenceThreshold=1.5 via POST → 400 PAC-002

**Severity:** `CRITICAL`
**OWASP:** `N/A`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `AdminPostureConfigController.createConfig()` (`@WebMvcTest`)
**Test File:** `src/test/java/com/carebridge/backend/exercise/AdminPostureConfigControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §9.2, §10 error code PAC-002`

**Test Steps:**
1. `POST /api/v1/admin/posture-configs` with `SYSTEM_ADMIN` auth and body `{"exerciseId":"...","analysisMode":"MODEL_BASED","confidenceThreshold":1.5,"feedbackLevel":"DETAILED"}`
2. Assert: HTTP `400`, response body `error.code == "PAC-002"`
3. Assert: `postureConfigService.createConfig()` is **never invoked** (validation fails before the service layer — mock verify zero interactions)

**Expected Result (PASS):** `400 PAC-002`, service never called.
**Expected Result (FAIL):** Request reaches the service/DB layer, or returns a generic `500` instead of a clean `400`.

**Current Status:** 🔴 Not written

---

### PAC-TC-THRESH-007 — DB-level: raw INSERT with out-of-range threshold is rejected by CHECK constraint (Integration)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `posture_analysis_configs` table `CHECK` constraint (`chk_posture_config_confidence_threshold`)
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigConstraintIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-PAC-004 §Decision — DB CHECK is the last-resort backstop`

**Preconditions:** PostgreSQL Testcontainer running, migration `V20260707130000` applied

**Test Steps:**
1. Using `JdbcTemplate`, attempt a raw `INSERT INTO posture_analysis_configs (..., confidence_threshold, ...) VALUES (..., 2.0, ...)` — **bypassing the service/DTO layer entirely**
2. Assert: the insert throws (PostgreSQL raises a `check_violation`, surfaces as `DataIntegrityViolationException`)

**Expected Result (PASS = safe):** Insert rejected at the DB layer even when the application layer is bypassed.
**Expected Result (FAIL = BR-SAFETY gap):** Insert succeeds — confirms the defense-in-depth backstop from ADR-PAC-004 does not actually exist, migration `V20260707130000` is missing or malformed.

**Current Status:** 🔴 Not written

---

### PAC-TC-VER-001 — Create new version: supersedes current active, prior row's fields byte-for-byte unchanged

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigServiceImpl.createNewVersion()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-PAC-002 §Decision`, `TDS §6.2`, Logic Issue `L1`

**Preconditions:** `FX-003` is the current `ACTIVE` config for `FX-001`'s exercise

**Test Steps:**
1. Mock `postureConfigRepository.findByExerciseIdAndStatus(exerciseId, "ACTIVE")` → `FX-003`
2. Act: `createNewVersion(exerciseId, makeUpdateRequest(), adminUserId)`
3. Assert: `postureConfigRepository.save()` called **twice** — once for a **new** row (`status=ACTIVE`, new `postureConfigId`, `effectiveFrom≈now`), once for the **previous** row (same `postureConfigId` as `FX-003`, `status=SUPERSEDED`, `effectiveTo≈now`, but `analysisMode`/`ruleOrModelVersion`/`confidenceThreshold`/`feedbackLevel` **identical** to `FX-003`'s original values — captured via `ArgumentCaptor`)
4. Assert: `auditService.log(POSTURE_CONFIG_UPDATED, ...)` invoked once

**Expected Result (PASS):** Two independent rows: new `ACTIVE` with request's values, old `SUPERSEDED` with its **original, untouched** analysis parameters.
**Expected Result (FAIL):** The prior row's `analysis_mode`/`confidence_threshold`/etc. are overwritten with the new request's values (would destroy BR-SAFETY traceability — Logic Issue L1).

**Current Status:** 🔴 Not written
**Implementation Note:** Never call `previousRow.setAnalysisMode(request.getAnalysisMode())` or similar on the row being superseded — only `setStatus("SUPERSEDED")` and `setEffectiveTo(now)`.

---

### PAC-TC-VER-002 — Create new version: no active config exists yet

**Severity:** `MEDIUM`
**Feature Under Test:** `createNewVersion()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §6.2 alt branch`, error code `PAC-004`

**Preconditions:** `postureConfigRepository.findByExerciseIdAndStatus(exerciseId, "ACTIVE")` → `Optional.empty()`

**Test Steps:**
1. Act: `createNewVersion(exerciseId, makeUpdateRequest(), adminUserId)`
2. Assert: throws `PostureConfigNotFoundException` code `PAC-004`

**Expected Result (PASS):** Clear `404 PAC-004` guiding the admin to use `createConfig()` first.
**Expected Result (FAIL):** NPE or silent creation of an orphan `ACTIVE` row with no history predecessor.

**Current Status:** 🔴 Not written

---

### PAC-TC-ACT-001 — Activate a SUPERSEDED version (rollback)

**Severity:** `HIGH`
**Feature Under Test:** `activateVersion()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §6.3`, `ADR-PAC-002`

**Preconditions:** target = `FX-004` (`SUPERSEDED`); `FX-003` is currently `ACTIVE` for the same exercise

**Test Steps:**
1. Mock `postureConfigRepository.findById(FX-004.id)` → `FX-004`
2. Mock `postureConfigRepository.findByExerciseIdAndStatus(exerciseId, "ACTIVE")` → `FX-003`
3. Act: `activateVersion(FX-004.id, adminUserId)`
4. Assert: `FX-003` saved with `status=SUPERSEDED, effectiveTo≈now`; `FX-004` saved with `status=ACTIVE, effectiveFrom≈now, effectiveTo=null`; `auditService.log(POSTURE_CONFIG_ACTIVATED, ...)` invoked once

**Expected Result (PASS):** Exactly one row is `ACTIVE` after the call — the rolled-back-to version.
**Expected Result (FAIL):** Both rows end up `ACTIVE` (invariant violation) or the previously-active row is left dangling.

**Current Status:** 🔴 Not written

---

### PAC-TC-ACT-002 — Activate an already-ACTIVE version (idempotent no-op)

**Severity:** `MEDIUM`
**Feature Under Test:** `activateVersion()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §6.3 alt branch`, mirrors UC185 ADR-EXERCISE-ADMIN-002's idempotent activate/disable convention

**Preconditions:** target = `FX-003` (`status=ACTIVE` already)

**Test Steps:**
1. Act: `activateVersion(FX-003.id, adminUserId)`
2. Assert: response `status=ACTIVE`, `effectiveFrom` **unchanged** (no version bump); `postureConfigRepository.save()` **not** called (or called with no field changes — implementation choice, but no audit log entry should be produced for a true no-op)

**Expected Result (PASS):** HTTP 200, unchanged state, no spurious audit entry.
**Expected Result (FAIL):** `effectiveFrom` is bumped to "now" unnecessarily, or a duplicate `POSTURE_CONFIG_ACTIVATED` audit entry is created for a no-op.

**Current Status:** 🔴 Not written

---

### PAC-TC-ACT-003 — Activate a nonexistent version

**Severity:** `MEDIUM`
**Feature Under Test:** `activateVersion()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:** `postureConfigRepository.findById(unknownId)` → empty → assert `PostureConfigNotFoundException` code `PAC-004`.

**Current Status:** 🔴 Not written

---

### PAC-TC-LIST-001 — List version history for an exercise

**Severity:** `MEDIUM`
**Feature Under Test:** `listVersions()`
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. Mock `postureConfigRepository.findAllByExerciseIdOrderByEffectiveFromDesc(exerciseId)` → `[FX-003(ACTIVE), FX-004(SUPERSEDED)]`
2. Act: `listVersions(exerciseId)`
3. Assert: response contains both rows, newest (`ACTIVE`) first, correct mapping of all fields including `status`/`effectiveTo`

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### PAC-TC-SEC-001 — CONTENT_ADMIN is denied on all UC186 admin endpoints (regression guard for L4 / ADR-PAC-001)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminPostureConfigController` `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/exercise/AdminPostureConfigControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Valid JWT with role `CONTENT_ADMIN` (`FX-006`) — a role that IS authorized for the sibling UC185 endpoints but must NOT be authorized here.

**Test Steps (Attack Simulation — role-confusion):**
1. `POST /api/v1/admin/posture-configs` with `CONTENT_ADMIN` JWT
2. `POST /api/v1/admin/posture-configs/{exerciseId}/versions` with `CONTENT_ADMIN` JWT
3. `PATCH /api/v1/admin/posture-configs/{id}/activate` with `CONTENT_ADMIN` JWT
4. `GET /api/v1/admin/posture-configs/{exerciseId}` with `CONTENT_ADMIN` JWT

**Expected Result (PASS = hệ thống an toàn):** All 4 return `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any endpoint returns `2xx` — indicates the developer copy-pasted UC185's `@PreAuthorize("hasRole('CONTENT_ADMIN')")` instead of applying `SYSTEM_ADMIN` per ADR-PAC-001. **This is the single highest-value regression test in this spec.**

**Current Status:** 🔴 Not written

---

### PAC-TC-SEC-002 — MOTHER role is denied on all UC186 admin endpoints

**Severity:** `HIGH`
**OWASP:** `A01:2021`
**CWE:** `CWE-863`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminPostureConfigController`
**Test File:** `AdminPostureConfigControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:** Same 4 calls as `PAC-TC-SEC-001` with `FX-007` (`MOTHER`) JWT.
**Expected Result (PASS):** All 4 return `403`.

**Current Status:** 🔴 Not written

---

### PAC-TC-SEC-003 — No JWT → 401 on all UC186 admin endpoints

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminPostureConfigController` / `SecurityConfig`
**Test File:** `AdminPostureConfigControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:** Same 4 calls with no `Authorization` header.
**Expected Result (PASS):** All 4 return `401`.

**Current Status:** 🔴 Not written

---

### PAC-TC-SEC-004 — SYSTEM_ADMIN succeeds on all UC186 admin endpoints (positive control)

**Severity:** `HIGH`
**Feature Under Test:** `AdminPostureConfigController`
**Test File:** `AdminPostureConfigControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:** Same 4 calls with `FX-005` (`SYSTEM_ADMIN`) JWT and valid payloads.
**Expected Result (PASS):** All 4 return their documented success status (`201`/`201`/`200`/`200`) — proves `PAC-TC-SEC-001/002/003` are testing real authorization logic, not an over-broad `denyAll()` that would trivially pass the negative tests.

**Current Status:** 🔴 Not written

---

### PAC-TC-REGRESSION-001 — getActiveConfig() untouched by this feature

**Severity:** `HIGH`
**Feature Under Test:** `PostureConfigServiceImpl.getActiveConfig()` (existing method — RG-3)
**Test File:** `PostureConfigServiceTest.java`
**TDD Phase:** 🟢 Already passing (pre-existing) — this TC re-asserts it after the new methods are added

**Preconditions:** Existing fixtures unchanged from the current implementation.

**Test Steps:**
1. Call `getActiveConfig(exerciseId)` exactly as the existing test suite does today
2. Assert: identical behavior — same exceptions (`ExerciseNotFoundException`, `ResourceNotFoundException`), same `PostureConfigResponse` field mapping, same repository method invoked (`findActiveConfigByExerciseId`)

**Expected Result (PASS):** No behavioral drift — confirms C1 (§17.1 TDS) was honored.
**Expected Result (FAIL):** Any change in `getActiveConfig()`'s signature, exceptions, or response shape — indicates the new methods were added by editing the existing method instead of purely appending new ones.

**Current Status:** 🟢 Passing (pre-existing behavior; this TC only needs to keep passing, not be newly written)

---

### INTEGRATION TEST CASES

> Testcontainers (`PostgreSqlContainer`). Timeout: 120s. Flyway migrations (including `V20260707130000`) apply automatically on Spring context start.

### PAC-TC-INT-001 — Full lifecycle: create → new-version → activate rollback → list (end-to-end)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: createConfig → createNewVersion → activateVersion(rollback) → listVersions`
**Test File:** `src/test/java/com/carebridge/backend/exercise/PostureConfigLifecycleIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- PostgreSQL container running, Flyway migrations applied (incl. `V20260707130000`)
- Seed: one `PUBLISHED` exercise with `supportsPostureAnalysis=true`, one `SYSTEM_ADMIN` user

**Test Steps:**
1. `POST /api/v1/admin/posture-configs` → capture `postureConfigId` #1, assert `status=ACTIVE`
2. `POST /api/v1/admin/posture-configs/{exerciseId}/versions` with different `ruleOrModelVersion` → capture `postureConfigId` #2, assert `status=ACTIVE`
3. `GET /api/v1/admin/posture-configs/{exerciseId}` → assert #1 is now `SUPERSEDED`, #2 is `ACTIVE`
4. `PATCH /api/v1/admin/posture-configs/{postureConfigId#1}/activate` (rollback)
5. `GET /api/v1/admin/posture-configs/{exerciseId}` → assert #1 is `ACTIVE`, #2 is `SUPERSEDED`
6. `GET /api/v1/exercises/{exerciseId}/posture-config` (existing Mother-facing endpoint) → assert it returns #1's values (proves `getActiveConfig()` correctly reflects the rollback without any code change to it)

**Expected Result (PASS):**
- DB assertion: `SELECT count(*) FROM posture_analysis_configs WHERE exercise_id=? AND status='ACTIVE'` = exactly `1` at every step
- API response shapes match §9.2

**Expected Result (FAIL):** More than one `ACTIVE` row at any point; Mother-facing endpoint out of sync with the admin-visible active version.

**DB Assertion:**
```java
List<PostureAnalysisConfig> active = jdbcTemplate.query(
    "SELECT * FROM posture_analysis_configs WHERE exercise_id = ? AND status = 'ACTIVE'",
    (rs, i) -> mapRow(rs), exerciseId);
assertThat(active).hasSize(1);
assertThat(active.get(0).getPostureConfigId()).isEqualTo(expectedActiveId);
```

**Current Status:** 🔴 Not written

---

### PAC-TC-INT-002 — Audit log completeness across full lifecycle

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditService` integration across create/new-version/activate`
**Test File:** `PostureConfigLifecycleIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Test Steps:**
1. Perform the same 3 mutations as `PAC-TC-INT-001` steps 1, 2, 4
2. Query `audit_logs WHERE entity_type = 'PostureAnalysisConfig'`

**Expected Result (PASS):** Exactly 3 rows, one each for `POSTURE_CONFIG_CREATED`, `POSTURE_CONFIG_UPDATED`, `POSTURE_CONFIG_ACTIVATED`, correct `actor_user_id`.

**Current Status:** 🔴 Not written

---

### PAC-TC-INT-003 — DB CHECK constraint survives a raw bypass attempt

*(Same as `PAC-TC-THRESH-007`, listed here for cross-reference in the Integration group — not duplicated as a separate test file.)*

---

### WEB COMPONENT TEST CASES (React + TypeScript + Vitest — Admin Portal)

> Feature folder: `05_Development/CareBridgeWebApp/src/features/postureConfiguration/` (already scaffolded, `.gitkeep` only — this is the first feature populating it).

### PAC-TC-WEB-001 — Version history list renders ACTIVE/SUPERSEDED rows correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `PostureConfigVersionListPage` component
**Test File:** `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/PostureConfigVersionListPage.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock `GET /api/v1/admin/posture-configs/{exerciseId}` → `[{status:'ACTIVE',...}, {status:'SUPERSEDED',...}]`
2. Render `<PostureConfigVersionListPage exerciseId="..." />`
3. Assert: the `ACTIVE` row is visually distinguished (e.g., a badge/label) and appears first

**Expected Result (PASS):** Both rows render with correct status labeling.
**Expected Result (FAIL):** Status not distinguished, or rows in wrong order.

**Current Status:** 🔴 Not written

---

### PAC-TC-WEB-002 — Create/new-version form rejects confidenceThreshold outside [0.0, 1.0] client-side

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** `PostureConfigForm` component
**Test File:** `05_Development/CareBridgeWebApp/src/features/postureConfiguration/components/PostureConfigForm.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Render `<PostureConfigForm onSubmit={mockSubmit} />`
2. Type `1.5` into the confidence-threshold input, submit
3. Assert: inline validation error shown ("must be between 0.0 and 1.0"), `mockSubmit` **not** called

**Expected Result (PASS):** Client-side gate matches server-side `PAC-002` rule — prevents an obviously-invalid request from ever reaching the network (defense-in-depth mirroring ADR-PAC-004 on the frontend).
**Expected Result (FAIL):** Form submits with an out-of-range value, relying solely on the backend to catch it (poor UX, and a symptom the client-side constraint from the TDS wasn't implemented).

**Current Status:** 🔴 Not written

---

### PAC-TC-WEB-003 — "Activate" button calls the correct endpoint and refreshes the list

**Severity:** `MEDIUM`
**Feature Under Test:** `PostureConfigVersionListPage` — activate action
**Test File:** `PostureConfigVersionListPage.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Render list with a `SUPERSEDED` row
2. Click its "Activate" button
3. Assert: `PATCH /api/v1/admin/posture-configs/{id}/activate` called with the correct `id`; list re-fetched afterward showing updated statuses

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PAC-TC-CREATE-001` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing — `./mvnw test` 2026-07-04 |
| `PAC-TC-CREATE-002` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-CREATE-003` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-CREATE-004` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-THRESH-001..005` | `CreatePostureConfigRequestValidationTest.java` | `[x]` | `[x]` | Passing — 5/5 |
| `PAC-TC-THRESH-006` | `AdminPostureConfigControllerTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-THRESH-007` | `PostureConfigLifecycleIntegrationTest.java` (combined with INT group, not a separate file per §note) | `[ ]` | `[ ]` | **Written, NOT executed** — Docker daemon unavailable in this session (Testcontainers cannot start). Code compiles; run manually once Docker is available. |
| `PAC-TC-VER-001` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-VER-002` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-ACT-001..003` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing — 3/3 |
| `PAC-TC-LIST-001` | `PostureConfigServiceTest.java` | `[x]` | `[x]` | Passing |
| `PAC-TC-SEC-001..004` | `AdminPostureConfigControllerSecurityTest.java` | `[x]` | `[x]` | Passing — 11/11 (all 4 endpoints x CONTENT_ADMIN/MOTHER/no-JWT/SYSTEM_ADMIN) |
| `PAC-TC-INT-001..002` | `PostureConfigLifecycleIntegrationTest.java` | `[ ]` | `[ ]` | **Written, NOT executed** — Docker daemon unavailable in this session. |
| `PAC-TC-WEB-001..003` | `postureConfiguration/**/*.test.tsx` | `[ ]` | `[ ]` | **Not started** — Web Admin Portal UI out of scope for this backend-focused session |
| `PAC-TC-REGRESSION-001` | `PostureConfigServiceTest.java` | n/a | `[x]` | Passing — confirms `getActiveConfig()` untouched (C1 honored) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.
> **Scope of Red Gate stubs: NEW methods only** (`createConfig`, `createNewVersion`, `activateVersion`, `listVersions`). `getActiveConfig()` is pre-existing and already implemented — it is NOT stubbed and `PAC-TC-REGRESSION-001` must stay 🟢 throughout.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub for the 4 NEW methods only (PHẢI throw)
// getActiveConfig() body is UNCHANGED — left exactly as-is (RG-3 / constraint C1)
@Service
@RequiredArgsConstructor
public class PostureConfigServiceImpl implements IPostureConfigService {

    private final ExerciseRepository exerciseRepository;
    private final PostureAnalysisConfigRepository postureConfigRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PostureConfigResponse> getActiveConfig(UUID exerciseId) {
        // UNCHANGED — existing implementation, not part of Red Gate scope
    }

    @Override
    public ApiResponse<AdminPostureConfigResponse> createConfig(CreatePostureConfigRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ApiResponse<AdminPostureConfigResponse> createNewVersion(UUID exerciseId, UpdatePostureConfigRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ApiResponse<AdminPostureConfigResponse> activateVersion(UUID postureConfigId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ApiResponse<List<AdminPostureConfigResponse>> listVersions(UUID exerciseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PAC-TC-CREATE-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `PAC-TC-VER-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PAC-TC-ACT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PAC-TC-LIST-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PAC-TC-SEC-004` (positive control) | `throw('Not implemented')` | 🔴 FAIL (should 500, not 2xx) | ☐ FAIL ☐ PASS | |
| `PAC-TC-REGRESSION-001` | *(not stubbed)* | 🟢 PASS (must stay green) | ☐ PASS ☐ FAIL | Any FAIL here means constraint C1 was violated |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled during implementation)
- Tất cả FAIL (except `PAC-TC-REGRESSION-001`)? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `04_Implement/UC186_ManagePostureAnalysisConfiguration/red-gate-evidence.log` (to be created during implementation)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EXERCISE-IMP-ADMIN-002` đã được review và approve
- [ ] Logic Issues (§2, L1-L4) đã được confirm với Principal Architect
- [ ] Flyway migration `V20260707130000__add_posture_config_confidence_threshold_check.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `npm test` (CareBridgeWebApp) — `postureConfiguration` feature tests xanh
- [ ] Test coverage ≥ 80% lines cho `PostureConfigServiceImpl`'s new methods
- [ ] Không có business logic trong `AdminPostureConfigController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `PAC-TC-SEC-001` (CONTENT_ADMIN denied) passes — this is the primary regression guard for ADR-PAC-001
- [ ] `PAC-TC-THRESH-003/004/007` (out-of-range rejection at DTO + DB layer) pass — BR-SAFETY exit gate
- [ ] `PAC-TC-REGRESSION-001` stays green throughout implementation — confirms `getActiveConfig()` untouched

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả NEW-method tests FAIL với throw stub before implementation; `PAC-TC-REGRESSION-001` stays green
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — every test uses `PostureConfigTestFactory.makeX()`, no shared mutable fixtures outside `@Test`
- [ ] **Oracle Source** — every expected value traces to an ADR/BR/TDS section (see each TC's "Oracle Source" field)

### Suspension Criteria

- Migration `V20260707130000` fails on staging
- Discovery of existing production data in `posture_analysis_configs` that would violate the new `CHECK` constraint (would require a data-cleanup step before migration — currently confirmed zero rows exist)
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE posture_analysis_configs DROP CONSTRAINT IF EXISTS chk_posture_config_confidence_threshold;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707130000';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/exercise/
git checkout -- src/main/resources/db/migration/V20260707130000__add_posture_config_confidence_threshold_check.sql
git checkout -- src/test/java/com/carebridge/backend/exercise/
git checkout -- 05_Development/CareBridgeWebApp/src/features/postureConfiguration/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu có)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `CONTENT_ADMIN` role instead of `SYSTEM_ADMIN` (would silently invalidate `PAC-TC-SEC-001`) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies business logic (e.g., supersede logic) inside `AdminPostureConfigControllerTest` instead of `PostureConfigServiceTest` | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a service/type not in TDS §8, or asserts against a mutated `getActiveConfig()` | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Spec v1.0 — UC186 Manage Posture Analysis Configuration.*
*Status: Draft — pending review and explicit "Approved" per `.claude/rules/implement-flow.md`. No production code or test code has been written for this feature yet.*
