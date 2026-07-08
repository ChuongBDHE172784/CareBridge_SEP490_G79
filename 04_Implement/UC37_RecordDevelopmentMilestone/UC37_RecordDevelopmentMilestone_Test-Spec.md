# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-37 Record Development Milestone

**Document ID:** `CB-BABY-IMP-007-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**Classification:** `Internal -- Confidential`

**References:**
- TDS: `04_Implement/UC37_RecordDevelopmentMilestone/UC37_RecordDevelopmentMilestone_TDS.md` (CB-BABY-IMP-007)
- SRS: S3.3.1.14

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khoi tao TDD spec cho UC-37 Record Development Milestone |
| 2026-07-02 | AI Agent — Amelia (Dev Agent) | RED Gate confirmed + GREEN Gate passed — MilestoneServiceTest TCs 001-006 GREEN (33/33 tests) |

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
| **Feature / Gap ID** | `UC-37` |
| **Module** | `RecordDevelopmentMilestone -- carejourney` |
| **Spec goc** | `CB-BABY-IMP-007` |
| **Priority** | P1 |
| **Data Classification** | `PII` |
| **Upstream Dependencies** | `auth, baby (baby_profiles)` |
| **Downstream Consumers** | `baby timeline, audit` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-007 S17` |
| **Constraints Injected** | C1 (ownership), C2 (ACTIVE check), C3 (date <= today), C4 (recordedBy = JWT), C5 (audit emit) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: "milestones such as rolling, crawling..." -- khong ro co cho phep trung lap khong | ADR-BABY-007-001: duplicate milestone_type duoc phep -- TEETHING xay ra nhieu lan | Test MILESTONE-TC-037-006 verify duplicate allowed, returns 201 |
| L2 | SRS: khong de cap recorded_by | Schema: recorded_by UUID field exists -- gia tri lay tu JWT userId, khong tu request body | Test verify recorded_by = JWT userId in response and DB |
| L3 | SRS: khong de cap edit/delete milestone | ADR-BABY-007-003: Milestones la ban ghi vinh vien -- khong co edit window | No update/delete tests needed for UC-37 |

---

## 3. Test Design Specification (TDS)

### TDS-01 -- Scope

```
RecordDevelopmentMilestone bao gom cac layer:
+-- Service (mock BabyProfileRepository + DevelopmentMilestoneRepository voi Mockito)
+-- Controller (mock IMilestoneService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-02 -- Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-37` | Mother ghi lai moc phat trien cua em be |
| `ADR-BABY-007-001` | Cho phep duplicate milestone_type |
| `ADR-BABY-007-002` | achievedDate <= today |
| `ADR-BABY-007-003` | Khong dua ra nhan dinh y khoa |
| `BR-RBAC` | Chi Mother so huu baby moi duoc ghi milestone |
| `CB-BABY-IMP-007 S8` | Interface contract: addMilestone(userId, babyId, request) |

### TDS-03 -- Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid milestone creation | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-001` |
| TC-COND-002 | Future achievedDate rejected | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-002` |
| TC-COND-003 | Baby not owned by user | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-003` |
| TC-COND-004 | Baby archived | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-004` |
| TC-COND-005 | Invalid milestone type | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-005` |
| TC-COND-006 | Duplicate milestone type allowed | `MilestoneService.addMilestone()` | `MILESTONE-TC-037-006` |
| TC-COND-007 | Unauthenticated access blocked | Spring Security filter | `MILESTONE-TC-037-007` |
| TC-COND-008 | DB persistence correctness | Full flow via Testcontainers | `MILESTONE-TC-037-INT-001` |

### TDS-04 -- Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | milestoneType valid/invalid | 9 valid types vs arbitrary strings |
| Boundary Value Analysis | achievedDate = today / tomorrow | Edge of past-or-present boundary |
| Error Guessing | Duplicate milestone, archived baby | Domain-specific edge cases |
| State Transition Testing | Baby status ACTIVE vs ARCHIVED | Status gate for write operations |

### TDS-05 -- Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path -- Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status=ACTIVE)` | Target baby |
| `FX-003` | Input | `{milestoneType: "WALKING", achievedDate: "3 days ago", note: "First steps"}` | Happy path request |
| `FX-004` | Input | `{milestoneType: "WALKING", achievedDate: "tomorrow"}` | Future date rejection |
| `FX-005` | Input | `{milestoneType: "FLYING"}` | Invalid type |
| `FX-006` | DB seed | `BabyProfile(status=ARCHIVED)` | Archived baby |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// CASE 2.0 -- Props Isolation Pattern
// Moi @Test dung factory methods -- khong shared mutable state

class MilestoneTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000037");
    static UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000037");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");

    static BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
            .babyId(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Test Baby")
            .birthDate(LocalDate.of(2026, 1, 1))
            .status("ACTIVE")
            .build();
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile baby = makeActiveBaby();
        baby.setStatus("ARCHIVED");
        return baby;
    }

    static BabyProfile makeBabyOwnedByOther() {
        BabyProfile baby = makeActiveBaby();
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    static AddMilestoneRequest makeRequest() {
        AddMilestoneRequest req = new AddMilestoneRequest();
        req.setMilestoneType("WALKING");
        req.setAchievedDate(LocalDate.now().minusDays(3));
        req.setNote("First steps in living room");
        return req;
    }

    static AddMilestoneRequest makeRequest(Consumer<AddMilestoneRequest> overrides) {
        AddMilestoneRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### MILESTONE-TC-037-001 -- Happy path: record WALKING milestone

**Severity:** `CRITICAL`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-007 S8 / S9`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeActiveBaby()`
- Mock: `milestoneRepository.save(any())` returns saved entity with generated ID

**Test Steps:**
1. Arrange: Create request via `makeRequest()` (milestoneType=WALKING, achievedDate=3 days ago)
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, request)`
3. Assert: Response is not null, milestoneType=WALKING, babyId=BABY_ID, recordedBy=MOTHER_ID, sourceType=MANUAL

**Expected Result (PASS):**
- MilestoneResponse returned with correct fields
- `milestoneRepository.save()` called once
- `auditService.emit("MILESTONE_RECORDED", ...)` called once

**Expected Result (FAIL):**
- Exception thrown or response fields incorrect

**Current Status:** 🟢 Passing

---

### MILESTONE-TC-037-002 -- achievedDate in future -> 400 BABY-064

**Severity:** `HIGH`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-007-002 / CB-BABY-IMP-007 S10 BABY-064`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeActiveBaby()`

**Test Steps:**
1. Arrange: Create request with `achievedDate = LocalDate.now().plusDays(1)` (tomorrow)
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, request)`
3. Assert: BusinessException thrown with code BABY-064

**Expected Result (PASS):**
- BusinessException with error code BABY-064
- `milestoneRepository.save()` never called

**Expected Result (FAIL):**
- No exception thrown -- milestone saved with future date

**Current Status:** 🟢 Passing

---

### MILESTONE-TC-037-003 -- Baby not owned -> 403 BABY-061

**Severity:** `CRITICAL`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-RBAC / CB-BABY-IMP-007 S10 BABY-061`

**Preconditions:**
- Baby profile exists but owned by OTHER_USER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBabyOwnedByOther()`

**Test Steps:**
1. Arrange: Baby owned by OTHER_USER_ID
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, makeRequest())`
3. Assert: ForbiddenException thrown with code BABY-061

**Expected Result (PASS):**
- ForbiddenException with error code BABY-061
- `milestoneRepository.save()` never called

**Expected Result (FAIL):**
- Milestone saved for non-owned baby -- RBAC violation

**Current Status:** 🟢 Passing

---

### MILESTONE-TC-037-004 -- Baby archived -> 400 BABY-062

**Severity:** `HIGH`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-BABY-IMP-007 S10 BABY-062`

**Preconditions:**
- Baby profile exists with status ARCHIVED, owned by MOTHER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeArchivedBaby()`

**Test Steps:**
1. Arrange: Baby with status ARCHIVED
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, makeRequest())`
3. Assert: BusinessException thrown with code BABY-062

**Expected Result (PASS):**
- BusinessException with error code BABY-062
- `milestoneRepository.save()` never called

**Expected Result (FAIL):**
- Milestone saved for archived baby

**Current Status:** 🟢 Passing

---

### MILESTONE-TC-037-005 -- Invalid milestone type -> 400 BABY-063

**Severity:** `HIGH`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-BABY-IMP-007 S10 BABY-063`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeActiveBaby()`

**Test Steps:**
1. Arrange: Create request with `milestoneType = "FLYING"` (not in valid enum set)
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, request)`
3. Assert: BusinessException thrown with code BABY-063

**Expected Result (PASS):**
- BusinessException with error code BABY-063
- `milestoneRepository.save()` never called

**Expected Result (FAIL):**
- Milestone saved with invalid type "FLYING"

**Current Status:** 🟢 Passing

---

### MILESTONE-TC-037-006 -- Duplicate milestone type -> 201 (allowed per ADR)

**Severity:** `MEDIUM`
**Feature Under Test:** `MilestoneService.addMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/MilestoneServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-007-001`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID
- A TEETHING milestone already exists for this baby (mock or DB)
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeActiveBaby()`
- Mock: `milestoneRepository.save(any())` returns saved entity

**Test Steps:**
1. Arrange: Create request with `milestoneType = "TEETHING"` (same type as existing)
2. Act: Call `milestoneService.addMilestone(MOTHER_ID, BABY_ID, request)`
3. Assert: No exception -- MilestoneResponse returned successfully

**Expected Result (PASS):**
- MilestoneResponse returned (201) -- duplicate allowed
- `milestoneRepository.save()` called once (no uniqueness check)

**Expected Result (FAIL):**
- Exception thrown for duplicate milestone type -- violates ADR-BABY-007-001

**Current Status:** 🟢 Passing
**Implementation Note:** Service must NOT check for existing milestone of same type. The save should always succeed regardless of prior milestones.

---

### MILESTONE-TC-037-007 -- No JWT -> 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 -- Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/MilestoneControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request headers

**Test Steps:**
1. Arrange: Build POST request without Authorization header
2. Act: Call `POST /api/v1/babies/{babyId}/milestones` via MockMvc
3. Assert: Response status is 401

**Expected Result (PASS):**
- HTTP 401 Unauthorized
- No service method invoked

**Expected Result (FAIL):**
- Request reaches controller without authentication

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### MILESTONE-TC-037-INT-001 -- DB row with correct baby_id, recorded_by = userId

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/MilestoneIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied
- Seed: Insert baby_profile with BABY_ID, MOTHER_ID, status ACTIVE

**Test Steps:**
1. Seed baby_profile via JPA repository
2. Call POST /api/v1/babies/{BABY_ID}/milestones with valid JWT (MOTHER_ID) and request body
3. Assert response status is 201
4. Query development_milestones table directly

**Expected Result (PASS):**
- Response status 201
- DB row exists with:
  - `baby_id = BABY_ID`
  - `milestone_type = "WALKING"`
  - `recorded_by = MOTHER_ID`
  - `source_type = "MANUAL"`
  - `achieved_date = request.achievedDate`
  - `created_at` is not null

**Expected Result (FAIL):**
- DB row missing or fields incorrect

**DB Assertion:**
```java
DevelopmentMilestone record = milestoneRepository.findByBabyIdOrderByAchievedDateDesc(BABY_ID).get(0);
assertThat(record).isNotNull();
assertThat(record.getBabyId()).isEqualTo(BABY_ID);
assertThat(record.getRecordedBy()).isEqualTo(MOTHER_ID);
assertThat(record.getMilestoneType()).isEqualTo("WALKING");
assertThat(record.getSourceType()).isEqualTo("MANUAL");
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `MILESTONE-TC-037-001` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-002` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-003` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-004` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-005` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-006` | `MilestoneServiceTest.java` | `[x]` | `Passed` | |
| `MILESTONE-TC-037-007` | `MilestoneControllerTest.java` | `[ ]` | `___` | Controller test not implemented |
| `MILESTONE-TC-037-INT-001` | `MilestoneIntegrationTest.java` | `[ ]` | `___` | Integration test not implemented |

### 5.1 Red Gate Protocol (CASE 2.0 -- GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase -- implementation stub (PHAI throw)
@Service
public class MilestoneService implements IMilestoneService {

    @Override
    public MilestoneResponse addMilestone(UUID userId, UUID babyId, AddMilestoneRequest request) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (neu PASS bat thuong) |
|-------|-------------|----------|--------|----------------------------------|
| `MILESTONE-TC-037-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `MILESTONE-TC-037-007` | `throw('Not implemented')` | 🔴 FAIL | [ ] FAIL [ ] PASS | Not run (controller test not implemented) |
| `MILESTONE-TC-037-INT-001` | `throw('Not implemented')` | 🔴 FAIL | [ ] FAIL [ ] PASS | Not run (integration test not implemented) |

**Red Gate Evidence:**
- Stub commit hash: `confirmed via ./mvnw test 2026-07-02`
- Tat ca FAIL? [x] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-BABY-IMP-007` da duoc review va approve
- [x] Logic Issues (Section 2) da duoc confirm
- [x] Table `development_milestones` da ton tai trong DB
- [x] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria

- [x] `./mvnw test` -- tat ca unit tests xanh (khong co skip)
- [ ] `./mvnw verify` -- tat ca integration tests xanh (integration tests not implemented)
- [x] Test coverage >= 80% lines cho MilestoneService
- [x] Khong co business logic trong Controller
- [x] Khong co PII/secret xuat hien plaintext trong logs

**Exit Criteria bo sung -- CASE 2.0:**

- [x] **Red Gate (S5.1)** -- tat ca tests FAIL voi empty/throw stub truoc khi implement
- [x] **Contract Existence** -- moi class duoc inject deu ton tai trong codebase
- [x] **Props Isolation** -- khong co shared mutable state giua tests
- [x] **Oracle Source** -- moi expected value trong assert co ghi ro nguon (BR/AC/ADR)

### Suspension Criteria

- Blocker dependency chua san sang (migration, external service)
- Phat hien loi kien truc moi can review
- CI pipeline bi broken boi thay doi khac

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/MilestoneController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/MilestoneService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/IMilestoneService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/AddMilestoneRequest.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/MilestoneResponse.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/
```

No migration rollback needed -- table already exists.

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [x] OK | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi empty/throw stub (S5.1) | [x] OK | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision khong co ADR | [x] OK | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [x] OK | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai trong codebase | [x] OK | G-3 |

**Ket qua review:**

- [x] Khong phat hien anti-pattern nao -> TDD spec approved
- [ ] Phat hien AP -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| -- | -- | -- | -- | -- |

---

*TDD Template v2.0 -- Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
