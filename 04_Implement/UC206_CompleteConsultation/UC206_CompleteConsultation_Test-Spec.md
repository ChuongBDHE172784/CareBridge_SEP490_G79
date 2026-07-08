# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC206 — Complete Consultation — Test Specification

**Document ID:** `FPT-EDU-TDD-CB-CONSULTATION-206`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (consultation_sessions L898-909)
- `04_Implement/UC206_CompleteConsultation/UC206_CompleteConsultation_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — upstream owner of session state machine, ownership check, and `ConsultationSessionStatusChanged` event
- `04_Implement/UC96_WriteConsultationSummary/UC96_WriteConsultationSummary_TDS.md` — downstream summary-eligibility gate
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.5 (UC-206, L4431-4450) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, audit mandates, package convention)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC206 |

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
| **Feature / Gap ID** | `UC-206` |
| **Module** | `Consultation — Session Completion` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-206` |
| **Priority** | 🟡 P2 *(SRS L4444: Priority Medium)* |
| **Sprint** | `Sprint 4 "Real Providers And Admin Polish"` — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `Confidential` (completion unlocks `Sensitive-PII` summary (UC-96) + payment/commission (UC-127)) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` (exact BRs SRS L4446 lists for UC-206) |
| **Upstream Dependencies** | `UC-95 Manage Consultation Session` (`ConsultationSessionService`, `ConsultationSessionPolicy`, `ConsultationSessionEntity`, `ConsultationSessionStatusChanged` event) |
| **Downstream Consumers** | `UC-96 Write Consultation Summary`, `UC-79 Review Expert After Consultation`, `UC-127 Calculate Revenue and Commission` |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TS/Vite) + Mobile (Flutter, mockup CB-185) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC206_CompleteConsultation_TDS.md §17` (C1-C8); reused `ADR-SESSION-001/002/003` (UC95) |
| **Constraints Injected** | State-transition guard IN_SESSION→COMPLETED only (C1), assigned+verified Expert ownership reuse (C2), scoped mutation session_status/ended_at only — never expert_summary (C3), Zego teardown must not revert state (C4), reuse UC95 event published post-commit (C5), no auto-complete on disconnect (C6), no invented minimum duration (C7), extend UC95 service not new entity (C8) |
| **Model** | `Claude Opus 4.8` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, use `V1__init_schema.sql` and approved migrations as the final persistence oracle; ERD is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-206 (generic template) does not name a `session_status` enum or the completion transition | `consultation_sessions.session_status varchar(30) DEFAULT 'WAITING'` (no CHECK); state machine owned by UC95 `ADR-SESSION-001` | Tests assert the exact reused enum + the single edge `IN_SESSION → COMPLETED`; never a direct `WAITING → COMPLETED` |
| L2 | UC95 §8.1 sketches `endSession(...)`; UC-206 is a separate use case for the same transition | Both `Draft`; TDS §1.2 reconciles to canonical `completeSession(...)` / `POST .../complete` | Tests target `completeSession(...)` and `POST /sessions/{id}/complete`; do NOT create a second divergent method |
| L3 | SRS considers a possible "cannot complete too quickly" abuse rule | No SRS/BR/mockup source defines any minimum-duration value; `ADR-COMPLETE-002` is `Open` | Tests do NOT assert any minimum-duration threshold; `assertMinimumDuration()` is a no-op — a zero-elapsed completion is allowed |
| L4 | Ambiguity on whether completion also writes the summary / touches other lifecycle fields | TDS §6.4 invariant 2: completion mutates ONLY `session_status`+`ended_at`(+`updated_at`); `expert_summary` is UC-96's scope | Tests assert `expert_summary`/`started_at` are byte-for-byte unchanged after completion |
| L5 | "within 24 hours" summary window shown in mockup CB-185 | UI copy only; no SRS/BR enforcement rule | Tests do NOT assert any 24h timer/deadline behavior |
| L6 | SRS lists no admin actor for UC-206 (unlike UC-207 which adds System Admin) | SRS L4436: Primary Actor "Verified Expert", Secondary "None" | Tests assert SYSTEM_ADMIN cannot complete via this endpoint (403) — admin-forced closure belongs to UC-207 |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` and approved Flyway migrations in the test basis whenever database schema facts or persistence side effects are part of the oracle.

### TDS-01 — Scope / Phạm vi

```
Consultation Session Completion (UC-206) bao gồm các layer:
├── Domain (ConsultationSessionPolicy — reused guards, pure logic)
├── Application / Services (ConsultationSessionService.completeSession — mock JPA Repository + IZegoCloudService + ApplicationEventPublisher với Mockito)
├── Controller (ConsultationSessionController.completeSession — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL — full complete flow + DB-state + event assertions)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-206` (§3.3.14.5, L4431-4450) | Verified Expert ends session; status changes for summary/review/reconciliation |
| `ADR-SESSION-001` (UC95, reused) | State machine; only `IN_SESSION → COMPLETED`; `'COMPLETED'` definitive terminal value |
| `ADR-SESSION-002` (UC95, reused) | Ownership — assigned, verified Expert only |
| `ADR-SESSION-003` (UC95, reused) | ZegoCloud failure must not mutate/revert authoritative state |
| `ADR-COMPLETE-001` (UC206) | Completion trigger + downstream fan-out event |
| `ADR-COMPLETE-002` (UC206, Open) | No minimum-duration enforcement |
| `BR-RBAC`, `BR-CONSULTATION` (SRS L4446) | Role/ownership-scoped access; auditable lifecycle state |
| `CB-CONSULTATION-IMP-206 §9/§10/§16` | API contract, `COMP-0xx` error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Assigned verified Expert completes IN_SESSION session | `ConsultationSessionService.completeSession()` | `COMP-TC-001` |
| TC-COND-002 | Complete attempted from WAITING (never started) | `ConsultationSessionPolicy.assertTransitionAllowed()` | `COMP-TC-002` |
| TC-COND-003 | Complete attempted on already-terminal session | `ConsultationSessionPolicy.assertTransitionAllowed()` | `COMP-TC-003` |
| TC-COND-004 | Non-assigned Expert attempts complete | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `COMP-TC-004` |
| TC-COND-005 | Assigned but unverified Expert attempts complete | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `COMP-TC-005` |
| TC-COND-006 | Event emitted with correct old/new status payload | `ConsultationSessionService` (ApplicationEventPublisher) | `COMP-TC-006` |
| TC-COND-007 | Completion does not mutate `expert_summary`/`started_at` | `ConsultationSessionService.completeSession()` | `COMP-TC-007` |
| TC-COND-008 | ZegoCloud teardown failure does not revert COMPLETED | `ConsultationSessionService.completeSession()` | `COMP-TC-008` |
| TC-COND-009 | Session not found | `ConsultationSessionController`/`Service` | `COMP-TC-009` |
| TC-COND-010 | No minimum-duration block (zero-elapsed completion allowed) | `ConsultationSessionPolicy.assertMinimumDuration()` (no-op) | `COMP-TC-010` |
| TC-COND-011 | Controller has no business logic (layer discipline) | `ConsultationSessionController.completeSession()` | `COMP-TC-INT-001` |
| TC-COND-012 | Downstream eligibility unlocked at boundary — UC-96/UC-79/UC-127 gate passes only after COMPLETED | Integration — DB `session_status` + event | `COMP-TC-INT-002` |
| TC-COND-013 | SYSTEM_ADMIN cannot complete via this endpoint | `ConsultationSessionController` auth | `COMP-TC-013` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | `session_status` FSM edge `IN_SESSION → COMPLETED` (and rejected edges) | Core invariant of the feature |
| Equivalence Partitioning | Source state {WAITING, IN_SESSION, COMPLETED/NO_SHOW/CANCELLED}; role {assigned-verified, assigned-unverified, non-assigned, admin} | One representative per class |
| Error Guessing | ZegoCloud teardown exception injection; DB write failure | External-service/DB failure explicitly in scope (SRS E3, ADR-SESSION-003) |
| Boundary Value Analysis | Downstream-eligibility boundary (immediately before vs after COMPLETED) | Confirms the gate flips exactly on the transition |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `consultation_bookings{status:'CONFIRMED', expert_profile_id: EXPERT-001}` | Assigned booking |
| `FX-002` | DB seed | `consultation_sessions{session_status:'IN_SESSION', started_at: now()-30min, ended_at: null, expert_summary: null}` | Completable session |
| `FX-003` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status:'VERIFIED'}` | Assigned, verified Expert |
| `FX-004` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status:'PENDING'}` | Assigned but unverified Expert |
| `FX-005` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth — assigned Expert |
| `FX-006` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth — non-assigned Expert |
| `FX-007` | JWT | `{sub: 'USER-ADMIN-001', role: 'SYSTEM_ADMIN'}` | Auth — admin (must be denied) |
| `FX-008` | DB seed | `consultation_sessions{session_status:'WAITING'}` | Wrong-state (never started) |
| `FX-009` | DB seed | `consultation_sessions{session_status:'COMPLETED', ended_at: now()-1h}` | Already-terminal case |
| `FX-010` | DB seed | `consultation_sessions{session_status:'IN_SESSION', started_at: now(), ended_at: null}` | Zero-elapsed completion (no min-duration) |
| `FX-011` | Mock | `IZegoCloudService.endSession()` returns void (success) | Happy teardown |
| `FX-012` | Mock | `IZegoCloudService.endSession()` throws `ZegoCloudException` | Teardown-failure simulation |
| `FX-013` | DB seed | `consultation_sessions{session_status:'IN_SESSION', expert_summary:'PRE-EXISTING NOTE', started_at: T0}` | Boundary — assert summary/started_at unchanged |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// CompleteConsultationTestFactory.java
class CompleteConsultationTestFactory {

    static final UUID BOOKING_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1    = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    static final UUID ADMIN_USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000403");
    static final Instant STARTED_AT       = Instant.parse("2026-07-03T10:00:00Z");

    static ConsultationBookingEntity makeBooking() {
        return makeBooking(b -> {});
    }
    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-000000000501"));
        booking.setStatus("CONFIRMED");
        overrides.accept(booking);
        return booking;
    }

    // Baseline: an IN_SESSION session ready to be completed (FX-002)
    static ConsultationSessionEntity makeInSessionSession() {
        return makeSession(s -> {
            s.setSessionStatus("IN_SESSION");
            s.setStartedAt(STARTED_AT);
            s.setEndedAt(null);
            s.setExpertSummary(null);
        });
    }
    static ConsultationSessionEntity makeSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus("IN_SESSION");
        session.setStartedAt(STARTED_AT);
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
}
```

---

### COMP-TC-001 — Happy path: assigned verified Expert completes IN_SESSION session

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-COMPLETE-001` (completion flow), `ADR-SESSION-001` (`'COMPLETED'` definitive value) — `UC206 TDS §6.1, §6.4`

**Preconditions:**
- `FX-001` (booking `CONFIRMED`), `FX-002` (session `IN_SESSION`), `FX-003` (verified Expert), `FX-011` (Zego teardown success)

**Test Steps:**
1. Arrange: `sessionRepository.findById(SESSION_ID_1)` returns `makeInSessionSession()`; booking returns assigned-to `EXPERT_PROFILE_ID_1`; mock `IZegoCloudService.endSession()` no-op
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: response `sessionStatus == "COMPLETED"` (exact literal), `endedAt` non-null; `repository.save()` called once with `sessionStatus='COMPLETED'` and `endedAt != null`

**Expected Result (PASS — hành vi đúng):**
- Session transitions `IN_SESSION → COMPLETED`; `ended_at` set to a non-null instant; literal is `'COMPLETED'` (not `'ENDED'`/`'FINISHED'`)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Status unchanged, wrong terminal literal, or `ended_at` left null

**Current Status:** 🔴 Not written
**Implementation Note:** `'COMPLETED'` must match byte-for-byte the value UC-96/UC-79/UC-127 gate on.

---

### COMP-TC-002 — Wrong-state rejection: complete from WAITING → 409 COMP-005

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SESSION-001` (no direct `WAITING→COMPLETED`), error code `COMP-005` (`UC206 TDS §10`)

**Preconditions:** `FX-008` (session `WAITING`), assigned verified Expert

**Test Steps:**
1. Arrange: session `session_status='WAITING'`
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionTransitionException` with code `COMP-005`; `repository.save()` never called; no event published

**Expected Result (PASS):** Rejected — a never-started session cannot be completed; no state write, no event
**Expected Result (FAIL):** `WAITING` session allowed to complete (invariant 1 violation)

**Current Status:** 🔴 Not written

---

### COMP-TC-003 — Wrong-state rejection: complete on already-terminal session → 409 COMP-002

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SESSION-001` (terminal states are final), error code `COMP-002` (`UC206 TDS §10`)

**Preconditions:** `FX-009` (session already `COMPLETED`), assigned verified Expert

**Test Steps:**
1. Arrange: session `session_status='COMPLETED'`, `ended_at` already set
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionConflictException` (`COMP-002`); no second `save()`; no duplicate event; original `ended_at` unchanged

**Expected Result (PASS):** Re-completing a terminal session is rejected (not a silent 200), preventing duplicate downstream fan-out
**Expected Result (FAIL):** Completion re-applied / duplicate `ConsultationSessionStatusChanged` emitted

**Current Status:** 🔴 Not written
**Implementation Note:** Also covers `NO_SHOW`/`CANCELLED` source states (parameterized) — all terminal → `COMP-002`.

---

### COMP-TC-004 — Ownership denied: non-assigned Expert → 403 COMP-004

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SESSION-002` (reused), error code `COMP-004` (`UC206 TDS §10, §16`)

**Preconditions:** `FX-002` (IN_SESSION) assigned to `EXPERT_PROFILE_ID_1`; caller = `OTHER_EXPERT_USER_ID`

**Test Steps:**
1. Arrange: booking assigned to `EXPERT_PROFILE_ID_1` (→ `EXPERT_USER_ID_1`)
2. Act: `service.completeSession(SESSION_ID_1, OTHER_EXPERT_USER_ID)`
3. Assert: throws `SessionAuthorizationException` (`COMP-004`); ownership checked BEFORE any state write; no `save()`, no event

**Expected Result (PASS):** Rejected fail-fast; zero side effects
**Expected Result (FAIL):** Non-owner able to complete, or state mutated before the ownership check

**Current Status:** 🔴 Not written

---

### COMP-TC-005 — Ownership denied: assigned but unverified Expert → 403 COMP-004

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SESSION-002` — "Verified Expert" actor (SRS L4436), error code `COMP-004`

**Preconditions:** `FX-004` (assigned Expert with `verification_status='PENDING'`)

**Test Steps:**
1. Arrange: matching `user_id` but `verification_status='PENDING'`
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionAuthorizationException` (`COMP-004`)

**Expected Result (PASS):** Rejected despite matching `user_id`, because not `VERIFIED`
**Expected Result (FAIL):** Unverified Expert allowed to complete

**Current Status:** 🔴 Not written

---

### COMP-TC-006 — `ConsultationSessionStatusChanged` emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService.completeSession()` (event emission)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC206 TDS §7` (reused UC95 event), `ADR-COMPLETE-001`

**Preconditions:** `FX-001`/`FX-002`/`FX-003` seeded; `ApplicationEventPublisher` mocked

**Test Steps:**
1. Arrange: mock `ApplicationEventPublisher`
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: `eventPublisher.publishEvent()` called once with a `ConsultationSessionStatusChanged` whose `payload.previousStatus == "IN_SESSION"`, `payload.newStatus == "COMPLETED"`, `payload.sessionId == SESSION_ID_1`, `payload.bookingId == BOOKING_ID_1`, `payload.endedAt != null`

**Expected Result (PASS):** Exactly one event, payload matches the transition; (integration variant asserts publish happens after commit)
**Expected Result (FAIL):** No event, wrong payload values, or a newly-invented event type instead of the reused UC95 event

**Current Status:** 🔴 Not written
**Implementation Note:** Must reuse UC95's `ConsultationSessionStatusChanged` — a new event class is AP-CB-201/AP-AI-005.

---

### COMP-TC-007 — Completion does NOT mutate `expert_summary`/`started_at` (write-scope boundary)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC206 TDS §6.4 invariant 2`, constraint C3

**Preconditions:** `FX-013` (session `IN_SESSION`, `expert_summary='PRE-EXISTING NOTE'`, `started_at=T0`)

**Test Steps:**
1. Arrange: session with a pre-existing `expert_summary` and a fixed `started_at`
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: the saved entity has `expertSummary == "PRE-EXISTING NOTE"` (unchanged) and `startedAt == T0` (unchanged); only `sessionStatus` and `endedAt` differ

**Expected Result (PASS):** `expert_summary` and `started_at` byte-for-byte identical after completion
**Expected Result (FAIL):** Completion path overwrites/clears `expert_summary` or rewrites `started_at` (crosses into UC-96/UC-95 scope — AP-CB-202)

**Current Status:** 🔴 Not written

---

### COMP-TC-008 — ZegoCloud teardown failure does NOT revert COMPLETED

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-SESSION-003` (reused — realtime layer never gates authoritative state), constraint C4

**Preconditions:** `FX-002` (IN_SESSION), `FX-012` (`IZegoCloudService.endSession()` throws)

**Test Steps:**
1. Arrange: state persist succeeds; mock `IZegoCloudService.endSession()` to throw `ZegoCloudException`
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: the session is still persisted as `COMPLETED` with `ended_at` set; the teardown failure is caught/logged; the completion still returns 200 with `sessionStatus='COMPLETED'`; the event is still published

**Expected Result (PASS):** `COMPLETED` state persists; teardown failure is non-fatal (persist first, best-effort teardown after)
**Expected Result (FAIL):** Teardown exception rolls back / reverts / blocks the `COMPLETED` state (AP-CB-203)

**Current Status:** 🔴 Not written

---

### COMP-TC-009 — Session not found → 404 COMP-003

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController`/`ConsultationSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/CompleteConsultationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** error code `COMP-003` (`UC206 TDS §10`)

**Preconditions:** No session with the given ID exists

**Test Steps:**
1. Arrange: `sessionRepository.findById(nonExistentId)` returns `Optional.empty()`
2. Act: `POST /api/v1/consultations/sessions/{nonExistentId}/complete`
3. Assert: HTTP 404, body `{"error":{"code":"COMP-003", ...}}`

**Expected Result (PASS):** 404 with correct error code
**Expected Result (FAIL):** 500 or wrong error code

**Current Status:** 🔴 Not written

---

### COMP-TC-010 — No minimum-duration block: zero-elapsed completion allowed

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionPolicy.assertMinimumDuration()` (no-op, ADR-COMPLETE-002 Open)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/CompleteConsultationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-COMPLETE-002` (Open — no enforcement), constraint C7

**Preconditions:** `FX-010` (`started_at = now()`, `IN_SESSION`)

**Test Steps:**
1. Arrange: session started essentially now (elapsed ≈ 0)
2. Act: `service.completeSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: completion succeeds → `COMPLETED` (no exception, no minimum-duration rejection)

**Expected Result (PASS):** Zero-elapsed completion succeeds — no invented threshold blocks it
**Expected Result (FAIL):** An implementation invents a minimum-duration rule and rejects (violates C7 / ADR-COMPLETE-002)

**Current Status:** 🔴 Not written
**Implementation Note:** Guards against AP-AI-003 (implicit decision inventing a threshold not in any source).

---

### COMP-TC-013 — SYSTEM_ADMIN cannot complete via this endpoint → 403

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController.completeSession()` authorization
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/CompleteConsultationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `UC206 TDS §16` (Auth Matrix), SRS L4436 (Primary Actor: Verified Expert only; no admin actor for UC-206)

**Preconditions:** `FX-007` (admin JWT), session `IN_SESSION`

**Test Steps:**
1. Arrange: caller has role `SYSTEM_ADMIN`
2. Act: `POST /sessions/{id}/complete`
3. Assert: 403 (`COMP-004`) — admin is not the assigned Verified Expert; admin-forced closure is UC-207's scope, not UC-206

**Expected Result (PASS):** Admin denied at this endpoint
**Expected Result (FAIL):** Admin allowed to complete (scope creep into UC-207)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### COMP-TC-INT-001 — Controller layer contains no business logic

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController.completeSession()` (layer discipline)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/CompleteConsultationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CLAUDE.md` (Controller: validation + mapping only), constraint C8

**Preconditions:** `@WebMvcTest(ConsultationSessionController.class)`, `IConsultationSessionService` mocked

**Test Steps:**
1. Arrange: mock service to return a fixed `ConsultationSessionResponse{sessionStatus='COMPLETED'}`
2. Act: `POST /sessions/{id}/complete`
3. Assert: controller delegates 1:1 to `service.completeSession()` — `verify(service, times(1)).completeSession(any(), any())`; no conditional/state logic in the controller

**Expected Result (PASS):** Controller is a thin adapter
**Expected Result (FAIL):** Controller contains an `if` implementing a business rule (AP-AI-004)

**Current Status:** 🔴 Not written

---

### COMP-TC-INT-002 — Full flow: completion unlocks downstream eligibility (DB + event boundary)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /sessions/{id}/complete` → DB state + published event
**Test File:** `src/test/java/com/carebridge/backend/consultation/CompleteConsultationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-COMPLETE-001`, `UC96 §ADR-SUMMARY-001` (summary gate = `session_status='COMPLETED'`)

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`); Flyway applied at context start
- Seed `FX-001`, `FX-002` (IN_SESSION), `FX-003` (verified Expert)
- An `ApplicationEvents` recorder (`@RecordApplicationEvents`) capturing published events

**Test Steps:**
1. **Before:** assert the UC-96 summary-eligibility gate would REJECT (session `IN_SESSION` ≠ `COMPLETED`)
2. Call `POST /api/v1/consultations/sessions/{sessionId}/complete` with the verified Expert JWT
3. Assert response `200`, body `sessionStatus='COMPLETED'`
4. **After:** query the row — `session_status='COMPLETED'`, `ended_at IS NOT NULL`, `expert_summary IS NULL` (unchanged)
5. Assert exactly one `ConsultationSessionStatusChanged` recorded with `newStatus='COMPLETED'`
6. **Boundary:** assert the UC-96 summary-eligibility gate (`session_status == 'COMPLETED'`) now PASSES for the same session

**Expected Result (PASS):**
- Gate flips exactly on the transition: rejected before, allowed after
- `session_status='COMPLETED'`, `ended_at` set, `expert_summary` untouched
- One event with the correct payload

**Expected Result (FAIL):** Gate does not flip, `expert_summary` mutated, or missing/duplicate event

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findById(SESSION_ID_1).orElseThrow();
assertThat(record.getSessionStatus()).isEqualTo("COMPLETED");
assertThat(record.getEndedAt()).isNotNull();
assertThat(record.getExpertSummary()).isNull(); // completion never writes summary
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `COMP-TC-001` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-002` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-003` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-004` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-005` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-006` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-007` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-008` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-009` | `CompleteConsultationControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-010` | `CompleteConsultationServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-013` | `CompleteConsultationControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-INT-001` | `CompleteConsultationControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `COMP-TC-INT-002` | `CompleteConsultationIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với stub throw. Mọi test PHẢI FAIL.
> Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
// Added to the EXISTING ConsultationSessionService (UC95) — do NOT create a new service.
@Override
public ConsultationSessionResponse completeSession(UUID sessionId, UUID currentUserId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `COMP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `COMP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `COMP-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause. Rewrite test với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-206` đã được review và approve
- [ ] **BLOCKING:** UC-95 implemented — `ConsultationSessionService`/`Policy`/`Entity` + `ConsultationSessionStatusChanged` event exist
- [ ] §1.2 naming reconciliation confirmed (`completeSession`/`/complete` canonical)
- [ ] Logic Issues (Section 2) confirmed with Principal Architect
- [ ] Không cần migration mới (baseline scope) — xác nhận
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh; `flutter test` xanh (Mobile confirmation sheet)
- [ ] Test coverage ≥ 80% lines cho the `completeSession()` code path
- [ ] Không có business logic trong Controller (COMP-TC-INT-001 xanh)
- [ ] `session_status` literal `'COMPLETED'` verified byte-for-byte identical to what UC-79/UC-96/UC-127 consume
- [ ] Completion path proven to never mutate `expert_summary`/`started_at` (COMP-TC-007 xanh)
- [ ] Không có PII/health-context content xuất hiện plaintext trong logs

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại (`./mvnw compile` no errors); reuses UC95 classes, no new event/service/entity
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory pattern only)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS section)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-95 (session module) chưa sẵn sàng
- §1.2 naming reconciliation chưa được Tech Lead chốt
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# No migration in scope — code-only rollback:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong TDS §1.2 (naming reconciliation) và §11.1 (UC-95 blocker)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes terminal value ≠ `'COMPLETED'`, or invents a minimum-duration threshold | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a NEW event/service/entity instead of reusing UC95's | ☐ | G-3 |
| AP-CB-201 | Auto-completing session on disconnect | Test asserts a webhook/disconnect-driven auto-transition to `COMPLETED` | ☐ | G-1 |
| AP-CB-202 | Completion mutating summary/lifecycle fields | Test tolerates `completeSession()` writing `expert_summary`/`started_at` | ☐ | G-1 |
| AP-CB-203 | Realtime layer gating authoritative state | Test asserts a Zego teardown failure reverts `COMPLETED` | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Spec UC206 v1.0 — Draft. 13 test cases (4 CRITICAL, 5 HIGH, 4 MEDIUM),
covering happy completion, wrong-state rejection from WAITING and from
terminal, ownership denial (non-assigned + unverified + admin), event payload,
write-scope boundary, Zego-failure safety, no-minimum-duration, layer
discipline, and the downstream-eligibility boundary. Requires Entry Criteria
(§6) — notably UC-95 implemented — before Red Gate execution.*
