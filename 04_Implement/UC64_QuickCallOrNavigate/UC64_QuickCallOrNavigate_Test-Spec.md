# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC64 — Quick Call or Navigate

**Document ID:** `CB-MAP-IMP-002-TEST`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC64_QuickCallOrNavigate/UC64_QuickCallOrNavigate_TDS.md` (CB-MAP-IMP-002)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.41`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Tech Lead | Khởi tạo tài liệu — Test-Spec cho UC64 Quick Call or Navigate |

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
| **Feature / Gap ID** | `CB-MAP-IMP-002` |
| **Module** | `Quick Call or Navigate — map` |
| **Spec gốc** | `CB-MAP-IMP-002` |
| **Priority** | 🟠 P1 *(SRS Priority = High)* |
| **Sprint** | `S? — Open (chưa gán sprint cụ thể trong task allocation doc)` |
| **Milestone** | `Open` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC63 (facility selection)`, `care_facilities`, `IAM (JWT ROLE_MOTHER)` |
| **Downstream Consumers** | Không có (terminal action + audit log) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-002 §17`, `ADR-MAP-005/006/007/008` |
| **Constraints Injected** | C1 (native dialer, no ZegoCloud), C2 (log fire-and-forget), C3 (no PII in log), C4 (JWT userId), C5 (log failure silent) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS liệt kê ZegoCloud là secondary actor cho UC-64, nhưng không có evidence tích hợp ZegoCloud cho cuộc gọi PSTN/hotline nào trong codebase (UC154 chỉ dùng cho `consultations` đã CONFIRMED) | TDS ADR-MAP-005 (Proposed): dùng native `tel:` dialer | Test KHÔNG kiểm tra bất kỳ tích hợp ZegoCloud nào cho UC64; mọi test case liên quan "call" đều test qua `url_launcher`/`tel:` URI. Test case ghi rõ `Oracle Source: ADR-MAP-005 (Proposed — Open item, xem TDS §2)` |
| L2 | Không có bảng nào trong `V1__init_schema.sql` lưu quick-action log | TDS đề xuất migration mới `quick_action_logs` (`V20260701093000`) | Integration test dùng Testcontainers với migration mới này; test KHÔNG giả định bảng đã tồn tại trong V1 |
| L3 | Existing mock `_makeCall()`/`_openDirections()` trong `emergency_alert_detail_screen.dart` dùng Google Maps deep-link, không phải TrackAsia | TDS ADR-MAP-006: UC64 ưu tiên TrackAsia, fallback map mặc định — đây là code MỚI, KHÔNG sửa file cũ | Mobile test viết cho `QuickActionService` mới trong `emergencyMap/`, không viết test cho `emergency_alert_detail_screen.dart` (ngoài phạm vi UC64) |
| L4 | SRS không nói rõ điều gì xảy ra khi `facilityId` không tồn tại | TDS §10: `MAP-103` 404 nếu `facilityId` được truyền nhưng không tìm thấy; `facilityId` null vẫn hợp lệ (hotline không gắn facility) | Test case riêng cho: facilityId null (hợp lệ), facilityId không tồn tại (404), facilityId hợp lệ (201) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC64 Quick Call or Navigate bao gồm các layer:
├── Backend
│   ├── Service (QuickActionService — mock IQuickActionLogRepository, ICareFacilityRepository với Mockito)
│   ├── Controller (QuickActionController — @WebMvcTest, mock Service)
│   └── Integration (Testcontainers PostgreSQL — verify quick_action_logs persistence)
└── Mobile (Flutter)
    ├── Unit (QuickActionService — mock url_launcher qua flutter_test + mockito cho Dart)
    └── Widget (không bắt buộc cho UC64 — hành động không có UI phức tạp riêng, tái sử dụng UI có sẵn của UC63 facility list item)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-64 §3.3.1.41` | Mother gọi hotline/facility hoặc mở điều hướng; AF1 cancel; AF2 empty state (N/A cho action trực tiếp); E1 unauthorized; E2 invalid data; E3 external/log service failure |
| `ADR-MAP-005` | Native tel: dialer, KHÔNG ZegoCloud |
| `ADR-MAP-006` | TrackAsia ưu tiên, fallback map mặc định |
| `ADR-MAP-007` | Log best-effort, không chặn UI, không lưu PII |
| `ADR-MAP-008` | RBAC ROLE_MOTHER, userId từ JWT |
| BR-RBAC / BR-PRIVACY | Role check + data minimization cho log |
| `V1__init_schema.sql` + migration mới `quick_action_logs` | Cấu trúc cột, FK, CHECK constraint — oracle cho persistence assertions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mobile: `call()` mở native `tel:` dialer với đúng số điện thoại | `QuickActionService.call()` (Dart) | `QA-TC-001` |
| TC-COND-002 | Mobile: `call()` KHÔNG gọi bất kỳ ZegoCloud API/class nào | `QuickActionService.call()` (Dart) | `QA-TC-002` |
| TC-COND-003 | Mobile: `navigate()` ưu tiên mở TrackAsia deep-link nếu `canLaunchUrl` trả true cho scheme TrackAsia | `QuickActionService.navigate()` (Dart) | `QA-TC-003` |
| TC-COND-004 | Mobile: `navigate()` fallback sang map app mặc định khi TrackAsia không khả dụng | `QuickActionService.navigate()` (Dart) | `QA-TC-004` |
| TC-COND-005 | Backend: `logAction()` lưu đúng `userId, facilityId, actionType` vào `quick_action_logs`, KHÔNG lưu số điện thoại | `QuickActionService.logAction()` (Java) | `QA-TC-005` |
| TC-COND-006 | Backend: log API lỗi (DB down) không throw ra ngoài gây crash — nhưng vẫn trả lỗi rõ ràng cho HTTP layer (khác với UC63 vốn có fallback silent — ở đây API riêng, lỗi HTTP là chấp nhận được vì client không phụ thuộc kết quả) | `QuickActionController` error handling | `QA-TC-006` |
| TC-COND-007 | RBAC — ROLE_MOTHER only; role khác → 403 MAP-104 | `QuickActionController` auth | `QA-TC-007` |
| TC-COND-008 | Invalid `actionType` (không phải CALL/NAVIGATE) → 400 MAP-101 | Controller validation | `QA-TC-008` |
| TC-COND-009 | `facilityId` không tồn tại trong `care_facilities` → 404 MAP-103; `facilityId` null → vẫn 201 hợp lệ | `QuickActionService.logAction()` | `QA-TC-009`, `QA-TC-010` |
| TC-COND-010 | Không có JWT → 401 IAM-001 | Security filter chain | `QA-TC-011` |
| TC-COND-011 | userId ghi vào log PHẢI từ JWT, không override được qua request body | `QuickActionController` | `QA-TC-012` |
| TC-COND-012 (Integration) | Full flow — API tạo `quick_action_logs` record đúng, FK tới `care_facilities` hợp lệ | End-to-end | `QA-TC-INT-001` |
| TC-COND-013 (Mobile) | `logActionFireAndForget()` KHÔNG được `await` trước khi `call()`/`navigate()` hoàn tất (kiểm tra thứ tự gọi) | `QuickActionService` (Dart) integration | `QA-TC-013` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `actionType` (CALL/NAVIGATE/invalid), `facilityId` (null/valid/invalid) | Cover input domain đầy đủ |
| Boundary Value Analysis | N/A rõ ràng cho UC64 (không có trường số cần biên) — bỏ qua |
| Error Guessing | TrackAsia app không cài, dialer không khả dụng (emulator), DB down khi log | External-service/environment-failure paths |
| State Transition Testing | N/A — `quick_action_logs` là append-only, không có state machine |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `care_facilities { facilityId: 'facility-001', phone: '+842854042829' }` | Facility hợp lệ cho log |
| `FX-002` | Mock (Dart) | `canLaunchUrl(tel:...) → true`, `launchUrl → true` | Happy path call |
| `FX-003` | Mock (Dart) | `canLaunchUrl(trackasia://...) → true` | TrackAsia khả dụng |
| `FX-004` | Mock (Dart) | `canLaunchUrl(trackasia://...) → false`, `canLaunchUrl(geo:...) → true` | Fallback map app |
| `FX-005` | Mock (Java) | `IQuickActionLogRepository.save() → throws DataAccessException` | Log failure test |
| `FX-006` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth hợp lệ |
| `FX-007` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` | Auth sai role |
| `FX-008` | Request | `{ facilityId: 'facility-001', actionType: 'CALL' }` | Baseline request hợp lệ |
| `FX-009` | Request | `{ facilityId: null, actionType: 'NAVIGATE' }` | Hotline không gắn facility |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (Backend)
// ═══════════════════════════════════════════════════════════

class QuickActionTestFactory {

    static CareFacility makeFacility() {
        return makeFacility(f -> {});
    }

    static CareFacility makeFacility(Consumer<CareFacility> overrides) {
        CareFacility facility = new CareFacility();
        facility.setFacilityId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        facility.setName("Bệnh viện Test A");
        facility.setPhone("+842854042829");
        overrides.accept(facility);
        return facility;
    }

    static QuickActionLogRequest makeRequest() {
        return makeRequest(r -> {});
    }

    static QuickActionLogRequest makeRequest(Consumer<QuickActionLogRequest> overrides) {
        QuickActionLogRequest request = new QuickActionLogRequest();
        request.setFacilityId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        request.setActionType(QuickActionType.CALL);
        overrides.accept(request);
        return request;
    }

    static UUID makeMotherUserId() {
        return UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    }
}
```

```dart
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (Mobile / Dart)
// ═══════════════════════════════════════════════════════════

class QuickActionTestFactory {
  static Map<String, dynamic> makeFacility({Map<String, dynamic>? overrides}) {
    final base = {
      'facilityId': 'facility-001',
      'name': 'Bệnh viện Test A',
      'phone': '+842854042829',
      'latitude': 10.7769,
      'longitude': 106.7009,
    };
    if (overrides != null) base.addAll(overrides);
    return base;
  }
}
```

---

### QA-TC-001 — call() mở native tel: dialer với đúng số điện thoại

**Severity:** `CRITICAL`
**Feature Under Test:** `QuickActionService.call()` (Dart)
**Test File:** `test/features/emergencyMap/services/quick_action_service_test.dart`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MAP-005 (TDS §3, Proposed)`

**Preconditions:**
- Mock `url_launcher` (`canLaunchUrl`, `launchUrl`) qua `MethodChannel` mock hoặc dependency injection wrapper — `FX-002`

**Test Steps:**
1. Arrange: mock launcher trả `canLaunchUrl(Uri.parse('tel:+842854042829')) → true`
2. Act: gọi `QuickActionService.call('+842854042829')`
3. Assert: `launchUrl` được gọi đúng 1 lần với `Uri.parse('tel:+842854042829')`

**Expected Result (PASS):**
- `launchUrl` gọi đúng URI, method trả `true`

**Expected Result (FAIL):**
- URI sai định dạng, hoặc gọi launchUrl với scheme khác `tel:`

**Current Status:** 🔴 Not written

---

### QA-TC-002 — call() KHÔNG gọi bất kỳ ZegoCloud API nào

**Severity:** `CRITICAL`
**Feature Under Test:** `QuickActionService.call()` (Dart)
**Test File:** `test/features/emergencyMap/services/quick_action_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-MAP-005`

**Preconditions:**
- Không mock ZegoCloud SDK — nếu code import ZegoCloud class, compile/test sẽ fail vì dependency không được setup trong test cho module này

**Test Steps:**
1. Arrange: kiểm tra import statement của `quick_action_service_impl.dart` (static check qua test hoặc code review script — có thể thực hiện qua `grep` trong CI thay vì unit test runtime)
2. Act/Assert: verify file KHÔNG import bất kỳ package/class nào có tên chứa "zego" (case-insensitive)

**Expected Result (PASS):**
- Không có import ZegoCloud nào trong `QuickActionService` implementation

**Expected Result (FAIL):**
- File import ZegoCloud SDK

**Current Status:** 🔴 Not written
**Implementation Note:** Có thể triển khai như 1 CI lint script (`grep -ri zego lib/features/emergencyMap/`) thay vì strict unit test runtime nếu framework không hỗ trợ import inspection dễ dàng; ghi rõ trong PR nếu chuyển sang lint check.

---

### QA-TC-003 — navigate() ưu tiên mở TrackAsia deep-link khi khả dụng

**Severity:** `HIGH`
**Feature Under Test:** `QuickActionService.navigate()` (Dart)
**Test File:** `test/features/emergencyMap/services/quick_action_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MAP-006`

**Preconditions:** `FX-003` — `canLaunchUrl(trackasia://...) → true`

**Test Steps:**
1. Arrange: mock launcher: TrackAsia scheme khả dụng
2. Act: gọi `navigate(latitude: 10.7769, longitude: 106.7009)`
3. Assert: `launchUrl` được gọi với URI chứa scheme TrackAsia (không phải `geo:` hay Google Maps)

**Expected Result (PASS):**
- TrackAsia deep-link được launch

**Expected Result (FAIL):**
- Fallback map được mở dù TrackAsia khả dụng

**Current Status:** 🔴 Not written

---

### QA-TC-004 — navigate() fallback sang map app mặc định khi TrackAsia không khả dụng

**Severity:** `HIGH`
**Feature Under Test:** `QuickActionService.navigate()` (Dart)
**Test File:** `test/features/emergencyMap/services/quick_action_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MAP-006`

**Preconditions:** `FX-004` — TrackAsia `canLaunchUrl → false`, fallback `canLaunchUrl → true`

**Test Steps:**
1. Arrange: mock TrackAsia scheme false, fallback true
2. Act: gọi `navigate(latitude: 10.7769, longitude: 106.7009)`
3. Assert: `launchUrl` được gọi với URI fallback (`geo:` hoặc Google Maps), method trả `true`

**Expected Result (PASS):**
- Fallback map mở thành công, hành động KHÔNG thất bại hoàn toàn

**Expected Result (FAIL):**
- `navigate()` trả `false`/throw khi TrackAsia không khả dụng thay vì fallback

**Current Status:** 🔴 Not written

---

### QA-TC-005 — logAction() lưu đúng dữ liệu tối thiểu, không lưu PII thừa

**Severity:** `CRITICAL`
**Feature Under Test:** `QuickActionService.logAction()` (Java)
**Test File:** `src/test/java/com/carebridge/backend/map/service/QuickActionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MAP-007`, `V1__init_schema.sql` sync migration schema (§5.2 TDS)

**Preconditions:** `FX-008`, `FX-006`

**Test Steps:**
1. Arrange: mock `IQuickActionLogRepository.save()` trả về entity với id sinh ra
2. Act: gọi `logAction(makeRequest(), makeMotherUserId())`
3. Assert: `save()` được gọi với `QuickActionLog` có đúng `userId`, `facilityId`, `actionType`; response trả `quickActionLogId` + `createdAt`

**Expected Result (PASS):**
- Entity lưu đúng field, không có field nào khác ngoài schema (§5.2 TDS: chỉ `user_id, facility_id, action_type, created_at`)

**Expected Result (FAIL):**
- Entity lưu thêm field không có trong schema (vd: phone number) hoặc thiếu field bắt buộc

**Current Status:** 🔴 Not written

---

### QA-TC-006 — Log API lỗi DB → trả lỗi rõ ràng, không crash

**Severity:** `HIGH`
**Feature Under Test:** `QuickActionController` error handling
**Test File:** `src/test/java/com/carebridge/backend/map/controller/QuickActionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `SRS UC-64 E3`, `TDS §10 MAP-105`

**Preconditions:** `FX-005` — repository throws DataAccessException

**Test Steps:**
1. Arrange: mock Service throws exception mô phỏng DB down
2. Act: `POST /api/v1/map/quick-actions/log`
3. Assert: response 503, `error.code == "MAP-105"`, không có stack trace lộ ra client

**Expected Result (PASS):**
- 503 với error code chuẩn, response body không chứa exception message nội bộ

**Expected Result (FAIL):**
- 500 generic không có error code, hoặc lộ stack trace

**Current Status:** 🔴 Not written

---

### QA-TC-007 — RBAC: role khác ROLE_MOTHER → 403 MAP-104

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `QuickActionController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/QuickActionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-MAP-008`

**Preconditions:** JWT `FX-007` (ROLE_EXPERT)

**Test Steps (Attack Simulation):**
1. Act: `POST /api/v1/map/quick-actions/log` với JWT ROLE_EXPERT
2. Assert: 403, `error.code == "MAP-104"`

**Expected Result (PASS = hệ thống an toàn):** 403 Forbidden
**Expected Result (FAIL = lỗ hổng tồn tại):** 201 Created cho role không được phép

**Current Status:** 🔴 Not written

---

### QA-TC-008 — Invalid actionType → 400 MAP-101

**Severity:** `MEDIUM`
**Feature Under Test:** `QuickActionController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/QuickActionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 MAP-101`

**Test Steps:**
1. Act: `POST /api/v1/map/quick-actions/log` với `{"actionType": "SMS"}`
2. Assert: 400, `error.code == "MAP-101"`

**Expected Result (PASS):** 400 với error code MAP-101
**Expected Result (FAIL):** Request được xử lý với actionType không hợp lệ

**Current Status:** 🔴 Not written

---

### QA-TC-009 — facilityId không tồn tại → 404 MAP-103

**Severity:** `MEDIUM`
**Feature Under Test:** `QuickActionService.logAction()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/QuickActionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §10 MAP-103`

**Preconditions:** Mock `ICareFacilityRepository.findById()` → `Optional.empty()`

**Test Steps:**
1. Arrange: request với `facilityId` không tồn tại
2. Act: gọi `logAction(request, userId)`
3. Assert: throws exception tương ứng `MAP-103`, không tạo `QuickActionLog`

**Expected Result (PASS):** Exception `MAP-103`, không có INSERT xảy ra
**Expected Result (FAIL):** Log vẫn được tạo dù `facilityId` không tồn tại

**Current Status:** 🔴 Not written

---

### QA-TC-010 — facilityId null → vẫn 201 hợp lệ (hotline không gắn facility)

**Severity:** `MEDIUM`
**Feature Under Test:** `QuickActionService.logAction()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/QuickActionServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §5.2 (facility_id nullable)`, `FX-009`

**Test Steps:**
1. Arrange: request với `facilityId = null`, `actionType = CALL`
2. Act: gọi `logAction(request, userId)`
3. Assert: log tạo thành công, `facilityId` trong entity là `null`, không lookup `ICareFacilityRepository`

**Expected Result (PASS):** 201-equivalent response, không throw
**Expected Result (FAIL):** NullPointerException hoặc validation reject facilityId null

**Current Status:** 🔴 Not written

---

### QA-TC-011 — Không có JWT → 401 IAM-001

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306`
**Feature Under Test:** `Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/QuickActionControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Act: `POST /api/v1/map/quick-actions/log` không có `Authorization` header
2. Assert: 401, `error.code == "IAM-001"`

**Expected Result (PASS = an toàn):** 401 Unauthorized
**Expected Result (FAIL = lỗ hổng):** 201 Created không cần auth

**Current Status:** 🔴 Not written

---

### QA-TC-012 — userId PHẢI từ JWT, không override qua request body

**Severity:** `CRITICAL`
**CWE:** `CWE-639`
**Feature Under Test:** `QuickActionController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/QuickActionControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-MAP-008`

**Test Steps (Attack Simulation):**
1. Arrange: JWT với `sub=mother-001`; request body không có field `userId` (API contract không expose param này — xem §9 TDS)
2. Act: gọi endpoint, mock Service để capture `userId` argument
3. Assert: `userId` truyền vào Service khớp JWT `sub`, không thể ghi đè qua body/query

**Expected Result (PASS = an toàn):** `userId` luôn từ JWT
**Expected Result (FAIL = lỗ hổng):** Endpoint chấp nhận `userId` field tùy ý từ client

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### QA-TC-INT-001 — Full flow: API → DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/map/quick-actions/log → quick_action_logs INSERT`
**Test File:** `src/test/java/com/carebridge/backend/map/QuickActionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL Testcontainer, Flyway migration bao gồm `V20260701093000__create_quick_action_logs.sql` applied
- Seed `care_facilities` với `FX-001`

**Test Steps:**
1. Seed `care_facilities`
2. Call `POST /api/v1/map/quick-actions/log` với JWT ROLE_MOTHER, `facilityId=FX-001.facilityId`, `actionType=CALL`
3. Assert DB: `quick_action_logs` có 1 record mới với đúng `user_id`, `facility_id`, `action_type='CALL'`

**Expected Result (PASS):**
- Response 201, DB có record khớp

**Expected Result (FAIL):**
- Record không được tạo, hoặc FK constraint fail không đúng lý do

**DB Assertion:**
```java
List<QuickActionLog> logs = quickActionLogRepository.findAll();
assertThat(logs).hasSize(1);
assertThat(logs.get(0).getActionType()).isEqualTo(QuickActionType.CALL);
assertThat(logs.get(0).getFacilityId()).isEqualTo(seededFacilityId);
```

**Current Status:** 🔴 Not written

---

### QA-TC-013 — Mobile: logActionFireAndForget() KHÔNG được await trước call()/navigate()

**Severity:** `HIGH`
**Feature Under Test:** `QuickActionService` (Dart) — thứ tự thực thi
**Test File:** `test/features/emergencyMap/services/quick_action_service_order_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-MAP-007`

**Preconditions:**
- Mock HTTP client cho log API với delay giả lập (vd: 2 giây); mock `url_launcher` trả về ngay lập tức

**Test Steps:**
1. Arrange: mock log API có độ trễ 2s; mock launcher trả về instant
2. Act: gọi hàm cấp cao (vd: `onTapCallButton()`) đo thời điểm `launchUrl` được gọi
3. Assert: `launchUrl` hoàn tất TRƯỚC KHI log API call resolve — tức là code không `await` log call trước khi launch dialer

**Expected Result (PASS):**
- `launchUrl` gọi xong trong < 300ms bất kể log API delay 2s

**Expected Result (FAIL):**
- UI bị block chờ log API trước khi mở dialer (vi phạm ADR-MAP-007/C2)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `QA-TC-001` | `quick_action_service_test.dart` | `[ ]` | `[ ]` | |
| `QA-TC-002` | `quick_action_service_test.dart` | `[ ]` | `[ ]` | |
| `QA-TC-003` | `quick_action_service_test.dart` | `[ ]` | `[ ]` | |
| `QA-TC-004` | `quick_action_service_test.dart` | `[ ]` | `[ ]` | |
| `QA-TC-005` | `QuickActionServiceTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-006` | `QuickActionControllerTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-007` | `QuickActionControllerTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-008` | `QuickActionControllerTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-009` | `QuickActionServiceTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-010` | `QuickActionServiceTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-011` | `QuickActionControllerTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-012` | `QuickActionControllerTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-INT-001` | `QuickActionIntegrationTest.java` | `[ ]` | `[ ]` | |
| `QA-TC-013` | `quick_action_service_order_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase (Backend):**

```java
@Service
public class QuickActionService implements IQuickActionService {

    @Override
    public QuickActionLogResponse logAction(QuickActionLogRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Stub cho Red Phase (Mobile):**

```dart
class QuickActionServiceImpl implements QuickActionService {
  @override
  Future<bool> call(String phoneNumber) {
    throw UnimplementedError('Not implemented — Red Phase stub');
  }

  @override
  Future<bool> navigate({required double latitude, required double longitude, String? label}) {
    throw UnimplementedError('Not implemented — Red Phase stub');
  }

  @override
  void logActionFireAndForget({required String? facilityId, required String actionType}) {
    throw UnimplementedError('Not implemented — Red Phase stub');
  }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `QA-TC-001` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-002` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-003` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-004` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-005` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-006` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-007` | `throw` (via Controller → Service) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-008` | N/A — validation trước Service | 🔴 FAIL (nếu chưa có `@Valid`) | ☐ FAIL ☐ PASS | |
| `QA-TC-009` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-010` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-011` | N/A — Security filter chain | 🔴 FAIL (nếu security config chưa có) | ☐ FAIL ☐ PASS | |
| `QA-TC-012` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-INT-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `QA-TC-013` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (điền khi bắt đầu implement)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-002` đã được review và approve (hiện tại `Draft`)
- [ ] Logic Issues (Section 2) đã confirm với Principal Architect / TV4-Lâm — **đặc biệt L1 (ZegoCloud conflict)** cần quyết định rõ ràng trước khi implement
- [ ] Flyway migration `V20260701093000__create_quick_action_logs.sql` đã approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã chuẩn bị
- [ ] TrackAsia deep-link scheme (nếu dùng SDK mới) đã được approval theo CLAUDE.md ("no new dependencies without approval")

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `flutter test` — tất cả mobile unit tests xanh
- [ ] Test coverage ≥ 80% lines cho `QuickActionService` (cả Java và Dart)
- [ ] Không có business logic trong `QuickActionController` (chỉ validation + mapping)
- [ ] Không có PII (số điện thoại) xuất hiện plaintext trong logs hoặc DB ngoài schema đã định nghĩa
- [ ] Xác nhận KHÔNG có import ZegoCloud trong `map` package hoặc `emergencyMap` mobile feature

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/SRS)

### Suspension Criteria (Điều kiện tạm dừng)

- Chưa có quyết định cuối cùng về ZegoCloud secondary actor (Open item TDS §2) — có thể tạm triển khai theo ADR-MAP-005 (native dialer) nhưng PHẢI ghi rõ giả định này trong PR description
- TrackAsia SDK/dependency chưa được approval chính thức

---

## 7. Rollback Plan

```bash
# Revert migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS quick_action_logs CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260701000001';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/
git checkout -- src/main/resources/db/migration/V20260701093000__create_quick_action_logs.sql
git checkout -- src/test/java/com/carebridge/backend/map/
git checkout -- lib/features/emergencyMap/services/quick_action_service*.dart
git checkout -- test/features/emergencyMap/services/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume ZegoCloud integration không có ADR xác nhận (vi phạm §2 L1) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (vd: tính toán route trong Controller) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `ZegoCloudService`/service không tồn tại trong §8 TDS | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec v1.0 — Draft. Chưa Approved. Xem TDS §2 (Ma trận Truy vết — Open item ZegoCloud conflict) trước khi chuyển Status sang `Approved`.*
