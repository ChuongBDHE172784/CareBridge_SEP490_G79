# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC98 — View Contribution Points — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-098`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`contribution_points` L241-249, `expert_profiles` L786-800)
- `04_Implement/UC98_ViewContributionPoints/UC98_ViewContributionPoints_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16 of it)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.12 — UC-98 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC98 |

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
| **Feature / Gap ID** | `GAP-UC98` |
| **Module** | `Consultation — Expert Contribution Points & Badges` |
| **Spec gốc** | `CB-CONSULTATION-IMP-098` |
| **Priority** | 🟠 P1 (gamification/reporting, no financial risk) |
| **Sprint** | `S4 Real Providers And Admin Polish` |
| **Milestone** | Expert Dashboard reporting tabs |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `contribution_points` (existing schema, no writer service yet — tests seed directly) |
| **Downstream Consumers** | None (terminal reporting view) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONSULTATION-IMP-098 §17` (ADR-CTB-001, ADR-CTB-002, ADR-CTB-003) |
| **Constraints Injected** | Server-side-only `userId` resolution; no badges table; placeholder thresholds must stay commented; no UC92 contract assumptions; strictly read-only repository usage; boxed types for nullable columns |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Description mentions "badges" with no defined tiers/thresholds | No `badges` table exists; TDS ADR-CTB-002 defines PLACEHOLDER thresholds (Bronze=50, Silver=200, Gold=500) | Tests assert badge tier boundaries at exactly these placeholder values AND assert the response/UI never claims these are final without a "subject to change" framing; a future threshold change must only require editing `BadgeThresholdPolicyTest`'s expected constants, not rewriting test structure |
| L2 | `contribution_points.points`, `user_id`, `source_id`, `reason`, `source_type`, `recorded_at` are ALL nullable (no `NOT NULL` in schema) | A row with `points IS NULL` must not NPE the SUM aggregation | UC98-TC-009 (boundary) seeds a row with `points = NULL` and asserts the summary endpoint still returns a valid (non-crashing) total, treating NULL as 0 in the SUM |
| L3 | No FK constraint exists on `contribution_points.user_id` — a malformed/orphaned row (user_id referencing a deleted user) is schema-legal | Query must still function defensively; cross-expert isolation must not rely on FK integrity, only on the WHERE clause | UC98-TC-SEC-002 explicitly seeds ledger rows for two independent `user_id` values with no assumption of referential integrity, proving isolation is enforced by query filter alone |
| L4 | SRS gives no explicit maximum badge tier / "already at top" behavior | `resolveNextTier()` must handle the case where total points already exceed `GOLD_MIN` | UC98-TC-006 asserts `nextBadgeTier=null` and `pointsToNextTier=null` when `totalPoints >= GOLD_MIN` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation.Contribution module bao gồm các layer:
├── Domain (BadgeThresholdPolicy — pure function, no deps, no mock needed)
├── Domain (ExpertContributionPolicy — authorization logic, mock repository với Mockito)
├── Services (ExpertContributionService — mock JPA Repository với Mockito)
├── Controller (ExpertContributionController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real contribution_points/expert_profiles rows)
└── Web Frontend (React Testing Library + Vitest — ContributionPointsPage, BadgeProgressIndicator)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-98 §3.2.1.12` | Happy path display, empty state (AF2), access-denied (E1) |
| `ADR-CTB-001` | Server-side-only identity resolution; ownership isolation |
| `ADR-CTB-002` | Badge tier boundary correctness at placeholder thresholds |
| `ADR-CTB-003` | No hallucinated UC92 event/method dependency in tests |
| `BR-RBAC` | Role/verification-status gating |
| `V1__init_schema.sql` | Nullable-column boundary tests |
| `CB-CONSULTATION-IMP-098 §8/§9/§10` | Service/repository contracts, API shapes, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified Expert with points earned requests summary | `ExpertContributionService.getContributionSummary()` | `CTB-TC-001` |
| TC-COND-002 | Verified Expert requests paginated activity ledger | `ExpertContributionService.getContributionActivity()` | `CTB-TC-002` |
| TC-COND-003 | Verified Expert with zero points (new expert) | Both service methods — empty state | `CTB-TC-003` |
| TC-COND-004 | Unverified Expert attempts access | `ExpertContributionPolicy.assertIsVerifiedExpertOwner()` | `CTB-TC-004` |
| TC-COND-005 | Non-expert role (MOTHER/FAMILY) attempts access | Controller RBAC guard | `CTB-TC-005` |
| TC-COND-006 | Unauthenticated request | Spring Security filter chain | `CTB-TC-006` |
| TC-COND-007 | Badge tier boundary: exactly at BRONZE_MIN (50) | `BadgeThresholdPolicy.resolveTier()` | `CTB-TC-007` |
| TC-COND-008 | Badge tier boundary: 1 point below BRONZE_MIN (49) | `BadgeThresholdPolicy.resolveTier()` | `CTB-TC-007` |
| TC-COND-009 | Badge tier boundary: exactly at SILVER_MIN (200), GOLD_MIN (500) | `BadgeThresholdPolicy.resolveTier()` | `CTB-TC-008` |
| TC-COND-010 | Already at max tier (GOLD, points > 500) — no next tier | `BadgeThresholdPolicy.resolveNextTier()` | `CTB-TC-006` |
| TC-COND-011 | Two experts each with data — Expert A must never see Expert B's ledger | `IContributionPointRepository` query scoping | `CTB-TC-INT-001`, `CTB-TC-SEC-001` |
| TC-COND-012 | Client attempts to pass `userId`/`expertId` param | Controller DTO binding | `CTB-TC-SEC-001` |
| TC-COND-013 | `page`/`size` out of bounds | Bean Validation | `CTB-TC-009` |
| TC-COND-014 | Row with `points = NULL` does not crash SUM aggregation | Repository query (L2) | `CTB-TC-010` |
| TC-COND-015 | Summary total equals SUM of every page of activity for the same expert | Cross-check aggregation | `CTB-TC-INT-002` |
| TC-COND-016 | Web: ContributionPointsPage renders summary + badge + table on successful fetch | React component | `CTB-WEB-TC-001` |
| TC-COND-017 | Web: ContributionPointsPage renders empty state when zero points | React component | `CTB-WEB-TC-002` |
| TC-COND-018 | Web: ContributionPointsPage renders error UI on 403/500 | React component | `CTB-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role/verification-status combinations | Four distinct authorization outcomes |
| Boundary Value Analysis | Badge tier thresholds (49/50, 199/200, 499/500, 500+); `page`/`size` bounds; NULL `points` | Directly exercises the placeholder threshold constants and nullable schema columns |
| State Transition Testing | N/A — no stateful workflow in this UC (pure read + computed tier) | Confirms UC98 has no mutation path, reinforcing read-only NFR |
| Error Guessing | IDOR via extraneous param; orphaned `user_id` row with no FK; unrecognized `source_type` string | Financial-adjacent isolation risk still applies even though data is non-monetary |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-101` | DB seed | Expert A: `expert_profiles{verification_status:'VERIFIED'}` + 5 `contribution_points` rows summing to 265 points | Happy path summary/breakdown, SILVER tier |
| `FX-102` | DB seed | Expert B: `expert_profiles{verification_status:'VERIFIED'}` + 2 `contribution_points` rows summing to 30 points, distinct `user_id` from FX-101 | Cross-expert isolation proof |
| `FX-103` | DB seed | Expert C: `expert_profiles{verification_status:'PENDING'}`, zero contribution rows | Unverified-expert rejection |
| `FX-104` | DB seed | Expert D: `expert_profiles{verification_status:'VERIFIED'}`, zero contribution rows | Pure empty-state (AF2) for legitimately-allowed caller |
| `FX-105` | JWT | `{ sub: Expert-A-user-id, role: 'EXPERT' }` | Auth context for Expert A |
| `FX-106` | JWT | `{ sub: Expert-B-user-id, role: 'EXPERT' }` | Auth context for Expert B |
| `FX-107` | JWT | `{ sub: Mother-user-id, role: 'MOTHER' }` | Non-expert role rejection |
| `FX-108` | DB seed | `contribution_points` row with `points = NULL` for Expert A | Boundary L2 verification |
| `FX-109` | DB seed | Expert E: exactly 50 total points (BRONZE_MIN boundary) | Badge boundary exact-match test |
| `FX-110` | DB seed | Expert F: exactly 49 total points (BRONZE_MIN - 1) | Badge boundary just-below test |
| `FX-111` | DB seed | Expert G: exactly 500 total points (GOLD_MIN boundary, max tier) | Badge max-tier / no-next-tier test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ContributionTestFactory.java — mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class ContributionTestFactory {

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

    static ContributionPointEntity makePointRecord(UUID userId, Integer points, Consumer<ContributionPointEntity> overrides) {
        ContributionPointEntity c = new ContributionPointEntity();
        c.setPointRecordId(UUID.randomUUID());
        c.setUserId(userId);
        c.setPoints(points);
        c.setReason("Answer marked helpful");
        c.setSourceType("ANSWER");
        c.setSourceId(UUID.randomUUID());
        c.setRecordedAt(Instant.now());
        overrides.accept(c);
        return c;
    }
}
```

---

### CTB-TC-001 — Verified Expert with points earned gets correct contribution summary

**Severity:** `HIGH`
**Feature Under Test:** `ExpertContributionService.getContributionSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ExpertContributionServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.2.1.12 Normal Flow Step 5`, `ADR-CTB-001`

**Preconditions:** FX-101 (Expert A, sum = 265).

**Test Steps:**
1. Mock `IContributionPointRepository.sumPointsByUserId()` to return `265`.
2. Mock `BadgeThresholdPolicy.resolveTier(265)` → `SILVER`; `resolveNextTier(265)` → `GOLD` (min 500).
3. Call `getContributionSummary(expertAUserId)`.
4. Assert `totalPoints=265`, `currentBadgeTier="SILVER"`, `nextBadgeTier="GOLD"`, `pointsToNextTier=235`.

**Expected Result (PASS):** All fields match.
**Expected Result (FAIL):** Wrong tier, wrong points-to-next-tier math, or NPE.

**Current Status:** 🔴 Not written

---

### CTB-TC-002 — Activity ledger returns paginated per-event rows in descending recency order

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertContributionService.getContributionActivity()`
**Test File:** `ExpertContributionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §8.1 ContributionActivityItemResponse`

**Preconditions:** FX-101, 5 point records with distinct `recordedAt` timestamps.

**Test Steps:**
1. Mock repository page returning 5 `ContributionPointEntity` sorted by `recordedAt DESC`.
2. Call `getContributionActivity(expertAUserId, query)`.
3. Assert returned DTO list preserves the same order and field values (points, reason, sourceType, recordedAt).

**Expected Result (PASS):** Order and field mapping preserved.
**Expected Result (FAIL):** Order scrambled or a field dropped/mismapped.

**Current Status:** 🔴 Not written

---

### CTB-TC-003 — Verified Expert with zero points gets zeroed empty-state summary, not an error

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertContributionService.getContributionSummary()`
**Test File:** `ExpertContributionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `SRS §3.2.1.12 AF2`

**Preconditions:** FX-104 (Expert D, zero rows).

**Test Steps:**
1. Mock `sumPointsByUserId()` returning `0`.
2. Call `getContributionSummary(expertDUserId)`.
3. Assert `totalPoints=0`, `currentBadgeTier="UNRANKED"`, `nextBadgeTier="BRONZE"`, `pointsToNextTier=50`.

**Expected Result (PASS):** Zeroed but valid DTO, no exception.
**Expected Result (FAIL):** NPE, or a 404/500 for a legitimately empty but valid expert.

**Current Status:** 🔴 Not written

---

### CTB-TC-004 — Unverified Expert is rejected with CTB-004 before any data query

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertContributionPolicy.assertIsVerifiedExpertOwner()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ExpertContributionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-CTB-001`, `BR-RBAC`

**Preconditions:** FX-103 (Expert C, `PENDING`).

**Test Steps:**
1. Mock `IExpertProfileRepository.findByUserId()` returning Expert C's `PENDING` profile.
2. Call `assertIsVerifiedExpertOwner(expertCUserId)`.
3. Assert `ForbiddenAccessException` code `CTB-004`.
4. Verify `IContributionPointRepository` NEVER invoked.

**Expected Result (PASS):** Exception before any ledger query.
**Expected Result (FAIL):** Query executes despite unverified status.

**Current Status:** 🔴 Not written

---

### CTB-TC-005 — Non-expert role (MOTHER) is rejected at controller/security layer

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertContributionController` (`@WebMvcTest`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ExpertContributionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC`, `TDS §16`

**Preconditions:** FX-107 JWT (role=MOTHER).

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/experts/me/contribution-points/summary").with(jwt of FX-107))`.
2. Assert HTTP 403.
3. Assert `IExpertContributionService` never invoked.

**Expected Result (PASS):** 403, service untouched.
**Expected Result (FAIL):** 200 returned or service invoked.

**Current Status:** 🔴 Not written

---

### CTB-TC-006 — Badge tier at maximum (GOLD): no next tier, pointsToNextTier is null

**Severity:** `MEDIUM`
**Feature Under Test:** `BadgeThresholdPolicy.resolveNextTier()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/BadgeThresholdPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-CTB-002`, Logic Issue L4

**Preconditions:** FX-111 (exactly 500 points) and a value above (e.g. 620).

**Test Steps:**
1. Call `resolveTier(500)` → assert `"GOLD"`.
2. Call `resolveNextTier(500)` → assert `Optional.empty()`.
3. Call `resolveTier(620)` → assert `"GOLD"` (still max tier, no tier above Gold exists).
4. In `ExpertContributionService`, assert the resulting `ContributionSummaryResponse` has `nextBadgeTier=null` and `pointsToNextTier=null` for a 500+-point expert.

**Expected Result (PASS):** No next tier beyond GOLD; nulls, not an exception or a bogus tier name.
**Expected Result (FAIL):** `NoSuchElementException` from unchecked `Optional.get()`, or a fabricated "PLATINUM" tier not defined anywhere.

**Current Status:** 🔴 Not written

---

### CTB-TC-007 — Badge tier boundary: exactly at BRONZE_MIN (50) vs. one below (49)

**Severity:** `MEDIUM`
**Feature Under Test:** `BadgeThresholdPolicy.resolveTier()`
**Test File:** `BadgeThresholdPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`, `TC-COND-008`
**Oracle Source:** `ADR-CTB-002` (`BRONZE_MIN = 50`, inclusive lower bound `>=`)

**Test Steps:**
1. `resolveTier(50)` → assert `"BRONZE"`.
2. `resolveTier(49)` → assert `"UNRANKED"`.

**Expected Result (PASS):** Boundary is inclusive at exactly 50.
**Expected Result (FAIL):** Off-by-one error (49 counted as BRONZE, or 50 counted as UNRANKED).

**Current Status:** 🔴 Not written

---

### CTB-TC-008 — Badge tier boundaries: exactly at SILVER_MIN (200) and GOLD_MIN (500)

**Severity:** `MEDIUM`
**Feature Under Test:** `BadgeThresholdPolicy.resolveTier()`
**Test File:** `BadgeThresholdPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-CTB-002`

**Test Steps:**
1. `resolveTier(199)` → assert `"BRONZE"`.
2. `resolveTier(200)` → assert `"SILVER"`.
3. `resolveTier(499)` → assert `"SILVER"`.
4. `resolveTier(500)` → assert `"GOLD"`.

**Expected Result (PASS):** All four boundary values map correctly.
**Expected Result (FAIL):** Any off-by-one at either threshold.

**Current Status:** 🔴 Not written

---

### CTB-TC-009 — page/size out of bounds rejected as 400

**Severity:** `LOW`
**Feature Under Test:** `ContributionQueryRequest` Bean Validation
**Test File:** `ExpertContributionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §10 — CTB-001`, `TDS §8.1 @Min/@Max`

**Test Steps:**
1. Request `size=101` → assert 400 CTB-001.
2. Request `page=-1` → assert 400 CTB-001.
3. Request `size=100` (boundary, allowed max) → assert 200.

**Expected Result (PASS):** 101/−1 rejected; 100 accepted.
**Expected Result (FAIL):** Silent clamping or incorrect rejection of the valid boundary.

**Current Status:** 🔴 Not written

---

### CTB-TC-010 — Row with points = NULL does not crash the summary SUM aggregation (schema boundary L2)

**Severity:** `MEDIUM`
**Feature Under Test:** `IContributionPointRepository.sumPointsByUserId()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ContributionPointRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `V1__init_schema.sql` L243 (`points integer`, nullable) + Logic Issue L2

**Preconditions:** FX-108 (`points = NULL`) plus one normal FX-101-style row (`points=10`) for the same expert, seeded via Testcontainers.

**Test Steps:**
1. Seed both rows.
2. Call `sumPointsByUserId(expertAUserId)`.
3. Assert result `== 10` (NULL row contributes 0, does not throw, does not become `NULL` overall due to `COALESCE`).

**Expected Result (PASS):** Deterministic non-null sum.
**Expected Result (FAIL):** Query throws, or returns `NULL` instead of `10` (missing COALESCE), or unboxing NPE if entity field is a primitive `int`.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### CTB-TC-SEC-001 — Extraneous userId query param is ignored; caller always receives only their own ledger

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Legal:** `BR-RBAC`, data isolation requirement (TDS RG-4)
**Feature Under Test:** `ExpertContributionController` DTO binding + full request pipeline
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ExpertContributionSecurityIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-101 (Expert A) and FX-102 (Expert B) both seeded via Testcontainers.

**Test Steps (Attack Simulation):**
1. Authenticate as Expert A (FX-105).
2. Call `GET /api/v1/experts/me/contribution-points/activity?userId=<Expert-B-UUID>&expertId=<Expert-B-UUID>`.
3. Inspect response `content[]` — assert every row's `pointRecordId` belongs to Expert A's seeded set (FX-101), never Expert B's (FX-102).

**Expected Result (PASS = hệ thống an toàn):** 200 OK with ONLY Expert A's rows; extraneous params silently ignored.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any Expert B row appears, or endpoint 500s on unknown param binding.

**Current Status:** 🔴 Not written

---

### CTB-TC-SEC-002 — Cross-expert repository query never returns another expert's ledger row, even with no FK integrity

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** Data isolation (TDS RG-4)
**Feature Under Test:** `IContributionPointRepository.findByUserIdOrderByRecordedAtDesc()`
**Test File:** `ContributionPointRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-101, FX-102 both seeded (different `user_id`, no FK constraint per Logic Issue L3).

**Test Steps (Attack Simulation):**
1. Query repository directly with Expert A's `userId`.
2. Assert result set size == FX-101's row count exactly, and every row's `userId == Expert A's ID`.
3. Repeat with Expert B's ID, assert fully disjoint result set.

**Expected Result (PASS = hệ thống an toàn):** Disjoint result sets, zero overlap, independent of the absence of an FK constraint.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any row appears in both result sets.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### CTB-TC-INT-001 — Full authenticated flow: seeded DB -> API -> isolated per-expert response

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Policy -> Service -> Repository -> PostgreSQL (Testcontainers)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/contribution/ExpertContributionControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:** PostgreSQL Testcontainer, Flyway auto-applied. Seed FX-101 (Expert A) and FX-102 (Expert B).

**Test Steps:**
1. Authenticate as Expert A (FX-105).
2. `GET /contribution-points/summary` → assert `totalPoints==265`.
3. `GET /contribution-points/activity` → assert `totalElements==5`, all rows belong to Expert A.
4. Re-authenticate as Expert B (FX-106), repeat — assert `totalPoints==30`, `totalElements==2`, fully independent of step 2-3.

**Expected Result (PASS):** Each expert sees exactly their own totals/rows.
**Expected Result (FAIL):** Any mismatch or cross-contamination.

**DB Assertion:**
```java
List<ContributionPointEntity> expertARows = contributionPointRepository.findByUserIdOrderByRecordedAtDesc(expertAUserId, Pageable.unpaged()).getContent();
assertThat(expertARows).hasSize(5);
assertThat(expertARows).allMatch(r -> r.getUserId().equals(expertAUserId));
```

**Current Status:** 🔴 Not written

---

### CTB-TC-INT-002 — Summary total equals SUM of every page of the activity ledger for the same expert

**Severity:** `MEDIUM`
**Feature Under Test:** `Cross-check: getContributionSummary() vs. getContributionActivity() consistency`
**Test File:** `ExpertContributionControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:** FX-101 seeded (5 rows, sum 265).

**Test Steps:**
1. Fetch summary — record `totalPoints`.
2. Fetch activity with `size=100` (all rows on one page).
3. Manually sum `points` across all `content[]` items.
4. Assert manual sum == summary's `totalPoints`.

**Expected Result (PASS):** Exact equality.
**Expected Result (FAIL):** Any drift.

**Current Status:** 🔴 Not written

---

### CTB-WEB-TC-001 — ContributionPointsPage renders summary, badge indicator, and activity table on successful fetch

**Severity:** `MEDIUM`
**Feature Under Test:** `ContributionPointsPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expertContribution/pages/ContributionPointsPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** Mocked fetch of `GET .../summary` and `.../activity` returning happy-path fixtures matching TDS §9.2.

**Test Steps:**
1. Render `<ContributionPointsPage />` inside a `QueryClientProvider` test harness.
2. `await screen.findByText(...)` for total points figure and current badge tier label.
3. Assert activity table renders one row per fixture item.

**Expected Result (PASS):** Summary + badge + table populated.
**Expected Result (FAIL):** Stuck loading state or wrong values rendered.

**Current Status:** 🔴 Not written

---

### CTB-WEB-TC-002 — ContributionPointsPage renders empty state when API returns zero points

**Severity:** `LOW`
**Feature Under Test:** `ContributionPointsPage.tsx`
**Test File:** `ContributionPointsPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Mock API returning zeroed summary (`totalPoints:0`, `currentBadgeTier:"UNRANKED"`) + empty activity array.
2. Render component.
3. Assert an empty-state message is shown (e.g., "No contribution activity yet"), not a blank/broken table.

**Expected Result (PASS):** Explicit empty-state UI per SRS AF2.
**Expected Result (FAIL):** Blank screen or crash on empty array `.map()`.

**Current Status:** 🔴 Not written

---

### CTB-WEB-TC-003 — ContributionPointsPage renders access-denied/error UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `ContributionPointsPage.tsx`
**Test File:** `ContributionPointsPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Test Steps:**
1. Mock API returning 403 `CTB-004` error body.
2. Render component.
3. Assert an error/access-denied message is shown, not a crash or infinite spinner.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection / white screen.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CTB-TC-001` | `ExpertContributionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-002` | `ExpertContributionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-003` | `ExpertContributionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-004` | `ExpertContributionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-005` | `ExpertContributionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-006` | `BadgeThresholdPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-007` | `BadgeThresholdPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-008` | `BadgeThresholdPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-009` | `ExpertContributionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-010` | `ContributionPointRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-SEC-001` | `ExpertContributionSecurityIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-SEC-002` | `ContributionPointRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-INT-001` | `ExpertContributionControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-TC-INT-002` | `ExpertContributionControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CTB-WEB-TC-001` | `ContributionPointsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `CTB-WEB-TC-002` | `ContributionPointsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `CTB-WEB-TC-003` | `ContributionPointsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertContributionService implements IExpertContributionService {

    @Override
    public ContributionSummaryResponse getContributionSummary(UUID callerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Page<ContributionActivityItemResponse> getContributionActivity(UUID callerUserId, ContributionQueryRequest query) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ExpertContributionPolicy {
    public ExpertProfileEntity assertIsVerifiedExpertOwner(UUID callerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class BadgeThresholdPolicy {
    public BadgeTier resolveTier(long totalPoints) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public Optional<BadgeTier> resolveNextTier(long totalPoints) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CTB-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CTB-TC-004` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CTB-TC-006` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CTB-TC-SEC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CTB-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CONSULTATION-IMP-098` approved (currently Draft, ADR-CTB-001/002/003 Proposed/Accepted-as-documentation)
- [ ] Logic Issues (§2, L1-L4) confirmed with Tech Lead
- [ ] Product has been asked to confirm/replace badge threshold placeholders (ADR-CTB-002) — non-blocking for test-writing (tests target the placeholder values explicitly, trivially updatable)
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green (note: `vitest`/`@testing-library/react` are NOT yet in `package.json` — same infrastructure prerequisite as sibling UC97 Test-Spec §6; must be added once, shared across both features)
- [ ] Test coverage ≥ 80% lines for `ExpertContributionService`, `ExpertContributionPolicy`, `BadgeThresholdPolicy`
- [ ] No business logic in `ExpertContributionController` (validation + mapping only)
- [ ] `CTB-TC-SEC-001` and `CTB-TC-SEC-002` both green — mandatory gate given data-isolation risk, even though data is non-financial
- [ ] Badge threshold placeholder values remain clearly commented in code as pending Product confirmation (ADR-CTB-002) — verified by code review, not an automated test

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `ContributionTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet installed — CTB-WEB-TC-* suspended until added (shared prerequisite with UC97, one PR covers both)
- Sibling UC92 (community answer point-earning writer) remains unimplemented — does not block UC98's own tests (seed data directly), but blocks realistic non-empty demo data

---

## 7. Rollback Plan

```bash
# No migration to revert — UC98 introduces no schema change.

git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/contribution/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/contribution/
git checkout -- 05_Development/CareBridgeWebApp/src/features/expertContribution/

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — ADR-CTB-001/002/003 cover all decisions | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — controller tests only assert validation/RBAC status codes | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, or assumes a concrete UC92 event class | ☑ Không phát hiện — no UC92 event/method referenced anywhere in this spec (ADR-CTB-003 honored) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review and Red Gate execution at implementation time.*
