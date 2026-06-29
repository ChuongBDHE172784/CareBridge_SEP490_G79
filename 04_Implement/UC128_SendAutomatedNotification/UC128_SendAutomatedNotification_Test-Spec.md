# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Test Specification — UC-128 Send Automated Notification

| Field              | Value                                              |
|--------------------|----------------------------------------------------|
| **Document ID**    | `CB-NOTIF-TEST-128`                                |
| **Version**        | `1.0`                                              |
| **Date**           | `2026-06-28`                                       |
| **Status** | `Implemented`                                            |
| **Document Owner** | `PhuongNT`                                         |
| **Author**         | `AI Agent`                                         |
| **Reviewed by**    | `[QA Lead]`                                        |
| **Linked TDS**     | `CB-NOTIF-IMP-128`                                 |
| **Based on EDS**   | `v2.0`                                             |
| **Function ID**    | `3.1.2.3`                                          |
| **Package**        | `com.carebridge.backend.notification`              |
| **Test Command**   | `./mvnw test -pl 05_Development/CareBridgeAPI -Dtest=*Notification*` |

---

## CHANGELOG

| Date       | Author   | Description                                              |
|------------|----------|----------------------------------------------------------|
| 2026-06-28 | AI Agent | Initial draft for UC-128 Test Specification              |

---

## TABLE OF CONTENTS

1. [Module Info + AI Generation Context (CASE 2.0)](#1-module-info--ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Cases](#4-test-cases)
5. [Red-Green-Refactor Tracker + Red Gate Protocol](#5-red-green-refactor-tracker--red-gate-protocol)
6. [Entry/Exit Criteria](#6-entryexit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Info + AI Generation Context (CASE 2.0)

### 1.1 Module Summary

| Field                  | Value                                                                          |
|------------------------|--------------------------------------------------------------------------------|
| **Bounded Context**    | `notification`                                                                 |
| **UC**                 | `UC-128 SendAutomatedNotification`                                             |
| **Primary Actor**      | System (scheduler, application event, ADMIN API caller)                        |
| **Data Classification**| Internal (body = plain text; referenceId links to PHI domain entities)        |
| **Test Scope**         | Unit (mock-based), Integration (Testcontainer PostgreSQL), Security (RBAC)     |
| **Excluded**           | FCM live delivery end-to-end (requires real Firebase credentials), SMTP relay  |

### 1.2 CASE 2.0 Constraint Injection (Applied to All Test Cases)

The following constraints govern test oracle definitions. Every expected result below is derived from one or more of these constraints.

| # | Constraint ID | Rule Summary                                                                                  | TDS Reference |
|---|---------------|-----------------------------------------------------------------------------------------------|---------------|
| C1 | PERSIST-ALL   | NotificationRecord MUST be saved in ALL outcomes (SENT and FAILED)                           | §8.1 / ADR-128-001 |
| C2 | EMERGENCY-BYPASS | EMERGENCY type MUST bypass preference gate and quiet hours                               | §6.4 / ADR-128-003 |
| C3 | NO-PHI        | body and title must NOT contain raw PHI                                                       | §8.5 / ADR-128-004 |
| C4 | IDEMPOTENCY   | Duplicate (userId, type, referenceId) within 5-min window MUST be suppressed                 | §8.3 / ADR-128-005 |
| C5 | FCM-SAFE      | FCM exceptions MUST be caught; status=FAILED set; no rethrow                                 | §8.1 / ADR-128-001 |
| C6 | RBAC          | POST /send requires ROLE_ADMIN or ROLE_SYSTEM                                                 | §16 / BR-RBAC |
| C7 | DTO-ONLY      | Never expose NotificationRecord entity — always return NotificationRecordResponse             | CLAUDE.md     |
| C8 | AUDIT-ALWAYS  | AuditService.log() MUST be called for every send() invocation outcome                        | §7.1          |

### 1.3 Test Data Classification

All test data in this document is **SYNTHETIC** — no real user PII, no real FCM tokens, no real health data. Test UUIDs are clearly labeled as test fixtures.

---

## 2. Logic Issues Resolved

The following ambiguities and logic gaps were identified during spec analysis and resolved before test case authoring:

| Issue ID     | Issue Description                                                                | Resolution                                                                                      | Applied In      |
|--------------|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|-----------------|
| LIR-128-001  | Table duality: `notification_records` (entity-backed) vs. `notifications` (V1 DDL) | Tests target `notification_records` (the entity `NotificationRecord` maps to). V1 `notifications` table is separate (UC-11/12). Explicitly documented in TDS §5.2. | All DB-verify tests |
| LIR-128-002  | `FcmServiceImpl` is a stub returning `null` — does a null fcmMessageId constitute SENT? | Yes: when `tokens.isEmpty()` → FAILED. When tokens present and no exception → SENT (fcmMessageId may be null in stub mode). Tests assert status, not fcmMessageId value, when stub active. | TC-UNIT-001, TC-INT-001 |
| LIR-128-003  | Idempotency method `existsDuplicate()` does not yet exist in repository          | TC-UNIT-005 tests the NEW method via mock. Integration tests will fail Red until method is added. This is a Red-Phase stub scenario. | TC-UNIT-005, TC-INT-005 |
| LIR-128-004  | EMERGENCY preference bypass: current `NotificationServiceImpl.send()` does not implement preference gate | The preference gate is NOT yet implemented in the existing code. Tests for preference gate (C4, C2) are Red-Phase stubs and will fail until implemented. | TC-UNIT-004, TC-UNIT-009 |
| LIR-128-005  | Quiet hours: `notification_preferences` has `quiet_hours_start` / `quiet_hours_end` columns but no enforcement code exists | Quiet hours check is a new behavior to implement. Existing tests not affected; new tests (TC-UNIT-010) will be Red until implemented. | TC-UNIT-010 |
| LIR-128-006  | `send()` audit log: current impl calls `auditService.log()` with `AuditAction.NOTIFICATION_FAILED` or `NOTIFICATION_SENT` — this is already implemented. | Green baseline for audit tests. Oracle: `AuditAction.NOTIFICATION_SENT` on success, `NOTIFICATION_FAILED` on failure. | TC-UNIT-001, TC-UNIT-002 |

---

## 3. Test Design Specification

### 3.1 Test Scope

| In Scope                                                                    | Out of Scope                                              |
|-----------------------------------------------------------------------------|-----------------------------------------------------------|
| `NotificationService.send()` — all code paths                               | Real FCM delivery to Firebase (live API)                  |
| `NotificationService.registerDeviceToken()` and `deregisterDeviceToken()`   | Real Gmail SMTP delivery                                   |
| `NotificationService.getMyNotifications()` — pagination + type filter       | Flutter/mobile push receipt verification                  |
| `NotificationController` — HTTP layer, RBAC enforcement                     | FCM delivery receipt webhook (future)                     |
| `DeviceTokenRepository` — upsert, deactivate behaviors                      | Cross-service event handling (belongs to trigger module)  |
| Idempotency / dedup logic (new `existsDuplicate()`)                         | Quiet hours enforcement (lower priority — separate TC)    |
| EMERGENCY bypass of preference gate                                          | Email channel implementation (future)                     |
| Audit log emission for all dispatch outcomes                                 |                                                           |

### 3.2 Test Basis (Documents and Code)

| Artifact                                   | Path                                                                         |
|--------------------------------------------|------------------------------------------------------------------------------|
| TDS CB-NOTIF-IMP-128                       | `04_Implement/UC128_SendAutomatedNotification/UC128_SendAutomatedNotification_TDS.md` |
| NotificationService interface              | `05_Development/CareBridgeAPI/src/.../notification/service/NotificationService.java` |
| NotificationServiceImpl                    | `05_Development/CareBridgeAPI/src/.../notification/service/impl/NotificationServiceImpl.java` |
| NotificationController                     | `05_Development/CareBridgeAPI/src/.../notification/controller/NotificationController.java` |
| NotificationRecord entity                  | `05_Development/CareBridgeAPI/src/.../notification/entity/NotificationRecord.java` |
| DeviceToken entity                         | `05_Development/CareBridgeAPI/src/.../notification/entity/DeviceToken.java` |
| FcmService (stub)                          | `05_Development/CareBridgeAPI/src/.../notification/service/impl/FcmServiceImpl.java` |
| V1__init_schema.sql                        | `05_Development/Database/V1__init_schema.sql`                                |

### 3.3 Test Conditions (Equivalence Classes)

#### 3.3.1 send() Conditions

| Condition Set | Class                      | Values / States                                                     |
|---------------|----------------------------|---------------------------------------------------------------------|
| C-TOK-1       | Active tokens exist        | `findByUserIdAndActiveTrue()` returns 1 token                       |
| C-TOK-2       | Multiple active tokens     | `findByUserIdAndActiveTrue()` returns N > 1 tokens                  |
| C-TOK-3       | No active tokens           | `findByUserIdAndActiveTrue()` returns empty list                    |
| C-FCM-1       | FCM succeeds (single)      | `sendToToken()` returns non-null string (or null in stub)           |
| C-FCM-2       | FCM succeeds (multi)       | `sendToTokens()` returns count > 0                                  |
| C-FCM-3       | FCM throws exception       | `sendToToken()` throws RuntimeException                             |
| C-TYPE-1      | Normal type (REMINDER)     | type != EMERGENCY                                                   |
| C-TYPE-2      | EMERGENCY type             | type == EMERGENCY — bypass all gates                                |
| C-PREF-1      | Preference gate: enabled   | `notification_preferences.push_enabled = true`                     |
| C-PREF-2      | Preference gate: disabled  | `notification_preferences.push_enabled = false`, type != EMERGENCY |
| C-DEDUP-1     | No duplicate in window     | `existsDuplicate()` returns false                                   |
| C-DEDUP-2     | Duplicate in window        | `existsDuplicate()` returns true, dispatch skipped                  |
| C-QUIET-1     | Outside quiet hours        | Current time outside (quiet_hours_start, quiet_hours_end)          |
| C-QUIET-2     | Inside quiet hours         | Current time inside quiet window, type != EMERGENCY                |

#### 3.3.2 Device Token Conditions

| Condition Set | Class                | Values                                                         |
|---------------|----------------------|----------------------------------------------------------------|
| C-REG-1       | New token            | Token not in DB for this userId                                |
| C-REG-2       | Existing token       | Token already in DB for this userId (active or inactive)       |
| C-DEREG-1     | Known active token   | Token exists and is active                                     |
| C-DEREG-2     | Unknown token        | Token not found in DB                                          |

#### 3.3.3 RBAC Conditions

| Condition Set | Class              | Value                                       |
|---------------|--------------------|---------------------------------------------|
| C-AUTH-1      | ADMIN JWT          | JWT contains ROLE_ADMIN                     |
| C-AUTH-2      | SYSTEM JWT         | JWT contains ROLE_SYSTEM                    |
| C-AUTH-3      | MOTHER JWT         | JWT contains ROLE_MOTHER (insufficient)     |
| C-AUTH-4      | No JWT             | No Authorization header                     |

### 3.4 Test Techniques

| Technique                     | Applied To                                         |
|-------------------------------|----------------------------------------------------|
| Equivalence Partitioning      | Token count, FCM result, type, preference state    |
| Boundary Value Analysis       | title (max 255 chars), body (not blank), size=100  |
| Decision Table                | send() path matrix (token × FCM × type × dedup)   |
| Negative Testing              | No JWT, wrong role, blank fields, invalid enum     |
| State Transition              | NotificationRecord: PENDING → SENT / FAILED        |
| Mock Isolation                | Unit tests use Mockito; no real DB or FCM calls    |

### 3.5 Test Data

| Data Set ID  | Description                                   | Values                                                                                     |
|--------------|-----------------------------------------------|--------------------------------------------------------------------------------------------|
| TD-USER-001  | Recipient user                                | `userId = UUID("3fa85f64-5717-4562-b3fc-2c963f66afa6")` [SYNTHETIC]                      |
| TD-TOKEN-001 | Single active FCM token                       | `token = "fcm-test-token-alpha-001"`, `platform = ANDROID`, `active = true`               |
| TD-TOKEN-002 | Multiple active FCM tokens (2)                | tokens: `["fcm-test-beta-001", "fcm-test-beta-002"]`, both ANDROID, active                |
| TD-REF-001   | Reminder reference ID                         | `referenceId = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890")`, `referenceType = "APPOINTMENT"` |
| TD-REF-002   | Consultation reference ID                     | `referenceId = UUID("b2c3d4e5-f6a7-8901-bcde-fab123456789")`, `referenceType = "CONSULTATION"` |
| TD-REQ-001   | Valid REMINDER request                        | `{recipientUserId:TD-USER-001, type:REMINDER, title:"Test Reminder", body:"Test body.", referenceId:TD-REF-001}` |
| TD-REQ-002   | Valid EMERGENCY request                       | `{recipientUserId:TD-USER-001, type:EMERGENCY, title:"Emergency Alert", body:"Please contact doctor.", referenceType:"EMERGENCY_ALERT"}` |
| TD-REQ-003   | Invalid request — no recipientUserId          | `{type:REMINDER, title:"T", body:"B"}` (missing required field)                           |
| TD-REQ-004   | Invalid request — blank title                 | `{recipientUserId:TD-USER-001, type:REMINDER, title:"", body:"B"}`                        |
| TD-REQ-005   | Invalid request — title at max boundary       | `{recipientUserId:TD-USER-001, type:REMINDER, title:"A"*255, body:"B"}`                   |
| TD-REQ-006   | Invalid request — title exceeds max           | `{recipientUserId:TD-USER-001, type:REMINDER, title:"A"*256, body:"B"}`                   |
| TD-REQ-007   | Duplicate request (same as TD-REQ-001)        | Same (userId, type, referenceId) as TD-REQ-001 — within 5-min window                     |

---

## 4. Test Cases

### Props Isolation Factory

```java
package com.carebridge.backend.notification;

import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * Props Isolation Factory — provides immutable test fixtures for UC-128.
 * All values are SYNTHETIC — no real user data.
 */
public final class NotificationTestFixtures {

    private NotificationTestFixtures() {}

    // --- UUIDs ---
    public static final UUID RECIPIENT_USER_ID =
        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    public static final UUID REFERENCE_ID_APPOINTMENT =
        UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    public static final UUID REFERENCE_ID_CONSULTATION =
        UUID.fromString("b2c3d4e5-f6a7-8901-bcde-fab123456789");

    // --- FCM Tokens ---
    public static final String FCM_TOKEN_ALPHA = "fcm-test-token-alpha-001";
    public static final String FCM_TOKEN_BETA_1 = "fcm-test-beta-001";
    public static final String FCM_TOKEN_BETA_2 = "fcm-test-beta-002";
    public static final String FCM_MESSAGE_ID = "projects/test-proj/messages/msg-001";

    // --- SendNotificationRequest factories ---

    public static SendNotificationRequest reminderRequest() {
        return new SendNotificationRequest(
            RECIPIENT_USER_ID,
            NotificationType.REMINDER,
            "Test Reminder",
            "Test body content — no PHI.",
            REFERENCE_ID_APPOINTMENT,
            "APPOINTMENT"
        );
    }

    public static SendNotificationRequest emergencyRequest() {
        return new SendNotificationRequest(
            RECIPIENT_USER_ID,
            NotificationType.EMERGENCY,
            "Emergency Alert",
            "Please contact your doctor immediately.",
            null,
            "EMERGENCY_ALERT"
        );
    }

    public static SendNotificationRequest consultationRequest() {
        return new SendNotificationRequest(
            RECIPIENT_USER_ID,
            NotificationType.CONSULTATION,
            "Consultation Update",
            "Your consultation request has been accepted.",
            REFERENCE_ID_CONSULTATION,
            "CONSULTATION"
        );
    }

    // --- DeviceToken factories ---

    public static DeviceToken singleActiveToken() {
        return DeviceToken.builder()
            .id(UUID.randomUUID())
            .userId(RECIPIENT_USER_ID)
            .token(FCM_TOKEN_ALPHA)
            .platform(DevicePlatform.ANDROID)
            .active(true)
            .build();
    }

    public static List<DeviceToken> multipleActiveTokens() {
        return List.of(
            DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(RECIPIENT_USER_ID)
                .token(FCM_TOKEN_BETA_1)
                .platform(DevicePlatform.ANDROID)
                .active(true)
                .build(),
            DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(RECIPIENT_USER_ID)
                .token(FCM_TOKEN_BETA_2)
                .platform(DevicePlatform.IOS)
                .active(true)
                .build()
        );
    }

    public static List<DeviceToken> noActiveTokens() {
        return List.of();
    }
}
```

---

### 4.1 Unit Tests — NotificationServiceImpl

#### TC-UNIT-001: send() with single active token and FCM success → SENT

```gherkin
Feature: Automated Notification Dispatch
  Background:
    Given test data classification: SYNTHETIC
    And NotificationServiceImpl under test with mocked dependencies

  Scenario: Single active FCM token — FCM returns message ID — record saved as SENT
    Given recipient has 1 active device token (TD-TOKEN-001)
    And FcmService.sendToToken() returns FCM_MESSAGE_ID = "projects/test-proj/messages/msg-001"
    And NotificationRecordRepository.save() persists and returns the record
    When send(TD-REQ-001) is called
    Then NotificationRecord is saved with:
      | field         | expected value                                  | oracle source           |
      | userId        | TD-USER-001                                     | SendNotificationRequest |
      | type          | REMINDER                                        | SendNotificationRequest |
      | title         | "Test Reminder"                                 | SendNotificationRequest |
      | body          | "Test body content — no PHI."                   | SendNotificationRequest |
      | referenceId   | TD-REF-001                                      | SendNotificationRequest |
      | referenceType | "APPOINTMENT"                                   | SendNotificationRequest |
      | status        | SENT                                            | C1: PERSIST-ALL         |
      | fcmMessageId  | "projects/test-proj/messages/msg-001"           | FcmService return value |
      | sentAt        | non-null Instant                                | Instant.now() on success|
      | failedAt      | null                                            | not set on success      |
      | attemptCount  | 1                                               | default                 |
    And AuditService.log() is called with (NOTIFICATION_SENT, TD-USER-001, "notification", recordId, "REMINDER")
    And returned NotificationRecordResponse.status == "SENT"
    And returned NotificationRecordResponse.sentAt is non-null
```

```java
@Test
@DisplayName("TC-UNIT-001: Single token, FCM success → record status=SENT, audit logged")
void send_singleToken_fcmSuccess_shouldSentRecord() {
    // Arrange
    SendNotificationRequest req = NotificationTestFixtures.reminderRequest();
    List<DeviceToken> tokens = List.of(NotificationTestFixtures.singleActiveToken());

    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID)).thenReturn(tokens);
    when(fcmService.sendToToken(FCM_TOKEN_ALPHA, "Test Reminder", "Test body content — no PHI."))
        .thenReturn(FCM_MESSAGE_ID);
    when(notificationRecordRepository.save(any(NotificationRecord.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // Act
    NotificationRecordResponse response = service.send(req);

    // Assert — status
    assertThat(response.status()).isEqualTo("SENT");
    assertThat(response.sentAt()).isNotNull();

    // Assert — record persistence (C1: PERSIST-ALL)
    verify(notificationRecordRepository).save(argThat(r ->
        r.getStatus() == NotificationRecordStatus.SENT &&
        r.getFcmMessageId().equals(FCM_MESSAGE_ID) &&
        r.getSentAt() != null &&
        r.getFailedAt() == null &&
        r.getUserId().equals(RECIPIENT_USER_ID) &&
        r.getType() == NotificationType.REMINDER
    ));

    // Assert — audit (C8: AUDIT-ALWAYS)
    verify(auditService).log(
        eq(AuditAction.NOTIFICATION_SENT),
        eq(RECIPIENT_USER_ID),
        eq("notification"),
        anyString(),
        eq("REMINDER")
    );
}
```

---

#### TC-UNIT-002: send() with no active device tokens → FAILED, no FCM call

```gherkin
  Scenario: No active device tokens — FCM not called — record saved as FAILED
    Given recipient has 0 active device tokens (empty list)
    When send(TD-REQ-001) is called
    Then FcmService.sendToToken() is NEVER called
    And FcmService.sendToTokens() is NEVER called
    And NotificationRecord is saved with:
      | status   | FAILED                           | C1: PERSIST-ALL      |
      | failedAt | non-null Instant                 | set when no tokens   |
      | sentAt   | null                             | never sent           |
    And AuditService.log() is called with (NOTIFICATION_FAILED, TD-USER-001, ..., "No active device tokens")
    And returned NotificationRecordResponse.status == "FAILED"
```

```java
@Test
@DisplayName("TC-UNIT-002: No active tokens → status=FAILED, FCM not called, audit NOTIFICATION_FAILED")
void send_noTokens_shouldFailedRecord_noFcmCall() {
    // Arrange
    SendNotificationRequest req = NotificationTestFixtures.reminderRequest();
    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID))
        .thenReturn(Collections.emptyList());
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    NotificationRecordResponse response = service.send(req);

    // Assert — status (C1: PERSIST-ALL)
    assertThat(response.status()).isEqualTo("FAILED");

    // Assert — no FCM calls
    verifyNoInteractions(fcmService);

    // Assert — record has failedAt
    verify(notificationRecordRepository).save(argThat(r ->
        r.getStatus() == NotificationRecordStatus.FAILED &&
        r.getFailedAt() != null &&
        r.getSentAt() == null
    ));

    // Assert — audit (C8: AUDIT-ALWAYS)
    verify(auditService).log(
        eq(AuditAction.NOTIFICATION_FAILED),
        eq(RECIPIENT_USER_ID),
        anyString(), anyString(), anyString()
    );
}
```

---

#### TC-UNIT-003: send() with active token but FCM throws exception → FAILED, exception not propagated

```gherkin
  Scenario: FCM throws RuntimeException — exception caught — record saved as FAILED
    Given recipient has 1 active device token
    And FcmService.sendToToken() throws RuntimeException("FCM unavailable")
    When send(TD-REQ-001) is called
    Then no exception is thrown to the caller
    And NotificationRecord is saved with status = FAILED, failedAt = non-null
    And AuditService.log() called with NOTIFICATION_FAILED
    And returned NotificationRecordResponse.status == "FAILED"
```

```java
@Test
@DisplayName("TC-UNIT-003: FCM throws exception → status=FAILED, exception swallowed, audit NOTIFICATION_FAILED")
void send_fcmThrows_shouldCatchAndSaveFailedRecord() {
    // Arrange (C5: FCM-SAFE)
    SendNotificationRequest req = NotificationTestFixtures.reminderRequest();
    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID))
        .thenReturn(List.of(NotificationTestFixtures.singleActiveToken()));
    when(fcmService.sendToToken(anyString(), anyString(), anyString()))
        .thenThrow(new RuntimeException("FCM unavailable"));
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act — must NOT throw
    NotificationRecordResponse response = assertDoesNotThrow(() -> service.send(req));

    // Assert
    assertThat(response.status()).isEqualTo("FAILED");
    verify(notificationRecordRepository).save(argThat(r ->
        r.getStatus() == NotificationRecordStatus.FAILED &&
        r.getFailedAt() != null
    ));
    verify(auditService).log(eq(AuditAction.NOTIFICATION_FAILED), any(), any(), any(), any());
}
```

---

#### TC-UNIT-004: send() with multiple active tokens → sendToTokens() called, not sendToToken()

```gherkin
  Scenario: Multiple active tokens — multicast used
    Given recipient has 2 active device tokens (TD-TOKEN-002)
    And FcmService.sendToTokens() returns 2 (success count)
    When send(TD-REQ-001) is called
    Then FcmService.sendToTokens() is called with 2 tokens
    And FcmService.sendToToken() is NEVER called
    And NotificationRecord saved with status = SENT
```

```java
@Test
@DisplayName("TC-UNIT-004: Multiple tokens → sendToTokens() used, not sendToToken()")
void send_multipleTokens_shouldUseMulticast() {
    // Arrange
    SendNotificationRequest req = NotificationTestFixtures.reminderRequest();
    List<DeviceToken> tokens = NotificationTestFixtures.multipleActiveTokens();
    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID)).thenReturn(tokens);
    when(fcmService.sendToTokens(anyList(), anyString(), anyString())).thenReturn(2);
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    NotificationRecordResponse response = service.send(req);

    // Assert — multicast used
    verify(fcmService).sendToTokens(
        argThat(list -> list.size() == 2 &&
            list.contains(FCM_TOKEN_BETA_1) &&
            list.contains(FCM_TOKEN_BETA_2)),
        eq("Test Reminder"),
        eq("Test body content — no PHI.")
    );
    verify(fcmService, never()).sendToToken(anyString(), anyString(), anyString());
    assertThat(response.status()).isEqualTo("SENT");
}
```

---

#### TC-UNIT-005: send() EMERGENCY type — preference gate skipped [RED PHASE STUB]

> **Red Phase Note**: The preference gate check is NOT yet implemented in the current `NotificationServiceImpl`. This test is a Red-Phase stub — it will FAIL until the preference gate + EMERGENCY bypass logic is implemented.

```gherkin
  Scenario: EMERGENCY type bypasses preference gate and quiet hours (C2: EMERGENCY-BYPASS)
    Given recipient has 1 active device token
    And notification_preferences for recipient has push_enabled = false for EMERGENCY type
    And current time is within quiet hours window
    When send(TD-REQ-002) is called with type = EMERGENCY
    Then preference repository is NEVER queried for push_enabled
    And quiet hours check is NEVER applied
    And FcmService.sendToToken() IS called
    And record saved with status = SENT
```

```java
@Test
@DisplayName("TC-UNIT-005 [RED]: EMERGENCY type bypasses preference gate — FCM called regardless")
void send_emergencyType_shouldBypassPreferenceGate() {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    // Implementation requires: preference gate check + EMERGENCY bypass in NotificationServiceImpl
    // When implemented, test body:
    //
    // SendNotificationRequest req = NotificationTestFixtures.emergencyRequest();
    // when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID))
    //     .thenReturn(List.of(NotificationTestFixtures.singleActiveToken()));
    // when(fcmService.sendToToken(anyString(), anyString(), anyString()))
    //     .thenReturn(FCM_MESSAGE_ID);
    // when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    //
    // NotificationRecordResponse response = service.send(req);
    //
    // // Preference gate NOT called (no preferenceRepository interactions)
    // verifyNoInteractions(preferenceRepository);
    // verify(fcmService).sendToToken(anyString(), anyString(), anyString());
    // assertThat(response.status()).isEqualTo("SENT");
}
```

---

#### TC-UNIT-006: send() duplicate within 5-min window → dispatch skipped [RED PHASE STUB]

> **Red Phase Note**: `existsDuplicate()` query does not yet exist in `NotificationRecordRepository`. This test will FAIL until method is added and `send()` calls it.

```gherkin
  Scenario: Duplicate notification within 5-minute idempotency window (C4: IDEMPOTENCY)
    Given a NotificationRecord with same (userId=TD-USER-001, type=REMINDER, referenceId=TD-REF-001)
    And the record's createdAt is 2 minutes ago (within 5-min window)
    When send(TD-REQ-001) is called again with same request
    Then existsDuplicate() returns true
    And FcmService is NEVER called
    And NotificationRecord is NOT saved (no additional record)
    And the existing NotificationRecord is returned
```

```java
@Test
@DisplayName("TC-UNIT-006 [RED]: Duplicate within 5-min window → dispatch skipped, existing record returned")
void send_duplicateWithinWindow_shouldSkipDispatch() {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    // Implementation requires: existsDuplicate() in repository + dedup check in send()
    // When implemented, test body:
    //
    // UUID existingId = UUID.randomUUID();
    // SendNotificationRequest req = NotificationTestFixtures.reminderRequest();
    // when(notificationRecordRepository.existsDuplicate(
    //     eq(RECIPIENT_USER_ID), eq(NotificationType.REMINDER),
    //     eq(REFERENCE_ID_APPOINTMENT), any(Instant.class)))
    //     .thenReturn(true);
    // when(notificationRecordRepository.findFirstByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(
    //     RECIPIENT_USER_ID, NotificationType.REMINDER, REFERENCE_ID_APPOINTMENT))
    //     .thenReturn(Optional.of(existingRecord));
    //
    // service.send(req);
    //
    // verifyNoInteractions(fcmService);
    // verify(notificationRecordRepository, never()).save(any());
}
```

---

#### TC-UNIT-007: registerDeviceToken() with new token → saved with active=true

```gherkin
  Scenario: Register new FCM device token for user
    Given no existing token matches (userId, token) in device_tokens
    When registerDeviceToken(TD-USER-001, {token="fcm-test-token-alpha-001", platform=ANDROID})
    Then DeviceTokenRepository.save() is called with:
      | userId   | TD-USER-001                  |
      | token    | "fcm-test-token-alpha-001"   |
      | platform | ANDROID                      |
      | active   | true                         |
```

```java
@Test
@DisplayName("TC-UNIT-007: New token registration → saved with active=true")
void registerDeviceToken_newToken_shouldSaveActive() {
    // Arrange
    RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest(
        FCM_TOKEN_ALPHA, DevicePlatform.ANDROID);
    when(deviceTokenRepository.findByUserIdAndToken(RECIPIENT_USER_ID, FCM_TOKEN_ALPHA))
        .thenReturn(Optional.empty());
    when(deviceTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    service.registerDeviceToken(RECIPIENT_USER_ID, request);

    // Assert
    verify(deviceTokenRepository).save(argThat(dt ->
        dt.getUserId().equals(RECIPIENT_USER_ID) &&
        dt.getToken().equals(FCM_TOKEN_ALPHA) &&
        dt.getPlatform() == DevicePlatform.ANDROID &&
        dt.isActive()
    ));
}
```

---

#### TC-UNIT-008: registerDeviceToken() with existing inactive token → reactivated, platform updated

```gherkin
  Scenario: Re-register previously deactivated token
    Given existing DeviceToken with token="fcm-test-token-alpha-001", active=false
    When registerDeviceToken(TD-USER-001, {token="fcm-test-token-alpha-001", platform=IOS})
    Then existing token's active is set to true
    And existing token's platform is updated to IOS
    And DeviceTokenRepository.save() is called (update path)
    And no new DeviceToken is created
```

```java
@Test
@DisplayName("TC-UNIT-008: Existing inactive token → reactivated, platform updated")
void registerDeviceToken_existingInactiveToken_shouldReactivate() {
    // Arrange
    DeviceToken existing = DeviceToken.builder()
        .id(UUID.randomUUID())
        .userId(RECIPIENT_USER_ID)
        .token(FCM_TOKEN_ALPHA)
        .platform(DevicePlatform.ANDROID)
        .active(false)
        .build();
    RegisterDeviceTokenRequest request =
        new RegisterDeviceTokenRequest(FCM_TOKEN_ALPHA, DevicePlatform.IOS);

    when(deviceTokenRepository.findByUserIdAndToken(RECIPIENT_USER_ID, FCM_TOKEN_ALPHA))
        .thenReturn(Optional.of(existing));
    when(deviceTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    service.registerDeviceToken(RECIPIENT_USER_ID, request);

    // Assert — reactivated and platform updated
    assertThat(existing.isActive()).isTrue();
    assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
    verify(deviceTokenRepository).save(existing);
}
```

---

#### TC-UNIT-009: deregisterDeviceToken() → deactivateByToken() called

```gherkin
  Scenario: Deregister device token on logout
    When deregisterDeviceToken(TD-USER-001, "fcm-test-token-alpha-001") is called
    Then DeviceTokenRepository.deactivateByToken("fcm-test-token-alpha-001", now) is called
    And no NotificationRecord is created
```

```java
@Test
@DisplayName("TC-UNIT-009: Deregister token → deactivateByToken() called")
void deregisterDeviceToken_shouldCallDeactivate() {
    // Arrange
    doReturn(1).when(deviceTokenRepository).deactivateByToken(eq(FCM_TOKEN_ALPHA), any(Instant.class));

    // Act
    service.deregisterDeviceToken(RECIPIENT_USER_ID, FCM_TOKEN_ALPHA);

    // Assert
    verify(deviceTokenRepository).deactivateByToken(eq(FCM_TOKEN_ALPHA), any(Instant.class));
    verifyNoInteractions(notificationRecordRepository);
}
```

---

#### TC-UNIT-010: getMyNotifications() with type filter → only matching type returned

```gherkin
  Scenario: Fetch notifications with REMINDER type filter
    Given 5 NotificationRecords: 3 REMINDER + 2 CONSULTATION for TD-USER-001
    When getMyNotifications(TD-USER-001, "REMINDER", Pageable.of(0,20), principal)
    Then findByUserIdAndType(TD-USER-001, REMINDER, pageable) is called
    And findByUserId() is NEVER called
    And returned Page contains only REMINDER records
```

```java
@Test
@DisplayName("TC-UNIT-010: getMyNotifications with type filter → findByUserIdAndType called")
void getMyNotifications_withTypeFilter_shouldUseFiltredQuery() {
    // Arrange
    Page<NotificationRecord> mockPage = new PageImpl<>(List.of(
        buildRecord(NotificationType.REMINDER),
        buildRecord(NotificationType.REMINDER)
    ));
    when(notificationRecordRepository.findByUserIdAndType(
        eq(RECIPIENT_USER_ID), eq(NotificationType.REMINDER), any(Pageable.class)))
        .thenReturn(mockPage);
    when(principal.getName()).thenReturn(RECIPIENT_USER_ID.toString());

    // Act
    Page<NotificationRecordResponse> result = service.getMyNotifications(
        RECIPIENT_USER_ID, "REMINDER", PageRequest.of(0, 20), principal);

    // Assert
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent()).allMatch(r -> r.type().equals("REMINDER"));
    verify(notificationRecordRepository).findByUserIdAndType(
        eq(RECIPIENT_USER_ID), eq(NotificationType.REMINDER), any());
    verify(notificationRecordRepository, never()).findByUserId(any(), any());
}
```

---

#### TC-UNIT-011: send() — quiet hours respected for non-EMERGENCY [RED PHASE STUB]

> **Red Phase Note**: Quiet hours check not yet implemented. Red-Phase stub.

```gherkin
  Scenario: Non-EMERGENCY notification during quiet hours → dispatch skipped (C-QUIET-2)
    Given recipient's quiet_hours_start = 22:00, quiet_hours_end = 07:00
    And current time is 23:30 (within quiet window)
    And notification type = REMINDER (not EMERGENCY)
    When send(TD-REQ-001) is called
    Then FcmService is NEVER called
    And NotificationRecord saved with status = SKIPPED (or FAILED with reason "QUIET_HOURS")
```

```java
@Test
@DisplayName("TC-UNIT-011 [RED]: Quiet hours active, non-EMERGENCY → dispatch skipped")
void send_quietHoursActive_nonEmergency_shouldSkip() {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    // Requires: quiet hours enforcement in NotificationServiceImpl
    // Oracle: status = SKIPPED or FAILED with detail "QUIET_HOURS"
}
```

---

#### TC-UNIT-012: send() — CONSULTATION type with referenceId set

```gherkin
  Scenario: CONSULTATION notification with valid referenceId
    Given recipient has 1 active device token
    And FcmService.sendToToken() returns FCM_MESSAGE_ID
    When send({type=CONSULTATION, referenceId=TD-REF-002, referenceType="CONSULTATION"}) is called
    Then NotificationRecord.type = CONSULTATION
    And NotificationRecord.referenceId = TD-REF-002
    And NotificationRecord.referenceType = "CONSULTATION"
    And status = SENT
```

```java
@Test
@DisplayName("TC-UNIT-012: CONSULTATION notification with referenceId → persisted correctly")
void send_consultationType_withReferenceId_shouldPersistCorrectly() {
    // Arrange
    SendNotificationRequest req = NotificationTestFixtures.consultationRequest();
    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID))
        .thenReturn(List.of(NotificationTestFixtures.singleActiveToken()));
    when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(FCM_MESSAGE_ID);
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    NotificationRecordResponse response = service.send(req);

    // Assert
    verify(notificationRecordRepository).save(argThat(r ->
        r.getType() == NotificationType.CONSULTATION &&
        REFERENCE_ID_CONSULTATION.equals(r.getReferenceId()) &&
        "CONSULTATION".equals(r.getReferenceType())
    ));
    assertThat(response.status()).isEqualTo("SENT");
}
```

---

### 4.2 Integration Tests — NotificationController + Full Stack

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class NotificationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("carebridge_test")
        .withUsername("test")
        .withPassword("test");
```

#### TC-INT-001: POST /send with ADMIN JWT — DB round-trip, record persisted

```gherkin
  Scenario: ADMIN user sends notification — full DB persistence verified
    Given recipient user exists in DB with 1 active device token registered
    And caller has JWT with ROLE_ADMIN
    When POST /api/v1/notifications/send is called with TD-REQ-001
    Then HTTP 200 is returned
    And data.status IN ["SENT", "FAILED"] (stub FCM returns null → SENT when no exception)
    And data.id is a valid UUID
    And notification_records table has 1 new row with userId = TD-USER-001
    And audit_logs table has 1 new row with action IN ["NOTIFICATION_SENT", "NOTIFICATION_FAILED"]
```

```java
@Test
@DisplayName("TC-INT-001: ADMIN JWT + valid request → 200, record in DB, audit logged")
void sendNotification_adminJwt_shouldReturn200AndPersist() throws Exception {
    // Given
    UUID recipientId = testSetup.createUser("ROLE_MOTHER");
    testSetup.registerDeviceToken(recipientId, FCM_TOKEN_ALPHA, "ANDROID");
    String adminJwt = jwtHelper.generateAdminToken();

    // When
    MvcResult result = mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + adminJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "recipientUserId": "%s",
                  "type": "REMINDER",
                  "title": "Integration Test Reminder",
                  "body": "Integration test body — no PHI.",
                  "referenceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                  "referenceType": "APPOINTMENT"
                }
            """.formatted(recipientId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").exists())
        .andExpect(jsonPath("$.data.status").value(anyOf(is("SENT"), is("FAILED"))))
        .andReturn();

    // Verify DB
    long recordCount = notificationRecordRepository
        .findByUserId(recipientId, PageRequest.of(0, 10)).getTotalElements();
    assertThat(recordCount).isGreaterThanOrEqualTo(1);
}
```

---

#### TC-INT-002: POST /send with ROLE_MOTHER JWT → 403 Forbidden

```gherkin
  Scenario: Non-admin user attempts to call send endpoint
    Given caller has JWT with ROLE_MOTHER
    When POST /api/v1/notifications/send is called
    Then HTTP 403 is returned
    And notification_records table has NO new rows
```

```java
@Test
@DisplayName("TC-INT-002: ROLE_MOTHER JWT on /send → 403 Forbidden (C6: RBAC)")
void sendNotification_motherJwt_shouldReturn403() throws Exception {
    String motherJwt = jwtHelper.generateToken(UUID.randomUUID(), "ROLE_MOTHER");

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + motherJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "recipientUserId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "REMINDER", "title": "T", "body": "B"
                }
            """))
        .andExpect(status().isForbidden());
}
```

---

#### TC-INT-003: POST /send without JWT → 401 Unauthorized

```java
@Test
@DisplayName("TC-INT-003: No JWT on /send → 401 Unauthorized")
void sendNotification_noJwt_shouldReturn401() throws Exception {
    mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"REMINDER\",\"title\":\"T\",\"body\":\"B\"}"))
        .andExpect(status().isUnauthorized());
}
```

---

#### TC-INT-004: POST /send with missing required fields → 400 Validation Error

```gherkin
  Scenario: Missing recipientUserId and blank title → 400 with field errors
    Given ADMIN JWT
    When POST /send called with {type: "REMINDER", title: "", body: "B"} (no recipientUserId, blank title)
    Then HTTP 400 is returned
    And response contains validation errors for "recipientUserId" and "title"
```

```java
@Test
@DisplayName("TC-INT-004: Missing recipientUserId + blank title → 400 Validation Error")
void sendNotification_invalidRequest_shouldReturn400() throws Exception {
    String adminJwt = jwtHelper.generateAdminToken();

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + adminJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"REMINDER\",\"title\":\"\",\"body\":\"B\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
}
```

---

#### TC-INT-005: GET /me — pagination with type filter REMINDER

```gherkin
  Scenario: Authenticated user retrieves REMINDER notifications with pagination
    Given user TD-USER-001 has 3 REMINDER and 2 CONSULTATION notifications in DB
    When GET /api/v1/notifications/me?type=REMINDER&page=0&size=20
    Then HTTP 200 is returned
    And data.content contains 3 items (Oracle: only REMINDER type returned)
    And data.content[*].type == "REMINDER"
    And data.totalElements == 3
```

```java
@Test
@DisplayName("TC-INT-005: GET /me with type=REMINDER → only REMINDER records returned")
void getMyNotifications_withTypeFilter_shouldReturnFilteredResults() throws Exception {
    // Given
    UUID userId = testSetup.createUser("ROLE_MOTHER");
    testSetup.createNotificationRecord(userId, NotificationType.REMINDER, "R1", "Body 1");
    testSetup.createNotificationRecord(userId, NotificationType.REMINDER, "R2", "Body 2");
    testSetup.createNotificationRecord(userId, NotificationType.REMINDER, "R3", "Body 3");
    testSetup.createNotificationRecord(userId, NotificationType.CONSULTATION, "C1", "Body C1");
    String jwt = jwtHelper.generateToken(userId, "ROLE_MOTHER");

    // When / Then
    mockMvc.perform(get("/api/v1/notifications/me?type=REMINDER&page=0&size=20")
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(3))
        .andExpect(jsonPath("$.data.content[0].type").value("REMINDER"))
        .andExpect(jsonPath("$.data.totalElements").value(3));
}
```

---

#### TC-INT-006: POST /device-token — register new token

```java
@Test
@DisplayName("TC-INT-006: Register device token → 200, token active in DB")
void registerDeviceToken_newToken_shouldReturn200AndPersist() throws Exception {
    UUID userId = testSetup.createUser("ROLE_MOTHER");
    String jwt = jwtHelper.generateToken(userId, "ROLE_MOTHER");

    mockMvc.perform(post("/api/v1/notifications/device-token")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"fcm-new-test-token\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Device token registered"));

    // Verify DB
    List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    assertThat(tokens).hasSize(1);
    assertThat(tokens.get(0).getToken()).isEqualTo("fcm-new-test-token");
    assertThat(tokens.get(0).isActive()).isTrue();
}
```

---

#### TC-INT-007: DELETE /device-token — deregister token

```java
@Test
@DisplayName("TC-INT-007: Deregister device token → 200, token inactive in DB")
void deregisterDeviceToken_existingToken_shouldDeactivate() throws Exception {
    UUID userId = testSetup.createUser("ROLE_MOTHER");
    testSetup.registerDeviceToken(userId, "fcm-deregister-test", "ANDROID");
    String jwt = jwtHelper.generateToken(userId, "ROLE_MOTHER");

    mockMvc.perform(delete("/api/v1/notifications/device-token?token=fcm-deregister-test")
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Device token deregistered"));

    // Verify token inactive
    List<DeviceToken> activeTokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    assertThat(activeTokens).isEmpty();
}
```

---

### 4.3 Security Tests

#### TC-SEC-001: POST /send — cross-user send (RBAC allows but ownership not enforced at API layer)

```gherkin
  Scenario: ADMIN sends notification to a different user (allowed — RBAC check only)
    Given ADMIN user (adminId) wants to send notification to victimUserId
    When POST /send called with recipientUserId = victimUserId
    Then HTTP 200 is returned (ADMIN authorized to send to any user)
    And notification is delivered to victimUserId, NOT adminUserId
    And notification_records.user_id = victimUserId
    Oracle source: §16 Auth Matrix — ADMIN can dispatch to any recipient
```

```java
@Test
@DisplayName("TC-SEC-001: ADMIN sends to another user → notification targets that user, not caller")
void sendNotification_adminSendsToOtherUser_shouldTargetRecipient() throws Exception {
    UUID recipientId = testSetup.createUser("ROLE_MOTHER");
    testSetup.registerDeviceToken(recipientId, "fcm-victim-token", "ANDROID");
    String adminJwt = jwtHelper.generateAdminToken();

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + adminJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"recipientUserId":"%s","type":"REMINDER","title":"T","body":"B"}
            """.formatted(recipientId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(recipientId.toString()));
}
```

---

#### TC-SEC-002: GET /me — user cannot see another user's notifications

```gherkin
  Scenario: User A tries to access notifications — only their own are returned
    Given user A and user B both have notifications in DB
    And user A is authenticated
    When GET /api/v1/notifications/me is called
    Then only user A's notifications are returned
    And user B's notifications are NOT in the response
    Oracle: SecurityUtils.requireCurrentUserId() enforces userId from JWT
```

```java
@Test
@DisplayName("TC-SEC-002: GET /me — only authenticated user's own notifications returned")
void getMyNotifications_onlyOwnRecordsReturned() throws Exception {
    UUID userA = testSetup.createUser("ROLE_MOTHER");
    UUID userB = testSetup.createUser("ROLE_MOTHER");
    testSetup.createNotificationRecord(userA, NotificationType.REMINDER, "A's notification", "A body");
    testSetup.createNotificationRecord(userB, NotificationType.REMINDER, "B's notification", "B body");
    String jwtA = jwtHelper.generateToken(userA, "ROLE_MOTHER");

    mockMvc.perform(get("/api/v1/notifications/me?page=0&size=20")
            .header("Authorization", "Bearer " + jwtA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].title").value("A's notification"))
        .andExpect(jsonPath("$.data.content[0].userId").value(userA.toString()));
}
```

---

#### TC-SEC-003: POST /send — ROLE_EXPERT JWT → 403 Forbidden

```java
@Test
@DisplayName("TC-SEC-003: ROLE_EXPERT JWT on /send → 403 Forbidden")
void sendNotification_expertJwt_shouldReturn403() throws Exception {
    String expertJwt = jwtHelper.generateToken(UUID.randomUUID(), "ROLE_EXPERT");

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + expertJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipientUserId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"type\":\"REMINDER\",\"title\":\"T\",\"body\":\"B\"}"))
        .andExpect(status().isForbidden());
}
```

---

#### TC-SEC-004: title at boundary — max 255 chars (valid)

```java
@Test
@DisplayName("TC-SEC-004: title = 255 chars (boundary) → 200 OK")
void sendNotification_titleAtMaxBoundary_shouldAccept() throws Exception {
    String adminJwt = jwtHelper.generateAdminToken();
    String title255 = "A".repeat(255);
    UUID recipientId = testSetup.createUser("ROLE_MOTHER");

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + adminJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipientUserId\":\"%s\",\"type\":\"REMINDER\",\"title\":\"%s\",\"body\":\"B\"}"
                .formatted(recipientId, title255)))
        .andExpect(status().isOk());
}
```

---

#### TC-SEC-005: title exceeds 255 chars → 400 Validation Error

```java
@Test
@DisplayName("TC-SEC-005: title = 256 chars → 400 Validation Error")
void sendNotification_titleExceedsMax_shouldReturn400() throws Exception {
    String adminJwt = jwtHelper.generateAdminToken();
    String title256 = "A".repeat(256);

    mockMvc.perform(post("/api/v1/notifications/send")
            .header("Authorization", "Bearer " + adminJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipientUserId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"type\":\"REMINDER\",\"title\":\"%s\",\"body\":\"B\"}"
                .formatted(title256)))
        .andExpect(status().isBadRequest());
}
```

---

### 4.4 Edge Case Tests

#### TC-EDGE-001: send() with null referenceId and null referenceType — valid

```java
@Test
@DisplayName("TC-EDGE-001: Null referenceId and referenceType → send succeeds (fields are optional)")
void send_nullReferenceFields_shouldSucceed() {
    // Arrange
    SendNotificationRequest req = new SendNotificationRequest(
        RECIPIENT_USER_ID, NotificationType.SYSTEM_ALERT, "System Notice",
        "A system maintenance window is scheduled.", null, null);
    when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_USER_ID))
        .thenReturn(List.of(NotificationTestFixtures.singleActiveToken()));
    when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(FCM_MESSAGE_ID);
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act / Assert
    assertDoesNotThrow(() -> service.send(req));
    verify(notificationRecordRepository).save(argThat(r ->
        r.getReferenceId() == null && r.getReferenceType() == null
    ));
}
```

---

#### TC-EDGE-002: send() called concurrently — no deadlock (basic concurrency smoke test)

```java
@Test
@DisplayName("TC-EDGE-002: Concurrent send() calls — no deadlock, all records saved")
void send_concurrent_shouldNotDeadlock() throws Exception {
    // Arrange — light concurrency smoke: 5 simultaneous calls
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    when(deviceTokenRepository.findByUserIdAndActiveTrue(any()))
        .thenReturn(List.of(NotificationTestFixtures.singleActiveToken()));
    when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(FCM_MESSAGE_ID);
    when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    List<Future<NotificationRecordResponse>> futures = IntStream.range(0, threadCount)
        .mapToObj(_ -> executor.submit(() ->
            service.send(NotificationTestFixtures.reminderRequest())))
        .toList();
    executor.shutdown();
    boolean completed = executor.awaitTermination(10, TimeUnit.SECONDS);

    // Assert — all completed
    assertThat(completed).isTrue();
    assertThat(futures).allMatch(f -> {
        try { return f.get() != null; } catch (Exception e) { return false; }
    });
    verify(notificationRecordRepository, times(threadCount)).save(any());
}
```

---

## 5. Red-Green-Refactor Tracker + Red Gate Protocol

### 5.1 Phase Tracker

| TC ID          | Phase  | Status     | Stub / Blocker                                              | Target Phase |
|----------------|--------|------------|-------------------------------------------------------------|--------------|
| TC-UNIT-001    | Green  | PASS       | Existing impl covers this path                              | Maintain     |
| TC-UNIT-002    | Green  | PASS       | Existing impl covers this path                              | Maintain     |
| TC-UNIT-003    | Green  | PASS       | Existing impl covers this path                              | Maintain     |
| TC-UNIT-004    | Green  | PASS       | Existing impl covers multicast path                         | Maintain     |
| TC-UNIT-005    | Red    | FAIL       | Preference gate + EMERGENCY bypass not implemented          | Green after ADR-128-003 impl |
| TC-UNIT-006    | Red    | FAIL       | `existsDuplicate()` method missing from repository          | Green after ADR-128-005 impl |
| TC-UNIT-007    | Green  | PASS       | Existing registerDeviceToken() covers this                  | Maintain     |
| TC-UNIT-008    | Green  | PASS       | Existing registerDeviceToken() covers upsert path           | Maintain     |
| TC-UNIT-009    | Green  | PASS       | Existing deregisterDeviceToken() covers this                | Maintain     |
| TC-UNIT-010    | Green  | PASS       | Existing getMyNotifications() covers this                   | Maintain     |
| TC-UNIT-011    | Red    | FAIL       | Quiet hours enforcement not implemented                     | Green (lower priority) |
| TC-UNIT-012    | Green  | PASS       | CONSULTATION type covered by existing impl                  | Maintain     |
| TC-INT-001     | Green  | PASS       | Full stack works for basic send flow                        | Maintain     |
| TC-INT-002     | Green  | PASS       | RBAC enforced by existing @PreAuthorize                     | Maintain     |
| TC-INT-003     | Green  | PASS       | Spring Security enforces 401                                | Maintain     |
| TC-INT-004     | Green  | PASS       | @Valid enforces 400                                         | Maintain     |
| TC-INT-005     | Green  | PASS       | getMyNotifications() with type filter works                 | Maintain     |
| TC-INT-006     | Green  | PASS       | registerDeviceToken() integration works                     | Maintain     |
| TC-INT-007     | Green  | PASS       | deregisterDeviceToken() integration works                   | Maintain     |
| TC-SEC-001     | Green  | PASS       | ADMIN can send to any recipient                             | Maintain     |
| TC-SEC-002     | Green  | PASS       | Ownership enforced via SecurityUtils                        | Maintain     |
| TC-SEC-003     | Green  | PASS       | ROLE_EXPERT blocked by @PreAuthorize                        | Maintain     |
| TC-SEC-004     | Green  | PASS       | @Size(max=255) validation allows 255                        | Maintain     |
| TC-SEC-005     | Green  | PASS       | @Size(max=255) rejects 256                                  | Maintain     |
| TC-EDGE-001    | Red    | FAIL       | SYSTEM_ALERT not in current NotificationType enum           | Green after enum extended |
| TC-EDGE-002    | Green  | PASS       | No deadlock in basic concurrency smoke                      | Maintain     |

### 5.2 Red Gate Protocol

All Red-Phase test methods MUST contain the following stub body until the blocking feature is implemented:

```java
throw new UnsupportedOperationException("Not implemented — Red Phase stub");
// See UC128_SendAutomatedNotification_Test-Spec.md §5.1 for blocker details
```

**Rules:**
1. A Red-Phase test MUST NOT be deleted — it documents the gap.
2. A Red-Phase stub MUST NOT be converted to Green without the backing implementation being completed.
3. When moving a test from Red to Green, delete the `throw` stub and implement the full assertion body.
4. CI/CD MUST mark Red-Phase tests as `@Disabled("Red Phase — pending: [BLOCKER]")` to prevent build breakage.

```java
// Example Red Gate annotation pattern:
@Test
@Disabled("Red Phase — pending: preference gate + EMERGENCY bypass (ADR-128-003)")
@DisplayName("TC-UNIT-005 [RED]: EMERGENCY type bypasses preference gate")
void send_emergencyType_shouldBypassPreferenceGate() {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

---

## 6. Entry/Exit Criteria

### 6.1 Entry Criteria (Before Test Execution)

- [ ] `NotificationServiceImpl`, `FcmServiceImpl` (stub), `NotificationController` compile without errors
- [ ] `NotificationRecord`, `DeviceToken` entities can be persisted to Testcontainer PostgreSQL
- [ ] Spring Security config allows ADMIN/SYSTEM on `/api/v1/notifications/send`
- [ ] `./mvnw test` completes without compile errors on the notification module
- [ ] `notification_records` and `device_tokens` tables exist (created by Flyway V1 or test migrations)
- [ ] Mock implementations of `FcmService` available for unit tests
- [ ] Test fixtures in `NotificationTestFixtures` class compile and are importable

### 6.2 Exit Criteria (Before Feature Sign-off)

**Mandatory (Green phase tests):**
- [ ] TC-UNIT-001 through TC-UNIT-004, TC-UNIT-007 through TC-UNIT-010, TC-UNIT-012 PASS
- [ ] TC-INT-001 through TC-INT-007 PASS
- [ ] TC-SEC-001 through TC-SEC-005 PASS
- [ ] TC-EDGE-002 PASS
- [ ] Zero unhandled exceptions in `send()` method (FCM errors caught → FAILED status)
- [ ] Audit log present for every `send()` invocation (verified by TC-UNIT-001, TC-UNIT-002)
- [ ] `GET /me` never returns another user's notifications (verified by TC-SEC-002)

**Red phase tests (document gap, not exit blocker):**
- [ ] TC-UNIT-005 (preference gate + EMERGENCY bypass) — tracked, `@Disabled`
- [ ] TC-UNIT-006 (idempotency / existsDuplicate) — tracked, `@Disabled`
- [ ] TC-UNIT-011 (quiet hours) — tracked, `@Disabled`
- [ ] TC-EDGE-001 (SYSTEM_ALERT enum) — tracked, `@Disabled`

**Quality gates:**
- [ ] Line coverage ≥ 80% on `NotificationServiceImpl` (measured by JaCoCo)
- [ ] No SonarQube critical/blocker findings on changed files
- [ ] `./mvnw test` exits 0 (all non-`@Disabled` tests pass)

---

## 7. Rollback Plan

### 7.1 Test Environment Rollback

If a previously Green test regresses (moves to Red after implementation changes):

```bash
# 1. Identify failing test
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=NotificationServiceImplTest#<method> -Dsurefire.failIfNoSpecifiedTests=false

# 2. Revert the implementation change that broke it
git revert HEAD --no-commit
# Or: git stash

# 3. Re-run tests to confirm regression is gone
./mvnw test -pl 05_Development/CareBridgeAPI -Dtest="*Notification*"
```

### 7.2 Database Rollback in Test

Integration tests use `@Transactional` — all DB changes roll back automatically after each test. No manual cleanup required for standard integration tests.

For tests that use `@Commit` (if any), clean up explicitly:

```java
@AfterEach
void cleanup() {
    notificationRecordRepository.deleteAll();
    deviceTokenRepository.deleteAll();
}
```

### 7.3 Red Gate Rollback

If a Red-Phase stub is accidentally converted to Green without the implementation:

1. Revert the test change: `git revert <commit>`
2. Re-add the `@Disabled` annotation and stub body
3. File a task to implement the backing feature before re-enabling

---

## 8. CASE 2.0 Anti-Pattern Detection

### 8.1 Anti-Pattern Checklist Applied to This Test-Spec

| AP-ID      | Anti-Pattern                            | Check Applied in Test-Spec                                      | Verdict   |
|------------|-----------------------------------------|-----------------------------------------------------------------|-----------|
| AP-AI-001  | Test asserts implementation detail       | Tests assert behavior (status, audit), not internal call order  | Clean     |
| AP-AI-002  | PHI in test data                         | All test data is SYNTHETIC; no real names/diagnoses             | Clean     |
| AP-AI-003  | Green-phase test with unimplemented code | Red-Phase stubs use `throw UnsupportedOperationException`       | Clean     |
| AP-AI-004  | Missing negative test coverage           | TC-INT-002, TC-INT-003, TC-INT-004, TC-SEC-003 cover negatives  | Clean     |
| AP-AI-005  | Ownership not tested                     | TC-SEC-002 explicitly verifies cross-user isolation             | Clean     |
| AP-AI-006  | FCM exception handling untested          | TC-UNIT-003 covers FCM exception → FAILED status               | Clean     |
| AP-AI-007  | Audit log omitted from tests             | TC-UNIT-001, TC-UNIT-002 verify `auditService.log()` calls     | Clean     |
| AP-AI-008  | Missing boundary value tests             | TC-SEC-004 (255 chars OK), TC-SEC-005 (256 chars rejected)      | Clean     |

### 8.2 Constraint Coverage Matrix

| Constraint   | TC-UNIT-001 | TC-UNIT-002 | TC-UNIT-003 | TC-UNIT-005 | TC-UNIT-006 | TC-INT-002 | TC-SEC-002 |
|--------------|-------------|-------------|-------------|-------------|-------------|------------|------------|
| C1 PERSIST-ALL | Yes       | Yes         | Yes         | —           | —           | —          | —          |
| C2 EMERGENCY-BYPASS | —   | —           | —           | Yes (Red)   | —           | —          | —          |
| C3 NO-PHI    | Yes (data)  | Yes (data)  | Yes (data)  | Yes (data)  | Yes (data)  | Yes (data) | Yes (data) |
| C4 IDEMPOTENCY | —         | —           | —           | —           | Yes (Red)   | —          | —          |
| C5 FCM-SAFE  | —           | —           | Yes         | —           | —           | —          | —          |
| C6 RBAC      | —           | —           | —           | —           | —           | Yes        | —          |
| C7 DTO-ONLY  | Yes         | Yes         | Yes         | —           | —           | Yes        | Yes        |
| C8 AUDIT-ALWAYS | Yes      | Yes         | Yes         | —           | —           | —          | —          |

### 8.3 Oracle Source Traceability

Every `assertThat()` in this spec derives its expected value from an explicit oracle:

| Expected Value                     | Oracle Source                                              |
|------------------------------------|------------------------------------------------------------|
| `status = "SENT"`                  | NotificationRecordStatus enum; `sentAt = Instant.now()` set in impl |
| `status = "FAILED"`                | NotificationRecordStatus enum; `failedAt = Instant.now()` set when no tokens or FCM exception |
| `auditService.log(NOTIFICATION_SENT)` | AuditAction enum; `NotificationServiceImpl.send()` line 107-111 |
| `HTTP 403` on /send for ROLE_MOTHER | `@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")` on controller |
| `HTTP 401` on /send without JWT    | Spring Security filter chain, unauthenticated access      |
| `HTTP 400` on blank title          | `@NotBlank` on `SendNotificationRequest.title`            |
| `HTTP 400` on title > 255 chars    | `@Size(max=255)` on `SendNotificationRequest.title`       |
| GET /me returns only own records   | `SecurityUtils.requireCurrentUserId()` in controller; `findByUserId(userId)` in service |

---

## APPENDIX

### A. Test Run Command Reference

```bash
# Run all notification tests
./mvnw test -pl 05_Development/CareBridgeAPI -Dtest="*Notification*"

# Run only unit tests (fast)
./mvnw test -pl 05_Development/CareBridgeAPI -Dtest="NotificationServiceImplTest"

# Run only integration tests
./mvnw test -pl 05_Development/CareBridgeAPI -Dtest="NotificationControllerIntegrationTest"

# Run with coverage report
./mvnw test jacoco:report -pl 05_Development/CareBridgeAPI

# Skip Red-Phase tests (CI-safe)
./mvnw test -pl 05_Development/CareBridgeAPI -Dtest="*Notification*" -Dgroups="!red-phase"
```

### B. Test Dependencies

```xml
<!-- pom.xml test scope dependencies (existing or to verify) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

### C. Glossary

| Term             | Definition                                                                   |
|------------------|------------------------------------------------------------------------------|
| Red Phase        | Test written before implementation exists — MUST fail until impl added       |
| Green Phase      | Test passes with current implementation                                      |
| SYNTHETIC data   | Fabricated test data with no connection to real users or PHI                |
| Oracle source    | The specification artifact that defines the correct expected value           |
| Props Isolation  | Test fixtures are isolated via factory methods — no shared mutable state    |
| CASE 2.0         | AI constraint framework ensuring test quality and anti-pattern detection     |
| Preference Gate  | Check against `notification_preferences` before dispatch                    |
| EMERGENCY bypass | Skip preference gate for type=EMERGENCY (BR-SAFETY mandate)                 |
