# Test Specification — UC23: Update Mother Journey

| Field            | Value                                      |
|------------------|--------------------------------------------|
| Document ID      | CB-JOURNEY-IMP-002-TEST                    |
| Version          | 1.0                                        |
| Date             | 2026-06-26                                 |
| Status           | Draft                                      |
| Author           | AI Agent                                   |
| Related TDS      | CB-JOURNEY-IMP-002                         |
| SRS Reference    | SRS 3.3.1.2 — Update Mother Journey        |
| Test ID Prefix   | `JOURNEY-TC-023`                           |

---

## 1. Scope and Objectives

This document defines test cases for UC23 — Update Mother Journey. The scope covers:

- **Unit tests**: Service layer logic (ownership validation, status transitions, field mutation)
- **Controller tests**: HTTP request/response mapping, authentication enforcement
- **Integration tests**: End-to-end PUT request through the Spring context to PostgreSQL (Testcontainers)

**Objectives:**
1. Verify that only the authenticated owner can update a journey (IDOR prevention)
2. Verify status transition rules: ACTIVE→COMPLETED requires `delivery_date`
3. Verify immutability of `journey_type` and `owner_user_id`
4. Verify ARCHIVED status is rejected when set by a user
5. Verify audit event `JOURNEY_UPDATED` is emitted on every successful update

---

## 2. Logic Issues and Risk Notes

| ID  | Risk                              | Description                                                                                          | Mitigation                                                     |
|-----|-----------------------------------|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| L1  | Status transition validation      | Accepting COMPLETED without delivery_date leaves domain in inconsistent state                        | `validateStatusTransition()` must check delivery_date before save |
| L2  | IDOR via owner_user_id            | Owner check must compare `journey.ownerUserId` (from DB) against JWT userId — not any URL parameter  | Never trust URL params for ownership; always load from DB first |
| L3  | ARCHIVED status leak              | If service does not explicitly reject ARCHIVED, a user could reach an unrecoverable journey state    | Explicitly validate allowed user-settable statuses: `{ACTIVE, COMPLETED}` |
| L4  | Null-overwrite of protected fields | A PUT request with `null` values could silently overwrite `lastMenstrualDate` with null in DB       | Document which fields are null-safe vs. null-cleared; align with ADR-JOURNEY-002-001 |
| L5  | Race condition on status check    | Two concurrent PUT requests could both pass the ACTIVE check before one commits COMPLETED            | `@Transactional` + DB-level optimistic locking (future); acceptable for MVP |

---

## 3. Test Data Factory

```java
package com.carebridge.backend.carejourney.test;

import com.carebridge.backend.carejourney.dto.UpdateJourneyRequest;
import com.carebridge.backend.carejourney.entity.MotherJourney;
import com.carebridge.backend.carejourney.enums.JourneyStatus;
import com.carebridge.backend.carejourney.enums.JourneyType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Canonical test data factory for UC23 test cases.
 * UUIDs are deterministic to facilitate integration test assertions.
 */
public class JourneyUpdateTestFactory {

    // Deterministic UUIDs — unique to UC23 test suite
    public static final UUID MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000023");

    public static final UUID OTHER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000099");

    public static final UUID JOURNEY_ID =
            UUID.fromString("cccccccc-0000-0000-0000-000000000023");

    public static final UUID UNKNOWN_JOURNEY_ID =
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    /**
     * Returns a PREGNANCY journey in ACTIVE state, owned by MOTHER_ID.
     */
    public static MotherJourney makeActiveJourney() {
        return MotherJourney.builder()
                .journeyId(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .lastMenstrualDate(LocalDate.of(2025, 12, 1))
                .estimatedDueDate(LocalDate.of(2026, 9, 7))
                .build();
    }

    /**
     * Returns a PREGNANCY journey in COMPLETED state.
     */
    public static MotherJourney makeCompletedJourney() {
        return makeActiveJourney().toBuilder()
                .status(JourneyStatus.COMPLETED)
                .deliveryDate(LocalDate.of(2026, 9, 5))
                .build();
    }

    /**
     * Returns a PREGNANCY journey in ARCHIVED state (system-set).
     */
    public static MotherJourney makeArchivedJourney() {
        return makeActiveJourney().toBuilder()
                .status(JourneyStatus.ARCHIVED)
                .build();
    }

    /**
     * Minimal update request — changes only notes.
     */
    public static UpdateJourneyRequest makeUpdateRequest() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setLastMenstrualDate(LocalDate.of(2025, 12, 1));
        req.setEstimatedDueDate(LocalDate.of(2026, 9, 7));
        req.setDeliveryDate(null);
        req.setNotes("Updated notes for test");
        req.setStatus("ACTIVE");
        return req;
    }

    /**
     * Complete journey request — includes deliveryDate and COMPLETED status.
     */
    public static UpdateJourneyRequest makeCompleteRequest() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setLastMenstrualDate(LocalDate.of(2025, 12, 1));
        req.setEstimatedDueDate(LocalDate.of(2026, 9, 7));
        req.setDeliveryDate(LocalDate.of(2026, 9, 5));
        req.setNotes("Baby delivered safely.");
        req.setStatus("COMPLETED");
        return req;
    }

    /**
     * Complete journey request — missing deliveryDate (invalid).
     */
    public static UpdateJourneyRequest makeCompleteRequestWithoutDeliveryDate() {
        UpdateJourneyRequest req = makeCompleteRequest();
        req.setDeliveryDate(null);
        return req;
    }
}
```

---

## 4. Unit Test Cases

### JOURNEY-TC-023-001: Happy Path — Update Notes and EstimatedDueDate

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-001                                     |
| Test Name        | Happy path — update notes and estimatedDueDate         |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P1 — Critical                                          |
| Related Rule     | BR-JOURNEY-010, BR-JOURNEY-014                         |

**Preconditions:**
- Journey `JOURNEY_ID` exists in repository, owned by `MOTHER_ID`, status = ACTIVE
- JWT provides `ownerId = MOTHER_ID`

**Input:**
```json
{
  "lastMenstrualDate": "2025-12-01",
  "estimatedDueDate": "2026-09-10",
  "deliveryDate": null,
  "notes": "Updated notes for test",
  "status": "ACTIVE"
}
```

**Expected Behavior:**
1. `journeyRepository.findById(JOURNEY_ID)` returns the active journey
2. Ownership check passes (MOTHER_ID == MOTHER_ID)
3. Status check passes (ACTIVE)
4. `estimatedDueDate` updated to 2026-09-10
5. `notes` updated to "Updated notes for test"
6. `journeyRepository.save()` called once
7. `auditService.emit(JOURNEY_UPDATED, ...)` called once

**Expected Result:** `JourneyResponse` with `status=ACTIVE`, `notes="Updated notes for test"`, `estimatedDueDate=2026-09-10`

**Assertions:**
```java
assertThat(result.getStatus()).isEqualTo("ACTIVE");
assertThat(result.getNotes()).isEqualTo("Updated notes for test");
assertThat(result.getEstimatedDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
verify(auditService, times(1)).emit(eq("JOURNEY_UPDATED"), any());
```

---

### JOURNEY-TC-023-002: Complete Journey (status=COMPLETED with deliveryDate)

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-002                                     |
| Test Name        | Complete journey with deliveryDate — success           |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P1 — Critical                                          |
| Related Rule     | BR-JOURNEY-012                                         |

**Preconditions:**
- Journey exists, ACTIVE, owned by MOTHER_ID

**Input:**
```json
{
  "lastMenstrualDate": "2025-12-01",
  "estimatedDueDate": "2026-09-07",
  "deliveryDate": "2026-09-05",
  "notes": "Baby delivered safely.",
  "status": "COMPLETED"
}
```

**Expected Behavior:**
1. Ownership and status checks pass
2. `request.status == COMPLETED` and `request.deliveryDate != null` → allowed
3. `journey.status` set to COMPLETED, `journey.deliveryDate` set to 2026-09-05
4. Save and audit emit called

**Expected Result:** `JourneyResponse` with `status=COMPLETED`, `deliveryDate=2026-09-05`

**Assertions:**
```java
assertThat(result.getStatus()).isEqualTo("COMPLETED");
assertThat(result.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 9, 5));
verify(journeyRepository, times(1)).save(any());
```

---

### JOURNEY-TC-023-003: Complete Journey Without deliveryDate → 400 JOURNEY-013

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-003                                     |
| Test Name        | status=COMPLETED without deliveryDate — validation fail |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P1 — Critical                                          |
| Related Rule     | BR-JOURNEY-012                                         |

**Preconditions:**
- Journey exists, ACTIVE, owned by MOTHER_ID

**Input:**
```json
{
  "status": "COMPLETED",
  "deliveryDate": null
}
```

**Expected Behavior:**
1. Ownership and status checks pass
2. `request.status == COMPLETED` but `request.deliveryDate == null` → throw `JourneyStateException` with code JOURNEY-013

**Expected Result:** Exception thrown; `journeyRepository.save()` NOT called

**Assertions:**
```java
assertThatThrownBy(() -> journeyService.updateJourney(MOTHER_ID, JOURNEY_ID, req))
    .isInstanceOf(JourneyStateException.class)
    .hasMessageContaining("JOURNEY-013");
verify(journeyRepository, never()).save(any());
```

---

### JOURNEY-TC-023-004: Update Another User's Journey → 403 JOURNEY-011

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-004                                     |
| Test Name        | IDOR attempt — attacker updates victim's journey       |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P1 — Critical (Security)                               |
| Related Rule     | BR-JOURNEY-010                                         |

**Preconditions:**
- Journey `JOURNEY_ID` exists, owned by `MOTHER_ID`
- Caller provides `ownerId = OTHER_USER_ID` (different user)

**Input:** Any valid UpdateJourneyRequest

**Expected Behavior:**
1. `findById(JOURNEY_ID)` returns journey (ownerUserId = MOTHER_ID)
2. Ownership check: MOTHER_ID != OTHER_USER_ID → throw `JourneyAccessDeniedException(JOURNEY-011)`
3. No save, no audit emit

**Assertions:**
```java
assertThatThrownBy(() -> journeyService.updateJourney(OTHER_USER_ID, JOURNEY_ID, req))
    .isInstanceOf(JourneyAccessDeniedException.class)
    .hasMessageContaining("JOURNEY-011");
verify(journeyRepository, never()).save(any());
verify(auditService, never()).emit(any(), any());
```

---

### JOURNEY-TC-023-005: Journey Not Found → 404 JOURNEY-010

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-005                                     |
| Test Name        | Journey ID does not exist — 404                        |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P2 — High                                              |
| Related Rule     | BR-JOURNEY-010                                         |

**Preconditions:**
- `UNKNOWN_JOURNEY_ID` does not exist in repository

**Input:** `journeyId = UNKNOWN_JOURNEY_ID`, any UpdateJourneyRequest

**Expected Behavior:**
1. `findById(UNKNOWN_JOURNEY_ID)` returns `Optional.empty()`
2. Throw `JourneyNotFoundException(JOURNEY-010)`

**Assertions:**
```java
when(journeyRepository.findById(UNKNOWN_JOURNEY_ID)).thenReturn(Optional.empty());

assertThatThrownBy(() -> journeyService.updateJourney(MOTHER_ID, UNKNOWN_JOURNEY_ID, req))
    .isInstanceOf(JourneyNotFoundException.class)
    .hasMessageContaining("JOURNEY-010");
```

---

### JOURNEY-TC-023-006: Update COMPLETED Journey → 400 JOURNEY-012

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-006                                     |
| Test Name        | Attempt to update a COMPLETED journey — rejected       |
| Type             | Unit (JourneyServiceImpl)                              |
| Priority         | P1 — Critical                                          |
| Related Rule     | BR-JOURNEY-011                                         |

**Preconditions:**
- Journey exists, `status = COMPLETED`, owned by MOTHER_ID

**Input:** Any UpdateJourneyRequest (even valid ones)

**Expected Behavior:**
1. Journey loaded successfully
2. Ownership check passes
3. Status check: `COMPLETED != ACTIVE` → throw `JourneyStateException(JOURNEY-012)`
4. No save

**Assertions:**
```java
when(journeyRepository.findById(JOURNEY_ID))
    .thenReturn(Optional.of(JourneyUpdateTestFactory.makeCompletedJourney()));

assertThatThrownBy(() -> journeyService.updateJourney(MOTHER_ID, JOURNEY_ID, req))
    .isInstanceOf(JourneyStateException.class)
    .hasMessageContaining("JOURNEY-012");
verify(journeyRepository, never()).save(any());
```

---

### JOURNEY-TC-023-007: No JWT Token → 401 Unauthorized

| Field            | Value                                                  |
|------------------|--------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-007                                     |
| Test Name        | Request without Authorization header — 401             |
| Type             | Controller / Security Integration                      |
| Priority         | P1 — Critical                                          |
| Related Rule     | BR-RBAC                                                |

**Preconditions:** Spring Security configured with JWT filter

**Input:** PUT request with no `Authorization` header

**Expected Result:** HTTP 401 Unauthorized — service layer NOT called

**Assertions (MockMvc):**
```java
mockMvc.perform(put("/api/v1/journeys/" + JOURNEY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(makeUpdateRequest())))
    .andExpect(status().isUnauthorized());

verify(journeyService, never()).updateJourney(any(), any(), any());
```

---

## 5. Integration Test Cases

### JOURNEY-TC-023-INT-001: Integration — DB updated_at Changes, Status Stored Correctly

| Field            | Value                                                             |
|------------------|-------------------------------------------------------------------|
| Test ID          | JOURNEY-TC-023-INT-001                                            |
| Test Name        | Integration — full PUT flow verifies DB state                     |
| Type             | Integration (Testcontainers + Spring Boot Test)                   |
| Priority         | P1 — Critical                                                     |
| Dependencies     | PostgreSQL Testcontainer, Flyway migrations applied               |

**Preconditions:**
1. Testcontainer PostgreSQL started and schema migrated
2. Journey row inserted with `status=ACTIVE`, `updated_at = T0`
3. Authenticated JWT mocked for MOTHER_ID

**Steps:**
1. Record `T0 = SELECT updated_at FROM mother_journeys WHERE journey_id = ?`
2. Send `PUT /api/v1/journeys/{journeyId}` with updated notes
3. Wait for HTTP 200
4. Query DB: `SELECT status, notes, updated_at FROM mother_journeys WHERE journey_id = ?`

**Expected Result:**
- `status` = `ACTIVE` (unchanged)
- `notes` = new value from request
- `updated_at` > T0 (timestamp advanced)
- `journey_type` unchanged (immutability)
- `owner_user_id` unchanged (immutability)

**Assertions:**
```java
// Post-update DB query
MotherJourney updated = jdbcTemplate.queryForObject(
    "SELECT * FROM mother_journeys WHERE journey_id = ?",
    journeyRowMapper, JOURNEY_ID);

assertThat(updated.getNotes()).isEqualTo("Updated notes for test");
assertThat(updated.getStatus()).isEqualTo(JourneyStatus.ACTIVE);
assertThat(updated.getUpdatedAt()).isAfter(t0);
assertThat(updated.getJourneyType()).isEqualTo(JourneyType.PREGNANCY); // immutable
assertThat(updated.getOwnerUserId()).isEqualTo(MOTHER_ID);             // immutable
```

---

## 6. Negative and Edge Case Tests

| Test ID                | Scenario                                          | Input                              | Expected       |
|------------------------|---------------------------------------------------|------------------------------------|----------------|
| JOURNEY-TC-023-NEG-001 | Set status = ARCHIVED (system-only)               | `"status": "ARCHIVED"`             | HTTP 400       |
| JOURNEY-TC-023-NEG-002 | estimatedDueDate before lastMenstrualDate          | EDD = LMP - 1 day                  | HTTP 400       |
| JOURNEY-TC-023-NEG-003 | Notes exceed 2000 characters                      | `notes` = 2001 char string         | HTTP 400       |
| JOURNEY-TC-023-NEG-004 | Invalid UUID format in path                       | `journeyId = "not-a-uuid"`         | HTTP 400       |
| JOURNEY-TC-023-NEG-005 | Update ARCHIVED journey                           | Existing status = ARCHIVED         | HTTP 400 JOURNEY-012 |
| JOURNEY-TC-023-NEG-006 | ROLE_EXPERT attempts update                       | JWT role = ROLE_EXPERT             | HTTP 403       |

---

## 7. Test Environment Requirements

| Requirement              | Value                                                        |
|--------------------------|--------------------------------------------------------------|
| Java Version             | 21                                                           |
| Framework                | Spring Boot 3.5.x Test, JUnit 5, Mockito                    |
| DB (Integration)         | PostgreSQL via Testcontainers (`testcontainers:postgresql`)  |
| Migration                | Flyway applied automatically via `@SpringBootTest`           |
| Auth Mock                | `@WithMockUser(roles = "MOTHER")` or JWT mock filter        |
| Test Runner              | `./mvnw test -Dtest=JourneyUpdateServiceTest,JourneyControllerTest` |

---

## 8. Acceptance Criteria Checklist

| # | Criterion                                                                              | Verified By              |
|---|----------------------------------------------------------------------------------------|--------------------------|
| 1 | PUT /api/v1/journeys/{journeyId} returns HTTP 200 with updated data for own active journey | JOURNEY-TC-023-001     |
| 2 | status=COMPLETED with delivery_date returns HTTP 200                                   | JOURNEY-TC-023-002       |
| 3 | status=COMPLETED without delivery_date returns HTTP 400 JOURNEY-013                    | JOURNEY-TC-023-003       |
| 4 | Updating another user's journey returns HTTP 403 JOURNEY-011                           | JOURNEY-TC-023-004       |
| 5 | Non-existent journeyId returns HTTP 404 JOURNEY-010                                    | JOURNEY-TC-023-005       |
| 6 | Updating COMPLETED/ARCHIVED journey returns HTTP 400 JOURNEY-012                       | JOURNEY-TC-023-006       |
| 7 | No JWT returns HTTP 401                                                                | JOURNEY-TC-023-007       |
| 8 | DB updated_at advances after successful update                                         | JOURNEY-TC-023-INT-001   |
| 9 | journey_type and owner_user_id are never modified by PUT                               | JOURNEY-TC-023-INT-001   |
| 10 | AUDIT event JOURNEY_UPDATED emitted on every successful update                         | JOURNEY-TC-023-001, 002  |

---

*End of CB-JOURNEY-IMP-002-TEST — UC23 Update Mother Journey Test-Spec v1.0*
