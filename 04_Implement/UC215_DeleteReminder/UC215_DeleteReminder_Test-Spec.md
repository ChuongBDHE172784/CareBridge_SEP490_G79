# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-215 Delete Reminder

**Document ID:** `CB-REM-TDD-005`
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
- TDS: `04_Implement/UC215_DeleteReminder/UC215_DeleteReminder_TDS.md` (CB-REM-IMP-005)
- SRS: §3.3.16.4 (Table 237 — Delete Reminder)
- Schema oracle: `05_Development/CareBridgeAPI/src/main/resources/db/migration/` (reminders baseline + `V20260627100300__add_reminder_columns.sql`) — no new migration for UC-215
- ADRs: ADR-REM-002 (owner-only, reuse), ADR-REM-STATE-001 (terminal), ADR-REM-DELETE-001/002/003

> **Quy ước TDD:** test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo TDD spec cho UC-215 Delete Reminder |

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
| **Feature / Gap ID** | `UC-215` |
| **Module** | `DeleteReminder — reminder (Bounded Context)` |
| **Spec gốc** | `CB-REM-IMP-005` |
| **Priority** | 🟠 P1 (SRS Priority = Medium, Frequency = Occasional) |
| **Sprint** | `S[N]` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `auth (JWT/SecurityUtils), reminders table` |
| **Downstream Consumers** | `notification (FCM cancel via ReminderCancelled), audit` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REM-IMP-005 §17`, ADR-REM-DELETE-001/002/003, ADR-REM-002, ADR-REM-STATE-001 |
| **Constraints Injected** | C1 soft-delete only (no hard delete); C2 owner-only + JWT identity; C3 idempotent no-op; C4 terminal guard 409; C5 publish ReminderCancelled + audit |
| **Model** | `Claude (Opus) — CareBridge spec pipeline` |
| **Trust Level** | `T2 → T3 (pending Red Gate §5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `reminders` baseline + approved migrations are the final persistence oracle; ERD is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: "Deletes or disables a Mother-created reminder" — không nói xóa cứng hay mềm | BR-PRIVACY + Assumption retention/audit; enum thực tế đã có `CANCELLED`; cột `status` là VARCHAR | Test khẳng định **soft-delete** (status→CANCELLED, row còn tồn tại), KHÔNG hard delete |
| L2 | UC-212 prose gọi field là `accountId` (bug) | Entity thực tế dùng `ownerUserId` / `owner_user_id` | Test dùng `ownerUserId`; ownership check `reminder.getOwnerUserId().equals(callerId)` |
| L3 | SRS không nói hành vi khi delete lại reminder đã xóa | ADR-REM-DELETE-002: idempotent no-op | Test re-delete CANCELLED → 204, không save/publish/audit lại |
| L4 | SRS không nói delete reminder COMPLETED/SKIPPED | ADR-REM-STATE-001: terminal states không transition | Test COMPLETED/SKIPPED → 409 REM-017 |
| L5 | `AuditAction` enum thực tế chưa có value cho cancel | Chỉ có `REMINDER_CREATED` | Test yêu cầu audit `REMINDER_CANCELLED` (value phải được thêm — code-only, §TDS §11.3 Chặng 1) |
| L6 | Error prefix có thể nhầm `REMINDER-` | Mã thực tế dùng `REM-` (REM-001/004/006) | Dùng `REM-015..018` |

---

## 3. Test Design Specification (TDS)

> Bao gồm `reminders` baseline schema trong test basis vì persistence side-effect (status VARCHAR, row-still-exists) là một phần oracle.

### TDS-01 — Scope / Phạm vi
```
DeleteReminder gồm các layer:
├── Application/Service (mock ReminderRepository, INotificationService, AuditService, ApplicationEventPublisher với Mockito)
├── Controller (mock IReminderService với @WebMvcTest — 204/401/403/404/409)
└── Integration (Testcontainers PostgreSQL + @SpringBootTest — soft-delete persistence)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-215` (Table 237) | Mother deletes/disables own reminder; Occasional; BR-RBAC + BR-PRIVACY |
| `ADR-REM-002` | Owner-only delete → 403 non-owner |
| `ADR-REM-STATE-001` | COMPLETED/SKIPPED terminal → 409 |
| `ADR-REM-DELETE-001` | Soft-delete (status=CANCELLED), no hard delete |
| `ADR-REM-DELETE-002` | Idempotent re-delete → 204 no-op |
| `ADR-REM-DELETE-003` | Publish `ReminderCancelled` (fcmJobId) |
| `BR-PRIVACY / PDPA` | Retention (row preserved) + audit `REMINDER_CANCELLED` |
| `CB-REM-IMP-005 §8, §10` | Interface contract + error codes REM-015..018 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner deletes PENDING → soft-delete CANCELLED | `ReminderServiceImpl.deleteReminder()` | `REM215-TC-001` |
| TC-COND-002 | Re-delete CANCELLED → idempotent no-op | idempotent guard | `REM215-TC-002` |
| TC-COND-003 | Non-owner → 403 REM-016 (IDOR) | ownership guard | `REM215-TC-003` |
| TC-COND-004 | Not found → 404 REM-015 | repo lookup | `REM215-TC-004` |
| TC-COND-005 | COMPLETED → 409 REM-017 | terminal guard | `REM215-TC-005` |
| TC-COND-006 | SKIPPED → 409 REM-017 | terminal guard | `REM215-TC-006` |
| TC-COND-007 | Publish ReminderCancelled + audit | event + audit side-effects | `REM215-TC-007` |
| TC-COND-008 | Never hard-delete (deleteById not called; row persists) | soft-delete invariant | `REM215-TC-008`, `REM215-TC-INT-001` |
| TC-COND-009 | Controller returns 204; 401 without JWT | `ReminderController.deleteReminder()` | `REM215-TC-E2E-001`, `REM215-TC-E2E-002` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | PENDING→CANCELLED; terminal states rejected | Enum ReminderStatus có invariant terminal |
| Equivalence Partitioning | status ∈ {PENDING} vs {CANCELLED} vs {COMPLETED,SKIPPED} | Ba lớp hành vi khác nhau (delete / no-op / conflict) |
| Error Guessing | IDOR (non-owner), missing JWT, unknown id | Security & robustness |
| Boundary/Idempotency | delete rồi delete lại | ADR-REM-DELETE-002 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB/mock | `{id: REM-001, ownerUserId: USER-001, status: PENDING, fcmJobId: "job-abc"}` | Happy path soft-delete |
| `FX-002` | DB/mock | `{id: REM-002, ownerUserId: USER-001, status: CANCELLED}` | Idempotent no-op |
| `FX-003` | DB/mock | `{id: REM-003, ownerUserId: USER-001, status: COMPLETED}` | Terminal conflict |
| `FX-004` | DB/mock | `{id: REM-004, ownerUserId: USER-001, status: SKIPPED}` | Terminal conflict |
| `FX-005` | JWT | `{sub: USER-001, role: MOTHER}` owner | Auth context (owner) |
| `FX-006` | JWT | `{sub: USER-999, role: MOTHER}` non-owner | IDOR test |

---

## 4. Test Case Specification

> **TC ID format:** `REM215-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// DeleteReminderTestFactory.java — mỗi @Test tạo fresh instance
class DeleteReminderTestFactory {

    static final UUID OWNER   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID NONOWNER= UUID.fromString("00000000-0000-0000-0000-000000000999");

    // Baseline PENDING reminder (đồng bộ FX-001)
    static Reminder makePending() {
        Reminder r = new Reminder();
        r.setId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        r.setOwnerUserId(OWNER);
        r.setReminderType(ReminderType.APPOINTMENT);
        r.setTitle("OB-GYN Checkup");
        r.setScheduledAt(Instant.now().plusSeconds(3600));
        r.setRecurrenceType(RecurrenceType.NONE);
        r.setStatus(ReminderStatus.PENDING);
        r.setFcmJobId("job-abc");
        return r;
    }

    // Override status (đảm bảo mỗi test có instance riêng)
    static Reminder makeWithStatus(ReminderStatus status) {
        Reminder r = makePending();
        r.setStatus(status);
        return r;
    }
}
```

---

### REM215-TC-001 — Owner deletes PENDING → soft-delete CANCELLED

**Severity:** `CRITICAL`
**Legal:** `BR-PRIVACY / PDPA (retention)`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/reminder/DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-REM-DELETE-001 §Decision` (soft-delete = status CANCELLED); `CB-REM-IMP-005 §11.3 Chặng 3`

**Preconditions:** FX-001 (PENDING, owner=USER-001); FX-005 (owner JWT). Mock `reminderRepository.findById(REM-001)` → FX-001.

**Test Steps:**
1. Arrange: mock repo trả FX-001; capture `save()` argument.
2. Act: `deleteReminder(REM-001, USER-001)`.
3. Assert trạng thái + side effects.

**Expected Result (PASS):**
- Không throw.
- `save()` gọi đúng 1 lần với `reminder.status == CANCELLED`.
- `reminderRepository.deleteById(...)` **không** được gọi (verify never).

**Expected Result (FAIL — dấu hiệu lỗi):**
- Status không đổi thành CANCELLED, hoặc gọi hard delete.

**Current Status:** 🔴 Not written
**Implementation Note:** `reminder.setStatus(CANCELLED); reminderRepository.save(reminder);`

---

### REM215-TC-002 — Idempotent re-delete CANCELLED → no-op

**Severity:** `HIGH`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()`
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-REM-DELETE-002 §Decision`

**Preconditions:** FX-002 (status=CANCELLED, owner=USER-001).

**Test Steps:**
1. Mock `findById(REM-002)` → FX-002.
2. Act: `deleteReminder(REM-002, USER-001)`.

**Expected Result (PASS):**
- Không throw.
- `save()` **không** được gọi; `eventPublisher.publishEvent(...)` **không** gọi; `auditService.log(...)` **không** gọi.

**Expected Result (FAIL):**
- Ném lỗi, hoặc save/publish/audit lại (tạo trùng).

**Current Status:** 🔴 Not written

---

### REM215-TC-003 — Non-owner → 403 REM-016 (IDOR)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC / BR-PRIVACY`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()` ownership guard
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REM-002 §Decision`; `CB-REM-IMP-005 §10` (REM-016/403)

**Preconditions:** FX-001 (owner=USER-001); caller = USER-999 (FX-006).

**Test Steps (Attack Simulation):**
1. Mock `findById(REM-001)` → FX-001 (owner USER-001).
2. Act: `deleteReminder(REM-001, USER-999)`.

**Expected Result (PASS = an toàn):**
- Throw `BusinessException` với `code == "REM-016"`, HTTP 403.
- `save()` **không** gọi; status vẫn PENDING.

**Expected Result (FAIL = lỗ hổng):**
- Reminder của người khác bị cancel (IDOR thành công).

**Current Status:** 🔴 Not written

---

### REM215-TC-004 — Not found → 404 REM-015

**Severity:** `MEDIUM`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()`
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-REM-IMP-005 §10` (REM-015/404)

**Preconditions:** `findById(UNKNOWN)` → `Optional.empty()`.

**Expected Result (PASS):** Throw `BusinessException` code `REM-015` (404).
**Expected Result (FAIL):** NPE / 500 / trả 204.

**Current Status:** 🔴 Not written

---

### REM215-TC-005 — COMPLETED → 409 REM-017 (terminal)

**Severity:** `HIGH`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()` terminal guard
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-REM-STATE-001 §Decision`; `CB-REM-IMP-005 §10` (REM-017/409)

**Preconditions:** FX-003 (status=COMPLETED, owner=USER-001).

**Expected Result (PASS):** Throw `BusinessException` code `REM-017` (409); status vẫn COMPLETED; `save()` không gọi.
**Expected Result (FAIL):** COMPLETED bị chuyển thành CANCELLED (mất completion history).

**Current Status:** 🔴 Not written

---

### REM215-TC-006 — SKIPPED → 409 REM-017 (terminal)

**Severity:** `MEDIUM`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()` terminal guard
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-REM-STATE-001 §Decision`

**Preconditions:** FX-004 (status=SKIPPED, owner=USER-001).

**Expected Result (PASS):** Throw `BusinessException` code `REM-017` (409); status vẫn SKIPPED.
**Expected Result (FAIL):** SKIPPED bị cancel.

**Current Status:** 🔴 Not written

---

### REM215-TC-007 — Publishes ReminderCancelled (fcmJobId) + audit REMINDER_CANCELLED

**Severity:** `HIGH`
**Legal:** `BR-PRIVACY (POST-3 audit)`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()` side effects
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-REM-DELETE-003 §Decision`; `CB-REM-IMP-005 §7.3` (payload); §17 C5

**Preconditions:** FX-001 (PENDING, fcmJobId="job-abc").

**Test Steps:**
1. Mock repo; capture `eventPublisher.publishEvent(captor)` và `auditService.log(...)`.
2. Act: `deleteReminder(REM-001, USER-001)`.

**Expected Result (PASS):**
- Published event là `ReminderCancelled` với `reminderId==REM-001`, `ownerUserId==USER-001`, `fcmJobId=="job-abc"`.
- `auditService.log(AuditAction.REMINDER_CANCELLED, USER-001, "Reminder", REM-001, "cancelled")` gọi đúng 1 lần.

**Expected Result (FAIL):**
- Không publish event, hoặc audit sai action, hoặc mất `fcmJobId`.

**Current Status:** 🔴 Not written
**Implementation Note:** Cần thêm value `REMINDER_CANCELLED` vào `AuditAction` (code-only) trước khi test này pass.

---

### REM215-TC-008 — Invariant: NEVER hard-delete (deleteById never called)

**Severity:** `CRITICAL`
**CWE:** `CWE-212 — Improper Removal of Sensitive Information (retention violation nếu hard-delete)`
**Legal:** `BR-PRIVACY / PDPA (retention)`
**Feature Under Test:** `ReminderServiceImpl.deleteReminder()` soft-delete invariant
**Test File:** `DeleteReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-REM-DELETE-001 §Consequences (Invariant I1)`

**Preconditions:** FX-001 (PENDING).

**Test Steps:**
1. Spy/verify trên `reminderRepository`.
2. Act: `deleteReminder(REM-001, USER-001)`.

**Expected Result (PASS):**
- `verify(reminderRepository, never()).deleteById(any())`
- `verify(reminderRepository, never()).delete(any())`
- Chỉ `save()` được gọi.

**Expected Result (FAIL):**
- Bất kỳ lời gọi delete vật lý nào.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### REM215-TC-INT-001 — Soft-delete persists (row còn tồn tại, status CANCELLED)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: deleteReminder → UPDATE reminders`
**Test File:** `src/test/java/com/carebridge/backend/reminder/DeleteReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `reminders` schema (status VARCHAR, row-preserving UPDATE); `ADR-REM-DELETE-001`

**Preconditions:**
- PostgreSQL container (`@Testcontainers`), Flyway applied.
- Seed: 1 reminder PENDING owner=USER-001 qua `reminderRepository.save(...)`.

**Test Steps:**
1. Seed PENDING reminder, ghi nhận id.
2. Call `deleteReminder(id, USER-001)`.
3. Assert DB state.

**Expected Result (PASS):**
- `reminderRepository.findById(id)` vẫn trả về row (không empty).
- `row.status == CANCELLED`.
- `row.updatedAt` >= `row.createdAt`.

**Expected Result (FAIL):**
- `findById` trả empty (row bị hard-delete) → vi phạm retention.

**DB Assertion:**
```java
Reminder r = reminderRepository.findById(savedId).orElseThrow();
assertThat(r.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
```

**Current Status:** 🔴 Not written

---

### E2E / CONTROLLER TEST CASES

---

### REM215-TC-E2E-001 — DELETE (owner) → 204 No Content

**Severity:** `HIGH`
**Feature Under Test:** `ReminderController.deleteReminder()` + `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/reminder/ReminderControllerDeleteTest.java` (`@WebMvcTest`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-REM-IMP-005 §9.1` (204 No Content)

**Preconditions:** Mock `reminderService.deleteReminder(...)` (no-op). JWT owner MOTHER.

**Expected Result (PASS):** HTTP `204`, body rỗng.
**Expected Result (FAIL):** 200 với body, hoặc 500.

**Current Status:** 🔴 Not written

---

### REM215-TC-E2E-002 — DELETE without JWT → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** Security filter chain trên `DELETE /api/v1/reminders/{id}`
**Test File:** `ReminderControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** JWT auth baseline (unauthenticated → 401)

**Expected Result (PASS = an toàn):** HTTP `401`; `reminderService.deleteReminder` **không** được gọi.
**Expected Result (FAIL):** Cho phép xóa mà không auth.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REM215-TC-001` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-002` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-003` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-004` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-005` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-006` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-007` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-008` | `DeleteReminderServiceTest.java` | `[ ]` | `___` | — |
| `REM215-TC-INT-001` | `DeleteReminderIntegrationTest.java` | `[ ]` | `___` | — |
| `REM215-TC-E2E-001` | `ReminderControllerDeleteTest.java` | `[ ]` | `___` | — |
| `REM215-TC-E2E-002` | `ReminderControllerDeleteTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước implement, chạy toàn bộ suite với stub throw. Mọi test PHẢI FAIL. Nếu PASS ngay → AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**

```java
@Service
public class ReminderServiceImpl implements IReminderService {
    // ... createReminder, getReminderDetail đã tồn tại ...

    @Override
    public void deleteReminder(UUID reminderId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REM215-TC-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `REM215-TC-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-004` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-005` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-007` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-008` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-INT-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-E2E-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `REM215-TC-E2E-002` | N/A (401 trước khi vào service) | 🔴 FAIL nếu endpoint chưa map | ☐ FAIL ☐ PASS | Endpoint chưa tồn tại → 404/401 khác kỳ vọng |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `.omc/logs/uc215-red-gate-evidence.log`

> **Nếu bất kỳ test PASS:** Dừng. Xác định root cause. Rewrite test với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-REM-IMP-005` reviewed & approved; ADR-REM-DELETE-001/002/003 chuyển `Accepted`.
- [ ] Logic Issues (§2) confirmed với Tech Lead.
- [ ] Không cần Flyway migration (xác nhận `CANCELLED` đã tồn tại, `status` là VARCHAR).
- [ ] Test fixtures (§3 TDS-05) chuẩn bị.

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh.
- [ ] `./mvnw verify` — integration test (Testcontainers) xanh.
- [ ] Coverage ≥ 80% lines cho `deleteReminder()`.
- [ ] Không business logic trong Controller (chỉ resolve callerId + gọi service + 204).
- [ ] Không PII/secret plaintext trong logs.
- [ ] **Soft-delete invariant**: không có lời gọi `deleteById()`/`delete()` (REM215-TC-008 xanh).
- [ ] Audit `REMINDER_CANCELLED` sinh ra đúng.

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — mọi test FAIL với stub throw trước implement.
- [ ] **Contract Existence** — mọi class inject tồn tại; `./mvnw compile` không error. Lưu ý: `AuditAction.REMINDER_CANCELLED` và `INotificationService.cancelFcmPush` phải được thêm trước (nếu thiếu → compile error = Hallucinated Contract).
- [ ] **Props Isolation** — không shared mutable state (dùng `DeleteReminderTestFactory`).
- [ ] **Oracle Source** — mọi expected value có nguồn (BR/ADR).

### Suspension Criteria
- ADR-REM-DELETE-001/002/003 chưa được Tech Lead accept.
- Cơ chế FCM cancel (O1) block integration nếu subscriber được đưa vào scope (hiện đánh dấu Open — có thể tách subscriber ra ngoài để không block core delete).

---

## 7. Rollback Plan

```bash
# KHÔNG có migration để revert (soft-delete dùng cột status sẵn có).

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/

# Nếu cần khôi phục reminder bị cancel sai trong lúc test (dev only):
# psql ... -c "UPDATE reminders SET status='PENDING' WHERE reminder_id='<id>';"

# Gap vẫn OPEN → giữ nguyên entry trong tracking.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định cho phép cancel COMPLETED/SKIPPED (không có ADR) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic (ownership/state) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `cancelFcmPush`/`REMINDER_CANCELLED` như thể đã tồn tại mà chưa thêm | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

## 19. Implementation Sync

| Date | Status | Evidence |
|---|---|---|
| 2026-07-10 | `Partially Implemented` | `mvnw test -Dtest="UpdateReminderServiceTest,ReminderSecurityTest"` → 27/27 passing; `mvnw test -Dtest="*Reminder*Test,*TodayTask*Test"` → 61/61 passing. Full `mvnw test` remains red from unrelated existing failures, so truthful sync is not `Implemented`. |

*TDD Template v2.0 — CASE 2.0 Red Gate + Anti-Pattern Detection. Status: Draft.*
