# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC143 — Respond to Consultation Request

**Document ID:** `CB-CONSULTATION-IMP-143-TDD`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — `consultation_bookings` (L876-896), primary schema source
- `04_Implement/UC143_RespondToConsultationRequest/UC143_RespondToConsultationRequest_TDS.md` (`CB-CONSULTATION-IMP-143`)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_Test-Spec.md` (style/pattern reference, sibling consultation-domain doc)
- `04_Implement/UC78_.../UC78_TDS.md` (sibling — `DISP-006` Open item referenced in ADR-RESPOND-001)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Backend: `./mvnw test`. Web (Expert Portal, secondary surface): `npm run test:run` (Vitest). Mobile (Expert App, primary surface): `flutter test`.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC143 Respond to Consultation Request |

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
| **Feature / Gap ID** | `GAP-UC143` |
| **Module** | `Consultation — Booking Request Response (com.carebridge.backend.consultation)` |
| **Spec gốc** | `CB-CONSULTATION-IMP-143` |
| **Priority** | 🟡 Medium |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Platform** | Mobile (Expert App) primary, Web (Expert Portal) secondary/parity — per TDS §1 |
| **Data Classification** | `Confidential` (`topic` may carry health-context free text) |
| **Compliance Scope** | `PDPA / Luật 91/2025`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `Book Private Consultation (3.3.1.52, out of scope)`, `Pay Consultation Fee (3.3.1.53, out of scope)` |
| **Downstream Consumers** | `UC95 Manage Consultation Session` (session creation gated on `CONFIRMED`), Notification service |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONSULTATION-IMP-143 §17` |
| **Constraints Injected** | C1 (never write `PENDING_PAYMENT`/`PENDING_EXPERT_RESPONSE`), C2 (ownership + verification check), C3 (respondable-state guard, `PENDING_EXPERT_RESPONSE` only), C4 (propose-change field scoping — no price), C5 (controller = validation/mapping only) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | No `CHECK` constraint on `consultation_bookings.status`; exact enum values undefined by SRS | `ADR-RESPOND-001` proposes the request-response subset (`PENDING_EXPERT_RESPONSE`→`CONFIRMED`/`REJECTED`/`CHANGE_PROPOSED`) as an **application-level** enum, `Status: Proposed` pending Product/Tech Lead sign-off | Tests treat `ConsultationRequestPolicy.assertRespondable()` as the sole state-machine enforcement point (no DB CHECK constraint exists to lean on); flagged Open in §6 Suspension Criteria pending ADR-RESPOND-001 acceptance |
| L2 | Whether payment occurs before/after Expert response was unspecified in SRS | `ADR-RESPOND-001` Option B: payment-first, `PENDING_EXPERT_RESPONSE` is written by the (out-of-scope) payment service; UC143 only ever READS that value as entry precondition | Tests never assert that `ConsultationRequestService` writes `PENDING_PAYMENT`/`PENDING_EXPERT_RESPONSE` — only `CONFIRMED`/`REJECTED`/`CHANGE_PROPOSED` (C1 negative-assertion test included, `RESP138-TC-011`) |
| L3 | No SRS field-level detail on what "propose changes" may modify | `ADR-RESPOND-004` (Proposed, RG-6 Open) restricts proposable fields to `scheduledStart`/`scheduledEnd`/`channelType` only — price/commission fields are explicitly immutable via this endpoint | Tests assert a `proposedPriceAmount`/similar price field in the request payload is REJECTED (`400 RESP-001`), never silently ignored or accepted |
| L4 | This TDS cannot be implemented standalone (§1.2 Entry-Criteria Blocker — depends on out-of-scope booking-creation + payment services) | Confirmed by reading `consultation_bookings` FK/default structure — no booking-creation service exists yet in this repo snapshot | All unit/component tests seed `ConsultationBookingEntity` directly via test fixtures (bypassing the missing upstream services) — integration test explicitly notes this dependency as an Open blocking item, not silently worked around in production code |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Respond to Consultation Request bao gồm các layer:
├── Domain (ConsultationBookingEntity subset, BookingRequestStatus/RequestDecision enums — pure logic)
├── Policy (ConsultationRequestPolicy — assertIsAssignedExpert, assertRespondable, assertProposeChangeFieldsValid)
├── Services (ConsultationRequestService — mock ConsultationBookingRepository, ConsultationRequestPolicy,
│              ApplicationEventPublisher với Mockito)
├── Controller (ConsultationRequestController — mock IConsultationRequestService với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — full respond flow against real consultation_bookings row)
├── Mobile (Expert App — flutter_test widget tests for consultation_request_screen.dart, PRIMARY surface)
└── Web (Expert Portal — Vitest/Testing Library component tests for RequestResponseActions.tsx, SECONDARY/parity surface)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-143 §3.3.5.2 (L3552-3569)` | Expert accepts/rejects/proposes changes to a booking request |
| `ADR-RESPOND-001` | Booking status subset & payment-ordering assumption (Proposed, Open) |
| `ADR-RESPOND-002` | Ownership: assigned + VERIFIED Expert only |
| `ADR-RESPOND-003` | One-time, idempotent-on-retry response; no double-response |
| `ADR-RESPOND-004` | Propose-change field scoping — no price editability (Proposed, Open) |
| `BR-RBAC` | Role/ownership-scoped access |
| `BR-CONSULTATION` | Auditable lifecycle state |
| `CB-CONSULTATION-IMP-143 §8` | Service/Repository interface contracts |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Expert accepts booking within response window | `ConsultationRequestService.respond()` | `RESP138-TC-001` |
| TC-COND-002 | Expert rejects booking | `ConsultationRequestService.respond()` | `RESP138-TC-002` |
| TC-COND-003 | Expert proposes a schedule/channel change | `ConsultationRequestService.respond()` | `RESP138-TC-003` |
| TC-COND-004 | Propose-change with price field present → rejected | `ConsultationRequestPolicy.assertProposeChangeFieldsValid()` | `RESP138-TC-004` (CRITICAL — ADR-RESPOND-004) |
| TC-COND-005 | Duplicate response on already-decided booking → 409 | `ConsultationRequestService.respond()` | `RESP138-TC-005` |
| TC-COND-006 | Non-assigned Expert attempts respond → 403 (IDOR) | `ConsultationRequestPolicy.assertIsAssignedExpert()` | `RESP138-TC-006` (CRITICAL) |
| TC-COND-007 | Unverified assigned Expert attempts respond → 403 | `ConsultationRequestPolicy.assertIsAssignedExpert()` | `RESP138-TC-007` |
| TC-COND-008 | Booking not found → 404 | `ConsultationRequestService.respond()` | `RESP138-TC-008` |
| TC-COND-009 | Response attempted while `status='PENDING_PAYMENT'` (not yet payment-confirmed) → rejected | `ConsultationRequestPolicy.assertRespondable()` | `RESP138-TC-009` |
| TC-COND-010 | Invalid `decision` enum value → 400 | `RespondToRequestRequest` validation | `RESP138-TC-010` |
| TC-COND-011 | Service never writes `PENDING_PAYMENT`/`PENDING_EXPERT_RESPONSE` (negative assertion, C1) | `ConsultationRequestService.respond()` | `RESP138-TC-011` |
| TC-COND-012 | Controller RBAC — non-Expert role rejected before reaching service | `ConsultationRequestController` | `RESP138-TC-012` |
| TC-COND-013 | Full E2E: accept flow persists CONFIRMED + emits `ConsultationRequestResponded` | Integration | `RESP138-TC-INT-001` (CRITICAL) |
| TC-COND-014 | Full E2E: propose-change persists CHANGE_PROPOSED, original schedule columns preserved | Integration | `RESP138-TC-INT-002` |
| TC-COND-015 | Mobile: Expert App accept/reject/propose UI (primary surface) | Flutter widget | `RESP138-TC-MOB-001` |
| TC-COND-016 | Web: Expert Portal accept/reject/propose UI (secondary/parity surface) | Vitest component | `RESP138-TC-WEB-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `decision` (ACCEPT/REJECT/PROPOSE_CHANGE + invalid string) | DTO-layer enum boundary |
| State Transition Testing | `consultation_bookings.status` subset (§6.4 TDS) | Verify only defined edges are reachable, no illegal transitions |
| Decision Table | `assignedExpert` × `verified` × `bookingStatus` (respond eligibility matrix) | Combinatorial authorization + state coverage |
| Boundary Value Analysis | `proposedScheduledStart == proposedScheduledEnd` (zero-duration proposal) | Edge validation on propose-change payload |
| Security Testing (IDOR) | Cross-Expert booking access | ADR-RESPOND-002 ownership boundary |
| Error Guessing | Concurrent respond calls (race) on same booking | Verify no double-write / contradictory decisions persisted |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationBookingEntity{status='PENDING_EXPERT_RESPONSE', expertProfileId=EXPERT_ID}` | Happy path pending |
| `FX-002` | DB seed | `ConsultationBookingEntity{status='CONFIRMED'}` | Duplicate-response test |
| `FX-003` | DB seed | `ConsultationBookingEntity{status='PENDING_PAYMENT'}` | Not-yet-payable respondable-guard test |
| `FX-004` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` — matches `expert_profiles.user_id` for `FX-001` | Auth context, assigned & verified owner |
| `FX-005` | JWT | `{ sub: 'expert-002', role: 'ROLE_EXPERT' }` — NOT assigned to `FX-001` | Non-owner attacker (IDOR) |
| `FX-006` | Mock | `expert_profiles{user_id=expert-001, verification_status='PENDING'}` | Unverified-assigned-Expert test |
| `FX-007` | DTO | `RespondToRequestRequest{decision='PROPOSE_CHANGE', proposedScheduledStart, proposedScheduledEnd, proposedChannelType}` | Propose-change happy path |
| `FX-008` | DTO (malformed) | Propose-change payload with extraneous `proposedPriceAmount` field | ADR-RESPOND-004 rejection test |

---

## 4. Test Case Specification

> **TC ID format:** `RESP138-TC-[NNN]`

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ConsultationRequestTestFactory.java
// ═══════════════════════════════════════════════════════════
class ConsultationRequestTestFactory {

    static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B2");
    static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C3");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000D4");
    static final UUID REQUESTER_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000E5");

    static ConsultationBookingEntity makePendingResponseBooking() {
        return makePendingResponseBooking(b -> {});
    }

    static ConsultationBookingEntity makePendingResponseBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = ConsultationBookingEntity.builder()
                .bookingId(BOOKING_ID)
                .requesterUserId(REQUESTER_USER_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .topic("First trimester nutrition consultation")
                .channelType("CHAT")
                .durationMinutes((short) 30)
                .scheduledStart(Instant.now().plusSeconds(3600))
                .scheduledEnd(Instant.now().plusSeconds(5400))
                .priceSnapshotAmount(new BigDecimal("200000"))
                .commissionRateSnapshot(new BigDecimal("0.15"))
                .currency("VND")
                .status("PENDING_EXPERT_RESPONSE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        overrides.accept(booking);
        return booking;
    }

    static ExpertProfile makeVerifiedExpertProfile() {
        return ExpertProfile.builder()
                .expertProfileId(EXPERT_PROFILE_ID)
                .userId(EXPERT_USER_ID)
                .verificationStatus("VERIFIED")
                .build();
    }

    static RespondToRequestRequest makeAcceptRequest() {
        RespondToRequestRequest r = new RespondToRequestRequest();
        r.setDecision("ACCEPT");
        return r;
    }

    static RespondToRequestRequest makeProposeChangeRequest() {
        RespondToRequestRequest r = new RespondToRequestRequest();
        r.setDecision("PROPOSE_CHANGE");
        r.setProposedScheduledStart(Instant.now().plusSeconds(7200));
        r.setProposedScheduledEnd(Instant.now().plusSeconds(9000));
        r.setProposedChannelType("VOICE_CALL");
        return r;
    }
}
```

---

### RESP138-TC-001 — Expert accepts booking within response window

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestService.respond(UUID, RespondToRequestRequest, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-RESPOND-001 §Decision`, `SRS §3.3.5.2`

**Preconditions:** `FX-001` seeded (`status='PENDING_EXPERT_RESPONSE'`), `FX-004` (assigned, verified Expert JWT)

**Test Steps:**
1. Arrange: `ConsultationRequestTestFactory.makePendingResponseBooking()`, mock repository `findById()` returns it; mock `assertIsAssignedExpert`/`assertRespondable` to pass
2. Act: call `respond(BOOKING_ID, makeAcceptRequest(), EXPERT_USER_ID)`
3. Assert: `bookingRepository.save()` called once with `status='CONFIRMED'`
4. Assert: `eventPublisher.publishEvent()` called with `ConsultationRequestResponded{decision="ACCEPT", newStatus="CONFIRMED"}`

**Expected Result (PASS):** Response returned with `status="CONFIRMED"`.
**Expected Result (FAIL):** Wrong status persisted, or event not published.

**Current Status:** 🔴 Not written

---

### RESP138-TC-002 — Expert rejects booking

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestService.respond()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-RESPOND-001`

**Preconditions:** `FX-001` seeded

**Test Steps:**
1. Act: `respond(BOOKING_ID, {decision:"REJECT"}, EXPERT_USER_ID)`
2. Assert: `status='REJECTED'` persisted; `ConsultationRequestResponded{decision="REJECT"}` published

**Expected Result (PASS):** `status="REJECTED"`, terminal for this booking.
**Expected Result (FAIL):** Status not updated, or a non-terminal value persisted.

**Current Status:** 🔴 Not written

---

### RESP138-TC-003 — Expert proposes a schedule/channel change

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestService.respond()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-RESPOND-004 §Decision`

**Preconditions:** `FX-001` seeded, `FX-007` propose-change request

**Test Steps:**
1. Act: `respond(BOOKING_ID, makeProposeChangeRequest(), EXPERT_USER_ID)`
2. Assert: `status='CHANGE_PROPOSED'` persisted
3. Assert: original `scheduledStart`/`scheduledEnd`/`channelType` columns on the entity are UNCHANGED (only `status` mutated)
4. Assert: `ConsultationRequestChangeProposed{proposedScheduledStart, proposedScheduledEnd, proposedChannelType}` published with the NEW values in the event payload

**Expected Result (PASS):** Original schedule preserved on the row; proposed values travel only via the event.
**Expected Result (FAIL):** Original `scheduled_start`/`scheduled_end`/`channel_type` overwritten — violates ADR-RESPOND-004 invariant #4.

**Current Status:** 🔴 Not written

---

### RESP138-TC-004 — CRITICAL: Propose-change with price field present → rejected

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key` *(price tampering variant)*
**Feature Under Test:** `ConsultationRequestPolicy.assertProposeChangeFieldsValid(RespondToRequestRequest)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-RESPOND-004 §Decision`, `AP-CB-201`

**Preconditions:** `FX-008` malformed propose-change DTO carrying a `proposedPriceAmount` field

**Test Steps (Attack Simulation):**
1. Act: `assertProposeChangeFieldsValid(request)` where request includes a price-modifying field
2. Assert: `InvalidProposeChangeException` thrown with code `RESP-001`, HTTP 400

**Expected Result (PASS = hệ thống an toàn):** Price tampering attempt rejected at the policy layer; `price_snapshot_amount` untouched.
**Expected Result (FAIL = lỗ hổng tồn tại):** Price silently accepted/ignored (worse: persisted) — bypasses `price_locked_at` integrity guarantee, financial-safety violation.

**Current Status:** 🔴 Not written
**Implementation Note:** `RespondToRequestRequest` DTO itself should have NO price field (per §8.1) — this test also guards against a future field being carelessly added to the DTO without a corresponding policy rejection.

---

### RESP138-TC-005 — Duplicate response on already-decided booking → 409

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestService.respond()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-RESPOND-003 §Decision`

**Preconditions:** `FX-002` seeded (`status='CONFIRMED'`, already responded)

**Test Steps:**
1. Act: `respond(BOOKING_ID, {decision:"REJECT"}, EXPERT_USER_ID)`
2. Assert: `RequestAlreadyRespondedException` thrown, code `RESP-002`, HTTP 409
3. Assert: `verify(bookingRepository, never()).save(any())`

**Expected Result (PASS):** 409 returned; original `CONFIRMED` state untouched.
**Expected Result (FAIL):** State silently overwritten to `REJECTED`, contradicting the earlier `CONFIRMED` decision already communicated to the Mother.

**Current Status:** 🔴 Not written

---

### RESP138-TC-006 — CRITICAL: Non-assigned Expert attempts respond → 403 (IDOR)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ConsultationRequestPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-RESPOND-002 §Decision`, `BR-RBAC`

**Preconditions:** `FX-001` (`expertProfileId` maps to `EXPERT_USER_ID`); attacker JWT is `FX-005` (`OTHER_EXPERT_USER_ID`)

**Test Steps (Attack Simulation):**
1. Act: `assertIsAssignedExpert(booking, OTHER_EXPERT_USER_ID)`
2. Assert: `RequestAuthorizationException` thrown, code `RESP-004`, HTTP 403

**Expected Result (PASS = hệ thống an toàn):** 403 RESP-004; no state change; another Expert's booking is never respondable by a non-assigned Expert.
**Expected Result (FAIL = lỗ hổng tồn tại):** Attacker Expert can accept/reject/propose changes on a booking that isn't theirs — cross-tenant tampering, potential Mother/Expert mismatch in production.

**Current Status:** 🔴 Not written

---

### RESP138-TC-007 — Unverified assigned Expert attempts respond → 403

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-RESPOND-002 §Decision (verification_status=='VERIFIED')`

**Preconditions:** `FX-001`, `FX-006` — `expert_profiles.verification_status='PENDING'` for the assigned Expert

**Test Steps:**
1. Act: `assertIsAssignedExpert(booking, EXPERT_USER_ID)` where the Expert's own profile is `user_id`-matched but `verification_status != 'VERIFIED'`
2. Assert: `RequestAuthorizationException` thrown, code `RESP-004`, HTTP 403

**Expected Result (PASS):** Unverified Experts cannot respond even to their own assigned booking.
**Expected Result (FAIL):** Verification precondition bypassed — allows an unvetted Expert to confirm consultations.

**Current Status:** 🔴 Not written

---

### RESP138-TC-008 — Booking not found → 404

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestService.respond()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 Error Codes`

**Preconditions:** `bookingRepository.findById()` returns `Optional.empty()`

**Test Steps:**
1. Act: `respond(UUID.randomUUID(), makeAcceptRequest(), EXPERT_USER_ID)`
2. Assert: `RequestNotFoundException` thrown, code `RESP-003`, HTTP 404

**Expected Result (PASS):** Clean 404.
**Expected Result (FAIL):** 500 error, or NPE leaks internal detail.

**Current Status:** 🔴 Not written

---

### RESP138-TC-009 — Response attempted while status='PENDING_PAYMENT' → rejected

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestPolicy.assertRespondable(ConsultationBookingEntity)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-RESPOND-001 §Decision (Option B — payment gates response)`

**Preconditions:** `FX-003` seeded (`status='PENDING_PAYMENT'`, payment not yet confirmed)

**Test Steps:**
1. Act: `assertRespondable(booking)` where `booking.status == 'PENDING_PAYMENT'`
2. Assert: `RequestAlreadyRespondedException` (or equivalent not-yet-respondable exception) thrown, code `RESP-002`, HTTP 409

**Expected Result (PASS):** Expert cannot respond before payment confirms — matches ADR-RESPOND-001 Option B's "prevent Expert capacity waste on unpaid requests" rationale.
**Expected Result (FAIL):** Expert allowed to accept/reject an unpaid booking — contradicts the accepted ADR decision and risks wasted Expert capacity if the Mother never pays.

**Current Status:** 🔴 Not written
**Implementation Note:** `ADR-RESPOND-001` is `Status: Proposed`, not yet `Accepted` — this test encodes the TDS's OWN working assumption. If Product/Tech Lead later flips the ordering (response-before-payment), this test and its production code must be revised together; flagged as an Open dependency in §6.

---

### RESP138-TC-010 — Invalid decision enum value → 400

**Severity:** `LOW`
**Feature Under Test:** `RespondToRequestRequest` bean validation
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §8.1 (@Pattern regexp)`

**Preconditions:** N/A — pure DTO validation

**Test Steps:**
1. `POST /api/v1/consultations/requests/{bookingId}/respond` with `{"decision": "MAYBE_LATER"}`
2. Assert `400 Bad Request`, code `RESP-001`

**Expected Result (PASS):** Validation rejects unknown decision values before reaching the service layer.
**Expected Result (FAIL):** Invalid decision string reaches `ConsultationRequestService`, causing undefined behavior.

**Current Status:** 🔴 Not written

---

### RESP138-TC-011 — Service never writes PENDING_PAYMENT/PENDING_EXPERT_RESPONSE (C1 negative assertion)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestService.respond()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-RESPOND-001 (C1)`, `TDS §17.1 Constraint Table`

**Preconditions:** `FX-001` seeded; run for ALL three decisions (ACCEPT/REJECT/PROPOSE_CHANGE)

**Test Steps:**
1. For each `decision` in `{ACCEPT, REJECT, PROPOSE_CHANGE}`: act `respond(BOOKING_ID, request, EXPERT_USER_ID)`
2. Capture the `status` field of the entity passed to `bookingRepository.save()` via `ArgumentCaptor`
3. Assert captured `status` is NEVER `"PENDING_PAYMENT"` or `"PENDING_EXPERT_RESPONSE"` — only `CONFIRMED`/`REJECTED`/`CHANGE_PROPOSED`

**Expected Result (PASS = contract respected):** UC143's write surface strictly matches its documented ownership boundary (§1.3 of TDS).
**Expected Result (FAIL = ownership overlap):** Service writes into the payment service's value space — collision risk with the (out-of-scope) `Pay Consultation Fee` UC, a genuine architecture-integrity defect.

**Current Status:** 🔴 Not written

---

### RESP138-TC-012 — Controller RBAC: non-Expert role rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `POST /api/v1/consultations/requests/{bookingId}/respond`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** JWT with role `ROLE_MOTHER` (or any non-`ROLE_EXPERT`)

**Test Steps (Attack Simulation):**
1. `POST /api/v1/consultations/requests/{bookingId}/respond` with `ROLE_MOTHER` JWT
2. Assert `403 Forbidden` (Spring Security `@PreAuthorize`, before reaching service layer)

**Expected Result (PASS = hệ thống an toàn):** `403`, no service method invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** A Mother (or other non-Expert role) can call the Expert-only respond endpoint.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### RESP138-TC-INT-001 — Full E2E: accept flow persists CONFIRMED + emits ConsultationRequestResponded

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: POST /respond → ConsultationRequestService.respond() → consultation_bookings row updated → event published`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically (no new migration needed for UC143 — §5.3 of TDS)
- Seed: `consultation_bookings` row with `status='PENDING_EXPERT_RESPONSE'`, `expert_profile_id` matching a seeded `expert_profiles` row with `verification_status='VERIFIED'` and `user_id` matching the test JWT's `sub`
- **Documented Open dependency (TDS §1.2):** this integration test seeds the booking row directly since `Book Private Consultation`/`Pay Consultation Fee` are out of scope and not yet implemented — this is an accepted test-only workaround, not a production shortcut

**Test Steps:**
1. `POST /api/v1/consultations/requests/{bookingId}/respond` with `{"decision":"ACCEPT"}`, owning verified Expert JWT
2. Assert HTTP 200, body `status="CONFIRMED"`
3. Query DB directly: `consultation_bookings.status = 'CONFIRMED'`, `updated_at` advanced
4. Assert audit/event log shows `ConsultationRequestResponded` emitted with `decision=ACCEPT`

**Expected Result (PASS):**
- DB row transitioned exactly once, matching state machine §6.4
- Event correctly reflects the decision made

**Expected Result (FAIL):**
- DB state and API response diverge, or event missing/malformed

**DB Assertion:**
```java
ConsultationBookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
assertThat(booking.getUpdatedAt()).isAfter(booking.getCreatedAt());
```

**Current Status:** 🔴 Not written

---

### RESP138-TC-INT-002 — Full E2E: propose-change persists CHANGE_PROPOSED, original schedule preserved

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /respond (PROPOSE_CHANGE) → consultation_bookings row status updated, schedule columns untouched`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Same seed as `RESP138-TC-INT-001`, `scheduled_start`/`scheduled_end`/`channel_type` recorded before the call

**Test Steps:**
1. `POST /api/v1/consultations/requests/{bookingId}/respond` with `FX-007` propose-change payload
2. Assert HTTP 200, body `status="CHANGE_PROPOSED"`
3. Query DB: `scheduled_start`/`scheduled_end`/`channel_type` columns UNCHANGED from pre-call values
4. Assert `ConsultationRequestChangeProposed` event payload carries the NEW proposed values

**Expected Result (PASS):** DB row's original schedule columns are byte-for-byte identical pre/post-call; only `status` and `updated_at` changed.
**Expected Result (FAIL):** Original schedule columns silently overwritten — violates ADR-RESPOND-004 invariant, could desynchronize what the Mother originally booked from what's shown post-proposal.

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (Flutter — Expert App, primary surface)

---

### RESP138-TC-MOB-001 — Expert App: Accept/Reject/Propose UI renders and submits correctly

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestScreen` widget (`lib/features/consultation/screens/`)
**Test File:** `test/features/consultation/screens/consultation_request_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:** Widget pumped with a mocked pending-request API response (`status: PENDING_EXPERT_RESPONSE`)

**Test Steps:**
1. `pumpWidget(ConsultationRequestScreen(booking: fakePendingBooking))`
2. Verify 3 actions rendered: "Accept", "Reject", "Propose change"
3. Tap "Accept"
4. Verify API call triggered with `decision: "ACCEPT"`
5. Verify buttons disabled after tap (prevent double-submit, mirrors ADR-RESPOND-003's server-side guard)
6. Tap "Propose change" (separate pump) → verify a date/time + channel picker form appears; verify NO price input field is rendered anywhere in this form (client-side reflection of ADR-RESPOND-004)

**Expected Result (PASS):** UI reflects the 3 decision paths correctly; no price field exposed to the Expert in the propose-change form; double-submit prevented.
**Expected Result (FAIL):** Price field present in propose-change UI (misleads Expert into thinking they can re-price), or double-submit possible.

**Current Status:** 🔴 Not written
**Implementation Note:** Screen must treat server `409 RESP-002` responses (race with another client/tab) as authoritative — do not assume local disabled-state alone prevents a double-response; show a "already responded" message on 409.

---

### WEB TEST CASES (Vitest/Testing Library — Expert Portal, secondary/parity surface)

---

### RESP138-TC-WEB-001 — Expert Portal: RequestResponseActions component renders and submits correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `RequestResponseActions.tsx` (`src/features/consultationManagement/components/`)
**Test File:** `src/features/consultationManagement/components/RequestResponseActions.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** Component rendered with a mocked TanStack Query client seeded with a pending booking (`status: PENDING_EXPERT_RESPONSE`)

**Test Steps:**
1. `render(<RequestResponseActions booking={fakePendingBooking} />)` wrapped in `QueryClientProvider`
2. `screen.getByRole('button', { name: /accept/i })`, `/reject/i`, `/propose/i` all present
3. `userEvent.click(screen.getByRole('button', { name: /reject/i }))`
4. Assert the mutation hook (`consultationRequestApi.respond`) called with `{ decision: 'REJECT' }`
5. Assert buttons show a disabled/loading state during the in-flight mutation
6. Assert a 409 mutation error (already-responded) renders a user-facing "This request has already been responded to" message, not a raw error dump

**Expected Result (PASS):** Component correctly triggers mutation, handles loading and 409-conflict states gracefully; matches Mobile parity behavior for double-submit prevention.
**Expected Result (FAIL):** Mutation not called with correct payload, or a raw/unhandled error surfaces to the Expert.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RESP138-TC-001` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-002` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-003` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-004` | `ConsultationRequestPolicyTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — price integrity |
| `RESP138-TC-005` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-006` | `ConsultationRequestPolicyTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — IDOR |
| `RESP138-TC-007` | `ConsultationRequestPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-008` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-009` | `ConsultationRequestPolicyTest.java:TBD` | `[ ]` | `[ ]` | Depends on ADR-RESPOND-001 sign-off |
| `RESP138-TC-010` | `ConsultationRequestControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-011` | `ConsultationRequestServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-012` | `ConsultationRequestControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-INT-001` | `ConsultationRequestIntegrationTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL E2E |
| `RESP138-TC-INT-002` | `ConsultationRequestIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-MOB-001` | `consultation_request_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `RESP138-TC-WEB-001` | `RequestResponseActions.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationRequestService implements IConsultationRequestService {

    @Override
    public ConsultationRequestResponse respond(UUID bookingId, RespondToRequestRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ConsultationRequestResponse getRequest(UUID bookingId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ConsultationRequestResponse> listPendingRequests(UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RESP138-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESP138-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESP138-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(fill during implementation phase)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CONSULTATION-IMP-143` reviewed and approved (Status field = `Approved`)
- [ ] **BLOCKING (§1.2 of TDS):** `Book Private Consultation` and `Pay Consultation Fee` implemented, OR an explicit decision made to test UC143 in isolation via direct DB seeding (as this Test-Spec does for INT tests) while flagging production readiness as blocked
- [ ] `ADR-RESPOND-001` and `ADR-RESPOND-004` — currently `Status: Proposed` — MUST move to `Accepted` (Product/Tech Lead sign-off) before implementation begins; tests in this spec encode the TDS's own working assumption and will need revision if the ADRs are revised
- [ ] Logic Issues (Section 2) confirmed against actual codebase (`consultation_bookings` schema — done above)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration tests (`RESP138-TC-INT-001`, `RESP138-TC-INT-002`) green with Testcontainers
- [ ] `flutter test` — Mobile (Expert App, primary) widget test green
- [ ] `npm run test:run` — Web (Expert Portal, secondary) component test green
- [ ] Test coverage ≥ 80% lines for `ConsultationRequestService` and `ConsultationRequestPolicy`
- [ ] No business logic in `ConsultationRequestController` (validation + mapping only)
- [ ] No PII/secret in plaintext logs
- [ ] **CRITICAL**: `RESP138-TC-004` (price tampering block) and `RESP138-TC-006` (IDOR block) both green before merge

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL against throw-stub before implementation begins
- [ ] **Contract Existence** — `./mvnw compile` clean, no hallucinated imports
- [ ] **Props Isolation** — verified via `ConsultationRequestTestFactory`, no shared mutable state
- [ ] **Oracle Source** — every assert traces to BR-RBAC/BR-CONSULTATION/ADR-RESPOND-001/002/003/004

### Suspension Criteria

- `ADR-RESPOND-001`/`ADR-RESPOND-004` remain `Proposed` without Product/Tech Lead sign-off — implementation may proceed on the TDS's stated working assumption, but go-live requires sign-off (same posture as UC137's RG-4 precedent)
- `Book Private Consultation`/`Pay Consultation Fee` not yet implemented — production dispatch blocked regardless of this Test-Spec's own readiness (§1.2 Entry-Criteria Blocker)
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
# No new migration to revert (TDS §5.3 — no schema change).
git checkout -- src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/ConsultationRequestService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicy.java
git checkout -- src/test/java/com/carebridge/backend/consultation/

# Mobile
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/screens/consultation_request_screen.dart
git checkout -- 05_Development/CareBridgeMobileApp/test/features/consultation/screens/consultation_request_screen_test.dart

# Web
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/RequestResponseActions.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/RequestResponseActions.test.tsx

kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-RESPOND-001/002/003/004 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw-stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes booking creation/payment flow (out of scope) instead of seeding directly | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test verifies business logic instead of delegation | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a non-existent `safety_alerts`-style table or a `consultation_bookings.status` CHECK constraint that does not exist | ☐ | G-3 |
| AP-CB-201 *(project-specific, TDS §17.4)* | **Price tampering via propose-change** | `PROPOSE_CHANGE` handler/test accepts or persists a price/commission field | ☐ | **BLOCK** — violates ADR-RESPOND-004 explicitly |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
