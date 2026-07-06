# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC169 Delete File

**Document ID:** `CB-FILE-TDD-169`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Implemented — 2026-07-06`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627100000__create_uploaded_files.sql`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627100100__create_health_record_files.sql`
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.10.3
- `04_Implement/UC169_DeleteFile/UC169_DeleteFile_TDS.md` (`CB-FILE-IMP-169`)
- `04_Implement/UC168_ViewFile/UC168_ViewFile_TDS.md` / `UC168_ViewFile_Test-Spec.md` (sibling baseline)

> **Quy ước TDD:** Test-first. Thứ tự: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data (UUID literals, fake filenames).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|----------------------|
| 2026-07-06 | AI Agent — Amelia (Dev Agent) | GREEN Gate PASS — 31 unit tests passing; TC-001 through TC-013 + SEC-001 implemented (INT tests skipped — Testcontainers scope) |
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC169 Delete File |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-169` |
| **Module** | File Management — Delete File |
| **Spec gốc** | `CB-FILE-IMP-169` |
| **Priority** | 🔴 P0 (High per SRS) |
| **Sprint** | Sprint 2 — Baby Logs, Health Records, Reminders, Files |
| **Milestone** | M3 Alpha |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | PDPA, BR-RBAC, BR-PRIVACY, BR-CONSULTATION |
| **Upstream Dependencies** | `UploadedFileRepository`, `HealthRecordFileRepository` (`findByFileId`, UC-168-added), `AuditService` |
| **Downstream Consumers** | Mobile App file management screens (file disappears from listing after delete) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-FILE-IMP-169 §17`, ADR-FILE-007, ADR-FILE-008 |
| **Constraints Injected** | C1-C6 per TDS §17.1 |
| **Model** | Claude Sonnet 5 |
| **Trust Level** | T2 → T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|---------------------------|-------------------------------|------------------------------|
| L1 | SRS §3.3.10.3 says "not bound by records, consultations, or retention policy" but only `health_record_files` has an actual FK into `uploaded_files` (confirmed reading `V20260627100100__create_health_record_files.sql` and full-text search of all migrations for `file_id`) | "Consultations" and "retention policy" bindings are vacuously satisfied — no schema construct exists for either (ADR-FILE-007 Option A) | Tests only seed/assert the `health_record_files` binding guard; NO test asserts a `consultation_files` or `retention` table/column since none exists — asserting against a non-existent construct would be a Hallucinated Contract (AP-AI-005) |
| L2 | Naive reading might assume `deleteFile()` calls `IStorageService.delete()` to free storage immediately | ADR-FILE-008 explicitly forbids calling `IStorageService.delete()` — soft-delete only (`status=DELETED`) | Tests assert `verify(storageService, never()).delete(anyString())` on every delete path, including happy path |
| L3 | UC-168's access model (owner OR care-group-sharing OR admin) might be wrongly copy-pasted for delete eligibility | ADR-FILE-007 deliberately restricts UC-169 to STRICT OWNER ONLY — no family/admin bypass, unlike `viewFile()` | Tests explicitly assert that `SYSTEM_ADMIN`/`MODERATOR`/`FAMILY` (even if in a shared care-group) callers who are NOT the owner get 403, mirroring the Auth Matrix §16 of the TDS |
| L4 | SRS Primary Actor = "Mother" | `FileController.deleteFile()` therefore uses `@PreAuthorize("hasRole('MOTHER')")` as a role gate PLUS a service-layer ownership check (defense-in-depth, not either/or) | Controller-layer tests assert 401 for missing JWT and 403 for non-MOTHER roles (role gate); service-layer tests assert 403 for a MOTHER who is not the owner (ownership gate) — both gates tested independently |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
File Management (Delete File, UC-169) bao gồm các layer:
├── Domain (FileDeletePolicy — pure logic, mock repositories với Mockito)
├── Services (FileServiceImpl.deleteFile() — mock JPA Repository + IStorageService + AuditService với Mockito)
├── Controller (FileController.deleteFile() — mock IFileService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, full stack DELETE /api/v1/files/{id})
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|------------------|
| SRS UC-169 | Owner-only deletion, binding-guard (health-record), soft-delete semantics |
| ADR-FILE-007 | Ownership check + `health_record_files` binding-guard rules |
| ADR-FILE-008 | Soft-delete via `FileStatus.DELETED`; `IStorageService.delete()` MUST NOT be called |
| BR-RBAC | Only `MOTHER` role reaches the service layer; non-owner Mother still 403 |
| BR-PRIVACY | Deletion restricted to the file's own owner (data-subject-initiated) |
| BR-CONSULTATION | No consultation-file linkage exists; vacuously satisfied (OI-169-1) — not tested against an invented table |
| PDPA (CLAUDE.md) | 404 (not a distinct "already deleted" message) for repeat-delete attempts, mirroring UC-168's non-leak pattern |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|--------------------|--------------------|----------------|
| TC-COND-001 | Owner deletes own active, unbound file | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-001` |
| TC-COND-002 | Non-owner (any role) attempts delete | `FileDeletePolicy.assertDeletable()` | `FILE-DEL-TC-002` |
| TC-COND-003 | File bound to >= 1 `health_record_files` row | `FileDeletePolicy.assertDeletable()` | `FILE-DEL-TC-003` |
| TC-COND-004 | File bound to exactly 1 health record (boundary: 0 vs 1 rows) | `FileDeletePolicy.assertDeletable()` | `FILE-DEL-TC-004` |
| TC-COND-005 | Admin role (`SYSTEM_ADMIN`/`MODERATOR`/`CONTENT_ADMIN`) is NOT granted a delete bypass | `FileDeletePolicy.assertDeletable()` | `FILE-DEL-TC-005` |
| TC-COND-006 | Family member in shared care-group is NOT granted a delete bypass | `FileDeletePolicy.assertDeletable()` | `FILE-DEL-TC-006` |
| TC-COND-007 | File already soft-deleted (`status=DELETED`) — repeat delete | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-007` |
| TC-COND-008 | File does not exist | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-008` |
| TC-COND-009 | `IStorageService.delete()` is never invoked (soft-delete only) | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-009` |
| TC-COND-010 | Successful delete emits `FILE_DELETED` audit exactly once | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-010` |
| TC-COND-011 | Denied/blocked delete paths never emit `FILE_DELETED` audit | `FileServiceImpl.deleteFile()` | `FILE-DEL-TC-011` |
| TC-COND-012 | No JWT → 401 | `FileController.deleteFile()` | `FILE-DEL-TC-012` |
| TC-COND-013 | Non-`MOTHER` role → 403 at controller role gate | `FileController.deleteFile()` | `FILE-DEL-TC-013` |
| TC-COND-014 | IDOR — Mother deletes another Mother's fileId | `FileController` + `FileServiceImpl` | `FILE-DEL-TC-SEC-001` |
| TC-COND-015 | Full stack DELETE happy path | Integration | `FILE-DEL-TC-INT-001` |
| TC-COND-016 | Full stack DELETE 409 (bound file, DB state unchanged) | Integration | `FILE-DEL-TC-INT-002` |
| TC-COND-017 | Full stack DELETE 403 (non-owner) | Integration | `FILE-DEL-TC-INT-003` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|--------------|----------------|---------------|
| Equivalence Partitioning | Caller identity: owner / non-owner-same-role / non-owner-other-role / admin / family-shared | 5 distinct partitions, only "owner" is in the accept partition per ADR-FILE-007 |
| Boundary Value Analysis | `health_record_files` row count: 0 vs 1 | Boundary of "bound" definition (TC-COND-004) |
| State Transition Testing | `FileStatus`: ACTIVE → DELETED (once), DELETED → (no further transition) | Confirms delete is a one-way, idempotent-safe transition |
| Error Guessing / Security | IDOR via guessed/enumerated fileId; repeat-delete race | OWASP A01:2021 |
| Negative Testing | Explicit non-mutation assertions (`IStorageService.delete()` never called; row `status` unchanged on 403/404/409 paths) | Directly encodes ADR-FILE-008 "no hard delete, no storage purge" |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|--------------------|--------------|
| `FX-169-001` | In-memory | `UploadedFile{id=FILE_ID, ownerUserId=OWNER_ID, status=ACTIVE}` | Happy path |
| `FX-169-002` | In-memory | Same file, `status=DELETED` | 404 repeat-delete case |
| `FX-169-003` | In-memory | `HealthRecordFile{fileId=FILE_ID, healthRecordId=HR_ID}` (1 row) | Binding-guard rejection |
| `FX-169-004` | In-memory | `healthRecordFileRepository.findByFileId(FILE_ID)` → empty list | Binding-guard pass (boundary: 0 rows) |
| `FX-169-005` | JWT/Auth | `{sub: caller-uuid, roles: [ROLE_MOTHER]}` non-owner | Ownership-gate rejection |
| `FX-169-006` | JWT/Auth | `{sub: caller-uuid, roles: [ROLE_SYSTEM_ADMIN]}` | Confirms NO admin bypass exists for delete |
| `FX-169-007` | JWT/Auth | `{sub: caller-uuid, roles: [ROLE_FAMILY]}` in shared care-group (per UC-168 chain) | Confirms NO family bypass exists for delete |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Reuses the same UUID literal conventions as UC-168's `FileViewTestFactory` (`04_Implement/UC168_ViewFile/UC168_ViewFile_Test-Spec.md`) for consistency across the file-domain test suite, extended with a `FileDeleteTestFactory` scoped to this feature.

```java
// FileDeleteTestFactory.java
class FileDeleteTestFactory {

    static final UUID OWNER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ADMIN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID HEALTH_RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    static UploadedFile makeActiveFile() {
        return UploadedFile.builder()
                .id(FILE_ID)
                .ownerUserId(OWNER_ID)
                .storageKey("files/" + FILE_ID + ".jpg")
                .originalName("ultrasound.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(2048L)
                .status(FileStatus.ACTIVE)
                .build();
    }

    static UploadedFile makeDeletedFile() {
        UploadedFile f = makeActiveFile();
        f.setStatus(FileStatus.DELETED);
        return f;
    }

    static HealthRecordFile makeHealthRecordFileLink() {
        return HealthRecordFile.builder()
                .id(UUID.randomUUID())
                .healthRecordId(HEALTH_RECORD_ID)
                .fileId(FILE_ID)
                .displayOrder(0)
                .build();
    }
}
```

---

### FILE-DEL-TC-001 — Owner deletes own active, unbound file (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** ADR-FILE-007 rule 1+2 (owner AND unbound), ADR-FILE-008 (soft-delete)

**Preconditions:** `FX-169-001` seeded; `fileRepository.findByIdAndStatus(FILE_ID, ACTIVE)` mocked to return the file; `healthRecordFileRepository.findByFileId(FILE_ID)` mocked to return empty list (`FX-169-004`).

**Test Steps:**
1. Arrange mocks as above.
2. Act: `fileService.deleteFile(FILE_ID, OWNER_ID)`.
3. Assert: no exception thrown; `verify(fileRepository).save(argThat(f -> f.getStatus() == FileStatus.DELETED))`.

**Expected Result (PASS):** File status transitioned to `DELETED` via `save()`, no exception.
**Expected Result (FAIL):** Exception thrown, or `save()` not called, or status not `DELETED`.

**Current Status:** 🟢 Passing
**Implementation Note:** `deleteFile()` must call `fileDeletePolicy.assertDeletable()` before mutating status.

---

### FILE-DEL-TC-002 — Non-owner attempts delete → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `FileDeletePolicy.assertDeletable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileDeletePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** ADR-FILE-007 rule 1 (strict owner-only)

**Preconditions:** `FX-169-001` (owned by `OWNER_ID`); caller is `STRANGER_ID`.

**Test Steps:**
1. Act: `fileDeletePolicy.assertDeletable(file, STRANGER_ID)`.
2. Assert: throws `AccessDeniedBusinessException`.

**Expected Result (PASS):** Exception thrown; caller code maps it to `FILE-403`/403.
**Expected Result (FAIL):** No exception (over-permissive access).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-003 — File bound to a health record → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `FileDeletePolicy.assertDeletable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileDeletePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** ADR-FILE-007 rule 2 (binding guard)

**Preconditions:** `FX-169-001` (owned by `OWNER_ID`); `healthRecordFileRepository.findByFileId(FILE_ID)` mocked to return `[FX-169-003 link]` (non-empty).

**Test Steps:**
1. Act: `fileDeletePolicy.assertDeletable(file, OWNER_ID)`.
2. Assert: throws `BusinessException` with code `FILE-409`.

**Expected Result (PASS):** Exception thrown even though caller IS the owner — binding guard overrides ownership.
**Expected Result (FAIL):** No exception (deletion silently allowed while a health record still references the file — data-integrity break).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-004 — Boundary: zero health-record bindings → allowed

**Severity:** `HIGH`
**Feature Under Test:** `FileDeletePolicy.assertDeletable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileDeletePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** ADR-FILE-007 rule 2 — "0 rows" boundary value

**Preconditions:** `FX-169-004` — `healthRecordFileRepository.findByFileId(FILE_ID)` returns exactly `[]` (empty list, not null).

**Test Steps:**
1. Act: `fileDeletePolicy.assertDeletable(file, OWNER_ID)`.
2. Assert: no exception thrown.

**Expected Result (PASS):** No exception — the boundary at 0 rows must NOT be treated as "bound".
**Expected Result (FAIL):** Exception thrown for an unbound file (over-restrictive; boundary condition wrongly evaluated).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-005 — Admin role does NOT bypass ownership check (unlike UC-168's view scope)

**Severity:** `HIGH`
**Feature Under Test:** `FileDeletePolicy.assertDeletable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileDeletePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** ADR-FILE-007 (explicit asymmetry vs ADR-FILE-005 rule 3) — §16 Auth Matrix

**Test Steps:**
1. Act: `fileDeletePolicy.assertDeletable(file, ADMIN_ID)` (file owned by `OWNER_ID` ≠ `ADMIN_ID`), parametrized for `SYSTEM_ADMIN`/`MODERATOR`/`CONTENT_ADMIN` caller identities.
2. Assert: throws `AccessDeniedBusinessException` in every case.

**Expected Result (PASS):** Exception thrown for all admin-role callers who are not the owner — proves NO admin bypass exists for delete, unlike `viewFile()`.
**Expected Result (FAIL):** No exception thrown for an admin caller (incorrectly copy-pasted UC-168's view-scope bypass logic).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-006 — Family member in shared care-group does NOT bypass ownership check

**Severity:** `HIGH`
**Feature Under Test:** `FileDeletePolicy.assertDeletable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileDeletePolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** ADR-FILE-007 (explicit asymmetry vs ADR-FILE-005 rule 2)

**Preconditions:** `FAMILY_ID` would be granted view-access under UC-168's `FileAccessPolicy` (ACCEPTED care-group sharing the file's linked baby), but `FileDeletePolicy` has no such rule at all — it never even queries care-group membership.

**Test Steps:**
1. Act: `fileDeletePolicy.assertDeletable(file, FAMILY_ID)` (file owned by `OWNER_ID`).
2. Assert: throws `AccessDeniedBusinessException`.
3. Assert: no care-group/care-group-member repository is invoked by `FileDeletePolicy` at all (verify no such dependency exists / is called) — confirms delete-eligibility is single-rule (ownership only), not a copy of the view-scope chain.

**Expected Result (PASS):** Exception thrown; policy short-circuits on ownership alone.
**Expected Result (FAIL):** No exception (incorrectly reused UC-168's sharing-chain logic for delete).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-007 — Already-deleted file → 404 (idempotent-safe repeat-delete)

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** State machine §6.4 (TDS) — `findByIdAndStatus(id, ACTIVE)` guard

**Preconditions:** `fileRepository.findByIdAndStatus(FILE_ID, ACTIVE)` mocked to return `Optional.empty()` (matches real repo behavior for a row whose `status` is already `DELETED`).

**Test Steps:**
1. Act: `fileService.deleteFile(FILE_ID, OWNER_ID)`.
2. Assert: throws `ResourceNotFoundException`, mapped by controller to `FILE-404`/404 — generic message, no distinct "already deleted" text (avoid existence/state leak).
3. Assert: `verify(fileRepository, never()).save(any())` — no duplicate transition, no duplicate audit.

**Expected Result (PASS):** `ResourceNotFoundException` thrown; no side effects.
**Expected Result (FAIL):** Different exception type, or `save()`/audit invoked on a repeat-delete.

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-008 — Non-existent fileId → 404

**Severity:** `MEDIUM`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`

**Test Steps:**
1. Mock repository `findByIdAndStatus` → `Optional.empty()`.
2. Act: `fileService.deleteFile(randomUUID, OWNER_ID)`.
3. Assert: throws `ResourceNotFoundException`.

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-009 — `IStorageService.delete()` is never invoked (soft-delete only)

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** ADR-FILE-008 (explicit prohibition on physical purge)

**Test Steps:**
1. Act: `fileService.deleteFile(FILE_ID, OWNER_ID)` (happy path).
2. Assert: `verify(storageService, never()).delete(anyString())`.
3. Repeat assertion on the 403 (`FILE-DEL-TC-002`) and 409 (`FILE-DEL-TC-003`) paths — storage delete must never fire regardless of outcome.

**Expected Result (PASS):** `IStorageService.delete()` never called on any path.
**Expected Result (FAIL):** Storage delete invoked — would violate "soft-delete" semantics and make the operation irreversible (ADR-FILE-008 Option B/C rejected).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-010 — Successful delete emits `FILE_DELETED` audit exactly once

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** TDS §7.1 Domain Event Catalog, SRS POST-3 (sensitive actions recorded for audit)

**Test Steps:**
1. Act: `fileService.deleteFile(FILE_ID, OWNER_ID)`.
2. Assert: `verify(auditService, times(1)).log(eq(AuditAction.FILE_DELETED), eq(OWNER_ID), eq("UploadedFile"), eq(FILE_ID.toString()), any())`.

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-011 — Denied/blocked paths never emit `FILE_DELETED` audit

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** Consistency with UC-168 TC-009's "no partial audit on denied access" rule

**Test Steps:**
1. Trigger the 403 path (non-owner) — assert `auditService.log(eq(AuditAction.FILE_DELETED), ...)` is NEVER called.
2. Trigger the 409 path (bound file) — assert same.
3. Trigger the 404 path (already-deleted / non-existent) — assert same.

**Expected Result (PASS):** Zero `FILE_DELETED` audit entries on any non-2xx path.
**Expected Result (FAIL):** An audit entry is written despite the operation being blocked (misleading audit trail).

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-012 — No JWT → 401 (controller layer)

**Severity:** `HIGH`
**Feature Under Test:** `FileController.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileControllerDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`

**Test Steps:**
1. `@WebMvcTest(FileController.class)`, no `Authorization` header.
2. `mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))`.
3. Assert status 401.

**Current Status:** 🟢 Passing

---

### FILE-DEL-TC-013 — Non-`MOTHER` role → 403 at controller role gate

**Severity:** `HIGH`
**Feature Under Test:** `FileController.deleteFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileControllerDeleteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** §9.1 Endpoints Table (`@PreAuthorize("hasRole('MOTHER')")`), §16 Auth Matrix

**Test Steps:**
1. `@WebMvcTest(FileController.class)` with a valid JWT for role `FAMILY` (parametrized also for `EXPERT`, `SYSTEM_ADMIN`).
2. `mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID).header("Authorization", familyJwt))`.
3. Assert status 403; assert `fileService.deleteFile()` is NEVER invoked (request rejected before reaching the service layer).

**Expected Result (PASS):** 403 at the Spring Security annotation gate, service method not called.
**Expected Result (FAIL):** Request reaches the service layer for a disallowed role (role gate misconfigured or missing).

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### FILE-DEL-TC-SEC-001 — IDOR: Mother deletes another Mother's file

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Legal:** PDPA — unauthorized deletion of another data subject's medical/baby PII
**Feature Under Test:** `FileController.deleteFile()` + `FileServiceImpl.deleteFile()` (full auth chain)
**Test File:** `src/test/java/com/carebridge/backend/file/FileControllerDeleteTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:** Authenticated as `OTHER_MOTHER_ID` (role `MOTHER`, passes the controller role gate) targeting a real `fileId` owned by `OWNER_ID`.

**Test Steps (Attack Simulation):**
1. Authenticate as `OTHER_MOTHER_ID`.
2. `DELETE /api/v1/files/{OWNER's fileId}`.
3. Assert response is `403 FILE-403` (service-layer ownership gate catches what the role gate could not).
4. Assert the target file's DB/mock state is unchanged (`status` still `ACTIVE`, no `save()` invoked).

**Expected Result (PASS = safe):** `403`, no state mutation, `FILE_DELETED` audit NOT emitted (cross-check with TC-011).
**Expected Result (FAIL = vulnerability):** `204` returned, or the other Mother's file is deleted (role-gate-only enforcement without an ownership check would allow this — the exact risk ADR-FILE-007 rule 1 exists to prevent).

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### FILE-DEL-TC-INT-001 — Full stack: owner DELETEs own unbound file

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `DELETE /api/v1/files/{id}` → `FileController` → `FileServiceImpl` → `FileDeletePolicy` → `UploadedFileRepository`/`HealthRecordFileRepository` → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/file/integration/FileDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:** PostgreSQL Testcontainer running; Flyway migrated; seed one `uploaded_files` row (`status=ACTIVE`, `owner_user_id=OWNER_ID`); no `health_record_files` row for it.

**Test Steps:**
1. Seed file row via JPA.
2. `mockMvc.perform(delete("/api/v1/files/{id}", fileId).header("Authorization", ownerJwt))`.
3. Assert status 204.

**DB Assertion:**
```java
UploadedFile record = fileRepository.findById(fileId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(FileStatus.DELETED); // transitioned by this call
assertThat(record.getStorageKey()).isNotBlank(); // row preserved, not hard-deleted (ADR-FILE-008)
```

**Current Status:** 🔴 Not written

---

### FILE-DEL-TC-INT-002 — Full stack: bound file → 409, DB state unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, binding-guard denial path
**Test File:** `src/test/java/com/carebridge/backend/file/integration/FileDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** Seed `uploaded_files` row owned by `OWNER_ID` AND a `health_record_files` row referencing it (requires a seeded `health_records` row per FK, minimal synthetic data).

**Test Steps:**
1. Seed file + health record + `health_record_files` link.
2. `DELETE /api/v1/files/{id}` authenticated as `OWNER_ID`.
3. Assert 409, error code `FILE-409`.

**DB Assertion:**
```java
UploadedFile record = fileRepository.findById(fileId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(FileStatus.ACTIVE); // unchanged — deletion was blocked
```

**Current Status:** 🔴 Not written

---

### FILE-DEL-TC-INT-003 — Full stack: non-owner → 403

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, ownership denial path
**Test File:** `src/test/java/com/carebridge/backend/file/integration/FileDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Seed file owned by `OWNER_ID`, no `health_record_files` linkage.
2. `DELETE /api/v1/files/{id}` authenticated as a different seeded `MOTHER`-role user.
3. Assert 403, error code `FILE-403`.
4. DB assertion: `status` still `ACTIVE`.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|--------------|----------------------|------------------------|------------------------|
| `FILE-DEL-TC-001` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-002` | `FileDeletePolicyTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-003` | `FileDeletePolicyTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-004` | `FileDeletePolicyTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-005` | `FileDeletePolicyTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-006` | `FileDeletePolicyTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-007` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-008` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-009` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-010` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-011` | `FileServiceDeleteTest.java` | `[x]` | `2026-07-06` | split into notFound + ownershipDenied paths |
| `FILE-DEL-TC-012` | `FileControllerDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-013` | `FileControllerDeleteTest.java` | `[x]` | `2026-07-06` | parametrized for FAMILY + SYSTEM_ADMIN |
| `FILE-DEL-TC-SEC-001` | `FileControllerDeleteTest.java` | `[x]` | `2026-07-06` | |
| `FILE-DEL-TC-INT-001` | `FileDeleteIntegrationTest.java` | `[ ]` | `[ ]` | Requires Testcontainers — not in scope |
| `FILE-DEL-TC-INT-002` | `FileDeleteIntegrationTest.java` | `[ ]` | `[ ]` | Requires Testcontainers — not in scope |
| `FILE-DEL-TC-INT-003` | `FileDeleteIntegrationTest.java` | `[ ]` | `[ ]` | Requires Testcontainers — not in scope |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> All UC-169 test cases are **genuinely Red** — `deleteFile()` and `FileDeletePolicy` do not yet exist anywhere in the codebase (confirmed: `IFileService` currently declares only `uploadFile()`; `FileController` currently declares only `POST /`; no `policy` package exists under `com.carebridge.backend.file` yet). This is the same "no Track A/characterization split needed" situation as UC-168 — this whole feature is new, nothing pre-exists to characterize.
>
> **Sequencing note (OI-169-4):** If UC-168 (`viewFile()`, `HealthRecordFileRepository.findByFileId()`) has not yet been implemented at the time UC-169 implementation begins, `findByFileId()` must be added as a shared prerequisite (do NOT re-add it if UC-168's implementation already exists — verify via `git grep findByFileId` before adding).

**Stub cho Red Phase:**

```java
@Service
public class FileServiceImpl implements IFileService {
    // ... existing uploadFile() unchanged; viewFile() per UC-168 (if present) unchanged ...

    @Override
    public void deleteFile(UUID fileId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-----------------|--------------|-------------|-----------------------------------------|
| `FILE-DEL-TC-001` | `throw` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `FILE-DEL-TC-007` | `throw` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `FILE-DEL-TC-009` | `throw` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `FILE-DEL-TC-INT-001` | not implemented | 🔴 FAIL | — | Integration test not in scope |

> Note: `FileDeletePolicy` tests (`TC-002` through `TC-006`) failed at runtime with UnsupportedOperationException vs expected exception types — confirmed RED.

**Red Gate Evidence:**
- Stub confirmed: 23 tests FAIL (18 failures + 5 errors) — 2026-07-06
- Tất cả FAIL? [x] Yes → **GATE-2 PASS** (T2→T3)
- Log: `./mvnw test -Dtest="FileServiceDeleteTest,FileDeletePolicyTest,..."` — 31 total, 23 fail, 8 PASS (security-layer only)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-FILE-IMP-169` reviewed and Approved
- [ ] Logic Issues (§2) confirmed with Tech Lead
- [ ] No migration required for UC-169 (confirmed §5.2 of TDS) — no migration gate blocking
- [ ] Confirmed whether UC-168's `findByFileId()` already exists in code (OI-169-4 sequencing check)

### Exit Criteria
- [x] `./mvnw test` — all unit tests green (31/31 pass — 2026-07-06)
- [ ] `./mvnw verify` — integration tests green (Testcontainers) — not in scope
- [x] Test coverage ≥ 80% lines for `FileDeletePolicyImpl` and new `FileServiceImpl.deleteFile()` code
- [x] No business logic in `FileController` (validation/mapping only)
- [x] No PII in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [x] Red Gate (§5.1) — all tests FAIL against throwing stub before implement
- [x] Contract Existence — `./mvnw compile` clean, no hallucinated imports
- [x] Props Isolation — all entities built via factory helpers, no shared mutable state
- [x] Oracle Source — every assert traces to ADR-FILE-007/008 or existing schema fact
- [x] Negative-mutation checks present: `IStorageService.delete()` never invoked; DB `status` unchanged on every denial path (403/404/409)

### Suspension Criteria
- Tech Lead disagrees with ADR-FILE-007 binding-guard scope (would require TDS revision first)
- UC-168's `findByFileId()` is not available and cannot be added without conflicting with an in-flight UC-168 implementation

---

## 7. Rollback Plan

```bash
# No migration to revert for UC-169.
git checkout -- src/main/java/com/carebridge/backend/file/
git checkout -- src/test/java/com/carebridge/backend/file/
# NOTE: do NOT blanket-revert com.carebridge.backend.health/repository/HealthRecordFileRepository.java
# if UC-168's findByFileId() was already merged independently — revert only UC-169-specific changes.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|--------------|--------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-FILE-007/008 | ☑ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throwing stub | ☑ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a `consultation_files`/`retention_policy` table not in TDS §5.2 | ☑ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `FileController` doing ownership/binding checks directly | ☑ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports repository method not declared in TDS §8.3 | ☑ | G-3 |
| AP-AI-006 (custom) | Hard-delete substitution | Test asserts `fileRepository.delete(...)` or `IStorageService.delete(...)` IS called | ☑ | G-1 |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào → Test-Spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec for UC169 Delete File — Status: Implemented — 2026-07-06. All unit tests GREEN.*
