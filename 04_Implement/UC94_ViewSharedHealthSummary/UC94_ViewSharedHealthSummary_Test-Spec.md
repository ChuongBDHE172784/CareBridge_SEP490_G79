# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC94 — View Shared Health Summary — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-094`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L164-195, L695-708, L876-896) — primary schema source
- `04_Implement/UC94_ViewSharedHealthSummary/UC94_ViewSharedHealthSummary_TDS.md` (`CB-CONSULTATION-IMP-094`) — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_Test-Spec.md` — sibling spec, same batch, style reference and shared ownership pattern
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.8 (L901-920) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, consent scope/expiry, audit mandates)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC94 |

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
| **Feature / Gap ID** | `UC-94` |
| **Module** | `Consultation — Shared Health Summary Access` (Bounded Context: `Consultation`, reads Health domain's `health_summaries`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-094` |
| **Priority** | 🔴 High |
| **Sprint** | `Sprint 4 "Real Providers And Admin Polish"` — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `Sensitive-PII` (maternal/baby health data shared into a consultation context) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `UC-75/76` booking creation (populates `shared_summary_id` + `consent_grants`), Health domain (`health_summaries` generation, out of scope) |
| **Downstream Consumers** | `UC-96 WriteConsultationSummary` (read-only influence, no code dependency) |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TypeScript/Vite) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC94_ViewSharedHealthSummary_TDS.md §17`, ADR-SUMMARY-ACCESS-001/002/003 |
| **Constraints Injected** | Consent-grant-gated access (C1), ownership before consent-check ordering (C2), minimum-necessary DTO projection (C3), every access individually audited (C4), no authorization caching (C5) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-94 wording ("user-granted scope and time limit") does not name a concrete enforcement mechanism | TDS ADR-SUMMARY-ACCESS-001 selects `consent_grants` (`data_type='EXPERT_SHARED_DATA'`, already a valid CHECK-constraint value) over a session-linked implicit expiry | Tests assert access is gated by `consent_grants.revoked_at IS NULL AND expiry_at > now()`, NOT by `consultation_sessions.session_status` |
| L2 | `consent_grants.recipient` is free-text `varchar(120)` with no FK to `expert_profiles`/booking | TDS proposes (Open, non-blocking) storing `recipient = expertProfileId.toString()` as the matching convention | Tests seed `recipient` as the string form of `EXPERT_PROFILE_ID_1` per this documented convention; a test explicitly marks this assumption in its Oracle Source comment so a future TDS correction is easy to locate |
| L3 | No documented field-level allowlist for `health_summaries.summary_json` → Expert-facing projection | TDS §4.3/§5.3 gap note 2 flags this as Open, pending Product/DPO input | Tests assert the MECHANISM (mapper never returns `ownerUserId`/`journeyId`/`babyId` raw) rather than a specific field allowlist, since the exact allowlist is not yet confirmed |
| L4 | Ownership vs. consent check ordering not explicit in SRS | TDS invariant #3 (§6.5): ownership is checked BEFORE consent, so a non-assigned Expert never learns whether a summary was shared (`403` before any `404`/consent-specific `403`) | `SUM-TC-008` explicitly asserts the ordering — a non-assigned Expert always receives the SAME `SUM-004` regardless of whether a summary was actually shared, preventing information leakage about sharing status |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Shared Health Summary Access bao gồm các layer:
├── Domain (SharedHealthSummaryPolicy — pure logic, no deps)
├── Application / Use Cases (SharedHealthSummaryService — mock JPA Repositories với Mockito)
├── Controller (SharedHealthSummaryController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full consent-gated read flow)
└── Web (SharedHealthSummaryPanel.tsx — Vitest + Testing Library, MSW for API mocking)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-94` (§3.2.1.8, L901-920) | Displays shared summaries/records within user-granted scope and time limit |
| `ADR-SUMMARY-ACCESS-001` | Consent-grant-gated access mechanism, no caching |
| `ADR-SUMMARY-ACCESS-002` | Ownership — assigned, verified Expert only |
| `ADR-SUMMARY-ACCESS-003` | Every view (success or denial) individually audited |
| `BR-RBAC` / `BR-PRIVACY` | Role/ownership-scoped, minimum-necessary access |
| `CB-CONSULTATION-IMP-094 §9/§10/§16` | API contract, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — assigned verified Expert views summary under active consent | `SharedHealthSummaryService.getSharedSummary()` | `SUM-TC-001` |
| TC-COND-002 | Non-assigned Expert attempts view → 403 (`SUM-004`) | `SharedHealthSummaryPolicy.assertIsAssignedExpert()` | `SUM-TC-002` |
| TC-COND-003 | Unverified assigned Expert attempts view → 403 (`SUM-004`) | `SharedHealthSummaryPolicy.assertIsAssignedExpert()` | `SUM-TC-003` |
| TC-COND-004 | No summary shared (`shared_summary_id IS NULL`) → 404 (`SUM-006`) | `SharedHealthSummaryService.getSharedSummary()` | `SUM-TC-004` |
| TC-COND-005 | Consent expired → 403 (`SUM-005`) | `SharedHealthSummaryPolicy.assertConsentActive()` | `SUM-TC-005` |
| TC-COND-006 | Consent revoked → 403 (`SUM-005`) | `SharedHealthSummaryPolicy.assertConsentActive()` | `SUM-TC-006` |
| TC-COND-007 | Booking not found → 404 (`SUM-003`) | `SharedHealthSummaryService.getSharedSummary()` | `SUM-TC-007` |
| TC-COND-008 | Ownership checked BEFORE consent — non-assigned Expert never learns sharing status | `SharedHealthSummaryService.getSharedSummary()` (check ordering) | `SUM-TC-008` |
| TC-COND-009 | Minimum-necessary projection — raw `ownerUserId`/`journeyId`/`babyId` never exposed | `SharedHealthSummaryMapper` | `SUM-TC-009` |
| TC-COND-010 | `SharedHealthSummaryViewed` event emitted with correct `consentGrantId` payload | `SharedHealthSummaryService` | `SUM-TC-010` |
| TC-COND-011 | `SharedHealthSummaryAccessDenied` event emitted on denial | `SharedHealthSummaryService` | `SUM-TC-011` |
| TC-COND-012 | Access re-evaluated on every request — no authorization caching (boundary: 1s after `expiry_at`) | `SharedHealthSummaryPolicy.assertConsentActive()` | `SUM-TC-012` |
| TC-COND-013 | Downstream DB read failure/timeout → 503 (`SUM-007`) | `SharedHealthSummaryService.getSharedSummary()` | `SUM-TC-013` |
| TC-COND-014 | Full flow via Testcontainers — active consent view, then revoke, then re-view denies | `SharedHealthSummaryController` + real DB | `SUM-TC-INT-001` |
| TC-COND-015 | Web: panel renders read-only summary fields | `SharedHealthSummaryPanel.tsx` | `SUM-TC-WEB-001` |
| TC-COND-016 | Web: 404 (no summary shared) renders graceful empty state | `SharedHealthSummaryPanel.tsx` | `SUM-TC-WEB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role (assigned/non-assigned/unverified), consent state (active/expired/revoked/none) | Covers each authorization/consent class with one representative case |
| Boundary Value Analysis | `expiry_at` boundary (1s before vs. 1s after) | "No grace period" is a hard invariant (ADR-SUMMARY-ACCESS-001 invariant #2) |
| Decision Table | Ownership × consent-state × summary-existence combinations | 200 vs 403 (ownership) vs 403 (consent) vs 404 (no summary) branching |
| Error Guessing | DB timeout injection, information-leakage probing via check-ordering | External-service failure and IDOR/info-leak are explicitly in scope |
| Security Testing | Ownership-before-consent ordering (`SUM-TC-008`), minimum-necessary field leakage (`SUM-TC-009`) | Direct third-party access to health data — highest-risk surface in this batch |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-S01` | DB seed | `consultation_bookings{booking_id: B1, expert_profile_id: EXPERT-001, requester_user_id: MOTHER-001, shared_summary_id: SUMMARY-001}` | Happy path booking with shared summary |
| `FX-S02` | DB seed | `health_summaries{summary_id: SUMMARY-001, owner_user_id: MOTHER-001, summary_json: {...}}` | Target summary row |
| `FX-S03` | DB seed | `consent_grants{user_id: MOTHER-001, data_type: 'EXPERT_SHARED_DATA', purpose: 'VIEW', recipient: 'EXPERT-001', revoked_at: NULL, expiry_at: now()+7d}` | Active consent grant |
| `FX-S04` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status: 'VERIFIED'}` | Assigned, verified Expert |
| `FX-S05` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status: 'PENDING'}` | Unverified Expert (negative case) |
| `FX-S06` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned |
| `FX-S07` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned |
| `FX-S08` | DB seed | `consent_grants{..., expiry_at: now()-1s}` | Just-expired consent (boundary) |
| `FX-S09` | DB seed | `consent_grants{..., revoked_at: now()-1h}` | Revoked consent |
| `FX-S10` | DB seed | `consultation_bookings{..., shared_summary_id: NULL}` | No summary shared |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership-before-consent ordering, minimum-necessary projection) |
| Web (React) | — | — | ✅ (Vitest) | ✅ (Testing Library) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

class SharedHealthSummaryTestFactory {

    static final UUID BOOKING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SUMMARY_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000601");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(MOTHER_USER_ID);
        booking.setSharedSummaryId(SUMMARY_ID_1);
        overrides.accept(booking);
        return booking;
    }

    static HealthSummaryEntity makeSummary() {
        HealthSummaryEntity summary = new HealthSummaryEntity();
        summary.setSummaryId(SUMMARY_ID_1);
        summary.setOwnerUserId(MOTHER_USER_ID);
        summary.setSummaryPeriod("TRIMESTER_2");
        summary.setSummaryJson(Map.of("weightTrend", "within normal range"));
        summary.setStatus("ACTIVE");
        return summary;
    }

    static ConsentGrantEntity makeActiveConsentGrant(Consumer<ConsentGrantEntity> overrides) {
        ConsentGrantEntity grant = new ConsentGrantEntity();
        grant.setId(1L);
        grant.setUserId(MOTHER_USER_ID);
        grant.setDataType("EXPERT_SHARED_DATA");
        grant.setPurpose("VIEW");
        grant.setRecipient(EXPERT_PROFILE_ID_1.toString()); // L2 — documented recipient convention
        grant.setConsentGivenAt(Instant.now().minus(Duration.ofDays(1)));
        grant.setExpiryAt(Instant.now().plus(Duration.ofDays(7)));
        grant.setRevokedAt(null);
        overrides.accept(grant);
        return grant;
    }

    static ExpertProfileEntity makeVerifiedExpertProfile() {
        ExpertProfileEntity profile = new ExpertProfileEntity();
        profile.setExpertProfileId(EXPERT_PROFILE_ID_1);
        profile.setUserId(EXPERT_USER_ID_1);
        profile.setVerificationStatus("VERIFIED");
        return profile;
    }

    static ExpertProfileEntity makeUnverifiedExpertProfile() {
        ExpertProfileEntity profile = makeVerifiedExpertProfile();
        profile.setVerificationStatus("PENDING");
        return profile;
    }
}
```

---

### SUM-TC-001 — Happy path: assigned verified Expert views summary under active consent

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC94 TDS §6.1` sequence diagram, `ADR-SUMMARY-ACCESS-001/002`

**Preconditions:** `FX-S01`, `FX-S02`, `FX-S03` (active grant), `FX-S04` (verified Expert)

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-S01`; mock `consentGrantRepository.findActiveByUserIdAndDataType(MOTHER_USER_ID, "EXPERT_SHARED_DATA")` returns `[FX-S03]`; mock `healthSummaryRepository.findById(SUMMARY_ID_1)` returns `FX-S02`.
2. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)`.
3. Assert: response `summaryId == SUMMARY_ID_1`, `consentExpiresAt == FX-S03.expiryAt`.

**Expected Result (PASS):** `200`-equivalent response with correct summary + consent expiry surfaced.
**Expected Result (FAIL):** Exception thrown, or wrong summary/consent data returned.

**Current Status:** 🔴 Not written

---

### SUM-TC-002 — Non-assigned Expert attempts view → 403 (`SUM-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `SharedHealthSummaryPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SharedHealthSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SUMMARY-ACCESS-002`

**Preconditions:** `FX-S01` booking assigned to `EXPERT_PROFILE_ID_1`; caller = `OTHER_EXPERT_USER_ID`

**Test Steps:**
1. Arrange: `booking = SharedHealthSummaryTestFactory.makeBooking(b -> {})`.
2. Act: `policy.assertIsAssignedExpert(booking, OTHER_EXPERT_USER_ID)`.
3. Assert: throws `SummaryAuthorizationException` code `SUM-004`.

**Expected Result (PASS):** Exception thrown before any consent/summary lookup occurs.
**Expected Result (FAIL):** No exception / summary returned to non-assigned Expert.

**Current Status:** 🔴 Not written

---

### SUM-TC-003 — Unverified assigned Expert attempts view → 403 (`SUM-004`)

**Severity:** `HIGH`
**Feature Under Test:** `SharedHealthSummaryPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SharedHealthSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SUMMARY-ACCESS-002` — "Verified Expert" actor requirement

**Preconditions:** `FX-S05` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`.
2. Act: `policy.assertIsAssignedExpert(booking, EXPERT_USER_ID_2)`.
3. Assert: throws `SummaryAuthorizationException` (`SUM-004`).

**Expected Result (PASS):** Rejected despite matching `user_id`, because `verification_status != 'VERIFIED'`.
**Expected Result (FAIL):** Unverified expert allowed to view.

**Current Status:** 🔴 Not written

---

### SUM-TC-004 — No summary shared for this booking → 404 (`SUM-006`)

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC94 TDS §6.2` alternative-flow sequence diagram (SRS AF2)

**Preconditions:** `FX-S10` (`shared_summary_id IS NULL`)

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns booking with `sharedSummaryId=null`; ownership check passes.
2. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)`.
3. Assert: throws `NoSummarySharedException` code `SUM-006`.

**Expected Result (PASS):** `404` — this is NOT an error state per SRS AF2, just an empty-state signal.
**Expected Result (FAIL):** `500` or an exception unrelated to `SUM-006`.

**Current Status:** 🔴 Not written

---

### SUM-TC-005 — Consent expired → 403 (`SUM-005`)

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedHealthSummaryPolicy.assertConsentActive()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SharedHealthSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC94 TDS §6.3` error-path sequence diagram, `ADR-SUMMARY-ACCESS-001`

**Preconditions:** `FX-S08` (`expiry_at = now()-1s`)

**Test Steps:**
1. Arrange: `consentGrantRepository.findActiveByUserIdAndDataType(...)` query predicate excludes expired rows (per repository `@Query`), so it returns `List.of()`.
2. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)`.
3. Assert: throws `ConsentExpiredException` code `SUM-005`.

**Expected Result (PASS):** Access denied immediately at/after `expiry_at` — no grace period.
**Expected Result (FAIL):** Summary returned despite expired consent.

**Current Status:** 🔴 Not written

---

### SUM-TC-006 — Consent revoked → 403 (`SUM-005`)

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedHealthSummaryPolicy.assertConsentActive()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SharedHealthSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SUMMARY-ACCESS-001`

**Preconditions:** `FX-S09` (`revoked_at` set 1 hour ago)

**Test Steps:**
1. Arrange: query predicate `revoked_at IS NULL` excludes this row → repository returns `List.of()`.
2. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)`.
3. Assert: throws `ConsentExpiredException` code `SUM-005` (same code as expiry — caller cannot distinguish revocation from natural expiry, by design, to avoid leaking revocation-timing signals).

**Expected Result (PASS):** Access denied immediately after revocation, regardless of remaining `expiry_at` window.
**Expected Result (FAIL):** Summary returned despite revocation.

**Current Status:** 🔴 Not written

---

### SUM-TC-007 — Booking not found → 404 (`SUM-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC94 TDS §10` error table

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `service.getSharedSummary(nonExistentId, EXPERT_USER_ID_1)`.
3. Assert: throws `BookingNotFoundException` code `SUM-003`.

**Current Status:** 🔴 Not written

---

### SUM-TC-008 — Ownership checked BEFORE consent: non-assigned Expert never learns sharing status

**Severity:** `HIGH`
**CWE:** `CWE-203 — Observable Discrepancy (information leakage)`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()` (check ordering)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC94 TDS §6.5` invariant #3

**Test Steps:**
1. Arrange (Case A): booking HAS a shared summary (`FX-S01`), caller is `OTHER_EXPERT_USER_ID` (non-assigned).
2. Arrange (Case B): booking has NO shared summary (`FX-S10`), caller is `OTHER_EXPERT_USER_ID` (non-assigned).
3. Act: `service.getSharedSummary(...)` for both cases.
4. Assert: BOTH cases throw the identical `SummaryAuthorizationException` code `SUM-004` — never `SUM-006` in Case B, since ownership is checked first and fails identically regardless of whether a summary exists.
5. Assert (via Mockito `verifyNoInteractions`): neither `consentGrantRepository` nor `healthSummaryRepository` is ever queried when ownership fails.

**Expected Result (PASS):** Identical `SUM-004` response in both cases — no distinguishable signal about sharing status leaks to an unauthorized caller.
**Expected Result (FAIL):** Case B returns a different error code (e.g., `SUM-006`), revealing to a non-assigned Expert that "no summary was shared" versus "you're not assigned" — an information-leakage vulnerability.

**Current Status:** 🔴 Not written

---

### SUM-TC-009 — Minimum-necessary projection: raw `ownerUserId`/`journeyId`/`babyId` never exposed

**Severity:** `CRITICAL`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `SharedHealthSummaryMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/SharedHealthSummaryMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"; `ADR-SUMMARY-ACCESS-001` minimum-necessary principle

**Test Steps:**
1. Act: `SharedHealthSummaryMapper.toResponse(SharedHealthSummaryTestFactory.makeSummary(), grant, bookingId)`.
2. Assert: resulting `SharedHealthSummaryResponse` JSON serialization contains no field named `ownerUserId`, `journeyId`, or `babyId` — matches exactly the field set declared in `UC94 TDS §9.2`.

**Expected Result (PASS):** Response matches exactly the documented `SharedHealthSummaryResponse` fields.
**Expected Result (FAIL):** Any raw entity identifier field (owner/journey/baby) leaks into the response.

**Current Status:** 🔴 Not written

---

### SUM-TC-010 — `SharedHealthSummaryViewed` event emitted with correct `consentGrantId` payload

**Severity:** `HIGH`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC94 TDS §7.1/§7.3`, `ADR-SUMMARY-ACCESS-003`

**Test Steps:**
1. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)` (happy path).
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `SharedHealthSummaryViewed` with `payload.bookingId==B1`, `payload.summaryId==SUMMARY_ID_1`, `payload.consentGrantId==1L`, `payload.viewedByUserId==EXPERT_USER_ID_1`.

**Current Status:** 🔴 Not written

---

### SUM-TC-011 — `SharedHealthSummaryAccessDenied` event emitted on denial

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC94 TDS §6.3` — "Denied attempts ARE logged"

**Test Steps:**
1. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)` under an expired-consent fixture (`FX-S08`).
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `SharedHealthSummaryAccessDenied` with `payload.reason=="CONSENT_EXPIRED_OR_REVOKED"`.

**Current Status:** 🔴 Not written

---

### SUM-TC-012 — Access re-evaluated on every request: 1s-before vs. 1s-after `expiry_at` boundary

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedHealthSummaryPolicy.assertConsentActive()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/SharedHealthSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC94 TDS §6.5` invariant #1/#2 — "no grace period"

**Test Steps:**
1. Arrange: `grant = makeActiveConsentGrant(g -> g.setExpiryAt(Instant.now().plusSeconds(1)))`.
2. Act (before expiry): `policy.assertConsentActive(grant)` — assert no exception.
3. Arrange: advance clock (or construct a second grant with `expiryAt = Instant.now().minusSeconds(1)`).
4. Act (after expiry): `policy.assertConsentActive(expiredGrant)` — assert throws `ConsentExpiredException`.

**Expected Result (PASS):** Boundary respected exactly — no caching of a prior "active" decision carries over past `expiry_at`.
**Expected Result (FAIL):** Grace period exists, or a cached authorization decision is reused past expiry.

**Current Status:** 🔴 Not written

---

### SUM-TC-013 — Downstream DB read failure/timeout → 503 (`SUM-007`)

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryService.getSharedSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/SharedHealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC94 TDS §6.4` timeout sequence diagram (SRS E3)

**Test Steps:**
1. Arrange: ownership + consent checks pass; mock `healthSummaryRepository.findById(...)` throws `DataAccessException`.
2. Act: `service.getSharedSummary(B1, EXPERT_USER_ID_1)`.
3. Assert: throws `SummaryReadUnavailableException` code `SUM-007`.

**Expected Result (PASS):** `503` surfaced, safe to retry (read-only, no state mutation risk).
**Expected Result (FAIL):** Exception swallowed silently, or wrong error code.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SUM-TC-INT-001 — Full flow: active consent view succeeds, revoke, then re-view denies

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `GET /api/v1/consultations/bookings/{id}/shared-summary` → DB-backed consent re-evaluation
**Test File:** `src/test/java/com/carebridge/backend/consultation/SharedHealthSummaryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-S01`, `FX-S02`, `FX-S03` (active grant) inserted via JPA

**Test Steps:**
1. Seed booking + summary + active consent grant.
2. `GET /api/v1/consultations/bookings/{B1}/shared-summary` with Expert JWT — assert `200`, body matches §9.2 schema.
3. Directly `UPDATE consent_grants SET revoked_at = now() WHERE id = 1` (simulating the Mother revoking consent via a separate flow, out of scope here).
4. `GET .../shared-summary` again with the SAME Expert JWT — assert `403` (`SUM-005`).

**Expected Result (PASS):** First call succeeds; second call (post-revocation) is denied — proving no authorization caching across requests.
**Expected Result (FAIL):** Second call still succeeds despite the DB-level revocation.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### SUM-TC-WEB-001 — SharedHealthSummaryPanel renders read-only summary fields

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryPanel.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/SharedHealthSummaryPanel.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC94 TDS §5.1` Web Component, `§9.2` response schema

**Preconditions:** MSW mock server configured for `GET /api/v1/consultations/bookings/:id/shared-summary` → 200 with `FX-S02`-equivalent payload

**Test Steps:**
1. Render `<SharedHealthSummaryPanel bookingId={B1} />` with a mocked TanStack Query client and MSW handler.
2. Assert: rendered text includes `summaryPeriod`, `summaryFields` values; no edit/submit controls present (read-only panel).

**Expected Result (PASS):** Panel displays summary data without any mutation affordance.
**Expected Result (FAIL):** Panel renders raw entity JSON, or exposes an edit action (out of scope for a read-only view).

**Current Status:** 🔴 Not written

---

### SUM-TC-WEB-002 — 404 (no summary shared) renders graceful empty state

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedHealthSummaryPanel.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/SharedHealthSummaryPanel.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC94 TDS §6.2` AF2, SRS "empty state with next allowed action"

**Preconditions:** MSW mock returns `404 {code: "SUM-006"}`

**Test Steps:**
1. Render `<SharedHealthSummaryPanel bookingId={B1} />`, MSW configured to return 404.
2. Assert: panel shows an empty-state message (e.g., "No health summary was shared for this consultation") — NOT a generic error crash.

**Expected Result (PASS):** Graceful, informative empty state.
**Expected Result (FAIL):** Unhandled error boundary / blank panel.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SUM-TC-001` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-002` | `SharedHealthSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-003` | `SharedHealthSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-004` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-005` | `SharedHealthSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-006` | `SharedHealthSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-007` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-008` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-009` | `SharedHealthSummaryMapperTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-010` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-011` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-012` | `SharedHealthSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-013` | `SharedHealthSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-INT-001` | `SharedHealthSummaryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `SUM-TC-WEB-001` | `SharedHealthSummaryPanel.test.tsx` | `[ ]` | `[ ]` | |
| `SUM-TC-WEB-002` | `SharedHealthSummaryPanel.test.tsx` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SharedHealthSummaryService implements ISharedHealthSummaryService {
    @Override
    public SharedHealthSummaryResponse getSharedSummary(UUID bookingId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SUM-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUM-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC94_ViewSharedHealthSummary_TDS.md` reviewed and Approved
- [ ] **BLOCKING:** Booking service (UC-75/76) implemented, populates `consultation_bookings.shared_summary_id`
- [ ] **BLOCKING:** `consent_grants` row creation path for `data_type='EXPERT_SHARED_DATA'` confirmed and implemented (Entry-Criteria Blocker §1.2)
- [ ] ADR-SUMMARY-ACCESS-001 confirmed by Product/Tech Lead/DPO — `recipient` matching convention specifically needs sign-off
- [ ] Minimum-necessary `summaryFields` allowlist confirmed by Product/DPO
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh
- [ ] Test coverage ≥ 80% lines cho `SharedHealthSummaryService`, `SharedHealthSummaryPolicy`
- [ ] `SUM-TC-005`, `SUM-TC-006`, `SUM-TC-008`, `SUM-TC-009`, `SUM-TC-012` (consent enforcement, info-leakage, minimum-necessary, no-grace-period) pass — these are release-blocking privacy gates
- [ ] Không có business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — dùng `SharedHealthSummaryTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn

### Suspension Criteria

- Booking/consent-grant prerequisite services not yet deployed to test environment
- `recipient` matching convention (L2) not yet confirmed by Product/Tech Lead
- Minimum-necessary `summaryFields` allowlist not yet confirmed by Product/DPO

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/
kubectl rollout undo deployment/carebridge-api

# Gap vẫn OPEN → giữ nguyên entry trong TDS §1.2 Entry-Criteria Blocker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a different expiry mechanism (e.g., session-linked) than `consent_grants`-gated | ☑ (all decisions traced to ADR-SUMMARY-ACCESS-001) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (no controller-level policy tests written) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces) | G-3 |
| **AP-CB-201** *(project-specific)* | **Authorization decision caching across requests** | Any test path where a summary remains viewable after `revoked_at`/`expiry_at` passes, without a fresh DB check | `SUM-TC-012`, `SUM-TC-INT-001` explicitly assert re-evaluation per request | **Release-blocking** |
| **AP-CB-202** *(project-specific)* | **Information leakage via differentiated error codes** | Non-assigned Expert receives a DIFFERENT error for "summary not shared" vs. "not assigned" | `SUM-TC-008` explicitly asserts identical `SUM-004` in both cases | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`SUM-TC-008`, `SUM-TC-012`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC94 v1.0 — Draft. Total test cases: 16 (13 unit/component/security + 1 integration + 2 web). Critical-severity: 5 (`SUM-TC-001, 002, 005, 006, 009, 012` — data-integrity, RBAC, and privacy gates). Requires Approved status change only by user/Tech Lead.*
