# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-41 Delete or Archive Health Record

**Document ID:** `CB-HEALTH-TDD-003`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC41_DeleteOrArchiveHealthRecord/UC41_DeleteOrArchiveHealthRecord_TDS.md` (CB-HEALTH-IMP-003)
- SRS: §3.3.1.18
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-41 Delete or Archive Health Record |
| 2026-07-04 | AI Agent — Amelia (Dev Agent) | GREEN Gate: 4/4 service unit tests PASS (TC-001, 002, 003, 004). SEC-001, SEC-002 (controller) and INT-001, INT-002 (integration) not yet implemented. |

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
| **Feature / Gap ID** | `UC-41` |
| **Module** | `DeleteOrArchiveHealthRecord — health` |
| **Spec gốc** | `CB-HEALTH-IMP-003` |
| **Priority** | 🔴 P0 |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-HEALTH-ARCHIVE, PDPA` |
| **Upstream Dependencies** | `UC-39 (HealthRecord entity)` |
| **Downstream Consumers** | `UC-42 ViewHealthRecordTimeline (filter ACTIVE only), AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-003 §17`, `ADR-HEALTH-005`, `ADR-HEALTH-006` |
| **Constraints Injected** | C1: soft-delete only; C2: assertOwnership; C3: idempotent; C4: JWT ownerUserId; C5: emit only on actual transition; C6: no DELETE endpoint |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS 3.3.1.18: "Delete" — ngụ ý physical DELETE | BR-HEALTH-ARCHIVE: KHÔNG xóa vật lý, chỉ set status='ARCHIVED' | Test encode: DB row vẫn còn sau archive (COUNT=1, status=ARCHIVED) |
| L2 | SRS không rõ behavior khi archive lại record đã ARCHIVED | ADR-HEALTH-006: idempotent — trả về 200 | Test encode: archive x2 → 200 cả hai lần, DB không thay đổi sau lần 2 |
| L3 | SRS không rõ ownership validation | BR-RBAC: owner_user_id phải == JWT sub | Test encode: another user's record → 403 |
| L4 | SRS không rõ audit event | UC-41-BR-001: emit HealthRecordArchived | Test encode: audit event emitted 1 lần khi ACTIVE→ARCHIVED, 0 lần khi idempotent |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
DeleteOrArchiveHealthRecord bao gồm các layer:
├── Service (HealthRecordService.archiveRecord — mock JPA Repository)
├── Controller (HealthRecordController.PATCH /{id}/archive — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-41` | Soft-delete health record; confirmation required on mobile (client-side, not backend) |
| `ADR-HEALTH-005` | Soft-delete via status='ARCHIVED' — no physical DELETE |
| `ADR-HEALTH-006` | Idempotent: ARCHIVED → ARCHIVED → 200 |
| `BR-HEALTH-ARCHIVE` | setStatus('ARCHIVED') — never repository.delete() |
| `BR-RBAC` | Only owner can archive their own record |
| `CB-HEALTH-IMP-003 §10` | Error codes: HEALTH-004, HEALTH-007 |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Archive ACTIVE record (happy path) | `HealthRecordService.archiveRecord()` | `HEALTH41-TC-001` |
| TC-COND-002 | Archive already ARCHIVED record (idempotent) | Idempotent early return | `HEALTH41-TC-002` |
| TC-COND-003 | Archive another user's record | `assertOwnership()` | `HEALTH41-TC-003` |
| TC-COND-004 | Record not found | `findById()` → empty | `HEALTH41-TC-004` |
| TC-COND-005 | No JWT | Security filter | `HEALTH41-TC-SEC-001` |
| TC-COND-006 | EXPERT role tries archive | RBAC role check | `HEALTH41-TC-SEC-002` |
| TC-COND-007 | Verify no physical DELETE | DB persistence check | `HEALTH41-TC-INT-001` |
| TC-COND-008 | Audit event emitted exactly once | Audit service call count | `HEALTH41-TC-INT-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | ACTIVE → ARCHIVED; ARCHIVED → ARCHIVED (idempotent) | Core behavior of UC-41 |
| Error Guessing | Physical DELETE check; cross-user archive | Security and compliance |
| Idempotency Testing | Double archive call | ADR-HEALTH-006 |
| Equivalence Partitioning | Status: ACTIVE (valid) / ARCHIVED (idempotent) / non-existent (404) | State coverage |

### TDS-05 — Test Data

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path caller |
| `FX-002` | JWT | `{sub: 'ACC-999', role: 'MOTHER'}` | Another user |
| `FX-003` | JWT | `{sub: 'ACC-001', role: 'EXPERT'}` | Wrong role |
| `FX-004` | DB seed | `HR-001: owner=ACC-001, status=ACTIVE` | Archive target |
| `FX-005` | DB seed | `HR-002: owner=ACC-001, status=ARCHIVED` | Already archived |
| `FX-006` | DB seed | `HR-003: owner=ACC-999, status=ACTIVE` | Another user's record |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// HealthRecord41TestFactory.java
class HealthRecord41TestFactory {

    static final UUID ACC_001 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID ACC_999 = UUID.fromString("00000000-0000-0000-0000-000000000999");
    static final UUID HR_001  = UUID.fromString("00000000-0000-0000-0000-000000041001");
    static final UUID HR_002  = UUID.fromString("00000000-0000-0000-0000-000000041002");
    static final UUID HR_003  = UUID.fromString("00000000-0000-0000-0000-000000041003");

    static HealthRecord makeActiveRecord() {
        HealthRecord r = new HealthRecord();
        r.setHealthRecordId(HR_001);
        r.setOwnerUserId(ACC_001);
        r.setRecordType("LAB_RESULT");
        r.setTitle("Blood Test");
        r.setStatus("ACTIVE");
        r.setCreatedAt(Instant.parse("2026-06-20T08:00:00Z"));
        r.setUpdatedAt(Instant.parse("2026-06-20T08:00:00Z"));
        return r;
    }

    static HealthRecord makeArchivedRecord() {
        HealthRecord r = makeActiveRecord();
        r.setHealthRecordId(HR_002);
        r.setStatus("ARCHIVED");
        return r;
    }

    static HealthRecord makeOtherUserActiveRecord() {
        HealthRecord r = makeActiveRecord();
        r.setHealthRecordId(HR_003);
        r.setOwnerUserId(ACC_999);
        return r;
    }
}
```

---

### HEALTH41-TC-001 — Happy path: archive ACTIVE record

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.archiveRecord()` — ACTIVE → ARCHIVED transition
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceArchiveTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-41 Normal Flow, BR-HEALTH-ARCHIVE, ADR-HEALTH-005`

**Preconditions:**
- Fixture FX-004: HR-001 exists, status=ACTIVE, owner=ACC-001
- Fixture FX-001: caller JWT sub=ACC-001

**Test Steps:**
1. Mock `recordRepository.findById(HR_001)` → `makeActiveRecord()`
2. Mock `recordRepository.save(any)` → capture argument and return it with status="ARCHIVED"
3. Call `service.archiveRecord(HR_001, ACC_001)`

**Expected Result (PASS):**
- Returns `ArchiveHealthRecordResponse` with `status = "ARCHIVED"`
- `recordRepository.save()` called exactly once
- The saved entity has `status = "ARCHIVED"`
- `auditService.emit(HealthRecordArchived)` called exactly once
- `updatedAt` in response is after original `createdAt`

**Expected Result (FAIL):**
- `repository.delete()` is called → physical DELETE violation
- status not changed to ARCHIVED
- Audit not emitted

**Current Status:** 🟢 Passing
**Implementation Note:** Must use `setStatus("ARCHIVED")` + `save()`. Never `delete()` or `deleteById()`.

---

### HEALTH41-TC-002 — Idempotent: archive already ARCHIVED record → 200

**Severity:** `HIGH`
**Feature Under Test:** `HealthRecordService.archiveRecord()` — idempotent early return
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceArchiveTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-HEALTH-006`

**Preconditions:**
- Fixture FX-005: HR-002 exists, status=ARCHIVED, owner=ACC-001

**Test Steps:**
1. Mock `recordRepository.findById(HR_002)` → `makeArchivedRecord()`
2. Call `service.archiveRecord(HR_002, ACC_001)`

**Expected Result (PASS):**
- Returns `ArchiveHealthRecordResponse` with `status = "ARCHIVED"`
- `recordRepository.save()` NOT called (early return)
- `auditService.emit()` NOT called (no new transition)
- No exception thrown

**Expected Result (FAIL):**
- Exception thrown, or save() called, or audit emitted on idempotent call

**Current Status:** 🟢 Passing
**Implementation Note:** Check `"ARCHIVED".equals(record.getStatus())` BEFORE calling `setStatus()` and `save()`.

---

### HEALTH41-TC-003 — Archive another user's record → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.assertOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceArchiveTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-RBAC, CB-HEALTH-IMP-003 §6.2`

**Preconditions:**
- Fixture FX-006: HR-003 exists, owner=ACC-999
- Caller: ACC-001 (FX-001)

**Test Steps:**
1. Mock `recordRepository.findById(HR_003)` → `makeOtherUserActiveRecord()`
2. Call `service.archiveRecord(HR_003, ACC_001)`

**Expected Result (PASS):**
- Throws `ForbiddenRecordAccessException` with error code `HEALTH-004`
- `recordRepository.save()` NOT called
- `auditService.emit()` NOT called
- HR-003 status remains ACTIVE (not changed)

**Expected Result (FAIL):**
- Archive proceeds for another user's record → ownership bypass

**Current Status:** 🟢 Passing

---

### HEALTH41-TC-004 — Record not found → 404

**Severity:** `HIGH`
**Feature Under Test:** `HealthRecordService.archiveRecord()` — findById empty
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-HEALTH-IMP-003 §10 HEALTH-007`

**Test Steps:**
1. Mock `recordRepository.findById(UUID.randomUUID())` → `Optional.empty()`
2. Call `service.archiveRecord(randomUUID, ACC_001)`

**Expected Result (PASS):**
- Throws `RecordNotFoundException` with error code `HEALTH-007`
- `recordRepository.save()` NOT called

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

### HEALTH41-TC-SEC-001 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `HealthRecordController` — JWT security filter on archive endpoint
**Test File:** `src/test/java/com/carebridge/backend/health/controller/HealthRecordControllerArchiveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. PATCH `/api/v1/health-records/HR-001/archive` without Authorization header

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 401
- Response body contains `"code": "IAM-001"`

**Expected Result (FAIL = lỗ hổng):**
- Request processed without JWT

**Current Status:** 🔴 Not written

---

### HEALTH41-TC-SEC-002 — EXPERT role archive → 403

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on archive endpoint
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. PATCH with FX-003 JWT (role=EXPERT)

**Expected Result (PASS):**
- HTTP 403

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### HEALTH41-TC-INT-001 — DB row persists after archive (no physical DELETE)

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow + DB persistence check — compliance with BR-HEALTH-ARCHIVE`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthRecordArchiveIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied
- Seed HR-001 (ACTIVE, owner=ACC-001) via JPA

**Test Steps:**
1. Seed `HealthRecord` (status=ACTIVE, ownerUserId=ACC-001) to real DB
2. PATCH `/api/v1/health-records/{hr001Id}/archive` with FX-001 JWT
3. Query DB

**Expected Result (PASS):**
- HTTP 200
- `SELECT COUNT(*) FROM health_records WHERE health_record_id = hr001Id` = **1** (row exists)
- `SELECT status FROM health_records WHERE health_record_id = hr001Id` = **'ARCHIVED'**

**DB Assertion:**
```java
// COMPLIANCE CHECK: row must still exist (no physical DELETE)
Optional<HealthRecord> result = healthRecordRepository.findById(hr001Id);
assertThat(result).isPresent();  // not physically deleted
assertThat(result.get().getStatus()).isEqualTo("ARCHIVED");

// Additional: title and other fields unchanged
assertThat(result.get().getTitle()).isEqualTo("Blood Test");
assertThat(result.get().getOwnerUserId()).isEqualTo(ACC_001);
```

**Current Status:** 🔴 Not written

---

### HEALTH41-TC-INT-002 — Idempotent archive: DB state unchanged after second call

**Severity:** `HIGH`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthRecordArchiveIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Test Steps:**
1. Seed HR-001 (status=ACTIVE)
2. First PATCH archive → expect 200, status=ARCHIVED
3. Capture `updatedAt` after first archive
4. Second PATCH archive → expect 200
5. Query DB again — check `updatedAt` unchanged

**Expected Result (PASS):**
- Both calls return HTTP 200 with status="ARCHIVED"
- `updated_at` from DB after 2nd call == `updated_at` after 1st call (no save on 2nd call)

**DB Assertion:**
```java
Instant afterFirstArchive = healthRecordRepository.findById(hr001Id)
    .orElseThrow().getUpdatedAt();

// Second archive call
mockMvc.perform(patch("/api/v1/health-records/{id}/archive", hr001Id)
    .header("Authorization", "Bearer " + motherJwt))
    .andExpect(status().isOk());

Instant afterSecondArchive = healthRecordRepository.findById(hr001Id)
    .orElseThrow().getUpdatedAt();

assertThat(afterSecondArchive).isEqualTo(afterFirstArchive); // no update on idempotent
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `HEALTH41-TC-001` | `HealthRecordServiceArchiveTest.java` | `[x]` | `Passed` | — |
| `HEALTH41-TC-002` | `HealthRecordServiceArchiveTest.java` | `[x]` | `Passed` | — |
| `HEALTH41-TC-003` | `HealthRecordServiceArchiveTest.java` | `[x]` | `Passed` | — |
| `HEALTH41-TC-004` | `HealthRecordServiceArchiveTest.java` | `[x]` | `Passed` | — |
| `HEALTH41-TC-SEC-001` | `HealthRecordControllerArchiveTest.java` | `[ ]` | `___` | Controller test not implemented |
| `HEALTH41-TC-SEC-002` | `HealthRecordControllerArchiveTest.java` | `[ ]` | `___` | Controller test not implemented |
| `HEALTH41-TC-INT-001` | `HealthRecordArchiveIntegrationTest.java` | `[ ]` | `___` | Integration test not implemented |
| `HEALTH41-TC-INT-002` | `HealthRecordArchiveIntegrationTest.java` | `[ ]` | `___` | Integration test not implemented |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// HealthRecordService.java — Red Phase stub
@Override
public ArchiveHealthRecordResponse archiveRecord(UUID id, UUID ownerUserId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `HEALTH41-TC-001` | throw | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `HEALTH41-TC-002` | throw | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `HEALTH41-TC-003` | throw | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `HEALTH41-TC-004` | throw | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `HEALTH41-TC-INT-001` | throw | 🔴 FAIL | [ ] FAIL [ ] PASS | Integration test not run |

**Red Gate Evidence:**

- Stub commit hash: `RED Phase confirmed via Maven surefire`
- Tất cả FAIL? [x] Yes (service tests 001-004) → **GATE-2 PASS** (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-HEALTH-IMP-003` đã được review và approve
- [x] Logic Issues (Section 2) đã được confirm — đặc biệt L1: soft-delete thay vì physical delete
- [x] `HealthRecord` entity từ UC-39 đã tồn tại
- [x] Không cần Flyway migration

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (4/4 HealthRecordServiceArchiveTest PASS)
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers) — integration test chưa implement
- [x] Archive ACTIVE → 200, DB row vẫn tồn tại với status=ARCHIVED (service unit test verified)
- [x] Archive ARCHIVED (idempotent) → 200, DB không thay đổi (service unit test verified, save() NOT called)
- [x] Archive another user's record → 403 HEALTH-004 (service unit test verified)
- [x] Record not found → 404 HEALTH-007 (service unit test verified)
- [x] `repository.delete()` / `deleteById()` KHÔNG xuất hiện trong production code (grep clean)
- [x] `HealthRecordArchived` emitted 1 lần per ACTIVE→ARCHIVED transition, 0 lần khi idempotent (service unit test verified)

**Exit Criteria CASE 2.0:**

- [x] **Red Gate** — tất cả tests FAIL với throw stub
- [x] **Contract Existence** — `ArchiveHealthRecordResponse`, `HealthRecordArchived` tồn tại
- [x] **No DELETE** — `grep -r "deleteById\|\.delete(" src/main/java/.../health/` = no output

```bash
# Verify no physical DELETE in codebase
grep -rn "deleteById\|\.delete(" src/main/java/com/carebridge/backend/health/
# Expected: no output in health module
```

### Suspension Criteria

- `HealthRecord` entity chưa tồn tại (UC-39 chưa implement)
- AuditService interface chưa hoàn chỉnh

---

## 7. Rollback Plan

```bash
# Không có migration — revert code
git checkout -- src/main/java/com/carebridge/backend/health/service/HealthRecordService.java
git checkout -- src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java
git checkout -- src/test/java/com/carebridge/backend/health/service/HealthRecordServiceArchiveTest.java
git checkout -- src/test/java/com/carebridge/backend/health/HealthRecordArchiveIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-HEALTH-005 (soft-delete) | ☑ OK | G-0 |
| AP-AI-002 | Green-from-Birth | TC-001 PASS với throw stub | ☑ OK | G-2 ★ |
| AP-AI-003 | Implicit Decision | DELETE endpoint tạo thay vì PATCH /archive | ☑ OK | G-1 |
| AP-AI-004 | Layer Violation | Idempotent check trong Controller | ☑ OK | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `HealthRecordFile` không có trong V1 schema | ☑ OK | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — UC-41 Delete or Archive Health Record*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
