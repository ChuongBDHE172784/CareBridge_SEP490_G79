# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-133: Configure Safety Monitoring

**Document ID:** `CB-SAFETY-TEST-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — System Architect`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `CB-SAFETY-IMP-001` — TDS for UC-133 Configure Safety Monitoring
- `01_Requirements/SRS.md` — SRS 3.3.4.1
- `V1__init_schema.sql` — safety_monitoring_settings table
- `V38__add_safety_monitoring_config_columns.sql` — schema evolution

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) -> chạy -> xác nhận FAIL -> implement -> PASS -> refactor.
> Không mark test là PASS nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — System Architect | Khởi tạo tài liệu — TDD spec cho UC-133 Configure Safety Monitoring |

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
| **Feature / Gap ID** | `UC-133` |
| **Module** | `Configure Safety Monitoring — safety` |
| **Spec gốc** | `CB-SAFETY-IMP-001` |
| **Priority** | Critical |
| **Sprint** | `S4 (2026-06-23 -> 2026-07-04)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `GDPR Art. 7 (Consent), Art. 32 (Security)` |
| **Upstream Dependencies** | `identity (JWT), caregroup (care_group_members), audit (AuditService)` |
| **Downstream Consumers** | `UC-134 EnableFallDetection, UC-136 DetectSuspectedFallOrImpact` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-001 section 17, ADR-001, ADR-002, ADR-003` |
| **Constraints Injected** | C1: Upsert pattern, C2: Recipient validation, C3: countdownSeconds range, C4: JWT identity, C5: Layer separation, C6: Audit logging, C7: consentGranted gate |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC spec says table name `safety_monitoring_config` | V1 schema uses `safety_monitoring_settings` | Tests use `safety_monitoring_settings` table name; entity maps to it |
| L2 | UC spec says field `consentGranted` directly in table | V1 has `sensor_consent_at` (timestamp) not boolean | V38 migration adds `consent_granted` boolean column; tests verify both fields sync |
| L3 | UC spec says `alertRecipientIds` is a list | V1 has only `emergency_contact_user_id` (single UUID) | V38 adds `alert_recipient_ids UUID[]` array column; tests verify array storage |
| L4 | UC spec references `accountId` | V1 schema uses `user_id` as FK | Tests use `user_id` column name; service accepts `accountId` parameter mapped to `user_id` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-133 Configure Safety Monitoring bao gồm các layer:
├── Domain (SafetyMonitoringConfig entity — field validation)
├── Policy (SafetyRecipientPolicy — recipient eligibility check)
├── Service (SafetyMonitoringConfigServiceImpl — upsert logic + audit)
├── Controller (SafetyMonitoringConfigController — @WebMvcTest, validation + mapping)
└── Integration (Testcontainers PostgreSQL — full upsert flow with DB)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS 3.3.4.1` | Configure consent, recipients, countdown, location sharing, active hours |
| `ADR-001` | Upsert pattern — INSERT ON CONFLICT UPDATE |
| `ADR-002` | Alert recipients must be ACCEPTED care group members |
| `BR-SAFETY-001` | consentGranted must be true before fall detection can be enabled |
| `BR-SAFETY-003` | countdownSeconds range 10-60, default 30 |
| `BR-SAFETY-005` | Audit log every config change |
| `GDPR Art. 7` | Consent timestamp tracking |
| `CB-SAFETY-IMP-001 section 8` | Service interface contract, DTO validation rules |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Upsert creates new config when none exists | `SafetyMonitoringConfigServiceImpl.upsertConfig()` | `SAFETY-TC-001` |
| TC-COND-002 | Upsert updates existing config | `SafetyMonitoringConfigServiceImpl.upsertConfig()` | `SAFETY-TC-002` |
| TC-COND-003 | Invalid recipients rejected | `SafetyRecipientPolicy.validateRecipients()` | `SAFETY-TC-003` |
| TC-COND-004 | countdownSeconds below minimum rejected | `ConfigureSafetyMonitoringRequest` validation | `SAFETY-TC-004` |
| TC-COND-005 | countdownSeconds above maximum rejected | `ConfigureSafetyMonitoringRequest` validation | `SAFETY-TC-005` |
| TC-COND-006 | Default countdownSeconds applied when null | `SafetyMonitoringConfigServiceImpl.upsertConfig()` | `SAFETY-TC-006` |
| TC-COND-007 | GET returns existing config | `SafetyMonitoringConfigServiceImpl.getConfig()` | `SAFETY-TC-007` |
| TC-COND-008 | GET returns 404 when no config exists | `SafetyMonitoringConfigServiceImpl.getConfig()` | `SAFETY-TC-008` |
| TC-COND-009 | Non-MOTHER role denied access | `@PreAuthorize` on controller | `SAFETY-TC-009` |
| TC-COND-010 | Audit log emitted on config change | `AuditService.log()` | `SAFETY-TC-010` |
| TC-COND-011 | consentGranted syncs sensor_consent_at | `SafetyMonitoringConfigServiceImpl.upsertConfig()` | `SAFETY-TC-011` |
| TC-COND-012 | Empty alertRecipientIds is valid | `SafetyRecipientPolicy.validateRecipients()` | `SAFETY-TC-012` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | countdownSeconds (valid: 10-60, invalid: <10, >60) | Three partitions: below-min, valid-range, above-max |
| Boundary Value Analysis | countdownSeconds boundary (9, 10, 60, 61) | Validate exact boundaries |
| State Transition Testing | consentGranted (false->true, true->false) | Verify sensor_consent_at sync |
| Error Guessing | SQL injection in activeHoursStart, non-existent recipientIds | Security edge cases |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ userId: "mother-001-uuid", role: "MOTHER" }` | Authenticated mother for all tests |
| `FX-002` | DB seed | `{ careGroupId: "cg-001", ownerUserId: "mother-001-uuid" }` | Care group owned by mother |
| `FX-003` | DB seed | `{ careGroupMemberId: "cgm-001", careGroupId: "cg-001", userId: "member-001-uuid", invitationStatus: "ACCEPTED" }` | Valid alert recipient |
| `FX-004` | DB seed | `{ careGroupMemberId: "cgm-002", careGroupId: "cg-001", userId: "member-002-uuid", invitationStatus: "PENDING" }` | Invalid recipient (not ACCEPTED) |
| `FX-005` | JWT | `{ sub: "mother-001-uuid", role: "MOTHER" }` | Auth context for mother |
| `FX-006` | JWT | `{ sub: "expert-001-uuid", role: "EXPERT" }` | Auth context for non-mother |
| `FX-007` | DB seed | `{ settingId: "config-001-uuid", userId: "mother-001-uuid", consentGranted: true, countdownSeconds: 30 }` | Pre-existing config for update tests |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// SafetyMonitoringConfigTestFactory.java
class SafetyMonitoringConfigTestFactory {

    // Baseline valid entity — synced with FX-007
    static SafetyMonitoringConfig makeConfig() {
        SafetyMonitoringConfig config = new SafetyMonitoringConfig();
        config.setSettingId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        config.setUserId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
        config.setConsentGranted(true);
        config.setIsEnabled(false);
        config.setCountdownSeconds(30);
        config.setLocationSharingEnabled(false);
        config.setAlertRecipientIds(List.of());
        config.setActiveHoursStart(null);
        config.setActiveHoursEnd(null);
        config.setCreatedAt(Instant.parse("2026-06-26T00:00:00Z"));
        config.setUpdatedAt(Instant.parse("2026-06-26T00:00:00Z"));
        return config;
    }

    static SafetyMonitoringConfig makeConfig(Consumer<SafetyMonitoringConfig> overrides) {
        SafetyMonitoringConfig config = makeConfig();
        overrides.accept(config);
        return config;
    }

    static ConfigureSafetyMonitoringRequest makeRequest() {
        ConfigureSafetyMonitoringRequest request = new ConfigureSafetyMonitoringRequest();
        request.setConsentGranted(true);
        request.setCountdownSeconds(30);
        request.setShareLocationInEmergency(false);
        request.setAlertRecipientIds(List.of());
        return request;
    }

    static ConfigureSafetyMonitoringRequest makeRequest(Consumer<ConfigureSafetyMonitoringRequest> overrides) {
        ConfigureSafetyMonitoringRequest request = makeRequest();
        overrides.accept(request);
        return request;
    }

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID NON_MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");
}
```

---

### SAFETY-TC-001 — Upsert Creates New Config (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.upsertConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-001 (upsert pattern), BR-SAFETY-001 (consent required)`

**Preconditions:**
- No existing safety_monitoring_settings record for MOTHER_ID
- SafetyRecipientPolicy mock returns valid for empty recipients
- AuditService mock available
- Fixtures: FX-001

**Test Steps:**
1. Arrange: Mock configRepository.findByUserId(MOTHER_ID) returns Optional.empty()
2. Arrange: Mock configRepository.save() returns entity with generated settingId
3. Act: Call service.upsertConfig(MOTHER_ID, makeRequest())
4. Assert: configRepository.save() called exactly once
5. Assert: Saved entity has userId = MOTHER_ID, consentGranted = true, countdownSeconds = 30

**Expected Result (PASS):**
- Response contains settingId (non-null UUID)
- Response.consentGranted = true
- Response.countdownSeconds = 30
- configRepository.save() called once

**Expected Result (FAIL):**
- NullPointerException if upsert logic not implemented
- Response missing fields

**Current Status:** RED Not written

---

### SAFETY-TC-002 — Upsert Updates Existing Config

**Severity:** `HIGH`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.upsertConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-001 (upsert pattern)`

**Preconditions:**
- Existing safety_monitoring_settings record for MOTHER_ID with countdownSeconds=30
- Fixtures: FX-001, FX-007

**Test Steps:**
1. Arrange: Mock configRepository.findByUserId(MOTHER_ID) returns existing config (countdownSeconds=30)
2. Act: Call service.upsertConfig(MOTHER_ID, makeRequest(r -> r.setCountdownSeconds(45)))
3. Assert: configRepository.save() called with entity where countdownSeconds=45
4. Assert: settingId unchanged (same entity updated, not new record)

**Expected Result (PASS):**
- Response.countdownSeconds = 45
- Same settingId as existing record
- updatedAt > original updatedAt

**Expected Result (FAIL):**
- New record created instead of update (duplicate)
- countdownSeconds not changed

**Current Status:** RED Not written

---

### SAFETY-TC-003 — Invalid Alert Recipients Rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyRecipientPolicy.validateRecipients()`
**Test File:** `src/test/java/com/carebridge/backend/safety/policy/SafetyRecipientPolicyTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-002, BR-SAFETY-002`

**Preconditions:**
- Care group owned by MOTHER_ID has ACCEPTED member MEMBER_ID
- NON_MEMBER_ID is NOT in any care group
- Fixtures: FX-002, FX-003

**Test Steps:**
1. Arrange: Mock careGroupRepository returns groups owned by MOTHER_ID
2. Arrange: Mock careGroupMemberRepository returns [MEMBER_ID] as accepted members
3. Act: Call policy.validateRecipients(MOTHER_ID, List.of(NON_MEMBER_ID))
4. Assert: SafetyException thrown with code "SAFETY-001"

**Expected Result (PASS):**
- SafetyException thrown with code "SAFETY-001"
- Exception message contains NON_MEMBER_ID

**Expected Result (FAIL):**
- No exception thrown (validation bypassed)

**Current Status:** RED Not written

---

### SAFETY-TC-004 — countdownSeconds Below Minimum

**Severity:** `MEDIUM`
**Feature Under Test:** `ConfigureSafetyMonitoringRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyMonitoringConfigControllerTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SAFETY-003`

**Preconditions:**
- Valid JWT for MOTHER role
- Fixtures: FX-005

**Test Steps:**
1. Arrange: Create request with countdownSeconds = 9
2. Act: PUT /api/v1/safety/monitoring-config with request body
3. Assert: Response status 400
4. Assert: Error references countdownSeconds validation

**Expected Result (PASS):**
- HTTP 400 returned
- Error message mentions countdownSeconds min constraint

**Expected Result (FAIL):**
- HTTP 200 returned (validation bypassed)

**Current Status:** RED Not written

---

### SAFETY-TC-005 — countdownSeconds Above Maximum

**Severity:** `MEDIUM`
**Feature Under Test:** `ConfigureSafetyMonitoringRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyMonitoringConfigControllerTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SAFETY-003`

**Preconditions:**
- Valid JWT for MOTHER role
- Fixtures: FX-005

**Test Steps:**
1. Arrange: Create request with countdownSeconds = 61
2. Act: PUT /api/v1/safety/monitoring-config with request body
3. Assert: Response status 400

**Expected Result (PASS):**
- HTTP 400 returned

**Expected Result (FAIL):**
- HTTP 200 returned (validation bypassed)

**Current Status:** RED Not written

---

### SAFETY-TC-006 — Default countdownSeconds When Null

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.upsertConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-SAFETY-003 (default 30)`

**Preconditions:**
- No existing config
- Fixtures: FX-001

**Test Steps:**
1. Arrange: Create request with countdownSeconds = null
2. Act: Call service.upsertConfig(MOTHER_ID, request)
3. Assert: Saved entity has countdownSeconds = 30 (default)

**Expected Result (PASS):**
- Response.countdownSeconds = 30

**Expected Result (FAIL):**
- NullPointerException or countdownSeconds = 0

**Current Status:** RED Not written

---

### SAFETY-TC-007 — GET Returns Existing Config

**Severity:** `HIGH`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.getConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `US-SAFETY-002`

**Preconditions:**
- Existing config record for MOTHER_ID
- Fixtures: FX-007

**Test Steps:**
1. Arrange: Mock configRepository.findByUserId(MOTHER_ID) returns existing config
2. Act: Call service.getConfig(MOTHER_ID)
3. Assert: Response matches stored config values

**Expected Result (PASS):**
- Response.settingId matches stored value
- Response.consentGranted, countdownSeconds match stored values

**Expected Result (FAIL):**
- Wrong values returned or exception thrown

**Current Status:** RED Not written

---

### SAFETY-TC-008 — GET Returns 404 When No Config

**Severity:** `HIGH`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.getConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-SAFETY-IMP-001 section 10 (SAFETY-003)`

**Preconditions:**
- No config record for MOTHER_ID

**Test Steps:**
1. Arrange: Mock configRepository.findByUserId(MOTHER_ID) returns Optional.empty()
2. Act: Call service.getConfig(MOTHER_ID)
3. Assert: SafetyException thrown with code "SAFETY-003"

**Expected Result (PASS):**
- SafetyException thrown with code "SAFETY-003"

**Expected Result (FAIL):**
- Null returned instead of exception

**Current Status:** RED Not written

---

### SAFETY-TC-009 — Non-MOTHER Role Denied Access

**Severity:** `CRITICAL`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `SafetyMonitoringConfigController` @PreAuthorize
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyMonitoringConfigControllerTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-SAFETY-IMP-001 section 16 Auth Matrix`

**Preconditions:**
- Valid JWT for EXPERT role
- Fixtures: FX-006

**Test Steps:**
1. Arrange: Create MockMvc with EXPERT role JWT
2. Act: PUT /api/v1/safety/monitoring-config
3. Assert: Response status 403

**Expected Result (PASS):**
- HTTP 403 Forbidden

**Expected Result (FAIL):**
- HTTP 200 (authorization bypassed)

**Current Status:** RED Not written

---

### SAFETY-TC-010 — Audit Log Emitted on Config Change

**Severity:** `HIGH`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.upsertConfig()` audit emission
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-SAFETY-005, GDPR Art. 5.1(e)`

**Preconditions:**
- AuditService mock
- Fixtures: FX-001

**Test Steps:**
1. Arrange: Mock AuditService.log()
2. Act: Call service.upsertConfig(MOTHER_ID, makeRequest())
3. Assert: AuditService.log() called exactly once with event type "SafetyConfigUpdated"

**Expected Result (PASS):**
- AuditService.log() called with correct event type and userId

**Expected Result (FAIL):**
- AuditService.log() never called

**Current Status:** RED Not written

---

### SAFETY-TC-011 — consentGranted Syncs sensor_consent_at

**Severity:** `HIGH`
**Feature Under Test:** `SafetyMonitoringConfigServiceImpl.upsertConfig()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyMonitoringConfigServiceImplTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-SAFETY-001, GDPR Art. 7`

**Preconditions:**
- Existing config with consentGranted=false, sensorConsentAt=null
- Fixtures: FX-007 modified

**Test Steps:**
1. Arrange: Existing config with consentGranted=false
2. Act: Call service.upsertConfig with consentGranted=true
3. Assert: Saved entity has sensorConsentAt set to a non-null Instant

**Expected Result (PASS):**
- sensorConsentAt is non-null after consent granted
- sensorConsentAt is within last few seconds

**Expected Result (FAIL):**
- sensorConsentAt remains null after consent granted

**Current Status:** RED Not written

---

### SAFETY-TC-012 — Empty alertRecipientIds Is Valid

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyRecipientPolicy.validateRecipients()`
**Test File:** `src/test/java/com/carebridge/backend/safety/policy/SafetyRecipientPolicyTest.java`
**TDD Phase:** RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-SAFETY-IMP-001 section 8.3`

**Preconditions:**
- None

**Test Steps:**
1. Act: Call policy.validateRecipients(MOTHER_ID, List.of())
2. Assert: No exception thrown

**Expected Result (PASS):**
- Method returns normally (no exception)

**Expected Result (FAIL):**
- Exception thrown for empty list

**Current Status:** RED Not written

---

### SECURITY TEST CASES

---

### SAFETY-TC-SEC-001 — SQL Injection in activeHoursStart

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `SafetyMonitoringConfigController` input validation
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyMonitoringConfigControllerTest.java`
**TDD Phase:** RED

**Preconditions:**
- Valid MOTHER JWT

**Test Steps (Attack Simulation):**
1. Arrange: Create request with activeHoursStart = "'; DROP TABLE safety_monitoring_settings; --"
2. Act: PUT /api/v1/safety/monitoring-config with malicious payload
3. Assert: Response status 400 (validation fails on @Pattern)

**Expected Result (PASS = safe):**
- HTTP 400 returned
- Table safety_monitoring_settings still exists

**Expected Result (FAIL = vulnerability):**
- HTTP 200 returned or table dropped

**Current Status:** RED Not written

---

### SAFETY-TC-SEC-002 — Unauthenticated Access

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyMonitoringConfigControllerTest.java`
**TDD Phase:** RED

**Preconditions:**
- No JWT token provided

**Test Steps:**
1. Act: PUT /api/v1/safety/monitoring-config without Authorization header
2. Assert: Response status 401

**Expected Result (PASS = safe):**
- HTTP 401 Unauthorized

**Expected Result (FAIL = vulnerability):**
- HTTP 200 (endpoint accessible without auth)

**Current Status:** RED Not written

---

### INTEGRATION TEST CASES

---

### SAFETY-TC-INT-001 — Full Upsert Flow with PostgreSQL

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyMonitoringConfigIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001, TC-COND-002`

**Preconditions:**
- PostgreSQL container running (@Testcontainers auto-start)
- Flyway migration V1+V38 applied automatically
- Seed: Mother user record, care group with ACCEPTED member

**Test Steps:**
1. Seed mother user and care group with ACCEPTED member via JPA
2. Call PUT /api/v1/safety/monitoring-config via MockMvc with MOTHER JWT
3. Assert HTTP 200
4. Query safety_monitoring_settings via JdbcTemplate
5. Assert exactly 1 record with correct field values
6. Call PUT again with different countdownSeconds
7. Assert still exactly 1 record (upsert, not duplicate)
8. Assert countdownSeconds updated

**Expected Result (PASS):**
- Exactly 1 record in safety_monitoring_settings for the mother
- Fields match request values
- Second PUT updates same record

**Expected Result (FAIL):**
- Duplicate records created
- Field values wrong

**DB Assertion:**
```java
SafetyMonitoringConfig record = configRepository.findByUserId(motherId).orElseThrow();
assertThat(record).isNotNull();
assertThat(record.getConsentGranted()).isTrue();
assertThat(record.getCountdownSeconds()).isEqualTo(30);
assertThat(record.getAlertRecipientIds()).containsExactly(memberId);
```

**Current Status:** RED Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|-----------------|-----------------|
| `SAFETY-TC-001` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-002` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-003` | `SafetyRecipientPolicyTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-004` | `SafetyMonitoringConfigControllerTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-005` | `SafetyMonitoringConfigControllerTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-006` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-007` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-008` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-009` | `SafetyMonitoringConfigControllerTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-010` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-011` | `SafetyMonitoringConfigServiceImplTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-012` | `SafetyRecipientPolicyTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-SEC-001` | `SafetyMonitoringConfigControllerTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-SEC-002` | `SafetyMonitoringConfigControllerTest.java` | `[ ]` | `[hash]` | |
| `SAFETY-TC-INT-001` | `SafetyMonitoringConfigIntegrationTest.java` | `[ ]` | `[hash]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SafetyMonitoringConfigServiceImpl implements SafetyMonitoringConfigService {

    @Override
    public SafetyMonitoringConfigResponse upsertConfig(UUID accountId, ConfigureSafetyMonitoringRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SafetyMonitoringConfigResponse getConfig(UUID accountId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SAFETY-TC-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-006` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-007` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-008` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-009` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-010` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-011` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-012` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-SEC-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-SEC-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `SAFETY-TC-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- All FAIL? [ ] Yes -> GATE-2 PASS (T2->T3) -> proceed to implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] Flyway migration `V38__add_safety_monitoring_config_columns.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho SafetyMonitoringConfigServiceImpl
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Upsert pattern verified — no duplicate config records
- [ ] Recipient validation verified — only ACCEPTED members accepted
- [ ] Audit log emitted for every config change

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (section 5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria

- Blocker dependency chưa sẵn sàng (caregroup module, audit service)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration V38 (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE safety_monitoring_settings DROP COLUMN IF EXISTS consent_granted;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE safety_monitoring_settings DROP COLUMN IF EXISTS alert_recipient_ids;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE safety_monitoring_settings DROP COLUMN IF EXISTS active_hours_start;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE safety_monitoring_settings DROP COLUMN IF EXISTS active_hours_end;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '38';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/safety/
git checkout -- src/main/resources/db/migration/V38__add_safety_monitoring_config_columns.sql
git checkout -- src/test/java/com/carebridge/backend/safety/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | [x] All TCs reference ADR/BR | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (section 5.1) | [ ] Pending Red Gate | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | [x] All decisions have ADRs | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | [x] TC-009 verifies auth at controller, business logic in service | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | [ ] Pending implementation | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào -> TDD spec approved
- [ ] Phát hiện AP -> ghi vào bảng dưới -> fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| None detected | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
