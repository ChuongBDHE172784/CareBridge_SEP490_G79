# TEST SPECIFICATION — UC-159 Receive Community Reply Notification
# Đặc tả Kiểm thử — Nhận Thông báo Trả lời Cộng đồng

| Field                  | Value                                               |
|------------------------|-----------------------------------------------------|
| **Document ID**        | `CB-NOTIF-TEST-002`                                 |
| **Version**            | `1.1`                                               |
| **Date**               | `2026-06-26`                                        |
| **Status**             | `Implemented`                                       |
| **Document Owner**     | `PhuongNT`                                          |
| **Author**             | `AI Agent`                                          |
| **Reviewed by**        | `[Tech Lead]`                                       |
| **DPO Sign-off**       | `[ ] Pending`                                       |
| **Approved by**        | `[Principal Architect]`                             |
| **Last Review**        | `2026-07-07`                                        |
| **Based on EDS**       | `v2.0`                                              |
| **TDS Reference**      | `CB-NOTIF-IMP-002`                                  |
| **Data Classification**| `Internal`                                          |

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                  |
|------------|-----------------|---------------------------------------------------|
| 2026-06-26 | AI Agent        | Tạo Test-Spec lần đầu cho UC-159                  |

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

| Field                   | Value                                                       |
|-------------------------|-------------------------------------------------------------|
| **Feature / UC ID**     | `UC-159`                                                    |
| **Module**              | `notification — CommunityReplyNotification`                 |
| **Spec gốc**            | `CB-NOTIF-IMP-002`                                          |
| **Priority**            | 🟠 P1                                                       |
| **Data Classification** | `Internal`                                                  |
| **Upstream Dependencies**| `community (Question/Answer), notification_preferences, question_notification_mutes` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                   | Value                                                       |
|-------------------------|-------------------------------------------------------------|
| **AI Assisted?**        | `Yes`                                                       |
| **Constraint Source**   | `CB-NOTIF-IMP-002 §17`                                     |
| **Constraints Injected**| C1 (dual gate), C2 (self-reply), C3 (metadata), C4 (retry) |
| **Trust Level**         | `T2 → T3 (pending Red Gate)`                               |

---

## 2. Logic Issues Resolved

| # | Spec gốc                          | Thực tế                                  | Fix trong test                              |
|---|-----------------------------------|------------------------------------------|---------------------------------------------|
| L1 | Spec chỉ đề cập preference check | ADR-NOTIF-CR-001: cần thêm mute check   | Test dual gate: preference AND mute check   |
| L2 | Không đề cập self-reply           | ADR-NOTIF-CR-002: skip self-reply        | Test case riêng cho self-reply              |
| L3 | referenceId không rõ              | referenceId = answerId, metadata.questionId | Test metadata contains both IDs          |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
notification.CommunityReplyNotification bao gồm:
├── Service Layer (CommunityReplyNotificationService)
│   ├── Preference gate
│   ├── Mute gate
│   └── Self-reply guard
└── Integration (Testcontainers + community module)
```

### TDS-02 — Test Basis

| Source          | Items Derived                                         |
|-----------------|-------------------------------------------------------|
| UC-159          | Reply notification, mute, self-reply skip             |
| BR-NOTIF-CR-001 | COMMUNITY_REPLY preference gate                       |
| BR-NOTIF-CR-002 | Per-question mute gate                                |
| BR-NOTIF-CR-003 | metadata: {questionId, answerId}                      |
| BR-NOTIF-CR-004 | Self-reply guard                                      |
| ADR-NOTIF-003   | Retry 3x, backoff 0/2/4s                             |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID    | Test Condition                                    | Coverage Item                               | Test Cases         |
|-----------------|---------------------------------------------------|---------------------------------------------|--------------------|
| TC-COND-CR-001  | Preference enabled + not muted + not self → send | `sendReplyNotification()` full path         | `NOTIFCR-TC-001`   |
| TC-COND-CR-002  | Question muted → skip                            | `muteRepository.existsByUserIdAndQuestionId` | `NOTIFCR-TC-002`  |
| TC-COND-CR-003  | Self-reply → skip                                | `isSelfReply()` check                       | `NOTIFCR-TC-003`   |
| TC-COND-CR-004  | COMMUNITY_REPLY preference disabled → skip        | `preferenceRepository.isEnabled()`          | `NOTIFCR-TC-004`   |
| TC-COND-CR-005  | FCM retry 3 lần → FAILED                         | `FcmServiceImpl.sendWithRetry()`            | `NOTIFCR-TC-005`   |
| TC-COND-CR-006  | metadata chứa cả questionId và answerId          | `NotificationRecord.metadata`               | `NOTIFCR-TC-006`   |
| TC-COND-CR-007  | Mute endpoint ownership check                    | `MuteNotificationController`                | `NOTIFCR-TC-007`   |
| TC-COND-CR-008  | Integration: full flow → DB verify               | Full stack                                  | `NOTIFCR-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique               | Applied To                     | Rationale                           |
|-------------------------|--------------------------------|-------------------------------------|
| Equivalence Partitioning | Gating conditions (3 gates)   | Each gate: enabled/disabled         |
| State Transition Testing | Notification send/skip states  | Guard chain order matters           |
| Error Guessing          | Self-reply edge case           | Common source of notification spam  |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type    | Value                                                            | Mục đích              |
|--------------|---------|------------------------------------------------------------------|-----------------------|
| `FX-CR-001`  | DB seed | question q-001 owned by user-001, COMMUNITY_REPLY=enabled, not muted | Happy path        |
| `FX-CR-002`  | DB seed | q-002 muted by user-002 (question_notification_mutes row exists) | Mute test            |
| `FX-CR-003`  | Mock    | answerer = question owner (self-reply scenario)                  | Self-reply test       |
| `FX-CR-004`  | DB seed | user-003 has COMMUNITY_REPLY preference = disabled               | Preference gate test  |
| `FX-CR-005`  | Mock    | FCM always fails                                                 | Retry/FAILED test     |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class CommunityReplyNotificationTestFactory {
    static CommunityReplyNotification makeValidNotification() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new CommunityReplyNotification.CommunityReplyNotificationBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static CommunityReplyNotification makeValidNotification(Consumer<CommunityReplyNotification> overrides) {
        var entity = makeValidNotification();
        overrides.accept(entity);
        return entity;
    }
}
```

---

### NOTIFCR-TC-001 — Gửi thành công: preference enabled + not muted + not self-reply

**Severity:** `HIGH`
**Feature Under Test:** `CommunityReplyNotificationService.sendReplyNotification()`
**Test File:** `src/test/java/com/carebridge/backend/notification/service/CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-001`
**Oracle Source:** `BR-NOTIF-CR-001`, `BR-NOTIF-CR-002`, `BR-NOTIF-CR-004`

**Preconditions:**
- FX-CR-001: q-001 owned by user-001, COMMUNITY_REPLY=enabled, not muted
- answerer = user-002 (khác owner)
- FCM mock success

**Test Steps:**
1. Arrange: mock preference → enabled, mute → false, FCM → success
2. Act: `service.sendReplyNotification(q-001, answer-001, user-002)`
3. Assert: FCM được gọi 1 lần; record saved với status=SENT

**Expected Result (PASS):**
- `fcmService.sendWithRetry()` call count = 1
- Record: `type=COMMUNITY_REPLY`, `status=SENT`, `referenceId=answer-001`

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Happy path — notification gửi thành công
    Given q-001 owned by user-001 (COMMUNITY_REPLY enabled, not muted)
    And answerer = user-002 (not owner)
    When sendReplyNotification(q-001, answer-001, user-002)
    Then FCM được gọi 1 lần
    And NotificationRecord status = SENT
```

---

### NOTIFCR-TC-002 — Skip khi câu hỏi bị mute

**Severity:** `HIGH`
**Feature Under Test:** Per-question mute gate
**Test File:** `...CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-002`
**Oracle Source:** `ADR-NOTIF-CR-001`, `BR-NOTIF-CR-002`

**Preconditions:**
- FX-CR-002: user-002 đã mute q-002

**Test Steps:**
1. Arrange: mock `muteRepository.existsByUserIdAndQuestionId(user-002, q-002)` → true
2. Act: `service.sendReplyNotification(q-002, answer-002, user-003)`
3. Assert: FCM KHÔNG được gọi; record KHÔNG được lưu

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Câu hỏi bị mute → skip notification
    Given user-002 đã mute q-002
    When sendReplyNotification(q-002, answer-002, user-003)
    Then FCM KHÔNG được gọi
    And NotificationRecord KHÔNG được tạo
```

---

### NOTIFCR-TC-003 — Skip self-reply

**Severity:** `MEDIUM`
**Feature Under Test:** `isSelfReply()` guard
**Test File:** `...CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-003`
**Oracle Source:** `ADR-NOTIF-CR-002`

**Test Steps:**
1. Arrange: q-003 owned by user-001; answerer = user-001 (same)
2. Act: `service.sendReplyNotification(q-003, answer-003, user-001)`
3. Assert: FCM KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### NOTIFCR-TC-004 — COMMUNITY_REPLY preference disabled → skip

**Severity:** `HIGH`
**Feature Under Test:** Preference gate — COMMUNITY_REPLY type
**Test File:** `...CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-004`
**Oracle Source:** `BR-NOTIF-CR-001`

**Test Steps:**
1. Arrange: preference COMMUNITY_REPLY = disabled for owner
2. Act: `service.sendReplyNotification(q-004, answer-004, user-002)`
3. Assert: FCM KHÔNG được gọi; `muteRepository` cũng KHÔNG được gọi (preference check first)

**Current Status:** 🔴 Not written

---

### NOTIFCR-TC-005 — FCM retry 3 lần → status FAILED

**Severity:** `CRITICAL`
**Feature Under Test:** FCM retry exhaustion
**Test File:** `...CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-005`
**Oracle Source:** `ADR-NOTIF-003`, `BR-NOTIF-CR-005`

**Preconditions:**
- FX-CR-005: FCM mock always returns error

**Test Steps:**
1. Arrange: preference enabled, not muted, FCM mock → always fail
2. Act: `service.sendReplyNotification(q-001, answer-001, user-002)`
3. Assert: Record saved với `status=FAILED`, `attemptCount=3`

**Current Status:** 🔴 Not written

```gherkin
  Scenario: FCM thất bại 3 lần → FAILED record
    Given FCM mock luôn trả về lỗi
    When sendReplyNotification() với preconditions hợp lệ
    Then NotificationRecord status = FAILED, attemptCount = 3
```

---

### NOTIFCR-TC-006 — metadata chứa cả questionId và answerId

**Severity:** `HIGH`
**Feature Under Test:** `NotificationRecord.metadata` JSON structure
**Test File:** `...CommunityReplyNotificationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-006`
**Oracle Source:** `BR-NOTIF-CR-003`

**Test Steps:**
1. Arrange: happy path setup (FX-CR-001)
2. Capture: ArgumentCaptor on `recordRepository.save()`
3. Assert: `capturedRecord.getMetadata()` contains `{"questionId": "q-001", "answerId": "answer-001"}`

**Expected Result (PASS):**
- `metadata` JSONB field chứa cả `questionId` và `answerId`

**Current Status:** 🔴 Not written

---

### NOTIFCR-TC-007 — Mute endpoint ownership: không mute câu hỏi của người khác

**Severity:** `HIGH`
**Feature Under Test:** `MuteNotificationController.muteQuestion()` — ownership check
**Test File:** `...controller/MuteNotificationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-CR-007`
**Oracle Source:** `ADR-NOTIF-CR-001`

**Test Steps:**
1. Arrange: q-010 owned by user-010; attacker = user-011
2. Act: `POST /api/v1/notifications/mute/questions/q-010` với JWT của user-011
3. Assert: HTTP 403; no mute record created

**Note:** Mute chỉ có ý nghĩa với câu hỏi của chính mình hoặc câu hỏi mình quan tâm (follow). Check ownership của question người dùng muốn mute (chỉ owner của câu hỏi mới cần mute).

**Current Status:** 🔴 Not written

---

### NOTIFCR-TC-INT-001 — Integration: answer posted → notification DB record

**Severity:** `HIGH`
**Feature Under Test:** Full stack: AnswerPostedEvent → notification record
**Test File:** `src/test/java/com/carebridge/backend/notification/CommunityReplyNotificationIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL Testcontainer running
- question q-001 owned by user-001 (preference enabled, not muted)
- FCM mock (WireMock) returns success

**Test Steps:**
1. Service call: `sendReplyNotification(q-001-uuid, answer-001-uuid, user-002-uuid)`
2. DB assert: `SELECT * FROM notification_records WHERE metadata->>'questionId' = 'q-001-uuid'`
3. Assert: 1 row, status='SENT', type='COMMUNITY_REPLY'

**Expected Result (PASS):**
- 1 record với status='SENT'
- metadata JSON: `{"questionId": "q-001-uuid", "answerId": "answer-001-uuid"}`

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID               | Test File                                          | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR             |
|---------------------|----------------------------------------------------|--------|----------|--------------------------|
| `NOTIFCR-TC-001`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | —                        |
| `NOTIFCR-TC-002`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | —                        |
| `NOTIFCR-TC-003`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | Self-reply guard implemented |
| `NOTIFCR-TC-004`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | —                        |
| `NOTIFCR-TC-005`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | —                        |
| `NOTIFCR-TC-006`    | `...CommunityReplyNotificationServiceTest.java`    | `[x]`  | `[x] 2026-07-07` | —                        |
| `NOTIFCR-TC-007`    | `...controller/MuteNotificationControllerTest.java`| `[ ]`  | —        | —                        |
| `NOTIFCR-TC-INT-001`| `...CommunityReplyNotificationIntegrationTest.java`| `[ ]`  | —        | —                        |

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] `CB-NOTIF-IMP-002` TDS approved
- [ ] UC-158 infrastructure sẵn sàng (notification_records table)
- [ ] ADR-NOTIF-CR-001, ADR-NOTIF-CR-002 Accepted
- [ ] Community module (Question/Answer) đã hoạt động

### Exit Criteria (DoD)
- [x] Targeted service tests xanh for implemented unit scope
- [x] Self-reply guard hoạt động (TC-003)
- [x] Mute gate hoạt động (TC-002)
- [x] FCM metadata chứa questionId + answerId (TC-006)

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/notification/service/CommunityReplyNotificationService.java
psql -c "DROP TABLE IF EXISTS question_notification_mutes;"
psql -c "ALTER TABLE notification_records DROP COLUMN IF EXISTS metadata;"
```

---

## 8. Implementation Evidence

| Field | Value |
|-------|-------|
| Implementation Date | `2026-07-07` |
| Test Command | `mvn test -Dtest=ReminderNotificationServiceTest,CommunityReplyNotificationServiceTest,ConsultationNotificationServiceTest,FamilyAlertServiceTest` |
| Targeted Result | `22 tests run, 0 failures, 0 errors` |
| Full Suite Note | `mvn test` still has unrelated integration/testcontainers failures outside notification scope. Notification service tests are green. |

---

## 9. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern      | Dấu hiệu                          | Check |
|-----------|-------------------|-----------------------------------|-------|
| AP-AI-001 | Unconstrained Gen | Skip dual gate check              | ☐     |
| AP-AI-002 | Green-from-Birth  | Test PASS với throw stub          | ☐     |
| AP-AI-003 | Implicit Decision | Self-reply without ADR reference  | ☐     |
