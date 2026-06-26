# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC61 — View Risk Triage Result

**Document ID:** `CB-TRIAGE-IMP-002-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC61_ViewRiskTriageResult/UC61_ViewRiskTriageResult_TDS.md` (CB-TRIAGE-IMP-002)
- `04_Implement/UC60_RunAISymptomIntake/UC60_RunAISymptomIntake_TDS.md` (CB-TRIAGE-IMP-001)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.1.38`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC61 View Risk Triage Result |

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
| **Feature / Gap ID** | `CB-TRIAGE-IMP-002` |
| **Module** | `View Risk Triage Result — triage (read-only)` |
| **Spec gốc** | `CB-TRIAGE-IMP-002` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC60 (intake_sessions table), IAM (JWT)` |
| **Downstream Consumers** | `Mobile App (Flutter), Web App (React)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-TRIAGE-IMP-002 §17`, `ADR-TRIAGE-003` |
| **Constraints Injected** | C1 (owner-only), C2 (disclaimer required), C3 (COMPLETED only), C4 (readOnly tx), C5 (userId from JWT) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec không rõ FAILED session được trả về hay không | BR-TRIAGE-001: chỉ COMPLETED được trả | Test phải verify FAILED → TRIAGE-006 |
| L2 | Cross-user access không được đề cập rõ | BR-AI-004: owner-only | Test verify userB không thấy session của userA |
| L3 | disclaimer có thể null nếu AI chưa trả | BR-AI-003: disclaimer bắt buộc | Test verify disclaimer never null in 200 response |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC61 View Risk Triage Result bao gồm:
├── Service (TriageService.getResult() — mock Repository với Mockito)
├── Controller (IntakeController GET — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — verify cross-user isolation)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-61 §3.3.1.38` | Mother xem riskLevel + summary + disclaimer |
| `BR-AI-003` | disclaimer bắt buộc trong response |
| `BR-AI-004` | Owner-only: không xem session của người khác |
| `BR-TRIAGE-001` | Chỉ trả session COMPLETED |
| `ADR-TRIAGE-003` | @Transactional(readOnly=true) |
| `CB-TRIAGE-IMP-002 §10` | Error codes TRIAGE-003, TRIAGE-004, TRIAGE-006 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Session COMPLETED → 200 + riskLevel | `TriageService.getResult()` | `VIEW-TC-001` |
| TC-COND-002 | Session không tồn tại → 404 TRIAGE-003 | `findByIdAndUserId()` empty | `VIEW-TC-002` |
| TC-COND-003 | Session PROCESSING → 422 TRIAGE-006 | status check | `VIEW-TC-003` |
| TC-COND-004 | Cross-user access → 404 TRIAGE-003 | owner isolation | `VIEW-TC-004` |
| TC-COND-005 | No JWT → 401 | Security filter | `VIEW-TC-005` |
| TC-COND-006 | disclaimer không null trong 200 response | BR-AI-003 | `VIEW-TC-006` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | session status (COMPLETED/PROCESSING/FAILED) | Cover trạng thái |
| State Transition Testing | IntakeStatus FSM | Verify chỉ COMPLETED được phép |
| Error Guessing | Cross-user access attempt | Security isolation |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `intake_session {status: COMPLETED, riskLevel: GREEN, userId: user-A}` | Happy path |
| `FX-002` | DB seed | `intake_session {status: PROCESSING, userId: user-A}` | Not-complete test |
| `FX-003` | DB seed | `intake_session {status: FAILED, userId: user-A}` | Failed session test |
| `FX-004` | JWT | `{sub: 'user-A', roles: ['ROLE_MOTHER']}` | Owner auth |
| `FX-005` | JWT | `{sub: 'user-B', roles: ['ROLE_MOTHER']}` | Cross-user attack |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// TriageResultTestFactory.java
class TriageResultTestFactory {

    static IntakeSession makeCompletedSession() {
        IntakeSession session = new IntakeSession();
        session.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        session.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010")); // user-A
        session.setRiskLevel(RiskLevel.GREEN);
        session.setStatus(IntakeStatus.COMPLETED);
        session.setSummary("SYNTHETIC_SUMMARY");
        session.setDisclaimer("SYNTHETIC_DISCLAIMER — not medical advice");
        session.setCreatedAt(Instant.parse("2026-06-26T08:00:00Z"));
        session.setCompletedAt(Instant.parse("2026-06-26T08:00:05Z"));
        return session;
    }

    static IntakeSession makeCompletedSession(Consumer<IntakeSession> overrides) {
        IntakeSession s = makeCompletedSession();
        overrides.accept(s);
        return s;
    }
}
```

---

### VIEW-TC-001 — Happy path: COMPLETED session → 200 + riskLevel + disclaimer

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.getResult()`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-61 §3.3.1.38 / BR-AI-003`

**Preconditions:**
- Repository mock: `findByIdAndUserId(sessionId, userId)` → FX-001 (COMPLETED session)

**Test Steps:**
1. Arrange: mock repo → FX-001
2. Act: `triageService.getResult(sessionId, userId)`
3. Assert: result.riskLevel == "GREEN"; result.disclaimer not blank; result.status == "COMPLETED"

**Expected Result (PASS):**
- `TriageResultResponse.riskLevel` = "GREEN"
- `TriageResultResponse.disclaimer` not blank
- `TriageResultResponse.completedAt` not null

**Expected Result (FAIL):**
- disclaimer null/blank (BR-AI-003 violation)
- riskLevel null

**Current Status:** 🔴 Not written

---

### VIEW-TC-002 — Session không tồn tại → 404 TRIAGE-003

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.getResult() — not found`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-TRIAGE-IMP-002 §10 TRIAGE-003`

**Test Steps:**
1. Arrange: mock `findByIdAndUserId()` → `Optional.empty()`
2. Act: `triageService.getResult(randomUUID, userId)`
3. Assert: `TriageNotFoundException` thrown với code TRIAGE-003

**Expected Result (PASS):**
- `TriageNotFoundException` với message code "TRIAGE-003"

**Current Status:** 🔴 Not written

---

### VIEW-TC-003 — Session PROCESSING → 422 TRIAGE-006

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageService.getResult() — status check`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-TRIAGE-001 / CB-TRIAGE-IMP-002 §10 TRIAGE-006`

**Test Steps:**
1. Arrange: mock repo → FX-002 (PROCESSING session)
2. Act: `triageService.getResult(sessionId, userId)`
3. Assert: `TriageSessionNotCompletedException` thrown với code TRIAGE-006

**Expected Result (PASS):**
- `TriageSessionNotCompletedException` thrown

**Current Status:** 🔴 Not written

---

### VIEW-TC-004 — Cross-user access → 404 TRIAGE-003 (không leak data)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-AI-004 / PDPA`
**Feature Under Test:** `IIntakeSessionRepository.findByIdAndUserId() isolation`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Test Steps:**
1. Arrange: seed FX-001 (session belonging to user-A); userB JWT (FX-005)
2. Act: `GET /api/v1/triage/intake/{sessionId}` với JWT của userB
3. Assert: HTTP 404; response does NOT contain riskLevel of user-A's session

**Expected Result (PASS — hệ thống an toàn):**
- HTTP 404 TRIAGE-003

**Expected Result (FAIL = security vulnerability):**
- HTTP 200 với data của user-A trả về cho user-B

**Current Status:** 🔴 Not written

---

### VIEW-TC-005 — No JWT → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Act: `GET /api/v1/triage/intake/{id}` BỎ Authorization header
2. Assert: HTTP 401

**Expected Result (PASS):**
- HTTP 401

**Current Status:** 🔴 Not written

---

### VIEW-TC-006 — disclaimer NOT null trong 200 response

**Severity:** `HIGH`
**Legal:** `BR-AI-003 / PDPA`
**Feature Under Test:** `TriageResultResponse.disclaimer mapping`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-AI-003`

**Test Steps:**
1. Arrange: COMPLETED session với disclaimer = null (edge case — corrupted data)
2. Act: `triageService.getResult(sessionId, userId)`
3. Assert: response.disclaimer không null — service phải provide fallback disclaimer

**Expected Result (PASS):**
- disclaimer không bao giờ null/blank trong 200 response

**Implementation Note:** TriageService phải provide fallback disclaimer nếu DB value null

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VIEW-TC-001` | `TriageServiceTest.java` | `[ ]` | `—` | — |
| `VIEW-TC-002` | `TriageServiceTest.java` | `[ ]` | `—` | — |
| `VIEW-TC-003` | `TriageServiceTest.java` | `[ ]` | `—` | — |
| `VIEW-TC-004` | `TriageIntegrationTest.java` | `[ ]` | `—` | — |
| `VIEW-TC-005` | `IntakeControllerTest.java` | `[ ]` | `—` | — |
| `VIEW-TC-006` | `TriageServiceTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// TriageService.java — bổ sung stub cho getResult()
@Override
@Transactional(readOnly = true)
public TriageResultResponse getResult(UUID sessionId, UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VIEW-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VIEW-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VIEW-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VIEW-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VIEW-TC-005` | Security filter active | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VIEW-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-TRIAGE-IMP-002` đã được review và approve
- [ ] UC60 đã được implement (intake_sessions table tồn tại)
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] Test fixtures FX-001 đến FX-005 đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho getResult() method
- [ ] Cross-user isolation verified (VIEW-TC-004 PASS)
- [ ] disclaimer không bao giờ null trong 200 response (VIEW-TC-006 PASS)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi IntakeSession dùng `TriageResultTestFactory.makeCompletedSession()`

### Suspension Criteria

- UC60 chưa được implement (intake_sessions không tồn tại)
- CI pipeline broken

---

## 7. Rollback Plan

```bash
# Không có migration để rollback — UC61 chỉ thêm read method
git checkout -- src/main/java/com/carebridge/backend/triage/TriageService.java
git checkout -- src/main/java/com/carebridge/backend/triage/IntakeController.java
git checkout -- src/test/java/com/carebridge/backend/triage/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-AI-003/BR-AI-004 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume cross-user check tanpa ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có ownership logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import TriageResultRepository không tồn tại | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC61 View Risk Triage Result — CB-TRIAGE-IMP-002-TEST*
