# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử — UC-13 Search and Filter

**Document ID:** `CB-SEARCH-IMP-013-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] N/A — không xử lý PII trực tiếp`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC13_SearchAndFilter/UC13_SearchAndFilter_TDS.md` (CB-SEARCH-IMP-013 v1.0)
- `01_Requirements/SRS.md §3.1.1.13`
- `08_References/Template/PHASE-4_Test-Spec.md`

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL → implement → PASS → refactor.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                        |
| ---------- | --------------- | -------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-13 Search and Filter           |

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

| Field                    | Value                                                        |
| ------------------------ | ------------------------------------------------------------ |
| **Feature / Gap ID**     | `UC-13`                                                      |
| **Module**               | `Search and Filter — search (cross-cutting)`                 |
| **Spec gốc**             | `CB-SEARCH-IMP-013`                                          |
| **Priority**             | P1 (High)                                                    |
| **Sprint**               | `S3 (2026-06-26 → 2026-07-09)`                              |
| **Milestone**            | `M3 Alpha — 2026-07-11`                                      |
| **Data Classification**  | `Internal`                                                   |
| **Compliance Scope**     | `N/A`                                                        |
| **Upstream Dependencies**| `security (JWT)`, `community`, `content`, `expert-profile`, `profile` |
| **Downstream Consumers** | `Mobile UI (Flutter)`, `Web UI (React)`                     |

### 1.1 AI Generation Context (CASE 2.0)

| Field                 | Value                                                        |
| --------------------- | ------------------------------------------------------------ |
| **AI Assisted?**      | `Yes`                                                        |
| **Constraint Source** | `CB-SEARCH-IMP-013 §17`, `ADR-001`, `ADR-002`               |
| **Constraints Injected** | C1 (userId từ JWT), C2 (bind params), C3 (snippet strip), C4 (visibility filter), C6 (no Redis/ES) |
| **Model**             | `Claude Sonnet 4.6`                                          |
| **Trust Level**       | `T2 → T3 (pending Red Gate)`                                |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                              | Thực tế (schema / policy)                                         | Fix áp dụng trong test                                    |
|---|-----------------------------------------------------|-------------------------------------------------------------------|----------------------------------------------------------|
| L1| SRS nói "filter by dateRange" cho QUESTION          | Schema hiện tại không có dateRange column riêng — dùng created_at | Test filter dùng created_at (dateFrom, dateTo)           |
| L2| SRS không nói rõ snippet length                     | TDS §8 quy định max 200 ký tự, không chứa HTML                   | Test assert snippet.length() <= 200 và không có `<tag>`  |
| L3| `ReportTargetType` enum hiện chỉ có QUESTION, ANSWER, CONTENT | Search type khác với ReportTargetType — SearchType là enum riêng | Tạo SearchType enum riêng trong `search` package         |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-13 Search and Filter bao gồm các layer:
├── Domain / Enum (SearchType — pure logic, no deps)
├── Application / Use Cases (SearchServiceImpl — mock IDomainSearchProvider)
├── Provider layer (QuestionSearchProvider, ContentSearchProvider, v.v. — mock Repository)
├── Controller (SearchController — mock ISearchService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + real providers với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source              | Items Derived                                                          |
| ------------------- | ---------------------------------------------------------------------- |
| `SRS UC-13`         | Tìm kiếm QUESTION, CONTENT, EXPERT, PROFILE; filter per type           |
| `ADR-SEARCH-001`    | Dùng ILIKE, bind params, không dùng Redis/ES                          |
| `ADR-SEARCH-002`    | Visibility filter per domain                                           |
| `BR-SEARCH-AUTH`    | Chỉ authenticated user mới được tìm kiếm                              |
| `BR-SEARCH-QUERY`   | q: 1–200 ký tự, not blank                                             |
| `BR-SEARCH-PAGE`    | page >= 0, size 1–50                                                   |
| `CB-SEARCH-IMP-013 §10` | Error codes: SEARCH-001, SEARCH-002, SEARCH-003                   |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID   | Test Condition                                             | Coverage Item                             | Test Cases                                      |
| -------------- | ---------------------------------------------------------- | ----------------------------------------- | ----------------------------------------------- |
| TC-COND-001    | Query hợp lệ → trả về kết quả đúng type                  | `SearchServiceImpl.search()`             | `SEARCH-TC-013-001`                             |
| TC-COND-002    | Query rỗng → lỗi SEARCH-001                               | `SearchRequest` validation                | `SEARCH-TC-013-002`                             |
| TC-COND-003    | Type không hợp lệ → lỗi SEARCH-002                        | `SearchType` enum binding                 | `SEARCH-TC-013-003`                             |
| TC-COND-004    | page < 0 → lỗi SEARCH-003                                 | `SearchRequest.page` validation           | `SEARCH-TC-013-004`                             |
| TC-COND-005    | Không có JWT → 401                                         | `SecurityConfig` / `@PreAuthorize`        | `SEARCH-TC-013-005`                             |
| TC-COND-006    | Kết quả QUESTION không chứa DELETED items                 | `QuestionSearchProvider` visibility filter| `SEARCH-TC-013-006`                             |
| TC-COND-007    | Snippet <= 200 ký tự, không chứa HTML                     | `SearchItemResponse.snippet`              | `SEARCH-TC-013-007`                             |
| TC-COND-008    | Filter EXPERT theo specialty                               | `ExpertSearchProvider.search()`           | `SEARCH-TC-013-008`                             |
| TC-COND-009    | SQL Injection attempt → safe                              | ILIKE bind params                         | `SEARCH-TC-013-009` (Security)                  |
| TC-COND-010    | Pagination đúng — page=1 trả về trang 2                   | `PaginationMeta`                          | `SEARCH-TC-013-010` (Integration)               |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4)      | Applied To                              | Rationale                                           |
| ----------------------------- | --------------------------------------- | --------------------------------------------------- |
| Equivalence Partitioning      | `q` length: empty, 1-char, 200-char, 201-char | Kiểm tra boundary của validation rule           |
| Boundary Value Analysis       | `size`: 0, 1, 50, 51                   | Kiểm tra boundary pagination                        |
| Error Guessing                | SQL Injection, XSS trong `q`            | Attack vector phổ biến cho search endpoint          |
| State Transition Testing      | Không áp dụng (stateless)              | —                                                   |
| Decision Table                | type × filter combinations             | Kiểm tra đúng provider được chọn                   |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                           | Mục đích                                  |
| ---------- | ------- | ----------------------------------------------------------------------- | ----------------------------------------- |
| `FX-S-001` | DB seed | 10 community_questions: 5 title chứa "thai kỳ", 5 không; 1 DELETED    | Happy path QUESTION search                |
| `FX-S-002` | DB seed | 4 contents: 3 PUBLISHED title chứa "dinh dưỡng", 1 DRAFT              | Content search visibility filter test     |
| `FX-S-003` | DB seed | 3 expert_profiles: 2 ACTIVE specialty=OBSTETRICS, 1 INACTIVE           | Expert search + specialty filter          |
| `FX-S-004` | DB seed | 2 user_profiles: display_name chứa "Nguyễn"                            | Profile search                            |
| `FX-S-005` | JWT     | `{ sub: 'user-search-001', role: 'ROLE_MOTHER' }`                      | Auth context cho happy path               |
| `FX-S-006` | JWT     | Expired/missing token                                                   | Auth failure test                         |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// SearchTestFactory.java
// Package: com.carebridge.backend.search
class SearchTestFactory {

    static SearchRequest makeSearchRequest() {
        SearchRequest req = new SearchRequest();
        req.setQ("thai kỳ");
        req.setType(SearchType.QUESTION);
        req.setPage(0);
        req.setSize(20);
        return req;
    }

    static SearchRequest makeSearchRequest(Consumer<SearchRequest> overrides) {
        SearchRequest req = makeSearchRequest();
        overrides.accept(req);
        return req;
    }

    static SearchItemResponse makeSearchItem(SearchType type) {
        return SearchItemResponse.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .title("Test Question về thai kỳ")
            .snippet("Mình đang mang thai tuần 28 và...")
            .type(type)
            .metadata(new HashMap<>())
            .createdAt(Instant.now())
            .build();
    }
}
```

---

### SEARCH-TC-013-001 — Tìm kiếm câu hỏi hợp lệ (Happy Path)

**Severity:** `HIGH`
**Feature Under Test:** `SearchServiceImpl.search()` + `QuestionSearchProvider.search()`
**Test File:** `src/test/java/com/carebridge/backend/search/SearchServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-SEARCH-QUERY`, `ADR-001 §Decision`

**Preconditions:**
- Mock `QuestionSearchProvider` trả về Page chứa 5 items với title "thai kỳ"
- Fixture: `FX-S-001`, `FX-S-005`

**Test Steps:**
1. Arrange: tạo `SearchRequest` q="thai kỳ", type=QUESTION, page=0, size=20 qua `SearchTestFactory.makeSearchRequest()`
2. Arrange: mock `QuestionSearchProvider.supports(QUESTION)` → true; mock `.search(...)` → Page với 5 items
3. Act: gọi `searchService.search(request, userId)`
4. Assert: response.type == QUESTION; items.size() == 5; pagination.totalElements == 5

**Expected Result (PASS):**
- `response.getType()` == `SearchType.QUESTION`
- `response.getItems().size()` == 5
- `response.getPagination().getPage()` == 0

**Expected Result (FAIL):**
- `NullPointerException` nếu provider không được inject
- `ClassCastException` nếu response type sai

**Current Status:** RED — Not written
**Implementation Note:** SearchServiceImpl phải iterate providers list và gọi `supports()` để resolve đúng provider.

---

### SEARCH-TC-013-002 — Query rỗng → SEARCH-001

**Severity:** `HIGH`
**Feature Under Test:** `SearchRequest` validation / `SearchController`
**Test File:** `src/test/java/com/carebridge/backend/search/SearchControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-SEARCH-QUERY`, `CB-SEARCH-IMP-013 §10`

**Preconditions:**
- Mock `ISearchService` (không cần gọi)
- Fixture: `FX-S-005`

**Test Steps:**
1. Arrange: tạo request với q="" (blank string)
2. Act: `mockMvc.perform(get("/api/v1/search").param("q", "").param("type", "QUESTION"))` với JWT header
3. Assert: status == 400; body.error.code == "SEARCH-001"

**Expected Result (PASS):**
- HTTP 400
- `error.code` == "SEARCH-001"
- `error.details[0].field` == "q"

**Expected Result (FAIL):**
- HTTP 200 (validation không hoạt động)
- `ISearchService.search()` được gọi với q rỗng

**Current Status:** RED — Not written

---

### SEARCH-TC-013-003 — Type không hợp lệ → SEARCH-002

**Severity:** `HIGH`
**Feature Under Test:** `SearchRequest.type` enum binding
**Test File:** `src/test/java/com/carebridge/backend/search/SearchControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-SEARCH-TYPE`, `CB-SEARCH-IMP-013 §10`

**Preconditions:**
- Fixture: `FX-S-005` (valid JWT)

**Test Steps:**
1. Act: `mockMvc.perform(get("/api/v1/search").param("q", "test").param("type", "INVALID_TYPE"))` với JWT
2. Assert: status == 400; error.code == "SEARCH-002"

**Expected Result (PASS):**
- HTTP 400
- `error.code` == "SEARCH-002"

**Expected Result (FAIL):**
- HTTP 500 (unhandled enum binding error)
- HTTP 200 (type không được validate)

**Current Status:** RED — Not written

---

### SEARCH-TC-013-004 — page < 0 → SEARCH-003

**Severity:** `MEDIUM`
**Feature Under Test:** `SearchRequest.page` validation
**Test File:** `src/test/java/com/carebridge/backend/search/SearchControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SEARCH-PAGE`, `CB-SEARCH-IMP-013 §10`

**Preconditions:**
- Fixture: `FX-S-005`

**Test Steps:**
1. Act: `GET /api/v1/search?q=test&type=QUESTION&page=-1` với JWT
2. Assert: status == 400; error.code == "SEARCH-003"

**Expected Result (PASS):**
- HTTP 400; `error.code` == "SEARCH-003"

**Expected Result (FAIL):**
- HTTP 500 hoặc negative page được accept

**Current Status:** RED — Not written

---

### SEARCH-TC-013-005 — Không có JWT → 401

**Severity:** `CRITICAL`
**Feature Under Test:** Spring Security filter / `@PreAuthorize("isAuthenticated()")`
**Test File:** `src/test/java/com/carebridge/backend/search/SearchControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SEARCH-AUTH`, `CB-SEARCH-IMP-013 §16`

**Preconditions:**
- Không có Authorization header
- Fixture: `FX-S-006`

**Test Steps:**
1. Act: `GET /api/v1/search?q=test&type=QUESTION` (không có Authorization header)
2. Assert: status == 401

**Expected Result (PASS):**
- HTTP 401
- `error.code` == "IAM-001" hoặc Spring Security default

**Expected Result (FAIL):**
- HTTP 200 (endpoint không được bảo vệ)

**Current Status:** RED — Not written

---

### SEARCH-TC-013-006 — Visibility filter: không trả về DELETED questions

**Severity:** `HIGH`
**Feature Under Test:** `QuestionSearchProvider.search()` — visibility filter
**Test File:** `src/test/java/com/carebridge/backend/search/QuestionSearchProviderTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-002 §Decision`

**Preconditions:**
- Fixture: `FX-S-001` — 5 ACTIVE questions title "thai kỳ", 1 DELETED question title "thai kỳ xấu"
- Mock/real `CommunityQuestionRepository`

**Test Steps:**
1. Arrange: seed FX-S-001 trong mock repository
2. Act: gọi `QuestionSearchProvider.search("thai kỳ", filter, userId, Pageable.of(0,20))`
3. Assert: kết quả chứa 5 items; không có item nào có id của DELETED question

**Expected Result (PASS):**
- `page.getTotalElements()` == 5 (DELETED bị exclude)
- Không có item nào với metadata chứa status=DELETED

**Expected Result (FAIL):**
- `page.getTotalElements()` == 6 (bao gồm cả DELETED)

**Current Status:** RED — Not written
**Implementation Note:** Query PHẢI có điều kiện `AND status != 'DELETED'` (hoặc tương đương JPA).

---

### SEARCH-TC-013-007 — Snippet <= 200 ký tự, không chứa HTML

**Severity:** `MEDIUM`
**Feature Under Test:** Snippet generation trong `SearchProvider` implementations
**Test File:** `src/test/java/com/carebridge/backend/search/SearchItemMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-SEARCH-IMP-013 §8.1 (SearchItemResponse.snippet)`

**Preconditions:**
- Question body = "Mình đang <b>mang thai</b> tuần 28. " repeated 20 times (>200 chars)

**Test Steps:**
1. Arrange: tạo `CommunityQuestion` với body chứa HTML và độ dài > 200 ký tự
2. Act: map sang `SearchItemResponse` qua provider mapping logic
3. Assert: `item.getSnippet().length()` <= 200; không có `<` hay `>` trong snippet

**Expected Result (PASS):**
- `snippet.length()` <= 200
- `snippet` không chứa ký tự `<` hoặc `>`

**Expected Result (FAIL):**
- Snippet quá 200 ký tự hoặc chứa HTML tags

**Current Status:** RED — Not written

---

### SEARCH-TC-013-008 — Filter EXPERT theo specialty

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertSearchProvider.search()` với filter specialty
**Test File:** `src/test/java/com/carebridge/backend/search/ExpertSearchProviderTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-SEARCH-FILTER`, `ADR-001`

**Preconditions:**
- Fixture: `FX-S-003` — 2 ACTIVE OBSTETRICS, 1 INACTIVE OBSTETRICS, 1 ACTIVE CARDIOLOGY

**Test Steps:**
1. Arrange: request q="bác sĩ", type=EXPERT, specialty="OBSTETRICS"; seed FX-S-003
2. Act: `ExpertSearchProvider.search("bác sĩ", filter{specialty="OBSTETRICS"}, userId, pageable)`
3. Assert: kết quả chứa đúng 2 items (ACTIVE + OBSTETRICS)

**Expected Result (PASS):**
- `page.getTotalElements()` == 2
- Tất cả items có metadata.specialty == "OBSTETRICS"

**Expected Result (FAIL):**
- Trả về INACTIVE hoặc CARDIOLOGY experts

**Current Status:** RED — Not written

---

### SECURITY TEST CASES

### SEARCH-TC-013-009 — SQL Injection trong query param

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `QuestionSearchProvider.search()` — ILIKE bind params
**Test File:** `src/test/java/com/carebridge/backend/search/SearchSecurityTest.java`
**TDD Phase:** RED

**Preconditions:**
- Fixture: `FX-S-001` (5 questions trong DB)
- Fixture: `FX-S-005` (valid JWT)

**Test Steps (Attack Simulation):**
1. Arrange: q = `"'; DROP TABLE community_questions; --"`
2. Act: `GET /api/v1/search?q=%27%3B+DROP+TABLE+community_questions%3B+--&type=QUESTION` với JWT
3. Assert: HTTP 200 hoặc 400 (query xử lý an toàn); bảng community_questions vẫn tồn tại

**Expected Result (PASS = hệ thống an toàn):**
- Bảng `community_questions` vẫn tồn tại (count > 0)
- Không có exception `DropTableException` hay tương tự
- Response là 200 với 0 kết quả (hoặc 400 nếu special chars bị reject)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Bảng `community_questions` bị xóa
- HTTP 500 với SQL error message trong response

**Current Status:** RED — Not written
**Implementation Note:** PHẢI dùng `@Param` với JPA `@Query` hoặc Spring Data method — KHÔNG nối string.

---

### INTEGRATION TEST CASES

### SEARCH-TC-013-010 — Phân trang đúng (page=1)

**Severity:** `HIGH`
**Feature Under Test:** `SearchServiceImpl.search()` + Pagination — full flow
**Test File:** `src/test/java/com/carebridge/backend/search/SearchIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied
- Seed: 25 community_questions với title chứa "dinh dưỡng" (all ACTIVE)

**Test Steps:**
1. Seed 25 questions với title "dinh dưỡng thai kỳ {i}"
2. Gọi GET `/api/v1/search?q=dinh dưỡng&type=QUESTION&page=0&size=10` với JWT
3. Assert page 0: 10 items, totalElements=25, totalPages=3
4. Gọi GET với `page=1&size=10`
5. Assert page 1: 10 items, và không có item nào trùng id với page 0

**Expected Result (PASS):**
- Page 0: 10 items, pagination.totalElements=25
- Page 1: 10 items, không có id trùng với page 0
- Page 2: 5 items

**Expected Result (FAIL):**
- Trùng lặp item giữa các trang
- totalElements sai

**DB Assertion:**
```java
// Verify total count trong DB
long count = questionRepository.countByTitleContainingIgnoreCase("dinh dưỡng");
assertThat(count).isEqualTo(25);
```

**Current Status:** RED — Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID                   | Test File                                      | RED confirmed | GREEN (commit) | REFACTOR note           |
| ----------------------- | ---------------------------------------------- | ------------- | -------------- | ----------------------- |
| `SEARCH-TC-013-001`     | `SearchServiceImplTest.java`                   | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-002`     | `SearchControllerTest.java`                    | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-003`     | `SearchControllerTest.java`                    | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-004`     | `SearchControllerTest.java`                    | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-005`     | `SearchControllerTest.java`                    | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-006`     | `QuestionSearchProviderTest.java`              | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-007`     | `SearchItemMapperTest.java`                    | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-008`     | `ExpertSearchProviderTest.java`                | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-009`     | `SearchSecurityTest.java`                      | `[ ]`         | `—`            | —                       |
| `SEARCH-TC-013-010`     | `SearchIntegrationTest.java`                   | `[ ]`         | `—`            | —                       |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — SearchServiceImpl stub (PHẢI throw)
@Service
public class SearchServiceImpl implements ISearchService {

    @Override
    public SearchResultResponse search(SearchRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Red Phase — QuestionSearchProvider stub
@Component
public class QuestionSearchProvider implements IDomainSearchProvider {

    @Override
    public boolean supports(SearchType type) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Page<SearchItemResponse> search(String q, SearchRequest filter, UUID userId, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID                | Stub Result                  | Expected   | Actual         | Root Cause (nếu PASS bất thường) |
| -------------------- | ---------------------------- | ---------- | -------------- | --------------------------------- |
| `SEARCH-TC-013-001`  | `throw('Not implemented')`   | FAIL       | ☐ FAIL ☐ PASS | —                                 |
| `SEARCH-TC-013-002`  | `throw('Not implemented')`   | FAIL       | ☐ FAIL ☐ PASS | —                                 |
| `SEARCH-TC-013-003`  | `throw('Not implemented')`   | FAIL       | ☐ FAIL ☐ PASS | —                                 |
| `SEARCH-TC-013-005`  | `throw('Not implemented')`   | FAIL       | ☐ FAIL ☐ PASS | —                                 |
| `SEARCH-TC-013-009`  | `throw('Not implemented')`   | FAIL       | ☐ FAIL ☐ PASS | —                                 |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SEARCH-IMP-013` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Flyway migration `V{n}__add_search_indexes.sql` đã được approved
- [ ] Test fixtures `FX-S-001` đến `FX-S-006` đã được chuẩn bị
- [ ] Các bảng domain hiện có đã sẵn sàng (community_questions, contents, v.v.)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh
- [ ] Test coverage ≥ 80% cho `SearchServiceImpl` và mỗi `SearchProvider`
- [ ] Không có business logic trong `SearchController`
- [ ] Snippet đã được strip HTML và truncate trong mọi provider
- [ ] Visibility filter hoạt động đúng cho mỗi type (DELETED/INACTIVE/DRAFT không xuất hiện)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi test instance tạo qua `SearchTestFactory`
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/ADR)

### Suspension Criteria

- Bảng domain chưa tồn tại (community_questions, contents, expert_profiles)
- CI pipeline bị broken bởi thay đổi khác
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# Revert search indexes
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_community_questions_title;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_contents_title;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_expert_profiles_display_name;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_user_profiles_display_name_search;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/search/
git checkout -- src/test/java/com/carebridge/backend/search/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                         | Check | Gate chặn |
| --------- | ------------------------ | ----------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào        | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty/throw stub (§5.1)            | ☐     | G-2       |
| AP-AI-003 | Implicit Decision        | Test assume Redis/ES không có trong ADR          | ☐     | G-1       |
| AP-AI-004 | Layer Violation          | Test verify SearchController có query logic      | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import service/type không tồn tại           | ☐     | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
