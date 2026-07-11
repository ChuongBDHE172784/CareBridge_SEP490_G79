# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-121: Submit Sponsored Content

**Document ID:** `CB-PTR-TEST-004`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Partially Implemented — 2026-07-11 (9/11 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — campaign metadata, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC121_SubmitSponsoredContent/UC121_SubmitSponsoredContent_TDS.md` (`CB-PTR-IMP-004`)
- SRS Section 3.2.3.4
- Siblings (Draft): UC-118 (`CB-PTR-IMP-001`), UC-119 (`CB-PTR-IMP-002`, ADR-005), UC-120 (`CB-PTR-IMP-003`, parallel structure)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-121 Submit Sponsored Content (Status=Draft) |
| 2026-07-11 | AI Agent — Amelia   | RED 11/11 confirmed; 9/11 GREEN; PostgreSQL assertions pending container runtime |

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
| **Feature / UC ID**       | `UC-121`                                                                                                                |
| **Module**                | `Submit Sponsored Content — partner (greenfield over existing sponsored_campaigns)`                                    |
| **Spec gốc**              | `CB-PTR-IMP-004`                                                                                                          |
| **Priority**              | `P1 — Medium, Occasional`                                                                                                  |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A` (sponsor transparency — ADR-005)                                                                                     |
| **Upstream Dependencies** | `partner (UC-118/119 infra)`, `security`, `audit`                                                                          |
| **Downstream Consumers**  | `UC-124 Approve`, `UC-125 Remove`, `UC-122 Performance`                                                                    |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-004 §17`, `ADR-001..ADR-006`                                                                                           |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (partner_id from context)`, `C3 (approval_status PENDING + reviewed_by null server-set)`, `C4 (APPROVED-org)`, `C5 (sponsor_label required, date range)`, `C6 (no migration)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                    | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS chung chung ("submit sponsored content")                 | approval_status default PENDING; partner APPROVED-only (ADR-002/003)                 | Test assert PENDING server-set; non-APPROVED → PTR-014                            |
| L2  | reviewed_by khi submit?                                      | ADR-003: reviewed_by null khi submit; chỉ UC-124 set                                | Test SSC-TC-405 assert reviewed_by null on create                                |
| L3  | Self-approve / impersonation risk                           | ADR-002/003: server-set status + partner_id from context                            | SSC-TC-405 (status/reviewed_by server-set), SSC-TC-406 (partner_id from context)  |
| L4  | sponsor_label bắt buộc không?                               | ADR-005 (Accepted, resolved via project analysis): v1 yêu cầu non-blank                                        | Test SSC-TC-404 blank sponsorLabel → PTR-015 (Oracle: ADR-005 Accepted — removable if Product later allows unlabeled campaigns)              |
| L5  | date range?                                                 | ADR-006: end_date ≥ start_date nếu cả hai có mặt                                     | Test SSC-TC-404 end<start → PTR-015; start_date-in-past không chặn (Open)         |
| L6  | Role name / 403 code                                        | UC-119 ADR-005 (resolved = PARTNER); confirmed ACCESS_DENIED — PTR-004 unreachable, same pattern as MOD-004                             | Security test assert 403 + note                                                   |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (SponsoredCampaignController.submitCampaign() — @WebMvcTest)
├── Service (SponsoredCampaignServiceImpl.submitCampaign() — mock OrgRepo + CampRepo + audit)
├── Repository (SponsoredCampaignRepository.save; PartnerOrganizationRepository.findByRepresentativeUserId)
└── Integration (Full POST — Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.3.4` | Partner submits sponsored campaign |
| `TDS ADR-002` | APPROVED-org precondition; partner_id from context |
| `TDS ADR-003` | approval_status PENDING + reviewed_by null server-set |
| `TDS ADR-005/006` | sponsor_label required; end_date ≥ start_date |
| `sponsored_campaigns` schema (line 1051) | Columns + default PENDING + reviewed_by FK oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — APPROVED org → campaign PENDING, reviewed_by null, partner_id=org.id | `SponsoredCampaignServiceImpl.submitCampaign()` | `SSC-TC-401`   |
| TC-COND-002  | No org for user → PTR-013 (404)                                            | service                                          | `SSC-TC-402`        |
| TC-COND-003  | Org not APPROVED → PTR-014 (409)                                           | service                                          | `SSC-TC-403`        |
| TC-COND-004  | Validation (blank sponsorLabel / end<start) → PTR-015 (400)                | controller/service                               | `SSC-TC-404`        |
| TC-COND-005  | approval_status PENDING + reviewed_by null server-set (body ignored)       | service                                          | `SSC-TC-405`        |
| TC-COND-006  | partner_id from resolve, body ignored                                      | service                                          | `SSC-TC-406`        |
| TC-COND-007  | Audit log called once                                                     | `AuditService` mock verify                       | `SSC-TC-407`        |
| TC-COND-008  | Non-PARTNER → 403                                                       | `@PreAuthorize`                                   | `SSC-TC-408`        |
| TC-COND-009  | No JWT → 401                                                               | entry point                                       | `SSC-TC-409`        |
| TC-COND-010  | Full POST integration — DB campaign PENDING + reviewed_by null            | Testcontainers                                    | `SSC-TC-INT-001`    |
| TC-COND-011  | Non-approved org integration → PTR-014, no row                            | Testcontainers                                    | `SSC-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Equivalence Partitioning | org status (APPROVED vs others) | ADR-002 |
| Negative / Server-Set | approval_status/reviewed_by/partner_id from body ignored | ADR-002/003 |
| Boundary | end_date vs start_date | ADR-006 (PTR-015) |
| Error Guessing | wrong role, self-approve, impersonation, blank sponsorLabel | Security + ADR-005 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                       | Mục đích                                  |
| ----------- | -------- | --------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-801`   | DB seed | `PartnerOrganization{representativeUserId: U1, status: APPROVED}`      | Happy path                                     |
| `FX-802`   | DB seed | `PartnerOrganization{representativeUserId: U2, status: PENDING_APPROVAL}` | PTR-014                                      |
| `FX-803`   | (none)  | U3 has no org                                                        | PTR-013                                        |
| `FX-804`   | JWT     | `{sub: U1, role: "ROLE_PARTNER"}`                                 | Auth happy path                                |
| `FX-805`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}`                               | Auth failure (403)                             |
| `FX-806`   | none    | No `Authorization`                                                  | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class SubmitSponsoredContentTestFactory {
    static final UUID OWNER_USER_ID = UUID.fromString("f5000000-0000-0000-0000-000000000001");
    static final UUID ORG_ID        = UUID.fromString("f6000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ORG_ID  = UUID.fromString("f6000000-0000-0000-0000-0000000000ff");

    static PartnerOrganization makeOrg(UUID repUserId, OrganizationStatus status) {
        return PartnerOrganization.builder().id(ORG_ID).name("ABC").type(OrganizationType.CLINIC)
                .status(status).representativeUserId(repUserId).build();
    }
    static SubmitSponsoredContentRequest makeRequest() {
        return new SubmitSponsoredContentRequest("Ưu đãi", "Giảm 20%",
                LocalDate.parse("2026-07-05"), LocalDate.parse("2026-07-31"), "Được tài trợ bởi ABC");
    }
}
```

---

### SSC-TC-401 — Happy path: APPROVED org → campaign PENDING, reviewed_by null, partner_id=org.id

**Severity:** `HIGH`
**Feature Under Test:** `SponsoredCampaignServiceImpl.submitCampaign(request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-002/ADR-003`, `sponsored_campaigns` default PENDING

**Preconditions:** `findByRepresentativeUserId(OWNER_USER_ID)` → `FX-801` (APPROVED)

**Expected Result (PASS):** saved `approvalStatus == PENDING`, `reviewedBy == null`, `partnerId == ORG_ID`; response fields match request.

**Current Status:** 🔴 Not written

---

### SSC-TC-402 — No org for user → 404 PTR-013

**Severity:** `HIGH`
**Feature Under Test:** org resolve
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 PTR-013`

**Preconditions:** `findByRepresentativeUserId` → empty (FX-803)

**Expected Result (PASS):** throws `PTR-013`, 404; no save.

**Current Status:** 🔴 Not written

---

### SSC-TC-403 — Org not APPROVED → 409 PTR-014

**Severity:** `CRITICAL`
**Feature Under Test:** APPROVED-org precondition (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002`

**Test Steps:** org status = PENDING_APPROVAL (FX-802), SUSPENDED, REJECTED

**Expected Result (PASS):** each throws `PTR-014`, 409; no campaign created.

**Current Status:** 🔴 Not written

---

### SSC-TC-404 — Validation (blank sponsorLabel / end<start) → 400 PTR-015

**Severity:** `MEDIUM`
**Feature Under Test:** validation (ADR-005/006)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 PTR-015`, `ADR-005 (sponsor_label required — Accepted)`, `ADR-006 (date range)`

**Test Steps:**
1. Sub-case a: blank `sponsorLabel` → 400 PTR-015 (Oracle: ADR-005 Accepted — removable if Product later allows unlabeled campaigns)
2. Sub-case b: `endDate` before `startDate` → 400 PTR-015

**Expected Result (PASS):** both 400/PTR-015; no campaign created.

**Current Status:** 🔴 Not written
**Implementation Note:** Sub-case (a) is gated on ADR-005 (Proposed). If Product allows unlabeled campaigns,
remove that sub-case and the sponsorLabel `@NotBlank`.

---

### SSC-TC-405 — approval_status PENDING + reviewed_by null server-set (body ignored)

**Severity:** `CRITICAL`
**Feature Under Test:** anti-self-approve (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-003`

**Test Steps:** DTO has no approvalStatus/reviewedBy fields (§8.3); verify saved entity has `approvalStatus == PENDING`, `reviewedBy == null` unconditionally.

**Expected Result (PASS):** PENDING + null regardless of any injection attempt.
**Expected Result (FAIL):** caller-influenced status or reviewed_by → self-approve, BLOCKING.

**Current Status:** 🔴 Not written

---

### SSC-TC-406 — partner_id from resolve, body ignored

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — IDOR`
**Feature Under Test:** partner_id ownership resolve (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-002`

**Expected Result (PASS):** saved `partnerId == ORG_ID` (caller's own), never `OTHER_ORG_ID`.

**Current Status:** 🔴 Not written

---

### SSC-TC-407 — Audit log called once

**Severity:** `MEDIUM`
**Feature Under Test:** audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/SubmitSponsoredContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### SSC-TC-408 — Non-PARTNER → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/SubmitSponsoredContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16`, UC-119 ADR-005

**Preconditions:** JWT role MOTHER (FX-805)

**Expected Result (PASS):** 403 (PTR-004 or ACCESS_DENIED — assert 403 minimum; verify role string).

**Current Status:** 🔴 Not written

---

### SSC-TC-409 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/SubmitSponsoredContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401. Assert status only.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### SSC-TC-INT-001 — Full POST — DB campaign PENDING + reviewed_by null

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/partner/campaigns` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/SubmitSponsoredContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-801` (APPROVED); owner JWT

**Expected Result (PASS):** 201; DB `sponsored_campaigns` row with `approval_status='PENDING'`, `reviewed_by IS NULL`, `partner_id = ORG_ID`.

**Current Status:** 🔴 Not written

---

### SSC-TC-INT-002 — Non-approved org → PTR-014, no row

**Severity:** `HIGH`
**Feature Under Test:** precondition end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/SubmitSponsoredContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS ADR-002`

**Preconditions:** seed `FX-802` (PENDING_APPROVAL)

**Expected Result (PASS):** 409 PTR-014; `count(*)` in `sponsored_campaigns` for that org == 0.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `SSC-TC-401`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `SSC-TC-402`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `SSC-TC-403`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `SSC-TC-404`      | `SubmitSponsoredContentControllerTest.java`          | `[ ]`               | —                     | ADR-005 sub-case removable |
| `SSC-TC-405`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | ★ self-approve guard |
| `SSC-TC-406`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | ★ IDOR guard         |
| `SSC-TC-407`      | `SubmitSponsoredContentServiceImplTest.java`         | `[ ]`               | —                     | —                    |
| `SSC-TC-408`      | `SubmitSponsoredContentControllerSecurityTest.java`  | `[ ]`               | —                     | ADR-005 role string  |
| `SSC-TC-409`      | `SubmitSponsoredContentControllerSecurityTest.java`  | `[ ]`               | —                     | —                    |
| `SSC-TC-INT-001`  | `SubmitSponsoredContentIntegrationTest.java`         | `[ ]`               | —                     | —                    |
| `SSC-TC-INT-002`  | `SubmitSponsoredContentIntegrationTest.java`         | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// SponsoredCampaignServiceImpl.java — Red Phase stub
@Override
public SubmitSponsoredContentResponse submitCampaign(SubmitSponsoredContentRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `SSC-TC-401`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `SSC-TC-403`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `SSC-TC-405`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `SSC-TC-406`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `SSC-TC-408`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `SSC-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-118 deployed; UC-119 `findByRepresentativeUserId` available
- [ ] ADR-005 (role name) resolved — BLOCKING; ADR-005 (sponsor_label required) confirmed
- [ ] No migration needed — confirmed; verify no CHECK on `sponsored_campaigns.approval_status`
- [ ] Fixtures FX-801..FX-806 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=SubmitSponsoredContentServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=SubmitSponsoredContentControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=SubmitSponsoredContentControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=SubmitSponsoredContentIntegrationTest` — all PASS (Testcontainers)
- [ ] SSC-TC-405: no self-approve (PENDING + reviewed_by null) — VERIFIED (CRITICAL)
- [ ] SSC-TC-406: no IDOR (partner_id from context) — VERIFIED (CRITICAL)
- [ ] SSC-TC-403: APPROVED-org precondition — VERIFIED (CRITICAL)
- [ ] SSC-TC-408: non-PARTNER rejected — VERIFIED (CRITICAL, contingent ADR-005)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`SponsoredCampaign`, `CampaignApprovalStatus`, DTOs, PTR-013/014/015)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited

### Suspension Criteria
- ADR-005 (role name) unresolved; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert. Re-run UC-118/119/120 tests to confirm no regression on shared partner package.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-002/ADR-003                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | SSC-TC-401..407 PASS với throw stub                                             | `[x]`   | G-2 ★       |
| AP-AI-002 | Self-Approve             | Không có TC assert approval_status/reviewed_by server-set (thiếu SSC-TC-405)    | `[x]`   | G-2 ★       |
| AP-AI-003 | IDOR / Impersonation     | Không có TC assert partner_id from context (thiếu SSC-TC-406)                   | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Greenfield Java over existing `sponsored_campaigns`; no schema
delta. Parallel to UC-120. Self-approve (SSC-TC-405: PENDING + reviewed_by null), IDOR (SSC-TC-406), and
APPROVED-org precondition (SSC-TC-403) are the CRITICAL gates. Role name (UC-119 ADR-005) verified before RED.*
