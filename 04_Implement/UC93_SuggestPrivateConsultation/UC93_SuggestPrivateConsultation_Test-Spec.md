# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-93 Suggest Private Consultation

**Document ID:** `CB-EXP-TDD-093`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `N/A`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC93_SuggestPrivateConsultation/UC93_SuggestPrivateConsultation_TDS.md` (companion TDS)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.7 (lines 880-899)
- `04_Implement/UC78_SubmitDisputeOrRefundRequest/` (checked for VNPay pattern — none found; confirms no payment integration exists yet)

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-93 |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-93` |
| **Module** | `SuggestPrivateConsultation` — `expert` bounded context |
| **Spec gốc** | `CB-EXP-IMP-093` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 4` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `community_questions`, `expert_profiles`, `expert_consultation_prices`, NEW `consultation_suggestions` |
| **Downstream Consumers** | Future booking UC (out of scope), notification module |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXP-IMP-093 §17` |
| **Constraints Injected** | C1-C6 |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS lists VNPay as Secondary Actor, implying possible direct interaction | No VNPay client exists anywhere in the codebase (confirmed via search); `consultation_bookings` (the real payable entity) is untouched by this UC | Tests assert ZERO interaction with any payment-related repository/client — `Mockito.verifyNoInteractions()` on a mocked `PaymentGatewayClient`-shaped dependency, and assert no `payment_transactions`/`consultation_bookings` row is created |
| L2 | SRS "transparent fee" doesn't specify snapshot vs. live value | `consultation_bookings.price_snapshot_amount` establishes an existing snapshot pattern in this schema | Tests assert fee fields are copied at creation, and a subsequent price change does NOT alter an already-created suggestion |
| L3 | No explicit duplicate-prevention rule in SRS | New unique constraint `(question_id, expert_profile_id)` added in migration | Tests assert second suggestion attempt on same question by same expert fails with `CSUG-004` |
| L4 | No explicit ownership check for `expertPriceId` in SRS | Any expert could otherwise reference another expert's price row without a check | Tests assert cross-expert `expertPriceId` is rejected with `CSUG-005` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
SuggestPrivateConsultation bao gồm các layer:
├── Domain (ConsultationSuggestionPolicy — pure logic)
├── Services (ConsultationSuggestionServiceImpl — mock repos với Mockito)
├── Controller (ConsultationSuggestionController — @WebMvcTest)
├── Integration (Testcontainers PostgreSQL + Flyway migration V20260703100100)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-93` (lines 880-899) | Normal flow, E1-E3, secondary actor scoping |
| `V1__init_schema.sql` + `V20260703100100` (new) | `expert_consultation_prices`, `consultation_bookings` (untouched, verified), new `consultation_suggestions` |
| `CB-EXP-IMP-093 §3/§8/§10/§16/§17` | ADRs, interfaces, error codes, auth matrix, constraints |
| BR-RBAC / BR-CONSULTATION | Ownership + auditable-lifecycle requirements |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified expert suggests with valid reason, single active price | `ConsultationSuggestionServiceImpl.suggest()` | `CSUG-TC-001` |
| TC-COND-002 | No VNPay/payment interaction occurs | `ConsultationSuggestionServiceImpl.suggest()` | `CSUG-TC-002` |
| TC-COND-003 | Fee snapshot immutable after later price change | `ConsultationSuggestionServiceImpl` | `CSUG-TC-003` |
| TC-COND-004 | No ACTIVE price → CSUG-003 | `ConsultationSuggestionServiceImpl.suggest()` | `CSUG-TC-004` |
| TC-COND-005 | Duplicate suggestion → CSUG-004 | `ConsultationSuggestionServiceImpl.suggest()` | `CSUG-TC-005` |
| TC-COND-006 | `expertPriceId` ownership violation → CSUG-005 | `ConsultationSuggestionPolicy` | `CSUG-TC-006` |
| TC-COND-007 | Unverified expert → EXPQ-004 | `ConsultationSuggestionPolicy` | `CSUG-TC-007` |
| TC-COND-008 | Question not APPROVED → CSUG-002 | `ConsultationSuggestionServiceImpl` | `CSUG-TC-008` |
| TC-COND-009 | `reason` boundary validation (9/10/1000/1001 chars) | DTO validation | `CSUG-TC-009`..`012` |
| TC-COND-010 | Multiple active prices, `expertPriceId` omitted → lowest-duration default | `ConsultationSuggestionServiceImpl` | `CSUG-TC-013` |
| TC-COND-011 | Audit log + domain event emitted | `ConsultationSuggestionServiceImpl` | `CSUG-TC-014`, `CSUG-TC-015` |
| TC-COND-012 | Web suggestion form + fee display | `SuggestConsultationForm.tsx` | `CSUG-TC-WEB-001`, `CSUG-TC-WEB-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | price row status (ACTIVE / EXPIRED / no rows) | Only ACTIVE valid |
| Boundary Value Analysis | reason length (9, 10, 1000, 1001 chars) | Exact DTO bounds |
| State Transition | `SuggestionStatus` (must stay SUGGESTED from this endpoint) | Safety/scope invariant |
| Error Guessing | cross-expert `expertPriceId`, duplicate POST, missing price | Security/business-rule edge cases |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Reuse | `ExpertCommunityTestFactory.makeVerifiedExpert()` | Happy path |
| `FX-002` | Reuse | `ExpertCommunityTestFactory.makeUnverifiedExpert()` | E1 |
| `FX-003` | Reuse | `ExpertCommunityTestFactory.makeApprovedQuestion(...)` | Happy path |
| `FX-011` | DB seed | `ExpertConsultationPrice{expertProfileId=FX-001.id, status:'ACTIVE', priceAmount:300000, durationMinutes:30}` | Happy path fee snapshot |
| `FX-012` | DB seed | `ExpertConsultationPrice{expertProfileId=<OTHER expert>, status:'ACTIVE'}` | Ownership violation (CSUG-005) |
| `FX-013` | DB seed | `ExpertConsultationPrice{expertProfileId=FX-001.id, status:'ACTIVE', durationMinutes:60}` (second row) | Default-selection test (lowest duration = FX-011) |
| `FX-014` | Request | `SuggestConsultationRequest{reason:"a".repeat(9)}` | Boundary — too short |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
// Extends the SHARED ExpertCommunityTestFactory from UC-91/UC-92
class ExpertCommunityTestFactory {
    // ... makeVerifiedExpert(), makeUnverifiedExpert(), makeApprovedQuestion() — see UC-91 §4

    static ExpertConsultationPrice makeActivePrice(UUID expertProfileId, BigDecimal amount, short durationMinutes) {
        return ExpertConsultationPrice.builder()
                .expertPriceId(UUID.randomUUID())
                .expertProfileId(expertProfileId)
                .priceAmount(amount)
                .currency("VND")
                .durationMinutes(durationMinutes)
                .channelType("VIDEO_CALL")
                .status("ACTIVE")
                .build();
    }

    static SuggestConsultationRequest makeValidSuggestionRequest(UUID questionId) {
        SuggestConsultationRequest req = new SuggestConsultationRequest();
        req.setQuestionId(questionId);
        req.setReason("Cần trao đổi riêng để đánh giá kỹ hơn tình trạng của bạn.");
        return req;
    }
}
```

---

### CSUG-TC-001 — Happy path: suggestion created with snapshotted fee

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-93 Normal Flow` / `CB-EXP-IMP-093 §8`

**Preconditions:** FX-001 (verified expert), FX-003 (approved question), FX-011 (one active price)

**Test Steps:**
1. Mock repos to return FX-001, FX-003, `[FX-011]`
2. Call `service.suggest(expertUserId, makeValidSuggestionRequest(FX-003.id))`

**Expected Result (PASS):** Returns `ConsultationSuggestionResponse` with `priceAmount=300000`, `currency="VND"`, `status="SUGGESTED"`
**Current Status:** 🔴 Not written

---

### CSUG-TC-002 — No VNPay/payment interaction occurs (CRITICAL security/scope test)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-EXP-IMP-093 §17 C1` / `ADR-EXP-093-02`

**Test Steps:**
1. Inject a mock `PaymentGatewayClient`-shaped collaborator (if such a bean is ever wired into this service by mistake) and a mock `ConsultationBookingRepository`-shaped collaborator
2. Happy path call
3. `Mockito.verifyNoInteractions(paymentGatewayClientMock, consultationBookingRepositoryMock)`

**Expected Result (PASS):** Zero interactions with any payment/booking collaborator
**Expected Result (FAIL):** Any interaction detected — flags `AP-EXP-093-A`

**Current Status:** 🔴 Not written

---

### CSUG-TC-003 — Fee snapshot immutable after later price change

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()` + persistence
**Test File:** `src/test/java/com/carebridge/backend/expert/ConsultationSuggestionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-EXP-IMP-093 §17 C3`

**Test Steps:**
1. Seed FX-011 (`priceAmount=300000`), create suggestion
2. Update FX-011's `priceAmount` to `500000` directly in DB
3. Re-fetch the suggestion by id

**Expected Result (PASS):** Suggestion's `priceAmountSnapshot` still `300000` (unchanged)
**Expected Result (FAIL):** Snapshot reflects the new `500000` (live-join bug)

**Current Status:** 🔴 Not written

---

### CSUG-TC-004 — E2: no ACTIVE price row → CSUG-003

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-EXP-IMP-093 §10 CSUG-003`

**Test Steps:** Mock price repo → empty list; call `service.suggest(...)`
**Expected Result (PASS):** Throws `NoActivePriceException` [CSUG-003]
**Current Status:** 🔴 Not written

---

### CSUG-TC-005 — AF1: duplicate suggestion on same question → CSUG-004

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionIntegrationTest.java` (needs real unique-constraint enforcement, not mockable in unit test)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-EXP-IMP-093 §5.2 uq_consultation_suggestions_question_expert`

**Test Steps:**
1. Create suggestion for (question, expert) pair
2. Attempt to create a second suggestion for the SAME pair

**Expected Result (PASS):** Second call throws `DuplicateSuggestionException` [CSUG-004] (service catches `DataIntegrityViolationException` from the unique constraint, or pre-checks via `findByQuestionIdAndExpertProfileId`)
**Current Status:** 🔴 Not written

---

### CSUG-TC-006 — `expertPriceId` ownership violation → CSUG-005

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationSuggestionPolicy.assertOwnsPriceRow()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ConsultationSuggestionPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-EXP-IMP-093 §17 C4`

**Test Steps:** Call `policy.assertOwnsPriceRow(FX-001, FX-012)` (FX-012 belongs to a different expert)
**Expected Result (PASS):** Throws exception containing `CSUG-005`
**Current Status:** 🔴 Not written

---

### CSUG-TC-007 — E1: unverified expert rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-EXP-IMP-093 §17 C5` (ordering)

**Test Steps:**
1. Mock `ExpertProfileRepository.findByUserId` → FX-002 (PENDING)
2. Call `service.suggest(...)`

**Expected Result (PASS):** Throws `ExpertNotVerifiedException` [EXPQ-004]; `Mockito.verifyNoInteractions(communityQuestionRepository, expertConsultationPriceRepository, consultationSuggestionRepository)` confirms early rejection order (C5)
**Current Status:** 🔴 Not written

---

### CSUG-TC-008 — E2: question not found/not APPROVED → CSUG-002

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Test Steps:** Mock question repo → `Optional.empty()`; call `service.suggest(...)`
**Expected Result (PASS):** Throws exception containing `CSUG-002`
**Current Status:** 🔴 Not written

---

### CSUG-TC-009 — Boundary: reason exactly 10 chars (valid)

**Severity:** `MEDIUM`
**Feature Under Test:** `SuggestConsultationRequest` bean validation
**Test File:** `src/test/java/com/carebridge/backend/expert/dto/SuggestConsultationRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Test Steps:** Validate `reason = "a".repeat(10)`
**Expected Result (PASS):** No violations
**Current Status:** 🔴 Not written

---

### CSUG-TC-010 — Boundary: reason 9 chars (invalid)

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-009`
**Test Steps:** Validate `reason = "a".repeat(9)`
**Expected Result (PASS):** 1 violation
**Current Status:** 🔴 Not written

---

### CSUG-TC-011 — Boundary: reason exactly 1000 chars (valid)

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-009`
**Test Steps:** Validate `reason = "a".repeat(1000)`
**Expected Result (PASS):** No violations
**Current Status:** 🔴 Not written

---

### CSUG-TC-012 — Boundary: reason 1001 chars (invalid)

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-009`
**Test Steps:** Validate `reason = "a".repeat(1001)`
**Expected Result (PASS):** 1 violation
**Current Status:** 🔴 Not written

---

### CSUG-TC-013 — Multiple active prices, `expertPriceId` omitted → lowest-duration default selected

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-EXP-IMP-093 §3 ADR-EXP-093-03` (Open item — documents the deterministic default, flagged for Product confirmation)

**Test Steps:**
1. Mock price repo → `[FX-011 (30 min), FX-013 (60 min)]`, ordered by duration ascending (per repository method name `findAllByExpertProfileIdAndStatusOrderByDurationMinutesAsc`)
2. Call `service.suggest(...)` with `expertPriceId = null`

**Expected Result (PASS):** Response snapshot matches FX-011 (30-minute, lowest duration)
**Expected Result (FAIL):** FX-013 selected, or non-deterministic selection

**Current Status:** 🔴 Not written

---

### CSUG-TC-014 — Audit log emitted with correct action

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-EXP-IMP-093 §17 C6`

**Test Steps:** Happy path; verify `AuditService.log(eq(AuditAction.CONSULTATION_SUGGESTED), ...)` called exactly once
**Current Status:** 🔴 Not written

---

### CSUG-TC-015 — Domain event `PrivateConsultationSuggested` published

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationSuggestionServiceImpl.suggest()`
**Test File:** `ConsultationSuggestionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-EXP-IMP-093 §7.3`

**Test Steps:** Happy path; verify `eventPublisher.publishEvent(any(PrivateConsultationSuggested.class))` called once, payload `suggestionId` matches saved id
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### CSUG-TC-SEC-001 — Cross-expert `expertPriceId` injection attempt via API

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Feature Under Test:** `ConsultationSuggestionController` + `ConsultationSuggestionServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ConsultationSuggestionControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Expert A authenticated; request body includes `expertPriceId` = Expert B's price row UUID (FX-012)

**Expected Result (PASS = safe):** `403` or `409` with `CSUG-005`, no suggestion created
**Expected Result (FAIL = vulnerable):** `201` with Expert B's price silently used — cross-tenant data leak

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### CSUG-TC-INT-001 — Full flow: suggestion persisted, unique constraint enforced by DB

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: HTTP → Controller → Service → Repository → PostgreSQL (post-migration V20260703100100)`
**Test File:** `src/test/java/com/carebridge/backend/expert/ConsultationSuggestionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-005`

**Preconditions:** PostgreSQL Testcontainer, Flyway migrated (incl. `V20260703100100`), seeded verified expert + approved question + active price

**Test Steps:**
1. `POST .../consultation-suggestions` (first time) → 201
2. `POST .../consultation-suggestions` (second time, same question+expert) → 409 CSUG-004

**Expected Result (PASS):** As above; DB shows exactly 1 row for the `(question_id, expert_profile_id)` pair

**DB Assertion:**
```java
Optional<ConsultationSuggestion> saved = suggestionRepository.findByQuestionIdAndExpertProfileId(questionId, expertProfileId);
assertThat(saved).isPresent();
assertThat(saved.get().getStatus()).isEqualTo(SuggestionStatus.SUGGESTED);
assertThat(saved.get().getPriceAmountSnapshot()).isEqualByComparingTo(new BigDecimal("300000"));
```

**Current Status:** 🔴 Not written

---

## Web Test Cases (Vitest + Testing Library)

### CSUG-TC-WEB-001 — Suggestion form displays transparent fee before submit

**Severity:** `HIGH`
**Feature Under Test:** `SuggestConsultationForm.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/components/SuggestConsultationForm.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock `useExpertActivePrices` query hook → `[{ priceAmount: 300000, currency: 'VND', durationMinutes: 30 }]`
2. Render form

**Expected Result (PASS):** Fee amount/currency/duration visible in the DOM before the expert submits (transparency requirement, SRS)
**Current Status:** 🔴 Not written

---

### CSUG-TC-WEB-002 — Submit calls mutation with reason only (no fee mutation client-side)

**Severity:** `MEDIUM`
**Feature Under Test:** `SuggestConsultationForm.tsx`
**Test File:** same
**TDD Phase:** 🔴 RED

**Test Steps:** Fill reason ≥ 10 chars, submit
**Expected Result (PASS):** Mutation called with `{ questionId, reason, expertPriceId? }` only — no `priceAmount` field sent from client (server is the source of truth for the snapshot, C3)
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CSUG-TC-001` | `ConsultationSuggestionServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-002` | same | `[ ]` | `[ ]` | |
| `CSUG-TC-003` | `ConsultationSuggestionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-004` | `ConsultationSuggestionServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-005` | `ConsultationSuggestionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-006` | `ConsultationSuggestionPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-007` | `ConsultationSuggestionServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-008` | same | `[ ]` | `[ ]` | |
| `CSUG-TC-009`-`012` | `SuggestConsultationRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-013`-`015` | `ConsultationSuggestionServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-SEC-001` | `ConsultationSuggestionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-INT-001` | `ConsultationSuggestionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CSUG-TC-WEB-001`-`002` | Web test files (see above) | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationSuggestionServiceImpl implements IConsultationSuggestionService {

    @Override
    public ConsultationSuggestionResponse suggest(UUID expertUserId, SuggestConsultationRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CSUG-TC-001`, `002`, `004`, `007`, `008`, `013`-`015` | throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CSUG-TC-006` | N/A — policy tested directly | 🔴 FAIL (class not found until implemented) | ☐ FAIL ☐ PASS | |
| `CSUG-TC-003`, `005`, `INT-001` | requires real DB — will FAIL at compile/schema-not-found until migration + entity exist | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS**
- Log file: `[path]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-EXP-IMP-093` reviewed and **Approved**
- [ ] Logic Issues (§2) confirmed
- [ ] Migration `V20260703100100__create_consultation_suggestions.sql` approved and run on staging
- [ ] `ExpertProfile` entity (from UC-91) already merged

### Exit Criteria (DoD)
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify` integration green
- [ ] Coverage ≥ 80% for `ConsultationSuggestionServiceImpl`
- [ ] No business logic in `ConsultationSuggestionController`
- [ ] `npm run test:run` green for web test files
- [ ] `CSUG-TC-002` (no VNPay interaction) passing — CRITICAL gate, cannot ship without this
- [ ] No PII/secret in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1)
- [ ] Contract Existence (`./mvnw compile`)
- [ ] Props Isolation
- [ ] Oracle Source cited for every assert

### Suspension Criteria
- TDS not Approved
- Migration not yet applied to test DB
- `ExpertProfile`/`ExpertConsultationPrice` entities not yet created

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS consultation_suggestions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703100100';"
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/main/resources/db/migration/V20260703100100__create_consultation_suggestions.sql
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | VNPay call added without new ADR superseding ADR-EXP-093-02 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test asserts price-snapshot business logic directly | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports non-existent `VNPayClient`/`ConsultationBooking` write path | ☐ | G-3 |
| **AP-EXP-093-A (project-specific)** | **Premature payment processing** | Any test/implementation path calling a payment gateway or writing `payment_transactions`/`consultation_bookings` from this UC | ☐ | **G-1 — CRITICAL** |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | None detected at Draft stage | — | — |

---

*Status: Draft — do not implement until both this file and the companion TDS are marked `Approved`.*
