# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-109: Manage Community Topics

| Field              | Value                                                       |
| ------------------ | ----------------------------------------------------------- |
| **Document ID**    | `CB-COMMUNITY-TDD-005`                                      |
| **Version**        | `1.0`                                                       |
| **Date**           | `2026-06-24`                                                |
| **Status**         | `Approved`                                                  |
| **Spec gốc**       | `CB-COMMUNITY-IMP-005` (UC109_ManageCommunityTopics_TDS.md) |
| **Author**         | `AI Agent — Amelia (Dev Agent)`                             |
| **Reviewed by**    | `HuyND`                                                     |
| **DPO Sign-off**   | `[x] Approved`                                              |
| **Approved by**    | `[x] Approved`                                              |
| **Classification** | `Internal`                                                  |

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                                    |
| ---------- | ----------------- | ---------------------------------------------------- |
| 2026-06-24 | AI Agent — Amelia | Hoàn thành kiểm thử TDD (Green Phase)                |
| 2026-06-23 | AI Agent — Amelia | Khởi tạo TDD spec cho UC-109 Manage Community Topics |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                                      |
| ------------------------- | ------------------------------------------------------------------------------------------ |
| **Feature / UC ID**       | `UC-109`                                                                                   |
| **Module**                | `community — CommunityTopicService`                                                        |
| **Spec gốc**              | `CB-COMMUNITY-IMP-005`                                                                     |
| **Priority**              | 🔴 P0 — Prerequisite cho UC-54, UC-162, UC-198                                              |
| **Sprint**                | `S1 (2026-06-23 → 2026-07-07)`                                                             |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                                    |
| **Data Classification**   | `Internal`                                                                                 |
| **Compliance Scope**      | `BR-RBAC (MODERATOR only for write)`                                                       |
| **Upstream Dependencies** | `security (JWT)`, `identity (User — MODERATOR role)`                                       |
| **Downstream Consumers**  | `community (UC-54 CreateQuestion)`, `community (UC-162 Search)`, `community (UC-198 Feed)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                                         |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                                                         |
| **Constraint Source**    | `CB-COMMUNITY-IMP-005 §17`, `ADR-COM-011`, `ADR-COM-012`                                                                                      |
| **Constraints Injected** | C1: name unique case-insensitive, C2: soft delete only (isHidden), C3: MODERATOR role for writes, C4: GET returns all topics including hidden |
| **Model**                | `claude-sonnet-4-6`                                                                                                                           |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                                  |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                  | Thực tế (schema / policy)                                                 | Fix áp dụng trong test                        |
| --- | --------------------------------------- | ------------------------------------------------------------------------- | --------------------------------------------- |
| L1  | "Ẩn topic" có thể được hiểu là delete   | Chỉ soft delete: `isHidden=true`, record vẫn tồn tại                      | Test: sau khi hide, topic vẫn trong DB        |
| L2  | GET topics không rõ filter              | Moderator thấy tất cả (kể cả hidden), User thường chỉ thấy isHidden=false | Test 2 endpoint hoặc dùng role context        |
| L3  | name uniqueness không rõ case-sensitive | case-insensitive unique check                                             | Test "Dinh Dưỡng" và "dinh dưỡng" là conflict |
| L4  | sortOrder không rõ auto-assign          | Nếu không truyền, sortOrder = max+1                                       | Test sortOrder tự tăng                        |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-109 CommunityTopicService bao gồm các layer:
├── Domain (CommunityTopic entity — isHidden invariant, name uniqueness)
├── Application/Service (CommunityTopicServiceImpl)
├── Controller (CommunityTopicController — MODERATOR guard for writes)
└── Integration (CommunityTopicRepository)
```

### TDS-02 — Test Basis

| Source                             | Items Derived                                |
| ---------------------------------- | -------------------------------------------- |
| `SRS 3.2.2.11` UC-109              | Moderator tạo, sửa, ẩn topic                 |
| `CB-COMMUNITY-IMP-005 ADR-COM-011` | ROLE_MODERATOR required for write operations |
| `BR-COM-015`                       | name unique case-insensitive                 |
| `BR-COM-016`                       | ẩn topic = soft delete (isHidden=true)       |
| `BR-COM-017`                       | name: @NotBlank, max 100 chars               |
| `BR-COM-018`                       | GET returns all topics for moderator         |

### TDS-03 — Test Conditions

| Condition ID | Test Condition                                         | Coverage Item                             | Test Cases       |
| ------------ | ------------------------------------------------------ | ----------------------------------------- | ---------------- |
| TC-COND-001  | Tạo topic hợp lệ — happy path                          | `CommunityTopicServiceImpl.createTopic()` | `COM-TC-001`     |
| TC-COND-002  | name trùng (case-insensitive) bị từ chối               | `existsByNameIgnoreCase()`                | `COM-TC-002`     |
| TC-COND-003  | name rỗng bị từ chối                                   | `@NotBlank`                               | `COM-TC-003`     |
| TC-COND-004  | name > 100 ký tự bị từ chối                            | `@Size(max=100)`                          | `COM-TC-004`     |
| TC-COND-005  | Ẩn topic (isHidden=true) — soft delete                 | `CommunityTopicServiceImpl.hideTopic()`   | `COM-TC-005`     |
| TC-COND-006  | Ẩn topic không xóa questions                           | DB assertion                              | `COM-TC-INT-002` |
| TC-COND-007  | MOTHER role bị từ chối POST 403                        | `@PreAuthorize`                           | `COM-TC-006`     |
| TC-COND-008  | MOTHER role bị từ chối PATCH 403                       | `@PreAuthorize`                           | `COM-TC-007`     |
| TC-COND-009  | GET /topics — authenticated user thấy active topics    | `findAllByIsHiddenFalse()`                | `COM-TC-008`     |
| TC-COND-010  | GET /admin/topics — MODERATOR thấy tất cả kể cả hidden | `findAllOrderBySortOrder()`               | `COM-TC-009`     |
| TC-COND-011  | Cập nhật topic name thành công                         | `CommunityTopicServiceImpl.updateTopic()` | `COM-TC-010`     |

### TDS-04 — Test Techniques

| Technique                | Applied To                                      | Rationale           |
| ------------------------ | ----------------------------------------------- | ------------------- |
| Equivalence Partitioning | name (valid, empty, >100 chars, duplicate)      | 4 partitions        |
| State Transition Testing | isHidden (false → true — irreversible via hide) | Soft delete state   |
| Error Guessing           | name với ký tự đặc biệt, HTML injection         | OWASP A03           |
| Boundary Value Analysis  | name length: 1, 100, 101 chars                  | Max length boundary |

### TDS-05 — Test Data

| Fixture ID   | Type    | Value                                                                          | Mục đích                     |
| ------------ | ------- | ------------------------------------------------------------------------------ | ---------------------------- |
| `FX-COM-001` | JWT     | `{ sub: "mod-001", role: "MODERATOR" }`                                        | Happy path auth              |
| `FX-COM-002` | JWT     | `{ sub: "user-002", role: "MOTHER" }`                                          | Unauthorized write           |
| `FX-COM-003` | JWT     | `{ sub: "user-003", role: "MOTHER" }`                                          | Authorized GET               |
| `FX-COM-004` | DB seed | `CommunityTopic { id: "topic-existing", name: "Dinh dưỡng", isHidden: false }` | Duplicate name test          |
| `FX-COM-005` | Input   | `{ name: "Thai kỳ", description: "Chủ đề thai kỳ", icon: "🤰" }`                | Valid create                 |
| `FX-COM-006` | Input   | `{ name: "dinh dưỡng" }`                                                       | Duplicate (case-insensitive) |
| `FX-COM-007` | Input   | `{ name: "" }`                                                                 | Empty name                   |
| `FX-COM-008` | Input   | `{ name: "A".repeat(101) }`                                                    | Exceeds max length           |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
private static final String BASE_NAME = "Thai kỳ khỏe mạnh";
private static final String BASE_DESC = "Chủ đề về sức khỏe thai kỳ";

private CreateCommunityTopicRequest makeCreateRequest(Consumer<CreateCommunityTopicRequest.Builder> override) {
    var builder = CreateCommunityTopicRequest.builder()
        .name(BASE_NAME)
        .description(BASE_DESC)
        .icon("🤰");
    override.accept(builder);
    return builder.build();
}
```

---

### COM-TC-001 — Tạo topic hợp lệ — Happy Path

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.createTopic(CreateCommunityTopicRequest, String actorId)`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS 3.2.2.11`, `BR-COM-017`

```gherkin
Feature: Manage Community Topics
  Background:
    Given test data classification: SYNTHETIC
    And no topic named "Thai kỳ khỏe mạnh" exists in database

  Scenario: Moderator creates a valid topic
    Given I am authenticated as MODERATOR (FX-COM-001)
    When I call createTopic with name="Thai kỳ khỏe mạnh", description="...", icon="🤰"
    Then a CommunityTopic is persisted with isHidden=false
    And response contains id, name="Thai kỳ khỏe mạnh", isHidden=false
    And topicRepository.save() was called exactly once
```

**Current Status:** 🟢 Passing

---

### COM-TC-002 — name trùng (case-insensitive) bị từ chối 409

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.createTopic()` — uniqueness check
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-COM-015`

```gherkin
  Scenario: Duplicate topic name (case-insensitive) is rejected
    Given topic "Dinh dưỡng" already exists (FX-COM-004)
    And topicRepository.existsByNameIgnoreCase("dinh dưỡng") = true
    When createTopic with name = "dinh dưỡng" (FX-COM-006)
    Then CareBridgeException thrown with error code "COM-002"
    And topicRepository.save() was NOT called
```

**Current Status:** 🟢 Passing

---

### COM-TC-003 — name rỗng bị từ chối 400

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateCommunityTopicRequest` validation (`@NotBlank`)
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`

```gherkin
  Scenario: Empty topic name is rejected
    Given I am authenticated as MODERATOR
    When POST /api/v1/community/topics with name = "" (FX-COM-007)
    Then response status is 400 Bad Request
    And error code is "COM-001"
    And details contains { field: "name", message: "name is required" }
```

**Current Status:** 🟢 Passing

---

### COM-TC-004 — name > 100 ký tự bị từ chối 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Size(max=100)` validation
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`

```gherkin
  Scenario: Topic name exceeding 100 chars is rejected
    Given I am authenticated as MODERATOR
    When POST /api/v1/community/topics with name = "A" * 101 (FX-COM-008)
    Then response status is 400 Bad Request
    And error code is "COM-001"
```

**Current Status:** 🟢 Passing

---

### COM-TC-005 — Ẩn topic (soft delete)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.hideTopic(String topicId, String actorId)`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-COM-016 — soft delete invariant`

```gherkin
  Scenario: Moderator hides a topic (soft delete)
    Given topic "topic-existing" exists with isHidden=false (FX-COM-004)
    And topicRepository.findById("topic-existing") = present
    When hideTopic("topic-existing", "mod-001")
    Then topic entity's isHidden = true
    And topicRepository.save(entity) was called with isHidden=true
    And topic record still exists in database (no DELETE)
```

**Current Status:** 🟢 Passing

---

### COM-TC-006 — MOTHER role bị từ chối POST 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285`
**Feature Under Test:** `POST /api/v1/community/topics` — RBAC guard
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

```gherkin
  Scenario: MOTHER role cannot create topics
    Given I am authenticated with role MOTHER (FX-COM-002)
    When POST /api/v1/community/topics with valid request body
    Then response status is 403 Forbidden
    And error code is "COM-004"
    And no CommunityTopic is persisted
```

**Current Status:** 🟢 Passing

---

### COM-TC-007 — MOTHER role bị từ chối PATCH 403

**Severity:** `CRITICAL`
**Feature Under Test:** `PATCH /api/v1/community/topics/{id}` — RBAC guard
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`

```gherkin
  Scenario: MOTHER role cannot update topics
    Given I am authenticated with role MOTHER
    When PATCH /api/v1/community/topics/topic-existing with { isHidden: true }
    Then response status is 403 Forbidden
    And error code is "COM-004"
```

**Current Status:** 🟢 Passing

---

### COM-TC-008 — GET topics — User thấy active topics

**Severity:** `MEDIUM`
**Feature Under Test:** `GET /api/v1/community/topics` — filtered by isHidden=false
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

```gherkin
  Scenario: Authenticated user sees only non-hidden topics
    Given topics: [{id: "t1", isHidden: false}, {id: "t2", isHidden: true}]
    And I am authenticated as MOTHER (FX-COM-003)
    When GET /api/v1/community/topics
    Then response status is 200 OK
    And response contains only topic "t1"
    And response does NOT contain topic "t2" (hidden)
```

**Current Status:** 🟢 Passing

---

### COM-TC-009 — GET all topics — MODERATOR thấy tất cả

**Severity:** `MEDIUM`
**Feature Under Test:** `GET /api/v1/admin/community/topics` — returns all including hidden
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`

```gherkin
  Scenario: MODERATOR sees all topics including hidden ones
    Given topics: [{id: "t1", isHidden: false}, {id: "t2", isHidden: true}]
    And I am authenticated as MODERATOR (FX-COM-001)
    When GET /api/v1/admin/community/topics
    Then response contains both "t1" and "t2"
    And response indicates isHidden=true for "t2"
```

**Current Status:** 🟢 Passing

---

### COM-TC-010 — Cập nhật topic thành công

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityTopicServiceImpl.updateTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`

```gherkin
  Scenario: Moderator updates topic name and description
    Given topic "topic-existing" exists
    When PATCH /api/v1/community/topics/topic-existing with { name: "Sức khỏe thai kỳ", description: "Mô tả mới" }
    Then response status is 200 OK
    And response name = "Sức khỏe thai kỳ"
    And DB record updated (no new record created)
```

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

### COM-TC-INT-001 — Full topic create flow

**Severity:** `HIGH`
**Feature Under Test:** `Full: Controller → Service → Repository`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicIntegrationTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:**
- PostgreSQL Testcontainer running
- `community_topics` table clean (no seed)
- JWT `FX-COM-001`

**Test Steps:**
1. POST `/api/v1/community/topics` với `FX-COM-005`, Authorization = MODERATOR JWT
2. Assert HTTP 201 Created
3. Query DB: `SELECT * FROM community_topics WHERE name = 'Thai kỳ khỏe mạnh'`
4. Assert: `is_hidden = false`, `created_by = 'mod-001'`

**DB Assertion:**
```sql
SELECT id, name, is_hidden, created_by FROM community_topics WHERE name = 'Thai kỳ khỏe mạnh';
-- Expected: is_hidden = false, created_by = 'mod-001'
```

**Current Status:** 🟢 Passing

---

### COM-TC-INT-002 — Ẩn topic không xóa questions

**Severity:** `HIGH`
**Feature Under Test:** `Soft delete preserves questions`
**Test File:** `CommunityTopicIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `BR-COM-016`

**Preconditions:**
- PostgreSQL Testcontainer
- Topic "topic-001" seeded với 3 questions

**Test Steps:**
1. PATCH `/api/v1/community/topics/topic-001` với `{ isHidden: true }`
2. Assert HTTP 200 OK
3. Query `community_topics`: `is_hidden = true`
4. Query `community_questions WHERE topic_id = 'topic-001'`: count = 3 (unchanged)

**DB Assertion:**
```sql
SELECT is_hidden FROM community_topics WHERE id = 'topic-001';
-- Expected: is_hidden = true

SELECT COUNT(*) FROM community_questions WHERE topic_id = 'topic-001';
-- Expected: 3 (questions still exist)
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                            | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ---------------- | ------------------------------------ | --------------- | ---------------- | --------------- |
| `COM-TC-001`     | `CommunityTopicServiceImplTest.java` | `[x]`           | Passed           | —               |
| `COM-TC-002`     | `CommunityTopicServiceImplTest.java` | `[x]`           | Passed           | —               |
| `COM-TC-003`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-004`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-005`     | `CommunityTopicServiceImplTest.java` | `[x]`           | Passed           | Soft delete     |
| `COM-TC-006`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-007`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-008`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-009`     | `CommunityTopicControllerTest.java`  | `[x]`           | Passed           | —               |
| `COM-TC-010`     | `CommunityTopicServiceImplTest.java` | `[x]`           | Passed           | —               |
| `COM-TC-INT-001` | `CommunityTopicIntegrationTest.java` | `[x]`           | Passed           | —               |
| `COM-TC-INT-002` | `CommunityTopicIntegrationTest.java` | `[x]`           | Passed           | —               |

### 5.1 Red Gate Protocol (CASE 2.0)

```java
// CommunityTopicServiceImpl.java — Red Phase Stub
@Service
public class CommunityTopicServiceImpl implements CommunityTopicService {
    @Override
    public CommunityTopicResponse createTopic(CreateCommunityTopicRequest request, String actorId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public CommunityTopicResponse updateTopic(String id, UpdateCommunityTopicRequest request, String actorId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public void hideTopic(String id, String actorId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-COMMUNITY-IMP-005` đã được review
- [x] Logic Issues (Section 2) đã được confirm
- [x] DB migration cho `community_topics` table approved
- [x] UC-109 được implement TRƯỚC UC-54, UC-162, UC-198

### Exit Criteria (DoD)

- [x] Tất cả unit tests trong `CommunityTopicServiceImplTest.java` PASS
- [x] Tất cả integration tests PASS
- [x] Test coverage ≥ 80%
- [x] `POST /api/v1/community/topics` — 201 với topic mới, 403 với MOTHER role
- [x] `PATCH /api/v1/community/topics/{id}` — 200 với MODERATOR, 403 với MOTHER
- [x] Soft delete: `isHidden=true`, record vẫn tồn tại trong DB
- [x] Duplicate name (case-insensitive) → 409 với `COM-002`

**Exit Criteria CASE 2.0:**
- [x] Red Gate §5.1 — tất cả TC FAIL với stub
- [x] `CommunityTopicService`, `CommunityTopicRepository`, `CommunityTopicResponse` đã được khai báo
- [x] Props Isolation — factory pattern được dùng trong mọi test

---

## 7. Rollback Plan

```bash
# Revert DB migration
./mvnw flyway:undo

# Revert source files
git checkout -- src/main/java/com/carebridge/backend/community/

# Downstream UC-54, UC-162, UC-198 sẽ fail compile — rollback chúng trước
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu                                    | Check | Gate  |
| --------- | ------------------------ | ------------------------------------------- | ----- | ----- |
| AP-AI-001 | Unconstrained Generation | TC không reference BR-COM-015/016           | [x]   | G-0   |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty stub                    | [x]   | G-2 ★ |
| AP-AI-003 | Implicit Decision        | Soft delete không có ADR                    | [x]   | G-1   |
| AP-AI-004 | Layer Violation          | Controller test kiểm tra uniqueness logic   | [x]   | G-4   |
| AP-AI-005 | Hallucinated Contract    | Import `CommunityTopicService` chưa tồn tại | [x]   | G-3   |

---

*TDD Spec v1.0 — UC-109 Manage Community Topics — CareBridge CB-COMMUNITY-TDD-005*
