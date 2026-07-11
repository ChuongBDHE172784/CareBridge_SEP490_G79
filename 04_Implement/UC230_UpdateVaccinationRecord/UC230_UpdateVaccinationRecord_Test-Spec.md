# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-230 Update Vaccination Record — Test Specification

**Document ID:** `CB-VAC-TEST-003`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (13/20 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (lines 660-672 `vaccination_records`, 679-693 `health_records`, 1734 FK)
- `04_Implement/UC230_UpdateVaccinationRecord/UC230_UpdateVaccinationRecord_TDS.md` (`CB-VAC-IMP-003`) — companion Technical Design Specification
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.3 (Table 252) — UC-230 SRS source
- `04_Implement/UC222_UpdateFamilyTask/UC222_UpdateFamilyTask_TDS.md` — content-vs-status precedent (ADR-FAM-074)
- `05_Development/CareBridgeAPI/.../vaccination/service/impl/VaccinationServiceImpl.java` — real merge logic evidence for the "Mother-entered record" scope boundary

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-10` | `AI Agent` | Truthful sync after implementation evidence: status set to Partially Implemented, 13/20 tests PASS in VaccinationServiceImplTest; Red Gate not reconstructed because implementation pre-existed; controller/INT/E2E remain pending |
| `2026-07-03` | `AI Agent — Test Designer` | Khởi tạo tài liệu — Test-Spec cho UC-230 Update Vaccination Record |

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
| **Feature / Gap ID** | `UC-230` |
| **Module** | `UpdateVaccinationRecord — vaccination bounded context` |
| **Spec gốc** | `CB-VAC-IMP-003` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S[N] — Open (not yet scheduled)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` (no BR-SAFETY tag per SRS §3.3.19.3) |
| **Upstream Dependencies** | `vaccination` module (existing UC-228 code), `baby` module (`BabyProfile`, `BabyAccessPolicy`), `health` module (`HealthRecord`, `HealthRecordRepository`) |
| **Downstream Consumers** | CB-173 Vaccination Detail screen; UC-228 View Schedule (reflects corrected data) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-VAC-IMP-003 §17` (Constraint Injection Block C1-C6), ADR-VAC-004..008 |
| **Constraints Injected** | C1 (real schema authority), C2 (owner-only auth), C3 (proof validation), C4 (status rejection), C5 (JWT-sourced callerId), C6 (clearProof semantics) |
| **Model** | `Claude — Technical Architect / Test Designer session` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` and approved migrations are the final
> persistence oracle; ERD/prior docs are only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC-228's Approved TDS documents `VaccinationRecord` fields `babyProfileId`, `referenceScheduleId`, `facility`, `notes`, `proofFileId`, and a `vac_record_status` enum limited to `{COMPLETED, POSTPONED}` | Real entity (`vaccination/entity/VaccinationRecord.java`) uses `babyId`, no `referenceScheduleId` FK, `facilityName`, no `notes`, `VaccinationRecordStatus` has 3 values `{SCHEDULED, COMPLETED, POSTPONED}` | All test fixtures/factories in §4 use the real field names and 3-value enum; TCs never assert against `facility`/`notes`/`referenceScheduleId` |
| L2 | Prompt's literal instruction says "same `BabyAccessPolicy`-style check as UC-229" (implying reuse of the existing `canView()` predicate) | `canView()` grants owner **OR** ACCEPTED care-group member — too permissive for a Mother-only write path per BR-RBAC least-privilege (ADR-VAC-005 in the TDS) | Test cases distinguish **owner** (allowed) from **ACCEPTED-but-non-owner care member** (denied, VAC-002) — see VAC230-TC-011; this is the single biggest behavioral divergence from a literal reading of the instruction, and is called out here per Policy 4.4 |
| L3 | Prompt names the sequence "status-field-in-request-ignored-or-rejected" as an open design point | TDS ADR-VAC-007 resolves this as **explicit rejection** (400 VAC-012), not silent ignore, for safety-adjacent reasons on health data | VAC230-TC-009/TC-010 assert a thrown `BusinessException(400, "VAC-012")` and assert **no field is partially applied**, not a silently-succeeding 200 |
| L4 | `04_Implement/UC229_AddVaccinationRecord/` was empty at authoring time — no canonical proof-record validation contract to cite | TDS ADR-VAC-006 independently specifies: `HealthRecordRepository.findByIdAndStatus(id, ACTIVE)` + `babyId` match | **RESOLVED** — UC-229's TDS now exists and was updated (ADR-VAC-229-004) to use the identical `findByIdAndStatus(id, ACTIVE)` + `babyId` check as this ADR. No reconciliation gap remains; test oracles may now also cite `04_Implement/UC229_AddVaccinationRecord/UC229_AddVaccinationRecord_TDS.md` ADR-VAC-229-004 as confirming consistency |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UpdateVaccinationRecord bao gồm các layer:
├── Domain (VaccinationRecord entity mutation logic — pure, no deps)
├── Services (VaccinationServiceImpl.updateVaccinationRecord — mock JPA repos + BabyAccessPolicy với Mockito)
├── Controller (VaccinationController PATCH endpoint — @WebMvcTest, mock IVaccinationService)
└── Integration (Testcontainers PostgreSQL — real vaccination_records + health_records rows)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-230` (§3.3.19.3, Table 252) | Mother-only actor scope; content-only update; E1/E2/E3 exception classes |
| `ADR-VAC-004` | Real entity/schema field names used in all fixtures |
| `ADR-VAC-005` | Owner-only authorization — non-owner care member denial test |
| `ADR-VAC-006` | Proof-record existence + same-baby validation tests |
| `ADR-VAC-007` | Status-field-rejection tests (explicit, not silent) |
| `ADR-VAC-008` | Partial-update null semantics + `clearProof` tri-state tests |
| `BR-RBAC` / `BR-PRIVACY` / PDPA | Authorization and cross-baby data isolation tests |
| `CB-VAC-IMP-003` §8/§9/§10 | Interface contracts, endpoint shape, error code table |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner updates one content field | `VaccinationServiceImpl.updateVaccinationRecord()` | `VAC230-TC-001` |
| TC-COND-002 | Owner updates multiple content fields at once | same | `VAC230-TC-002` |
| TC-COND-003 | Owner sets a valid same-baby proof record | same + `HealthRecordRepository` | `VAC230-TC-003` |
| TC-COND-004 | Owner clears an existing proof record via `clearProof=true` | same | `VAC230-TC-004` |
| TC-COND-005 | Request with all fields null/absent is a no-op except `updated_at` | same | `VAC230-TC-005` |
| TC-COND-006 | `proofRecordId` does not exist | same | `VAC230-TC-006` |
| TC-COND-007 | `proofRecordId` exists but is not `ACTIVE` | same | `VAC230-TC-007` |
| TC-COND-008 | `proofRecordId` belongs to a different baby | same | `VAC230-TC-008` |
| TC-COND-009 | Request body contains a `status` key alone | same | `VAC230-TC-009` |
| TC-COND-010 | Request body contains `status` + a valid field together | same | `VAC230-TC-010` |
| TC-COND-011 | Caller is an ACCEPTED care-group member, not the owner | `BabyAccessPolicy.isOwner()` | `VAC230-TC-011` |
| TC-COND-012 | Caller is unauthenticated | `VaccinationController` (`@PreAuthorize`) | `VAC230-TC-012` |
| TC-COND-013 | `babyId` does not exist | `VaccinationServiceImpl` | `VAC230-TC-013` |
| TC-COND-014 | `recordId` does not exist for a valid `babyId` | `VaccinationRecordRepository.findByIdAndBabyId` | `VAC230-TC-014` |
| TC-COND-015 | `recordId` exists but belongs to a *different* baby than the path `babyId` | same | `VAC230-TC-015` |
| TC-COND-016 | `vaccineName` exceeds 200 chars | Bean Validation (`@Size`) | `VAC230-TC-016` |
| TC-COND-017 | `doseNumber` is zero or negative | Bean Validation (`@Positive`) | `VAC230-TC-017` |
| TC-COND-018 | Full integration: proof-record update persists to DB | `VaccinationRecordRepository` + Testcontainers | `VAC230-TC-INT-001` |
| TC-COND-019 | Full integration: `status` column untouched across repeated non-status updates | same | `VAC230-TC-INT-002` |
| TC-COND-020 | Injection-style payload in `facilityName` is stored/escaped safely (JPA parameterization) | `VaccinationServiceImpl` + Testcontainers | `VAC230-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | proof-record validity states (valid / not-found / inactive / cross-baby) | Four distinct partitions per ADR-VAC-006 |
| Boundary Value Analysis | `doseNumber` (0, 1, negative), `vaccineName` length (200 vs 201 chars) | Bean validation boundaries |
| State Transition Testing | `status` field must remain fixed across every content update | Confirms ADR-VAC-007 invariant holds under repeated mutation |
| Error Guessing | attempting to smuggle `status` alongside a valid field; cross-baby `recordId`/`proofRecordId` mismatches; SQL-injection-style strings | Security/robustness on a Sensitive-PII endpoint |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `BabyProfile { id: BABY-1, ownerUserId: ACC-MOTHER }` | Owner baseline |
| `FX-002` | DB seed | `VaccinationRecord { id: REC-1, babyId: BABY-1, vaccineName: 'BCG', doseNumber: 1, status: COMPLETED, facilityName: 'Old Clinic' }` | Update target, happy path |
| `FX-003` | DB seed | `HealthRecord { id: HR-1, babyId: BABY-1, status: ACTIVE }` | Valid proof record (same baby) |
| `FX-004` | DB seed | `HealthRecord { id: HR-2, babyId: BABY-2, status: ACTIVE }` | Cross-baby proof record (should be rejected) |
| `FX-005` | DB seed | `HealthRecord { id: HR-3, babyId: BABY-1, status: ARCHIVED }` | Inactive proof record (should be rejected) |
| `FX-006` | JWT | `{ sub: ACC-MOTHER, role: MOTHER }` | Owner auth context |
| `FX-007` | JWT | `{ sub: ACC-CAREMEMBER, role: FAMILY }` — `CareGroupMember { userId: ACC-CAREMEMBER, careGroupId: <BABY-1's group>, inviteStatus: ACCEPTED }` | Non-owner but `canView()`-eligible caller — proves ADR-VAC-005's stricter check |
| `FX-008` | JWT | `{ sub: ACC-STRANGER }` — no relation to `BABY-1` | Fully unrelated caller |

---

## 4. Test Case Specification

> **TC ID format:** `VAC230-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written (all TCs below — pre-implementation)

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// vaccination/UpdateVaccinationRecordTestFactory.java
// ═══════════════════════════════════════════════════════════
class UpdateVaccinationRecordTestFactory {

    static final UUID BABY_1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BABY_2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID ACC_MOTHER = UUID.fromString("00000000-0000-0000-0000-00000000ac01");
    static final UUID ACC_CAREMEMBER = UUID.fromString("00000000-0000-0000-0000-00000000ac02");
    static final UUID ACC_STRANGER = UUID.fromString("00000000-0000-0000-0000-00000000ac03");

    // Baseline BabyProfile owned by ACC_MOTHER — synced with FX-001 (§3 TDS-05)
    static BabyProfile makeBaby() {
        return BabyProfile.builder()
                .id(BABY_1)
                .ownerUserId(ACC_MOTHER)
                .nickname("Bean")
                .build();
    }

    // Baseline persisted vaccination record -- synced with FX-002
    static VaccinationRecord makeRecord() {
        return VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_1)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .status(VaccinationRecordStatus.COMPLETED)
                .facilityName("Old Clinic")
                .build();
    }

    static VaccinationRecord makeRecord(Consumer<VaccinationRecord> overrides) {
        VaccinationRecord record = makeRecord();
        overrides.accept(record);
        return record;
    }

    // Valid same-baby proof record -- synced with FX-003
    static HealthRecord makeActiveHealthRecordForBaby1() {
        return HealthRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_1)
                .status(HealthRecordStatus.ACTIVE)
                .build();
    }

    // Cross-baby proof record -- synced with FX-004
    static HealthRecord makeActiveHealthRecordForBaby2() {
        return HealthRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_2)
                .status(HealthRecordStatus.ACTIVE)
                .build();
    }

    // Inactive proof record -- synced with FX-005
    static HealthRecord makeInactiveHealthRecordForBaby1() {
        return HealthRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_1)
                .status(HealthRecordStatus.ARCHIVED)
                .build();
    }

    static UpdateVaccinationRecordRequest makeRequest() {
        return new UpdateVaccinationRecordRequest(); // all fields null, clearProof=false
    }
}
```

---

### VAC230-TC-001 — Owner updates a single content field (facilityName)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationServiceImpl.updateVaccinationRecord()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationServiceUpdateTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-VAC-IMP-003 §8.1` (service contract) + `ADR-VAC-008` (null = no change)

**Preconditions:**
- `FX-001` baby owned by `ACC_MOTHER`; `FX-002` persisted record `REC-1`

**Test Steps:**
1. Build request `{ facilityName: "Bệnh viện Nhi Trung ương" }`, all other fields null, `clearProof=false`
2. Call `updateVaccinationRecord(BABY_1, REC-1, request, ACC_MOTHER)`
3. Assert returned `VaccinationRecordResponse.facilityName` equals `"Bệnh viện Nhi Trung ương"`
4. Assert `vaccineName`, `doseNumber`, `status`, `proofRecordId` are unchanged from `FX-002`

**Expected Result (PASS):** facilityName updated; all other fields untouched; no exception.
**Expected Result (FAIL):** any other field mutated, or status changed, or exception thrown.
**Current Status:** 🟢 Passing

---

### VAC230-TC-002 — Owner updates multiple content fields at once

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationServiceImpl.updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-VAC-IMP-003 §8.1`

**Test Steps:**
1. Build request `{ vaccineName: "DTP-HepB-Hib", doseNumber: 2, administeredDate: "2026-03-15", facilityName: "New Clinic" }`
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** all four fields updated simultaneously in one write; `status` unchanged.
**Expected Result (FAIL):** partial application (some fields updated, others not) with no thrown exception.
**Current Status:** 🟢 Passing

---

### VAC230-TC-003 — Owner sets a valid same-baby proof record (CRITICAL)

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationServiceImpl.validateProofRecord()` + `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-VAC-006` (proof validation rule)

**Preconditions:** `FX-003` — `HealthRecord HR-1` for `BABY_1`, status `ACTIVE`

**Test Steps:**
1. Build request `{ proofRecordId: HR-1.id, clearProof: false }`
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** `healthRecordRepository.findByIdAndStatus(HR-1.id, ACTIVE)` invoked exactly once; record's `proofRecordId` set to `HR-1.id`; response reflects it.
**Expected Result (FAIL):** proof set without validation call, or validation skipped.
**Current Status:** 🟢 Passing

---

### VAC230-TC-004 — Owner clears an existing proof record via `clearProof=true`

**Severity:** `HIGH`
**Feature Under Test:** `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-VAC-008`

**Preconditions:** record's `proofRecordId` pre-set to `HR-1.id`

**Test Steps:**
1. Build request `{ proofRecordId: null, clearProof: true }` (also test `{ proofRecordId: HR-2.id, clearProof: true }` — clearProof wins regardless of value)
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** `proofRecordId` becomes `null`; `healthRecordRepository` is **never** invoked (validation skipped per ADR-VAC-008).
**Expected Result (FAIL):** proof record left set, or validation attempted despite `clearProof=true`.
**Current Status:** 🟢 Passing

---

### VAC230-TC-005 — No-op request leaves content unchanged except `updatedAt`

**Severity:** `MEDIUM`
**Feature Under Test:** `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-VAC-008` (null = no change)

**Test Steps:**
1. Build fully-empty request via `makeRequest()` factory
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** all content fields identical to pre-call state; `updatedAt` still refreshed (via `@UpdateTimestamp` on `save()`); no exception.
**Expected Result (FAIL):** an exception thrown for an empty request, or an unrelated field mutated.
**Current Status:** 🔴 Not written

---

### VAC230-TC-006 — `proofRecordId` does not exist → VAC-010

**Severity:** `HIGH`
**Feature Under Test:** `validateProofRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-VAC-IMP-003 §10` error table (`VAC-010`, 404)

**Test Steps:**
1. Build request `{ proofRecordId: <random UUID not in DB> }`
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** `BusinessException` thrown with `httpStatus=404`, `code="VAC-010"`; no `save()` invoked.
**Expected Result (FAIL):** silent no-op, wrong code, or record saved with a dangling proof reference.
**Current Status:** 🟢 Passing

---

### VAC230-TC-007 — `proofRecordId` exists but is not `ACTIVE` → VAC-010

**Severity:** `HIGH`
**Feature Under Test:** `validateProofRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-VAC-006` + `CB-VAC-IMP-003 §10` (`VAC-010`)

**Preconditions:** `FX-005` — `HR-3` for `BABY_1`, status `ARCHIVED`

**Test Steps:**
1. Build request `{ proofRecordId: HR-3.id }`
2. Call service with `ACC_MOTHER`

**Expected Result (PASS):** `BusinessException(404, "VAC-010")` — same code as "not found," since `findByIdAndStatus(id, ACTIVE)` returns empty for a non-active row.
**Expected Result (FAIL):** inactive health record accepted as a valid proof link.
**Current Status:** 🟢 Passing

---

### VAC230-TC-008 — `proofRecordId` belongs to a different baby → VAC-011 (CRITICAL, PDPA)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-PRIVACY / PDPA minimum-necessary access`
**Feature Under Test:** `validateProofRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-VAC-006` + `CB-VAC-IMP-003 §10` (`VAC-011`, 409)

**Preconditions:** `FX-004` — `HR-2` belongs to `BABY_2`, status `ACTIVE`

**Test Steps:**
1. Build request `{ proofRecordId: HR-2.id }` targeting `BABY_1`'s vaccination record
2. Call service with `ACC_MOTHER` (owner of `BABY_1`)

**Expected Result (PASS):** `BusinessException(409, "VAC-011")`; record's `proofRecordId` remains unchanged (not linked cross-baby).
**Expected Result (FAIL):** the cross-baby health record is linked — a PDPA-relevant data leak.
**Current Status:** 🟢 Passing

---

### VAC230-TC-009 — `status` key alone in request body → VAC-012, explicit rejection (CRITICAL)

**Severity:** `CRITICAL`
**Legal:** `Safety-adjacent (health data integrity) — no direct legal citation, per ADR-VAC-007 rationale`
**Feature Under Test:** `rejectStatusField()` / `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-VAC-007` + `CB-VAC-IMP-003 §10` (`VAC-012`, 400)

**Test Steps:**
1. Simulate a request body `{"status": "POSTPONED"}` (raw JSON with an unrecognized `status` key)
2. Call service/controller with `ACC_MOTHER`

**Expected Result (PASS):** `BusinessException(400, "VAC-012")` thrown; entity's `status` column value is verified unchanged after the call.
**Expected Result (FAIL):** `status` silently ignored and 200 returned (violates ADR-VAC-007's explicit-rejection decision), or `status` actually changed.
**Current Status:** 🟢 Passing

---

### VAC230-TC-010 — `status` key alongside a valid field → whole request rejected, no partial apply (CRITICAL)

**Severity:** `CRITICAL`
**Feature Under Test:** `rejectStatusField()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-VAC-007` ("Request is rejected wholesale... no partial application")

**Test Steps:**
1. Simulate request body `{"status": "COMPLETED", "facilityName": "Should Not Apply"}`
2. Call service with `ACC_MOTHER`
3. After the exception, re-fetch the record

**Expected Result (PASS):** `BusinessException(400, "VAC-012")`; `facilityName` is **still** the pre-call value (`"Old Clinic"`) — no partial write occurred.
**Expected Result (FAIL):** `facilityName` updated despite the overall request being rejected (partial-apply bug).
**Current Status:** 🟢 Passing

---

### VAC230-TC-011 — ACCEPTED care-group member (non-owner) is denied (CRITICAL, RBAC)

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `BabyAccessPolicy.isOwner()` used by `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-VAC-005` (owner-only, stricter than `canView()`) — this is the L2 divergence recorded in §2

**Preconditions:** `FX-007` — `ACC_CAREMEMBER` is an `ACCEPTED` member of `BABY_1`'s care group (would pass `canView()` but must **fail** `isOwner()`)

**Test Steps:**
1. Build a valid content-only request
2. Call service with `ACC_CAREMEMBER`

**Expected Result (PASS):** `BusinessException(403, "VAC-002")` — confirms the write path is stricter than the read path (`canView()` would have returned `true` for this same caller).
**Expected Result (FAIL):** update succeeds for a non-owner care-group member (regression to UC-228's read-scoped permissiveness).
**Current Status:** 🟢 Passing

---

### VAC230-TC-012 — Unauthenticated caller → 401

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationController` (`@PreAuthorize("isAuthenticated()")`)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationControllerUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** Spring Security default `401` for anonymous access to a `@PreAuthorize("isAuthenticated()")` endpoint (project-wide convention, see UC-228's controller)

**Test Steps:**
1. `PATCH /api/v1/vaccination/babies/{babyId}/records/{recordId}` with no `Authorization` header

**Expected Result (PASS):** HTTP `401`.
**Current Status:** 🔴 Not written

---

### VAC230-TC-013 — Unknown `babyId` → VAC-001

**Severity:** `HIGH`
**Feature Under Test:** `updateVaccinationRecord()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-VAC-IMP-003 §10` (`VAC-001`, reused from UC-228)

**Test Steps:**
1. Call service with a `babyId` not present in `baby_profiles`

**Expected Result (PASS):** `BusinessException(404, "VAC-001")`.
**Current Status:** 🟢 Passing

---

### VAC230-TC-014 — Unknown `recordId` for a valid `babyId` → VAC-009

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationRecordRepository.findByIdAndBabyId()`
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-VAC-IMP-003 §10` (`VAC-009`)

**Test Steps:**
1. Call service with valid `BABY_1` and a random `recordId` not in `vaccination_records`

**Expected Result (PASS):** `BusinessException(404, "VAC-009")`.
**Expected Result (FAIL):** a different code (e.g. reusing `VAC-001`) — the two "not found" cases must be distinguishable.
**Current Status:** 🟢 Passing

---

### VAC230-TC-015 — `recordId` exists but belongs to a different baby → VAC-009 (not VAC-011)

**Severity:** `HIGH`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `findByIdAndBabyId()` scoped lookup
**Test File:** same as TC-001
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `ADR-VAC-004`/`ADR-VAC-005` — the record lookup is **scoped by babyId**, so a `recordId` belonging to `BABY_2` addressed via `BABY_1`'s path must appear identically to "not found," not leak a distinguishable existence signal

**Test Steps:**
1. Seed a `VaccinationRecord` for `BABY_2`
2. Call `updateVaccinationRecord(BABY_1, <BABY_2's recordId>, request, ACC_MOTHER)`

**Expected Result (PASS):** `BusinessException(404, "VAC-009")` — identical to a truly nonexistent `recordId`, preventing cross-baby existence enumeration.
**Expected Result (FAIL):** a different error code/behavior reveals that the record exists under a different baby.
**Current Status:** 🟢 Passing

---

### VAC230-TC-016 — `vaccineName` exceeds 200 chars → 400 (Bean Validation)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateVaccinationRecordRequest` (`@Size(max=200)`)
**Test File:** `VaccinationControllerUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CB-VAC-IMP-003 §8.1` DTO annotation (`@Size(max = 200)`, mirrors entity column `vaccine_name varchar(200)` in `V1__init_schema.sql` line 663)

**Test Steps:**
1. `PATCH` with `vaccineName` = 201-character string

**Expected Result (PASS):** HTTP `400`, `GlobalExceptionHandler.handleMethodArgumentNotValid` response (`error: "VALIDATION_ERROR"`), field detail names `vaccineName`.
**Current Status:** 🔴 Not written

---

### VAC230-TC-017 — `doseNumber` zero or negative → 400 (Bean Validation, Boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateVaccinationRecordRequest` (`@Positive`)
**Test File:** `VaccinationControllerUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `CB-VAC-IMP-003 §8.1` (`@Positive` annotation)

**Test Steps:**
1. `PATCH` with `doseNumber = 0`, then repeat with `doseNumber = -1`

**Expected Result (PASS):** both requests return HTTP `400` `VALIDATION_ERROR`.
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### VAC230-TC-SEC-001 — Injection-style payload in `facilityName` handled safely

**Severity:** `LOW`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Legal:** `N/A — defensive robustness check, not a compliance requirement`
**Feature Under Test:** `VaccinationServiceImpl.updateVaccinationRecord()` (via Testcontainers, real Hibernate parameterized queries)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationRecordUpdateIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** PostgreSQL Testcontainer running; `FX-002` seeded

**Test Steps (Attack Simulation):**
1. Build request `{ facilityName: "Clinic'; DROP TABLE vaccination_records; --" }`
2. Call the real service against the container DB

**Expected Result (PASS = hệ thống an toàn):** the literal string is stored verbatim in `facility_name`; `vaccination_records` table still exists and is queryable afterward (JPA/Hibernate parameter binding prevents injection by construction — this test documents/confirms that guarantee for this specific field).
**Expected Result (FAIL = lỗ hổng tồn tại):** table dropped, or query error indicating string concatenation was used instead of a bound parameter.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### VAC230-TC-INT-001 — Full flow: proof-record update persists to DB correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH request → VaccinationServiceImpl → VaccinationRecordRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/VaccinationRecordUpdateIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`, Flyway auto-applies `V1__init_schema.sql`)
- Seed: `FX-001` baby, `FX-002` vaccination record, `FX-003` health record (`HR-1`, `ACTIVE`, `baby_id = BABY_1`)

**Test Steps:**
1. Seed minimal data via JPA repositories
2. Call `PATCH /api/v1/vaccination/babies/{BABY_1}/records/{REC-1}` with `{ proofRecordId: HR-1.id }` (owner JWT)
3. Assert HTTP `200`
4. Query DB directly for the row

**Expected Result (PASS):**
- Response body's `proofRecordId` equals `HR-1.id`
- DB row: `SELECT proof_record_id FROM vaccination_records WHERE vaccination_record_id = ?` returns `HR-1.id`

**DB Assertion:**
```java
VaccinationRecord record = recordRepository.findById(savedId).orElseThrow();
assertThat(record.getProofRecordId()).isEqualTo(HR_1_ID);
assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED); // untouched
```

**Current Status:** 🔴 Not written

---

### VAC230-TC-INT-002 — `status` column remains untouched across repeated non-status updates (CRITICAL)

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow — status invariant across multiple PATCH calls`
**Test File:** same as TC-INT-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `ADR-VAC-007` invariant + TDS §6.4 State Machine Note

**Test Steps:**
1. Seed `FX-002` with `status = COMPLETED`
2. Issue 3 sequential `PATCH` calls, each changing a different content field (`facilityName`, then `administeredDate`, then `vaccineName`)
3. After each call, query the DB row's `status` column directly

**Expected Result (PASS):** `status` reads `COMPLETED` after all three calls — never transitions, regardless of how many content updates occur.
**Expected Result (FAIL):** `status` changes to any other value at any point.
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VAC230-TC-001` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-002` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-003` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-004` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-005` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-006` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-007` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-008` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-009` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-010` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-011` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-012` | `VaccinationControllerUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-013` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-014` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-015` | `VaccinationServiceUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-016` | `VaccinationControllerUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-017` | `VaccinationControllerUpdateTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-SEC-001` | `VaccinationRecordUpdateIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-INT-001` | `VaccinationRecordUpdateIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `VAC230-TC-INT-002` | `VaccinationRecordUpdateIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class VaccinationServiceImpl implements IVaccinationService {

    @Override
    public VaccinationRecordResponse updateVaccinationRecord(
            UUID babyId, UUID recordId, UpdateVaccinationRecordRequest request, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VAC230-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC230-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

> Controller-level TCs (`TC-012`, `TC-016`, `TC-017`) FAIL at the Red stub too, since
> `@WebMvcTest` still routes through the (throwing) service mock/stub or Bean Validation runs
> before the service is invoked at all (for `TC-016`/`TC-017`, validation failure is independent
> of the stub and should already 🔴 FAIL for the *right* reason — no service call reached).

**Red Gate Evidence:**

- Stub commit hash: `N/A - Red Gate not reconstructed because production implementation already existed before this execution.`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(to be filled at implementation time)*

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-VAC-IMP-003` đã được review và approve
- [ ] Logic Issues (§2 above) đã được confirm với Tech Lead — **đặc biệt L2** (owner-only vs
      `canView()` divergence) vì đây là điểm khác biệt so với chỉ dẫn gốc
- [ ] Không cần Flyway migration mới (ADR-VAC-004) — chỉ cần xác nhận `V1__init_schema.sql` đã
      applied trên môi trường test
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `VaccinationServiceImpl.updateVaccinationRecord()` và helpers
- [ ] Không có business logic trong `VaccinationController`
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `status` column verified untouched in `VAC230-TC-INT-002` across ≥ 3 sequential updates

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi test dùng `UpdateVaccinationRecordTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value có trích dẫn ADR/BR/§ cụ thể (xem mỗi TC ở §4)

### Suspension Criteria (Điều kiện tạm dừng)

- ~~`04_Implement/UC229_AddVaccinationRecord/` TDS lands with a proof-validation design that conflicts
  with ADR-VAC-006 — pause and reconcile before implementing `VAC230-TC-003/006/007/008`~~ **RESOLVED**
  — UC-229's TDS now exists and matches ADR-VAC-006 exactly (`findByIdAndStatus(id, ACTIVE)` + `babyId`
  match); no longer a suspension condition
- Flyway baseline (`V1__init_schema.sql`) not yet applied on the test environment

---

## 7. Rollback Plan

```bash
# No migration to revert -- ADR-VAC-004 introduces zero DDL.

# Revert implementation files (dev only)
git checkout -- src/main/java/com/carebridge/backend/vaccination/
git checkout -- src/main/java/com/carebridge/backend/baby/policy/BabyAccessPolicy.java
git checkout -- src/test/java/com/carebridge/backend/vaccination/

# Gap remains OPEN -- keep UC-230 entry in whatever gap-tracking doc the project uses
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ *(mỗi TC ở §4 có `Oracle Source` trích ADR/§ cụ thể)* | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(chưa chạy — Red Gate pending tại implementation time)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ *(owner-only check traced to ADR-VAC-005; proof validation to ADR-VAC-006; status rejection to ADR-VAC-007)* | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ *(controller TCs — TC-012/016/017 — chỉ test routing/validation, không test business rules)* | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ *(all types — `VaccinationRecord`, `HealthRecord`, `BabyProfile`, `BabyAccessPolicy`, `HealthRecordRepository` — verified to exist by reading real source; only `UpdateVaccinationRecordRequest`, `VaccinationRecordResponse`, `BabyAccessPolicy.isOwner()`, `VaccinationRecordRepository.findByIdAndBabyId()` are new, and each is declared in the companion TDS §8 before being used here)* | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ngoài AP-AI-002 (chưa thể verify cho đến khi code + chạy
      test tồn tại — đây là trạng thái bình thường ở giai đoạn Draft, không phải một finding)
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | Không có AP nào được phát hiện tại thời điểm viết Test-Spec này (ngoài AP-AI-002 đang chờ Red Gate ở implementation time) | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
