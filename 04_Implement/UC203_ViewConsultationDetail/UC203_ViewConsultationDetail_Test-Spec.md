# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-203 View Consultation Detail

**Document ID:** `CB-CON-TDD-004`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tên] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (consultation domain ~876-1000)
- `04_Implement/UC203_ViewConsultationDetail/UC203_ViewConsultationDetail_TDS.md` (CB-CON-IMP-004) — Technical Design Spec
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.14.2 (Table 225) — UC-203
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — ADR-SESSION-001 (session_status value set)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/UC78_SubmitDisputeOrRefundRequest_TDS.md` — DisputeStatus / RefundStatus enums
- BR-RBAC, BR-PRIVACY, BR-CONSULTATION (SRS UC-203 Business Rules); PDPA

> **Quy ước TDD:** viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo Test-Spec cho UC-203 (read-only, ownership/IDOR + PII-minimization focus) |

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
| **Feature / Gap ID** | `UC-203` |
| **Module** | `ViewConsultationDetail — consultation` |
| **Spec gốc** | `CB-CON-IMP-004` |
| **Priority** | 🟠 P1 (IDOR/PII-critical read path) |
| **Sprint** | `S[N] (2026-07-03 → …)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA` (BR-PRIVACY minimum-necessary), `BR-RBAC` |
| **Upstream Dependencies** | `consultation_bookings, consultation_sessions, payment_transactions, consultation_disputes, refund_records, expert_profiles, users` |
| **Downstream Consumers** | Web/Mobile `CB-060` detail screen |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CON-IMP-004 §17`, `ADR-CDT-001/002/003` |
| **Constraints Injected** | C1 IDOR participant guard; C2 counterpart PII minimization; C3 real split-schema (no unified enum); C4 identity from JWT; C6 404-before-403 ordering |
| **Model** | `claude-opus-4-8` |
| **Trust Level** | `T2 → T3 (pending Red Gate §5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` + approved migrations are the final persistence oracle; ERD/draft specs are supporting evidence only.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Sibling **Draft** `UC75_BookPrivateConsultation` invented a unified `consultations` table + single `consultation_status` enum (`PENDING_PAYMENT→CONFIRMED→IN_SESSION→COMPLETED`, alt `CANCELLED/NO_SHOW/DISPUTED`) | Applied schema `V1__init_schema.sql` (~876-1000) has **separate** tables: `consultation_bookings.status`, `consultation_sessions.session_status`, `payment_transactions.status`, `consultation_disputes.status`, `refund_records.status` — 5 independent app-level columns, no DB CHECK. Priority: applied schema > unapproved draft (CLAUDE.md "current code and migrations override historical design notes"). | Tests assert the response exposes **distinct** `bookingStatus`/`sessionStatus`/`paymentStatus`/`disputeStatus`/`refundStatus` fields. No test expects a unified `consultation_status`. (CDT-TC-007) |
| L2 | SRS UC-203 Normal Flow reads "system validates access" generically — no explicit ownership predicate | ADR-CDT-001: detail-by-ID needs an explicit participant check (IDOR/CWE-639). Owner = `requester_user_id` (mother) OR `expert_profiles.user_id` of `booking.expert_profile_id` (expert) OR admin | Tests assert 403 CDT-003 for authenticated non-participant (CDT-TC-003) and correct pass for both owner roles (CDT-TC-001/002) |
| L3 | SRS shows "expert or user" identity — no PII scope stated | BR-PRIVACY minimum-necessary (ADR-CDT-002): counterpart = displayName (+ title/specialty for expert) only; no email/phone/raw ids/health payload | Test asserts serialized response contains no `@`, no `email`/`phone` keys (CDT-TC-006) |
| L4 | SRS "shared-data scope" wording ambiguous | ADR-CDT-002: surface only `shared_summary_id` presence (`sharedDataScopePresent`) + reference id; contents gated by UC-208 | Test asserts `sharedDataScopePresent` reflects `shared_summary_id != null` and no summary contents present (CDT-TC-008) |
| L5 | Session/dispute/refund status enum values not restated in SRS | Owned upstream: `session_status` values by ADR-SESSION-001 (WAITING/IN_SESSION/COMPLETED/NO_SHOW/CANCELLED); DisputeStatus/RefundStatus by UC78 | Tests cite ADR-SESSION-001 and UC78 as oracle for those expected values (CDT-TC-005) |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` in the test basis for all persistence/status oracle facts.

### TDS-01 — Scope / Phạm vi

```
ViewConsultationDetail bao gồm các layer:
├── Policy (pure logic — ConsultationParticipantPolicy, no deps) — ownership/IDOR
├── Service (mock repositories với Mockito) — assembly + 404/403 ordering
├── Mapper (pure) — PII minimization + multi-status mapping
├── Controller (@WebMvcTest, mock Service) — path var + exception→HTTP mapping
└── Integration (Testcontainers PostgreSQL + @SpringBootTest) — end-to-end over real schema
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-203` (§3.3.14.2) | Normal Flow (view detail), E1 (access denied outside scope), AF2 (no matching data), fields: expert/user, time, modality, status, fee, shared-data scope |
| `ADR-CDT-001` | Participant ownership predicate; 404-before-403 ordering (IDOR guard) |
| `ADR-CDT-002` | Counterpart PII minimization; shared-data scope = reference only |
| `ADR-CDT-003` | Real split-schema multi-status model (no unified enum) |
| `ADR-SESSION-001` (UC95) | Valid `session_status` value set |
| `UC78 TDS §8` | DisputeStatus / RefundStatus enum values |
| `BR-RBAC / BR-PRIVACY / PDPA` | Access control + minimum-necessary compliance |
| `V1__init_schema.sql` ~876-1000 | Column names / status source-of-truth per table |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner mother can view own booking detail | `ConsultationDetailService.getDetail` | `CDT-TC-001` |
| TC-COND-002 | Assigned expert can view booking detail (via expert_profiles.user_id) | `ConsultationParticipantPolicy.assertCanView` | `CDT-TC-002` |
| TC-COND-003 | Authenticated non-participant denied (IDOR) | `ConsultationParticipantPolicy` → 403 | `CDT-TC-003` |
| TC-COND-004 | Unknown bookingId → 404 (before ownership) | `ConsultationDetailService` → 404 | `CDT-TC-004` |
| TC-COND-005 | Dispute + refund status surfaced when present | `ConsultationDetailMapper` | `CDT-TC-005` |
| TC-COND-006 | No counterpart PII leaked | `ConsultationDetailMapper` / response serialization | `CDT-TC-006` |
| TC-COND-007 | Multi-status model — distinct fields, no unified enum | `ConsultationDetailResponse` | `CDT-TC-007` |
| TC-COND-008 | Shared-data scope surfaced as reference only | `ConsultationDetailMapper` | `CDT-TC-008` |
| TC-COND-009 | Admin can view any booking | `ConsultationParticipantPolicy` | `CDT-TC-009` |
| TC-COND-010 | End-to-end over real schema (integration) | Full flow | `CDT-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller class: owner-mother / owner-expert / admin / non-participant | Distinct authorization outcomes |
| Boundary Value Analysis | Optional rows present vs absent (dispute/refund/session/shared_summary null) | Nullable-field edges |
| State-based (read) | Rendered status combinations from 5 independent columns | Verify multi-status assembly |
| Error Guessing | IDOR enumeration, PII leakage, entity serialization | OWASP A01 (Broken Access Control) / CWE-639 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | Booking B1 `{ requester_user_id=MOTHER-001, expert_profile_id=EP-001, status='CONFIRMED', channel_type='VIDEO', shared_summary_id=SS-1 }` | Happy path owner-mother |
| `FX-002` | DB seed | ExpertProfile EP-001 `{ expert_profile_id=EP-001, user_id=EXPERT-USER-001, professional_title='Pediatrician', specialty='Pediatrics' }` | Expert ownership resolution |
| `FX-003` | DB seed | Session `{ booking_id=B1, session_status='COMPLETED' }` | sessionStatus oracle (ADR-SESSION-001) |
| `FX-004` | DB seed | Payment `{ booking_id=B1, status='PAID', gross_amount=200000, refund_amount=0 }` | paymentStatus/fee |
| `FX-005` | DB seed | Dispute `{ booking_id=B1, status='UNDER_REVIEW' }` + Refund `{ payment_id=P1, status='PROCESSING' }` | Dispute/refund surfacing (UC78 enums) |
| `FX-006` | DB seed | Booking B2 `{ requester_user_id=MOTHER-001, shared_summary_id=NULL }`, no dispute/refund | Null-branch / empty scope |
| `FX-007` | JWT | `{ sub=MOTHER-001, role=ROLE_MOTHER }`, `{ sub=EXPERT-USER-001, role=ROLE_EXPERT }`, `{ sub=MOTHER-999, role=ROLE_MOTHER }`, `{ sub=ADMIN-001, role=ROLE_SYSTEM_ADMIN }` | Auth contexts |

---

## 4. Test Case Specification

> **TC ID format:** `CDT-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation. Mỗi @Test dùng factory (no shared mutable state).
// ConsultationDetailTestFactory.java
// ═══════════════════════════════════════════════════════════
class ConsultationDetailTestFactory {

    static final UUID MOTHER_001       = UUID.fromString("00000000-0000-0000-0000-0000000m0001".replace('m','0'));
    static final UUID MOTHER_999       = UUID.fromString("00000000-0000-0000-0000-000000009999");
    static final UUID EXPERT_USER_001  = UUID.fromString("00000000-0000-0000-0000-0000000e0001".replace('e','0'));
    static final UUID EP_001           = UUID.fromString("00000000-0000-0000-0000-0000000ep001".replace("ep","00"));
    static final UUID B1               = UUID.fromString("00000000-0000-0000-0000-0000000b0001".replace('b','0'));

    // Baseline valid booking owned by MOTHER_001 / EP_001 — đồng bộ FX-001
    static ConsultationBookingEntity makeBooking() {
        ConsultationBookingEntity b = new ConsultationBookingEntity();
        b.setBookingId(B1);
        b.setRequesterUserId(MOTHER_001);
        b.setExpertProfileId(EP_001);
        b.setChannelType("VIDEO");
        b.setDurationMinutes((short) 30);
        b.setStatus("CONFIRMED");
        b.setPriceSnapshotAmount(new BigDecimal("200000"));
        b.setCurrency("VND");
        b.setSharedSummaryId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        return b;
    }
    static ConsultationBookingEntity makeBooking(Consumer<ConsultationBookingEntity> overrides) {
        ConsultationBookingEntity b = makeBooking(); overrides.accept(b); return b;
    }

    static ExpertProfileEntity makeExpertProfile() {
        ExpertProfileEntity e = new ExpertProfileEntity();
        e.setExpertProfileId(EP_001);
        e.setUserId(EXPERT_USER_001);
        e.setProfessionalTitle("Pediatrician");
        e.setSpecialty("Pediatrics");
        return e;
    }

    static ConsultationSessionEntity makeSession(String status) {
        ConsultationSessionEntity s = new ConsultationSessionEntity();
        s.setBookingId(B1); s.setSessionStatus(status); return s;
    }
    static PaymentTransactionEntity makePayment(String status) {
        PaymentTransactionEntity p = new PaymentTransactionEntity();
        p.setBookingId(B1); p.setStatus(status);
        p.setGrossAmount(new BigDecimal("200000")); p.setRefundAmount(BigDecimal.ZERO);
        p.setCurrency("VND"); return p;
    }
}
```

> Note: the UUID literals above are illustrative synthetic placeholders; implementers should use fixed valid UUIDs (e.g. `...0001`, `...0002`) — the exact literal is not the oracle, the ownership *relationship* is.

---

### CDT-TC-001 — Owner mother sees own booking detail (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationDetailService.getDetail()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationDetailServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-CDT-001 §Decision` (mother = requester_user_id) + `SRS UC-203 Normal Flow`

**Preconditions:** FX-001, FX-002, FX-007 (MOTHER-001 JWT).

**Test Steps:**
1. Arrange: mock `bookingRepo.findById(B1)` → `makeBooking()`; session/payment present.
2. Act: `service.getDetail(B1, MOTHER_001, "ROLE_MOTHER")`.
3. Assert: response returned.

**Expected Result (PASS):** `response.bookingId == B1`; `counterpart.displayName` is the expert's display name; `feeAmount == 200000`; `bookingStatus == "CONFIRMED"`.

**Expected Result (FAIL):** Throws Forbidden/NotFound, or returns another booking's data.

**Current Status:** 🔴 Not written

---

### CDT-TC-002 — Assigned expert sees booking detail (ownership via expert_profiles.user_id)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationParticipantPolicy.assertCanView()` (expert path)
**Test File:** `.../ConsultationParticipantPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CDT-001 §Decision` — expert owner = `expert_profiles.user_id` of `booking.expert_profile_id`

**Preconditions:** FX-001, FX-002, FX-007 (EXPERT-USER-001 JWT).

**Test Steps:**
1. Mock `expertProfileRepo.findById(EP_001)` → `makeExpertProfile()` (user_id = EXPERT_USER_001).
2. Call `assertCanView(makeBooking(), EXPERT_USER_001, "ROLE_EXPERT")`.

**Expected Result (PASS):** No exception; expert is recognized as participant; counterpart displayed = the mother's display name only.

**Expected Result (FAIL):** Throws CDT-003, or matches raw `expert_profile_id` against `callerUserId` (wrong key).

**Current Status:** 🔴 Not written

---

### CDT-TC-003 — Non-participant denied (IDOR guard) 🔒

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC / PDPA — SRS UC-203 E1 (outside permitted data scope)`
**Feature Under Test:** `ConsultationParticipantPolicy.assertCanView()`
**Test File:** `.../ConsultationParticipantPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-CDT-001 §Decision` — non-participant → 403 CDT-003

**Preconditions:** FX-001 (B1 owned by MOTHER-001/EP-001), FX-007 (MOTHER-999 JWT).

**Test Steps (Attack Simulation):**
1. B1 belongs to MOTHER-001; caller is MOTHER-999 (authenticated, unrelated).
2. Call `assertCanView(makeBooking(), MOTHER_999, "ROLE_MOTHER")`.

**Expected Result (PASS = safe):** Throws `ForbiddenException` mapped to `403 CDT-003`; no booking field returned.

**Expected Result (FAIL = vulnerable):** No exception → non-owner can read another user's consultation (IDOR).

**Current Status:** 🔴 Not written

---

### CDT-TC-004 — Unknown bookingId → 404 (checked before ownership)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationDetailService.getDetail()`
**Test File:** `.../ConsultationDetailServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-CDT-001 §Decision` (C6: existence before ownership) + `SRS UC-203 AF2/E2`

**Preconditions:** `bookingRepo.findById(unknown)` → `Optional.empty()`.

**Test Steps:**
1. Mock repo to return empty.
2. Call `getDetail(randomUuid, MOTHER_001, "ROLE_MOTHER")`.

**Expected Result (PASS):** Throws `NotFoundException` → `404 CDT-004`. Policy `assertCanView` is **not** invoked (verify with Mockito `verify(policy, never())`).

**Expected Result (FAIL):** Returns 403 for unknown id, or NPE.

**Current Status:** 🔴 Not written

---

### CDT-TC-005 — Dispute + refund status surfaced when present

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationDetailMapper.toResponse()`
**Test File:** `.../ConsultationDetailMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC78 TDS §8` (DisputeStatus values incl. `UNDER_REVIEW`; RefundStatus incl. `PROCESSING`)

**Preconditions:** FX-005 (dispute `UNDER_REVIEW`, refund `PROCESSING`).

**Test Steps:**
1. Map with a dispute row (`status='UNDER_REVIEW'`) and refund row (`status='PROCESSING'`, refundAmount).
2. Inspect response.

**Expected Result (PASS):** `disputeStatus == "UNDER_REVIEW"`, `refundStatus == "PROCESSING"`, `refundAmount` reflects the refund row.

**Expected Result (FAIL):** Fields null/wrong, or dispute/refund status invented not from the source table.

**Current Status:** 🔴 Not written

---

### CDT-TC-006 — No counterpart PII leaked 🔒

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control (excessive data exposure)`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Legal:** `BR-PRIVACY / PDPA minimum-necessary`
**Feature Under Test:** `ConsultationDetailResponse` serialization
**Test File:** `.../ConsultationDetailMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-CDT-002 §Decision`

**Preconditions:** FX-001..FX-004.

**Test Steps:**
1. Build the response for owner-mother viewing B1.
2. `String json = objectMapper.writeValueAsString(response);`

**Expected Result (PASS = safe):**
```java
assertThat(json).doesNotContain("@");        // no email
assertThat(json.toLowerCase()).doesNotContain("email");
assertThat(json.toLowerCase()).doesNotContain("phone");
// counterpart is displayName (+ title/specialty) only; no raw user_id/account_id key
```

**Expected Result (FAIL = leak):** JSON contains an email, phone, or raw counterpart id.

**Current Status:** 🔴 Not written

---

### CDT-TC-007 — Multi-status model: distinct fields, no unified enum

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationDetailResponse` shape
**Test File:** `.../ConsultationDetailMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-CDT-003 §Decision` + `V1__init_schema.sql` ~876-1000 (separate status columns)

**Preconditions:** FX-001, FX-003 (`session_status='COMPLETED'`), FX-004 (`payment.status='PAID'`).

**Test Steps:**
1. Build response.
2. Assert distinct fields present.

**Expected Result (PASS):** `bookingStatus == "CONFIRMED"` (from booking), `sessionStatus == "COMPLETED"` (from session), `paymentStatus == "PAID"` (from payment) — three independent fields. There is **no** `consultationStatus` field.

**Expected Result (FAIL):** A single collapsed `consultationStatus`/`status` field (UC75 model), or session/payment status mixed into one.

**Current Status:** 🔴 Not written

---

### CDT-TC-008 — Shared-data scope surfaced as reference only

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationDetailMapper.toResponse()`
**Test File:** `.../ConsultationDetailMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-CDT-002 §Decision` (shared_summary_id presence only; contents gated by UC-208)

**Preconditions:** FX-001 (`shared_summary_id=SS-1`) and FX-006 (`shared_summary_id=NULL`).

**Test Steps:**
1. Map booking with `shared_summary_id` set → assert `sharedDataScopePresent == true` and `sharedSummaryId == SS-1`.
2. Map booking with `shared_summary_id == null` → assert `sharedDataScopePresent == false`, `sharedSummaryId == null`.
3. Assert response carries **no** health-summary contents field.

**Expected Result (PASS):** presence flag mirrors nullability; no summary body exposed.

**Expected Result (FAIL):** presence flag wrong, or summary contents inlined (UC-208 scope leak).

**Current Status:** 🔴 Not written

---

### CDT-TC-009 — Admin can view any booking

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationParticipantPolicy.assertCanView()` (admin branch)
**Test File:** `.../ConsultationParticipantPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-CDT-001 §Decision` (admin unrestricted) + `§16 Auth Matrix`

**Preconditions:** FX-007 (ADMIN-001 `ROLE_SYSTEM_ADMIN`).

**Test Steps:** Call `assertCanView(makeBooking(), ADMIN_001, "ROLE_SYSTEM_ADMIN")` where ADMIN-001 is neither requester nor expert.

**Expected Result (PASS):** No exception (admin allowed).

**Expected Result (FAIL):** Throws CDT-003 for admin.

**Current Status:** 🔴 Not written

---

### CONTROLLER TEST CASES

### CDT-TC-010 — Controller maps ForbiddenException → 403 CDT-003

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationDetailController` (@WebMvcTest, mock service)
**Test File:** `.../ConsultationDetailControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `§10 Error Codes` + `§9 API Spec`

**Test Steps:** Mock `service.getDetail(...)` to throw `ForbiddenException(CDT-003)`; `GET /api/v1/consultations/{B1}` with a valid JWT.

**Expected Result (PASS):** HTTP 403, body `error.code == "CDT-003"`. No booking fields present.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### CDT-TC-INT-001 — End-to-end detail over real schema

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: GET /api/v1/consultations/{bookingId}`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationDetailIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `V1__init_schema.sql` (Flyway auto-applied) + `ADR-CDT-001/002/003`

**Preconditions:**
- PostgreSQL Testcontainer; Flyway migration applied on context start.
- Seed via SQL/JPA: users MOTHER-001 & EXPERT-USER-001; expert_profiles EP-001; booking B1; session (`COMPLETED`); payment (`PAID`); no dispute/refund.

**Test Steps:**
1. Authenticate as MOTHER-001 → `GET /consultations/{B1}` → assert 200, body shape (distinct statuses, no PII).
2. Authenticate as MOTHER-999 → same id → assert 403 CDT-003.
3. `GET` random UUID → assert 404 CDT-004.

**Expected Result (PASS):**
- Step 1: 200; `bookingStatus/sessionStatus/paymentStatus` distinct; no `@`/`email`/`phone` in body.
- Step 2: 403 CDT-003.
- Step 3: 404 CDT-004.

**DB Assertion:**
```java
ConsultationBookingEntity row = bookingRepo.findById(B1).orElseThrow();
assertThat(row.getRequesterUserId()).isEqualTo(MOTHER_001);
// read-only path: no rows mutated
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CDT-TC-001` | `ConsultationDetailServiceTest` | `[ ]` | `___` | — |
| `CDT-TC-002` | `ConsultationParticipantPolicyTest` | `[ ]` | `___` | — |
| `CDT-TC-003` | `ConsultationParticipantPolicyTest` | `[ ]` | `___` | IDOR — must fail on stub |
| `CDT-TC-004` | `ConsultationDetailServiceTest` | `[ ]` | `___` | verify policy never() |
| `CDT-TC-005` | `ConsultationDetailMapperTest` | `[ ]` | `___` | — |
| `CDT-TC-006` | `ConsultationDetailMapperTest` | `[ ]` | `___` | PII guard |
| `CDT-TC-007` | `ConsultationDetailMapperTest` | `[ ]` | `___` | multi-status |
| `CDT-TC-008` | `ConsultationDetailMapperTest` | `[ ]` | `___` | — |
| `CDT-TC-009` | `ConsultationParticipantPolicyTest` | `[ ]` | `___` | — |
| `CDT-TC-010` | `ConsultationDetailControllerTest` | `[ ]` | `___` | — |
| `CDT-TC-INT-001` | `ConsultationDetailIntegrationTest` | `[ ]` | `___` | Testcontainers |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ suite với throw-stub. Mọi test PHẢI FAIL. Nếu PASS ngay → AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**

```java
@Service
public class ConsultationDetailService implements IConsultationDetailService {
    @Override
    public ConsultationDetailResponse getDetail(UUID bookingId, UUID callerUserId, String callerRole) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Policy stub
public class ConsultationParticipantPolicy {
    public void assertCanView(ConsultationBookingEntity booking, UUID callerUserId, String callerRole) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Mapper stub
public class ConsultationDetailMapper {
    public ConsultationDetailResponse toResponse(/* ... */) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CDT-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `CDT-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ★ IDOR — must not pass on stub |
| `CDT-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ★ PII guard |
| `CDT-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CDT-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___/red-gate-evidence.log`

> Nếu bất kỳ test PASS: dừng lại, xác định root cause, rewrite từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-CON-IMP-004` đã review & approve.
- [ ] Logic Issues (§2) confirmed với Principal Architect (esp. L1 schema authority).
- [ ] No migration required — confirm consultation entities/repositories reused from UC-202/UC-95 exist.
- [ ] Test fixtures (§3 TDS-05) chuẩn bị.

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh (no skip).
- [ ] `./mvnw verify` — integration test (Testcontainers) xanh.
- [ ] Test coverage ≥ 80% lines cho `ConsultationDetailService` + `ConsultationParticipantPolicy`.
- [ ] Không có business logic trong Controller.
- [ ] Không có counterpart PII (email/phone) trong response hoặc logs.
- [ ] CDT-TC-003 (IDOR) và CDT-TC-006 (PII) PASS.

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw-stub trước khi implement.
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output.
- [ ] **Props Isolation** — no shared mutable state (mọi entity qua factory).
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (ADR-CDT-00x / ADR-SESSION-001 / UC78 / SRS).

### Suspension Criteria
- Consultation entities/repositories chưa tồn tại (blocker on UC-202/UC-95 groundwork).
- Phát hiện lỗi kiến trúc mới cần Principal Architect review.
- CI pipeline broken bởi thay đổi khác.

---

## 7. Rollback Plan

```bash
# No migration for this read-only feature — revert code only (dev).
git checkout -- src/main/java/com/carebridge/backend/consultation/controller/ConsultationDetailController.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/ConsultationDetailService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/IConsultationDetailService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/policy/ConsultationParticipantPolicy.java
git checkout -- src/main/java/com/carebridge/backend/consultation/mapper/ConsultationDetailMapper.java
git checkout -- src/main/java/com/carebridge/backend/consultation/dto/response/ConsultationDetailResponse.java
git checkout -- src/test/java/com/carebridge/backend/consultation/

# UC-203 vẫn OPEN → giữ nguyên entry trong tracking.
# (No flyway_schema_history change — no migration was added.)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ (mọi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw-stub (§5.1) — esp. CDT-TC-003/006 | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes unified `consultation_status` (UC75) without ADR | ☐ (blocked by CDT-TC-007 + ADR-CDT-003) | G-1 |
| AP-AI-004 | Layer Violation | Test verifies business logic in Controller | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports service/type/field (e.g. counterpart.email) không tồn tại trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved.
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement.

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `CDT-TC-___` | | | ☐ |

---

*TDD Template v2.0 — CASE 2.0 Red Gate + Anti-Pattern Detection. Status: Draft.*
