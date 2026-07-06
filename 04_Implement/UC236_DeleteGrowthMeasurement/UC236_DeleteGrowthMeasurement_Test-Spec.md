# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-236 Delete Growth Measurement

**Document ID:** `CB-BABY-IMP-012-TEST`
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
- TDS: `04_Implement/UC236_DeleteGrowthMeasurement/UC236_DeleteGrowthMeasurement_TDS.md` (`CB-BABY-IMP-012`)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.9 (Table 258)
- Migration baseline: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (growth_measurements, dòng 647–658)
- Proposed migration: `V20260703000100__add_growth_measurement_deleted_at.sql` (ADR-BABY-012-001, chưa tạo file — Proposed)
- Sibling precedent: `04_Implement/UC38_ViewGrowthChart/UC38_ViewGrowthChart_Test-Spec.md`, `04_Implement/UC215_DeleteReminder/UC215_DeleteReminder_TDS.md`
- Sibling consumer (cross-cutting): `04_Implement/UC237_ViewGrowthMeasurementHistory/` *(thư mục rỗng tại thời điểm viết — chưa có spec; xem §2 L3)*

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent` | Khởi tạo TDD spec cho UC-236 Delete Growth Measurement (soft-delete via `deleted_at`) |

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
| **Feature / Gap ID** | `UC-236` |
| **Module** | `DeleteGrowthMeasurement — carejourney` |
| **Spec gốc** | `CB-BABY-IMP-012` |
| **Priority** | 🟡 P2 *(SRS Priority = Medium, Frequency = Occasional — Table 258)* |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC` *(SRS Table 258 chỉ liệt kê BR-RBAC — không có BR-PRIVACY cho UC này; retention neo vào Assumption + POST-3 của chính Table 258, xem §2 L1)* |
| **PDPA Note** | Bảng `growth_measurements` chứa dữ liệu sức khỏe trẻ em (PII nhạy cảm). Soft-delete (không hard-delete) là cơ chế phù hợp với nguyên tắc lưu trữ tối thiểu-có-mục-đích của PDPA/Assumption Table 258 ("CareBridge retains data according to privacy and audit policies") — dữ liệu vẫn được giữ cho audit, không bị xóa cứng khỏi hệ thống. DPO sign-off vẫn `Pending` vì đây là thay đổi schema PII thật (cột `deleted_at` mới). |
| **Upstream Dependencies** | `auth (JWT), baby (baby_profiles), growth_measurements` |
| **Downstream Consumers** | `UC-237 View Growth Measurement History (PHẢI loại trừ deleted_at IS NOT NULL)`, `UC-38 View Growth Chart (cần cập nhật filter — Open Item)`, `audit` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-012 §17` (C1–C5) |
| **Constraints Injected** | C1 (soft-delete only, cấm hard delete), C2 (ownership BABY-071), C3 (idempotent no-op ADR-BABY-012-002), C4 (cross-baby guard BABY-080/081/070), C5 (event + audit + UC-237 exclusion ADR-BABY-012-003) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Bắt buộc điền trước khi viết test. Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Table 258 mô tả "Soft-deletes an incorrectly entered growth measurement" nhưng **không định nghĩa cơ chế** soft-delete cụ thể | Baseline `V1__init_schema.sql` (dòng 647–658): bảng `growth_measurements` **không có** cột `status`/cờ/timestamp nào để biểu diễn trạng thái xóa mềm — khác hẳn `reminders`/`vaccination_records` vốn đã có `status VARCHAR`. TDS §3 ADR-BABY-012-001 kết luận **cần Flyway migration mới** thêm `deleted_at timestamptz NULL` | Test đợi entity có field `deletedAt`; giả lập migration `V20260703000100` đã áp dụng trong integration test (BABY236-TC-INT-001) trước khi assert |
| L2 | SRS không đề cập hành vi khi xóa lại một measurement đã xóa (retry/double-tap) | TDS §3 ADR-BABY-012-002 (Proposed): idempotent success — trả `204`, không ghi `deleted_at` lại, không publish event, không audit trùng | `BABY236-TC-002` verify save()/publish()/audit KHÔNG được gọi lần 2 |
| L3 | SRS không đề cập ràng buộc giữa UC-236 (Delete) và UC-237 (View History) — sibling UC-237 **chưa có spec** (thư mục rỗng tại thời điểm viết TDS) | TDS §3 ADR-BABY-012-003 (Proposed): mọi truy vấn đọc hiển thị (đặc biệt UC-237) PHẢI lọc `deleted_at IS NULL`. Đây là **phát biểu đầu tiên** của yêu cầu cross-cutting này — UC-237 phải kế thừa khi được soạn | `BABY236-TC-008` verify measurement đã soft-delete bị loại khỏi kết quả `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc` (repository method mà UC-237 sẽ dùng) |
| L4 | SRS Table 258 chỉ liệt kê **BR-RBAC**, không có BR-PRIVACY (khác UC-234/UC-235 trong cùng batch) | Retention/audit trail cho soft-delete được suy ra từ **Assumption** + **POST-3** của chính Table 258, không phải một BR riêng | Test không assert theo mã "BR-PRIVACY" nào; chỉ assert theo POST-3 (audit log ghi nhận) và BR-RBAC (ownership) |
| L5 | SRS không đề cập trường hợp measurement thuộc về một baby khác với `babyId` trong path (IDOR chéo baby) | TDS §8/§10: cross-baby guard mới `BABY-081` (403) — không có trong SRS text nhưng cần thiết để chặn injection `measurementId` của baby khác qua path `babyId` hợp lệ | `BABY236-TC-006` + `BABY236-TC-SEC-001` (security/IDOR) |
| L6 | Cột `status` **không tồn tại** trên `growth_measurements` — rủi ro AI hallucinate theo pattern reminders (`status = 'CANCELLED'`) | Entity thực tế chỉ có các cột liệt kê ở V1 dòng 647–658 + `deleted_at` mới (Proposed) | Red Gate stub (§5.1) và AP-AI-005 check (§8) phải chặn code dùng `measurement.getStatus()` |
| L7 | Test factory `makeOwnedBaby()` dùng `BabyProfile.builder().babyId(BABY_ID)...status("ACTIVE")` | **RESOLVED 2026-07-04** — xác nhận trực tiếp trên đĩa (`baby/entity/BabyProfile.java`): field ID thực tế tên là `id` (`@Column(name="baby_id") private UUID id`), KHÔNG phải `babyId`; `status` là enum `BabyProfileStatus` (ACTIVE/ARCHIVED), KHÔNG phải `String`. Code cũ sẽ KHÔNG compile | Factory đã sửa thành `.id(BABY_ID)` và `.status(BabyProfileStatus.ACTIVE)` (§4 Props Isolation Boilerplate) |
| L8 | Repository method `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc` được UC-236 khai báo tại §8.2 TDS thiếu tham số `Pageable` (trả `List<>`), trong khi UC-237 (consumer thật) cần `Page<>` + `Pageable` cho phân trang | **RESOLVED 2026-07-04** — đối chiếu `CB-BABY-IMP-013` (UC-237) §8.2: chữ ký chính thức là `Page<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(UUID babyId, Pageable pageable)`. Đây là MỘT method dùng chung bởi cả hai UC (không phải 2 method trùng tên khác chữ ký) — đã sửa TDS §8.2 và test cases dưới đây (`BABY236-TC-008`, `BABY236-TC-INT-001`) để dùng đúng chữ ký này | Test case đã cập nhật gọi `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, Pageable.unpaged())` và assert trên `Page<GrowthMeasurement>` thay vì `List<GrowthMeasurement>` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
DeleteGrowthMeasurement bao gồm các layer:
├── Service (mock BabyProfileRepository + GrowthMeasurementRepository với Mockito)
├── Controller (mock IGrowthMeasurementService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — migration V20260703000100 áp dụng tự động)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-236` (Table 258) | Mother soft-delete growth measurement nhập sai |
| `ADR-BABY-012-001` | Cơ chế `deleted_at` — soft-delete, cấm hard-delete |
| `ADR-BABY-012-002` | Idempotent re-delete → 204 no-op |
| `ADR-BABY-012-003` | Cross-cutting: loại trừ soft-deleted khỏi UC-237 |
| `ADR-BABY-008-004` | Owner-only access (reuse UC-38) |
| `ADR-BABY-008-003` | No medical interpretation (reuse UC-38) |
| `BR-RBAC` | Chỉ Mother sở hữu baby mới được xóa measurement |
| `CB-BABY-IMP-012 §8/§9/§10` | Interface contract, API spec, error codes BABY-070/071/080/081/082/083 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner soft-deletes a live measurement | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-001` |
| TC-COND-002 | Re-delete an already soft-deleted measurement (idempotent) | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-002` |
| TC-COND-003 | Baby not owned by caller | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-003` |
| TC-COND-004 | Baby not found | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-004` |
| TC-COND-005 | Measurement not found | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-005` |
| TC-COND-006 | Measurement belongs to a different baby (cross-baby mismatch) | `GrowthMeasurementService.deleteMeasurement()` | `BABY236-TC-006` |
| TC-COND-007 | Unauthenticated access blocked | Spring Security filter | `BABY236-TC-007` |
| TC-COND-008 | Soft-deleted measurement excluded from history-view query (UC-237 cross-reference) | `GrowthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc()` | `BABY236-TC-008` |
| TC-COND-009 | Full DB flow: migration applied, row persists, count unchanged | Testcontainers integration | `BABY236-TC-INT-001` |
| TC-COND-010 | IDOR — non-owner attempts delete via API | Controller + Security | `BABY236-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `deletedAt` null vs non-null | Phân biệt live vs already-deleted → hai nhánh hành vi khác nhau |
| Boundary Value Analysis | First delete vs second (retry) delete call | Biên giữa "lần đầu" (side-effects fire) và "lần sau" (no-op) |
| State Transition Testing | LIVE → DELETED (§6.5 TDS state machine) | Verify chỉ một transition hợp lệ, không có "undelete" trong scope |
| Error Guessing | Cross-baby `measurementId` injection qua `babyId` hợp lệ | IDOR/BOLA — attacker thử measurementId của baby khác |
| Error Guessing | Hallucinated `status` column | Rủi ro AI copy pattern reminders có `status`; growth_measurements không có |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path — Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status='ACTIVE')` | Target baby (owner) |
| `FX-003` | DB seed | `BabyProfile(babyId=OTHER_BABY_ID, ownerUserId=OTHER_USER_ID)` | Non-owned baby / cross-baby source |
| `FX-004` | DB seed | `GrowthMeasurement(growthMeasurementId=MEAS_ID, babyId=BABY_ID, deletedAt=null)` | Live measurement — happy path target |
| `FX-005` | DB seed | `GrowthMeasurement(growthMeasurementId=MEAS_ID_DELETED, babyId=BABY_ID, deletedAt=<past Instant>)` | Already soft-deleted — idempotent test |
| `FX-006` | DB seed | `GrowthMeasurement(growthMeasurementId=MEAS_OTHER_BABY, babyId=OTHER_BABY_ID, deletedAt=null)` | Cross-baby mismatch target |
| `FX-007` | env | Flyway migration `V20260703000100` applied (Testcontainers auto-migrate) | Integration test schema baseline |

---

## 4. Test Case Specification

> **TC ID format:** `BABY236-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory methods, không shared mutable state
// ═══════════════════════════════════════════════════════════

class GrowthMeasurementDeleteTestFactory {

    static UUID MOTHER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000236");
    static UUID OTHER_USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000999");
    static UUID BABY_ID         = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000236");
    static UUID OTHER_BABY_ID   = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000999");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");
    static UUID MEAS_ID         = UUID.fromString("aaaa0236-0000-0000-0000-000000000001");
    static UUID MEAS_ID_DELETED = UUID.fromString("aaaa0236-0000-0000-0000-000000000002");
    static UUID MEAS_OTHER_BABY = UUID.fromString("aaaa0999-0000-0000-0000-000000000001");
    static UUID NON_EXISTENT_MEAS_ID = UUID.fromString("aaaa0236-0000-0000-0000-999999999999");

    static BabyProfile makeOwnedBaby() {
        return BabyProfile.builder()
            .id(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Delete Test Baby")
            .status(BabyProfileStatus.ACTIVE)
            .build();
    }

    static BabyProfile makeBabyOwnedByOther() {
        BabyProfile baby = makeOwnedBaby();
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    static GrowthMeasurement makeLiveMeasurement() {
        return GrowthMeasurement.builder()
            .growthMeasurementId(MEAS_ID)
            .babyId(BABY_ID)
            .measuredDate(LocalDate.of(2026, 6, 1))
            .weightKg(new BigDecimal("6.5"))
            .heightCm(new BigDecimal("60"))
            .deletedAt(null)   // live
            .build();
    }

    static GrowthMeasurement makeAlreadyDeletedMeasurement() {
        return GrowthMeasurement.builder()
            .growthMeasurementId(MEAS_ID_DELETED)
            .babyId(BABY_ID)
            .measuredDate(LocalDate.of(2026, 5, 1))
            .weightKg(new BigDecimal("6.0"))
            .heightCm(new BigDecimal("58"))
            .deletedAt(Instant.parse("2026-06-15T10:00:00Z"))   // already soft-deleted
            .build();
    }

    static GrowthMeasurement makeMeasurementOfOtherBaby() {
        return GrowthMeasurement.builder()
            .growthMeasurementId(MEAS_OTHER_BABY)
            .babyId(OTHER_BABY_ID)   // NOT the path babyId — cross-baby mismatch
            .measuredDate(LocalDate.of(2026, 6, 1))
            .deletedAt(null)
            .build();
    }

    // Overload để override specific fields khi cần
    static GrowthMeasurement makeLiveMeasurement(Consumer<GrowthMeasurement> overrides) {
        GrowthMeasurement m = makeLiveMeasurement();
        overrides.accept(m);
        return m;
    }
}
```

---

### BABY236-TC-001 — Happy path: owner soft-deletes live measurement

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-012 §3 ADR-BABY-012-001, §8 IGrowthMeasurementService`

**Preconditions:**
- Baby exists via `makeOwnedBaby()`, owned by `MOTHER_ID`
- Measurement exists via `makeLiveMeasurement()`, `deletedAt == null`
- Mock: `babyProfileRepository.findById(BABY_ID)` → baby
- Mock: `growthMeasurementRepository.findById(MEAS_ID)` → measurement

**Test Steps:**
1. Arrange: baby + live measurement as above
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, BABY_ID, MEAS_ID)`
3. Assert: measurement.getDeletedAt() != null (non-null Instant); `growthMeasurementRepository.save()` called exactly once; `deleteById()` never called; `GrowthMeasurementDeleted` event published; audit log contains `GROWTH_MEASUREMENT_DELETED`

**Expected Result (PASS — hành vi đúng):**
- No exception; `deletedAt` set to a non-null Instant; single `save()` invocation; event + audit fired exactly once

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown; or `deleteById()`/hard-delete called (violates ADR-BABY-012-001); or `deletedAt` remains null

**Current Status:** 🔴 Not written
**Implementation Note:** Must call `save()`, never `deleteById()`. Entity field name must be `deletedAt` (no `status` field exists on this entity).

---

### BABY236-TC-002 — Idempotent: re-delete already soft-deleted measurement → no-op

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-BABY-IMP-012 §3 ADR-BABY-012-002`

**Preconditions:**
- Measurement exists via `makeAlreadyDeletedMeasurement()`, `deletedAt = 2026-06-15T10:00:00Z` (already set)
- Mock: `babyProfileRepository.findById(BABY_ID)` → owned baby
- Mock: `growthMeasurementRepository.findById(MEAS_ID_DELETED)` → already-deleted measurement

**Test Steps:**
1. Arrange: baby owner OK, measurement already has `deletedAt` set
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, BABY_ID, MEAS_ID_DELETED)`
3. Assert: no exception thrown; `growthMeasurementRepository.save()` NOT called; `GrowthMeasurementDeleted` NOT published; audit log NOT written a second time; `deletedAt` value unchanged (still `2026-06-15T10:00:00Z`, not overwritten with a new timestamp)

**Expected Result (PASS):**
- Silent no-op success (maps to 204 at controller layer); original `deletedAt` timestamp preserved

**Expected Result (FAIL):**
- `save()` called again (overwrites `deletedAt`); duplicate event/audit; or exception (409) thrown — violates ADR-BABY-012-002 idempotent-success decision

**Current Status:** 🔴 Not written
**Implementation Note:** Guard must check `deletedAt != null` BEFORE any mutation and return immediately.

---

### BABY236-TC-003 — Baby not owned by caller → 403 BABY-071

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-BABY-IMP-012 §10 BABY-071 (reused from CB-BABY-IMP-008), ADR-BABY-008-004`

**Preconditions:**
- Baby exists via `makeBabyOwnedByOther()` — `ownerUserId = OTHER_USER_ID`
- Mock: `babyProfileRepository.findById(BABY_ID)` → baby owned by other

**Test Steps:**
1. Arrange: baby owned by `OTHER_USER_ID`
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, BABY_ID, MEAS_ID)`
3. Assert: `ForbiddenException` thrown with code `BABY-071`; `growthMeasurementRepository.findById()` never invoked (fail fast before touching measurement)

**Expected Result (PASS):**
- `ForbiddenException` with `BABY-071` (403); no measurement mutation occurs

**Expected Result (FAIL):**
- Measurement soft-deleted despite non-ownership — RBAC violation

**Current Status:** 🔴 Not written

---

### BABY236-TC-004 — Baby not found → 404 BABY-070

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-BABY-IMP-012 §10 BABY-070 (reused from CB-BABY-IMP-008)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(NON_EXISTENT_BABY_ID)` → `Optional.empty()`

**Test Steps:**
1. Arrange: non-existent babyId
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, NON_EXISTENT_BABY_ID, MEAS_ID)`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-070`

**Expected Result (PASS):**
- `ResourceNotFoundException` with `BABY-070` (404)

**Expected Result (FAIL):**
- `NullPointerException` or unhandled error instead of a typed 404

**Current Status:** 🔴 Not written

---

### BABY236-TC-005 — Measurement not found → 404 BABY-080

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-BABY-IMP-012 §10 BABY-080 (new)`

**Preconditions:**
- Baby owned by `MOTHER_ID` (found OK)
- Mock: `growthMeasurementRepository.findById(NON_EXISTENT_MEAS_ID)` → `Optional.empty()`

**Test Steps:**
1. Arrange: valid owned baby, non-existent measurementId
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, BABY_ID, NON_EXISTENT_MEAS_ID)`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-080`

**Expected Result (PASS):**
- `ResourceNotFoundException` with `BABY-080` (404)

**Expected Result (FAIL):**
- Wrong code returned (e.g., reuses `BABY-070` for measurement instead of dedicated `BABY-080`), or unhandled exception

**Current Status:** 🔴 Not written

---

### BABY236-TC-006 — Cross-baby mismatch (measurement belongs to a different baby) → 403 BABY-081

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `GrowthMeasurementService.deleteMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementDeleteServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-BABY-IMP-012 §10 BABY-081 (new), §2 L5`

**Preconditions:**
- Path `babyId = BABY_ID` (owned by `MOTHER_ID`, exists)
- Measurement `MEAS_OTHER_BABY` exists via `makeMeasurementOfOtherBaby()` with `babyId = OTHER_BABY_ID` (not the path babyId)
- Mock: `growthMeasurementRepository.findById(MEAS_OTHER_BABY)` → measurement with mismatched `babyId`

**Test Steps:**
1. Arrange: caller owns `BABY_ID`; targets `measurementId = MEAS_OTHER_BABY` which actually belongs to `OTHER_BABY_ID`
2. Act: call `growthMeasurementService.deleteMeasurement(MOTHER_ID, BABY_ID, MEAS_OTHER_BABY)`
3. Assert: `ForbiddenException` thrown with code `BABY-081`; measurement's `deletedAt` remains unchanged (still null)

**Expected Result (PASS):**
- `ForbiddenException` with `BABY-081` (403); no mutation

**Expected Result (FAIL):**
- Measurement gets soft-deleted despite belonging to a different baby — IDOR/BOLA vulnerability (CWE-639)

**Current Status:** 🔴 Not written
**Implementation Note:** This guard is not explicit in SRS text (§2 L5) but is required to prevent cross-baby ID injection via a valid `babyId` + foreign `measurementId`.

---

### BABY236-TC-007 — No JWT → 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request headers

**Test Steps:**
1. Arrange: build DELETE request without `Authorization` header
2. Act: call `DELETE /api/v1/babies/{babyId}/growth-measurements/{measurementId}` via MockMvc
3. Assert: response status is 401; service method never invoked

**Expected Result (PASS):**
- HTTP 401 Unauthorized; no service call reaches `GrowthMeasurementService`

**Expected Result (FAIL):**
- Request reaches controller/service without authentication

**Current Status:** 🔴 Not written

---

### BABY236-TC-008 — Soft-deleted measurement excluded from history-view query (cross-reference UC-237)

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/repository/GrowthMeasurementRepositoryTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-BABY-IMP-012 §3 ADR-BABY-012-003, §8.2 (PROPOSED repository method for UC-237)`

**Preconditions:**
- `BABY_ID` has two measurements seeded: one live (`deletedAt = null`), one soft-deleted (`deletedAt = <past Instant>`)

**Test Steps:**
1. Arrange: seed 1 live + 1 soft-deleted measurement for `BABY_ID` (via `@DataJpaTest` + `TestEntityManager`, or Testcontainers)
2. Act: call `growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, Pageable.unpaged())` — **signature confirmed 2026-07-04 to match `CB-BABY-IMP-013` (UC-237) §8.2 exactly: `Page<GrowthMeasurement> ...(UUID babyId, Pageable pageable)`, not the earlier unpaginated `List<>` draft (§2 L8)**
3. Assert: result `Page.getContent()` contains exactly 1 measurement (the live one); the soft-deleted measurement's id is absent from the result

**Expected Result (PASS):**
- Result excludes the soft-deleted row — confirms the query contract that UC-237 depends on (ADR-BABY-012-003)

**Expected Result (FAIL):**
- Soft-deleted measurement appears in the "live" query result — cross-cutting requirement violated, UC-237 would leak deleted data

**Current Status:** 🔴 Not written
**Implementation Note:** This test defines the contract UC-237 must consume. If UC-237's spec is written later without reusing this exact filter, flag as a regression against ADR-BABY-012-003.

---

### SECURITY TEST CASES

---

### BABY236-TC-SEC-001 — IDOR: non-owner attempts delete via API

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC violation if guard fails — unauthorized modification of another user's health data`
**Feature Under Test:** `GrowthMeasurementController.deleteMeasurement() + GrowthMeasurementService ownership guard`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- `OTHER_MOTHER` has a valid JWT but does not own `BABY_ID`
- `BABY_ID` has a live measurement `MEAS_ID`

**Test Steps (Attack Simulation):**
1. Authenticate as `OTHER_MOTHER` (valid JWT, `ROLE_MOTHER`, not owner of `BABY_ID`)
2. Call `DELETE /api/v1/babies/{BABY_ID}/growth-measurements/{MEAS_ID}` with `OTHER_MOTHER`'s JWT
3. Check response and DB state

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden` with `code: "BABY-071"`; `MEAS_ID.deletedAt` remains `null` in DB after the attempt

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `204 No Content` returned and/or `deletedAt` set — non-owner successfully deleted another Mother's health data

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### BABY236-TC-INT-001 — Full flow: migration applied, soft-delete persists, row count unchanged, excluded from live query

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → Repository → PostgreSQL (with V20260703000100 migration)`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/GrowthMeasurementDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically on Spring context start, including proposed `V20260703000100__add_growth_measurement_deleted_at.sql`
- Seed: `baby_profiles` row for `BABY_ID` owned by `MOTHER_ID`
- Seed: 2 `growth_measurements` rows for `BABY_ID` (both live, `deleted_at IS NULL`)

**Test Steps:**
1. Seed baby + 2 measurements via JPA repository
2. Confirm `information_schema.columns` reports `deleted_at` column exists on `growth_measurements` (migration sanity check)
3. Call `DELETE /api/v1/babies/{BABY_ID}/growth-measurements/{firstMeasurementId}` with valid MOTHER JWT
4. Assert response and DB state
5. Call the same DELETE again (retry) and assert idempotent 204, no duplicate audit

**Expected Result (PASS):**
- Migration column `deleted_at` present (timestamptz, nullable)
- Step-3 response status 204
- `SELECT * FROM growth_measurements WHERE growth_measurement_id = :firstMeasurementId` still returns exactly 1 row, with `deleted_at IS NOT NULL`
- `SELECT COUNT(*) FROM growth_measurements WHERE baby_id = :BABY_ID` still equals 2 (no hard-delete — row count unchanged)
- `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, Pageable.unpaged())` returns a `Page` with exactly 1 measurement (the untouched second one)
- Step-5 retry also returns 204, and audit log contains exactly 1 `GROWTH_MEASUREMENT_DELETED` entry for that measurement (not 2)

**Expected Result (FAIL):**
- Migration column missing; row physically deleted (count decreases); soft-deleted row still appears in the live-filter query; duplicate audit entries after retry

**DB Assertion:**
```java
GrowthMeasurement deleted = growthMeasurementRepository.findById(firstMeasurementId).orElseThrow();
assertThat(deleted.getDeletedAt()).isNotNull();

long totalCount = growthMeasurementRepository.countByBabyId(BABY_ID);
assertThat(totalCount).isEqualTo(2); // no hard-delete

Page<GrowthMeasurement> liveOnly =
    growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, Pageable.unpaged());
assertThat(liveOnly.getContent()).hasSize(1);
assertThat(liveOnly.getContent()).extracting(GrowthMeasurement::getGrowthMeasurementId)
    .doesNotContain(firstMeasurementId);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `BABY236-TC-001` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-002` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-003` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-004` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-005` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-006` | `GrowthMeasurementDeleteServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-007` | `GrowthMeasurementControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-008` | `GrowthMeasurementRepositoryTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-SEC-001` | `GrowthMeasurementControllerSecurityTest.java` | `[ ]` | `[hash]` | |
| `BABY236-TC-INT-001` | `GrowthMeasurementDeleteIntegrationTest.java` | `[ ]` | `[hash]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class GrowthMeasurementService implements IGrowthMeasurementService {

    @Override
    public void deleteMeasurement(UUID userId, UUID babyId, UUID measurementId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `BABY236-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `BABY236-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-007` | `throw('Not implemented')` (via 500 from unmapped exception, not 401) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-008` | N/A — repository-level test, not gated by service stub; verify FAIL via unimplemented repository method | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY236-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-BABY-IMP-012` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Tech Lead/Principal Architect — L7/L8 đã RESOLVED 2026-07-04 (BabyProfile field bug + repository signature drift); ADR-BABY-012-004 (baby ACTIVE-required cho delete) vẫn là NEEDS-DECISION, chưa chọn phương án
- [ ] **Flyway migration `V20260703000100__add_growth_measurement_deleted_at.sql` đã được Tech Lead + DBA approve và chạy thành công trên staging** (ADR-BABY-012-001 — Proposed → Accepted)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers, migration `V20260703000100` áp dụng)
- [ ] Test coverage ≥ 80% lines cho `GrowthMeasurementService` (delete path)
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `deleteById()` KHÔNG xuất hiện bất kỳ đâu trong `GrowthMeasurementService`/`Repository` liên quan đến delete flow (grep check)
- [ ] Row count trong `growth_measurements` không đổi sau delete (verify §4 BABY236-TC-INT-001)
- [ ] Soft-deleted measurement bị loại khỏi query dùng cho UC-237 (BABY236-TC-008)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua factory `GrowthMeasurementDeleteTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/§ trong TDS)

### Suspension Criteria (Điều kiện tạm dừng)

- `ADR-BABY-012-001/002/003` vẫn ở trạng thái `Proposed`, chưa được Tech Lead + DBA Accept (blocker cứng — có schema change thật)
- Migration `V20260703000100` chưa chạy được trên staging
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files (code)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/service/GrowthMeasurementService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/service/IGrowthMeasurementService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/entity/GrowthMeasurement.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/

# Revert migration (dev only — KHÔNG chạy trên production without DBA approval)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.growth_measurements DROP COLUMN IF EXISTS deleted_at;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703000100';"

# Gap vẫn OPEN → giữ nguyên entry trong tracker cho tới khi ADR được Accepted và re-implement
```

> **Cảnh báo dữ liệu:** `DROP COLUMN deleted_at` sẽ khiến mọi measurement đã soft-delete "sống lại" (mất dấu xóa). Không mất row gốc (soft-delete không hard-delete) → rủi ro mất measurement = 0, nhưng rủi ro "un-delete" ngoài ý muốn = có. Chỉ rollback migration khi đã đánh giá tác động với DBA.

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ OK — mọi TC có `Oracle Source` | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Pending (chờ chạy Red Gate thật) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ OK — mọi hành vi (soft-delete, idempotent, cross-baby guard) trace về ADR-BABY-012-00x | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ OK — ownership/state/cross-baby logic chỉ test ở Service layer; Controller test (`TC-007`) chỉ verify auth filter | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc dùng cột `status` (không có trên `growth_measurements`) | ☑ OK — factory dùng field `deletedAt`; không dùng `getStatus()`/`setStatus()` cho `GrowthMeasurement` (xem §2 L6) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Phát hiện AP tiềm ẩn (theo dõi, chưa phải fix) → ghi vào bảng dưới → xác nhận trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| AP-AI-002 (pending) | Toàn bộ | Red Gate (§5.1) chưa được chạy thật — bảng verification vẫn để trống ô Actual | Chạy `./mvnw test` với service stub throw trước khi bắt đầu implement thật; điền kết quả vào §5.1 | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft. Không được set Approved cho tới khi ADR-BABY-012-001/002/003 được Tech Lead + DBA Accept và Red Gate (§5.1) được xác nhận FAIL thật trên stub.*
