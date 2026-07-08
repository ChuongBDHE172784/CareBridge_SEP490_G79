# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-38 View Growth Chart

**Document ID:** `CB-BABY-IMP-008-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**Classification:** `Internal -- Confidential`

**References:**
- TDS: `04_Implement/UC38_ViewGrowthChart/UC38_ViewGrowthChart_TDS.md` (CB-BABY-IMP-008)
- SRS: S3.3.1.15

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khoi tao TDD spec cho UC-38 View Growth Chart |
| 2026-07-04 | AI Agent — Amelia (Dev Agent) | GREEN Gate: 5/5 service unit tests PASS (TC-001 to TC-005). TC-006 (controller) and INT-001 (integration) not yet implemented. |

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
| **Feature / Gap ID** | `UC-38` |
| **Module** | `ViewGrowthChart -- carejourney` |
| **Spec goc** | `CB-BABY-IMP-008` |
| **Priority** | P1 |
| **Data Classification** | `PII` |
| **Upstream Dependencies** | `auth, baby (baby_profiles), growth_measurements` |
| **Downstream Consumers** | `Mobile app chart rendering` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-008 S17` |
| **Constraints Injected** | C1 (ownership), C2 (ARCHIVED allowed), C3 (sorted ASC), C4 (ageInDays in service), C5 (no medical interpretation) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: "Displays baby weight, height, head circumference" -- khong ro xu ly baby ARCHIVED | ADR-BABY-008-002: ARCHIVED baby CAN be viewed -- khac voi UC32/UC34 yeu cau ACTIVE | Test GROWTH-TC-038-003 verify archived baby returns 200 |
| L2 | SRS: khong de cap truong hop chua co measurements | Schema: growth_measurements co the rong cho baby moi | Test GROWTH-TC-038-002 verify empty list = 200, NOT 404 |
| L3 | SRS: khong de cap cach tinh tuoi | ageInDays computed tu baby.birthDate -- neu birthDate null thi ageInDays = 0 | Test verify ageInDays calculation and null birthDate handling |
| L4 | SRS: "with prompts to ask an expert" | "Ask an expert" la client-side UI, khong phai backend logic | Khong co test cho feature nay -- backend chi tra ve du lieu |

---

## 3. Test Design Specification (TDS)

### TDS-01 -- Scope

```
ViewGrowthChart bao gom cac layer:
+-- Service (mock BabyProfileRepository + GrowthMeasurementRepository voi Mockito)
+-- Controller (mock IGrowthService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-02 -- Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-38` | Mother xem bieu do tang truong cua em be |
| `ADR-BABY-008-001` | Tra ve du lieu tho, client render chart |
| `ADR-BABY-008-002` | ARCHIVED baby CAN be viewed |
| `ADR-BABY-008-003` | Khong dua ra nhan dinh y khoa |
| `BR-RBAC` | Chi Mother so huu baby moi duoc xem |
| `CB-BABY-IMP-008 S8` | Interface contract: getGrowthChart(userId, babyId) |

### TDS-03 -- Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid growth chart retrieval with measurements | `GrowthService.getGrowthChart()` | `GROWTH-TC-038-001` |
| TC-COND-002 | No measurements returns empty list (not 404) | `GrowthService.getGrowthChart()` | `GROWTH-TC-038-002` |
| TC-COND-003 | Archived baby allowed for growth chart view | `GrowthService.getGrowthChart()` | `GROWTH-TC-038-003` |
| TC-COND-004 | Baby not owned by user | `GrowthService.getGrowthChart()` | `GROWTH-TC-038-004` |
| TC-COND-005 | Baby not found | `GrowthService.getGrowthChart()` | `GROWTH-TC-038-005` |
| TC-COND-006 | Unauthenticated access blocked | Spring Security filter | `GROWTH-TC-038-006` |
| TC-COND-007 | DB sorted and ageInDays calculated | Full flow via Testcontainers | `GROWTH-TC-038-INT-001` |

### TDS-04 -- Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Baby status ACTIVE vs ARCHIVED | Both should return 200 |
| Boundary Value Analysis | Empty measurements list | Edge: 0 measurements vs N measurements |
| State Transition Testing | Baby status gate (none for this UC) | Verify no status gate applied |
| Error Guessing | Null birthDate for ageInDays | Defensive edge case |

### TDS-05 -- Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path -- Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status=ACTIVE, birthDate=2026-01-15)` | Target baby |
| `FX-003` | DB seed | 2 GrowthMeasurement records for BABY_ID | Measurement data |
| `FX-004` | DB seed | `BabyProfile(status=ARCHIVED)` | Archived baby test |
| `FX-005` | DB seed | BabyProfile with no growth_measurements | Empty list test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// CASE 2.0 -- Props Isolation Pattern
// Moi @Test dung factory methods -- khong shared mutable state

class GrowthChartTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000038");
    static UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000038");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");

    static BabyProfile makeBaby() {
        return BabyProfile.builder()
            .babyId(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Growth Baby")
            .birthDate(LocalDate.of(2026, 1, 15))
            .status("ACTIVE")
            .build();
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile baby = makeBaby();
        baby.setStatus("ARCHIVED");
        return baby;
    }

    static BabyProfile makeBabyOwnedByOther() {
        BabyProfile baby = makeBaby();
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    static BabyProfile makeBabyWithNullBirthDate() {
        BabyProfile baby = makeBaby();
        baby.setBirthDate(null);
        return baby;
    }

    static List<GrowthMeasurement> makeMeasurements() {
        return List.of(
            GrowthMeasurement.builder()
                .growthMeasurementId(UUID.fromString("aaaa0001-0000-0000-0000-000000000001"))
                .babyId(BABY_ID)
                .measuredDate(LocalDate.of(2026, 2, 15))
                .weightKg(new BigDecimal("4.2"))
                .heightCm(new BigDecimal("52"))
                .headCircumferenceCm(new BigDecimal("35"))
                .note("1 month checkup")
                .build(),
            GrowthMeasurement.builder()
                .growthMeasurementId(UUID.fromString("aaaa0002-0000-0000-0000-000000000002"))
                .babyId(BABY_ID)
                .measuredDate(LocalDate.of(2026, 3, 15))
                .weightKg(new BigDecimal("5.1"))
                .heightCm(new BigDecimal("55"))
                .headCircumferenceCm(new BigDecimal("37"))
                .note("2 month checkup")
                .build()
        );
    }

    static List<GrowthMeasurement> makeMeasurements(Consumer<List<GrowthMeasurement>> overrides) {
        List<GrowthMeasurement> list = new ArrayList<>(makeMeasurements());
        overrides.accept(list);
        return list;
    }
}
```

---

### GROWTH-TC-038-001 -- Happy path: GET growth chart with measurements list

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.getGrowthChart()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-008 S8 / S9`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID, birthDate = 2026-01-15
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()`
- Mock: `growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID)` returns `makeMeasurements()`

**Test Steps:**
1. Arrange: Create baby via `makeBaby()`, measurements via `makeMeasurements()`
2. Act: Call `growthService.getGrowthChart(MOTHER_ID, BABY_ID)`
3. Assert: Response is not null
   - babyId = BABY_ID
   - nickname = "Growth Baby"
   - birthDate = 2026-01-15
   - measurements.size() = 2
   - measurements[0].measuredDate = 2026-02-15 (first in ASC order)
   - measurements[0].ageInDays = 31
   - measurements[1].measuredDate = 2026-03-15
   - measurements[1].ageInDays = 59

**Expected Result (PASS):**
- GrowthChartResponse returned with 2 measurements sorted ASC
- ageInDays calculated correctly from birthDate

**Expected Result (FAIL):**
- Exception thrown or measurements unsorted or ageInDays wrong

**Current Status:** 🟢 Passing

---

### GROWTH-TC-038-002 -- No measurements yet -> 200 with empty measurements array

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.getGrowthChart()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-BABY-IMP-008 S9 (200 OK with empty list)`

**Preconditions:**
- Baby profile exists with status ACTIVE, owned by MOTHER_ID
- Mock: `growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID)` returns empty list

**Test Steps:**
1. Arrange: Baby exists, no measurements
2. Act: Call `growthService.getGrowthChart(MOTHER_ID, BABY_ID)`
3. Assert: Response is not null, measurements list is empty (not null), no exception thrown

**Expected Result (PASS):**
- GrowthChartResponse with measurements = [] (empty list)
- No exception -- 200 OK semantics

**Expected Result (FAIL):**
- ResourceNotFoundException thrown (treating empty data as "not found")
- measurements field is null instead of empty list

**Current Status:** 🟢 Passing
**Implementation Note:** Service must return empty list, never null. Empty data is valid -- baby just has no measurements yet.

---

### GROWTH-TC-038-003 -- Archived baby -> 200 (allowed for growth chart viewing)

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.getGrowthChart()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-008-002`

**Preconditions:**
- Baby profile exists with status ARCHIVED, owned by MOTHER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeArchivedBaby()`
- Mock: `growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID)` returns `makeMeasurements()`

**Test Steps:**
1. Arrange: Baby with status ARCHIVED and existing measurements
2. Act: Call `growthService.getGrowthChart(MOTHER_ID, BABY_ID)`
3. Assert: Response returned successfully with measurements -- no exception

**Expected Result (PASS):**
- GrowthChartResponse with measurements (no status gate for read-only)
- Differs from UC32/UC34 which require ACTIVE

**Expected Result (FAIL):**
- BusinessException thrown for ARCHIVED baby -- violates ADR-BABY-008-002

**Current Status:** 🟢 Passing

---

### GROWTH-TC-038-004 -- Baby not owned -> 403 BABY-071

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.getGrowthChart()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-RBAC / CB-BABY-IMP-008 S10 BABY-071`

**Preconditions:**
- Baby profile exists but owned by OTHER_USER_ID
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBabyOwnedByOther()`

**Test Steps:**
1. Arrange: Baby owned by OTHER_USER_ID
2. Act: Call `growthService.getGrowthChart(MOTHER_ID, BABY_ID)`
3. Assert: ForbiddenException thrown with code BABY-071

**Expected Result (PASS):**
- ForbiddenException with error code BABY-071
- No data returned -- RBAC enforced

**Expected Result (FAIL):**
- Growth chart data returned for non-owned baby -- RBAC violation

**Current Status:** 🟢 Passing

---

### GROWTH-TC-038-005 -- Baby not found -> 404 BABY-070

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.getGrowthChart()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-BABY-IMP-008 S10 BABY-070`

**Preconditions:**
- Mock: `babyProfileRepository.findById(NON_EXISTENT_BABY_ID)` returns empty

**Test Steps:**
1. Arrange: Non-existent babyId
2. Act: Call `growthService.getGrowthChart(MOTHER_ID, NON_EXISTENT_BABY_ID)`
3. Assert: ResourceNotFoundException thrown with code BABY-070

**Expected Result (PASS):**
- ResourceNotFoundException with error code BABY-070

**Expected Result (FAIL):**
- NullPointerException or other unhandled error

**Current Status:** 🟢 Passing

---

### GROWTH-TC-038-006 -- No JWT -> 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 -- Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthChartControllerTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request headers

**Test Steps:**
1. Arrange: Build GET request without Authorization header
2. Act: Call `GET /api/v1/babies/{babyId}/growth-chart` via MockMvc
3. Assert: Response status is 401

**Expected Result (PASS):**
- HTTP 401 Unauthorized
- No service method invoked

**Expected Result (FAIL):**
- Request reaches controller without authentication

**Current Status:** RED Not written

---

### INTEGRATION TEST CASES

---

### GROWTH-TC-038-INT-001 -- Integration: seed 3 growth measurements, verify sorted ASC, ageInDays correct

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/GrowthChartIntegrationTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied
- Seed: Insert baby_profile with BABY_ID, MOTHER_ID, birthDate=2026-01-15, status=ACTIVE
- Seed: Insert 3 growth_measurements with measured_dates: 2026-04-15, 2026-02-15, 2026-03-15 (intentionally out of order)

**Test Steps:**
1. Seed baby_profile via JPA repository
2. Seed 3 growth_measurements via JPA repository (INSERT in reverse order to test sort)
3. Call GET /api/v1/babies/{BABY_ID}/growth-chart with valid JWT (MOTHER_ID)
4. Assert response

**Expected Result (PASS):**
- Response status 200
- `measurements.size() == 3`
- Measurements sorted by measured_date ASC:
  - [0] measuredDate = 2026-02-15, ageInDays = 31
  - [1] measuredDate = 2026-03-15, ageInDays = 59
  - [2] measuredDate = 2026-04-15, ageInDays = 90
- Each measurement has weightKg, heightCm, headCircumferenceCm values
- babyId = BABY_ID, nickname = "Growth Baby"

**Expected Result (FAIL):**
- Measurements not sorted
- ageInDays calculated incorrectly
- Missing fields in response

**DB Assertion:**
```java
// Verify via API response (integration test uses real DB)
GrowthChartResponse response = objectMapper.readValue(
    result.getResponse().getContentAsString(),
    new TypeReference<ApiResponse<GrowthChartResponse>>() {}
).getData();

assertThat(response.getMeasurements()).hasSize(3);
assertThat(response.getMeasurements().get(0).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 2, 15));
assertThat(response.getMeasurements().get(0).getAgeInDays()).isEqualTo(31);
assertThat(response.getMeasurements().get(1).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 3, 15));
assertThat(response.getMeasurements().get(1).getAgeInDays()).isEqualTo(59);
assertThat(response.getMeasurements().get(2).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 4, 15));
assertThat(response.getMeasurements().get(2).getAgeInDays()).isEqualTo(90);
```

**Current Status:** RED Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `GROWTH-TC-038-001` | `GrowthServiceTest.java` | `[x]` | `Passed` | |
| `GROWTH-TC-038-002` | `GrowthServiceTest.java` | `[x]` | `Passed` | |
| `GROWTH-TC-038-003` | `GrowthServiceTest.java` | `[x]` | `Passed` | |
| `GROWTH-TC-038-004` | `GrowthServiceTest.java` | `[x]` | `Passed` | |
| `GROWTH-TC-038-005` | `GrowthServiceTest.java` | `[x]` | `Passed` | |
| `GROWTH-TC-038-006` | `GrowthChartControllerTest.java` | `[ ]` | `___` | Controller test not implemented |
| `GROWTH-TC-038-INT-001` | `GrowthChartIntegrationTest.java` | `[ ]` | `___` | Integration test not implemented |

### 5.1 Red Gate Protocol (CASE 2.0 -- GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase -- implementation stub (PHAI throw)
@Service
public class GrowthService implements IGrowthService {

    @Override
    public GrowthChartResponse getGrowthChart(UUID userId, UUID babyId) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (neu PASS bat thuong) |
|-------|-------------|----------|--------|----------------------------------|
| `GROWTH-TC-038-001` | `throw('Not implemented')` | FAIL | ☑ FAIL ☐ PASS | — |
| `GROWTH-TC-038-002` | `throw('Not implemented')` | FAIL | ☑ FAIL ☐ PASS | — |
| `GROWTH-TC-038-003` | `throw('Not implemented')` | FAIL | ☑ FAIL ☐ PASS | — |
| `GROWTH-TC-038-004` | `throw('Not implemented')` | FAIL | ☑ FAIL ☐ PASS | — |
| `GROWTH-TC-038-005` | `throw('Not implemented')` | FAIL | ☑ FAIL ☐ PASS | — |
| `GROWTH-TC-038-006` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | Controller test not run |
| `GROWTH-TC-038-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | Integration test not run |

**Red Gate Evidence:**
- Stub commit hash: `RED Phase confirmed via Maven surefire`
- Tat ca FAIL? [x] Yes (service tests 001-005) -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement
- Log file: `Maven surefire output — all 5 GrowthServiceTest methods FAIL with UnsupportedOperationException`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-BABY-IMP-008` da duoc review va approve
- [x] Logic Issues (Section 2) da duoc confirm
- [x] Table `growth_measurements` da ton tai trong DB
- [x] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria

- [x] `./mvnw test` -- tat ca unit tests xanh (5/5 GrowthServiceTest PASS)
- [ ] `./mvnw verify` -- tat ca integration tests xanh (Testcontainers) — integration test khong duoc implement
- [ ] Test coverage >= 80% lines cho GrowthService — chua do
- [x] Khong co business logic trong Controller
- [x] Khong co PII/secret xuat hien plaintext trong logs
- [x] ageInDays calculated correctly for all test data

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
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/GrowthChartController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/GrowthService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/IGrowthService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/GrowthChartResponse.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/GrowthDataPoint.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/
```

No migration rollback needed -- table already exists.

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [x] OK | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi empty/throw stub (S5.1) | [x] OK — Red Gate PASS confirmed | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision khong co ADR | [x] OK | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [x] OK | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai trong codebase | [x] OK — all contracts verified | G-3 |

**Ket qua review:**

- [x] Khong phat hien anti-pattern nao -> TDD spec approved
- [ ] Phat hien AP -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| -- | -- | -- | -- | -- |

---

*TDD Template v2.0 -- Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
