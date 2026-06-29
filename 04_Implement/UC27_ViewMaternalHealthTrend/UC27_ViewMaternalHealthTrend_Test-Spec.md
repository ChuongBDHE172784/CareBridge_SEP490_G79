# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Đặc tả Kiểm thử Hướng Phát triển — UC-27 View Maternal Health Trend

**Document ID:** `CB-JOURNEY-IMP-006-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**References:** `CB-JOURNEY-IMP-006`, SRS §3.3.1.6

---

## 1. Thông tin Module

| Field                     | Value                                                    |
| ------------------------- | -------------------------------------------------------- |
| **Feature / Gap ID**      | `UC-27`                                                  |
| **Module**                | `ViewMaternalHealthTrend — Bounded Context: journey`     |
| **Spec gốc**              | `CB-JOURNEY-IMP-006`                                     |
| **Data Classification**   | `Sensitive-PII`                                          |
| **Compliance Scope**      | `BR-RBAC, BR-PRIVACY, BR-SAFETY`                        |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                    | Thực tế                                            | Fix áp dụng trong test                         |
| - | ------------------------------------------ | -------------------------------------------------- | ---------------------------------------------- |
| L1 | Empty result handling không specify       | ADR: 200 + empty array, KHÔNG 404                  | Test assert 200 với empty dataPoints           |
| L2 | Sort order không specify                  | Cần ASC cho chart rendering                        | Test assert data sorted by measuredAt ASC      |
| L3 | BLOOD_PRESSURE dual value not clarified   | Both valueNumeric + valueSecondary in each point   | Test assert both fields populated for BP       |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
ViewMaternalHealthTrend module:
├── Service (MetricServiceImpl.getMetricTrend() — mock repos)
├── Controller (MetricController — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL)

Out of scope:
├── MaternalHealthMetric entity internals (tested in UC-25/26)
├── Chart rendering (client-side)
```

### TDS-02 — Test Basis

| Source                     | Items Derived                             |
| -------------------------- | ----------------------------------------- |
| `SRS UC-27 §3.3.1.6`      | View trend data for chart rendering       |
| `ADR-JOURNEY-006-001`     | Raw data only, no statistical analysis    |
| `ADR-JOURNEY-006-002`     | metricType required                       |
| `BR-SAFETY`               | No medical interpretation in response     |

### TDS-03 — Test Conditions

| Condition ID   | Test Condition                              | Test Cases          |
| -------------- | ------------------------------------------- | ------------------- |
| TC-COND-27-01  | Valid metricType + data exists → data points | METRIC-TC-027-001   |
| TC-COND-27-02  | No data in range → empty array              | METRIC-TC-027-002   |
| TC-COND-27-03  | Missing metricType → 400                    | METRIC-TC-027-003   |
| TC-COND-27-04  | Journey not owned → 403                     | METRIC-TC-027-004   |
| TC-COND-27-05  | BLOOD_PRESSURE → dual values                | METRIC-TC-027-005   |
| TC-COND-27-06  | No JWT → 401                                | METRIC-TC-027-006   |

### TDS-05 — Test Data

| Fixture ID | Type    | Value                                          |
| ---------- | ------- | ---------------------------------------------- |
| `FX-27-01` | DB seed | 5 WEIGHT metrics for journey, spanning 3 months |
| `FX-27-02` | DB seed | 3 BLOOD_PRESSURE metrics with dual values      |
| `FX-27-03` | JWT     | Valid JWT for Mother owning the journey         |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class MetricTrendTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000027");
    static UUID JOURNEY_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000027");

    static MotherJourney makeJourney() {
        return MotherJourney.builder()
            .journeyId(JOURNEY_ID).ownerUserId(MOTHER_ID)
            .journeyType(JourneyType.PREGNANCY).status(JourneyStatus.ACTIVE).build();
    }

    static List<MaternalHealthMetric> makeWeightMetrics(int count) {
        return IntStream.range(0, count).mapToObj(i ->
            MaternalHealthMetric.builder()
                .journeyId(JOURNEY_ID).metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("62").add(new BigDecimal(i)))
                .unit("kg").measuredAt(Instant.now().minus(count - i, ChronoUnit.DAYS))
                .build()
        ).toList();
    }
}
```

---

### METRIC-TC-027-001 — Happy path: WEIGHT trend

**Severity:** `HIGH` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-01

**Test Steps:**
1. Arrange: mock journeyRepo → return journey owned by user; mock metricRepo → return 5 WEIGHT metrics
2. Act: `metricService.getMetricTrend(userId, journeyId, WEIGHT, from, to)`
3. Assert: response.dataPoints.size() == 5, sorted by measuredAt ASC, metricType == "WEIGHT"

---

### METRIC-TC-027-002 — Empty range → 200 with empty array

**Severity:** `MEDIUM` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-02

**Test Steps:**
1. Arrange: mock metricRepo → return empty list
2. Act: `metricService.getMetricTrend(userId, journeyId, BLOOD_GLUCOSE, from, to)`
3. Assert: response.dataPoints is empty list, HTTP 200 (NOT 404)

---

### METRIC-TC-027-003 — Missing metricType → 400

**Severity:** `MEDIUM` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-03

**Test Steps:**
1. Act: `MockMvc.perform(GET("/api/v1/journeys/{id}/metrics"))` — no metricType param
2. Assert: HTTP 400

---

### METRIC-TC-027-004 — Journey not owned → 403

**Severity:** `HIGH` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-04

**Test Steps:**
1. Arrange: journey.ownerUserId = different UUID
2. Act: `metricService.getMetricTrend(userId, journeyId, WEIGHT, from, to)`
3. Assert: throws exception with code METRIC-021

---

### METRIC-TC-027-005 — BLOOD_PRESSURE → dual values

**Severity:** `HIGH` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-05

**Test Steps:**
1. Arrange: 3 BP metrics with valueNumeric (diastolic) + valueSecondary (systolic)
2. Act: `metricService.getMetricTrend(userId, journeyId, BLOOD_PRESSURE, from, to)`
3. Assert: each dataPoint has non-null valueNumeric AND valueSecondary

---

### METRIC-TC-027-006 — No JWT → 401

**Severity:** `CRITICAL` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-27-06

**Test Steps:**
1. Act: `MockMvc.perform(GET(...))` without Authorization header
2. Assert: HTTP 401

---

### METRIC-TC-027-INT-001 — Integration: seeded data matches response

**Severity:** `HIGH` | **TDD Phase:** 🔴 RED

**Preconditions:** Testcontainers PostgreSQL, Flyway applied, seed 5 WEIGHT metrics

**Test Steps:**
1. Insert 5 WEIGHT metrics with known values into DB
2. Call GET endpoint
3. Assert: response.dataPoints.size() == 5, values match seeded data, sorted ASC

---

## 5. Red-Green-Refactor Tracker

| TC ID                 | Test File                           | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
| --------------------- | ----------------------------------- | ------ | -------- | ----------- |
| METRIC-TC-027-001     | MetricServiceTrendTest.java         | [ ]    | —        | —           |
| METRIC-TC-027-002     | MetricServiceTrendTest.java         | [ ]    | —        | —           |
| METRIC-TC-027-003     | MetricControllerTrendTest.java      | [ ]    | —        | —           |
| METRIC-TC-027-004     | MetricServiceTrendTest.java         | [ ]    | —        | —           |
| METRIC-TC-027-005     | MetricServiceTrendTest.java         | [ ]    | —        | —           |
| METRIC-TC-027-006     | MetricControllerTrendTest.java      | [ ]    | —        | —           |
| METRIC-TC-027-INT-001 | MetricTrendIntegrationTest.java     | [ ]    | —        | —           |

### Red Gate Stub

```java
@Override
public MetricTrendResponse getMetricTrend(UUID userId, UUID journeyId,
        MetricType metricType, Instant from, Instant to) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

---

## 6. Entry / Exit Criteria

### Entry
- [ ] TDS CB-JOURNEY-IMP-006 reviewed
- [ ] UC-25 (AddMetric) implemented — data source available

### Exit (DoD)
- [ ] All unit tests green
- [ ] Integration test green
- [ ] Coverage ≥ 80% for getMetricTrend()
- [ ] No medical interpretation in response
- [ ] Sorted by measuredAt ASC confirmed

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/carejourney/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu                               | Check | Gate |
| --------- | --------------------- | --------------------------------------- | ----- | ---- |
| AP-AI-001 | Unconstrained Gen     | TC không reference ADR/TDS constraint   | ☐     | G-0  |
| AP-AI-002 | Green-from-Birth      | Test PASS với throw stub                | ☐     | G-2★ |
| AP-AI-003 | Implicit Decision     | Test compute trend line in expected     | ☐     | G-1  |

---

*TDD Template v2.0 — UC-27 View Maternal Health Trend*
*Status: Draft*
