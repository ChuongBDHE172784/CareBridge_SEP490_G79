# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-204 — Reschedule Consultation

**Document ID:** `CB-CONSULTATION-TDD-204`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent (Test Designer)`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (L817-826 `expert_availability`, L876-909 `consultation_bookings`/`consultation_sessions` split, L1527-1528 session UNIQUE FK, L1635-1639 indices, L1823-1836 FKs)
- `04_Implement/UC204_RescheduleConsultation/UC204_RescheduleConsultation_TDS.md` (`CB-CONSULTATION-IMP-204`) — Technical Specification for this feature
- `02_Requirements/SRS/3_Functional_Specification.md` L4389-4408 (Table 226, UC-204) — Functional requirements
- `03_Design/UI_UX/WebAppScreen/CB-187 Reschedule Consultation (UC-204)/code.html` — UI oracle (deadline/limit/confirmation policy text)
- `03_Design/UI_UX/MobileAppScreen/CB-187 …/code.html`, `CB-183 …/code.html` — UI oracle (mobile propose/reason field)
- `CLAUDE.md` — CareBridge architecture/delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo Test-Spec cho UC-204 Reschedule Consultation, tương ứng với TDS `CB-CONSULTATION-IMP-204`. |

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
| **Feature / Gap ID** | `UC-204` |
| **Module** | `Reschedule — consultation` (package `com.carebridge.backend.consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-204` (`UC204_RescheduleConsultation_TDS.md`) |
| **Priority** | 🟠 P1 *(SRS Table 226 Priority=Medium, Frequency=Regular; treated P1 since it mutates a live booking's schedule)* |
| **Sprint** | `S[N] (TBD)` — pending sprint assignment |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` (booking schedule + participant linkage) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `consultation_bookings`, `consultation_sessions`, `expert_availability` (schema already applied); UC-75 booking-lifecycle status literals (`Open` — see §2) |
| **Downstream Consumers** | Notification service, UC-205 Cancel Consultation (reject→cancel handoff, out of scope), UC-202/UC-203 read models |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONSULTATION-IMP-204 §17` (constraints C1-C6), `ADR-RESCH-001` through `ADR-RESCH-008` |
| **Constraints Injected** | C1 (real split schema only), C2 (two-step propose→confirm), C3 (ownership + session boundary), C4 (duration/price immutability, no VNPay), C5 (package layout, DTO-only responses), C6 (Open seams must not be hard-coded) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` and approved migrations are the final persistence oracle; ERD/sibling drafts are only supporting evidence.
> **Bắt buộc điền trước khi viết test.** Test cases encode the **corrected** behavior below, not the sibling draft's invented model.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Sibling **Draft** TDS `UC75_BookPrivateConsultation` models a unified `consultations` table + `consultation_status` enum (`PENDING_PAYMENT → CONFIRMED → IN_SESSION → COMPLETED`, alt `CANCELLED/NO_SHOW/DISPUTED`) | `V1__init_schema.sql` L876-909 **splits** the domain into `consultation_bookings` (own `status`) and `consultation_sessions` (separate `session_status`); UC75 is an **unapproved Draft**, applied schema wins per CareBridge rule "current code and migrations override historical design notes" (ADR-RESCH-006) | All test fixtures/assertions target `ConsultationBookingEntity.status` and `ConsultationSessionEntity.sessionStatus` as two independent fields — no test may reference a unified `Consultation`/`consultation_status` type |
| L2 | SRS description only says "before the allowed deadline" — no numeric hour value anywhere | ADR-RESCH-002: deadline threshold is `Open`, config-driven (`carebridge.consultation.reschedule.deadline-hours`), no authoritative number exists | Deadline boundary tests (`RESCH-TC-006`/`007`) inject the threshold as a **test parameter** (e.g., `deadlineHours = 24` used ONLY as a synthetic test fixture value, explicitly NOT asserted as the real business rule) — assertions are on the **relational** boundary (`now` vs `scheduled_start − deadlineHours`), not on a specific hour count |
| L3 | CB-187 policy text says "Chỉ được đổi tối đa 1 lần miễn phí" but `consultation_bookings` has no `reschedule_count` column (ADR-RESCH-003, genuine schema gap) | Counting mechanism is `Open` (column vs event-derived) — no migration created in the TDS | `RESCH-TC-013` (limit-reached) is written against the `ReschedulePolicy.assertWithinRescheduleLimit(bookingId)` seam using an **injected mock counter** (test double), marked `🟡 Pending mechanism sign-off` in the RGR tracker — NOT a hard DB assertion until ADR-RESCH-003 is resolved |
| L4 | `consultation_bookings.status` beyond default `PENDING_PAYMENT` is undocumented in any source (ADR-RESCH-005) | No confirmed enum exists; `RESCHEDULE_PROPOSED` and the "active pre-session" literal are AI-proposed, `Open` | Tests use symbolic constants `ACTIVE_PRE_SESSION_STATUS` / `RESCHEDULE_PROPOSED_STATUS` from the test factory (never a bare string literal scattered across tests) so a later literal change is a one-line fixture edit, not a test rewrite |
| L5 | SRS Table 226 lists no Secondary Actor and is silent on money movement | CB-187 confirms reschedule is free (1st time) and reject→refund is a **cancellation** outcome, not part of reschedule itself (ADR-RESCH-008) | Tests assert `price_snapshot_amount`/`commission_rate_snapshot`/`price_locked_at` are **unchanged** after both propose and respond; no VNPay client interaction is verified (mock `VnPayGatewayClient` must never be invoked — verified via `verifyNoInteractions`) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Reschedule module bao gồm các layer:
├── Domain (ReschedulePolicy — pure logic, no deps, mock repos with Mockito)
├── Service (RescheduleService — mock Repository/Policy/EventPublisher với Mockito)
├── Controller (RescheduleController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full propose→respond flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-204 (Table 226) | Two-actor propose/confirm behavior, deadline rule, exception E1/E2 handling |
| `ADR-RESCH-001` | Two-step propose→confirm; slot unchanged until ACCEPT |
| `ADR-RESCH-002` | Deadline enforcement (parameterized boundary) |
| `ADR-RESCH-003` | Reschedule-count limit (pending mechanism sign-off) |
| `ADR-RESCH-004` | Ownership + reschedulable-state precondition (session boundary) |
| `ADR-RESCH-005` | Status literal handling via symbolic constants |
| `ADR-RESCH-006` | Split-schema modeling (booking vs session tables) |
| `ADR-RESCH-007` | Slot-conflict validation (availability window + overlap) |
| `ADR-RESCH-008` | No VNPay call, price/commission snapshot immutability |
| `CB-CONSULTATION-IMP-204` §8-§10 | Interface contracts, error codes (`RESCH-001..008`, `RESCH-500`) |
| BR-RBAC / BR-CONSULTATION / PDPA | Authorization scope, auditable lifecycle, minimum-necessary access |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — Mother proposes, Expert accepts | `RescheduleService.proposeReschedule/respondToReschedule` | `RESCH-TC-001`, `RESCH-TC-002` |
| TC-COND-002 | Happy path — Expert proposes, Mother accepts | `RescheduleService` | `RESCH-TC-003` |
| TC-COND-003 | Reject path — counterparty rejects, slot unchanged | `RescheduleService.respondToReschedule` | `RESCH-TC-004` |
| TC-COND-004 | Deadline exceeded → `RESCH-005`, no mutation | `ReschedulePolicy.assertBeforeDeadline` | `RESCH-TC-005`, `RESCH-TC-006` |
| TC-COND-005 | Slot conflict (no AVAILABLE window) → `RESCH-002` | `ReschedulePolicy.assertNoSlotConflict` | `RESCH-TC-007` |
| TC-COND-006 | Slot conflict (overlaps another active booking) → `RESCH-002` | `ReschedulePolicy.assertNoSlotConflict` | `RESCH-TC-008` |
| TC-COND-007 | Ownership denied — non-participant → `RESCH-004` | `ReschedulePolicy.assertCanReschedule` | `RESCH-TC-009` |
| TC-COND-008 | Booking not found → `RESCH-003` | `RescheduleService` | `RESCH-TC-010` |
| TC-COND-009 | Session already started → `RESCH-007` | `ReschedulePolicy.assertBookingReschedulable` | `RESCH-TC-011` |
| TC-COND-010 | Booking already CANCELLED/COMPLETED → `RESCH-007` | `ReschedulePolicy.assertBookingReschedulable` | `RESCH-TC-012` |
| TC-COND-011 | Reschedule limit reached → `RESCH-006` (pending mechanism sign-off) | `ReschedulePolicy.assertWithinRescheduleLimit` | `RESCH-TC-013` |
| TC-COND-012 | Idempotency — respond twice / no pending proposal → `RESCH-008` | `RescheduleService.respondToReschedule` | `RESCH-TC-014` |
| TC-COND-013 | `scheduled_end` always = `newStart + duration_minutes`; duration immutable | `ReschedulePolicy` / `RescheduleService` | `RESCH-TC-015` |
| TC-COND-014 | Price/commission snapshot columns never mutated; no VNPay call | `RescheduleService` | `RESCH-TC-016` |
| TC-COND-015 | Responder must be the OTHER party (proposer cannot self-accept) | `ReschedulePolicy.assertCanReschedule` (respond context) | `RESCH-TC-017` |
| TC-COND-016 | Controller returns correct HTTP status per error code | `RescheduleController` | `RESCH-TC-018` |
| TC-COND-017 | Response never leaks JPA entity fields | `RescheduleMapper` | `RESCH-TC-019` |
| TC-COND-018 | Full propose→accept integration flow persists correctly | Integration (`consultation_bookings` row) | `RESCH-TC-INT-001` |
| TC-COND-019 | Security — non-owner IDOR attempt via `respond` endpoint | `RescheduleController` E2E | `RESCH-TC-020` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller identity (requester / assigned expert / unrelated user) | Three distinct authorization outcomes |
| Boundary Value Analysis | Deadline threshold (`now` exactly at, just before, just after cutoff) | Off-by-one risk on deadline math (ADR-RESCH-002) |
| State Transition Testing | Booking status (`ACTIVE_PRE_SESSION_STATUS ↔ RESCHEDULE_PROPOSED_STATUS`), session boundary (`WAITING`/started) | FSM correctness per §6.5 state machine |
| Error Guessing | Double-respond, self-accept, stale bookingId, malformed `newStart` | Idempotency + IDOR + input-fuzzing coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | Booking: `status=ACTIVE_PRE_SESSION_STATUS`, `scheduled_start=now+5d`, `duration_minutes=45`, no session row | Happy path base |
| `FX-002` | DB seed | Same booking + `consultation_sessions` row `session_status='WAITING'`, `started_at=NULL` | Still-reschedulable boundary (§1.2) |
| `FX-003` | DB seed | Same booking + `consultation_sessions` row `started_at=<past instant>` | Session-started blocker (`RESCH-007`) |
| `FX-004` | DB seed | Booking `status='CANCELLED'` | Terminal-state blocker (`RESCH-007`) |
| `FX-005` | DB seed | `expert_availability` row `status='AVAILABLE'` covering the proposed new slot | Slot-conflict happy path |
| `FX-006` | DB seed | No `expert_availability` row covering the proposed slot | Slot-conflict rejection (`RESCH-002`) |
| `FX-007` | DB seed | A second active booking for the same expert overlapping the proposed new slot | Overlap rejection (`RESCH-002`) |
| `FX-008` | JWT | `{ sub: requesterUserId, role: 'MOTHER' }` | Auth context — owner |
| `FX-009` | JWT | `{ sub: <other user>, role: 'MOTHER' }` | Auth context — non-owner |
| `FX-010` | JWT | `{ sub: expertUserId, role: 'EXPERT' }` | Auth context — assigned expert |
| `FX-011` | Config (test) | `reschedule.deadline-hours = 24` *(SYNTHETIC test parameter only — NOT the confirmed business value; see L2)* | Deadline boundary math |
| `FX-012` | Mock | `VnPayGatewayClient` mock, asserted `verifyNoInteractions()` | Confirms no money movement (ADR-RESCH-008) |

---

## 4. Test Case Specification

> **TC ID format:** `RESCH-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state giữa các test cases (chống AP-AI-002 Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// RescheduleTestFactory.java
class RescheduleTestFactory {

    // Symbolic status constants — see Logic Issue L4 (ADR-RESCH-005, Open)
    static final String ACTIVE_PRE_SESSION_STATUS = "CONFIRMED"; // placeholder — Open, pending UC-75 reconciliation
    static final String RESCHEDULE_PROPOSED_STATUS = "RESCHEDULE_PROPOSED"; // Proposed literal (ADR-RESCH-005)

    static final UUID REQUESTER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXPERT_USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID OTHER_USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000099");

    // FX-001 baseline — happy path, reschedulable, no session
    static ConsultationBookingEntity makeBooking() {
        return makeBooking(b -> {});
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.randomUUID());
        booking.setRequesterUserId(REQUESTER_USER_ID);
        booking.setExpertProfileId(EXPERT_PROFILE_ID);
        booking.setDurationMinutes((short) 45);
        booking.setScheduledStart(Instant.now().plus(Duration.ofDays(5)));
        booking.setScheduledEnd(Instant.now().plus(Duration.ofDays(5)).plus(Duration.ofMinutes(45)));
        booking.setPriceSnapshotAmount(new BigDecimal("500000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.20"));
        booking.setStatus(ACTIVE_PRE_SESSION_STATUS);
        overrides.accept(booking);
        return booking;
    }

    // FX-002/FX-003 — session boundary fixtures
    static ConsultationSessionEntity makeWaitingSession(UUID bookingId) {
        ConsultationSessionEntity s = new ConsultationSessionEntity();
        s.setSessionId(UUID.randomUUID());
        s.setBookingId(bookingId);
        s.setSessionStatus("WAITING");
        s.setStartedAt(null);
        return s;
    }

    static ConsultationSessionEntity makeStartedSession(UUID bookingId) {
        ConsultationSessionEntity s = makeWaitingSession(bookingId);
        s.setSessionStatus("IN_SESSION");
        s.setStartedAt(Instant.now().minus(Duration.ofMinutes(10)));
        return s;
    }

    // FX-005/FX-006 — availability fixtures
    static ExpertAvailabilityEntity makeAvailableWindow(UUID expertProfileId, Instant start, Instant end) {
        ExpertAvailabilityEntity a = new ExpertAvailabilityEntity();
        a.setAvailabilityId(UUID.randomUUID());
        a.setExpertProfileId(expertProfileId);
        a.setStartAt(start.minus(Duration.ofMinutes(30)));
        a.setEndAt(end.plus(Duration.ofMinutes(30)));
        a.setChannelType("VIDEO");
        a.setStatus("AVAILABLE");
        return a;
    }

    static ProposeRescheduleRequest makeProposeRequest(Instant newStart) {
        ProposeRescheduleRequest r = new ProposeRescheduleRequest();
        r.setNewStart(newStart);
        r.setReason("Xin đổi lịch — synthetic test reason");
        return r;
    }
}
```

---

### RESCH-TC-001 — Happy path: Mother proposes a valid new slot

**Severity:** `CRITICAL`
**Feature Under Test:** `RescheduleService.proposeReschedule()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RescheduleServiceProposeTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-RESCH-001` (two-step propose), `CB-CONSULTATION-IMP-204` §9.2 (200 response shape)

**Preconditions:**
- `FX-001` booking seeded (no session row), `FX-005` availability window covers the proposed new slot
- Caller JWT: `FX-008` (requester)

**Test Steps:**
1. Arrange: `booking = RescheduleTestFactory.makeBooking()`; mock `bookingRepository.findById` → booking; mock `sessionRepository.findByBookingId` → `Optional.empty()`; mock `availabilityRepository.findAvailableWindowCovering` → present.
2. Act: `service.proposeReschedule(booking.getBookingId(), makeProposeRequest(newStart), REQUESTER_USER_ID)`.
3. Assert: returned `status == RESCHEDULE_PROPOSED_STATUS`; returned `scheduledStart == booking's ORIGINAL start` (unchanged — Invariant 2).

**Expected Result (PASS — hành vi đúng):**
- `bookingRepository.save()` called once with `status=RESCHEDULE_PROPOSED_STATUS`, `scheduledStart`/`scheduledEnd` **unchanged**.
- `eventPublisher.publishEvent(ConsultationRescheduleProposed)` called exactly once.

**Expected Result (FAIL — dấu hiệu lỗi):**
- Slot fields mutated before ACCEPT, or event not published, or wrong status literal used.

**Current Status:** 🔴 Not written
**Implementation Note:** Use symbolic `RESCHEDULE_PROPOSED_STATUS` constant, never a bare string.

---

### RESCH-TC-002 — Happy path: Expert accepts Mother's proposal → slot applied

**Severity:** `CRITICAL`
**Feature Under Test:** `RescheduleService.respondToReschedule()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/RescheduleServiceRespondTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-RESCH-001`, `CB-CONSULTATION-IMP-204` §9.2 (accept response), Invariant 1/2 (§6.5)

**Preconditions:**
- Booking in `RESCHEDULE_PROPOSED_STATUS` state, proposed by requester (`FX-001` + proposal state)
- Caller JWT: `FX-010` (assigned expert = counterparty)

**Test Steps:**
1. Arrange booking with `status=RESCHEDULE_PROPOSED_STATUS`, stored proposed slot `newStart`.
2. Act: `service.respondToReschedule(bookingId, {decision: ACCEPT}, EXPERT_USER_ID)`.
3. Assert.

**Expected Result (PASS):**
- `scheduledStart == newStart`, `scheduledEnd == newStart + duration_minutes` (exactly `booking.durationMinutes`, unchanged from before — Invariant 1).
- `status` reverts to `ACTIVE_PRE_SESSION_STATUS`.
- `eventPublisher.publishEvent(ConsultationRescheduled)` called exactly once with `confirmedBy = EXPERT_USER_ID`.

**Expected Result (FAIL):**
- `scheduledEnd` computed from a client-supplied duration, or `durationMinutes` field changed.

**Current Status:** 🔴 Not written

---

### RESCH-TC-003 — Happy path: Expert proposes, Mother accepts (reverse actor direction)

**Severity:** `HIGH`
**Feature Under Test:** `RescheduleService` (both methods)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** SRS Table 226 "Primary Actor: Mother, Verified Expert" (either party may propose)

**Test Steps:**
1. Propose as `EXPERT_USER_ID`; respond as `REQUESTER_USER_ID`.

**Expected Result (PASS):** Symmetric behavior to `RESCH-TC-001`/`002` — no actor-direction-specific bug (e.g., no hard-coded assumption that only the Mother may propose).

**Current Status:** 🔴 Not written

---

### RESCH-TC-004 — Reject path: counterparty rejects, original slot preserved

**Severity:** `CRITICAL`
**Feature Under Test:** `RescheduleService.respondToReschedule()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-RESCH-001` (reject → proposal voided, slot unchanged); CB-187 UI text "lịch cũ sẽ bị hủy và hoàn tiền" is explicitly OUT of scope here (delegated to UC-205)

**Test Steps:**
1. Booking in `RESCHEDULE_PROPOSED_STATUS`.
2. Act: `respondToReschedule(bookingId, {decision: REJECT}, counterpartyUserId)`.

**Expected Result (PASS):**
- `scheduledStart`/`scheduledEnd` remain the **original** values (never the proposed ones).
- `status` reverts to `ACTIVE_PRE_SESSION_STATUS`.
- `eventPublisher.publishEvent(ConsultationRescheduleRejected)` called; **no** call to any cancel/refund service (this UC does not trigger UC-205 directly — only emits the event for a future consumer).

**Current Status:** 🔴 Not written

---

### RESCH-TC-005 — Deadline exceeded → RESCH-005, no mutation

**Severity:** `CRITICAL`
**Feature Under Test:** `ReschedulePolicy.assertBeforeDeadline()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** SRS §3.3.14.3 "before the allowed deadline"; `ADR-RESCH-002` (parameterized threshold, `FX-011`)

**Preconditions:** `FX-011` test config `deadline-hours=24` (SYNTHETIC — see Logic Issue L2, NOT an asserted real business value).

**Test Steps:**
1. Booking `scheduled_start = now + 1 hour` (inside the 24h synthetic deadline window).
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):**
- Throws `RescheduleDeadlineException` mapped to `RESCH-005` / HTTP 422.
- `bookingRepository.save()` is **never** called (no partial mutation).

**Expected Result (FAIL):** Booking status/slot changed despite deadline violation.

**Current Status:** 🔴 Not written
**Implementation Note:** Assert the **relational** boundary (`now > scheduledStart.minus(deadline)`), not a hard-coded hour count.

---

### RESCH-TC-006 — Deadline boundary: exactly at the cutoff is still allowed (BVA)

**Severity:** `HIGH`
**Feature Under Test:** `ReschedulePolicy.assertBeforeDeadline()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-RESCH-002` — boundary semantics: reject only when `now` is **later than** the cutoff (strictly after), so exactly-at-cutoff is still allowed.

**Test Steps:**
1. `now == scheduledStart.minus(deadlineHours)` exactly (synthetic clock injection).
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** No exception — proposal succeeds (boundary is inclusive of the cutoff instant itself).

**Current Status:** 🔴 Not written

---

### RESCH-TC-007 — Slot conflict: no AVAILABLE window covers new slot → RESCH-002

**Severity:** `CRITICAL`
**Feature Under Test:** `ReschedulePolicy.assertNoSlotConflict()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** SRS Exception E2; `ADR-RESCH-007`; `FX-006`

**Test Steps:**
1. `availabilityRepository.findAvailableWindowCovering(...)` → `Optional.empty()`.
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** Throws `RescheduleSlotConflictException` → `RESCH-002` / HTTP 409; no mutation.

**Current Status:** 🔴 Not written

---

### RESCH-TC-008 — Slot conflict: new slot overlaps another active booking of the same expert → RESCH-002

**Severity:** `CRITICAL`
**Feature Under Test:** `ReschedulePolicy.assertNoSlotConflict()` / `ConsultationBookingRepository.findOverlappingActiveByExpert()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-RESCH-007`; `FX-007`

**Test Steps:**
1. `findOverlappingActiveByExpert(...)` returns a non-empty list containing another booking (excluding the one being rescheduled).
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** Throws `RESCH-002`; the overlap query correctly **excludes** the booking being rescheduled itself (self-overlap must not false-positive).

**Current Status:** 🔴 Not written

---

### RESCH-TC-009 — Ownership denied: unrelated user cannot propose → RESCH-004

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ReschedulePolicy.assertCanReschedule()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-RESCH-004`; BR-RBAC

**Test Steps:**
1. Caller `OTHER_USER_ID` (not requester, not assigned expert).
2. Act: `proposeReschedule(bookingId, request, OTHER_USER_ID)`.

**Expected Result (PASS = hệ thống an toàn):** Throws `RescheduleAuthorizationException` → `RESCH-004` / HTTP 403; no booking data returned or mutated.

**Expected Result (FAIL = lỗ hổng tồn tại):** Unrelated user succeeds in mutating another user's booking.

**Current Status:** 🔴 Not written

---

### RESCH-TC-010 — Booking not found → RESCH-003

**Severity:** `HIGH`
**Feature Under Test:** `RescheduleService.proposeReschedule()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-CONSULTATION-IMP-204` §8.1 Javadoc `@throws BookingNotFoundException (RESCH-003)`

**Test Steps:**
1. `bookingRepository.findById(unknownId)` → `Optional.empty()`.
2. Act: `proposeReschedule(unknownId, request, REQUESTER_USER_ID)`.

**Expected Result (PASS):** Throws `BookingNotFoundException` → `RESCH-003` / HTTP 404.

**Current Status:** 🔴 Not written

---

### RESCH-TC-011 — Session already started → RESCH-007 (session boundary, §1.2)

**Severity:** `CRITICAL`
**Feature Under Test:** `ReschedulePolicy.assertBookingReschedulable()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** TDS §1.2 (cross-cutting session boundary, `ADR-SESSION-001` reused); `FX-003`

**Test Steps:**
1. `sessionRepository.findByBookingId(bookingId)` → `makeStartedSession(bookingId)` (`session_status='IN_SESSION'`, `started_at` set).
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** Throws `RescheduleStateException` → `RESCH-007` / HTTP 409; no mutation.

**Current Status:** 🔴 Not written

---

### RESCH-TC-012 — Booking already CANCELLED/COMPLETED → RESCH-007

**Severity:** `HIGH`
**Feature Under Test:** `ReschedulePolicy.assertBookingReschedulable()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-RESCH-004`; `FX-004`

**Test Steps:**
1. Booking `status='CANCELLED'` (terminal literal — used illustratively per Logic Issue L4; the exact terminal literal set is `Open`).
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** Throws `RESCH-007` / HTTP 409.

**Current Status:** 🔴 Not written

---

### RESCH-TC-013 — Reschedule limit reached → RESCH-006 (mechanism pending sign-off)

**Severity:** `MEDIUM` *(downgraded from CRITICAL pending ADR-RESCH-003 resolution — see RGR tracker)*
**Feature Under Test:** `ReschedulePolicy.assertWithinRescheduleLimit()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UI oracle CB-187 "Chỉ được đổi tối đa 1 lần miễn phí"; `ADR-RESCH-003` (`Open` — counting mechanism unresolved)

**Preconditions:** A test-double reschedule-count source (mock) reporting `count >= limit(1)` for the booking — **NOT** a real DB column (none exists yet).

**Test Steps:**
1. Mock the count-source seam to report the limit already reached.
2. Act: `proposeReschedule(...)`.

**Expected Result (PASS):** Throws `RescheduleLimitException` → `RESCH-006` / HTTP 409.

**Current Status:** 🟡 **Pending mechanism sign-off (ADR-RESCH-003) — written against the policy seam only; MUST be revisited once Option A (column) or Option B (event-derived) is chosen. Do not mark 🟢 GREEN until the real counting source is wired.**

---

### RESCH-TC-014 — Idempotency: responding twice / no pending proposal → RESCH-008

**Severity:** `HIGH`
**Feature Under Test:** `RescheduleService.respondToReschedule()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-CONSULTATION-IMP-204` §8.1 Javadoc `@throws RescheduleStateException (RESCH-008)`; §9.1 endpoint idempotency note

**Test Steps:**
1. Booking already in `ACTIVE_PRE_SESSION_STATUS` (no pending proposal, e.g. never proposed, or a prior respond already resolved it).
2. Act: `respondToReschedule(bookingId, {decision: ACCEPT}, counterpartyUserId)`.

**Expected Result (PASS):** Throws `RESCH-008` / HTTP 409; calling it again after a first successful ACCEPT (booking now `ACTIVE_PRE_SESSION_STATUS`) also throws `RESCH-008` — **no double slot-application, no double event emission**.

**Current Status:** 🔴 Not written

---

### RESCH-TC-015 — Duration/end-time correctness: `scheduled_end` always = `newStart + duration_minutes`

**Severity:** `CRITICAL`
**Feature Under Test:** `RescheduleService.respondToReschedule()` (ACCEPT path)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** Invariant 1 (TDS §6.5); `ADR-RESCH-007`

**Test Steps:**
1. Booking `durationMinutes=45`. Proposal `newStart = X`.
2. Act: accept.

**Expected Result (PASS):**
```java
assertThat(booking.getScheduledEnd())
    .isEqualTo(newStart.plus(Duration.ofMinutes(booking.getDurationMinutes())));
assertThat(booking.getDurationMinutes()).isEqualTo((short) 45); // unchanged
```

**Current Status:** 🔴 Not written

---

### RESCH-TC-016 — No financial side effects: price/commission snapshot immutable, VNPay never called

**Severity:** `CRITICAL`
**Feature Under Test:** `RescheduleService` (both propose and respond)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-RESCH-008`; `FX-012`

**Test Steps:**
1. Capture `priceSnapshotAmount`/`commissionRateSnapshot`/`priceLockedAt` before propose+accept.
2. Run full propose→accept flow.
3. Assert values unchanged; assert `verifyNoInteractions(vnPayGatewayClient)`.

**Expected Result (PASS):** All snapshot fields byte-for-byte equal before/after; zero interactions with the VNPay mock.

**Current Status:** 🔴 Not written

---

### RESCH-TC-017 — Proposer cannot self-accept their own proposal

**Severity:** `HIGH`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Feature Under Test:** `ReschedulePolicy.assertCanReschedule()` (respond context)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `ADR-RESCH-001` (counterparty must accept — self-accept would defeat the two-step consent model)

**Test Steps:**
1. Requester proposes; then the **same** requester (not the expert) calls `respondToReschedule(ACCEPT)`.

**Expected Result (PASS):** Throws `RESCH-004` (the responder is not the counterparty) — self-accept is rejected.

**Current Status:** 🔴 Not written

---

### RESCH-TC-018 — Controller maps each policy exception to the documented HTTP status

**Severity:** `HIGH`
**Feature Under Test:** `RescheduleController`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/RescheduleControllerTest.java` (`@WebMvcTest`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CB-CONSULTATION-IMP-204` §10 Error Code table (status column)

**Test Steps (table-driven):**

| Mocked Service Exception | Expected HTTP |
|---|---|
| `RescheduleValidationException` | 400 |
| `RescheduleSlotConflictException` | 409 |
| `BookingNotFoundException` | 404 |
| `RescheduleAuthorizationException` | 403 |
| `RescheduleDeadlineException` | 422 |
| `RescheduleLimitException` | 409 |
| `RescheduleStateException` | 409 |

**Expected Result (PASS):** Each mapped status matches §10 exactly; body contains the matching `RESCH-0xx` code.

**Current Status:** 🔴 Not written

---

### RESCH-TC-019 — Response DTO never leaks JPA entity internals

**Severity:** `HIGH`
**Feature Under Test:** `RescheduleMapper`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `CLAUDE.md` Delivery Rules ("Never expose JPA entities in API responses")

**Test Steps:**
```java
String json = objectMapper.writeValueAsString(rescheduleResponse);
assertThat(json).doesNotContain("hibernateLazyInitializer");
assertThat(json).doesNotContain("requesterUserId"); // only bookingId/slots/status exposed per §8.1 DTO
```

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### RESCH-TC-020 — IDOR attack: forged bookingId in respond endpoint

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Legal:** `BR-RBAC`, PDPA minimum-necessary access
**Feature Under Test:** `RescheduleController` (E2E, `@SpringBootTest` + Testcontainers)
**Test File:** `src/test/java/com/carebridge/backend/consultation/RescheduleSecurityE2ETest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Booking A owned by Mother-1/Expert-1; attacker is Mother-2 (unrelated).

**Test Steps (Attack Simulation):**
1. Mother-2 authenticates with a valid JWT for their own account.
2. Mother-2 calls `PATCH /consultations/bookings/{bookingA_id}/reschedule/respond` with a guessed/enumerated `bookingId` belonging to Mother-1/Expert-1.
3. Inspect response and DB state.

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`, body `{"error":{"code":"RESCH-004"}}`.
- Booking A's `scheduled_start`/`status` in DB unchanged.

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Mother-2 successfully mutates Booking A's schedule.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### RESCH-TC-INT-001 — Full propose→accept flow persists correctly end-to-end

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: propose → respond(ACCEPT)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/RescheduleIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: `FX-001` booking (via JPA), `FX-005` availability window

**Test Steps:**
1. Seed booking + availability via repository (SYNTHETIC data only).
2. `POST`/`PATCH …/reschedule` with a valid new slot as the requester.
3. `PATCH …/reschedule/respond {ACCEPT}` as the assigned expert.
4. Assert DB state.

**Expected Result (PASS):**
- `consultation_bookings.scheduled_start`/`scheduled_end` updated to the new slot.
- `consultation_bookings.status` back to the active pre-session literal.
- No row created/mutated in `consultation_sessions`, `payment_transactions`, or `refund_records` (this UC touches booking only — ADR-RESCH-008).

**DB Assertion:**
```java
ConsultationBookingEntity record = bookingRepository.findById(savedId).orElseThrow();
assertThat(record.getScheduledStart()).isEqualTo(newStart);
assertThat(record.getScheduledEnd()).isEqualTo(newStart.plus(Duration.ofMinutes(record.getDurationMinutes())));
assertThat(record.getStatus()).isEqualTo(RescheduleTestFactory.ACTIVE_PRE_SESSION_STATUS);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RESCH-TC-001` | `RescheduleServiceProposeTest.java` | `[ ]` | `___` | |
| `RESCH-TC-002` | `RescheduleServiceRespondTest.java` | `[ ]` | `___` | |
| `RESCH-TC-003` | `RescheduleServiceRespondTest.java` | `[ ]` | `___` | |
| `RESCH-TC-004` | `RescheduleServiceRespondTest.java` | `[ ]` | `___` | |
| `RESCH-TC-005` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-006` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-007` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-008` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-009` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-010` | `RescheduleServiceProposeTest.java` | `[ ]` | `___` | |
| `RESCH-TC-011` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-012` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-013` | `ReschedulePolicyTest.java` | `[ ]` | `___` | 🟡 blocked on ADR-RESCH-003 |
| `RESCH-TC-014` | `RescheduleServiceRespondTest.java` | `[ ]` | `___` | |
| `RESCH-TC-015` | `RescheduleServiceRespondTest.java` | `[ ]` | `___` | |
| `RESCH-TC-016` | `RescheduleServiceIntegrationTest.java` | `[ ]` | `___` | |
| `RESCH-TC-017` | `ReschedulePolicyTest.java` | `[ ]` | `___` | |
| `RESCH-TC-018` | `RescheduleControllerTest.java` | `[ ]` | `___` | |
| `RESCH-TC-019` | `RescheduleMapperTest.java` | `[ ]` | `___` | |
| `RESCH-TC-020` | `RescheduleSecurityE2ETest.java` | `[ ]` | `___` | |
| `RESCH-TC-INT-001` | `RescheduleIntegrationTest.java` | `[ ]` | `___` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class RescheduleService implements IRescheduleService {

    @Override
    public RescheduleResponse proposeReschedule(UUID bookingId, ProposeRescheduleRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public RescheduleResponse respondToReschedule(UUID bookingId, RescheduleResponseRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ReschedulePolicy {
    public void assertCanReschedule(ConsultationBookingEntity booking, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertBookingReschedulable(ConsultationBookingEntity booking, Optional<ConsultationSessionEntity> sessionOpt) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertBeforeDeadline(ConsultationBookingEntity booking, Instant now) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertWithinRescheduleLimit(UUID bookingId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertNoSlotConflict(UUID expertProfileId, Instant newStart, Instant newEnd, UUID excludingBookingId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RESCH-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `RESCH-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ⚠️ mechanism Open — gate applies to the seam only |
| `RESCH-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-020` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RESCH-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(to be filled when implementation branch is opened)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(to be generated at implementation time)*

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-204` đã được review và approve
- [ ] **Open items resolved:** deadline value (ADR-RESCH-002), reschedule-count mechanism (ADR-RESCH-003), booking status literals (ADR-RESCH-005) confirmed by Product/Tech Lead/UC-75 owner
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] No new Flyway migration required for the core flow (confirmed — §5.3 of TDS); IF `reschedule_count` migration is approved, it must run successfully on staging first
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip), trừ `RESCH-TC-013` nếu ADR-RESCH-003 vẫn `Open` (phải được explicitly waived by Tech Lead, không silently skipped)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `RescheduleService`/`ReschedulePolicy`
- [ ] Không có business logic trong `RescheduleController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `RESCH-TC-016` xanh: không có tương tác VNPay nào, snapshot giá bất biến
- [ ] `RESCH-TC-015` xanh: `scheduled_end` luôn = `newStart + duration_minutes`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua `RescheduleTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/UI oracle) — verified above, every TC cites one

### Suspension Criteria (Điều kiện tạm dừng)

- ADR-RESCH-002/003/005 Open items unresolved beyond Sprint start (blocks `RESCH-TC-005/006/013` and all status-literal-dependent tests)
- UC-75 booking-lifecycle status enum revised in a way that contradicts this Test-Spec's symbolic constants
- CI pipeline broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No new migration in baseline scope (§5.3 of TDS) — code-only rollback:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/

# IF the optional reschedule_count migration (ADR-RESCH-003 Option A) was applied:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.consultation_bookings DROP COLUMN IF EXISTS reschedule_count;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

# Gap vẫn OPEN → giữ nguyên entry trong TDS §11.1 Prerequisites
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ Checklist cho reviewer khi test cases được AI hỗ trợ generate.

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Checked — every TC cites an Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(verify at Red Gate execution time)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes an architecture decision without an ADR (e.g., unified `consultations` table) | ☑ Checked — all TCs use split-schema entities (ADR-RESCH-006) | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic | ☑ Checked — `RESCH-TC-018` only asserts HTTP status mapping | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports service/type not in TDS §8 | ☑ Checked — all types trace to TDS §8.1/§8.2 | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở cấp spec (static review) → TDD spec approved for Red Gate execution
- [ ] AP-AI-002 (Green-from-Birth) can only be confirmed once the Red Gate is actually executed against the stub — pending implementation kickoff

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(none at spec-review time)* | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
*Test-Spec UC-204 v1.0 — Draft. Requires Tech Lead sign-off on Open items (ADR-RESCH-002/003/005) before `RESCH-TC-005/006/013` and all status-literal-dependent assertions may be treated as binding.*
