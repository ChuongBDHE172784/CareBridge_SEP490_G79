# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-240 — Configure Consultation Price Bands — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-240`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] N/A — no subject PII (see TDS §1)`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L842-860) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.8.1 (Table 102, UC-240)
- `04_Implement/UC240_ConfigureConsultationPriceBands/UC240_ConfigureConsultationPriceBands_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `03_Design/UI_UX/WebAppScreen/CB-216 Consultation Price Bands (UC-240)/code.html` — list oracle
- `03_Design/UI_UX/WebAppScreen/CB-217 Configure Consultation Price Band (UC-240)/code.html` — configure-form oracle
- `03_Design/UI_UX/WebAppScreen/CB-218 Deactivate Price Band Confirmation (UC-240)/code.html` — deactivation-confirmation oracle
- `04_Implement/UC143_RespondToConsultationRequest/UC143_RespondToConsultationRequest_TDS.md` — `channelType` literal set oracle (`CHAT`/`VOICE_CALL`/`VIDEO_CALL`)
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data (module has no subject PII regardless).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC-240 |

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
| **Feature / Gap ID** | `UC-240` |
| **Module** | `Consultation — Configure Price Bands (Admin)` |
| **Spec gốc** | `CB-CONSULTATION-IMP-240` (UC-240 TDS) |
| **Priority** | 🟡 Medium (SRS Table 102) |
| **Sprint** | Consultation Pricing batch UC-238→UC-241 — TV4-Lâm |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Internal` (no subject PII — pricing/commission policy configuration only) |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | Identity/RBAC (`SYSTEM_ADMIN` role resolution) — platform baseline |
| **Downstream Consumers** | **UC-238 Set Consultation Price**, **UC-239 Update Consultation Price** (both consume `ConsultationPriceBandCreated`/`ConsultationPriceBandDeactivated` events / read `price_band_id` to bound Expert pricing and inherit `commission_rate`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC-240 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (Admin-only RBAC), C2 (overlap rejection + null-specialty-universal), C3 (deactivation non-cascade), C4 (commission sole-source-of-truth / no cross-write), C5 (package layout + app-level validation, no new migration) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Table 102 names VNPay as a Secondary Actor for the whole "Consultation Pricing & Commission" group, which could mislead an implementer into wiring a VNPay call into UC-240 | `UC-240 TDS §1.3` clarifies UC-240 never calls VNPay — it only stores the commission policy that later governs how VNPay-settled payments split | Tests assert **zero interactions** with any VNPay/gateway client across every UC-240 service method (`PBAND-TC-018`) |
| L2 | CB-216 mockup displays a "Phiên bản" (version, e.g. `v2.1`) column | `consultation_price_bands` (`V1__init_schema.sql` L842-859) has **no version column**; `UC-240 TDS §5.3` flags this as a schema-vs-mockup discrepancy with no schema change planned | Tests never assert a `version`/`versionNo` field on `PriceBandResponse`/`PriceBandListItemResponse` — its absence is the correct behavior, not a gap |
| L3 | Neither SRS nor the schema originally stated whether `commission_rate` is stored as a whole-number percent (`15`) or a fraction (`0.15`); no sample value was found anywhere else in the repo at the time (checked UC75/UC126/UC143/UC204 — none cite a commission_rate example) | `UC-240 TDS ADR-PBAND-006` sharpened this into a crisp question (percent vs fraction); **RESOLVED 2026-07-04** — user/product-owner accepted the fraction representation, range `[0.0, 0.5]`, e.g. `0.15` = 15% | Tests assert the concrete `[0.0, 0.5]` fraction bound directly (`PBAND-TC-005` rejects `0.6`/negative, `PBAND-TC-006` accepts `0.0`/`0.15`/`0.5`) — no longer a config-parameterized placeholder; representation is now firm |
| L4 | Ambiguity risk: an implementer could design "does band A overlap band B" to also decide specialty-precedence for lookup (which specific-vs-universal band UC-238 should prefer) | `ADR-PBAND-003` (`UC-240 TDS §3`) scopes UC-240's responsibility to only the NULL-means-universal semantics for its own overlap guard; specialty **precedence** in lookup is UC-238's concern and is now resolved there (`UC238_SetConsultationPrice_TDS.md` ADR-SETPR-006, `Accepted` — most-specific-match-wins) | Tests for UC-240 assert only that a universal (`NULL`) band and any specialty-specific band on the same channel/duration are treated as overlapping (`PBAND-TC-007`), and that two *different* non-null specialties on the same channel/duration do NOT overlap (`PBAND-TC-008`) — no precedence/lookup-ordering assertion is made here (correctly out of this module's test scope) |
| L5 | No DB CHECK constraint exists on `channel_type`/`status`/prices/`commission_rate` (verified — same posture as prior consultation modules) | `V1__init_schema.sql` L842-859 confirms free `varchar`/`numeric` columns, no CHECK; taxonomy and range enforced app-level only per `ADR-PBAND-006` | Tests assert app-level rejection of invalid `channelType` enum values and out-of-range prices/commission (`PBAND-TC-003/004/005/019`) |
| L6 | Risk that an implementer conflates "deactivate a band" with "cascade-delete/null the FK on dependent rows", given `expert_consultation_prices.price_band_id` is `NOT NULL` | `ADR-PBAND-002` (`UC-240 TDS §3`) — confirmed, CB-218-sourced decision: deactivation is status-only; existing Expert prices/bookings are explicitly unaffected | Tests seed an Expert price + a booking referencing the band, deactivate the band, then assert both rows are byte-for-byte unchanged and still resolvable by FK (`PBAND-TC-010`, `PBAND-TC-011`) — this is the release-blocking, single highest-risk regression this module could introduce |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Configure Price Bands (Admin) module bao gồm các layer:
├── Domain (PriceBandPolicy — pure logic, no deps)
├── Services (PriceBandService — mock JPA Repository với Mockito)
├── Mapper (PriceBandMapper — request/entity/DTO projection, pure logic)
├── Controller (PriceBandController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest — NO WireMock/VNPay,
    since UC-240 never calls the payment gateway — TDS §1.3 / L1 above)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-240 (§3.2.8.1, Table 102) | Actor (System Admin only), trigger, description ("manages platform price limits and commission rates"), exceptions E1-E3 |
| `UC-240 TDS` ADR-PBAND-001..006 | Overlap rejection, non-cascading deactivation, null-specialty-universal, Admin-only gate, commission sole-source-of-truth, range validation |
| `V1__init_schema.sql` L842-860 | Column names/types/defaults/nullability as persistence oracle; `expert_consultation_prices.price_band_id` NOT NULL as the non-cascade evidence |
| `BR-RBAC` / `BR-CONSULTATION` | Admin-only authorization, auditable pricing lifecycle |
| CB-217 mockup | Channel radios (Video/Voice/Chat), duration options (15/30/45/60), min/max price fields, commission slider (0-50, default 15) |
| CB-218 mockup | Deactivation confirmation copy — "prices already set and bookings already made will NOT be affected" |
| `UC-240 TDS §9-10` | API contract, error codes (`PBAND-0xx`) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy — create ACTIVE band, commission set | `PriceBandService.createBand()` | `PBAND-TC-001` |
| TC-COND-002 | Overlapping ACTIVE band rejected | `PriceBandPolicy.assertNoActiveOverlap()` | `PBAND-TC-002` |
| TC-COND-003 | Invalid range (min ≥ max) rejected | `PriceBandPolicy.assertValidRange()` | `PBAND-TC-003` |
| TC-COND-004 | Negative price rejected (boundary) | `PriceBandPolicy.assertValidRange()` | `PBAND-TC-004` |
| TC-COND-005 | Commission out of range rejected | `PriceBandPolicy.assertValidRange()` | `PBAND-TC-005` |
| TC-COND-006 | Commission within configured bound accepted | `PriceBandPolicy.assertValidRange()` | `PBAND-TC-006` |
| TC-COND-007 | `specialtyScope = null` treated as universal in overlap logic | `PriceBandPolicy.assertNoActiveOverlap()` / `isUniversal()` | `PBAND-TC-007` |
| TC-COND-008 | Specialty-specific band does not overlap a different specialty | `PriceBandPolicy.assertNoActiveOverlap()` | `PBAND-TC-008` |
| TC-COND-009 | Happy — deactivate band (status-only) | `PriceBandService.deactivateBand()` | `PBAND-TC-009` |
| TC-COND-010 | Deactivated band not selectable for NEW Expert prices (cross-cutting) | `PriceBandService.deactivateBand()` + downstream lookup contract | `PBAND-TC-010` |
| TC-COND-011 | Deactivation does NOT affect existing Expert prices/bookings | `PriceBandService.deactivateBand()` | `PBAND-TC-011` |
| TC-COND-012 | Non-admin (Expert/Partner) denied | `PriceBandPolicy.assertAdmin()` | `PBAND-TC-012` |
| TC-COND-013 | Unauthenticated request | `PriceBandController` security filter | `PBAND-TC-013` |
| TC-COND-014 | Deactivate a non-existent band | `PriceBandService.deactivateBand()` | `PBAND-TC-014` |
| TC-COND-015 | Deactivate an already-deactivated band | `PriceBandPolicy.assertDeactivatable()` | `PBAND-TC-015` |
| TC-COND-016 | `ConsultationPriceBandCreated` event payload correctness | `PriceBandService.createBand()` | `PBAND-TC-016` |
| TC-COND-017 | `ConsultationPriceBandDeactivated` event payload correctness | `PriceBandService.deactivateBand()` | `PBAND-TC-017` |
| TC-COND-018 | `commission_rate` written only on the band; zero interaction with Expert-price/booking/VNPay collaborators | `PriceBandService` (all methods) | `PBAND-TC-018` |
| TC-COND-019 | Invalid `channelType`/`durationMinutes` enum rejected | DTO validation (`@Pattern`) | `PBAND-TC-019` |
| TC-COND-020 | E2E — create then deactivate via MockMvc/Testcontainers, DB rows correct, no expert-price/booking write | `PriceBandController` + real DB | `PBAND-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `channelType` valid (`VIDEO_CALL`/`VOICE_CALL`/`CHAT`) vs. invalid classes | Confirms the exact CB-217-sourced literal set is enforced, nothing invented |
| Boundary Value Analysis | `minimumPrice`/`maximumPrice` (0, negative, equal, min<max), `commissionRate` (`-0.01`, `0.0`, `0.15`, `0.5`, `0.6`) | Confirms `ADR-PBAND-006` non-negative + min<max + concrete `[0.0, 0.5]` commission-fraction invariants exactly at the edges |
| State Transition Testing | Band `ACTIVE → DEACTIVATED` (absorbing terminal state) | Validates `ADR-PBAND-002`/state machine (`TDS §6.6`), absorbing-state guard on repeat deactivation |
| Decision Table | Actor role (Admin/Expert/Partner/Guest) × band overlap presence/absence × specialty null/non-null | 201/403/409 branching combinations |
| Error Guessing | Replay deactivation on an already-`DEACTIVATED` band; construct overlapping bands with boundary-adjacent `effective_from`/`effective_to` | Idempotency/edge-of-range attack surface for the overlap guard |
| Negative/Interaction Testing | `verifyNoInteractions` on Expert-price/booking repositories and any VNPay/gateway client across every UC-240 method | Scope-boundary enforcement (`ADR-PBAND-005`, `L1`) — the commission single-writer discipline |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-201` | Request | `CreatePriceBandRequest{channelType='VIDEO_CALL', durationMinutes=30, specialtyScope=null, minimumPrice=200000, maximumPrice=500000, commissionRate=0.15, currency='VND', effectiveFrom=2026-08-01T00:00Z, effectiveTo=null}` | Happy-path create baseline (CB-217 oracle; commissionRate as fraction — ADR-PBAND-006 Accepted) |
| `FX-202` | DB seed | `ConsultationPriceBandEntity{priceBandId=PB1, channelType='VIDEO_CALL', durationMinutes=30, specialtyScope=null, status='ACTIVE', effectiveFrom=2026-07-01T00:00Z, effectiveTo=null}` | Existing ACTIVE band — overlap-rejection source |
| `FX-203` | DB seed | Same key as `FX-202` but `specialtyScope='CARDIOLOGY'`, `status='ACTIVE'` | Specialty-specific band — used to prove a *different* specialty (`'DERMATOLOGY'`) does not overlap it |
| `FX-204` | DB seed | `ConsultationPriceBandEntity{priceBandId=PB2, status='DEACTIVATED', ...}` | Already-deactivated band — re-deactivate / not-selectable tests |
| `FX-205` | JWT | `{sub: 'ADM1', role: 'SYSTEM_ADMIN'}` | Auth context — admin |
| `FX-206` | JWT | `{sub: 'EXP1', role: 'EXPERT'}` | Auth context — non-admin (Expert) |
| `FX-207` | JWT | *(absent — no Authorization header)* | Unauthenticated request |
| `FX-208` | DB seed | `ExpertConsultationPriceEntity{expertPriceId=EP1, priceBandId=PB1, priceAmount=350000, status='ACTIVE'}` | Non-cascade evidence — must survive `PB1` deactivation unchanged |
| `FX-209` | DB seed | `ConsultationBookingEntity{bookingId=BK1, expertPriceId=EP1, priceSnapshotAmount=350000, commissionRateSnapshot=0.15, status='COMPLETED'}` | Non-cascade evidence — booking snapshot must survive `PB1` deactivation unchanged (fraction form — ADR-PBAND-006 Accepted) |
| `FX-210` | Config | `pricing.commission.min=0.0`, `pricing.commission.max=0.5` (test profile) | Concrete commission bound (`ADR-PBAND-006` — **Accepted** 2026-07-04, fraction `[0.0, 0.5]`); bound still externalized via config, but the unit/representation is now firm, not a placeholder |
| `FX-211` | Mock spies | `ExpertConsultationPriceRepository` mock, `ConsultationBookingRepository` mock, `VnPayGatewayClient`/generic gateway mock — all asserted via `verifyNoInteractions` | Scope-boundary guard (`PBAND-TC-018`) |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `PriceBandPolicy`, `PriceBandService`, `PriceBandMapper` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest PriceBandController` | ✅ MockMvc full flow | ✅ RBAC / scope-boundary |
| Web | ✅ `ConfigurePriceBandDrawer.tsx` / `PriceBandListPage.tsx` / `DeactivateBandDialog.tsx` unit | — | ✅ component test (form → request mapping per CB-217/218) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

class PriceBandTestFactory {

    static CreatePriceBandRequest makeCreateRequest() {
        CreatePriceBandRequest request = new CreatePriceBandRequest();
        request.setChannelType("VIDEO_CALL");
        request.setDurationMinutes((short) 30);
        request.setSpecialtyScope(null);
        request.setMinimumPrice(new BigDecimal("200000"));
        request.setMaximumPrice(new BigDecimal("500000"));
        request.setCommissionRate(new BigDecimal("0.15")); // fraction, 0.15 = 15% (ADR-PBAND-006, Accepted)
        request.setCurrency("VND");
        request.setEffectiveFrom(Instant.parse("2026-08-01T00:00:00Z"));
        request.setEffectiveTo(null);
        request.setNote("Initial video-call 30-min platform band.");
        return request;
    }

    static CreatePriceBandRequest makeCreateRequest(Consumer<CreatePriceBandRequest> overrides) {
        CreatePriceBandRequest request = makeCreateRequest();
        overrides.accept(request);
        return request;
    }

    static ConsultationPriceBandEntity makeActiveBand() {
        ConsultationPriceBandEntity band = new ConsultationPriceBandEntity();
        band.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        band.setConfiguredBy(UUID.fromString("00000000-0000-0000-0000-00000000ADM1"));
        band.setChannelType("VIDEO_CALL");
        band.setDurationMinutes((short) 30);
        band.setSpecialtyScope(null);
        band.setMinimumPrice(new BigDecimal("200000"));
        band.setMaximumPrice(new BigDecimal("500000"));
        band.setCommissionRate(new BigDecimal("0.15")); // fraction, 0.15 = 15% (ADR-PBAND-006, Accepted)
        band.setCurrency("VND");
        band.setEffectiveFrom(Instant.parse("2026-07-01T00:00:00Z"));
        band.setEffectiveTo(null);
        band.setStatus("ACTIVE");
        return band;
    }

    static ConsultationPriceBandEntity makeActiveBand(Consumer<ConsultationPriceBandEntity> overrides) {
        ConsultationPriceBandEntity band = makeActiveBand();
        overrides.accept(band);
        return band;
    }

    static ConsultationPriceBandEntity makeDeactivatedBand() {
        ConsultationPriceBandEntity band = makeActiveBand(b -> {
            b.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-0000000000B2"));
            b.setStatus("DEACTIVATED");
        });
        return band;
    }

    static DeactivatePriceBandRequest makeDeactivateRequest() {
        DeactivatePriceBandRequest request = new DeactivatePriceBandRequest();
        request.setReason("Superseded by 2026-Q3 pricing update.");
        return request;
    }

    static DeactivatePriceBandRequest makeDeactivateRequest(Consumer<DeactivatePriceBandRequest> overrides) {
        DeactivatePriceBandRequest request = makeDeactivateRequest();
        overrides.accept(request);
        return request;
    }

    // Non-cascade evidence fixtures — must remain byte-for-byte unchanged after band deactivation
    static ExpertConsultationPriceEntity makeExpertPriceUnderBand(UUID priceBandId) {
        ExpertConsultationPriceEntity price = new ExpertConsultationPriceEntity();
        price.setExpertPriceId(UUID.fromString("00000000-0000-0000-0000-0000000000EP1"));
        price.setPriceBandId(priceBandId);
        price.setPriceAmount(new BigDecimal("350000"));
        price.setStatus("ACTIVE");
        return price;
    }

    static ConsultationBookingEntity makeBookingUnderExpertPrice(UUID expertPriceId) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000BK1"));
        booking.setExpertPriceId(expertPriceId);
        booking.setPriceSnapshotAmount(new BigDecimal("350000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.15")); // fraction, 0.15 = 15% (ADR-PBAND-006, Accepted)
        booking.setStatus("COMPLETED");
        return booking;
    }
}
```

---

### PBAND-TC-001 — Happy path: create ACTIVE price band with commission set

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandService.createBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-240 TDS §6.1` sequence diagram + `ADR-PBAND-005` + CB-217 "Lưu cấu hình" happy path

**Preconditions:**
- No existing band shares `FX-201`'s key (no overlap candidates returned).
- `FX-205` JWT for admin `ADM1`.

**Test Steps:**
1. Arrange: mock `priceBandRepository.findByChannelTypeAndDurationMinutesAndStatus("VIDEO_CALL", 30, "ACTIVE")` returns an empty list; mock `save(...)` returns the persisted entity.
2. Act: `priceBandService.createBand(makeCreateRequest(), ADM1)`.
3. Assert: returned DTO `status == "ACTIVE"`; saved entity has `configuredBy=ADM1`, `commissionRate=0.15` (fraction — ADR-PBAND-006, Accepted), `channelType="VIDEO_CALL"`, `durationMinutes=30`.

**Expected Result (PASS):** `201`-equivalent response; band persisted `ACTIVE` with `configuredBy` set to the caller and `commissionRate` from the request.
**Expected Result (FAIL):** Wrong status, `configuredBy` not set, or `commissionRate` silently dropped/defaulted.

**Current Status:** 🔴 Not written

---

### PBAND-TC-002 — Overlapping ACTIVE band rejected → 409 (`PBAND-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Feature Under Test:** `PriceBandPolicy.assertNoActiveOverlap()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-PBAND-001` (`UC-240 TDS §3`), `UC-240 TDS §6.2` error-path sequence diagram, `UC-240 TDS §2 Logic Issues` (this doc) N/A — direct ADR citation

**Preconditions:** `FX-202` existing `ACTIVE` band (`VIDEO_CALL`, 30 min, `specialtyScope=null`, open-ended effective range from `2026-07-01`).

**Test Steps:**
1. Arrange: mock `priceBandRepository.findByChannelTypeAndDurationMinutesAndStatus("VIDEO_CALL", 30, "ACTIVE")` returns `[FX-202]`.
2. Act: `priceBandService.createBand(makeCreateRequest(r -> r.setEffectiveFrom(Instant.parse("2026-08-15T00:00:00Z"))), ADM1)` — a new band whose effective range intersects `FX-202`'s open-ended range.
3. Assert: throws `PriceBandOverlapException` code `PBAND-004`; `priceBandRepository.save()` is **never** called (`verify(priceBandRepository, never()).save(any())`).

**Expected Result (PASS):** Exception thrown; no INSERT occurs; existing `FX-202` band untouched.
**Expected Result (FAIL):** A second overlapping ACTIVE band is silently persisted, creating ambiguity for UC-238's lookup.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense for `ADR-PBAND-001` — must be release-blocking (see §6 Exit Criteria).

---

### PBAND-TC-003 — Invalid range: `minimumPrice >= maximumPrice` rejected → 400 (`PBAND-002`)

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandPolicy.assertValidRange()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-PBAND-006` (`UC-240 TDS §3`), `UC-240 TDS §6.3` error-path sequence diagram

**Test Steps:**
1. Act (equal): `priceBandService.createBand(makeCreateRequest(r -> { r.setMinimumPrice(new BigDecimal("300000")); r.setMaximumPrice(new BigDecimal("300000")); }), ADM1)`.
2. Assert: throws `InvalidPriceRangeException` code `PBAND-002`.
3. Act (inverted): `makeCreateRequest(r -> { r.setMinimumPrice(new BigDecimal("500000")); r.setMaximumPrice(new BigDecimal("200000")); })`.
4. Assert: same exception/code.

**Expected Result (PASS):** Both equal and inverted ranges rejected before any repository access.
**Expected Result (FAIL):** Either case is silently accepted, or only one of the two is caught.

**Current Status:** 🔴 Not written

---

### PBAND-TC-004 — Boundary: negative price rejected → 400 (`PBAND-002`)

**Severity:** `MEDIUM`
**Feature Under Test:** `PriceBandPolicy.assertValidRange()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-PBAND-006` — "both non-negative"

**Test Steps:**
1. Act (negative min): `makeCreateRequest(r -> r.setMinimumPrice(new BigDecimal("-1")))`.
2. Assert: violation/exception `PBAND-002`.
3. Act (zero min, valid): `makeCreateRequest(r -> r.setMinimumPrice(BigDecimal.ZERO))`.
4. Assert: no exception from this rule (zero is the valid boundary — `>= 0`).
5. Act (negative max): `makeCreateRequest(r -> r.setMaximumPrice(new BigDecimal("-1")))`.
6. Assert: violation/exception `PBAND-002`.

**Expected Result (PASS):** Exactly at the `0` boundary is accepted; anything below `0` is rejected for either field.
**Expected Result (FAIL):** Off-by-one on the boundary, or negative values silently accepted.

**Current Status:** 🔴 Not written

---

### PBAND-TC-005 — Commission rate out of `[0.0, 0.5]` fraction range rejected → 400 (`PBAND-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `PriceBandPolicy.assertValidRange()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-PBAND-006` (`UC-240 TDS §3` — commission fraction range **Accepted** 2026-07-04), `FX-210` test config bound `[0.0, 0.5]`

**Preconditions:** `FX-210` — test profile `pricing.commission.min=0.0`, `pricing.commission.max=0.5`.

**Test Steps:**
1. Act (above max): `makeCreateRequest(r -> r.setCommissionRate(new BigDecimal("0.6")))`.
2. Assert: throws `InvalidCommissionException` code `PBAND-003`.
3. Act (negative): `makeCreateRequest(r -> r.setCommissionRate(new BigDecimal("-0.01")))`.
4. Assert: same exception/code.

**Expected Result (PASS):** Values outside `[0.0, 0.5]` rejected (e.g. `0.6` and `-0.01`).
**Expected Result (FAIL):** Out-of-bound commission silently accepted, corrupting the platform's revenue-split policy.

**Current Status:** 🔴 Not written
**Implementation Note:** Bound is read from configuration (not hardcoded) for tunability, but the fraction representation and default cap (`0.5`) are now firm per `ADR-PBAND-006` (Accepted 2026-07-04, resolving `UC-240 TDS §2 Logic Issues L3`).

---

### PBAND-TC-006 — Commission rate within `[0.0, 0.5]` fraction range accepted (boundary: 0.0 / 0.5; default 0.15)

**Severity:** `MEDIUM`
**Feature Under Test:** `PriceBandPolicy.assertValidRange()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** CB-217 commission slider `min=0 max=50` (default `15`), mapped to fraction storage `[0.0, 0.5]` (default `0.15`) per `ADR-PBAND-006` (Accepted 2026-07-04), `FX-210` config bound

**Test Steps:**
1. Act (lower boundary): `makeCreateRequest(r -> r.setCommissionRate(BigDecimal.ZERO))`.
2. Assert: no exception from `assertValidRange`.
3. Act (upper boundary): `makeCreateRequest(r -> r.setCommissionRate(new BigDecimal("0.5")))`.
4. Assert: no exception.
5. Act (platform default): `makeCreateRequest(r -> r.setCommissionRate(new BigDecimal("0.15")))`.
6. Assert: no exception.

**Expected Result (PASS):** `0.0`, `0.15`, and `0.5` all accepted (inclusive bound, matching CB-217's slider `min`/`max`/default mapped to fraction storage).
**Expected Result (FAIL):** Either boundary incorrectly rejected (off-by-one), or the fraction default `0.15` rejected.

**Current Status:** 🔴 Not written

---

### PBAND-TC-007 — `specialtyScope = null` treated as universal in the overlap check

**Severity:** `CRITICAL`
**Feature Under Test:** `PriceBandPolicy.assertNoActiveOverlap()` / `isUniversal()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-PBAND-003` (`UC-240 TDS §3` — "NULL means applies to ALL specialties"), `UC-240 TDS §2 Logic Issues L4`

**Preconditions:** `FX-203` existing `ACTIVE` band, same channel/duration, `specialtyScope='CARDIOLOGY'`.

**Test Steps:**
1. Act: attempt to create a new universal band (`specialtyScope=null`) with the same channel/duration and an overlapping effective range as `FX-203`.
2. Assert: throws `PriceBandOverlapException` code `PBAND-004` — a universal candidate is treated as overlapping **any** specialty-specific existing band on the same channel/duration.

**Expected Result (PASS):** A universal band cannot be created alongside a same-channel/duration specialty-specific band with overlapping dates — prevents an ambiguous "does the universal or the specific band apply" situation for UC-238.
**Expected Result (FAIL):** Universal candidate is incorrectly treated as non-overlapping (e.g. compared via strict string equality against a non-null value), allowing ambiguous coexistence.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense for `ADR-PBAND-003`'s NULL semantics — the only assertion this module makes about specialty scope (precedence/lookup-ordering is explicitly out of scope — `L4`).

---

### PBAND-TC-008 — Specialty-specific band does NOT overlap a different specialty

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandPolicy.assertNoActiveOverlap()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-PBAND-003` (specialty-specific scoping), `ADR-PBAND-001` (overlap key includes specialty)

**Preconditions:** `FX-203` existing `ACTIVE` band, `specialtyScope='CARDIOLOGY'`, same channel/duration.

**Test Steps:**
1. Act: create a new band with the same channel/duration, `specialtyScope='DERMATOLOGY'`, and an effective range that would overlap `FX-203`'s dates if specialty were ignored.
2. Assert: **no** exception — creation succeeds; the two bands coexist because their `specialtyScope` values differ and neither is `null`/universal.

**Expected Result (PASS):** Two specialty-specific bands on different specialties for the same channel/duration are independent — each Expert-price lookup in that specialty resolves unambiguously.
**Expected Result (FAIL):** Overlap check incorrectly ignores `specialtyScope` and rejects two genuinely distinct, non-conflicting bands.

**Current Status:** 🔴 Not written

---

### PBAND-TC-009 — Happy path: deactivate a price band (status-only)

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandService.deactivateBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC-240 TDS §6.4` sequence diagram, CB-218 "Xác nhận ngừng" happy path

**Preconditions:** `FX-202` band (`status='ACTIVE'`).

**Test Steps:**
1. Arrange: mock `priceBandRepository.findById(PB1)` returns `FX-202`; mock `save(...)` returns the updated entity.
2. Act: `priceBandService.deactivateBand(PB1, makeDeactivateRequest(), ADM1)`.
3. Assert: returned DTO `status == "DEACTIVATED"`; saved entity has `status="DEACTIVATED"`, all other fields (`minimumPrice`, `maximumPrice`, `commissionRate`, `channelType`, `durationMinutes`, `specialtyScope`) **unchanged** from `FX-202`.

**Expected Result (PASS):** Only `status` (and `updatedAt`) change; every other column is byte-identical to the pre-deactivation row.
**Expected Result (FAIL):** Any economic field (`minimumPrice`/`maximumPrice`/`commissionRate`) mutated during deactivation, or the row is deleted.

**Current Status:** 🔴 Not written

---

### PBAND-TC-010 — Deactivated band is not selectable for NEW Expert prices (cross-cutting)

**Severity:** `CRITICAL`
**Feature Under Test:** `PriceBandService.deactivateBand()` + downstream lookup contract (`ConsultationPriceBandDeactivated` consumed by UC-238)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-PBAND-002` (`UC-240 TDS §3` — "only NEW Expert prices are blocked"), `UC-240 TDS §7.1` event catalog ("UC-238 stops offering it")

**Preconditions:** `FX-202` band (`status='ACTIVE'`).

**Test Steps:**
1. Act: `priceBandService.deactivateBand(PB1, makeDeactivateRequest(), ADM1)`.
2. Assert: `priceBandRepository.findByChannelTypeAndDurationMinutesAndStatus("VIDEO_CALL", 30, "ACTIVE")` (the query UC-238's lookup is contractually specified to use — `UC-240 TDS §8.2`) would no longer return `PB1` post-deactivation, i.e. mock the repository's real filter semantics and assert `PB1` is absent from the `"ACTIVE"`-filtered result after `save()`.
3. Assert: `ConsultationPriceBandDeactivated` event is published (captured via `ArgumentCaptor`) with `payload.priceBandId == PB1`.

**Expected Result (PASS):** After deactivation, any query scoped to `status='ACTIVE'` excludes this band — the only channel through which UC-238 is specified to discover eligible bands (`ADR-PBAND-001`'s `findByChannelTypeAndDurationMinutesAndStatus` query, reused as the lookup contract).
**Expected Result (FAIL):** The band still appears in an `ACTIVE`-filtered query after deactivation, allowing UC-238 to create a new Expert price against a band the Admin explicitly closed.

**Current Status:** 🔴 Not written
**Implementation Note:** This test defines the **contract boundary** with UC-238 (which is out of this module's implementation scope) — it validates that UC-240's own persistence guarantees the precondition UC-238 depends on, without implementing UC-238's lookup itself.

---

### PBAND-TC-011 — Deactivation does NOT affect existing Expert prices or bookings (non-cascade)

**Severity:** `CRITICAL`
**CWE:** `CWE-460 — Improper Cleanup on Thrown Exception` *(closest applicable; primary concern is unintended cascade/orphaning, not a CWE-specific vulnerability)*
**Feature Under Test:** `PriceBandService.deactivateBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-PBAND-002` (`UC-240 TDS §3` — verbatim CB-218 copy: "Các mức giá đã được chuyên gia thiết lập và các lịch hẹn đã đặt sẽ KHÔNG bị ảnh hưởng"), `UC-240 TDS §2 Logic Issues L6`

**Preconditions:** `FX-202` band (`PB1`, `status='ACTIVE'`); `FX-208` Expert price `EP1` referencing `PB1`; `FX-209` booking `BK1` referencing `EP1`.

**Test Steps:**
1. Arrange: mock `expertConsultationPriceRepository`/`consultationBookingRepository` (or their read-only equivalents used only for the assertion, not invoked by the service) to return `FX-208`/`FX-209` unchanged.
2. Act: `priceBandService.deactivateBand(PB1, makeDeactivateRequest(), ADM1)`.
3. Assert: `priceBandRepository.save()` is called exactly once (the band itself); `verifyNoInteractions` on `expertConsultationPriceRepository` and `consultationBookingRepository` — the service never touches either table.
4. Assert (fixture-level, documents the invariant): `FX-208.getPriceBandId() == PB1` and `FX-209.getExpertPriceId() == EP1` remain unchanged before/after — no test double sets them to `null` or deletes them.

**Expected Result (PASS):** Zero interactions with Expert-price/booking repositories; `expert_consultation_prices.price_band_id` and `consultation_bookings.expert_price_id` FKs remain intact by construction (the service never had the opportunity to null or delete them).
**Expected Result (FAIL):** Any interaction with `ExpertConsultationPriceRepository`/`ConsultationBookingRepository` occurs during deactivation — indicates a cascade regression (`AP-CB-006`), a release-blocking defect that would silently invalidate Experts' existing pricing/bookings.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important test in this module — the automated proof of `ADR-PBAND-002`. Must be release-blocking (see §6 Exit Criteria).

---

### PBAND-TC-012 — Non-admin (Expert/Partner) denied → 403 (`PBAND-005`)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `PriceBandPolicy.assertAdmin()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-PBAND-004` (`UC-240 TDS §3`), `BR-RBAC`, SRS Table 102 "Primary Actor: System Admin", `UC-240 TDS §6.5` error-path sequence diagram

**Preconditions:** `FX-206` (Expert JWT).

**Test Steps:**
1. Act: `priceBandPolicy.assertAdmin("EXPERT")`.
2. Assert: throws `PriceBandAccessDeniedException` code `PBAND-005`.
3. Repeat for role `"PARTNER"`.
4. Assert: same exception/code.

**Expected Result (PASS):** Both non-admin roles rejected before any repository access; no band created/deactivated.
**Expected Result (FAIL):** A non-admin role is allowed to configure platform commission — a critical RBAC bypass distinct from (and easily confused with) UC-238/239's Expert-role gate.

**Current Status:** 🔴 Not written
**Implementation Note:** Explicitly assert this policy is **not** the same instance/logic as UC-238/239's Expert-ownership policy — this gate denies Experts, theirs allows them (for their own price only).

---

### PBAND-TC-013 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `PriceBandController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/PriceBandControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC-240 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/admin/consultations/price-bands` with no `Authorization` header.
2. Assert: `401 Unauthorized`.
3. Send `PATCH /api/v1/admin/consultations/price-bands/{id}/deactivation` with no `Authorization` header.
4. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401` on both endpoints; no band mutated.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### PBAND-TC-014 — Deactivate a non-existent band → 404 (`PBAND-006`)

**Severity:** `MEDIUM`
**Feature Under Test:** `PriceBandService.deactivateBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC-240 TDS §10` error table (`PBAND-006`)

**Test Steps:**
1. Arrange: mock `priceBandRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `priceBandService.deactivateBand(randomId, makeDeactivateRequest(), ADM1)`.
3. Assert: throws `PriceBandNotFoundException` code `PBAND-006`.

**Current Status:** 🔴 Not written

---

### PBAND-TC-015 — Deactivate an already-deactivated band → 409 (`PBAND-007`)

**Severity:** `MEDIUM`
**Feature Under Test:** `PriceBandPolicy.assertDeactivatable()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/PriceBandPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC-240 TDS §6.6` state machine ("absorbing" `DEACTIVATED` state), `UC-240 TDS §10` error table (`PBAND-007`)

**Preconditions:** `FX-204` band (`status='DEACTIVATED'`).

**Test Steps:**
1. Arrange: mock `priceBandRepository.findById(PB2)` returns `FX-204`.
2. Act: `priceBandService.deactivateBand(PB2, makeDeactivateRequest(), ADM1)`.
3. Assert: throws `PriceBandAlreadyDeactivatedException` code `PBAND-007`; `priceBandRepository.save()` is **never** called.

**Expected Result (PASS):** Idempotent-guard rejects the repeat call; no duplicate `DEACTIVATED` write.
**Expected Result (FAIL):** Silent no-op success masking a caller bug, or an exception thrown from an unrelated cause.

**Current Status:** 🔴 Not written

---

### PBAND-TC-016 — `ConsultationPriceBandCreated` event payload correctness

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandService.createBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC-240 TDS §7.3` `ConsultationPriceBandCreated` payload schema — "consumed by UC-238 band lookup"

**Test Steps:**
1. Act: `priceBandService.createBand(makeCreateRequest(), ADM1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `ConsultationPriceBandCreated` with `payload.priceBandId` equal to the persisted id, `payload.channelType == "VIDEO_CALL"`, `payload.durationMinutes == 30`, `payload.specialtyScope == null`, `payload.commissionRate == 0.15` (fraction — ADR-PBAND-006, Accepted), `payload.configuredBy == ADM1`.

**Expected Result (PASS):** Event payload carries every field UC-238's band lookup is specified to need.
**Expected Result (FAIL):** Payload missing `commissionRate`/`specialtyScope`, or `priceBandId` not matching the persisted row.

**Current Status:** 🔴 Not written

---

### PBAND-TC-017 — `ConsultationPriceBandDeactivated` event payload correctness

**Severity:** `HIGH`
**Feature Under Test:** `PriceBandService.deactivateBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC-240 TDS §7.3` `ConsultationPriceBandDeactivated` payload schema

**Test Steps:**
1. Act: `priceBandService.deactivateBand(PB1, makeDeactivateRequest(r -> r.setReason("test reason")), ADM1)`.
2. Assert: captured `ConsultationPriceBandDeactivated` event has `payload.priceBandId == PB1`, `payload.deactivatedBy == ADM1`, `payload.reason == "test reason"`, `payload.deactivatedAt != null`.

**Expected Result (PASS):** Event carries the actor, reason, and timestamp UC-238 (and audit) need.
**Expected Result (FAIL):** `reason` dropped, or `deactivatedBy` missing.

**Current Status:** 🔴 Not written

---

### PBAND-TC-018 — Scope-boundary guard: `commission_rate` written only on the band; zero interaction with Expert-price/booking/gateway collaborators

**Severity:** `CRITICAL`
**Legal:** `ADR-PBAND-005` (commission single-writer discipline), `UC-240 TDS §2 Logic Issues L1`
**Feature Under Test:** `PriceBandService.createBand()` / `deactivateBand()` / `listBands()` / `getBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/PriceBandServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-PBAND-005` — "UC-240 never writes `expert_consultation_prices` or `consultation_bookings`"; `UC-240 TDS §1.3` — "no `VnPayGatewayClient` is constructed or invoked"

**Preconditions:** `FX-211` — `ExpertConsultationPriceRepository`, `ConsultationBookingRepository`, and a generic `PaymentGatewayClient`/`VnPayGatewayClient` injected as Mockito mocks with **zero** stubbing.

**Test Steps:**
1. For each of `createBand()`, `deactivateBand()`, `listBands()`, `getBand()` (parameterized test), invoke the method with fresh fixtures (Props Isolation).
2. Assert: `verifyNoInteractions(expertConsultationPriceRepository)`, `verifyNoInteractions(consultationBookingRepository)`, `verifyNoInteractions(paymentGatewayClient)` after each call.

**Expected Result (PASS):** Zero interactions with any commission-adjacent or payment collaborator across all four methods.
**Expected Result (FAIL):** Any interaction occurs — indicates `AP-CB-008` (commission leak) or a VNPay wiring regression, both release-blocking.

**Current Status:** 🔴 Not written
**Implementation Note:** Structurally, this module should not even have these types injected; if they compile in as constructor dependencies at all, that is itself a design smell to flag in review.

---

### PBAND-TC-019 — Invalid `channelType`/`durationMinutes` rejected → 400 (`PBAND-001`)

**Severity:** `MEDIUM`
**Feature Under Test:** `CreatePriceBandRequest` DTO validation (`@Pattern`, `@Positive`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/CreatePriceBandRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `UC-240 TDS §8.1` DTO annotation (`@Pattern(regexp = "VIDEO_CALL|VOICE_CALL|CHAT")`), CB-217 channel radios (Video/Voice/Chat only)

**Test Steps:**
1. Act: validate `makeCreateRequest(r -> r.setChannelType("EMAIL"))` (not in the allowed set).
2. Assert: violation on `channelType`; error code `PBAND-001`.
3. Act: validate `makeCreateRequest(r -> r.setDurationMinutes((short) -5))`.
4. Assert: violation on `durationMinutes` (`@Positive`); error code `PBAND-001`.

**Expected Result (PASS):** Arbitrary/typo channel values and non-positive durations rejected before reaching the service layer.
**Expected Result (FAIL):** Unrecognized `channelType` or negative/zero duration silently accepted.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### PBAND-TC-INT-001 — E2E: create then deactivate (Testcontainers), non-cascade verified against real DB

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/admin/consultations/price-bands` → `PATCH .../{id}/deactivation` → DB rows correct
**Test File:** `src/test/java/com/carebridge/backend/consultation/PriceBandIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`).
- Flyway migration applied automatically (no new migration expected — verify none was added).
- Seed: `FX-208` Expert price and `FX-209` booking inserted via JPA, referencing the band that will be created in step 1 (seed order adjusted: create band first, then these rows, matching the real FK dependency direction).

**Test Steps:**
1. `POST /api/v1/admin/consultations/price-bands` with JWT for `ADM1` and `FX-201` body.
2. Assert response `201`, body matches §9.2 schema (`status="ACTIVE"`).
3. Seed `expert_consultation_prices` row referencing the returned `priceBandId`, then a `consultation_bookings` row referencing that Expert price.
4. `PATCH /api/v1/admin/consultations/price-bands/{priceBandId}/deactivation` with JWT for `ADM1` and body `{reason: "..."}`.
5. Assert response `200`, `status="DEACTIVATED"`.
6. Assert DB: `SELECT status FROM consultation_price_bands WHERE price_band_id = ?` returns `'DEACTIVATED'`.
7. Assert DB: the previously-seeded `expert_consultation_prices` row and `consultation_bookings` row are **unchanged** (same `price_band_id`/`expert_price_id`, same amounts).

**Expected Result (PASS):**
- API `201` then `200`; DB band row updated to `DEACTIVATED`; **zero** changes to the pre-existing Expert-price/booking rows.

**Expected Result (FAIL):**
- API error, DB row not updated, or (critical) the Expert-price/booking rows are altered or orphaned — scope-boundary/cascade breach.

**DB Assertion:**
```java
ConsultationPriceBandEntity band = priceBandRepository.findById(priceBandId).orElseThrow();
assertThat(band.getStatus()).isEqualTo("DEACTIVATED");
assertThat(band.getMinimumPrice()).isEqualByComparingTo("200000"); // unchanged (ADR-PBAND-002)
assertThat(band.getCommissionRate()).isEqualByComparingTo("15");   // unchanged

ExpertConsultationPriceEntity price = expertPriceRepository.findById(expertPriceId).orElseThrow();
assertThat(price.getPriceBandId()).isEqualTo(priceBandId); // FK intact, not nulled
assertThat(price.getStatus()).isEqualTo("ACTIVE");         // untouched by deactivation

ConsultationBookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
assertThat(booking.getExpertPriceId()).isEqualTo(expertPriceId); // untouched
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PBAND-TC-001` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-002` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-003` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-004` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-005` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-006` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-007` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-008` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-009` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-010` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-011` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-012` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-013` | `PriceBandControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-014` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-015` | `PriceBandPolicyTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-016` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-017` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-018` | `PriceBandServiceTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-019` | `CreatePriceBandRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `PBAND-TC-INT-001` | `PriceBandIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class PriceBandService implements IPriceBandService {

    @Override
    public PriceBandResponse createBand(CreatePriceBandRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public PriceBandResponse deactivateBand(UUID priceBandId, DeactivatePriceBandRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Page<PriceBandListItemResponse> listBands(PriceBandFilter filter, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public PriceBandResponse getBand(UUID priceBandId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PBAND-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PBAND-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC240_ConfigureConsultationPriceBands_TDS.md` reviewed and Approved
- [ ] `ADR-PBAND-001..006` confirmed by Product/Tech Lead (`ADR-PBAND-001..006` all `Accepted`; `ADR-PBAND-006` commission-rate representation pinned 2026-07-04 — fraction `[0.0, 0.5]`, e.g. `0.15` = 15%)
- [ ] Test fixtures (§3 TDS-05) prepared
- [ ] No DPO review required (no subject PII — TDS §1 header)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers; **no WireMock/VNPay needed** — confirms `L1`/`ADR-PBAND-005`)
- [ ] Test coverage ≥ 80% lines cho `PriceBandService`, `PriceBandPolicy`, `PriceBandMapper`
- [ ] Không có business logic trong `PriceBandController`
- [ ] `PBAND-TC-002` (overlap guard) and `PBAND-TC-011` (non-cascade guard) **PASS** — these are release-blocking gates
- [ ] `PBAND-TC-018` (commission scope-boundary guard) **PASS** — release-blocking
- [ ] `PBAND-TC-016`/`PBAND-TC-017` (event payload correctness) PASS — required before UC-238/239 implementation can begin safely

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — mỗi test dùng `PriceBandTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- New requirement surfaces requiring `MODERATOR`/`CONTENT_ADMIN` access to price-band endpoints (currently `Open` per TDS §16) — needs Tech Lead review before test/auth-matrix rewrite
- A future in-place "edit band" endpoint is approved (TDS §9.1 note) — would require new test cases not covered here

---

## 7. Rollback Plan

```bash
# Revert implementation files (dev only — no migration in scope, TDS §5.3 confirms
# consultation_price_bands already exists in V1__init_schema.sql)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultation/pricing/

# Bands already created/deactivated via this feature remain as-is (append-of-status;
# no data rollback is performed). Gap stays OPEN in sprint tracking until re-implemented.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to `ADR-PBAND-00X`) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security, e.g. `PBAND-TC-013`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces) | G-3 |
| **AP-CB-006** *(project-specific)* | **Deactivation cascade — deletes the band row or nulls `expert_consultation_prices.price_band_id`** | Any test path where `deactivateBand()` results in a write to `ExpertConsultationPriceRepository`/`ConsultationBookingRepository`, or the band row is removed | `PBAND-TC-011` explicitly asserts `verifyNoInteractions`; `PBAND-TC-INT-001` asserts the Expert-price/booking rows are byte-identical pre/post-deactivation against a real DB | **Release-blocking** |
| **AP-CB-007** *(project-specific)* | **Overlap allowed — two ACTIVE bands with the same key + overlapping range persist** | `createBand()` succeeds when `findByChannelTypeAndDurationMinutesAndStatus` returns an intersecting existing `ACTIVE` band | `PBAND-TC-002`, `PBAND-TC-007` assert `PBAND-004` is thrown and `save()` is never called | **Release-blocking** |
| **AP-CB-008** *(project-specific)* | **Commission leak — UC-240 writes/reads commission via `expert_consultation_prices`/booking, or calls a payment gateway** | Any interaction with `ExpertConsultationPriceRepository`, `ConsultationBookingRepository`, or a gateway client from `PriceBandService` | `PBAND-TC-018` explicitly asserts `verifyNoInteractions` across all four service methods | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`PBAND-TC-002`, `PBAND-TC-007`, `PBAND-TC-011`, `PBAND-TC-018`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC-240 v1.0 — Draft. Total test cases: 19 unit/component + 1 integration
= 20. Critical-severity: 7 (`PBAND-TC-002, 007, 010, 011, 012, 013, 018` —
overlap-guard, specialty-universal-semantics, cross-cutting not-selectable,
non-cascade, RBAC, authentication, and commission scope-boundary gates). Requires
Approved status change only by user/Tech Lead. UC-240 is the upstream prerequisite
for UC-238/UC-239 (band lookup + commission inheritance) — `PBAND-TC-010`,
`PBAND-TC-016`, and `PBAND-TC-017` are the contract-level guarantees those sibling
specs may build against without re-reading UC-240 internals. The non-cascade
invariant (`PBAND-TC-011`, `PBAND-TC-INT-001`) is explicitly and redundantly tested
because it is the single highest-risk regression this module could introduce.*
