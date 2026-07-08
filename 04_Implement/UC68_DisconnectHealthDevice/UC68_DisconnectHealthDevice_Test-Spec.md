# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC68 — Disconnect Health Device

**Document ID:** `CB-DEVICE-IMP-003-TEST`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect / Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC68_DisconnectHealthDevice/UC68_DisconnectHealthDevice_TDS.md` (CB-DEVICE-IMP-003)
- `04_Implement/UC66_ConnectHealthDevice/UC66_ConnectHealthDevice_TDS.md` (shared entity/state machine)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.45`

> Viết test trước → chạy → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC68 (Draft) |
| 2026-07-02 | AI Agent — Test Designer (reconciliation) | **Corrected schema reference:** `device_connections` (invented, did not exist) → `health_device_connections` (real, `V1__init_schema.sql` L1115). State transition `CONNECTED → DISCONNECTED` corrected to `ACTIVE → REVOKED`. **Major simplification:** since UC68's ADR-DEVICE-007 no longer calls `ConsentService.revoke()` (UC66's corrected ADR-DEVICE-002 removed the `ConsentGrant` dual-write), all test cases and fixtures referencing consent-revoke transactional behavior (`DISCONNECT-TC-001`, `DISCONNECT-TC-007`, `DISCONNECT-TC-008`) are rewritten as simple single-table UPDATE assertions — the `@Transactional` dual-write rollback scenario (`DISCONNECT-TC-007`) no longer applies and is removed/repurposed. |

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
| **Feature / Gap ID** | `CB-DEVICE-IMP-003` |
| **Module** | `Disconnect Health Device — health.device` |
| **Spec gốc** | `CB-DEVICE-IMP-003` (TDS) |
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
| **Constraint Source** | `CB-DEVICE-IMP-003 §17`, `ADR-DEVICE-007` |
| **Constraints Injected** | C1 (transactional disconnect+revoke), C2 (no delete), C3 (metric history preserved), C4 (ownership), C5 (DeviceDisconnected event) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 (CORRECTED 2026-07-02) | ~~SRS UC-68 flows không đặc tả rõ có cần revoke consent hay không~~ — Bản gốc ADR-DEVICE-007 giả định dual-write vào `ConsentGrant`, dựa trên tiền đề sai (`device_connections.consent_grant_id` FK không tồn tại trên schema thực) | ADR-DEVICE-007 (corrected): disconnect là single-table UPDATE trên `health_device_connections.status='REVOKED'` — KHÔNG gọi `ConsentService` | Test verify `HealthDeviceConnection.status == REVOKED` sau disconnect; KHÔNG còn assertion nào về `ConsentGrant`/`ConsentService` |
| L2 | Không rõ có xóa lịch sử metric khi disconnect hay không | TDS §1 Out-of-scope: KHÔNG xóa | Test verify metric count unchanged trước/sau disconnect |
| L3 | Disconnect trên record đã REVOKED — SRS không đặc tả hành vi | ADR-DEVICE-001 (UC66, corrected): REVOKED là terminal state | Test verify second disconnect call → 409 DEVICE-203, không throw exception khác |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Disconnect Health Device (health.device) bao gồm các layer:
├── Service (DeviceConnectionService.disconnect() — mock IHealthDeviceConnectionRepository + EventPublisher)
├── Controller (@WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL — verify single-table UPDATE persisted)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-68 §3.3.1.45` | Mother disconnects device, stops sync |
| `ADR-DEVICE-007 (corrected)` | Single-table status UPDATE — no ConsentService dependency |
| `ADR-DEVICE-001 (UC66, corrected)` | Terminal state REVOKED, append-only |
| `BR-RBAC` | Ownership required |
| `BR-PRIVACY` | Access revocation timeliness (status=REVOKED read immediately by other flows) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path disconnect: ACTIVE → REVOKED | `disconnect()` | `DISCONNECT-TC-001` |
| TC-COND-002 | Disconnect on already-REVOKED record → 409 DEVICE-203 | `disconnect()` | `DISCONNECT-TC-002` |
| TC-COND-003 | Disconnect on non-existent id → 409 DEVICE-203 | `disconnect()` | `DISCONNECT-TC-003` |
| TC-COND-004 | Disconnect by non-owner → 403 DEVICE-204 | `disconnect()` ownership | `DISCONNECT-TC-004` |
| TC-COND-005 | Metric history preserved (not deleted) after disconnect | Data integrity | `DISCONNECT-TC-005` |
| TC-COND-006 | `DeviceDisconnected` event published exactly once | Event | `DISCONNECT-TC-006` |
| TC-COND-007 (REPURPOSED 2026-07-02) | ~~Transactional rollback: if consent revoke fails~~ → No longer applicable (no second table involved). Repurposed: repository save failure (e.g. `DataAccessException`) propagates cleanly without leaving a partial/inconsistent in-memory state | Error handling | `DISCONNECT-TC-007` |
| TC-COND-008 (REMOVED 2026-07-02) | ~~Disconnect with null `consentGrantId` (legacy record)~~ — `consentGrantId` field no longer exists on `HealthDeviceConnection` (see UC66 ADR-DEVICE-002 corrected) | — | Removed — no longer a valid scenario |
| TC-COND-009 (Integration) | Full disconnect flow via Testcontainers — DB status persisted | End-to-end | `DISCONNECT-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | ACTIVE→REVOKED, REVOKED→(reject) | Core state machine invariant (shared with UC66) |
| Error Guessing | Repository save failure mid-update | Error handling verification |
| Equivalence Partitioning | Owner vs non-owner caller | RBAC coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-DEVICE-CONN-CONNECTED` | DB seed | Reuses `DeviceConnectionTestFactory.makeConnectedDevice()` (UC66 shared factory, `status=ACTIVE`) | Happy path |
| `FX-DEVICE-CONN-DISCONNECTED` | DB seed | Reuses `DeviceConnectionTestFactory.makeDisconnectedDevice()` (UC66 shared factory, `status=REVOKED`) | Already-disconnected negative test |
| `FX-DEVICE-CONN-OTHER-USER` | DB seed | `makeConnectedDevice(c -> c.setUserId(OTHER_MOTHER_USER_ID))` | Ownership negative test |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (ownership) |
| Mobile (Flutter) | ✅ (service/repo) | — | ✅ | ✅ (widget test — disconnect confirmation dialog) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Tái sử dụng `DeviceConnectionTestFactory` từ UC66 (`src/test/java/com/carebridge/backend/health/device/DeviceConnectionTestFactory.java`). Không tạo factory trùng lặp.
>
> **CORRECTED (2026-07-02):** `makeConnectedDeviceWithoutConsent()` REMOVED — `HealthDeviceConnection` (real entity) has no `consentGrantId` field to null out. `makeConnectedDeviceOwnedByOther()` retained, now builds `HealthDeviceConnection`.

```java
// ═══════════════════════════════════════════════════════════
// Bổ sung method vào DeviceConnectionTestFactory.java (đã tạo ở UC66)
// để hỗ trợ UC68 test scenarios — KHÔNG tạo factory mới.
// Targets REAL schema entity HealthDeviceConnection.
// ═══════════════════════════════════════════════════════════

class DeviceConnectionTestFactory {
    // ... (existing methods from UC66: makeConnectedDevice(), makeDisconnectedDevice(),
    //      makeValidRequest(), MOTHER_USER_ID, PARTNER_USER_ID, OTHER_MOTHER_USER_ID) ...

    // NEW for UC68:
    static HealthDeviceConnection makeConnectedDeviceOwnedByOther() {
        return makeConnectedDevice(c -> c.setUserId(OTHER_MOTHER_USER_ID));
    }
}
```

---

### DISCONNECT-TC-001 — Happy path disconnect: status + consent revoked atomically

**Severity:** `CRITICAL`
**Feature Under Test:** `DeviceConnectionService.disconnect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-68 Normal Flow` / `ADR-DEVICE-007 (corrected)`

**Preconditions:** `DeviceConnectionTestFactory.makeConnectedDevice()` (`status=ACTIVE`) returned by `findById()`

**Test Steps:**
1. Arrange: mock repo returns ACTIVE connection
2. Act: `service.disconnect(connectionId, MOTHER_USER_ID)`
3. Assert: saved entity `status=REVOKED`, `updatedAt` advanced to ≥ test start; no interaction with any `ConsentService`/`ConsentGrant` type

**Expected Result (PASS):** Status update occurs; no consent-service interaction attempted.
**Expected Result (FAIL):** Status unchanged, or code attempts to call a `ConsentService` that no longer applies to this flow (Hallucinated Contract — AP-AI-005).

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-002 — Disconnect on already-REVOKED record rejected

**Severity:** `HIGH`
**Feature Under Test:** `DeviceConnectionService.disconnect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-DEVICE-001 (UC66, corrected) — REVOKED is terminal`

**Preconditions:** `makeDisconnectedDevice()` (`status=REVOKED`) returned by `findById()`

**Test Steps:**
1. Arrange: mock repo returns already-REVOKED device
2. Act: `service.disconnect(connectionId, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-203`; `repository.save()` never called

**Expected Result (PASS):** Rejected with 409, no side effects.
**Expected Result (FAIL):** Status re-saved despite already being terminal.

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-003 — Disconnect on non-existent connection id rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `DeviceConnectionService.disconnect()`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** TDS §10 Error Codes

**Test Steps:**
1. Arrange: mock `findById()` returns `Optional.empty()`
2. Act: `service.disconnect(nonExistentId, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-203`

**Expected Result (PASS):** 409 DEVICE-203.
**Expected Result (FAIL):** NullPointerException or wrong error code.

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-004 — Non-owner disconnect attempt rejected (403 DEVICE-204)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `DeviceConnectionService.disconnect()` ownership check
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-RBAC` / TDS §16 Authorization Matrix

**Preconditions:** `makeConnectedDeviceOwnedByOther()` returned by `findById()`; caller is `MOTHER_USER_ID`

**Test Steps:**
1. Arrange: connection owned by `OTHER_MOTHER_USER_ID`
2. Act: `service.disconnect(connectionId, MOTHER_USER_ID)`
3. Assert: throws exception `DEVICE-204` (403); no status change

**Expected Result (PASS = safe):** 403 DEVICE-204.
**Expected Result (FAIL = vulnerability):** Disconnect succeeds on another user's device — cross-user RBAC bypass.

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-005 — Metric history preserved after disconnect

**Severity:** `CRITICAL`
**Legal:** `PDPA — data minimization applies to consent scope, not medical history deletion`
**Feature Under Test:** `DeviceConnectionService.disconnect()` side effects
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java` (unit — verify no metric-deletion call) + `DISCONNECT-TC-INT-001` (integration — verify DB count)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC68 TDS §1 Out-of-scope`

**Test Steps:**
1. Arrange: happy path disconnect setup
2. Act: `service.disconnect(connectionId, MOTHER_USER_ID)`
3. Assert: `MaternalHealthMetricRepository` (or any metric-deleting method) is NEVER invoked by `DeviceConnectionService`

**Expected Result (PASS):** No interaction with metric repository at all.
**Expected Result (FAIL):** Metric records deleted or modified — violates data retention expectation.

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-006 — DeviceDisconnected event published exactly once

**Severity:** `HIGH`
**Feature Under Test:** `DeviceConnectionService.disconnect()` → `ApplicationEventPublisher`
**Test File:** `src/test/java/com/carebridge/backend/health/device/service/DeviceConnectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §7.1 Domain Event Catalog`

**Test Steps:**
1. Arrange: happy path
2. Act: `service.disconnect(connectionId, MOTHER_USER_ID)`
3. Assert: `eventPublisher.publishEvent(any(DeviceDisconnected.class))` called once; payload `deviceConnectionId` matches

**Expected Result (PASS):** Event published once, correct payload.
**Expected Result (FAIL):** Event missing or wrong payload.

**Current Status:** 🔴 Not written

---

### DISCONNECT-TC-007 (REPURPOSED 2026-07-02) — Repository save failure propagates cleanly, no partial state

**Severity:** `HIGH`
**Feature Under Test:** `DeviceConnectionService.disconnect()` error handling
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceConnectionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-DEVICE-007 §Decision (corrected)`

> **Note:** This test case previously verified `@Transactional` rollback between two tables (`health_device_connections` + `consent_grants`) when `ConsentService.revoke()` failed. Since ADR-DEVICE-007 (corrected) removed the `ConsentService` dependency entirely — disconnect is now a single-table UPDATE — the original two-table atomicity scenario no longer exists. This test case is repurposed to verify a single, simpler invariant: if the repository save itself fails (e.g., DB connectivity error), the exception propagates without corrupting in-memory or downstream state.

**Preconditions:** Testcontainers PostgreSQL; mock `healthDeviceConnectionRepository.save(...)` to throw `DataAccessException`

**Test Steps:**
1. Arrange: seed ACTIVE connection; force `repository.save()` to throw
2. Act: call `service.disconnect(connectionId, MOTHER_USER_ID)`, expect exception propagated
3. Assert: query DB directly — `health_device_connections.status` is STILL `ACTIVE` (no partial write occurred, since the single UPDATE statement itself failed atomically)

**Expected Result (PASS):** Exception propagates, DB state unchanged.
**Expected Result (FAIL):** Status shows REVOKED despite save failure, or exception is silently swallowed.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### DISCONNECT-TC-INT-001 — Full disconnect flow via Testcontainers

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH /disconnect → health_device_connections.status`
**Test File:** `src/test/java/com/carebridge/backend/health/device/DeviceConnectionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainer; baseline `V1__init_schema.sql` applied (health_device_connections pre-exists, no UC68 migration required — §5.2)
- Seed: ACTIVE `health_device_connections` row + 2 `maternal_health_metrics` rows referencing it

**Test Steps:**
1. Seed connected device with metrics
2. Call `PATCH /api/v1/health/devices/connections/{id}/disconnect`
3. Assert DB state directly

**Expected Result (PASS):**
- `health_device_connections.status = 'REVOKED'`, `updated_at` advanced
- `maternal_health_metrics` count for that device UNCHANGED (still 2 rows)

**Expected Result (FAIL):** Any of the above not satisfied.

**DB Assertion:**
```java
HealthDeviceConnection record = healthDeviceConnectionRepository.findById(connectionId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(DeviceConnectionStatus.REVOKED);
long metricCount = metricRepository.countBySourceReferenceId(connectionId);
assertThat(metricCount).isEqualTo(2L);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DISCONNECT-TC-001` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-002` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-003` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-004` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-005` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-006` | `DeviceConnectionServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `DISCONNECT-TC-007` | `DeviceConnectionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | repurposed 2026-07-02 — see TC body |
| `DISCONNECT-TC-INT-001` | `DeviceConnectionIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// Add to DeviceConnectionService stub (already exists from UC66 Red Phase)
@Override
public DeviceConnectionResponse disconnect(UUID connectionId, UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `DISCONNECT-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DISCONNECT-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS**
- Log file: chưa tạo (Draft phase)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-DEVICE-IMP-003` đã review và approve
- [ ] UC66 đã implement và deploy (dependency: `health_device_connections` table — đã tồn tại sẵn từ `V1__init_schema.sql` + `DeviceConnectionService.connect()`)
- [x] ~~`ConsentService.revoke()` xác nhận hoạt động đúng~~ — **Không còn cần thiết** (ADR-DEVICE-007 corrected loại bỏ dependency này)

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] `./mvnw verify` (Testcontainers) xanh, đặc biệt `DISCONNECT-TC-007` (error handling, repurposed)
- [ ] Coverage ≥ 80% cho `DeviceConnectionService.disconnect()`
- [ ] Metric history preservation verified (DISCONNECT-TC-005, DISCONNECT-TC-INT-001)
- [ ] Không business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate pass
- [ ] Contract Existence verified
- [ ] Props Isolation — dùng `DeviceConnectionTestFactory` (UC66, mở rộng)
- [ ] Oracle Source ghi rõ cho mọi assert

### Suspension Criteria

- `health_device_connections` không truy cập được trên môi trường test (bảng phải tồn tại từ `V1__init_schema.sql`)

---

## 7. Rollback Plan

```bash
# Không có migration để revert (UC68 không tạo migration mới — health_device_connections đã tồn tại từ V1__init_schema.sql).
git checkout -- src/main/java/com/carebridge/backend/health/device/service/DeviceConnectionService.java
git checkout -- src/main/java/com/carebridge/backend/health/device/controller/DeviceConnectionController.java
git checkout -- src/test/java/com/carebridge/backend/health/device/
kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-DEVICE-007 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume metric deletion behavior không có trong TDS §1 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (status update logic) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import type không tồn tại trong codebase, hoặc mock/verify a `ConsentService` interaction that no longer exists in this flow (see ADR-DEVICE-007 corrected) | ☐ | G-3 |
| AP-DEVICE-007 | Schema Drift | Test hoặc implementation target bảng `device_connections` (không tồn tại) thay vì `health_device_connections` thực | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern → approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
