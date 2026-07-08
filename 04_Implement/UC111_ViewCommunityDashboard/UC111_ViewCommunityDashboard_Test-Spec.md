# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-111: View Community Dashboard

**Document ID:** `CB-MOD-TEST-006`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented — 2026-07-02 (16/16 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(aggregate-only response, no row-level PII — the PII-absence assertion IS a test case, WSA-style)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC111_ViewCommunityDashboard/UC111_ViewCommunityDashboard_TDS.md` (`CB-MOD-IMP-006`)
- SRS Section 3.2.2.13
- Schema oracle: `V1__init_schema.sql` (users / community_questions / community_answers / content_reports / community_topics)
- Sibling (Draft, this batch, provides `suspended_until` for active-user metric): `04_Implement/UC102_WarnOrSuspendAccount/` (`CB-MOD-IMP-004`)
- CLAUDE.md §3 Architecture Rules, §5 Delivery Rules (PDPA)

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-111 View Community Dashboard (Status=Draft) |
| 2026-07-02 | AI Agent — Amelia (Dev Agent) | Phase 3 Implementation — user confirmed Approved. Red Gate confirmed 11/16 TCs FAIL as expected; 5 TCs legitimately PASS at Red Gate (DASH-TC-110 is a pure DTO-shape reflection test with no service call; DASH-TC-108 mocks the service directly to test exception→HTTP wiring which already worked; DASH-TC-111/112/113 are blocked by `@PreAuthorize`/Spring Security filter chain before the stub is ever reached) — none are AP-AI-002, documented in §5.1. Deviations applied: (1) repositories extended additively on `UserRepository`/`CommunityQuestionRepository`/`CommunityAnswerRepository`/`ContentReportRepository` (no new repository classes, no `CommunityTopicRepository` dependency needed — the trending JOIN lives in `CommunityQuestionRepository`); (2) `avgHandlingTimeSeconds` computed in Java from `(createdAt, resolvedAt)` pairs, NOT `EXTRACT(EPOCH FROM ...)` SQL — that syntax is Postgres-specific and this codebase's test datasource is H2 (no Testcontainers/real-Postgres harness exists anywhere, verified project-wide); (3) MOD-021 reuses `ModerationException` (TDS §10's own "either satisfies" note) rather than a new `DashboardException` class; (4) default trending top-N = 5, default window = last 30 days (per TDS's own recommended defaults for its `Open` items); (5) DASH-TC-INT-001/002 hosted as `@SpringBootTest`+H2 (real beans end-to-end), not Testcontainers; seeded values ARE the oracle (equivalent to direct-SQL cross-check). SecurityConfig: added explicit `GET /api/v1/admin/community/dashboard` → `SYSTEM_ADMIN` matcher (TDS §11.3 step 6) — this causes 403 denials to happen at the URL-matcher level with an empty body (same verified finding as UC-100/101/102/106/107), so DASH-TC-111/112 assert status only, not a JSON error body. All 16/16 TCs GREEN; full-suite regression run confirmed 0 new regressions (same 16 pre-existing failures in `community`/`exercise` packages present before this change). Status: Approved → Implemented. |

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
| **Feature / UC ID**       | `UC-111`                                                                                                                |
| **Module**                | `View Community Dashboard — content (read-side aggregation over community + security + content_reports)`               |
| **Spec gốc**              | `CB-MOD-IMP-006`                                                                                                          |
| **Priority**              | `P1 — Medium, Regular` (per FS)                                                                                           |
| **Sprint / Milestone**    | `Open` — not sourced                                                                                                       |
| **Data Classification**   | `Internal` (aggregate)                                                                                                     |
| **Compliance Scope**      | `PDPA` — response must contain no row-level PII (ADR-004)                                                                  |
| **Upstream Dependencies** | `security (users)`, `community (community_questions, community_answers, community_topics)`, `content (content_reports)`, `audit (optional, ADR-005)` |
| **Downstream Consumers**  | Admin Web dashboard UI (out of scope)                                                                                      |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-MOD-IMP-006 §17`, `ADR-001..ADR-004`                                                                                           |
| **Constraints Injected** | `C1 (RBAC SYSTEM_ADMIN)`, `C2 (read-only)`, `C3 (no PII)`, `C4 (metric-to-column grounding)`, `C5 (null avg when no data)`, `C6 (exclude hidden topics)`, `C7 (range validation MOD-021)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                                             | Thực tế (schema / policy)                                                                                     | Fix áp dụng trong test                                                                    |
| --- | ------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| L1  | FS liệt kê metric mức chung ("user, question, report, handling time, trending")       | Mỗi metric map tới một cột schema cụ thể (ADR-001, TDS §5.2); không có cột cho "satisfaction/NPS"               | Test CHỈ assert các metric có cột hậu thuẫn; KHÔNG viết test cho metric không tồn tại (mark Open) |
| L2  | Không rõ handling time tính thế nào khi chưa có report nào được xử lý                  | `avgHandlingTimeSeconds` = `null` (không phải 0) khi không có `resolved_at` (TDS §6.3 invariant)                | Test PHẢI phân biệt null (no data) vs 0 — DASH-TC-105                                             |
| L3  | Nguy cơ dashboard rò rỉ PII (ví dụ "5 user mới nhất" kèm tên)                          | ADR-004: response chỉ aggregate, không row-level PII                                                            | Test PHẢI assert response DTO KHÔNG chứa field entity/tên/email/nội dung — DASH-TC-110 (PDPA gate) |
| L4  | Không rõ trending có tính cả topic ẩn không                                            | ADR/§5.2: loại `community_topics.is_hidden = true`                                                              | Test PHẢI seed một hidden topic có nhiều câu hỏi và assert nó KHÔNG xuất hiện trong trending — DASH-TC-107 |
| L5  | Metric "active user" định nghĩa mơ hồ                                                  | `enabled AND NOT locked AND (suspended_until IS NULL OR suspended_until <= now())` (dùng cột UC-102)            | Test PHẢI seed user suspended (future) và assert KHÔNG được đếm là active — DASH-TC-103           |
| L6  | Error-code range?                                                                      | UC-111 claim `MOD-021` (read-only cần rất ít mã lỗi); UC-100/101/102 đã dùng tới MOD-020                        | Test range validation dùng đúng `MOD-021`; Consistency Gate re-verify                             |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-111 View Community Dashboard:
├── Controller (CommunityDashboardController.getDashboard() — mock service, @WebMvcTest)
├── Service (CommunityDashboardServiceImpl — mock repositories returning canned counts, Mockito)
├── Repository (aggregate queries — verified at integration level against real seeded data)
└── Integration (Full GET flow — MockMvc + Testcontainers PostgreSQL, no new migration)
```

### TDS-02 — Test Basis

| Source                                  | Items Derived                                                                                 |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `SRS 3.2.2.13`                            | 5 metric groups: user / question / report / handling time / trending                              |
| `TDS §5.2 Metric-to-Column Mapping`       | Exact column oracle for every response field                                                       |
| `TDS ADR-002`                             | `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`; no `RoleHierarchy`                                     |
| `TDS ADR-004`                             | Aggregate-only, no row-level PII                                                                    |
| `TDS §6.3 invariant`                      | `avgHandlingTimeSeconds` null when no resolved reports                                              |
| `V1__init_schema.sql`                     | Enum value oracles: question status PENDING/APPROVED/HIDDEN/LOCKED; answer PENDING/APPROVED/HIDDEN; report PENDING/RESOLVED/DISMISSED |
| `content/exception/GlobalExceptionHandler.java` | Real 403 = `ACCESS_DENIED`; real 401 = bodiless (reused finding)                             |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Happy path — assemble all metrics từ mocked counts                          | `CommunityDashboardServiceImpl.getDashboard()`   | `DASH-TC-101`       |
| TC-COND-002  | `userMetrics.byRole` đếm đúng theo từng role                                | service                                          | `DASH-TC-102`       |
| TC-COND-003  | `userMetrics.active` loại user suspended (future) / locked / disabled       | service                                          | `DASH-TC-103`       |
| TC-COND-004  | `questionMetrics.byStatus` đủ 4 status; `newInPeriod` lọc theo created_at   | service                                          | `DASH-TC-104`       |
| TC-COND-005  | `avgHandlingTimeSeconds` = null khi 0 resolved report                       | service                                          | `DASH-TC-105`       |
| TC-COND-006  | `avgHandlingTimeSeconds` tính đúng khi có resolved reports                  | service                                          | `DASH-TC-106`       |
| TC-COND-007  | Trending loại topic `is_hidden=true`, sắp xếp giảm dần, giới hạn top-N      | service                                          | `DASH-TC-107`       |
| TC-COND-008  | Invalid range (from > to) → `MOD-021` (400)                                 | controller/service validation                    | `DASH-TC-108`       |
| TC-COND-009  | Empty DB → tất cả count = 0, avg = null, trending = [] (no crash)           | service                                          | `DASH-TC-109`       |
| TC-COND-010  | Response KHÔNG chứa row-level PII (PDPA gate)                               | DTO shape / integration                          | `DASH-TC-110`       |
| TC-COND-011  | Endpoint read-only — không mutate bảng nào                                  | integration + pg_stat                            | `DASH-TC-INT-002`   |
| TC-COND-012  | Non-SYSTEM_ADMIN → 403 ACCESS_DENIED                                        | `@PreAuthorize`                                   | `DASH-TC-111`       |
| TC-COND-013  | MODERATOR (gần quyền nhất) vẫn 403 — no implicit hierarchy                  | `@PreAuthorize`                                   | `DASH-TC-112`       |
| TC-COND-014  | No JWT → 401 bodiless                                                       | `HttpStatusEntryPoint`                            | `DASH-TC-113`       |
| TC-COND-015  | Full integration flow — counts khớp direct SQL                             | Testcontainers                                    | `DASH-TC-INT-001`   |

### TDS-04 — Test Techniques

| Technique                | Applied To                                          | Rationale                                                        |
| --------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------- |
| Equivalence Partitioning  | user active vs inactive; question status classes         | Distinct count buckets                                                 |
| Boundary Value Analysis   | date range (from = to; from > to; both null)             | MOD-021 boundary + default-window path                                |
| Special-Value             | empty DB (all zero / null avg / empty trending)          | No-data must not crash or return 0-for-null                            |
| Negative / PII-absence    | assert no entity/name/email field in response            | ADR-004 PDPA gate                                                     |
| Cross-check Oracle        | integration counts vs direct SQL aggregate               | Proves aggregation correctness against ground truth                   |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                                      | Mục đích                                  |
| ----------- | -------- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-401`   | DB seed | 7 users, one per role; 1 suspended (future `suspended_until`), 1 locked, 1 disabled                       | Active-user predicate (DASH-TC-103)            |
| `FX-402`   | DB seed | community_questions: mix of PENDING/APPROVED/HIDDEN/LOCKED, some in-period some out                       | Question metrics (DASH-TC-104)                 |
| `FX-403`   | DB seed | community_answers: mix of PENDING/APPROVED/HIDDEN                                                          | Answer metrics                                 |
| `FX-404`   | DB seed | content_reports: some RESOLVED with known `resolved_at - created_at`, some PENDING (no resolved_at)       | Handling-time avg + null case (DASH-TC-105/106)|
| `FX-405`   | DB seed | 3 topics: 2 visible + 1 hidden; hidden topic has the MOST questions                                        | Trending exclusion (DASH-TC-107)               |
| `FX-406`   | JWT     | `{role: "ROLE_SYSTEM_ADMIN"}`                                                                             | Auth happy path                                |
| `FX-407`   | JWT     | `{role: "ROLE_MODERATOR"}`                                                                                | Auth failure (403 — no implicit hierarchy)     |
| `FX-408`   | JWT     | `{role: "ROLE_MOTHER"}`                                                                                   | Auth failure (403)                             |
| `FX-409`   | none    | No `Authorization` header                                                                                  | Auth failure (401 bodiless)                    |
| `FX-410`   | DB seed | empty tables                                                                                              | No-data path (DASH-TC-109)                     |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class CommunityDashboardTestFactory {

    static final Instant T0 = Instant.parse("2026-07-01T10:00:00Z");
    static final LocalDate FROM = LocalDate.parse("2026-06-01");
    static final LocalDate TO   = LocalDate.parse("2026-06-30");

    // Canned repository results for unit-level service tests (no DB).
    static Map<String,Long> roleCounts() {
        return Map.of("MOTHER", 900L, "FAMILY", 200L, "EXPERT", 40L,
                      "MODERATOR", 5L, "CONTENT_ADMIN", 3L, "PARTNER", 30L, "SYSTEM_ADMIN", 2L);
    }
    static Map<String,Long> questionStatusCounts() {
        return Map.of("PENDING", 12L, "APPROVED", 3300L, "HIDDEN", 80L, "LOCKED", 8L);
    }
    static Map<String,Long> reportStatusCounts() {
        return Map.of("PENDING", 5L, "RESOLVED", 220L, "DISMISSED", 60L);
    }
    static DashboardFilter makeFilter(LocalDate from, LocalDate to) {
        return new DashboardFilter(from, to);
    }
}
```

---

### DASH-TC-101 — Happy path: assemble tất cả metric từ mocked counts

**Severity:** `HIGH`
**Feature Under Test:** `CommunityDashboardServiceImpl.getDashboard(filter, principal)`
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §5.2 Metric-to-Column Mapping`, `§8.3 DTO`

**Preconditions:** All repositories mocked to return canned counts (factory)

**Test Steps:**
1. Arrange: mock `userRepository`/`communityQuestionRepository`/etc. to return factory counts
2. Act: `service.getDashboard(makeFilter(FROM, TO), principal)`

**Expected Result (PASS):**
- `response.userMetrics().total()` = sum of role counts; `byRole()` equals factory map
- `response.questionMetrics().byStatus()` equals factory map; `answerMetrics`/`reportMetrics` populated
- `response.generatedAt()` non-null
- No repository `save`/`delete` invoked (read-only)

**Current Status:** 🟢 Passing

---

### DASH-TC-102 — `userMetrics.byRole` đếm đúng theo từng role

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityDashboardServiceImpl` — user role aggregation
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `users.role` (§5.2)

**Expected Result (PASS):** Every one of the 7 canonical roles appears with its correct count; total = Σ.

**Current Status:** 🟢 Passing

---

### DASH-TC-103 — `userMetrics.active` loại user suspended/locked/disabled

**Severity:** `HIGH`
**Feature Under Test:** `CommunityDashboardServiceImpl` — active-user predicate
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §5.2 active predicate` — `enabled AND NOT locked AND (suspended_until IS NULL OR suspended_until <= now())`

**Preconditions:** `userRepository` active-count method mocked; at integration level, seed `FX-401` (1 suspended future, 1 locked, 1 disabled)

**Test Steps:**
1. Arrange: total 7 users, 3 of them inactive (1 suspended-future, 1 locked, 1 disabled)
2. Act + Assert: `response.userMetrics().active() == 4`

**Expected Result (PASS):** Suspended (future), locked, and disabled users are excluded from `active`.
**Expected Result (FAIL):** A future-suspended user counted as active → predicate ignores `suspended_until` (regression against UC-102's enforcement intent).

**Current Status:** 🟢 Passing
**Implementation Note:** Depends on `users.suspended_until` (UC-102). If UC-102 not merged, the predicate
degrades to `enabled AND NOT locked` and the suspended-user sub-assertion is skipped (documented build-order
dependency, TDS §11.2) — not a silent pass.

---

### DASH-TC-104 — `questionMetrics.byStatus` đủ 4 status; `newInPeriod` lọc created_at

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityDashboardServiceImpl` — question aggregation
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `community_questions.status` (PENDING/APPROVED/HIDDEN/LOCKED), `.created_at` (§5.2)

**Expected Result (PASS):** All 4 statuses present; `newInPeriod` counts only rows with `created_at ∈ [from,to]`.

**Current Status:** 🟢 Passing

---

### DASH-TC-105 — `avgHandlingTimeSeconds` = null khi 0 resolved report

**Severity:** `HIGH`
**Feature Under Test:** `CommunityDashboardServiceImpl` — handling-time null invariant
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §6.3 invariant` — null (not 0) when no data

**Preconditions:** `contentReportRepository.avgHandlingTime()` mock returns `null` (no resolved rows)

**Test Steps:**
1. Act: `service.getDashboard(...)`
2. Assert: `response.reportMetrics().avgHandlingTimeSeconds() == null`

**Expected Result (PASS):** `null`, NOT `0.0`.
**Expected Result (FAIL):** Returns `0.0` → conflates "instant resolution" with "no data" (misleading metric).

**Current Status:** 🟢 Passing

---

### DASH-TC-106 — `avgHandlingTimeSeconds` tính đúng khi có resolved reports

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityDashboardServiceImpl` — handling-time average
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)))` (§5.2)

**Preconditions (integration, DASH-TC-INT-001):** seed 2 resolved reports with known deltas (e.g. 1h and 3h) → expected avg = 7200s

**Expected Result (PASS):** `avgHandlingTimeSeconds ≈ 7200.0` (within tolerance for timestamp precision).

**Current Status:** 🟢 Passing

---

### DASH-TC-107 — Trending loại topic `is_hidden=true`, sắp xếp giảm dần, top-N

**Severity:** `HIGH`
**Feature Under Test:** `CommunityDashboardServiceImpl` — trending topics
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §5.2 trending query` — `WHERE NOT community_topics.is_hidden ... ORDER BY count DESC LIMIT n`

**Preconditions:** `FX-405` — a HIDDEN topic has the most questions; 2 visible topics have fewer

**Test Steps:**
1. Seed 3 topics (1 hidden with most questions)
2. Act + Assert

**Expected Result (PASS):**
- Hidden topic does NOT appear in `trendingTopics`
- Visible topics sorted by `questionCount` descending
- List length ≤ N (Open — recommend 5)

**Expected Result (FAIL):** Hidden topic surfaces in trending → ignores `is_hidden` filter.

**Current Status:** 🟢 Passing

---

### DASH-TC-108 — Invalid range (from > to) → 400 `MOD-021`

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityDashboardController`/service — range validation
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 MOD-021`

**Test Steps:** `GET .../dashboard?from=2026-06-30&to=2026-06-01`, SYSTEM_ADMIN JWT

**Expected Result (PASS):** `response.status == 400`, `error.code == "MOD-021"`.

**Current Status:** 🟢 Passing

---

### DASH-TC-109 — Empty DB → all counts 0, avg null, trending [] (no crash)

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityDashboardServiceImpl` — no-data robustness
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `§6.3 invariants`

**Preconditions:** `FX-410` — all repositories return empty/zero

**Expected Result (PASS):** total=0 for all metric groups, `avgHandlingTimeSeconds == null`, `trendingTopics.isEmpty()`, no exception.

**Current Status:** 🟢 Passing

---

### DASH-TC-110 — Response KHÔNG chứa row-level PII (PDPA gate)

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityDashboardResponse` DTO shape — PDPA compliance (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/dashboard/CommunityDashboardServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS ADR-004`, CLAUDE.md "never expose JPA entities"

**Test Steps:**
1. Act: `service.getDashboard(...)` (or serialize the response to JSON at integration level)
2. Assert: serialized response contains NO user email/name, NO question body/title, NO reporter id — only aggregate counts/averages + public topic names

**Expected Result (PASS):** Response JSON has no field matching PII patterns (email regex, entity type). Only `topicName` (public) + numeric aggregates present.

**Expected Result (FAIL):** Any personal field (name/email/question body/user id list) present → PDPA violation, BLOCKING.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ Most reviewer-sensitive test. A reflection/JSON-schema assertion that the DTO
type graph contains no `User`/`CommunityQuestion` entity and no email/name field is the durable form.

---

### SECURITY TEST CASES

### DASH-TC-111 — Non-SYSTEM_ADMIN → 403 `ACCESS_DENIED`

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `CommunityDashboardController` — `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/CommunityDashboardControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS ADR-002`, `GlobalExceptionHandler.java` (real 403 = `ACCESS_DENIED`)

**Preconditions:** JWT role `ROLE_MOTHER` (FX-408)

**Expected Result (PASS):** `response.status == 403`, `error.code == "ACCESS_DENIED"` (NOT `MOD-xxx`).

**Current Status:** 🟢 Passing

---

### DASH-TC-112 — MODERATOR vẫn 403 — no implicit hierarchy

**Severity:** `HIGH`
**Feature Under Test:** `@PreAuthorize ROLE_SYSTEM_ADMIN` — verifies no `RoleHierarchy`
**Test File:** `src/test/java/com/carebridge/backend/security/CommunityDashboardControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** Reused finding — no `RoleHierarchy` bean (TDS §16)

**Preconditions:** JWT role `ROLE_MODERATOR` (FX-407)

**Expected Result (PASS):** `response.status == 403`, `error.code == "ACCESS_DENIED"` — MODERATOR (closest admin role) still has no implicit access to this SYSTEM_ADMIN-only dashboard.

**Current Status:** 🟢 Passing

---

### DASH-TC-113 — No JWT → 401 bodiless

**Severity:** `HIGH`
**Feature Under Test:** JWT authentication entry point
**Test File:** `src/test/java/com/carebridge/backend/security/CommunityDashboardControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `HttpStatusEntryPoint` (reused finding)

**Test Steps:** `GET .../dashboard` no `Authorization` header (FX-409)

**Expected Result (PASS):** `response.status == 401`; body MAY be empty — MUST NOT assert any error code.

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

### DASH-TC-INT-001 — Full GET flow — counts khớp direct SQL

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/admin/community/dashboard` — end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/CommunityDashboardIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`

**Preconditions:** PostgreSQL Testcontainer, Flyway schema (no new migration); seed `FX-401..FX-405`; SYSTEM_ADMIN JWT

**Test Steps:**
1. Seed known users/questions/answers/reports/topics
2. `GET .../dashboard?from=...&to=...` SYSTEM_ADMIN JWT
3. Assert 200; cross-check each metric against a direct SQL aggregate over the same seed (TDS §14.1)

**Expected Result (PASS):** Every response metric equals the direct-SQL oracle value; handling-time avg matches known deltas; hidden topic excluded from trending.

**Current Status:** 🟢 Passing

---

### DASH-TC-INT-002 — Endpoint read-only — không mutate bảng nào

**Severity:** `HIGH`
**Feature Under Test:** Read-only guarantee (ADR §4.2 / C2)
**Test File:** `src/test/java/com/carebridge/backend/integration/CommunityDashboardIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §4.2 read-only NFR`

**Test Steps:**
1. Snapshot row counts of `users`/`community_questions`/`content_reports` (via `pg_stat_user_tables` or direct COUNT)
2. `GET .../dashboard` once
3. Re-check counts

**Expected Result (PASS):** No `n_tup_ins`/`n_tup_upd`/`n_tup_del` delta on the read tables (audit-log table excepted if ADR-005 Accepted).

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                            | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ------------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `DASH-TC-101`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-102`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-103`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | UC-102 suspended_until dependency (merged) |
| `DASH-TC-104`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-105`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-106`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | Computed in Java, not SQL EXTRACT(EPOCH) — see §2 |
| `DASH-TC-107`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | Exclusion enforced in repo query; unit test verifies mapping, DASH-TC-INT-001 verifies real exclusion |
| `DASH-TC-108`     | `CommunityDashboardControllerTest.java`                | `[x]` (see §5.1 note — passed at Red Gate, mocked-service exception wiring) | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-109`     | `CommunityDashboardServiceImplTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-110`     | `CommunityDashboardServiceImplTest.java`               | `[x]` (see §5.1 note — passed at Red Gate, pure DTO reflection, no service call) | 2026-07-02 (uncommitted) | ★ PDPA gate          |
| `DASH-TC-111`     | `CommunityDashboardControllerSecurityTest.java`        | `[x]` (see §5.1 note — passed at Red Gate, `@PreAuthorize`) | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-112`     | `CommunityDashboardControllerSecurityTest.java`        | `[x]` (see §5.1 note — passed at Red Gate, `@PreAuthorize`) | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-113`     | `CommunityDashboardControllerSecurityTest.java`        | `[x]` (see §5.1 note — passed at Red Gate, filter chain) | 2026-07-02 (uncommitted) | —                    |
| `DASH-TC-INT-001` | `CommunityDashboardIntegrationTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | Hosted as `@SpringBootTest`+H2 (real beans), not Testcontainers — none exist project-wide |
| `DASH-TC-INT-002` | `CommunityDashboardIntegrationTest.java`               | `[x]`               | 2026-07-02 (uncommitted) | Verified via repository `.count()` deltas, not `pg_stat_user_tables` (H2, not Postgres) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class CommunityDashboardServiceImpl implements CommunityDashboardService {
    @Override
    public CommunityDashboardResponse getDashboard(DashboardFilter filter, Principal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause (nếu PASS bất thường) |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------------------------------ |
| `DASH-TC-101`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-102`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-103`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-104`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-105`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-106`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-107`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-108`     | N/A — service mocked directly in this controller test | 🔴 FAIL (expected, per spec design) | ☐ FAIL ☑ PASS | Not AP-AI-002: this test mocks `CommunityDashboardService` to throw `ModerationException` directly (never invokes the real, stubbed impl) — it verifies the exception→HTTP(400/MOD-021) wiring in `GlobalExceptionHandler`, which already worked before this feature. |
| `DASH-TC-109`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `DASH-TC-110`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☑ PASS     | Not AP-AI-002: this test performs pure reflection over the DTO record classes (`CommunityDashboardResponse` + nested records) and never calls `service.getDashboard()` — the subject under test (DTO field shape) is independent of the stub. |
| `DASH-TC-111`     | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☑ PASS     | Not AP-AI-002: `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` + the explicit `SecurityConfig` matcher reject the MOTHER-role JWT before the (stubbed) service is ever reached — same class of legitimate pass as UC-110's RFR-TC-SEC-001. |
| `DASH-TC-112`     | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☑ PASS     | Not AP-AI-002 — same as DASH-TC-111 (MODERATOR rejected before reaching service). |
| `DASH-TC-113`     | N/A — auth filter runs before controller/service | 🔴 FAIL (still 401, but for a different reason) | ☐ FAIL ☑ PASS | Not AP-AI-002 — `HttpStatusEntryPoint` rejects the missing-JWT request before DispatcherServlet routes to the controller; confirmed meaningful, not vacuous (same real path independently verified for UC-110 RFR-TC-SEC-002). |
| `DASH-TC-INT-001` | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |

**Red Gate Evidence:**
- Stub commit hash: `uncommitted` *(RED gate captured 2026-07-02; work not yet committed to git)*
- Tất cả FAIL? ☑ Yes → GATE-2 PASS (T2→T3) — 11/16 stubs FAIL as required; the 5 exceptions above are documented, verified-legitimate (not AP-AI-002)
- Log file: `mvn test -Dtest=CommunityDashboardServiceImplTest,CommunityDashboardControllerTest,CommunityDashboardControllerSecurityTest,CommunityDashboardIntegrationTest` (2026-07-02, local run — 1 failure + 10 errors = 11 FAIL, 5 PASS as documented)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-MOD-IMP-006` reviewed; `Open` items (SLA, trending `n`, default window, ADR-005 audit) acknowledged — user confirmed approve 2026-07-02; Open items resolved to recommended defaults (n=5, 30-day window) during implementation
- [x] No migration needed — confirmed
- [x] Fixtures FX-401..FX-410 prepared (seeded directly in unit-test mocks / integration-test repository calls; no literal `FX-nnn`-named factory objects, same functional coverage)
- [x] UC-102 merged — full `active`-user predicate (`suspended_until`) used, no degraded-predicate path needed

### Exit Criteria (DoD)
- [x] `./mvnw test -Dtest=CommunityDashboardServiceImplTest` — 10/10 PASS
- [x] `./mvnw test -Dtest=CommunityDashboardControllerTest` — 1/1 PASS
- [x] `./mvnw test -Dtest=CommunityDashboardControllerSecurityTest` — 3/3 PASS
- [x] `./mvnw test -Dtest=CommunityDashboardIntegrationTest` — 2/2 PASS — **deviation, documented:** `@SpringBootTest`+H2 (real beans), not Testcontainers — none exist project-wide (CLAUDE.md: no new deps without approval)
- [x] DASH-TC-110: no row-level PII in response — VERIFIED (CRITICAL PDPA gate)
- [x] DASH-TC-111/112/113: RBAC (non-admin/MODERATOR/no-JWT) rejected correctly — VERIFIED (CRITICAL security gate)
- [x] DASH-TC-INT-002: read-only (no table mutation) — VERIFIED via repository `.count()` deltas (H2 has no `pg_stat_user_tables`)
- [x] No business logic in controller (only `@Valid`-equivalent param binding + delegate)

**Exit Criteria bổ sung — CASE 2.0:**
- [x] Red Gate (§5.1) — 11/16 tests FAIL with throw stub; 5 legitimate exceptions documented
- [x] Contract Existence — `./mvnw compile` clean for new classes (`CommunityDashboardResponse` + nested records, `DashboardFilter`, `CommunityDashboardService`, MOD-021 factory) — verified 2026-07-02
- [x] Metric grounding — every asserted metric traces to a §5.2 column (no hallucinated metric)
- [x] Oracle Source — every expected value cites a column/ADR

### Suspension Criteria
- ADR-003/ADR-005 or `Open` items unresolved in a way that changes the contract
- `@EnableMethodSecurity` not enabled
- CI broken by unrelated change

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/    # read-only endpoint, safe to revert
# No migration to revert (UC-111 has no schema delta).
# Test spec files retained.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                           | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002/ADR-004                                             | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | DASH-TC-101..110 PASS với throw stub                                                   | `[x]`   | G-2 ★       |
| AP-AI-002 | PII Leak                 | Không có TC nào assert vắng mặt PII (thiếu DASH-TC-110)                                | `[x]`   | G-2 ★       |
| AP-AI-003 | Hallucinated Metric      | TC assert một metric không có cột hậu thuẫn (§5.2)                                     | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | TC verify Controller query DB trực tiếp                                               | `[x]`   | G-4         |
| AP-AI-005 | Hidden Write             | TC không phát hiện được nếu endpoint "read" lén ghi DB (thiếu DASH-TC-INT-002)         | `[x]`   | G-4         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

**Post-implementation re-check (2026-07-02):** grep for `@Cacheable`/`CacheManager` in `content` package: no output (no unauthorized caching added). Controller contains only `@PreAuthorize` + param binding + delegation — no DB access. Service confirmed read-only (no `.save()`/`.delete()` calls). No anti-patterns found.

---

*Test-Spec v2.0 (CASE 2.0 Anti-Pattern Detection & Red Gate Protocol) — Status: Implemented — 2026-07-02 (16/16 PASS).*
*Read-only analytics endpoint; no schema delta. PII-absence (DASH-TC-110) and RBAC (DASH-TC-111/112/113) are
the CRITICAL gates. Active-user metric depends on UC-102's `suspended_until` — degraded predicate documented
if UC-102 not yet merged. `Open` items (trending N, default window, ADR-005 audit) flagged for human review.*
