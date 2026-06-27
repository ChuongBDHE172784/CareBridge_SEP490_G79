# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-45 Create Appointment Reminder

**Document ID:** `CB-REM-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC45_CreateAppointmentReminder/UC45_CreateAppointmentReminder_TDS.md` (CB-REM-IMP-001)
- SRS: §3.3.1.22

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-45 |

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
| **Feature / Gap ID** | `UC-45` |
| **Module** | `CreateAppointmentReminder — reminder` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `PII` |
| **Upstream Dependencies** | `auth, Firebase FCM` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "creates a reminder" — không rõ past time handling | BR-REM-002: scheduledAt ≥ now + 5 min | Test encode past time → 400 |
| L2 | SRS: không rõ FCM failure handling | ADR-REM-001: FCM failure → warning, reminder still saved | Test encode FCM failure path |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid reminder creation | `ReminderService.createReminder()` | `REM-TC-001` |
| TC-COND-002 | scheduledAt too soon | `validateScheduledAt()` | `REM-TC-002` |
| TC-COND-003 | Recurrence without end date | DTO validation | `REM-TC-003` |
| TC-COND-004 | EXPERT role → 403 | `@PreAuthorize` | `REM-TC-004` |
| TC-COND-005 | FCM token missing → warning | `NotificationService` | `REM-TC-005` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path |
| `FX-002` | Input | `{reminderType: "APPOINTMENT", title: "OB-GYN", scheduledAt: now+2h, recurrenceType: "NONE"}` | Happy path |
| `FX-003` | Input | `{scheduledAt: yesterday}` | Past time |
| `FX-004` | Input | `{recurrenceType: "DAILY"}` (no end date) | Missing end date |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class ReminderTestFactory {
    static Reminder makeReminder() {
        Reminder r = new Reminder();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000030"));
        r.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setReminderType(ReminderType.APPOINTMENT);
        r.setTitle("OB-GYN Checkup");
        r.setScheduledAt(ZonedDateTime.now().plusHours(2));
        r.setRecurrenceType(RecurrenceType.NONE);
        r.setStatus(ReminderStatus.PENDING);
        return r;
    }
}
```

---

### REM-TC-001 — Happy path

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.createReminder()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `UC-45 Normal Flow`

**Test Steps:**
1. Mock `notificationService.scheduleFcmPush()` → "fcm-job-123"
2. Mock `reminderRepository.save()` → saved reminder
3. Call `createReminder(FX-002, ACC-001)`

**Expected Result (PASS):**
- Returns response with `status = "PENDING"`, `reminderType = "APPOINTMENT"`
- `notificationService.scheduleFcmPush()` called with correct scheduledAt
- `auditService.emit(ReminderCreated)` called

**Current Status:** 🔴 Not written

---

### REM-TC-002 — scheduledAt in past → 400

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.validateScheduledAt()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-REM-002`

**Test Steps:**
1. Call `createReminder()` with `scheduledAt = now - 1 hour`

**Expected Result (PASS):**
- Throws `InvalidScheduledAtException` with code `REM-002`
- `reminderRepository.save()` NOT called
- FCM NOT called

**Current Status:** 🔴 Not written

---

### REM-TC-003 — DAILY recurrence without end date → 400

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-REM-003`

**Test Steps:** POST with FX-004 (DAILY, no recurrenceEndDate)

**Expected Result:** 400, error REM-003

**Current Status:** 🔴 Not written

---

### REM-TC-004 — EXPERT role → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**TDD Phase:** 🔴 RED

**Expected Result:** 403

**Current Status:** 🔴 Not written

---

### REM-TC-005 — FCM failure → reminder saved with warning

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationService` error handling in ReminderService
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-REM-001`

**Test Steps:**
1. Mock `notificationService.scheduleFcmPush()` → throws exception
2. Call `createReminder(FX-002)`

**Expected Result (PASS):**
- Reminder is saved in DB despite FCM failure
- Response 201 with `fcmJobId = null` or warning field
- No exception propagated to client

**Current Status:** 🔴 Not written

---

### REM-TC-INT-001 — Reminder persisted in DB

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. POST with FX-001 JWT + FX-002 body
2. Assert 201
3. `SELECT status FROM reminders WHERE account_id='ACC-001'` → 'PENDING'

```java
Reminder saved = reminderRepo.findByAccountId(ACC_001).get(0);
assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
assertThat(saved.getReminderType()).isEqualTo(ReminderType.APPOINTMENT);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `REM-TC-001` | `[ ]` | `___` | — |
| `REM-TC-002` | `[ ]` | `___` | — |
| `REM-TC-003` | `[ ]` | `___` | — |
| `REM-TC-004` | `[ ]` | `___` | — |
| `REM-TC-005` | `[ ]` | `___` | — |
| `REM-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] FCM scheduling tested (happy + failure path)
- [ ] scheduledAt validation (past + too soon)
- [ ] Recurrence end date required when recurrenceType != NONE
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/main/resources/db/migration/V23__create_reminders.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-001 | ☐ | G-0 |
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-005 | ☐ INotificationService exists | G-3 |
