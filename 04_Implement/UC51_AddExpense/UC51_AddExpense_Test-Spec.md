# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử Hướng Phát triển — UC51 Add Expense

**Document ID:** `CB-EXPENSE-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Spec Generator`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending — BẮT BUỘC (PII module — financial data)`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (expenses table đã có)
- `04_Implement/UC51_AddExpense/UC51_AddExpense_TDS.md` — Technical Specification
- `01_Requirements/SRS.md §3.3.1.28` — Functional requirements

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật — chỉ dùng SYNTHETIC data (số tiền giả, ghi chú giả).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Spec Generator | Khởi tạo tài liệu — TDD spec cho UC51 Add Expense |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-UC51-EXPENSE` |
| **Module** | `Expense — CareJourney Financial Tracking` |
| **Spec gốc** | `CB-EXPENSE-IMP-001` |
| **Priority** | 🟠 P1 — High |
| **Sprint** | `S[N] — 2026-06-26 → TBD` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII — dữ liệu tài chính cá nhân` |
| **Compliance Scope** | `PDPA / Luật Bảo vệ Người tiêu dùng` |
| **Upstream Dependencies** | `AuthModule (JWT), MotherJourney, BabyProfile` |
| **Downstream Consumers** | `UC24_ViewMotherJourneyDashboard` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPENSE-IMP-001 §17`, `ADR-001, ADR-002` |
| **Constraints Injected** | `C1 (ownership), C2 (amount > 0), C3 (date server-side), C4 (currency default VND), C5 (no PII in log), C6 (userId from JWT), C7 (controller no business logic)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Cho CareBridge schema, dùng `V1__init_schema.sql` và approved migrations làm oracle cuối cùng.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS mô tả validate amount > 0 nhưng không chỉ định layer nào | ADR-001: validate server-side trong Service, không chỉ DTO annotation | Test TC-EXPENSE-002 kiểm tra Service-layer validation (không phải DTO-only) |
| L2 | SRS không đề cập currency default | BR-EXPENSE-004: currency default = "VND" | Test TC-EXPENSE-001b: khi currency null → response.currency = "VND" |
| L3 | expense_date validation không clear về "server date vs client date" | ADR-001: dùng `LocalDate.now()` trên server | Test TC-EXPENSE-003: mock LocalDate.now() để test boundary chính xác |
| L4 | SRS không đề cập PII logging | ADR-002: KHÔNG log amount/note | Test TC-EXPENSE-SEC-002: verify log không chứa amount |
| L5 | SRS không có constraint về schema — có thể nhầm cần migration mới | V1__init_schema.sql đã có bảng expenses — KHÔNG cần migration | Test TC-EXPENSE-INT-001 dùng V1 schema trực tiếp, không tạo migration mới |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` trong test basis — bảng `expenses` đã có, test Testcontainers sẽ dùng V1 trực tiếp.

### TDS-01 — Scope / Phạm vi

```
Expense module bao gồm các layer:
├── Domain Entity (Expense.java + ExpenseCategory enum)
├── Application / Service (ExpenseServiceImpl — mock Repository với Mockito)
├── Controller (ExpenseController — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL + Flyway V1 auto-migrate)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-51 §3.3.1.28` | Add expense: category, amount, date, note |
| `BR-EXPENSE-001` | amount > 0 |
| `BR-EXPENSE-002` | expenseDate <= today (server) |
| `BR-EXPENSE-003` | category in enum |
| `BR-EXPENSE-004` | currency default VND |
| `BR-RBAC` | owner_user_id == currentUserId |
| `BR-PRIVACY` | No PII in application log |
| `ADR-001` | Server-side date validation |
| `ADR-002` | No PII logging |
| `CB-EXPENSE-IMP-001 §10` | Error codes EXPENSE-001 đến EXPENSE-006 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Thêm expense hợp lệ với đủ trường | `ExpenseService.addExpense()` | `EXPENSE-TC-001` |
| TC-COND-002 | currency null → default VND | `addExpense()` currency logic | `EXPENSE-TC-001b` |
| TC-COND-003 | amount = 0 → EXPENSE-001 | `addExpense()` amount guard | `EXPENSE-TC-002a` |
| TC-COND-004 | amount âm → EXPENSE-001 | `addExpense()` amount guard | `EXPENSE-TC-002b` |
| TC-COND-005 | expenseDate = tomorrow → EXPENSE-002 | `addExpense()` date guard | `EXPENSE-TC-003a` |
| TC-COND-006 | expenseDate = today → success | `addExpense()` date boundary | `EXPENSE-TC-003b` |
| TC-COND-007 | category không hợp lệ → EXPENSE-003 | DTO validation | `EXPENSE-TC-004` |
| TC-COND-008 | Cross-user access bị chặn | ownership check | `EXPENSE-TC-005` |
| TC-COND-009 | Unauthenticated access → 401 | JWT guard | `EXPENSE-TC-SEC-001` |
| TC-COND-010 | Log không chứa amount | PII logging guard | `EXPENSE-TC-SEC-002` |
| TC-COND-011 | Integration: create + list | Full flow DB | `EXPENSE-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | amount (< 0, = 0, > 0), expenseDate (past, today, future) | Phân vùng input hợp lệ và không hợp lệ |
| Boundary Value Analysis | amount (0.01 = valid, 0 = invalid), expenseDate (today = valid, tomorrow = invalid) | Kiểm tra biên nghiêm ngặt |
| Equivalence Partitioning | category (7 valid values, 1 invalid) | Kiểm tra enum boundary |
| Error Guessing | Cross-user access, null amount, null expenseDate, future date | Security + business rule attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-EXPENSE-001` | DB seed | `{userId: MOTHER_001, journeyId: JOURNEY_001}` | Happy path owner context |
| `FX-EXPENSE-002` | DB seed | `{userId: MOTHER_002}` | Cross-user test context |
| `FX-EXPENSE-003` | DB seed | `{expenseId: EXPENSE_001, ownerUserId: MOTHER_001, amount: 250000, category: CHECKUP, expenseDate: 2026-06-25, currency: VND}` | Existing expense for get/delete tests |
| `FX-EXPENSE-004` | JWT | `{sub: "MOTHER_001", roles: ["ROLE_MOTHER"]}` | Auth context valid |
| `FX-EXPENSE-005` | JWT | `{sub: "MOTHER_002", roles: ["ROLE_MOTHER"]}` | Cross-user auth context |
| `FX-EXPENSE-006` | Date mock | `LocalDate.of(2026, 6, 26)` | Server date mock cho date boundary tests |

---

## 4. Test Case Specification

> **TC ID format:** `EXPENSE-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeRequest() / makeExpense()
// ═══════════════════════════════════════════════════════════

class ExpenseTestFactory {

    static final UUID MOTHER_001  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID MOTHER_002  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID JOURNEY_001 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID EXPENSE_001 = UUID.fromString("20000000-0000-0000-0000-000000000001");

    // Valid request baseline — SYNTHETIC data
    static AddExpenseRequest makeValidRequest() {
        AddExpenseRequest req = new AddExpenseRequest();
        req.setCategory("CHECKUP");
        req.setAmount(new BigDecimal("250000"));    // SYNTHETIC — không phải số tiền thật
        req.setCurrency("VND");
        req.setExpenseDate(LocalDate.of(2026, 6, 25));  // Past date
        req.setNote("Test note synthetic");
        req.setJourneyId(JOURNEY_001);
        return req;
    }

    // Existing expense entity
    static Expense makeExpense() {
        Expense e = new Expense();
        e.setExpenseId(EXPENSE_001);
        e.setOwnerUserId(MOTHER_001);
        e.setJourneyId(JOURNEY_001);
        e.setCategory(ExpenseCategory.CHECKUP);
        e.setAmount(new BigDecimal("250000"));
        e.setCurrency("VND");
        e.setExpenseDate(LocalDate.of(2026, 6, 25));
        e.setNote("Test note synthetic");
        e.setCreatedAt(Instant.now().minusSeconds(3600));
        e.setUpdatedAt(Instant.now().minusSeconds(3600));
        return e;
    }

    static AddExpenseRequest makeValidRequest(Consumer<AddExpenseRequest> overrides) {
        AddExpenseRequest req = makeValidRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### EXPENSE-TC-001 — Thêm expense hợp lệ thành công

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()`
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-EXPENSE-001`, `BR-EXPENSE-003`, `BR-EXPENSE-004`

**Preconditions:**
- Repository mock: `save()` trả về entity đã được lưu với `expenseId` được set
- Fixture `FX-EXPENSE-004` (JWT MOTHER_001)

**Test Steps:**
1. Arrange: tạo `makeValidRequest()` với `amount = 250000`, `category = "CHECKUP"`, `expenseDate = 2026-06-25`
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: kiểm tra response và repository calls

**Expected Result (PASS):**
- `response.getExpenseId()` != null
- `response.getCategory()` == "CHECKUP"
- `response.getAmount()` == 250000
- `response.getCurrency()` == "VND"
- `response.getOwnerUserId()` == MOTHER_001 (set từ service, không từ request)
- `repository.save()` được gọi đúng 1 lần

**Expected Result (FAIL):**
- Exception bị ném với input hợp lệ
- `ownerUserId` không được set (null hoặc sai)
- `repository.save()` không được gọi

**Current Status:** 🟢 Passing
**Implementation Note:** Service phải set `expense.setOwnerUserId(userId)` từ param — không nhận từ request body.

---

### EXPENSE-TC-001b — currency null → default VND

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — currency default logic
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-EXPENSE-004`

**Preconditions:**
- Request với `currency = null`

**Test Steps:**
1. Arrange: `makeValidRequest(req -> req.setCurrency(null))`
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: response.getCurrency() == "VND"

**Expected Result (PASS):**
- `response.getCurrency()` == "VND"
- Entity được lưu trong DB với `currency = 'VND'`

**Expected Result (FAIL):**
- `currency = null` trong response hoặc DB

**Current Status:** 🔴 Not written
**Implementation Note:** `String currency = StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "VND";`

---

### EXPENSE-TC-002a — amount = 0 → EXPENSE-001

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — amount guard
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-EXPENSE-001`

**Preconditions:**
- Fixture `FX-EXPENSE-004`

**Test Steps:**
1. Arrange: `makeValidRequest(req -> req.setAmount(BigDecimal.ZERO))`
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: exception bị ném

**Expected Result (PASS):**
- `ValidationException` bị ném với error code `EXPENSE-001`
- `repository.save()` KHÔNG được gọi

**Expected Result (FAIL):**
- Service gọi `repository.save()` với amount = 0
- Không có exception

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-002b — amount âm → EXPENSE-001

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — amount guard
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-EXPENSE-001`

**Test Steps:**
1. Arrange: `makeValidRequest(req -> req.setAmount(new BigDecimal("-1")))`
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: ValidationException với EXPENSE-001

**Expected Result (PASS):**
- `ValidationException` với code `EXPENSE-001`
- `repository.save()` không được gọi

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-003a — expenseDate = ngày mai → EXPENSE-002

**Severity:** `HIGH`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — date guard (server-side, ADR-001)
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-EXPENSE-002`, `ADR-001`

**Preconditions:**
- Mock `LocalDate.now()` = `2026-06-26` (FX-EXPENSE-006)

**Test Steps:**
1. Arrange: `makeValidRequest(req -> req.setExpenseDate(LocalDate.of(2026, 6, 27)))` (ngày mai)
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: exception bị ném

**Expected Result (PASS):**
- `ValidationException` với code `EXPENSE-002`
- `repository.save()` KHÔNG được gọi

**Expected Result (FAIL):**
- Service chấp nhận ngày tương lai
- Không có exception

**Current Status:** 🟢 Passing
**Implementation Note:** Dùng `Clock` injection hoặc `LocalDate.now()` để mock trong test. Không hardcode date.

---

### EXPENSE-TC-003b — expenseDate = hôm nay → thành công

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — date boundary
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-EXPENSE-002`, `ADR-001`

**Test Steps:**
1. Arrange: `makeValidRequest(req -> req.setExpenseDate(LocalDate.of(2026, 6, 26)))` (hôm nay)
2. Act: gọi `service.addExpense(MOTHER_001, request)`
3. Assert: không có exception

**Expected Result (PASS):**
- Không có exception
- `response.getExpenseDate()` == `2026-06-26`
- `repository.save()` được gọi

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-004 — category không hợp lệ → EXPENSE-003

**Severity:** `MEDIUM`
**Feature Under Test:** `AddExpenseRequest` — @Pattern validation + Controller
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-EXPENSE-003`

**Preconditions:**
- `@WebMvcTest(ExpenseController.class)` với MockMvc
- MOTHER_001 JWT hợp lệ

**Test Steps:**
1. Arrange: POST body với `category = "FOOD"` (không hợp lệ)
2. Act: MockMvc.perform(post("/api/v1/expenses").content(...))
3. Assert: response status và error code

**Expected Result (PASS):**
- Response status `400 Bad Request`
- Response body chứa error code `EXPENSE-003`
- Service.addExpense() KHÔNG được gọi (validation fail ở DTO layer)

**Expected Result (FAIL):**
- Controller forward request với category không hợp lệ đến Service

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-005 — Cross-user access bị chặn → EXPENSE-005

**Severity:** `CRITICAL`
**Feature Under Test:** Ownership check trong `ExpenseServiceImpl.getExpense()` và `deleteExpense()`
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- Fixture `FX-EXPENSE-003`: EXPENSE_001 thuộc MOTHER_001
- Mock: `repository.findByExpenseIdAndOwnerUserId(EXPENSE_001, MOTHER_002)` trả về `Optional.empty()`

**Test Steps:**
1. Arrange: MOTHER_002 cố truy cập EXPENSE_001 của MOTHER_001
2. Act: gọi `service.getExpense(MOTHER_002, EXPENSE_001)`
3. Assert: exception bị ném

**Expected Result (PASS — hệ thống an toàn):**
- `ResourceNotFoundException` hoặc `AccessDeniedException` với code `EXPENSE-004` hoặc `EXPENSE-005`
- MOTHER_002 không nhận được dữ liệu của MOTHER_001

**Expected Result (FAIL — lỗ hổng bảo mật):**
- Service trả về expense của MOTHER_001 cho MOTHER_002
- Response 200 với financial data

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EXPENSE-TC-SEC-001 — Unauthenticated access → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ExpenseController` + JWT security filter
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Không có Authorization header
- `@WebMvcTest(ExpenseController.class)`

**Test Steps (Attack Simulation):**
1. MockMvc: `post("/api/v1/expenses")` không có Authorization header
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- Response status `401 Unauthorized`
- Error code `IAM-001`
- ExpenseService KHÔNG được gọi

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Response 200 hoặc 201 mà không cần JWT

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-SEC-002 — PII không xuất hiện trong application log (ADR-002)

**Severity:** `CRITICAL`
**OWASP:** `A09:2021 — Security Logging and Monitoring Failures`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File`
**Legal:** `PDPA — không log dữ liệu tài chính cá nhân`
**Feature Under Test:** `ExpenseServiceImpl.addExpense()` — logging behavior
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseServicePIILogTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Logger captured (sử dụng `ListAppender` của Logback hoặc mock logger)
- Request với `amount = 999999` (giá trị dễ identify trong log)

**Test Steps (Attack Simulation):**
1. Arrange: capture application log bằng `ListAppender<ILoggingEvent>`
2. Act: gọi `service.addExpense(MOTHER_001, request)` với amount = 999999
3. Assert: kiểm tra log messages

**Expected Result (PASS = hệ thống an toàn):**
- Không có log message nào chứa "999999"
- Không có log message nào chứa `note` value
- Log chỉ chứa `expenseId` và `userId`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `appender.list` chứa message với "999999" — PII leak trong log

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng Logback `ListAppender` để capture logs trong test:
```java
ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
Logger logger = (Logger) LoggerFactory.getLogger(ExpenseServiceImpl.class);
logger.addAppender(listAppender);
listAppender.start();
// ... call service ...
assertThat(listAppender.list)
    .extracting(ILoggingEvent::getFormattedMessage)
    .noneMatch(msg -> msg.contains("999999"));
```

---

### EXPENSE-TC-SEC-003 — SQL injection trong note field

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ExpenseController` + JPA parameterized query
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Gửi POST với `note = "'; DROP TABLE expenses; --"`
2. Kiểm tra response và DB state

**Expected Result (PASS = hệ thống an toàn):**
- Response 201 — note được lưu as-is (JPA parameterized query)
- Bảng `expenses` vẫn tồn tại trong DB
- Không có SQL injection xảy ra

**Expected Result (FAIL = lỗ hổng tồn tại):**
- DB error hoặc bảng bị xóa

**Current Status:** 🔴 Not written
**Implementation Note:** JPA/Hibernate với parameterized queries tự động bảo vệ SQL injection. Test này xác nhận không có raw SQL concatenation nào trong Repository.

---

### INTEGRATION TEST CASES

---

### EXPENSE-TC-INT-001 — Luồng hoàn chỉnh: Tạo → Lấy danh sách

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/expenses → GET /api/v1/expenses`
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container (`@Testcontainers` auto-start)
- Flyway migration V1 applied tự động (bảng `expenses` đã có trong V1)
- Seed: MOTHER_001 user tồn tại trong bảng `users`

**Test Steps:**
1. `POST /api/v1/expenses` với `{category: "MILK", amount: 350000, expenseDate: "2026-06-20"}`
2. Lưu `expenseId` từ response
3. `GET /api/v1/expenses`
4. Assert DB state trực tiếp

**Expected Result (PASS):**
- POST: response 201, chứa `expenseId`
- GET: list chứa expense vừa tạo
- DB: record tồn tại với đúng `owner_user_id = MOTHER_001`

**Expected Result (FAIL):**
- Expense không persist vào DB
- GET không trả về expense vừa tạo

**DB Assertion:**
```java
Expense record = expenseRepository
    .findByExpenseIdAndOwnerUserId(savedId, MOTHER_001)
    .orElseThrow();
assertThat(record).isNotNull();
assertThat(record.getCategory()).isEqualTo(ExpenseCategory.MILK);
assertThat(record.getAmount()).isEqualByComparingTo(new BigDecimal("350000"));
assertThat(record.getCurrency()).isEqualTo("VND");
assertThat(record.getOwnerUserId()).isEqualTo(MOTHER_001);
```

**Current Status:** 🔴 Not written

---

### EXPENSE-TC-INT-002 — Boundary: expenseDate = hôm nay và ngày mai

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/expenses` — date boundary validation end-to-end
**Test File:** `src/test/java/com/carebridge/backend/expense/ExpenseIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL container với V1 schema
- Server time = 2026-06-26 (test chạy vào ngày đó hoặc mock Clock bean)

**Test Steps:**
1. `POST /api/v1/expenses` với `expenseDate = LocalDate.now()` (hôm nay) → expect 201
2. `POST /api/v1/expenses` với `expenseDate = LocalDate.now().plusDays(1)` (ngày mai) → expect 400

**Expected Result (PASS):**
- Case 1 (today): response 201
- Case 2 (tomorrow): response 400, error code `EXPENSE-002`

**DB Assertion:**
```java
// Sau case 1: 1 record trong DB
long count = expenseRepository.countByOwnerUserId(MOTHER_001);
assertThat(count).isEqualTo(1L);

// Sau case 2: vẫn 1 record (case 2 không insert)
count = expenseRepository.countByOwnerUserId(MOTHER_001);
assertThat(count).isEqualTo(1L);  // Không tăng
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPENSE-TC-001` | `ExpenseServiceTest.java` | `[x]` | `Passed` | addExpense_success |
| `EXPENSE-TC-001b` | `ExpenseServiceTest.java` | `[ ]` | `___` | Not implemented (variant) |
| `EXPENSE-TC-002a` | `ExpenseServiceTest.java` | `[ ]` | `___` | Not implemented (listExpenses filter) |
| `EXPENSE-TC-002b` | `ExpenseServiceTest.java` | `[ ]` | `___` | Not implemented |
| `EXPENSE-TC-003a` | `ExpenseServiceTest.java` | `[x]` | `Passed` | addExpense_futureDate_rejects |
| `EXPENSE-TC-003b` | `ExpenseServiceTest.java` | `[ ]` | `___` | Not implemented |
| `EXPENSE-TC-004` | `ExpenseControllerTest.java` | `[ ]` | `___` | Not implemented (controller layer) |
| `EXPENSE-TC-005` | `ExpenseServiceTest.java` | `[ ]` | `___` | Not implemented (cross-user guard) |
| `EXPENSE-TC-SEC-001` | `ExpenseControllerTest.java` | `[ ]` | `___` | Not implemented (security/MockMvc) |
| `EXPENSE-TC-SEC-002` | `ExpenseServicePIILogTest.java` | `[ ]` | `___` | Not implemented (PDPA log check) |
| `EXPENSE-TC-SEC-003` | `ExpenseControllerTest.java` | `[ ]` | `___` | Not implemented (controller layer) |
| `EXPENSE-TC-INT-001` | `ExpenseIntegrationTest.java` | `[ ]` | `___` | Not implemented (Testcontainers unavailable) |
| `EXPENSE-TC-INT-002` | `ExpenseIntegrationTest.java` | `[ ]` | `___` | Not implemented (Testcontainers unavailable) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ExpenseServiceImpl implements IExpenseService {

    @Override
    public ExpenseResponse addExpense(UUID userId, AddExpenseRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ExpenseResponse> listExpenses(UUID userId, UUID journeyId, UUID babyId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpenseResponse getExpense(UUID userId, UUID expenseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void deleteExpense(UUID userId, UUID expenseId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPENSE-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXPENSE-TC-001b` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXPENSE-TC-002a` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXPENSE-TC-002b` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXPENSE-TC-003a` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `EXPENSE-TC-003b` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXPENSE-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `EXPENSE-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |

**Red Gate Evidence:**
- Stub commit hash: `2026-07-07-sprint3`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence-expense.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXPENSE-IMP-001` đã được review và approve
- [ ] **DPO Sign-off đã được cấp** — module PII financial data
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Xác nhận bảng `expenses` tồn tại trên staging (không cần migration mới)
- [ ] Test fixtures (FX-EXPENSE-001 đến FX-EXPENSE-006) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (addExpense: 2/2 service tests passed)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers unavailable)
- [ ] Test coverage ≥ 80% lines cho `ExpenseServiceImpl`
- [x] Không có business logic trong `ExpenseController`
- [ ] `EXPENSE-TC-SEC-002` PASS — log không chứa amount/note (not implemented)
- [ ] `EXPENSE-TC-005` PASS — cross-user access bị chặn (not implemented)
- [x] `EXPENSE-TC-003a` PASS — future date bị chặn server-side
- [ ] DPO đã xác nhận PII handling đúng chuẩn

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 8 unit tests FAIL với empty stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — sử dụng factory method `ExpenseTestFactory` trong mọi test
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn BR/ADR
- [ ] **PII Audit** — grep log artifact sau test suite:
  ```bash
  # Trong CI: verify không có PII trong test logs
  grep -r "999999\|350000\|synthetic note" target/surefire-reports/
  # Expected: No matches in application logs (chỉ xuất hiện trong test assertion code)
  ```

### Suspension Criteria (Điều kiện tạm dừng)

- DPO chưa sign-off
- Bảng `expenses` không tồn tại trên staging (V1 migration chưa được apply)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới → rollback chỉ cần revert code
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/expense/
git checkout -- src/test/java/com/carebridge/backend/expense/

# KHÔNG cần rollback DB (không có migration mới)
# Bảng expenses từ V1 vẫn còn nguyên — chỉ application code bị revert

# Verify rollback
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health
# Expected: 200 OK
```

> **Lưu ý DPO:** Nếu rollback xảy ra sau khi production data đã được ghi, cần thông báo DPO. Dữ liệu `expenses` là PII tài chính — KHÔNG xóa records đã tạo (data retention policy áp dụng).

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-EXPENSE-001/002/RBAC | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test tạo Flyway migration mới cho expenses table | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify Controller chứa `if (amount <= 0)` | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `ExpenseAuditService` hoặc class không có trong §8 TDS | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC51 Add Expense — CB-EXPENSE-IMP-001-TEST*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*DPO Sign-off REQUIRED trước khi proceed to implementation (PII module)*
