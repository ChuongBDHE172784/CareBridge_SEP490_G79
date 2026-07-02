# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC79 — Review Expert After Consultation — Test Specification

**Document ID:** `CB-CONSULTATION-TDD-079`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (L957-967, L1859-1866, L1645) — primary schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.56 (L2935-2954)
- `04_Implement/UC79_ReviewExpertAfterConsultation/UC79_ReviewExpertAfterConsultation_TDS.md` (`CB-CONSULTATION-IMP-079`) — Technical Design Spec, this Test-Spec's basis
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_Test-Spec.md` — sibling spec, same package/entry-criteria blocker, style reference
- `CLAUDE.md` — architecture/RBAC/audit rules

> **Quy ước TDD:** Test cases viết TRƯỚC production code. Thứ tự: viết test →
> chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC79 |

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
| **Feature / Gap ID** | `UC-79` |
| **Module** | `Consultation — Expert Review` |
| **Spec gốc** | `CB-CONSULTATION-IMP-079` (UC79 TDS) |
| **Priority** | 🔴 High (per SRS) |
| **Sprint** | `Sprint 2 — Complete Core CRUD And UI Wiring` |
| **Milestone** | Owner: TV4-Lâm |
| **Data Classification** | `Internal / PII-adjacent` |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY`, `BR-CONSULTATION` |
| **Upstream Dependencies** | Booking service (UC-75/76), Session lifecycle service (UC-77) — ALL BLOCKING, see §6 Entry Criteria |
| **Downstream Consumers** | Expert directory rating aggregation (out of scope), Content moderation workspace (out of scope) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC79 TDS §17.2` Constraint Injection Block |
| **Constraints Injected** | C1 (session-completion gate), C2 (ownership + no self-review), C3 (one review per booking), C4 (always PENDING at creation), C5 (no ZegoCloud dependency), C6 (package layout / no entity leakage) |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-79 text ("after a confirmed consultation session") does not name the exact terminal status string, and UC-77 (which would set it) is unimplemented | `UC79 TDS ADR-REVIEW-001` assumes `consultation_sessions.session_status = 'COMPLETED'` as a **Proposed**, unconfirmed value | Tests assert the precondition check by literal string `'COMPLETED'` per the ADR, and the test doc explicitly notes (via `Oracle Source`) that this string is an assumption pending UC-77 confirmation — must be revisited if UC-77 lands with a different terminal value |
| L2 | SRS does not specify whether ZegoCloud (listed as secondary actor) is called directly by UC79 | `UC79 TDS §1.3` concludes UC79 never calls ZegoCloud — it only reads `consultation_sessions.session_status` from the DB | `REVIEW-TC-013` explicitly asserts zero interaction with any ZegoCloud client from the review submission path (AP-CB-002 / C5 guard) |
| L3 | No DB `CHECK` constraint on `expert_reviews.rating` (schema allows any `smallint`) | `UC79 TDS §5.3` — genuine gap, app-level validation required (`ExpertReviewPolicy.validateRating`) | `REVIEW-TC-005` boundary-tests rating at 0, 1, 5, 6 to enforce the 1-5 range purely at the service/DTO layer, since the DB will not reject an out-of-range value |
| L4 | No unique constraint exists on `expert_reviews.booking_id`, so the DB alone cannot prevent duplicate reviews | `UC79 TDS ADR-REVIEW-003` — service-level `assertNoExistingReview()` check proposed as the immediate safe default; a DB unique constraint migration (`V20260702110100`) is proposed but **not created** by this batch (Open, needs Tech Lead approval) | `REVIEW-TC-004` encodes the **service-level** 409 conflict check; a migration-level uniqueness test is explicitly out of scope until the proposed migration is approved (flagged, not silently assumed) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation Expert Review module bao gồm các layer:
├── Domain (ExpertReviewPolicy — pure logic, no deps)
├── Service (ExpertReviewService — mock JPA Repository với Mockito)
├── Controller (ExpertReviewController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-79 (§3.3.1.56) | Trigger, actor, "after a confirmed consultation session" precondition |
| `UC79 TDS` ADR-REVIEW-001..003 | Session-completion gate, ownership/self-review guard, one-review-per-booking |
| `V1__init_schema.sql` L957-967, L1645 | Column names/types/defaults/FKs as persistence oracle; confirms no unique index on `booking_id` |
| `BR-RBAC` / `BR-PRIVACY` / `BR-CONSULTATION` | Authorization, data-minimization, auditable lifecycle |
| `UC79 TDS §9-10` | API contract, error codes (`REV-001`..`REV-006`) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — Mother submits valid review after completed session | `ExpertReviewService.submitReview()` | `REVIEW-TC-001` |
| TC-COND-002 | Ownership violation — non-owner submits review → 403 (`REV-004`) | `ExpertReviewPolicy.assertCanReview()` | `REVIEW-TC-002` |
| TC-COND-003 | Session not completed → 400 (`REV-006`) | `ExpertReviewPolicy.assertSessionCompleted()` | `REVIEW-TC-003` |
| TC-COND-004 | Duplicate review attempt → 409 (`REV-002`) | `ExpertReviewPolicy.assertNoExistingReview()` | `REVIEW-TC-004` |
| TC-COND-005 | Rating out of 1-5 boundary → 400 (`REV-001`) | `ExpertReviewPolicy.validateRating()` / DTO validation | `REVIEW-TC-005` |
| TC-COND-006 | Booking not found → 404 (`REV-003`) | `ExpertReviewService.submitReview()` | `REVIEW-TC-006` |
| TC-COND-007 | New review always created with `moderationStatus=PENDING` (anti auto-approve) | `ExpertReviewService.submitReview()` | `REVIEW-TC-007` |
| — | Comment length boundary (0 / 1000 / 1001 chars) | DTO validation | `REVIEW-TC-008` |
| — | Unauthenticated request → 401 | `ExpertReviewController` security filter | `REVIEW-TC-009` |
| — | Expert attempts to review their own consultation → 403 (`REV-004`) | `ExpertReviewPolicy.assertCanReview()` | `REVIEW-TC-010` |
| — | `ExpertReviewSubmitted` event emitted with correct payload | `ExpertReviewService.submitReview()` | `REVIEW-TC-011` |
| — | `GET` review — only reviewer/moderator/admin may view | `ExpertReviewController` / `ExpertReviewPolicy` | `REVIEW-TC-012` |
| — | UC79 never invokes any ZegoCloud client (C5 guard) | `ExpertReviewService.submitReview()` | `REVIEW-TC-013` |
| — | Response never leaks raw entity fields (`reviewerUserId`) | `ExpertReviewMapper` | `REVIEW-TC-014` |
| TC-COND (E2E) | E2E — full submit review API flow via MockMvc/Testcontainers | `ExpertReviewController` + real DB | `REVIEW-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Boundary Value Analysis | `rating` (0, 1, 5, 6); `comment` length (0, 1000, 1001 chars) | Confirms `@Min(1) @Max(5)` and `@Size(max=1000)` boundaries per §8.1 |
| State Transition Testing | Review `PENDING→APPROVED/REJECTED` (§6.4 state machine) — UC79 must only ever produce the `[*]→PENDING` transition | Validates ADR-REVIEW-001/C4 invariant that submission never sets a terminal moderation state |
| Error Guessing | IDOR via `bookingId` manipulation, self-review by expert account, replay/duplicate submission | Ownership + idempotency + self-dealing attack surface |
| Decision Table | Ownership × session-status × existing-review combinations | 201 vs 400 vs 403 vs 409 branching |
| Equivalence Partitioning | `session_status` valid (`COMPLETED`) vs invalid (`WAITING`, `CANCELLED`, etc.) classes | Confirms precondition gate treats any non-`COMPLETED` value identically |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-R01` | DB seed | `ConsultationBookingEntity{bookingId=B1, requesterUserId=U1}` | Happy path owner |
| `FX-R02` | DB seed | `ConsultationSessionEntity{bookingId=B1, sessionStatus='COMPLETED'}` | Session-completed precondition satisfied |
| `FX-R03` | DB seed | `ConsultationSessionEntity{bookingId=B1, sessionStatus='WAITING'}` | Session-not-completed negative test |
| `FX-R04` | JWT | `{sub: 'U1', role: 'MOTHER'}` | Auth context — booking owner |
| `FX-R05` | JWT | `{sub: 'U2', role: 'MOTHER'}` | Auth context — non-owner |
| `FX-R06` | DB seed | Existing `ExpertReviewEntity{bookingId=B1, moderationStatus='PENDING'}` | Duplicate review conflict |
| `FX-R07` | DB seed | `ExpertProfileEntity{expertProfileId=EX1, userId=U3}` + JWT `{sub:'U3', role:'EXPERT'}` where `U3` is also `requesterUserId` of a manipulated booking | Self-review negative test |
| `FX-R08` | JWT | `{sub:'MOD1', role:'MODERATOR'}` | Auth context — moderator read access |

### TDS-06 — Applicability Matrix

| Layer | Unit | Integration | Component | E2E | Security |
|-------|------|-------------|-----------|-----|----------|
| Backend | ✅ `ExpertReviewPolicy`, `ExpertReviewService` | ✅ Repository + Testcontainers | ✅ `@WebMvcTest ExpertReviewController` | ✅ MockMvc full flow | ✅ IDOR / RBAC / self-review |
| Mobile | ✅ `expert_review_service.dart` unit | — | ✅ `review_expert_screen` widget test | — | — |
| Web | N/A (Mobile-only platform per SRS "Other Information: Platform: Mobile App") | — | — | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

class ExpertReviewTestFactory {

    static ConsultationBookingEntity makeBooking() {
        ConsultationBookingEntity booking = new ConsultationBookingEntity();
        booking.setBookingId(UUID.fromString("00000000-0000-0000-0000-0000000000B1"));
        booking.setRequesterUserId(UUID.fromString("00000000-0000-0000-0000-000000000U1"));
        booking.setStatus("COMPLETED");
        return booking;
    }

    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity booking = makeBooking();
        overrides.accept(booking);
        return booking;
    }

    static ConsultationSessionEntity makeCompletedSession() {
        ConsultationSessionEntity session = new ConsultationSessionEntity();
        session.setSessionId(UUID.fromString("00000000-0000-0000-0000-0000000000S1"));
        session.setBookingId(makeBooking().getBookingId());
        session.setSessionStatus("COMPLETED"); // ADR-REVIEW-001 assumed terminal value
        return session;
    }

    static ConsultationSessionEntity makeSession(Consumer<ConsultationSessionEntity> overrides) {
        ConsultationSessionEntity session = makeCompletedSession();
        overrides.accept(session);
        return session;
    }

    static ExpertProfileEntity makeExpertProfile() {
        ExpertProfileEntity expert = new ExpertProfileEntity();
        expert.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-0000000000EX"));
        expert.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000U3"));
        return expert;
    }

    static ExpertReviewEntity makeReview() {
        ExpertReviewEntity review = new ExpertReviewEntity();
        review.setReviewId(UUID.fromString("00000000-0000-0000-0000-0000000000R1"));
        review.setBookingId(makeBooking().getBookingId());
        review.setReviewerUserId(makeBooking().getRequesterUserId());
        review.setExpertProfileId(makeExpertProfile().getExpertProfileId());
        review.setRating((short) 5);
        review.setComment("Very helpful and patient, explained everything clearly.");
        review.setModerationStatus("PENDING");
        return review;
    }

    static ExpertReviewEntity makeReview(Consumer<ExpertReviewEntity> overrides) {
        ExpertReviewEntity review = makeReview();
        overrides.accept(review);
        return review;
    }

    static SubmitExpertReviewRequest makeSubmitRequest() {
        SubmitExpertReviewRequest request = new SubmitExpertReviewRequest();
        request.setBookingId(makeBooking().getBookingId());
        request.setRating((short) 5);
        request.setComment("Very helpful and patient, explained everything clearly.");
        return request;
    }

    static SubmitExpertReviewRequest makeSubmitRequest(Consumer<SubmitExpertReviewRequest> overrides) {
        SubmitExpertReviewRequest request = makeSubmitRequest();
        overrides.accept(request);
        return request;
    }
}
```

---

### REVIEW-TC-001 — Happy path: Mother submits valid review after completed session

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertReviewService.submitReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertReviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC79 TDS §6.1` sequence diagram + `V1__init_schema.sql` L957-967 (`moderation_status` default `'PENDING'`)

**Preconditions:**
- `FX-R01` booking seeded (`requesterUserId = U1`)
- `FX-R02` session seeded (`sessionStatus = 'COMPLETED'`)
- `FX-R04` JWT for `U1`

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(B1)` returns `FX-R01`; mock `sessionRepository.findByBookingId(B1)` returns `FX-R02`; mock `reviewRepository.findByBookingId(B1)` returns `Optional.empty()`.
2. Act: `expertReviewService.submitReview(ExpertReviewTestFactory.makeSubmitRequest(), U1)`.
3. Assert: returned `ExpertReviewResponse.moderationStatus == "PENDING"`; `reviewRepository.save()` called once with `reviewerUserId == U1`, `rating == 5`.

**Expected Result (PASS):** `201`-equivalent response, `moderationStatus="PENDING"`, `reviewRepository.save()` invoked exactly once.
**Expected Result (FAIL):** Any exception thrown, or review persisted with wrong `reviewerUserId`/`moderationStatus`.

**Current Status:** 🔴 Not written
**Implementation Note:** `ExpertReviewService` must call `ExpertReviewPolicy.assertCanReview()`, `assertSessionCompleted()`, `assertNoExistingReview()`, and `validateRating()` — in that order — before persistence.

---

### REVIEW-TC-002 — Ownership violation: non-owner submits review → 403 (`REV-004`)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `ExpertReviewPolicy.assertCanReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC79 TDS ADR-REVIEW-002`, `BR-RBAC`

**Preconditions:** `FX-R01` booking owned by `U1`; caller is `U2` (`FX-R05`).

**Test Steps:**
1. Arrange: `booking = ExpertReviewTestFactory.makeBooking()` (owner `U1`).
2. Act: `expertReviewPolicy.assertCanReview(booking, U2)`.
3. Assert: throws `ReviewAuthorizationException` with code `REV-004`.

**Expected Result (PASS):** Exception thrown, no review persisted.
**Expected Result (FAIL):** No exception / review created for non-owner.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-003 — Session not completed → 400 (`REV-006`)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertReviewPolicy.assertSessionCompleted()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC79 TDS ADR-REVIEW-001`, `§6.2` alternative-flow sequence diagram

**Preconditions:** `FX-R03` session with `sessionStatus = 'WAITING'` (not `'COMPLETED'`).

**Test Steps:**
1. Arrange: `session = ExpertReviewTestFactory.makeSession(s -> s.setSessionStatus("WAITING"))`.
2. Act: `expertReviewPolicy.assertSessionCompleted(session)`.
3. Assert: throws `ReviewPreconditionException` code `REV-006`; no `expert_reviews` row created (verified at Service level via `verifyNoInteractions(reviewRepository)` in the accompanying Service-layer test).

**Expected Result (PASS):** Exception thrown before persistence — this is the primary automated defense against AP-CB-002 (allowing review before precondition met).
**Expected Result (FAIL):** Review created despite session not being completed.

**Current Status:** 🔴 Not written
**Implementation Note:** Also assert this behavior at the Service layer (`ExpertReviewServiceTest`) with the same fixture to close the gap between policy-unit and service-integration coverage — see `REVIEW-TC-INT-001` for the end-to-end confirmation.

---

### REVIEW-TC-004 — Duplicate review attempt → 409 (`REV-002`)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertReviewPolicy.assertNoExistingReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC79 TDS §2 Logic Issues L4` / `ADR-REVIEW-003` (service-level guard since no DB unique constraint exists)

**Preconditions:** `FX-R06` — existing `PENDING` review for `B1`.

**Test Steps:**
1. Arrange: mock `reviewRepository.findByBookingId(B1)` returns `Optional.of(existingReview)`.
2. Act: `expertReviewPolicy.assertNoExistingReview(B1)`.
3. Assert: throws `ReviewConflictException` code `REV-002`.

**Expected Result (PASS):** Exception `REV-002`, no second row persisted.
**Expected Result (FAIL):** Second review silently created for the same booking.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-005 — Boundary: `rating` outside 1-5 range → 400 (`REV-001`)

**Severity:** `MEDIUM`
**Feature Under Test:** `SubmitExpertReviewRequest` DTO validation (`@Min(1) @Max(5)`) + `ExpertReviewPolicy.validateRating()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/SubmitExpertReviewRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC79 TDS §8.1` DTO annotation; `§5.3` genuine schema gap (no DB `CHECK` on `rating`)

**Test Steps:**
1. Act (rating=0): validate `ExpertReviewTestFactory.makeSubmitRequest(r -> r.setRating((short) 0))`.
2. Assert: violation on `rating` field (`@Min(1)`).
3. Act (rating=1): validate `makeSubmitRequest(r -> r.setRating((short) 1))`.
4. Assert: no violation.
5. Act (rating=5): validate `makeSubmitRequest(r -> r.setRating((short) 5))`.
6. Assert: no violation.
7. Act (rating=6): validate `makeSubmitRequest(r -> r.setRating((short) 6))`.
8. Assert: violation on `rating` field (`@Max(5)`).

**Expected Result (PASS):** Boundary respected exactly at 1/5; 0 and 6 rejected.
**Expected Result (FAIL):** Off-by-one on boundary, or out-of-range value silently persisted (critical since DB has no `CHECK` constraint — this test is the ONLY enforcement).

**Current Status:** 🔴 Not written

---

### REVIEW-TC-006 — Booking not found → 404 (`REV-003`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertReviewService.submitReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertReviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC79 TDS §10` error table

**Test Steps:**
1. Arrange: mock `bookingRepository.findById(anyUUID)` returns `Optional.empty()`.
2. Act: `expertReviewService.submitReview(ExpertReviewTestFactory.makeSubmitRequest(), U1)`.
3. Assert: throws `BookingNotFoundException` code `REV-003`.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-007 — Anti-pattern guard: new review always created with `moderationStatus=PENDING`

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertReviewService.submitReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertReviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC79 TDS §1.4 Moderation Pattern Reference`, `§6.4` state machine invariant #4, `C4` in §17.1

**Test Steps:**
1. Arrange: happy-path fixtures (`FX-R01`, `FX-R02`).
2. Act: `expertReviewService.submitReview(ExpertReviewTestFactory.makeSubmitRequest(), U1)`.
3. Assert: persisted entity captured via `ArgumentCaptor<ExpertReviewEntity>` has `moderationStatus == "PENDING"` — never `"APPROVED"` or `"REJECTED"`, regardless of `rating` value (even `rating=5` must not auto-approve).

**Expected Result (PASS):** `moderationStatus` is always `PENDING` immediately after submission.
**Expected Result (FAIL):** Any code path sets `APPROVED`/`REJECTED` directly from `submitReview()` — indicates an anti-pattern where a high rating causes auto-approval bypassing moderation.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary automated defense against a "high rating auto-approve" anti-pattern, analogous to UC78's `DISP-TC-008` financial-safety gate.

---

### REVIEW-TC-008 — Boundary: `comment` length validation (0 / 1000 / 1001 chars)

**Severity:** `MEDIUM`
**Feature Under Test:** `SubmitExpertReviewRequest` DTO validation (`@Size(max=1000)`, nullable)
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/SubmitExpertReviewRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC79 TDS §8.1`

**Test Steps:**
1. Act (null comment): validate `ExpertReviewTestFactory.makeSubmitRequest(r -> r.setComment(null))`.
2. Assert: no violation (schema: `expert_reviews.comment` is nullable).
3. Act (1000 chars): validate `makeSubmitRequest(r -> r.setComment("a".repeat(1000)))`.
4. Assert: no violation.
5. Act (1001 chars): validate `makeSubmitRequest(r -> r.setComment("a".repeat(1001)))`.
6. Assert: violation on `comment` field (`@Size(max=1000)`).

**Expected Result (PASS):** Boundary respected exactly at 1000/1001; null accepted.
**Expected Result (FAIL):** Off-by-one on max length, or null incorrectly rejected.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-009 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `ExpertReviewController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ExpertReviewControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** derived from `UC79 TDS §16` Authorization Matrix

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/consultations/bookings/{id}/reviews` with no `Authorization` header.
2. Assert: `401 Unauthorized`.

**Expected Result (PASS = hệ thống an toàn):** `401`, no review created.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without auth.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-010 — Self-review guard: expert cannot review their own consultation → 403 (`REV-004`)

**Severity:** `HIGH`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Feature Under Test:** `ExpertReviewPolicy.assertCanReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC79 TDS ADR-REVIEW-002` defense-in-depth check

**Preconditions:** `FX-R07` — a booking manipulated so `requesterUserId == expertProfile.userId == U3` (simulating an attempt to review a self-booked consultation).

**Test Steps:**
1. Arrange: `booking = makeBooking(b -> b.setRequesterUserId(U3))`; expert profile `userId = U3`.
2. Act: `expertReviewPolicy.assertCanReview(booking, U3)`.
3. Assert: throws `ReviewAuthorizationException` code `REV-004` — same code as ownership violation, since this is a defense-in-depth check on top of the primary ownership rule.

**Expected Result (PASS):** Exception thrown, no self-review persisted.
**Expected Result (FAIL):** Self-review succeeds, allowing rating manipulation.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-011 — `ExpertReviewSubmitted` event emitted with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `ExpertReviewService.submitReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertReviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC79 TDS §7.1/§7.3`

**Test Steps:**
1. Act: `expertReviewService.submitReview(ExpertReviewTestFactory.makeSubmitRequest(), U1)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())`; captured event is `ExpertReviewSubmitted` with `payload.bookingId == B1`, `payload.reviewerUserId == U1`, `payload.rating == 5`, `payload.moderationStatus == "PENDING"`.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-012 — `GET` review: only reviewer, moderator, or admin may view

**Severity:** `HIGH`
**Feature Under Test:** `ExpertReviewController` / `ExpertReviewPolicy` authorization on read path
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ExpertReviewControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC79 TDS §16` Authorization Matrix

**Test Steps:**
1. Arrange: `FX-R06` review owned by `U1`.
2. Act (owner): `GET /api/v1/consultations/reviews/{reviewId}` with JWT `U1`.
3. Assert: `200 OK`.
4. Act (non-owner, non-moderator): `GET .../reviews/{reviewId}` with JWT `U2`.
5. Assert: `403 Forbidden` code `REV-004`.
6. Act (moderator): `GET .../reviews/{reviewId}` with JWT `FX-R08` (`MODERATOR`).
7. Assert: `200 OK`.

**Expected Result (PASS):** Only owner/moderator/admin can read; all others rejected.
**Expected Result (FAIL):** Any non-owner, non-privileged role can read another Mother's review.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-013 — Guard: UC79 never invokes any ZegoCloud client (C5)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertReviewService.submitReview()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertReviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC79 TDS §1.3` ZegoCloud Secondary Actor — Verified Non-Dependency, `C5` in §17.1

**Test Steps:**
1. Arrange: happy-path fixtures; if a ZegoCloud client bean/interface exists anywhere in the Spring context under test, mock it.
2. Act: `expertReviewService.submitReview(ExpertReviewTestFactory.makeSubmitRequest(), U1)`.
3. Assert: no ZegoCloud client mock has any interaction (`verifyNoInteractions`) — if no such dependency is even injected into `ExpertReviewService`, this test passes trivially by construction (constructor signature has no ZegoCloud parameter).

**Expected Result (PASS):** Zero ZegoCloud interactions; `ExpertReviewService` constructor has no ZegoCloud dependency at all.
**Expected Result (FAIL):** Any ZegoCloud SDK/API call is made from the review submission path — violates AP-AI-005/AP-CB-002 hallucinated-dependency anti-pattern.

**Current Status:** 🔴 Not written

---

### REVIEW-TC-014 — Response never leaks raw entity/internal fields

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `ExpertReviewMapper`
**Test File:** `src/test/java/com/carebridge/backend/consultation/mapper/ExpertReviewMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `CLAUDE.md` — "Never expose JPA entities in API responses"

**Test Steps:**
1. Act: `ExpertReviewMapper.toResponse(ExpertReviewTestFactory.makeReview())`.
2. Assert: resulting `ExpertReviewResponse` JSON serialization matches exactly the fields declared in `UC79 TDS §9.2` (`reviewId`, `bookingId`, `expertProfileId`, `rating`, `comment`, `moderationStatus`, `createdAt`) — no raw JPA entity, no extra internal field.

**Expected Result (PASS):** Response matches exactly the `ExpertReviewResponse` fields in TDS §9.2.
**Expected Result (FAIL):** Extra internal fields (e.g., raw entity reference) serialized.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### REVIEW-TC-INT-001 — E2E: submit review API flow (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `POST /api/v1/consultations/bookings/{id}/reviews` → DB persisted row
**Test File:** `src/test/java/com/carebridge/backend/consultation/ExpertReviewSubmissionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** TDS-03 E2E row

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically
- Seed: `FX-R01` booking + `FX-R02` completed session inserted via JPA before test

**Test Steps:**
1. Seed booking `B1` owned by `U1`, and a linked session with `session_status='COMPLETED'`.
2. `POST /api/v1/consultations/bookings/{B1}/reviews` with JWT for `U1` and valid body (`rating=5`, `comment="..."`).
3. Assert response `201`, body matches §9.2 schema, `moderationStatus == "PENDING"`.
4. Assert DB: `SELECT * FROM expert_reviews WHERE booking_id = 'B1'` returns exactly 1 row.
5. Repeat step 2 with the same booking (simulating a duplicate submission) — assert `409` (`REV-002`), and DB still has exactly 1 row.

**Expected Result (PASS):**
- API `201` on first submission; DB row present with correct `reviewer_user_id`, `rating`, `moderation_status='PENDING'`.
- Second submission rejected with `409`; row count stays at 1.

**Expected Result (FAIL):**
- API error on first submission, DB row missing/duplicated, or second submission silently creates a second row.

**DB Assertion:**
```java
List<ExpertReviewEntity> rows = reviewRepository.findAllByBookingId(bookingId);
assertThat(rows).hasSize(1);
assertThat(rows.get(0).getModerationStatus()).isEqualTo("PENDING");
assertThat(rows.get(0).getReviewerUserId()).isEqualTo(U1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REVIEW-TC-001` | `ExpertReviewServiceTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-002` | `ExpertReviewPolicyTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-003` | `ExpertReviewPolicyTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-004` | `ExpertReviewPolicyTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-005` | `SubmitExpertReviewRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-006` | `ExpertReviewServiceTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-007` | `ExpertReviewServiceTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-008` | `SubmitExpertReviewRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-009` | `ExpertReviewControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-010` | `ExpertReviewPolicyTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-011` | `ExpertReviewServiceTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-012` | `ExpertReviewControllerTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-013` | `ExpertReviewServiceTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-014` | `ExpertReviewMapperTest.java` | `[ ]` | `[ ]` | |
| `REVIEW-TC-INT-001` | `ExpertReviewSubmissionIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertReviewService implements IExpertReviewService {
    @Override
    public ExpertReviewResponse submitReview(SubmitExpertReviewRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertReviewResponse getReview(UUID reviewId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REVIEW-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REVIEW-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] **BLOCKING:** Booking creation service (UC-75/76) implemented; `consultation_bookings` rows exist with real `requester_user_id`
- [ ] **BLOCKING:** Session lifecycle service (UC-77) implemented, and its terminal "completed" `session_status` value **confirmed** by Product/Tech Lead (ADR-REVIEW-001 currently assumes `'COMPLETED'`, `Proposed`)
- [ ] `UC79_ReviewExpertAfterConsultation_TDS.md` reviewed and Approved
- [ ] ADR-REVIEW-001 and ADR-REVIEW-003 confirmed by Product/Tech Lead (currently `Proposed`)
- [ ] DPO sign-off on review comment content handling (user-generated text about a named expert)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExpertReviewService`, `ExpertReviewPolicy`
- [ ] Không có business logic trong `ExpertReviewController`
- [ ] `REVIEW-TC-003` (session-completion gate), `REVIEW-TC-004` (duplicate guard), and `REVIEW-TC-007` (anti auto-approve) pass — these are release-blocking data-integrity gates
- [ ] `REVIEW-TC-013` (no ZegoCloud dependency) passes — confirms C5 constraint

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — mỗi test dùng `ExpertReviewTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã áp dụng ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- Booking/Session prerequisite services not yet deployed to test environment
- UC-77's actual `session_status` terminal value differs from the `'COMPLETED'` assumption in ADR-REVIEW-001 — requires Tech Lead review and possible test rewrite before proceeding
- New architecture unknown discovered requiring Tech Lead review

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/

# IF the proposed unique-constraint migration (ADR-REVIEW-003) was applied:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.expert_reviews DROP CONSTRAINT IF EXISTS uq_expert_reviews_booking_id;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_expert_reviews_booking_id;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260702110100';"

# Gap vẫn OPEN → giữ nguyên entry trong task tracker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (all TCs cite Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(to verify at Red Gate execution)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all decisions traced to ADR-REVIEW-00X) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check security/mapping, e.g. `REVIEW-TC-009`, `REVIEW-TC-012`) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc gọi ZegoCloud SDK không có trong scope | ☑ (all types match TDS §8 interfaces; `REVIEW-TC-013` explicitly guards against ZegoCloud hallucination) | G-3 |
| **AP-CB-002** *(project-specific, cross-referenced from UC78 sibling spec)* | **Allowing review before session-completion precondition met** | `ExpertReviewService.submitReview()` persists a row without calling `assertSessionCompleted()` first, or the check is bypassable | `REVIEW-TC-003` explicitly asserts the precondition gate; `REVIEW-TC-INT-001` confirms end-to-end | **Release-blocking** |
| **AP-CB-003** *(project-specific)* | **High rating auto-approving moderation status** | Any test path where a `rating=5` (or any value) submission results in `moderationStatus != 'PENDING'` directly from `submitReview()` | `REVIEW-TC-007` explicitly asserts `moderationStatus == "PENDING"` regardless of rating value | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`REVIEW-TC-003`, `REVIEW-TC-007`, `REVIEW-TC-013`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC79 v1.0 — Draft. Total test cases: 15 (14 unit/component/security + 1 integration). Critical-severity: 5 (`REVIEW-TC-001, 002, 003, 007, 009` — data-integrity, RBAC, and safety gates). Requires Approved status change only by user/Tech Lead.*
