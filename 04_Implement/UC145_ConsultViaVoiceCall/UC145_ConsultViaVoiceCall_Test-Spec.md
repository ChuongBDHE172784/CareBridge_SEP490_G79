# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC145 — Consult via Voice Call — Test Specification

**Document ID:** `FPT-EDU-TDD-CB-CONSULTATION-145`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC145_ConsultViaVoiceCall/UC145_ConsultViaVoiceCall_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — reused session lifecycle owner
- `04_Implement/UC154_EstablishRealtimeCommunicationSession/UC154_EstablishRealtimeCommunicationSession_TDS.md` — reused ZegoCloud pattern (via UC95)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.5.4 (L3594-3613) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, consent, audit mandates)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.dart`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC145 |

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
| **Feature / Gap ID** | `UC-145` |
| **Module** | `Consultation — Voice Call` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-145` |
| **Priority** | 🟠 P1 (SRS Priority: Medium) |
| **Sprint** | `Sprint 2` — TV4-Lâm |
| **Milestone** | Sprint 2 |
| **Data Classification** | `Confidential` (session metadata); no audio stream persisted (ADR-VOICE-002) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `UC-95 ManageConsultationSession` (`joinSession()`, entities, policy), `UC-154 EstablishRealtimeCommunicationSession` (`IZegoCloudService`, via UC95) |
| **Downstream Consumers** | `UC-146 ConsultViaVideoCall` (reuses this feature's `SessionMode`/`sessionMode` mechanism) |
| **Platform** | Backend (Java 21/Spring Boot) + Mobile (Flutter/Dart, Expert App) — Web explicitly out of scope per SRS Platform field (see TDS §1.4) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC145_ConsultViaVoiceCall_TDS.md §17` (C1-C7), ADR-VOICE-001/002, reused ADR-SESSION-001/002/003 (UC95) |
| **Constraints Injected** | Reuse UC95's `joinSession()`/Policy/Repository (C1), additive `sessionMode` parameter via overload (C2), `sessionMode` never gates state machine (C3), assigned+verified Expert ownership unchanged (C4), ZegoCloud delegation to UC154 via UC95 (C5), no audio persistence (C6), no state mutation on ZegoCloud failure (C7) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-145 generic template does not name a `sessionMode` mechanism at all | TDS ADR-VOICE-001 introduces `sessionMode` as an additive query parameter on UC95's existing `joinSession()`, not a new endpoint | Tests call the SAME `joinSession()` method signature UC95 defines, with an added `SessionMode` argument — never a new controller method |
| L2 | SRS text does not specify whether voice call requires a distinct backend contract | TDS §1.1 confirms it is a thin, additive variant (RG-3 finding) | Tests assert `ConsultationSessionPolicy.assertIsAssignedExpert()` is called with IDENTICAL semantics to UC95 — no voice-specific bypass or relaxation exists |
| L3 | No SRS-stated numeric SLA for voice-call join | TDS §4.1 proposes `< 400ms` p99 as Open/inherited from UC95 | Tests do not assert a hard SLA number; performance is out of unit/integration test scope |
| L4 | SRS "Platform: Expert App" (L3610) vs UC146's "Expert App / Expert Portal" (L3631) — a real difference, not silently reconciled | TDS §1.4 marks this Open, defaults to Mobile-only scope for UC145 | Web test cases are explicitly OUT OF SCOPE for this Test-Spec (see TDS-01 scope below); only Backend + Mobile are covered |
| L5 | `session_status` enum values not defined in SRS text | Confirmed by UC95 ADR-SESSION-001: `WAITING/IN_SESSION/COMPLETED/NO_SHOW/CANCELLED` — UC145 does not add or rename any value | Tests reuse this exact enum; no voice-specific status introduced |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Voice Call (UC145) bao gồm các layer:
├── Domain (SessionMode enum — pure value object, no deps)
├── Application / Use Cases (ConsultationSessionService.joinSession() overload —
│   mock JPA Repository + IZegoCloudService với Mockito; reuses UC95's mocked
│   collaborators, only the sessionMode pass-through path is new)
├── Controller (ConsultationSessionController — @WebMvcTest, mock Service;
│   only the new optional query-param binding is new test surface)
├── Integration (Testcontainers PostgreSQL — join flow persists sessionMode
│   into technical_log_json, verifies no new column)
└── Mobile (voice_call_screen.dart — flutter_test + mocktail, mocked API client)

OUT OF SCOPE for this Test-Spec: Web UI (per TDS §1.4 Platform Discrepancy —
Expert App only per exact SRS text).
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-145` (§3.3.5.4, L3594-3613) | Verified Expert joins a voice call inside a confirmed consultation session |
| `ADR-VOICE-001` (this TDS) | `sessionMode` as additive query parameter/DTO field; default `VIDEO` preserves UC95 backward compatibility |
| `ADR-VOICE-002` (this TDS) | No audio stream persisted; ZegoCloud handles transport |
| `ADR-SESSION-001/002/003` (UC95, reused) | State machine, ownership, ZegoCloud failure safety — unchanged, applies identically |
| `BR-RBAC` | Role/ownership-scoped access |
| `CB-CONSULTATION-IMP-145 §9/§10/§16` | API contract, error codes, authorization matrix |
| PDPA / BR-CONSULTATION | Auditable lifecycle state requirement |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Assigned, verified Expert joins with `sessionMode=VOICE` | `ConsultationSessionService.joinSession(id, user, VOICE)` | `VOICE-TC-001` |
| TC-COND-002 | `sessionMode` omitted defaults to `VIDEO` (backward compatibility) | `ConsultationSessionService.joinSession(id, user)` (2-arg overload) | `VOICE-TC-002` |
| TC-COND-003 | Invalid `sessionMode` value rejected | `ConsultationSessionController` binding/validation | `VOICE-TC-003` |
| TC-COND-004 | Ownership violation — non-assigned Expert joins voice call | `ConsultationSessionPolicy.assertIsAssignedExpert()` (reused, UC95) | `VOICE-TC-004` |
| TC-COND-005 | Unverified assigned Expert attempts voice join | `ConsultationSessionPolicy.assertIsAssignedExpert()` (reused, UC95) | `VOICE-TC-005` |
| TC-COND-006 | ZegoCloud SDK failure on voice join | `ConsultationSessionService.joinSession()` (reused failure path, UC95 ADR-SESSION-003) | `VOICE-TC-006` |
| TC-COND-007 | `sessionMode` does not gate `session_status` transitions | `ConsultationSessionPolicy.assertTransitionAllowed()` (reused, UC95) | `VOICE-TC-007` |
| TC-COND-008 | `technicalLogJson` optionally records `{"sessionMode":"VOICE"}` on first join | `ConsultationSessionService.joinSession()` | `VOICE-TC-008` |
| TC-COND-009 | No new `session_mode` DB column introduced (Option B verification) | Integration — DB column scan | `VOICE-TC-INT-001` |
| TC-COND-010 | ZegoCloud token never persisted to DB (reused UC95/UC154 invariant) | Integration — DB column/value scan | `VOICE-TC-INT-002` |
| TC-COND-011 | Mobile: voice_call_screen initializes ZegoCloud SDK with camera disabled | `voice_call_screen.dart` | `VOICE-TC-MOB-001` |
| TC-COND-012 | Mobile: 503 error surfaces retry guidance to Expert | `voice_call_screen.dart` | `VOICE-TC-MOB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `sessionMode` value (`VOICE`/`VIDEO`/invalid), Role (assigned/non-assigned/unverified) | Covers each media-mode and authorization class with one representative case |
| Boundary Value Analysis | `sessionMode` omitted vs explicitly `VIDEO` (both must resolve to identical behavior) | Confirms the "default" path and "explicit" path are equivalent, per ADR-VOICE-001 |
| State Transition Testing | `session_status` FSM unaffected by `sessionMode` (§6.4 of UC95, unchanged) | Core invariant — `sessionMode` must never leak into the state machine |
| Error Guessing | ZegoCloud SDK exception injection, invalid enum string injection | External-service failure and malformed input are explicitly in scope (SRS E2/E3) |
| Contract Identity Check | Verifying `SessionMode`/`JoinSessionResponse` types match UC95's extension exactly | Anti-pattern-specific technique (AP-CB-201/202) — prevents parallel-type duplication |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-V001` | DB seed | `consultation_bookings{status:'CONFIRMED', expert_profile_id: EXPERT-001}` | Happy path booking (reused shape from UC95 FX-001) |
| `FX-V002` | DB seed | `consultation_sessions{session_status:'WAITING', booking_id: FX-V001}` | Initial session state |
| `FX-V003` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status:'VERIFIED'}` | Assigned, verified Expert |
| `FX-V004` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status:'PENDING'}` | Unverified Expert (negative case) |
| `FX-V005` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned |
| `FX-V006` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned |
| `FX-V007` | Mock | `IZegoCloudService.generateToken()` returns `ZegoTokenDto{roomId, token:"04A...", expiresAt}` | Happy path token |
| `FX-V008` | Mock | `IZegoCloudService.generateToken()` throws `ZegoCloudException` | Failure simulation |
| `FX-V009` | Query param | `sessionMode=VOICE` | Explicit voice-mode request |
| `FX-V010` | Query param | `sessionMode=AUDIO` (invalid enum value) | Validation-error case |
| `FX-V011` | Query param | *(omitted entirely)* | Default-mode case |
| `FX-V012` | Mobile Mock | `ZegoExpressEngine` mock — asserts `enableCamera(false)` called | Client-side SDK config verification |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// Naming convention reused from UC95's ConsultationSessionTestFactory
// ═══════════════════════════════════════════════════════════

// ConsultationVoiceCallTestFactory.java
class ConsultationVoiceCallTestFactory {

    static final UUID BOOKING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");

    // Reuses UC95's ConsultationSessionTestFactory shape verbatim — see
    // UC95_ManageConsultationSession_Test-Spec.md §4 for the canonical
    // makeBooking()/makeSession()/makeVerifiedExpertProfile() definitions.
    // This factory adds ONLY the sessionMode-specific helper below.

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

    // NEW — UC145-specific helper
    static JoinSessionResponse makeVoiceJoinResponse() {
        return makeJoinResponse(r -> r.setSessionMode(SessionMode.VOICE));
    }

    static JoinSessionResponse makeJoinResponse(Consumer<JoinSessionResponse> overrides) {
        JoinSessionResponse response = new JoinSessionResponse();
        response.setSessionId(SESSION_ID_1);
        response.setRoomId(SESSION_ID_1.toString());
        response.setZegoToken("04AAAAAGxxxxxxxx-test-token");
        response.setZegoAppId(12345678L);
        response.setTokenExpiresAt(Instant.now().plusSeconds(3600));
        response.setSessionStatus("IN_SESSION");
        response.setSessionMode(SessionMode.VIDEO); // default per ADR-VOICE-001
        overrides.accept(response);
        return response;
    }
}
```

---

### VOICE-TC-001 — Happy path: assigned verified Expert joins with `sessionMode=VOICE`

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession(UUID, UUID, SessionMode)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-VOICE-001` (this TDS §3), `ADR-SESSION-001/002` (UC95, reused) — `UC145_ConsultViaVoiceCall_TDS.md §6.1`

**Preconditions:**
- `FX-V001` (booking, `CONFIRMED`), `FX-V002` (session, `WAITING`), `FX-V003` (verified Expert)
- `FX-V007` (ZegoCloud mock returns success), `FX-V009` (`sessionMode=VOICE`)

**Test Steps:**
1. Arrange: seed session via `ConsultationVoiceCallTestFactory.makeSession()`, mock `IZegoCloudService.generateToken()` to return `FX-V007`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1, SessionMode.VOICE)`
3. Assert: response contains `sessionStatus="IN_SESSION"`, `sessionMode=SessionMode.VOICE`; repository `save()` invoked once with `sessionStatus=IN_SESSION`

**Expected Result (PASS — hành vi đúng):**
- Session transitions `WAITING → IN_SESSION` (identical to UC95's happy path); `JoinSessionResponse.sessionMode` equals `SessionMode.VOICE`

**Expected Result (FAIL — dấu hiệu lỗi):**
- `sessionMode` not echoed back, or session-join logic diverges from UC95's established transition behavior

**Current Status:** 🔴 Not written
**Implementation Note:** This test MUST reuse `ConsultationSessionPolicy.assertIsAssignedExpert()` from UC95 — do not mock a voice-specific policy method (none exists).

---

### VOICE-TC-002 — `sessionMode` omitted defaults to `VIDEO` (backward compatibility with UC95)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession(UUID, UUID)` (2-arg overload)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-VOICE-001` — "default `VIDEO` to preserve UC95's existing behavior"

**Preconditions:** `FX-V001`, `FX-V002`, `FX-V003`, `FX-V007`; `FX-V011` (parameter omitted)

**Test Steps:**
1. Arrange: same seed as `VOICE-TC-001`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1)` (no third argument)
3. Assert: response `sessionMode == SessionMode.VIDEO`; behavior otherwise identical to explicit `VIDEO` call

**Expected Result (PASS):** The 2-argument overload delegates internally to the 3-argument method with `SessionMode.VIDEO` — no separate code path exists
**Expected Result (FAIL):** Omitted parameter causes a null/exception, or resolves to `VOICE` instead of `VIDEO` (breaking UC95 callers)

**Current Status:** 🔴 Not written
**Implementation Note:** This is the critical backward-compatibility guarantee for UC95 — any existing UC95 caller must be unaffected by this TDS's change.

---

### VOICE-TC-003 — Invalid `sessionMode` value → 400 (`SES-001`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionController` (query parameter binding/validation)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** Error code `SES-001` (TDS §10), `FX-V010`

**Preconditions:** `@WebMvcTest(ConsultationSessionController.class)`, `IConsultationSessionService` mocked

**Test Steps:**
1. Arrange: `@WebMvcTest` context
2. Act: `POST /api/v1/consultations/sessions/{sessionId}/join?sessionMode=AUDIO`
3. Assert: HTTP 400, body `{"error":{"code":"SES-001", ...}}`; service `joinSession()` never invoked

**Expected Result (PASS):** Invalid enum value rejected at the controller binding layer before reaching the service
**Expected Result (FAIL):** 500 error, or invalid value silently coerced to a valid mode

**Current Status:** 🔴 Not written

---

### VOICE-TC-004 — Ownership violation: non-assigned Expert joins voice call → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()` (reused, UC95)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SESSION-002` (UC95, reused), error code `SES-004`

**Preconditions:** `FX-V001`, `FX-V002` seeded; caller = `OTHER_EXPERT_USER_ID`

**Test Steps:**
1. Arrange: booking assigned to `EXPERT_PROFILE_ID_1` (→ `EXPERT_USER_ID_1`)
2. Act: call `service.joinSession(SESSION_ID_1, OTHER_EXPERT_USER_ID, SessionMode.VOICE)`
3. Assert: throws `SessionAuthorizationException` with code `SES-004`; no ZegoCloud call made

**Expected Result (PASS):** Rejected identically to UC95's non-voice join — no `sessionMode`-specific bypass exists
**Expected Result (FAIL):** `sessionMode=VOICE` somehow bypasses the ownership check (critical security regression)

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important negative test in this batch — it proves `sessionMode` introduces NO new attack surface on the ownership guard.

---

### VOICE-TC-005 — Unverified assigned Expert attempts voice join → 403

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()` (reused, UC95)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SESSION-002` (UC95, reused) — "Verified Expert" actor requirement

**Preconditions:** `FX-V004` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1, SessionMode.VOICE)`
3. Assert: throws `SessionAuthorizationException` (`SES-004`)

**Expected Result (PASS):** Rejected despite matching `user_id`, because `verification_status != 'VERIFIED'`
**Expected Result (FAIL):** Unverified expert allowed to join a voice call

**Current Status:** 🔴 Not written

---

### VOICE-TC-006 — ZegoCloud SDK failure on voice join → 503, `session_status` unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession()` (reused failure path)
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SESSION-003` (UC95, reused), error code `SES-005`

**Preconditions:** `FX-V008` (ZegoCloud mock throws)

**Test Steps:**
1. Arrange: `IZegoCloudService.generateToken()` throws `ZegoCloudException`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1, SessionMode.VOICE)`
3. Assert: throws `SessionServiceUnavailableException` (`SES-005`); repository `save()` never invoked

**Expected Result (PASS):** `session_status` remains `WAITING`; failure behavior identical to UC95, regardless of `sessionMode`
**Expected Result (FAIL):** Session status changed despite ZegoCloud failure

**Current Status:** 🔴 Not written

---

### VOICE-TC-007 — `sessionMode` does not gate `session_status` transitions

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionPolicy.assertTransitionAllowed()` (reused, UC95)
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSessionPolicyVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SESSION-001 §6.4` (UC95, reused) — state machine invariant, TDS §6.4 "Invariant bất biến #2"

**Preconditions:** Session joined with `sessionMode=VOICE`, now `IN_SESSION`

**Test Steps:**
1. Arrange: session `session_status='IN_SESSION'`, joined originally with `sessionMode=VOICE`
2. Act: call `service.endSession(SESSION_ID_1, EXPERT_USER_ID_1)` (identical to UC95's end-session call — no voice-specific end method exists)
3. Assert: `session_status` transitions to `'COMPLETED'` — the exact same terminal value and transition path as a video session

**Expected Result (PASS):** `sessionMode` value has zero influence on the state machine's allowed transitions
**Expected Result (FAIL):** A `VOICE`-mode session cannot reach `COMPLETED`, or reaches a different terminal value

**Current Status:** 🔴 Not written
**Implementation Note:** This test directly guards against AP-AI-003 (Implicit Decision) — an AI implementation must not invent a voice-specific terminal state.

---

### VOICE-TC-008 — `technicalLogJson` optionally records `{"sessionMode":"VOICE"}` on first join

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionService.joinSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVoiceCallTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-VOICE-001` Decision — "optionally records `sessionMode` on `technicalLogJson`"

**Preconditions:** `FX-V001`, `FX-V002`, `FX-V003`, `FX-V007`, `FX-V009`

**Test Steps:**
1. Arrange: seed session `WAITING`
2. Act: call `service.joinSession(SESSION_ID_1, EXPERT_USER_ID_1, SessionMode.VOICE)`
3. Assert: repository `save()` argument's `technicalLogJson` field contains a JSON object with `sessionMode: "VOICE"`

**Expected Result (PASS):** `technicalLogJson` is populated without requiring any new column
**Expected Result (FAIL):** `sessionMode` written to a non-existent column (would fail at compile/migration level) or dropped entirely

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### VOICE-TC-INT-001 — No new `session_mode` DB column introduced (Option B verification)

**Severity:** `HIGH`
**Feature Under Test:** Schema verification — `consultation_sessions` table structure
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationVoiceCallIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-VOICE-001` Option B, TDS §5.3 — "No new migration is required"

**Preconditions:** PostgreSQL container running (`@Testcontainers` auto-start), Flyway migration applied automatically

**Test Steps:**
1. Start Spring context with Testcontainers PostgreSQL
2. Query `information_schema.columns` for `consultation_sessions`
3. Assert: no column named `session_mode` exists

**Expected Result (PASS):**
```sql
SELECT column_name FROM information_schema.columns
WHERE table_name = 'consultation_sessions' AND column_name = 'session_mode';
-- Expected: 0 rows
```

**Expected Result (FAIL):** A `session_mode` column exists (indicates an unauthorized migration was added, violating ADR-VOICE-001 Option B decision)

**Current Status:** 🔴 Not written

---

### VOICE-TC-INT-002 — ZegoCloud token never persisted to DB (full integration, reused UC95/UC154 invariant)

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `POST /sessions/{id}/join?sessionMode=VOICE` → DB state
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationVoiceCallIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-SESSION-003`/`UC154 ADR-ZEGO-001` (reused via UC95) — token never persisted

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Seed: `FX-V001`, `FX-V002`, `FX-V003` inserted via JPA

**Test Steps:**
1. Seed booking + session + verified expert profile
2. Call `POST /api/v1/consultations/sessions/{sessionId}/join?sessionMode=VOICE` with a valid Expert JWT
3. Assert response `200` with `zegoToken` present and `sessionMode="VOICE"`
4. Query `information_schema.columns` for `consultation_sessions` — assert no column name contains `token`
5. Query the actual row — assert no column value equals the returned `zegoToken` string

**Expected Result (PASS):**
- `SELECT column_name FROM information_schema.columns WHERE table_name='consultation_sessions' AND column_name LIKE '%token%'` returns 0 rows
- `session_status = 'IN_SESSION'`

**Expected Result (FAIL):** Any DB column stores the token value

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findById(SESSION_ID_1).orElseThrow();
assertThat(record.getSessionStatus()).isEqualTo("IN_SESSION");
assertThat(record.getTechnicalLogJson().toString()).contains("VOICE");
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (flutter_test + mocktail)

---

### VOICE-TC-MOB-001 — `voice_call_screen.dart` initializes ZegoCloud SDK with camera disabled

**Severity:** `MEDIUM`
**Feature Under Test:** `voice_call_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/consultation/screens/voice_call_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §5.1` Mobile Screen component, `§9.1` API contract, `FX-V012` mock

**Preconditions:** `ZegoExpressEngine` mocked via `mocktail`; API client mocked to return a successful `sessionMode: "VOICE"` join response

**Test Steps:**
1. Pump `VoiceCallScreen` widget with a mocked API client and mocked ZegoCloud engine
2. Act: trigger the join flow (e.g., `initState` auto-join or explicit "Join" button tap)
3. Assert: mocked `ZegoExpressEngine.enableCamera(false)` was called exactly once; `enableMic`/`muteMic` NOT muted by default

**Expected Result (PASS):** Camera explicitly disabled; audio-only local stream configured
**Expected Result (FAIL):** Camera left enabled (defaults to video), or SDK not initialized at all

**Current Status:** 🔴 Not written

---

### VOICE-TC-MOB-002 — 503 error surfaces retry guidance to Expert (Mobile)

**Severity:** `MEDIUM`
**Feature Under Test:** `voice_call_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/consultation/screens/voice_call_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-SESSION-003` (UC95, reused), SRS Exception E3 ("retry guidance")

**Preconditions:** Mocked API client returns `503 {code: "SES-005"}` on join

**Test Steps:**
1. Pump `VoiceCallScreen`, mock API client configured to return 503
2. Act: trigger join flow
3. Assert: UI displays a retry-guidance message (not a generic crash) and a "Retry" action remains available

**Expected Result (PASS):** User-facing retry affordance shown, matching SRS E3
**Expected Result (FAIL):** Unhandled exception / blank screen / app crash

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VOICE-TC-001` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-002` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-003` | `ConsultationSessionControllerVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-004` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-005` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-006` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-007` | `ConsultationSessionPolicyVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-008` | `ConsultationSessionServiceVoiceCallTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-INT-001` | `ConsultationVoiceCallIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-INT-002` | `ConsultationVoiceCallIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-MOB-001` | `voice_call_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `VOICE-TC-MOB-002` | `voice_call_screen_test.dart:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
// Extends UC95's ConsultationSessionService stub — the 3-arg overload is the new surface
@Service
public class ConsultationSessionService implements IConsultationSessionService {

    @Override
    public JoinSessionResponse joinSession(UUID sessionId, UUID currentUserId, SessionMode sessionMode) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    // 2-arg overload inherited via default method delegating to the 3-arg stub above —
    // also throws until the 3-arg method is implemented.
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VOICE-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-003` | Controller validation independent of service stub — must still FAIL because service mock never configured for a 400 case pre-implementation | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-007` | (pure policy, no stub — must implement state table) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VOICE-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-145` đã được review và approve
- [ ] `UC-95 ManageConsultationSession` implemented and stable — **blocking**
- [ ] `IZegoCloudService` (UC-154, via UC95) implemented and stable — **blocking**
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Không cần migration mới (baseline scope) — xác nhận
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `flutter test` — Mobile tests xanh
- [ ] Test coverage ≥ 80% lines cho new/modified code paths in `ConsultationSessionService`
- [ ] Không có business logic trong Controller
- [ ] `session_status` state machine unaffected — verified byte-for-byte identical terminal value `'COMPLETED'` to UC95/UC96
- [ ] No `session_mode` DB column introduced (VOICE-TC-INT-001 green)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase (`./mvnw compile` no errors)
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory pattern only)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS section)

### Suspension Criteria (Điều kiện tạm dừng)

- `UC-95 ManageConsultationSession` chưa sẵn sàng
- `IZegoCloudService` (UC-154) chưa sẵn sàng
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# No migration in baseline scope — code-only rollback:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong TDS §11.1 Prerequisites
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a voice-specific terminal state other than `'COMPLETED'` | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |
| AP-CB-201 | Re-inventing UC95's session-join flow | Test asserts a parallel `VoiceCallService`/`VoiceCallController` instead of extending UC95's `ConsultationSessionService` | ☐ | G-1 |
| AP-CB-202 | Persisting audio/media stream data | Test asserts a new table/column/blob-storage call for voice audio | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Spec UC145 v1.0 — Draft. 12 test cases (4 CRITICAL, 4 HIGH, 4 MEDIUM). Requires Entry Criteria (§6) satisfied — blocking on UC95/UC154 implementation — before Red Gate execution.*
