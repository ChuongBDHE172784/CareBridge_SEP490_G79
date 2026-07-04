# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC185 — Manage Pregnancy Exercises — Test Specification

**Document ID:** `CB-EXERCISE-TDD-185`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `N/A — no PII processed`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`pregnancy_exercises` L1196-1212, `audit_logs` L31)
- `04_Implement/UC185_ManagePregnancyExercises/UC185_ManagePregnancyExercises_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16/§17 of it)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.6.1 — UC-185 functional requirements
- `04_Implement/UC29_ViewAndSelectPregnancyExercise/UC29_ViewAndSelectPregnancyExercise_TDS.md` — read-side precedent (existing, unchanged)
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data. Data Classification của module này là `Internal` (không có PII), nhưng convention SYNTHETIC-only vẫn được áp dụng nhất quán.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent` | Khởi tạo tài liệu — Test-Spec cho UC185, khớp với TDS `CB-EXERCISE-IMP-ADMIN-001` v1.0 |
| `2026-07-04` | `AI Agent` | Approved by user — proceeding to implementation |
| `2026-07-04` | `AI Agent` | Implemented and verified: 28/28 backend tests GREEN via `./mvnw test` — `AdminExerciseServiceTest` (11: create-defaults-DRAFT, update happy path + versionNo bump, null-safetyWarning-unchanged, **blank-safetyWarning-rejected EX-ADMIN-002 CRITICAL**, not-found EX-001, activate DRAFT→PUBLISHED + idempotent no-op, disable PUBLISHED→ARCHIVED, getById, list-all-statuses), `AdminExerciseControllerSecurityTest` (13: MOTHER denied x4 endpoints, SYSTEM_ADMIN denied on create — confirms `CONTENT_ADMIN`-only per ADR-EXERCISE-ADMIN-001, no-JWT 401 x2, CONTENT_ADMIN succeeds x6 positive controls), `AdminExerciseControllerTest` (4: durationMinutes boundary >180/<1 → 400, missing safetyWarning on create → 400, blank safetyWarning on update → 400 EX-ADMIN-002 at HTTP layer). Web component tests (form/list) not started — Admin Portal UI out of scope this session. Full pre-existing `exercise` package regression suite reran green (see UC186/UC30 docs for shared regression run). |

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
| **Feature / Gap ID** | `GAP-UC185` |
| **Module** | `Exercise — Manage Pregnancy Exercises (Admin CRUD)` |
| **Spec gốc** | `CB-EXERCISE-IMP-ADMIN-001` |
| **Priority** | 🟠 P1 (content-safety adjacent via `safety_warning` protection), backing feature is `Medium` per SRS |
| **Sprint** | Current sprint (per TDS `Last Verified 2026-07-03`) |
| **Milestone** | Admin Portal — content management screens |
| **Data Classification** | `Internal` (curated content, no PII) |
| **Compliance Scope** | `BR-RBAC` only; `N/A` for PDPA/consent |
| **Upstream Dependencies** | `PregnancyExercise` entity (existing), `ExerciseRepository` (existing, extended), `ExerciseMapper` (existing, extended), `AuditService`/`AuditAction` (existing, extended), `Role.CONTENT_ADMIN` (existing) |
| **Downstream Consumers** | `UC29 — View and Select Pregnancy Exercise` (Mother, reads `status=PUBLISHED` only), `UC177 — View Pregnancy Exercise Detail`, `UC178 — Complete Pre-Exercise Safety Check` (reads `safety_warning`), `UC186 — Manage Posture Analysis Configuration` (references `exercise_id`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXERCISE-IMP-ADMIN-001 §17` (C1-C7, ADR-EXERCISE-ADMIN-001..004) |
| **Constraints Injected** | `CONTENT_ADMIN`-only RBAC (C1); `safetyWarning` null=unchanged/blank=reject (C2); reuse existing entity/repo/mapper, no duplicate mapping (C3); idempotent activate/disable (C4); mandatory audit log per mutation (C5); no hard delete (C6); `durationMinutes` 1-180 bound (C7) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

### 1.2 Pre-Existing Code Scope Note (mirrors UC117/UC168/UC169 partial-code pattern)

> **Important distinction — read this before writing any test:** UC185's TDS §1 confirms the **read side already exists and is fully implemented** (`PregnancyExercise` entity, `ExerciseRepository.findByExerciseIdAndStatus`/`findPublishedByFilters`, `ExerciseMapper.toSummaryResponse`/`toDetailResponse`, `ExerciseNotFoundException`), consumed by UC29/UC177/UC178-184. **None of that existing read-side code is in scope for this Test-Spec.** Tests here cover **only the NEW admin-CRUD surface**: `AdminExerciseController`, `IAdminExerciseService`/`AdminExerciseServiceImpl`, `CreateExerciseRequest`/`UpdateExerciseRequest`/`AdminExerciseResponse`, `InvalidExerciseStateException`, plus the two **extension points** on existing files (`ExerciseRepository.findAllByFilters`, `ExerciseMapper.toAdminResponse/toEntity/applyUpdate`) and the 4 new `AuditAction` enum constants. The Red Gate stub (§5.1) is scoped accordingly — it throws only from the new admin service methods, never from the existing read-side methods, which must continue to pass their own UC29/UC177 tests unmodified throughout this feature's Red→Green cycle.

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-185 Exception E2 says "Invalid, missing, expired, or conflicting data is rejected" without specifying `safetyWarning`-specific semantics | TDS ADR-EXERCISE-ADMIN-004 defines an **asymmetric** rule: `safetyWarning` is the only `UpdateExerciseRequest` field with "null=unchanged / blank=reject" semantics; all other fields are standard full-replace `PUT` | Dedicated test group (EX-ADMIN-TC-006/007/008) isolates this field's 3-way branching (null / blank / non-blank) separately from generic full-replace field tests |
| L2 | TDS §17.1 C7 flags `durationMinutes` bound (1-180) as `Open` — "exact business-approved bound not in SRS" | Column is `smallint` (very wide range); TDS derives 1-180 as a reasonable domain default, not an SRS-confirmed number | Boundary tests (EX-ADMIN-TC-004) assert exactly the TDS-documented bounds (1 and 180 inclusive, 0 and 181 rejected) but this Test-Spec explicitly flags the **business-approval gap** as an Open item (see §6 Suspension Criteria) — do not silently harden/change the bound without a Tech Lead-approved ADR update |
| L3 | TDS §6.3 Case 3 documents idempotent activate/disable but does not specify whether `version_no` increments on a no-op idempotent call | TDS Invariant §6.4 note 3: "Every state transition and content update must increment `version_no` by exactly 1 (**except idempotent no-op activate/disable calls, which do not increment**)" | EX-ADMIN-TC-010/011 explicitly assert `versionNo` is UNCHANGED on a no-op idempotent activate/disable call, distinct from the version-bump assertion on a genuine transition |
| L4 | TDS §16 Authorization Matrix excludes `SYSTEM_ADMIN` from all admin exercise CRUD endpoints, flagged `Open` for later confirmation | No SRS Secondary Actor named for UC-185; matches `AdminContentController` precedent (`CONTENT_ADMIN`-only, no `SYSTEM_ADMIN` override) | EX-ADMIN-TC-SEC-002 explicitly tests `SYSTEM_ADMIN` role is REJECTED (403) — this locks in the current TDS decision; if the Open item is resolved differently later, this test and the TDS §16 matrix must be updated together, not independently |
| L5 | `CreateExerciseRequest.safetyWarning` is `@NotBlank` (required on create per §8.1), but `UpdateExerciseRequest.safetyWarning` is nullable-with-special-semantics — these are two different validation regimes on the "same" field name | Confirmed intentional per ADR-EXERCISE-ADMIN-004 (§3, Option A) — creation requires an initial warning; update allows omission for "no change" | Create tests (EX-ADMIN-TC-001, TC-005) assert `safetyWarning` is `@NotBlank`-enforced on `POST`; Update tests (EX-ADMIN-TC-006/007/008) assert the distinct 3-way rule on `PUT` — never conflated into one shared validator test |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Exercise.AdminCRUD module bao gồm các layer (NEW code only — xem §1.2):
├── DTO Validation (CreateExerciseRequest / UpdateExerciseRequest — Bean Validation unit tests)
├── Services (AdminExerciseServiceImpl — mock ExerciseRepository/ExerciseMapper/AuditService với Mockito)
├── Controller (AdminExerciseController — mock IAdminExerciseService với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real pregnancy_exercises/audit_logs rows, full CRUD lifecycle)
├── Security (RBAC matrix — all 7 roles × 6 endpoints)
└── Web Frontend (React Testing Library + Vitest — Admin Portal Exercise management form/list)

KHÔNG trong scope (existing, unchanged, đã có test coverage riêng dưới UC29/UC177):
├── ExerciseRepository.findByExerciseIdAndStatus / findPublishedByFilters
├── ExerciseMapper.toSummaryResponse / toDetailResponse
└── Mother-facing GET /api/v1/exercises, GET /api/v1/exercises/{id}
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-185 §3.2.6.1` Normal Flow Step 3-4 | Create/update/activate/disable happy paths |
| `SRS UC-185 Exception E1` | Access-denied (unauthenticated/unauthorized) |
| `SRS UC-185 Exception E2` | Field-level validation rejection |
| `SRS UC-185 Postcondition POST-3` | Audit log completeness for every mutation |
| `ADR-EXERCISE-ADMIN-001` | RBAC — `CONTENT_ADMIN` only, no other role |
| `ADR-EXERCISE-ADMIN-002` | Status-based activate/disable, idempotency, no hard delete |
| `ADR-EXERCISE-ADMIN-003` | Audit log emission per mutation type (4 distinct `AuditAction`) |
| `ADR-EXERCISE-ADMIN-004` | `safetyWarning` null=unchanged/blank=reject asymmetric rule |
| `TDS §8/§9/§10` | Service/repository/controller contracts, request/response shapes, error codes |
| `TDS §16` | Full 7-role × 6-endpoint authorization matrix |
| `V1__init_schema.sql` L1196-1212 | Exact column types/nullability driving fixtures and boundary tests |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | CONTENT_ADMIN creates a valid exercise | `AdminExerciseServiceImpl.create()` | `EX-ADMIN-TC-001` |
| TC-COND-002 | Create with missing required field (`title`) | `CreateExerciseRequest` Bean Validation | `EX-ADMIN-TC-002` |
| TC-COND-003 | Create with missing `safetyWarning` | `CreateExerciseRequest` Bean Validation | `EX-ADMIN-TC-005` |
| TC-COND-004 | `durationMinutes` boundary (0, 1, 180, 181) | `CreateExerciseRequest` `@Min(1) @Max(180)` | `EX-ADMIN-TC-004` |
| TC-COND-005 | CONTENT_ADMIN updates content fields (full-replace, non-safetyWarning fields) | `AdminExerciseServiceImpl.update()` | `EX-ADMIN-TC-003` |
| TC-COND-006 | Update with `safetyWarning=null` (omitted) | `AdminExerciseServiceImpl.update()` — ADR-004 "unchanged" branch | `EX-ADMIN-TC-006` |
| TC-COND-007 | Update with `safetyWarning=""` (explicit blank) | `AdminExerciseServiceImpl.update()` — ADR-004 "reject" branch | `EX-ADMIN-TC-007` |
| TC-COND-008 | Update with `safetyWarning="<non-blank>"` | `AdminExerciseServiceImpl.update()` — ADR-004 "apply" branch | `EX-ADMIN-TC-008` |
| TC-COND-009 | Update on unknown `exerciseId` | `AdminExerciseServiceImpl.update()` — EX-001 | `EX-ADMIN-TC-009` |
| TC-COND-010 | Activate a `DRAFT` exercise (genuine transition) | `AdminExerciseServiceImpl.activate()` | `EX-ADMIN-TC-010` |
| TC-COND-011 | Activate an already-`PUBLISHED` exercise (idempotent no-op) | `AdminExerciseServiceImpl.activate()` — ADR-002 idempotency | `EX-ADMIN-TC-011` |
| TC-COND-012 | Disable a `PUBLISHED` exercise (genuine transition) | `AdminExerciseServiceImpl.disable()` | `EX-ADMIN-TC-012` |
| TC-COND-013 | Disable an already-`ARCHIVED` exercise (idempotent no-op) | `AdminExerciseServiceImpl.disable()` — ADR-002 idempotency | `EX-ADMIN-TC-013` |
| TC-COND-014 | Activate/disable on unknown `exerciseId` | `AdminExerciseServiceImpl.activate()/disable()` — EX-001 | `EX-ADMIN-TC-014` |
| TC-COND-015 | `ARCHIVED` → `PUBLISHED` re-activation (state machine edge) | `AdminExerciseServiceImpl.activate()` — §6.4 state machine | `EX-ADMIN-TC-015` |
| TC-COND-016 | `DRAFT` → `ARCHIVED` direct retirement without publishing | `AdminExerciseServiceImpl.disable()` — §6.4 state machine | `EX-ADMIN-TC-016` |
| TC-COND-017 | List all exercises regardless of status, with filters | `AdminExerciseServiceImpl.list()` / `ExerciseRepository.findAllByFilters()` | `EX-ADMIN-TC-017` |
| TC-COND-018 | Get single exercise by ID regardless of status | `AdminExerciseServiceImpl.getById()` | `EX-ADMIN-TC-018` |
| TC-COND-019 | No `DELETE` endpoint/repository method exists | Contract-existence check (C6) | `EX-ADMIN-TC-019` |
| TC-COND-020 | Every successful mutation writes exactly 1 `audit_logs` row with correct `AuditAction` | `AdminExerciseServiceImpl` → `AuditService.log()` | `EX-ADMIN-TC-020`, `EX-ADMIN-TC-INT-002` |
| TC-COND-021 | Non-`CONTENT_ADMIN` roles (MOTHER, FAMILY, EXPERT, MODERATOR, SYSTEM_ADMIN, PARTNER) denied on all 6 endpoints | Controller `@PreAuthorize` / Spring Security | `EX-ADMIN-TC-SEC-001`, `EX-ADMIN-TC-SEC-002` |
| TC-COND-022 | Unauthenticated request (no/invalid JWT) | Spring Security filter chain | `EX-ADMIN-TC-SEC-003` |
| TC-COND-023 | Disabling an exercise takes immediate effect on Mother-facing read side (UC29) | `ExerciseRepository.findPublishedByFilters` (existing, read-only verification) | `EX-ADMIN-TC-INT-001` |
| TC-COND-024 | `create()` never invokes `ExerciseRepository.delete*` (no hard-delete escape hatch, defensive) | Mockito `verifyNoInteractions`/`verify(never())` | `EX-ADMIN-TC-019` |
| TC-COND-025 | Web: Admin exercise form renders validation error when `safetyWarning` submitted blank on update | `ExerciseAdminForm.tsx` | `EX-ADMIN-WEB-TC-001` |
| TC-COND-026 | Web: Admin exercise list renders all statuses with activate/disable action buttons | `ExerciseAdminListPage.tsx` | `EX-ADMIN-WEB-TC-002` |
| TC-COND-027 | Web: Admin exercise form renders 403 error state for non-CONTENT_ADMIN session | `ExerciseAdminForm.tsx` | `EX-ADMIN-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | 7 roles × RBAC outcome (CONTENT_ADMIN=allow, all others=deny) | Confirms C1/§16 matrix exhaustively without combinatorial explosion |
| Boundary Value Analysis | `durationMinutes` (0, 1, 180, 181); `safetyWarning` (null / "" / non-blank) | Both are schema/DTO-derived boundaries directly cited in TDS §8.1/§17 C2/C7 |
| State Transition Testing | `DRAFT`→`PUBLISHED`→`ARCHIVED`→`PUBLISHED` full cycle per §6.4 state machine | Exhaustively covers every documented transition + idempotent no-op edges |
| Decision Table Testing | `safetyWarning` 3-way branch (null/blank/non-blank) × (existing value present/absent) | ADR-EXERCISE-ADMIN-004's asymmetric rule is a decision-table-shaped requirement |
| Error Guessing | Contract-existence check for absent `DELETE`; extraneous fields in request body; idempotent-call version-bump confusion | Highest-risk items per TDS §17.4 anti-pattern table (AP-AI-003 "adds a delete endpoint") |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DTO | `CreateExerciseRequest{title:"Pelvic Tilt Stretch", trimesterScope:SECOND, difficultyLevel:EASY, durationMinutes:10, safetyWarning:"Stop if dizzy or in pain.", supportsPostureAnalysis:true}` | Happy path create |
| `FX-002` | DB seed | `PregnancyExercise{status:DRAFT, versionNo:1, safetyWarning:"Original warning text."}` | Update/activate/disable base fixture |
| `FX-003` | DB seed | `PregnancyExercise{status:PUBLISHED, versionNo:2}` | Idempotent-activate + disable-transition fixture |
| `FX-004` | DB seed | `PregnancyExercise{status:ARCHIVED, versionNo:3}` | Idempotent-disable + re-activate fixture |
| `FX-005` | JWT | `{ sub: admin-user-id, role: 'CONTENT_ADMIN' }` | Auth context for allowed requests |
| `FX-006` | JWT (×6) | One JWT per non-`CONTENT_ADMIN` role: `MOTHER`, `FAMILY`, `EXPERT`, `MODERATOR`, `SYSTEM_ADMIN`, `PARTNER` | RBAC denial matrix |
| `FX-007` | DTO | `UpdateExerciseRequest{..., safetyWarning:null}` | "unchanged" branch |
| `FX-008` | DTO | `UpdateExerciseRequest{..., safetyWarning:""}` | "reject" branch |
| `FX-009` | DTO | `UpdateExerciseRequest{..., safetyWarning:"New warning."}` | "apply" branch |
| `FX-010` | DB seed | `PregnancyExercise{status:PUBLISHED}` used by `UC29`'s `findPublishedByFilters` read query | Cross-UC read-side-effect verification (TC-COND-023) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// AdminExerciseTestFactory.java — mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class AdminExerciseTestFactory {

    static CreateExerciseRequest makeCreateRequest() {
        CreateExerciseRequest r = new CreateExerciseRequest();
        r.setTitle("Pelvic Tilt Stretch");
        r.setDescription("Gentle stretch to relieve lower back tension.");
        r.setTrimesterScope(TrimesterScope.SECOND);
        r.setDifficultyLevel(DifficultyLevel.EASY);
        r.setDurationMinutes((short) 10);
        r.setInstructionContent("1. Kneel on all fours...\n2. Arch and round your back slowly...");
        r.setMediaUrl("https://cdn.carebridge.dev/exercises/pelvic-tilt.mp4");
        r.setSafetyWarning("Stop immediately if you feel dizziness or sharp pain.");
        r.setSupportsPostureAnalysis(true);
        return r;
    }

    static CreateExerciseRequest makeCreateRequest(Consumer<CreateExerciseRequest> overrides) {
        CreateExerciseRequest r = makeCreateRequest();
        overrides.accept(r);
        return r;
    }

    static UpdateExerciseRequest makeUpdateRequest(Consumer<UpdateExerciseRequest> overrides) {
        UpdateExerciseRequest r = new UpdateExerciseRequest();
        r.setTitle("Pelvic Tilt Stretch (Updated)");
        r.setDescription("Revised instructions.");
        r.setTrimesterScope(TrimesterScope.SECOND);
        r.setDifficultyLevel(DifficultyLevel.EASY);
        r.setDurationMinutes((short) 12);
        r.setInstructionContent("1. Kneel on all fours...\n2. Arch and round your back slowly (revised)...");
        r.setMediaUrl("https://cdn.carebridge.dev/exercises/pelvic-tilt-v2.mp4");
        r.setSafetyWarning(null); // default: "unchanged" branch, override per test
        r.setSupportsPostureAnalysis(true);
        overrides.accept(r);
        return r;
    }

    static PregnancyExercise makeExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(UUID.randomUUID());
        e.setCreatedBy(UUID.randomUUID());
        e.setTitle("Pelvic Tilt Stretch");
        e.setDescription("Gentle stretch to relieve lower back tension.");
        e.setTrimesterScope(TrimesterScope.SECOND);
        e.setDifficultyLevel(DifficultyLevel.EASY);
        e.setDurationMinutes((short) 10);
        e.setInstructionContent("1. Kneel on all fours...");
        e.setMediaUrl("https://cdn.carebridge.dev/exercises/pelvic-tilt.mp4");
        e.setSafetyWarning("Original warning text.");
        e.setSupportsPostureAnalysis(true);
        e.setStatus(ExerciseStatus.DRAFT);
        e.setVersionNo(1);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        overrides.accept(e);
        return e;
    }

    static UUID makeAdminUserId() {
        return UUID.randomUUID();
    }
}
```

---

### EX-ADMIN-TC-001 — CONTENT_ADMIN creates a valid exercise, defaults to DRAFT

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.create()`
**Test File:** `src/test/java/com/carebridge/backend/exercise/service/AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §6.1 Sequence Diagram — Create`, `TDS §9.2 POST response example`, `ADR-EXERCISE-ADMIN-002`

**Preconditions:** FX-001.

**Test Steps:**
1. Mock `ExerciseMapper.toEntity(request, adminUserId)` to return an entity with `status=DRAFT`, `versionNo=1`.
2. Mock `ExerciseRepository.save(entity)` to return the same entity.
3. Call `adminExerciseService.create(FX-001, adminUserId)`.
4. Assert returned `AdminExerciseResponse.status == "DRAFT"` and `versionNo == 1`.
5. Verify `AuditService.log(EXERCISE_CREATED, adminUserId, "PregnancyExercise", exerciseId, ...)` was called exactly once.

**Expected Result (PASS):** New exercise persisted as `DRAFT`, audit row emitted.
**Expected Result (FAIL):** Status defaults to anything other than `DRAFT`, or audit call missing/wrong action.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-002 — Create with missing required `title` is rejected with EX-ADMIN-001

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateExerciseRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/exercise/dto/CreateExerciseRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.1 @NotBlank @Size(max=255)`, `TDS §10 EX-ADMIN-001`

**Test Steps:**
1. Build `FX-001` via `makeCreateRequest(r -> r.setTitle(null))`.
2. Validate with `jakarta.validation.Validator`.
3. Assert one violation on `title` property.

**Expected Result (PASS):** Validation violation raised, controller-level test (EX-ADMIN-TC-INT-003) confirms `400 EX-ADMIN-001`.
**Expected Result (FAIL):** No violation / silently defaults `title`.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-003 — Update replaces all standard fields (full-replace PUT semantics)

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.update()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §8.1 UpdateExerciseRequest`, `ADR-EXERCISE-ADMIN-004` ("all others are standard full-replace PUT fields")

**Preconditions:** FX-002 (existing DRAFT exercise, versionNo=1).

**Test Steps:**
1. Mock `ExerciseRepository.findById(exerciseId)` → FX-002.
2. Build update request via `makeUpdateRequest(r -> r.setSafetyWarning(null))` (safetyWarning untouched, all other fields new values).
3. Call `update(exerciseId, request, adminUserId)`.
4. Assert `title`, `description`, `trimesterScope`, `difficultyLevel`, `durationMinutes`, `instructionContent`, `mediaUrl`, `supportsPostureAnalysis` all equal the new request values (full replace).
5. Assert `versionNo` incremented from 1 to 2.

**Expected Result (PASS):** All non-safetyWarning fields fully replaced, version+1.
**Expected Result (FAIL):** Any field partially merged instead of replaced, or version not incremented.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-004 — durationMinutes boundary validation (0, 1, 180, 181)

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateExerciseRequest`/`UpdateExerciseRequest` `@Min(1) @Max(180)`
**Test File:** `CreateExerciseRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §8.1`, `TDS §17.1 C7` (flagged `Open` — business-approved bound not SRS-confirmed, see Logic Issue L2)

**Test Steps:**
1. `durationMinutes=0` → assert validation violation (below `@Min(1)`).
2. `durationMinutes=1` → assert NO violation (boundary inclusive).
3. `durationMinutes=180` → assert NO violation (boundary inclusive).
4. `durationMinutes=181` → assert validation violation (above `@Max(180)`).

**Expected Result (PASS):** 0 and 181 rejected; 1 and 180 accepted exactly at the documented bounds.
**Expected Result (FAIL):** Off-by-one on either bound.

**Current Status:** 🔴 Not written
**Note:** Per Logic Issue L2, this bound is TDS-derived, not SRS-confirmed — do not change without an ADR update.

---

### EX-ADMIN-TC-005 — Create with missing safetyWarning is rejected with EX-ADMIN-001

**Severity:** `HIGH`
**Feature Under Test:** `CreateExerciseRequest` `@NotBlank` on `safetyWarning`
**Test File:** `CreateExerciseRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §8.1 "required on create (cannot create without a warning)"`, Logic Issue L5

**Test Steps:**
1. Build `FX-001` via `makeCreateRequest(r -> r.setSafetyWarning(null))`.
2. Validate.
3. Assert violation on `safetyWarning`.
4. Repeat with `r.setSafetyWarning("")` — assert violation too (blank also rejected on create, unlike update's "null=unchanged" nuance which does not apply here since there is no prior value to preserve).

**Expected Result (PASS):** Both null and blank rejected on CREATE.
**Expected Result (FAIL):** Exercise created with no safety warning.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-006 — Update with safetyWarning=null preserves existing value unchanged (CASE 2.0 safety constraint)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminExerciseServiceImpl.update()` — ADR-EXERCISE-ADMIN-004 "unchanged" branch
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-EXERCISE-ADMIN-004` Option A, `TDS §6.2 Sequence Diagram — Update Safety Warning Guard`, `TDS §17.1 C2`

**Preconditions:** FX-002 (`safetyWarning="Original warning text."`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002.
2. Build `FX-007` (`safetyWarning=null`), all other fields changed.
3. Call `update(exerciseId, FX-007, adminUserId)`.
4. Assert resulting entity's `safetyWarning` is STILL `"Original warning text."` (byte-for-byte unchanged).
5. Assert other fields (title, etc.) WERE updated to the new request values.
6. Assert `versionNo` incremented (content changed, even though safetyWarning didn't).

**Expected Result (PASS):** `safetyWarning` untouched; every other field replaced.
**Expected Result (FAIL):** `safetyWarning` becomes null/blank, OR the entire update is incorrectly rejected (null should NOT be an error — only blank is).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the CASE 2.0 explicit safety-preservation test — "warnings field must never be silently blanked" per the task's mandatory constraint.

---

### EX-ADMIN-TC-007 — Update with safetyWarning="" (explicit blank) is rejected with EX-ADMIN-002 (CASE 2.0 safety constraint)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminExerciseServiceImpl.update()` — ADR-EXERCISE-ADMIN-004 "reject" branch
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-EXERCISE-ADMIN-004`, `TDS §9.2 400 Bad Request example (EX-ADMIN-002)`, `TDS §10 EX-ADMIN-002`, `TDS §17.1 C2`

**Preconditions:** FX-002 (`safetyWarning="Original warning text."`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002.
2. Build `FX-008` (`safetyWarning=""`).
3. Call `update(exerciseId, FX-008, adminUserId)`.
4. Assert `InvalidExerciseStateException` thrown with code `EX-ADMIN-002`.
5. Verify `ExerciseRepository.save(...)` was NEVER called (no partial persistence before rejection).
6. Verify `AuditService.log(...)` was NEVER called (rejected request produces no audit trail).

**Expected Result (PASS = safety enforced):** Exception thrown before any persistence; original warning remains in DB (unverified by this unit test but guaranteed by "save never called").
**Expected Result (FAIL = safety violated):** `safetyWarning` silently blanked, OR exception thrown but `save()` was still called with the blanked value (partial-persistence bug).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary CASE 2.0 "warnings field must never be silently blanked" test explicitly requested by the task brief.

---

### EX-ADMIN-TC-008 — Update with safetyWarning="<non-blank text>" applies the new value

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.update()` — ADR-EXERCISE-ADMIN-004 "apply" branch
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-EXERCISE-ADMIN-004`

**Preconditions:** FX-002 (`safetyWarning="Original warning text."`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002.
2. Build `FX-009` (`safetyWarning="New warning."`).
3. Call `update(exerciseId, FX-009, adminUserId)`.
4. Assert entity's `safetyWarning == "New warning."`.
5. Assert `versionNo` incremented.

**Expected Result (PASS):** New non-blank value fully applied.
**Expected Result (FAIL):** Old value retained despite an explicit deliberate change, or update rejected.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-009 — Update on unknown exerciseId throws EX-001

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminExerciseServiceImpl.update()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §8.1 "@throws ExerciseNotFoundException (EX-001)"`, `TDS §10`

**Test Steps:**
1. Mock `findById(unknownId)` → `Optional.empty()`.
2. Call `update(unknownId, anyRequest, adminUserId)`.
3. Assert `ExerciseNotFoundException` thrown (reused existing exception, per C3).

**Expected Result (PASS):** `EX-001` surfaced, `ExerciseNotFoundException` (existing class) reused, not a new duplicate exception type.
**Expected Result (FAIL):** Wrong/no exception, or a new exception class invented (AP-AI-005 violation).

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-010 — Activate a DRAFT exercise transitions to PUBLISHED and increments version

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.activate()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §6.4 State Machine (DRAFT → PUBLISHED)`, `ADR-EXERCISE-ADMIN-002`

**Preconditions:** FX-002 (`status=DRAFT`, `versionNo=1`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002.
2. Call `activate(exerciseId, adminUserId)`.
3. Assert resulting `status == "PUBLISHED"`.
4. Assert `versionNo` incremented from 1 to 2 (genuine transition, per Invariant §6.4 note 3).
5. Verify `AuditService.log(EXERCISE_ACTIVATED, ...)` called once.

**Expected Result (PASS):** Status transitions, version bumps, audit emitted.
**Expected Result (FAIL):** Wrong target status, version not bumped, or no audit call.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-011 — Idempotent activate on already-PUBLISHED exercise returns 200 unchanged, no version bump

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminExerciseServiceImpl.activate()` — idempotency (Logic Issue L3)
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §6.3 Case 3`, `TDS §6.4 Invariant note 3 ("except idempotent no-op ... which do not increment")`, `TDS §17.1 C4`

**Preconditions:** FX-003 (`status=PUBLISHED`, `versionNo=2`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-003.
2. Call `activate(exerciseId, adminUserId)`.
3. Assert `status == "PUBLISHED"` (unchanged, no error).
4. Assert `versionNo` STILL `2` (NOT bumped — this is the specific behavior this test guards).
5. Decide/assert per TDS §6.3 whether `save()`/audit is invoked for a no-op (TDS sequence diagram shows the no-op still returns a response without describing a skipped save — assert `ExerciseRepository.save()` is NOT called for a true no-op, consistent with "no version bump" implying no persistence write).

**Expected Result (PASS):** No exception, unchanged status/version, idempotent per C4.
**Expected Result (FAIL):** Exception thrown for already-published exercise, OR version incorrectly bumped on a no-op call.

**Current Status:** 🔴 Not written
**Open Item:** TDS does not explicitly state whether a no-op idempotent call still writes an audit log row — flagged in §6 Suspension Criteria; this test asserts the conservative interpretation (no redundant audit/save on true no-op) pending Tech Lead confirmation.

---

### EX-ADMIN-TC-012 — Disable a PUBLISHED exercise transitions to ARCHIVED and increments version

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.disable()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §6.4 State Machine (PUBLISHED → ARCHIVED)`, `ADR-EXERCISE-ADMIN-002`

**Preconditions:** FX-003 (`status=PUBLISHED`, `versionNo=2`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-003.
2. Call `disable(exerciseId, adminUserId)`.
3. Assert `status == "ARCHIVED"`, `versionNo == 3`.
4. Verify `AuditService.log(EXERCISE_DISABLED, ...)` called once.

**Expected Result (PASS):** Transition + version bump + audit as documented.
**Expected Result (FAIL):** Any mismatch.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-013 — Idempotent disable on already-ARCHIVED exercise returns 200 unchanged, no version bump

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminExerciseServiceImpl.disable()` — idempotency
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §17.1 C4`, `TDS §6.4 Invariant note 3`

**Preconditions:** FX-004 (`status=ARCHIVED`, `versionNo=3`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-004.
2. Call `disable(exerciseId, adminUserId)`.
3. Assert `status == "ARCHIVED"` (unchanged), `versionNo` STILL `3`.

**Expected Result (PASS):** No-op success, version untouched.
**Expected Result (FAIL):** Version bumped on a true no-op.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-014 — Activate/disable on unknown exerciseId throws EX-001

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminExerciseServiceImpl.activate()`/`disable()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §6.3 Case 2`, `TDS §8.1`

**Test Steps:**
1. Mock `findById(unknownId)` → `Optional.empty()`.
2. Call `activate(unknownId, adminUserId)` → assert `ExerciseNotFoundException`.
3. Repeat for `disable(unknownId, adminUserId)` → assert `ExerciseNotFoundException`.

**Expected Result (PASS):** Both throw `EX-001` consistently.
**Expected Result (FAIL):** Either method behaves differently or swallows the error.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-015 — ARCHIVED exercise can be re-activated to PUBLISHED (state machine edge)

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminExerciseServiceImpl.activate()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §6.4 State Machine ("ARCHIVED --> PUBLISHED: re-activates a retired exercise")`

**Preconditions:** FX-004 (`status=ARCHIVED`, `versionNo=3`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-004.
2. Call `activate(exerciseId, adminUserId)`.
3. Assert `status == "PUBLISHED"`, `versionNo == 4` (genuine transition since source status differs from target).

**Expected Result (PASS):** Re-activation from ARCHIVED works exactly like DRAFT→PUBLISHED.
**Expected Result (FAIL):** Re-activation blocked or treated incorrectly as a no-op.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-016 — DRAFT exercise can be directly retired to ARCHIVED without publishing (state machine edge)

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminExerciseServiceImpl.disable()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS §6.4 State Machine ("DRAFT --> ARCHIVED: retires a draft without publishing")`

**Preconditions:** FX-002 (`status=DRAFT`, `versionNo=1`).

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002.
2. Call `disable(exerciseId, adminUserId)`.
3. Assert `status == "ARCHIVED"`, `versionNo == 2`.

**Expected Result (PASS):** Direct DRAFT→ARCHIVED transition succeeds without requiring an intermediate PUBLISHED state.
**Expected Result (FAIL):** Transition rejected or requires publish-first.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-017 — List returns all exercises regardless of status, with optional filters

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminExerciseServiceImpl.list()` / `ExerciseRepository.findAllByFilters()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `TDS §8.2 findAllByFilters (status filter OPTIONAL, unlike read-side)`

**Test Steps:**
1. Mock `ExerciseRepository.findAllByFilters(null, null, null, pageable)` to return a `Page` containing one `DRAFT`, one `PUBLISHED`, one `ARCHIVED` exercise.
2. Call `list(null, null, null, 0, 20)`.
3. Assert all 3 statuses present in the returned page (unlike Mother-facing `findPublishedByFilters`, which would only return `PUBLISHED`).

**Expected Result (PASS):** Admin list is status-unrestricted by default.
**Expected Result (FAIL):** List silently filters to `PUBLISHED` only (would indicate accidental reuse of the Mother-facing query — AP-AI-003 violation).

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-018 — Get single exercise by ID regardless of status

**Severity:** `LOW`
**Feature Under Test:** `AdminExerciseServiceImpl.getById()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS §8.1 "Retrieves a single exercise regardless of status (admin view)"`

**Test Steps:**
1. Mock `findById(exerciseId)` → FX-002 (`status=DRAFT`).
2. Call `getById(exerciseId)`.
3. Assert response returned even though status is `DRAFT` (not `PUBLISHED`) — admin view is not status-gated, unlike `findByExerciseIdAndStatus(id, PUBLISHED)` used by UC177.

**Expected Result (PASS):** DRAFT exercise visible to admin.
**Expected Result (FAIL):** `ExerciseNotFoundException` incorrectly thrown for a non-PUBLISHED exercise (would indicate accidental reuse of the Mother-facing status-scoped query).

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-019 — No hard-delete capability exists (contract-existence + defensive check)

**Severity:** `CRITICAL`
**Feature Under Test:** `IAdminExerciseService` contract + `ExerciseRepository` contract
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`, `TC-COND-024`
**Oracle Source:** `TDS §17.1 C6`, `ADR-EXERCISE-ADMIN-002`, `TDS §17.4 AP-AI-003`

**Test Steps:**
1. **Contract check:** Reflectively assert `IAdminExerciseService` interface declares NO method named `delete`/`remove`/`purge` (case-insensitive substring match).
2. **Contract check:** Reflectively assert `AdminExerciseController` declares no `@DeleteMapping` annotation anywhere in the class.
3. **Behavioral check:** During `create()`/`update()`/`activate()`/`disable()` unit tests, verify `ExerciseRepository.delete(any())`/`deleteById(any())` is NEVER invoked (Mockito `verify(exerciseRepository, never()).delete(any())` / `deleteById(any())`).

**Expected Result (PASS):** No delete surface exists anywhere in the new admin code.
**Expected Result (FAIL):** Any `delete`-shaped method found on the service interface, or a `@DeleteMapping` on the controller, or an unexpected repository `delete` call.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-020 — Every mutation type emits exactly one audit_logs row with the correct AuditAction

**Severity:** `HIGH`
**Feature Under Test:** `AdminExerciseServiceImpl.create/update/activate/disable()` → `AuditService.log()`
**Test File:** `AdminExerciseServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `ADR-EXERCISE-ADMIN-003`, `TDS §7.1 Domain Event Catalog`, `SRS UC-185 Postcondition POST-3`

**Test Steps:**
1. For each of the 4 mutation methods (`create`, `update`, `activate` on a genuine transition, `disable` on a genuine transition), invoke with a properly-mocked repository/mapper.
2. Assert `AuditService.log(...)` is called EXACTLY once per call, with the corresponding `AuditAction` constant: `EXERCISE_CREATED`, `EXERCISE_UPDATED`, `EXERCISE_ACTIVATED`, `EXERCISE_DISABLED` respectively.
3. Assert `entity_type == "PregnancyExercise"` and `entity_id == exerciseId` in every call.

**Expected Result (PASS):** 1:1 mapping between mutation type and `AuditAction` constant, matching TDS §7.1 exactly.
**Expected Result (FAIL):** Wrong/missing/duplicate audit call, or generic/incorrect `AuditAction` reused across mutation types (AP-AI-003-adjacent — would violate ADR-EXERCISE-ADMIN-003's rejected Option B).

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EX-ADMIN-TC-SEC-001 — Non-CONTENT_ADMIN roles are denied 403 on all 6 admin exercise endpoints (RBAC matrix, Part 1: MOTHER/FAMILY/EXPERT)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminExerciseController` `@PreAuthorize("hasRole('CONTENT_ADMIN')")`
**Test File:** `src/test/java/com/carebridge/backend/exercise/controller/AdminExerciseControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §16 Authorization Matrix`, `ADR-EXERCISE-ADMIN-001`

**Preconditions:** JWTs for `MOTHER`, `FAMILY`, `EXPERT` (subset of FX-006).

**Test Steps (parameterized over role × endpoint, 3 roles × 6 endpoints = 18 combinations):**
1. For each role, call each of: `GET /api/v1/admin/exercises`, `GET /api/v1/admin/exercises/{id}`, `POST /api/v1/admin/exercises`, `PUT /api/v1/admin/exercises/{id}`, `PATCH .../activate`, `PATCH .../disable`.
2. Assert HTTP 403 for every combination.
3. Assert `IAdminExerciseService` was never invoked (Mockito `verifyNoInteractions`).

**Expected Result (PASS):** All 18 combinations return 403, service never touched.
**Expected Result (FAIL):** Any combination returns 200/201, or service invoked despite wrong role.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-SEC-002 — Non-CONTENT_ADMIN roles are denied 403 on all 6 admin exercise endpoints (RBAC matrix, Part 2: MODERATOR/SYSTEM_ADMIN/PARTNER)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminExerciseController` `@PreAuthorize("hasRole('CONTENT_ADMIN')")`
**Test File:** `AdminExerciseControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §16 Authorization Matrix ("SYSTEM_ADMIN is deliberately excluded")`, Logic Issue L4

**Preconditions:** JWTs for `MODERATOR`, `SYSTEM_ADMIN`, `PARTNER` (subset of FX-006).

**Test Steps (parameterized, 3 roles × 6 endpoints = 18 combinations):**
1. Same as EX-ADMIN-TC-SEC-001, split into a second test class/method for `MODERATOR`, `SYSTEM_ADMIN`, `PARTNER`.
2. Special emphasis on `SYSTEM_ADMIN`: explicitly assert 403 even though `SYSTEM_ADMIN` typically has elevated privileges elsewhere in the system — this UC deliberately has NO admin override (§16 note).

**Expected Result (PASS):** All 18 combinations return 403, including `SYSTEM_ADMIN`.
**Expected Result (FAIL):** `SYSTEM_ADMIN` (or any other role) incorrectly allowed through — this would silently contradict the TDS §16 documented decision without a corresponding ADR update.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-SEC-003 — Unauthenticated request rejected with 401 before reaching controller logic

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain for `/api/v1/admin/exercises/**`
**Test File:** `AdminExerciseControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `TDS §15.2 "No JWT -> 401"`, `TDS §9.1 Auth Level: JWT Bearer`

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/exercises"))` with no `Authorization` header.
2. Assert HTTP 401 and body `error.code == "IAM-001"`.
3. Repeat for `POST /api/v1/admin/exercises` with no header.

**Expected Result (PASS):** 401 consistently for all endpoints, `IAM-001`.
**Expected Result (FAIL):** Any endpoint reachable without auth, or NPE from missing principal.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EX-ADMIN-TC-INT-001 — Disabling a PUBLISHED exercise immediately hides it from Mother-facing UC29 read side (Testcontainers)

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `AdminExerciseController.disable()` → DB write → existing `ExerciseRepository.findPublishedByFilters()` (UC29's read query, unmodified)
**Test File:** `src/test/java/com/carebridge/backend/exercise/integration/AdminExerciseCrossUcIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `ADR-EXERCISE-ADMIN-002 Option A ("read-side already filters on status=PUBLISHED so activate/disable takes effect immediately")`, `TDS §6.4 Invariant #2`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied.
- Seed FX-010 (`status=PUBLISHED`) directly via JPA/JdbcTemplate.

**Test Steps:**
1. `GET /api/v1/exercises` (existing Mother-facing endpoint, UC29) as a MOTHER — assert FX-010's `exerciseId` IS present in results.
2. `PATCH /api/v1/admin/exercises/{FX-010 id}/disable` as CONTENT_ADMIN — assert 200, `status=ARCHIVED`.
3. Re-call `GET /api/v1/exercises` as MOTHER — assert FX-010's `exerciseId` is NO LONGER present.
4. Assert this required ZERO changes to `ExerciseRepository.findPublishedByFilters()` (proven implicitly — that method is not touched by this feature per §1.2 scope note).

**Expected Result (PASS):** Disable takes immediate cross-UC effect without any read-side code changes.
**Expected Result (FAIL):** Exercise still visible to Mother after disable, or read-side query needed modification (would indicate ADR-EXERCISE-ADMIN-002 assumption was wrong).

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-INT-002 — Full CRUD lifecycle produces correct audit_logs rows in order (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `AdminExerciseController` → `AdminExerciseServiceImpl` → `ExerciseRepository`/`AuditService` → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/exercise/integration/AdminExerciseControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:** PostgreSQL Testcontainer, Flyway applied, CONTENT_ADMIN JWT (FX-005).

**Test Steps:**
1. `POST /api/v1/admin/exercises` (FX-001) → capture `exerciseId`.
2. `PUT /api/v1/admin/exercises/{id}` with a valid update.
3. `PATCH /api/v1/admin/exercises/{id}/activate`.
4. `PATCH /api/v1/admin/exercises/{id}/disable`.
5. Query `SELECT action, entity_id FROM audit_logs WHERE entity_type='PregnancyExercise' AND entity_id=? ORDER BY created_at ASC`.
6. Assert exactly 4 rows, in order: `EXERCISE_CREATED`, `EXERCISE_UPDATED`, `EXERCISE_ACTIVATED`, `EXERCISE_DISABLED`.

**Expected Result (PASS):** Exactly 4 audit rows in the documented order.
**Expected Result (FAIL):** Missing row, extra row, or wrong action recorded.

**DB Assertion:**
```java
List<String> actions = jdbcTemplate.queryForList(
    "SELECT action FROM audit_logs WHERE entity_type='PregnancyExercise' AND entity_id=? ORDER BY created_at ASC",
    String.class, exerciseId);
assertThat(actions).containsExactly("EXERCISE_CREATED", "EXERCISE_UPDATED", "EXERCISE_ACTIVATED", "EXERCISE_DISABLED");
```

**Current Status:** 🔴 Not written

---

### EX-ADMIN-TC-INT-003 — Validation and safety-warning-blank errors surface with correct HTTP status and error code end-to-end

**Severity:** `HIGH`
**Feature Under Test:** Full flow: Controller `@Valid` + `AdminExerciseServiceImpl` + `GlobalExceptionHandler`
**Test File:** `AdminExerciseControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`, `TC-COND-007`

**Test Steps:**
1. `POST /api/v1/admin/exercises` with `{}` (empty body) → assert 400, `error.code=="EX-ADMIN-001"`.
2. Create a valid exercise, then `PUT .../{id}` with `safetyWarning:""` → assert 400, `error.code=="EX-ADMIN-002"`.
3. `PATCH .../activate` on a random unknown UUID → assert 404, `error.code=="EX-001"`.

**Expected Result (PASS):** All 3 error paths match TDS §9.2/§10 exactly.
**Expected Result (FAIL):** Wrong status code or error code for any path.

**Current Status:** 🔴 Not written

---

### WEB COMPONENT TEST CASES (Admin Portal — Vitest / React Testing Library)

---

### EX-ADMIN-WEB-TC-001 — Exercise admin form shows validation error and blocks submit when safetyWarning is cleared to blank on edit

**Severity:** `HIGH`
**Feature Under Test:** `ExerciseAdminForm.tsx` (Admin Portal)
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExercise/components/ExerciseAdminForm.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-025`
**Oracle Source:** `TDS §9.2 EX-ADMIN-002 response body`, ADR-EXERCISE-ADMIN-004

**Preconditions:** Component rendered in "edit" mode with an existing exercise that has a non-blank `safetyWarning`.

**Test Steps:**
1. Render `<ExerciseAdminForm mode="edit" initialData={fixture} />`.
2. Clear the "Safety Warning" textarea to an empty string.
3. Click "Save".
4. Assert an inline validation message is shown (e.g., "Safety warning cannot be left blank — leave the field untouched to keep the current warning") and the `PUT` request is NOT sent (mock fetch/axios `not.toHaveBeenCalled()`).

**Expected Result (PASS):** Client-side guard prevents the blank-warning submission before it ever reaches the API (defense in depth alongside the server-side EX-ADMIN-002 check).
**Expected Result (FAIL):** Form submits the blank value, or no user-facing warning shown.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-WEB-TC-002 — Exercise admin list renders all statuses with Activate/Disable action buttons

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseAdminListPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExercise/pages/ExerciseAdminListPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-026`
**Oracle Source:** `TDS §9.1 GET /api/v1/admin/exercises`, `TDS §6.4 State Machine`

**Preconditions:** Mocked `GET /api/v1/admin/exercises` returns 3 rows (`DRAFT`, `PUBLISHED`, `ARCHIVED`).

**Test Steps:**
1. Render `<ExerciseAdminListPage />`.
2. `await screen.findAllByRole('row')` — assert 3 data rows rendered (all statuses visible, not filtered to PUBLISHED only).
3. Assert the `DRAFT` row shows an "Activate" button and a "Disable" button (per state machine: DRAFT can go to either PUBLISHED or ARCHIVED).
4. Assert the `PUBLISHED` row shows a "Disable" button but not an enabled "Activate" button (idempotent no-op UX — button hidden or disabled by convention).

**Expected Result (PASS):** List shows the full unfiltered admin view with status-appropriate actions.
**Expected Result (FAIL):** List silently filtered to PUBLISHED only, or wrong actions shown per status.

**Current Status:** 🔴 Not written

---

### EX-ADMIN-WEB-TC-003 — Exercise admin form renders access-denied UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `ExerciseAdminForm.tsx`
**Test File:** `ExerciseAdminForm.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `TDS §9.2 403 Forbidden example (IAM-003)`

**Test Steps:**
1. Mock `POST /api/v1/admin/exercises` to return 403 `{error:{code:"IAM-003", message:"Insufficient permissions"}}`.
2. Render form, fill valid data, submit.
3. Assert an access-denied/error message is shown, not a crash or silent failure.

**Expected Result (PASS):** Graceful error UI shown.
**Expected Result (FAIL):** Unhandled promise rejection / white screen / silent no-op.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EX-ADMIN-TC-001` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-002` | `CreateExerciseRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-003` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-004` | `CreateExerciseRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-005` | `CreateExerciseRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-006` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-007` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-008` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-009` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-010` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-011` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-012` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-013` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-014` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-015` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-016` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-017` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-018` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-019` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-020` | `AdminExerciseServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-SEC-001` | `AdminExerciseControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-SEC-002` | `AdminExerciseControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-SEC-003` | `AdminExerciseControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-INT-001` | `AdminExerciseCrossUcIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-INT-002` | `AdminExerciseControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-TC-INT-003` | `AdminExerciseControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-WEB-TC-001` | `ExerciseAdminForm.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-WEB-TC-002` | `ExerciseAdminListPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EX-ADMIN-WEB-TC-003` | `ExerciseAdminForm.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> **Scope note (per §1.2):** The stub below covers ONLY the new `AdminExerciseServiceImpl` (admin-CRUD methods). It does **NOT** touch `ExerciseRepository.findByExerciseIdAndStatus`/`findPublishedByFilters` or `ExerciseMapper.toSummaryResponse`/`toDetailResponse`, which are existing, already-implemented, already-tested read-side methods consumed by UC29/UC177. Those methods must remain fully functional (not stubbed) throughout this feature's Red→Green cycle — `EX-ADMIN-TC-INT-001` explicitly depends on the existing `findPublishedByFilters` continuing to work unmodified. The two extension points (`ExerciseRepository.findAllByFilters` — new method on an existing interface; `ExerciseMapper.toAdminResponse/toEntity/applyUpdate` — new methods on an existing class) ARE part of the Red Gate stub since they are net-new code introduced by this feature.

**Stub cho Red Phase:**

```java
@Service
public class AdminExerciseServiceImpl implements IAdminExerciseService {

    @Override
    public AdminExerciseResponse create(CreateExerciseRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AdminExerciseResponse update(UUID exerciseId, UpdateExerciseRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AdminExerciseResponse activate(UUID exerciseId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AdminExerciseResponse disable(UUID exerciseId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AdminExerciseResponse getById(UUID exerciseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public PaginatedResponse<AdminExerciseResponse> list(
            ExerciseStatus status, TrimesterScope trimester, DifficultyLevel difficulty, int page, int size) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Extension stub — new methods only; existing ExerciseRepository query methods (findByExerciseIdAndStatus,
// findPublishedByFilters) are NOT stubbed and continue to function via their existing implementation.
public interface ExerciseRepository extends JpaRepository<PregnancyExercise, UUID> {
    // ... existing methods unchanged, NOT part of Red Gate ...

    @Query("SELECT e FROM PregnancyExercise e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:trimester IS NULL OR e.trimesterScope = :trimester) AND " +
           "(:difficulty IS NULL OR e.difficultyLevel = :difficulty) ORDER BY e.createdAt DESC")
    Page<PregnancyExercise> findAllByFilters(
            @Param("status") ExerciseStatus status,
            @Param("trimester") TrimesterScope trimester,
            @Param("difficulty") DifficultyLevel difficulty,
            Pageable pageable);
    // NOTE: this method signature itself is not "throwing" — Red Gate for this method is proven by
    // EX-ADMIN-TC-017 failing with a MethodNotImplementedError/compile-absence until the @Query is added.
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EX-ADMIN-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-ADMIN-TC-007` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-ADMIN-TC-011` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-ADMIN-TC-019` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-ADMIN-TC-SEC-001` | `throw(...)` (or compile-absent `AdminExerciseController`) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EX-ADMIN-TC-INT-002` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| *(control, NOT stubbed)* UC29 `ExerciseServiceTest` existing suite | No stub applied | 🟢 Must stay PASS throughout | ☐ PASS ☐ FAIL | If this fails, the Red Gate stub incorrectly touched existing read-side code — STOP and fix scope |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả UC185-new-code FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Existing UC29/UC177 suite vẫn PASS trong lúc stub active? ☐ Yes (mandatory — confirms scope isolation per §1.2)
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EXERCISE-IMP-ADMIN-001` reviewed (currently `Draft`, ADR-EXERCISE-ADMIN-001..004 `Accepted`)
- [ ] Logic Issues (§2, L1-L5) confirmed with Tech Lead, especially L2 (`durationMinutes` bound is TDS-derived, not SRS-approved) and L4 (`SYSTEM_ADMIN` exclusion)
- [ ] No migration required (TDS §5.2) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories
- [ ] Confirmed existing `PregnancyExercise`/`ExerciseRepository`/`ExerciseMapper`/`ExerciseNotFoundException` compile and existing UC29/UC177 test suites are green BEFORE this feature's Red Gate stub is applied (baseline for §5.1 scope-isolation check)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green (new `exercise` admin classes)
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green (verify `vitest`/`@testing-library/react` are present in `05_Development/CareBridgeWebApp/package.json`; if absent, this is an infrastructure prerequisite — see Suspension Criteria)
- [ ] Test coverage ≥ 80% lines for `AdminExerciseServiceImpl`
- [ ] No business logic in `AdminExerciseController` (validation + mapping only)
- [ ] Existing UC29/UC177/UC178-184 test suites remain green, unmodified, throughout (proves §1.2 scope isolation held)
- [ ] `EX-ADMIN-TC-007` (safety-warning-blank rejection) and `EX-ADMIN-TC-SEC-001`/`SEC-002` (RBAC matrix) all green — mandatory gates, not optional, given the CASE 2.0 safety constraint and BR-RBAC risk
- [ ] `EX-ADMIN-TC-019` (no hard-delete contract check) green — mandatory per ADR-EXERCISE-ADMIN-002/C6

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all NEW-code tests FAIL with stub before implementation; existing read-side suite stays green throughout
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `AdminExerciseTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema/TDS-section citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) status in `05_Development/CareBridgeWebApp/package.json` must be re-verified at implementation time — if absent, `EX-ADMIN-WEB-TC-*` are suspended until added (separate small PR, not part of this feature's core scope per CLAUDE.md "smallest scoped change")
- `durationMinutes` 1-180 bound (Logic Issue L2) is TDS-derived, not SRS-confirmed — flagged `Open`; do not silently change the bound in test or implementation without a Tech Lead-approved ADR update
- Whether a no-op idempotent activate/disable call still writes an audit log row is not explicitly stated in the TDS (Logic Issue / EX-ADMIN-TC-011 note) — this Test-Spec asserts the conservative "no redundant write" interpretation pending confirmation; if Tech Lead confirms audit-on-no-op is required, `EX-ADMIN-TC-011`/`TC-013` must be updated together with a TDS clarification
- Media file upload pipeline (`mediaUrl` as plain string) is out of scope per TDS §1 "Out-of-Scope" — no test coverage for actual file upload in this spec, consistent with the TDS

---

## 7. Rollback Plan

```bash
# No migration to revert — UC185 introduces no schema change (TDS §5.2).

# Revert NEW implementation files only (do not touch existing read-side files)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminExerciseController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/IAdminExerciseService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/AdminExerciseServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/dto/CreateExerciseRequest.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/dto/UpdateExerciseRequest.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/dto/AdminExerciseResponse.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/exception/InvalidExerciseStateException.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/service/AdminExerciseServiceImplTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/AdminExerciseControllerSecurityTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/integration/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExercise/

# Revert EXTENSIONS carefully — these files are SHARED with UC29/UC177, revert only the added methods, never the whole file blindly
git diff 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/repository/ExerciseRepository.java
git diff 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/mapper/ExerciseMapper.java
git diff 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java

# After reverting extensions, re-run UC29/UC177 existing suites to confirm no collateral damage:
# ./mvnw test -Dtest=ExerciseServiceTest,ExerciseControllerTest

# Redeploy previous artifact
kubectl rollout undo deployment/carebridge-api  # (or equivalent per actual deploy target)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> Table reuses the exact anti-patterns already defined in TDS §17.4 (AP-AI-001, AP-AI-003, AP-AI-005) plus the standard CASE 2.0 catalog entries (AP-AI-002, AP-AI-004) used consistently across this project's other Test-Specs (e.g. UC97). No contradicting or invented anti-patterns added.

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào (per TDS §17.4: "Code không match bất kỳ constraint C1-C7 nào") | ☑ Không phát hiện — mọi TC có Oracle Source trỏ về ADR/TDS/SRS cụ thể | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement — tracker ở §5.1 chưa tick | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (per TDS §17.4: "e.g. adds a delete endpoint, or a new lookup table") | ☑ Không phát hiện — `EX-ADMIN-TC-019` explicitly tests the ABSENCE of a delete endpoint; `EX-ADMIN-TC-017/018` explicitly test that admin queries do NOT reuse the Mother-facing status-scoped queries | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — `EX-ADMIN-TC-SEC-*` test controller only for RBAC/validation; safety-warning/state-machine/audit business logic asserted only in `AdminExerciseServiceImplTest` | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (per TDS §17.4: "e.g. invents a PregnancyExerciseV2 entity") | ☑ Không phát hiện — all types (`PregnancyExercise`, `ExerciseRepository`, `ExerciseMapper`, `ExerciseNotFoundException`, `AuditService`) match TDS §5.1/§8 exactly; no duplicate entity/mapping introduced (C3 compliance verified structurally by test design, not just assertion) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)
- [x] Additional CareBridge-specific check: no test in this spec touches or modifies `ExerciseRepository.findByExerciseIdAndStatus`/`findPublishedByFilters` or `ExerciseMapper.toSummaryResponse`/`toDetailResponse` — confirmed via §1.2 scope note and §5.1 Red Gate scope isolation control row

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
