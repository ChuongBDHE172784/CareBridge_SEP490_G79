# TEST SPECIFICATION — UC-158 Receive Reminder Notification
# Đặc tả Kiểm thử — Nhận Thông báo Nhắc nhở

| Field                  | Value                                          |
|------------------------|------------------------------------------------|
| **Document ID**        | `CB-NOTIF-TEST-001`                            |
| **Version**            | `1.0`                                          |
| **Date**               | `2026-06-26`                                   |
| **Status**             | `Draft`                                        |
| **Document Owner**     | `PhuongNT`                                     |
| **Author**             | `AI Agent`                                     |
| **Reviewed by**        | `[Tech Lead]`                                  |
| **DPO Sign-off**       | `[ ] Pending`                                  |
| **Approved by**        | `[Principal Architect]`                        |
| **Last Review**        | `2026-06-26`                                   |
| **Based on EDS**       | `v2.0`                                         |
| **TDS Reference**      | `CB-NOTIF-IMP-001`                             |
| **Standard**           | `ISO/IEC/IEEE 29119-3:2021`                    |
| **Data Classification**| `Internal`                                     |

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                      |
|------------|-----------------|--------------------------------------------------------|
| 2026-06-26 | AI Agent        | Tạo Test-Spec lần đầu cho UC-158                       |

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

| Field                   | Value                                                           |
|-------------------------|-----------------------------------------------------------------|
| **Feature / UC ID**     | `UC-158`                                                        |
| **Module**              | `notification — ReminderNotification`                           |
| **Spec gốc**            | `CB-NOTIF-IMP-001`                                              |
| **Priority**            | 🟠 P1                                                           |
| **Data Classification** | `Internal`                                                      |
| **Upstream Dependencies**| `care (Reminder), identity (FCM token), notification.preferences` |
| **Downstream Consumers**| `AuditService, AlertService`                                    |

### 1.1 AI Generation Context (CASE 2.0)

| Field                  | Value                                                        |
|------------------------|--------------------------------------------------------------|
| **AI Assisted?**       | `Yes`                                                        |
| **Constraint Source**  | `CB-NOTIF-IMP-001 §17`, `ADR-NOTIF-001/002/003`             |
| **Constraints Injected**| C1 (preference gate), C2 (retry backoff), C3 (record persist) |
| **Model**              | `claude-sonnet-4-6`                                          |
| **Trust Level**        | `T2 → T3 (pending Red Gate)`                                 |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                  | Thực tế                                     | Fix áp dụng trong test                          |
|---|------------------------------------------|---------------------------------------------|-------------------------------------------------|
| L1 | Spec không nêu rõ skip behavior         | skip silently nếu preference disabled       | Test phải verify FCM KHÔNG được gọi khi disabled |
| L2 | Spec không rõ backoff timing            | 0s, 2s, 4s (exponential)                   | Test mock timing để verify attempt order        |
| L3 | Spec không rõ FCM token missing case    | NOTIF-003 nếu không có token               | Test case riêng cho missing FCM token           |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
notification.ReminderNotification bao gồm:
├── Service Layer (ReminderNotificationService — mock FCM + repos)
├── FCM Service (FcmServiceImpl — mock FirebaseMessaging)
└── Integration (Testcontainers PostgreSQL + FCM mock server)
```

### TDS-02 — Test Basis

| Source          | Items Derived                                                  |
|-----------------|----------------------------------------------------------------|
| UC-158          | Gửi notification khi reminder due, preference check, retry    |
| BR-NOTIF-001    | Preference gate — chỉ gửi nếu enabled                        |
| BR-NOTIF-002    | Retry tối đa 3 lần exponential backoff                        |
| BR-NOTIF-003    | Lưu NotificationRecord với status đúng                        |
| BR-NOTIF-004    | referenceId = reminderId                                       |
| ADR-NOTIF-003   | Backoff timing: 0s → 2s → 4s                                  |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID    | Test Condition                                   | Coverage Item                              | Test Cases           |
|-----------------|--------------------------------------------------|--------------------------------------------|----------------------|
| TC-COND-N1-001  | Preference REMINDER enabled → FCM gửi           | `ReminderNotificationService.send()`       | `NOTIF-TC-001`       |
| TC-COND-N1-002  | Preference REMINDER disabled → skip             | `preferenceRepository.isEnabled()`         | `NOTIF-TC-002`       |
| TC-COND-N1-003  | FCM thất bại → retry 3 lần → FAILED             | `FcmServiceImpl.sendWithRetry()`           | `NOTIF-TC-003`       |
| TC-COND-N1-004  | FCM thất bại lần 1, thành công lần 2 → SENT     | `FcmServiceImpl.sendWithRetry()`           | `NOTIF-TC-004`       |
| TC-COND-N1-005  | Không có FCM token → NOTIF-003                   | `FcmService.validateToken()`               | `NOTIF-TC-005`       |
| TC-COND-N1-006  | NotificationRecord lưu đúng referenceId          | `NotificationRecord.referenceId`           | `NOTIF-TC-006`       |
| TC-COND-N1-007  | Integration: full flow DB verify                | Full stack                                 | `NOTIF-TC-INT-001`   |

### TDS-04 — Test Techniques

| Technique               | Applied To                         | Rationale                        |
|-------------------------|------------------------------------|----------------------------------|
| State Transition Testing| NotificationRecord status FSM      | SENT → DELIVERED / SENT → FAILED |
| Error Guessing          | FCM 503/network error scenarios    | Retry logic coverage             |
| Boundary Value Analysis | maxAttempts = 3 (không retry lần 4)| Prevent infinite loop            |

### TDS-05 — Test Data Requirements

| Fixture ID  | Type     | Value / Logic                                        | Mục đích                   |
|-------------|----------|------------------------------------------------------|----------------------------|
| `FX-N1-001` | DB seed  | `{userId: user-001, type: REMINDER, enabled: true}` | Happy path                 |
| `FX-N1-002` | DB seed  | `{userId: user-002, type: REMINDER, enabled: false}` | Skip test                 |
| `FX-N1-003` | Mock     | FCM always returns 503                               | Retry/FAILED test          |
| `FX-N1-004` | Mock     | FCM fails on attempt 1, succeeds on attempt 2        | Partial retry test         |
| `FX-N1-005` | DB seed  | user-003 has no FCM token registered                 | Missing token test         |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class ReminderNotificationTestFactory {
    static ReminderNotification makeValidNotification() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new ReminderNotification.ReminderNotificationBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static ReminderNotification makeValidNotification(Consumer<ReminderNotification> overrides) {
        var entity = makeValidNotification();
        overrides.accept(entity);
        return entity;
    }
}
```

---

### NOTIF-TC-001 — Gửi thành công khi preference enabled

**Severity:** `HIGH`
**Feature Under Test:** `ReminderNotificationService.sendReminderNotification()`
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ReminderNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-001`
**Oracle Source:** `BR-NOTIF-001`, `BR-NOTIF-003`

**Preconditions:**
- FX-N1-001: user-001 có preference REMINDER = enabled
- user-001 có FCM token "fcm-token-001"
- FCM mock trả về success `{messageId: "fcm-msg-001"}`

**Test Steps:**
1. Arrange: mock `preferenceRepository.findByUserIdAndType()` → enabled=true
2. Arrange: mock `fcmService.sendWithRetry()` → FcmResult{success=true, messageId="fcm-msg-001"}
3. Act: `service.sendReminderNotification(reminder-uuid, user-001-uuid)`
4. Assert: `fcmService.sendWithRetry()` được gọi đúng 1 lần
5. Assert: `recordRepository.save()` được gọi với `status = SENT`, `referenceId = reminder-uuid`

**Expected Result (PASS):**
- `fcmService.sendWithRetry()` call count = 1
- Saved record: `status = SENT`, `fcmMessageId = "fcm-msg-001"`, `referenceId = reminder-uuid`

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Preference enabled → FCM gửi thành công
    Given user-001 có REMINDER preference = enabled
    And FCM mock returns success
    When sendReminderNotification(reminder-uuid, user-001-uuid)
    Then fcmService.sendWithRetry() được gọi 1 lần
    And NotificationRecord status = SENT
    And NotificationRecord.referenceId = reminder-uuid
```

---

### NOTIF-TC-002 — Skip gửi khi preference disabled

**Severity:** `HIGH`
**Feature Under Test:** `ReminderNotificationService` — preference gate
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ReminderNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-002`
**Oracle Source:** `BR-NOTIF-001`, `ADR-NOTIF-002`

**Preconditions:**
- FX-N1-002: user-002 có preference REMINDER = disabled

**Test Steps:**
1. Arrange: mock `preferenceRepository` → enabled=false
2. Act: `service.sendReminderNotification(reminder-uuid, user-002-uuid)`
3. Assert: `fcmService.sendWithRetry()` KHÔNG được gọi
4. Assert: `recordRepository.save()` KHÔNG được gọi

**Expected Result (PASS):**
- `fcmService` mock: verifyZeroInteractions()
- `recordRepository` mock: verifyZeroInteractions()

**Current Status:** 🔴 Not written

---

### NOTIF-TC-003 — FCM thất bại 3 lần → status FAILED

**Severity:** `CRITICAL`
**Feature Under Test:** `FcmServiceImpl.sendWithRetry()` + `ReminderNotificationService`
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ReminderNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-003`
**Oracle Source:** `ADR-NOTIF-003`, `BR-NOTIF-002`

**Preconditions:**
- FX-N1-003: FCM mock always throws FirebaseMessagingException

**Test Steps:**
1. Arrange: mock `fcmService.sendWithRetry()` → FcmResult{success=false} (simulates 3 failures)
2. Act: `service.sendReminderNotification(reminder-uuid, user-001-uuid)`
3. Assert: `recordRepository.save()` được gọi với `status = FAILED`, `attemptCount = 3`

**Expected Result (PASS):**
- Saved record: `status = FAILED`, `attemptCount = 3`, `failedAt != null`
- Alert service triggered (optional — verify via event)

**Expected Result (FAIL):**
- `attemptCount < 3` — retry không đủ lần
- Exception uncaught propagated to caller

**Current Status:** 🔴 Not written

```gherkin
  Scenario: FCM thất bại cả 3 lần → FAILED record
    Given FCM mock luôn trả về lỗi 503
    When sendReminderNotification() được gọi
    Then fcmService.sendWithRetry() được gọi với maxAttempts=3
    And NotificationRecord status = FAILED
    And NotificationRecord.attemptCount = 3
    And failedAt != null
```

---

### NOTIF-TC-004 — FCM thất bại lần 1, thành công lần 2

**Severity:** `HIGH`
**Feature Under Test:** `FcmServiceImpl.sendWithRetry()` — partial retry
**Test File:** `src/test/java/com/carebridge/backend/notification/service/FcmServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-004`
**Oracle Source:** `ADR-NOTIF-003`

**Preconditions:**
- FX-N1-004: FCM mock fails on first call, succeeds on second

**Test Steps:**
1. Arrange: mock FirebaseMessaging — first call throws, second call returns messageId
2. Act: `fcmService.sendWithRetry(token, message, 3)`
3. Assert: `FirebaseMessaging.send()` được gọi đúng 2 lần
4. Assert: returned `FcmResult.success = true`

**Expected Result (PASS):**
- `send()` call count = 2
- FcmResult: `{success: true, messageId: "fcm-msg-002"}`

**Current Status:** 🔴 Not written

---

### NOTIF-TC-005 — Không có FCM token → NOTIF-003

**Severity:** `HIGH`
**Feature Under Test:** `ReminderNotificationService` — token validation
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ReminderNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-005`
**Oracle Source:** `BR-NOTIF-005`, error code `NOTIF-003`

**Preconditions:**
- FX-N1-005: user-003 không có FCM token trong DB

**Test Steps:**
1. Arrange: mock user device repository → empty
2. Act: `service.sendReminderNotification(reminder-uuid, user-003-uuid)`
3. Assert: `NotificationRecord` được lưu với `status = FAILED`
   hoặc: exception `NotificationException("NOTIF-003")` được throw (theo design choice)

**Expected Result (PASS):**
- Notification không được gửi qua FCM
- Record lưu với status = FAILED và error message "FCM token not found"

**Current Status:** 🔴 Not written

---

### NOTIF-TC-006 — NotificationRecord lưu đúng referenceId = reminderId

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationRecord.referenceId` mapping
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ReminderNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-006`
**Oracle Source:** `BR-NOTIF-004`

**Test Steps:**
1. Arrange: happy path setup (preference enabled, FCM mock success)
2. Act: `service.sendReminderNotification(UUID.fromString("reminder-uuid-123"), user-001-uuid)`
3. Capture: ArgumentCaptor trên `recordRepository.save()`
4. Assert: `capturedRecord.getReferenceId() = UUID("reminder-uuid-123")`
5. Assert: `capturedRecord.getReferenceType() = "REMINDER"`

**Expected Result (PASS):**
- `referenceId` khớp với `reminderId` đầu vào
- `referenceType = "REMINDER"`

**Current Status:** 🔴 Not written

---

### NOTIF-TC-INT-001 — Integration: full flow → DB verify

**Severity:** `HIGH`
**Feature Under Test:** Full stack notification flow
**Test File:** `src/test/java/com/carebridge/backend/notification/ReminderNotificationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-N1-007`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied
- user-001 với REMINDER preference = enabled và FCM token
- FCM mock server (WireMock) trả về success

**Test Steps:**
1. Call `service.sendReminderNotification(reminderId, userId)` qua full bean context
2. Query DB: `SELECT * FROM notification_records WHERE reference_id = reminderId`
3. Assert: 1 row, `status = 'SENT'`, `reference_type = 'REMINDER'`

**Expected Result (PASS):**
- 1 row trong `notification_records` với đúng fields
- `fcm_message_id` không null

**DB Assertion:**
```sql
SELECT status, reference_id, reference_type, attempt_count
FROM notification_records
WHERE user_id = 'user-001-uuid'
ORDER BY created_at DESC
LIMIT 1;
-- Expected: status='SENT', reference_type='REMINDER', attempt_count=1
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                                              | 🔴 RED confirmed | 🟢 GREEN | 🔵 REFACTOR |
|--------------------|--------------------------------------------------------|-----------------|----------|-------------|
| `NOTIF-TC-001`     | `...notification/service/ReminderNotificationServiceTest.java` | `[ ]` | — | — |
| `NOTIF-TC-002`     | `...notification/service/ReminderNotificationServiceTest.java` | `[ ]` | — | — |
| `NOTIF-TC-003`     | `...notification/service/ReminderNotificationServiceTest.java` | `[ ]` | — | Extract `handleRetryExhausted()` |
| `NOTIF-TC-004`     | `...notification/service/FcmServiceImplTest.java`      | `[ ]` | — | — |
| `NOTIF-TC-005`     | `...notification/service/ReminderNotificationServiceTest.java` | `[ ]` | — | — |
| `NOTIF-TC-006`     | `...notification/service/ReminderNotificationServiceTest.java` | `[ ]` | — | — |
| `NOTIF-TC-INT-001` | `...notification/ReminderNotificationIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol

```java
// ReminderNotificationService.java — Red Stub
@Service
public class ReminderNotificationService implements INotificationService {
    @Override
    public void sendReminderNotification(UUID reminderId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] `CB-NOTIF-IMP-001` TDS approved
- [ ] ADR-NOTIF-001/002/003 Accepted
- [ ] Firebase project configured, service account in env
- [ ] Flyway migration script reviewed

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả 7 test cases xanh
- [ ] NOTIF-TC-003 xanh — retry exhaustion đúng hoạt động
- [ ] Notification preference gate hoạt động (NOTIF-TC-002 xanh)
- [ ] FCM token không được log plaintext
- [ ] Integration test qua Testcontainers xanh

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/notification/
./mvnw flyway:repair  # nếu migration chưa hoàn thành
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern      | Dấu hiệu                              | Check |
|-----------|-------------------|---------------------------------------|-------|
| AP-AI-001 | Unconstrained Gen | TC không reference BR/ADR             | ☐     |
| AP-AI-002 | Green-from-Birth  | Test PASS với empty stub              | ☐     |
| AP-AI-003 | Implicit Decision | Hardcode retry count mà không có ADR  | ☐     |
| AP-AI-005 | Hallucinated Contract | Import class không tồn tại         | ☐     |
