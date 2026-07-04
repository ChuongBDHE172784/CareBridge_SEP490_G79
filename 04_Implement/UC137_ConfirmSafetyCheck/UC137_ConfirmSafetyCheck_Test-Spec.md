# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC137 — Confirm Safety Check

**Document ID:** `CB-SAFETY-IMP-005-TDD`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260705090000__create_safety_check_prompts.sql` — new migration (this feature)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.4.5`
- `04_Implement/UC137_ConfirmSafetyCheck/UC137_ConfirmSafetyCheck_TDS.md` (`CB-SAFETY-IMP-005`)
- `04_Implement/UC136_DetectSuspectedFallOrImpact/UC136_DetectSuspectedFallOrImpact_TDS.md`

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC137 Confirm Safety Check |
| 2026-07-03 | AI Agent | Đóng RG-4: Product Owner xác nhận `countdownSeconds=30`, FCM retry xác định không cần thiết (ADR-SAFETY-008). Cập nhật §2 Logic Issues L3, Suspension Criteria. |

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
| **Feature / Gap ID** | `GAP-UC137` |
| **Module** | `Confirm Safety Check — safety bounded context` |
| **Spec gốc** | `CB-SAFETY-IMP-005` |
| **Priority** | 🔴 P0 — life-safety critical |
| **Sprint** | `Sprint 2 — IMU Detection And Emergency Alert` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC136 (SuspectedFallDetected)`, `safety_monitoring_config` |
| **Downstream Consumers** | `com.carebridge.backend.ai.event.EmergencyEscalationTriggered` → `EmergencyEscalationHandler` (UC62, existing) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-005 §17` |
| **Constraints Injected** | C1 (auto-escalate on timeout), C2 (ownership check), C3 (server-side countdown), C4 (reuse existing escalation path), C5 (append-only imu_safety_events preserved), C6 (terminal state) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Task brief referenced `safety_monitoring_settings.countdown_seconds` and `safety_events.user_response`/`response_at` | Real table is `safety_monitoring_config` (no countdown column originally) and `imu_safety_events` (append-only, REVOKE UPDATE/DELETE, no response columns) — verified in `V20260627000005__create_safety_monitoring_config.sql` and `V20260627000007__create_safety_events.sql` | Tests assert against NEW table `safety_check_prompts` (via `V20260705090000` migration) and the new `countdown_seconds` column added to `safety_monitoring_config` — never assume the non-existent `safety_events`/`safety_monitoring_settings` names |
| L2 | Assumed a "UC62 emergency session listens directly to SuspectedFallDetected" | Actual: `EmergencyEscalationHandler` only listens to `com.carebridge.backend.ai.event.EmergencyEscalationTriggered`; nothing consumes `SuspectedFallDetected` today | Tests verify UC137's new `SuspectedFallDetectedHandler` bridges the gap, and that escalation republishes the EXISTING `EmergencyEscalationTriggered` type (not a new/duplicate type) |
| L3 | SRS text for UC137 is generic-templated and does not state exact countdown seconds or FCM retry policy | No explicit number found anywhere in SRS §3.3.4.5 | **RG-4 RESOLVED 2026-07-03**: Product Owner confirmed `countdownSeconds` default = 30 (ADR-SAFETY-009, Accepted). FCM retry mechanism confirmed unnecessary (ADR-SAFETY-008 — countdown is server-side, independent of FCM delivery). Tests use `countdownSeconds=30` as the confirmed default, still parameterized/injectable for other test scenarios (e.g. boundary tests), but no longer flagged as blocking Open |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Confirm Safety Check bao gồm các layer:
├── Domain (SafetyCheckPrompt entity, SafetyCheckResponseType enum — pure logic)
├── Services (SafetyCheckService — mock ISafetyCheckRepository, ApplicationEventPublisher với Mockito)
├── Scheduler (SafetyCheckTimeoutScheduler — mock ISafetyCheckService)
├── Controller (SafetyCheckController — mock ISafetyCheckService với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — full flow incl. downstream EmergencyEscalationHandler/FamilyAlertService)
└── Mobile (Flutter widget tests — confirm screen countdown UI, flutter_test)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-137 §3.3.4.5` | Confirm prompt shown before auto-alert; 3 response options |
| `ADR-SAFETY-007` | Confirm-countdown gate architecture, reuse of existing escalation handler |
| `ADR-SAFETY-008` | FCM push failure must not block countdown |
| `ADR-SAFETY-009` | countdown_seconds schema gap + migration |
| `BR-SAFETY` | Timeout MUST auto-escalate — CRITICAL |
| `BR-RBAC` | Only owning Mother may confirm |
| `CB-SAFETY-IMP-005 §8` | Service/Repository interface contracts |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother confirms "I am OK" before expiry | `SafetyCheckService.confirm()` | `SAFETY137-TC-001` |
| TC-COND-002 | Mother confirms "Need help" before expiry | `SafetyCheckService.confirm()` | `SAFETY137-TC-002` |
| TC-COND-003 | Mother confirms "Call emergency" before expiry | `SafetyCheckService.confirm()` | `SAFETY137-TC-003` |
| TC-COND-004 | Countdown expires with no response | `SafetyCheckService.processExpired()` | `SAFETY137-TC-004` (CRITICAL) |
| TC-COND-005 | Duplicate confirm on terminal-state prompt | `SafetyCheckService.confirm()` | `SAFETY137-TC-005` |
| TC-COND-006 | Non-owner attempts confirm | `SafetyCheckService.confirm()` | `SAFETY137-TC-006` |
| TC-COND-007 | FCM push failure during prompt creation | `SuspectedFallDetectedHandler` | `SAFETY137-TC-007` |
| TC-COND-008 | Countdown snapshot immutability vs. live config | `SafetyCheckService.createPrompt()` | `SAFETY137-TC-008` |
| TC-COND-009 | Expired prompt confirm race (SAFETY-011) | `SafetyCheckService.confirm()` | `SAFETY137-TC-009` |
| TC-COND-010 | Controller RBAC — non-Mother role rejected | `SafetyCheckController` | `SAFETY137-TC-010` |
| TC-COND-011 | Full E2E: timeout → EmergencySession → FamilyAlertLog | Integration | `SAFETY137-TC-INT-001` (CRITICAL) |
| TC-COND-012 | Mobile: countdown UI ticks down and disables after response | Flutter widget | `SAFETY137-TC-MOB-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `SafetyCheckResponseType` (3 valid values + invalid string) | Ensure enum boundary validated at DTO layer |
| Boundary Value Analysis | `expiresAt` exactly at `now`, `now - 1ms`, `now + 1ms` | Timeout is life-safety critical — off-by-one must not leave a gap |
| State Transition Testing | `SafetyCheckPrompt` state machine (PENDING → terminal) | Verify no illegal reverse transitions |
| Error Guessing | Concurrent confirm + scheduler race | Verify unique constraint / optimistic path prevents double-escalation |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `SafetyCheckPrompt{responseType=null, expiresAt=now+30s}` | Happy path pending |
| `FX-002` | DB seed | `SafetyCheckPrompt{responseType=I_AM_OK, respondedAt=now-5s}` | Terminal-state duplicate test |
| `FX-003` | DB seed | `SafetyCheckPrompt{expiresAt=now-1s, responseType=null, autoEscalated=false}` | Expired-pending fixture for scheduler |
| `FX-004` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context, owner |
| `FX-005` | JWT | `{ sub: 'mother-002', role: 'ROLE_MOTHER' }` | Non-owner attacker |
| `FX-006` | DB seed | `safety_monitoring_config{countdownSeconds=45}` | Snapshot-immutability test |

---

## 4. Test Case Specification

> **TC ID format:** `SAFETY137-TC-[NNN]`

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// SafetyCheckTestFactory.java
// ═══════════════════════════════════════════════════════════
class SafetyCheckTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B2");
    static final UUID SAFETY_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C3");

    static SafetyCheckPrompt makePendingPrompt() {
        return makePendingPrompt(p -> {});
    }

    static SafetyCheckPrompt makePendingPrompt(Consumer<SafetyCheckPrompt> overrides) {
        SafetyCheckPrompt prompt = SafetyCheckPrompt.builder()
                .id(UUID.randomUUID())
                .safetyEventId(SAFETY_EVENT_ID)
                .userId(MOTHER_ID)
                .countdownSeconds(30)
                .promptSentAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(30))
                .responseType(null)
                .autoEscalated(false)
                .createdBy("SYSTEM")
                .build();
        overrides.accept(prompt);
        return prompt;
    }

    static SuspectedFallDetected makeSuspectedFallDetected() {
        return new SuspectedFallDetected(
                UUID.randomUUID(), MOTHER_ID, SAFETY_EVENT_ID,
                "SUSPECTED_FALL", 14.0, null, null, Instant.now());
    }
}
```

---

### SAFETY137-TC-001 — Confirm "I am OK" within countdown → no escalation

**Severity:** `HIGH`
**Feature Under Test:** `SafetyCheckService.confirm(UUID, UUID, SafetyCheckResponseType)`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-007 §Decision`, `SRS §3.3.4.5`

**Preconditions:**
- `FX-001` seeded (pending prompt, not expired)

**Test Steps:**
1. Arrange: `SafetyCheckTestFactory.makePendingPrompt()`, mock repository `findById()` returns it
2. Act: call `confirm(promptId, MOTHER_ID, I_AM_OK)`
3. Assert: `responseType == I_AM_OK`, `respondedAt != null`, `eventPublisher` never invoked with `EmergencyEscalationTriggered`

**Expected Result (PASS):** Response returned with `escalated=false`; repository `save()` called once with updated entity.

**Expected Result (FAIL):** Escalation event published incorrectly, or state not persisted.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-002 — Confirm "Need help" → immediate escalation

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckService.confirm()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-007`, `SRS §3.3.4.5`

**Preconditions:** `FX-001` seeded

**Test Steps:**
1. Act: `confirm(promptId, MOTHER_ID, NEED_HELP)`
2. Assert: `eventPublisher.publishEvent()` called with `EmergencyEscalationTriggered{triggerSource="FALL_DETECTION_CONFIRMED"}`

**Expected Result (PASS):** Event published exactly once; response `escalated=true`.
**Expected Result (FAIL):** No event published, or wrong `triggerSource`.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-003 — Confirm "Call emergency" → immediate escalation

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckService.confirm()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-007`

**Preconditions:** `FX-001` seeded

**Test Steps:**
1. Act: `confirm(promptId, MOTHER_ID, CALL_EMERGENCY)`
2. Assert: same escalation behavior as TC-002, `responseType=CALL_EMERGENCY`

**Expected Result (PASS):** Event published, `escalated=true`.
**Expected Result (FAIL):** Behavior diverges from NEED_HELP path without documented reason.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-004 — CRITICAL: Countdown expiry with no response auto-escalates

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyCheckService.processExpired()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SAFETY (CRITICAL)`, `ADR-SAFETY-007 §Decision`

**Preconditions:** `FX-003` seeded (`expiresAt = now - 1s`, `responseType = null`, `autoEscalated = false`)

**Test Steps:**
1. Arrange: mock `repository.findPendingExpired(any())` returns `[FX-003 prompt]`
2. Act: call `processExpired()`
3. Assert: prompt saved with `autoEscalated=true`, `escalationTriggeredAt` set
4. Assert: `EmergencyEscalationTriggered` published with `triggerSource="FALL_DETECTION_UNCONFIRMED"`

**Expected Result (PASS — hành vi đúng):** Escalation ALWAYS fires for every expired-pending prompt found — zero silent skips.

**Expected Result (FAIL — dấu hiệu lỗi):** Any prompt found by `findPendingExpired` that does NOT result in a published event — this is a P0 life-safety bug.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important test in the suite. Must be run with multiple prompts in the returned list to confirm the loop does not short-circuit on first item.

---

### SAFETY137-TC-005 — Duplicate confirm on terminal-state prompt → SAFETY-010

**Severity:** `HIGH`
**Feature Under Test:** `SafetyCheckService.confirm()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SAFETY-007 (terminal state invariant)`

**Preconditions:** `FX-002` seeded (already `responseType=I_AM_OK`)

**Test Steps:**
1. Act: `confirm(promptId, MOTHER_ID, NEED_HELP)`
2. Assert: `SafetyException` thrown with code `SAFETY-010`, HTTP 409

**Expected Result (PASS):** Exception thrown; original `responseType=I_AM_OK` untouched (verify via `verify(repository, never()).save(any())`).
**Expected Result (FAIL):** State silently overwritten to NEED_HELP, or no exception thrown.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-006 — Non-owner attempts confirm → SAFETY-004

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `SafetyCheckService.confirm()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC`

**Preconditions:** `FX-001` seeded (`userId = MOTHER_ID`)

**Test Steps (Attack Simulation):**
1. Act: `confirm(promptId, OTHER_USER_ID, I_AM_OK)`
2. Assert: `SafetyException` thrown with code `SAFETY-004`, HTTP 403

**Expected Result (PASS = hệ thống an toàn):** 403 SAFETY-004; no state change.
**Expected Result (FAIL = lỗ hổng tồn tại):** Attacker can confirm/resolve another user's safety check, potentially suppressing a real emergency escalation.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-007 — FCM push failure during prompt creation does not block

**Severity:** `HIGH`
**Feature Under Test:** `SuspectedFallDetectedHandler.onSuspectedFallDetected()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SuspectedFallDetectedHandlerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SAFETY-008 §Decision`

**Preconditions:** Mock FCM adapter configured to throw `RuntimeException`

**Test Steps:**
1. Act: publish `SuspectedFallDetected` (via `SafetyCheckTestFactory.makeSuspectedFallDetected()`)
2. Assert: `SafetyCheckPrompt` still persisted with correct `expiresAt`
3. Assert: no exception propagates out of the handler

**Expected Result (PASS):** Prompt created regardless of FCM outcome.
**Expected Result (FAIL):** Exception bubbles up and prevents countdown from starting — violates "never delay emergency routing".

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-008 — Countdown snapshot immutable vs. live config changes

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyCheckService.createPrompt()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-SAFETY-IMP-005 §8.1 Javadoc`

**Preconditions:** `FX-006` — `safety_monitoring_config.countdown_seconds = 45` for `MOTHER_ID`

**Test Steps:**
1. Act: `createPrompt(makeSuspectedFallDetected())`
2. Assert: `prompt.countdownSeconds == 45`, `expiresAt == promptSentAt + 45s`
3. Mutate config to `countdown_seconds = 10` (simulate concurrent config change)
4. Assert: original prompt's `expiresAt` unchanged

**Expected Result (PASS):** Snapshot behavior confirmed — config changes never retroactively shrink/extend an in-flight countdown.
**Expected Result (FAIL):** `expiresAt` recalculated dynamically from live config — dangerous, could shorten a countdown a Mother is mid-response on, or silently extend past the safety SLA.

**Current Status:** 🔴 Not written

---

### SAFETY137-TC-009 — Confirm called after expiresAt but before scheduler runs (race) → SAFETY-011

**Severity:** `HIGH`
**Feature Under Test:** `SafetyCheckService.confirm()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyCheckServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-SAFETY-IMP-005 §10 Error Codes`

**Preconditions:** `FX-003`-like prompt (`expiresAt` in the past, `responseType=null`, `autoEscalated=false` — scheduler has not yet processed it)

**Test Steps:**
1. Act: `confirm(promptId, MOTHER_ID, I_AM_OK)` called in this race window
2. Assert: `SafetyException` thrown with code `SAFETY-011`, HTTP 410

**Expected Result (PASS):** Late confirm rejected cleanly with 410; entity untouched so the scheduler can still safely auto-escalate on its next poll.
**Expected Result (FAIL):** Late confirm silently accepted (could suppress a legitimate escalation that the scheduler should still fire), or confirm and scheduler race to double-publish escalation.

**Current Status:** 🔴 Not written
**Implementation Note:** Confirm and `processExpired()` must not both be allowed to mutate a prompt — recommend a DB-level guard (`WHERE response_type IS NULL AND auto_escalated = false` in the UPDATE) in addition to app-level check.

---

### SAFETY137-TC-010 — Controller RBAC: non-Mother role rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `PATCH /api/v1/safety/checks/{id}/confirm`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyCheckControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:** JWT with role `ROLE_PARTNER` (or any non-`ROLE_MOTHER`)

**Test Steps (Attack Simulation):**
1. `PATCH /api/v1/safety/checks/{id}/confirm` with `ROLE_PARTNER` JWT
2. Assert `403 Forbidden` (Spring Security `@PreAuthorize`, before reaching service layer)

**Expected Result (PASS = hệ thống an toàn):** `403`, no service method invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** Non-Mother role can confirm/suppress another actor's safety check.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SAFETY137-TC-INT-001 — Full E2E: timeout → EmergencySession → FamilyAlertLog

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: SuspectedFallDetected → SafetyCheckPrompt timeout → EmergencyEscalationTriggered → EmergencySession → FamilyAlertLog`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyCheckTimeoutIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically (incl. `V20260705090000`)
- Seed: `imu_monitoring_sessions` ACTIVE session for test user; `safety_monitoring_config` with `countdown_seconds=1`
- `FamilyMemberPortAdapter` stubbed to return at least 1 FCM token (real adapter returns empty list — see UC138 TDS gap)

**Test Steps:**
1. Publish `SuspectedFallDetected` for test user (simulating UC136 output)
2. Assert `safety_check_prompts` row created with `expires_at ≈ now + 1s`
3. Wait 2 seconds (past countdown + one scheduler poll cycle)
4. Assert `safety_check_prompts.auto_escalated = true`
5. Assert `emergency_sessions` row created with `trigger_source` reflecting fall-detection origin
6. Assert `family_alert_log` row created (proves existing UC65 `FamilyAlertService` was reached)

**Expected Result (PASS):**
- All 3 tables (`safety_check_prompts`, `emergency_sessions`, `family_alert_log`) show consistent linked state
- Total elapsed time from publish to `family_alert_log` row < 10s (test timeout)

**Expected Result (FAIL):**
- Any link in the chain missing — indicates UC137 did not correctly bridge into the existing UC62/UC65 wiring

**DB Assertion:**
```java
SafetyCheckPrompt prompt = safetyCheckRepository.findBySafetyEventId(safetyEventId).orElseThrow();
assertThat(prompt.isAutoEscalated()).isTrue();

EmergencySession session = emergencySessionRepository.findActiveByUserId(userId).orElseThrow();
assertThat(session.getStatus()).isEqualTo(EmergencyStatus.ACTIVE);

boolean alertSent = familyAlertLogRepository.existsBySessionId(session.getId());
assertThat(alertSent).isTrue();
```

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (Flutter)

---

### SAFETY137-TC-MOB-001 — Countdown UI ticks down and locks after response

**Severity:** `HIGH`
**Feature Under Test:** `SafetyCheckConfirmScreen` widget (`lib/features/safetyMonitoring/screens/`)
**Test File:** `test/features/safetyMonitoring/screens/safety_check_confirm_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** Widget pumped with `expiresAt = now + 30s` from mocked API response

**Test Steps:**
1. `pumpWidget(SafetyCheckConfirmScreen(prompt: fakePrompt))`
2. Verify 3 buttons rendered: "I am OK", "Need help", "Call emergency"
3. Tap "I am OK"
4. Verify API call triggered with `response: I_AM_OK`
5. Verify buttons disabled after tap (prevent double-submit)
6. Verify countdown label updates every second via `Timer.periodic` (use `flutter_test`'s `pumpAndSettle`/`FakeAsync`)

**Expected Result (PASS):** UI reflects countdown accurately; only one response can be submitted; screen shows confirmation state after success.
**Expected Result (FAIL):** Double-submit possible, or countdown display drifts from actual `expiresAt` (misleads Mother about remaining time — safety UX risk).

**Current Status:** 🔴 Not written
**Implementation Note:** Screen should NOT locally decide "timeout reached, hide buttons" without checking with backend — backend `processExpired()` is the source of truth (C1). Local countdown display is purely informational.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SAFETY137-TC-001` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-002` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-003` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-004` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — verify first |
| `SAFETY137-TC-005` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-006` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-007` | `SuspectedFallDetectedHandlerTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-008` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-009` | `SafetyCheckServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-010` | `SafetyCheckControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `SAFETY137-TC-INT-001` | `SafetyCheckTimeoutIntegrationTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL E2E |
| `SAFETY137-TC-MOB-001` | `safety_check_confirm_screen_test.dart:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SafetyCheckService implements ISafetyCheckService {

    @Override
    public SafetyCheckPromptResponse createPrompt(SuspectedFallDetected event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SafetyCheckPromptResponse confirm(UUID promptId, UUID userId, SafetyCheckResponseType response) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void processExpired() {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SAFETY137-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SAFETY137-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SAFETY137-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(fill during implementation phase)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-005` reviewed and approved (Status field = `Approved`)
- [ ] Logic Issues (Section 2) confirmed against actual codebase (`safety_monitoring_config`, `imu_safety_events` — done above)
- [ ] Flyway migration `V20260705090000` reviewed and staged
- [ ] UC136 (`SuspectedFallDetected`) and existing UC62/UC65 wiring verified present in codebase (done — `EmergencyEscalationHandler`, `FamilyAlertService` confirmed to exist)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration test (`SAFETY137-TC-INT-001`) green with Testcontainers
- [ ] `flutter test` — mobile widget test green
- [ ] Test coverage ≥ 80% lines for `SafetyCheckService`
- [ ] No business logic in `SafetyCheckController` (validation + mapping only)
- [ ] No PII/secret in plaintext logs
- [ ] **CRITICAL**: `SAFETY137-TC-004` and `SAFETY137-TC-INT-001` both green — timeout auto-escalate proven end-to-end before merge

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL against throw-stub before implementation begins
- [ ] **Contract Existence** — `./mvnw compile` clean, no hallucinated imports
- [ ] **Props Isolation** — verified via `SafetyCheckTestFactory`, no shared mutable state
- [ ] **Oracle Source** — every assert traces to BR-SAFETY/BR-RBAC/ADR-SAFETY-007/008/009

### Suspension Criteria

- `V20260705090000` migration blocked by DBA review
- ~~Product Owner has not yet confirmed `countdown_seconds` default (RG-4)~~ — **RESOLVED 2026-07-03**, confirmed = 30, no longer a suspension condition
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS safety_check_prompts CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE safety_monitoring_config DROP COLUMN IF EXISTS countdown_seconds;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705090000';"

git checkout -- src/main/java/com/carebridge/backend/safety/
git checkout -- src/main/resources/db/migration/V20260705090000__create_safety_check_prompts.sql
git checkout -- src/test/java/com/carebridge/backend/safety/

# Gap vẫn OPEN → giữ nguyên entry trong task tracking
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-SAFETY-007/008/009 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw-stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes architecture (e.g. direct UC62 call) not in ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test verifies business logic instead of delegation | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `SafetyEvent.userResponse` or `safety_monitoring_settings` (non-existent) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
