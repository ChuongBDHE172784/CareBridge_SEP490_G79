# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC14 — Báo cáo Nội dung hoặc Tài khoản (Report Content or Account)

**Document ID:** `CB-MOD-IMP-014-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC14_ReportContentOrAccount/UC14_ReportContentOrAccount_TDS.md` (CB-MOD-IMP-014 v1.0) — Technical Design Spec
- `01_Requirements/SRS.md` — UC-14 Functional requirements
- `02_Design/ADR/` — Architecture Decision Records liên quan
- Business Rules: BR-MOD-001 (self-report forbidden), BR-MOD-002 (rate limit 5/24h)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-06-26` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC14 ReportContentOrAccount |
| `2026-07-04` | `AI Agent` | Approved by user — proceeding to implementation |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
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
| **Feature / Gap ID** | `UC-14` |
| **Module** | `Report Content or Account — Moderation Bounded Context` |
| **Spec gốc** | `CB-MOD-IMP-014` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S3 (2026-07-01 → 2026-07-14)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `N/A` |
| **Upstream Dependencies** | `IAM Module (JWT), User Module (target lookup), Post/Comment Module (target lookup)` |
| **Downstream Consumers** | `Moderation Dashboard, Audit Service` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MOD-IMP-014 §17`, `BR-MOD-001`, `BR-MOD-002` |
| **Constraints Injected** | `C1: reporterId từ JWT; C2: rate limit rolling 24h; C3: duplicate check same reporter + same target; C4: emit MODERATION_ACTION audit event; C5: status khởi tạo là PENDING` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> **Bắt buộc điền trước khi viết test.**
> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.
> Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Self-report check có thể dùng `reporterId` từ request body | `reporterId` PHẢI lấy từ JWT `userId` claim — không tin request body | Test mock SecurityContext để inject userId, không truyền reporterId trong request body |
| L2 | Rate limit "5 reports per day" có thể hiểu là calendar day (00:00-23:59) | Rate limit là **rolling window 24 giờ** tính từ thời điểm report đầu tiên — không phải calendar day | Test fixture seed 5 reports trong 23h59m trước → expect 429; seed 5 reports > 24h trước → expect 201 |
| L3 | Duplicate check có thể so sánh theo reason | Duplicate report check là **cùng reporter + cùng target** — bất kể reason | Test: reporter A + target B với reason khác nhau → vẫn 400 MOD-004 |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
UC14 ReportContentOrAccount bao gồm các layer:
├── Domain (ContentReport entity, ContentReportTargetType enum, ReportStatus enum)
├── Application / Use Cases (mock ContentReportRepository với Mockito)
├── Services (ContentReportService — mock repo và audit service)
├── Controller (ContentReportController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

**Ngoài phạm vi:**
- Moderation review flow (UC khác)
- Admin ban action sau khi report được review
- Notification flow khi report được xử lý

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-14` | Submit report, actor: all authenticated users |
| `BR-MOD-001` | Reporter không thể report chính mình |
| `BR-MOD-002` | Rate limit 5 reports per user per 24h rolling window |
| `CB-MOD-IMP-014 §10` | Error codes MOD-001, MOD-002, MOD-003, MOD-004 |
| `CB-MOD-IMP-014 §7` | Domain event MODERATION_ACTION |
| `RBAC Policy` | Yêu cầu JWT Bearer token hợp lệ cho tất cả roles |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Reporter hợp lệ, target tồn tại, chưa vượt rate limit | `ContentReportService.submitReport()` | `RPT-TC-014-001` |
| TC-COND-002 | Reporter ID từ JWT trùng với targetId | `ContentReportService.checkSelfReport()` | `RPT-TC-014-002` |
| TC-COND-003 | Reporter đã gửi ≥ 5 reports trong 24h rolling window | `ContentReportService.checkRateLimit()` | `RPT-TC-014-003` |
| TC-COND-004 | targetId không tồn tại trong DB | `ContentReportService.validateTarget()` | `RPT-TC-014-004` |
| TC-COND-005 | Cùng reporter + cùng targetId đã tồn tại trong DB | `ContentReportRepository.existsByReporterIdAndTargetId()` | `RPT-TC-014-005` |
| TC-COND-006 | targetType có giá trị không hợp lệ (ngoài enum) | `ContentReportController` — Bean Validation | `RPT-TC-014-006` |
| TC-COND-007 | Request không có JWT Bearer token | Spring Security filter chain | `RPT-TC-014-007` |
| TC-COND-008 | Report được lưu với status PENDING, audit event được emit | Full flow integration | `RPT-TC-014-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | targetType enum values (valid / invalid) | Phân loại rõ ràng các giá trị hợp lệ và không hợp lệ |
| Boundary Value Analysis | Rate limit: 4 reports (under), 5 reports (at limit), 6 reports (over limit) | Kiểm tra biên 24h rolling window |
| State Transition Testing | ReportStatus: khởi tạo PENDING | Đảm bảo trạng thái ban đầu đúng |
| Error Guessing | Self-report attempt, duplicate report, no JWT | Các lỗi phổ biến trong content moderation |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-MOD-001` | JWT / SecurityContext | `userId = 00000000-0000-0000-0000-000000000014` | Reporter identity (happy path) |
| `FX-MOD-002` | DB seed | `ContentReport` với `reporterId = REPORTER_ID`, `targetId = TARGET_ID`, `createdAt = now() - 23h` | Rate limit boundary test |
| `FX-MOD-003` | Request body | `targetId = bbbbbbbb-0000-0000-0000-000000000014`, `targetType = POST`, `reason = "Nội dung vi phạm"` | Standard valid request |
| `FX-MOD-004` | DB seed | 5 `ContentReport` records với `reporterId = REPORTER_ID`, tất cả trong 24h gần nhất | Rate limit exceeded scenario |
| `FX-MOD-005` | Request body | `targetId = REPORTER_ID` (same as JWT userId) | Self-report test |
| `FX-MOD-006` | Request body | `targetType = "INVALID_ENUM_VALUE"` | Bean validation test |

---

## 4. Test Case Specification

> **TC ID format:** `RPT-TC-014-NNN`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW (theo CVSS)
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state giữa các test cases.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory method
// ═══════════════════════════════════════════════════════════

// ContentReportTestFactory.java
class ContentReportTestFactory {

    static UUID REPORTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    static UUID TARGET_ID   = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000014");

    static ReportRequest makeRequest() {
        ReportRequest req = new ReportRequest();
        req.setTargetId(TARGET_ID);
        req.setTargetType(ContentReportTargetType.POST);
        req.setReason("Nội dung vi phạm cộng đồng");
        return req;
    }

    // Overload để override specific fields
    static ReportRequest makeRequest(Consumer<ReportRequest> overrides) {
        ReportRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    static ContentReport makeSavedReport() {
        ContentReport report = new ContentReport();
        report.setId(UUID.randomUUID());
        report.setReporterId(REPORTER_ID);
        report.setTargetId(TARGET_ID);
        report.setTargetType(ContentReportTargetType.POST);
        report.setReason("Nội dung vi phạm cộng đồng");
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return report;
    }
}
```

---

### RPT-TC-014-001 — Happy path: Submit report hợp lệ → 201 Created

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/reports — ContentReportController.submitReport()`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MOD-IMP-014 §9` — API Specification, HTTP 201 Created

**Preconditions:**
- JWT Bearer token hợp lệ với `userId = REPORTER_ID`
- targetId (TARGET_ID) tồn tại trong DB
- REPORTER_ID ≠ TARGET_ID (không tự báo cáo)
- REPORTER_ID chưa có report nào trong 24h gần nhất
- Không có report nào từ REPORTER_ID đến TARGET_ID trước đó

**Test Steps:**
1. Arrange: mock `ContentReportService.submitReport()` trả về `ContentReportResponse` với status `PENDING`; mock SecurityContext userId = REPORTER_ID
2. Act: `POST /api/v1/reports` với body `ContentReportTestFactory.makeRequest()`
3. Assert: HTTP 201; response body có `id` (non-null UUID), `status = "PENDING"`, `createdAt` (non-null)

**Expected Result (PASS — hành vi đúng):**
- HTTP status: `201 Created`
- Response body: `{ "id": "<uuid>", "status": "PENDING", "createdAt": "<iso-timestamp>" }`
- `ContentReportService.submitReport()` được gọi đúng 1 lần

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 400/500 hoặc response thiếu trường `id` / `status`
- Service không được gọi (controller reject sớm không đúng)

**Current Status:** 🔴 Not written
**Implementation Note:** Controller không được đặt reporterId trong request body — phải extract từ JWT SecurityContext. Service trả về `ContentReportResponse`.

---

### RPT-TC-014-002 — Self-report attempt → 400 MOD-001

**Severity:** `HIGH`
**Feature Under Test:** `ContentReportService.submitReport()` — self-report guard
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-MOD-001` — Reporter không thể report chính mình; `CB-MOD-IMP-014 §10` MOD-001

**Preconditions:**
- JWT userId = `REPORTER_ID`
- Request body có `targetId = REPORTER_ID` (cùng với userId từ JWT)

**Test Steps:**
1. Arrange: mock SecurityContext userId = REPORTER_ID; tạo request với `targetId = REPORTER_ID` (self-target)
2. Act: gọi `ContentReportService.submitReport(request, reporterId=REPORTER_ID)`
3. Assert: throw `ContentModerationException` với code `MOD-001`; HTTP 400

**Expected Result (PASS — hành vi đúng):**
- `ContentModerationException` với `errorCode = "MOD-001"` được throw
- HTTP status: `400 Bad Request`
- Response: `{ "error": { "code": "MOD-001", "message": "Không thể tự báo cáo chính mình" } }`
- Không có DB write nào xảy ra

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 201 (report được tạo thành công — vi phạm BR-MOD-001)
- Exception khác được throw hoặc sai error code

**Current Status:** 🔴 Not written
**Implementation Note:** L1 — reporterId PHẢI lấy từ JWT, không phải request body. So sánh `reporterId.equals(request.getTargetId())` trong service layer trước khi persist.

---

### RPT-TC-014-003 — Rate limit exceeded → 429 MOD-002

**Severity:** `HIGH`
**Feature Under Test:** `ContentReportService.submitReport()` — rate limit guard
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-MOD-002` — 5 reports per user per 24h rolling window; `CB-MOD-IMP-014 §10` MOD-002

**Preconditions:**
- JWT userId = REPORTER_ID
- DB đã có 5 ContentReport records với `reporterId = REPORTER_ID` và `createdAt` trong khoảng `now() - 24h` đến `now()`

**Test Steps:**
1. Arrange: mock `ContentReportRepository.countByReporterIdAndCreatedAtAfter(REPORTER_ID, now().minus(24h))` trả về `5L`
2. Act: gọi `ContentReportService.submitReport(request, REPORTER_ID)`
3. Assert: throw `RateLimitExceededException` với code `MOD-002`; HTTP 429

**Expected Result (PASS — hành vi đúng):**
- `RateLimitExceededException` với `errorCode = "MOD-002"` được throw
- HTTP status: `429 Too Many Requests`
- Response: `{ "error": { "code": "MOD-002", "message": "Vượt quá giới hạn báo cáo (5 lần/24 giờ)" } }`
- Không có DB write nào xảy ra

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 201 (vi phạm rate limit BR-MOD-002)
- Rate limit dùng calendar day thay vì rolling 24h

**Current Status:** 🔴 Not written
**Implementation Note:** L2 — Rolling window: `Instant threshold = Instant.now().minus(Duration.ofHours(24))`. Repository query: `countByReporterIdAndCreatedAtAfter(reporterId, threshold) >= 5`.

---

### RPT-TC-014-004 — Target not found → 404 MOD-003

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentReportService.submitReport()` — target validation
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MOD-IMP-014 §10` — MOD-003 Target not found

**Preconditions:**
- JWT userId = REPORTER_ID
- `request.targetId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")` (không tồn tại)

**Test Steps:**
1. Arrange: mock target lookup service trả về `Optional.empty()` cho targetId không tồn tại
2. Act: gọi `ContentReportService.submitReport(request, REPORTER_ID)`
3. Assert: throw `TargetNotFoundException` với code `MOD-003`; HTTP 404

**Expected Result (PASS — hành vi đúng):**
- `TargetNotFoundException` với `errorCode = "MOD-003"` được throw
- HTTP status: `404 Not Found`
- Response: `{ "error": { "code": "MOD-003", "message": "Không tìm thấy mục tiêu báo cáo" } }`

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 201 hoặc report được lưu với targetId không tồn tại
- HTTP 500 thay vì 404

**Current Status:** 🔴 Not written
**Implementation Note:** Service phải validate target tồn tại theo `targetType` (POST, COMMENT, EXPERT, USER) trước khi persist.

---

### RPT-TC-014-005 — Duplicate report (same reporter + same target) → 400 MOD-004

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentReportService.submitReport()` — duplicate check
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MOD-IMP-014 §10` — MOD-004 Already reported same target

**Preconditions:**
- JWT userId = REPORTER_ID
- DB đã có `ContentReport` với `reporterId = REPORTER_ID` và `targetId = TARGET_ID`
- Request có cùng `targetId = TARGET_ID` nhưng khác reason

**Test Steps:**
1. Arrange: mock `ContentReportRepository.existsByReporterIdAndTargetId(REPORTER_ID, TARGET_ID)` trả về `true`
2. Act: gọi `ContentReportService.submitReport(request, REPORTER_ID)` với request có reason khác
3. Assert: throw `DuplicateReportException` với code `MOD-004`; HTTP 400

**Expected Result (PASS — hành vi đúng):**
- `DuplicateReportException` với `errorCode = "MOD-004"` được throw
- HTTP status: `400 Bad Request`
- Response: `{ "error": { "code": "MOD-004", "message": "Bạn đã báo cáo mục tiêu này rồi" } }`

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 201 (duplicate report được tạo — vi phạm uniqueness rule)
- Duplicate check dùng reason thay vì reporter + target

**Current Status:** 🔴 Not written
**Implementation Note:** L3 — Duplicate check theo `(reporterId, targetId)` — bất kể reason. Không phải `(reporterId, targetId, reason)`.

---

### RPT-TC-014-006 — Invalid targetType enum value → 400 Bean Validation

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentReportController.submitReport()` — Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MOD-IMP-014 §9` — Request Validation

**Preconditions:**
- JWT Bearer token hợp lệ
- Request body có `targetType = "INVALID_TYPE"` (không phải POST, COMMENT, EXPERT, USER)

**Test Steps:**
1. Arrange: mock SecurityContext với valid userId
2. Act: `POST /api/v1/reports` với body `{ "targetId": "...", "targetType": "INVALID_TYPE", "reason": "test" }`
3. Assert: HTTP 400; response có validation error message; `ContentReportService` KHÔNG được gọi

**Expected Result (PASS — hành vi đúng):**
- HTTP status: `400 Bad Request`
- Response body chứa validation error về `targetType` field
- `ContentReportService.submitReport()` không được gọi (controller reject tại validation layer)

**Expected Result (FAIL — dấu hiệu lỗi):**
- HTTP 201 hoặc HTTP 500
- Service được gọi với giá trị enum không hợp lệ

**Current Status:** 🔴 Not written
**Implementation Note:** `@Valid` annotation trên request body trong controller; `ContentReportTargetType` enum tự động gây `HttpMessageNotReadableException` khi Jackson deserialize.

---

### RPT-TC-014-007 — Không có JWT Bearer token → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `POST /api/v1/reports` — Spring Security filter
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-MOD-IMP-014 §9` — Auth: JWT Bearer required

**Preconditions:**
- Không có `Authorization` header trong request

**Test Steps (Attack Simulation):**
1. Arrange: không cấu hình mock SecurityContext
2. Act: `POST /api/v1/reports` không có `Authorization: Bearer ...` header
3. Assert: HTTP 401; `ContentReportService` không được gọi

**Expected Result (PASS = hệ thống an toàn):**
- HTTP status: `401 Unauthorized`
- `ContentReportService.submitReport()` không được gọi

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP 201 hoặc 403 (authenticated tự động hoặc sai error code)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSQLContainer`). Timeout: 120s.

---

### RPT-TC-014-INT-001 — Integration: Report được lưu vào DB với status PENDING

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/reports → ContentReportService → ContentReportRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/content/ContentReportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-008`
**Oracle Source:** `CB-MOD-IMP-014 §14` — DB Verification

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied (ContentReport table tồn tại)
- Seed: user với `id = REPORTER_ID` tồn tại trong `users` table
- Seed: target post với `id = TARGET_ID` tồn tại trong relevant table
- Không có existing report từ REPORTER_ID đến TARGET_ID

**Test Steps:**
1. Seed minimal data: insert user + target into DB via JPA
2. Tạo JWT token với `userId = REPORTER_ID`
3. `POST /api/v1/reports` với `ContentReportTestFactory.makeRequest()` và Authorization header
4. Assert HTTP 201
5. Query DB để verify record

**Expected Result (PASS):**
- HTTP status `201 Created`
- DB assertion: record tồn tại với đúng field values
- `status = PENDING` (không phải REVIEWED hay DISMISSED)
- Audit event `MODERATION_ACTION` được emit

**Expected Result (FAIL):**
- Record không tồn tại trong DB sau khi API trả 201
- `status != PENDING` khi mới tạo

**DB Assertion:**
```java
// ContentReportIntegrationTest.java
ContentReport saved = contentReportRepository
    .findByReporterIdAndTargetId(REPORTER_ID, TARGET_ID)
    .orElseThrow();
assertThat(saved).isNotNull();
assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
assertThat(saved.getReporterId()).isEqualTo(ContentReportTestFactory.REPORTER_ID);
assertThat(saved.getTargetId()).isEqualTo(ContentReportTestFactory.TARGET_ID);
assertThat(saved.getTargetType()).isEqualTo(ContentReportTargetType.POST);
assertThat(saved.getReason()).isEqualTo("Nội dung vi phạm cộng đồng");
assertThat(saved.getCreatedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RPT-TC-014-001` | `ContentReportControllerTest.java` | `[ ]` | ` ` | Extract reporterId extraction sang SecurityUtils |
| `RPT-TC-014-002` | `ContentReportServiceTest.java` | `[ ]` | ` ` | Extract self-report check sang private method |
| `RPT-TC-014-003` | `ContentReportServiceTest.java` | `[ ]` | ` ` | Extract rate limit check sang RateLimitPolicy class |
| `RPT-TC-014-004` | `ContentReportServiceTest.java` | `[ ]` | ` ` | Extract target validation theo Strategy Pattern (per targetType) |
| `RPT-TC-014-005` | `ContentReportServiceTest.java` | `[ ]` | ` ` | ` ` |
| `RPT-TC-014-006` | `ContentReportControllerTest.java` | `[ ]` | ` ` | ` ` |
| `RPT-TC-014-007` | `ContentReportControllerTest.java` | `[ ]` | ` ` | ` ` |
| `RPT-TC-014-INT-001` | `ContentReportIntegrationTest.java` | `[ ]` | ` ` | ` ` |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub.
> Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ContentReportService implements IContentReportService {

    @Override
    public ContentReportResponse submitReport(ReportRequest request, UUID reporterId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RPT-TC-014-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `RPT-TC-014-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-007` | N/A (Spring Security) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `RPT-TC-014-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence.log`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MOD-IMP-014` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm: L1 (JWT source), L2 (rolling window), L3 (duplicate check logic)
- [ ] `content_reports` table tồn tại trong DB (hoặc Flyway migration approved)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị trong test environment

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test -pl 05_Development/CareBridgeAPI` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify -pl 05_Development/CareBridgeAPI` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ContentReportService` class
- [ ] Không có business logic trong `ContentReportController` (chỉ có validation + mapping)
- [ ] `reporterId` luôn lấy từ JWT, không có trong request body
- [ ] `status` mặc định là `PENDING` khi tạo mới

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " \
    src/test/java/com/carebridge/backend/content/ContentReportServiceTest.java
  # Mọi instance PHẢI nằm trong @Test hoặc dùng ContentReportTestFactory
  ```
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR-MOD-001, BR-MOD-002, MOD-00X)

### Suspension Criteria (Điều kiện tạm dừng)

- `content_reports` table chưa được migrate vào staging
- Flyway migration conflict với migration khác
- CI pipeline bị broken bởi thay đổi khác trên branch `dev`

---

## 7. Rollback Plan

```bash
# Nếu cần rollback implementation (không có migration mới theo TDS)
# ContentReport entity ĐÃ TỒN TẠI — không cần drop table

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
# Không cần rollback migration vì entity đã tồn tại trước UC-14
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ Checklist cho reviewer khi test cases được AI hỗ trợ generate.

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(chưa có)_ | ` ` | ` ` | ` ` | ☐ |

**Ghi chú CASE 2.0 cho UC-14:**
- AP-AI-005 risk: `ContentReportTargetType` và `ReportStatus` enum PHẢI tồn tại trong `com.carebridge.backend.content` package trước khi viết test. Verify bằng `./mvnw compile`.
- AP-AI-003 risk: Rate limit check implement ở Service layer (không phải Filter) — documented trong TDS §AI Prompt Constraints.

---

*TDD Spec v1.0 — UC14 ReportContentOrAccount*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Document ID: CB-MOD-IMP-014-TEST*
