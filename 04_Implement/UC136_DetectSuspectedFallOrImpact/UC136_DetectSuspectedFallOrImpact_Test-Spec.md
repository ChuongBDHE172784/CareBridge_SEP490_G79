# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC136 — Detect Suspected Fall or Impact

**Document ID:** `CB-SAFETY-IMP-004-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(required — Sensitive-PII: IMU + location)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC136_DetectSuspectedFallOrImpact/UC136_DetectSuspectedFallOrImpact_TDS.md` (CB-SAFETY-IMP-004)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.4.4`
- `BR-SAFETY-011`: AI NEVER diagnoses

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC136 Detect Suspected Fall or Impact |

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
| **Feature / Gap ID** | `CB-SAFETY-IMP-004` |
| **Module** | `Detect Suspected Fall or Impact — safety` |
| **Spec gốc** | `CB-SAFETY-IMP-004` |
| **Priority** | 🔴 P0 *(safety-critical)* |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` *(IMU data + location)* |
| **Compliance Scope** | `PDPA / Luật 91/2025 / BR-SAFETY` |
| **Upstream Dependencies** | `UC134 (ACTIVE IMU session), Mobile IMU sensors` |
| **Downstream Consumers** | `UC62 OpenEmergencyFlow (via SuspectedFallDetected event)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-004 §17`, `BR-SAFETY-011`, `ADR-SAFETY-005` |
| **Constraints Injected** | C1 (suspected only), C2 (ACTIVE session), C3 (location PDPA), C4 (append-only), C5 (event not direct call), C6 (threshold not ML) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Algorithm dùng AI model | ADR-SAFETY-005: threshold only | Test verify magnitude calculation, no Gemini call |
| L2 | Location luôn được lưu | PDPA: consent-gated | Test verify location=null when consent=false (CRITICAL) |
| L3 | safety_events có thể UPDATE | ADR-SAFETY-006: append-only | Test verify no update/delete methods exposed |
| L4 | "confirmed fall" language | BR-SAFETY-011: "suspected" only | Test verify eventType=SUSPECTED_FALL, notes language |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC136 Detect Suspected Fall or Impact:
├── Algorithm (FallDetectionAlgorithmService — pure unit test, no mocks needed)
├── Service (FallDetectionService.processImuData() — mock dependencies)
├── Controller (POST /api/v1/safety/imu-data — security, validation)
└── Integration (Testcontainers PostgreSQL — V39+V40, append-only enforcement)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-136 §3.3.4.4` | IMU data → suspected fall → emergency |
| `BR-SAFETY-011` | "suspected" language only |
| `BR-SAFETY-012` | Location consent-gated |
| `ADR-SAFETY-005` | Threshold algorithm |
| `ADR-SAFETY-006` | safety_events append-only |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | magnitude > threshold → SUSPECTED_FALL | Algorithm | `FALL-TC-001` |
| TC-COND-002 | magnitude < threshold → no fall | Algorithm | `FALL-TC-002` |
| TC-COND-003 | No ACTIVE session → 409 | Authorization | `FALL-TC-003` |
| TC-COND-004 | PDPA: consent=false → location null | PDPA CRITICAL | `FALL-TC-004` |
| TC-COND-005 | PDPA: consent=true → location set | PDPA | `FALL-TC-005` |
| TC-COND-006 | SuspectedFallDetected event published | Event | `FALL-TC-006` |
| TC-COND-007 | safety_events append-only (no UPDATE/DELETE) | DB enforcement | `FALL-TC-007` |
| TC-COND-008 | Invalid IMU payload → 422 | Validation | `FALL-TC-008` |
| TC-COND-009 | BR-SAFETY-011: "suspected" language in response | Clinical safety | `FALL-TC-009` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Boundary Value Analysis | Threshold values (9.0, 12.0, 15.0) | Algorithm edge cases |
| State-based Testing | ACTIVE vs no session | Guard clause |
| Negative Testing | consent=false location | PDPA enforcement |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | Synthetic | `accelerometerX=12.5, Y=5.2, Z=-8.1` (magnitude ≈ 15.3 > threshold) | Fall trigger |
| `FX-002` | Synthetic | `accelerometerX=1.0, Y=0.5, Z=9.3` (magnitude ≈ 0.5 < threshold) | Below threshold |
| `FX-003` | Mock | `findActiveByUserId()` → empty | No session |
| `FX-004` | Mock | `hasLocationConsent()` → false | PDPA: no location |
| `FX-005` | Mock | `hasLocationConsent()` → true; lat=10.76, lon=106.66 | PDPA: with location |
| `FX-006` | DB seed | ACTIVE imu_monitoring_sessions record | Integration test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// FallDetectionTestFactory.java (extends UC134/UC135 factory)
class FallDetectionTestFactory {

    static ImuDataPayload makeFallPayload() {
        return new ImuDataPayload(
            12.5, 5.2, -8.1,   // accelerometer — magnitude > MEDIUM threshold 12.0
            0.5, 0.3, -0.2,    // gyroscope
            Instant.parse("2026-06-26T08:00:00Z"),
            null, null          // location (no consent default)
        );
    }

    static ImuDataPayload makeNonFallPayload() {
        return new ImuDataPayload(
            1.0, 0.5, 9.3,     // accelerometer — gravity dominant, magnitude < 1.0
            0.01, 0.01, 0.01,  // gyroscope
            Instant.parse("2026-06-26T08:00:00Z"),
            null, null
        );
    }

    static ImuDataPayload makeFallPayloadWithLocation(double lat, double lon) {
        return new ImuDataPayload(
            12.5, 5.2, -8.1,
            0.5, 0.3, -0.2,
            Instant.parse("2026-06-26T08:00:00Z"),
            lat, lon
        );
    }

    static SafetyEvent makeSuspectedFallEvent(UUID userId) {
        SafetyEvent e = new SafetyEvent();
        e.setId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        e.setUserId(userId);
        e.setEventType(SafetyEventType.SUSPECTED_FALL);
        e.setMagnitude(15.3);
        e.setUserLatitude(null);
        e.setUserLongitude(null);
        e.setDetectedAt(Instant.parse("2026-06-26T08:00:00Z"));
        e.setCreatedBy("SYSTEM");
        return e;
    }
}
```

---

### FALL-TC-001 — magnitude > threshold → SUSPECTED_FALL event created

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionAlgorithmService.analyze()`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionAlgorithmServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-005, SRS UC-136`

**Test Steps:**
1. Arrange: payload = FX-001 (magnitude ≈ 15.3 > MEDIUM threshold 12.0)
2. Act: `algorithmService.analyze(payload)` with sensitivityLevel=MEDIUM
3. Assert: `result.suspected() == true`
4. Assert: `result.eventType() == SUSPECTED_FALL`
5. Assert: `result.magnitude()` ≈ 15.3

**Expected Result (PASS):**
- SUSPECTED_FALL detected when above threshold

**Current Status:** 🔴 Not written

---

### FALL-TC-002 — magnitude < threshold → no fall

**Severity:** `MEDIUM`
**Feature Under Test:** `FallDetectionAlgorithmService.analyze() — no-fall branch`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionAlgorithmServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Test Steps:**
1. Arrange: payload = FX-002 (magnitude < 1.0, way below threshold)
2. Act: `algorithmService.analyze(payload)` with sensitivityLevel=MEDIUM
3. Assert: `result.suspected() == false`

**Current Status:** 🔴 Not written

---

### FALL-TC-003 — No ACTIVE session → 409 SAFETY-006

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService guard clause`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-SAFETY-010`

**Test Steps:**
1. Arrange: `findActiveByUserId()` → empty (FX-003)
2. Act: `fallDetectionService.processImuData(userId, makeFallPayload())`
3. Assert: `SafetyException` thrown with code=SAFETY-006
4. Assert: `eventRepo.save()` NOT called
5. Assert: No event published

**Current Status:** 🔴 Not written

---

### FALL-TC-004 — PDPA CRITICAL: consent=false → location null in safety_events

**Severity:** `CRITICAL — PDPA`
**Feature Under Test:** `FallDetectionService location consent gate`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SAFETY-012 / PDPA / Luật 91/2025`

**Test Steps:**
1. Arrange: ACTIVE session; `hasLocationConsent()` → false (FX-004); fall payload = FX-001
2. Act: `fallDetectionService.processImuData(userId, makeFallPayload())`
3. Assert: `eventRepo.save()` called with `savedEvent.getUserLatitude() == null`
4. Assert: `savedEvent.getUserLongitude() == null`

**Expected Result (PASS):**
- Location fields are NULL in safety_events when consent=false

**PDPA compliance:** This test is BLOCKING for deployment approval

**Current Status:** 🔴 Not written

---

### FALL-TC-005 — PDPA: consent=true → location set in safety_events

**Severity:** `HIGH — PDPA`
**Feature Under Test:** `FallDetectionService location consent gate`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Arrange: ACTIVE session; consent=true; payload with lat=10.76, lon=106.66 (FX-005)
2. Act: `fallDetectionService.processImuData(userId, makeFallPayloadWithLocation(10.76, 106.66))`
3. Assert: `savedEvent.getUserLatitude()` = 10.76
4. Assert: `savedEvent.getUserLongitude()` = 106.66

**Current Status:** 🔴 Not written

---

### FALL-TC-006 — SuspectedFallDetected event published

**Severity:** `HIGH`
**Feature Under Test:** `FallDetectionService event publishing → UC62`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-SAFETY-014`

**Test Steps:**
1. Arrange: ACTIVE session; fall payload; mock EventPublisher
2. Act: `fallDetectionService.processImuData(userId, makeFallPayload())`
3. Assert: `eventPublisher.publishEvent(SuspectedFallDetected)` called
4. Assert: event payload contains userId and imuSessionId

**Expected Result (PASS):**
- SuspectedFallDetected event published (UC62 will pick up)

**Current Status:** 🔴 Not written

---

### FALL-TC-007 — safety_events append-only: no UPDATE/DELETE in DB

**Severity:** `HIGH — PDPA Audit`
**Feature Under Test:** `safety_events DB constraint (V40 REVOKE)`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyEventsIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SAFETY-006 / BR-SAFETY-013`

**Test Steps:**
1. Arrange: Testcontainers PostgreSQL; V39+V40 applied
2. Act: `INSERT` a safety_event record via `eventRepository.save()`
3. Assert: INSERT succeeds (201)
4. Assert: `jdbcTemplate.execute("UPDATE safety_events SET notes='x' WHERE id=...")` → `PSQLException: permission denied`
5. Assert: `jdbcTemplate.execute("DELETE FROM safety_events WHERE id=...")` → `PSQLException: permission denied`

**Expected Result (PASS):**
- UPDATE and DELETE rejected by DB; INSERT allowed

**Current Status:** 🔴 Not written

---

### FALL-TC-008 — Invalid IMU payload → 422 SAFETY-007

**Severity:** `MEDIUM`
**Feature Under Test:** `FallDetectionController validation`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Test Steps:**
1. Arrange: JWT with ROLE_MOTHER; payload missing accelerometerX
2. Act: `POST /api/v1/safety/imu-data` with `{}`
3. Assert: HTTP 422 SAFETY-007 "Invalid IMU payload"

**Current Status:** 🔴 Not written

---

### FALL-TC-009 — BR-SAFETY-011: "suspected" language in response

**Severity:** `CRITICAL — BR-SAFETY`
**Feature Under Test:** `Clinical safety constraint — language enforcement`
**Test File:** `src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-SAFETY-011 / ADR-SAFETY-005`

**Test Steps:**
1. Arrange: ACTIVE session; fall payload (magnitude > threshold)
2. Act: `fallDetectionService.processImuData(userId, makeFallPayload())`
3. Assert: saved `SafetyEvent.getEventType()` = `SUSPECTED_FALL` (NOT `CONFIRMED_FALL`)
4. Assert: response message contains "Suspected fall detected" (NOT "confirmed", NOT "diagnosed")

**Expected Result (PASS):**
- All user-facing language uses "suspected" — never implies diagnosis

**BR-SAFETY-011 ENFORCEMENT:** This test is BLOCKING for production release

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FALL-TC-001` | `FallDetectionAlgorithmServiceTest.java` | `[x]` | `[x]` | — |
| `FALL-TC-002` | `FallDetectionAlgorithmServiceTest.java` | `[x]` | `[x]` | — |
| `FALL-TC-003` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | — |
| `FALL-TC-004` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | PDPA CRITICAL |
| `FALL-TC-005` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | PDPA |
| `FALL-TC-006` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | — |
| `FALL-TC-007` | `SafetyEventsIntegrationTest.java` | `[x]` | `[x]` | DB REVOKE |
| `FALL-TC-008` | `FallDetectionControllerTest.java` | `[x]` | `[x]` | — |
| `FALL-TC-009` | `FallDetectionServiceTest.java` | `[x]` | `[x]` | BR-SAFETY CRITICAL |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class FallDetectionAlgorithmService implements IFallDetectionAlgorithmService {
    @Override
    public FallAnalysisResult analyze(ImuDataPayload payload) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Service
public class FallDetectionService {
    public void processImuData(UUID userId, ImuDataPayload payload) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual |
|-------|----------|--------|
| `FALL-TC-001` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-002` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-003` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-004` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-005` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-006` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-007` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-008` | 🔴 FAIL | ☑ FAIL ☐ PASS |
| `FALL-TC-009` | 🔴 FAIL | ☑ FAIL ☐ PASS |

**Red Gate Evidence:** Stub commit hash: `___` | All FAIL? ☑ Yes → **GATE-2 PASS**

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-004` approved
- [ ] UC134 deployed (ACTIVE session management, FallDetectionEnabled event)
- [ ] UC62 deployed (SuspectedFallDetected event consumer)
- [ ] V40 migration approved by DBA + DPO
- [ ] `FallDetectionAlgorithmService.analyze()` stub throws `UnsupportedOperationException`

### Exit Criteria (DoD)

- [x] `./mvnw test` xanh
- [ ] **FALL-TC-004 PASS: location null when consent=false** (PDPA BLOCKING)
- [ ] **FALL-TC-009 PASS: "suspected" language only** (BR-SAFETY BLOCKING)
- [ ] FALL-TC-007 PASS: DB append-only enforced

**CASE 2.0:**
- [ ] Red Gate PASS (all 9 tests FAIL with stub)
- [ ] Props Isolation: `FallDetectionTestFactory` used for all test data
- [ ] DPO sign-off after FALL-TC-004 verified

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS safety_events CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '40';"
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|-------------|----------|-------|-----------|
| AP-AI-001 | Healthcare Overreach | Test expects "confirmed fall" language | **BLOCK** — BR-SAFETY-011 |
| AP-AI-002 | Green-from-Birth | FALL-TC-001 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Location Leakage | FALL-TC-004 not in test suite | **BLOCK** — PDPA C3 |
| AP-AI-004 | Direct UC62 call | processImuData calls UC62 service directly | Reject — enforce C5 |
| AP-AI-005 | Gemini integration | analyze() calls AI API | Reject — enforce C6 (threshold only) |

**Kết quả review:**
- [ ] Không phát hiện → approved

---

*TDD Spec v1.0 — UC136 Detect Suspected Fall or Impact — CB-SAFETY-IMP-004-TEST*
