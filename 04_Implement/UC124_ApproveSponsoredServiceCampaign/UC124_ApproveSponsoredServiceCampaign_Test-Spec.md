# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-124: Approve Sponsored Service/Campaign

**Document ID:** `CB-PTR-TEST-007`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Partially Implemented — 2026-07-11 (11/12 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — status change, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC124_ApproveSponsoredServiceCampaign/UC124_ApproveSponsoredServiceCampaign_TDS.md` (`CB-PTR-IMP-007`)
- SRS Section 3.2.3.7
- Siblings (Draft): UC-120 (`CB-PTR-IMP-003`), UC-121 (`CB-PTR-IMP-004`), UC-123 (`CB-PTR-IMP-006`, admin-decision pattern)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                                        |
| ---------- | -------------------- | ----------------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-124 Approve Sponsored Service/Campaign (Status=Draft) |
| 2026-07-11 | AI Agent — Amelia   | RED 12/12 confirmed; 11/12 GREEN; PostgreSQL dual-target rollback assertion pending            |

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
| **Feature / UC ID**       | `UC-124`                                                                                                                |
| **Module**                | `Approve Sponsored Service/Campaign — partner (admin decision, targetType-dispatched)`                                 |
| **Spec gốc**              | `CB-PTR-IMP-007`                                                                                                          |
| **Priority**              | `P0 — High, Regular`                                                                                                       |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (PartnerService [UC-120], SponsoredCampaign [UC-121])`, `security`, `audit`                                       |
| **Downstream Consumers**  | Public catalog visibility; UC-122 status counts                                                                           |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-007 §17`, `ADR-001..ADR-005`                                                                                           |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (PENDING-only transition)`, `C3 (reason for REJECT)`, `C4 (reviewed_by asymmetry — no invented column)`, `C5 (parametrized targetType)`, `C6 (atomic)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | --------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS gộp "Service/Campaign" — hai endpoint hay một?         | ADR-001: một endpoint parametrized targetType                                       | Test cover cả SERVICE và CAMPAIGN qua cùng endpoint; unsupported → PTR-023        |
| L2  | reviewed_by cho service?                                  | ADR-003: `partner_services` KHÔNG có cột reviewed_by; chỉ campaign có               | Test ASC-TC-704: APPROVE SERVICE KHÔNG set reviewed_by (không có cột); reviewer chỉ audit |
| L3  | Re-decide item đã quyết?                                  | ADR-002: chỉ PENDING mới decide; đã quyết → PTR-024                                  | Test ASC-TC-703: APPROVED/REJECTED item → PTR-024                                 |
| L4  | Reason cho REJECT?                                        | ADR-005: REJECT bắt buộc reason (design decision)                                    | Test ASC-TC-705 gắn `ADR-005 (Open)`                                              |
| L5  | Admin role / 403                                          | ADR-004 (SYSTEM_ADMIN, resolved); confirmed ACCESS_DENIED — PTR-004 unreachable                       | Security test assert 403 + note                                                   |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (SponsoredApprovalController.decide() — @WebMvcTest)
├── Service (SponsoredApprovalServiceImpl.decide() — mock both repos + audit)
├── Repository (PartnerServiceRepository / SponsoredCampaignRepository findById/save)
└── Integration (Full POST both targetTypes — Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.3.7` | Admin approves/rejects service/campaign |
| `TDS ADR-001` | Single parametrized endpoint |
| `TDS ADR-002` | PENDING-only decide |
| `TDS ADR-003` | reviewed_by asymmetry (campaign only) |
| `partner_services` / `sponsored_campaigns` schema | approval_status + reviewed_by (campaign) oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | APPROVE CAMPAIGN PENDING→APPROVED + reviewed_by set                        | `SponsoredApprovalServiceImpl.decide()`         | `ASC-TC-701`        |
| TC-COND-002  | REJECT CAMPAIGN → REJECTED (reason)                                        | service                                          | `ASC-TC-702`        |
| TC-COND-003  | Already-decided (APPROVED/REJECTED) → PTR-024                             | service                                          | `ASC-TC-703`        |
| TC-COND-004  | APPROVE SERVICE PENDING→APPROVED, NO reviewed_by column touched            | service                                          | `ASC-TC-704`        |
| TC-COND-005  | REJECT missing reason → PTR-025                                           | service                                          | `ASC-TC-705`        |
| TC-COND-006  | Unsupported targetType → PTR-023                                          | controller/service                               | `ASC-TC-706`        |
| TC-COND-007  | target not found → PTR-022 (404)                                         | service                                          | `ASC-TC-707`        |
| TC-COND-008  | Audit called once (both target types)                                     | `AuditService` mock verify                       | `ASC-TC-708`        |
| TC-COND-009  | Non-admin → 403                                                            | `@PreAuthorize`                                   | `ASC-TC-709`        |
| TC-COND-010  | PARTNER → 403 (anti-self-approve)                                      | `@PreAuthorize`                                   | `ASC-TC-710`        |
| TC-COND-011  | No JWT → 401                                                               | entry point                                       | `ASC-TC-711`        |
| TC-COND-012  | Full POST integration both types — status changes; campaign reviewed_by set; atomicity | Testcontainers                    | `ASC-TC-INT-001`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| State Transition Testing | approval_status PENDING → {APPROVED, REJECTED} + terminal | ADR-002 |
| Equivalence Partitioning | targetType (SERVICE/CAMPAIGN/unsupported) | ADR-001/PTR-023 |
| Negative / Schema-Asymmetry | SERVICE has no reviewed_by column | ADR-003 |
| Error Guessing | re-decide, missing reason, non-admin, partner | Security + ADR-002/005 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                     | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-1101`  | DB seed | `SponsoredCampaign{approvalStatus: PENDING, reviewedBy: null}`       | APPROVE/REJECT campaign                        |
| `FX-1102`  | DB seed | `SponsoredCampaign{approvalStatus: APPROVED}`                       | Re-decide → PTR-024                            |
| `FX-1103`  | DB seed | `PartnerService{approvalStatus: PENDING}`                          | APPROVE service (no reviewed_by column)        |
| `FX-1104`  | JWT     | `{role: "ROLE_SYSTEM_ADMIN"}`                                       | Auth happy path                                |
| `FX-1105`  | JWT     | `{role: "ROLE_PARTNER"}`                                       | Auth failure (403, anti-self-approve)          |
| `FX-1106`  | JWT     | `{role: "ROLE_MOTHER"}`                                            | Auth failure (403)                             |
| `FX-1107`  | none    | No `Authorization`                                                 | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class SponsoredApprovalTestFactory {
    static final UUID ADMIN_ID    = UUID.fromString("fe000000-0000-0000-0000-0000000000ad");
    static final UUID CAMPAIGN_ID = UUID.fromString("ff000000-0000-0000-0000-00000000000c");
    static final UUID SERVICE_ID  = UUID.fromString("ff000000-0000-0000-0000-00000000000d");

    static SponsoredCampaign makeCampaign(CampaignApprovalStatus status) {
        return SponsoredCampaign.builder().campaignId(CAMPAIGN_ID).partnerId(UUID.randomUUID())
                .title("Ưu đãi").approvalStatus(status).reviewedBy(null).build();
    }
    static PartnerService makeService(ServiceApprovalStatus status) {
        return PartnerService.builder().serviceId(SERVICE_ID).partnerId(UUID.randomUUID())
                .serviceName("Khám thai").approvalStatus(status).build();
    }
    static ContentDecisionRequest makeRequest(ContentDecision decision, String reason) {
        return new ContentDecisionRequest(decision, reason);
    }
}
```

---

### ASC-TC-701 — APPROVE CAMPAIGN PENDING→APPROVED + reviewed_by set

**Severity:** `HIGH`
**Feature Under Test:** `SponsoredApprovalServiceImpl.decide(CAMPAIGN, id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-002/003`, `sponsored_campaigns.reviewed_by`

**Preconditions:** `sponsoredCampaignRepository.findById(CAMPAIGN_ID)` → `FX-1101` (PENDING)

**Expected Result (PASS):** saved `approvalStatus == APPROVED`, `reviewedBy == ADMIN_ID`; response reflects transition.

**Current Status:** 🔴 Not written

---

### ASC-TC-702 — REJECT CAMPAIGN → REJECTED (reason)

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — reject campaign
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-002/005`

**Expected Result (PASS):** saved `approvalStatus == REJECTED`, `reviewedBy == ADMIN_ID`, reason recorded.

**Current Status:** 🔴 Not written

---

### ASC-TC-703 — Already-decided → 409 PTR-024

**Severity:** `CRITICAL`
**Feature Under Test:** `decide()` — PENDING-only guard (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002`

**Preconditions:** `findById` → `FX-1102` (APPROVED)

**Test Steps:** decide APPROVE/REJECT on an already-APPROVED campaign

**Expected Result (PASS):** throws `PTR-024`, 409; no status change (no re-decide).
**Expected Result (FAIL):** re-decide succeeds → decision-integrity breach, BLOCKING.

**Current Status:** 🔴 Not written

---

### ASC-TC-704 — APPROVE SERVICE PENDING→APPROVED, NO reviewed_by touched

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — service path, reviewed_by asymmetry (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-003` — `partner_services` has NO reviewed_by column

**Preconditions:** `partnerServiceRepository.findById(SERVICE_ID)` → `FX-1103` (PENDING)

**Test Steps:** decide APPROVE on SERVICE

**Expected Result (PASS):** saved `approvalStatus == APPROVED`; `PartnerService` entity has NO reviewedBy field/setter invoked (no such column); reviewer captured in audit only.
**Expected Result (FAIL):** code attempts to set a reviewed_by on `PartnerService` (non-existent column) → compile/runtime error or hallucinated column, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Schema-asymmetry guard — proves the implementer did NOT invent a reviewed_by
column on `partner_services` (ADR-003).

---

### ASC-TC-705 — REJECT missing reason → 400 PTR-025

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — reason guard (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-005` (Open)

**Test Steps:** REJECT with `reason=null` (both CAMPAIGN and SERVICE)

**Expected Result (PASS):** throws `PTR-025`, 400; no status change.

**Current Status:** 🔴 Not written

---

### ASC-TC-706 — Unsupported targetType → 400 PTR-023

**Severity:** `MEDIUM`
**Feature Under Test:** targetType dispatch (ADR-001)
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §10 PTR-023`

**Test Steps:** path `targetType=UNKNOWN` (or a value outside {SERVICE, CAMPAIGN})

**Expected Result (PASS):** 400, `error.code == "PTR-023"` (or path-enum 400 — assert 400 minimum).

**Current Status:** 🔴 Not written

---

### ASC-TC-707 — target not found → 404 PTR-022

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — lookup
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §10 PTR-022`

**Preconditions:** `findById` → empty (for the given targetType)

**Expected Result (PASS):** `PTR-022`, 404.

**Current Status:** 🔴 Not written

---

### ASC-TC-708 — Audit called once (both target types)

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/SponsoredApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` with admin id + targetType + targetId + decision (for both SERVICE and CAMPAIGN — this is how SERVICE's reviewer is captured, ADR-003).

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### ASC-TC-709 — Non-admin → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/SponsoredApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16`, ADR-004 (role resolved = SYSTEM_ADMIN)

**Preconditions:** JWT role MOTHER (FX-1106)

**Expected Result (PASS):** 403 (assert status; verify role).

**Current Status:** 🔴 Not written

---

### ASC-TC-710 — PARTNER → 403 (anti-self-approve)

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize` — partner cannot approve own content
**Test File:** `src/test/java/com/carebridge/backend/security/SponsoredApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §16 (PARTNER = ❌ never)`

**Preconditions:** JWT role PARTNER (FX-1105)

**Expected Result (PASS):** 403 — a partner can never approve their own service/campaign (complements UC-120/121 server-set PENDING).

**Current Status:** 🔴 Not written

---

### ASC-TC-711 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/SponsoredApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### ASC-TC-INT-001 — Full POST both types — status changes; campaign reviewed_by set; atomicity

**Severity:** `CRITICAL`
**Feature Under Test:** `POST /admin/partner-content/{type}/{id}/decision` end to end + transaction
**Test File:** `src/test/java/com/carebridge/backend/integration/SponsoredApprovalIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §4 Atomicity`, ADR-003

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-1101` (campaign PENDING) + `FX-1103` (service PENDING); admin JWT

**Test Steps:**
1. POST CAMPAIGN APPROVE → 200; DB `sponsored_campaigns.approval_status='APPROVED'`, `reviewed_by = admin id`
2. POST SERVICE APPROVE → 200; DB `partner_services.approval_status='APPROVED'` (no reviewed_by column)
3. (Atomicity) force audit failure on a decision → assert approval_status rolled back to PENDING

**Expected Result (PASS):** both transitions persist; campaign reviewed_by set; service has no reviewer column; forced failure rolls back status + audit together.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `ASC-TC-701`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | —                    |
| `ASC-TC-702`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | —                    |
| `ASC-TC-703`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | ★ re-decide guard    |
| `ASC-TC-704`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | ★ no-invented-column |
| `ASC-TC-705`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | ADR-005              |
| `ASC-TC-706`      | `SponsoredApprovalControllerTest.java`               | `[ ]`               | —                     | —                    |
| `ASC-TC-707`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | —                    |
| `ASC-TC-708`      | `SponsoredApprovalServiceImplTest.java`              | `[ ]`               | —                     | —                    |
| `ASC-TC-709`      | `SponsoredApprovalControllerSecurityTest.java`       | `[ ]`               | —                     | —                    |
| `ASC-TC-710`      | `SponsoredApprovalControllerSecurityTest.java`       | `[ ]`               | —                     | ★ anti-self-approve  |
| `ASC-TC-711`      | `SponsoredApprovalControllerSecurityTest.java`       | `[ ]`               | —                     | —                    |
| `ASC-TC-INT-001`  | `SponsoredApprovalIntegrationTest.java`              | `[ ]`               | —                     | ★ atomicity + asymmetry |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// SponsoredApprovalServiceImpl.java — Red Phase stub
@Override
public ContentDecisionResponse decide(PartnerContentTargetType targetType, UUID targetId,
                                      ContentDecisionRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `ASC-TC-701`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `ASC-TC-703`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `ASC-TC-704`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `ASC-TC-709`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `ASC-TC-710`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `ASC-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-120/121 deployed (PartnerService/SponsoredCampaign entities + repos)
- [ ] ADR-004 (admin role) confirmed
- [ ] No migration needed — confirmed
- [ ] Fixtures FX-1101..FX-1107 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=SponsoredApprovalServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=SponsoredApprovalControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=SponsoredApprovalIntegrationTest` — all PASS (Testcontainers)
- [ ] ASC-TC-703: re-decide guard (PENDING-only) — VERIFIED (CRITICAL decision integrity)
- [ ] ASC-TC-704: no invented reviewed_by on service — VERIFIED (CRITICAL schema-asymmetry)
- [ ] ASC-TC-710: PARTNER cannot approve — VERIFIED (CRITICAL anti-self-approve)
- [ ] ASC-TC-INT-001: atomicity — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`PartnerContentTargetType`, `ContentDecision`, DTOs, PTR-022/023/024/025)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited

### Suspension Criteria
- ADR-004 (role) unresolved; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-002/ADR-003                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | ASC-TC-701..708 PASS với throw stub                                            | `[x]`   | G-2 ★       |
| AP-AI-002 | Re-Decide                | Không có TC cho already-decided (thiếu ASC-TC-703)                              | `[x]`   | G-2 ★       |
| AP-AI-003 | Hallucinated Column      | Không có TC assert service KHÔNG có reviewed_by (thiếu ASC-TC-704)               | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Admin decision over UC-120/121; no schema delta. Single
parametrized endpoint. Re-decide guard (ASC-TC-703), no-invented-column (ASC-TC-704, `partner_services` has
no reviewed_by), and anti-self-approve (ASC-TC-710) are the CRITICAL gates. Admin role (ADR-004) confirmed
before RED.*
