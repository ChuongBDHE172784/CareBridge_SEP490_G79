# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC78 — Submit Dispute or Refund Request — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-078`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L969-1000, L1859-1882) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.55 (L2914-2933)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC78 |

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
| **Feature / Gap ID** | `UC-78` |
| **Module** | `Consultation — Dispute & Refund` |
| **Spec gốc** | `CB-CONSULTATION-IMP-078` (UC78 TDS) |
| **Priority** | 🔴 High (per SRS) |
| **Sprint** | `Sprint 2 — Complete Core CRUD And UI Wiring` |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY`, `BR-CONSULTATION` |
| **Upstream Dependencies** | Booking service, Payment capture service, Session service (ALL BLOCKING — see §6 Entry Criteria) |
| **Downstream Consumers** | Commission/settlement recalculation (out of scope), Notification service |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC78 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (manual refund gate), C2 (ownership check), C3 (VNPay idempotency), C4 (package layout), C5 (no entity leakage) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-78 text is generic boilerplate; does not state whether refund is automatic or admin-gated | Schema has `consultation_disputes.resolved_by` + `refund_records.approved_by` (both nullable `uuid` FK to `users`), implying human-gated resolution (ADR-DISPUTE-001) | Tests assert NO refund is created/dispatched directly from `submitDispute()`; refund creation is only reachable via `processApprovedRefund()` given an `APPROVED` dispute |
| L2 | SRS does not define a dispute reason taxonomy explicitly as an enum | `consultation_disputes.reason_code` is free `varchar(50)`, no DB CHECK constraint; SRS description text names 3 categories (expert absent / scope violation / technical issue) | Tests assert app-level validation rejects any `reasonCode` outside `{EXPERT_ABSENT, SCOPE_VIOLATION, TECHNICAL_ISSUE, OTHER}` (ADR-DISPUTE-004 proposal) |
| L3 | No explicit uniqueness constraint in schema preventing duplicate open disputes per booking | No DB unique/partial index exists on `consultation_disputes(booking_id)` | Tests encode the **service-level** 409 conflict check (`findOpenDisputesByBookingId`), since DB does not enforce this — service must guard it |
| L4 | No index exists on `refund_records.payment_id`/`dispute_id` (only FK) | Confirmed via schema read — no `CREATE INDEX` found for these columns | Not a functional test concern; flagged in TDS §5.3 as an Open follow-up, NOT tested here (no functional behavior depends on the index) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Dispute & Refund module bao gồm các layer:
├── Domain (DisputePolicy — pure logic, no deps)
├── Services (DisputeService, RefundService — mock JPA Repository với Mockito)
├── Controller (DisputeController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + WireMock cho VNPay, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-78 (§3.3.1.55) | Trigger, actor, exceptions E1-E3 |
| `UC78 TDS` ADR-DISPUTE-001..004 | Manual approval gate, ownership, idempotency, taxonomy |
| `V1__init_schema.sql` L969-1000 | Column names/types/defaults/FKs as persistence oracle |
| `BR-RBAC` / `BR-PRIVACY` / `BR-CONSULTATION` | Authorization, data-minimization, auditable lifecycle |
| `UC78 TDS §9-10` | API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother submits valid dispute for own booking | `DisputeService.submitDispute()` | `DISP-TC-001` |
| TC-COND-002 | Non-owner attempts to submit dispute | `DisputePolicy.assertCanSubmitDispute()` | `DISP-TC-002` |
| TC-COND-003 | Duplicate open dispute for same booking | `DisputeService.submitDispute()` | `DISP-TC-003` |
| TC-COND-004 | Invalid `reasonCode` value | `DisputePolicy.validateReasonCode()` | `DISP-TC-004` |
| TC-COND-005 | Blank/oversized `description` | DTO validation | `DISP-TC-005` |
| TC-COND-006 | Booking not found | `DisputeService.submitDispute()` | `DISP-TC-006` |
| TC-COND-007 | Unauthenticated request | `DisputeController` security filter | `DISP-TC-007` |
| TC-COND-008 | Refund NOT auto-created on dispute submission (anti auto-approve) | `DisputeService.submitDispute()` | `DISP-TC-008` |
| TC-COND-009 | `processApprovedRefund` rejects non-APPROVED dispute | `RefundService.processApprovedRefund()` | `DISP-TC-009` |
| TC-COND-010 | VNPay refund happy path — SUCCESS terminal state | `RefundService.processApprovedRefund()` | `DISP-TC-010` |
| TC-COND-011 | VNPay refund timeout/5xx — FAILED terminal state, no crash | `RefundService.processApprovedRefund()` | `DISP-TC-011` |
| TC-COND-012 | Refund idempotency — repeated approval call is a no-op (no second VNPay call) | `RefundService.processApprovedRefund()` | `DISP-TC-012` |
| TC-COND-013 | `DisputeSubmitted` event emitted with correct payload | `DisputeService.submitDispute()` | `DISP-TC-013` |
| TC-COND-014 | `RefundProcessed` event emitted on terminal VNPay result | `RefundService.processApprovedRefund()` | `DISP-TC-014` |
| TC-COND-015 | E2E — full submit dispute API flow via MockMvc/Testcontainers | `DisputeController` + real DB | `DISP-TC-INT-001` |
| TC-COND-016 | Response never leaks raw entity fields (e.g., `submittedBy` internal user object) | `DisputeMapper` | `DISP-TC-015` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `reasonCode` valid/invalid classes | 4 valid values vs. arbitrary invalid strings |
| Boundary Value Analysis | `description` length (0, 1, 2000, 2001 chars) | Confirms `@Size(max=2000)` boundary |
| State Transition Testing | Dispute `OPEN→UNDER_REVIEW→APPROVED/REJECTED`; Refund `PENDING→PROCESSING→SUCCESS/FAILED` | Validates ADR-DISPUTE-001/003 state machines |
| Error Guessing | IDOR via booking ID manipulation, replay of approval call | Ownership + idempotency attack surface |
| Decision Table | Ownership × dispute-existing-state combinations | 409 vs 403 vs 201 branching |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationBookingEntity{bookingId=B1, requesterUserId=U1, status='COMPLETED'}` | Happy path owner |
| `FX-002` | DB seed | `ConsultationBookingEntity{bookingId=B1, requesterUserId=U1}` + JWT for `U2` | Ownership violation |
| `FX-003` | DB seed | Existing `ConsultationDisputeEntity{bookingId=B1, status='OPEN'}` | Duplicate dispute conflict |
| `FX-004` | JWT | `{sub: 'U1', role: 'MOTHER'}` | Auth context — booking owner |
| `FX-005` | JWT | `{sub: 'U2', role: 'MOTHER'}` | Auth context — non-owner |
| `FX-006` | DB seed | `ConsultationDisputeEntity{status='APPROVED'}` + `PaymentTransactionEntity{status='SUCCESS', grossAmount=500000}` | Refund happy path |
| `FX-007` | DB seed | `ConsultationDisputeEntity{status='OPEN'}` (not approved) | Refund rejection (`DISP-005`) |
| `FX-008` | WireMock stub | VNPay refund endpoint returns `200 {status: SUCCESS, gatewayRefundId: 'VNP-REF-1'}` | Refund success path |
| `FX-009` | WireMock stub | VNPay refund endpoint returns `504 Gateway Timeout` | Refund failure path |
| `FX-010` | DB seed | `RefundRecordEntity{disputeId=D1, status='PROCESSING'}` | Idempotency guard test |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `DisputePolicy`, `DisputeService`, `RefundService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest DisputeController` | ✅ MockMvc full flow | ✅ IDOR / RBAC |
| Mobile | ✅ `dispute_service.dart` unit | — | ✅ `submit_dispute_screen` widget test | — | — |
| Web | N/A (Mobile-only platform per SRS "Other Information: Platform: Mobile App") | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

class ConsultationDisputeTestFactory {

    static ConsultationBookingEntity makeBooking() {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-000000000U1"));
        booking.setStatus("COMPLETED");
        return booking;
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = makeBooking();
        overrides.accept(booking);
        return booking;
    }

    static ConsultationDisputeEntity makeDispute() {
        ConsultationDisputeEntity dispute = new ConsultationDisputeEntity();
        dispute.setDisputeId(UUID.fromString("00000000-0000-0000-0000-0000000000D1"));
        dispute.setBookingId(makeBooking().getBookingId());
        dispute.setSubmittedBy(makeBooking().getRequesterUserId());
        dispute.setReasonCode("EXPERT_ABSENT");
        dispute.setDescription("Expert did not join within 15 minutes.");
        dispute.setStatus("OPEN");
        return dispute;
    }

    static ConsultationDisputeEntity makeDispute(Consumer<ConsultationDisputeEntity> overrides) {
        ConsultationDisputeEntity dispute = makeDispute();
        overrides.accept(dispute);
        return dispute;
    }

    static SubmitDisputeRequest makeSubmitRequest() {
        SubmitDisputeRequest request = new SubmitDisputeRequest();
        request.setBookingId(makeBooking().getBookingId());
        request.setReasonCode("EXPERT_ABSENT");
        request.setDescription("Expert did not join within 15 minutes.");
        request.setEvidenceUrls(List.of());
        return request;
    }

    static SubmitDisputeRequest makeSubmitRequest(Consumer<SubmitDisputeRequest> overrides) {
        SubmitDisputeRequest request = makeSubmitRequest();
        overrides.accept(request);
        return request;
    }

    static PaymentTransactionEntity makePayment() {
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setPaymentId(UUID.fromString("00000000-0000-0000-0000-0000000000P1"));
        payment.setBookingId(makeBooking().getBookingId());
        payment.setGatewayName("VNPAY");
        payment.setGrossAmount(new BigDecimal("500000"));
        payment.setStatus("SUCCESS");
        return payment;
    }

    static RefundRecordEntity makeRefund(Consumer<RefundRecordEntity> overrides) {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundId(UUID.fromString("00000000-0000-0000-0000-0000000000R1"));
        refund.setPaymentId(makePayment().getPaymentId());
        refund.setDisputeId(makeDispute().getDisputeId());
        refund.setRefundAmount(new BigDecimal("500000"));
        refund.setStatus("PENDING");
        overrides.accept(refund);
        return refund;
    }
}
```

---

### DISP-TC-001 — Happy path: Mother submits valid dispute for own booking

**Severity:** `CRITICAL`
**Feature Under Test:** `DisputeService.submitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC78 TDS §6.1` sequence diagram + `V1__init_schema.sql` L969-984 (`status` default `'OPEN'`)

**Preconditions:**
- `FX-001` booking seeded (`requesterUserId = U1`)
- `FX-004` JWT for `U1`

**Test Steps:**
1. Arrange: `booking = ConsultationDisputeTestFactory.makeBooking()`; mock `bookingRepository.findById(B1)` returns it; mock `disputeRepository.findOpenDisputesByBookingId(B1)` returns empty list.
2. Act: `disputeService.submitDispute(makeSubmitRequest(), U1)`.
3. Assert: returned `DisputeResponse.status == "OPEN"`; `disputeRepository.save()` called once with `submittedBy == U1`.

**Expected Result (PASS):** `201`-equivalent response, `status="OPEN"`, `disputeRepository.save()` invoked exactly once.
**Expected Result (FAIL):** Any exception thrown, or dispute persisted with wrong `submittedBy`.

**Current Status:** 🔴 Not written
**Implementation Note:** `DisputeService` must call `DisputePolicy.assertCanSubmitDispute` before persistence.

---

### DISP-TC-002 — Ownership violation: non-owner submits dispute → 403 (`DISP-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `DisputePolicy.assertCanSubmitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/DisputePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC78 TDS ADR-DISPUTE-002`, `BR-RBAC`

**Preconditions:** `FX-002` — booking owned by `U1`, requester is `U2`.

**Test Steps:**
1. Arrange: `booking = makeBooking()` (owner `U1`).
2. Act: `disputePolicy.assertCanSubmitDispute(booking, U2)`.
3. Assert: throws `DisputeAuthorizationException` with code `DISP-004`.

**Expected Result (PASS):** Exception thrown, no dispute persisted.
**Expected Result (FAIL):** No exception / dispute created for non-owner.

**Current Status:** 🔴 Not written

---

### DISP-TC-003 — Duplicate open dispute for same booking → 409 (`DISP-002`)

**Severity:** `HIGH`
**Feature Under Test:** `DisputeService.submitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC78 TDS §2 Logic Issues L3` (service-level guard since no DB unique constraint exists)

**Preconditions:** `FX-003` — existing `OPEN` dispute for `B1`.

**Test Steps:**
1. Arrange: mock `disputeRepository.findOpenDisputesByBookingId(B1)` returns `List.of(existingDispute)`.
2. Act: `disputeService.submitDispute(makeSubmitRequest(), U1)`.
3. Assert: throws `DisputeConflictException` code `DISP-002`; `disputeRepository.save()` never called.

**Expected Result (PASS):** Exception `DISP-002`, no new row persisted.
**Expected Result (FAIL):** Second dispute silently created.

**Current Status:** 🔴 Not written

---

### DISP-TC-004 — Invalid `reasonCode` → 400 (`DISP-001`)

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputePolicy.validateReasonCode()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/DisputePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC78 TDS ADR-DISPUTE-004` taxonomy proposal

**Test Steps:**
1. Act: `disputePolicy.validateReasonCode("NOT_A_REAL_REASON")`.
2. Assert: throws `DisputeValidationException` code `DISP-001`.

**Expected Result (PASS):** Exception thrown for any value outside `{EXPERT_ABSENT, SCOPE_VIOLATION, TECHNICAL_ISSUE, OTHER}`.
**Expected Result (FAIL):** Arbitrary string accepted silently.

**Current Status:** 🔴 Not written

---

### DISP-TC-005 — Boundary: `description` length validation (0 / 2000 / 2001 chars)

**Severity:** `MEDIUM`
**Feature Under Test:** `SubmitDisputeRequest` DTO validation (`@NotBlank @Size(max=2000)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/SubmitDisputeRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC78 TDS §8.1` DTO annotation

**Test Steps:**
1. Act (blank): validate `makeSubmitRequest(r -> r.setDescription(""))`.
2. Assert: violation on `description` field (`@NotBlank`).
3. Act (2000 chars): validate `makeSubmitRequest(r -> r.setDescription("a".repeat(2000)))`.
4. Assert: no violation.
5. Act (2001 chars): validate `makeSubmitRequest(r -> r.setDescription("a".repeat(2001)))`.
6. Assert: violation on `description` field (`@Size(max=2000)`).

**Expected Result (PASS):** Boundary respected exactly at 2000/2001.
**Expected Result (FAIL):** Off-by-one on max length, or blank accepted.

**Current Status:** 🔴 Not written

---

### DISP-TC-006 — Booking not found → 404 (`DISP-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputeService.submitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC78 TDS §10` error table

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `disputeService.submitDispute(makeSubmitRequest(), U1)`.
3. Assert: throws `BookingNotFoundException` code `DISP-003`.

**Current Status:** 🔴 Not written

---

### DISP-TC-007 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `DisputeController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/DisputeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC78 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/bookings/{id}/disputes` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401`, no dispute created.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### DISP-TC-008 — Anti-pattern guard: refund is NOT auto-created on dispute submission

**Severity:** `CRITICAL`
**Feature Under Test:** `DisputeService.submitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC78 TDS ADR-DISPUTE-001` (manual approval gate) — this is the AP-CB-001 anti-pattern detector test

**Test Steps:**
1. Arrange: mock `RefundService` as a Mockito mock injected into `DisputeService` constructor (if wired) OR verify no `RefundRecordRepository` interaction occurs from this code path.
2. Act: `disputeService.submitDispute(makeSubmitRequest(), U1)`.
3. Assert: `refundRecordRepository` (or `RefundService`) has **zero** interactions (`verifyNoInteractions`).

**Expected Result (PASS):** No refund-related repository/service call occurs during submission.
**Expected Result (FAIL):** Any `RefundRecordRepository.save()` or `VnPayGatewayClient` call occurs — indicates AP-CB-001 (auto-approving refund without admin gate).

**Current Status:** 🔴 Not written
**Implementation Note:** This test is the primary automated defense against the CASE 2.0 anti-pattern flagged in TDS §17.4 (AP-CB-001).

---

### DISP-TC-009 — `processApprovedRefund` rejects non-APPROVED dispute → 409 (`DISP-005`)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.processApprovedRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC78 TDS ADR-DISPUTE-001`, `§8.1` `IRefundService` Javadoc

**Preconditions:** `FX-007` — dispute status `OPEN` (not `APPROVED`).

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns dispute with `status="OPEN"`.
2. Act: `refundService.processApprovedRefund(D1, adminId)`.
3. Assert: throws `RefundConflictException` code `DISP-005`; VNPay client never invoked (`verifyNoInteractions(vnPayGatewayClient)`).

**Expected Result (PASS):** Exception thrown, zero VNPay calls.
**Expected Result (FAIL):** Refund processed for a non-approved dispute (financial safety violation).

**Current Status:** 🔴 Not written

---

### DISP-TC-010 — VNPay refund happy path — terminal SUCCESS state

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.processApprovedRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC78 TDS §6.2` sequence diagram

**Preconditions:** `FX-006` — `APPROVED` dispute + `SUCCESS` payment; `FX-008` WireMock VNPay 200 stub.

**Test Steps:**
1. Arrange: mock dispute `status="APPROVED"`; mock payment lookup; WireMock stub returns `{status:"SUCCESS", gatewayRefundId:"VNP-REF-1"}`.
2. Act: `refundService.processApprovedRefund(D1, adminId)`.
3. Assert: `refundRecordRepository.save()` called with sequential states `PENDING`→`PROCESSING`→`SUCCESS`; final entity has `gatewayRefundId="VNP-REF-1"`, `processedAt != null`.

**Expected Result (PASS):** Refund record ends in `SUCCESS` with gateway reference populated.
**Expected Result (FAIL):** Wrong terminal state, or gateway reference missing.

**Current Status:** 🔴 Not written

---

### DISP-TC-011 — VNPay refund timeout/5xx → terminal FAILED state, no crash (`VNP-001`)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.processApprovedRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC78 TDS §6.4` error-path sequence diagram, `ADR-DISPUTE-003`

**Preconditions:** `FX-006` + `FX-009` WireMock VNPay `504` stub.

**Test Steps:**
1. Arrange: WireMock stub configured to return `504 Gateway Timeout`.
2. Act: `refundService.processApprovedRefund(D1, adminId)`.
3. Assert: throws `RefundGatewayException` code `VNP-001`; `refundRecordRepository` final state is `FAILED` (not stuck `PROCESSING`, not silently `SUCCESS`).

**Expected Result (PASS):** `VNP-001` surfaced to caller, DB record `FAILED`.
**Expected Result (FAIL):** Record left `PROCESSING` indefinitely, or exception swallowed silently, or (worst case) marked `SUCCESS` despite gateway failure.

**Current Status:** 🔴 Not written

---

### DISP-TC-012 — Refund idempotency: repeated approval call is a no-op (no second VNPay call)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow (double-spend/double-refund class)`
**Feature Under Test:** `RefundService.processApprovedRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC78 TDS ADR-DISPUTE-003`

**Preconditions:** `FX-010` — existing `refund_records` row already `status="PROCESSING"` for `disputeId=D1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByDisputeId(D1)` returns existing `PROCESSING` record.
2. Act: `refundService.processApprovedRefund(D1, adminId)` (second call, simulating retry/replay).
3. Assert: returns the existing record unchanged; `vnPayGatewayClient.refund()` is **never** invoked (`verifyNoInteractions`).

**Expected Result (PASS):** Zero additional VNPay calls; existing record returned as-is.
**Expected Result (FAIL):** A second VNPay refund call is dispatched (double-refund risk).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the core automated regression test for the idempotency ADR — must never regress.

---

### DISP-TC-013 — `DisputeSubmitted` event emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `DisputeService.submitDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC78 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `disputeService.submitDispute(makeSubmitRequest(), U1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `DisputeSubmitted` with `payload.bookingId == B1`, `payload.submittedBy == U1`, `payload.reasonCode == "EXPERT_ABSENT"`.

**Current Status:** 🔴 Not written

---

### DISP-TC-014 — `RefundProcessed` event emitted on terminal VNPay result

**Severity:** `HIGH`
**Feature Under Test:** `RefundService.processApprovedRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC78 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `refundService.processApprovedRefund(D1, adminId)` (SUCCESS path, `FX-008`).
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured `RefundProcessed` with `payload.status == "SUCCESS"`, `payload.refundId != null`.

**Current Status:** 🔴 Not written

---

### DISP-TC-015 — Response never leaks raw entity/internal fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `DisputeMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/DisputeMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"

**Test Steps:**
1. Act: `DisputeMapper.toResponse(makeDispute())`.
2. Assert: resulting `DisputeResponse` JSON serialization contains no field named `submittedBy`/`resolvedBy` as raw UUID exposing internal actor identity beyond what UC78 API contract (§9.2) declares.

**Expected Result (PASS):** Response matches exactly the `DisputeResponse` fields in TDS §9.2.
**Expected Result (FAIL):** Extra internal fields serialized.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### DISP-TC-INT-001 — E2E: submit dispute API flow (Testcontainers + WireMock)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/consultations/bookings/{id}/disputes` → DB persisted row
**Test File:** `src/test/java/com/carebridge/backend/consultation/DisputeSubmissionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-001` booking row inserted via JPA before test

**Test Steps:**
1. Seed booking `B1` owned by `U1` via repository.
2. `POST /api/v1/consultations/bookings/{B1}/disputes` with JWT for `U1` and valid body.
3. Assert response `201`, body matches §9.2 schema.
4. Assert DB: `SELECT * FROM consultation_disputes WHERE booking_id = 'B1'` returns exactly 1 row with `status='OPEN'`.

**Expected Result (PASS):**
- API `201`; DB row present with correct `submitted_by`, `reason_code`.

**Expected Result (FAIL):**
- API error, or DB row missing/duplicated.

**DB Assertion:**
```java
ConsultationDisputeEntity record = disputeRepository.findByBookingId(bookingId).get(0);
assertThat(record.getStatus()).isEqualTo("OPEN");
assertThat(record.getSubmittedBy()).isEqualTo(U1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DISP-TC-001` | `DisputeServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-002` | `DisputePolicyTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-003` | `DisputeServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-004` | `DisputePolicyTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-005` | `SubmitDisputeRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-006` | `DisputeServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-007` | `DisputeControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-008` | `DisputeServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-009` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-010` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-011` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-012` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-013` | `DisputeServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-014` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-015` | `DisputeMapperTest.java` | `[ ]` | `[ ]` | |
| `DISP-TC-INT-001` | `DisputeSubmissionIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DisputeService implements IDisputeService {
    @Override
    public DisputeResponse submitDispute(SubmitDisputeRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public DisputeResponse getDispute(UUID disputeId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Service
public class RefundService implements IRefundService {
    @Override
    public RefundRecordEntity processApprovedRefund(UUID disputeId, UUID approvedBy) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `DISP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** Booking service (UC-75/UC-76 — creates `consultation_bookings` rows) implemented and deployed to a test environment
- [ ] **BLOCKING:** Payment capture service (UC-76/`3.1.2.1`) implemented, populates `payment_transactions` with a real `SUCCESS`-equivalent state
- [ ] **BLOCKING:** Session service (UC-77) implemented if booking-eligibility check for dispute (`DISP-006`) depends on session completion — **Open, confirm exact eligible booking statuses with Tech Lead**
- [ ] `UC78_SubmitDisputeOrRefundRequest_TDS.md` reviewed and Approved
- [ ] ADR-DISPUTE-001 and ADR-DISPUTE-004 confirmed by Product/Tech Lead (currently `Proposed`)
- [ ] Test fixtures (§3 TDS-05) prepared
- [ ] WireMock VNPay stub server configured for integration tests (no existing VNPay integration pattern found in repo — this is the first)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers + WireMock)
- [ ] Test coverage ≥ 80% lines cho `DisputeService`, `RefundService`, `DisputePolicy`
- [ ] Không có business logic trong `DisputeController`
- [ ] Không có PII/evidence URL xuất hiện plaintext trong logs
- [ ] `DISP-TC-008` and `DISP-TC-012` (anti-auto-approve + idempotency) pass — these are release-blocking financial-safety gates

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — mỗi test dùng `ConsultationDisputeTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- Booking/Payment/Session prerequisite services not yet deployed to test environment
- VNPay sandbox credentials unavailable for WireMock contract verification
- New architecture unknown discovered (e.g., booking eligibility statuses for dispute differ from assumption) requiring Tech Lead review

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/

# IF optional index migration (TDS §5.3) was applied:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_refund_records_payment_id, idx_refund_records_dispute_id;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260702110000';"

# Gap vẫn OPEN → giữ nguyên entry trong task tracker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-DISPUTE-00X) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `DISP-TC-007`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces) | G-3 |
| **AP-CB-001** *(project-specific)* | **Auto-approving refund without admin gate** | Any test path where `submitDispute()` results in a VNPay call or `refund_records` row without an intervening `APPROVED` state check | `DISP-TC-008`, `DISP-TC-009` explicitly assert `verifyNoInteractions` on refund/VNPay path | **Release-blocking** |
| **AP-CB-002** *(project-specific)* | **Allowing review/dispute action before precondition met** *(cross-reference — applies to UC79 sibling spec; noted here for consistency since both features share the anti-pattern class)* | N/A for UC78 (dispute has no "session completed" precondition; booking-eligibility check `DISP-006` is the analogous guard) | `DISP-TC-006` (booking existence); booking-status eligibility test **Open** pending `DISP-006` confirmation | Non-blocking (Open item) |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`DISP-TC-008`, `DISP-TC-009`, `DISP-TC-012`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC78 v1.0 — Draft. Total test cases: 16 (15 unit/component + 1 integration). Critical-severity: 7 (`DISP-TC-001, 002, 007, 008, 009, 011, 012` — financial-safety and RBAC gates). Requires Approved status change only by user/Tech Lead.*
