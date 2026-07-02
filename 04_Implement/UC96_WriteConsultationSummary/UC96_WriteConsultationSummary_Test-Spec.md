# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC96 — Write Consultation Summary — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-096`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L898-909, L1425-1426, L1838-1839) — primary schema source
- `04_Implement/UC96_WriteConsultationSummary/UC96_WriteConsultationSummary_TDS.md` (`CB-CONSULTATION-IMP-096`) — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` §ADR-SESSION-001 — upstream, confirmed `session_status='COMPLETED'` terminal value (reused verbatim)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_Test-Spec.md` — sibling spec, same batch, style reference
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.10 (L943-962) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, audit, "AI provides guidance only; never diagnose, prescribe" mandate)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC96 |

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
| **Feature / Gap ID** | `UC-96` |
| **Module** | `Consultation — Post-Session Summary` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-096` |
| **Priority** | 🔴 High |
| **Sprint** | `Sprint 4 "Real Providers And Admin Polish"` — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `Sensitive-PII` (free-text summary referencing a named Mother's health-consultation context) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION`, `BR-SAFETY` |
| **Upstream Dependencies** | `UC-95 Manage Consultation Session` — session must reach `session_status='COMPLETED'` (reused terminal value, `ADR-SESSION-001`) |
| **Downstream Consumers** | Mother-facing consultation history view (out of scope), `UC-79 Review Expert After Consultation` (shares the same completion gate, no code dependency) |
| **Platform** | Backend (Java 21/Spring Boot) + Web (React/TypeScript/Vite) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC96_WriteConsultationSummary_TDS.md §17`, ADR-SUMMARY-001/002/003 |
| **Constraints Injected** | Session-completion precondition reusing UC95's confirmed value (C1), ownership (C2), non-blocking content-safety nudge (C3), no AI-generated summary content (C4), scoped write — never touches session-lifecycle fields (C5) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-96 does not name the session-completion terminal value | `UC95 TDS ADR-SESSION-001` confirms `'COMPLETED'` as definitive — UC96 reuses it verbatim, no new assumption | Tests assert against the literal string `'COMPLETED'`, sourced from UC95's Test-Spec (`SES-TC-006`), not re-derived independently — guarantees byte-for-byte cross-document consistency |
| L2 | SRS "safe next steps" wording is ambiguous between a hard content filter and a soft nudge | `ADR-SUMMARY-002` selects a NON-BLOCKING advisory warning (Option B), explicitly rejecting a hard reject (Option C) to avoid over-blocking a licensed Expert's professional judgment | Tests assert `contentSafetyWarnings` may be non-empty while the write STILL SUCCEEDS (`SUMW-TC-005`) — a test asserting rejection on detected content would fail Red Gate incorrectly and must not be written |
| L3 | `expert_summary` has no version history column | TDS §5.3 gap note 1 proposes the `ConsultationSummaryWritten` event log as the audit trail, not a new migration | Tests assert the event is emitted on EVERY write (including overwrite/re-submission), not just the first — this is how "history" is reconstructed per the TDS's documented approach |
| L4 | No DB `CHECK` constraint on `expert_summary` length | `WriteConsultationSummaryRequest.summaryText` has `@Size(max=5000)` — app-level only | `SUMW-TC-004` boundary-tests exactly at 5000/5001 chars, since the DB itself will not reject an oversized value |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Post-Session Summary bao gồm các layer:
├── Domain (ConsultationSummaryPolicy — pure logic, no deps)
├── Application / Use Cases (ConsultationSummaryService — mock JPA Repository với Mockito)
├── Controller (ConsultationSummaryController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full write + session-status-isolation flow)
└── Web (WriteSummaryPage.tsx, ContentSafetyWarningBanner.tsx — Vitest + Testing Library, MSW)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-96` (§3.2.1.10, L943-962) | Writes consultation summary and safe next steps for user review |
| `ADR-SUMMARY-001` | Session-completion precondition, reusing UC95's confirmed `'COMPLETED'` |
| `ADR-SUMMARY-002` | Content-safety: non-blocking advisory nudge only |
| `ADR-SUMMARY-003` | Ownership — assigned, verified Expert only |
| `BR-RBAC` / `BR-SAFETY` | Role/ownership-scoped, no diagnostic/prescriptive platform behavior |
| `CB-CONSULTATION-IMP-096 §9/§10/§13/§16` | API contract, error codes, test-condition summary, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — assigned Expert writes summary after session `COMPLETED` | `ConsultationSummaryService.writeSummary()` | `SUMW-TC-001` |
| TC-COND-002 | Ownership violation — non-assigned Expert attempts write → 403 (`SUMW-004`) | `ConsultationSummaryPolicy.assertIsAssignedExpert()` | `SUMW-TC-002` |
| TC-COND-003 | Session not `COMPLETED` (e.g. `IN_SESSION`) → 400 (`SUMW-006`) | `ConsultationSummaryPolicy.assertSessionCompleted()` | `SUMW-TC-003` |
| TC-COND-004 | Boundary: `summaryText` blank / 5000 / 5001 chars → 400 (`SUMW-001`) | DTO validation | `SUMW-TC-004` |
| TC-COND-005 | Content with dosage-like pattern → 200 OK with non-empty `contentSafetyWarnings` (non-blocking) | `ConsultationSummaryPolicy.validateContentSafety()` | `SUMW-TC-005` |
| TC-COND-006 | Content with normal informational language → 200 OK, empty `contentSafetyWarnings` | `ConsultationSummaryPolicy.validateContentSafety()` | `SUMW-TC-006` |
| TC-COND-007 | Re-submission overwrites previous `expertSummary` (idempotent PUT semantics) | `ConsultationSummaryService.writeSummary()` | `SUMW-TC-007` |
| TC-COND-008 | Session not found → 404 (`SUMW-003`) | `ConsultationSummaryService.writeSummary()` | `SUMW-TC-008` |
| TC-COND-009 | `ConsultationSummaryWritten` event emitted on EVERY successful write (incl. overwrite) | `ConsultationSummaryService` | `SUMW-TC-009` |
| TC-COND-010 | Service never mutates `session_status`/`started_at`/`ended_at` (architecture-boundary guard) | `ConsultationSummaryService`, `ConsultationSessionRepository.updateExpertSummary()` | `SUMW-TC-010` |
| TC-COND-011 | Mother (booking owner) can read summary via `GET`, cannot write via `PUT` | `ConsultationSummaryController` authorization | `SUMW-TC-011` |
| — | Unverified assigned Expert attempts write → 403 (`SUMW-004`) | `ConsultationSummaryPolicy.assertIsAssignedExpert()` | `SUMW-TC-012` |
| — | Downstream DB write failure/timeout → 503 (`SUMW-007`), idempotent retry safe | `ConsultationSummaryService.writeSummary()` | `SUMW-TC-013` |
| — | Full flow via Testcontainers — write, verify DB column scoping, re-write overwrite | `ConsultationSummaryController` + real DB | `SUMW-TC-INT-001` |
| — | Web: WriteSummaryPage submits and displays confirmation | `WriteSummaryPage.tsx` | `SUMW-TC-WEB-001` |
| — | Web: ContentSafetyWarningBanner shows warning but allows submit to proceed | `ContentSafetyWarningBanner.tsx` | `SUMW-TC-WEB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Boundary Value Analysis | `summaryText` length (0, 5000, 5001 chars) | Confirms `@NotBlank @Size(max=5000)` boundary — the only enforcement, since DB has no length CHECK |
| State Transition Testing | Precondition gate on `session_status` (only `COMPLETED` accepted) | Core invariant shared across UC79/UC95/UC96 |
| Decision Table | Ownership × session-status × content-safety-flag combinations | 200 (clean) vs 200 (warned) vs 400 (precondition) vs 403 (ownership) branching |
| Error Guessing | DB write timeout, cross-module boundary violation (service mutating fields it shouldn't) | External-service/DB failure and layer-discipline are explicitly in scope |
| Non-Functional (Advisory) Testing | Content-safety check must warn, never block | Direct verification of `ADR-SUMMARY-002`'s core design decision |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-W01` | DB seed | `consultation_bookings{booking_id: B1, expert_profile_id: EXPERT-001, requester_user_id: MOTHER-001}` | Happy path booking |
| `FX-W02` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'COMPLETED'}` | Precondition satisfied |
| `FX-W03` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'IN_SESSION'}` | Precondition NOT satisfied (negative) |
| `FX-W04` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, verification_status: 'VERIFIED'}` | Assigned, verified Expert |
| `FX-W05` | DB seed | `expert_profiles{expert_profile_id: EXPERT-002, user_id: USER-EXPERT-002, verification_status: 'PENDING'}` | Unverified Expert (negative) |
| `FX-W06` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — assigned |
| `FX-W07` | JWT | `{sub: 'USER-EXPERT-999', role: 'EXPERT'}` | Auth context — non-assigned |
| `FX-W08` | JWT | `{sub: 'MOTHER-001', role: 'MOTHER'}` | Auth context — booking owner (read-only) |
| `FX-W09` | Request body | `summaryText: "...take 500mg twice daily..."` | Content-safety-flagged text |
| `FX-W10` | Request body | `summaryText: "Discussed nutrition, recommended continued monitoring."` | Clean, informational text |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership, boundary-write scoping) |
| Web (React) | — | — | ✅ (Vitest) | ✅ (Testing Library) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Reuses UC95's ConsultationSessionTestFactory conventions where
// entities overlap (ConsultationSessionEntity is shared with UC95).
// ═══════════════════════════════════════════════════════════

class ConsultationSummaryTestFactory {

    static final UUID BOOKING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setRequesterUserId(MOTHER_USER_ID);
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeCompletedSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus("COMPLETED"); // ADR-SUMMARY-001 — reused verbatim from UC95 ADR-SESSION-001
        overrides.accept(session);
        return session;
    }

    static WriteConsultationSummaryRequest makeSummaryRequest(Consumer<WriteConsultationSummaryRequest> overrides) {
        WriteConsultationSummaryRequest request = new WriteConsultationSummaryRequest();
        request.setSummaryText("Discussed second-trimester nutrition and mild fatigue. Recommended continued monitoring.");
        overrides.accept(request);
        return request;
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

### SUMW-TC-001 — Happy path: assigned Expert writes summary after session `COMPLETED`

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC96 TDS §6.1` sequence diagram, `ADR-SUMMARY-001/003`

**Preconditions:** `FX-W01`, `FX-W02` (`COMPLETED`), `FX-W04` (verified Expert)

**Test Steps:**
1. Arrange: mock `sessionRepository.findById(S1)` returns `FX-W02`; mock `bookingRepository.findById(B1)` returns `FX-W01`.
2. Act: `service.writeSummary(S1, ConsultationSummaryTestFactory.makeSummaryRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: `sessionRepository.updateExpertSummary(S1, summaryText)` invoked once; response `contentSafetyWarnings` empty.

**Expected Result (PASS):** `200`-equivalent response, summary persisted via the scoped update method.
**Expected Result (FAIL):** Exception thrown, or wrong repository method invoked.

**Current Status:** 🔴 Not written

---

### SUMW-TC-002 — Ownership violation: non-assigned Expert attempts write → 403 (`SUMW-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ConsultationSummaryPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SUMMARY-003`

**Preconditions:** `FX-W01` booking assigned to `EXPERT_PROFILE_ID_1`; caller = `OTHER_EXPERT_USER_ID`

**Test Steps:**
1. Arrange: `booking = ConsultationSummaryTestFactory.makeBooking(b -> {})`.
2. Act: `policy.assertIsAssignedExpert(booking, OTHER_EXPERT_USER_ID)`.
3. Assert: throws `SummaryAuthorizationException` code `SUMW-004`.

**Expected Result (PASS):** Exception thrown; no write occurs.
**Expected Result (FAIL):** Summary written by a non-assigned Expert.

**Current Status:** 🔴 Not written

---

### SUMW-TC-003 — Session not `COMPLETED` → 400 (`SUMW-006`)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSummaryPolicy.assertSessionCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SUMMARY-001`, `UC96 TDS §6.3` error-path sequence diagram

**Preconditions:** `FX-W03` (`session_status='IN_SESSION'`)

**Test Steps:**
1. Arrange: `session = ConsultationSummaryTestFactory.makeCompletedSession(s -> s.setSessionStatus("IN_SESSION"))`.
2. Act: `policy.assertSessionCompleted(session)`.
3. Assert: throws `SummaryPreconditionException` code `SUMW-006`; no `expert_summary` write occurs (verified at Service level via `verifyNoInteractions(sessionRepository.updateExpertSummary(...))` in the accompanying Service-layer test).

**Expected Result (PASS):** Exception thrown before persistence — mirrors UC79's `REV-006` precondition-guard pattern for consistency.
**Expected Result (FAIL):** Summary written for a session that hasn't completed.

**Current Status:** 🔴 Not written

---

### SUMW-TC-004 — Boundary: `summaryText` length (0 / 5000 / 5001 chars)

**Severity:** `MEDIUM`
**Feature Under Test:** `WriteConsultationSummaryRequest` DTO validation (`@NotBlank @Size(max=5000)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/WriteConsultationSummaryRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC96 TDS §8.1` DTO annotation; `§5.3` genuine schema gap (no DB length limit on `expert_summary`)

**Test Steps:**
1. Act (blank): validate `makeSummaryRequest(r -> r.setSummaryText(""))`.
2. Assert: violation on `summaryText` (`@NotBlank`).
3. Act (5000 chars): validate `makeSummaryRequest(r -> r.setSummaryText("a".repeat(5000)))`.
4. Assert: no violation.
5. Act (5001 chars): validate `makeSummaryRequest(r -> r.setSummaryText("a".repeat(5001)))`.
6. Assert: violation on `summaryText` (`@Size(max=5000)`).

**Expected Result (PASS):** Boundary respected exactly at 5000/5001; blank rejected.
**Expected Result (FAIL):** Off-by-one on boundary, or an oversized value silently persisted (critical since DB has no CHECK constraint — this test is the ONLY enforcement).

**Current Status:** 🔴 Not written

---

### SUMW-TC-005 — Content with dosage-like pattern → 200 OK with non-empty `contentSafetyWarnings` (non-blocking)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryPolicy.validateContentSafety()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SUMMARY-002` — non-blocking advisory nudge (Option B, explicitly rejects Option C hard-block)

**Preconditions:** `FX-W09` (`"...take 500mg twice daily..."`)

**Test Steps:**
1. Act: `policy.validateContentSafety(FX-W09.summaryText)`.
2. Assert: returns a non-empty `List<String>` warning list.
3. Act (integration-level, in Service test): `service.writeSummary(S1, makeSummaryRequest(r -> r.setSummaryText(FX-W09.summaryText)), EXPERT_USER_ID_1)`.
4. Assert: write STILL SUCCEEDS (`sessionRepository.updateExpertSummary()` IS invoked) — the warning is surfaced in the response, never blocks persistence.

**Expected Result (PASS):** Warning present AND summary saved — this is the release-blocking assertion for ADR-SUMMARY-002.
**Expected Result (FAIL):** Write rejected/blocked due to detected content (violates AP-CB-302, §8) — a test asserting rejection here would itself be a spec defect per Logic Issue L2.

**Current Status:** 🔴 Not written

---

### SUMW-TC-006 — Content with normal informational language → 200 OK, empty `contentSafetyWarnings`

**Severity:** `LOW`
**Feature Under Test:** `ConsultationSummaryPolicy.validateContentSafety()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC96 TDS §6.1` happy-path sequence diagram

**Preconditions:** `FX-W10` (clean informational text)

**Test Steps:**
1. Act: `policy.validateContentSafety(FX-W10.summaryText)`.
2. Assert: returns an empty `List<String>`.

**Expected Result (PASS):** No false-positive warning on ordinary informational language.
**Expected Result (FAIL):** Warning incorrectly raised on benign content.

**Current Status:** 🔴 Not written

---

### SUMW-TC-007 — Re-submission overwrites previous `expertSummary` (idempotent PUT semantics)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC96 TDS §9.1` — `PUT` idempotent overwrite semantics

**Test Steps:**
1. Act: `service.writeSummary(S1, makeSummaryRequest(r -> r.setSummaryText("First version")), EXPERT_USER_ID_1)`.
2. Act again: `service.writeSummary(S1, makeSummaryRequest(r -> r.setSummaryText("Revised version")), EXPERT_USER_ID_1)`.
3. Assert: `sessionRepository.updateExpertSummary()` invoked twice, second call with `"Revised version"`; no duplicate-submission error (unlike UC79's `REV-002` uniqueness rule — this endpoint intentionally allows overwrite).

**Expected Result (PASS):** Second call succeeds and overwrites; no conflict error.
**Expected Result (FAIL):** Second call rejected, or both versions somehow coexist (schema only has one `expert_summary` column, so coexistence is structurally impossible — this guards against an incorrect service-level uniqueness check being added by mistake).

**Current Status:** 🔴 Not written

---

### SUMW-TC-008 — Session not found → 404 (`SUMW-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC96 TDS §10` error table

**Test Steps:**
1. Arrange: mock `sessionRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `service.writeSummary(nonExistentId, makeSummaryRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: throws `SessionNotFoundException` code `SUMW-003`.

**Current Status:** 🔴 Not written

---

### SUMW-TC-009 — `ConsultationSummaryWritten` event emitted on EVERY successful write (incl. overwrite)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC96 TDS §7.1/§7.3`, `§5.3 gap note 1` (event log serves as the audit/history trail)

**Test Steps:**
1. Act: `service.writeSummary(S1, makeSummaryRequest(r -> r.setSummaryText("First")), EXPERT_USER_ID_1)`.
2. Act again: `service.writeSummary(S1, makeSummaryRequest(r -> r.setSummaryText("Second")), EXPERT_USER_ID_1)`.
3. Assert: `eventPublisher.publishEvent(captor.capture())` called TWICE, once per write, each with the exact `summaryText` value written in that call (payload includes full text per §7.3, since it is the history mechanism).

**Expected Result (PASS):** One event per write call, including overwrites — this is how "history" is reconstructed per §5.3 gap note 1.
**Expected Result (FAIL):** Event only fires on first write, or payload doesn't match the actual persisted text.

**Current Status:** 🔴 Not written

---

### SUMW-TC-010 — Architecture-boundary guard: service never mutates `session_status`/`started_at`/`ended_at`

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`, `ConsultationSessionRepository.updateExpertSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC96 TDS §6.5` invariant #4, `C5` in §17.1

**Test Steps:**
1. Act: `service.writeSummary(S1, makeSummaryRequest(r -> {}), EXPERT_USER_ID_1)` (happy path).
2. Assert: the ONLY repository interaction is `updateExpertSummary(sessionId, summaryText)` — verify via Mockito that no other `ConsultationSessionRepository` method (e.g., a hypothetical `updateStatus()` or generic `save()`) is ever invoked from this service.
3. Assert (via the `@Query` annotation on `updateExpertSummary`, code-review-level check documented here): the `UPDATE` statement's `SET` clause touches only `expert_summary` and `updated_at`.

**Expected Result (PASS):** Zero interaction with any session-lifecycle-mutating method.
**Expected Result (FAIL):** `session_status`/`started_at`/`ended_at` are touched by this module — crosses into UC95's exclusive write scope (architecture-boundary violation, `AP-CB-303`).

**Current Status:** 🔴 Not written

---

### SUMW-TC-011 — Mother (booking owner) can read summary via `GET`, cannot write via `PUT`

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryController` authorization
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC96 TDS §16` Authorization Matrix

**Preconditions:** `FX-W08` JWT for `MOTHER_USER_ID` (booking owner)

**Test Steps:**
1. Act (read): `GET /api/v1/consultations/sessions/{S1}/summary` with `FX-W08`.
2. Assert: `200 OK`.
3. Act (write): `PUT /api/v1/consultations/sessions/{S1}/summary` with `FX-W08` and a valid body.
4. Assert: `403 Forbidden` code `SUMW-004`.

**Expected Result (PASS):** Mother has read-only access; write remains Expert-exclusive.
**Expected Result (FAIL):** Mother can write/overwrite the expert's summary.

**Current Status:** 🔴 Not written

---

### SUMW-TC-012 — Unverified assigned Expert attempts write → 403 (`SUMW-004`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryPolicy.assertIsAssignedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `ADR-SUMMARY-003`, consistent with UC94/UC95 pattern (`SES-TC-003`, `SUM-TC-003`)

**Preconditions:** `FX-W05` (unverified expert profile) assigned to the booking

**Test Steps:**
1. Arrange: `expert_profiles.verification_status = 'PENDING'`.
2. Act: `policy.assertIsAssignedExpert(booking, EXPERT_USER_ID_2)`.
3. Assert: throws `SummaryAuthorizationException` (`SUMW-004`).

**Expected Result (PASS):** Rejected despite matching `user_id`, because not verified.
**Expected Result (FAIL):** Unverified expert allowed to write.

**Current Status:** 🔴 Not written

---

### SUMW-TC-013 — Downstream DB write failure/timeout → 503 (`SUMW-007`), idempotent retry safe

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSummaryService.writeSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC96 TDS §6.4` timeout sequence diagram (SRS E3)

**Test Steps:**
1. Arrange: ownership + precondition + content-safety checks pass; mock `sessionRepository.updateExpertSummary(...)` throws `DataAccessException`.
2. Act: `service.writeSummary(S1, makeSummaryRequest(r -> {}), EXPERT_USER_ID_1)`.
3. Assert: throws `SummaryWriteUnavailableException` code `SUMW-007`.

**Expected Result (PASS):** `503` surfaced; retry is safe since the `UPDATE` is naturally idempotent per `sessionId`.
**Expected Result (FAIL):** Exception swallowed silently, or wrong error code.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SUMW-TC-INT-001 — Full flow: write, verify column scoping, re-write overwrites (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `PUT /api/v1/consultations/sessions/{id}/summary` → DB persisted, scoped write
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSummaryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** TDS-03 E2E row

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-W01` booking + `FX-W02` (`COMPLETED` session) inserted via JPA

**Test Steps:**
1. Seed booking `B1` + session `S1` (`session_status='COMPLETED'`), snapshot `session_status`/`started_at`/`ended_at` values before the call.
2. `PUT /api/v1/consultations/sessions/{S1}/summary` with Expert JWT and `{"summaryText": "First version"}`.
3. Assert response `200`, `expert_summary` in DB equals `"First version"`.
4. Assert `session_status`/`started_at`/`ended_at` UNCHANGED from the pre-call snapshot (architecture-boundary guard, DB-level confirmation).
5. Repeat step 2 with `{"summaryText": "Revised version"}`.
6. Assert DB `expert_summary` now equals `"Revised version"` (overwrite confirmed) and row count for the session is still exactly 1 (no duplicate row).

**Expected Result (PASS):** Write succeeds, session-lifecycle columns untouched, overwrite semantics confirmed.
**Expected Result (FAIL):** Any lifecycle column mutated, or overwrite fails/duplicates.

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findById(S1).orElseThrow();
assertThat(record.getExpertSummary()).isEqualTo("Revised version");
assertThat(record.getSessionStatus()).isEqualTo("COMPLETED"); // unchanged
assertThat(record.getStartedAt()).isEqualTo(preCallStartedAt); // unchanged
assertThat(record.getEndedAt()).isEqualTo(preCallEndedAt); // unchanged
```

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### SUMW-TC-WEB-001 — WriteSummaryPage submits and displays confirmation

**Severity:** `MEDIUM`
**Feature Under Test:** `WriteSummaryPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/pages/WriteSummaryPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC96 TDS §5.1` Web Page component, `§9.1` API contract

**Preconditions:** MSW mock server configured for `PUT /api/v1/consultations/sessions/:id/summary` → 200

**Test Steps:**
1. Render `<WriteSummaryPage sessionId={S1} />` with a mocked TanStack Query client and MSW handler.
2. Act: type into the summary textarea, `userEvent.click(screen.getByRole('button', { name: /save summary/i }))`.
3. Assert: mutation hook fires with the typed text; UI shows a success confirmation.

**Expected Result (PASS):** UI reflects successful save.
**Expected Result (FAIL):** Submit has no effect, or wrong endpoint/payload sent.

**Current Status:** 🔴 Not written

---

### SUMW-TC-WEB-002 — ContentSafetyWarningBanner shows warning but allows submit to proceed

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentSafetyWarningBanner.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/consultationManagement/components/ContentSafetyWarningBanner.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `ADR-SUMMARY-002`

**Preconditions:** MSW mock returns `200` with a non-empty `contentSafetyWarnings` array

**Test Steps:**
1. Render `<WriteSummaryPage sessionId={S1} />`, submit text that triggers a warning response.
2. Assert: `ContentSafetyWarningBanner` displays the warning message.
3. Assert: the save action is NOT blocked by the banner — page shows the summary as successfully saved (non-blocking, per ADR-SUMMARY-002).

**Expected Result (PASS):** Warning shown alongside a successful save — never a hard block in the UI either.
**Expected Result (FAIL):** UI prevents submission when a warning is present (would misrepresent the non-blocking backend contract).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SUMW-TC-001` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-002` | `ConsultationSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-003` | `ConsultationSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-004` | `WriteConsultationSummaryRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-005` | `ConsultationSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-006` | `ConsultationSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-007` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-008` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-009` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-010` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-011` | `ConsultationSummaryControllerTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-012` | `ConsultationSummaryPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-013` | `ConsultationSummaryServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-INT-001` | `ConsultationSummaryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `SUMW-TC-WEB-001` | `WriteSummaryPage.test.tsx` | `[ ]` | `[ ]` | |
| `SUMW-TC-WEB-002` | `ContentSafetyWarningBanner.test.tsx` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationSummaryService implements IConsultationSummaryService {
    @Override
    public ConsultationSummaryResponse writeSummary(UUID sessionId, WriteConsultationSummaryRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ConsultationSummaryResponse getSummary(UUID sessionId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SUMW-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMW-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC96_WriteConsultationSummary_TDS.md` reviewed and Approved
- [ ] **BLOCKING:** `UC-95 Manage Consultation Session` implemented (not merely specified) — sessions must be able to reach `session_status='COMPLETED'`
- [ ] ADR-SUMMARY-002's exact content-safety keyword/pattern list confirmed by Product/DPO/clinical advisor
- [ ] DPO review for `expert_summary` free-text content — sign-off pending
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — Web tests xanh
- [ ] Test coverage ≥ 80% lines cho `ConsultationSummaryService`, `ConsultationSummaryPolicy`
- [ ] `SUMW-TC-003` (precondition gate), `SUMW-TC-005` (non-blocking nudge), `SUMW-TC-010` (boundary guard) pass — these are release-blocking safety/architecture gates
- [ ] Không có business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — dùng `ConsultationSummaryTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn

### Suspension Criteria

- UC-95 not yet implemented (`consultation_sessions` cannot reach `COMPLETED`)
- ADR-SUMMARY-002's content-safety pattern list not yet confirmed by Product/DPO

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/consultationManagement/
kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a different session-completion terminal value than `'COMPLETED'` | ☑ (all decisions traced to ADR-SUMMARY-001, reused from UC95) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `SUMW-TC-011`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc duplicate `ConsultationSessionEntity`/`ConsultationSessionRepository` | ☑ (all types match TDS §8 interfaces; extends UC95's existing repository) | G-3 |
| **AP-CB-301** *(project-specific, cross-referenced from TDS §17.4)* | **AI/platform auto-generating diagnostic or prescriptive summary content** | Any code path synthesizing `summaryText` server-side without an Expert's explicit typed input | No test in this suite exercises server-side generation — `SUMW-TC-001`/`007` always supply an Expert-authored `summaryText` in the request | **Release-blocking** |
| **AP-CB-302** *(project-specific)* | **Content-safety check blocking persistence** | `validateContentSafety()` throws/rejects instead of returning a warning list | `SUMW-TC-005` explicitly asserts the write STILL SUCCEEDS despite a detected pattern | **Release-blocking** |
| **AP-CB-303** *(project-specific)* | **Summary write mutating session lifecycle fields** | `ConsultationSummaryService`/`updateExpertSummary()` also sets `session_status`/`started_at`/`ended_at` | `SUMW-TC-010`, `SUMW-TC-INT-001` explicitly assert these fields are untouched | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`SUMW-TC-005`, `SUMW-TC-010`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC96 v1.0 — Draft. Total test cases: 16 (13 unit/component/security + 1 integration + 2 web). Critical-severity: 3 (`SUMW-TC-001, 003, 010` — precondition, ownership, and architecture-boundary gates). Requires Approved status change only by user/Tech Lead.*
