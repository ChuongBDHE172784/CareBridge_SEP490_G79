# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC115 — Create Staff Account — Test Specification

**Document ID:** `CB-IDENTITY-TDD-115`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Implemented — 2026-07-04 (AdminStaffController/ServiceImplTest all PASS, verified independently)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`users` L532-544); plus new migration `V20260704090100__add_users_must_change_password.sql` (TDS §5.2)
- `04_Implement/UC115_CreateStaffAccount/UC115_CreateStaffAccount_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16 of it)
- `04_Implement/UC114_ManageUserAccounts/UC114_ManageUserAccounts_Test-Spec.md`, `04_Implement/UC116_UpdateRoleAndPermission/UC116_UpdateRoleAndPermission_Test-Spec.md` — sibling Admin Governance cluster specs (shared `AdminGovernanceTestFactory` naming convention)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.17 — UC-115 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

> ⚠️ **CRITICAL SECURITY GATE — This UC is the highest privilege-escalation risk in the Admin Governance cluster.** Tests `UC115-TC-SEC-001` and `UC115-TC-SEC-002` (self-escalation prevention) are **release-blocking**, not optional. No production deploy of this feature may proceed without both green.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC115 |
| `2026-07-04` | `AI Agent` | Approved by user — proceeding to implementation |

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
| **Feature / Gap ID** | `GAP-UC115` |
| **Module** | `Identity — Admin Staff Account Provisioning (Admin Governance cluster)` |
| **Spec gốc** | `CB-IDENTITY-IMP-115` |
| **Priority** | 🔴 P0 (privilege-escalation surface — highest risk item in cluster) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Milestone** | Admin Portal governance tabs |
| **Data Classification** | `PII` (new staff email/phone/full name) + `Credential Material` (temp password) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC` |
| **Upstream Dependencies** | `security.entity.User`, `security.rbac.Role`, `security.policy.PasswordComplexityPolicy`, `PasswordEncoder`, `security.service.EmailService`, `audit.service.AuditService` (existing, reused) |
| **Downstream Consumers** | UC114 (created staff accounts appear in admin user list), UC116 (role of a created staff account can later be updated), UC117 (staff-creation events audited) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-IDENTITY-IMP-115 §17` (ADR-IAM-004, ADR-IAM-005, ADR-IAM-006) |
| **Constraints Injected** | Endpoint `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` only; service-layer re-validation of `role ∈ {MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN}`; never accept admin-supplied password; always set `mustChangePassword=true`; roll back entire transaction on email delivery failure |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS text ("creates accounts according to permissions") does not name which caller role(s) may invoke this UC | ADR-IAM-004 resolves this: ONLY `SYSTEM_ADMIN` may call the endpoint at all — no role-hierarchy delegation exists in the codebase | UC115-TC-SEC-001/SEC-002 assert every non-SYSTEM_ADMIN role is rejected at the controller boundary before any service logic runs |
| L2 | `RegisterRequest.role` (self-registration DTO) is `@NotNull` but has no value-set constraint in the DTO itself | This UC creates a **separate** admin-only endpoint; self-registration's unconstrained role field is explicitly out of scope here, not fixed by this UC | Tests only exercise the new `POST /api/v1/admin/staff-accounts` endpoint; no test asserts changes to `RegisterRequest` |
| L3 | Schema has no flag distinguishing "must rotate temp password before first use" | TDS §5.2 introduces additive `users.must_change_password boolean NOT NULL DEFAULT false` via `V20260704090100` | Integration test seeds/queries this exact column; unit tests assert `mustChangePassword=true` is always set at creation, never left `false` |
| L4 | ADR-IAM-005 leaves login-flow enforcement of `mustChangePassword` as an explicitly flagged Open follow-up (not implemented by this UC) | Confirmed Open in TDS §3 ADR-IAM-005 Hệ quả | No test in this spec asserts login-flow rejection when `mustChangePassword=true` — flagged as Open item in §6 Suspension Criteria, not silently assumed complete |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Identity.Admin.Staff module bao gồm các layer:
├── Domain (assertStaffRole guard — pure logic, unit-tested via AdminStaffServiceImplTest)
├── Services (AdminStaffServiceImpl — mock UserRepository/PasswordEncoder/EmailService/AuditService với Mockito)
├── Controller (AdminStaffController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real users/audit_logs rows, migration V20260704090100 applied)
└── Web Frontend (React Testing Library + Vitest — CreateStaffAccountForm, StaffAccountCreatedConfirmation)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-115 §3.2.2.17` | Happy path creation, generic exception template (E1-E3) |
| `ADR-IAM-004` | ONLY SYSTEM_ADMIN reaches creation logic; service re-validates staff-role value set (defense in depth) |
| `ADR-IAM-005` | Temp password never admin-supplied; SecureRandom generation; `mustChangePassword=true` always set; email-failure rolls back transaction |
| `ADR-IAM-006` | Every creation synchronously audited, same transaction as insert |
| `BR-RBAC` | Only `SYSTEM_ADMIN` may call this endpoint |
| `V1__init_schema.sql` + `V20260704090100` | Exact `users` column set including new `must_change_password` |
| `CB-IDENTITY-IMP-115 §8/§9/§10/§16` | Service/repository contracts, API shapes, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | SYSTEM_ADMIN creates a MODERATOR account (happy path) | `AdminStaffService.createStaffAccount()` | `UC115-TC-001` |
| TC-COND-002 | SYSTEM_ADMIN creates a CONTENT_ADMIN account | `AdminStaffService.createStaffAccount()` | `UC115-TC-002` |
| TC-COND-003 | SYSTEM_ADMIN creates a SYSTEM_ADMIN account (only permitted caller) | `AdminStaffService.createStaffAccount()` | `UC115-TC-003` |
| TC-COND-004 | Temp password is SecureRandom-generated, never admin-supplied, never returned in response | `AdminStaffServiceImpl.generateTempPassword()` | `UC115-TC-004` |
| TC-COND-005 | `mustChangePassword=true` always set on creation | `AdminStaffServiceImpl.createStaffAccount()` | `UC115-TC-005` |
| TC-COND-006 | `role=MOTHER` (non-staff role) rejected with IAM-115-005 | `assertStaffRole()` | `UC115-TC-006` |
| TC-COND-007 | `role=FAMILY`/`EXPERT`/`PARTNER` rejected with IAM-115-005 | `assertStaffRole()` | `UC115-TC-007` |
| TC-COND-008 | Duplicate email rejected with IAM-115-002 (409) | `existsByEmail` guard | `UC115-TC-008` |
| TC-COND-009 | Duplicate phone rejected with IAM-115-002 (409) | `existsByPhone` guard | `UC115-TC-009` |
| TC-COND-010 | Non-SYSTEM_ADMIN caller (MODERATOR/CONTENT_ADMIN/etc.) attempts creation — structural self-escalation prevention | Controller `@PreAuthorize` | `UC115-TC-SEC-001` |
| TC-COND-011 | MODERATOR/CONTENT_ADMIN JWT attempts to create a SYSTEM_ADMIN account specifically | Controller `@PreAuthorize` (defense in depth with TC-COND-010) | `UC115-TC-SEC-002` |
| TC-COND-012 | `EmailService` delivery fails — entire transaction rolled back, no orphaned account | `AdminStaffServiceImpl` transactional boundary | `UC115-TC-010` |
| TC-COND-013 | Missing/invalid `email`/`name` rejected with IAM-115-003 | Bean Validation | `UC115-TC-011` |
| TC-COND-014 | Generated temp password satisfies existing `PasswordComplexityPolicy` | `generateTempPassword()` | `UC115-TC-012` |
| TC-COND-015 | Password never appears in application logs | `AdminStaffServiceImpl` / `EmailService` call | `UC115-TC-013` |
| TC-COND-016 | Full creation round trip against real DB — `users` row + `audit_logs` row verified | Full flow | `UC115-TC-INT-001` |
| TC-COND-017 | Injection attempt in `name`/`email` fields handled safely | Bean Validation / JPA parameter binding | `UC115-TC-SEC-003` |
| TC-COND-018 | Web: CreateStaffAccountForm submits and shows confirmation without displaying a password | React component | `UC115-WEB-TC-001` |
| TC-COND-019 | Web: CreateStaffAccountForm rejects role=MOTHER at the client validation layer (defense in depth) | React component | `UC115-WEB-TC-002` |
| TC-COND-020 | Web: CreateStaffAccountForm renders 403 error UI for non-admin session | React component | `UC115-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Staff-role partition (`MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN`) vs. non-staff-role partition (`MOTHER/FAMILY/EXPERT/PARTNER`) | ADR-IAM-004 defense-in-depth check operates on this exact partition |
| Boundary Value Analysis | Password complexity boundary (min length/charset per `PasswordComplexityPolicy`) | Reused existing policy class — boundary inherited, not invented |
| State Transition Testing | Account lifecycle `PROVISIONED → ACTIVE` / `PROVISIONED → LOCKED` (TDS §6.3 state machine) | Explicit FSM documented in TDS |
| Error Guessing | Self-escalation via role param tampering; email-delivery-failure orphan-account risk; password leak in logs/response | Highest-severity risks named explicitly in TDS §3/§4.3/§10 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-115-001` | DB seed | Admin caller: `User{role:SYSTEM_ADMIN, enabled:true}` (via `AdminGovernanceTestFactory.makeSystemAdmin()`) | Caller identity |
| `FX-115-002` | Request | `CreateStaffAccountRequest{email:"new.mod@carebridge.dev", phone:"+84901112222", name:"Tran Van B", role:MODERATOR}` | Happy path |
| `FX-115-003` | Request | Same as FX-115-002 but `role:SYSTEM_ADMIN` | Highest-privilege creation path |
| `FX-115-004` | Request | Same as FX-115-002 but `role:MOTHER` | Non-staff-role rejection |
| `FX-115-005` | JWT | `{ sub: moderator-user-id, role: 'MODERATOR' }` | Self-escalation attempt caller |
| `FX-115-006` | JWT | `{ sub: content-admin-user-id, role: 'CONTENT_ADMIN' }` | Self-escalation attempt caller (second role) |
| `FX-115-007` | DB seed | Existing user with `email="existing@carebridge.dev"` | Duplicate-email conflict |
| `FX-115-008` | Mock | `EmailService.send(...)` throws `MailSendException` | Rollback-on-failure verification |
| `FX-115-009` | Request | `name="<script>alert(1)</script>"`, `email="'; DROP TABLE users; --@x.com"` | Injection attempt |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Extends the shared `AdminGovernanceTestFactory` (see UC114 Test-Spec §4) with UC115-specific `makeXxx()` methods for staff-account creation.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// AdminGovernanceTestFactory.java — shared factory across UC114/UC115/UC116
// This block extends the factory with UC115-specific methods.
// mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class AdminGovernanceTestFactory {

    // --- Inherited from UC114 Test-Spec: makeSystemAdmin(), makeUser(Role) ---

    static CreateStaffAccountRequest makeCreateStaffRequest(Consumer<CreateStaffAccountRequest> overrides) {
        CreateStaffAccountRequest r = new CreateStaffAccountRequest();
        r.setEmail("new.staff+" + UUID.randomUUID() + "@carebridge.dev");
        r.setPhone("+84901112222");
        r.setName("Test Staff Member");
        r.setRole(Role.MODERATOR);
        overrides.accept(r);
        return r;
    }

    static String makeCompliantTempPassword() {
        // Mirrors PasswordComplexityPolicy minimums — never used as a real assertion oracle,
        // only as a stand-in when a test needs a syntactically valid password string.
        return "Tmp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "1a";
    }
}
```

---

### UC115-TC-001 — SYSTEM_ADMIN creates a MODERATOR account (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/service/AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-IDENTITY-IMP-115 §6.1 Sequence Diagram — Happy Path`, `§8.1 Service Interface`

**Preconditions:** FX-115-001 (admin caller), FX-115-002 (request).

**Test Steps:**
1. Mock `UserRepository.existsByEmail()`/`existsByPhone()` → `false`.
2. Mock `PasswordEncoder.encode()` to return a deterministic hash string.
3. Call `adminStaffService.createStaffAccount(adminId, FX-115-002)`.
4. Assert `UserRepository.save()` invoked with `role=MODERATOR, enabled=true, mustChangePassword=true`.
5. Assert `EmailService.send(...)` invoked with the target email.
6. Assert `AuditService.log(AuditAction.STAFF_ACCOUNT_CREATED, adminId, "USER", newUserId, payload)` invoked once.
7. Assert returned `StaffAccountResponse` has no password field.

**Expected Result (PASS):** All four collaborators (repository, encoder, email, audit) invoked correctly; response DTO is password-free.
**Expected Result (FAIL):** Any collaborator skipped, or response contains a password-shaped field.

**Current Status:** 🔴 Not written
**Implementation Note:** All four side effects must occur inside the same `@Transactional` boundary per ADR-IAM-005/006.

---

### UC115-TC-002 — SYSTEM_ADMIN creates a CONTENT_ADMIN account

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-IAM-004 Decision — staff roles = {MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN}`

**Test Steps:**
1. Call `createStaffAccount(adminId, makeCreateStaffRequest(r -> r.setRole(Role.CONTENT_ADMIN)))`.
2. Assert saved entity `role=CONTENT_ADMIN`.

**Expected Result (PASS):** CONTENT_ADMIN accepted identically to MODERATOR path.
**Expected Result (FAIL):** Role incorrectly rejected or mismapped.

**Current Status:** 🔴 Not written

---

### UC115-TC-003 — SYSTEM_ADMIN creates a SYSTEM_ADMIN account (only the caller who already holds SYSTEM_ADMIN may do this)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-IAM-004 Decision — Option A`, `CB-IDENTITY-IMP-115 §16 Authorization Matrix`

**Preconditions:** FX-115-001 (SYSTEM_ADMIN caller), FX-115-003 (`role=SYSTEM_ADMIN` request).

**Test Steps:**
1. Call `createStaffAccount(adminId, FX-115-003)` where `adminId` already holds `SYSTEM_ADMIN`.
2. Assert creation succeeds; saved entity `role=SYSTEM_ADMIN`.
3. Assert audit payload's `assignedRole == SYSTEM_ADMIN` and `createdByAdminId == adminId`.

**Expected Result (PASS):** Only a caller structurally guaranteed to already be SYSTEM_ADMIN (enforced at controller) can reach this path; service-level creation succeeds and is fully audited.
**Expected Result (FAIL):** Missing audit trail for the highest-privilege creation type, or role mismapped.

**Current Status:** 🔴 Not written
**Implementation Note:** This test only proves the service-layer behavior for an already-authorized caller. The structural guarantee that ONLY SYSTEM_ADMIN reaches this method at all is proven separately by `UC115-TC-SEC-001`/`SEC-002` at the controller layer — do not conflate the two.

---

### UC115-TC-004 — Temp password is SecureRandom-generated, never admin-supplied, never returned in response

**Severity:** `CRITICAL`
**CWE:** `CWE-798 — Use of Hard-coded Credentials` (inverse check — proving randomness/non-hardcoding)
**Feature Under Test:** `AdminStaffServiceImpl.generateTempPassword()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-IAM-005 Decision — Option A`, `CB-IDENTITY-IMP-115 §17 C3`

**Test Steps:**
1. Assert `CreateStaffAccountRequest` DTO (per §8.1) has NO password/tempPassword field at all — reflection check.
2. Call `createStaffAccount()` twice with different requests; capture the two arguments passed to `PasswordEncoder.encode()` via Mockito `ArgumentCaptor`.
3. Assert the two captured plaintext values are different (proves fresh `SecureRandom` generation, not a static/hardcoded value).
4. Assert `StaffAccountResponse` (response DTO) has no field that could carry a plaintext or hashed password.

**Expected Result (PASS):** No password input surface exists on the request DTO; two calls yield two distinct generated passwords; response DTO is password-free.
**Expected Result (FAIL):** Request DTO accepts a password field, or two calls yield an identical/predictable password.

**Current Status:** 🔴 Not written

---

### UC115-TC-005 — mustChangePassword is always true at creation

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-IDENTITY-IMP-115 §6.3 State Machine — invariant #3`

**Test Steps:**
1. Call `createStaffAccount(adminId, FX-115-002)`.
2. Assert saved `User.mustChangePassword == true`.
3. Assert `StaffAccountResponse.mustChangePassword == true` in the returned DTO.

**Expected Result (PASS):** Flag is unconditionally `true`; no code path allows `false` at insert time.
**Expected Result (FAIL):** Flag defaults to `false`, or is settable via the request DTO (which must not expose this field at all per §8.1).

**Current Status:** 🔴 Not written

---

### UC115-TC-006 — role=MOTHER is rejected with IAM-115-005 before any persistence

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffServiceImpl.assertStaffRole()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-IAM-004 Decision — service-level defense-in-depth check`, `CB-IDENTITY-IMP-115 §10 IAM-115-005`

**Preconditions:** FX-115-004 (`role=MOTHER`).

**Test Steps:**
1. Call `createStaffAccount(adminId, FX-115-004)`.
2. Assert `ValidationException` with code `IAM-115-005`.
3. Assert `UserRepository.save()`, `EmailService.send()`, `AuditService.log()` all NEVER invoked.

**Expected Result (PASS):** Guard fires before any side effect; this endpoint can never be repurposed to create self-registration-role accounts.
**Expected Result (FAIL):** A MOTHER/FAMILY/EXPERT/PARTNER account is created via this admin-only endpoint.

**Current Status:** 🔴 Not written

---

### UC115-TC-007 — role=FAMILY/EXPERT/PARTNER all rejected with IAM-115-005 (parameterized)

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffServiceImpl.assertStaffRole()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-IAM-004`

**Test Steps:**
1. Parameterized test over `{FAMILY, EXPERT, PARTNER}`.
2. For each, call `createStaffAccount(adminId, makeCreateStaffRequest(r -> r.setRole(role)))`.
3. Assert `IAM-115-005` for all three.

**Expected Result (PASS):** All three non-staff roles rejected identically to MOTHER.
**Expected Result (FAIL):** Any one of the three is silently accepted.

**Current Status:** 🔴 Not written

---

### UC115-TC-008 — Duplicate email is rejected with IAM-115-002 (409)

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-IDENTITY-IMP-115 §6.2 Error Path — Duplicate email`

**Preconditions:** FX-115-007 (existing user with same email); `UserRepository.existsByEmail()` mocked → `true`.

**Test Steps:**
1. Call `createStaffAccount(adminId, makeCreateStaffRequest(r -> r.setEmail("existing@carebridge.dev")))`.
2. Assert `ValidationException` `IAM-115-002`, HTTP 409 at controller level.
3. Assert no `save()`/`send()`/`log()` calls.

**Expected Result (PASS):** Conflict detected before any write.
**Expected Result (FAIL):** Duplicate row inserted, or existing user silently overwritten.

**Current Status:** 🔴 Not written

---

### UC115-TC-009 — Duplicate phone is rejected with IAM-115-002 (409)

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminStaffService.createStaffAccount()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-IDENTITY-IMP-115 §4.2 Uniqueness`

**Test Steps:**
1. Mock `existsByEmail()` → `false`, `existsByPhone()` → `true`.
2. Call `createStaffAccount(...)`.
3. Assert `IAM-115-002`.

**Expected Result (PASS):** Phone uniqueness enforced independently of email.
**Expected Result (FAIL):** Phone collision ignored, allowing two accounts to share a phone number.

**Current Status:** 🔴 Not written

---

### UC115-TC-010 — EmailService delivery failure rolls back the entire transaction, no orphaned account

**Severity:** `HIGH`
**Feature Under Test:** `AdminStaffService.createStaffAccount()` transactional boundary
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/service/AdminStaffServiceTransactionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-IDENTITY-IMP-115 §10 IAM-115-006`, `ADR-IAM-005`

**Preconditions:** FX-115-008 (`EmailService.send()` throws), real Testcontainers DB (not mocked repository — must prove actual rollback).

**Test Steps:**
1. Call `createStaffAccount(adminId, FX-115-002)` with `EmailService` configured to throw.
2. Assert a `502`-mapped exception (`IAM-115-006`) propagates.
3. Query the DB directly: assert NO `users` row exists for the attempted email.
4. Query `audit_logs`: assert NO `STAFF_ACCOUNT_CREATED` row was committed.

**Expected Result (PASS):** Full rollback — zero trace of the failed attempt in persisted state.
**Expected Result (FAIL):** An orphaned `enabled=true` account exists with no delivered credential — an unusable, unrecoverable account.

**Current Status:** 🔴 Not written
**Implementation Note:** This MUST be a real transactional integration test, not a mocked-repository unit test, since transaction rollback semantics cannot be proven with mocks alone.

---

### UC115-TC-011 — Missing/invalid email or name rejected with IAM-115-003

**Severity:** `LOW`
**Feature Under Test:** `CreateStaffAccountRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminStaffControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-IDENTITY-IMP-115 §8.1 @Email/@NotBlank`, `§10 IAM-115-003`

**Test Steps:**
1. POST with `email="not-an-email"` → assert 400 `IAM-115-003`.
2. POST with `name=""` → assert 400 `IAM-115-003`.

**Expected Result (PASS):** Both invalid shapes rejected before reaching the service.
**Expected Result (FAIL):** Malformed email accepted and persisted.

**Current Status:** 🔴 Not written

---

### UC115-TC-012 — Generated temp password satisfies existing PasswordComplexityPolicy

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminStaffServiceImpl.generateTempPassword()`
**Test File:** `AdminStaffServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-IDENTITY-IMP-115 §4.3 Security — Password strength`

**Test Steps:**
1. Generate 50 temp passwords via `generateTempPassword()` (reflection/package-visible test hook).
2. Assert every one passes `PasswordComplexityPolicy.isValid(password)` (the SAME existing policy class used elsewhere — not a new one).

**Expected Result (PASS):** 50/50 pass the existing complexity policy.
**Expected Result (FAIL):** Any generated password fails the existing policy, or a parallel/weaker policy was implemented instead of reusing the existing class.

**Current Status:** 🔴 Not written

---

### UC115-TC-013 — Temp password never appears in application logs

**Severity:** `CRITICAL`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File`
**Feature Under Test:** `AdminStaffServiceImpl.createStaffAccount()` / `EmailService` call site
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/security/AdminStaffLoggingSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-IDENTITY-IMP-115 §4.3 Security — Credential handling`, `§14.2 Log Verification`

**Test Steps:**
1. Attach a test log appender (e.g., Logback `ListAppender`) capturing all log events during `createStaffAccount()` execution.
2. Call the method with a known-value captured temp password (via `ArgumentCaptor` on `PasswordEncoder.encode()`).
3. Assert no captured log message (at any level, INFO through ERROR) contains the plaintext password substring.

**Expected Result (PASS):** Zero log lines contain the plaintext credential.
**Expected Result (FAIL):** Password appears in a DEBUG/INFO log statement (e.g., accidental `logger.info("Created user with password {}", tempPassword)`).

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES — RELEASE-BLOCKING SELF-ESCALATION GATE

---

### UC115-TC-SEC-001 — Non-SYSTEM_ADMIN caller cannot reach staff-account creation at all (structural self-escalation prevention)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-269 — Improper Privilege Management`
**Legal:** `BR-RBAC`, CASE 2.0 mandatory constraint (task brief RG-4)
**Feature Under Test:** `AdminStaffController` (`@WebMvcTest` with mocked `AdminStaffService`)
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminStaffControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** JWTs for every non-SYSTEM_ADMIN role: `MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, PARTNER` (FX-115-005, FX-115-006, and equivalents).

**Test Steps (Attack Simulation):**
1. Parameterized over all six non-SYSTEM_ADMIN roles.
2. `MockMvc.perform(post("/api/v1/admin/staff-accounts").with(jwt of role).content(FX-115-002 as JSON))`.
3. Assert HTTP 403, `error.code == "IAM-115-001"`.
4. Assert `AdminStaffService.createStaffAccount()` was NEVER invoked (Mockito `verifyNoInteractions`).

**Expected Result (PASS = hệ thống an toàn):** All six roles rejected at the `@PreAuthorize` layer; service never touched.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any non-SYSTEM_ADMIN role successfully reaches the service layer — this is a release-blocking finding (AP-CB-IAM-003).

**Current Status:** 🔴 Not written

---

### UC115-TC-SEC-002 — MODERATOR/CONTENT_ADMIN attempting to create a SYSTEM_ADMIN account is rejected before any business logic (self-role-escalation-to-SYSTEM_ADMIN attempt)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-269 — Improper Privilege Management`
**Legal:** CASE 2.0 mandatory constraint — "a MODERATOR/CONTENT_ADMIN must never be able to grant themselves or anyone SYSTEM_ADMIN"
**Feature Under Test:** `AdminStaffController` full pipeline
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/security/AdminStaffSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-115-005 (MODERATOR JWT), FX-115-006 (CONTENT_ADMIN JWT), FX-115-003 (`role: SYSTEM_ADMIN` request body), Testcontainers DB.

**Test Steps (Attack Simulation):**
1. Authenticate as MODERATOR (FX-115-005).
2. `POST /api/v1/admin/staff-accounts` with body `{email:"evil@x.com", name:"Evil", role:"SYSTEM_ADMIN"}`.
3. Assert HTTP 403 `IAM-115-001` — rejected at `@PreAuthorize`, never reaches `assertStaffRole()`.
4. Query DB directly: assert NO `users` row was created with `email="evil@x.com"`.
5. Repeat steps 1-4 authenticated as CONTENT_ADMIN (FX-115-006).
6. Repeat once more attempting `role:"MODERATOR"` (a lower-privilege staff role) from the MODERATOR caller — assert still 403 (proves the block is on CALLER identity, not on the requested role value).

**Expected Result (PASS = hệ thống an toàn):** Zero DB writes from any non-SYSTEM_ADMIN attempt, regardless of which role was requested; identical rejection whether requesting SYSTEM_ADMIN or a lower staff role.
**Expected Result (FAIL = lỗ hổng tồn tại):** A SYSTEM_ADMIN (or any staff) account is created by a MODERATOR/CONTENT_ADMIN caller — this is the single most severe finding possible in this cluster (AP-CB-IAM-002), release-blocking.

**Current Status:** 🔴 Not written
**Implementation Note:** This test must run against the REAL Spring Security filter chain (`@SpringBootTest` + `MockMvc` with full context, or Testcontainers integration harness) — not a unit test with a mocked `@PreAuthorize`, to prove the actual security configuration, not an assumption about it.

---

### UC115-TC-SEC-003 — Injection attempt in name/email fields handled safely

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection` / `CWE-79 — Cross-Site Scripting`
**Feature Under Test:** `AdminStaffService.createStaffAccount()` + JPA parameter binding
**Test File:** `AdminStaffSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Preconditions:** FX-115-009 (injection payloads in `name`/`email`).

**Test Steps (Attack Simulation):**
1. Authenticate as SYSTEM_ADMIN.
2. POST with FX-115-009 payload (adjusted to pass `@Email` validation where required, or targeting `name` only if email format is enforced).
3. Assert either HTTP 400 (validation rejects the malformed email) or, if `name` accepts the XSS payload as a literal string, assert it is stored/returned as an inert string (never executed, never breaks the SQL statement).

**Expected Result (PASS = hệ thống an toàn):** No SQL error, no stored/reflected script execution; JPA parameter binding treats the value as literal data.
**Expected Result (FAIL = lỗ hổng tồn tại):** 500 error exposing query structure, or unescaped script payload rendered in a later admin-list view.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC115-TC-INT-001 — Full staff-account creation round trip against real DB with audit verification

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Service -> Repository -> PostgreSQL (Testcontainers) -> EmailService (mocked bean)`
**Test File:** `src/test/java/com/carebridge/backend/identity/admin/controller/AdminStaffControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied automatically (including `V20260704090100`).
- `EmailService` replaced with a `@MockBean` that succeeds (captures the send call without real SMTP).
- Seed FX-115-001 (admin) via JPA before each test (fresh instance per Props Isolation rule).

**Test Steps:**
1. Authenticate as admin (derived from FX-115-001).
2. `POST /api/v1/admin/staff-accounts` with FX-115-002 body → assert HTTP 201.
3. Assert response body has no password field, `mustChangePassword=true`.
4. Query DB directly: assert `users` row exists with `role='MODERATOR'`, `enabled=true`, `must_change_password=true`, `password_hash` populated and NOT equal to any plaintext value.
5. Query `audit_logs`: assert exactly one row `action='STAFF_ACCOUNT_CREATED'`, `entity_id=newUserId`, `actor_id=adminId`.
6. Assert mocked `EmailService.send()` was invoked exactly once with the target email.

**Expected Result (PASS):** DB state, audit trail, and email dispatch all consistent and atomic.
**Expected Result (FAIL):** Any of the three (DB row, audit row, email call) missing or inconsistent with the others.

**DB Assertion:**
```java
User created = userRepository.findByEmail("new.moderator@carebridge.dev").orElseThrow();
assertThat(created.getRole()).isEqualTo(Role.MODERATOR);
assertThat(created.isMustChangePassword()).isTrue();
assertThat(created.getPasswordHash()).isNotBlank();
List<AuditLog> logs = auditLogRepository.findByEntityIdAndAction(created.getId(), AuditAction.STAFF_ACCOUNT_CREATED);
assertThat(logs).hasSize(1);
```

**Current Status:** 🔴 Not written

---

### UC115-WEB-TC-001 — CreateStaffAccountForm submits and shows confirmation without displaying a password

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateStaffAccountForm.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminGovernance/components/CreateStaffAccountForm.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Preconditions:** Mock `POST /api/v1/admin/staff-accounts` returning 201 happy-path fixture matching TDS §9.2 (no password field).

**Test Steps:**
1. Render `<CreateStaffAccountForm />`, fill email/name/role, submit.
2. `await screen.findByText(...)` confirmation message.
3. Assert no element in the rendered DOM contains a password-shaped value; assert the confirmation explicitly states credentials were emailed, not displayed.

**Expected Result (PASS):** Confirmation UI never renders a password value anywhere.
**Expected Result (FAIL):** Response body (if it ever contained a password) is rendered on screen.

**Current Status:** 🔴 Not written

---

### UC115-WEB-TC-002 — CreateStaffAccountForm role selector only offers staff roles (client-side defense in depth)

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateStaffAccountForm.tsx`
**Test File:** `CreateStaffAccountForm.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `ADR-IAM-004` (UI-level defense-in-depth mirroring the backend guard — backend remains the enforcement authority)

**Test Steps:**
1. Render the form.
2. Query the role `<select>`/combobox options.
3. Assert options are exactly `{MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN}` — no `MOTHER/FAMILY/EXPERT/PARTNER` option exists to select.

**Expected Result (PASS):** Client cannot even construct a non-staff-role request via the UI.
**Expected Result (FAIL):** Full role enum exposed in the selector, relying solely on the backend 400 (defense-in-depth gap, not a security failure by itself since backend still enforces IAM-115-005).

**Current Status:** 🔴 Not written

---

### UC115-WEB-TC-003 — CreateStaffAccountForm renders access-denied UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateStaffAccountForm.tsx`
**Test File:** `CreateStaffAccountForm.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Test Steps:**
1. Mock API returning 403 `IAM-115-001`.
2. Submit form.
3. Assert an access-denied message renders, not a crash.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection / silent failure that could mislead the admin into thinking creation succeeded.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC115-TC-001` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-002` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-003` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-004` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-005` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-006` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-007` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-008` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-009` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-010` | `AdminStaffServiceTransactionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-011` | `AdminStaffControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-012` | `AdminStaffServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-013` | `AdminStaffLoggingSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-SEC-001` | `AdminStaffControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-SEC-002` | `AdminStaffSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-SEC-003` | `AdminStaffSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-TC-INT-001` | `AdminStaffControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC115-WEB-TC-001` | `CreateStaffAccountForm.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC115-WEB-TC-002` | `CreateStaffAccountForm.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC115-WEB-TC-003` | `CreateStaffAccountForm.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AdminStaffServiceImpl implements AdminStaffService {

    @Override
    public StaffAccountResponse createStaffAccount(UUID callerUserId, CreateStaffAccountRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC115-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC115-TC-006` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC115-TC-SEC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC115-TC-SEC-002` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC115-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-IDENTITY-IMP-115` approved (currently Draft — **ADR-IAM-005 Option C alternative explicitly flagged for Principal Architect decision before implementation**, per TDS §11.1)
- [ ] Logic Issues (§2, L1-L4) confirmed with Tech Lead
- [ ] Migration `V20260704090100__add_users_must_change_password.sql` approved and tested on staging ≥ 24h (§5.2 of TDS)
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green
- [ ] Test coverage ≥ 80% lines for `AdminStaffServiceImpl`
- [ ] No business logic in `AdminStaffController` (validation + mapping only)
- [ ] No password/tempPassword in logs or API responses at any level (`UC115-TC-013` green)
- [ ] `UC115-TC-SEC-001` and `UC115-TC-SEC-002` (self-escalation prevention) BOTH green — **mandatory, release-blocking gate, not optional**
- [ ] `UC115-TC-010` (transactional rollback on email failure) green

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `AdminGovernanceTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet confirmed present in `CareBridgeWebApp/package.json` — UC115-WEB-TC-* suspended until verified/added
- ADR-IAM-005 Option C (invite-token flow) remains an unresolved Principal Architect decision point — if selected during review, this entire Test-Spec's password-generation test cases (UC115-TC-004/005/012/013) must be superseded, not silently patched
- Login-flow enforcement of `mustChangePassword=true` (forced password-change redirect) is explicitly Open per ADR-IAM-005 Hệ quả — no test in this spec covers it; flagged as a separate follow-up feature, not a gap in this spec

---

## 7. Rollback Plan

```bash
# Bước 1: Revert migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.users DROP COLUMN IF EXISTS must_change_password;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260704090100';"

# Bước 2: Revert implementation and test files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260704090100__add_users_must_change_password.sql
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminGovernance/components/CreateStaffAccountForm*

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a role-hierarchy escalation model not in ADR-IAM-004 | ☑ Không phát hiện — UC115-TC-SEC-001/002 explicitly test the Option A structural guarantee | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — controller tests (TC-011, SEC-001) only assert RBAC/validation, business logic asserted only in Service tests | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import an invite-token entity/table not defined in this TDS (Option C not selected) | ☑ Không phát hiện — no test references an invite-token type | G-3 |
| AP-CB-IAM-002 | **Self-Role-Escalation** | Any code path lets a non-SYSTEM_ADMIN caller create/elevate an account to SYSTEM_ADMIN or any staff role | ☑ Explicitly covered — `UC115-TC-SEC-002` is a dedicated, mandatory, release-blocking test for exactly this scenario | **Release-blocking** |
| AP-CB-IAM-003 | **SYSTEM_ADMIN Grant By Non-SYSTEM_ADMIN** | Endpoint authorization allows any role other than SYSTEM_ADMIN to reach `createStaffAccount()` | ☑ Explicitly covered — `UC115-TC-SEC-001` parameterizes all six non-admin roles against the controller | **Release-blocking** |
| AP-CB-IAM-004 | Known Password | Admin-supplied or predictable password used for a new staff account | ☑ Explicitly covered — `UC115-TC-004` proves DTO has no password field and generated values differ per call | **Release-blocking** |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)
- [x] Cả hai self-escalation anti-pattern (AP-CB-IAM-002, AP-CB-IAM-003) có test case chuyên biệt, đánh dấu release-blocking rõ ràng

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
