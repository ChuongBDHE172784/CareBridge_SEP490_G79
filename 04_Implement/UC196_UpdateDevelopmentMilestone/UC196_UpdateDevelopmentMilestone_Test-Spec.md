# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-196 Update Development Milestone

**Document ID:** `CB-BABY-TDD-004`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md` (CB-BABY-IMP-004)
- SRS: §3.3.12.5
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260707120000__add_development_milestone_status_columns.sql`

> **Quy ước TDD:** Viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo TDD spec cho UC-196 Update Development Milestone |

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
| **Feature / Gap ID** | `UC-196` |
| **Module** | `UpdateDevelopmentMilestone — baby` |
| **Spec gốc** | `CB-BABY-IMP-004` |
| **Priority** | 🟡 P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy — UC192)`, `development_milestones` table |
| **Downstream Consumers** | `UC197 Delete Development Milestone` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC196 TDS §17`, `ADR-BABY-006`, `ADR-BABY-007` |
| **Constraints Injected** | C1 (milestoneStatus-only write), C2 (canManage strict ownership), C3 (DELETED → 404), C4 (path param not trusted), C5 (validation rules) |
| **Model** | `Claude (Sonnet 5)` |
| **Trust Level** | `T1 → T2 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.12.5: "Updates date, notes, or status" — không định nghĩa "status" là gì | `development_milestones` KHÔNG có cột `status` nào trong `V1__init_schema.sql`. ADR-BABY-006 định nghĩa `milestone_status` (achievement) tách biệt `record_status` (soft-delete, do UC197 sở hữu) | Test PHẢI verify update chỉ ghi `milestone_status`, không đụng `record_status` (§4 DISAMB test) |
| L2 | SRS không nói rõ ai được sửa — chỉ ghi Primary Actor = Mother | `BabyAccessPolicy.canView()` (UC192, đã ship) cho phép cả care group ACCEPTED member — nếu tái sử dụng nguyên cho mutation sẽ over-permission | Test PHẢI verify care group member (ACCEPTED, non-owner) nhận 403 khi update — dùng `canManage()` mới, KHÔNG `canView()` |
| L3 | SRS không định nghĩa validation rule cho "status" | Business rule tự suy luận: `status=ACHIEVED` cần có `achievedDate` để có ý nghĩa nghiệp vụ | Test PHẢI verify `status=ACHIEVED` thiếu `achievedDate` (cả cũ và mới) → 400 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UpdateDevelopmentMilestone bao gồm các layer:
├── Domain (MilestoneAchievementStatus/MilestoneRecordStatus — pure enums)
├── Services (DevelopmentMilestoneServiceImpl — mock JPA Repository + BabyAccessPolicy với Mockito)
├── Policy (BabyAccessPolicy.canManage() — unit test riêng, isolate khỏi canView())
├── Controller (DevelopmentMilestoneController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest, verify Flyway migration V20260707120000)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-196 §3.3.12.5` | Update date/notes/status behavior |
| `ADR-BABY-006` | milestone_status vs record_status disambiguation |
| `ADR-BABY-007` | canManage() strict ownership vs canView() |
| `BR-RBAC` | Owner-only mutation |
| `BR-PRIVACY` | Response minimum-necessary fields |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner updates status+achievedDate | `DevelopmentMilestoneServiceImpl.updateMilestone()` | `MILESTONE-UPD-TC-001` |
| TC-COND-002 | Owner updates note only (partial) | Partial update logic | `MILESTONE-UPD-TC-002` |
| TC-COND-003 | Care group member (ACCEPTED, non-owner) → 403 | `BabyAccessPolicy.canManage()` | `MILESTONE-UPD-TC-003` |
| TC-COND-004 | Unrelated user → 403 | `canManage()` | `MILESTONE-UPD-TC-004` |
| TC-COND-005 | Non-existent milestone → 404 | repo lookup | `MILESTONE-UPD-TC-005` |
| TC-COND-006 | Soft-deleted milestone (recordStatus=DELETED) → 404 | recordStatus guard | `MILESTONE-UPD-TC-006` |
| TC-COND-007 | Empty request body → 400 | validation | `MILESTONE-UPD-TC-007` |
| TC-COND-008 | status=ACHIEVED without achievedDate → 400 | validation | `MILESTONE-UPD-TC-008` |
| TC-COND-009 | [CRITICAL] Update status does NOT touch recordStatus | ADR-BABY-006 disambiguation | `MILESTONE-UPD-TC-DISAMB-001` |
| TC-COND-010 | IDOR — path babyId spoofed, ownership from DB only | ADR-BABY-007 / BR-RBAC | `MILESTONE-UPD-TC-SEC-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | achievedDate (past/present/future), note (empty/max-length/null) | Boundary validation coverage |
| Boundary Value Analysis | `note` length = 2000/2001 chars | `@Size(max=2000)` boundary |
| State Transition Testing | `milestoneStatus` FSM: PENDING→ACHIEVED→DELAYED | §6.3 State Machine trong TDS |
| Error Guessing | IDOR via path babyId mismatch, care group ACCEPTED privilege escalation attempt | Security-focused |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ milestoneId: MILESTONE-001, babyId: BABY-001, milestoneStatus: PENDING, recordStatus: ACTIVE, achievedDate: null }` | Happy path |
| `FX-002` | DB seed | `{ milestoneId: MILESTONE-002, babyId: BABY-001, milestoneStatus: ACHIEVED, recordStatus: DELETED }` | Soft-deleted reject |
| `FX-003` | DB seed | `{ babyId: BABY-001, ownerUserId: MOTHER-001 }` (BabyProfile) | Ownership chain |
| `FX-004` | DB seed | `{ careGroupId: CG-001, ownerAccountId: MOTHER-001, memberUserId: MOTHER-002, inviteStatus: ACCEPTED }` | Care group non-owner reject |
| `FX-005` | JWT | `{ sub: MOTHER-001, role: MOTHER }` | Auth context — owner |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════
class DevelopmentMilestoneTestFactory {

    static DevelopmentMilestone makePendingMilestone() {
        DevelopmentMilestone m = new DevelopmentMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        m.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        m.setMilestoneType("crawling");
        m.setAchievedDate(null);
        m.setNote("initial note");
        m.setSourceType("manual");
        m.setRecordedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        m.setMilestoneStatus(MilestoneAchievementStatus.PENDING);
        m.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        return m;
    }

    static DevelopmentMilestone makeSoftDeletedMilestone() {
        DevelopmentMilestone m = makePendingMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        m.setMilestoneStatus(MilestoneAchievementStatus.ACHIEVED);
        m.setRecordStatus(MilestoneRecordStatus.DELETED);
        return m;
    }

    static BabyProfile makeOwnerProfile() {
        BabyProfile p = new BabyProfile();
        p.setId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        p.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // MOTHER-001
        p.setNickname("Bean");
        return p;
    }

    static UpdateDevelopmentMilestoneRequest makeAchievedRequest() {
        UpdateDevelopmentMilestoneRequest r = new UpdateDevelopmentMilestoneRequest();
        r.setStatus(MilestoneAchievementStatus.ACHIEVED);
        r.setAchievedDate(LocalDate.of(2026, 7, 1));
        return r;
    }

    static UpdateDevelopmentMilestoneRequest makeEmptyRequest() {
        return new UpdateDevelopmentMilestoneRequest(); // all fields null
    }
}
```

---

### MILESTONE-UPD-TC-001 — Owner updates status + achievedDate → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `DevelopmentMilestoneServiceImpl.updateMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/DevelopmentMilestoneServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.3.12.5`

**Preconditions:** FX-001, FX-003, FX-005

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-001)` → `makePendingMilestone()`
2. Mock `babyProfileRepository.findById(BABY-001)` → `makeOwnerProfile()`
3. Mock `babyAccessPolicy.canManage(profile, MOTHER-001)` → true
4. Call `updateMilestone(MILESTONE-001, makeAchievedRequest(), MOTHER-001)`

**Expected Result (PASS):** Response 200 với `status=ACHIEVED`, `achievedDate=2026-07-01`; `milestoneRepository.save()` được gọi đúng 1 lần với entity có `milestoneStatus=ACHIEVED`

**Expected Result (FAIL):** Exception ném ra, hoặc `save()` không được gọi

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-002 — Owner updates note only (partial update) → 200

**Severity:** `HIGH`
**Feature Under Test:** `updateMilestone()` — partial update
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Test Steps:**
1. Request chỉ có `note="new note"` (achievedDate, status = null)
2. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`

**Expected Result (PASS):** Response 200, `note` đổi; `milestoneStatus` GIỮ NGUYÊN giá trị cũ (`PENDING`) — không bị null hoá bởi request field null

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-003 — Care group member (ACCEPTED, non-owner) → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage()` — strict ownership
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** FX-004 (MOTHER-002 is ACCEPTED care group member, NOT owner)

**Test Steps:**
1. Mock `babyProfileRepository.findById(BABY-001)` → profile owned by MOTHER-001
2. Mock `babyAccessPolicy.canManage(profile, MOTHER-002)` → **false** (real impl: `ownerUserId.equals(callerId)` → false vì MOTHER-002 ≠ MOTHER-001)
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-002)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-002` (403) — **dù MOTHER-002 có quyền VIEW (canView=true) qua care group, vẫn KHÔNG có quyền MANAGE**

**Expected Result (FAIL):** Nếu code dùng nhầm `canView()` thay vì `canManage()`, test này sẽ PASS sai (response 200) — đây chính là regression cần chặn

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test quan trọng nhất để phát hiện AP-AI-003 (dùng nhầm policy method có sẵn thay vì method mới theo ADR-BABY-007).

---

### MILESTONE-UPD-TC-004 — Unrelated user → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Expected Result:** throws `BusinessException` `MILESTONE-002` (403)

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-005 — Non-existent milestone → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Mock `milestoneRepository.findById(NONEXISTENT)` → `Optional.empty()`
2. Call `updateMilestone(NONEXISTENT, request, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-001` (404)

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-006 — Soft-deleted milestone (recordStatus=DELETED) → 404

**Severity:** `CRITICAL`
**Feature Under Test:** `recordStatus` guard
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-006`

**Preconditions:** FX-002 (`makeSoftDeletedMilestone()`)

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-002)` → soft-deleted entity (`recordStatus=DELETED`)
2. Call `updateMilestone(MILESTONE-002, request, MOTHER-001)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-001` (404) — treat as not-found, **KHÔNG cho phép "hồi sinh" record qua update**

**Expected Result (FAIL):** Nếu code cho phép update thành công → record bị "hồi sinh" trái phép, vi phạm ADR-BABY-006

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-007 — Empty request body → 400

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Call `updateMilestone(MILESTONE-001, makeEmptyRequest(), MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-003` (400)

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-008 — status=ACHIEVED without achievedDate → 400

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `SRS §3.3.12.5` (business inference — achievement needs a date)

**Test Steps:**
1. Entity fixture: `achievedDate=null` (FX-001, `makePendingMilestone()`)
2. Request: `{status: ACHIEVED}` (no `achievedDate` provided)
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-003` (400) — since neither existing nor new `achievedDate` is present

**Sub-case:** If entity already has `achievedDate` set (from prior update) and request only sends `status=ACHIEVED` without a new date → should SUCCEED (uses existing date). Cover as `MILESTONE-UPD-TC-008b`.

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-DISAMB-001 — [CRITICAL] Update status does NOT touch recordStatus

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-006 disambiguation invariant
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-BABY-006`

**Test Steps:**
1. Entity fixture: `milestoneStatus=PENDING, recordStatus=ACTIVE`
2. Request: `{status: DELAYED}`
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`
4. Capture the entity passed to `milestoneRepository.save(entityCaptor.capture())`

**Expected Result (PASS):**
- `entityCaptor.getValue().getMilestoneStatus() == DELAYED`
- `entityCaptor.getValue().getRecordStatus() == ACTIVE` (**UNCHANGED** — this is the disambiguation assertion)

**Expected Result (FAIL):** If `getRecordStatus()` is anything other than `ACTIVE`, or is null, or `DELETED` — the service is incorrectly conflating the two status concepts, a **direct violation of ADR-BABY-006**.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important test in this Test-Spec per task requirements — it proves UC196's status update never soft-deletes.

---

### SECURITY TEST CASES

---

### MILESTONE-UPD-TC-SEC-001 — IDOR: spoofed path babyId does not bypass ownership check

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** `DevelopmentMilestoneController.updateMilestone()` + `DevelopmentMilestoneServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/DevelopmentMilestoneControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- MILESTONE-001 actually belongs to BABY-001 (owned by MOTHER-001)
- Attacker MOTHER-999 sends request with path `babyId = BABY-999` (a baby MOTHER-999 owns) but `milestoneId = MILESTONE-001` (belongs to a different baby)

**Test Steps (Attack Simulation):**
1. `PATCH /api/v1/babies/BABY-999/milestones/MILESTONE-001` with JWT for MOTHER-999
2. Service resolves `milestone.getBabyId()` from DB (= BABY-001), NOT from the path
3. Load `BabyProfile` for BABY-001 → owner = MOTHER-001
4. `canManage(profile, MOTHER-999)` → false

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` `MILESTONE-002` — path `babyId` mismatch is irrelevant; authorization is always derived from `milestone.getBabyId()` read from DB

**Expected Result (FAIL = lỗ hổng tồn tại):** If the service trusts path `babyId` for authorization instead of the DB-resolved value, an attacker could potentially manipulate scoping logic

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MILESTONE-UPD-TC-INT-001 — Full flow: PATCH updates DB, record_status untouched

**Severity:** `HIGH`
**Feature Under Test:** Full flow: HTTP PATCH → Service → Repository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/DevelopmentMilestoneIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migrations applied automatically (including `V20260707120000`)
- Seed: BABY-001 owned by MOTHER-001; MILESTONE-001 with `milestoneStatus=PENDING, recordStatus=ACTIVE`

**Test Steps:**
1. `PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}` with `{status: ACHIEVED, achievedDate: "2026-07-01"}`, JWT for MOTHER-001
2. Assert response 200, body `status=ACHIEVED`
3. Query DB directly: `SELECT milestone_status, record_status FROM development_milestones WHERE milestone_id = ?`

**Expected Result (PASS):**
- `milestone_status = 'ACHIEVED'`
- `record_status = 'ACTIVE'` (unchanged)
- `updated_at` bumped forward

**DB Assertion:**
```java
DevelopmentMilestone record = milestoneRepository.findById(savedId).orElseThrow();
assertThat(record.getMilestoneStatus()).isEqualTo(MilestoneAchievementStatus.ACHIEVED);
assertThat(record.getRecordStatus()).isEqualTo(MilestoneRecordStatus.ACTIVE);
```

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES (Flutter/Dart)

---

### MILESTONE-UPD-TC-MOBILE-001 — Update form validates and submits successfully

**Severity:** `HIGH`
**Feature Under Test:** `UpdateMilestoneScreen` / `MilestoneFormWidget` (Flutter)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/update_milestone_screen_test.dart`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Pump `UpdateMilestoneScreen` with an existing milestone (status=PENDING)
2. Select status dropdown → `ACHIEVED`
3. Enter `achievedDate` via date picker
4. Tap "Save"

**Expected Result (PASS):** API call fired with `{status: "ACHIEVED", achievedDate: "..."}`; success snackbar shown; screen pops back to milestone detail with updated status displayed

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-MOBILE-002 — Selecting ACHIEVED without a date shows inline validation error

**Severity:** `MEDIUM`
**Feature Under Test:** `MilestoneFormWidget` client-side validation
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Pump form, select status = `ACHIEVED`, leave date field empty
2. Tap "Save"

**Expected Result (PASS):** Inline validation error shown ("Please select the achieved date"); no API call fired (mirrors backend `MILESTONE-003` rule client-side)

**Current Status:** 🔴 Not written

---

### MILESTONE-UPD-TC-MOBILE-003 — 403 response from server surfaces a permission error

**Severity:** `MEDIUM`
**Feature Under Test:** Error handling — mock HTTP client returns 403 `MILESTONE-002`
**TDD Phase:** 🔴 RED

**Expected Result (PASS):** UI shows a permission-denied message; no crash; form remains editable for retry (or navigates back per UX spec)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MILESTONE-UPD-TC-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-002` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-003` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-004` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-005` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-006` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-007` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-008` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-DISAMB-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-SEC-001` | `DevelopmentMilestoneControllerTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-UPD-TC-INT-001` | `DevelopmentMilestoneIntegrationTest.java:__` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
    @Override
    public DevelopmentMilestoneDetailResponse updateMilestone(
            UUID milestoneId, UpdateDevelopmentMilestoneRequest request, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MILESTONE-UPD-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MILESTONE-UPD-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MILESTONE-UPD-TC-DISAMB-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T1→T2) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-004` đã được review và approve
- [ ] Logic Issues (§2) đã confirm với Tech Lead
- [ ] Migration `V20260707120000` đã approved (companion cho cả UC196 và UC197)

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% cho `DevelopmentMilestoneServiceImpl`
- [ ] `MILESTONE-UPD-TC-DISAMB-001` PASS bắt buộc (gate riêng — không được skip)
- [ ] `MILESTONE-UPD-TC-003` (care group non-owner 403) PASS bắt buộc
- [ ] Không có business logic trong `DevelopmentMilestoneController`
- [ ] `flutter test` xanh cho mobile widget tests

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả tests FAIL với throw stub trước khi implement
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] Props Isolation: mọi test dùng `DevelopmentMilestoneTestFactory`, không shared mutable state

### Suspension Criteria
- Migration `V20260707120000` chưa approved/chạy được trên staging
- UC197 migration conflict phát hiện (cùng file migration, cần coordinate)

---

## 7. Rollback Plan

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260707120000__add_development_milestone_status_columns.sql
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test dùng `canView()` thay vì `canManage()` để mock 403 case, hoặc dùng 1 cột status chung | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
