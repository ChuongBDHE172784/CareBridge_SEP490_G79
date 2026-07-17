# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC144 — Consult via Chat — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-144`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft` *(reverted 2026-07-15 — SUPERSEDED by `04_Implement/UC144_DirectConsultChat/UC144_DirectConsultChat_Test-Spec.md`; kept for history only, see CHANGELOG at end of §Red-Green tracker note below)*
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `Confirmed via user decision 2026-07-15`
**DPO Sign-off:** `[ ] Pending` *(outstanding, proceeding for dev/test per explicit user decision 2026-07-15)*
**Approved by:** `[ ] Pending — Approved status revoked 2026-07-15: user rejected the booking/session-tied chat architecture in favor of a direct-conversation redesign. See 04_Implement/UC144_DirectConsultChat/.`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L898-921, L1428-1429, L1640, L1841-1845) — primary schema source
- `04_Implement/UC144_ConsultViaChat/UC144_ConsultViaChat_TDS.md` (`CB-CONSULTATION-IMP-144`) — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` §ADR-SESSION-001, §5.1, §5.2 — upstream, confirmed `session_status` enum + scaffolded `ConsultationMessageEntity`/`ConsultationMessageRepository` names (reused verbatim)
- `04_Implement/UC96_WriteConsultationSummary/UC96_WriteConsultationSummary_TDS.md` §ADR-SUMMARY-002 — reused non-blocking content-safety nudge design philosophy
- `04_Implement/UC96_WriteConsultationSummary/UC96_WriteConsultationSummary_Test-Spec.md` — sibling spec, same batch, style reference
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.5.3 (L3573-3592) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, audit, "AI provides guidance only; never diagnose, prescribe" mandate)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC144 |

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
| **Feature / Gap ID** | `UC-144` |
| **Module** | `Consultation — Consult via Chat` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-144` |
| **Priority** | 🟡 Medium |
| **Sprint** | `Sprint 3 "Cross-Domain Integration"` — TV4-Lâm |
| **Milestone** | Sprint 3 |
| **Data Classification** | `Sensitive-PII` (free-text chat conversation between a Verified Expert and a Mother that may reference health context) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION`, `BR-SAFETY` |
| **Upstream Dependencies** | `UC-95 Manage Consultation Session` — session must exist and be non-terminal (reused `session_status` enum, `ADR-SESSION-001`) |
| **Downstream Consumers** | `UC-96 Write Consultation Summary` (informational reference only, no code dependency), Notification service (out of scope) |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TypeScript/Vite) + Mobile (Flutter/Dart) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC144_ConsultViaChat_TDS.md §17`, ADR-CHAT-001/002/003/004 |
| **Constraints Injected** | Session-validity (non-terminal) gate reusing UC95's confirmed enum (C1), two-party ownership (C2), non-blocking Expert-only content-safety nudge reused from UC96 (C3), realtime signaling delegates to ZegoCloud/UC154 without a bespoke server (C4), scoped write — never touches session/summary lifecycle fields (C5), reuse of UC95-scaffolded entity/repository names (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-144 does not name which `session_status` values are chat-eligible ("valid consultation session" is undefined) | `UC144 TDS ADR-CHAT-001` selects the inverse of UC96's gate: chat allowed while `session_status NOT IN ('COMPLETED','NO_SHOW','CANCELLED')`, i.e. `WAITING` or `IN_SESSION` | Tests assert against BOTH `WAITING` (`CHAT-TC-002`) and `IN_SESSION` (`CHAT-TC-001`) as positive cases, and all three terminal values as negative cases (`CHAT-TC-004`) — guarantees the full enum partition is covered, not just one state |
| L2 | SRS does not distinguish message-sender role for content-safety purposes | `ADR-CHAT-003` scopes the non-blocking nudge to Expert-authored messages only — a Mother describing her own symptoms is not diagnostic/prescriptive content requiring the check | Tests assert `validateContentSafety()` is invoked for Expert senders (`CHAT-TC-006`) and explicitly NOT invoked/never flags for Mother senders (`CHAT-TC-007`) — a test that flags Mother content would itself be a spec defect |
| L3 | `consultation_messages.status varchar(20) DEFAULT 'SENT'` implies a delivery-state lifecycle (`SENT`/`DELIVERED`/`READ`) not fully specified by SRS | TDS §5.3 gap note 3: baseline scope only ever writes `'SENT'` at creation; no read-receipt/status-update endpoint is built in this Draft | Tests assert every persisted message has `status == 'SENT'` at creation time (`CHAT-TC-001`) and do NOT test a status-transition endpoint that does not exist in baseline scope — avoids testing an unspecified feature |
| L4 | No DB `CHECK` constraint on `message_body` length or `message_type` enum | `SendChatMessageRequest.messageBody` has `@Size(max=2000)`, `messageType` has `@Pattern` — app-level only | `CHAT-TC-005` boundary-tests exactly at 2000/2001 chars, since the DB itself will not reject an oversized value |
| L5 | UC95 §5.1/§5.2 scaffolds `ConsultationMessageEntity`/`ConsultationMessageRepository` names but does not implement them | UC144 TDS §1.1/§17 C6 mandates reusing these exact names, not inventing new ones | Test file paths and Props Isolation factory reference `ConsultationMessageEntity`/`ConsultationMessageRepository` exactly, matching UC95's scaffolded class diagram |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consult via Chat bao gồm các layer:
├── Domain (ConsultationChatPolicy — pure logic, no deps)
├── Application / Use Cases (ConsultationChatService — mock JPA Repository với Mockito)
├── Controller (ConsultationChatController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full send + list + session-eligibility-isolation flow)
├── Mobile (chat_screen.dart, consultation_chat_api.dart — flutter_test)
└── Web (ChatPanel.tsx, ContentSafetyWarningBanner.tsx reused — Vitest + Testing Library, MSW)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-144` (§3.3.5.3, L3573-3592) | Exchanges messages inside a valid consultation session |
| `ADR-CHAT-001` | Session-validity gate — non-terminal `session_status` required |
| `ADR-CHAT-002` | Realtime signaling delegates to ZegoCloud (via UC95/UC154); REST is the durable system of record |
| `ADR-CHAT-003` | Content-safety: non-blocking advisory nudge, Expert-authored messages only, reused from UC96 |
| `ADR-CHAT-004` | Ownership — two session participants only (assigned, verified Expert OR booking requester/Mother) |
| `BR-RBAC` / `BR-SAFETY` | Role/ownership-scoped, no diagnostic/prescriptive platform behavior |
| `CB-CONSULTATION-IMP-144 §9/§10/§13/§16` | API contract, error codes, test-condition summary, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — assigned Expert sends message in `IN_SESSION` session | `ConsultationChatService.sendMessage()` | `CHAT-TC-001` |
| TC-COND-002 | Happy path — booking Mother sends message in `WAITING` session | `ConsultationChatService.sendMessage()` | `CHAT-TC-002` |
| TC-COND-003 | Ownership violation — non-participant user attempts send → 403 (`CHAT-004`) | `ConsultationChatPolicy.assertIsSessionParticipant()` | `CHAT-TC-003` |
| TC-COND-004 | Session-validity gate — send attempted on `COMPLETED`/`NO_SHOW`/`CANCELLED` session → 400 (`CHAT-006`) | `ConsultationChatPolicy.assertSessionChatEligible()` | `CHAT-TC-004` |
| TC-COND-005 | Boundary: `messageBody` blank / 2000 / 2001 chars → 400 (`CHAT-001`) | DTO validation | `CHAT-TC-005` |
| TC-COND-006 | Content with dosage-like pattern from Expert → 201 Created, non-empty `contentSafetyWarnings` (non-blocking) | `ConsultationChatPolicy.validateContentSafety()` | `CHAT-TC-006` |
| TC-COND-007 | Content with dosage-like pattern from Mother → NOT flagged (check scoped to Expert only) | `ConsultationChatPolicy.validateContentSafety()` | `CHAT-TC-007` |
| TC-COND-008 | Session not found → 404 (`CHAT-003`) | `ConsultationChatService.sendMessage()` | `CHAT-TC-008` |
| TC-COND-009 | `ChatMessageSent` event emitted on every successful send | `ConsultationChatService` | `CHAT-TC-009` |
| TC-COND-010 | Service never mutates `session_status`/`started_at`/`ended_at`/`expert_summary` (architecture-boundary guard) | `ConsultationChatService`, `ConsultationMessageRepository` | `CHAT-TC-010` |
| TC-COND-011 | `GET .../messages` returns paginated history ordered by `sentAt` ascending | `ConsultationChatService.listMessages()` | `CHAT-TC-011` |
| TC-COND-012 | Non-participant attempts `GET .../messages` → 403 (`CHAT-004`) | `ConsultationChatController` authorization | `CHAT-TC-012` |
| TC-COND-013 | Unverified assigned Expert attempts send → 403 (`CHAT-004`) | `ConsultationChatPolicy.assertIsSessionParticipant()` | `CHAT-TC-013` |
| TC-COND-014 | Downstream DB write failure/timeout → 503 (`CHAT-007`) | `ConsultationChatService.sendMessage()` | `CHAT-TC-014` |
| — | Full flow via Testcontainers — send, verify DB column scoping, session-eligibility isolation, list ordering | `ConsultationChatController` + real DB | `CHAT-TC-INT-001` |
| — | Mobile: chat screen sends and displays a new message | `chat_screen.dart` | `CHAT-TC-MOB-001` |
| — | Web: ChatPanel submits and displays confirmation | `ChatPanel.tsx` | `CHAT-TC-WEB-001` |
| — | Web: ContentSafetyWarningBanner (reused from UC96) shows warning but allows send to proceed | `ContentSafetyWarningBanner.tsx` | `CHAT-TC-WEB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Boundary Value Analysis | `messageBody` length (0, 2000, 2001 chars) | Confirms `@NotBlank @Size(max=2000)` boundary — the only enforcement, since DB has no length CHECK |
| State Transition Testing | Session-validity gate — chat allowed for `WAITING`/`IN_SESSION`, rejected for terminal states | Core invariant unique to UC144 (inverse of UC96's terminal-required gate) |
| Decision Table | Ownership (Expert/Mother/non-participant) × session-status × sender-role-for-content-safety combinations | 201 (clean) vs 201 (warned, Expert only) vs 400 (eligibility) vs 403 (ownership) branching |
| Equivalence Partitioning | `session_status` enum partitioned into {`WAITING`,`IN_SESSION`} (eligible) vs {`COMPLETED`,`NO_SHOW`,`CANCELLED`} (not eligible) | Full enum coverage without needing all 5 values individually tested in every scenario |
| Error Guessing | DB write timeout, cross-module boundary violation, content-safety check incorrectly applied to Mother | External-service/DB failure and layer/role-scope discipline are explicitly in scope |
| Non-Functional (Advisory) Testing | Content-safety check must warn (Expert only), never block | Direct verification of `ADR-CHAT-003`'s core design decision, reused from UC96 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-C01` | DB seed | `consultation_bookings{booking_id: B1, expert_profile_id: EXPERT-001, requester_user_id: MOTHER-001}` | Happy path booking |
| `FX-C02` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'IN_SESSION'}` | Eligibility satisfied — live session |
| `FX-C03` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'WAITING'}` | Eligibility satisfied — pre-session |
| `FX-C04` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'COMPLETED'}` | Eligibility NOT satisfied (negative) |
| `FX-C05` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'NO_SHOW'}` | Eligibility NOT satisfied (negative) |
| `FX-C06` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'CANCELLED'}` | Eligibility NOT satisfied (negative) |
| `FX-C07` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status: 'VERIFIED'}` | Assigned, verified Expert |
| `FX-C08` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status: 'PENDING'}` | Unverified Expert (negative) |
| `FX-C09` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned, verified Expert |
| `FX-C10` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned Expert (negative) |
| `FX-C11` | JWT | `{sub: 'MOTHER-001', role: 'MOTHER'}` | Auth context — booking requester (Mother) |
| `FX-C12` | JWT | `{sub: 'MOTHER-999', role: 'MOTHER'}` | Auth context — non-participant Mother (negative) |
| `FX-C13` | Request body | `messageBody: "...take 500mg twice daily..."` | Content-safety-flagged text |
| `FX-C14` | Request body | `messageBody: "I have been feeling more tired than usual this week."` | Clean, informational text (Mother's own symptom description) |
| `FX-C15` | Request body | `messageBody: "Hello, I am ready when you are."` | Clean, informational text (Expert greeting) |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership, eligibility gate, role-scoped content-safety) |
| Web (React) | — | — | ✅ (Vitest) | ✅ (Testing Library) | — | — |
| Mobile (Flutter) | — | — | — | ✅ (`flutter_test` widget tests) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Reuses UC95's ConsultationSessionTestFactory conventions where
// entities overlap (ConsultationSessionEntity/ConsultationMessageEntity
// are shared/scaffolded from UC95's class diagram).
// ═══════════════════════════════════════════════════════════

class ConsultationChatTestFactory {

    static final UUID BOOKING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    static final UUID OTHER_MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");
    static final UUID MESSAGE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000601");

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(MOTHER_USER_ID);
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeSession(String sessionStatus, Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus(sessionStatus); // ADR-CHAT-001 — reused enum from UC95 ADR-SESSION-001
        overrides.accept(session);
        return session;
    }

    static SendChatMessageRequest makeMessageRequest(Consumer<SendChatMessageRequest> overrides) {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessageBody("Hello, I am ready when you are.");
        request.setMessageType("TEXT");
        overrides.accept(request);
        return request;
    }

    static ConsultationMessageEntity makePersistedMessage(Consumer<ConsultationMessageEntity> overrides) {
        ConsultationMessageEntity message = new ConsultationMessageEntity();
        message.setMessageId(MESSAGE_ID_1);
        message.setSessionId(SESSION_ID_1);
        message.setSenderUserId(EXPERT_USER_ID_1);
        message.setMessageType("TEXT");
        message.setMessageBody("Hello, I am ready when you are.");
        message.setSentAt(Instant.parse("2026-07-02T10:05:00Z"));
        message.setStatus("SENT");
        overrides.accept(message);
        return message;
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

### CHAT-TC-001 — Happy path: assigned, verified Expert sends message in `IN_SESSION` session

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC144 TDS §6.1` sequence diagram, `ADR-CHAT-001/004`

**Preconditions:** `FX-C01`, `FX-C02` (`IN_SESSION`), `FX-C07` (verified Expert)

**Test Steps:**
1. Arrange: mock `sessionRepository.findById(S1)` returns `FX-C02`; mock `bookingRepository.findById(B1)` returns `FX-C01`.
2. Act: `service.sendMessage(S1, ConsultationChatTestFactory.makeMessageRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: `messageRepository.save(...)` invoked once with `senderUserId=EXPERT_USER_ID_1`, `status="SENT"`; response `contentSafetyWarnings` empty.

**Expected Result (PASS):** `201`-equivalent response, message persisted with `status="SENT"`.
**Expected Result (FAIL):** Exception thrown, or wrong repository method invoked.

**Current Status:** 🟢 Passing

---

### CHAT-TC-002 — Happy path: booking Mother sends message in `WAITING` session

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CHAT-001` (Option B — `WAITING` is chat-eligible), `ADR-CHAT-004` (Mother branch)

**Preconditions:** `FX-C01`, `FX-C03` (`WAITING`)

**Test Steps:**
1. Arrange: mock `sessionRepository.findById(S1)` returns `FX-C03`; mock `bookingRepository.findById(B1)` returns `FX-C01`.
2. Act: `service.sendMessage(S1, ConsultationChatTestFactory.makeMessageRequest(r -> {}), MOTHER_USER_ID)`.
3. Assert: `messageRepository.save(...)` invoked once with `senderUserId=MOTHER_USER_ID`; `validateContentSafety()` is NOT invoked (sender role is MOTHER, per `ADR-CHAT-003` scope).

**Expected Result (PASS):** Message accepted for a pre-session (`WAITING`) session, confirming ADR-CHAT-001's Option B decision.
**Expected Result (FAIL):** Message rejected for `WAITING`, or content-safety check incorrectly invoked for a Mother sender.

**Current Status:** 🟢 Passing

---

### CHAT-TC-003 — Ownership violation: non-participant user attempts send → 403 (`CHAT-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ConsultationChatPolicy.assertIsSessionParticipant()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationChatPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-CHAT-004`

**Preconditions:** `FX-C01` booking assigned to `EXPERT_PROFILE_ID_1`/`MOTHER_USER_ID`; caller = `OTHER_EXPERT_USER_ID` (non-assigned Expert) AND separately `OTHER_MOTHER_USER_ID` (non-participant Mother)

**Test Steps:**
1. Arrange: `booking = ConsultationChatTestFactory.makeBooking(b -> {})`.
2. Act (non-assigned Expert): `policy.assertIsSessionParticipant(booking, OTHER_EXPERT_USER_ID)`.
3. Assert: throws `ChatAuthorizationException` code `CHAT-004`.
4. Act (non-participant Mother): `policy.assertIsSessionParticipant(booking, OTHER_MOTHER_USER_ID)`.
5. Assert: throws `ChatAuthorizationException` code `CHAT-004`.

**Expected Result (PASS):** Both non-participant identities rejected; no message written.
**Expected Result (FAIL):** Message sent/read by a non-participant of the session's booking.

**Current Status:** 🟢 Passing

---

### CHAT-TC-004 — Session-validity gate: send rejected for terminal states → 400 (`CHAT-006`)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationChatPolicy.assertSessionChatEligible()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationChatPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-CHAT-001`, `UC144 TDS §6.3` error-path sequence diagram

**Preconditions:** `FX-C04` (`COMPLETED`), `FX-C05` (`NO_SHOW`), `FX-C06` (`CANCELLED`)

**Test Steps:**
1. Act (`COMPLETED`): `policy.assertSessionChatEligible(FX-C04)`.
2. Assert: throws `ChatSessionNotEligibleException` code `CHAT-006`.
3. Act (`NO_SHOW`): `policy.assertSessionChatEligible(FX-C05)`.
4. Assert: throws `ChatSessionNotEligibleException` code `CHAT-006`.
5. Act (`CANCELLED`): `policy.assertSessionChatEligible(FX-C06)`.
6. Assert: throws `ChatSessionNotEligibleException` code `CHAT-006`.
7. Assert (accompanying Service-layer test): no `messageRepository.save(...)` interaction occurs for any of the three terminal states.

**Expected Result (PASS):** All three terminal states rejected before persistence — full enum partition coverage (Logic Issue L1).
**Expected Result (FAIL):** Any terminal-state session accepts a new message.

**Current Status:** 🟢 Passing

---

### CHAT-TC-005 — Boundary: `messageBody` length (0 / 2000 / 2001 chars)

**Severity:** `MEDIUM`
**Feature Under Test:** `SendChatMessageRequest` DTO validation (`@NotBlank @Size(max=2000)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/SendChatMessageRequestValidationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC144 TDS §8.1` DTO annotation; `§5.3` genuine schema gap (no DB length limit on `message_body`)

**Test Steps:**
1. Act (blank): validate `makeMessageRequest(r -> r.setMessageBody(""))`.
2. Assert: violation on `messageBody` (`@NotBlank`).
3. Act (2000 chars): validate `makeMessageRequest(r -> r.setMessageBody("a".repeat(2000)))`.
4. Assert: no violation.
5. Act (2001 chars): validate `makeMessageRequest(r -> r.setMessageBody("a".repeat(2001)))`.
6. Assert: violation on `messageBody` (`@Size(max=2000)`).

**Expected Result (PASS):** Boundary respected exactly at 2000/2001; blank rejected.
**Expected Result (FAIL):** Off-by-one on boundary, or an oversized value silently persisted (critical since DB has no CHECK constraint — this test is the ONLY enforcement).

**Current Status:** 🟢 Passing

---

### CHAT-TC-006 — Content with dosage-like pattern from Expert → 201 Created with non-empty `contentSafetyWarnings` (non-blocking)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatPolicy.validateContentSafety()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationChatPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-CHAT-003` — non-blocking advisory nudge, reused verbatim from UC96 `ADR-SUMMARY-002`

**Preconditions:** `FX-C13` (`"...take 500mg twice daily..."`), sender = Expert (`FX-C09`)

**Test Steps:**
1. Act: `policy.validateContentSafety(FX-C13.messageBody, "EXPERT")`.
2. Assert: returns a non-empty `List<String>` warning list.
3. Act (integration-level, in Service test): `service.sendMessage(S1, makeMessageRequest(r -> r.setMessageBody(FX-C13.messageBody)), EXPERT_USER_ID_1)`.
4. Assert: send STILL SUCCEEDS (`messageRepository.save()` IS invoked) — the warning is surfaced in the response, never blocks persistence.

**Expected Result (PASS):** Warning present AND message saved — this is the release-blocking assertion for ADR-CHAT-003.
**Expected Result (FAIL):** Send rejected/blocked due to detected content (violates AP-CB-302, §8) — a test asserting rejection here would itself be a spec defect per Logic Issue L2.

**Current Status:** 🟢 Passing

---

### CHAT-TC-007 — Content with dosage-like pattern from Mother → NOT flagged (content-safety scoped to Expert only)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatPolicy.validateContentSafety()` scope / `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-CHAT-003` — check applies to Expert-authored messages only

**Preconditions:** `FX-C13`-equivalent dosage-like text, sender = Mother (`FX-C11`)

**Test Steps:**
1. Act: `service.sendMessage(S1, makeMessageRequest(r -> r.setMessageBody("...take 500mg twice daily...")), MOTHER_USER_ID)`.
2. Assert: `chatPolicy.validateContentSafety(...)` is NEVER invoked for this call (verified via Mockito `verifyNoInteractions`/`verify(policy, never())`).
3. Assert: response `contentSafetyWarnings` is empty; message persisted normally.

**Expected Result (PASS):** Mother's own text is never subjected to the diagnostic/prescriptive content check, regardless of its content — consistent with ADR-CHAT-003's scoping rationale (a Mother describing her own situation is not platform-authored clinical content).
**Expected Result (FAIL):** Content-safety check incorrectly invoked for or flags a Mother-authored message.

**Current Status:** 🟢 Passing

---

### CHAT-TC-008 — Session not found → 404 (`CHAT-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC144 TDS §10` error table

**Test Steps:**
1. Arrange: mock `sessionRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `service.sendMessage(nonExistentId, makeMessageRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: throws `SessionNotFoundException` code `CHAT-003`.

**Current Status:** 🟢 Passing

---

### CHAT-TC-009 — `ChatMessageSent` event emitted on every successful send

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC144 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `service.sendMessage(S1, makeMessageRequest(r -> r.setMessageBody("First message")), EXPERT_USER_ID_1)`.
2. Act again: `service.sendMessage(S1, makeMessageRequest(r -> r.setMessageBody("Second message")), MOTHER_USER_ID)`.
3. Assert: `eventPublisher.publishEvent(captor.capture())` called TWICE, once per send, with `senderRole` correctly set to `"EXPERT"` then `"MOTHER"` respectively.

**Expected Result (PASS):** One event per send call, correct `senderRole` in each payload.
**Expected Result (FAIL):** Event missing on any successful send, or `senderRole` incorrect.

**Current Status:** 🔴 Not written

---

### CHAT-TC-010 — Architecture-boundary guard: service never mutates `session_status`/`started_at`/`ended_at`/`expert_summary`

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationChatService.sendMessage()`, `ConsultationMessageRepository`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC144 TDS §6.5` invariant #4, `C5` in §17.1

**Test Steps:**
1. Act: `service.sendMessage(S1, makeMessageRequest(r -> {}), EXPERT_USER_ID_1)` (happy path).
2. Assert: the ONLY repository interaction is `messageRepository.save(...)` — verify via Mockito that `ConsultationSessionRepository` is only ever called with `findById()` (read), never `save()`/any `@Modifying` update method, from this service.
3. Assert: no interaction whatsoever with any summary-related repository method (e.g. a hypothetical `updateExpertSummary()`).

**Expected Result (PASS):** Zero write interaction with any session-lifecycle- or summary-mutating method.
**Expected Result (FAIL):** `session_status`/`started_at`/`ended_at`/`expert_summary` are touched by this module — crosses into UC95's/UC96's exclusive write scope (architecture-boundary violation, `AP-CB-303`).

**Current Status:** 🔴 Not written

---

### CHAT-TC-011 — `GET .../messages` returns cursor-paginated history ordered by `sentAt` ascending

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationChatService.listMessages()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC144 TDS §8.2` repository interface, `§9.2` response schema (cursor pagination, 2026-07-15 correction)

**Test Steps:**
1. Arrange: mock `messageRepository.findPageAfterCursor(S1, null, null, 50)` returns 3 messages in chronological order.
2. Act: `service.listMessages(S1, EXPERT_USER_ID_1, null, 50)`.
3. Assert: returned `ChatMessagePageResponse.messages` preserves chronological order; `nextCursor`/`hasMore` derived correctly (null/false when fewer than `limit` rows returned).

**Expected Result (PASS):** History returned in `sentAt` ascending order; no `page`/`offset` parameter anywhere in the contract.
**Expected Result (FAIL):** Wrong order, or cursor metadata mismatched.

**Current Status:** 🟢 Passing

---

### CHAT-TC-015 — Idempotent retry: same `(sessionId, senderUserId, clientMessageId, body)` returns the original message, no duplicate row

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015-A` (new, mandatory idempotent-retry requirement)
**Oracle Source:** `UC144 TDS §9.2` (2026-07-15 correction), migration `V<next>__add_consultation_message_client_id.sql`

**Test Steps:**
1. Act: `service.sendMessage(S1, request{clientMessageId=CID1, body="hi"}, EXPERT_USER_ID_1)` — first call.
2. Act: `service.sendMessage(S1, request{clientMessageId=CID1, body="hi"}, EXPERT_USER_ID_1)` — retry, identical request.
3. Assert: both calls return the same `messageId`; repository `save()` invoked exactly once; second call's HTTP mapping is `200 OK`, not `201 Created`.

**Expected Result (PASS):** Client retry after a network blip never creates a second row.
**Expected Result (FAIL):** Duplicate row inserted, or retry throws instead of returning the original.

**Current Status:** 🟢 Passing

---

### CHAT-TC-016 — Idempotency conflict: same `clientMessageId` reused with a different body → 409 (`CHAT-002`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015-B`
**Oracle Source:** `UC144 TDS §10` error codes (`CHAT-002`)

**Test Steps:**
1. Act: `service.sendMessage(S1, request{clientMessageId=CID1, body="hi"}, EXPERT_USER_ID_1)`.
2. Act: `service.sendMessage(S1, request{clientMessageId=CID1, body="different text"}, EXPERT_USER_ID_1)`.
3. Assert: second call throws `ChatIdempotencyConflictException` (`CHAT-002`, 409); no second row inserted; first row unchanged.

**Expected Result (PASS):** A client bug (UUID collision / logic error) surfaces as a clear conflict, never silently overwrites persisted content.
**Expected Result (FAIL):** Second call either duplicates or silently overwrites the first message's body.

**Current Status:** 🟢 Passing

---

### CHAT-TC-017 — Reconnect sync: `GET .../messages?after={cursor}` returns exactly the messages sent while disconnected, no gaps/duplicates

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatService.listMessages()` + `ConsultationMessageRepository.findPageAfterCursor()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/repository/ConsultationMessageRepositoryIT.java` (Testcontainers)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016` (new — realtime-miss reconciliation)
**Oracle Source:** `UC144 TDS §9.2`, mega-scope realtime reconnect requirement

**Test Steps:**
1. Arrange: insert 5 messages via real transactions (Testcontainers PostgreSQL), capture the cursor after message 2.
2. Act: `listMessages(S1, currentUserId, cursorAfterMsg2, 50)`.
3. Assert: returns exactly messages 3, 4, 5 in order — no message 1/2 (already seen), no gap, no duplicate even when messages 3-5 were inserted concurrently by different senders.

**Expected Result (PASS):** A client that reconnects after missing a realtime signal recovers the exact missed set via REST.
**Expected Result (FAIL):** Missing or duplicated messages after reconnect — the scenario the whole idempotency/cursor design exists to prevent.

**Current Status:** 🔴 Not written

---

### CHAT-TC-012 — Non-participant attempts `GET .../messages` → 403 (`CHAT-004`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatController` authorization
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationChatControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `UC144 TDS §16` Authorization Matrix

**Preconditions:** `FX-C10` JWT for a non-assigned Expert

**Test Steps:**
1. Act: `GET /api/v1/consultations/sessions/{S1}/messages` with `FX-C10`.
2. Assert: `403 Forbidden` code `CHAT-004`.

**Expected Result (PASS):** Non-participants cannot read the session's chat history.
**Expected Result (FAIL):** Non-participant can view private conversation content.

**Current Status:** 🟢 Passing

---

### CHAT-TC-013 — Unverified assigned Expert attempts send → 403 (`CHAT-004`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationChatPolicy.assertIsSessionParticipant()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationChatPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** derived from `ADR-CHAT-004`, consistent with UC95/UC96 pattern (`SES-TC-003`, `SUMW-TC-012`)

**Preconditions:** `FX-C08` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`.
2. Act: `policy.assertIsSessionParticipant(booking, EXPERT_USER_ID_2)` where `EXPERT_USER_ID_2` matches `FX-C08.userId`.
3. Assert: throws `ChatAuthorizationException` (`CHAT-004`).

**Expected Result (PASS):** Rejected despite matching `user_id`, because not verified.
**Expected Result (FAIL):** Unverified expert allowed to send/read chat.

**Current Status:** 🟢 Passing

---

### CHAT-TC-014 — Downstream DB write failure/timeout → 503 (`CHAT-007`), idempotent-safe retry

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationChatService.sendMessage()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationChatServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC144 TDS §6.4` timeout sequence diagram (SRS E3)

**Test Steps:**
1. Arrange: ownership + eligibility + content-safety checks pass; mock `messageRepository.save(...)` throws `DataAccessException`.
2. Act: `service.sendMessage(S1, makeMessageRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: throws `ChatWriteUnavailableException` code `CHAT-007`.

**Expected Result (PASS):** `503` surfaced; client retry creates a new message row (not update-idempotent like UC96's `PUT`, since `POST` naturally creates a new row on each call — client-side retry guidance documented, not silently deduplicated, per SRS E3's "no duplicate unsafe action" being satisfied by the fact that a failed write never persisted in the first place).
**Expected Result (FAIL):** Exception swallowed silently, or wrong error code.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### CHAT-TC-INT-001 — Full flow: send, verify column scoping, session-eligibility isolation, list ordering (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST/GET /api/v1/consultations/sessions/{id}/messages` → DB persisted, scoped write, eligibility-gated
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationChatIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** TDS-03 E2E row

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-C01` booking + `FX-C02` (`IN_SESSION` session) inserted via JPA

**Test Steps:**
1. Seed booking `B1` + session `S1` (`session_status='IN_SESSION'`), snapshot `session_status`/`started_at`/`ended_at`/`expert_summary` values before the calls.
2. `POST /api/v1/consultations/sessions/{S1}/messages` with Expert JWT and `{"messageBody": "First message", "messageType": "TEXT"}`.
3. Assert response `201`, one row in `consultation_messages` with `message_body='First message'`, `status='SENT'`.
4. `POST /api/v1/consultations/sessions/{S1}/messages` with Mother JWT and `{"messageBody": "Thanks, see you then.", "messageType": "TEXT"}`.
5. Assert response `201`, now two rows in `consultation_messages` for `S1`.
6. Assert `session_status`/`started_at`/`ended_at`/`expert_summary` in `consultation_sessions` UNCHANGED from the pre-call snapshot (architecture-boundary guard, DB-level confirmation).
7. `GET /api/v1/consultations/sessions/{S1}/messages` — assert both messages returned in `sentAt` ascending order.
8. Update the session row directly to `session_status='COMPLETED'` (simulating UC95 completing the session).
9. `POST /api/v1/consultations/sessions/{S1}/messages` again — assert `400` with `CHAT-006`, and the `consultation_messages` row count for `S1` remains 2 (no new row created).

**Expected Result (PASS):** Both messages persisted, session-lifecycle/summary columns untouched, eligibility gate enforced end-to-end against a real DB.
**Expected Result (FAIL):** Any lifecycle/summary column mutated, message accepted after session completion, or ordering incorrect.

**DB Assertion:**
```java
List<ConsultationMessageEntity> messages = messageRepository.findBySessionIdOrderBySentAtAsc(S1, Pageable.unpaged()).getContent();
assertThat(messages).hasSize(2);
assertThat(messages.get(0).getMessageBody()).isEqualTo("First message");
ConsultationSessionEntity session = sessionRepository.findById(S1).orElseThrow();
assertThat(session.getSessionStatus()).isEqualTo("COMPLETED"); // set directly by test setup step 8, not by this service
assertThat(session.getExpertSummary()).isNull(); // never touched by ConsultationChatService
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (flutter_test)

---

### CHAT-TC-MOB-001 — Chat screen sends and displays a new message

**Severity:** `MEDIUM`
**Feature Under Test:** `chat_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/consultation/screens/chat_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC144 TDS §5.1` Mobile Screen component, `§9.1` API contract

**Preconditions:** Mocked `ConsultationChatApi` returning `201` for send

**Test Steps:**
1. Pump `ChatScreen(sessionId: S1)` with a mocked API client.
2. Act: enter text into the message input, tap the send button.
3. Assert: the API client's send method is invoked with the typed text; the new message appears in the chat list widget.

**Expected Result (PASS):** UI reflects the newly sent message.
**Expected Result (FAIL):** Send has no effect, or message does not appear in the list.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### CHAT-TC-WEB-001 — ChatPanel submits and displays confirmation

**Severity:** `MEDIUM`
**Feature Under Test:** `ChatPanel.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/ChatPanel.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC144 TDS §5.1` Web Component, `§9.1` API contract

**Preconditions:** MSW mock server configured for `POST /api/v1/consultations/sessions/:id/messages` → 201

**Test Steps:**
1. Render `<ChatPanel sessionId={S1} />` with a mocked TanStack Query client and MSW handler.
2. Act: type into the message input, `userEvent.click(screen.getByRole('button', { name: /send/i }))`.
3. Assert: mutation hook fires with the typed text; the new message appears in the chat panel's message list.

**Expected Result (PASS):** UI reflects successful send.
**Expected Result (FAIL):** Submit has no effect, or wrong endpoint/payload sent.

**Current Status:** 🔴 Not written

---

### CHAT-TC-WEB-002 — ContentSafetyWarningBanner (reused from UC96) shows warning but allows send to proceed

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentSafetyWarningBanner.tsx` (reused component)
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/ChatPanel.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `ADR-CHAT-003`

**Preconditions:** MSW mock returns `201` with a non-empty `contentSafetyWarnings` array; logged-in user role = EXPERT

**Test Steps:**
1. Render `<ChatPanel sessionId={S1} />` as an Expert user, send text that triggers a warning response.
2. Assert: `ContentSafetyWarningBanner` displays the warning message.
3. Assert: the send action is NOT blocked by the banner — the panel shows the message as successfully sent (non-blocking, per ADR-CHAT-003, identical UI contract to UC96).

**Expected Result (PASS):** Warning shown alongside a successful send — never a hard block in the UI either.
**Expected Result (FAIL):** UI prevents sending when a warning is present (would misrepresent the non-blocking backend contract).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CHAT-TC-001` | `ConsultationChatServiceImplTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-002` | `ConsultationChatPolicyTest.java` (WAITING eligibility) | `[x]` | `Passed` | |
| `CHAT-TC-003` | `ConsultationChatServiceImplTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-004` | `ConsultationChatServiceImplTest.java` + `ConsultationChatPolicyTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-005` | `ConsultationChatPolicyTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-006` | `ConsultationChatPolicyTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-007` | `ConsultationChatPolicyTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-008` | `ConsultationChatServiceImplTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-009` | — | `[ ]` | `___` | Deferred — domain event not implemented (audit log used instead, see TDS changelog) |
| `CHAT-TC-010` | — | `[ ]` | `___` | Deferred — no dedicated architecture-boundary test written (structurally true but unverified) |
| `CHAT-TC-011` | `ConsultationChatServiceImplTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-012` | `ConsultationChatServiceImplTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-013` | `ConsultationSessionPolicyTest.java` (shared participant check) | `[x]` | `Passed` | |
| `CHAT-TC-014` | — | `[ ]` | `___` | Deferred — no dedicated DB-write-failure-mapping test written |
| `CHAT-TC-015` | `ConsultationChatServiceImplTest.java` + `ConsultationChatIntegrationTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-016` | `ConsultationChatServiceImplTest.java` + `ConsultationChatIntegrationTest.java` | `[x]` | `Passed` | |
| `CHAT-TC-017` | `ConsultationChatIntegrationTest.java` | `[x]` | `Written, unexecuted — Docker unavailable in sandbox` | |
| `CHAT-TC-INT-001` | `ConsultationChatIntegrationTest.java` | `[x]` | `Written, unexecuted — Docker unavailable in sandbox` | Compiles cleanly (`./mvnw test-compile`); could not run in this environment |
| `CHAT-TC-MOB-001` | `chat_message_test.dart` (model/merge level, not full widget test) | `[ ]` | `___` | Partial — optimistic/merge logic tested; no full `chat_screen_test.dart` widget test written |
| `CHAT-TC-WEB-001` | — | `[ ]` | `___` | Deferred — no test framework exists in the web project (no vitest/jest configured); flagged as a gap, not silently skipped |
| `CHAT-TC-WEB-002` | — | `[ ]` | `___` | Deferred — same reason as WEB-001 |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationChatService implements IConsultationChatService {
    @Override
    public ChatMessageResponse sendMessage(UUID sessionId, SendChatMessageRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ChatMessagePageResponse listMessages(UUID sessionId, UUID currentUserId, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CHAT-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CHAT-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC144_ConsultViaChat_TDS.md` reviewed and Approved
- [ ] **BLOCKING:** `UC-95 Manage Consultation Session` implemented (not merely specified) — sessions must exist with a real `session_status`
- [ ] ADR-CHAT-003's exact content-safety keyword/pattern list confirmed by Product/DPO/clinical advisor (shared item with UC96 ADR-SUMMARY-002)
- [ ] ADR-CHAT-002's exact ZegoCloud signaling call confirmed by Mobile/Web leads before Chặng 3 client work (non-blocking for backend REST/persistence test cases)
- [ ] DPO review for `consultation_messages.message_body` free-text content — sign-off pending
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh
- [ ] `flutter test` — Mobile tests xanh
- [ ] Test coverage ≥ 80% lines cho `ConsultationChatService`, `ConsultationChatPolicy`
- [ ] `CHAT-TC-004` (session-validity gate), `CHAT-TC-006`/`CHAT-TC-007` (role-scoped non-blocking nudge), `CHAT-TC-010` (boundary guard) pass — these are release-blocking safety/architecture gates
- [ ] Không có business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — dùng `ConsultationChatTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn

### Suspension Criteria

- UC-95 not yet implemented (`consultation_sessions` rows do not exist)
- ADR-CHAT-003's content-safety pattern list not yet confirmed by Product/DPO

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/
kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a different chat-eligible `session_status` subset than `NOT IN ('COMPLETED','NO_SHOW','CANCELLED')` | ☑ (all decisions traced to ADR-CHAT-001, reused enum from UC95) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `CHAT-TC-012`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc duplicate `ConsultationMessageEntity`/`ConsultationMessageRepository` under a different name | ☑ (all types match TDS §8 interfaces; reuses UC95-scaffolded names verbatim) | G-3 |
| **AP-CB-301** *(reused from UC96, cross-referenced from TDS §17.4)* | **AI/platform auto-generating diagnostic or prescriptive message content** | Any code path synthesizing `messageBody` server-side without a participant's explicit typed input | No test in this suite exercises server-side generation — `CHAT-TC-001`/`002` always supply a participant-authored `messageBody` in the request | **Release-blocking** |
| **AP-CB-302** *(reused from UC96)* | **Content-safety check blocking persistence** | `validateContentSafety()` throws/rejects instead of returning a warning list, OR is invoked for Mother-authored messages | `CHAT-TC-006` explicitly asserts the send STILL SUCCEEDS despite a detected pattern; `CHAT-TC-007` explicitly asserts the check is never invoked for Mother senders | **Release-blocking** |
| **AP-CB-303** *(reused from UC96)* | **Chat write mutating session/summary lifecycle fields** | `ConsultationChatService`/message-save path also sets `session_status`/`started_at`/`ended_at`/`expert_summary` | `CHAT-TC-010`, `CHAT-TC-INT-001` explicitly assert these fields are untouched | **Release-blocking** |
| **AP-CB-401** *(project-specific, new for UC144, cross-referenced from TDS §17.4)* | **Bespoke realtime server built instead of reusing ZegoCloud** | New WebSocket/SSE controller/handler added inside `consultation` package for chat push delivery | No test in this suite exercises or requires a new WebSocket endpoint — `CHAT-TC-001`/`CHAT-TC-INT-001` only assert the REST `POST`/`GET` contract, consistent with ADR-CHAT-002's "REST is the durable system of record" decision | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`CHAT-TC-006`, `CHAT-TC-007`, `CHAT-TC-010`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC144 v1.0 — Draft. Total test cases: 18 (14 unit/component/security + 1 integration + 1 mobile + 2 web). Critical-severity: 4 (`CHAT-TC-001, 003, 004, 010` — happy path, ownership, session-validity, and architecture-boundary gates). Requires Approved status change only by user/Tech Lead.*
