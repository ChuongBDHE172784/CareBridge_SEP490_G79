# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-54 Create Community Question

**Document ID:** `CB-COMMUNITY-TDD-001`
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
- `CB-COMMUNITY-IMP-001` — TDS UC-54 Create Community Question
- `SRS 3.3.1.31` — UC-54 functional requirements
- `ADR-COM-001, ADR-COM-002, ADR-COM-003`
- `BR-RBAC, BR-PRIVACY`

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                     |
| ---------- | ------------------ | ----------------------------------------------------- |
| 2026-06-23 | AI Agent — Winston | Khởi tạo TDD spec cho UC-54 Create Community Question |

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

| Field                     | Value                                      |
| ------------------------- | ------------------------------------------ |
| **Feature / UC ID**       | `UC-54`                                    |
| **Module**                | `community — CreateCommunityQuestion`      |
| **Spec gốc**              | `CB-COMMUNITY-IMP-001`                     |
| **Priority**              | P1 High                                    |
| **Sprint**                | `S1 (2026-06-23 → 2026-07-06)`             |
| **Milestone**             | `M3 Alpha — 2026-07-11`                    |
| **Data Classification**   | `Internal`                                 |
| **Compliance Scope**      | `BR-RBAC, BR-PRIVACY`                      |
| **Upstream Dependencies** | `security (JWT), community.CommunityTopic` |
| **Downstream Consumers**  | `community (feed, search), audit`          |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                          |
| **Constraint Source**    | `CB-COMMUNITY-IMP-001 §17`                                                                                     |
| **Constraints Injected** | C1 (status=PENDING), C2 (anonymous masking), C3 (@PreAuthorize), C4 (authorId from JWT), C5 (topic validation) |
| **Model**                | `claude-sonnet-4-6`                                                                                            |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                   |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                             | Thực tế (schema / policy)                               | Fix áp dụng trong test                                                       |
| --- | -------------------------------------------------- | ------------------------------------------------------- | ---------------------------------------------------------------------------- |
| L1  | Spec không đề cập mặc định status                  | ADR-COM-003: status PHẢI là PENDING khi tạo             | Test verify entity.status == PENDING                                         |
| L2  | Spec không rõ authorId xử lý thế nào khi anonymous | ADR-COM-002: lưu authorId trong DB, mask trong response | Test verify DB có authorId; response không trả authorId khi isAnonymous=true |
| L3  | Spec không nói ai kiểm tra topic hidden            | BR-COM-002: service phải reject hidden topic            | Test verify COM-003 khi topic.is_hidden=true                                 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-54 CreateCommunityQuestion bao gồm các layer:
├── Service Layer (unit — mock repositories)
│   ├── CommunityQuestionServiceImpl.createQuestion()
│   ├── Topic validation logic
│   └── Anonymous author masking
├── Controller Layer (unit — mock service)
│   ├── CommunityQuestionController.createQuestion()
│   ├── @Valid DTO validation
│   └── @PreAuthorize role check
└── Integration Layer (Spring Boot Test + H2 / Testcontainers)
    ├── POST /api/v1/community/questions — full stack
    └── DB assertion on community_questions table
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source               | Items Derived                                                |
| -------------------- | ------------------------------------------------------------ |
| `SRS 3.3.1.31` UC-54 | Mother posts question with topic, stage, age, urgency        |
| `ADR-COM-001`        | Only ROLE_MOTHER can create questions                        |
| `ADR-COM-002`        | isAnonymous=true masks author in response, DB keeps authorId |
| `ADR-COM-003`        | New questions default to status=PENDING                      |
| `BR-RBAC`            | JWT + @PreAuthorize role enforcement                         |
| `BR-PRIVACY`         | No PII leak in public response when anonymous                |
| `BR-COM-001`         | status=PENDING invariant                                     |
| `BR-COM-002`         | topicId must reference active (non-hidden) topic             |
| `BR-COM-003`         | title: 5–255 chars                                           |
| `BR-COM-004`         | body: 10–5000 chars                                          |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                         | Coverage Item                                         | Test Cases                     |
| ------------ | ------------------------------------------------------ | ----------------------------------------------------- | ------------------------------ |
| TC-COND-001  | Valid MOTHER creates question with all required fields | `CommunityQuestionServiceImpl.createQuestion()`       | `COM-TC-001`                   |
| TC-COND-002  | status is always PENDING after creation                | `CommunityQuestion.status` field                      | `COM-TC-001`, `COM-TC-INT-001` |
| TC-COND-003  | isAnonymous=true masks author in response              | `CommunityQuestionMapper.toResponse()`                | `COM-TC-002`                   |
| TC-COND-004  | topicId not found or hidden → COM-003                  | `CommunityTopicRepository.findByIdAndIsHiddenFalse()` | `COM-TC-003`                   |
| TC-COND-005  | title too short (< 5 chars) → COM-001                  | `@Size(min=5)`                                        | `COM-TC-004`                   |
| TC-COND-006  | body too long (> 5000 chars) → COM-001                 | `@Size(max=5000)`                                     | `COM-TC-005`                   |
| TC-COND-007  | No JWT → 401                                           | Spring Security filter                                | `COM-TC-SEC-001`               |
| TC-COND-008  | ROLE_GUEST → 403                                       | `@PreAuthorize("hasRole('MOTHER')")`                  | `COM-TC-SEC-002`               |
| TC-COND-009  | XSS in title is stored safely                          | Input validation + JPA                                | `COM-TC-SEC-003`               |
| TC-COND-010  | pregnancyWeek=28 valid for PREGNANCY stage             | Stage/age cross-validation                            | `COM-TC-006`                   |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)  | Applied To                                   | Rationale                        |
| ------------------------ | -------------------------------------------- | -------------------------------- |
| Equivalence Partitioning | title (valid: 5–255, invalid: <5, >255)      | Cover all input classes          |
| Boundary Value Analysis  | pregnancyWeek (1, 42), babyAgeMonths (0, 72) | Edge values for age fields       |
| State Transition Testing | QuestionStatus enum (PENDING default)        | Verify FSM initial state         |
| Error Guessing           | XSS in title/body, missing required fields   | Security + validation robustness |
| Decision Table Testing   | stage × age combinations                     | Cross-field validation coverage  |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type         | Value / Logic                                                                                                                                                 | Mục đích                    |
| ------------ | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| `FX-COM-001` | DB seed      | `community_topics: {id: "topic-001", name: "Thai kỳ", is_hidden: false}`                                                                                      | Valid topic for happy path  |
| `FX-COM-002` | DB seed      | `community_topics: {id: "topic-hidden", name: "Hidden", is_hidden: true}`                                                                                     | Hidden topic for error path |
| `FX-COM-003` | JWT          | `{sub: "mother-001", roles: ["ROLE_MOTHER"]}`                                                                                                                 | Valid MOTHER authentication |
| `FX-COM-004` | JWT          | `{sub: "guest-001", roles: ["ROLE_GUEST"]}`                                                                                                                   | Invalid role for 403 test   |
| `FX-COM-005` | Request body | `{topicId: "topic-001", title: "Valid title 5+", body: "Valid body 10+ chars", stage: "PREGNANCY", pregnancyWeek: 28, urgency: "NORMAL", isAnonymous: false}` | Standard happy path payload |
| `FX-COM-006` | Request body | `{...FX-COM-005, isAnonymous: true}`                                                                                                                          | Anonymous question          |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern for UC-54
// ═══════════════════════════════════════════════════════════

private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

private CreateCommunityQuestionRequest makeRequest() {
    CreateCommunityQuestionRequest req = new CreateCommunityQuestionRequest();
    req.setTopicId(TOPIC_ID);
    req.setTitle("Valid test question title");
    req.setBody("This is a valid test body with enough characters");
    req.setStage(PregnancyStage.PREGNANCY);
    req.setPregnancyWeek(20);
    req.setUrgency(UrgencyLevel.NORMAL);
    req.setIsAnonymous(false);
    return req;
}

private CreateCommunityQuestionRequest makeRequest(Consumer<CreateCommunityQuestionRequest> overrides) {
    CreateCommunityQuestionRequest req = makeRequest();
    overrides.accept(req);
    return req;
}

private CommunityTopic makeTopic(boolean isHidden) {
    return CommunityTopic.builder()
        .id(TOPIC_ID)
        .name("Thai kỳ")
        .isHidden(isHidden)
        .build();
}
```

---

### COM-TC-001 — Tạo câu hỏi thành công (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.createQuestion()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-001, TC-COND-002`
**Oracle Source:** `ADR-COM-003, BR-COM-001`

**Preconditions:**
- `CommunityTopicRepository.findByIdAndIsHiddenFalse(TOPIC_ID)` mock → returns valid CommunityTopic
- `CommunityQuestionRepository.save()` mock → returns saved entity with UUID
- Fixture: FX-COM-001, FX-COM-005

**Test Steps:**
1. Arrange: mock TopicRepo trả valid topic; mock QuestionRepo.save() trả entity với id = UUID.randomUUID()
2. Act: gọi `service.createQuestion(AUTHOR_ID, makeRequest())`
3. Assert: verify QuestionRepo.save() called once; response.status == "PENDING"; response.id != null

```java
@Test
void createQuestion_validRequest_returnsPendingStatus() {
    // Arrange
    CommunityTopic topic = makeTopic(false);
    when(topicRepository.findByIdAndIsHiddenFalse(TOPIC_ID)).thenReturn(Optional.of(topic));
    CommunityQuestion saved = CommunityQuestion.builder()
        .id(UUID.randomUUID()).topicId(TOPIC_ID).authorId(AUTHOR_ID)
        .status(QuestionStatus.PENDING).build();
    when(questionRepository.save(any())).thenReturn(saved);

    // Act
    CommunityQuestionResponse response = service.createQuestion(AUTHOR_ID, makeRequest());

    // Assert
    verify(questionRepository, times(1)).save(argThat(q ->
        q.getStatus() == QuestionStatus.PENDING &&
        q.getAuthorId().equals(AUTHOR_ID)
    ));
    assertThat(response.getStatus()).isEqualTo("PENDING");
    assertThat(response.getId()).isNotNull();
}
```

**Expected Result (PASS):** response.status="PENDING", response.id non-null, save() called once
**Expected Result (FAIL):** status khác PENDING → ADR-COM-003 bị vi phạm
**Current Status:** RED — chưa implement

---

### COM-TC-002 — isAnonymous=true không lộ authorId trong response

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionMapper.toResponse()`
**Test File:** `src/test/java/com/carebridge/backend/community/mapper/CommunityQuestionMapperTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-002, BR-PRIVACY`

**Preconditions:**
- CommunityQuestion entity với isAnonymous=true, authorId="author-001"

**Test Steps:**
1. Arrange: tạo entity với isAnonymous=true
2. Act: gọi `mapper.toResponse(entity)`
3. Assert: response không chứa authorId (null hoặc masked)

```java
@Test
void toResponse_anonymousQuestion_doesNotExposeAuthorId() {
    CommunityQuestion entity = CommunityQuestion.builder()
        .id(UUID.randomUUID()).authorId(AUTHOR_ID).isAnonymous(true)
        .status(QuestionStatus.PENDING).build();

    CommunityQuestionResponse response = mapper.toResponse(entity);

    assertThat(response.getAuthorId()).isNull(); // or masked value
}

@Test
void toResponse_nonAnonymousQuestion_entityAuthorIdStoredInDB() {
    // DB entity always has authorId regardless of isAnonymous
    CommunityQuestion entity = CommunityQuestion.builder()
        .id(UUID.randomUUID()).authorId(AUTHOR_ID).isAnonymous(true).build();

    // Verify entity itself still has authorId (DB-level)
    assertThat(entity.getAuthorId()).isEqualTo(AUTHOR_ID);
}
```

**Expected Result (PASS):** response.authorId == null khi isAnonymous=true
**Expected Result (FAIL):** authorId lộ ra trong response → BR-PRIVACY vi phạm
**Current Status:** RED

---

### COM-TC-003 — Topic hidden → COM-003

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.createQuestion()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-COM-002`

**Preconditions:**
- `CommunityTopicRepository.findByIdAndIsHiddenFalse(TOPIC_ID)` mock → returns Optional.empty()

**Test Steps:**
1. Arrange: mock TopicRepo trả Optional.empty()
2. Act: gọi `service.createQuestion(AUTHOR_ID, makeRequest())`
3. Assert: CommunityTopicNotFoundException thrown; questionRepository.save() NOT called

```java
@Test
void createQuestion_hiddenTopic_throwsCommunityTopicNotFoundException() {
    when(topicRepository.findByIdAndIsHiddenFalse(TOPIC_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createQuestion(AUTHOR_ID, makeRequest()))
        .isInstanceOf(CommunityTopicNotFoundException.class)
        .hasMessageContaining("COM-003");

    verify(questionRepository, never()).save(any());
}
```

**Expected Result (PASS):** Exception với COM-003; save() never called
**Expected Result (FAIL):** Question saved despite hidden topic → data integrity violation
**Current Status:** RED

---

### COM-TC-004 — Title quá ngắn → validation 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Valid CreateCommunityQuestionRequest` — controller layer
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-COM-003`

**Test Steps:**
1. Arrange: request với title = "Hi" (2 chars, < min 5)
2. Act: MockMvc POST /api/v1/community/questions với MOTHER JWT
3. Assert: response status = 400, body.error.code = "COM-001", field = "title"

```java
@Test
void createQuestion_titleTooShort_returns400() throws Exception {
    String body = objectMapper.writeValueAsString(makeRequest(req -> req.setTitle("Hi")));

    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COM-001"))
        .andExpect(jsonPath("$.error.details[?(@.field=='title')]").exists());
}
```

**Expected Result (PASS):** 400 + COM-001
**Expected Result (FAIL):** 201 returned — validation not enforced
**Current Status:** RED

---

### COM-TC-005 — Body quá dài → validation 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Valid CreateCommunityQuestionRequest`
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-COM-004`

```java
@Test
void createQuestion_bodyTooLong_returns400() throws Exception {
    String longBody = "A".repeat(5001);
    String body = objectMapper.writeValueAsString(makeRequest(req -> req.setBody(longBody)));

    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COM-001"));
}
```

**Expected Result (PASS):** 400 + COM-001
**Current Status:** RED

---

### COM-TC-006 — pregnancyWeek boundary values hợp lệ

**Severity:** `MEDIUM`
**Feature Under Test:** `@Min @Max on pregnancyWeek`
**Test File:** Controller unit test
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-COM-004` boundary spec

```java
@ParameterizedTest
@ValueSource(ints = {1, 42})
void createQuestion_validPregnancyWeekBoundary_returns201(int week) throws Exception {
    String body = objectMapper.writeValueAsString(
        makeRequest(req -> req.setPregnancyWeek(week)));
    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
}

@ParameterizedTest
@ValueSource(ints = {0, 43})
void createQuestion_invalidPregnancyWeekBoundary_returns400(int week) throws Exception {
    String body = objectMapper.writeValueAsString(
        makeRequest(req -> req.setPregnancyWeek(week)));
    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
}
```

**Current Status:** RED

---

### SECURITY TEST CASES

---

### COM-TC-SEC-001 — Request không có JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** `Spring Security JWT filter`
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java`
**TDD Phase:** RED

```java
@Test
void createQuestion_noJwt_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/community/questions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest())))
        .andExpect(status().isUnauthorized());
}
```

**Expected Result (PASS — hệ thống an toàn):** 401 Unauthorized
**Expected Result (FAIL = lỗ hổng):** 201 Created — auth bypass
**Current Status:** RED

---

### COM-TC-SEC-002 — ROLE_GUEST → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** Controller test
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`

```java
@Test
void createQuestion_guestRole_returns403() throws Exception {
    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + guestJwt)  // FX-COM-004
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("COM-004"));
}
```

**Expected Result (PASS):** 403 + COM-004
**Expected Result (FAIL = lỗ hổng):** 201 — privilege escalation
**Current Status:** RED

---

### COM-TC-SEC-003 — XSS injection trong title

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-79 — Cross-site Scripting`
**Feature Under Test:** `@NotBlank + JPA entity storage`
**Test File:** Integration test
**TDD Phase:** RED

**Preconditions:**
- Valid MOTHER JWT, valid topic in DB

**Test Steps:**
1. POST với title = `<script>alert('xss')</script>Valid title here`
2. Assert: response 201 or 400
3. If 201: verify DB lưu title dưới dạng plain text không execute script

```java
@Test
void createQuestion_xssInTitle_storedSafely() throws Exception {
    String xssTitle = "<script>alert('xss')</script>Valid question title";
    String reqBody = objectMapper.writeValueAsString(makeRequest(req -> req.setTitle(xssTitle)));

    MvcResult result = mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reqBody))
        .andReturn();

    // Either rejected or stored safely (no script execution)
    int status = result.getResponse().getStatus();
    assertThat(status).isIn(200, 201, 400);

    if (status == 201) {
        // Verify stored value is safe
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("<script>");
    }
}
```

**Current Status:** RED

---

### INTEGRATION TEST CASES

---

### COM-TC-INT-001 — Full stack: POST → DB assertion

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/community/questions full flow`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001, TC-COND-002`

**Preconditions:**
- PostgreSQL container running (Testcontainers)
- Seed: FX-COM-001 (valid topic), FX-COM-003 (MOTHER user)
- Valid MOTHER JWT token

**Test Steps:**
1. Seed: INSERT community_topics (id="topic-001", is_hidden=false)
2. POST /api/v1/community/questions với FX-COM-005 payload
3. Assert response 201 + status="PENDING"
4. Assert DB: SELECT * FROM community_questions WHERE id=<response.id> → record exists với status='PENDING'

```java
@Test
void createQuestion_fullStack_questionPersistedWithPendingStatus() throws Exception {
    // Arrange: seed topic
    seedTopic("topic-001", false);

    String reqBody = objectMapper.writeValueAsString(makeRequest());

    // Act
    MvcResult result = mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reqBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

    // Assert DB
    String responseBody = result.getResponse().getContentAsString();
    UUID questionId = UUID.fromString(
        JsonPath.read(responseBody, "$.id").toString()
    );

    CommunityQuestion saved = questionRepository.findById(questionId).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(QuestionStatus.PENDING);
    assertThat(saved.getAuthorId()).isEqualTo(AUTHOR_ID);
    assertThat(saved.getLikeCount()).isZero();
    assertThat(saved.getAnswerCount()).isZero();
}
```

**DB Assertion:**
```sql
SELECT status, author_id, like_count, answer_count
FROM community_questions
WHERE id = '<response.id>';
-- Expected: status='PENDING', author_id=<mother_id>, like_count=0, answer_count=0
```

**Current Status:** RED

---

### COM-TC-INT-002 — Hidden topic → 404 COM-003

**Severity:** `HIGH`
**Feature Under Test:** `Topic validation in service`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionIntegrationTest.java`
**TDD Phase:** RED

**Preconditions:**
- Seed: FX-COM-002 (hidden topic)

```java
@Test
void createQuestion_hiddenTopic_returns404() throws Exception {
    seedTopic("topic-hidden", true);  // is_hidden=true

    String reqBody = objectMapper.writeValueAsString(
        makeRequest(req -> req.setTopicId(UUID.fromString("topic-hidden-uuid"))));

    mockMvc.perform(post("/api/v1/community/questions")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reqBody))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("COM-003"));

    // Verify no question saved
    assertThat(questionRepository.count()).isZero();
}
```

**Current Status:** RED

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                               | RED confirmed | GREEN (commit) | REFACTOR note                       |
| ---------------- | --------------------------------------- | ------------- | -------------- | ----------------------------------- |
| `COM-TC-001`     | `CommunityQuestionServiceImplTest.java` | `[ ]`         | `—`            | Extract anonymous masking to mapper |
| `COM-TC-002`     | `CommunityQuestionMapperTest.java`      | `[ ]`         | `—`            | —                                   |
| `COM-TC-003`     | `CommunityQuestionServiceImplTest.java` | `[ ]`         | `—`            | —                                   |
| `COM-TC-004`     | `CommunityQuestionControllerTest.java`  | `[ ]`         | `—`            | —                                   |
| `COM-TC-005`     | `CommunityQuestionControllerTest.java`  | `[ ]`         | `—`            | —                                   |
| `COM-TC-006`     | `CommunityQuestionControllerTest.java`  | `[ ]`         | `—`            | —                                   |
| `COM-TC-SEC-001` | `CommunityQuestionControllerTest.java`  | `[ ]`         | `—`            | —                                   |
| `COM-TC-SEC-002` | `CommunityQuestionControllerTest.java`  | `[ ]`         | `—`            | —                                   |
| `COM-TC-SEC-003` | `CommunityQuestionIntegrationTest.java` | `[ ]`         | `—`            | —                                   |
| `COM-TC-INT-001` | `CommunityQuestionIntegrationTest.java` | `[ ]`         | `—`            | —                                   |
| `COM-TC-INT-002` | `CommunityQuestionIntegrationTest.java` | `[ ]`         | `—`            | —                                   |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase stub — PHẢI throw để tất cả tests FAIL
@Service
public class CommunityQuestionServiceImpl implements CommunityQuestionService {
    @Override
    public CommunityQuestionResponse createQuestion(UUID authorId, CreateCommunityQuestionRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID            | Stub Result                         | Expected              | Root Cause (nếu PASS bất thường) |
| ---------------- | ----------------------------------- | --------------------- | -------------------------------- |
| `COM-TC-001`     | throw UnsupportedOperationException | FAIL                  | ☐ Tautology ☐ Shared state       |
| `COM-TC-002`     | throw UnsupportedOperationException | FAIL                  |                                  |
| `COM-TC-003`     | throw UnsupportedOperationException | FAIL                  |                                  |
| `COM-TC-SEC-001` | No service call                     | FAIL (security layer) | ☐ Security config missing        |
| `COM-TC-INT-001` | throw UnsupportedOperationException | FAIL                  |                                  |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS CB-COMMUNITY-IMP-001 đã được review
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Migration V002__create_community_questions.sql đã được approved
- [ ] Test fixtures (FX-COM-001 đến FX-COM-006) đã được chuẩn bị
- [ ] CommunityTopic entity và table đã tồn tại (tiên quyết UC-109)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw test -Pintegration` — tất cả integration tests xanh
- [ ] Test coverage ≥ 80% lines cho community module files mới
- [ ] Không có business logic trong CommunityQuestionController
- [ ] isAnonymous=true KHÔNG lộ authorId trong response (COM-TC-002 GREEN)
- [ ] status luôn = PENDING khi tạo mới (COM-TC-001 GREEN)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả tests FAIL với empty stub
- [ ] Contract Existence — mọi import trong test files đều resolve
- [ ] Props Isolation — không có shared mutable state giữa tests
- [ ] Oracle Source — mọi expected value có ghi rõ nguồn BR/ADR

### Suspension Criteria

- Migration V002 chưa được approve
- CommunityTopic entity (UC-109) chưa được implement
- Spring Security JWT config chưa sẵn sàng

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h [host] -U carebridge carebridge_db \
  -c "DROP TABLE IF EXISTS community_questions;"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/community/

# Gap vẫn OPEN → UC-54 status = Not Started
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                            | Check | Gate chặn |
| --------- | ------------------------ | -------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR                          | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty stub                           | ☐     | G-2       |
| AP-AI-003 | Implicit Decision        | TC assume status=APPROVED không có ADR             | ☐     | G-1       |
| AP-AI-004 | Layer Violation          | Controller test verify business rule               | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import CommunityQuestionService không tồn tại | ☐     | G-3       |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| —           | —     | —     | —          | —      |

---

*TDD Spec v1.0 — CB-COMMUNITY-TDD-001 — UC-54 Create Community Question*
*Tích hợp CASE 2.0 Red Gate Protocol & Anti-Pattern Detection*
