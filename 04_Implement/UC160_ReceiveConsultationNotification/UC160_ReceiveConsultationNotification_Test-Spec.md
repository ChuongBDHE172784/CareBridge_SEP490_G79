# TEST SPECIFICATION — UC-160 Receive Consultation Notification
# Đặc tả Kiểm thử — Nhận Thông báo Tư vấn

| Field                  | Value                                             |
|------------------------|---------------------------------------------------|
| **Document ID**        | `CB-NOTIF-TEST-003`                               |
| **Version**            | `1.0`                                             |
| **Date**               | `2026-06-26`                                      |
| **Status**             | `Approved`                                        |
| **Document Owner**     | `PhuongNT`                                        |
| **Author**             | `AI Agent`                                        |
| **Reviewed by**        | `[Tech Lead]`                                     |
| **DPO Sign-off**       | `[ ] Pending`                                     |
| **Approved by**        | `[Principal Architect]`                           |
| **Last Review**        | `2026-06-26`                                      |
| **Based on EDS**       | `v2.0`                                            |
| **TDS Reference**      | `CB-NOTIF-IMP-003`                                |
| **Data Classification**| `Internal`                                        |

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                              |
|------------|-----------------|------------------------------------------------|
| 2026-06-26 | AI Agent        | Tạo Test-Spec lần đầu cho UC-160               |

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
| **Feature / UC ID**     | `UC-160`                                                        |
| **Module**              | `notification — ConsultationNotification`                       |
| **Spec gốc**            | `CB-NOTIF-IMP-003`                                              |
| **Priority**            | 🟠 P1                                                           |
| **Data Classification** | `Internal`                                                      |
| **Upstream Dependencies**| `consultation module, zegocloud webhook, notification_preferences` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                   | Value                                                          |
|-------------------------|----------------------------------------------------------------|
| **AI Assisted?**        | `Yes`                                                          |
| **Constraint Source**   | `CB-NOTIF-IMP-003 §17`                                        |
| **Constraints Injected**| C1 (no zego token), C2 (recipients), C3 (deep link)          |
| **Trust Level**         | `T2 → T3 (pending Red Gate)`                                  |

---

## 2. Logic Issues Resolved

| # | Spec gốc                                    | Thực tế                                        | Fix trong test                                    |
|---|---------------------------------------------|------------------------------------------------|---------------------------------------------------|
| L1 | Spec không rõ recipient per event type    | ADR-NOTIF-CON-003: EXPERT_JOINED→only MOTHER  | Test mỗi event type để verify recipients          |
| L2 | Spec không đề cập token exclusion         | ADR-NOTIF-CON-002: ZegoToken NEVER in payload | Security test kiểm tra payload không có token    |
| L3 | Deep link format chưa được định nghĩa     | carebridge://consultation/{id}/{action}        | Test assert deep link format đúng                |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
notification.ConsultationNotification bao gồm:
├── Service Layer (ConsultationNotificationService)
│   ├── resolveRecipients() — per event type
│   ├── buildPayload() — no ZegoToken
│   └── buildDeepLink() — correct format
├── Security: ZegoToken exclusion test
└── Integration: full event → FCM → DB
```

### TDS-02 — Test Basis

| Source              | Items Derived                                              |
|---------------------|------------------------------------------------------------|
| UC-160              | 4 event types, recipients, deep link, ZegoToken exclusion |
| BR-NOTIF-CON-001    | 4 notification types: BOOKED, REMINDER, EXPERT_JOINED, CANCELLED |
| BR-NOTIF-CON-002    | Deep link in payload                                       |
| BR-NOTIF-CON-003    | ZegoToken NEVER in payload                                 |
| ADR-NOTIF-CON-003   | Multi-recipient resolve per event type                     |
| ADR-NOTIF-003       | Retry 3x backoff                                           |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID    | Test Condition                                              | Coverage Item                              | Test Cases          |
|-----------------|-------------------------------------------------------------|--------------------------------------------|---------------------|
| TC-COND-CN-001  | EXPERT_JOINED → chỉ gửi cho MOTHER                        | `resolveRecipients(EXPERT_JOINED)`         | `CONNOTIF-TC-001`   |
| TC-COND-CN-002  | CONSULTATION_BOOKED → gửi cho cả MOTHER và EXPERT          | `resolveRecipients(CONSULTATION_BOOKED)`   | `CONNOTIF-TC-002`   |
| TC-COND-CN-003  | Payload KHÔNG chứa ZegoCloud token                         | `buildPayload()` security check            | `CONNOTIF-TC-003`   |
| TC-COND-CN-004  | Deep link format đúng per event type                       | `buildDeepLink()`                          | `CONNOTIF-TC-004`   |
| TC-COND-CN-005  | FCM retry 3 lần → FAILED                                   | `FcmServiceImpl.sendWithRetry()`           | `CONNOTIF-TC-005`   |
| TC-COND-CN-006  | CONSULTATION_CANCELLED gửi cho tất cả participants         | `resolveRecipients(CONSULTATION_CANCELLED)`| `CONNOTIF-TC-006`   |
| TC-COND-CN-007  | Integration: EXPERT_JOINED event → DB record               | Full stack                                 | `CONNOTIF-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique               | Applied To                        | Rationale                           |
|-------------------------|-----------------------------------|-------------------------------------|
| Equivalence Partitioning | 4 event types                    | Each type has different recipients  |
| Security Testing        | ZegoToken exclusion               | Critical security requirement       |
| Boundary Value Analysis  | Multi-recipient: 1 vs 2 recipients | EXPERT_JOINED vs BOOKED             |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type    | Value                                                       | Mục đích                  |
|--------------|---------|-------------------------------------------------------------|---------------------------|
| `FX-CN-001`  | DB seed | consultation c-001 {motherId: user-001, expertId: user-002} | Standard consultation     |
| `FX-CN-002`  | Mock    | FCM always fails                                            | Retry/FAILED test         |
| `FX-CN-003`  | Mock    | buildPayload() with all event types                         | Deep link + ZegoToken test|

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class ConsultationNotificationTestFactory {
    static ConsultationNotification makeValidNotification() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new ConsultationNotification.ConsultationNotificationBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static ConsultationNotification makeValidNotification(Consumer<ConsultationNotification> overrides) {
        var entity = makeValidNotification();
        overrides.accept(entity);
        return entity;
    }
}
```

---

### CONNOTIF-TC-001 — EXPERT_JOINED gửi cho chỉ MOTHER

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationNotificationService.resolveRecipients()` — EXPERT_JOINED
**Test File:** `src/test/java/com/carebridge/backend/notification/service/ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-001`
**Oracle Source:** `ADR-NOTIF-CON-003`

**Preconditions:**
- FX-CN-001: consultation c-001 với motherId=user-001, expertId=user-002
- Cả hai có FCM tokens và CONSULTATION preference=enabled

**Test Steps:**
1. Arrange: mock consultation repository → c-001 với motherId/expertId
2. Arrange: mock FCM success
3. Act: `service.sendConsultationNotification(c-001-uuid, EXPERT_JOINED)`
4. Assert: FCM được gọi đúng 1 lần (cho user-001/MOTHER)
5. Assert: FCM KHÔNG được gọi cho user-002/EXPERT

**Expected Result (PASS):**
- FCM call count = 1
- Recipient = motherId (user-001)

**Current Status:** 🔴 Not written

```gherkin
  Scenario: EXPERT_JOINED → chỉ MOTHER nhận notification
    Given consultation c-001 {mother: user-001, expert: user-002}
    When sendConsultationNotification(c-001, EXPERT_JOINED)
    Then FCM được gọi 1 lần cho user-001 (MOTHER)
    And FCM KHÔNG được gọi cho user-002 (EXPERT)
```

---

### CONNOTIF-TC-002 — CONSULTATION_BOOKED gửi cho MOTHER và EXPERT

**Severity:** `HIGH`
**Feature Under Test:** `resolveRecipients()` — CONSULTATION_BOOKED multi-recipient
**Test File:** `...ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-002`
**Oracle Source:** `ADR-NOTIF-CON-003`

**Test Steps:**
1. Arrange: FX-CN-001 với cả mother và expert có FCM tokens
2. Act: `service.sendConsultationNotification(c-001, CONSULTATION_BOOKED)`
3. Assert: FCM được gọi đúng 2 lần (1 cho user-001, 1 cho user-002)

**Expected Result (PASS):**
- FCM call count = 2
- 2 NotificationRecords được lưu (1 per recipient)

**Current Status:** 🔴 Not written

---

### CONNOTIF-TC-003 — Payload KHÔNG chứa ZegoCloud token (SECURITY)

**Severity:** `CRITICAL`
**CWE:** `CWE-312 — Cleartext Storage of Sensitive Information`
**Feature Under Test:** `ConsultationNotificationService.buildPayload()`
**Test File:** `...ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-003`
**Oracle Source:** `ADR-NOTIF-CON-002`

**Test Steps:**
1. For each event type in `[BOOKED, REMINDER, EXPERT_JOINED, CANCELLED]`:
2. Act: `service.buildPayload(consultation, eventType)`
3. Assert: returned payload serialized to Map does NOT contain key "zegoToken", "roomToken", "token", "zego"

**Expected Result (PASS):**
- 0 occurrences of Zego-related keys in payload for all 4 event types

**Expected Result (FAIL = security breach):**
- Any Zego token present in FCM payload → critical vulnerability

**Current Status:** 🔴 Not written

```gherkin
  Scenario: ZegoCloud token KHÔNG được có trong FCM payload
    When buildPayload() được gọi với bất kỳ ConsultationNotificationType nào
    Then payload.serialized không chứa "zegoToken"
    And payload.serialized không chứa "roomToken"
    And payload.serialized không chứa keys chứa "zego" (case-insensitive)
```

---

### CONNOTIF-TC-004 — Deep link format đúng per event type

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationNotificationService.buildDeepLink()`
**Test File:** `...ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-004`
**Oracle Source:** `ADR-NOTIF-CON-001`

**Test Steps (parametrized):**

| eventType               | Expected deep link                                |
|-------------------------|---------------------------------------------------|
| `CONSULTATION_BOOKED`   | `carebridge://consultation/{id}/detail`           |
| `CONSULTATION_REMINDER` | `carebridge://consultation/{id}/detail`           |
| `EXPERT_JOINED`         | `carebridge://consultation/{id}/join`             |
| `CONSULTATION_CANCELLED`| `carebridge://consultation/{id}/cancelled`        |

**Expected Result (PASS):**
- Deep link matches expected format for each event type

**Current Status:** 🔴 Not written

---

### CONNOTIF-TC-005 — FCM retry 3 lần → status FAILED

**Severity:** `CRITICAL`
**Feature Under Test:** FCM retry exhaustion for consultation notification
**Test File:** `...ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-005`
**Oracle Source:** `ADR-NOTIF-003`, `BR-NOTIF-CON-004`

**Preconditions:**
- FX-CN-002: FCM mock always fails

**Test Steps:**
1. Arrange: FCM mock → always throws FirebaseMessagingException
2. Act: `service.sendConsultationNotification(c-001, EXPERT_JOINED)`
3. Assert: NotificationRecord saved with `status=FAILED`, `attemptCount=3`

**Expected Result (PASS):**
- Record: `status=FAILED`, `attemptCount=3`
- No uncaught exception propagated

**Current Status:** 🔴 Not written

---

### CONNOTIF-TC-006 — CONSULTATION_CANCELLED gửi cho tất cả participants

**Severity:** `HIGH`
**Feature Under Test:** `resolveRecipients()` — CONSULTATION_CANCELLED
**Test File:** `...ConsultationNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-006`
**Oracle Source:** `ADR-NOTIF-CON-003`

**Test Steps:**
1. Arrange: consultation với mother + expert
2. Act: `service.sendConsultationNotification(c-001, CONSULTATION_CANCELLED)`
3. Assert: FCM được gọi 2 lần (cho cả mother và expert)

**Current Status:** 🔴 Not written

```gherkin
  Scenario: CONSULTATION_CANCELLED gửi cho tất cả
    When sendConsultationNotification(c-001, CONSULTATION_CANCELLED)
    Then FCM được gọi 2 lần (MOTHER + EXPERT)
```

---

### CONNOTIF-TC-INT-001 — Integration: EXPERT_JOINED event → DB record

**Severity:** `HIGH`
**Feature Under Test:** Full stack: event handler → service → FCM → DB
**Test File:** `src/test/java/com/carebridge/backend/notification/ConsultationNotificationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CN-007`

**Preconditions:**
- PostgreSQL Testcontainer running
- consultation c-001 trong DB
- FCM WireMock returns success

**Test Steps:**
1. Call: `service.sendConsultationNotification(c-001-uuid, EXPERT_JOINED)`
2. DB assert: `SELECT * FROM notification_records WHERE metadata->>'eventType' = 'EXPERT_JOINED'`
3. Assert: 1 row, status='SENT'
4. Assert: `metadata ? 'zegoToken' = false` (no ZegoToken in DB metadata)

**Expected Result (PASS):**
- 1 notification_record for MOTHER
- metadata: `{eventType: EXPERT_JOINED, deepLink: "carebridge://consultation/c-001/join"}`
- metadata: does NOT contain zegoToken

**DB Assertion:**
```sql
SELECT status, metadata, metadata ? 'zegoToken' as has_zego_token
FROM notification_records
WHERE metadata->>'eventType' = 'EXPERT_JOINED';
-- Expected: status='SENT', has_zego_token=false
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID                 | Test File                                                | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR                   |
|-----------------------|----------------------------------------------------------|--------|----------|-------------------------------|
| `CONNOTIF-TC-001`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | Extract `resolveRecipients()` |
| `CONNOTIF-TC-002`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | —                             |
| `CONNOTIF-TC-003`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | Extract `buildPayload()` pure |
| `CONNOTIF-TC-004`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | Extract `buildDeepLink()`     |
| `CONNOTIF-TC-005`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | —                             |
| `CONNOTIF-TC-006`     | `...ConsultationNotificationServiceTest.java`            | `[ ]`  | —        | —                             |
| `CONNOTIF-TC-INT-001` | `...ConsultationNotificationIntegrationTest.java`        | `[ ]`  | —        | —                             |

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] `CB-NOTIF-IMP-003` TDS approved
- [ ] UC-158 notification infrastructure sẵn sàng
- [ ] Consultation module hoàn chỉnh
- [ ] ADR-NOTIF-CON-001, 002, 003 Accepted

### Exit Criteria (DoD)
- [ ] Tất cả 7 test cases xanh
- [ ] `CONNOTIF-TC-003` (ZegoToken security test) xanh — CRITICAL
- [ ] EXPERT_JOINED chỉ gửi cho MOTHER (TC-001)
- [ ] Deep link format đúng cho tất cả 4 event types (TC-004)
- [ ] Security scan: no ZegoToken in FCM logs

**Exit Criteria bổ sung — Security:**
- [ ] CI có bước `grep -i "zegoToken\|roomToken" FCM_payload_logs` → 0 matches

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/notification/service/ConsultationNotificationService.java
# Không có migration thêm — tái dùng notification_records
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu                                   | Check |
|-----------|-----------------------|--------------------------------------------|-------|
| AP-AI-001 | Unconstrained Gen     | ZegoToken test không có ADR reference      | ☐     |
| AP-AI-002 | Green-from-Birth      | TC-003 PASS với empty `buildPayload()`     | ☐     |
| AP-AI-003 | Implicit Decision     | Recipient mapping không có ADR             | ☐     |
| AP-AI-005 | Hallucinated Contract | Import ZegoCloudService không tồn tại      | ☐     |
