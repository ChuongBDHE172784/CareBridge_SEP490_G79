# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-91 View Expert Question Queue

**Document ID:** `CB-EXP-TDD-091`
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
- `04_Implement/UC91_ViewExpertQuestionQueue/UC91_ViewExpertQuestionQueue_TDS.md` (companion TDS)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.5 (lines 838-857)

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Backend: `./mvnw test`. Web: `npm run test:run` (Vitest).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-91 |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-91` |
| **Module** | `ViewExpertQuestionQueue` — `expert` bounded context |
| **Spec gốc** | `CB-EXP-IMP-091` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 4 — Real Providers And Admin Polish` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `community_questions`, `community_topics`, `community_answers`, `expert_profiles` |
| **Downstream Consumers** | UC-92 Post Expert Answer, UC-93 Suggest Private Consultation |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXP-IMP-091 §17` |
| **Constraints Injected** | C1-C5 (reuse community repos, verify-before-query, anonymity masking, SecurityUtils identity, controller/service separation) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS says "specialty-matched questions" without defining the algorithm | `expert_profiles.specialty` and `community_topics.name` are both free-text varchar, no FK/taxonomy link (confirmed in `V1__init_schema.sql` lines 786-800, 147-157) | Tests assert ILIKE-based partial match (ADR-EXP-091-02 Option B) AND assert the "All Topics" unfiltered fallback returns results when `specialtyOnly=false` |
| L2 | SRS generic "Access denied" (E1) doesn't specify HTTP code | Existing pattern (`CommunityAnswerController`) uses `@PreAuthorize` + 403 | Tests assert exactly `403` + `EXPQ-004` body, not a generic 401/500 |
| L3 | No explicit rule on already-expert-answered questions | `CommunityAnswerRepository.findQuestionIdsWithExpertAnswer` exists and is reused (ADR-EXP-091-01) | Tests assert questions with `is_expert_labeled=true` answer are excluded from queue results |
| L4 | Anonymous question author display | `community_questions.is_anonymous` boolean exists; no masking logic exists yet anywhere in codebase | Tests assert `authorDisplayName` returns a fixed masked label, never real `users.full_name`, when `isAnonymous=true` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
ViewExpertQuestionQueue bao gồm các layer:
├── Domain (ExpertQuestionQueuePolicy — pure logic, no deps)
├── Services (ExpertQuestionQueueServiceImpl — mock repositories với Mockito)
├── Controller (ExpertQuestionQueueController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL, full stack)
└── Web (QuestionQueuePage.tsx — Vitest + Testing Library, mock TanStack Query)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-91` (lines 838-857) | Normal flow, AF1-AF3, E1-E3 |
| `V1__init_schema.sql` | `community_questions`, `community_topics`, `community_answers`, `expert_profiles` exact columns/constraints |
| `CB-EXP-IMP-091 §3/§8/§10/§16` | ADRs, interfaces, error codes, auth matrix |
| BR-RBAC | Only VERIFIED experts access queue |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified expert requests queue, matching questions exist | `ExpertQuestionQueueServiceImpl.getQueue()` | `EXPQ-TC-001` |
| TC-COND-002 | No matching questions (empty state, AF2) | `ExpertQuestionQueueServiceImpl.getQueue()` | `EXPQ-TC-002` |
| TC-COND-003 | Unverified/non-expert caller | `ExpertQuestionQueuePolicy.assertVerifiedExpert()` | `EXPQ-TC-003`, `EXPQ-TC-004` |
| TC-COND-004 | Anonymous question masking | `ExpertQuestionQueueServiceImpl` mapper | `EXPQ-TC-005` |
| TC-COND-005 | Already-expert-answered question excluded | `ExpertQuestionQueueServiceImpl.getQueue()` | `EXPQ-TC-006` |
| TC-COND-006 | Topic/stage filter applied | `ExpertQuestionQueueServiceImpl.getQueue()` | `EXPQ-TC-007` |
| TC-COND-007 | Pagination boundaries (page/size) | `ExpertQuestionQueueController` | `EXPQ-TC-008` |
| TC-COND-008 | No JWT / expired JWT | `ExpertQuestionQueueController` | `EXPQ-TC-009` |
| TC-COND-009 | Web queue page renders list + empty state | `QuestionQueuePage.tsx` | `EXPQ-TC-WEB-001`, `EXPQ-TC-WEB-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | verification_status values (VERIFIED / PENDING / REJECTED / null) | Only VERIFIED is valid partition |
| Boundary Value Analysis | page size (0, 1, 20, 50, 51) | Max page size = 50 per NFR §4.1 |
| State Transition | N/A (read-only feature) | — |
| Error Guessing | SQL-injection-style topicId, malformed UUID | OWASP A03 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `ExpertProfile{userId, specialty:'Dinh dưỡng', verificationStatus:'VERIFIED'}` | Happy path |
| `FX-002` | DB seed | `ExpertProfile{verificationStatus:'PENDING'}` | E1 reject |
| `FX-003` | DB seed | `CommunityQuestion{status:APPROVED, topicId matches FX-001 specialty}` | Match |
| `FX-004` | DB seed | `CommunityQuestion{status:APPROVED, anonymous:true}` | Masking |
| `FX-005` | DB seed | `CommunityAnswer{questionId=FX-003.id, isExpertLabeled:true}` | Exclusion |
| `FX-006` | JWT | `{ sub: expertUserId, role: 'EXPERT' }` | Auth context |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ExpertCommunityTestFactory.java — SHARED across UC-91/UC-92/UC-93
// src/test/java/com/carebridge/backend/expert/support/ExpertCommunityTestFactory.java
class ExpertCommunityTestFactory {

    static ExpertProfile makeVerifiedExpert() {
        return ExpertProfile.builder()
                .expertProfileId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .specialty("Dinh dưỡng thai kỳ")
                .verificationStatus("VERIFIED")
                .build();
    }

    static ExpertProfile makeUnverifiedExpert() {
        ExpertProfile p = makeVerifiedExpert();
        p.setVerificationStatus("PENDING");
        return p;
    }

    static CommunityTopic makeTopic(String name) {
        return CommunityTopic.builder().id(UUID.randomUUID()).name(name).build();
    }

    static CommunityQuestion makeApprovedQuestion(UUID topicId, boolean anonymous) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(topicId)
                .authorId(UUID.randomUUID())
                .title("Test question title")
                .body("Test question body content")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .anonymous(anonymous)
                .status(QuestionStatus.APPROVED)
                .build();
    }

    static CommunityAnswer makeExpertAnswer(UUID questionId) {
        return CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(questionId)
                .authorId(UUID.randomUUID())
                .body("Expert answer body text here, minimum ten chars")
                .expertLabeled(true)
                .personalExperience(false)
                .status(AnswerStatus.APPROVED)
                .build();
    }
}
```

---

### EXPQ-TC-001 — Happy path: verified expert retrieves specialty-matched queue

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl.getQueue()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertQuestionQueueServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-91 Normal Flow Step 5` / `CB-EXP-IMP-091 §8`

**Preconditions:**
- FX-001 (verified expert), FX-003 (approved question, topic name ILIKE-matches specialty)

**Test Steps:**
1. Mock `ExpertProfileRepository.findByUserId` → FX-001
2. Mock `CommunityQuestionRepository.findAllByStatusOrderByCreatedAtDesc` → Page containing FX-003
3. Mock `CommunityAnswerRepository.findQuestionIdsWithExpertAnswer` → empty set
4. Call `service.getQueue(expertUserId, filter, pageable)`

**Expected Result (PASS):**
- Returned page contains 1 item with `questionId = FX-003.id`, `specialtyMatched = true`

**Expected Result (FAIL):** Empty page or exception thrown

**Current Status:** 🔴 Not written

---

### EXPQ-TC-002 — AF2: no matching data returns empty page (not error)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl.getQueue()`
**Test File:** same as above
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `SRS UC-91 AF2`

**Test Steps:**
1. Mock question repository → empty Page
2. Call `service.getQueue(...)`

**Expected Result (PASS):** `Page.isEmpty() == true`, `totalElements == 0`, no exception
**Expected Result (FAIL):** Exception thrown, or `404` semantics leak into a read-list endpoint

**Current Status:** 🔴 Not written

---

### EXPQ-TC-003 — E1: unverified expert rejected with EXPQ-004

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ExpertQuestionQueuePolicy.assertVerifiedExpert()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertQuestionQueuePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-EXP-IMP-091 §10 EXPQ-004`

**Test Steps:**
1. Build FX-002 (PENDING expert)
2. Call `policy.assertVerifiedExpert(FX-002)`

**Expected Result (PASS):** Throws `ExpertNotVerifiedException` with message containing `EXPQ-004`
**Expected Result (FAIL):** No exception, or wrong exception type

**Current Status:** 🔴 Not written

---

### EXPQ-TC-004 — E1: caller with no expert_profiles row rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl.getQueue()`
**Test File:** `ExpertQuestionQueueServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-EXP-IMP-091 §10 EXPQ-004`

**Test Steps:**
1. Mock `ExpertProfileRepository.findByUserId` → `Optional.empty()`
2. Call `service.getQueue(randomUserId, filter, pageable)`

**Expected Result (PASS):** Throws `ExpertNotVerifiedException` [EXPQ-004] before any question-repository call (verify via `Mockito.verifyNoInteractions(communityQuestionRepository)`)
**Expected Result (FAIL):** Question repository invoked despite missing profile (constraint C2 violation)

**Current Status:** 🔴 Not written

---

### EXPQ-TC-005 — Anonymous question author is masked

**Severity:** `HIGH`
**CWE:** `CWE-359 — Exposure of Private Info`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl` mapper logic
**Test File:** `ExpertQuestionQueueServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-EXP-IMP-091 §17 C3`

**Test Steps:**
1. FX-004 (anonymous=true) in repository result
2. Call `service.getQueue(...)`

**Expected Result (PASS):** `authorDisplayName` equals a fixed masked constant (e.g. `"Ẩn danh"`), never equal to any seeded `users.full_name` value
**Expected Result (FAIL):** Real author name leaks in response

**Current Status:** 🔴 Not written

---

### EXPQ-TC-006 — Already expert-answered question excluded from queue

**Severity:** `HIGH`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl.getQueue()`
**Test File:** `ExpertQuestionQueueServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-EXP-IMP-091 §3 ADR-EXP-091-01`

**Test Steps:**
1. Mock question repo → Page with FX-003
2. Mock `findQuestionIdsWithExpertAnswer` → `{FX-003.id}` (already answered)
3. Call `service.getQueue(...)`

**Expected Result (PASS):** Returned page excludes `FX-003.id`
**Expected Result (FAIL):** Already-answered question still appears in queue

**Current Status:** 🔴 Not written

---

### EXPQ-TC-007 — Topic/stage filter narrows results

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertQuestionQueueServiceImpl.getQueue()`
**Test File:** `ExpertQuestionQueueServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-EXP-IMP-091 §8 QueueFilterRequest`

**Test Steps:**
1. Set `filter.topicId = specific topic UUID`
2. Call `service.getQueue(...)`

**Expected Result (PASS):** `CommunityQuestionRepository.findAllByStatusAndTopicIdOrderByCreatedAtDesc` invoked with that topicId (Mockito `verify`)
**Expected Result (FAIL):** Unfiltered method called instead

**Current Status:** 🔴 Not written

---

### EXPQ-TC-008 — Pagination boundary: size > 50 clamped or rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertQuestionQueueController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertQuestionQueueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-EXP-IMP-091 §4.1 NFR (max 50)`

**Test Steps:**
1. `GET /api/v1/expert/question-queue?size=200`

**Expected Result (PASS):** Effective page size capped at 50 (either clamped or `400 EXPQ-001`) — implementation decides which; test asserts one deterministic behavior is documented and enforced
**Expected Result (FAIL):** Uncapped page size accepted (DoS risk)

**Current Status:** 🔴 Not written

---

### EXPQ-TC-009 — No JWT → 401 (not 403)

**Severity:** `HIGH`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** `ExpertQuestionQueueController`
**Test File:** `ExpertQuestionQueueControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-EXP-IMP-091 §10 EXPQ-003`

**Test Steps (Attack Simulation):**
1. `GET /api/v1/expert/question-queue` with no `Authorization` header

**Expected Result (PASS = safe):** `401 Unauthorized`
**Expected Result (FAIL = vulnerable):** `200 OK` or leaks data

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EXPQ-TC-SEC-001 — SQL-injection-style topicId parameter rejected safely

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89`
**Feature Under Test:** `ExpertQuestionQueueController` input validation
**Test File:** `ExpertQuestionQueueControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. `GET /api/v1/expert/question-queue?topicId=' OR '1'='1`

**Expected Result (PASS = safe):** `400 EXPQ-001` (malformed UUID) — Spring's `UUID` path/query binding rejects non-UUID strings natively; JPA parameterized queries prevent injection regardless
**Expected Result (FAIL = vulnerable):** `500` with raw SQL error, or successful bypass

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EXPQ-TC-INT-001 — Full flow: verified expert queries queue against real DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request → Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertQuestionQueueIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-005`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrated
- Seed: FX-001 (verified expert user), one topic, FX-003 (approved question), FX-005 (expert-answered question on a DIFFERENT question)

**Test Steps:**
1. Seed data via JPA repositories
2. `GET /api/v1/expert/question-queue` with expert JWT
3. Assert response

**Expected Result (PASS):**
- `200 OK`, response contains the unanswered question, excludes the expert-answered one

**DB Assertion:**
```java
List<CommunityQuestion> approved = communityQuestionRepository.findAllByStatusOrderByCreatedAtDesc(QuestionStatus.APPROVED, Pageable.unpaged()).getContent();
assertThat(approved).extracting(CommunityQuestion::getId).contains(fx003.getId());
```

**Current Status:** 🔴 Not written

---

## Web Test Cases (Vitest + Testing Library)

### EXPQ-TC-WEB-001 — QuestionQueuePage renders list from API

**Severity:** `HIGH`
**Feature Under Test:** `QuestionQueuePage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/pages/QuestionQueuePage.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock `useExpertQuestionQueue` (TanStack Query hook) to resolve with 2 items
2. Render `<QuestionQueuePage />` inside `QueryClientProvider`
3. Assert both question titles appear on screen

**Expected Result (PASS):** Both titles rendered
**Expected Result (FAIL):** Loading/error state stuck, or crash

**Current Status:** 🔴 Not written

---

### EXPQ-TC-WEB-002 — Empty state shows "browse all topics" action (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `QuestionQueuePage.tsx`
**Test File:** same as above
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock hook → `{ content: [], totalElements: 0 }`
2. Render page

**Expected Result (PASS):** Empty-state message + button/link to toggle `specialtyOnly=false` visible
**Expected Result (FAIL):** Blank screen with no next action

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPQ-TC-001` | `ExpertQuestionQueueServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-002` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-003` | `ExpertQuestionQueuePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-004` | `ExpertQuestionQueueServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-005` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-006` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-007` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-008` | `ExpertQuestionQueueControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-009` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-SEC-001` | same | `[ ]` | `[ ]` | |
| `EXPQ-TC-INT-001` | `ExpertQuestionQueueIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-WEB-001` | `QuestionQueuePage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPQ-TC-WEB-002` | same | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertQuestionQueueServiceImpl implements IExpertQuestionQueueService {

    @Override
    public Page<ExpertQueueQuestionResponse> getQueue(UUID expertUserId, QueueFilterRequest filter, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPQ-TC-001` | throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPQ-TC-002` | throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPQ-TC-003` | N/A (policy tested directly, no stub needed) | 🔴 FAIL until policy implemented | ☐ FAIL ☐ PASS | |
| `EXPQ-TC-004`-`009` | throws / 500 | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (fill at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-EXP-IMP-091` reviewed and **Approved** (currently Draft)
- [ ] Logic Issues (§2) confirmed
- [ ] No migration required (confirmed §5.2 of TDS)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [ ] Test coverage ≥ 80% lines for `ExpertQuestionQueueServiceImpl`
- [ ] No business logic in `ExpertQuestionQueueController`
- [ ] `npm run test:run` green for `QuestionQueuePage.test.tsx`
- [ ] No PII/secret in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — all tests FAIL with stub
- [ ] Contract Existence — `./mvnw compile` no errors
- [ ] Props Isolation — factory-only instantiation in tests
- [ ] Oracle Source — every assert cites BR/AC/ADR

### Suspension Criteria
- TDS not yet Approved (blocks all implementation per `.claude/rules/implement-flow.md`)
- `ExpertProfile` entity not yet created (blocks TC-001, TC-003, TC-004, INT-001)

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
# No migration to revert for UC-91.
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Specialty match assumed without ADR-EXP-091-02 citation | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test asserts business logic (specialty match) directly | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExpertProfile`/`ExpertProfileRepository` before Chặng 1 lands | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | None detected at Draft stage | — | — |

---

*Status: Draft — do not implement until both this file and the companion TDS are marked `Approved`.*
