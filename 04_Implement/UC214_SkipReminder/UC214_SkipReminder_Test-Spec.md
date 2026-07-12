# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-214 Skip Reminder

**Document ID:** `CB-REM-TDD-004`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `TV2-Bách`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC214_SkipReminder/UC214_SkipReminder_TDS.md` (CB-REM-IMP-004)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.16.3 Skip Reminder (~line 4605)
- Ground truth: `Reminder.java`, `ReminderStatus.java`, `ReminderRepository.java`, `ReminderServiceImpl.java`, `AuditAction.java`
- Schema oracle: `V20260627100300__add_reminder_columns.sql` + applied migrations
- UI/UX: `03_Design/UI_UX/MobileAppScreen/CB-274 Skip Reminder (UC-214)/code.html`

> **Quy ước TDD:** test (`.java`) → chạy → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo TDD spec cho UC-214 Skip Reminder (Draft) |

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
| **Feature / Gap ID** | `UC-214` |
| **Module** | `SkipReminder — reminder (Bounded Context)` |
| **Spec gốc** | `CB-REM-IMP-004` |
| **Priority** | 🟡 P2 (SRS Priority: Medium) |
| **Sprint** | `S[N]` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-SAFETY, PDPA` |
| **Upstream Dependencies** | `auth (JWT), reminders table, audit, notification (FCM)` |
| **Downstream Consumers** | `reminder detail (UC-212), audit trail` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REM-IMP-004 §17`, `ADR-REM-002 / ADR-REM-STATE-001 / ADR-REM-RECUR-001 / ADR-REM-SKIP-001` |
| **Constraints Injected** | C1 ownership-scoped lookup; C2 PENDING-only skip → REM-011; C3 no-delete/no-null recurrence; C4 skipReason audit-only; C5 callerId from JWT; C6 recurrence materialize Open→flag OFF; C7 no medical advice |
| **Model** | `Claude Opus 4.8` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Persistence oracle = `V1__init_schema.sql` + applied migrations. Test encode hành vi **đã sửa** theo ground truth, không phải theo brief.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy / code) | Fix áp dụng trong test |
|---|------------------------|----------------------------------|------------------------|
| L1 | UI mockup có textarea "Lý do bỏ qua" ⇒ ngụ ý có `skip_reason` | Bảng `reminders` **KHÔNG** có column `skip_reason` (xác nhận `Reminder.java` + migrations) | Test: `skipReason` chỉ vào audit details, KHÔNG assert column reminders (ADR-REM-SKIP-001) |
| L2 | Brief nói entity có `recurrenceRule` (`recurrence_rule`) | Code thực tế: KHÔNG có `recurrence_rule`; recurrence = `recurrenceType` enum (`NONE/DAILY/WEEKLY/MONTHLY`) + `recurrenceEndDate` (`Instant`) | Test dùng `recurrenceType`, không tham chiếu `recurrence_rule` |
| L3 | UC-212 TDS prose dùng `accountId` | Code thực tế: field là `ownerUserId` / column `owner_user_id` | Test + factory dùng `ownerUserId` |
| L4 | Thuật toán "occurrence kế tiếp" giả định | KHÔNG được document ở đâu (Open-1..Open-4, ADR-REM-RECUR-001); UC-45 chỉ lưu 1 row, không có scheduler materialize | Test materialize (TC-002) gắn cờ Implementation Note "feature-flag ON"; oracle đánh dấu Open; TC-003 cover recurrence-ended |
| L5 | `scheduledAt` kiểu ngày/giờ local | Code thực tế: `Instant` (UTC) | Test dùng `Instant` |
| L6 | Enum có `SNOOZED` (UC-48 Draft) | `ReminderStatus` thực tế: chỉ `PENDING/COMPLETED/SKIPPED/CANCELLED` | Test terminal-state chỉ dùng COMPLETED/SKIPPED/CANCELLED |
| L7 | Non-owner ⇒ 403 (như UC-212) | UC-214 đề xuất ownership-scoped `findByIdAndOwnerUserId` ⇒ 404 REM-006 (anti-IDOR) — nhưng lựa chọn 403 vs 404 là **Open** (§6.4 TDS) | TC-004 oracle ghi cả hai; mặc định assert REM-006/404, Implementation Note nêu nhánh 403 |
| L8 | `AuditAction` có sẵn action skip | Ground truth: chỉ `REMINDER_CREATED` tồn tại | Implementation phải thêm `REMINDER_SKIPPED`; test assert audit dùng action mới này |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
SkipReminder bao gồm các layer:
├── Service (ReminderServiceImpl.skipReminder) — mock ReminderRepository/AuditService/INotificationService (Mockito)
├── Controller (ReminderController POST /{id}/skip) — @WebMvcTest, mock IReminderService
└── Integration (@SpringBootTest + Testcontainers PostgreSQL) — full flow qua DB
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.3.16.3` | Skip một occurrence không xóa recurrence config; actor Mother; "Chỉ lần này" |
| `ADR-REM-002` | Owner-only mutate → non-owner reject |
| `ADR-REM-STATE-001` | Terminal-state: chỉ PENDING mới skip; đã terminal → REM-011 |
| `ADR-REM-RECUR-001` | Materialize occurrence kế tiếp (Open) |
| `ADR-REM-SKIP-001` | skipReason không persist vào reminders |
| `BR-SAFETY-002` | Không medical advice trong response/audit |
| `BR-RBAC / PDPA` | callerId từ JWT; audit ghi thao tác |
| `CB-REM-IMP-004 §8, §10` | Interface contract + error codes REM-011..014 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner skip PENDING non-recurring → SKIPPED | `skipReminder()` | `REM214-TC-001` |
| TC-COND-002 | Owner skip PENDING recurring → SKIPPED + next occurrence | `materializeNextOccurrence()` | `REM214-TC-002` |
| TC-COND-003 | Recurring quá end-date → SKIPPED, không tạo row mới | `materializeNextOccurrence()` | `REM214-TC-003` |
| TC-COND-004 | Non-owner → reject, no side-effect | ownership-scoped lookup | `REM214-TC-004`, `REM214-TC-012` |
| TC-COND-005 | Not found → 404 REM-006 | repo lookup | `REM214-TC-005` |
| TC-COND-006 | Already-terminal → 409 REM-011 | state guard | `REM214-TC-006/007/008` |
| TC-COND-007 | skipReason không persist vào reminders | audit vs entity | `REM214-TC-009` |
| TC-COND-008 | skipReason quá dài → 400 REM-012 | DTO validation | `REM214-TC-010` |
| TC-COND-009 | Audit `REMINDER_SKIPPED` emitted | `AuditService.log()` | `REM214-TC-011` |
| TC-COND-010 | No medical advice in output | response mapping | `REM214-TC-013` |
| TC-COND-011 | Recurrence config không bị null-hóa | INV-2 | `REM214-TC-001`, `REM214-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | `ReminderStatus` (PENDING→SKIPPED; terminal reject) | Core invariant INV-1 |
| Equivalence Partitioning | recurrenceType {NONE} vs {DAILY/WEEKLY/MONTHLY} | Non-recurring vs recurring path |
| Boundary Value Analysis | skipReason length = 1000 (ok) / 1001 (REM-012); recurrence next vs end-date | Validation & recurrence stop |
| Error Guessing (Security) | IDOR trên reminderId của user khác | CWE-639 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{id:REM-001, ownerUserId:U-001, type:MEDICATION, status:PENDING, recurrenceType:NONE}` | Happy non-recurring |
| `FX-002` | DB seed | `{id:REM-002, ownerUserId:U-001, status:PENDING, recurrenceType:DAILY, recurrenceEndDate:null}` | Happy recurring |
| `FX-003` | DB seed | `{id:REM-003, ownerUserId:U-001, status:PENDING, recurrenceType:DAILY, recurrenceEndDate:<past>}` | Recurrence ended |
| `FX-004` | DB seed | `{id:REM-004, ownerUserId:U-001, status:SKIPPED}` | Double-skip |
| `FX-005` | DB seed | `{id:REM-005, ownerUserId:U-001, status:COMPLETED}` | Terminal completed |
| `FX-006` | DB seed | `{id:REM-006c, ownerUserId:U-001, status:CANCELLED}` | Terminal cancelled |
| `FX-007` | JWT | `{ sub: U-001, role: MOTHER }` | Owner auth context |
| `FX-008` | JWT | `{ sub: U-999, role: MOTHER }` | Non-owner / IDOR |
| `FX-009` | input | `skipReason` = 1001-char string | REM-012 boundary |

---

## 4. Test Case Specification

> **TC ID format:** `REM214-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation. Mỗi @Test gọi factory (fresh instance).
// Ground truth: Reminder dùng ownerUserId, scheduledAt(Instant), recurrenceType enum.
// ═══════════════════════════════════════════════════════════
class SkipReminderTestFactory {

    static final UUID OWNER   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID STRANGER= UUID.fromString("00000000-0000-0000-0000-000000000999");

    // Baseline: PENDING, non-recurring (đồng bộ FX-001)
    static Reminder makePendingNonRecurring() {
        return Reminder.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000101"))
            .ownerUserId(OWNER)
            .reminderType(ReminderType.MEDICATION)
            .title("Uống thuốc điều trị cao huyết áp")
            .scheduledAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .recurrenceType(RecurrenceType.NONE)
            .status(ReminderStatus.PENDING)
            .build();
    }

    // Overload override
    static Reminder makePending(Consumer<Reminder> overrides) {
        Reminder r = makePendingNonRecurring();
        overrides.accept(r);
        return r;
    }

    // Recurring DAILY, no end date (FX-002)
    static Reminder makePendingDaily() {
        return makePending(r -> {
            r.setId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
            r.setRecurrenceType(RecurrenceType.DAILY);
            r.setRecurrenceEndDate(null);
        });
    }

    static Reminder makeWithStatus(ReminderStatus s) {
        return makePending(r -> r.setStatus(s));
    }
}
```

---

### REM214-TC-001 — Owner skip PENDING non-recurring → SKIPPED

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderServiceImpl.skipReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-011`
**Oracle Source:** `SRS §3.3.16.3` (skip one occurrence) + `ADR-REM-STATE-001` (PENDING→SKIPPED) + `Reminder.java` (recurrence_type preserved)

**Preconditions:** FX-001; repo mock trả `makePendingNonRecurring()` cho `findByIdAndOwnerUserId(REM-001, U-001)`.

**Test Steps:**
1. Arrange: mock repo → FX-001; mock `save` trả chính reminder.
2. Act: `skipReminder(REM-001, U-001, new SkipReminderRequest())`.
3. Assert: status, nextOccurrenceId, recurrenceType.

**Expected Result (PASS):**
- `reminder.getStatus() == SKIPPED`
- `response.getNextOccurrenceId() == null`, `response.getNextScheduledAt() == null`
- `reminder.getRecurrenceType() == RecurrenceType.NONE` (không bị null-hóa)
- `repo.save(reminder)` gọi đúng 1 lần; **không** có INSERT thứ hai.

**Expected Result (FAIL):** status vẫn PENDING, hoặc recurrenceType bị set null, hoặc tạo occurrence mới cho non-recurring.

**Current Status:** 🔴 Not written

---

### REM214-TC-002 — Owner skip PENDING recurring (DAILY) → SKIPPED + occurrence kế tiếp

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderServiceImpl.skipReminder()` + `materializeNextOccurrence()`
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `SRS §3.3.16.3` ("without deleting the recurrence configuration" + UI "Lần nhắc kế tiếp: Mai") + `ADR-REM-RECUR-001` (Option A). ⚠️ Bước nhảy `+1 day` là **Open-1** — Implementation Note bên dưới.

**Preconditions:** FX-002; feature-flag materialize = ON (nếu Open chưa chốt → xem Implementation Note).

**Test Steps:**
1. Arrange: repo mock → `makePendingDaily()`; capture các đối tượng `save(...)`.
2. Act: `skipReminder(REM-002, U-001, {})`.
3. Assert: original SKIPPED + một row PENDING mới.

**Expected Result (PASS):**
- Row gốc `REM-002.status == SKIPPED`.
- Một `Reminder` mới được `save` với `status == PENDING`, `ownerUserId == U-001`, `recurrenceType == DAILY` (config sao chép).
- `response.getNextOccurrenceId() != null`, `response.getNextScheduledAt()` = `original.scheduledAt + 1 day` **(Open-1 — assert bước nhảy chỉ khi thuật toán được Tech Lead chốt; nếu chưa, assert `!= null` và `> original.scheduledAt`)**.

**Expected Result (FAIL):** recurrence config bị mất; hoặc không có row mới; hoặc row mới cũng SKIPPED.

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu Open-1..Open-4 chưa chốt (C6), `materializeNextOccurrence` để flag OFF ⇒ test này chuyển sang oracle "flag OFF": `nextOccurrenceId == null`. TC không được PASS bằng cách bịa thuật toán recurrence.

---

### REM214-TC-003 — Skip recurring đã quá recurrence_end_date → SKIPPED, KHÔNG tạo row mới

**Severity:** `HIGH`
**Feature Under Test:** `materializeNextOccurrence()` stop condition
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REM-RECUR-001 Open-4` (next > recurrence_end_date ⇒ no new row) + `Reminder.recurrenceEndDate`

**Preconditions:** FX-003 (`recurrenceEndDate` ở quá khứ).

**Expected Result (PASS):** `REM-003.status == SKIPPED`; **không** có `save` INSERT thứ hai; `response.nextOccurrenceId == null`.

**Expected Result (FAIL):** tạo occurrence kế tiếp vượt quá end-date.

**Current Status:** 🔴 Not written
**Implementation Note:** So sánh mốc `Instant` — cách so (đầu/cuối ngày) là Open-4; test dùng end-date rõ ràng ở quá khứ để tránh biên mơ hồ.

---

### REM214-TC-004 — Non-owner skip → reject, no side-effect

**Severity:** `CRITICAL`
**Feature Under Test:** ownership-scoped lookup (`findByIdAndOwnerUserId`)
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REM-002` (owner-only) + `CB-REM-IMP-004 §6.4` (404 REM-006 mặc định; 403 REM-004 là nhánh Open)

**Preconditions:** REM-001 thuộc U-001; caller U-999. `findByIdAndOwnerUserId(REM-001, U-999)` → `Optional.empty()`.

**Expected Result (PASS):** throw `BusinessException` với `code == "REM-006"`, `httpStatus == NOT_FOUND`; **không** gọi `save`; **không** gọi audit.

**Expected Result (FAIL):** skip thành công cho non-owner (IDOR), hoặc lộ existence qua 403 khi thiết kế chọn 404.

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu Tech Lead chốt nhánh 403 (nhất quán UC-212), đổi oracle sang `REM-004 / FORBIDDEN` + dùng `findById` rồi so `ownerUserId`. Chỉ một nhánh được active tại thời điểm implement.

---

### REM214-TC-005 — Not found → 404 REM-006

**Severity:** `HIGH`
**Feature Under Test:** repo lookup miss
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `IReminderService` javadoc (REM-006/404) — nhất quán UC-212 `getReminderDetail`

**Expected Result (PASS):** throw `BusinessException` `REM-006` (404).

**Current Status:** 🔴 Not written

---

### REM214-TC-006 — Skip already-SKIPPED → 409 REM-011

**Severity:** `HIGH`
**Feature Under Test:** state guard (INV-1)
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-REM-STATE-001` + `CB-REM-IMP-004 §10` (REM-011/409)

**Preconditions:** FX-004 (status=SKIPPED).

**Expected Result (PASS):** throw `BusinessException` `REM-011` (409); status không đổi; no audit/save.

**Current Status:** 🔴 Not written

---

### REM214-TC-007 — Skip already-COMPLETED → 409 REM-011

**Severity:** `HIGH`
**Feature Under Test:** state guard (INV-1)
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-REM-STATE-001` + §10 REM-011

**Preconditions:** FX-005 (status=COMPLETED).

**Expected Result (PASS):** throw `BusinessException` `REM-011` (409).

**Current Status:** 🔴 Not written

---

### REM214-TC-008 — Skip already-CANCELLED → 409 REM-011

**Severity:** `MEDIUM`
**Feature Under Test:** state guard (INV-1)
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-REM-STATE-001` + §10 REM-011

**Preconditions:** FX-006 (status=CANCELLED).

**Expected Result (PASS):** throw `BusinessException` `REM-011` (409).

**Current Status:** 🔴 Not written

---

### REM214-TC-009 — skipReason nhận được nhưng KHÔNG persist vào reminders

**Severity:** `MEDIUM`
**Feature Under Test:** `skipReminder()` audit path
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-REM-SKIP-001` (skipReason → audit details only; không có column `skip_reason`)

**Preconditions:** FX-001; request `{skipReason:"Đã uống sớm hơn"}`.

**Test Steps:**
1. Act: `skipReminder(REM-001, U-001, {skipReason:"Đã uống sớm hơn"})`.
2. Assert audit + entity.

**Expected Result (PASS):**
- `AuditService.log(eq(REMINDER_SKIPPED), eq(U-001), eq("Reminder"), eq(REM-001), detailsCaptor)` được gọi; details chứa `"Đã uống sớm hơn"`.
- Entity `Reminder` **không** có field nào lưu `skipReason` (verify qua reflection/không tồn tại getter) — đảm bảo không có schema drift.

**Expected Result (FAIL):** skipReason được set vào entity / xuất hiện trong bảng reminders.

**Current Status:** 🔴 Not written

---

### REM214-TC-010 — skipReason vượt max length → 400 REM-012

**Severity:** `LOW`
**Feature Under Test:** `SkipReminderRequest` bean validation (`@Size(max=1000)`)
**Test File:** `src/test/java/com/carebridge/backend/reminder/SkipReminderControllerTest.java` (@WebMvcTest)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-REM-IMP-004 §8.1` (`@Size(max=1000, message="REM-012")`) + §10 REM-012/400

**Preconditions:** FX-009 (1001-char skipReason).

**Expected Result (PASS):** HTTP 400, body error code `REM-012`; service `skipReminder` **không** được gọi.

**Current Status:** 🔴 Not written

---

### REM214-TC-011 — Audit `REMINDER_SKIPPED` được emit đúng payload

**Severity:** `HIGH`
**Feature Under Test:** `skipReminder()` → `AuditService.log()`
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-REM-IMP-004 §7` (ReminderSkipped) + `AuditAction.REMINDER_SKIPPED` (phải được thêm) + UC-45 pattern `auditService.log(...)`

**Expected Result (PASS):** `AuditService.log(REMINDER_SKIPPED, U-001, "Reminder", REM-001, details)` gọi đúng 1 lần **sau** khi status set SKIPPED (thứ tự: save → audit).

**Expected Result (FAIL):** dùng `REMINDER_CREATED`; hoặc audit gọi trước khi save; hoặc không audit.

**Current Status:** 🔴 Not written
**Implementation Note:** Yêu cầu thêm enum `AuditAction.REMINDER_SKIPPED` — nếu thiếu, compile fail (Contract Existence gate).

---

### REM214-TC-013 — Response/audit KHÔNG chứa medication dosage (BR-SAFETY)

**Severity:** `HIGH`
**Feature Under Test:** `SkipReminderResponse` mapping
**Test File:** `SkipReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-SAFETY-002` (no medical advice) — nhất quán UC-212 TC-005

**Test Steps:**
```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("dosage");
assertThat(json).doesNotContain("prescription");
assertThat(json.toLowerCase()).doesNotContain("diagnos");
```

**Expected Result (PASS):** JSON không chứa dosage/prescription/diagnos.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### REM214-TC-012 — IDOR: skip reminder của người khác qua ID

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — bảo vệ health data của người khác`
**Feature Under Test:** `POST /api/v1/reminders/{id}/skip` end-to-end guard
**Test File:** `src/test/java/com/carebridge/backend/reminder/SkipReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REM-002` + `CB-REM-IMP-004 §6.4` (anti-IDOR: empty owner-scoped lookup → 404, không lộ existence)

**Preconditions:** FX-008 (JWT U-999 hợp lệ, role MOTHER); REM-001 thuộc U-001.

**Test Steps (Attack Simulation):**
1. Đăng nhập U-999 (JWT hợp lệ).
2. `POST /api/v1/reminders/REM-001/skip` với JWT U-999.
3. Kiểm tra response + DB state của REM-001.

**Expected Result (PASS = an toàn):** HTTP 404 (`REM-006`); `REM-001.status` vẫn `PENDING`; không audit `REMINDER_SKIPPED` cho REM-001.

**Expected Result (FAIL = lỗ hổng):** 200 và REM-001 bị SKIPPED bởi U-999; hoặc 403 tiết lộ reminder tồn tại (khi thiết kế chọn 404).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### REM214-TC-INT-001 — Full flow POST /skip trên recurring reminder (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /skip → DB (SKIPPED row + optional next PENDING row)`
**Test File:** `src/test/java/com/carebridge/backend/reminder/SkipReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002, TC-COND-011`
**Oracle Source:** `V20260627100300__add_reminder_columns.sql` + `Reminder.java` (persistence oracle) + `ADR-REM-RECUR-001`

**Preconditions:** PostgreSQL container; Flyway applied; seed REM-002 (PENDING, DAILY, owner U-001) qua JPA.

**Test Steps:**
1. Seed REM-002.
2. `POST /api/v1/reminders/REM-002/skip` (JWT U-001).
3. Assert DB.

**Expected Result (PASS):**
- Response 200, `status=SKIPPED`.
- `reminders` row REM-002: `status='SKIPPED'`, `recurrence_type='DAILY'` (không null).
- (flag ON) đúng 1 row PENDING mới cùng `owner_user_id`, `recurrence_type='DAILY'`.
- Tổng số row không giảm (INV-3: không DELETE).

**DB Assertion:**
```java
Reminder skipped = reminderRepository.findById(REM_002).orElseThrow();
assertThat(skipped.getStatus()).isEqualTo(ReminderStatus.SKIPPED);
assertThat(skipped.getRecurrenceType()).isEqualTo(RecurrenceType.DAILY); // INV-2
```

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu materialize flag OFF (Open chưa chốt), bỏ assert "row PENDING mới" và assert `nextOccurrenceId == null`.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REM214-TC-001` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-002` | `SkipReminderServiceTest.java` | `[ ]` | `___` | extract `materializeNextOccurrence` |
| `REM214-TC-003` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-004` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-005` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-006` | `SkipReminderServiceTest.java` | `[ ]` | `___` | parametrize terminal states |
| `REM214-TC-007` | `SkipReminderServiceTest.java` | `[ ]` | `___` | (see TC-006) |
| `REM214-TC-008` | `SkipReminderServiceTest.java` | `[ ]` | `___` | (see TC-006) |
| `REM214-TC-009` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-010` | `SkipReminderControllerTest.java` | `[ ]` | `___` | — |
| `REM214-TC-011` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-012` | `SkipReminderControllerTest.java` | `[ ]` | `___` | — |
| `REM214-TC-013` | `SkipReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM214-TC-INT-001` | `SkipReminderIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ suite với stub throw. **Mọi test PHẢI FAIL.** Test PASS ngay → AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**

```java
@Service
public class ReminderServiceImpl implements IReminderService {
    // ... createReminder / getReminderDetail giữ nguyên ...

    @Override
    public SkipReminderResponse skipReminder(UUID reminderId, UUID callerId, SkipReminderRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REM214-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `REM214-TC-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-004` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-005` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-007` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-008` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-009` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-010` | `throw`/no-bean | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-011` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-012` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-013` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM214-TC-INT-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-REM-IMP-004` được review và **Approved** (hiện `Draft`)
- [ ] ADR-REM-STATE-001 / ADR-REM-RECUR-001 / ADR-REM-SKIP-001 chuyển `Accepted`; Open-1..Open-4 + quyết định 403-vs-404 (§6.4) được Tech Lead chốt
- [ ] Logic Issues (§2) confirm với Principal Architect
- [ ] Không cần migration (xác nhận §5.2 TDS) — chỉ thêm `AuditAction.REMINDER_SKIPPED`
- [ ] Test fixtures (TDS-05) chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không skip)
- [ ] `./mvnw verify` — integration test (Testcontainers) xanh
- [ ] Coverage ≥ 80% lines cho `skipReminder()`
- [ ] Không có business logic trong Controller (chỉ validation + mapping)
- [ ] Không PII/secret plaintext trong logs (kể cả skipReason nếu ADR-REM-SKIP-001 chốt audit-only)
- [ ] Recurrence config của row bị skip KHÔNG bị null-hóa (INV-2)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub throw trước implement
- [ ] **Contract Existence** — `AuditAction.REMINDER_SKIPPED`, `SkipReminderRequest/Response`, `skipReminder()` compile:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"   # Expected: no output
  ```
- [ ] **Props Isolation** — mọi Reminder tạo qua `SkipReminderTestFactory`
- [ ] **Oracle Source** — mọi expected value có nguồn (BR/AC/ADR/SRS)

### Suspension Criteria

- Open-1..Open-4 (recurrence) hoặc quyết định 403-vs-404 chưa được chốt → materialize/ownership branch bị chặn
- ADR còn `Proposed`
- CI pipeline broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# KHÔNG có migration cho UC-214 — không drop table.
# Revert code implementation + audit enum + tests
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/

# Nếu materialize flag đã ON và tạo row occurrence sai → data remediation thủ công (Tech Lead + DPO):
# psql ... DELETE FROM reminders WHERE status='PENDING' AND created_at > '<deploy-time>' AND <điều kiện xác định row sai>;

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | TC assume thuật toán recurrence không có trong ADR (bịa bước nhảy) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test dùng `accountId`, `recurrence_rule`, hay `skip_reason` column (không tồn tại) | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

## 19. Implementation Sync

| Date | Status | Evidence |
|---|---|---|
| 2026-07-10 | `Partially Implemented` | `mvnw test -Dtest="UpdateReminderServiceTest,ReminderSecurityTest"` → 27/27 passing; `mvnw test -Dtest="*Reminder*Test,*TodayTask*Test"` → 61/61 passing. Full `mvnw test` remains red from unrelated existing failures, so truthful sync is not `Implemented`. |

*TDD Template v2.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol. Status: Draft — không implement khi TDS còn Draft / ADR còn Proposed.*
