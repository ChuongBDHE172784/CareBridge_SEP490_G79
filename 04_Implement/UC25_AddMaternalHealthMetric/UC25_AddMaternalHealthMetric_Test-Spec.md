# UC25 — Add Maternal Health Metric: Test Specification

| Field          | Value                                        |
|----------------|----------------------------------------------|
| Document ID    | CB-JOURNEY-IMP-004-TEST                      |
| Version        | 1.0                                          |
| Date           | 2026-06-26                                   |
| Status         | Draft                                        |
| Author         | AI Agent                                     |
| TDS Reference  | CB-JOURNEY-IMP-004                           |
| SRS Reference  | SRS 3.3.1.4                                  |

---

## 1. Test Scope and Objectives

This document defines the test cases for UC25 — Add Maternal Health Metric. The tests verify:

- Correct metric persistence for all supported metric types
- Journey ownership and active-status enforcement
- Blood pressure dual-value validation (diastolic + systolic)
- `measured_at` range validation
- Gemini AI graceful degradation (metric saved even when AI fails)
- Authentication enforcement
- Audit event emission

**Out of scope:** Gemini AI model accuracy, push notification delivery.

---

## 2. Logic Issues and Edge Cases

| ID | Logic Issue                          | Description                                                                                                                                    |
|----|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| L1 | BLOOD_PRESSURE dual value convention | `value_numeric` = diastolic (lower number, e.g., 80), `value_secondary` = systolic (higher number, e.g., 120). This is non-conventional (usually systolic is primary). Tests must verify correct column mapping. |
| L2 | Gemini AI fail-open behavior         | When Gemini times out or throws an exception, the metric is saved with `aiInsight=null` and `redFlagAlert=false`. The system does NOT fail-close. Tests must verify the 201 is still returned. |
| L3 | `measured_at` window boundaries      | Allowed range: `[now() − 7 days, now() + 5 min]`. Boundary values at exactly −7 days and exactly +5 min should be tested. A timestamp 1 second outside either boundary must be rejected. |
| L4 | Journey ownership via DB lookup      | Ownership must be validated by querying `mother_journeys.owner_user_id`, not by trusting the JWT subject alone or a request body field. Tests must cover a case where user authenticates correctly but accesses another user's journey. |

---

## 3. Test Props Factory

```java
package com.carebridge.backend.carejourney.fixture;

import com.carebridge.backend.carejourney.dto.AddMetricRequest;
import com.carebridge.backend.carejourney.entity.MaternalHealthMetric;
import com.carebridge.backend.carejourney.entity.MetricType;
import com.carebridge.backend.carejourney.entity.MotherJourney;
import com.carebridge.backend.carejourney.entity.JourneyStatus;
import com.carebridge.backend.carejourney.entity.JourneyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MetricTestFactory {

    // Fixed UUIDs for deterministic tests
    public static final UUID MOTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000025");
    public static final UUID JOURNEY_ID  = UUID.fromString("dddddddd-0000-0000-0000-000000000025");
    public static final UUID OTHER_USER  = UUID.fromString("11111111-0000-0000-0000-000000000025");
    public static final UUID OTHER_JOURNEY = UUID.fromString("cccccccc-0000-0000-0000-000000000025");

    public static MotherJourney makeActiveJourney() {
        return MotherJourney.builder()
                .journeyId(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeCompletedJourney() {
        return MotherJourney.builder()
                .journeyId(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.COMPLETED)
                .build();
    }

    public static MotherJourney makeOtherUsersJourney() {
        return MotherJourney.builder()
                .journeyId(OTHER_JOURNEY)
                .ownerUserId(OTHER_USER)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static AddMetricRequest makeWeightRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.WEIGHT);
        req.setValueNumeric(new BigDecimal("65.5"));
        req.setUnit("kg");
        req.setMeasuredAt(Instant.now());
        req.setSourceType("MANUAL");
        return req;
    }

    public static AddMetricRequest makeBloodPressureRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.BLOOD_PRESSURE);
        req.setValueNumeric(new BigDecimal("80"));    // diastolic
        req.setValueSecondary(new BigDecimal("120")); // systolic
        req.setUnit("mmHg");
        req.setMeasuredAt(Instant.now());
        req.setSourceType("MANUAL");
        return req;
    }

    public static AddMetricRequest makeBloodPressureMissingSecondary() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.BLOOD_PRESSURE);
        req.setValueNumeric(new BigDecimal("80"));
        // valueSecondary intentionally omitted
        req.setUnit("mmHg");
        req.setMeasuredAt(Instant.now());
        return req;
    }

    public static AddMetricRequest makeFutureMeasuredAtRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.WEIGHT);
        req.setValueNumeric(new BigDecimal("66.0"));
        req.setUnit("kg");
        req.setMeasuredAt(Instant.now().plusSeconds(600)); // +10 minutes — outside 5-min window
        return req;
    }

    public static AddMetricRequest makeBloodGlucoseRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.BLOOD_GLUCOSE);
        req.setValueNumeric(new BigDecimal("95"));
        req.setUnit("mg/dL");
        req.setMeasuredAt(Instant.now());
        req.setNote("Fasting glucose reading");
        return req;
    }
}
```

---

## 4. Unit Test Cases

### METRIC-TC-025-001: Happy Path — Add WEIGHT Metric

| Field       | Value                                                      |
|-------------|------------------------------------------------------------|
| Test ID     | METRIC-TC-025-001                                          |
| Priority    | P0 — Critical                                              |
| Type        | Unit (Service layer)                                       |
| Precondition| Journey `JOURNEY_ID` exists, owned by `MOTHER_ID`, status = ACTIVE |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricTestFactory.makeActiveJourney()));
when(metricRepository.save(any(MaternalHealthMetric.class)))
    .thenAnswer(inv -> { MaternalHealthMetric m = inv.getArgument(0); m.setMetricId(UUID.randomUUID()); return m; });
// Gemini mock: return a future that completes with no red flag
when(geminiIntegration.analyzeMetric(any(), any(), any()))
    .thenReturn(CompletableFuture.completedFuture(new AiInsightResult("", false)));
```

**Act:**
```java
MetricResponse response = metricService.addMetric(
    MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeWeightRequest()
);
```

**Assert:**
```java
assertNotNull(response.getMetricId());
assertEquals("WEIGHT", response.getMetricType());
assertEquals(new BigDecimal("65.5"), response.getValueNumeric());
assertNull(response.getValueSecondary());
assertEquals("kg", response.getUnit());
assertFalse(response.isRedFlagAlert());
verify(metricRepository, times(1)).save(any(MaternalHealthMetric.class));
verify(auditService, times(1)).emit(eq("HEALTH_METRIC_ADDED"), any(), any());
```

**Expected:** Response returned with correct values; `save()` called once; audit emitted.

---

### METRIC-TC-025-002: Happy Path — Add BLOOD_PRESSURE with Both Values

| Field       | Value                                                      |
|-------------|------------------------------------------------------------|
| Test ID     | METRIC-TC-025-002                                          |
| Priority    | P0 — Critical                                              |
| Type        | Unit (Service layer)                                       |
| Precondition| Journey active and owned by MOTHER_ID                      |

**Arrange:** Same mocks as TC-001. Request uses `makeBloodPressureRequest()`.

**Act:**
```java
MetricResponse response = metricService.addMetric(
    MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeBloodPressureRequest()
);
```

**Assert:**
```java
assertEquals("BLOOD_PRESSURE", response.getMetricType());
assertEquals(new BigDecimal("80"), response.getValueNumeric());   // diastolic
assertEquals(new BigDecimal("120"), response.getValueSecondary()); // systolic
assertEquals("mmHg", response.getUnit());

// Verify DB save captured correct column mapping
ArgumentCaptor<MaternalHealthMetric> captor = ArgumentCaptor.forClass(MaternalHealthMetric.class);
verify(metricRepository).save(captor.capture());
assertEquals(new BigDecimal("80"), captor.getValue().getValueNumeric());
assertEquals(new BigDecimal("120"), captor.getValue().getValueSecondary());
```

**Expected:** 201 with correct diastolic/systolic values in correct columns (L1).

---

### METRIC-TC-025-003: BLOOD_PRESSURE Missing valueSecondary (Systolic)

| Field       | Value                                 |
|-------------|---------------------------------------|
| Test ID     | METRIC-TC-025-003                     |
| Priority    | P0 — Critical                         |
| Type        | Unit (Service layer)                  |
| Precondition| Journey active and owned by MOTHER_ID |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricTestFactory.makeActiveJourney()));
```

**Act:**
```java
BloodPressureFieldException ex = assertThrows(BloodPressureFieldException.class,
    () -> metricService.addMetric(
        MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeBloodPressureMissingSecondary()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-005", ex.getErrorCode());
verify(metricRepository, never()).save(any());
verify(auditService, never()).emit(any(), any(), any());
```

**Expected:** `BloodPressureFieldException` thrown with code `METRIC-005`; no DB insert.

---

### METRIC-TC-025-004: Journey Not Owned by User (Authorization Failure)

| Field       | Value                                                                |
|-------------|----------------------------------------------------------------------|
| Test ID     | METRIC-TC-025-004                                                    |
| Priority    | P0 — Critical                                                        |
| Type        | Unit (Service layer)                                                 |
| Precondition| Journey exists but belongs to `OTHER_USER`, not the requesting MOTHER_ID |

**Arrange:**
```java
// findByJourneyIdAndOwnerUserId returns empty because MOTHER_ID is not the owner
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.empty());
```

**Act:**
```java
JourneyNotFoundException ex = assertThrows(JourneyNotFoundException.class,
    () -> metricService.addMetric(
        MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeWeightRequest()
    )
);
```

**Assert:**
```java
// Service should return 404 or 403 (per TDS: METRIC-002 = 403)
// Implementation may throw JourneyNotFoundException mapped to 403
assertEquals("METRIC-002", ex.getErrorCode());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-002` (403) thrown; no metric saved. Verifies L4 — ownership checked via DB.

---

### METRIC-TC-025-005: Journey Status = COMPLETED

| Field       | Value                                 |
|-------------|---------------------------------------|
| Test ID     | METRIC-TC-025-005                     |
| Priority    | P1 — High                             |
| Type        | Unit (Service layer)                  |
| Precondition| Journey owned by MOTHER_ID, status = COMPLETED |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricTestFactory.makeCompletedJourney()));
```

**Act:**
```java
JourneyNotActiveException ex = assertThrows(JourneyNotActiveException.class,
    () -> metricService.addMetric(
        MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeWeightRequest()
    )
);
```

**Assert:**
```java
assertEquals("METRIC-003", ex.getErrorCode());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-003` (400) thrown; no insert attempted.

---

### METRIC-TC-025-006: `measuredAt` More Than 5 Minutes in the Future

| Field       | Value                                 |
|-------------|---------------------------------------|
| Test ID     | METRIC-TC-025-006                     |
| Priority    | P1 — High                             |
| Type        | Unit (Service layer)                  |
| Precondition| Journey active and owned by MOTHER_ID |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricTestFactory.makeActiveJourney()));
AddMetricRequest req = MetricTestFactory.makeFutureMeasuredAtRequest();
// req.measuredAt = now + 10 minutes
```

**Act:**
```java
InvalidMeasuredAtException ex = assertThrows(InvalidMeasuredAtException.class,
    () -> metricService.addMetric(MOTHER_ID, JOURNEY_ID, req)
);
```

**Assert:**
```java
assertEquals("METRIC-004", ex.getErrorCode());
verify(metricRepository, never()).save(any());
```

**Expected:** `METRIC-004` (400) thrown. Verifies L3 boundary.

---

### METRIC-TC-025-007: No JWT Token (Unauthenticated Request)

| Field       | Value                          |
|-------------|--------------------------------|
| Test ID     | METRIC-TC-025-007              |
| Priority    | P0 — Critical                  |
| Type        | Controller / Spring Security   |
| Precondition| No Authorization header sent   |

**Test approach:** MockMvc integration test against `MetricController`.

**Act:**
```java
mockMvc.perform(post("/api/v1/journeys/{journeyId}/metrics", JOURNEY_ID)
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(MetricTestFactory.makeWeightRequest())))
    .andExpect(status().isUnauthorized());
```

**Assert:** HTTP 401 Unauthorized. Spring Security filter chain rejects before controller.

---

### METRIC-TC-025-008: Gemini AI Throws Exception — Metric Still Saved (Graceful Degradation)

| Field       | Value                                           |
|-------------|-------------------------------------------------|
| Test ID     | METRIC-TC-025-008                               |
| Priority    | P0 — Critical (BR-SAFETY)                       |
| Type        | Unit (Service layer)                            |
| Precondition| Journey active and owned; Gemini AI is mocked to throw |

**Arrange:**
```java
when(journeyRepository.findByJourneyIdAndOwnerUserId(JOURNEY_ID, MOTHER_ID))
    .thenReturn(Optional.of(MetricTestFactory.makeActiveJourney()));
when(metricRepository.save(any())).thenAnswer(inv -> {
    MaternalHealthMetric m = inv.getArgument(0);
    m.setMetricId(UUID.randomUUID());
    return m;
});
// Gemini throws RuntimeException (simulates timeout or API error)
when(geminiIntegration.analyzeMetric(any(), any(), any()))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Gemini timeout")));
```

**Act:**
```java
MetricResponse response = metricService.addMetric(
    MOTHER_ID, JOURNEY_ID, MetricTestFactory.makeWeightRequest()
);
```

**Assert:**
```java
// Metric must still be saved (L2 — fail-open)
assertNotNull(response.getMetricId());
assertEquals("WEIGHT", response.getMetricType());
// AI fields default to safe values
assertNull(response.getAiInsight());
assertFalse(response.isRedFlagAlert());
// Save and audit still called
verify(metricRepository, times(1)).save(any());
verify(auditService, times(1)).emit(eq("HEALTH_METRIC_ADDED"), any(), any());
```

**Expected:** HTTP 201 returned; `aiInsight=null`, `redFlagAlert=false`; no exception propagated to client. Verifies ADR-JOURNEY-004-002 and BR-SAFETY.

---

## 5. Integration Test Cases

### METRIC-TC-025-INT-001: End-to-End — DB Row Confirmed, Gemini Mocked

| Field         | Value                                                          |
|---------------|----------------------------------------------------------------|
| Test ID       | METRIC-TC-025-INT-001                                          |
| Priority      | P0 — Critical                                                  |
| Type          | Integration (`@SpringBootTest` + Testcontainers PostgreSQL)    |
| Precondition  | Test DB with mother_journeys seed for MOTHER_ID + JOURNEY_ID   |

**Setup:**
```java
@BeforeEach
void seedJourney() {
    journeyRepository.save(MetricTestFactory.makeActiveJourney());
    // Mock Gemini bean via @MockBean to avoid external calls
}
```

**Act:**
```java
// Authenticate as MOTHER_ID, POST request
mockMvc.perform(post("/api/v1/journeys/{journeyId}/metrics", JOURNEY_ID)
    .header("Authorization", "Bearer " + motherJwt())
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(MetricTestFactory.makeWeightRequest())))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.data.metricId").isNotEmpty())
    .andExpect(jsonPath("$.data.metricType").value("WEIGHT"))
    .andExpect(jsonPath("$.data.valueNumeric").value(65.5));
```

**Assert (DB verification):**
```java
List<MaternalHealthMetric> persisted = metricRepository.findByJourneyId(JOURNEY_ID);
assertEquals(1, persisted.size());
assertEquals(MetricType.WEIGHT, persisted.get(0).getMetricType());
assertEquals(new BigDecimal("65.5"), persisted.get(0).getValueNumeric());
assertNotNull(persisted.get(0).getCreatedAt());
```

**Expected:** Exactly 1 row in `maternal_health_metrics`; all fields match request.

---

## 6. Boundary and Edge Case Supplement

| Test ID                    | Scenario                                                      | Expected                   |
|----------------------------|---------------------------------------------------------------|----------------------------|
| METRIC-TC-025-EDGE-001     | `measuredAt` = exactly now − 7 days                           | 201 Created (boundary OK)  |
| METRIC-TC-025-EDGE-002     | `measuredAt` = now − 7 days − 1 second                        | 400 METRIC-004             |
| METRIC-TC-025-EDGE-003     | `measuredAt` = exactly now + 5 minutes                        | 201 Created (boundary OK)  |
| METRIC-TC-025-EDGE-004     | `measuredAt` = now + 5 minutes + 1 second                     | 400 METRIC-004             |
| METRIC-TC-025-EDGE-005     | `metricType = FETAL_MOVEMENT` with `valueNumeric = null`      | 201 Created (nulls allowed for symptom types) |
| METRIC-TC-025-EDGE-006     | `note` field = 2001 characters                                | 400 Validation error       |
| METRIC-TC-025-EDGE-007     | Journey not found at all (no row in DB)                       | 404 METRIC-001             |

---

## 7. Test Coverage Requirements

| Layer                    | Target Coverage | Notes                                             |
|--------------------------|-----------------|---------------------------------------------------|
| Service (MetricServiceImpl) | >= 85%       | All branches including AI graceful degradation    |
| Controller (MetricController) | >= 80%     | Happy path + auth enforcement                     |
| Repository (IMetricRepository) | >= 70%    | Integration tests via Testcontainers              |
| Validation (AddMetricRequest) | >= 90%     | Bean validation and custom validators             |

---

## 8. Acceptance Criteria Checklist

- [ ] TC-025-001 passes: WEIGHT metric saved, 201 returned
- [ ] TC-025-002 passes: BLOOD_PRESSURE saves diastolic in `value_numeric`, systolic in `value_secondary`
- [ ] TC-025-003 passes: Missing systolic for BP returns 400 METRIC-005
- [ ] TC-025-004 passes: Other user's journey returns 403 METRIC-002
- [ ] TC-025-005 passes: COMPLETED journey returns 400 METRIC-003
- [ ] TC-025-006 passes: Future `measuredAt` returns 400 METRIC-004
- [ ] TC-025-007 passes: No JWT returns 401
- [ ] TC-025-008 passes: Gemini error returns 201 (fail-open, L2)
- [ ] TC-025-INT-001 passes: DB row confirmed in Testcontainers PostgreSQL
- [ ] Audit event `HEALTH_METRIC_ADDED` emitted on every successful add
- [ ] No exception propagated to client when Gemini AI fails
- [ ] `redFlagAlert` defaults to `false` when AI is unavailable
