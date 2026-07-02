# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-72 — Manage Family Permission — Test Specification

**Document ID:** `CB-FAMILY-TDD-072`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (line 747: `care_group_members.permission_json jsonb`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V2__spec_sync_from_tds.sql` — `device_tokens` table (lines 100-120)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.49 (lines 2788-2807) — Functional requirements
- `04_Implement/UC72_ManageFamilyPermission/UC72_ManageFamilyPermission_TDS.md` (CB-FAMILY-IMP-072) — Technical Specification
- ADR-FAM-002, ADR-FAM-020, ADR-FAM-021, ADR-FAM-022, ADR-FAM-023 (TDS §3) — Architecture Decision Records
- N/A — no VN legal article citation available for this feature beyond PDPA data-minimization principle (BR-PRIVACY); do not invent a specific Điều/Luật citation

> **Quy ước TDD:** This document describes test cases BEFORE production code is written.
> Required order: write test (`.java`) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test ✅ until `./mvnw test` is green.
> No real PII in test data — SYNTHETIC only.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Initial draft — TDD spec for UC-72 Manage Family Permission |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-UC72` |
| **Module** | Family Sync — Care Group Permission Management (Bounded Context: `family`) |
| **Spec gốc** | `CB-FAMILY-IMP-072` (UC72_ManageFamilyPermission_TDS.md) |
| **Priority** | 🔴 P0 (SRS Priority: High) |
| **Sprint** | `S3 Cross-Domain Integration` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` (family-data access-control flags) |
| **Compliance Scope** | `PDPA` (data minimization / BR-PRIVACY); `N/A` for GDPR/CCPA |
| **Upstream Dependencies** | `CareGroup`, `CareGroupMember` entities (UC-70); `FcmService`, `DeviceTokenRepository` (existing notification module); `AuditService` |
| **Downstream Consumers** | Future UC-3.3.1.51 View Shared Care Calendar, UC-3.3.3.2 View Shared Data, UC-3.3.3.4 View Family Alert (NOT implemented/tested here — enforcement of these flags in other features is out of scope) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAMILY-IMP-072 §17.1` (C1-C7), ADR-FAM-002/020/021/022/023 |
| **Constraints Injected** | OWNER-only mutation (C1, Open); ACCEPTED-only target (C2, Open); 4-boolean permission shape (C3, Open); caller identity from `SecurityUtils` only (C4); layer separation (C5); FCM failure non-blocking (C6); no new migration (C7) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-72 gives no field-level spec for the permission model | `care_group_members.permission_json` is jsonb, currently unused; TDS ADR-FAM-020 (Open) proposes 4 booleans: calendar/logs/alerts/records | Tests assert exactly these 4 keys are read/written; unknown keys rejected (FAM-022) |
| L2 | SRS "required role or permission" is generic — does not name OWNER explicitly | Existing code pattern (UC-70) makes the group creator the sole `OWNER`; ADR-FAM-021 (Open) proposes OWNER-only management | Tests include an explicit non-owner-403 authorization test case (FAM72-TC-006) |
| L3 | SRS does not state target-member eligibility status | `listMembers()` code evidence (informal "ADR-FAM-002") already uses `InviteStatus.ACCEPTED` as the sole membership bar; ADR-FAM-023 (Open) extends this to UC-72 | Tests include a PENDING-member-target rejection case (FAM72-TC-007) and a REVOKED-member case (FAM72-TC-008) |
| L4 | Generic SRS Exception E3 ("external/network/server failure handled with retry guidance, no duplicate unsafe action") does not specify FCM-specific behavior | ADR-FAM-022 defines FCM failure as non-blocking / does not roll back the DB write | Test FAM72-TC-010 asserts DB write persists and 200 is returned even when `FcmService` throws |
| L5 | `CareGroupMemberRepository` has no method to fetch a single member scoped to its group | TDS §8.2 adds `findByIdAndCareGroupId(UUID, UUID)` | Repository-level test/mock verifies this exact method signature is invoked, not a raw `findById` |
| L6 | No existing `existsByCareGroupIdAndUserIdAndInviteStatusAndMemberRole`-style method to check OWNER role specifically | `existsByCareGroupIdAndUserIdAndInviteStatus` does not filter by role; policy implementation must additionally check `memberRole == OWNER` (TDS §8.2 note) | Test FAM72-TC-006 seeds a non-OWNER ACCEPTED member to prove role (not just ACCEPTED status) is checked |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Family / Manage Family Permission bao gồm các layer:
├── Domain (FamilyPermission value object — pure logic, no deps)
├── Application / Use Cases (updateFamilyPermission/getFamilyPermission — mock JPA Repository với Mockito)
├── Services (CareGroupServiceImpl — mock CareGroupMemberRepository, CareGroupAuthorizationPolicy,
│              FcmService, DeviceTokenRepository, AuditService với Mockito)
├── Policy (CareGroupAuthorizationPolicy — mock CareGroupMemberRepository)
├── Controller (CareGroupController — mock ICareGroupService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full PATCH/GET round trip)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-72` (§3.3.1.49) | Happy-path grant/update flow; E1 access denied; E2 invalid data; E3 external failure retry guidance |
| `ADR-FAM-002` | ACCEPTED-only membership invariant reused for target-eligibility checks |
| `ADR-FAM-020` (Open) | 4-boolean permission shape; unknown-key rejection |
| `ADR-FAM-021` (Open) | OWNER-only authorization |
| `ADR-FAM-022` | FCM failure is non-blocking |
| `ADR-FAM-023` (Open) | Target member must be ACCEPTED |
| `BR-RBAC` | Role/permission-scope enforcement tests |
| `BR-PRIVACY` | Minimum-necessary-access — reject unknown flags; no cross-group leakage |
| `BR-CONSULTATION` | **Not applicable** — no booking/payment/consultation lifecycle in this feature |
| `CB-FAMILY-IMP-072` §8, §9, §10, §16 | Interface contracts, API contract, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | OWNER updates permission with valid flags → success | `CareGroupServiceImpl.updateFamilyPermission()` | `FAM72-TC-001` |
| TC-COND-002 | Partial update (only some flags provided) leaves others unchanged | `updateFamilyPermission()` | `FAM72-TC-002` |
| TC-COND-003 | Member (or OWNER) views own/managed current permission | `getFamilyPermission()` | `FAM72-TC-003` |
| TC-COND-004 | Care group not found → 404 FAM-005 | `updateFamilyPermission()`, `getFamilyPermission()` | `FAM72-TC-004`, `FAM72-TC-005` |
| TC-COND-005 | Non-owner attempts update → 403 FAM-021 | `CareGroupAuthorizationPolicy.canManagePermissions()` | `FAM72-TC-006` |
| TC-COND-006 | Target member is PENDING → 404 FAM-020 | `updateFamilyPermission()` | `FAM72-TC-007` |
| TC-COND-007 | Target member is REVOKED → 404 FAM-020 | `updateFamilyPermission()` | `FAM72-TC-008` |
| TC-COND-008 | Empty/unknown-key payload → 400 FAM-022 | `updateFamilyPermission()` | `FAM72-TC-009` |
| TC-COND-009 | FCM send throws → DB write persists, 200 returned | `updateFamilyPermission()` (ADR-FAM-022) | `FAM72-TC-010` |
| TC-COND-010 | Member with no active device tokens → FCM skipped gracefully, no exception | `updateFamilyPermission()` | `FAM72-TC-011` |
| TC-COND-011 | Audit log entry created on success | `AuditService.log(CARE_GROUP_PERMISSION_UPDATED, ...)` | `FAM72-TC-012` |
| TC-COND-012 | `FamilyPermissionUpdated` event published with correct previous/new payload | Domain event publish | `FAM72-TC-013` |
| TC-COND-013 | Non-target, non-owner member attempts GET → 403 FAM-003 | `getFamilyPermission()` | `FAM72-TC-014` |
| TC-COND-014 | Unauthenticated caller → 401 | `CareGroupController` `@PreAuthorize` | `FAM72-TC-015` |
| TC-COND-015 | Boundary — all 4 flags explicitly false | `updateFamilyPermission()` | `FAM72-TC-016` |
| TC-COND-016 | SQL/JSON-injection style payload in flag values rejected safely | Controller input validation | `FAM72-TC-017` (security) |
| TC-COND-017 | Full E2E: PATCH then GET reflects persisted state | Integration | `FAM72-TC-INT-001` |
| TC-COND-018 | Cross-group access attempt (memberId belongs to a different group) → 404/403 | `updateFamilyPermission()` | `FAM72-TC-018` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Permission flag combinations (all-true / all-false / mixed / partial-null) | Reduces combinatorial explosion of 2^4 boolean states to representative classes |
| Boundary Value Analysis | Zero non-null fields (FAM-022 boundary) vs. exactly one non-null field (valid boundary) | Confirms the "at least one field" validation boundary (§8.1 DTO validator) |
| State Transition Testing | `InviteStatus` of target member: ACCEPTED (valid) / PENDING / REVOKED (invalid) | Confirms ADR-FAM-023 invariant across all 3 relevant states |
| Error Guessing | Unknown JSON keys, injection-style strings in flag values, cross-group memberId | Covers BR-PRIVACY / OWASP-adjacent input-validation risks |
| Role-Based Access Control Testing | OWNER vs MEMBER vs VIEWER vs unauthenticated caller | Directly covers ADR-FAM-021 and §16 Authorization Matrix |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroup{ id, ownerUserId=FX-004.sub, status=ACTIVE }` | Happy path group |
| `FX-002` | DB seed | `CareGroupMember{ id, careGroupId=FX-001.id, userId=random, memberRole=OWNER, inviteStatus=ACCEPTED }` | OWNER member row |
| `FX-003` | DB seed | `CareGroupMember{ id, careGroupId=FX-001.id, userId=random, memberRole=MEMBER, inviteStatus=ACCEPTED, permissionJson='{"calendar":true,"logs":false,"alerts":true,"records":false}' }` | Target member for update/read |
| `FX-004` | JWT | `{ sub: 'owner-001', role: 'MOTHER' }` | Auth context for OWNER caller |
| `FX-005` | JWT | `{ sub: 'member-002', role: 'FAMILY' }` | Auth context for non-owner caller |
| `FX-006` | DB seed | `CareGroupMember{ ..., memberRole=MEMBER, inviteStatus=PENDING }` | Target-not-eligible case (FAM72-TC-007) |
| `FX-007` | DB seed | `CareGroupMember{ ..., memberRole=MEMBER, inviteStatus=REVOKED }` | Target-not-eligible case (FAM72-TC-008) |
| `FX-008` | DB seed | `DeviceToken{ userId=FX-003.userId, token='fcm-token-synthetic-001', active=true }` | FCM token lookup happy path |
| `FX-009` | env | none required — `FcmService` mocked in unit tests; real Firebase SDK not invoked | Isolation from external dependency |

---

## 4. Test Case Specification

> **TC ID format:** `FAM72-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// package com.carebridge.backend.family (test sources)
// Shared across UC-70/UC-71/UC-72/UC-73/UC-83 sibling test suites.
// Methods grow additively — do not redefine an existing signature.
// ═══════════════════════════════════════════════════════════

// CareGroupTestFactory.java
class CareGroupTestFactory {

    static CareGroup makeCareGroup() {
        return CareGroup.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .groupName("Test Care Group")
                .status(CareGroupStatus.ACTIVE)
                .build();
    }

    static CareGroup makeCareGroup(Consumer<CareGroup.CareGroupBuilder> overrides) {
        CareGroup.CareGroupBuilder builder = CareGroup.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .groupName("Test Care Group")
                .status(CareGroupStatus.ACTIVE);
        overrides.accept(builder);
        return builder.build();
    }

    static CareGroupMember makeCareGroupMember() {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .permissionJson(null)
                .build();
    }

    static CareGroupMember makeCareGroupMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = makeCareGroupMember();
        overrides.accept(member);
        return member;
    }

    /** UC-72 addition: builds a member with a pre-set permission_json shape. */
    static CareGroupMember makeCareGroupMemberWithPermission(String calendarLogsAlertsRecordsJson) {
        return makeCareGroupMember(m -> m.setPermissionJson(calendarLogsAlertsRecordsJson));
    }

    /** UC-72 addition: builds a valid UpdateFamilyPermissionRequest payload. */
    static UpdateFamilyPermissionRequest makePermissionUpdateRequest(Consumer<UpdateFamilyPermissionRequest> overrides) {
        UpdateFamilyPermissionRequest request = new UpdateFamilyPermissionRequest();
        request.setCalendar(true);
        request.setLogs(false);
        request.setAlerts(true);
        request.setRecords(false);
        overrides.accept(request);
        return request;
    }

    static DeviceToken makeDeviceToken(UUID userId) {
        DeviceToken token = new DeviceToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setToken("fcm-token-synthetic-" + UUID.randomUUID());
        token.setActive(true);
        return token;
    }
}
```

---

### FAM72-TC-001 — Owner updates permission with valid flags (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-72 Normal Flow Step 4/5` + `ADR-FAM-020 (Open, proposed default)`

**Preconditions:**
- FX-001 (CareGroup, ACTIVE), FX-002 (OWNER member = caller), FX-003 (target MEMBER, ACCEPTED)

**Test Steps:**
1. Arrange: seed FX-001/002/003 via `CareGroupTestFactory`; mock repository `findByIdAndCareGroupId` to return FX-003.
2. Act: call `updateFamilyPermission(groupId, FX-003.id, {calendar:true, logs:false, alerts:true, records:false}, FX-002.userId)`.
3. Assert: response contains exactly these 4 flags; repository `save()` invoked once with matching `permissionJson`.

**Expected Result (PASS — hành vi đúng):**
- Returns `FamilyPermissionResponse{calendar:true, logs:false, alerts:true, records:false}`; no exception thrown.

**Expected Result (FAIL — dấu hiệu lỗi):**
- Wrong flags persisted, or method throws unexpectedly.

**Current Status:** 🔴 Not written
**Implementation Note:** Use `FamilyPermission.fromJson`/`toJson` helpers; do not hand-roll JSON string concatenation.

---

### FAM72-TC-002 — Partial update leaves unspecified flags unchanged

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.1 UpdateFamilyPermissionRequest — nullable fields mean "unchanged"`

**Preconditions:**
- FX-003 seeded with `permissionJson = {"calendar":true,"logs":false,"alerts":true,"records":false}`.

**Test Steps:**
1. Act: call `updateFamilyPermission(...)` with only `{ logs: true }` (others null).
2. Assert: resulting permission = `{calendar:true, logs:true, alerts:true, records:false}`.

**Expected Result (PASS):** Only `logs` changed; other 3 flags retain previous values.
**Expected Result (FAIL):** Other flags reset to `false`/default instead of preserving prior value.

**Current Status:** 🔴 Not written
**Implementation Note:** Merge against `previousPermissions`, not against a blank default object.

---

### FAM72-TC-003 — Owner/member views current permission grant

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.getFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §1.1 own scope decision — POST-1 "clear result state is shown"`

**Preconditions:** FX-003 seeded with a known `permissionJson`.

**Test Steps:**
1. Act: call `getFamilyPermission(groupId, FX-003.id, FX-003.userId)` (member views own record).
2. Assert: response matches seeded `permissionJson` exactly.

**Expected Result (PASS):** Correct flags returned, no mutation occurs (read-only, no `save()` call).
**Expected Result (FAIL):** Stale/incorrect flags, or an unexpected write occurs.

**Current Status:** 🔴 Not written
**Implementation Note:** Mark method `@Transactional(readOnly = true)` matching `listMembers()` convention.

---

### FAM72-TC-004 — Update on nonexistent care group → 404 FAM-005

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 Error Codes — FAM-005 (reused from UC-70/UC-216)`

**Preconditions:** `groupRepository.findById(groupId)` mocked to return empty.

**Test Steps:**
1. Act: call `updateFamilyPermission(unknownGroupId, memberId, request, callerId)`.
2. Assert: `BusinessException` thrown with `httpStatus=404`, `code="FAM-005"`.

**Expected Result (PASS):** Exception thrown before any repository write attempt.
**Expected Result (FAIL):** NPE, or write attempted on absent group.

**Current Status:** 🔴 Not written

---

### FAM72-TC-005 — Read on nonexistent care group → 404 FAM-005

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.getFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 Error Codes — FAM-005`

**Preconditions:** `groupRepository.findById(groupId)` mocked to return empty.

**Test Steps:**
1. Act: call `getFamilyPermission(unknownGroupId, memberId, callerId)`.
2. Assert: `BusinessException(404, "FAM-005", ...)`.

**Expected Result (PASS):** Same as above for the read path.
**Expected Result (FAIL):** Silent null/empty response returned instead of exception.

**Current Status:** 🔴 Not written

---

### FAM72-TC-006 — Non-owner (ACCEPTED but role=MEMBER) attempts update → 403 FAM-021

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `CareGroupAuthorizationPolicy.canManagePermissions()`, `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupAuthorizationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-021 (Open, proposed default) — OWNER-only`

**Preconditions:**
- Caller is an ACCEPTED member with `memberRole = MEMBER` (NOT `OWNER`) of the target group.

**Test Steps:**
1. Arrange: seed caller as ACCEPTED `MEMBER`-role row (not OWNER).
2. Act: call `updateFamilyPermission(groupId, targetMemberId, request, callerId)`.
3. Assert: `BusinessException(403, "FAM-021", ...)`; `save()` never invoked (`Mockito.verify(repo, never()).save(any())`).

**Expected Result (PASS):** 403 thrown, no persistence side effect, no FCM call attempted.
**Expected Result (FAIL):** Update succeeds despite caller not being OWNER (privilege escalation risk).

**Current Status:** 🔴 Not written
**Implementation Note:** This test specifically distinguishes "ACCEPTED" (which existing `existsBy...Status` alone would satisfy) from "ACCEPTED AND role=OWNER" — verifies L6 fix (§2).

---

### FAM72-TC-007 — Target member in PENDING status → 404 FAM-020

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-023 (Open) reusing ADR-FAM-002 invariant`

**Preconditions:** FX-006 — target member seeded with `inviteStatus = PENDING`.

**Test Steps:**
1. Act: OWNER calls `updateFamilyPermission(groupId, FX-006.id, request, ownerId)`.
2. Assert: `BusinessException(404, "FAM-020", ...)`.

**Expected Result (PASS):** Update rejected; `permissionJson` remains untouched (still null/prior value).
**Expected Result (FAIL):** Update succeeds against a PENDING (not-yet-consented) member — privacy violation.

**Current Status:** 🔴 Not written

---

### FAM72-TC-008 — Target member in REVOKED status → 404 FAM-020

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-FAM-023 (Open)`

**Preconditions:** FX-007 — target member seeded with `inviteStatus = REVOKED`.

**Test Steps:**
1. Act: OWNER calls `updateFamilyPermission(groupId, FX-007.id, request, ownerId)`.
2. Assert: `BusinessException(404, "FAM-020", ...)`.

**Expected Result (PASS):** Rejected identically to the PENDING case.
**Expected Result (FAIL):** A revoked member's permissions can still be silently modified.

**Current Status:** 🔴 Not written

---

### FAM72-TC-009 — Empty/all-null payload → 400 FAM-022

**Severity:** `HIGH`
**Feature Under Test:** `UpdateFamilyPermissionRequest` validator, `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 FAM-022; §8.1 "at least one non-null field required"`

**Preconditions:** Valid OWNER caller and valid ACCEPTED target member.

**Test Steps:**
1. Act: call `updateFamilyPermission(groupId, memberId, new UpdateFamilyPermissionRequest() /* all null */, ownerId)`.
2. Assert: `BusinessException(400, "FAM-022", ...)`.

**Expected Result (PASS):** Rejected before any repository write.
**Expected Result (FAIL):** No-op update silently "succeeds" with 200 and no actual change (ambiguous result state, violates SRS E2).

**Current Status:** 🔴 Not written

---

### FAM72-TC-010 — FCM send failure does not roll back DB write (E3 / ADR-FAM-022)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `SRS UC-72 Exception E3` + `ADR-FAM-022`

**Preconditions:** Valid update request; `fcmService.sendToTokens(...)` mocked to throw `RuntimeException("FCM unavailable")`.

**Test Steps:**
1. Act: call `updateFamilyPermission(...)` with FCM mock configured to throw.
2. Assert: method completes normally (no exception propagates to caller); repository `save()` was invoked with updated `permissionJson`; response is a valid `FamilyPermissionResponse`.

**Expected Result (PASS):** DB write committed; response 200-equivalent returned; FCM exception caught/logged internally only.
**Expected Result (FAIL):** Exception propagates and the whole request fails (or worse, the DB write is rolled back), violating E3 "no duplicate unsafe action" / forcing an unnecessary retry of an already-safe operation.

**Current Status:** 🔴 Not written
**Implementation Note:** Wrap the FCM call block in try/catch inside the service method per ADR-FAM-022; do not let a `@Transactional` rollback trigger from this catch block.

---

### FAM72-TC-011 — Target member has no active device tokens → FCM skipped gracefully

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §6.1 sequence diagram — DeviceTokenRepository.findByUserIdAndActiveTrue`

**Preconditions:** `deviceTokenRepository.findByUserIdAndActiveTrue(targetUserId)` mocked to return empty list.

**Test Steps:**
1. Act: call `updateFamilyPermission(...)`.
2. Assert: `fcmService.sendToTokens()` is NOT invoked (or invoked with an empty list and handled without error); update still succeeds.

**Expected Result (PASS):** No exception; permission update succeeds regardless of notification reachability.
**Expected Result (FAIL):** NPE or unnecessary FCM call with an empty token list causing an error.

**Current Status:** 🔴 Not written

---

### FAM72-TC-012 — Successful update creates an audit log entry

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()` → `AuditService.log()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `SRS UC-72 POST-3` + `TDS §2 Traceability Matrix (AuditAction.CARE_GROUP_PERMISSION_UPDATED)`

**Preconditions:** Valid OWNER caller and ACCEPTED target member.

**Test Steps:**
1. Act: call `updateFamilyPermission(...)`.
2. Assert: `Mockito.verify(auditService).log(eq(AuditAction.CARE_GROUP_PERMISSION_UPDATED), eq(callerId), eq("CareGroupMember"), eq(memberId.toString()), anyString())`.

**Expected Result (PASS):** Exactly one audit log call with correct `AuditAction` enum value.
**Expected Result (FAIL):** No audit call, or wrong `AuditAction` value used (breaks POST-3 traceability).

**Current Status:** 🔴 Not written
**Implementation Note:** `AuditAction.CARE_GROUP_PERMISSION_UPDATED` is a NEW enum constant — must be added to `com.carebridge.backend.audit.entity.AuditAction` (currently only has `CARE_GROUP_CREATED`).

---

### FAM72-TC-013 — `FamilyPermissionUpdated` event published with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §7.3 Payload Schema`

**Preconditions:** FX-003 with known previous `permissionJson`; `ApplicationEventPublisher` mocked/captured.

**Test Steps:**
1. Act: call `updateFamilyPermission(...)` changing `logs` from `false` to `true`.
2. Assert: captured event payload has `previousPermissions.logs=false`, `newPermissions.logs=true`, correct `careGroupId`/`careGroupMemberId`/`updatedBy`.

**Expected Result (PASS):** Event payload accurately reflects before/after state.
**Expected Result (FAIL):** Event missing, or previous/new values swapped/incorrect.

**Current Status:** 🔴 Not written

---

### FAM72-TC-014 — Non-target, non-owner member attempts GET → 403 FAM-003

**Severity:** `HIGH`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `CareGroupServiceImpl.getFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §16 Authorization Matrix — GET restricted to target member or OWNER`

**Preconditions:** Caller is a third ACCEPTED member of the same group, neither the target nor the OWNER.

**Test Steps:**
1. Act: call `getFamilyPermission(groupId, otherMemberId, thirdPartyCallerId)`.
2. Assert: `BusinessException(403, "FAM-003", ...)`.

**Expected Result (PASS):** Read denied — prevents a member from viewing another member's permission grant (BR-PRIVACY minimum-necessary access).
**Expected Result (FAIL):** Any accepted member can read any other member's permission record — data-exposure leak.

**Current Status:** 🔴 Not written

---

### FAM72-TC-015 — Unauthenticated caller → 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `CareGroupController` (`@PreAuthorize`)
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `SRS UC-72 Exception E1`

**Preconditions:** No `Authorization` header / no valid JWT.

**Test Steps (Attack Simulation):**
1. Send `PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions` with no Authorization header.
2. Assert response.

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized` (Spring Security filter chain, before controller method executes).
**Expected Result (FAIL = lỗ hổng tồn tại):** Request reaches service layer / returns 200 without authentication.

**Current Status:** 🔴 Not written

---

### FAM72-TC-016 — Boundary: all 4 flags explicitly set to false

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `Boundary Value Analysis (TDS-04)`

**Preconditions:** Valid OWNER + ACCEPTED target.

**Test Steps:**
1. Act: call with `{calendar:false, logs:false, alerts:false, records:false}`.
2. Assert: persisted `permissionJson` = all-false; request is NOT rejected as "empty" (distinguish `false` from `null`/absent, per FAM-022 boundary in TC-009).

**Expected Result (PASS):** All-false is a valid, distinct state from "no fields provided."
**Expected Result (FAIL):** Validator incorrectly treats `false` values as "absent" and throws FAM-022 (boolean/null confusion bug).

**Current Status:** 🔴 Not written
**Implementation Note:** DTO fields must be boxed `Boolean` (nullable), not primitive `boolean`, to distinguish "unset" from "false."

---

### SECURITY TEST CASES

---

### FAM72-TC-017 — Injection-style string rejected in permission flag field (type confusion attempt)

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-20 — Improper Input Validation`
**Legal:** `PDPA data-minimization — malformed input must not silently corrupt permission_json`
**Feature Under Test:** `UpdateFamilyPermissionRequest` binding / Jackson deserialization
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Valid OWNER JWT.

**Test Steps (Attack Simulation):**
1. Send `PATCH .../permissions` with body `{ "calendar": "'; DROP TABLE care_group_members; --" }` (string where boolean expected).
2. Assert response.

**Expected Result (PASS = hệ thống an toàn):** `400 Bad Request` from Jackson type-mismatch / Bean Validation — request never reaches the repository layer (JPA/parameterized queries also make raw SQL injection structurally impossible here, but the type-confusion input must still be rejected cleanly, not silently coerced).
**Expected Result (FAIL = lỗ hổng tồn tại):** Server error (500) leaking stack trace, or the malformed value is coerced/stored as a literal string inside `permission_json`.

**Current Status:** 🔴 Not written

---

### FAM72-TC-018 — Cross-group memberId attempt (member belongs to a different care group)

**Severity:** `HIGH`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `CareGroupServiceImpl.updateFamilyPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS §8.2 findByIdAndCareGroupId(memberId, careGroupId) — scoped lookup`

**Preconditions:** `memberId` exists in the DB but belongs to `otherGroupId != groupId` in the path.

**Test Steps (Attack Simulation):**
1. OWNER of `groupId` calls `PATCH /api/v1/care-groups/{groupId}/members/{memberIdFromAnotherGroup}/permissions`.
2. Assert response.

**Expected Result (PASS = hệ thống an toàn):** `404 FAM-020` — `findByIdAndCareGroupId` returns empty because the composite (memberId, groupId) does not match; no cross-group data leak or mutation.
**Expected Result (FAIL = lỗ hổng tồn tại):** Update applied to a member of a different care group (IDOR — Insecure Direct Object Reference).

**Current Status:** 🔴 Not written
**Implementation Note:** This is precisely why `findByIdAndCareGroupId` (not a bare `findById`) is required in §8.2 — reviewers must confirm the repository call uses BOTH ids.

---

### INTEGRATION TEST CASES

---

### FAM72-TC-INT-001 — Full E2E: PATCH then GET reflects persisted permission state

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH /permissions → GET /permissions`
**Test File:** `src/test/java/com/carebridge/backend/family/ManageFamilyPermissionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically on Spring context start (confirms no NEW migration is
  needed — `permission_json`/`device_tokens` already present from V1/V2)
- Seed: one `CareGroup` (ACTIVE), one OWNER member (ACCEPTED), one target MEMBER (ACCEPTED, no
  initial `permissionJson`)

**Test Steps:**
1. Seed minimal data via `CareGroupTestFactory` + direct repository saves.
2. `PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions` as OWNER with
   `{calendar:true, logs:true, alerts:false, records:false}`.
3. `GET /api/v1/care-groups/{groupId}/members/{memberId}/permissions` as the same OWNER.
4. Assert DB state directly via repository.

**Expected Result (PASS):**
- PATCH returns 200 with matching flags.
- GET returns identical flags to what was just PATCHed.
- DB row's `permission_json` column contains the expected JSON.

**Expected Result (FAIL):**
- GET returns stale/different data than what PATCH persisted (read-after-write inconsistency).

**DB Assertion:**
```java
CareGroupMember record = memberRepository.findById(savedMemberId).orElseThrow();
assertThat(record.getPermissionJson()).isNotNull();
FamilyPermission perm = FamilyPermission.fromJson(record.getPermissionJson());
assertThat(perm.isCalendar()).isTrue();
assertThat(perm.isLogs()).isTrue();
assertThat(perm.isAlerts()).isFalse();
assertThat(perm.isRecords()).isFalse();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM72-TC-001` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-002` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-003` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-004` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-005` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-006` | `CareGroupAuthorizationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-007` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-008` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-009` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-010` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-011` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-012` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-013` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-014` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-015` | `CareGroupControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-016` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-017` | `CareGroupControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-018` | `CareGroupServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM72-TC-INT-001` | `ManageFamilyPermissionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (MUST throw)
// Add to CareGroupServiceImpl temporarily before real implementation
@Override
public FamilyPermissionResponse updateFamilyPermission(
        UUID careGroupId, UUID memberId, UpdateFamilyPermissionRequest request, UUID callerId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

@Override
public FamilyPermissionResponse getFamilyPermission(UUID careGroupId, UUID memberId, UUID callerId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM72-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-015` | `throw('Not implemented')` (401 expected pre-controller, stub irrelevant) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-017` | `throw('Not implemented')` (400 expected pre-service, stub irrelevant) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM72-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` (to be generated at implementation time)

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAMILY-IMP-072` đã được review và approve
- [ ] ADR-FAM-020, ADR-FAM-021, ADR-FAM-023 confirmed by DPO/product (currently Open — approval
      blocks Entry)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] **Không cần Flyway migration** — confirmed §5.2 of TDS; `permission_json` and
      `device_tokens` already exist and applied
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — all unit tests green (no skips)
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Test coverage ≥ 80% lines for `CareGroupServiceImpl` new methods and
      `CareGroupAuthorizationPolicy`
- [ ] No business logic in `CareGroupController` (validation + mapping only)
- [ ] No PII/device-token plaintext appears in logs
- [ ] All 18 functional/security TCs + 1 integration TC pass
- [ ] FCM failure path (FAM72-TC-010) verified not to roll back the DB write

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — every injected class exists in the codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — no shared mutable state between tests (each test uses
      `CareGroupTestFactory` fresh-instance methods)
- [ ] **Oracle Source** — every expected value in an assertion cites BR/AC/ADR (see each TC's
      `Oracle Source` field above)

### Suspension Criteria (Điều kiện tạm dừng)

- ADR-FAM-020/021/023 remain unconfirmed by DPO/product beyond sprint boundary
- CI pipeline broken by unrelated changes
- Discovery of a new architectural gap requiring Principal Architect review (e.g. if
  `permission_json` turns out to need a real Postgres CHECK constraint)

---

## 7. Rollback Plan

```bash
# No migration to revert — confirmed no schema change in this feature (TDS §5.2).

# Revert implementation files only
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/

# Gap remains OPEN — keep entry in PHASE_GAP_ANALYSIS.md (if tracked) until re-approved
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ (all 19 TCs cite an Oracle Source — see §4) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ (Red Gate table pending execution at implementation time) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ (all authorization/eligibility/shape assumptions trace to ADR-FAM-020/021/023) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ (controller tests, FAM72-TC-015/017, verify only HTTP status/validation, not business rules) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ (all imports — `FcmService`, `DeviceTokenRepository`, `AuditService`, `BusinessException` — verified to exist in codebase during research; `FamilyPermission`/`UpdateFamilyPermissionRequest`/`CareGroupAuthorizationPolicy` are NEW per this TDS and must be created before tests compile) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Cần fix trước khi implement → xem bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-005` (partial) | All TCs referencing `FamilyPermission`, `UpdateFamilyPermissionRequest`, `CareGroupAuthorizationPolicy` | These 3 types do not yet exist in the codebase — they are NEW types defined by this TDS (§8.1/§8.2), not hallucinated from an unrelated module | Confirm at Red Gate that compile fails cleanly for "not yet created" reasons, not for referencing a genuinely nonexistent unrelated contract; create these types as part of Red Phase stub setup (empty class shells throwing `UnsupportedOperationException` where applicable) before running Red Gate | ☐ |

---

*TDD Template v2.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol applied.*
*Status: Draft. All permission-model, authorization, and eligibility test oracles trace to Open
ADRs (ADR-FAM-020/021/023) — do not treat any assertion here as final until those ADRs are
Accepted.*
