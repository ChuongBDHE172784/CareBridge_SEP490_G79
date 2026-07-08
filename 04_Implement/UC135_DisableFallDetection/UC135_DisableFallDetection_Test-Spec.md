# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC135 — Disable Fall Detection

**Document ID:** `CB-SAFETY-IMP-003-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC135_DisableFallDetection/UC135_DisableFallDetection_TDS.md` (CB-SAFETY-IMP-003)
- `04_Implement/UC134_EnableFallDetection/UC134_EnableFallDetection_TDS.md` (CB-SAFETY-IMP-002)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.4.3`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC135 Disable Fall Detection |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CB-SAFETY-IMP-003` |
| **Module** | `Disable Fall Detection — safety` |
| **Spec gốc** | `CB-SAFETY-IMP-003` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC134 (FallDetectionService interface, imu_monitoring_sessions table)` |
| **Downstream Consumers** | `UC136 (stops receiving IMU data)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-003 §17`, `ADR-SAFETY-004` |
| **Constraints Injected** | C1 (STOPPED not DELETE), C2 (no-op if no ACTIVE), C3 (event publish), C4 (event-driven) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | disable() có thể DELETE record | ADR-SAFETY-004: append-only | Test verify record still exists with status=STOPPED |
| L2 | Throws exception if no ACTIVE | BR-SAFETY-008: no-op | Test verify no exception on empty |
| L3 | PDPA: ended_at không được set | Audit trail cần ended_at | Test verify ended_at != null |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC135 Disable Fall Detection:
├── Service (FallDetectionService.disable() — mock Repository, EventPublisher)
├── Event Handler (SafetyConfigChangedHandler — disable branch)
└── Integration (Testcontainers PostgreSQL — verify record not deleted, status=STOPPED)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-135 §3.3.4.3` | Disable → status=STOPPED |
| `ADR-SAFETY-004` | Append-only (no DELETE) |
| `BR-SAFETY-007` | status=STOPPED; record not deleted |
| `BR-SAFETY-008` | No active session → no-op |
| `BR-SAFETY-009` | SafetyConfigChanged(false) trigger |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | disable() — ACTIVE session exists → STOPPED | `FallDetectionService.disable()` | `DIS-TC-001` |
| TC-COND-002 | disable() — no ACTIVE session → no-op | No exception | `DIS-TC-002` |
| TC-COND-003 | SafetyConfigChanged(false) → disable() triggered | Event handler | `DIS-TC-003` |
| TC-COND-004 | FallDetectionDisabled event published | Event publishing | `DIS-TC-004` |
| TC-COND-005 | PDPA: record NOT deleted (audit trail) | Append-only | `DIS-TC-005` |
| TC-COND-006 | ROLE_PARTNER access → 403 | Authorization | `DIS-TC-006` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | ACTIVE→STOPPED | IMU session FSM |
| Negative Testing | no ACTIVE session | no-op branches |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB seed / Mock | `imu_monitoring_sessions {status: ACTIVE, userId: 'user-001'}` | disable() with ACTIVE |
| `FX-002` | Mock | `findActiveByUserId()` → empty | No-op test |
| `FX-003` | Event | `SafetyConfigChanged {userId: 'user-001', fallDetectionEnabled: false}` | Event trigger |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// FallDetectionTestFactory.java (extends from UC134 factory)
class FallDetectionTestFactory {

    static ImuMonitoringSession makeActiveSession() {
        ImuMonitoringSession s = new ImuMonitoringSession();
        s.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        s.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        s.setStatus(ImuSessionStatus.ACTIVE);
        s.setSensitivityLevel("MEDIUM");
        s.setStartedAt(Instant.parse("2026-06-26T08:00:00Z"));
        s.setEndedAt(null);
        return s;
    }

    static SafetyConfigChanged makeConfigChangedDisableEvent() {
        return new SafetyConfigChanged(
            UUID.randomUUID(),
            "SafetyConfigChanged",
            Instant.now(),
            "1.0",
            new SafetyConfigChanged.Payload(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                false,  // <-- disabled
                "MEDIUM"
            ),
            new SafetyConfigChanged.Metadata(UUID.randomUUID(), "user-001")
        );
    }
}
```

---

### DIS-TC-001 — disable(): ACTIVE session → status=STOPPED + endedAt set

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService.disable()`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-004 / BR-SAFETY-007`

**Test Steps:**
1. Arrange: `findActiveByUserId()` → FX-001 (ACTIVE session)
2. Act: `fallDetectionService.disable(userId)`
3. Assert: `repo.save()` called with session.status=STOPPED
4. Assert: `session.getEndedAt()` != null

**Expected Result (PASS):**
- STOPPED status saved; endedAt set; record exists

**Current Status:** 🔴 Not written

---

### DIS-TC-002 — disable(): no ACTIVE session → no-op

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService.disable() — no-op branch`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-SAFETY-008`

**Test Steps:**
1. Arrange: `findActiveByUserId()` → empty (FX-002)
2. Act: `fallDetectionService.disable(userId)`
3. Assert: no exception thrown
4. Assert: `repo.save()` NOT called

**Expected Result (PASS):**
- Silent no-op; no exception; no DB update

**Current Status:** 🔴 Not written

---

### DIS-TC-003 — SafetyConfigChanged(false) → disable() triggered

**Severity:** `HIGH`
**Feature Under Test:** `SafetyConfigChangedHandler — disable branch`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigChangedHandlerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-SAFETY-009`

**Test Steps:**
1. Arrange: mock FallDetectionService; event = FX-003 (enabled=false)
2. Act: `handler.onSafetyConfigChanged(event)`
3. Assert: `fallDetectionService.disable()` called (not enable)

**Expected Result (PASS):**
- disable() invoked when fallDetectionEnabled=false

**Current Status:** 🔴 Not written

---

### DIS-TC-004 — FallDetectionDisabled event published

**Severity:** `MEDIUM`
**Feature Under Test:** `FallDetectionService event publishing`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Test Steps:**
1. Arrange: FX-001 (ACTIVE session); mock EventPublisher
2. Act: `fallDetectionService.disable(userId)`
3. Assert: `eventPublisher.publishEvent(FallDetectionDisabled)` called

**Expected Result (PASS):**
- FallDetectionDisabled event published

**Current Status:** 🔴 Not written

---

### DIS-TC-005 — PDPA CRITICAL: record NOT deleted after disable()

**Severity:** `CRITICAL — PDPA`
**Feature Under Test:** `Append-only constraint — no DELETE`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SAFETY-004 / BR-SAFETY-007 / PDPA`

**Test Steps:**
1. Arrange: Testcontainers PostgreSQL; V39 applied; INSERT ACTIVE session
2. Act: `fallDetectionService.disable(userId)`
3. Assert: `SELECT count(*) FROM imu_monitoring_sessions WHERE user_id=?` = 1 (not 0)
4. Assert: `status = 'STOPPED'`; `ended_at` != null

**Expected Result (PASS):**
- Record still in DB with status=STOPPED (NOT deleted)

**PDPA compliance:** Audit trail must be preserved

**Current Status:** 🔴 Not written

---

### DIS-TC-006 — ROLE_PARTNER → 403 Forbidden

**Severity:** `HIGH`
**Feature Under Test:** `Authorization`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Arrange: JWT with ROLE_PARTNER
2. Act: `POST /api/v1/safety/fall-detection/disable`
3. Assert: HTTP 403; code = SAFETY-004

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DIS-TC-001` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | — |
| `DIS-TC-002` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | — |
| `DIS-TC-003` | `SafetyConfigChangedHandlerTest.java` | `[x]` | `[x]` | — |
| `DIS-TC-004` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | — |
| `DIS-TC-005` | `FallDetectionIntegrationTest.java` | `[x]` | `[x]` | PDPA CRITICAL |
| `DIS-TC-006` | `FallDetectionControllerTest.java` | `[x]` | `[x]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> `FallDetectionService.disable()` stub đã được định nghĩa trong UC134 Red Gate stub:

```java
@Override
public void disable(UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual |
|-------|----------|--------|
| `DIS-TC-001` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `DIS-TC-002` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `DIS-TC-003` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `DIS-TC-004` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `DIS-TC-005` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `DIS-TC-006` | 🔴 FAIL | ☑ FAIL ☐ PASS |

**Red Gate Evidence:** Stub commit hash: `___` | All FAIL? ☑ Yes → **GATE-2 PASS**

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-003` approved
- [ ] UC134 deployed (FallDetectionService interface, imu_monitoring_sessions table)
- [ ] `FallDetectionService.disable()` stub throws `UnsupportedOperationException`

### Exit Criteria (DoD)

- [x] `./mvnw test` xanh
- [ ] DIS-TC-005 PASS: PDPA — record NOT deleted (CRITICAL)
- [ ] DIS-TC-002 PASS: no-op branch (no exception)

**CASE 2.0:**
- [ ] Red Gate PASS
- [ ] Props Isolation: `FallDetectionTestFactory` used

---

## 7. Rollback Plan

```bash
# No migration to rollback
# Revert code only
git checkout -- src/main/java/com/carebridge/backend/safety/FallDetectionService.java
git checkout -- src/main/java/com/carebridge/backend/safety/SafetyConfigChangedHandler.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|-------------|----------|-------|-----------|
| AP-AI-001 | Unconstrained Gen | Code DELETE record instead of STOPPED | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | DIS-TC-001 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-004 | Swallowed Exceptions | disable() hides exception when no session | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện → approved

---

*TDD Spec v1.0 — UC135 Disable Fall Detection — CB-SAFETY-IMP-003-TEST*
