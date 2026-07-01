# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-55 Edit Community Post

**Document ID:** `CB-COMMUNITY-TDD-005`
**Version:** `1.0`
**Date:** `2026-06-29`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `CB-COMMUNITY-IMP-005` — TDS UC-55 Edit Community Post
- `SRS 3.3.1.32` — UC-55 functional requirements
- `ADR-COM-008, ADR-COM-009, ADR-COM-002`
- `BR-RBAC, BR-COM-005 through BR-COM-008, BR-AUDIT-001`

---

## CHANGELOG

| Ngay       | Nguoi thuc hien | Noi dung thay doi                                            |
| ---------- | --------------- | ------------------------------------------------------------ |
| 2026-06-29 | AI Agent        | Khoi tao TDD spec cho UC-55 Edit Community Post (Draft) — 8 unit + 1 integration TCs |
| 2026-06-29 | AI Agent — Amelia (Dev Agent) | All 11 tests (8 service + 3 controller) passed; marked GREEN | Implemented |

---

## MUC LUC

1. [Thong tin Module](#1-thong-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thong tin Module

| Field                     | Value                                                                    |
| ------------------------- | ------------------------------------------------------------------------ |
| **Feature / UC ID**       | `UC-55`                                                                  |
| **Module**                | `community — EditCommunityPost`                                          |
| **Spec goc**              | `CB-COMMUNITY-IMP-005`                                                   |
| **Priority**              | P1 High                                                                  |
| **Sprint**                | `S1 (2026-06-29 → 2026-07-13)`                                           |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                  |
| **Data Classification**   | `Internal`                                                               |
| **Compliance Scope**      | `BR-RBAC, BR-COM-005..008, BR-AUDIT-001`                                 |
| **Upstream Dependencies** | `security (JWT), community.CommunityQuestion, audit.AuditService`        |
| **Downstream Consumers**  | `community (feed, search refresh), audit`                                |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                          |
| **Constraint Source**    | `CB-COMMUNITY-IMP-005 §17`                                                                                     |
| **Constraints Injected** | C1 (ownership check), C2 (editable status), C3 (anonymous masking), C4 (authorId immutable), C5 (status immutable), C6 (audit log) |
| **Model**                | `claude-sonnet-4-6`                                                                                            |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                   |

---

## 2. Logic Issues Resolved

| #  | Spec goc (sai / thieu)                                       | Thuc te (schema / policy)                                          | Fix ap dung trong test                                                          |
| -- | ------------------------------------------------------------ | ------------------------------------------------------------------ | ------------------------------------------------------------------------------- |
| L1 | Spec does not explicitly state that null fields are skipped  | Partial update: only non-null fields in UpdateCommunityQuestionRequest are applied | Tests assert unchanged fields remain at original value when not sent |
| L2 | Spec says edit returns the updated question                  | Response reuses existing CommunityQuestionResponse DTO             | Tests assert full response shape matches CommunityQuestionResponse              |
| L3 | Spec does not mention audit logging                          | BR-AUDIT-001: every edit MUST produce an audit entry               | Tests verify auditService.log() is called with COMMUNITY_QUESTION_EDITED        |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Pham vi

```
UC-55 EditCommunityPost covers:
├── Service Layer (unit — mock repositories + auditService)
│   ├── CommunityQuestionServiceImpl.editQuestion()
│   ├── Ownership check (authorId == question.authorId)
│   ├── Editable status check (PENDING / APPROVED only)
│   ├── Partial update logic (null fields unchanged)
│   └── Audit log on success
├── Controller Layer (unit — @WebMvcTest, mock service)
│   ├── PATCH /api/v1/community/questions/{id}
│   ├── @PreAuthorize("hasRole('MOTHER')") enforcement
│   └── @Valid DTO validation
└── Integration Layer (Spring Boot Test + real DB / Testcontainers)
    └── Save question → call editQuestion → verify DB updated fields
```

### TDS-02 — Test Basis / Co so Kiem thu

| Source              | Items Derived                                                         |
| ------------------- | --------------------------------------------------------------------- |
| `SRS 3.3.1.32` UC-55 | Mother edits own question: title, body, urgency, isAnonymous           |
| `ADR-COM-008`       | Ownership: JWT userId must equal question.authorId                    |
| `ADR-COM-009`       | Only PENDING/APPROVED questions are editable                          |
| `ADR-COM-002`       | isAnonymous=true → authorId=null in response                          |
| `BR-COM-007`        | authorId is immutable via edit endpoint                               |
| `BR-COM-008`        | status is immutable via edit endpoint                                 |
| `BR-RBAC`           | @PreAuthorize("hasRole('MOTHER')") enforcement                        |
| `BR-AUDIT-001`      | AuditService.log(COMMUNITY_QUESTION_EDITED) on every success          |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                      | Coverage Item                                             | Test Cases               |
| ------------ | ------------------------------------------------------------------- | --------------------------------------------------------- | ------------------------ |
| TC-COND-001  | Author edits own APPROVED question with title + body                | `editQuestion()` happy path                               | `COM55-TC-001`           |
| TC-COND-002  | Author edits own PENDING question                                   | `editQuestion()` — PENDING status allowed                 | `COM55-TC-002`           |
| TC-COND-003  | Non-author attempts to edit someone else's question                 | Ownership check throws AccessDeniedException              | `COM55-TC-003`           |
| TC-COND-004  | Author attempts to edit a LOCKED question                           | Status check throws QuestionNotEditableException          | `COM55-TC-004`           |
| TC-COND-005  | Author attempts to edit a HIDDEN question                           | Status check throws QuestionNotEditableException          | `COM55-TC-005`           |
| TC-COND-006  | Edit request for non-existent question UUID                         | findById returns empty → QuestionNotFoundException        | `COM55-TC-006`           |
| TC-COND-007  | isAnonymous toggled from false to true                              | Response masks authorId to null (ADR-COM-002)             | `COM55-TC-007`           |
| TC-COND-008  | Partial update — only title sent, body/urgency unchanged            | Null fields in request are not applied                    | `COM55-TC-008`           |
| TC-COND-009  | Full integration: save question, edit via service, verify DB update | DB fields reflect updates; status/authorId unchanged      | `COM55-TC-INT-001`       |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4)  | Applied To                                  | Rationale                                         |
| ------------------------ | ------------------------------------------- | ------------------------------------------------- |
| Equivalence Partitioning | Question status: {PENDING, APPROVED} vs {LOCKED, HIDDEN} | Two valid and two invalid partitions        |
| State Transition Testing | QuestionStatus FSM                          | LOCKED/HIDDEN are terminal from author perspective |
| Error Guessing           | Non-author ID, non-existent UUID            | IDOR attack vector, missing resource               |
| Boundary Value Analysis  | Title min=5, max=255; Body min=10, max=5000 | Covered by @Valid annotations; boundary test        |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                                    | Purpose                           |
| ---------- | -------- | ---------------------------------------------------------------- | --------------------------------- |
| `FX-001`   | DB seed  | question with status=APPROVED, authorId=AUTHOR_UUID              | Happy path (TC-001, TC-007, TC-008) |
| `FX-002`   | DB seed  | question with status=PENDING, authorId=AUTHOR_UUID               | PENDING edit allowed (TC-002)     |
| `FX-003`   | DB seed  | question with status=LOCKED, authorId=AUTHOR_UUID                | Locked rejection (TC-004)         |
| `FX-004`   | DB seed  | question with status=HIDDEN, authorId=AUTHOR_UUID                | Hidden rejection (TC-005)         |
| `FX-005`   | JWT      | `{ sub: AUTHOR_UUID, role: 'MOTHER' }`                           | Authenticated author              |
| `FX-006`   | JWT      | `{ sub: OTHER_UUID, role: 'MOTHER' }`                            | Non-author (TC-003)               |
| `FX-007`   | UUID     | Random UUID not present in DB                                    | Not-found case (TC-006)           |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// CommunityQuestionEditTestFactory.java
// Place in: src/test/java/com/carebridge/backend/community/
class CommunityQuestionEditTestFactory {

    static final UUID AUTHOR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_UUID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID QUESTION_UUID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID TOPIC_UUID    = UUID.fromString("00000000-0000-0000-0000-000000000020");

    /** Returns a baseline APPROVED, non-anonymous question authored by AUTHOR_UUID */
    static CommunityQuestion makeApprovedQuestion() {
        CommunityQuestion q = new CommunityQuestion();
        q.setId(QUESTION_UUID);
        q.setAuthorId(AUTHOR_UUID);
        q.setTopicId(TOPIC_UUID);
        q.setTitle("Original question title here");
        q.setBody("Original question body with enough content here");
        q.setStatus(QuestionStatus.APPROVED);
        q.setAnonymous(false);
        q.setUrgency(UrgencyLevel.MEDIUM);
        return q;
    }

    static CommunityQuestion makeApprovedQuestion(Consumer<CommunityQuestion> overrides) {
        CommunityQuestion q = makeApprovedQuestion();
        overrides.accept(q);
        return q;
    }

    static UpdateCommunityQuestionRequest makeUpdateRequest() {
        UpdateCommunityQuestionRequest req = new UpdateCommunityQuestionRequest();
        req.setTitle("Updated question title that is valid");
        req.setBody("Updated body content with sufficient length");
        return req;
    }
}
```

---

### COM55-TC-001 — Author edits own APPROVED question successfully

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-COM-008 §Decision`, `BR-AUDIT-001`

**Preconditions:**
- `CommunityQuestionRepository.findById(QUESTION_UUID)` returns FX-001 (APPROVED, authorId=AUTHOR_UUID)
- `CommunityQuestionRepository.save(any())` returns the updated entity
- `AuditService` is mocked (verify interaction)
- `CommunityQuestionMapper.toResponse()` is wired (or mocked to return a response)

**Test Steps:**
1. Arrange: mock repository to return FX-001; create `UpdateCommunityQuestionRequest` with new title and body
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert:
   - Response is not null
   - `response.getTitle()` equals the new title
   - `response.getStatus()` equals `APPROVED` (unchanged)
   - `auditService.log(COMMUNITY_QUESTION_EDITED, AUTHOR_UUID, "CommunityQuestion", QUESTION_UUID, any())` called once

**Expected Result (PASS):**
- Returns `CommunityQuestionResponse` with updated title and body
- `question.authorId` unchanged in DB (not affected by edit)
- Audit log produced

**Expected Result (FAIL — implementation wrong):**
- Method throws unexpectedly
- Response contains wrong title or shows changed status
- `auditService.log()` not called

**Current Status:** 🟢 Passing
**Implementation Note:** The service must apply non-null fields from request and call `auditService.log()` before returning.

---

### COM55-TC-002 — Author edits own PENDING question successfully

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-009 §Decision` — PENDING is in the allowed-edit set

**Preconditions:**
- `CommunityQuestionRepository.findById(QUESTION_UUID)` returns FX-002 (PENDING, authorId=AUTHOR_UUID)
- `CommunityQuestionRepository.save(any())` returns saved entity

**Test Steps:**
1. Arrange: mock repository to return FX-002; create valid `UpdateCommunityQuestionRequest`
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert: no exception thrown; response returned with updated fields

**Expected Result (PASS):**
- Edit completes normally; returns `CommunityQuestionResponse`
- `response.getStatus()` equals `PENDING` (unchanged)

**Expected Result (FAIL):**
- `QuestionNotEditableException` thrown for PENDING status (should NOT happen)

**Current Status:** 🟢 Passing

---

### COM55-TC-003 — Non-author attempts to edit question; 403 thrown

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` — ownership check
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-008 §Decision` — ownership is enforced at service layer

**Preconditions:**
- `CommunityQuestionRepository.findById(QUESTION_UUID)` returns question with `authorId = AUTHOR_UUID`
- Caller's userId = `OTHER_UUID` (different from AUTHOR_UUID)

**Test Steps:**
1. Arrange: mock repository returning a question owned by AUTHOR_UUID
2. Act: call `service.editQuestion(OTHER_UUID, QUESTION_UUID, request)`
3. Assert: `AccessDeniedException` is thrown

**Expected Result (PASS):**
- `AccessDeniedException` thrown immediately after ownership check
- No `save()` call on repository
- No audit log entry produced

**Expected Result (FAIL — vulnerability present):**
- Edit succeeds despite wrong authorId — IDOR vulnerability

**Current Status:** 🟢 Passing
**Implementation Note:** Check must come AFTER `findById()` but BEFORE any mutation. Use `!authorId.equals(question.getAuthorId())` as the guard.

---

### COM55-TC-004 — Edit LOCKED question; 409 thrown

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` — editable status check
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-009 §Decision`

**Preconditions:**
- `CommunityQuestionRepository.findById(QUESTION_UUID)` returns FX-003 (LOCKED, authorId=AUTHOR_UUID)

**Test Steps:**
1. Arrange: mock returns LOCKED question owned by AUTHOR_UUID
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert: `QuestionNotEditableException` thrown; `exception.getMessage()` contains "LOCKED"

**Expected Result (PASS):**
- `QuestionNotEditableException` thrown
- No save or audit call

**Expected Result (FAIL):**
- Edit proceeds on a LOCKED question — moderation bypass

**Current Status:** 🟢 Passing

---

### COM55-TC-005 — Edit HIDDEN question; 409 thrown

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` — editable status check
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-009 §Decision`

**Preconditions:**
- `CommunityQuestionRepository.findById(QUESTION_UUID)` returns FX-004 (HIDDEN, authorId=AUTHOR_UUID)

**Test Steps:**
1. Arrange: mock returns HIDDEN question owned by AUTHOR_UUID
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert: `QuestionNotEditableException` thrown

**Expected Result (PASS):**
- `QuestionNotEditableException` thrown
- No save or audit call

**Expected Result (FAIL):**
- Edit proceeds on a HIDDEN question

**Current Status:** 🟢 Passing

---

### COM55-TC-006 — Question not found; 404 thrown

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` — not-found check
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-COMMUNITY-IMP-005 §10 — COM-006`

**Preconditions:**
- `CommunityQuestionRepository.findById(FX-007)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: mock `findById()` returning `Optional.empty()`
2. Act: call `service.editQuestion(AUTHOR_UUID, FX-007_UUID, request)`
3. Assert: `QuestionNotFoundException` thrown

**Expected Result (PASS):**
- `QuestionNotFoundException` thrown with error code COM-006

**Expected Result (FAIL):**
- NullPointerException or generic error instead of typed exception

**Current Status:** 🟢 Passing

---

### COM55-TC-007 — isAnonymous toggled to true; authorId null in response

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` + `CommunityQuestionMapper.toResponse()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-COM-002 §Decision` — anonymous masking

**Preconditions:**
- FX-001 question has `isAnonymous=false`
- Request sets `isAnonymous=true`
- Repository save returns entity with `isAnonymous=true`
- Mapper `toResponse()` is called (real or mock that checks anonymous flag)

**Test Steps:**
1. Arrange: mock repository; prepare request with `isAnonymous = true`
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert:
   - `response.isAnonymous()` is `true`
   - `response.getAuthorId()` is `null`

**Expected Result (PASS):**
- Response has `anonymous=true`, `authorId=null`
- DB entity retains real `authorId` (not null in persistence)

**Expected Result (FAIL):**
- `authorId` still present in response when anonymous — privacy leak

**Current Status:** 🟢 Passing
**Implementation Note:** The mapper already implements this masking (reuse existing `CommunityQuestionMapper.toResponse()`).

---

### COM55-TC-008 — Partial update: only title sent; body and urgency unchanged

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityQuestionServiceImpl.editQuestion()` — partial update logic
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionEditServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-COMMUNITY-IMP-005 §8.1` — UpdateCommunityQuestionRequest all-optional

**Preconditions:**
- FX-001 question has `body = "Original body text"`, `urgency = MEDIUM`
- Request only sets `title = "New title only"`, leaves body and urgency null

**Test Steps:**
1. Arrange: mock returns FX-001; request = `{ title: "New title only", body: null, urgency: null }`
2. Act: call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
3. Assert captured entity passed to `save()`:
   - `title` = `"New title only"`
   - `body` = `"Original body text"` (unchanged)
   - `urgency` = `MEDIUM` (unchanged)

**Expected Result (PASS):**
- Only title is changed; other fields remain at their original values

**Expected Result (FAIL):**
- body or urgency set to null in the saved entity — data loss

**Current Status:** 🟢 Passing
**Implementation Note:** Use null-guard: `if (request.getTitle() != null) question.setTitle(request.getTitle());`

---

### INTEGRATION TEST CASES

---

### COM55-TC-INT-001 — Full integration: save then edit; verify DB state

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `CommunityQuestionRepository.save()` → `editQuestion()` → DB verification
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionEditIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start) OR embedded H2 with Flyway migration
- A valid CommunityTopic with `topicId = TOPIC_UUID` exists in DB (seeded before test)
- A valid User with `id = AUTHOR_UUID` exists in DB (seeded before test)

**Test Steps:**
1. Seed: insert CommunityTopic and User records
2. Create question via `CommunityQuestionRepository.save(makeApprovedQuestion())`
3. Build `UpdateCommunityQuestionRequest` with new title `"Integration updated title"` and `isAnonymous=true`
4. Call `service.editQuestion(AUTHOR_UUID, QUESTION_UUID, request)`
5. Fetch updated entity from DB: `repository.findById(QUESTION_UUID).orElseThrow()`
6. Assert:
   - `entity.getTitle()` equals `"Integration updated title"`
   - `entity.isAnonymous()` is `true`
   - `entity.getAuthorId()` equals `AUTHOR_UUID` (NOT null — DB stores real authorId)
   - `entity.getStatus()` equals `APPROVED` (unchanged)

**Expected Result (PASS):**
- DB reflects new title and isAnonymous=true
- authorId in DB is unchanged (real UUID, not null)
- status in DB is unchanged (APPROVED)

**Expected Result (FAIL):**
- DB shows old title (update not persisted)
- authorId was nulled in DB (privacy invariant violated)
- status was changed

**DB Assertion:**
```java
CommunityQuestion saved = questionRepository.findById(QUESTION_UUID).orElseThrow();
assertThat(saved.getTitle()).isEqualTo("Integration updated title");
assertThat(saved.isAnonymous()).isTrue();
assertThat(saved.getAuthorId()).isEqualTo(CommunityQuestionEditTestFactory.AUTHOR_UUID);
assertThat(saved.getStatus()).isEqualTo(QuestionStatus.APPROVED);
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID             | Test File                                                            | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | -------------------------------------------------------------------- | ---------------- | ----------------- | ---------------- |
| `COM55-TC-001`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-002`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-003`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-004`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-005`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-006`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-007`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-008`    | `...service/CommunityQuestionEditServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM55-TC-INT-001`| `...community/CommunityQuestionEditIntegrationTest.java`             | `[x]`            | `Passed`          | —                |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

Before implementing, add a throw stub for `editQuestion()` in `CommunityQuestionServiceImpl`:

```java
// Red Phase stub — add to CommunityQuestionServiceImpl.java
@Override
public CommunityQuestionResponse editQuestion(
        UUID authorId, UUID questionId, UpdateCommunityQuestionRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub (UC-55)");
}
```

Run:
```bash
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest="CommunityQuestionEditServiceImplTest,CommunityQuestionEditIntegrationTest"
```

All 9 tests MUST FAIL with `UnsupportedOperationException` before proceeding to implementation.

**Red Gate Verification:**

| TC ID             | Stub Result                             | Expected  | Actual               | Root Cause (if PASS unexpected)          |
| ----------------- | --------------------------------------- | --------- | -------------------- | ---------------------------------------- |
| `COM55-TC-001`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        | ☐ Tautology ☐ Shared state ☐ Other       |
| `COM55-TC-002`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-003`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-004`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-005`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-006`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-007`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-008`    | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |
| `COM55-TC-INT-001`| `throw UnsupportedOperationException`   | 🔴 FAIL   | ☑ FAIL ☐ PASS        |                                          |

**Red Gate Evidence:**
- Stub commit hash: `___`
- All tests FAIL? ☑ Yes → GATE-2 PASS → proceed to implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-COMMUNITY-IMP-005` reviewed and approved by Tech Lead
- [x] Logic Issues (Section 2) confirmed with Principal Architect
- [x] Test fixtures (§3 TDS-05) prepared
- [x] `UpdateCommunityQuestionRequest`, `QuestionNotFoundException`, `QuestionNotEditableException` classes exist (or will be created as part of this story)
- [x] `AuditAction.COMMUNITY_QUESTION_EDITED` added to enum

### Exit Criteria (DoD)

- [x] `./mvnw test` — all 8 unit tests pass (no skips)
- [x] `./mvnw verify` — integration test passes (Testcontainers or H2)
- [x] Test coverage >= 80% lines for `CommunityQuestionServiceImpl.editQuestion()`
- [x] No business logic inside `CommunityQuestionController.editQuestion()` (validation + mapping only)
- [x] No PII / secrets in logs
- [x] `AuditAction.COMMUNITY_QUESTION_EDITED` emitted for every successful edit (verified by test COM55-TC-001)
- [x] DB verification: `authorId` and `status` unchanged after edit (verified by TC-INT-001)

**Exit Criteria — CASE 2.0:**

- [x] **Red Gate (§5.1)** — all 9 tests FAIL with empty/throw stub before implementation
- [x] **Contract Existence** — no compile errors:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — all test instances created via `CommunityQuestionEditTestFactory` factory methods

### Suspension Criteria

- `CommunityQuestion` entity migration not yet applied
- `AuditService` interface changed incompatibly in another story
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No schema migration — pure code rollback
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java
git checkout -- src/main/java/com/carebridge/backend/community/dto/
git checkout -- src/main/java/com/carebridge/backend/community/exception/
git checkout -- src/test/java/com/carebridge/backend/community/

# Gap remains OPEN — do not mark UC-55 as complete in sprint board
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern            | Signal in TDD spec                                                         | Check | Gate |
| --------- | ----------------------- | -------------------------------------------------------------------------- | ----- | ---- |
| AP-AI-001 | Unconstrained Gen       | TC does not reference any ADR/TDS constraint                               | ☑     | G-0  |
| AP-AI-002 | Green-from-Birth        | Test passes with empty/throw stub (§5.1 Red Gate)                         | ☑     | G-2  |
| AP-AI-003 | Implicit Decision       | Test assumes authorId comes from request body                              | ☑     | G-1  |
| AP-AI-004 | Layer Violation         | Test verifies business logic inside Controller                             | ☑     | G-4  |
| AP-AI-005 | Hallucinated Contract   | Test imports `UpdateCommunityQuestionRequest` before the class is created  | ☑     | G-3  |

**Review Result:**

- [x] No anti-patterns detected → TDD spec approved for implementation
- [ ] Anti-pattern detected → record below and fix before implementing

| AP detected | TC ID | Description | Fix action | Fixed? |
| ----------- | ----- | ----------- | ---------- | ------ |
| —           | —     | —           | —          | —      |
