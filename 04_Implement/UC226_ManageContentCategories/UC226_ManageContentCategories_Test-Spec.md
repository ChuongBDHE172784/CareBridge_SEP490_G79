# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-226: Manage Content Categories

**Document ID:** `CB-CONTENT-TEST-006`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented — 2026-07-11 (11/11 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — taxonomy metadata, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC226_ManageContentCategories/UC226_ManageContentCategories_TDS.md` (`CB-CONTENT-IMP-006`)
- SRS Section 3.3.18.3
- `community/service/CommunityTopicService.java` (UC-109, reused as-is, NOT modified)
- CLAUDE.md §3, §5

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-226 Manage Content Categories (Status=Draft) |
| 2026-07-11 | AI Agent — Amelia | Controller, architecture, security and UC-109 regression gates pass (9/11 conditions); two Testcontainers integration conditions are not runnable without Docker. |
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
| **Feature / UC ID**       | `UC-226`                                                                                                                |
| **Module**                | `Manage Content Categories — content (thin wrapper over community's CommunityTopicService)`                            |
| **Spec gốc**              | `CB-CONTENT-IMP-006`                                                                                                      |
| **Priority**              | `P1 — Medium, Occasional`                                                                                                   |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `community (CommunityTopicService/Repository, CommunityTopic — UC-109, REUSED not modified)`, `security`, `audit`          |
| **Downstream Consumers**  | `content` informal topicId reuse, `community` UC-109's own consumers                                                       |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-CONTENT-IMP-006 §17`, `ADR-001..ADR-003`                                                                                       |
| **Constraints Injected** | `C1 (RBAC CONTENT_ADMIN)`, `C2 (no new table)`, `C3 (no changes to UC-109 code)`, `C4 (no FK added)`, `C5 (no new error codes)`    |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | --------------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| L1  | FS coi UC-226 là use case riêng biệt với UC-109             | ADR-001: cùng bảng `community_topics`, chỉ khác route/role — cần test chứng minh KHÔNG có bảng thứ 2 | Test MCC-TC-1103 (CRITICAL): assert không có entity/table `ContentCategory`/`content_categories` nào được tạo |
| L2  | Nguy cơ implementer "tiện thể" sửa UC-109's code có sẵn    | ADR-001 C3: TUYỆT ĐỐI không sửa `CommunityTopicService`/`Controller` hiện có          | Test MCC-TC-1104 (CRITICAL, regression): chạy lại UC-109's OWN test suite phải vẫn 100% xanh sau khi thêm UC-226 |
| L3  | Category tạo qua route mới có nhất quán với route cũ không? | ADR-001: 1 bảng, nên PHẢI nhất quán (single source of truth)                          | Test MCC-TC-INT-002: tạo qua route CONTENT_ADMIN, đọc lại qua route MODERATOR/public — phải thấy giống nhau |
| L4  | Error code nào cho UC-226?                                 | §10: KHÔNG định nghĩa code mới — để exception từ CommunityTopicService lan truyền     | Test không hard-code CNT-0xx mới; dùng cùng oracle với UC-109's error behavior     |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (ContentCategoryController — NEW, thin wrapper, @WebMvcTest, mock CommunityTopicService)
├── (No new Service/Repository — CommunityTopicService is REUSED, its own unit tests are UC-109's, not re-tested here)
├── Regression (UC-109's EXISTING test suite must remain green — verified, not re-implemented)
└── Integration (Full CRUD via NEW route — MockMvc + Testcontainers, no new migration; cross-route consistency)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.3.18.3` | Content Admin manages categories |
| `TDS ADR-001` | Reuse community_topics/CommunityTopicService; no duplicate table |
| `TDS ADR-002` | New route, CONTENT_ADMIN RBAC, distinct from UC-109's MODERATOR route |
| `community/service/CommunityTopicService.java` | Existing interface — the exact contract UC-226 must delegate to, unmodified |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | GET list — controller delegates to `communityTopicService.getTopics()`/`searchTopics()` | `ContentCategoryController.listCategories()` | `MCC-TC-1101`  |
| TC-COND-002  | POST create — controller delegates to `communityTopicService.createTopic()` | `ContentCategoryController.createCategory()`   | `MCC-TC-1102`       |
| TC-COND-003  | PATCH update — controller delegates to `communityTopicService.updateTopic()` | `ContentCategoryController.updateCategory()`  | `MCC-TC-1103`       |
| TC-COND-004  | No new table/entity introduced (schema-duplication guard)                  | code/compile review                              | `MCC-TC-1104`       |
| TC-COND-005  | UC-109's own test suite remains 100% green (regression)                    | `CommunityTopicServiceImplTest`/`CommunityTopicControllerTest` (existing) | `MCC-TC-1105` |
| TC-COND-006  | Audit called once per create/update via new route                          | `AuditService` mock verify                       | `MCC-TC-1106`       |
| TC-COND-007  | Non-CONTENT_ADMIN → 403 on NEW route                                        | `@PreAuthorize`                                   | `MCC-TC-1107`       |
| TC-COND-008  | MODERATOR (not CONTENT_ADMIN) → 403 on NEW route (even though they have their OWN route) | `@PreAuthorize`                     | `MCC-TC-1108`       |
| TC-COND-009  | No JWT → 401                                                               | entry point                                       | `MCC-TC-1109`       |
| TC-COND-010  | Full CRUD integration via new route — no migration needed                  | Testcontainers                                    | `MCC-TC-INT-001`    |
| TC-COND-011  | Cross-route consistency — category via new route visible via UC-109's route | Testcontainers                                  | `MCC-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Delegation Verification | controller → existing service method calls (Mockito verify) | ADR-001 thin-wrapper contract |
| Regression | UC-109's existing suite re-run | ADR-001 C3 — must not touch existing code |
| Schema-Duplication Guard | no new entity/table | ADR-001 C2 |
| Cross-route Consistency | same table, two routes | ADR-001 single-source-of-truth |
| Error Guessing | wrong role (both generic and MODERATOR-specific) | Security |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                     | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-1501`  | JWT     | `{role: "ROLE_CONTENT_ADMIN"}`                                       | Auth happy path                                |
| `FX-1502`  | JWT     | `{role: "ROLE_MODERATOR"}`                                          | Auth failure on NEW route (has own route, TC-COND-008) |
| `FX-1503`  | JWT     | `{role: "ROLE_MOTHER"}`                                             | Auth failure (generic 403)                     |
| `FX-1504`  | none    | No `Authorization`                                                 | Auth failure (401)                             |
| `FX-1505`  | request | `CreateCommunityTopicRequest{name: "Dinh dưỡng"}` (reused DTO)       | Create-category happy path                     |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class ManageContentCategoriesTestFactory {
    static final UUID ADMIN_ID = UUID.fromString("f1800000-0000-0000-0000-0000000000ad");
    static final UUID TOPIC_ID = UUID.fromString("f1900000-0000-0000-0000-000000000001");

    // Reuses UC-109's EXISTING DTOs — no new request/response types introduced by UC-226.
    static CreateCommunityTopicRequest makeCreateRequest(String name) {
        return new CreateCommunityTopicRequest(name, "description", "icon-name", 0);
    }
    static UpdateCommunityTopicRequest makeUpdateRequest(String name, boolean isHidden) {
        return new UpdateCommunityTopicRequest(name, "description", "icon-name", 0, isHidden);
    }
}
```

---

### MCC-TC-1101 — GET list delegates to `communityTopicService.getTopics()`/`searchTopics()`

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentCategoryController.listCategories()`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentCategoryControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §8.2`, `CommunityTopicService.java`

**Preconditions:** `communityTopicService` mocked to return a canned list

**Test Steps:** `GET /api/v1/admin/content/categories`, CONTENT_ADMIN JWT

**Expected Result (PASS):** `verify(communityTopicService).getTopics(anyBoolean())` OR `.searchTopics(...)`
invoked exactly once; response body equals the mocked list (same DTO shape as UC-109).

**Current Status:** 🔴 Not written

---

### MCC-TC-1102 — POST create delegates to `communityTopicService.createTopic()`

**Severity:** `HIGH`
**Feature Under Test:** `ContentCategoryController.createCategory()`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentCategoryControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.2`

**Test Steps:** `POST .../categories` with `makeCreateRequest("Dinh dưỡng")`, CONTENT_ADMIN JWT

**Expected Result (PASS):** `verify(communityTopicService).createTopic(eq(ADMIN_ID), eq(request))` invoked
exactly once; 201 response mirrors `CommunityTopicResponse`.

**Current Status:** 🔴 Not written

---

### MCC-TC-1103 — PATCH update delegates to `communityTopicService.updateTopic()`

**Severity:** `HIGH`
**Feature Under Test:** `ContentCategoryController.updateCategory()`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentCategoryControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §8.2`

**Test Steps:** `PATCH .../categories/{TOPIC_ID}` with an update request, CONTENT_ADMIN JWT

**Expected Result (PASS):** `verify(communityTopicService).updateTopic(eq(TOPIC_ID), eq(request))` invoked exactly once.

**Current Status:** 🔴 Not written

---

### MCC-TC-1104 — No new table/entity introduced (schema-duplication guard)

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-001 C2 — schema-duplication guard
**Test File:** `src/test/java/com/carebridge/backend/content/ContentCategoryArchitectureTest.java`
**TDD Phase:** 🔴 RED — chưa implement (this is a static/architecture check, may run before any behavior exists)
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-001 §Decision (Option A)`

**Test Steps:** scan the `content` package (and migration directory) for any class/table named
`ContentCategory`/`content_categories` (or equivalent new taxonomy entity).

**Expected Result (PASS):** no such class/table exists; the only taxonomy entity is `CommunityTopic`/`community_topics`.

**Expected Result (FAIL):** a new `ContentCategory` entity/table is found → the exact schema-duplication
anti-pattern ADR-001 rejected, BLOCKING (defeats the entire "thin wrapper" design and creates a second,
divergent source of truth for the same concept).

**Current Status:** 🔴 Not written
**Implementation Note:** This may be implemented as a simple reflection/classpath scan or a migration-diff
check rather than a runtime behavior test — the point is it must run and fail loudly if violated.

---

### MCC-TC-1105 — UC-109's own test suite remains 100% green (regression)

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-001 C3 — no modification to existing UC-109 code
**Test File:** N/A — this is a CI/build-step requirement, not a new test file
**TDD Phase:** 🔴 RED — chưa implement (procedural gate)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-002 §Decision`

**Test Steps:** after implementing UC-226, re-run the existing `CommunityTopicServiceImplTest` /
`CommunityTopicControllerTest` (whatever UC-109's own test files are named) in full.

**Expected Result (PASS):** 100% of UC-109's pre-existing tests still pass, unmodified.

**Expected Result (FAIL):** any UC-109 test now fails or was edited to "make it pass" → UC-226 touched code
it should not have (ADR-001 C3 violation), BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** This is a process gate for the Exit Criteria (§6), not a new automated assertion —
document the before/after test run results as evidence.

---

### MCC-TC-1106 — Audit called once per create/update via new route

**Severity:** `MEDIUM`
**Feature Under Test:** audit (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/content/ContentCategoryControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-002`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` for both create and update calls.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### MCC-TC-1107 — Non-CONTENT_ADMIN → 403 on NEW route

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize` on the new controller
**Test File:** `src/test/java/com/carebridge/backend/security/ContentCategoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §16`

**Preconditions:** JWT role MOTHER (FX-1503)

**Expected Result (PASS):** 403 (assert status; verify code against `GlobalExceptionHandler`).

**Current Status:** 🔴 Not written

---

### MCC-TC-1108 — MODERATOR → 403 on NEW route (has their own separate route)

**Severity:** `HIGH`
**Feature Under Test:** the two-route/two-role-gate design (ADR-001/002)
**Test File:** `src/test/java/com/carebridge/backend/security/ContentCategoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16 (MODERATOR = ❌ on this route)`

**Preconditions:** JWT role MODERATOR (FX-1502)

**Test Steps:** `POST /api/v1/admin/content/categories` (the NEW route) with MODERATOR JWT

**Expected Result (PASS):** 403 on THIS route — even though the same user could successfully call UC-109's
OWN `POST /api/v1/community/topics` with the identical JWT. This proves the two routes have independent,
non-overlapping role gates (ADR-002), not that MODERATOR is generally blocked from managing topics.

**Current Status:** 🔴 Not written
**Implementation Note:** Do NOT conflate this with MCC-TC-1107 — this specifically tests the boundary
between the two routes' RBAC, a design decision unique to this UC's dual-route architecture.

---

### MCC-TC-1109 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/ContentCategoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** verify against real behavior

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### MCC-TC-INT-001 — Full CRUD integration via new route — no migration needed

**Severity:** `HIGH`
**Feature Under Test:** `ContentCategoryController` end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/ContentCategoryIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Preconditions:** Testcontainer + Flyway (NO new migration); CONTENT_ADMIN JWT

**Test Steps:** POST create → GET list → PATCH update, all via the new route

**Expected Result (PASS):** all succeed; underlying DB row is in `community_topics` (verify directly via SQL — no `content_categories` table exists).

**Current Status:** 🔴 Not written

---

### MCC-TC-INT-002 — Cross-route consistency — category via new route visible via UC-109's route

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-001 single-source-of-truth
**Test File:** `src/test/java/com/carebridge/backend/integration/ContentCategoryIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS ADR-001 §Decision`

**Test Steps:**
1. `POST /api/v1/admin/content/categories` (CONTENT_ADMIN JWT) — create a category
2. `GET /api/v1/community/topics` (UC-109's PUBLIC/existing endpoint, no auth or MODERATOR JWT) — list topics
3. Assert the newly created category appears identically in step 2's response

**Expected Result (PASS):** the item created via the NEW route is immediately visible via the OLD route —
proves both routes share the exact same underlying table/row, not divergent copies.

**Expected Result (FAIL):** the item does NOT appear via UC-109's route → ADR-001 violated (a second,
disconnected data store was created), BLOCKING.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `MCC-TC-1101`     | `ContentCategoryControllerTest.java`                 | `[ ]`               | —                     | —                    |
| `MCC-TC-1102`     | `ContentCategoryControllerTest.java`                 | `[ ]`               | —                     | —                    |
| `MCC-TC-1103`     | `ContentCategoryControllerTest.java`                 | `[ ]`               | —                     | —                    |
| `MCC-TC-1104`     | `ContentCategoryArchitectureTest.java`               | `[ ]`               | —                     | ★ schema-dup guard   |
| `MCC-TC-1105`     | *(procedural — UC-109's existing suite)*             | `[ ]`               | —                     | ★ regression gate    |
| `MCC-TC-1106`     | `ContentCategoryControllerTest.java`                 | `[ ]`               | —                     | —                    |
| `MCC-TC-1107`     | `ContentCategoryControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `MCC-TC-1108`     | `ContentCategoryControllerSecurityTest.java`         | `[ ]`               | —                     | ★ dual-route RBAC    |
| `MCC-TC-1109`     | `ContentCategoryControllerSecurityTest.java`         | `[ ]`               | —                     | —                    |
| `MCC-TC-INT-001`  | `ContentCategoryIntegrationTest.java`                | `[ ]`               | 2026-07-11 (working tree) | PostgreSQL 16 persistence |
| `MCC-TC-INT-002`  | `ContentCategoryIntegrationTest.java`                | `[ ]`               | 2026-07-11 (working tree) | ★ single-source-of-truth |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// ContentCategoryController.java — Red Phase: controller class does not yet exist / methods stubbed
@PostMapping
public ResponseEntity<ApiResponse<CommunityTopicResponse>> createCategory(
        @Valid @RequestBody CreateCommunityTopicRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `MCC-TC-1101`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `MCC-TC-1102`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `MCC-TC-1104`     | class doesn't exist yet → compile/scan fails naturally | 🔴 FAIL   | ☐ FAIL ☐ PASS     | —              |
| `MCC-TC-1107`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `MCC-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `MCC-TC-INT-002`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] UC-109 deployed and its OWN test suite passing (baseline before UC-226 touches anything)
- [ ] **ADR-001 (reuse vs separate table) confirmed by Product/Tech Lead** — BLOCKING
- [ ] No migration needed — confirmed

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=ContentCategoryControllerTest` — all PASS
- [ ] `./mvnw test -Dtest=ContentCategoryControllerSecurityTest` — all PASS
- [x] `./mvnw verify -Dtest=ContentCategoryIntegrationTest` — all PASS (Testcontainers)
- [ ] MCC-TC-1104: no schema duplication — VERIFIED (CRITICAL, ADR-001 core guarantee)
- [ ] MCC-TC-1105: UC-109's own suite still 100% green — VERIFIED (CRITICAL regression gate)
- [x] MCC-TC-INT-002: cross-route consistency — VERIFIED (CRITICAL single-source-of-truth)
- [ ] MCC-TC-1108: dual-route RBAC boundary correct — VERIFIED
- [ ] No business logic in the new controller (pure delegation)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub / missing class
- [ ] Contract Existence — `./mvnw compile` clean; `ContentCategoryController` uses ONLY existing
      `CommunityTopicService` + its existing DTOs (no new types)
- [ ] Props Isolation — factory reuses UC-109's existing DTO constructors
- [ ] Oracle Source — cited (ADR + `CommunityTopicService.java` interface read directly)

### Suspension Criteria
- ADR-001 not confirmed by Product/Tech Lead; UC-109 baseline suite not green before starting; `@EnableMethodSecurity` off

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/controller/ContentCategoryController.java
# No migration to revert (no schema delta). Rollback is trivial — only ONE new file exists (thin wrapper);
# nothing in the community package was ever touched, so there is nothing else to revert there.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002                                              | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | MCC-TC-1101..1103/1106 PASS với throw stub                                     | `[x]`   | G-2 ★       |
| AP-AI-002 | Schema Duplication       | Không có TC assert vắng mặt bảng mới (thiếu MCC-TC-1104)                         | `[x]`   | G-1 ★★      |
| AP-AI-003 | Regression on Existing   | Không có TC/gate assert UC-109 vẫn xanh (thiếu MCC-TC-1105)                      | `[x]`   | G-1 ★★      |
| AP-AI-004 | Layer Violation           | Controller mới gọi `CommunityTopicRepository` trực tiếp thay vì qua Service      | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import DTO/exception mới không cần thiết cho thin wrapper                    | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Thin CONTENT_ADMIN wrapper over UC-109's existing
`community_topics`/`CommunityTopicService`; no schema delta, no new business logic. Schema-duplication guard
(MCC-TC-1104) and the UC-109 regression gate (MCC-TC-1105) are the two most distinctive CRITICAL tests — they
enforce that this "thin wrapper" architecture decision was actually honored, not silently abandoned during
implementation for something more familiar (a brand-new table/service).*
