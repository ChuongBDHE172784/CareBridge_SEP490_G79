# TEST SPECIFICATION — UC-161 Receive Emergency Alert
# Đặc tả Kiểm thử — Nhận Cảnh báo Khẩn cấp

| Field                  | Value                                               |
|------------------------|-----------------------------------------------------|
| **Document ID**        | `CB-NOTIF-TEST-004`                                 |
| **Version**            | `1.0`                                               |
| **Date**               | `2026-06-26`                                        |
| **Status**             | `Draft`                                             |
| **Document Owner**     | `PhuongNT`                                          |
| **Author**             | `AI Agent`                                          |
| **Reviewed by**        | `[Tech Lead]`                                       |
| **DPO Sign-off**       | `[ ] Pending`                                       |
| **Approved by**        | `[Principal Architect]`                             |
| **Last Review**        | `2026-06-26`                                        |
| **Based on EDS**       | `v2.0`                                              |
| **TDS Reference**      | `CB-NOTIF-IMP-004`                                  |
| **Standard**           | `ISO/IEC/IEEE 29119-3:2021`                         |
| **Data Classification**| `Sensitive-PII`                                     |
| **Priority**           | 🔴 CRITICAL — Safety-critical module                 |

> **CRITICAL NOTICE:** Đây là test spec cho safety-critical module. Mọi test case PHẢI được review bởi Principal Architect VÀ DPO trước khi deploy lên production. Failure mode trong production có thể ảnh hưởng đến tính mạng người dùng.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                |
|------------|-----------------|--------------------------------------------------|
| 2026-06-26 | AI Agent        | Tạo Test-Spec lần đầu cho UC-161 Emergency Alert |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                   | Value                                                                  |
|-------------------------|------------------------------------------------------------------------|
| **Feature / UC ID**     | `UC-161`                                                               |
| **Module**              | `notification — EmergencyAlert`                                        |
| **Spec gốc**            | `CB-NOTIF-IMP-004`                                                     |
| **Priority**            | 🔴 P0 CRITICAL                                                         |
| **Data Classification** | `Sensitive-PII` (location coordinates)                                 |
| **Compliance Scope**    | `GDPR Art. 6.1(d) — vital interests`                                  |
| **Upstream Dependencies**| `safety (SafetyEvent/IMU), identity (FamilyMember), location (TrackAsia)` |
| **Downstream Consumers**| `AuditService, on-call AlertService, SmsFallback (placeholder)`        |

### 1.1 AI Generation Context (CASE 2.0)

| Field                   | Value                                                                  |
|-------------------------|------------------------------------------------------------------------|
| **AI Assisted?**        | `Yes`                                                                  |
| **Constraint Source**   | `CB-NOTIF-IMP-004 §17`, `ADR-EMERG-001 through 005`                  |
| **Constraints Injected**| C1 (all family members), C2 (high-priority FCM), C3 (SMS fallback), C4 (UUID v4), C5 (no location in logs) |
| **Model**               | `claude-sonnet-4-6`                                                    |
| **Trust Level**         | `T2 → T3 (pending Red Gate) — REQUIRES HUMAN REVIEW before T5`       |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                      | Thực tế                                         | Fix áp dụng trong test                             |
|---|---------------------------------------------|--------------------------------------------------|----------------------------------------------------|
| L1 | Spec không rõ "all family members"         | ADR-EMERG-002: query VIEW_EMERGENCY_ALERT permission | Test verify permission-based filtering         |
| L2 | Spec không rõ idempotency                  | safetyEventId = idempotency key → 409 on duplicate | Test duplicate safetyEventId → 409           |
| L3 | Spec không rõ FCM priority                 | ADR-EMERG-001: HIGH android + apns-priority=10  | Test FCM message config                           |
| L4 | Spec không rõ null location case           | ADR-EMERG-005: null-safe nếu không có location  | Test với user không có location data             |
| L5 | SMS fallback là "placeholder"              | ISmsFallbackService: log + mark attempted        | Test verify fallback called, not actual SMS       |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
notification.EmergencyAlert bao gồm:
├── Controller (SafetyEventController — input validation, idempotency)
├── Service (EmergencyAlertService — broadcast, fallback, alert status)
├── FCM High-Priority (FcmHighPriorityServiceImpl — message config)
├── SMS Fallback (SmsFallbackServiceImpl — placeholder)
└── Integration (Testcontainers PostgreSQL + FCM WireMock)
```

### TDS-02 — Test Basis

| Source         | Items Derived                                                          |
|----------------|------------------------------------------------------------------------|
| UC-161         | Broadcast, high-priority FCM, SMS fallback, location in payload       |
| BR-EMERG-001   | FCM HIGH priority + APNs critical                                     |
| BR-EMERG-002   | Payload: userId, lat/lng, timestamp, safetyEventId                    |
| BR-EMERG-003   | All family members with VIEW_EMERGENCY_ALERT                          |
| BR-EMERG-004   | FCM failure → SMS fallback                                            |
| BR-EMERG-005   | safetyEventId = UUID v4, unique                                       |
| BR-EMERG-006   | Location from TrackAsia (last known)                                  |
| GDPR Art.6.1(d)| Location PII handling — vital interests                               |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID    | Test Condition                                            | Coverage Item                                    | Test Cases           |
|-----------------|-----------------------------------------------------------|--------------------------------------------------|----------------------|
| TC-COND-EA-001  | Broadcast đến ALL authorized family members              | `resolveAuthorizedFamilyMembers()`               | `EMERG-TC-001`       |
| TC-COND-EA-002  | FCM fail → SMS fallback trigger                          | `triggerFallback()` + `SmsFallbackService`       | `EMERG-TC-002`       |
| TC-COND-EA-003  | FCM message là HIGH priority (Android + APNs)            | `FcmHighPriorityServiceImpl.buildMessage()`      | `EMERG-TC-003`       |
| TC-COND-EA-004  | safetyEventId UUID v4 validation                         | `SafetyEventController` + `@Valid`               | `EMERG-TC-004`       |
| TC-COND-EA-005  | Duplicate safetyEventId → 409 idempotency               | Idempotency check in service/controller          | `EMERG-TC-005`       |
| TC-COND-EA-006  | Payload chứa đúng location data                          | `buildPayload()` + `LocationService`             | `EMERG-TC-006`       |
| TC-COND-EA-007  | Location coordinates KHÔNG trong app logs (security)    | Log filtering / no-log on PII fields             | `EMERG-TC-007`       |
| TC-COND-EA-008  | Null-safe nếu không có last known location              | `buildPayload()` null handling                   | `EMERG-TC-008`       |
| TC-COND-EA-009  | alertStatus = PARTIAL nếu một số FCM fail               | `updateAlertStatus()` logic                      | `EMERG-TC-009`       |
| TC-COND-EA-010  | Integration: POST safety-event → DB records verify      | Full stack                                       | `EMERG-TC-INT-001`   |

### TDS-04 — Test Techniques

| Technique                  | Applied To                                   | Rationale                             |
|---------------------------|----------------------------------------------|---------------------------------------|
| Equivalence Partitioning  | FCM success / fail per recipient             | Partial failure → PARTIAL status      |
| Boundary Value Analysis   | 0 family members → NOTIF-014                 | Empty recipient list edge case        |
| Security Testing          | Location PII in logs                         | GDPR Art. 25 — no PII in logs        |
| State Transition Testing  | AlertStatus: PENDING→SENT/PARTIAL/FAILED     | Complete FSM coverage                 |
| Error Guessing            | Duplicate safetyEventId (IMU may send twice) | Common edge case for IMU sensors      |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type     | Value / Logic                                                                   | Mục đích                          |
|--------------|----------|---------------------------------------------------------------------------------|-----------------------------------|
| `FX-EA-001`  | DB seed  | user-001 với 3 family members có VIEW_EMERGENCY_ALERT                          | Happy path broadcast             |
| `FX-EA-002`  | DB seed  | user-002 với 2 family members: fm-A (FCM success), fm-B (FCM fail)            | Partial failure + fallback test  |
| `FX-EA-003`  | DB seed  | safetyEventId = "550e8400-e29b-41d4-a716-446655440000" đã tồn tại trong DB   | Idempotency test                 |
| `FX-EA-004`  | DB seed  | user-003 không có user_last_locations record                                   | Null location test               |
| `FX-EA-005`  | Mock     | FCM always fails                                                                | All-FAILED + SMS fallback test   |
| `FX-EA-006`  | DB seed  | user-004 không có family members                                                | NOTIF-014 test                  |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class EmergencyAlertTestFactory {
    static EmergencyAlert makeValidAlert() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new EmergencyAlert.EmergencyAlertBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static EmergencyAlert makeValidAlert(Consumer<EmergencyAlert> overrides) {
        var entity = makeValidAlert();
        overrides.accept(entity);
        return entity;
    }
}
```

> **IMPORTANT:** Test data phải là `SYNTHETIC`. KHÔNG ĐƯỢC dùng tọa độ thật của người dùng thật trong test.

---

### EMERG-TC-001 — Broadcast thành công đến tất cả authorized family members

**Severity:** `CRITICAL`
**Feature Under Test:** `EmergencyAlertService.broadcastAlert()` — full broadcast
**Test File:** `src/test/java/com/carebridge/backend/notification/service/EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-001`
**Oracle Source:** `ADR-EMERG-002`, `BR-EMERG-003`

**Preconditions:**
- FX-EA-001: user-001 với 3 family members (fm-001, fm-002, fm-003)
- Tất cả có VIEW_EMERGENCY_ALERT permission và FCM tokens
- FCM WireMock returns success
- user-001 có last known location: lat=10.1, lng=106.7

**Test Steps:**
1. Arrange: mock familyMemberRepository → 3 members; FCM → success; location → {10.1, 106.7}
2. Act: `service.broadcastAlert(safetyEventId)`
3. Assert: `fcmHighPriorityService.sendCritical()` được gọi đúng 3 lần
4. Assert: 3 `EmergencyAlertRecord` được lưu với `status=SENT`
5. Assert: `SafetyEvent.alertStatus = SENT`

**Expected Result (PASS):**
- FCM call count = 3
- All records: `status = SENT`, `fallbackTriggered = false`
- SafetyEvent: `alertStatus = SENT`

**Expected Result (FAIL):**
- FCM call count < 3 → một số family members không nhận được alert
- AlertStatus sai

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Broadcast thành công đến 3 family members
    Given user-001 có 3 authorized family members
    And FCM mock returns success
    When broadcastAlert(safetyEventId)
    Then fcmHighPriorityService.sendCritical() được gọi 3 lần
    And 3 EmergencyAlertRecords với status=SENT
    And SafetyEvent.alertStatus = SENT
```

---

### EMERG-TC-002 — FCM fail cho 1 member → SMS fallback

**Severity:** `CRITICAL`
**Feature Under Test:** `EmergencyAlertService.triggerFallback()`
**Test File:** `...EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-002`
**Oracle Source:** `ADR-EMERG-003`, `BR-EMERG-004`, `BR-EMERG-007`

**Preconditions:**
- FX-EA-002: 2 family members; FCM success for fm-A, fail for fm-B
- fm-B có phoneNumber "0901234567"

**Test Steps:**
1. Arrange: FCM mock — success for fm-A token, fail for fm-B token
2. Act: `service.broadcastAlert(safetyEventId)`
3. Assert: `smsFallbackService.sendFallback("0901234567", any)` được gọi 1 lần
4. Assert: `EmergencyAlertRecord[fm-B].fallbackTriggered = true`
5. Assert: `EmergencyAlertRecord[fm-B].status = FAILED`
6. Assert: `SafetyEvent.alertStatus = PARTIAL`

**Expected Result (PASS):**
- SMS fallback called once for fm-B
- fm-A record: SENT; fm-B record: FAILED + fallbackTriggered=true
- alertStatus = PARTIAL

**Expected Result (FAIL = Safety Risk):**
- SMS fallback NOT called → family member completely missed alert

**Current Status:** 🔴 Not written

```gherkin
  Scenario: FCM fail cho fm-B → SMS fallback trigger
    Given 2 family members: fm-A (FCM ok), fm-B (FCM fail)
    When broadcastAlert(safetyEventId)
    Then smsFallbackService.sendFallback() được gọi cho fm-B
    And EmergencyAlertRecord[fm-B].fallbackTriggered = true
    And SafetyEvent.alertStatus = PARTIAL
```

---

### EMERG-TC-003 — FCM message có HIGH priority config

**Severity:** `CRITICAL`
**Feature Under Test:** `FcmHighPriorityServiceImpl.buildMessage()` — priority config
**Test File:** `src/test/java/com/carebridge/backend/notification/service/FcmHighPriorityServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-003`
**Oracle Source:** `ADR-EMERG-001`, `BR-EMERG-001`

**Test Steps:**
1. Arrange: capture `FirebaseMessaging.send()` argument
2. Act: `fcmHighPriorityService.sendCritical(token, payload)`
3. Assert: `message.androidConfig.priority = HIGH`
4. Assert: `message.apnsConfig.headers["apns-priority"] = "10"`

**Expected Result (PASS):**
- AndroidConfig priority = HIGH
- APNs priority header = "10"

**Expected Result (FAIL = Alert may not wake up device):**
- Standard priority used → alert ignored on DND devices

**Current Status:** 🔴 Not written

---

### EMERG-TC-004 — safetyEventId không hợp lệ → 400

**Severity:** `HIGH`
**Feature Under Test:** `SafetyEventController` — UUID v4 validation
**Test File:** `src/test/java/com/carebridge/backend/notification/controller/SafetyEventControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-004`
**Oracle Source:** `ADR-EMERG-004`, `BR-EMERG-005`, error code `NOTIF-012`

**Test Steps (parametrized):**

| Input safetyEventId        | Expected HTTP | Expected Code |
|----------------------------|---------------|---------------|
| `"NOT-A-UUID"`             | 400           | `NOTIF-012`   |
| `"12345"`                  | 400           | `NOTIF-012`   |
| `""` (empty)               | 400           | `NOTIF-012`   |
| Valid UUID v4              | 202           | (success)     |

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Invalid safetyEventId → 400
    When POST /api/v1/safety-events với safetyEventId = "NOT-A-UUID"
    Then HTTP 400
    And error.code = "NOTIF-012"
    And broadcastAlert() KHÔNG được gọi
```

---

### EMERG-TC-005 — Duplicate safetyEventId → 409 (Idempotency)

**Severity:** `CRITICAL`
**Feature Under Test:** Idempotency check
**Test File:** `...SafetyEventControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-005`
**Oracle Source:** `ADR-EMERG-004`, `BR-EMERG-005`, error code `NOTIF-013`

**Preconditions:**
- FX-EA-003: safetyEventId "550e8400-..." đã tồn tại trong safety_events table

**Test Steps:**
1. Arrange: mock safetyEventRepository → findById returns existing event
2. Act: `POST /api/v1/safety-events` với safetyEventId = "550e8400-..."
3. Assert: HTTP 409; error code `NOTIF-013`
4. Assert: `broadcastAlert()` KHÔNG được gọi lần 2

**Expected Result (PASS):**
- HTTP 409
- Không có duplicate notification gửi đi

**Expected Result (FAIL = Alert spam):**
- HTTP 202 lần 2 → duplicate alerts sent to family members

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Duplicate safetyEventId → 409 không gửi lại
    Given safety event uuid-001 đã được xử lý
    When POST /api/v1/safety-events với cùng safetyEventId
    Then HTTP 409
    And response.error.code = "NOTIF-013"
    And broadcastAlert() KHÔNG được gọi
```

---

### EMERG-TC-006 — Payload chứa đúng location data

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyAlertService.buildPayload()` — location inclusion
**Test File:** `...EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-006`
**Oracle Source:** `BR-EMERG-002`, `ADR-EMERG-005`

**Test Steps:**
1. Arrange: LocationService mock → `{lat: 10.123456, lng: 106.789012}` for user-001
2. Arrange: capture `fcmHighPriorityService.sendCritical()` argument
3. Act: `service.broadcastAlert(safetyEventId)`
4. Assert: `capturedPayload.latitude = 10.123456`
5. Assert: `capturedPayload.longitude = 106.789012`
6. Assert: `capturedPayload.safetyEventId` khớp với input

**Current Status:** 🔴 Not written

---

### EMERG-TC-007 — Location coordinates KHÔNG trong app logs (SECURITY/GDPR)

**Severity:** `CRITICAL`
**CWE:** `CWE-312 — Cleartext Storage of Sensitive Information`
**Legal:** `GDPR Art. 25 — Privacy by Design`
**Feature Under Test:** Log output filtering trong `EmergencyAlertService`
**Test File:** `...EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-007`
**Oracle Source:** `ADR-EMERG-005`, `C5` trong Constraint Block

**Test Steps:**
1. Arrange: setup ListAppender để capture log output
2. Arrange: location = {lat: 10.123456, lng: 106.789012}
3. Act: `service.broadcastAlert(safetyEventId)` — full service call
4. Assert: KHÔNG có log message nào chứa "10.123456" hoặc "106.789012"
5. Assert: KHÔNG có log message nào chứa "latitude" hoặc "longitude" kèm giá trị

**Expected Result (PASS):**
- 0 log messages contain precise coordinates

**Expected Result (FAIL = GDPR violation):**
- Coordinates appear in logs → PII exposure to log aggregation systems

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Location coordinates không xuất hiện trong logs
    Given location = {lat: 10.123456, lng: 106.789012}
    When broadcastAlert() được thực thi đầy đủ
    Then captured log output KHÔNG chứa "10.123456"
    And captured log output KHÔNG chứa "106.789012"
```

---

### EMERG-TC-008 — Null location: broadcast vẫn gửi được (null-safe)

**Severity:** `HIGH`
**Feature Under Test:** `buildPayload()` null-safe location handling
**Test File:** `...EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-008`
**Oracle Source:** `ADR-EMERG-005`

**Preconditions:**
- FX-EA-004: user-003 không có user_last_locations record

**Test Steps:**
1. Arrange: LocationService mock → returns `Optional.empty()` for user-003
2. Act: `service.broadcastAlert(safetyEventId)` với user-003
3. Assert: `broadcastAlert()` completes without NullPointerException
4. Assert: FCM được gọi; payload.latitude = null, payload.longitude = null
5. Assert: Alert records lưu với status=SENT

**Expected Result (PASS):**
- Broadcast succeeds even without location
- Payload: `{latitude: null, longitude: null}` — family members still receive alert

**Current Status:** 🔴 Not written

---

### EMERG-TC-009 — alertStatus = PARTIAL khi một số FCM fail

**Severity:** `HIGH`
**Feature Under Test:** `updateAlertStatus()` logic — PARTIAL determination
**Test File:** `...EmergencyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-009`
**Oracle Source:** `SafetyEvent.AlertStatus` FSM

**Preconditions:**
- 3 family members: fm-001 (FCM success), fm-002 (FCM fail), fm-003 (FCM success)

**Test Steps:**
1. Arrange: FCM mock — success for fm-001 and fm-003, fail for fm-002
2. Act: `service.broadcastAlert(safetyEventId)`
3. Assert: `SafetyEvent.alertStatus = PARTIAL` (not SENT, not FAILED)
4. Assert: `smsFallbackService.sendFallback()` gọi 1 lần cho fm-002

**Expected Result (PASS):**
- alertStatus = PARTIAL
- 2/3 records SENT, 1/3 FAILED + fallbackTriggered=true

**Current Status:** 🔴 Not written

---

### EMERG-TC-INT-001 — Integration: POST safety-event → DB records verify

**Severity:** `CRITICAL`
**Feature Under Test:** Full stack: HTTP request → DB records
**Test File:** `src/test/java/com/carebridge/backend/notification/EmergencyAlertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-EA-010`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migrations applied (safety_events, emergency_alert_records, user_last_locations tables)
- user-001 với 2 authorized family members
- user-001 có last known location in user_last_locations
- FCM WireMock server returns success

**Test Steps:**
1. `POST /api/v1/safety-events` với FALL_DETECTED event và valid UUID v4
2. Assert HTTP 202 Accepted
3. DB: `SELECT * FROM safety_events WHERE id = safetyEventId` → 1 row, alertStatus in ('SENT','PARTIAL')
4. DB: `SELECT COUNT(*) FROM emergency_alert_records WHERE safety_event_id = safetyEventId` = 2
5. DB: All records status = 'SENT'
6. Log check: `grep "10\." carebridge.log` → no matches (GDPR)

**Expected Result (PASS):**
- 1 safety_event row
- 2 emergency_alert_record rows với status='SENT'
- alertStatus = 'SENT'
- No location coordinates in application logs

**DB Assertion:**
```sql
-- Verify safety event processed
SELECT id, alert_status, processed_at
FROM safety_events
WHERE id = 'safety-event-uuid';
-- Expected: alert_status in ('SENT', 'PARTIAL'), processed_at != null

-- Verify alert records
SELECT family_member_id, status, fallback_triggered
FROM emergency_alert_records
WHERE safety_event_id = 'safety-event-uuid';
-- Expected: 2 rows, status='SENT', fallback_triggered=false
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                                                    | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR                      |
|--------------------|--------------------------------------------------------------|--------|----------|----------------------------------|
| `EMERG-TC-001`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | Extract `broadcastToMember()`    |
| `EMERG-TC-002`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | Extract `triggerFallback()`      |
| `EMERG-TC-003`     | `...FcmHighPriorityServiceImplTest.java`                     | `[ ]`  | —        | Extract `buildHighPriorityMessage()` |
| `EMERG-TC-004`     | `...SafetyEventControllerTest.java`                          | `[ ]`  | —        | —                                |
| `EMERG-TC-005`     | `...SafetyEventControllerTest.java`                          | `[ ]`  | —        | —                                |
| `EMERG-TC-006`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | Extract `buildPayload()` pure fn |
| `EMERG-TC-007`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | CRITICAL — no shortcuts          |
| `EMERG-TC-008`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | —                                |
| `EMERG-TC-009`     | `...EmergencyAlertServiceTest.java`                          | `[ ]`  | —        | Extract `determineAlertStatus()` |
| `EMERG-TC-INT-001` | `...EmergencyAlertIntegrationTest.java`                      | `[ ]`  | —        | —                                |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// EmergencyAlertService.java — Red Phase Stub
@Service
public class EmergencyAlertService implements IEmergencyAlertService {
    @Override
    public void broadcastAlert(UUID safetyEventId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// FcmHighPriorityServiceImpl.java — Red Phase Stub
@Service
public class FcmHighPriorityServiceImpl implements IFcmHighPriorityService {
    @Override
    public FcmResult sendCritical(String fcmToken, EmergencyAlertPayload payload) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID           | Stub Result              | Expected  | Actual          | Root Cause (nếu PASS bất thường) |
|-----------------|--------------------------|-----------|-----------------|----------------------------------|
| `EMERG-TC-001`  | UnsupportedOperation     | 🔴 FAIL   | ☐ FAIL ☐ PASS   | —                                |
| `EMERG-TC-002`  | UnsupportedOperation     | 🔴 FAIL   | ☐ FAIL ☐ PASS   | —                                |
| `EMERG-TC-003`  | UnsupportedOperation     | 🔴 FAIL   | ☐ FAIL ☐ PASS   | —                                |
| `EMERG-TC-007`  | UnsupportedOperation     | 🔴 FAIL   | ☐ FAIL ☐ PASS   | NOTE: TC-007 may PASS if logging not yet implemented |

---

## 6. Entry / Exit Criteria

### Entry Criteria — CRITICAL MODULE

- [ ] `CB-NOTIF-IMP-004` TDS approved bởi **Principal Architect**
- [ ] **DPO sign-off** đã nhận (Sensitive-PII + location data)
- [ ] ADR-EMERG-001 đến ADR-EMERG-005 đã Accepted
- [ ] APNs critical alert entitlement confirmed (hoặc fallback plan)
- [ ] Threat model cho location data sharing đã được review
- [ ] Flyway migration scripts reviewed bởi DBA
- [ ] Test fixtures đã được chuẩn bị (FX-EA-001 đến FX-EA-006)

### Exit Criteria (DoD) — CRITICAL MODULE

- [ ] **Tất cả 10 test cases xanh** (không cho phép skip bất kỳ test nào)
- [ ] **EMERG-TC-007** (location PII log test) PHẢI xanh — mandatory security gate
- [ ] **EMERG-TC-002** (SMS fallback) PHẢI xanh — mandatory safety gate
- [ ] **EMERG-TC-003** (FCM HIGH priority) PHẢI xanh — mandatory safety gate
- [ ] **EMERG-TC-005** (idempotency) PHẢI xanh — prevent alert spam
- [ ] Integration test (EMERG-TC-INT-001) xanh qua Testcontainers
- [ ] Security scan: location coordinates NOT in application logs
- [ ] Load test: 100 concurrent alerts (1 user × 5 family members each) — p99 < 10s
- [ ] Principal Architect review production deployment plan

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả 10 tests FAIL với stub
- [ ] Không có production PII trong test fixtures

### Suspension Criteria

- FCM HIGH priority không hoạt động trên test devices → escalate to Principal Architect
- APNs entitlement chưa được Apple approve → implement fallback standard priority + document risk
- SMS provider không available → confirm placeholder strategy with DPO

---

## 7. Rollback Plan

```bash
# CRITICAL: Rollback phải được thực hiện NGAY LẬP TỨC nếu:
# - Location data leak phát hiện
# - Duplicate alerts gửi đến family members
# - Alert hoàn toàn không gửi được

# Bước 1: Disable module bằng feature flag
# (Set FEATURE_EMERGENCY_ALERT=false → restart)

# Bước 2: Rollback deployment
kubectl rollout undo deployment/carebridge-api

# Bước 3: Verify rollback
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/actuator/health

# Bước 4: Thông báo DPO ngay nếu location data bị leak
# (GDPR Art. 33 — 72 giờ window từ khi phát hiện)

# Bước 5: Rollback DB (chỉ khi bảng mới, KHÔNG có production data)
# psql -c "DROP TABLE IF EXISTS emergency_alert_records;"
# psql -c "DROP TABLE IF EXISTS safety_events;"
# psql -c "DROP TABLE IF EXISTS user_last_locations;"
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern           | Dấu hiệu trong Test Spec                        | Check | Gate chặn |
|-----------|------------------------|-------------------------------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Gen      | TC không reference ADR-EMERG-xxx               | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth       | TC-007 (log test) PASS với empty stub          | ☐     | G-2 ★     |
| AP-AI-003 | Implicit Decision      | FCM HIGH priority không có ADR reference       | ☐     | G-1       |
| AP-AI-004 | Layer Violation        | Location fetch logic trong controller          | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract  | Import real SMS service (not placeholder)      | ☐     | G-3       |

**Critical Anti-Pattern for Safety Module:**
- AP-SAFETY-001: Test skip hoặc `@Disabled` trên EMERG-TC-002 (SMS fallback) → **NEVER allowed**
- AP-SAFETY-002: Test mock SafetyEvent outcome but not verify DB state → reject (must verify actual persistence)

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] DPO review completed → approved for implementation
- [ ] Principal Architect signed off → cleared for production deployment

---

## PHỤ LỤC

### A. Test Environment Requirements

| Requirement             | Minimum                       | Note                                          |
|-------------------------|-------------------------------|-----------------------------------------------|
| PostgreSQL              | 15+ (Testcontainers)          | Cho integration tests                         |
| FCM mock server         | WireMock                      | Simulate FCM responses                        |
| Java heap               | 512MB+                        | Testcontainers cần thêm memory               |
| Network                 | Outbound blocked              | Unit tests không cần network                  |

### B. Synthetic Test Data — Location Coordinates

Tất cả test coordinates PHẢI là synthetic (không có thực):
- `lat: 0.0, lng: 0.0` — null island (ocean)
- `lat: 10.123456, lng: 106.789012` — synthetic (không là địa chỉ thật)

KHÔNG ĐƯỢC dùng tọa độ của địa điểm thật (trường học, bệnh viện, nhà riêng) trong test data.
