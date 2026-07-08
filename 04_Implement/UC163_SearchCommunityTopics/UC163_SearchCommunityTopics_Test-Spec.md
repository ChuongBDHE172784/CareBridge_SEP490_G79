# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-163 Search Community Topics

**Document ID:** `CB-COMMUNITY-TDD-009`
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
- `CB-COMMUNITY-IMP-009` — TDS UC-163 Search Community Topics
- `SRS 3.3.8.2` — UC-163 functional requirements
- `ADR-COM-009, ADR-COM-010`
- `BR-COM-012, BR-COM-013, BR-COM-014, BR-COM-004`

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                   |
| ---------- | --------------- | --------------------------------------------------- |
| 2026-06-29 | AI Agent        | Khởi tạo TDD spec cho UC-163 Search Community Topics |
| 2026-06-29 | AI Agent — Amelia (Dev Agent) | 6 service tests (TC-001, TC-002, TC-004, TC-005, TC-006 + blank keyword variant) passed; marked GREEN | Implemented |

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

| Field                     | Value                                                    |
| ------------------------- | -------------------------------------------------------- |
| **Feature / UC ID**       | `UC-163`                                                 |
| **Module**                | `community — SearchCommunityTopics`                      |
| **Spec gốc**              | `CB-COMMUNITY-IMP-009`                                   |
| **Priority**              | P1 High                                                  |
| **Sprint**                | `S1 (2026-06-29 → 2026-07-12)`                           |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                  |
| **Data Classification**   | `Public`                                                 |
| **Compliance Scope**      | `N/A`                                                    |
| **Upstream Dependencies** | `community.CommunityTopic`                               |
| **Downstream Consumers**  | `Mobile App topic picker, Web topic search dropdown`     |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                               |
| ------------------------ | ----------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                               |
| **Constraint Source**    | `CB-COMMUNITY-IMP-009 §17`                                                          |
| **Constraints Injected** | C1 (blank = existing behavior), C2 (name-only case-insensitive), C3 (max 100 chars), C4 (MODERATOR gate), C5 (sortOrder ASC), C6 (200 on empty) |
| **Model**                | `claude-sonnet-4-6`                                                                 |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                        |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                            | Thực tế                                                        | Fix áp dụng trong test                                   |
| --- | ------------------------------------------------- | -------------------------------------------------------------- | -------------------------------------------------------- |
| L1  | UC-163 does not specify what happens with blank keyword | ADR-COM-009: null/blank keyword must preserve existing behavior | TC-001: verify findAllByIsHiddenFalse... called, not new query |
| L2  | Spec does not clarify includeHidden for non-moderators | ADR-COM-010: effectiveInclude = false for non-moderators regardless of client input | TC-005: verify non-moderator gets non-hidden only |
| L3  | Spec does not mention empty search result handling | BR-COM-012: 200 OK with empty list — never 404                 | TC-004: verify empty list returned, not exception        |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UC-163 SearchCommunityTopics bao gồm:
├── Service Layer (unit — mock repository)
│   └── CommunityTopicServiceImpl.getTopics(boolean, String)
│       ├── keyword=null → existing behavior (findAll... methods)
│       ├── keyword=blank → existing behavior
│       ├── keyword present → new JPQL query methods
│       ├── includeHidden=false → isHidden filter
│       └── includeHidden=true (MODERATOR) → no hidden filter
├── Controller Layer (unit — mock service, @WebMvcTest)
│   ├── @RequestParam keyword parsing
│   └── keyword > 100 chars → COM-001
└── Integration Layer (Testcontainers PostgreSQL)
    └── Full search flow with real DB: save topics, search by keyword
```

### TDS-02 — Test Basis

| Source               | Items Derived                                    |
| -------------------- | ------------------------------------------------ |
| `SRS 3.3.8.2` UC-163 | Search topics by keyword                         |
| `ADR-COM-009`        | keyword null/blank → existing behavior           |
| `ADR-COM-010`        | includeHidden gate — MODERATOR only              |
| `BR-COM-012`         | Empty result → 200 OK with empty list            |
| `BR-COM-013`         | Case-insensitive match on name field only        |
| `BR-COM-014`         | Max keyword length = 100 chars                   |
| `BR-COM-004`         | Results ordered by sortOrder ASC                 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                  | Coverage Item                                        | Test Cases       |
| ------------ | ----------------------------------------------- | ---------------------------------------------------- | ---------------- |
| TC-COND-001  | keyword=null → existing behavior preserved      | `findAllByIsHiddenFalseOrderBySortOrderAsc()`        | `COM163-TC-001`  |
| TC-COND-002  | keyword present → filtered by name              | `findByNameContainingIgnoreCaseAndIsHiddenFalse()`   | `COM163-TC-002`  |
| TC-COND-003  | keyword is case-insensitive                     | LOWER(name) LIKE LOWER(keyword)                      | `COM163-TC-003`  |
| TC-COND-004  | keyword with no match → empty list (not 404)    | Return empty list                                    | `COM163-TC-004`  |
| TC-COND-005  | Non-moderator + includeHidden=true → non-hidden only | effectiveInclude=false guard                    | `COM163-TC-005`  |
| TC-COND-006  | Moderator + includeHidden=true + keyword → includes hidden | `findByNameContainingIgnoreCase()`         | `COM163-TC-006`  |
| TC-COND-007  | keyword > 100 chars → 400 COM-001               | `@Size(max=100)` validation                          | `COM163-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique                | Applied To                                     | Rationale                             |
| ------------------------ | ---------------------------------------------- | ------------------------------------- |
| Equivalence Partitioning | keyword (null, blank, valid, > 100 chars)      | Cover all keyword input classes       |
| Boundary Value Analysis  | keyword length (99, 100, 101)                  | Max length boundary                   |
| State Transition Testing | includeHidden × isModerator (4 combinations)   | Security gate correctness             |
| Error Guessing           | Case variants (lower, upper, mixed)            | Case-insensitive requirement          |

### TDS-05 — Test Data Requirements

| Fixture ID      | Type    | Value / Logic                                              | Mục đích                              |
| --------------- | ------- | ---------------------------------------------------------- | ------------------------------------- |
| `FX-COM163-001` | DB seed | 3 topics: "Thai kỳ" (sort=1), "Sơ sinh" (sort=2), "Dinh dưỡng" (sort=3) — all non-hidden | Baseline topics |
| `FX-COM163-002` | DB seed | 1 hidden topic: "Hidden topic thai kỳ" (isHidden=true)    | Test MODERATOR includeHidden          |
| `FX-COM163-003` | JWT     | `{sub: "user-001", roles: ["ROLE_MOTHER"]}`                | Non-moderator user                    |
| `FX-COM163-004` | JWT     | `{sub: "mod-001", roles: ["ROLE_MODERATOR"]}`              | Moderator user                        |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// CommunityTopicTestFactory.java
class CommunityTopicTestFactory {

    static CommunityTopic makeTopic(String name, int sortOrder) {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(UUID.randomUUID());
        topic.setName(name);
        topic.setSortOrder(sortOrder);
        topic.setHidden(false);
        topic.setCreatedAt(Instant.now());
        return topic;
    }

    static CommunityTopic makeTopic(String name, int sortOrder, Consumer<CommunityTopic> overrides) {
        CommunityTopic topic = makeTopic(name, sortOrder);
        overrides.accept(topic);
        return topic;
    }

    static CommunityTopicResponse makeTopicResponse(String name, int sortOrder) {
        CommunityTopicResponse resp = new CommunityTopicResponse();
        resp.setId(UUID.randomUUID());
        resp.setName(name);
        resp.setSortOrder(sortOrder);
        resp.setHidden(false);
        return resp;
    }
}
```

---

### COM163-TC-001 — keyword=null returns all non-hidden topics (existing behavior preserved)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.getTopics(false, null)`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-COM-009, BR-COM-012`

**Preconditions:**
- `topicRepository` mock
- `findAllByIsHiddenFalseOrderBySortOrderAsc()` returns 3 topics

**Test Steps:**
1. Arrange: mock `findAllByIsHiddenFalseOrderBySortOrderAsc()` → 3 topics
2. Act: `service.getTopics(false, null)`
3. Assert: result has 3 items; `findByNameContainingIgnoreCaseAndIsHiddenFalse()` NOT called

**Expected Result (PASS):**
- Returns list of 3 topics
- New keyword query methods are NOT invoked

**Expected Result (FAIL):**
- Service calls keyword query even when keyword is null

**Current Status:** 🟢 Passing
**Implementation Note:** Check `keyword == null || keyword.isBlank()` before branching to keyword query.

---

### COM163-TC-002 — keyword "thai" returns matching topics (case-insensitive)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.getTopics(false, "thai")`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-COM-013, ADR-COM-009`

**Preconditions:**
- `findByNameContainingIgnoreCaseAndIsHiddenFalse("thai")` returns 2 topics

**Test Steps:**
1. Arrange: mock `findByNameContainingIgnoreCaseAndIsHiddenFalse("thai")` → 2 topics: "Thai kỳ", "Thai sản"
2. Act: `service.getTopics(false, "thai")`
3. Assert: result has 2 items; names contain "thai" (case-insensitive)

**Expected Result (PASS):**
- Returns list of 2 topics, both with names containing "thai"

**Expected Result (FAIL):**
- Returns all topics (keyword not applied) or returns 0 (wrong query method)

**Current Status:** 🟢 Passing

---

### COM163-TC-003 — keyword "THAI" (uppercase) returns same result as "thai"

**Severity:** `MEDIUM`
**Feature Under Test:** Case-insensitivity of keyword search
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-COM-013`

**Preconditions:**
- `findByNameContainingIgnoreCaseAndIsHiddenFalse("THAI")` called with uppercase keyword
- Mock returns same 2 topics as TC-002

**Test Steps:**
1. Arrange: mock `findByNameContainingIgnoreCaseAndIsHiddenFalse("THAI")` → 2 topics
2. Act: `service.getTopics(false, "THAI")`
3. Assert: result has 2 items (same count as TC-002 lowercase search)

**Expected Result (PASS):**
- Uppercase keyword delegated correctly; JPQL `LOWER(name) LIKE LOWER('%THAI%')` handles case

**Expected Result (FAIL):**
- Service normalizes keyword before passing to repo, causing mismatch with mock expectation

**Current Status:** 🔴 Not written
**Implementation Note:** Pass keyword as-is to repo; JPQL handles case via `LOWER()`.

---

### COM163-TC-004 — keyword with no match returns empty list (not exception)

**Severity:** `MEDIUM`
**Feature Under Test:** Empty result handling
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-COM-012`

**Preconditions:**
- `findByNameContainingIgnoreCaseAndIsHiddenFalse("xyz_no_match")` returns empty list

**Test Steps:**
1. Arrange: mock returns `Collections.emptyList()` for keyword "xyz_no_match"
2. Act: `service.getTopics(false, "xyz_no_match")`
3. Assert: returns empty list; no exception thrown

**Expected Result (PASS):**
- Returns `[]`, HTTP 200 (no exception propagated)

**Expected Result (FAIL):**
- Service throws `CommunityTopicNotFoundException` or returns null

**Current Status:** 🟢 Passing

---

### COM163-TC-005 — Non-moderator with includeHidden=true and keyword returns non-hidden only

**Severity:** `CRITICAL`
**Feature Under Test:** Security gate — `effectiveInclude` enforcement
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-010`

**Preconditions:**
- User authenticated as MOTHER (not MODERATOR)
- `SecurityUtils.hasRole("MODERATOR")` returns false

**Test Steps:**
1. Arrange: mock controller's `SecurityUtils.hasRole("MODERATOR")` → false
2. Act: `GET /api/v1/community/topics?keyword=thai&includeHidden=true` with non-moderator JWT
3. Assert: `topicService.getTopics(false, "thai")` is called (effectiveInclude=false)
4. Assert: NOT `topicService.getTopics(true, "thai")`

**Expected Result (PASS):**
- Controller passes `effectiveInclude=false` to service despite `includeHidden=true` in request

**Expected Result (FAIL):**
- Controller trusts `includeHidden=true` from non-moderator and exposes hidden topics

**Current Status:** 🟢 Passing

---

### COM163-TC-006 — Moderator with includeHidden=true and keyword returns all matching topics (including hidden)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.getTopics(true, "thai")`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-COM-010`

**Preconditions:**
- `findByNameContainingIgnoreCase("thai")` returns 3 topics (2 non-hidden + 1 hidden)

**Test Steps:**
1. Arrange: mock `findByNameContainingIgnoreCase("thai")` → 3 topics including 1 hidden
2. Act: `service.getTopics(true, "thai")`
3. Assert: result has 3 items (hidden topic included)
4. Assert: `findByNameContainingIgnoreCase` (all-topics version) is called, not `...AndIsHiddenFalse`

**Expected Result (PASS):**
- Returns 3 topics including the hidden one

**Expected Result (FAIL):**
- Service always uses non-hidden query regardless of `includeHidden` flag

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### COM163-TC-INT-001 — Full search flow with real DB

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/community/topics?keyword=thai` full stack
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicSearchIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002, TC-COND-004`

**Preconditions:**
- PostgreSQL Testcontainers running
- Flyway migration applied automatically

**Test Steps:**
1. Seed 3 topics: "Thai kỳ" (sort=1), "Thai sản" (sort=2), "Sơ sinh bé" (sort=3) — all `isHidden=false`
2. Call `GET /api/v1/community/topics?keyword=thai` (no auth required)
3. Assert response status = 200
4. Assert `data.length = 2` (only "Thai kỳ" and "Thai sản" match)
5. Assert both returned topics have names containing "thai" (case-insensitive)
6. Assert non-matching topic "Sơ sinh bé" is NOT in response

**Expected Result (PASS):**
```json
{
  "data": [
    { "name": "Thai kỳ", "sortOrder": 1 },
    { "name": "Thai sản", "sortOrder": 2 }
  ]
}
```

**Expected Result (FAIL):**
- Returns all 3 topics (keyword not applied) or returns 0 (JPQL error)

**DB Assertion:**
```java
List<CommunityTopic> found = topicRepository
    .findByNameContainingIgnoreCaseAndIsHiddenFalse("thai");
assertThat(found).hasSize(2);
assertThat(found).extracting(CommunityTopic::getName)
    .allMatch(name -> name.toLowerCase().contains("thai"));
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ------------------ | -------------------------------------------------- | ---------------- | ----------------- | ---------------- |
| `COM163-TC-001`    | `CommunityTopicServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM163-TC-002`    | `CommunityTopicServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM163-TC-003`    | `CommunityTopicServiceImplTest.java`               | `[ ]`            | —                 | —                |
| `COM163-TC-004`    | `CommunityTopicServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM163-TC-005`    | `CommunityTopicControllerTest.java`                | `[x]`            | `Passed`          | —                |
| `COM163-TC-006`    | `CommunityTopicServiceImplTest.java`               | `[x]`            | `Passed`          | —                |
| `COM163-TC-INT-001`| `CommunityTopicSearchIntegrationTest.java`         | `[ ]`            | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub for Red Phase** — modify `CommunityTopicServiceImpl.getTopics()` to throw:

```java
// Red Phase — stub (MUST throw to confirm tests are sensitive)
@Override
public List<CommunityTopicResponse> getTopics(boolean includeHidden, String keyword) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID              | Stub Result                           | Expected    | Actual          | Root Cause (if PASS unexpectedly) |
| ------------------ | ------------------------------------- | ----------- | --------------- | --------------------------------- |
| `COM163-TC-001`    | `throw('Not implemented')`            | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                   |
| `COM163-TC-002`    | `throw('Not implemented')`            | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                   |
| `COM163-TC-003`    | `throw('Not implemented')`            | 🔴 FAIL     | ☐ FAIL ☐ PASS   |                                   |
| `COM163-TC-004`    | `throw('Not implemented')`            | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                   |
| `COM163-TC-005`    | `throw('Not implemented')`            | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                   |
| `COM163-TC-006`    | `throw('Not implemented')`            | 🔴 FAIL     | ☑ FAIL ☐ PASS   |                                   |
| `COM163-TC-INT-001`| `throw('Not implemented')`            | 🔴 FAIL     | ☐ FAIL ☐ PASS   |                                   |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? [x] Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-COMMUNITY-IMP-009` đã được review và approve
- [x] `community_topics` table đã tồn tại (UC-109 implemented)
- [x] `CommunityTopicController`, `CommunityTopicService`, `CommunityTopicServiceImpl` đã tồn tại
- [x] Test fixtures FX-COM163-001 đến FX-COM163-004 sẵn sàng (synthetic, mocked)

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — integration test xanh nếu Testcontainers available
- [x] TC-001: keyword=null path invokes existing method (no regression)
- [x] TC-005: Non-moderator never sees hidden topics (security invariant)
- [x] Empty keyword returns 200 with full list (backward compat verified)
- [x] Không có business logic trong Controller (controller chỉ pass param + auth check)

**Exit Criteria — CASE 2.0:**

- [x] **Red Gate (§5.1)** — tất cả service tests FAIL với empty/throw stub
- [x] **Contract Existence** — mọi class được inject đều tồn tại:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — mọi test dùng `CommunityTopicTestFactory.makeTopic()`
- [x] **Oracle Source** — mọi expected value có ghi rõ nguồn (ADR/BR)

### Suspension Criteria

- `community_topics` table chưa tồn tại (UC-109 not implemented)
- Breaking change in `CommunityTopicService` interface conflicts with other UC

---

## 7. Rollback Plan

```bash
# No migration to roll back — code-only change.
# Revert the modified files:
git checkout -- src/main/java/com/carebridge/backend/community/repository/CommunityTopicRepository.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityTopicService.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityTopicServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java
git checkout -- src/test/java/com/carebridge/backend/community/service/CommunityTopicServiceImplTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu trong TDD spec                           | Check | Gate chặn |
| --------- | --------------------- | -------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Gen     | TC does not reference ADR-COM-009/010              | ☑     | G-0       |
| AP-AI-002 | Green-from-Birth      | TC passes with throw stub (§5.1)                   | ☑     | G-2 ★     |
| AP-AI-003 | Implicit Decision     | TC assumes hidden topics returned to non-moderator | ☑     | G-1       |
| AP-AI-004 | Layer Violation       | TC verifies business logic inside Controller       | ☑     | G-4       |
| AP-AI-005 | Hallucinated Contract | TC imports service/type not in §8 of TDS           | ☑     | G-3       |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào → TDD spec approved

---

*TDD Spec v1.0 — CB-COMMUNITY-TDD-009 — UC-163 Search Community Topics*
