# TEST SPECIFICATION — UC-157 Manage Privacy Settings
# Đặc tả Kiểm thử — Quản lý Cài đặt Riêng tư

| Field                  | Value                                           |
|------------------------|-------------------------------------------------|
| **Document ID**        | `CB-PRIV-TEST-001`                              |
| **Version**            | `1.0`                                           |
| **Date**               | `2026-06-26`                                    |
| **Status**             | `Approved`                                      |
| **Document Owner**     | `PhuongNT`                                      |
| **Author**             | `AI Agent`                                      |
| **Reviewed by**        | `[Tech Lead]`                                   |
| **DPO Sign-off**       | `[ ] Pending`                                   |
| **Approved by**        | `[Principal Architect]`                         |
| **Last Review**        | `2026-06-26`                                    |
| **Based on EDS**       | `v2.0`                                          |
| **TDS Reference**      | `CB-PRIV-IMP-001`                               |
| **Standard**           | `ISO/IEC/IEEE 29119-3:2021`                     |
| **Data Classification**| `Sensitive-PII`                                 |

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                    |
|------------|-----------------|------------------------------------------------------|
| 2026-06-26 | AI Agent        | Tạo Test-Spec lần đầu cho UC-157                     |

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

| Field                   | Value                                                         |
|-------------------------|---------------------------------------------------------------|
| **Feature / UC ID**     | `UC-157`                                                      |
| **Module**              | `privacy — ManagePrivacySettings`                            |
| **Spec gốc**            | `CB-PRIV-IMP-001`                                             |
| **Priority**            | 🟠 P1                                                         |
| **Data Classification** | `Sensitive-PII`                                               |
| **Compliance Scope**    | `GDPR Art. 7, Art. 25`                                       |
| **Upstream Dependencies**| `security (JWT), identity (User)`                           |
| **Downstream Consumers**| `AnalyticsService, AuditService`                             |

### 1.1 AI Generation Context (CASE 2.0)

| Field                  | Value                                                          |
|------------------------|----------------------------------------------------------------|
| **AI Assisted?**       | `Yes`                                                          |
| **Constraint Source**  | `CB-PRIV-IMP-001 §17`, `ADR-PRIV-001/002/003`                |
| **Constraints Injected**| C1 (lazy creation), C2 (ownership), C3 (consent withdrawal) |
| **Model**              | `claude-sonnet-4-6`                                            |
| **Trust Level**        | `T2 → T3 (pending Red Gate)`                                  |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                              | Thực tế (schema / policy)                           | Fix áp dụng trong test                               |
|---|-----------------------------------------------------|-----------------------------------------------------|------------------------------------------------------|
| L1 | Spec không đề cập lazy creation                   | ADR-PRIV-001: tạo record khi GET lần đầu            | Test phải verify record được tạo sau GET đầu tiên   |
| L2 | Spec không rõ partial update                       | null fields giữ nguyên giá trị cũ                  | Test PUT với 1 field → verify field khác không đổi  |
| L3 | Spec không rõ endpoint structure                   | `/api/v1/privacy-settings/me` — userId từ principal | Test phải dùng /me endpoint, không có userId in path |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
privacy.ManagePrivacySettings bao gồm:
├── Domain (PrivacySettings entity, ProfileVisibility enum)
├── Service Layer (PrivacySettingsService — mock repository)
├── Controller Layer (PrivacySettingsController — mock service)
└── Integration (Testcontainers PostgreSQL — full flow)
```

### TDS-02 — Test Basis

| Source           | Items Derived                                                     |
|------------------|-------------------------------------------------------------------|
| UC-157           | GET settings, PUT settings, lazy creation, ownership enforcement |
| ADR-PRIV-001     | Lazy creation với safe defaults                                   |
| ADR-PRIV-002     | Ownership check ở service layer                                   |
| ADR-PRIV-003     | Analytics consent withdrawal immediate effect                     |
| BR-PRIV-001..006 | All business rules cho privacy settings                           |
| GDPR Art. 25     | Privacy by Default — safe defaults enforcement                    |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID   | Test Condition                                          | Coverage Item                            | Test Cases          |
|----------------|---------------------------------------------------------|------------------------------------------|---------------------|
| TC-COND-P-001  | GET lần đầu → tạo record với safe defaults             | `PrivacySettingsService.getOrCreateDefault()` | `PRIV-TC-001`  |
| TC-COND-P-002  | GET khi đã có record → trả về existing                 | `PrivacySettingsService.getSettings()`   | `PRIV-TC-002`       |
| TC-COND-P-003  | PUT cập nhật hợp lệ → lưu DB + emit event              | `PrivacySettingsService.updateSettings()` | `PRIV-TC-003`      |
| TC-COND-P-004  | PUT profileVisibility không hợp lệ → 400               | `@Valid` annotation trên controller      | `PRIV-TC-004`       |
| TC-COND-P-005  | Không có JWT → 401                                     | Spring Security filter chain             | `PRIV-TC-005`       |
| TC-COND-P-006  | analyticsConsent withdrawal → event phát ra            | `PrivacySettingsUpdatedEvent`            | `PRIV-TC-006`       |
| TC-COND-P-007  | Partial update — field không gửi giữ nguyên            | `PrivacySettingsService.applyChanges()`  | `PRIV-TC-007`       |
| TC-COND-P-008  | Integration: full GET → PUT → DB verify                | Full stack                               | `PRIV-TC-INT-001`   |

### TDS-04 — Test Techniques

| Technique                  | Applied To                          | Rationale                              |
|----------------------------|-------------------------------------|----------------------------------------|
| Equivalence Partitioning   | profileVisibility enum values       | Valid (3 values) vs invalid            |
| Boundary Value Analysis    | boolean fields                      | true/false boundaries                  |
| State Transition Testing   | Settings lifecycle (null→created→updated) | Lazy creation state              |
| Error Guessing             | Ownership bypass attempt            | Security: /me enforces principal       |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                        | Mục đích                    |
|------------|----------|------------------------------------------------------|-----------------------------|
| `FX-P-001` | DB seed  | No row in privacy_settings for user-001             | Lazy creation test          |
| `FX-P-002` | DB seed  | `{userId: user-002, profileVisibility: FRIENDS_ONLY, analyticsConsent: false}` | Update test |
| `FX-P-003` | DB seed  | `{userId: user-003, analyticsConsent: true}`        | Consent withdrawal test     |
| `FX-P-004` | JWT      | `{sub: user-001, roles: [ROLE_MOTHER]}`             | Standard auth               |
| `FX-P-005` | JWT      | `{sub: user-004, roles: [ROLE_EXPERT]}`             | Multi-role test             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class PrivacySettingsTestFactory {
    static PrivacySettings makeValidSettings() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new PrivacySettings.PrivacySettingsBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static PrivacySettings makeValidSettings(Consumer<PrivacySettings> overrides) {
        var entity = makeValidSettings();
        overrides.accept(entity);
        return entity;
    }
}
```

> **TC ID format:** `PRIV-TC-[NNN]`
> **Data Classification:** Tất cả test data là `SYNTHETIC` — không dùng PII thật.

---

### PRIV-TC-001 — GET lần đầu tạo settings với safe defaults

**Severity:** `HIGH`
**Feature Under Test:** `PrivacySettingsService.getOrCreateDefault()`
**Test File:** `src/test/java/com/carebridge/backend/privacy/service/PrivacySettingsServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-001`
**Oracle Source:** `ADR-PRIV-001`, `BR-PRIV-001`, `BR-PRIV-006`

**Preconditions:**
- DB không có row `privacy_settings` cho user-001 (FX-P-001)
- user-001 có JWT hợp lệ với ROLE_MOTHER (FX-P-004)

**Test Steps:**
1. Arrange: mock `IPrivacySettingsRepository.findByUserId(user-001)` → `Optional.empty()`
2. Arrange: mock `repository.save(any())` → return saved entity
3. Act: `service.getSettings(user-001-uuid, principalUser001)`
4. Assert: verify `repository.save()` được gọi 1 lần với `profileVisibility = FRIENDS_ONLY`

**Expected Result (PASS):**
- `repository.save()` được gọi đúng 1 lần
- Returned DTO: `profileVisibility = "FRIENDS_ONLY"`, `locationSharingEnabled = false`, `analyticsConsent = false`

**Expected Result (FAIL):**
- Service không tạo record → NullPointerException hoặc trả về null

**Current Status:** 🔴 Not written

```gherkin
Feature: Privacy Settings — Lazy Creation
  Background:
    Given test data classification: SYNTHETIC

  Scenario: GET lần đầu tạo record với safe defaults
    Given user "user-001" chưa có privacy settings
    When service.getSettings() được gọi với principal user-001
    Then repository.save() được gọi 1 lần
    And settings.profileVisibility = FRIENDS_ONLY
    And settings.locationSharingEnabled = false
    And settings.analyticsConsent = false
```

---

### PRIV-TC-002 — GET khi đã có record trả về existing settings

**Severity:** `MEDIUM`
**Feature Under Test:** `PrivacySettingsService.getSettings()`
**Test File:** `src/test/java/com/carebridge/backend/privacy/service/PrivacySettingsServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-002`
**Oracle Source:** `ADR-PRIV-001`

**Preconditions:**
- DB có row cho user-002 với `profileVisibility = PRIVATE` (FX-P-002 modified)

**Test Steps:**
1. Arrange: mock `findByUserId(user-002)` → `Optional.of(existingSettings)`
2. Act: `service.getSettings(user-002-uuid, principalUser002)`
3. Assert: `repository.save()` KHÔNG được gọi; returned `profileVisibility = PRIVATE`

**Expected Result (PASS):**
- `repository.save()` call count = 0
- Returned DTO phản ánh existing settings

**Current Status:** 🔴 Not written

---

### PRIV-TC-003 — PUT cập nhật hợp lệ → lưu và emit event

**Severity:** `HIGH`
**Feature Under Test:** `PrivacySettingsService.updateSettings()`
**Test File:** `src/test/java/com/carebridge/backend/privacy/service/PrivacySettingsServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-003`
**Oracle Source:** `BR-PRIV-005`, `ADR-PRIV-001`

**Preconditions:**
- FX-P-002: user-002 có existing settings
- FX-P-004: JWT của user-002

**Test Steps:**
1. Arrange: mock repository để return existing settings cho user-002
2. Arrange: capture `ApplicationEventPublisher.publishEvent()` calls
3. Act: `service.updateSettings(user-002-uuid, request{profileVisibility: PRIVATE}, principal002)`
4. Assert: `repository.save()` gọi 1 lần; event `PrivacySettingsUpdatedEvent` được publish

**Expected Result (PASS):**
- `repository.save()` call count = 1 với `profileVisibility = PRIVATE`
- `eventPublisher.publishEvent()` call count = 1 với event type `PrivacySettingsUpdated`
- AuditService.log() được gọi

**Current Status:** 🔴 Not written

---

### PRIV-TC-004 — PUT profileVisibility không hợp lệ → 400

**Severity:** `HIGH`
**Feature Under Test:** `PrivacySettingsController.updateMySettings()` — @Valid
**Test File:** `src/test/java/com/carebridge/backend/privacy/controller/PrivacySettingsControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-004`
**Oracle Source:** `BR-PRIV-002`, error code `PRIV-001`

**Preconditions:**
- user-001 đã đăng nhập (FX-P-004)

**Test Steps:**
1. Arrange: MockMvc setup với Spring Security test support
2. Act: `PUT /api/v1/privacy-settings/me` với body `{"profileVisibility": "INVALID_ENUM"}`
3. Assert: HTTP 400; response body `error.code = "PRIV-001"`

**Expected Result (PASS):**
- HTTP status 400
- `service.updateSettings()` KHÔNG được gọi (validation fails before service)
- Error code `PRIV-001` trong response

**Expected Result (FAIL):**
- HTTP 200 hoặc 500 — validation không hoạt động

**Current Status:** 🔴 Not written

---

### PRIV-TC-005 — Không có JWT → 401

**Severity:** `CRITICAL`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/privacy/controller/PrivacySettingsControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-005`
**Oracle Source:** Spring Security JWT config

**Test Steps:**
1. Act: `GET /api/v1/privacy-settings/me` không có Authorization header
2. Assert: HTTP 401

**Expected Result (PASS):**
- HTTP 401 Unauthorized

**Current Status:** 🔴 Not written

---

### PRIV-TC-006 — Analytics consent withdrawal phát event với flag đúng

**Severity:** `CRITICAL`
**Feature Under Test:** `PrivacySettingsService.updateSettings()` — consent withdrawal
**Test File:** `src/test/java/com/carebridge/backend/privacy/service/PrivacySettingsServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-006`
**Oracle Source:** `ADR-PRIV-003`, `GDPR Art. 7.3`

**Preconditions:**
- FX-P-003: user-003 có `analyticsConsent = true` trong DB

**Test Steps:**
1. Arrange: mock repository để return settings với `analyticsConsent = true` cho user-003
2. Arrange: capture published event
3. Act: `service.updateSettings(user-003, {analyticsConsent: false}, principal003)`
4. Assert: event `analyticsConsentWithdrawn = true` trong PrivacySettingsUpdatedEvent

**Expected Result (PASS):**
- `PrivacySettingsUpdatedEvent.analyticsConsentWithdrawn() = true`
- Settings được lưu với `analyticsConsent = false`

**Expected Result (FAIL):**
- Event không có flag `analyticsConsentWithdrawn` hoặc flag = false — vi phạm GDPR Art. 7.3

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Rút lại analytics consent → event phát ra ngay
    Given user "user-003" có analyticsConsent = true
    When PUT /api/v1/privacy-settings/me với {analyticsConsent: false}
    Then HTTP 200 được trả về
    And PrivacySettingsUpdatedEvent được phát ra
    And event.analyticsConsentWithdrawn = true
    And DB: analytics_consent = false ngay lập tức
```

---

### PRIV-TC-007 — Partial update — field không gửi giữ nguyên

**Severity:** `MEDIUM`
**Feature Under Test:** `PrivacySettingsService.applyChanges()`
**Test File:** `src/test/java/com/carebridge/backend/privacy/service/PrivacySettingsServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-007`
**Oracle Source:** `BR-PRIV-001` — partial update semantics

**Preconditions:**
- user-002 có settings: `{profileVisibility: FRIENDS_ONLY, locationSharingEnabled: true, analyticsConsent: false}`

**Test Steps:**
1. Act: `service.updateSettings(user-002, {profileVisibility: "PRIVATE"}, principal002)`
   (chỉ gửi profileVisibility, không gửi locationSharingEnabled)
2. Assert: `locationSharingEnabled` vẫn là `true` sau update

**Expected Result (PASS):**
- `locationSharingEnabled = true` (không thay đổi)
- `profileVisibility = PRIVATE` (đã cập nhật)

**Current Status:** 🔴 Not written

---

### PRIV-TC-INT-001 — Integration: Full flow GET → PUT → DB verify

**Severity:** `HIGH`
**Feature Under Test:** Full stack: Controller → Service → Repository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/privacy/PrivacySettingsIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-P-008`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied (privacy_settings table exists)
- No row in privacy_settings for user-001
- JWT token cho user-001 (ROLE_MOTHER)

**Test Steps:**
1. `GET /api/v1/privacy-settings/me` với JWT user-001
2. Assert HTTP 200; `profileVisibility = "FRIENDS_ONLY"` (lazy created)
3. Verify DB: `SELECT COUNT(*) FROM privacy_settings WHERE user_id = 'user-001'` = 1
4. `PUT /api/v1/privacy-settings/me` với `{profileVisibility: "PRIVATE", dataExportOptOut: true}`
5. Assert HTTP 200
6. Verify DB: `SELECT profile_visibility FROM privacy_settings WHERE user_id = 'user-001'` = `PRIVATE`

**Expected Result (PASS):**
- Cả GET và PUT trả về 200
- DB sau GET: 1 row với safe defaults
- DB sau PUT: `profile_visibility = 'PRIVATE'`, `data_export_opt_out = true`

**Current Status:** 🔴 Not written

```gherkin
  Scenario: Integration — GET tạo defaults, PUT cập nhật, DB verify
    Given test data classification: SYNTHETIC
    And PostgreSQL Testcontainer đang chạy
    And không có row nào trong privacy_settings cho user-001
    When GET /api/v1/privacy-settings/me với JWT của user-001
    Then HTTP 200 và record được tạo với safe defaults
    When PUT /api/v1/privacy-settings/me với {profileVisibility: "PRIVATE"}
    Then HTTP 200
    And SELECT profile_visibility FROM privacy_settings WHERE user_id = 'user-001' = 'PRIVATE'
```

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                   | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note           |
|------------------|---------------------------------------------|-----------------|-------------------|-----------------------------|
| `PRIV-TC-001`    | `...service/PrivacySettingsServiceTest.java` | `[ ]`          | —                 | Extract `buildDefaults()`   |
| `PRIV-TC-002`    | `...service/PrivacySettingsServiceTest.java` | `[ ]`          | —                 | —                           |
| `PRIV-TC-003`    | `...service/PrivacySettingsServiceTest.java` | `[ ]`          | —                 | —                           |
| `PRIV-TC-004`    | `...controller/PrivacySettingsControllerTest.java` | `[ ]`    | —                 | —                           |
| `PRIV-TC-005`    | `...controller/PrivacySettingsControllerTest.java` | `[ ]`    | —                 | —                           |
| `PRIV-TC-006`    | `...service/PrivacySettingsServiceTest.java` | `[ ]`          | —                 | Extract `detectWithdrawal()`|
| `PRIV-TC-007`    | `...service/PrivacySettingsServiceTest.java` | `[ ]`          | —                 | —                           |
| `PRIV-TC-INT-001`| `...privacy/PrivacySettingsIntegrationTest.java` | `[ ]`      | —                 | —                           |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// PrivacySettingsService.java — Red Phase Stub
@Service
public class PrivacySettingsService implements IPrivacySettingsService {
    @Override
    public PrivacySettingsResponse getSettings(UUID userId, UserPrincipal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public PrivacySettingsResponse updateSettings(UUID userId, UpdatePrivacySettingsRequest request, UserPrincipal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] `CB-PRIV-IMP-001` TDS đã được review
- [ ] ADR-PRIV-001/002/003 đã Accepted
- [ ] DPO đã sign-off (Sensitive-PII module)
- [ ] Flyway migration script đã được approved
- [ ] Test fixtures đã được chuẩn bị (FX-P-001 → FX-P-005)

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả 8 test cases xanh
- [ ] Test coverage ≥ 80% cho `privacy.*` package
- [ ] Không có `@SuppressWarnings("unchecked")` trong production code
- [ ] Audit log sinh ra đúng format sau mỗi PUT
- [ ] GET lần đầu tạo record với safe defaults (verified bởi PRIV-TC-001)
- [ ] PRIV-TC-006 xanh — analytics consent withdrawal hoạt động đúng

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả 8 tests FAIL với empty stub
- [ ] Contract Existence — mọi import trong test files resolve

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/privacy/

# Revert migration (chỉ khi bảng mới, không có production data)
# psql -c "DROP TABLE IF EXISTS privacy_settings; DROP TYPE IF EXISTS profile_visibility;"

# Flyway repair
./mvnw flyway:repair
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern          | Dấu hiệu trong Test Spec              | Check | Gate chặn |
|-----------|-----------------------|---------------------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Gen     | TC không reference ADR/BR nào         | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth      | Test PASS với empty/throw stub        | ☐     | G-2 ★     |
| AP-AI-003 | Implicit Decision     | Test assume ownership check không có ADR | ☐  | G-1       |
| AP-AI-004 | Layer Violation       | Test verify service logic trong controller | ☐ | G-4       |
| AP-AI-005 | Hallucinated Contract | Import class không tồn tại            | ☐     | G-3       |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
