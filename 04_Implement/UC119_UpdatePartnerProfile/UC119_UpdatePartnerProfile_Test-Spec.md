# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-119: Update Partner Profile

**Document ID:** `CB-PTR-TEST-002`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — no new PII field vs UC-118)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC119_UpdatePartnerProfile/UC119_UpdatePartnerProfile_TDS.md` (`CB-PTR-IMP-002`)
- SRS Section 3.2.3.2
- Sibling (Approved, infra + state machine + PTR error codes oracle): `04_Implement/UC118_CreatePartnerProfile/` (`CB-PTR-IMP-001`)
- CLAUDE.md §3 Architecture Rules, §5 Delivery Rules

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-119 Update Partner Profile (Status=Draft)   |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-119`                                                                                                                |
| **Module**                | `Update Partner Profile — partner (brownfield extension of UC-118)`                                                     |
| **Spec gốc**              | `CB-PTR-IMP-002`                                                                                                          |
| **Priority**              | `P0 — High, Occasional` (per FS)                                                                                          |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (all UC-118 infra)`, `security (SecurityContext)`, `audit (AuditService)`                                        |
| **Downstream Consumers**  | `UC-123 Approve Partner Profile` (if ADR-003 later resets status on edit)                                                  |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-002 §17`, `ADR-001..ADR-005`                                                                                           |
| **Constraints Injected** | `C1 (RBAC PARTNER)`, `C2 (anti-impersonation — id from context)`, `C3 (status/representativeUserId immutable)`, `C4 (ownership resolve → PTR-007)`, `C5 (validation reuse)`, `C6 (reuse UC-118 infra, no migration)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                                     | Thực tế (schema / policy / UC-118)                                                                     | Fix áp dụng trong test                                                                    |
| --- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| L1  | FS mô tả update chung chung ("updates profile information")                  | Editable fields = contact/descriptive; `status` & `representativeUserId` KHÔNG editable (ADR-001/003)     | Test PHẢI assert `status` & `representativeUserId` KHÔNG đổi sau update (negative assertion)      |
| L2  | Không rõ edit có reset status về PENDING không                              | ADR-003 (Accepted, resolved via project analysis): v1 KHÔNG reset — least-surprise, smallest scoped change                       | Test happy-path assert `status` giữ nguyên; gắn Oracle `ADR-003 (Accepted — project-analysis default)`; removable if Product later requires re-review |
| L3  | Nguy cơ impersonation (Partner A sửa hồ sơ Partner B)                        | ADR-002: id luôn từ SecurityContext, không từ body → vector bị loại theo thiết kế                         | Test PHẢI assert partnerId truyền trong body BỊ BỎ QUA; resolve theo current user — PTR-UC119-TC-206 |
| L4  | Role name?                                                                   | Mâu thuẫn nguồn `PARTNER` (UC-118) vs `PARTNER` (CLAUDE.md) — ADR-005 Open                            | Security test dùng `PARTNER` theo UC-118 nhưng gắn note ADR-005; verify role string thật trước RED |
| L5  | Status nào được sửa?                                                         | ADR-003 (Accepted): v1 SUSPENDED/REJECTED không sửa được → PTR-009                                            | Test PTR-UC119-TC-203 gắn Oracle `ADR-003 (Accepted — project-analysis default)`; removable nếu Product cho SUSPENDED sửa         |
| L6  | 403 code là gì?                                                             | UC-118 §10 định nghĩa PTR-004; nhưng moderation cluster thấy code thật là ACCESS_DENIED — cần verify       | Security test assert 403 + (PTR-004 HOẶC ACCESS_DENIED) — assert status 403 là oracle tối thiểu ổn định |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UC-119 Update Partner Profile:
├── Controller (PartnerProfileController.updateProfile() — mock service, @WebMvcTest)
├── Service (PartnerProfileServiceImpl.updateProfile() — mock PartnerOrganizationRepository + audit, Mockito)
├── Repository (findByRepresentativeUserId / save — existing/additive)
└── Integration (Full PUT flow — MockMvc + Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source                                  | Items Derived                                                                                 |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `SRS 3.2.3.2`                             | Partner updates own profile                                                                        |
| `TDS ADR-001`                             | Editable fields; reuse UC-118 infra; no migration                                                  |
| `TDS ADR-002`                             | Ownership via SecurityContext; anti-impersonation                                                  |
| `TDS ADR-003`                             | status/representativeUserId immutable; edit no-reset (Accepted); SUSPENDED/REJECTED forbidden (Accepted)   |
| `UC-118 TDS §10`                          | PTR error-code prefix + PTR-004 (403) / PTR-006 (401) precedent                                     |
| `partner/entity/OrganizationStatus.java`  | PENDING_APPROVAL/APPROVED/SUSPENDED/REJECTED — state oracle                                         |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — update editable fields; status & representativeUserId unchanged | `PartnerProfileServiceImpl.updateProfile()`    | `PUP-TC-201`        |
| TC-COND-002  | No profile for current user → PTR-007 (404)                                | service                                          | `PUP-TC-202`        |
| TC-COND-003  | status SUSPENDED/REJECTED → PTR-009 (409) [ADR-003 Accepted]                    | service                                          | `PUP-TC-203`        |
| TC-COND-004  | Invalid email/phone → PTR-001 (reused validators)                          | controller `@Valid` / service                    | `PUP-TC-204`        |
| TC-COND-005  | Audit log called exactly once on success                                   | `AuditService` mock verify                       | `PUP-TC-205`        |
| TC-COND-006  | Anti-impersonation: partnerId in body ignored; resolve by current user     | service                                          | `PUP-TC-206`        |
| TC-COND-007  | status NOT changed (negative assertion, dedicated)                         | service                                          | `PUP-TC-207`        |
| TC-COND-008  | Non-PARTNER → 403                                                       | `@PreAuthorize`                                   | `PUP-TC-208`        |
| TC-COND-009  | No JWT → 401                                                                | entry point                                       | `PUP-TC-209`        |
| TC-COND-010  | SQL injection in `description` field                                        | JPA parameterized                                 | `PUP-TC-210`        |
| TC-COND-011  | Full PUT integration — DB reflects new fields, same status                 | Testcontainers                                    | `PUP-TC-INT-001`    |
| TC-COND-012  | Integration: representativeUserId unchanged in DB                          | Testcontainers                                    | `PUP-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique                | Applied To                                          | Rationale                                                        |
| --------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------- |
| Negative / Immutability   | status & representativeUserId unchanged                  | ADR-001/003 core guarantee                                            |
| Equivalence Partitioning  | status classes (editable vs forbidden)                  | ADR-003 SUSPENDED/REJECTED partition                                  |
| Error Guessing            | impersonation (id in body), SQL injection, wrong role   | Security vectors                                                     |
| Regression (cross-UC)     | reuse UC-118 validators                                  | Guards against divergent validation                                 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                       | Mục đích                                  |
| ----------- | -------- | ---------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-601`   | DB seed | `PartnerOrganization{representativeUserId: U1, status: APPROVED}`                          | Happy-path edit (status preserved)             |
| `FX-602`   | DB seed | `PartnerOrganization{representativeUserId: U2, status: SUSPENDED}`                         | PTR-009 forbidden-status test (ADR-003 Accepted)   |
| `FX-603`   | (none)  | current user U3 has NO partner profile                                                     | PTR-007 not-found                              |
| `FX-604`   | JWT     | `{sub: U1, role: "ROLE_PARTNER"}`                                                     | Auth happy path (owner)                        |
| `FX-605`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}`                                                    | Auth failure (403)                             |
| `FX-606`   | none    | No `Authorization` header                                                                  | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class UpdatePartnerProfileTestFactory {

    static final UUID OWNER_USER_ID = UUID.fromString("f1000000-0000-0000-0000-000000000001");
    static final UUID PARTNER_ID    = UUID.fromString("f2000000-0000-0000-0000-000000000001");
    static final UUID OTHER_PARTNER_ID = UUID.fromString("f2000000-0000-0000-0000-0000000000ff");

    static PartnerOrganization makePartner(UUID representativeUserId, OrganizationStatus status,
                                           Consumer<PartnerOrganization> overrides) {
        PartnerOrganization p = PartnerOrganization.builder()
                .id(PARTNER_ID)
                .name("Phòng khám ABC")
                .type(OrganizationType.CLINIC)
                .address("123 X").city("Hà Nội")
                .phone("0901234567").email("a@abc.vn")
                .status(status)
                .representativeUserId(representativeUserId)
                .build();
        overrides.accept(p);
        return p;
    }

    static UpdatePartnerProfileRequest makeRequest(Consumer<...> overrides) {
        // full editable field set; factory owns construction (no shared mutable state)
        return new UpdatePartnerProfileRequest("Phòng khám ABC (updated)", OrganizationType.CLINIC,
                "456 Y", "Hà Nội", "0907654321", "new@abc.vn", "https://abc.vn", null, "Mô tả mới");
    }
}
```

---

### PUP-TC-201 — Happy path: update editable fields; status & representativeUserId unchanged

**Severity:** `HIGH`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile(request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-001 §Decision`, `ADR-003 §Decision (status no-reset)`

**Preconditions:** `findByRepresentativeUserId(OWNER_USER_ID)` returns `FX-601` (status=APPROVED)

**Test Steps:**
1. Arrange: request updates name/address/phone/email/description
2. Act: `service.updateProfile(request, principal)` (principal resolves OWNER_USER_ID)
3. Assert (capture saved entity)

**Expected Result (PASS):**
- Saved `PartnerOrganization` has new name/address/phone/email/description
- Saved `status == APPROVED` (**unchanged** — ADR-003)
- Saved `representativeUserId == OWNER_USER_ID` (**unchanged**)
- `response.status() == APPROVED`

**Expected Result (FAIL):** status flipped to PENDING_APPROVAL, or representativeUserId changed → ADR-001/003 violation.

**Current Status:** 🔴 Not written

---

### PUP-TC-202 — No profile for current user → 404 PTR-007

**Severity:** `HIGH`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile()` — ownership resolve
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 PTR-007`, `§8.1 @throws PTR-007`

**Preconditions:** `findByRepresentativeUserId(currentUserId)` returns `Optional.empty()` (FX-603)

**Test Steps:** Act + Assert: throws `PartnerException` code `PTR-007`, `httpStatus == 404`

**Expected Result (PASS):** `PTR-007`; no save, no audit.

**Current Status:** 🔴 Not written

---

### PUP-TC-203 — status SUSPENDED/REJECTED → 409 PTR-009 (ADR-003 Accepted)

**Severity:** `MEDIUM`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile()` — status guard
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-003 §Decision` — **Accepted, resolved via project analysis (not a sourced fact)**

**Preconditions:** `findByRepresentativeUserId(U2)` returns `FX-602` (status=SUSPENDED)

**Test Steps:**
1. Sub-case a: status SUSPENDED → Act + Assert: `PTR-009`, 409
2. Sub-case b: status REJECTED → Act + Assert: `PTR-009`, 409

**Expected Result (PASS):** Both throw `PTR-009`; no save.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Gated on ADR-003. If Product decides SUSPENDED partners MAY edit (to fix issues
before reinstatement), this test AND `PTR-009` must be removed together — do not leave dead logic.

---

### PUP-TC-204 — Invalid email/phone → 400 PTR-001 (reused validators)

**Severity:** `MEDIUM`
**Feature Under Test:** `@Valid` bean validation reused from UC-118
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC-118 §10 PTR-001`, `TDS ADR-001 (validator reuse)`

**Test Steps:** `PUT` with `email="not-an-email"` / `phone="abc"`, PARTNER JWT

**Expected Result (PASS):** `response.status == 400`, `error.code == "PTR-001"` (same validation contract as UC-118).

**Current Status:** 🔴 Not written

---

### PUP-TC-205 — Audit log called exactly once on success

**Severity:** `MEDIUM`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-004`

**Test Steps:** valid update → `verify(auditService, times(1)).log(...)`

**Expected Result (PASS):** Exactly 1 audit call with the partner-update action.

**Current Status:** 🔴 Not written

---

### PUP-TC-206 — Anti-impersonation: partnerId in body ignored; resolve by current user

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control (IDOR)`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile()` — ownership resolve (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-002 §Decision`

**Preconditions:** current user OWNER_USER_ID owns `FX-601` (PARTNER_ID). A malicious body attempts to include
`partnerId = OTHER_PARTNER_ID` (some other partner's id) — but the DTO has NO partnerId field per §8.3, so
this is tested by confirming the service resolves ONLY via `findByRepresentativeUserId(currentUserId)`.

**Test Steps:**
1. Arrange: valid request (DTO has no partnerId field to inject); current user = OWNER_USER_ID
2. Act + Assert: `verify(repo).findByRepresentativeUserId(OWNER_USER_ID)`; the saved entity is `PARTNER_ID` (owner's), NEVER `OTHER_PARTNER_ID`

**Expected Result (PASS):** Only the current user's own profile (`PARTNER_ID`) is ever loaded/saved; no code path
accepts a caller-supplied partner id.

**Expected Result (FAIL):** Any path that loads/saves a partner by a body-supplied id → IDOR vulnerability, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This test doubles as a design guard — if a future refactor adds a `partnerId`
field to `UpdatePartnerProfileRequest`, this test must fail until ownership is re-verified server-side.

---

### PUP-TC-207 — status NOT changed (dedicated negative assertion)

**Severity:** `HIGH`
**Feature Under Test:** `PartnerProfileServiceImpl.updateProfile()` — status immutability (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/partner/UpdatePartnerProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-003`

**Test Steps:** Update a profile whose status is APPROVED; even if request JSON contained a `status` field (it
should be ignored/absent per §8.3), assert saved `status == APPROVED`.

**Expected Result (PASS):** `status` unchanged regardless of any status value a caller tries to smuggle.
**Expected Result (FAIL):** A caller-supplied status is honored → privilege escalation (self-approve), BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** Mirrors UC-118 ADR-003's "no self-approve" guarantee — a partner must never approve
their own profile by sneaking `status=APPROVED` into an update.

---

### SECURITY TEST CASES

### PUP-TC-208 — Non-PARTNER → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `PartnerProfileController.updateProfile()` — `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/UpdatePartnerProfileControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-002/ADR-005`, `UC-118 §10 (PTR-004 403 — verify vs ACCESS_DENIED)`

**Preconditions:** JWT role `ROLE_MOTHER` (FX-605)

**Expected Result (PASS):** `response.status == 403`; error code is `PTR-004` OR `ACCESS_DENIED` — assert
`status == 403` as the stable minimum oracle (exact code pending the §6.3 verification, ADR-005).

**Current Status:** 🔴 Not written
**Implementation Note:** Role string confirmed as `PARTNER` (ADR-005, resolved) — no longer a risk factor
before this test can go GREEN — a wrong role string would make even a legitimate partner get 403.

---

### PUP-TC-209 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT authentication entry point
**Test File:** `src/test/java/com/carebridge/backend/security/UpdatePartnerProfileControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC-118 §10 (PTR-006 401 — verify vs bodiless)`

**Test Steps:** `PUT .../partner/profile` no `Authorization` header (FX-606)

**Expected Result (PASS):** `response.status == 401`. Body form (PTR-006 vs bodiless) `Open` — assert 401 as minimum.

**Current Status:** 🔴 Not written

---

### PUP-TC-210 — SQL injection trong `description` field

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89`
**Feature Under Test:** `description` field handling
**Test File:** `src/test/java/com/carebridge/backend/security/UpdatePartnerProfileControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Test Steps:** `PUT` with `description="x'; DROP TABLE partner_organizations;--"`, owner JWT

**Expected Result (PASS):** Stored verbatim (JPA parameterized); `partner_organizations` table intact.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### PUP-TC-INT-001 — Full PUT flow — DB reflects new fields, same status

**Severity:** `HIGH`
**Feature Under Test:** `PUT /api/v1/partner/profile` — end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/UpdatePartnerProfileIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`

**Preconditions:** PostgreSQL Testcontainer, Flyway (no new migration); seed `FX-601` (APPROVED); owner JWT

**Test Steps:**
1. Seed partner (status APPROVED)
2. `PUT .../partner/profile` new fields
3. Assert 200; re-fetch `partner_organizations` row

**Expected Result (PASS):**
- DB: name/address/phone/email/description updated
- DB: `status == 'APPROVED'` (unchanged), `representative_user_id` unchanged, `updated_at` bumped

**Current Status:** 🔴 Not written

---

### PUP-TC-INT-002 — representativeUserId unchanged in DB

**Severity:** `HIGH`
**Feature Under Test:** representativeUserId immutability end-to-end
**Test File:** `src/test/java/com/carebridge/backend/integration/UpdatePartnerProfileIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS ADR-001/ADR-002`

**Test Steps:** update, then assert DB `representative_user_id` equals the original owner id.

**Expected Result (PASS):** Unchanged.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                              | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | --------------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `PUP-TC-201`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `PUP-TC-202`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `PUP-TC-203`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | ADR-003 Accepted |
| `PUP-TC-204`      | `UpdatePartnerProfileControllerTest.java`                | `[ ]`               | —                     | —                    |
| `PUP-TC-205`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `PUP-TC-206`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | ★ IDOR guard         |
| `PUP-TC-207`      | `UpdatePartnerProfileServiceImplTest.java`               | `[ ]`               | —                     | ★ self-approve guard |
| `PUP-TC-208`      | `UpdatePartnerProfileControllerSecurityTest.java`        | `[ ]`               | —                     | ADR-005 role string  |
| `PUP-TC-209`      | `UpdatePartnerProfileControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `PUP-TC-210`      | `UpdatePartnerProfileControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `PUP-TC-INT-001`  | `UpdatePartnerProfileIntegrationTest.java`               | `[ ]`               | —                     | —                    |
| `PUP-TC-INT-002`  | `UpdatePartnerProfileIntegrationTest.java`               | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// PartnerProfileServiceImpl.java — added method only; createProfile() (UC-118) unchanged/already GREEN
@Override
public UpdatePartnerProfileResponse updateProfile(UpdatePartnerProfileRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause (nếu PASS bất thường) |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------------------------------ |
| `PUP-TC-201`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `PUP-TC-202`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `PUP-TC-206`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `PUP-TC-207`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `PUP-TC-208`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —                                     |
| `PUP-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → GATE-2 PASS (T2→T3)
- Log file: `Open`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-118 (`CB-PTR-IMP-001`) deployed (infra exists)
- [x] **ADR-005 resolved** — real role string confirmed as `PARTNER` (user-confirmed + verified via code)
- [ ] **ADR-003 confirmed** — edit-resets-status? editable-statuses? — Product decision
- [ ] Fixtures FX-601..FX-606 prepared
- [ ] No migration needed — confirmed

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=UpdatePartnerProfileServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=UpdatePartnerProfileControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=UpdatePartnerProfileControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=UpdatePartnerProfileIntegrationTest` — all PASS (Testcontainers)
- [ ] PUP-TC-206: anti-impersonation (IDOR) — VERIFIED (CRITICAL security gate)
- [ ] PUP-TC-207: no self-approve via status smuggling — VERIFIED (CRITICAL)
- [ ] PUP-TC-201/INT-001: status & representativeUserId immutable — VERIFIED (CRITICAL data integrity)
- [ ] PUP-TC-208: non-PARTNER rejected — VERIFIED (CRITICAL, contingent on ADR-005 role string)
- [ ] No business logic in controller (only `@Valid` + delegate)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — all tests FAIL with throw stub before implement
- [ ] Contract Existence — `./mvnw compile` clean (`UpdatePartnerProfileRequest/Response`, PTR-007/009 factories)
- [ ] Props Isolation — factory methods used, no shared mutable static
- [ ] Oracle Source — every expected value cites ADR/UC-118 §

### Suspension Criteria
- ADR-003 or ADR-005 unresolved
- `@EnableMethodSecurity` not enabled
- CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert. Test spec files retained.
# NOTE: reverting must not break UC-118's createProfile() on the shared PartnerProfileService — re-run UC-118 tests.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                           | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002/ADR-003                                             | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | PUP-TC-201..207 PASS với throw stub                                                    | `[x]`   | G-2 ★       |
| AP-AI-002 | IDOR / Impersonation     | Không có TC assert id-from-context (thiếu PUP-TC-206)                                  | `[x]`   | G-2 ★       |
| AP-AI-003 | Implicit Decision        | TC tự cho edit reset status mà không tham chiếu ADR-003                                | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                         | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import service/entity không có trong §8                                            | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0 Anti-Pattern Detection & Red Gate Protocol) — Status: Draft.*
*Brownfield extension of UC-118; no schema delta. IDOR/anti-impersonation (PUP-TC-206) and no-self-approve
(PUP-TC-207) are the CRITICAL security gates. ADR-003 (edit-resets-status, editable-statuses) and ADR-005
(role name) are both resolved via project analysis - confirmed `PARTNER`, edit does not reset status.*
