# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-22 Create Mother Journey

**Document ID:** `CB-JOURNEY-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC22_CreateMotherJourney/UC22_CreateMotherJourney_TDS.md` (CB-JOURNEY-IMP-001)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-22 |

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

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-22` |
| **Module** | `CreateMotherJourney — journey` |
| **Spec gốc** | `CB-JOURNEY-IMP-001` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `auth (JWT), accounts table` |
| **Downstream Consumers** | `journey dashboard, notification, audit` |

### 1.1 AI Generation Context

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-JOURNEY-IMP-001 §17` |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix áp dụng |
|---|----------|---------|-------------|
| L1 | SRS: "creates a journey" — không rõ duplicate handling | ADR-JOURNEY-001: 1 ACTIVE journey per type | Test encode duplicate check → 409 |
| L2 | SRS: không rõ startDate constraint | BR-JOURNEY-003: startDate ≤ today + 7 days | Test encode future date → 400 |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
CreateMotherJourney module bao gồm:
├── Domain (JourneyType enum, JourneyStatus enum)
├── Service (JourneyService — mock JPA Repository)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `UC-22` | Mother creates journey, receives 201 with journeyId |
| `ADR-JOURNEY-001` | Duplicate active journey → 409 |
| `BR-JOURNEY-003` | startDate constraint |
| `BR-RBAC` | Only ROLE_MOTHER |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid journey creation | `JourneyService.createJourney()` | `JOURNEY-TC-001` |
| TC-COND-002 | Duplicate active journey | `validateNoDuplicateActiveJourney()` | `JOURNEY-TC-002` |
| TC-COND-003 | Invalid journey type | DTO validation | `JOURNEY-TC-003` |
| TC-COND-004 | EXPERT role blocked | `@PreAuthorize` | `JOURNEY-TC-004` |
| TC-COND-005 | Full DB persistence | Integration test | `JOURNEY-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | journeyType enum | 4 valid types + invalid |
| Boundary Value Analysis | startDate | today, today+7, today+8 |
| State Transition Testing | JourneyStatus | ACTIVE → COMPLETED/ARCHIVED |
| Error Guessing | Auth bypass | EXPERT role, no JWT |

### TDS-05 — Test Data

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{id: UUID-001, accountId: ACC-001, journeyType: PREGNANCY, status: ACTIVE}` | Duplicate check |
| `FX-002` | JWT | `{sub: 'ACC-002', role: 'MOTHER'}` | Happy path auth |
| `FX-003` | JWT | `{sub: 'ACC-003', role: 'EXPERT'}` | RBAC test |
| `FX-004` | Input | `{journeyType: "PREGNANCY", startDate: today}` | Happy path input |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class JourneyTestFactory {
    static MotherJourney makeJourney() {
        MotherJourney j = new MotherJourney();
        j.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        j.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        j.setJourneyType(JourneyType.PREGNANCY);
        j.setStatus(JourneyStatus.ACTIVE);
        j.setStartDate(LocalDate.now());
        return j;
    }

    static CreateJourneyRequest makeRequest() {
        CreateJourneyRequest r = new CreateJourneyRequest();
        r.setJourneyType(JourneyType.PREGNANCY);
        r.setStartDate(LocalDate.now());
        return r;
    }
}
```

---

### JOURNEY-TC-001 — Happy path: create PREGNANCY journey

**Severity:** `CRITICAL`
**Feature Under Test:** `JourneyService.createJourney()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-22 Normal Flow Step 5`

**Preconditions:**
- No existing ACTIVE PREGNANCY journey for account ACC-002
- FX-002 JWT

**Test Steps:**
1. Mock `journeyRepository.findByAccountIdAndJourneyTypeAndStatus()` → empty
2. Mock `journeyRepository.save()` → saved journey with UUID
3. Call `journeyService.createJourney(FX-004, ACC-002)`

**Expected Result (PASS):**
- Returns `CreateJourneyResponse` with `status = "ACTIVE"`, `journeyType = "PREGNANCY"`
- `repository.save()` called exactly once
- `auditService.emit(JourneyCreated)` called once

**Expected Result (FAIL):**
- Returns null or throws unexpected exception

**Current Status:** 🔴 Not written

---

### JOURNEY-TC-002 — Duplicate active journey → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `JourneyService.validateNoDuplicateActiveJourney()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-JOURNEY-001`

**Preconditions:**
- FX-001 seed: existing ACTIVE PREGNANCY journey for account ACC-001
- FX-002 JWT for ACC-001

**Test Steps:**
1. Mock `journeyRepository.findByAccountIdAndJourneyTypeAndStatus()` → returns FX-001
2. Call `journeyService.createJourney({journeyType: PREGNANCY}, ACC-001)`

**Expected Result (PASS):**
- Throws `DuplicateActiveJourneyException` with code `JOURNEY-002`
- `repository.save()` NOT called

**Current Status:** 🔴 Not written

---

### JOURNEY-TC-003 — Invalid journey type → 400

**Severity:** `HIGH`
**Feature Under Test:** `CreateJourneyRequest` DTO validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`

**Test Steps:**
1. POST `/api/v1/journeys` with `{"journeyType": "INVALID_TYPE"}`
2. Spring @Valid kicks in

**Expected Result (PASS):**
- 400 response with validation error
- `service.createJourney()` NOT called

**Current Status:** 🔴 Not written

---

### JOURNEY-TC-004 — EXPERT role → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on endpoint
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Test Steps:**
1. POST `/api/v1/journeys` with FX-003 JWT (EXPERT role)

**Expected Result (PASS):**
- 403 Forbidden
- `service.createJourney()` NOT called

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### JOURNEY-TC-SEC-001 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** JWT filter
**TDD Phase:** 🔴 RED

**Test Steps:**
1. POST `/api/v1/journeys` without Authorization header

**Expected Result (PASS):** 401 Unauthorized

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### JOURNEY-TC-INT-001 — Full DB persistence

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/journeys` → DB assertion
**Test File:** `src/test/java/com/carebridge/backend/journey/CreateMotherJourneyIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway V20 migration applied
- Account ACC-002 seeded in accounts table

**Test Steps:**
1. POST `/api/v1/journeys` with FX-002 JWT and FX-004 body
2. Assert response 201
3. Assert DB: `SELECT COUNT(*) FROM mother_journeys WHERE account_id='ACC-002'` = 1
4. Assert DB: `status = 'ACTIVE'`
5. Assert audit log event `JourneyCreated`

**DB Assertion:**
```java
MotherJourney record = journeyRepo.findByAccountIdAndJourneyTypeAndStatus(
    ACC_002, JourneyType.PREGNANCY, JourneyStatus.ACTIVE
).orElseThrow();
assertThat(record.getStatus()).isEqualTo(JourneyStatus.ACTIVE);
assertThat(record.getJourneyType()).isEqualTo(JourneyType.PREGNANCY);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN | 🔵 REFACTOR note |
|-------|-----------|-----------------|----------|------------------|
| `JOURNEY-TC-001` | `JourneyServiceTest.java` | `[ ]` | `___` | — |
| `JOURNEY-TC-002` | `JourneyServiceTest.java` | `[ ]` | `___` | — |
| `JOURNEY-TC-003` | `JourneyControllerTest.java` | `[ ]` | `___` | — |
| `JOURNEY-TC-004` | `JourneyControllerTest.java` | `[ ]` | `___` | — |
| `JOURNEY-TC-SEC-001` | `JourneyControllerTest.java` | `[ ]` | `___` | — |
| `JOURNEY-TC-INT-001` | `CreateMotherJourneyIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol

```java
// Red Phase stub
@Service
public class JourneyService implements IJourneyService {
    @Override
    public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID accountId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

| TC ID | Expected | Actual | Root Cause (nếu PASS) |
|-------|----------|--------|----------------------|
| `JOURNEY-TC-001` | 🔴 FAIL | ☐ | — |
| `JOURNEY-TC-002` | 🔴 FAIL | ☐ | — |
| `JOURNEY-TC-INT-001` | 🔴 FAIL | ☐ | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS CB-JOURNEY-IMP-001 reviewed and approved
- [ ] Flyway V20 migration approved and tested on staging
- [ ] Test fixtures (FX-001 through FX-004) prepared

### Exit Criteria
- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [ ] Coverage ≥ 80% for JourneyService
- [ ] No business logic in JourneyController
- [ ] Audit log emitted for every successful create
- [ ] **Red Gate (§5.1)** — all tests FAIL with stub before implement
- [ ] **Oracle Source** — all expected values traceable to ADR/BR

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/journey/
git checkout -- src/main/resources/db/migration/V20__create_mother_journey.sql
git checkout -- src/test/java/com/carebridge/backend/journey/
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS mother_journeys CASCADE;"
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Check | Gate |
|-------|-------------|-------|------|
| AP-AI-001 | Unconstrained Generation | ☐ All TCs reference ADR/BR | G-0 |
| AP-AI-002 | Green-from-Birth | ☐ Tests FAIL with stub | G-2 ★ |
| AP-AI-003 | Implicit Decision | ☐ No architecture assumed without ADR | G-1 |
| AP-AI-004 | Layer Violation | ☐ Controller has no business logic | G-4 |
| AP-AI-005 | Hallucinated Contract | ☐ All imports exist in codebase | G-3 |
