# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC168 View File

**Document ID:** `CB-FILE-TDD-168`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
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
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.10.2
- `04_Implement/UC168_ViewFile/UC168_ViewFile_TDS.md` (`CB-FILE-IMP-168`)

> **Quy ước TDD:** Test-first. Thứ tự: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data (UUID literals, fake filenames).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|----------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC168 View File |

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
| **Feature / Gap ID** | `UC-168` |
| **Module** | File Management — View File |
| **Spec gốc** | `CB-FILE-IMP-168` |
| **Priority** | 🔴 P0 (High per SRS) |
| **Sprint** | Sprint 2 — Baby Logs, Health Records, Reminders, Files |
| **Milestone** | M3 Alpha |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | PDPA, BR-RBAC |
| **Upstream Dependencies** | `UploadedFileRepository`, `HealthRecordFileRepository`, `IStorageService`, `care_group_members` schema |
| **Downstream Consumers** | Mobile App file preview screens |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-FILE-IMP-168 §17`, ADR-FILE-005, ADR-FILE-006 |
| **Constraints Injected** | C1-C5 per TDS §17.1 |
| **Model** | Claude Sonnet 5 |
| **Trust Level** | T2 → T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|---------------------------|-------------------------------|------------------------------|
| L1 | SRS §3.3.10.2 says "within valid access scope and sharing period" but no `expires_at`/ACL column exists on `uploaded_files` (confirmed reading `V20260627100000__create_uploaded_files.sql`) | Presigned URL TTL (15 min, `IStorageService`) is the only existing "period" concept (ADR-FILE-004/006) | Tests assert `generatePresignedUrl(key, 15)` is called with `15` exactly — never assert a persisted expiry field |
| L2 | "Access scope" is undefined in SRS beyond BR-RBAC | Resolved via ADR-FILE-005: owner OR care-group-sharing-chain OR admin role | Tests seed `health_record_files` + `care_group_members` chain explicitly per scenario, and assert 403 when chain is absent |
| L3 | SRS Primary Actor = generic "User" (not role-restricted) | `FileController.viewFile()` therefore uses no `@PreAuthorize` role restriction (any authenticated principal); scope is enforced in `FileServiceImpl`/`FileAccessPolicy`, not at the controller annotation level | Controller-layer tests assert 401 for missing JWT but NOT 403-by-role (role is not the gate — ownership/sharing is) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
File Management (View File, UC-168) bao gồm các layer:
├── Domain (FileAccessPolicy — pure logic, mock repositories với Mockito)
├── Services (FileServiceImpl.viewFile() — mock JPA Repository + IStorageService + AuditService với Mockito)
├── Controller (FileController.viewFile() — mock IFileService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, full stack GET /api/v1/files/{id})
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|------------------|
| SRS UC-168 | Access scope check, sharing-period presigned URL, preview/download response shape |
| ADR-FILE-005 | Owner / care-group-sharing-chain / admin access rules |
| ADR-FILE-006 | 15-minute presigned URL reuse as "sharing period" |
| BR-RBAC | Authenticated-but-out-of-scope caller → 403, not 401 |
| PDPA (CLAUDE.md) | 404 (not a distinct "deleted" message) for soft-deleted files to avoid existence leak |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|--------------------|--------------------|----------------|
| TC-COND-001 | Owner views own active file | `FileServiceImpl.viewFile()` | `FILE-VIEW-TC-001` |
| TC-COND-002 | Non-owner, no sharing chain | `FileAccessPolicy.assertViewable()` | `FILE-VIEW-TC-002` |
| TC-COND-003 | Family member in ACCEPTED care-group sharing the linked baby | `FileAccessPolicy.assertViewable()` | `FILE-VIEW-TC-003` |
| TC-COND-004 | Family member with PENDING (not ACCEPTED) invitation | `FileAccessPolicy.assertViewable()` | `FILE-VIEW-TC-004` |
| TC-COND-005 | Admin role (`SYSTEM_ADMIN`/`MODERATOR`/`CONTENT_ADMIN`) bypasses scope | `FileAccessPolicy.assertViewable()` | `FILE-VIEW-TC-005` |
| TC-COND-006 | File soft-deleted (`status=DELETED`) | `FileServiceImpl.viewFile()` | `FILE-VIEW-TC-006` |
| TC-COND-007 | File does not exist | `FileServiceImpl.viewFile()` | `FILE-VIEW-TC-007` |
| TC-COND-008 | Presigned URL TTL always 15 min | `FileServiceImpl.viewFile()` | `FILE-VIEW-TC-008` |
| TC-COND-009 | Successful view emits `FILE_VIEWED` audit | `FileServiceImpl.viewFile()` | `FILE-VIEW-TC-009` |
| TC-COND-010 | No JWT → 401 | `FileController.viewFile()` | `FILE-VIEW-TC-010` |
| TC-COND-011 | IDOR — enumerate other users' fileIds | `FileController` + `FileServiceImpl` | `FILE-VIEW-TC-SEC-001` |
| TC-COND-012 | Full stack GET happy path | Integration | `FILE-VIEW-TC-INT-001` |
| TC-COND-013 | Full stack GET 403 (non-owner) | Integration | `FILE-VIEW-TC-INT-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|--------------|----------------|---------------|
| Equivalence Partitioning | Access scope: owner / shared / not-shared / admin | 4 distinct partitions per ADR-FILE-005 |
| Boundary Value Analysis | Care-group `invitation_status`: ACCEPTED vs PENDING vs REVOKED | Boundary of "sharing" definition |
| State Transition Testing | `FileStatus`: ACTIVE vs DELETED | Confirms view never bypasses soft-delete guard |
| Error Guessing / Security | IDOR via sequential/guessed fileId | OWASP A01:2021 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|--------------------|--------------|
| `FX-168-001` | In-memory | `UploadedFile{id=FILE_ID, ownerUserId=OWNER_ID, status=ACTIVE}` | Happy path |
| `FX-168-002` | In-memory | Same file, `status=DELETED` | 404 case |
| `FX-168-003` | In-memory | `HealthRecordFile{fileId=FILE_ID, healthRecordId=HR_ID}` + `HealthRecord{id=HR_ID, babyId=BABY_ID}` + `CareGroup{babyId=BABY_ID}` + `CareGroupMember{userId=FAMILY_ID, invitationStatus=ACCEPTED}` | Sharing-chain happy path |
| `FX-168-004` | In-memory | Same as FX-168-003 but `invitationStatus=PENDING` | Sharing-chain rejection |
| `FX-168-005` | JWT/Auth | `{sub: caller-uuid, roles: [ROLE_SYSTEM_ADMIN]}` | Admin bypass |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// FileViewTestFactory.java
class FileViewTestFactory {

    static final UUID OWNER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ADMIN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID HEALTH_RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID BABY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000030");
    static final UUID CARE_GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

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

### FILE-VIEW-TC-001 — Owner views own active file (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceViewTest.java`
**TDD Phase:** 🔴 RED — genuinely new method
**Condition Ref:** `TC-COND-001`
**Oracle Source:** ADR-FILE-005 rule 1 (owner)

**Preconditions:** `FX-168-001` seeded; `fileRepository.findByIdAndStatus(FILE_ID, ACTIVE)` mocked to return the file.

**Test Steps:**
1. Arrange mocks: repository returns active file owned by `OWNER_ID`; storage returns presigned URL.
2. Act: `fileService.viewFile(FILE_ID, OWNER_ID)`.
3. Assert: response `fileId`, `originalName`, `mimeType`, `presignedUrl` populated; no exception thrown.

**Expected Result (PASS):** `ViewFileResponse` returned with all fields matching the file entity.
**Expected Result (FAIL):** Exception thrown, or wrong fields.

**Current Status:** 🔴 Not written
**Implementation Note:** `viewFile()` must call `fileAccessPolicy.assertViewable()` before generating the presigned URL.

---

### FILE-VIEW-TC-002 — Non-owner, no sharing chain → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `FileAccessPolicy.assertViewable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** ADR-FILE-005 (default-deny when no rule matches)

**Preconditions:** `FX-168-001`; `healthRecordFileRepository.findByFileId(FILE_ID)` returns empty list (no linkage at all).

**Test Steps:**
1. Act: `fileAccessPolicy.assertViewable(file, STRANGER_ID, List.of("ROLE_MOTHER"))`.
2. Assert: throws `AccessDeniedBusinessException`.

**Expected Result (PASS):** Exception thrown with message referencing access denial; caller code maps it to `FILE-403`/403.
**Expected Result (FAIL):** No exception (over-permissive access).

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-003 — Family member in ACCEPTED care-group sharing linked baby → allowed

**Severity:** `HIGH`
**Feature Under Test:** `FileAccessPolicy.assertViewable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** ADR-FILE-005 rule 2

**Preconditions:** `FX-168-003` fully seeded (health_record_files → health_records.baby_id → care_groups.baby_id → care_group_members ACCEPTED for `FAMILY_ID`).

**Test Steps:**
1. Mock chain: `findByFileId` → `[link]`; health record lookup → `baby_id=BABY_ID`; care group lookup by baby → `[group]`; care-group-member lookup → `ACCEPTED` for `FAMILY_ID`.
2. Act: `assertViewable(file, FAMILY_ID, List.of("ROLE_FAMILY"))`.
3. Assert: no exception thrown.

**Expected Result (PASS):** No exception.
**Expected Result (FAIL):** Exception thrown for a legitimately shared family member (over-restrictive).

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-004 — Family member with PENDING invitation → 403 (boundary)

**Severity:** `HIGH`
**Feature Under Test:** `FileAccessPolicy.assertViewable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** ADR-FILE-005 rule 2 — "ACCEPTED" boundary value

**Preconditions:** `FX-168-004` — same chain as TC-003 but `invitation_status=PENDING`.

**Test Steps:**
1. Mock chain identical to TC-003 except membership status `PENDING`.
2. Act: `assertViewable(file, FAMILY_ID, List.of("ROLE_FAMILY"))`.
3. Assert: throws `AccessDeniedBusinessException`.

**Expected Result (PASS):** Exception thrown — an unaccepted invite must NOT grant access.
**Expected Result (FAIL):** No exception (boundary condition wrongly treated as accepted).

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-005 — Admin role bypasses scope check

**Severity:** `MEDIUM`
**Feature Under Test:** `FileAccessPolicy.assertViewable()`
**Test File:** `src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** ADR-FILE-005 rule 3

**Test Steps:**
1. Act: `assertViewable(file, ADMIN_ID, List.of("ROLE_SYSTEM_ADMIN"))` with no health-record linkage seeded at all.
2. Assert: no exception thrown (admin oversight bypasses owner/sharing check).

**Expected Result (PASS):** No exception for `ROLE_SYSTEM_ADMIN`, `ROLE_MODERATOR`, `ROLE_CONTENT_ADMIN` (parametrized).
**Expected Result (FAIL):** Exception thrown for admin roles.

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-006 — Soft-deleted file → 404 (not "file deleted" message)

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceViewTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** State machine §6.3 (TDS) — `findByIdAndStatus(id, ACTIVE)` guard

**Preconditions:** `fileRepository.findByIdAndStatus(FILE_ID, ACTIVE)` mocked to return `Optional.empty()` (matches real repo behavior for a DELETED-status row).

**Test Steps:**
1. Act: `fileService.viewFile(FILE_ID, OWNER_ID)`.
2. Assert: throws `ResourceNotFoundException`, mapped by controller to `FILE-404`/404 — message must be generic ("File not found"), not "File was deleted" (avoid existence leak to non-owners).

**Expected Result (PASS):** `ResourceNotFoundException` thrown.
**Expected Result (FAIL):** Different exception type, or a message revealing deletion status to a non-owner caller.

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-007 — Non-existent fileId → 404

**Severity:** `MEDIUM`
**Feature Under Test:** `FileServiceImpl.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceViewTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Mock repository `findByIdAndStatus` → `Optional.empty()`.
2. Act: `fileService.viewFile(randomUUID, OWNER_ID)`.
3. Assert: throws `ResourceNotFoundException`.

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-008 — Presigned URL TTL always 15 minutes

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceViewTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** ADR-FILE-006 / ADR-FILE-004 (existing "PDPA: max 15" contract in `IStorageService` javadoc)

**Test Steps:**
1. Act: `fileService.viewFile(FILE_ID, OWNER_ID)`.
2. Assert: `verify(storageService).generatePresignedUrl(eq(storageKey), eq(15))`.

**Expected Result (PASS):** Exactly `15` passed, never a caller-controlled or longer value.
**Expected Result (FAIL):** Any other TTL value, or TTL sourced from request input (would violate PDPA cap).

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-009 — Successful view emits FILE_VIEWED audit exactly once

**Severity:** `HIGH`
**Feature Under Test:** `FileServiceImpl.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileServiceViewTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** TDS §7.1 Domain Event Catalog, SRS POST-3 (sensitive actions recorded for audit)

**Test Steps:**
1. Act: `fileService.viewFile(FILE_ID, OWNER_ID)`.
2. Assert: `verify(auditService, times(1)).log(eq(AuditAction.FILE_VIEWED), eq(OWNER_ID), eq("UploadedFile"), eq(FILE_ID.toString()), any())`.
3. Assert: on the 403 path (TC-002), `auditService.log(FILE_VIEWED, ...)` is NEVER called (no partial audit on denied access).

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-010 — No JWT → 401 (controller layer)

**Severity:** `HIGH`
**Feature Under Test:** `FileController.viewFile()`
**Test File:** `src/test/java/com/carebridge/backend/file/FileControllerViewTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. `@WebMvcTest(FileController.class)`, no `Authorization` header.
2. `mockMvc.perform(get("/api/v1/files/{id}", FILE_ID))`.
3. Assert status 401.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### FILE-VIEW-TC-SEC-001 — IDOR: sequential/guessed fileId enumeration

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Legal:** PDPA — unauthorized access to medical/baby PII
**Feature Under Test:** `FileController.viewFile()` + `FileServiceImpl.viewFile()` (full auth chain)
**Test File:** `src/test/java/com/carebridge/backend/file/FileControllerViewTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Authenticated as `STRANGER_ID` (no ownership, no sharing chain) targeting a real `fileId` owned by `OWNER_ID`.

**Test Steps (Attack Simulation):**
1. Authenticate as `STRANGER_ID`.
2. `GET /api/v1/files/{OWNER's fileId}`.
3. Assert response is `403 FILE-403`, and crucially the response body does NOT include `originalName`/`presignedUrl`/any file metadata (no partial data leak on denied path).

**Expected Result (PASS = safe):** `403`, no metadata in body, `FILE_VIEWED` audit NOT emitted (TC-009 cross-check).
**Expected Result (FAIL = vulnerability):** `200` returned, or file metadata present in a 403 error body.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FILE-VIEW-TC-INT-001 — Full stack: owner GETs own file

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `GET /api/v1/files/{id}` → `FileController` → `FileServiceImpl` → `FileAccessPolicy` → `UploadedFileRepository` → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/file/integration/FileViewIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** PostgreSQL Testcontainer running; Flyway migrated; seed one `uploaded_files` row (`status=ACTIVE`, `owner_user_id=OWNER_ID`).

**Test Steps:**
1. Seed file row via JPA.
2. `mockMvc.perform(get("/api/v1/files/{id}", fileId).header("Authorization", ownerJwt))`.
3. Assert status 200; response body `data.fileId` matches; `data.presignedUrl` not blank.

**Expected Result (PASS):** 200, correct payload shape per §9.2 of TDS.

**DB Assertion:**
```java
UploadedFile record = fileRepository.findById(fileId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(FileStatus.ACTIVE); // unchanged by view
```

**Current Status:** 🔴 Not written

---

### FILE-VIEW-TC-INT-002 — Full stack: non-owner without sharing chain → 403

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, denial path
**Test File:** `src/test/java/com/carebridge/backend/file/integration/FileViewIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Test Steps:**
1. Seed file owned by `OWNER_ID`, no `health_record_files` linkage.
2. `GET /api/v1/files/{id}` authenticated as a different seeded user.
3. Assert 403, error code `FILE-403`.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|--------------|----------------------|------------------------|------------------------|
| `FILE-VIEW-TC-001` | `FileServiceViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-002` | `FileAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-003` | `FileAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-004` | `FileAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-005` | `FileAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-006` | `FileServiceViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-007` | `FileServiceViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-008` | `FileServiceViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-009` | `FileServiceViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-010` | `FileControllerViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-SEC-001` | `FileControllerViewTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-INT-001` | `FileViewIntegrationTest.java` | `[ ]` | `[ ]` | |
| `FILE-VIEW-TC-INT-002` | `FileViewIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> All UC-168 test cases are **genuinely Red** — `viewFile()`, `ViewFileResponse`, `FileAccessPolicy` do not yet exist anywhere in the codebase (confirmed: `IFileService` currently declares only `uploadFile()`; `FileController` currently declares only `POST /`). There is no "Track A / characterization" split needed here (unlike UC117) because nothing in this endpoint's code path pre-exists — this whole feature is new.

**Stub cho Red Phase:**

```java
@Service
public class FileServiceImpl implements IFileService {
    // ... existing uploadFile() unchanged ...

    @Override
    public ViewFileResponse viewFile(UUID fileId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-----------------|--------------|-------------|-----------------------------------------|
| `FILE-VIEW-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FILE-VIEW-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FILE-VIEW-TC-008` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FILE-VIEW-TC-INT-001` | `throw` (via controller 500) | 🔴 FAIL | ☐ FAIL ☐ PASS | |

> Note: `FileAccessPolicy` tests (`TC-002` through `TC-005`) fail at **compile time** initially since the interface/impl do not exist yet — this is an acceptable/expected Red Gate signal (class does not exist), to be confirmed once the interface skeleton (throwing stub) is added.

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled during implementation)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3)
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-FILE-IMP-168` reviewed and Approved
- [ ] Logic Issues (§2) confirmed with Tech Lead
- [ ] No migration required for UC-168 (confirmed §5.2 of TDS) — no migration gate blocking

### Exit Criteria
- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [ ] Test coverage ≥ 80% lines for `FileAccessPolicyImpl` and new `FileServiceImpl.viewFile()` code
- [ ] No business logic in `FileController` (validation/mapping only)
- [ ] No PII in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — all tests FAIL against throwing stub before implement
- [ ] Contract Existence — `./mvnw compile` clean, no hallucinated imports
- [ ] Props Isolation — all entities built via `FileViewTestFactory`, no shared mutable state
- [ ] Oracle Source — every assert traces to ADR-FILE-005/006 or existing schema fact

### Suspension Criteria
- Tech Lead disagrees with ADR-FILE-005 access-scope model (would require TDS revision first)

---

## 7. Rollback Plan

```bash
# No migration to revert for UC-168.
git checkout -- src/main/java/com/carebridge/backend/file/
git checkout -- src/main/java/com/carebridge/backend/health/repository/HealthRecordFileRepository.java
git checkout -- src/test/java/com/carebridge/backend/file/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|--------------|--------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-FILE-005/006 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throwing stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a `file_shares` table not in TDS §5.2 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `FileController` doing scope checks directly | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports repository method not declared in TDS §8.3 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → Test-Spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec for UC168 View File — Status: Draft. Awaiting review before Approved.*
