# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC205 — Cancel Consultation — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-205`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L876-1000, L1422-1444, L1527-1528, L1636-1643, L1823-1884) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.4 (L4410-4429)
- `04_Implement/UC205_CancelConsultation/UC205_CancelConsultation_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` + `_Test-Spec.md` — reused `VnPayGatewayClient` + refund state machine + WireMock test pattern
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — ADR-SESSION-001 (session terminal `CANCELLED`)
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC205 |

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
| **Feature / Gap ID** | `UC-205` |
| **Module** | `Consultation — Cancel Consultation` |
| **Spec gốc** | `CB-CONSULTATION-IMP-205` (UC205 TDS) |
| **Priority** | 🟠 P1 *(SRS Priority: Medium; financial-safety class → treated as release-blocking for refund-safety TCs)* |
| **Sprint** | UC203→UC210 consultation-lifecycle batch — TV4-Lâm |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | Booking service (UC-75/UC-76), UC78 refund infra (`VnPayGatewayClient`), UC95 session service (ALL BLOCKING — see §6 Entry Criteria) |
| **Downstream Consumers** | Notification service, Commission/Settlement reversal (out of scope) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC205 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (real split-table schema), C2 (ownership: requester or assigned expert), C3 (reuse UC78 VnPayGatewayClient + idempotent state machine, dispute_id=NULL), C4 (refund amount from cancellation_policy_snapshot, no hard-coded numbers), C5 (session cascade per ADR-SESSION-001, no entity leakage) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Sibling UC75 Draft TDS invented a unified `consultations` table / `consultation_status` enum | Real applied schema is split-table: `consultation_bookings.status`, separate `consultation_sessions.session_status`, `payment_transactions.status`, `refund_records.status` (`V1__init_schema.sql` L876-1000) | Every test asserts writes against `consultation_bookings`/`consultation_sessions`/`refund_records` directly; a test that assumes a `consultations` table is a spec violation (AP-CB-004, §8) |
| L2 | SRS UC-205 text is generic boilerplate ("cancels according to deadline, fee, and reason policy") — gives **no** numeric deadline/percentage | `consultation_bookings.cancellation_policy_snapshot` (text) is the per-booking authority; mockup CB-165's "100% within 24h" copy is illustrative UI text, not a confirmed BR | Tests parametrize `CancellationPolicy.computeRefund()` against **fixture-supplied** `RefundDecision` values (`FX-006`/`FX-007`) rather than asserting a specific hard-coded percentage/deadline; the exact numbers remain `Open` — flagged, not invented, in test oracles |
| L3 | Booking cancelled-status string value not stated anywhere in SRS | `ADR-CANC-001` proposes `'CANCELLED'` on `consultation_bookings.status`, pending Product/Tech Lead confirmation | Tests assert the **mechanism** (a cancellable booking transitions to a single terminal, non-`PENDING_PAYMENT`, previously-agreed string) using the constant `ConsultationBookingTestFactory.CANCELLED_STATUS = "CANCELLED"`; if Product changes the string, only the factory constant needs updating (no test logic rewrite) |
| L4 | UC78 already defines `refund_records` with nullable `dispute_id`/`approved_by`, but nothing previously tested the **cancellation-origin** discriminator | `refund_records.dispute_id` (nullable, L989) distinguishes a UC-205 auto-refund (`dispute_id = NULL`) from a UC-210 manual dispute refund (`dispute_id` set) — `ADR-CANC-003` | `CANC-TC-009` explicitly asserts the persisted `refund_records` row has `dispute_id IS NULL` for a cancellation-triggered refund — this is the automated regression guard for the dual-trigger design |
| L5 | No DB unique/partial index exists preventing a booking from having two simultaneous non-`FAILED` refunds (only FK exists on `refund_records.payment_id`) | Confirmed via schema read (same finding as UC78 §5.3) | Idempotency is a **service-level** guard (`ADR-CANC-006`); `CANC-TC-008` tests this at the service layer since the DB does not enforce it |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Cancel Consultation module bao gồm các layer:
├── Domain (CancelConsultationPolicy, CancellationPolicy — pure logic, no deps)
├── Services (CancelConsultationService, CancellationRefundService — mock JPA Repository với Mockito)
├── Controller (CancelConsultationController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + WireMock cho VNPay, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-205 (§3.3.14.4) | Trigger, dual actor (Mother/Expert), exceptions E1-E3 |
| `UC205 TDS` ADR-CANC-001..006 | Status value (Open), ownership, dual-trigger refund reuse, refund-policy mechanism (Open numbers), session cascade, idempotency |
| `V1__init_schema.sql` L876-1000 | Column names/types/defaults/FKs/unique constraints as persistence oracle |
| `UC78 TDS/Test-Spec` | `VnPayGatewayClient` contract, refund state machine, WireMock stub pattern (reused, not re-derived) |
| `ADR-SESSION-001` (UC95) | `session_status='CANCELLED'` terminal value contract |
| `BR-RBAC` / `BR-CONSULTATION` | Authorization, auditable lifecycle |
| `UC205 TDS §9-10` | API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother (requester) cancels own booking within refund window | `CancelConsultationService.cancelConsultation()` | `CANC-TC-001` |
| TC-COND-002 | Cancel outside refund window — no refund created | `CancelConsultationService` + `CancellationPolicy.computeRefund()` | `CANC-TC-002` |
| TC-COND-003 | Non-owner Mother attempts cancellation | `CancelConsultationPolicy.assertCanCancel()` | `CANC-TC-003` |
| TC-COND-004 | Non-assigned Expert attempts cancellation | `CancelConsultationPolicy.assertCanCancel()` | `CANC-TC-004` |
| TC-COND-005 | Assigned Expert cancels own booking (happy path variant) | `CancelConsultationService.cancelConsultation()` | `CANC-TC-005` |
| TC-COND-006 | Already-`CANCELLED` booking → 409 | `CancelConsultationPolicy.assertCancellable()` | `CANC-TC-006` |
| TC-COND-007 | Already-`COMPLETED` booking → 409 | `CancelConsultationPolicy.assertCancellable()` | `CANC-TC-007` |
| TC-COND-008 | Refund idempotency — duplicate cancel/replay does not double-refund | `CancellationRefundService.processCancellationRefund()` | `CANC-TC-008` |
| TC-COND-009 | Cancellation refund row has `dispute_id = NULL` (dual-trigger discriminator) | `CancellationRefundService.processCancellationRefund()` | `CANC-TC-009` |
| TC-COND-010 | Session cascade — existing `WAITING` session set to `CANCELLED` | `CancelConsultationService.cancelConsultation()` | `CANC-TC-010` |
| TC-COND-011 | No session row exists — no session write attempted | `CancelConsultationService.cancelConsultation()` | `CANC-TC-011` |
| TC-COND-012 | VNPay refund timeout/5xx — `FAILED` terminal state, booking stays `CANCELLED`, no crash | `CancellationRefundService.processCancellationRefund()` | `CANC-TC-012` |
| TC-COND-013 | VNPay refund happy path — `SUCCESS` terminal state | `CancellationRefundService.processCancellationRefund()` | `CANC-TC-013` |
| TC-COND-014 | Booking not found → 404 | `CancelConsultationService.cancelConsultation()` | `CANC-TC-014` |
| TC-COND-015 | Unauthenticated request → 401 | `CancelConsultationController` security filter | `CANC-TC-015` |
| TC-COND-016 | `ConsultationCancelled` event emitted with correct payload | `CancelConsultationService.cancelConsultation()` | `CANC-TC-016` |
| TC-COND-017 | `RefundProcessed` event emitted on terminal VNPay result (reused UC78 schema) | `CancellationRefundService.processCancellationRefund()` | `CANC-TC-017` |
| TC-COND-018 | Response never leaks raw entity fields | `CancelConsultationMapper` | `CANC-TC-018` |
| TC-COND-019 | Boundary: `reason` length validation (0 / 1000 / 1001 chars) | DTO validation | `CANC-TC-019` |
| TC-COND-020 | Concurrent-request race — second in-flight cancel hits refund-in-progress guard | `CancelConsultationService.cancelConsultation()` | `CANC-TC-020` |
| TC-COND-021 | E2E — full cancel API flow via MockMvc/Testcontainers | `CancelConsultationController` + real DB | `CANC-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Actor identity (requester / assigned expert / neither) | 3 partitions map directly to 403 vs 200 branching |
| Boundary Value Analysis | `reason` length (0, 1000, 1001 chars); refund-window boundary (exact deadline instant) | Confirms `@Size(max=1000)` and the within/outside-window split in `CancellationPolicy` |
| State Transition Testing | Booking `[cancellable]→CANCELLED`; Session `WAITING→CANCELLED`; Refund `PENDING→PROCESSING→{SUCCESS\|FAILED}` | Validates ADR-CANC-001/005/006 + reused ADR-DISPUTE-003 state machine |
| Error Guessing | IDOR via bookingId manipulation, replay of cancel call, VNPay timeout injection | Ownership + idempotency + gateway-failure attack surface |
| Decision Table | Ownership × booking-status × refund-window combinations | 200(refund) vs 200(no-refund) vs 403 vs 409 branching |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationBookingEntity{bookingId=B1, requesterUserId=U1, expertProfileId=EP1, status='CONFIRMED', scheduledStart=now+48h, priceSnapshotAmount=350000}` | Happy path — cancellable, within window |
| `FX-002` | DB seed | Same as `FX-001` but `scheduledStart=now+2h` (inside a non-refundable window per policy snapshot) | Happy path B — outside refund window |
| `FX-003` | JWT | `{sub: 'U1', role: 'MOTHER'}` | Auth context — requester |
| `FX-004` | JWT | `{sub: 'U2', role: 'MOTHER'}` | Auth context — non-owner Mother |
| `FX-005` | JWT | `{sub: 'UE1', role: 'EXPERT'}` — resolves as owner of `EP1` | Auth context — assigned Expert |
| `FX-005b` | JWT | `{sub: 'UE2', role: 'EXPERT'}` — resolves as owner of a different `expert_profile_id` | Auth context — non-assigned Expert |
| `FX-006` | Value | `RefundDecision{eligible=true, refundAmount=350000, currency='VND'}` | Refund-eligible computation result (mechanism test, not a hard-coded SRS number) |
| `FX-007` | Value | `RefundDecision{eligible=false, refundAmount=0, currency='VND'}` | Refund-ineligible computation result |
| `FX-008` | DB seed | `ConsultationBookingEntity{...status='CANCELLED'}` | Already-terminal (cancelled) conflict case |
| `FX-009` | DB seed | `ConsultationBookingEntity{...status='COMPLETED'}` | Already-terminal (completed) conflict case |
| `FX-010` | DB seed | `ConsultationSessionEntity{bookingId=B1, sessionStatus='WAITING'}` | Session-cascade case |
| `FX-011` | DB seed | `PaymentTransactionEntity{paymentId=P1, bookingId=B1, status='SUCCESS', grossAmount=350000}` | Refund basis |
| `FX-012` | WireMock stub *(reused from UC78 FX-008)* | VNPay refund endpoint returns `200 {status: SUCCESS, gatewayRefundId: 'VNP-REF-2'}` | Refund success path |
| `FX-013` | WireMock stub *(reused from UC78 FX-009)* | VNPay refund endpoint returns `504 Gateway Timeout` | Refund failure path |
| `FX-014` | DB seed | `RefundRecordEntity{paymentId=P1, disputeId=NULL, status='PROCESSING'}` | Idempotency guard test — existing in-flight refund |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `CancelConsultationPolicy`, `CancellationPolicy`, `CancelConsultationService`, `CancellationRefundService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest CancelConsultationController` | ✅ MockMvc full flow | ✅ IDOR / RBAC |
| Mobile | ✅ `cancel_consultation_repository.dart` unit | — | ✅ `cancel_consultation_confirmation` widget test (CB-165) | — | — |
| Web | ✅ Cancel entry point on Consultation Detail (CB-060) — component test | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

class CancelConsultationTestFactory {

    // ADR-CANC-001: exact string value is Open/Proposed — isolated in one constant
    // so a future confirmation only requires updating this factory, not every test.
    static final String CANCELLED_STATUS = "CANCELLED";

    static ConsultationBookingEntity makeBooking() {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-0000000000U1"));
        booking.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-00000000EP1"));
        booking.setStatus("CONFIRMED");
        booking.setScheduledStart(Instant.now().plus(Duration.ofHours(48)));
        booking.setPriceSnapshotAmount(new BigDecimal("350000"));
        booking.setCancellationPolicySnapshot("REFUND_100_IF_BEFORE_24H"); // format is Open (ADR-CANC-004)
        booking.setCurrency("VND");
        return booking;
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = makeBooking();
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(UUID.fromString("00000000-0000-0000-0000-0000000000S1"));
        session.setBookingId(makeBooking().getBookingId());
        session.setSessionStatus("WAITING");
        overrides.accept(session);
        return session;
    }

    static PaymentTransactionEntity makePayment() {
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setPaymentId(UUID.fromString("00000000-0000-0000-0000-0000000000P1"));
        payment.setBookingId(makeBooking().getBookingId());
        payment.setGatewayName("VNPAY");
        payment.setGrossAmount(new BigDecimal("350000"));
        payment.setStatus("SUCCESS");
        return payment;
    }

    static CancelConsultationRequest makeRequest() {
        CancelConsultationRequest request = new CancelConsultationRequest();
        request.setReason("Con bị ốm, không thể tham gia đúng giờ.");
        return request;
    }

    static CancelConsultationRequest makeRequest(Consumer<CancelConsultationRequest> overrides) {
        CancelConsultationRequest request = makeRequest();
        overrides.accept(request);
        return request;
    }

    static RefundDecision makeEligibleDecision() {
        return new RefundDecision(true, new BigDecimal("350000"), "VND");
    }

    static RefundDecision makeIneligibleDecision() {
        return new RefundDecision(false, BigDecimal.ZERO, "VND");
    }

    static RefundRecordEntity makeRefund(Consumer<RefundRecordEntity> overrides) {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundId(UUID.fromString("00000000-0000-0000-0000-0000000000R2"));
        refund.setPaymentId(makePayment().getPaymentId());
        refund.setDisputeId(null); // ADR-CANC-003 — cancellation refund always NULL dispute_id
        refund.setRefundAmount(new BigDecimal("350000"));
        refund.setStatus("PENDING");
        overrides.accept(refund);
        return refund;
    }
}
```

---

### CANC-TC-001 — Happy path A: Requester Mother cancels within refund window (refund created, SUCCESS)

**Severity:** `CRITICAL`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC205 TDS §6.1` sequence diagram + `ADR-CANC-001/003/004` + `V1__init_schema.sql` L893 (`status` column)

**Preconditions:**
- `FX-001` booking seeded (`requesterUserId = U1`, cancellable status)
- `FX-003` JWT for `U1`
- `CancellationPolicy.computeRefund()` mocked to return `FX-006` (eligible)

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-001`; mock `cancellationPolicy.computeRefund(booking, any)` returns `makeEligibleDecision()`.
2. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)`.
3. Assert: `bookingRepository.save()` called with `status == CANCELLED_STATUS`; `cancellationRefundService.processCancellationRefund(...)` invoked exactly once with the eligible decision; response `refund` block populated.

**Expected Result (PASS):** Booking persisted with `status=CANCELLED`; refund dispatch triggered; response contains `refund.refundAmount == 350000`.
**Expected Result (FAIL):** Booking status unchanged, wrong status string, or refund path skipped despite eligibility.

**Current Status:** 🔴 Not written
**Implementation Note:** `CancelConsultationService` must call `CancelConsultationPolicy.assertCanCancel` + `assertCancellable` before persistence.

---

### CANC-TC-002 — Happy path B: Cancel outside refund window (booking cancelled, no refund created)

**Severity:** `HIGH`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()` + `CancellationPolicy.computeRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC205 TDS §6.2` sequence diagram, `ADR-CANC-004` (mechanism; exact deadline `Open`)

**Preconditions:** `FX-002` booking (near-term `scheduledStart`); `cancellationPolicy.computeRefund()` mocked to return `FX-007` (ineligible).

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-002`; mock `computeRefund` returns `makeIneligibleDecision()`.
2. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)`.
3. Assert: booking `status == CANCELLED_STATUS`; `cancellationRefundService.processCancellationRefund()` called but returns `Optional.empty()`; response `refund == null`; **zero** `refundRecordRepository.save()` calls.

**Expected Result (PASS):** Booking cancelled, no refund row created, response `refund=null`.
**Expected Result (FAIL):** A refund row is created despite an ineligible decision (financial-safety violation).

**Current Status:** 🔴 Not written

---

### CANC-TC-003 — Ownership violation: non-owner Mother attempts cancellation → 403 (`CANC-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `CancelConsultationPolicy.assertCanCancel()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/CancelConsultationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC205 TDS ADR-CANC-002`, `BR-RBAC`

**Preconditions:** `FX-001` booking (owner `U1`); `FX-004` JWT for `U2`.

**Test Steps:**
1. Arrange: `booking = makeBooking()` (requester `U1`, expert profile `EP1`).
2. Act: `cancelConsultationPolicy.assertCanCancel(booking, U2)`.
3. Assert: throws `CancelAuthorizationException` code `CANC-004`.

**Expected Result (PASS):** Exception thrown, no booking write.
**Expected Result (FAIL):** No exception / cancellation allowed for a non-owner, non-assigned user.

**Current Status:** 🔴 Not written

---

### CANC-TC-004 — Ownership violation: non-assigned Expert attempts cancellation → 403 (`CANC-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — IDOR`
**Feature Under Test:** `CancelConsultationPolicy.assertCanCancel()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/CancelConsultationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC205 TDS ADR-CANC-002`

**Preconditions:** `FX-001` booking (`expertProfileId = EP1`); `FX-005b` JWT resolving to a different expert profile `EP2`.

**Test Steps:**
1. Act: `cancelConsultationPolicy.assertCanCancel(booking, UE2)` where `UE2` owns `EP2 != EP1`.
2. Assert: throws `CancelAuthorizationException` code `CANC-004`.

**Expected Result (PASS):** Exception thrown for an expert not assigned to this booking.
**Expected Result (FAIL):** Any expert can cancel any booking (cross-tenant IDOR).

**Current Status:** 🔴 Not written

---

### CANC-TC-005 — Happy path variant: assigned Expert cancels own booking

**Severity:** `HIGH`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC205 TDS ADR-CANC-002` — SRS Primary Actor "Mother, Verified Expert" (either)

**Preconditions:** `FX-001` booking (`expertProfileId = EP1`); `FX-005` JWT for `UE1` owning `EP1`.

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-001`; expert-ownership resolver returns `UE1` for `EP1`.
2. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), UE1)`.
3. Assert: no exception; `bookingRepository.save()` called with `status=CANCELLED_STATUS`.

**Expected Result (PASS):** Assigned Expert successfully cancels.
**Expected Result (FAIL):** Assigned Expert incorrectly rejected (SRS explicitly allows either actor).

**Current Status:** 🔴 Not written

---

### CANC-TC-006 — Already-`CANCELLED` booking → 409 (`CANC-002`)

**Severity:** `CRITICAL`
**Feature Under Test:** `CancelConsultationPolicy.assertCancellable()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/CancelConsultationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC205 TDS ADR-CANC-006` (idempotency guard)

**Preconditions:** `FX-008` — booking already `status='CANCELLED'`.

**Test Steps:**
1. Act: `cancelConsultationPolicy.assertCancellable(FX-008 booking)`.
2. Assert: throws `CancelConflictException` code `CANC-002`.

**Expected Result (PASS):** Exception thrown; no second cancellation processed.
**Expected Result (FAIL):** Booking silently re-cancelled (opens door to double-refund via `CANC-TC-008`).

**Current Status:** 🔴 Not written

---

### CANC-TC-007 — Already-`COMPLETED` booking → 409 (`CANC-002`)

**Severity:** `HIGH`
**Feature Under Test:** `CancelConsultationPolicy.assertCancellable()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/CancelConsultationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC205 TDS ADR-CANC-006`

**Preconditions:** `FX-009` — booking `status='COMPLETED'`.

**Test Steps:**
1. Act: `cancelConsultationPolicy.assertCancellable(FX-009 booking)`.
2. Assert: throws `CancelConflictException` code `CANC-002`.

**Expected Result (PASS):** A completed consultation cannot be cancelled.
**Expected Result (FAIL):** Completed booking incorrectly reverted to cancelled.

**Current Status:** 🔴 Not written

---

### CANC-TC-008 — Refund idempotency: duplicate cancel/replay does not double-refund

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow (double-refund class)`
**Feature Under Test:** `CancellationRefundService.processCancellationRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancellationRefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC205 TDS ADR-CANC-006` (mirrors UC78 `ADR-DISPUTE-003`)

**Preconditions:** `FX-014` — existing `refund_records` row already `status='PROCESSING'` for `paymentId=P1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByPaymentId(P1)` returns `List.of(FX-014)`.
2. Act: `cancellationRefundService.processCancellationRefund(B1, P1, makeEligibleDecision(), U1)` (second call, simulating retry/replay).
3. Assert: returns the existing record unchanged; `vnPayGatewayClient.refund()` is **never** invoked (`verifyNoInteractions`); no new `refund_records` row inserted.

**Expected Result (PASS):** Zero additional VNPay calls; existing record returned as-is.
**Expected Result (FAIL):** A second VNPay refund call is dispatched, or a second `refund_records` row is created (double-refund risk).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the core automated regression test for ADR-CANC-006 — must never regress; release-blocking.

---

### CANC-TC-009 — Cancellation refund row has `dispute_id = NULL` (dual-trigger discriminator)

**Severity:** `CRITICAL`
**Feature Under Test:** `CancellationRefundService.processCancellationRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancellationRefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC205 TDS ADR-CANC-003`, `V1__init_schema.sql` L989 (`refund_records.dispute_id` nullable)

**Preconditions:** `FX-011` payment `SUCCESS`; no pre-existing refund for `P1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByPaymentId(P1)` returns empty list; `vnPayGatewayClient.refund(...)` stubbed to succeed.
2. Act: `cancellationRefundService.processCancellationRefund(B1, P1, makeEligibleDecision(), U1)`.
3. Assert: `refundRecordRepository.save()` invoked with an entity where `disputeId == null`; `approvedBy` is `null` or a system sentinel (never a human admin id).

**Expected Result (PASS):** Persisted refund row is discriminable as cancellation-origin (`dispute_id IS NULL`), distinct from a UC-210 dispute refund.
**Expected Result (FAIL):** `disputeId` is non-null, or the refund is indistinguishable from a manual dispute refund — breaks ADR-CANC-003's dual-trigger design and any downstream reporting relying on the discriminator.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense for the dual-trigger refund ADR (AP-CB-005, §8).

---

### CANC-TC-010 — Session cascade: existing `WAITING` session set to `CANCELLED`

**Severity:** `HIGH`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC205 TDS ADR-CANC-005`, citing `ADR-SESSION-001` (UC95 TDS §3/§6.4 — `CANCELLED` is the confirmed terminal value)

**Preconditions:** `FX-001` booking; `FX-010` session (`sessionStatus='WAITING'`) linked to `B1`.

**Test Steps:**
1. Arrange: mock `sessionRepository.findByBookingId(B1)` returns `FX-010`.
2. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)`.
3. Assert: `sessionRepository.save()` called with `sessionStatus == "CANCELLED"` — same string value ADR-SESSION-001 already confirmed, not redefined here.

**Expected Result (PASS):** Session cascaded to the UC95-confirmed terminal value.
**Expected Result (FAIL):** Session left `WAITING`/`IN_SESSION` (orphaned live session), or a different terminal string is invented (violates ADR-SESSION-001 reuse).

**Current Status:** 🔴 Not written

---

### CANC-TC-011 — No session row exists — no session write attempted

**Severity:** `MEDIUM`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC205 TDS ADR-CANC-005` (session cascade is conditional)

**Preconditions:** `FX-001` booking; `sessionRepository.findByBookingId(B1)` returns empty (typical — cancellation usually precedes any join).

**Test Steps:**
1. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)`.
2. Assert: `sessionRepository.save()` is **never** called (`verifyNoInteractions` on the save path).

**Expected Result (PASS):** No spurious session row created/updated when none exists.
**Expected Result (FAIL):** Service throws or fabricates a session row when none exists.

**Current Status:** 🔴 Not written

---

### CANC-TC-012 — VNPay refund timeout/5xx → terminal `FAILED` state, booking stays `CANCELLED`, no crash (`VNP-001`)

**Severity:** `CRITICAL`
**Feature Under Test:** `CancellationRefundService.processCancellationRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancellationRefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC205 TDS §6.5` error-path sequence diagram, `ADR-CANC-003` (mirrors UC78 `ADR-DISPUTE-003`), `FX-013` WireMock `504` stub

**Preconditions:** `FX-011` payment; `FX-013` WireMock VNPay `504` stub configured.

**Test Steps:**
1. Arrange: WireMock stub returns `504 Gateway Timeout` for the refund call.
2. Act: `cancellationRefundService.processCancellationRefund(B1, P1, makeEligibleDecision(), U1)`.
3. Assert: throws `RefundGatewayException` code `VNP-001`; persisted `refund_records` final state is `FAILED` (not stuck `PROCESSING`, not silently `SUCCESS`). Separately assert (via `CancelConsultationServiceTest`) that the booking's `status='CANCELLED'` write already committed **before** the refund dispatch is unaffected by this failure (booking cancellation and refund dispatch are decoupled steps).

**Expected Result (PASS):** `VNP-001` surfaced, DB record `FAILED`, booking remains cancelled regardless of refund outcome.
**Expected Result (FAIL):** Record left `PROCESSING` indefinitely, exception swallowed silently, refund marked `SUCCESS` despite gateway failure, or the booking cancellation itself is rolled back due to the refund failure (over-coupling).

**Current Status:** 🔴 Not written

---

### CANC-TC-013 — VNPay refund happy path — terminal `SUCCESS` state

**Severity:** `HIGH`
**Feature Under Test:** `CancellationRefundService.processCancellationRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancellationRefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC205 TDS §6.1` sequence diagram; reuses UC78 `ADR-DISPUTE-003` state machine verbatim

**Preconditions:** `FX-011` payment; `FX-012` WireMock VNPay `200` stub.

**Test Steps:**
1. Arrange: WireMock stub returns `{status:"SUCCESS", gatewayRefundId:"VNP-REF-2"}`.
2. Act: `cancellationRefundService.processCancellationRefund(B1, P1, makeEligibleDecision(), U1)`.
3. Assert: `refundRecordRepository.save()` called with sequential states `PENDING`→`PROCESSING`→`SUCCESS`; final entity has `gatewayRefundId="VNP-REF-2"`, `processedAt != null`, `disputeId == null`.

**Expected Result (PASS):** Refund ends in `SUCCESS` with gateway reference populated and correct discriminator.
**Expected Result (FAIL):** Wrong terminal state, missing gateway reference, or `disputeId` incorrectly populated.

**Current Status:** 🔴 Not written

---

### CANC-TC-014 — Booking not found → 404 (`CANC-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC205 TDS §10` error table

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `cancelConsultationService.cancelConsultation(randomId, makeRequest(), U1)`.
3. Assert: throws `BookingNotFoundException` code `CANC-003`.

**Current Status:** 🔴 Not written

---

### CANC-TC-015 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `CancelConsultationController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/CancelConsultationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC205 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/bookings/{id}/cancel` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401`, no booking mutation, no refund dispatch.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### CANC-TC-016 — `ConsultationCancelled` event emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC205 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)` (eligible-refund path).
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured event is `ConsultationCancelled` with `payload.bookingId==B1`, `payload.newStatus=="CANCELLED"`, `payload.refundEligible==true`, `metadata.causedBy==U1`.

**Current Status:** 🔴 Not written

---

### CANC-TC-017 — `RefundProcessed` event emitted on terminal VNPay result (reused UC78 schema)

**Severity:** `MEDIUM`
**Feature Under Test:** `CancellationRefundService.processCancellationRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancellationRefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC78 TDS §7.1/§7.3` (payload schema reused verbatim, not redefined)

**Test Steps:**
1. Act: `cancellationRefundService.processCancellationRefund(...)` (SUCCESS path, `FX-012`).
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured `RefundProcessed` with `payload.status=="SUCCESS"`, `payload.disputeId==null` (cancellation-origin marker).

**Current Status:** 🔴 Not written

---

### CANC-TC-018 — Response never leaks raw entity/internal fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `CancelConsultationMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/CancelConsultationMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"

**Test Steps:**
1. Act: `CancelConsultationMapper.toResponse(makeBooking(), refundRecordOrNull)`.
2. Assert: resulting `CancelConsultationResponse` JSON contains only the fields declared in TDS §9.2 (`bookingId`, `status`, `cancelledAt`, `refund{refundId, refundAmount, currency, status}`) — no raw `requesterUserId`/`expertProfileId`/internal audit columns.

**Expected Result (PASS):** Response matches exactly the `CancelConsultationResponse` schema in TDS §9.2.
**Expected Result (FAIL):** Extra internal fields serialized.

**Current Status:** 🔴 Not written

---

### CANC-TC-019 — Boundary: `reason` length validation (0 / 1000 / 1001 chars)

**Severity:** `LOW`
**Feature Under Test:** `CancelConsultationRequest` DTO validation (`@Size(max=1000)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/CancelConsultationRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `UC205 TDS §8.1` DTO annotation (reason is optional per `Open` reason-policy item)

**Test Steps:**
1. Act (blank): validate `makeRequest(r -> r.setReason(""))`.
2. Assert: no violation (reason is optional, not `@NotBlank`).
3. Act (1000 chars): validate `makeRequest(r -> r.setReason("a".repeat(1000)))`.
4. Assert: no violation.
5. Act (1001 chars): validate `makeRequest(r -> r.setReason("a".repeat(1001)))`.
6. Assert: violation on `reason` field (`@Size(max=1000)`), error code `CANC-001`.

**Expected Result (PASS):** Boundary respected exactly at 1000/1001; blank/omitted reason accepted.
**Expected Result (FAIL):** Off-by-one on max length, or blank incorrectly rejected (reason is `Open`-optional, not mandatory).

**Current Status:** 🔴 Not written

---

### CANC-TC-020 — Concurrent-request race: second in-flight cancel hits refund-in-progress guard → 409 (`CANC-005`)

**Severity:** `HIGH`
**CWE:** `CWE-362 — Concurrent Execution using Shared Resource with Improper Synchronization (Race Condition)`
**Feature Under Test:** `CancelConsultationService.cancelConsultation()` (refund-in-progress branch, distinct from the `CANC-002` already-`CANCELLED` guard)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CancelConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `UC205 TDS §10` (`CANC-005`), `ADR-CANC-006`

**Preconditions:** `FX-001` booking whose status transition to `CANCELLED` has already committed by request #1, but `FX-014` refund row is still `PROCESSING` when request #2 arrives (simulates two near-simultaneous cancel calls before the first's refund settles — `CANC-002`'s booking-state guard alone does not cover this narrower window because the refund side-effect can outlive the booking-status commit).

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns booking already `status=CANCELLED_STATUS` (request #1 already committed); mock `refundRecordRepository.findByPaymentId(P1)` returns `List.of(FX-014)` (`status='PROCESSING'`).
2. Act: `cancelConsultationService.cancelConsultation(B1, makeRequest(), U1)` (request #2).
3. Assert: throws `CancelConflictException` code `CANC-005` (not a generic `CANC-002`, since the distinguishing condition is an in-flight refund, not merely a terminal booking) OR — if the booking-state guard fires first because status is already terminal — assert `CANC-002` is returned and **document via this test that `CANC-005` is unreachable through the public `cancelConsultation()` entry point once `CANC-002` already gates it**, making `CANC-005` reachable only via `CancellationRefundService.processCancellationRefund()` invoked directly (already covered by `CANC-TC-008`).

**Expected Result (PASS):** Either explicit `CANC-005` is thrown for the narrow race, or the test documents/asserts that `CANC-002` supersedes it at the service boundary — no double VNPay call in either case.
**Expected Result (FAIL):** A second VNPay refund dispatch occurs, or an unhandled exception/500 is returned instead of a clean 409.

**Current Status:** 🔴 Not written
**Implementation Note:** This TC exists to close the CG-2 consistency gap between TDS §10's `CANC-005` error code and test coverage — implementer must confirm at code-review time whether `CANC-005` is reachable from the controller or is purely an internal `CancellationRefundService` guard (already exercised by `CANC-TC-008`); update this TC's assertion to match once confirmed.

---

### INTEGRATION TEST CASES

---

### CANC-TC-INT-001 — E2E: cancel consultation API flow (Testcontainers + WireMock)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/consultations/bookings/{id}/cancel` → DB state (booking + session + refund)
**Test File:** `src/test/java/com/carebridge/backend/consultation/CancelConsultationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-001` booking, `FX-011` payment, `FX-010` session inserted via JPA before test
- WireMock VNPay stub (`FX-012`) configured for `200 SUCCESS`

**Test Steps:**
1. Seed booking `B1` (owner `U1`), payment `P1`, session (`WAITING`).
2. `POST /api/v1/consultations/bookings/{B1}/cancel` with JWT for `U1` and body `{"reason": "..."}`.
3. Assert response `200`, body matches §9.2 schema, `refund.status` reflects terminal VNPay result.
4. Assert DB: `consultation_bookings.status='CANCELLED'`, `consultation_sessions.session_status='CANCELLED'`, exactly one `refund_records` row for `payment_id=P1` with `dispute_id IS NULL`.

**Expected Result (PASS):**
- API `200`; DB rows consistent across all three tables in a single logical outcome.

**Expected Result (FAIL):**
- API error, or DB rows inconsistent (e.g., booking cancelled but session not cascaded, or duplicate refund rows).

**DB Assertion:**
```java
ConsultationBookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
assertThat(booking.getStatus()).isEqualTo("CANCELLED");

ConsultationSessionEntity session = sessionRepository.findByBookingId(bookingId).orElseThrow();
assertThat(session.getSessionStatus()).isEqualTo("CANCELLED");

List<RefundRecordEntity> refunds = refundRecordRepository.findByPaymentId(paymentId);
assertThat(refunds).hasSize(1);
assertThat(refunds.get(0).getDisputeId()).isNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CANC-TC-001` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-002` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-003` | `CancelConsultationPolicyTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-004` | `CancelConsultationPolicyTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-005` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-006` | `CancelConsultationPolicyTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-007` | `CancelConsultationPolicyTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-008` | `CancellationRefundServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-009` | `CancellationRefundServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-010` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-011` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-012` | `CancellationRefundServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-013` | `CancellationRefundServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-014` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-015` | `CancelConsultationControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-016` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-017` | `CancellationRefundServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-018` | `CancelConsultationMapperTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-019` | `CancelConsultationRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-020` | `CancelConsultationServiceTest.java` | `[ ]` | `[ ]` | |
| `CANC-TC-INT-001` | `CancelConsultationIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class CancelConsultationService implements ICancelConsultationService {
    @Override
    public CancelConsultationResponse cancelConsultation(UUID bookingId,
                                                          CancelConsultationRequest request,
                                                          UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Service
public class CancellationRefundService implements ICancellationRefundService {
    @Override
    public Optional<RefundRecordEntity> processCancellationRefund(UUID bookingId,
                                                                   UUID paymentId,
                                                                   RefundDecision decision,
                                                                   UUID cancelledBy) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CANC-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-020` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CANC-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** Booking service (UC-75/UC-76 — creates `consultation_bookings` rows with real `requester_user_id`/`expert_profile_id`/`status`/`scheduled_start`/`price_snapshot_amount`/`cancellation_policy_snapshot`) implemented and deployed to a test environment
- [ ] **BLOCKING:** UC78 refund infrastructure (`VnPayGatewayClient` interface + refund state machine / shared dispatch component) implemented and stable
- [ ] **BLOCKING:** UC95 session lifecycle (`consultation_sessions.session_status`, ADR-SESSION-001) implemented
- [ ] `UC205_CancelConsultation_TDS.md` reviewed and Approved
- [ ] ADR-CANC-001 (booking cancelled-status string value) and ADR-CANC-004 (cancellation-policy-snapshot format + exact refund percentages/deadlines) confirmed by Product/Tech Lead (currently `Proposed`/`Open`)
- [ ] Test fixtures (§3 TDS-05) prepared
- [ ] WireMock VNPay stub server configured for integration tests (reuse UC78's existing WireMock setup — do not duplicate)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers + WireMock)
- [ ] Test coverage ≥ 80% lines cho `CancelConsultationService`, `CancellationRefundService`, `CancelConsultationPolicy`, `CancellationPolicy`
- [ ] Không có business logic trong `CancelConsultationController` (chỉ có validation + mapping)
- [ ] Không có PII/reason text xuất hiện plaintext trong logs
- [ ] `CANC-TC-008` and `CANC-TC-009` (idempotency + dispute_id=NULL discriminator) pass — release-blocking financial-safety gates
- [ ] `CANC-TC-006`/`CANC-TC-007` (already-terminal rejection) pass — release-blocking

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — mỗi test dùng `CancelConsultationTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- Booking/Payment/Session/UC78-refund prerequisite services not yet deployed to test environment
- ADR-CANC-001 (status string) or ADR-CANC-004 (refund policy format/numbers) still unconfirmed at start of Sprint implementation
- New architecture unknown discovered (e.g., the exact cancellable-status set differs from assumption) requiring Tech Lead review

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in scope for UC205)
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
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-CANC-00X or reused ADR-DISPUTE-003/ADR-SESSION-001) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `CANC-TC-015`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, or redefines `VnPayGatewayClient` | ☑ (all types match TDS §8 interfaces; VNPay client reused, not redefined) | G-3 |
| **AP-CB-003** *(project-specific)* | **Double-refund on duplicate cancel** | Any test path where a second cancel/replay dispatches a second VNPay call or creates a second `refund_records` row | `CANC-TC-008` explicitly asserts `verifyNoInteractions`/existing-record-returned | **Release-blocking** |
| **AP-CB-004** *(project-specific)* | **Wrong-table write (UC75 unified-table contradiction)** | Test/implementation writes to a `consultations`/`consultation_status` target that does not exist in `V1__init_schema.sql` | All TCs assert against `consultation_bookings`/`consultation_sessions`/`refund_records` only (L1) | **Release-blocking** |
| **AP-CB-005** *(project-specific)* | **Missing dual-trigger discriminator** | A cancellation-created `refund_records` row has a non-null `dispute_id`, making it indistinguishable from a UC-210 manual dispute refund | `CANC-TC-009` explicitly asserts `dispute_id IS NULL` | **Release-blocking** |
| **AP-CB-006** *(project-specific)* | **Invented fee/deadline numbers** | Test hard-codes a specific refund percentage/deadline hour not sourced from `cancellation_policy_snapshot` or an approved ADR | All refund-amount assertions use fixture-injected `RefundDecision` values (`FX-006`/`FX-007`), never a literal percentage claimed as an SRS fact | G-1 |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`CANC-TC-008`, `CANC-TC-009`, `CANC-TC-006/007`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC205 v1.0 — Draft. Total test cases: 21 (20 unit/component + 1 integration). Critical-severity: 8 (`CANC-TC-001, 003, 004, 006, 008, 009, 012, 015` — financial-safety, IDOR, and idempotency gates). Requires Approved status change only by user/Tech Lead. Open items carried from TDS: ADR-CANC-001 (booking cancelled-status string value) and ADR-CANC-004 (cancellation-policy-snapshot format + exact refund percentages/deadlines) — NOT invented, mechanism-only tested. Consistency Gate CG-1..CG-9 executed against `UC205_CancelConsultation_TDS.md`: all 9 checks passed after adding `CANC-TC-020` to close the CG-2 gap (TDS error code `CANC-005` previously lacked dedicated test coverage).*
