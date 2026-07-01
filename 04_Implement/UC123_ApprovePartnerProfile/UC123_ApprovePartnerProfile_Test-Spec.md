# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-123: Approve Partner Profile

**Document ID:** `CB-PTR-TEST-006`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — status change, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC123_ApprovePartnerProfile/UC123_ApprovePartnerProfile_TDS.md` (`CB-PTR-IMP-006`)
- SRS Section 3.2.3.6
- Sibling (Approved, state machine oracle): UC-118 (`CB-PTR-IMP-001` ADR-003)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-123 Approve Partner Profile (Status=Draft)  |

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
| **Feature / UC ID**       | `UC-123`                                                                                                                |
| **Module**                | `Approve Partner Profile — partner (admin state transition)`                                                            |
| **Spec gốc**              | `CB-PTR-IMP-006`                                                                                                          |
| **Priority**              | `P0 — High, Regular`                                                                                                       |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (PartnerOrganization/OrganizationStatus/PartnerException — UC-118)`, `security`, `audit`                          |
| **Downstream Consumers**  | Partner (APPROVED → can submit UC-120/121)                                                                                 |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-006 §17`, `ADR-002 (state machine)`, `ADR-005 (reason)`                                                                |
| **Constraints Injected** | `C1 (RBAC SYSTEM_ADMIN)`, `C2 (transition guard)`, `C3 (reason for REJECT/SUSPEND)`, `C4 (atomic status+audit)`, `C5 (no new state, admin-only)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                              | Thực tế (UC-118 state machine)                                                      | Fix áp dụng trong test                                                    |
| --- | ------------------------------------------------------ | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS "approve or reject" không nói transition invalid    | UC-118 ADR-003 state machine là oracle: chỉ transition hợp lệ                        | Test PHẢI cover cả transition hợp lệ VÀ invalid → PTR-020                         |
| L2  | Scope suspend/reinstate?                               | ADR-003 (Accepted, resolved via project analysis): v1 gồm suspend/reinstate như bundled scope                   | Test APP-TC-604/605 gắn Oracle `ADR-003 (Accepted)`; bundled scope, not removable unless Product later extracts a separate UC            |
| L3  | Reason bắt buộc?                                       | ADR-005: REJECT/SUSPEND cần reason (design decision)                                | Test APP-TC-606 gắn `ADR-005 (Open)`                                              |
| L4  | Partner tự đổi status?                                 | UC-118/119 đảm bảo không; UC-123 chỉ SYSTEM_ADMIN                                    | Security test: PARTNER → 403 (chống self-approve)                             |
| L5  | 403 code                                               | confirmed ACCESS_DENIED — PTR-004 unreachable, same pattern as MOD-004                                                   | Security test assert 403 + note                                                  |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (PartnerApprovalController.decide() — @WebMvcTest)
├── Service (PartnerApprovalServiceImpl.decide() — mock repo + audit, Mockito)
├── Repository (findById / save)
└── Integration (Full POST — Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.3.6` | Admin approves/rejects |
| `UC-118 ADR-003 state machine` | Valid transitions oracle (§8.3 table) |
| `TDS ADR-002/003/005` | Transition guard; suspend/reinstate scope Open; reason for REJECT/SUSPEND |
| `partner/entity/OrganizationStatus.java` | PENDING_APPROVAL/APPROVED/SUSPENDED/REJECTED |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | APPROVE PENDING_APPROVAL → APPROVED                                        | `PartnerApprovalServiceImpl.decide()`           | `APP-TC-601`        |
| TC-COND-002  | REJECT PENDING_APPROVAL → REJECTED (reason)                               | service                                          | `APP-TC-602`        |
| TC-COND-003  | Invalid transition (APPROVED→APPROVE, REJECTED→APPROVE, PENDING→SUSPEND) → PTR-020 | service                              | `APP-TC-603`        |
| TC-COND-004  | SUSPEND APPROVED → SUSPENDED (reason) [ADR-003 Accepted]                       | service                                          | `APP-TC-604`        |
| TC-COND-005  | REINSTATE SUSPENDED → APPROVED [ADR-003 Accepted]                              | service                                          | `APP-TC-605`        |
| TC-COND-006  | REJECT/SUSPEND missing reason → PTR-021                                    | service                                          | `APP-TC-606`        |
| TC-COND-007  | partner not found → PTR-018 (404)                                         | service                                          | `APP-TC-607`        |
| TC-COND-008  | Audit called once with admin id + decision                                | `AuditService` mock verify                       | `APP-TC-608`        |
| TC-COND-009  | Non-SYSTEM_ADMIN → 403                                                      | `@PreAuthorize`                                   | `APP-TC-609`        |
| TC-COND-010  | PARTNER → 403 (anti-self-approve)                                       | `@PreAuthorize`                                   | `APP-TC-610`        |
| TC-COND-011  | No JWT → 401                                                               | entry point                                       | `APP-TC-611`        |
| TC-COND-012  | Full POST integration — DB status changes; atomicity                       | Testcontainers                                    | `APP-TC-INT-001`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| State Transition Testing | all (decision × currentStatus) pairs | ADR-002 state machine — valid + invalid |
| Equivalence Partitioning | valid vs invalid transitions | PTR-020 partition |
| Error Guessing | missing reason, non-admin, partner self-approve | Security + ADR-005 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                     | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-1001`  | DB seed | `PartnerOrganization{status: PENDING_APPROVAL}`                      | APPROVE/REJECT happy paths                     |
| `FX-1002`  | DB seed | `PartnerOrganization{status: APPROVED}`                             | SUSPEND / invalid-APPROVE-again                |
| `FX-1003`  | DB seed | `PartnerOrganization{status: REJECTED}`                            | Invalid transition (REJECTED→APPROVE)          |
| `FX-1004`  | DB seed | `PartnerOrganization{status: SUSPENDED}`                          | REINSTATE                                       |
| `FX-1005`  | JWT     | `{role: "ROLE_SYSTEM_ADMIN"}`                                       | Auth happy path                                |
| `FX-1006`  | JWT     | `{role: "ROLE_PARTNER"}`                                       | Auth failure (403, anti-self-approve)          |
| `FX-1007`  | JWT     | `{role: "ROLE_MODERATOR"}`                                         | Auth failure (403, no hierarchy)               |
| `FX-1008`  | none    | No `Authorization`                                                 | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class PartnerApprovalTestFactory {
    static final UUID ADMIN_ID   = UUID.fromString("fc000000-0000-0000-0000-0000000000ad");
    static final UUID PARTNER_ID = UUID.fromString("fd000000-0000-0000-0000-000000000001");

    static PartnerOrganization makeOrg(OrganizationStatus status) {
        return PartnerOrganization.builder().id(PARTNER_ID).name("ABC").type(OrganizationType.CLINIC)
                .status(status).representativeUserId(UUID.randomUUID()).build();
    }
    static PartnerDecisionRequest makeRequest(PartnerDecision decision, String reason) {
        return new PartnerDecisionRequest(decision, reason);
    }
}
```

---

### APP-TC-601 — APPROVE PENDING_APPROVAL → APPROVED

**Severity:** `HIGH`
**Feature Under Test:** `PartnerApprovalServiceImpl.decide(partnerId, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §8.3 transition table`, UC-118 ADR-003

**Preconditions:** `findById(PARTNER_ID)` → `FX-1001` (PENDING_APPROVAL)

**Test Steps:** request `{decision: APPROVE}` → Act → capture saved org

**Expected Result (PASS):** saved `status == APPROVED`; `response.previousStatus == PENDING_APPROVAL`, `newStatus == APPROVED`, `decidedByAdminId == ADMIN_ID`.

**Current Status:** 🔴 Not written

---

### APP-TC-602 — REJECT PENDING_APPROVAL → REJECTED (reason)

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — reject path
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.3`, `ADR-005 (reason required)`

**Preconditions:** `findById` → `FX-1001` (PENDING_APPROVAL)

**Test Steps:** request `{decision: REJECT, reason: "Thiếu giấy phép"}`

**Expected Result (PASS):** saved `status == REJECTED`; `reason` recorded.

**Current Status:** 🔴 Not written

---

### APP-TC-603 — Invalid transition → 409 PTR-020

**Severity:** `CRITICAL`
**Feature Under Test:** `decide()` — transition guard (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §8.3 transition table`, UC-118 ADR-003 state machine

**Test Steps:**
1. APPROVED (FX-1002) + `decision: APPROVE` → PTR-020
2. REJECTED (FX-1003) + `decision: APPROVE` → PTR-020
3. PENDING_APPROVAL (FX-1001) + `decision: SUSPEND` → PTR-020

**Expected Result (PASS):** all throw `PTR-020`, 409; no status change (no save).
**Expected Result (FAIL):** any invalid transition succeeds → state-machine breach, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ The core state-integrity guard — proves UC-123 honors UC-118's state machine
rather than allowing arbitrary status jumps.

---

### APP-TC-604 — SUSPEND APPROVED → SUSPENDED (reason) [ADR-003 Accepted]

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — suspend (related transition)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-003 §Decision` — **Accepted scope (bundled)**

**Preconditions:** `findById` → `FX-1002` (APPROVED)

**Test Steps:** `{decision: SUSPEND, reason: "Vi phạm chính sách"}`

**Expected Result (PASS):** saved `status == SUSPENDED`.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Gated on ADR-003. If Product moves suspend/reinstate to a separate UC, this test
and `SUSPEND`/`REINSTATE` decisions are removed from UC-123.

---

### APP-TC-605 — REINSTATE SUSPENDED → APPROVED [ADR-003 Accepted]

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — reinstate (related transition)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-003 §Decision` — **Accepted scope (bundled)**

**Preconditions:** `findById` → `FX-1004` (SUSPENDED)

**Expected Result (PASS):** saved `status == APPROVED`.

**Current Status:** 🔴 Not written
**Implementation Note:** Same ADR-003 (Accepted, bundled scope) as APP-TC-604.

---

### APP-TC-606 — REJECT/SUSPEND missing reason → 400 PTR-021

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — reason guard (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-005` — design decision (Open)

**Test Steps:** REJECT with `reason=null`; SUSPEND with `reason="  "`

**Expected Result (PASS):** both throw `PTR-021`, 400; no status change.

**Current Status:** 🔴 Not written

---

### APP-TC-607 — partner not found → 404 PTR-018

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — lookup
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §10 PTR-018`

**Preconditions:** `findById` → empty

**Expected Result (PASS):** `PTR-018`, 404.

**Current Status:** 🔴 Not written

---

### APP-TC-608 — Audit called once with admin id + decision

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/PartnerApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` with admin id + decision + partnerId.
**Expected Result (FAIL):** 0 calls → an unaudited approval decision (accountability gap).

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### APP-TC-609 — Non-SYSTEM_ADMIN → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/PartnerApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16`, no `RoleHierarchy`

**Preconditions:** JWT role MODERATOR (FX-1007)

**Expected Result (PASS):** 403 (assert status; confirmed ACCESS_DENIED (PTR-004 unreachable)).

**Current Status:** 🔴 Not written

---

### APP-TC-610 — PARTNER → 403 (anti-self-approve)

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize` — partner cannot approve
**Test File:** `src/test/java/com/carebridge/backend/security/PartnerApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §16 (PARTNER = ❌ never)`

**Preconditions:** JWT role PARTNER (FX-1006)

**Expected Result (PASS):** 403 — a partner can never reach the approval endpoint (structural anti-self-approve, complementing UC-118/119 which prevent status self-set).

**Current Status:** 🔴 Not written

---

### APP-TC-611 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/PartnerApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### APP-TC-INT-001 — Full POST — DB status changes; atomicity

**Severity:** `CRITICAL`
**Feature Under Test:** `POST /api/v1/admin/partners/{id}/decision` end to end + transaction boundary
**Test File:** `src/test/java/com/carebridge/backend/integration/PartnerApprovalIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §4 Atomicity`

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-1001` (PENDING_APPROVAL); admin JWT

**Test Steps:**
1. Seed PENDING_APPROVAL org
2. POST `{decision: APPROVE}`
3. Assert 200; re-fetch DB row → `status='APPROVED'`
4. (Atomicity sub-case) force audit failure inside the transaction → assert status rolled back to PENDING_APPROVAL

**Expected Result (PASS):** status updated on success; on forced failure, status + audit both roll back (no half-committed approval).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `APP-TC-601`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `APP-TC-602`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `APP-TC-603`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | ★ state-machine guard |
| `APP-TC-604`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | ADR-003 Accepted     |
| `APP-TC-605`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | ADR-003 Accepted     |
| `APP-TC-606`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | ADR-005              |
| `APP-TC-607`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `APP-TC-608`      | `PartnerApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `APP-TC-609`      | `PartnerApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `APP-TC-610`      | `PartnerApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | ★ anti-self-approve  |
| `APP-TC-611`      | `PartnerApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `APP-TC-INT-001`  | `PartnerApprovalIntegrationTest.java`                | `[ ]`               | —                     | ★ atomicity          |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// PartnerApprovalServiceImpl.java — Red Phase stub
@Override
public PartnerDecisionResponse decide(UUID partnerId, PartnerDecisionRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `APP-TC-601`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `APP-TC-603`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `APP-TC-606`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `APP-TC-609`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `APP-TC-610`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `APP-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-118 deployed (PartnerOrganization + OrganizationStatus + repo)
- [ ] ADR-003 (suspend/reinstate scope) confirmed by Product
- [ ] No migration needed — confirmed
- [ ] Fixtures FX-1001..FX-1008 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=PartnerApprovalServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=PartnerApprovalControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=PartnerApprovalIntegrationTest` — all PASS (Testcontainers)
- [ ] APP-TC-603: invalid-transition guard (state machine) — VERIFIED (CRITICAL state integrity)
- [ ] APP-TC-610: PARTNER cannot approve (anti-self-approve) — VERIFIED (CRITICAL)
- [ ] APP-TC-609: non-SYSTEM_ADMIN rejected — VERIFIED (CRITICAL)
- [ ] APP-TC-INT-001: atomicity (status+audit) — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`PartnerDecision`, DTOs, PTR-018/020/021)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited (state machine table / ADR)

### Suspension Criteria
- ADR-003 (scope) unresolved; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# No migration to revert. Never fix status via direct DB edit without sign-off — use a new admin action.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-002 state machine                                        | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | APP-TC-601..608 PASS với throw stub                                             | `[x]`   | G-2 ★       |
| AP-AI-002 | Invalid Transition       | Không có TC cho transition invalid (thiếu APP-TC-603)                            | `[x]`   | G-2 ★       |
| AP-AI-003 | Implicit Decision        | TC tự thêm suspend/reinstate mà không tham chiếu ADR-003 Accepted                | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                   | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Admin state transition over UC-118 aggregate; no schema delta.
Invalid-transition guard (APP-TC-603, state machine integrity), anti-self-approve (APP-TC-610), and atomicity
(APP-TC-INT-001) are the CRITICAL gates. Suspend/reinstate scope (ADR-003) is Accepted — resolved via project
analysis as a bundled part of this endpoint's state machine, not a separate UC.*
