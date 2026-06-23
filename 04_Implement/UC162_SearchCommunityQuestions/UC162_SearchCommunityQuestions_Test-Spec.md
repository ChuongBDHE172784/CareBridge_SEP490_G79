# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-162 Search Community Questions

**Document ID:** `CB-COMMUNITY-TDD-003`
**Version:** `1.0`
**Date:** `2026-06-23`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `CB-COMMUNITY-IMP-003` — TDS UC-162 Search Community Questions
- `SRS 3.3.8.1` — UC-162 functional requirements
- `ADR-COM-007, ADR-COM-008`
- `BR-RBAC`

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                       |
| ---------- | ------------------ | ------------------------------------------------------- |
| 2026-06-23 | AI Agent — Winston | Khởi tạo TDD spec cho UC-162 Search Community Questions |

---

## 1. Thông tin Module

| Field                     | Value                                                   |
| ------------------------- | ------------------------------------------------------- |
| **Feature / UC ID**       | `UC-162`                                                |
| **Module**                | `community — SearchCommunityQuestions`                  |
| **Spec gốc**              | `CB-COMMUNITY-IMP-003`                                  |
| **Priority**              | P1 High (Frequent)                                      |
| **Sprint**                | `S1 (2026-06-23 → 2026-07-06)`                          |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                 |
| **Data Classification**   | `Internal`                                              |
| **Compliance Scope**      | `BR-RBAC`                                               |
| **Upstream Dependencies** | `community.CommunityQuestion, community.CommunityTopic` |
| **Downstream Consumers**  | `Mobile App search results`                             |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                    |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                    |
| **Constraint Source**    | `CB-COMMUNITY-IMP-003 §17`                                                                               |
| **Constraints Injected** | C1 (APPROVED only), C2 (max size=100), C3 (parameterized query), C4 (200 on empty), C5 (isAuthenticated) |
| **Model**                | `claude-sonnet-4-6`                                                                                      |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                             |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                           | Thực tế                                                     | Fix áp dụng trong test                            |
| --- | ------------------------------------------------ | ----------------------------------------------------------- | ------------------------------------------------- |
| L1  | Spec không nói rõ empty search trả gì            | ADR-COM-007: 200 OK với empty content array, không phải 404 | Test verify 200 + content:[] khi không có kết quả |
| L2  | Spec không giới hạn size                         | ADR-COM-008: max size=100                                   | Test verify size=200 bị cap/reject                |
| L3  | Spec không nói PENDING questions có bị lọc không | ADR-COM-007: chỉ APPROVED                                   | Test verify PENDING không xuất hiện trong kết quả |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UC-162 SearchCommunityQuestions bao gồm:
├── Repository Layer (unit — H2 or mock)
│   └── CommunityQuestionRepository.searchApproved()
├── Service Layer (unit — mock repository)
│   └── CommunityQuestionSearchServiceImpl.searchQuestions()
├── Controller Layer (unit — mock service)
│   ├── GET /api/v1/community/questions param parsing
│   └── @PreAuthorize("isAuthenticated()")
└── Integration Layer (Testcontainers PostgreSQL)
    └── Full search flow with real DB
```

### TDS-02 — Test Basis

| Source               | Items Derived                                      |
| -------------------- | -------------------------------------------------- |
| `SRS 3.3.8.1` UC-162 | Search by keyword, topicId, stage, hasExpertAnswer |
| `ADR-COM-007`        | Only APPROVED questions in results                 |
| `ADR-COM-008`        | Pagination max size=100, default page=0 size=20    |
| `BR-RBAC`            | isAuthenticated() required                         |
| `BR-COM-010`         | Paginated results                                  |
| `BR-COM-011`         | Keyword search case-insensitive, in title and body |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                          | Coverage Item                     | Test Cases          |
| ------------ | --------------------------------------- | --------------------------------- | ------------------- |
| TC-COND-001  | Keyword matching — title hit            | `searchApproved()` ILIKE query    | `COM162-TC-001`     |
| TC-COND-002  | No results → 200 with empty content     | Return type                       | `COM162-TC-002`     |
| TC-COND-003  | PENDING questions excluded from results | `WHERE status='APPROVED'`         | `COM162-TC-003`     |
| TC-COND-004  | topicId filter works                    | `AND topicId=:topicId`            | `COM162-TC-004`     |
| TC-COND-005  | stage filter works                      | `AND stage=:stage`                | `COM162-TC-005`     |
| TC-COND-006  | hasExpertAnswer=true filter works       | expert-labeled subquery           | `COM162-TC-006`     |
| TC-COND-007  | size > 100 is rejected/capped           | Max size enforcement              | `COM162-TC-007`     |
| TC-COND-008  | Pagination: page=1 returns second page  | `Pageable.ofSize(20).withPage(1)` | `COM162-TC-008`     |
| TC-COND-009  | No JWT → 401                            | Spring Security                   | `COM162-TC-SEC-001` |
| TC-COND-010  | SQL injection in keyword                | Parameterized query               | `COM162-TC-SEC-002` |

### TDS-04 — Test Techniques

| Technique                | Applied To                                                | Rationale                      |
| ------------------------ | --------------------------------------------------------- | ------------------------------ |
| Equivalence Partitioning | keyword (null, empty, valid, XSS), filters (null vs. set) | Cover input classes            |
| Boundary Value Analysis  | size (1, 20, 100, 101), page (0, 1, last)                 | Pagination boundaries          |
| State Transition Testing | QuestionStatus (APPROVED shown, others hidden)            | Status filter correctness      |
| Error Guessing           | SQL injection in keyword                                  | Security — parameterized query |

### TDS-05 — Test Data Requirements

| Fixture ID      | Type    | Value / Logic                                            | Mục đích                        |
| --------------- | ------- | -------------------------------------------------------- | ------------------------------- |
| `FX-COM162-001` | DB seed | 5 APPROVED questions, 2 với keyword "thai nhi" in title  | Keyword search test             |
| `FX-COM162-002` | DB seed | 2 PENDING questions                                      | Verify exclusion from results   |
| `FX-COM162-003` | DB seed | 3 questions with stage=PREGNANCY, 2 with stage=BABY_CARE | Stage filter test               |
| `FX-COM162-004` | DB seed | 2 questions with expert-labeled answers                  | hasExpertAnswer filter          |
| `FX-COM162-005` | JWT     | `{sub: "user-001", roles: ["ROLE_MOTHER"]}`              | Authenticated user              |
| `FX-COM162-006` | DB seed | 25 APPROVED questions                                    | Pagination test (2 pages of 20) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
private CommunityQuestionSearchRequest makeSearchRequest() {
    CommunityQuestionSearchRequest req = new CommunityQuestionSearchRequest();
    req.setPage(0);
    req.setSize(20);
    return req;
}

private CommunityQuestionSearchRequest makeSearchRequest(
        Consumer<CommunityQuestionSearchRequest> overrides) {
    CommunityQuestionSearchRequest req = makeSearchRequest();
    overrides.accept(req);
    return req;
}

private CommunityQuestion makeApprovedQuestion(String title) {
    return CommunityQuestion.builder()
        .id(UUID.randomUUID())
        .title(title)
        .status(QuestionStatus.APPROVED)
        .build();
}
```

---

### COM162-TC-001 — Keyword search trả về matching APPROVED questions

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionSearchServiceImpl.searchQuestions()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionSearchServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-COM-011, ADR-COM-007`

```java
@Test
void searchQuestions_withKeyword_returnsMatchingApprovedOnly() {
    // Arrange
    Page<CommunityQuestion> page = new PageImpl<>(List.of(
        makeApprovedQuestion("Thai nhi 28 tuần đạp"),
        makeApprovedQuestion("Thai nhi 20 tuần phát triển")
    ), PageRequest.of(0, 20), 2L);

    when(questionRepository.searchApproved(
        eq("thai nhi"), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(page);

    // Act
    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> r.setKeyword("thai nhi")));

    // Assert
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2L);
    assertThat(result.getPage()).isZero();
}
```

**Current Status:** RED

---

### COM162-TC-002 — Không có kết quả → 200 với empty content

**Severity:** `MEDIUM`
**Feature Under Test:** Return type when no results
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-007`

```java
@Test
void searchQuestions_noResults_returns200WithEmptyContent() {
    when(questionRepository.searchApproved(any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());

    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> r.setKeyword("xyz_nonexistent")));

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();
    // Not throwing exception — 200 OK
}
```

**Current Status:** RED

---

### COM162-TC-003 — PENDING questions không xuất hiện trong kết quả

**Severity:** `HIGH`
**Feature Under Test:** `searchApproved()` status filter
**Test File:** Integration test
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-007`

```java
@Test
void searchQuestions_pendingQuestionsExcluded() {
    // Seed: 2 APPROVED + 2 PENDING
    seedApprovedQuestion("Approved title one");
    seedApprovedQuestion("Approved title two");
    seedQuestion("Pending question", QuestionStatus.PENDING);
    seedQuestion("Another pending", QuestionStatus.PENDING);

    // Act — no keyword filter
    MvcResult result = mockMvc.perform(get("/api/v1/community/questions")
            .header("Authorization", "Bearer " + userJwt))
        .andExpect(status().isOk())
        .andReturn();

    Integer totalElements = JsonPath.read(result.getResponse().getContentAsString(), "$.totalElements");
    assertThat(totalElements).isEqualTo(2);  // Only APPROVED ones
}
```

**Current Status:** RED

---

### COM162-TC-004 — topicId filter

**Severity:** `MEDIUM`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`

```java
@Test
void searchQuestions_topicIdFilter_returnsOnlyMatchingTopic() {
    UUID topicA = UUID.randomUUID();
    UUID topicB = UUID.randomUUID();

    Page<CommunityQuestion> filtered = new PageImpl<>(List.of(
        CommunityQuestion.builder().topicId(topicA).status(QuestionStatus.APPROVED).build()
    ), PageRequest.of(0, 20), 1L);

    when(questionRepository.searchApproved(isNull(), eq(topicA), isNull(), isNull(), any()))
        .thenReturn(filtered);

    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> r.setTopicId(topicA)));

    assertThat(result.getTotalElements()).isEqualTo(1L);
}
```

**Current Status:** RED

---

### COM162-TC-005 — stage filter

**Severity:** `MEDIUM`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`

```java
@Test
void searchQuestions_stageFilter_returnsOnlyMatchingStage() {
    // Seed: 3 PREGNANCY, 2 BABY_CARE (all APPROVED)
    // Filter: stage=PREGNANCY
    // Expect: 3 results
    when(questionRepository.searchApproved(isNull(), isNull(), eq("PREGNANCY"), isNull(), any()))
        .thenReturn(new PageImpl<>(List.of(
            makeApprovedQuestion("P1"), makeApprovedQuestion("P2"), makeApprovedQuestion("P3")
        ), PageRequest.of(0, 20), 3L));

    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> r.setStage(PregnancyStage.PREGNANCY)));

    assertThat(result.getTotalElements()).isEqualTo(3L);
}
```

**Current Status:** RED

---

### COM162-TC-006 — hasExpertAnswer=true filter

**Severity:** `MEDIUM`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`

```java
@Test
void searchQuestions_hasExpertAnswerTrue_returnsQuestionsWithExpertLabeled() {
    when(questionRepository.searchApproved(isNull(), isNull(), isNull(), eq(true), any()))
        .thenReturn(new PageImpl<>(List.of(
            makeApprovedQuestion("Expert answered Q1"),
            makeApprovedQuestion("Expert answered Q2")
        ), PageRequest.of(0, 20), 2L));

    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> r.setHasExpertAnswer(true)));

    assertThat(result.getTotalElements()).isEqualTo(2L);
    result.getContent().forEach(q ->
        assertThat(q.hasExpertAnswer()).isTrue()
    );
}
```

**Current Status:** RED

---

### COM162-TC-007 — size > 100 bị reject

**Severity:** `MEDIUM`
**Feature Under Test:** `Max size enforcement`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-COM-008`

```java
@Test
void searchQuestions_sizeOver100_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/community/questions")
            .param("size", "200")
            .header("Authorization", "Bearer " + userJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COM-001"));
}
```

**Current Status:** RED

---

### COM162-TC-008 — Pagination: page=1 trả về trang thứ hai

**Severity:** `MEDIUM`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`

```java
@Test
void searchQuestions_page1_returnsSecondPage() {
    // Mock: page 1 of 25 total with size=20 → 5 items
    Page<CommunityQuestion> secondPage = new PageImpl<>(
        List.of(makeApprovedQuestion("Page2-Q1"), makeApprovedQuestion("Page2-Q2")),
        PageRequest.of(1, 20), 25L
    );
    when(questionRepository.searchApproved(any(), any(), any(), any(),
        argThat(p -> p.getPageNumber() == 1))).thenReturn(secondPage);

    PagedResponse<CommunityQuestionSummaryResponse> result =
        service.searchQuestions(makeSearchRequest(r -> { r.setPage(1); r.setSize(20); }));

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getTotalElements()).isEqualTo(25L);
    assertThat(result.getTotalPages()).isEqualTo(2);
}
```

**Current Status:** RED

---

### SECURITY TEST CASES

---

### COM162-TC-SEC-001 — Không có JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**CWE:** `CWE-306`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-009`

```java
@Test
void searchQuestions_noJwt_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/community/questions")
            .param("keyword", "test"))
        .andExpect(status().isUnauthorized());
}
```

**Current Status:** RED

---

### COM162-TC-SEC-002 — SQL injection trong keyword

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainers running
- Seed: FX-COM162-001 (community_questions table populated)
- Valid JWT token

```java
@Test
void searchQuestions_sqlInjectionInKeyword_tableNotDropped() throws Exception {
    String injectionAttempt = "'; DROP TABLE community_questions; --";

    mockMvc.perform(get("/api/v1/community/questions")
            .param("keyword", injectionAttempt)
            .header("Authorization", "Bearer " + userJwt))
        .andExpect(status().isOk());

    // Verify table still exists and has data
    long count = questionRepository.count();
    assertThat(count).isGreaterThan(0);
}
```

**Expected Result (PASS):** 200 OK, table intact — parameterized query prevents injection
**Expected Result (FAIL = lỗ hổng):** Exception or table dropped/empty
**Current Status:** RED

---

### INTEGRATION TEST CASES

---

### COM162-TC-INT-001 — Full search flow với real DB

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/community/questions full stack`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionSearchIntegrationTest.java`
**TDD Phase:** RED

**Preconditions:**
- PostgreSQL Testcontainers
- Seed: FX-COM162-001, FX-COM162-002

```java
@Test
void searchQuestions_fullStack_returnsOnlyApproved() throws Exception {
    // Seed
    seedApprovedQuestion("Thai nhi 28 tuần");
    seedApprovedQuestion("Bé 3 tháng hay quấy đêm");
    seedPendingQuestion("Pending question");

    mockMvc.perform(get("/api/v1/community/questions")
            .header("Authorization", "Bearer " + userJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2));
}

@Test
void searchQuestions_keywordFilter_casInsensitive() throws Exception {
    seedApprovedQuestion("THAI NHI lớn bé");
    seedApprovedQuestion("Bé sơ sinh khỏe mạnh");

    mockMvc.perform(get("/api/v1/community/questions")
            .param("keyword", "thai nhi")  // lowercase
            .header("Authorization", "Bearer " + userJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
}
```

**Current Status:** RED

---

## 5. Red-Green-Refactor Tracker

| TC ID               | Test File                                     | RED confirmed | GREEN (commit) | REFACTOR note     |
| ------------------- | --------------------------------------------- | ------------- | -------------- | ----------------- |
| `COM162-TC-001`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-002`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-003`     | `CommunityQuestionSearchIntegrationTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-004`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-005`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-006`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-007`     | `CommunityQuestionControllerTest.java`        | `[ ]`         | `—`            | —                 |
| `COM162-TC-008`     | `CommunityQuestionSearchServiceImplTest.java` | `[ ]`         | `—`            | —                 |
| `COM162-TC-SEC-001` | `CommunityQuestionControllerTest.java`        | `[ ]`         | `—`            | —                 |
| `COM162-TC-SEC-002` | `CommunityQuestionSearchIntegrationTest.java` | `[ ]`         | `—`            | Critical security |
| `COM162-TC-INT-001` | `CommunityQuestionSearchIntegrationTest.java` | `[ ]`         | `—`            | —                 |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class CommunityQuestionSearchServiceImpl implements CommunityQuestionSearchService {
    @Override
    public PagedResponse<CommunityQuestionSummaryResponse> searchQuestions(
            CommunityQuestionSearchRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS CB-COMMUNITY-IMP-003 reviewed
- [ ] community_questions và community_answers tables exist (UC-54, UC-56 done)
- [ ] community_topics table exists (UC-109)
- [ ] Test fixtures FX-COM162-001 đến FX-COM162-006 ready

### Exit Criteria (DoD)

- [ ] Tất cả unit tests xanh
- [ ] PENDING questions không xuất hiện trong search results (COM162-TC-003 GREEN)
- [ ] SQL injection bị ngăn chặn (COM162-TC-SEC-002 GREEN)
- [ ] Pagination hoạt động đúng (COM162-TC-008 GREEN)
- [ ] size > 100 bị reject (COM162-TC-007 GREEN)

**CASE 2.0 Exit:**
- [ ] Red Gate passed
- [ ] Props Isolation enforced
- [ ] All assertions traceable to ADR/BR

### Suspension Criteria

- community_questions table chưa tồn tại
- PostgreSQL Testcontainers không available

---

## 7. Rollback Plan

```bash
kubectl rollout undo deployment/carebridge-api
# Search endpoint reverts — no DB migration to rollback
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu                                | Check | Gate chặn |
| --------- | --------------------- | --------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Gen     | Query không filter status=APPROVED      | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth      | COM162-TC-SEC-002 PASS với empty stub   | ☐     | G-2       |
| AP-AI-003 | Implicit Decision     | Code return 404 khi no results          | ☐     | G-1       |
| AP-AI-005 | Hallucinated Contract | Test dùng SearchElasticService không có | ☐     | G-3       |

---

*TDD Spec v1.0 — CB-COMMUNITY-TDD-003 — UC-162 Search Community Questions*
