# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-225 View Verified Content Detail

**Document ID:** `CB-CONTENT-TEST-003`
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
- `04_Implement/UC225_ViewVerifiedContentDetail/UC225_ViewVerifiedContentDetail_TDS.md` — TDS
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.18.2` — UC-225 spec

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                                          |
| ---------- | ------------------------------------- | ---------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Khởi tạo TDD spec cho UC-225 View Verified Content Detail |

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

| Field                   | Value                                                                             |
| ----------------------- | --------------------------------------------------------------------------------- |
| **Feature / UC ID**     | `UC-225`                                                                          |
| **Module**              | `content — ViewVerifiedContentDetail`                                            |
| **Spec gốc**            | `CB-CONTENT-IMP-003`                                                             |
| **Priority**            | 🟠 P1                                                                             |
| **Milestone**           | `M3 Alpha — 2026-07-11`                                                          |
| **Data Classification** | `Internal`                                                                       |
| **Compliance Scope**    | `BR-RBAC, BR-SAFETY`                                                             |
| **Upstream Dependencies** | `security (JWT), content (ContentItem)`                                        |
| **Downstream Consumers** | `Mobile content detail screen, Admin web content view`                          |

### 1.1 AI Generation Context (CASE 2.0)

| Field               | Value                                                    |
| ------------------- | -------------------------------------------------------- |
| **AI Assisted?**    | `Yes`                                                    |
| **Constraint Source** | `UC225_TDS §17`, `ADR-CON-225-1`, `ADR-CON-225-2`    |
| **Constraints Injected** | No DB column for warning; computed isContentStale; APPROVED filter; authorId excluded |
| **Model**           | `claude-sonnet-4-6`                                      |
| **Trust Level**     | `T2 → T3 (pending Red Gate)`                            |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | ContentDetailResponse thiếu `sourceLabel`, `updatedAt` | Fields exist in ContentItem entity + DB but not mapped in DTO | Tests phải verify new fields present in response body |
| L2 | "related warnings" không có DB column | ADR-CON-225-2: computed from `updatedAt > 365 days` | Test TC-225-2 với content cũ hơn 12 tháng; verify `isContentStale=true` |
| L3 | `author_user_id` in ContentItem entity | BR-PRIVACY: must NOT appear in ContentDetailResponse | TC-225-8 phải verify field absent from response |
| L4 | Existing endpoint already filters status=APPROVED | ContentServiceImpl must already have this filter — verify | TC-225-3 phải verify non-APPROVED content returns 404 |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
ViewVerifiedContentDetail bao gồm:
├── Mapper layer (ContentMapper.toDetailResponse) — primary test target
│   ├── Maps sourceLabel, updatedAt from ContentItem
│   └── Computes isContentStale from updatedAt (365 day threshold)
├── Controller layer (ContentController.getContentById)
│   └── @WebMvcTest + MockMvc → verify HTTP 200/401/404 + new fields in JSON
└── Integration (Testcontainers + @SpringBootTest)
    └── DB-to-DTO mapping end-to-end with real content_items data
```

### TDS-02 — Test Basis

- UC-225 (SRS §3.3.18.2): "Displays content, source, version, update date, and related warnings"
- ADR-CON-225-1: APPROVED-only for regular users
- ADR-CON-225-2: `isContentStale = (updatedAt < NOW - 365 days)`
- BR-SAFETY: stale content must be flagged
- BR-PRIVACY (existing): `author_user_id` excluded from response

### TDS-03 — Test Conditions

| TC-ID    | Condition                                          | Technique          | Layer        |
| -------- | -------------------------------------------------- | ------------------ | ------------ |
| TC-225-1 | Get APPROVED content with all fields present       | Happy path         | Mapper + CT  |
| TC-225-2 | Content updated 13 months ago → isContentStale=true | BR-SAFETY test    | Mapper       |
| TC-225-3 | Get PENDING content as regular user → 404          | Authorization test | CT           |
| TC-225-4 | Get non-existent content ID → 404                  | Error path         | CT           |
| TC-225-5 | Unauthenticated request → 401                      | Security test      | Controller   |
| TC-225-6 | Content with null sourceLabel → null in response   | Null-safety test   | Mapper       |
| TC-225-7 | Content with null updatedAt → isContentStale=false | Null-safety test   | Mapper       |
| TC-225-8 | author_user_id not in response                     | Privacy test       | CT (JSON check) |

### TDS-04 — Test Data Requirements

- All data from `ViewContentDetailTestFactory` (Props Isolation)
- Clock manipulation via `Clock.fixed()` for stale content test
- Testcontainers for integration tests

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
package com.carebridge.backend.content;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ViewContentDetailTestFactory {

    public static final UUID CONTENT_ID   = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    public static final UUID TOPIC_ID     = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID AUTHOR_ID    = UUID.fromString("11111111-0000-0000-0000-000000000001");

    public static ContentItem makeApprovedContent() {
        return ContentItem.builder()
            .id(CONTENT_ID)
            .type(ContentType.ARTICLE)
            .title("Dinh dưỡng thai kỳ")
            .body("Nội dung chi tiết về dinh dưỡng...")
            .stage(ContentStage.PREGNANCY)
            .topicId(TOPIC_ID)
            .status(ContentStatus.APPROVED)
            .versionNo(1)
            .sourceLabel("WHO Guidelines 2024")
            .authorUserId(AUTHOR_ID)
            .publishedAt(Instant.now().minus(30, ChronoUnit.DAYS))
            .updatedAt(Instant.now().minus(30, ChronoUnit.DAYS))
            .build();
    }

    public static ContentItem makeStaleContent() {
        // Updated 400 days ago (> 365 day threshold)
        return makeApprovedContent().toBuilder()
            .updatedAt(Instant.now().minus(400, ChronoUnit.DAYS))
            .build();
    }

    public static ContentItem makeContentWithNullSourceLabel() {
        return makeApprovedContent().toBuilder().sourceLabel(null).build();
    }

    public static ContentItem makeContentWithNullUpdatedAt() {
        return makeApprovedContent().toBuilder().updatedAt(null).build();
    }

    public static ContentItem makePendingContent() {
        return makeApprovedContent().toBuilder().status(ContentStatus.PENDING).build();
    }
}
```

---

### TC-225-1 — Get APPROVED content → all required fields present

**Severity:** 🔴 Critical
**Oracle source:** UC-225 "displays content, source, version, update date"; `ContentItem` entity fields
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert (Mapper Unit Test):**
```java
// Arrange
ContentItem item = ViewContentDetailTestFactory.makeApprovedContent();

// Act
ContentDetailResponse response = contentMapper.toDetailResponse(item);

// Assert
assertThat(response.getSourceLabel()).isEqualTo("WHO Guidelines 2024");
assertThat(response.getUpdatedAt()).isNotNull();
assertThat(response.isContentStale()).isFalse();
assertThat(response.getId()).isEqualTo(CONTENT_ID);
assertThat(response.getVersion()).isEqualTo(1);
```

**Intended test file:** `ContentMapperTest.java`
**Failure signature:** `AssertionError: expected "WHO Guidelines 2024" but was null` — sourceLabel not mapped

---

### TC-225-2 — Content updated 13 months ago → isContentStale = true

**Severity:** 🔴 Critical (BR-SAFETY)
**Oracle source:** ADR-CON-225-2 — 365 day stale threshold; BR-SAFETY healthcare warning
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange — updatedAt is 400 days in the past
ContentItem staleItem = ViewContentDetailTestFactory.makeStaleContent();

// Act
ContentDetailResponse response = contentMapper.toDetailResponse(staleItem);

// Assert
assertThat(response.isContentStale()).isTrue();
```

**Intended test file:** `ContentMapperTest.java`
**Failure signature:** `expected true but was false` — stale computation not implemented

---

### TC-225-7 — Content with null updatedAt → isContentStale = false (null-safe)

**Severity:** 🟠 High
**Oracle source:** ADR-CON-225-2 — null updatedAt must not throw NPE; default to non-stale
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert:**
```java
// Arrange
ContentItem nullUpdated = ViewContentDetailTestFactory.makeContentWithNullUpdatedAt();

// Act — must not throw NullPointerException
ContentDetailResponse response = contentMapper.toDetailResponse(nullUpdated);

// Assert
assertThat(response.isContentStale()).isFalse();
assertThat(response.getUpdatedAt()).isNull();
```

**Intended test file:** `ContentMapperTest.java`

---

### TC-225-8 — author_user_id must NOT appear in API response

**Severity:** 🔴 Critical (BR-PRIVACY)
**Oracle source:** BR-PRIVACY (comment in existing `ContentDetailResponse`: "authorId intentionally excluded")
**TDD Phase:** 🔴 RED — chưa implement
**Current Status:** 🔴 Not written

**Arrange / Act / Assert (Controller Test):**
```java
// Arrange
when(contentService.getContentById(CONTENT_ID)).thenReturn(someResponse);

// Act
MvcResult result = mockMvc.perform(get("/api/v1/content/{id}", CONTENT_ID)
    .header("Authorization", "Bearer " + validJwt))
    .andExpect(status().isOk())
    .andReturn();

// Assert — JSON must not contain authorId / authorUserId
String json = result.getResponse().getContentAsString();
assertThat(json).doesNotContain("authorId");
assertThat(json).doesNotContain("authorUserId");
```

**Intended test file:** `ContentControllerTest.java`

---

## 5. Red-Green-Refactor Tracker

| TC-ID    | Test File                       | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| -------- | ------------------------------- | ---------------- | ----------------- | ---------------- |
| TC-225-1 | `ContentMapperTest.java`        | [ ]              | —                 | —                |
| TC-225-2 | `ContentMapperTest.java`        | [ ]              | —                 | —                |
| TC-225-3 | `ContentControllerTest.java`    | [ ]              | —                 | —                |
| TC-225-4 | `ContentControllerTest.java`    | [ ]              | —                 | —                |
| TC-225-5 | `ContentControllerTest.java`    | [ ]              | —                 | —                |
| TC-225-6 | `ContentMapperTest.java`        | [ ]              | —                 | —                |
| TC-225-7 | `ContentMapperTest.java`        | [ ]              | —                 | —                |
| TC-225-8 | `ContentControllerTest.java`    | [ ]              | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0) — Gate 2

> **Note:** UC-225 is a DTO extension on an existing endpoint. The existing `getContentById()` endpoint already exists. Red Gate applies to the **new fields** — tests for `sourceLabel`, `updatedAt`, `isContentStale` will fail until the mapper is updated.

**Red Gate Verification:**

| TC-ID    | Expected (before mapper update) | Actual           |
| -------- | ------------------------------- | ---------------- |
| TC-225-1 | 🔴 FAIL (sourceLabel=null)      | ☐ FAIL ☐ PASS   |
| TC-225-2 | 🔴 FAIL (isContentStale=false)  | ☐ FAIL ☐ PASS   |
| TC-225-7 | 🔴 FAIL (may NPE)               | ☐ FAIL ☐ PASS   |

Tất cả FAIL? [ ] Yes [ ] No

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-CONTENT-IMP-003` approved
- [ ] `ContentDetailResponse.java` updated with new fields (sourceLabel, updatedAt, isContentStale)
- [ ] Red Gate: tests written but mapper NOT yet updated (RED phase)

### Exit Criteria (DoD)
- [ ] All 8 TCs pass (`./mvnw test -Dtest=ContentMapperTest,ContentControllerTest`)
- [ ] TC-225-1 (all fields), TC-225-2 (BR-SAFETY stale), TC-225-7 (null-safe), TC-225-8 (privacy) must pass
- [ ] Red Gate confirmed
- [ ] Props Isolation factory used
- [ ] Mobile content detail screen updated to show stale warning banner

---

## 7. Rollback Plan

```bash
git revert <commit-hash-of-contentDetailResponse-update>
# No schema migration to rollback
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern                          | Signal                              | Check | Gate  |
| ----- | ------------------------------------- | ----------------------------------- | ----- | ----- |
| AP-1  | Test passes before mapper update      | TC-225-1 passes with old mapper     | [ ]   | RG-2  |
| AP-2  | isContentStale stored in DB           | New DB column for stale flag        | [ ]   | C3    |
| AP-3  | authorId in JSON response             | TC-225-8 assertion absent           | [ ]   | C2    |
| AP-4  | NPE when updatedAt is null            | TC-225-7 throws NullPointerException | [ ]  | CG-2  |
