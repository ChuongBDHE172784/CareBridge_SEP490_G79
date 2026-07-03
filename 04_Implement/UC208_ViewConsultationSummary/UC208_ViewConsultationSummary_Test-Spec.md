# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-208 — View Consultation Summary — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-208`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L532-544 users, L786-800 expert_profiles, L876-909 bookings/sessions) — primary schema source
- `04_Implement/UC208_ViewConsultationSummary/UC208_ViewConsultationSummary_TDS.md` (`CB-CONSULTATION-IMP-208`) — Technical Design Specification (this feature)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` §ADR-SESSION-001 — upstream, confirmed `session_status='COMPLETED'` terminal value (reused verbatim, third consumer after UC96/UC79)
- `04_Implement/UC96_WriteConsultationSummary/UC96_WriteConsultationSummary_TDS.md` §ADR-SUMMARY-001/002 — upstream owner of `expert_summary` write + content-safety
- `04_Implement/UC202_ViewConsultationList/UC202_ViewConsultationList_Test-Spec.md` — sibling spec, closest read-only/ownership testing pattern
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.7 (Table 230, L4473-4492) — Functional requirement
- `CLAUDE.md` — Project rules (RBAC, audit, "AI provides guidance only; never diagnose, prescribe" mandate)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC-208 |

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
| **Feature / Gap ID** | `UC-208` |
| **Module** | `Consultation — View Consultation Summary (read side)` (Bounded Context: `Consultation`) |
| **Spec gốc** | `CB-CONSULTATION-IMP-208` |
| **Priority** | 🟠 P1 (SRS Priority: Medium; Frequency of Use: Frequent) |
| **Sprint** | Sprint 4 batch (UC203→UC210) — TV4-Lâm |
| **Milestone** | Sprint 4 |
| **Data Classification** | `PII` (health-adjacent expert-authored guidance content about a named Mother) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-CONSULTATION`, `BR-SAFETY` (read-boundary only) |
| **Upstream Dependencies** | `UC-95 Manage Consultation Session` (`session_status` state machine, `ADR-SESSION-001`), `UC-96 Write Consultation Summary` (`expert_summary` write + content-safety, `ADR-SUMMARY-001/002`) |
| **Downstream Consumers** | `CB-166 Consultation Summary` mobile screen, `CB-060 Consultation Detail` web summary block |
| **Platform** | Backend (Java 21/Spring Boot) + Mobile (Flutter/Dart) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC208_ViewConsultationSummary_TDS.md §17`, `ADR-SUMR-001/002/003/004` |
| **Constraints Injected** | Requester-Mother-only ownership (C1), completion+written availability gate reusing UC95's confirmed `'COMPLETED'` value (C2), zero-write guarantee (C3), verbatim content / no read-path synthesis (C4), whitelist DTO — no internal fields/PII (C5), package/layer discipline (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` is the final persistence oracle; ERD is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-208 does not name the visibility/completion terminal value | `UC95 TDS ADR-SESSION-001` confirms `'COMPLETED'` as definitive; `UC96 TDS ADR-SUMMARY-001` reuses it verbatim; this TDS's `ADR-SUMR-002` reuses it a third time | Tests assert against the literal string `'COMPLETED'` only — never derive/invent a second terminal value. `SUMR-TC-004` explicitly exercises a non-`'COMPLETED'` status (`'IN_SESSION'`) as the negative case |
| L2 | SRS description ("safe next steps") could be misread as UC-208 itself being responsible for content safety | `UC96 TDS ADR-SUMMARY-002` is the upstream generator/enforcer of content safety; `UC208 TDS ADR-SUMR-003` scopes UC-208's own obligation to "never synthesize/alter/strip" on the READ path only | `SUMR-TC-008` asserts the read path returns `expert_summary` byte-for-byte with no transformation — it does NOT re-test UC-96's content-safety keyword logic (that belongs to `UC96_WriteConsultationSummary_Test-Spec.md`, not duplicated here) |
| L3 | SRS AF2 ("empty state with next allowed action") could be misimplemented as `404` | `UC208 TDS ADR-SUMR-002` explicitly rejects `404` for "not yet available" in favor of `200 OK` + `summaryAvailable=false` + `summaryStatus` reason | `SUMR-TC-004`/`SUMR-TC-005` assert HTTP `200` (not `404`) with the correct `summaryStatus` enum value for each not-ready reason |
| L4 | SRS says "Secondary Actors: None" but does not explicitly state Expert/Admin are denied on THIS endpoint | `UC208 TDS ADR-SUMR-001/004`: UC-208 is Mother-only; Expert/Admin's technical read is a SEPARATE endpoint owned by UC-96, not this one | `SUMR-TC-003` explicitly asserts an Expert caller (even the session's own assigned Expert) receives `403` on the UC-208 endpoint — this is deliberate endpoint separation, not an oversight |
| L5 | No DB `CHECK`/index guarantees uniqueness of one session per booking | Schema: `consultation_sessions.booking_id` (no explicit UNIQUE constraint verified in `V1__init_schema.sql` L898-909) — service must handle `findByBookingId` returning the correct single/latest row deterministically | `SUMR-TC-006`/`SUMR-TC-INT-001` seed exactly one session per booking; a genuine multiplicity ambiguity is flagged Open in TDS §5.3 note 3 equivalent — not a blocking issue for baseline single-session-per-booking assumption |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
View Consultation Summary (read side) bao gồm các layer:
├── Domain (ConsultationSummaryViewPolicy — pure logic, no deps)
├── Application / Use Cases (ConsultationSummaryViewService — mock JPA Repositories với Mockito)
├── Controller (ConsultationSummaryViewController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full read + ownership isolation + availability flow)
└── Mobile (consultation_summary_screen.dart — Flutter widget tests, mocked API client)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-208` (§3.3.14.7, Table 230, L4473-4492) | Mother-only; "expert-written summary and safe next steps"; AF2 empty state; E1 access denial |
| `ADR-SUMR-001` | Requester-Mother-only ownership (single-record IDOR guard) |
| `ADR-SUMR-002` | Availability gate — `'COMPLETED'` + non-null summary → `200`; not-ready → `200` with flag, never `404` |
| `ADR-SUMR-003` | Read-path safety boundary — verbatim content, static disclaimer, no synthesis |
| `ADR-SUMR-004` | Distinct booking-keyed Mother endpoint, separate from UC-96's session-keyed Expert/Admin read |
| `BR-RBAC` / `BR-SAFETY` | Role/ownership-scoped; no diagnostic/prescriptive platform behavior on read |
| `CB-CONSULTATION-IMP-208 §9/§10/§13/§16` | API contract, error codes, test-condition summary, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — owner Mother views summary after session `COMPLETED` + written | `ConsultationSummaryViewService.viewSummary()` | `SUMR-TC-001` |
| TC-COND-002 | Ownership violation (IDOR) — different Mother (not requester) → 403 (`SUMR-004`) | `ConsultationSummaryViewPolicy.assertIsRequesterMother()` | `SUMR-TC-002` |
| TC-COND-003 | Role violation — Expert (even assigned) / other role on this Mother-only endpoint → 403 (`SUMR-004`) | `ConsultationSummaryViewPolicy.assertIsRequesterMother()` | `SUMR-TC-003` |
| TC-COND-004 | Not available — session not `COMPLETED` (`IN_SESSION`) → 200, `summaryAvailable=false`, `SESSION_NOT_COMPLETED` | `ConsultationSummaryViewPolicy.resolveAvailability()` | `SUMR-TC-004` |
| TC-COND-005 | Not available — `COMPLETED` but `expert_summary=null` → 200, `summaryAvailable=false`, `SUMMARY_NOT_WRITTEN` | `ConsultationSummaryViewPolicy.resolveAvailability()` | `SUMR-TC-005` |
| TC-COND-006 | Booking not found → 404 (`SUMR-003`) | `ConsultationSummaryViewService.viewSummary()` | `SUMR-TC-006` |
| TC-COND-007 | Response never includes internal-only fields (`technical_log_json`, `communication_room_id`) or other-user PII (email/phone) | `ConsultationSummaryViewResponse` / `ConsultationSummaryViewMapper` | `SUMR-TC-007` |
| TC-COND-008 | Read-path safety — `expert_summary` returned byte-for-byte verbatim; disclaimer is static, not content-derived | `ConsultationSummaryViewMapper` | `SUMR-TC-008` |
| TC-COND-009 | Context correctness — `expertName` from `users.full_name` only, `topic`/`durationMinutes` from booking, `sessionDate` from session | `ConsultationSummaryViewMapper` | `SUMR-TC-009` |
| TC-COND-010 | Zero-write guarantee — endpoint never mutates any table | `ConsultationSummaryViewService` | `SUMR-TC-010` |
| TC-COND-011 | Malformed `bookingId` path variable (not a UUID) → 400 (`SUMR-001`) | `ConsultationSummaryViewController` | `SUMR-TC-011` |
| — | Full flow via Testcontainers — ownership isolation + availability flag + enriched read model over real DB | `ConsultationSummaryViewController` + real DB | `SUMR-TC-INT-001` |
| — | Mobile: CB-166 renders summary/guidance/disclaimer, or empty-state when `summaryAvailable=false` | `consultation_summary_screen.dart` | `SUMR-TC-MOBILE-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller identity (owner Mother / other Mother / Expert / Admin / guest) | Confirms `ADR-SUMR-001`'s single-class "owner Mother" partition vs all-else-denied |
| State Transition Testing | `session_status` × `expert_summary` null/non-null combinations | Core availability decision table (`ADR-SUMR-002`) |
| Decision Table | Ownership × completion × summary-written → {200 available, 200 not-available×2 reasons, 403, 404} | Exhaustively covers `ADR-SUMR-001/002` branching |
| Boundary Value Analysis | `expert_summary` at exactly `null` vs a zero-length string vs populated text | Confirms `SUMMARY_NOT_WRITTEN` uses `IS NULL`, not blank-string heuristics |
| Error Guessing | IDOR via enumerated `bookingId`, response field leakage, IDOR via role-forgery | Security-critical since this is a PII read endpoint |
| Non-Functional (Advisory) Testing | Verbatim content-passthrough | Direct verification of `ADR-SUMR-003`'s core design decision |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-R01` | DB seed | `consultation_bookings{booking_id: B1, requester_user_id: MOTHER-001, expert_profile_id: EXPERT-001, topic: 'Third-trimester nutrition', duration_minutes: 45}` | Happy path booking |
| `FX-R02` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'COMPLETED', expert_summary: 'Discussed nutrition...', ended_at: <ts>}` | Availability satisfied |
| `FX-R03` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'IN_SESSION', expert_summary: null}` | Not available — session not completed |
| `FX-R04` | DB seed | `consultation_sessions{session_id: S1, booking_id: B1, session_status: 'COMPLETED', expert_summary: null}` | Not available — completed but not yet written |
| `FX-R05` | DB seed | `expert_profiles{expert_profile_id: EXPERT-001, user_id: USER-EXPERT-001, specialty: 'Obstetrics', professional_title: 'BS.'}` | Expert display context |
| `FX-R06` | DB seed | `users{user_id: USER-EXPERT-001, full_name: 'Le Thi Mai', email: 'expert@example.test'}` | Expert display name — email must NEVER be mapped |
| `FX-R07` | JWT | `{sub: 'MOTHER-001', role: 'MOTHER'}` | Auth context — owner Mother |
| `FX-R08` | JWT | `{sub: 'MOTHER-002', role: 'MOTHER'}` | Auth context — different Mother (non-owner, negative) |
| `FX-R09` | JWT | `{sub: 'USER-EXPERT-001', role: 'EXPERT'}` | Auth context — the session's own assigned Expert (still denied on this endpoint) |
| `FX-R10` | JWT | `{sub: 'ADMIN-001', role: 'SYSTEM_ADMIN'}` | Auth context — Admin (denied on this Mother-only endpoint) |
| `FX-R11` | Request | `bookingId = <random UUID not present in DB>` | Not-found negative case |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (IDOR, role-forgery, field-leak) |
| Mobile (Flutter) | — | — | — | ✅ (widget test) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Reuses UC95/UC96's shared-entity ID conventions where entities
// overlap (ConsultationSessionEntity/ConsultationBookingEntity are
// shared, read-only, with UC95/UC96 — no duplication).
// ═══════════════════════════════════════════════════════════

class ConsultationSummaryViewTestFactory {

    static final UUID BOOKING_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID SESSION_ID_1        = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1    = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID MOTHER_USER_ID_1    = UUID.fromString("00000000-0000-0000-0000-000000000501"); // owner
    static final UUID MOTHER_USER_ID_2    = UUID.fromString("00000000-0000-0000-0000-000000000502"); // non-owner
    static final UUID ADMIN_USER_ID_1     = UUID.fromString("00000000-0000-0000-0000-000000000601");

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(BOOKING_ID_1);
        booking.setRequesterUserId(MOTHER_USER_ID_1);
        booking.setExpertProfileId(EXPERT_PROFILE_ID_1);
        booking.setTopic("Third-trimester nutrition");
        booking.setDurationMinutes((short) 45);
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeAvailableSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(SESSION_ID_1);
        session.setBookingId(BOOKING_ID_1);
        session.setSessionStatus("COMPLETED"); // ADR-SUMR-002 — reused verbatim from UC95 ADR-SESSION-001
        session.setExpertSummary("Discussed third-trimester nutrition and mild heartburn. Recommended smaller, frequent meals.");
        session.setEndedAt(Instant.parse("2026-06-30T09:45:00Z"));
        overrides.accept(session);
        return session;
    }

    static ExpertProfileEntity makeExpertProfile() {
        ExpertProfileEntity profile = new ExpertProfileEntity();
        profile.setExpertProfileId(EXPERT_PROFILE_ID_1);
        profile.setUserId(EXPERT_USER_ID_1);
        profile.setSpecialty("Obstetrics - Nutrition");
        profile.setProfessionalTitle("BS.");
        return profile;
    }

    static UserEntity makeExpertUser() {
        UserEntity user = new UserEntity();
        user.setUserId(EXPERT_USER_ID_1);
        user.setFullName("Le Thi Mai");
        user.setEmail("expert@example.test"); // MUST NEVER be mapped into the response DTO
        return user;
    }
}
```

---

### SUMR-TC-001 — Happy path: owner Mother views summary after session `COMPLETED` + written

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSummaryViewService.viewSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryViewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC208 TDS §6.1` happy-path sequence diagram, `ADR-SUMR-001/002`

**Preconditions:** `FX-R01`, `FX-R02` (`COMPLETED` + summary text), `FX-R05`, `FX-R06`

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-R01`; mock `sessionRepository.findByBookingId(B1)` returns `FX-R02`; mock expert/user repos return `FX-R05`/`FX-R06`.
2. Act: `service.viewSummary(B1, MOTHER_USER_ID_1, "MOTHER")`.
3. Assert: response `summaryAvailable=true`, `summaryStatus="AVAILABLE"`, `expertSummary` equals `FX-R02.expertSummary` exactly, `expertName="Le Thi Mai"`, `topic="Third-trimester nutrition"`.

**Expected Result (PASS):** Full read model returned; no repository write method invoked.
**Expected Result (FAIL):** Missing/incorrect fields, or `summaryAvailable=false` despite satisfied preconditions.

**Current Status:** 🔴 Not written

---

### SUMR-TC-002 — Ownership violation (IDOR): different Mother (not requester) → 403 (`SUMR-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ConsultationSummaryViewPolicy.assertIsRequesterMother()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryViewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SUMR-001`

**Preconditions:** `FX-R01` (`requester_user_id = MOTHER_USER_ID_1`); caller = `MOTHER_USER_ID_2` (`FX-R08`)

**Test Steps:**
1. Arrange: `booking = ConsultationSummaryViewTestFactory.makeBooking(b -> {})`.
2. Act: `policy.assertIsRequesterMother(booking, MOTHER_USER_ID_2, "MOTHER")`.
3. Assert: throws `SummaryViewAuthorizationException` code `SUMR-004`.
4. Assert (Service-level, in accompanying test): ownership check occurs BEFORE `sessionRepository.findByBookingId()` is ever invoked — no summary content is loaded for a denied caller.

**Expected Result (PASS):** Exception thrown; zero session-repository interaction.
**Expected Result (FAIL):** Summary content is read/returned to a non-owner Mother, or the ownership check runs after content is already loaded (existence-oracle leak risk).

**Current Status:** 🔴 Not written

---

### SUMR-TC-003 — Role violation: Expert (even the session's own assigned Expert) / other role → 403 (`SUMR-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `ConsultationSummaryViewPolicy.assertIsRequesterMother()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryViewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC208 TDS §1.2` (SRS Table 230 — Primary Actor = Mother, Secondary Actors = None), `ADR-SUMR-001/004`

**Preconditions:** caller = `USER-EXPERT-001` (`FX-R09`, the booking's own assigned Expert) and separately `ADMIN-001` (`FX-R10`)

**Test Steps:**
1. Act: `policy.assertIsRequesterMother(booking, EXPERT_USER_ID_1, "EXPERT")`.
2. Assert: throws `SummaryViewAuthorizationException` code `SUMR-004` — even though this IS the assigned Expert for the booking (ownership on this endpoint is Mother-identity-based, not Expert-assignment-based).
3. Repeat with `("ADMIN-001", "SYSTEM_ADMIN")`; assert same `403`.

**Expected Result (PASS):** Both Expert and Admin denied on this specific Mother-only endpoint, confirming `ADR-SUMR-004`'s deliberate endpoint separation from UC-96's session-keyed Expert/Admin read.
**Expected Result (FAIL):** Expert or Admin receives `200` from this endpoint — would collapse the intended endpoint separation and widen the Mother-facing surface beyond SRS scope.

**Current Status:** 🔴 Not written

---

### SUMR-TC-004 — Not available: session not `COMPLETED` → 200, `summaryAvailable=false`, `SESSION_NOT_COMPLETED`

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryViewPolicy.resolveAvailability()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryViewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SUMR-002`, `UC208 TDS §6.2` not-available sequence diagram, SRS AF2

**Preconditions:** `FX-R03` (`session_status='IN_SESSION'`, `expert_summary=null`)

**Test Steps:**
1. Arrange: `session = ConsultationSummaryViewTestFactory.makeAvailableSession(s -> { s.setSessionStatus("IN_SESSION"); s.setExpertSummary(null); })`.
2. Act: `policy.resolveAvailability(session)`.
3. Assert: returns `SummaryAvailabilityStatus.SESSION_NOT_COMPLETED`.
4. Assert (Service/Controller-level, in accompanying test): HTTP response is `200 OK` (NOT `404`) with `summaryAvailable=false`, `expertSummary=null`.

**Expected Result (PASS):** `200` empty-state response per AF2 — never `404` for an owned, existing booking.
**Expected Result (FAIL):** `404` returned (violates `ADR-SUMR-002`, Logic Issue L3), or summary content leaked despite non-`COMPLETED` status.

**Current Status:** 🔴 Not written

---

### SUMR-TC-005 — Not available: `COMPLETED` but `expert_summary=null` → 200, `summaryAvailable=false`, `SUMMARY_NOT_WRITTEN`

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSummaryViewPolicy.resolveAvailability()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationSummaryViewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SUMR-002`

**Preconditions:** `FX-R04` (`session_status='COMPLETED'`, `expert_summary=null`)

**Test Steps:**
1. Arrange: `session = ConsultationSummaryViewTestFactory.makeAvailableSession(s -> s.setExpertSummary(null))` (status stays `COMPLETED`).
2. Act: `policy.resolveAvailability(session)`.
3. Assert: returns `SummaryAvailabilityStatus.SUMMARY_NOT_WRITTEN` (distinct from `SESSION_NOT_COMPLETED` in `SUMR-TC-004`).

**Expected Result (PASS):** Correct, distinguishable reason code returned — the two "not-ready" states are never conflated.
**Expected Result (FAIL):** Returns `AVAILABLE` for a null summary (content-leak risk of `null`/empty text to the client), or conflates this reason with `SESSION_NOT_COMPLETED`.

**Current Status:** 🔴 Not written

---

### SUMR-TC-006 — Booking not found → 404 (`SUMR-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSummaryViewService.viewSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryViewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC208 TDS §6.4` not-found sequence diagram, `§10` error table

**Preconditions:** `FX-R11` (random UUID not present in DB)

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `service.viewSummary(FX-R11.bookingId, MOTHER_USER_ID_1, "MOTHER")`.
3. Assert: throws `ConsultationBookingNotFoundException` code `SUMR-003`.

**Expected Result (PASS):** `404` surfaced; distinct from the `200`-with-flag "not available" cases (`SUMR-TC-004/005`) — a genuinely nonexistent booking is a true error, not an empty-state.
**Expected Result (FAIL):** Wrong status code, or a nonexistent booking silently returns `summaryAvailable=false` instead of `404`.

**Current Status:** 🔴 Not written

---

### SUMR-TC-007 — Response never includes internal-only fields or other-user PII (whitelist DTO)

**Severity:** `CRITICAL`
**CWE:** `CWE-200 — Exposure of Sensitive Information to an Unauthorized Actor`
**Feature Under Test:** `ConsultationSummaryViewResponse` / `ConsultationSummaryViewMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ConsultationSummaryViewMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SUMR-003`, `UC208 TDS §5.1/§8.1` (whitelist DTO), PDPA data-minimization

**Test Steps:**
1. Act: `mapper.toResponse(booking, session, expertProfile, expertUser, AVAILABLE)`.
2. Serialize response to JSON: `String json = objectMapper.writeValueAsString(response);`.
3. Assert: `json` does NOT contain `technicalLogJson`, `communicationRoomId`, `email`, `phone`, or the literal string `"expert@example.test"` (`FX-R06`'s email).
4. Assert: response class field set is exactly the whitelist declared in `UC208 TDS §8.1` (reflection-based field-name check against an explicit allow-list).

**Expected Result (PASS):** Only whitelisted fields present; no internal/PII leak.
**Expected Result (FAIL):** Any internal-only or other-user-PII field appears in the serialized response — release-blocking privacy defect.

**Current Status:** 🔴 Not written

---

### SUMR-TC-008 — Read-path safety: `expert_summary` returned byte-for-byte verbatim; disclaimer is static

**Severity:** `CRITICAL`
**Legal:** `CLAUDE.md — "AI provides guidance only; never diagnose, prescribe, or delay emergency routing"`
**Feature Under Test:** `ConsultationSummaryViewMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ConsultationSummaryViewMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-SUMR-003` — read path must not synthesize/alter/strip content

**Preconditions:** `FX-R02` (`expertSummary = "Discussed third-trimester nutrition and mild heartburn. Recommended smaller, frequent meals."`)

**Test Steps:**
1. Act: `response = mapper.toResponse(booking, FX-R02session, expertProfile, expertUser, AVAILABLE)`.
2. Assert: `response.getExpertSummary()` is `.equals()` (exact string equality, not `.contains()`/fuzzy) to `FX-R02.getExpertSummary()` — no trimming, no truncation, no appended/prepended text.
3. Assert: `response.getDisclaimer()` equals the fixed static disclaimer constant regardless of `expertSummary` content (test with two different summary texts, disclaimer string identical both times).
4. Assert (code-review-level, documented here): no code path in `ConsultationSummaryViewMapper`/`ConsultationSummaryViewService` calls an LLM/AI client, translation service, or text-summarization utility.

**Expected Result (PASS):** Exact verbatim pass-through; disclaimer content-independent.
**Expected Result (FAIL):** Any transformation of `expert_summary` (whitespace normalization beyond what's stored, truncation, translation, AI rewrite) — violates `ADR-SUMR-003` (`AP-CB-201`).

**Current Status:** 🔴 Not written

---

### SUMR-TC-009 — Context correctness: `expertName`/`topic`/`durationMinutes`/`sessionDate` sourced correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSummaryViewMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ConsultationSummaryViewMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC208 TDS §5.3` schema field mapping, `§8.1` DTO field list

**Test Steps:**
1. Act: `mapper.toResponse(FX-R01booking, FX-R02session, FX-R05profile, FX-R06user, AVAILABLE)`.
2. Assert: `expertName == "Le Thi Mai"` (from `users.full_name`, NOT `users.email`).
3. Assert: `expertSpecialty == "Obstetrics - Nutrition"` (from `expert_profiles.specialty`).
4. Assert: `topic == "Third-trimester nutrition"` (from `consultation_bookings.topic`).
5. Assert: `durationMinutes == 45` (from `consultation_bookings.duration_minutes`).
6. Assert: `sessionDate == Instant.parse("2026-06-30T09:45:00Z")` (from `consultation_sessions.ended_at`).

**Expected Result (PASS):** Each context field traces to its documented schema source, no cross-wiring.
**Expected Result (FAIL):** Any field sourced from the wrong column (e.g., `expertName` accidentally populated from `email`).

**Current Status:** 🔴 Not written

---

### SUMR-TC-010 — Zero-write guarantee: endpoint never mutates any table

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSummaryViewService.viewSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationSummaryViewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC208 TDS §6.5` invariant #1, `C3` in §17.1

**Test Steps:**
1. Act: `service.viewSummary(B1, MOTHER_USER_ID_1, "MOTHER")` (happy path).
2. Assert: via Mockito, verify NO `@Modifying`/`save()`/`update*()` method is ever invoked on `ConsultationSessionRepository`, `ConsultationBookingRepository`, or any other repository injected into this service.
3. Repeat for the not-available (`SUMR-TC-004`/`005`) and not-found (`SUMR-TC-006`) paths — zero writes on every branch, including error branches.

**Expected Result (PASS):** Zero write interactions across all branches.
**Expected Result (FAIL):** Any write call detected (e.g., a mistaken "mark as viewed" mutation) — violates `C3`/invariant #1 (`AP-CB-204`).

**Current Status:** 🔴 Not written

---

### SUMR-TC-011 — Malformed `bookingId` path variable → 400 (`SUMR-001`)

**Severity:** `LOW`
**Feature Under Test:** `ConsultationSummaryViewController`
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationSummaryViewControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC208 TDS §10` error table (`SUMR-001`)

**Test Steps:**
1. Act: `GET /api/v1/consultations/bookings/not-a-valid-uuid/summary` with a valid Mother JWT.
2. Assert: HTTP `400`, error code `SUMR-001`; controller never invokes the service (path-variable binding fails before the service layer is reached).

**Expected Result (PASS):** `400` returned for a malformed path variable, distinct from the `404` (`SUMR-003`, well-formed but nonexistent UUID) case.
**Expected Result (FAIL):** `500` (unhandled binding exception) or the malformed value reaching the service layer.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SUMR-TC-INT-001 — Full flow: ownership isolation + availability flag + enriched read model (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `GET /api/v1/consultations/bookings/{id}/summary` → real DB
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationSummaryViewIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** TDS-03 E2E row

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-R01` (booking, `requester_user_id=MOTHER_USER_ID_1`) + `FX-R02` (`COMPLETED` session with summary) + `FX-R05`/`FX-R06` (expert context) inserted via JPA; a second unrelated booking/session pair for a different Mother, to prove cross-tenant isolation

**Test Steps:**
1. Seed booking `B1`/session `S1` (owner `MOTHER_USER_ID_1`) and an unrelated booking `B2`/session `S2` (owner `MOTHER_USER_ID_2`, also `COMPLETED` with a summary).
2. `GET /api/v1/consultations/bookings/{B1}/summary` with `MOTHER_USER_ID_1`'s JWT (`FX-R07`). Assert `200`, `summaryAvailable=true`, `expertSummary` matches `S1`'s stored value exactly (not `S2`'s).
3. `GET /api/v1/consultations/bookings/{B1}/summary` with `MOTHER_USER_ID_2`'s JWT (`FX-R08`). Assert `403` (`SUMR-004`) — confirms cross-tenant isolation at the DB-integration level, not just in mocked unit tests.
4. Update `S1.session_status` to `'IN_SESSION'` directly via JPA (simulating pre-completion), re-call step 2's request. Assert `200`, `summaryAvailable=false`, `summaryStatus='SESSION_NOT_COMPLETED'`.
5. Assert across all calls: no row in `consultation_sessions`/`consultation_bookings` was modified (`updated_at` unchanged from pre-test snapshot).

**Expected Result (PASS):** Full ownership isolation and availability-flag correctness confirmed against a real database; zero side effects.
**Expected Result (FAIL):** Cross-tenant leak, incorrect availability flag, or any row mutation detected.

**DB Assertion:**
```java
ConsultationSessionEntity record = sessionRepository.findByBookingId(B1).orElseThrow();
assertThat(record.getUpdatedAt()).isEqualTo(preTestUpdatedAtSnapshot); // unchanged — zero-write guarantee
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (Flutter widget tests)

---

### SUMR-TC-MOBILE-001 — CB-166 renders summary/guidance/disclaimer, or empty-state when unavailable

**Severity:** `MEDIUM`
**Feature Under Test:** `consultation_summary_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/consultation/consultation_summary_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC208 TDS §5.1` Mobile Screen component, `§9.1` API contract, CB-166 mockup

**Preconditions:** Mocked API client stub for `GET /consultations/bookings/:id/summary`

**Test Steps:**
1. Render `ConsultationSummaryScreen(bookingId: B1)` with a mocked API client returning the `summaryAvailable=true` payload (§9.2 example).
2. Assert: topic, expert name/specialty, summary text, and the static disclaimer are all displayed.
3. Re-render with a mocked `summaryAvailable=false, summaryStatus='SESSION_NOT_COMPLETED'` payload.
4. Assert: screen shows an empty-state (not a raw error), consistent with SRS AF2, and does NOT render a null/blank summary block.

**Expected Result (PASS):** UI reflects both available and not-available states correctly.
**Expected Result (FAIL):** Empty-state renders as an error screen, or a `null` summary is displayed as blank text instead of a proper empty-state message.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SUMR-TC-001` | `ConsultationSummaryViewServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-002` | `ConsultationSummaryViewPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-003` | `ConsultationSummaryViewPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-004` | `ConsultationSummaryViewPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-005` | `ConsultationSummaryViewPolicyTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-006` | `ConsultationSummaryViewServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-007` | `ConsultationSummaryViewMapperTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-008` | `ConsultationSummaryViewMapperTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-009` | `ConsultationSummaryViewMapperTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-010` | `ConsultationSummaryViewServiceTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-011` | `ConsultationSummaryViewControllerTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-INT-001` | `ConsultationSummaryViewIntegrationTest.java` | `[ ]` | `[ ]` | |
| `SUMR-TC-MOBILE-001` | `consultation_summary_screen_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
@Service
public class ConsultationSummaryViewService implements IConsultationSummaryViewService {
    @Override
    public ConsultationSummaryViewResponse viewSummary(UUID bookingId, UUID currentUserId, String currentRole) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SUMR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUMR-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

> Note: `SUMR-TC-007/008/009` target `ConsultationSummaryViewMapper` directly
> (a pure function), so their Red Gate stub is a mapper that returns an empty
> `ConsultationSummaryViewResponse` rather than throwing — both are valid Red
> Gate forms per CASE 2.0 as long as the assertions fail against the stub's
> empty/default output (i.e., `expertSummary` field-equality assertions fail
> because the stub returns `null`/default, not the seeded value).

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC208_ViewConsultationSummary_TDS.md` reviewed and Approved
- [ ] `UC-95 Manage Consultation Session` and `UC-96 Write Consultation Summary` implemented (data prerequisite — otherwise every booking returns `summaryAvailable=false`, which is still a valid, testable response, but happy-path integration testing needs real `COMPLETED`+written data)
- [ ] ADR-SUMR-001..004 confirmed by Tech Lead
- [ ] DPO review for read exposure of health-adjacent guidance content — sign-off pending
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `flutter test` — Mobile widget tests xanh
- [ ] Test coverage ≥ 80% lines cho `ConsultationSummaryViewService`, `ConsultationSummaryViewPolicy`, `ConsultationSummaryViewMapper`
- [ ] `SUMR-TC-002` (IDOR), `SUMR-TC-003` (role separation), `SUMR-TC-007` (field whitelist), `SUMR-TC-008` (verbatim safety), `SUMR-TC-010` (zero-write) pass — these are release-blocking privacy/safety/architecture gates
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors; all injected types exist in codebase (reused, not duplicated, `ConsultationSessionEntity`/`ConsultationBookingEntity`)
- [ ] **Props Isolation** — dùng `ConsultationSummaryViewTestFactory`, không shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS section)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-95/UC-96 not yet implemented (no real `COMPLETED`+written session data exists to exercise the happy path meaningfully — negative/not-available paths can still be tested against mocks)
- ADR-SUMR-001..004 not yet confirmed by Tech Lead
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# Revert implementation files (dev only — no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong sprint tracking
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes a different completion terminal value than `'COMPLETED'`, or expects `404` for not-yet-available | ☑ (all traced to `ADR-SUMR-002`, reused from UC95/UC96) | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic (e.g., ownership/availability decisions in the controller) | ☑ (controller-level tests only check HTTP mapping/security, business logic asserted at Policy/Service level) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports service/type not in codebase, or duplicates `ConsultationSessionEntity`/`ConsultationBookingEntity` | ☑ (all types match `UC208 TDS §8`; reuses UC95/UC96's shared entities) | G-3 |
| **AP-CB-201** *(project-specific, cross-referenced from TDS §17.4)* | **Read path synthesizing/altering clinical content** | Any code path that AI-generates, summarizes, translates, or strips `expertSummary`/disclaimer on read | `SUMR-TC-008` explicitly asserts exact-string verbatim equality and content-independent disclaimer | **Release-blocking** |
| **AP-CB-202** *(project-specific)* | **PII/internal-field leak** | Response or mapper includes `technical_log_json`, `communication_room_id`, email, or phone | `SUMR-TC-007` explicitly asserts these are absent from the serialized JSON | **Release-blocking** |
| **AP-CB-203** *(project-specific)* | **IDOR / broadened scope** | Ownership check skipped/reordered after content load, or Expert/Admin allowed on this endpoint | `SUMR-TC-002`, `SUMR-TC-003` explicitly assert denial and check-before-load ordering | **Release-blocking** |
| **AP-CB-204** *(project-specific)* | **Read causing a write** | Any `@Modifying`/`save()` invoked from the read path | `SUMR-TC-010` explicitly asserts zero write interactions across all branches (happy/not-available/not-found) | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`SUMR-TC-002/003/007/008/010`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC-208 v1.0 — Draft. Total test cases: 13 (11 unit/component/security + 1 integration + 1 mobile widget). Critical-severity: 6 (`SUMR-TC-001, 002, 003, 007, 008, 010` — happy-path data-shape, IDOR, role-separation, field-whitelist, verbatim-safety, and zero-write gates). Requires Approved status change only by user/Tech Lead.*
