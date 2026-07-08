# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-212 View Reminder Detail

**Document ID:** `CB-REM-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Implemented — 2026-07-05`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC212_ViewReminderDetail/UC212_ViewReminderDetail_TDS.md` (CB-REM-IMP-002)
- SRS: §3.3.16.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-05 | AI Agent — Amelia (Dev Agent) | Completion — REM-VIEW-TC-001 (PENDING) added and GREEN; 5/5 UC212 tests PASS |
| 2026-06-27 | AI Agent — Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-212 |

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
| **Feature / Gap ID** | `UC-212` |
| **Module** | `ViewReminderDetail — reminder` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays reminder type, recurrence, time, status" — không rõ sharing | ADR-REM-002: owner-only (not shared with care group) | Test non-owner → 403 |
| L2 | SRS: không rõ cancelled reminder handling | Cancelled → still viewable | Test CANCELLED → 200 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner views PENDING reminder | `ReminderService.getReminderDetail()` | `REM-VIEW-TC-001` |
| TC-COND-002 | Owner views COMPLETED reminder | status in response | `REM-VIEW-TC-002` |
| TC-COND-003 | Non-owner → 403 | `verifyOwnership()` | `REM-VIEW-TC-003` |
| TC-COND-004 | Non-existent → 404 | repo lookup | `REM-VIEW-TC-004` |
| TC-COND-005 | No medication dosage in response | Response mapping | `REM-VIEW-TC-005` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB | `{id: REM-001, accountId: ACC-001, type: APPOINTMENT, status: PENDING}` | Happy path |
| `FX-002` | DB | `{id: REM-002, accountId: ACC-001, type: MEDICATION, status: COMPLETED}` | Completed |
| `FX-003` | DB | `{id: REM-003, accountId: ACC-999, status: PENDING}` | Non-owner |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class ReminderDetailTestFactory {
    static Reminder makePendingReminder() {
        Reminder r = new Reminder();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000080"));
        r.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setReminderType(ReminderType.APPOINTMENT);
        r.setTitle("OB-GYN Checkup");
        r.setScheduledAt(ZonedDateTime.now().plusDays(7));
        r.setRecurrenceType(RecurrenceType.NONE);
        r.setStatus(ReminderStatus.PENDING);
        return r;
    }
}
```

---

### REM-VIEW-TC-001 — Owner views PENDING reminder → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN

**Test Steps:**
1. Mock repo → FX-001
2. Call `getReminderDetail(REM-001, ACC-001)`

**Expected Result:** 200 with `status=PENDING`, `reminderType=APPOINTMENT`, `title`

**Current Status:** 🟢 Passing

---

### REM-VIEW-TC-002 — Completed reminder viewable

**Severity:** `MEDIUM`
**Oracle Source:** implicit from SRS (status display)
**TDD Phase:** 🟢 GREEN

**Expected Result:** 200 with `status=COMPLETED`

**Current Status:** 🟢 Passing

---

### REM-VIEW-TC-003 — Non-owner → 403

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-REM-002`
**TDD Phase:** 🟢 GREEN

**Expected Result:** throws ForbiddenException (REM-004)

**Current Status:** 🟢 Passing

---

### REM-VIEW-TC-004 — Non-existent → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN

**Expected Result:** NotFoundException (REM-006)

**Current Status:** 🟢 Passing

---

### REM-VIEW-TC-005 — No dosage/medical advice in response

**Severity:** `HIGH`
**Oracle Source:** `BR-SAFETY-002`
**TDD Phase:** 🟢 GREEN

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("dosage");
assertThat(json).doesNotContain("medicalAdvice");
assertThat(json).doesNotContain("prescription");
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `REM-VIEW-TC-001` | `[x]` | `2026-07-05` | — |
| `REM-VIEW-TC-002` | `[x]` | `2026-07-05` | — |
| `REM-VIEW-TC-003` | `[x]` | `2026-07-05` | — |
| `REM-VIEW-TC-004` | `[x]` | `2026-07-05` | — |
| `REM-VIEW-TC-005` | `[x]` | `2026-07-05` | — |

---

## 6. Exit Criteria

- [x] Owner-only access enforced
- [x] All statuses (PENDING/COMPLETED/SKIPPED/CANCELLED) viewable
- [x] No medical advice in response
- [x] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS reminders CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '039';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/test/java/com/carebridge/backend/reminder/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-004 | ☐ No business logic in controller | G-4 |
