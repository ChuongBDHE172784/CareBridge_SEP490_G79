# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-167 Upload File

**Document ID:** `CB-FILE-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC167_UploadFile/UC167_UploadFile_TDS.md` (CB-FILE-IMP-001)
- SRS: §3.3.10.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-167 |

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
| **Feature / Gap ID** | `UC-167` |
| **Module** | `UploadFile — file` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "file, size, and ownership checks" — không rõ limits | ADR-FILE-001: max 20MB, allowed MIMEs | Test encode size/type checks |
| L2 | SRS: không rõ storageKey format | ADR-FILE-003: UUID-based key, not filename | Test that storageKey != originalName |
| L3 | SRS: không rõ quota | ADR-FILE-002: max 500 files per account | Test quota exceeded |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid JPEG upload | `FileService.uploadFile()` | `FILE-TC-001` |
| TC-COND-002 | Valid PDF upload | `FileService.uploadFile()` | `FILE-TC-002` |
| TC-COND-003 | File too large | `validateFileSize()` | `FILE-TC-003` |
| TC-COND-004 | Invalid MIME type | `validateFileType()` | `FILE-TC-004` |
| TC-COND-005 | Storage quota exceeded | `validateStorageQuota()` | `FILE-TC-005` |
| TC-COND-006 | storageKey UUID-based | `generateStorageKey()` | `FILE-TC-006` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path |
| `FX-002` | File | `2MB JPEG file mock` | Valid upload |
| `FX-003` | File | `25MB file mock` | Size exceeded |
| `FX-004` | File | `.exe file mock, MIME: application/x-msdownload` | Invalid type |
| `FX-005` | DB | 500 files for ACC-001 | Quota exceeded |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class FileTestFactory {
    static MockMultipartFile makeJpeg() {
        return new MockMultipartFile(
            "file", "ultrasound.jpg",
            "image/jpeg",
            new byte[2 * 1024 * 1024]  // 2MB
        );
    }

    static MockMultipartFile makeLargeFile() {
        return new MockMultipartFile(
            "file", "large.jpg",
            "image/jpeg",
            new byte[25 * 1024 * 1024]  // 25MB
        );
    }

    static MockMultipartFile makeExeFile() {
        return new MockMultipartFile(
            "file", "virus.exe",
            "application/x-msdownload",
            new byte[100]
        );
    }
}
```

---

### FILE-TC-001 — Happy path: JPEG upload

**Severity:** `CRITICAL`
**Feature Under Test:** `FileService.uploadFile()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `UC-167 Normal Flow`

**Test Steps:**
1. Mock `storageService.store()` → OK
2. Mock `fileRepository.save()` → saved FileRecord
3. Mock `fileRepository.countByAccountIdAndStatus()` → 0
4. Call `fileService.uploadFile(FX-002, ACC-001)`

**Expected Result (PASS):**
- Returns response with `originalName = "ultrasound.jpg"`, `mimeType = "image/jpeg"`
- `storageService.store()` called with UUID-based storageKey (not "ultrasound.jpg")
- `auditService.emit(FileUploaded)` called

**Current Status:** 🔴 Not written

---

### FILE-TC-002 — Valid PDF upload

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:** 201

**Current Status:** 🔴 Not written

---

### FILE-TC-003 — File too large → 400

**Severity:** `CRITICAL`
**Feature Under Test:** `FileService.validateFileSize()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FILE-001 (max 20MB)`

**Test Steps:**
1. Upload FX-003 (25MB)

**Expected Result (PASS):**
- Throws `FileSizeExceededException` (FILE-002)
- `storageService.store()` NOT called

**Current Status:** 🔴 Not written

---

### FILE-TC-004 — Invalid MIME type → 400

**Severity:** `CRITICAL`
**Feature Under Test:** `FileService.validateFileType()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FILE-001`

**Test Steps:**
1. Upload FX-004 (.exe)

**Expected Result:** throws FILE-001

**Current Status:** 🔴 Not written

---

### FILE-TC-005 — Storage quota exceeded → 409

**Severity:** `HIGH`
**Feature Under Test:** `FileService.validateStorageQuota()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FILE-002`

**Test Steps:**
1. Mock `fileRepository.countByAccountIdAndStatus()` → 500
2. Upload new file

**Expected Result:** throws FILE-003

**Current Status:** 🔴 Not written

---

### FILE-TC-006 — storageKey is UUID-based, NOT originalName

**Severity:** `HIGH`
**Feature Under Test:** `FileService.generateStorageKey()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FILE-003`

**Test Steps:**
1. Upload JPEG
2. Capture storageKey passed to `storageService.store()`

**Expected Result (PASS):**
- storageKey matches pattern `[UUID]\.jpg` (not `ultrasound.jpg`)

**Current Status:** 🔴 Not written

---

### FILE-TC-INT-001 — File record in DB after upload

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

```java
FileRecord saved = fileRepo.findByAccountId(ACC_001).get(0);
assertThat(saved.getMimeType()).isEqualTo("image/jpeg");
assertThat(saved.getStatus()).isEqualTo(FileStatus.ACTIVE);
assertThat(saved.getStorageKey()).doesNotContain("ultrasound.jpg");
assertThat(saved.getSizeBytes()).isEqualTo(2 * 1024 * 1024L);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `FILE-TC-001` | `[ ]` | `___` | — |
| `FILE-TC-003` | `[ ]` | `___` | — |
| `FILE-TC-004` | `[ ]` | `___` | — |
| `FILE-TC-005` | `[ ]` | `___` | — |
| `FILE-TC-006` | `[ ]` | `___` | — |
| `FILE-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] MIME type validated from actual content (not just extension)
- [ ] storageKey is UUID-based (security requirement)
- [ ] Quota check before write to storage
- [ ] Presigned URL TTL = 15 min (tested in UC-168)
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/file/
git checkout -- src/main/resources/db/migration/V25__create_files.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-001 | ☐ | G-0 |
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-005 | ☐ IStorageService exists | G-3 |
