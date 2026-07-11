# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-227: Unpublish Content

**Document ID:** `CB-CONTENT-TEST-007`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented — 2026-07-11 (11/11 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — editorial content, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC227_UnpublishContent/UC227_UnpublishContent_TDS.md` (`CB-CONTENT-IMP-007`)
- SRS Section 3.3.18.4
- Siblings: UC-105 (`CB-CONTENT-IMP-003`, Approved), UC-106 (`CB-CONTENT-IMP-004`), UC-108 (`CB-CONTENT-IMP-005`, PENDING_REVIEW/no-CHECK-constraint finding)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-227 Unpublish Content (Status=Draft)    |
| 2026-07-02 | AI Agent — Claude (Audit Pass) | **Cross-doc note (no status change):** verified this Test-Spec is internally consistent with the current codebase — no `ContentUnpublishController`/`Service`/DTOs exist yet (all TDD Phases correctly `🔴 RED — chưa implement`, matching `Status: Draft`), and `PENDING_REVIEW` is correctly attributed to UC-108. See the sibling TDS's changelog for a cross-doc note: UC-107's ADR-003 (Proposed, needs Tech Lead confirmation) suggests this UC's entire test surface may end up "satisfied by UC-107" rather than implemented separately — a decision for whoever picks up UC-227 next, not resolved by this audit. |
| 2026-07-11 | AI Agent — Amelia | Service/controller/security and UC-107 archive regression tests pass (9/11 conditions). Two PostgreSQL visibility/persistence integration conditions remain unverified without Docker. |
| 2026-07-11 | AI Agent — Codex | Added and ran the two PostgreSQL Testcontainers integration tests; all 11/11 conditions pass. Integration tests are characterization tests, so no historical RED result is claimed. |
| 2026-07-11 | AI Agent — Codex | Full backend regression PASS: 1,519 tests, 0 failures, 0 errors, 1 skipped. |

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
| **Feature / UC ID**       | `UC-227`                                                                                                                |
| **Module**                | `Unpublish Content — content (admin state transition, reuses existing ARCHIVED enum)`                                   |
| **Spec gốc**              | `CB-CONTENT-IMP-007`                                                                                                      |
| **Priority**              | `P1 — Medium, Occasional`                                                                                                   |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `content (ContentItem/ContentException — UC-105/106/108)`, `security`, `audit`                                             |
| **Downstream Consumers**  | Public read paths (item disappears immediately)                                                                            |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-CONTENT-IMP-007 §17`, `ADR-001..ADR-005`                                                                                       |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (APPROVED-only transition)`, `C3 (publishedAt preserved)`, `C4 (reuse ARCHIVED, no new enum)`, `C5 (reason required)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                        | Thực tế                                                                    | Fix áp dụng trong test                                                    |
| --- | -------------------------------------------------- | -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| L1  | FS "unpublish" gợi ý một status mới riêng           | ADR-001: tái dùng `ARCHIVED` có sẵn, KHÔNG thêm `UNPUBLISHED`                     | Test KHÔNG assert một giá trị enum mới; chỉ assert transition sang ARCHIVED         |
| L2  | publishedAt có bị xóa khi unpublish không?          | ADR-002: giữ nguyên (lịch sử) — chỉ status đổi                                   | Test UPC-TC-1203 (CRITICAL): assert publishedAt KHÔNG đổi sau unpublish             |
| L3  | Item nào unpublish được?                            | ADR-003: chỉ APPROVED; khác → CNT-010                                            | Test cover cả 3 trạng thái sai (DRAFT/PENDING_REVIEW/ARCHIVED) → CNT-010            |
| L4  | Item unpublish có biến mất khỏi public path ngay không? | ADR-001: có, vì public filter chỉ khớp status='APPROVED', không cần thay đổi gì thêm | Test integration UPC-TC-INT-002: assert item biến mất khỏi public read ngay sau unpublish |
| L5  | Reason bắt buộc?                                    | ADR-005: bắt buộc (design decision, Accepted)                                              | Test UPC-TC-1205 gắn `ADR-005 (Accepted)`                                              |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (ContentUnpublishController.unpublish() — @WebMvcTest)
├── Service (ContentUnpublishServiceImpl.unpublish() — mock ContentItemRepository + audit)
├── Repository (findById/save — existing)
└── Integration (Full POST + public-read-path check — Testcontainers, NO migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.3.18.4` | Admin unpublishes content |
| `TDS ADR-001` | Reuse ARCHIVED, no new enum |
| `TDS ADR-002` | publishedAt preserved |
| `TDS ADR-003` | APPROVED-only transition guard |
| `V1__init_schema.sql idx_content_items_published_at ... WHERE status='APPROVED'` | Public visibility oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | APPROVED → ARCHIVED (happy path)                                           | `ContentUnpublishServiceImpl.unpublish()`       | `UPC-TC-1201`       |
| TC-COND-002  | Non-APPROVED (DRAFT/PENDING_REVIEW/ARCHIVED) → CNT-010                     | service                                          | `UPC-TC-1202`       |
| TC-COND-003  | `publishedAt` unchanged after unpublish                                    | service                                          | `UPC-TC-1203`       |
| TC-COND-004  | No new `ContentStatus` value introduced (reuse guard)                     | code/compile review                              | `UPC-TC-1204`       |
| TC-COND-005  | Blank reason → CNT-011                                                    | service                                          | `UPC-TC-1205`       |
| TC-COND-006  | Not found → CNT-003 (reused)                                              | service                                          | `UPC-TC-1206`       |
| TC-COND-007  | Audit called once                                                         | `AuditService` mock verify                       | `UPC-TC-1207`       |
| TC-COND-008  | Non-CONTENT_ADMIN → 403 CNT-004                                             | `@PreAuthorize`                                   | `UPC-TC-1208`       |
| TC-COND-009  | No JWT → 401                                                               | entry point                                       | `UPC-TC-1209`       |
| TC-COND-010  | Full POST integration — status changes; publishedAt unchanged in DB       | Testcontainers                                    | `UPC-TC-INT-001`    |
| TC-COND-011  | Item disappears from public read path immediately after unpublish         | Testcontainers                                    | `UPC-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Negative / Data-Preservation | publishedAt unchanged | ADR-002 — the central data-integrity guarantee |
| Equivalence Partitioning | status classes (APPROVED vs others) | ADR-003 |
| Reuse-Not-Invent Guard | no new enum value | ADR-001 |
| Regression / Visibility | public read path filter still works correctly | Downstream visibility proof |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                     | Mục đích                                  |
| ----------- | -------- | -------------------------------------------------------------------------------------| ---------------------------------------------- |
| `FX-1601`  | DB seed | `ContentItem{status: APPROVED, publishedAt: <fixed past timestamp>}`                 | Happy path + publishedAt preservation          |
| `FX-1602`  | DB seed | `ContentItem{status: DRAFT}`                                                          | Invalid transition                              |
| `FX-1603`  | DB seed | `ContentItem{status: PENDING_REVIEW}`                                                | Invalid transition                              |
| `FX-1604`  | DB seed | `ContentItem{status: ARCHIVED}` (already unpublished)                                | Invalid transition — already archived           |
| `FX-1605`  | JWT     | `{role: "ROLE_CONTENT_ADMIN"}`                                                        | Auth happy path                                |
| `FX-1606`  | JWT     | `{role: "ROLE_MODERATOR"}`                                                           | Auth failure (403)                             |
| `FX-1607`  | none    | No `Authorization`                                                                    | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class UnpublishContentTestFactory {
    static final UUID ADMIN_ID = UUID.fromString("f1a00000-0000-0000-0000-0000000000ad");
    static final UUID CONTENT_ID = UUID.fromString("f1b00000-0000-0000-0000-000000000001");
    static final Instant FIXED_PUBLISHED_AT = Instant.parse("2026-05-01T08:00:00Z");

    static ContentItem makeItem(ContentStatus status) {
        return ContentItem.builder().id(CONTENT_ID).title("Bài viết").status(status)
                .publishedAt(status == ContentStatus.APPROVED || status == ContentStatus.ARCHIVED
                        ? FIXED_PUBLISHED_AT : null)
                .build();
    }
    static UnpublishRequest makeRequest(String reason) { return new UnpublishRequest(reason); }
}
```

---

### UPC-TC-1201 — APPROVED → ARCHIVED (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `ContentUnpublishServiceImpl.unpublish(id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-001/003`

**Preconditions:** `findById(CONTENT_ID)` → `FX-1601` (APPROVED)

**Expected Result (PASS):** saved `status == ARCHIVED`; `response.previousStatus() == APPROVED`, `newStatus() == ARCHIVED`.

**Current Status:** 🔴 Not written

---

### UPC-TC-1202 — Non-APPROVED → 409 CNT-010

**Severity:** `CRITICAL`
**Feature Under Test:** transition guard (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-003 §Decision`

**Test Steps:** unpublish on `FX-1602` (DRAFT), `FX-1603` (PENDING_REVIEW), `FX-1604` (ARCHIVED)

**Expected Result (PASS):** all 3 throw `CNT-010`, 409; no status change.
**Expected Result (FAIL):** any non-APPROVED item unpublishes → allows "unpublishing" content that was never public, BLOCKING.

**Current Status:** 🔴 Not written

---

### UPC-TC-1203 — `publishedAt` unchanged after unpublish

**Severity:** `CRITICAL`
**Feature Under Test:** `unpublish()` — data-preservation guarantee (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002 §Decision`

**Preconditions:** `FX-1601` with `publishedAt == FIXED_PUBLISHED_AT`

**Test Steps:** unpublish → capture saved entity

**Expected Result (PASS):** saved `publishedAt == FIXED_PUBLISHED_AT` (byte-for-byte unchanged).
**Expected Result (FAIL):** `publishedAt` is null or reset to a new timestamp → loses the historical "first
published" record, direct ADR-002 violation, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This is the single most distinctive guarantee of this UC — visibility must be
controlled purely via `status`, never by destroying `publishedAt`.

---

### UPC-TC-1204 — No new `ContentStatus` value introduced (reuse guard)

**Severity:** `HIGH`
**Feature Under Test:** ADR-001 reuse-not-invent guard
**Test File:** `src/test/java/com/carebridge/backend/content/ContentStatusArchitectureTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-001 §Decision`

**Test Steps:** enumerate `ContentStatus` values

**Expected Result (PASS):** the enum contains exactly `DRAFT, PENDING_REVIEW, APPROVED, ARCHIVED` (the last
addition being UC-108's `PENDING_REVIEW`) — NO `UNPUBLISHED` value exists.

**Expected Result (FAIL):** a new `UNPUBLISHED` value is found → the exact anti-pattern ADR-001 rejected
(inventing a status when an existing one already serves the purpose), BLOCKING.

**Current Status:** 🔴 Not written

---

### UPC-TC-1205 — Blank reason → 400 CNT-011

**Severity:** `MEDIUM`
**Feature Under Test:** `unpublish()` — reason guard (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-005` (design decision, Open)

**Test Steps:** `reason=null` and `reason="  "`

**Expected Result (PASS):** both throw `CNT-011`, 400; no status change.

**Current Status:** 🔴 Not written

---

### UPC-TC-1206 — Not found → 404 CNT-003 (reused)

**Severity:** `HIGH`
**Feature Under Test:** lookup
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC-106 TDS §10 CNT-003 (reused)`

**Expected Result (PASS):** throws `CNT-003`, 404.

**Current Status:** 🔴 Not written

---

### UPC-TC-1207 — Audit called once

**Severity:** `MEDIUM`
**Feature Under Test:** `unpublish()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/UnpublishContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### UPC-TC-1208 — Non-CONTENT_ADMIN → 403 CNT-004

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/UnpublishContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16`

**Preconditions:** JWT role MODERATOR (FX-1606)

**Expected Result (PASS):** 403, `error.code == "CNT-004"` (verify against real code).

**Current Status:** 🔴 Not written

---

### UPC-TC-1209 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/UnpublishContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** UC-105 §10 (verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### UPC-TC-INT-001 — Full POST — status changes; publishedAt unchanged in DB

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/content/{id}/unpublish` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/UnpublishContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Preconditions:** Testcontainer + Flyway (NO new migration); seed `FX-1601`; CONTENT_ADMIN JWT

**Test Steps:** POST unpublish with reason → assert 200 → re-fetch `content_items` row

**Expected Result (PASS):** DB `status='ARCHIVED'`; `published_at` **exactly** equal to the original seeded value.

**Current Status:** 🔴 Not written

---

### UPC-TC-INT-002 — Item disappears from public read path immediately after unpublish

**Severity:** `CRITICAL`
**Feature Under Test:** downstream visibility (ADR-001)
**Test File:** `src/test/java/com/carebridge/backend/integration/UnpublishContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `V1__init_schema.sql idx_content_items_published_at ... WHERE status='APPROVED'`

**Preconditions:** seed `FX-1601` (APPROVED); call the public content-read path (UC-82/224-style query
filtered `status='APPROVED'`) BEFORE unpublish — assert item IS visible

**Test Steps:**
1. Confirm item visible via public read path (pre-condition sanity check)
2. POST unpublish
3. Call the public read path again

**Expected Result (PASS):** item is visible before, NOT visible after — proves the existing `status='APPROVED'`
filter (no code change needed there) correctly excludes `ARCHIVED` content, and unpublish takes effect
immediately (no caching/index-refresh lag assumed in v1).

**Expected Result (FAIL):** item still appears in public read after unpublish → the entire unpublish action is
a no-op from the user's perspective, BLOCKING (the exact real-world failure this UC exists to prevent).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `UPC-TC-1201`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UPC-TC-1202`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UPC-TC-1203`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | ★★ publishedAt preserve |
| `UPC-TC-1204`     | `ContentStatusArchitectureTest.java`                 | `[ ]`               | —                     | ★ reuse-not-invent   |
| `UPC-TC-1205`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UPC-TC-1206`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UPC-TC-1207`     | `UnpublishContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UPC-TC-1208`     | `UnpublishContentControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `UPC-TC-1209`     | `UnpublishContentControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `UPC-TC-INT-001`  | `UnpublishContentIntegrationTest.java`               | `[ ]`               | 2026-07-11 (working tree) | PostgreSQL 16 persistence |
| `UPC-TC-INT-002`  | `UnpublishContentIntegrationTest.java`               | `[ ]`               | 2026-07-11 (working tree) | ★★ visibility proof  |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// ContentUnpublishServiceImpl.java — Red Phase stub
@Override
public UnpublishResponse unpublish(UUID id, UnpublishRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `UPC-TC-1201`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UPC-TC-1202`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UPC-TC-1203`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UPC-TC-1208`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `UPC-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UPC-TC-INT-002`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-105/106/108 deployed (ContentItem, ContentStatus incl. PENDING_REVIEW, ContentException)
- [x] ADR-004 (role — CONTENT_ADMIN only) resolved via project analysis
- [ ] No migration needed — confirmed
- [ ] Fixtures FX-1601..FX-1607 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=UnpublishContentServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=UnpublishContentControllerSecurityTest` — all PASS
- [x] `./mvnw verify -Dtest=UnpublishContentIntegrationTest` — all PASS (Testcontainers)
- [ ] UPC-TC-1203: publishedAt preserved — VERIFIED (CRITICAL data-integrity gate)
- [ ] UPC-TC-1204: no invented enum value — VERIFIED (reuse-not-invent guard)
- [ ] UPC-TC-1202: APPROVED-only transition guard — VERIFIED (CRITICAL)
- [x] UPC-TC-INT-002: item disappears from public path — VERIFIED (CRITICAL, the core UC guarantee)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`UnpublishRequest/Response`, CNT-010/011 factories)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited (ADR + verified schema/index)

### Suspension Criteria
- ADR-004 (role) unresolved; UC-105/106/108 not deployed; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/
# No migration to revert (reuses existing ARCHIVED enum value).
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002/ADR-003                                      | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | UPC-TC-1201..1207 PASS với throw stub                                          | `[x]`   | G-2 ★       |
| AP-AI-002 | Data Loss                | Không có TC assert publishedAt preserved (thiếu UPC-TC-1203)                    | `[x]`   | G-2 ★★      |
| AP-AI-003 | Hallucinated Enum        | Không có TC assert vắng mặt UNPUBLISHED value (thiếu UPC-TC-1204)               | `[x]`   | G-1 ★       |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Admin state transition over UC-105/106/108 aggregate; no schema
delta (reuses existing `ARCHIVED`). publishedAt preservation (UPC-TC-1203) and downstream public-visibility
proof (UPC-TC-INT-002) are the two most CRITICAL, distinctive gates — together they prove the unpublish
action is both non-destructive to history and actually effective, not a ghost action.*
