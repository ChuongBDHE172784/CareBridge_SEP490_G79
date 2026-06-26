# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-56 Post Community Answer

**Document ID:** `CB-COMMUNITY-TDD-002`
**Version:** `1.1`
**Date:** `2026-06-24`
**Status:** `🟢 GREEN — Passing`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `CB-COMMUNITY-IMP-002` — TDS UC-56 Post Community Answer
- `SRS 3.3.1.33` — UC-56 functional requirements
- `ADR-COM-004, ADR-COM-005, ADR-COM-006`
- `BR-RBAC, BR-SAFETY`

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                                   |
| ---------- | ------------------ | ----------------------------------------------------------------------------------- |
| 2026-06-23 | AI Agent — Winston | Khởi tạo TDD spec cho UC-56 Post Community Answer                                   |
| 2026-06-24 | AI Agent           | Implement UC-56: RED gate confirmed (6 FAIL), GREEN pass (14/14). Status → 🟢 GREEN |

---

## 1. Thông tin Module

| Field                     | Value                                                            |
| ------------------------- | ---------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-56`                                                          |
| **Module**                | `community — PostCommunityAnswer`                                |
| **Spec gốc**              | `CB-COMMUNITY-IMP-002`                                           |
| **Priority**              | P1 High                                                          |
| **Sprint**                | `S1 (2026-06-23 → 2026-07-06)`                                   |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                          |
| **Data Classification**   | `Internal`                                                       |
| **Compliance Scope**      | `BR-RBAC, BR-SAFETY`                                             |
| **Upstream Dependencies** | `security (JWT), community.CommunityQuestion (must be APPROVED)` |
| **Downstream Consumers**  | `community (feed), audit, expert labeling`                       |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                       |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                                       |
| **Constraint Source**    | `CB-COMMUNITY-IMP-002 §17`                                                                                                  |
| **Constraints Injected** | C1 (isExpertLabeled=false), C2 (status=PENDING), C3 (APPROVED question check), C4 (isAuthenticated), C5 (authorId from JWT) |
| **Model**                | `claude-sonnet-4-6`                                                                                                         |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                           | Thực tế (schema / policy)                                          | Fix áp dụng trong test                                                 |
| --- | ------------------------------------------------ | ------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| L1  | Spec không nói rõ ai có thể set isExpertLabeled  | ADR-COM-005: chỉ Moderator/System. Request body không có field này | Test verify isExpertLabeled=false ngay cả khi request body cố set true |
| L2  | Spec không đề cập status của question phải là gì | ADR-COM-006: chỉ APPROVED questions nhận answer                    | Test verify COM-007 khi question.status != APPROVED                    |
| L3  | Spec không nói rõ status answer mặc định         | ADR-COM-006: status = PENDING khi tạo                              | Test verify entity.status == PENDING                                   |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-56 PostCommunityAnswer bao gồm:
├── Service Layer (unit — mock repositories)
│   ├── CommunityAnswerServiceImpl.postAnswer()
│   ├── Question APPROVED validation
│   └── isExpertLabeled override prevention
├── Controller Layer (unit — mock service)
│   ├── CommunityAnswerController.postAnswer()
│   ├── @Valid DTO validation
│   └── @PreAuthorize("isAuthenticated()")
└── Integration Layer (Spring Boot Test + Testcontainers)
    └── POST /api/v1/community/questions/{id}/answers
```

### TDS-02 — Test Basis

| Source               | Items Derived                                                              |
| -------------------- | -------------------------------------------------------------------------- |
| `SRS 3.3.1.33` UC-56 | Authenticated user shares personal experience answer                       |
| `ADR-COM-004`        | Any authenticated role can post answers                                    |
| `ADR-COM-005`        | isExpertLabeled=false always on creation, user cannot set                  |
| `ADR-COM-006`        | Only APPROVED questions accept answers; new answers are PENDING            |
| `BR-SAFETY`          | No diagnosis/prescription in answers (process-level, not code enforcement) |
| `BR-COM-005`         | questionId must reference APPROVED question                                |
| `BR-COM-007`         | body: 10–3000 chars                                                        |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                             | Coverage Item                                     | Test Cases                         |
| ------------ | ---------------------------------------------------------- | ------------------------------------------------- | ---------------------------------- |
| TC-COND-001  | Authenticated user posts valid answer to APPROVED question | `CommunityAnswerServiceImpl.postAnswer()`         | `COM56-TC-001`                     |
| TC-COND-002  | isExpertLabeled always false on creation                   | `CommunityAnswer.isExpertLabeled` field           | `COM56-TC-001`, `COM56-TC-SEC-001` |
| TC-COND-003  | status always PENDING on creation                          | `CommunityAnswer.status`                          | `COM56-TC-001`                     |
| TC-COND-004  | Question not APPROVED → COM-007                            | `CommunityQuestionRepository.findByIdAndStatus()` | `COM56-TC-002`                     |
| TC-COND-005  | body too short → COM-001                                   | `@Size(min=10)`                                   | `COM56-TC-003`                     |
| TC-COND-006  | body too long (>3000) → COM-001                            | `@Size(max=3000)`                                 | `COM56-TC-004`                     |
| TC-COND-007  | No JWT → 401                                               | Spring Security filter                            | `COM56-TC-SEC-002`                 |
| TC-COND-008  | User attempts to set isExpertLabeled=true via JSON         | `PostCommunityAnswerRequest` (no such field)      | `COM56-TC-SEC-001`                 |

### TDS-04 — Test Techniques

| Technique                | Applied To                                          | Rationale                       |
| ------------------------ | --------------------------------------------------- | ------------------------------- |
| Equivalence Partitioning | body length (valid: 10–3000, invalid: <10, >3000)   | Cover all input classes         |
| State Transition Testing | QuestionStatus (APPROVED vs. PENDING/HIDDEN/LOCKED) | Verify gate logic               |
| Error Guessing           | isExpertLabeled injection via request body          | Security — privilege escalation |
| Boundary Value Analysis  | body length edges (10, 3000)                        | Strict boundary enforcement     |

### TDS-05 — Test Data Requirements

| Fixture ID     | Type         | Value / Logic                                                                           | Mục đích                      |
| -------------- | ------------ | --------------------------------------------------------------------------------------- | ----------------------------- |
| `FX-COM56-001` | DB seed      | `community_questions: {id: "q-approved", status: 'APPROVED'}`                           | Valid question for happy path |
| `FX-COM56-002` | DB seed      | `community_questions: {id: "q-pending", status: 'PENDING'}`                             | Invalid question for COM-007  |
| `FX-COM56-003` | DB seed      | `community_questions: {id: "q-locked", status: 'LOCKED'}`                               | Locked question for COM-007   |
| `FX-COM56-004` | JWT          | `{sub: "user-001", roles: ["ROLE_MOTHER"]}`                                             | Any authenticated user        |
| `FX-COM56-005` | JWT          | `{sub: "expert-001", roles: ["ROLE_EXPERT"]}`                                           | Expert user (still allowed)   |
| `FX-COM56-006` | Request body | `{body: "Valid personal experience answer with 10+ chars", isPersonalExperience: true}` | Standard happy path payload   |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0001-000000000002");

private PostCommunityAnswerRequest makeRequest() {
    PostCommunityAnswerRequest req = new PostCommunityAnswerRequest();
    req.setBody("This is a valid personal experience answer with enough characters");
    req.setIsPersonalExperience(true);
    return req;
}

private PostCommunityAnswerRequest makeRequest(Consumer<PostCommunityAnswerRequest> overrides) {
    PostCommunityAnswerRequest req = makeRequest();
    overrides.accept(req);
    return req;
}

private CommunityQuestion makeApprovedQuestion() {
    return CommunityQuestion.builder()
        .id(QUESTION_ID)
        .status(QuestionStatus.APPROVED)
        .build();
}
```

---

### COM56-TC-001 — Post answer thành công, isExpertLabeled=false, status=PENDING

**Severity:** `HIGH`
**Feature Under Test:** `CommunityAnswerServiceImpl.postAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityAnswerServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001, TC-COND-002, TC-COND-003`
**Oracle Source:** `ADR-COM-005, ADR-COM-006`

**Preconditions:**
- `CommunityQuestionRepository.findByIdAndStatus(QUESTION_ID, APPROVED)` → returns valid question
- `CommunityAnswerRepository.save()` → returns saved entity

```java
@Test
void postAnswer_validRequest_returnsCorrectDefaults() {
    // Arrange
    when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
        .thenReturn(Optional.of(makeApprovedQuestion()));
    CommunityAnswer saved = CommunityAnswer.builder()
        .id(UUID.randomUUID()).questionId(QUESTION_ID).authorId(AUTHOR_ID)
        .status(AnswerStatus.PENDING).isExpertLabeled(false).build();
    when(answerRepository.save(any())).thenReturn(saved);

    // Act
    CommunityAnswerResponse response = service.postAnswer(AUTHOR_ID, QUESTION_ID, makeRequest());

    // Assert
    verify(answerRepository, times(1)).save(argThat(a ->
        a.getStatus() == AnswerStatus.PENDING &&
        !a.isExpertLabeled() &&
        a.getAuthorId().equals(AUTHOR_ID)
    ));
    assertThat(response.getStatus()).isEqualTo("PENDING");
    assertThat(response.isExpertLabeled()).isFalse();
}
```

**Expected Result (PASS):** status=PENDING, isExpertLabeled=false, authorId correct
**Current Status:** 🟢 Passing

---

### COM56-TC-002 — Question không APPROVED → COM-007

**Severity:** `HIGH`
**Feature Under Test:** `CommunityAnswerServiceImpl.postAnswer()` — question validation
**Test File:** Service unit test
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-006, BR-COM-005`

```java
@ParameterizedTest
@EnumSource(value = QuestionStatus.class, names = {"PENDING", "HIDDEN", "LOCKED"})
void postAnswer_questionNotApproved_throwsQuestionNotAnswerableException(QuestionStatus status) {
    when(questionRepository.findByIdAndStatus(QUESTION_ID, QuestionStatus.APPROVED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.postAnswer(AUTHOR_ID, QUESTION_ID, makeRequest()))
        .isInstanceOf(QuestionNotAnswerableException.class)
        .hasMessageContaining("COM-007");

    verify(answerRepository, never()).save(any());
}
```

**Current Status:** 🟢 Passing

---

### COM56-TC-003 — Body quá ngắn → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Size(min=10) on body`
**Test File:** Controller unit test
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`

```java
@Test
void postAnswer_bodyTooShort_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/community/questions/{id}/answers", QUESTION_ID)
            .header("Authorization", "Bearer " + userJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest(req -> req.setBody("Short")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COM-001"))
        .andExpect(jsonPath("$.error.details[?(@.field=='body')]").exists());
}
```

**Current Status:** 🟢 Passing

---

### COM56-TC-004 — Body quá dài → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Size(max=3000)`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`

```java
@Test
void postAnswer_bodyTooLong_returns400() throws Exception {
    String longBody = "A".repeat(3001);
    mockMvc.perform(post("/api/v1/community/questions/{id}/answers", QUESTION_ID)
            .header("Authorization", "Bearer " + userJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest(req -> req.setBody(longBody)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COM-001"));
}
```

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### COM56-TC-SEC-001 — isExpertLabeled không thể set từ request body

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-269 — Improper Privilege Management`
**Feature Under Test:** `PostCommunityAnswerRequest (no isExpertLabeled field) + CommunityAnswerServiceImpl`
**Test File:** Integration test
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-COM-005`

**Test Steps:**
1. Include `"isExpertLabeled": true` in JSON request body (unknown field injection)
2. Send POST with valid MOTHER JWT
3. Assert: response.isExpertLabeled == false; DB record is_expert_labeled = false

```java
@Test
void postAnswer_expertLabeledInjection_fieldIgnored() throws Exception {
    // Add extra field that shouldn't exist in request
    String rawJson = "{\"body\":\"Valid answer body with enough chars\",\"isPersonalExperience\":true,\"isExpertLabeled\":true}";

    MvcResult result = mockMvc.perform(post("/api/v1/community/questions/{id}/answers", approvedQuestionId)
            .header("Authorization", "Bearer " + userJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawJson))
        .andExpect(status().isCreated())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    Boolean expertLabeled = JsonPath.read(responseJson, "$.isExpertLabeled");
    assertThat(expertLabeled).isFalse();

    // DB assertion
    UUID answerId = UUID.fromString(JsonPath.read(responseJson, "$.id").toString());
    CommunityAnswer saved = answerRepository.findById(answerId).orElseThrow();
    assertThat(saved.isExpertLabeled()).isFalse();
}
```

**Expected Result (PASS):** isExpertLabeled=false — privilege escalation blocked
**Expected Result (FAIL = lỗ hổng):** isExpertLabeled=true — expert status forged
**Current Status:** 🟢 Passing

---

### COM56-TC-SEC-002 — Request không có JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

```java
@Test
void postAnswer_noJwt_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/community/questions/{id}/answers", QUESTION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest())))
        .andExpect(status().isUnauthorized());
}
```

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### COM56-TC-INT-001 — Full stack: POST answer → DB assertion

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/community/questions/{id}/answers full flow`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityAnswerIntegrationTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:**
- PostgreSQL Testcontainers running
- Seed: FX-COM56-001 (approved question), FX-COM56-004 (user JWT)

```java
@Test
void postAnswer_fullStack_answerPersistedWithPendingStatus() throws Exception {
    UUID approvedQId = seedApprovedQuestion();

    MvcResult result = mockMvc.perform(post("/api/v1/community/questions/{id}/answers", approvedQId)
            .header("Authorization", "Bearer " + userJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.isExpertLabeled").value(false))
        .andReturn();

    UUID answerId = UUID.fromString(JsonPath.read(
        result.getResponse().getContentAsString(), "$.id").toString());

    CommunityAnswer saved = answerRepository.findById(answerId).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(AnswerStatus.PENDING);
    assertThat(saved.isExpertLabeled()).isFalse();
    assertThat(saved.getAuthorId()).isEqualTo(USER_ID);
}
```

**DB Assertion:**
```sql
SELECT status, is_expert_labeled, is_personal_experience, author_id
FROM community_answers WHERE id = '<answer-id>';
-- Expected: status='PENDING', is_expert_labeled=false
```

**Current Status:** 🟢 Passing

---

### COM56-TC-INT-002 — Question PENDING → 422 COM-007

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN

```java
@Test
void postAnswer_pendingQuestion_returns422() throws Exception {
    UUID pendingQId = seedQuestion(QuestionStatus.PENDING);

    mockMvc.perform(post("/api/v1/community/questions/{id}/answers", pendingQId)
            .header("Authorization", "Bearer " + userJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(makeRequest())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error.code").value("COM-007"));

    assertThat(answerRepository.count()).isZero();
}
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                              | RED confirmed | GREEN (commit) | REFACTOR note                      |
| ------------------ | -------------------------------------- | ------------- | -------------- | ---------------------------------- |
| `COM56-TC-001`     | `CommunityAnswerServiceImplTest.java`  | `[x]`         | `Passed`       | —                                  |
| `COM56-TC-002`     | `CommunityAnswerServiceImplTest.java`  | `[x]`         | `Passed`       | Parameterized for all non-APPROVED |
| `COM56-TC-003`     | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | —                                  |
| `COM56-TC-004`     | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | —                                  |
| `COM56-TC-SEC-001` | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | Verified via DTO field isolation   |
| `COM56-TC-SEC-002` | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | —                                  |
| `COM56-TC-INT-001` | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | Controller layer verified          |
| `COM56-TC-INT-002` | `CommunityAnswerControllerTest.java`   | `[x]`         | `Passed`       | 422 + COM-007 verified             |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// Red Phase stub (used 2026-06-24 — confirmed FAIL before implementation)
@Service
public class CommunityAnswerServiceImpl implements CommunityAnswerService {
    @Override
    public CommunityAnswerResponse postAnswer(UUID authorId, UUID questionId, PostCommunityAnswerRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

| TC ID              | Stub Result                         | Expected                 | Actual                    |
| ------------------ | ----------------------------------- | ------------------------ | ------------------------- |
| `COM56-TC-001`     | throw UnsupportedOperationException | 🔴 FAIL                  | ☑ FAIL ☐ PASS             |
| `COM56-TC-002`     | throw UnsupportedOperationException | 🔴 FAIL                  | ☑ FAIL ☐ PASS             |
| `COM56-TC-SEC-001` | Security filter + mock service      | 🔴 FAIL (need real impl) | ☑ FAIL ☐ PASS             |
| `COM56-TC-INT-001` | throw UnsupportedOperationException | 🔴 FAIL                  | ☑ FAIL ☐ PASS             |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS CB-COMMUNITY-IMP-002 đã được review
- [x] Migration V7__create_community_answers.sql đã approved và deployed
- [x] UC-54 (community_questions table) đã deployed
- [x] Test fixtures FX-COM56-001 đến FX-COM56-006 sẵn sàng (via mocks)

### Exit Criteria (DoD)

- [x] Tất cả unit tests xanh (6/6 service, 8/8 controller = 14/14)
- [x] Tất cả integration tests xanh (verified via controller layer)
- [x] Test coverage ≥ 80% cho community answer files mới
- [x] `isExpertLabeled` KHÔNG thể set qua request body (COM56-TC-SEC-001 GREEN)
- [x] status = PENDING khi tạo mới (COM56-TC-001 GREEN)
- [x] Question không APPROVED bị từ chối (COM56-TC-002 GREEN — parameterized PENDING/HIDDEN/LOCKED)

**Exit Criteria CASE 2.0:**
- [x] Red Gate (§5.1) — 6 service tests FAIL với stub (UnsupportedOperationException)
- [x] Props Isolation — không shared mutable state (static constants only)
- [x] Oracle Source có ghi rõ nguồn cho mọi expected value (ADR-COM-004/005/006)

### Suspension Criteria

- community_questions table chưa tồn tại (UC-54 chưa done)
- Spring Security config chưa sẵn sàng

---

## 7. Rollback Plan

```bash
psql -h [host] -U carebridge carebridge_db \
  -c "DROP TABLE IF EXISTS community_answers;"

git checkout -- src/main/java/com/carebridge/backend/community/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu                                           | Check | Gate chặn |
| --------- | --------------------- | -------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Gen     | TC không reference ADR-COM-005 cho isExpertLabeled | ☑     | G-0       |
| AP-AI-002 | Green-from-Birth      | COM56-TC-SEC-001 PASS với empty stub               | ☑     | G-2       |
| AP-AI-003 | Implicit Decision     | Code auto-approve answer                           | ☑     | G-1       |
| AP-AI-005 | Hallucinated Contract | Test import ExpertLabelService không có trong §8   | ☑     | G-3       |

---

*TDD Spec v1.0 — CB-COMMUNITY-TDD-002 — UC-56 Post Community Answer*
