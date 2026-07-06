# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC239 — Update Consultation Price — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-239`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` L842-874 (pricing), L876-895 (bookings), L1416-1421 (PKs), L1817-1833 (FKs) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.7.2 (UC-239, Table 101)
- `04_Implement/UC239_UpdateConsultationPrice/UC239_UpdateConsultationPrice_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `04_Implement/UC205_CancelConsultation/UC205_CancelConsultation_TDS.md` + `_Test-Spec.md` — ADR-writing / dual-trigger versioning reasoning style reused
- `03_Design/UI_UX/MobileAppScreen/CB-210 Update Consultation Price (UC-239)/code.html`, `CB-211 Deactivate Consultation Price Confirmation (UC-239)/code.html`
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC239 |

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
| **Feature / Gap ID** | `UC-239` |
| **Module** | `Consultation — Update Consultation Price` |
| **Spec gốc** | `CB-CONSULTATION-IMP-239` (UC239 TDS) |
| **Priority** | 🟠 P1 *(SRS Priority: Medium; financial-integrity class → treated as release-blocking for locked-price-immutability TCs)* |
| **Sprint** | UC238→UC241 consultation-pricing batch — LamVH |
| **Milestone** | Owner: LamVH |
| **Data Classification** | `Confidential` (commercial pricing; not personal PII) |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC-238 (Set Consultation Price — seeds the initial ACTIVE row), UC-240 (Price Bands — supplies min/max range), expert-profile module (ownership + verified-status resolution) — ALL BLOCKING, see §6 Entry Criteria |
| **Downstream Consumers** | UC-75 (Book Private Consultation — reads ACTIVE price at booking time), UC-241 (View Consultation Price — reads version history), Notification service (out of scope) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC239 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (append-new-version + supersede-old, not in-place UPDATE), C2 (never write `consultation_bookings`; locked price snapshot immutable), C3 (deactivate is a separate no-successor sub-action), C4 (band-range validation; band/channel/duration carried over unchanged), C5 (verified owning expert only; no-active-price rejected; no entity leakage) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-239 text is generic boilerplate ("applies relevant business rules") — gives no explicit persistence mechanism for "new price version" | `expert_consultation_prices.version_no` (`integer NOT NULL DEFAULT 1`, `V1__init_schema.sql` L871) plus `status`/`effective_from`/`effective_to` is the schema-level anchor confirming an append-only versioned-row design (TDS ADR-UPDPR-001) | Every "update" test asserts TWO writes — the old row transitioning to `INACTIVE` and a NEW row inserted with `version_no = previous + 1` — never a single in-place `UPDATE` of `price_amount` on the same row (`UPDPR-TC-001`, `UPDPR-TC-010`) |
| L2 | SRS says "without changing locked booking prices" but never states which column or table this protects | `consultation_bookings.price_snapshot_amount` (`numeric NOT NULL`, L888) is the locked amount; it is populated once at booking time and has no FK-cascade write path back from `expert_consultation_prices` (TDS ADR-UPDPR-002) | `UPDPR-TC-005` is a dedicated regression test: seed a booking referencing the OLD price version, snapshot `price_snapshot_amount` before the update, run `createNewVersion()`, and assert byte-for-byte equality afterward — this is the release-blocking invariant test the coordinator explicitly required |
| L3 | The non-active status string for a superseded/deactivated price row is never named in the SRS | UC239 TDS `ADR-UPDPR-003` fixes `'INACTIVE'` as a single non-active value for both supersede and deactivate; now `Accepted`/reconciled — `UC238_SetConsultationPrice_TDS.md` §6.6 state machine independently cites the same `'INACTIVE'` literal, no divergence found | Tests reference the status string via a single test-factory constant `ExpertConsultationPriceTestFactory.INACTIVE_STATUS = "INACTIVE"` (mirrors UC205's `CANCELLED_STATUS` isolation pattern) so a future coordinated rename only touches the factory, not every test assertion |
| L4 | Deactivate (CB-211 mockup) could be mistaken for just "another kind of update" with a null/zero price | UC239 TDS `ADR-UPDPR-004` mandates deactivate as a DISTINCT sub-action: `status='INACTIVE'`, `effective_to=now()`, **no successor row**, `version_no` unchanged | `UPDPR-TC-002` explicitly asserts `priceRepository.save()` is called exactly once (not twice) for deactivate, and that no row with a higher `version_no` is ever created — the automated guard against conflating deactivate with a new version |
| L5 | Band/channel/duration mutability is not explicitly forbidden anywhere in the SRS text | UC239 TDS `ADR-UPDPR-005` fixes `price_band_id`/`channel_type`/`duration_minutes` as carried-over-unchanged fields; `UpdateConsultationPriceRequest` (TDS §8.1) does not even expose these fields as inputs | `UPDPR-TC-010` asserts the newly inserted version's `priceBandId`/`channelType`/`durationMinutes` are identical to the superseded row's, proving the DTO-level scope boundary is enforced at the service layer too (defense in depth) |
| L6 | No DB constraint prevents two simultaneous `ACTIVE` rows for the same expert+channel+duration (only the FK on `price_band_id` exists) | Confirmed via schema read (TDS §5.3) — same finding pattern as UC205/UC78 `refund_records.payment_id`; the "exactly one ACTIVE" invariant is a service-level guard (`ADR-UPDPR-007`) | Idempotency/no-active/single-ACTIVE guards are tested at the service layer (`UPDPR-TC-004`, `UPDPR-TC-008`) since the DB does not enforce them |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Update Consultation Price module bao gồm các layer:
├── Domain (UpdateConsultationPricePolicy — pure logic, no deps)
├── Services (UpdateConsultationPriceService — mock JPA Repository với Mockito)
├── Controller (UpdateConsultationPriceController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-239 (Table 101, §3.2.7.2) | Trigger, single actor (Verified Expert), exceptions E1-E3, "new price version without changing locked booking prices" |
| `UC239 TDS` ADR-UPDPR-001..007 | Versioned-row design, locked-price immutability, deactivate distinction, band-fixed scope boundary, ownership/verified guard, no-active/idempotency guards |
| `V1__init_schema.sql` L842-895, L1416-1421, L1817-1833 | Column names/types/defaults/FKs as persistence oracle for both `expert_consultation_prices` and `consultation_bookings` |
| Mockup `CB-210` (Update Price) | Price-history UI (multiple past versions), band-guidance suggestion text, "Effective Date"/"Reason for Change" fields |
| Mockup `CB-211` (Deactivate) | "no new bookings; existing bookings proceed normally" copy → the exact business behavior `UPDPR-TC-002`/`UPDPR-TC-005` encode |
| `BR-RBAC` / `BR-CONSULTATION` | Authorization, auditable pricing lifecycle |
| `UC239 TDS §9-10` | API contract, error codes `UPDPR-001..006` |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified owning expert creates a new price version within band; old row superseded | `UpdateConsultationPriceService.createNewVersion()` | `UPDPR-TC-001` |
| TC-COND-002 | Verified owning expert deactivates the ACTIVE price; no successor row created | `UpdateConsultationPriceService.deactivate()` | `UPDPR-TC-002` |
| TC-COND-003 | New `priceAmount` below `minimum_price` → 400 `UPDPR-002` | `UpdateConsultationPricePolicy.assertWithinBand()` | `UPDPR-TC-003` |
| TC-COND-003b | New `priceAmount` above `maximum_price` → 400 `UPDPR-002` | `UpdateConsultationPricePolicy.assertWithinBand()` | `UPDPR-TC-003b` |
| TC-COND-004 | No ACTIVE price exists for the target → 409 `UPDPR-003` (new-version path) | `UpdateConsultationPricePolicy.assertActiveExists()` | `UPDPR-TC-004` |
| TC-COND-005 | **Invariant** — an existing booking's `price_snapshot_amount` is byte-for-byte unaffected by a price update | `UpdateConsultationPriceService.createNewVersion()` + DB assertion | `UPDPR-TC-005` |
| TC-COND-006 | Unverified expert (owner but not verified) denied → 403 `UPDPR-005` | `UpdateConsultationPricePolicy.assertCanManage()` | `UPDPR-TC-006` |
| TC-COND-007 | Non-owner verified expert denied → 403 `UPDPR-005` | `UpdateConsultationPricePolicy.assertCanManage()` | `UPDPR-TC-007` |
| TC-COND-008 | `expertPriceId` not found → 404 `UPDPR-004` | `UpdateConsultationPriceService` | `UPDPR-TC-008` |
| TC-COND-009 | Deactivate an already-`INACTIVE` price → 409 `UPDPR-006` (idempotency) | `UpdateConsultationPricePolicy.assertActiveExists()` (deactivate path) | `UPDPR-TC-009` |
| TC-COND-010 | Band-band-fixed scope: new version carries over `priceBandId`/`channelType`/`durationMinutes` unchanged from the superseded row | `UpdateConsultationPriceService.createNewVersion()` | `UPDPR-TC-010` |
| TC-COND-011 | Boundary: `priceAmount` exactly at `minimum_price`/`maximum_price` accepted (inclusive) | `UpdateConsultationPricePolicy.assertWithinBand()` | `UPDPR-TC-011` |
| TC-COND-012 | Superseded old row keeps its original `price_amount` unchanged — only `status`/`effective_to` mutated | `UpdateConsultationPriceService.createNewVersion()` | `UPDPR-TC-012` |
| TC-COND-013 | `ExpertConsultationPriceVersioned` event emitted with correct payload on new-version | `UpdateConsultationPriceService.createNewVersion()` | `UPDPR-TC-013` |
| TC-COND-014 | `ExpertConsultationPriceDeactivated` event emitted with correct payload on deactivate | `UpdateConsultationPriceService.deactivate()` | `UPDPR-TC-014` |
| TC-COND-015 | Response never leaks raw entity/internal fields | `ExpertConsultationPriceMapper` | `UPDPR-TC-015` |
| TC-COND-016 | Unauthenticated request → 401 | `UpdateConsultationPriceController` security filter | `UPDPR-TC-016` |
| TC-COND-017 | DTO validation: negative/zero `priceAmount`, missing `effectiveFrom` → 400 `UPDPR-001` | DTO validation | `UPDPR-TC-017` |
| TC-COND-018 | Concurrent-update race: single-ACTIVE invariant holds (only one ACTIVE row survives two near-simultaneous `createNewVersion` calls) | `UpdateConsultationPriceService.createNewVersion()` (locking read) | `UPDPR-TC-018` |
| TC-COND-019 | E2E — full new-version + deactivate API flow via Testcontainers, re-verifying booking snapshot immutability at the DB layer | `UpdateConsultationPriceController` + real DB | `UPDPR-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Actor identity (owner-verified / owner-unverified / non-owner) | 3 partitions map directly to 200/201 vs 403 branching |
| Boundary Value Analysis | `priceAmount` vs band `[minimum_price, maximum_price]` (min-1, min, max, max+1) | Confirms `assertWithinBand()` inclusive boundary (ADR-UPDPR-005) |
| State Transition Testing | Price row `ACTIVE→INACTIVE` (supersede or deactivate); `[*]→ACTIVE(version_no=N+1)` (new version) | Validates ADR-UPDPR-001/003/004 state machine |
| Error Guessing | IDOR via `expertPriceId` manipulation, replay of deactivate, replay of update after deactivate | Ownership + idempotency + no-active-price attack surface |
| Decision Table | Ownership × verified-status × active-status × band-range combinations | 201(new version) vs 200(deactivated) vs 403 vs 409 vs 400 branching |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ExpertConsultationPriceEntity{expertPriceId=PR1, expertProfileId=EP1, priceBandId=BAND1, channelType='VIDEO_CALL', durationMinutes=30, priceAmount=120.00, status='ACTIVE', versionNo=1, effectiveFrom=now-180d}` | Happy path — existing ACTIVE price to update/deactivate |
| `FX-002` | DB seed | `ConsultationPriceBandEntity{priceBandId=BAND1, channelType='VIDEO_CALL', durationMinutes=30, minimumPrice=110.00, maximumPrice=150.00, commissionRate=0.15}` | Band-range oracle for validation tests |
| `FX-003` | JWT | `{sub: 'UE1', role: 'EXPERT'}` — resolves as verified owner of `EP1` | Auth context — verified owning expert |
| `FX-004` | JWT | `{sub: 'UE1', role: 'EXPERT'}` — resolves as owner of `EP1` but `verified=false` | Auth context — unverified owner |
| `FX-005` | JWT | `{sub: 'UE2', role: 'EXPERT'}` — resolves as owner of a different `expertProfileId` (`EP2 != EP1`), verified | Auth context — non-owner verified expert |
| `FX-006` | Value | `UpdateConsultationPriceRequest{priceAmount=130.00, effectiveFrom=now+30d, reason='Annual inflation adjustment.'}` | Happy-path new-version request (matches CB-210 illustrative flow) |
| `FX-007` | Value | `UpdateConsultationPriceRequest{priceAmount=100.00, ...}` (below `FX-002.minimumPrice=110`) | Out-of-band (too low) |
| `FX-007b` | Value | `UpdateConsultationPriceRequest{priceAmount=160.00, ...}` (above `FX-002.maximumPrice=150`) | Out-of-band (too high) |
| `FX-008` | DB seed | `ExpertConsultationPriceEntity{...status='INACTIVE', versionNo=1}` | Already-inactive price — no-active-price / already-inactive conflict cases |
| `FX-009` | DB seed | `ConsultationBookingEntity{bookingId=B1, expertPriceId=PR1, priceSnapshotAmount=120.00, status='CONFIRMED'}` | Locked-booking-price invariant test (references `FX-001` before it is superseded) |
| `FX-010` | Value | `UpdateConsultationPriceRequest{priceAmount=110.00, ...}` — exactly `minimumPrice` | Boundary — inclusive lower bound |
| `FX-011` | Value | `UpdateConsultationPriceRequest{priceAmount=150.00, ...}` — exactly `maximumPrice` | Boundary — inclusive upper bound |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `UpdateConsultationPricePolicy`, `UpdateConsultationPriceService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest UpdateConsultationPriceController` | ✅ MockMvc full flow | ✅ IDOR / RBAC |
| Mobile | ✅ `update_consultation_price_repository.dart` unit | — | ✅ `update_consultation_price_screen` widget test (CB-210), `deactivate_price_confirmation` widget test (CB-211) | — | — |
| Web | ✅ Update/Deactivate entry points (CB-214/CB-215) — component test | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

class ExpertConsultationPriceTestFactory {

    // ADR-UPDPR-003: 'INACTIVE' is Accepted and reconciled with UC238
    // (UC238_SetConsultationPrice_TDS.md §6.6 cites the same literal) —
    // isolated in one constant so a future coordinated rename only requires
    // updating this factory, not every test.
    static final String ACTIVE_STATUS = "ACTIVE";
    static final String INACTIVE_STATUS = "INACTIVE";

    static ExpertConsultationPriceEntity makePrice() {
        ExpertConsultationPriceEntity price = new ExpertConsultationPriceEntity();
        price.setExpertPriceId(UUID.fromString("00000000-0000-0000-0000-0000000PR001"));
        price.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-00000000EP01"));
        price.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-000000BAND01"));
        price.setChannelType("VIDEO_CALL");
        price.setDurationMinutes((short) 30);
        price.setPriceAmount(new BigDecimal("120.00"));
        price.setCurrency("VND");
        price.setStatus(ACTIVE_STATUS);
        price.setVersionNo(1);
        price.setEffectiveFrom(Instant.now().minus(Duration.ofDays(180)));
        price.setEffectiveTo(null);
        return price;
    }

    static ExpertConsultationPriceEntity makePrice(Consumer<ExpertConsultationPriceEntity> overrides) {
        ExpertConsultationPriceEntity price = makePrice();
        overrides.accept(price);
        return price;
    }

    static ConsultationPriceBandEntity makeBand() {
        ConsultationPriceBandEntity band = new ConsultationPriceBandEntity();
        band.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-000000BAND01"));
        band.setChannelType("VIDEO_CALL");
        band.setDurationMinutes((short) 30);
        band.setMinimumPrice(new BigDecimal("110.00"));
        band.setMaximumPrice(new BigDecimal("150.00"));
        band.setCommissionRate(new BigDecimal("0.15"));
        band.setCurrency("VND");
        return band;
    }

    static ConsultationBookingEntity makeBookingReferencing(UUID expertPriceId) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        booking.setExpertPriceId(expertPriceId);
        booking.setPriceSnapshotAmount(new BigDecimal("120.00")); // must equal FX-001.priceAmount at seed time
        booking.setStatus("CONFIRMED");
        return booking;
    }

    static UpdateConsultationPriceRequest makeRequest() {
        UpdateConsultationPriceRequest request = new UpdateConsultationPriceRequest();
        request.setPriceAmount(new BigDecimal("130.00"));
        request.setEffectiveFrom(Instant.now().plus(Duration.ofDays(30)));
        request.setReason("Annual inflation adjustment.");
        return request;
    }

    static UpdateConsultationPriceRequest makeRequest(Consumer<UpdateConsultationPriceRequest> overrides) {
        UpdateConsultationPriceRequest request = makeRequest();
        overrides.accept(request);
        return request;
    }
}
```

---

### UPDPR-TC-001 — Happy path A: new price version created (old row superseded, new row `version_no = prev+1`, `ACTIVE`)

**Severity:** `CRITICAL`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC239 TDS §6.1` sequence diagram + `ADR-UPDPR-001` + `V1__init_schema.sql` L871 (`version_no` column)

**Preconditions:**
- `FX-001` price seeded (`expertProfileId=EP1`, `status=ACTIVE`, `versionNo=1`, `priceAmount=120.00`)
- `FX-002` band seeded (`minimumPrice=110.00`, `maximumPrice=150.00`)
- `FX-003` JWT for verified owner `UE1`

**Test Steps:**
1. Arrange: mock `priceRepository.findById(PR1)` returns `FX-001` (locking read); mock `bandRepository.findById(BAND1)` returns `FX-002`.
2. Act: `updateConsultationPriceService.createNewVersion(PR1, makeRequest(), UE1)` (`priceAmount=130.00`, within band).
3. Assert: `priceRepository.save()` invoked twice in the transaction — once with the OLD row (`status=INACTIVE_STATUS`, `effectiveTo != null`, `priceAmount` still `120.00`), once with a NEW row (`versionNo=2`, `status=ACTIVE_STATUS`, `priceAmount=130.00`, `effectiveTo=null`); response `versionNo==2`, `status=="ACTIVE"`.

**Expected Result (PASS):** Old row superseded intact (amount unchanged), new ACTIVE row inserted with `versionNo=2`.
**Expected Result (FAIL):** Old row's `priceAmount` mutated in place, OR `versionNo` not incremented by exactly 1, OR two ACTIVE rows coexist.

**Current Status:** 🔴 Not written
**Implementation Note:** `UpdateConsultationPriceService` must call `UpdateConsultationPricePolicy.assertCanManage` + `assertActiveExists` + `assertWithinBand` before any write, and both saves must occur in one `@Transactional` boundary.

---

### UPDPR-TC-002 — Happy path B: deactivate (ACTIVE→INACTIVE, no successor row)

**Severity:** `CRITICAL`
**Feature Under Test:** `UpdateConsultationPriceService.deactivate()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC239 TDS §6.2` sequence diagram, `ADR-UPDPR-004`, mockup `CB-211` copy ("no new bookings; existing bookings proceed normally")

**Preconditions:** `FX-001` price (`status=ACTIVE`, `versionNo=1`); `FX-003` JWT for verified owner `UE1`.

**Test Steps:**
1. Arrange: mock `priceRepository.findById(PR1)` returns `FX-001`.
2. Act: `updateConsultationPriceService.deactivate(PR1, UE1)`.
3. Assert: `priceRepository.save()` invoked **exactly once** with `status=INACTIVE_STATUS`, `effectiveTo != null`, `versionNo` **unchanged** (still `1`); no second/new-version row is ever constructed or saved.

**Expected Result (PASS):** Single-row deactivation; no successor version created; response `status=="INACTIVE"`.
**Expected Result (FAIL):** A second (`versionNo=2`) row is created (conflated with `createNewVersion`), or the row is deleted instead of status-flipped.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the automated guard distinguishing deactivate from update — must never regress; release-blocking per ADR-UPDPR-004.

---

### UPDPR-TC-003 — Out-of-band `priceAmount` (below minimum) → 400 (`UPDPR-002`)

**Severity:** `HIGH`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC239 TDS ADR-UPDPR-005`, `V1__init_schema.sql` L847 (`minimum_price`)

**Preconditions:** `FX-002` band (`minimumPrice=110.00`); `FX-007` request (`priceAmount=100.00`).

**Test Steps:**
1. Act: `updateConsultationPricePolicy.assertWithinBand(new BigDecimal("100.00"), makeBand())`.
2. Assert: throws `PriceOutOfBandException` code `UPDPR-002`.

**Expected Result (PASS):** Exception thrown; no write to `expert_consultation_prices`.
**Expected Result (FAIL):** Price accepted below the admin-approved band floor.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-003b — Out-of-band `priceAmount` (above maximum) → 400 (`UPDPR-002`)

**Severity:** `HIGH`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003b`
**Oracle Source:** `UC239 TDS ADR-UPDPR-005`, `V1__init_schema.sql` L848 (`maximum_price`)

**Preconditions:** `FX-002` band (`maximumPrice=150.00`); `FX-007b` request (`priceAmount=160.00`).

**Test Steps:**
1. Act: `updateConsultationPricePolicy.assertWithinBand(new BigDecimal("160.00"), makeBand())`.
2. Assert: throws `PriceOutOfBandException` code `UPDPR-002`.

**Expected Result (PASS):** Exception thrown for above-ceiling price.
**Expected Result (FAIL):** Price accepted above the admin-approved band ceiling.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-004 — No ACTIVE price exists (new-version path) → 409 (`UPDPR-003`)

**Severity:** `CRITICAL`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertActiveExists()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC239 TDS ADR-UPDPR-007`

**Preconditions:** `FX-008` — price already `status='INACTIVE'`.

**Test Steps:**
1. Act: `updateConsultationPricePolicy.assertActiveExists(FX-008 price)`.
2. Assert: throws `NoActivePriceException` code `UPDPR-003`.

**Expected Result (PASS):** Exception thrown; message steers caller to Set (UC-238); no write occurs.
**Expected Result (FAIL):** An inactive/superseded row is silently "updated again" (violates ADR-UPDPR-001/007 single-ACTIVE invariant).

**Current Status:** 🔴 Not written

---

### UPDPR-TC-005 — INVARIANT: existing booking's locked `price_snapshot_amount` unaffected by a price update

**Severity:** `CRITICAL`
**CWE:** `CWE-840 — Business Logic Errors (unintended retroactive financial mutation)`
**Legal:** `BR-CONSULTATION — auditable, non-retroactive pricing lifecycle`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()` — cross-table invariant
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC239 TDS ADR-UPDPR-002`, SRS Table 101 Description ("without changing locked booking prices"), `V1__init_schema.sql` L888 (`price_snapshot_amount`)

**Preconditions:**
- `FX-001` price (`expertPriceId=PR1`, `priceAmount=120.00`, `status=ACTIVE`)
- `FX-009` booking seeded referencing `PR1` with `priceSnapshotAmount=120.00`
- `FX-003` JWT for verified owner `UE1`

**Test Steps:**
1. Arrange: snapshot `bookingRepository.findById(B1).getPriceSnapshotAmount()` = `120.00` BEFORE the update.
2. Act: `updateConsultationPriceService.createNewVersion(PR1, makeRequest(priceAmount=130.00), UE1)`.
3. Assert (unit level): `bookingRepository` (or any repository touching `consultation_bookings`) has **zero interactions** — `verifyNoInteractions(bookingRepository)`.
4. Assert (integration level, mirrored in `UPDPR-TC-INT-001`): re-read the booking row from the DB; `price_snapshot_amount` still equals `120.00` exactly (byte-for-byte `BigDecimal` equality), and `expert_price_id` still points at the now-`INACTIVE` `PR1` row (not silently reassigned to the new version).

**Expected Result (PASS):** `consultation_bookings` untouched in every sense — no repository interaction, no changed value, no reassigned FK.
**Expected Result (FAIL):** Any mutation of `price_snapshot_amount`, or the booking's `expert_price_id` silently repointed to the new ACTIVE version — either is a release-blocking financial-integrity violation.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary regression test the coordinator flagged as release-blocking. `UpdateConsultationPriceService` must not inject/autowire a booking-write path at all for this use case — the absence of the dependency is itself part of the design (ADR-UPDPR-002).

---

### UPDPR-TC-006 — Unverified expert (owner but not verified) denied → 403 (`UPDPR-005`)

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertCanManage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC239 TDS ADR-UPDPR-006`, SRS Primary Actor "Verified Expert"

**Preconditions:** `FX-001` price (`expertProfileId=EP1`); `FX-004` JWT for `UE1` (owner of `EP1` but `verified=false`).

**Test Steps:**
1. Act: `updateConsultationPricePolicy.assertCanManage(FX-001, UE1)` where the expert-profile lookup for `EP1` returns `verified=false`.
2. Assert: throws `PriceAuthorizationException` code `UPDPR-005`.

**Expected Result (PASS):** Exception thrown; no price write.
**Expected Result (FAIL):** An unverified expert can change live pricing (contradicts SRS "Verified Expert" primary actor).

**Current Status:** 🔴 Not written

---

### UPDPR-TC-007 — Non-owner verified expert denied → 403 (`UPDPR-005`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertCanManage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC239 TDS ADR-UPDPR-006`

**Preconditions:** `FX-001` price (`expertProfileId=EP1`); `FX-005` JWT for `UE2` (verified owner of a different `EP2 != EP1`).

**Test Steps:**
1. Act: `updateConsultationPricePolicy.assertCanManage(FX-001, UE2)`.
2. Assert: throws `PriceAuthorizationException` code `UPDPR-005`.

**Expected Result (PASS):** Exception thrown for a verified-but-wrong-owner expert.
**Expected Result (FAIL):** Cross-expert price tampering allowed (IDOR).

**Current Status:** 🔴 Not written

---

### UPDPR-TC-008 — `expertPriceId` not found → 404 (`UPDPR-004`)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()` / `deactivate()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC239 TDS §10` error table

**Test Steps:**
1. Arrange: mock `priceRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `updateConsultationPriceService.createNewVersion(randomId, makeRequest(), UE1)`.
3. Assert: throws `PriceNotFoundException` code `UPDPR-004`.
4. Repeat for `deactivate(randomId, UE1)` — same exception/code.

**Expected Result (PASS):** Both entry points reject a nonexistent id identically.
**Current Status:** 🔴 Not written

---

### UPDPR-TC-009 — Deactivate an already-`INACTIVE` price → 409 (`UPDPR-006`) idempotency

**Severity:** `HIGH`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Feature Under Test:** `UpdateConsultationPriceService.deactivate()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC239 TDS ADR-UPDPR-007`, `§10` error table

**Preconditions:** `FX-008` price already `status='INACTIVE'`.

**Test Steps:**
1. Arrange: mock `priceRepository.findById(PR1)` returns `FX-008`.
2. Act: `updateConsultationPriceService.deactivate(PR1, UE1)` (second/duplicate call).
3. Assert: throws `PriceAlreadyInactiveException` code `UPDPR-006`; `priceRepository.save()` never invoked.

**Expected Result (PASS):** Clean 409, zero writes on replay.
**Expected Result (FAIL):** Silent no-op success (masking a client bug) or an unhandled exception/500.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-010 — Band-fixed scope boundary: new version carries over `priceBandId`/`channelType`/`durationMinutes` unchanged

**Severity:** `HIGH`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC239 TDS ADR-UPDPR-005` — "changing the channel or duration is a new Set (UC-238), not an Update"

**Preconditions:** `FX-001` price (`priceBandId=BAND1`, `channelType='VIDEO_CALL'`, `durationMinutes=30`).

**Test Steps:**
1. Act: `updateConsultationPriceService.createNewVersion(PR1, makeRequest(), UE1)`.
2. Assert: the captured NEW row argument to `priceRepository.save()` has `priceBandId==BAND1`, `channelType=="VIDEO_CALL"`, `durationMinutes==30` — identical to the superseded row; `UpdateConsultationPriceRequest` itself has no setters for these fields (compile-time enforcement, verified by DTO inspection).

**Expected Result (PASS):** Band/channel/duration immutable across a version bump.
**Expected Result (FAIL):** A caller can smuggle a different band/channel/duration through the update endpoint.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-011 — Boundary: `priceAmount` exactly at band min/max is accepted (inclusive)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/UpdateConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC239 TDS ADR-UPDPR-005` (inclusive `[min, max]`, confirmed reconciled with `UC238 TDS ADR-SETPR-003`)

**Test Steps:**
1. Act (min): `assertWithinBand(new BigDecimal("110.00"), makeBand())` (`FX-010`).
2. Assert: no exception.
3. Act (max): `assertWithinBand(new BigDecimal("150.00"), makeBand())` (`FX-011`).
4. Assert: no exception.
5. Act (min-1 / max+1): re-run `UPDPR-TC-003`/`UPDPR-TC-003b` values — still rejected.

**Expected Result (PASS):** Exact boundary values accepted; one cent below/above rejected.
**Expected Result (FAIL):** Off-by-one exclusion of the boundary itself (contradicts the confirmed inclusive design).

**Current Status:** 🔴 Not written
**Implementation Note:** Inclusive boundary is confirmed (not merely proposed) — reconciled with UC238 ADR-SETPR-003, which independently fixes the identical inclusive-endpoints rule.

---

### UPDPR-TC-012 — Superseded old row keeps its original `price_amount` unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC239 TDS ADR-UPDPR-001/002`

**Preconditions:** `FX-001` price (`priceAmount=120.00`).

**Test Steps:**
1. Act: `updateConsultationPriceService.createNewVersion(PR1, makeRequest(priceAmount=130.00), UE1)`.
2. Assert: the captured OLD-row argument to `priceRepository.save()` has `priceAmount` still equal to `120.00` (unchanged) — only `status` (`→INACTIVE_STATUS`) and `effectiveTo` (`→ non-null cutoff`) differ from the pre-update state.

**Expected Result (PASS):** Historical amount preserved for audit/history (UC-241 consumers).
**Expected Result (FAIL):** Old row's `priceAmount` is overwritten with the new value (would corrupt version history and violate ADR-UPDPR-001's "not an in-place UPDATE" mandate).

**Current Status:** 🔴 Not written

---

### UPDPR-TC-013 — `ExpertConsultationPriceVersioned` event emitted with correct payload

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC239 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `updateConsultationPriceService.createNewVersion(PR1, makeRequest(), UE1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured event is `ExpertConsultationPriceVersioned` with `payload.supersededPriceId==PR1`, `payload.previousVersionNo==1`, `payload.newVersionNo==2`, `payload.previousAmount==120.00`, `payload.newAmount==130.00`, `metadata.causedBy==UE1`.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-014 — `ExpertConsultationPriceDeactivated` event emitted with correct payload

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateConsultationPriceService.deactivate()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC239 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `updateConsultationPriceService.deactivate(PR1, UE1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured `ExpertConsultationPriceDeactivated` with `payload.priceId==PR1`, `payload.versionNo==1` (unchanged), `metadata.causedBy==UE1`.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-015 — Response never leaks raw entity/internal fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `ExpertConsultationPriceMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ExpertConsultationPriceMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"

**Test Steps:**
1. Act: `ExpertConsultationPriceMapper.toResponse(makePrice())`.
2. Assert: resulting `ExpertConsultationPriceResponse` JSON contains only the fields declared in TDS §9.2 (`expertPriceId`, `expertProfileId`, `priceBandId`, `channelType`, `durationMinutes`, `priceAmount`, `currency`, `status`, `versionNo`, `effectiveFrom`, `effectiveTo`) — no raw internal audit columns (`createdAt`/`updatedAt` are internal and excluded per §9.2 examples).

**Expected Result (PASS):** Response matches exactly the `ExpertConsultationPriceResponse` schema in TDS §9.2.
**Expected Result (FAIL):** Extra internal fields serialized.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-016 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `UpdateConsultationPriceController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/UpdateConsultationPriceControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC239 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/prices/{id}/new-version` with no `Authorization` header.
2. Assert: `401 Unauthorized`.
3. Repeat for `PATCH /api/v1/consultations/prices/{id}/deactivate`.

**Expected Result (PASS = hệ thống an toàn):** `401` for both endpoints, no price mutation.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-017 — DTO validation: non-positive `priceAmount` / missing `effectiveFrom` → 400 (`UPDPR-001`)

**Severity:** `LOW`
**Feature Under Test:** `UpdateConsultationPriceRequest` DTO validation (`@DecimalMin`, `@NotNull`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/UpdateConsultationPriceRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC239 TDS §8.1` DTO annotations

**Test Steps:**
1. Act (zero amount): validate `makeRequest(r -> r.setPriceAmount(BigDecimal.ZERO))`.
2. Assert: violation on `priceAmount` (`@DecimalMin(inclusive=false)`), error code `UPDPR-001`.
3. Act (negative amount): validate `makeRequest(r -> r.setPriceAmount(new BigDecimal("-10.00")))`.
4. Assert: violation, `UPDPR-001`.
5. Act (missing effectiveFrom): validate `makeRequest(r -> r.setEffectiveFrom(null))`.
6. Assert: violation on `effectiveFrom` (`@NotNull`), `UPDPR-001`.

**Expected Result (PASS):** All three invalid inputs rejected at the DTO layer before hitting the service.
**Expected Result (FAIL):** A non-positive price or missing effective date reaches the service layer.

**Current Status:** 🔴 Not written

---

### UPDPR-TC-018 — Concurrent-update race: single-ACTIVE invariant holds

**Severity:** `HIGH`
**CWE:** `CWE-362 — Concurrent Execution using Shared Resource with Improper Synchronization (Race Condition)`
**Feature Under Test:** `UpdateConsultationPriceService.createNewVersion()` (locking read)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/UpdateConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `UC239 TDS ADR-UPDPR-007` (single-ACTIVE guard via locking/optimistic read)

**Preconditions:** `FX-001` price `ACTIVE`, `versionNo=1`; two near-simultaneous `createNewVersion` calls simulated against the same starting row.

**Test Steps:**
1. Arrange: mock `priceRepository.findById(PR1)` to return `FX-001` (version 1, ACTIVE) to the first caller, then simulate the second caller re-reading and observing the row is already `INACTIVE` (i.e., the repository's locking/optimistic-check causes the second call's `assertActiveExists` to fail after the first commits).
2. Act: invoke `createNewVersion` twice "concurrently" (sequential test simulating the interleave — first call completes and commits, second call re-reads and hits the guard).
3. Assert: exactly one call succeeds (produces `versionNo=2`, `ACTIVE`); the second call throws `NoActivePriceException` (`UPDPR-003`) or an optimistic-locking conflict mapped to a 409 — never two ACTIVE rows.

**Expected Result (PASS):** Only one ACTIVE row survives; the loser gets a clean 409, not a silent double-ACTIVE state.
**Expected Result (FAIL):** Two ACTIVE rows exist simultaneously for the same expert+channel+duration (violates ADR-UPDPR-001/007 — release-blocking).

**Current Status:** 🔴 Not written
**Implementation Note:** True concurrency is exercised at the integration level (`UPDPR-TC-INT-001` variant, optional); this unit test documents and asserts the guard's logical behavior via mocked sequential reads.

---

### INTEGRATION TEST CASES

---

### UPDPR-TC-INT-001 — E2E: new-version + deactivate API flow (Testcontainers), booking-snapshot immutability re-verified at DB layer

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST .../new-version` and `PATCH .../deactivate` → DB state (`expert_consultation_prices` + `consultation_bookings`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/UpdateConsultationPriceIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-001` price, `FX-002` band, `FX-009` booking referencing `FX-001`, inserted via JPA before test

**Test Steps:**
1. Seed price `PR1` (`ACTIVE`, `versionNo=1`, `120.00`), band `BAND1` (`110-150`), booking `B1` (`priceSnapshotAmount=120.00`, `expertPriceId=PR1`).
2. `POST /api/v1/consultations/prices/{PR1}/new-version` with JWT for verified owner `UE1`, body `{"priceAmount": 130.00, "effectiveFrom": "...", "reason": "..."}`.
3. Assert response `201`, body matches §9.2 schema (`versionNo:2`, `status:"ACTIVE"`).
4. Assert DB: two rows for `(EP1, VIDEO_CALL, 30)` — old `PR1` now `status='INACTIVE'` with `price_amount` still `120.00`; new row `status='ACTIVE'`, `version_no=2`, `price_amount=130.00`.
5. Assert DB: booking `B1.price_snapshot_amount` still `120.00`; `B1.expert_price_id` still `PR1` (not reassigned).
6. `PATCH /api/v1/consultations/prices/{newPriceId}/deactivate` with JWT for `UE1`.
7. Assert response `200`, `status:"INACTIVE"`; assert DB: no third row created (`version_no` still `2` on that lineage's latest row), no ACTIVE row remains for `(EP1, VIDEO_CALL, 30)`.

**Expected Result (PASS):** API responses correct; DB rows consistent across `expert_consultation_prices` and `consultation_bookings`; booking snapshot provably untouched end-to-end.
**Expected Result (FAIL):** API error, or any DB inconsistency (booking snapshot changed, old row's amount overwritten, duplicate ACTIVE rows, or a spurious version row from deactivate).

**DB Assertion:**
```java
List<ExpertConsultationPriceEntity> lineage = priceRepository
    .findAllByExpertProfileIdAndChannelTypeAndDurationMinutesOrderByVersionNo(EP1, "VIDEO_CALL", (short) 30);
assertThat(lineage).extracting(ExpertConsultationPriceEntity::getVersionNo).containsExactly(1, 2);
assertThat(lineage.get(0).getStatus()).isEqualTo("INACTIVE");
assertThat(lineage.get(0).getPriceAmount()).isEqualByComparingTo("120.00"); // unchanged
assertThat(lineage.get(1).getPriceAmount()).isEqualByComparingTo("130.00");

ConsultationBookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
assertThat(booking.getPriceSnapshotAmount()).isEqualByComparingTo("120.00"); // untouched
assertThat(booking.getExpertPriceId()).isEqualTo(PR1); // FK not reassigned
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UPDPR-TC-001` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-002` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-003` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-003b` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-004` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-005` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-006` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-007` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-008` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-009` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-010` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-011` | `UpdateConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-012` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-013` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-014` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-015` | `ExpertConsultationPriceMapperTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-016` | `UpdateConsultationPriceControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-017` | `UpdateConsultationPriceRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-018` | `UpdateConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `UPDPR-TC-INT-001` | `UpdateConsultationPriceIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class UpdateConsultationPriceService implements IUpdateConsultationPriceService {
    @Override
    public ExpertConsultationPriceResponse createNewVersion(UUID expertPriceId,
                                                             UpdateConsultationPriceRequest request,
                                                             UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertConsultationPriceResponse deactivate(UUID expertPriceId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UPDPR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-003b` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UPDPR-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** UC-238 (Set Consultation Price) implemented and deployed to a test environment — produces the initial ACTIVE `expert_consultation_prices` row UC239 requires (§1.4 TDS)
- [ ] **BLOCKING:** UC-240 (Configure Consultation Price Bands) implemented — `consultation_price_bands` carries real `minimum_price`/`maximum_price`
- [ ] **BLOCKING:** Expert-profile module exposes `expert_profile_id → owning user_id` + verified-status read
- [ ] `UC239_UpdateConsultationPrice_TDS.md` reviewed and Approved
- [ ] ADR-UPDPR-003 (non-active status string `'INACTIVE'`) — reconciled with UC-238 TDS §6.6 (now `Accepted`, no divergence found)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `UpdateConsultationPriceService`, `UpdateConsultationPricePolicy`
- [ ] Không có business logic trong `UpdateConsultationPriceController` (chỉ có validation + mapping)
- [ ] `UPDPR-TC-005` (locked-booking-price immutability) and `UPDPR-TC-001`/`UPDPR-TC-002`/`UPDPR-TC-012` (versioning mechanics) pass — release-blocking financial-integrity gates
- [ ] `UPDPR-TC-004`/`UPDPR-TC-009` (no-active / already-inactive rejection) pass — release-blocking
- [ ] `UPDPR-TC-018` (single-ACTIVE race guard) pass — release-blocking

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — mỗi test dùng `ExpertConsultationPriceTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-238/UC-240/expert-profile prerequisite services not yet deployed to test environment
- ADR-UPDPR-003 (non-active status string) still unconfirmed once UC-238 TDS lands and conflicts with `'INACTIVE'`
- New architecture unknown discovered (e.g., a DB partial-unique index is added retroactively, changing the concurrency-guard test shape)

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in scope for UC239)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong task tracker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-UPDPR-00X) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `UPDPR-TC-016`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces) | G-3 |
| **AP-CB-007** *(project-specific)* | **In-place price UPDATE** | Any test/implementation path mutates `price_amount` on the existing row instead of appending a new version | `UPDPR-TC-001`/`UPDPR-TC-012` explicitly assert the old row's `priceAmount` is unchanged and a NEW row is inserted | **Release-blocking** |
| **AP-CB-008** *(project-specific)* | **Locked-booking-price mutation** | Any write path touches `consultation_bookings.price_snapshot_amount` or reassigns `expert_price_id` | `UPDPR-TC-005` explicitly asserts `verifyNoInteractions(bookingRepository)` + DB byte-for-byte equality | **Release-blocking** |
| **AP-CB-009** *(project-specific)* | **Deactivate/Update conflation** | A deactivate call creates a successor (`version_no+1`) row, or an update call fails to supersede the old row | `UPDPR-TC-002` asserts exactly one `save()` call for deactivate; `UPDPR-TC-001` asserts exactly two for new-version | **Release-blocking** |
| **AP-CB-010** *(project-specific)* | **Double-ACTIVE race** | Two ACTIVE rows coexist for the same expert+channel+duration after concurrent updates | `UPDPR-TC-018` asserts the second concurrent caller is rejected, never silently double-committed | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`UPDPR-TC-001/002/005/012/018`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC239 v1.0 — Draft. Total test cases: 20 (19 unit/component + 1 integration). Critical-severity: 8 (`UPDPR-TC-001, 002, 005, 006, 007, 004, 012` plus release-blocking race guard `UPDPR-TC-018` — financial-integrity, IDOR, versioning-mechanics, and race-condition gates). Requires Approved status change only by user/Tech Lead. ADR-UPDPR-003 (non-active status string `'INACTIVE'`, reconciled with UC-238 TDS §6.6) and the boundary-inclusivity rule in ADR-UPDPR-005 (`UPDPR-TC-011`, reconciled with UC-238 ADR-SETPR-003) are now `Accepted` — NOT invented, cited and mechanism-tested. Consistency Gate CG-1..CG-9 executed against `UC239_UpdateConsultationPrice_TDS.md` — see below.*

---

## Consistency Gate CG-1..CG-9 (executed against `UC239_UpdateConsultationPrice_TDS.md`)

| Gate | Check | Result |
|------|-------|--------|
| CG-1 | Every TDS ADR (UPDPR-001..007) has ≥1 corresponding TC | ✅ Pass — ADR-001→`TC-001/012`; ADR-002→`TC-005`; ADR-003→`TC-001/002` (status constant); ADR-004→`TC-002`; ADR-005→`TC-003/003b/010/011`; ADR-006→`TC-006/007`; ADR-007→`TC-004/009/018` |
| CG-2 | Every TDS error code (§10, `UPDPR-001..006`) has ≥1 dedicated TC | ✅ Pass — `UPDPR-001`→`TC-017`; `UPDPR-002`→`TC-003/003b/011`; `UPDPR-003`→`TC-004`; `UPDPR-004`→`TC-008`; `UPDPR-005`→`TC-006/007`; `UPDPR-006`→`TC-009` |
| CG-3 | Every TDS §8 interface method has ≥1 TC exercising it | ✅ Pass — `createNewVersion()`→`TC-001/003/003b/004/005/010/011/012/013/017/018`; `deactivate()`→`TC-002/009/014` |
| CG-4 | Every TDS §7 domain event has a dedicated emission TC | ✅ Pass — `ExpertConsultationPriceVersioned`→`TC-013`; `ExpertConsultationPriceDeactivated`→`TC-014` |
| CG-5 | Every TDS §16 Authorization Matrix row/denial path has a TC | ✅ Pass — verified-owner ✅ paths covered by `TC-001/002`; unverified/non-owner ❌ paths by `TC-006/007`; unauthenticated by `TC-016` |
| CG-6 | Every TDS §6 sequence-diagram error branch has a corresponding TC | ✅ Pass — §6.3 (out-of-band)→`TC-003/003b`; §6.4 (no active)→`TC-004`; §6.5 (denied)→`TC-006/007` |
| CG-7 | The coordinator-mandated invariant test (existing-booking-price-snapshot-unaffected) is present and release-blocking | ✅ Pass — `UPDPR-TC-005` (unit) + step 5 of `UPDPR-TC-INT-001` (integration), both marked release-blocking in §6 Exit Criteria and §8 anti-pattern table (`AP-CB-008`) |
| CG-8 | No TC invents a numeric/string value not sourced from an ADR, schema, or fixture (no hallucinated business facts) | ✅ Pass — band values (110/150/120/130) are FX-defined fixtures, not claimed SRS facts; status string `'INACTIVE'` is isolated behind one factory constant tied to the now-`Accepted` ADR-UPDPR-003 (reconciled with UC-238), not hard-coded per-test |
| CG-9 | Every TC declares an Oracle Source traceable to the TDS/schema/mockup (no unattributed expected values) | ✅ Pass — all 20 TCs cite `UC239 TDS §X` / `ADR-UPDPR-00X` / `V1__init_schema.sql` line refs / mockup CB-210/CB-211 in their Oracle Source field |

**CG-1..CG-9 overall: 9/9 PASS.** No fixes required against the TDS; no TDS edits made (per instruction, TDS left untouched).
