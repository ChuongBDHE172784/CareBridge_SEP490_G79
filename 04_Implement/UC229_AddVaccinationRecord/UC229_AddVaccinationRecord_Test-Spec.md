# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-229 Add Vaccination Record — Test-Spec

**Document ID:** `CB-VAC-TDD-229`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — bảng `vaccination_records` (dòng 660–672), FK (dòng 1730–1734)
- `04_Implement/UC229_AddVaccinationRecord/UC229_AddVaccinationRecord_TDS.md` — Technical Design Spec (nguồn chính cho tài liệu này)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.2, Table 251 — SRS gốc UC-229
- `04_Implement/UC228_ViewVaccinationSchedule/UC228_ViewVaccinationSchedule_TDS.md` — sibling (đã drift, KHÔNG dùng làm nguồn kiến trúc, chỉ mã lỗi VAC-001/002 còn hợp lệ)
- Luật 91/2025 (PDPA VN) — dữ liệu cá nhân nhạy cảm (sức khỏe trẻ em)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC-229 Add Vaccination Record, dựa trên TDS `CB-VAC-IMP-229` |

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
| **Feature / Gap ID** | `UC-229` |
| **Module** | `Vaccination — Add Vaccination Record (Bounded Context: Vaccination & Growth Tracking)` |
| **Spec gốc** | `CB-VAC-IMP-229` (`04_Implement/UC229_AddVaccinationRecord/UC229_AddVaccinationRecord_TDS.md`) |
| **Priority** | 🟠 P1 *(Medium priority theo SRS Table 251, nhưng liên quan BR-SAFETY nên không hạ xuống P2)* |
| **Sprint** | `[Chưa gán — Open]` |
| **Milestone** | `[Chưa gán — Open]` |
| **Data Classification** | `Sensitive-PII` (dữ liệu sức khỏe trẻ em) |
| **Compliance Scope** | `PDPA (Luật 91/2025) — BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `baby` (`BabyProfileRepository`, `BabyAccessPolicy`), `health` (`HealthRecordRepository`, `HealthRecord` entity) |
| **Downstream Consumers** | `vaccination` read-side (`VaccinationServiceImpl.getVaccinationSchedule` — string-key merge `vaccineName|doseNumber`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-VAC-IMP-229 §17.1 (C1–C7)`, `ADR-VAC-229-001..004` |
| **Constraints Injected** | C1 (real-code ground truth, không dùng kiến trúc UC228-doc); C2 (status=COMPLETED on create); C3 (owner-only write); C4 (proof FK validation); C6 (no future administeredDate); C7 (no new migration) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Bắt buộc điền trước khi viết test. Test cases encode hành vi **đã sửa**, không phải hành vi trong spec gốc sai lệch.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy / code) | Fix áp dụng trong test |
|---|------------------------|--------------------------------|------------------------|
| L1 | TDS-doc UC228 (Approved) mô tả migration `V27__create_vaccination_tables.sql`, enum `vac_record_status{COMPLETED,POSTPONED}` (thiếu SCHEDULED), FK `reference_schedule_id`/`proof_file_id`, endpoint `/api/v1/baby-profiles/{babyId}/vaccination-schedule` | Bảng `vaccination_records` thực nằm trong `V1__init_schema.sql` (dòng 660–672); Java enum `VaccinationRecordStatus{SCHEDULED,COMPLETED,POSTPONED}`; không có FK reference_schedule; column thực là `proof_record_id` FK → `health_records` (V1 dòng 1733–1734); endpoint thực `/api/v1/vaccination/babies/{babyId}/schedule` (`VaccinationController` dòng 16, 23) | Mọi TC dùng path `/api/v1/vaccination/babies/{babyId}/records`, enum 3 giá trị, và validate FK `proof_record_id` — KHÔNG dùng đặc tả UC228-doc (xem ADR-VAC-229-001) |
| L2 | Không rõ status mặc định khi Mother thêm record — entity default là `SCHEDULED` | SRS: "Records vaccination dose, **date**, facility" = ghi sự kiện đã xảy ra; read-side chỉ hiển thị `administeredDate` khi `status==COMPLETED` (`VaccinationServiceImpl` dòng 75–77) | TC happy-path assert `status == COMPLETED` (không phải default entity `SCHEDULED`) — oracle: ADR-VAC-229-002 |
| L3 | Column `proof_record_id` + FK đã tồn tại trong schema (V1) nhưng chưa map trong `VaccinationRecord.java` — dễ nhầm là cần migration mới | Test KHÔNG assert migration mới; test assert JPA mapping field `proofRecordId` tồn tại và persist đúng cột `proof_record_id` | TC-INT khẳng định `save()` ghi đúng `proof_record_id` mà không cần Flyway thay đổi (oracle: ADR-VAC-229-004, V1 dòng 669) |
| L4 | Không rõ ai được phép ghi (owner-only vs care-group `canView`) | Write dùng owner-only + `hasRole('MOTHER')`, khác `canView` (owner+ACCEPTED member) của read-side | TC ownership-denied dùng non-owner MOTHER (không phải care-group member) → phải bị từ chối VAC-002, khác hành vi cho phép của UC-228 read |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Vaccination — Add Vaccination Record bao gồm các layer:
├── Domain (VaccinationRecord entity — pure field/state invariants)
├── Application / Services (VaccinationServiceImpl — mock BabyProfileRepository,
│    BabyAccessPolicy/owner-check, VaccinationRecordRepository, HealthRecordRepository qua Mockito)
├── Controller (VaccinationController.addVaccinationRecord — @WebMvcTest, mock IVaccinationService)
└── Integration (Testcontainers PostgreSQL — full save + FK assertion, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-229` (Table 251) | Mother ghi mũi tiêm đã thực hiện (dose, date, facility, proof optional) |
| `ADR-VAC-229-001` | Path `/api/v1/vaccination/babies/{babyId}/records`, enum 3 giá trị, string-key (không FK) cho reference |
| `ADR-VAC-229-002` | status=COMPLETED on create; administeredDate NOT NULL, không ở tương lai |
| `ADR-VAC-229-003` | Owner-only write (không dùng canView) |
| `ADR-VAC-229-004` | Proof FK validation qua `HealthRecordRepository.findById` |
| `BR-RBAC` / `BR-PRIVACY` / `BR-SAFETY` | Role MOTHER + owner; minimum-necessary; không ghi ngày tương lai |
| `CB-VAC-IMP-229 §10` | Bảng mã lỗi VAC-001..008 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy add — không có proof | `VaccinationServiceImpl.addVaccinationRecord()` | `VAC229-TC-001` |
| TC-COND-002 | Happy add — có proof hợp lệ | cùng trên + `HealthRecordRepository` | `VAC229-TC-002`, `VAC229-TC-INT-001` |
| TC-COND-003 | Baby không tồn tại | service — VAC-001 | `VAC229-TC-003` |
| TC-COND-004 | Caller không phải owner | service — VAC-002 | `VAC229-TC-004` |
| TC-COND-005 | Thiếu field bắt buộc | DTO `@Valid` — VAC-004 | `VAC229-TC-005`, `VAC229-TC-006`, `VAC229-TC-007` |
| TC-COND-006 | administeredDate ở tương lai | service — VAC-008 | `VAC229-TC-008` |
| TC-COND-007 | proofRecordId không tồn tại | service — VAC-005 | `VAC229-TC-009` |
| TC-COND-008 | proofRecordId thuộc baby khác | service — VAC-006 | `VAC229-TC-010`, `VAC229-TC-E2E-002` |
| TC-COND-009 | Duplicate COMPLETED record | service — VAC-007 | `VAC229-TC-011` |
| TC-COND-010 | scheduledDate invariant (null khi Mother-entered) | entity state | `VAC229-TC-012` |
| TC-COND-011 | Role không phải MOTHER | Controller `@PreAuthorize` | `VAC229-TC-013`, `VAC229-TC-E2E-002b` |
| TC-COND-012 | Full E2E happy path qua JWT | API | `VAC229-TC-E2E-001` |
| TC-COND-013 | Không có JWT | API/Security | `VAC229-TC-E2E-003` |
| TC-COND-014 | Read-side (UC-228) khớp đúng record vừa thêm | Integration cross-feature | `VAC229-TC-INT-002` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | vaccineName/doseNumber/administeredDate hợp lệ vs rỗng vs null | Phân vùng input hợp lệ/không hợp lệ cho `@Valid` |
| Boundary Value Analysis | administeredDate = today vs tomorrow vs yesterday | Biên `@PastOrPresent`/VAC-008 |
| State Transition Testing | VaccinationRecord (không có state trước) → COMPLETED | Xác nhận invariant "chỉ INSERT COMPLETED" (§6.5 TDS) |
| Error Guessing | proofRecordId ngẫu nhiên/của baby khác/null | Tấn công cross-tenant FK (BR-PRIVACY) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `BabyProfile{id: BABY-1, ownerUserId: MOTHER-1}` | Happy path owner |
| `FX-002` | DB seed | `BabyProfile{id: BABY-2, ownerUserId: MOTHER-2}` | Ownership-denied / cross-tenant proof |
| `FX-003` | DB seed | `HealthRecord{id: HR-1, babyId: BABY-1, ownerUserId: MOTHER-1, recordType: VACCINATION_FORM}` | Proof hợp lệ |
| `FX-004` | DB seed | `HealthRecord{id: HR-2, babyId: BABY-2, ownerUserId: MOTHER-2}` | Proof cross-tenant (invalid) |
| `FX-005` | DB seed | `VaccinationRecord{babyId: BABY-1, vaccineName:"BCG", doseNumber:1, status:COMPLETED}` | Duplicate-check seed |
| `FX-006` | JWT | `{sub: MOTHER-1, role: MOTHER}` | Auth context happy path |
| `FX-007` | JWT | `{sub: MOTHER-2, role: MOTHER}` | Auth context ownership-denied |
| `FX-008` | JWT | `{sub: FAMILY-1, role: FAMILY}` | Non-MOTHER role rejection |

---

## 4. Test Case Specification

> **TC ID format:** `VAC229-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

// VaccinationTestFactory.java
class VaccinationTestFactory {

    // Baseline baby owner — đồng bộ FX-001
    static BabyProfile makeBaby() {
        return makeBaby(b -> {});
    }

    static BabyProfile makeBaby(Consumer<BabyProfile> overrides) {
        BabyProfile baby = BabyProfile.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000000b1"))
                .ownerUserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"))
                .nickname("Bé Bống")
                .birthDate(LocalDate.of(2025, 1, 1))
                .build();
        overrides.accept(baby);
        return baby;
    }

    // Baseline request — đồng bộ FX request happy path
    static AddVaccinationRecordRequest makeRequest() {
        return makeRequest(r -> {});
    }

    static AddVaccinationRecordRequest makeRequest(Consumer<AddVaccinationRecordRequest> overrides) {
        AddVaccinationRecordRequest req = new AddVaccinationRecordRequest();
        req.setVaccineName("BCG");
        req.setDoseNumber((short) 1);
        req.setAdministeredDate(LocalDate.now().minusDays(1));
        req.setFacilityName("VNVC Hoàng Văn Thụ");
        req.setProofRecordId(null);
        overrides.accept(req);
        return req;
    }

    // Health record fixture cho proof linkage — đồng bộ FX-003/FX-004
    static HealthRecord makeHealthRecord(UUID babyId, UUID ownerUserId) {
        return HealthRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000000h1"))
                .babyId(babyId)
                .ownerUserId(ownerUserId)
                .recordType(RecordType.VACCINATION_FORM)
                .title("Giấy chứng nhận tiêm BCG")
                .build();
    }

    // VaccinationRecord đã có sẵn — dùng cho duplicate-check (FX-005)
    static VaccinationRecord makeExistingRecord(UUID babyId) {
        return VaccinationRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000000v1"))
                .babyId(babyId)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .administeredDate(LocalDate.now().minusDays(30))
                .status(VaccinationRecordStatus.COMPLETED)
                .build();
    }
}
```

---

### VAC229-TC-001 — Happy add without proof → status COMPLETED

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-VAC-229-002` (TDS §3) — record tạo mới PHẢI có `status=COMPLETED`, `administeredDate` NOT NULL

**Preconditions:**
- FX-001 (`makeBaby()`, ownerUserId = callerId)
- FX request: `makeRequest()` (proofRecordId = null)

**Test Steps:**
1. Arrange: mock `babyRepository.findById(BABY-1)` → `Optional.of(makeBaby())`; mock `recordRepository.save(any())` trả lại argument
2. Act: gọi `addVaccinationRecord(BABY-1, makeRequest(), callerId=MOTHER-1)`
3. Assert: response/entity `status == COMPLETED`; `administeredDate` = request date; `scheduledDate == null`; `proofRecordId == null`

**Expected Result (PASS):** Response `201`-equivalent object với `status="COMPLETED"`, `recordRepository.save` được gọi đúng 1 lần với entity đúng field.
**Expected Result (FAIL):** status vẫn là default `SCHEDULED`, hoặc `administeredDate` bị null.

**Current Status:** 🔴 Not written
**Implementation Note:** Service phải override `@Builder.Default` bằng cách set `status(COMPLETED)` tường minh khi build entity.

---

### VAC229-TC-002 — Happy add with valid proof → proofRecordId persisted

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-VAC-229-004` — proof FK validation, V1 dòng 669/1733-1734

**Preconditions:**
- FX-001, FX-003 (`HealthRecord{id:HR-1, babyId:BABY-1, ownerUserId:MOTHER-1}`)
- Request: `makeRequest(r -> r.setProofRecordId(HR-1))`

**Test Steps:**
1. Arrange: mock baby found + owner OK; mock `healthRecordRepository.findByIdAndStatus(HR-1, ACTIVE)` → `Optional.of(makeHealthRecord(BABY-1, MOTHER-1))`
2. Act: `addVaccinationRecord(BABY-1, request, MOTHER-1)`
3. Assert: saved entity có `proofRecordId == HR-1`; không exception ném ra

**Expected Result (PASS):** `proofRecordId` echo đúng trong response; `healthRecordRepository.findByIdAndStatus` được gọi đúng 1 lần (real method — xem `HealthRecordRepository.java`).
**Expected Result (FAIL):** proof bị bỏ qua (null) dù request có gửi, hoặc exception sai ném ra.

**Current Status:** 🔴 Not written

---

### VAC229-TC-003 — Baby not found → VAC-001/404

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-VAC-IMP-229 §10` — VAC-001 (reused từ read-side, cùng ngữ nghĩa 404 baby not found)

**Preconditions:** `babyRepository.findById(any())` → `Optional.empty()`

**Test Steps:**
1. Act: `addVaccinationRecord(NON_EXISTENT_ID, makeRequest(), callerId)`
2. Assert: `BusinessException` với `httpStatus=404`, `code="VAC-001"`

**Expected Result (PASS):** Exception đúng code/status, `recordRepository.save` KHÔNG được gọi.
**Expected Result (FAIL):** NullPointerException hoặc code sai.

**Current Status:** 🔴 Not written

---

### VAC229-TC-004 — Ownership denied (non-owner) → VAC-002/403

**Severity:** `CRITICAL`
**CWE:** `CWE-284 — Improper Access Control`
**Legal:** `PDPA — BR-PRIVACY minimum-necessary access`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-VAC-229-003` — owner-only write, tái dùng mã VAC-002

**Preconditions:** FX-001 (`ownerUserId=MOTHER-1`); caller = `MOTHER-2` (không phải owner, không phải care-group member)

**Test Steps:**
1. Arrange: `babyRepository.findById(BABY-1)` → baby với `ownerUserId=MOTHER-1`
2. Act: `addVaccinationRecord(BABY-1, makeRequest(), callerId=MOTHER-2)`
3. Assert: `BusinessException(403, "VAC-002")`; `recordRepository.save` KHÔNG được gọi

**Expected Result (PASS):** Access bị từ chối đúng code, không có side-effect ghi dữ liệu.
**Expected Result (FAIL):** Record vẫn được lưu (lỗ hổng bảo mật nghiêm trọng — ghi đè dữ liệu y tế của baby không thuộc quyền).

**Current Status:** 🔴 Not written

---

### VAC229-TC-005 — Missing vaccineName → VAC-004/400

**Severity:** `MEDIUM`
**Feature Under Test:** `AddVaccinationRecordRequest` `@Valid` / Controller`@WebMvcTest`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-229 §8.1` — `@NotBlank` trên `vaccineName`

**Preconditions:** JWT MOTHER hợp lệ (FX-006)

**Test Steps:**
1. Act: `POST /api/v1/vaccination/babies/{babyId}/records` với `vaccineName=""` (hoặc thiếu field)
2. Assert: `400`, `error.code="VAC-004"`, `details[].field="vaccineName"`

**Expected Result (PASS):** 400 với message field-level.
**Expected Result (FAIL):** 500 hoặc request được chấp nhận với vaccineName rỗng.

**Current Status:** 🔴 Not written

---

### VAC229-TC-006 — Missing doseNumber → VAC-004/400

**Severity:** `MEDIUM`
**Feature Under Test:** `AddVaccinationRecordRequest` `@Valid`
**Test File:** `VaccinationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-229 §8.1` — `@NotNull @Min(1)` trên `doseNumber`

**Test Steps:**
1. Act: POST với `doseNumber=null`
2. Assert: `400`, `code="VAC-004"`, `details[].field="doseNumber"`

**Expected Result (PASS):** 400 đúng field.
**Expected Result (FAIL):** doseNumber null được lưu vào DB (column cho phép null — nhưng business rule bắt buộc).

**Current Status:** 🔴 Not written

---

### VAC229-TC-007 — Missing administeredDate → VAC-004/400

**Severity:** `MEDIUM`
**Feature Under Test:** `AddVaccinationRecordRequest` `@Valid`
**Test File:** `VaccinationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-229 §8.1` — `@NotNull @PastOrPresent` trên `administeredDate`

**Test Steps:**
1. Act: POST với `administeredDate=null`
2. Assert: `400`, `code="VAC-004"`, `details[].field="administeredDate"`

**Expected Result (PASS):** 400 đúng field.
**Expected Result (FAIL):** Record lưu với `administeredDate=null`, vi phạm invariant §6.5 TDS.

**Current Status:** 🔴 Not written

---

### VAC229-TC-008 — administeredDate ở tương lai → VAC-008/400

**Severity:** `HIGH`
**Legal:** `BR-SAFETY (SRS Table 251)`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-VAC-229-002` + `CB-VAC-IMP-229 §10` VAC-008

**Preconditions:** FX-001; request `administeredDate = LocalDate.now().plusDays(1)`

**Test Steps:**
1. Act: `addVaccinationRecord(BABY-1, request, MOTHER-1)`
2. Assert: `BusinessException(400, "VAC-008")`

**Expected Result (PASS):** Reject đúng mã, `save` không được gọi.
**Expected Result (FAIL):** Record ở tương lai được lưu — sai lệch lịch tiêm tương lai (rủi ro BR-SAFETY: mis-scheduling downstream).

**Current Status:** 🔴 Not written
**Implementation Note:** `@PastOrPresent` trên DTO chỉ chặn *tương lai theo giờ hệ thống lúc validate*; cần double-check ở service nếu muốn message riêng biệt VAC-008 (khác VAC-004 generic).

---

### VAC229-TC-009 — proofRecordId không tồn tại → VAC-005/404

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-VAC-229-004` + `CB-VAC-IMP-229 §10` VAC-005

**Preconditions:** FX-001; request `proofRecordId = UUID ngẫu nhiên không tồn tại`; `healthRecordRepository.findByIdAndStatus(..., ACTIVE)` → `Optional.empty()`

**Test Steps:**
1. Act: `addVaccinationRecord(BABY-1, request, MOTHER-1)`
2. Assert: `BusinessException(404, "VAC-005")`

**Expected Result (PASS):** Reject đúng mã, không insert record.
**Expected Result (FAIL):** Record được lưu với `proof_record_id` trỏ tới hàng không tồn tại → vi phạm FK, DB sẽ throw constraint violation không kiểm soát (leak lỗi 500 thay vì 404 sạch).

**Current Status:** 🔴 Not written

---

### VAC229-TC-010 — proofRecordId thuộc baby khác → VAC-006/403 (cross-tenant)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — BR-PRIVACY`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-VAC-229-004` + `CB-VAC-IMP-229 §10` VAC-006

**Preconditions:** FX-001 (BABY-1/MOTHER-1); FX-004 (`HealthRecord{HR-2, babyId:BABY-2, ownerUserId:MOTHER-2}`)

**Test Steps:**
1. Arrange: request cho BABY-1 với `proofRecordId=HR-2`
2. Act: `addVaccinationRecord(BABY-1, request, MOTHER-1)`
3. Assert: `BusinessException(403, "VAC-006")`

**Expected Result (PASS):** Reject, record KHÔNG được insert.
**Expected Result (FAIL — lỗ hổng nghiêm trọng):** Vaccination record của BABY-1 bị liên kết tới hồ sơ y tế của BABY-2 → rò rỉ tham chiếu chéo dữ liệu sức khỏe giữa các gia đình khác nhau.

**Current Status:** 🔴 Not written

---

### VAC229-TC-011 — Duplicate COMPLETED record → VAC-007/409

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()`
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-VAC-IMP-229 §10` VAC-007; repository method `findByBabyIdAndVaccineNameAndDoseNumberAndStatus` (existing, `VaccinationRecordRepository.java` dòng 15–16)

**Preconditions:** FX-005 (existing `VaccinationRecord{BABY-1, "BCG", dose 1, COMPLETED}`)

**Test Steps:**
1. Arrange: mock `recordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus(BABY-1, "BCG", 1, COMPLETED)` → `Optional.of(existingRecord)`
2. Act: `addVaccinationRecord(BABY-1, makeRequest(), MOTHER-1)` (cùng vaccine+dose)
3. Assert: `BusinessException(409, "VAC-007")`

**Expected Result (PASS):** Reject đúng mã, `save` không gọi thêm lần 2.
**Expected Result (FAIL):** Hai record COMPLETED trùng lặp cùng baby+vaccine+dose → read-side merge (`Collectors.toMap` với `(a,b)->a`, `VaccinationServiceImpl` dòng 58) sẽ âm thầm bỏ qua 1 record mà không báo lỗi.

**Current Status:** 🔴 Not written

---

### VAC229-TC-012 — scheduledDate luôn null cho Mother-entered record (state invariant)

**Severity:** `LOW`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()` — entity construction
**Test File:** `VaccinationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-VAC-IMP-229 §6.5` (State Machine invariant) + ADR-VAC-229-002 hệ quả

**Test Steps:**
1. Act: `addVaccinationRecord(BABY-1, makeRequest(), MOTHER-1)` (happy path)
2. Assert: entity được save có `scheduledDate == null`

**Expected Result (PASS):** `scheduledDate` không bị set nhầm từ `administeredDate`.
**Expected Result (FAIL):** `scheduledDate` bị gán giá trị sai, gây nhầm lẫn với luồng reference-schedule tự sinh của UC-228.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### VAC229-TC-013 — Role khác MOTHER bị từ chối (Spring Security)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `VaccinationController.addVaccinationRecord()` — `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** JWT với role `FAMILY` (FX-008)

**Test Steps (Attack Simulation):**
1. Chuẩn bị JWT role=FAMILY (không phải MOTHER)
2. `POST /api/v1/vaccination/babies/{babyId}/records` với JWT đó
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` do `@PreAuthorize` chặn trước khi vào service.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request được xử lý, family member ghi được dữ liệu y tế của baby không có quyền ghi.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### VAC229-TC-INT-001 — Full flow: add with proof persists FK correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST request → Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/AddVaccinationRecordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Preconditions:**
- PostgreSQL Testcontainer chạy, Flyway migration (V1 + V20260627100500) áp dụng tự động
- Seed: `BabyProfile` (FX-001), `HealthRecord` (FX-003)

**Test Steps:**
1. Seed baby + health record proof
2. `POST /api/v1/vaccination/babies/{babyId}/records` với `proofRecordId=HR-1`
3. Assert DB: `SELECT * FROM vaccination_records WHERE baby_id = BABY-1`

**Expected Result (PASS):**
- 1 row mới, `status='COMPLETED'`, `proof_record_id = HR-1`, `administered_date` NOT NULL
- Response 201 với đúng field

**Expected Result (FAIL):** FK violation (nếu proof-record logic sai) hoặc row thiếu field.

**DB Assertion:**
```java
VaccinationRecord record = recordRepository.findAllByBabyId(babyId).get(0);
assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);
assertThat(record.getProofRecordId()).isEqualTo(healthRecordId);
assertThat(record.getAdministeredDate()).isNotNull();
```

**Current Status:** 🔴 Not written

---

### VAC229-TC-INT-002 — Cross-feature: record vừa thêm khớp đúng ở UC-228 read-side

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.addVaccinationRecord()` → `VaccinationServiceImpl.getVaccinationSchedule()` (string-key merge)
**Test File:** `AddVaccinationRecordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Seed reference schedule entry `{vaccineName:"BCG", doseNumber:1, offsetDays:0}` (V20260627100500 hoặc test seed)

**Test Steps:**
1. `POST .../records` thêm `{vaccineName:"BCG", doseNumber:1, administeredDate: today}`
2. `GET /api/v1/vaccination/babies/{babyId}/schedule`
3. Assert dose "BCG"/1 trong response có `status="COMPLETED"` và `administeredDate` đúng

**Expected Result (PASS):** Read-side merge đúng theo key `vaccineName|doseNumber` (`VaccinationServiceImpl` dòng 56, 70).
**Expected Result (FAIL):** Dose vẫn hiển thị `SCHEDULED`/`OVERDUE` — chứng tỏ key mismatch (vd sai kiểu Short vs int trong key string).

**Current Status:** 🔴 Not written
**Implementation Note:** `VaccinationDoseDto.doseNumber` là `int` còn `VaccinationRecord.doseNumber` là `Short` — key string phải serialize nhất quán (`"BCG|1"` cả hai phía) để tránh lệch kiểu.

---

### E2E TEST CASES

---

### VAC229-TC-E2E-001 — Full E2E happy path qua JWT

**Severity:** `HIGH`
**Feature Under Test:** `Full flow qua API thực (JWT hợp lệ)`
**Test File:** `AddVaccinationRecordIntegrationTest.java` (hoặc E2E suite riêng)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Test Steps:**
```gherkin
Given user MOTHER (FX-006) đã đăng nhập, có JWT hợp lệ, owner của BABY-1
When POST /api/v1/vaccination/babies/BABY-1/records được gọi với:
  | Header          | Value              |
  | Authorization   | Bearer [token]     |
  | Content-Type    | application/json   |
Then response status là 201
And response.data.status == "COMPLETED"
And database chứa record mới với baby_id = BABY-1
```

**Expected Result (PASS):** 201 + DB row.
**Expected Result (FAIL):** Bất kỳ mismatch giữa response và DB state.

**Current Status:** 🔴 Not written

---

### VAC229-TC-E2E-002 — Cross-tenant proof rejection qua API thực

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** Full API — `POST /api/v1/vaccination/babies/{babyId}/records`
**Test File:** `AddVaccinationRecordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Test Steps:**
```gherkin
Given HealthRecord HR-2 thuộc BABY-2 (không phải BABY-1)
When POST /api/v1/vaccination/babies/BABY-1/records với proofRecordId=HR-2, JWT của MOTHER-1 (owner BABY-1)
Then response status là 403
And response body chứa error code "VAC-006"
And KHÔNG có record mới trong vaccination_records
```

**Current Status:** 🔴 Not written

---

### VAC229-TC-E2E-003 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security filter chain
**Test File:** `AddVaccinationRecordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Test Steps:**
```gherkin
When POST /api/v1/vaccination/babies/BABY-1/records không có header Authorization
Then response status là 401
And response body chứa error code "IAM-001" (theo pattern chung của hệ thống)
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VAC229-TC-001` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-002` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-003` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-004` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-005` | `VaccinationControllerTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-006` | `VaccinationControllerTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-007` | `VaccinationControllerTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-008` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-009` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-010` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-011` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-012` | `VaccinationServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-013` | `VaccinationControllerTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-INT-001` | `AddVaccinationRecordIntegrationTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-INT-002` | `AddVaccinationRecordIntegrationTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-E2E-001` | `AddVaccinationRecordIntegrationTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-E2E-002` | `AddVaccinationRecordIntegrationTest.java` | `[ ]` | `[ ]` | |
| `VAC229-TC-E2E-003` | `AddVaccinationRecordIntegrationTest.java` | `[ ]` | `[ ]` | |

**TC count:** 18 (13 unit/security + 2 integration + 3 E2E). **Critical count:** 5 (`VAC229-TC-001`, `VAC229-TC-004`, `VAC229-TC-010`, `VAC229-TC-013`, `VAC229-TC-E2E-002`).

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class VaccinationServiceImpl implements IVaccinationService {

    @Override
    public VaccinationScheduleResponse getVaccinationSchedule(UUID babyProfileId, UUID callerId) {
        // existing implementation — không đổi
        ...
    }

    @Override
    public AddVaccinationRecordResponse addVaccinationRecord(
            UUID babyId, AddVaccinationRecordRequest request, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// Controller stub tương ứng
@PostMapping("/babies/{babyId}/records")
@PreAuthorize("hasRole('MOTHER')")
public ResponseEntity<ApiResponse<AddVaccinationRecordResponse>> addVaccinationRecord(
        @PathVariable UUID babyId,
        @Valid @RequestBody AddVaccinationRecordRequest request,
        Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VAC229-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-005` | DTO `@Valid` fires BEFORE stub (Controller layer) | 🔴 FAIL (400, not the stub exception) | ☐ FAIL ☐ PASS | ☐ Validation bypassed |
| `VAC229-TC-006` | tương tự TC-005 | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-007` | tương tự TC-005 | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-013` | `@PreAuthorize` fires BEFORE stub | 🔴 FAIL (403, not the stub exception) | ☐ FAIL ☐ PASS | ☐ Security bypassed |
| `VAC229-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-E2E-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-E2E-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC229-TC-E2E-003` | Security filter fires BEFORE stub | 🔴 FAIL (401, not the stub exception) | ☐ FAIL ☐ PASS | ☐ Auth filter bypassed |

**Red Gate Evidence:**
- Stub commit hash: `___ (pending — chưa implement)`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log — pending]`

> **Nếu bất kỳ test PASS bất thường** (vd TC-005/006/007/013/E2E-003 pass vì lý do KHÁC với validation/security — tức code path thực sự chạy qua service stub mà không throw): dừng lại, xác định root cause, rewrite test.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-VAC-IMP-229` đã được review và approve (hiện tại: Draft — pending)
- [ ] Logic Issues (§2 trên) đã được confirm với Principal Architect
- [ ] KHÔNG cần Flyway migration mới (§5.2 TDS xác nhận column+FK đã có trong V1) — bỏ qua bước "migration approved on staging"
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị (FX-001..FX-008)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả 18 test cases xanh (không skip)
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers, TC-INT-001/002)
- [ ] Test coverage ≥ 80% lines cho `VaccinationServiceImpl.addVaccinationRecord()`
- [ ] Không có business logic trong `VaccinationController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Cả 5 CRITICAL TCs (`TC-001`, `TC-004`, `TC-010`, `TC-013`, `TC-E2E-002`) PASS trước khi coi feature là "done"

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 18 tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi (mọi class/DTO trong §8 TDS tồn tại)
- [ ] **Props Isolation** — mọi test dùng `VaccinationTestFactory.makeX()`, không biến static mutable dùng chung
- [ ] **Oracle Source** — mọi TC ở §4 có `Oracle Source` trỏ về ADR/BR/error-code cụ thể (đã điền 100%)

### Suspension Criteria

- `BabyAccessPolicy.isOwner()` (RESOLVED trong TDS ADR-VAC-229-003 — canonical method định nghĩa bởi UC-230 ADR-VAC-005) — không còn block; test service layer mock/gọi `isOwner()` trực tiếp
- Health-records proof upload UX (OPEN-2 trong TDS, câu hỏi chia sẻ với UC-232 OPEN-4) chưa xác nhận — không block test service/API layer (chỉ ảnh hưởng mobile UX)
- CI pipeline broken bởi thay đổi khác ngoài phạm vi UC-229

---

## 7. Rollback Plan

```bash
# KHÔNG có migration để revert cho UC-229 (không tạo bảng/cột mới — xem TDS §5.2, §12.2).

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/

# Nếu đã thêm JPA mapping proofRecordId và cần revert riêng field đó:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/entity/VaccinationRecord.java

# Gap vẫn OPEN → giữ nguyên Status: Draft trong TDS/Test-Spec cho tới khi re-approved
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC ở §4 có `Oracle Source` | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ chạy Red Gate thực tế (chưa implement) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — dùng ADR-VAC-229-001..004 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — TC-005/006/007/013 chỉ test validation/security ở layer đúng | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — `AddVaccinationRecordRequest/Response`, `VaccinationRecordMapper` là `<<planned>>` khai báo rõ trong TDS §5.1, chưa import như thể đã tồn tại | G-3 |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (AP-AI-002 chờ Red Gate thực thi sau khi có stub code — không thể xác nhận bằng document review) → TDD spec approved *(pending Red Gate execution + Tech Lead sign-off)*

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết spec)_ | — | — | — | — |

---

*Test-Spec v1.0 — UC-229 Add Vaccination Record. Status: Draft. Không set Approved cho tới khi Tech Lead + DPO ký duyệt.*
