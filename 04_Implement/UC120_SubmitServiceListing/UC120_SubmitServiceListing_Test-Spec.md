# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-120: Submit Service Listing

**Document ID:** `CB-PTR-TEST-003`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — service catalog, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC120_SubmitServiceListing/UC120_SubmitServiceListing_TDS.md` (`CB-PTR-IMP-003`)
- SRS Section 3.2.3.3
- Siblings (Draft, this batch): UC-118 (`CB-PTR-IMP-001`, infra), UC-119 (`CB-PTR-IMP-002`, role-name ADR-005 resolved = `PARTNER`)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                            |
| ---------- | -------------------- | -------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-120 Submit Service Listing (Status=Draft) |

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
| **Feature / UC ID**       | `UC-120`                                                                                                                |
| **Module**                | `Submit Service Listing — partner (greenfield Java over existing partner_services table)`                              |
| **Spec gốc**              | `CB-PTR-IMP-003`                                                                                                          |
| **Priority**              | `P0 — High, Occasional`                                                                                                    |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (PartnerOrganization/OrganizationStatus/PartnerException — UC-118; findByRepresentativeUserId — UC-119)`, `security`, `audit` |
| **Downstream Consumers**  | `UC-124 Approve`, `UC-125 Remove`, `UC-122 Performance`                                                                    |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-003 §17`, `ADR-001..ADR-004`                                                                                           |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (partner_id from context)`, `C3 (approval_status server-set PENDING)`, `C4 (APPROVED-org precondition)`, `C5 (no migration)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                        | Thực tế (schema / policy)                                                                     | Fix áp dụng trong test                                                        |
| --- | ---------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| L1  | FS chung chung ("submit a service listing")                     | `approval_status` default PENDING; partner phải APPROVED (ADR-002/003)                          | Test assert approval_status=PENDING server-set; partner non-APPROVED → PTR-011        |
| L2  | Nguy cơ self-approve (body chứa approval_status=APPROVED)       | ADR-003: server hard-code PENDING                                                               | Test PSL-TC-305: body approval_status=APPROVED bị bỏ qua, DB vẫn PENDING              |
| L3  | Nguy cơ gán partner_id sai (body chứa partner_id khác)          | ADR-002: partner_id resolve từ org của current user                                            | Test PSL-TC-306: partner_id resolve theo current user, body ignored                   |
| L4  | Partner chưa duyệt vẫn lên catalog?                             | ADR-002: chỉ APPROVED được submit                                                              | Test PSL-TC-303 cho từng non-APPROVED status → PTR-011                                |
| L5  | Role name / 403 code                                            | UC-119 ADR-005 resolved (role = `PARTNER`); 403 code confirmed `ACCESS_DENIED` — PTR-004 unreachable, same pattern as MOD-004        | Security test assert 403, `error.code == "ACCESS_DENIED"`                          |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (PartnerServiceController.submitService() — @WebMvcTest, mock service)
├── Service (PartnerServiceServiceImpl.submitService() — mock OrgRepo + ServiceRepo + audit, Mockito)
├── Repository (PartnerServiceRepository.save; PartnerOrganizationRepository.findByRepresentativeUserId)
└── Integration (Full POST — MockMvc + Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source                                  | Items Derived                                                                       |
| ------------------------------------------ | ------------------------------------------------------------------------------------- |
| `SRS 3.2.3.3`                             | Partner submits service listing                                                       |
| `TDS ADR-002`                             | APPROVED-org precondition; partner_id from context                                    |
| `TDS ADR-003`                             | approval_status server-set PENDING                                                    |
| `partner_services` schema (line 1038)     | Columns + default 'PENDING' oracle                                                    |
| `partner/entity/OrganizationStatus.java`  | PENDING_APPROVAL/APPROVED/SUSPENDED/REJECTED for precondition partitioning            |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — APPROVED org → listing created, PENDING, partner_id=org.id     | `PartnerServiceServiceImpl.submitService()`     | `PSL-TC-301`        |
| TC-COND-002  | No org for user → PTR-010 (404)                                            | service                                          | `PSL-TC-302`        |
| TC-COND-003  | Org status != APPROVED (PENDING/SUSPENDED/REJECTED) → PTR-011 (409)        | service                                          | `PSL-TC-303`        |
| TC-COND-004  | Validation (blank name / negative price / bad URL) → PTR-012 (400)         | controller `@Valid` / service                    | `PSL-TC-304`        |
| TC-COND-005  | approval_status server-set PENDING even if body injects APPROVED           | service                                          | `PSL-TC-305`        |
| TC-COND-006  | partner_id from resolve, body value ignored                                | service                                          | `PSL-TC-306`        |
| TC-COND-007  | currency defaults to VND when null                                         | service                                          | `PSL-TC-307`        |
| TC-COND-008  | Audit log called once on success                                          | `AuditService` mock verify                       | `PSL-TC-308`        |
| TC-COND-009  | Non-PARTNER → 403                                                       | `@PreAuthorize`                                   | `PSL-TC-309`        |
| TC-COND-010  | No JWT → 401                                                               | entry point                                       | `PSL-TC-310`        |
| TC-COND-011  | Full POST integration — DB row PENDING + correct partner_id               | Testcontainers                                    | `PSL-TC-INT-001`    |
| TC-COND-012  | Non-approved org integration → PTR-011, no row created                     | Testcontainers                                    | `PSL-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Equivalence Partitioning | org status (APPROVED vs others) | ADR-002 precondition |
| Negative / Server-Set | approval_status, partner_id from body ignored | ADR-002/003 gates |
| Boundary | priceFrom (0 valid, negative invalid) | PTR-012 |
| Error Guessing | wrong role, self-approve injection, impersonation | Security |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                             | Mục đích                                  |
| ----------- | -------- | --------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-701`   | DB seed | `PartnerOrganization{representativeUserId: U1, status: APPROVED}`             | Happy path                                     |
| `FX-702`   | DB seed | `PartnerOrganization{representativeUserId: U2, status: PENDING_APPROVAL}`     | PTR-011 (not approved)                         |
| `FX-703`   | DB seed | `PartnerOrganization{representativeUserId: U3, status: SUSPENDED}`            | PTR-011 (suspended variant)                    |
| `FX-704`   | (none)  | U4 has no org                                                               | PTR-010                                        |
| `FX-705`   | JWT     | `{sub: U1, role: "ROLE_PARTNER"}`                                        | Auth happy path                                |
| `FX-706`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}`                                       | Auth failure (403)                             |
| `FX-707`   | none    | No `Authorization`                                                          | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class SubmitServiceListingTestFactory {
    static final UUID OWNER_USER_ID = UUID.fromString("f3000000-0000-0000-0000-000000000001");
    static final UUID ORG_ID        = UUID.fromString("f4000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ORG_ID  = UUID.fromString("f4000000-0000-0000-0000-0000000000ff");

    static PartnerOrganization makeOrg(UUID repUserId, OrganizationStatus status) {
        return PartnerOrganization.builder().id(ORG_ID).name("ABC").type(OrganizationType.CLINIC)
                .status(status).representativeUserId(repUserId).build();
    }
    static SubmitServiceListingRequest makeRequest() {
        return new SubmitServiceListingRequest("Khám thai", "Gói khám", new BigDecimal("500000"), "VND", "https://abc.vn/b");
    }
}
```

---

### PSL-TC-301 — Happy path: APPROVED org → listing PENDING, partner_id=org.id

**Severity:** `HIGH`
**Feature Under Test:** `PartnerServiceServiceImpl.submitService(request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-002/ADR-003`, `partner_services` default PENDING

**Preconditions:** `findByRepresentativeUserId(OWNER_USER_ID)` → `FX-701` (APPROVED)

**Test Steps:** Arrange valid request → Act `submitService(...)` → capture saved `PartnerService`

**Expected Result (PASS):**
- Saved `approvalStatus == PENDING`, `partnerId == ORG_ID`
- `response.approvalStatus() == PENDING`, `serviceName`/`priceFrom`/`currency` match request

**Current Status:** 🔴 Not written

---

### PSL-TC-302 — No org for user → 404 PTR-010

**Severity:** `HIGH`
**Feature Under Test:** ownership/org resolve
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 PTR-010`

**Preconditions:** `findByRepresentativeUserId` → empty (FX-704)

**Expected Result (PASS):** throws `PartnerException` `PTR-010`, 404; no save.

**Current Status:** 🔴 Not written

---

### PSL-TC-303 — Org not APPROVED → 409 PTR-011

**Severity:** `CRITICAL`
**Feature Under Test:** APPROVED-org precondition (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002`

**Test Steps:** sub-cases: org status = PENDING_APPROVAL (FX-702), SUSPENDED (FX-703), REJECTED

**Expected Result (PASS):** each throws `PTR-011`, 409; no listing created.
**Expected Result (FAIL):** any non-APPROVED org gets a listing → unvetted partner on catalog.

**Current Status:** 🔴 Not written

---

### PSL-TC-304 — Validation (blank name/negative price/bad URL) → 400 PTR-012

**Severity:** `MEDIUM`
**Feature Under Test:** `@Valid` / service validation
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 PTR-012`, `§8.3 validators`

**Test Steps:** blank `serviceName`; `priceFrom = -1`; malformed `bookingUrl`

**Expected Result (PASS):** 400, `error.code == "PTR-012"` (or field-level 400 — assert 400 + field as minimum).

**Current Status:** 🔴 Not written

---

### PSL-TC-305 — approval_status server-set PENDING even if body injects APPROVED

**Severity:** `CRITICAL`
**Feature Under Test:** anti-self-approve (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-003`

**Test Steps:** DTO has no `approvalStatus` field (§8.3), so this verifies the saved entity's status is ALWAYS
PENDING regardless of any raw-JSON injection attempt.

**Expected Result (PASS):** Saved `approvalStatus == PENDING` unconditionally.
**Expected Result (FAIL):** A caller-influenced status is honored → self-approve, BLOCKING.

**Current Status:** 🔴 Not written

---

### PSL-TC-306 — partner_id from resolve, body value ignored

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — IDOR`
**Feature Under Test:** partner_id ownership resolve (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-002`

**Test Steps:** DTO has no `partnerId` field; verify saved `partnerId == ORG_ID` (caller's own org), never `OTHER_ORG_ID`.

**Expected Result (PASS):** partner_id always the caller's own org.
**Expected Result (FAIL):** any body-supplied partner id path → IDOR, BLOCKING.

**Current Status:** 🔴 Not written

---

### PSL-TC-307 — currency defaults to VND when null

**Severity:** `LOW`
**Feature Under Test:** currency default
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `partner_services.currency DEFAULT 'VND'`

**Expected Result (PASS):** null currency → stored/returned as `VND`.

**Current Status:** 🔴 Not written

---

### PSL-TC-308 — Audit log called once on success

**Severity:** `MEDIUM`
**Feature Under Test:** audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitServiceListingServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### PSL-TC-309 — Non-PARTNER → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/SubmitServiceListingControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16`, UC-119 ADR-005

**Preconditions:** JWT role MOTHER (FX-706)

**Expected Result (PASS):** 403 (confirmed ACCESS_DENIED (PTR-004 unreachable, same pattern as MOD-004)).

**Current Status:** 🔴 Not written

---

### PSL-TC-310 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/SubmitServiceListingControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401. Assert status only.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### PSL-TC-INT-001 — Full POST — DB row PENDING + correct partner_id

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/partner/services` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/SubmitServiceListingIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-701` (APPROVED org); owner JWT

**Test Steps:** POST valid listing → assert 201 → re-fetch `partner_services` row

**Expected Result (PASS):** DB row exists with `approval_status='PENDING'`, `partner_id = ORG_ID`, fields match.

**Current Status:** 🔴 Not written

---

### PSL-TC-INT-002 — Non-approved org → PTR-011, no row created

**Severity:** `HIGH`
**Feature Under Test:** precondition end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/SubmitServiceListingIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS ADR-002`

**Preconditions:** seed `FX-702` (PENDING_APPROVAL org)

**Expected Result (PASS):** 409 PTR-011; `SELECT count(*) FROM partner_services WHERE partner_id=<org>` == 0.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                        | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | --------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `PSL-TC-301`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-302`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-303`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-304`      | `SubmitServiceListingControllerTest.java`          | `[ ]`               | —                     | —                    |
| `PSL-TC-305`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | ★ self-approve guard |
| `PSL-TC-306`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | ★ IDOR guard         |
| `PSL-TC-307`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-308`      | `SubmitServiceListingServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-309`      | `SubmitServiceListingControllerSecurityTest.java`  | `[ ]`               | —                     | ADR-005 role string  |
| `PSL-TC-310`      | `SubmitServiceListingControllerSecurityTest.java`  | `[ ]`               | —                     | —                    |
| `PSL-TC-INT-001`  | `SubmitServiceListingIntegrationTest.java`         | `[ ]`               | —                     | —                    |
| `PSL-TC-INT-002`  | `SubmitServiceListingIntegrationTest.java`         | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// PartnerServiceServiceImpl.java — Red Phase stub
@Override
public SubmitServiceListingResponse submitService(SubmitServiceListingRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `PSL-TC-301`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PSL-TC-303`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PSL-TC-305`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PSL-TC-306`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PSL-TC-309`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `PSL-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit hash `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-118 deployed; UC-119 `findByRepresentativeUserId` available
- [x] ADR-005 (UC-119) role-name resolved — confirmed `PARTNER`
- [ ] No migration needed — confirmed; verify no CHECK on `partner_services.approval_status`
- [ ] Fixtures FX-701..FX-707 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=SubmitServiceListingServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=SubmitServiceListingControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=SubmitServiceListingControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=SubmitServiceListingIntegrationTest` — all PASS (Testcontainers)
- [ ] PSL-TC-305: no self-approve (approval_status always PENDING) — VERIFIED (CRITICAL)
- [ ] PSL-TC-306: no IDOR (partner_id from context) — VERIFIED (CRITICAL)
- [ ] PSL-TC-303: APPROVED-org precondition — VERIFIED (CRITICAL)
- [ ] PSL-TC-309: non-PARTNER rejected — VERIFIED (CRITICAL, contingent ADR-005)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`PartnerService`, `ServiceApprovalStatus`, DTOs, PTR-010/011/012)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — every expected value cites ADR/schema

### Suspension Criteria
- ADR-005 (role name) unresolved; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert. Re-run UC-118/119 tests to confirm no regression on shared partner package.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-002/ADR-003                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | PSL-TC-301..308 PASS với throw stub                                             | `[x]`   | G-2 ★       |
| AP-AI-002 | Self-Approve             | Không có TC assert approval_status server-set (thiếu PSL-TC-305)                | `[x]`   | G-2 ★       |
| AP-AI-003 | IDOR / Impersonation     | Không có TC assert partner_id from context (thiếu PSL-TC-306)                   | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Greenfield Java over existing `partner_services`; no schema delta.
Self-approve (PSL-TC-305), IDOR (PSL-TC-306), and APPROVED-org precondition (PSL-TC-303) are the CRITICAL
gates. Role name (UC-119 ADR-005) must be verified in real code before RED.*
