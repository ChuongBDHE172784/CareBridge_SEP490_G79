# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-197 Delete Development Milestone

**Document ID:** `CB-BABY-TDD-005`
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
- TDS: `04_Implement/UC197_DeleteDevelopmentMilestone/UC197_DeleteDevelopmentMilestone_TDS.md` (CB-BABY-IMP-005)
- Companion TDS: `04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md` (entity/migration/canManage() owner)
- SRS: §3.3.12.6
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260707120000__add_development_milestone_status_columns.sql`

> **Quy ước TDD:** Viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo TDD spec cho UC-197 Delete Development Milestone |

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
| **Feature / Gap ID** | `UC-197` |
| **Module** | `DeleteDevelopmentMilestone — baby` |
| **Spec gốc** | `CB-BABY-IMP-005` |
| **Priority** | 🟡 P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy.canManage() — UC196)`, `DevelopmentMilestone` entity/repository (UC196, shared) |
| **Downstream Consumers** | — |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC197 TDS §17`, `ADR-BABY-006`, `ADR-BABY-007`, `ADR-BABY-008` |
| **Constraints Injected** | C1 (recordStatus-only write), C2 (soft-delete only, no hard-delete), C3 (canManage strict ownership), C4 (double-delete → 404), C5 (no new migration) |
| **Model** | `Claude (Sonnet 5)` |
| **Trust Level** | `T1 → T2 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.12.6: "Soft-deletes a Mother-recorded development milestone" — không định nghĩa cơ chế soft-delete cụ thể | `development_milestones` không có cột soft-delete nào trước migration `V20260707120000`. ADR-BABY-008 định nghĩa `record_status` (ACTIVE/DELETED), TÁCH BIỆT khỏi `milestone_status` (UC196) | Test PHẢI verify delete chỉ ghi `record_status`, không đụng `milestone_status` (§4 DISAMB test) |
| L2 | SRS không nói rõ hành vi double-delete | ADR-BABY-008 quyết định: double-delete → 404 (nhất quán UC194/195 "treat as not-found" pattern), KHÔNG 409 | Test PHẢI verify gọi delete lần 2 trả 404, không lỗi 500 |
| L3 | SRS không nói rõ có hard-delete hay không | ADR-BABY-008: LUÔN soft-delete, row không bao giờ bị xoá vật lý (BR-PRIVACY retention) | Test PHẢI verify `COUNT(*)` không đổi trước/sau khi xoá |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
DeleteDevelopmentMilestone bao gồm các layer:
├── Domain (MilestoneRecordStatus — pure enum, shared với UC196)
├── Services (DevelopmentMilestoneServiceImpl.deleteMilestone() — mock JPA Repository + BabyAccessPolicy với Mockito)
├── Policy (BabyAccessPolicy.canManage() — reuse test double từ UC196)
├── Controller (DevelopmentMilestoneController.deleteMilestone() — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — verify soft-delete row retention)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-197 §3.3.12.6` | Soft-delete behavior |
| `ADR-BABY-006` | milestone_status vs record_status disambiguation |
| `ADR-BABY-007` | canManage() strict ownership |
| `ADR-BABY-008` | soft-delete only, double-delete → 404 |
| `BR-RBAC` | Owner-only mutation |
| `BR-PRIVACY` | Row retention — no hard-delete |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner soft-deletes milestone | `DevelopmentMilestoneServiceImpl.deleteMilestone()` | `MILESTONE-DEL-TC-001` |
| TC-COND-002 | Row NOT hard-deleted (still queryable) | `save()` not `delete()` | `MILESTONE-DEL-TC-002` |
| TC-COND-003 | Care group member (ACCEPTED, non-owner) → 403 | `BabyAccessPolicy.canManage()` | `MILESTONE-DEL-TC-003` |
| TC-COND-004 | Unrelated user → 403 | `canManage()` | `MILESTONE-DEL-TC-004` |
| TC-COND-005 | Non-existent milestone → 404 | repo lookup | `MILESTONE-DEL-TC-005` |
| TC-COND-006 | Double-delete (already DELETED) → 404 | `recordStatus` guard | `MILESTONE-DEL-TC-006` |
| TC-COND-007 | [CRITICAL] Delete does NOT touch milestoneStatus | ADR-BABY-006 disambiguation | `MILESTONE-DEL-TC-DISAMB-001` |
| TC-COND-008 | IDOR — path babyId spoofed, ownership from DB only | ADR-BABY-007 / BR-RBAC | `MILESTONE-DEL-TC-SEC-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | `recordStatus` FSM: ACTIVE→DELETED (terminal) | §6.3 State Machine trong TDS |
| Error Guessing | Double-delete, IDOR via path babyId mismatch, care group ACCEPTED privilege escalation | Security/robustness-focused |
| Equivalence Partitioning | recordStatus ∈ {ACTIVE, DELETED} — 2 classes | Guard condition coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ milestoneId: MILESTONE-001, babyId: BABY-001, milestoneStatus: ACHIEVED, recordStatus: ACTIVE }` | Happy path |
| `FX-002` | DB seed | `{ milestoneId: MILESTONE-002, babyId: BABY-001, milestoneStatus: PENDING, recordStatus: DELETED }` | Double-delete reject |
| `FX-003` | DB seed | `{ babyId: BABY-001, ownerUserId: MOTHER-001 }` (BabyProfile) | Ownership chain |
| `FX-004` | DB seed | `{ careGroupId: CG-001, ownerAccountId: MOTHER-001, memberUserId: MOTHER-002, inviteStatus: ACCEPTED }` | Care group non-owner reject |
| `FX-005` | JWT | `{ sub: MOTHER-001, role: MOTHER }` | Auth context — owner |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Reuse DevelopmentMilestoneTestFactory từ UC196 test suite,
// mở rộng thêm factory riêng cho các case xoá.
// ═══════════════════════════════════════════════════════════
class DevelopmentMilestoneDeleteTestFactory {

    static DevelopmentMilestone makeActiveAchievedMilestone() {
        DevelopmentMilestone m = new DevelopmentMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        m.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        m.setMilestoneType("first_smile");
        m.setAchievedDate(LocalDate.of(2026, 5, 1));
        m.setNote("first documented smile");
        m.setMilestoneStatus(MilestoneAchievementStatus.ACHIEVED);
        m.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        return m;
    }

    static DevelopmentMilestone makeAlreadyDeletedMilestone() {
        DevelopmentMilestone m = makeActiveAchievedMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        m.setMilestoneStatus(MilestoneAchievementStatus.PENDING);
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
}
```

---

### MILESTONE-DEL-TC-001 — Owner soft-deletes milestone → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `DevelopmentMilestoneServiceImpl.deleteMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/DevelopmentMilestoneServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.3.12.6`

**Preconditions:** FX-001, FX-003, FX-005

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-001)` → `makeActiveAchievedMilestone()`
2. Mock `babyProfileRepository.findById(BABY-001)` → `makeOwnerProfile()`
3. Mock `babyAccessPolicy.canManage(profile, MOTHER-001)` → true
4. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`

**Expected Result (PASS):** No exception; `milestoneRepository.save()` called exactly once with entity where `recordStatus == DELETED`

**Expected Result (FAIL):** Exception thrown, or `save()` not called, or `milestoneRepository.delete()`/`deleteById()` called instead (hard-delete violation)

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-002 — Row is NOT hard-deleted

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-008 (soft-delete only)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-008`

**Test Steps:**
1. Same setup as `MILESTONE-DEL-TC-001`
2. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`
3. Verify `milestoneRepository.delete(any())` — **never invoked**
4. Verify `milestoneRepository.deleteById(any())` — **never invoked**

**Expected Result (PASS):** `verify(milestoneRepository, never()).delete(any())` and `verify(milestoneRepository, never()).deleteById(any())` both pass; only `save()` is invoked

**Expected Result (FAIL):** If `delete()`/`deleteById()` is called, this is a **direct violation of ADR-BABY-008** (BR-PRIVACY retention breach)

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-003 — Care group member (ACCEPTED, non-owner) → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage()` — strict ownership
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** FX-004 (MOTHER-002 is ACCEPTED care group member, NOT owner)

**Test Steps:**
1. Mock `babyProfileRepository.findById(BABY-001)` → profile owned by MOTHER-001
2. Mock `babyAccessPolicy.canManage(profile, MOTHER-002)` → **false**
3. Call `deleteMilestone(MILESTONE-001, MOTHER-002)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-002` (403); `milestoneRepository.save()` never invoked

**Current Status:** 🔴 Not written
**Implementation Note:** Same pattern as UC196's `MILESTONE-UPD-TC-003` — validates `canManage()` reuse consistency across both mutation UCs.

---

### MILESTONE-DEL-TC-004 — Unrelated user → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Expected Result:** throws `BusinessException` `MILESTONE-002` (403)

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-005 — Non-existent milestone → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Mock `milestoneRepository.findById(NONEXISTENT)` → `Optional.empty()`
2. Call `deleteMilestone(NONEXISTENT, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-001` (404)

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-006 — Double-delete (already DELETED) → 404

**Severity:** `HIGH`
**Feature Under Test:** `recordStatus` guard, idempotency
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-008`

**Preconditions:** FX-002 (`makeAlreadyDeletedMilestone()`)

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-002)` → entity with `recordStatus=DELETED`
2. Call `deleteMilestone(MILESTONE-002, MOTHER-001)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-001` (404) — **NOT** 409, **NOT** 500; `save()` never invoked (no further side-effect)

**Expected Result (FAIL):** 500 error, or successful 200 (would mean the guard is missing and `save()` is called redundantly)

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-DISAMB-001 — [CRITICAL] Delete does NOT touch milestoneStatus

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-006 disambiguation invariant
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-006`

**Test Steps:**
1. Entity fixture: `milestoneStatus=ACHIEVED, recordStatus=ACTIVE`
2. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`
3. Capture the entity passed to `milestoneRepository.save(entityCaptor.capture())`

**Expected Result (PASS):**
- `entityCaptor.getValue().getRecordStatus() == DELETED`
- `entityCaptor.getValue().getMilestoneStatus() == ACHIEVED` (**UNCHANGED** — this is the disambiguation assertion)

**Expected Result (FAIL):** If `getMilestoneStatus()` is anything other than the original `ACHIEVED` value, or is null, or reset to a default — the service is incorrectly conflating the two status concepts, a **direct violation of ADR-BABY-006**.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the mirror test to UC196's `MILESTONE-UPD-TC-DISAMB-001` — together they prove the two status concepts are write-isolated in BOTH directions (UC196 never sets recordStatus; UC197 never sets milestoneStatus).

---

### SECURITY TEST CASES

---

### MILESTONE-DEL-TC-SEC-001 — IDOR: spoofed path babyId does not bypass ownership check

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** `DevelopmentMilestoneController.deleteMilestone()` + `DevelopmentMilestoneServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/DevelopmentMilestoneControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- MILESTONE-001 actually belongs to BABY-001 (owned by MOTHER-001)
- Attacker MOTHER-999 sends request with path `babyId = BABY-999` (a baby MOTHER-999 owns) but `milestoneId = MILESTONE-001` (belongs to a different baby)

**Test Steps (Attack Simulation):**
1. `DELETE /api/v1/babies/BABY-999/milestones/MILESTONE-001` with JWT for MOTHER-999
2. Service resolves `milestone.getBabyId()` from DB (= BABY-001), NOT from the path
3. Load `BabyProfile` for BABY-001 → owner = MOTHER-001
4. `canManage(profile, MOTHER-999)` → false

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` `MILESTONE-002` — path `babyId` mismatch does not grant access; MILESTONE-001 remains `recordStatus=ACTIVE` (unaffected)

**Expected Result (FAIL = lỗ hổng tồn tại):** MILESTONE-001 gets deleted despite the caller not owning its actual baby

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MILESTONE-DEL-TC-INT-001 — Full flow: DELETE soft-deletes, row retained, milestoneStatus untouched

**Severity:** `HIGH`
**Feature Under Test:** Full flow: HTTP DELETE → Service → Repository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/DevelopmentMilestoneIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-002, TC-COND-007`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migrations applied automatically (including `V20260707120000`)
- Seed: BABY-001 owned by MOTHER-001; MILESTONE-001 with `milestoneStatus=ACHIEVED, recordStatus=ACTIVE`

**Test Steps:**
1. `SELECT COUNT(*) FROM development_milestones WHERE baby_id = ?` — capture `countBefore`
2. `DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}` with JWT for MOTHER-001
3. Assert response 200
4. `SELECT COUNT(*) FROM development_milestones WHERE baby_id = ?` — capture `countAfter`
5. `SELECT milestone_status, record_status FROM development_milestones WHERE milestone_id = ?`

**Expected Result (PASS):**
- `countAfter == countBefore` (row retained, NOT hard-deleted)
- `record_status = 'DELETED'`
- `milestone_status = 'ACHIEVED'` (unchanged from seed value)

**DB Assertion:**
```java
long countBefore = milestoneRepository.count();
// ... perform DELETE via MockMvc/WebTestClient ...
long countAfter = milestoneRepository.count();
assertThat(countAfter).isEqualTo(countBefore);

DevelopmentMilestone record = milestoneRepository.findById(milestoneId).orElseThrow();
assertThat(record.getRecordStatus()).isEqualTo(MilestoneRecordStatus.DELETED);
assertThat(record.getMilestoneStatus()).isEqualTo(MilestoneAchievementStatus.ACHIEVED);
```

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES (Flutter/Dart)

---

### MILESTONE-DEL-TC-MOBILE-001 — Delete requires confirmation dialog

**Severity:** `HIGH`
**Feature Under Test:** `MilestoneListItem` / `MilestoneDetailScreen` (Flutter) — delete action
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/delete_milestone_test.dart`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Pump `MilestoneDetailScreen` for an existing milestone
2. Tap "Delete" button
3. Verify a confirmation `AlertDialog` appears
4. Tap "Cancel" in dialog

**Expected Result (PASS):** No API call fired; dialog dismissed; milestone still shown

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-MOBILE-002 — Confirmed delete removes item from list and shows success

**Severity:** `HIGH`
**Feature Under Test:** Delete confirmation flow — success path
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Pump `MilestoneDetailScreen`, tap "Delete", tap "Confirm" in dialog
2. Mock API returns 200

**Expected Result (PASS):** API DELETE call fired with correct `milestoneId`; success snackbar shown; navigates back to milestone list; deleted item no longer visible in list

**Current Status:** 🔴 Not written

---

### MILESTONE-DEL-TC-MOBILE-003 — 403 response from server surfaces a permission error

**Severity:** `MEDIUM`
**Feature Under Test:** Error handling — mock HTTP client returns 403 `MILESTONE-002`
**TDD Phase:** 🔴 RED

**Expected Result (PASS):** UI shows a permission-denied message; item is NOT removed from the local list (state remains consistent with server); no crash

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MILESTONE-DEL-TC-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-002` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-003` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-004` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-005` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-006` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-DISAMB-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-SEC-001` | `DevelopmentMilestoneControllerTest.java:__` | `[ ]` | `___` | — |
| `MILESTONE-DEL-TC-INT-001` | `DevelopmentMilestoneIntegrationTest.java:__` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
    // ... updateMilestone() stub từ UC196 ...

    @Override
    public void deleteMilestone(UUID milestoneId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MILESTONE-DEL-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MILESTONE-DEL-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MILESTONE-DEL-TC-DISAMB-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T1→T2) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-005` đã được review và approve
- [ ] Logic Issues (§2) đã confirm với Tech Lead
- [ ] UC196 đã implement `DevelopmentMilestone` entity, `DevelopmentMilestoneRepository`, `BabyAccessPolicy.canManage()`, và migration `V20260707120000` (dependency cứng — UC197 KHÔNG thể test độc lập nếu thiếu các thành phần này)

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% cho method `deleteMilestone()`
- [ ] `MILESTONE-DEL-TC-DISAMB-001` PASS bắt buộc (gate riêng — không được skip)
- [ ] `MILESTONE-DEL-TC-002` (no hard-delete) PASS bắt buộc
- [ ] `MILESTONE-DEL-TC-006` (double-delete idempotency) PASS bắt buộc
- [ ] Không có business logic trong `DevelopmentMilestoneController`
- [ ] `flutter test` xanh cho mobile widget tests

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả tests FAIL với throw stub trước khi implement
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] Props Isolation: mọi test dùng `DevelopmentMilestoneDeleteTestFactory`, không shared mutable state

### Suspension Criteria
- UC196 chưa deploy (entity/migration/`canManage()` chưa sẵn sàng)
- Migration `V20260707120000` conflict phát hiện giữa 2 PR song song

---

## 7. Rollback Plan

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/
# Migration KHÔNG revert riêng ở đây — sở hữu bởi UC196, xem UC196 Test-Spec §7
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test mock `repository.delete()` thay vì `save()` với recordStatus=DELETED (hard-delete assumption), hoặc dùng `canView()` thay vì `canManage()` | ☐ | G-1 |
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
