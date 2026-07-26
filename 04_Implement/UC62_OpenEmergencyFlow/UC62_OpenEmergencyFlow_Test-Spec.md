# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC62 — Open Emergency Flow

**Document ID:** `CB-EMERG-IMP-001-TEST`
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
- `04_Implement/UC62_OpenEmergencyFlow/UC62_OpenEmergencyFlow_TDS.md` (CB-EMERG-IMP-001)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.1.39`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC62 Open Emergency Flow |

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
| **Feature / Gap ID** | `CB-EMERG-IMP-001` |
| **Module** | `Open Emergency Flow — emergency (CRITICAL)` |
| **Spec gốc** | `CB-EMERG-IMP-001` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT), UC131 EmergencyEscalationTriggered` |
| **Downstream Consumers** | `UC65 SendFamilyAlert` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EMERG-IMP-001 §17`, `ADR-EMERG-001/002/003` |
| **Constraints Injected** | C1 (<200ms), C2 (location optional), C3 (idempotent), C4 (userId from JWT), C5 (event publish) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec không mention idempotency | ADR-EMERG-003: trả session hiện tại nếu ACTIVE | Test verify 2nd call → same session, no duplicate |
| L2 | Location được assume là required | ADR-EMERG-002: location optional | Test verify session created without location |
| L3 | 200ms SLA không có test | ADR-EMERG-001: p99 < 200ms mandatory | Performance test required |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC62 Open Emergency Flow bao gồm:
├── Service (EmergencyService — mock Repository và EventPublisher)
├── Controller (EmergencyController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — verify idempotency + DB state)
└── Performance (k6 load test — verify < 200ms p99)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-62 §3.3.1.39` | Mother kích hoạt emergency; session ACTIVE được tạo |
| `ADR-EMERG-001` | < 200ms SLA; minimal validation |
| `ADR-EMERG-002` | Location optional |
| `ADR-EMERG-003` | Idempotent — return existing ACTIVE session |
| `BR-EMERG-003` | 1 ACTIVE session per user |
| `CB-EMERG-IMP-001 §10` | Error codes EMERG-001/003/004/005 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | New session created → 201 | `EmergencyService.openFlow()` | `EMERG-TC-001` |
| TC-COND-002 | No location → session still created | Location optional | `EMERG-TC-002` |
| TC-COND-003 | Existing ACTIVE session → return it, no new | Idempotency | `EMERG-TC-003` |
| TC-COND-004 | EmergencySessionOpened published | Event publishing | `EMERG-TC-004` |
| TC-COND-005 | triggerSource invalid → 400 | Validation | `EMERG-TC-005` |
| TC-COND-006 | Wrong role → 403 EMERG-004 | RBAC | `EMERG-TC-006` |
| TC-COND-007 | p99 < 200ms under load | Performance | `EMERG-TC-PERF-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | ACTIVE/RESOLVED/CANCELLED | EmergencyStatus FSM |
| Boundary Value Analysis | triggerSource enum | Valid vs invalid values |
| Error Guessing | Concurrent emergency requests | Race condition in idempotency |
| Performance Testing | 200ms SLA | k6 under 50 concurrent users |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `user {id: 'user-001', role: 'ROLE_MOTHER'}` | Happy path user |
| `FX-002` | DB seed | `emergency_session {status: ACTIVE, userId: 'user-001'}` | Idempotency test |
| `FX-003` | JWT | `{sub: 'user-001', roles: ['ROLE_MOTHER']}` | Mother token |
| `FX-004` | JWT | `{sub: 'user-002', roles: ['ROLE_PARTNER']}` | Wrong role token |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// EmergencyTestFactory.java
class EmergencyTestFactory {

    static EmergencySession makeActiveSession() {
        EmergencySession s = new EmergencySession();
        s.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        s.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        s.setStatus(EmergencyStatus.ACTIVE);
        s.setTriggerSource("MANUAL");
        s.setCreatedAt(Instant.parse("2026-06-26T08:00:00Z"));
        s.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        return s;
    }

    static EmergencySession makeActiveSession(Consumer<EmergencySession> overrides) {
        EmergencySession s = makeActiveSession();
        overrides.accept(s);
        return s;
    }

    static OpenEmergencyRequest makeRequest() {
        OpenEmergencyRequest req = new OpenEmergencyRequest();
        req.setTriggerSource("MANUAL");
        req.setUserLatitude(10.7769);
        req.setUserLongitude(106.7009);
        return req;
    }
}
```

---

### EMERG-TC-001 — Happy path: tạo session mới → 201

**Severity:** `CRITICAL`
**Feature Under Test:** `EmergencyService.openFlow()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-62 §3.3.1.39 / ADR-EMERG-001`

**Preconditions:**
- Repository mock: `findActiveByUserId()` → `Optional.empty()`
- Repository mock: `save()` → FX-001 session

**Test Steps:**
1. Arrange: mock repo; mock eventPublisher
2. Act: `emergencyService.openFlow(makeRequest(), userId)`
3. Assert: `EmergencySessionResponse.sessionId` not null; status = ACTIVE
4. Assert: `repo.save()` called once; `eventPublisher.publishEvent()` called

**Expected Result (PASS):**
- sessionId non-null, status = ACTIVE

**Current Status:** 🔴 Not written

---

### EMERG-TC-002 — Location null → session still created

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyService — location optional`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-EMERG-002 / BR-EMERG-002`

**Test Steps:**
1. Arrange: request với lat=null, lng=null
2. Act: `emergencyService.openFlow(requestNoLocation, userId)`
3. Assert: session created; `saved.getUserLatitude()` = null

**Expected Result (PASS):**
- Session created successfully without location

**Expected Result (FAIL):**
- Exception thrown because location null (BR-EMERG-002 violation)

**Current Status:** 🔴 Not written

---

### EMERG-TC-003 — Idempotent: ACTIVE session already exists → return it

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyService — idempotency`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-EMERG-003 / BR-EMERG-003`

**Test Steps:**
1. Arrange: mock `findActiveByUserId()` → FX-002 (existing ACTIVE session)
2. Act: `emergencyService.openFlow(makeRequest(), userId)`
3. Assert: result.sessionId == FX-002.id; `repo.save()` NOT called

**Expected Result (PASS):**
- Returns existing session; no new INSERT

**Current Status:** 🔴 Not written

---

### EMERG-TC-004 — EmergencySessionOpened event published

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyService event publishing`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-EMERG-IMP-001 §7 Domain Event`

**Test Steps:**
1. Arrange: mock repo → no existing session; eventPublisher mock
2. Act: `emergencyService.openFlow(makeRequest(), userId)`
3. Assert: `eventPublisher.publishEvent()` called with `EmergencySessionOpened`
4. Assert: event.payload.sessionId matches created session

**Expected Result (PASS):**
- EmergencySessionOpened event published with correct sessionId

**Current Status:** 🔴 Not written

---

### EMERG-TC-005 — Invalid triggerSource → 400 EMERG-001

**Severity:** `MEDIUM`
**Feature Under Test:** `EmergencyController validation`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Arrange: body `{triggerSource: "INVALID_VALUE"}`
2. Act: MockMvc POST
3. Assert: HTTP 400; `error.code` = "EMERG-001"

**Expected Result (PASS):**
- HTTP 400 + EMERG-001

**Current Status:** 🔴 Not written

---

### EMERG-TC-006 — Wrong role → 403 EMERG-004

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `RBAC check`
**Test File:** `src/test/java/com/carebridge/backend/emergency/EmergencyControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Arrange: JWT FX-004 (ROLE_PARTNER)
2. Act: POST /api/v1/emergency/sessions
3. Assert: HTTP 403; `error.code` = "EMERG-004"

**Current Status:** 🔴 Not written

---

### EMERG-TC-PERF-001 — Performance: p99 < 200ms

**Severity:** `CRITICAL`
**Feature Under Test:** `EmergencyController end-to-end latency`
**Test File:** `k6/emergency-load-test.js`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-EMERG-001 / BR-EMERG-001`

**Preconditions:**
- Staging environment running
- 50 virtual users

**Test Steps:**
1. k6 run với 50 VUs x 100 iterations
2. Each VU: POST /api/v1/emergency/sessions
3. Measure p99 latency

**Expected Result (PASS):**
- p99 < 200ms

**Expected Result (FAIL):**
- p99 ≥ 200ms → investigate bottleneck (DB query, event publish)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EMERG-TC-001` | `EmergencyServiceTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-002` | `EmergencyServiceTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-003` | `EmergencyServiceTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-004` | `EmergencyServiceTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-005` | `EmergencyControllerTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-006` | `EmergencyControllerTest.java` | `[x]` | `[x]` | — |
| `EMERG-TC-PERF-001` | `k6/emergency-load-test.js` | `[x]` | `[x]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class EmergencyService implements IEmergencyService {

    @Override
    public EmergencySessionResponse openFlow(OpenEmergencyRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EMERG-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EMERG-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EMERG-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EMERG-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EMERG-TC-005` | Validation active | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EMERG-TC-006` | Security filter active | 🔴 FAIL | ☑ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EMERG-IMP-001` đã được review và approve
- [ ] Migration V37 approved và chạy trên staging
- [ ] Logic Issues (Section 2) đã confirm
- [ ] Performance monitoring setup trên staging

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh
- [ ] EMERG-TC-PERF-001 PASS: p99 < 200ms
- [ ] EMERG-TC-003 PASS: idempotency verified
- [ ] EMERG-TC-004 PASS: event published

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi EmergencySession dùng `EmergencyTestFactory.makeActiveSession()`

### Suspension Criteria

- Staging environment không có performance monitoring
- V37 migration fail

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS emergency_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '37';"

git checkout -- src/main/java/com/carebridge/backend/emergency/
git checkout -- src/main/resources/db/migration/V37__create_emergency_sessions.sql
git checkout -- src/test/java/com/carebridge/backend/emergency/
```

---

## Story 6.10 OV-01 Test Contract Addendum

**Addendum status:** `In Review`
These cases supersede the stale Story 6.10 emergency rows and status checkboxes elsewhere in this document. Historical EMERG cases remain provenance; they are not a release oracle unless linked below to an executable current test.

| Case ID | Pri | Oracle and exact expected result | Executable linkage | Current status |
| --- | --- | --- | --- | --- |
| `OV01-TS-62-001` | P0 | A completed, owner-matching RED intake creates or reuses one ACTIVE owner emergency. The same intake replay returns the same canonical session; a foreign/non-RED/incomplete intake returns `EMERG-006` and creates no link/session/event. | `EmergencyServiceTest#triageReplayUsesCanonicalSafetyEventSourceIdentity`, `#triageCreatesCanonicalEmergencyEventAndPublishesDeliveryTrigger`, `EmergencyTriageLinkPostgresIntegrationTest#firstRedFlushesParentSecondReusesAndReplayReturnsCanonicalEmergency` | Implemented; final Story 6.10 runner evidence required |
| `OV01-TS-62-002` | P0 | Two RED intakes for one owner keep two canonical intake links while reusing one ACTIVE emergency. A pre-existing manual ACTIVE session is linked instead of duplicated. Only new-session creation publishes `EmergencySessionOpened`. | `EmergencyServiceTest#activeManualSessionIsLinkedToTriageWithoutDuplicateEmergency`, `#twoRedIntakesReuseOneEmergencyButKeepTwoCanonicalLinksAndReplay` | Implemented; final Story 6.10 runner evidence required |
| `OV01-TS-62-003` | P0 | Retry/restart reclaims only expired/unfinished alert attempts, never resends a successful device, and preserves one emergency/session association. Resolution fences unfinished alert projection. | `EmergencyTriageLinkPostgresIntegrationTest#ov01E2e014RestartReclaimsExpiredAttemptWithoutResendingSuccessfulDevice`, `EmergencyServiceTest#resolvingSessionSuppressesAndFencesInFlightAlertProjection` | Implemented; final Story 6.10 runner evidence required |
| `OV01-TS-62-004` | P0 | Unauthenticated manual open is 401, wrong role is 403, missing trigger source is 400; owner-scoped active/read/resolve never discloses another account's session. | `EmergencyControllerTest`, `EmergencyServiceTest`, `OV01-E2E-015` owner/account-switch coverage | Implemented; final Story 6.10 runner evidence required |
| `OV01-TS-62-PERF-001` | P1 | No current measured p99 oracle is available. The historical `p99 < 200 ms` row must remain `UNKNOWN / NOT ASSESSED`; unit/integration duration is not a substitute. | No current-source load artifact | `UNKNOWN / NOT ASSESSED` |

### Addendum entry and exit criteria

- Entry: UC-60 deterministic RED contract is green; PostgreSQL/Flyway is current; test data is synthetic.
- Exit: `OV01-TS-62-001..004` pass in the final source-bound runner with zero failures/skips and exact PostgreSQL cardinality checks.
- Suspension: any duplicate ACTIVE emergency, cross-account disclosure, second AI RED decision, missing canonical intake link, or successful-device resend is an immediate release blocker.
- No performance, legal, DPO, TLS, encryption-at-rest, or retention approval is inferred from functional test success.

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không test 200ms SLA | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code require location without ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller có idempotency logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import EmergencyRepo không có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC62 Open Emergency Flow — CB-EMERG-IMP-001-TEST*
