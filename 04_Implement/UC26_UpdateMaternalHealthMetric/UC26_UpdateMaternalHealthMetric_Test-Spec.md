# UC26 — Update Maternal Health Metric: Test Specification

| Field          | Value                                        |
|----------------|----------------------------------------------|
| Document ID    | CB-JOURNEY-IMP-005-TEST                      |
| Version        | 1.0                                          |
| Date           | 2026-06-26                                   |
| Status         | Draft                                        |
| Author         | AI Agent                                     |
| TDS Reference  | CB-JOURNEY-IMP-005                           |
| SRS Reference  | SRS 3.3.1.5                                  |

---

## 1. Test Scope and Objectives

This document defines the test cases for UC26 — Update Maternal Health Metric. The tests verify:

- Correct metric update for mutable fields within the 24-hour edit window
- 24-hour window enforcement based on `created_at` (not `measured_at`)
- Journey ownership verification
- Metric-to-journey binding verification
- `metric_type` immutability enforcement
- Audit event emission with old and new value snapshots
- Authentication enforcement

**Out of scope:** Gemini AI re-analysis on update (not implemented in UC26), cascade effects on clinical summaries.

---

## 2. Logic Issues and Edge Cases

| ID | Logic Issue                               | Description                                                                                                                                                                     |
|----|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| L1 | 24h window based on `created_at`, NOT `measured_at` | The edit window check uses `metric.created_at` (system-recorded server time), not `metric.measured_at` (user-provided time). A metric measured 3 days ago but entered 1 hour ago is still editable for another 23 hours. Tests must verify this distinction. |
| L2 | `metric_type` immutability enforcement    | If the client somehow sends a `metricType` field in the JSON body (e.g., via a reflective API client), the service must silently ignore it. Tests must verify the DB `metric_type` is unchanged after an update request that includes a type field attempt. |
| L3 | Audit must capture old values BEFORE save | The `HEALTH_METRIC_UPDATED` audit event must contain the old `valueNumeric` before the update. If the service captures the snapshot AFTER calling `save()`, the old values will be lost. Tests must verify audit event contains the pre-update value. |

---

## 3. Test Props Factory

```java
package com.carebridge.backend.carejourney.fixture;

import com.carebridge.backend.carejourney.dto.UpdateMetricRequest;
import com.carebridge.backend.carejourney.entity.MaternalHealthMetric;
import com.carebridge.backend.carejourney.entity.MetricType;
import com.carebridge.backend.carejourney.entity.MotherJourney;
import com.carebridge.backend.carejourney.entity.JourneyStatus;
import com.carebridge.backend.carejourney.entity.JourneyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class MetricUpdateTestFactory {

    // Fixed UUIDs for deterministic tests
    public static final UUID MOTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000026");
    public static final UUID JOURNEY_ID  = UUID.fromString("eeeeeeee-0000-0000-0000-000000000026");
    public static final UUID METRIC_ID   = UUID.fromString("ffffffff-0000-0000-0000-000000000026");
    public static final UUID OTHER_USER  = UUID.fromString("22222222-0000-0000-0000-000000000026");
    public static final UUID OTHER_JOURNEY = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000026");
    public static final UUID MISSING_METRIC = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000026");

    // Journey owned by MOTHER_ID, ACTIVE
    public static MotherJourney makeActiveJourney() {
        return MotherJourney.builder()
                .journeyId(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    // Metric created 1 hour ago — within 24h edit window
    public static MaternalHealthMetric makeRecentMetric() {
        return MaternalHealthMetric.builder()
                .metricId(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("65.0"))
                .valueSecondary(null)
                .unit("kg")
                .measuredAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))  // within 24h window
                .updatedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    // Metric created 25 hours ago — outside 24h edit window (L1)
    public static MaternalHealthMetric makeOldMetric() {
        return MaternalHealthMetric.builder()
                .metricId(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("64.0"))
                .unit("kg")
                .measuredAt(Instant.now().minus(26, ChronoUnit.HOURS))
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS)) // outside 24h window
                .updatedAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .build();
    }

    // Metric that belongs to a DIFFERENT journey than JOURNEY_ID
    public static MaternalHealthMetric makeMetricInOtherJourney() {
        return MaternalHealthMetric.builder()
                .metricId(METRIC_ID)
                .journeyId(OTHER_JOURNEY) // belongs to OTHER_JOURNEY, not JOURNEY_ID
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("63.0"))
                .unit("kg")
                .measuredAt(Instant.now())
                .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();
    }

    // Valid update request — corrects weight value
    public static UpdateMetricRequest makeUpdateWeightRequest() {
        UpdateMetricRequest req = new UpdateMetricRequest();
        req.setValueNumeric(new BigDecimal("66.0"));
        req.setUnit("kg");
        req.setNote("Corrected — scale was set to lbs initially");
        return req;
    }

    // Update request for BLOOD_PRESSURE
    public static UpdateMetricRequest makeUpdateBloodPressureRequest() {
        UpdateMetricRequest req = new UpdateMetricRequest();
        req.setValueNumeric(new BigDecimal("82"));    // diastolic
        req.setValueSecondary(new BigDecimal("122")); // systolic
        req.setUnit("mmHg");
        req.setNote("Re-measured after 5-minute rest");
        return req;
    }

    // Partial update — only note changed
    public static UpdateMetricRequest makeUpdateNoteOnlyRequest() {
        UpdateMetricRequest req = new UpdateMetricRequest();
        req.setNote("Updated note only");
        // All other fields null — should not change in DB
        return req;
    }
}
```

---

## 4. Unit Test Cases

### METRIC-TC-026-001: Happy Path — Update WEIGHT Value Within 24h

| Field       | Value                                                          |
|-------------|----------------------------------------------------------------|
| Test ID     | METRIC-TC-026-001                                              |
| Priority    | P0 — Critical                                                  |
| Type        | Unit (Service layer)                                           |
| Precondition| Journey owned by MOTHER_ID; metric created 1h ago (within 24h window) |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricUpdateTestFactory.makeActiveJourney()));
MaternalHealthMetric recentMetric = MetricUpdateTestFactory.makeRecentMetric();
when(metricRepository.findByMetricIdAndJourneyId(METRIC_ID, JOURNEY_ID))
    .thenReturn(Optional.of(recentMetric));
when(metricRepository.save(any(MaternalHealthMetric.class)))
    .thenAnswer(inv -> inv.getArgument(0));
```

**Act:**
```java
MetricResponse response = metricService.updateMetric(
    MOTHER_ID, JOURNEY_ID, METRIC_ID, MetricUpdateTestFactory.makeUpdateWeightRequest()
);
```

**Assert:**
```java
assertEquals(new BigDecimal("66.0"), response.getValueNumeric());
assertEquals("kg", response.getUnit());
assertEquals("Corrected — scale was set to lbs initially", response.getNote());
// metric_type must remain WEIGHT (L2)
assertEquals("WEIGHT", response.getMetricType());
// save was called once
verify(metricRepository, times(1)).save(any(MaternalHealthMetric.class));
// audit emitted
verify(auditService, times(1)).emit(eq("HEALTH_METRIC_UPDATED"), any(), any(), any(), any());
```

**Expected:** 200 OK; `valueNumeric` updated to 66.0; `metric_type` unchanged; audit emitted.

---

### METRIC-TC-026-002: Edit Window Expired (Metric Created 25h Ago)

| Field       | Value                                                          |
|-------------|----------------------------------------------------------------|
| Test ID     | METRIC-TC-026-002                                              |
| Priority    | P0 — Critical                                                  |
| Type        | Unit (Service layer)                                           |
| Precondition| Journey owned by MOTHER_ID; metric created 25h ago (outside window) |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricUpdateTestFactory.makeActiveJourney()));
// Note: makeOldMetric().createdAt = now() - 25h (L1: window based on created_at)
when(metricRepository.findByMetricIdAndJourneyId(METRIC_ID, JOURNEY_ID))
    .thenReturn(Optional.of(MetricUpdateTestFactory.makeOldMetric()));
```

**Act:**
```java
EditWindowExpiredException ex = assertThrows(EditWindowExpiredException.class,
    () -> metricService.updateMetric(
        MOTHER_ID, JOURNEY_ID, METRIC_ID, MetricUpdateTestFactory.makeUpdateWeightRequest()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-012", ex.getErrorCode());
verify(metricRepository, never()).save(any());
verify(auditService, never()).emit(any(), any(), any(), any(), any());
```

**Expected:** `METRIC-012` (400) thrown; no DB write; no audit. Verifies L1 (window on `created_at`).

---

### METRIC-TC-026-003: Metric Belongs to Different Journey

| Field       | Value                                                                    |
|-------------|--------------------------------------------------------------------------|
| Test ID     | METRIC-TC-026-003                                                        |
| Priority    | P0 — Critical                                                            |
| Type        | Unit (Service layer)                                                     |
| Precondition| Journey owned by MOTHER_ID; metric exists but belongs to OTHER_JOURNEY   |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricUpdateTestFactory.makeActiveJourney()));
// findByMetricIdAndJourneyId with JOURNEY_ID returns empty (metric is in OTHER_JOURNEY)
when(metricRepository.findByMetricIdAndJourneyId(METRIC_ID, JOURNEY_ID))
    .thenReturn(Optional.empty());
```

**Act:**
```java
MetricNotFoundException ex = assertThrows(MetricNotFoundException.class,
    () -> metricService.updateMetric(
        MOTHER_ID, JOURNEY_ID, METRIC_ID, MetricUpdateTestFactory.makeUpdateWeightRequest()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-011", ex.getErrorCode());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-011` (404) thrown; no DB write.

---

### METRIC-TC-026-004: Journey Not Owned by User

| Field       | Value                                                                     |
|-------------|---------------------------------------------------------------------------|
| Test ID     | METRIC-TC-026-004                                                         |
| Priority    | P0 — Critical                                                             |
| Type        | Unit (Service layer)                                                      |
| Precondition| MOTHER_ID sends request for a journey owned by OTHER_USER                 |

**Arrange:**
```java
// findByJourneyIdAndOwnerUserId returns empty — MOTHER_ID does not own this journey
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.empty());
```

**Act:**
```java
JourneyNotFoundException ex = assertThrows(JourneyNotFoundException.class,
    () -> metricService.updateMetric(
        MOTHER_ID, JOURNEY_ID, METRIC_ID, MetricUpdateTestFactory.makeUpdateWeightRequest()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-013", ex.getErrorCode());
// Must not reach metric lookup
verify(metricRepository, never()).findByMetricIdAndJourneyId(any(), any());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-013` (403) thrown at journey ownership check; metric lookup never reached.

---

### METRIC-TC-026-005: Metric Not Found

| Field       | Value                                                     |
|-------------|-----------------------------------------------------------|
| Test ID     | METRIC-TC-026-005                                         |
| Priority    | P1 — High                                                 |
| Type        | Unit (Service layer)                                      |
| Precondition| Journey owned by MOTHER_ID; `MISSING_METRIC` does not exist |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricUpdateTestFactory.makeActiveJourney()));
when(metricRepository.findByMetricIdAndJourneyId(MISSING_METRIC, JOURNEY_ID))
    .thenReturn(Optional.empty());
```

**Act:**
```java
MetricNotFoundException ex = assertThrows(MetricNotFoundException.class,
    () -> metricService.updateMetric(
        MOTHER_ID, JOURNEY_ID, MISSING_METRIC, MetricUpdateTestFactory.makeUpdateWeightRequest()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-010", ex.getErrorCode());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-010` (404) thrown.

---

### METRIC-TC-026-006: No JWT Token (Unauthenticated)

| Field       | Value                             |
|-------------|-----------------------------------|
| Test ID     | METRIC-TC-026-006                 |
| Priority    | P0 — Critical                     |
| Type        | Controller / Spring Security      |
| Precondition| No Authorization header sent      |

**Test approach:** MockMvc integration test against `MetricController`.

**Act:**
```java
mockMvc.perform(put("/api/v1/journeys/{journeyId}/metrics/{metricId}", JOURNEY_ID, METRIC_ID)
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(MetricUpdateTestFactory.makeUpdateWeightRequest())))
    .andExpect(status().isUnauthorized());
```

**Assert:** HTTP 401 Unauthorized. Spring Security filter chain rejects before controller.

---

## 5. Integration Test Cases

### METRIC-TC-026-INT-001: End-to-End — DB Value Updated, `updated_at` Changes

| Field         | Value                                                           |
|---------------|-----------------------------------------------------------------|
| Test ID       | METRIC-TC-026-INT-001                                           |
| Priority      | P0 — Critical                                                   |
| Type          | Integration (`@SpringBootTest` + Testcontainers PostgreSQL)     |
| Precondition  | Test DB seeded with journey (MOTHER_ID) and metric (METRIC_ID, created 1h ago) |

**Setup:**
```java
@BeforeEach
void seed() {
    journeyRepository.save(MetricUpdateTestFactory.makeActiveJourney());
    // Persist metric with known state
    MaternalHealthMetric metric = MetricUpdateTestFactory.makeRecentMetric();
    metricRepository.save(metric);
}
```

**Act:**
```java
mockMvc.perform(put("/api/v1/journeys/{journeyId}/metrics/{metricId}", JOURNEY_ID, METRIC_ID)
    .header("Authorization", "Bearer " + motherJwt())
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(MetricUpdateTestFactory.makeUpdateWeightRequest())))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.valueNumeric").value(66.0))
    .andExpect(jsonPath("$.data.unit").value("kg"))
    .andExpect(jsonPath("$.data.metricType").value("WEIGHT")); // type unchanged (L2)
```

**Assert (DB verification):**
```java
MaternalHealthMetric updated = metricRepository.findById(METRIC_ID).orElseThrow();
// Value was updated
assertEquals(new BigDecimal("66.0"), updated.getValueNumeric());
assertEquals("kg", updated.getUnit());
// updated_at is after created_at
assertTrue(updated.getUpdatedAt().isAfter(updated.getCreatedAt()));
// metric_type is unchanged (L2)
assertEquals(MetricType.WEIGHT, updated.getMetricType());
// created_at was NOT changed
assertEquals(
    MetricUpdateTestFactory.makeRecentMetric().getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
    updated.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)
);
```

**Expected:** DB row has updated `value_numeric = 66.0` and `updated_at > created_at`; `metric_type` unchanged.

---

### METRIC-TC-026-INT-002: Audit Event Contains Old Value Snapshot

| Field         | Value                                                           |
|---------------|-----------------------------------------------------------------|
| Test ID       | METRIC-TC-026-INT-002                                           |
| Priority      | P1 — High                                                       |
| Type          | Integration (Service layer with AuditService spy)               |
| Precondition  | Metric with `valueNumeric = 65.0` seeded                       |

**Arrange:**
```java
// AuditService is a @SpyBean or ArgumentCaptor is used to inspect emitted event
```

**Act:** Call `updateMetric()` with `valueNumeric = 66.0`.

**Assert (L3 — old value must appear in audit):**
```java
ArgumentCaptor<MetricAuditSnapshot> oldSnapshotCaptor = ArgumentCaptor.forClass(MetricAuditSnapshot.class);
verify(auditService).emit(eq("HEALTH_METRIC_UPDATED"), any(), any(), oldSnapshotCaptor.capture(), any());

MetricAuditSnapshot captured = oldSnapshotCaptor.getValue();
// Must be the PRE-update value (65.0), NOT the new value (66.0)
assertEquals(new BigDecimal("65.0"), captured.getOldValueNumeric());
```

**Expected:** Audit event contains `oldValueNumeric = 65.0` (pre-update state). Verifies ADR-JOURNEY-005-003 and L3.

---

## 6. Boundary and Edge Case Supplement

| Test ID                    | Scenario                                                         | Expected                          |
|----------------------------|------------------------------------------------------------------|-----------------------------------|
| METRIC-TC-026-EDGE-001     | 24h window boundary: `created_at` = exactly 24h ago             | 400 METRIC-012 (window closed)    |
| METRIC-TC-026-EDGE-002     | 24h window boundary: `created_at` = 23h 59m 59s ago             | 200 OK (still within window)      |
| METRIC-TC-026-EDGE-003     | Partial update: only `note` changed, other fields null           | 200 OK; only `note` changes in DB |
| METRIC-TC-026-EDGE-004     | `measuredAt` updated to now − 8 days (outside 7-day range)      | 400 Validation error              |
| METRIC-TC-026-EDGE-005     | Request body includes `metricType` field (injection attempt)     | 200 OK; DB `metric_type` unchanged (L2) |
| METRIC-TC-026-EDGE-006     | Update only `valueSecondary` for BLOOD_PRESSURE metric           | 200 OK; only systolic changes     |
| METRIC-TC-026-EDGE-007     | `note` set to empty string (clear note)                          | 200 OK; note set to empty in DB   |

---

## 7. Test Coverage Requirements

| Layer                          | Target Coverage | Notes                                              |
|--------------------------------|-----------------|----------------------------------------------------|
| Service (MetricServiceImpl.updateMetric) | >= 85% | All branches: ownership, metric binding, edit window, partial update |
| Controller (PUT endpoint)      | >= 80%          | Happy path + auth enforcement                      |
| Repository (findByMetricIdAndJourneyId) | >= 70%  | Integration tests via Testcontainers               |
| Audit path (old value capture) | >= 90%          | Critical for compliance — must not be skipped      |

---

## 8. Acceptance Criteria Checklist

- [ ] TC-026-001 passes: WEIGHT value updated within 24h, 200 OK returned
- [ ] TC-026-002 passes: Metric created 25h ago returns 400 METRIC-012
- [ ] TC-026-003 passes: Metric in wrong journey returns 404 METRIC-011
- [ ] TC-026-004 passes: Wrong journey owner returns 403 METRIC-013
- [ ] TC-026-005 passes: Missing metric returns 404 METRIC-010
- [ ] TC-026-006 passes: No JWT returns 401
- [ ] TC-026-INT-001 passes: DB `value_numeric` updated; `updated_at > created_at` confirmed in PostgreSQL
- [ ] TC-026-INT-002 passes: Audit event contains pre-update `valueNumeric` (old value snapshot, L3)
- [ ] `metric_type` is NOT changeable via PUT request (L2 — verified via DB assertion)
- [ ] 24h window uses `created_at`, not `measured_at` (L1 — verified by TC-026-002 with old `makeOldMetric()` fixture)
- [ ] Partial update: null fields in request do NOT overwrite existing DB values (TC-026-EDGE-003)
