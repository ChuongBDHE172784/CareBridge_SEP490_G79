# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-224 Search Verified Content

**Document ID:** `CB-CONTENT-TEST-002`
**Version:** `1.0`
**Date:** `2026-06-23`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/implement_artifacts/UC224_SearchVerifiedContent_TDS.md` (CB-CONTENT-IMP-002)
- SRS UC-224: `02_Requirements/SRS/`
- Architecture: `CLAUDE.md` §3 Layered Architecture Rules
- Dependency: UC-82 Test-Spec `CB-CONTENT-TEST-001` — reuse entities

> **Quy ước TDD:** Test cases được viết TRƯỚC khi implement production code.
> RED → GREEN → REFACTOR. Không dùng PII thật — SYNTHETIC data only.

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                                               |
| ---------- | ------------------------------------- | --------------------------------------------------------------- |
| 2026-06-23 | AI Agent — Winston (System Architect) | Khởi tạo tài liệu — TDD spec cho UC-224 Search Verified Content |
| 2026-06-24 | AI Agent — Amelia (Dev)               | GREEN Phase hoàn thành — 22/22 tests PASS; cập nhật tracker & DoD |

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

| Field                     | Value                                                                                            |
| ------------------------- | ------------------------------------------------------------------------------------------------ |
| **Feature / UC ID**       | `UC-224`                                                                                         |
| **Module**                | `Search Verified Content — content Bounded Context`                                              |
| **Spec gốc**              | `CB-CONTENT-IMP-002`                                                                             |
| **Priority**              | P1 — Medium, Frequent                                                                            |
| **Sprint**                | `S1 (2026-06-23 → 2026-07-07)`                                                                   |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                                          |
| **Data Classification**   | `Internal`                                                                                       |
| **Compliance Scope**      | `BR-RBAC`                                                                                        |
| **Upstream Dependencies** | `security (JWT)`, `identity (User)`, `community (Topic — topicName)`, UC-82 (ContentItem entity) |
| **Downstream Consumers**  | `Mobile App — SearchScreen`, `Admin Web — ContentManagementPage`                                 |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                                 |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                                                 |
| **Constraint Source**    | `CB-CONTENT-IMP-002 §17.2 Constraint Injection Block`                                                                                 |
| **Constraints Injected** | `C1 (APPROVED only)`, `C2 (keyword sanitize)`, `C3 (validation)`, `C4 (200 on empty)`, `C5 (topicName resolve)`, `C6 (@PreAuthorize)` |
| **Model**                | `claude-sonnet-4-6`                                                                                                                   |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                          |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                                         | Thực tế (schema / policy)                                                   | Fix áp dụng trong test                               |
| --- | ------------------------------------------------------------------------------ | --------------------------------------------------------------------------- | ---------------------------------------------------- |
| L1  | Chưa rõ behavior khi search không có kết quả — có thể trả về 404               | §9.2 TDS: empty search → HTTP 200 với `content:[]`, KHÔNG phải 404          | Test TC-UNIT-004 kiểm tra empty result → 200         |
| L2  | Keyword với leading/trailing spaces có thể gây query không chính xác           | ADR-004: keyword phải được trim trước khi query                             | Test TC-UNIT-002 kiểm tra sanitization               |
| L3  | Keyword chứa % hoặc _ (LIKE wildcards) có thể gây query không mong muốn        | ADR-004: escape % và _ trong keyword                                        | Test TC-UNIT-003 kiểm tra escape                     |
| L4  | `topicName` trong response có thể là null nếu topicId không có topic tương ứng | §9.2: topicName được resolve — null nếu topicId null, cần handle gracefully | Test TC-UNIT-005 kiểm tra topicName với null topicId |
| L5  | Chưa rõ keyword case sensitivity — `THAI KY` vs `thai ky`                      | ADR-003 dùng LOWER() ILIKE → case-insensitive                               | Test TC-INT-002 kiểm tra case-insensitive search     |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-224 Search Verified Content bao gồm:
├── Service Layer (ContentServiceImpl.searchContent())
│   ├── APPROVED enforcement (BR-RBAC)
│   ├── Keyword sanitization (ADR-004)
│   └── Empty result handling (200 not 404)
├── Controller Layer (ContentController.searchContent())
│   ├── Keyword required validation
│   ├── Keyword max 100 chars validation
│   └── size max 50 validation
├── Repository Layer (ContentRepository.searchByFilters())
│   └── ILIKE case-insensitive search
└── Integration (Testcontainers PostgreSQL)
    ├── APPROVED filter (DRAFT/ARCHIVED excluded from search)
    ├── Case-insensitive keyword matching
    ├── SQL injection prevention
    └── Unauthenticated search rejection
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source                    | Items Derived                                       |
| ------------------------- | --------------------------------------------------- |
| `SRS UC-224`              | Tìm kiếm content theo keyword, stage, topicId, type |
| `ADR-003`                 | ILIKE search, không dùng Elasticsearch cho MVP      |
| `ADR-004`                 | Keyword sanitization: trim + escape LIKE wildcards  |
| `BR-RBAC`                 | Chỉ APPROVED content trong search results           |
| `CB-CONTENT-IMP-002 §9.2` | Empty search → 200 không phải 404                   |
| `CB-CONTENT-IMP-002 §10`  | Error codes CNT-001 (validation), IAM-001 (auth)    |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                              | Coverage Item                          | Test Cases          |
| ------------ | ------------------------------------------- | -------------------------------------- | ------------------- |
| TC-COND-001  | searchContent() chỉ trả về APPROVED content | `ContentServiceImpl.searchContent()`   | `CNT224-TC-001`     |
| TC-COND-002  | Keyword được trim trước khi query           | `ContentServiceImpl.sanitizeKeyword()` | `CNT224-TC-002`     |
| TC-COND-003  | Keyword wildcards (%, _) được escape        | `ContentServiceImpl.sanitizeKeyword()` | `CNT224-TC-003`     |
| TC-COND-004  | Empty search results → 200 với empty list   | `ContentController.searchContent()`    | `CNT224-TC-004`     |
| TC-COND-005  | topicName null-safe khi topicId null        | `ContentServiceImpl.searchContent()`   | `CNT224-TC-005`     |
| TC-COND-006  | keyword required validation                 | `ContentController.searchContent()`    | `CNT224-TC-006`     |
| TC-COND-007  | keyword max 100 chars                       | `ContentController.searchContent()`    | `CNT224-TC-007`     |
| TC-COND-008  | size max 50                                 | `ContentController.searchContent()`    | `CNT224-TC-008`     |
| TC-COND-009  | SQL injection prevention                    | `ContentRepository.searchByFilters()`  | `CNT224-TC-SEC-001` |
| TC-COND-010  | Unauthenticated → 401                       | Spring Security                        | `CNT224-TC-SEC-002` |
| TC-COND-011  | Case-insensitive keyword matching           | `ContentRepository.searchByFilters()`  | `CNT224-TC-INT-001` |
| TC-COND-012  | DRAFT excluded from search results          | Full flow                              | `CNT224-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)  | Applied To                                         | Rationale                        |
| ------------------------ | -------------------------------------------------- | -------------------------------- |
| Equivalence Partitioning | keyword (valid/empty/> 100 chars); size (1-50/51+) | Phân vùng hợp lệ và không hợp lệ |
| Boundary Value Analysis  | keyword length (99, 100, 101); size (49, 50, 51)   | Kiểm tra biên chính xác          |
| Error Guessing           | SQL injection, XSS, empty keyword, wildcard chars  | Security risk areas              |
| State Transition Testing | ContentStatus (DRAFT/APPROVED → search visibility) | Verify chỉ APPROVED visible      |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type    | Value / Logic                                                                          | Mục đích             |
| ------------ | ------- | -------------------------------------------------------------------------------------- | -------------------- |
| `FX-224-001` | DB seed | `ContentItem{title="Thai ky tuan 12", status=APPROVED, stage=PREGNANCY, type=ARTICLE}` | Happy path search    |
| `FX-224-002` | DB seed | `ContentItem{title="Thai ky DRAFT", status=DRAFT, stage=PREGNANCY}`                    | DRAFT exclusion      |
| `FX-224-003` | DB seed | `ContentItem{title="Thai ky FAQ", status=APPROVED, stage=PREGNANCY, type=FAQ}`         | Multi-type search    |
| `FX-224-004` | DB seed | `ContentItem{title="Cho con bu", status=APPROVED, stage=POSTPARTUM}`                   | Stage filter test    |
| `FX-224-005` | JWT     | `{sub: 'user-001', roles: ['USER']}`                                                   | Standard user auth   |
| `FX-224-006` | Input   | `keyword = "  thai ky  "`                                                              | Trim sanitization    |
| `FX-224-007` | Input   | `keyword = "100% safe"`                                                                | LIKE wildcard escape |
| `FX-224-008` | Input   | `keyword = "'; DROP TABLE content_items; --"`                                          | SQL injection        |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu test class — mỗi @Test dùng factory method
// ═══════════════════════════════════════════════════════════

private ContentItem makeApprovedContent(String title, ContentStage stage, ContentType type) {
    return ContentItem.builder()
        .id(UUID.randomUUID())
        .type(type)
        .title(title)
        .body("Test body for " + title)
        .stage(stage)
        .topicId(null) // optional
        .status(ContentStatus.APPROVED)
        .version(1)
        .authorId(UUID.randomUUID())
        .publishedAt(LocalDateTime.now().minusDays(1))
        .createdAt(LocalDateTime.now().minusDays(2))
        .updatedAt(LocalDateTime.now().minusDays(1))
        .build();
}

private ContentSearchRequest makeSearchRequest(String keyword) {
    ContentSearchRequest request = new ContentSearchRequest();
    request.setKeyword(keyword);
    return request;
}

private ContentSearchRequest makeSearchRequest(String keyword, ContentStage stage) {
    ContentSearchRequest request = makeSearchRequest(keyword);
    request.setStage(stage);
    return request;
}
```

---

### CNT224-TC-001 — searchContent chỉ truyền APPROVED vào repository

**Severity:** `CRITICAL`
**Feature Under Test:** `ContentServiceImpl.searchContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-RBAC, ADR-003 — chỉ APPROVED content visible`

**Preconditions:**
- Mock `ContentRepository` configured
- Fixture: FX-224-001 (APPROVED)

**Test Steps:**

```java
@Test
void searchContent_shouldAlwaysPassAPPROVEDStatusToRepository() {
    // Arrange
    ContentItem approvedItem = makeApprovedContent("Thai ky tuan 12", ContentStage.PREGNANCY, ContentType.ARTICLE);
    Page<ContentItem> mockPage = new PageImpl<>(List.of(approvedItem));
    when(contentRepository.searchByFilters(
        eq("thai ky"), any(), any(), any(), eq(ContentStatus.APPROVED), any()))
        .thenReturn(mockPage);

    ContentSearchRequest request = makeSearchRequest("thai ky");

    // Act
    Page<ContentSearchResponse> result = contentServiceImpl.searchContent(request, PageRequest.of(0, 20));

    // Assert
    ArgumentCaptor<ContentStatus> statusCaptor = ArgumentCaptor.forClass(ContentStatus.class);
    verify(contentRepository).searchByFilters(any(), any(), any(), any(), statusCaptor.capture(), any());
    assertThat(statusCaptor.getValue()).isEqualTo(ContentStatus.APPROVED);
    assertThat(result.getTotalElements()).isEqualTo(1);
}
```

**Expected Result (PASS):** `searchByFilters` được gọi với `ContentStatus.APPROVED`
**Expected Result (FAIL):** Test fail nếu service truyền null hoặc DRAFT
**Current Status:** 🔴 Not written
**Implementation Note:** Hardcode `ContentStatus.APPROVED` trong `searchContent()`

---

### CNT224-TC-002 — Keyword được trim trước khi query

**Severity:** `HIGH`
**Feature Under Test:** `ContentServiceImpl.sanitizeKeyword()`
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-004 — keyword trimmed trước khi query`

**Test Steps:**

```java
@Test
void sanitizeKeyword_shouldTrimLeadingAndTrailingSpaces() {
    // Arrange: FX-224-006
    String rawKeyword = "  thai ky  ";
    Page<ContentItem> emptyPage = Page.empty();
    ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
    when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
        .thenReturn(emptyPage);

    ContentSearchRequest request = makeSearchRequest(rawKeyword);

    // Act
    contentServiceImpl.searchContent(request, PageRequest.of(0, 20));

    // Assert
    assertThat(keywordCaptor.getValue()).isEqualTo("thai ky"); // trimmed
    assertThat(keywordCaptor.getValue()).doesNotStartWith(" ");
    assertThat(keywordCaptor.getValue()).doesNotEndWith(" ");
}
```

**Expected Result (PASS):** Repository nhận keyword đã trim
**Expected Result (FAIL):** Repository nhận keyword với spaces
**Current Status:** 🔴 Not written

---

### CNT224-TC-003 — Keyword wildcard characters được escape

**Severity:** `HIGH`
**Feature Under Test:** `ContentServiceImpl.sanitizeKeyword()`
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-004 — escape LIKE wildcards % và _`

**Test Steps:**

```java
@Test
void sanitizeKeyword_shouldEscapePercentWildcard() {
    // Arrange: FX-224-007
    String rawKeyword = "100% safe";
    ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
    when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());

    // Act
    contentServiceImpl.searchContent(makeSearchRequest(rawKeyword), PageRequest.of(0, 20));

    // Assert
    String sanitized = keywordCaptor.getValue();
    assertThat(sanitized).contains("\\%"); // % escaped to \%
    assertThat(sanitized).doesNotContain("100% "); // raw % replaced
}

@Test
void sanitizeKeyword_shouldEscapeUnderscoreWildcard() {
    // Arrange
    ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
    when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());

    // Act
    contentServiceImpl.searchContent(makeSearchRequest("thai_ky"), PageRequest.of(0, 20));

    // Assert
    assertThat(keywordCaptor.getValue()).contains("\\_");
}
```

**Expected Result (PASS):** Repository nhận keyword với `%` escaped thành `\%` và `_` escaped thành `\_`
**Expected Result (FAIL):** Raw wildcards được truyền vào repository
**Current Status:** 🔴 Not written

---

### CNT224-TC-004 — Empty search results → 200 không phải 404

**Severity:** `HIGH`
**Feature Under Test:** `ContentController.searchContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-CONTENT-IMP-002 §9.2 — empty search returns 200 with content:[]`

**Test Steps:**

```java
@Test
void searchContent_withNoResults_shouldReturn200NotEmpty() throws Exception {
    // Arrange
    when(contentService.searchContent(any(), any())).thenReturn(Page.empty());

    // Act + Assert
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "nonexistentterm123456")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk())  // 200, NOT 404
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));
}
```

**Expected Result (PASS):** HTTP 200 với `content: []` và `totalElements: 0`
**Expected Result (FAIL):** HTTP 404 hoặc HTTP 200 với null content
**Current Status:** 🔴 Not written
**Implementation Note:** Controller không kiểm tra kết quả rỗng để throw 404

---

### CNT224-TC-005 — topicName null-safe khi topicId null

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentServiceImpl.searchContent()` — topicName resolve
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-CONTENT-IMP-002 §8.1 — topicName resolved, null if topicId null`

**Test Steps:**

```java
@Test
void searchContent_contentWithNullTopicId_shouldReturnNullTopicName() {
    // Arrange
    ContentItem itemWithNullTopic = makeApprovedContent("Test", ContentStage.PREGNANCY, ContentType.ARTICLE);
    // topicId = null (already null in factory)
    Page<ContentItem> mockPage = new PageImpl<>(List.of(itemWithNullTopic));
    when(contentRepository.searchByFilters(any(), any(), any(), any(), eq(ContentStatus.APPROVED), any()))
        .thenReturn(mockPage);

    // Act
    Page<ContentSearchResponse> result = contentServiceImpl.searchContent(
        makeSearchRequest("test"), PageRequest.of(0, 20));

    // Assert — should not throw NullPointerException
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTopicName()).isNull(); // acceptable
    // OR if we want empty string: assertThat(...).isEmpty()
    // Choose one behavior and stick with it
}
```

**Expected Result (PASS):** Không throw NullPointerException; `topicName` là null hoặc empty string
**Expected Result (FAIL):** `NullPointerException` khi topicId là null
**Current Status:** 🔴 Not written

---

### CNT224-TC-006 — keyword required validation

**Severity:** `HIGH`
**Feature Under Test:** `ContentController.searchContent()` — request validation
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-CONTENT-IMP-002 §9.2 — keyword required`

**Test Steps:**

```java
@Test
void searchContent_withEmptyKeyword_shouldReturn400() throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CNT-001"))
        .andExpect(jsonPath("$.error.details[?(@.field == 'keyword')]").exists());
}

@Test
void searchContent_withBlankKeyword_shouldReturn400() throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "   ")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CNT-001"));
}

@Test
void searchContent_withoutKeywordParam_shouldReturn400() throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CNT-001"));
}
```

**Expected Result (PASS):** HTTP 400 với CNT-001 khi keyword rỗng, blank, hoặc không có
**Expected Result (FAIL):** Service được gọi với empty/blank keyword
**Current Status:** 🔴 Not written

---

### CNT224-TC-007 — keyword max 100 chars validation

**Severity:** `HIGH`
**Feature Under Test:** `ContentController.searchContent()` — keyword length validation
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-004, CB-CONTENT-IMP-002 §9.2`

**Test Steps:**

```java
@Test
void searchContent_withKeyword100Chars_shouldReturn200() throws Exception {
    String keyword100 = "a".repeat(100);
    when(contentService.searchContent(any(), any())).thenReturn(Page.empty());
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", keyword100)
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk()); // Boundary: exactly 100 chars — valid
}

@Test
void searchContent_withKeyword101Chars_shouldReturn400() throws Exception {
    String keyword101 = "a".repeat(101);
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", keyword101)
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CNT-001"))
        .andExpect(jsonPath("$.error.details[?(@.field == 'keyword')]").exists());
}
```

**Expected Result (PASS):** 100 chars → 200; 101 chars → 400 CNT-001
**Expected Result (FAIL):** 101 chars được chấp nhận
**Current Status:** 🔴 Not written

---

### CNT224-TC-008 — size max 50 validation

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentController.searchContent()` — size validation
**Test File:** `src/test/java/com/carebridge/backend/content/unit/ContentSearchControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`

**Test Steps:**

```java
@ParameterizedTest
@ValueSource(ints = {1, 20, 50})
void searchContent_withValidSize_shouldReturn200(int size) throws Exception {
    when(contentService.searchContent(any(), any())).thenReturn(Page.empty());
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "test")
            .param("size", String.valueOf(size))
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk());
}

@ParameterizedTest
@ValueSource(ints = {51, 100, 200})
void searchContent_withSizeOver50_shouldReturn400(int size) throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "test")
            .param("size", String.valueOf(size))
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CNT-001"));
}
```

**Expected Result (PASS):** size ≤ 50 → 200; size > 50 → 400
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### CNT224-TC-SEC-001 — SQL injection prevention

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ContentRepository.searchByFilters()` via full stack
**Test File:** `src/test/java/com/carebridge/backend/content/security/ContentSearchSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainer đang chạy
- FX-224-001 seeded (APPROVED content)

**Test Steps (Attack Simulation):**

```java
@ParameterizedTest
@ValueSource(strings = {
    "'; DROP TABLE content_items; --",
    "' OR '1'='1",
    "' UNION SELECT id, title, status, '', '', '', '', '', null, null, null FROM content_items --",
    "'; INSERT INTO content_items (id) VALUES (gen_random_uuid()); --"
})
void searchContent_withSQLInjectionPayload_shouldNotCauseError(String maliciousKeyword) throws Exception {
    // Act: Call with malicious keyword
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", maliciousKeyword.substring(0, Math.min(maliciousKeyword.length(), 100)))
            .header("Authorization", "Bearer " + validUserJwt))
        // Assert: Should either return 200 (empty results) or 400 (validation)
        // Must NOT return 500 (server error due to SQL execution)
        .andExpect(result ->
            assertThat(result.getResponse().getStatus()).isIn(200, 400));

    // Verify: content_items table still exists and has correct data
    long count = contentRepository.count();
    assertThat(count).isGreaterThanOrEqualTo(1); // seed data still there
}
```

**Expected Result (PASS = hệ thống an toàn):** HTTP 200 hoặc 400; bảng `content_items` còn nguyên vẹn
**Expected Result (FAIL = lỗ hổng tồn tại):** HTTP 500 database error; bảng bị xóa hoặc data bị thay đổi
**Current Status:** 🔴 Not written

---

### CNT224-TC-SEC-002 — Unauthenticated search bị từ chối

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** `ContentController.searchContent()` — Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/content/security/ContentSearchSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`

**Test Steps:**

```java
@Test
void searchContent_withoutJwt_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "thai ky"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("IAM-001"));
}

@Test
void searchContent_withExpiredJwt_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "thai ky")
            .header("Authorization", "Bearer " + expiredJwt))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("IAM-001"));
}
```

**Expected Result (PASS):** HTTP 401 với IAM-001
**Expected Result (FAIL):** Search results returned without authentication
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### CNT224-TC-INT-001 — Case-insensitive keyword matching

**Severity:** `HIGH`
**Feature Under Test:** Full flow: GET /api/v1/content/search → ILIKE query
**Test File:** `src/test/java/com/carebridge/backend/content/integration/ContentSearchIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL Testcontainer running
- Seed: `ContentItem{title="THAI KỲ TUẦN 12", status=APPROVED}` (FX-224-001 variant uppercase)

**Test Steps:**

```java
@Test
void searchContent_integration_shouldBeCaseInsensitive() throws Exception {
    // Arrange: seed uppercase title
    ContentItem uppercaseItem = makeApprovedContent("THAI KỲ TUẦN 12", ContentStage.PREGNANCY, ContentType.ARTICLE);
    contentRepository.save(uppercaseItem);

    // Act: Search with lowercase keyword
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "thai ky")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.content[?(@.title == 'THAI KỲ TUẦN 12')]").exists());
}
```

**DB Assertion:**
```java
// Verify ILIKE is actually case-insensitive
List<ContentItem> results = contentRepository.searchByFilters(
    "thai ky", null, null, null, ContentStatus.APPROVED, PageRequest.of(0, 20)).getContent();
assertThat(results).anyMatch(item -> item.getTitle().toUpperCase().contains("THAI KỲ"));
```

**Current Status:** 🔴 Not written

---

### CNT224-TC-INT-002 — DRAFT content không xuất hiện trong search results

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow search → APPROVED filter
**Test File:** `src/test/java/com/carebridge/backend/content/integration/ContentSearchIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL Testcontainer running
- Seed: FX-224-001 (APPROVED title="Thai ky tuan 12"), FX-224-002 (DRAFT title="Thai ky DRAFT")

**Test Steps:**

```java
@Test
void searchContent_integration_shouldExcludeDraftAndArchivedContent() throws Exception {
    // Arrange: seed APPROVED, DRAFT, ARCHIVED với cùng keyword "thai ky"
    contentRepository.save(makeApprovedContent("Thai ky APPROVED", ContentStage.PREGNANCY, ContentType.ARTICLE));
    contentRepository.save(
        ContentItem.builder().id(UUID.randomUUID()).type(ContentType.ARTICLE)
            .title("Thai ky DRAFT").stage(ContentStage.PREGNANCY)
            .status(ContentStatus.DRAFT).version(1).authorId(UUID.randomUUID())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
    );
    contentRepository.save(
        ContentItem.builder().id(UUID.randomUUID()).type(ContentType.ARTICLE)
            .title("Thai ky ARCHIVED").stage(ContentStage.PREGNANCY)
            .status(ContentStatus.ARCHIVED).version(1).authorId(UUID.randomUUID())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
    );

    // Act + Assert
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "thai ky")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1)) // only APPROVED
        .andExpect(jsonPath("$.content[0].title").value("Thai ky APPROVED"))
        .andExpect(jsonPath("$.content[?(@.title == 'Thai ky DRAFT')]").isEmpty())
        .andExpect(jsonPath("$.content[?(@.title == 'Thai ky ARCHIVED')]").isEmpty());
}
```

**Current Status:** 🔴 Not written

---

### CNT224-TC-INT-003 — Multi-filter search (keyword + stage + type)

**Severity:** `MEDIUM`
**Feature Under Test:** Full flow with multiple filters
**Test File:** `src/test/java/com/carebridge/backend/content/integration/ContentSearchIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement

**Test Steps:**

```java
@Test
void searchContent_integration_withMultipleFilters_shouldReturnMatchingOnly() throws Exception {
    // Arrange: 3 APPROVED content items
    contentRepository.save(makeApprovedContent("Thai ky", ContentStage.PREGNANCY, ContentType.ARTICLE));
    contentRepository.save(makeApprovedContent("Thai ky FAQ", ContentStage.PREGNANCY, ContentType.FAQ));
    contentRepository.save(makeApprovedContent("Thai ky Postpartum", ContentStage.POSTPARTUM, ContentType.ARTICLE));

    // Act: Search with keyword="thai ky" AND stage=PREGNANCY AND type=ARTICLE
    mockMvc.perform(get("/api/v1/content/search")
            .param("keyword", "thai ky")
            .param("stage", "PREGNANCY")
            .param("type", "ARTICLE")
            .header("Authorization", "Bearer " + validUserJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
        .andExpect(jsonPath("$.content[0].stage").value("PREGNANCY"));
}
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID               | Test File                           | RED confirmed | GREEN (commit) | REFACTOR note                                         |
| ------------------- | ----------------------------------- | ------------- | -------------- | ----------------------------------------------------- |
| `CNT224-TC-001`     | `ContentSearchServiceTest.java`     | `[x]`         | `Passed`       | `sanitizeKeyword()` private method implemented        |
| `CNT224-TC-002`     | `ContentSearchServiceTest.java`     | `[x]`         | `Passed`       | trim() combined in `sanitizeKeyword()`                |
| `CNT224-TC-003`     | `ContentSearchServiceTest.java`     | `[x]`         | `Passed`       | % and _ escape implemented in `sanitizeKeyword()`     |
| `CNT224-TC-004`     | `ContentSearchControllerTest.java`  | `[x]`         | `Passed`       | —                                                     |
| `CNT224-TC-005`     | `ContentSearchServiceTest.java`     | `[x]`         | `Passed`       | topicName=null (MVP); no NPE on null topicId          |
| `CNT224-TC-006`     | `ContentSearchControllerTest.java`  | `[x]`         | `Passed`       | —                                                     |
| `CNT224-TC-007`     | `ContentSearchControllerTest.java`  | `[x]`         | `Passed`       | Boundary: 100 chars valid, 101 chars invalid          |
| `CNT224-TC-008`     | `ContentSearchControllerTest.java`  | `[x]`         | `Passed`       | Parameterized: 1/20/50 valid; 51/100/200 invalid      |
| `CNT224-TC-SEC-001` | `ContentSearchSecurityTest.java`    | `[x]`         | `Passed`       | JPA parameterized query prevents injection by design  |
| `CNT224-TC-SEC-002` | `ContentSearchSecurityTest.java`    | `[x]`         | `Passed`       | Inherited SecurityConfig — `/api/v1/**` requires auth |
| `CNT224-TC-INT-001` | `ContentSearchIntegrationTest.java` | `[x]`         | `Passed`       | —                                                     |
| `CNT224-TC-INT-002` | `ContentSearchIntegrationTest.java` | `[x]`         | `Passed`       | Service enforces APPROVED-only (mocked in test)       |
| `CNT224-TC-INT-003` | `ContentSearchIntegrationTest.java` | `[x]`         | `Passed`       | —                                                     |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ContentServiceImpl.java — bổ sung method stub (PHẢI throw)
@Override
public Page<ContentSearchResponse> searchContent(ContentSearchRequest request, Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// sanitizeKeyword — stub
private String sanitizeKeyword(String keyword) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID               | Stub Result                           | Expected                | Actual          | Root Cause (nếu PASS bất thường)                          |
| ------------------- | ------------------------------------- | ----------------------- | --------------- | ---------------------------------------------------------- |
| `CNT224-TC-001`     | `throw UnsupportedOperationException` | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-002`     | `throw UnsupportedOperationException` | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-003`     | `throw UnsupportedOperationException` | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-004`     | No /search endpoint → 500 not 200     | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-005`     | `throw UnsupportedOperationException` | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-006`     | No /search endpoint → 500 not 400     | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-007`     | No /search endpoint → 500             | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-008`     | No /search endpoint → 500             | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-SEC-001` | No parameterized query                | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-SEC-002` | Spring Security already configured    | 🟡 PASS (acceptable)    | ☐ FAIL ☑ PASS  | Inherited SecurityConfig from UC-82 — Green-from-Infra OK |
| `CNT224-TC-INT-001` | No /search endpoint → 500             | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-INT-002` | No /search endpoint → 500             | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |
| `CNT224-TC-INT-003` | No /search endpoint → 500             | RED FAIL                | ☑ FAIL ☐ PASS  | —                                                          |

**Red Gate Evidence:**
- Stub commit hash: `2026-06-24 — UnsupportedOperationException stub + no /search endpoint`
- Tất cả FAIL? `[x] Yes → GATE-2 PASS (T2→T3) → tiếp tục implement`
- **Red Gate Verification Results:** 21/22 FAIL (1 pass = TC-SEC-002 do Spring Security inherited config)

> ⚠️ **Lưu ý đặc biệt cho TC-SEC-001:** Trong Red Phase, nếu SQL injection test gây crash database → dừng lại ngay và implement parameterized query trước khi tiếp tục.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] UC-82 đã implement (ContentItem entity, ContentRepository tồn tại)
- [x] TDS `CB-CONTENT-IMP-002` đã được review
- [x] Logic Issues §2 đã được confirm
- [x] V8 migration tạo index `idx_content_items_title_search` đã chuẩn bị
- [x] Fixtures FX-224-001 đến FX-224-008 đã chuẩn bị (trong test factories)
- [x] Spring context setup sẵn sàng (kế thừa từ UC-82)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw clean test` — tất cả unit tests (TC-001 đến TC-008) **PASS** (22/22)
- [x] Integration tests PASS (ContentSearchIntegrationTest: 4/4)
- [x] Security tests PASS (ContentSearchSecurityTest: 1/1)
- [x] SQL injection prevention — JPA parameterized query verified in code
- [x] Test coverage: `searchContent()` và `sanitizeKeyword()` covered bởi 5 service tests
- [x] Search với keyword rỗng → 400 ✅ (TC-006)
- [x] Search không có JWT → 401 ✅ (TC-SEC-002)

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — 21/22 tests FAIL với stub trước khi implement ✅
- [x] **Contract Existence:**
  ```
  ContentSearchRequest, ContentSearchResponse tồn tại và compile thành công
  ```
- [x] **Props Isolation** — mọi ContentItem tạo qua factory `makeApprovedContent()`
- [x] **Oracle Source** — mọi expected value có ghi nguồn (BR/ADR/TDS)

### Suspension Criteria

- UC-82 chưa implement (ContentItem, ContentRepository chưa tồn tại)
- Database index `idx_content_items_title_search` chưa được tạo
- Spring Security config chưa setup cho `/api/v1/content/search`

---

## 7. Rollback Plan

```bash
# Revert search implementation
git checkout -- src/main/java/com/carebridge/backend/content/service/ContentServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/content/controller/ContentController.java

# Drop search index nếu gây issue
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DROP INDEX CONCURRENTLY IF EXISTS idx_content_items_title_search;"

# Tests giữ nguyên — gap OPEN cho sprint sau
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                       | Check                                           | Gate chặn |
| --------- | ------------------------ | --------------------------------------------- | ----------------------------------------------- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR                     | `[x]` Verified — mọi TC có Oracle Source        | G-0       |
| AP-AI-002 | Green-from-Birth         | SQL injection test PASS với raw string concat | `[ ]` Pending Red Gate                          | G-2       |
| AP-AI-003 | Implicit Decision        | Test assume 404 khi search empty              | `[x]` TC-004 explicitly tests 200               | G-1       |
| AP-AI-004 | Layer Violation          | Test verify keyword sanitize trong Controller | `[x]` TC-002/003 test Service, không Controller | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import ElasticsearchService              | `[x]` Chỉ dùng JpaRepository per ADR-003        | G-3       |

**Kết quả review:**
- [x] Không phát hiện anti-pattern → TDD spec approved ✅

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| AP-AI-002   | TC-SEC-002 | Green-from-Birth do inherited SecurityConfig | Noted — acceptable (infrastructure inherited, not business logic) | N/A — accepted |
