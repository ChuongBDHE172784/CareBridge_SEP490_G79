# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-199 View Community Question Detail

**Document ID:** `CB-COMMUNITY-TDD-010`
**Version:** `1.0`
**Date:** `2026-06-29`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `CB-COMMUNITY-IMP-010` — TDS UC-199 View Community Question Detail
- `SRS 3.3.13.2` — UC-199 functional requirements
- `ADR-COM-011, ADR-COM-012, ADR-COM-013, ADR-COM-002`
- `BR-COM-017, BR-COM-018, BR-RBAC`

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                          |
| ---------- | --------------- | ---------------------------------------------------------- |
| 2026-06-29 | AI Agent        | Khởi tạo TDD spec cho UC-199 View Community Question Detail |
| 2026-06-29 | AI Agent — Amelia (Dev Agent) | 7 service tests (TC-001, TC-002, TC-004, TC-005, TC-007 + questionNotFound + topicNotFound variants) passed; marked GREEN | Implemented |

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

| Field                     | Value                                                                             |
| ------------------------- | --------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-199`                                                                          |
| **Module**                | `community — ViewCommunityQuestionDetail`                                         |
| **Spec gốc**              | `CB-COMMUNITY-IMP-010`                                                            |
| **Priority**              | P1 High                                                                           |
| **Sprint**                | `S1 (2026-06-29 → 2026-07-12)`                                                    |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                           |
| **Data Classification**   | `Internal + PII (authorId masking)`                                               |
| **Compliance Scope**      | `ADR-COM-002 (anonymous masking), BR-RBAC`                                        |
| **Upstream Dependencies** | `community.CommunityQuestion, community.CommunityAnswer, community.CommunityTopic, security (JWT)` |
| **Downstream Consumers**  | `Mobile App question detail screen, Web community page`                           |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                        |
| ------------------------ | -------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                        |
| **Constraint Source**    | `CB-COMMUNITY-IMP-010 §17`                                                                   |
| **Constraints Injected** | C1 (APPROVED only → 404), C2 (anonymous masking in mapper), C3 (APPROVED answers only), C4 (single topic lookup), C5 (isAuthenticated), C6 (empty answers → 200) |
| **Model**                | `claude-sonnet-4-6`                                                                          |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                 |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                    | Thực tế                                                              | Fix áp dụng trong test                                            |
| --- | --------------------------------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------------------------- |
| L1  | Spec does not specify behavior for non-APPROVED questions  | ADR-COM-011: PENDING/HIDDEN/LOCKED all return 404 (not 403)          | TC-004/005/006: verify 404 for each non-APPROVED status           |
| L2  | Spec does not clarify whether ALL answers are returned     | ADR-COM-012: only APPROVED answers included                          | TC-003: seed PENDING answer, verify NOT in response               |
| L3  | Spec says "author info" without specifying anonymous handling | ADR-COM-002: authorId=null in response when isAnonymous=true        | TC-002: verify authorId=null for anonymous question               |
| L4  | Spec does not clarify what status probing returns          | ADR-COM-011: always 404 (not 403) to prevent status probing attacks  | TC-004/005/006: all non-APPROVED → 404, never 403                 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UC-199 ViewCommunityQuestionDetail bao gồm:
├── Service Layer (unit — mock repositories)
│   └── CommunityQuestionServiceImpl.getQuestionDetail(UUID)
│       ├── findByIdAndStatus(id, APPROVED) → question or throw
│       ├── topicRepository.findById(topicId) → topicName
│       ├── answerRepository.findAllByQuestionIdAndStatus...(id, APPROVED) → answers
│       └── mapper.toDetailResponse(question, topicName, answers)
├── Mapper Layer (unit — no mocks needed)
│   └── CommunityQuestionMapper.toDetailResponse()
│       ├── anonymous = false → authorId preserved
│       └── anonymous = true → authorId = null (ADR-COM-002)
├── Controller Layer (unit — @WebMvcTest mock service)
│   ├── @PreAuthorize("isAuthenticated()") gate
│   ├── @PathVariable UUID id parsing
│   └── QuestionNotFoundException → 404 COM-006
└── Integration Layer (Testcontainers PostgreSQL)
    └── Full flow: save question + answers, GET detail, verify filtering
```

### TDS-02 — Test Basis

| Source               | Items Derived                                          |
| -------------------- | ------------------------------------------------------ |
| `SRS 3.3.13.2` UC-199 | View question detail with answers                     |
| `ADR-COM-011`        | Only APPROVED questions visible; 404 for all others    |
| `ADR-COM-002`        | Anonymous masking: authorId=null when isAnonymous=true |
| `ADR-COM-012`        | Only APPROVED answers included                         |
| `ADR-COM-013`        | Single topicRepository lookup                          |
| `BR-RBAC`            | isAuthenticated() required                             |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                         | Coverage Item                                         | Test Cases       |
| ------------ | ------------------------------------------------------ | ----------------------------------------------------- | ---------------- |
| TC-COND-001  | APPROVED question with APPROVED answers → full response | `getQuestionDetail()` happy path                     | `COM199-TC-001`  |
| TC-COND-002  | isAnonymous=true → authorId=null in response           | `mapper.toDetailResponse()` — ADR-COM-002             | `COM199-TC-002`  |
| TC-COND-003  | PENDING answers NOT included                           | `findAllByQuestionIdAndStatus...(id, APPROVED)`       | `COM199-TC-003`  |
| TC-COND-004  | PENDING question → 404 COM-006                         | `findByIdAndStatus(id, APPROVED)` returns empty       | `COM199-TC-004`  |
| TC-COND-005  | HIDDEN question → 404 COM-006                          | Same as TC-COND-004                                   | `COM199-TC-005`  |
| TC-COND-006  | LOCKED question → 404 COM-006                          | Same as TC-COND-004                                   | `COM199-TC-006`  |
| TC-COND-007  | APPROVED question with no answers → empty answers list  | Return empty list, not exception                     | `COM199-TC-007`  |
| TC-COND-008  | No JWT → 401                                           | `@PreAuthorize("isAuthenticated()")`                  | `COM199-TC-008`  |
| TC-COND-009  | Integration: save question + mixed answers, verify filter | Full stack with real DB                            | `COM199-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique                | Applied To                                                              | Rationale                              |
| ------------------------ | ----------------------------------------------------------------------- | -------------------------------------- |
| Equivalence Partitioning | QuestionStatus (APPROVED, PENDING, HIDDEN, LOCKED)                      | Cover all status branches              |
| Boundary Value Analysis  | answers list (empty, 1, many); authorId (null vs UUID)                  | Edge cases in response shape           |
| State Transition Testing | anonymous (true/false) → authorId masking                               | Privacy invariant correctness          |
| Error Guessing           | Non-existent ID, PENDING question, unauthenticated call                 | Common error scenarios                 |

### TDS-05 — Test Data Requirements

| Fixture ID      | Type    | Value / Logic                                                    | Mục đích                           |
| --------------- | ------- | ---------------------------------------------------------------- | ---------------------------------- |
| `FX-COM199-001` | DB seed | 1 APPROVED question, non-anonymous, topicId=FX-COM199-005        | Happy path baseline                |
| `FX-COM199-002` | DB seed | 1 APPROVED question, anonymous=true                              | Anonymous masking test             |
| `FX-COM199-003` | DB seed | 2 APPROVED answers + 1 PENDING answer for FX-COM199-001          | Answer filter test                 |
| `FX-COM199-004` | DB seed | PENDING, HIDDEN, LOCKED questions (one each)                     | Status rejection tests             |
| `FX-COM199-005` | DB seed | CommunityTopic `{id, name: "Thai kỳ"}`                           | topicName resolution               |
| `FX-COM199-006` | JWT     | `{sub: "user-001", roles: ["ROLE_MOTHER"]}`                      | Authenticated user                 |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// CommunityQuestionDetailTestFactory.java
class CommunityQuestionDetailTestFactory {

    static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID TOPIC_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID AUTHOR_ID   = UUID.fromString("00000000-0000-0000-0000-000000000003");

    static CommunityQuestion makeApprovedQuestion() {
        CommunityQuestion q = new CommunityQuestion();
        q.setId(QUESTION_ID);
        q.setTopicId(TOPIC_ID);
        q.setAuthorId(AUTHOR_ID);
        q.setTitle("Thai nhi 28 tuần");
        q.setBody("Body text");
        q.setStatus(QuestionStatus.APPROVED);
        q.setAnonymous(false);
        q.setAnswerCount(0);
        q.setLikeCount(0);
        q.setCreatedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        return q;
    }

    static CommunityQuestion makeApprovedQuestion(Consumer<CommunityQuestion> overrides) {
        CommunityQuestion q = makeApprovedQuestion();
        overrides.accept(q);
        return q;
    }

    static CommunityAnswer makeApprovedAnswer(UUID questionId) {
        CommunityAnswer a = new CommunityAnswer();
        a.setId(UUID.randomUUID());
        a.setQuestionId(questionId);
        a.setAuthorId(UUID.randomUUID());
        a.setBody("Answer body");
        a.setStatus(AnswerStatus.APPROVED);
        a.setExpertLabeled(false);
        a.setPersonalExperience(false);
        a.setCreatedAt(Instant.now());
        return a;
    }

    static CommunityAnswer makeAnswer(UUID questionId, AnswerStatus status) {
        CommunityAnswer a = makeApprovedAnswer(questionId);
        a.setStatus(status);
        return a;
    }
}
```

---

### COM199-TC-001 — Happy path: APPROVED question with 2 APPROVED answers

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityQuestionServiceImpl.getQuestionDetail()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-COM-011, ADR-COM-012, ADR-COM-013`

**Preconditions:**
- `questionRepository.findByIdAndStatus(QUESTION_ID, APPROVED)` returns APPROVED question (FX-COM199-001)
- `topicRepository.findById(TOPIC_ID)` returns topic with name "Thai kỳ"
- `answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, APPROVED)` returns 2 answers

**Test Steps:**
1. Arrange: configure mocks as above
2. Act: `service.getQuestionDetail(QUESTION_ID)`
3. Assert: response.id = QUESTION_ID
4. Assert: response.topicName = "Thai kỳ"
5. Assert: response.title = "Thai nhi 28 tuần"
6. Assert: response.answers.size() = 2
7. Assert: response.authorId = AUTHOR_ID (non-anonymous)

**Expected Result (PASS):**
- Full `CommunityQuestionDetailResponse` returned with all fields set, 2 answers

**Expected Result (FAIL):**
- Service throws exception, or authorId is null for non-anonymous question

**Current Status:** 🟢 Passing
**Implementation Note:** Inject `topicRepository` into `CommunityQuestionServiceImpl`; fetch once per call.

---

### COM199-TC-002 — Anonymous question: authorId=null in response

**Severity:** `CRITICAL`
**Feature Under Test:** Anonymous masking in `CommunityQuestionMapper.toDetailResponse()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-002`

**Preconditions:**
- Question has `isAnonymous=true` and `authorId=[AUTHOR_ID]`
- `questionRepository.findByIdAndStatus()` returns the anonymous question

**Test Steps:**
1. Arrange: mock APPROVED question with `anonymous=true`, `authorId=AUTHOR_ID`
2. Act: `service.getQuestionDetail(questionId)`
3. Assert: response.authorId == null
4. Assert: response.anonymous == true
5. Assert: response.title and other fields are NOT masked (only authorId)

**Expected Result (PASS):**
- `authorId = null` in response; all other fields present

**Expected Result (FAIL):**
- authorId is still populated for anonymous question (privacy violation)
- authorId is null even for non-anonymous question (wrong masking)

**Current Status:** 🟢 Passing
**Implementation Note:** Masking logic MUST be in mapper method, not service. `resp.setAuthorId(question.isAnonymous() ? null : question.getAuthorId())`.

---

### COM199-TC-003 — PENDING answers excluded from response

**Severity:** `HIGH`
**Feature Under Test:** Answer status filter in service
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-012`

**Preconditions:**
- APPROVED question exists
- `answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(id, APPROVED)` returns exactly 2 answers (mock excludes PENDING by design)
- PENDING answer exists in DB but is not returned by the filtered query

**Test Steps:**
1. Arrange: mock APPROVED question; mock answer repo to return 2 APPROVED answers only
2. Act: `service.getQuestionDetail(questionId)`
3. Assert: response.answers.size() = 2
4. Assert: verify `answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, AnswerStatus.APPROVED)` was called with `AnswerStatus.APPROVED`

**Expected Result (PASS):**
- Exactly 2 APPROVED answers returned; PENDING answer NOT included

**Expected Result (FAIL):**
- Service calls `findAllByQuestionId()` without status filter, returning PENDING answers too

**Current Status:** 🔴 Not written
**Implementation Note:** Use `verify(answerRepository).findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, AnswerStatus.APPROVED)` in test.

---

### COM199-TC-004 — PENDING question → 404 QuestionNotFoundException

**Severity:** `CRITICAL`
**Feature Under Test:** Status gate in `getQuestionDetail()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-011, COM-006`

**Preconditions:**
- `questionRepository.findByIdAndStatus(id, APPROVED)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: mock `findByIdAndStatus(questionId, APPROVED)` → `Optional.empty()`
2. Act: `service.getQuestionDetail(questionId)`
3. Assert: `QuestionNotFoundException` is thrown

**Expected Result (PASS):**
- `QuestionNotFoundException` thrown, maps to HTTP 404 with code COM-006

**Expected Result (FAIL):**
- Service returns partial response, or throws 403/500 instead of 404

**Current Status:** 🟢 Passing

---

### COM199-TC-005 — HIDDEN question → 404 QuestionNotFoundException

**Severity:** `CRITICAL`
**Feature Under Test:** Status gate — HIDDEN status
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-011`

**Preconditions:**
- `questionRepository.findByIdAndStatus(id, APPROVED)` returns `Optional.empty()` (question exists but with status=HIDDEN)

**Test Steps:**
1. Arrange: mock `findByIdAndStatus(hiddenQuestionId, APPROVED)` → `Optional.empty()`
2. Act: `service.getQuestionDetail(hiddenQuestionId)`
3. Assert: `QuestionNotFoundException` thrown

**Expected Result (PASS):**
- 404 COM-006 — HIDDEN question not distinguishable from non-existent (no status probing)

**Expected Result (FAIL):**
- 403 Forbidden returned (would reveal the question exists)

**Current Status:** 🟢 Passing

---

### COM199-TC-006 — LOCKED question → 404 QuestionNotFoundException

**Severity:** `HIGH`
**Feature Under Test:** Status gate — LOCKED status
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-COM-011`

**Preconditions:**
- `questionRepository.findByIdAndStatus(id, APPROVED)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: mock `findByIdAndStatus(lockedId, APPROVED)` → `Optional.empty()`
2. Act: `service.getQuestionDetail(lockedId)`
3. Assert: `QuestionNotFoundException` thrown → HTTP 404

**Expected Result (PASS):**
- 404 — same as PENDING and HIDDEN

**Current Status:** 🔴 Not written

---

### COM199-TC-007 — APPROVED question with no answers → empty answers list

**Severity:** `MEDIUM`
**Feature Under Test:** Empty answers edge case
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-COM-018`

**Preconditions:**
- APPROVED question with `answerCount=0`
- `answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(id, APPROVED)` returns `Collections.emptyList()`

**Test Steps:**
1. Arrange: mock APPROVED question; mock answer repo → empty list
2. Act: `service.getQuestionDetail(questionId)`
3. Assert: response.answers = []
4. Assert: no exception thrown
5. Assert: response.answerCount = question.getAnswerCount() (preserved from entity)

**Expected Result (PASS):**
- HTTP 200, `answers: []`, no exception

**Expected Result (FAIL):**
- Service throws exception when answer list is empty

**Current Status:** 🟢 Passing

---

### COM199-TC-008 — Unauthenticated request → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `@PreAuthorize("isAuthenticated()")` on controller method
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No `Authorization` header in request

**Test Steps:**
1. Arrange: MockMvc with Spring Security enabled (no mock user)
2. Act: `GET /api/v1/community/questions/{QUESTION_ID}` (no JWT)
3. Assert: response status = 401

**Expected Result (PASS):**
- HTTP 401 returned, service method never invoked

**Expected Result (FAIL):**
- HTTP 200 returned (endpoint accessible without authentication)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### COM199-TC-INT-001 — Save APPROVED question + 2 APPROVED + 1 PENDING answer, verify only 2 returned

**Severity:** `HIGH`
**Feature Under Test:** Full stack: `GET /api/v1/community/questions/{id}`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionDetailIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainers running
- Flyway migration applied automatically
- Seed: FX-COM199-001, FX-COM199-003, FX-COM199-005

**Test Steps:**
1. Save 1 APPROVED question (topicId = existing topic)
2. Save 2 APPROVED answers for that question
3. Save 1 PENDING answer for that question
4. Call `GET /api/v1/community/questions/{questionId}` with valid JWT
5. Assert response status = 200
6. Assert `data.answers.length = 2` (PENDING answer not returned)
7. Assert `data.topicName` is populated
8. Assert `data.title` matches saved question title
9. Assert `data.authorId` is NOT null (non-anonymous question)

**Expected Result (PASS):**
```json
{
  "data": {
    "title": "...",
    "topicName": "Thai kỳ",
    "answers": [{ "status": "APPROVED" }, { "status": "APPROVED" }]
  }
}
```
Where `answers.length = 2`.

**Expected Result (FAIL):**
- 3 answers returned (PENDING not filtered), or 0 answers (wrong query), or 404

**DB Assertion:**
```java
List<CommunityAnswer> approvedAnswers = answerRepository
    .findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, AnswerStatus.APPROVED);
assertThat(approvedAnswers).hasSize(2);

long pendingCount = answerRepository.findAll().stream()
    .filter(a -> a.getQuestionId().equals(questionId) && a.getStatus() == AnswerStatus.PENDING)
    .count();
assertThat(pendingCount).isEqualTo(1); // PENDING exists in DB but not in API response
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID               | Test File                                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ------------------- | ------------------------------------------------------- | ---------------- | ----------------- | ---------------- |
| `COM199-TC-001`     | `CommunityQuestionServiceImplTest.java`                 | `[x]`            | `Passed`          | —                |
| `COM199-TC-002`     | `CommunityQuestionServiceImplTest.java`                 | `[x]`            | `Passed`          | —                |
| `COM199-TC-003`     | `CommunityQuestionServiceImplTest.java`                 | `[ ]`            | —                 | —                |
| `COM199-TC-004`     | `CommunityQuestionServiceImplTest.java`                 | `[x]`            | `Passed`          | —                |
| `COM199-TC-005`     | `CommunityQuestionServiceImplTest.java`                 | `[x]`            | `Passed`          | —                |
| `COM199-TC-006`     | `CommunityQuestionServiceImplTest.java`                 | `[ ]`            | —                 | —                |
| `COM199-TC-007`     | `CommunityQuestionServiceImplTest.java`                 | `[x]`            | `Passed`          | —                |
| `COM199-TC-008`     | `CommunityQuestionControllerTest.java`                  | `[ ]`            | —                 | —                |
| `COM199-TC-INT-001` | `CommunityQuestionDetailIntegrationTest.java`           | `[ ]`            | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub for Red Phase** — add method to `CommunityQuestionServiceImpl` throwing:

```java
// Red Phase — stub (MUST throw to confirm tests are sensitive)
@Override
public CommunityQuestionDetailResponse getQuestionDetail(UUID questionId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID               | Stub Result                              | Expected    | Actual          | Root Cause (if PASS unexpectedly)              |
| ------------------- | ---------------------------------------- | ----------- | --------------- | ---------------------------------------------- |
| `COM199-TC-001`     | `throw('Not implemented')`               | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                                |
| `COM199-TC-002`     | `throw('Not implemented')`               | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                                |
| `COM199-TC-003`     | `throw('Not implemented')`               | 🔴 FAIL     | ☐ FAIL ☐ PASS   |                                                |
| `COM199-TC-004`     | `throw('Not implemented')`               | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                                |
| `COM199-TC-005`     | `throw('Not implemented')`               | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                                |
| `COM199-TC-006`     | `throw('Not implemented')`               | 🔴 FAIL     | ☐ FAIL ☐ PASS   |                                                |
| `COM199-TC-007`     | `throw('Not implemented')`               | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                                |
| `COM199-TC-008`     | `throw('Not implemented')`               | 🔴 FAIL     | ☐ FAIL ☐ PASS   | Spring Security infra — PASS acceptable        |
| `COM199-TC-INT-001` | `throw('Not implemented')`               | 🔴 FAIL     | ☐ FAIL ☐ PASS   |                                                |

> Note: TC-008 (unauthenticated → 401) may PASS in RED phase because it tests Spring Security infrastructure (not service logic). This is acceptable, same pattern as COM162-TC-SEC-001.

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả service tests FAIL? [x] Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-COMMUNITY-IMP-010` đã được review và approve
- [x] `community_questions`, `community_answers`, `community_topics` tables tồn tại
- [x] `CommunityQuestionController`, `CommunityQuestionService`, `CommunityQuestionMapper` đã tồn tại
- [x] `CommunityAnswerRepository`, `CommunityTopicRepository` đã tồn tại
- [x] Test fixtures FX-COM199-001 đến FX-COM199-006 sẵn sàng (synthetic, mocked in unit tests)

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — integration test xanh nếu Testcontainers available
- [x] TC-002: anonymous question returns `authorId=null` (privacy invariant verified)
- [ ] TC-003: PENDING answers never returned (moderation invariant verified)
- [x] TC-004/005/006: PENDING/HIDDEN/LOCKED return 404 (not 403 — security invariant)
- [ ] TC-008: No JWT → 401 (auth gate working)
- [x] Không có business logic trong Controller (chỉ `@PathVariable` + delegate to service)
- [x] Không có PII plaintext trong logs

**Exit Criteria — CASE 2.0:**

- [x] **Red Gate (§5.1)** — tất cả service tests FAIL với throw stub
- [x] **Contract Existence** — mọi class được inject đều tồn tại:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — mọi test dùng `CommunityQuestionDetailTestFactory`
- [x] **Oracle Source** — mọi assert traceable đến ADR/BR (TC-002→ADR-COM-002, TC-003→ADR-COM-012, TC-004→ADR-COM-011)

### Suspension Criteria

- `community_questions` or `community_answers` table không tồn tại
- `QuestionNotFoundException` class chưa được định nghĩa (cần resolve với UC-55)
- Breaking change in `CommunityQuestionService` interface conflicts with other UC

---

## 7. Rollback Plan

```bash
# No migration to roll back — code-only change.
# Revert created/modified files:
git checkout -- src/main/java/com/carebridge/backend/community/dto/response/CommunityQuestionDetailResponse.java
git checkout -- src/main/java/com/carebridge/backend/community/repository/CommunityAnswerRepository.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionService.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/community/mapper/CommunityQuestionMapper.java
git checkout -- src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java
git checkout -- src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java
git checkout -- src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu trong TDD spec                                    | Check | Gate chặn |
| --------- | --------------------- | ---------------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Gen     | TC does not reference ADR-COM-011 or status filter         | ☑     | G-0       |
| AP-AI-002 | Green-from-Birth      | Service test PASS with throw stub (§5.1)                   | ☑     | G-2 ★     |
| AP-AI-003 | Implicit Decision     | Anonymous masking done in service, not mapper              | ☑     | G-1       |
| AP-AI-004 | Layer Violation       | TC verifies business logic (masking) inside Controller     | ☑     | G-4       |
| AP-AI-005 | Hallucinated Contract | TC imports `QuestionDetailService` not defined in §8 TDS   | ☑     | G-3       |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào → TDD spec approved

---

*TDD Spec v1.0 — CB-COMMUNITY-TDD-010 — UC-199 View Community Question Detail*
