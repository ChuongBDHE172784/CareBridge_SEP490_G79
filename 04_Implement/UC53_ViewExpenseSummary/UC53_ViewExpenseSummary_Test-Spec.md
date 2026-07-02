# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# UC53 — View Expense Summary — Test Specification

**Document ID:** `CB-CAREJOURNEY-TDD-053`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`public.expenses` lines 767-779, `public.mother_journeys` lines 559-571)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.30 (UC-53, table 132) — functional requirements
- `04_Implement/UC53_ViewExpenseSummary/UC53_ViewExpenseSummary_TDS.md` — Technical Specification (this feature's TDS)
- `04_Implement/UC52_UpdateExpense/UC52_UpdateExpense_TDS.md` — sibling feature sharing the `Expense` entity
- `CLAUDE.md` — architecture rules

> **TDD convention:** Tests are written BEFORE production code. Mandatory order: write test → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test ✅ until `./mvnw test` (backend) is green.
> No real PII in test data — SYNTHETIC only.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Initial draft — Test-Spec for UC53 View Expense Summary |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC53-ViewExpenseSummary` |
| **Module** | `Care Journey — Expense Management` |
| **Spec gốc** | `CB-CAREJOURNEY-IMP-053` (UC53 TDS) |
| **Priority** | 🔴 P0 (SRS Priority: High) |
| **Sprint** | `Open` — not scheduled in `function-spec-task-allocation.md` |
| **Milestone** | `Open` |
| **Data Classification** | `PII` (aggregated family financial data) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT auth)`, `public.expenses`, `public.mother_journeys`, UC52's `Expense` JPA entity |
| **Downstream Consumers** | Mobile Care Journey dashboard (screen not yet identified — Open) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC53_ViewExpenseSummary_TDS.md §3 ADR-CJ-053-01/02/03`, `§8 Interface Specification`, `§16 Authorization Matrix` |
| **Constraints Injected** | Stage grouping via LEFT JOIN to `mother_journeys.journey_type` with `UNSPECIFIED` fallback; no target-user parameter (ownership implicit); aggregate-only response, no raw rows; group by `(bucket, currency)` — never sum mismatched currencies |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS says "by month, stage, or category" without defining what "stage" means at the data level | `expenses` has no `stage` column; only `mother_journeys.journey_type` (unconstrained varchar) is stage-adjacent | Tests treat `stage` grouping as a pass-through of whatever `journey_type` string is stored, plus an explicit `UNSPECIFIED` bucket for `journey_id IS NULL` expenses |
| L2 | Brief assumed greenfield schema | `public.expenses` and `public.mother_journeys` already exist in `V1__init_schema.sql` (confirmed by direct read) | Tests assume both tables pre-exist; no schema-creation test needed |
| L3 | No explicit rule for mixed-currency totals | `expenses.currency varchar(10) DEFAULT 'VND'` — multiple currencies are technically possible per user | Tests assert that buckets are keyed by `(groupKey, currency)` pair and `grandTotal` is a per-currency map, never a single summed number across currencies |
| L4 | SRS AF2 says "no matching data" → "empty state", not specific to this UC, but generically applicable | No DB rows for a given user/filter combination | Tests assert `200 OK` with empty `buckets: []` and `grandTotal: {}`, never `404`/`500` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Care Journey — Expense Summary (UC53) covers:
├── Domain (ExpenseGroupBy enum validation — pure logic)
├── Service (ExpenseSummaryService — mock Repository with Mockito)
├── Controller (ExpenseSummaryController — mock Service with @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, full stack via @SpringBootTest, real GROUP BY queries)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-53 (3.3.1.30)` | Summary by month/stage/category, PRE-1..4, POST-1..3, AF1-3, E1-3 |
| `V1__init_schema.sql` (public.expenses, public.mother_journeys) | Column definitions, FK relationships, absence of `stage` column |
| `ADR-CJ-053-01` (UC53 TDS §3) | Grouping mode semantics, `UNSPECIFIED` fallback for stage |
| `ADR-CJ-053-02` (UC53 TDS §3) | No target-user parameter; implicit ownership |
| `ADR-CJ-053-03` (UC53 TDS §3) | Aggregate-only response; per-currency bucket keys |
| `BR-RBAC` | Role restriction to `MOTHER` |
| `BR-PRIVACY` | No raw transaction detail in summary response |
| `CB-CAREJOURNEY-IMP-053` §9-10 | API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Summary grouped by MONTH returns correct totals | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-001` |
| TC-COND-002 | Summary grouped by CATEGORY returns correct totals | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-002` |
| TC-COND-003 | Summary grouped by STAGE joins mother_journeys correctly | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-003` |
| TC-COND-004 | Expenses with NULL journeyId bucket into "UNSPECIFIED" under STAGE grouping | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-004` |
| TC-COND-005 | Empty result set returns 200 with empty buckets (not 404/500) | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-005` |
| TC-COND-006 | Mixed-currency expenses never summed together in one bucket | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-006` |
| TC-COND-007 | Invalid `groupBy` value rejected with 400/EXPSUM-001 | `ExpenseSummaryController` | `EXPSUM-TC-007` |
| TC-COND-008 | `from > to` date range rejected with 400/EXPSUM-001 | `ExpenseSummaryRequest` validation | `EXPSUM-TC-008` |
| TC-COND-009 | Summary only includes caller's own expenses (ownership isolation) | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-009` |
| TC-COND-010 | Unauthenticated request rejected (401) | `ExpenseSummaryController` | `EXPSUM-TC-010` |
| TC-COND-011 | Non-Mother role rejected (403) | `ExpenseSummaryController` | `EXPSUM-TC-011` |
| TC-COND-012 | Summary total equals raw SUM(amount) on seeded data | Integration — `ExpenseSummaryRepository` | `EXPSUM-TC-INT-001` |
| TC-COND-013 | Full API happy path via MockMvc/Testcontainers, all 3 groupBy modes | `ExpenseSummaryController` E2E | `EXPSUM-TC-E2E-001` |
| TC-COND-014 | journeyId/babyId optional filters narrow the aggregation correctly | `ExpenseSummaryService.getSummary()` | `EXPSUM-TC-012` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `groupBy` (valid enum values vs. invalid string) | Enum-bound input domain |
| Boundary Value Analysis | `from == to` (single-day range), empty result set (0 rows) | Edge of date-range and data-availability |
| Error Guessing | Ownership-bypass attempt via crafted `journeyId`/`babyId` belonging to another user | Security regression coverage — verifies filters never override the `owner_user_id` scope |
| State Transition Testing | N/A — stateless read endpoint | Not applicable; explicitly skipped |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-101` | DB seed | Mother A: 3 expenses in `2026-05` (`VND`, category `Vaccination`/`Diapers`), 1 in `2026-06` (`VND`, category `Checkup`), all linked to `journeyId = J1` where `mother_journeys.journey_type = 'PREGNANCY'` | Month/category/stage happy path |
| `FX-102` | DB seed | Mother A: 1 expense with `journeyId = NULL` | UNSPECIFIED stage bucket test |
| `FX-103` | DB seed | Mother A: 1 expense with `currency = 'USD'` in the same month as a `VND` expense | Mixed-currency bucket isolation test |
| `FX-104` | DB seed | Mother B: 2 expenses (different owner) | Ownership isolation negative test |
| `FX-105` | JWT | `{ sub: motherA-userId, role: 'MOTHER' }` | Auth context for owner |
| `FX-106` | JWT | `{ sub: expertX-userId, role: 'EXPERT' }` | Role-level rejection test |

---

## 4. Test Case Specification

> **TC ID format:** `EXPSUM-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — MANDATORY)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

// ExpenseSummaryTestFactory.java
class ExpenseSummaryTestFactory {

    static final UUID MOTHER_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID MOTHER_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");
    static final UUID JOURNEY_1_ID = UUID.fromString("00000000-0000-0000-0000-0000000000J1");

    static Expense makeExpense(Consumer<Expense> overrides) {
        Expense expense = Expense.builder()
                .expenseId(UUID.randomUUID())
                .ownerUserId(MOTHER_A_ID)
                .journeyId(JOURNEY_1_ID)
                .babyId(null)
                .category("Vaccination")
                .amount(new BigDecimal("300000"))
                .currency("VND")
                .expenseDate(LocalDate.of(2026, 5, 10))
                .note("seed")
                .build();
        overrides.accept(expense);
        return expense;
    }

    static MotherJourney makeJourney(Consumer<MotherJourney> overrides) {
        MotherJourney journey = MotherJourney.builder()
                .journeyId(JOURNEY_1_ID)
                .ownerUserId(MOTHER_A_ID)
                .journeyType("PREGNANCY")
                .build();
        overrides.accept(journey);
        return journey;
    }

    static ExpenseSummaryRequest makeRequest(Consumer<ExpenseSummaryRequest> overrides) {
        ExpenseSummaryRequest request = new ExpenseSummaryRequest();
        request.setGroupBy(ExpenseGroupBy.MONTH);
        overrides.accept(request);
        return request;
    }
}
```

---

### EXPSUM-TC-001 — Summary grouped by MONTH returns correct totals

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-CJ-053-01 (UC53 TDS §3, §6.1)` — month bucket derived from `expense_date`

**Preconditions:** Fixture `FX-101`, `FX-105`

**Test Steps:**
1. Arrange: mock repository `sumByMonth(...)` → 2 buckets `[{2026-05, VND, 900000, 2}, {2026-06, VND, 200000, 1}]` matching `FX-101` shape
2. Act: call `getSummary(MOTHER_A_ID, makeRequest(r -> r.setGroupBy(ExpenseGroupBy.MONTH)))`
3. Assert: response `buckets` has 2 entries with exact `groupKey`/`totalAmount`/`count`; `grandTotal.get("VND") == 1100000`

**Expected Result (PASS):** Exact bucket values and correct grand total sum.
**Expected Result (FAIL):** Missing bucket, wrong total, or wrong currency key.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-002 — Summary grouped by CATEGORY returns correct totals

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CJ-053-01` — category grouping is a direct pass-through of `expenses.category`

**Preconditions:** Fixture `FX-101`, `FX-105`

**Test Steps:**
1. Arrange: mock `sumByCategory(...)` → buckets for `Vaccination`, `Diapers`, `Checkup`
2. Act: call `getSummary(MOTHER_A_ID, makeRequest(r -> r.setGroupBy(ExpenseGroupBy.CATEGORY)))`
3. Assert: 3 buckets returned with `groupKey` equal to the literal category strings

**Expected Result (PASS):** Bucket keys match category strings exactly (case-sensitive, no normalization invented).
**Expected Result (FAIL):** Categories merged incorrectly or missing.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-003 — Summary grouped by STAGE joins mother_journeys.journey_type correctly

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-CJ-053-01` — stage = `mother_journeys.journey_type` via LEFT JOIN

**Preconditions:** Fixture `FX-101` (journey type `PREGNANCY`), `FX-105`

**Test Steps:**
1. Arrange: mock `sumByStage(...)` → 1 bucket `{groupKey: "PREGNANCY", currency: VND, totalAmount: 1100000, count: 3}`
2. Act: call `getSummary(MOTHER_A_ID, makeRequest(r -> r.setGroupBy(ExpenseGroupBy.STAGE)))`
3. Assert: bucket `groupKey == "PREGNANCY"` (pass-through, no invented canonical stage list per ADR-CJ-053-01 Open item)

**Expected Result (PASS):** Bucket key equals the raw `journey_type` string stored in `mother_journeys`.
**Expected Result (FAIL):** Stage value transformed/translated without basis, or journey not joined at all.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-004 — Expenses with NULL journeyId bucket into "UNSPECIFIED" under STAGE grouping

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-CJ-053-01` — LEFT JOIN with `COALESCE(journey_type, 'UNSPECIFIED')`, explicit decision to never drop these rows (AF2 rationale)

**Preconditions:** Fixture `FX-102` (expense with `journeyId = NULL`), `FX-105`

**Test Steps:**
1. Arrange: mock `sumByStage(...)` → includes bucket `{groupKey: "UNSPECIFIED", ...}` reflecting `FX-102`
2. Act: call `getSummary(MOTHER_A_ID, makeRequest(r -> r.setGroupBy(ExpenseGroupBy.STAGE)))`
3. Assert: an `"UNSPECIFIED"` bucket is present and its `count`/`totalAmount` include the NULL-journey expense

**Expected Result (PASS):** `"UNSPECIFIED"` bucket present with correct aggregation; no expense silently dropped from the total.
**Expected Result (FAIL):** NULL-journey expense missing from summary entirely (data loss in the view) or exception thrown.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-005 — Empty result set returns 200 with empty buckets (not error)

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseSummaryService.getSummary()` / `ExpenseSummaryController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** SRS AF2 (generic UC template) — "no matching data" → empty state, not an error

**Preconditions:** No expenses seeded for the calling user

**Test Steps:**
1. Arrange: mock repository query → empty list
2. Act: `GET /api/v1/expenses/summary?groupBy=MONTH`
3. Assert: HTTP `200`, body `data.buckets == []`, `data.grandTotal == {}`

**Expected Result (PASS):** `200` with empty structures.
**Expected Result (FAIL):** `404`/`500` returned instead, or `null` body.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-006 — Mixed-currency expenses never summed together in one bucket

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-CJ-053-03 (UC53 TDS §3)` — no FX conversion invented; group by `(groupKey, currency)`

**Preconditions:** Fixture `FX-103` (one `VND` and one `USD` expense in the same month)

**Test Steps:**
1. Arrange: mock `sumByMonth(...)` → 2 separate buckets for the same month, one per currency
2. Act: call `getSummary(MOTHER_A_ID, makeRequest(r -> r.setGroupBy(ExpenseGroupBy.MONTH)))`
3. Assert: response contains 2 buckets with identical `groupKey` (same month) but different `currency`; `grandTotal` has separate `VND` and `USD` keys, never one combined number

**Expected Result (PASS):** Currencies never mixed into a single total.
**Expected Result (FAIL):** A single bucket/total silently adds `VND` + `USD` numeric values together (financially incorrect and unsafe).

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-007 — Invalid groupBy value rejected (400/EXPSUM-001)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpenseSummaryController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC53 TDS §6.4, §10` — invalid enum binding → 400/EXPSUM-001

**Preconditions:** None

**Test Steps:**
1. Act: `GET /api/v1/expenses/summary?groupBy=INVALID`
2. Assert: `400` with `error.code == "EXPSUM-001"`

**Expected Result (PASS):** `400`/`EXPSUM-001`.
**Expected Result (FAIL):** `500` (unhandled binding exception) or silently defaults to a groupBy value.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-008 — from > to date range rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpenseSummaryRequest` validation
**Test File:** `src/test/java/com/carebridge/backend/carejourney/dto/ExpenseSummaryRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC53 TDS §8.1` — cross-field `from <= to` validation

**Preconditions:** None

**Test Steps:**
1. Arrange: `makeRequest(r -> { r.setFrom(LocalDate.of(2026,6,1)); r.setTo(LocalDate.of(2026,1,1)); })`
2. Act: validate
3. Assert: violation present referencing the date range

**Expected Result (PASS):** Validation error raised.
**Expected Result (FAIL):** No violation; invalid range silently accepted and produces confusing/empty results downstream.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-009 — Summary only includes caller's own expenses (ownership isolation)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-CJ-053-02 (UC53 TDS §3)` — no target-user parameter; repository always scoped by `owner_user_id`

**Preconditions:** Fixture `FX-101` (Mother A), `FX-104` (Mother B), `FX-105`

**Test Steps:**
1. Arrange: verify repository method signature has no `targetUserId`/similar parameter that could be attacker-controlled — call `getSummary(MOTHER_A_ID, request)`
2. Act: assert repository was invoked with `ownerUserId == MOTHER_A_ID` exactly (Mockito `verify` with argument captor)
3. Assert: resulting totals only reflect `FX-101` amounts, never `FX-104` (Mother B's) amounts

**Expected Result (PASS):** Repository call scoped strictly to the authenticated caller; Mother B's data never appears.
**Expected Result (FAIL):** Any Mother B amount leaks into Mother A's summary.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-010 — Unauthenticated request rejected (401)

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `ExpenseSummaryController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC53 TDS §9.1, §16`

**Preconditions:** No `Authorization` header

**Test Steps (Attack Simulation):**
1. `GET /api/v1/expenses/summary` with no auth header
2. Assert `401`

**Expected Result (PASS = safe):** `401 Unauthorized`.
**Expected Result (FAIL = vulnerability):** `200` returned without auth.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-011 — Non-Mother role rejected (403)

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpenseSummaryController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC53 TDS §16 Authorization Matrix`

**Preconditions:** Fixture `FX-106` (`EXPERT` role)

**Test Steps (Attack Simulation):**
1. Authenticate as `EXPERT`
2. `GET /api/v1/expenses/summary`
3. Assert `403`/`EXPSUM-003`

**Expected Result (PASS):** `403`.
**Expected Result (FAIL):** Request succeeds for wrong role.

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-012 — journeyId/babyId optional filters narrow aggregation correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpenseSummaryService.getSummary()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** SRS AF3 (generic UC template) — "optional filters ... may be added when the screen supports them"; UC53 TDS §8.1

**Preconditions:** Fixture `FX-101` (journeyId `J1`), plus a second seeded expense under a different `journeyId`

**Test Steps:**
1. Arrange: request with `journeyId = J1`
2. Act: call `getSummary(MOTHER_A_ID, request)`
3. Assert: repository invoked with `journeyId = J1`; response excludes totals from the other journey

**Expected Result (PASS):** Filter narrows results correctly.
**Expected Result (FAIL):** Filter ignored; all expenses returned regardless of `journeyId`.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### EXPSUM-TC-INT-001 — Summary total equals raw SUM(amount) on seeded data

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: ExpenseSummaryController → ExpenseSummaryService → ExpenseSummaryRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/ExpenseSummaryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: 5 `expenses` rows for Mother A across 2 months/2 categories/1 journey (`FX-101`+`FX-102`+`FX-103` combined)

**Test Steps:**
1. Seed data directly via JPA/JdbcTemplate
2. Call `GET /api/v1/expenses/summary?groupBy=MONTH`
3. Independently compute `SELECT currency, SUM(amount) FROM expenses WHERE owner_user_id=? GROUP BY currency` via raw JDBC
4. Assert the API's `grandTotal` matches the raw query result exactly, per currency

**Expected Result (PASS):** API aggregation matches raw SQL aggregation bit-for-bit (BigDecimal comparison).
**Expected Result (FAIL):** Any discrepancy between API total and raw SQL total.

**DB Assertion:**
```java
BigDecimal rawTotalVnd = jdbcTemplate.queryForObject(
    "SELECT SUM(amount) FROM expenses WHERE owner_user_id = ? AND currency = 'VND'",
    BigDecimal.class, motherAId);
assertThat(response.getData().getGrandTotal().get("VND")).isEqualByComparingTo(rawTotalVnd);
```

**Current Status:** 🔴 Not written

---

### EXPSUM-TC-E2E-001 — Full happy-path API flow across all 3 groupBy modes

**Severity:** `HIGH`
**Feature Under Test:** `Full flow via real HTTP`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/ExpenseSummaryE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** Seeded Mother JWT (`FX-105`), seeded expenses (`FX-101`, `FX-102`)

**Test Steps:**
1. `GET /api/v1/expenses/summary?groupBy=MONTH` → assert `200`, buckets present
2. `GET /api/v1/expenses/summary?groupBy=STAGE` → assert `200`, includes `PREGNANCY` and `UNSPECIFIED` buckets
3. `GET /api/v1/expenses/summary?groupBy=CATEGORY` → assert `200`, category buckets present

**Expected Result (PASS):** All 3 calls succeed with correct shape.
**Expected Result (FAIL):** Any call errors or returns incorrect grouping.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPSUM-TC-001` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-002` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-003` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-004` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-005` | `ExpenseSummaryControllerTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-006` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-007` | `ExpenseSummaryControllerTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-008` | `ExpenseSummaryRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-009` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-010` | `ExpenseSummaryControllerTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-011` | `ExpenseSummaryControllerTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-012` | `ExpenseSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-INT-001` | `ExpenseSummaryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `EXPSUM-TC-E2E-001` | `ExpenseSummaryE2ETest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (MUST throw)
@Service
public class ExpenseSummaryServiceImpl implements IExpenseSummaryService {

    @Override
    public ExpenseSummaryResponse getSummary(UUID ownerUserId, ExpenseSummaryRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPSUM-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `EXPSUM-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-007` | Enum binding failure occurs in Spring MVC before hitting stub | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-008` | N/A — pure Bean Validation, no service stub involved | 🔴 FAIL (until validator added) | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-010` | Security filter returns 401 before hitting stub | 🔴 FAIL (if security config missing) | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-011` | Security filter returns 403 before hitting stub | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPSUM-TC-E2E-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___` (to be filled at implementation time)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CAREJOURNEY-IMP-053` reviewed and Approved
- [ ] Logic Issues (§2) confirmed with Principal Architect / user
- [ ] UC52's `Expense` JPA entity/repository exists (shared dependency) — coordinate implementation order
- [ ] No Flyway migration blocking (confirmed §3 TDS-02 — no schema change)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Test coverage ≥ 80% lines for `ExpenseSummaryService`
- [ ] No business logic in `ExpenseSummaryController` (validation + mapping only)
- [ ] No PII/secret in plaintext logs
- [ ] Empty state always returns 200 (verified by `EXPSUM-TC-005`)
- [ ] Mixed-currency totals never merged (verified by `EXPSUM-TC-006`)
- [ ] Ownership isolation verified (verified by `EXPSUM-TC-009`)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — `./mvnw compile` produces no errors
- [ ] **Props Isolation** — no shared mutable state between tests (all via `ExpenseSummaryTestFactory`)
- [ ] **Oracle Source** — every expected value cites BR/ADR/AC per test case above

### Suspension Criteria

- UC52's `Expense` entity/repository not yet available (blocking shared dependency)
- Canonical "stage"/`journey_type` value list required by product before proceeding (ADR-CJ-053-01 Open item)
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No new migration for UC53 — code-only rollback
git checkout -- src/main/java/com/carebridge/backend/carejourney/
git checkout -- src/test/java/com/carebridge/backend/carejourney/

# Gap remains OPEN in tracking until re-approved
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào trong spec draft này (self-check by author at draft time — pending independent reviewer confirmation)
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected at draft time)_ | | | | |

---

*Test-Spec v1.0 (Draft) — UC53 View Expense Summary.*
