# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC116 — Update Role and Permission — Test Specification

**Document ID:** `CB-IDENTITY-TDD-116`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Implemented — 2026-07-04 (AdminRoleController/ServiceImplTest all PASS, verified independently)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`users` L532-544)
- `04_Implement/UC116_UpdateRoleAndPermission/UC116_UpdateRoleAndPermission_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16 of it)
- `04_Implement/UC114_ManageUserAccounts/UC114_ManageUserAccounts_Test-Spec.md`, `04_Implement/UC115_CreateStaffAccount/UC115_CreateStaffAccount_Test-Spec.md` — sibling Admin Governance cluster specs (shared `AdminGovernanceTestFactory` naming convention)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.18 — UC-116 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

> ⚠️ **CRITICAL SECURITY GATE — This UC is the single highest-risk endpoint in the entire Admin Governance cluster.** It is the ONLY place in the system that can change ANY user's `role`, including elevating a caller's own account to `SYSTEM_ADMIN`. Tests `UC116-TC-SEC-001` (self-escalation, any-direction self-targeting) and `UC116-TC-SEC-002` (non-admin caller rejection) are **release-blocking**, not optional.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC116 |
| `2026-07-04` | `AI Agent` | Approved by user — proceeding to implementation |
| `2026-07-28` | `AI Agent` | Added staff-governance source/destination scope tests for MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN only |

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
| **Feature / Gap ID** | `GAP-UC116` |
| **Module** | `Identity — Admin Role & Access Right Management (Admin Governance cluster)` |
| **Spec gốc** | `CB-IDENTITY-IMP-116` |
| **Priority** | 🔴 P0 (single highest-risk role-mutation surface in the system) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Milestone** | Admin Portal governance tabs |
| **Data Classification** | `PII-adjacent` (mutates `role`/`enabled`/`locked` on an existing PII-bearing `users` row) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC` |
| **Upstream Dependencies** | `security.entity.User`, `security.rbac.Role`, `audit.service.AuditService` (existing, reused) |
| **Downstream Consumers** | UC114 (role/status reflected in admin user list), UC117 (role-change events audited) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-IDENTITY-IMP-116 §17` (ADR-IAM-007, ADR-IAM-008) |
| **Constraints Injected** | Endpoint `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` only; UNCONDITIONAL rejection of `targetUserId == callerUserId` before any mutation for ANY role direction; write only `users.role`/`locked`; never invent a permission-flag schema; audit captures both previousRole/newRole |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS text ("assigns roles, updates permissions, locks or unlocks access rights") implies a possible fine-grained permission-flag system | Codebase inspection confirms the only permission model is the single `Role` enum on `users.role` (RG-6, TDS §1.1) — no permission-flag table exists or should be invented | Tests assert `UpdateUserRoleRequest` has exactly `newRole`/`lockAccessRights`/`reason` fields; a dedicated test (UC116-TC-012) asserts no permission-flag entity/table is referenced |
| L2 | UC114's `UpdateUserStatusRequest` also mutates `enabled`/`locked` — risk of overlapping write surfaces between UC114 and UC116 | TDS §1.1 explicitly scopes UC116's `role` reassignment as exclusive to this UC; UC114 never exposes `role` in its request DTO | Cross-check test (UC116-TC-013) asserts `UpdateUserStatusRequest` (UC114's DTO) has no `role`/`newRole` field — write-surface non-overlap verified structurally |
| L3 | ADR-IAM-003 (UC114) only blocks self-targeting for `enabled`/`locked`; a naive reader could assume UC116 reuses the SAME conditional guard | ADR-IAM-007 explicitly strengthens this to an UNCONDITIONAL block — no exception for any role direction, including self-demotion | UC116-TC-006 (self-escalation) AND UC116-TC-007 (self-demotion) are both tested and BOTH must be rejected — proving the guard is stricter than UC114's, not identical to it |
| L4 | No SRS text defines what happens if `newRole` equals the target's current role (no-op role "change") | Not addressed by any ADR — TDS is silent | Test UC116-TC-011 (boundary) exercises `newRole == currentRole` and documents the observed behavior (still audited as a mutation event, since TDS does not special-case a no-op) as an Open item to confirm with Tech Lead, not silently assumed |
| L5 | Earlier UC116 design allowed any of the seven roles as source and destination | Role/permission administration is a staff-governance workflow; UC115 already defines the staff set as `MODERATOR`, `CONTENT_ADMIN`, `SYSTEM_ADMIN` | Add service and web tests that reject non-staff targets and non-staff destinations before save/audit |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Identity.Admin.Role module bao gồm các layer:
├── Domain (assertNotSelfTarget guard — pure logic, unit-tested via AdminRoleServiceImplTest)
├── Services (AdminRoleServiceImpl — mock UserRepository/AuditService với Mockito)
├── Controller (AdminRoleController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real users/audit_logs rows)
└── Web Frontend (React Testing Library + Vitest — RoleAssignmentModal, UserRoleBadge)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-116 §3.2.2.18` | Happy path role reassignment, generic exception template (E1-E3) |
| `ADR-IAM-007` | ONLY SYSTEM_ADMIN reaches role-mutation logic; UNCONDITIONAL self-target block for any role direction |
| `ADR-IAM-008` | Every mutation synchronously audited with distinct `ROLE_PERMISSION_UPDATED` action, capturing previousRole + newRole |
| `BR-RBAC` | Only `SYSTEM_ADMIN` may call this endpoint |
| `RG-6 (§1.1 TDS)` | No fine-grained permission-flag schema — role + enabled/locked toggles are the entire scope |
| `V1__init_schema.sql` | Exact `users.role`/`enabled`/`locked` column set |
| `CB-IDENTITY-IMP-116 §8/§9/§10/§16` | Service/repository contracts, API shapes, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | SYSTEM_ADMIN reassigns a target's role (MODERATOR→CONTENT_ADMIN happy path) | `AdminRoleService.updateRole()` | `UC116-TC-001` |
| TC-COND-002 | SYSTEM_ADMIN reassigns a target's role AND locks access rights simultaneously | `AdminRoleService.updateRole()` | `UC116-TC-002` |
| TC-COND-003 | SYSTEM_ADMIN promotes a target to SYSTEM_ADMIN (only SYSTEM_ADMIN caller can do this, target ≠ caller) | `AdminRoleService.updateRole()` | `UC116-TC-003` |
| TC-COND-004 | Audit payload captures both previousRole and newRole | `AuditService.log()` call | `UC116-TC-004` |
| TC-COND-005 | `newRole` missing/invalid enum value rejected with IAM-116-002 | Bean Validation | `UC116-TC-005` |
| TC-COND-006 | SYSTEM_ADMIN attempts to change OWN role to SYSTEM_ADMIN or any role (self-escalation) | Self-target guard (ADR-IAM-007, unconditional) | `UC116-TC-SEC-001` |
| TC-COND-007 | SYSTEM_ADMIN attempts to change OWN role to a LOWER-privilege role (self-demotion) | Self-target guard, unconditional (proves it's not direction-dependent) | `UC116-TC-006` |
| TC-COND-008 | Non-SYSTEM_ADMIN role attempts this endpoint | Controller `@PreAuthorize` | `UC116-TC-SEC-002` |
| TC-COND-009 | `targetUserId` does not exist | Not-found guard | `UC116-TC-007` |
| TC-COND-010 | `reason` exceeds 500 chars | Bean Validation | `UC116-TC-008` |
| TC-COND-011 | Write surface touches ONLY `users.role`/`locked`, never a permission-flag table | Repository/entity scope | `UC116-TC-012` |
| TC-COND-012 | `UpdateUserStatusRequest` (UC114) has no role field — write-surface non-overlap | Cross-UC contract check | `UC116-TC-013` |
| TC-COND-013 | `newRole == currentRole` (no-op reassignment) boundary behavior | Service logic boundary | `UC116-TC-011` |
| TC-COND-014 | Full round trip against real DB — `users.role` + `audit_logs` row verified | Full flow | `UC116-TC-INT-001` |
| TC-COND-015 | Injection attempt in `reason` field handled safely | Bean Validation / JPA parameter binding | `UC116-TC-SEC-003` |
| TC-COND-016 | Web: RoleAssignmentModal disables submission when target === current admin session user | React component | `UC116-WEB-TC-001` |
| TC-COND-017 | Web: RoleAssignmentModal renders confirmation after successful role change | React component | `UC116-WEB-TC-002` |
| TC-COND-018 | Web: RoleAssignmentModal renders 403 error UI (self-escalation or non-admin) | React component | `UC116-WEB-TC-003` |
| TC-COND-019 | Target current role is outside staff-governance scope | `AdminRoleServiceImpl.assertStaffGovernanceScope()` | `UC116-TC-014` |
| TC-COND-020 | Requested destination is outside staff-governance scope | `AdminRoleServiceImpl.assertStaffGovernanceScope()` | `UC116-TC-015` |
| TC-COND-021 | Web exposes only three staff destinations and blocks direct URL access for non-staff target | `UpdateUserRolePage` | `UC116-WEB-TC-004`, `UC116-WEB-TC-005` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Staff roles (`MODERATOR`, `CONTENT_ADMIN`, `SYSTEM_ADMIN`) vs. non-staff roles (`MOTHER`, `FAMILY`, `EXPERT`, `PARTNER`) × caller-is-target vs. caller-is-not-target | Separates the accepted staff-governance role matrix from explicitly rejected domain/consumer roles |
| Boundary Value Analysis | `reason` length (500/501); `newRole == currentRole` no-op boundary | Schema/DTO-driven from §8; explicit Open item per Logic Issue L4 |
| State Transition Testing | Staff-governance role transitions within `MODERATOR`, `CONTENT_ADMIN`, `SYSTEM_ADMIN` (TDS §6.3), gated by caller identity and source/destination scope | Verifies the accepted staff matrix and rejection boundaries |
| Error Guessing | Self-escalation via own-id targeting (both up AND down); non-admin caller; injection in `reason` | Highest-severity risk named explicitly as "top self-escalation risk" in TDS §3 ADR-IAM-007 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-116-001` | DB seed | Admin caller A: `User{role:SYSTEM_ADMIN}` (via `AdminGovernanceTestFactory.makeSystemAdmin()`) | Caller identity |
| `FX-116-002` | DB seed | Admin caller B (peer admin, distinct id): `User{role:SYSTEM_ADMIN}` | Proves a role fix requires a PEER admin, not self |
| `FX-116-003` | DB seed | Target: `User{role:MODERATOR}` | Reassignment target |
| `FX-116-004` | Request | `UpdateUserRoleRequest{newRole:CONTENT_ADMIN, reason:"Promoted"}` | Happy path |
| `FX-116-005` | Request | `UpdateUserRoleRequest{newRole:SYSTEM_ADMIN}` targeting caller's OWN id | Self-escalation attempt |
| `FX-116-006` | Request | `UpdateUserRoleRequest{newRole:MOTHER}` targeting caller's OWN id | Self-demotion attempt (proves unconditional guard) |
| `FX-116-007` | JWT | `{ sub: moderator-user-id, role: 'MODERATOR' }` | Non-admin caller rejection |
| `FX-116-008` | Request | `reason="'; DROP TABLE users; --"` | Injection attempt |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Extends the shared `AdminGovernanceTestFactory` (see UC114 Test-Spec §4) with UC116-specific `makeXxx()` methods for role reassignment.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// AdminGovernanceTestFactory.java — shared factory across UC114/UC115/UC116
// This block extends the factory with UC116-specific methods.
// mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class AdminGovernanceTestFactory {

    // --- Inherited from UC114 Test-Spec: makeSystemAdmin(), makeUser(Role) ---
    // --- Inherited from UC115 Test-Spec: makeCreateStaffRequest(...) ---

    static UpdateUserRoleRequest makeUpdateRoleRequest(Consumer<UpdateUserRoleRequest> overrides) {
        UpdateUserRoleRequest r = new UpdateUserRoleRequest();
        r.setNewRole(Role.CONTENT_ADMIN);
        r.setLockAccessRights(null);
        r.setReason("Promoted to content moderation team");
        overrides.accept(r);
        return r;
    }

    // A second, distinct SYSTEM_ADMIN — required because ADR-IAM-007 makes
    // "fix your own role" structurally impossible; tests that need a legitimate
    // role-correction path must use a PEER admin, never the same id twice.
    static User makePeerSystemAdmin() {
        return makeSystemAdmin(u -> u.setId(UUID.randomUUID()));
    }
}
```

---

### UC116-TC-001 — SYSTEM_ADMIN reassigns a target's role (happy path, MODERATOR→CONTENT_ADMIN)

**Severity:** `HIGH`
**Feature Under Test:** `AdminRoleService.updateRole()`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/service/AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-IDENTITY-IMP-116 §6.1 Sequence Diagram — Happy Path`, `§8.1 Service Interface`

**Preconditions:** FX-116-001 (admin caller), FX-116-003 (target, `role=MODERATOR`).

**Test Steps:**
1. Mock `UserRepository.findById(targetId)` to return the target user.
2. Call `adminRoleService.updateRole(adminId, targetId, FX-116-004)` where `adminId != targetId`.
3. Assert `UserRepository.save()` invoked with `role=CONTENT_ADMIN`.
4. Assert `AuditService.log(AuditAction.ROLE_PERMISSION_UPDATED, adminId, "USER", targetId, payload)` invoked once with `previousRole=MODERATOR, newRole=CONTENT_ADMIN`.
5. Assert returned `UserRoleResponse` has `previousRole=MODERATOR, newRole=CONTENT_ADMIN`.

**Expected Result (PASS):** Mutation and audit both occur with correct before/after values.
**Expected Result (FAIL):** Audit missing, or role mismapped.

**Current Status:** 🔴 Not written
**Implementation Note:** Both save and audit call must occur inside the same `@Transactional` boundary per ADR-IAM-008.

---

### UC116-TC-002 — SYSTEM_ADMIN reassigns role AND locks access rights in the same request

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminRoleService.updateRole()`
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-IDENTITY-IMP-116 §8.1 UpdateUserRoleRequest.lockAccessRights`

**Test Steps:**
1. Call `updateRole(adminId, targetId, makeUpdateRoleRequest(r -> { r.setNewRole(Role.MODERATOR); r.setLockAccessRights(true); }))`.
2. Assert saved entity has `role=MODERATOR` AND `locked=true` in the same save call.
3. Assert audit payload captures `previousLocked`/`newLocked` alongside `previousRole`/`newRole`.

**Expected Result (PASS):** Both fields mutated atomically, both captured in one audit event.
**Expected Result (FAIL):** Only one of the two fields applied, or two separate audit events emitted for a single request.

**Current Status:** 🔴 Not written

---

### UC116-TC-003 — SYSTEM_ADMIN promotes a DIFFERENT target user to SYSTEM_ADMIN (legitimate elevation by a peer admin)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminRoleService.updateRole()`
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-IDENTITY-IMP-116 §16 Authorization Matrix — SYSTEM_ADMIN (other target): ✅`

**Preconditions:** FX-116-001 (admin caller A), FX-116-003 (target, distinct id, `role=MODERATOR`).

**Test Steps:**
1. Call `updateRole(adminA_Id, targetId, makeUpdateRoleRequest(r -> r.setNewRole(Role.SYSTEM_ADMIN)))` where `adminA_Id != targetId`.
2. Assert success — this is the ONE legitimate path to grant SYSTEM_ADMIN: an existing SYSTEM_ADMIN elevating a DIFFERENT user.
3. Assert audit payload `newRole=SYSTEM_ADMIN`, `createdByAdminId`-equivalent field `= adminA_Id`.

**Expected Result (PASS):** Legitimate cross-user elevation succeeds and is fully audited — this is the control case proving the guard blocks SELF-targeting specifically, not all SYSTEM_ADMIN grants.
**Expected Result (FAIL):** Elevation incorrectly blocked (over-broad guard), or under-audited.

**Current Status:** 🔴 Not written
**Implementation Note:** This test is the deliberate positive contrast to `UC116-TC-SEC-001` — do not conflate "self-target always blocked" with "SYSTEM_ADMIN grants always blocked."

---

### UC116-TC-004 — Audit payload always captures both previousRole and newRole

**Severity:** `HIGH`
**Feature Under Test:** `AdminRoleService.updateRole()` audit payload construction
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-IAM-008 Decision`, `CB-IDENTITY-IMP-116 §7.3 Payload Schema`

**Test Steps:**
1. Capture the payload argument passed to `AuditService.log()` via `ArgumentCaptor`.
2. Assert payload has non-null `previousRole` AND `newRole` fields matching the pre/post state exactly.

**Expected Result (PASS):** Both fields always populated, never null.
**Expected Result (FAIL):** `previousRole` omitted, making the audit trail unable to reconstruct history (breaks UC117 traceability).

**Current Status:** 🔴 Not written

---

### UC116-TC-005 — newRole missing or invalid enum value rejected with IAM-116-002

**Severity:** `LOW`
**Feature Under Test:** `UpdateUserRoleRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminRoleControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-IDENTITY-IMP-116 §8.1 @NotNull`, `§10 IAM-116-002`

**Test Steps:**
1. PATCH with `newRole=null` → assert HTTP 400 `IAM-116-002`.
2. PATCH with `newRole="NOT_A_REAL_ROLE"` (malformed enum string) → assert HTTP 400 `IAM-116-002`.

**Expected Result (PASS):** Both invalid shapes rejected before reaching the service.
**Expected Result (FAIL):** Null or garbage role value reaches the service layer.

**Current Status:** 🔴 Not written

---

### UC116-TC-006 — SYSTEM_ADMIN attempting to DEMOTE their own role is rejected (unconditional guard, self-demotion direction)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminRoleServiceImpl.assertNotSelfTarget()`
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-IAM-007 Decision — Option A, "unconditionally... any direction of change"`

**Preconditions:** FX-116-001 (admin caller, targeting own id), FX-116-006 (`newRole=MOTHER`, a DOWNGRADE).

**Test Steps:**
1. Call `updateRole(adminId, adminId, FX-116-006)` — caller attempts to demote themselves to `MOTHER`.
2. Assert `AccessDeniedBusinessException` with code `IAM-116-004`.
3. Assert `UserRepository.save()` and `AuditService.log()` NEVER invoked.

**Expected Result (PASS):** Guard rejects self-targeting regardless of whether the change is an upgrade or downgrade — proving it is NOT merely an "escalation" check but an unconditional identity check, per ADR-IAM-007's explicit contrast with UC114's weaker guard.
**Expected Result (FAIL):** Self-demotion is allowed (guard incorrectly implemented as escalation-only, contradicting ADR-IAM-007 Option A vs. Option B decision).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the test that distinguishes ADR-IAM-007 (Option A, chosen) from Option B (rejected) — it MUST fail identically to the escalation case in `UC116-TC-SEC-001`.

---

### UC116-TC-007 — Non-existent targetUserId returns 404 IAM-116-003

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminRoleService.updateRole()`
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-IDENTITY-IMP-116 §6.2 Error Path — Target not found`

**Test Steps:**
1. Mock `UserRepository.findById(unknownId)` → `Optional.empty()`.
2. Call `updateRole(adminId, unknownId, FX-116-004)` where `adminId != unknownId` (self-target guard must pass first).
3. Assert `ResourceNotFoundException` `IAM-116-003`.

**Expected Result (PASS):** 404-mapped exception, no save/audit call.
**Expected Result (FAIL):** NPE, or silent no-op.

**Current Status:** 🔴 Not written
**Implementation Note:** Verify the self-target guard (identity comparison) runs on IDs alone and does not require a DB lookup — so this not-found case must be reachable independently of the guard.

---

### UC116-TC-008 — reason field exceeding 500 chars is rejected

**Severity:** `LOW`
**Feature Under Test:** `UpdateUserRoleRequest` Bean Validation
**Test File:** `AdminRoleControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-IDENTITY-IMP-116 §8.1 @Size(max=500)`

**Test Steps:**
1. PATCH with `reason` = 501 chars → assert HTTP 400.
2. PATCH with `reason` = exactly 500 chars → assert HTTP 200 (boundary inclusive).

**Expected Result (PASS):** 501 rejected, 500 accepted.
**Expected Result (FAIL):** Boundary off-by-one in either direction.

**Current Status:** 🔴 Not written

---

### UC116-TC-011 — newRole equal to current role (no-op reassignment) — boundary behavior

**Severity:** `LOW`
**Feature Under Test:** `AdminRoleService.updateRole()`
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** Logic Issue L4 — TDS is silent on this boundary; documented here as an explicit Open item, not a silent assumption

**Preconditions:** Target with `role=MODERATOR`; request `newRole=MODERATOR` (identical).

**Test Steps:**
1. Call `updateRole(adminId, targetId, makeUpdateRoleRequest(r -> r.setNewRole(Role.MODERATOR)))`.
2. Assert the call succeeds (TDS does not forbid a no-op reassignment) and an audit event IS still emitted with `previousRole == newRole == MODERATOR`.

**Expected Result (PASS):** Consistent, documented behavior: no-op reassignments are allowed and still audited (safer default — an admin action is still recorded even if it changed nothing).
**Expected Result (FAIL):** Inconsistent behavior (e.g., silently skips the audit call for a no-op, creating an unaudited admin action) — flag this as a TDS ambiguity to confirm with Tech Lead before implementation, per Logic Issue L4.

**Current Status:** 🔴 Not written
**Implementation Note:** OPEN ITEM — if Tech Lead decides no-op reassignments should be rejected outright (e.g., as a distinct validation error) instead of accepted-and-audited, this test must be rewritten; do not silently pick a behavior without confirmation.

---

### UC116-TC-012 — Write surface touches ONLY users.role/locked, never a permission-flag table

**Severity:** `HIGH`
**Feature Under Test:** `AdminRoleService.updateRole()` persistence scope
**Test File:** `AdminRoleServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `RG-6 (§1.1 TDS)`, `ADR-IAM-001` (shared with UC114/115)

**Test Steps:**
1. Verify (via Mockito `verifyNoMoreInteractions` on all injected repository mocks beyond `UserRepository`) that `AdminRoleServiceImpl` has no dependency on any `PermissionRepository`/`RoleRepository`/`UserRoleRepository` type — constructor signature inspection.
2. Assert the only repository field type in `AdminRoleServiceImpl` is `UserRepository`.

**Expected Result (PASS):** Service class has no permission-flag-schema dependency at all.
**Expected Result (FAIL):** A `PermissionRepository` or similar hallucinated dependency is injected, violating RG-6.

**Current Status:** 🔴 Not written

---

### UC116-TC-013 — UpdateUserStatusRequest (UC114's DTO) has no role field — write-surface non-overlap proof

**Severity:** `MEDIUM`
**Feature Under Test:** Cross-UC contract consistency — `UpdateUserStatusRequest` (UC114) vs. `UpdateUserRoleRequest` (UC116)
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/contract/AdminGovernanceWriteSurfaceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-IDENTITY-IMP-116 §1.1 Scope Statement — "keep the two UCs' write surfaces non-overlapping"`

**Test Steps:**
1. Reflectively enumerate all declared fields on `com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest` (UC114).
2. Assert no field is named `role`/`newRole`, and no field is of type `Role`.
3. Reflectively enumerate fields on `UpdateUserRoleRequest` (UC116); assert no field is named `enabled` (UC114's exclusive field — `lockAccessRights` is a distinct, differently-named field and is explicitly permitted here per §8.1).

**Expected Result (PASS):** The two DTOs have non-overlapping mutation surfaces exactly as designed.
**Expected Result (FAIL):** UC114's status DTO gains a `role` field (or vice versa), creating two independently-authorized code paths that can mutate the same column — a maintainability/audit-trail hazard.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES — RELEASE-BLOCKING SELF-ESCALATION GATE

---

### UC116-TC-SEC-001 — SYSTEM_ADMIN attempting to change OWN role to SYSTEM_ADMIN (or any role) is unconditionally rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-269 — Improper Privilege Management`
**Legal:** `BR-RBAC`, CASE 2.0 mandatory constraint (task brief RG-4 — "the top self-escalation risk to eliminate")
**Feature Under Test:** `AdminRoleServiceImpl.assertNotSelfTarget()` + full controller pipeline
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/security/AdminRoleSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-116-001 (SYSTEM_ADMIN caller, targeting own id), FX-116-005 (`newRole=SYSTEM_ADMIN`, a no-op-looking "reaffirm my own admin role" attempt — included specifically because a naive implementation might special-case "same role" as harmless), Testcontainers DB.

**Test Steps (Attack Simulation):**
1. Authenticate as SYSTEM_ADMIN (FX-116-001).
2. `PATCH /api/v1/admin/users/{ownUserId}/role` with `{newRole: "SYSTEM_ADMIN"}`.
3. Assert HTTP 403 `IAM-116-004` — rejected even though the caller ALREADY holds SYSTEM_ADMIN and is "granting" themselves the role they already have.
4. Repeat with `{newRole: "CONTENT_ADMIN"}` (a genuine attempted self-escalation-adjacent change) targeting own id → assert 403 `IAM-116-004`.
5. Query DB directly: assert `users.role` for the caller's own row is UNCHANGED from its pre-test value in both cases.

**Expected Result (PASS = hệ thống an toàn):** ALL self-targeting role-change attempts rejected, with zero exception for "no-op" same-role requests — proving the guard checks identity, not the semantic effect of the change.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any self-targeting role PATCH succeeds — this is the single most severe finding possible in the entire 4-UC cluster (AP-CB-IAM-002), release-blocking.

**Current Status:** 🔴 Not written
**Implementation Note:** This test must run against the REAL Spring Security filter chain and REAL service — not mocked — to prove the actual guard, not an assumption about it.

---

### UC116-TC-SEC-002 — Non-SYSTEM_ADMIN caller cannot reach role-mutation logic at all

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-269 — Improper Privilege Management`
**Legal:** `BR-RBAC`, CASE 2.0 mandatory constraint
**Feature Under Test:** `AdminRoleController` (`@WebMvcTest` with mocked `AdminRoleService`)
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminRoleControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:** JWTs for every non-SYSTEM_ADMIN role: `MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, PARTNER` (FX-116-007 and equivalents).

**Test Steps (Attack Simulation):**
1. Parameterized over all six non-SYSTEM_ADMIN roles.
2. `MockMvc.perform(patch("/api/v1/admin/users/{id}/role", randomUUID).with(jwt of role).content(FX-116-004 as JSON))`.
3. Assert HTTP 403, `error.code == "IAM-116-001"`.
4. Assert `AdminRoleService.updateRole()` NEVER invoked (Mockito `verifyNoInteractions`).

**Expected Result (PASS = hệ thống an toàn):** All six roles rejected at `@PreAuthorize`; service never touched — structurally impossible for a MODERATOR/CONTENT_ADMIN to grant SYSTEM_ADMIN via this endpoint (AP-CB-IAM-003 guard).
**Expected Result (FAIL = lỗ hổng tồn tại):** Any non-SYSTEM_ADMIN role reaches the service layer.

**Current Status:** 🔴 Not written

---

### UC116-TC-SEC-003 — Injection attempt in reason field handled safely

**Severity:** `MEDIUM`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `AdminRoleService.updateRole()` + JPA parameter binding
**Test File:** `AdminRoleSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:** FX-116-008 (`reason` = SQL injection payload).

**Test Steps (Attack Simulation):**
1. Authenticate as SYSTEM_ADMIN, target a DIFFERENT user (not self).
2. PATCH with `reason` set to FX-116-008.
3. Assert HTTP 200 (payload treated as literal text) and no SQL error; assert `audit_logs.new_value_json` stores the literal string without executing it.

**Expected Result (PASS = hệ thống an toàn):** Parameterized binding neutralizes the payload.
**Expected Result (FAIL = lỗ hổng tồn tại):** 500 error exposing query structure, or unexpected DB mutation beyond the intended row.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC116-TC-INT-001 — Full role-reassignment round trip against real DB with audit verification

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Service -> Repository -> PostgreSQL (Testcontainers)`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminRoleControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied automatically.
- Seed FX-116-001 (admin caller A) and FX-116-003 (target, `role=MODERATOR`) via JPA before each test (fresh instance per Props Isolation rule).

**Test Steps:**
1. Authenticate as admin caller A.
2. `PATCH /api/v1/admin/users/{targetId}/role` with `{newRole:"CONTENT_ADMIN", reason:"Integration test"}` → assert HTTP 200.
3. Query DB directly: assert `users.role = 'CONTENT_ADMIN'` for the target row.
4. Query `audit_logs`: assert exactly one row `action='ROLE_PERMISSION_UPDATED'`, `entity_id=targetId`, `actor_id=adminA_Id`, payload contains `previousRole=MODERATOR` and `newRole=CONTENT_ADMIN`.
5. Attempt a self-targeting PATCH as admin caller A against their own id → assert HTTP 403 and assert `users.role` for admin A's own row is unchanged in the DB.

**Expected Result (PASS):** DB state and audit trail consistent for the legitimate mutation; self-targeting attempt leaves zero DB trace of a role change.
**Expected Result (FAIL):** Mutation applied without matching audit row, or self-targeting attempt partially succeeds.

**DB Assertion:**
```java
User updated = userRepository.findById(targetId).orElseThrow();
assertThat(updated.getRole()).isEqualTo(Role.CONTENT_ADMIN);
List<AuditLog> logs = auditLogRepository.findByEntityIdAndAction(targetId, AuditAction.ROLE_PERMISSION_UPDATED);
assertThat(logs).hasSize(1);

User adminASelf = userRepository.findById(adminAId).orElseThrow();
assertThat(adminASelf.getRole()).isEqualTo(Role.SYSTEM_ADMIN); // unchanged after failed self-target attempt
```

**Current Status:** 🔴 Not written

---

### UC116-WEB-TC-001 — RoleAssignmentModal disables submission when target equals the current admin session user

**Severity:** `HIGH`
**Feature Under Test:** `RoleAssignmentModal.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminGovernance/components/RoleAssignmentModal.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-IAM-007` (UI-level defense-in-depth mirroring the unconditional backend guard)

**Test Steps:**
1. Render `<RoleAssignmentModal targetUserId={sessionUserId} />` where `targetUserId` equals the authenticated admin's own id.
2. Assert the role selector and/or submit button is rendered `disabled`, with an explanatory message (e.g., "You cannot change your own role").

**Expected Result (PASS):** Self-targeting is prevented at the UI layer before any request is sent.
**Expected Result (FAIL):** Modal allows submission for the caller's own row, relying solely on the backend 403.

**Current Status:** 🔴 Not written

---

### UC116-WEB-TC-002 — RoleAssignmentModal renders confirmation after a successful role change

**Severity:** `LOW`
**Feature Under Test:** `RoleAssignmentModal.tsx`
**Test File:** `RoleAssignmentModal.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Mock API returning 200 happy-path fixture for a different target user.
2. Submit the form.
3. Assert a success confirmation renders with the new role displayed.

**Expected Result (PASS):** Confirmation shown with correct new role value.
**Expected Result (FAIL):** No feedback, or stale role displayed.

**Current Status:** 🔴 Not written

---

### UC116-WEB-TC-003 — RoleAssignmentModal renders 403 error UI (self-escalation or non-admin)

**Severity:** `MEDIUM`
**Feature Under Test:** `RoleAssignmentModal.tsx`
**Test File:** `RoleAssignmentModal.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Test Steps:**
1. Mock API returning 403 `IAM-116-004`.
2. Submit form (simulating a bypass of the client-side self-target guard, e.g., stale session state).
3. Assert an error message renders, not a crash or silent failure.

**Expected Result (PASS):** Graceful, explicit error UI even if the client-side guard (UC116-WEB-TC-001) is somehow bypassed — defense in depth.
**Expected Result (FAIL):** Unhandled promise rejection.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC116-TC-001` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-002` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-003` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-004` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-005` | `AdminRoleControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-006` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-007` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-008` | `AdminRoleControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-011` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-012` | `AdminRoleServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-013` | `AdminGovernanceWriteSurfaceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-SEC-001` | `AdminRoleSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-SEC-002` | `AdminRoleControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-SEC-003` | `AdminRoleSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-TC-INT-001` | `AdminRoleControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC116-WEB-TC-001` | `RoleAssignmentModal.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC116-WEB-TC-002` | `RoleAssignmentModal.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC116-WEB-TC-003` | `RoleAssignmentModal.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AdminRoleServiceImpl implements AdminRoleService {

    @Override
    public UserRoleResponse updateRole(UUID callerUserId, UUID targetUserId, UpdateUserRoleRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC116-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC116-TC-006` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC116-TC-SEC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC116-TC-SEC-002` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC116-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-IDENTITY-IMP-116` approved (currently Draft, ADR-IAM-007/008 Accepted pending Principal Architect confirmation)
- [ ] Logic Issues (§2, L1-L4) confirmed with Tech Lead — **L4 (no-op reassignment behavior) is an explicit open question requiring sign-off before `UC116-TC-011` can move past Red phase**
- [ ] No migration required (§5.2 of TDS) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories, including a PEER admin fixture (FX-116-002) distinct from the primary caller

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green
- [ ] Test coverage ≥ 80% lines for `AdminRoleServiceImpl`
- [ ] No business logic in `AdminRoleController` (validation + mapping only)
- [ ] No permission-flag-schema dependency introduced (`UC116-TC-012` green)
- [ ] `UC116-TC-SEC-001` (unconditional self-target block, both directions) AND `UC116-TC-SEC-002` (non-admin caller rejection) BOTH green — **mandatory, release-blocking gate, not optional**
- [ ] `UC116-TC-006` (self-demotion variant) green — proves the guard is unconditional, not escalation-only

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `AdminGovernanceTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet confirmed present in `CareBridgeWebApp/package.json` — UC116-WEB-TC-* suspended until verified/added
- `UC116-TC-011` (no-op reassignment boundary, Logic Issue L4) suspended pending explicit Tech Lead confirmation of desired behavior — not to be silently resolved by the implementer

---

## 7. Rollback Plan

```bash
# No migration to revert — UC116 introduces no schema change (§5.2 TDS).

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminGovernance/components/RoleAssignmentModal*

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `roles`/`user_roles` table usage without new ADR | ☑ Không phát hiện — UC116-TC-012 explicitly guards ADR-IAM-001/RG-6 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — controller tests (TC-005, TC-008, SEC-002) only assert RBAC/validation; business logic asserted only in Service tests | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import a `PermissionRepository`/`PermissionFlag` entity not defined in this TDS | ☑ Không phát hiện — UC116-TC-012 proves no such dependency exists | G-3 |
| AP-CB-IAM-002 | **Self-Role-Escalation** | Any code path lets `targetUserId == callerUserId` succeed for a role change, in ANY direction | ☑ Explicitly covered — `UC116-TC-SEC-001` (escalation-flavored self-target) AND `UC116-TC-006` (demotion-flavored self-target) both dedicated, mandatory, release-blocking | **Release-blocking** |
| AP-CB-IAM-003 | **SYSTEM_ADMIN Grant By Non-SYSTEM_ADMIN** | Any role other than `SYSTEM_ADMIN` can reach `updateRole()` | ☑ Explicitly covered — `UC116-TC-SEC-002` parameterizes all six non-admin roles against the controller | **Release-blocking** |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)
- [x] Cả hai self-escalation anti-pattern (AP-CB-IAM-002, AP-CB-IAM-003) có test case chuyên biệt, đánh dấu release-blocking rõ ràng
- [x] Self-target guard đã được kiểm chứng ở CẢ HAI hướng (escalation và demotion) — điểm khác biệt then chốt so với ADR-IAM-003 của UC114

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
