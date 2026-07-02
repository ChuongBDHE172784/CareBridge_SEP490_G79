# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC97 — View Revenue and Commission — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-097`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`payment_transactions` L923-939, `commission_records` L941-955, `settlement_records` L1002-1018, `expert_profiles` L786-800)
- `04_Implement/UC97_ViewRevenueAndCommission/UC97_ViewRevenueAndCommission_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16 of it)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.11 — UC-97 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC97 |

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
| **Feature / Gap ID** | `GAP-UC97` |
| **Module** | `Consultation — Expert Revenue & Commission Reporting` |
| **Spec gốc** | `CB-CONSULTATION-IMP-097` |
| **Priority** | 🔴 P0 (financial data isolation), backing feature is 🟠 P1 |
| **Sprint** | `S4 Real Providers And Admin Polish` |
| **Milestone** | Expert Dashboard reporting tabs |
| **Data Classification** | `Internal / Financial` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `payment_transactions`, `commission_records`, `settlement_records`, `expert_profiles` (existing schema, no writer service yet — tests seed directly) |
| **Downstream Consumers** | None (terminal reporting view) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONSULTATION-IMP-097 §17` (ADR-REV-001, ADR-REV-002) |
| **Constraints Injected** | Server-side-only `expertProfileId` resolution; no VNPay call; no dispute endpoint; no new migration without proof; strictly read-only repository usage |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Normal Flow is a generic cross-UC template that never explicitly forbids passing `expertProfileId` as a param | `ADR-REV-001` mandates JWT-only identity resolution to prevent IDOR (CWE-639) | All test cases construct requests WITHOUT any expert-identifier param; a dedicated security test (UC97-TC-SEC-001) asserts an injected/extraneous param is ignored, not honored |
| L2 | SRS Description mentions "reconciliation status" ambiguously | Schema has no `reconciliation` table/column; only `settlement_records.status` and `commission_records.settlement_status` (both free-text, no CHECK constraint) exist | Tests assert `reconciliationStatus` derived value is one of `PENDING`/`SETTLED`/`NOT_YET_SETTLED` per TDS §1.4/ADR-REV-002 mapping, and assert NO write endpoint exists for it |
| L3 | `commission_records.settlement_status` and `settlement_records.status` are both unconstrained `varchar(30)` — no DB-level enum | Application must treat any unexpected string defensively (log + fallback) rather than throw | UC97-TC-012 (boundary) verifies an unrecognized status string does not crash the endpoint, and is surfaced as-is (pass-through) rather than mapped incorrectly |
| L4 | `commission_records.eligible_at` is nullable (`timestamptz`, no NOT NULL) | Period-filtering by `eligible_at` would silently exclude rows with NULL `eligible_at` | TDS §8.2 query filters by `created_at` (NOT NULL) instead of `eligible_at` for period bounds; tests assert rows with NULL `eligible_at` still appear in an unfiltered request |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation.Revenue module bao gồm các layer:
├── Domain (ExpertRevenuePolicy — pure authorization logic, mock repository với Mockito)
├── Services (ExpertRevenueService — mock JPA Repository với Mockito)
├── Controller (ExpertRevenueController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real commission_records/settlement_records/expert_profiles rows)
└── Web Frontend (React Testing Library + Vitest — RevenueCommissionPage, RevenueSummaryCards, RevenueBreakdownTable)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-97 §3.2.1.11` | Happy path display, empty state (AF2), access-denied (E1), external-failure resilience (E3) |
| `ADR-REV-001` | Server-side-only identity resolution; ownership isolation |
| `ADR-REV-002` | Aggregation correctness (summary = sum of breakdown); reconciliation status derivation |
| `BR-RBAC` | Role/verification-status gating |
| `BR-CONSULTATION` | No mutation exposed; auditable read boundary |
| `V1__init_schema.sql` | Exact column names/types/nullability driving fixtures and boundary tests |
| `CB-CONSULTATION-IMP-097 §8/§9/§10` | Service/repository contracts, API shapes, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified Expert with completed sessions requests summary | `ExpertRevenueService.getRevenueSummary()` | `REV-TC-001` |
| TC-COND-002 | Verified Expert with completed sessions requests breakdown | `ExpertRevenueService.getRevenueBreakdown()` | `REV-TC-002` |
| TC-COND-003 | Verified Expert with zero completed sessions (new expert) | Both service methods — empty state | `REV-TC-003` |
| TC-COND-004 | Unverified Expert (`verification_status='PENDING'`) attempts access | `ExpertRevenuePolicy.assertIsVerifiedExpertOwner()` | `REV-TC-004` |
| TC-COND-005 | Non-expert role (MOTHER/FAMILY) attempts access | Controller RBAC guard | `REV-TC-005` |
| TC-COND-006 | Unauthenticated request (no/invalid JWT) | Spring Security filter chain | `REV-TC-006` |
| TC-COND-007 | Two experts each with data — Expert A must never see Expert B's rows | `ICommissionRecordRepository` query scoping | `REV-TC-INT-001`, `REV-TC-SEC-001` |
| TC-COND-008 | Client attempts to pass `expertProfileId`/`expertId` param | Controller DTO binding | `REV-TC-SEC-001` |
| TC-COND-009 | `periodStart` after `periodEnd` (invalid range) | Bean Validation | `REV-TC-007` |
| TC-COND-010 | `page`/`size` out of bounds (negative page, size > 100) | Bean Validation | `REV-TC-008` |
| TC-COND-011 | Summary totals must equal SUM of breakdown page across all pages | Cross-check aggregation | `REV-TC-INT-002` |
| TC-COND-012 | Row with NULL `eligible_at` still included in unfiltered summary | Repository query (L4) | `REV-TC-009` |
| TC-COND-013 | Commission with no linked `settlement_records` row → `reconciliationStatus = NOT_YET_SETTLED` | Service merge logic | `REV-TC-010` |
| TC-COND-014 | Commission linked to a `settlement_records` row → `reconciliationStatus` = that row's `status` | Service merge logic | `REV-TC-011` |
| TC-COND-015 | Unrecognized/unexpected status string does not crash endpoint (boundary) | Service defensive handling | `REV-TC-012` |
| TC-COND-016 | Web: RevenueCommissionPage renders summary + table on successful fetch | React component | `REV-WEB-TC-001` |
| TC-COND-017 | Web: RevenueCommissionPage renders empty state when zero sessions | React component | `REV-WEB-TC-002` |
| TC-COND-018 | Web: RevenueCommissionPage renders error/retry UI on 403/500 | React component | `REV-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role/verification-status combinations (Verified Expert / Unverified Expert / Non-expert / Unauthenticated) | Four distinct authorization outcomes to cover |
| Boundary Value Analysis | `page`/`size` bounds (0, 1, 100, 101); `periodStart == periodEnd`; NULL `eligible_at` | Schema-driven boundaries directly from `V1__init_schema.sql` nullability |
| State Transition Testing | `commission_records.settlement_status` PENDING → settled via linked `settlement_records` | Reconciliation status derivation is state-dependent |
| Error Guessing | IDOR via extraneous param; SQL injection in `periodStart` string param; JWT tampering | Financial data isolation is the highest-risk surface in this UC |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | Expert A: `expert_profiles{verification_status:'VERIFIED'}` + 3 `commission_records` rows (2 settled, 1 pending) | Happy path summary/breakdown |
| `FX-002` | DB seed | Expert B: `expert_profiles{verification_status:'VERIFIED'}` + 2 `commission_records` rows, distinct amounts from FX-001 | Cross-expert isolation proof |
| `FX-003` | DB seed | Expert C: `expert_profiles{verification_status:'PENDING'}`, zero commission rows | Unverified-expert rejection + empty-state combo |
| `FX-004` | DB seed | Expert D: `expert_profiles{verification_status:'VERIFIED'}`, zero commission rows | Pure empty-state (AF2) for a legitimately-allowed caller |
| `FX-005` | JWT | `{ sub: Expert-A-user-id, role: 'EXPERT' }` | Auth context for Expert A requests |
| `FX-006` | JWT | `{ sub: Expert-B-user-id, role: 'EXPERT' }` | Auth context for Expert B requests |
| `FX-007` | JWT | `{ sub: Mother-user-id, role: 'MOTHER' }` | Non-expert role rejection |
| `FX-008` | DB seed | `commission_records` row with `eligible_at = NULL`, `created_at` set | Boundary L4 verification |
| `FX-009` | DB seed | `commission_records` row with `settlement_status = 'UNKNOWN_LEGACY_VALUE'` (simulating unexpected data) | Boundary TC-COND-015 |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// RevenueTestFactory.java — mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class RevenueTestFactory {

    static ExpertProfileEntity makeVerifiedExpert() {
        ExpertProfileEntity e = new ExpertProfileEntity();
        e.setExpertProfileId(UUID.randomUUID());
        e.setUserId(UUID.randomUUID());
        e.setVerificationStatus("VERIFIED");
        return e;
    }

    static ExpertProfileEntity makeVerifiedExpert(Consumer<ExpertProfileEntity> overrides) {
        ExpertProfileEntity e = makeVerifiedExpert();
        overrides.accept(e);
        return e;
    }

    static CommissionRecordEntity makeCommission(UUID expertProfileId, Consumer<CommissionRecordEntity> overrides) {
        CommissionRecordEntity c = new CommissionRecordEntity();
        c.setCommissionId(UUID.randomUUID());
        c.setPaymentId(UUID.randomUUID());
        c.setExpertProfileId(expertProfileId);
        c.setOriginalPrice(new BigDecimal("500000"));
        c.setCommissionRate(new BigDecimal("0.15"));
        c.setCommissionAmount(new BigDecimal("75000"));
        c.setGatewayFee(BigDecimal.ZERO);
        c.setRefundAmount(BigDecimal.ZERO);
        c.setExpertNetAmount(new BigDecimal("425000"));
        c.setEligibleAt(Instant.now());
        c.setSettlementStatus("PENDING");
        c.setCreatedAt(Instant.now());
        overrides.accept(c);
        return c;
    }

    static SettlementRecordEntity makeSettlement(UUID commissionId, UUID expertProfileId) {
        SettlementRecordEntity s = new SettlementRecordEntity();
        s.setSettlementId(UUID.randomUUID());
        s.setCommissionId(commissionId);
        s.setExpertProfileId(expertProfileId);
        s.setStatus("SETTLED");
        s.setSettledAt(Instant.now());
        s.setReferenceCode("STL-TEST-0001");
        return s;
    }
}
```

---

### REV-TC-001 — Verified Expert with completed sessions gets correct revenue summary

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRevenueService.getRevenueSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.2.1.11 Normal Flow Step 5` / `ADR-REV-002 Option B`

**Preconditions:** FX-001 (Expert A, 3 commission rows: 2 amount=75000, 1 amount=50000).

**Test Steps:**
1. Mock `ICommissionRecordRepository.sumByExpertProfileId()` to return aggregate `{count:3, gross:1500000, commission:200000, net:1300000}`.
2. Call `expertRevenueService.getRevenueSummary(expertAUserId, emptyQuery)`.
3. Assert returned `RevenueSummaryResponse` fields match mocked aggregate exactly.

**Expected Result (PASS):** `totalCompletedSessions=3`, `totalPlatformCommission=200000`, `totalExpertNetRevenue=1300000`.
**Expected Result (FAIL):** Any field mismatched, or NPE, or wrong expert scoped.

**Current Status:** 🔴 Not written
**Implementation Note:** Service must not recompute sums itself — trust the repository aggregate query result verbatim (single source of truth per ADR-REV-002).

---

### REV-TC-002 — Breakdown returns paginated per-session rows with correct reconciliation status

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRevenueService.getRevenueBreakdown()`
**Test File:** `ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.1 RevenueBreakdownItemResponse`, `ADR-REV-002`

**Preconditions:** FX-001, one commission linked to a `settlement_records` row (FX-005-equivalent).

**Test Steps:**
1. Mock repository page of 3 `CommissionRecordEntity` + mock `findByCommissionIdIn()` returning 1 matching `SettlementRecordEntity`.
2. Call `getRevenueBreakdown(expertAUserId, query)`.
3. Assert the settled row has `reconciliationStatus="SETTLED"` and `settlementReferenceCode` populated; the other two have `reconciliationStatus="NOT_YET_SETTLED"` and null reference code.

**Expected Result (PASS):** Exactly 1 of 3 rows shows `SETTLED`, others `NOT_YET_SETTLED`.
**Expected Result (FAIL):** All rows show same status, or reference code leaks onto unsettled rows.

**Current Status:** 🔴 Not written

---

### REV-TC-003 — Verified Expert with zero completed sessions gets zeroed empty-state summary, not an error

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRevenueService.getRevenueSummary()`
**Test File:** `ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `SRS §3.2.1.11 AF2 — empty state with next allowed action`

**Preconditions:** FX-004 (Expert D, zero commission rows).

**Test Steps:**
1. Mock repository aggregate query returning NULL sums / 0 count.
2. Call `getRevenueSummary(expertDUserId, emptyQuery)`.
3. Assert response has `totalCompletedSessions=0` and all monetary fields `= BigDecimal.ZERO` (not null, not an exception).

**Expected Result (PASS):** Zeroed DTO, HTTP-level 200 (verified at controller integration level too).
**Expected Result (FAIL):** NullPointerException on SUM coalescing, or 404/500 thrown for a legitimately empty but valid expert.

**Current Status:** 🔴 Not written
**Implementation Note:** Repository JPQL uses `COALESCE(SUM(...), 0)` per TDS §8.2 — this test guards against removing that COALESCE.

---

### REV-TC-004 — Unverified Expert is rejected with REV-004 before any data query

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRevenuePolicy.assertIsVerifiedExpertOwner()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/ExpertRevenuePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REV-001`, `BR-RBAC`

**Preconditions:** FX-003 (Expert C, `verification_status='PENDING'`).

**Test Steps:**
1. Mock `IExpertProfileRepository.findByUserId()` to return Expert C's profile with `PENDING` status.
2. Call `assertIsVerifiedExpertOwner(expertCUserId)`.
3. Assert `ForbiddenAccessException` with code `REV-004` is thrown.
4. Verify `ICommissionRecordRepository` was NEVER invoked (no leaked query before authorization).

**Expected Result (PASS):** Exception thrown before any commission/settlement repository call.
**Expected Result (FAIL):** Data query executes despite unverified status, or wrong/no exception thrown.

**Current Status:** 🔴 Not written

---

### REV-TC-005 — Non-expert role (MOTHER) is rejected at controller/security layer

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRevenueController` (`@WebMvcTest` with mocked `IExpertRevenueService`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/ExpertRevenueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC`, `TDS §16 Authorization Matrix`

**Preconditions:** FX-007 JWT (role=MOTHER).

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/experts/me/revenue/summary").with(jwt of FX-007))`.
2. Assert HTTP 403.
3. Assert `IExpertRevenueService` was never invoked (Mockito `verifyNoInteractions`).

**Expected Result (PASS):** 403, service untouched.
**Expected Result (FAIL):** 200 returned, or service invoked despite wrong role.

**Current Status:** 🔴 Not written

---

### REV-TC-006 — Unauthenticated request is rejected with 401 before reaching controller logic

**Severity:** `CRITICAL`
**Feature Under Test:** Spring Security filter chain for `/api/v1/experts/me/revenue/**`
**Test File:** `ExpertRevenueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §10 Error Codes — REV-003`

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/experts/me/revenue/summary"))` with no `Authorization` header.
2. Assert HTTP 401 and body `error.code == "REV-003"`.

**Expected Result (PASS):** 401 REV-003.
**Expected Result (FAIL):** Any other status, or NPE from missing principal.

**Current Status:** 🔴 Not written

---

### REV-TC-007 — periodStart after periodEnd is rejected as 400 validation error

**Severity:** `MEDIUM`
**Feature Under Test:** `RevenueQueryRequest` Bean Validation / controller
**Test File:** `ExpertRevenueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §10 — REV-001`

**Test Steps:**
1. Request with `periodStart=2026-07-01&periodEnd=2026-01-01`.
2. Assert HTTP 400, `error.code=="REV-001"`, `details[0].field=="periodStart"`.

**Expected Result (PASS):** 400 with field-level detail.
**Expected Result (FAIL):** 200 returned with a nonsensical/empty range silently accepted.

**Current Status:** 🔴 Not written

---

### REV-TC-008 — page/size out of bounds rejected as 400

**Severity:** `LOW`
**Feature Under Test:** `RevenueQueryRequest` Bean Validation
**Test File:** `ExpertRevenueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §4.1 NFR — pagination bounds`, `TDS §8.1 @Min/@Max`

**Test Steps:**
1. Request `size=101` → assert 400 REV-001.
2. Request `page=-1` → assert 400 REV-001.
3. Request `size=100` (boundary, allowed max) → assert 200.

**Expected Result (PASS):** 101/−1 rejected; 100 accepted (boundary inclusive per `@Max(100)`).
**Expected Result (FAIL):** 101 silently clamped instead of rejected, or 100 incorrectly rejected.

**Current Status:** 🔴 Not written

---

### REV-TC-009 — Commission row with NULL eligible_at is still included in unfiltered summary (schema boundary L4)

**Severity:** `MEDIUM`
**Feature Under Test:** `ICommissionRecordRepository.sumByExpertProfileId()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/CommissionRecordRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `V1__init_schema.sql` L951 (`eligible_at timestamptz` nullable) + Logic Issue L4

**Preconditions:** FX-008 seeded via Testcontainers (row with `eligible_at IS NULL`, `created_at` set).

**Test Steps:**
1. Seed FX-008 plus one normal FX-001-style row for the same expert.
2. Call `sumByExpertProfileId(expertId, null, null)` (no period filter).
3. Assert `count == 2` (both rows counted, including the NULL-`eligible_at` one).

**Expected Result (PASS):** NULL `eligible_at` row is not silently dropped.
**Expected Result (FAIL):** `count == 1` — proves the query incorrectly filters by `eligible_at` instead of `created_at`.

**Current Status:** 🔴 Not written

---

### REV-TC-010 — Commission with no linked settlement row derives reconciliationStatus = NOT_YET_SETTLED

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRevenueService` merge logic
**Test File:** `ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-REV-002` decision block

**Test Steps:**
1. Mock breakdown page with one commission row, `settlementRecordRepository.findByCommissionIdIn()` returns empty list.
2. Assert resulting DTO `reconciliationStatus == "NOT_YET_SETTLED"`.

**Expected Result (PASS):** Derived constant applied.
**Expected Result (FAIL):** Null/blank status, or raw `commission_records.settlement_status` ("PENDING") leaked unmapped, contradicting the documented derivation rule.

**Current Status:** 🔴 Not written

---

### REV-TC-011 — Commission with linked settlement row surfaces that row's status verbatim

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRevenueService` merge logic
**Test File:** `ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-REV-002`

**Test Steps:**
1. Mock one commission + one matching `SettlementRecordEntity{status:"SETTLED", referenceCode:"STL-TEST-0001"}`.
2. Assert DTO `reconciliationStatus=="SETTLED"` and `settlementReferenceCode=="STL-TEST-0001"`.

**Expected Result (PASS):** Values pass through unmodified.
**Expected Result (FAIL):** Status recomputed/overwritten incorrectly.

**Current Status:** 🔴 Not written

---

### REV-TC-012 — Unrecognized settlement_status string does not crash the endpoint (defensive boundary)

**Severity:** `LOW`
**Feature Under Test:** `ExpertRevenueService` merge logic
**Test File:** `ExpertRevenueServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** Logic Issue L3 (unconstrained `varchar(30)`, no CHECK constraint)

**Preconditions:** FX-009 (`settlement_status='UNKNOWN_LEGACY_VALUE'`).

**Test Steps:**
1. Feed a commission row with the unexpected status value, no linked settlement.
2. Call service merge logic.
3. Assert no exception thrown; `reconciliationStatus` falls back to `NOT_YET_SETTLED` (since no settlement row exists) — the raw unexpected `settlement_status` value on the commission itself is not exposed as `reconciliationStatus` per ADR-REV-002 (only settlement-row-derived states are surfaced).

**Expected Result (PASS):** No crash, deterministic fallback value.
**Expected Result (FAIL):** `IllegalArgumentException`/enum-mapping crash, or 500 at controller level.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### REV-TC-SEC-001 — Extraneous expertProfileId query param is ignored; caller always receives only their own data

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Legal:** `BR-RBAC`, financial-data isolation requirement (TDS RG-4)
**Feature Under Test:** `ExpertRevenueController` DTO binding + full request pipeline
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/ExpertRevenueSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-001 (Expert A, JWT FX-005) and FX-002 (Expert B) both seeded via Testcontainers.

**Test Steps (Attack Simulation):**
1. Authenticate as Expert A (FX-005 JWT).
2. Call `GET /api/v1/experts/me/revenue/breakdown?expertProfileId=<Expert-B-UUID>&expertId=<Expert-B-UUID>`.
3. Inspect response `content[]` — assert every row's underlying `commissionId` belongs to Expert A's seeded set (FX-001), never Expert B's (FX-002).

**Expected Result (PASS = hệ thống an toàn):** 200 OK containing ONLY Expert A's rows; extraneous params silently ignored (no binding field exists for them per TDS §8.1 DTO).
**Expected Result (FAIL = lỗ hổng tồn tại):** Any Expert B row appears in the response, or the endpoint 500s trying to bind the unknown param.

**Current Status:** 🔴 Not written

---

### REV-TC-SEC-002 — Cross-expert repository query never returns another expert's row even with correct own-scoped call

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** Financial-data isolation (TDS RG-4)
**Feature Under Test:** `ICommissionRecordRepository.findByExpertProfileIdOrderByCreatedAtDesc()`
**Test File:** `CommissionRecordRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-001, FX-002 both seeded (different `expert_profile_id`).

**Test Steps (Attack Simulation):**
1. Query repository directly with Expert A's `expertProfileId`.
2. Assert result set size == FX-001's row count exactly (3), and every row's `expertProfileId == Expert A's ID`.
3. Repeat with Expert B's ID, assert disjoint result set.

**Expected Result (PASS = hệ thống an toàn):** Fully disjoint result sets; zero overlap.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any row appears in both result sets, or Expert A's query returns Expert B's rows (e.g., missing WHERE clause).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### REV-TC-INT-001 — Full authenticated flow: seeded DB -> API -> isolated per-expert response

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Policy -> Service -> Repository -> PostgreSQL (Testcontainers)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/revenue/ExpertRevenueControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied automatically.
- Seed FX-001 (Expert A) and FX-002 (Expert B) via `JdbcTemplate`/JPA before each test (fresh instance per Props Isolation rule).

**Test Steps:**
1. Authenticate as Expert A (FX-005).
2. `GET /api/v1/experts/me/revenue/summary` → assert totals match FX-001 seed exactly.
3. `GET /api/v1/experts/me/revenue/breakdown` → assert `totalElements == 3`, all rows belong to Expert A.
4. Re-authenticate as Expert B (FX-006), repeat — assert totals/rows match FX-002, fully independent of step 2-3 results.

**Expected Result (PASS):**
- Expert A sees exactly their 3 rows/summed totals.
- Expert B sees exactly their 2 rows/summed totals.
- No cross-contamination between the two calls.

**Expected Result (FAIL):** Any total or row count mismatched, or cross-expert leakage.

**DB Assertion:**
```java
List<CommissionRecordEntity> expertARows = commissionRecordRepository.findByExpertProfileIdOrderByCreatedAtDesc(expertAId, Pageable.unpaged()).getContent();
assertThat(expertARows).hasSize(3);
assertThat(expertARows).allMatch(r -> r.getExpertProfileId().equals(expertAId));
```

**Current Status:** 🔴 Not written

---

### REV-TC-INT-002 — Summary totals equal SUM of every page of the breakdown for the same expert/filter

**Severity:** `HIGH`
**Feature Under Test:** `Cross-check: ExpertRevenueService.getRevenueSummary() vs. getRevenueBreakdown() aggregation consistency`
**Test File:** `ExpertRevenueControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:** FX-001 seeded with 3 rows.

**Test Steps:**
1. Fetch summary — record `totalPlatformCommission`.
2. Fetch breakdown with `size=100` (all rows on one page given small fixture size).
3. Manually sum `commissionAmount` across all `content[]` items.
4. Assert manual sum == summary's `totalPlatformCommission`.

**Expected Result (PASS):** Exact equality (financial correctness, ADR-REV-002).
**Expected Result (FAIL):** Any drift between the two independently-computed totals.

**Current Status:** 🔴 Not written

---

### REV-WEB-TC-001 — RevenueCommissionPage renders summary cards and breakdown table on successful fetch

**Severity:** `MEDIUM`
**Feature Under Test:** `RevenueCommissionPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expertRevenue/pages/RevenueCommissionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** MSW (or manual `vi.fn()` mock) intercepts `GET /api/v1/experts/me/revenue/summary` and `.../breakdown` returning happy-path fixtures matching TDS §9.2.

**Test Steps:**
1. Render `<RevenueCommissionPage />` wrapped in a `QueryClientProvider` test harness.
2. `await screen.findByText(...)` for the total revenue figure.
3. Assert breakdown table renders one row per fixture item.

**Expected Result (PASS):** Summary cards + table populated from fetched data.
**Expected Result (FAIL):** Loading spinner stuck, or wrong figures rendered.

**Current Status:** 🔴 Not written

---

### REV-WEB-TC-002 — RevenueCommissionPage renders empty state when API returns zero sessions

**Severity:** `LOW`
**Feature Under Test:** `RevenueCommissionPage.tsx`
**Test File:** `RevenueCommissionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Mock API returning zeroed summary + empty breakdown array.
2. Render component.
3. Assert an "empty state" message is shown (e.g., "No completed sessions yet"), not a blank/broken table.

**Expected Result (PASS):** Explicit empty-state UI per SRS AF2.
**Expected Result (FAIL):** Blank screen, or crash on empty array `.map()`.

**Current Status:** 🔴 Not written

---

### REV-WEB-TC-003 — RevenueCommissionPage renders access-denied/error UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `RevenueCommissionPage.tsx`
**Test File:** `RevenueCommissionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Test Steps:**
1. Mock API returning 403 `REV-004` error body.
2. Render component.
3. Assert an error/access-denied message is shown, not a crash or infinite spinner.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection / white screen.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REV-TC-001` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-002` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-003` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-004` | `ExpertRevenuePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-005` | `ExpertRevenueControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-006` | `ExpertRevenueControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-007` | `ExpertRevenueControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-008` | `ExpertRevenueControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-009` | `CommissionRecordRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-010` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-011` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-012` | `ExpertRevenueServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-SEC-001` | `ExpertRevenueSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-SEC-002` | `CommissionRecordRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-INT-001` | `ExpertRevenueControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-TC-INT-002` | `ExpertRevenueControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `REV-WEB-TC-001` | `RevenueCommissionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `REV-WEB-TC-002` | `RevenueCommissionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `REV-WEB-TC-003` | `RevenueCommissionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertRevenueService implements IExpertRevenueService {

    @Override
    public RevenueSummaryResponse getRevenueSummary(UUID callerUserId, RevenueQueryRequest query) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Page<RevenueBreakdownItemResponse> getRevenueBreakdown(UUID callerUserId, RevenueQueryRequest query) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ExpertRevenuePolicy {
    public ExpertProfileEntity assertIsVerifiedExpertOwner(UUID callerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REV-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REV-TC-004` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REV-TC-SEC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REV-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CONSULTATION-IMP-097` approved (currently Draft, ADR-REV-001/002 Proposed)
- [ ] Logic Issues (§2, L1-L4) confirmed with Tech Lead
- [ ] No migration required (§5.2 of TDS) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green (note: `vitest`/`@testing-library/react` are NOT yet in `package.json` — must be added as a dev-dependency and a `test`/`test:run` script added to `05_Development/CareBridgeWebApp/package.json` before REV-WEB-TC-* can execute; flagged as an infrastructure prerequisite, see §6 Suspension Criteria)
- [ ] Test coverage ≥ 80% lines for `ExpertRevenueService`, `ExpertRevenuePolicy`
- [ ] No business logic in `ExpertRevenueController` (validation + mapping only)
- [ ] No financial figures/PII in logs at INFO or above
- [ ] `REV-TC-SEC-001` and `REV-TC-SEC-002` both green — mandatory gate, not optional, given financial-isolation risk

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `RevenueTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet installed — REV-WEB-TC-* suspended until `package.json`/`vite.config.ts` are updated (separate small PR, not part of this feature's core scope per CLAUDE.md "smallest scoped change")
- Upstream payment/commission-writer services (3.1.2.1/3.1.2.2) remain unimplemented — does not block this UC's own tests (all seed data directly), but blocks realistic end-to-end demo data

---

## 7. Rollback Plan

```bash
# No migration to revert — UC97 introduces no schema change.

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/revenue/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/revenue/
git checkout -- 05_Development/CareBridgeWebApp/src/features/expertRevenue/

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — ADR-REV-001/002 cover all decisions | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — REV-TC-005/006/007/008 test controller only for validation/RBAC, business logic asserted only in Service tests | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — all types match TDS §8 interfaces exactly | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
