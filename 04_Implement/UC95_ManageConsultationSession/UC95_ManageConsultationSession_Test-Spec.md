# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC95 — Manage Consultation Session — Test Specification

**Document ID:** `FPT-EDU-TDD-CB-CONSULTATION-095`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft` *(reverted 2026-07-15 — see note below)*

<!-- 2026-07-15 AI Agent — Amelia (Dev Agent): GREEN — TC-COND-001/002/003/004/005/006/007/011
    implemented and passing via ConsultationSessionPolicyTest (SES-TC-001..010) and
    ConsultationSessionServiceImplTest (SES-TC-101..107). RED/deferred, honestly not
    implemented in this pass: TC-COND-008 (invalid direct-transition guard beyond the
    join/end boundary), TC-COND-009 (no-show reconciliation job), TC-COND-010 (explicit
    mid-session-disconnect non-transition test), TC-COND-012 (ConsultationSessionStatusChanged
    event emission — audit log used instead, see TDS changelog). Verified via
    `./mvnw test -Dtest=com.carebridge.backend.consultation.**`: 52/52 passed, 0 failures
    (consultation module only; Testcontainers-based integration test could not execute in
    this sandbox — no Docker — but compiles cleanly). -->

<!-- 2026-07-15 AI Agent — Technical Architect: DECOUPLED — reverted Approved → Draft.
    UC-144 chat no longer depends on consultation_sessions (see UC144_DirectConsultChat/).
    The tested code above (ConsultationSessionServiceImplTest, ConsultationSessionPolicyTest)
    is being deleted along with the now-unused service/controller/policy it exercised, since
    it has no remaining consumer. This spec is retained for history; re-open with a fresh
    Draft->Approval cycle if booking-tied session management is requested again. -->

**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC154_EstablishRealtimeCommunicationSession/UC154_EstablishRealtimeCommunicationSession_TDS.md` — reused ZegoCloud pattern
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.9 (L922-941) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, consent, audit mandates)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC95 |

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
| **Feature / Gap ID** | `UC-95` |
| **Module** | `Consultation — Session Management` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-095` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 4 "Real Providers And Admin Polish"` — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `Confidential` (session metadata); `consultation_messages.message_body` is `Sensitive-PII`-adjacent |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `UC-154 EstablishRealtimeCommunicationSession` (`IZegoCloudService`), `UC-75/76` booking/payment |
| **Downstream Consumers** | `UC-79 ReviewExpertAfterConsultation`, `UC-96 WriteConsultationSummary` |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TypeScript/Vite) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC95_ManageConsultationSession_TDS.md §17` (C1-C7), ADR-SESSION-001/002/003 |
| **Constraints Injected** | State-machine transition guard (C1), assigned+verified Expert ownership (C2), ZegoCloud delegation to UC154 (C3), no state mutation on ZegoCloud failure (C4), explicit-end-only COMPLETED transition (C5), token never persisted (C6), package/entity-exposure rules (C7) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-95 generic template does not name a `session_status` enum | `consultation_sessions.session_status varchar(30) NOT NULL DEFAULT 'WAITING'` (no CHECK constraint) | Tests assert against the application-level enum confirmed in TDS §ADR-SESSION-001: `WAITING/IN_SESSION/COMPLETED/NO_SHOW/CANCELLED` |
| L2 | UC154's TDS uses `consultationId` terminology, potentially implying booking-level granularity | Schema splits `consultation_bookings`/`consultation_sessions` 1:1 (`UNIQUE(booking_id)`) | Tests use `session_id` as the ZegoCloud `roomId`, per TDS §5.3 gap note 2, not `booking_id` |
| L3 | No SRS-stated numeric SLA | TDS §4.1 proposes `< 400ms` p99 as Open/proposed | Tests do not assert a hard SLA number; performance is out of unit/integration test scope (flagged for future load-test suite) |
| L4 | ZegoCloud failure handling ambiguous in SRS (only generic E3 wording) | TDS ADR-SESSION-003 mirrors UC154's "status NOT changed on failure" invariant | Tests explicitly assert `session_status` is unchanged after a simulated `ZegoCloudException` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Session Management bao gồm các layer:
├── Domain (ConsultationSessionPolicy — pure logic, no deps)
├── Application / Use Cases (ConsultationSessionService — mock JPA Repository + IZegoCloudService với Mockito)
├── Controller (ConsultationSessionController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full join/end/status-update flow)
└── Web (SessionRoomPage.tsx, consultationSessionApi.ts — Vitest + Testing Library, MSW for API mocking)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-95` (§3.2.1.9, L922-941) | Receives bookings, joins sessions, updates participation status |
| `ADR-SESSION-001` | State machine transitions, definitive `'COMPLETED'` terminal value |
| `ADR-SESSION-002` | Ownership — assigned, verified Expert only |
| `ADR-SESSION-003` | ZegoCloud failure/timeout/reconnect safety |
| `BR-RBAC` | Role/ownership-scoped access |
| `CB-CONSULTATION-IMP-095 §9/§10/§16` | API contract, error codes, authorization matrix |
| PDPA / BR-CONSULTATION | Auditable lifecycle state requirement |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Assigned, verified Expert joins WAITING session | `ConsultationSessionService.joinSession()` | `SES-TC-001` |
| TC-COND-002 | Non-assigned Expert attempts join | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `SES-TC-002` |
| TC-COND-003 | Unverified assigned Expert attempts join | `ConsultationSessionPolicy.assertIsAssignedExpert()` | `SES-TC-003` |
| TC-COND-004 | ZegoCloud SDK failure on join | `ConsultationSessionService.joinSession()` | `SES-TC-004` |
| TC-COND-005 | Retry after ZegoCloud failure succeeds | `ConsultationSessionService.joinSession()` (idempotency) | `SES-TC-005` |
| TC-COND-006 | Expert ends IN_SESSION session → COMPLETED | `ConsultationSessionService.endSession()` | `SES-TC-006` |
| TC-COND-007 | End attempted on non-IN_SESSION session | `ConsultationSessionPolicy.assertTransitionAllowed()` | `SES-TC-007` |
| TC-COND-008 | Invalid direct WAITING→COMPLETED transition | `ConsultationSessionPolicy.assertTransitionAllowed()` | `SES-TC-008` |
| TC-COND-009 | No-show reconciliation after grace period | `ConsultationSessionService` (reconciliation check) | `SES-TC-009` |
| TC-COND-010 | Mid-session disconnect does not auto-transition | `ConsultationSessionService` (no webhook-driven transition) | `SES-TC-010` |
| TC-COND-011 | Session not found | `ConsultationSessionController` | `SES-TC-011` |
| TC-COND-012 | Event emission on valid transition | `ConsultationSessionService` (ApplicationEventPublisher) | `SES-TC-012` |
| TC-COND-013 | Controller has no business logic (layer violation guard) | `ConsultationSessionController` | `SES-TC-INT-001` |
| TC-COND-014 | ZegoCloud token never persisted to DB | Integration — DB column scan | `SES-TC-INT-002` |
| TC-COND-015 | Web: SessionRoomPage renders join button and calls API | `SessionRoomPage.tsx` | `SES-TC-WEB-001` |
| TC-COND-016 | Web: 503 error surfaces retry guidance to user | `SessionRoomPage.tsx` | `SES-TC-WEB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role (assigned/non-assigned/unverified), session_status value | Covers each authorization/state class with one representative case |
| Boundary Value Analysis | 15-minute no-show grace period (14:59 vs 15:01) | Grace-period edge is a hard business rule (ADR-SESSION-003) |
| State Transition Testing | `session_status` FSM (§6.4 of TDS) | Core invariant of the whole module |
| Error Guessing | ZegoCloud SDK exception injection, DB timeout | External-service failure is explicitly in scope (SRS E3) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `consultation_bookings{status:'CONFIRMED', expert_profile_id: EXPERT-001}` | Happy path booking |
| `FX-002` | DB seed | `consultation_sessions{session_status:'WAITING', booking_id: FX-001}` | Initial session state |
| `FX-003` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status:'VERIFIED'}` | Assigned, verified Expert |
| `FX-004` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status:'PENDING'}` | Unverified Expert (negative case) |
| `FX-005` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned |
| `FX-006` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned |
| `FX-007` | Mock | `IZegoCloudService.generateToken()` returns `ZegoTokenDto{roomId, token:"04A...", expiresAt}` | Happy path token |
| `FX-008` | Mock | `IZegoCloudService.generateToken()` throws `ZegoCloudException` | Failure simulation |
| `FX-009` | DB seed | `consultation_sessions{session_status:'IN_SESSION', started_at: now()-30min}` | End-session precondition |
| `FX-010` | DB seed | `consultation_bookings{scheduled_start: now()-16min}` + session `WAITING` | No-show boundary (past grace) |
| `FX-011` | DB seed | `consultation_bookings{scheduled_start: now()-14min}` + session `WAITING` | No-show boundary (within grace) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// ConsultationSessionTestFactory.java
class ConsultationSessionTestFactory {

    static final UUID BOOKING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");

    static ConsultationBookingEntity makeBooking() {
        return makeBooking(b -> {});
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-000000000501"));
        booking.setStatus("CONFIRMED");
        booking.setScheduledStart(Instant.now());
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeSession() {
        return makeSession(s -> {});
    }

    static ConsultationSessionEntity makeSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus("WAITING");
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

### SES-TC-001 — Happy path: assigned verified Expert joins WAITING session

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SESSION-001` (state transition), `ADR-SESSION-002` (ownership) — `UC95_ManageConsultationSession_TDS.md §6.1, §6.4`

**Preconditions:**
- `FX-001` (booking, `CONFIRMED`), `FX-002` (session, `WAITING`), `FX-003` (verified Expert)
- `FX-007` (ZegoCloud mock returns success)

**Test Steps:**
1. Arrange: seed session via `ConsultationSessionTestFactory.makeSession()`, mock `IZegoCloudService.generateToken()` to return `FX-007`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: response contains `sessionStatus="IN_SESSION"`, `zegoToken` non-null; repository `save()` invoked once with `sessionStatus=IN_SESSION`, `startedAt` set

**Expected Result (PASS — hành vi đúng):**
- Session transitions `WAITING → IN_SESSION`; `communicationRoomId == sessionId.toString()`; token returned but not persisted

**Expected Result (FAIL — dấu hiệu lỗi):**
- Status not updated, or token appears in any entity field being persisted

**Current Status:** 🔴 Not written
**Implementation Note:** Only the FIRST successful join transitions the status (idempotency guard — see SES-TC-005).

---

### SES-TC-002 — Ownership violation: non-assigned Expert joins → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SESSION-002`, error code `SES-004` (`TDS §10`)

**Preconditions:**
- `FX-001`, `FX-002` seeded; caller = `OTHER_EXPERT_USER_ID` (not `expert_profiles.user_id` for the booking)

**Test Steps:**
1. Arrange: booking assigned to `EXPERT_PROFILE_ID_1` (→ `EXPERT_USER_ID_1`)
2. Act: call `service.joinSession(SESSION_ID_1, OTHER_EXPERT_USER_ID)`
3. Assert: throws `SessionAuthorizationException` with code `SES-004`

**Expected Result (PASS):** Exception thrown; no repository write; no ZegoCloud call made (fail fast before external call)
**Expected Result (FAIL):** Join succeeds, or ZegoCloud is called before the ownership check

**Current Status:** 🔴 Not written

---

### SES-TC-003 — Unverified assigned Expert attempts join → 403

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SESSION-002` — "Verified Expert" actor requirement, `TDS §1` Primary Actor field

**Preconditions:** `FX-004` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionAuthorizationException` (`SES-004`)

**Expected Result (PASS):** Rejected despite matching `user_id`, because `verification_status != 'VERIFIED'`
**Expected Result (FAIL):** Unverified expert allowed to join

**Current Status:** 🔴 Not written

---

### SES-TC-004 — ZegoCloud SDK failure on join → 503, session_status unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SESSION-003` (mirrors UC154 ADR-ZEGO-001), error code `SES-005`

**Preconditions:** `FX-008` (ZegoCloud mock throws)

**Test Steps:**
1. Arrange: `IZegoCloudService.generateToken()` throws `ZegoCloudException`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionServiceUnavailableException` (`SES-005`); repository `save()` never invoked

**Expected Result (PASS):** `session_status` remains `WAITING`; no partial state written
**Expected Result (FAIL):** Session status changed despite ZegoCloud failure, or exception swallowed silently

**Current Status:** 🔴 Not written

---

### SES-TC-005 — Retry after ZegoCloud failure succeeds (idempotent re-join)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService.joinSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SESSION-003` — retry safety, SRS Exception E3

**Preconditions:** First call throws (`FX-008`), second call succeeds (`FX-007`)

**Test Steps:**
1. Arrange: mock `generateToken()` to throw once, then succeed
2. Act: call `joinSession()` twice sequentially
3. Assert: first call throws `SES-005`; second call returns 200 with `sessionStatus=IN_SESSION`; `WAITING→IN_SESSION` transition occurs exactly once (on the successful call)

**Expected Result (PASS):** No duplicate/unsafe state transition; retry is safe
**Expected Result (FAIL):** Duplicate transition attempted, or second call also fails due to stuck state from the first

**Current Status:** 🔴 Not written

---

### SES-TC-006 — Expert ends IN_SESSION session → COMPLETED (definitive terminal value)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.endSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SESSION-001` — `'COMPLETED'` confirmed as definitive terminal value

**Preconditions:** `FX-009` (session `IN_SESSION`)

**Test Steps:**
1. Arrange: session `session_status='IN_SESSION'`
2. Act: call `service.endSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: response `sessionStatus == "COMPLETED"` (exact string, not `"ENDED"`/`"FINISHED"`); `endedAt` set; `IZegoCloudService.endSession()` invoked once

**Expected Result (PASS):** `session_status` literal value is `'COMPLETED'` — this exact string is what UC79/UC96 consume
**Expected Result (FAIL):** Any other terminal string used, breaking cross-document consistency

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important assertion in the whole consultation-domain batch — UC79 and UC96 both depend on this exact literal.

---

### SES-TC-007 — End attempted on non-IN_SESSION session → 409

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SESSION-001` §6.4 state machine, error code `SES-006`

**Preconditions:** Session `session_status='WAITING'`

**Test Steps:**
1. Arrange: session in `WAITING`
2. Act: call `service.endSession(SESSION_ID_1, EXPERT_USER_ID_1)`
3. Assert: throws `SessionTransitionException` (`SES-006`)

**Expected Result (PASS):** Rejected — only `IN_SESSION → COMPLETED` is a valid `endSession()` transition
**Expected Result (FAIL):** `WAITING` session incorrectly allowed to end

**Current Status:** 🔴 Not written

---

### SES-TC-008 — Invalid direct WAITING → COMPLETED transition rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-SESSION-001 §6.4` state machine — invariant 1

**Preconditions:** None (pure policy unit test)

**Test Steps:**
1. Arrange: `current='WAITING'`, `target='COMPLETED'`
2. Act: call `policy.assertTransitionAllowed("WAITING", "COMPLETED")`
3. Assert: throws `SessionTransitionException`

**Expected Result (PASS):** Direct skip-transition rejected — must pass through `IN_SESSION`
**Expected Result (FAIL):** Transition silently allowed

**Current Status:** 🔴 Not written

---

### SES-TC-009 — No-show reconciliation: WAITING past 15-min grace → NO_SHOW

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService` (no-show reconciliation check)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-SESSION-003` — 15-minute grace period (mirrors UC154 ADR-ZEGO-002)

**Preconditions:** `FX-010` (`scheduled_start = now()-16min`, `session_status='WAITING'`)

**Test Steps:**
1. Arrange: booking `scheduled_start` 16 minutes in the past, session still `WAITING`, `expert_joined_at` still null (via `started_at IS NULL`)
2. Act: trigger reconciliation check (e.g., on `getSession()` read path per TDS §11.3 Chặng 3)
3. Assert: session transitions to `NO_SHOW`

**Boundary case — FX-011 (14 min past, within grace):**
4. Arrange: `scheduled_start = now()-14min`
5. Act: same reconciliation check
6. Assert: session remains `WAITING` (grace period not yet exceeded)

**Expected Result (PASS):** Exactly at the 15-minute boundary, before → `WAITING` stays, after → `NO_SHOW`
**Expected Result (FAIL):** Off-by-one on the boundary, or no-show applied while Expert already joined

**Current Status:** 🔴 Not written
**Implementation Note:** Boundary Value Analysis (TDS-04) — test both 14:59 and 15:01 past `scheduled_start`.

---

### SES-TC-010 — Mid-session disconnect does not auto-transition

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService` (no webhook-driven auto-transition)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-SESSION-003` — "mid-session disconnect does not auto-transition"

**Preconditions:** Session `IN_SESSION`

**Test Steps:**
1. Arrange: session `IN_SESSION`; simulate a disconnect scenario (no explicit `endSession()` call made)
2. Act: assert no code path exists that transitions status without an explicit `POST /end` call (verified by absence of any ZegoCloud-webhook-consuming handler in the service)
3. Assert: `session_status` remains `IN_SESSION` after simulated disconnect

**Expected Result (PASS):** Only explicit `endSession()` changes status to `COMPLETED`
**Expected Result (FAIL):** Any implicit disconnect-triggered transition exists (violates C5/AP-CB-102)

**Current Status:** 🔴 Not written

---

### SES-TC-011 — Session not found → 404

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController` / `ConsultationSessionService.getSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** Error code `SES-003` (`TDS §10`)

**Preconditions:** No session with the given ID exists

**Test Steps:**
1. Arrange: `sessionRepository.findById(nonExistentId)` returns `Optional.empty()`
2. Act: `GET /api/v1/consultations/sessions/{nonExistentId}`
3. Assert: HTTP 404, body `{"error":{"code":"SES-003", ...}}`

**Expected Result (PASS):** 404 with correct error code
**Expected Result (FAIL):** 500 or wrong error code

**Current Status:** 🔴 Not written

---

### SES-TC-012 — `ConsultationSessionStatusChanged` event emitted on every valid transition

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService` (event emission)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §7.1` Domain Event Catalog

**Preconditions:** `FX-001`/`FX-002` seeded

**Test Steps:**
1. Arrange: mock `ApplicationEventPublisher`
2. Act: call `joinSession()` (triggers `WAITING→IN_SESSION`)
3. Assert: `eventPublisher.publishEvent()` called with a `ConsultationSessionStatusChanged` whose `payload.previousStatus="WAITING"`, `payload.newStatus="IN_SESSION"`

**Expected Result (PASS):** Event payload matches exact transition
**Expected Result (FAIL):** No event published, or wrong payload values

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### SES-TC-INT-001 — Controller layer contains no business logic

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionController` (layer discipline)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** `@WebMvcTest(ConsultationSessionController.class)`, `IConsultationSessionService` mocked

**Test Steps:**
1. Arrange: mock service to return a fixed `JoinSessionResponse`
2. Act: `POST /sessions/{id}/join`
3. Assert: controller delegates 1:1 to `service.joinSession()` — no conditional/state logic executed in the controller itself (verified via Mockito `verify(service, times(1)).joinSession(any(), any())` with no additional service calls)

**Expected Result (PASS):** Controller is a thin adapter
**Expected Result (FAIL):** Controller contains an `if` branch implementing business rules (layer violation, AP-AI-004)

**Current Status:** 🔴 Not written

---

### SES-TC-INT-002 — ZegoCloud token never persisted to DB (full integration)

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `POST /sessions/{id}/join` → DB state
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSessionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-SESSION-003`/`UC154 ADR-ZEGO-001` (reused) — token never persisted

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically at Spring context start
- Seed: `FX-001`, `FX-002`, `FX-003` inserted via JPA

**Test Steps:**
1. Seed booking + session + verified expert profile
2. Call `POST /api/v1/consultations/sessions/{sessionId}/join` with a valid Expert JWT
3. Assert response `200` with `zegoToken` present
4. Query `information_schema.columns` for `consultation_sessions` — assert no column name contains `token`
5. Query the actual row — assert no column value equals the returned `zegoToken` string

**Expected Result (PASS):**
- `SELECT column_name FROM information_schema.columns WHERE table_name='consultation_sessions' AND column_name LIKE '%token%'` returns 0 rows
- `session_status = 'IN_SESSION'`, `started_at IS NOT NULL`

**Expected Result (FAIL):** Any DB column stores the token value

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findById(SESSION_ID_1).orElseThrow();
assertThat(record.getSessionStatus()).isEqualTo("IN_SESSION");
assertThat(record.getStartedAt()).isNotNull();
// Reflection/column scan assertion for token absence — see SQL above, executed via JdbcTemplate
```

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### SES-TC-WEB-001 — SessionRoomPage renders join button and calls API on click

**Severity:** `MEDIUM`
**Feature Under Test:** `SessionRoomPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/pages/SessionRoomPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §5.1` Web Page component, `§9.1` API contract

**Preconditions:** MSW mock server configured for `POST /api/v1/consultations/sessions/:id/join` → 200

**Test Steps:**
1. Render `<SessionRoomPage />` with a mocked TanStack Query client and MSW handler
2. Act: `userEvent.click(screen.getByRole('button', { name: /join session/i }))`
3. Assert: mutation hook fires; UI transitions to "in session" state; no raw JPA/entity fields rendered

**Expected Result (PASS):** UI reflects `sessionStatus: "IN_SESSION"` after successful join
**Expected Result (FAIL):** Button click has no effect, or wrong endpoint called

**Current Status:** 🔴 Not written

---

### SES-TC-WEB-002 — 503 error surfaces retry guidance to Expert

**Severity:** `MEDIUM`
**Feature Under Test:** `SessionRoomPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/pages/SessionRoomPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-SESSION-003`, SRS Exception E3 ("retry guidance")

**Preconditions:** MSW mock returns `503 {code: "SES-005"}`

**Test Steps:**
1. Render `<SessionRoomPage />`, MSW configured to return 503 on join
2. Act: click join button
3. Assert: UI displays a retry-guidance message (not a generic crash) and a "Retry" action remains available

**Expected Result (PASS):** User-facing retry affordance shown, matching SRS E3
**Expected Result (FAIL):** Unhandled error / blank screen

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SES-TC-001` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-002` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-003` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-004` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-005` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-006` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-007` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-008` | `ConsultationSessionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-009` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-010` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-011` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-012` | `ConsultationSessionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-INT-001` | `ConsultationSessionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-INT-002` | `ConsultationSessionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-WEB-001` | `SessionRoomPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `SES-TC-WEB-002` | `SessionRoomPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ConsultationSessionService implements IConsultationSessionService {

    @Override
    public JoinSessionResponse joinSession(UUID sessionId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ConsultationSessionResponse endSession(UUID sessionId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ConsultationSessionResponse updateParticipationStatus(UUID sessionId, UpdateParticipationStatusRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ConsultationSessionResponse getSession(UUID sessionId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ConsultationSessionResponse> listAssignedSessions(UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SES-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-008` | (pure policy, no stub — must implement state table) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SES-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-095` đã được review và approve
- [ ] `UC-154 EstablishRealtimeCommunicationSession` (`IZegoCloudService`) implemented and stable — **blocking**
- [ ] `UC-75/76` booking/payment service implemented — **blocking**
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Không cần migration mới (baseline scope) — xác nhận
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh
- [ ] Test coverage ≥ 80% lines cho `ConsultationSessionService`
- [ ] Không có business logic trong Controller (SES-TC-INT-001 xanh)
- [ ] Không có PII/health-context message body xuất hiện plaintext trong logs
- [ ] `session_status` literal `'COMPLETED'` verified byte-for-byte identical to what UC79/UC96 consume (cross-document consistency check)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase (`./mvnw compile` no errors)
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory pattern only)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS section)

### Suspension Criteria (Điều kiện tạm dừng)

- `IZegoCloudService` (UC-154) chưa sẵn sàng
- Booking/payment service (UC-75/76) chưa sẵn sàng
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# No migration in baseline scope — code-only rollback:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/

# Gap vẫn OPEN → giữ nguyên entry trong TDS §1.2 Entry-Criteria Blocker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume terminal value khác `'COMPLETED'` | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |
| AP-CB-101 | Re-inventing ZegoCloud integration | Test asserts a locally-implemented token generator instead of delegating to `IZegoCloudService` | ☐ | G-1 |
| AP-CB-102 | Auto-completing session on disconnect | Test asserts a webhook-driven auto-transition to `COMPLETED`/`NO_SHOW` | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Spec UC95 v1.0 — Draft. 16 test cases (4 CRITICAL, 6 HIGH, 4 MEDIUM). Requires Entry Criteria (§6) satisfied before Red Gate execution.*
