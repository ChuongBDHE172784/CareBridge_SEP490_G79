# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC69 — View Device Data Trend

**Document ID:** `CB-DEVICE-IMP-004-TEST`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Partially Implemented — 2026-07-10 (targeted backend tests PASS; full regression blocked by non-device Family/Exercise failures)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect / Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC69_ViewDeviceDataTrend/UC69_ViewDeviceDataTrend_TDS.md` (CB-DEVICE-IMP-004)
- `04_Implement/UC66_ConnectHealthDevice/UC66_ConnectHealthDevice_TDS.md` (shared `HealthDeviceConnection` entity, real table `health_device_connections`)
- `04_Implement/UC67_ImportDeviceDataManually/UC67_ImportDeviceDataManually_TDS.md` (shared `MaternalHealthMetric` provenance model)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.46`

> Viết test trước → chạy → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC69 (Draft) |
| 2026-07-02 | AI Agent — Test Designer (reconciliation) | **Corrected schema reference:** `device_connections` (invented, did not exist) → `health_device_connections` (real, `V1__init_schema.sql` L1115) — reconciled with UC130's independently-verified schema research. `sourceLabel` DEVICE-path assertions now reference `HealthDeviceConnection.deviceName`/`providerName` fixtures from the corrected UC66 `DeviceConnectionTestFactory`. |

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
| **Feature / Gap ID** | `CB-DEVICE-IMP-004` |
| **Module** | `View Device Data Trend — health.device` |
| **Spec gốc** | `CB-DEVICE-IMP-004` (TDS) |
| **Priority** | 🟠 P1 (High per SRS) |
| **Sprint** | `Device Sync And Care Edge Cases (TV2-Bách)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC66 health_device_connections`, `UC67 maternal_health_metrics` (imported data must exist) |
| **Downstream Consumers** | Mobile Health module UI (trend charts) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-DEVICE-IMP-004 §17`, `ADR-DEVICE-008 (Proposed)`, `ADR-DEVICE-009 (Accepted)` |
| **Constraints Injected** | C1 (read-only, no writes), C2 (no invented accuracy threshold — stub `false` until ADR-DEVICE-008 Accepted), C3 (ownership check before returning data), C4 (empty state returns 200, not 404), C5 (sourceLabel resolution rule) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-69 nói "accuracy warnings" nhưng không định nghĩa ngưỡng/logic cụ thể | ADR-DEVICE-008 còn `Proposed` — TDS §11.3 yêu cầu stub `accuracyWarning=false` cố định cho đến khi Accepted | Test khẳng định `accuracyWarning` LUÔN `false` trong bản build hiện tại (không test một logic ngưỡng chưa được duyệt) — ngăn AI tự bịa ngưỡng lâm sàng (AP-AI-001) |
| L2 | SRS không nói rõ hành vi khi không có dữ liệu trong khoảng thời gian truy vấn | TDS §6.2 (AF2): trả `200 {points:[], hasAnyData:false}`, KHÔNG 404 | Test verify HTTP 200 + `hasAnyData=false`, không phải 404 |
| L3 | SRS không đặc tả rõ ai được xem trend của Mother (Family/Partner/Expert?) | TDS §16 Authorization Matrix: chỉ `ROLE_MOTHER` sở hữu journey được xem; các role khác Open (O2) — mặc định strict deny | Test verify FAMILY/PARTNER/EXPERT gọi API đều nhận 403, không có ngoại lệ ngầm |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
View Device Data Trend (health.device) bao gồm các layer:
├── Service (DeviceTrendService.getTrend() — mock MaternalHealthMetricRepository + IHealthDeviceConnectionRepository)
├── Controller (@WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL — verify read-only, mixed-source aggregation)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-69 §3.3.1.46` | Mother xem trend dữ liệu thiết bị kèm source label + accuracy warning |
| `ADR-DEVICE-008 (Proposed)` | Accuracy warning là rule-based, KHÔNG có ngưỡng chính thức — stub `false` |
| `ADR-DEVICE-009 (Accepted)` | Aggregation on-demand, không cache/background job |
| `BR-RBAC` | Chỉ owner journey mới xem trend |
| `BR-PRIVACY` | Minimum necessary — không rò rỉ dữ liệu journey khác |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path: trend với dữ liệu hỗn hợp MANUAL + DEVICE, đúng thứ tự `measuredAt` tăng dần | `getTrend()` | `TREND-TC-001` |
| TC-COND-002 | Empty state: không có metric trong khoảng thời gian → 200 `{points:[], hasAnyData:false}` | `getTrend()` AF2 | `TREND-TC-002` |
| TC-COND-003 | `metricType` không hợp lệ → 400 `DEVICE-301` | Validation | `TREND-TC-003` |
| TC-COND-004 | `from > to` → 400 `DEVICE-301` | Validation | `TREND-TC-004` |
| TC-COND-005 | Caller không sở hữu `journeyId` → 403 `DEVICE-304` | Ownership (RBAC) | `TREND-TC-005` |
| TC-COND-006 | `journeyId` không tồn tại → 404 `DEVICE-302` | Not-found handling | `TREND-TC-006` |
| TC-COND-007 | `sourceLabel` resolve đúng: DEVICE → tên thiết bị (join `health_device_connections`); MANUAL → `"Manual entry"` | Source label logic | `TREND-TC-007` |
| TC-COND-008 | `accuracyWarning` LUÔN `false` trong bản build hiện tại (ADR-DEVICE-008 chưa Accepted — không được tự bịa ngưỡng) | Anti-hallucination guard | `TREND-TC-008` |
| TC-COND-009 | Read-only: `getTrend()` không gọi bất kỳ `save()`/`delete()` nào trên metric hoặc device repo | Data integrity (TDS §4.2) | `TREND-TC-009` |
| TC-COND-010 (Integration) | Full trend query qua Testcontainers với dữ liệu MANUAL/DEVICE hỗn hợp | End-to-end | `TREND-TC-INT-001` |
| TC-COND-011 (Security) | Cross-user: thao túng `journeyId` query param để đọc dữ liệu người khác → 403, không rò rỉ | Security/ownership | `TREND-TC-SEC-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | Owner vs non-owner caller; MANUAL vs DEVICE source | RBAC + provenance coverage |
| Boundary/Edge Case | Empty result set; `from == to`; `from > to` | Time-range validation |
| Error Guessing | Invalid `metricType` enum value; non-existent `journeyId` | Input robustness |
| Negative/Security Testing | `journeyId` param tampering across users | CWE-639 (IDOR) prevention |
| Anti-Hallucination Guard | `accuracyWarning` forced-false assertion while ADR-DEVICE-008 is Proposed | Prevents AI from inventing unapproved clinical thresholds (CLAUDE.md — AI provides guidance only) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-DEVICE-CONN-CONNECTED` | DB seed | Reuses `DeviceConnectionTestFactory.makeConnectedDevice()` (UC66 shared factory) | DEVICE-sourced trend point label resolution |
| `FX-METRIC-MANUAL` | DB seed | Reuses `ImportDeviceMetricTestFactory.makeExpectedMetric()` (UC67 shared factory, `sourceType=MANUAL`) | Manual-sourced trend point |
| `FX-METRIC-DEVICE` | DB seed | `ImportDeviceMetricTestFactory` variant with `sourceType=DEVICE`, `sourceReferenceId=<deviceConnectionId>` | Device-sourced trend point |
| `FX-METRIC-OTHER-JOURNEY` | DB seed | `ImportDeviceMetricTestFactory.makeManualRequest()` persisted under `OTHER_JOURNEY_ID` | Cross-journey leak negative test |
| `FX-JOURNEY-EMPTY` | DB seed | Journey id with zero metrics in queried range | Empty-state (AF2) test |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (IDOR/ownership) |
| Mobile (Flutter) | ✅ (service/repo) | — | ✅ | ✅ (widget test — trend chart, empty state, accuracy badge hidden while stub false) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Tái sử dụng `DeviceConnectionTestFactory` (UC66) và `ImportDeviceMetricTestFactory` (UC67). Không tạo factory trùng lặp cho fixtures đã có.

```java
// ═══════════════════════════════════════════════════════════
// Bổ sung method vào ImportDeviceMetricTestFactory.java (đã tạo ở UC67)
// để hỗ trợ UC69 test scenarios — KHÔNG tạo factory mới.
// Đặt tại: src/test/java/com/carebridge/backend/health/device/ImportDeviceMetricTestFactory.java
// ═══════════════════════════════════════════════════════════

class ImportDeviceMetricTestFactory {
    // ... (existing methods from UC67: makeManualRequest(), makeDeviceTaggedRequest(),
    //      makeExpectedMetric(), JOURNEY_ID, OTHER_JOURNEY_ID) ...

    // NEW for UC69:
    static MaternalHealthMetric makeDeviceSourcedMetric(UUID deviceConnectionId) {
        return MaternalHealthMetric.builder()
            .id(UUID.randomUUID())
            .journeyId(JOURNEY_ID)
            .metricType(MetricType.HEART_RATE)
            .valueNumeric(new BigDecimal("72"))
            .unit("bpm")
            .measuredAt(Instant.parse("2026-07-02T07:30:00Z"))
            .sourceType(DataSource.DEVICE)
            .sourceReferenceId(deviceConnectionId)
            .status(MetricStatus.ACTIVE)
            .build();
    }

    static List<MaternalHealthMetric> makeMixedSourceTrendSeries(UUID deviceConnectionId) {
        return List.of(
            makeExpectedMetric(),                          // MANUAL, 2026-07-01
            makeDeviceSourcedMetric(deviceConnectionId)     // DEVICE, 2026-07-02
        );
    }
}
```

---

### TREND-TC-001 — Happy path: mixed MANUAL/DEVICE trend, sorted ascending

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceTrendService.getTrend()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-69 Normal Flow` / `UC69 TDS §6.1`

**Preconditions:** `ImportDeviceMetricTestFactory.makeMixedSourceTrendSeries(deviceConnectionId)` returned by `MaternalHealthMetricRepository`; `IHealthDeviceConnectionRepository.findById()` returns `DeviceConnectionTestFactory.makeConnectedDevice()` with `deviceName="Mi Band 8"`

**Test Steps:**
1. Arrange: mock repo returns 2 metrics (1 MANUAL, 1 DEVICE) for `journeyId=JOURNEY_ID`, `metricType` matching
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: response `points` has 2 items sorted by `measuredAt` ascending; `hasAnyData=true`

**Expected Result (PASS):** Correct ordering and count.
**Expected Result (FAIL):** Wrong order, missing points, or exception.

**Current Status:** 🔴 Not written

---

### TREND-TC-002 — Empty state returns 200 with hasAnyData=false

**Severity:** `HIGH`
**Feature Under Test:** `DeviceTrendService.getTrend()` AF2
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC69 TDS §6.2 (AF2)`

**Preconditions:** Repo returns empty list for `FX-JOURNEY-EMPTY`

**Test Steps:**
1. Arrange: mock repo returns `List.of()`
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: `response.hasAnyData() == false`; `response.points()` is empty; no exception thrown

**Expected Result (PASS):** 200 with empty payload, not 404.
**Expected Result (FAIL):** Exception thrown, or 404 returned at controller layer.

**Current Status:** 🔴 Not written

---

### TREND-TC-003 — Invalid metricType rejected (400 DEVICE-301)

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceTrendController` request validation
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/DeviceTrendControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC69 TDS §10 Error Codes`

**Test Steps:**
1. Arrange: `@WebMvcTest`, request with `metricType=NOT_A_REAL_TYPE`
2. Act: `GET /api/v1/health/metrics/trend?...&metricType=NOT_A_REAL_TYPE`
3. Assert: HTTP 400, body `error.code == "DEVICE-301"`

**Expected Result (PASS):** 400 DEVICE-301.
**Expected Result (FAIL):** 500 error or silent pass-through.

**Current Status:** 🔴 Not written

---

### TREND-TC-004 — from > to rejected (400 DEVICE-301)

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceTrendService.getTrend()` validation
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC69 TDS §9.1 Interface Spec`

**Test Steps:**
1. Arrange: `query.from = 2026-07-10`, `query.to = 2026-07-01` (reversed range)
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: throws `DeviceTrendException` with code `DEVICE-301`; repository never queried

**Expected Result (PASS):** Rejected before any DB call.
**Expected Result (FAIL):** Query executes with reversed range, returns empty silently (masks a client bug).

**Current Status:** 🔴 Not written

---

### TREND-TC-005 — Non-owner query rejected (403 DEVICE-304)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `DeviceTrendService.getTrend()` ownership check
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC` / `UC69 TDS §16 Authorization Matrix`

**Preconditions:** `journeyId` owned by `OTHER_MOTHER_USER_ID`; caller is `MOTHER_USER_ID`

**Test Steps:**
1. Arrange: journey ownership policy returns `false` for `(journeyId, MOTHER_USER_ID)`
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-304` (403); `MaternalHealthMetricRepository` query method NEVER invoked

**Expected Result (PASS = safe):** 403 DEVICE-304, no data touched.
**Expected Result (FAIL = vulnerability):** Trend data of another user's journey returned — cross-user data leak.

**Current Status:** 🔴 Not written

---

### TREND-TC-006 — Non-existent journeyId rejected (404 DEVICE-302)

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceTrendService.getTrend()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC69 TDS §10 Error Codes`

**Test Steps:**
1. Arrange: journey lookup returns `Optional.empty()` for the given `journeyId`
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-302` (404)

**Expected Result (PASS):** 404 DEVICE-302.
**Expected Result (FAIL):** NullPointerException or wrong error code.

**Current Status:** 🔴 Not written

---

### TREND-TC-007 — sourceLabel resolves device name vs "Manual entry"

**Severity:** `HIGH`
**Feature Under Test:** `DeviceTrendService.resolveSourceLabel()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC69 TDS §9.2 Response Schema` / `§17 C5`

**Preconditions:** Mixed series from `ImportDeviceMetricTestFactory.makeMixedSourceTrendSeries(deviceConnectionId)`; `IHealthDeviceConnectionRepository.findById(deviceConnectionId)` returns device with `deviceName="Mi Band 8"`

**Test Steps:**
1. Arrange: as above
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: point with `sourceType=MANUAL` has `sourceLabel=="Manual entry"`; point with `sourceType=DEVICE` has `sourceLabel=="Mi Band 8"`

**Expected Result (PASS):** Labels resolved per rule.
**Expected Result (FAIL):** Wrong or missing label; device lookup not attempted.

**Current Status:** 🔴 Not written

---

### TREND-TC-008 — accuracyWarning is always false (ADR-DEVICE-008 still Proposed — anti-hallucination guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceTrendService.computeAccuracyWarning()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-DEVICE-008 (Proposed)` / `UC69 TDS §11.3 warning note` / CLAUDE.md ("AI provides guidance only")

**Preconditions:** Mixed series including a MANUAL metric (which Option A of ADR-DEVICE-008 would flag if Accepted) and a DEVICE metric

**Test Steps:**
1. Arrange: as `TREND-TC-001`
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: EVERY point in the response has `accuracyWarning == false`, regardless of `sourceType`

**Expected Result (PASS):** Stub behavior respected — no clinical/accuracy judgement is emitted while the ADR is unapproved.
**Expected Result (FAIL):** Any point has `accuracyWarning == true` — indicates an unapproved threshold was implemented (must be rejected per AP-AI-001, §8).

**Current Status:** 🔴 Not written

---

### TREND-TC-009 — Read-only: no write/delete calls on any repository

**Severity:** `HIGH`
**Feature Under Test:** `DeviceTrendService.getTrend()` side-effect boundary
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceTrendServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `UC69 TDS §4.2 Data Integrity`

**Test Steps:**
1. Arrange: happy path setup with Mockito spies/mocks on `MaternalHealthMetricRepository` and `IHealthDeviceConnectionRepository`
2. Act: `service.getTrend(query, MOTHER_USER_ID)`
3. Assert: `verify(metricRepository, never()).save(any())`; `verify(metricRepository, never()).delete(any())`; same for device repository

**Expected Result (PASS):** Zero write interactions.
**Expected Result (FAIL):** Any save/delete/update call detected.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### TREND-TC-INT-001 — Full trend query via Testcontainers with mixed sources

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: GET /trend → DB read across maternal_health_metrics + health_device_connections`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceTrendIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer; UC66/UC67 migrations applied (no new migration for UC69)
- Seed: 1 `health_device_connections` row (`status=ACTIVE`, `deviceName="Mi Band 8"`) + 2 `maternal_health_metrics` rows (1 MANUAL, 1 DEVICE referencing the connection) for `JOURNEY_ID`, plus 1 unrelated metric under `OTHER_JOURNEY_ID`

**Test Steps:**
1. Seed data as above
2. Call `GET /api/v1/health/metrics/trend?journeyId=JOURNEY_ID&metricType=HEART_RATE&from=..&to=..` as `MOTHER_USER_ID` (owner of `JOURNEY_ID`)
3. Assert HTTP response body directly

**Expected Result (PASS):**
- Response contains only the 2 points belonging to `JOURNEY_ID` (the `OTHER_JOURNEY_ID` metric is excluded)
- `sourceLabel` for the DEVICE point equals `"Mi Band 8"`
- All `accuracyWarning` fields are `false`

**Expected Result (FAIL):** Cross-journey leakage, wrong label, or `accuracyWarning=true` present.

**Current Status:** 🔴 Not written

---

### TREND-TC-SEC-001 — Cross-user journeyId query param manipulation blocked

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — IDOR via query parameter`
**Feature Under Test:** `GET /api/v1/health/metrics/trend` end-to-end auth enforcement
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceTrendIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC69 TDS §16 Authorization Matrix`

**Preconditions:** `OTHER_JOURNEY_ID` seeded with metrics, owned by a different Mother account; caller authenticates as `MOTHER_USER_ID`

**Test Steps:**
1. Seed `OTHER_JOURNEY_ID` with metric data not owned by caller
2. Call `GET /api/v1/health/metrics/trend?journeyId=OTHER_JOURNEY_ID&metricType=HEART_RATE&...` with `MOTHER_USER_ID`'s JWT
3. Assert HTTP 403, body `error.code == "DEVICE-304"`, and response contains no metric data

**Expected Result (PASS = safe):** 403, no data returned.
**Expected Result (FAIL = vulnerability):** 200 with another user's health data.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `TREND-TC-001` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-002` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-003` | `DeviceTrendControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-004` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-005` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-006` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-007` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-008` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-009` | `DeviceTrendServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-INT-001` | `DeviceTrendIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `TREND-TC-SEC-001` | `DeviceTrendIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// DeviceTrendService stub (Red Phase)
@Override
public DeviceTrendResponse getTrend(DeviceTrendQuery query, UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `TREND-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `TREND-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS**
- Log file: chưa tạo (Draft phase)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-DEVICE-IMP-004` đã review và approve
- [ ] UC66 và UC67 đã implement và deploy (dependencies: `health_device_connections`, `maternal_health_metrics`)
- [ ] **BLOCKER:** `ADR-DEVICE-008` vẫn `Proposed` — implementation PHẢI dùng stub `accuracyWarning=false` cho đến khi được Accepted chính thức (xem `TREND-TC-008`)

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] `./mvnw verify` (Testcontainers) xanh, đặc biệt `TREND-TC-INT-001` (cross-journey isolation) và `TREND-TC-SEC-001` (IDOR)
- [ ] Coverage ≥ 80% cho `DeviceTrendService`
- [ ] `TREND-TC-008` xanh với `accuracyWarning` luôn `false` — KHÔNG được merge nếu có logic ngưỡng tự phát minh
- [ ] Không business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate pass
- [ ] Contract Existence verified
- [ ] Props Isolation — dùng `ImportDeviceMetricTestFactory` (UC67) + `DeviceConnectionTestFactory` (UC66), không tạo factory trùng lặp
- [ ] Oracle Source ghi rõ cho mọi assert

### Suspension Criteria

- UC66 hoặc UC67 chưa deploy (`health_device_connections` hoặc `maternal_health_metrics` không sẵn sàng)
- `ADR-DEVICE-008` bị thay đổi quyết định mà chưa cập nhật lại `TREND-TC-008`/`TREND-TC-INT-001`

---

## 7. Rollback Plan

```bash
# Không có migration để revert (UC69 không tạo migration mới — chỉ đọc dữ liệu có sẵn từ UC66/UC67).
git checkout -- src/main/java/com/carebridge/backend/health/device/service/DeviceTrendService.java
git checkout -- src/main/java/com/carebridge/backend/health/device/controller/DeviceTrendController.java
git checkout -- src/test/java/com/carebridge/backend/health/device/
kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | Code implement `accuracyWarning` với ngưỡng lâm sàng/AI tự bịa thay vì stub `false` | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub `throw` | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code trả 404 khi không có dữ liệu (vi phạm AF2/C4) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller chứa logic tổng hợp/aggregation thay vì Service | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import type/field không tồn tại trong §8 Interface Spec | ☐ | G-3 |

**Kết quả review CASE 2.0 (đặc thù UC69):** `ADR-DEVICE-008` là **Proposed**, chưa `Accepted`. `TREND-TC-008` là gate bắt buộc — bất kỳ implementation nào set `accuracyWarning=true` dựa trên logic tự phát minh phải bị reject theo AP-AI-001 cho đến khi ADR được duyệt chính thức.

- [ ] Không phát hiện anti-pattern → approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
