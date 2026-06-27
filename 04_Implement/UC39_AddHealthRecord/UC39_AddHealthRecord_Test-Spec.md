# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-39 Add Health Record

**Document ID:** `CB-HEALTH-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC39_AddHealthRecord/UC39_AddHealthRecord_TDS.md` (CB-HEALTH-IMP-001)
- SRS: §3.3.1.16

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-39 |

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
| **Feature / Gap ID** | `UC-39` |
| **Module** | `AddHealthRecord — health` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "uploads" — không rõ file ownership | ADR-HEALTH-002: FileOwnershipValidator chạy trước save | Test encode ownership check before save |
| L2 | SRS: không rõ recordDate constraint | BR-HEALTH-002: @PastOrPresent | Test encode future date → 400 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid record creation | `HealthRecordService.addHealthRecord()` | `HEALTH-TC-001` |
| TC-COND-002 | File not owned by caller | `FileOwnershipValidator` | `HEALTH-TC-002` |
| TC-COND-003 | Future recordDate | DTO `@PastOrPresent` | `HEALTH-TC-003` |
| TC-COND-004 | Invalid recordType | DTO validation | `HEALTH-TC-004` |
| TC-COND-005 | No JWT | JWT filter | `HEALTH-TC-SEC-001` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path |
| `FX-002` | Input | `{recordType: "LAB_RESULT", title: "Blood Test", recordDate: "2026-06-15"}` | Happy path |
| `FX-003` | DB | `files.id = FILE-001, account_id = ACC-001` | Owned file |
| `FX-004` | DB | `files.id = FILE-002, account_id = ACC-999` | Foreign file |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class HealthRecordTestFactory {
    static HealthRecord makeRecord() {
        HealthRecord r = new HealthRecord();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        r.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setRecordType(RecordType.LAB_RESULT);
        r.setTitle("Blood Test");
        r.setRecordDate(LocalDate.of(2026, 6, 15));
        r.setStatus(HealthRecordStatus.ACTIVE);
        return r;
    }

    static AddHealthRecordRequest makeRequest() {
        AddHealthRecordRequest r = new AddHealthRecordRequest();
        r.setRecordType(RecordType.LAB_RESULT);
        r.setTitle("Blood Test");
        r.setRecordDate(LocalDate.of(2026, 6, 15));
        return r;
    }
}
```

---

### HEALTH-TC-001 — Happy path (no files)

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.addHealthRecord()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `UC-39 Normal Flow`

**Test Steps:**
1. Call `addHealthRecord(FX-002, ACC-001)` with no fileIds
2. Mock `recordRepository.save()` → saved

**Expected Result (PASS):**
- Returns response with `status = "ACTIVE"`, `recordType = "LAB_RESULT"`
- `auditService.emit(HealthRecordAdded)` called

**Current Status:** 🔴 Not written

---

### HEALTH-TC-002 — File not owned → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `FileOwnershipValidator`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-HEALTH-002`

**Test Steps:**
1. Call `addHealthRecord()` with `fileIds = [FILE-002]` (owned by ACC-999)
2. Mock `FileOwnershipValidator.validate()` → throws exception

**Expected Result (PASS):**
- Throws `ForbiddenFileReferenceException` with code `HEALTH-003`
- `recordRepository.save()` NOT called

**Current Status:** 🔴 Not written

---

### HEALTH-TC-003 — Future recordDate → 400

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Test Steps:** POST with `recordDate = "2030-01-01"` via @WebMvcTest

**Expected Result:** 400

**Current Status:** 🔴 Not written

---

### HEALTH-TC-004 — Invalid recordType → 400

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** 400 validation error

**Current Status:** 🔴 Not written

---

### HEALTH-TC-SEC-001 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021`
**TDD Phase:** 🔴 RED

**Expected Result:** 401

**Current Status:** 🔴 Not written

---

### HEALTH-TC-INT-001 — File linked correctly in DB

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed FILE-001 with account_id=ACC-001
2. POST with fileIds=[FILE-001]
3. Assert `SELECT COUNT(*) FROM health_record_files WHERE file_id='FILE-001'` = 1

```java
List<HealthRecordFile> files = hrf_repo.findByHealthRecordId(savedId);
assertThat(files).hasSize(1);
assertThat(files.get(0).getFileId()).isEqualTo(FILE_001);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `HEALTH-TC-001` | `[ ]` | `___` | — |
| `HEALTH-TC-002` | `[ ]` | `___` | — |
| `HEALTH-TC-003` | `[ ]` | `___` | — |
| `HEALTH-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] FileOwnershipValidator tested and working
- [ ] File linking to health_record_files table tested
- [ ] No medical interpretation in responses
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/health/
git checkout -- src/main/resources/db/migration/V22__create_health_records.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-001 | ☐ | G-0 |
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-005 | ☐ FileOwnershipValidator exists | G-3 |
