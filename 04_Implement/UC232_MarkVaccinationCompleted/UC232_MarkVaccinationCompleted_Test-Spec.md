# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test-Spec — UC-232 Mark Vaccination Completed

**Document ID:** `CB-VAC-TDD-005`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (7/13 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC232_MarkVaccinationCompleted/UC232_MarkVaccinationCompleted_TDS.md` (CB-VAC-IMP-005) — Technical Design Specification (this feature's TDS)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` §660-672, §1730-1734, §1609-1610 — primary schema oracle (`vaccination_records` table, FKs, indexes)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/service/impl/VaccinationServiceImpl.java` §36-101 — read-side merge logic (oracle for dual-path necessity)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/policy/BabyAccessPolicy.java` — authorization oracle
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.5 Table 254 — UC-232 functional spec
- `03_Design/UI_UX/MobileAppScreen/CB-277 Mark Vaccination as Completed (UC-232)/code.html` — UI field oracle (administeredDate, facilityName)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-10` | `AI Agent` | Truthful sync after implementation evidence: status set to Partially Implemented, 7/13 tests PASS in VaccinationServiceImplTest; Red Gate not reconstructed because implementation pre-existed; controller/INT/E2E remain pending |
| `2026-07-03` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC-232 Mark Vaccination Completed |

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
| **Feature / Gap ID** | `GAP-UC232` |
| **Module** | `MarkVaccinationCompleted — vaccination bounded context` |
| **Spec gốc** | `CB-VAC-IMP-005` (UC232 TDS) |
| **Priority** | 🟠 P1 |
| **Sprint** | `S[N] (2026-07-03 → TBD)` — `Open` (chưa gán sprint) |
| **Milestone** | `M3 Alpha` — `Open` (chưa xác nhận) |
| **Data Classification** | `PII` (lịch sử tiêm chủng của trẻ) |
| **Compliance Scope** | `PDPA` · `BR-RBAC` · `BR-PRIVACY` |
| **Upstream Dependencies** | `baby` (BabyProfile, BabyAccessPolicy), `health` (health_records — proof), `vaccination` (VaccinationReferenceSchedule) |
| **Downstream Consumers** | UC-228 View Vaccination Schedule (read-side merge sẽ thấy status COMPLETED sau khi test này pass) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-VAC-IMP-005 §17.2 Constraint Injection Block`, `ADR-VAC-005/006/007/008` |
| **Constraints Injected** | C1 dual-path lookup+branch; C2 no migration (reuse `proof_record_id`); C3 `BabyAccessPolicy.isOwner` authz (owner-only, updated from `canView` — see TDS ADR-VAC-006 revision); C4 identity from `SecurityUtils.requireCurrentUserId`; C5 409/404/422 validation order before persist |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` is the final persistence oracle; ERD/sibling TDS is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|---------------------------|--------------------------|
| L1 | Sibling `UC228_ViewVaccinationSchedule_TDS.md` (Approved) mô tả vaccination module như module mới không có ràng buộc string-key hiển thị rõ | REAL CODE: `VaccinationServiceImpl` merge dùng string key `vaccineName+"\|"+doseNumber` (§54-58, §70) — một dose "SCHEDULED"/"OVERDUE" hiển thị trên UI có thể **KHÔNG có** `vaccination_records` row | Test phải cover **CẢ HAI** nhánh: update-existing row (`VAC232-TC-001`) VÀ create-new row khi không có row nào (`VAC232-TC-002`) — không giả định luôn có row sẵn |
| L2 | Naive design (nếu chỉ hỗ trợ 1 path) sẽ implicit assume record luôn tồn tại trước khi complete → 404 giả cho mọi "SCHEDULED" virtual entry | `VaccinationRecordRepository` KHÔNG có unique constraint `(baby_id, vaccine_name, dose_number)` trong V1 — nên lookup phải tìm record bất kể status (kể cả COMPLETED) để tránh insert trùng | `VAC232-TC-003` (đã COMPLETED → reject 409) dùng derived query mới `findByBabyIdAndVaccineNameAndDoseNumber` (không filter status) làm oracle của test setup |
| L3 | `VaccinationRecord.java` entity (code hiện tại) KHÔNG map cột `proof_record_id` dù cột + FK đã tồn tại trong `V1__init_schema.sql` §669, §1733-1734 | Entity cần thêm field `proofRecordId` — đây là thay đổi code bổ sung (KHÔNG migration) | `VAC232-TC-006` xác nhận `proofRecordId` được set khi hợp lệ; `VAC232-TC-007` xác nhận reject khi proof thuộc baby khác (422 VAC-020) |
| L4 | SRS Table 254 không chỉ rõ error code cụ thể (chỉ mô tả E1/E2/E3 chung chung) | TDS UC-232 gán `VAC-001/002` (reused từ UC-228) + `VAC-017..020` (dành riêng, không đụng VAC-004..016/021..024 của sibling UC) | Mọi TC dưới đây cite đúng error code theo TDS §10, không tự đặt code mới |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
MarkVaccinationCompleted bao gồm các layer:
├── Domain (VaccinationRecord entity + VaccinationRecordStatus — pure logic)
├── Service (VaccinationCompletionServiceImpl — mock JPA Repository với Mockito)
├── Controller (VaccinationController.markCompleted — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — full dual-path persistence + FK proof_record_id)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-232 Table 254` | Actor=Mother; "marks a planned vaccination as completed and links it to a vaccination record" → dual-path scope |
| `CB-VAC-IMP-005 ADR-VAC-005` | Dual-path (update vs insert) — core test dimension |
| `CB-VAC-IMP-005 ADR-VAC-006` | Authorization via `BabyAccessPolicy.isOwner` — owner-only (updated; no longer reuses read-side `canView`) |
| `CB-VAC-IMP-005 ADR-VAC-007` | proof_record_id reuse — proof validation tests |
| `CB-VAC-IMP-005 ADR-VAC-008` | Schema authority = V1 — no migration test, entity mapping only |
| BR-RBAC / BR-PRIVACY | Ownership-denied and PII-minimization tests |
| `V1__init_schema.sql` | Persistence assertions (columns, FK, default status) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|-----------------|----------------|-------------|
| TC-COND-001 | Existing SCHEDULED/POSTPONED row → UPDATE to COMPLETED | `VaccinationCompletionServiceImpl.markCompleted()` path (a) | `VAC232-TC-001` |
| TC-COND-002 | No existing row (virtual entry) → INSERT new COMPLETED row | `markCompleted()` path (b) | `VAC232-TC-002` |
| TC-COND-003 | Existing row already COMPLETED → reject | `markCompleted()` guard | `VAC232-TC-003` |
| TC-COND-004 | Caller is not the baby owner | `BabyAccessPolicy.isOwner` false branch | `VAC232-TC-004` |
| TC-COND-005 | Baby profile not found | `babyRepository.findById` empty | `VAC232-TC-005` |
| TC-COND-006 | Valid `proofRecordId` (same baby) accepted | `validateProof()` happy | `VAC232-TC-006` |
| TC-COND-007 | Invalid/foreign `proofRecordId` rejected | `validateProof()` reject | `VAC232-TC-007` |
| TC-COND-008 | `vaccineName+doseNumber` absent from reference catalog | reference-catalog guard | `VAC232-TC-008` |
| TC-COND-009 | Missing/invalid required input fields | `@Valid` on `MarkVaccinationCompletedRequest` | `VAC232-TC-009` |
| TC-COND-010 | Unauthenticated request | Spring Security filter chain | `VAC232-TC-010` |
| TC-COND-011 | Full E2E persistence (path b) via Testcontainers | Controller→Service→Repository→DB | `VAC232-TC-INT-001` |
| TC-COND-012 | Full E2E persistence (path a) via Testcontainers | Controller→Service→Repository→DB | `VAC232-TC-INT-002` |
| TC-COND-013 | No duplicate row created for same key (INV-1) | Repository dual-path lookup | `VAC232-TC-011` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|--------------------------|------------|-----------|
| Equivalence Partitioning | Record-exists vs record-absent (path a/b) | Two distinct persistence branches per ADR-VAC-005 |
| Boundary Value Analysis | `doseNumber` (< 1 invalid), `administeredDate` (future vs today vs past) | `@Min(1)` and `@PastOrPresent` constraints |
| State Transition Testing | `VaccinationRecordStatus` FSM (SCHEDULED/POSTPONED → COMPLETED; COMPLETED → reject) | §6.4 State Machine invariant INV-2 |
| Error Guessing | IDOR via `proofRecordId` from a different baby's health_record | BR-PRIVACY / minimum-necessary access |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|----------------|----------|
| `FX-001` | DB seed | `BabyProfile{id=B1, ownerUserId=M1, birthDate=2026-01-01}` | Baseline owner baby |
| `FX-002` | DB seed | `VaccinationRecord{babyId=B1, vaccineName="DTP-VGB-Hib", doseNumber=1, status=SCHEDULED}` | Path (a) update target |
| `FX-003` | DB seed | `VaccinationReferenceSchedule{vaccineName="BCG", doseNumber=1, offsetDays=0}` (no matching record row for B1) | Path (b) virtual entry |
| `FX-004` | DB seed | `VaccinationRecord{babyId=B1, vaccineName="BCG", doseNumber=1, status=COMPLETED}` | Already-completed reject case |
| `FX-005` | DB seed | `HealthRecord{id=H1, babyId=B1}` | Valid proof target |
| `FX-006` | DB seed | `HealthRecord{id=H2, babyId=B2}` (different baby) | Invalid/foreign proof target |
| `FX-007` | JWT | `{sub: "M1", role: "MOTHER"}` | Owner caller auth context |
| `FX-008` | JWT | `{sub: "M2", role: "MOTHER"}` (not owner, not ACCEPTED member of B1) | Ownership-denied caller |

---

## 4. Test Case Specification

> **TC ID format:** `VAC232-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

// VaccinationCompletionTestFactory.java
class VaccinationCompletionTestFactory {

    static final UUID BABY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID NON_MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    // Baseline baby — đồng bộ FX-001
    static BabyProfile makeBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .birthDate(LocalDate.of(2026, 1, 1))
                .nickname("Bé Test")
                .build();
    }

    static BabyProfile makeBaby(Consumer<BabyProfile.BabyProfileBuilder> overrides) {
        var b = BabyProfile.builder().id(BABY_ID).ownerUserId(OWNER_ID).birthDate(LocalDate.of(2026,1,1));
        overrides.accept(b);
        return b.build();
    }

    // Existing SCHEDULED record — đồng bộ FX-002 (path a target)
    static VaccinationRecord makeScheduledRecord() {
        return VaccinationRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000000b1"))
                .babyId(BABY_ID)
                .vaccineName("DTP-VGB-Hib")
                .doseNumber((short) 1)
                .status(VaccinationRecordStatus.SCHEDULED)
                .build();
    }

    static VaccinationRecord makeRecord(Consumer<VaccinationRecord> overrides) {
        VaccinationRecord r = makeScheduledRecord();
        overrides.accept(r);
        return r;
    }

    // Already-COMPLETED record — đồng bộ FX-004
    static VaccinationRecord makeCompletedRecord() {
        return VaccinationRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-0000000000b2"))
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .status(VaccinationRecordStatus.COMPLETED)
                .administeredDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    // Reference catalog entry — đồng bộ FX-003
    static VaccinationReferenceSchedule makeReference(String vaccineName, int doseNumber) {
        return VaccinationReferenceSchedule.builder()
                .vaccineName(vaccineName)
                .doseNumber((short) doseNumber)
                .offsetDays(0)
                .build();
    }

    // Request DTO — valid baseline
    static MarkVaccinationCompletedRequest makeRequest() {
        MarkVaccinationCompletedRequest req = new MarkVaccinationCompletedRequest();
        req.setVaccineName("BCG");
        req.setDoseNumber((short) 1);
        req.setAdministeredDate(LocalDate.of(2026, 7, 1));
        return req;
    }

    static MarkVaccinationCompletedRequest makeRequest(Consumer<MarkVaccinationCompletedRequest> overrides) {
        MarkVaccinationCompletedRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    // Valid proof health_record — đồng bộ FX-005
    static HealthRecord makeProofRecord(UUID id, UUID babyId) {
        return HealthRecord.builder().id(id).babyId(babyId).build();
    }
}
```

---

### VAC232-TC-001 — Path (a): Update existing SCHEDULED record to COMPLETED

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-VAC-IMP-005 ADR-VAC-005 §Decision` (dual-path core decision) + `VaccinationRecord.java` entity fields (§existing) ← expected fields/status values from ADR-VAC-005 and entity definition, not AI assumption.

**Preconditions:**
- `FX-001` baby B1 owned by M1
- `FX-002` existing `VaccinationRecord` (B1, "DTP-VGB-Hib", dose 1, status=SCHEDULED)
- Caller = M1 (owner)

**Test Steps:**
1. Arrange: mock `babyRepository.findById(B1)` → `makeBaby()`; mock `accessPolicy.isOwner` → true; mock `referenceRepository` catalog contains ("DTP-VGB-Hib",1); mock `recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(B1,"DTP-VGB-Hib",1)` → `Optional.of(makeScheduledRecord())`.
2. Act: call `markCompleted(B1, makeRequest(r -> {r.setVaccineName("DTP-VGB-Hib"); r.setDoseNumber((short)1); r.setAdministeredDate(LocalDate.of(2026,7,1));}), M1)`.
3. Assert: `recordRepository.save()` called exactly once with an entity whose `status == COMPLETED` and `administeredDate == 2026-07-01`; response `created == false`; no `INSERT`-style save (same entity id as `makeScheduledRecord()`).

**Expected Result (PASS):**
- Response `VaccinationCompletionResponse{status="COMPLETED", created=false, vaccinationRecordId=<same as FX-002 id>}`
- `recordRepository.save()` invoked once (UPDATE semantics)

**Expected Result (FAIL — dấu hiệu lỗi):**
- A new record is created instead (duplicate), OR `created=true`, OR `administeredDate` not persisted

**Current Status:** 🟢 Passing
**Implementation Note:** Lookup must be status-agnostic (`findByBabyIdAndVaccineNameAndDoseNumber`, no status filter) so SCHEDULED/POSTPONED rows are found for update.

---

### VAC232-TC-002 — Path (b): Create new COMPLETED record for virtual/synthesized entry

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `VaccinationServiceImpl.java` §65-94 (read-side merge logic — proves a "SCHEDULED"/"OVERDUE" dose on the schedule view may have NO `vaccination_records` row) + `CB-VAC-IMP-005 ADR-VAC-005 §Options Considered / Option A`.

**Preconditions:**
- `FX-001` baby B1 owned by M1
- `FX-003` reference catalog has ("BCG", dose 1); **no** `VaccinationRecord` exists for (B1, "BCG", 1)
- Caller = M1 (owner)

**Test Steps:**
1. Arrange: mock `recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(B1,"BCG",1)` → `Optional.empty()`; mock `referenceRepository` catalog contains ("BCG",1); mock `recordRepository.save(any())` to return a persisted entity with generated id.
2. Act: call `markCompleted(B1, makeRequest(), M1)` (vaccineName="BCG", doseNumber=1, administeredDate=2026-07-01).
3. Assert: `recordRepository.save()` called with a **new** `VaccinationRecord` (no pre-existing id) whose `babyId=B1`, `vaccineName="BCG"`, `doseNumber=1`, `status=COMPLETED`, `administeredDate=2026-07-01`.

**Expected Result (PASS):**
- Response `created == true`, `status == "COMPLETED"`, HTTP mapping → 201 (verified at controller/integration layer, see `VAC232-TC-INT-001`)

**Expected Result (FAIL):**
- `BusinessException` thrown (404) treating the virtual entry as "not found" — this is the exact bug this dual-path design prevents
- Or `created == false` (misclassified as update)

**Current Status:** 🟢 Passing
**Implementation Note:** Do NOT throw 404 when lookup returns empty — that is the expected trigger for the INSERT branch, not an error.

---

### VAC232-TC-003 — Already-COMPLETED target rejected (409)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-VAC-IMP-005 §6.4 State Machine — Invariant INV-2` + `CB-VAC-IMP-005 §10 Error Codes Table (VAC-018)`.

**Preconditions:**
- `FX-004` existing `VaccinationRecord` (B1, "BCG", dose 1, status=COMPLETED)
- Caller = M1 (owner)

**Test Steps:**
1. Arrange: mock lookup → `Optional.of(makeCompletedRecord())`.
2. Act: call `markCompleted(B1, makeRequest(), M1)` targeting the same (vaccineName, doseNumber).
3. Assert: `BusinessException` thrown with `httpStatus=409`, `code="VAC-018"`; `recordRepository.save()` **never** invoked.

**Expected Result (PASS):** Exception `VAC-018` (409), no persistence side effect.
**Expected Result (FAIL):** No exception thrown / record silently re-saved / wrong error code.

**Current Status:** 🟢 Passing
**Implementation Note:** Guard `if (existing.getStatus() == COMPLETED) throw` must run before any mutation.

---

### VAC232-TC-004 — Ownership denied (403)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`, `PDPA minimum-necessary access`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BabyAccessPolicy.java` (planned `isOwner()`, canonical from UC-230 ADR-VAC-005) + `CB-VAC-IMP-005 §10 (VAC-002)` + `CB-VAC-IMP-005 ADR-VAC-006` (updated — owner-only, no longer `canView`).

**Preconditions:**
- `FX-001` baby B1 owned by M1
- Caller = M2 (`FX-008`, not owner, not ACCEPTED care-group member of B1)

**Test Steps:**
1. Arrange: mock `babyRepository.findById(B1)` → `makeBaby()`; mock `accessPolicy.isOwner(baby, M2)` → false.
2. Act: call `markCompleted(B1, makeRequest(), M2)`.
3. Assert: `BusinessException{httpStatus=403, code="VAC-002"}` thrown; no repository write calls occur.

**Expected Result (PASS — hệ thống an toàn):** `403 VAC-002`, zero persistence calls.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request succeeds despite caller lacking access (IDOR).

**Current Status:** 🔴 Not written

---

### VAC232-TC-005 — Baby not found (404)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `VaccinationServiceImpl.java` §39-41 (reused VAC-001 pattern) + `CB-VAC-IMP-005 §10 (VAC-001)`.

**Preconditions:** `babyRepository.findById(unknownId)` returns empty.

**Test Steps:**
1. Act: call `markCompleted(unknownBabyId, makeRequest(), M1)`.
2. Assert: `BusinessException{httpStatus=404, code="VAC-001"}`.

**Expected Result (PASS):** `404 VAC-001`.
**Expected Result (FAIL):** NullPointerException or wrong code/status.

**Current Status:** 🔴 Not written

---

### VAC232-TC-006 — Valid proofRecordId accepted and persisted

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()` → `validateProof()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `V1__init_schema.sql` §669, §1733-1734 (`proof_record_id` column + FK to `health_records`) + `CB-VAC-IMP-005 ADR-VAC-007 §Decision`.

**Preconditions:**
- `FX-005` `HealthRecord{id=H1, babyId=B1}` exists
- Path (b) scenario (no existing vaccination record) for simplicity

**Test Steps:**
1. Arrange: mock `healthRecordRepository.findById(H1)` → `Optional.of(makeProofRecord(H1, B1))`; request includes `proofRecordId=H1`.
2. Act: call `markCompleted(B1, makeRequest(r -> r.setProofRecordId(H1)), M1)`.
3. Assert: saved entity has `proofRecordId == H1`; response `proofRecordId == H1`.

**Expected Result (PASS):** `proofRecordId` persisted and echoed in response.
**Expected Result (FAIL):** `proofRecordId` silently dropped/null despite valid input (entity mapping missing — see TDS L3).

**Current Status:** 🟢 Passing
**Implementation Note:** Requires the `VaccinationRecord.proofRecordId` field mapping added per ADR-VAC-007 (no migration).

---

### VAC232-TC-007 — Invalid/foreign proofRecordId rejected (422)

**Severity:** `HIGH`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR via cross-baby proof link)`
**Legal:** `BR-PRIVACY`, `PDPA minimum-necessary`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()` → `validateProof()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-VAC-IMP-005 §10 (VAC-020)` + `CB-VAC-IMP-005 ADR-VAC-007` (proof must belong to same baby).

**Preconditions:** `FX-006` `HealthRecord{id=H2, babyId=B2}` (belongs to a **different** baby than B1).

**Test Steps (Attack Simulation):**
1. Caller M1 (legit owner of B1) submits request for B1 with `proofRecordId=H2` (belongs to B2, a baby M1 does not own).
2. Act: call `markCompleted(B1, makeRequest(r -> r.setProofRecordId(H2)), M1)`.
3. Assert: `BusinessException{httpStatus=422, code="VAC-020"}`; no save invoked.

**Expected Result (PASS = hệ thống an toàn):** `422 VAC-020`, request rejected, no cross-baby data linkage created.
**Expected Result (FAIL = lỗ hổng tồn tại):** Vaccination record saved with `proofRecordId` pointing to another baby's health record — cross-baby PII linkage.

**Current Status:** 🟢 Passing

---

### VAC232-TC-008 — Vaccine/dose not found in reference catalog (404)

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()`
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-VAC-IMP-005 §10 (VAC-019)` + `VaccinationReferenceRepository.java` (catalog source of truth) — edge case explicitly called out in TDS §6.3 E4.

**Preconditions:** Request specifies `vaccineName="UNKNOWN_VAX"`, `doseNumber=9` — no matching row in `vaccination_reference_schedules`.

**Test Steps:**
1. Arrange: mock `referenceRepository` catalog lookup for ("UNKNOWN_VAX", 9) → absent.
2. Act: call `markCompleted(B1, makeRequest(r -> {r.setVaccineName("UNKNOWN_VAX"); r.setDoseNumber((short)9);}), M1)`.
3. Assert: `BusinessException{httpStatus=404, code="VAC-019"}`; no record lookup/save attempted (fail-fast before persistence).

**Expected Result (PASS):** `404 VAC-019`.
**Expected Result (FAIL):** Silently creates a `VaccinationRecord` for a vaccine/dose combination that doesn't exist in the MoH reference schedule (data integrity risk — orphaned record with no corresponding schedule entry).

**Current Status:** 🟢 Passing

---

### VAC232-TC-009 — Invalid input rejected (400)

**Severity:** `MEDIUM`
**Feature Under Test:** `MarkVaccinationCompletedRequest` bean validation (`@Valid` at controller)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-VAC-IMP-005 §8.1 Interface Specification` (`@NotBlank vaccineName`, `@NotNull @Min(1) doseNumber`, `@NotNull @PastOrPresent administeredDate`) + `CB-VAC-IMP-005 §10 (VAC-017)`.

**Preconditions:** None (unit/web-slice test).

**Test Steps:**
1. Act (a): POST with empty body `{}` → assert `400`, error code `VAC-017`.
2. Act (b): POST with `administeredDate` set to a future date (e.g. `2099-01-01`) → assert `400 VAC-017`.
3. Act (c): POST with `doseNumber=0` → assert `400 VAC-017`.

**Expected Result (PASS):** All three malformed payloads rejected with `400 VAC-017` and field-level details.
**Expected Result (FAIL):** Request reaches the service layer / DB with invalid data.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### VAC232-TC-010 — Unauthenticated access rejected

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `POST /api/v1/vaccination/babies/{babyId}/completions` (Spring Security filter chain, `@PreAuthorize("isAuthenticated()")`)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** No `Authorization` header / invalid JWT.

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/vaccination/babies/{babyId}/completions` with no JWT.
2. Send the same request with an expired/tampered JWT.
3. Inspect response.

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized` in both cases, service method never invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request reaches `VaccinationCompletionServiceImpl` without a valid principal.

**Current Status:** 🔴 Not written

---

### VAC232-TC-011 — No duplicate record created under dual-path (invariant INV-1)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationCompletionServiceImpl.markCompleted()` — repository lookup contract
**Test File:** `VaccinationCompletionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-VAC-IMP-005 §6.4 Invariant INV-1` + `CB-VAC-IMP-005 §5.2` (no unique DB constraint exists — OPEN-1 — so the **application layer** must guarantee this).

**Preconditions:** `FX-002` existing SCHEDULED record for (B1, "DTP-VGB-Hib", 1).

**Test Steps:**
1. Act: call `markCompleted()` twice in sequence for the same (babyId, vaccineName, doseNumber) with the lookup mock updated after the first call to reflect the now-COMPLETED state.
2. Assert: first call → path (a) UPDATE (`created=false`); second call → `409 VAC-018` (per `VAC232-TC-003`), not a second INSERT.

**Expected Result (PASS):** Exactly one `VaccinationRecord` row conceptually exists for the key after both calls.
**Expected Result (FAIL):** Two rows exist for the same `(baby_id, vaccine_name, dose_number)` — violates INV-1.

**Current Status:** 🟢 Passing
**Implementation Note:** This test also documents residual risk OPEN-1 (no unique DB index) — a concurrent race between two requests is NOT fully closed by this test (single-threaded sequential simulation only); flagged as `Open` in TDS §Appendix C.

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### VAC232-TC-INT-001 — Full E2E: Path (b) create-new persists correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/vaccination/babies/{babyId}/completions → DB` (create-new branch)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationCompletionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL Testcontainer running; Flyway migrations applied automatically (baseline `V1__init_schema.sql` + reference seed migration)
- Seed: `FX-001` baby B1 owned by M1; reference catalog contains ("BCG", 1); no `vaccination_records` row for that key

**Test Steps:**
1. Seed minimal data (baby, reference catalog row).
2. `POST /api/v1/vaccination/babies/{B1}/completions` with `{vaccineName:"BCG", doseNumber:1, administeredDate:"2026-07-01"}`, JWT of M1.
3. Assert HTTP response.
4. Query `vaccination_records` table directly.

**Expected Result (PASS):**
- Response status `201`, body `data.created=true`, `data.status="COMPLETED"`
- `vaccination_records` contains exactly 1 row for `(baby_id=B1, vaccine_name='BCG', dose_number=1)` with `status='COMPLETED'`, `administered_date='2026-07-01'`

**Expected Result (FAIL):** 404/500, or no row persisted, or wrong status code.

**DB Assertion:**
```java
VaccinationRecord record = recordRepository
        .findByBabyIdAndVaccineNameAndDoseNumber(B1, "BCG", (short) 1)
        .orElseThrow();
assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);
assertThat(record.getAdministeredDate()).isEqualTo(LocalDate.of(2026, 7, 1));
```

**Current Status:** 🔴 Not written

---

### VAC232-TC-INT-002 — Full E2E: Path (a) update-existing persists correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST .../completions → DB` (update-existing branch)
**Test File:** `VaccinationCompletionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- Seed `FX-001` + `FX-002` (existing SCHEDULED record for B1/"DTP-VGB-Hib"/1) inserted via JPA before the call

**Test Steps:**
1. Seed baby + existing SCHEDULED record.
2. `POST /api/v1/vaccination/babies/{B1}/completions` with `{vaccineName:"DTP-VGB-Hib", doseNumber:1, administeredDate:"2026-07-01", facilityName:"VNVC"}`.
3. Assert response + DB row count unchanged (still 1 row for that key, same `vaccination_record_id`).

**Expected Result (PASS):**
- Response `200`, `data.created=false`
- Same `vaccination_record_id` as the pre-seeded row now has `status='COMPLETED'`, `facility_name='VNVC'`
- Row count for the key is still exactly 1 (no duplicate inserted)

**Expected Result (FAIL):** A second row inserted (duplicate), or original row's id changed, or response `created=true`.

**DB Assertion:**
```java
List<VaccinationRecord> rows = recordRepository.findAllByBabyId(B1).stream()
        .filter(r -> r.getVaccineName().equals("DTP-VGB-Hib") && r.getDoseNumber() == 1)
        .toList();
assertThat(rows).hasSize(1);
assertThat(rows.get(0).getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-------------------|----------------------|---------------------|
| `VAC232-TC-001` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-002` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-003` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-004` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-005` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-006` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-007` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-008` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-009` | `VaccinationControllerTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-010` | `VaccinationControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-011` | `VaccinationCompletionServiceImplTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-INT-001` | `VaccinationCompletionIntegrationTest.java` | `[ ]` | `[ ]` | |
| `VAC232-TC-INT-002` | `VaccinationCompletionIntegrationTest.java` | `[ ]` | `[ ]` | |

**Total TCs:** 13 (Unit/Service: 8, Controller-validation: 1, Security: 1 dedicated + IDOR/ownership checks embedded in TC-004/TC-007, Integration: 2, Invariant: 1)

**Severity breakdown:**
- **CRITICAL (4):** `VAC232-TC-001`, `VAC232-TC-002`, `VAC232-TC-004`, `VAC232-TC-010`
- **HIGH (6):** `VAC232-TC-003`, `VAC232-TC-005`, `VAC232-TC-007`, `VAC232-TC-011`, `VAC232-TC-INT-001`, `VAC232-TC-INT-002`
- **MEDIUM (3):** `VAC232-TC-006`, `VAC232-TC-008`, `VAC232-TC-009`

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class VaccinationCompletionServiceImpl implements IVaccinationCompletionService {

    @Override
    public VaccinationCompletionResponse markCompleted(UUID babyId,
                                                        MarkVaccinationCompletedRequest request,
                                                        UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|-------------------------------------|
| `VAC232-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `VAC232-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-009` | `400 expected but 5xx from stub` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-010` | `401 expected but 5xx from stub` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC232-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `N/A - Red Gate not reconstructed because production implementation already existed before this execution.`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(Open — chưa có, sẽ tạo khi chạy Red Gate thực tế)*

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-VAC-IMP-005` đã được review và approve (hiện tại: `Draft`)
- [ ] Logic Issues (§2 trên) đã được confirm với Principal Architect
- [ ] Không cần Flyway migration mới (ADR-VAC-008 xác nhận schema baseline đã đủ) — chỉ cần entity mapping thêm cho `proof_record_id`
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `VaccinationCompletionServiceImpl`
- [ ] Không có business logic trong `VaccinationController` (chỉ validation + mapping + status code selection)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Cả hai đường dẫn dual-path (`VAC232-TC-001`, `VAC232-TC-002`) pass độc lập
- [ ] Không có record trùng `(baby_id, vaccine_name, dose_number)` sau test suite (`VAC232-TC-011`)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua `VaccinationCompletionTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã ghi trong mỗi TC ở §4)

### Suspension Criteria

- OPEN-1 (unique index), OPEN-3 (audit trail) — TDS Appendix C, chưa được Principal Architect/Product phân xử; không chặn việc viết test cho `markCompleted()` core (OPEN-2 canManage đã RESOLVED — dùng `isOwner()`)
- ~~OPEN-4 (proof-upload UX, shared with UC-229 OPEN-2)~~ — **RESOLVED, Accepted by user/product owner, 2026-07-04** (xem TDS Appendix C + ADR-VAC-007 Accepted): client-supplied `proofRecordId` là API contract duy nhất; checkbox "Tạo hồ sơ y tế tự động" là tiện ích UX phía client bọc luồng hai bước sẵn có (không phải auto-create phía server). `VAC232-TC-006`/`VAC232-TC-007` đã test đúng contract này (valid/invalid `proofRecordId` do client cung cấp) và **không** còn gated/hedged trên câu hỏi này.
- CI pipeline bị broken bởi thay đổi khác
- `proof_record_id` entity mapping (ADR-VAC-007) gặp xung đột với sibling UC-229 đang phát triển song song

---

## 7. Rollback Plan

```bash
# KHÔNG có migration mới trong scope UC-232 (ADR-VAC-008) → không cần revert DDL

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/

# Nếu entity mapping proof_record_id (ADR-VAC-007) gây side-effect không mong muốn cho sibling UC:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/entity/VaccinationRecord.java

# Gap vẫn OPEN → giữ nguyên entry trong tracking (không có PHASE_GAP_ANALYSIS.md riêng cho batch này — ghi Open)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC cite `CB-VAC-IMP-005` ADR/section | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ xác nhận thực tế khi Red Gate chạy (hiện tại: thiết kế đúng theo template, chưa execute) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — dual-path (ADR-VAC-005), no-migration (ADR-VAC-008), proof reuse (ADR-VAC-007) đều có ADR | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — `VAC232-TC-009`/`010` chỉ test validation/auth tại controller layer, business logic tests đều target `VaccinationCompletionServiceImpl` | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — `IVaccinationCompletionService`, `VaccinationCompletionServiceImpl`, `MarkVaccinationCompletedRequest` đều là NEW types **đã khai báo tường minh** trong TDS §8 (chưa tồn tại trong code THẬT tại thời điểm viết spec — đây là kỳ vọng vì TDD viết TRƯỚC implementation, không phải hallucination của type đã có mà đặt sai tên) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-design → TDD spec approved for Red Gate execution
- [ ] AP-AI-002 (Green-from-Birth) cần xác nhận thực nghiệm khi Red Gate thực sự chạy (§5.1) — không thể tick trước khi có log thực tế

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| — | — | Không có AP phát hiện tại thời điểm review spec (2026-07-03) | — | ☐ N/A |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft. Chờ Principal Architect review trước khi chuyển In Review/Approved.*
