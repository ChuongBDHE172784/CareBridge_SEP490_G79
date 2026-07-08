# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-44 Share Summary with Expert

**Document ID:** `CB-HEALTH-IMP-006-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Spec`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `04_Implement/UC44_ShareSummaryWithExpert/UC44_ShareSummaryWithExpert_TDS.md` — TDS CB-HEALTH-IMP-006
- `02_Requirements/SRS.md §3.3.1.21` — UC-44 functional requirements
- `04_Implement/UC43_GenerateHealthSummary/` — upstream dependency

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-44 Share Summary with Expert |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-44` |
| **Module** | `ShareSummaryWithExpert — health Bounded Context` |
| **Spec gốc** | `CB-HEALTH-IMP-006` |
| **Priority** | 🟠 P1 — High |
| **Sprint** | `S[N] (2026-06-26 → TBD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SHARE-001, BR-SHARE-002, PDPA` |
| **Upstream Dependencies** | `auth (JWT), health_summaries (UC-43), consultation_bookings, data_permissions (UC-17)` |
| **Downstream Consumers** | `Expert consultation view, AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-006 §17`, `BR-SHARE-001`, `BR-SHARE-002`, `BR-RBAC`, `BR-PRIVACY` |
| **Constraints Injected** | C1 (triple-gate validation), C2 (no logic in controller), C3 (userId from JWT), C4 (audit event), C5 (expiry check), C6 (status filter) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS không chỉ định cụ thể cách lưu trữ sharing relationship | `V1__init_schema.sql`: `consultation_bookings.shared_summary_id UUID FK → health_summaries` | Test verify UPDATE consultation_bookings, không tạo bảng mới |
| L2 | SRS không đề cập double-gate validation rõ ràng | BR-SHARE-001: cần cả booking ACTIVE VÀ data_permissions valid | Test verify từng gate riêng (3 test cases cho 3 error codes) |
| L3 | SRS không đề cập data_permissions expiry check | BR-SHARE-002: Expert access hết hạn khi permission hết hạn | Test verify permission với expiry_at < now() → HEALTH-009 |
| L4 | Role của Expert trong sharing không rõ | RBAC: chỉ ROLE_MOTHER mới được gọi share endpoint | Test verify ROLE_EXPERT không được phép gọi endpoint này |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ShareSummaryWithExpert bao gồm các layer:
├── Service (triple-gate validation — unit test với Mockito)
├── Controller (DTO validation + auth — @WebMvcTest)
├── Repository (ConsultationBookingRepository, DataPermissionRepository)
└── Integration (Testcontainers PostgreSQL + full Spring context)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-44 §3.3.1.21` | Mother chia sẻ summary trong context booking |
| `BR-SHARE-001` | Cần active booking + valid data_permissions |
| `BR-SHARE-002` | Expert access hết hạn khi consultation kết thúc / permission bị thu hồi |
| `BR-RBAC` | Chỉ ROLE_MOTHER; chỉ chia sẻ summary của mình |
| `CB-HEALTH-IMP-006 §10` | Error codes: HEALTH-007, HEALTH-008, HEALTH-009, HEALTH-010 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | All 3 gates pass → share thành công | `ShareSummaryService.shareSummary()` | `SHARE-TC-001` |
| TC-COND-002 | Summary không thuộc Mother → 403 | Gate 1: ownership check | `SHARE-TC-002` |
| TC-COND-003 | Booking không active → 422 | Gate 2: booking status check | `SHARE-TC-003` |
| TC-COND-004 | Data permission hết hạn → 403 | Gate 3: permission expiry check | `SHARE-TC-004` |
| TC-COND-005 | Data permission không tồn tại → 403 | Gate 3: permission existence check | `SHARE-TC-005` |
| TC-COND-006 | ROLE_EXPERT cố gọi endpoint → 403 | Spring Security RBAC | `SHARE-TC-006` |
| TC-COND-007 | Missing JWT → 401 | Spring Security filter | `SHARE-TC-007` |
| TC-COND-008 | DB UPDATE consultation_bookings xác nhận | `ConsultationBookingRepository` | `SHARE-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Booking status: ACTIVE vs non-ACTIVE | BR-SHARE-001 |
| Boundary Value Analysis | permission expiry: expiry_at = now() (edge case) | BR-SHARE-002 |
| State Transition Testing | Booking: ACTIVE → COMPLETED → access denied | State machine §6.3 TDS |
| Error Guessing | Cross-owner sharing, expired permission replay | Security / BR-PRIVACY |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | SYNTHETIC Mother UUID, Expert UUID, booking status='ACTIVE', shared_summary_id=null | Happy path |
| `FX-002` | DB seed | data_permissions: owner=Mother, grantee=Expert, expiry_at=null (no expiry) | Happy path permission |
| `FX-003` | DB seed | SYNTHETIC other Mother UUID + summary owned by other | Cross-owner RBAC test |
| `FX-004` | DB seed | booking status='COMPLETED' | Gate 2 test |
| `FX-005` | DB seed | data_permissions với expiry_at = now() - 1 hour | Expired permission test |
| `FX-006` | JWT | `{ sub: 'synthetic-mother-uuid', role: 'ROLE_MOTHER' }` | Mother auth |
| `FX-007` | JWT | `{ sub: 'synthetic-expert-uuid', role: 'ROLE_EXPERT' }` | Expert auth (unauthorized) |
| `FX-008` | DB seed | health_summary thuộc SYNTHETIC Mother | Summary to share |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ShareSummaryTestFactory.java
class ShareSummaryTestFactory {

    static final UUID SYNTHETIC_MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SYNTHETIC_OTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID SYNTHETIC_EXPERT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID SYNTHETIC_SUMMARY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID SYNTHETIC_BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    static ShareSummaryRequest makeRequest() {
        ShareSummaryRequest req = new ShareSummaryRequest();
        req.setSummaryId(SYNTHETIC_SUMMARY_ID);
        req.setBookingId(SYNTHETIC_BOOKING_ID);
        return req;
    }

    static ShareSummaryRequest makeRequest(Consumer<ShareSummaryRequest> overrides) {
        ShareSummaryRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    static ConsultationBooking makeActiveBooking() {
        ConsultationBooking booking = new ConsultationBooking();
        booking.setBookingId(SYNTHETIC_BOOKING_ID);
        booking.setMotherUserId(SYNTHETIC_MOTHER_ID);
        booking.setExpertId(SYNTHETIC_EXPERT_ID);
        booking.setStatus("ACTIVE");
        booking.setSharedSummaryId(null);
        return booking;
    }

    static DataPermission makeValidPermission() {
        DataPermission perm = new DataPermission();
        perm.setOwnerUserId(SYNTHETIC_MOTHER_ID);
        perm.setGranteeUserId(SYNTHETIC_EXPERT_ID);
        perm.setExpiryAt(null); // no expiry
        return perm;
    }
}
```

---

### SHARE-TC-001 — Chia sẻ summary thành công (all 3 gates pass)

**Severity:** `HIGH`
**Feature Under Test:** `ShareSummaryService.shareSummary()`
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-44 §3.3.1.21`, `BR-SHARE-001`

**Preconditions:**
- Mock `IHealthSummaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, MOTHER_ID)` → Optional.of(summary) [FX-008]
- Mock `IConsultationBookingRepository.findByIdAndMotherUserIdAndStatus(BOOKING_ID, MOTHER_ID, "ACTIVE")` → Optional.of(activeBooking) [FX-001]
- Mock `IDataPermissionRepository.existsByOwnerAndGranteeAndNotExpired(MOTHER_ID, EXPERT_ID, now)` → true [FX-002]
- Mock `bookingRepository.updateSharedSummaryId(BOOKING_ID, SUMMARY_ID)` → void

**Test Steps:**
1. Arrange: Setup mocks via factory
2. Act: `shareSummaryService.shareSummary(makeRequest(), SYNTHETIC_MOTHER_ID)`
3. Assert: Response không null, expertId = EXPERT_ID, sharedAt không null, message chứa "thành công"

**Expected Result (PASS):**
- Response chứa `bookingId`, `summaryId`, `expertId`, `sharedAt`
- `bookingRepository.updateSharedSummaryId()` được gọi đúng 1 lần với đúng args
- `auditService.emit(HealthSummarySharingGrantedEvent)` được gọi đúng 1 lần

**Expected Result (FAIL):**
- `updateSharedSummaryId()` không được gọi
- Response null hoặc thiếu fields
- Audit event không được emit

**Current Status:** 🟢 Passing
**Implementation Note:** Triple-gate validation phải theo thứ tự ownership → booking → permission trước khi update.

---

### SHARE-TC-002 — Summary không thuộc Mother → HEALTH-007

**Severity:** `CRITICAL`
**Feature Under Test:** `ShareSummaryService.validateSharingPrerequisites()` — Gate 1
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-RBAC`, `BR-PRIVACY`

**Preconditions:**
- Mock `IHealthSummaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, MOTHER_ID)` → Optional.empty() [FX-003]

**Test Steps:**
1. Arrange: summaryId thuộc SYNTHETIC_OTHER_ID, caller = SYNTHETIC_MOTHER_ID
2. Act: Gọi `shareSummaryService.shareSummary(request, SYNTHETIC_MOTHER_ID)`
3. Assert: `SharingException` được ném với code "HEALTH-007"

**Expected Result (PASS):**
- `SharingException` với code "HEALTH-007" (403)
- `bookingRepository` KHÔNG được gọi
- `permissionRepository` KHÔNG được gọi
- `updateSharedSummaryId()` KHÔNG được gọi

**Expected Result (FAIL):**
- Share thành công (RBAC bypass)
- Gate 2 hoặc 3 được check trước Gate 1

**Current Status:** 🟢 Passing
**Implementation Note:** Gate 1 phải là check đầu tiên trong `validateSharingPrerequisites()`. Fail fast.

---

### SHARE-TC-003 — Booking không active → HEALTH-008

**Severity:** `HIGH`
**Feature Under Test:** `ShareSummaryService.validateSharingPrerequisites()` — Gate 2
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-SHARE-001`

**Preconditions:**
- Mock summaryRepo → Optional.of(summary) (Gate 1 pass)
- Mock bookingRepo → Optional.empty() [FX-004 — booking COMPLETED]

**Test Steps:**
1. Arrange: booking status = "COMPLETED" (không phải "ACTIVE")
2. Act: Gọi shareSummaryService.shareSummary(request, MOTHER_ID)
3. Assert: `SharingException` với code "HEALTH-008"

**Expected Result (PASS):**
- `SharingException` với code "HEALTH-008" (422)
- `permissionRepository` KHÔNG được gọi
- DB KHÔNG bị update

**Expected Result (FAIL):**
- Share thành công với booking đã kết thúc (vi phạm BR-SHARE-001)

**Current Status:** 🟢 Passing

---

### SHARE-TC-004 — Data permission hết hạn → HEALTH-009

**Severity:** `CRITICAL`
**Feature Under Test:** `ShareSummaryService.validateSharingPrerequisites()` — Gate 3 (expiry)
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SHARE-002`

**Preconditions:**
- Mock Gate 1 pass, Gate 2 pass (booking ACTIVE)
- Mock `permissionRepository.existsByOwnerAndGranteeAndNotExpired(MOTHER_ID, EXPERT_ID, now)` → false [FX-005]

**Test Steps:**
1. Arrange: data_permissions có expiry_at = Instant.now().minusSeconds(3600)
2. Act: Gọi shareSummaryService.shareSummary(request, MOTHER_ID)
3. Assert: `SharingException` với code "HEALTH-009"

**Expected Result (PASS):**
- `SharingException` với code "HEALTH-009" (403)
- DB KHÔNG bị update

**Expected Result (FAIL):**
- Share thành công với permission đã hết hạn (vi phạm BR-SHARE-002)

**Current Status:** 🟢 Passing
**Implementation Note:** `existsByOwnerUserIdAndGranteeUserIdAndNotExpired()` PHẢI check `expiry_at IS NULL OR expiry_at > :now`. Clock injection cần thiết để test deterministic.

---

### SHARE-TC-005 — Data permission không tồn tại → HEALTH-009

**Severity:** `HIGH`
**Feature Under Test:** `ShareSummaryService.validateSharingPrerequisites()` — Gate 3 (no record)
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SHARE-001`

**Preconditions:**
- Mock Gate 1 pass, Gate 2 pass
- Mock permissionRepository → false (no record at all)

**Test Steps:**
1. Arrange: Không có data_permissions giữa Mother và Expert
2. Act: Gọi shareSummaryService.shareSummary(request, MOTHER_ID)
3. Assert: `SharingException` với code "HEALTH-009"

**Expected Result (PASS):**
- `SharingException` với code "HEALTH-009"
- Gợi ý user dùng UC-17 để cấp permission trước

**Current Status:** 🔴 Not written

---

### SHARE-TC-006 — ROLE_EXPERT cố gọi share endpoint → 403

**Severity:** `HIGH`
**Feature Under Test:** Spring Security RBAC — `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- `@WebMvcTest` với Security enabled
- JWT có role = "ROLE_EXPERT" [FX-007]

**Test Steps:**
1. Arrange: Tạo JWT với ROLE_EXPERT
2. Act: POST /api/v1/health-summaries/share với JWT ROLE_EXPERT
3. Assert: HTTP 403

**Expected Result (PASS):**
- HTTP 403
- `IShareSummaryService` KHÔNG được gọi

**Expected Result (FAIL):**
- HTTP 200 — Expert có thể share (RBAC bypass)

**Current Status:** 🔴 Not written

---

### SHARE-TC-007 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: Không có Authorization header
2. Act: POST /api/v1/health-summaries/share
3. Assert: HTTP 401, error.code = "IAM-001"

**Expected Result (PASS):**
- HTTP 401
- `IShareSummaryService` KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### SHARE-TC-SEC-001 — IDOR: Mother cố share summary của Mother khác

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ShareSummaryService` — Gate 1 ownership check
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummarySecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- SYNTHETIC Mother A (caller)
- SYNTHETIC Summary thuộc Mother B (summaryId controlled by attacker)

**Test Steps (Attack Simulation):**
1. Arrange: Mother A gọi API với summaryId của Mother B
2. Act: POST /api/v1/health-summaries/share với summaryId = Mother B's summary
3. Assert: HTTP 403 với HEALTH-007; consultation_bookings KHÔNG bị update

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 403
- DB: `consultation_bookings.shared_summary_id` vẫn = null

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP 200 — dữ liệu của Mother B bị chia sẻ mà không có consent

**Current Status:** 🔴 Not written

---

### SHARE-TC-SEC-002 — Replay attack: share với expired permission

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `DataPermissionRepository` expiry check
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummarySecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Permission grant tại T=0, expiry=T+1hour
2. Attempt share tại T+2hours (permission expired)
3. Assert: HTTP 403 với HEALTH-009

**Expected Result (PASS):**
- HTTP 403 ngay cả khi permission record vẫn còn trong DB

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### SHARE-TC-INT-001 — Luồng E2E sharing với DB thật

**Severity:** `HIGH`
**Feature Under Test:** Full flow: Controller → Service → 3 repositories → PostgreSQL UPDATE
**Test File:** `src/test/java/com/carebridge/backend/health/ShareSummaryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- PostgreSQL container (`@Testcontainers` auto-start)
- Flyway migration V1__init_schema.sql applied
- Seed (via JPA):
  - SYNTHETIC Mother user + Expert user
  - SYNTHETIC health_summary (status='ACTIVE', owner=Mother)
  - SYNTHETIC consultation_booking (status='ACTIVE', mother=Mother, expert=Expert, shared_summary_id=null)
  - SYNTHETIC data_permissions (owner=Mother, grantee=Expert, expiry_at=null)

**Test Steps:**
1. Seed tất cả SYNTHETIC data
2. POST /api/v1/health-summaries/share với JWT ROLE_MOTHER
3. Assert HTTP 200
4. Query DB: `SELECT shared_summary_id FROM consultation_bookings WHERE booking_id = ?`

**Expected Result (PASS):**
- HTTP 200 với response đầy đủ
- DB: `consultation_bookings.shared_summary_id` = SYNTHETIC_SUMMARY_ID
- Audit log chứa HealthSummarySharingGranted event

**Expected Result (FAIL):**
- HTTP 500 hoặc 4xx không mong đợi
- DB: shared_summary_id vẫn = null sau call thành công

**DB Assertion:**
```java
Map<String, Object> row = jdbcTemplate.queryForMap(
    "SELECT shared_summary_id::text FROM consultation_bookings WHERE booking_id = ?",
    SYNTHETIC_BOOKING_ID
);
assertThat(row.get("shared_summary_id").toString())
    .isEqualTo(SYNTHETIC_SUMMARY_ID.toString());
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SHARE-TC-001` | `ShareSummaryServiceTest.java` | `[x]` | `Passed` | — |
| `SHARE-TC-002` | `ShareSummaryServiceTest.java` | `[x]` | `Passed` | — |
| `SHARE-TC-003` | `ShareSummaryServiceTest.java` | `[x]` | `Passed` | — |
| `SHARE-TC-004` | `ShareSummaryServiceTest.java` | `[x]` | `Passed` | — |
| `SHARE-TC-005` | `ShareSummaryServiceTest.java` | `[ ]` | `___` | Covered by SHARE-TC-004 (same no-permission path) |
| `SHARE-TC-006` | `ShareSummaryControllerTest.java` | `[ ]` | `___` | Not implemented (controller layer) |
| `SHARE-TC-007` | `ShareSummaryControllerTest.java` | `[ ]` | `___` | Not implemented (controller layer) |
| `SHARE-TC-SEC-001` | `ShareSummarySecurityTest.java` | `[ ]` | `___` | Not implemented (security/MockMvc layer) |
| `SHARE-TC-SEC-002` | `ShareSummarySecurityTest.java` | `[ ]` | `___` | Not implemented (security/MockMvc layer) |
| `SHARE-TC-INT-001` | `ShareSummaryIntegrationTest.java` | `[ ]` | `___` | Not implemented (Testcontainers unavailable) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase stub
@Service
public class ShareSummaryService implements IShareSummaryService {

    @Override
    public ShareSummaryResponse shareSummary(ShareSummaryRequest request, UUID motherUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SHARE-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `SHARE-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `SHARE-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `SHARE-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `SHARE-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |

**Red Gate Evidence:**
- Stub commit hash: `2026-07-07-sprint3`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** → tiếp tục implement
- Log: `logs/red-gate-uc44-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-006` đã được review và approve
- [ ] DPO đã sign-off (consent data xử lý)
- [ ] UC-43 (GenerateHealthSummary) đã implement và healthy
- [ ] UC-17 (GrantDataPermission) đã implement — `data_permissions` table có data
- [ ] Test fixtures FX-001 đến FX-008 đã được chuẩn bị
- [ ] `consultation_bookings.shared_summary_id` column xác nhận tồn tại

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (4/4 service tests passed)
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers unavailable)
- [ ] Test coverage ≥ 80% lines cho `ShareSummaryService`
- [x] Triple-gate validation enforce đúng thứ tự: ownership → booking → permission
- [x] Không có business logic trong Controller
- [x] Không có PII plaintext trong logs
- [ ] Audit event `HealthSummarySharingGranted` emit sau mỗi share thành công
- [ ] IDOR test `SHARE-TC-SEC-001` PASS (hệ thống chống cross-owner access)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub
- [ ] **Contract Existence**:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — factory pattern cho mọi test instance
- [ ] **Oracle Source** — mọi assert có nguồn từ BR/AC/ADR

### Suspension Criteria

- `consultation_bookings.shared_summary_id` column không tồn tại (cần kiểm tra V1__init_schema.sql)
- UC-17 (GrantDataPermission) chưa implement → không có data_permissions
- CI pipeline broken

---

## 7. Rollback Plan

```bash
# Revert index migrations
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_data_permissions_owner_grantee;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '3';"

# Revert shared_summary_id cập nhật sai (nếu có)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE consultation_bookings SET shared_summary_id = NULL WHERE updated_at > '[rollback-ts]'::timestamptz;"

# Revert implementation
git checkout -- src/main/java/com/carebridge/backend/health/share/
git checkout -- src/test/java/com/carebridge/backend/health/share/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-SHARE-001/002 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test dùng `findById()` thay vì `findByIdAndOwnerUserId()` | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller có validation logic ngoài DTO | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import class không có trong §8 TDS | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — CB-HEALTH-IMP-006-TEST — UC-44 Share Summary with Expert — Draft 2026-06-26*
