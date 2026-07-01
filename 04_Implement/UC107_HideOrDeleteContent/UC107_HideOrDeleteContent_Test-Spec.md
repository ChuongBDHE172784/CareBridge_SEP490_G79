# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-107: Hide or Delete Content

**Document ID:** `CB-CONTENT-TEST-006`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — editorial content, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC107_HideOrDeleteContent/UC107_HideOrDeleteContent_TDS.md` (`CB-CONTENT-IMP-006`)
- SRS Section 3.2.2.9
- Sibling (Draft, structural template): UC-106 (`CB-CONTENT-TEST-004`)
- Sibling (Approved, infra oracle): UC-105 (`CB-CONTENT-IMP-003`)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                                     |
| ---------- | -------------------- | ------------------------------------------------------------------------------------------ |
| 2026-07-02 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-107 Hide or Delete Content (Status=Draft)          |

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
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-107`                                                                                                                 |
| **Module**                | `Hide or Delete Content — content (brownfield extension of UC-105/106)`                                                 |
| **Spec gốc**              | `CB-CONTENT-IMP-006`                                                                                                    |
| **Priority**              | `P0 — High, Occasional`                                                                                                 |
| **Sprint / Milestone**    | `Sprint 3`                                                                                                               |
| **Data Classification**   | `Internal`                                                                                                               |
| **Compliance Scope**      | `N/A`                                                                                                                    |
| **Upstream Dependencies** | `content (ContentItem/ContentException — UC-105/106)`, `security`, `audit`                                              |
| **Downstream Consumers**  | Public read paths (must exclude `ARCHIVED`); **subsumes UC-227** (Sprint 4) for `APPROVED→ARCHIVED` case                |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                    |
| ------------------------ | --------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                    |
| **Constraint Source**    | `CB-CONTENT-IMP-006 §17`, `ADR-001..ADR-005`                                                             |
| **Constraints Injected** | `C1 (RBAC reused)`, `C2 (ARCHIVED only, no hard-delete)`, `C3 (transition guard)`, `C4 (reason required)`, `C5 (single endpoint, subsumes UC-227)` |
| **Model**                | `claude-sonnet-5`                                                                                        |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                             |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                        | Thực tế                                                                          | Fix áp dụng trong test                                                                 |
| --- | -------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| L1  | FS nói "hides or soft-deletes" — hai khái niệm khác nhau?      | ADR-001: cả hai đều map vào cùng `ARCHIVED`, không phân biệt trong dữ liệu         | Test KHÔNG tạo case phân biệt "hide" vs "delete" — chỉ assert `status == ARCHIVED`          |
| L2  | UC-107 số hiệu bị thiếu hoàn toàn khỏi batch ban đầu           | UC được lấp bởi phiên này, claim `CNT-006`/`CNT-007`                               | Test UCT-TC-1202/1203 xác nhận đúng 2 mã lỗi mới, không trùng UC-106/108/227                |
| L3  | Có giới hạn status nguồn nào được phép archive không?          | ADR-002: DRAFT/PENDING_REVIEW/APPROVED → ARCHIVED; đã ARCHIVED → CNT-006 (409)     | Test UCT-TC-1201a/b/c (3 status nguồn) + UCT-TC-1202 (idempotency guard)                    |
| L4  | Có xoá hàng khỏi DB không (hard-delete)?                       | ADR-001: KHÔNG — chỉ soft-delete qua `status`                                      | Test UCT-TC-1206 (integration): row count không đổi sau khi archive — regression guard      |
| L5  | Endpoint này có trùng với UC-227 (`/unpublish`) không?         | ADR-003: UC-107 subsumes UC-227; chỉ MỘT endpoint (`/archive`)                     | Không viết test riêng cho `/unpublish` — ghi chú trong TDS §Phụ lục, không phải trong test  |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (AdminContentController.hideContent() — @WebMvcTest, mock service)
├── Service (AdminContentServiceImpl.hideContent() — mock ContentItemRepository + audit)
├── Repository (findById/save — existing, no new finder)
└── Integration (Full POST — MockMvc + Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.2.9` | Hide/soft-delete content |
| `TDS ADR-001` | ARCHIVED reuse; no hard-delete |
| `TDS ADR-002` | Transition guard (3 valid sources, 1 blocked) |
| `TDS ADR-005` | reason required |
| `content/entity/ContentStatus.java` | DRAFT/PENDING_REVIEW/APPROVED/ARCHIVED oracle |
| `UC-106 TDS/Test-Spec` | CNT-003/004/005 reused; structural test template |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases           |
| ------------- | ---------------------------------------------------------------------- | ------------------------------------------ | --------------------- |
| TC-COND-001  | Happy path from DRAFT → ARCHIVED                                        | `AdminContentServiceImpl.hideContent()`   | `UCT-TC-1201a`        |
| TC-COND-002  | Happy path from PENDING_REVIEW → ARCHIVED                               | service                                    | `UCT-TC-1201b`        |
| TC-COND-003  | Happy path from APPROVED → ARCHIVED                                     | service                                    | `UCT-TC-1201c`        |
| TC-COND-004  | Already ARCHIVED → 409 CNT-006 (idempotency guard)                      | service                                    | `UCT-TC-1202`         |
| TC-COND-005  | Not found → 404 CNT-003 (reused)                                        | service                                    | `UCT-TC-1203`         |
| TC-COND-006  | Blank/null reason → 400 CNT-007                                         | controller/service                         | `UCT-TC-1204`         |
| TC-COND-007  | Only `status` field changes; all other fields unchanged                | service                                    | `UCT-TC-1205`         |
| TC-COND-008  | Row is never physically deleted (COUNT unchanged)                      | Testcontainers                             | `UCT-TC-1206`         |
| TC-COND-009  | Audit called once with `CONTENT_HIDDEN`                                | `AuditService` mock verify                 | `UCT-TC-1207`         |
| TC-COND-010  | Non-CONTENT_ADMIN → 403 CNT-004                                         | `@PreAuthorize`                            | `UCT-TC-1208`         |
| TC-COND-011  | No JWT → 401                                                            | entry point                                | `UCT-TC-1209`         |
| TC-COND-012  | Full POST integration — DB row ARCHIVED, public read path excludes it   | Testcontainers                             | `UCT-TC-INT-1201`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Equivalence Partitioning | 3 valid source statuses (DRAFT/PENDING_REVIEW/APPROVED) | ADR-002 transition guard is the central oracle |
| Negative / Idempotency | already-ARCHIVED rejection | ADR-002 (blocks duplicate no-op action) |
| Boundary | reason blank/null/whitespace-only | ADR-005 `@NotBlank` |
| Regression | field-preservation (no accidental wipe), row-not-deleted | ADR-001 (soft-delete guarantee) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                     | Mục đích                                       |
| ----------- | -------- | ---------------------------------------------------------------------------------- | ------------------------------------------------ |
| `FX-1701`  | DB seed | `ContentItem{id: D1, status: DRAFT, title: "A", body: "...", versionNo: 1}`         | Happy path DRAFT (UCT-TC-1201a)                  |
| `FX-1702`  | DB seed | `ContentItem{id: D2, status: PENDING_REVIEW}`                                       | Happy path PENDING_REVIEW (UCT-TC-1201b)         |
| `FX-1703`  | DB seed | `ContentItem{id: D3, status: APPROVED}`                                             | Happy path APPROVED (UCT-TC-1201c)               |
| `FX-1704`  | DB seed | `ContentItem{id: D4, status: ARCHIVED}`                                             | Idempotency-guard rejection (UCT-TC-1202)        |
| `FX-1705`  | JWT     | `{role: "ROLE_CONTENT_ADMIN"}`                                                       | Auth happy path                                  |
| `FX-1706`  | JWT     | `{role: "ROLE_MODERATOR"}`                                                           | Auth failure (403)                               |
| `FX-1707`  | none    | No `Authorization`                                                                  | Auth failure (401)                               |
| `FX-1708`  | request | `HideContentRequest{reason: ""}` / `{reason: null}` / `{reason: "   "}`             | Blank-reason validation (UCT-TC-1204)             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class HideContentTestFactory {
    static final UUID ADMIN_ID = UUID.fromString("f1700000-0000-0000-0000-0000000000ad");
    static final UUID D1 = UUID.fromString("f1700000-0000-0000-0000-000000000001"); // DRAFT
    static final UUID D2 = UUID.fromString("f1700000-0000-0000-0000-000000000002"); // PENDING_REVIEW
    static final UUID D3 = UUID.fromString("f1700000-0000-0000-0000-000000000003"); // APPROVED
    static final UUID D4 = UUID.fromString("f1700000-0000-0000-0000-000000000004"); // already ARCHIVED

    static ContentItem makeItem(UUID id, ContentStatus status) {
        return ContentItem.builder().id(id).title("A").body("body text")
                .stage(ContentStage.PREGNANCY).type(ContentType.ARTICLE)
                .status(status).versionNo(1).authorUserId(UUID.randomUUID()).build();
    }
    static HideContentRequest makeRequest(String reason) {
        return new HideContentRequest(reason);
    }
}
```

---

### UCT-TC-1201a — Happy path: DRAFT → ARCHIVED

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.hideContent(id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS ADR-002 §Decision`

**Preconditions:** `findById(D1)` → `FX-1701` (status=DRAFT); request `reason="Thông tin lỗi thời"`

**Expected Result (PASS):** saved `status == ARCHIVED`; response `previousStatus == DRAFT`, `newStatus == ARCHIVED`, `reason` echoed, `hiddenByAdminId` set, `hiddenAt` set.

**Current Status:** 🔴 Not written

---

### UCT-TC-1201b — Happy path: PENDING_REVIEW → ARCHIVED

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-002 §Decision`

**Preconditions:** `findById(D2)` → `FX-1702` (status=PENDING_REVIEW)

**Expected Result (PASS):** saved `status == ARCHIVED`; response `previousStatus == PENDING_REVIEW`.
**Implementation Note:** Confirms UC-107 does not require waiting for UC-108's approval decision first — an item can be pulled even while pending review.

**Current Status:** 🔴 Not written

---

### UCT-TC-1201c — Happy path: APPROVED → ARCHIVED

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002 §Decision, ADR-003 (this is the case UC-227 subsumes)`

**Preconditions:** `findById(D3)` → `FX-1703` (status=APPROVED)

**Expected Result (PASS):** saved `status == ARCHIVED`; response `previousStatus == APPROVED`.

**Current Status:** 🔴 Not written

---

### UCT-TC-1202 — Already ARCHIVED → 409 CNT-006 (idempotency guard)

**Severity:** `HIGH`
**Feature Under Test:** transition guard (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 CNT-006 (new)`

**Preconditions:** `findById(D4)` → `FX-1704` (status=ARCHIVED)

**Expected Result (PASS):** throws `ContentException` `CNT-006`, 409; no `save()` call (verify never-saved).
**Expected Result (FAIL):** re-archives silently (200) or throws unrelated 500 → breaks idempotency contract.

**Current Status:** 🔴 Not written

---

### UCT-TC-1203 — Not found → 404 CNT-003 (reused)

**Severity:** `HIGH`
**Feature Under Test:** lookup
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC-106 TDS §10 CNT-003 (reused)`

**Preconditions:** `findById` → empty

**Expected Result (PASS):** throws `ContentException` `CNT-003`, 404.

**Current Status:** 🔴 Not written

---

### UCT-TC-1204 — Blank/null/whitespace reason → 400 CNT-007

**Severity:** `MEDIUM`
**Feature Under Test:** validation (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-005, §10 CNT-007 (new)`

**Test Steps:** POST with `reason=""`, `reason=null`, `reason="   "` (3 sub-cases, `FX-1708`)

**Expected Result (PASS):** 400, `error.code == "CNT-007"` for all 3 sub-cases.

**Current Status:** 🔴 Not written

---

### UCT-TC-1205 — Only `status` changes; all other fields preserved

**Severity:** `CRITICAL`
**Feature Under Test:** field-preservation guard (ADR-001, no accidental wipe)
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §8.3 DTO (HideContentRequest has only `reason`, no other field)`

**Preconditions:** `FX-1703` (D3: title="A", body="body text", stage=PREGNANCY, type=ARTICLE, versionNo=1)

**Expected Result (PASS):** after hide, saved item's `title`, `body`, `stage`, `type`, `versionNo`, `authorUserId` are all unchanged — only `status` differs.
**Expected Result (FAIL):** any of these fields nulled or reset → BLOCKING (silent data loss).

**Current Status:** 🔴 Not written

---

### UCT-TC-1206 — Row is never physically deleted (regression guard)

**Severity:** `CRITICAL`
**Feature Under Test:** soft-delete guarantee (ADR-001)
**Test File:** `src/test/java/com/carebridge/backend/integration/HideContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-001 §Decision (Phương án C loại trừ)`

**Test Steps:** seed `FX-1703`; `SELECT COUNT(*) FROM content_items` before hide; POST archive; `SELECT COUNT(*)` after

**Expected Result (PASS):** row count identical before/after; row still fetchable by id with `status='ARCHIVED'`.
**Expected Result (FAIL):** row count decreases → hard-delete regression, BLOCKING (irreversible data loss).

**Current Status:** 🔴 Not written

---

### UCT-TC-1207 — Audit called once with CONTENT_HIDDEN

**Severity:** `MEDIUM`
**Feature Under Test:** audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(AuditAction.CONTENT_HIDDEN, adminId, "CONTENT_ITEM", id, ...)`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### UCT-TC-1208 — Non-CONTENT_ADMIN → 403 CNT-004

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** class-level `@PreAuthorize` (reused UC-105/106)
**Test File:** `src/test/java/com/carebridge/backend/security/HideContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC-106 TDS §10 CNT-004 (reused)`

**Preconditions:** JWT role MODERATOR (`FX-1706`)

**Expected Result (PASS):** 403, `error.code == "CNT-004"`.

**Current Status:** 🔴 Not written

---

### UCT-TC-1209 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/HideContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UC-105/106 §10 (verify exact behavior)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### UCT-TC-INT-1201 — Full POST — DB row ARCHIVED, public read path excludes it

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/content/{id}/archive` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/HideContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-1703` (APPROVED, publicly visible); CONTENT_ADMIN JWT

**Test Steps:** confirm item appears via public read endpoint (`status='APPROVED'` filter) → POST archive → assert 200 → re-query public read endpoint → re-query `content_items` directly

**Expected Result (PASS):** direct DB row reflects `status='ARCHIVED'`, row still exists; public read endpoint no longer returns this item.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                                   | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note        |
| -------------------- | ---------------------------------------------- | ------------------ | -------------------- | -------------------------- |
| `UCT-TC-1201a`      | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | —                           |
| `UCT-TC-1201b`      | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | —                           |
| `UCT-TC-1201c`      | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | ★ subsumes UC-227          |
| `UCT-TC-1202`       | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | ★ idempotency oracle       |
| `UCT-TC-1203`       | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | —                           |
| `UCT-TC-1204`       | `HideContentControllerTest.java`              | `[ ]`               | —                     | —                           |
| `UCT-TC-1205`       | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | ★ field-preservation       |
| `UCT-TC-1206`       | `HideContentIntegrationTest.java`             | `[ ]`               | —                     | ★ hard-delete regression   |
| `UCT-TC-1207`       | `HideContentServiceImplTest.java`             | `[ ]`               | —                     | —                           |
| `UCT-TC-1208`       | `HideContentControllerSecurityTest.java`      | `[ ]`               | —                     | —                           |
| `UCT-TC-1209`       | `HideContentControllerSecurityTest.java`      | `[ ]`               | —                     | —                           |
| `UCT-TC-INT-1201`   | `HideContentIntegrationTest.java`             | `[ ]`               | —                     | —                           |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// AdminContentServiceImpl.java — Red Phase stub (createContent/updateContent unchanged from UC-105/106)
@Override
public HideContentResponse hideContent(UUID id, HideContentRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID              | Stub Result                                                    | Expected          | Actual        | Root Cause |
| -------------------- | ------------------------------------------------------------------| -------------------- | ---------------- | ------------- |
| `UCT-TC-1201a`      | `throw UnsupportedOperationException`                              | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-1202`       | `throw UnsupportedOperationException`                              | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-1205`       | `throw UnsupportedOperationException`                              | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-1208`       | `@PreAuthorize inherited but method doesn't exist → 404/405`       | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-INT-1201`   | `throw UnsupportedOperationException`                              | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-105/106 deployed (`ContentItem`, `ContentException`, `AdminContentController`/`Service`)
- [ ] No migration needed for `content_items` — confirmed (ADR-001/002)
- [ ] Fixtures FX-1701..FX-1708 prepared
- [ ] `AuditAction` CHECK constraint verified (add migration for `CONTENT_HIDDEN` if constrained)

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=HideContentServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=HideContentControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=HideContentControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=HideContentIntegrationTest` — all PASS (Testcontainers)
- [ ] UCT-TC-1202: idempotency guard (CNT-006) — VERIFIED (CRITICAL)
- [ ] UCT-TC-1205: field-preservation — VERIFIED (CRITICAL)
- [ ] UCT-TC-1206: row never hard-deleted — VERIFIED (CRITICAL)
- [ ] UCT-TC-1208: non-CONTENT_ADMIN rejected — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`HideContentRequest/Response`, CNT-006/007 factories)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited

### Suspension Criteria
- UC-105/106 not deployed; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/
# No migration to revert for content_items. If AuditAction CHECK constraint was migrated for
# CONTENT_HIDDEN, do NOT revert it once any audit row references it — additive-only.
# Re-run UC-105/106 tests to confirm no regression.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | -------------------------------------------------------------------------------| ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/002/005                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | UCT-TC-1201a..1207 PASS với throw stub                                          | `[x]`   | G-2 ★       |
| AP-AI-002 | Hard-Delete Regression   | Không có TC assert row count unchanged (thiếu UCT-TC-1206)                     | `[x]`   | G-2 ★       |
| AP-AI-003 | Field-Wipe Breach        | Không có TC assert field khác không đổi (thiếu UCT-TC-1205)                    | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation          | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong TDS §8 (vd ContentStatus.HIDDEN)                | `[x]`   | G-3         |
| AP-AI-006 | Duplicate Endpoint       | TC viết cho `/unpublish` riêng biệt (trùng UC-227, vi phạm ADR-003)             | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Brownfield extension of UC-105/106; no schema delta for
`content_items`. UCT-TC-1202 (idempotency), UCT-TC-1205 (field-preservation), and UCT-TC-1206 (no hard-delete)
are the CRITICAL gates — a failure here means either a duplicate no-op action, silent data loss, or an
irreversible hard-delete regression.*
