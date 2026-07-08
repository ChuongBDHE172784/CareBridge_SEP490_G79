# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-209 — Resolve Consultation Dispute — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-209`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L969-984, L1874-1875) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.8 (L4494-4513), §3.3.14.9 (L4515-4534, UC-210 boundary)
- `04_Implement/UC209_ResolveConsultationDispute/UC209_ResolveConsultationDispute_TDS.md` — Technical Design Spec (this Test-Spec's basis)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` — authoritative source for `ADR-DISPUTE-001..004` (reused, not redefined)
- `03_Design/UI_UX/WebAppScreen/CB-200 Resolve Consultation Dispute (UC-209)/code.html` — resolution workspace oracle
- `03_Design/UI_UX/WebAppScreen/CB-093 Consultation Disputes (UC-209)/code.html` — dispute list oracle
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC-209 |

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
| **Feature / Gap ID** | `UC-209` |
| **Module** | `Consultation — Resolve Dispute (Admin)` |
| **Spec gốc** | `CB-CONSULTATION-IMP-209` (UC-209 TDS) |
| **Priority** | 🟡 Medium (per SRS L4507) |
| **Sprint** | Consultation batch UC203→UC210 — TV4-Lâm |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC78 dispute submission (produces `OPEN` disputes — BLOCKING, see §6 Entry Criteria), Booking/Payment read models |
| **Downstream Consumers** | **UC-210 Approve or Reject Refund** (consumes `ConsultationDisputeResolved` event / `status=APPROVED & resolution_type=REFUND_RECOMMENDED` as precondition), Notification service |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC-209 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (no refund_records/VNPay from UC-209 — scope boundary), C2 (admin-only RBAC), C3 (terminal-state guard), C4 (data-minimized DTO), C5 (package layout + reuse of UC78 entity/repository) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC78 explicitly left the admin-side resolution endpoint unspecified ("a future admin-side spec") but already defined `ADR-DISPUTE-001..004`; SRS UC-209 text is generic boilerplate and does not itself state the state-machine transitions | UC-209 TDS §3 **reuses** `ADR-DISPUTE-001` (manual refund gate), `ADR-DISPUTE-002` (ownership/RBAC pattern extended to admin-only), `ADR-DISPUTE-003` (VNPay idempotency — not exercised here), `ADR-DISPUTE-004` (`reason_code` taxonomy, read-only in this module) verbatim — they are NOT redefined | Tests cite these ADRs as Oracle Source wherever the underlying dispute lifecycle/ownership concept is exercised, without re-deriving them |
| L2 | SRS is silent on whether "request more info" is a new status, sub-state, or side-channel action | `ADR-RSDP-001` (UC-209 TDS §3) derives from the CB-200 mockup: request-info is a **non-terminal** action that ensures `UNDER_REVIEW` and leaves `resolved_by/resolved_at/resolution_type` NULL; no new DB status added | Tests assert `REQUEST_INFO` leaves resolution fields NULL and only touches `status` (`RSDP-TC-005`) |
| L3 | Ambiguity risk: an implementer could conflate "recommend refund" with actually creating a `refund_records` row / calling VNPay, duplicating UC-210 and breaking `ADR-DISPUTE-001`'s human-gate intent | `ADR-RSDP-004` (UC-209 TDS §3) — confirmed cross-cutting decision: UC-209 sets outcome ONLY (`status`+`resolution_type` on `consultation_disputes`); UC-210 owns `refund_records`/VNPay exclusively | Tests explicitly assert **zero interactions** with `RefundRecordRepository`/`VnPayGatewayClient`/`IRefundService` on every resolution path (`RSDP-TC-001`, `RSDP-TC-002`, `RSDP-TC-014`) — this is the release-blocking scope-boundary gate |
| L4 | SRS says "Reviews **minimum necessary data**" (L4501) but does not enumerate which fields are in/out of scope | `ADR-RSDP-003` (UC-209 TDS §3) enumerates included vs. redacted fields (phone/email/address/health-record content/other bookings excluded; display name + masked ID + adjudication-relevant snapshot included) | Tests assert the admin detail DTO never serializes redacted fields (`RSDP-TC-010`) |
| L5 | No DB CHECK constraint exists on `consultation_disputes.status`/`resolution_type` (verified — same posture as UC78's `reason_code`) | `V1__init_schema.sql` L969-984 confirms free `varchar` columns, no CHECK; taxonomy enforced app-level only per `ADR-RSDP-002` | Tests assert app-level rejection of invalid `action`/`resolution_type` values (`RSDP-TC-001` validation branch, DTO test) |
| L6 | A dispute already in a terminal state (`APPROVED`/`REJECTED`) must never be re-resolved, but neither SRS nor the schema enforces this at the DB layer | `ADR-RSDP-001` state machine (UC-209 TDS §6.7) — terminal states are absorbing; service-layer guard required | Tests assert `RSDP-004` on any resolution attempt against an already-terminal dispute (`RSDP-TC-006`) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Resolve Dispute (Admin) module bao gồm các layer:
├── Domain (DisputeResolutionPolicy — pure logic, no deps)
├── Services (DisputeResolutionService — mock JPA Repository với Mockito)
├── Mapper (DisputeAdminMapper — data-minimization projection, pure logic)
├── Controller (DisputeResolutionController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest — NO WireMock/VNPay,
    since UC-209 never calls the payment gateway — ADR-RSDP-004)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-209 (§3.3.14.8, L4494-4513) | Actor (System Admin only), trigger, "minimum necessary data", exceptions E1-E3 |
| SRS UC-210 (§3.3.14.9, L4515-4534) | Sibling boundary — confirms refund approval is a distinct use case |
| `UC-209 TDS` ADR-RSDP-001..004 | State-machine mapping, `resolution_type` taxonomy, data-minimization, UC-209/UC-210 boundary |
| `UC78 TDS` ADR-DISPUTE-001..004 (reused) | Manual refund gate, ownership/RBAC pattern, VNPay idempotency (not exercised), reason-code taxonomy |
| `V1__init_schema.sql` L969-984, L1874-1875 | Column names/types/defaults/FK as persistence oracle |
| `BR-RBAC` / `BR-PRIVACY` / `BR-CONSULTATION` | Admin-only authorization, data-minimization, auditable lifecycle |
| CB-200 / CB-093 mockups | Resolution action set (request-info/reject/recommend-refund/resolve), mandatory rationale field, list columns/filters |
| `UC-209 TDS §9-10` | API contract, error codes (`RSDP-0xx`) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin resolves dispute to APPROVED with refund recommendation | `DisputeResolutionService.resolveDispute()` | `RSDP-TC-001` |
| TC-COND-002 | Admin resolves dispute to APPROVED without refund (upheld, no refund) | `DisputeResolutionService.resolveDispute()` | `RSDP-TC-002` |
| TC-COND-003 | Admin rejects dispute (REJECTED) | `DisputeResolutionService.resolveDispute()` | `RSDP-TC-003` |
| TC-COND-004 | Admin starts review — OPEN → UNDER_REVIEW | `DisputeResolutionService.startReview()` | `RSDP-TC-004` |
| TC-COND-005 | Request-info side action — non-terminal, resolved_* stay NULL | `DisputeResolutionService.resolveDispute()` (action=REQUEST_INFO) | `RSDP-TC-005` |
| TC-COND-006 | Wrong-state — resolving an already-terminal dispute | `DisputeResolutionPolicy.assertResolvable()` | `RSDP-TC-006` |
| TC-COND-007 | Blank `resolutionNote` rejected | DTO validation / `DisputeResolutionPolicy` | `RSDP-TC-007` |
| TC-COND-008 | Non-admin (Mother/Expert party) denied | `DisputeResolutionPolicy.assertAdmin()` | `RSDP-TC-008` |
| TC-COND-009 | Unauthenticated request | `DisputeResolutionController` security filter | `RSDP-TC-009` |
| TC-COND-010 | Data-minimization — admin DTO never leaks redacted PII | `DisputeAdminMapper.toDetail()` | `RSDP-TC-010` |
| TC-COND-011 | Dispute not found | `DisputeResolutionService` | `RSDP-TC-011` |
| TC-COND-012 | Invalid `action` value rejected | DTO validation (`@Pattern`) | `RSDP-TC-012` |
| TC-COND-013 | Boundary — `resolutionNote` length (0 / 2000 / 2001 chars) | DTO validation (`@Size(max=2000)`) | `RSDP-TC-013` |
| TC-COND-014 | Scope-boundary guard — resolution NEVER creates `refund_records` / NEVER calls VNPay, for ANY action | `DisputeResolutionService.resolveDispute()` (all 5 actions) | `RSDP-TC-014` |
| TC-COND-015 | `ConsultationDisputeResolved` event payload — `refundRecommended=true` for UC-210 consumption (RECOMMEND_REFUND path) | `DisputeResolutionService.resolveDispute()` | `RSDP-TC-015` |
| TC-COND-016 | `ConsultationDisputeResolved` event payload — `refundRecommended=false` (REJECT / RESOLVE_NO_REFUND paths) | `DisputeResolutionService.resolveDispute()` | `RSDP-TC-016` |
| TC-COND-017 | `ConsultationDisputeInfoRequested` / `ConsultationDisputeUnderReview` events emitted correctly for non-terminal actions | `DisputeResolutionService` | `RSDP-TC-017` |
| TC-COND-018 | List endpoint returns only admin-visible, data-minimized rows; filter by status (CB-093) | `DisputeResolutionService.listDisputes()` | `RSDP-TC-018` |
| TC-COND-019 | GET detail is side-effect-free — no status mutation on read | `DisputeResolutionService.getDisputeDetail()` | `RSDP-TC-019` |
| TC-COND-020 | E2E — full resolve-to-APPROVED flow via MockMvc/Testcontainers, DB row updated, no `refund_records` row created | `DisputeResolutionController` + real DB | `RSDP-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `action` valid/invalid classes | 5 valid actions vs. arbitrary invalid strings |
| Boundary Value Analysis | `resolutionNote` length (0, 1, 2000, 2001 chars) | Confirms `@Size(max=2000)` + `@NotBlank` boundary |
| State Transition Testing | Dispute `OPEN → UNDER_REVIEW → {APPROVED\|REJECTED}` (absorbing terminal states) | Validates `ADR-RSDP-001` state machine, absorbing-state guard |
| Decision Table | Actor role (Admin/Mother/Expert/Guest) × dispute current-state combinations | 200/403/404/409 branching |
| Error Guessing | Replay of resolution call on terminal dispute (double-resolve), IDOR via disputeId manipulation | Idempotency/terminal-state attack surface |
| Negative/Interaction Testing | `verifyNoInteractions` on refund/VNPay collaborators across ALL 5 actions | Scope-boundary enforcement (`ADR-RSDP-004`) — the primary financial-safety gate of this module |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-101` | DB seed | `ConsultationDisputeEntity{disputeId=D1, bookingId=B1, submittedBy=U1, status='OPEN', reasonCode='EXPERT_ABSENT'}` | Happy path — dispute available for resolution |
| `FX-102` | DB seed | Same as `FX-101` but `status='UNDER_REVIEW'` | Resolution from `UNDER_REVIEW` |
| `FX-103` | DB seed | `ConsultationDisputeEntity{disputeId=D2, status='APPROVED', resolvedBy=ADM1, resolvedAt=<past>, resolutionType='REFUND_RECOMMENDED'}` | Wrong-state / already-resolved test |
| `FX-104` | JWT | `{sub: 'ADM1', role: 'SYSTEM_ADMIN'}` | Auth context — admin |
| `FX-105` | JWT | `{sub: 'U1', role: 'MOTHER'}` | Auth context — non-admin party (dispute submitter) |
| `FX-106` | JWT | `{sub: 'EX1', role: 'EXPERT'}` | Auth context — non-admin party (expert) |
| `FX-107` | DB seed | `ConsultationBookingEntity{bookingId=B1, modality='ONLINE', scheduledAt=..., status='COMPLETED'}` | Booking snapshot for detail DTO |
| `FX-108` | DB seed | `PaymentTransactionEntity{paymentId=P1, bookingId=B1, grossAmount=500000, status='SUCCESS'}` | Payment snapshot for detail DTO |
| `FX-109` | Mock spies | `RefundRecordRepository` mock, `VnPayGatewayClient` mock, `IRefundService` mock — all asserted via `verifyNoInteractions` | Scope-boundary guard (`RSDP-TC-014`) |
| `FX-110` | DB seed | Party entity with PII fields: `phone='+8490...'`, `email='mai@example.test'`, `address='...'` (SYNTHETIC) | Data-minimization negative assertion source |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `DisputeResolutionPolicy`, `DisputeResolutionService`, `DisputeAdminMapper` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest DisputeResolutionController` | ✅ MockMvc full flow | ✅ RBAC / scope-boundary |
| Web | ✅ `DisputeResolutionWorkspace.tsx` / `DisputeListPage.tsx` unit | — | ✅ component test (button → action mapping per CB-200) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

class DisputeResolutionTestFactory {

    static ConsultationDisputeEntity makeOpenDispute() {
        ConsultationDisputeEntity dispute = new ConsultationDisputeEntity();
        dispute.setDisputeId(UUID.fromString("00000000-0000-0000-0000-0000000000D1"));
        dispute.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        dispute.setSubmittedBy(UUID.fromString("00000000-0000-0000-0000-000000000U01"));
        dispute.setReasonCode("EXPERT_ABSENT");
        dispute.setDescription("Expert did not join within 15 minutes.");
        dispute.setStatus("OPEN");
        dispute.setResolvedBy(null);
        dispute.setResolutionType(null);
        dispute.setResolutionNote(null);
        dispute.setResolvedAt(null);
        return dispute;
    }

    static ConsultationDisputeEntity makeOpenDispute(Consumer<ConsultationDisputeEntity> overrides) {
        ConsultationDisputeEntity dispute = makeOpenDispute();
        overrides.accept(dispute);
        return dispute;
    }

    static ConsultationDisputeEntity makeTerminalDispute(String terminalStatus, String resolutionType) {
        ConsultationDisputeEntity dispute = makeOpenDispute();
        dispute.setStatus(terminalStatus); // "APPROVED" | "REJECTED"
        dispute.setResolutionType(resolutionType);
        dispute.setResolutionNote("Previously resolved.");
        dispute.setResolvedBy(UUID.fromString("00000000-0000-0000-0000-00000000ADM1"));
        dispute.setResolvedAt(Instant.parse("2026-06-01T00:00:00Z"));
        return dispute;
    }

    static ResolveDisputeRequest makeResolveRequest(String action) {
        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAction(action);
        request.setResolutionNote("Rationale: session-log evidence reviewed against CS-04/CS-12.");
        return request;
    }

    static ResolveDisputeRequest makeResolveRequest(String action, Consumer<ResolveDisputeRequest> overrides) {
        ResolveDisputeRequest request = makeResolveRequest(action);
        overrides.accept(request);
        return request;
    }

    static ConsultationBookingEntity makeBookingSnapshot() {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        booking.setModality("ONLINE");
        booking.setStatus("COMPLETED");
        return booking;
    }

    static PaymentTransactionEntity makePaymentSnapshot() {
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setPaymentId(UUID.fromString("00000000-0000-0000-0000-0000000000P1"));
        payment.setBookingId(makeBookingSnapshot().getBookingId());
        payment.setGrossAmount(new BigDecimal("500000"));
        payment.setStatus("SUCCESS");
        return payment;
    }

    // Full-PII party fixture — used ONLY to assert redaction (never expected in output)
    static PartyRawRecord makeRawPartyWithPII() {
        PartyRawRecord party = new PartyRawRecord();
        party.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000U01"));
        party.setDisplayName("Nguyễn Thị Mai");
        party.setPhone("+84901234567");        // SYNTHETIC — must be redacted
        party.setEmail("mai.test@example.test"); // SYNTHETIC — must be redacted
        party.setAddress("123 Test Street, Hanoi"); // SYNTHETIC — must be redacted
        return party;
    }
}
```

---

### RSDP-TC-001 — Happy path: resolve to APPROVED with refund recommendation

**Severity:** `CRITICAL`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-209 TDS §6.1` sequence diagram + `ADR-RSDP-002` taxonomy table + CB-200 "Đề xuất Hoàn tiền" button

**Preconditions:**
- `FX-101` dispute seeded (`status='OPEN'`)
- `FX-104` JWT for admin `ADM1`

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D1)` returns `FX-101`; mock `disputeRepository.save(...)` returns the saved entity.
2. Act: `disputeResolutionService.resolveDispute(D1, makeResolveRequest("RECOMMEND_REFUND"), ADM1)`.
3. Assert: returned DTO `status == "APPROVED"`, `resolutionType == "REFUND_RECOMMENDED"`; saved entity has `resolvedBy=ADM1`, `resolvedAt != null`, `resolutionNote` equals request note.

**Expected Result (PASS):** `200`-equivalent response; `status=APPROVED`; `resolutionType=REFUND_RECOMMENDED`; all resolution fields populated in one `save()` call.
**Expected Result (FAIL):** Wrong terminal state, missing `resolvedBy/resolvedAt`, or resolution split across multiple non-atomic saves.

**Current Status:** 🔴 Not written
**Implementation Note:** Must NOT touch `RefundRecordRepository`/`VnPayGatewayClient` — see `RSDP-TC-014` for the explicit negative assertion (kept separate for single-responsibility test clarity, but reviewers should treat both as covering the same invariant).

---

### RSDP-TC-002 — Happy path: resolve to APPROVED, no refund (upheld without refund)

**Severity:** `HIGH`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-RSDP-002` taxonomy table (`RESOLVE_NO_REFUND` → `RESOLVED_NO_REFUND`) + CB-200 "Giải quyết" button

**Preconditions:** `FX-102` dispute (`status='UNDER_REVIEW'`).

**Test Steps:**
1. Act: `disputeResolutionService.resolveDispute(D1, makeResolveRequest("RESOLVE_NO_REFUND"), ADM1)`.
2. Assert: `status == "APPROVED"`, `resolutionType == "RESOLVED_NO_REFUND"`.

**Expected Result (PASS):** Dispute approved without a refund recommendation flag.
**Expected Result (FAIL):** `resolutionType` incorrectly set to `REFUND_RECOMMENDED`, or refund-related side effect occurs.

**Current Status:** 🔴 Not written

---

### RSDP-TC-003 — Happy path: reject dispute

**Severity:** `HIGH`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC-209 TDS §6.2` sequence diagram, CB-200 "Từ chối" button

**Preconditions:** `FX-101` dispute (`status='OPEN'`).

**Test Steps:**
1. Act: `disputeResolutionService.resolveDispute(D1, makeResolveRequest("REJECT"), ADM1)`.
2. Assert: `status == "REJECTED"`, `resolutionType == "REJECTED"`, `resolvedBy/resolvedAt` populated.

**Expected Result (PASS):** Dispute terminal at `REJECTED`; no refund artifact created.
**Expected Result (FAIL):** Wrong status, or a `refund_records`-related call occurs.

**Current Status:** 🔴 Not written

---

### RSDP-TC-004 — Start review: OPEN → UNDER_REVIEW

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputeResolutionService.startReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-RSDP-001` — "Đưa ra Quyết định" header action in CB-200

**Test Steps:**
1. Arrange: `FX-101` dispute (`status='OPEN'`).
2. Act: `disputeResolutionService.startReview(D1, ADM1)`.
3. Assert: `status == "UNDER_REVIEW"`; `resolvedBy/resolutionType/resolvedAt` remain `null`.
4. Act again (idempotency): `startReview(D1, ADM1)` on an already-`UNDER_REVIEW` dispute.
5. Assert: no exception; `status` stays `"UNDER_REVIEW"`.

**Expected Result (PASS):** Non-terminal transition; idempotent re-entry into `UNDER_REVIEW`.
**Expected Result (FAIL):** Exception on repeated call, or resolution fields incorrectly populated.

**Current Status:** 🔴 Not written

---

### RSDP-TC-005 — Side action: request more info (non-terminal)

**Severity:** `CRITICAL`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()` (action=`REQUEST_INFO`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC-209 TDS ADR-RSDP-001` (Option B — non-terminal action), `UC-209 TDS §6.3` sequence diagram, `UC-209 TDS §2 Logic Issues L2`

**Preconditions:** `FX-101` dispute (`status='OPEN'`).

**Test Steps:**
1. Act: `disputeResolutionService.resolveDispute(D1, makeResolveRequest("REQUEST_INFO"), ADM1)`.
2. Assert: returned/saved entity has `status == "UNDER_REVIEW"`; `resolvedBy == null`; `resolutionType == null`; `resolvedAt == null`.
3. Assert: `ConsultationDisputeInfoRequested` event published (captured via `ArgumentCaptor`).

**Expected Result (PASS):** Status advances to `UNDER_REVIEW` only; all terminal-resolution fields stay `NULL`; correct event emitted.
**Expected Result (FAIL):** `resolvedBy`/`resolutionType`/`resolvedAt` populated (incorrectly treating this as terminal), or dispute set to a status outside the 4-state enum.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense for `ADR-RSDP-001`'s non-terminal-action decision — must never regress into treating `REQUEST_INFO` as a terminal outcome.

---

### RSDP-TC-006 — Wrong-state: resolving an already-terminal dispute → 409 (`RSDP-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Feature Under Test:** `DisputeResolutionPolicy.assertResolvable()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/DisputeResolutionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC-209 TDS ADR-RSDP-001` state machine (§6.7), `UC-209 TDS §2 Logic Issues L6`, `UC-209 TDS §6.4` error-path sequence diagram

**Preconditions:** `FX-103` — dispute already `APPROVED`.

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(D2)` returns `FX-103` (`status='APPROVED'`).
2. Act: `disputeResolutionService.resolveDispute(D2, makeResolveRequest("REJECT"), ADM1)`.
3. Assert: throws `DisputeAlreadyResolvedException` code `RSDP-004`; `disputeRepository.save()` is **never** called (`verify(disputeRepository, never()).save(any())`).

**Expected Result (PASS):** Exception thrown; DB state unchanged; no second resolution recorded.
**Expected Result (FAIL):** Dispute silently re-resolved, overwriting the original `resolvedBy`/`resolutionType`/`resolvedAt`.

**Current Status:** 🔴 Not written

---

### RSDP-TC-007 — Blank `resolutionNote` rejected → 400 (`RSDP-006`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ResolveDisputeRequest` DTO validation (`@NotBlank`) / `DisputeResolutionPolicy`
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/ResolveDisputeRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** CB-200 sticky bar label "Lý do xử lý (**Bắt buộc**)"; `UC-209 TDS §8.1` DTO annotation; `UC-209 TDS §10` error table (`RSDP-006`)

**Test Steps:**
1. Act: validate `makeResolveRequest("REJECT", r -> r.setResolutionNote(""))`.
2. Assert: violation on `resolutionNote` (`@NotBlank`); if bypassed to service layer, `DisputeResolutionPolicy` throws `RSDP-006`.

**Expected Result (PASS):** Blank rationale rejected before any state mutation.
**Expected Result (FAIL):** Resolution proceeds with an empty rationale.

**Current Status:** 🔴 Not written

---

### RSDP-TC-008 — Non-admin (Mother/Expert party) denied → 403 (`RSDP-002`)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `DisputeResolutionPolicy.assertAdmin()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/DisputeResolutionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC-209 TDS ADR-DISPUTE-002 (reused)`, `BR-RBAC`, SRS UC-209 "Primary Actor: System Admin" (L4499), `UC-209 TDS §6.5` error-path sequence diagram

**Preconditions:** `FX-105` (Mother, the dispute's own submitter) and `FX-106` (Expert party).

**Test Steps:**
1. Act: `disputeResolutionPolicy.assertAdmin("MOTHER")`.
2. Assert: throws `DisputeAccessDeniedException` code `RSDP-002`.
3. Repeat for `"EXPERT"` role.
4. Assert: same exception/code.

**Expected Result (PASS):** Both party roles rejected; no dispute state read/written for either.
**Expected Result (FAIL):** A party role is allowed to resolve its own dispute (self-adjudication — critical RBAC bypass).

**Current Status:** 🔴 Not written
**Implementation Note:** Explicitly test that even the dispute's own submitter (`U1`, owner of the underlying booking) is denied — resolution authority is role-based, not ownership-based, the inverse of UC78's `ADR-DISPUTE-002`.

---

### RSDP-TC-009 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `DisputeResolutionController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/DisputeResolutionControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC-209 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `PATCH /api/v1/admin/consultations/disputes/{id}/resolution` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401`; no dispute mutated.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### RSDP-TC-010 — Data-minimization: admin detail DTO never leaks redacted PII

**Severity:** `CRITICAL`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Legal:** `PDPA (Luật 91/2025)` — minimum-necessary access
**Feature Under Test:** `DisputeAdminMapper.toDetail()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/DisputeAdminMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** SRS UC-209 "Reviews **minimum necessary data**" (L4501); `UC-209 TDS ADR-RSDP-003`; `BR-PRIVACY`

**Preconditions:** `FX-110` raw party record with phone/email/address (SYNTHETIC).

**Test Steps:**
1. Act: `disputeAdminMapper.toDetail(makeOpenDispute(), makeBookingSnapshot(), makePaymentSnapshot(), makeRawPartyWithPII(), makeRawPartyWithPII())`.
2. Assert (JSON serialization of the resulting `DisputeAdminDetailResponse`): contains `displayName` and a masked ID for each party; does **not** contain `phone`, `email`, or `address` as literal substrings anywhere in the serialized payload.
3. Assert: `PaymentSnapshot.maskedTxnRef` does not equal the raw gateway transaction ID verbatim (masked).

**Expected Result (PASS):** No redacted field appears in the response; only adjudication-relevant fields present (matches TDS §8.1 `DisputeAdminDetailResponse` field list exactly).
**Expected Result (FAIL):** Any redacted PII field serialized, or raw entity returned directly.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense against `AP-CB-004` (PII over-exposure) flagged in TDS §17.4.

---

### RSDP-TC-011 — Dispute not found → 404 (`RSDP-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()` / `getDisputeDetail()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC-209 TDS §10` error table (`RSDP-003`; cross-ref UC78 `DISP-003`)

**Test Steps:**
1. Arrange: mock `disputeRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `disputeResolutionService.resolveDispute(randomId, makeResolveRequest("REJECT"), ADM1)`.
3. Assert: throws `DisputeNotFoundException` code `RSDP-003`.

**Current Status:** 🔴 Not written

---

### RSDP-TC-012 — Invalid `action` value rejected → 400 (`RSDP-001`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ResolveDisputeRequest` DTO validation (`@Pattern`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/ResolveDisputeRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC-209 TDS §8.1` DTO annotation (`@Pattern(regexp = "START_REVIEW|REQUEST_INFO|REJECT|RESOLVE_NO_REFUND|RECOMMEND_REFUND")`)

**Test Steps:**
1. Act: validate `makeResolveRequest("APPROVE_EVERYTHING")` (not in the allowed set).
2. Assert: violation on `action` field; error code `RSDP-001`.

**Expected Result (PASS):** Arbitrary/typo action values rejected before reaching the service layer.
**Expected Result (FAIL):** Unrecognized action silently accepted or causes an uncaught exception instead of a controlled 400.

**Current Status:** 🔴 Not written

---

### RSDP-TC-013 — Boundary: `resolutionNote` length validation (0 / 2000 / 2001 chars)

**Severity:** `MEDIUM`
**Feature Under Test:** `ResolveDisputeRequest` DTO validation (`@NotBlank @Size(max=2000)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/ResolveDisputeRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC-209 TDS §8.1` DTO annotation

**Test Steps:**
1. Act (blank): validate `makeResolveRequest("REJECT", r -> r.setResolutionNote(""))`.
2. Assert: violation (`@NotBlank`).
3. Act (2000 chars): validate with `resolutionNote = "a".repeat(2000)`.
4. Assert: no violation.
5. Act (2001 chars): validate with `resolutionNote = "a".repeat(2001)`.
6. Assert: violation (`@Size(max=2000)`).

**Expected Result (PASS):** Boundary respected exactly at 2000/2001.
**Expected Result (FAIL):** Off-by-one on max length, or blank accepted.

**Current Status:** 🔴 Not written

---

### RSDP-TC-014 — Scope-boundary guard: resolution NEVER creates `refund_records` or calls VNPay, for ANY action

**Severity:** `CRITICAL`
**Legal:** `ADR-RSDP-004` (financial-safety scope boundary), `ADR-DISPUTE-001` (reused human-gate ADR)
**Feature Under Test:** `DisputeResolutionService.resolveDispute()` — all 5 actions
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `UC-209 TDS ADR-RSDP-004` — "UC-209 sets the dispute OUTCOME only... never creates refund_records rows... that is UC-210's exclusive responsibility"

**Preconditions:** `FX-109` — `RefundRecordRepository`, `VnPayGatewayClient`, `IRefundService` injected as Mockito mocks with **zero** stubbing.

**Test Steps:**
1. For each action in `{START_REVIEW, REQUEST_INFO, REJECT, RESOLVE_NO_REFUND, RECOMMEND_REFUND}` (parameterized test):
   a. Arrange: fresh `FX-101`/`FX-102` dispute per case (Props Isolation).
   b. Act: invoke the corresponding service method with that action.
   c. Assert: `verifyNoInteractions(refundRecordRepository)`, `verifyNoInteractions(vnPayGatewayClient)`, `verifyNoInteractions(refundService)`.

**Expected Result (PASS):** Zero interactions with any refund/VNPay collaborator across all 5 actions, including `RECOMMEND_REFUND` (the one most likely to be miscoded as "create the refund now").
**Expected Result (FAIL):** Any refund-related repository/client/service call occurs — indicates `AP-CB-003` (scope-boundary breach), a release-blocking defect that would duplicate/bypass UC-210 and violate `ADR-DISPUTE-001`'s manual-approval gate.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important test in this module — it is the automated proof of the UC-209/UC-210 boundary confirmed in TDS §1.2/ADR-RSDP-004. Must be release-blocking (see §6 Exit Criteria).

---

### RSDP-TC-015 — `ConsultationDisputeResolved` event payload: `refundRecommended=true` (RECOMMEND_REFUND path)

**Severity:** `HIGH`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC-209 TDS §7.3` `ConsultationDisputeResolved` payload schema — "the UC-210 trigger"

**Test Steps:**
1. Act: `disputeResolutionService.resolveDispute(D1, makeResolveRequest("RECOMMEND_REFUND"), ADM1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `ConsultationDisputeResolved` with `payload.outcome == "APPROVED"`, `payload.resolutionType == "REFUND_RECOMMENDED"`, `payload.refundRecommended == true`, `payload.disputeId == D1`, `payload.resolvedBy == ADM1`.

**Expected Result (PASS):** Event payload carries the exact boolean flag UC-210 is specified to check as its precondition.
**Expected Result (FAIL):** `refundRecommended` missing, `false`, or payload lacks `disputeId`/`paymentId` needed for UC-210 lookup.

**Current Status:** 🔴 Not written
**Implementation Note:** This test is the contract-level guarantee that UC-210 can be implemented against a stable trigger without re-reading UC-209 internals.

---

### RSDP-TC-016 — `ConsultationDisputeResolved` event payload: `refundRecommended=false` (REJECT / RESOLVE_NO_REFUND paths)

**Severity:** `HIGH`
**Feature Under Test:** `DisputeResolutionService.resolveDispute()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `UC-209 TDS §7.3` payload schema

**Test Steps:**
1. Act (a): `resolveDispute(D1, makeResolveRequest("REJECT"), ADM1)`. Assert captured event `payload.refundRecommended == false`, `payload.outcome == "REJECTED"`.
2. Act (b), fresh dispute: `resolveDispute(D1b, makeResolveRequest("RESOLVE_NO_REFUND"), ADM1)`. Assert captured event `payload.refundRecommended == false`, `payload.outcome == "APPROVED"`, `payload.resolutionType == "RESOLVED_NO_REFUND"`.

**Expected Result (PASS):** UC-210 correctly receives `refundRecommended=false` and will not act on these disputes.
**Expected Result (FAIL):** Flag incorrectly `true` for a non-refund outcome — would cause UC-210 to erroneously process a refund.

**Current Status:** 🔴 Not written

---

### RSDP-TC-017 — Non-terminal events: `ConsultationDisputeUnderReview` / `ConsultationDisputeInfoRequested`

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputeResolutionService.startReview()`, `resolveDispute()` (REQUEST_INFO)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `UC-209 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `startReview(D1, ADM1)`. Assert `ConsultationDisputeUnderReview` published with `payload.disputeId == D1`, `payload.reviewedBy == ADM1`.
2. Act: `resolveDispute(D1, makeResolveRequest("REQUEST_INFO"), ADM1)`. Assert `ConsultationDisputeInfoRequested` published with `payload.note` equal to the request's `resolutionNote`.
3. Assert: neither call publishes `ConsultationDisputeResolved` (that event is terminal-only).

**Current Status:** 🔴 Not written

---

### RSDP-TC-018 — List endpoint: data-minimized rows, filterable by status (CB-093)

**Severity:** `MEDIUM`
**Feature Under Test:** `DisputeResolutionService.listDisputes()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** CB-093 mockup columns (case/user, expert, category, status, refund, action) + `UC-209 TDS §9.2` list response example

**Test Steps:**
1. Arrange: mock `disputeRepository.findByStatusIn(List.of("OPEN","UNDER_REVIEW"), pageable)` returns a page containing `FX-101`.
2. Act: `disputeResolutionService.listDisputes(new DisputeAdminFilter(statuses=["OPEN","UNDER_REVIEW"]), ADM1)`.
3. Assert: returned `DisputeAdminListItemResponse` items expose `caseRef`, `clientDisplayName`, `expertDisplayName`, `category`, `status`, `refundOutcome` — and no raw PII (phone/email) fields.

**Expected Result (PASS):** List matches CB-093 columns; only minimized fields present.
**Expected Result (FAIL):** Raw entity fields leaked, or filter ignored (returns terminal disputes when not requested).

**Current Status:** 🔴 Not written

---

### RSDP-TC-019 — GET detail is side-effect-free (no status mutation on read)

**Severity:** `HIGH`
**Feature Under Test:** `DisputeResolutionService.getDisputeDetail()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/DisputeResolutionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `UC-209 TDS ADR-RSDP-001` Option B rationale — "keeps GET idempotent/safe"; `UC-209 TDS §6.6` sequence diagram

**Test Steps:**
1. Arrange: `FX-101` dispute (`status='OPEN'`).
2. Act: `disputeResolutionService.getDisputeDetail(D1, ADM1)` (call twice).
3. Assert: `disputeRepository.save()` is **never** invoked (`verify(disputeRepository, never()).save(any())`); dispute `status` remains `"OPEN"` after both calls.

**Expected Result (PASS):** Repeated reads never mutate state — REST-safe.
**Expected Result (FAIL):** First `GET` silently advances status to `UNDER_REVIEW` (violates ADR-RSDP-001 Option B, reintroduces rejected Option C).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### RSDP-TC-INT-001 — E2E: resolve-to-APPROVED flow (Testcontainers), no `refund_records` row created

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `PATCH /api/v1/admin/consultations/disputes/{id}/resolution` → DB row updated
**Test File:** `src/test/java/com/carebridge/backend/consultation/DisputeResolutionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically (no new migration expected — verify none was added)
- Seed: `FX-101` dispute row inserted via JPA before test; NO WireMock/VNPay stub configured (must not be needed — `ADR-RSDP-004`)

**Test Steps:**
1. Seed dispute `D1` (`status='OPEN'`) via repository.
2. `PATCH /api/v1/admin/consultations/disputes/{D1}/resolution` with JWT for `ADM1` and body `{action: "RECOMMEND_REFUND", resolutionNote: "..."}`.
3. Assert response `200`, body matches §9.2 schema (`status="APPROVED"`, `resolutionType="REFUND_RECOMMENDED"`).
4. Assert DB: `SELECT * FROM consultation_disputes WHERE dispute_id = 'D1'` shows `status='APPROVED'`, `resolution_type='REFUND_RECOMMENDED'`, `resolved_by`/`resolved_at` populated.
5. Assert DB: `SELECT COUNT(*) FROM refund_records WHERE dispute_id = 'D1'` returns `0`.

**Expected Result (PASS):**
- API `200`; DB dispute row updated correctly; **zero** `refund_records` rows exist for this dispute after UC-209 alone runs.

**Expected Result (FAIL):**
- API error, DB row not updated, or (critical) a `refund_records` row was created — scope-boundary breach.

**DB Assertion:**
```java
ConsultationDisputeEntity record = disputeRepository.findById(disputeId).orElseThrow();
assertThat(record.getStatus()).isEqualTo("APPROVED");
assertThat(record.getResolutionType()).isEqualTo("REFUND_RECOMMENDED");
assertThat(record.getResolvedBy()).isEqualTo(adminUserId);
assertThat(record.getResolvedAt()).isNotNull();

long refundRowCount = refundRecordRepository.findByDisputeId(disputeId).stream().count();
assertThat(refundRowCount).isZero(); // ADR-RSDP-004 — UC-209 never creates refund_records
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RSDP-TC-001` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-002` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-003` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-004` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-005` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-006` | `DisputeResolutionPolicyTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-007` | `ResolveDisputeRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-008` | `DisputeResolutionPolicyTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-009` | `DisputeResolutionControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-010` | `DisputeAdminMapperTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-011` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-012` | `ResolveDisputeRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-013` | `ResolveDisputeRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-014` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-015` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-016` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-017` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-018` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-019` | `DisputeResolutionServiceTest.java` | `[ ]` | `[ ]` | |
| `RSDP-TC-INT-001` | `DisputeResolutionIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DisputeResolutionService implements IDisputeResolutionService {

    @Override
    public Page<DisputeAdminListItemResponse> listDisputes(DisputeAdminFilter filter, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public DisputeAdminDetailResponse getDisputeDetail(UUID disputeId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public DisputeAdminDetailResponse startReview(UUID disputeId, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public DisputeAdminDetailResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RSDP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RSDP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** UC78 dispute-submission code implemented so `consultation_disputes` rows in `OPEN` actually exist (TDS §1.3 Open item)
- [ ] **BLOCKING:** Booking/Payment read models available for the admin detail snapshot (reuse UC202 conventions)
- [ ] `UC209_ResolveConsultationDispute_TDS.md` reviewed and Approved
- [ ] `ADR-RSDP-001`, `ADR-RSDP-002` confirmed by Product/Tech Lead (currently `Proposed`)
- [ ] Reused `ADR-DISPUTE-001`, `ADR-DISPUTE-004` confirmed by Product/Tech Lead (still `Proposed` in UC78 — inherited dependency)
- [ ] Test fixtures (§3 TDS-05) prepared
- [ ] DPO review scheduled for admin exposure of both-parties' dispute data (`ADR-RSDP-003`)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers; **no WireMock/VNPay needed** — confirms `ADR-RSDP-004`)
- [ ] Test coverage ≥ 80% lines cho `DisputeResolutionService`, `DisputeResolutionPolicy`, `DisputeAdminMapper`
- [ ] Không có business logic trong `DisputeResolutionController`
- [ ] Không có PII vượt "minimum necessary" xuất hiện plaintext trong logs hoặc response
- [ ] `RSDP-TC-014` (scope-boundary guard, all 5 actions) and `RSDP-TC-010` (data-minimization) **PASS** — these are release-blocking gates
- [ ] `RSDP-TC-015`/`RSDP-TC-016` (event payload correctness) PASS — required before UC-210 implementation can begin safely

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — mỗi test dùng `DisputeResolutionTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- UC78 dispute-submission not yet deployed to a test environment (no `OPEN` disputes to seed against)
- Booking/Payment read model contracts change in a way that breaks the admin detail snapshot shape
- Product/Tech Lead has not confirmed `ADR-RSDP-001`/`ADR-RSDP-002` (or the inherited `ADR-DISPUTE-001`/`ADR-DISPUTE-004`) — architectural review required before continuing
- New requirement surfaces requiring `MODERATOR` access to resolution endpoints (currently `Open` per TDS §16) — needs Tech Lead review before test/auth-matrix rewrite

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in scope — TDS §5.3 confirms no schema change)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultation/disputes/

# Disputes already resolved via this feature remain resolved (append-of-status; no data
# rollback is performed — resolution decisions are an audit trail, not reversible by rollback).
# If a scope-boundary breach (AP-CB-003) created refund_records during a bad deploy,
# escalate to Finance/Tech Lead per TDS §12.3 — do NOT silently delete refund_records rows.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-RSDP-00X or reused ADR-DISPUTE-00X) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security, e.g. `RSDP-TC-009`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types match TDS §8 interfaces) | G-3 |
| **AP-CB-003** *(project-specific)* | **Scope-boundary breach — UC-209 writes `refund_records` or calls VNPay/`IRefundService`** | Any test path where `resolveDispute()`/`startReview()` results in a call to `RefundRecordRepository`, `VnPayGatewayClient`, or `IRefundService` | `RSDP-TC-014` explicitly asserts `verifyNoInteractions` across all 5 actions; `RSDP-TC-INT-001` asserts zero `refund_records` rows after E2E resolution | **Release-blocking** |
| **AP-CB-004** *(project-specific)* | **PII over-exposure — admin DTO leaks non-adjudication PII** | Admin detail/list response contains phone/email/address/health-record content/other bookings | `RSDP-TC-010` (detail), `RSDP-TC-018` (list) assert absence of redacted fields in serialized output | **Release-blocking** |
| **AP-CB-005** *(project-specific, cross-reference to `AP-CB-001`/UC78)* | **Auto-approving refund without admin gate — inherited concern from `ADR-DISPUTE-001`** | N/A directly for UC-209 (UC-209 never calls VNPay at all — stronger guarantee than UC78's guard); flagged for consistency since both specs guard the same financial-safety invariant from opposite sides of the workflow | Structurally impossible in UC-209 by design (no VNPay client wired) — verified by `RSDP-TC-014` | Non-blocking (structural, not behavioral, guarantee) |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`RSDP-TC-010`, `RSDP-TC-014`, `RSDP-TC-015`, `RSDP-TC-016`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC-209 v1.0 — Draft. Total test cases: 20 (19 unit/component + 1 integration).
Critical-severity: 7 (`RSDP-TC-001, 005, 006, 008, 009, 010, 014` — state-machine,
RBAC, data-minimization, and scope-boundary gates). Requires Approved status
change only by user/Tech Lead. Reuses `ADR-DISPUTE-001..004` from UC78 verbatim;
introduces `ADR-RSDP-001..004` for the resolution-specific decisions. The
UC-209/UC-210 boundary (no `refund_records`/VNPay from this module) is
explicitly and redundantly tested (`RSDP-TC-014`, `RSDP-TC-INT-001`) because it
is the single highest-risk regression this module could introduce.*
