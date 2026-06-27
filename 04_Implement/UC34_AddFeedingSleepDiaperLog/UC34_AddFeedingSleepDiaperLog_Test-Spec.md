# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-34 Add Feeding Sleep Diaper Log

**Document ID:** `CB-BABY-IMP-004-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC34_AddFeedingSleepDiaperLog/UC34_AddFeedingSleepDiaperLog_TDS.md` (CB-BABY-IMP-004)
- SRS: S3.3.1.11

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khoi tao TDD spec cho UC-34 Add Feeding Sleep Diaper Log |

---

## MUC LUC

1. [Thong tin Module](#1-thong-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thong tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-34` |
| **Module** | `AddFeedingSleepDiaperLog — carejourney` |
| **Spec goc** | `CB-BABY-IMP-004` |
| **Priority** | P0 |
| **Data Classification** | `PII` |
| **Upstream Dependencies** | `auth, UC-31 (baby_profiles), baby_daily_logs table` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-004 S17` |
| **Constraints Injected** | C1 (ownership via baby_profiles), C2 (ACTIVE check), C3 (recorded_by from JWT), C4 (audit), C5 (log_type enum), C6 (FEEDING unit) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: does not specify who recorded_by should be | ADR-BABY-008: recorded_by MUST be JWT userId, not from request body | Test verifies recorded_by = JWT userId, ignores request body value |
| L2 | SRS: does not specify uniqueness per day per type | ADR-BABY-007: multiple log types per day allowed, no unique constraint | Test verifies multiple FEEDING logs on same day succeed |
| L3 | SRS: does not specify unit requirement for FEEDING | BR-BABY-036: FEEDING + quantity -> unit required | Test verifies 400 when FEEDING has quantity but no unit |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
AddFeedingSleepDiaperLog bao gom cac layer:
+-- Service (mock BabyProfileRepository + BabyDailyLogRepository voi Mockito)
+-- Controller (mock BabyDailyLogService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | FEEDING log with quantity + unit | `BabyDailyLogService.addDailyLog()` | `BABY-TC-034-001` |
| TC-COND-002 | SLEEP log with started_at + ended_at | `BabyDailyLogService.addDailyLog()` | `BABY-TC-034-002` |
| TC-COND-003 | DIAPER minimal (just log_type) | `BabyDailyLogService.addDailyLog()` | `BABY-TC-034-003` |
| TC-COND-004 | FEVER log with temperature | `BabyDailyLogService.addDailyLog()` | `BABY-TC-034-004` |
| TC-COND-005 | Baby not owned | `BabyDailyLogService.checkOwnership()` | `BABY-TC-034-005` |
| TC-COND-006 | Baby archived | `BabyDailyLogService.checkActiveStatus()` | `BABY-TC-034-006` |
| TC-COND-007 | Invalid log_type | DTO validation | `BABY-TC-034-007` |
| TC-COND-008 | No JWT | Spring Security filter | `BABY-TC-034-008` |
| TC-COND-009 | DB persistence verified | Full flow | `BABY-TC-034-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | log_type: valid 6 types vs invalid | Enum validation |
| Equivalence Partitioning | Baby status: ACTIVE vs ARCHIVED | State-dependent behavior |
| Boundary Value Analysis | Ownership: match vs mismatch | Binary authorization check |
| Error Guessing | Missing JWT, recorded_by spoofing | Security vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: MOTHER_ID, role: 'MOTHER'}` | Happy path auth |
| `FX-002` | DB seed | `{babyId: BABY_ID, ownerUserId: MOTHER_ID, status: 'ACTIVE'}` | Active baby |
| `FX-003` | DB seed | `{babyId: ARCHIVED_BABY_ID, ownerUserId: MOTHER_ID, status: 'ARCHIVED'}` | Archived baby |
| `FX-004` | DB seed | `{babyId: OTHER_BABY_ID, ownerUserId: OTHER_MOTHER_ID, status: 'ACTIVE'}` | Baby owned by another |
| `FX-005` | Input | `{logType: "FEEDING", quantity: 120, unit: "ml"}` | FEEDING request |
| `FX-006` | Input | `{logType: "SLEEP", startedAt: "...", endedAt: "..."}` | SLEEP request |
| `FX-007` | Input | `{logType: "DIAPER"}` | Minimal DIAPER request |
| `FX-008` | Input | `{logType: "FEVER", quantity: 38.5, unit: "celsius"}` | FEVER request |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// BabyDailyLogTestFactory.java
class BabyDailyLogTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000034");
    static final UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000034");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID OTHER_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");
    static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");

    static BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
            .babyId(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Baby Test")
            .status("ACTIVE")
            .build();
    }

    static BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
            .babyId(ARCHIVED_BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Archived Baby")
            .status("ARCHIVED")
            .build();
    }

    static BabyProfile makeOtherMotherBaby() {
        return BabyProfile.builder()
            .babyId(OTHER_BABY_ID)
            .ownerUserId(OTHER_MOTHER_ID)
            .nickname("Other Baby")
            .status("ACTIVE")
            .build();
    }

    static AddBabyDailyLogRequest makeFeedingRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("FEEDING");
        req.setQuantity(new BigDecimal("120"));
        req.setUnit("ml");
        return req;
    }

    static AddBabyDailyLogRequest makeSleepRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("SLEEP");
        req.setStartedAt(Instant.parse("2026-06-26T13:00:00Z"));
        req.setEndedAt(Instant.parse("2026-06-26T15:30:00Z"));
        return req;
    }

    static AddBabyDailyLogRequest makeDiaperRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("DIAPER");
        return req;
    }

    static AddBabyDailyLogRequest makeFeverRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("FEVER");
        req.setQuantity(new BigDecimal("38.5"));
        req.setUnit("celsius");
        req.setNote("Mild fever after vaccination");
        return req;
    }
}
```

---

### BABY-TC-034-001 — Happy path FEEDING log -> 201

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogService.addDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-34 Normal Flow, ADR-BABY-007`

**Preconditions:**
- Mother authenticated (FX-001)
- Baby profile exists with status ACTIVE and owner = MOTHER_ID (FX-002)

**Test Steps:**
1. Arrange: Mock `babyProfileRepository.findById(BABY_ID)` -> returns FX-002 active baby
2. Arrange: Mock `babyDailyLogRepository.save()` -> returns saved log with generated babyLogId
3. Act: Call `service.addDailyLog(BABY_ID, makeFeedingRequest(), MOTHER_ID)`
4. Assert: Response logType = "FEEDING", quantity = 120, unit = "ml"
5. Assert: recorded_by in saved entity = MOTHER_ID (from JWT, not request)
6. Assert: `auditService.emit(BABY_LOG_ADDED)` called once

**Expected Result (PASS):**
- Returns AddBabyDailyLogResponse with correct FEEDING data
- recorded_by = MOTHER_ID
- Audit event emitted

**Expected Result (FAIL):**
- Exception thrown, or recorded_by not set to JWT userId, or audit not emitted

**Current Status:** RED — Not written

---

### BABY-TC-034-002 — SLEEP log with started_at + ended_at -> 201

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogService.addDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC-34 Normal Flow`

**Preconditions:**
- Mother authenticated, baby ACTIVE and owned

**Test Steps:**
1. Arrange: Mock baby profile exists, ACTIVE, owned
2. Act: Call `service.addDailyLog(BABY_ID, makeSleepRequest(), MOTHER_ID)`
3. Assert: Response logType = "SLEEP"
4. Assert: startedAt = "2026-06-26T13:00:00Z", endedAt = "2026-06-26T15:30:00Z"

**Expected Result (PASS):**
- Returns response with SLEEP log and time window preserved
- recorded_by = MOTHER_ID

**Current Status:** RED — Not written

---

### BABY-TC-034-003 — DIAPER log minimal (just log_type) -> 201

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogService.addDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007 (quick-entry design)`

**Preconditions:**
- Mother authenticated, baby ACTIVE and owned

**Test Steps:**
1. Arrange: Mock baby profile exists, ACTIVE, owned
2. Act: Call `service.addDailyLog(BABY_ID, makeDiaperRequest(), MOTHER_ID)`
3. Assert: Response logType = "DIAPER"
4. Assert: quantity = null, unit = null, startedAt = null, endedAt = null (all optional fields null)

**Expected Result (PASS):**
- Returns response with DIAPER log, all optional fields null
- Quick-entry design — minimal required fields honored

**Current Status:** RED — Not written

---

### BABY-TC-034-004 — FEVER log with temperature -> 201

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogService.addDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC-34 Normal Flow`

**Preconditions:**
- Mother authenticated, baby ACTIVE and owned

**Test Steps:**
1. Arrange: Mock baby profile exists, ACTIVE, owned
2. Act: Call `service.addDailyLog(BABY_ID, makeFeverRequest(), MOTHER_ID)`
3. Assert: Response logType = "FEVER", quantity = 38.5, unit = "celsius"
4. Assert: note = "Mild fever after vaccination"

**Expected Result (PASS):**
- Returns response with FEVER log data preserved

**Current Status:** RED — Not written

---

### BABY-TC-034-005 — Baby not owned -> 403 BABY-031

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `BabyDailyLogService.checkOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-BABY-031, BR-RBAC`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists but owned by OTHER_MOTHER_ID (FX-004)

**Test Steps:**
1. Arrange: Mock `babyProfileRepository.findById(OTHER_BABY_ID)` -> returns FX-004
2. Act: Call `service.addDailyLog(OTHER_BABY_ID, makeFeedingRequest(), MOTHER_ID)`
3. Assert: Throws exception with error code BABY-031

**Expected Result (PASS):**
- Throws ForbiddenException with code BABY-031
- `babyDailyLogRepository.save()` NOT called

**Expected Result (FAIL):**
- Log created for baby owned by another user (access control bypass)

**Current Status:** RED — Not written

---

### BABY-TC-034-006 — Baby archived -> 400 BABY-032

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogService.checkActiveStatus()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-BABY-032`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists with status ARCHIVED and owner = MOTHER_ID (FX-003)

**Test Steps:**
1. Arrange: Mock `babyProfileRepository.findById(ARCHIVED_BABY_ID)` -> returns FX-003
2. Act: Call `service.addDailyLog(ARCHIVED_BABY_ID, makeFeedingRequest(), MOTHER_ID)`
3. Assert: Throws exception with error code BABY-032

**Expected Result (PASS):**
- Throws BadRequestException with code BABY-032
- `babyDailyLogRepository.save()` NOT called

**Current Status:** RED — Not written

---

### BABY-TC-034-007 — Invalid log_type -> 400 BABY-033

**Severity:** `HIGH`
**Feature Under Test:** `DTO validation (@Pattern on logType)`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/BabyDailyLogControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-BABY-033`

**Preconditions:**
- Mother authenticated

**Test Steps:**
1. Arrange: @WebMvcTest with JWT mock
2. Act: POST /api/v1/babies/{babyId}/daily-logs with body `{"logType": "INVALID"}`
3. Assert: Response status 400 with error code BABY-033

**Expected Result (PASS):**
- 400 Bad Request with BABY-033
- Service layer NOT reached

**Current Status:** RED — Not written

---

### BABY-TC-034-008 — No JWT -> 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/BabyDailyLogControllerTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request

**Test Steps:**
1. Arrange: @WebMvcTest with no JWT mock
2. Act: POST /api/v1/babies/{babyId}/daily-logs without Authorization header
3. Assert: Response status 401

**Expected Result (PASS):**
- 401 Unauthorized

**Current Status:** RED — Not written

---

### INTEGRATION TEST CASES

---

### BABY-TC-034-INT-001 — Full DB: correct baby_id, log_type, recorded_by

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> DB`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/BabyDailyLogIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL container running (@Testcontainers)
- Flyway migration applied
- Seed: Insert baby profile with BABY_ID, MOTHER_ID, status=ACTIVE

**Test Steps:**
1. Seed baby profile via repository
2. POST /api/v1/babies/{BABY_ID}/daily-logs with JWT(MOTHER_ID) and body `{"logType":"FEEDING","quantity":120,"unit":"ml"}`
3. Assert response 201
4. Assert DB: baby_daily_logs row exists with baby_id = BABY_ID
5. Assert DB: log_type = 'FEEDING'
6. Assert DB: recorded_by = MOTHER_ID (from JWT, not request body)
7. Assert DB: quantity = 120, unit = 'ml'

**Expected Result (PASS):**
- Database row created with correct baby_id, log_type, recorded_by
- recorded_by matches JWT userId (not client-supplied value)

**DB Assertion:**
```java
List<BabyDailyLog> logs = dailyLogRepo.findByBabyId(BABY_ID);
assertThat(logs).hasSize(1);

BabyDailyLog log = logs.get(0);
assertThat(log.getBabyId()).isEqualTo(BABY_ID);
assertThat(log.getLogType()).isEqualTo("FEEDING");
assertThat(log.getRecordedBy()).isEqualTo(MOTHER_ID);
assertThat(log.getQuantity()).isEqualByComparingTo(new BigDecimal("120"));
assertThat(log.getUnit()).isEqualTo("ml");
```

**Current Status:** RED — Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `BABY-TC-034-001` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-002` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-003` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-004` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-005` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-006` | `BabyDailyLogServiceTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-007` | `BabyDailyLogControllerTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-008` | `BabyDailyLogControllerTest.java` | `[ ]` | `___` | — |
| `BABY-TC-034-INT-001` | `BabyDailyLogIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class BabyDailyLogService implements IBabyDailyLogService {

    @Override
    public AddBabyDailyLogResponse addDailyLog(UUID babyId, AddBabyDailyLogRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause |
|-------|-------------|----------|--------|------------|
| `BABY-TC-034-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-006` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-007` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-008` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-034-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tat ca FAIL? [ ] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-004` da duoc review va approve
- [ ] baby_profiles table exists (from UC-31 migration)
- [ ] baby_daily_logs table exists
- [ ] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria
- [ ] `./mvnw test` — tat ca unit tests xanh
- [ ] `./mvnw verify` — tat ca integration tests xanh
- [ ] Test coverage >= 80% cho BabyDailyLogService
- [ ] Khong co business logic trong Controller
- [ ] recorded_by duoc set tu JWT userId (khong tu request body)
- [ ] Tat ca 6 log_type (FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE) duoc test
- [ ] Red Gate (S5.1) — tat ca tests FAIL voi stub truoc khi implement
- [ ] FEEDING + quantity -> unit required duoc test

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/BabyDailyLogService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/repository/BabyDailyLogRepository.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/entity/BabyDailyLog.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/AddBabyDailyLogRequest.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/AddBabyDailyLogResponse.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/service/BabyDailyLogServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/controller/BabyDailyLogControllerTest.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/BabyDailyLogIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [ ] | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi stub (S5.1) | [ ] | G-2 |
| AP-AI-003 | Implicit Decision | Test accept recorded_by tu request body | [ ] | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [ ] | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai | [ ] | G-3 |

**Ket qua review:**
- [ ] Khong phat hien anti-pattern nao -> TDD spec approved
- [ ] Phat hien AP -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| | | | | |

---

*TDD Template v2.0 — Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
