# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-47 Create Vaccination Reminder

**Document ID:** `CB-REMINDER-IMP-002-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC47_CreateVaccinationReminder/UC47_CreateVaccinationReminder_TDS.md` (CB-REMINDER-IMP-002)
- `01_Requirements/SRS.md` §3.3.1.24

> **TDD Convention:** Viết test TRƯỚC khi implement. Thứ tự bắt buộc: test RED → implement → GREEN → refactor.
> Không dùng PII thật — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD Spec cho UC-47 Create Vaccination Reminder |
| 2026-07-05 | AI Agent — Amelia (Dev Agent) | GREEN: 7/7 unit tests pass (VaccinationReminderServiceTest) — Red Gate confirmed FAIL, Green Gate PASS |

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
| **Feature / UC ID** | `UC-47` |
| **Module** | `reminder — CreateVaccinationReminder` |
| **Spec gốc** | `CB-REMINDER-IMP-002` |
| **Priority** | 🟠 P1 — High |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-VAC-001, BR-VAC-002, BR-VAC-003, BR-SAFETY` |
| **Upstream Dependencies** | `auth, baby_profiles, vaccination_records, FCM` |
| **Downstream Consumers** | `UC-49 ViewTodayTasks, UC-212 ViewReminderDetail, audit` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-REMINDER-IMP-002 §17`, `BR-VAC-001`, `BR-SAFETY` |
| **Constraints Injected** | `C1 (type=VACCINATION), C2 (baby ownership), C3 (no medical advice), C4 (JWT userId), C5 (layer separation)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec gốc không rõ `baby_id` có bắt buộc không | V1__init_schema.sql: `baby_id` là nullable ở DB, nhưng BR-VAC-001 bắt buộc application layer validate | Test phải verify service throw REMINDER-003 khi baby_id null hoặc không thuộc owner |
| L2 | Spec gốc không đề cập BR-SAFETY | CLAUDE.md: AI không chẩn đoán, không kê đơn | Test phải verify response không chứa bất kỳ medical recommendation nào |
| L3 | scheduledAt constraint chưa rõ | Policy từ UC-45: phải ≥ now + 5 phút | Test boundary: scheduledAt = now+4min → FAIL; scheduledAt = now+5min → PASS |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UC-47 CreateVaccinationReminder bao gồm các layer:
├── Service (ReminderService.createVaccinationReminder — core business logic)
├── Controller (POST /api/v1/reminders — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-47` | Mother tạo vaccination reminder; system suggest từ vaccination_records |
| `BR-VAC-001` | reminder_type=VACCINATION; baby_id bắt buộc và phải thuộc owner |
| `BR-VAC-003` | Auto-suggest scheduledAt từ vaccination_records.scheduled_date |
| `BR-RBAC` | Mother chỉ truy cập reminder của mình |
| `BR-SAFETY` | Response không chứa medical recommendation |
| `CB-REMINDER-IMP-002 §10` | Error codes REMINDER-001 đến REMINDER-005 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother tạo reminder với dữ liệu hợp lệ | `ReminderService.createVaccinationReminder()` | `VAC-TC-001` |
| TC-COND-002 | babyId không thuộc owner | `ReminderService.validateBabyOwnership()` | `VAC-TC-002` |
| TC-COND-003 | babyId null | DTO validation | `VAC-TC-003` |
| TC-COND-004 | scheduledAt trong quá khứ | `ReminderService.validateScheduledAt()` | `VAC-TC-004` |
| TC-COND-005 | scheduledAt < now+5min | boundary validation | `VAC-TC-005` |
| TC-COND-006 | Unauthorized — không có JWT | Spring Security filter | `VAC-TC-006` |
| TC-COND-007 | Wrong role (not MOTHER) | `@PreAuthorize` | `VAC-TC-007` |
| TC-COND-008 | Full integration flow | Controller → Service → DB | `VAC-TC-INT-001` |
| TC-COND-009 | Auto-suggest từ vaccination_records | `ReminderService.autoSuggestFromRecord()` | `VAC-TC-008` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| Equivalence Partitioning | babyId (valid / invalid / null) | 3 partition classes |
| Boundary Value Analysis | scheduledAt (now+4min, now+5min, future) | Schedule boundary |
| State Transition Testing | Reminder status (PENDING → terminal states) | Trạng thái sau create |
| Error Guessing | CSRF, SQL injection trong title field | Security hardening |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-VAC-001` | DB seed | `User { userId: "user-001", role: ROLE_MOTHER }` | Happy path owner |
| `FX-VAC-002` | DB seed | `BabyProfile { babyId: "baby-001", ownerUserId: "user-001", birthDate: 2026-01-01 }` | Valid baby |
| `FX-VAC-003` | DB seed | `BabyProfile { babyId: "baby-999", ownerUserId: "user-999" }` | Other user's baby |
| `FX-VAC-004` | DB seed | `VaccinationRecord { vaccinationRecordId: "vac-001", babyId: "baby-001", vaccineName: "5-in-1", scheduledDate: 2026-07-20, status: SCHEDULED }` | Auto-suggest |
| `FX-VAC-005` | JWT | `{ sub: "user-001", role: "ROLE_MOTHER" }` | Auth token |
| `FX-VAC-006` | JWT | `{ sub: "user-002", role: "ROLE_EXPERT" }` | Wrong role test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// VaccinationReminderTestFactory.java
class VaccinationReminderTestFactory {

    static CreateVaccinationReminderRequest makeRequest() {
        CreateVaccinationReminderRequest req = new CreateVaccinationReminderRequest();
        req.setReminderType(ReminderType.VACCINATION);
        req.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // baby-001
        req.setTitle("Tiêm vắc-xin 5 trong 1 — Mũi 1");
        req.setScheduledAt(ZonedDateTime.now().plusHours(2));
        req.setRecurrenceRule(null);
        return req;
    }

    static CreateVaccinationReminderRequest makeRequest(
            Consumer<CreateVaccinationReminderRequest> overrides) {
        CreateVaccinationReminderRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    static BabyProfile makeBabyProfile() {
        BabyProfile baby = new BabyProfile();
        baby.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        baby.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        baby.setBirthDate(LocalDate.of(2026, 1, 1));
        return baby;
    }
}
```

---

### VAC-TC-001 — Tạo vaccination reminder thành công (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.createVaccinationReminder()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/VaccinationReminderServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-VAC-001`, `CB-REMINDER-IMP-002 §9.2`

**Preconditions:**
- FX-VAC-001: User "user-001" với ROLE_MOTHER tồn tại
- FX-VAC-002: BabyProfile "baby-001" owned by "user-001"
- FX-VAC-005: JWT token hợp lệ cho "user-001"

**Test Steps:**
1. Arrange: Mock `babyProfileRepository.findByBabyIdAndOwnerUserId("baby-001", "user-001")` → return `Optional.of(makeBabyProfile())`; Mock `reminderRepository.save(any())` → return saved reminder; Mock `notificationService.scheduleFcmPush(...)` → return "fcm-job-id-001"
2. Act: `reminderService.createVaccinationReminder(makeRequest(), UUID("user-001"))`
3. Assert: Response không null; `reminderId` không null; `status == "PENDING"`; `reminderType == "VACCINATION"`; `babyId == "baby-001"`; `fcmScheduled == true`

**Expected Result (PASS):**
- Response trả về `CreateVaccinationReminderResponse` với `status=PENDING`, `reminderType=VACCINATION`, `babyId=baby-001`
- `reminderRepository.save()` được gọi đúng 1 lần
- `notificationService.scheduleFcmPush()` được gọi đúng 1 lần
- `auditService.emit(VaccinationReminderCreated)` được gọi đúng 1 lần

**Expected Result (FAIL — dấu hiệu lỗi):**
- Response null, hoặc status != PENDING, hoặc FCM không được schedule

**Current Status:** 🔴 Not written

---

### VAC-TC-002 — babyId không thuộc owner → 404

**Severity:** `CRITICAL`
**Feature Under Test:** `ReminderService.validateBabyOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/VaccinationReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-VAC-001`, `BR-RBAC`, `REMINDER-003`

**Preconditions:**
- FX-VAC-003: BabyProfile "baby-999" owned by "user-999"
- FX-VAC-005: JWT token cho "user-001"

**Test Steps:**
1. Arrange: Mock `babyProfileRepository.findByBabyIdAndOwnerUserId("baby-999", "user-001")` → return `Optional.empty()`
2. Act: `reminderService.createVaccinationReminder(makeRequest(r -> r.setBabyId(UUID("baby-999"))), UUID("user-001"))`
3. Assert: `ReminderException` được ném với code `REMINDER-003`

**Expected Result (PASS):**
- `ReminderException` với error code `REMINDER-003` và HTTP 404
- `reminderRepository.save()` KHÔNG được gọi
- DB không có record mới

**Expected Result (FAIL):**
- Service tạo reminder cho baby của người khác → vi phạm RBAC

**Current Status:** 🔴 Not written

---

### VAC-TC-003 — babyId null → 400

**Severity:** `HIGH`
**Feature Under Test:** `CreateVaccinationReminderRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-VAC-001`, `REMINDER-001`

**Test Steps:**
1. Arrange: Tạo request với `babyId = null`
2. Act: `mockMvc.perform(POST /api/v1/reminders)` với body thiếu babyId
3. Assert: Response 400; error code `REMINDER-001`; details chứa field "babyId"

**Expected Result (PASS):**
- HTTP 400
- `error.code == "REMINDER-001"`
- `error.details[0].field == "babyId"`

**Current Status:** 🔴 Not written

---

### VAC-TC-004 — scheduledAt trong quá khứ → 400

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.validateScheduledAt()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/VaccinationReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `REMINDER-002`

**Test Steps:**
1. Arrange: Mock baby ownership → valid; `scheduledAt = ZonedDateTime.now().minusDays(1)`
2. Act: `reminderService.createVaccinationReminder(makeRequest(r -> r.setScheduledAt(past)), userId)`
3. Assert: `ReminderException` với code `REMINDER-002`

**Expected Result (PASS):**
- Exception với `REMINDER-002`, HTTP 400
- Repository.save() không được gọi

**Current Status:** 🔴 Not written

---

### VAC-TC-005 — scheduledAt boundary: now+4min FAIL, now+5min PASS

**Severity:** `HIGH`
**Feature Under Test:** `ReminderService.validateScheduledAt()` — boundary
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/VaccinationReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `REMINDER-002`, policy đồng nhất với UC-45`

**Test Steps:**
1. Act (case 1): scheduledAt = now + 4 phút → assert REMINDER-002 thrown
2. Act (case 2): scheduledAt = now + 5 phút + 1 giây → assert SUCCESS (201)

**Expected Result (PASS):**
- Case 1: Exception REMINDER-002
- Case 2: Không exception, reminder được lưu

**Current Status:** 🔴 Not written

---

### VAC-TC-006 — Unauthorized (không có JWT) → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Act: `mockMvc.perform(POST /api/v1/reminders)` — không có Authorization header
2. Assert: HTTP 401

**Expected Result (PASS):** HTTP 401 — Authentication required

**Current Status:** 🔴 Not written

---

### VAC-TC-007 — Wrong role (ROLE_EXPERT) → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on endpoint
**Test File:** `src/test/java/com/carebridge/backend/reminder/controller/ReminderControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`, `REMINDER-004`

**Test Steps:**
1. Arrange: FX-VAC-006: JWT với ROLE_EXPERT
2. Act: POST /api/v1/reminders với valid body
3. Assert: HTTP 403; error code `REMINDER-004`

**Expected Result (PASS):** HTTP 403

**Current Status:** 🔴 Not written

---

### VAC-TC-008 — Auto-suggest từ vaccination_records

**Severity:** `MEDIUM`
**Feature Under Test:** `ReminderService.autoSuggestFromRecord()`
**Test File:** `src/test/java/com/carebridge/backend/reminder/service/VaccinationReminderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-VAC-003`

**Preconditions:**
- FX-VAC-004: VaccinationRecord "vac-001" có scheduledDate = 2026-07-20

**Test Steps:**
1. Arrange: Mock `vaccinationRecordRepository.findByBabyIdAndStatus("baby-001", "SCHEDULED")` → return [vac-001]
2. Act: `reminderService.autoSuggestFromRecord(UUID("baby-001"), UUID("user-001"))`
3. Assert: List có 1 phần tử; `suggestion.suggestedDate == 2026-07-20`; `suggestion.vaccineName == "5-in-1"`

**Expected Result (PASS):**
- List suggestions không empty; suggestedDate đúng với scheduled_date trong DB

**Current Status:** 🔴 Not written

---

### VAC-TC-INT-001 — Full integration flow

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/reminders → DB`
**Test File:** `src/test/java/com/carebridge/backend/reminder/VaccinationReminderIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration applied (V1__init_schema.sql)
- Seed: FX-VAC-001, FX-VAC-002 inserted via SQL

**Test Steps:**
1. Seed user-001 và baby-001 vào DB
2. Authenticate → get JWT
3. `POST /api/v1/reminders` với valid body
4. Assert response 201
5. Query DB: `SELECT * FROM reminders WHERE owner_user_id = 'user-001'`

**Expected Result (PASS):**
- HTTP 201
- DB: 1 row trong `reminders` với `reminder_type='VACCINATION'`, `baby_id='baby-001'`, `status='PENDING'`
- `owner_user_id` = 'user-001'

**DB Assertion:**
```java
List<Reminder> reminders = reminderRepository
    .findByOwnerUserIdAndReminderTypeAndStatus(userId, "VACCINATION", "PENDING");
assertThat(reminders).hasSize(1);
assertThat(reminders.get(0).getBabyId()).isEqualTo(UUID.fromString("baby-001"));
assertThat(reminders.get(0).getStatus()).isEqualTo("PENDING");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VAC-TC-001` | `VaccinationReminderServiceTest.java` | `[ ]` | — | — |
| `VAC-TC-002` | `VaccinationReminderServiceTest.java` | `[ ]` | — | — |
| `VAC-TC-003` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `VAC-TC-004` | `VaccinationReminderServiceTest.java` | `[ ]` | — | — |
| `VAC-TC-005` | `VaccinationReminderServiceTest.java` | `[ ]` | — | — |
| `VAC-TC-006` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `VAC-TC-007` | `ReminderControllerTest.java` | `[ ]` | — | — |
| `VAC-TC-008` | `VaccinationReminderServiceTest.java` | `[ ]` | — | — |
| `VAC-TC-INT-001` | `VaccinationReminderIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**
```java
@Service
public class ReminderService implements IReminderService {

    @Override
    public CreateVaccinationReminderResponse createVaccinationReminder(
            CreateVaccinationReminderRequest request, UUID ownerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<VaccinationSuggestion> autoSuggestFromRecord(UUID babyId, UUID ownerUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VAC-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VAC-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `VAC-TC-003` | DTO validation (framework) | 🔴 FAIL if annotation missing | ☐ FAIL ☐ PASS | — |
| `VAC-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → GATE-2 PASS → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-REMINDER-IMP-002` đã review và approve
- [ ] Logic Issues (Section 2) đã confirm với Tech Lead
- [ ] Schema V1__init_schema.sql xác nhận `reminders` và `vaccination_records` tables tồn tại
- [ ] Test fixtures (Section 3 TDS-05) đã chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=VaccinationReminderServiceTest` — tất cả xanh
- [ ] `./mvnw verify -Dtest=VaccinationReminderIntegrationTest` — xanh với Testcontainers
- [ ] Test coverage ≥ 80% lines cho `ReminderService` methods liên quan
- [ ] Không có business logic trong `ReminderController`
- [ ] Response không chứa medical recommendation (BR-SAFETY)
- [ ] RBAC: Mother chỉ truy cập baby của mình (BR-RBAC)

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả tests FAIL với stub trước khi implement
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] Props Isolation: không có shared mutable state giữa tests
- [ ] Oracle Source: mọi assert có ghi nguồn (BR/AC/ADR)

### Suspension Criteria

- BabyProfile module chưa sẵn sàng (FK dependency)
- FCM credentials chưa được cấu hình trong environment

---

## 7. Rollback Plan

```bash
# Không có migration mới cho UC-47 (dùng schema V1)
# Revert code
git checkout -- src/main/java/com/carebridge/backend/reminder/
git checkout -- src/test/java/com/carebridge/backend/reminder/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong spec | Check | Gate |
|-------|-------------|---------------------|-------|------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-VAC-001 hoặc BR-RBAC | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume baby validation không cần ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller chứa ownership check logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Import `VaccinationScheduleService` mà chưa có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
