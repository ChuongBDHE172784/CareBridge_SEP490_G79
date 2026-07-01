# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-171 Follow Topic

**Document ID:** `CB-COMMUNITY-TEST-007`
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
- `04_Implement/UC171_FollowTopic/UC171_FollowTopic_TDS.md` — TDS
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.8.4` — UC-171 spec

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                           |
| ---------- | ------------------------------------- | ------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Khởi tạo TDD spec cho UC-171 Follow Topic  |

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
| **Feature / UC ID**     | `UC-171`                                               |
| **Module**              | `community — FollowTopic`                             |
| **Spec gốc**            | `CB-COMMUNITY-IMP-007`                                |
| **Priority**            | 🟡 P2                                                  |
| **Milestone**           | `M3 Alpha — 2026-07-11`                               |
| **Data Classification** | `Internal`                                            |
| **Compliance Scope**    | `BR-RBAC`                                             |
| **Upstream Dependencies** | `security (JWT), community (CommunityTopic)`        |
| **Downstream Consumers** | `notification (future FCM), feed personalization`    |

### 1.1 AI Generation Context (CASE 2.0)

| Field               | Value                                              |
| ------------------- | -------------------------------------------------- |
| **AI Assisted?**    | `Yes`                                              |
| **Constraint Source** | `UC171_TDS §17`, `ADR-COM-171-1`, `ADR-COM-171-2` |
| **Constraints Injected** | Toggle semantics; hidden topic = 409; no FCM in scope; UNIQUE constraint |
| **Model**           | `claude-sonnet-4-6`                                |
| **Trust Level**     | `T2 → T3 (pending Red Gate)`                      |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | `user_topic_follows` table không tồn tại trong V1 | Cần migration V20260701000001 trước khi test | Integration test phải verify table tồn tại |
| L2 | UC đề cập FCM secondary actor | FCM is explicitly out of scope (ADR-COM-171-1) | Tests không verify FCM calls — chỉ verify DB state |
| L3 | Toggle semantics: 1st call=follow, 2nd=unfollow | Must be truly idempotent by pair (1+1=0) | Add TC-171-6 double-call test |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
FollowTopic bao gồm:
├── Service layer (TopicFollowServiceImpl.toggleFollow)
│   ├── findTopic → check isHidden → findExistingFollow → toggle
│   └── mock repositories với Mockito
├── Controller layer (CommunityTopicController.toggleFollow)
│   └── @WebMvcTest + MockMvc → verify HTTP 200/401/404/409
└── Integration (Testcontainers PostgreSQL + @SpringBootTest)
    ├── Migration V20260701000001 applied (user_topic_follows table)
    └── DB state: follow record inserted / deleted correctly
```

### TDS-02 — Test Basis

- UC-171 (SRS §3.3.8.4): "Follows or unfollows topics to personalize the feed"
- ADR-COM-171-1: Toggle POST endpoint
- ADR-COM-171-2: Idempotent upsert with UNIQUE constraint
- BR-COM-171-2: Cannot follow hidden topic → 409
- V20260701000001: `user_topic_follows` table migration

### TDS-03 — Test Conditions

| TC-ID    | Condition                               | Technique          | Layer        |
| -------- | --------------------------------------- | ------------------ | ------------ |
| TC-171-1 | Follow visible topic (no existing row)  | Happy path         | Service + IT |
| TC-171-2 | Unfollow (existing row → delete)        | Toggle path        | Service + IT |
| TC-171-3 | Follow non-existent topic               | Error path         | Service + CT |
| TC-171-4 | Follow hidden topic                     | Authorization test | Service + CT |
| TC-171-5 | Unauthenticated request                 | Security test      | Controller   |
| TC-171-6 | Double follow call (idempotency pair)   | Idempotency        | Service      |
| TC-171-7 | Two users follow same topic             | Concurrency        | IT           |

### TDS-04 — Test Data Requirements

- All data from `FollowTopicTestFactory` (Props Isolation)
- Testcontainers PostgreSQL for integration tests (verifies UNIQUE constraint)
- No real PII

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
package com.carebridge.backend.community;

import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.UserTopicFollow;
import java.util.UUID;

public class FollowTopicTestFactory {

    public static final UUID USER_ID     = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final UUID USER_2_ID   = UUID.fromString("22222222-0000-0000-0000-000000000002");
    public static final UUID TOPIC_ID    = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID HIDDEN_TOPIC_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    public static CommunityTopic makeVisibleTopic() {
        return CommunityTopic.builder()
            .id(TOPIC_ID)
            .name("Dinh dưỡng thai kỳ")
            .isHidden(false)
            .sortOrder(1)
            .build();
    }

    public static CommunityTopic makeHiddenTopic() {
        return CommunityTopic.builder()
            .id(HIDDEN_TOPIC_ID)
            .name("Hidden Topic")
            .isHidden(true)
            .sortOrder(99)
            .build();
    }

    public static UserTopicFollow makeExistingFollow() {
        return UserTopicFollow.builder()
            .id(UUID.randomUUID())
            .userId(USER_ID)
            .topicId(TOPIC_ID)
            .build();
    }
}
```

---

### TC-171-1 — Follow visible topic (first time)

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-171-2 — toggle follow when no existing record
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityTopic topic = FollowTopicTestFactory.makeVisibleTopic();
when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.empty());
when(followRepository.save(any())).thenReturn(FollowTopicTestFactory.makeExistingFollow());

// Act
TopicFollowResponse response = followService.toggleFollow(TOPIC_ID, USER_ID);

// Assert
assertThat(response.isFollowed()).isTrue();
assertThat(response.getTopicId()).isEqualTo(TOPIC_ID);
verify(followRepository).save(any(UserTopicFollow.class));
verify(followRepository, never()).delete(any());
```

**Intended test file:** `TopicFollowServiceImplTest.java`
**Failure signature:** `UnsupportedOperationException: Not implemented`

---

### TC-171-2 — Unfollow (existing row → delete)

**Severity:** 🔴 Critical
**Oracle source:** ADR-COM-171-2 — toggle unfollow when existing record
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityTopic topic = FollowTopicTestFactory.makeVisibleTopic();
UserTopicFollow existing = FollowTopicTestFactory.makeExistingFollow();
when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.of(existing));

// Act
TopicFollowResponse response = followService.toggleFollow(TOPIC_ID, USER_ID);

// Assert
assertThat(response.isFollowed()).isFalse();
verify(followRepository).delete(existing);
verify(followRepository, never()).save(any());
```

**Intended test file:** `TopicFollowServiceImplTest.java`

---

### TC-171-4 — Follow hidden topic returns 409

**Severity:** 🟠 High
**Oracle source:** BR-COM-171-2 — cannot follow hidden topic
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
CommunityTopic hiddenTopic = FollowTopicTestFactory.makeHiddenTopic();
when(topicRepository.findById(HIDDEN_TOPIC_ID)).thenReturn(Optional.of(hiddenTopic));

// Act + Assert
assertThatThrownBy(() -> followService.toggleFollow(HIDDEN_TOPIC_ID, USER_ID))
    .isInstanceOf(TopicHiddenException.class);
verify(followRepository, never()).save(any());
verify(followRepository, never()).delete(any());
```

**Expected HTTP:** `409 Conflict {"code":"TOPIC_HIDDEN"}`
**Intended test file:** `TopicFollowServiceImplTest.java` + `CommunityTopicControllerTest.java`

---

### TC-171-6 — Double follow call (idempotency pair)

**Severity:** 🟡 Medium
**Oracle source:** ADR-COM-171-2 — toggle: 1st=follow, 2nd=unfollow
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Description:** First call sets `followed=true`, second call sets `followed=false`. System does not error on double-call.

**Intended test file:** `TopicFollowServiceImplTest.java` (two sequential calls with state tracking)

---

## 5. Red-Green-Refactor Tracker

| TC-ID    | Test File                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| -------- | --------------------------------------- | ---------------- | ----------------- | ---------------- |
| TC-171-1 | `TopicFollowServiceImplTest.java`       | [ ]              | —                 | —                |
| TC-171-2 | `TopicFollowServiceImplTest.java`       | [ ]              | —                 | —                |
| TC-171-3 | `CommunityTopicControllerTest.java`     | [ ]              | —                 | —                |
| TC-171-4 | `TopicFollowServiceImplTest.java`       | [ ]              | —                 | —                |
| TC-171-5 | `CommunityTopicControllerTest.java`     | [ ]              | —                 | —                |
| TC-171-6 | `TopicFollowServiceImplTest.java`       | [ ]              | —                 | —                |
| TC-171-7 | `TopicFollowIntegrationTest.java`       | [ ]              | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0) — Gate 2

**Red Gate Stub:**
```java
// TopicFollowServiceImpl.toggleFollow() Red Phase stub
public TopicFollowResponse toggleFollow(UUID topicId, UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC-ID    | Expected  | Actual           |
| -------- | --------- | ---------------- |
| TC-171-1 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-171-2 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |
| TC-171-4 | 🔴 FAIL   | ☐ FAIL ☐ PASS   |

Tất cả FAIL? [ ] Yes [ ] No

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-COMMUNITY-IMP-007` approved
- [ ] Migration `V20260701000001__create_topic_follows.sql` exists
- [ ] `UserTopicFollow` entity class created
- [ ] Red Gate stubs in place

### Exit Criteria (DoD)
- [ ] All 7 TCs pass (`./mvnw test -Dtest=TopicFollowServiceImplTest,CommunityTopicControllerTest,TopicFollowIntegrationTest`)
- [ ] TC-171-1, TC-171-2 (toggle), TC-171-4 (hidden), TC-171-5 (unauth) must pass
- [ ] Red Gate confirmed
- [ ] Props Isolation factory used for all test data
- [ ] No FCM calls verified in any test (out of scope)

---

## 7. Rollback Plan

```bash
git revert <commit-hash-of-followTopic-implementation>
# DB: DROP TABLE IF EXISTS user_topic_follows; (only if no data)
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern                          | Signal                         | Check | Gate  |
| ----- | ------------------------------------- | ------------------------------ | ----- | ----- |
| AP-1  | Test passes against no-op stub        | TC-171-1 passes with stub      | [ ]   | RG-2  |
| AP-2  | Non-idempotent: error on double-call  | TC-171-6 throws exception      | [ ]   | CG-2  |
| AP-3  | FCM call in test setup                | FCM mock verified in test      | [ ]   | C5    |
| AP-4  | Missing hidden topic guard test       | No TC-171-4 in test file       | [ ]   | CG-1  |
