# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-108: Approve Content Version

**Document ID:** `CB-CONTENT-TEST-005`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented`
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
| 2026-07-02 | AI Agent — Amelia (Dev Agent) | RED→GREEN→REFACTOR complete. All 11 spec'd test cases (CAV-TC-1001..1010, CAV-TC-INT-001 — 14 test executions incl. parameterized CAV-TC-1003/1005) implemented and PASSING. Red Gate: 10 service-level executions FAILED against the `UnsupportedOperationException` stub as required; the 3 security tests (CAV-TC-1008/1009/1010) and CAV-TC-INT-001 passed already at Red Gate — same verified finding as UC-100/101/102/106/107 (URL-matcher role/JWT denial precedes `DispatcherServlet`/the service stub; the integration test mocks `ContentApprovalService` directly and never touches the stub). Full regression: 778 tests, 33 pre-existing errors (unchanged baseline), 0 new failures. CAV-TC-INT-001 uses the established mocked-service-layer adaptation (no Testcontainers/real-DB harness exists in this codebase) — see its file-level note. |
| 2026-07-02 | AI Agent — Amelia (Dev Agent) | **Post-implementation advisor review found two real gaps in the initial GREEN implementation, missed by the spec'd 11 TCs**: (1) TDS §6.1's explicit sequence step `item.setPublishedAt(now()) [if not already set]` on APPROVE had NOT been implemented — `searchByFilters()` orders by `publishedAt DESC NULLS LAST`, so newly-approved content would have silently sunk to the bottom of the public feed; (2) BR-AUDIT-001 lists `reason` as part of the audit record, but the original audit detail string omitted it on REJECT (lost from the trail, only present in the transient HTTP response). Both were fixed in `ContentApprovalServiceImpl.decide()` (added `publishedAt` assignment guarded on APPROVE + null-check; appended `reason=...` to the audit detail string when present), then covered by 3 new tests: `CAV-TC-1011` (publishedAt set on APPROVE when null), `CAV-TC-1012` (publishedAt NOT overwritten if already set), `CAV-TC-1013` (audit detail includes reason on REJECT). Total service-level test methods: 10 (13 executions incl. parameterized, per surefire); 17 total across all three UC-108 test classes (13 service + 3 security + 1 integration). Full regression re-run: 781 tests (778 + 3 new), 33 pre-existing errors (unchanged baseline — same 6 known DB-dependent classes: `BackendApplicationTests`, `ExerciseControllerDetailSecurityTest`, `ExerciseDetailIntegrationTest`, `RegistrationIntegrationTest`, `AuthServiceRegisterTest`, `TriageIntegrationTest`), 0 new failures. |

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
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-003`

**Preconditions:** `findById(CONTENT_ID)` → `FX-1401` (PENDING_REVIEW, versionNo=3)

**Expected Result (PASS):** saved `status == APPROVED`; `response.versionNoAtDecision() == 3`.

**Current Status:** 🟢 Passing

---

### CAV-TC-1002 — REJECT PENDING_REVIEW → DRAFT (reason)

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — reject path
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-003`, `ADR-005`

**Test Steps:** `{decision: REJECT, reason: "Thiếu trích dẫn"}`

**Expected Result (PASS):** saved `status == DRAFT` (reject returns to draft, not ARCHIVED); reason recorded.

**Current Status:** 🟢 Passing

---

### CAV-TC-1003 — Invalid transition (DRAFT/APPROVED/ARCHIVED) → 409 CNT-008

**Severity:** `CRITICAL`
**Feature Under Test:** `decide()` — transition guard (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-003 §Decision`

**Test Steps:** decide on `FX-1402` (DRAFT), `FX-1403` (APPROVED), `FX-1404` (ARCHIVED) — each with `decision: APPROVE`

**Expected Result (PASS):** all 3 throw `CNT-008`, 409; no status change.
**Expected Result (FAIL):** any non-PENDING_REVIEW item gets decided → allows skipping review (e.g. DRAFT→APPROVED directly), BLOCKING.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ Proves content cannot bypass review by going straight from DRAFT to APPROVED.

---

### CAV-TC-1004 — Not found → 404 CNT-003 (reused)

**Severity:** `HIGH`
**Feature Under Test:** lookup
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC-106 TDS §10 CNT-003 (reused)`

**Expected Result (PASS):** throws `CNT-003`, 404.

**Current Status:** 🟢 Passing

---

### CAV-TC-1005 — REJECT missing reason → 400 CNT-009

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — reason guard (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-005` (design decision, Open)

**Test Steps:** REJECT with `reason=null` and `reason="  "`

**Expected Result (PASS):** both throw `CNT-009`, 400; no status change.

**Current Status:** 🟢 Passing

---

### CAV-TC-1006 — `versionNoAtDecision` reflects current versionNo only (no history lookup)

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-001 scope guard — no invented version-history query
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-001 §Decision (Option A)`

**Test Steps:** decide APPROVE on `FX-1401` (versionNo=3)

**Expected Result (PASS):**
- `response.versionNoAtDecision() == 3` (the item's CURRENT versionNo, read directly off the row)
- No repository/query for any `content_item_versions`-style table is invoked (verify no such mock/dependency exists in the service — the DTO/interface contract in §8 has no version-history repository)

**Expected Result (FAIL):** service attempts to query a version-history table that doesn't exist → compile
error or hallucinated schema, BLOCKING (this is the direct AP-AI-002 anti-pattern from the TDS).

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ This test enforces the ADR-001 scope boundary — it's the regression guard against
an implementer "helpfully" inventing a version-history mechanism not in this TDS's schema section.

---

### CAV-TC-1007 — Audit called once with admin id + versionNo

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` with admin id, content id, and versionNo captured in details.

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

### CAV-TC-1008 — Non-SYSTEM_ADMIN (generic) → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16`

**Preconditions:** JWT role MOTHER (FX-1407)

**Expected Result (PASS):** 403 (assert status; CNT-004/ACCESS_DENIED verify against real code).

**Current Status:** 🟢 Passing

---

### CAV-TC-1009 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** UC-105 §10 (verify)

**Expected Result (PASS):** 401.

**Current Status:** 🟢 Passing

---

### CAV-TC-1010 — CONTENT_ADMIN specifically → 403 (separation of duties)

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize` — the item's own author cannot self-approve
**Test File:** `src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS ADR-004 §Decision`, `§16 Auth Matrix (CONTENT_ADMIN = ❌ never)`

**Preconditions:** JWT for the CONTENT_ADMIN who authored `FX-1401` itself (`sub == AUTHOR_ID`, FX-1406)

**Test Steps:** the item's own author attempts `POST .../content/{id}/decision {decision: APPROVE}`

**Expected Result (PASS):** 403 — rejected purely by role (`CONTENT_ADMIN` is never in the allowed-role set for
this endpoint), regardless of whether they authored the item. This is a structural guarantee, not an
ownership check — CONTENT_ADMIN never reaches this endpoint at all.

**Expected Result (FAIL):** the author's own CONTENT_ADMIN token is accepted → an author can rubber-stamp
their own content, defeating the entire purpose of a review gate, BLOCKING.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ This is functionally identical to CAV-TC-1008 at the HTTP layer (both expect 403)
but is kept as its own severity-tagged test because it encodes the *reason* (separation of duties) rather
than generic RBAC — a future reviewer should not merge/delete it as "duplicate of 1008."

---

### INTEGRATION TEST CASES

### CAV-TC-INT-001 — Full POST — status changes; public read excludes PENDING_REVIEW

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/content/{id}/decision` end to end + downstream visibility
**Test File:** `src/test/java/com/carebridge/backend/integration/ContentApprovalIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`

**Preconditions:** Testcontainer + Flyway (NO new migration — Java-enum-only); seed `FX-1401` (PENDING_REVIEW); admin JWT

**Test Steps:**
1. Before decide: call the public content-read path (UC-82/224 style query filtered `status='APPROVED'`) → assert the item is NOT visible (still PENDING_REVIEW)
2. POST APPROVE → 200
3. Re-fetch DB row → `status='APPROVED'`
4. Call the public read path again → assert the item IS now visible

**Expected Result (PASS):** item invisible pre-approval, visible post-approval — proves `PENDING_REVIEW` never
leaks into the public `APPROVED`-filtered index/query, and approval actually publishes it.

**Current Status:** 🟢 Passing

---

### POST-IMPLEMENTATION ADVISOR-DRIVEN TEST CASES (not in original 11-TC spec)

### CAV-TC-1011 — APPROVE sets publishedAt when not already set

**Severity:** `HIGH`
**Feature Under Test:** `decide()` — TDS §6.1 sequence step, `searchByFilters()` ordering correctness
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `TDS §6.1 sequence diagram (line 218)`; `ContentRepository.searchByFilters()` — `ORDER BY c.publishedAt DESC NULLS LAST`

**Expected Result (PASS):** on APPROVE of an item with `publishedAt == null`, the saved row has `publishedAt` set to (approximately) the decision time.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ Found missing during post-implementation advisor review — TDS §6.1 explicitly specifies this step but it was omitted from the original GREEN implementation and from the original 11 TCs. Without it, `searchByFilters()`'s `ORDER BY publishedAt DESC NULLS LAST` would silently sink newly-approved content to the bottom of the public feed.

---

### CAV-TC-1012 — APPROVE does not overwrite an existing publishedAt

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — idempotency of the publish timestamp
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `TDS §6.1` — "[if not already set]"

**Expected Result (PASS):** on APPROVE of an item that already has a `publishedAt`, the saved value is unchanged.

**Current Status:** 🟢 Passing

---

### CAV-TC-1013 — REJECT audit detail includes the reason

**Severity:** `MEDIUM`
**Feature Under Test:** `decide()` — audit traceability (BR-AUDIT-001)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `TDS §2 Ma trận Truy vết, BR-AUDIT-001` — "Quyết định audit log (ai, item nào, versionNo, kết quả, reason)"

**Expected Result (PASS):** the `AuditService.log(...)` details argument for a REJECT decision contains the submitted reason string.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ Found missing during post-implementation advisor review — the original audit detail string was `"decision=X versionNo=Y"` with no reason; BR-AUDIT-001 explicitly requires it in the audit record, not just the transient HTTP response.

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `CAV-TC-1001`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | —                    |
| `CAV-TC-1002`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | —                    |
| `CAV-TC-1003`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | ★ transition guard   |
| `CAV-TC-1004`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | —                    |
| `CAV-TC-1005`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | —                    |
| `CAV-TC-1006`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | ★ ADR-001 scope guard |
| `CAV-TC-1007`     | `ContentApprovalServiceImplTest.java`                | `[x]`               | Passed                | —                    |
| `CAV-TC-1008`     | `ContentApprovalControllerSecurityTest.java`         | `[x]` (n/a — passed pre-stub, see §5.1 note) | Passed | —                    |
| `CAV-TC-1009`     | `ContentApprovalControllerSecurityTest.java`         | `[x]` (n/a — passed pre-stub, see §5.1 note) | Passed | —                    |
| `CAV-TC-1010`     | `ContentApprovalControllerSecurityTest.java`         | `[x]` (n/a — passed pre-stub, see §5.1 note) | Passed | ★ separation of duties |
| `CAV-TC-INT-001`  | `ContentApprovalIntegrationTest.java`                | `[x]` (n/a — mocks service, see §5.1 note) | Passed | —                    |
| `CAV-TC-1011`     | `ContentApprovalServiceImplTest.java`                | n/a — added post-implementation, see note below | Passed | ★ publishedAt gap (advisor-found) |
| `CAV-TC-1012`     | `ContentApprovalServiceImplTest.java`                | n/a — added post-implementation, see note below | Passed | —                    |
| `CAV-TC-1013`     | `ContentApprovalServiceImplTest.java`                | n/a — added post-implementation, see note below | Passed | ★ audit reason gap (advisor-found) |

**Note on CAV-TC-1011/1012/1013:** these were not part of the original 11-TC Red Gate cycle. A
post-implementation advisor review found the `publishedAt` and audit-reason gaps described in the CHANGELOG;
the production-code fix and these 3 regression tests were written together in the same pass, then run once
and confirmed GREEN. No separate RED confirmation was executed for these 3 — an honest deviation from the
strict RED-first protocol for this small, targeted post-review addition. By construction, they are not
uniformly meaningful bug-guards: `CAV-TC-1011` (asserts `publishedAt` non-null post-decide) and `CAV-TC-1013`
(asserts audit detail contains the reason) WOULD have failed against the pre-fix code — they are real
regression guards. `CAV-TC-1012` (asserts an already-set `publishedAt` is left unchanged) WOULD have PASSED
against the pre-fix code too, since the pre-fix code never touched the field at all — it guards against a
*future* regression (e.g. someone later unconditionally overwriting `publishedAt`), not the bug that was
actually found.

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
| `CAV-TC-1001`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed |
| `CAV-TC-1002`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed |
| `CAV-TC-1003`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed (all 3 parameterized statuses) |
| `CAV-TC-1004`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed |
| `CAV-TC-1005`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed (both parameterized reasons) |
| `CAV-TC-1006`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed |
| `CAV-TC-1007`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | Stub throws as designed |
| `CAV-TC-1008`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☑ PASS     | **Correction to TDS assumption**: `@PreAuthorize` and the `SecurityConfig` URL-matcher rule were added in the SAME Red-Phase step as the controller/stub (per this codebase's established workflow — same finding as UC-100/101/102/106/107). RBAC denial happens at the filter chain, before `DispatcherServlet` ever reaches the (stubbed) service — so this test already returns 403 correctly even with the stub throwing `UnsupportedOperationException`. Not a Green-from-Birth violation: it tests RBAC wiring, not business logic, and RBAC wiring genuinely does not depend on the service implementation. |
| `CAV-TC-1009`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403 test, but N/A here) | ☐ FAIL ☑ PASS | Same as CAV-TC-1008 — no-JWT 401 is enforced by the JWT filter, independent of the service stub. |
| `CAV-TC-1010`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☑ PASS     | Same correction as CAV-TC-1008 — CONTENT_ADMIN is structurally excluded from the allowed-role set at the URL-matcher level, independent of the service stub. |
| `CAV-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☑ PASS     | `ContentApprovalService` is `@MockitoBean`-mocked in this test (established adaptation, no Testcontainers/real-DB harness in this codebase — same finding as UC-100/101/102/106/107); the mock returns a canned response regardless of the real stub's behavior, so this test never touches the stub. |

**Red Gate Evidence:** Stub commit — not committed — verified locally via `./mvnw test` against the working
tree (uncommitted, `dev` branch, consistent with the rest of this batch). Tất cả service-level FAIL? ☑ Yes
(10/10 executions across CAV-TC-1001..1007) → GATE-2 PASS. The 4 non-service-level tests (CAV-TC-1008/1009/
1010/INT-001) passed at Red Gate for the structural reasons documented above, not because of a stub defect —
this is the established, previously-reviewed pattern for this codebase's security/integration test layer, not
an anomaly specific to this UC.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] UC-105/106 deployed (ContentItem with versionNo, ContentException)
- [x] **ADR-001 (no version-history, Option A) confirmed by Product/Tech Lead** — BLOCKING (confirmed via user's blanket batch-approval directive)
- [x] No migration needed for `content_items.status` — confirmed (verified no CHECK constraint); note a separate migration WAS needed for `AuditAction.CONTENT_DECIDED` (see TDS §11.2)
- [x] Fixtures FX-1401..FX-1408 prepared (inlined via `ApproveContentVersionTestFactory`, not separate seed files — same pattern as UC-102/106/107)

### Exit Criteria (DoD)
- [x] `./mvnw test -Dtest=ContentApprovalServiceImplTest` — all PASS (10/10 executions)
- [x] `./mvnw test -Dtest=ContentApprovalControllerSecurityTest` — all PASS (3/3)
- [x] `./mvnw test -Dtest=ContentApprovalIntegrationTest` — all PASS (1/1). Note: no `mvnw verify`/Testcontainers step — none exists in this codebase (verified, same finding as UC-100/101/102/106/107); this ran via the standard `test` phase against a mocked service layer.
- [x] CAV-TC-1003: transition guard (PENDING_REVIEW-only) — VERIFIED (CRITICAL, prevents review bypass)
- [x] CAV-TC-1006: no invented version-history query — VERIFIED (CRITICAL, ADR-001 scope guard)
- [x] CAV-TC-1010: CONTENT_ADMIN cannot self-approve — VERIFIED (CRITICAL separation of duties)
- [x] CAV-TC-INT-001: PENDING_REVIEW never leaks to public before approval — VERIFIED AT THE HTTP-WIRING LEVEL ONLY (response reflects PENDING_REVIEW→APPROVED). NOT independently re-verified against a real DB read-path query end-to-end (no Testcontainers/real-DB harness exists in this codebase); the guarantee that `PENDING_REVIEW` is excluded from `status='APPROVED'` filters is a logical consequence of `ContentRepository`'s unchanged filter clauses, not a re-executed integration assertion. Documented honestly per Truthful Sync, not silently upgraded to fully verified.
- [x] No business logic in controller — verified by inspection (`ContentApprovalController.decide()` only calls `SecurityUtils`-free `@Valid`/delegate to service)

**Exit Criteria bổ sung — CASE 2.0:**
- [x] Red Gate — all 10 service-level executions FAIL with throw stub (see §5.1)
- [x] Contract Existence — `./mvnw compile` clean (`ContentStatus.PENDING_REVIEW`, `ContentDecision`, DTOs, CNT-008/009)
- [x] Props Isolation — `ApproveContentVersionTestFactory` used across all service-level tests
- [x] Oracle Source — cited (ADR + verified schema check) in every TC's Oracle Source field

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
- [x] Post-implementation re-check: no version-history table/repository was introduced (AP-AI-002 Invented
      Schema — clean, `ContentApprovalServiceImpl` depends only on `ContentRepository` + `AuditService`); no
      self-approve path exists (AP-AI-003 — clean, verified by CAV-TC-1010); no layer violation (AP-AI-004 —
      controller delegates only); no hallucinated contract (AP-AI-005 — all imports trace to §8).

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Implemented (2026-07-02). Admin decision over UC-105/106 aggregate; no
schema delta for `content_items.status` (verified) — separate migration required for `AuditAction.
CONTENT_DECIDED`. The scope guard against inventing a version-history mechanism (CAV-TC-1006) and
separation-of-duties (CAV-TC-1010) are the two most distinctive CRITICAL gates in this spec — both directly
enforce ADR-001's explicit, human-reviewed decision not to build full versioning in v1. Both verified GREEN.*
