# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-122: View Partner Performance

**Document ID:** `CB-PTR-TEST-005`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(aggregate own-org data, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC122_ViewPartnerPerformance/UC122_ViewPartnerPerformance_TDS.md` (`CB-PTR-IMP-005`)
- SRS Section 3.2.3.5
- Siblings (Draft): UC-120 (`CB-PTR-IMP-003`), UC-121 (`CB-PTR-IMP-004`), UC-111 (read-only pattern `CB-MOD-IMP-006`)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-122 View Partner Performance (Status=Draft) |

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
| **Feature / UC ID**       | `UC-122`                                                                                                                |
| **Module**                | `View Partner Performance — partner (read-only aggregate, own org)`                                                    |
| **Spec gốc**              | `CB-PTR-IMP-005`                                                                                                          |
| **Priority**              | `P1 — Medium, Regular`                                                                                                     |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (UC-118/119/120/121 infra, partner_expert_links)`, `security`                                                     |
| **Downstream Consumers**  | Partner Web performance UI                                                                                                 |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-005 §17`, `ADR-001..ADR-002`                                                                                           |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (read-only)`, `C3 (own-org filter, IDOR guard)`, `C4 (metric grounding, no referral/click)`, `C5 (no PII)`         |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | --------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS gợi ý "referral/campaign performance"                 | Không có bảng referral/click/conversion → chỉ đếm được catalog status + expert links (ADR-001) | Test CHỈ assert 3 metric groundable; KHÔNG assert referral/click (không tồn tại) |
| L2  | Nguy cơ IDOR (xem performance partner khác)              | ADR-002: filter theo org của current user, không nhận partner_id                    | Test PPF-TC-503: chỉ đếm rows của org A; org B's rows KHÔNG lọt vào               |
| L3  | Role name / 403 code                                     | UC-119 ADR-005 (Open); PTR-004 vs ACCESS_DENIED                                      | Security test assert 403 + note                                                   |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (PartnerPerformanceController.getPerformance() — @WebMvcTest)
├── Service (PartnerPerformanceServiceImpl — mock repos returning canned counts)
├── Repository (count-by-status; active expert links)
└── Integration (Full GET — Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.3.5` | Partner views own performance |
| `TDS §5.2` | 3 grounded metrics; column oracle |
| `TDS ADR-001` | No referral/click metrics |
| `TDS ADR-002` | Own-org filter, IDOR guard |
| `partner_services`/`sponsored_campaigns`/`partner_expert_links` schema | Status enum + ACTIVE oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — aggregates for own org                                        | `PartnerPerformanceServiceImpl.getPerformance()` | `PPF-TC-501`      |
| TC-COND-002  | No org for user → PTR-016 (404)                                            | service                                          | `PPF-TC-502`        |
| TC-COND-003  | Ownership: only own org's rows counted (org B noise excluded)              | integration                                      | `PPF-TC-503`        |
| TC-COND-004  | Invalid range (from > to) → PTR-017 (400)                                  | controller/service                               | `PPF-TC-504`        |
| TC-COND-005  | activeExpertLinks counts only status='ACTIVE'                             | service                                          | `PPF-TC-505`        |
| TC-COND-006  | Response has NO referral/click/conversion field (grounding guard)          | DTO shape                                         | `PPF-TC-506`        |
| TC-COND-007  | Empty data → all counts 0/empty maps, no crash                            | service                                          | `PPF-TC-507`        |
| TC-COND-008  | Non-PARTNER → 403                                                       | `@PreAuthorize`                                   | `PPF-TC-508`        |
| TC-COND-009  | No JWT → 401                                                               | entry point                                       | `PPF-TC-509`        |
| TC-COND-010  | Read-only — no mutation                                                     | integration + pg_stat                            | `PPF-TC-INT-002`    |
| TC-COND-011  | Full GET integration — counts match direct SQL for own org                | Testcontainers                                    | `PPF-TC-INT-001`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Ownership isolation | own org vs other org rows | ADR-002 IDOR guard |
| Boundary | date range | PTR-017 |
| Special-Value | empty data | robustness |
| Negative / grounding | no referral/click field | ADR-001 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                     | Mục đích                                  |
| ----------- | -------- | ----------------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-901`   | DB seed | Org A (owner U1) with services (mixed status), campaigns (mixed), expert links (mixed status) | Happy path + active-link filter        |
| `FX-902`   | DB seed | Org B with its own services/campaigns/links                                          | Ownership isolation noise (must NOT leak into A's counts) |
| `FX-903`   | (none)  | U3 has no org                                                                       | PTR-016                                        |
| `FX-904`   | JWT     | `{sub: U1, role: "ROLE_PARTNER"}`                                                | Auth happy path (owner of A)                   |
| `FX-905`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}`                                               | Auth failure (403)                             |
| `FX-906`   | none    | No `Authorization`                                                                  | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class PartnerPerformanceTestFactory {
    static final UUID OWNER_A = UUID.fromString("fa000000-0000-0000-0000-000000000001");
    static final UUID ORG_A   = UUID.fromString("fb000000-0000-0000-0000-00000000000a");
    static final UUID ORG_B   = UUID.fromString("fb000000-0000-0000-0000-00000000000b");

    static PerformanceFilter makeFilter(LocalDate from, LocalDate to) { return new PerformanceFilter(from, to); }
    static Map<String,Long> svcCounts()  { return Map.of("PENDING", 2L, "APPROVED", 5L, "REJECTED", 1L); }
    static Map<String,Long> campCounts() { return Map.of("PENDING", 1L, "APPROVED", 3L); }
}
```

---

### PPF-TC-501 — Happy path: aggregates for own org

**Severity:** `HIGH`
**Feature Under Test:** `PartnerPerformanceServiceImpl.getPerformance(filter, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §5.2`

**Preconditions:** org resolve returns ORG_A; repos return factory counts

**Expected Result (PASS):** `serviceListingsByStatus` == factory svc map; `campaignsByStatus` == factory camp map; `activeExpertLinks` correct; no repo mutation.

**Current Status:** 🔴 Not written

---

### PPF-TC-502 — No org for user → 404 PTR-016

**Severity:** `HIGH`
**Feature Under Test:** org resolve
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 PTR-016`

**Expected Result (PASS):** throws `PTR-016`, 404.

**Current Status:** 🔴 Not written

---

### PPF-TC-503 — Ownership: only own org's rows counted (org B excluded)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — IDOR`
**Feature Under Test:** own-org filter (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/integration/PartnerPerformanceIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002`

**Preconditions:** seed org A (FX-901) + org B (FX-902) with their own rows; call as owner of A

**Expected Result (PASS):** counts reflect ONLY org A's rows; org B's services/campaigns/links never counted.
**Expected Result (FAIL):** org B's rows leak into A's counts → cross-partner data leak, BLOCKING.

**Current Status:** 🔴 Not written

---

### PPF-TC-504 — Invalid range (from > to) → 400 PTR-017

**Severity:** `MEDIUM`
**Feature Under Test:** range validation
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 PTR-017`

**Expected Result (PASS):** 400, `error.code == "PTR-017"`.

**Current Status:** 🔴 Not written

---

### PPF-TC-505 — activeExpertLinks counts only status='ACTIVE'

**Severity:** `MEDIUM`
**Feature Under Test:** expert-link active filter
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `partner_expert_links.status` (default ACTIVE)

**Preconditions (integration):** seed links with ACTIVE + non-ACTIVE statuses

**Expected Result (PASS):** only ACTIVE links counted.

**Current Status:** 🔴 Not written

---

### PPF-TC-506 — Response has NO referral/click/conversion field (grounding guard)

**Severity:** `HIGH`
**Feature Under Test:** metric grounding (ADR-001)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-001`

**Test Steps:** assert `PartnerPerformanceResponse` type graph has NO referral/click/conversion/view/revenue field — only the 3 grounded metrics + dates.

**Expected Result (PASS):** no hallucinated-metric field present.
**Expected Result (FAIL):** a referral/click field exists (backed by no column) → invented metric.

**Current Status:** 🔴 Not written

---

### PPF-TC-507 — Empty data → all counts 0/empty, no crash

**Severity:** `MEDIUM`
**Feature Under Test:** no-data robustness
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerPerformanceServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`

**Expected Result (PASS):** empty maps + `activeExpertLinks == 0`; no exception.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### PPF-TC-508 — Non-PARTNER → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/PartnerPerformanceControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16`, UC-119 ADR-005

**Preconditions:** JWT role MOTHER (FX-905)

**Expected Result (PASS):** 403 (assert status; confirmed ACCESS_DENIED (PTR-004 unreachable)).

**Current Status:** 🔴 Not written

---

### PPF-TC-509 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/PartnerPerformanceControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### PPF-TC-INT-001 — Full GET — counts match direct SQL for own org

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/partner/performance` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/PartnerPerformanceIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`

**Preconditions:** Testcontainer + Flyway (no new migration); seed FX-901 (org A) + FX-902 (org B); owner-A JWT

**Expected Result (PASS):** each metric equals the direct-SQL count for org A (§14 SQL); org B excluded.

**Current Status:** 🔴 Not written

---

### PPF-TC-INT-002 — Read-only — no mutation

**Severity:** `HIGH`
**Feature Under Test:** read-only guarantee
**Test File:** `src/test/java/com/carebridge/backend/integration/PartnerPerformanceIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Expected Result (PASS):** no insert/update/delete delta on partner tables after a GET.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `PPF-TC-501`      | `PartnerPerformanceServiceImplTest.java`             | `[ ]`               | —                     | —                    |
| `PPF-TC-502`      | `PartnerPerformanceServiceImplTest.java`             | `[ ]`               | —                     | —                    |
| `PPF-TC-503`      | `PartnerPerformanceIntegrationTest.java`             | `[ ]`               | —                     | ★ IDOR / ownership   |
| `PPF-TC-504`      | `PartnerPerformanceControllerTest.java`              | `[ ]`               | —                     | —                    |
| `PPF-TC-505`      | `PartnerPerformanceServiceImplTest.java`             | `[ ]`               | —                     | —                    |
| `PPF-TC-506`      | `PartnerPerformanceServiceImplTest.java`             | `[ ]`               | —                     | ★ grounding guard    |
| `PPF-TC-507`      | `PartnerPerformanceServiceImplTest.java`             | `[ ]`               | —                     | —                    |
| `PPF-TC-508`      | `PartnerPerformanceControllerSecurityTest.java`      | `[ ]`               | —                     | ADR-005 role string  |
| `PPF-TC-509`      | `PartnerPerformanceControllerSecurityTest.java`      | `[ ]`               | —                     | —                    |
| `PPF-TC-INT-001`  | `PartnerPerformanceIntegrationTest.java`             | `[ ]`               | —                     | —                    |
| `PPF-TC-INT-002`  | `PartnerPerformanceIntegrationTest.java`             | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// PartnerPerformanceServiceImpl.java — Red Phase stub
@Override
public PartnerPerformanceResponse getPerformance(PerformanceFilter filter, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `PPF-TC-501`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PPF-TC-503`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PPF-TC-506`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PPF-TC-508`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `PPF-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-120/121 deployed (entities/repos for count queries); read-only `PartnerExpertLinkRepository` available/added
- [ ] ADR-005 (role name) resolved — BLOCKING
- [ ] No migration needed — confirmed
- [ ] Fixtures FX-901..FX-906 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=PartnerPerformanceServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=PartnerPerformanceControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=PartnerPerformanceControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=PartnerPerformanceIntegrationTest` — all PASS (Testcontainers)
- [ ] PPF-TC-503: ownership isolation (no cross-partner leak) — VERIFIED (CRITICAL IDOR gate)
- [ ] PPF-TC-506: no invented referral/click metric — VERIFIED (grounding guard)
- [ ] PPF-TC-508: non-PARTNER rejected — VERIFIED (CRITICAL, contingent ADR-005)
- [ ] PPF-TC-INT-002: read-only — VERIFIED
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`PartnerPerformanceResponse`, `PerformanceFilter`, PTR-016/017)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited

### Suspension Criteria
- ADR-005 (role name) unresolved; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert. Read-only endpoint.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | PPF-TC-501..507 PASS với throw stub                                             | `[x]`   | G-2 ★       |
| AP-AI-002 | IDOR                     | Không có TC assert own-org filter (thiếu PPF-TC-503)                            | `[x]`   | G-2 ★       |
| AP-AI-003 | Hallucinated Metric      | TC assert referral/click metric không có cột (§5.2)                            | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller query DB trực tiếp                                        | `[x]`   | G-4         |
| AP-AI-005 | Hidden Write             | TC không phát hiện endpoint "read" ghi DB (thiếu PPF-TC-INT-002)                | `[x]`   | G-4         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Read-only aggregate over partner's own data; no schema delta.
Ownership isolation (PPF-TC-503, IDOR) and metric grounding (PPF-TC-506, no invented referral/click) are the
CRITICAL gates. Role name (UC-119 ADR-005) verified before RED.*
