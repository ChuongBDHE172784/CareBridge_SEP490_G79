# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC207 — Mark Consultation No-show — Test Specification

**Document ID:** `FPT-EDU-TDD-CB-CONSULTATION-207`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC207_MarkConsultationNoShow/UC207_MarkConsultationNoShow_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` §ADR-SESSION-001/002/003 — reused session lifecycle owner (authoritative, cited not redefined)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` — dispute/refund boundary reference
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.6 (L4452-4471, Table 229) — Functional requirement (UC-207)
- `CLAUDE.md` — Project rules (RBAC, consent, audit mandates)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC207 |

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
| **Feature / Gap ID** | `UC-207` |
| **Module** | `Consultation — Mark No-show` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-207` |
| **Priority** | 🟠 P1 |
| **Sprint** | `Sprint 4 "Real Providers And Admin Polish"` — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `Confidential` (no-show evidence note is `Sensitive-PII`-adjacent — may reference health context) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY`, `BR-SAFETY`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `UC-95 ManageConsultationSession` (session lifecycle owner — `ConsultationSessionEntity/Service/Policy/Repository`, reused not recreated), `UC-75/76` booking/payment (`scheduled_start`) |
| **Downstream Consumers** | `UC-209 ResolveConsultationDispute`, `UC-210 ApproveOrRejectRefund` (boundary — override/refund only, see §2 L-item), Notification service |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TypeScript/Vite, CB-188/CB-199) + Mobile (Flutter, CB-184) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC207_MarkConsultationNoShow_TDS.md §17` (C1-C7), ADR-NSH-001/002 (new), ADR-SESSION-001/002/003 (UC95, reused) |
| **Constraints Injected** | State-transition confined to `WAITING→NO_SHOW` only (C1), assigned+verified Expert ownership reused from UC95 (C2), waiting-period gate via named configurable constant (C3), no rollback on notification failure / no mutation on rejection (C4), admin surface read/audit-only with override delegated to UC-209 (C5), shared `ConsultationSessionStatusChanged` event reused (C6), package/entity-exposure/minimum-necessary rules (C7) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-207 says "Records no-show **after the waiting period**" but does not quantify it (no numeric minutes anywhere in SRS §3.3.14.6) | TDS `ADR-NSH-001` proposes reusing UC95 ADR-SESSION-003's `15 minutes` figure (also shown in CB-199 mockup timeline: "Hết thời gian chờ ... sau 15 phút") as a **named configurable constant**, explicitly flagged `Open-NSH-2` | Tests assert the *mechanism* (before threshold → `NSH-005`; at/after threshold → allowed) against the configured `WAITING_PERIOD` constant, never a bare hard-coded literal in the assertion oracle. Boundary tests use `scheduled_start - (WAITING_PERIOD - 1min)` and `scheduled_start - (WAITING_PERIOD + 1min)` relative offsets, not an assumed absolute "15" unless the constant itself resolves to that value. |
| L2 | SRS lists both "Verified Expert" and "System Admin" as primary actors of UC-207 with a single generic template Normal Flow — does not distinguish the two very different actions (marking vs. reviewing) | TDS `§1`/`ADR-NSH-002` splits this into two API surfaces: Expert `POST .../no-show` (mutates state) and Admin `GET .../no-show-reviews` + `GET .../{id}/no-show-review` (read-only + uphold annotation) | Tests are organized in two families: mark-side (`NSH-TC-001..006`, `010..013`) and review-side (`NSH-TC-007..008`), each asserting the correct role gate independently — no test conflates the two roles. |
| L3 | CB-199 UI mockup shows an "Ghi đè / Hợp lệ (Hoàn tiền)" (override → refund) admin button that visually lives on the same screen as the no-show confirmation | TDS `ADR-NSH-002` draws an explicit boundary: override/refund is dispute resolution, owned by `UC-209`/`UC-210`, **not** a UC207 endpoint; UC207 returns `NSH-006` if this action is attempted through its own surface | `NSH-TC-013` explicitly asserts the boundary is enforced (`NSH-006`, no `consultation_disputes`/`refund_records` row created by UC207 code) — prevents an AI implementation from silently building the refund path inside `ConsultationSessionService` (AP-CB-203). |
| L4 | UC95's own ADR-SESSION-003 framed `WAITING → NO_SHOW` as an **automatic reconciliation** transition (background check), while UC207 requires an **actor-initiated** confirmation (Expert explicitly marks it, per CB-184/CB-188) | TDS `§1.2` reconciles this: UC207 implements the explicit, actor-initiated counterpart of the same edge; the state machine invariant (only one terminal value, one direction) is identical, only the trigger differs | Tests never assert a background/cron-triggered transition for UC207 — `NSH-TC-001` explicitly asserts the trigger is an authenticated `POST` call from the assigned Expert, not a scheduled job. |
| L5 | UI mockups (CB-188/CB-199) show a required free-text no-show reason/evidence note, but `consultation_sessions` has no dedicated `no_show_reason` column (`V1__init_schema.sql` L898-909) | TDS `§5.3`/`Open-NSH-4` proposes (not migrates) storing the note as a structured entry inside the existing `technical_log_json jsonb` column | `NSH-TC-INT-002` asserts the note is retrievable from `technical_log_json` after a successful mark — no new column/migration is asserted or required by any test. |
| L6 | SRS UC-207 Exceptions are the generic E1/E2/E3 template wording ("access denied", "invalid data rejected", "external service failure handled with retry guidance") without feature-specific detail | Tests translate these generically-worded exceptions into concrete, TDS-cited assertions: E1 → `NSH-TC-004/005/008` (403 `NSH-004`), E2 → `NSH-TC-002/003/006/012` (409/400), E3 → `NSH-TC-INT-001` (notification failure does not roll back committed state) | Each test's Oracle Source cites the exact TDS ADR/error-code, not the generic SRS wording alone. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Mark Consultation No-show bao gồm các layer:
├── Domain (ConsultationSessionPolicy.assertWaitingPeriodElapsed — pure logic, no deps)
├── Application / Use Cases (ConsultationSessionService.markNoShow/listNoShowReviews/getNoShowReview
│                             — mock JPA Repository với Mockito; NO ZegoCloud collaborator needed for this slice)
├── Controller (ConsultationSessionController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full mark → DB state + event flow)
└── Web (MarkNoShowDialog.tsx / NoShowReviewPage.tsx — Vitest + Testing Library, MSW for API mocking)
```

> **Reuse boundary:** this TDD spec does **not** re-test UC95's `joinSession`/`endSession`/general
> state-machine plumbing — those are covered by `UC95_ManageConsultationSession_Test-Spec.md`. Only
> the `WAITING → NO_SHOW` transition and the admin review surface are in scope here.

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-207` (§3.3.14.6, L4452-4471, Table 229) | "Records no-show after the waiting period and session-status evidence"; actors Verified Expert + System Admin |
| `ADR-SESSION-001` (UC95, reused) | `NO_SHOW` is terminal; the `WAITING → NO_SHOW` edge; no direct transition from any other state |
| `ADR-SESSION-002` (UC95, reused) | Ownership — assigned, verified Expert only may mark |
| `ADR-SESSION-003` (UC95, reused) | External-service/notification failure must not mutate `session_status`; no duplicate unsafe action |
| `ADR-NSH-001` (new) | Waiting-period threshold gate — `Open-NSH-2` exact minutes |
| `ADR-NSH-002` (new) | Admin review scope — read/audit + uphold only; override/refund → UC-209/UC-210 boundary |
| `BR-RBAC` / `BR-PRIVACY` | Role/ownership-scoped access; minimum-necessary admin review DTO |
| `CB-CONSULTATION-IMP-207 §9/§10/§16` | API contract, error codes (`NSH-001..006, NSH-500`), authorization matrix |
| PDPA / BR-CONSULTATION | Auditable lifecycle state requirement |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Assigned, verified Expert marks WAITING session past waiting period | `ConsultationSessionService.markNoShow()` | `NSH-TC-001` |
| TC-COND-002 | Waiting period not yet elapsed | `ConsultationSessionPolicy.assertWaitingPeriodElapsed()` | `NSH-TC-002` |
| TC-COND-003 | Session not in WAITING state (e.g. `IN_SESSION`) | `ConsultationSessionPolicy.assertTransitionAllowed()` | `NSH-TC-003` |
| TC-COND-004 | Non-assigned Expert attempts mark | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `NSH-TC-004` |
| TC-COND-005 | Unverified assigned Expert attempts mark | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `NSH-TC-005` |
| TC-COND-006 | Blank/missing `reason` | `MarkNoShowRequest` validation | `NSH-TC-006` |
| TC-COND-007 | Admin lists `NO_SHOW` reviews (minimum-necessary) | `ConsultationSessionService.listNoShowReviews()` | `NSH-TC-007` |
| TC-COND-008 | Non-admin (Expert/Mother) calls admin review endpoints | `ConsultationSessionController` admin routes | `NSH-TC-008` |
| TC-COND-009 | `ConsultationSessionStatusChanged` payload correctness | `ConsultationSessionService` (event emission) | `NSH-TC-009` |
| TC-COND-010 | Boundary — waiting-period threshold ± 1 minute | `ConsultationSessionPolicy.assertWaitingPeriodElapsed()` | `NSH-TC-010` |
| TC-COND-011 | Session not found | `ConsultationSessionController` | `NSH-TC-011` |
| TC-COND-012 | Already-`NO_SHOW` (terminal) session re-marked | `ConsultationSessionPolicy.assertTransitionAllowed()` | `NSH-TC-012` |
| TC-COND-013 | Admin attempts override/refund via UC207 surface | `ConsultationSessionController` (boundary enforcement) | `NSH-TC-013` |
| TC-COND-014 | Notification/audit dispatch failure after commit | `ConsultationSessionService.markNoShow()` (integration) | `NSH-TC-INT-001` |
| TC-COND-015 | No-show note persisted and retrievable in `technical_log_json` | Integration — DB state | `NSH-TC-INT-002` |
| TC-COND-016 | Web: `MarkNoShowDialog` requires reason, calls API on submit | `MarkNoShowDialog.tsx` | `NSH-TC-WEB-001` |
| TC-COND-017 | Web: `NoShowReviewPage` override action navigates to UC-209 flow, not a UC207 call | `NoShowReviewPage.tsx` | `NSH-TC-WEB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role (assigned-verified / assigned-unverified / non-assigned / admin / non-admin), `session_status` value | Covers each authorization/state class with one representative case |
| Boundary Value Analysis | Waiting-period threshold (`WAITING_PERIOD - 1min` vs `WAITING_PERIOD + 1min`) | Hard business-rule edge (ADR-NSH-001); off-by-one is the most likely defect |
| State Transition Testing | `session_status` — only `WAITING → NO_SHOW` in scope, all other source states rejected | Core invariant of this slice (reuses UC95 §6.4 FSM) |
| Error Guessing | Already-terminal re-mark, admin-override-via-wrong-endpoint, blank reason | Boundary-crossing attempts an AI implementation is prone to mishandle |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-N01` | DB seed | `consultation_bookings{status:'CONFIRMED', expert_profile_id: EXPERT-001, scheduled_start: now()-20min}` | Happy-path booking, past waiting period |
| `FX-N02` | DB seed | `consultation_sessions{session_status:'WAITING', booking_id: FX-N01, started_at: NULL}` | Initial session state — no one has joined |
| `FX-N03` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status:'VERIFIED'}` | Assigned, verified Expert |
| `FX-N04` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status:'PENDING'}` | Unverified Expert (negative case) |
| `FX-N05` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned |
| `FX-N06` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned |
| `FX-N07` | JWT | `{sub: 'ADMIN-001', role: 'SYSTEM_ADMIN'}` | Auth context — admin |
| `FX-N08` | DB seed | `consultation_bookings{scheduled_start: now()-(WAITING_PERIOD - 1min)}` + session `WAITING` | Boundary — within grace (too early) |
| `FX-N09` | DB seed | `consultation_bookings{scheduled_start: now()-(WAITING_PERIOD + 1min)}` + session `WAITING` | Boundary — past grace (allowed) |
| `FX-N10` | DB seed | `consultation_sessions{session_status:'IN_SESSION'}` | Wrong-state precondition (not WAITING) |
| `FX-N11` | DB seed | `consultation_sessions{session_status:'NO_SHOW'}` | Already-terminal precondition (idempotency guard) |
| `FX-N12` | Mock | `ApplicationEventPublisher.publishEvent()` throws / notification dispatch throws | Failure-after-commit simulation (ADR-SESSION-003) |
| `FX-N13` | Request | `MarkNoShowRequest{reason: ""}` | Blank-reason validation case |
| `FX-N14` | Request | `MarkNoShowRequest{reason: "Called 3 times, no answer."}` | Valid evidence note |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// Extends the ConsultationSessionTestFactory pattern already established
// in UC95_ManageConsultationSession_Test-Spec.md — do NOT fork a parallel
// factory class; add UC207-specific overloads here if the shared factory
// class does not yet exist in the codebase at implementation time.
// ═══════════════════════════════════════════════════════════

// ConsultationSessionTestFactory.java (shared with UC95; UC207 adds no-show helpers)
class ConsultationSessionTestFactory {

    static final UUID BOOKING_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1    = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    static final UUID ADMIN_USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000901");

    // Reused from UC95's WAITING_PERIOD constant under test (ADR-NSH-001) —
    // tests reference this named constant, never a bare literal.
    static final Duration WAITING_PERIOD = Duration.ofMinutes(15); // Open-NSH-2 — proposed default

    static ConsultationBookingEntity makeBookingPastWaitingPeriod() {
        return makeBooking(b -> b.setScheduledStart(Instant.now().minus(WAITING_PERIOD).minusMinutes(5)));
    }

    static ConsultationBookingEntity makeBookingWithinWaitingPeriod() {
        return makeBooking(b -> b.setScheduledStart(Instant.now().minus(WAITING_PERIOD).plusMinutes(1)));
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-000000000501"));
        booking.setStatus("CONFIRMED");
        booking.setScheduledStart(Instant.now().minusMinutes(20));
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeWaitingSession() {
        return makeSession(s -> s.setSessionStatus("WAITING"));
    }

    static ConsultationSessionEntity makeSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus("WAITING");
        session.setStartedAt(null); // participant never joined — no-show precondition
        overrides.accept(session);
        return session;
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

    static MarkNoShowRequest makeMarkNoShowRequest() {
        return makeMarkNoShowRequest(r -> {});
    }

    static MarkNoShowRequest makeMarkNoShowRequest(Consumer<MarkNoShowRequest> overrides) {
        MarkNoShowRequest request = new MarkNoShowRequest();
        request.setReason("Called the participant 3 times, no answer; not online past waiting period.");
        overrides.accept(request);
        return request;
    }
}
```

---

### NSH-TC-001 — Happy path: assigned verified Expert marks WAITING session past waiting period → NO_SHOW

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.markNoShow()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SESSION-001` (terminal `NO_SHOW`, `WAITING→NO_SHOW` edge — UC95, reused), `ADR-SESSION-002` (ownership — UC95, reused), `ADR-NSH-001` (waiting-period gate) — `UC207_MarkConsultationNoShow_TDS.md §6.1, §6.5`

**Preconditions:**
- `FX-N01`/`FX-N09` (booking, `scheduled_start` past `WAITING_PERIOD`), `FX-N02` (session `WAITING`, `started_at=null`), `FX-N03` (verified Expert)

**Test Steps:**
1. Arrange: seed via `makeBookingPastWaitingPeriod()` + `makeWaitingSession()` + `makeVerifiedExpertProfile()`; request `makeMarkNoShowRequest()`
2. Act: call `service.markNoShow(SESSION_ID_1, request, EXPERT_USER_ID_1)`
3. Assert: response `sessionStatus == "NO_SHOW"`; repository `save()` invoked once with `sessionStatus=NO_SHOW`, `endedAt` set, `startedAt` remains `null`

**Expected Result (PASS — hành vi đúng):**
- Session transitions `WAITING → NO_SHOW`; exact literal `"NO_SHOW"` (not `"NOSHOW"`/`"ABSENT"`); `ConsultationSessionStatusChanged` published with `newStatus="NO_SHOW"`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Status not updated, wrong literal used, or transition allowed without the waiting-period/ownership checks executing first

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important assertion for this slice — it is the one and only transition UC207 owns.

---

### NSH-TC-002 — Too early: waiting period not yet elapsed → 409 NSH-005, status unchanged

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertWaitingPeriodElapsed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-NSH-001` — waiting-period gate; error code `NSH-005` (`TDS §10`)

**Preconditions:** `FX-N08` (`scheduled_start` within `WAITING_PERIOD`), session `WAITING`

**Test Steps:**
1. Arrange: booking via `makeBookingWithinWaitingPeriod()`
2. Act: call `service.markNoShow(SESSION_ID_1, makeMarkNoShowRequest(), EXPERT_USER_ID_1)`
3. Assert: throws `NoShowTooEarlyException` with code `NSH-005`; repository `save()` never invoked

**Expected Result (PASS):** `session_status` remains `WAITING`; no partial state written; SRS E2 ("invalid ... data is rejected")
**Expected Result (FAIL):** Mark succeeds before the threshold, or status mutated despite rejection

**Current Status:** 🔴 Not written

---

### NSH-TC-003 — Wrong state: session not WAITING (IN_SESSION) → 409 NSH-002

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SESSION-001 §6.4` (UC95, reused — no-show only from `WAITING`), error code `NSH-002` (≡ `SES-006`, `TDS §10`)

**Preconditions:** `FX-N10` (session `IN_SESSION`), booking past waiting period

**Test Steps:**
1. Arrange: session `session_status='IN_SESSION'`
2. Act: call `service.markNoShow(SESSION_ID_1, makeMarkNoShowRequest(), EXPERT_USER_ID_1)`
3. Assert: throws `SessionTransitionException` (`NSH-002`)

**Expected Result (PASS):** Rejected — only `WAITING → NO_SHOW` is a valid `markNoShow()` transition; `IN_SESSION` (participant already joined) is explicitly out of scope per `Open-NSH-1`
**Expected Result (FAIL):** `IN_SESSION` session incorrectly allowed to be marked no-show

**Current Status:** 🔴 Not written
**Implementation Note:** Do NOT silently add an `IN_SESSION → NO_SHOW` edge to satisfy this — that is `Open-NSH-1`, owned by UC95, not decided here.

---

### NSH-TC-004 — Ownership violation: non-assigned Expert marks no-show → 403 NSH-004

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SESSION-002` (UC95, reused), error code `NSH-004` (≡ `SES-004`, `TDS §10`)

**Preconditions:** `FX-N01`/`FX-N02` seeded; caller = `OTHER_EXPERT_USER_ID` (not `expert_profiles.user_id` for the booking)

**Test Steps:**
1. Arrange: booking assigned to `EXPERT_PROFILE_ID_1` (→ `EXPERT_USER_ID_1`)
2. Act: call `service.markNoShow(SESSION_ID_1, makeMarkNoShowRequest(), OTHER_EXPERT_USER_ID)`
3. Assert: throws `SessionAuthorizationException` (`NSH-004`); no repository write; ownership check executes before the waiting-period check (fail-fast on identity first)

**Expected Result (PASS):** Exception thrown; no state mutation
**Expected Result (FAIL):** Mark succeeds, or waiting-period check runs before ownership (wrong fail-fast order)

**Current Status:** 🔴 Not written

---

### NSH-TC-005 — Unverified assigned Expert attempts mark → 403 NSH-004

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SESSION-002` (UC95, reused) — "Verified Expert" actor requirement

**Preconditions:** `FX-N04` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`
2. Act: call `service.markNoShow(SESSION_ID_1, makeMarkNoShowRequest(), EXPERT_USER_ID_1)`
3. Assert: throws `SessionAuthorizationException` (`NSH-004`)

**Expected Result (PASS):** Rejected despite matching `user_id`, because `verification_status != 'VERIFIED'`
**Expected Result (FAIL):** Unverified expert allowed to mark no-show

**Current Status:** 🔴 Not written

---

### NSH-TC-006 — Validation: blank `reason` → 400 NSH-001

**Severity:** `MEDIUM`
**Feature Under Test:** `MarkNoShowRequest` (`@NotBlank` validation)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §8.1` DTO contract, error code `NSH-001`; CB-188/CB-199 UI mark the note field "Bắt buộc" (required)

**Preconditions:** `FX-N13` (`reason: ""`)

**Test Steps:**
1. Arrange: `POST` body `{"reason": ""}`
2. Act: `POST /api/v1/consultations/sessions/{sessionId}/no-show`
3. Assert: HTTP 400, body `{"error":{"code":"NSH-001", ...}}`; service method never invoked

**Expected Result (PASS):** Validation short-circuits before the service layer
**Expected Result (FAIL):** Blank reason accepted, or validation happens inside the service instead of the DTO (layer violation)

**Current Status:** 🔴 Not written

---

### NSH-TC-007 — Admin lists NO_SHOW reviews (minimum-necessary fields)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionService.listNoShowReviews()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-NSH-002` — admin review scope; `TDS §9.2` response schema; BR-PRIVACY minimum-necessary

**Preconditions:** At least one session with `session_status='NO_SHOW'` seeded (`FX-N11`)

**Test Steps:**
1. Arrange: seed a `NO_SHOW` session; caller = `ADMIN_USER_ID` (`FX-N07`, role `SYSTEM_ADMIN`)
2. Act: call `service.listNoShowReviews(0, 20, ADMIN_USER_ID)`
3. Assert: returns a page containing `NoShowReviewResponse` with only the minimum-necessary fields declared in `TDS §8.1` (`sessionId, bookingId, sessionStatus, scheduledStart, startedAt, endedAt, expertName, participantName, noShowReason, markedBy, markedAt`) — no raw health-record content, no `ConsultationSessionEntity` leak

**Expected Result (PASS):** DTO shape matches `TDS §8.1` exactly; repository queried via `findBySessionStatus('NO_SHOW', pageable)`
**Expected Result (FAIL):** Entity exposed directly, or extra/health-record fields present (BR-PRIVACY violation)

**Current Status:** 🔴 Not written

---

### NSH-TC-008 — Non-admin (Expert/Mother) calls admin review endpoint → 403 NSH-004

**Severity:** `HIGH`
**CWE:** `CWE-284 — Improper Access Control`
**Feature Under Test:** `ConsultationSessionController` (admin route guard)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §16` Authorization Matrix — Expert/Mother row is ❌ for both admin review endpoints

**Preconditions:** Caller JWT has role `EXPERT` or no admin role

**Test Steps:**
1. Arrange: authenticate as `EXPERT_USER_ID_1` (role `EXPERT`, not `SYSTEM_ADMIN`)
2. Act: `GET /api/v1/consultations/sessions/no-show-reviews`
3. Assert: HTTP 403, body `{"error":{"code":"NSH-004", ...}}`

**Expected Result (PASS):** Access denied regardless of whether the caller is the assigned Expert on any `NO_SHOW` session
**Expected Result (FAIL):** Expert/Mother can read admin review data (BR-PRIVACY breach — cross-role data exposure)

**Current Status:** 🔴 Not written

---

### NSH-TC-009 — `ConsultationSessionStatusChanged` event payload correctness

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService.markNoShow()` (event emission)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §7.1/§7.3` Domain Event Catalog (shared event, reused schema)

**Preconditions:** `FX-N01`/`FX-N02`/`FX-N09` seeded (past waiting period)

**Test Steps:**
1. Arrange: mock `ApplicationEventPublisher`
2. Act: call `markNoShow()` (triggers `WAITING→NO_SHOW`)
3. Assert: `eventPublisher.publishEvent()` called once with a `ConsultationSessionStatusChanged` whose `payload.previousStatus="WAITING"`, `payload.newStatus="NO_SHOW"`, `payload.startedAt=null`, `payload.endedAt` set, `metadata.causedBy=EXPERT_USER_ID_1.toString()`

**Expected Result (PASS):** Event payload matches exact transition and uses the **shared** event type (not a forked `NoShowMarked` event — AP-CB-202)
**Expected Result (FAIL):** No event published, wrong payload values, or a new/forked event type used

**Current Status:** 🔴 Not written

---

### NSH-TC-010 — Boundary: waiting-period threshold ± 1 minute (BVA)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertWaitingPeriodElapsed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-NSH-001` — `WAITING_PERIOD` boundary (Boundary Value Analysis, `TDS-04`)

**Preconditions:** `FX-N08` (`scheduled_start = now() - (WAITING_PERIOD - 1min)`), `FX-N09` (`scheduled_start = now() - (WAITING_PERIOD + 1min)`)

**Test Steps:**
1. Arrange/Act/Assert (within grace): `assertWaitingPeriodElapsed(bookingWithin, now())` → throws `NoShowTooEarlyException`
2. Arrange/Act/Assert (past grace): `assertWaitingPeriodElapsed(bookingPast, now())` → returns normally (no exception)

**Expected Result (PASS):** Exactly at the configured threshold, before → rejected, after → allowed; test references `WAITING_PERIOD` constant, not a bare "15"
**Expected Result (FAIL):** Off-by-one at the boundary (e.g. inclusive/exclusive mismatch)

**Current Status:** 🔴 Not written
**Implementation Note:** If `Open-NSH-2` is later confirmed to a different value, only the `WAITING_PERIOD` constant changes — this test must not need rewriting.

---

### NSH-TC-011 — Session not found → 404 NSH-003

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController` / `ConsultationSessionService.markNoShow()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** Error code `NSH-003` (≡ `SES-003`, `TDS §10`)

**Preconditions:** No session with the given ID exists

**Test Steps:**
1. Arrange: `sessionRepository.findById(nonExistentId)` returns `Optional.empty()`
2. Act: `POST /api/v1/consultations/sessions/{nonExistentId}/no-show`
3. Assert: HTTP 404, body `{"error":{"code":"NSH-003", ...}}`

**Expected Result (PASS):** 404 with correct error code
**Expected Result (FAIL):** 500 or wrong error code

**Current Status:** 🔴 Not written

---

### NSH-TC-012 — Already-terminal (`NO_SHOW`) session re-marked → 409 NSH-002 (idempotency guard)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()` / `assertNotTerminal()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-SESSION-001` (UC95, reused) — invariant 2, "`NO_SHOW` is terminal — no transition out of it"; error code `NSH-002`

**Preconditions:** `FX-N11` (session already `session_status='NO_SHOW'`)

**Test Steps:**
1. Arrange: session already `NO_SHOW`
2. Act: call `service.markNoShow(SESSION_ID_1, makeMarkNoShowRequest(), EXPERT_USER_ID_1)`
3. Assert: throws `SessionTransitionException` (`NSH-002`); no duplicate event published

**Expected Result (PASS):** Re-marking a terminal session is rejected — matches the "No-show" terminal invariant; not silently treated as a no-op success
**Expected Result (FAIL):** Duplicate mark accepted, or state silently re-written (violates §6.5 invariant 2)

**Current Status:** 🔴 Not written

---

### NSH-TC-013 — Admin override/refund attempt via UC207 surface → 409 NSH-006 (boundary enforcement)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController` (boundary guard — no override endpoint exists)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-NSH-002` — override/refund delegated to UC-209/UC-210; error code `NSH-006`

**Preconditions:** `FX-N11` (session `NO_SHOW`), caller = `ADMIN_USER_ID`

**Test Steps:**
1. Arrange: session already `NO_SHOW`
2. Act: attempt an override/reversal call against the UC207 surface (e.g. a hypothetical `PATCH .../no-show-review/{id}/override` or reusing the mark endpoint to force a reversal)
3. Assert: request is rejected with `NSH-006`; no `consultation_disputes` or `refund_records` row is created by any UC207 code path; `session_status` remains `NO_SHOW`

**Expected Result (PASS):** UC207 code contains zero logic that creates disputes/refunds — the assertion is effectively "no such capability exists in this module"
**Expected Result (FAIL):** UC207 implements a reversal/refund action directly (violates C5/AP-CB-203, duplicates UC-209/UC-210)

**Current Status:** 🔴 Not written
**Implementation Note:** This test also functions as an architectural guard — if a future PR adds refund logic inside `ConsultationSessionService`, this test should catch the boundary violation.

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### NSH-TC-INT-001 — Notification/audit dispatch failure does not roll back a committed NO_SHOW

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `POST /sessions/{id}/no-show` with a failing notification dispatch
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSessionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-SESSION-003` (UC95, reused) — external-service failure must not corrupt session state; SRS E3 ("retry guidance and no duplicate unsafe action")

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically at Spring context start
- Seed: `FX-N01`/`FX-N09` (past waiting period), `FX-N02`, `FX-N03` inserted via JPA
- `FX-N12` — notification/event-listener mock configured to throw

**Test Steps:**
1. Seed booking + session + verified expert profile
2. Call `POST /api/v1/consultations/sessions/{sessionId}/no-show` with a valid Expert JWT, with the downstream notification handler simulated to throw
3. Assert response is still `200` (or the DB-committed state is `NO_SHOW` even if the async notification path fails independently)
4. Query the DB — assert `session_status = 'NO_SHOW'`, `ended_at IS NOT NULL`

**Expected Result (PASS):** `session_status = 'NO_SHOW'` regardless of notification-dispatch outcome — the state transition is not coupled to notification delivery success
**Expected Result (FAIL):** A notification failure causes the transaction to roll back the already-decided `NO_SHOW` status, or throws an unhandled 500 that leaves the session in an inconsistent state

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findById(SESSION_ID_1).orElseThrow();
assertThat(record.getSessionStatus()).isEqualTo("NO_SHOW");
assertThat(record.getEndedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

### NSH-TC-INT-002 — No-show evidence note persisted and retrievable (technical_log_json)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /sessions/{id}/no-show` → DB `technical_log_json` state
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSessionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §5.3` `Open-NSH-4` (proposed default: note stored in `technical_log_json`)

**Preconditions:** Same seed as `NSH-TC-INT-001`, no failure simulation

**Test Steps:**
1. Seed booking + session + verified expert profile (past waiting period)
2. Call `POST /api/v1/consultations/sessions/{sessionId}/no-show` with `{"reason": "Called 3 times, no answer."}`
3. Assert response `200`
4. Query `consultation_sessions.technical_log_json` for the row — assert it contains a `noShow` entry with `reason == "Called 3 times, no answer."`, `markedBy == EXPERT_USER_ID_1`

**Expected Result (PASS):** Evidence note retrievable from `technical_log_json`; `expert_summary` column untouched (reserved for UC96)
**Expected Result (FAIL):** Note lost, or written to the wrong column, or a new column/migration silently introduced

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### NSH-TC-WEB-001 — MarkNoShowDialog requires reason and calls API on submit (CB-188)

**Severity:** `MEDIUM`
**Feature Under Test:** `MarkNoShowDialog.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/MarkNoShowDialog.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS §5.1` Web Component, CB-188 mockup (required "Ghi chú / Bằng chứng vắng mặt *" textarea, irreversibility warning)

**Preconditions:** MSW mock server configured for `POST /api/v1/consultations/sessions/:id/no-show` → 200

**Test Steps:**
1. Render `<MarkNoShowDialog />` with a mocked TanStack Query client and MSW handler
2. Act: attempt submit with empty reason → assert submit button disabled/blocked, no API call made
3. Act: type a reason, click "Xác nhận vắng mặt" (`userEvent.click`)
4. Assert: mutation hook fires with the typed reason; UI shows the irreversibility/policy notice from CB-188 before submission is enabled

**Expected Result (PASS):** No API call with a blank reason; API called with the correct payload once reason is filled
**Expected Result (FAIL):** Submit allowed with blank reason (contradicts CB-188 "Bắt buộc"), or wrong endpoint/payload called

**Current Status:** 🔴 Not written

---

### NSH-TC-WEB-002 — NoShowReviewPage override action navigates to UC-209 flow, not a UC207 API call (CB-199)

**Severity:** `MEDIUM`
**Feature Under Test:** `NoShowReviewPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/admin/pages/NoShowReviewPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `ADR-NSH-002` boundary; CB-199 mockup ("Ghi đè / Hợp lệ (Hoàn tiền)" button)

**Preconditions:** MSW mock server configured for `GET /no-show-reviews` and `GET /{id}/no-show-review` → 200; no mock registered for any override/refund endpoint

**Test Steps:**
1. Render `<NoShowReviewPage />` with a seeded no-show review record
2. Act: click "Ghi đè / Hợp lệ (Hoàn tiền)"
3. Assert: the click triggers navigation/routing into the dispute-resolution flow (UC-209) — no HTTP call is made to any UC207 endpoint for this action
4. Act: click "Xác nhận Vắng mặt" (uphold)
5. Assert: this action calls only a read/audit-annotation path — no refund/reversal side effect

**Expected Result (PASS):** UI never calls a UC207 endpoint to perform the reversal; boundary from `ADR-NSH-002` is honored on the frontend too
**Expected Result (FAIL):** Frontend calls a UC207-owned endpoint to process the refund/override (duplicated dispute logic — AP-CB-203 surfacing in the UI layer)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `NSH-TC-001` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-002` | `ConsultationSessionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-003` | `ConsultationSessionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-004` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-005` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-006` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-007` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-008` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-009` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-010` | `ConsultationSessionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-011` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-012` | `ConsultationSessionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-013` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-INT-001` | `ConsultationSessionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-INT-002` | `ConsultationSessionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-WEB-001` | `MarkNoShowDialog.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `NSH-TC-WEB-002` | `NoShowReviewPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
// Added methods on ConsultationSessionService (UC95-owned class, UC207 delta only)
@Service
public class ConsultationSessionService implements IConsultationSessionService {

    // ... UC95 methods unchanged/already stubbed there ...

    @Override
    public ConsultationSessionResponse markNoShow(UUID sessionId, MarkNoShowRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Page<NoShowReviewResponse> listNoShowReviews(int page, int size, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public NoShowReviewResponse getNoShowReview(UUID sessionId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// ConsultationSessionPolicy — new method stub (pure logic, no throw-only stub possible;
// Red Gate for NSH-TC-002/010/012 uses a deliberately-wrong placeholder instead, e.g.
// "always return without checking", to prove the test fails for the RIGHT reason).
public class ConsultationSessionPolicy {
    public void assertWaitingPeriodElapsed(ConsultationBookingEntity booking, Instant now) {
        // Red Phase placeholder — deliberately does nothing (must make NSH-TC-002/010 FAIL
        // because no exception is thrown when one is expected)
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `NSH-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `NSH-TC-002` | policy no-op placeholder (no throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-003` | (pure policy, no stub — must implement state table) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-006` | DTO validation not yet wired | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-008` | route guard not yet wired | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-010` | policy no-op placeholder (no throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-012` | (pure policy, no stub — must implement terminal-state guard) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `NSH-TC-013` | endpoint does not exist yet | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS bất thường:** Dừng lại. Xác định root cause từ bảng trên (đặc biệt lưu ý
> `NSH-TC-002`/`NSH-TC-010`/`NSH-TC-012` — các test dùng policy no-op placeholder có nguy cơ
> Tautology cao nhất nếu assertion không thực sự kiểm tra exception).

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-207` đã được review và approve
- [ ] `UC-95 ManageConsultationSession` (`ConsultationSessionEntity/Service/Policy/Repository`) implemented and stable — **blocking**, UC207 extends these
- [ ] `ADR-NSH-001` waiting-period minutes (`Open-NSH-2`) confirmed by Product/Tech Lead
- [ ] `ADR-NSH-002` admin-review scope + UC-209 boundary confirmed
- [ ] `Open-NSH-4` no-show-note storage decision confirmed (proposed: `technical_log_json`)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Không cần migration mới (baseline scope) — xác nhận
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh
- [ ] Test coverage ≥ 80% lines cho các method mới trong `ConsultationSessionService`/`ConsultationSessionPolicy`
- [ ] Không có business logic trong Controller
- [ ] Không có PII/health-context note xuất hiện plaintext ngoài phạm vi cần thiết trong logs
- [ ] `session_status` literal `'NO_SHOW'` verified byte-for-byte identical to what UC95/UC209/UC210 consume (cross-document consistency check)
- [ ] Admin review DTO verified minimum-necessary (no raw health-record content) — BR-PRIVACY
- [ ] No override/refund logic present in UC207 code (`NSH-TC-013` green) — UC-209/UC-210 boundary intact

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw/no-op stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory pattern only)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS section)

### Suspension Criteria (Điều kiện tạm dừng)

- `UC-95` session lifecycle chưa sẵn sàng/stable
- Booking/payment service (`UC-75/76`) chưa sẵn sàng
- `Open-NSH-2`/`Open-NSH-4` chưa được Product/Tech Lead xác nhận trước khi viết assertion literal
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No migration in baseline scope — code-only rollback:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/MarkNoShowDialog.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/MarkNoShowDialog.test.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/admin/pages/NoShowReviewPage.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/admin/pages/NoShowReviewPage.test.tsx
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/mark_no_show_confirmation.dart

# Gap vẫn OPEN → giữ nguyên entry trong TDS §1.2/§11.1 (Open-NSH-1..4)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw/no-op stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes an edge other than `WAITING→NO_SHOW` (e.g. silently allows `IN_SESSION→NO_SHOW`) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic (e.g. waiting-period math inline in controller) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a service/type not present in `TDS §8` | ☐ | G-3 |
| AP-CB-201 | Parallel no-show entity | Test seeds/asserts a `NoShowEntity`/`NoShowRepository` instead of reusing `ConsultationSession*` (UC95) | ☐ | G-1 |
| AP-CB-202 | Forked lifecycle event | Test asserts a `NoShowMarked` event instead of the shared `ConsultationSessionStatusChanged` | ☐ | G-1 |
| AP-CB-203 | Refund/reversal in UC207 | Test asserts UC207 code creates/updates `consultation_disputes` or `refund_records` | ☐ | G-1 |
| AP-CB-204 | Hard-coded grace literal | Test/implementation hard-codes `plusMinutes(15)` inline instead of referencing the named `WAITING_PERIOD` constant | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Spec UC207 v1.0 — Draft. 17 test cases (3 CRITICAL, 6 HIGH, 8 MEDIUM). Requires Entry Criteria
(§6) satisfied — including `Open-NSH-2` (waiting-period minutes) and `Open-NSH-4` (no-show note
storage) confirmation — before Red Gate execution.*
