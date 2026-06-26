# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC133 — Configure Safety Monitoring

**Document ID:** `CB-SAFETY-IMP-001-TEST`
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
- `04_Implement/UC133_ConfigureSafetyMonitoring/UC133_ConfigureSafetyMonitoring_TDS.md` (CB-SAFETY-IMP-001)
- `02_Requirements/SRS/Functional_Specifications.md §3.3.4.1`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC133 Configure Safety Monitoring |

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
| **Feature / Gap ID** | `CB-SAFETY-IMP-001` |
| **Module** | `Configure Safety Monitoring — safety` |
| **Spec gốc** | `CB-SAFETY-IMP-001` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT), User Profile` |
| **Downstream Consumers** | `UC134 EnableFallDetection, UC135 DisableFallDetection` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-001 §17`, `ADR-SAFETY-001/002` |
| **Constraints Injected** | C1 (upsert), C2 (event publish), C3 (userId from JWT), C4 (GET default), C5 (enum validation) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | GET trả 404 nếu chưa có config | ADR-SAFETY-001: trả default | Test verify GET returns default when no record |
| L2 | Tạo nhiều config records per user | ADR-SAFETY-001: unique constraint | Test verify 2 PUT = 1 record |
| L3 | SafetyConfigChanged event không mentioned | ADR-SAFETY-002: required | Test verify event published |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC133 Configure Safety Monitoring:
├── Service (SafetyConfigService — mock Repository và EventPublisher)
├── Controller (SafetyConfigController — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — verify upsert behavior)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-133 §3.3.4.1` | Mother configure safety monitoring |
| `ADR-SAFETY-001` | Upsert pattern — 1 record per user |
| `ADR-SAFETY-002` | SafetyConfigChanged event |
| `BR-SAFETY-002` | 1 config per user |
| `BR-SAFETY-003/004` | fallDetection=true/false → trigger UC134/UC135 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | First config → INSERT | `upsertByUserId()` | `SCONFIG-TC-001` |
| TC-COND-002 | Update config → UPDATE (no duplicate) | Upsert | `SCONFIG-TC-002` |
| TC-COND-003 | SafetyConfigChanged event published | Event | `SCONFIG-TC-003` |
| TC-COND-004 | GET when no config → return default | Default behavior | `SCONFIG-TC-004` |
| TC-COND-005 | sensitivityLevel invalid → 400 | Validation | `SCONFIG-TC-005` |
| TC-COND-006 | Wrong role → 403 | RBAC | `SCONFIG-TC-006` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | sensitivityLevel (LOW/MEDIUM/HIGH/INVALID) | Cover valid/invalid |
| State Transition | fallDetectionEnabled true/false | Config state |
| Error Guessing | 2nd PUT with same user | Upsert behavior |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Mock | `findByUserId()` → empty | First config test |
| `FX-002` | DB seed | `safety_monitoring_config {user_id: 'user-001', fallDetectionEnabled: false}` | Update test |
| `FX-003` | JWT | `{sub: 'user-001', roles: ['ROLE_MOTHER']}` | Mother token |
| `FX-004` | JWT | `{sub: 'user-002', roles: ['ROLE_PARTNER']}` | Wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// SafetyConfigTestFactory.java
class SafetyConfigTestFactory {

    static SafetyMonitoringConfig makeConfig() {
        SafetyMonitoringConfig c = new SafetyMonitoringConfig();
        c.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        c.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        c.setFallDetectionEnabled(false);
        c.setSensitivityLevel("MEDIUM");
        c.setEmergencyAutoAlert(true);
        c.setUpdatedAt(Instant.parse("2026-06-26T08:00:00Z"));
        return c;
    }

    static SafetyConfigRequest makeRequest() {
        SafetyConfigRequest req = new SafetyConfigRequest();
        req.setFallDetectionEnabled(true);
        req.setSensitivityLevel("MEDIUM");
        req.setEmergencyAutoAlert(true);
        return req;
    }
}
```

---

### SCONFIG-TC-001 — First config: INSERT new record

**Severity:** `HIGH`
**Feature Under Test:** `SafetyConfigService.configure()`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-001 / BR-SAFETY-002`

**Test Steps:**
1. Arrange: mock `findByUserId()` → empty; mock `save()` → FX-001-like config
2. Act: `safetyConfigService.configure(makeRequest(), userId)`
3. Assert: `save()` called once; result.fallDetectionEnabled = true

**Expected Result (PASS):**
- New config saved; fallDetectionEnabled = true

**Current Status:** 🔴 Not written

---

### SCONFIG-TC-002 — Update: no duplicate record

**Severity:** `HIGH`
**Feature Under Test:** `SafetyConfigService — upsert`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-001`

**Test Steps:**
1. Arrange: Testcontainers; seed FX-002 (existing config)
2. Act: PUT /api/v1/safety/config với fallDetectionEnabled=true
3. Assert: `count(*) FROM safety_monitoring_config WHERE user_id='user-001'` = 1
4. Assert: updated config has fallDetectionEnabled = true

**Expected Result (PASS):**
- Still exactly 1 record per user

**Current Status:** 🔴 Not written

---

### SCONFIG-TC-003 — SafetyConfigChanged event published

**Severity:** `HIGH`
**Feature Under Test:** `SafetyConfigService event publishing`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-002`

**Test Steps:**
1. Arrange: mock EventPublisher
2. Act: `safetyConfigService.configure(makeRequest(), userId)`
3. Assert: `eventPublisher.publishEvent()` called with `SafetyConfigChanged`

**Current Status:** 🔴 Not written

---

### SCONFIG-TC-004 — GET when no config → return default

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyConfigService.getConfig() — default behavior`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-SAFETY-IMP-001 §8.1 ISafetyConfigService.getConfig()`

**Test Steps:**
1. Arrange: `findByUserId()` → empty
2. Act: `safetyConfigService.getConfig(userId)`
3. Assert: returns `SafetyConfigResponse` with defaults (fallDetectionEnabled=false, sensitivityLevel=MEDIUM)

**Expected Result (PASS):**
- Default config returned (not 404)

**Current Status:** 🔴 Not written

---

### SCONFIG-TC-005 — Invalid sensitivityLevel → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyConfigController validation`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. PUT body: `{sensitivityLevel: "INVALID"}`
2. Assert: HTTP 400; error.code = "SAFETY-001"

**Current Status:** 🔴 Not written

---

### SCONFIG-TC-006 — Wrong role → 403 SAFETY-004

**Severity:** `HIGH`
**Feature Under Test:** `RBAC`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. JWT: FX-004 (ROLE_PARTNER)
2. PUT /api/v1/safety/config
3. Assert: HTTP 403; error.code = "SAFETY-004"

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SCONFIG-TC-001` | `SafetyConfigServiceTest.java` | `[ ]` | `—` | — |
| `SCONFIG-TC-002` | `SafetyConfigIntegrationTest.java` | `[ ]` | `—` | — |
| `SCONFIG-TC-003` | `SafetyConfigServiceTest.java` | `[ ]` | `—` | — |
| `SCONFIG-TC-004` | `SafetyConfigServiceTest.java` | `[ ]` | `—` | — |
| `SCONFIG-TC-005` | `SafetyConfigControllerTest.java` | `[ ]` | `—` | — |
| `SCONFIG-TC-006` | `SafetyConfigControllerTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
@Service
public class SafetyConfigService implements ISafetyConfigService {
    @Override
    public SafetyConfigResponse configure(SafetyConfigRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public SafetyConfigResponse getConfig(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual |
|-------|----------|--------|
| `SCONFIG-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `SCONFIG-TC-002` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `SCONFIG-TC-003` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `SCONFIG-TC-004` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `SCONFIG-TC-005` | 🔴 FAIL | ☐ FAIL ☐ PASS |
| `SCONFIG-TC-006` | 🔴 FAIL | ☐ FAIL ☐ PASS |

**Red Gate Evidence:** Stub commit hash: `___` | All FAIL? ☐ Yes → **GATE-2 PASS**

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-001` approved
- [ ] Migration V38 approved and chạy trên staging

### Exit Criteria (DoD)

- [ ] `./mvnw test` xanh
- [ ] `./mvnw verify` xanh (Testcontainers)
- [ ] SCONFIG-TC-002 PASS: 1 record per user verified
- [ ] SCONFIG-TC-003 PASS: event published

**CASE 2.0:**
- [ ] Red Gate PASS
- [ ] `./mvnw compile` no errors
- [ ] Props Isolation: `SafetyConfigTestFactory` used

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS safety_monitoring_config CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '38';"
git checkout -- src/main/java/com/carebridge/backend/safety/
git checkout -- src/main/resources/db/migration/V38__create_safety_monitoring_config.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|-------------|----------|-------|-----------|
| AP-AI-001 | Unconstrained Gen | TC không test upsert behavior | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code INSERT instead of upsert | ☐ | G-1 |
| AP-AI-005 | Hallucinated Contract | Test import SafetyConfigDAO not in §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện → approved

---

*TDD Spec v1.0 — UC133 Configure Safety Monitoring — CB-SAFETY-IMP-001-TEST*
