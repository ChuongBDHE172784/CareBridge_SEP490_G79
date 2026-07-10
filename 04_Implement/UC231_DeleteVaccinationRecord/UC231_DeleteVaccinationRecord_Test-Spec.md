# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-231 Delete Vaccination Record — Test Specification

**Document ID:** `CB-VAC-TDD-231`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (9/14 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending` *(bắt buộc — module xử lý Sensitive-PII sức khỏe trẻ em)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC231_DeleteVaccinationRecord/UC231_DeleteVaccinationRecord_TDS.md` (`CB-VAC-IMP-231`) — Technical Design Specification (this Test-Spec's primary basis)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.4 (Table 253) — Functional requirement (UC-231)
- `04_Implement/UC215_DeleteReminder/UC215_DeleteReminder_TDS.md` §ADR-REM-DELETE-002 — idempotent re-delete precedent (reused)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/` — REAL CODE ground truth
- PDPA (Luật 91/2025) — legal basis cho Sensitive-PII sức khỏe trẻ em

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-10` | `AI Agent` | Truthful sync after implementation evidence: status set to Partially Implemented, 9/14 tests PASS in VaccinationServiceImplTest; Red Gate not reconstructed because implementation pre-existed; controller/INT/E2E remain pending |
| `2026-07-03` | `AI Agent — Test Designer` | Khởi tạo tài liệu — TDD spec cho UC-231 Delete Vaccination Record, dựa trên `CB-VAC-IMP-231` (Draft). |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-231` |
| **Module** | `Vaccination & Growth Tracking — Delete Vaccination Record` |
| **Spec gốc** | `CB-VAC-IMP-231` (`04_Implement/UC231_DeleteVaccinationRecord/UC231_DeleteVaccinationRecord_TDS.md`) |
| **Priority** | 🟠 P1 *(SRS Priority = Medium; treated P1 due to Sensitive-PII delete + auth surface)* |
| **Sprint** | `Open — chưa gán sprint cụ thể` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` *(dữ liệu tiêm chủng trẻ sơ sinh)* |
| **Compliance Scope** | `PDPA (Luật 91/2025)` + `BR-RBAC` + `BR-PRIVACY` |
| **Upstream Dependencies** | `BabyProfileRepository`, `BabyAccessPolicy` (module `baby`), `SecurityUtils`, `BusinessException`, `ApiResponse` (module `common`) |
| **Downstream Consumers** | UC-228 View Vaccination Schedule (`getVaccinationSchedule` phải loại `DELETED`); Audit/Notification context (event `VaccinationRecordDeleted`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-VAC-IMP-231 §17` (Constraint Injection Block C1–C5); ADR-VAC-DELETE-001..004 |
| **Constraints Injected** | (1) Soft-delete BẮT BUỘC, cấm hard delete; (2) Owner-only 2-tier auth (VAC-002 → VAC-013); (3) Idempotent re-delete = 204 no-op, không phát event trùng; (4) `getVaccinationSchedule` phải lọc `DELETED` trước index; (5) Identity từ `SecurityUtils.requireCurrentUserId`, response 204 không chứa nội dung y tế |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate — §5.1)` |

---

## 2. Logic Issues Resolved

> Điền trước khi viết test. Liệt kê mọi sai lệch giữa spec thiết kế (TDS) và schema/policy/codebase thực tế mà test cases phải encode như hành vi **đã sửa**.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Sibling TDS `UC228_ViewVaccinationSchedule` (Approved) không phản ánh đúng REAL CODE hiện tại của `VaccinationController`/`VaccinationServiceImpl` | REAL CODE: `VaccinationController` chỉ có `GET /babies/{babyId}/schedule`; `VaccinationRecordStatus` chỉ `{SCHEDULED, COMPLETED, POSTPONED}`; `VaccinationRecord` không có `createdBy` | Test cases KHÔNG assert theo UC-228 stale; assert theo REAL CODE trích dẫn tại `VaccinationServiceImpl.java` dòng 36–101 và `VaccinationRecordStatus.java` |
| L2 | `VaccinationRecordStatus` không có giá trị soft-delete sẵn có (`DELETED`/`CANCELLED`/`ARCHIVED`) | Enum chỉ `{SCHEDULED, COMPLETED, POSTPONED}`; cột `status` là `VARCHAR(20)` (`@Enumerated(EnumType.STRING)`), không phải Postgres enum type | TC giả định enum ĐÃ được mở rộng thêm `DELETED` (ADR-VAC-DELETE-001, `Proposed`) — test viết theo hành vi kỳ vọng SAU khi enum được thêm; KHÔNG migration nào được test (không có Flyway thay đổi) |
| L3 | `getVaccinationSchedule()` dùng chuỗi `if (COMPLETED) … else if (POSTPONED) … else …` — **không exhaustive** đối với giá trị `DELETED` mới | Một record `DELETED` sẽ rơi vào nhánh `else` (SCHEDULED/OVERDUE) nếu không lọc trước — bug "xóa nhưng vẫn hiện" | `VAC231-TC-012` bắt buộc: sau soft-delete, record phải bị loại khỏi `recordMap` TRƯỚC khi vào merge logic (ADR-VAC-DELETE-004); assert dose trở về trạng thái suy ra từ reference catalog, không phải trạng thái cũ của record đã xóa |
| L4 | `VaccinationRecord` không có cột xác định "Mother-entered" hay owner trực tiếp | Ownership suy ra qua `baby.ownerUserId` (bảng `baby_profiles`), không phải trên `vaccination_records` | Test dùng `BabyProfile.ownerUserId` làm oracle cho "owner", không giả định field không tồn tại trên `VaccinationRecord` |
| L5 | Chưa có repository method để nạp record kèm ràng buộc `babyId` | REAL CODE hiện chỉ có `findAllByBabyId` và `findByBabyIdAndVaccineNameAndDoseNumberAndStatus` | Test giả định method mới `findByIdAndBabyId(recordId, babyId)` (TDS §8.2) tồn tại; nếu record tồn tại nhưng thuộc baby khác → `VAC-014` (không phải `VAC-001`) |
| L6 | Idempotency của re-delete chưa có tiền lệ nội bộ module vaccination | `04_Implement/UC215_DeleteReminder/UC215_DeleteReminder_TDS.md` §ADR-REM-DELETE-002 đã chọn 204 no-op cho reminder | `VAC231-TC-004` áp dụng CÙNG hành vi (204, không event lặp) để nhất quán toàn app |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Vaccination — Delete Vaccination Record (UC-231) bao gồm các layer:
├── Domain (VaccinationRecordStatus enum — pure logic, no deps)
├── Service (VaccinationServiceImpl.deleteVaccinationRecord — mock JPA Repository + BabyAccessPolicy với Mockito)
├── Service (VaccinationServiceImpl.getVaccinationSchedule — regression: filter DELETED)
├── Controller (VaccinationController.deleteVaccinationRecord — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full DELETE → GET schedule round-trip)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS §3.3.19.4 (Table 253) | Soft-delete (không hard delete); actor Mother; BR-RBAC + BR-PRIVACY; Frequency Occasional |
| `CB-VAC-IMP-231` §3 ADR-VAC-DELETE-001 | Enum mới `DELETED`, không Flyway migration |
| `CB-VAC-IMP-231` §3 ADR-VAC-DELETE-002 | Owner-only 2-tier auth (VAC-002 rồi VAC-013) |
| `CB-VAC-IMP-231` §3 ADR-VAC-DELETE-003 | Idempotent re-delete = 204 no-op |
| `CB-VAC-IMP-231` §3 ADR-VAC-DELETE-004 | Exhaustiveness fix — filter DELETED trong `getVaccinationSchedule` |
| `CB-VAC-IMP-231` §10 | Error codes VAC-001/002 (reused) + VAC-013..016 |
| BR-RBAC / BR-PRIVACY / PDPA | Least privilege; minimum-necessary access; audit trail retention |
| UI mockup CB-276 | Copy xác nhận xóa, checkbox bắt buộc, success state (mã giao dịch + audit timestamp) — oracle cho UX, không cho API contract |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner soft-deletes record ở mọi status non-DELETED | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-001, 002, 003` |
| TC-COND-002 | Re-delete record đã `DELETED` | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-004` |
| TC-COND-003 | Care-group member (canView=true, không owner) bị từ chối | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-005` |
| TC-COND-004 | Người ngoài (canView=false) bị từ chối, không leak tồn tại | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-006` |
| TC-COND-005 | Baby/record không tồn tại | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-007, 008` |
| TC-COND-006 | Record tồn tại nhưng thuộc baby khác | `VaccinationRecordRepository.findByIdAndBabyId` | `VAC231-TC-009` |
| TC-COND-007 | UUID sai định dạng | `VaccinationController.deleteVaccinationRecord()` | `VAC231-TC-010` |
| TC-COND-008 | Lỗi persistence bất ngờ | `VaccinationServiceImpl.deleteVaccinationRecord()` | `VAC231-TC-011` |
| TC-COND-009 | Record `DELETED` bị loại khỏi lịch (UC-228 cross-check) | `VaccinationServiceImpl.getVaccinationSchedule()` | `VAC231-TC-012` |
| TC-COND-010 | Response không rò rỉ nội dung y tế/PII | API contract (204 body) | `VAC231-TC-013` |
| TC-COND-011 | Unauthenticated request | `VaccinationController` `@PreAuthorize` | `VAC231-TC-014` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Trạng thái `status` (SCHEDULED/COMPLETED/POSTPONED đều → DELETED) | Đại diện cho mọi lifecycle state được phép xóa (firm decision — bất kỳ status non-DELETED nào cũng xóa được, BR-PRIVACY "incorrectly entered data" rationale) |
| Boundary Value Analysis | Record đã `DELETED` (biên trạng thái terminal) | Kiểm tra idempotency tại chính biên terminal |
| State Transition Testing | `VaccinationRecordStatus` state machine (§6.3 TDS) | Xác nhận `DELETED` là terminal, không transition ngược trừ re-delete no-op |
| Error Guessing | UUID sai định dạng, record thuộc baby khác, canView race | Các case dễ bị bỏ sót khi implement nhanh |
| Access Control Testing | 2-tier auth (canView → owner check) | Cốt lõi BR-RBAC/BR-PRIVACY của UC này |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed (BabyProfile) | `{ id: BABY-01, ownerUserId: MOTHER-01, nickname: 'Bé Bin' }` | Owner baseline |
| `FX-002` | DB seed (VaccinationRecord) | `{ id: REC-01, babyId: BABY-01, vaccineName: 'BCG', doseNumber: 1, status: COMPLETED, administeredDate: 2023-10-15 }` | Happy path delete |
| `FX-003` | DB seed (VaccinationRecord) | `{ id: REC-02, babyId: BABY-01, status: SCHEDULED }` | Boundary: xóa record chưa hoàn thành |
| `FX-004` | DB seed (VaccinationRecord) | `{ id: REC-03, babyId: BABY-01, status: POSTPONED }` | Boundary: xóa record đã hoãn |
| `FX-005` | DB seed (VaccinationRecord) | `{ id: REC-04, babyId: BABY-01, status: DELETED }` | Idempotent re-delete |
| `FX-006` | DB seed (CareGroupMember) | `{ careGroupId: BABY-01, userId: FAMILY-01, inviteStatus: ACCEPTED }` | Member — canView=true, không owner |
| `FX-007` | DB seed (BabyProfile + VaccinationRecord khác) | `{ id: BABY-02, ownerUserId: MOTHER-02 }` + `REC-05 (babyId=BABY-02)` | Mismatch: record thuộc baby khác |
| `FX-008` | JWT | `{ sub: MOTHER-01, role: MOTHER }` | Auth context — owner |
| `FX-009` | JWT | `{ sub: FAMILY-01, role: FAMILY }` | Auth context — member non-owner |
| `FX-010` | JWT | `{ sub: STRANGER-01, role: MOTHER }` (không liên quan baby) | Auth context — người ngoài |
| `FX-011` | VaccinationReferenceSchedule seed | `{ vaccineName: 'BCG', doseNumber: 1, offsetDays: 0 }` | Oracle cho merge logic UC-228 sau khi xóa |

---

## 4. Test Case Specification

> **TC ID format:** `VAC231-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written (toàn bộ — Red Gate chưa chạy)

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory, không shared mutable state
// ═══════════════════════════════════════════════════════════

// VaccinationRecordTestFactory.java
class VaccinationRecordTestFactory {

    static final UUID BABY_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BABY2_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-00000000m001");
    static final UUID MOTHER2_ID= UUID.fromString("00000000-0000-0000-0000-00000000m002");
    static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-00000000f001");

    // Baseline hợp lệ — đồng bộ với FX-001
    static BabyProfile makeBaby() {
        return makeBaby(b -> {});
    }

    static BabyProfile makeBaby(Consumer<BabyProfile> overrides) {
        BabyProfile baby = BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Bé Bin")
                .birthDate(LocalDate.of(2023, 8, 1))
                .build();
        overrides.accept(baby);
        return baby;
    }

    // Baseline hợp lệ — đồng bộ với FX-002
    static VaccinationRecord makeRecord() {
        return makeRecord(r -> {});
    }

    static VaccinationRecord makeRecord(Consumer<VaccinationRecord> overrides) {
        VaccinationRecord record = VaccinationRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000rec1"))
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .status(VaccinationRecordStatus.COMPLETED)
                .administeredDate(LocalDate.of(2023, 10, 15))
                .build();
        overrides.accept(record);
        return record;
    }

    // Overload cho care-group member (FX-006)
    static CareGroupMember makeAcceptedMember(UUID babyId, UUID userId) {
        CareGroupMember member = new CareGroupMember();
        member.setCareGroupId(babyId);
        member.setUserId(userId);
        member.setInviteStatus(InviteStatus.ACCEPTED);
        return member;
    }
}
```

---

### VAC231-TC-001 — Owner soft-deletes a COMPLETED record (happy path)

**Severity:** `CRITICAL`
**Legal:** `BR-PRIVACY, PDPA`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-VAC-IMP-231 §11.3 Chặng 2` (service pseudocode) + ADR-VAC-DELETE-001 (soft-delete via `status=DELETED`) + ADR-VAC-DELETE-002 (owner-only)

**Preconditions:**
- FX-001 (BabyProfile, owner=MOTHER-01), FX-002 (VaccinationRecord REC-01, status=COMPLETED)

**Test Steps:**
1. Arrange: mock `babyRepository.findById(BABY_ID)` → FX-001; mock `accessPolicy.canView(baby, MOTHER_ID)` → true; mock `recordRepository.findByIdAndBabyId(REC-01, BABY_ID)` → FX-002
2. Act: gọi `service.deleteVaccinationRecord(BABY_ID, REC-01, MOTHER_ID)`
3. Assert: `recordRepository.save(...)` được gọi đúng 1 lần với `record.getStatus() == DELETED`; `eventPublisher.publishEvent(...)` gọi đúng 1 lần với `VaccinationRecordDeleted` chứa `previousStatus=COMPLETED`

**Expected Result (PASS):**
- Method trả về (void) không throw; `save()` invoked once với status=DELETED; event published once

**Expected Result (FAIL — dấu hiệu lỗi):**
- Record bị xóa cứng (`deleteById`/`delete()` gọi thay vì `save()`) → vi phạm C1 (§17 TDS)
- Hoặc `save()` không được gọi / status không đổi

**Current Status:** 🟢 Passing
**Implementation Note:** Không dùng `recordRepository.delete()`/`deleteById()` — chỉ `save()` sau khi `setStatus(DELETED)`.

---

### VAC231-TC-002 — Owner soft-deletes a SCHEDULED record

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-VAC-IMP-231 §6.3` State Machine — *"Firm decision: MỌI status non-DELETED đều xóa được"* (BR-PRIVACY "incorrectly entered data" rationale, parallel UC-236)

**Preconditions:** FX-001, FX-003 (REC-02, status=SCHEDULED)

**Test Steps:**
1. Arrange như TC-001 nhưng record = FX-003
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-02, MOTHER_ID)`
3. Assert: status chuyển `DELETED`, không phụ thuộc lifecycle state ban đầu

**Expected Result (PASS):** Xóa thành công bất kể status = SCHEDULED (chưa hoàn thành)
**Expected Result (FAIL):** Service từ chối xóa vì record "chưa hoàn thành" (giả định sai — không có precondition nào giới hạn theo status trong ADR)
**Current Status:** 🟢 Passing

---

### VAC231-TC-003 — Owner soft-deletes a POSTPONED record

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-VAC-IMP-231 §6.3` State Machine (POSTPONED → DELETED transition)

**Preconditions:** FX-001, FX-004 (REC-03, status=POSTPONED)

**Test Steps:**
1. Arrange như TC-001 nhưng record = FX-004
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-03, MOTHER_ID)`
3. Assert: status chuyển `DELETED`

**Expected Result (PASS):** Xóa thành công từ POSTPONED
**Expected Result (FAIL):** Exception không mong muốn hoặc status không đổi
**Current Status:** 🟢 Passing

---

### VAC231-TC-004 — Re-delete an already-DELETED record is idempotent (204 no-op)

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-VAC-IMP-231 §3 ADR-VAC-DELETE-003` — reused từ `UC215_DeleteReminder_TDS.md §ADR-REM-DELETE-002` (idempotent success, no-op, không phát lại event)

**Preconditions:** FX-001, FX-005 (REC-04, status=DELETED)

**Test Steps:**
1. Arrange: `recordRepository.findByIdAndBabyId` trả về FX-005 (đã DELETED)
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-04, MOTHER_ID)`
3. Assert: `recordRepository.save(...)` **KHÔNG** được gọi; `eventPublisher.publishEvent(...)` **KHÔNG** được gọi; method trả về bình thường (không throw)

**Expected Result (PASS):** Không side-effect nào (no UPDATE, no event); controller layer trả `204 No Content`
**Expected Result (FAIL):** `save()` hoặc `publishEvent()` bị gọi lại (audit trùng) — vi phạm C3; hoặc method throw lỗi 409 (sai lựa chọn — phải là no-op theo ADR)
**Current Status:** 🟢 Passing
**Implementation Note:** Guard đầu hàm: `if (rec.getStatus() == DELETED) return;` — trước khi set status/save/publish.

---

### VAC231-TC-005 — Care-group member (non-owner) is denied deletion

**Severity:** `CRITICAL`
**CWE:** `CWE-284 — Improper Access Control`
**Legal:** `BR-RBAC, BR-PRIVACY (minimum-necessary)`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-VAC-IMP-231 §3 ADR-VAC-DELETE-002` + `§10` (VAC-013, 403)

**Preconditions:** FX-001 (owner=MOTHER-01), FX-006 (FAMILY-01 ACCEPTED member), FX-002 (REC-01)

**Test Steps:**
1. Arrange: `accessPolicy.canView(baby, FAMILY_ID)` → true (member); `baby.getOwnerUserId()` = MOTHER_ID ≠ FAMILY_ID
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-01, FAMILY_ID)`
3. Assert: throws `BusinessException` với `httpStatus=403`, `code="VAC-013"`; `recordRepository.save()` không được gọi

**Expected Result (PASS):** `BusinessException(403, VAC-013, "Delete restricted to the baby owner")`
**Expected Result (FAIL):** Xóa thành công (thiếu tầng owner check) hoặc trả nhầm mã lỗi (vd VAC-002)
**Current Status:** 🟢 Passing

---

### VAC231-TC-006 — Non-member (stranger) is denied — no existence leak

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC, BR-PRIVACY`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-VAC-IMP-231 §3 ADR-VAC-DELETE-002` + `§10` (VAC-002, 403, reused từ UC-228)

**Preconditions:** FX-001, FX-010 (JWT của STRANGER-01, không quan hệ gì với baby)

**Test Steps (Attack Simulation):**
1. Arrange: `accessPolicy.canView(baby, STRANGER_ID)` → false
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-01, STRANGER_ID)`
3. Assert: throws `BusinessException(403, "VAC-002", ...)` **trước khi** kiểm tra owner; response không tiết lộ record có tồn tại hay không

**Expected Result (PASS = an toàn):** `403 VAC-002`, cùng response shape dù record tồn tại hay không (không leak)
**Expected Result (FAIL = lỗ hổng):** Trả `404` khác biệt cho case "record không tồn tại" vs "record tồn tại nhưng không được xem" theo cách lộ thông tin, hoặc bỏ qua canView check
**Current Status:** 🟢 Passing

---

### VAC231-TC-007 — Baby not found

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-231 §10` (VAC-001, 404, reused — nhất quán với `getVaccinationSchedule` dòng 39–41 REAL CODE)

**Preconditions:** `babyRepository.findById(NON_EXISTENT_BABY_ID)` → `Optional.empty()`

**Test Steps:**
1. Act: `service.deleteVaccinationRecord(NON_EXISTENT_BABY_ID, ANY_REC_ID, MOTHER_ID)`
2. Assert: throws `BusinessException(404, "VAC-001", ...)`

**Expected Result (PASS):** `404 VAC-001`
**Expected Result (FAIL):** NPE hoặc lỗi 500 không kiểm soát
**Current Status:** 🟢 Passing

---

### VAC231-TC-008 — Vaccination record not found

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-231 §10` (VAC-001, 404)

**Preconditions:** FX-001 exists; `recordRepository.findByIdAndBabyId(NON_EXISTENT_REC_ID, BABY_ID)` → `Optional.empty()`

**Test Steps:**
1. Arrange: baby found, canView=true, owner=true
2. Act: `service.deleteVaccinationRecord(BABY_ID, NON_EXISTENT_REC_ID, MOTHER_ID)`
3. Assert: throws `BusinessException(404, "VAC-001", ...)`

**Expected Result (PASS):** `404 VAC-001`
**Expected Result (FAIL):** Lỗi khác mã hoặc không throw
**Current Status:** 🟢 Passing

---

### VAC231-TC-009 — Record exists but belongs to a different baby

**Severity:** `HIGH`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `VaccinationRecordRepository.findByIdAndBabyId()` + `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-VAC-IMP-231 §8.2` (repository interface) + `§10` (VAC-014, 404)

**Preconditions:** FX-007 (BABY-02, REC-05 thuộc BABY-02); caller là owner của BABY-01

**Test Steps (Attack Simulation):**
1. Arrange: caller cung cấp `babyId=BABY_ID` (BABY-01, của chính họ) nhưng `recordId=REC-05` (thực thuộc BABY-02)
2. Act: `service.deleteVaccinationRecord(BABY_ID, REC-05, MOTHER_ID)` — `findByIdAndBabyId(REC-05, BABY_ID)` trả `Optional.empty()` vì ràng buộc babyId không khớp
3. Assert: throws `BusinessException(404, "VAC-014"...)` — **không** cho phép xóa record của baby khác qua path param sai lệch

**Expected Result (PASS = an toàn):** `404 VAC-014`, record BABY-02 không bị ảnh hưởng
**Expected Result (FAIL = lỗ hổng):** Record của BABY-02 bị xóa nhầm (IDOR — Insecure Direct Object Reference)
**Current Status:** 🔴 Not written

---

### VAC231-TC-010 — Invalid UUID format in path

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationController.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-VAC-IMP-231 §10` (VAC-015, 400)

**Preconditions:** none (input-level validation, @WebMvcTest, service mocked)

**Test Steps:**
1. Act: `DELETE /api/v1/vaccination/babies/not-a-uuid/records/{recordId}`
2. Assert: response `400`, body `{"error":{"code":"VAC-015",...}}`

**Expected Result (PASS):** `400 VAC-015`
**Expected Result (FAIL):** `500` không kiểm soát (unhandled `MethodArgumentTypeMismatchException`)
**Current Status:** 🔴 Not written

---

### VAC231-TC-011 — Unexpected persistence failure rolls back cleanly

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationServiceImpl.deleteVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-VAC-IMP-231 §10` (VAC-016, 500) + `§11.3` (`@Transactional` on delete method)

**Preconditions:** FX-001, FX-002; `recordRepository.save(...)` mock throws `DataAccessException`

**Test Steps:**
1. Act: `service.deleteVaccinationRecord(BABY_ID, REC-01, MOTHER_ID)`
2. Assert: exception propagates (mapped to `VAC-016`/500 at a higher layer); `eventPublisher.publishEvent(...)` **KHÔNG** được gọi (event chỉ phát sau khi save thành công)

**Expected Result (PASS):** Không event nào bị publish khi save fail; transaction rollback (no partial state)
**Expected Result (FAIL):** Event được publish trước khi persistence xác nhận thành công (race điều kiện dữ liệu không nhất quán)
**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### VAC231-TC-012 — Soft-deleted record is excluded from schedule view (UC-228 cross-check)

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `DELETE .../records/{recordId}` → `GET .../schedule`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-VAC-IMP-231 §3 ADR-VAC-DELETE-004` (exhaustiveness fix) — trực tiếp bảo vệ chống bug được ghi nhận tại L3 (§2)

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- FX-011 (VaccinationReferenceSchedule seed: BCG dose 1, offsetDays=0)
- FX-001 (BabyProfile owner=MOTHER-01, birthDate set)
- FX-002 (REC-01: BCG dose 1, status=COMPLETED, administeredDate=2023-10-15) — record này hiện đang surface trong `GET /schedule` là `COMPLETED`

**Test Steps:**
1. Seed FX-001, FX-002, FX-011
2. Gọi `GET /api/v1/vaccination/babies/{babyId}/schedule` (baseline) — assert dose "BCG|1" có `status=COMPLETED`, `administeredDate=2023-10-15`
3. Gọi `DELETE /api/v1/vaccination/babies/{babyId}/records/{REC-01}` với JWT MOTHER-01 — assert `204`
4. Gọi lại `GET /api/v1/vaccination/babies/{babyId}/schedule`
5. Assert: dose "BCG|1" **không còn** `status=COMPLETED`/`administeredDate` của record đã xóa; trạng thái phải suy ra lại từ reference catalog + `expectedDate` (SCHEDULED hoặc OVERDUE tùy `today` so với `expectedDate`)

**Expected Result (PASS):**
- Row `vaccination_records` cho REC-01 vẫn tồn tại trong DB với `status='DELETED'` (không bị xóa cứng)
- Response GET schedule không phản ánh dữ liệu của record đã xóa (bug từ L3 §2 đã được fix)

**Expected Result (FAIL — dấu hiệu lỗi ADR-VAC-DELETE-004 chưa áp dụng):**
- Dose "BCG|1" vẫn hiển thị `COMPLETED`/`administeredDate` cũ (record `DELETED` rơi vào nhánh `else` của `getVaccinationSchedule`'s if/else chain và bị index nhầm) — đây CHÍNH LÀ rủi ro exhaustiveness đã ghi trong TDS §3 ADR-VAC-DELETE-004

**DB Assertion:**
```java
VaccinationRecord record = recordRepository.findById(rec01Id).orElseThrow();
assertThat(record).isNotNull(); // row KHÔNG bị xóa cứng
assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.DELETED);
```

**Current Status:** 🔴 Not written
**Implementation Note:** Trong `getVaccinationSchedule()`, filter `records.stream().filter(r -> r.getStatus() != VaccinationRecordStatus.DELETED)` TRƯỚC khi `.collect(Collectors.toMap(...))` (dòng ~54 REAL CODE hiện tại).

---

### SECURITY / E2E TEST CASES

---

### VAC231-TC-013 — 204 response contains no PII / medical advice

**Severity:** `MEDIUM`
**OWASP:** `A01:2021 — Broken Access Control` *(indirect — data minimization)*
**Legal:** `BR-SAFETY (inherited), PDPA`
**Feature Under Test:** `VaccinationController.deleteVaccinationRecord()` — response contract
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-VAC-IMP-231 §9.2` (204 No Content, no body) + BR-SAFETY inherited constraint (C5, §17)

**Preconditions:** FX-001, FX-002, valid MOTHER JWT

**Test Steps:**
1. Act: `DELETE /api/v1/vaccination/babies/{babyId}/records/{recordId}`
2. Assert: HTTP status `204`; response body length = 0 (empty)

**Expected Result (PASS = an toàn):** `204`, body rỗng — không dữ liệu y tế/PII nào bị echo lại
**Expected Result (FAIL = lỗ hổng):** Response chứa vaccine name/facility/ngày tiêm hoặc bất kỳ tư vấn y tế nào
**Current Status:** 🔴 Not written

---

### VAC231-TC-014 — Unauthenticated request is rejected

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `VaccinationController` `@PreAuthorize("isAuthenticated()")`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** REAL CODE — `@PreAuthorize("isAuthenticated()")` pattern đã dùng cho `getVaccinationSchedule` (dòng 24, `VaccinationController.java`), tái dùng nguyên vẹn cho endpoint delete mới (`CB-VAC-IMP-231 §9.1`)

**Preconditions:** không có `Authorization` header

**Test Steps (Attack Simulation):**
1. Act: `DELETE /api/v1/vaccination/babies/{babyId}/records/{recordId}` không kèm JWT
2. Assert: response `401 Unauthorized`

**Expected Result (PASS = an toàn):** `401`
**Expected Result (FAIL = lỗ hổng):** Request được xử lý (thiếu `@PreAuthorize` trên method mới)
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VAC231-TC-001` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-002` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-003` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-004` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-005` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-006` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-007` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-008` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-009` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-010` | `VaccinationControllerDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-011` | `VaccinationServiceImplDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-012` | `VaccinationDeleteIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-013` | `VaccinationControllerDeleteTest.java` | `[ ]` | `[ ]` | — |
| `VAC231-TC-014` | `VaccinationControllerDeleteTest.java` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected**.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class VaccinationServiceImpl implements IVaccinationService {

    @Override
    public VaccinationScheduleResponse getVaccinationSchedule(UUID babyProfileId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void deleteVaccinationRecord(UUID babyProfileId, UUID recordId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VAC231-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `VAC231-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-010` | `throw('Not implemented')` (via controller → 500) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC231-TC-014` | N/A — `@PreAuthorize` gate fires before service call, still expect 401 (test targets filter chain, not stub) | 🔴 FAIL *(compile-time, method not yet routed)* | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `N/A - Red Gate not reconstructed because production implementation already existed before this execution.`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(điền sau khi chạy)*

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-VAC-IMP-231` đã được review và approve (hiện `Draft`)
- [ ] **ADR-VAC-DELETE-001** (thêm enum `DELETED`) đã được Tech Lead sign-off (hiện `Proposed`)
- [ ] **ADR-VAC-DELETE-004** (filter DELETED khỏi merge UC-228) đã được xác nhận đi kèm
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] Không có Flyway migration cần chạy (xác nhận §5.2 TDS — không migration)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (14 TCs, không skip)
- [ ] `./mvnw verify` — integration test `VAC231-TC-012` xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `VaccinationServiceImpl` (bao gồm nhánh mới)
- [ ] Không có business logic trong `VaccinationController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `getVaccinationSchedule()` đã cập nhật filter `DELETED` (ADR-VAC-DELETE-004) — verify bằng `VAC231-TC-012`
- [ ] Không có `DELETE FROM`/`deleteById()`/`delete()` nào được dùng cho vaccination record (grep xác nhận)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 14 tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — `findByIdAndBabyId`, `VaccinationRecordDeleted`, `VaccinationRecordStatus.DELETED` tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory-based, xem §4 boilerplate)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã điền ở mỗi TC)

### Suspension Criteria (Điều kiện tạm dừng)

- ADR-VAC-DELETE-001 chưa được Tech Lead sign-off (Proposed) — implementation bị block
- DPO chưa sign-off cơ chế soft-delete (Sensitive-PII)
- Phát hiện thêm điểm phân nhánh khác trên `VaccinationRecordStatus` ngoài `getVaccinationSchedule()` cần rà soát bổ sung (§2 L3)

---

## 7. Rollback Plan

```bash
# KHÔNG có Flyway migration cho UC-231 (§5.2 TDS) — chỉ revert mã.
git checkout -- src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java
git checkout -- src/main/java/com/carebridge/backend/vaccination/service/IVaccinationService.java
git checkout -- src/main/java/com/carebridge/backend/vaccination/service/impl/VaccinationServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/vaccination/entity/VaccinationRecordStatus.java
git checkout -- src/main/java/com/carebridge/backend/vaccination/repository/VaccinationRecordRepository.java
git checkout -- src/main/java/com/carebridge/backend/vaccination/event/VaccinationRecordDeleted.java
git checkout -- src/test/java/com/carebridge/backend/vaccination/

# Data remediation (nếu record bị soft-delete nhầm trong quá trình test manual trên staging):
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE vaccination_records SET status='SCHEDULED' WHERE vaccination_record_id='<id>' AND status='DELETED';"

# Gap vẫn OPEN → giữ nguyên entry trong sprint backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ *(không phát hiện — mọi TC có Oracle Source)* | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(chưa chạy — pending Red Gate)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (vd giả định hard-delete, hoặc bỏ qua filter DELETED) | ☐ *(không phát hiện — mọi assumption trace về ADR-VAC-DELETE-001..004)* | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ *(không phát hiện — controller TCs chỉ verify HTTP status/routing/@PreAuthorize)* | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (vd `VaccinationRecord.createdBy` — KHÔNG tồn tại) | ☐ *(đã rà — §2 L4 ghi rõ field không tồn tại, factory không dùng field này)* | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở thời điểm viết spec (pre-Red-Gate) → TDD spec sẵn sàng cho Red Gate
- [ ] Phát hiện AP sau khi chạy Red Gate thực tế → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(none yet — điền sau khi Red Gate chạy)* | — | — | — | ☐ |

---

*TDD Spec v1.0 — UC-231 Delete Vaccination Record. Status: Draft. Dựa trên `CB-VAC-IMP-231` (Draft).*
