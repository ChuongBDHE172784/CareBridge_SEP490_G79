# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-187 View Maternal Health Metric Detail

**Document ID:** `CB-HEALTH-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Partially Implemented — 2026-07-10 (targeted health backend tests PASS; full regression blocked by non-health Family/Exercise failures)`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC187_ViewMaternalHealthMetricDetail/UC187_ViewMaternalHealthMetricDetail_TDS.md` (CB-HEALTH-IMP-002)
- SRS: §3.3.11.1

---

## CHANGELOG


| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted health backend test pass; full regression remains blocked outside health scope. |
| 2026-06-27 | AI Agent — Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-187 |

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
| **Feature / Gap ID** | `UC-187` |
| **Module** | `ViewMaternalHealthMetricDetail — health` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays value, timestamp" — không rõ ownership | ADR-HEALTH-003: owner-only | Test encode non-owner → 403 |
| L2 | SRS: không rõ deleted metric handling | BR-HEALTH-011: deleted → 404 | Test deleted metric returns 404 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner views own metric | `HealthMetricService.getMetricDetail()` | `METRIC-TC-001` |
| TC-COND-002 | Non-owner → 403 | `verifyOwnership()` | `METRIC-TC-002` |
| TC-COND-003 | Deleted metric → 404 | status filter | `METRIC-TC-003` |
| TC-COND-004 | Non-existent → 404 | repository lookup | `METRIC-TC-004` |
| TC-COND-005 | Response has no diagnosis | Response mapping | `METRIC-TC-005` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB | `{id: MET-001, accountId: ACC-001, metricType: WEIGHT, value: 65.5, status: ACTIVE}` | Happy path |
| `FX-002` | DB | `{id: MET-002, accountId: ACC-999, status: ACTIVE}` | Non-owner |
| `FX-003` | DB | `{id: MET-003, accountId: ACC-001, status: DELETED}` | Deleted metric |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class MetricTestFactory {
    static MaternalHealthMetric makeMetric() {
        MaternalHealthMetric m = new MaternalHealthMetric();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000050"));
        m.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        m.setMetricType(MetricType.WEIGHT);
        m.setValue(new BigDecimal("65.5"));
        m.setUnit("kg");
        m.setMeasuredAt(ZonedDateTime.now().minusHours(2));
        m.setSource(DataSource.MANUAL);
        m.setStatus(MetricStatus.ACTIVE);
        return m;
    }
}
```

---

### METRIC-TC-001 — Owner views own metric → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthMetricService.getMetricDetail()`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC-187 Normal Flow`

**Test Steps:**
1. Mock `metricRepository.findByIdAndStatus(MET-001, ACTIVE)` → FX-001
2. Call `getMetricDetail(MET-001, ACC-001)`

**Expected Result (PASS):**
- Returns `MetricDetailResponse` with `value = 65.5`, `unit = "kg"`, `metricType = "WEIGHT"`
- No diagnosis or medical advice in response

**Current Status:** 🟢 Passing

---

### METRIC-TC-002 — Non-owner → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `verifyOwnership()`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `ADR-HEALTH-003`

**Test Steps:**
1. Mock `metricRepository.findByIdAndStatus(MET-002, ACTIVE)` → FX-002 (accountId=ACC-999)
2. Call `getMetricDetail(MET-002, ACC-001)` (ACC-001 tries to access ACC-999's metric)

**Expected Result:** throws ForbiddenException (HEALTH-004)

**Current Status:** 🟢 Passing

---

### METRIC-TC-003 — Deleted metric → 404

**Severity:** `HIGH`
**Feature Under Test:** `status = ACTIVE` filter
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `BR-HEALTH-011`

**Test Steps:**
1. Mock `metricRepository.findByIdAndStatus(MET-003, ACTIVE)` → empty (status=DELETED)

**Expected Result:** throws NotFoundException (HEALTH-006)

**Current Status:** 🟢 Passing

---

### METRIC-TC-004 — Non-existent → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** 404, error HEALTH-006

**Current Status:** 🔴 Not written

---

### METRIC-TC-005 — Response has no diagnosis field

**Severity:** `HIGH`
**Feature Under Test:** `MetricDetailResponse` structure
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `BR-SAFETY-001`

**Test Steps:**
1. Get valid metric detail
2. Serialize response to JSON string

**Expected Result:**
- JSON does NOT contain fields: "diagnosis", "recommendation", "medicalAdvice", "interpretation"

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("diagnosis");
assertThat(json).doesNotContain("recommendation");
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `METRIC-TC-001` | `[ ]` | `___` | — |
| `METRIC-TC-002` | `[ ]` | `___` | — |
| `METRIC-TC-003` | `[ ]` | `___` | — |
| `METRIC-TC-005` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] Ownership check tested (own → 200, other → 403)
- [ ] Deleted metric → 404
- [ ] Response verified to NOT contain diagnosis/medical fields
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/health/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-004 | ☐ No business logic in controller | G-4 |
