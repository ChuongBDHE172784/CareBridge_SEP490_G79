# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-49 View Today Tasks

**Document ID:** `CB-REMINDER-IMP-004-TEST`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC49_ViewTodayTasks/UC49_ViewTodayTasks_TDS.md` (CB-REMINDER-IMP-004)
- `01_Requirements/SRS.md` §3.3.1.26

> **TDD Convention:** Viết test TRƯỚC khi implement. Thứ tự: RED → GREEN → refactor.
> Không dùng PII thật — chỉ dùng SYNTHETIC data. Fixed clock trong tests để kiểm soát "today".

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD Spec cho UC-49 View Today Tasks |

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

| Field | Value |
|-------|-------|
| **Feature / UC ID** | `UC-49` |
| **Module** | `reminder / care-task — ViewTodayTasks` |
| **Spec gốc** | `CB-REMINDER-IMP-004` |
| **Priority** | 🟠 P1 — High (Frequent access) |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-TODAY-001, BR-TODAY-SORT` |
| **Upstream Dependencies** | `auth, reminders, care_tasks, care_groups` |
| **Downstream Consumers** | `Mobile App Today screen` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REMINDER-IMP-004 §17`, `BR-TODAY-001`, `ADR-TODAY-001`, `ADR-TODAY-002` |
| **Constraints Injected** | `C1 (timezone), C2 (PENDING/SNOOZED only), C3 (care_tasks OPEN+owned), C4 (sort priority), C5 (JWT userId)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec chưa rõ timezone "today" tính như thế nào | ADR-TODAY-002: dùng X-User-Timezone header, fallback Asia/Ho_Chi_Minh | Test phải dùng fixed clock và verify boundary 00:00:00 / 23:59:59 theo timezone user |
| L2 | Spec chưa rõ checklist_items có trong today tasks không | CLAUDE.md + schema: checklist_items là admin-managed templates; UC-49 không query trực tiếp bảng này | Test phải verify response KHÔNG chứa checklist_items riêng lẻ |
| L3 | Spec chưa rõ sort order khi cùng giờ | BR-TODAY-SORT: priority VACCINATION > MEDICATION > APPOINTMENT > CARE_TASK | Test phải có scenario cùng dueAt, verify priority sort |
| L4 | Spec không đề cập SNOOZED reminders | BR-TODAY-001: include PENDING và SNOOZED (cả hai visible) | Test phải verify SNOOZED reminder xuất hiện trong today list |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UC-49 ViewTodayTasks bao gồm:
├── Service (TodayTaskService.getTodayTasks — aggregation + sort)
│   ├── computeDateRange(timezone) — timezone boundary computation
│   ├── mergeAndSort(reminders, careTasks) — priority sort
│   └── toPriority(reminderType) — priority mapping
├── Controller (GET /api/v1/reminders/today — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-49` | Mother xem today tasks — aggregate reminders + care_tasks |
| `BR-TODAY-001` | Reminders (scheduled_at today, PENDING/SNOOZED) + care_tasks (due_at today, OPEN) |
| `BR-TODAY-SORT` | Sort: VACCINATION=1 > MEDICATION=2 > APPOINTMENT=3 > CARE_TASK=4; secondary: dueAt ASC |
| `BR-RBAC` | owner_user_id filter; assigned_to filter |
| `ADR-TODAY-001` | 2 queries riêng biệt, merge trong Service |
| `ADR-TODAY-002` | Timezone computation từ X-User-Timezone header |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — reminders + care_tasks hôm nay | `getTodayTasks()` | `TODAY-TC-001` |
| TC-COND-002 | Không có tasks hôm nay → empty | `mergeAndSort()` empty | `TODAY-TC-002` |
| TC-COND-003 | Reminder ngày mai không xuất hiện | `computeDateRange()` upper bound | `TODAY-TC-003` |
| TC-COND-004 | Reminder COMPLETED không xuất hiện | status filter PENDING/SNOOZED | `TODAY-TC-004` |
| TC-COND-005 | Sort priority — VACCINATION trước MEDICATION trước APPOINTMENT | `toPriority()` + `mergeAndSort()` | `TODAY-TC-005` |
| TC-COND-006 | SNOOZED reminder xuất hiện trong today | status filter | `TODAY-TC-006` |
| TC-COND-007 | Care task của user khác không xuất hiện | `assigned_to` filter | `TODAY-TC-007` |
| TC-COND-008 | Timezone fallback khi không có header | `computeDateRange()` default | `TODAY-TC-008` |
| TC-COND-009 | Timezone boundary — 23:59:59 hôm nay (last minute task) | `computeDateRange()` lower/upper | `TODAY-TC-009` |
| TC-COND-010 | Unauthorized → 401 | Spring Security | `TODAY-TC-010` |
| TC-COND-011 | Wrong role → 403 | `@PreAuthorize` | `TODAY-TC-011` |
| TC-COND-012 | Invalid timezone → 400 | header validation | `TODAY-TC-012` |
| TC-COND-013 | Full integration flow | Controller → Service → DB | `TODAY-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| Equivalence Partitioning | scheduledAt: today / yesterday / tomorrow | Date boundary |
| Boundary Value Analysis | scheduledAt = today 00:00:00, today 23:59:59 | Range boundary |
| State Transition Testing | status: PENDING, SNOOZED (include); COMPLETED, SKIPPED (exclude) | Status filter |
| Equivalence Partitioning | reminderType: VACCINATION, MEDICATION, APPOINTMENT, CARE_TASK | Priority sort |
| Error Guessing | Invalid timezone, missing JWT | Header/auth edge cases |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-TODAY-001` | DB seed | `User { userId: "user-001", role: ROLE_MOTHER }` | Owner |
| `FX-TODAY-002` | DB seed | `Reminder { reminderId: "rem-vac", type: VACCINATION, scheduledAt: TODAY@08:00+07, status: PENDING, owner: user-001 }` | Today reminder — VACCINATION |
| `FX-TODAY-003` | DB seed | `Reminder { reminderId: "rem-med", type: MEDICATION, scheduledAt: TODAY@09:00+07, status: SNOOZED, owner: user-001 }` | Today reminder — MEDICATION SNOOZED |
| `FX-TODAY-004` | DB seed | `Reminder { reminderId: "rem-apt", type: APPOINTMENT, scheduledAt: TODAY@10:00+07, status: PENDING, owner: user-001 }` | Today reminder — APPOINTMENT |
| `FX-TODAY-005` | DB seed | `Reminder { reminderId: "rem-tomorrow", type: VACCINATION, scheduledAt: TOMORROW@08:00+07, status: PENDING, owner: user-001 }` | Tomorrow — không include |
| `FX-TODAY-006` | DB seed | `Reminder { reminderId: "rem-done", type: MEDICATION, scheduledAt: TODAY@07:00+07, status: COMPLETED, owner: user-001 }` | COMPLETED — không include |
| `FX-TODAY-007` | DB seed | `CareTask { careTaskId: "task-001", assignedTo: user-001, dueAt: TODAY@15:00+07, status: OPEN }` | Today care task |
| `FX-TODAY-008` | DB seed | `CareTask { careTaskId: "task-other", assignedTo: user-999, dueAt: TODAY@15:00+07, status: OPEN }` | Other user's task |
| `FX-TODAY-009` | Clock | Fixed clock: `2026-06-26T00:00:00Z` | Deterministic "today" |
| `FX-TODAY-010` | JWT | `{ sub: "user-001", role: "ROLE_MOTHER" }` | Auth |
| `FX-TODAY-011` | JWT | `{ sub: "user-002", role: "ROLE_EXPERT" }` | Wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// TodayTaskTestFactory.java
class TodayTaskTestFactory {

    // Fixed "today" = 2026-06-26 in Asia/Ho_Chi_Minh
    static final ZoneId USER_TZ = ZoneId.of("Asia/Ho_Chi_Minh");
    static final LocalDate TODAY = LocalDate.of(2026, 6, 26);
    static final ZonedDateTime TODAY_START = TODAY.atStartOfDay(USER_TZ);
    static final ZonedDateTime TODAY_END = TODAY.atTime(23, 59, 59).atZone(USER_TZ);

    static Reminder makeVaccinationReminder() {
        Reminder r = new Reminder();
        r.setReminderId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        r.setReminderType("VACCINATION");
        r.setTitle("Tiêm vắc-xin 5 trong 1");
        r.setScheduledAt(TODAY_START.plusHours(8));   // 08:00 today
        r.setStatus("PENDING");
        return r;
    }

    static Reminder makeMedicationReminder() {
        Reminder r = new Reminder();
        r.setReminderId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        r.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        r.setReminderType("MEDICATION");
        r.setTitle("Uống sắt buổi sáng");
        r.setScheduledAt(TODAY_START.plusHours(9));   // 09:00 today
        r.setStatus("SNOOZED");
        return r;
    }

    static CareTask makeCareTask() {
        CareTask t = new CareTask();
        t.setCareTaskId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        t.setAssignedTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        t.setTitle("Cân nặng em bé tuần 12");
        t.setDueAt(TODAY_START.plusHours(15));  // 15:00 today
        t.setStatus("OPEN");
        return t;
    }
}
```

---

### TODAY-TC-001 — Happy path: reminders + care_tasks hôm nay

**Severity:** `CRITICAL`
**Feature Under Test:** `TodayTaskService.getTodayTasks()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-TODAY-001`, `CB-REMINDER-IMP-004 §9.2`

**Preconditions:**
- Fixed clock = 2026-06-26T00:00:00+07:00
- FX-TODAY-002: rem-vac (VACCINATION, 08:00, PENDING)
- FX-TODAY-003: rem-med (MEDICATION, 09:00, SNOOZED)
- FX-TODAY-007: task-001 (CARE_TASK, 15:00, OPEN)

**Test Steps:**
1. Arrange: Mock `reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(userId, todayStart, todayEnd, [PENDING, SNOOZED])` → return [rem-vac, rem-med]; Mock `careTaskRepository.findByAssignedToAndDueAtBetweenAndStatus(userId, todayStart, todayEnd, OPEN)` → return [task-001]
2. Act: `todayTaskService.getTodayTasks(UUID("user-001"), "Asia/Ho_Chi_Minh")`
3. Assert: `response.totalCount == 3`; `response.reminders.size() == 2`; `response.careTasks.size() == 1`; `response.date == LocalDate.of(2026, 6, 26)`; `response.timezone == "Asia/Ho_Chi_Minh"`

**Expected Result (PASS):**
- totalCount = 3
- reminders[0].reminderType = "VACCINATION" (priority 1)
- reminders[1].reminderType = "MEDICATION" (priority 2)
- careTasks[0].taskType = "CARE_TASK"

**Expected Result (FAIL):**
- totalCount != 3, hoặc sort order sai, hoặc response null

**Current Status:** 🔴 Not written

---

### TODAY-TC-002 — Không có tasks hôm nay → empty response

**Severity:** `HIGH`
**Feature Under Test:** `TodayTaskService.getTodayTasks()` — empty state
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-TODAY-001`, `CB-REMINDER-IMP-004 §9.2`

**Test Steps:**
1. Arrange: Both repositories return empty lists
2. Act: `todayTaskService.getTodayTasks(userId, timezone)`
3. Assert: `response.totalCount == 0`; `response.reminders == []`; `response.careTasks == []`; response không null

**Expected Result (PASS):**
- HTTP 200; totalCount = 0; không throw exception

**Current Status:** 🔴 Not written

---

### TODAY-TC-003 — Reminder ngày mai không xuất hiện

**Severity:** `HIGH`
**Feature Under Test:** `computeDateRange()` upper bound
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-TODAY-001`

**Test Steps:**
1. Arrange: FX-TODAY-005: rem-tomorrow (scheduledAt = tomorrow 08:00, PENDING)
2. Arrange: `reminderRepository` return [rem-tomorrow] khi query có todayEnd ngày mai
3. Act: `todayTaskService.getTodayTasks(userId, "Asia/Ho_Chi_Minh")`
4. Assert: response.reminders = [] (rem-tomorrow không nằm trong today range)

**Implementation Note:** Test này verify `computeDateRange()` tính todayEnd đúng là `today 23:59:59` (không phải tomorrow 00:00:00).

**Current Status:** 🔴 Not written

---

### TODAY-TC-004 — Reminder COMPLETED không xuất hiện

**Severity:** `HIGH`
**Feature Under Test:** `status IN ('PENDING', 'SNOOZED')` filter
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-TODAY-001`

**Test Steps:**
1. Arrange: FX-TODAY-006: rem-done (COMPLETED, today 07:00)
2. Arrange: Mock reminder repo với status filter [PENDING, SNOOZED] → return [] (không có COMPLETED)
3. Assert: response.reminders = []
4. Verify: `reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn()` được gọi với statuses = ["PENDING", "SNOOZED"]

**Expected Result (PASS):** Reminder COMPLETED không xuất hiện

**Current Status:** 🔴 Not written

---

### TODAY-TC-005 — Sort priority: VACCINATION > MEDICATION > APPOINTMENT

**Severity:** `CRITICAL`
**Feature Under Test:** `TodayTaskService.mergeAndSort()` + `toPriority()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-TODAY-SORT`

**Preconditions:**
- 3 reminders cùng scheduledAt = TODAY@08:00: APPOINTMENT, VACCINATION, MEDICATION (thứ tự random từ DB)

**Test Steps:**
1. Arrange: Mock repo → return [APPOINTMENT rem, VACCINATION rem, MEDICATION rem] (thứ tự này)
2. Act: `getTodayTasks(userId, timezone)`
3. Assert: `response.reminders[0].reminderType == "VACCINATION"`; `response.reminders[1].reminderType == "MEDICATION"`; `response.reminders[2].reminderType == "APPOINTMENT"`

**Expected Result (PASS):** Thứ tự: VACCINATION (1) → MEDICATION (2) → APPOINTMENT (3)

**Current Status:** 🔴 Not written

---

### TODAY-TC-006 — SNOOZED reminder xuất hiện trong today list

**Severity:** `HIGH`
**Feature Under Test:** status filter includes SNOOZED
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-TODAY-001`

**Test Steps:**
1. Arrange: FX-TODAY-003: rem-med (SNOOZED, today 09:00)
2. Arrange: Mock repo với statuses [PENDING, SNOOZED] → return [rem-med]
3. Assert: response.reminders[0].status = "SNOOZED"; response.totalCount = 1

**Expected Result (PASS):** SNOOZED reminder hiển thị trong today list

**Current Status:** 🔴 Not written

---

### TODAY-TC-007 — Care task của user khác không xuất hiện

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `assigned_to = userId` filter trong ICareTaskRepository
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: Mock `careTaskRepository.findByAssignedToAndDueAtBetweenAndStatus("user-001", ...)` → return [] (task-other không được trả về vì assigned_to = user-999)
2. Assert: response.careTasks = []

**Expected Result (PASS = hệ thống an toàn):** Care task của user-999 không xuất hiện

**Expected Result (FAIL = lỗ hổng):** Care task của user-999 xuất hiện → RBAC violation

**Current Status:** 🔴 Not written

---

### TODAY-TC-008 — Timezone fallback khi không có X-User-Timezone header

**Severity:** `MEDIUM`
**Feature Under Test:** `computeDateRange()` default timezone
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/TodayTaskControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-TODAY-002`

**Test Steps:**
1. Act: `mockMvc.perform(GET /api/v1/reminders/today)` — không có X-User-Timezone header
2. Assert: Response 200; `response.timezone == "Asia/Ho_Chi_Minh"`

**Expected Result (PASS):** Fallback đến Asia/Ho_Chi_Minh, không throw exception

**Current Status:** 🔴 Not written

---

### TODAY-TC-009 — Timezone boundary: reminder tại 23:59:59 hôm nay — xuất hiện

**Severity:** `HIGH`
**Feature Under Test:** `computeDateRange()` — upper boundary inclusive
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/TodayTaskServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-TODAY-001`

**Test Steps:**
1. Arrange: Reminder "rem-late" scheduledAt = TODAY@23:59:59+07:00, PENDING
2. Arrange: Mock repo → return [rem-late] khi query với todayStart..todayEnd
3. Assert: response.reminders[0].taskId = "rem-late"

**Expected Result (PASS):** Reminder tại 23:59:59 hôm nay được include

**Current Status:** 🔴 Not written

---

### TODAY-TC-010 — Unauthorized (không có JWT) → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/TodayTaskControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. Act: GET /api/v1/reminders/today — không có Authorization header
2. Assert: HTTP 401

**Current Status:** 🔴 Not written

---

### TODAY-TC-011 — Wrong role (ROLE_EXPERT) → 403

**Severity:** `HIGH`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/TodayTaskControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-RBAC`, `REMINDER-004`

**Test Steps:**
1. Arrange: FX-TODAY-011 — JWT với ROLE_EXPERT
2. Act: GET /api/v1/reminders/today
3. Assert: HTTP 403, error code REMINDER-004

**Current Status:** 🔴 Not written

---

### TODAY-TC-012 — Invalid timezone header → 400

**Severity:** `MEDIUM`
**Feature Under Test:** X-User-Timezone header validation
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/TodayTaskControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `REMINDER-008`

**Test Steps:**
1. Act: GET /api/v1/reminders/today với header `X-User-Timezone: INVALID/ZONE`
2. Assert: HTTP 400; error code REMINDER-008

**Current Status:** 🔴 Not written

---

### TODAY-TC-INT-001 — Full integration: Aggregate từ DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: GET /api/v1/reminders/today → DB`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ViewTodayTasksIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer + Flyway migration (V1__init_schema.sql)
- Seed: FX-TODAY-001, FX-TODAY-002 (rem-vac, PENDING, today), FX-TODAY-007 (task-001, OPEN, today)
- Fixed clock inject vào Spring context (dùng `@TestConfiguration` với `Clock` bean)

**Test Steps:**
1. Seed data vào DB
2. Authenticate → JWT
3. `GET /api/v1/reminders/today` với header `X-User-Timezone: Asia/Ho_Chi_Minh`
4. Assert response 200
5. Verify response JSON

**Expected Result (PASS):**
- HTTP 200
- `totalCount = 2` (1 reminder + 1 care_task)
- `reminders[0].reminderType = "VACCINATION"`
- `careTasks[0].taskType = "CARE_TASK"`

**DB Assertion:**
```java
// Verify direct DB state to confirm data correctness
long reminderCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM reminders WHERE owner_user_id = ? AND status IN ('PENDING','SNOOZED') " +
    "AND scheduled_at BETWEEN ? AND ?",
    Long.class, userId, todayStartUtc, todayEndUtc
);
assertThat(reminderCount).isEqualTo(1L);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `TODAY-TC-001` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-002` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-003` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-004` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-005` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-006` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-007` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-008` | `TodayTaskControllerTest.java` | `[ ]` | — | — |
| `TODAY-TC-009` | `TodayTaskServiceTest.java` | `[ ]` | — | — |
| `TODAY-TC-010` | `TodayTaskControllerTest.java` | `[ ]` | — | — |
| `TODAY-TC-011` | `TodayTaskControllerTest.java` | `[ ]` | — | — |
| `TODAY-TC-012` | `TodayTaskControllerTest.java` | `[ ]` | — | — |
| `TODAY-TC-INT-001` | `ViewTodayTasksIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**
```java
@Service
public class TodayTaskService implements ITodayTaskService {

    @Override
    public TodayTaskResponse getTodayTasks(UUID userId, String timezone) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|--------|----------------------------------|
| `TODAY-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `TODAY-TC-002` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `TODAY-TC-005` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `TODAY-TC-007` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `TODAY-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → GATE-2 PASS → tiếp tục implement

> **Lưu ý:** TODAY-TC-010 (401) và TODAY-TC-011 (403) có thể PASS ngay nếu Spring Security đã configured — đây là framework behavior, không phải AP-AI-002. Tách ra khỏi Red Gate scope.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-REMINDER-IMP-004` đã review và approve
- [ ] Logic Issues (Section 2) đã confirm — đặc biệt L1 (timezone) và L2 (no checklist_items)
- [ ] UC-47 và UC-48 đã implement (reminders phải tồn tại trong DB)
- [ ] `care_tasks` table index `(assigned_to, due_at)` đã được DBA confirm
- [ ] Test fixtures (Section 3 TDS-05) đã chuẩn bị với fixed clock configuration

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=TodayTaskServiceTest` — tất cả xanh
- [ ] `./mvnw verify -Dtest=ViewTodayTasksIntegrationTest` — xanh
- [ ] Test coverage ≥ 80% lines cho `TodayTaskService`
- [ ] Sort order được verify qua TC-005 (priority sort với cùng dueAt)
- [ ] Timezone boundary được verify qua TC-009 (23:59:59 inclusive)
- [ ] RBAC được verify qua TC-007 và TC-011
- [ ] Response time < 400ms trong load test với 50 concurrent requests

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả business logic tests FAIL với stub
- [ ] Clock injection: tests dùng `@TestConfiguration Clock` bean thay vì `LocalDate.now()`
- [ ] Props Isolation: TodayTaskTestFactory dùng static factory, không shared mutable state

### Suspension Criteria

- UC-47/48 chưa stable
- care_tasks index chưa được tạo trên staging

---

## 7. Rollback Plan

```bash
# Không có migration mới (nếu không cần index mới)
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/test/java/com/carebridge/backend/reminder/

# Nếu đã tạo index mới trên care_tasks:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_care_task_assigned_to_due_at;"
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong spec | Check | Gate |
|-------|-------------|---------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | getTodayTasks() không filter theo BR-TODAY-001 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC-001 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code dùng UTC làm "today" thay vì user timezone | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller gọi reminderRepository trực tiếp thay vì TodayTaskService | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Query `checklist_items` table trực tiếp trong UC-49 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC-49 View Today Tasks*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
