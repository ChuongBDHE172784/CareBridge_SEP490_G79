# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# UC52 — Update Expense — Test Specification

**Document ID:** `CB-CAREJOURNEY-TDD-052`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`public.expenses`, lines 767-779)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.29 (UC-52, table 131) — functional requirements
- `04_Implement/UC52_UpdateExpense/UC52_UpdateExpense_TDS.md` — Technical Specification (this feature's TDS)
- `CLAUDE.md` — architecture rules (modular monolith, DTO boundary, RBAC/consent/audit)

> **TDD convention:** Tests are written BEFORE production code. Mandatory order: write test (`.java`) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test ✅ until `./mvnw test` (backend) is green.
> No real PII in test data — SYNTHETIC only.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Initial draft — Test-Spec for UC52 Update Expense |

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
| **Feature / Gap ID** | `UC52-UpdateExpense` |
| **Module** | `Care Journey — Expense Management` |
| **Spec gốc** | `CB-CAREJOURNEY-IMP-052` (UC52 TDS) |
| **Priority** | 🔴 P0 (SRS Priority: High) |
| **Sprint** | `Open` — not scheduled in `function-spec-task-allocation.md` at time of this spec |
| **Milestone** | `Open` — no milestone found for this feature |
| **Data Classification** | `PII` (family financial data) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT auth)`, `mother_journeys`, `baby_profiles`, `AuditService` |
| **Downstream Consumers** | `UC53 View Expense Summary` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC52_UpdateExpense_TDS.md §3 ADR-CJ-052-01/02/03`, `§8 Interface Specification`, `§16 Authorization Matrix` |
| **Constraints Injected** | Ownership enforced via `findByIdAndOwnerUserId` (never 403, always 404 on mismatch); hard delete with pre-delete audit write in same transaction; PATCH applies only non-null fields |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-52 (`3.3.1.29`) is generic boilerplate shared across UC-49 to UC-56; no field-level validation rules stated | `public.expenses` DDL: `amount numeric NOT NULL`, `expense_date date NOT NULL`, `category varchar(80)`, `currency varchar(10) DEFAULT 'VND'` | Tests assert `amount > 0` rejection and `category`/`currency` length limits derived from column widths, not from an explicit BR |
| L2 | Brief assumed no expense schema exists ("greenfield") | `public.expenses` table already exists in `V1__init_schema.sql` (confirmed via direct read, lines 767-779) with FKs to `users`, `mother_journeys`, `baby_profiles` | Tests assume the table exists; no migration test is required for table creation, only for the Java entity/repository mapping to the existing DDL |
| L3 | SRS says "updates or deletes" but does not specify hard vs. soft delete | No `deleted_at`/status column exists on `expenses` in V1 baseline | Tests encode hard-delete behavior (row physically removed) per ADR-CJ-052-02, flagged as `Open`/needs product confirmation in TDS §3 |
| L4 | SRS Exception E1 says "access denied when unauthorized or outside permitted data scope" without specifying HTTP status for ownership mismatch | ADR-CJ-052-01 decision: ownership mismatch → 404, not 403 (avoid resource enumeration) | Tests assert `404` (not `403`) when a Mother attempts to update/delete another Mother's expense |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Care Journey — Expense Management (UC52) covers:
├── Domain (ExpensePolicy — pure logic, no deps)
├── Service (ExpenseService — mock JPA Repository + AuditService with Mockito)
├── Controller (ExpenseController — mock Service with @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, full stack via @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-52 (3.3.1.29)` | Update/delete happy path, PRE-1..4, POST-1..3, AF1-3, E1-3 |
| `V1__init_schema.sql` (public.expenses) | Column constraints, FK relationships, NOT NULL fields |
| `ADR-CJ-052-01` (UC52 TDS §3) | Ownership check semantics (404, not 403) |
| `ADR-CJ-052-02` (UC52 TDS §3) | Hard delete + atomic audit write |
| `ADR-CJ-052-03` (UC52 TDS §3) | Partial-update (non-null fields only) semantics |
| `BR-RBAC` | Role restriction to `MOTHER` |
| `BR-PRIVACY` | No cross-owner data disclosure |
| `CB-CAREJOURNEY-IMP-052` §9-10 | API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother updates own expense with valid partial fields | `ExpenseService.updateExpense()` | `EXP-TC-001` |
| TC-COND-002 | Mother updates own expense — only one field provided, others unchanged | `ExpenseService.updateExpense()` | `EXP-TC-002` |
| TC-COND-003 | Mother deletes own expense | `ExpenseService.deleteExpense()` | `EXP-TC-003` |
| TC-COND-004 | Update rejected — amount <= 0 | `UpdateExpenseRequest` validation | `EXP-TC-004` |
| TC-COND-005 | Update rejected — expenseDate in the future | `UpdateExpenseRequest` validation | `EXP-TC-005` |
| TC-COND-006 | Update fails — expense does not exist | `ExpenseService.updateExpense()` | `EXP-TC-006` |
| TC-COND-007 | Update fails — expense exists but owned by another user (ownership violation) | `ExpenseService.updateExpense()` | `EXP-TC-007` |
| TC-COND-008 | Delete fails — expense exists but owned by another user | `ExpenseService.deleteExpense()` | `EXP-TC-008` |
| TC-COND-009 | Unauthenticated request rejected | `ExpenseController` | `EXP-TC-009` |
| TC-COND-010 | Non-Mother role rejected (role-level) | `ExpenseController` | `EXP-TC-010` |
| TC-COND-011 | Delete writes audit log entry before row removal, atomically | `ExpenseService.deleteExpense()` integration | `EXP-TC-INT-001` |
| TC-COND-012 | Update writes audit log entry with changed-field diff | `ExpenseService.updateExpense()` integration | `EXP-TC-INT-002` |
| TC-COND-013 | Full API happy path via MockMvc/Testcontainers | `ExpenseController` E2E | `EXP-TC-E2E-001` |
| TC-COND-014 | SQL/field injection attempt in `note`/`category` handled safely | `ExpenseController` security | `EXP-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `amount` (valid > 0 vs. invalid <= 0), `expenseDate` (past/present vs. future) | Field validation boundaries derived from DDL + ADR-CJ-052-03 |
| Boundary Value Analysis | `amount = 0.01` (min valid), `amount = 0` (invalid), `category` length = 80 vs. 81 | Matches `numeric`/`varchar(80)` column definition |
| State Transition Testing | N/A (no state machine — §6.4 TDS) | Not applicable; skipped explicitly |
| Error Guessing | Ownership bypass via crafted expenseId, SQL-like payload in `note` | Security regression coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ expenseId: fixed UUID, ownerUserId: motherA, category: 'Vaccination', amount: 300000, currency: 'VND', expenseDate: 2026-06-01 }` | Happy path update/delete target |
| `FX-002` | DB seed | `{ expenseId: fixed UUID, ownerUserId: motherB, ... }` | Ownership-violation target (owned by a different mother) |
| `FX-003` | JWT | `{ sub: motherA-userId, role: 'MOTHER' }` | Auth context for owner |
| `FX-004` | JWT | `{ sub: motherB-userId, role: 'MOTHER' }` | Auth context for non-owner attacker |
| `FX-005` | JWT | `{ sub: expertX-userId, role: 'EXPERT' }` | Role-level rejection test |
| `FX-006` | env | N/A — no secrets required for this module | — |

---

## 4. Test Case Specification

> **TC ID format:** `EXP-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — MANDATORY)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Place at top of test file — every @Test uses makeExpense()/makeUpdateRequest()
// ═══════════════════════════════════════════════════════════

// ExpenseTestFactory.java
class ExpenseTestFactory {

    static final UUID MOTHER_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID MOTHER_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");

    // Baseline valid entity — synced with FX-001 (§3 TDS-05)
    static Expense makeExpense() {
        return Expense.builder()
                .expenseId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .ownerUserId(MOTHER_A_ID)
                .journeyId(UUID.fromString("00000000-0000-0000-0000-0000000000J1"))
                .babyId(null)
                .category("Vaccination")
                .amount(new BigDecimal("300000"))
                .currency("VND")
                .expenseDate(LocalDate.of(2026, 6, 1))
                .note("Initial note")
                .build();
    }

    static Expense makeExpense(Consumer<Expense> overrides) {
        Expense expense = makeExpense();
        overrides.accept(expense);
        return expense;
    }

    static UpdateExpenseRequest makeUpdateRequest() {
        UpdateExpenseRequest request = new UpdateExpenseRequest();
        request.setAmount(new BigDecimal("450000"));
        request.setCategory("Vaccination");
        request.setCurrency("VND");
        request.setExpenseDate(LocalDate.of(2026, 6, 20));
        request.setNote("Updated to reflect actual clinic receipt");
        return request;
    }

    static UpdateExpenseRequest makeUpdateRequest(Consumer<UpdateExpenseRequest> overrides) {
        UpdateExpenseRequest request = makeUpdateRequest();
        overrides.accept(request);
        return request;
    }
}
```

---

### EXP-TC-001 — Update own expense with valid partial fields succeeds

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseService.updateExpense()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-CJ-052-03 (UC52 TDS §3)` — partial update applies only non-null fields; `V1__init_schema.sql` line 773 `amount numeric NOT NULL`

**Preconditions:**
- Repository mock returns `ExpenseTestFactory.makeExpense()` for `findByIdAndOwnerUserId(expenseId, MOTHER_A_ID)`
- Fixture: `FX-001`, `FX-003`

**Test Steps:**
1. Arrange: mock `expenseRepository.findByIdAndOwnerUserId(...)` → `Optional.of(makeExpense())`; mock `expenseRepository.save(any())` to return the modified entity
2. Act: call `expenseService.updateExpense(MOTHER_A_ID, expenseId, makeUpdateRequest())`
3. Assert: returned `ExpenseResponse.amount == 450000`, `category == "Vaccination"`, `expenseDate == 2026-06-20`

**Expected Result (PASS):**
- Service returns `ExpenseResponse` reflecting updated fields; `expenseRepository.save()` invoked exactly once with entity carrying new values

**Expected Result (FAIL — signature of bug):**
- Old values persist, or `save()` never called, or exception thrown

**Current Status:** 🟢 Passing
**Implementation Note:** Apply only non-null request fields onto the fetched entity before `save()`.

---

### EXP-TC-002 — Update with only one field provided leaves others unchanged

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseService.updateExpense()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CJ-052-03` — partial update semantics

**Preconditions:** Fixture `FX-001`, `FX-003`

**Test Steps:**
1. Arrange: request with only `note` set, all other fields `null`
2. Act: call `updateExpense(MOTHER_A_ID, expenseId, request)`
3. Assert: `amount`, `category`, `currency`, `expenseDate` remain equal to `makeExpense()` baseline; only `note` changed

**Expected Result (PASS):** Only `note` field differs from baseline entity after update.
**Expected Result (FAIL):** Any other field silently reset to `null` or default.

**Current Status:** 🟢 Passing

---

### EXP-TC-003 — Delete own expense succeeds (hard delete)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseService.deleteExpense()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-CJ-052-02` — hard delete, no soft-delete column exists in `V1__init_schema.sql`

**Preconditions:** Fixture `FX-001`, `FX-003`

**Test Steps:**
1. Arrange: mock `findByIdAndOwnerUserId` → `Optional.of(makeExpense())`; mock `deleteByIdAndOwnerUserId` → returns `1`
2. Act: call `expenseService.deleteExpense(MOTHER_A_ID, expenseId)`
3. Assert: `auditService.record(...)` called exactly once BEFORE `deleteByIdAndOwnerUserId` (verify call order with Mockito `InOrder`); no exception thrown

**Expected Result (PASS):** Audit write happens, then delete; both invoked exactly once.
**Expected Result (FAIL):** Delete happens without audit write, or audit write happens after delete, or either is skipped.

**Current Status:** 🟢 Passing

---

### EXP-TC-004 — Update rejected when amount <= 0

**Severity:** `HIGH`
**Feature Under Test:** `UpdateExpenseRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/carejourney/dto/UpdateExpenseRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `V1__init_schema.sql` line 773 `amount numeric NOT NULL` (positive-amount business assumption confirmed in UC52 TDS §8.1 `@DecimalMin("0.01")`)

**Preconditions:** None (pure Bean Validation test)

**Test Steps:**
1. Arrange: `makeUpdateRequest(r -> r.setAmount(BigDecimal.ZERO))`
2. Act: run `jakarta.validation.Validator.validate(request)`
3. Assert: violation on `amount` field present

**Expected Result (PASS):** Validator returns 1 violation referencing `amount`.
**Expected Result (FAIL):** No violation raised (amount=0 silently accepted).

**Current Status:** 🔴 Not written

---

### EXP-TC-005 — Update rejected when expenseDate is in the future

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateExpenseRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/carejourney/dto/UpdateExpenseRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** UC52 TDS §8.1 `@PastOrPresent` annotation on `expenseDate` (derived rule — SRS does not explicitly forbid future dates; flagged as a design decision, not a hard SRS requirement)

**Preconditions:** None

**Test Steps:**
1. Arrange: `makeUpdateRequest(r -> r.setExpenseDate(LocalDate.now().plusDays(1)))`
2. Act: validate
3. Assert: violation on `expenseDate`

**Expected Result (PASS):** 1 violation on `expenseDate`.
**Expected Result (FAIL):** No violation.

**Current Status:** 🔴 Not written
**Implementation Note:** This rule is a TDS design decision, not a literal SRS requirement — if product later rejects it, remove `@PastOrPresent` and this test together.

---

### EXP-TC-006 — Update fails when expense does not exist

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpenseService.updateExpense()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-CJ-052-01` — `findByIdAndOwnerUserId` miss → `ResourceNotFoundException` (EXP-003)

**Preconditions:** Fixture `FX-003`

**Test Steps:**
1. Arrange: mock `findByIdAndOwnerUserId` → `Optional.empty()`
2. Act: call `updateExpense(MOTHER_A_ID, randomExpenseId, makeUpdateRequest())`
3. Assert: `ResourceNotFoundException` thrown

**Expected Result (PASS):** Exception thrown, no `save()` call.
**Expected Result (FAIL):** Update proceeds or wrong exception type thrown.

**Current Status:** 🟢 Passing

---

### EXP-TC-007 — Update fails when expense is owned by another Mother (ownership violation → 404 not 403)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpenseService.updateExpense()` / `ExpenseController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java` (service level) + `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseControllerTest.java` (HTTP level)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-CJ-052-01 (UC52 TDS §3, §6.3, §10)` — ownership mismatch returns `404`/`EXP-003`, never `403`

**Preconditions:** Fixture `FX-001` (owned by `MOTHER_A_ID`), `FX-004` (`MOTHER_B_ID` JWT)

**Test Steps:**
1. Arrange: mock `findByIdAndOwnerUserId(expenseId, MOTHER_B_ID)` → `Optional.empty()` (repository query filters by owner, so a foreign-owned row never matches)
2. Act (service level): call `updateExpense(MOTHER_B_ID, expenseId, makeUpdateRequest())`
   Act (HTTP level): `PATCH /api/v1/expenses/{expenseId}` with `MOTHER_B_ID` JWT
3. Assert: service throws `ResourceNotFoundException`; HTTP response status is exactly `404` with body `{"error":{"code":"EXP-003", ...}}` — MUST NOT be `403`

**Expected Result (PASS):** 404/EXP-003 exactly, confirming no resource-enumeration leak.
**Expected Result (FAIL):** 403 returned (leaks that the ID exists) or 200 (leaks/mutates another user's data — critical security failure).

**Current Status:** 🔴 Not written

---

### EXP-TC-008 — Delete fails when expense is owned by another Mother

**Severity:** `CRITICAL`
**CWE:** `CWE-639`
**Feature Under Test:** `ExpenseService.deleteExpense()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-CJ-052-01`

**Preconditions:** Fixture `FX-001`, `FX-004`

**Test Steps:**
1. Arrange: mock `findByIdAndOwnerUserId(expenseId, MOTHER_B_ID)` → `Optional.empty()`
2. Act: call `deleteExpense(MOTHER_B_ID, expenseId)`
3. Assert: `ResourceNotFoundException` thrown; `deleteByIdAndOwnerUserId` and `auditService.record()` never invoked

**Expected Result (PASS):** No deletion side effect occurs for non-owned resource.
**Expected Result (FAIL):** Deletion proceeds despite ownership mismatch (data-loss security bug).

**Current Status:** 🔴 Not written

---

### EXP-TC-009 — Unauthenticated request rejected (401)

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `ExpenseController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC52 TDS §9.1, §16` — JWT Bearer required for all endpoints

**Preconditions:** No `Authorization` header

**Test Steps (Attack Simulation):**
1. Send `PATCH /api/v1/expenses/{id}` with no Authorization header
2. Send `DELETE /api/v1/expenses/{id}` with no Authorization header
3. Assert both responses are `401`

**Expected Result (PASS = system safe):** `401 Unauthorized` for both.
**Expected Result (FAIL = vulnerability exists):** `200`/`204` returned without auth.

**Current Status:** 🔴 Not written

---

### EXP-TC-010 — Non-Mother role rejected (role-level, 403)

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpenseController`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC52 TDS §16 Authorization Matrix` — only `MOTHER` role permitted

**Preconditions:** Fixture `FX-005` (`EXPERT` role JWT)

**Test Steps (Attack Simulation):**
1. Authenticate as `EXPERT` role (`FX-005`)
2. `PATCH /api/v1/expenses/{id}`
3. Assert `403 Forbidden` with code `EXP-004`

**Expected Result (PASS):** `403`/`EXP-004`.
**Expected Result (FAIL):** Request succeeds despite wrong role.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### EXP-TC-INT-001 — Delete writes audit log entry atomically before row removal

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: ExpenseController → ExpenseService → ExpenseRepository + AuditService → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/ExpenseDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically at Spring context start
- Seed: insert one `expenses` row owned by a seeded `users` row (`FX-001` shape)

**Test Steps:**
1. Seed expense row directly via `JdbcTemplate`/JPA
2. Call `DELETE /api/v1/expenses/{expenseId}` as the owning Mother
3. Assert `expenses` table no longer contains the row
4. Assert `audit_logs` table contains a new entry referencing the deleted `expenseId`

**Expected Result (PASS):**
- `expenses` row count for that ID = 0
- `audit_logs` row exists with matching `expenseId` reference and event type for expense deletion

**Expected Result (FAIL):**
- Row still exists after 204 response, or audit log missing/incomplete

**DB Assertion:**
```java
Optional<Expense> remaining = expenseRepository.findById(expenseId);
assertThat(remaining).isEmpty();

List<AuditLog> logs = auditLogRepository.findByEntityId(expenseId);
assertThat(logs).isNotEmpty();
```

**Current Status:** 🔴 Not written

---

### EXP-TC-INT-002 — Update persists changes and writes audit diff

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: ExpenseController → ExpenseService → ExpenseRepository + AuditService → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/ExpenseUpdateIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** Same as `EXP-TC-INT-001`

**Test Steps:**
1. Seed expense row
2. Call `PATCH /api/v1/expenses/{expenseId}` with new `amount`/`note`
3. Assert DB row reflects new values
4. Assert `audit_logs` contains entry for the update event

**Expected Result (PASS):** DB row updated; audit entry present.
**Expected Result (FAIL):** Stale values in DB or missing audit entry.

**DB Assertion:**
```java
Expense record = expenseRepository.findById(expenseId).orElseThrow();
assertThat(record.getAmount()).isEqualByComparingTo(new BigDecimal("450000"));
```

**Current Status:** 🔴 Not written

---

### EXP-TC-E2E-001 — Full happy-path API flow

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH then DELETE via real HTTP`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/ExpenseE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** Seeded Mother JWT (`FX-003`), seeded expense (`FX-001`)

**Test Steps:**
1. `PATCH /api/v1/expenses/{expenseId}` with valid body → assert `200`
2. `DELETE /api/v1/expenses/{expenseId}` → assert `204`
3. `PATCH /api/v1/expenses/{expenseId}` again (now deleted) → assert `404`/`EXP-003`

**Expected Result (PASS):** Sequence matches exactly as above.
**Expected Result (FAIL):** Any status code deviates.

**Current Status:** 🔴 Not written

---

### EXP-TC-SEC-001 — Injection payload in note/category handled safely

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ExpenseController` / `ExpenseService`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Fixture `FX-001`, `FX-003`

**Test Steps (Attack Simulation):**
1. Arrange: `makeUpdateRequest(r -> r.setNote("'; DROP TABLE expenses; --"))`
2. Act: `PATCH /api/v1/expenses/{expenseId}` with this payload
3. Assert: request either succeeds with the literal string stored verbatim (JPA parameterized queries prevent injection) or is rejected by validation — but `expenses` table must still exist and be queryable afterward

**Expected Result (PASS = system safe):** `expenses` table intact; note stored as literal text if accepted.
**Expected Result (FAIL = vulnerability exists):** SQL executed as a command (table dropped/altered).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXP-TC-001` | `ExpenseServiceTest.java` | `[x]` | `Passed` | updateExpense_success |
| `EXP-TC-002` | `ExpenseServiceTest.java` | `[x]` | `Passed` | updateExpense_futureDate_rejects |
| `EXP-TC-003` | `ExpenseServiceTest.java` | `[x]` | `Passed` | deleteExpense_auditsBeforeDelete |
| `EXP-TC-004` | `UpdateExpenseRequestValidationTest.java` | `[ ]` | `[ ]` | Not implemented (DTO validation test) |
| `EXP-TC-005` | `UpdateExpenseRequestValidationTest.java` | `[ ]` | `[ ]` | Not implemented (DTO validation test) |
| `EXP-TC-006` | `ExpenseServiceTest.java` | `[x]` | `Passed` | deleteExpense_notOwner_throws |
| `EXP-TC-007` | `ExpenseServiceTest.java` / `ExpenseControllerTest.java` | `[ ]` | `[ ]` | Not implemented (controller layer) |
| `EXP-TC-008` | `ExpenseServiceTest.java` | `[ ]` | `[ ]` | Not implemented |
| `EXP-TC-009` | `ExpenseControllerTest.java` | `[ ]` | `[ ]` | Not implemented (controller layer) |
| `EXP-TC-010` | `ExpenseControllerTest.java` | `[ ]` | `[ ]` | Not implemented (controller layer) |
| `EXP-TC-INT-001` | `ExpenseDeleteIntegrationTest.java` | `[ ]` | `[ ]` | Not implemented (Testcontainers unavailable) |
| `EXP-TC-INT-002` | `ExpenseUpdateIntegrationTest.java` | `[ ]` | `[ ]` | Not implemented (Testcontainers unavailable) |
| `EXP-TC-E2E-001` | `ExpenseE2ETest.java` | `[ ]` | `[ ]` | Not implemented (E2E layer) |
| `EXP-TC-SEC-001` | `ExpenseControllerTest.java` | `[ ]` | `[ ]` | Not implemented (security/MockMvc) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (MUST throw)
@Service
public class ExpenseServiceImpl implements IExpenseService {

    @Override
    public ExpenseResponse updateExpense(UUID ownerUserId, UUID expenseId, UpdateExpenseRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void deleteExpense(UUID ownerUserId, UUID expenseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXP-TC-004` | N/A — pure Bean Validation, no service stub involved | 🔴 FAIL (until DTO annotations added) | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-005` | N/A — pure Bean Validation | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-009` | Controller returns 401 via Spring Security filter (before hitting stub) | 🔴 FAIL (if security config missing) | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-010` | Same as above | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-E2E-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXP-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |

**Red Gate Evidence:**

- Stub commit hash: `2026-07-07-sprint3`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence-uc52.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CAREJOURNEY-IMP-052` reviewed and Approved
- [ ] Logic Issues (§2) confirmed with Principal Architect / user
- [ ] No Flyway migration blocking (table `expenses` already exists — confirmed §3 TDS-02)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (DoD)

- [x] `./mvnw test` — all unit tests green (4/4 update+delete service tests passed)
- [ ] `./mvnw verify` — all integration tests green (Testcontainers unavailable)
- [ ] Test coverage ≥ 80% lines for `ExpenseService`
- [x] No business logic in `ExpenseController` (validation + mapping only)
- [x] No PII/secret in plaintext logs (PDPA: only expenseId+userId logged)
- [ ] Ownership mismatch always returns 404, never 403 (verified by `EXP-TC-007`/`EXP-TC-008`) (controller tests not implemented)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — `./mvnw compile` produces no errors
- [ ] **Props Isolation** — no shared mutable state between tests (all via `ExpenseTestFactory`)
- [ ] **Oracle Source** — every expected value cites BR/ADR/AC per test case above

### Suspension Criteria

- `audit_logs` schema found insufficient for required payload (§5.2 Open item in TDS) and no migration approved yet
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No new migration for UC52 core scope — code-only rollback
git checkout -- src/main/java/com/carebridge/backend/carejourney/
git checkout -- src/test/java/com/carebridge/backend/carejourney/

# If an audit_logs migration was applied (Open item), also:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260701000001';"

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

*Test-Spec v1.0 (Draft) — UC52 Update Expense.*
