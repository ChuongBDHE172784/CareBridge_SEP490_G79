# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC67 — Import Device Data Manually

**Document ID:** `CB-DEVICE-IMP-002-TEST`
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
- `04_Implement/UC67_ImportDeviceDataManually/UC67_ImportDeviceDataManually_TDS.md` (CB-DEVICE-IMP-002)
- `04_Implement/UC66_ConnectHealthDevice/UC66_ConnectHealthDevice_TDS.md` (upstream dependency)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.44`

> **Quy ước TDD:** Viết test trước → chạy → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC67 (Draft) |
| 2026-07-02 | AI Agent — Test Designer (reconciliation) | **Corrected schema reference:** `device_connections` (invented, did not exist) → `health_device_connections` (real, `V1__init_schema.sql` L1115) — reconciled with UC130's independently-verified schema research. Device fixture reuse updated: `DeviceConnectionTestFactory.makeConnectedDevice()`/`makeDisconnectedDevice()` (from UC66, corrected) now return `HealthDeviceConnection` with `status ACTIVE/REVOKED`. Error condition DEVICE-102 now triggers on non-ACTIVE status. |

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
| **Feature / Gap ID** | `CB-DEVICE-IMP-002` |
| **Module** | `Import Device Data Manually — health.device` |
| **Spec gốc** | `CB-DEVICE-IMP-002` (TDS) |
| **Priority** | 🟠 P1 (High per SRS) |
| **Sprint** | `Device Sync And Care Edge Cases (TV2-Bách)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC66 health_device_connections` |
| **Downstream Consumers** | `UC69 ViewDeviceDataTrend` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-DEVICE-IMP-002 §17`, `ADR-DEVICE-004/005/006` |
| **Constraints Injected** | C1 (reuse MaternalHealthMetric), C2 (sourceReferenceId validation), C3 (heuristic-only validation), C4 (journey ownership), C5 (DeviceDataImported event) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | `MetricType` entity hiện tại thiếu `SLEEP_DURATION`, `STEPS_COUNT`, `SPO2` | UC-67 SRS yêu cầu heart rate, sleep, steps, SpO2, blood pressure | Test verify các metricType mới được chấp nhận và lưu đúng |
| L2 | `MaternalHealthMetric.java` không map cột `source_reference_id` (đã tồn tại trong schema V1 dòng 582) | Cần map để lưu provenance | Test verify entity mapping + persisted value sau khi mở rộng |
| L3 | SRS không định nghĩa sanity range cụ thể cho từng metricType | ADR-DEVICE-006 đề xuất range tạm thời (Open Item O1 trong TDS) | Test dùng range đề xuất trong TDS §8.1 làm oracle tạm thời — ghi rõ "Proposed" trong Oracle Source, KHÔNG coi là final |
| L4 | Không rõ liệu `sourceType=DEVICE` có bắt buộc kiểm tra `health_device_connections.status=ACTIVE` hay không (SRS không đặc tả chi tiết) | ADR-DEVICE-005 quyết định: bắt buộc kiểm tra | Test verify import với device REVOKED bị reject (DEVICE-102) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Import Device Data Manually (health.device) bao gồm các layer:
├── Domain (MaternalHealthMetric extended — pure logic)
├── Service (DeviceDataImportService — mock MaternalHealthMetricRepository + IDeviceConnectionRepository + EventPublisher)
├── Controller (@WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL — verify FK linkage to health_device_connections)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-67 §3.3.1.44` | Manual/mock import of heart rate, sleep, steps, SpO2, blood pressure |
| `ADR-DEVICE-004` | Reuse MaternalHealthMetric — extend MetricType |
| `ADR-DEVICE-005` | sourceReferenceId → health_device_connections FK, must be ACTIVE + owned by caller |
| `ADR-DEVICE-006` | Heuristic sanity validation, non-diagnostic |
| `BR-RBAC` | Journey ownership required |
| `V1__init_schema.sql` + `V20260701140100` (new) | FK constraint, index additions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path manual import (SPO2, valid range) → 201 | `importMetric()` | `IMPORT-TC-001` |
| TC-COND-002 | Happy path device-tagged import (HEART_RATE, valid device) → 201, sourceReferenceId set | `importMetric()` | `IMPORT-TC-002` |
| TC-COND-003 | Value out of sanity range → 400 DEVICE-101 | Validation | `IMPORT-TC-003` |
| TC-COND-004 | sourceType=DEVICE with disconnected device → 409 DEVICE-102 | `importMetric()` | `IMPORT-TC-004` |
| TC-COND-005 | sourceType=DEVICE with device owned by another user → 403/409 | `importMetric()` ownership check | `IMPORT-TC-005` |
| TC-COND-006 | journeyId not owned by caller → 403 DEVICE-103 | Ownership | `IMPORT-TC-006` |
| TC-COND-007 | Missing required field (metricType) → 400 DEVICE-100 | Validation | `IMPORT-TC-007` |
| TC-COND-008 | All new MetricType values (SLEEP_DURATION, STEPS_COUNT, SPO2) accepted | Enum coverage | `IMPORT-TC-008` |
| TC-COND-009 | `DeviceDataImported` event published with correct payload | Event | `IMPORT-TC-009` |
| TC-COND-010 (Boundary) | Exact boundary values (min/max inclusive) accepted; min-1/max+1 rejected | Boundary Value Analysis | `IMPORT-TC-010` |
| TC-COND-011 (Integration) | Persisted row with correct FK to health_device_connections | End-to-end | `IMPORT-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | `sourceType` (MANUAL/DEVICE/invalid) | Cover both valid provenance paths |
| Boundary Value Analysis | `valueNumeric` per metricType range (§8.1 TDS) | Sanity check correctness at edges |
| State-based Testing | Device connection status (ACTIVE vs REVOKED) referenced by import | Cross-feature consistency with UC66/UC68 |
| Error Guessing | Cross-user device reference, non-owned journey | Ownership bypass attempts |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-JOURNEY-001` | DB seed | `{ journeyId: UUID, ownerUserId: MOTHER_USER_ID }` | Happy path ownership |
| `FX-JOURNEY-OTHER-001` | DB seed | `{ journeyId: UUID, ownerUserId: OTHER_MOTHER_USER_ID }` | Ownership negative test |
| `FX-DEVICE-CONN-CONNECTED` | DB seed | Reuses `DeviceConnectionTestFactory.makeConnectedDevice()` (UC66 shared factory) | Device-tagged import |
| `FX-DEVICE-CONN-DISCONNECTED` | DB seed | Reuses `DeviceConnectionTestFactory.makeDisconnectedDevice()` (UC66 shared factory) | DEVICE-102 negative test |
| `FX-METRIC-SPO2-VALID` | Request body | `{ metricType: 'SPO2', valueNumeric: 97, unit: '%' }` | Happy path |
| `FX-METRIC-HR-OUTOFRANGE` | Request body | `{ metricType: 'HEART_RATE', valueNumeric: 9999, unit: 'bpm' }` | DEVICE-101 negative test |
| `FX-METRIC-HR-BOUNDARY` | Request body | `{ metricType: 'HEART_RATE', valueNumeric: 30 }` / `{ valueNumeric: 250 }` / `{ valueNumeric: 29 }` / `{ valueNumeric: 251 }` | Boundary Value Analysis |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership bypass) |
| Mobile (Flutter) | ✅ (service/repo layer) | — | ✅ | ✅ (widget test — manual entry form) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// ImportDeviceMetricTestFactory.java
// Đặt tại: src/test/java/com/carebridge/backend/health/device/ImportDeviceMetricTestFactory.java
// Tái sử dụng DeviceConnectionTestFactory (UC66) cho device fixtures.
// ═══════════════════════════════════════════════════════════

class ImportDeviceMetricTestFactory {

    static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID OTHER_JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A2");

    static ImportDeviceMetricRequest makeManualRequest() {
        return makeManualRequest(r -> {});
    }

    static ImportDeviceMetricRequest makeManualRequest(Consumer<ImportDeviceMetricRequest> overrides) {
        ImportDeviceMetricRequest request = new ImportDeviceMetricRequest();
        request.setJourneyId(JOURNEY_ID);
        request.setMetricType(MetricType.SPO2);
        request.setValueNumeric(new BigDecimal("97"));
        request.setUnit("%");
        request.setMeasuredAt(Instant.parse("2026-07-01T07:30:00Z"));
        request.setSourceType(DataSource.MANUAL);
        overrides.accept(request);
        return request;
    }

    static ImportDeviceMetricRequest makeDeviceTaggedRequest(UUID deviceConnectionId) {
        return makeManualRequest(r -> {
            r.setMetricType(MetricType.HEART_RATE);
            r.setValueNumeric(new BigDecimal("72"));
            r.setUnit("bpm");
            r.setSourceType(DataSource.DEVICE);
            r.setDeviceConnectionId(deviceConnectionId);
        });
    }

    static MaternalHealthMetric makeExpectedMetric() {
        return MaternalHealthMetric.builder()
            .id(UUID.randomUUID())
            .journeyId(JOURNEY_ID)
            .metricType(MetricType.SPO2)
            .valueNumeric(new BigDecimal("97"))
            .unit("%")
            .measuredAt(Instant.parse("2026-07-01T07:30:00Z"))
            .sourceType(DataSource.MANUAL)
            .sourceReferenceId(null)
            .status(MetricStatus.ACTIVE)
            .build();
    }
}
```

---

### IMPORT-TC-001 — Manual import happy path (SPO2)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceDataImportService.importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-67 Normal Flow` / `ADR-DEVICE-004`

**Test Steps:**
1. Arrange: `makeManualRequest()`; mock journey ownership check passes
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: saved entity `sourceType=MANUAL`, `sourceReferenceId=null`; response 201 equivalent DTO

**Expected Result (PASS):** Metric persisted with MANUAL provenance, no device reference.
**Expected Result (FAIL):** Exception, or sourceReferenceId incorrectly populated.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-002 — Device-tagged import happy path (HEART_RATE)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceDataImportService.importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-DEVICE-005`

**Preconditions:** `DeviceConnectionTestFactory.makeConnectedDevice()` owned by `MOTHER_USER_ID` mocked as found by repo

**Test Steps:**
1. Arrange: `makeDeviceTaggedRequest(connectedDeviceId)`
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: saved `sourceType=DEVICE`, `sourceReferenceId=connectedDeviceId`

**Expected Result (PASS):** Metric linked correctly to active device connection.
**Expected Result (FAIL):** Link missing or wrong sourceReferenceId.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-003 — Value out of sanity range rejected (DEVICE-101)

**Severity:** `HIGH`
**Feature Under Test:** `ImportDeviceMetricRequest` validation
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/HealthMetricDeviceImportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-DEVICE-006 §Sanity Range Table (Proposed)` — **Open Item O1, pending clinical review**

**Test Steps:**
1. Arrange: `makeManualRequest(r -> { r.setMetricType(HEART_RATE); r.setValueNumeric(9999); })`
2. Act: `POST /api/v1/health/metrics/device-import`
3. Assert: HTTP 400, `error.code == "DEVICE-101"`

**Expected Result (PASS):** 400 + `DEVICE-101`, message neutral (no "abnormal"/"dangerous" wording per ADR-DEVICE-006 C3).
**Expected Result (FAIL):** Value accepted, or message uses diagnostic language.

**Current Status:** 🔴 Not written
**Implementation Note:** Assert message text does NOT contain diagnostic terms — this enforces C3.

---

### IMPORT-TC-004 — Device-tagged import with disconnected device rejected (DEVICE-102)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceDataImportService.importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-DEVICE-005`

**Preconditions:** `DeviceConnectionTestFactory.makeDisconnectedDevice()` mocked

**Test Steps:**
1. Arrange: `makeDeviceTaggedRequest(disconnectedDeviceId)`
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-102`; repository.save() never called

**Expected Result (PASS):** Rejected, no side effects.
**Expected Result (FAIL):** Metric saved despite disconnected device reference — cross-feature (UC66/UC68) consistency violation.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-005 — Device owned by another user rejected

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `DeviceDataImportService.importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC`

**Preconditions:** Connected device belongs to `OTHER_MOTHER_USER_ID`, request made by `MOTHER_USER_ID`

**Test Steps:**
1. Arrange: `makeDeviceTaggedRequest(otherUsersDeviceId)`
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: throws exception (403/409); no metric saved

**Expected Result (PASS = safe):** Rejected — cannot tag import to another user's device.
**Expected Result (FAIL = vulnerability):** Metric saved with cross-user device reference.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-006 — Non-owned journeyId rejected (DEVICE-103)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceDataImportService.importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: `makeManualRequest(r -> r.setJourneyId(OTHER_JOURNEY_ID))`, `OTHER_JOURNEY_ID` owned by different user
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-103` (403)

**Expected Result (PASS):** Rejected.
**Expected Result (FAIL):** Metric saved for a journey not owned by caller.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-007 — Missing metricType rejected (DEVICE-100)

**Severity:** `MEDIUM`
**Feature Under Test:** `ImportDeviceMetricRequest` validation
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/HealthMetricDeviceImportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS §10 Error Codes

**Test Steps:**
1. Arrange: JSON body missing `metricType`
2. Act: `POST /api/v1/health/metrics/device-import`
3. Assert: HTTP 400, `error.code == "DEVICE-100"`

**Expected Result (PASS):** 400 + `DEVICE-100`.
**Expected Result (FAIL):** Accepted with null metricType, or wrong error code.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-008 — New MetricType values accepted (SLEEP_DURATION, STEPS_COUNT, SPO2)

**Severity:** `HIGH`
**Feature Under Test:** `MetricType` enum + `importMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `SRS UC-67 Description` (heart rate, sleep, steps, SpO2, blood pressure) / `ADR-DEVICE-004`

**Test Steps (parameterized, one per metricType):**
1. Arrange: `makeManualRequest(r -> r.setMetricType(SLEEP_DURATION))` (and STEPS_COUNT, SPO2 variants within valid range)
2. Act: `service.importMetric(request, MOTHER_USER_ID)`
3. Assert: 201-equivalent success for each; persisted `metricType` matches

**Expected Result (PASS):** All three new enum values accepted end-to-end.
**Expected Result (FAIL):** `IllegalArgumentException` on enum deserialization, or metric rejected.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-009 — DeviceDataImported event published

**Severity:** `HIGH`
**Feature Under Test:** `DeviceDataImportService.importMetric()` → `ApplicationEventPublisher`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §7.1 Domain Event Catalog`

**Test Steps:**
1. Arrange: happy path manual import
2. Act: `service.importMetric(makeManualRequest(), MOTHER_USER_ID)`
3. Assert: `eventPublisher.publishEvent(any(DeviceDataImported.class))` called once; payload `metricType == "SPO2"`, `sourceType == "MANUAL"`

**Expected Result (PASS):** Event published with correct payload.
**Expected Result (FAIL):** Event missing or payload incorrect.

**Current Status:** 🔴 Not written

---

### IMPORT-TC-010 — Boundary values for HEART_RATE (30/250 valid, 29/251 invalid)

**Severity:** `MEDIUM`
**Feature Under Test:** Boundary validation for `valueNumeric`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceDataImportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-DEVICE-006 §Sanity Range Table (Proposed, Open Item O1)`

**Test Steps (parameterized):**
1. `valueNumeric=30` → accepted (min inclusive)
2. `valueNumeric=250` → accepted (max inclusive)
3. `valueNumeric=29` → rejected `DEVICE-101`
4. `valueNumeric=251` → rejected `DEVICE-101`

**Expected Result (PASS):** Boundaries match table in TDS §8.1 exactly.
**Expected Result (FAIL):** Off-by-one error at either boundary.

**Current Status:** 🔴 Not written
**Implementation Note:** If Open Item O1 range is later revised by Tech Lead, this TC's oracle values MUST be updated together with TDS §8.1 (single source of truth).

---

### INTEGRATION TEST CASES

---

### IMPORT-TC-INT-001 — Persisted metric with correct FK to health_device_connections

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /device-import → DB row + FK`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceDataImportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL Testcontainer running; baseline `V1__init_schema.sql` applied (health_device_connections pre-exists) + migration `V20260701140100` applied
- Seed: active (`status=ACTIVE`) `health_device_connections` row for `MOTHER_USER_ID`

**Test Steps:**
1. Seed connected device + journey
2. Call `POST /api/v1/health/metrics/device-import` with `sourceType=DEVICE`
3. Assert DB state

**Expected Result (PASS):**
- `maternal_health_metrics` row exists with `source_reference_id` = seeded `health_device_connections.connection_id`
- FK constraint satisfied (no orphan reference)

**Expected Result (FAIL):** Row missing or FK violation error.

**DB Assertion:**
```java
MaternalHealthMetric record = metricRepository.findById(savedId).orElseThrow();
assertThat(record.getSourceReferenceId()).isEqualTo(seededDeviceConnectionId);
assertThat(record.getSourceType()).isEqualTo(DataSource.DEVICE);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `IMPORT-TC-001` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-002` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-003` | `HealthMetricDeviceImportControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-004` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-005` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-006` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-007` | `HealthMetricDeviceImportControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-008` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-009` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-010` | `DeviceDataImportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `IMPORT-TC-INT-001` | `DeviceDataImportIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class DeviceDataImportService implements IDeviceDataImportService {

    @Override
    public MetricDetailResponse importMetric(ImportDeviceMetricRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `IMPORT-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-003` | N/A (validation layer) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-007` | N/A (validation layer) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `IMPORT-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS**
- Log file: chưa tạo (Draft phase)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-DEVICE-IMP-002` đã review và approve
- [ ] UC66 TDS đã approve (dependency: `health_device_connections` table — đã tồn tại sẵn từ `V1__init_schema.sql`, không cần migration)
- [ ] Sanity range table (Open Item O1) đã được xác nhận hoặc explicitly accepted as Proposed
- [ ] Migration `V20260701140100` approved và chạy thành công

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] `./mvnw verify` (Testcontainers) xanh
- [ ] Coverage ≥ 80% cho `DeviceDataImportService`
- [ ] Không business logic trong Controller
- [ ] Validation message không dùng ngôn ngữ chẩn đoán (C3 enforced)
- [ ] Cross-user device reference bị chặn (IMPORT-TC-005)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate pass
- [ ] Contract Existence verified
- [ ] Props Isolation — dùng `ImportDeviceMetricTestFactory` + `DeviceConnectionTestFactory` (UC66)
- [ ] Oracle Source ghi rõ cho mọi assert, đặc biệt đánh dấu "Proposed" cho sanity range values

### Suspension Criteria

- Sanity range Open Item chưa resolve
- UC66 migration chưa deploy trên staging

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "ALTER TABLE maternal_health_metrics DROP CONSTRAINT IF EXISTS fk_mhm_source_device_connection;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260701000002';"

git checkout -- src/main/java/com/carebridge/backend/health/
git checkout -- src/main/resources/db/migration/V20260701140100__extend_metric_type_and_source.sql
git checkout -- src/test/java/com/carebridge/backend/health/device/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC tạo bảng mới thay vì mở rộng entity hiện có | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định ngưỡng y khoa "chính thức" không có nguồn | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern → approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
