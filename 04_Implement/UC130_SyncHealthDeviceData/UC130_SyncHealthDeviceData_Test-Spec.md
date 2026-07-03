# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC130 — Sync Health Device Data

**Document ID:** `CB-DEVICE-SYNC-001-TEST`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect / Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (dòng 1115-1142, 1927-1931 — `health_device_connections` / `device_measurements`)
- `04_Implement/UC130_SyncHealthDeviceData/UC130_SyncHealthDeviceData_TDS.md` (CB-DEVICE-SYNC-001)
- `04_Implement/UC68_DisconnectHealthDevice/UC68_DisconnectHealthDevice_Test-Spec.md` (style/structure reference — cùng domain `health.device`)
- `02_Requirements/SRS/3_Functional_Specification.md §3.1.2.4` (dòng 534-553)

> Viết test trước → chạy → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ. Mọi thay đổi phải ghi vào bảng này.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC130 (Draft) |
| 2026-07-02 | AI Agent — Technical Architect (reconciliation, later same day) | **Cross-TDS reconciliation update:** ADR-SYNC-001 blocker RESOLVED — UC66/67/68/69 TDS + Test-Spec corrected to use real `health_device_connections`/`device_measurements` schema, matching UC130's design. Suspension Criteria below updated accordingly. No functional test-case changes required for UC130 itself. |

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
| **Feature / Gap ID** | `CB-DEVICE-SYNC-001` |
| **Module** | `Sync Health Device Data — health.device` |
| **Spec gốc** | `CB-DEVICE-SYNC-001` (TDS) |
| **Priority** | 🔴 P0 *(auto-sync xử lý Sensitive-PII, consent re-check mỗi lần)* |
| **Sprint** | `Device Sync And Care Edge Cases (TV2-Bách)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT)`, `consent` module (`consent_granted_at` trên `health_device_connections`), `integration.wearable.WearableProviderClient` (mock-first) |
| **Downstream Consumers** | Health metric trend read views (tương lai — tương đương UC69, đọc `device_measurements`), Audit log sink |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-DEVICE-SYNC-001 §17`, `ADR-SYNC-001..005` |
| **Constraints Injected** | C1 (real schema `health_device_connections`/`device_measurements`, KHÔNG dùng `device_connections` của UC66), C2 (consent+status re-checked mỗi lần), C3 (provider chỉ qua `WearableProviderClient`/mock), C4 (skip-and-continue, không rollback toàn batch), C5 (idempotency theo `connection_id`+`source_record_id`), C6 (`token_reference` không lộ), C7 (event publish sau mỗi lần sync) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC66/67/68/69 TDS (Draft) tự đề xuất bảng mới `device_connections`; nếu test UC130 tái sử dụng entity đó, test sẽ target sai bảng | `V1__init_schema.sql` dòng 1115-1142 xác nhận bảng THỰC là `health_device_connections`/`device_measurements` (ADR-SYNC-001, Proposed nhưng là quyết định TDS này) | Toàn bộ test dùng entity `HealthDeviceConnection`/`DeviceMeasurement`, repository `IHealthDeviceConnectionRepository`/`IDeviceMeasurementRepository` — KHÔNG import bất kỳ type nào từ `DeviceConnectionTestFactory` (UC66/68) vì nó target bảng khác |
| L2 | SRS UC-130 flows là template chung, không đặc tả cơ chế trigger (manual vs scheduled vs cả hai) | ADR-SYNC-002 quyết định: cả 2 — manual `POST .../sync` VÀ `DeviceSyncScheduler` nền, dùng chung `syncNow()` | Test cover cả `syncNow()` (caller = user JWT) và `syncAllActiveConnections()` (caller = internal service identity) như 2 coverage item riêng |
| L3 | Không rõ 1 record lỗi trong batch có làm fail toàn bộ sync hay không | ADR-SYNC-005: skip-and-continue — record lỗi bị skip, batch còn lại vẫn lưu; response trả `syncedCount`/`skippedCount`/`skippedReasons` | Test verify partial-success response, không throw exception khi có record invalid xen giữa các record hợp lệ |
| L4 | `device_measurements.source_record_id` là kiểu `uuid` trong schema thực, không phải `varchar` như một số provider trả về | TDS §5.2 Open Item O3: map bằng `UUID.nameUUIDFromBytes()` để derive UUID ổn định từ provider ID gốc | Test factory tạo `sourceRecordId` là UUID hợp lệ; 1 test case riêng verify cùng input string → cùng UUID derive (deterministic) cho mục đích idempotency |
| L5 | Không có UNIQUE constraint DB composite `(connection_id, source_record_id)` — idempotency chỉ ở service layer (Open Item O4) | `existsByConnectionIdAndSourceRecordId()` PHẢI được gọi trước mỗi insert | Test verify service gọi `existsBy...()` trước `save()`; integration test verify sync lặp lại không tạo duplicate dù không có DB constraint |
| L6 | `last_synced_at` cập nhật khi nào không rõ nếu có lỗi giữa chừng | §6.4 Invariant #3: chỉ cập nhật SAU KHI transaction lưu measurement thành công; lỗi provider trước khi nhận dữ liệu → KHÔNG cập nhật | Test verify `lastSyncedAt` không đổi khi `ProviderUnavailableException` xảy ra trước khi có bất kỳ record nào |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Sync Health Device Data (health.device) bao gồm các layer:
├── Service (DeviceSyncService — mock IHealthDeviceConnectionRepository + IDeviceMeasurementRepository
│             + WearableProviderClient + DeviceMeasurementValidator + ApplicationEventPublisher)
├── Validator (DeviceMeasurementValidator — pure logic, sanity range, không mock)
├── Controller (@WebMvcTest DeviceSyncController, mock IDeviceSyncService)
├── Scheduler (DeviceSyncScheduler — mock IDeviceSyncService, verify iteration + per-connection isolation)
└── Integration (Testcontainers PostgreSQL — verify persisted rows, idempotency, transactional invariants)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-130 §3.1.2.4` | Đồng bộ dữ liệu thiết bị tự động/nền |
| `ADR-SYNC-001` | Dùng bảng thực `health_device_connections`/`device_measurements` |
| `ADR-SYNC-002` | Cả manual "Sync Now" và scheduled job dùng chung `syncNow()` |
| `ADR-SYNC-003` | Provider access chỉ qua `WearableProviderClient`; `MockWearableProviderClient` duy nhất |
| `ADR-SYNC-004` | Consent + status re-verify mỗi lần sync, không cache |
| `ADR-SYNC-005` | Skip-and-continue cho record lỗi, không rollback toàn batch |
| `TDS §6.4 Invariants #1-4` | Sync chỉ chạy khi ACTIVE+consent; idempotency; `last_synced_at` update timing; batch vs provider-error rollback |
| `TDS §10 Error Codes` | SYNC-001..006 |
| `TDS §16 Authorization Matrix` | Ownership-scoped `ROLE_MOTHER`; admin override; scheduler = internal identity |
| `BR-RBAC` / `BR-PRIVACY` / `BR-SAFETY` | Ownership, consent timeliness, non-diagnostic skip reasons |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path manual sync: pulls new measurements, saves, updates `lastSyncedAt`, publishes event | `syncNow()` | `SYNC-TC-001` |
| TC-COND-002 | Connection not ACTIVE → reject `SYNC-002`, no provider call | `syncNow()` | `SYNC-TC-002` |
| TC-COND-003 | Connection ACTIVE but `consentGrantedAt` null/revoked → reject `SYNC-002` | `syncNow()` (ADR-SYNC-004) | `SYNC-TC-003` |
| TC-COND-004 | Caller does not own connection → 403 `SYNC-004` | `syncNow()` ownership | `SYNC-TC-004` |
| TC-COND-005 | Connection id not found → 404 `SYNC-003` | `syncNow()` | `SYNC-TC-005` |
| TC-COND-006 | Out-of-sanity-range measurement skipped, valid ones still saved (skip-and-continue) | `syncNow()` + `DeviceMeasurementValidator` (ADR-SYNC-005) | `SYNC-TC-006` |
| TC-COND-007 | Duplicate `sourceRecordId` for same connection skipped, not re-inserted | `syncNow()` idempotency (C5) | `SYNC-TC-007` |
| TC-COND-008 | Provider throws `ProviderUnavailableException` before any data received → 503 `SYNC-005`, retryable=true, `lastSyncedAt` unchanged | `syncNow()` error path (Invariant #3/#4) | `SYNC-TC-008` |
| TC-COND-009 | Sync via `sourceType`/mechanism distinct from UC67 manual import — verify sync-created rows are NOT tagged with UC67's manual-entry source enum | `syncNow()` vs UC67 semantics | `SYNC-TC-009` |
| TC-COND-010 | `DeviceDataSynced` event published exactly once per successful `syncNow()` call, including `syncedCount=0` case | `syncNow()` → `ApplicationEventPublisher` (C7) | `SYNC-TC-010` |
| TC-COND-011 | `DeviceSyncFailed` event published on provider failure (not `DeviceDataSynced`) | `syncNow()` error path (§7.1) | `SYNC-TC-011` |
| TC-COND-012 | `syncAllActiveConnections()` iterates all ACTIVE connections; per-connection failure does not abort remaining batch | `DeviceSyncScheduler` / `syncAllActiveConnections()` (ADR-SYNC-002 + §6.2 note) | `SYNC-TC-012` |
| TC-COND-013 | `token_reference` never appears in `DeviceSyncResultResponse` or event payload | Security (C6 / §4.3) | `SYNC-TC-013` |
| TC-COND-014 | Retry after transient provider failure succeeds without duplicating already-synced records | Retry/idempotency combined | `SYNC-TC-014` |
| TC-COND-015 (Integration) | Full manual sync flow via Testcontainers — persisted `device_measurements` rows + `last_synced_at` update | End-to-end | `SYNC-TC-INT-001` |
| TC-COND-016 (Integration) | Idempotency — sync same connection twice with same mock dataset → no duplicate rows | End-to-end | `SYNC-TC-INT-002` |
| TC-COND-017 (Security/E2E) | `ROLE_PARTNER` attempts manual sync trigger → 403 | Controller/E2E | `SYNC-TC-E2E-001` |
| TC-COND-018 (Mobile) | Mobile `DeviceSyncService` (Dart) calls sync endpoint and parses `DeviceSyncResultResponse` correctly | Mobile service layer | `SYNC-TC-MOB-001` |
| TC-COND-019 (Mobile Widget) | "Sync Now" button widget shows loading → success summary (`syncedCount`/`skippedCount`) or error state | Mobile widget | `SYNC-TC-MOB-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | Valid vs invalid `measurementType`/`valueNumeric` ranges | Core sanity validation coverage (ADR-SYNC-005) |
| Boundary Value Analysis | Sanity range min/max per `measurementType` (§8.1 table) | Off-by-one risk at range edges |
| State Transition Testing | `status ∈ {ACTIVE, INACTIVE, REVOKED}` gating sync eligibility | Core invariant (ADR-SYNC-004) |
| Error Guessing | Provider timeout mid-batch, consent revoked between connect and sync, duplicate `sourceRecordId` | Race/edge conditions typical of async sync |
| State Transition (batch) | Skip-and-continue vs full-batch-rollback distinction | Distinguishes provider-level failure (rollback) from record-level failure (skip) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-SYNC-CONN-ACTIVE` | DB seed | `HealthDeviceSyncTestFactory.makeActiveConnection()` — `status=ACTIVE`, `consentGrantedAt=now().minus(1h)` | Happy path |
| `FX-SYNC-CONN-INACTIVE` | DB seed | `makeActiveConnection(c -> c.setStatus(INACTIVE))` | TC-COND-002 |
| `FX-SYNC-CONN-NO-CONSENT` | DB seed | `makeActiveConnection(c -> c.setConsentGrantedAt(null))` | TC-COND-003 |
| `FX-SYNC-CONN-OTHER-USER` | DB seed | `makeActiveConnection(c -> c.setUserId(OTHER_MOTHER_USER_ID))` | TC-COND-004 |
| `FX-SYNC-RAW-VALID` | In-memory DTO | `HealthDeviceSyncTestFactory.makeRawMeasurement()` — `HEART_RATE=72bpm` (within range) | Happy path |
| `FX-SYNC-RAW-OUT-OF-RANGE` | In-memory DTO | `makeRawMeasurement(r -> r.setValueNumeric(300))` — HEART_RATE > 250 max | TC-COND-006 |
| `FX-SYNC-RAW-DUPLICATE` | In-memory DTO | `makeRawMeasurement(r -> r.setSourceRecordId(EXISTING_SOURCE_RECORD_ID))` | TC-COND-007 |
| `FX-SYNC-PROVIDER-UNAVAILABLE` | Mock behavior | `providerClient.fetchNewMeasurements(...)` throws `ProviderUnavailableException` | TC-COND-008, TC-COND-011 |
| `MOTHER_USER_ID` / `OTHER_MOTHER_USER_ID` / `PARTNER_USER_ID` | Constants | `HealthDeviceSyncTestFactory` static UUIDs | RBAC/ownership tests |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership + `token_reference` leak) |
| Mobile (Flutter) | ✅ (service layer, `flutter_test`) | — | — | ✅ (`flutter_test` widget — "Sync Now" button states) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> **Naming decision (Open item — flagged, not silently resolved):** UC68's Test-Spec reuses `DeviceConnectionTestFactory`, which targets UC66's self-proposed `device_connections` table/entity. Per this TDS's ADR-SYNC-001, UC130 targets the DIFFERENT, actually-existing schema (`health_device_connections`/`device_measurements`). Reusing `DeviceConnectionTestFactory` as-is would silently mix two incompatible entity models. This Test-Spec therefore introduces a **new**, differently-named factory — `HealthDeviceSyncTestFactory` — following the exact same `makeXxx()` / `makeXxx(Consumer<T> overrides)` extension pattern established by `DeviceConnectionTestFactory`, but scoped to the real `HealthDeviceConnection`/`DeviceMeasurement` entities. See Open Item flagged in the final report — this divergence must be reconciled by Tech Lead alongside ADR-SYNC-001/Open-Item-O1.

```java
// ═══════════════════════════════════════════════════════════
// HealthDeviceSyncTestFactory.java — NEW factory for UC130
// src/test/java/com/carebridge/backend/health/device/HealthDeviceSyncTestFactory.java
// Targets REAL schema entities (HealthDeviceConnection/DeviceMeasurement) per ADR-SYNC-001.
// Follows the same makeXxx()/makeXxx(overrides) pattern as DeviceConnectionTestFactory (UC66/68),
// but is intentionally a SEPARATE class — do not merge until Open Item O1 (schema conflict) resolved.
// ═══════════════════════════════════════════════════════════

class HealthDeviceSyncTestFactory {

    static final UUID MOTHER_USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID OTHER_MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    static final UUID PARTNER_USER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000103");
    static final UUID EXISTING_SOURCE_RECORD_ID =
        UUID.nameUUIDFromBytes("provider-record-existing-001".getBytes());

    // Baseline: ACTIVE connection with valid consent, owned by MOTHER_USER_ID
    static HealthDeviceConnection makeActiveConnection() {
        HealthDeviceConnection conn = new HealthDeviceConnection();
        conn.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        conn.setUserId(MOTHER_USER_ID);
        conn.setProviderName("MOCK_PROVIDER");
        conn.setDeviceName("Test Wearable");
        conn.setTokenReference("tok-ref-should-never-leak");
        conn.setConsentGrantedAt(Instant.now().minusSeconds(3600));
        conn.setLastSyncedAt(Instant.now().minusSeconds(1800));
        conn.setStatus(DeviceConnectionStatus.ACTIVE);
        return conn;
    }

    static HealthDeviceConnection makeActiveConnection(Consumer<HealthDeviceConnection> overrides) {
        HealthDeviceConnection conn = makeActiveConnection();
        overrides.accept(conn);
        return conn;
    }

    // Raw measurement DTO returned by WearableProviderClient — valid, within sanity range
    static RawMeasurement makeRawMeasurement() {
        RawMeasurement raw = new RawMeasurement();
        raw.setMeasurementType("HEART_RATE");
        raw.setValueNumeric(new BigDecimal("72"));
        raw.setUnit("bpm");
        raw.setMeasuredAt(Instant.now());
        raw.setSourceRecordId(UUID.nameUUIDFromBytes(
            ("provider-record-" + UUID.randomUUID()).getBytes()));
        return raw;
    }

    static RawMeasurement makeRawMeasurement(Consumer<RawMeasurement> overrides) {
        RawMeasurement raw = makeRawMeasurement();
        overrides.accept(raw);
        return raw;
    }

    static DeviceMeasurement makeExistingMeasurement(UUID connectionId, UUID sourceRecordId) {
        DeviceMeasurement m = new DeviceMeasurement();
        m.setId(UUID.randomUUID());
        m.setConnectionId(connectionId);
        m.setMeasurementType("HEART_RATE");
        m.setValueNumeric(new BigDecimal("70"));
        m.setUnit("bpm");
        m.setMeasuredAt(Instant.now().minusSeconds(7200));
        m.setSourceRecordId(sourceRecordId);
        return m;
    }
}
```

---

### SYNC-TC-001 — Happy path manual sync: saves new measurements, updates lastSyncedAt, publishes event

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceSyncService.syncNow()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §6.1 Sequence Diagram — Happy Path` / `ADR-SYNC-002 §Decision`

**Preconditions:** `HealthDeviceSyncTestFactory.makeActiveConnection()` returned by `findById()`; `providerClient.fetchNewMeasurements(...)` returns 1 valid `RawMeasurement` (`FX-SYNC-RAW-VALID`); `existsByConnectionIdAndSourceRecordId()` returns `false`

**Test Steps:**
1. Arrange: mock repo/provider/validator as above
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: `measurementRepository.save()` called once with mapped `DeviceMeasurement`; `connectionRepository.save()` called with `lastSyncedAt` updated to a time ≥ test start; response `syncedCount=1`, `skippedCount=0`

**Expected Result (PASS):** Measurement persisted, `lastSyncedAt` advanced, response counts correct.
**Expected Result (FAIL):** Measurement not saved, or `lastSyncedAt` unchanged, or wrong counts.

**Current Status:** 🔴 Not written

---

### SYNC-TC-002 — Connection not ACTIVE rejected (SYNC-002), no provider call

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceSyncService.syncNow()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §10 Error Codes (SYNC-002)` / `ADR-SYNC-004`

**Preconditions:** `makeActiveConnection(c -> c.setStatus(INACTIVE))` (`FX-SYNC-CONN-INACTIVE`)

**Test Steps:**
1. Arrange: connection status = INACTIVE
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: throws `DeviceSyncException` with code `SYNC-002`; `providerClient.fetchNewMeasurements()` never called; `connectionRepository.save()` never called

**Expected Result (PASS):** 409 SYNC-002, zero side effects.
**Expected Result (FAIL):** Provider called despite inactive connection, or wrong error code.

**Current Status:** 🔴 Not written

---

### SYNC-TC-003 — Consent missing/null rejected (SYNC-002), re-verified every call (ADR-SYNC-004)

**Severity:** `CRITICAL`
**Legal:** `PDPA / Luật 91/2025 — consent withdrawal must take effect immediately`
**Feature Under Test:** `DeviceSyncService.syncNow()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SYNC-004 §Decision`

**Preconditions:** `makeActiveConnection(c -> c.setConsentGrantedAt(null))` (`FX-SYNC-CONN-NO-CONSENT`)

**Test Steps:**
1. Arrange: `status=ACTIVE` but `consentGrantedAt=null`
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: throws `DeviceSyncException(SYNC-002)`; provider never called

**Expected Result (PASS):** Sync blocked without valid consent, even though status is ACTIVE.
**Expected Result (FAIL):** Sync proceeds despite missing consent — PDPA violation.

**Current Status:** 🔴 Not written

---

### SYNC-TC-004 — Non-owner sync attempt rejected (403 SYNC-004)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `DeviceSyncService.syncNow()` ownership check
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-RBAC` / `TDS §16 Authorization Matrix`

**Preconditions:** `makeActiveConnection(c -> c.setUserId(OTHER_MOTHER_USER_ID))` (`FX-SYNC-CONN-OTHER-USER`); caller = `MOTHER_USER_ID`

**Test Steps:**
1. Arrange: connection owned by `OTHER_MOTHER_USER_ID`
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: throws `AccessDeniedException`/`DeviceSyncException(SYNC-004)`; provider never called

**Expected Result (PASS = safe):** 403 SYNC-004.
**Expected Result (FAIL = vulnerability):** Sync succeeds on another user's connection — cross-user RBAC bypass, direct data leak of Sensitive-PII.

**Current Status:** 🔴 Not written

---

### SYNC-TC-005 — Non-existent connection id rejected (404 SYNC-003)

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceSyncService.syncNow()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §10 Error Codes (SYNC-003)`

**Test Steps:**
1. Arrange: `findById()` returns `Optional.empty()`
2. Act: `service.syncNow(nonExistentId, MOTHER_USER_ID)`
3. Assert: throws `DeviceSyncException(SYNC-003)`

**Expected Result (PASS):** 404 SYNC-003.
**Expected Result (FAIL):** `NoSuchElementException` leaks or wrong error code.

**Current Status:** 🔴 Not written

---

### SYNC-TC-006 — Out-of-range measurement skipped, valid ones still saved (skip-and-continue)

**Severity:** `HIGH`
**Feature Under Test:** `DeviceSyncService.syncNow()` + `DeviceMeasurementValidator.isWithinSanityRange()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SYNC-005 §Decision` / `TDS §8.1 Sanity Range Table (HEART_RATE 30-250 bpm)`

**Preconditions:** provider returns 2 `RawMeasurement`: 1 valid (`FX-SYNC-RAW-VALID`, 72bpm), 1 invalid (`FX-SYNC-RAW-OUT-OF-RANGE`, 300bpm)

**Test Steps:**
1. Arrange: batch of 2 raw measurements as above
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: `measurementRepository.save()` called exactly once (only the valid record); response `syncedCount=1`, `skippedCount=1`, `skippedReasons` contains a neutral (non-diagnostic) message; no exception thrown

**Expected Result (PASS):** Partial success — valid record saved, invalid skipped, batch not aborted.
**Expected Result (FAIL):** Either both records saved (validation bypassed) or entire batch rejected (violates ADR-SYNC-005 skip-and-continue).

**Current Status:** 🔴 Not written

---

### SYNC-TC-007 — Duplicate sourceRecordId skipped (idempotency)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceSyncService.syncNow()` idempotency guard
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §6.4 Invariant #2` / Constraint C5 (§17.1)

**Preconditions:** `makeRawMeasurement(r -> r.setSourceRecordId(EXISTING_SOURCE_RECORD_ID))` (`FX-SYNC-RAW-DUPLICATE`); `measurementRepository.existsByConnectionIdAndSourceRecordId(connectionId, EXISTING_SOURCE_RECORD_ID)` mocked to return `true`

**Test Steps:**
1. Arrange: provider returns 1 raw measurement whose `sourceRecordId` already exists for this connection
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: `measurementRepository.save()` never called for that record; response `skippedCount=1`; `skippedReasons` mentions duplicate

**Expected Result (PASS):** No duplicate insert attempted.
**Expected Result (FAIL):** `save()` called despite existing `sourceRecordId` — violates idempotency invariant.

**Current Status:** 🔴 Not written

---

### SYNC-TC-008 — Provider unavailable before any data received → 503 SYNC-005, lastSyncedAt unchanged

**Severity:** `HIGH`
**Feature Under Test:** `DeviceSyncService.syncNow()` error/retry path
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §6.3 Sequence Diagram — Error/Retry Path` / `§6.4 Invariant #3`

**Preconditions:** `providerClient.fetchNewMeasurements(...)` throws `ProviderUnavailableException` (`FX-SYNC-PROVIDER-UNAVAILABLE`)

**Test Steps:**
1. Arrange: provider mock throws on first call
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: throws `DeviceSyncException(SYNC-005)` with `retryable=true`; `connectionRepository.save()` (i.e. `lastSyncedAt` update) never called; `measurementRepository.save()` never called

**Expected Result (PASS):** 503 SYNC-005 retryable; no partial state change.
**Expected Result (FAIL):** `lastSyncedAt` advances despite total provider failure — next scheduled sync would silently skip the missed window.

**Current Status:** 🔴 Not written

---

### SYNC-TC-009 — Sync-created measurements are NOT tagged with UC67's manual-import source semantics

**Severity:** `HIGH`
**Feature Under Test:** `DeviceSyncService.syncNow()` mapping RawMeasurement → DeviceMeasurement
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §7.1 Domain Event Catalog — naming rationale` / `TDS §1 Out-of-scope (manual entry = UC67, different entity/table)`

**Preconditions:** happy path setup (`FX-SYNC-CONN-ACTIVE`, `FX-SYNC-RAW-VALID`)

**Test Steps:**
1. Arrange: happy path
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`
3. Assert: persisted row is a `device_measurements` entity (via `IDeviceMeasurementRepository`), NOT a `maternal_health_metrics` row (via UC67's `MaternalHealthMetricRepository`); the published event is `DeviceDataSynced`, not `DeviceDataImported`

**Expected Result (PASS):** UC130 sync writes exclusively to `device_measurements` / publishes `DeviceDataSynced`, structurally distinct from UC67's manual-import path (`maternal_health_metrics` / `DeviceDataImported`).
**Expected Result (FAIL):** Sync path accidentally writes into UC67's table or reuses UC67's event type — semantic collision noted in TDS §7.1 as explicitly rejected.

**Current Status:** 🔴 Not written
**Implementation Note:** This test encodes TDS §7.1's explicit decision NOT to reuse `DeviceDataImported`. It does not assert on a `sourceType` enum column because no such column is confirmed for `device_measurements` in `V1__init_schema.sql`; it asserts entity/table/event separation only. See Open Item flagged in final report.

---

### SYNC-TC-010 — DeviceDataSynced event published exactly once, including syncedCount=0 case

**Severity:** `HIGH`
**Feature Under Test:** `DeviceSyncService.syncNow()` → `ApplicationEventPublisher`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §7.1 Domain Event Catalog` / Constraint C7 (§17.1)

**Test Steps:**
1. Arrange (case A): happy path, 1 measurement synced
2. Arrange (case B): `providerClient.fetchNewMeasurements(...)` returns empty list
3. Act: `service.syncNow(connectionId, MOTHER_USER_ID)` for both cases
4. Assert: `eventPublisher.publishEvent(any(DeviceDataSynced.class))` called exactly once in BOTH cases; payload `syncedCount` matches (1 and 0 respectively); `triggerType="MANUAL"`

**Expected Result (PASS):** Event fires even when there is nothing new to sync.
**Expected Result (FAIL):** Event skipped when `syncedCount=0` — violates C7 ("kể cả khi syncedCount=0").

**Current Status:** 🔴 Not written

---

### SYNC-TC-011 — DeviceSyncFailed event published on provider failure (not DeviceDataSynced)

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceSyncService.syncNow()` error path event
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §7.1 Domain Event Catalog` / `§7.3 Payload Schema (DeviceSyncFailed)`

**Preconditions:** `FX-SYNC-PROVIDER-UNAVAILABLE`

**Test Steps:**
1. Arrange: provider throws `ProviderUnavailableException`
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`, expect exception
3. Assert: `eventPublisher.publishEvent(any(DeviceSyncFailed.class))` called once with `retryable=true`, neutral `failureReason` text; `DeviceDataSynced` NEVER published for this call

**Expected Result (PASS):** Correct event type emitted for failure path, mutually exclusive with success event.
**Expected Result (FAIL):** Wrong event type, or both events published, or `failureReason` uses diagnostic/alarming language (violates BR-SAFETY neutral-language rule).

**Current Status:** 🔴 Not written

---

### SYNC-TC-012 — syncAllActiveConnections() iterates all ACTIVE connections; per-connection failure does not abort batch

**Severity:** `HIGH`
**Feature Under Test:** `DeviceSyncService.syncAllActiveConnections()` / `DeviceSyncScheduler`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §6.2 Sequence Diagram — Scheduled Background Sync` / `ADR-SYNC-002 §Decision`

**Preconditions:** `connectionRepository.findByStatus(ACTIVE)` returns 3 connections; syncing connection #2 throws an unexpected `RuntimeException`

**Test Steps:**
1. Arrange: 3 ACTIVE connections, connection #2's internal `syncNow()` call throws
2. Act: `service.syncAllActiveConnections()`
3. Assert: connections #1 and #3 are still synced (their `syncNow()` invoked and completed); no exception propagates out of `syncAllActiveConnections()`; failure for #2 is caught and logged

**Expected Result (PASS):** One failing connection does not block the rest of the scheduled batch.
**Expected Result (FAIL):** Exception from connection #2 propagates and connections #1/#3 (processed after #2 in iteration order) never sync — violates ADR-SYNC-005 skip-and-continue principle extended to scheduler level.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### SYNC-TC-013 — token_reference never leaks in response DTO or event payload

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control` (sensitive data exposure adjacent)
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** `PDPA / Luật 91/2025 — token/credential confidentiality`
**Feature Under Test:** `DeviceSyncResultResponse` mapping + `DeviceDataSynced`/`DeviceSyncFailed` payload construction
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceSyncServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §4.3 Security` / Constraint C6 (§17.1)

**Preconditions:** `makeActiveConnection()` has `tokenReference="tok-ref-should-never-leak"`

**Test Steps (Attack Simulation):**
1. Arrange: happy path sync
2. Act: `service.syncNow(connectionId, MOTHER_USER_ID)`, capture response and published event payload
3. Assert: serialize `DeviceSyncResultResponse` to JSON — string `"tok-ref-should-never-leak"` does NOT appear; serialize captured `DeviceDataSynced`/`DeviceSyncFailed` payload — same assertion

**Expected Result (PASS = hệ thống an toàn):** No token value anywhere in response/event payload.
**Expected Result (FAIL = lỗ hổng tồn tại):** `tokenReference` leaks into API response or event/audit log — credential exposure.

**Current Status:** 🔴 Not written

---

### SYNC-TC-E2E-001 — ROLE_PARTNER attempts manual sync trigger → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `DeviceSyncController.POST /api/v1/health/devices/connections/{id}/sync`
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/DeviceSyncControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `@WebMvcTest(DeviceSyncController.class)`; JWT with `ROLE_PARTNER`

**Test Steps (Attack Simulation):**
1. Arrange: authenticate as `PARTNER_USER_ID` with `ROLE_PARTNER`
2. Act: `POST /api/v1/health/devices/connections/{id}/sync`
3. Assert: `403 Forbidden`; `deviceSyncService.syncNow()` never invoked

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`, no service call.
**Expected Result (FAIL = lỗ hổng tồn tại):** Partner role can trigger sync on a connection it does not own/manage — RBAC bypass.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SYNC-TC-INT-001 — Full manual sync flow via Testcontainers: persisted rows + last_synced_at

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /sync → device_measurements rows + health_device_connections.last_synced_at`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceSyncIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL Testcontainer; baseline `V1__init_schema.sql` applied (tables pre-exist, no UC130 migration required per §5.2)
- Seed: 1 `health_device_connections` row (`status=ACTIVE`, `consent_granted_at` set) via `HealthDeviceSyncTestFactory`
- `MockWearableProviderClient` configured (via test profile) to return 2 valid `RawMeasurement`

**Test Steps:**
1. Seed ACTIVE connection with consent
2. Call `POST /api/v1/health/devices/connections/{id}/sync` with valid Mother JWT
3. Assert DB state directly

**Expected Result (PASS):**
- Response `200 OK`, `syncedCount=2`, `skippedCount=0`
- `device_measurements` table has exactly 2 new rows with `connection_id` matching the seeded connection
- `health_device_connections.last_synced_at` updated to a value ≥ test start time

**Expected Result (FAIL):** Any of the above not satisfied.

**DB Assertion:**
```java
List<DeviceMeasurement> rows = measurementRepository.findByConnectionIdOrderByMeasuredAtDesc(connectionId);
assertThat(rows).hasSize(2);
HealthDeviceConnection conn = connectionRepository.findById(connectionId).orElseThrow();
assertThat(conn.getLastSyncedAt()).isAfterOrEqualTo(testStartInstant);
```

**Current Status:** 🔴 Not written

---

### SYNC-TC-INT-002 — Idempotency: repeated sync with same mock dataset does not duplicate rows

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: repeated POST /sync calls`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceSyncIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:**
- PostgreSQL Testcontainer; seeded ACTIVE connection
- `MockWearableProviderClient` returns the SAME set of `RawMeasurement` (same `sourceRecordId` values) on every call — simulates provider re-delivering already-fetched records

**Test Steps:**
1. Seed ACTIVE connection
2. Call `POST .../sync` (1st call) → expect `syncedCount=2`
3. Call `POST .../sync` (2nd call, same mock dataset) → expect `syncedCount=0`, `skippedCount=2` (all duplicates)
4. Assert DB state directly

**Expected Result (PASS):**
- Total `device_measurements` row count for the connection after both calls = 2 (not 4)
- No unique constraint violation / exception on 2nd call

**Expected Result (FAIL):** Duplicate rows inserted, or 2nd call throws instead of gracefully skipping — violates idempotency invariant (TDS §6.4 Invariant #2, Open Item O4 risk realized).

**DB Assertion:**
```java
long count = measurementRepository.findByConnectionIdOrderByMeasuredAtDesc(connectionId).size();
assertThat(count).isEqualTo(2L);
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (Flutter — `flutter_test`)

---

### SYNC-TC-MOB-001 — Mobile DeviceSyncService calls sync endpoint and parses response

**Severity:** `HIGH`
**Feature Under Test:** `lib/features/health_device/services/device_sync_service.dart` — `DeviceSyncService.syncNow(connectionId)`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/health_device/device_sync_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS §9.2 Response Schema (200 OK)`

**Preconditions:** Mocked HTTP client (`http.Client` mock or `MockClient` from `package:http/testing.dart`) returns the TDS §9.2 sample JSON body with status 200

**Test Steps:**
1. Arrange: mock client returns `{"connectionId":..., "syncedCount":5, "skippedCount":1, "skippedReasons":[...], "lastSyncedAt":...}`
2. Act: `await deviceSyncService.syncNow(connectionId)`
3. Assert: returned model has `syncedCount == 5`, `skippedCount == 1`, `skippedReasons.length == 1`

**Expected Result (PASS):** DTO correctly deserialized from API response shape defined in TDS §9.2.
**Expected Result (FAIL):** Parsing exception or field mismatch (e.g. wrong JSON key names) — a hallucinated-contract risk against §9.2.

**Current Status:** 🔴 Not written

---

### SYNC-TC-MOB-002 — "Sync Now" widget shows loading → success summary or error state

**Severity:** `MEDIUM`
**Feature Under Test:** `lib/features/health_device/widgets/sync_now_button.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/health_device/sync_now_button_widget_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §9.2` (success/error response shapes) — UI text/copy is Open (no SRS source), test asserts STATE transitions only, not exact copy

**Test Steps (Case A — success):**
1. Pump widget with mocked `DeviceSyncService` returning a successful `DeviceSyncResultResponse`
2. Tap "Sync Now" button
3. Assert: widget shows a loading indicator immediately after tap, then transitions to a success/summary state showing `syncedCount`/`skippedCount` once the future resolves

**Test Steps (Case B — provider error, SYNC-005):**
1. Pump widget with mocked `DeviceSyncService` throwing an exception mapped from `SYNC-005`
2. Tap "Sync Now" button
3. Assert: widget transitions to an error state (not a crash, not a silently-swallowed failure)

**Expected Result (PASS):** Both loading→success and loading→error transitions render without exceptions leaking into the widget tree.
**Expected Result (FAIL):** Widget throws unhandled exception, or stays stuck in loading state, or silently shows nothing on error.

**Current Status:** 🔴 Not written
**Implementation Note:** Exact error message copy is not specified anywhere in TDS/SRS — assert on state/semantics (e.g. `find.byType(ErrorBanner)`), not literal text, to avoid encoding an unsourced assumption as a hard oracle.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SYNC-TC-001` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-002` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-003` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-004` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-005` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-006` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-007` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-008` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-009` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-010` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-011` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-012` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-013` | `DeviceSyncServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-E2E-001` | `DeviceSyncControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-INT-001` | `DeviceSyncIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-INT-002` | `DeviceSyncIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-MOB-001` | `device_sync_service_test.dart:TBD` | `[ ]` | `[ ]` | |
| `SYNC-TC-MOB-002` | `sync_now_button_widget_test.dart:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// Red Phase — implementation stub (PHẢI throw)
// package com.carebridge.backend.health.device.service
@Service
public class DeviceSyncService implements IDeviceSyncService {

    @Override
    public DeviceSyncResultResponse syncNow(UUID connectionId, UUID callerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void syncAllActiveConnections() {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```dart
// Red Phase — Mobile stub (PHẢI throw)
class DeviceSyncService {
  Future<DeviceSyncResultResponse> syncNow(String connectionId) {
    throw UnimplementedError('Not implemented — Red Phase stub');
  }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SYNC-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-E2E-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-MOB-001` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SYNC-TC-MOB-002` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS**
- Log file: chưa tạo (Draft phase)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-DEVICE-SYNC-001` đã review và approve
- [x] ~~**ADR-SYNC-001 đã được Tech Lead xác nhận**~~ — **RESOLVED 2026-07-02** (không còn BLOCKER — xem TDS §3, ADR-SYNC-001 nay Status=Accepted, và CHANGELOG)
- [ ] ADR-SYNC-002, ADR-SYNC-003 đã Accepted (hiện đang `Proposed`)
- [ ] DPO sign-off (module xử lý health/wearable sync data tự động)
- [ ] Nếu chưa có luồng "connect" thật trên schema thực, dùng `HealthDeviceSyncTestFactory` để seed trực tiếp `HealthDeviceConnection` (per TDS Open Item O2) — KHÔNG block test-writing trên việc này

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] `./mvnw verify` (Testcontainers) xanh, đặc biệt `SYNC-TC-INT-002` (idempotency)
- [ ] `flutter test` xanh cho `SYNC-TC-MOB-001`, `SYNC-TC-MOB-002`
- [ ] Coverage ≥ 80% cho `DeviceSyncService`
- [ ] Không business logic trong `DeviceSyncController`
- [ ] `token_reference` xác nhận không leak (`SYNC-TC-013`)
- [ ] Consent re-check mỗi lần sync xác nhận qua test (`SYNC-TC-003`)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate pass (§5.1)
- [ ] Contract Existence verified (`./mvnw compile` sạch lỗi)
- [ ] Props Isolation — dùng `HealthDeviceSyncTestFactory` cho mọi test case, không shared mutable state
- [ ] Oracle Source ghi rõ cho mọi assert

### Suspension Criteria

- [x] ~~ADR-SYNC-001 chưa được Tech Lead resolve (rủi ro: implement song song 2 hệ schema với UC66-69)~~ — **RESOLVED 2026-07-02.** UC66-69 đã được sửa lại để dùng cùng schema thực (`health_device_connections`/`device_measurements`) như UC130.
- `health_device_connections`/`device_measurements` không truy cập được trên môi trường test (bảng phải tồn tại từ `V1__init_schema.sql` — nếu thiếu, baseline schema bị lệch)

---

## 7. Rollback Plan

```bash
# Nếu migration index tùy chọn (V20260704130000) đã áp dụng:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_health_device_connections_status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_device_measurements_connection_source;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260704130000';"

# Revert implementation files (không có bảng mới để DROP — bảng đã tồn tại từ V1)
git checkout -- src/main/java/com/carebridge/backend/health/device/service/DeviceSyncService.java
git checkout -- src/main/java/com/carebridge/backend/health/device/controller/DeviceSyncController.java
git checkout -- src/main/java/com/carebridge/backend/health/device/scheduler/DeviceSyncScheduler.java
git checkout -- src/main/java/com/carebridge/backend/integration/wearable/
git checkout -- src/test/java/com/carebridge/backend/health/device/
kubectl rollout undo deployment/carebridge-api

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu áp dụng)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-SYNC-00X nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với `UnsupportedOperationException` stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume real vendor SDK behavior không có trong ADR-SYNC-003 (vẫn Proposed, mock-only) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify `DeviceSyncController` có business logic (consent check, sanity validation) thay vì delegate cho Service | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import type/method không tồn tại trong §8 Interface Specification hoặc trong codebase | ☐ | G-3 |
| AP-DEVICE-SYNC-006 | Duplicate metric rows on repeated sync | Test không cover idempotency (`SYNC-TC-007`, `SYNC-TC-INT-002`) — thiếu assertion về duplicate `(connection_id, source_record_id)` | ☐ | G-5 |
| AP-DEVICE-SYNC-007 | Syncing data for a disconnected/inactive connection | Test không cover `status != ACTIVE` gating (`SYNC-TC-002`) hoặc consent-null gating (`SYNC-TC-003`) | ☐ | G-5 |
| AP-DEVICE-SYNC-008 | Schema Drift (UC130-specific) | Test hoặc implementation accidentally targets UC66's self-proposed `device_connections` table/entity instead of the real `health_device_connections`/`device_measurements` (ADR-SYNC-001) | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern → approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
