# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-201 Delete Own Answer

**Document ID:** `CB-COMMUNITY-TEST-009`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] HuyND — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — schema oracle
- `04_Implement/UC201_DeleteOwnAnswer/UC201_DeleteOwnAnswer_TDS.md` — TDS
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.13.4` — UC-201 spec

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                              |
| ---------- | ------------------------------------- | ---------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Khởi tạo TDD spec cho UC-201 Delete Own Answer |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                   | Value                                                                        |
| ----------------------- | ---------------------------------------------------------------------------- |
| **Feature / UC ID**     | `UC-201`                                                                     |
| **Module**              | `community — DeleteOwnAnswer`                                               |
| **Spec gốc**            | `CB-COMMUNITY-IMP-009`                                                      |
| **Priority**            | 🟡 P2                                                                        |
| **Milestone**           | `M3 Alpha — 2026-07-11`                                                     |
| **Data Classification** | `Internal`                                                                  |
| **Compliance Scope**    | `BR-RBAC`                                                                   |
| **Upstream Dependencies** | `security (JWT), community (CommunityAnswer, CommunityQuestion)`          |
| **Downstream Consumers** | `community question detail (answer_count), moderation queue`               |

### 1.1 AI Generation Context (CASE 2.0)

| Field               | Value                                                |
| ------------------- | ---------------------------------------------------- |
| **AI Assisted?**    | `Yes`                                                |
| **Constraint Source** | `UC201_TDS §17`, `ADR-COM-201-1`, `ADR-COM-201-3`  |
| **Constraints Injected** | Soft-delete DELETED; decrement answer_count only if APPROVED; no hard delete; MODERATOR bypass |
| **Model**           | `claude-sonnet-4-6`                                  |
| **Trust Level**     | `T2 → T3 (pending Red Gate)`                        |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC nói "soft-deletes according to community policy" không rõ mechanism | ADR-COM-201-2: status=DELETED | Test phải verify status=DELETED sau xóa, không phải physical delete |
| L2 | UC không nêu ảnh hưởng đến answer_count | ADR-COM-201-3: decrement only when prev status=APPROVED | Tests TC-201-1 vs TC-201-2 phân biệt rõ behavior này |
| L3 | answer_count có thể âm nếu không có guard | DB không có UNSIGNED constraint | Service phải có `AND answer_count > 0` guard trong decrementAnswerCount |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
DeleteOwnAnswer bao gồm:
├── Service layer (CommunityAnswerServiceImpl.deleteAnswer)
│   ├── findById → ownership check → capture prevStatus → set DELETED → save → conditional decrement
│   └── mock với Mockito
├── Controller layer (CommunityAnswerController.deleteAnswer)
│   └── @WebMvcTest + MockMvc → HTTP 204/401/403/404
└── Integration (Testcontainers + @SpringBootTest)
    ├── Migration V20260701000002 applied (DELETED status)
    └── DB state: answer DELETED, question.answer_count updated correctly
```

### TDS-02 — Test Basis

- UC-201 (SRS §3.3.13.4): "Soft-deletes the user's own answer according to community policy"
- ADR-COM-201-1: Owner-only, MODERATOR bypass
- ADR-COM-201-2: DELETED status (shared migration with UC-170)
- ADR-COM-201-3: Decrement `answer_count` only if previous status was APPROVED
- V20260701000002: Adds DELETED to both community tables

### TDS-03 — Test Conditions

| TC-ID    | Condition                                          | Technique          | Layer        |
| -------- | -------------------------------------------------- | ------------------ | ------------ |
| TC-201-1 | Author deletes own APPROVED answer                 | Happy path         | Service + IT |
| TC-201-2 | Author deletes own PENDING answer                  | Alternative path   | Service      |
| TC-201-3 | Author deletes own HIDDEN answer                   | Alternative path   | Service      |
| TC-201-4 | Non-owner attempts delete                          | Authorization test | Service + CT |
| TC-201-5 | MODERATOR deletes other user's answer              | Authorization test | Service      |
| TC-201-6 | Delete non-existent answer ID                      | Error path         | CT           |
| TC-201-7 | Unauthenticated request                            | Security test      | Controller   |
| TC-201-8 | Already-DELETED answer (idempotency)               | Idempotency        | Service      |
| TC-201-9 | answer_count never goes negative (guard test)      | Boundary test      | Service      |

### TDS-04 — Test Data Requirements

- All data from `DeleteOwnAnswerTestFactory` (Props Isolation)
- Testcontainers for integration tests (verify DB answer_count)
- No PII in test data

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
package com.carebridge.backend.community;

import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import java.util.UUID;

public class DeleteOwnAnswerTestFactory {

    public static final UUID AUTHOR_ID     = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final UUID NON_OWNER_ID  = UUID.fromString("22222222-0000-0000-0000-000000000002");
    public static final UUID MODERATOR_ID  = UUID.fromString("33333333-0000-0000-0000-000000000003");
    public static final UUID ANSWER_ID     = UUID.fromString("55555555-0000-0000-0000-000000000001");
    public static final UUID QUESTION_ID   = UUID.fromString("44444444-0000-0000-0000-000000000001");

    public static CommunityAnswer makeApprovedAnswer() {
        return CommunityAnswer.builder()
            .id(ANSWER_ID)
            .questionId(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .body("Answer body")
            .personalExperience(false)
            .expertLabeled(false)
            .status(AnswerStatus.APPROVED)
            .build();
    }

    public static CommunityAnswer makePendingAnswer() {
        return makeApprovedAnswer().toBuilder().status(AnswerStatus.PENDING).build();
    }

    public static CommunityAnswer makeHiddenAnswer() {
        return makeApprovedAnswer().toBuilder().status(AnswerStatus.HIDDEN).build();
    }

    public static CommunityAnswer makeDeletedAnswer() {
        return makeApprovedAnswer().toBuilder().status(AnswerStatus.DELETED).build();
    }

    public static CommunityQuestion makeQuestionWithAnswerCount(int count) {
        return CommunityQuestion.builder()
            .id(QUESTION_ID)
            .authorId(UUID.randomUUID())
            .topicId(UUID.randomUUID())
            .title("Parent question")
            .body("Body")
            .stage(PregnancyStage.PREGNANCY)
            .urgency(UrgencyLevel.NORMAL)
            .status(QuestionStatus.APPROVED)
            .answerCount(count)
            .build();
    }
}
```

---

### TC-201-1 — Author deletes APPROVED answer → answer_count decremented

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-201-3 — decrement when prev status = APPROVED; UC-201 happy path
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityAnswer answer = DeleteOwnAnswerTestFactory.makeApprovedAnswer();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
when(answerRepository.save(any())).thenReturn(answer);

// Act
answerService.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

// Assert
ArgumentCaptor<CommunityAnswer> captor = ArgumentCaptor.forClass(CommunityAnswer.class);
verify(answerRepository).save(captor.capture());
assertThat(captor.getValue().getStatus()).isEqualTo(AnswerStatus.DELETED);
// Decrement must be called because previous status was APPROVED
verify(questionRepository).decrementAnswerCount(QUESTION_ID);
```

**Expected persistence:** `community_answers.status = 'DELETED'`, `community_questions.answer_count -= 1`
**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

### TC-201-2 — Author deletes PENDING answer → answer_count NOT decremented

**Severity:** 🟠 High
**Oracle source:** ADR-COM-201-3 — no decrement when prev status = PENDING
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityAnswer pendingAnswer = DeleteOwnAnswerTestFactory.makePendingAnswer();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(pendingAnswer));
when(answerRepository.save(any())).thenReturn(pendingAnswer);

// Act
answerService.deleteAnswer(ANSWER_ID, AUTHOR_ID, false);

// Assert
verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
// NO decrement — PENDING was not counted in answer_count
verify(questionRepository, never()).decrementAnswerCount(any());
```

**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

### TC-201-5 — MODERATOR deletes other user's answer

**Severity:** 🟠 High
**Oracle source:** ADR-COM-201-1 — MODERATOR bypass
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange — MODERATOR_ID is not the author
CommunityAnswer answer = DeleteOwnAnswerTestFactory.makeApprovedAnswer();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
when(answerRepository.save(any())).thenReturn(answer);

// Act — isModerator=true bypasses ownership check
answerService.deleteAnswer(ANSWER_ID, MODERATOR_ID, true);

// Assert
verify(answerRepository).save(argThat(a -> a.getStatus() == AnswerStatus.DELETED));
```

**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

### TC-201-9 — answer_count never goes negative

**Severity:** 🟠 High
**Oracle source:** Data integrity — `AND answer_count > 0` guard in decrementAnswerCount
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Description:** When `answer_count = 0` on the parent question (data inconsistency edge case), calling decrementAnswerCount must not set it to -1. The `AND answer_count > 0` guard ensures this.

**Verification approach:** Integration test — create question with `answer_count=0`, delete an APPROVED answer, verify `answer_count` stays at 0.

**Intended test file:** `CommunityAnswerIntegrationTest.java`

---

## 5. Red-Green-Refactor Tracker

| TC-ID    | Test File                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| -------- | --------------------------------------- | ---------------- | ----------------- | ---------------- |
| TC-201-1 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-2 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-3 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-4 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-5 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-6 | `CommunityAnswerControllerTest.java`    | [ ]              | —                 | —                |
| TC-201-7 | `CommunityAnswerControllerTest.java`    | [ ]              | —                 | —                |
| TC-201-8 | `CommunityAnswerServiceImplTest.java`   | [ ]              | —                 | —                |
| TC-201-9 | `CommunityAnswerIntegrationTest.java`   | [ ]              | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0) — Gate 2

**Red Gate Stub:**
```java
public void deleteAnswer(UUID answerId, UUID callerId, boolean isModeratorCaller) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC-ID    | Expected  | Actual           |
| -------- | --------- | ---------------- |
| TC-201-1 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-201-2 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-201-5 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |

Tất cả FAIL? [ ] Yes [ ] No

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-COMMUNITY-IMP-009` approved
- [ ] Migration `V20260701000002__add_deleted_status_community.sql` applied
- [ ] `AnswerStatus.DELETED` added to enum
- [ ] `decrementAnswerCount()` method added to `CommunityQuestionRepository`
- [ ] Red Gate stubs in place

### Exit Criteria (DoD)
- [ ] All 9 TCs pass (`./mvnw test -Dtest=CommunityAnswerServiceImplTest,CommunityAnswerControllerTest,CommunityAnswerIntegrationTest`)
- [ ] TC-201-1 (approved → decrement), TC-201-2 (pending → no decrement), TC-201-9 (no negative count) must pass
- [ ] Red Gate confirmed
- [ ] Props Isolation factory used for all test data

---

## 7. Rollback Plan

```bash
git revert <commit-hash-of-deleteAnswer-implementation>
# DB rollback shared with UC-170: see UC170_TDS.md §12
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern                         | Signal                               | Check | Gate  |
| ----- | ------------------------------------ | ------------------------------------ | ----- | ----- |
| AP-1  | Test passes against stub             | TC-201-1 passes with stub            | [ ]   | RG-2  |
| AP-2  | Hard delete tested                   | `verify(repo.delete())` assertion    | [ ]   | CG-2  |
| AP-3  | Always decrement answer_count        | TC-201-2 not in test file            | [ ]   | CG-2  |
| AP-4  | answer_count negative edge case skipped | No TC-201-9 in test file          | [ ]   | CG-2  |
