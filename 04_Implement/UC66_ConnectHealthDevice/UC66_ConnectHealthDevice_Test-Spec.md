# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC66 — Connect Health Device

**Document ID:** `CB-DEVICE-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect / Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC66_ConnectHealthDevice/UC66_ConnectHealthDevice_TDS.md` (CB-DEVICE-IMP-001)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.43`
- `04_Implement/implement_artifacts/function-spec-task-allocation.md` (dòng 676-680)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC66 Connect Health Device (Draft) |

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
| **Feature / Gap ID** | `CB-DEVICE-IMP-001` |
| **Module** | `Connect Health Device — health.device` |
| **Spec gốc** | `CB-DEVICE-IMP-001` (TDS) |
| **Priority** | 🟠 P1 (High per SRS) |
| **Sprint** | `Device Sync And Care Edge Cases (TV2-Bách)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT)`, `consent` module |
| **Downstream Consumers** | `UC67, UC68, UC69` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-DEVICE-IMP-001 §17`, `ADR-DEVICE-001/002` |
| **Constraints Injected** | C1 (consent-before-connect), C2 (idempotent reconnect), C3 (userId from JWT), C4 (append-only, no delete), C5 (DeviceConnected event) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-66 flows là generic template — không quy định hành vi khi reconnect trong khi đã CONNECTED | ADR-DEVICE-001: idempotent — trả về record hiện có | Test verify 2 lần connect() liên tiếp cùng deviceType = 1 record CONNECTED, response thứ 2 vẫn 2xx |
| L2 | `ConsentDataType` không có giá trị `DEVICE_DATA` chuyên biệt (Open Item O1 trong TDS) | Enum hiện tại: `HEALTH_RECORD, LOCATION, FAMILY_DATA, COMMUNITY_POST, SENSITIVE_DATA, RAG_CONTEXT, EXPERT_SHARED_DATA` | Test dùng `HEALTH_RECORD` làm giá trị tạm thời cho `ConsentGrant.dataType`; nếu Tech Lead approve thêm `DEVICE_DATA`, cập nhật test fixture tương ứng (ghi chú rõ trong `FX-CONSENT-001`) |
| L3 | Spec không nêu rõ cấu trúc bảng — không có bảng `device`/`wearable` nào trong `V1__init_schema.sql` | Xác nhận greenfield — cần migration mới `V20260701140000__create_device_connections.sql` | Test integration verify migration tạo đúng bảng + FK constraints |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Connect Health Device (health.device) bao gồm các layer:
├── Domain (DeviceConnection entity — pure logic, no deps)
├── Service (DeviceConnectionService — mock IDeviceConnectionRepository + ConsentService + ApplicationEventPublisher với Mockito)
├── Controller (DeviceConnectionController — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL @SpringBootTest — verify persisted row + consent linkage)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-66 §3.3.1.43` | Mother connects wearable/health platform after consent |
| `ADR-DEVICE-001` | Idempotent connect; append-only lifecycle |
| `ADR-DEVICE-002` | Consent capture required before CONNECTED |
| `BR-RBAC` | Only authenticated ROLE_MOTHER may connect own device |
| `BR-PRIVACY` | Consent/purpose/minimum-necessary access rules |
| `V1__init_schema.sql` + `V20260701140000` (new) | `device_connections` table structure, FK to `users`/`mother_journeys`/`consent_grants` |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path: valid request + consent accepted → 201 CONNECTED | `DeviceConnectionService.connect()` | `DEVICE-TC-001` |
| TC-COND-002 | Reconnect while already CONNECTED (same deviceType) → idempotent, no duplicate | `connect()` | `DEVICE-TC-002` |
| TC-COND-003 | `deviceType` missing/invalid → 400 DEVICE-001 | `ConnectDeviceRequest` validation | `DEVICE-TC-003` |
| TC-COND-004 | `consentAccepted=false` → 400 DEVICE-006 | `connect()` | `DEVICE-TC-004` |
| TC-COND-005 | Non-MOTHER role attempts connect → 403 DEVICE-004 | `DeviceConnectionController` | `DEVICE-TC-005` |
| TC-COND-006 | Consent grant created and linked (`consent_grant_id`) before status=CONNECTED | `ConsentService.grant()` integration | `DEVICE-TC-006` |
| TC-COND-007 | `DeviceConnected` event published exactly once per successful connect | `EventPublisher` | `DEVICE-TC-007` |
| TC-COND-008 | Two different deviceTypes for same user → both CONNECTED simultaneously (no cross-device conflict) | `connect()` | `DEVICE-TC-008` |
| TC-COND-009 | Ownership: user A cannot list/see user B's connections | `listActiveConnections()` | `DEVICE-TC-009` |
| TC-COND-010 (Integration) | Full connect flow via Testcontainers — DB row + FK to consent_grants persisted | End-to-end | `DEVICE-TC-INT-001` |
| TC-COND-011 (Security) | SQL/script injection attempt in `deviceName` field handled safely | Input sanitization | `DEVICE-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `deviceType` (valid enum values vs invalid string) | Cover valid/invalid input classes |
| Boundary Value Analysis | `deviceName` length (0, 1, 120, 121 chars) | Column is VARCHAR(120) |
| State Transition Testing | CONNECTED reconnect idempotency | Core state machine invariant (ADR-DEVICE-001) |
| Error Guessing | Concurrent double-submit of connect() | Race condition on idempotent check |
| Security Testing (Error Guessing) | Injection in `deviceName` | OWASP A03:2021 Injection |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-USER-MOTHER-001` | JWT / DB seed | `{ userId: '00000000-0000-0000-0000-000000000001', role: 'MOTHER' }` | Happy path actor |
| `FX-USER-PARTNER-001` | JWT / DB seed | `{ userId: '00000000-0000-0000-0000-000000000002', role: 'PARTNER' }` | RBAC negative test |
| `FX-DEVICE-CONN-001` | DB seed | `{ deviceType: 'SMARTWATCH', deviceName: 'Mi Band 8', status: 'CONNECTED' }` | Reconnect/idempotency test |
| `FX-CONSENT-001` | DB seed | `{ dataType: 'HEALTH_RECORD', purpose: 'SHARE', consentGivenAt: now(), expiryAt: now()+365d }` | Consent linkage verification (see L2) |
| `FX-REQ-VALID-001` | Request body | `{ deviceType: 'SMARTWATCH', deviceName: 'Mi Band 8', consentAccepted: true }` | Happy path request |
| `FX-REQ-INVALID-001` | Request body | `{ deviceType: 'INVALID_TYPE', consentAccepted: true }` | Validation failure |
| `FX-REQ-NOCONSENT-001` | Request body | `{ deviceType: 'SMARTWATCH', consentAccepted: false }` | Consent rejection |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ |
| Mobile (Flutter) | ✅ (service/repo layer) | — | ✅ | ✅ (widget test — connect screen consent dialog) | — | — |

---

## 4. Test Case Specification

> **TC ID format:** `DEVICE-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state giữa các test cases. Factory này (`DeviceConnectionTestFactory`) được tái sử dụng thống nhất trong Test-Spec của cả UC66/UC67/UC68/UC69.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// DeviceConnectionTestFactory.java — shared across UC66/67/68/69 test suites
// Đặt tại: src/test/java/com/carebridge/backend/health/device/DeviceConnectionTestFactory.java
// ═══════════════════════════════════════════════════════════

class DeviceConnectionTestFactory {

    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID PARTNER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID OTHER_MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // Giá trị baseline hợp lệ — đồng bộ với FX-DEVICE-CONN-001 (§3 TDS-05)
    static DeviceConnection makeConnectedDevice() {
        return makeConnectedDevice(c -> {});
    }

    static DeviceConnection makeConnectedDevice(Consumer<DeviceConnection> overrides) {
        DeviceConnection conn = DeviceConnection.builder()
            .id(UUID.randomUUID())
            .userId(MOTHER_USER_ID)
            .deviceType(DeviceType.SMARTWATCH)
            .deviceName("Mi Band 8")
            .status(DeviceConnectionStatus.CONNECTED)
            .consentGrantId(1L)
            .consentedAt(Instant.parse("2026-07-01T08:00:00Z"))
            .connectedAt(Instant.parse("2026-07-01T08:00:00Z"))
            .build();
        overrides.accept(conn);
        return conn;
    }

    static DeviceConnection makeDisconnectedDevice() {
        return makeConnectedDevice(c -> {
            c.setStatus(DeviceConnectionStatus.DISCONNECTED);
            c.setDisconnectedAt(Instant.parse("2026-07-01T09:00:00Z"));
        });
    }

    static ConnectDeviceRequest makeValidRequest() {
        return makeValidRequest(r -> {});
    }

    static ConnectDeviceRequest makeValidRequest(Consumer<ConnectDeviceRequest> overrides) {
        ConnectDeviceRequest request = new ConnectDeviceRequest();
        request.setDeviceType(DeviceType.SMARTWATCH);
        request.setDeviceName("Mi Band 8");
        request.setConsentAccepted(true);
        overrides.accept(request);
        return request;
    }

    static ConsentGrant makeConsentGrant() {
        return ConsentGrant.builder()
            .id(1L)
            .userId(MOTHER_USER_ID)
            .dataType(ConsentDataType.HEALTH_RECORD) // see Logic Issue L2
            .purpose(ConsentPurpose.SHARE)
            .consentGivenAt(Instant.parse("2026-07-01T08:00:00Z"))
            .expiryAt(Instant.parse("2027-07-01T08:00:00Z"))
            .version(1)
            .build();
    }
}
```

---

### DEVICE-TC-001 — Connect device happy path creates CONNECTED record with consent

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceConnectionService.connect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-66 Normal Flow Step 4-5` / `ADR-DEVICE-002`

**Preconditions:**
- No existing `device_connections` record for `MOTHER_USER_ID` + `SMARTWATCH`
- `ConsentService.grant()` mocked to return `makeConsentGrant()`

**Test Steps:**
1. Arrange: `ConnectDeviceRequest request = DeviceConnectionTestFactory.makeValidRequest();` mock repository `findFirstByUserIdAndDeviceTypeAndStatus...` returns `Optional.empty()`
2. Act: `service.connect(request, MOTHER_USER_ID)`
3. Assert: repository.save() called once with entity having `status=CONNECTED`, `consentGrantId=1`, response DTO has `status="CONNECTED"`

**Expected Result (PASS — hành vi đúng):**
- Response `DeviceConnectionResponse.status == "CONNECTED"`, `consentedAt` non-null.

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or status not CONNECTED, or consent not called before save.

**Current Status:** 🔴 Not written
**Implementation Note:** Ensure `ConsentService.grant()` is called and awaited BEFORE `repository.save()` per ADR-DEVICE-002 ordering.

---

### DEVICE-TC-002 — Reconnect while already CONNECTED is idempotent (no duplicate)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceConnectionService.connect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-DEVICE-001 §Decision`

**Preconditions:**
- Mock repository `findFirstByUserIdAndDeviceTypeAndStatusOrderByConnectedAtDesc(MOTHER_USER_ID, SMARTWATCH, CONNECTED)` returns `Optional.of(makeConnectedDevice())`

**Test Steps:**
1. Arrange: existing connection fixture
2. Act: `service.connect(makeValidRequest(), MOTHER_USER_ID)`
3. Assert: `repository.save()` is NEVER called; returned response matches the existing connection's id

**Expected Result (PASS):** No new row created; response reflects existing CONNECTED record.
**Expected Result (FAIL):** `repository.save()` invoked (duplicate created) or exception thrown.

**Current Status:** 🔴 Not written

---

### DEVICE-TC-003 — Invalid deviceType rejected with DEVICE-001

**Severity:** `HIGH`
**Feature Under Test:** `ConnectDeviceRequest` validation / `DeviceConnectionController`
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/DeviceConnectionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `SRS UC-66 Exception E2` / TDS §10 Error Codes

**Preconditions:** MockMvc configured with `@WebMvcTest(DeviceConnectionController.class)`

**Test Steps:**
1. Arrange: JSON body with `deviceType: "INVALID_TYPE"`
2. Act: `POST /api/v1/health/devices/connections`
3. Assert: HTTP 400, body `error.code == "DEVICE-001"`

**Expected Result (PASS):** 400 + `DEVICE-001`.
**Expected Result (FAIL):** 200/201 returned, or wrong error code.

**Current Status:** 🔴 Not written

---

### DEVICE-TC-004 — consentAccepted=false rejected with DEVICE-006

**Severity:** `CRITICAL`
**Legal:** `PDPA / Luật 91/2025 — consent required before processing health device data`
**Feature Under Test:** `DeviceConnectionService.connect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-DEVICE-002` / TDS §10 `DEVICE-006`

**Preconditions:** Request with `consentAccepted=false`

**Test Steps:**
1. Arrange: `makeValidRequest(r -> r.setConsentAccepted(false))`
2. Act: `service.connect(request, MOTHER_USER_ID)`
3. Assert: throws `DeviceConnectionException` with code `DEVICE-006`; `ConsentService.grant()` and `repository.save()` never called

**Expected Result (PASS):** Exception `DEVICE-006`, no side effects.
**Expected Result (FAIL):** Record created despite missing consent (BR-PRIVACY violation).

**Current Status:** 🔴 Not written

---

### DEVICE-TC-005 — Non-MOTHER role forbidden (403 DEVICE-004)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `DeviceConnectionController` (`@PreAuthorize`)
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/DeviceConnectionControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC` / TDS §16 Authorization Matrix

**Preconditions:** JWT with `role=PARTNER` (`FX-USER-PARTNER-001`)

**Test Steps:**
1. Arrange: authenticate as PARTNER
2. Act: `POST /api/v1/health/devices/connections` with valid body
3. Assert: HTTP 403, `error.code == "DEVICE-004"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` + `DEVICE-004`.
**Expected Result (FAIL = lỗ hổng tồn tại):** 201 returned — RBAC bypass.

**Current Status:** 🔴 Not written

---

### DEVICE-TC-006 — Consent grant linked via consent_grant_id before CONNECTED

**Severity:** `HIGH`
**Feature Under Test:** `DeviceConnectionService.connect()` orchestration order
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-DEVICE-002`

**Test Steps:**
1. Arrange: mock `ConsentService.grant()` returns `makeConsentGrant()` (id=1)
2. Act: `service.connect(makeValidRequest(), MOTHER_USER_ID)`
3. Assert: saved `DeviceConnection.consentGrantId == 1L`; `InOrder` verify `consentService.grant()` called before `repository.save()`

**Expected Result (PASS):** Ordering and linkage correct.
**Expected Result (FAIL):** `consentGrantId` null, or save() called before grant().

**Current Status:** 🔴 Not written

---

### DEVICE-TC-007 — DeviceConnected event published exactly once

**Severity:** `HIGH`
**Feature Under Test:** `DeviceConnectionService.connect()` → `ApplicationEventPublisher`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §7.1 Domain Event Catalog`

**Test Steps:**
1. Arrange: happy path setup
2. Act: `service.connect(makeValidRequest(), MOTHER_USER_ID)`
3. Assert: `eventPublisher.publishEvent(any(DeviceConnected.class))` called exactly 1 time; payload `userId == MOTHER_USER_ID`

**Expected Result (PASS):** Event published once with correct payload.
**Expected Result (FAIL):** Event missing, duplicated, or wrong payload.

**Current Status:** 🔴 Not written

---

### DEVICE-TC-008 — Two different deviceTypes can both be CONNECTED simultaneously

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceConnectionService.connect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-DEVICE-001 §Invariant` (per user, per deviceType uniqueness — not global)

**Test Steps:**
1. Arrange: existing CONNECTED `SMARTWATCH` for `MOTHER_USER_ID`; request `deviceType=BLOOD_PRESSURE_MONITOR`
2. Act: `service.connect(request, MOTHER_USER_ID)`
3. Assert: new record created (save() called); both connections coexist

**Expected Result (PASS):** New CONNECTED record for `BLOOD_PRESSURE_MONITOR` created.
**Expected Result (FAIL):** Second connect rejected or overwrites first.

**Current Status:** 🔴 Not written

---

### DEVICE-TC-009 — Ownership: user cannot list another user's connections

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `DeviceConnectionService.listActiveConnections()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-RBAC` (strict ownership)

**Test Steps:**
1. Arrange: connections exist for `OTHER_MOTHER_USER_ID`
2. Act: `service.listActiveConnections(MOTHER_USER_ID)`
3. Assert: repository queried with `userId=MOTHER_USER_ID` only; result excludes other user's rows

**Expected Result (PASS):** Only caller's own connections returned.
**Expected Result (FAIL):** Cross-user data leak.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### DEVICE-TC-SEC-001 — Injection attempt in deviceName handled safely

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Legal:** `PDPA — data integrity of stored health device metadata`
**Feature Under Test:** `DeviceConnectionController` / JPA parameterized queries
**Test File:** `src/test/java/com/carebridge/backend/health/device/controller/DeviceConnectionControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** MockMvc + Testcontainers PostgreSQL (integration variant recommended)

**Test Steps (Attack Simulation):**
1. Arrange: `deviceName = "Mi Band'; DROP TABLE device_connections;--"`
2. Act: `POST /api/v1/health/devices/connections` with malicious `deviceName`
3. Assert: request either succeeds with `deviceName` stored verbatim as literal string (JPA parameterized) or is rejected by length/charset validation; `device_connections` table still exists afterward

**Expected Result (PASS = hệ thống an toàn):** Table intact; value stored as literal text, no SQL executed.
**Expected Result (FAIL = lỗ hổng tồn tại):** Table dropped or query error indicating raw SQL concatenation.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### DEVICE-TC-INT-001 — Full connect flow persists row with FK linkage

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /connections → DB row + consent_grants FK`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceConnectionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically (including `V20260701140000__create_device_connections.sql`)
- Seed: `users` row for `MOTHER_USER_ID`

**Test Steps:**
1. Seed minimal user row
2. Call `POST /api/v1/health/devices/connections` with valid JWT + body
3. Assert DB state directly via repository

**Expected Result (PASS):**
- `device_connections` row exists with `status='CONNECTED'`, `consent_grant_id` referencing a valid `consent_grants.id`
- API response 201 with matching `id`

**Expected Result (FAIL):**
- Row missing, FK null, or constraint violation error

**DB Assertion:**
```java
DeviceConnection record = deviceConnectionRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(DeviceConnectionStatus.CONNECTED);
assertThat(record.getConsentGrantId()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DEVICE-TC-001` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-002` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-003` | `DeviceConnectionControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-004` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-005` | `DeviceConnectionControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-006` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-007` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-008` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-009` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-SEC-001` | `DeviceConnectionControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `DEVICE-TC-INT-001` | `DeviceConnectionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DeviceConnectionService implements IDeviceConnectionService {

    @Override
    public DeviceConnectionResponse connect(ConnectDeviceRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<DeviceConnectionResponse> listActiveConnections(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `DEVICE-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-003` | N/A (validation layer, not service stub) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEVICE-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` — chưa tạo (Draft phase)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-DEVICE-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead (đặc biệt L2 — ConsentDataType)
- [ ] Flyway migration `V20260701140000__create_device_connections.sql` đã approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `DeviceConnectionService`
- [ ] Không có business logic trong `DeviceConnectionController` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Reconnect idempotency verified (DEVICE-TC-002)
- [ ] Consent-before-connect ordering verified (DEVICE-TC-006)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — dùng `DeviceConnectionTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria

- Migration `V20260701140000` chưa approved
- ConsentDataType enum decision (L2/O1) chưa resolve và block test fixture chính xác

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS device_connections CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260701000001';"

git checkout -- src/main/java/com/carebridge/backend/health/device/
git checkout -- src/main/resources/db/migration/V20260701140000__create_device_connections.sql
git checkout -- src/test/java/com/carebridge/backend/health/device/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume wearable SDK vendor không có ADR (ADR-DEVICE-003 vẫn Proposed) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
