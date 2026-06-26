# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC134 — Enable Fall Detection

**Document ID:** `CB-SAFETY-IMP-002-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC134_EnableFallDetection/UC134_EnableFallDetection_TDS.md` (CB-SAFETY-IMP-002)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.4.2`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC134 Enable Fall Detection |

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
| **Feature / Gap ID** | `CB-SAFETY-IMP-002` |
| **Module** | `Enable Fall Detection — safety` |
| **Spec gốc** | `CB-SAFETY-IMP-002` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC133 SafetyConfigChanged event, IAM` |
| **Downstream Consumers** | `UC136 DetectSuspectedFallOrImpact` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-002 §17`, `ADR-SAFETY-003` |
| **Constraints Injected** | C1 (idempotent), C2 (event-driven), C3 (event publish), C4 (sensitivityLevel from config), C5 (STOPPED not DELETE) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | enable() có thể create multiple sessions | ADR-SAFETY-003: idempotent | Test verify 2nd enable = same session |
| L2 | Trigger mechanism không rõ | ADR-SAFETY-003: @EventListener | Test verify event triggers enable() |
| L3 | disable() deletes record | Append-only: set status=STOPPED | Test verify record still exists after disable |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC134 Enable Fall Detection:
├── Service (FallDetectionService.enable() — mock Repository, EventPublisher)
├── Event Handler (SafetyConfigChangedHandler — verify wiring)
└── Integration (Testcontainers PostgreSQL — verify session persistence)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-134 §3.3.4.2` | Enable fall detection → ACTIVE IMU session |
| `ADR-SAFETY-003` | Event-driven; idempotent |
| `BR-SAFETY-005` | 1 ACTIVE session per user |
| `BR-SAFETY-006` | Triggered by SafetyConfigChanged |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | enable() — no ACTIVE → create new | `FallDetectionService.enable()` | `FD-TC-001` |
| TC-COND-002 | enable() — ACTIVE exists → return it | Idempotency | `FD-TC-002` |
| TC-COND-003 | SafetyConfigChanged → enable() triggered | Event wiring | `FD-TC-003` |
| TC-COND-004 | FallDetectionEnabled event published | Event publishing | `FD-TC-004` |
| TC-COND-005 | disable() → STOPPED (not delete) | Append-only | `FD-TC-005` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | ACTIVE/STOPPED | IMU session FSM |
| Error Guessing | Multiple concurrent enable() calls | Race condition in idempotency |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | Mock | `findActiveByUserId()` → empty | First enable |
| `FX-002` | DB seed | `imu_monitoring_sessions {status: ACTIVE, userId: 'user-001'}` | Idempotency test |
| `FX-003` | Event | `SafetyConfigChanged {userId: 'user-001', fallDetectionEnabled: true, sensitivityLevel: 'MEDIUM'}` | Event trigger |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// FallDetectionTestFactory.java
class FallDetectionTestFactory {

    static ImuMonitoringSession makeActiveSession() {
        ImuMonitoringSession s = new ImuMonitoringSession();
        s.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        s.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        s.setStatus(ImuSessionStatus.ACTIVE);
        s.setSensitivityLevel("MEDIUM");
        s.setStartedAt(Instant.parse("2026-06-26T08:00:00Z"));
        return s;
    }

    static SafetyConfigChanged makeConfigChangedEvent(boolean enabled) {
        return new SafetyConfigChanged(
            UUID.randomUUID(),
            "SafetyConfigChanged",
            Instant.now(),
            "1.0",
            new SafetyConfigChanged.Payload(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                enabled,
                "MEDIUM"
            ),
            new SafetyConfigChanged.Metadata(UUID.randomUUID(), "user-001")
        );
    }
}
```

---

### FD-TC-001 — enable(): no ACTIVE session → create new

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService.enable()`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-134 §3.3.4.2`

**Test Steps:**
1. Arrange: `findActiveByUserId()` → empty (FX-001)
2. Act: `fallDetectionService.enable(userId, "MEDIUM")`
3. Assert: `repo.save()` called; result.status = ACTIVE

**Expected Result (PASS):**
- New ACTIVE session created

**Current Status:** 🔴 Not written

---

### FD-TC-002 — enable(): ACTIVE already exists → idempotent return

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService.enable() — idempotency`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-003 / BR-SAFETY-005`

**Test Steps:**
1. Arrange: `findActiveByUserId()` → FX-002 (ACTIVE session)
2. Act: `fallDetectionService.enable(userId, "MEDIUM")`
3. Assert: `repo.save()` NOT called; result.sessionId = FX-002.id

**Expected Result (PASS):**
- Existing session returned; no new INSERT

**Current Status:** 🔴 Not written

---

### FD-TC-003 — SafetyConfigChanged (enabled=true) → enable() triggered

**Severity:** `HIGH`
**Feature Under Test:** `SafetyConfigChangedHandler wiring`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigChangedHandlerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-003 / BR-SAFETY-006`

**Test Steps:**
1. Arrange: mock FallDetectionService; event = FX-003 (enabled=true)
2. Act: `handler.onSafetyConfigChanged(event)`
3. Assert: `fallDetectionService.enable()` called with userId="user-001"

**Current Status:** 🔴 Not written

---

### FD-TC-004 — FallDetectionEnabled event published

**Severity:** `MEDIUM`
**Feature Under Test:** `FallDetectionService event publishing`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Test Steps:**
1. Arrange: no ACTIVE session; mock EventPublisher
2. Act: `fallDetectionService.enable(userId, "MEDIUM")`
3. Assert: `eventPublisher.publishEvent(FallDetectionEnabled)` called

**Current Status:** 🔴 Not written

---

### FD-TC-005 — disable(): set status=STOPPED (not delete)

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService.disable() — append-only`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-SAFETY-IMP-002 §17 C5 (append-only)`

**Test Steps:**
1. Arrange: FX-002 (ACTIVE session) returned by findActiveByUserId()
2. Act: `fallDetectionService.disable(userId)`
3. Assert: session.status = STOPPED; `repo.delete()` NOT called
4. Assert: `repo.save(stoppedSession)` called

**Expected Result (PASS):**
- Session exists with status=STOPPED (not deleted)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FD-TC-001` | `FallDetectionServiceTest.java` | `[ ]` | `—` | — |
| `FD-TC-002` | `FallDetectionServiceTest.java` | `[ ]` | `—` | — |
| `FD-TC-003` | `SafetyConfigChangedHandlerTest.java` | `[ ]` | `—` | — |
| `FD-TC-004` | `FallDetectionServiceTest.java` | `[ ]` | `—` | — |
| `FD-TC-005` | `FallDetectionServiceTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class FallDetectionService implements IFallDetectionService {
    @Override
    public ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public void disable(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual |
|-------|----------|--------|
| `FD-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `FD-TC-002` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `FD-TC-003` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `FD-TC-004` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `FD-TC-005` | 🔴 FAIL | ☐ FAIL ☐ PASS |

**Red Gate Evidence:** Stub commit hash: `___` | All FAIL? ☐ Yes → **GATE-2 PASS**

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-002` approved
- [ ] UC133 deployed (SafetyConfigChanged event infrastructure)
- [ ] V39 migration approved

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] FD-TC-002 PASS: idempotency verified
- [ ] FD-TC-005 PASS: append-only (no delete)

**CASE 2.0:**
- [ ] Red Gate PASS
- [ ] Props Isolation: `FallDetectionTestFactory` used

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS imu_monitoring_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '39';"
git checkout -- src/main/java/com/carebridge/backend/safety/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|-------------|----------|-------|-----------|
| AP-AI-001 | Unconstrained Gen | TC không test idempotency | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-005 | Hallucinated Contract | Test import ImuRepo not in §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện → approved

---

*TDD Spec v1.0 — UC134 Enable Fall Detection — CB-SAFETY-IMP-002-TEST*
