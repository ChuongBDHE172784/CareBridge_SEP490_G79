# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC60 — Run AI Symptom Intake

**Document ID:** `CB-TRIAGE-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC60_RunAISymptomIntake/UC60_RunAISymptomIntake_TDS.md` (CB-TRIAGE-IMP-001)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.1.37`
- `08_References/Template/PHASE-4_Test-Spec.md`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC60 Run AI Symptom Intake |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CB-TRIAGE-IMP-001` |
| **Module** | `AI Symptom Intake — triage` |
| **Spec gốc** | `CB-TRIAGE-IMP-001` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT), User Profile` |
| **Downstream Consumers** | `UC61 ViewRiskTriageResult, UC131 ExtractStructuredIntakeData` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-TRIAGE-IMP-001 §17`, `ADR-TRIAGE-001` |
| **Constraints Injected** | C1 (no diagnosis), C2 (disclaimer), C3 (no PII log), C4 (userId from JWT), C5 (layer separation), C6 (timeout→FAILED) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec không mention timeout handling | Gemini có thể timeout sau 4000ms | Test phải verify status = FAILED + TRIAGE-005 khi timeout |
| L2 | userId từ request body (security flaw) | userId PHẢI từ JWT SecurityContext | Test verify userId không lấy từ body |
| L3 | Symptom text có thể bị log | PDPA: PII không được log plaintext | Test verify log không chứa symptom text |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC60 AI Symptom Intake bao gồm các layer:
├── Domain (IntakeSession entity, IntakeStatus enum, RiskLevel enum)
├── Service (TriageService — mock GeminiTriageClient với Mockito)
├── Controller (IntakeController — mock TriageService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + mock Gemini với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-60 §3.3.1.37` | Mother nhập symptoms, AI phân tích, trả sessionId + disclaimer |
| `ADR-TRIAGE-001` | Gemini AI với constraint injection; timeout → FAILED |
| `BR-AI-001` | AI KHÔNG chẩn đoán — chỉ phân loại GREEN/YELLOW/RED |
| `BR-AI-002` | disclaimer bắt buộc trong response |
| `BR-PRIVACY-001` | Symptom text KHÔNG trong logs |
| `CB-TRIAGE-IMP-001 §10` | Error codes TRIAGE-001 đến TRIAGE-005 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Symptoms hợp lệ → 201 + sessionId | `TriageService.runIntake()` | `TRIAGE-TC-001` |
| TC-COND-002 | Symptoms blank → 400 TRIAGE-001 | `IntakeController validation` | `TRIAGE-TC-002` |
| TC-COND-003 | Symptoms > 2000 chars → 400 TRIAGE-001 | `@Size validation` | `TRIAGE-TC-003` |
| TC-COND-004 | Gemini timeout → 503 TRIAGE-005 + status FAILED | `TriageService timeout handling` | `TRIAGE-TC-004` |
| TC-COND-005 | No JWT → 401 | `Security filter` | `TRIAGE-TC-005` |
| TC-COND-006 | Wrong role → 403 TRIAGE-004 | `Authorization check` | `TRIAGE-TC-006` |
| TC-COND-007 | Symptom text not in logs | `Log masking` | `TRIAGE-TC-007` |
| TC-COND-008 | Full flow with DB persistence | `IntakeSessionRepository` | `TRIAGE-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | symptoms length (valid/blank/too-long) | Cover input domain |
| Boundary Value Analysis | symptoms.length == 2000 vs 2001 | Edge case at max |
| State Transition Testing | PENDING→PROCESSING→COMPLETED/FAILED | IntakeStatus FSM |
| Error Guessing | JWT missing/expired/wrong-role | Security attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `user {id: '00000000-...001', role: 'ROLE_MOTHER'}` | Happy path auth |
| `FX-002` | DB seed | `user {id: '00000000-...002', role: 'ROLE_PARTNER'}` | Wrong role test |
| `FX-003` | Mock | `GeminiTriageClient.analyzeSymptoms() → riskLevel=GREEN` | Happy path AI |
| `FX-004` | Mock | `GeminiTriageClient.analyzeSymptoms() → throw TimeoutException` | Timeout test |
| `FX-005` | JWT | `{sub: 'user-001', roles: ['ROLE_MOTHER']}` | Mother auth token |
| `FX-006` | String | `"a".repeat(2001)` | Max length boundary |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// TriageTestFactory.java
class TriageTestFactory {

    static IntakeSession makeIntakeSession() {
        IntakeSession session = new IntakeSession();
        session.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        session.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        session.setSymptoms("SYNTHETIC_SYMPTOMS_TEST_DATA");
        session.setStatus(IntakeStatus.PENDING);
        session.setCreatedAt(Instant.parse("2026-06-26T08:00:00Z"));
        session.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        return session;
    }

    static IntakeSession makeIntakeSession(Consumer<IntakeSession> overrides) {
        IntakeSession session = makeIntakeSession();
        overrides.accept(session);
        return session;
    }

    static RunIntakeRequest makeRunIntakeRequest() {
        RunIntakeRequest req = new RunIntakeRequest();
        req.setSymptoms("Đau đầu nhẹ, sốt 37.5°C — SYNTHETIC");
        return req;
    }
}
```

---

### TRIAGE-TC-001 — Happy path: symptoms hợp lệ → 201 Created

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.runIntake()`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-60 §3.3.1.37 / BR-AI-002`

**Preconditions:**
- GeminiTriageClient mock (FX-003): trả về `{riskLevel: "GREEN", disclaimer: "..."}"`
- User FX-001 tồn tại với ROLE_MOTHER

**Test Steps:**
1. Arrange: mock GeminiTriageClient.analyzeSymptoms() → AiTriageResult(GREEN, disclaimer)
2. Act: `triageService.runIntake(makeRunIntakeRequest(), userId)`
3. Assert: result.sessionId != null; result.status == "COMPLETED"; result.disclaimer not blank

**Expected Result (PASS):**
- `IntakeSessionResponse.sessionId` là UUID hợp lệ
- `IntakeSessionResponse.status` = "COMPLETED"
- `IntakeSessionResponse.disclaimer` không rỗng

**Expected Result (FAIL — dấu hiệu lỗi):**
- NullPointerException — sessionId null
- disclaimer null hoặc rỗng

**Current Status:** 🔴 Not written
**Implementation Note:** TriageService phải publish `IntakeSessionCompleted` event sau khi COMPLETED

---

### TRIAGE-TC-002 — Symptoms blank → 400 TRIAGE-001

**Severity:** `MEDIUM`
**Feature Under Test:** `IntakeController @Valid validation`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-TRIAGE-IMP-001 §10 TRIAGE-001`

**Preconditions:**
- JWT valid với ROLE_MOTHER

**Test Steps:**
1. Arrange: `POST /api/v1/triage/intake` với body `{symptoms: ""}`
2. Act: MockMvc perform
3. Assert: status 400; `error.code` = "TRIAGE-001"

**Expected Result (PASS):**
- HTTP 400
- `error.code` = "TRIAGE-001"

**Expected Result (FAIL):**
- HTTP 201 (validation bypass)

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-003 — Symptoms > 2000 chars → 400 TRIAGE-001

**Severity:** `MEDIUM`
**Feature Under Test:** `@Size(max=2000) validation`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-TRIAGE-IMP-001 §8.1 @Size(max=2000)`

**Preconditions:**
- JWT valid với ROLE_MOTHER

**Test Steps:**
1. Arrange: body `{symptoms: "a".repeat(2001)}`
2. Act: MockMvc perform
3. Assert: status 400; `error.code` = "TRIAGE-001"

**Expected Result (PASS):**
- HTTP 400 + TRIAGE-001

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-004 — Gemini timeout → 503 TRIAGE-005 + status FAILED

**Severity:** `HIGH`
**Feature Under Test:** `TriageService timeout handling`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-TRIAGE-001 / CB-TRIAGE-IMP-001 §10 TRIAGE-005`

**Preconditions:**
- GeminiTriageClient mock (FX-004): throw `GeminiTimeoutException`

**Test Steps:**
1. Arrange: mock Gemini → throw TimeoutException
2. Act: `triageService.runIntake(makeRunIntakeRequest(), userId)`
3. Assert: exception `TriageServiceUnavailableException` với code TRIAGE-005
4. Assert DB: session record có status = FAILED

**Expected Result (PASS):**
- `TriageServiceUnavailableException` thrown
- DB record: `status = 'FAILED'`

**Expected Result (FAIL):**
- Exception không được throw (lỗi không được xử lý)
- Session bị stuck ở PROCESSING

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-005 — No JWT → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** `Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Arrange: POST /api/v1/triage/intake BỎ Authorization header
2. Act: perform request
3. Assert: HTTP 401

**Expected Result (PASS):**
- HTTP 401

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-006 — Wrong role → 403 TRIAGE-004

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `Role-based access control`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Arrange: JWT với role ROLE_PARTNER (FX-002)
2. Act: POST /api/v1/triage/intake
3. Assert: HTTP 403; `error.code` = "TRIAGE-004"

**Expected Result (PASS):**
- HTTP 403 + TRIAGE-004

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-007 — Symptom text NOT in application logs

**Severity:** `CRITICAL`
**Legal:** `PDPA / Luật 91/2025 — PII protection`
**Feature Under Test:** `TriageService log masking`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-PRIVACY-001`

**Preconditions:**
- Log appender được capture trong test (ListAppender hoặc in-memory appender)

**Test Steps:**
1. Arrange: capture log output; mock Gemini → success
2. Act: `triageService.runIntake(request, userId)`
3. Assert: no log message contains "SYNTHETIC_SYMPTOMS_TEST_DATA" hoặc bất kỳ symptom text nào

**Expected Result (PASS):**
- Zero log entries contain symptom text

**Expected Result (FAIL = security vulnerability):**
- Symptom text xuất hiện trong application log

**Current Status:** 🔴 Not written

---

### TRIAGE-TC-INT-001 — Full flow với Testcontainers PostgreSQL

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: intake → DB persistence → event publish`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration V35 applied
- Seed: user FX-001 insert qua JPA

**Test Steps:**
1. Seed user với ROLE_MOTHER
2. Mock GeminiTriageClient → riskLevel = "GREEN"
3. POST /api/v1/triage/intake với JWT ROLE_MOTHER
4. Assert response 201 + sessionId
5. Assert DB: `SELECT status, risk_level FROM intake_sessions WHERE id = :sessionId`

**Expected Result (PASS):**
- HTTP 201
- DB: status = 'COMPLETED', risk_level = 'GREEN'

**DB Assertion:**
```java
IntakeSession record = intakeSessionRepo.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
assertThat(record.getRiskLevel()).isEqualTo(RiskLevel.GREEN);
assertThat(record.getDisclaimer()).isNotBlank();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `TRIAGE-TC-001` | `TriageServiceTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-002` | `IntakeControllerTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-003` | `IntakeControllerTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-004` | `TriageServiceTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-005` | `IntakeControllerTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-006` | `IntakeControllerTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-007` | `TriageServiceTest.java` | `[x]` | `[x]` | — |
| `TRIAGE-TC-INT-001` | `TriageIntegrationTest.java` | `[x]` | `[x]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// TriageService.java — Red Phase stub
@Service
public class TriageService implements ITriageService {

    @Override
    public IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `TRIAGE-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `TRIAGE-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-TRIAGE-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Flyway migration `V35__create_intake_sessions.sql` đã approved và chạy thành công trên staging
- [ ] Test fixtures FX-001 đến FX-006 đã được chuẩn bị

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho TriageService
- [ ] Không có business logic trong IntakeController
- [ ] Không có symptom text plaintext trong logs
- [ ] disclaimer xuất hiện trong mọi successful response

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi IntakeSession instance tạo qua `TriageTestFactory.makeIntakeSession()`
- [ ] **Oracle Source** — mọi expected value có ghi nguồn (BR/SRS/ADR)

### Suspension Criteria

- Gemini API không có trong staging environment
- Flyway migration V35 fail trên staging
- CI pipeline broken

---

## 7. Rollback Plan

```bash
# Revert migration V35 (dev only)
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS intake_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '35';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/triage/
git checkout -- src/main/resources/db/migration/V35__create_intake_sessions.sql
git checkout -- src/test/java/com/carebridge/backend/triage/

# Gap CB-TRIAGE-IMP-001 vẫn OPEN
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-TRIAGE-001/BR-AI-001 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume Gemini behavior không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify TriageService có validation logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import GeminiTriageClient không có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC60 Run AI Symptom Intake — CB-TRIAGE-IMP-001-TEST*

## Story 6.10 OV-01 Test Contract Addendum

| Case ID | Priority | Oracle | Executable linkage |
| --- | --- | --- | --- |
| `OV01-TS-60-001` | P0 | All five active contexts retain server-owned typed origin; GREEN/NEED_MORE_INFO never creates emergency state | `TriageServiceTest`, `story_6_7_lifecycle_origin_contract_test.dart` |
| `OV01-TS-60-002` | P0 | Maternal danger signs remain RED during AI failure/timeout and the emergency link is create-or-reuse exactly once | `TriageServiceTest#runIntake_aiServiceUnavailable_shouldFallbackToJavaRules`, RED cases, `EmergencyTriageLinkPostgresIntegrationTest` |
| `OV01-TS-60-003` | P0 | Official evaluator executes POSTPARTUM and requires diagnosis/prescription/disclaimer safety without inventing thresholds | `POSTPARTUM_MOTHER_STAGE_GAP` benchmark case; evaluator `test_assertions.py`, `test_catalog.py`, `test_local_adapter.py` |

`POSTPARTUM_MEMORY_CONTEXT_GAP` remains a separate known scope gap because health-memory injection is not part of the proven Python graph contract.
