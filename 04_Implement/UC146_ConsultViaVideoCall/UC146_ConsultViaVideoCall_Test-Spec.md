# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC146 — Consult via Video Call

**Document ID:** `CB-CONSULTATION-IMP-146-TDD`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — `consultation_sessions` (L898-909), primary schema source
- `04_Implement/UC146_ConsultViaVideoCall/UC146_ConsultViaVideoCall_TDS.md` (`CB-CONSULTATION-IMP-146`)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` (session lifecycle owner, reused verbatim)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_Test-Spec.md` (style/pattern reference)
- `04_Implement/UC145_ConsultViaVoiceCall/UC145_ConsultViaVoiceCall_TDS.md` (sibling — owns `SessionMode`/`sessionMode` mechanism, ADR-VOICE-001)
- `04_Implement/UC154_EstablishRealtimeCommunicationSession/UC154_EstablishRealtimeCommunicationSession_TDS.md` (ZegoCloud token pattern, reused via UC95)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Backend: `./mvnw test`. Web (Expert Portal): `npm run test:run` (Vitest). Mobile (Expert App): `flutter test`.
> **UC146 is a thin variant** — the majority of coverage below is CONTRACT-IDENTITY / REUSE verification against
> UC95/UC145's already-specified test suites, not new backend logic. Do not duplicate UC95/UC145's own test files;
> reference them.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC146 Consult via Video Call |

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
| **Feature / Gap ID** | `GAP-UC146` |
| **Module** | `Consultation — Consult via Video Call (com.carebridge.backend.consultation, thin variant of UC95/UC145)` |
| **Spec gốc** | `CB-CONSULTATION-IMP-146` |
| **Priority** | 🟡 Medium |
| **Sprint** | `Sprint 2` |
| **Platform** | Expert App (Mobile) **and** Expert Portal (Web) — both in scope per TDS §1.4 (opposite default from sibling UC145, which is Mobile-only) |
| **Data Classification** | `Confidential` (session metadata only; video/audio stream never persisted — ADR-VIDEO-002) |
| **Compliance Scope** | `PDPA / Luật 91/2025`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies (BLOCKING)** | `UC95 ManageConsultationSession` (session lifecycle owner), `UC145 ConsultViaVoiceCall` (`SessionMode` mechanism owner, ADR-VOICE-001), `UC154 EstablishRealtimeCommunicationSession` (ZegoCloud token, via UC95) |
| **Downstream Consumers** | None new |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONSULTATION-IMP-146 §17` |
| **Constraints Injected** | C1 (reuse UC145's `SessionMode`/`JoinSessionResponse`/overload — no second type), C2 (reuse UC95's service/policy/repository — no parallel `VideoCallService`), C3 (`sessionMode` never gates `session_status`), C4 (ownership check identical to UC95 ADR-SESSION-002), C5 (token generation delegates to `IZegoCloudService`), C6 (no video/audio stream persisted), C7 (ZegoCloud failure never mutates `session_status`) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | A naive reading of "UC146 = video call feature" could imply a brand-new `VideoCallService`/`VideoCallController`/second `SessionMode` enum | TDS §1.2 Reuse Boundary and §ADR-VIDEO-001 explicitly forbid this — UC146 is a thin client-side variant of UC95's `joinSession()`, sharing UC145's `sessionMode` extension verbatim | Tests are structured primarily as **contract-identity verification** (same enum, same DTO, same service class) rather than net-new backend unit tests; a dedicated negative test (`VIDEO146-TC-007`) explicitly asserts no second `SessionMode`/service type exists |
| L2 | Platform field assumption — could be mistakenly copied from sibling UC145 (Mobile-only) | TDS §1.4 explicitly states UC146's SRS platform is `Expert App / Expert Portal` (BOTH), the OPPOSITE default from UC145 which scoped Web out | This Test-Spec includes BOTH a Flutter widget test (`VIDEO146-TC-MOB-001`) AND a Vitest component test (`VIDEO146-TC-WEB-001`) — Web is NOT skipped here despite UC145's Test-Spec precedent |
| L3 | No dedicated `session_mode` DB column exists; a careless test could assert on a first-class column | `technical_log_json jsonb` (existing, unchanged) is the only column touched, storing `{"sessionMode":"VIDEO"}` optionally — per ADR-VIDEO-001/ADR-VOICE-001 Option B | Tests assert `information_schema.columns` has ZERO rows for a `session_mode` column (mirrors TDS §14.1 verification query) — guards against an AI implementation adding a first-class column |
| L4 | UC146's own backend "new code" surface is nearly zero (§1.1 "precise, narrow delta") | The only genuinely new production concern is client-side ZegoCloud SDK camera-track configuration + the Web Expert Portal surface | Backend-layer test cases in this spec are almost entirely REUSE/regression checks against UC95/UC145's existing contracts, not new Service/Policy unit tests — this Test-Spec does not duplicate UC95's/UC145's own `ConsultationSessionServiceTest.java` test methods, it references them |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consult via Video Call bao gồm các layer:
├── Backend (REUSED, contract-identity checks only — no new Service/Policy logic):
│   ├── ConsultationSessionService.joinSession(id, userId, SessionMode.VIDEO) — reused from UC95+UC145
│   ├── ConsultationSessionPolicy.assertIsAssignedExpert() — reused from UC95, unchanged
│   └── IZegoCloudService.generateToken() — reused from UC154 via UC95, unchanged
├── Controller (REUSED) — ConsultationSessionController, @WebMvcTest, video-mode request/response shape only
├── Integration (Testcontainers PostgreSQL) — full video-join flow against real consultation_sessions row
├── Mobile (Expert App — flutter_test widget tests for video_call_screen.dart, NEW UI)
└── Web (Expert Portal — Vitest/Testing Library component tests for VideoCallPanel.tsx, NEW UI)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-146 §3.3.5.5 (L3615-3634)` | Verified Expert joins a video call inside a confirmed session |
| `ADR-VIDEO-001` | Reuse of UC145's `sessionMode` mechanism as the default case; no second mechanism |
| `ADR-VIDEO-002` | No video/audio stream persisted; ZegoCloud token pattern (UC154) unchanged |
| `ADR-SESSION-001/002/003` (UC95, reused) | State machine, ownership, ZegoCloud failure safety |
| `ADR-VOICE-001` (UC145, reused) | `SessionMode` enum, `sessionMode` query param, `JoinSessionResponse.sessionMode` field |
| `BR-RBAC` | Assigned + verified Expert only |
| `CB-CONSULTATION-IMP-146 §8` | Service Interface (reused contract) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — assigned Expert joins with `sessionMode=VIDEO` | `ConsultationSessionService.joinSession()` (reused) | `VIDEO146-TC-001` |
| TC-COND-002 | Default mode when `sessionMode` omitted → defaults to `VIDEO` | `IConsultationSessionService.joinSession()` 2-arg overload (reused) | `VIDEO146-TC-002` |
| TC-COND-003 | Ownership violation — non-assigned Expert joins video call → 403 | `ConsultationSessionPolicy.assertIsAssignedExpert()` (reused) | `VIDEO146-TC-003` (CRITICAL) |
| TC-COND-004 | ZegoCloud SDK failure on video join → 503, `session_status` unchanged | `ConsultationSessionService.joinSession()` (reused) | `VIDEO146-TC-004` (CRITICAL) |
| TC-COND-005 | `sessionMode` does not gate `session_status` state machine — video session reaches COMPLETED via identical `end` call | State machine (UC95 §6.4, reused) | `VIDEO146-TC-005` |
| TC-COND-006 | `technicalLogJson` optionally records `{"sessionMode":"VIDEO"}` on first join | `ConsultationSessionService.joinSession()` | `VIDEO146-TC-006` |
| TC-COND-007 | Contract-identity: UC146 does NOT introduce a second/divergent `SessionMode` enum or DTO | Static/contract check | `VIDEO146-TC-007` (CRITICAL — architecture-integrity) |
| TC-COND-008 | No `session_mode` first-class DB column exists | Schema verification | `VIDEO146-TC-008` |
| TC-COND-009 | Controller RBAC — non-Expert role rejected | `ConsultationSessionController` (reused) | `VIDEO146-TC-009` |
| TC-COND-010 | Full E2E: video join persists `IN_SESSION` + `technical_log_json`, ZegoCloud token never persisted | Integration | `VIDEO146-TC-INT-001` (CRITICAL) |
| TC-COND-011 | Mobile: Expert App video-call screen initializes camera-enabled SDK, renders controls | Flutter widget | `VIDEO146-TC-MOB-001` |
| TC-COND-012 | Web: Expert Portal video-call panel initializes camera-enabled SDK, renders controls | Vitest component | `VIDEO146-TC-WEB-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `sessionMode` (`VOICE`/`VIDEO`/omitted/invalid) | Confirm `VIDEO` and omitted-default share identical server behavior |
| Contract/Interface Testing | `SessionMode`, `JoinSessionResponse`, `IConsultationSessionService` types | Guards against AP-CB-301/AP-CB-302 (duplicated contract) |
| State Transition Testing | `session_status` (WAITING→IN_SESSION→COMPLETED, reused UC95 §6.4) | Verify video mode introduces no new edge |
| Error Guessing | ZegoCloud throws mid-token-generation for a video join | Verify identical failure-safety invariant to UC95/UC145 (no video-specific exception path) |
| Security Testing (IDOR) | Non-assigned Expert joins video session | ADR-SESSION-002 (reused) ownership boundary |
| Schema Inspection | `information_schema.columns` | Guards against a hallucinated `session_mode` column (L3) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ConsultationSessionEntity{sessionStatus='WAITING', bookingId=BOOKING_ID}` | Happy path first-joiner |
| `FX-002` | DB seed | `ConsultationSessionEntity{sessionStatus='COMPLETED'}` | Terminal-state join-attempt guard (reused UC95 semantics) |
| `FX-003` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` — matches assigned, verified Expert | Auth context, owner |
| `FX-004` | JWT | `{ sub: 'expert-002', role: 'ROLE_EXPERT' }` — NOT assigned | Non-owner attacker (IDOR) |
| `FX-005` | Mock | `IZegoCloudService.generateToken()` throws `ZegoCloudException` | Failure-safety path |
| `FX-006` | Mock | `IZegoCloudService.generateToken()` returns `ZegoTokenDto{roomId, token, expiresAt}` | Happy path token issuance |
| `FX-007` | Query param | `sessionMode=VIDEO` explicit, and omitted (implicit default) — both tested | Default-mode equivalence |

---

## 4. Test Case Specification

> **TC ID format:** `VIDEO146-TC-[NNN]`

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ConsultationSessionTestFactory.java
// (reused/extended from UC95 — this TDS does not invent a new factory,
//  per §1.2 Reuse Boundary; UC146-specific helpers are additive)
// ═══════════════════════════════════════════════════════════
class ConsultationSessionTestFactory {

    static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B2");
    static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C3");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000D4");

    static ConsultationSessionEntity makeWaitingSession() {
        return makeWaitingSession(s -> {});
    }

    static ConsultationSessionEntity makeWaitingSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = ConsultationSessionEntity.builder()
                .sessionId(SESSION_ID)
                .bookingId(BOOKING_ID)
                .sessionStatus("WAITING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        overrides.accept(session);
        return session;
    }

    // === UC146-specific additive helpers (video mode) ===

    static ZegoTokenDto makeZegoToken() {
        return new ZegoTokenDto("room-" + SESSION_ID, "04AAAAAGxxxxxxxx-synthetic", 12345678L,
                Instant.now().plusSeconds(3600));
    }

    static JoinSessionResponse makeExpectedVideoJoinResponse() {
        return JoinSessionResponse.builder()
                .sessionId(SESSION_ID)
                .roomId("room-" + SESSION_ID)
                .zegoToken("04AAAAAGxxxxxxxx-synthetic")
                .zegoAppId(12345678L)
                .tokenExpiresAt(Instant.now().plusSeconds(3600))
                .sessionStatus("IN_SESSION")
                .sessionMode(SessionMode.VIDEO) // reused enum from UC145 — not redeclared here
                .build();
    }
}
```

---

### VIDEO146-TC-001 — Happy path: assigned Expert joins with sessionMode=VIDEO

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSessionService.joinSession(UUID, UUID, SessionMode)` *(reused from UC95+UC145)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-VIDEO-001 §Decision`, `SRS §3.3.5.5`

**Preconditions:** `FX-001` seeded (`sessionStatus='WAITING'`), `FX-003` assigned+verified Expert JWT, `FX-006` ZegoCloud mock succeeds

**Test Steps:**
1. Arrange: `ConsultationSessionTestFactory.makeWaitingSession()`, mock repository `findById()` returns it; mock policy checks pass
2. Act: call `joinSession(SESSION_ID, EXPERT_USER_ID, SessionMode.VIDEO)`
3. Assert: `sessionRepository.save()` called with `sessionStatus='IN_SESSION'`, `technicalLogJson` containing `{"sessionMode":"VIDEO"}`
4. Assert: response `sessionMode == SessionMode.VIDEO`

**Expected Result (PASS):** Identical control flow to UC95/UC145's happy path, differing only in `sessionMode` value — proves UC146 introduces no divergent logic.
**Expected Result (FAIL):** Any video-specific branch in `ConsultationSessionService` not already covered by UC145's `sessionMode` handling — indicates a violation of the reuse boundary (§1.2).

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-002 — Default mode when sessionMode omitted → defaults to VIDEO

**Severity:** `MEDIUM`
**Feature Under Test:** `IConsultationSessionService.joinSession(UUID, UUID)` 2-arg overload *(reused from UC95, default preserved by UC145's ADR-VOICE-001)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-VIDEO-001 §Decision`, `ADR-VOICE-001 (UC145, reused)`

**Preconditions:** `FX-001` seeded

**Test Steps:**
1. Act: call `joinSession(SESSION_ID, EXPERT_USER_ID)` (2-arg overload, no explicit mode)
2. Assert: internally delegates to `joinSession(sessionId, currentUserId, SessionMode.VIDEO)` (verify via spy or behavior-equivalence assertion on the response)
3. Assert: response `sessionMode == SessionMode.VIDEO`

**Expected Result (PASS):** Omitting the parameter and explicitly passing `VIDEO` produce byte-identical outcomes.
**Expected Result (FAIL):** Omitted parameter defaults to something other than `VIDEO` (e.g., `VOICE`) — breaking backward compatibility with pre-UC145 callers.

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-003 — CRITICAL: Ownership violation — non-assigned Expert joins video call

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ConsultationSessionPolicy.assertIsAssignedExpert()` *(reused verbatim from UC95 ADR-SESSION-002)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SESSION-002 (UC95, reused)`, `C4`

**Preconditions:** `FX-001` (`booking.expertProfileId` maps to `EXPERT_USER_ID`); attacker JWT is `FX-004` (`OTHER_EXPERT_USER_ID`)

**Test Steps (Attack Simulation):**
1. Act: `joinSession(SESSION_ID, OTHER_EXPERT_USER_ID, SessionMode.VIDEO)`
2. Assert: `SessionAuthorizationException` thrown, code `SES-004`, HTTP 403

**Expected Result (PASS = hệ thống an toàn):** 403 SES-004 — identical to UC95's/UC145's voice-call authorization behavior; `sessionMode=VIDEO` grants no bypass.
**Expected Result (FAIL = lỗ hổng tồn tại):** A non-assigned Expert can join another Expert's video consultation — the video-call surface would be a NEW attack vector on an already-secured endpoint if this regresses.

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-004 — CRITICAL: ZegoCloud SDK failure on video join → 503, session_status unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSessionService.joinSession()` *(reused failure-safety path from UC95 ADR-SESSION-003)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SESSION-003 (UC95, reused)`, `C7`

**Preconditions:** `FX-001` seeded (`sessionStatus='WAITING'`), `FX-005` — `zegoCloudService.generateToken()` throws

**Test Steps:**
1. Act: `joinSession(SESSION_ID, EXPERT_USER_ID, SessionMode.VIDEO)`
2. Assert: `SessionServiceUnavailableException` thrown, code `SES-005`, HTTP 503
3. Assert: `sessionRepository.save()` NEVER invoked — `session_status` remains `WAITING` (verify via `verify(sessionRepository, never()).save(any())`)

**Expected Result (PASS):** Failure-safety invariant holds identically for video mode — no video-specific exception handling exists (as TDS §6.3 states).
**Expected Result (FAIL):** `session_status` mutated despite ZegoCloud failure — corrupts session state, blocks legitimate future join attempts.

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-005 — sessionMode does not gate session_status — video session reaches COMPLETED normally

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSessionService` state machine *(reused UC95 §6.4)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SESSION-001 (UC95, reused)`, `C3`

**Preconditions:** Session joined with `sessionMode=VIDEO`, now `IN_SESSION`

**Test Steps:**
1. Act: call the existing `endSession()`/`completeSession()` method (UC95, reused, unchanged) on a video-mode session
2. Assert: `sessionStatus` transitions to `COMPLETED` via the exact same code path as a voice-mode or original (pre-UC145) session

**Expected Result (PASS):** No video-specific branching exists in the completion path — `sessionMode` is confirmed orthogonal metadata.
**Expected Result (FAIL):** Completion logic differs based on `sessionMode`, or `technicalLogJson`'s `sessionMode` value blocks/alters the transition — violates invariant #2 (TDS §6.4).

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-006 — technicalLogJson optionally records {"sessionMode":"VIDEO"} on first join

**Severity:** `LOW`
**Feature Under Test:** `ConsultationSessionService.joinSession()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSessionServiceVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §5.3 Data Structure`

**Preconditions:** `FX-001` seeded, first joiner

**Test Steps:**
1. Act: `joinSession(SESSION_ID, EXPERT_USER_ID, SessionMode.VIDEO)`
2. Capture the entity passed to `save()`
3. Assert `technicalLogJson` contains key `sessionMode` with value `"VIDEO"`

**Expected Result (PASS):** Metadata correctly recorded using the same mechanism UC145 introduced for `VOICE`.
**Expected Result (FAIL):** `technicalLogJson` left null/unset for video joins, or a different key name used than UC145's voice equivalent (inconsistency between sibling docs).

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-007 — CRITICAL: Contract-identity — no second/divergent SessionMode enum or DTO introduced

**Severity:** `CRITICAL`
**Feature Under Test:** Static contract check — `SessionMode` enum, `JoinSessionResponse` class, `IConsultationSessionService` interface identity
**Test File:** `src/test/java/com/carebridge/backend/consultation/contract/UC146ContractIdentityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `§1.2 Reuse Boundary`, `ADR-VIDEO-001`, `AP-CB-301`, `AP-CB-302`

**Preconditions:** N/A — reflection/compile-time check

**Test Steps:**
1. Assert (via reflection or direct import) that `com.carebridge.backend.consultation.dto.SessionMode` is the SAME class used by both UC145's and UC146's calling code — no `com.carebridge.backend.consultation.dto.VideoMode` or similarly-named second enum exists in the compiled classpath
2. Assert `JoinSessionResponse` has exactly one canonical definition (no `VideoJoinResponse` subclass/parallel DTO)
3. Assert no `VideoCallService`/`VideoCallController` class exists anywhere in `com.carebridge.backend.consultation` or `com.carebridge.backend.emergency` packages (grep-based or classpath-scan assertion)

**Expected Result (PASS = architecture integrity preserved):** Single `SessionMode` enum, single `JoinSessionResponse`, single `ConsultationSessionService` — UC146 truly a thin variant.
**Expected Result (FAIL = architecture-integrity incident):** A second/divergent type or parallel service class found — per TDS §12.1, this triggers a Tech Lead-level rollback condition (AP-CB-301/AP-CB-302).

**Current Status:** 🔴 Not written
**Implementation Note:** This test should be added to CI as a permanent regression guard, not removed after initial GREEN — it protects the reuse boundary against future AI-assisted changes too.

---

### VIDEO146-TC-008 — No session_mode first-class DB column exists

**Severity:** `MEDIUM`
**Feature Under Test:** Schema verification (`consultation_sessions` table)
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSessionSchemaVerificationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §14.1 Database Inspection`, `ADR-VIDEO-001 Option B`

**Preconditions:** Testcontainers PostgreSQL with Flyway migrations applied (no new migration expected for UC146)

**Test Steps:**
1. Query `information_schema.columns WHERE table_name='consultation_sessions' AND column_name='session_mode'`
2. Assert result set has 0 rows

**Expected Result (PASS):** No first-class `session_mode` column — mode lives only in `technical_log_json`, confirming no migration was silently added for this TDS.
**Expected Result (FAIL):** A `session_mode` column exists — indicates an unauthorized/undocumented migration was introduced, contradicting TDS §5.3 "No new migration is required."

**Current Status:** 🔴 Not written

---

### VIDEO146-TC-009 — Controller RBAC: non-Expert role rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `POST /api/v1/consultations/sessions/{sessionId}/join?sessionMode=VIDEO` *(reused endpoint, UC95)*
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSessionControllerVideoModeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:** JWT with role `ROLE_MOTHER` (or any non-`ROLE_EXPERT`)

**Test Steps (Attack Simulation):**
1. `POST /api/v1/consultations/sessions/{sessionId}/join?sessionMode=VIDEO` with `ROLE_MOTHER` JWT
2. Assert `403 Forbidden`, service method never invoked

**Expected Result (PASS = hệ thống an toàn):** `403`, identical to UC95/UC145's existing RBAC gate — the `sessionMode` query parameter does not weaken it.
**Expected Result (FAIL = lỗ hổng tồn tại):** Adding `?sessionMode=VIDEO` to the request somehow bypasses the existing role check (a regression, since UC146 must not modify this gate per C2).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### VIDEO146-TC-INT-001 — Full E2E: video join persists IN_SESSION + technical_log_json, ZegoCloud token never persisted

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: POST /sessions/{id}/join?sessionMode=VIDEO → ConsultationSessionService.joinSession() → consultation_sessions row updated`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSessionVideoJoinIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically (NO new migration for UC146 — confirms §5.3)
- Seed: `consultation_bookings` + `consultation_sessions{sessionStatus='WAITING'}`, assigned verified Expert
- `IZegoCloudService` stubbed (`@MockBean`) to return a synthetic token (never a real ZegoCloud call in tests)

**Test Steps:**
1. `POST /api/v1/consultations/sessions/{sessionId}/join?sessionMode=VIDEO` with owning verified Expert JWT
2. Assert HTTP 200, body `sessionStatus="IN_SESSION"`, `sessionMode="VIDEO"`
3. Query DB: `consultation_sessions.session_status='IN_SESSION'`, `technical_log_json @> '{"sessionMode":"VIDEO"}'`
4. Run the reconciliation queries from TDS §14.1 — assert 0 rows for `session_mode` column and 0 rows for any `%token%` column
5. Assert no ZegoCloud token substring appears anywhere in the `consultation_sessions` row (scan all text/jsonb columns)

**Expected Result (PASS):**
- DB state matches the response exactly
- No ephemeral token persisted anywhere — confirms ADR-VIDEO-002/UC154 invariant holds for the video-mode path too

**Expected Result (FAIL):**
- Any mismatch between reported and persisted state, or token leakage into a persisted column

**DB Assertion:**
```java
ConsultationSessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
assertThat(session.getSessionStatus()).isEqualTo("IN_SESSION");
assertThat(session.getTechnicalLogJson().get("sessionMode").asText()).isEqualTo("VIDEO");

// Reused UC95/UC145/UC154 invariant — no token column exists at all
List<String> tokenColumns = jdbcTemplate.queryForList(
    "SELECT column_name FROM information_schema.columns " +
    "WHERE table_name = 'consultation_sessions' AND column_name LIKE '%token%'",
    String.class);
assertThat(tokenColumns).isEmpty();
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (Flutter — Expert App)

---

### VIDEO146-TC-MOB-001 — Expert App: video-call screen initializes camera-enabled SDK, renders controls

**Severity:** `HIGH`
**Feature Under Test:** `VideoCallScreen` widget (`lib/features/consultation/screens/video_call_screen.dart`) — **[NEW — UC146]**
**Test File:** `test/features/consultation/screens/video_call_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:** Widget pumped with a mocked `join?sessionMode=VIDEO` API response (`fakeVideoJoinResponse`); ZegoCloud SDK wrapped behind a fake/mock client for widget testing (no real SDK network call)

**Test Steps:**
1. `pumpWidget(VideoCallScreen(joinResponse: fakeVideoJoinResponse))`
2. Verify the mocked ZegoCloud SDK client's `enableCamera(true)` was called during `initState`
3. Verify both audio and video local tracks are requested to publish (mock call assertion)
4. Verify controls rendered: mute/unmute, camera on/off toggle, end-call button (`video_call_controls.dart`)
5. Tap camera-off toggle → verify `enableCamera(false)` called on the mock SDK client (client-side-only toggle, no new API call — per §1.5 RG-6 default position)
6. Verify NO `joinSession()` API call is triggered by the camera toggle (assert the mocked API client's `join` method call-count remains 1 throughout)

**Expected Result (PASS):** Camera-enabled SDK init confirmed; mid-call toggle is client-side only, matching TDS §6.2's documented default design position.
**Expected Result (FAIL):** Camera not enabled on init (falls back to voice-only unexpectedly), or toggling the camera triggers an unnecessary re-join API call.

**Current Status:** 🔴 Not written
**Implementation Note:** §1.5 RG-6 (voice↔video mid-session toggle) remains Open for Product/Tech Lead sign-off — this test encodes the TDS's stated default (client-side-only), consistent with UC145's mirrored test if/when it exists.

---

### WEB TEST CASES (Vitest/Testing Library — Expert Portal)

---

### VIDEO146-TC-WEB-001 — Expert Portal: video-call panel initializes camera-enabled SDK, renders controls

**Severity:** `MEDIUM`
**Feature Under Test:** `VideoCallPanel.tsx` (`src/features/consultationManagement/components/VideoCallPanel.tsx`) — **[NEW — UC146]**
**Test File:** `src/features/consultationManagement/components/VideoCallPanel.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** Component rendered with a mocked `join?sessionMode=VIDEO` API response via TanStack Query; ZegoCloud Web SDK mocked (no real network/media device call in unit test)

**Test Steps:**
1. `render(<VideoCallPanel session={fakeVideoJoinResponse} />)` wrapped in `QueryClientProvider`
2. Assert the mocked ZegoCloud Web SDK client's local-stream-publish call was invoked with camera enabled
3. `screen.getByRole('button', { name: /mute/i })`, `/camera/i`, `/end call/i` all present
4. `userEvent.click(screen.getByRole('button', { name: /camera/i }))` → assert mock SDK `enableCamera(false)` called; assert NO new `join` mutation triggered (call-count check on the mocked API client, same invariant as Mobile)
5. `userEvent.click(screen.getByRole('button', { name: /end call/i }))` → assert the existing (reused, UC95) `endSession`/`end` mutation is called — NOT a UC146-specific "video end" endpoint

**Expected Result (PASS):** Web parity with Mobile behavior; end-call reuses UC95's existing session-end contract, no video-specific backend call invented.
**Expected Result (FAIL):** A UC146-specific end-call endpoint invented (violates C2 reuse boundary), or camera toggle triggers a backend call.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VIDEO146-TC-001` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-002` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-003` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — IDOR |
| `VIDEO146-TC-004` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — failure safety |
| `VIDEO146-TC-005` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-006` | `ConsultationSessionServiceVideoModeTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-007` | `UC146ContractIdentityTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — architecture integrity |
| `VIDEO146-TC-008` | `ConsultationSessionSchemaVerificationTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-009` | `ConsultationSessionControllerVideoModeTest.java:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-INT-001` | `ConsultationSessionVideoJoinIntegrationTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL E2E |
| `VIDEO146-TC-MOB-001` | `video_call_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `VIDEO146-TC-WEB-001` | `VideoCallPanel.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> **Note:** Because UC146 reuses UC95/UC145's `ConsultationSessionService` almost verbatim, the Red Gate stub
> below is scoped to the NEW artifacts this TDS actually introduces (Mobile/Web UI + the contract-identity
> test's target). Backend service-level tests (`VIDEO146-TC-001` through `006`, `009`) are RED by construction
> only if UC95/UC145 are not yet implemented — once those are GREEN, this TDS adds no further backend RED-phase
> stub of its own (§1.1 "precise, narrow delta").

**Stub cho Red Phase (client-side, illustrative — Dart):**

```dart
class VideoCallScreen extends StatefulWidget {
  // ...
  @override
  State<VideoCallScreen> createState() => throw UnimplementedError(
      'Not implemented — Red Phase stub');
}
```

**Stub cho Red Phase (client-side, illustrative — TypeScript):**

```typescript
export function VideoCallPanel(props: VideoCallPanelProps): JSX.Element {
  throw new Error("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VIDEO146-TC-007` | Contract-identity check fails before any implementation exists | 🔴 FAIL (no types to verify yet / explicit fail-closed) | ☐ FAIL ☐ PASS | |
| `VIDEO146-TC-MOB-001` | `throw(UnimplementedError)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIDEO146-TC-WEB-001` | `throw(Error)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VIDEO146-TC-INT-001` | Fails if UC95/UC145 `joinSession(sessionMode)` overload absent | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(fill during implementation phase)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-CONSULTATION-IMP-146` reviewed and approved (Status field = `Approved`)
- [ ] **BLOCKING:** `UC-95 ManageConsultationSession` implemented and stable
- [ ] **BLOCKING:** `UC-145 ConsultViaVoiceCall`'s `SessionMode`/`sessionMode` extension (ADR-VOICE-001) implemented — UC146 cannot be built before or in divergence from it
- [ ] **BLOCKING:** `IZegoCloudService` (UC154, via UC95) implemented and stable
- [ ] `ADR-VIDEO-001` confirmed by Product/Tech Lead (currently `Accepted` by the TDS itself — pending formal sign-off)
- [ ] §1.4 Platform Discrepancy resolved by Product (confirm Mobile+Web scope — this Test-Spec proceeds with BOTH per the TDS's stated default)
- [ ] §1.5 RG-6 (voice→video mid-session toggle mechanism) — non-blocking for baseline join scope, but `VIDEO146-TC-MOB-001`/`WEB-001` encode the TDS's default (client-side-only) design

### Exit Criteria (DoD)

- [ ] `./mvnw test` green (full suite, confirming no regression to UC95/UC145's existing tests)
- [ ] `./mvnw verify` — integration test (`VIDEO146-TC-INT-001`) green with Testcontainers
- [ ] `flutter test` — Mobile (Expert App) widget test green
- [ ] `npm run test:run` — Web (Expert Portal) component test green
- [ ] No business logic in `ConsultationSessionController` beyond what UC95 already has (UC146 adds none)
- [ ] No PII/secret in plaintext logs; no ZegoCloud token in any DB column
- [ ] **CRITICAL**: `VIDEO146-TC-007` (contract-identity — no divergent `SessionMode`/service) green before merge; this is UC146's single most important test, analogous in importance to UC137's timeout test
- [ ] **CRITICAL**: `VIDEO146-TC-003` (IDOR) and `VIDEO146-TC-004` (ZegoCloud failure safety) both green

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — new UC146 artifacts (Mobile/Web UI) FAIL against throw-stub before implementation begins
- [ ] **Contract Existence** — `./mvnw compile` clean, no hallucinated imports; confirms `SessionMode`/`JoinSessionResponse` imported from UC145's package, not redeclared
- [ ] **Props Isolation** — verified via `ConsultationSessionTestFactory` (reused/extended, not reinvented)
- [ ] **Oracle Source** — every assert traces to BR-RBAC/BR-CONSULTATION/ADR-VIDEO-001/002/ADR-SESSION-001/002/003 (UC95)/ADR-VOICE-001 (UC145)
- [ ] **Reuse Boundary Gate (project-specific)** — `VIDEO146-TC-007`/`AP-CB-301`/`AP-CB-302` all confirm zero duplicated service/enum/DTO

### Suspension Criteria

- `UC-95`/`UC-145`/`UC-154` not yet implemented — UC146 implementation and this Test-Spec's INT test cannot proceed (hard sequencing dependency, TDS §11.1)
- §1.4 Platform Discrepancy not yet confirmed by Product — Web scope (`VIDEO146-TC-WEB-001`) may need to be de-scoped if Product later narrows UC146 to Mobile-only (would require revising this Test-Spec, not silently dropping the test)
- §1.5 RG-6 unresolved — non-blocking for baseline join scope per TDS, but blocks any FUTURE mid-session-toggle-as-API-call test if that design position is later reversed
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
# No new migration to revert (TDS §5.3 — no schema change).
# No backend service/policy/repository files to revert — UC146 introduces none beyond UC95/UC145's own scope.

# Mobile
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/screens/video_call_screen.dart
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/widgets/video_call_controls.dart
git checkout -- 05_Development/CareBridgeMobileApp/test/features/consultation/screens/video_call_screen_test.dart

# Web
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/VideoCallPanel.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/components/VideoCallPanel.test.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/models/consultationSession.ts
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/services/consultationSessionApi.ts

# Contract-identity regression test (keep even on rollback — protects UC95/UC145 boundary going forward)
# git checkout -- src/test/java/com/carebridge/backend/consultation/contract/UC146ContractIdentityTest.java  (optional)

kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không match bất kỳ constraint C1-C7 nào | ☐ | G-0 |
| AP-AI-003 | Implicit Decision | Test assumes `sessionMode` is a new first-class DB column instead of `technicalLogJson` | ☐ | G-1 — violates ADR-VIDEO-001/ADR-VOICE-001 Option B |
| AP-AI-005 | Hallucinated Contract | Test/code re-implements ZegoCloud token generation instead of calling `IZegoCloudService` | ☐ | G-3 — violates C5, duplicates UC154 logic |
| AP-CB-301 *(project-specific, TDS §17.4)* | **Declaring a second/divergent `SessionMode` mechanism** | A new `VideoMode` enum, `VideoJoinRequest`, or separate `sessionMode`-like parameter created instead of reusing UC145's exact types | ☐ | **BLOCK** — violates C1/ADR-VIDEO-001; covered by `VIDEO146-TC-007` |
| AP-CB-302 *(project-specific, TDS §17.4)* | **Re-inventing UC95's session-join flow** | New `VideoCallService`/`VideoCallController`/parallel entity created inside `consultation` package duplicating `ConsultationSessionService` | ☐ | **BLOCK** — must extend UC95's existing interface (§1.2); covered by `VIDEO146-TC-007` |
| AP-CB-303 *(project-specific, TDS §17.4)* | **Persisting video/audio stream data** | New table/column/blob-storage call added to persist video call footage | ☐ | **BLOCK** — violates ADR-VIDEO-002, no BR/SRS basis; covered by `VIDEO146-TC-INT-001`/`VIDEO146-TC-008` |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
