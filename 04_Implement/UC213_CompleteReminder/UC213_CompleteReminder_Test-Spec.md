# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-213 Complete Reminder

**Document ID:** `CB-REM-TDD-213`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC213_CompleteReminder/UC213_CompleteReminder_TDS.md` (CB-REM-IMP-213)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.16.2 (Table 235)
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (reminders 715–728) + `V20260627100300__add_reminder_columns.sql`
- Code: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/`
- Prior art: UC-212 TDD `CB-REM-TDD-002`, UC-48 `ADR-REM-STATE-001`

> **Quy ước TDD:** test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo TDD spec cho UC-213 Complete Reminder (Draft) |

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
| **Feature / Gap ID** | `UC-213` |
| **Module** | `CompleteReminder — Bounded Context: reminder` |
| **Spec gốc** | `CB-REM-IMP-213` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S[N]` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC (SRS §3.3.16.2), BR-SAFETY, PDPA` |
| **Upstream Dependencies** | `auth (JWT), reminders table, INotificationService (FCM)` |
| **Downstream Consumers** | `audit, notification (FCM cancel), UC-212, UC-49` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REM-IMP-213 §17`, `ADR-REM-002`, `ADR-REM-213-001`, `ADR-REM-213-002 (Open)` |
| **Constraints Injected** | Ownership (REM-009), terminal guard (REM-007), not-found (REM-008), audit `REMINDER_COMPLETED`, no medication advice, recurrence path gated (C6) |
| **Model** | `Claude Opus 4.8` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Nguồn phân xử schema: `V1__init_schema.sql` + `V20260627100300`. ERD chỉ là bằng chứng phụ.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy / code) | Fix áp dụng trong test |
|---|------------------------|----------------------------------|------------------------|
| L1 | SRS §3.3.16.2: "updates the next recurrence if applicable" — **không mô tả cơ chế** | Bảng `reminders` 1 row/reminder; entity map `recurrenceType` enum (không map `recurrence_rule`); create-new-row vs mutate chưa chốt → `ADR-REM-213-002` **Open** | Test recurring (`TC-002/003`) đánh dấu **phụ thuộc Open item**; encode Phương án (a) đề xuất nhưng gate sau sign-off. Không test bất kỳ RRULE parsing nào |
| L2 | UI mockup `CB-273` hiển thị "Người nhận: Bà Nội" | `reminders` **không có** cột recipient/shared-with; `baby_profiles.nickname` không khớp ngữ nghĩa "bà" | Không có field recipient trong response; `TC-010` assert response không có recipient |
| L3 | Exemplar UC-212/45/48 prose dùng `ZonedDateTime scheduledAt` và `accountId` | Entity thật: `scheduledAt`/`recurrenceEndDate`/`snoozedUntil` là **`Instant`**; field ownership là **`ownerUserId`** (cột `owner_user_id`), KHÔNG phải `accountId` | Factory + assertions dùng `Instant` và `ownerUserId` |
| L4 | UC-48 Draft đề xuất `ReminderStatus.SNOOZED` | Enum thật `ReminderStatus` = **PENDING/COMPLETED/SKIPPED/CANCELLED** (không SNOOZED) | Terminal set = {COMPLETED, SKIPPED, CANCELLED}; không test SNOOZED |
| L5 | Draft UC-46/47/48 dùng prefix `REMINDER-` | Code thật wired **`REM-`** (REM-001/004/006 đang chạy) | UC-213 dùng `REM-007..010`; test assert đúng chuỗi mã `REM-00X` |
| L6 | UI mockup có nút "Hoàn tác" (Undo) | `ADR-REM-213-001`: COMPLETED là terminal, không undo phía server | Không có test undo; `TC-004` xác nhận complete-lại → 409 |
| L7 | `AuditAction` enum | Chỉ có `REMINDER_CREATED`; `REMINDER_COMPLETED` **chưa tồn tại** | `TC-009` yêu cầu thêm enum value; test verify `auditService.log(REMINDER_COMPLETED,...)` |
| L8 | `INotificationService` | Chỉ có `scheduleFcmPush(...)`; **không có** `cancelFcmPush` | `TC-009` yêu cầu bổ sung method; mock verify `cancelFcmPush(fcmJobId)` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi
```
CompleteReminder bao gồm các layer:
├── Application/Service (mock ReminderRepository, INotificationService, AuditService với Mockito) — trọng tâm
├── Controller (@WebMvcTest, mock IReminderService) — RBAC + status mapping
└── Integration (Testcontainers PostgreSQL + Flyway) — persistence side-effect
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-213 §3.3.16.2` | Complete → status COMPLETED; "updates next recurrence if applicable"; BR-RBAC |
| `ADR-REM-002` | Owner-only → non-owner 403 (REM-009) |
| `ADR-REM-213-001` | Terminal guard → complete trên terminal state 409 (REM-007) |
| `ADR-REM-213-002` (Open) | Recurring: tạo lần kế tiếp (Phương án a) — test gated |
| `BR-SAFETY-002` (UC-212) | Response không dosage/prescription/diagnosis |
| `V1__init_schema.sql` + `V20260627100300` | Cột `status`, `scheduled_at`, `recurrence_type`, `recurrence_end_date`, `owner_user_id` |
| PDPA / BR-RBAC | Audit `REMINDER_COMPLETED`; identity từ JWT |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner completes PENDING non-recurring | `ReminderServiceImpl.completeReminder()` | `REM213-TC-001` |
| TC-COND-002 | Owner completes recurring → lần kế tiếp (Open) | `advanceRecurrence()` | `REM213-TC-002` |
| TC-COND-003 | Recurring vượt `recurrence_end_date` → không tạo (Open) | `advanceRecurrence()` boundary | `REM213-TC-003` |
| TC-COND-004 | Complete reminder đã terminal → 409 | `assertNotTerminal()` | `REM213-TC-004/005/006` |
| TC-COND-005 | Non-owner → 403 | ownership check | `REM213-TC-007` |
| TC-COND-006 | Not found → 404 | repo lookup | `REM213-TC-008` |
| TC-COND-007 | Audit + FCM cancel side-effects | `auditService.log`, `cancelFcmPush` | `REM213-TC-009` |
| TC-COND-008 | No medication advice / no recipient trong response | response mapping | `REM213-TC-010` |
| TC-COND-009 | Persistence side-effect (DB) | Testcontainers | `REM213-TC-INT-001` |
| TC-COND-010 | Auth: no JWT 401 / EXPERT 403 | `@PreAuthorize` | `REM213-TC-SEC-001/002` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | `ReminderStatus` (PENDING→COMPLETED; terminal reject) | Đảm bảo invariant terminal-state |
| Equivalence Partitioning | terminal set {COMPLETED,SKIPPED,CANCELLED} vs PENDING | Phân vùng hợp lệ/không hợp lệ |
| Boundary Value Analysis | recurring `next` vs `recurrence_end_date` | Ranh giới tạo/không tạo lần kế tiếp |
| Error Guessing | non-owner, not-found, no JWT, wrong role | Attack/misuse vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{id: REM-001, ownerUserId: ACC-001, type: MEDICATION, status: PENDING, recurrenceType: NONE, fcmJobId: "job-1"}` | Happy non-recurring |
| `FX-002` | DB seed | `{id: REM-002, ownerUserId: ACC-001, status: COMPLETED}` | Terminal reject |
| `FX-003` | DB seed | `{id: REM-003, ownerUserId: ACC-001, status: SKIPPED}` | Terminal reject |
| `FX-004` | DB seed | `{id: REM-004, ownerUserId: ACC-001, status: CANCELLED}` | Terminal reject |
| `FX-005` | DB seed | `{id: REM-005, ownerUserId: ACC-999, status: PENDING}` | Non-owner (caller ACC-001) |
| `FX-010` | DB seed | `{id: REM-050, ownerUserId: ACC-001, status: PENDING, recurrenceType: DAILY, recurrenceEndDate: null, scheduledAt: T}` | Recurring create-next (Open) |
| `FX-011` | DB seed | `{id: REM-051, ownerUserId: ACC-001, status: PENDING, recurrenceType: DAILY, recurrenceEndDate: T (=scheduledAt), scheduledAt: T}` | Recurring boundary (next>end) |
| `FX-JWT-owner` | JWT | `{ sub: ACC-001, role: MOTHER }` | Owner auth |
| `FX-JWT-expert` | JWT | `{ sub: ACC-500, role: EXPERT }` | RBAC reject |

---

## 4. Test Case Specification

> **TC ID format:** `REM213-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> Mỗi `@Test` tạo fresh instance qua factory. Không shared mutable state (chống AP-AI-002).

```java
// ═══════════════════════════════════════════════════════════
// CompleteReminderTestFactory.java — Props Isolation Pattern
// Đồng bộ với FX-* (§3 TDS-05). Chú ý: entity dùng Instant + ownerUserId (KHÔNG accountId).
// ═══════════════════════════════════════════════════════════
class CompleteReminderTestFactory {

    static final UUID OWNER   = UUID.fromString("00000000-0000-0000-0000-000000000001"); // ACC-001
    static final UUID OTHER   = UUID.fromString("00000000-0000-0000-0000-000000000999"); // ACC-999

    // Baseline hợp lệ: PENDING, non-recurring — đồng bộ FX-001
    static Reminder makePendingReminder() {
        Reminder r = new Reminder();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        r.setOwnerUserId(OWNER);
        r.setReminderType(ReminderType.MEDICATION);
        r.setTitle("Uong thuoc huyet ap");
        r.setScheduledAt(Instant.parse("2026-07-03T01:00:00Z"));
        r.setRecurrenceType(RecurrenceType.NONE);
        r.setStatus(ReminderStatus.PENDING);
        r.setFcmJobId("job-1");
        return r;
    }

    // Override specific fields
    static Reminder makeReminder(Consumer<Reminder> overrides) {
        Reminder r = makePendingReminder();
        overrides.accept(r);
        return r;
    }

    // Recurring DAILY — đồng bộ FX-010
    static Reminder makeDailyRecurring() {
        return makeReminder(r -> {
            r.setId(UUID.fromString("00000000-0000-0000-0000-000000000110"));
            r.setRecurrenceType(RecurrenceType.DAILY);
            r.setRecurrenceEndDate(null);
        });
    }
}
```

---

### REM213-TC-001 — Owner completes non-recurring PENDING reminder → 200 COMPLETED

**Severity:** `CRITICAL`
**Legal:** `BR-RBAC (SRS §3.3.16.2)`
**Feature Under Test:** `ReminderServiceImpl.completeReminder(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/reminder/CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.3.16.2 ("Marks a reminder as completed")` + `ADR-REM-213-001 §Decision (PENDING→COMPLETED)`

**Preconditions:** FX-001 (REM-001 PENDING, owner ACC-001, recurrenceType NONE).

**Test Steps:**
1. Arrange: `when(repo.findById(REM-001)).thenReturn(Optional.of(makePendingReminder()))`.
2. Act: `completeReminder(REM-001, ACC-001)`.
3. Assert: response + repo.save.

**Expected Result (PASS):** `response.status == "COMPLETED"`; `repo.save()` gọi với `status=COMPLETED`; `nextReminderId == null`.
**Expected Result (FAIL):** status vẫn PENDING, hoặc tạo row mới cho non-recurring.

**Current Status:** 🔴 Not written

---

### REM213-TC-002 — Owner completes recurring DAILY reminder → tạo lần kế tiếp *(phụ thuộc Open: ADR-REM-213-002)*

**Severity:** `HIGH`
**Feature Under Test:** `ReminderServiceImpl.completeReminder()` → `advanceRecurrence()`
**Test File:** `CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED — **GATED** (chỉ bật sau Tech Lead sign-off `ADR-REM-213-002` Phương án (a))
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-REM-213-002 §Decision (Proposed): next = scheduledAt + 1 ngày cho DAILY; INSERT row PENDING mới`

**Preconditions:** FX-010 (REM-050 PENDING, DAILY, no end date, scheduledAt=2026-07-03T01:00:00Z).

**Test Steps:**
1. `when(repo.findById(REM-050)).thenReturn(Optional.of(makeDailyRecurring()))`.
2. `when(repo.save(any())).thenAnswer(returnsFirstArg())`.
3. Act: `completeReminder(REM-050, ACC-001)`.

**Expected Result (PASS):** REM-050 → `COMPLETED`; `repo.save()` gọi 2 lần (update + insert); row mới `status=PENDING`, `scheduledAt=2026-07-04T01:00:00Z`; `response.nextScheduledAt=2026-07-04T01:00:00Z`.
**Expected Result (FAIL):** không tạo row mới, hoặc scheduledAt sai.

**Implementation Note:** Nếu `ADR-REM-213-002` **chưa** sign-off → test này giữ `@Disabled("Blocked by ADR-REM-213-002 Open")`, không đếm vào exit gate. Không parse `recurrence_rule`.
**Current Status:** 🔴 Not written (GATED)

---

### REM213-TC-003 — Recurring, lần kế tiếp vượt `recurrence_end_date` → KHÔNG tạo row mới *(Open)*

**Severity:** `MEDIUM`
**Feature Under Test:** `advanceRecurrence()` boundary
**TDD Phase:** 🔴 RED — **GATED** (ADR-REM-213-002)
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REM-213-002 §Decision (if next > recurrence_end_date → không tạo)`

**Preconditions:** FX-011 (REM-051, DAILY, recurrenceEndDate = scheduledAt, nên next = scheduledAt+1day > end).

**Expected Result (PASS):** REM-051 → `COMPLETED`; `repo.save()` gọi đúng **1 lần** (chỉ update); `response.nextReminderId == null`.
**Expected Result (FAIL):** vẫn tạo row mới vượt end date.
**Current Status:** 🔴 Not written (GATED)

---

### REM213-TC-004 — Complete reminder đã COMPLETED → 409 REM-007

**Severity:** `CRITICAL`
**Feature Under Test:** `assertNotTerminal()`
**Test File:** `CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REM-213-001 §Decision (terminal reject)` + `CB-REM-IMP-213 §10 (REM-007/409)`

**Preconditions:** FX-002 (REM-002 status=COMPLETED, owner ACC-001).

**Test Steps:** `findById → COMPLETED`; act `completeReminder(REM-002, ACC-001)`.

**Expected Result (PASS):** throws `BusinessException` với `code == "REM-007"`, HTTP 409; `repo.save()` **không** được gọi.
**Expected Result (FAIL):** set lại COMPLETED / trả 200 (idempotent-sai) / mã lỗi khác.
**Current Status:** 🔴 Not written

---

### REM213-TC-005 — Complete reminder đã SKIPPED → 409 REM-007

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REM-213-001` + `§10 REM-007`. Preconditions: FX-003 (SKIPPED).
**Expected Result (PASS):** `BusinessException code REM-007` (409); no save.
**Current Status:** 🔴 Not written

---

### REM213-TC-006 — Complete reminder đã CANCELLED → 409 REM-007

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REM-213-001` + `§10 REM-007`. Preconditions: FX-004 (CANCELLED).
**Expected Result (PASS):** `BusinessException code REM-007` (409); no save.
**Implementation Note:** CANCELLED cũng là terminal → không complete được (khác với UC-212 nơi CANCELLED vẫn *viewable*).
**Current Status:** 🔴 Not written

---

### REM213-TC-007 — Non-owner completes reminder → 403 REM-009

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC, ADR-REM-002 (owner-only)`
**Feature Under Test:** ownership check trong `completeReminder()`
**Test File:** `CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-REM-002 §Decision` + `CB-REM-IMP-213 §10 (REM-009/403)`

**Preconditions:** FX-005 (REM-005 ownerUserId=ACC-999); caller = ACC-001.

**Test Steps (Attack Simulation):** `findById(REM-005) → ownerUserId=ACC-999`; act `completeReminder(REM-005, ACC-001)`.

**Expected Result (PASS = an toàn):** throws `BusinessException code REM-009` (403); `repo.save()` không gọi; status không đổi.
**Expected Result (FAIL = lỗ hổng):** reminder của ACC-999 bị ACC-001 complete.
**Current Status:** 🔴 Not written

---

### REM213-TC-008 — Complete non-existent reminder → 404 REM-008

**Severity:** `MEDIUM`
**Feature Under Test:** repo lookup path
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-REM-IMP-213 §10 (REM-008/404)` + pattern getReminderDetail (UC-212 findById→404)

**Preconditions:** `findById(UNKNOWN) → Optional.empty()`.
**Expected Result (PASS):** throws `BusinessException code REM-008` (404).
**Expected Result (FAIL):** NullPointerException / 500 / leak existence.
**Current Status:** 🔴 Not written

---

### REM213-TC-009 — Complete emits `REMINDER_COMPLETED` audit + cancels pending FCM push

**Severity:** `HIGH`
**Feature Under Test:** side-effects: `auditService.log(...)`, `notificationService.cancelFcmPush(...)`
**Test File:** `CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-REM-IMP-213 §7 Domain Event Catalog` + `§17 C3/C4`

**Preconditions:** FX-001 (fcmJobId="job-1").

**Test Steps:** act `completeReminder(REM-001, ACC-001)`; verify mocks.

**Expected Result (PASS):**
- `verify(auditService).log(eq(AuditAction.REMINDER_COMPLETED), eq(ACC-001), eq("Reminder"), eq(REM-001.toString()), anyString())` — gọi 1 lần.
- `verify(notificationService).cancelFcmPush("job-1")` — gọi 1 lần.
**Expected Result (FAIL):** thiếu audit, hoặc log sai action (`REMINDER_CREATED`), hoặc không huỷ FCM.
**Implementation Note:** Yêu cầu thêm enum `AuditAction.REMINDER_COMPLETED` (L7) và method `INotificationService.cancelFcmPush` (L8).
**Current Status:** 🔴 Not written

---

### REM213-TC-010 — Response không chứa medication advice và không có recipient field

**Severity:** `HIGH`
**Legal:** `BR-SAFETY-002` + `§9 recipient Open note`
**Feature Under Test:** `CompleteReminderResponse` serialization
**Test File:** `CompleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-SAFETY-002 (UC-212 §17 C2)` + `CB-REM-IMP-213 §9 (no recipient column)`

**Test Steps:**
```java
CompleteReminderResponse resp = service.completeReminder(REM-001, ACC-001);
String json = objectMapper.writeValueAsString(resp);
```
**Expected Result (PASS):**
```java
assertThat(json).doesNotContain("dosage");
assertThat(json).doesNotContain("prescription");
assertThat(json).doesNotContain("diagnos");
assertThat(json).doesNotContain("recipient");
assertThat(json).doesNotContain("Người nhận");
```
**Expected Result (FAIL):** response chứa dosage/recipient.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### REM213-TC-INT-001 — Full complete flow với Testcontainers

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: seed PENDING → completeReminder → DB status=COMPLETED`
**Test File:** `src/test/java/com/carebridge/backend/reminder/CompleteReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `V1__init_schema.sql (reminders.status)` + `ADR-REM-213-001`

**Preconditions:** PostgreSQL container (`@Testcontainers`), Flyway applied; seed REM-001 (PENDING, ownerUserId=ACC-001).

**Test Steps:** seed → `completeReminder(REM-001, ACC-001)` → assert DB.

**Expected Result (PASS):**
```java
Reminder rec = reminderRepository.findById(REM_001).orElseThrow();
assertThat(rec.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
```
**Expected Result (FAIL):** status không đổi trong DB.
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### REM213-TC-SEC-001 — No JWT → 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `PATCH /api/v1/reminders/{id}/complete` (Spring Security filter)
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-REM-IMP-213 §16 (GUEST → 401)`
**Test Steps:** gọi endpoint không kèm Authorization header.
**Expected Result (PASS = an toàn):** `401 Unauthorized`; service không được gọi.
**Current Status:** 🔴 Not written

---

### REM213-TC-SEC-002 — ROLE_EXPERT → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` trên controller
**Test File:** `ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-REM-IMP-213 §16 (EXPERT → 403)`
**Preconditions:** FX-JWT-expert.
**Test Steps:** gọi endpoint với JWT role EXPERT.
**Expected Result (PASS = an toàn):** `403 Forbidden`; `reminderService.completeReminder()` không được gọi.
**Expected Result (FAIL):** EXPERT complete được reminder.
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REM213-TC-001` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-002` | `CompleteReminderServiceTest.java` | `[ ]` (GATED) | `___` | @Disabled tới khi ADR-REM-213-002 sign-off |
| `REM213-TC-003` | `CompleteReminderServiceTest.java` | `[ ]` (GATED) | `___` | — |
| `REM213-TC-004` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-005` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-006` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-007` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-008` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-009` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-010` | `CompleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM213-TC-INT-001` | `CompleteReminderIntegrationTest.java` | `[ ]` | `___` | — |
| `REM213-TC-SEC-001` | `ReminderControllerTest.java` | `[ ]` | `___` | — |
| `REM213-TC-SEC-002` | `ReminderControllerTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ suite với stub throw. Mọi test PHẢI FAIL. Nếu PASS ngay → AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**
```java
@Service
public class ReminderServiceImpl implements IReminderService {
    // ... createReminder(), getReminderDetail() giữ nguyên ...
    @Override
    public CompleteReminderResponse completeReminder(UUID reminderId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```
> Method mới cần cho tests biên dịch (Red Gate = contract-exists nhưng chưa hành xử):
> - `AuditAction.REMINDER_COMPLETED` (enum value)
> - `INotificationService.cancelFcmPush(String)` — stub `throw new UnsupportedOperationException("Not implemented — Red Phase stub");`
> - `CompleteReminderResponse` DTO (fields theo §8.1)

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REM213-TC-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `REM213-TC-004` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-007` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-008` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-009` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-010` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-INT-001` | `throw ...` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM213-TC-SEC-002` | `@PreAuthorize` active | 🔴 FAIL (không tới stub) hoặc PASS-đúng-403 | ☐ FAIL ☐ PASS | Security-only test — verify chặn ở controller |

> **Lưu ý SEC-002:** đây là test authorization ở controller layer; có thể PASS ngay cả với stub vì `@PreAuthorize` chặn trước khi vào service. Đây KHÔNG phải AP-AI-002 (nó không test business logic của stub). Ghi rõ trong evidence.

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả business-logic tests FAIL? ☐ Yes → GATE-2 PASS (T2→T3)
- Log file: `.omc/logs/uc213-red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-REM-IMP-213` đã review (Status hiện tại: Draft — cần Approved trước khi code).
- [ ] Logic Issues (§2, L1–L8) confirm với Principal Architect.
- [ ] `ADR-REM-213-002` (recurrence) sign-off **nếu** implement path recurring (TC-002/003). Path non-recurring không bị chặn.
- [ ] Bảng `reminders` xác nhận tồn tại (V1 + V20260627100300) — không cần migration mới.
- [ ] Fixtures (§3 TDS-05) chuẩn bị.

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests (non-gated) xanh; TC-002/003 GREEN chỉ khi ADR-REM-213-002 sign-off, ngược lại `@Disabled`.
- [ ] `./mvnw verify` — integration tests (Testcontainers) xanh.
- [ ] Coverage ≥ 80% lines cho `ReminderServiceImpl.completeReminder`.
- [ ] Controller chỉ validation + mapping (không business logic).
- [ ] Không PII/secret plaintext trong logs.
- [ ] Complete trên terminal state luôn 409 REM-007; non-owner 403 REM-009; not-found 404 REM-008.

**Exit bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả business-logic tests FAIL với stub throw.
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` → no output (mọi type/method inject tồn tại: `CompleteReminderResponse`, `AuditAction.REMINDER_COMPLETED`, `cancelFcmPush`).
- [ ] Props Isolation: mọi entity tạo qua factory trong `@Test`.
- [ ] Oracle Source: mọi expected value có nguồn (SRS/ADR/BR/§).

### Suspension Criteria
- `ADR-REM-213-002` chưa sign-off → treo TC-002/003 (`@Disabled`), không treo phần còn lại.
- Phát hiện lỗi kiến trúc mới cần Principal Architect review.
- CI pipeline broken bởi thay đổi khác.

---

## 7. Rollback Plan

```bash
# UC-213 KHÔNG có migration mới → không rollback schema.
# Revert code:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/

# Nếu Phương án (a) đã tạo row lần kế tiếp lỗi (chỉ dev/staging):
# psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
#   -c "DELETE FROM reminders WHERE status='PENDING' AND created_at > '<deploy_ts>';"

# Gap vẫn OPEN → giữ nguyên entry trong tracker.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ (mỗi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định cơ chế recurrence chưa chốt là "đã chốt"; hoặc test recipient field | ☐ (TC-002/003 GATED; TC-010 assert no recipient) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic | ☐ (SEC tests chỉ verify RBAC/mapping) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `ReminderStatus.SNOOZED` / method ngoài §8 | ☐ (L4: SNOOZED không tồn tại) | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Template v2.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol. Status: Draft — chưa Approved.*
