# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-112 View Expert Dashboard — Test Specification

**Document ID:** `CB-EXPGOV-TDD-112`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source (`expert_profiles` L786-800, `community_answers` L82-94, `consultation_bookings` L876-896, `expert_reviews` L957-967, `content_reports` L222-234)
- `04_Implement/UC112_ViewExpertDashboard/UC112_ViewExpertDashboard_TDS.md` — companion TDS (this spec implements §8/§9/§10/§16/§17 of it)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.14 — UC-112 functional requirements
- `04_Implement/UC97_ViewRevenueAndCommission/UC97_ViewRevenueAndCommission_Test-Spec.md` — structural/style precedent for a read-only Admin aggregate dashboard
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.
> Test cases KHÔNG hardcode một business threshold cụ thể cho "report rate cao" hay bất kỳ giá trị nghiệp vụ nào — chỉ assert tính đúng đắn CẤU TRÚC/TOÁN HỌC của công thức đã `Accepted` trong TDS (ADR-DASH-001/002, cả hai đã Accepted 2026-07-02; `target_type` literal xác nhận là `ANSWER`).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC112 |
| `2026-07-02` | `AI Agent` | Đóng OI-1/OI-2 theo quyết định Product: ADR-DASH-001/002 → `Accepted`; `target_type = ReportTargetType.ANSWER` xác nhận từ code thật; bỏ trạng thái blocking trên `UC112-TC-004/005/013`, Entry/Exit/Suspension Criteria |

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
| **Feature / Gap ID** | `GAP-UC112` |
| **Module** | `Expert — Admin Governance Dashboard (aggregate summary)` |
| **Spec gốc** | `CB-EXPGOV-IMP-112` |
| **Priority** | High (SRS Table 80), no financial write-path — 🟠 P1 |
| **Sprint** | `Sprint 3 "Cross-Domain Integration"` — TV4-Lâm |
| **Milestone** | Admin Portal expert governance reporting |
| **Data Classification** | `Internal` — aggregate counts/sums/averages only, zero individual identifiers (ADR-DASH-003) |
| **Compliance Scope** | `PDPA`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `expert_profiles`, `community_answers`, `consultation_bookings`, `expert_reviews`, `content_reports` (all existing schema, read-only — tests seed directly) |
| **Downstream Consumers** | None (terminal reporting view) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPGOV-IMP-112 §17` (ADR-DASH-001 through ADR-DASH-005) |
| **Constraints Injected** | No invented "reports" export feature; no invented composite quality score; zero individual-identifier fields in response; no new migration/cache/scheduled job; `SYSTEM_ADMIN`-only |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Description names "reports" as a dashboard metric with zero elaboration; no `expert_reports` table exists anywhere in `V1__init_schema.sql` | `ADR-DASH-001` (Status: `Proposed`, still Open per TDS §18 OI-1) resolves this to `content_reports` filtered to expert-authored `community_answers` (`is_expert_labeled=true`), joined via the untyped `target_type`/`target_id` columns — the exact `target_type` literal is owned by the moderation module and is NOT confirmed in this TDS | Tests assert the STRUCTURAL correctness of the count/join (e.g., given N seeded `content_reports` rows pointing at expert-authored answers, `totalReportsAgainstExpertContent == N`) using a placeholder `target_type` value injected via the test fixture/config, NOT a hardcoded business threshold for "high report rate." No test asserts what report COUNT should trigger an alert or be considered "too many" — that judgment remains Open (OI-1) |
| L2 | SRS Description names "quality metrics" (plural) with no formula | `ADR-DASH-002` (Status: `Proposed`, Open per TDS §18 OI-2) resolves this to `AVG(expert_reviews.rating)` + rating distribution (1-5) + report-rate% — explicitly rejects any weighted composite "quality score" (Option C rejected in ADR-DASH-002) | Tests assert the MATH is correct (`averageRating` == arithmetic mean of seeded ratings; `ratingDistribution` counts match seed; `reportRatePercentage` == `reports / totalExpertAnswers * 100`) — no test encodes a specific numeric threshold (e.g., "rating below 3.5 is bad") since no such business rule is Accepted anywhere in the TDS |
| L3 | `content_reports.target_type` has no DB CHECK constraint (untyped polymorphic reference, `V1__init_schema.sql` L222-234) | Application-layer filtering by `target_type` is a hard dependency on a literal string owned by another module, not yet confirmed (TDS §18 OI-1) | `UC112-TC-013` (boundary) verifies that a `content_reports` row with an unexpected/unrelated `target_type` (e.g., `'QUESTION'` instead of the expected answer-type literal) is correctly EXCLUDED from `reportsMetric`, proving the filter is type-safe rather than counting everything indiscriminately |
| L4 | `expert_reviews.moderation_status` and rating aggregation could divide by zero when no reviews exist | TDS §8.2 mandates `COALESCE(AVG(...), 0)` in the JPQL query (`IExpertReviewRepository.averageRatingWhereModerationStatus`) | `UC112-TC-008` (boundary, 0 reviews) explicitly verifies `averageRating == BigDecimal.ZERO`, not `null` and not a division-by-zero exception |
| L5 | SRS Alternative Flow AF2 ("system displays an empty state") is a generic cross-UC template applied to a dashboard with 5 independent metric groups | TDS §6.2 mandates every count/sum/avg coalesces to zero on empty tables, HTTP 200 (never 404/500) | `UC112-TC-006` seeds a completely empty DB (Testcontainers, no rows in any of the 5 tables) and asserts a fully-zeroed `ExpertDashboardResponse`, not an exception |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Expert.Dashboard module bao gồm các layer:
├── Domain (aggregation math — pure logic, no framework dependency)
├── Services (ExpertDashboardService — mock 5 repositories với Mockito)
├── Controller (ExpertDashboardController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — real rows across all 5 tables)
└── Web Frontend (React Testing Library + Vitest — ExpertDashboardPage, summary/metric cards)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-112 §3.2.2.14` | Happy path display, empty state (AF2), access-denied (E1) |
| `ADR-DASH-001` (Accepted) | Structural join correctness for `reportsMetric`; explicitly NOT a business threshold |
| `ADR-DASH-002` (Accepted) | Structural/mathematical correctness of `qualityMetrics`; explicitly NOT a composite score |
| `ADR-DASH-003` (Accepted) | Zero individual-identifier fields anywhere in the response — CRITICAL test |
| `ADR-DASH-004` (Accepted) | `SYSTEM_ADMIN`-only authorization |
| `ADR-DASH-005` (Accepted) | Query-time aggregation, no cache — every call recomputes from live table state |
| `BR-RBAC` | Role gating |
| `BR-CONSULTATION` | No mutation exposed; read-only repository usage only |
| `V1__init_schema.sql` | Exact column names/types/nullability driving fixtures and boundary tests |
| `CB-EXPGOV-IMP-112 §8/§9/§10` | Service/repository contracts, API shapes, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | 3 experts seeded across verification statuses → correct `expertCounts` | `ExpertDashboardService.getDashboardSummary()` | `UC112-TC-001` |
| TC-COND-002 | Expert-authored answers seeded across statuses → correct `answerMetrics` | Service aggregation | `UC112-TC-002` |
| TC-COND-003 | Consultation bookings seeded across statuses → correct `consultationMetrics` | Service aggregation | `UC112-TC-003` |
| TC-COND-004 | `content_reports` seeded against expert-authored answers (`target_type='ANSWER'`, confirmed literal) → correct `reportsMetric` count | Service aggregation | `UC112-TC-004` |
| TC-COND-005 | `expert_reviews` seeded with known ratings → `averageRating`/`ratingDistribution` math correct | Service aggregation | `UC112-TC-005` |
| TC-COND-006 | Completely empty DB (all 5 tables) → fully-zeroed response, HTTP 200 | Service + Controller | `UC112-TC-006` |
| TC-COND-007 | Non-SYSTEM_ADMIN role (EXPERT/MODERATOR/CONTENT_ADMIN/MOTHER/FAMILY) attempts access | Controller RBAC guard | `UC112-TC-007` |
| TC-COND-008 | 0 reviews → `averageRating` coalesces to 0, no divide-by-zero | Repository/Service (L4) | `UC112-TC-008` |
| TC-COND-009 | 1 review → `averageRating` equals that single rating exactly | Repository/Service | `UC112-TC-009` |
| TC-COND-010 | Many reviews (mixed 1-5) → `averageRating` and `ratingDistribution` match hand-computed expectation | Repository/Service | `UC112-TC-010` |
| TC-COND-011 | Response DTO never contains an individual `UUID`/name/email for any expert/review/report/booking | Response DTO shape (ADR-DASH-003) — CRITICAL | `UC112-TC-011` |
| TC-COND-012 | Unauthenticated request (no/invalid JWT) | Spring Security filter chain | `UC112-TC-012` |
| TC-COND-013 | `content_reports` row with unrelated `target_type` is excluded from `reportsMetric` (L3 boundary) | Repository query filter | `UC112-TC-013` |
| TC-COND-014 | `reportRatePercentage` denominator is 0 (zero expert answers) → 0, not divide-by-zero/NaN | Service derived-field math | `UC112-TC-014` |
| TC-COND-015 | Full seeded flow: DB → API → correct numbers across all 5 metric groups simultaneously | Integration (Testcontainers) | `UC112-TC-INT-001` |
| TC-COND-016 | Web: ExpertDashboardPage renders all summary cards on successful fetch | React component | `UC112-WEB-TC-001` |
| TC-COND-017 | Web: ExpertDashboardPage renders empty-state cards when all metrics are zero | React component | `UC112-WEB-TC-002` |
| TC-COND-018 | Web: ExpertDashboardPage renders access-denied/error UI on 403 | React component | `UC112-WEB-TC-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role partitions (`SYSTEM_ADMIN` / `EXPERT` / `MODERATOR` / `CONTENT_ADMIN` / `MOTHER`&`FAMILY` / Unauthenticated) | Six distinct authorization outcomes per §16 Authorization Matrix |
| Boundary Value Analysis | Review count (0 / 1 / many); `reportRatePercentage` denominator (0 / >0); rating value bounds (1 / 5) | Schema-driven boundaries directly from `expert_reviews.rating` (smallint 1-5) and division-by-zero risk (L4/L2) |
| Structural / Mathematical Verification | `AVG`, `COUNT`, `GROUP BY`, percentage derivation | ADR-DASH-001/002 are both `Accepted` (business meaning + arithmetic) and must be provably correct |
| State Transition N/A | — | UC-112 is pure read; no state machine to cover |
| Error Guessing | Unrelated `target_type` counted by mistake; PII field accidentally serialized onto DTO; cache staleness reintroduced | Highest risk surfaces: ADR-DASH-001 join scope, ADR-DASH-003 identifier leakage, ADR-DASH-005 no-cache invariant |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | 3 `expert_profiles`: 1 `VERIFIED`, 1 `PENDING`, 1 `REJECTED` | `expertCounts` structural correctness (TC-COND-001) |
| `FX-002` | DB seed | 5 `community_answers` with `is_expert_labeled=true` (3 `APPROVED`, 1 `PENDING`, 1 `HIDDEN`) + 2 with `is_expert_labeled=false` (must be excluded) | `answerMetrics` correctness + label-filter proof (TC-COND-002) |
| `FX-003` | DB seed | 4 `consultation_bookings`: 2 `COMPLETED`, 1 `CANCELLED`, 1 `PENDING_PAYMENT` | `consultationMetrics` correctness (TC-COND-003) |
| `FX-004` | DB seed | 2 `content_reports` rows with `target_type` = the expert-answer literal, `target_id` pointing at 2 of FX-002's expert-labeled answer IDs; 1 `content_reports` row with an unrelated `target_type` (e.g., `'QUESTION'`) | `reportsMetric` structural join correctness + L3 boundary exclusion (TC-COND-004, TC-COND-013) |
| `FX-005` | DB seed | 6 `expert_reviews`, `moderation_status='APPROVED'`, ratings `[5,5,5,4,3,1]` | `qualityMetrics` math verification — hand-computed avg = 3.8333..., distribution `{1:1,3:1,4:1,5:3}` (TC-COND-005/010) |
| `FX-006` | DB seed | 0 `expert_reviews` rows | Divide-by-zero boundary (TC-COND-008) |
| `FX-007` | DB seed | Exactly 1 `expert_reviews` row, `rating=4` | Single-review boundary — avg must equal exactly `4` (TC-COND-009) |
| `FX-008` | JWT | `{ sub: admin-user-id, role: 'SYSTEM_ADMIN' }` | Authorized caller |
| `FX-009` | JWT | `{ sub: expert-user-id, role: 'EXPERT' }` | Unauthorized-role caller |
| `FX-010` | JWT | `{ sub: moderator-user-id, role: 'MODERATOR' }` | Unauthorized-role caller |
| `FX-011` | DB seed | Completely empty `expert_profiles`/`community_answers`/`consultation_bookings`/`expert_reviews`/`content_reports` | Empty-state / AF2 (TC-COND-006) |
| `FX-012` | DB seed | 0 expert-labeled `community_answers` (denominator=0) but 1 `content_reports` row present | `reportRatePercentage` divide-by-zero guard (TC-COND-014) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertDashboardTestFactory.java — mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class ExpertDashboardTestFactory {

    static ExpertProfileEntity makeExpertProfile(ExpertVerificationStatus status) {
        ExpertProfileEntity e = new ExpertProfileEntity();
        e.setExpertProfileId(UUID.randomUUID());
        e.setUserId(UUID.randomUUID());
        e.setVerificationStatus(status);
        e.setRatingAvg(BigDecimal.ZERO);
        e.setCreatedAt(Instant.now());
        return e;
    }

    static CommunityAnswerEntity makeExpertAnswer(String status, Consumer<CommunityAnswerEntity> overrides) {
        CommunityAnswerEntity a = new CommunityAnswerEntity();
        a.setId(UUID.randomUUID());
        a.setQuestionId(UUID.randomUUID());
        a.setAuthorId(UUID.randomUUID());
        a.setIsExpertLabeled(true);
        a.setStatus(status);
        a.setCreatedAt(Instant.now());
        overrides.accept(a);
        return a;
    }

    static ConsultationBookingEntity makeBooking(UUID expertProfileId, String status) {
        ConsultationBookingEntity b = new ConsultationBookingEntity();
        b.setBookingId(UUID.randomUUID());
        b.setExpertProfileId(expertProfileId);
        b.setStatus(status);
        b.setScheduledStart(Instant.now());
        b.setCreatedAt(Instant.now());
        return b;
    }

    static ExpertReviewEntity makeReview(UUID expertProfileId, int rating, Consumer<ExpertReviewEntity> overrides) {
        ExpertReviewEntity r = new ExpertReviewEntity();
        r.setReviewId(UUID.randomUUID());
        r.setExpertProfileId(expertProfileId);
        r.setRating((short) rating);
        r.setModerationStatus("APPROVED");
        r.setCreatedAt(Instant.now());
        overrides.accept(r);
        return r;
    }

    static ContentReportEntity makeReport(String targetType, UUID targetId, String status) {
        ContentReportEntity c = new ContentReportEntity();
        c.setReportId(UUID.randomUUID());
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        c.setStatus(status);
        c.setCreatedAt(Instant.now());
        return c;
    }
}
```

---

### UC112-TC-001 — Three experts seeded across statuses produce structurally correct expertCounts

**Severity:** `HIGH`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` — `expertCounts` group
**Test File:** `src/test/java/com/carebridge/backend/expert/dashboard/ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §1.2 metric mapping table ("expert count")`, `TDS §8.1 ExpertCountsResponse`

**Preconditions:** FX-001 (1 `VERIFIED`, 1 `PENDING`, 1 `REJECTED`, 0 `NEEDS_MORE_INFO`).

**Test Steps:**
1. Mock `IExpertProfileRepository.countByVerificationStatus()` per enum value to return `{PENDING:1, VERIFIED:1, REJECTED:1, NEEDS_MORE_INFO:0}`.
2. Call `expertDashboardService.getDashboardSummary()`.
3. Assert `expertCounts.totalExperts == 3` and each per-status count matches exactly.

**Expected Result (PASS):** `totalExperts=3`, `pendingCount=1`, `verifiedCount=1`, `rejectedCount=1`, `needsMoreInfoCount=0`.
**Expected Result (FAIL):** Any count mismatched, `totalExperts` not equal to the sum of the four per-status counts.

**Current Status:** 🔴 Not written
**Implementation Note:** `totalExperts` MUST equal the sum of the four per-status counts — this is a structural invariant, not a business threshold.

---

### UC112-TC-002 — Expert-labeled answers counted correctly; non-expert-labeled answers excluded

**Severity:** `HIGH`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` — `answerMetrics` group
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §1.2 ("answers")`, `V1__init_schema.sql community_answers.is_expert_labeled`

**Preconditions:** FX-002 (5 expert-labeled: 3 `APPROVED`/1 `PENDING`/1 `HIDDEN`; 2 non-expert-labeled).

**Test Steps:**
1. Mock `ICommunityAnswerRepository.countByIsExpertLabeledTrue()` → `5`; `countByIsExpertLabeledTrueAndStatus()` per status → `{PENDING:1, APPROVED:3, HIDDEN:1}`.
2. Call `getDashboardSummary()`.
3. Assert `answerMetrics.totalExpertAnswers == 5` (never `7` — the 2 non-labeled rows must never be counted).

**Expected Result (PASS):** `totalExpertAnswers=5`, `pendingCount=1`, `approvedCount=3`, `hiddenCount=1`.
**Expected Result (FAIL):** `totalExpertAnswers` includes non-expert-labeled rows (e.g., `=7`).

**Current Status:** 🔴 Not written

---

### UC112-TC-003 — Consultation bookings grouped by status produce correct consultationMetrics

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` — `consultationMetrics` group
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §1.2 ("consultations")`

**Preconditions:** FX-003 (2 `COMPLETED`, 1 `CANCELLED`, 1 `PENDING_PAYMENT`).

**Test Steps:**
1. Mock `IConsultationBookingRepository.countByStatus()` per status.
2. Call `getDashboardSummary()`.
3. Assert `consultationMetrics.totalBookings == 4` and per-status breakdown matches.

**Expected Result (PASS):** `totalBookings=4`, `completedCount=2`, `cancelledCount=1`, `pendingPaymentCount=1`.
**Expected Result (FAIL):** `totalBookings` not equal to sum of per-status counts.

**Current Status:** 🔴 Not written

---

### UC112-TC-004 — reportsMetric structural join correctness (ADR-DASH-001, Accepted)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` — `reportsMetric` group
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-DASH-001` (Status: `Accepted`, 2026-07-02) — `target_type = ReportTargetType.ANSWER` confirmed literal, `content.entity.ReportTargetType`

**Preconditions:** FX-004 (2 reports with `target_type='ANSWER'` correctly targeting expert-labeled answers, 1 report with `target_type='QUESTION'` — see UC112-TC-013 for that exclusion assertion).

**Test Steps:**
1. Mock `ICommunityAnswerRepository.findIdsByIsExpertLabeledTrue()` to return the set of expert-answer IDs from FX-002/FX-004.
2. Mock `IContentReportRepository.countByTargetTypeAndTargetIdIn(ReportTargetType.ANSWER, expertAnswerIds)` → `2`.
3. Call `getDashboardSummary()`.
4. Assert `reportsMetric.totalReportsAgainstExpertContent == 2` — exactly the count of `ANSWER`-typed reports whose `target_id` is in the expert-answer ID set.

**Expected Result (PASS):** Count reflects only `ANSWER`-typed reports correctly joined to expert-authored content; no assertion is made about whether `2` represents a "high" or "acceptable" report rate — that is a UI/product-copy concern, not this test's scope.
**Expected Result (FAIL):** Count includes the `QUESTION`-typed report (`=3`), or omits a correctly-targeted one.

**Current Status:** 🔴 Not written
**Implementation Note:** `target_type = ReportTargetType.ANSWER` is now a confirmed literal (code-verified, not a placeholder) — the test uses this exact enum value directly, no longer configurable/parameterized.

---

### UC112-TC-005 — qualityMetrics arithmetic correctness (ADR-DASH-002, Accepted)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` — `qualityMetrics` group
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-DASH-002` (Status: `Accepted`, 2026-07-02), `TDS §8.1 QualityMetricsResponse`

**Preconditions:** FX-005 (6 `APPROVED` reviews, ratings `[5,5,5,4,3,1]`).

**Test Steps:**
1. Mock `IExpertReviewRepository.averageRatingWhereModerationStatus("APPROVED")` → `new BigDecimal("3.8333")` (hand-computed: `(5+5+5+4+3+1)/6 = 23/6 = 3.8333...`).
2. Mock `countGroupByRating("APPROVED")` → `{1:1, 3:1, 4:1, 5:3}`.
3. Call `getDashboardSummary()`.
4. Assert `qualityMetrics.averageRating` matches the hand-computed mean and `ratingDistribution` matches exactly, including the ABSENT key `2` (no reviews with rating=2 — must not appear as `{2:0}` or be omitted inconsistently; assert against the documented map shape only).

**Expected Result (PASS):** `averageRating ≈ 3.8333` (scale-tolerant comparison), distribution counts match seed exactly. No assertion on whether this average is "good" or "bad" quality — Product confirmed no additional threshold/scoring is required (former OI-2, now closed).
**Expected Result (FAIL):** Average miscalculated (e.g., simple count-based instead of rating-weighted), or distribution includes reviews with `moderation_status != 'APPROVED'`.

**Current Status:** 🔴 Not written
**Implementation Note:** Per ADR-DASH-002 Option C (explicitly REJECTED), this test must never assert a weighted composite "quality score" — only the plain average, distribution, and report-rate percentage.

---

### UC112-TC-006 — Completely empty database returns fully-zeroed response with HTTP 200, not an error (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardService.getDashboardSummary()` + `ExpertDashboardController`
**Test File:** `ExpertDashboardServiceTest.java` (service level) / `ExpertDashboardControllerIntegrationTest.java` (HTTP level)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `SRS §3.2.2.14 AF2`, `TDS §6.2 Empty-State Sequence Diagram`

**Preconditions:** FX-011 (all 5 tables empty).

**Test Steps:**
1. Mock/seed all repositories to return `0`/empty/`NULL` for every aggregate query.
2. Call `getDashboardSummary()` (service test) and `GET /api/v1/admin/expert-dashboard/summary` (integration test, SYSTEM_ADMIN JWT).
3. Assert every count field `== 0`, `averageRating == BigDecimal.ZERO`, `ratingDistribution` is an empty map, `reportRatePercentage == BigDecimal.ZERO`.
4. Assert HTTP 200 (never 404/500) at the integration level.

**Expected Result (PASS):** Fully-zeroed `ExpertDashboardResponse`, HTTP 200.
**Expected Result (FAIL):** Any `NullPointerException`, `ArithmeticException` (divide-by-zero), or non-200 status for a legitimately empty but valid admin request.

**Current Status:** 🔴 Not written

---

### UC112-TC-007 — Non-SYSTEM_ADMIN roles are rejected with 403 at the controller/security layer

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertDashboardController` (`@WebMvcTest` with mocked `IExpertDashboardService`)
**Test File:** `src/test/java/com/carebridge/backend/expert/dashboard/ExpertDashboardControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-DASH-004`, `BR-RBAC`, `TDS §16 Authorization Matrix`

**Preconditions:** FX-009 JWT (`EXPERT`), FX-010 JWT (`MODERATOR`); additional runs with `CONTENT_ADMIN`, `MOTHER`, `FAMILY` roles per the Authorization Matrix.

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/expert-dashboard/summary").with(jwt of each non-admin role))`, one sub-test per role (parameterized test).
2. Assert HTTP 403 with `error.code == "DASH-103"` for every non-`SYSTEM_ADMIN` role.
3. Assert `IExpertDashboardService` was never invoked (Mockito `verifyNoInteractions`) for any of them.

**Expected Result (PASS):** 403 DASH-103 for all five non-admin roles tested; service never touched.
**Expected Result (FAIL):** 200 returned for any non-admin role, or service invoked despite wrong role.

**Current Status:** 🔴 Not written

---

### UC112-TC-008 — Zero reviews: averageRating coalesces to 0, no divide-by-zero (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `IExpertReviewRepository.averageRatingWhereModerationStatus()` / `ExpertDashboardService`
**Test File:** `ExpertDashboardServiceTest.java` (unit) + `ExpertReviewRepositoryIntegrationTest.java` (integration)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** Logic Issue L4, `TDS §8.2 COALESCE(AVG(...), 0)` JPQL

**Preconditions:** FX-006 (0 `expert_reviews` rows).

**Test Steps:**
1. Seed/mock zero review rows.
2. Call `getDashboardSummary()`.
3. Assert `qualityMetrics.averageRating.equals(BigDecimal.ZERO)` — not `null`, not an exception.

**Expected Result (PASS):** `averageRating == 0`, no exception.
**Expected Result (FAIL):** `NullPointerException`, `ArithmeticException`, or `averageRating == null`.

**Current Status:** 🔴 Not written
**Implementation Note:** This test guards the `COALESCE(AVG(...), 0)` clause in `IExpertReviewRepository` — removing it must fail this test.

---

### UC112-TC-009 — Exactly one review: averageRating equals that single rating exactly (boundary)

**Severity:** `LOW`
**Feature Under Test:** `ExpertDashboardService` — `qualityMetrics`
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §8.1 QualityMetricsResponse`

**Preconditions:** FX-007 (exactly 1 review, `rating=4`).

**Test Steps:**
1. Mock `averageRatingWhereModerationStatus()` → `new BigDecimal("4")`.
2. Call `getDashboardSummary()`.
3. Assert `averageRating.compareTo(new BigDecimal("4")) == 0` and `ratingDistribution == {4:1}`.

**Expected Result (PASS):** Exact match, no averaging artifact for n=1.
**Expected Result (FAIL):** Any rounding/truncation error or distribution mismatch.

**Current Status:** 🔴 Not written

---

### UC112-TC-010 — Many reviews with mixed ratings: averageRating and ratingDistribution match hand-computed expectation (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardService` — `qualityMetrics`
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §8.1 QualityMetricsResponse`

**Preconditions:** FX-005 (6 reviews, ratings `[5,5,5,4,3,1]`).

**Test Steps:**
1. Mock aggregation results per FX-005.
2. Call `getDashboardSummary()`.
3. Assert `averageRating` matches `23/6` within a defined scale tolerance, and `ratingDistribution` sums to `6` across its values (`1+1+1+3=6`).

**Expected Result (PASS):** Distribution values sum exactly to total review count; average matches hand computation.
**Expected Result (FAIL):** Distribution sum != total review count (indicates a lost/duplicated row in the `GROUP BY`).

**Current Status:** 🔴 Not written

---

### UC112-TC-011 — Response DTO never contains an individual identifier field (ADR-DASH-003) — CRITICAL PII guard

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control` (over-exposure) / `A08:2021 — Software and Data Integrity Failures` (unintended field serialization)
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** `PDPA`, `ADR-DASH-003`
**Feature Under Test:** `ExpertDashboardResponse` and all nested DTOs — full serialization shape
**Test File:** `src/test/java/com/carebridge/backend/expert/dashboard/ExpertDashboardResponseShapeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-DASH-003` (Accepted), `TDS §4.2 Minimum aggregation granularity NFR`

**Preconditions:** FX-001 through FX-005 seeded (realistic populated dashboard) via Testcontainers.

**Test Steps:**
1. Call `GET /api/v1/admin/expert-dashboard/summary` with a valid SYSTEM_ADMIN JWT against a fully-seeded DB.
2. Serialize the full JSON response body.
3. Reflectively/structurally assert NONE of the following ever appear anywhere in the payload: any field literally named/typed as an `expertProfileId` list, `reviewId`, `reportId`, `bookingId`, `authorId`, `userId`, `email`, or any raw `UUID` value belonging to an individual seeded entity (cross-check every emitted UUID-shaped string against the set of seeded entity IDs — none should match).
4. Assert the response contains ONLY the five documented top-level metric groups (`expertCounts`, `answerMetrics`, `consultationMetrics`, `reportsMetric`, `qualityMetrics`) plus `generatedAt`.

**Expected Result (PASS = safe):** Zero individual identifiers found anywhere in the response; only counts/sums/averages/distributions/timestamp.
**Expected Result (FAIL = PII leak):** Any seeded entity's UUID, name, or email appears verbatim in the response body.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the highest-priority test in this spec per ADR-DASH-003's explicit rationale (keeps Data Classification at `Internal`, avoids DPO gate). Any regression here is a security incident per TDS §12.1 rollback trigger table.

---

### UC112-TC-012 — Unauthenticated request is rejected with 401 before reaching controller logic

**Severity:** `CRITICAL`
**Feature Under Test:** Spring Security filter chain for `/api/v1/admin/expert-dashboard/**`
**Test File:** `ExpertDashboardControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §10 Error Codes — DASH-102`

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/expert-dashboard/summary"))` with no `Authorization` header.
2. Assert HTTP 401 and body `error.code == "DASH-102"`.

**Expected Result (PASS):** 401 DASH-102.
**Expected Result (FAIL):** Any other status, or NPE from missing principal.

**Current Status:** 🔴 Not written

---

### UC112-TC-013 — content_reports row with unrelated target_type is excluded from reportsMetric (schema boundary L3)

**Severity:** `HIGH`
**Feature Under Test:** `IContentReportRepository.countByTargetTypeAndTargetIdIn()`
**Test File:** `src/test/java/com/carebridge/backend/expert/dashboard/ContentReportRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** Logic Issue L3, `V1__init_schema.sql content_reports` (untyped `target_type`/`target_id`, no CHECK constraint)

**Preconditions:** FX-004 seeded via Testcontainers (2 correctly-typed reports + 1 with an unrelated `target_type`, e.g. `'QUESTION'`, pointing at a `community_questions` row rather than an expert answer).

**Test Steps:**
1. Seed all 3 `content_reports` rows.
2. Call `countByTargetTypeAndTargetIdIn(expertAnswerTargetType, expertAnswerIds)` directly against the repository.
3. Assert the returned count is exactly `2` — the unrelated-`target_type` row is excluded even though its structure otherwise resembles a valid report row.

**Expected Result (PASS):** Count excludes the mismatched-`target_type` row.
**Expected Result (FAIL):** Count includes it (`=3`), proving the filter is not type-safe — this would silently inflate `reportsMetric` with reports unrelated to expert content.

**Current Status:** 🔴 Not written

---

### UC112-TC-014 — reportRatePercentage denominator of zero yields 0, not divide-by-zero/NaN (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardService` — `qualityMetrics.reportRatePercentage` derived-field math
**Test File:** `ExpertDashboardServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §8.1 QualityMetricsResponse` (`reportRatePercentage`, "0 if denominator 0"), `ADR-DASH-002` decision block

**Preconditions:** FX-012 (0 expert-labeled answers, but 1 `content_reports` row present — an edge case where the numerator is nonzero but the denominator is zero).

**Test Steps:**
1. Mock `answerMetrics.totalExpertAnswers == 0` and `reportsMetric.totalReportsAgainstExpertContent == 1`.
2. Call `getDashboardSummary()`.
3. Assert `qualityMetrics.reportRatePercentage.equals(BigDecimal.ZERO)` — not `NaN`, not an `ArithmeticException`.

**Expected Result (PASS):** `reportRatePercentage == 0` with the explicit guard.
**Expected Result (FAIL):** `ArithmeticException: / by zero`, or `Infinity`/`NaN` serialized into the JSON response.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC112-TC-INT-001 — Full seeded flow across all 5 tables: DB → API → correct numbers for every metric group simultaneously

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request -> Controller -> Service -> 5 Repositories -> PostgreSQL (Testcontainers)`
**Test File:** `src/test/java/com/carebridge/backend/expert/dashboard/ExpertDashboardControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §6.1 Happy-Path Sequence Diagram`, `TDS §9.2 Response Schema`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied automatically.
- Seed FX-001 (3 experts) + FX-002 (5 expert-labeled + 2 non-labeled answers) + FX-003 (4 bookings) + FX-004 (3 reports, 2 valid) + FX-005 (6 reviews) via `JdbcTemplate`/JPA, fresh per Props Isolation rule.

**Test Steps:**
1. Authenticate as `SYSTEM_ADMIN` (FX-008).
2. `GET /api/v1/admin/expert-dashboard/summary`.
3. Assert `expertCounts.totalExperts == 3`, `answerMetrics.totalExpertAnswers == 5`, `consultationMetrics.totalBookings == 4`, `reportsMetric.totalReportsAgainstExpertContent == 2`, `qualityMetrics.averageRating` matches the hand-computed mean of FX-005's ratings.
4. Cross-check: re-run the equivalent raw SQL from `TDS §14.1 Database Inspection` directly against the Testcontainer and assert the API response matches the raw-SQL result exactly for every metric group.

**Expected Result (PASS):** Every metric group's numbers match both the seed data expectation AND the independently-run raw SQL query — proving the JPA/repository layer introduces no drift.
**Expected Result (FAIL):** Any metric group's count/average diverges from either the seed expectation or the raw-SQL cross-check.

**DB Assertion:**
```java
Long rawExpertCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM expert_profiles", Long.class);
assertThat(response.getExpertCounts().getTotalExperts()).isEqualTo(rawExpertCount.intValue());
```

**Current Status:** 🔴 Not written

---

### UC112-WEB-TC-001 — ExpertDashboardPage renders all summary cards on successful fetch

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/ExpertDashboardPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** MSW (or `vi.fn()` mock) intercepts `GET /api/v1/admin/expert-dashboard/summary`, returning the happy-path fixture from TDS §9.2.

**Test Steps:**
1. Render `<ExpertDashboardPage />` wrapped in a `QueryClientProvider` test harness.
2. `await screen.findByText(...)` for the total expert count figure.
3. Assert all five metric-group cards (`ExpertCountsCard`, `AnswerMetricsCard`, `ConsultationMetricsCard`, `ReportsMetricCard`, `QualityMetricsCard`) render with values matching the fixture.

**Expected Result (PASS):** All five cards populated from fetched data.
**Expected Result (FAIL):** Loading spinner stuck, or wrong/missing figures on any card.

**Current Status:** 🔴 Not written

---

### UC112-WEB-TC-002 — ExpertDashboardPage renders empty-state cards when all metrics are zero

**Severity:** `LOW`
**Feature Under Test:** `ExpertDashboardPage.tsx`
**Test File:** `ExpertDashboardPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

**Test Steps:**
1. Mock API returning the fully-zeroed empty-state fixture from TDS §9.2.
2. Render component.
3. Assert an explicit "No expert activity yet" (or equivalent) empty-state message is shown, not a blank/broken chart or `NaN`/`undefined` rendered anywhere.

**Expected Result (PASS):** Explicit empty-state UI per SRS AF2, no rendering artifacts from zero values.
**Expected Result (FAIL):** Blank screen, `NaN%` displayed for `reportRatePercentage`, or crash on empty `ratingDistribution` map iteration.

**Current Status:** 🔴 Not written

---

### UC112-WEB-TC-003 — ExpertDashboardPage renders access-denied/error UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertDashboardPage.tsx`
**Test File:** `ExpertDashboardPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Test Steps:**
1. Mock API returning 403 `DASH-103` error body.
2. Render component.
3. Assert an error/access-denied message is shown, not a crash or infinite spinner.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection / white screen.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC112-TC-001` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-002` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-003` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-004` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-005` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-006` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-007` | `ExpertDashboardControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-008` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-009` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-010` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-011` | `ExpertDashboardResponseShapeTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-012` | `ExpertDashboardControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-013` | `ContentReportRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-014` | `ExpertDashboardServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-TC-INT-001` | `ExpertDashboardControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC112-WEB-TC-001` | `ExpertDashboardPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC112-WEB-TC-002` | `ExpertDashboardPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC112-WEB-TC-003` | `ExpertDashboardPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertDashboardService implements IExpertDashboardService {

    @Override
    public ExpertDashboardResponse getDashboardSummary() {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC112-TC-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC112-TC-006` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC112-TC-011` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC112-TC-INT-001` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EXPGOV-IMP-112` reviewed (currently Draft; `ADR-DASH-001`/`ADR-DASH-002` both `Accepted` 2026-07-02 — former OI-1/OI-2 closed)
- [ ] Logic Issues (§2, L1-L5) confirmed with Tech Lead
- [ ] No migration required (TDS §5.2) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories
- [x] `UC112-TC-004`, `UC112-TC-013`, and the `reportsMetric`-related assertions in `UC112-TC-INT-001` now use the CONFIRMED `target_type = ReportTargetType.ANSWER` literal (code-verified) — no longer blocked

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — all integration tests green (Testcontainers)
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green (verify `vitest`/`@testing-library/react` are present in `05_Development/CareBridgeWebApp/package.json`; if not yet added by a prior UC, flagged as an infrastructure prerequisite, see Suspension Criteria)
- [ ] Test coverage ≥ 80% lines for `ExpertDashboardService`
- [ ] No business logic in `ExpertDashboardController` (validation/RBAC + mapping only)
- [ ] No individual identifiers/PII in logs at INFO or above
- [ ] `UC112-TC-011` (PII shape guard) green — mandatory gate, not optional, given ADR-DASH-003's CRITICAL classification
- [ ] `UC112-TC-013` (target_type filter boundary) green — mandatory gate, protects against silently over-counting `reportsMetric`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implementation
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `ExpertDashboardTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema citation (§4 "Oracle Source" fields)
- [x] **ADR discipline**: no test in this spec asserts a specific business threshold value for "high report rate" or "quality score" — verified by manual review of §4; both `ADR-DASH-001`/`ADR-DASH-002` are now `Accepted`

### Suspension Criteria

- ~~`ADR-DASH-001`/`ADR-DASH-002` Open~~ — **RESOLVED 2026-07-02**, no longer a suspension condition
- Web test infrastructure (Vitest + Testing Library) status inherited from prior UCs (e.g., UC97) — confirm presence in `package.json` before `UC112-WEB-TC-*` execution; if absent, install as a small separate prerequisite per CLAUDE.md "smallest scoped change"

---

## 7. Rollback Plan

```bash
# No migration to revert — UC112 introduces no schema change.

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/dashboard/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/dashboard/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/

# ADR-DASH-001/002 đã Accepted (2026-07-02) → không còn gap cần theo dõi ở bước này
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR, hoặc hardcode một threshold nghiệp vụ chưa Accepted | ☑ Không phát hiện — `UC112-TC-004`/`UC112-TC-005` explicitly test structure/math only, không assert giá trị "report rate cao" hay "quality tốt" nào | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — `UC112-TC-007`/`UC112-TC-012` test controller only for RBAC/auth, business logic asserted only in Service tests | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc giả định một `target_type` literal chưa xác nhận | ☑ Không phát hiện — all types match TDS §8 interfaces exactly; `target_type = ReportTargetType.ANSWER` is a confirmed, code-verified literal | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)
- [x] Explicitly verified: no test case in §4 encodes an invented numeric business threshold for "reports" volume or "quality" rating — all such assertions are limited to structural/mathematical correctness of the now-Accepted formula shape (ADR-DASH-001/002)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — ADR-DASH-001/002 both `Accepted` (2026-07-02); pending Tech Lead review and Red Gate execution at implementation time.*
