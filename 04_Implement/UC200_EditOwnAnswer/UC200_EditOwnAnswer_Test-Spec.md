# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-200 Edit Own Answer

**Document ID:** `CB-COMMUNITY-TEST-008`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[x] HuyND — Approved`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `HuyND`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — schema oracle
- `04_Implement/UC200_EditOwnAnswer/UC200_EditOwnAnswer_TDS.md` — TDS
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.13.3` — UC-200 spec

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                            |
| ---------- | ------------------------------------- | -------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Khởi tạo TDD spec cho UC-200 Edit Own Answer |
| 2026-07-01 | AI Agent — Amelia (Dev Agent) | Implemented and ran all test cases against real code (added directly to `CommunityAnswerServiceImplTest.java` / `CommunityAnswerControllerTest.java` rather than new dedicated files). Tests written alongside implementation (not strict stub-first Red Gate — see §5.1). All pass via `./mvnw test`. Status=Approved. |

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

| Field                   | Value                                                  |
| ----------------------- | ------------------------------------------------------ |
| **Feature / UC ID**     | `UC-200`                                               |
| **Module**              | `community — EditOwnAnswer`                           |
| **Spec gốc**            | `CB-COMMUNITY-IMP-008`                                |
| **Priority**            | 🟡 P2                                                  |
| **Milestone**           | `M3 Alpha — 2026-07-11`                               |
| **Data Classification** | `Internal`                                            |
| **Compliance Scope**    | `BR-RBAC`                                             |
| **Upstream Dependencies** | `security (JWT), community (CommunityAnswer)`       |
| **Downstream Consumers** | `community question detail, moderation queue`        |

### 1.1 AI Generation Context (CASE 2.0)

| Field               | Value                                               |
| ------------------- | --------------------------------------------------- |
| **AI Assisted?**    | `Yes`                                               |
| **Constraint Source** | `UC200_TDS §17`, `ADR-COM-200-1`, `ADR-COM-200-2` |
| **Constraints Injected** | Status resets to PENDING; expertLabeled immutable; HIDDEN blocks edit; owner-only |
| **Model**           | `claude-sonnet-4-6`                                 |
| **Trust Level**     | `T2 → T3 (pending Red Gate)`                       |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC chỉ nói "edit when not locked" — không đề cập status DELETED | DELETED status sẽ tồn tại sau V20260701000002 | Test phải verify DELETED answer cũng không thể edit (tương tự HIDDEN) |
| L2 | UC không nêu rõ trạng thái sau edit | ADR-COM-200-2: phải reset về PENDING | Test TC-200-1 phải assert `status == PENDING` sau edit APPROVED answer |
| L3 | UC không đề cập `expertLabeled` | ADR-COM-200-1: immutable field | Test TC-200-9 phải verify expertLabeled không thay đổi |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
EditOwnAnswer bao gồm:
├── Service layer (CommunityAnswerServiceImpl.editAnswer)
│   ├── findById → ownership check → status guard → update → reset PENDING → save
│   └── mock CommunityAnswerRepository với Mockito
├── Controller layer (CommunityAnswerController.editAnswer)
│   └── @WebMvcTest + MockMvc → verify HTTP 200/400/403/404/409
└── Integration (Testcontainers + @SpringBootTest)
    └── DB state: body updated, status=PENDING persisted
```

### TDS-02 — Test Basis

- UC-200 (SRS §3.3.13.3): "Edits the user's own answer when it is not locked"
- ADR-COM-200-1: Owner-only, no MODERATOR bypass
- ADR-COM-200-2: Status reset to PENDING after edit
- BR-COM-200-3: Only `body` and `isPersonalExperience` editable
- BR-COM-200-4: `expertLabeled` is immutable

### TDS-03 — Test Conditions

| TC-ID    | Condition                                      | Technique          | Layer        |
| -------- | ---------------------------------------------- | ------------------ | ------------ |
| TC-200-1 | Author edits own APPROVED answer               | Happy path         | Service + IT |
| TC-200-2 | Author edits own PENDING answer                | Alternative path   | Service      |
| TC-200-3 | Attempt edit HIDDEN answer                     | Error path         | Service      |
| TC-200-4 | Attempt edit DELETED answer                    | Error path         | Service      |
| TC-200-5 | Non-owner attempts edit                        | Authorization test | Service + CT |
| TC-200-6 | Edit with blank body                           | Validation test    | Controller   |
| TC-200-7 | Edit with body > 2000 chars                    | Boundary test      | Controller   |
| TC-200-8 | Edit non-existent answer ID                    | Error path         | CT           |
| TC-200-9 | Attempt to change expertLabeled via edit       | Security/invariant | Service      |
| TC-200-10 | Unauthenticated request                       | Security test      | Controller   |

### TDS-04 — Test Data Requirements

- All data from `EditOwnAnswerTestFactory` (Props Isolation)
- Testcontainers for integration tests
- No PII in test data

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
package com.carebridge.backend.community;

import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import java.util.UUID;

public class EditOwnAnswerTestFactory {

    public static final UUID AUTHOR_ID     = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final UUID NON_OWNER_ID  = UUID.fromString("22222222-0000-0000-0000-000000000002");
    public static final UUID ANSWER_ID     = UUID.fromString("55555555-0000-0000-0000-000000000001");
    public static final UUID QUESTION_ID   = UUID.fromString("44444444-0000-0000-0000-000000000001");

    public static CommunityAnswer makeApprovedAnswer() {
        return CommunityAnswer.builder()
            .id(ANSWER_ID)
            .questionId(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .body("Original answer body")
            .personalExperience(false)
            .expertLabeled(false)
            .status(AnswerStatus.APPROVED)
            .build();
    }

    public static CommunityAnswer makeApprovedExpertLabeledAnswer() {
        return makeApprovedAnswer().toBuilder().expertLabeled(true).build();
    }

    public static CommunityAnswer makeHiddenAnswer() {
        return makeApprovedAnswer().toBuilder().status(AnswerStatus.HIDDEN).build();
    }

    public static CommunityAnswer makeDeletedAnswer() {
        return makeApprovedAnswer().toBuilder().status(AnswerStatus.DELETED).build();
    }

    public static EditAnswerRequest makeValidRequest() {
        EditAnswerRequest req = new EditAnswerRequest();
        req.setBody("Updated answer body — new content");
        req.setPersonalExperience(true);
        return req;
    }

    public static EditAnswerRequest makeBlankBodyRequest() {
        EditAnswerRequest req = new EditAnswerRequest();
        req.setBody("");
        req.setPersonalExperience(false);
        return req;
    }
}
```

---

### TC-200-1 — Author edits own APPROVED answer → status resets to PENDING

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-200-2 — status reset; UC-200 happy path
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange / Act / Assert:**
```java
// Arrange
CommunityAnswer answer = EditOwnAnswerTestFactory.makeApprovedAnswer();
EditAnswerRequest request = EditOwnAnswerTestFactory.makeValidRequest();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(answer));
when(answerRepository.save(any())).thenReturn(answer);

// Act
CommunityAnswerResponse response = answerService.editAnswer(ANSWER_ID, AUTHOR_ID, request);

// Assert
ArgumentCaptor<CommunityAnswer> captor = ArgumentCaptor.forClass(CommunityAnswer.class);
verify(answerRepository).save(captor.capture());
CommunityAnswer saved = captor.getValue();
assertThat(saved.getStatus()).isEqualTo(AnswerStatus.PENDING);
assertThat(saved.getBody()).isEqualTo("Updated answer body — new content");
assertThat(saved.isPersonalExperience()).isTrue();
```

**Expected persistence:** `status='PENDING'`, `body='Updated...'`, `is_personal_experience=true`
**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

### TC-200-3 — Attempt edit HIDDEN answer returns 409

**Severity:** 🟠 High
**Oracle source:** BR-COM-200-1 — HIDDEN answers not editable
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange / Act / Assert:**
```java
// Arrange
CommunityAnswer hidden = EditOwnAnswerTestFactory.makeHiddenAnswer();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(hidden));

// Act + Assert
assertThatThrownBy(() -> answerService.editAnswer(ANSWER_ID, AUTHOR_ID, makeValidRequest()))
    .isInstanceOf(AnswerNotEditableException.class);
verify(answerRepository, never()).save(any());
```

**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

### TC-200-9 — expertLabeled field must not change after edit

**Severity:** 🔴 Critical
**Oracle source:** BR-COM-200-4 — expertLabeled is immutable via author edit
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange / Act / Assert:**
```java
// Arrange — answer with expertLabeled=true
CommunityAnswer expertAnswer = EditOwnAnswerTestFactory.makeApprovedExpertLabeledAnswer();
EditAnswerRequest request = EditOwnAnswerTestFactory.makeValidRequest();
when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(expertAnswer));
when(answerRepository.save(any())).thenReturn(expertAnswer);

// Act
answerService.editAnswer(ANSWER_ID, AUTHOR_ID, request);

// Assert — expertLabeled must still be true
ArgumentCaptor<CommunityAnswer> captor = ArgumentCaptor.forClass(CommunityAnswer.class);
verify(answerRepository).save(captor.capture());
assertThat(captor.getValue().isExpertLabeled()).isTrue();
```

**Intended test file:** `CommunityAnswerServiceImplTest.java`

---

## 5. Red-Green-Refactor Tracker

| TC-ID     | Test File                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| --------- | --------------------------------------- | ---------------- | ----------------- | ---------------- |
| TC-200-1  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-2  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-3  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-4  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-5  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-6  | `CommunityAnswerControllerTest.java`    | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-7  | `CommunityAnswerControllerTest.java`    | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-8  | `CommunityAnswerControllerTest.java`    | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-9  | `CommunityAnswerServiceImplTest.java`   | [ ] N/A — see §5.1 note | Passed | —          |
| TC-200-10 | `CommunityAnswerControllerTest.java`    | [ ] N/A — see §5.1 note | Passed | —          |

### 5.1 Red Gate Protocol (CASE 2.0) — Gate 2

**Red Gate Stub:**
```java
public CommunityAnswerResponse editAnswer(UUID answerId, UUID callerId, EditAnswerRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC-ID    | Expected  | Actual           |
| -------- | --------- | ---------------- |
| TC-200-1 | 🔴 FAIL   | ☐ FAIL ☐ PASS — not run |
| TC-200-3 | 🔴 FAIL   | ☐ FAIL ☐ PASS — not run |
| TC-200-9 | 🔴 FAIL   | ☐ FAIL ☐ PASS — not run |

Tất cả FAIL? [ ] Yes [ ] No — **not applicable this pass.** Tests were authored alongside the implementation, not against a throw-stub first. Correctness evidenced by full suite passing (`./mvnw test`, 0 failures) and code review against `BR-COM-200-1..4`/`ADR-COM-200-1..2`. Not a fabricated Red Gate pass.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-COMMUNITY-IMP-008` approved
- [x] `EditAnswerRequest.java` DTO created
- [ ] Red Gate stubs in place — not run this pass (see §5.1)

### Exit Criteria (DoD)
- [x] All TCs pass (`./mvnw test -Dtest=CommunityAnswerServiceImplTest,CommunityAnswerControllerTest` — 0 failures)
- [x] TC-200-1 (happy path + PENDING reset), TC-200-3 (HIDDEN guard), TC-200-9 (expertLabeled immutable) pass
- [ ] Red Gate confirmed — not performed this pass
- [x] Props Isolation factory used (per-test entity builders)

---

## 7. Rollback Plan

```bash
git revert <commit-hash-of-editAnswer-implementation>
# No schema migration to rollback
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern                         | Signal                            | Check | Gate  |
| ----- | ------------------------------------ | --------------------------------- | ----- | ----- |
| AP-1  | Test passes against stub             | TC-200-1 passes with stub         | [ ] not checked — Red Gate not run this pass | RG-2  |
| AP-2  | Status not reset after edit          | TC-200-1 checks status unchanged  | [x] none found — `editAnswer_ownApprovedAnswer_resetsToPending` asserts PENDING | CG-2  |
| AP-3  | expertLabeled can be set via request | Request DTO has expertLabeled field | [x] none found — `EditAnswerRequest` has no expertLabeled field | C3    |
| AP-4  | HIDDEN answer can be edited          | TC-200-3 assertion absent         | [x] present — `editAnswer_hiddenAnswer_throwsAnswerNotEditableException` | CG-1  |
