# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-170 Delete Community Post

**Document ID:** `CB-COMMUNITY-TEST-006`
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
- `04_Implement/UC170_DeleteCommunityPost/UC170_DeleteCommunityPost_TDS.md` — TDS
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.8.3` — UC-170 spec

> **TDD Convention:** Write tests BEFORE production code. Red 🔴 → Green 🟢 → Refactor 🔵. Do NOT mark passing without running `./mvnw test`.

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                                    |
| ---------- | ------------------------------------- | ---------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Khởi tạo TDD spec cho UC-170 Delete Community Post  |

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

| Field                   | Value                                                     |
| ----------------------- | --------------------------------------------------------- |
| **Feature / UC ID**     | `UC-170`                                                  |
| **Module**              | `community — DeleteCommunityPost`                        |
| **Spec gốc**            | `CB-COMMUNITY-IMP-006`                                   |
| **Priority**            | 🟠 P1                                                     |
| **Milestone**           | `M3 Alpha — 2026-07-11`                                  |
| **Data Classification** | `Internal`                                               |
| **Compliance Scope**    | `BR-RBAC`                                                |
| **Upstream Dependencies** | `security (JWT), community (CommunityQuestion)`        |
| **Downstream Consumers** | `community feed, community search`                      |

### 1.1 AI Generation Context (CASE 2.0)

| Field               | Value                                                    |
| ------------------- | -------------------------------------------------------- |
| **AI Assisted?**    | `Yes`                                                    |
| **Constraint Source** | `UC170_TDS §17`, `ADR-COM-170-1`, `ADR-COM-170-2`    |
| **Constraints Injected** | Soft-delete DELETED status; ownership check before status; LOCKED guard; migration required |
| **Model**           | `claude-sonnet-4-6`                                      |
| **Trust Level**     | `T2 → T3 (pending Red Gate)`                            |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC mô tả "Deletes or archives" không rõ mechanism | DB có CHECK constraint: `('PENDING','APPROVED','HIDDEN','LOCKED')` — cần add DELETED | Test phải verify migration V20260701000002 được apply và status=DELETED sau xóa |
| L2 | UC không đề cập HIDDEN status có bị xóa không | HIDDEN posts vẫn là owned content — author có thể xóa | Test TC-170 phải include test xóa HIDDEN post |
| L3 | Idempotency không được nêu trong UC | Good API design: double-delete = 204 | Add idempotency test TC-170-8 |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
DeleteCommunityPost bao gồm:
├── Service layer (CommunityQuestionServiceImpl.deleteQuestion)
│   ├── findById → ownership check → status guard → save DELETED
│   └── mock CommunityQuestionRepository với Mockito
├── Controller layer (CommunityQuestionController.deleteQuestion)
│   └── @WebMvcTest + MockMvc → verify HTTP 204/401/403/404/409
└── Integration (Testcontainers PostgreSQL + @SpringBootTest)
    ├── Migration V20260701000002 applied
    └── DB state: status=DELETED persisted
```

### TDS-02 — Test Basis

- UC-170 (SRS §3.3.8.3): "Deletes or archives the user's own post when not locked or under investigation"
- ADR-COM-170-1: Ownership check — author OR MODERATOR
- ADR-COM-170-2: DELETED status (not HIDDEN) for soft-delete
- V1__init_schema.sql: `community_questions_status_check` constraint
- V20260701000002: Adds DELETED to CHECK constraint

### TDS-03 — Test Conditions

| TC-ID    | Condition                             | Technique          | Layer        |
| -------- | ------------------------------------- | ------------------ | ------------ |
| TC-170-1 | Author deletes APPROVED question      | Happy path         | Service + IT |
| TC-170-2 | Author deletes PENDING question       | Alternative path   | Service      |
| TC-170-3 | Author deletes HIDDEN question        | Alternative path   | Service      |
| TC-170-4 | Delete LOCKED question                | Error path         | Service      |
| TC-170-5 | Non-owner tries to delete             | Authorization test | Service + CT |
| TC-170-6 | MODERATOR deletes other user's post   | Authorization test | Service      |
| TC-170-7 | Delete non-existent question          | Error path         | Service + CT |
| TC-170-8 | Delete already-DELETED (idempotency)  | Idempotency        | Service      |
| TC-170-9 | Unauthenticated request               | Security test      | Controller   |

Legend: IT = Integration Test, CT = Controller Test (WebMvcTest)

### TDS-04 — Test Data Requirements

- All test data created via `DeleteCommunityPostTestFactory` (Props Isolation)
- No real PII — synthetic UUIDs only
- Testcontainers PostgreSQL for integration tests
- Clock: `Instant.now()` — no clock manipulation needed
- Cleanup: `@Transactional` on each integration test method

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
package com.carebridge.backend.community;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import java.util.UUID;

public class DeleteCommunityPostTestFactory {

    public static final UUID AUTHOR_ID    = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final UUID NON_OWNER_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
    public static final UUID MODERATOR_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");
    public static final UUID QUESTION_ID  = UUID.fromString("44444444-0000-0000-0000-000000000004");

    public static CommunityQuestion makeApprovedQuestion() {
        return CommunityQuestion.builder()
            .id(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .topicId(UUID.randomUUID())
            .title("Test question")
            .body("Test body content for approved question")
            .stage(PregnancyStage.PREGNANCY)
            .urgency(UrgencyLevel.NORMAL)
            .status(QuestionStatus.APPROVED)
            .build();
    }

    public static CommunityQuestion makePendingQuestion() {
        return makeApprovedQuestion().toBuilder().status(QuestionStatus.PENDING).build();
    }

    public static CommunityQuestion makeHiddenQuestion() {
        return makeApprovedQuestion().toBuilder().status(QuestionStatus.HIDDEN).build();
    }

    public static CommunityQuestion makeLockedQuestion() {
        return makeApprovedQuestion().toBuilder().status(QuestionStatus.LOCKED).build();
    }

    public static CommunityQuestion makeDeletedQuestion() {
        return makeApprovedQuestion().toBuilder().status(QuestionStatus.DELETED).build();
    }
}
```

---

### TC-170-1 — Author deletes own APPROVED question

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-170-2 (DELETED status); UC-170 happy path
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Preconditions:**
- Question exists with `status = APPROVED` and `authorId = AUTHOR_ID`
- Migration V20260701000002 applied

**Arrange / Act / Assert (Service Unit Test):**
```java
// Arrange
CommunityQuestion question = DeleteCommunityPostTestFactory.makeApprovedQuestion();
when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
when(questionRepository.save(any())).thenReturn(question);

// Act
questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false);

// Assert
ArgumentCaptor<CommunityQuestion> captor = ArgumentCaptor.forClass(CommunityQuestion.class);
verify(questionRepository).save(captor.capture());
assertThat(captor.getValue().getStatus()).isEqualTo(QuestionStatus.DELETED);
```

**Expected persistence:** `community_questions.status = 'DELETED'`
**Intended test file:** `CommunityQuestionServiceImplTest.java`
**Failure signature:** `AssertionError: expected DELETED but was HIDDEN` (wrong status) or `NoMethodError` (method not implemented)

---

### TC-170-4 — Attempt delete LOCKED question

**Severity:** 🔴 Critical
**Oracle source:** BR-COM-170-1 — "not locked or under investigation"
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityQuestion locked = DeleteCommunityPostTestFactory.makeLockedQuestion();
when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(locked));

// Act + Assert
assertThatThrownBy(() -> questionService.deleteQuestion(QUESTION_ID, AUTHOR_ID, false))
    .isInstanceOf(QuestionLockedException.class);
verify(questionRepository, never()).save(any());
```

**Expected HTTP:** `409 Conflict` with `{"code":"QUESTION_LOCKED"}`
**Intended test file:** `CommunityQuestionServiceImplTest.java` + `CommunityQuestionControllerTest.java`

---

### TC-170-5 — Non-owner attempts delete

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-170-1 — ownership check
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityQuestion question = DeleteCommunityPostTestFactory.makeApprovedQuestion();
when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

// Act + Assert
assertThatThrownBy(() -> questionService.deleteQuestion(QUESTION_ID, NON_OWNER_ID, false))
    .isInstanceOf(AccessDeniedException.class);
verify(questionRepository, never()).save(any());
```

**Expected HTTP:** `403 Forbidden`
**Intended test file:** `CommunityQuestionServiceImplTest.java`

---

### TC-170-6 — MODERATOR deletes other user's question

**Severity:** 🟠 High
**Oracle source:** ADR-COM-170-1 — MODERATOR bypass
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityQuestion question = DeleteCommunityPostTestFactory.makeApprovedQuestion();
when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
when(questionRepository.save(any())).thenReturn(question);

// Act — MODERATOR_ID is not the author but isModerator=true
questionService.deleteQuestion(QUESTION_ID, MODERATOR_ID, true);

// Assert
verify(questionRepository).save(argThat(q -> q.getStatus() == QuestionStatus.DELETED));
```

**Intended test file:** `CommunityQuestionServiceImplTest.java`

---

### TC-170-9 — Unauthenticated request returns 401

**Severity:** 🔴 Critical
**Oracle source:** Security config — all `/api/v1/**` requires authentication
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert (Controller Test):**
```java
mockMvc.perform(delete("/api/v1/community/questions/{id}", QUESTION_ID))
    .andExpect(status().isUnauthorized());
```

**Intended test file:** `CommunityQuestionControllerTest.java`

---

## 5. Red-Green-Refactor Tracker

| TC-ID    | Test File                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| -------- | --------------------------------------- | ---------------- | ----------------- | ---------------- |
| TC-170-1 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-2 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-3 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-4 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-5 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-6 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-7 | `CommunityQuestionControllerTest.java`  | [ ]              | —                 | —                |
| TC-170-8 | `CommunityQuestionServiceImplTest.java` | [ ]              | —                 | —                |
| TC-170-9 | `CommunityQuestionControllerTest.java`  | [ ]              | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0) — Gate 2

**Red Gate Stub:**
```java
// CommunityQuestionServiceImpl.deleteQuestion() Red Phase stub
public void deleteQuestion(UUID questionId, UUID callerId, boolean isModeratorCaller) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC-ID    | Expected  | Actual           |
| -------- | --------- | ---------------- |
| TC-170-1 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-170-4 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-170-5 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-170-9 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |

Tất cả FAIL? [ ] Yes [ ] No

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-COMMUNITY-IMP-006` approved
- [ ] Migration `V20260701000002__add_deleted_status_community.sql` exists
- [ ] `QuestionStatus.DELETED` added to enum
- [ ] Red Gate stubs in place

### Exit Criteria (DoD)
- [ ] All 9 TCs pass (`./mvnw test -Dtest=CommunityQuestionServiceImplTest,CommunityQuestionControllerTest`)
- [ ] TC-170-1 (happy path), TC-170-4 (locked), TC-170-5 (non-owner), TC-170-9 (unauth) must pass
- [ ] Red Gate confirmed: stubs cause test failure before implementation
- [ ] Props Isolation factory used for all test data
- [ ] No physical DELETE in any test assertion (verify soft-delete only)

---

## 7. Rollback Plan

```bash
# Revert application code
git revert <commit-hash-of-deleteQuestion-implementation>

# DB rollback (if migration was applied)
# See UC170_TDS.md §12 for SQL rollback
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern                      | Signal                         | Check | Gate  |
| ----- | --------------------------------- | ------------------------------ | ----- | ----- |
| AP-1  | Test passes against no-op stub    | TC-170-1 passes with stub      | [ ]   | RG-2  |
| AP-2  | Real PII in test data             | Real name/email in factory     | [ ]   | DoD   |
| AP-3  | Test verifies hard delete         | `verify(repo).delete()` call   | [ ]   | CG-2  |
| AP-4  | Missing auth test                 | No TC-170-9 in test file       | [ ]   | CG-1  |
