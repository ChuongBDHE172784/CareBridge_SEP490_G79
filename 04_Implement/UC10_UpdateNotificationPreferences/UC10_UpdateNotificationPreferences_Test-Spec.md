# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Đặc tả Kiểm thử Hướng Phát triển — UC-10 Update Notification Preferences

**Document ID:** `CB-NOTIF-IMP-010-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC10_UpdateNotificationPreferences/UC10_UpdateNotificationPreferences_TDS.md` (`CB-NOTIF-IMP-010`)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.1.1.10
- `ADR-010-001`, `ADR-010-002`

> **Quy ước TDD:** Viết test → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `.\mvnw.cmd test` chưa xanh.
> Không dùng PII thật — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                                    |
| ---------- | --------------- | -------------------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-10 Update Notification Preferences          |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                     |
| ------------------------- | ------------------------------------------------------------------------- |
| **Feature / Gap ID**      | `UC-10`                                                                   |
| **Module**                | `UpdateNotificationPreferences — Bounded Context: notification`           |
| **Spec gốc**              | `CB-NOTIF-IMP-010`                                                        |
| **Priority**              | 🟠 P1                                                                     |
| **Sprint**                | `S2 (2026-07-11 → 2026-07-25)`                                            |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                   |
| **Data Classification**   | `PII`                                                                     |
| **Compliance Scope**      | `PDPA (Luật 91/2025 Điều 14)`, `GDPR Art. 5.1, Art. 7`                   |
| **Upstream Dependencies** | `security (JWT Auth)`, `identity (User)`, `firebase (FCM integration)`   |
| **Downstream Consumers**  | `notification-sender`, `reminder`, `community`, `emergency`               |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                       |
| ------------------------ | ----------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                       |
| **Constraint Source**    | `CB-NOTIF-IMP-010 §17`, `ADR-010-001`, `ADR-010-002`       |
| **Constraints Injected** | C1 (UPSERT idempotent), C2 (own preferences only), C4 (FCM token validation) |
| **Model**                | `Claude Sonnet 4.6`                                         |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                               |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                               | Thực tế (schema / policy)                                     | Fix áp dụng trong test                                  |
| - | ----------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------- |
| L1 | SRS không specify UPSERT vs INSERT behavior          | ADR-010-001: UPSERT — idempotent operation                    | Test gọi update 2 lần với cùng input → kết quả giống nhau |
| L2 | Không rõ default state khi user mới đăng ký          | ADR-010-001: Tất cả preferences mặc định là `enabled=true`   | Test: GET khi chưa có preferences → trả về defaults   |
| L3 | SRS không specify partial vs full update             | TDS §8: `PUT /notification-preferences` là full replacement cho list được gửi | Test: chỉ gửi 1 preference item → chỉ item đó được update |
| L4 | FCM token format không được validate trong SRS       | ADR-010-002: max 512 ký tự, @NotBlank                        | Test: blank token → 400 validation error               |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UpdateNotificationPreferences bao gồm:
├── Domain (NotificationChannel, NotificationCategory enums)
├── Service (NotificationPreferenceServiceImpl — mock repositories)
├── Controller (NotificationPreferenceController — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL)

Out of scope:
├── FCM message delivery (tested trong UC-11, UC-158)
├── Reminder/Emergency notification triggers
```

### TDS-02 — Test Basis

| Source                  | Items Derived                                           |
| ----------------------- | ------------------------------------------------------- |
| `SRS UC-10 §3.1.1.10`   | Configure notification channels and categories          |
| `ADR-010-001`           | UPSERT behavior, notification_preferences table structure |
| `ADR-010-002`           | Device token storage, FCM token validation              |
| `BR-NOTIF-OWN`          | User chỉ update preferences của chính mình             |
| `BR-NOTIF-AUDIT`        | Audit log bắt buộc cho mọi preference change           |
| `CB-NOTIF-IMP-010 §10`  | Error codes NOTIF-001, NOTIF-002, NOTIF-003            |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID   | Test Condition                                     | Coverage Item                          | Test Cases           |
| -------------- | -------------------------------------------------- | -------------------------------------- | -------------------- |
| TC-COND-10-01  | GET preferences khi chưa có → trả về defaults     | `getPreferences()` với empty DB        | NOTIF-TC-010-001     |
| TC-COND-10-02  | PUT preferences hợp lệ → upsert thành công        | `updatePreferences()` happy path       | NOTIF-TC-010-002     |
| TC-COND-10-03  | PUT preferences với channel không hợp lệ          | Enum validation                        | NOTIF-TC-010-003     |
| TC-COND-10-04  | PUT device token hợp lệ → upsert device token     | `registerDeviceToken()` happy path     | NOTIF-TC-010-004     |
| TC-COND-10-05  | PUT device token rỗng → 400                        | `@NotBlank` validation                 | NOTIF-TC-010-005     |
| TC-COND-10-06  | PUT là idempotent (gọi 2 lần cùng input)           | UPSERT behavior                        | NOTIF-TC-010-006     |
| TC-COND-10-07  | Không có JWT → 401                                 | Spring Security                        | NOTIF-TC-010-007     |
| TC-COND-10-08  | Integration: DB state đúng sau update              | Full flow                              | NOTIF-TC-010-INT-001 |

### TDS-04 — Test Techniques

| Technique                 | Applied To                   | Rationale                             |
| ------------------------- | ---------------------------- | ------------------------------------- |
| Equivalence Partitioning  | channel/category enum values | Valid vs invalid enum values          |
| Boundary Value Analysis   | FCM token length (1, 512, 513) | Max 512 constraint                  |
| Idempotency Testing       | PUT preferences              | UPSERT must be safe to call multiple times |
| State Transition Testing  | preference enabled/disabled   | Toggle behavior                       |

### TDS-05 — Test Data Requirements

| Fixture ID | Type   | Value / Logic                                          | Mục đích                  |
| ---------- | ------ | ------------------------------------------------------ | ------------------------- |
| `FX-10-01` | JWT    | Valid JWT for user "00000000-0000-0000-0000-000000000010" | Auth context             |
| `FX-10-02` | DB seed | 2 notification_preferences rows: PUSH/REMINDER=true, EMAIL/COMMUNITY=false | Existing prefs test |
| `FX-10-03` | Request | `[{channel:PUSH, category:REMINDER, enabled:false}]` | Toggle test              |
| `FX-10-04` | Request | `{token:"fcmToken123", platform:ANDROID}`              | Device token test        |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// NotificationPreferenceTestFactory.java
class NotificationPreferenceTestFactory {

    static UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    static NotificationPreferenceRequest makePreferenceRequest() {
        PreferenceItem item = new PreferenceItem();
        item.setChannel(NotificationChannel.PUSH);
        item.setCategory(NotificationCategory.REMINDER);
        item.setEnabled(true);

        NotificationPreferenceRequest req = new NotificationPreferenceRequest();
        req.setPreferences(List.of(item));
        return req;
    }

    static DeviceTokenRequest makeDeviceTokenRequest() {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("fcmToken_syntheticTest123");
        req.setPlatform(DevicePlatform.ANDROID);
        return req;
    }
}
```

---

### NOTIF-TC-010-001 — GET preferences khi chưa có data → trả về defaults

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationPreferenceServiceImpl.getPreferences(UUID)`
**Test File:** `src/test/java/com/carebridge/backend/notification/service/NotificationPreferenceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-01`
**Oracle Source:** `ADR-010-001 §Decision` — defaults = tất cả enabled=true

**Test Steps:**
1. Arrange: mock `preferenceRepository.findByUserId()` trả về empty list
2. Act: `preferenceService.getPreferences(TEST_USER_ID)`
3. Assert: response chứa 15 items (3 channels × 5 categories), tất cả `enabled=true`

**Expected Result (PASS):** 15 preference items với enabled=true
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-002 — PUT preferences hợp lệ → upsert thành công

**Severity:** `HIGH`
**Feature Under Test:** `NotificationPreferenceServiceImpl.updatePreferences()`
**Test File:** `...NotificationPreferenceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-02`
**Oracle Source:** `ADR-010-001 §Decision`

**Test Steps:**
1. Arrange: mock repository; request = [{PUSH, REMINDER, false}]
2. Act: `preferenceService.updatePreferences(TEST_USER_ID, makePreferenceRequest())`
3. Assert:
   - `preferenceRepository.upsert(userId, PUSH, REMINDER, false)` gọi đúng 1 lần
   - `auditService.emit()` được gọi
   - Response trả về updated preference item

**Expected Result (PASS):** Upsert called correctly, audit emitted
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-003 — Invalid channel enum → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationPreferenceController` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/notification/controller/NotificationPreferenceControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-03`
**Oracle Source:** `BR-NOTIF-CHAN`, Error code `NOTIF-001`

**Test Steps:**
1. Arrange: `@WebMvcTest`; request với channel = "INVALID_CHANNEL"
2. Act: `MockMvc.perform(PUT("/api/v1/users/me/notification-preferences").content(...))`
3. Assert: response status 400

**Expected Result (PASS):** HTTP 400 với validation error
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-004 — PUT device token hợp lệ → 200

**Severity:** `HIGH`
**Feature Under Test:** `NotificationPreferenceServiceImpl.registerDeviceToken()`
**Test File:** `...NotificationPreferenceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-04`
**Oracle Source:** `ADR-010-002 §Decision`

**Test Steps:**
1. Arrange: mock `deviceTokenRepository.upsertByUserIdAndToken()`; request = {token:"fcmABC", platform:ANDROID}
2. Act: `preferenceService.registerDeviceToken(TEST_USER_ID, makeDeviceTokenRequest())`
3. Assert: `deviceTokenRepository.upsertByUserIdAndToken()` được gọi đúng args

**Expected Result (PASS):** Repository upsert called with correct args
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-005 — Blank device token → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@NotBlank` trên `DeviceTokenRequest.token`
**Test File:** `...NotificationPreferenceControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-05`
**Oracle Source:** `BR-NOTIF-FCM`, Error code `NOTIF-003`

**Test Steps:**
1. Arrange: request với token = "" (blank)
2. Act: PUT `/api/v1/users/me/device-token` với blank token
3. Assert: HTTP 400

**Expected Result (PASS):** HTTP 400 validation error
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-006 — Idempotency: gọi 2 lần cùng input → kết quả nhất quán

**Severity:** `HIGH`
**Feature Under Test:** `UPSERT behavior`
**Test File:** `...NotificationPreferenceServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-06`
**Oracle Source:** `ADR-010-001 §Decision` — UPSERT phải idempotent

**Test Steps:**
1. Arrange: mock repository
2. Act: Gọi `updatePreferences()` 2 lần với cùng request
3. Assert: `preferenceRepository.upsert()` được gọi đúng 1×2 = 2 lần (không throw exception, state consistent)

**Expected Result (PASS):** Không exception, consistent state
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-007 — Không có JWT → 401 (Security Test)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306`
**Feature Under Test:** `Spring Security filter`
**Test File:** `...NotificationPreferenceControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-10-07`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: no Authorization header
2. Act: PUT `/api/v1/users/me/notification-preferences`
3. Assert: HTTP 401

**Expected Result (PASS):** HTTP 401
**Current Status:** 🔴 Not written

---

### NOTIF-TC-010-INT-001 — Integration: DB state đúng sau update

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → notification_preferences table`
**Test File:** `src/test/java/com/carebridge/backend/notification/NotificationPreferenceIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL container (Testcontainers)
- Flyway migration applied (includes notification_preferences table)
- Seed: user tồn tại

**Test Steps:**
1. PUT `/api/v1/users/me/notification-preferences` với `[{PUSH, EMERGENCY, false}]`
2. Assert response 200
3. Assert DB: `SELECT enabled FROM notification_preferences WHERE user_id=? AND channel='PUSH' AND category='EMERGENCY'` = false

**DB Assertion:**
```java
Boolean enabled = jdbcTemplate.queryForObject(
    "SELECT enabled FROM notification_preferences WHERE user_id=? AND channel=? AND category=?",
    Boolean.class, userId, "PUSH", "EMERGENCY");
assertThat(enabled).isFalse();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID                   | Test File                                     | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
| ----------------------- | --------------------------------------------- | ------ | -------- | ----------- |
| `NOTIF-TC-010-001`      | `NotificationPreferenceServiceTest.java`      | `[ ]`  | —        | —           |
| `NOTIF-TC-010-002`      | `NotificationPreferenceServiceTest.java`      | `[ ]`  | —        | —           |
| `NOTIF-TC-010-003`      | `NotificationPreferenceControllerTest.java`   | `[ ]`  | —        | —           |
| `NOTIF-TC-010-004`      | `NotificationPreferenceServiceTest.java`      | `[ ]`  | —        | —           |
| `NOTIF-TC-010-005`      | `NotificationPreferenceControllerTest.java`   | `[ ]`  | —        | —           |
| `NOTIF-TC-010-006`      | `NotificationPreferenceServiceTest.java`      | `[ ]`  | —        | —           |
| `NOTIF-TC-010-007`      | `NotificationPreferenceControllerTest.java`   | `[ ]`  | —        | —           |
| `NOTIF-TC-010-INT-001`  | `NotificationPreferenceIntegrationTest.java`  | `[ ]`  | —        | —           |

### 5.1 Red Gate Protocol

**Stub:**
```java
@Service
public class NotificationPreferenceServiceImpl implements INotificationPreferenceService {
    @Override
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceRequest req) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public void registerDeviceToken(UUID userId, DeviceTokenRequest req) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Evidence:** Stub commit hash: `___` | Tất cả FAIL? ☐ Yes → GATE-2 PASS

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-NOTIF-IMP-010` đã được review
- [ ] Flyway migration cho `notification_preferences` và `device_tokens` đã approved
- [ ] FCM integration interface đã sẵn sàng (mock hoặc real)
- [ ] Test fixtures (FX-10-01 đến FX-10-04) đã chuẩn bị

### Exit Criteria (DoD)

- [ ] `.\mvnw.cmd test` — unit tests xanh
- [ ] `.\mvnw.cmd verify` — integration tests xanh
- [ ] Test coverage ≥ 80% cho `NotificationPreferenceServiceImpl`
- [ ] UPSERT hoạt động đúng (idempotent)
- [ ] Audit log `PreferencesUpdated` được ghi

**CASE 2.0:**
- [ ] Red Gate — tests FAIL với stub
- [ ] Props Isolation — dùng factory methods

### Suspension Criteria

- Migration chưa được approve/run trên staging
- FCM integration chưa có mock

---

## 7. Rollback Plan

```bash
# Revert nếu migration gây lỗi
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS notification_preferences CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS device_tokens CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = 'V_NOTIF';"

git checkout -- src/main/java/com/carebridge/backend/notification/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu                                      | Check | Gate |
| --------- | --------------------- | --------------------------------------------- | ----- | ---- |
| AP-AI-001 | Unconstrained Gen     | TC không reference ADR-010-001/ADR-010-002    | ☐     | G-0  |
| AP-AI-002 | Green-from-Birth      | Test PASS với throw stub                      | ☐     | G-2★ |
| AP-AI-003 | Implicit Decision     | Test assume INSERT thay vì UPSERT             | ☐     | G-1  |
| AP-AI-005 | Hallucinated Contract | Test import service không tồn tại             | ☐     | G-3  |

---

*TDD Template v2.0 — UC-10 Update Notification Preferences*
*Status: Draft — chờ Tech Lead review.*
