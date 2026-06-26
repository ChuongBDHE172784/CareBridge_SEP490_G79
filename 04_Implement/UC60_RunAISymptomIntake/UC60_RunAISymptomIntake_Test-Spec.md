# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-60: Run AI Symptom Intake

| Field              | Value                                                  |
| ------------------ | ------------------------------------------------------ |
| **Document ID**    | `CB-TRIAGE-TDD-001`                                    |
| **Version**        | `1.0`                                                  |
| **Date**           | `2026-06-26`                                           |
| **Status**         | `Draft`                                                |
| **Standard**       | `ISO/IEC/IEEE 29119-3:2021`                            |
| **Spec gốc**       | `CB-TRIAGE-IMP-001` (UC60_RunAISymptomIntake_TDS.md)   |
| **Author**         | `AI Agent`                                             |
| **Reviewed by**    | `[ ] Pending`                                          |
| **DPO Sign-off**   | `[ ] Pending`                                          |
| **Approved by**    | `[ ] Pending`                                          |
| **Classification** | `Internal — Confidential`                              |

**References:**
- `CB-TRIAGE-IMP-001` — Technical Design Specification for UC-60
- `01_Requirements/SRS.md` — SRS 3.3.1.37
- `TriageRedFlagPolicy.java` — Existing red flag detection policy
- `GeminiClient.java` — Existing Gemini API interface

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL → implement → PASS → refactor.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày       | Người thực hiện | Nội dung thay đổi                                         |
| ---------- | --------------- | ---------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-60 Run AI Symptom Intake          |

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

| Field                     | Value                                                                                             |
| ------------------------- | ------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-60`                                                                                           |
| **Module**                | `triage — IntakeSessionService`                                                                   |
| **Spec gốc**              | `CB-TRIAGE-IMP-001`                                                                               |
| **Priority**              | P0 (High)                                                                                         |
| **Sprint**                | `S3 (2026-07-14 → 2026-07-28)`                                                                    |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                                           |
| **Data Classification**   | `Sensitive-PII (health symptoms)`                                                                 |
| **Compliance Scope**      | `BR-RBAC, BR-PRIVACY, BR-SAFETY`                                                                 |
| **Upstream Dependencies** | `security (JWT)`, `integration.gemini (GeminiClient)`, `identity (User entity)`                   |
| **Downstream Consumers**  | `UC-131 (ExtractStructuredIntakeData)`, `UC-61 (ViewRiskTriageResult)`                             |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                                                                                                                                  |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                                                                                                                                                  |
| **Constraint Source**    | `CB-TRIAGE-IMP-001 §17`, `ADR-001 to ADR-004`                                                                                                                                                                                         |
| **Constraints Injected** | C1: safety prompt block mandatory, C2: JWT-only identity, C3: append-only conversation, C4: max 20 questions, C5: disclaimer on all responses, C6: red flag immediate completion, C7: 30-min timeout                                    |
| **Model**                | `claude-sonnet-4-6`                                                                                                                                                                                                                    |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                                                                                                                           |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                               | Thực tế (schema / policy)                                        | Fix áp dụng trong test                                                  |
| --- | ----------------------------------------------------- | ---------------------------------------------------------------- | ----------------------------------------------------------------------- |
| L1  | Không rõ xử lý khi user đã có session IN_PROGRESS     | Mỗi user chỉ có tối đa 1 active session                          | Test: startSession khi đã có active → 409 TRIAGE-007                    |
| L2  | Không rõ conversation JSON format                      | JSONB array: `[{role, content, timestamp}]`                       | Test: verify conversationJson structure after each interaction           |
| L3  | Timeout logic chưa rõ                                  | lastActivityAt + 30 phút: check tại mỗi submitAnswer             | Test: mock clock to simulate 31 minutes → TRIAGE-006                   |
| L4  | Red flag trong câu trả lời vs trong câu hỏi ban đầu   | Check red flag ở CẢ initialSymptom (start) VÀ answer (submit)    | Test: red flag in start → immediate emergency response                 |
| L5  | Gemini unavailable handling                            | Return fallback message, session vẫn IN_PROGRESS, retry later    | Test: mock GeminiClient throws → fallback question returned             |
| L6  | questionCount increment timing                         | Increment AFTER AI generates question, not on user submit         | Test: questionCount = AI questions asked, not user answers              |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-60 (Run AI Symptom Intake) bao gồm các layer:
├── Domain (IntakeSession entity, IntakeSessionStatus enum — pure logic)
├── Service (IntakeSessionService — mock Repository + GeminiClient với Mockito)
├── Policy (TriageRedFlagPolicy — existing, tested via integration)
├── Prompt (IntakePromptBuilder — unit test prompt construction)
├── Controller (IntakeSessionController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source                          | Items Derived                                                   |
| ------------------------------- | --------------------------------------------------------------- |
| `SRS UC-60 (3.3.1.37)`         | Start session, submit answer, complete session, session status   |
| `ADR-001`                       | Conversational intake via Gemini, conversation JSON storage      |
| `ADR-002`                       | Safety-first prompt design — no diagnosis/prescription           |
| `ADR-003`                       | JWT-only identity — no userId from request body                  |
| `ADR-004`                       | Max 20 questions, 30-min timeout                                 |
| `BR-SAFETY-001`                 | Disclaimer required on all responses                             |
| `BR-SAFETY-002`                 | Red flag → emergency routing                                     |
| `BR-RBAC-001`                   | Only MOTHER role can access                                      |
| `BR-PRIVACY-001`                | userId from JWT only                                             |
| `CB-TRIAGE-IMP-001 §10`        | Error codes TRIAGE-001 through TRIAGE-010                        |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID  | Test Condition                                | Coverage Item                                      | Test Cases                             |
| ------------- | --------------------------------------------- | -------------------------------------------------- | -------------------------------------- |
| TC-COND-001   | Start session happy path                      | `IntakeSessionService.startSession()`              | `TRIAGE-TC-001`                        |
| TC-COND-002   | Start session with active session exists      | `IntakeSessionService.startSession()`              | `TRIAGE-TC-002`                        |
| TC-COND-003   | Submit answer happy path                      | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-003`                        |
| TC-COND-004   | Red flag detection in answer                  | `TriageRedFlagPolicy.isRedFlag()` + submitAnswer   | `TRIAGE-TC-004`                        |
| TC-COND-005   | Red flag detection in initial symptom         | `TriageRedFlagPolicy.isRedFlag()` + startSession   | `TRIAGE-TC-005`                        |
| TC-COND-006   | Session timeout (30 min)                      | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-006`                        |
| TC-COND-007   | Maximum questions limit (20)                  | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-007`                        |
| TC-COND-008   | Complete session manually                     | `IntakeSessionService.completeSession()`           | `TRIAGE-TC-008`                        |
| TC-COND-009   | Submit to already-completed session           | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-009`                        |
| TC-COND-010   | Gemini API unavailable                        | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-010`                        |
| TC-COND-011   | Unauthorized access (wrong role)              | `IntakeSessionController`                          | `TRIAGE-TC-011`                        |
| TC-COND-012   | IDOR — access another user's session          | `IntakeSessionService.submitAnswer()`              | `TRIAGE-TC-012`                        |
| TC-COND-013   | Validation — empty answer                     | `SubmitAnswerRequest` validation                   | `TRIAGE-TC-013`                        |
| TC-COND-014   | Validation — answer too long (>1000 chars)    | `SubmitAnswerRequest` validation                   | `TRIAGE-TC-014`                        |
| TC-COND-015   | ConversationJson append-only integrity        | `IntakeSessionService`                             | `TRIAGE-TC-015`                        |
| TC-COND-016   | Disclaimer present on all responses           | `IntakeSessionMapper`                              | `TRIAGE-TC-016`                        |
| TC-COND-017   | Full E2E intake flow                          | All layers                                         | `TRIAGE-TC-INT-001`                    |
| TC-COND-018   | SQL injection in answer field                 | `IntakeSessionController`                          | `TRIAGE-TC-SEC-001`                    |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)    | Applied To                               | Rationale                                                    |
| -------------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| Equivalence Partitioning   | answer length (0, 1-1000, >1000)         | Validate boundary conditions on input field                  |
| Boundary Value Analysis    | questionCount (0, 1, 19, 20, 21)         | Ensure limit at exactly 20                                   |
| State Transition Testing   | IntakeSessionStatus FSM                  | Validate all valid/invalid state transitions                 |
| Error Guessing             | SQL injection, XSS in answer             | Healthcare app — security-critical                           |
| Decision Table             | Red flag + timeout + maxQ combinations   | Multiple conditions can overlap — need combinatorial test    |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                                       | Mục đích                          |
| ---------- | -------- | ------------------------------------------------------------------- | --------------------------------- |
| `FX-001`   | DB seed  | `{ userId: "user-001", role: "MOTHER", enabled: true }`            | Happy path — authenticated mother |
| `FX-002`   | DB seed  | `{ sessionId: "session-001", userId: "user-001", status: "IN_PROGRESS", questionCount: 3 }` | Active session for submit tests |
| `FX-003`   | DB seed  | `{ sessionId: "session-002", userId: "user-001", status: "COMPLETED" }` | Already-completed session |
| `FX-004`   | DB seed  | `{ sessionId: "session-003", userId: "user-001", status: "IN_PROGRESS", questionCount: 20 }` | Max questions reached |
| `FX-005`   | DB seed  | `{ sessionId: "session-004", userId: "user-001", lastActivityAt: now()-35min }` | Timed out session |
| `FX-006`   | JWT      | `{ sub: "user-001", role: "MOTHER" }`                               | Auth context — Mother             |
| `FX-007`   | JWT      | `{ sub: "user-002", role: "EXPERT" }`                               | Auth context — wrong role         |
| `FX-008`   | JWT      | `{ sub: "user-003", role: "MOTHER" }`                               | Auth context — different Mother (IDOR test) |
| `FX-009`   | Mock     | `GeminiClient.generate() returns "Triệu chứng bắt đầu từ khi nào?"` | Deterministic AI response |
| `FX-010`   | Mock     | `GeminiClient.generate() throws GeminiUnavailableException`          | Gemini unavailable scenario       |
| `FX-011`   | Input    | `answer: "Tôi bị chảy máu nhiều"` (contains red flag keyword)       | Red flag detection               |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory methods
// ═══════════════════════════════════════════════════════════

// IntakeSessionTestFactory.java
class IntakeSessionTestFactory {

    static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID DEFAULT_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    static IntakeSession makeSession() {
        IntakeSession session = new IntakeSession();
        session.setId(DEFAULT_SESSION_ID);
        session.setUserId(DEFAULT_USER_ID);
        session.setStatus(IntakeSessionStatus.IN_PROGRESS);
        session.setConversationJson("[]");
        session.setQuestionCount(0);
        session.setStartedAt(OffsetDateTime.now());
        session.setLastActivityAt(OffsetDateTime.now());
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    static IntakeSession makeSession(Consumer<IntakeSession> overrides) {
        IntakeSession session = makeSession();
        overrides.accept(session);
        return session;
    }

    static StartIntakeRequest makeStartRequest() {
        StartIntakeRequest request = new StartIntakeRequest();
        request.setInitialSymptom("đau bụng dưới");
        return request;
    }

    static SubmitAnswerRequest makeSubmitRequest() {
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setAnswer("Khoảng 2 ngày trước");
        return request;
    }

    static SubmitAnswerRequest makeSubmitRequest(String answer) {
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setAnswer(answer);
        return request;
    }
}
```

---

### TRIAGE-TC-001 — Start intake session successfully

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.startSession()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-60 / BR-TRIAGE-003` — session starts in IN_PROGRESS state

**Preconditions:**
- Mock IntakeSessionRepository: `findByUserIdAndStatus()` returns empty list
- Mock GeminiClient: `generate()` returns "Triệu chứng bắt đầu từ khi nào?"
- Fixture: FX-006 (MOTHER JWT), FX-009 (Gemini mock)

**Test Steps:**
1. Arrange: Create StartIntakeRequest with initialSymptom = "đau bụng dưới"
2. Act: Call `intakeSessionService.startSession(userId, request)`
3. Assert: Verify response fields

**Expected Result (PASS):**
- Response.sessionId is not null
- Response.firstQuestion is "Triệu chứng bắt đầu từ khi nào?"
- Response.status is "IN_PROGRESS"
- Response.disclaimer contains "Đây không phải chẩn đoán y tế"
- Repository.save() was called once with IntakeSession having status=IN_PROGRESS, questionCount=1

**Expected Result (FAIL):**
- Response missing sessionId or disclaimer
- Status is not IN_PROGRESS
- Repository.save() not called

**Current Status:** RED Not written

---

### TRIAGE-TC-002 — Start session blocked when active session exists

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.startSession()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-TRIAGE-003` — only one active session per user

**Preconditions:**
- Mock IntakeSessionRepository: `findByUserIdAndStatus(userId, IN_PROGRESS)` returns 1 session
- Fixture: FX-002 (existing active session)

**Test Steps:**
1. Arrange: Mock repository returns existing IN_PROGRESS session
2. Act: Call `intakeSessionService.startSession(userId, request)`
3. Assert: Exception thrown

**Expected Result (PASS):**
- `ActiveSessionExistsException` thrown with error code `TRIAGE-007`
- Repository.save() NOT called

**Expected Result (FAIL):**
- No exception thrown — second session created

**Current Status:** RED Not written

---

### TRIAGE-TC-003 — Submit answer and receive next question

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC-60 / ADR-001` — conversational intake flow

**Preconditions:**
- Mock IntakeSessionRepository: `findByIdAndUserId()` returns session with questionCount=3, status=IN_PROGRESS, lastActivityAt=now()
- Mock GeminiClient: `generate()` returns next question
- Fixture: FX-002, FX-006, FX-009

**Test Steps:**
1. Arrange: Create SubmitAnswerRequest with answer = "Khoảng 2 ngày trước"
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Response and session state

**Expected Result (PASS):**
- Response.nextQuestion is not null
- Response.questionNumber is 4
- Response.isCompleted is false
- Response.status is "IN_PROGRESS"
- Response.disclaimer contains "Đây không phải chẩn đoán y tế"
- Session.questionCount updated to 4
- Session.lastActivityAt updated
- Session.conversationJson appended with user answer and AI question

**Expected Result (FAIL):**
- questionCount not incremented
- conversationJson not appended
- lastActivityAt not refreshed

**Current Status:** RED Not written

---

### TRIAGE-TC-004 — Red flag keyword detected in answer

**Severity:** `CRITICAL`
**CWE:** `N/A (safety requirement)`
**Legal:** `BR-SAFETY-002`
**Feature Under Test:** `IntakeSessionService.submitAnswer()` + `TriageRedFlagPolicy`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SAFETY-002` — red flag keywords trigger emergency routing

**Preconditions:**
- Mock IntakeSessionRepository: returns IN_PROGRESS session
- TriageRedFlagPolicy: real instance (not mocked)
- Fixture: FX-002, FX-011

**Test Steps:**
1. Arrange: SubmitAnswerRequest with answer = "Tôi bị chảy máu nhiều"
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Emergency response

**Expected Result (PASS):**
- Response.isCompleted is true
- Response.status is "COMPLETED"
- Response.riskLevel is "RED"
- Response.emergencyGuidance contains "gọi 115"
- Session.status transitions to COMPLETED
- Session.completedAt is set
- GeminiClient.generate() NOT called (skip AI question on red flag)
- IntakeRedFlagTriggered event published

**Expected Result (FAIL):**
- Session not completed on red flag
- AI question still generated after red flag
- No emergency guidance in response

**Current Status:** RED Not written
**Implementation Note:** TriageRedFlagPolicy already exists — use real instance, not mock

---

### TRIAGE-TC-005 — Red flag keyword in initial symptom at session start

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY-002`
**Feature Under Test:** `IntakeSessionService.startSession()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SAFETY-002` — red flag check on initial symptom too

**Preconditions:**
- StartIntakeRequest with initialSymptom = "con tôi bị co giật"
- Fixture: FX-006

**Test Steps:**
1. Arrange: Request with red flag keyword in initialSymptom
2. Act: Call `intakeSessionService.startSession(userId, request)`
3. Assert: Immediate emergency response

**Expected Result (PASS):**
- Session created with status COMPLETED (not IN_PROGRESS)
- Response contains riskLevel "RED" and emergencyGuidance
- No Gemini API call made

**Expected Result (FAIL):**
- Session created as IN_PROGRESS despite red flag in initial symptom

**Current Status:** RED Not written

---

### TRIAGE-TC-006 — Session timeout after 30 minutes inactivity

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-004, BR-TRIAGE-002` — 30-minute timeout

**Preconditions:**
- Mock IntakeSessionRepository: returns session with lastActivityAt = 35 minutes ago
- Fixture: FX-005

**Test Steps:**
1. Arrange: Session with lastActivityAt = OffsetDateTime.now().minusMinutes(35)
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Timeout exception

**Expected Result (PASS):**
- `SessionTimedOutException` thrown with code `TRIAGE-006`
- Session.status transitions to TIMED_OUT
- GeminiClient NOT called

**Expected Result (FAIL):**
- No timeout check — answer processed normally despite 35-minute gap

**Current Status:** RED Not written

---

### TRIAGE-TC-007 — Maximum questions limit (20) auto-completes session

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-004, BR-TRIAGE-001` — max 20 questions

**Preconditions:**
- Mock IntakeSessionRepository: returns session with questionCount = 19
- Fixture: FX-004 (modified to 19)

**Test Steps:**
1. Arrange: Session with questionCount = 19 (this submit will be question 20)
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Session auto-completes after 20th question

**Expected Result (PASS):**
- Response.isCompleted is true
- Response.status is "COMPLETED"
- Session.questionCount is 20
- Session.completedAt is set
- IntakeSessionCompleted event published

**Expected Result (FAIL):**
- questionCount exceeds 20
- Session not auto-completed

**Current Status:** RED Not written

---

### TRIAGE-TC-008 — Complete session manually

**Severity:** `MEDIUM`
**Feature Under Test:** `IntakeSessionService.completeSession()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC-60` — manual completion before max questions

**Preconditions:**
- Mock IntakeSessionRepository: returns IN_PROGRESS session with questionCount=5
- Fixture: FX-002

**Test Steps:**
1. Arrange: Active session with questionCount=5
2. Act: Call `intakeSessionService.completeSession(userId, sessionId)`
3. Assert: Session completed

**Expected Result (PASS):**
- Response.status is "COMPLETED"
- Session.status is COMPLETED
- Session.completedAt is set
- IntakeSessionCompleted event published

**Expected Result (FAIL):**
- Status not changed to COMPLETED

**Current Status:** RED Not written

---

### TRIAGE-TC-009 — Submit answer to already-completed session

**Severity:** `MEDIUM`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-TRIAGE-003` — state machine: only IN_PROGRESS allows submit

**Preconditions:**
- Mock IntakeSessionRepository: returns session with status = COMPLETED
- Fixture: FX-003

**Test Steps:**
1. Arrange: Session with status COMPLETED
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Exception thrown

**Expected Result (PASS):**
- `SessionAlreadyCompletedException` thrown with code `TRIAGE-008`
- No changes to session or conversation

**Expected Result (FAIL):**
- Answer accepted on completed session

**Current Status:** RED Not written

---

### TRIAGE-TC-010 — Gemini API unavailable during submit answer

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-001` — handle Gemini unavailability gracefully

**Preconditions:**
- Mock GeminiClient: `generate()` throws `GeminiUnavailableException`
- Mock IntakeSessionRepository: returns IN_PROGRESS session
- Fixture: FX-002, FX-010

**Test Steps:**
1. Arrange: GeminiClient mocked to throw GeminiUnavailableException
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Graceful degradation

**Expected Result (PASS):**
- User answer is still appended to conversationJson
- Fallback message returned as nextQuestion (e.g., "Xin lỗi, hệ thống AI tạm thời gặp sự cố. Vui lòng thử lại.")
- Session remains IN_PROGRESS (not crashed)
- questionCount NOT incremented (AI question not generated)

**Expected Result (FAIL):**
- 500 Internal Server Error thrown to client
- Session corrupted or conversation lost

**Current Status:** RED Not written

---

### TRIAGE-TC-011 — Controller: unauthorized access (wrong role)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC-001`
**Feature Under Test:** `IntakeSessionController`
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/IntakeSessionControllerTest.java`
**TDD Phase:** RED

**Preconditions:**
- @WebMvcTest context
- JWT token with role = EXPERT (not MOTHER)
- Fixture: FX-007

**Test Steps:**
1. Arrange: MockMvc with EXPERT role JWT
2. Act: POST /api/v1/triage/intake/sessions
3. Assert: 403 Forbidden

**Expected Result (PASS):**
- HTTP 403 Forbidden
- Response body contains error code TRIAGE-003
- Service method NOT called

**Expected Result (FAIL):**
- 200/201 response — EXPERT can create intake session

**Current Status:** RED Not written

---

### TRIAGE-TC-012 — Service: IDOR prevention — different user's session

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-PRIVACY-001`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-003` — JWT-only identity, query by userId + sessionId

**Preconditions:**
- Session belongs to user-001
- Request made by user-003 (different user)
- Mock IntakeSessionRepository: `findByIdAndUserId(sessionId, user-003)` returns empty
- Fixture: FX-008

**Test Steps:**
1. Arrange: Session owned by user-001, attempting user is user-003
2. Act: Call `intakeSessionService.submitAnswer(user-003-id, sessionId, request)`
3. Assert: ResourceNotFoundException

**Expected Result (PASS):**
- `ResourceNotFoundException` thrown with code `TRIAGE-002`
- No data returned about the session (no information leakage)

**Expected Result (FAIL):**
- User-003 can access user-001's session

**Current Status:** RED Not written

---

### TRIAGE-TC-013 — Validation: empty answer rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `SubmitAnswerRequest` @NotBlank validation
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/IntakeSessionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-TRIAGE-IMP-001 §8` — answer is @NotBlank

**Preconditions:**
- @WebMvcTest context
- Valid MOTHER JWT

**Test Steps:**
1. Arrange: SubmitAnswerRequest with answer = "" (empty string)
2. Act: POST /api/v1/triage/intake/sessions/{id}/answers
3. Assert: 400 Bad Request

**Expected Result (PASS):**
- HTTP 400
- Error code TRIAGE-001
- Details mention "answer" field

**Expected Result (FAIL):**
- Empty answer accepted and processed

**Current Status:** RED Not written

---

### TRIAGE-TC-014 — Validation: answer exceeds 1000 characters

**Severity:** `MEDIUM`
**Feature Under Test:** `SubmitAnswerRequest` @Size validation
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/IntakeSessionControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-TRIAGE-IMP-001 §8` — answer max 1000 chars

**Preconditions:**
- @WebMvcTest context
- Valid MOTHER JWT

**Test Steps:**
1. Arrange: SubmitAnswerRequest with answer = "a".repeat(1001)
2. Act: POST /api/v1/triage/intake/sessions/{id}/answers
3. Assert: 400 Bad Request

**Expected Result (PASS):**
- HTTP 400
- Error code TRIAGE-001

**Expected Result (FAIL):**
- Oversized answer accepted

**Current Status:** RED Not written

---

### TRIAGE-TC-015 — ConversationJson is append-only

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionService.submitAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `ADR-001, C3` — append-only conversation

**Preconditions:**
- Session with existing conversationJson containing 2 messages
- Fixture: FX-002 (modified with 2-message conversation)

**Test Steps:**
1. Arrange: Session with conversationJson = `[{msg1}, {msg2}]`
2. Act: Call `intakeSessionService.submitAnswer(userId, sessionId, request)`
3. Assert: Original messages preserved, new messages appended

**Expected Result (PASS):**
- conversationJson now contains 4 messages: `[{msg1}, {msg2}, {user-answer}, {ai-question}]`
- msg1 and msg2 are unchanged (exact same content)

**Expected Result (FAIL):**
- Original messages modified or removed
- conversationJson replaced instead of appended

**Current Status:** RED Not written

---

### TRIAGE-TC-016 — Disclaimer present on all response types

**Severity:** `HIGH`
**Legal:** `BR-SAFETY-001`
**Feature Under Test:** `IntakeSessionMapper` + all response DTOs
**Test File:** `src/test/java/com/carebridge/backend/triage/service/IntakeSessionServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `BR-SAFETY-001, C5` — disclaimer mandatory

**Preconditions:**
- Various scenarios producing StartIntakeResponse and SubmitAnswerResponse

**Test Steps:**
1. Act: Call startSession() → check disclaimer
2. Act: Call submitAnswer() normal → check disclaimer
3. Act: Call submitAnswer() red flag → check disclaimer

**Expected Result (PASS):**
- ALL responses contain non-null, non-empty disclaimer field
- Disclaimer contains "Đây không phải chẩn đoán y tế"

**Expected Result (FAIL):**
- Any response missing disclaimer

**Current Status:** RED Not written

---

### SECURITY TEST CASES

---

### TRIAGE-TC-SEC-001 — SQL injection attempt in answer field

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Legal:** `BR-SAFETY, BR-PRIVACY`
**Feature Under Test:** `IntakeSessionController` + JPA parameterized queries
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/IntakeSessionControllerTest.java`
**TDD Phase:** RED

**Preconditions:**
- @WebMvcTest context
- Valid MOTHER JWT
- Active session exists

**Test Steps (Attack Simulation):**
1. Arrange: answer = `"'; DROP TABLE intake_sessions; --"`
2. Act: POST /api/v1/triage/intake/sessions/{id}/answers
3. Assert: SQL injection neutralized

**Expected Result (PASS):**
- Answer stored safely as plain text (JPA parameterized query prevents injection)
- intake_sessions table still exists
- No SQL execution from user input

**Expected Result (FAIL):**
- Table dropped or query executed from user input

**Current Status:** RED Not written

---

### INTEGRATION TEST CASES

---

### TRIAGE-TC-INT-001 — Full intake session lifecycle

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: start → submit 3 answers → complete → verify DB state`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeSessionIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-017`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration V35 applied
- Mother user seeded in users table
- GeminiClient mocked via @MockBean

**Test Steps:**
1. Seed: Insert Mother user in DB
2. POST /api/v1/triage/intake/sessions → 201
3. POST /api/v1/triage/intake/sessions/{id}/answers (3 times) → 200 each
4. POST /api/v1/triage/intake/sessions/{id}/complete → 200
5. GET /api/v1/triage/intake/sessions/{id} → verify COMPLETED

**Expected Result (PASS):**
- Session in DB with status = COMPLETED
- questionCount = 4 (1 initial + 3 follow-ups)
- conversationJson contains 7 entries (4 AI questions + 3 user answers)
- All timestamps properly set

**Expected Result (FAIL):**
- Session state inconsistent with expected flow
- Missing conversation entries

**DB Assertion:**
```java
IntakeSession session = intakeSessionRepository.findById(sessionId).orElseThrow();
assertThat(session).isNotNull();
assertThat(session.getStatus()).isEqualTo(IntakeSessionStatus.COMPLETED);
assertThat(session.getQuestionCount()).isEqualTo(4);
assertThat(session.getCompletedAt()).isNotNull();
```

**Current Status:** RED Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                                          | RED confirmed | GREEN (commit) | REFACTOR note |
| ------------------ | -------------------------------------------------- | ------------- | -------------- | ------------- |
| `TRIAGE-TC-001`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-002`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-003`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-004`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-005`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-006`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-007`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-008`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-009`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-010`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-011`    | `IntakeSessionControllerTest.java`                 | `[ ]`         |                |               |
| `TRIAGE-TC-012`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-013`    | `IntakeSessionControllerTest.java`                 | `[ ]`         |                |               |
| `TRIAGE-TC-014`    | `IntakeSessionControllerTest.java`                 | `[ ]`         |                |               |
| `TRIAGE-TC-015`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-016`    | `IntakeSessionServiceTest.java`                    | `[ ]`         |                |               |
| `TRIAGE-TC-SEC-001`| `IntakeSessionControllerTest.java`                 | `[ ]`         |                |               |
| `TRIAGE-TC-INT-001`| `IntakeSessionIntegrationTest.java`                | `[ ]`         |                |               |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class IntakeSessionService implements IIntakeSessionService {

    @Override
    public StartIntakeResponse startSession(UUID userId, StartIntakeRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SubmitAnswerResponse submitAnswer(UUID userId, UUID sessionId, SubmitAnswerRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public CompleteIntakeResponse completeSession(UUID userId, UUID sessionId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public IntakeSessionStatusResponse getSessionStatus(UUID userId, UUID sessionId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID              | Stub Result                    | Expected  | Actual        | Root Cause (nếu PASS bất thường) |
| ------------------ | ------------------------------ | --------- | ------------- | -------------------------------- |
| `TRIAGE-TC-001`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-002`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-003`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-004`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-005`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-006`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-007`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-008`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-009`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-010`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-011`    | `@PreAuthorize blocks`         | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-012`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-013`    | `@Valid rejects before service`| FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-014`    | `@Valid rejects before service`| FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-015`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-016`    | `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-SEC-001`| `JPA param query neutralizes`  | FAIL      | [ ] FAIL [ ] PASS |                                |
| `TRIAGE-TC-INT-001`| `throw('Not implemented')`     | FAIL      | [ ] FAIL [ ] PASS |                                |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? [ ] Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `src/test/resources/red-gate-evidence-uc60.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-TRIAGE-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] Flyway migration `V35__create_intake_sessions.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị
- [ ] GeminiClient interface sẵn sàng (already exists in codebase)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho IntakeSessionService class
- [ ] Không có business logic trong IntakeSessionController
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Mọi response chứa disclaimer field
- [ ] Red flag detection hoạt động cho cả initialSymptom và answer
- [ ] Session timeout check hoạt động (30 phút)
- [ ] questionCount limit enforced (max 20)
- [ ] IDOR prevention verified (userId from JWT only)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 18 tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- GeminiClient interface thay đổi breaking
- V35 migration bị conflict với migration khác
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS intake_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TYPE IF EXISTS intake_session_status CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '35';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/triage/
git checkout -- src/main/resources/db/migration/V35__create_intake_sessions.sql
git checkout -- src/test/java/com/carebridge/backend/triage/

# Feature remains in backlog for next sprint
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern            | Dấu hiệu trong TDD spec                              | Check | Gate chặn |
| --------- | ----------------------- | ----------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR constraint nào               | [ ]   | G-0       |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty/throw stub (§5.1)                  | [ ]   | G-2       |
| AP-AI-003 | Implicit Decision        | Test assume architecture decision không có ADR          | [ ]   | G-1       |
| AP-AI-004 | Layer Violation          | Test verify controller có business logic                | [ ]   | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import service/type không tồn tại trong codebase   | [ ]   | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
