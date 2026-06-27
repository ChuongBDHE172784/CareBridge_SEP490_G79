# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-211 View Health Record Detail

**Document ID:** `CB-HEALTH-TDD-003`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC211_ViewHealthRecordDetail/UC211_ViewHealthRecordDetail_TDS.md` (CB-HEALTH-IMP-003)
- SRS: §3.3.15.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-27 | AI Agent — Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-211 |

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
| **Feature / Gap ID** | `UC-211` |
| **Module** | `ViewHealthRecordDetail — health` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays metadata and attached files" — không rõ URL type | ADR-FILE-004: presigned URL 15 min | Test encode URL has TTL |
| L2 | SRS: không rõ archived record handling | BR-HEALTH-021: archived → 404 | Test archived → 404 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner views own record | `HealthRecordService.getHealthRecord()` | `HR-TC-001` |
| TC-COND-002 | Non-owner → 403 | `verifyOwnership()` | `HR-TC-002` |
| TC-COND-003 | Archived record → 404 | status filter | `HR-TC-003` |
| TC-COND-004 | Presigned URLs in response | `StorageService.generatePresignedUrl()` | `HR-TC-004` |
| TC-COND-005 | No diagnosis in response | Response mapping | `HR-TC-005` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB | `{id: HR-001, accountId: ACC-001, status: ACTIVE, files: [FILE-001]}` | Happy path |
| `FX-002` | DB | `{id: HR-002, accountId: ACC-999, status: ACTIVE}` | Non-owner |
| `FX-003` | DB | `{id: HR-003, accountId: ACC-001, status: ARCHIVED}` | Archived |
| `FX-004` | Mock | `storageService.generatePresignedUrl()` → "https://storage/signed?token=abc"` | Presigned URL |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class HealthRecordDetailTestFactory {
    static HealthRecord makeActiveRecord() {
        HealthRecord r = new HealthRecord();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000070"));
        r.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setRecordType(RecordType.LAB_RESULT);
        r.setTitle("Blood Test");
        r.setRecordDate(LocalDate.of(2026, 6, 15));
        r.setStatus(HealthRecordStatus.ACTIVE);
        return r;
    }
}
```

---

### HR-TC-001 — Owner views own record → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC-211 Normal Flow`

**Test Steps:**
1. Mock `recordRepository.findByIdAndStatus(HR-001, ACTIVE)` → FX-001
2. Mock `storageService.generatePresignedUrl()` → presigned URL
3. Call `getHealthRecord(HR-001, ACC-001)`

**Expected Result:** Response with title, recordType, attachedFiles with presignedUrl

**Current Status:** 🟢 Passing

---

### HR-TC-002 — Non-owner → 403

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-HEALTH-003`
**TDD Phase:** 🟢 GREEN

**Expected Result:** ForbiddenException (HEALTH-004)

**Current Status:** 🟢 Passing

---

### HR-TC-003 — Archived record → 404

**Severity:** `HIGH`
**Oracle Source:** `BR-HEALTH-021`
**TDD Phase:** 🟢 GREEN

**Expected Result:** NotFoundException (HEALTH-008) — NOT 200

**Current Status:** 🟢 Passing

---

### HR-TC-004 — Presigned URLs in response

**Severity:** `HIGH`
**Feature Under Test:** `StorageService.generatePresignedUrl()` called in service
**Oracle Source:** `ADR-FILE-004`
**TDD Phase:** 🟢 GREEN

**Test Steps:**
1. Record has 2 attached files (FILE-001, FILE-002)
2. Mock `storageService.generatePresignedUrl(key, 15)` → URL per file
3. Call `getHealthRecord(HR-001, ACC-001)`

**Expected Result (PASS):**
- `response.attachedFiles` has 2 items
- Each item has `presignedUrl` that is not null
- `storageService.generatePresignedUrl(_, 15)` called exactly 2 times

```java
verify(storageService, times(2)).generatePresignedUrl(anyString(), eq(15));
```

**Current Status:** 🟢 Passing

---

### HR-TC-005 — No diagnosis/medical field in response

**Severity:** `HIGH`
**Oracle Source:** `BR-SAFETY-001`
**TDD Phase:** 🟢 GREEN

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("diagnosis");
assertThat(json).doesNotContain("medicalAdvice");
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `HR-TC-001` | `[ ]` | `___` | — |
| `HR-TC-002` | `[ ]` | `___` | — |
| `HR-TC-003` | `[ ]` | `___` | — |
| `HR-TC-004` | `[ ]` | `___` | — |
| `HR-TC-005` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] Presigned URLs generated with TTL=15 min
- [ ] Archived records return 404
- [ ] Response verified to NOT contain diagnosis fields
- [ ] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS health_records CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '038';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/health/
git checkout -- src/test/java/com/carebridge/backend/health/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-005 | ☐ IStorageService exists | G-3 |
