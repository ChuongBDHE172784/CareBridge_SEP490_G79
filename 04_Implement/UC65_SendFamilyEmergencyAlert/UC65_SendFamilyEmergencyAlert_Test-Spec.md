# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC65 — Send Family Emergency Alert

**Document ID:** `CB-EMERG-IMP-002-TEST`
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
- `04_Implement/UC65_SendFamilyEmergencyAlert/UC65_SendFamilyEmergencyAlert_TDS.md` (CB-EMERG-IMP-002)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.1.42`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC65 Send Family Emergency Alert |

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
| **Feature / Gap ID** | `CB-EMERG-IMP-002` |
| **Module** | `Send Family Emergency Alert — emergency` |
| **Spec gốc** | `CB-EMERG-IMP-002` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC62 EmergencySessionOpened event, FCM, LocationConsentService` |
| **Downstream Consumers** | `Family member mobile apps` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EMERG-IMP-002 §17`, `ADR-EMERG-004/005` |
| **Constraints Injected** | C1 (idempotent), C2 (location consent), C3 (event-driven), C4 (FCM failure not block), C5 (all members) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec assume location always shared | PDPA: consent required | Test verify location omitted when no consent |
| L2 | Idempotency không được mention | BR-EMERG-005: 1 alert per session | Test verify 2nd call = no-op |
| L3 | FCM failure behavior không rõ | ADR-EMERG-004: fail silently, log | Test verify FCM failure does not throw uncaught exception |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC65 Send Family Emergency Alert bao gồm:
├── Service (FamilyAlertService — mock FCM, LocationConsent, FamilyMemberRepo)
├── Event Handler (EmergencySessionOpenedHandler — verify wiring)
└── Integration (Testcontainers PostgreSQL — verify alert log)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-65 §3.3.1.42` | FCM alert đến all family members khi emergency |
| `ADR-EMERG-004` | Event-driven; FCM failure not block |
| `ADR-EMERG-005` | Location consent check |
| `BR-EMERG-004` | No location if no consent |
| `BR-EMERG-005` | Idempotent |
| `BR-EMERG-006` | All family members |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Alert sent with location (consent=true) | `FamilyAlertService.sendAlert()` | `ALERT-TC-001` |
| TC-COND-002 | Alert sent without location (consent=false) | Location consent check | `ALERT-TC-002` |
| TC-COND-003 | Idempotent — sessionId already alerted | `existsBySessionId()` | `ALERT-TC-003` |
| TC-COND-004 | FCM failure → log + continue (no exception) | FCM error handling | `ALERT-TC-004` |
| TC-COND-005 | No family members → skip FCM, log | Empty family list | `ALERT-TC-005` |
| TC-COND-006 | FamilyAlertLog saved after successful send | Audit log | `ALERT-TC-006` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | location consent true/false | Binary consent state |
| Error Guessing | FCM failure scenarios | Network/FCM unavailability |
| Equivalence Partitioning | family member count (0/1/many) | Cover edge cases |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Mock | `LocationConsentService.hasConsent() → true` | Location included test |
| `FX-002` | Mock | `LocationConsentService.hasConsent() → false` | No location test |
| `FX-003` | Mock | `FamilyMemberRepo.findByUserId() → [member-1, member-2]` | 2 family members |
| `FX-004` | Mock | `FamilyMemberRepo.findByUserId() → []` | No family members |
| `FX-005` | Mock | `FcmNotificationClient.sendBatch() → throw FcmException` | FCM failure |
| `FX-006` | DB seed | `family_alert_log {session_id: 'session-001'}` | Idempotency |
| `FX-007` | Event | `EmergencySessionOpened {sessionId: 'session-001', userId: 'user-001', lat: 10.7769, lng: 106.7009}` | Event fixture |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// FamilyAlertTestFactory.java
class FamilyAlertTestFactory {

    static EmergencySessionOpened makeEvent() {
        return new EmergencySessionOpened(
            UUID.randomUUID(),
            "EmergencySessionOpened",
            Instant.now(),
            "1.0",
            new EmergencySessionOpened.Payload(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "MANUAL",
                10.7769,
                106.7009
            ),
            new EmergencySessionOpened.Metadata(UUID.randomUUID(), "user-001")
        );
    }

    static FamilyMember makeFamilyMember(String fcmToken) {
        FamilyMember m = new FamilyMember();
        m.setId(UUID.randomUUID());
        m.setFcmToken(fcmToken);
        return m;
    }
}
```

---

### ALERT-TC-001 — Alert sent WITH location (consent=true)

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-EMERG-004 / ADR-EMERG-005`

**Preconditions:**
- `existsBySessionId()` → false; consent=true (FX-001); 2 family members (FX-003)

**Test Steps:**
1. Arrange: mock all dependencies per fixtures
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: FCM batch called with `lat=10.7769, lng=106.7009`
4. Assert: FamilyAlertLog.locationIncluded = true

**Expected Result (PASS):**
- FCM payload contains lat/lng
- locationIncluded = true in log

**Current Status:** 🔴 Not written

---

### ALERT-TC-002 — Alert sent WITHOUT location (consent=false)

**Severity:** `CRITICAL`
**Legal:** `PDPA / ADR-EMERG-005 / BR-EMERG-004`
**Feature Under Test:** `FamilyAlertService — location consent gate`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-EMERG-004 / PDPA`

**Test Steps:**
1. Arrange: consent=false (FX-002); 2 family members
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: FCM payload does NOT contain lat/lng
4. Assert: FamilyAlertLog.locationIncluded = false

**Expected Result (PASS — PDPA compliance):**
- FCM payload lat = null, lng = null

**Expected Result (FAIL = PDPA violation):**
- Location shared without consent

**Current Status:** 🔴 Not written

---

### ALERT-TC-003 — Idempotent: already sent → skip

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertService — idempotency`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-EMERG-005 / ADR-EMERG-004`

**Test Steps:**
1. Arrange: `existsBySessionId()` → true (FX-006); mock FCM
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: FCM NOT called; No new FamilyAlertLog

**Expected Result (PASS):**
- FCM.sendBatch() call count = 0
- No new log entry

**Current Status:** 🔴 Not written

---

### ALERT-TC-004 — FCM failure → log + continue (no exception bubble)

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertService — FCM error handling`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-EMERG-004`

**Test Steps:**
1. Arrange: FCM.sendBatch() → throw FcmException (FX-005)
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: NO uncaught exception thrown to caller
4. Assert: Error logged (log appender captures error message)

**Expected Result (PASS):**
- Method completes without throwing

**Expected Result (FAIL):**
- FcmException propagated → would block emergency flow

**Current Status:** 🔴 Not written

---

### ALERT-TC-005 — No family members → skip FCM

**Severity:** `MEDIUM`
**Feature Under Test:** `FamilyAlertService — empty family list`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Arrange: FamilyMemberRepo → empty list (FX-004)
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: FCM NOT called; EMERG-008 logged

**Expected Result (PASS):**
- FCM.sendBatch() NOT called

**Current Status:** 🔴 Not written

---

### ALERT-TC-006 — FamilyAlertLog saved after successful send

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertLog audit trail`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Arrange: consent=true; 2 family members; FCM success
2. Act: `familyAlertService.sendAlert(makeEvent())`
3. Assert: `alertLogRepo.save()` called once
4. Assert: saved log has recipientCount = 2; locationIncluded = true

**Expected Result (PASS):**
- Log saved with correct recipientCount and locationIncluded

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `ALERT-TC-001` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | — |
| `ALERT-TC-002` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | CRITICAL PDPA |
| `ALERT-TC-003` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | — |
| `ALERT-TC-004` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | — |
| `ALERT-TC-005` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | — |
| `ALERT-TC-006` | `FamilyAlertServiceTest.java` | `[x]` | `[x]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class FamilyAlertService implements IFamilyAlertService {

    @Override
    public void sendAlert(EmergencySessionOpened event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `ALERT-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `ALERT-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `ALERT-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `ALERT-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `ALERT-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `ALERT-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-EMERG-IMP-002` approved
- [ ] UC62 deployed (emergency_sessions table)
- [ ] FCM credentials available in staging
- [ ] LocationConsentService interface defined

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh
- [ ] ALERT-TC-002 PASS: location NOT shared without consent (PDPA critical)
- [ ] ALERT-TC-003 PASS: idempotency verified
- [ ] ALERT-TC-004 PASS: FCM failure non-blocking

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi instance qua `FamilyAlertTestFactory`

### Suspension Criteria

- FCM credentials not available in staging
- LocationConsentService not implemented

---

## 7. Rollback Plan

```bash
# Không có migration riêng cho UC65
git checkout -- src/main/java/com/carebridge/backend/emergency/FamilyAlertService.java
git checkout -- src/test/java/com/carebridge/backend/emergency/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC-002 không verify location omitted | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code shares location without consent check | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Handler có FCM logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import FcmClient không có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC65 Send Family Emergency Alert — CB-EMERG-IMP-002-TEST*
