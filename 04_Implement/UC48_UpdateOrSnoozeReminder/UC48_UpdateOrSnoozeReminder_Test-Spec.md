# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-48 Update or Snooze Reminder

**Document ID:** `CB-REMINDER-IMP-003-TEST`
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
- `04_Implement/UC48_UpdateOrSnoozeReminder/UC48_UpdateOrSnoozeReminder_TDS.md` (CB-REMINDER-IMP-003)
- `01_Requirements/SRS.md` §3.3.1.25

> **TDD Convention:** Viết test TRƯỚC khi implement. Thứ tự bắt buộc: RED → GREEN → refactor.
> Không dùng PII thật — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD Spec cho UC-48 Update or Snooze Reminder |

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
| **Feature / UC ID** | `UC-48` |
| **Module** | `reminder — UpdateOrSnoozeReminder` |
| **Spec gốc** | `CB-REMINDER-IMP-003` |
| **Priority** | 🟠 P1 — High |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-REMINDER-UPDATE-001, BR-REMINDER-SNOOZE-001, BR-REMINDER-COMPLETE-001, BR-REMINDER-SKIP-001` |
| **Upstream Dependencies** | `auth, reminders (UC-45/46/47), FCM` |
| **Downstream Consumers** | `UC-49 ViewTodayTasks, UC-212 ViewReminderDetail, audit` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REMINDER-IMP-003 §17`, `BR-REMINDER-UPDATE-001`, `ADR-REM-STATE-001` |
| **Constraints Injected** | `C1 (immutable COMPLETED/SKIPPED), C2 (ownership 404), C3 (FCM reschedule on snooze), C4 (JWT userId), C5 (type/owner immutable)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec không rõ SNOOZED reminder có thể update được không | BR-REMINDER-UPDATE-001: chỉ PENDING và SNOOZED có thể update | Test phải verify SNOOZED reminder → update OK; COMPLETED/SKIPPED → REMINDER-006 |
| L2 | Spec không rõ ownership fail trả 403 hay 404 | Policy (security best practice): trả 404 để không leak existence | Test expect 404, không phải 403, khi reminder không thuộc owner |
| L3 | Spec không đề cập FCM cancel khi snooze | ADR-REM-FCM-002: PHẢI cancel FCM cũ trước khi schedule mới | Test phải verify cancelFcmJob() được gọi trước scheduleFcmPush() |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UC-48 UpdateOrSnoozeReminder bao gồm:
├── Service (4 methods: updateReminder, snoozeReminder, completeReminder, skipReminder)
├── Controller (4 PATCH endpoints — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-48` | Update/snooze/complete/skip reminder |
| `BR-REMINDER-UPDATE-001` | Chỉ PENDING/SNOOZED mới có thể update |
| `BR-REMINDER-SNOOZE-001` | Snooze → SNOOZED + snoozed_until + FCM reschedule |
| `BR-REMINDER-COMPLETE-001` | Complete → COMPLETED (terminal) |
| `BR-REMINDER-SKIP-001` | Skip → SKIPPED (terminal) |
| `BR-RBAC` | Owner-only access |
| `ADR-REM-STATE-001` | Terminal state machine invariants |
| `CB-REMINDER-IMP-003 §10` | Error codes REMINDER-003, 006, 007 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Update PENDING reminder — happy path | `ReminderService.updateReminder()` | `UPD-TC-001` |
| TC-COND-002 | Snooze PENDING reminder — happy path | `ReminderService.snoozeReminder()` | `UPD-TC-002` |
| TC-COND-003 | Complete PENDING reminder | `ReminderService.completeReminder()` | `UPD-TC-003` |
| TC-COND-004 | Skip PENDING reminder | `ReminderService.skipReminder()` | `UPD-TC-004` |
| TC-COND-005 | Update COMPLETED reminder → 409 | `validateMutable()` | `UPD-TC-005` |
| TC-COND-006 | Update SKIPPED reminder → 409 | `validateMutable()` | `UPD-TC-006` |
| TC-COND-007 | Update SNOOZED reminder → success | `validateMutable()` — SNOOZED is mutable | `UPD-TC-007` |
| TC-COND-008 | Reminder không thuộc owner → 404 | `validateOwnership()` | `UPD-TC-008` |
| TC-COND-009 | snoozedUntil trong quá khứ → 400 | `validateSnoozedUntil()` | `UPD-TC-009` |
| TC-COND-010 | Unauthorized → 401 | Spring Security filter | `UPD-TC-010` |
| TC-COND-011 | Wrong role → 403 | `@PreAuthorize` | `UPD-TC-011` |
| TC-COND-012 | Full integration snooze flow | DB state verification | `UPD-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| State Transition Testing | `PENDING → SNOOZED/COMPLETED/SKIPPED` | Core state machine per ADR-REM-STATE-001 |
| Equivalence Partitioning | reminder status (PENDING, SNOOZED, COMPLETED, SKIPPED) | 4 status classes × 4 operations |
| Boundary Value Analysis | snoozedUntil (past, now, future) | Snooze time validation |
| Error Guessing | RBAC bypass, wrong owner | Security hardening |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-UPD-001` | DB seed | `User { userId: "user-001", role: ROLE_MOTHER }` | Owner |
| `FX-UPD-002` | DB seed | `Reminder { reminderId: "rem-001", ownerUserId: "user-001", status: PENDING }` | Mutable reminder |
| `FX-UPD-003` | DB seed | `Reminder { reminderId: "rem-002", ownerUserId: "user-001", status: COMPLETED }` | Immutable |
| `FX-UPD-004` | DB seed | `Reminder { reminderId: "rem-003", ownerUserId: "user-001", status: SKIPPED }` | Immutable |
| `FX-UPD-005` | DB seed | `Reminder { reminderId: "rem-004", ownerUserId: "user-001", status: SNOOZED, snoozedUntil: future }` | SNOOZED (mutable) |
| `FX-UPD-006` | DB seed | `Reminder { reminderId: "rem-999", ownerUserId: "user-999", status: PENDING }` | Other user's reminder |
| `FX-UPD-007` | JWT | `{ sub: "user-001", role: "ROLE_MOTHER" }` | Auth |
| `FX-UPD-008` | JWT | `{ sub: "user-002", role: "ROLE_EXPERT" }` | Wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ReminderTestFactory.java
class ReminderTestFactory {

    static Reminder makePendingReminder() {
        Reminder r = new Reminder();
        r.setReminderId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        r.setReminderType("VACCINATION");
        r.setTitle("Tiêm vắc-xin 5 trong 1");
        r.setScheduledAt(ZonedDateTime.now().plusDays(3));
        r.setStatus("PENDING");
        r.setSnoozedUntil(null);
        return r;
    }

    static Reminder makeReminder(Consumer<Reminder> overrides) {
        Reminder r = makePendingReminder();
        overrides.accept(r);
        return r;
    }

    static UpdateReminderRequest makeUpdateRequest() {
        UpdateReminderRequest req = new UpdateReminderRequest();
        req.setTitle("Tiêm vắc-xin 5 trong 1 (cập nhật)");
        req.setScheduledAt(ZonedDateTime.now().plusDays(4));
        return req;
    }

    static SnoozeReminderRequest makeSnoozeRequest() {
        SnoozeReminderRequest req = new SnoozeReminderRequest();
        req.setSnoozedUntil(ZonedDateTime.now().plusHours(2));
        return req;
    }
}
```

---

### UPD-TC-001 — Update PENDING reminder thành công

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.updateReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-REMINDER-UPDATE-001`, `CB-REMINDER-IMP-003 §9.2`

**Preconditions:**
- FX-UPD-002: Reminder "rem-001" status=PENDING, owned by "user-001"
- FX-UPD-007: JWT cho "user-001"

**Test Steps:**
1. Arrange: Mock `reminderRepository.findByReminderIdAndOwnerUserId("rem-001", "user-001")` → return `Optional.of(makePendingReminder())`; Mock save() → return updated reminder; Mock `notificationService.cancelFcmJob()` và `scheduleFcmPush()`
2. Act: `reminderService.updateReminder("rem-001", makeUpdateRequest(), "user-001")`
3. Assert: Response status còn PENDING; title và scheduledAt đã thay đổi; `reminderRepository.save()` called once; FCM rescheduled = true

**Expected Result (PASS):**
- HTTP 200; `status=PENDING`; `title` updated; `fcmRescheduled=true`
- `save()` called with new title/scheduledAt

**Expected Result (FAIL):**
- Response không có field mới, hoặc FCM không được reschedule

**Current Status:** 🔴 Not written

---

### UPD-TC-002 — Snooze PENDING reminder thành công

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.snoozeReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-REMINDER-SNOOZE-001`, `ADR-REM-FCM-002`

**Preconditions:**
- FX-UPD-002: Reminder "rem-001" status=PENDING

**Test Steps:**
1. Arrange: Mock repo → PENDING reminder; Mock notificationService
2. Act: `reminderService.snoozeReminder("rem-001", makeSnoozeRequest(), "user-001")`
3. Assert: `status == SNOOZED`; `snoozedUntil` set; `cancelFcmJob()` called 1 lần; `scheduleFcmPush()` called 1 lần với snoozedUntil; audit event `ReminderSnoozed` emitted

**Expected Result (PASS):**
- Response: `status=SNOOZED`, `snoozedUntil` not null, `fcmRescheduled=true`
- `cancelFcmJob()` được gọi TRƯỚC `scheduleFcmPush()`

**Current Status:** 🔴 Not written

---

### UPD-TC-003 — Complete reminder thành công

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.completeReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-REMINDER-COMPLETE-001`

**Test Steps:**
1. Arrange: Mock repo → PENDING reminder
2. Act: `reminderService.completeReminder("rem-001", "user-001")`
3. Assert: `status == COMPLETED`; FCM job canceled; audit `ReminderCompleted` emitted

**Expected Result (PASS):**
- Response: `status=COMPLETED`
- `cancelFcmJob()` called

**Current Status:** 🔴 Not written

---

### UPD-TC-004 — Skip reminder thành công

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.skipReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-REMINDER-SKIP-001`

**Test Steps:**
1. Arrange: Mock repo → PENDING reminder
2. Act: `reminderService.skipReminder("rem-001", "user-001")`
3. Assert: `status == SKIPPED`

**Expected Result (PASS):** `status=SKIPPED`

**Current Status:** 🔴 Not written

---

### UPD-TC-005 — Update COMPLETED reminder → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.validateMutable()` — COMPLETED immutable
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-REMINDER-COMPLETE-001`, `REMINDER-006`

**Test Steps:**
1. Arrange: Mock repo → COMPLETED reminder (`status=COMPLETED`)
2. Act: `reminderService.updateReminder("rem-002", makeUpdateRequest(), "user-001")`
3. Assert: `ReminderException` với code `REMINDER-006`; `save()` KHÔNG được gọi

**Expected Result (PASS):**
- Exception REMINDER-006 (HTTP 409)
- DB không bị thay đổi

**Expected Result (FAIL):**
- Service update COMPLETED reminder → vi phạm ADR-REM-STATE-001

**Current Status:** 🔴 Not written

---

### UPD-TC-006 — Snooze SKIPPED reminder → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.validateMutable()` — SKIPPED immutable
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-REMINDER-SKIP-001`, `REMINDER-006`

**Test Steps:**
1. Arrange: Mock repo → SKIPPED reminder
2. Act: `reminderService.snoozeReminder("rem-003", makeSnoozeRequest(), "user-001")`
3. Assert: `ReminderException` REMINDER-006

**Expected Result (PASS):** Exception REMINDER-006 (HTTP 409)

**Current Status:** 🔴 Not written

---

### UPD-TC-007 — Update SNOOZED reminder → success

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.updateReminder()` trên SNOOZED reminder
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-REMINDER-UPDATE-001` — SNOOZED là mutable

**Test Steps:**
1. Arrange: Mock repo → SNOOZED reminder
2. Act: `reminderService.updateReminder("rem-004", makeUpdateRequest(), "user-001")`
3. Assert: Response 200; status vẫn là SNOOZED; title đã thay đổi

**Expected Result (PASS):**
- Response: `status=SNOOZED` (không thay đổi status), `title` updated

**Current Status:** 🔴 Not written

---

### UPD-TC-008 — Reminder không thuộc owner → 404

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ReminderService.validateOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/UpdateReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`, `REMINDER-003`

**Test Steps:**
1. Arrange: Mock `findByReminderIdAndOwnerUserId("rem-999", "user-001")` → `Optional.empty()`
2. Act: `reminderService.updateReminder("rem-999", makeUpdateRequest(), "user-001")`
3. Assert: Exception REMINDER-003 (HTTP 404)

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 404 — không leak thông tin existence
- `save()` KHÔNG được gọi

**Expected Result (FAIL = lỗ hổng):**
- HTTP 403 (leak existence) hoặc 200 (update thành công → RBAC bypass)

**Current Status:** 🔴 Not written

---

### UPD-TC-009 — snoozedUntil trong quá khứ → 400

**Severity:** `HIGH`
**Feature Under Test:** `SnoozeReminderRequest` validation
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `REMINDER-007`

**Test Steps:**
1. Act: `mockMvc.perform(PATCH /api/v1/reminders/rem-001/snooze)` với `snoozedUntil = now - 1h`
2. Assert: HTTP 400; error code `REMINDER-007`

**Expected Result (PASS):** HTTP 400, `REMINDER-007`

**Current Status:** 🔴 Not written

---

### UPD-TC-010 — Unauthorized → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `Spring Security filter`
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. Act: PATCH /api/v1/reminders/{id}/snooze — không có Authorization header
2. Assert: HTTP 401

**Current Status:** 🔴 Not written

---

### UPD-TC-011 — Wrong role (ROLE_EXPERT) → 403

**Severity:** `HIGH`
**Feature Under Test:** `@PreAuthorize` on PATCH endpoints
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-RBAC`, `REMINDER-004`

**Test Steps:**
1. Arrange: FX-UPD-008 — JWT với ROLE_EXPERT
2. Act: PATCH /api/v1/reminders/{id}/complete
3. Assert: HTTP 403; error code REMINDER-004

**Current Status:** 🔴 Not written

---

### UPD-TC-INT-001 — Full integration: Snooze flow với DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH /api/v1/reminders/{id}/snooze → DB`
**Test File:** `src/test/java/com/carebridge/backend/reminder/UpdateReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL Testcontainer + Flyway migration (V1__init_schema.sql)
- Seed: FX-UPD-001, FX-UPD-002

**Test Steps:**
1. Seed user-001 và reminder "rem-001" (PENDING)
2. Authenticate → JWT
3. `PATCH /api/v1/reminders/rem-001/snooze` với snoozedUntil = now+2h
4. Assert response 200, status=SNOOZED
5. Query DB: SELECT từ reminders WHERE reminder_id = 'rem-001'

**Expected Result (PASS):**
- HTTP 200
- DB: `status='SNOOZED'`, `snoozed_until IS NOT NULL`, `updated_at > created_at`

**DB Assertion:**
```java
Reminder updated = reminderRepository
    .findByReminderIdAndOwnerUserId(reminderId, userId).orElseThrow();
assertThat(updated.getStatus()).isEqualTo("SNOOZED");
assertThat(updated.getSnoozedUntil()).isNotNull();
assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UPD-TC-001` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-002` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-003` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-004` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-005` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-006` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-007` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-008` | `UpdateReminderServiceTest.java` | `[ ]` | — | — |
| `UPD-TC-009` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `UPD-TC-010` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `UPD-TC-011` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `UPD-TC-INT-001` | `UpdateReminderIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**
```java
@Service
public class ReminderService implements IReminderService {

    @Override
    public UpdateReminderResponse updateReminder(
            UUID reminderId, UpdateReminderRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public UpdateReminderResponse snoozeReminder(
            UUID reminderId, SnoozeReminderRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public UpdateReminderResponse completeReminder(UUID reminderId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public UpdateReminderResponse skipReminder(UUID reminderId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|--------|----------------------------------|
| `UPD-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `UPD-TC-005` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `UPD-TC-008` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `UPD-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-REMINDER-IMP-003` đã review và approve
- [ ] Logic Issues (Section 2) đã confirm
- [ ] UC-47 `createVaccinationReminder` đã implement (dependency: reminder phải tồn tại trước khi update)
- [ ] Test fixtures (Section 3 TDS-05) đã chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=UpdateReminderServiceTest` — tất cả xanh
- [ ] `./mvnw verify -Dtest=UpdateReminderIntegrationTest` — xanh
- [ ] Test coverage ≥ 80% lines cho 4 public methods
- [ ] `validateMutable()` được test với tất cả 4 trạng thái: PENDING (mutable), SNOOZED (mutable), COMPLETED (immutable), SKIPPED (immutable)
- [ ] Ownership check trả về 404, không phải 403

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả tests FAIL với throw stub
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] Oracle Source: tất cả assert có ghi nguồn BR/ADR

### Suspension Criteria

- UC-47 createReminder chưa stable
- FCM credentials chưa configured trong test environment

---

## 7. Rollback Plan

```bash
# Không có migration mới → chỉ rollback code
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/test/java/com/carebridge/backend/reminder/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong spec | Check | Gate |
|-------|-------------|---------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-REMINDER-UPDATE-001 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC-005 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code allow COMPLETED → PENDING transition | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller gọi reminderRepository trực tiếp | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Import FCMJobScheduler mà không có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
