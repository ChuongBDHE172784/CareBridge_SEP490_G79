# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test-Spec — UC-86 View Family Alert

**Document ID:** `CB-FAM-TDD-007`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.3.4 (L3296-3315) — functional requirements
- `04_Implement/UC86_ViewFamilyAlert/UC86_ViewFamilyAlert_TDS.md` (`CB-FAM-IMP-007`) — companion Technical Design Specification
- `04_Implement/UC84_ViewSharedData/UC84_ViewSharedData_TDS.md` (`CB-FAM-IMP-004`) — sibling precedent for `CareGroupAccessPolicy`/`PermissionCategory`
- `CLAUDE.md` — CareBridge architecture and delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC-86 View Family Alert |

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
| **Feature / Gap ID** | `GAP-UC86` |
| **Module** | `ViewFamilyAlert — family bounded context (read path over notification data)` |
| **Spec gốc** | `CB-FAM-IMP-007` |
| **Priority** | 🟠 P1 (Medium priority per SRS, but touches safety-relevant EMERGENCY alert visibility) |
| **Sprint** | `Sprint 3 "Cross-Domain Integration"` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `notification` package (`NotificationRecord`, `NotificationType`, `NotificationRecordRepository`), `family` package (`CareGroupMember`, `CareGroupMemberRepository`, `InviteStatus`, shared `CareGroupAccessPolicy`) |
| **Downstream Consumers** | Family Sync mobile inbox screen |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-007 §17`, `ADR-FAM-006`, `ADR-FAM-007`, `ADR-FAM-008` |
| **Constraints Injected** | C1 (self-scope from JWT), C2 (EMERGENCY safety override), C3 (deny-by-default for non-EMERGENCY), C4 (field minimization), C5 (no new migration), C6 (shared policy class), C7 (read-only, no authoring), C8 (404 not 403 for cross-user detail) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.3.4 does not specify which data source backs "family alert" | `notification_records` table (existing `NotificationRecord` entity) is the confirmed reuse target per ADR-FAM-006 — tests assert against `NotificationRecordRepository.findByUserId`, NOT any `safety_alerts`/`family_alert_log` query | All test fixtures seed `NotificationRecord` rows, not `safety_alerts` rows |
| L2 | SRS does not specify the exact consent-minimization mechanism | `care_group_members.permission_json` (jsonb) exists as a DB column but has NO JPA field on `CareGroupMember` today — tests must first assert the entity change (Chặng 1 in TDS) exists before testing `hasPermission()` | TC-UNIT-001 series test `CareGroupAccessPolicy.hasPermission()` against a `CareGroupMember` fixture with `permissionJson` populated |
| L3 | SRS does not specify whether EMERGENCY-type alerts can be hidden by permission settings | ADR-FAM-008 establishes a safety override: EMERGENCY alerts are NEVER filtered by `permission_json.alerts` | TC-UNIT-002/TC-INT-002 explicitly assert EMERGENCY visibility even with `alerts=false` |
| L4 | SRS AF2 "no matching data" wording is ambiguous between empty-list (200) and not-found (404) | Following UC-84's established pattern (AF2 = 200 with empty `items[]`), NOT a 404/error | TC-UNIT-003 asserts 200 + empty array, never an exception, for zero-record case |
| L5 | Cross-user alert detail access — ambiguous between 403 (exists, no permission) and 404 (hide existence) | ADR-FAM-008/C8: 404 chosen to avoid confirming existence to non-owner (least-privilege information-disclosure minimization) | TC-E2E-002 asserts 404, explicitly NOT 403, for another user's `alertId` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewFamilyAlert bao gồm các layer:
├── Domain (pure logic — FamilyAlertSeverity mapping, no deps)
├── Application / Use Cases (FamilyAlertServiceImpl — mock JPA Repository với Mockito)
├── Policy (CareGroupAccessPolicy.hasPermission — mock JPA Repository với Mockito)
├── Controller (FamilyAlertController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-86 §3.3.3.4 (L3296-3315)` | Normal Flow steps, AF1-AF3, E1-E3, PRE-1..4, POST-1..3 |
| `ADR-FAM-006` | Alert source = `notification_records`, read-only, no authoring |
| `ADR-FAM-007` | Deny-by-default permission gate, field-level minimization |
| `ADR-FAM-008` | Self-scoped inbox, EMERGENCY safety override |
| `BR-RBAC` | Access denied when unauthenticated/out-of-scope |
| `BR-PRIVACY` / PDPA | Minimum-necessary field exposure |
| `CB-FAM-IMP-007 §8` | Service/Repository contract shapes |
| `CB-FAM-IMP-007 §10` | Error code assertions (FAM-001, FAM-020, FAM-021, FAM-022) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Authenticated user with own EMERGENCY alert, permission flag false | `FamilyAlertServiceImpl.listFamilyAlerts()` | `FAM-TC-001` |
| TC-COND-002 | Authenticated user with own non-EMERGENCY care-group alert, flag true/false/NULL/missing | `FamilyAlertServiceImpl.listFamilyAlerts()` | `FAM-TC-002`, `FAM-TC-003`, `FAM-TC-004`, `FAM-TC-005` |
| TC-COND-003 | No alerts exist for user | `FamilyAlertServiceImpl.listFamilyAlerts()` | `FAM-TC-006` |
| TC-COND-004 | Unauthenticated request | `FamilyAlertController.listFamilyAlerts()` | `FAM-TC-007` |
| TC-COND-005 | Detail view of own alert | `FamilyAlertServiceImpl.getFamilyAlertDetail()` | `FAM-TC-008` |
| TC-COND-006 | Detail view of another user's alert | `FamilyAlertServiceImpl.getFamilyAlertDetail()` | `FAM-TC-009` |
| TC-COND-007 | Detail view, non-EMERGENCY, permission denied | `FamilyAlertServiceImpl.getFamilyAlertDetail()` | `FAM-TC-010` |
| TC-COND-008 | Response DTO field-level minimization (no raw body/email/phone) | `FamilyAlertItemDto` mapping | `FAM-TC-011` |
| TC-COND-009 | Pagination and `unreadCount` computation | `FamilyAlertServiceImpl.listFamilyAlerts()` | `FAM-TC-012` |
| TC-COND-010 | `CareGroupAccessPolicy.hasPermission()` deny-by-default (shared w/ UC-84) | `CareGroupAccessPolicy` | `FAM-TC-013`, `FAM-TC-014` |
| TC-COND-011 | Full E2E flow through controller + service + DB | Integration | `FAM-TC-INT-001`, `FAM-TC-INT-002` |
| TC-COND-012 | Security: SQL/NoSQL injection attempt via `alertId` path param | `FamilyAlertController` | `FAM-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `NotificationType` values (EMERGENCY vs. all others) | Two behaviorally distinct partitions per ADR-FAM-008 |
| Boundary Value Analysis | `permission_json` states: populated-true, populated-false, missing-key, NULL entirely | Exact boundary of deny-by-default rule (ADR-FAM-007) |
| State Transition Testing | `isRead` false→true (reused from UC-12, not re-tested here beyond confirming reuse) | Avoid duplicate test investment on already-tested mutation |
| Error Guessing | Cross-user `alertId` enumeration, missing JWT, malformed pagination params | Security/attack vectors per BR-RBAC |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `NotificationRecord{ userId: ACC-001, type: EMERGENCY, isRead: false }` | Safety-override happy path |
| `FX-002` | DB seed | `NotificationRecord{ userId: ACC-001, type: REMINDER, referenceType: 'CARE_GROUP' }` | Deny-by-default candidate |
| `FX-003` | DB seed | `CareGroupMember{ userId: ACC-001, careGroupId: CG-001, inviteStatus: ACCEPTED, permissionJson: '{"alerts": true}' }` | Permission-granted case |
| `FX-004` | DB seed | `CareGroupMember{ userId: ACC-001, careGroupId: CG-001, inviteStatus: ACCEPTED, permissionJson: '{"alerts": false}' }` | Permission-denied case |
| `FX-005` | DB seed | `CareGroupMember{ userId: ACC-001, careGroupId: CG-001, inviteStatus: ACCEPTED, permissionJson: null }` | NULL deny-by-default case |
| `FX-006` | DB seed | `CareGroupMember{ userId: ACC-001, careGroupId: CG-001, inviteStatus: ACCEPTED, permissionJson: '{}' }` | Missing-key deny-by-default case |
| `FX-007` | JWT | `{ sub: 'ACC-001', role: 'FAMILY' }` | Auth context, owner |
| `FX-008` | JWT | `{ sub: 'ACC-002', role: 'FAMILY' }` | Auth context, non-owner (cross-user test) |
| `FX-009` | DB seed | Zero `NotificationRecord` rows for `ACC-003` | Empty-state test |

---

## 4. Test Case Specification

> **TC ID format:** `FAM-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

class FamilyAlertTestFactory {

    static NotificationRecord makeNotification() {
        return makeNotification(r -> {});
    }

    static NotificationRecord makeNotification(Consumer<NotificationRecord.NotificationRecordBuilder> overrides) {
        NotificationRecord.NotificationRecordBuilder b = NotificationRecord.builder()
            .id(UUID.randomUUID())
            .userId(UUID.fromString("00000000-0000-0000-0000-0000000000A1"))
            .type(NotificationType.EMERGENCY)
            .title("Possible fall detected")
            .body("A safety event was detected for the monitored user.")
            .status(NotificationRecordStatus.SENT)
            .isRead(false)
            .createdAt(Instant.parse("2026-07-02T07:55:00Z"))
            .sentAt(Instant.parse("2026-07-02T07:55:01Z"));
        overrides.accept(b);
        return b.build();
    }

    static CareGroupMember makeMember() {
        return makeMember(m -> {});
    }

    static CareGroupMember makeMember(Consumer<CareGroupMember.CareGroupMemberBuilder> overrides) {
        CareGroupMember.CareGroupMemberBuilder b = CareGroupMember.builder()
            .id(UUID.randomUUID())
            .careGroupId(UUID.fromString("00000000-0000-0000-0000-0000000CG01"))
            .userId(UUID.fromString("00000000-0000-0000-0000-0000000000A1"))
            .memberRole(GroupMemberRole.MEMBER)
            .inviteStatus(InviteStatus.ACCEPTED)
            .permissionJson("{\"alerts\": true}");
        overrides.accept(b);
        return b.build();
    }
}
```

---

### FAM-TC-001 — EMERGENCY alert always visible regardless of permission flag

**Severity:** `CRITICAL`
**Feature Under Test:** `FamilyAlertServiceImpl.listFamilyAlerts()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/FamilyAlertServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-008 §Quyết định (safety override)`

**Preconditions:**
- `FX-001` (EMERGENCY NotificationRecord for ACC-001)
- `FX-004` (CareGroupMember for ACC-001 with `alerts: false`)

**Test Steps:**
1. Mock `notificationRecordRepository.findByUserId(ACC-001, pageable)` to return `[FX-001]`.
2. Call `familyAlertService.listFamilyAlerts(ACC-001, 0, 20)`.
3. Assert response.

**Expected Result (PASS):**
- `items` contains exactly 1 entry with `type=EMERGENCY`, `severity=EMERGENCY`.
- `CareGroupAccessPolicy.hasPermission()` is NEVER invoked for this record (verify via Mockito `verifyNoInteractions` scoped to this item, or assert call count excludes the EMERGENCY row).

**Expected Result (FAIL):**
- EMERGENCY item missing from `items`, or `hasPermission()` was called and returned false causing exclusion.

**Current Status:** 🔴 Not written
**Implementation Note:** Filter order in `FamilyAlertServiceImpl` must short-circuit on `type == EMERGENCY` BEFORE calling `hasPermission()`.

---

### FAM-TC-002 — Non-EMERGENCY, care-group alert, permission=true → visible

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertServiceImpl.listFamilyAlerts()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/FamilyAlertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-007 §Quyết định`

**Preconditions:** `FX-002` (REMINDER, referenceType=CARE_GROUP), `FX-003` (permission alerts=true)

**Test Steps:**
1. Mock repository return `[FX-002]`.
2. Mock `hasPermission(groupId, ACC-001, ALERTS)` → `true`.
3. Call `listFamilyAlerts(ACC-001, 0, 20)`.

**Expected Result (PASS):** `items` contains the REMINDER alert.
**Expected Result (FAIL):** Item excluded despite permission=true.
**Current Status:** 🔴 Not written

---

### FAM-TC-003 — Non-EMERGENCY, care-group alert, permission=false → excluded

**Severity:** `CRITICAL`
**Feature Under Test:** `FamilyAlertServiceImpl.listFamilyAlerts()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-007 §Quyết định (deny-by-default)`

**Preconditions:** `FX-002`, `FX-004` (permission alerts=false)

**Test Steps:** Same as FAM-TC-002 but `hasPermission()` → `false`.

**Expected Result (PASS):** `items` does NOT contain the REMINDER alert; `totalItems` reflects the filtered count; NO exception thrown.
**Expected Result (FAIL):** Item present, or a `ForbiddenException` is thrown for the whole list request (wrong — per-item filtering, not whole-request rejection).
**Current Status:** 🔴 Not written

---

### FAM-TC-004 — Non-EMERGENCY, permission_json NULL → excluded (deny-by-default)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-007 §Quyết định`

**Preconditions:** `FX-005` (permissionJson = null)

**Test Steps:**
1. Call `hasPermission(CG-001, ACC-001, PermissionCategory.ALERTS)` directly.

**Expected Result (PASS):** Returns `false`.
**Expected Result (FAIL):** Returns `true` or throws `NullPointerException`.
**Current Status:** 🔴 Not written

---

### FAM-TC-005 — Non-EMERGENCY, permission_json missing `alerts` key → excluded

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-007 §Quyết định`

**Preconditions:** `FX-006` (permissionJson = `'{}'`)

**Expected Result (PASS):** Returns `false`.
**Expected Result (FAIL):** Returns `true` or throws parsing exception uncaught.
**Current Status:** 🔴 Not written

---

### FAM-TC-006 — Empty alert list → 200 with empty items (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `FamilyAlertServiceImpl.listFamilyAlerts()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `SRS AF2 (L3307)`, consistent with UC-84's AF2 pattern

**Preconditions:** `FX-009` (zero records for ACC-003)

**Expected Result (PASS):** `totalItems=0`, `unreadCount=0`, `items=[]`; no exception thrown.
**Expected Result (FAIL):** Exception thrown, or `null` returned instead of empty list.
**Current Status:** 🔴 Not written

---

### FAM-TC-007 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `FamilyAlertController.listFamilyAlerts()`
**Test File:** `src/test/java/com/carebridge/backend/family/controller/FamilyAlertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `SRS E1 (L3308)`, `BR-RBAC`

**Preconditions:** No `Authorization` header

**Test Steps:**
1. `GET /api/v1/family-alerts` without JWT via `@WebMvcTest` MockMvc.

**Expected Result (PASS):** `401 Unauthorized`, body `error.code = "FAM-001"`.
**Expected Result (FAIL):** `200` returned, or `500` (unhandled exception).
**Current Status:** 🔴 Not written

---

### FAM-TC-008 — Detail view of own alert → 200

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertServiceImpl.getFamilyAlertDetail()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-FAM-IMP-007 §8.1`

**Preconditions:** `FX-001` owned by ACC-001

**Expected Result (PASS):** Returns `FamilyAlertItemDto` matching `FX-001`.
**Expected Result (FAIL):** Throws unexpected exception or returns null.
**Current Status:** 🔴 Not written

---

### FAM-TC-009 — Detail view of another user's alert → 404 (not 403)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `FamilyAlertServiceImpl.getFamilyAlertDetail()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-008 §Quyết định`, `CB-FAM-IMP-007 C8`

**Preconditions:** `FX-001` owned by ACC-001; caller is ACC-002

**Test Steps:**
1. Call `getFamilyAlertDetail(ACC-002, FX-001.id)`.

**Expected Result (PASS):** Throws `NotFoundException` with code `FAM-020` (NOT `ForbiddenException`/`FAM-021`).
**Expected Result (FAIL):** Returns the alert data (leak), or throws 403 instead of 404 (confirms existence to non-owner).
**Current Status:** 🔴 Not written
**Implementation Note:** Query MUST be `findByIdAndUserId`-style (ownership baked into the query), not `findById` + separate ownership check that could leak via timing/error-message differences.

---

### FAM-TC-010 — Detail view, non-EMERGENCY, permission denied → 403

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertServiceImpl.getFamilyAlertDetail()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-FAM-007`

**Preconditions:** `FX-002` (REMINDER, own alert), `FX-004` (permission alerts=false)

**Expected Result (PASS):** Throws `ForbiddenException` code `FAM-021`.
**Expected Result (FAIL):** Returns data despite permission denial, or wrong error code.
**Current Status:** 🔴 Not written

---

### FAM-TC-011 — Response DTO excludes raw body / PII

**Severity:** `CRITICAL`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** `PDPA — minimum-necessary access`
**Feature Under Test:** `FamilyAlertServiceImpl` mapping logic (`toFamilyAlertItemDto`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-007 C4/C5`, `BR-PRIVACY`

**Preconditions:** `FX-001` with `body` containing a fabricated PII-shaped string, e.g. `"Contact mother at 090-000-0000 or mother@example.com"`

**Test Steps:**
1. Map `FX-001` through the service's DTO conversion.
2. Inspect `FamilyAlertItemDto.summary` and all other fields.

**Expected Result (PASS):** No field contains the raw phone number or email substring; `summary` is a bounded, redacted/derived string, not `body` verbatim.
**Expected Result (FAIL):** `summary` or any field equals `body` verbatim or contains the phone/email substring.
**Current Status:** 🔴 Not written

---

### FAM-TC-012 — Pagination and unreadCount computation

**Severity:** `MEDIUM`
**Feature Under Test:** `FamilyAlertServiceImpl.listFamilyAlerts()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-FAM-IMP-007 §8.1 FamilyAlertListResponse`

**Preconditions:** 3 records: 2 unread, 1 read, all EMERGENCY (to avoid permission-filter noise)

**Expected Result (PASS):** `totalItems=3`, `unreadCount=2`.
**Expected Result (FAIL):** Miscount, or `unreadCount` computed before permission filtering (must be computed AFTER filtering, on the final visible set).
**Current Status:** 🔴 Not written

---

### FAM-TC-013 — hasPermission() with alerts=true

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-007`

**Preconditions:** `FX-003`

**Expected Result (PASS):** Returns `true`.
**Current Status:** 🔴 Not written

---

### FAM-TC-014 — hasPermission() when caller is not an ACCEPTED member

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-007`, mirrors UC-84's `isMember` precedent

**Preconditions:** `CareGroupMember` with `inviteStatus = PENDING` or `REVOKED`, or no member row at all

**Expected Result (PASS):** Returns `false` regardless of `permissionJson` content.
**Expected Result (FAIL):** Returns `true` for a PENDING/REVOKED member.
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### FAM-TC-SEC-001 — Path parameter injection / IDOR probe on alertId

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Insecure Direct Object Reference`
**Legal:** `PDPA — unauthorized access to family/health-adjacent data`
**Feature Under Test:** `FamilyAlertController.getFamilyAlertDetail()`
**Test File:** `src/test/java/com/carebridge/backend/family/controller/FamilyAlertControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** ACC-002 has a valid JWT; ACC-001 owns an EMERGENCY alert

**Test Steps (Attack Simulation):**
1. ACC-002 requests `GET /api/v1/family-alerts/{ACC-001's alertId}`.
2. Attempt sequential/enumerated UUIDs to probe for other alerts.

**Expected Result (PASS = hệ thống an toàn):** Every non-owned `alertId` → `404 FAM-020`, uniform response shape/timing regardless of whether the ID exists at all vs. belongs to someone else (no oracle for enumeration).

**Expected Result (FAIL = lỗ hổng tồn tại):** Any response reveals whether an `alertId` exists (e.g., 403 vs 404 distinction), enabling enumeration; or data leaks.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FAM-TC-INT-001 — Full flow with DB, mixed alert types and permission combos

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP GET → Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/family/FamilyAlertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied (existing `V1__init_schema.sql` + all subsequent, including the family package's tables — NO new migration expected per this TDS)
- Seed: `FX-001` (EMERGENCY), `FX-002` (REMINDER/CARE_GROUP), `FX-003`/`FX-004`/`FX-005`/`FX-006` permission variants across 4 distinct care groups

**Test Steps:**
1. Seed DB with all fixtures for ACC-001 across 4 care groups (one per permission variant).
2. `GET /api/v1/family-alerts` as ACC-001.
3. Assert DB state and response shape.

**Expected Result (PASS):**
- EMERGENCY alert always present.
- REMINDER alert present only for the care group with `alerts=true`.
- Response `totalItems` matches exactly the expected visible count (1 EMERGENCY + 1 permitted REMINDER = 2).

**Expected Result (FAIL):** Wrong count, or an exception/500 instead of a filtered 200.

**DB Assertion:**
```java
List<NotificationRecord> all = notificationRecordRepository.findByUserId(ACC_001, Pageable.unpaged()).getContent();
assertThat(all).hasSize(4); // all rows still exist in DB — filtering is response-layer only, not a delete
```

**Current Status:** 🔴 Not written

---

### FAM-TC-INT-002 — EMERGENCY override survives real permission_json NULL in DB

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow`
**Test File:** `src/test/java/com/carebridge/backend/family/FamilyAlertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:** Real DB row with `permission_json IS NULL` (via raw SQL insert to bypass any application-level default), plus an EMERGENCY `NotificationRecord`

**Test Steps:**
1. Insert `care_group_members` row via JDBC with `permission_json = NULL` explicitly.
2. Insert EMERGENCY `notification_records` row for the same user.
3. `GET /api/v1/family-alerts` as that user.

**Expected Result (PASS):** EMERGENCY alert is present in the response despite `permission_json IS NULL` at the raw DB level (not just the Java-object level) — proves the safety override is not accidentally short-circuited by an ORM null-handling quirk.

**Expected Result (FAIL):** EMERGENCY alert missing, or a `NullPointerException` propagates to a 500.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM-TC-001` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-002` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-003` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-004` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-005` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-006` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-007` | `FamilyAlertControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-008` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-009` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-010` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-011` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-012` | `FamilyAlertServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-013` | `CareGroupAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-014` | `CareGroupAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-SEC-001` | `FamilyAlertControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-INT-001` | `FamilyAlertIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM-TC-INT-002` | `FamilyAlertIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class FamilyAlertServiceImpl implements IFamilyAlertService {

    @Override
    public FamilyAlertListResponse listFamilyAlerts(UUID accountId, int page, int size) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public FamilyAlertItemDto getFamilyAlertDetail(UUID accountId, UUID alertId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-004` | N/A (tests `CareGroupAccessPolicy` directly, not the service stub) | 🔴 FAIL (policy also unimplemented) | ☐ FAIL ☐ PASS | |
| `FAM-TC-005` | N/A (policy) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-007` | Controller delegates to stub → 500, not 401 | 🔴 FAIL (expects 401, gets 500) | ☐ FAIL ☐ PASS | |
| `FAM-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-013` | N/A (policy stub separately throws) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-014` | N/A (policy stub separately throws) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-SEC-001` | Controller delegates to stub → 500 | 🔴 FAIL (expects 404, gets 500) | ☐ FAIL ☐ PASS | |
| `FAM-TC-INT-001` | Full stack stub → 500 | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-TC-INT-002` | Full stack stub → 500 | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS bất thường (đặc biệt FAM-TC-001/003/004 — permission logic):** Dừng lại. Đây là dấu hiệu Tautology (test không thực sự assert filtering logic) hoặc Shared State (fixture bị mutate giữa các test). Rewrite theo Props Isolation Pattern (§4).

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-007` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect — đặc biệt L2 (permission_json entity mapping) và L5 (404 vs 403 decision)
- [ ] `CareGroupMember.permissionJson` field mapping coordinated with UC-84 implementer (Open Item OI-86-3) to avoid duplicate PR conflicts
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `FamilyAlertServiceImpl`
- [ ] Không có business logic trong `FamilyAlertController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] EMERGENCY safety-override behavior explicitly covered and green (FAM-TC-001, FAM-TC-INT-002) — this is the release-blocking invariant for this module
- [ ] Cross-user 404-not-403 behavior explicitly covered and green (FAM-TC-009, FAM-TC-SEC-001)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi entity instance tạo qua `FamilyAlertTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-FAM-006/007/008, SRS line refs)

### Suspension Criteria (Điều kiện tạm dừng)

- `CareGroupMember.permissionJson` entity mapping not yet landed by either UC-84 or UC-86 implementer (blocking dependency, Open Item OI-86-3)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review (e.g., if UC-84's own implementation diverges from the shared `CareGroupAccessPolicy` contract assumed here)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No DB migration to revert for this module (entity-mapping only, see TDS §5.2).
# If the CareGroupMember.permissionJson field addition was implemented as part of this
# UC's PR (not UC-84's), revert it along with the rest:

git checkout -- src/main/java/com/carebridge/backend/family/entity/CareGroupMember.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/FamilyAlertServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/FamilyAlertController.java
git checkout -- src/test/java/com/carebridge/backend/family/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu có) và trong
# OPEN ITEMS SUMMARY của CB-FAM-IMP-007.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ (all TCs reference an ADR/SRS line — see Oracle Source fields) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ (pending Red Gate run at implementation time) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ (all assumptions traced to ADR-FAM-006/007/008) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ (FAM-TC-007/SEC-001 assert HTTP status only, not business rules) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ (verified: `NotificationRecord`, `CareGroupMember`, `InviteStatus` all exist; `FamilyAlertServiceImpl`/`IFamilyAlertService`/`FamilyAlertController`/`CareGroupAccessPolicy` are new, to be created per TDS §8) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn Draft — pending implementation-time Red Gate confirmation
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
