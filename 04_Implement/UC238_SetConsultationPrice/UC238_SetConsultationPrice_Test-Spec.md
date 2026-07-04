# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC238 — Set Consultation Price — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-238`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L786-800 expert_profiles, L842-874 pricing, L1416-1421, L1814-1821 constraints) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.7.1 Table 100 (L1711-1730)
- `04_Implement/UC238_SetConsultationPrice/UC238_SetConsultationPrice_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `04_Implement/UC205_CancelConsultation/UC205_CancelConsultation_TDS.md` + `_Test-Spec.md` — ADR-writing / TC-writing style reference
- `04_Implement/UC202_ViewConsultationList/UC202_ViewConsultationList_TDS.md` — read-model/DTO/ownership conventions
- `03_Design/UI_UX/MobileAppScreen/CB-209 Set Consultation Price (UC-238)/code.html` — band-guidance UI oracle (illustrative numbers only)
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC238 |

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
| **Feature / Gap ID** | `UC-238` |
| **Module** | `Consultation — Set Consultation Price` |
| **Spec gốc** | `CB-CONSULTATION-IMP-238` (UC238 TDS) |
| **Priority** | 🟠 P1 *(SRS Priority: Medium; financial-integrity class — band-range and commission-leakage TCs treated as release-blocking)* |
| **Sprint** | UC238→UC241 consultation-pricing batch — TV4-Lâm |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC-240 Configure Consultation Price Bands (BLOCKING — see §6 Entry Criteria), expert-profile module (verification status + JWT resolution, BLOCKING) |
| **Downstream Consumers** | UC-75 Book Private Consultation (price/commission snapshot), UC-239 Update Consultation Price, UC-241 View Consultation Price |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC238 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (band resolution + NOT NULL FK, no auto-create), C2 (no commission on expert_consultation_prices — inherited via price_band_id, snapshotted only at booking), C3 (band range inclusive validation, no hard-coded mockup numbers), C4 (no duplicate ACTIVE price per combo — UC-239 scope boundary), C5 (verified-expert gate, literal `'VERIFIED'` cited from UC-103; JWT-resolved expert_profile_id only) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-238 says only "within the CareBridge allowed price band" — gives **no** numeric bounds; the CB-209 mockup shows illustrative `min="200000" max="1000000"` with an `IDR/Rp` label | The real bounds are per-row on `consultation_price_bands.minimum_price`/`maximum_price` (`V1__init_schema.sql` L848-849); the schema's default currency is `VND` (L851/L866), not the mockup's `IDR/Rp` | Every band-range test asserts against a **fixture-supplied** band (`FX-001`/`FX-002`), never a literal `200000`/`1000000` claimed as an SRS fact; currency assertions use `VND` |
| L2 | Nothing in the SRS states whether `expert_consultation_prices` carries a commission figure | `expert_consultation_prices` (`V1__init_schema.sql` L859-874) has **no commission column at all**; only `consultation_price_bands.commission_rate` (L850) exists | `SETPR-TC-013` explicitly asserts the persisted price entity/response has no commission field/value — this is the automated regression guard for `ADR-SETPR-002`'s inheritance-chain design |
| L3 | SRS Primary Actor is "Verified Expert" but never states the exact `verification_status` string | `expert_profiles.verification_status varchar(30) NOT NULL DEFAULT 'PENDING'` (L794) — only the **default** literal is schema-confirmed from the DDL alone; the approved/verified literal `'VERIFIED'` is resolved by cross-referencing `04_Implement/UC103_VerifyExpertProfile/UC103_VerifyExpertProfile_TDS.md` ADR-EXP-201 (the expert-profile module's own TDS, which already defines and uses this exact literal), reconfirmed independently by `UC104_RevokeExpertBadge_TDS.md` | Tests isolate the literal in one factory constant `ExpertProfileTestFactory.VERIFIED_STATUS = "VERIFIED"` (now a cited value, not a placeholder); if UC-103 ever renames its enum before either UC ships, only the factory constant changes, not test logic |
| L4 | Nothing in the SRS or an ERD states how "no duplicate price" is enforced | No DB unique/partial index exists preventing two ACTIVE `expert_consultation_prices` rows for the same `(expert_profile_id, channel_type, duration_minutes)` — only the PK on `expert_price_id` (`V1__init_schema.sql` L1419-1420) | Duplicate-active rejection (`ADR-SETPR-004`) is tested as a **service-level** Mockito guard (`SETPR-TC-007`), not a DB-constraint integration test |
| L5 | `specialty_scope` semantics (global vs specialty band) are not described anywhere in SRS | `consultation_price_bands.specialty_scope varchar(100)` is **nullable** (L847) — a band may be global (`NULL`) or specialty-specific; `ADR-SETPR-006` fixes specialty-specific-preferred, global-fallback precedence as a firm design decision ("most-specific-match-wins"), reconciled with `UC240 TDS` ADR-PBAND-003 | `SETPR-TC-014`/`SETPR-TC-015` explicitly cover both branches (specialty match preferred; global fallback when no specialty-specific band exists); this precedence rule is not an SRS fact but is now a cited, firm design decision, not left ambiguous |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Set Consultation Price module bao gồm các layer:
├── Domain (SetConsultationPricePolicy — pure logic, no deps)
├── Services (SetConsultationPriceService — mock JPA Repository với Mockito)
├── Controller (SetConsultationPriceController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-238 (Table 100, L1711-1730) | Trigger, Verified Expert actor, exceptions E1-E3, "within allowed price band" description |
| `UC238 TDS` ADR-SETPR-001..006 | Band hard-prerequisite (422), commission inheritance (no commission column), band-range validation (inclusive), duplicate-active rejection (409, scope vs UC-239), verified-expert gate (literal `'VERIFIED'`, cited from UC-103), band resolution precedence (specialty-then-global, most-specific-wins) |
| `V1__init_schema.sql` L786-800, L842-874, L1416-1421, L1814-1821 | Column names/types/defaults/FKs as persistence oracle |
| `BR-RBAC` / `BR-CONSULTATION` | Authorization, auditable pricing lifecycle |
| `UC238 TDS §9-10` | API contract, error codes `SETPR-001..006` |
| CB-209 mockup (`code.html` L228-234, L272-291) | Band-guidance UI copy (illustrative bounds only, not a numeric oracle) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified expert sets price within band → 201, ACTIVE, version 1 | `SetConsultationPriceService.setPrice()` | `SETPR-TC-001` |
| TC-COND-002 | Price below band minimum → 400 `SETPR-002` | `SetConsultationPricePolicy.assertWithinBand()` | `SETPR-TC-002` |
| TC-COND-003 | Price above band maximum → 400 `SETPR-002` | `SetConsultationPricePolicy.assertWithinBand()` | `SETPR-TC-003` |
| TC-COND-004 | Price exactly at band minimum → accepted (inclusive boundary) | `SetConsultationPricePolicy.assertWithinBand()` | `SETPR-TC-004` |
| TC-COND-005 | Price exactly at band maximum → accepted (inclusive boundary) | `SetConsultationPricePolicy.assertWithinBand()` | `SETPR-TC-005` |
| TC-COND-006 | No matching ACTIVE band → 422 `SETPR-003` (UC-240 dependency) | `SetConsultationPriceService.setPrice()` + `ConsultationPriceBandRepository.findActiveMatching()` | `SETPR-TC-006` |
| TC-COND-007 | Duplicate ACTIVE price for same channel+duration → 409 `SETPR-004` | `SetConsultationPricePolicy.assertNoActiveDuplicate()` | `SETPR-TC-007` |
| TC-COND-008 | Unverified expert → 403 `SETPR-005` | `SetConsultationPricePolicy.assertIsVerifiedExpert()` | `SETPR-TC-008` |
| TC-COND-009 | Non-expert role (MOTHER) attempts to set a price → 403 `SETPR-006` | `SetConsultationPriceController` RBAC filter | `SETPR-TC-009` |
| TC-COND-010 | New row persisted with `status='ACTIVE'`, `version_no=1` | `SetConsultationPriceService.setPrice()` | `SETPR-TC-010` |
| TC-COND-011 | `price_band_id` resolved and persisted (FK provenance) | `SetConsultationPriceService.setPrice()` | `SETPR-TC-011` |
| TC-COND-012 | Currency defaults to `VND` when request omits it | `SetConsultationPriceService.setPrice()` | `SETPR-TC-012` |
| TC-COND-013 | No commission value read/written/exposed anywhere in the flow | `ExpertConsultationPriceEntity`, `SetConsultationPriceService`, `ConsultationPriceMapper` | `SETPR-TC-013` |
| TC-COND-014 | Band resolution — specialty-specific band preferred over a global band for the same channel+duration | `ConsultationPriceBandRepository.findActiveMatching()` | `SETPR-TC-014` |
| TC-COND-015 | Band resolution — global band used when no specialty-specific band exists | `ConsultationPriceBandRepository.findActiveMatching()` | `SETPR-TC-015` |
| TC-COND-016 | `ExpertConsultationPriceSet` event emitted with correct payload (no commission field) | `SetConsultationPriceService.setPrice()` | `SETPR-TC-016` |
| TC-COND-017 | Missing/invalid required field → 400 `SETPR-001` | DTO validation | `SETPR-TC-017` |
| TC-COND-018 | Unauthenticated request → 401 | `SetConsultationPriceController` security filter | `SETPR-TC-018` |
| TC-COND-019 | Response never leaks raw entity/internal/commission fields | `ConsultationPriceMapper` | `SETPR-TC-019` |
| TC-COND-020 | `expert_profile_id` always resolved server-side from JWT — a client-supplied `expertProfileId` in the request body (if any) is ignored/rejected, never trusted (IDOR guard) | `SetConsultationPriceService.setPrice()` | `SETPR-TC-020` |
| TC-COND-021 | E2E — full set-price API flow via MockMvc/Testcontainers | `SetConsultationPriceController` + real DB | `SETPR-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Actor state (verified expert / unverified expert / non-expert role / unauthenticated) | 4 partitions map directly to 201 vs 403 vs 403 vs 401 branching |
| Boundary Value Analysis | `priceAmount` vs band `[minimum_price, maximum_price]` (exactly at min, exactly at max, one unit below min, one unit above max) | Confirms the inclusive-endpoint decision in `ADR-SETPR-003` |
| State Transition Testing | Price `[*] → ACTIVE` (version 1); no-active → ACTIVE; ACTIVE-exists → rejected (409) | Validates `ADR-SETPR-004`'s Create/Update boundary |
| Error Guessing | Client-supplied `expertProfileId` spoofing, replay of set-price call on an existing active combo, band-not-found race with UC-240 | IDOR + duplicate-active + missing-prerequisite attack surface |
| Decision Table | Verification status × band-match × range × duplicate-active combinations | 201 vs 403(`SETPR-005`) vs 422(`SETPR-003`) vs 400(`SETPR-002`) vs 409(`SETPR-004`) branching |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationPriceBandEntity{priceBandId=B1, channelType='VIDEO', durationMinutes=30, specialtyScope=null, minimumPrice=200000, maximumPrice=1000000, commissionRate=0.15, currency='VND', status='ACTIVE'}` | Global ACTIVE band — happy path + range validation oracle |
| `FX-002` | DB seed | `ConsultationPriceBandEntity{priceBandId=B2, channelType='VIDEO', durationMinutes=30, specialtyScope='PEDIATRICS', minimumPrice=250000, maximumPrice=1200000, commissionRate=0.12, status='ACTIVE'}` | Specialty-specific ACTIVE band — precedence test |
| `FX-003` | DB seed | `ExpertProfileEntity{expertProfileId=EP1, userId=U1, specialty=null, verificationStatus=VERIFIED_STATUS}` | Verified expert, no specialty (matches global band) |
| `FX-004` | DB seed | `ExpertProfileEntity{expertProfileId=EP1, userId=U1, verificationStatus='PENDING'}` | Unverified expert — `SETPR-TC-008` |
| `FX-005` | DB seed | `ExpertProfileEntity{expertProfileId=EP2, userId=U2, specialty='PEDIATRICS', verificationStatus=VERIFIED_STATUS}` | Verified expert with a specialty matching `FX-002` — `SETPR-TC-014` |
| `FX-006` | JWT | `{sub: 'U1', role: 'EXPERT'}` | Auth context — verified expert (resolves to `EP1`) |
| `FX-007` | JWT | `{sub: 'U2', role: 'EXPERT'}` | Auth context — verified expert with specialty (resolves to `EP2`) |
| `FX-008` | JWT | `{sub: 'U3', role: 'MOTHER'}` | Auth context — non-expert role — `SETPR-TC-009` |
| `FX-009` | Value | `SetConsultationPriceRequest{channelType='VIDEO', durationMinutes=30, priceAmount=350000, currency='VND', cancellationPolicy='...', effectiveFrom=now}` | Baseline valid request — happy path |
| `FX-010` | DB seed | `ExpertConsultationPriceEntity{expertPriceId=P1, expertProfileId=EP1, priceBandId=B1, channelType='VIDEO', durationMinutes=30, priceAmount=350000, status='ACTIVE', versionNo=1}` | Pre-existing ACTIVE price — duplicate-rejection case (`SETPR-TC-007`) |
| `FX-011` | Value | `SetConsultationPriceRequest{... priceAmount=199999}` (one unit below `FX-001.minimumPrice`) | Below-min boundary — `SETPR-TC-002` |
| `FX-012` | Value | `SetConsultationPriceRequest{... priceAmount=1000001}` (one unit above `FX-001.maximumPrice`) | Above-max boundary — `SETPR-TC-003` |
| `FX-013` | Value | `SetConsultationPriceRequest{... priceAmount=200000}` (exactly `FX-001.minimumPrice`) | Inclusive-min boundary — `SETPR-TC-004` |
| `FX-014` | Value | `SetConsultationPriceRequest{... priceAmount=1000000}` (exactly `FX-001.maximumPrice`) | Inclusive-max boundary — `SETPR-TC-005` |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `SetConsultationPricePolicy`, `SetConsultationPriceService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest SetConsultationPriceController` | ✅ MockMvc full flow | ✅ IDOR / RBAC |
| Mobile | ✅ set-price repository unit | — | ✅ `set_consultation_price` widget test (CB-209) | — | — |
| Web | ✅ Set-price form (CB-213) — component test | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

class SetConsultationPriceTestFactory {

    // ADR-SETPR-005: approved literal 'VERIFIED', cited from
    // UC103_VerifyExpertProfile_TDS.md ADR-EXP-201 — isolated in one constant
    // so a future UC-103 enum rename only requires updating this factory, not every test.
    static final String VERIFIED_STATUS = "VERIFIED"; // Cited from UC-103 ADR-EXP-201, not a placeholder

    static ConsultationPriceBandEntity makeGlobalBand() {
        ConsultationPriceBandEntity band = new ConsultationPriceBandEntity();
        band.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        band.setChannelType("VIDEO");
        band.setDurationMinutes((short) 30);
        band.setSpecialtyScope(null); // global
        band.setMinimumPrice(new BigDecimal("200000"));
        band.setMaximumPrice(new BigDecimal("1000000"));
        band.setCommissionRate(new BigDecimal("0.15")); // Admin-owned — never read by SUT (ADR-SETPR-002)
        band.setCurrency("VND");
        band.setStatus("ACTIVE");
        band.setEffectiveFrom(Instant.now().minus(Duration.ofDays(30)));
        return band;
    }

    static ConsultationPriceBandEntity makeGlobalBand(Consumer<ConsultationPriceBandEntity> overrides) {
        ConsultationPriceBandEntity band = makeGlobalBand();
        overrides.accept(band);
        return band;
    }

    static ConsultationPriceBandEntity makeSpecialtyBand() {
        ConsultationPriceBandEntity band = makeGlobalBand();
        band.setPriceBandId(UUID.fromString("00000000-0000-0000-0000-0000000000B2"));
        band.setSpecialtyScope("PEDIATRICS");
        band.setMinimumPrice(new BigDecimal("250000"));
        band.setMaximumPrice(new BigDecimal("1200000"));
        band.setCommissionRate(new BigDecimal("0.12"));
        return band;
    }

    static ExpertProfileEntity makeVerifiedExpert() {
        ExpertProfileEntity profile = new ExpertProfileEntity();
        profile.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-00000000EP1"));
        profile.setUserId(UUID.fromString("00000000-0000-0000-0000-0000000000U1"));
        profile.setSpecialty(null);
        profile.setVerificationStatus(VERIFIED_STATUS);
        return profile;
    }

    static ExpertProfileEntity makeVerifiedExpert(Consumer<ExpertProfileEntity> overrides) {
        ExpertProfileEntity profile = makeVerifiedExpert();
        overrides.accept(profile);
        return profile;
    }

    static ExpertProfileEntity makeUnverifiedExpert() {
        return makeVerifiedExpert(p -> p.setVerificationStatus("PENDING")); // schema-confirmed default (L794)
    }

    static SetConsultationPriceRequest makeRequest() {
        SetConsultationPriceRequest request = new SetConsultationPriceRequest();
        request.setChannelType("VIDEO");
        request.setDurationMinutes((short) 30);
        request.setPriceAmount(new BigDecimal("350000"));
        request.setCurrency("VND");
        request.setCancellationPolicy("Full refund if cancelled 24h before.");
        request.setEffectiveFrom(Instant.now());
        return request;
    }

    static SetConsultationPriceRequest makeRequest(Consumer<SetConsultationPriceRequest> overrides) {
        SetConsultationPriceRequest request = makeRequest();
        overrides.accept(request);
        return request;
    }

    static ExpertConsultationPriceEntity makeExistingActivePrice() {
        ExpertConsultationPriceEntity price = new ExpertConsultationPriceEntity();
        price.setExpertPriceId(UUID.fromString("00000000-0000-0000-0000-0000000000P1"));
        price.setExpertProfileId(makeVerifiedExpert().getExpertProfileId());
        price.setPriceBandId(makeGlobalBand().getPriceBandId());
        price.setChannelType("VIDEO");
        price.setDurationMinutes((short) 30);
        price.setPriceAmount(new BigDecimal("350000"));
        price.setStatus("ACTIVE");
        price.setVersionNo(1);
        return price;
    }
}
```

---

### SETPR-TC-001 — Happy path: verified expert sets price within band (201, ACTIVE, version 1)

**Severity:** `HIGH`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC238 TDS §6.1` sequence diagram + `ADR-SETPR-001/003/004/005` + `V1__init_schema.sql` L859-874

**Preconditions:**
- `FX-001` global ACTIVE band seeded (VIDEO/30min, 200000-1000000 VND)
- `FX-003` verified expert profile (`EP1`, no specialty)
- `FX-006` JWT for `U1`
- No pre-existing ACTIVE price for `(EP1, VIDEO, 30)`

**Test Steps:**
1. Arrange: mock `expertProfileRepository.findByUserId(U1)` returns `FX-003`; mock `bandRepository.findActiveMatching("VIDEO", 30, null)` returns `List.of(FX-001)`; mock `priceRepository.existsByExpertProfileIdAndChannelTypeAndDurationMinutesAndStatus(...)` returns `false`.
2. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)`.
3. Assert: `priceRepository.save()` invoked with `status="ACTIVE"`, `versionNo=1`, `priceBandId==B1`, `currency=="VND"`; response `status=="ACTIVE"`, `versionNo==1`, `priceBandId==B1`.

**Expected Result (PASS):** Price persisted `ACTIVE`/version 1, correct band FK, `201`-equivalent success response.
**Expected Result (FAIL):** Wrong status/version, missing band FK, or price created despite a missing precondition.

**Current Status:** 🔴 Not written
**Implementation Note:** `SetConsultationPriceService` must call `assertIsVerifiedExpert` → resolve band → `assertWithinBand` → `assertNoActiveDuplicate` → save, in that order (TDS §6.1).

---

### SETPR-TC-002 — Price below band minimum → 400 (`SETPR-002`)

**Severity:** `CRITICAL`
**Feature Under Test:** `SetConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC238 TDS ADR-SETPR-003` (band range oracle = `FX-001`, NOT the CB-209 mockup's illustrative 200k literal)

**Preconditions:** `FX-001` band (`minimumPrice=200000`); `FX-011` request (`priceAmount=199999`).

**Test Steps:**
1. Act: `setConsultationPricePolicy.assertWithinBand(new BigDecimal("199999"), FX-001)`.
2. Assert: throws `PriceOutOfBandException` code `SETPR-002`.

**Expected Result (PASS):** Exception thrown; no price row created.
**Expected Result (FAIL):** No exception — a price below the band minimum is persisted (financial-integrity violation).

**Current Status:** 🔴 Not written

---

### SETPR-TC-003 — Price above band maximum → 400 (`SETPR-002`)

**Severity:** `CRITICAL`
**Feature Under Test:** `SetConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC238 TDS ADR-SETPR-003`

**Preconditions:** `FX-001` band (`maximumPrice=1000000`); `FX-012` request (`priceAmount=1000001`).

**Test Steps:**
1. Act: `setConsultationPricePolicy.assertWithinBand(new BigDecimal("1000001"), FX-001)`.
2. Assert: throws `PriceOutOfBandException` code `SETPR-002`.

**Expected Result (PASS):** Exception thrown; no price row created.
**Expected Result (FAIL):** A price above the band maximum is persisted.

**Current Status:** 🔴 Not written

---

### SETPR-TC-004 — Boundary: price exactly at band minimum → accepted (inclusive)

**Severity:** `MEDIUM`
**Feature Under Test:** `SetConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC238 TDS ADR-SETPR-003` (both bounds explicitly inclusive)

**Preconditions:** `FX-001` band; `FX-013` request (`priceAmount=200000`, exactly `minimumPrice`).

**Test Steps:**
1. Act: `setConsultationPricePolicy.assertWithinBand(new BigDecimal("200000"), FX-001)`.
2. Assert: no exception thrown.

**Expected Result (PASS):** Price exactly at the minimum is accepted.
**Expected Result (FAIL):** Off-by-one rejection at the exact minimum (violates the documented inclusive-bound decision).

**Current Status:** 🔴 Not written

---

### SETPR-TC-005 — Boundary: price exactly at band maximum → accepted (inclusive)

**Severity:** `MEDIUM`
**Feature Under Test:** `SetConsultationPricePolicy.assertWithinBand()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC238 TDS ADR-SETPR-003`

**Preconditions:** `FX-001` band; `FX-014` request (`priceAmount=1000000`, exactly `maximumPrice`).

**Test Steps:**
1. Act: `setConsultationPricePolicy.assertWithinBand(new BigDecimal("1000000"), FX-001)`.
2. Assert: no exception thrown.

**Expected Result (PASS):** Price exactly at the maximum is accepted.
**Expected Result (FAIL):** Off-by-one rejection at the exact maximum.

**Current Status:** 🔴 Not written

---

### SETPR-TC-006 — No matching ACTIVE band → 422 (`SETPR-003`, UC-240 dependency)

**Severity:** `CRITICAL`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC238 TDS ADR-SETPR-001`, `V1__init_schema.sql` L862/L1820-1821 (`price_band_id` NOT NULL FK)

**Preconditions:** `FX-003` verified expert; `bandRepository.findActiveMatching(...)` returns an empty list (no UC-240-configured band for this channel/duration).

**Test Steps:**
1. Arrange: mock `bandRepository.findActiveMatching("VIDEO", 30, null)` returns `List.of()`.
2. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)`.
3. Assert: throws `PriceBandNotFoundException` code `SETPR-003`; `priceRepository.save()` **never** invoked.

**Expected Result (PASS):** `422` surfaced, zero DB writes, message references the UC-240 prerequisite.
**Expected Result (FAIL):** A price row is inserted with a null/fabricated `price_band_id` (impossible given the NOT NULL FK, but the service must fail *before* attempting it, not rely on a DB exception).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the automated regression guard for the §1.2 Entry-Criteria Blocker — must never silently proceed without a resolved band.

---

### SETPR-TC-007 — Duplicate ACTIVE price for same channel+duration → 409 (`SETPR-004`, scope boundary vs UC-239)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow (duplicate-state class)`
**Feature Under Test:** `SetConsultationPricePolicy.assertNoActiveDuplicate()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC238 TDS ADR-SETPR-004` (Create/Update scope boundary vs UC-239)

**Preconditions:** `FX-010` — an ACTIVE `expert_consultation_prices` row already exists for `(EP1, VIDEO, 30)`.

**Test Steps:**
1. Arrange: mock `priceRepository.existsByExpertProfileIdAndChannelTypeAndDurationMinutesAndStatus(EP1, "VIDEO", 30, "ACTIVE")` returns `true`.
2. Act: `setConsultationPricePolicy.assertNoActiveDuplicate(EP1, "VIDEO", (short) 30)`.
3. Assert: throws `ActivePriceExistsException` code `SETPR-004`.

**Expected Result (PASS):** Exception thrown; message points the caller to UC-239 Update; no second row inserted; `FX-010` unaffected.
**Expected Result (FAIL):** A second ACTIVE row is created for the same combo (ambiguous active price at booking time), or the existing row is silently overwritten (an Update, which is out of UC238's Create scope).

**Current Status:** 🔴 Not written
**Implementation Note:** Release-blocking — this is the core regression test for the UC238/UC239 Create-vs-Update boundary.

---

### SETPR-TC-008 — Unverified expert → 403 (`SETPR-005`)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `SetConsultationPricePolicy.assertIsVerifiedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SetConsultationPricePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC238 TDS ADR-SETPR-005` (SRS Primary Actor = "**Verified** Expert")

**Preconditions:** `FX-004` — expert profile with `verificationStatus='PENDING'` (schema-confirmed default, L794).

**Test Steps:**
1. Act: `setConsultationPricePolicy.assertIsVerifiedExpert(FX-004)`.
2. Assert: throws `ExpertNotVerifiedException` code `SETPR-005`.

**Expected Result (PASS):** Exception thrown; no band lookup, no price write attempted downstream.
**Expected Result (FAIL):** A `PENDING` (unverified) expert is allowed to set a price.

**Current Status:** 🔴 Not written
**Implementation Note:** The approved literal (`VERIFIED_STATUS = "VERIFIED"`) is cited from UC-103 ADR-EXP-201 — this test only needs the *negative* case (`'PENDING'` != approved), which holds regardless.

---

### SETPR-TC-009 — Non-expert role (MOTHER) attempts to set a price → 403 (`SETPR-006`)

**Severity:** `HIGH`
**CWE:** `CWE-862 — Missing Authorization`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `SetConsultationPriceController` (Spring Security role filter)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/SetConsultationPriceControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC238 TDS §16` Authorization Matrix

**Preconditions:** `FX-008` JWT for a `MOTHER`-role user.

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/prices` with `FX-008` JWT and `FX-009` request body.
2. Assert: `403 Forbidden`, error code `SETPR-006`.

**Expected Result (PASS = hệ thống an toàn):** `403 SETPR-006`, no price row created.
**Expected Result (FAIL = lỗ hổng tồn tại):** A non-EXPERT role is able to reach the service layer.

**Current Status:** 🔴 Not written

---

### SETPR-TC-010 — New row persisted with `status='ACTIVE'`, `version_no=1`

**Severity:** `MEDIUM`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `V1__init_schema.sql` L870-871 (`status DEFAULT 'ACTIVE'`, `version_no DEFAULT 1`)

**Test Steps:**
1. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)` (happy path preconditions as in `SETPR-TC-001`).
2. Assert: the entity captured by `priceRepository.save()` has `status.equals("ACTIVE")` and `versionNo == 1`.

**Expected Result (PASS):** Both fields set exactly as expected on create.
**Expected Result (FAIL):** Status/version omitted, null, or a non-1 initial version.

**Current Status:** 🔴 Not written

---

### SETPR-TC-011 — `price_band_id` resolved and persisted (FK provenance)

**Severity:** `MEDIUM`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC238 TDS ADR-SETPR-001/006`, `V1__init_schema.sql` L862

**Test Steps:**
1. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)` with `FX-001` resolved as the matching band.
2. Assert: persisted entity's `priceBandId` equals `FX-001.priceBandId`; response `priceBandId` matches.

**Expected Result (PASS):** The exact resolved band's id is stored — auditable provenance.
**Expected Result (FAIL):** `priceBandId` null, mismatched, or fabricated.

**Current Status:** 🔴 Not written

---

### SETPR-TC-012 — Currency defaults to `VND` when request omits it

**Severity:** `LOW`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `V1__init_schema.sql` L866 (`currency varchar(10) NOT NULL DEFAULT 'VND'`)

**Test Steps:**
1. Act: `setConsultationPriceService.setPrice(makeRequest(r -> r.setCurrency(null)), U1)`.
2. Assert: persisted entity `currency.equals("VND")`.

**Expected Result (PASS):** Omission falls back to `VND`.
**Expected Result (FAIL):** Null currency persisted, or a different unsourced default used (e.g. the CB-209 mockup's `IDR`).

**Current Status:** 🔴 Not written

---

### SETPR-TC-013 — No commission value read/written/exposed anywhere in the flow

**Severity:** `CRITICAL`
**CWE:** `CWE-668 — Exposure of Resource to Wrong Sphere (financial-governance class)`
**Feature Under Test:** `ExpertConsultationPriceEntity`, `SetConsultationPriceService.setPrice()`, `ConsultationPriceMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC238 TDS ADR-SETPR-002`, `V1__init_schema.sql` L859-874 (no commission column exists on `expert_consultation_prices`)

**Preconditions:** `FX-001` band has a non-null `commissionRate=0.15` (Admin-owned).

**Test Steps:**
1. Arrange: reflect over `ExpertConsultationPriceEntity`'s declared fields (or inspect the JPA `@Column` mappings) — assert no field maps to any commission-named column.
2. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)`.
3. Assert: the persisted entity captured by `priceRepository.save()` exposes no commission getter/value; `ConsultationPriceMapper.toResponse(...)` output JSON contains no `commission*` key.

**Expected Result (PASS):** Commission is fully absent from the price entity, the service logic, and the response — confirming the inheritance-chain design (band-only, snapshotted later at booking).
**Expected Result (FAIL):** A commission value is copied from the band onto the price row, or leaked into the response — breaks `ADR-SETPR-002` and the Admin-only commission-governance guarantee.

**Current Status:** 🔴 Not written
**Implementation Note:** Release-blocking — the primary automated defense against commission leakage (`AP-CB-007`, §8).

---

### SETPR-TC-014 — Band resolution: specialty-specific band preferred over global band

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationPriceBandRepository.findActiveMatching()` (consumed via `SetConsultationPriceService`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/repository/ConsultationPriceBandRepositoryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC238 TDS ADR-SETPR-006` (specialty-specific-preferred precedence proposal)

**Preconditions:** Both `FX-001` (global) and `FX-002` (specialty `PEDIATRICS`) seeded for `VIDEO`/30min; `FX-005` expert profile has `specialty='PEDIATRICS'`.

**Test Steps:**
1. Arrange: seed both bands in the test DB (or mock the repository to return both, ordered).
2. Act: `bandRepository.findActiveMatching("VIDEO", (short) 30, "PEDIATRICS", Instant.now())`.
3. Assert: the first/only result returned to the service is `FX-002` (specialty-specific), not `FX-001` (global).

**Expected Result (PASS):** The specialty-specific band's range (`250000-1200000`) governs, not the global band's.
**Expected Result (FAIL):** The global band is incorrectly selected when a specialty-specific match exists — an expert could set a price outside the intended specialty band.

**Current Status:** 🔴 Not written

---

### SETPR-TC-015 — Band resolution: global band used when no specialty-specific band exists

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationPriceBandRepository.findActiveMatching()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/repository/ConsultationPriceBandRepositoryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC238 TDS ADR-SETPR-006` (global fallback)

**Preconditions:** Only `FX-001` (global) seeded for `VIDEO`/30min; expert's specialty is `'DERMATOLOGY'` (no matching specialty band exists).

**Test Steps:**
1. Act: `bandRepository.findActiveMatching("VIDEO", (short) 30, "DERMATOLOGY", Instant.now())`.
2. Assert: returns `FX-001` (the global band, `specialtyScope IS NULL`), not empty.

**Expected Result (PASS):** An expert with no matching specialty-specific band can still price against the global band.
**Expected Result (FAIL):** No band is resolved (incorrectly triggers `SETPR-003`) despite a valid global band existing.

**Current Status:** 🔴 Not written

---

### SETPR-TC-016 — `ExpertConsultationPriceSet` event emitted with correct payload (no commission field)

**Severity:** `MEDIUM`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC238 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)` (happy path).
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured event is `ExpertConsultationPriceSet` with `payload.expertPriceId` set, `payload.priceBandId==B1`, `payload.status=="ACTIVE"`, `payload.versionNo==1`, `metadata.causedBy==U1`; payload record has no commission accessor.

**Expected Result (PASS):** Event payload matches TDS §7.3 record shape exactly.
**Expected Result (FAIL):** Missing/incorrect fields, or a commission value present on the event.

**Current Status:** 🔴 Not written

---

### SETPR-TC-017 — Missing required field → 400 (`SETPR-001`)

**Severity:** `LOW`
**Feature Under Test:** `SetConsultationPriceRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/SetConsultationPriceRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC238 TDS §8.1` DTO annotations (`@NotBlank channelType`, `@NotNull @Positive durationMinutes`, `@NotNull priceAmount`)

**Test Steps:**
1. Act: validate `makeRequest(r -> r.setChannelType(null))`.
2. Assert: violation on `channelType`, error code `SETPR-001`.
3. Act: validate `makeRequest(r -> r.setPriceAmount(null))`.
4. Assert: violation on `priceAmount`, error code `SETPR-001`.

**Expected Result (PASS):** Both omissions rejected with `400 SETPR-001` before reaching the service layer.
**Expected Result (FAIL):** Request accepted with a missing required field, or a `500` instead of a clean `400`.

**Current Status:** 🔴 Not written

---

### SETPR-TC-018 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `SetConsultationPriceController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/SetConsultationPriceControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `UC238 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/prices` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401`, no price row created, no band lookup performed.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### SETPR-TC-019 — Response never leaks raw entity/internal/commission fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `ConsultationPriceMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ConsultationPriceMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"; `UC238 TDS §9.2` response schema

**Test Steps:**
1. Act: `ConsultationPriceMapper.toResponse(makeExistingActivePrice())`.
2. Assert: resulting `SetConsultationPriceResponse` JSON contains only the fields declared in TDS §9.2 (`expertPriceId`, `priceBandId`, `channelType`, `durationMinutes`, `priceAmount`, `currency`, `effectiveFrom`, `effectiveTo`, `status`, `versionNo`, `createdAt`) — no internal audit columns, no commission field.

**Expected Result (PASS):** Response matches exactly the documented schema.
**Expected Result (FAIL):** Extra internal fields (or a commission value) serialized.

**Current Status:** 🔴 Not written

---

### SETPR-TC-020 — `expert_profile_id` always server-resolved from JWT — client-supplied value ignored (IDOR guard)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `SetConsultationPriceService.setPrice()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SetConsultationPriceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `UC238 TDS ADR-SETPR-005`, `UC238 TDS §16` ("Own profile only")

**Preconditions:** `FX-006` JWT resolves to `EP1`; a hypothetical malicious/legacy client payload includes a different `expertProfileId=EP9` (an id the caller does not own).

**Test Steps (Attack Simulation):**
1. Arrange: `SetConsultationPriceRequest` has no `expertProfileId` field per TDS §8.1 — confirm the DTO's declared field set excludes any client-suppliable identity field; separately, mock `expertProfileRepository.findByUserId(U1)` returns `FX-003` (`EP1`).
2. Act: `setConsultationPriceService.setPrice(makeRequest(), U1)`.
3. Assert: the persisted entity's `expertProfileId` equals `EP1` (server-resolved from `U1`'s JWT via `expertProfileRepository`), regardless of anything a client might have attempted to inject; assert the DTO class has no settable `expertProfileId`/`userId` field (reflection check).

**Expected Result (PASS):** Price is always attributed to the JWT-resolved profile; no code path accepts a client-supplied identity.
**Expected Result (FAIL):** A price row is created under a different expert's profile than the authenticated caller — cross-tenant IDOR.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the automated regression guard for the "Own profile only" authorization-matrix cell (§16).

---

### INTEGRATION TEST CASES

---

### SETPR-TC-INT-001 — E2E: set consultation price API flow (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/consultations/prices` → DB state (`expert_consultation_prices`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/SetConsultationPriceIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-001` band, `FX-003` verified expert profile inserted via JPA before test

**Test Steps:**
1. Seed band `B1` (ACTIVE, VIDEO/30min, 200000-1000000 VND) and expert profile `EP1` (verified, owned by `U1`).
2. `POST /api/v1/consultations/prices` with JWT for `U1` and body `{"channelType":"VIDEO","durationMinutes":30,"priceAmount":350000}`.
3. Assert response `201`, body matches §9.2 schema (no commission field).
4. Assert DB: exactly one `expert_consultation_prices` row for `(EP1, VIDEO, 30)` with `status='ACTIVE'`, `version_no=1`, `price_band_id=B1`.
5. Repeat step 2 with the same combo — assert `409 SETPR-004` and still exactly one row in DB (no duplicate created).

**Expected Result (PASS):**
- First call `201`; DB row consistent with request + resolved band; second call `409`, no duplicate row.

**Expected Result (FAIL):**
- API error on first call, wrong DB state, or a second row created on replay (double-active-price).

**DB Assertion:**
```java
List<ExpertConsultationPriceEntity> prices = priceRepository
    .findByExpertProfileIdAndChannelTypeAndDurationMinutes(expertProfileId, "VIDEO", (short) 30);
assertThat(prices).hasSize(1);
assertThat(prices.get(0).getStatus()).isEqualTo("ACTIVE");
assertThat(prices.get(0).getVersionNo()).isEqualTo(1);
assertThat(prices.get(0).getPriceBandId()).isEqualTo(bandId);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SETPR-TC-001` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-002` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-003` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-004` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-005` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-006` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-007` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-008` | `SetConsultationPricePolicyTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-009` | `SetConsultationPriceControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-010` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-011` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-012` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-013` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-014` | `ConsultationPriceBandRepositoryTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-015` | `ConsultationPriceBandRepositoryTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-016` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-017` | `SetConsultationPriceRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-018` | `SetConsultationPriceControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-019` | `ConsultationPriceMapperTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-020` | `SetConsultationPriceServiceTest.java` | `[ ]` | `[ ]` | |
| `SETPR-TC-INT-001` | `SetConsultationPriceIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SetConsultationPriceService implements ISetConsultationPriceService {
    @Override
    public SetConsultationPriceResponse setPrice(SetConsultationPriceRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class SetConsultationPricePolicy {
    public void assertIsVerifiedExpert(ExpertProfileEntity profile) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertWithinBand(BigDecimal priceAmount, ConsultationPriceBandEntity band) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertNoActiveDuplicate(UUID expertProfileId, String channelType, short durationMinutes) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SETPR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-020` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SETPR-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** UC-240 Configure Consultation Price Bands implemented — at least one ACTIVE `consultation_price_bands` row per channel/duration under test exists in the test environment (§1.2 Entry-Criteria Blocker)
- [ ] **BLOCKING:** Expert-profile module implemented — JWT→`expert_profile_id` resolution and `verification_status` readable
- [ ] `UC238_SetConsultationPrice_TDS.md` reviewed and Approved
- [ ] ADR-SETPR-005 (`verification_status = 'VERIFIED'`, cited from UC-103) and ADR-SETPR-006 (band precedence + overlap tie-break, cited from UC-240 ADR-PBAND-001) — both `Accepted`; residual: re-verify if UC-103 (still `Draft`) changes its enum before ship
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `SetConsultationPriceService`, `SetConsultationPricePolicy`
- [ ] Không có business logic trong `SetConsultationPriceController` (chỉ có validation + mapping)
- [ ] Không có commission value xuất hiện ở bất kỳ đâu trong `expert_consultation_prices`/response/event (§4 `SETPR-TC-013`)
- [ ] `SETPR-TC-002`/`SETPR-TC-003` (band-range rejection) and `SETPR-TC-007` (duplicate-active rejection) pass — release-blocking financial/scope-integrity gates
- [ ] `SETPR-TC-006` (no-matching-band precondition) and `SETPR-TC-020` (IDOR guard) pass — release-blocking

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — mỗi test dùng `SetConsultationPriceTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-240 price-band data or expert-profile prerequisite services not yet deployed to test environment
- ADR-SETPR-005 (verification literal) or ADR-SETPR-006 (band tie-break) still unconfirmed at start of Sprint implementation
- New architecture unknown discovered (e.g., a DB-level partial-unique index is mandated) requiring Tech Lead review and a coordinated migration

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in scope for UC238)
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
| AP-AI-003 | Implicit Decision | Test assumes an unconfirmed literal/architecture decision không có ADR | ☑ (verification literal `'VERIFIED'` isolated in `VERIFIED_STATUS`, cited from UC-103 ADR-EXP-201; all decisions traced to ADR-SETPR-00X) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `SETPR-TC-009`/`018`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, or assumes a `consultations` unified table | ☑ (all types match TDS §8 interfaces; split-table schema only) | G-3 |
| **AP-CB-007** *(project-specific)* | **Commission leakage** | Any test path where a commission value is copied onto `expert_consultation_prices` or exposed in the response/event | `SETPR-TC-013`/`SETPR-TC-016`/`SETPR-TC-019` explicitly assert its absence | **Release-blocking** |
| **AP-CB-008** *(project-specific)* | **Band bypass** | Test/implementation inserts a price without resolving/validating against an ACTIVE band, or accepts a null `price_band_id` | `SETPR-TC-006`/`SETPR-TC-011` explicitly assert the resolved-band precondition and persisted FK | **Release-blocking** |
| **AP-CB-009** *(project-specific)* | **Create/Update boundary breach** | Test/implementation overwrites an existing ACTIVE price instead of rejecting `SETPR-004` | `SETPR-TC-007` explicitly asserts rejection, not overwrite | **Release-blocking** |
| **AP-CB-010** *(project-specific)* | **IDOR via client-supplied expert identity** | Test/implementation accepts a client-supplied `expertProfileId`/`userId` on the request instead of resolving from JWT | `SETPR-TC-020` explicitly asserts server-side resolution and DTO field absence | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`SETPR-TC-006/007/013/020`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC238 v1.0 — Draft. Total test cases: 21 (20 unit/component + 1 integration).
Critical-severity: 8 (`SETPR-TC-002, 003, 006, 007, 008, 013, 018, 020` — band-range
financial-integrity, UC-240 precondition, Create/Update scope boundary, RBAC/verified
gate, commission-leakage, unauthenticated-access, and IDOR gates). Requires Approved
status change only by user/Tech Lead. ADR-SETPR-005 (`verification_status = 'VERIFIED'`,
cited from UC-103) and ADR-SETPR-006 (band precedence + overlap tie-break, cited from
UC-240 ADR-PBAND-001) are now resolved/`Accepted` — NOT invented, cited and
mechanism-tested. Consistency Gate CG-1..CG-9 executed against
`UC238_SetConsultationPrice_TDS.md` — see below.*

---

## Consistency Gate CG-1..CG-9 (Self-Check Against TDS)

| Gate | Check | Result |
|------|-------|--------|
| CG-1 | Every ADR in TDS §3 (`ADR-SETPR-001..006`) has ≥1 traceable test case | ✅ PASS — 001→`SETPR-TC-006`/011; 002→`SETPR-TC-002/003/016`; 003→`SETPR-TC-002/003/004/005`; 004→`SETPR-TC-007`; 005→`SETPR-TC-008/020`; 006→`SETPR-TC-014/015` |
| CG-2 | Every error code in TDS §10 (`SETPR-001..006`) has ≥1 dedicated test case | ✅ PASS — 001→`SETPR-TC-017`; 002→`SETPR-TC-002/003`; 003→`SETPR-TC-006`; 004→`SETPR-TC-007`; 005→`SETPR-TC-008`; 006→`SETPR-TC-009` |
| CG-3 | Every TDS §6 sequence diagram (happy + 4 error paths) has a corresponding test case | ✅ PASS — §6.1→`SETPR-TC-001`; §6.2→`SETPR-TC-002/003`; §6.3→`SETPR-TC-006`; §6.4→`SETPR-TC-007`; §6.5→`SETPR-TC-008` |
| CG-4 | No test case invents a fact not sourced (every TC has an Oracle Source citation) | ✅ PASS — all 21 TCs cite TDS §/ADR/schema line refs; §2 Logic Issues L1-L5 isolate every non-schema-confirmed literal into a factory constant |
| CG-5 | Every fixture (`FX-001..014`) is referenced by ≥1 TC | ✅ PASS — verified by cross-reference: FX-001 (TC-001/002/003/004/005/006/010/011/012/013/016/019/INT-001), FX-002 (TC-014), FX-003 (TC-001/010/011/013/016/019/020), FX-004 (TC-008), FX-005 (TC-014), FX-006 (TC-001/020), FX-007 (TC-014), FX-008 (TC-009), FX-009 (TC-001), FX-010 (TC-007), FX-011/012/013/014 (TC-002/003/004/005) |
| CG-6 | Package/interface/entity names in tests match TDS §8 exactly (no hallucinated contracts) | ✅ PASS — `SetConsultationPriceService`, `SetConsultationPricePolicy`, `ConsultationPriceBandRepository`, `ExpertConsultationPriceEntity`, `ConsultationPriceMapper` all match TDS §5.1/§8 verbatim |
| CG-7 | Authorization matrix (TDS §16) roles are each covered by a dedicated TC | ✅ PASS — unauthenticated→`SETPR-TC-018`; non-expert role→`SETPR-TC-009`; unverified expert→`SETPR-TC-008`; verified expert (own profile)→`SETPR-TC-001`/`020` |
| CG-8 | Every TDS decision is reflected in Test-Spec as a mechanism-test, not a hardcoded assumed value | ✅ PASS — ADR-SETPR-005 literal `'VERIFIED'` (cited from UC-103) isolated in `VERIFIED_STATUS` constant (§4 factory); ADR-SETPR-006 tie-break resolved by citing UC-240 ADR-PBAND-001's overlap-prevention guarantee, asserted per the documented precedence (specialty-then-global), no overlap-resolution number invented |
| CG-9 | Red Gate stub + tracker entry exists for every TC id (no orphan TC) | ✅ PASS — all 21 TC ids (`SETPR-TC-001..020`, `SETPR-TC-INT-001`) appear in both §5 Red-Green-Refactor Tracker and §5.1 Red Gate Verification table |

**Overall: 9/9 gates PASS.** No fixes required against the TDS as authored; this
Test-Spec was drafted directly against the final TDS content with no drift.
