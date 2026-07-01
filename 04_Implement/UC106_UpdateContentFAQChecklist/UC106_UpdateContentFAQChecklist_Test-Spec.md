# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-106: Update Content/FAQ/Checklist

**Document ID:** `CB-CONTENT-TEST-004`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — editorial content, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC106_UpdateContentFAQChecklist/UC106_UpdateContentFAQChecklist_TDS.md` (`CB-CONTENT-IMP-004`)
- SRS Section 3.2.2.8
- Sibling (Approved, infra oracle): UC-105 (`CB-CONTENT-IMP-003`)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-106 Update Content/FAQ/Checklist (Status=Draft) |

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
| **Feature / UC ID**       | `UC-106`                                                                                                                |
| **Module**                | `Update Content/FAQ/Checklist — content (brownfield extension of UC-105)`                                              |
| **Spec gốc**              | `CB-CONTENT-IMP-004`                                                                                                      |
| **Priority**              | `P0 — High, Regular`                                                                                                       |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `content (ContentItem/ContentException — UC-105)`, `security`, `audit`                                                     |
| **Downstream Consumers**  | `UC-108 Approve Content Version` (uses incremented versionNo)                                                              |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-CONTENT-IMP-004 §17`, `ADR-001..ADR-004`                                                                                       |
| **Constraints Injected** | `C1 (RBAC reused)`, `C2 (immutable type/authorUserId)`, `C3 (versionNo++)`, `C4 (tags=topicId)`, `C5 (conditional duplicate check)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                        | Thực tế                                                                    | Fix áp dụng trong test                                                    |
| --- | -------------------------------------------------- | ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| L1  | FS nói "update tags" nhưng không có bảng tags       | ADR-001: "tags" = topicId đơn giá trị                                          | Test chỉ assert topicId thay đổi; KHÔNG test multi-tag (không tồn tại)             |
| L2  | Versioning cơ chế nào?                             | ADR-002: versionNo++ mỗi update; không có version-history table                | Test UCT-TC-903 (CRITICAL): assert versionNo tăng đúng +1 mỗi lần                  |
| L3  | type/authorUserId có sửa được không?                | ADR-002 (TDS): bất biến — không trong request DTO                              | Test UCT-TC-904: type/authorUserId KHÔNG đổi dù request cố gắng                    |
| L4  | Duplicate check khi update?                        | ADR-004: chỉ chạy nếu title/stage/type đổi, loại trừ chính item                | Test UCT-TC-905 (collision khi đổi) và UCT-TC-906 (không check khi giữ nguyên)     |
| L5  | CNT-003 chưa từng dùng thật                        | UC-106 là first real implementation (dossier §6.2 permission)                  | Test UCT-TC-902 dùng CNT-003 cho not-found                                         |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (AdminContentController.updateContent() — @WebMvcTest, mock service)
├── Service (AdminContentServiceImpl.updateContent() — mock ContentItemRepository + audit)
├── Repository (findById/save; duplicate-check query — existing/additive)
└── Integration (Full PUT — MockMvc + Testcontainers, no new migration)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.2.8` | Update content fields |
| `TDS ADR-001` | tags=topicId; no new table |
| `TDS ADR-002` | versionNo increment mechanism |
| `TDS ADR-004` | conditional duplicate check |
| `content/entity/ContentStatus.java` | DRAFT/APPROVED/ARCHIVED oracle |
| `UC-105 TDS §10` | CNT-001/002/004/005 reused; CNT-003 reserved, now implemented |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — fields updated, response reflects changes                     | `AdminContentServiceImpl.updateContent()`       | `UCT-TC-901`        |
| TC-COND-002  | Not found → CNT-003 (404)                                                  | service                                          | `UCT-TC-902`        |
| TC-COND-003  | versionNo increments by exactly 1 per successful update                    | service                                          | `UCT-TC-903`        |
| TC-COND-004  | type/authorUserId NEVER changed by this endpoint                          | service                                          | `UCT-TC-904`        |
| TC-COND-005  | Duplicate collision when title/stage/type changed → CNT-002               | service                                          | `UCT-TC-905`        |
| TC-COND-006  | No duplicate check triggered when title/stage/type unchanged               | service                                          | `UCT-TC-906`        |
| TC-COND-007  | versionNo null (legacy row) → initialized to 1 then incremented to 2       | service                                          | `UCT-TC-907`        |
| TC-COND-008  | Validation (blank title, oversized body) → CNT-001                        | controller/service                               | `UCT-TC-908`        |
| TC-COND-009  | Audit called once                                                          | `AuditService` mock verify                       | `UCT-TC-909`        |
| TC-COND-010  | Non-CONTENT_ADMIN → 403 CNT-004                                             | `@PreAuthorize`                                   | `UCT-TC-910`        |
| TC-COND-011  | No JWT → 401                                                               | entry point                                       | `UCT-TC-911`        |
| TC-COND-012  | Full PUT integration — DB row updated, versionNo incremented               | Testcontainers                                    | `UCT-TC-INT-001`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Negative / Immutability | type, authorUserId unchanged | ADR-002 (this TDS's ADR-002... wait — UC-106's own ADR referencing immutability is under editable-fields BR) |
| State-based | versionNo monotonic increment | Central versioning oracle |
| Conditional Logic | duplicate check on/off by field-change | ADR-004 |
| Boundary | body length, title length | CNT-001 reused validators |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                    | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------------------------ | ---------------------------------------------- |
| `FX-1301`  | DB seed | `ContentItem{id: C1, title: "A", stage: PREGNANCY, type: ARTICLE, versionNo: 2}`      | Happy path + versionNo increment               |
| `FX-1302`  | DB seed | `ContentItem{id: C2, title: "B", stage: PREGNANCY, type: ARTICLE}` (collision target) | Duplicate-check test (UCT-TC-905)              |
| `FX-1303`  | DB seed | `ContentItem{id: C3, versionNo: null}`                                                | versionNo null-init test (UCT-TC-907)          |
| `FX-1304`  | JWT     | `{role: "ROLE_CONTENT_ADMIN"}`                                                        | Auth happy path                                |
| `FX-1305`  | JWT     | `{role: "ROLE_MODERATOR"}`                                                            | Auth failure (403)                             |
| `FX-1306`  | none    | No `Authorization`                                                                    | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class UpdateContentTestFactory {
    static final UUID ADMIN_ID = UUID.fromString("f1400000-0000-0000-0000-0000000000ad");
    static final UUID C1 = UUID.fromString("f1500000-0000-0000-0000-000000000001");
    static final UUID C2 = UUID.fromString("f1500000-0000-0000-0000-000000000002");

    static ContentItem makeItem(UUID id, String title, ContentStage stage, ContentType type, Integer versionNo) {
        return ContentItem.builder().id(id).title(title).stage(stage).type(type)
                .status(ContentStatus.APPROVED).versionNo(versionNo).authorUserId(UUID.randomUUID()).build();
    }
    static UpdateContentRequest makeRequest(String title, ContentStage stage, ContentStatus status) {
        return new UpdateContentRequest(title, "body text", stage, null, status, "source");
    }
}
```

---

### UCT-TC-901 — Happy path: fields updated, response reflects changes

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.updateContent(id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §8.3 DTO`

**Preconditions:** `findById(C1)` → `FX-1301` (versionNo=2)

**Expected Result (PASS):** saved title/body/stage/status/sourceLabel match request; `response.versionNo() == 3`.

**Current Status:** 🔴 Not written

---

### UCT-TC-902 — Not found → 404 CNT-003

**Severity:** `HIGH`
**Feature Under Test:** lookup
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 CNT-003 (first real use)`

**Preconditions:** `findById` → empty

**Expected Result (PASS):** throws `ContentException` `CNT-003`, 404.

**Current Status:** 🔴 Not written

---

### UCT-TC-903 — versionNo increments by exactly 1 per successful update

**Severity:** `CRITICAL`
**Feature Under Test:** version increment (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002 §Decision`

**Test Steps:** update `FX-1301` (versionNo=2) twice in sequence (two separate calls)

**Expected Result (PASS):** first call → saved `versionNo == 3`; second call (re-fetch reflects 3) → saved `versionNo == 4`. Strictly +1 each time, never skips or resets.

**Expected Result (FAIL):** versionNo unchanged, decremented, or jumps by more than 1 → breaks UC-108's oracle.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This is the load-bearing invariant for UC-108 (Approve Content Version) — if
versionNo drifts, the downstream approval flow loses its ground truth.

---

### UCT-TC-904 — type/authorUserId NEVER changed by this endpoint

**Severity:** `CRITICAL`
**Feature Under Test:** immutability guard
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §8.3 DTO (no type/authorUserId field)`

**Test Steps:** update `FX-1301` (type=ARTICLE, some authorUserId); DTO has no type/authorUserId fields to inject

**Expected Result (PASS):** saved `type == ARTICLE` (unchanged), `authorUserId` unchanged.
**Expected Result (FAIL):** either field mutated → immutability breach (a content item silently changing
its own authorship or type), BLOCKING.

**Current Status:** 🔴 Not written

---

### UCT-TC-905 — Duplicate collision when title/stage/type changed → 409 CNT-002

**Severity:** `HIGH`
**Feature Under Test:** conditional duplicate check (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-004 §Decision`

**Preconditions:** `FX-1301` (C1, title="A") being updated; `FX-1302` (C2, title="B", same stage/type) exists

**Test Steps:** update C1's title to "B" (colliding with C2's title+stage+type)

**Expected Result (PASS):** throws `CNT-002`, 409; C1 unchanged (no save, no versionNo increment).

**Current Status:** 🔴 Not written

---

### UCT-TC-906 — No duplicate check triggered when title/stage/type unchanged

**Severity:** `MEDIUM`
**Feature Under Test:** conditional duplicate check — negative case (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-004 §Decision`

**Test Steps:** update `FX-1301` (C1) changing only `body`/`sourceLabel` (title/stage/type unchanged)

**Expected Result (PASS):** no `CNT-002` thrown even if C1's own title+stage+type would "collide with itself"
(the exclusion works); update succeeds.

**Current Status:** 🔴 Not written
**Implementation Note:** Regression guard against a false-positive self-collision bug.

---

### UCT-TC-907 — versionNo null (legacy row) → initialized to 1 then incremented to 2

**Severity:** `MEDIUM`
**Feature Under Test:** defensive null-init (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-002 §Decision (defensive)`

**Preconditions:** `findById(C3)` → `FX-1303` (versionNo=null)

**Expected Result (PASS):** saved `versionNo == 2` (treated as if starting at 1, then incremented).

**Current Status:** 🔴 Not written

---

### UCT-TC-908 — Validation (blank title, oversized body) → 400 CNT-001

**Severity:** `MEDIUM`
**Feature Under Test:** validation (reused UC-105)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC-105 TDS §10 CNT-001 (reused)`

**Test Steps:** blank `title`; `body` > 50000 chars

**Expected Result (PASS):** 400, `error.code == "CNT-001"`.

**Current Status:** 🔴 Not written

---

### UCT-TC-909 — Audit called once

**Severity:** `MEDIUM`
**Feature Under Test:** audit (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/content/UpdateContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS ADR-003`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### UCT-TC-910 — Non-CONTENT_ADMIN → 403 CNT-004

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** class-level `@PreAuthorize` (reused UC-105)
**Test File:** `src/test/java/com/carebridge/backend/security/UpdateContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC-105 TDS §10 CNT-004 (reused)`

**Preconditions:** JWT role MODERATOR (FX-1305)

**Expected Result (PASS):** 403, `error.code == "CNT-004"` (per UC-105's real precedent — verify this
matches actual code, not the ACCESS_DENIED drift seen in the moderation cluster; UC-105 is Approved so its
CNT-004 finding should already be accurate).

**Current Status:** 🔴 Not written

---

### UCT-TC-911 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/UpdateContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UC-105 §10 (verify exact behavior)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### UCT-TC-INT-001 — Full PUT — DB row updated, versionNo incremented

**Severity:** `HIGH`
**Feature Under Test:** `PUT /api/v1/admin/content/{id}` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/UpdateContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`

**Preconditions:** Testcontainer + Flyway (no new migration); seed `FX-1301` (versionNo=2); CONTENT_ADMIN JWT

**Test Steps:** PUT updated fields → assert 200 → re-fetch `content_items` row

**Expected Result (PASS):** DB row reflects new title/body/stage/status; `version_no == 3`; `content_type`/`author_user_id` unchanged.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                       | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ---------------------------------------------------| ------------------ | -------------------- | ------------------- |
| `UCT-TC-901`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-902`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-903`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | ★ versionNo oracle   |
| `UCT-TC-904`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | ★ immutability       |
| `UCT-TC-905`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-906`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-907`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-908`      | `UpdateContentControllerTest.java`                | `[ ]`               | —                     | —                    |
| `UCT-TC-909`      | `UpdateContentServiceImplTest.java`               | `[ ]`               | —                     | —                    |
| `UCT-TC-910`      | `UpdateContentControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `UCT-TC-911`      | `UpdateContentControllerSecurityTest.java`        | `[ ]`               | —                     | —                    |
| `UCT-TC-INT-001`  | `UpdateContentIntegrationTest.java`               | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// AdminContentServiceImpl.java — Red Phase stub (createContent() unchanged/already GREEN from UC-105)
@Override
public UpdateContentResponse updateContent(UUID id, UpdateContentRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `UCT-TC-901`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-903`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-904`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `UCT-TC-910`      | `@PreAuthorize inherited but method doesn't exist → 404/405` | 🔴 FAIL (no 403) | ☐ FAIL ☐ PASS | — |
| `UCT-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-105 deployed (ContentItem, ContentException, AdminContentController/Service)
- [ ] No migration needed — confirmed
- [ ] Fixtures FX-1301..FX-1306 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=UpdateContentServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=UpdateContentControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=UpdateContentControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=UpdateContentIntegrationTest` — all PASS (Testcontainers)
- [ ] UCT-TC-903: versionNo increments correctly — VERIFIED (CRITICAL, feeds UC-108)
- [ ] UCT-TC-904: type/authorUserId immutable — VERIFIED (CRITICAL)
- [ ] UCT-TC-910: non-CONTENT_ADMIN rejected — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub
- [ ] Contract Existence — `./mvnw compile` clean (`UpdateContentRequest/Response`, CNT-003 factory)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited

### Suspension Criteria
- UC-105 not deployed; `@EnableMethodSecurity` off; CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/
# No migration to revert. Re-run UC-105 tests to confirm no regression.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002/ADR-004                                      | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | UCT-TC-901..909 PASS với throw stub                                            | `[x]`   | G-2 ★       |
| AP-AI-002 | Missing Versioning       | Không có TC assert versionNo tăng (thiếu UCT-TC-903)                           | `[x]`   | G-2 ★       |
| AP-AI-003 | Immutability Breach      | Không có TC assert type/authorUserId unchanged (thiếu UCT-TC-904)               | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8 (vd tags table)                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Brownfield extension of UC-105; no schema delta. versionNo
increment (UCT-TC-903) and type/authorUserId immutability (UCT-TC-904) are the CRITICAL gates — versionNo is
the load-bearing oracle for UC-108's approval flow.*
