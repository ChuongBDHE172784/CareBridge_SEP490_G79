# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-241 View Expert Consultation Pricing

**Document ID:** `CB-CON-TDD-011`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending` *(no end-user PII in payload; commission/band internals are business-confidential, not PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential` *(band-internal fields are Confidential; end-user-facing payload is Internal, non-PII)*

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (lines 842–874, 1416–1420, 1817–1833) — primary schema source for `expert_consultation_prices` / `consultation_price_bands`
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.20.1 (Table 260, line ~5115) — SRS UC-241
- `04_Implement/UC241_ViewExpertConsultationPricing/UC241_ViewExpertConsultationPricing_TDS.md` (`CB-CON-IMP-011`) — Technical Specification, ADR-VPR-001..005
- `03_Design/UI_UX/MobileAppScreen/CB-204 Expert Consultation Pricing (UC-241)/code.html` — field/value oracle for happy-path response
- `03_Design/UI_UX/MobileAppScreen/CB-205 Booking Review (UC-75, UC-241)/code.html` — cancellation-policy field oracle
- `04_Implement/UC203_ViewConsultationDetail/UC203_ViewConsultationDetail_TDS.md` (`ADR-CDT-001`) — IDOR-gated contrast case

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo Test-Spec cho UC-241 View Expert Consultation Pricing |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-241` |
| **Module** | `ViewExpertConsultationPricing — consultation` |
| **Spec gốc** | `CB-CON-IMP-011` |
| **Priority** | 🟡 P2 |
| **Data Classification** | `Internal` (payload) / `Confidential` (excluded band internals) — no PII |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION`, `BR-PRIVACY` (business-confidentiality) |
| **Upstream Dependencies** | `expert_consultation_prices`, `consultation_price_bands` (FK only), `expert_profiles` |
| **Downstream Consumers** | CB-204 mobile screen, CB-205 Booking Review, UC-75 Book Private Consultation |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CON-IMP-011 §17`, `ADR-VPR-001..005` |
| **Constraints Injected** | C1 effective-dating predicate; C2 no ownership gate; C3 no commission/band internals; C4 existence-before-empty-list ordering; C5 layering/package; C6 opaque `expertPriceId` selector |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Bắt buộc điền trước khi viết test. Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS heading (`3_Functional_Specification.md` line 5117) reads "UC-241 View Expert Consultatioểm ie Pricing" — a title typo/mojibake | Normalized in TDS §1 footnote to "View Expert Consultation Pricing", consistent with §3.3.20.1 heading (line 5115) and Table 260 caption (line 5134). No requirement content changes. | No test asserts the literal typo string; all test names/comments use the normalized title. This row exists purely for traceability, per CareBridge documentation policy 4.4 (immutable history). |
| L2 | SRS does not state whether pricing is ownership-scoped like sibling read UCs (UC-202/UC-203) | `ADR-VPR-002`: this is a **public-within-app catalog read** — any authenticated user may view any expert's prices; there is deliberately **no** `assertCanView`/ownership predicate (contrast with UC-203 `ADR-CDT-001` IDOR guard) | `VIEWPR-TC-008` asserts a user with **no relationship** to the expert gets `200`, not `403`. A companion negative test (`VIEWPR-TC-008b`) asserts no ownership-policy class is invoked. |
| L3 | Naive read of `expert_consultation_prices` (`findByExpertProfileId`) would return superseded/future/expired rows, exposing prices not actually in force | `ADR-VPR-001`: effective-dating predicate `status='ACTIVE' AND effective_from<=now AND (effective_to IS NULL OR effective_to>now)` mechanized in the repository `@Query`, using `expert_consultation_prices` columns (`V1__init_schema.sql` 859–874) | `VIEWPR-TC-003`/`VIEWPR-TC-004`/`VIEWPR-TC-005` seed non-effective rows and assert they are excluded |
| L4 | `expert_consultation_prices.price_band_id` FKs to `consultation_price_bands` which carries `commission_rate`/`minimum_price`/`maximum_price` (`V1__init_schema.sql` 842–857) — a naive DTO built by joining or reusing a shared mapper could leak these | `ADR-VPR-003`: response DTO built field-by-field from the price entity only; commission/band internals never serialized | `VIEWPR-TC-006` is an explicit **negative-assertion** test on the serialized JSON |
| L5 | SRS AF2 says "empty state" for no matching data, but does not specify HTTP status | `ADR-VPR-004`: expert-with-no-prices is `200 []`; only unknown `expertProfileId` is `404 VIEWPR-003`. Existence check runs **before** the price query. | `VIEWPR-TC-002` (empty, 200) vs `VIEWPR-TC-007` (not found, 404) are separate, non-conflating tests |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` in the test basis for schema facts (§842–874).

### TDS-01 — Scope / Phạm vi

```
ViewExpertConsultationPricing bao gồm các layer:
├── Service (mock Repository với Mockito) — effective-dating + existence-check logic
├── Mapper (unit) — negative-assertion on DTO field set
├── Controller (mock Service với @WebMvcTest) — auth/binding only
└── Integration (Testcontainers PostgreSQL với @SpringBootTest) — real effective-dating query
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-241` (Table 260) | Happy path, AF2 empty state, E1 unauthenticated, E2 not-found/malformed-id |
| `ADR-VPR-001` | Effective-dating predicate correctness (future/expired/non-ACTIVE excluded) |
| `ADR-VPR-002` | Non-ownership-gated access (any authenticated role) |
| `ADR-VPR-003` | Commission/band-internals exclusion (negative assertion) |
| `ADR-VPR-004` | Empty-list-is-valid-not-error vs. expert-not-found distinction |
| `ADR-VPR-005` | `expertPriceId` opaque selector present |
| `CB-CON-IMP-011 §8/§9/§10` | Interface contract, response schema, error codes `VIEWPR-001..004` |
| `CB-204` / `CB-205` mockups | Expected field values (channel/duration/price/cancellation policy) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Multiple effective prices, ordered | `ViewExpertPricingService.getEffectivePrices()` | `VIEWPR-TC-001` |
| TC-COND-002 | Expert exists, no effective prices | `ViewExpertPricingService.getEffectivePrices()` | `VIEWPR-TC-002` |
| TC-COND-003 | Future-dated price excluded | `IExpertConsultationPriceRepository.findEffectivePrices()` | `VIEWPR-TC-003` |
| TC-COND-004 | Expired price excluded | `IExpertConsultationPriceRepository.findEffectivePrices()` | `VIEWPR-TC-004` |
| TC-COND-005 | Non-`ACTIVE` status excluded | `IExpertConsultationPriceRepository.findEffectivePrices()` | `VIEWPR-TC-005` |
| TC-COND-006 | Commission/band internals never in response | `ExpertConsultationPriceMapper.toItem()` | `VIEWPR-TC-006` |
| TC-COND-007 | Unknown expertProfileId | `ViewExpertPricingService.getEffectivePrices()` | `VIEWPR-TC-007` |
| TC-COND-008 | Non-owner/no-relationship access allowed (contrast UC-203) | `ViewExpertPricingController` | `VIEWPR-TC-008`, `VIEWPR-TC-008b` |
| TC-COND-009 | Unauthenticated request | `ViewExpertPricingController` (security filter) | `VIEWPR-TC-009` |
| TC-COND-010 | Malformed `expertProfileId` | `ViewExpertPricingController` | `VIEWPR-TC-010` |
| TC-COND-011 | `expertPriceId` present as opaque UC-75 selector | `ExpertConsultationPriceMapper.toItem()` | `VIEWPR-TC-011` |
| TC-COND-012 | Full end-to-end flow against real DB | Integration | `VIEWPR-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | expert exists-with-prices / exists-no-prices / not-exists | Three distinct response classes (200 list / 200 empty / 404) |
| Boundary Value Analysis | `effective_from`/`effective_to` exactly at `now` | `V1__init_schema.sql`: `effective_from <= now` (inclusive), `effective_to > now` (exclusive) — off-by-one risk at the boundary |
| State Transition (n/a — read-only) | — | No state machine; excluded per TDS-01 |
| Error Guessing | Ownership-gate regression (a future refactor "helpfully" adding `assertCanView`), commission-leak via join | Highest-risk regression vectors for this specific UC given sibling patterns |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `expert_consultation_prices` row: `status='ACTIVE'`, `effective_from=now-P1D`, `effective_to=NULL`, `channel_type='CHAT'`, `duration_minutes=30`, `price_amount=200000` | Happy path (in-force) |
| `FX-002` | DB seed | Same expert, `channel_type='VOICE'`, `duration_minutes=45`, `price_amount=350000` | Happy path (ordering) |
| `FX-003` | DB seed | Same expert, `channel_type='VIDEO'`, `duration_minutes=60`, `price_amount=500000` | Happy path (ordering) |
| `FX-004` | DB seed | `effective_from = now + P1D` (future-dated), otherwise like FX-001 | Effective-dating exclusion (future) |
| `FX-005` | DB seed | `effective_to = now - P1D` (expired), otherwise like FX-001 | Effective-dating exclusion (expired) |
| `FX-006` | DB seed | `status='SUPERSEDED'`, effective window otherwise in-force | Effective-dating exclusion (non-ACTIVE) |
| `FX-007` | DB seed | `consultation_price_bands` row: `commission_rate=0.15`, `minimum_price=100000`, `maximum_price=600000`, FK'd from FX-001..003 | Commission/band negative-assertion source |
| `FX-008` | DB seed | `expert_profiles` row with no `expert_consultation_prices` rows at all | AF2 empty-state |
| `FX-009` | JWT | `{ sub: 'user-no-relationship-001', role: 'ROLE_FAMILY' }` — no booking/relationship with the seeded expert | Non-ownership-gated access test |
| `FX-010` | JWT | none / expired token | Unauthenticated test |

---

## 4. Test Case Specification

> **TC ID format:** `VIEWPR-TC-[NNN]`

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// ExpertConsultationPriceTestFactory.java
class ExpertConsultationPriceTestFactory {

    // Baseline in-force price row — synced with FX-001 (§3 TDS-05)
    static ExpertConsultationPriceEntity makePrice(UUID expertProfileId, UUID priceBandId) {
        ExpertConsultationPriceEntity p = new ExpertConsultationPriceEntity();
        p.setExpertPriceId(UUID.randomUUID());
        p.setExpertProfileId(expertProfileId);
        p.setPriceBandId(priceBandId);
        p.setChannelType("CHAT");
        p.setDurationMinutes((short) 30);
        p.setPriceAmount(new BigDecimal("200000"));
        p.setCurrency("VND");
        p.setCancellationPolicy("Free cancellation up to 2 hours before the session.");
        p.setEffectiveFrom(Instant.now().minus(Duration.ofDays(1)));
        p.setEffectiveTo(null);
        p.setStatus("ACTIVE");
        p.setVersionNo(1);
        return p;
    }

    // Overload to override specific fields (e.g. effective window, status)
    static ExpertConsultationPriceEntity makePrice(UUID expertProfileId, UUID priceBandId,
                                                    Consumer<ExpertConsultationPriceEntity> overrides) {
        ExpertConsultationPriceEntity p = makePrice(expertProfileId, priceBandId);
        overrides.accept(p);
        return p;
    }

    // Price band carrying the confidential internals (FX-007) — never expected in the response
    static ConsultationPriceBandEntity makePriceBand() {
        ConsultationPriceBandEntity band = new ConsultationPriceBandEntity();
        band.setPriceBandId(UUID.randomUUID());
        band.setChannelType("CHAT");
        band.setDurationMinutes((short) 30);
        band.setMinimumPrice(new BigDecimal("100000"));
        band.setMaximumPrice(new BigDecimal("600000"));
        band.setCommissionRate(new BigDecimal("0.15"));
        band.setCurrency("VND");
        band.setEffectiveFrom(Instant.now().minus(Duration.ofDays(30)));
        band.setStatus("ACTIVE");
        return band;
    }

    static UUID makeExpertProfileId() {
        return UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    }
}
```

---

### VIEWPR-TC-001 — Happy path: multiple effective prices returned, ordered

**Severity:** `CRITICAL`
**Feature Under Test:** `ViewExpertPricingService.getEffectivePrices()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ViewExpertPricingServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-VPR-001` (effective-dating + ordering); CB-204 mockup (Chat 30' 200.000đ, Cuộc gọi 45' 350.000đ, Video 60' 500.000đ)

**Preconditions:**
- Expert `EXP-001` exists (`expert_profiles`)
- Seed FX-001, FX-002, FX-003 (all in-force, same expert)

**Test Steps:**
1. Arrange: seed 3 in-force price rows via `ExpertConsultationPriceTestFactory.makePrice(...)` with overrides for channel/duration/price to match FX-001..003
2. Act: call `getEffectivePrices(EXP-001)`
3. Assert: result has exactly 3 items, ordered `CHAT(30) → VOICE(45) → VIDEO(60)`, each item's `priceAmount`/`currency`/`channelType`/`durationMinutes`/`cancellationPolicy` match the seeded row

**Expected Result (PASS — hành vi đúng):**
- List size == 3, order exactly `[CHAT/30/200000, VOICE/45/350000, VIDEO/60/500000]`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Wrong count, wrong order, or field values not matching seed

**Current Status:** 🔴 Not written
**Implementation Note:** Repository query must `ORDER BY channel_type, duration_minutes` (ADR-VPR-001).

---

### VIEWPR-TC-002 — Happy path: empty list when expert exists but has no effective prices

**Severity:** `HIGH`
**Feature Under Test:** `ViewExpertPricingService.getEffectivePrices()`
**Test File:** `ViewExpertPricingServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-VPR-004` (SRS UC-241 AF2 empty-state)

**Preconditions:**
- Expert `EXP-002` exists (FX-008), zero `expert_consultation_prices` rows

**Test Steps:**
1. Arrange: seed expert with no price rows
2. Act: call `getEffectivePrices(EXP-002)`
3. Assert: result is an empty `List` (not `null`, no exception thrown)

**Expected Result (PASS):**
- `result.isEmpty() == true`; no exception; HTTP layer would map this to `200 []`

**Expected Result (FAIL):**
- `NotFoundException` thrown for an existing expert with no prices, or `null` returned

**Current Status:** 🔴 Not written
**Implementation Note:** Empty is a valid terminal result of step 3 in ADR-VPR-004's decision order — must not throw.

---

### VIEWPR-TC-003 — Effective-dating filter excludes future-dated price

**Severity:** `CRITICAL`
**Feature Under Test:** `IExpertConsultationPriceRepository.findEffectivePrices()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/repository/ExpertConsultationPriceRepositoryIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-VPR-001`; `V1__init_schema.sql` line 868 (`effective_from timestamptz NOT NULL`)

**Preconditions:**
- Expert `EXP-003`; seed FX-001 (in-force) + FX-004 (`effective_from = now + P1D`)

**Test Steps:**
1. Arrange: seed 1 in-force row and 1 future-dated row for the same expert
2. Act: call `findEffectivePrices(EXP-003, Instant.now())`
3. Assert: result contains exactly the in-force row; the future-dated row is absent

**Expected Result (PASS):** size == 1, matches the in-force row's `expertPriceId`
**Expected Result (FAIL):** size == 2 (future-dated leaked) or size == 0

**Current Status:** 🔴 Not written
**Implementation Note:** Uses a Testcontainers PostgreSQL instance (Integration Test category, §TDS-01).

---

### VIEWPR-TC-004 — Effective-dating filter excludes expired price

**Severity:** `CRITICAL`
**Feature Under Test:** `IExpertConsultationPriceRepository.findEffectivePrices()`
**Test File:** `ExpertConsultationPriceRepositoryIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-VPR-001`; `V1__init_schema.sql` line 869 (`effective_to timestamptz` nullable)

**Preconditions:**
- Expert `EXP-004`; seed FX-001 (in-force) + FX-005 (`effective_to = now - P1D`)

**Test Steps:**
1. Arrange: seed 1 in-force row and 1 expired row
2. Act: call `findEffectivePrices(EXP-004, Instant.now())`
3. Assert: only the in-force row is returned

**Expected Result (PASS):** size == 1, expired row absent
**Expected Result (FAIL):** expired row present in result

**Current Status:** 🔴 Not written
**Implementation Note:** Boundary case (TDS-04 BVA): also verify a row with `effective_to == now` exactly is excluded (`effective_to > :now` is strict).

---

### VIEWPR-TC-005 — Effective-dating filter excludes non-`ACTIVE` status

**Severity:** `HIGH`
**Feature Under Test:** `IExpertConsultationPriceRepository.findEffectivePrices()`
**Test File:** `ExpertConsultationPriceRepositoryIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-VPR-001`; `V1__init_schema.sql` line 870 (`status varchar(20) NOT NULL DEFAULT 'ACTIVE'`)

**Preconditions:**
- Expert `EXP-005`; seed FX-006 (`status='SUPERSEDED'`, otherwise in an in-force window)

**Test Steps:**
1. Arrange: seed a `SUPERSEDED` row whose effective window would otherwise qualify
2. Act: call `findEffectivePrices(EXP-005, Instant.now())`
3. Assert: result is empty

**Expected Result (PASS):** size == 0
**Expected Result (FAIL):** the `SUPERSEDED` row is returned

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-006 — Commission rate and price-band internals never exposed (negative assertion)

**Severity:** `CRITICAL`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies` (business-confidentiality analog)
**Legal:** `BR-PRIVACY (business-confidentiality)` — `ADR-VPR-003`
**Feature Under Test:** `ExpertConsultationPriceMapper.toItem()`, full serialized HTTP response
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ExpertConsultationPriceMapperTest.java` (unit) + `ViewExpertPricingControllerIT.java` (serialization)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-VPR-003`; `V1__init_schema.sql` lines 850 (`commission_rate`), 848–849 (`minimum_price`/`maximum_price`), 862 (`price_band_id`), 871 (`version_no`)

**Preconditions:**
- FX-001..003 seeded with FX-007 price band (`commission_rate=0.15`, `minimum_price=100000`, `maximum_price=600000`)

**Test Steps:**
1. Arrange: seed prices linked to a price band with known commission/min/max values
2. Act: call `getEffectivePrices(...)` end-to-end and serialize the response with `ObjectMapper`
3. Assert (mapper unit level): `ExpertPriceItemResponse` has no `commissionRate`/`minimumPrice`/`maximumPrice`/`priceBandId`/`versionNo` field at all (compile-time check via reflection/field list)
4. Assert (serialization level): JSON string does **not** contain `"commission"`, `"minimum_price"`, `"maximum_price"`, `"priceBandId"`, `"price_band_id"`, `"version"`

```java
String json = objectMapper.writeValueAsString(result);
assertThat(json).doesNotContainIgnoringCase("commission");
assertThat(json).doesNotContainIgnoringCase("minimum_price");
assertThat(json).doesNotContainIgnoringCase("maximum_price");
assertThat(json).doesNotContainIgnoringCase("priceBandId");
assertThat(json).doesNotContainIgnoringCase("price_band_id");
assertThat(json).doesNotContainIgnoringCase("version");
```

**Expected Result (PASS — hệ thống an toàn):**
- Reflection shows exactly 6 fields on `ExpertPriceItemResponse` (`expertPriceId`, `channelType`, `durationMinutes`, `priceAmount`, `currency`, `cancellationPolicy`); JSON assertions all pass

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Any commission/band-internal substring present in the serialized JSON, or mapper joins to `consultation_price_bands`

**Current Status:** 🔴 Not written
**Implementation Note:** This is the highest-priority test in this batch — directly enforces ADR-VPR-003.

---

### VIEWPR-TC-007 — Expert not found → 404 VIEWPR-003

**Severity:** `HIGH`
**Feature Under Test:** `ViewExpertPricingService.getEffectivePrices()` / Controller mapping
**Test File:** `ViewExpertPricingServiceTest.java`, `ViewExpertPricingControllerIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-VPR-004`; TDS §10 (`VIEWPR-003`)

**Preconditions:**
- `expertProfileId` = random UUID with no `expert_profiles` row

**Test Steps:**
1. Act: call `getEffectivePrices(randomUuid)`
2. Assert: `NotFoundException` thrown before any price query executes (verify via Mockito `verifyNoInteractions(priceRepo)`)
3. Controller level: HTTP response is `404` with body `{"error":{"code":"VIEWPR-003", ...}}`

**Expected Result (PASS):** 404, code `VIEWPR-003`, price repository never queried
**Expected Result (FAIL):** 200 with empty list (conflates not-found with empty — violates ADR-VPR-004), or 500

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-008 — Non-owner / no-relationship user still gets 200 (non-ownership-gated read)

**Severity:** `CRITICAL`
**CWE:** `CWE-284 — Improper Access Control` (verifying the *absence* of an incorrect restriction — inverse of typical IDOR test)
**Legal:** `ADR-VPR-002` / `BR-RBAC`
**Feature Under Test:** `ViewExpertPricingController` (full auth chain)
**Test File:** `ViewExpertPricingControllerIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-VPR-002` (explicit contrast with `ADR-CDT-001` in UC-203)

**Preconditions:**
- FX-009 JWT: authenticated user with **no** booking/relationship to `EXP-001`
- `EXP-001` has effective prices seeded (FX-001..003)

**Test Steps (Attack-shaped but expecting success — verifying the design intent, not a vulnerability):**
1. Authenticate as `FX-009` (`ROLE_FAMILY`, `user-no-relationship-001`)
2. Call `GET /api/v1/experts/EXP-001/consultation-prices`
3. Assert response status is `200`, body contains the 3 seeded items

**Expected Result (PASS = design intent correct):** `200 OK` with full price list — this is the **correct** behavior per ADR-VPR-002 (pricing is catalog info, not owned data)
**Expected Result (FAIL = design intent broken):** `403 Forbidden` — indicates someone has incorrectly added an ownership/participant gate (regression against ADR-VPR-002)

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-008b — No ownership-policy class is invoked on this path

**Severity:** `HIGH`
**Feature Under Test:** `ViewExpertPricingService` (structural check)
**Test File:** `ViewExpertPricingServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-VPR-002` / TDS §5.2 (no `policy` class listed for this feature's planned file paths)

**Test Steps:**
1. Static/reflection check: `ViewExpertPricingService` has no field of a `*Policy` type and no call to any `assertCanView`-style method
2. (Optional, if a shared policy interceptor exists elsewhere) Mockito `verifyNoInteractions()` on any injected ownership-policy mock

**Expected Result (PASS):** No policy dependency present; service only depends on the two repositories + mapper (§8, TDS §5.2)
**Expected Result (FAIL):** A `ConsultationParticipantPolicy`-style dependency has been introduced (AP-AI-003 — Implicit Decision, copying UC-203's pattern where it does not apply)

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-009 — Unauthenticated request → 401 VIEWPR-002

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** SRS UC-241 E1 / `PRE-3`
**Feature Under Test:** Security filter chain in front of `ViewExpertPricingController`
**Test File:** `ViewExpertPricingControllerIT.java`
**TDD Phase:** 🔴 RED

**Preconditions:** No `Authorization` header (or expired/invalid JWT — FX-010)

**Test Steps (Attack Simulation):**
1. Call `GET /api/v1/experts/{expertProfileId}/consultation-prices` with no JWT
2. Repeat with an expired/invalid JWT

**Expected Result (PASS = hệ thống an toàn):** `401` with code `VIEWPR-002` in both cases
**Expected Result (FAIL = lỗ hổng tồn tại):** `200` with data returned to an unauthenticated caller

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-010 — Malformed expertProfileId → 400 VIEWPR-001

**Severity:** `MEDIUM`
**Feature Under Test:** `ViewExpertPricingController` path-variable binding
**Test File:** `ViewExpertPricingControllerIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** TDS §10 (`VIEWPR-001`)

**Test Steps:**
1. Call `GET /api/v1/experts/not-a-uuid/consultation-prices`

**Expected Result (PASS):** `400` with code `VIEWPR-001`
**Expected Result (FAIL):** `500` (unhandled binding exception) or the request reaching the service layer

**Current Status:** 🔴 Not written

---

### VIEWPR-TC-011 — `expertPriceId` present as opaque selector for UC-75

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertConsultationPriceMapper.toItem()`
**Test File:** `ExpertConsultationPriceMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-VPR-005`

**Test Steps:**
1. Map a seeded `ExpertConsultationPriceEntity` (known `expertPriceId`)
2. Assert `ExpertPriceItemResponse.expertPriceId` equals the entity's PK exactly

**Expected Result (PASS):** field present and equal to the source PK
**Expected Result (FAIL):** field missing, null, or a different generated value

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### VIEWPR-TC-INT-001 — Full flow: seed → GET → verify DB-backed effective list

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/experts/{expertProfileId}/consultation-prices` end-to-end
**Test File:** `src/test/java/com/carebridge/backend/consultation/ViewExpertConsultationPricingIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: expert `EXP-001`, price band FX-007, prices FX-001 (in-force), FX-004 (future), FX-005 (expired), FX-006 (superseded)

**Test Steps:**
1. Seed the above fixtures via JPA
2. Authenticate as any valid role (e.g. `mother@carebridge.dev` fixture JWT)
3. `GET /api/v1/experts/EXP-001/consultation-prices`
4. Assert DB state unchanged (read-only) and response body

**Expected Result (PASS):**
- HTTP 200; body contains exactly the FX-001 item; FX-004/005/006 absent; no commission/band fields present
- `SELECT count(*) FROM expert_consultation_prices` unchanged before/after (no mutation)

**Expected Result (FAIL):**
- Any excluded fixture present in the body, or a write occurred

**DB Assertion:**
```java
long countBefore = priceRepo.count();
// ... call endpoint ...
long countAfter = priceRepo.count();
assertThat(countAfter).isEqualTo(countBefore); // read-only guarantee
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VIEWPR-TC-001` | `ViewExpertPricingServiceTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-002` | `ViewExpertPricingServiceTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-003` | `ExpertConsultationPriceRepositoryIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-004` | `ExpertConsultationPriceRepositoryIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-005` | `ExpertConsultationPriceRepositoryIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-006` | `ExpertConsultationPriceMapperTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-007` | `ViewExpertPricingServiceTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-008` | `ViewExpertPricingControllerIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-008b` | `ViewExpertPricingServiceTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-009` | `ViewExpertPricingControllerIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-010` | `ViewExpertPricingControllerIT.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-011` | `ExpertConsultationPriceMapperTest.java:__` | `[ ]` | `___` | |
| `VIEWPR-TC-INT-001` | `ViewExpertConsultationPricingIntegrationTest.java:__` | `[ ]` | `___` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ViewExpertPricingService implements IViewExpertPricingService {

    @Override
    public List<ExpertPriceItemResponse> getEffectivePrices(UUID expertProfileId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// ExpertConsultationPriceMapper stub
@Component
public class ExpertConsultationPriceMapper {
    public ExpertPriceItemResponse toItem(ExpertConsultationPriceEntity entity) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Repository — no stub needed (interface method with no default body); Testcontainers IT
// will fail at context load / query-not-implemented until the @Query is added.
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VIEWPR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `VIEWPR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-003` | query not implemented | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-004` | query not implemented | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-005` | query not implemented | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-008b` | no service exists yet | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-009` | endpoint not wired | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-010` | endpoint not wired | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIEWPR-TC-INT-001` | endpoint not wired | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(Open — to be attached when Red Gate is actually run)*

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CON-IMP-011` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] `expert_consultation_prices` / `consultation_price_bands` / `expert_profiles` present in applied schema (they are — no migration needed, TDS §11.2)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ViewExpertPricingService` và `ExpertConsultationPriceMapper`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có commission/band-internal field xuất hiện trong logs hoặc response (VIEWPR-TC-006)
- [ ] `VIEWPR-TC-008`/`008b` pass — confirms non-ownership-gated design is intact (no regression toward UC-203's pattern)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mỗi test dùng `ExpertConsultationPriceTestFactory.makePrice(...)`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-VPR-00x / TDS §8-10 / CB-204 / CB-205)

### Suspension Criteria (Điều kiện tạm dừng)

- Blocker dependency chưa sẵn sàng (e.g. `ExpertConsultationPriceEntity` not yet created by UC-238's write-side implementation)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review (e.g. if index on `expert_consultation_prices(expert_profile_id)` proves necessary for the SLA — currently `Open`, see TDS §5.3)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No migration to revert (read-only feature, no schema change — TDS §11.2).

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/consultation/controller/ViewExpertPricingController.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/IViewExpertPricingService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/ViewExpertPricingService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/mapper/ExpertConsultationPriceMapper.java
git checkout -- src/main/java/com/carebridge/backend/consultation/dto/response/ExpertPriceItemResponse.java
git checkout -- src/main/java/com/carebridge/backend/consultation/repository/IExpertConsultationPriceRepository.java
git checkout -- src/test/java/com/carebridge/backend/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-VPR-00x nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | `VIEWPR-TC-008`/`008b` catches an ownership-policy class copied from UC-203 without an ADR authorizing it | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller doing effective-dating filtering itself (business logic in controller) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a `ConsultationPriceBandResponse`-style type or a `commissionRate` field not in TDS §8 | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | *(none detected during spec authoring)* | — | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol. Status: Draft.*
