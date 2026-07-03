# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC210 — Approve or Reject Refund — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-210`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L876-1015, L1868-1890) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.9 (L4515-4534)
- `04_Implement/UC210_ApproveOrRejectRefund/UC210_ApproveOrRejectRefund_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` — reused ADR-DISPUTE-001/003, `VnPayGatewayClient` interface
- `03_Design/UI_UX/WebAppScreen/CB-201 Approve Refund Confirmation (UC-210)/code.html`, `CB-202 Reject Refund Confirmation (UC-210)/code.html`
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC-210 |

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
| **Feature / Gap ID** | `UC-210` |
| **Module** | `Consultation — Refund Approval` |
| **Spec gốc** | `CB-CONSULTATION-IMP-210` (UC-210 TDS) |
| **Priority** | 🟡 P2 — Medium (SRS §3.3.14.9); financial-safety tests inside are release-blocking regardless of priority |
| **Sprint** | Consultation batch UC203→UC210 |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Confidential` (refund amount, gateway transaction id, commission/settlement impact figures, admin actor identity) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC-209 Resolve Consultation Dispute (sets `consultation_disputes.status='APPROVED'` — BLOCKING, see §6 Entry Criteria), Payment capture service (`payment_transactions` paid state — BLOCKING) |
| **Downstream Consumers** | Commission/settlement recalculation (`commission_records`, `settlement_records` — out of scope consumers of `RefundProcessed`), Notification service |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC210 TDS §17.2` Constraint Injection Block (C1-C6) |
| **Constraints Injected** | C1 (APPROVED-dispute precondition, no mutation of dispute status), C2 (reused `VnPayGatewayClient` + idempotency), C3 (REJECTED terminal, no VNPay call on reject), C4 (System Admin only), C5 (package layout, no entity leakage), C6 (no invented partial-refund policy) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |
| **Data Classification Note** | Refund amount, gateway transaction IDs, and admin actor identity are `Confidential` financial data; all fixtures below use SYNTHETIC values only. Audit fields (`approved_by`, `requested_at`, `processed_at`) are contractually required outputs, not optional telemetry — tests assert their presence explicitly (§RFND-TC-014/015). |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-210 text is generic boilerplate; does not state that refund approval requires a *human* gate distinct from an automatic VNPay call | UC78's `ADR-DISPUTE-001` (reused, `Proposed`) establishes the manual-approval-before-VNPay-call rule; UC-210 IS that manual gate. Tests assert VNPay is called ONLY from `approveRefund()` after `RefundPolicy.assertDisputeApproved()` passes, never automatically | `RFND-TC-001`, `RFND-TC-003` assert the precondition check runs before any `VnPayGatewayClient` interaction |
| L2 | SRS does not separate "dispute outcome" from "the refund decision itself" | Schema gives `refund_records` its own `approved_by` FK distinct from `consultation_disputes.resolved_by` (UC210 TDS ADR-REFUND-001, `Proposed`); CB-202 mockup shows an independent refund-reject action | Tests assert `rejectRefund()`/`approveRefund()` NEVER write `consultation_disputes.status` — `RFND-TC-013` is the dedicated boundary guard (AP-CB-003) |
| L3 | SRS is silent on where a refund *rejection* is durably stored | `refund_records.status` has no DB CHECK (verified); UC210 TDS `ADR-REFUND-002` (`Proposed`) defines an application-level terminal `REJECTED` status, distinct from gateway `FAILED` | `RFND-TC-002`, `RFND-TC-008`, `RFND-TC-015` encode the `REJECTED` terminal path and its distinctness from `FAILED` |
| L4 | VNPay refund idempotency guard is defined by UC78 `ADR-DISPUTE-003` (reused, `Accepted`) but not restated per-endpoint | `refund_records.status` PENDING→PROCESSING guard prevents double-dispatch; UC-210 reuses this verbatim, does not redefine it | `RFND-TC-006`, `RFND-TC-007` assert `vnPayGatewayClient.refund()` is invoked at most once per dispute regardless of retry count |
| L5 | Partial-refund policy (percentages, thresholds) is **not defined anywhere in SRS** | `payment_transactions.refund_amount` / `commission_records.refund_amount` are cumulative numeric columns — structurally partial-refund-capable, but UC210 TDS `ADR-REFUND-003` explicitly marks the *policy* `Open` and only commits to a correct *full-refund* mechanism bounded by `refundable = gross_amount − refund_amount` | `RFND-TC-011`, `RFND-TC-012` test the bounded-amount mechanism only (full refund default + reject-if-exceeds-refundable); **no test encodes an invented partial-refund percentage** — this remains `Open` per ADR-REFUND-003 |
| L6 | No DB unique constraint prevents multiple refund decisions per dispute | No unique index on `refund_records.dispute_id` (schema verified, only FK) — service-level guard required | `RFND-TC-007`, `RFND-TC-008` encode the service-level 409 (`RFND-002`) conflict check since DB does not enforce it |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Refund Approval module bao gồm các layer:
├── Domain (RefundPolicy — pure logic, no deps)
├── Services (RefundService — mock JPA Repository + VnPayGatewayClient với Mockito)
├── Controller (RefundController — mock Service với @WebMvcTest, @PreAuthorize check)
└── Integration (Testcontainers PostgreSQL + WireMock cho VNPay, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-210 (§3.3.14.9) | Trigger, actor (System Admin), exceptions E1-E3, Precondition PRE-3/PRE-4 |
| `UC210 TDS` ADR-REFUND-001/002/003 | Precondition gate, REJECTED terminal, refundable-amount bound |
| `UC78 TDS` ADR-DISPUTE-001/003 (reused) | Manual-approval gate, VNPay idempotency guard |
| `V1__init_schema.sql` L876-1015, L1868-1890 | Column names/types/defaults/FKs as persistence oracle |
| `BR-RBAC` / `BR-CONSULTATION` | Authorization (System Admin only), auditable lifecycle |
| `UC210 TDS §9-10` | API contract, error codes (`RFND-0xx`, reused `VNP-001`) |
| CB-201 / CB-202 mockups | Refund preview fields (refundable amount, commission impact), reject-reason requirement |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy approve — APPROVED dispute + paid payment → refund `SUCCESS`, `payment_transactions` updated | `RefundService.approveRefund()` | `RFND-TC-001` |
| TC-COND-002 | Happy reject — reason stored, terminal `REJECTED`, no VNPay call | `RefundService.rejectRefund()` | `RFND-TC-002` |
| TC-COND-003 | Precondition fail on approve — dispute not `APPROVED` | `RefundPolicy.assertDisputeApproved()` | `RFND-TC-003` |
| TC-COND-004 | Precondition fail on reject — dispute not `APPROVED` | `RefundPolicy.assertDisputeApproved()` | `RFND-TC-004` |
| TC-COND-005 | VNPay timeout/5xx → terminal `FAILED`, `VNP-001`, no auto-retry | `RefundService.approveRefund()` | `RFND-TC-005` |
| TC-COND-006 | Idempotency — duplicate approve while `PROCESSING` → no second VNPay call | `RefundService.approveRefund()` | `RFND-TC-006` |
| TC-COND-007 | Idempotency — duplicate approve after `SUCCESS` → `RFND-002`, no VNPay call | `RefundService.approveRefund()` | `RFND-TC-007` |
| TC-COND-008 | Reject attempted on a dispute that already has a terminal refund → `RFND-002` | `RefundService.rejectRefund()` | `RFND-TC-008` |
| TC-COND-009 | Non-admin caller (e.g. `MODERATOR`) → `403`/`RFND-004` | `RefundController` security | `RFND-TC-009` |
| TC-COND-010 | Unauthenticated request → `401` | `RefundController` security filter | `RFND-TC-010` |
| TC-COND-011 | Refund amount exceeds refundable bound → `RFND-006` (422) | `RefundPolicy.computeRefundableAmount()` | `RFND-TC-011` |
| TC-COND-012 | Refund amount omitted → defaults to full refundable amount | `RefundService.approveRefund()` | `RFND-TC-012` |
| TC-COND-013 | Boundary guard — approve/reject NEVER writes `consultation_disputes.status` (AP-CB-003) | `RefundService` | `RFND-TC-013` |
| TC-COND-014 | Audit fields populated on approve (`approved_by`, `requested_at`, `processed_at`) | `RefundService.approveRefund()` | `RFND-TC-014` |
| TC-COND-015 | Audit fields + `REJECTED` distinctness from `FAILED` on reject | `RefundService.rejectRefund()` | `RFND-TC-015` |
| TC-COND-016 | Refund preview returns refundable amount + commission impact + dispute status | `RefundService.getRefundPreview()` | `RFND-TC-016` |
| TC-COND-017 | `RefundApproved` + `RefundProcessed(SUCCESS)` events emitted with correct payload | `RefundService.approveRefund()` | `RFND-TC-017` |
| TC-COND-018 | `RefundRejected` event emitted with correct payload | `RefundService.rejectRefund()` | `RFND-TC-018` |
| TC-COND-019 | Response never leaks raw entity/internal fields | `RefundMapper` | `RFND-TC-019` |
| TC-COND-020 | Missing/blank reject `reason` → 400 (`RFND-001`) | DTO validation (`RejectRefundRequest`) | `RFND-TC-020` |
| TC-COND-021 | Dispute or linked payment not found → 404 (`RFND-003`) | `RefundService.approveRefund()`/`getRefundPreview()` | `RFND-TC-021` |
| TC-COND-022 | E2E — full approve-refund API flow via MockMvc/Testcontainers/WireMock | `RefundController` + real DB + stubbed VNPay | `RFND-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `refundAmount` valid range vs. `≤0` vs. `> refundable` | Confirms `RFND-006` boundary enforcement |
| Boundary Value Analysis | `refundAmount` at exactly `refundable`, `refundable + 0.01`, omitted (defaults to full) | Confirms ADR-REFUND-003 bound is inclusive |
| State Transition Testing | Refund `PENDING → PROCESSING → {SUCCESS\|FAILED}` and `PENDING → REJECTED` | Validates ADR-DISPUTE-003 (reused) + ADR-REFUND-002 state machines |
| Error Guessing | Replay of approve call (idempotency attack), calling reject after a refund is already terminal, non-admin role escalation | Idempotency + RBAC attack surface |
| Decision Table | `disputeStatus × refundStatus (existing) × callerRole` combinations | 200 vs 403 vs 409 vs 422 branching |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationDisputeEntity{disputeId=D1, status='APPROVED', bookingId=B1}` | Happy path precondition satisfied |
| `FX-002` | DB seed | `ConsultationDisputeEntity{disputeId=D1, status='OPEN'}` | Precondition-fail fixture (`RFND-005`) |
| `FX-003` | DB seed | `PaymentTransactionEntity{paymentId=P1, bookingId=B1, grossAmount=1250000, refundAmount=0, status='SUCCESS', gatewayTransactionId='VNP-TXN-1'}` | Refund amount / VNPay call source |
| `FX-004` | JWT | `{sub: 'ADMIN1', role: 'SYSTEM_ADMIN'}` | Auth context — authorized admin |
| `FX-005` | JWT | `{sub: 'MOD1', role: 'MODERATOR'}` | Auth context — non-admin (RBAC violation) |
| `FX-006` | WireMock stub | VNPay refund endpoint returns `200 {status:"SUCCESS", gatewayRefundId:"VNP-REF-1"}` | Approve happy path |
| `FX-007` | WireMock stub | VNPay refund endpoint returns `504 Gateway Timeout` | Approve failure path |
| `FX-008` | DB seed | `RefundRecordEntity{refundId=R1, disputeId=D1, paymentId=P1, status='PROCESSING'}` | Idempotency guard — mid-flight |
| `FX-009` | DB seed | `RefundRecordEntity{refundId=R1, disputeId=D1, paymentId=P1, status='SUCCESS', gatewayRefundId='VNP-REF-1'}` | Idempotency guard — already terminal (`RFND-002`) |
| `FX-010` | DB seed | `CommissionRecordEntity{paymentId=P1, refundAmount=0, commissionAmount=150000, settlementStatus='PENDING'}` | Preview commission-impact display |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `RefundPolicy`, `RefundService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest RefundController` | ✅ MockMvc full flow | ✅ RBAC / precondition bypass |
| Web | ✅ `ApproveRefundModal.tsx`/`RejectRefundModal.tsx` unit (component test) | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

class RefundTestFactory {

    static ConsultationDisputeEntity makeApprovedDispute() {
        ConsultationDisputeEntity dispute = new ConsultationDisputeEntity();
        dispute.setDisputeId(UUID.fromString("00000000-0000-0000-0000-0000000000D1"));
        dispute.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        dispute.setStatus("APPROVED");
        return dispute;
    }

    static ConsultationDisputeEntity makeApprovedDispute(Consumer<ConsultationDisputeEntity> overrides) {
        ConsultationDisputeEntity dispute = makeApprovedDispute();
        overrides.accept(dispute);
        return dispute;
    }

    static PaymentTransactionEntity makePayment() {
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setPaymentId(UUID.fromString("00000000-0000-0000-0000-0000000000P1"));
        payment.setBookingId(makeApprovedDispute().getBookingId());
        payment.setGatewayName("VNPAY");
        payment.setGatewayTransactionId("VNP-TXN-1");
        payment.setGrossAmount(new BigDecimal("1250000"));
        payment.setRefundAmount(BigDecimal.ZERO);
        payment.setStatus("SUCCESS");
        return payment;
    }

    static PaymentTransactionEntity makePayment(Consumer<PaymentTransactionEntity> overrides) {
        PaymentTransactionEntity payment = makePayment();
        overrides.accept(payment);
        return payment;
    }

    static RefundRecordEntity makeRefund(Consumer<RefundRecordEntity> overrides) {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundId(UUID.fromString("00000000-0000-0000-0000-0000000000R1"));
        refund.setDisputeId(makeApprovedDispute().getDisputeId());
        refund.setPaymentId(makePayment().getPaymentId());
        refund.setRefundAmount(new BigDecimal("1250000"));
        refund.setStatus("PENDING");
        overrides.accept(refund);
        return refund;
    }

    static ApproveRefundRequest makeApproveRequest() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(null); // defaults to full refundable (ADR-REFUND-003)
        request.setReason("Service not delivered; partner cancelled.");
        return request;
    }

    static ApproveRefundRequest makeApproveRequest(Consumer<ApproveRefundRequest> overrides) {
        ApproveRefundRequest request = makeApproveRequest();
        overrides.accept(request);
        return request;
    }

    static RejectRefundRequest makeRejectRequest() {
        RejectRefundRequest request = new RejectRefundRequest();
        request.setReason("Evidence shows service was delivered per commitment; policy denies refund.");
        return request;
    }

    static RejectRefundRequest makeRejectRequest(Consumer<RejectRefundRequest> overrides) {
        RejectRefundRequest request = makeRejectRequest();
        overrides.accept(request);
        return request;
    }

    static CommissionRecordEntity makeCommission() {
        CommissionRecordEntity commission = new CommissionRecordEntity();
        commission.setPaymentId(makePayment().getPaymentId());
        commission.setCommissionAmount(new BigDecimal("150000"));
        commission.setRefundAmount(BigDecimal.ZERO);
        commission.setSettlementStatus("PENDING");
        return commission;
    }
}
```

---

### RFND-TC-001 — Happy path: Approve refund → VNPay `SUCCESS`

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC210 TDS §6.1` sequence diagram; `V1__init_schema.sql` L986-1000 (`refund_records`), L923-939 (`payment_transactions`)

**Preconditions:**
- `FX-001` dispute `status='APPROVED'`; `FX-003` payment `status='SUCCESS'`, `grossAmount=1250000`, `refundAmount=0`
- `FX-006` WireMock VNPay 200 stub
- No existing `refund_records` row for `D1`

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns `FX-001`; mock `paymentRepository.findByBookingId(B1)` returns `FX-003`; mock `refundRecordRepository.findByDisputeId(D1)` returns `Optional.empty()`; `vnPayGatewayClient.refund(...)` returns `{gatewayRefundId:"VNP-REF-1", status:"SUCCESS"}`.
2. Act: `refundService.approveRefund(D1, RefundTestFactory.makeApproveRequest(), ADMIN1)`.
3. Assert: `refundRecordRepository.save()` sequence reflects `PENDING`→`PROCESSING`→`SUCCESS`; final `RefundDecisionResponse.status=="SUCCESS"`, `gatewayRefundId=="VNP-REF-1"`; `paymentRepository.save()` called with `refundAmount==1250000`, `refundedAt != null`.

**Expected Result (PASS):** Refund reaches `SUCCESS`; payment updated in the same operation; `vnPayGatewayClient.refund()` invoked exactly once.
**Expected Result (FAIL):** Wrong terminal state, payment not updated, or VNPay called before the `APPROVED` precondition check.

**Current Status:** 🔴 Not written
**Implementation Note:** `RefundPolicy.assertDisputeApproved()` MUST run before any repository write.

---

### RFND-TC-002 — Happy path: Reject refund (no VNPay call)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC210 TDS §6.2` sequence diagram, `ADR-REFUND-002`

**Preconditions:** `FX-001` dispute `APPROVED`; no existing refund row for `D1`.

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns `FX-001`; mock `refundRecordRepository.findByDisputeId(D1)` returns `Optional.empty()`.
2. Act: `refundService.rejectRefund(D1, RefundTestFactory.makeRejectRequest(), ADMIN1)`.
3. Assert: `refundRecordRepository.save()` called once with `status=="REJECTED"`, `reason` matches request, `approvedBy==ADMIN1`, `processedAt != null`; `vnPayGatewayClient` has **zero** interactions (`verifyNoInteractions`).

**Expected Result (PASS):** Terminal `REJECTED` row persisted; no VNPay call; `consultation_disputes` untouched.
**Expected Result (FAIL):** VNPay called, or dispute status mutated, or reason not persisted.

**Current Status:** 🔴 Not written

---

### RFND-TC-003 — Precondition fail on approve: dispute not `APPROVED` → 409 (`RFND-005`)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundPolicy.assertDisputeApproved()` via `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/RefundPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC210 TDS ADR-REFUND-001`, `§10` error table

**Preconditions:** `FX-002` dispute `status='OPEN'`.

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns `FX-002`.
2. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)`.
3. Assert: throws `RefundPreconditionException` code `RFND-005`; `refundRecordRepository.save()` and `vnPayGatewayClient.refund()` never called (`verifyNoInteractions` on both).

**Expected Result (PASS):** Exception thrown before any persistence/gateway call.
**Expected Result (FAIL):** Refund created or VNPay called for a non-`APPROVED` dispute (financial-safety violation).

**Current Status:** 🔴 Not written

---

### RFND-TC-004 — Precondition fail on reject: dispute not `APPROVED` → 409 (`RFND-005`)

**Severity:** `HIGH`
**Feature Under Test:** `RefundPolicy.assertDisputeApproved()` via `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/RefundPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC210 TDS ADR-REFUND-001` (precondition applies symmetrically to both decisions)

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns `FX-002` (`OPEN`).
2. Act: `refundService.rejectRefund(D1, makeRejectRequest(), ADMIN1)`.
3. Assert: throws `RefundPreconditionException` code `RFND-005`; no `refund_records` row created.

**Expected Result (PASS):** Exception thrown, no persistence.
**Expected Result (FAIL):** Reject silently accepted for a non-`APPROVED` dispute.

**Current Status:** 🔴 Not written

---

### RFND-TC-005 — VNPay timeout/5xx → terminal `FAILED`, no auto-retry (`VNP-001`)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC210 TDS §6.3` error-path sequence diagram, `ADR-DISPUTE-003` (reused)

**Preconditions:** `FX-001` + `FX-003`; `FX-007` WireMock VNPay `504` stub.

**Test Steps:**
1. Arrange: `vnPayGatewayClient.refund(...)` throws `VnPayGatewayException`.
2. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)`.
3. Assert: throws `RefundGatewayException` code `VNP-001`; `refundRecordRepository` final state is `FAILED` (not stuck `PROCESSING`, not `SUCCESS`); `paymentRepository.save()` is **never** called (no money moved).

**Expected Result (PASS):** `VNP-001` surfaced, DB `FAILED`, payment untouched.
**Expected Result (FAIL):** Record left `PROCESSING` indefinitely, exception swallowed, or payment updated despite gateway failure.

**Current Status:** 🔴 Not written

---

### RFND-TC-006 — Idempotency: duplicate approve while refund `PROCESSING` → no second VNPay call

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow (double-spend/double-refund class)`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-DISPUTE-003` (reused from UC78), `UC210 TDS §6.4`

**Preconditions:** `FX-008` — existing `refund_records` row `status='PROCESSING'` for `D1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByDisputeId(D1)` returns `FX-008`.
2. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)` (second/concurrent call).
3. Assert: `vnPayGatewayClient.refund()` is **never** invoked (`verifyNoInteractions`); method returns the existing `PROCESSING` record unchanged (or a well-defined in-flight response — no new row created).

**Expected Result (PASS):** Zero additional VNPay calls; no duplicate `refund_records` row.
**Expected Result (FAIL):** A second VNPay refund call is dispatched (double-refund risk).

**Current Status:** 🔴 Not written
**Implementation Note:** Core automated regression test for the reused idempotency ADR — must never regress.

---

### RFND-TC-007 — Idempotency: duplicate approve after `SUCCESS` → `RFND-002`, no VNPay call

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-DISPUTE-003` (reused), `UC210 TDS §10` error table

**Preconditions:** `FX-009` — existing `refund_records` row `status='SUCCESS'` for `D1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByDisputeId(D1)` returns `FX-009`.
2. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)`.
3. Assert: throws `RefundConflictException` code `RFND-002`; `vnPayGatewayClient.refund()` never invoked.

**Expected Result (PASS):** `RFND-002`, zero VNPay calls.
**Expected Result (FAIL):** A second refund is dispatched for an already-`SUCCESS` dispute.

**Current Status:** 🔴 Not written

---

### RFND-TC-008 — Reject attempted on a dispute with an existing terminal refund → `RFND-002`

**Severity:** `MEDIUM`
**Feature Under Test:** `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC210 TDS §2 Logic Issues L6` (service-level guard, no DB unique constraint)

**Preconditions:** `FX-009` — existing `SUCCESS` refund for `D1`.

**Test Steps:**
1. Arrange: mock `refundRecordRepository.findByDisputeId(D1)` returns `FX-009`.
2. Act: `refundService.rejectRefund(D1, makeRejectRequest(), ADMIN1)`.
3. Assert: throws `RefundConflictException` code `RFND-002`; no new row created.

**Expected Result (PASS):** Exception thrown, existing `SUCCESS` record untouched.
**Expected Result (FAIL):** A `REJECTED` row is created alongside an already-`SUCCESS` refund.

**Current Status:** 🔴 Not written

---

### RFND-TC-009 — Non-admin caller (e.g. `MODERATOR`) → 403 (`RFND-004`)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `RefundController` (`@PreAuthorize`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/RefundControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC210 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/disputes/{id}/refund/approve` with `FX-005` JWT (`role=MODERATOR`).
2. Assert: `403 Forbidden`, body `error.code == "RFND-004"`.
3. Repeat for `/refund/reject`.

**Expected Result (PASS = hệ thống an toàn):** `403` for both endpoints; no refund row created.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed for a non-admin role.

**Current Status:** 🔴 Not written

---

### RFND-TC-010 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `RefundController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/RefundControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC210 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/disputes/{id}/refund/approve` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS):** `401`, no refund created.
**Expected Result (FAIL):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### RFND-TC-011 — Refund amount exceeds refundable bound → 422 (`RFND-006`)

**Severity:** `HIGH`
**Feature Under Test:** `RefundPolicy.computeRefundableAmount()` via `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/RefundPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC210 TDS ADR-REFUND-003`

**Preconditions:** `FX-003` payment `grossAmount=1250000`, `refundAmount=0` → `refundable=1250000`.

**Test Steps:**
1. Act: `refundService.approveRefund(D1, makeApproveRequest(r -> r.setRefundAmount(new BigDecimal("1250000.01"))), ADMIN1)`.
2. Assert: throws `RefundValidationException` code `RFND-006`; no VNPay call.
3. Boundary: `refundAmount == 1250000` (exactly refundable) → accepted (no exception).

**Expected Result (PASS):** Amount strictly `> refundable` rejected; amount `== refundable` accepted.
**Expected Result (FAIL):** Off-by-one at the boundary, or an over-refund silently processed.

**Current Status:** 🔴 Not written

---

### RFND-TC-012 — Refund amount omitted → defaults to full refundable amount

**Severity:** `MEDIUM`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC210 TDS ADR-REFUND-003` (mechanism supports full refund by default)

**Test Steps:**
1. Act: `refundService.approveRefund(D1, makeApproveRequest(r -> r.setRefundAmount(null)), ADMIN1)`.
2. Assert: `vnPayGatewayClient.refund()` called with `amount == refundable (1250000)`.

**Expected Result (PASS):** Full refundable amount used when `refundAmount` is null.
**Expected Result (FAIL):** Null amount causes an exception or defaults to zero/incorrect value.

**Current Status:** 🔴 Not written

---

### RFND-TC-013 — Boundary guard: UC-210 NEVER mutates `consultation_disputes.status` (AP-CB-003)

**Severity:** `CRITICAL`
**Feature Under Test:** `RefundService.approveRefund()` + `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC210 TDS §1.3` Scope Boundary, `AP-CB-003` (§17.4)

**Test Steps:**
1. Arrange: mock `disputeRepository` (or an equivalent read-only interface) such that any write method call (`save`/`update`) would be observable via Mockito.
2. Act: run both `approveRefund()` (happy path, `FX-001`+`FX-003`+`FX-006`) and `rejectRefund()` (happy path, `FX-001`).
3. Assert: `disputeRepository` receives **only** read invocations (`findById`) — `verify(disputeRepository, never()).save(any())` and no `@Modifying` update query is invoked on `consultation_disputes`.

**Expected Result (PASS):** Zero write interactions with the dispute repository across both flows.
**Expected Result (FAIL):** Any write to `consultation_disputes` — indicates AP-CB-003 (UC-210 overstepping into UC-209's exclusive scope).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense against the CASE 2.0 project-specific anti-pattern AP-CB-003 flagged in TDS §17.4.

---

### RFND-TC-014 — Audit fields populated on approve (`approved_by`, `requested_at`, `processed_at`)

**Severity:** `HIGH`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC210 TDS §4.3` NFR Security (audit requirement), `V1__init_schema.sql` L986-1000

**Test Steps:**
1. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)` (happy path, `FX-006` SUCCESS stub).
2. Assert: persisted/returned entity has `approvedBy == ADMIN1`, `requestedAt != null` (set at `PENDING` creation), `processedAt != null` (set at terminal `SUCCESS`), `processedAt >= requestedAt`.

**Expected Result (PASS):** All three audit fields populated and temporally consistent.
**Expected Result (FAIL):** Any audit field null/missing, or `approvedBy` not matching the calling admin.

**Current Status:** 🔴 Not written

---

### RFND-TC-015 — Audit fields on reject + `REJECTED` distinctness from `FAILED`

**Severity:** `HIGH`
**Feature Under Test:** `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC210 TDS ADR-REFUND-002`

**Test Steps:**
1. Act: `refundService.rejectRefund(D1, makeRejectRequest(), ADMIN1)`.
2. Assert: persisted entity has `status == "REJECTED"` (NOT `"FAILED"`), `reason` equals the request reason, `approvedBy == ADMIN1`, `processedAt != null`, `gatewayRefundId == null`.

**Expected Result (PASS):** `REJECTED` is a distinct, correctly audited terminal state.
**Expected Result (FAIL):** Reject conflated with `FAILED`, or reason/audit fields missing.

**Current Status:** 🔴 Not written

---

### RFND-TC-016 — Refund preview returns refundable amount + commission impact + dispute status

**Severity:** `MEDIUM`
**Feature Under Test:** `RefundService.getRefundPreview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC210 TDS §9.2` preview response schema; CB-201 mockup ("Số tiền hoàn trả", "Tác động đến đối soát")

**Preconditions:** `FX-001`, `FX-003`, `FX-010` commission record (`commissionAmount=150000`).

**Test Steps:**
1. Act: `refundService.getRefundPreview(D1, ADMIN1)`.
2. Assert: `RefundPreviewResponse.refundableAmount == 1250000`, `grossAmount == 1250000`, `alreadyRefunded == 0`, `disputeStatus == "APPROVED"`, `commissionImpact == 150000`, `gatewayName == "VNPAY"`.

**Expected Result (PASS):** All preview fields match the schema oracle.
**Expected Result (FAIL):** Incorrect refundable computation or missing commission-impact figure.

**Current Status:** 🔴 Not written

---

### RFND-TC-017 — `RefundApproved` + `RefundProcessed(SUCCESS)` events emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `RefundService.approveRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC210 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `refundService.approveRefund(D1, makeApproveRequest(), ADMIN1)` (SUCCESS path).
2. Assert: `eventPublisher.publishEvent(captor.capture())` captures, in order, a `RefundApproved` (payload `refundId`, `disputeId==D1`, `approvedBy==ADMIN1`) followed by a `RefundProcessed` (payload `status=="SUCCESS"`, `refundId != null`, `gatewayRefundId=="VNP-REF-1"`).

**Expected Result (PASS):** Both events emitted with correct payload and order.
**Expected Result (FAIL):** Missing event, wrong payload, or wrong order.

**Current Status:** 🔴 Not written

---

### RFND-TC-018 — `RefundRejected` event emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `RefundService.rejectRefund()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `UC210 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `refundService.rejectRefund(D1, makeRejectRequest(), ADMIN1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())` captures a `RefundRejected` with `payload.disputeId==D1`, `payload.reason` matching the request, `payload.rejectedBy==ADMIN1`.

**Expected Result (PASS):** Event emitted with correct payload.
**Expected Result (FAIL):** No event, or `RefundProcessed`/`RefundApproved` incorrectly emitted instead.

**Current Status:** 🔴 Not written

---

### RFND-TC-019 — Response never leaks raw entity/internal fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `RefundMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/RefundMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CLAUDE.md` — "Never expose JPA entities in API responses"; `UC210 TDS §8.1` DTO shape

**Test Steps:**
1. Act: `RefundMapper.toResponse(RefundTestFactory.makeRefund(r -> {}))`.
2. Assert: resulting `RefundDecisionResponse` JSON serialization contains exactly the fields declared in TDS §9.2 (`refundId`, `disputeId`, `paymentId`, `refundAmount`, `currency`, `status`, `gatewayRefundId`, `reason`, `requestedAt`, `processedAt`) and no raw entity/internal fields.

**Expected Result (PASS):** Response matches TDS §9.2 schema exactly.
**Expected Result (FAIL):** Extra internal fields serialized (e.g., raw JPA entity references).

**Current Status:** 🔴 Not written

---

### RFND-TC-020 — Missing/blank reject `reason` → 400 (`RFND-001`)

**Severity:** `MEDIUM`
**Feature Under Test:** `RejectRefundRequest` DTO validation (`@NotBlank`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/RejectRefundRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `UC210 TDS §8.1` DTO annotation, `§10` error table (`RFND-001`)

**Test Steps:**
1. Act: validate `RefundTestFactory.makeRejectRequest(r -> r.setReason(""))`.
2. Assert: violation on `reason` field (`@NotBlank`); `RefundController` maps this to `400` with `error.code == "RFND-001"`.
3. Act: validate with `reason = null`.
4. Assert: same violation.

**Expected Result (PASS):** Blank/null reason rejected before reaching `RefundService`.
**Expected Result (FAIL):** Reject accepted with an empty/absent reason, defeating the CB-202 "reason shown to user" requirement.

**Current Status:** 🔴 Not written

---

### RFND-TC-021 — Dispute or linked payment not found → 404 (`RFND-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `RefundService.approveRefund()` / `RefundService.getRefundPreview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RefundServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `UC210 TDS §10` error table (`RFND-003`)

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `refundService.approveRefund(unknownDisputeId, makeApproveRequest(), ADMIN1)`.
3. Assert: throws `RefundNotFoundException` code `RFND-003`.
4. Repeat with a dispute found but its linked payment missing (`paymentRepository.findByBookingId()` returns empty) — same `RFND-003`.

**Expected Result (PASS):** `404`/`RFND-003` for both missing-dispute and missing-payment cases; no refund row created.
**Expected Result (FAIL):** `NullPointerException` or wrong error code surfaced instead of a controlled 404.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### RFND-TC-INT-001 — E2E: approve-refund API flow (Testcontainers + WireMock)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/consultations/disputes/{id}/refund/approve` → DB persisted `SUCCESS` row + updated payment
**Test File:** `src/test/java/com/carebridge/backend/consultation/RefundApprovalIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-001` dispute (`APPROVED`) + `FX-003` payment inserted via JPA before test
- WireMock VNPay stub (`FX-006`) configured

**Test Steps:**
1. Seed dispute `D1` (`APPROVED`) and payment `P1` (`SUCCESS`, `grossAmount=1250000`).
2. `POST /api/v1/consultations/disputes/{D1}/refund/approve` with JWT for `ADMIN1` and body `{}` (amount omitted).
3. Assert response `200`, body matches §9.2 schema with `status:"SUCCESS"`.
4. Assert DB: `refund_records` has exactly 1 row for `dispute_id=D1` with `status='SUCCESS'`; `payment_transactions.refund_amount = 1250000`, `refunded_at IS NOT NULL`.

**Expected Result (PASS):** API `200`; DB rows consistent across `refund_records` + `payment_transactions`.
**Expected Result (FAIL):** API error, or DB rows missing/duplicated/inconsistent.

**DB Assertion:**
```java
RefundRecordEntity refund = refundRecordRepository.findByDisputeId(disputeId).orElseThrow();
assertThat(refund.getStatus()).isEqualTo("SUCCESS");
assertThat(refund.getApprovedBy()).isEqualTo(adminId);

PaymentTransactionEntity payment = paymentTransactionRepository.findById(paymentId).orElseThrow();
assertThat(payment.getRefundAmount()).isEqualByComparingTo(new BigDecimal("1250000"));
assertThat(payment.getRefundedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RFND-TC-001` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-002` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-003` | `RefundPolicyTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-004` | `RefundPolicyTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-005` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-006` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-007` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-008` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-009` | `RefundControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-010` | `RefundControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-011` | `RefundPolicyTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-012` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-013` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-014` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-015` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-016` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-017` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-018` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-019` | `RefundMapperTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-020` | `RejectRefundRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-021` | `RefundServiceTest.java` | `[ ]` | `[ ]` | |
| `RFND-TC-INT-001` | `RefundApprovalIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class RefundService implements IRefundService {

    @Override
    public RefundPreviewResponse getRefundPreview(UUID disputeId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public RefundDecisionResponse approveRefund(UUID disputeId, ApproveRefundRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public RefundDecisionResponse rejectRefund(UUID disputeId, RejectRefundRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RFND-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-020` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-021` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RFND-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** UC-209 Resolve Consultation Dispute implemented — can set `consultation_disputes.status='APPROVED'` in a test environment
- [ ] **BLOCKING:** Payment capture service populates `payment_transactions` with a real `SUCCESS`-equivalent state + `gateway_transaction_id`
- [ ] `UC210_ApproveOrRejectRefund_TDS.md` reviewed and Approved
- [ ] ADR-REFUND-001/002/003 confirmed by Product/Tech Lead (currently `Proposed`)
- [ ] Reused ADR-DISPUTE-001 (UC78) confirmed (currently `Proposed`)
- [ ] Test fixtures (§3 TDS-05) prepared
- [ ] WireMock VNPay stub server configured (shared pattern with UC78 — first VNPay integration in repo)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers + WireMock)
- [ ] Test coverage ≥ 80% lines cho `RefundService`, `RefundPolicy`
- [ ] Không có business logic trong `RefundController` (chỉ validation + `@PreAuthorize` + mapping)
- [ ] Không có PII/secret (VNPay key) xuất hiện plaintext trong logs
- [ ] `RFND-TC-006`, `RFND-TC-007` (idempotency) and `RFND-TC-013` (UC-209 boundary guard) pass — these are release-blocking financial-safety gates
- [ ] `RFND-TC-003`/`RFND-TC-004` (precondition gate) pass — release-blocking

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors; `VnPayGatewayClient` resolves to the UC78-defined interface (no duplicate/second client type)
- [ ] **Props Isolation** — mỗi test dùng `RefundTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-209 / payment-capture prerequisite services not yet deployed to test environment
- VNPay sandbox credentials/WireMock contract unavailable
- New architecture unknown discovered (e.g., UC-209's actual "recommend-refund" field differs from the `status='APPROVED'` assumption) requiring Tech Lead review

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in scope — see TDS §5.3)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultation/refund/

# No Flyway migration was applied for UC-210 — nothing to revert at the DB level.

# Gap vẫn OPEN → giữ nguyên entry trong task tracker (partial-refund policy, ADR-REFUND-003)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-REFUND-00X / ADR-DISPUTE-00X reused) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping — `RFND-TC-009`/`010`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces; `VnPayGatewayClient` reused, not reinvented) | G-3 |
| **AP-CB-001** *(project-specific, reused from UC78)* | **Approving refund without the manual admin gate** | Any test path where a refund reaches `PROCESSING`/`SUCCESS` without an intervening `APPROVED`-dispute + System-Admin-caller check | `RFND-TC-003`, `RFND-TC-009` explicitly assert the precondition/RBAC gate runs before any VNPay interaction | **Release-blocking** |
| **AP-CB-003** *(project-specific, UC-210-only)* | **UC-210 mutating the dispute outcome (`consultation_disputes.status`)** | Any write interaction with the dispute repository from `approveRefund()`/`rejectRefund()` | `RFND-TC-013` explicitly asserts zero write interactions with `ConsultationDisputeRepository` | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`RFND-TC-003`, `RFND-TC-006`, `RFND-TC-007`, `RFND-TC-009`, `RFND-TC-013`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC-210 v1.0 — Draft. Total test cases: 22 (21 unit/component/security + 1 integration). Critical-severity: 9 (`RFND-TC-001, 002, 003, 005, 006, 007, 009, 010, 013` — financial-safety, idempotency, and RBAC/boundary gates). Error-code coverage is bidirectionally complete against TDS §10 (`RFND-001..006`, `RFND-003` not-found, `VNP-001` reused; `RFND-500` is a generic unexpected-failure catch-all not unit-tested by design). Open item carried from TDS: partial-refund policy (ADR-REFUND-003) — mechanism tested for full refund + bound only; no percentage/threshold policy is asserted anywhere in this spec. Requires Approved status change only by user/Tech Lead.*
