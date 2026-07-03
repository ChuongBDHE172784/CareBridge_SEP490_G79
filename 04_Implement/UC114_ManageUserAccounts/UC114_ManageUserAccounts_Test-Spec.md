# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC114 — Manage User Accounts — Test Specification

**Document ID:** `CB-IDENTITY-TDD-114`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`users` L532-544, `audit_logs`)
- `04_Implement/UC114_ManageUserAccounts/UC114_ManageUserAccounts_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16 of it)
- `04_Implement/UC115_CreateStaffAccount/UC115_CreateStaffAccount_Test-Spec.md`, `04_Implement/UC116_UpdateRoleAndPermission/UC116_UpdateRoleAndPermission_Test-Spec.md` — sibling Admin Governance cluster specs (shared `AdminGovernanceTestFactory` naming convention)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.16 — UC-114 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC114 |

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
| **Feature / Gap ID** | `GAP-UC114` |
| **Module** | `Identity — Admin User Account Management (Admin Governance cluster)` |
| **Spec gốc** | `CB-IDENTITY-IMP-114` |
| **Priority** | 🔴 P0 (PII bulk exposure + admin authorization surface) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Milestone** | Admin Portal governance tabs |
| **Data Classification** | `PII` (email, phone, full_name of every platform user) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC` |
| **Upstream Dependencies** | `security.entity.User`, `security.repository.UserRepository`, `security.rbac.Role`, `audit.service.AuditService` (existing, reused) |
| **Downstream Consumers** | UC115 (created staff accounts appear in this list), UC116 (acts on rows this UC displays), UC117 (reads `USER_ACCOUNT_STATUS_CHANGED` audit events) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-IDENTITY-IMP-114 §17` (ADR-IAM-001, ADR-IAM-002, ADR-IAM-003) |
| **Constraints Injected** | Read/write ONLY existing `User` fields; never touch `roles`/`user_roles`; never expose `passwordHash`; every status mutation audited in same transaction; caller identity from `Principal` only; self-target guard mandatory |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS text ("displays, searches, filters, and manages user account status") does not name a concrete mutation model | TDS §1.3 scopes "manage status" strictly to `users.enabled`/`users.locked` boolean toggles — no new status enum | Tests assert `UpdateUserStatusRequest` has exactly `enabled`/`locked`/`reason` fields; no test exercises a hard-delete path (out of scope, TDS §1.3) |
| L2 | Schema physically has `roles`/`user_roles` tables suggesting many-to-many RBAC | Zero JPA entities/repositories reference them; only `users.role` enum column is live (ADR-IAM-001) | All fixtures build a single `Role` enum value on `User`; a dedicated test (UC114-TC-013) asserts the search query never references a `roles`/`user_roles` join |
| L3 | No SRS text addresses admin self-targeting | TDS §3 ADR-IAM-003 introduces a defensive self-protection guard as a non-invented engineering decision | UC114-TC-006/TC-007 cover both self-enable and self-disable/lock attempts, both rejected with `IAM-114-004` |
| L4 | `audit_logs_action_check` DB CHECK constraint (V1 L41) is a legacy list that already excludes several live `AuditAction` values | TDS §5.2 confirms this is pre-existing precedent, not a new risk; `USER_ACCOUNT_STATUS_CHANGED` follows the same pattern | Integration test (UC114-TC-INT-001) asserts the audit insert succeeds against the real Testcontainers schema despite the legacy CHECK gap, proving the precedent holds |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Identity.Admin.User module bao gồm các layer:
├── Domain (self-target guard — pure logic, unit-tested via AdminUserServiceImplTest)
├── Services (AdminUserServiceImpl — mock UserRepository/AuditService với Mockito)
├── Controller (AdminUserController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real users/audit_logs rows)
└── Web Frontend (React Testing Library + Vitest — AdminUserListPage, UserStatusToggleModal)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-114 §3.2.2.16` | Happy path search/filter, status toggle, generic exception template (E1-E3) |
| `ADR-IAM-001` | Reads/writes ONLY `users.role`/`enabled`/`locked`; never `roles`/`user_roles` |
| `ADR-IAM-002` | Every status mutation synchronously audited via `AuditService`, same transaction |
| `ADR-IAM-003` | Admin cannot change own account status — self-target guard |
| `BR-RBAC` | Only `SYSTEM_ADMIN` may call any endpoint in this UC |
| `V1__init_schema.sql` | Exact `users` column names/types driving fixtures and DTO-leak boundary tests |
| `CB-IDENTITY-IMP-114 §8/§9/§10/§16` | Service/repository contracts, API shapes, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | SYSTEM_ADMIN searches/filters users by role/enabled/locked | `AdminUserService.searchUsers()` | `UC114-TC-001` |
| TC-COND-002 | Search response never leaks `passwordHash` | `AdminUserMapper.toSummary()` | `UC114-TC-002` |
| TC-COND-003 | SYSTEM_ADMIN disables a target user's account | `AdminUserService.updateStatus()` | `UC114-TC-003` |
| TC-COND-004 | SYSTEM_ADMIN locks a target user's account | `AdminUserService.updateStatus()` | `UC114-TC-004` |
| TC-COND-005 | SYSTEM_ADMIN re-enables/unlocks a target account | `AdminUserService.updateStatus()` | `UC114-TC-005` |
| TC-COND-006 | Admin attempts to disable their own account | Self-target guard (ADR-IAM-003) | `UC114-TC-006` |
| TC-COND-007 | Admin attempts to lock their own account | Self-target guard (ADR-IAM-003) | `UC114-TC-007` |
| TC-COND-008 | Non-SYSTEM_ADMIN role attempts search endpoint | Controller `@PreAuthorize` | `UC114-TC-008` |
| TC-COND-009 | Non-SYSTEM_ADMIN role attempts status-update endpoint | Controller `@PreAuthorize` | `UC114-TC-009` |
| TC-COND-010 | `targetUserId` does not exist | `AdminUserService.updateStatus()` not-found guard | `UC114-TC-010` |
| TC-COND-011 | `reason` exceeds 500 chars | Bean Validation | `UC114-TC-011` |
| TC-COND-012 | Both `enabled`/`locked` omitted in request | Bean Validation / service guard | `UC114-TC-012` |
| TC-COND-013 | Search query never joins `roles`/`user_roles` tables (ADR-IAM-001) | `UserRepository.search()` | `UC114-TC-013` |
| TC-COND-014 | SQL/JSON injection attempt in `email`/`name` filter params | `UserRepository.search()` parameterized JPQL | `UC114-TC-SEC-001` |
| TC-COND-015 | Full search + status-update round trip against real DB, audit row verified | Full flow | `UC114-TC-INT-001` |
| TC-COND-016 | Pagination caps at `AppConstants.MAX_PAGE_SIZE = 100` | Controller/Service pagination bound | `UC114-TC-014` |
| TC-COND-017 | Web: AdminUserListPage renders paginated table on successful fetch | React component | `UC114-WEB-TC-001` |
| TC-COND-018 | Web: AdminUserListPage disables self-row's status toggle control | React component | `UC114-WEB-TC-002` |
| TC-COND-019 | Web: AdminUserListPage renders 403 access-denied UI | React component | `UC114-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role partitions (SYSTEM_ADMIN vs. every other role) | Binary authorization outcome per §16 matrix |
| Boundary Value Analysis | `reason` length (500/501 chars), page `size` (100/101) | Schema/DTO-driven boundaries from §8/§4.4 |
| State Transition Testing | `users.enabled`/`locked` ACTIVE↔DISABLED↔LOCKED (TDS §6.4 state machine) | Explicit FSM documented in TDS |
| Error Guessing | Self-target IDOR-style guard bypass; SQL injection in filter params; DTO password leak | Highest-risk surfaces named in TDS §4.3/§10 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-114-001` | DB seed | Admin caller: `User{role:SYSTEM_ADMIN, enabled:true, locked:false}` | Caller identity for happy-path/self-target tests |
| `FX-114-002` | DB seed | Target Mother: `User{role:MOTHER, enabled:true, locked:false}` | Status-toggle target |
| `FX-114-003` | DB seed | 5 additional `User` rows spanning all `Role` values, mixed `enabled`/`locked` | Search/filter coverage |
| `FX-114-004` | JWT | `{ sub: admin-user-id, role: 'SYSTEM_ADMIN' }` | Auth context for admin requests |
| `FX-114-005` | JWT | `{ sub: moderator-user-id, role: 'MODERATOR' }` | Non-admin rejection |
| `FX-114-006` | Request | `UpdateUserStatusRequest{enabled:false, reason:"a".repeat(501)}` | Boundary — reason too long |
| `FX-114-007` | Request | `email="'; DROP TABLE users; --"` filter param | SQL injection attempt |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Shared across the Admin Governance cluster (UC114/UC115/UC116) as `AdminGovernanceTestFactory`, extended per-file with UC-specific `makeXxx()` methods. This file defines the UC114-relevant subset.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// AdminGovernanceTestFactory.java — shared factory across UC114/UC115/UC116
// mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class AdminGovernanceTestFactory {

    static User makeSystemAdmin() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("admin+" + UUID.randomUUID() + "@carebridge.dev");
        u.setPhone("+84900000001");
        u.setName("Test System Admin");
        u.setRole(Role.SYSTEM_ADMIN);
        u.setEnabled(true);
        u.setLocked(false);
        u.setPasswordHash("$2a$10$stubHashNeverAsserted");
        u.setCreatedAt(Instant.now());
        return u;
    }

    static User makeSystemAdmin(Consumer<User> overrides) {
        User u = makeSystemAdmin();
        overrides.accept(u);
        return u;
    }

    static User makeUser(Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user+" + UUID.randomUUID() + "@carebridge.dev");
        u.setPhone("+84900000002");
        u.setName("Test " + role.name());
        u.setRole(role);
        u.setEnabled(true);
        u.setLocked(false);
        u.setPasswordHash("$2a$10$stubHashNeverAsserted");
        u.setCreatedAt(Instant.now());
        return u;
    }

    static User makeUser(Role role, Consumer<User> overrides) {
        User u = makeUser(role);
        overrides.accept(u);
        return u;
    }

    // UC114-specific: status mutation request
    static UpdateUserStatusRequest makeStatusRequest(Consumer<UpdateUserStatusRequest> overrides) {
        UpdateUserStatusRequest r = new UpdateUserStatusRequest();
        r.setEnabled(false);
        r.setLocked(null);
        r.setReason("Suspected policy violation — pending review");
        overrides.accept(r);
        return r;
    }

    static AdminUserSearchQuery makeSearchQuery(Consumer<AdminUserSearchQuery> overrides) {
        AdminUserSearchQuery q = new AdminUserSearchQuery();
        overrides.accept(q);
        return q;
    }
}
```

---

### UC114-TC-001 — SYSTEM_ADMIN searches/filters users and receives correct paginated DTOs

**Severity:** `HIGH`
**Feature Under Test:** `AdminUserService.searchUsers()`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/service/AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-IDENTITY-IMP-114 §6.1 Sequence Diagram — Search Users`, `§8.1 Service Interface`

**Preconditions:** FX-114-003 (6 users spanning all roles, mixed status flags).

**Test Steps:**
1. Mock `UserRepository.search(email, phone, name, role, enabled, locked, pageable)` to return a `Page<User>` matching `role=MOTHER, enabled=true`.
2. Call `adminUserService.searchUsers(AdminGovernanceTestFactory.makeSearchQuery(q -> { q.setRole(Role.MOTHER); q.setEnabled(true); }), pageable)`.
3. Assert returned `Page<AdminUserSummaryResponse>` matches the mocked entities field-for-field (id, email, phone, name, role, enabled, locked, createdAt).

**Expected Result (PASS):** DTO fields match source `User` entities exactly; repository invoked with the exact filter values passed in.
**Expected Result (FAIL):** Wrong filter forwarded to repository, or DTO mapping drops/mismatches a field.

**Current Status:** 🔴 Not written
**Implementation Note:** Service must delegate filtering entirely to `UserRepository.search()` — no in-memory re-filtering.

---

### UC114-TC-002 — Search response DTO never exposes passwordHash

**Severity:** `CRITICAL`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Feature Under Test:** `AdminUserMapper.toSummary()`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/mapper/AdminUserMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-IDENTITY-IMP-114 §4.3 Security — PII exposure`, `§17 C2`

**Preconditions:** `AdminGovernanceTestFactory.makeUser(Role.MOTHER)` with a non-null `passwordHash`.

**Test Steps:**
1. Map the entity via `AdminUserMapper.toSummary(user)`.
2. Reflectively enumerate all fields of the resulting `AdminUserSummaryResponse` class.
3. Assert no field name/type corresponds to `passwordHash`, and no getter returns the entity's hash value under any field name.

**Expected Result (PASS):** `AdminUserSummaryResponse` has no `passwordHash`-shaped field; reflection scan finds zero match.
**Expected Result (FAIL):** DTO class declares a password-related field, or a generic `Map<String,Object>` serialization path leaks it.

**Current Status:** 🔴 Not written

---

### UC114-TC-003 — SYSTEM_ADMIN disables a target user's account and audit event is emitted

**Severity:** `HIGH`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-IDENTITY-IMP-114 §6.2 Sequence Diagram — Update Status`, `ADR-IAM-002`

**Preconditions:** FX-114-001 (caller admin), FX-114-002 (target Mother, `enabled=true`).

**Test Steps:**
1. Mock `UserRepository.findById(targetId)` to return the target user.
2. Call `updateStatus(adminId, targetId, makeStatusRequest(r -> r.setEnabled(false)))`.
3. Assert `UserRepository.save()` invoked with `enabled=false`.
4. Assert `AuditService.log(AuditAction.USER_ACCOUNT_STATUS_CHANGED, adminId, "USER", targetId, payload)` invoked exactly once with `previousEnabled=true, newEnabled=false`.

**Expected Result (PASS):** Both repository save and audit log call happen; response DTO reflects `enabled=false`.
**Expected Result (FAIL):** Audit call missing/wrong payload, or mutation applied without corresponding audit event.

**Current Status:** 🔴 Not written
**Implementation Note:** Both calls must occur inside the same `@Transactional` boundary per ADR-IAM-002.

---

### UC114-TC-004 — SYSTEM_ADMIN locks a target user's account

**Severity:** `HIGH`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-IDENTITY-IMP-114 §6.4 State Machine`

**Preconditions:** FX-114-001, FX-114-002 (`locked=false`).

**Test Steps:**
1. Call `updateStatus(adminId, targetId, makeStatusRequest(r -> { r.setEnabled(null); r.setLocked(true); }))`.
2. Assert saved entity has `locked=true`, `lockedAt` populated, `enabled` unchanged (nullable field means "leave unchanged" per §8.1 DTO contract).

**Expected Result (PASS):** Only `locked`/`lockedAt` mutated; `enabled` untouched.
**Expected Result (FAIL):** `enabled` incorrectly overwritten to `false`/`null` when omitted from request.

**Current Status:** 🔴 Not written

---

### UC114-TC-005 — SYSTEM_ADMIN re-enables and unlocks a previously disabled/locked account

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-IDENTITY-IMP-114 §6.4 State Machine — DISABLED→ACTIVE, LOCKED→ACTIVE`

**Preconditions:** Target user with `enabled=false, locked=true`.

**Test Steps:**
1. Call `updateStatus(adminId, targetId, makeStatusRequest(r -> { r.setEnabled(true); r.setLocked(false); }))`.
2. Assert saved entity `enabled=true, locked=false`.
3. Assert audit payload captures `previousEnabled=false, newEnabled=true, previousLocked=true, newLocked=false`.

**Expected Result (PASS):** Full before/after state captured correctly.
**Expected Result (FAIL):** Partial state transition, or audit payload omits one of the two flags.

**Current Status:** 🔴 Not written

---

### UC114-TC-006 — Admin attempting to disable their own account is rejected (self-target guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-IAM-003`, `CB-IDENTITY-IMP-114 §10 IAM-114-004`

**Preconditions:** FX-114-001 (admin caller and target are the same user id).

**Test Steps:**
1. Call `updateStatus(adminId, adminId, makeStatusRequest(r -> r.setEnabled(false)))`.
2. Assert `AccessDeniedBusinessException` thrown with code `IAM-114-004`.
3. Verify `UserRepository.save()` and `AuditService.log()` were NEVER invoked (Mockito `verifyNoInteractions`/`verify(times(0))`).

**Expected Result (PASS):** Guard fires before any mutation or audit call.
**Expected Result (FAIL):** Mutation applied, or exception thrown only after a partial DB write.

**Current Status:** 🔴 Not written
**Implementation Note:** Guard must run before `findById`/`save` to avoid any partial side effect.

---

### UC114-TC-007 — Admin attempting to lock their own account is rejected (self-target guard, lock variant)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-IAM-003`

**Preconditions:** FX-114-001.

**Test Steps:**
1. Call `updateStatus(adminId, adminId, makeStatusRequest(r -> { r.setEnabled(null); r.setLocked(true); }))`.
2. Assert `IAM-114-004` thrown regardless of which flag was targeted.

**Expected Result (PASS):** Guard is field-agnostic — blocks self-target for any status field combination.
**Expected Result (FAIL):** Guard only checks `enabled` and allows self-lock through `locked`.

**Current Status:** 🔴 Not written

---

### UC114-TC-008 — Non-SYSTEM_ADMIN role is rejected at the search endpoint

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminUserController` (`@WebMvcTest` with mocked `AdminUserService`)
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminUserControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-IDENTITY-IMP-114 §16 Authorization Matrix`

**Preconditions:** FX-114-005 JWT (role=MODERATOR); parameterized across `MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, PARTNER`.

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/users").with(jwt of each non-admin role))`.
2. Assert HTTP 403, body `error.code == "IAM-114-001"`.
3. Assert `AdminUserService` never invoked.

**Expected Result (PASS):** 403 for all six non-admin roles.
**Expected Result (FAIL):** Any non-admin role receives 200.

**Current Status:** 🔴 Not written

---

### UC114-TC-009 — Non-SYSTEM_ADMIN role is rejected at the status-update endpoint

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminUserController`
**Test File:** `AdminUserControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-IDENTITY-IMP-114 §16 Authorization Matrix`

**Preconditions:** FX-114-005 JWT.

**Test Steps:**
1. `MockMvc.perform(patch("/api/v1/admin/users/{id}/status", randomUUID).with(jwt MODERATOR).content(...))`.
2. Assert HTTP 403 `IAM-114-001`.

**Expected Result (PASS):** 403, service untouched.
**Expected Result (FAIL):** Mutation reaches service layer despite wrong role.

**Current Status:** 🔴 Not written

---

### UC114-TC-010 — Non-existent targetUserId returns 404 IAM-114-003

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminUserService.updateStatus()`
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-IDENTITY-IMP-114 §6.3 Error Path — Target not found`

**Preconditions:** `UserRepository.findById(unknownId)` mocked to return `Optional.empty()`.

**Test Steps:**
1. Call `updateStatus(adminId, unknownId, makeStatusRequest(r -> r.setEnabled(false)))`.
2. Assert `ResourceNotFoundException` with code `IAM-114-003`.

**Expected Result (PASS):** 404-mapped exception thrown, no save/audit call.
**Expected Result (FAIL):** NPE, or silent no-op success response.

**Current Status:** 🔴 Not written

---

### UC114-TC-011 — reason field exceeding 500 chars is rejected as validation error

**Severity:** `LOW`
**Feature Under Test:** `UpdateUserStatusRequest` Bean Validation
**Test File:** `AdminUserControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-IDENTITY-IMP-114 §8.1 @Size(max=500)`, `§10 IAM-114-002`

**Preconditions:** FX-114-006 (`reason` = 501 chars).

**Test Steps:**
1. `PATCH .../status` with FX-114-006 body, admin JWT.
2. Assert HTTP 400, `error.code == "IAM-114-002"`.
3. Repeat with exactly 500 chars → assert 200 (boundary inclusive).

**Expected Result (PASS):** 501 rejected, 500 accepted.
**Expected Result (FAIL):** 501 silently truncated/accepted, or 500 incorrectly rejected.

**Current Status:** 🔴 Not written

---

### UC114-TC-012 — Request with both enabled and locked omitted is rejected

**Severity:** `LOW`
**Feature Under Test:** `AdminUserService.updateStatus()` / Bean Validation
**Test File:** `AdminUserServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-IDENTITY-IMP-114 §10 IAM-114-002 trigger condition`

**Test Steps:**
1. Call `updateStatus(adminId, targetId, makeStatusRequest(r -> { r.setEnabled(null); r.setLocked(null); }))`.
2. Assert `ValidationException`/`IAM-114-002` — a no-op request is rejected rather than silently succeeding.

**Expected Result (PASS):** Explicit rejection; no ambiguous no-op audit event created.
**Expected Result (FAIL):** Silent success with an empty-diff audit log row.

**Current Status:** 🔴 Not written

---

### UC114-TC-013 — Search repository query never references roles/user_roles tables

**Severity:** `HIGH`
**Feature Under Test:** `UserRepository.search()` JPQL
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/repository/UserRepositorySearchTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-IAM-001`

**Test Steps:**
1. Inspect the `@Query` annotation string on `UserRepository.search()` via reflection or a compiled-query capture.
2. Assert the JPQL text contains no reference to `Role r` join entities or `user_roles`/`roles` table names — only `User u` is queried.

**Expected Result (PASS):** Query is single-entity (`User`) scoped.
**Expected Result (FAIL):** Query joins a `roles`/`user_roles` mapping, violating ADR-IAM-001.

**Current Status:** 🔴 Not written

---

### UC114-TC-014 — Page size above MAX_PAGE_SIZE is capped/rejected

**Severity:** `LOW`
**Feature Under Test:** `AdminUserController` pagination binding
**Test File:** `AdminUserControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CB-IDENTITY-IMP-114 §4.4 Scalability — AppConstants.MAX_PAGE_SIZE = 100`

**Test Steps:**
1. Request `GET /api/v1/admin/users?size=101`.
2. Assert either HTTP 400 or a silently-capped `size=100` response (per existing `AppConstants` pagination convention used elsewhere in the codebase) — behavior must be verified against the existing pagination utility, not invented.

**Expected Result (PASS):** Consistent with existing `AppConstants.MAX_PAGE_SIZE` enforcement pattern elsewhere in the codebase.
**Expected Result (FAIL):** Unbounded page size accepted, risking full-table PII dump in one response.

**Current Status:** 🔴 Not written
**Implementation Note:** Verify the exact existing enforcement mechanism (Spring `Pageable` max-size resolver) before asserting 400 vs. cap — Open item, confirm against `AppConstants` usage elsewhere.

---

### SECURITY TEST CASES

---

### UC114-TC-SEC-001 — SQL/JSON injection attempt in email/name filter params is neutralized

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Legal:** `BR-RBAC`, PDPA data-integrity requirement
**Feature Under Test:** `UserRepository.search()` parameterized JPQL
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/security/AdminUserSearchSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-114-003 seeded via Testcontainers; FX-114-007 malicious filter string.

**Test Steps (Attack Simulation):**
1. Authenticate as SYSTEM_ADMIN (FX-114-004).
2. `GET /api/v1/admin/users?email=' OR '1'='1` and `?name={"$ne":null}`.
3. Assert HTTP 200 with zero or exact-substring-match results only (no full-table dump), and no SQL error/500.

**Expected Result (PASS = hệ thống an toàn):** Parameterized JPQL treats the string as a literal `LIKE` pattern; no injection effect, no crash.
**Expected Result (FAIL = lỗ hổng tồn tại):** Full user table returned, or a 500 stack trace leaks query structure.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC114-TC-INT-001 — Full search + status-update round trip against real DB with audit row verification

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Service -> Repository -> PostgreSQL (Testcontainers)`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminUserControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied automatically.
- Seed FX-114-001 (admin) and FX-114-002 (target Mother) via JPA before each test (fresh instance per Props Isolation rule).

**Test Steps:**
1. Authenticate as admin (FX-114-004).
2. `GET /api/v1/admin/users?role=MOTHER` → assert target user appears, `passwordHash` absent from JSON.
3. `PATCH /api/v1/admin/users/{targetId}/status` with `{enabled:false, reason:"Integration test"}` → assert HTTP 200.
4. Query DB directly: assert `users.enabled = false` for the target row.
5. Query `audit_logs` directly: assert exactly one row with `action='USER_ACCOUNT_STATUS_CHANGED'`, `entity_id=targetId`, `actor_id=adminId`.

**Expected Result (PASS):** DB state and audit trail both reflect the mutation atomically.
**Expected Result (FAIL):** Mutation applied without a matching audit row, or vice versa (transactional atomicity violated).

**DB Assertion:**
```java
User updated = userRepository.findById(targetId).orElseThrow();
assertThat(updated.isEnabled()).isFalse();
List<AuditLog> logs = auditLogRepository.findByEntityIdAndAction(targetId, AuditAction.USER_ACCOUNT_STATUS_CHANGED);
assertThat(logs).hasSize(1);
```

**Current Status:** 🔴 Not written

---

### UC114-WEB-TC-001 — AdminUserListPage renders paginated table on successful fetch

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminUserListPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminGovernance/pages/AdminUserListPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Preconditions:** Mock `GET /api/v1/admin/users` returning happy-path fixture matching TDS §9.2.

**Test Steps:**
1. Render `<AdminUserListPage />` under a test harness with admin auth context.
2. `await screen.findByText(...)` for a seeded user's email.
3. Assert table renders one row per fixture item with role/status badges.

**Expected Result (PASS):** Table populated from fetched data, no `passwordHash` column present.
**Expected Result (FAIL):** Loading state stuck, or wrong data rendered.

**Current Status:** 🔴 Not written

---

### UC114-WEB-TC-002 — AdminUserListPage disables the status-toggle control on the caller's own row

**Severity:** `HIGH`
**Feature Under Test:** `AdminUserListPage.tsx` / `UserStatusToggleModal.tsx`
**Test File:** `AdminUserListPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-IAM-003` (UI-level defense-in-depth mirroring the backend guard)

**Test Steps:**
1. Mock fetch returning a list that includes the logged-in admin's own user row.
2. Render component with the admin's own id as the authenticated principal.
3. Assert the status-toggle button/control on that specific row is rendered `disabled` or omitted.

**Expected Result (PASS):** Self-row control is disabled; other rows remain interactive.
**Expected Result (FAIL):** Self-row control is clickable, relying solely on the backend 403 (defense-in-depth gap).

**Current Status:** 🔴 Not written

---

### UC114-WEB-TC-003 — AdminUserListPage renders access-denied UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminUserListPage.tsx`
**Test File:** `AdminUserListPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`

**Test Steps:**
1. Mock API returning 403 `IAM-114-001` error body.
2. Render component.
3. Assert an access-denied message renders, not a crash or blank screen.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC114-TC-001` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-002` | `AdminUserMapperTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-003` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-004` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-005` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-006` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-007` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-008` | `AdminUserControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-009` | `AdminUserControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-010` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-011` | `AdminUserControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-012` | `AdminUserServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-013` | `UserRepositorySearchTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-014` | `AdminUserControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-SEC-001` | `AdminUserSearchSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-TC-INT-001` | `AdminUserControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC114-WEB-TC-001` | `AdminUserListPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC114-WEB-TC-002` | `AdminUserListPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC114-WEB-TC-003` | `AdminUserListPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Override
    public Page<AdminUserSummaryResponse> searchUsers(AdminUserSearchQuery query, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AdminUserSummaryResponse updateStatus(UUID callerUserId, UUID targetUserId, UpdateUserStatusRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC114-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC114-TC-006` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC114-TC-SEC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC114-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-IDENTITY-IMP-114` approved (currently Draft, ADR-IAM-001/002/003 Accepted pending Principal Architect confirmation)
- [ ] Logic Issues (§2, L1-L4) confirmed with Tech Lead
- [ ] No migration required (§5.2 of TDS) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green
- [ ] Test coverage ≥ 80% lines for `AdminUserServiceImpl`, `AdminUserMapper`
- [ ] No business logic in `AdminUserController` (validation + mapping only)
- [ ] No PII (`passwordHash`) in logs or API responses at any level
- [ ] `UC114-TC-006`, `UC114-TC-007` (self-target guard) both green — mandatory gate

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `AdminGovernanceTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet confirmed present in `CareBridgeWebApp/package.json` — UC114-WEB-TC-* suspended until verified/added (see UC97 precedent, same open item)
- UC114-TC-014 (pagination cap enforcement mechanism) suspended pending confirmation of the exact existing `AppConstants`/`Pageable` enforcement pattern — flagged as Open, not silently assumed

---

## 7. Rollback Plan

```bash
# No migration to revert — UC114 introduces no schema change (§5.2 TDS).

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminGovernance/

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume `roles`/`user_roles` usage without new ADR | ☑ Không phát hiện — UC114-TC-013 explicitly guards ADR-IAM-001 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — controller tests (TC-008/009/011/014) only assert RBAC/validation, business logic asserted only in Service tests | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — all types match TDS §8 interfaces exactly | G-3 |
| AP-CB-IAM-001 | **Self-Lockout Gap** | Missing `targetUserId == callerUserId` guard coverage | ☑ Không phát hiện — UC114-TC-006/TC-007 cover both enable and lock self-target paths | **Release-blocking** |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
