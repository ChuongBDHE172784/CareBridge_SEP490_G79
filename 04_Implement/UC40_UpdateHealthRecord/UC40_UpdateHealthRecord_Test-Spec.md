# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-40 Update Health Record

**Document ID:** `CB-HEALTH-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC40_UpdateHealthRecord/UC40_UpdateHealthRecord_TDS.md` (CB-HEALTH-IMP-002)
- SRS: §3.3.1.17
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-40 Update Health Record |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-40` |
| **Module** | `UpdateHealthRecord — health` |
| **Spec gốc** | `CB-HEALTH-IMP-002` |
| **Priority** | 🔴 P0 |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-HEALTH-UPDATE, PDPA` |
| **Upstream Dependencies** | `UC-39 AddHealthRecord (HealthRecord entity)` |
| **Downstream Consumers** | `UC-42 ViewHealthRecordTimeline, AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-002 §17`, `ADR-HEALTH-003`, `ADR-HEALTH-004` |
| **Constraints Injected** | C1: assertActiveStatus before patch; C2: assertOwnership; C3: PATCH semantics; C4: ownerUserId from JWT; C5: emit HealthRecordUpdated |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS 3.3.1.17 không rõ PATCH hay PUT | ADR-HEALTH-003: PATCH (partial update) | Test encode: chỉ field được gửi mới thay đổi trong DB |
| L2 | SRS không rõ behavior khi record đã ARCHIVED | BR-HEALTH-UPDATE + ADR-HEALTH-004: reject với 409 | Test encode: ARCHIVED record → HEALTH-006 409 |
| L3 | SRS không rõ ownership check | BR-RBAC: owner_user_id phải == JWT sub | Test encode: another user's record → 403 HEALTH-004 |
| L4 | V1 schema không có `tags` hay `notes` column riêng | V1 schema: chỉ có `source_name`, `source_type`, `file_url` | Test không encode `tags`/`notes` field — chỉ dùng columns trong V1 |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UpdateHealthRecord bao gồm các layer:
├── Service (HealthRecordService.updateHealthRecord — mock JPA Repository)
├── Controller (HealthRecordController.PATCH /{id} — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-40` | Partial update title, recordType, recordDate, sourceType, sourceName, fileUrl, babyId, journeyId |
| `ADR-HEALTH-003` | PATCH semantics — merge non-null fields only |
| `ADR-HEALTH-004` | ARCHIVED record → reject 409 |
| `BR-HEALTH-UPDATE` | Only ACTIVE records can be updated |
| `BR-RBAC` | Only owner can update their own record |
| `CB-HEALTH-IMP-002 §10` | Error codes: HEALTH-001, HEALTH-004, HEALTH-006, HEALTH-007 |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid PATCH on ACTIVE record owned by caller | `HealthRecordService.updateHealthRecord()` | `HEALTH40-TC-001` |
| TC-COND-002 | PATCH on ARCHIVED record | `assertActiveStatus()` | `HEALTH40-TC-002` |
| TC-COND-003 | PATCH on another user's record | `assertOwnership()` | `HEALTH40-TC-003` |
| TC-COND-004 | Record not found | `findById()` → empty | `HEALTH40-TC-004` |
| TC-COND-005 | Invalid recordType value | DTO validation | `HEALTH40-TC-005` |
| TC-COND-006 | Future recordDate | `@PastOrPresent` | `HEALTH40-TC-006` |
| TC-COND-007 | PATCH with only null fields (no-op) | applyPatch no-op | `HEALTH40-TC-007` |
| TC-COND-008 | No JWT | Security filter | `HEALTH40-TC-SEC-001` |
| TC-COND-009 | EXPERT role tries to PATCH | RBAC role check | `HEALTH40-TC-SEC-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | recordType values (valid / invalid) | Ensure enum validation |
| Boundary Value Analysis | recordDate (today / tomorrow / yesterday) | PastOrPresent boundary |
| State Transition Testing | record.status: ACTIVE → allow; ARCHIVED → reject | BR-HEALTH-UPDATE |
| Error Guessing | Cross-user PATCH, missing JWT, wrong role | BR-RBAC security |

### TDS-05 — Test Data

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path caller |
| `FX-002` | JWT | `{sub: 'ACC-999', role: 'MOTHER'}` | Another user |
| `FX-003` | JWT | `{sub: 'ACC-001', role: 'EXPERT'}` | Wrong role |
| `FX-004` | DB seed | `HR-001: owner=ACC-001, status=ACTIVE, type=LAB_RESULT` | Happy path record |
| `FX-005` | DB seed | `HR-002: owner=ACC-001, status=ARCHIVED` | Archived record |
| `FX-006` | DB seed | `HR-003: owner=ACC-999, status=ACTIVE` | Another user's record |
| `FX-007` | Input | `{title: "Updated Title", recordDate: "2026-06-20"}` | Valid partial update |
| `FX-008` | Input | `{recordDate: "2030-01-01"}` | Future date (invalid) |
| `FX-009` | Input | `{recordType: "INVALID_TYPE"}` | Invalid enum (invalid) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// HealthRecord40TestFactory.java
class HealthRecord40TestFactory {

    static final UUID ACC_001 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID ACC_999 = UUID.fromString("00000000-0000-0000-0000-000000000999");
    static final UUID HR_001  = UUID.fromString("00000000-0000-0000-0000-000000010001");
    static final UUID HR_002  = UUID.fromString("00000000-0000-0000-0000-000000010002");
    static final UUID HR_003  = UUID.fromString("00000000-0000-0000-0000-000000010003");

    static HealthRecord makeActiveRecord() {
        HealthRecord r = new HealthRecord();
        r.setHealthRecordId(HR_001);
        r.setOwnerUserId(ACC_001);
        r.setRecordType("LAB_RESULT");
        r.setTitle("Blood Test");
        r.setRecordDate(LocalDate.of(2026, 6, 15));
        r.setStatus("ACTIVE");
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return r;
    }

    static HealthRecord makeArchivedRecord() {
        HealthRecord r = makeActiveRecord();
        r.setHealthRecordId(HR_002);
        r.setStatus("ARCHIVED");
        return r;
    }

    static HealthRecord makeOtherUserRecord() {
        HealthRecord r = makeActiveRecord();
        r.setHealthRecordId(HR_003);
        r.setOwnerUserId(ACC_999);
        return r;
    }

    static UpdateHealthRecordRequest makeValidRequest() {
        UpdateHealthRecordRequest req = new UpdateHealthRecordRequest();
        req.setTitle("Updated Title");
        req.setRecordDate(LocalDate.of(2026, 6, 20));
        return req;
    }
}
```

---

### HEALTH40-TC-001 — Happy path: valid PATCH on ACTIVE record

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.updateHealthRecord()`
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-40 Normal Flow, CB-HEALTH-IMP-002 §6.1`

**Preconditions:**
- Fixture FX-004: HR-001 exists, status=ACTIVE, owner=ACC-001
- Fixture FX-001: caller JWT sub=ACC-001

**Test Steps:**
1. Mock `recordRepository.findById(HR_001)` → `makeActiveRecord()`
2. Mock `recordRepository.save(any)` → return updated record
3. Call `service.updateHealthRecord(HR_001, makeValidRequest(), ACC_001)`

**Expected Result (PASS):**
- Returns `UpdateHealthRecordResponse` with `title = "Updated Title"`, `recordDate = 2026-06-20`
- `recordRepository.save()` called exactly once
- `auditService.emit(HealthRecordUpdated)` called once
- `status` in response = `"ACTIVE"` (unchanged)

**Expected Result (FAIL):**
- Service throws exception, or save not called, or audit not emitted

**Current Status:** 🔴 Not written
**Implementation Note:** applyPatch() must only overwrite non-null fields; status must remain ACTIVE.

---

### HEALTH40-TC-002 — ARCHIVED record → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.assertActiveStatus()`
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-HEALTH-UPDATE, ADR-HEALTH-004, CB-HEALTH-IMP-002 §6.2`

**Preconditions:**
- Fixture FX-005: HR-002 exists, status=ARCHIVED, owner=ACC-001

**Test Steps:**
1. Mock `recordRepository.findById(HR_002)` → `makeArchivedRecord()`
2. Call `service.updateHealthRecord(HR_002, makeValidRequest(), ACC_001)`

**Expected Result (PASS):**
- Throws `ArchivedRecordException` with error code `HEALTH-006`
- `recordRepository.save()` NOT called
- `auditService.emit()` NOT called

**Expected Result (FAIL):**
- Exception not thrown; record is updated despite being ARCHIVED

**Current Status:** 🔴 Not written
**Implementation Note:** assertActiveStatus() must run before applyPatch() — see C1 constraint.

---

### HEALTH40-TC-003 — Another user's record → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.assertOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-RBAC, CB-HEALTH-IMP-002 §6.2`

**Preconditions:**
- Fixture FX-006: HR-003 exists, owner=ACC-999
- Caller: ACC-001 (FX-001)

**Test Steps:**
1. Mock `recordRepository.findById(HR_003)` → `makeOtherUserRecord()`
2. Call `service.updateHealthRecord(HR_003, makeValidRequest(), ACC_001)`

**Expected Result (PASS):**
- Throws `ForbiddenRecordAccessException` with error code `HEALTH-004`
- `recordRepository.save()` NOT called

**Expected Result (FAIL):**
- Record updated for another user → ownership bypass

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-004 — Record not found → 404

**Severity:** `HIGH`
**Feature Under Test:** `HealthRecordService.updateHealthRecord()` — findById empty
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-HEALTH-IMP-002 §10 HEALTH-007`

**Test Steps:**
1. Mock `recordRepository.findById(UUID.randomUUID())` → `Optional.empty()`
2. Call `service.updateHealthRecord(randomUUID, makeValidRequest(), ACC_001)`

**Expected Result (PASS):**
- Throws `RecordNotFoundException` with error code `HEALTH-007`

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-005 — Invalid recordType → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthRecordController` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/health/controller/HealthRecordControllerUpdateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Use `@WebMvcTest` — call PATCH with body `{"recordType": "INVALID_TYPE"}`
2. Use FX-001 JWT for auth

**Expected Result (PASS):**
- HTTP 400
- Response body contains `"code": "HEALTH-001"`

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-006 — Future recordDate → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@PastOrPresent` on UpdateHealthRecordRequest.recordDate`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. PATCH with body `{"recordDate": "2030-01-01"}`

**Expected Result (PASS):**
- HTTP 400, error code `HEALTH-001`

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-007 — All-null request body (no-op)

**Severity:** `LOW`
**Feature Under Test:** `applyPatch()` — no-op when all fields null
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Mock `recordRepository.findById(HR_001)` → `makeActiveRecord()`
2. Call `service.updateHealthRecord(HR_001, new UpdateHealthRecordRequest(), ACC_001)`
3. Capture the record passed to `save()`

**Expected Result (PASS):**
- Original fields (`title`, `recordType`, etc.) remain unchanged in the saved entity
- `save()` still called (updated_at refreshed)

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### HEALTH40-TC-SEC-001 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `HealthRecordController` — JWT security filter
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- No Authorization header

**Test Steps:**
1. PATCH `/api/v1/health-records/HR-001` without Authorization header

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 401
- Response body contains `"code": "IAM-001"`

**Expected Result (FAIL = lỗ hổng):**
- Request succeeds without JWT

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-SEC-002 — EXPERT role PATCH → 403

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on PATCH endpoint
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Test Steps:**
1. PATCH with FX-003 JWT (role=EXPERT)

**Expected Result (PASS):**
- HTTP 403

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### HEALTH40-TC-INT-001 — Full flow: PATCH ACTIVE record persists correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → Repository → DB`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthRecordUpdateIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied
- Seed HR-001 (ACTIVE, owner=ACC-001) via JPA

**Test Steps:**
1. Seed `HealthRecord` (status=ACTIVE, ownerUserId=ACC-001) to real DB
2. Call PATCH `/api/v1/health-records/{hr001Id}` with FX-001 JWT and `{title: "Integration Update"}`
3. Query DB via repository

**Expected Result (PASS):**
- HTTP 200
- `SELECT title FROM health_records WHERE health_record_id = hr001Id` = `"Integration Update"`
- `updated_at > created_at`

**DB Assertion:**
```java
HealthRecord updated = healthRecordRepository.findById(hr001Id).orElseThrow();
assertThat(updated.getTitle()).isEqualTo("Integration Update");
assertThat(updated.getStatus()).isEqualTo("ACTIVE");
assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
```

**Current Status:** 🔴 Not written

---

### HEALTH40-TC-INT-002 — PATCH ARCHIVED record in real DB → 409

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed `HealthRecord` (status=ARCHIVED, ownerUserId=ACC-001)
2. PATCH the record

**Expected Result (PASS):**
- HTTP 409
- DB record unchanged (status still ARCHIVED, title unchanged)

```java
HealthRecord unchanged = healthRecordRepository.findById(hr002Id).orElseThrow();
assertThat(unchanged.getStatus()).isEqualTo("ARCHIVED");
assertThat(unchanged.getTitle()).isEqualTo("Original Title"); // not modified
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `HEALTH40-TC-001` | `HealthRecordServiceUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-002` | `HealthRecordServiceUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-003` | `HealthRecordServiceUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-004` | `HealthRecordServiceUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-005` | `HealthRecordControllerUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-006` | `HealthRecordControllerUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-SEC-001` | `HealthRecordControllerUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-SEC-002` | `HealthRecordControllerUpdateTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-INT-001` | `HealthRecordUpdateIntegrationTest.java` | `[ ]` | `___` | — |
| `HEALTH40-TC-INT-002` | `HealthRecordUpdateIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// HealthRecordService.java — Red Phase stub
@Override
public UpdateHealthRecordResponse updateHealthRecord(UUID id,
                                                      UpdateHealthRecordRequest request,
                                                      UUID ownerUserId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|----------------------------------|
| `HEALTH40-TC-001` | throw | 🔴 FAIL | — |
| `HEALTH40-TC-002` | throw | 🔴 FAIL | — |
| `HEALTH40-TC-003` | throw | 🔴 FAIL | — |
| `HEALTH40-TC-INT-001` | throw | 🔴 FAIL | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-002` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] Không cần Flyway migration cho UC-40
- [ ] `HealthRecord` entity từ UC-39 đã tồn tại trong codebase

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] PATCH ACTIVE record → 200 OK, DB updated
- [ ] PATCH ARCHIVED record → 409 HEALTH-006
- [ ] PATCH another user's record → 403 HEALTH-004
- [ ] No business logic in Controller (only DTO validation + mapping)
- [ ] ownerUserId lấy từ JWT, không từ request body
- [ ] `HealthRecordUpdated` audit event emitted sau mỗi PATCH thành công

**Exit Criteria CASE 2.0:**

- [ ] **Red Gate** — tất cả tests FAIL với stub
- [ ] **Contract Existence** — `UpdateHealthRecordRequest`, `UpdateHealthRecordResponse`, `ArchivedRecordException` tồn tại
- [ ] **Props Isolation** — mỗi test dùng factory method, không shared mutable state

### Suspension Criteria

- `HealthRecord` entity chưa tồn tại (UC-39 chưa implement)
- Testcontainers không khởi động được trong CI environment

---

## 7. Rollback Plan

```bash
# Không có migration — chỉ revert code
git checkout -- src/main/java/com/carebridge/backend/health/service/HealthRecordService.java
git checkout -- src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java
git checkout -- src/test/java/com/carebridge/backend/health/service/HealthRecordServiceUpdateTest.java
git checkout -- src/test/java/com/carebridge/backend/health/HealthRecordUpdateIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-HEALTH-003/004 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC-002 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | PATCH logic không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | assertActiveStatus() trong Controller | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Import `HealthRecordFile` (không có trong V1) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — UC-40 Update Health Record*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
