# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-46 Create Medication Reminder

**Document ID:** `CB-REMINDER-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Spec`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `04_Implement/UC46_CreateMedicationReminder/UC46_CreateMedicationReminder_TDS.md` — TDS CB-REMINDER-IMP-001
- `02_Requirements/SRS.md §3.3.1.23` — UC-46 functional requirements

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-46 Create Medication Reminder |

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

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-46` |
| **Module** | `CreateMedicationReminder — reminder Bounded Context` |
| **Spec gốc** | `CB-REMINDER-IMP-001` |
| **Priority** | 🟠 P1 — High |
| **Sprint** | `S[N] (2026-06-26 → TBD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-REMINDER-001, BR-REMINDER-002, BR-REMINDER-003, BR-SAFETY` |
| **Upstream Dependencies** | `auth (JWT), Firebase FCM, journey, baby` |
| **Downstream Consumers** | `UC-158 ReceiveReminderNotification, UC-212 ViewReminderDetail, AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REMINDER-IMP-001 §17`, `BR-REMINDER-001`, `BR-REMINDER-002`, `BR-REMINDER-003`, `BR-SAFETY`, `BR-RBAC` |
| **Constraints Injected** | C1 (hardcode MEDICATION type), C2 (DTO validation), C3 (RRULE validation), C4 (FCM non-blocking), C5 (userId from JWT), C6 (no logic in controller), C7 (no drug suggestion) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS không chỉ định rõ ai set reminder_type | BR-REMINDER-001: `reminder_type='MEDICATION'` — phải được hardcode tại service layer | Test verify reminderType='MEDICATION' trong response và DB, KHÔNG nhận từ request |
| L2 | SRS không chỉ định format của recurrence_rule | `V1__init_schema.sql`: `recurrence_rule varchar(100)` — BR-REMINDER-002: RRULE string | Test validate cả RRULE hợp lệ (FREQ=DAILY;INTERVAL=1) và invalid (WEEKLY_BAD) |
| L3 | SRS không chỉ định xử lý khi FCM thất bại | ADR-REMINDER-002: FCM failure non-blocking — reminder vẫn được lưu | Test verify reminder được tạo thành công dù FCM mock throw exception |
| L4 | SRS dùng "medicine or vitamin reminder" — không phân biệt | BR-REMINDER-001: chỉ 1 reminder_type = 'MEDICATION' cho cả thuốc lẫn vitamin | Test chỉ expect MEDICATION type, không tạo VITAMIN type riêng |
| L5 | SRS không đề cập BR-SAFETY trong context reminder | CLAUDE.md/BR-SAFETY: hệ thống không kê đơn | Test verify title từ user được lưu nguyên vẹn, hệ thống không modify/append text |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
CreateMedicationReminder bao gồm các layer:
├── Service (createMedicationReminder, RRULE validation — unit test với Mockito)
├── Controller (DTO validation + auth — @WebMvcTest)
├── Repository (IReminderRepository.save())
├── FCM (IFcmService — mock trong unit/integration test, không call Firebase thật)
└── Integration (Testcontainers PostgreSQL + full Spring context + mock FCM)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-46 §3.3.1.23` | Mother nhập title, scheduledAt, recurrenceRule → reminder được tạo + FCM scheduled |
| `BR-REMINDER-001` | title NotBlank, scheduledAt NotNull, reminderType='MEDICATION' hardcoded |
| `BR-REMINDER-002` | recurrenceRule optional — nếu có phải là RRULE format |
| `BR-REMINDER-003` | FCM push notification được trigger sau khi reminder tạo thành công |
| `BR-SAFETY` | Hệ thống không modify title để thêm gợi ý thuốc |
| `BR-RBAC` | Chỉ ROLE_MOTHER; owner_user_id từ JWT |
| `CB-REMINDER-IMP-001 §10` | Error codes: REMINDER-001, REMINDER-002, REMINDER-003, REMINDER-004, REMINDER-005 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | title + scheduledAt hợp lệ, recurrenceRule = null → tạo thành công | `ReminderService.createMedicationReminder()` | `REMINDER-TC-001` |
| TC-COND-002 | title + scheduledAt + RRULE hợp lệ → tạo thành công với recurrence | `ReminderService.createMedicationReminder()` | `REMINDER-TC-002` |
| TC-COND-003 | title blank → 400 REMINDER-001 | DTO validation | `REMINDER-TC-003` |
| TC-COND-004 | scheduledAt null → 400 REMINDER-001 | DTO validation | `REMINDER-TC-004` |
| TC-COND-005 | scheduledAt ở quá khứ → 400 REMINDER-002 | `@FutureOrPresent` | `REMINDER-TC-005` |
| TC-COND-006 | recurrenceRule không hợp lệ → 400 REMINDER-003 | RRULE validator | `REMINDER-TC-006` |
| TC-COND-007 | FCM throw exception → reminder vẫn được lưu (non-blocking) | `ReminderService` FCM error handling | `REMINDER-TC-007` |
| TC-COND-008 | reminderType='MEDICATION' được hardcode, không nhận từ request | `ReminderService` | `REMINDER-TC-008` |
| TC-COND-009 | GET reminder không thuộc owner → 403/404 | RBAC | `REMINDER-TC-009` |
| TC-COND-010 | Không có JWT → 401 | Spring Security | `REMINDER-TC-010` |
| TC-COND-011 | E2E tạo reminder với DB thật | Full stack | `REMINDER-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | recurrenceRule: null, valid RRULE, invalid string | BR-REMINDER-002 |
| Boundary Value Analysis | scheduledAt: now() (edge của @FutureOrPresent), max title length 255 chars | BR-REMINDER-001 |
| State Transition Testing | Reminder status: PENDING → COMPLETED / SKIPPED / SNOOZED | State machine §6.3 TDS |
| Error Guessing | FCM failure, RBAC cross-owner, missing fields, injection in title | Security + robustness |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Request | `{ title: "SYNTHETIC Vitamin D", scheduledAt: tomorrow 08:00, recurrenceRule: null }` | Happy path no-recurrence |
| `FX-002` | Request | `{ title: "SYNTHETIC Iron 60mg", scheduledAt: tomorrow 07:00, recurrenceRule: "FREQ=DAILY;INTERVAL=1" }` | Happy path with RRULE |
| `FX-003` | Request | `{ title: "", scheduledAt: tomorrow 08:00 }` | BR-REMINDER-001 title validation |
| `FX-004` | Request | `{ title: "SYNTHETIC med", scheduledAt: "2020-01-01T08:00:00Z" }` | scheduledAt past |
| `FX-005` | Request | `{ title: "SYNTHETIC med", scheduledAt: tomorrow, recurrenceRule: "WEEKLY_INVALID" }` | Invalid RRULE |
| `FX-006` | JWT | `{ sub: 'synthetic-mother-uuid-001', role: 'ROLE_MOTHER' }` | Mother auth |
| `FX-007` | DB seed | SYNTHETIC reminder owned by different mother UUID | RBAC cross-owner |
| `FX-008` | Mock | `IFcmService.scheduleNotification()` throws FirebaseMessagingException | FCM failure test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ReminderTestFactory.java
class ReminderTestFactory {

    static final UUID SYNTHETIC_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SYNTHETIC_OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    static CreateMedicationReminderRequest makeRequest() {
        CreateMedicationReminderRequest req = new CreateMedicationReminderRequest();
        req.setTitle("SYNTHETIC Vitamin D 1000IU");
        req.setScheduledAt(Instant.now().plus(Duration.ofDays(1)));
        req.setRecurrenceRule(null);
        return req;
    }

    static CreateMedicationReminderRequest makeRequest(Consumer<CreateMedicationReminderRequest> overrides) {
        CreateMedicationReminderRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    static Reminder makeReminder() {
        Reminder reminder = new Reminder();
        reminder.setReminderId(UUID.randomUUID());
        reminder.setOwnerUserId(SYNTHETIC_OWNER_ID);
        reminder.setReminderType("MEDICATION");
        reminder.setTitle("SYNTHETIC Vitamin D 1000IU");
        reminder.setScheduledAt(Instant.now().plus(Duration.ofDays(1)));
        reminder.setStatus("PENDING");
        reminder.setCreatedAt(Instant.now());
        reminder.setUpdatedAt(Instant.now());
        return reminder;
    }

    static Reminder makeReminder(Consumer<Reminder> overrides) {
        Reminder reminder = makeReminder();
        overrides.accept(reminder);
        return reminder;
    }
}
```

---

### REMINDER-TC-001 — Tạo reminder không có recurrence thành công

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.createMedicationReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-REMINDER-001`, `UC-46 §3.3.1.23`

**Preconditions:**
- Mock `IReminderRepository.save()` trả về SYNTHETIC Reminder entity (FX-001 based)
- Mock `IFcmService.scheduleNotification()` trả về "fake-message-id"

**Test Steps:**
1. Arrange: `makeRequest()` với recurrenceRule=null
2. Act: `reminderService.createMedicationReminder(request, SYNTHETIC_OWNER_ID)`
3. Assert: Verify response fields và mock calls

**Expected Result (PASS):**
- Response không null
- `response.reminderType` = "MEDICATION"
- `response.status` = "PENDING"
- `response.recurrenceRule` = null
- `response.notificationScheduled` = true
- `repository.save()` được gọi đúng 1 lần
- `fcmService.scheduleNotification()` được gọi đúng 1 lần
- Entity được save có `ownerUserId = SYNTHETIC_OWNER_ID`

**Expected Result (FAIL):**
- `reminderType` khác "MEDICATION"
- `repository.save()` không được gọi
- `fcmService.scheduleNotification()` không được gọi

**Current Status:** 🔴 Not written
**Implementation Note:** `reminderType` PHẢI được set tại service, không từ request. Xem C1 trong §17 TDS.

---

### REMINDER-TC-002 — Tạo reminder với RRULE hằng ngày thành công

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.createMedicationReminder()` — với recurrenceRule
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-REMINDER-001`, `BR-REMINDER-002`

**Preconditions:**
- Mock repo và FCM như TC-001
- Request có `recurrenceRule = "FREQ=DAILY;INTERVAL=1"` [FX-002]

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setRecurrenceRule("FREQ=DAILY;INTERVAL=1"))`
2. Act: `reminderService.createMedicationReminder(request, SYNTHETIC_OWNER_ID)`
3. Assert: response.recurrenceRule = "FREQ=DAILY;INTERVAL=1"

**Expected Result (PASS):**
- Response chứa `recurrenceRule = "FREQ=DAILY;INTERVAL=1"`
- Entity được save có `recurrenceRule = "FREQ=DAILY;INTERVAL=1"`
- FCM được gọi 1 lần với scheduledAt đúng

**Expected Result (FAIL):**
- recurrenceRule bị null hoặc modified
- RRULE validation reject "FREQ=DAILY;INTERVAL=1" (false negative)

**Current Status:** 🔴 Not written

---

### REMINDER-TC-003 — title blank → REMINDER-001

**Severity:** `HIGH`
**Feature Under Test:** `CreateMedicationReminderRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-REMINDER-001`

**Preconditions:**
- `@WebMvcTest(ReminderController.class)` setup
- Mock `IReminderService` — KHÔNG nên được gọi

**Test Steps:**
1. Arrange: Body `{ "title": "", "scheduledAt": "2026-06-27T07:00:00Z" }` [FX-003]
2. Act: `mockMvc.perform(post("/api/v1/reminders").content(body).header("Authorization", "Bearer [jwt]"))`
3. Assert: HTTP 400, `error.code = "REMINDER-001"`, detail field = "title"

**Expected Result (PASS):**
- HTTP 400
- `error.code` = "REMINDER-001"
- `IReminderService.createMedicationReminder()` KHÔNG được gọi

**Expected Result (FAIL):**
- HTTP 201 — reminder tạo với title rỗng
- HTTP 500 — exception chưa được handle

**Current Status:** 🔴 Not written
**Implementation Note:** `@NotBlank` trên field `title` + ControllerAdvice map MethodArgumentNotValidException → REMINDER-001.

---

### REMINDER-TC-004 — scheduledAt null → REMINDER-001

**Severity:** `HIGH`
**Feature Under Test:** DTO validation — `@NotNull scheduledAt`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-REMINDER-001`

**Test Steps:**
1. Arrange: Body `{ "title": "SYNTHETIC Vitamin", "scheduledAt": null }` — hoặc omit field
2. Act: POST /api/v1/reminders
3. Assert: HTTP 400, error.code = "REMINDER-001", detail field = "scheduledAt"

**Expected Result (PASS):**
- HTTP 400 với REMINDER-001

**Current Status:** 🔴 Not written

---

### REMINDER-TC-005 — scheduledAt ở quá khứ → REMINDER-002

**Severity:** `HIGH`
**Feature Under Test:** DTO validation — `@FutureOrPresent scheduledAt`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-REMINDER-001`

**Preconditions:**
- Clock đã biết giá trị — scheduledAt = "2020-01-01T08:00:00Z" [FX-004]

**Test Steps:**
1. Arrange: Body với `scheduledAt = "2020-01-01T08:00:00Z"`
2. Act: POST /api/v1/reminders
3. Assert: HTTP 400, error.code = "REMINDER-002"

**Expected Result (PASS):**
- HTTP 400 với REMINDER-002

**Expected Result (FAIL):**
- HTTP 201 — reminder tạo với scheduledAt ở quá khứ

**Current Status:** 🔴 Not written
**Implementation Note:** `@FutureOrPresent` trên `scheduledAt` field. ControllerAdvice map → REMINDER-002.

---

### REMINDER-TC-006 — recurrenceRule không hợp lệ → REMINDER-003

**Severity:** `MEDIUM`
**Feature Under Test:** RRULE validator trong `ReminderService` hoặc DTO
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-REMINDER-002`

**Preconditions:**
- recurrenceRule = "WEEKLY_INVALID" [FX-005] — không phải RRULE format

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setRecurrenceRule("WEEKLY_INVALID"))`
2. Act: Gọi `reminderService.createMedicationReminder(request, SYNTHETIC_OWNER_ID)`
3. Assert: `ReminderException` được ném với code "REMINDER-003"

**Expected Result (PASS):**
- `ReminderException` với code "REMINDER-003"
- `repository.save()` KHÔNG được gọi
- `fcmService.scheduleNotification()` KHÔNG được gọi

**Expected Result (FAIL):**
- Reminder được tạo với RRULE không hợp lệ

**Current Status:** 🔴 Not written
**Implementation Note:** Tạo `@ValidRRule` annotation hoặc validate trong service trước khi save. RRULE phải bắt đầu bằng `FREQ=`.

---

### REMINDER-TC-007 — FCM throw exception → reminder vẫn được lưu (non-blocking)

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService` FCM error handling
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-REMINDER-003`, `ADR-REMINDER-002`

**Preconditions:**
- Mock `repository.save()` trả về entity thành công
- Mock `fcmService.scheduleNotification()` throws `RuntimeException("FCM unavailable")` [FX-008]

**Test Steps:**
1. Arrange: Setup mock FCM để throw exception
2. Act: Gọi `reminderService.createMedicationReminder(makeRequest(), SYNTHETIC_OWNER_ID)`
3. Assert: Không có exception được throw từ service; response trả về với `notificationScheduled=false`

**Expected Result (PASS):**
- `ReminderResponse` trả về thành công (không throw exception)
- `response.notificationScheduled` = false
- `repository.save()` đã được gọi và thành công
- Warning log chứa FCM failure message

**Expected Result (FAIL):**
- `ReminderService` throw exception khi FCM fail (vi phạm ADR-REMINDER-002)
- Reminder không được lưu khi FCM fail (rollback không mong muốn)

**Current Status:** 🔴 Not written
**Implementation Note:** `scheduleFcmNotification()` PHẢI được wrap trong try-catch. Chỉ log warning, không re-throw.

---

### REMINDER-TC-008 — reminderType luôn = 'MEDICATION' (không nhận từ request)

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.createMedicationReminder()` — type hardcoding
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-REMINDER-001`, `C1` in TDS §17`

**Preconditions:**
- Request body KHÔNG có trường `reminderType`
- Mock repo và FCM như TC-001

**Test Steps:**
1. Arrange: `makeRequest()` — không có reminderType field
2. Act: `reminderService.createMedicationReminder(request, SYNTHETIC_OWNER_ID)`
3. Assert: Entity được save có `reminderType = "MEDICATION"`

**Expected Result (PASS):**
- Entity.reminderType = "MEDICATION" khi save
- Response.reminderType = "MEDICATION"
- `CreateMedicationReminderRequest` KHÔNG có trường `reminderType`

**Expected Result (FAIL):**
- Service nhận reminderType từ bên ngoài (null hoặc value khác)

**Current Status:** 🔴 Not written
**Implementation Note:** `ReminderService` phải tự set `reminder.setReminderType("MEDICATION")` không phụ thuộc request.

---

### REMINDER-TC-009 — GET reminder không thuộc owner → REMINDER-005

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.getReminder()` — RBAC ownership
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- Mock `findByIdAndOwnerUserId(reminderId, SYNTHETIC_OWNER_ID)` → Optional.empty() [FX-007]

**Test Steps:**
1. Arrange: reminderId = UUID của reminder thuộc SYNTHETIC_OTHER_ID
2. Act: `reminderService.getReminder(reminderId, SYNTHETIC_OWNER_ID)`
3. Assert: `ReminderException` với code "REMINDER-004" hoặc "REMINDER-005"

**Expected Result (PASS):**
- Exception ném với code "REMINDER-004" (not found) hoặc "REMINDER-005" (forbidden)
- Mother không nhận được data của người khác

**Expected Result (FAIL):**
- Service trả về reminder của người khác (RBAC bypass)

**Current Status:** 🔴 Not written
**Implementation Note:** Repository phải dùng `findByIdAndOwnerUserId()`, không phải `findById()`.

---

### REMINDER-TC-010 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: Không có Authorization header
2. Act: POST /api/v1/reminders
3. Assert: HTTP 401, error.code = "IAM-001"

**Expected Result (PASS):**
- HTTP 401
- `IReminderService` KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### REMINDER-TC-SEC-001 — XSS injection trong title field

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-79 — Cross-site Scripting`
**Feature Under Test:** `CreateMedicationReminderRequest.title` storage
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Arrange: title = `"<script>alert('xss')</script>"`
2. Act: POST /api/v1/reminders
3. Assert: title được lưu as-is (escaped) hoặc 400 reject; KHÔNG execute script

**Expected Result (PASS = hệ thống an toàn):**
- Title được lưu như raw string (sanitized nếu cần)
- Khi GET reminder, title được return dưới dạng escaped string
- Response headers có `Content-Type: application/json` (không phải HTML)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Response chứa unescaped HTML/JS trong JSON context

**Current Status:** 🔴 Not written

---

### REMINDER-TC-SEC-002 — IDOR cross-owner reminder access

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `IReminderRepository.findByIdAndOwnerUserId()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Arrange: SYNTHETIC Mother A xác thực; reminderId thuộc SYNTHETIC Mother B
2. Act: GET /api/v1/reminders/{reminderId_of_B} với JWT của A
3. Assert: HTTP 403 hoặc 404

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 403 hoặc 404
- Mother A KHÔNG nhận được data của Mother B

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### REMINDER-TC-INT-001 — Luồng E2E tạo reminder với DB thật

**Severity:** `HIGH`
**Feature Under Test:** Full flow: Controller → Service → Repository → PostgreSQL + FCM mock
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container (`@Testcontainers` auto-start)
- Flyway V1__init_schema.sql applied
- `IFcmService` mock (không call Firebase thật)
- Seed: SYNTHETIC Mother user trong `users` table

**Test Steps:**
1. Seed SYNTHETIC Mother user
2. POST /api/v1/reminders với JWT ROLE_MOTHER, body = FX-002 (với RRULE)
3. Assert HTTP 201 và extract reminderId
4. GET /api/v1/reminders/{reminderId} để verify read
5. Query DB trực tiếp để verify persistence

**Expected Result (PASS):**
- HTTP 201 với reminderId hợp lệ
- DB: 1 record trong reminders
  - `reminder_type = 'MEDICATION'`
  - `status = 'PENDING'`
  - `owner_user_id = SYNTHETIC_MOTHER_ID`
  - `recurrence_rule = 'FREQ=DAILY;INTERVAL=1'`
- FCM mock được gọi đúng 1 lần
- Audit log: MedicationReminderCreated event

**Expected Result (FAIL):**
- HTTP 500 — service/repository error
- DB: reminder_type != 'MEDICATION'
- FCM mock được gọi 0 lần hoặc >1 lần

**DB Assertion:**
```java
Map<String, Object> row = jdbcTemplate.queryForMap(
    "SELECT reminder_type, status, owner_user_id::text, recurrence_rule " +
    "FROM reminders WHERE reminder_id = ?",
    savedReminderId
);
assertThat(row.get("reminder_type")).isEqualTo("MEDICATION");
assertThat(row.get("status")).isEqualTo("PENDING");
assertThat(row.get("owner_user_id").toString()).isEqualTo(SYNTHETIC_MOTHER_ID.toString());
assertThat(row.get("recurrence_rule")).isEqualTo("FREQ=DAILY;INTERVAL=1");
```

**Current Status:** 🔴 Not written

---

### REMINDER-TC-INT-002 — FCM failure không rollback DB record

**Severity:** `HIGH`
**Feature Under Test:** Transactional boundary — FCM failure isolation
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL container
- `IFcmService` mock configured to throw RuntimeException

**Test Steps:**
1. Seed SYNTHETIC Mother user
2. Configure FCM mock: `when(fcmService.scheduleNotification(...)).thenThrow(new RuntimeException("FCM down"))`
3. POST /api/v1/reminders với body hợp lệ
4. Assert HTTP 201 (không phải 500)
5. Query DB: reminder vẫn tồn tại với status='PENDING'

**Expected Result (PASS):**
- HTTP 201 với `notificationScheduled=false`
- DB: reminder được lưu thành công
- Application log: warning level message về FCM failure

**Expected Result (FAIL):**
- HTTP 500 — FCM exception propagated
- DB: reminder không được lưu (transaction rollback)

**DB Assertion:**
```java
int count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM reminders WHERE owner_user_id = ?",
    Integer.class, SYNTHETIC_MOTHER_ID
);
assertThat(count).isEqualTo(1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REMINDER-TC-001` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-002` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-003` | `ReminderControllerTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-004` | `ReminderControllerTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-005` | `ReminderControllerTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-006` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-007` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-008` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-009` | `ReminderServiceTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-010` | `ReminderControllerTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-SEC-001` | `ReminderSecurityTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-SEC-002` | `ReminderSecurityTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-INT-001` | `ReminderIntegrationTest.java` | `[ ]` | `___` | — |
| `REMINDER-TC-INT-002` | `ReminderIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase stub
@Service
public class ReminderService implements IReminderService {

    @Override
    public ReminderResponse createMedicationReminder(CreateMedicationReminderRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ReminderResponse getReminder(UUID reminderId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ReminderResponse> listReminders(UUID userId, String type, String status) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REMINDER-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state |
| `REMINDER-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REMINDER-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REMINDER-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REMINDER-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement
- Log: `logs/red-gate-uc46-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-REMINDER-IMP-001` đã được review và approve
- [ ] `reminders` table xác nhận tồn tại trong staging (V1__init_schema.sql)
- [ ] Firebase Admin SDK đã được cấu hình trong staging
- [ ] `IFcmService` interface đã được thiết kế (có thể mock)
- [ ] Test fixtures FX-001 đến FX-008 đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (REMINDER-TC-001 đến REMINDER-TC-010)
- [ ] `./mvnw verify` — integration tests xanh (INT-001, INT-002)
- [ ] Test coverage ≥ 80% lines cho `ReminderService`
- [ ] `reminderType='MEDICATION'` được hardcode tại service layer (không từ request)
- [ ] FCM failure KHÔNG rollback DB record (non-blocking confirmed)
- [ ] Không có business logic trong `ReminderController`
- [ ] RRULE validation reject invalid strings
- [ ] RBAC: `findByIdAndOwnerUserId()` được dùng thay `findById()`
- [ ] Audit event `MedicationReminderCreated` emit sau mỗi tạo thành công

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 14 tests FAIL với stub
- [ ] **Contract Existence**:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — mọi test instance tạo via `ReminderTestFactory`
- [ ] **Oracle Source** — mọi expected value trace về BR/AC/ADR

### Suspension Criteria

- Firebase Admin SDK không configure được trong staging
- `reminders` table chưa có (V1 migration chưa chạy)
- CI pipeline broken

---

## 7. Rollback Plan

```bash
# Revert compound index migration nếu cần
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_reminders_owner_type_status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '4';"

# Revert implementation
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/test/java/com/carebridge/backend/reminder/

# UC-46 vẫn OPEN → giữ entry trong backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-REMINDER-001/002/003 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test cho phép reminderType từ request (vi phạm C1) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller validate RRULE format (vi phạm C6) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test inject `FirebaseMessaging` trực tiếp thay vì `IFcmService` | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — CB-REMINDER-IMP-001-TEST — UC-46 Create Medication Reminder — Draft 2026-06-26*
