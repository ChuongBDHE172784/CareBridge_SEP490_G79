# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-108: Approve Content Version

**Document ID:** `CB-CONTENT-TEST-005`
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
- TDS: `04_Implement/UC108_ApproveContentVersion/UC108_ApproveContentVersion_TDS.md` (`CB-CONTENT-IMP-005`)
- SRS Section 3.2.2.10
- Siblings: UC-105 (`CB-CONTENT-IMP-003`, Approved, infra oracle), UC-106 (`CB-CONTENT-IMP-004`, versionNo mechanism)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-108 Approve Content Version (Status=Draft)  |

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
| **Feature / UC ID**       | `UC-108`                                                                                                                |
| **Module**                | `Approve Content Version — content (admin decision, separation-of-duties)`                                             |
| **Spec gốc**              | `CB-CONTENT-IMP-005`                                                                                                      |
| **Priority**              | `P0 — High, Regular`                                                                                                       |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `content (ContentItem/ContentException — UC-105/106)`, `security`, `audit`                                                 |
| **Downstream Consumers**  | Public read paths, `UC-227 Unpublish Content`                                                                              |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-CONTENT-IMP-005 §17`, `ADR-001..ADR-005`                                                                                       |
| **Constraints Injected** | `C1 (RBAC SYSTEM_ADMIN)`, `C2 (PENDING_REVIEW-only transition)`, `C3 (reason for REJECT)`, `C4 (no invented version-history table)`, `C5 (no migration)`, `C6 (separation of duties)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | --------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS "Approve Content Version" gợi ý chọn 1-trong-N version   | ADR-001: KHÔNG có version-history table; chỉ duyệt bản edit hiện tại (Option A)       | Test CAV-TC-1003 (CRITICAL) assert KHÔNG có code path truy vấn version lịch sử; versionNoAtDecision chỉ phản ánh giá trị hiện tại |
| L2  | ContentStatus có PENDING_REVIEW không?                     | ADR-002: thêm mới, Java-enum-only, KHÔNG migration (verified no CHECK constraint)    | Test không cần migration setup; assert enum tồn tại qua compile-time contract      |
| L3  | Ai được duyệt?                                             | ADR-004: chỉ SYSTEM_ADMIN; CONTENT_ADMIN (tác giả) KHÔNG BAO GIỜ                      | Security test CAV-TC-1010: CONTENT_ADMIN → 403 (separation of duties, không chỉ generic RBAC test) |
| L4  | Transition từ DRAFT thẳng sang APPROVED?                   | ADR-003: chỉ PENDING_REVIEW mới decide; DRAFT/APPROVED/ARCHIVED → CNT-008            | Test CAV-TC-1003 cover cả 3 trạng thái sai                                          |
| L5  | Reason cho REJECT?                                        | ADR-005: bắt buộc (design decision)                                                  | Test CAV-TC-1005 gắn `ADR-005`                                                     |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (ContentApprovalController.decide() — @WebMvcTest)
├── Service (ContentApprovalServiceImpl.decide() — mock ContentItemRepository + audit)
├── Repository (findById/save — existing)
└── Integration (Full POST — Testcontainers, NO migration — Java-enum-only)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.2.10` | Admin approves/rejects content |
| `TDS ADR-001` | No version-history; approve current edit only |
| `TDS ADR-002` | PENDING_REVIEW Java-enum-only, no migration |
| `TDS ADR-003` | PENDING_REVIEW-only transition guard |
| `TDS ADR-004` | SYSTEM_ADMIN only; CONTENT_ADMIN excluded (separation of duties) |
| `content/entity/ContentStatus.java` | DRAFT/PENDING_REVIEW(new)/APPROVED/ARCHIVED |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | APPROVE PENDING_REVIEW → APPROVED                                          | `ContentApprovalServiceImpl.decide()`           | `CAV-TC-1001`       |
| TC-COND-002  | REJECT PENDING_REVIEW → DRAFT (reason)                                    | service                                          | `CAV-TC-1002`       |
| TC-COND-003  | Invalid transition (DRAFT/APPROVED/ARCHIVED → decide) → CNT-008           | service                                          | `CAV-TC-1003`       |
| TC-COND-004  | Not found → CNT-003 (reused, 404)                                         | service                                          | `CAV-TC-1004`       |
| TC-COND-005  | REJECT missing reason → CNT-009                                           | service                                          | `CAV-TC-1005`       |
| TC-COND-006  | `versionNoAtDecision` reflects current versionNo at decide time only       | service                                          | `CAV-TC-1006`       |
| TC-COND-007  | Audit called once with admin id + versionNo                               | `AuditService` mock verify                       | `CAV-TC-1007`       |
| TC-COND-008  | Non-SYSTEM_ADMIN (generic) → 403                                           | `@PreAuthorize`                                   | `CAV-TC-1008`       |
| TC-COND-009  | No JWT → 401                                                               | entry point                                       | `CAV-TC-1009`       |
| TC-COND-010  | CONTENT_ADMIN specifically → 403 (separation of duties)                   | `@PreAuthorize`                                   | `CAV-TC-1010`       |
| TC-COND-011  | Full POST integration — status changes; public read excludes PENDING_REVIEW | Testcontainers                                  | `CAV-TC-INT-001`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| State Transition Testing | PENDING_REVIEW → {APPROVED, DRAFT}; other states rejected | ADR-003 |
| Negative / Scope Guard | no version-history query/table invoked | ADR-001 — the central schema-gap guard |
| Error Guessing | missing reason, wrong role, self-approve by author | Security + ADR-005/004 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                     | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-1401`  | DB seed | `ContentItem{status: PENDING_REVIEW, versionNo: 3, authorUserId: A1}` | APPROVE/REJECT happy paths                     |
| `FX-1402`  | DB seed | `ContentItem{status: DRAFT}`                                         | Invalid transition (CNT-008)                    |
| `FX-1403`  | DB seed | `ContentItem{status: APPROVED}`                                     | Invalid transition — already approved           |
| `FX-1404`  | DB seed | `ContentItem{status: ARCHIVED}`                                     | Invalid transition — archived                   |
| `FX-1405`  | JWT     | `{role: "ROLE_SYSTEM_ADMIN"}`                                       | Auth happy path                                |
| `FX-1406`  | JWT     | `{sub: A1, role: "ROLE_CONTENT_ADMIN"}`                             | Auth failure — the item's OWN author (separation of duties, CAV-TC-1010) |
| `FX-1407`  | JWT     | `{role: "ROLE_MOTHER"}`                                             | Auth failure (generic 403)                     |
| `FX-1408`  | none    | No `Authorization`                                                 | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class ApproveContentVersionTestFactory {
    static final UUID ADMIN_ID  = UUID.fromString("f1600000-0000-0000-0000-0000000000ad");
    static final UUID AUTHOR_ID = UUID.fromString("f1600000-0000-0000-0000-0000000000a1");
    static final UUID CONTENT_ID = UUID.fromString("f1700000-0000-0000-0000-000000000001");

    static ContentItem makeItem(ContentStatus status, Integer versionNo) {
        return ContentItem.builder().id(CONTENT_ID).title("Bài viết").status(status)
                .versionNo(versionNo).authorUserId(AUTHOR_ID).type(ContentType.ARTICLE).build();
    }
    static ContentDecisionRequest makeRequest(ContentDecision decision, String reason) {
        return new ContentDecisionRequest(decision, reason);
    }
}
```

---

### CAV-TC-1001 — APPROVE PENDING_REVIEW → APPROVED

**Severity:** `HIGH`
**Feature Under Test:** `ContentApprovalServiceImpl.decide(id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-003`

**Preconditions:** `findById(CONTENT_ID)` → `FX-1401` (PENDING_REVIEW, versionNo=3)

**Expected Result (PASS):** saved `status == APPROVED`; `response.versionNoAtDecision() == 3`.

**Current Status:** 🔴 Not written

---

### CAV-TC-1002 — REJECT PENDING_REVIEW → DRAFT (reason)

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — reject path
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-003`, `ADR-005`

**Test Steps:** `{decision: REJECT, reason: "Thiếu trích dẫn"}`

**Expected Result (PASS):** saved `status == DRAFT` (reject returns to draft, not ARCHIVED); reason recorded.

**Current Status:** 🔴 Not written

---

### CAV-TC-1003 — Invalid transition (DRAFT/APPROVED/ARCHIVED) → 409 CNT-008

**Severity:** `CRITICAL`
**Feature Under Test:** `decide()` — transition guard (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-003 §Decision`

**Test Steps:** decide on `FX-1402` (DRAFT), `FX-1403` (APPROVED), `FX-1404` (ARCHIVED) — each with `decision: APPROVE`

**Expected Result (PASS):** all 3 throw `CNT-008`, 409; no status change.
**Expected Result (FAIL):** any non-PENDING_REVIEW item gets decided → allows skipping review (e.g. DRAFT→APPROVED directly), BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Proves content cannot bypass review by going straight from DRAFT to APPROVED.

---

### CAV-TC-1004 — Not found → 404 CNT-003 (reused)

**Severity:** `HIGH`
**Feature Under Test:** lookup
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC-106 TDS §10 CNT-003 (reused)`

**Expected Result (PASS):** throws `CNT-003`, 404.

**Current Status:** 🔴 Not written

---

### CAV-TC-1005 — REJECT missing reason → 400 CNT-009

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — reason guard (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-005` (design decision, Open)

**Test Steps:** REJECT with `reason=null` and `reason="  "`

**Expected Result (PASS):** both throw `CNT-009`, 400; no status change.

**Current Status:** 🔴 Not written

---

### CAV-TC-1006 — `versionNoAtDecision` reflects current versionNo only (no history lookup)

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-001 scope guard — no invented version-history query
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-001 §Decision (Option A)`

**Test Steps:** decide APPROVE on `FX-1401` (versionNo=3)

**Expected Result (PASS):**
- `response.versionNoAtDecision() == 3` (the item's CURRENT versionNo, read directly off the row)
- No repository/query for any `content_item_versions`-style table is invoked (verify no such mock/dependency exists in the service — the DTO/interface contract in §8 has no version-history repository)

**Expected Result (FAIL):** service attempts to query a version-history table that doesn't exist → compile
error or hallucinated schema, BLOCKING (this is the direct AP-AI-002 anti-pattern from the TDS).

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This test enforces the ADR-001 scope boundary — it's the regression guard against
an implementer "helpfully" inventing a version-history mechanism not in this TDS's schema section.

---

### CAV-TC-1007 — Audit called once with admin id + versionNo

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` with admin id, content id, and versionNo captured in details.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### CAV-TC-1008 — Non-SYSTEM_ADMIN (generic) → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16`

**Preconditions:** JWT role MOTHER (FX-1407)

**Expected Result (PASS):** 403 (assert status; CNT-004/ACCESS_DENIED verify against real code).

**Current Status:** 🔴 Not written

---

### CAV-TC-1009 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** UC-105 §10 (verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### CAV-TC-1010 — CONTENT_ADMIN specifically → 403 (separation of duties)

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize` — the item's own author cannot self-approve
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS ADR-004 §Decision`, `§16 Auth Matrix (CONTENT_ADMIN = ❌ never)`

**Preconditions:** JWT for the CONTENT_ADMIN who authored `FX-1401` itself (`sub == AUTHOR_ID`, FX-1406)

**Test Steps:** the item's own author attempts `POST .../content/{id}/decision {decision: APPROVE}`

**Expected Result (PASS):** 403 — rejected purely by role (`CONTENT_ADMIN` is never in the allowed-role set for
this endpoint), regardless of whether they authored the item. This is a structural guarantee, not an
ownership check — CONTENT_ADMIN never reaches this endpoint at all.

**Expected Result (FAIL):** the author's own CONTENT_ADMIN token is accepted → an author can rubber-stamp
their own content, defeating the entire purpose of a review gate, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This is functionally identical to CAV-TC-1008 at the HTTP layer (both expect 403)
but is kept as its own severity-tagged test because it encodes the *reason* (separation of duties) rather
than generic RBAC — a future reviewer should not merge/delete it as "duplicate of 1008."

---

### INTEGRATION TEST CASES

### CAV-TC-INT-001 — Full POST — status changes; public read excludes PENDING_REVIEW

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/content/{id}/decision` end to end + downstream visibility
**Test File:** `src/test/java/com/carebridge/backend/integration/ContentApprovalIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`

**Preconditions:** Testcontainer + Flyway (NO new migration — Java-enum-only); seed `FX-1401` (PENDING_REVIEW); admin JWT

**Test Steps:**
1. Before decide: call the public content-read path (UC-82/224 style query filtered `status='APPROVED'`) → assert the item is NOT visible (still PENDING_REVIEW)
2. POST APPROVE → 200
3. Re-fetch DB row → `status='APPROVED'`
4. Call the public read path again → assert the item IS now visible

**Expected Result (PASS):** item invisible pre-approval, visible post-approval — proves `PENDING_REVIEW` never
leaks into the public `APPROVED`-filtered index/query, and approval actually publishes it.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `CAV-TC-1001`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `CAV-TC-1002`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `CAV-TC-1003`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | ★ transition guard   |
| `CAV-TC-1004`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `CAV-TC-1005`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `CAV-TC-1006`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | ★ ADR-001 scope guard |
| `CAV-TC-1007`     | `ContentApprovalServiceImplTest.java`                | `[ ]`               | —                     | —                    |
| `CAV-TC-1008`     | `ContentApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `CAV-TC-1009`     | `ContentApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `CAV-TC-1010`     | `ContentApprovalControllerSecurityTest.java`         | `[ ]`               | —                     | ★ separation of duties |
| `CAV-TC-INT-001`  | `ContentApprovalIntegrationTest.java`                | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// ContentApprovalServiceImpl.java — Red Phase stub
@Override
public ContentDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `CAV-TC-1001`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `CAV-TC-1003`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `CAV-TC-1006`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `CAV-TC-1008`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `CAV-TC-1010`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `CAV-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-105/106 deployed (ContentItem with versionNo, ContentException)
- [ ] **ADR-001 (no version-history, Option A) confirmed by Product/Tech Lead** — BLOCKING
- [ ] No migration needed — confirmed (verified no CHECK constraint)
- [ ] Fixtures FX-1401..FX-1408 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=ContentApprovalServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=ContentApprovalControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=ContentApprovalIntegrationTest` — all PASS (Testcontainers)
- [ ] CAV-TC-1003: transition guard (PENDING_REVIEW-only) — VERIFIED (CRITICAL, prevents review bypass)
- [ ] CAV-TC-1006: no invented version-history query — VERIFIED (CRITICAL, ADR-001 scope guard)
- [ ] CAV-TC-1010: CONTENT_ADMIN cannot self-approve — VERIFIED (CRITICAL separation of duties)
- [ ] CAV-TC-INT-001: PENDING_REVIEW never leaks to public before approval — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`ContentStatus.PENDING_REVIEW`, `ContentDecision`, DTOs, CNT-008/009)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited (ADR + verified schema check)

### Suspension Criteria
- ADR-001 (Option A limitation) not confirmed by Product; UC-105/106 not deployed; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/
# No migration to revert (Java-enum-only change to ContentStatus).
# Re-run UC-105/106 tests to confirm no regression from adding PENDING_REVIEW to the enum.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-003/ADR-004                                      | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | CAV-TC-1001..1007 PASS với throw stub                                          | `[x]`   | G-2 ★       |
| AP-AI-002 | Invented Schema          | TC không assert vắng mặt version-history query (thiếu CAV-TC-1006)             | `[x]`   | G-1 ★★      |
| AP-AI-003 | Self-Approve             | Không có TC assert CONTENT_ADMIN bị chặn (thiếu CAV-TC-1010)                    | `[x]`   | G-1 ★       |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8 (vd ContentVersionRepository)                | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Admin decision over UC-105/106 aggregate; no schema delta
(verified). The scope guard against inventing a version-history mechanism (CAV-TC-1006) and
separation-of-duties (CAV-TC-1010) are the two most distinctive CRITICAL gates in this spec — both directly
enforce ADR-001's explicit, human-reviewed decision not to build full versioning in v1.*
