# Test Specification — UC24: View Mother Journey Dashboard

| Field            | Value                                                     |
|------------------|-----------------------------------------------------------|
| Document ID      | CB-JOURNEY-IMP-003-TEST                                   |
| Version          | 1.0                                                       |
| Date             | 2026-06-26                                                |
| Status           | Draft                                                     |
| Author           | AI Agent                                                  |
| Related TDS      | CB-JOURNEY-IMP-003                                        |
| SRS Reference    | SRS 3.3.1.3 — View Mother Journey Dashboard               |
| Test ID Prefix   | `JOURNEY-TC-024`                                          |

---

## 1. Scope and Objectives

This document defines test cases for UC24 — View Mother Journey Dashboard. The scope covers:

- **Unit tests**: Service layer logic (pregnancy week calculation, trimester derivation, NO_JOURNEY case, dashboard status resolution)
- **Controller tests**: HTTP response shape, authentication enforcement
- **Integration tests**: End-to-end GET request through Spring context to PostgreSQL (Testcontainers)

**Objectives:**
1. Verify that no active journey returns HTTP 200 with `NO_JOURNEY` status — never 404
2. Verify pregnancy week calculation is correct (`floor(days / 7)`)
3. Verify trimester derivation: weeks 1-13=1, 14-27=2, 28+=3
4. Verify POSTPARTUM and other non-PREGNANCY journey types return null for pregnancy-specific fields
5. Verify the `/me` pattern never leaks data from other users
6. Verify endpoint is strictly read-only (no DB writes triggered by GET)

---

## 2. Logic Issues and Risk Notes

| ID  | Risk                                    | Description                                                                                                              | Mitigation                                                              |
|-----|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| L1  | NO_JOURNEY returns 404 instead of 200   | If service throws NotFoundException when no active journey, mobile app breaks its onboarding flow                        | Service must return `NO_JOURNEY` status with HTTP 200 — never throw 404 |
| L2  | Pregnancy week integer overflow/rounding | `ChronoUnit.WEEKS.between()` returns long — must cast to int safely; negative weeks if LMP is in the future             | Use `Math.max(0, weeks)` before casting to int                          |
| L3  | Trimester boundary errors               | Boundary conditions at week 13/14 and 27/28 — off-by-one errors are likely                                              | Test exactly at boundaries: week 13=trim1, week 14=trim2, week 27=trim2, week 28=trim3 |
| L4  | IDOR via clock injection                | If `LocalDate.now()` is called without injectable `Clock`, tests cannot deterministically assert pregnancy week          | Inject `Clock` into `JourneyServiceImpl`; tests provide `Clock.fixed()` |
| L5  | Multiple active journeys               | DB could theoretically have two ACTIVE journeys for one user (data integrity gap); dashboard should return the most recent | `findByOwnerUserIdAndStatus()` uses `LIMIT 1 ORDER BY created_at DESC`; document this behaviour |

---

## 3. Test Data Factory

```java
package com.carebridge.backend.carejourney.test;

import com.carebridge.backend.carejourney.entity.MotherJourney;
import com.carebridge.backend.carejourney.enums.JourneyStatus;
import com.carebridge.backend.carejourney.enums.JourneyType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Canonical test data factory for UC24 test cases.
 * Uses a fixed MOTHER_ID distinct from UC23 to prevent test pollution.
 */
public class JourneyDashboardTestFactory {

    // Deterministic UUID — unique to UC24 test suite
    public static final UUID MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000024");

    /**
     * Returns a PREGNANCY journey with lastMenstrualDate exactly weeksPregnant weeks ago.
     * estimatedDueDate is set to LMP + 40 weeks (standard obstetric due date).
     *
     * @param weeksPregnant number of weeks since LMP (e.g., 20 for week 20)
     */
    public static MotherJourney makePregnancyJourney(int weeksPregnant) {
        LocalDate lmpDate = LocalDate.now().minusWeeks(weeksPregnant);
        return MotherJourney.builder()
                .journeyId(UUID.randomUUID())
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(lmpDate)
                .lastMenstrualDate(lmpDate)
                .estimatedDueDate(lmpDate.plusWeeks(40))
                .build();
    }

    /**
     * Returns a PREGNANCY journey with lastMenstrualDate exactly daysAgo days ago.
     * Used for precise week/trimester boundary tests.
     *
     * @param daysAgo days since LMP (e.g., 140 for 20 weeks)
     */
    public static MotherJourney makePregnancyJourneyByDays(int daysAgo) {
        LocalDate lmpDate = LocalDate.now().minusDays(daysAgo);
        return MotherJourney.builder()
                .journeyId(UUID.randomUUID())
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(lmpDate)
                .lastMenstrualDate(lmpDate)
                .estimatedDueDate(lmpDate.plusWeeks(40))
                .build();
    }

    /**
     * Returns a POSTPARTUM journey in ACTIVE state.
     * No lastMenstrualDate or estimatedDueDate — not applicable to postpartum.
     */
    public static MotherJourney makePostpartumJourney() {
        return MotherJourney.builder()
                .journeyId(UUID.randomUUID())
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(30))
                .lastMenstrualDate(null)
                .estimatedDueDate(null)
                .build();
    }

    /**
     * Returns a PRE_PREGNANCY journey in ACTIVE state.
     */
    public static MotherJourney makePrePregnancyJourney() {
        return MotherJourney.builder()
                .journeyId(UUID.randomUUID())
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(10))
                .build();
    }
}
```

---

## 4. Unit Test Cases

### JOURNEY-TC-024-001: Happy Path — Active PREGNANCY Journey

| Field            | Value                                                           |
|------------------|-----------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-001                                             |
| Test Name        | Active PREGNANCY journey returns full dashboard with pregnancyWeek and trimester |
| Type             | Unit (JourneyServiceImpl)                                      |
| Priority         | P1 — Critical                                                  |
| Related Rule     | BR-JOURNEY-020, BR-JOURNEY-022, BR-JOURNEY-023                 |

**Preconditions:**
- Journey exists: `journeyType=PREGNANCY`, `status=ACTIVE`, `lastMenstrualDate = 20 weeks ago`
- Clock fixed to today's date

**Mock Setup:**
```java
MotherJourney journey = JourneyDashboardTestFactory.makePregnancyJourney(20);
when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
    .thenReturn(Optional.of(journey));
```

**Expected Result:**
- `status = ACTIVE_PREGNANCY`
- `pregnancyWeek = 20`
- `trimester = 2`
- `daysUntilDue = ChronoUnit.DAYS.between(today, lmp + 40 weeks)` (positive value)
- `journeyId` matches journey's ID
- `journeyType = "PREGNANCY"`

**Assertions:**
```java
JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getStatus()).isEqualTo(DashboardStatus.ACTIVE_PREGNANCY);
assertThat(result.getPregnancyWeek()).isEqualTo(20);
assertThat(result.getTrimester()).isEqualTo(2);
assertThat(result.getDaysUntilDue()).isPositive();
assertThat(result.getJourneyId()).isEqualTo(journey.getJourneyId());
assertThat(result.getJourneyType()).isEqualTo("PREGNANCY");
```

---

### JOURNEY-TC-024-002: Active POSTPARTUM Journey — Pregnancy Fields Are Null

| Field            | Value                                                           |
|------------------|-----------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-002                                             |
| Test Name        | Active POSTPARTUM journey — pregnancyWeek and trimester are null |
| Type             | Unit (JourneyServiceImpl)                                      |
| Priority         | P1 — Critical                                                  |
| Related Rule     | BR-JOURNEY-020                                                 |

**Preconditions:**
- Journey exists: `journeyType=POSTPARTUM`, `status=ACTIVE`, no lastMenstrualDate

**Mock Setup:**
```java
MotherJourney journey = JourneyDashboardTestFactory.makePostpartumJourney();
when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
    .thenReturn(Optional.of(journey));
```

**Expected Result:**
- `status = ACTIVE_POSTPARTUM`
- `pregnancyWeek = null`
- `trimester = null`
- `daysUntilDue = null`
- `journeyId` not null

**Assertions:**
```java
JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getStatus()).isEqualTo(DashboardStatus.ACTIVE_POSTPARTUM);
assertThat(result.getPregnancyWeek()).isNull();
assertThat(result.getTrimester()).isNull();
assertThat(result.getDaysUntilDue()).isNull();
assertThat(result.getJourneyId()).isNotNull();
```

---

### JOURNEY-TC-024-003: No Active Journey → HTTP 200 with NO_JOURNEY (NOT 404)

| Field            | Value                                                           |
|------------------|-----------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-003                                             |
| Test Name        | No active journey returns 200 with NO_JOURNEY — never 404      |
| Type             | Unit (JourneyServiceImpl) + Controller                         |
| Priority         | P1 — Critical                                                  |
| Related Rule     | BR-JOURNEY-021                                                 |

**Preconditions:**
- No journey with `status=ACTIVE` exists for MOTHER_ID

**Mock Setup:**
```java
when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
    .thenReturn(Optional.empty());
```

**Expected Result:**
- No exception thrown
- `status = NO_JOURNEY`
- All journey fields (`journeyId`, `journeyType`, `pregnancyWeek`, `trimester`, `daysUntilDue`, `estimatedDueDate`, `lastMenstrualDate`) are null

**Service Assertions:**
```java
JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getStatus()).isEqualTo(DashboardStatus.NO_JOURNEY);
assertThat(result.getJourneyId()).isNull();
assertThat(result.getJourneyType()).isNull();
assertThat(result.getPregnancyWeek()).isNull();
assertThat(result.getTrimester()).isNull();
assertThat(result.getDaysUntilDue()).isNull();
```

**Controller Assertions (MockMvc):**
```java
mockMvc.perform(get("/api/v1/journeys/me/dashboard")
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTHER"))))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.status").value("NO_JOURNEY"))
    .andExpect(jsonPath("$.data.journeyId").isEmpty());
```

---

### JOURNEY-TC-024-004: Pregnancy Week Calculation Accuracy — 140 Days Ago

| Field            | Value                                                           |
|------------------|-----------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-004                                             |
| Test Name        | LMP = 140 days ago → pregnancyWeek=20, trimester=2            |
| Type             | Unit (JourneyServiceImpl) — calculation-focused                |
| Priority         | P1 — Critical                                                  |
| Related Rule     | BR-JOURNEY-022                                                 |

**Preconditions:**
- Clock is fixed to a known date (e.g., 2026-06-26)
- `lastMenstrualDate = 2026-06-26 - 140 days = 2026-02-06`
- `140 / 7 = 20.0` → pregnancyWeek = 20

**Mock Setup:**
```java
Clock fixedClock = Clock.fixed(
    Instant.parse("2026-06-26T00:00:00Z"), ZoneId.systemDefault());
journeyService = new JourneyServiceImpl(journeyRepository, auditService, fixedClock);

MotherJourney journey = JourneyDashboardTestFactory.makePregnancyJourneyByDays(140);
when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
    .thenReturn(Optional.of(journey));
```

**Expected Result:**
- `pregnancyWeek = 20`
- `trimester = 2` (week 20 falls in weeks 14–27)

**Assertions:**
```java
JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getPregnancyWeek()).isEqualTo(20);
assertThat(result.getTrimester()).isEqualTo(2);
```

**Boundary Sub-cases (parameterized test):**

| Days Since LMP | Expected Week | Expected Trimester |
|----------------|---------------|--------------------|
| 91  (13 weeks) | 13            | 1                  |
| 98  (14 weeks) | 14            | 2                  |
| 189 (27 weeks) | 27            | 2                  |
| 196 (28 weeks) | 28            | 3                  |
| 140 (20 weeks) | 20            | 2                  |
| 0   (LMP today)| 0             | 1                  |

```java
@ParameterizedTest
@CsvSource({
    "91,  13, 1",
    "98,  14, 2",
    "189, 27, 2",
    "196, 28, 3",
    "140, 20, 2",
    "0,   0,  1"
})
void shouldCalculateCorrectWeekAndTrimester(int daysAgo, int expectedWeek, int expectedTrimester) {
    MotherJourney journey = JourneyDashboardTestFactory.makePregnancyJourneyByDays(daysAgo);
    when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
        .thenReturn(Optional.of(journey));

    JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

    assertThat(result.getPregnancyWeek()).isEqualTo(expectedWeek);
    assertThat(result.getTrimester()).isEqualTo(expectedTrimester);
}
```

---

### JOURNEY-TC-024-005: No JWT Token → 401 Unauthorized

| Field            | Value                                                           |
|------------------|-----------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-005                                             |
| Test Name        | Request without Authorization header returns 401               |
| Type             | Controller / Security Integration                              |
| Priority         | P1 — Critical                                                  |
| Related Rule     | BR-RBAC                                                        |

**Preconditions:** Spring Security configured with JWT filter

**Input:** GET request with no `Authorization` header

**Expected Result:** HTTP 401 — service layer NOT reached

**Assertions (MockMvc):**
```java
mockMvc.perform(get("/api/v1/journeys/me/dashboard"))
    .andExpect(status().isUnauthorized());

verify(journeyService, never()).getDashboard(any());
```

---

## 5. Integration Test Cases

### JOURNEY-TC-024-INT-001: Integration — Full GET Flow with Testcontainers

| Field            | Value                                                             |
|------------------|-------------------------------------------------------------------|
| Test ID          | JOURNEY-TC-024-INT-001                                            |
| Test Name        | Integration — active PREGNANCY journey, verify computed fields   |
| Type             | Integration (Testcontainers + Spring Boot Test)                   |
| Priority         | P1 — Critical                                                     |
| Dependencies     | PostgreSQL Testcontainer, Flyway migrations applied               |

**Preconditions:**
1. Testcontainer PostgreSQL started and schema migrated via Flyway
2. Insert journey row: `owner_user_id=MOTHER_ID`, `journey_type=PREGNANCY`, `status=ACTIVE`, `last_menstrual_date = 20 weeks ago`
3. Mock JWT for MOTHER_ID

**Steps:**
1. Send `GET /api/v1/journeys/me/dashboard` with valid JWT
2. Assert HTTP 200
3. Assert response body fields

**Assertions:**
```java
MvcResult result = mockMvc.perform(get("/api/v1/journeys/me/dashboard")
        .header("Authorization", "Bearer " + jwtForMotherId(MOTHER_ID)))
    .andExpect(status().isOk())
    .andReturn();

String body = result.getResponse().getContentAsString();
DocumentContext json = JsonPath.parse(body);

assertThat((String) json.read("$.data.status")).isEqualTo("ACTIVE_PREGNANCY");
assertThat((Integer) json.read("$.data.pregnancyWeek")).isEqualTo(20);
assertThat((Integer) json.read("$.data.trimester")).isEqualTo(2);
assertThat((String) json.read("$.data.journeyId")).isNotNull();

// Verify no writes occurred — table modified_at unchanged
Timestamp updatedAt = jdbcTemplate.queryForObject(
    "SELECT updated_at FROM mother_journeys WHERE owner_user_id = ?",
    Timestamp.class, MOTHER_ID);
assertThat(updatedAt).isEqualTo(insertedUpdatedAt); // unchanged
```

---

## 6. Negative and Edge Case Tests

| Test ID                  | Scenario                                               | Input                              | Expected                          |
|--------------------------|--------------------------------------------------------|------------------------------------|-----------------------------------|
| JOURNEY-TC-024-NEG-001   | Completed journey only — no active journey             | Only COMPLETED journey in DB       | HTTP 200, status=NO_JOURNEY       |
| JOURNEY-TC-024-NEG-002   | Archived journey only — no active journey              | Only ARCHIVED journey in DB        | HTTP 200, status=NO_JOURNEY       |
| JOURNEY-TC-024-NEG-003   | BABY_CARE active journey                               | journey_type=BABY_CARE, status=ACTIVE | HTTP 200, status=BABY_CARE, pregnancyWeek=null |
| JOURNEY-TC-024-NEG-004   | PRE_PREGNANCY active journey                           | journey_type=PRE_PREGNANCY, status=ACTIVE | HTTP 200, status=PRE_PREGNANCY, pregnancyWeek=null |
| JOURNEY-TC-024-NEG-005   | PREGNANCY with null lastMenstrualDate                  | lastMenstrualDate=null             | HTTP 200, pregnancyWeek=null      |
| JOURNEY-TC-024-NEG-006   | ROLE_EXPERT attempts to access /me/dashboard           | JWT role=ROLE_EXPERT               | HTTP 403                          |
| JOURNEY-TC-024-NEG-007   | Past due date (daysUntilDue negative)                  | estimatedDueDate = 5 days ago      | HTTP 200, daysUntilDue < 0        |

### Detail: JOURNEY-TC-024-NEG-005 — PREGNANCY with null lastMenstrualDate

```java
MotherJourney journey = MotherJourney.builder()
    .journeyId(UUID.randomUUID())
    .ownerUserId(MOTHER_ID)
    .journeyType(JourneyType.PREGNANCY)
    .status(JourneyStatus.ACTIVE)
    .lastMenstrualDate(null)   // LMP not yet recorded
    .build();
when(journeyRepository.findByOwnerUserIdAndStatus(MOTHER_ID, JourneyStatus.ACTIVE))
    .thenReturn(Optional.of(journey));

JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getStatus()).isEqualTo(DashboardStatus.ACTIVE_PREGNANCY);
assertThat(result.getPregnancyWeek()).isNull(); // cannot calculate without LMP
assertThat(result.getTrimester()).isNull();
```

### Detail: JOURNEY-TC-024-NEG-007 — Past Due Date

```java
// estimatedDueDate = 5 days ago → daysUntilDue = -5
MotherJourney journey = makePregnancyJourney(41); // 41 weeks, past 40-week due date
// estimatedDueDate = LMP + 40 weeks = 41 weeks - 40 weeks = 1 week ago
// daysUntilDue ≈ -7

JourneyDashboardResponse result = journeyService.getDashboard(MOTHER_ID);

assertThat(result.getDaysUntilDue()).isNegative();
assertThat(result.getStatus()).isEqualTo(DashboardStatus.ACTIVE_PREGNANCY);
```

---

## 7. Test Environment Requirements

| Requirement              | Value                                                        |
|--------------------------|--------------------------------------------------------------|
| Java Version             | 21                                                           |
| Framework                | Spring Boot 3.5.x Test, JUnit 5, Mockito, AssertJ           |
| DB (Integration)         | PostgreSQL via Testcontainers (`testcontainers:postgresql`)  |
| Migration                | Flyway applied automatically via `@SpringBootTest`           |
| Auth Mock                | `@WithMockUser(roles = "MOTHER")` or JWT mock filter        |
| Clock Injection          | `Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ...)`   |
| JSON Path                | `com.jayway.jsonpath:json-path` for integration assertions   |
| Test Runner              | `./mvnw test -Dtest=JourneyDashboardServiceTest,JourneyDashboardControllerTest` |

---

## 8. Acceptance Criteria Checklist

| # | Criterion                                                                                              | Verified By                  |
|---|--------------------------------------------------------------------------------------------------------|------------------------------|
| 1 | GET /api/v1/journeys/me/dashboard returns HTTP 200 for authenticated ROLE_MOTHER                       | JOURNEY-TC-024-001           |
| 2 | Active PREGNANCY journey returns pregnancyWeek and trimester                                           | JOURNEY-TC-024-001, 004      |
| 3 | Active POSTPARTUM journey returns null for pregnancy-specific fields                                   | JOURNEY-TC-024-002           |
| 4 | No active journey returns HTTP 200 with status=NO_JOURNEY — never HTTP 404                             | JOURNEY-TC-024-003           |
| 5 | Pregnancy week calculation is accurate: 140 days → week 20                                             | JOURNEY-TC-024-004           |
| 6 | Trimester boundaries are correct: week 13=1, week 14=2, week 27=2, week 28=3                          | JOURNEY-TC-024-004 (parameterized) |
| 7 | No JWT returns HTTP 401                                                                                | JOURNEY-TC-024-005           |
| 8 | Dashboard endpoint triggers no DB writes — updated_at unchanged after GET                              | JOURNEY-TC-024-INT-001       |
| 9 | ROLE_EXPERT and ROLE_ADMIN cannot access /me/dashboard                                                | JOURNEY-TC-024-NEG-006       |
| 10 | PREGNANCY with null lastMenstrualDate returns null pregnancyWeek (graceful degradation)               | JOURNEY-TC-024-NEG-005       |
| 11 | Past-due journeys return negative daysUntilDue (not an error)                                         | JOURNEY-TC-024-NEG-007       |

---

*End of CB-JOURNEY-IMP-003-TEST — UC24 View Mother Journey Dashboard Test-Spec v1.0*
