# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-43 Generate Health Summary

**Document ID:** `CB-HEALTH-IMP-005-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Spec`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary schema source
- `04_Implement/UC43_GenerateHealthSummary/UC43_GenerateHealthSummary_TDS.md` — TDS CB-HEALTH-IMP-005
- `02_Requirements/SRS.md §3.3.1.20` — UC-43 functional requirements

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo tài liệu TDD spec cho UC-43 Generate Health Summary |

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
| **Feature / Gap ID** | `UC-43` |
| **Module** | `GenerateHealthSummary — health Bounded Context` |
| **Spec gốc** | `CB-HEALTH-IMP-005` |
| **Priority** | 🟠 P1 — High |
| **Sprint** | `S[N] (2026-06-26 → TBD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY, BR-SUMMARY-001, BR-SUMMARY-002, PDPA` |
| **Upstream Dependencies** | `auth (JWT), health_records table, journey, baby` |
| **Downstream Consumers** | `UC-44 ShareSummaryWithExpert, AuditService, NotificationService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-005 §17`, `BR-SUMMARY-001`, `BR-SUMMARY-002`, `BR-SAFETY`, `BR-RBAC` |
| **Constraints Injected** | C1 (validateDataExists), C2 (summaryPeriod enum), C3 (Controller no-logic), C4 (userId from JWT), C5 (no diagnosis in json), C6 (audit event) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS không chỉ định giá trị enum cho summary_period | `V1__init_schema.sql`: `summary_period varchar(30)` — BR-SUMMARY-002 định nghĩa `24H`, `7D`, `CONSULTATION` | Test validate cả 3 giá trị hợp lệ và reject giá trị khác |
| L2 | SRS không chỉ định behaviour khi không có data | BR-SUMMARY-001: phải có ít nhất 1 record — throw lỗi HEALTH-003 | Test phải verify 422 trả về khi không có record |
| L3 | SRS không đề cập BR-SAFETY trong context summary | CLAUDE.md/BR-SAFETY: AI không chẩn đoán, không kê đơn | Test verify summary_json không chứa diagnosis field |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
GenerateHealthSummary bao gồm các layer:
├── Domain / Service (validateDataExists, aggregateRecords — unit test với Mockito)
├── Application / Controller (DTO validation — @WebMvcTest)
├── Repository (findByOwnerUserIdAndRecordDateBetweenAndStatus)
└── Integration (Testcontainers PostgreSQL + full Spring context)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-43 §3.3.1.20` | Mother chọn period → hệ thống tổng hợp → trả về summary |
| `BR-SUMMARY-001` | Phải có ít nhất 1 record trong period |
| `BR-SUMMARY-002` | summaryPeriod ∈ {24H, 7D, CONSULTATION} |
| `BR-RBAC` | Chỉ ROLE_MOTHER; chỉ truy cập data của mình |
| `BR-SAFETY` | summaryJson không được chứa chẩn đoán/kê đơn |
| `CB-HEALTH-IMP-005 §10` | Error codes: HEALTH-001, HEALTH-003, HEALTH-004, HEALTH-005 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | summaryPeriod hợp lệ + có data → tạo thành công | `HealthSummaryService.generateSummary()` | `HEALTH-TC-001` |
| TC-COND-002 | summaryPeriod không hợp lệ → 400 | `GenerateHealthSummaryRequest` DTO validation | `HEALTH-TC-002` |
| TC-COND-003 | Không có health record trong period → 422 | `HealthSummaryService.validateDataExists()` | `HEALTH-TC-003` |
| TC-COND-004 | summaryPeriod = 24H → date range tự động tính | `HealthSummaryService.resolveDateRange()` | `HEALTH-TC-004` |
| TC-COND-005 | summaryPeriod = CONSULTATION → date range từ request | `HealthSummaryService.generateSummary()` | `HEALTH-TC-005` |
| TC-COND-006 | summary_json không chứa diagnosis | `HealthSummaryService.aggregateRecords()` | `HEALTH-TC-006` |
| TC-COND-007 | GET summary không thuộc owner → 403/404 | `HealthSummaryService.getSummary()` | `HEALTH-TC-007` |
| TC-COND-008 | Không có JWT → 401 | Spring Security filter | `HEALTH-TC-008` |
| TC-COND-009 | Luồng E2E hoàn chỉnh qua REST API | Full stack | `HEALTH-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | summaryPeriod: {valid} vs {invalid} | BR-SUMMARY-002 |
| Boundary Value Analysis | periodStart = today (edge của @PastOrPresent) | Date validation |
| State Transition Testing | HealthSummary status: ACTIVE → ARCHIVED | State machine §6.3 |
| Error Guessing | RBAC cross-owner access, missing JWT, SQL injection in period | Security |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `health_records`: 3 records, SYNTHETIC mother UUID, status='ACTIVE', record_date in last 7 days | Happy path 7D |
| `FX-002` | DB seed | `health_records`: 1 record, SYNTHETIC mother UUID, status='ACTIVE', record_date = today | Happy path 24H |
| `FX-003` | DB seed | Không có health_records cho SYNTHETIC mother B | BR-SUMMARY-001 test |
| `FX-004` | JWT | `{ sub: 'synthetic-mother-uuid-001', role: 'ROLE_MOTHER' }` | Auth context |
| `FX-005` | JWT | `{ sub: 'synthetic-mother-uuid-002', role: 'ROLE_MOTHER' }` | Cross-owner RBAC test |
| `FX-006` | DB seed | `health_summaries`: 1 summary thuộc FX-005 owner | RBAC cross-owner test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// HealthSummaryTestFactory.java
class HealthSummaryTestFactory {

    static final UUID SYNTHETIC_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SYNTHETIC_OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    static HealthRecord makeHealthRecord() {
        HealthRecord record = new HealthRecord();
        record.setHealthRecordId(UUID.randomUUID());
        record.setOwnerUserId(SYNTHETIC_OWNER_ID);
        record.setRecordType("LAB_RESULT");
        record.setTitle("SYNTHETIC Lab Result");
        record.setRecordDate(LocalDate.now().minusDays(1));
        record.setStatus("ACTIVE");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return record;
    }

    static HealthRecord makeHealthRecord(Consumer<HealthRecord> overrides) {
        HealthRecord record = makeHealthRecord();
        overrides.accept(record);
        return record;
    }

    static GenerateHealthSummaryRequest makeRequest() {
        GenerateHealthSummaryRequest req = new GenerateHealthSummaryRequest();
        req.setSummaryPeriod("7D");
        req.setPeriodStart(LocalDate.now().minusDays(7));
        req.setPeriodEnd(LocalDate.now());
        return req;
    }

    static GenerateHealthSummaryRequest makeRequest(Consumer<GenerateHealthSummaryRequest> overrides) {
        GenerateHealthSummaryRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### HEALTH-TC-001 — Tạo summary 7D thành công

**Severity:** `HIGH`
**Feature Under Test:** `HealthSummaryService.generateSummary()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-SUMMARY-001`, `UC-43 §3.3.1.20`

**Preconditions:**
- SYNTHETIC Mother UUID: `00000000-0000-0000-0000-000000000001`
- Mock `IHealthRecordRepository.findByOwnerUserIdAndRecordDateBetweenAndStatus()` trả về list 3 records (FX-001)
- Mock `IHealthSummaryRepository.save()` trả về entity đã lưu

**Test Steps:**
1. Arrange: Setup mocks, tạo request với `summaryPeriod="7D"` via `makeRequest()`
2. Act: Gọi `summaryService.generateSummary(request, SYNTHETIC_OWNER_ID)`
3. Assert: Verify response không null, summaryId không null, status="ACTIVE", generatedBy="USER"

**Expected Result (PASS):**
- Response chứa `summaryId` (UUID hợp lệ)
- `summaryPeriod` = "7D"
- `status` = "ACTIVE"
- `generatedBy` = "USER"
- `save()` được gọi đúng 1 lần với entity có `ownerUserId = SYNTHETIC_OWNER_ID`
- `AuditService.emit()` được gọi đúng 1 lần

**Expected Result (FAIL):**
- NullPointerException nếu response null
- `save()` không được gọi
- `AuditService` không nhận event

**Current Status:** 🔴 Not written
**Implementation Note:** Service phải resolve dateRange từ period, gọi recordRepo, validate, aggregate, save, emit audit.

---

### HEALTH-TC-002 — summaryPeriod không hợp lệ → 400

**Severity:** `HIGH`
**Feature Under Test:** `GenerateHealthSummaryRequest` DTO validation / `HealthSummaryController`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-SUMMARY-002`

**Preconditions:**
- `@WebMvcTest(HealthSummaryController.class)` setup
- Mock `IHealthSummaryService` — không nên được gọi trong test này

**Test Steps:**
1. Arrange: Tạo request body `{ "summaryPeriod": "MONTHLY" }`
2. Act: `mockMvc.perform(post("/api/v1/health-summaries").content(...).header("Authorization", "Bearer [jwt]"))`
3. Assert: status = 400, body chứa `error.code = "HEALTH-001"`

**Expected Result (PASS):**
- HTTP 400
- `error.code` = "HEALTH-001"
- `IHealthSummaryService.generateSummary()` KHÔNG được gọi

**Expected Result (FAIL):**
- HTTP 201 — service đã gọi với input không hợp lệ
- HTTP 500 — exception chưa được handle

**Current Status:** 🔴 Not written
**Implementation Note:** `@Pattern(regexp="^(24H|7D|CONSULTATION)$")` phải có trên field `summaryPeriod` trong DTO. Controller advice phải map MethodArgumentNotValidException → HEALTH-001.

---

### HEALTH-TC-003 — Không có health record trong period → 422

**Severity:** `HIGH`
**Feature Under Test:** `HealthSummaryService.validateDataExists()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-SUMMARY-001`

**Preconditions:**
- Mock `IHealthRecordRepository` trả về empty list `[]` (FX-003)
- SYNTHETIC Mother không có records

**Test Steps:**
1. Arrange: Mock recordRepo trả về `Collections.emptyList()`
2. Act: Gọi `summaryService.generateSummary(makeRequest(), SYNTHETIC_OWNER_ID)`
3. Assert: Kiểm tra exception được ném với code "HEALTH-003"

**Expected Result (PASS):**
- `HealthSummaryException` được ném với code "HEALTH-003"
- `IHealthSummaryRepository.save()` KHÔNG được gọi
- Không có record nào được tạo

**Expected Result (FAIL):**
- Summary được tạo với 0 records (vi phạm BR-SUMMARY-001)
- Exception khác được ném (NullPointerException, etc.)

**Current Status:** 🔴 Not written
**Implementation Note:** `validateDataExists(List<HealthRecord> records)` phải throw `HealthSummaryException("HEALTH-003")` khi `records.isEmpty()`.

---

### HEALTH-TC-004 — summaryPeriod 24H tự động tính date range

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthSummaryService.resolveDateRange()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC-43 §3.3.1.20` — 24H summary

**Preconditions:**
- Request với `summaryPeriod="24H"` và `periodStart=null, periodEnd=null`
- Mock recordRepo trả về 1 record (FX-002)
- Clock mock để test deterministic

**Test Steps:**
1. Arrange: makeRequest với `summaryPeriod="24H"`, periodStart=null, periodEnd=null
2. Act: Gọi `summaryService.generateSummary(request, SYNTHETIC_OWNER_ID)`
3. Assert: recordRepo được gọi với `start = today - 1 day`, `end = today`

**Expected Result (PASS):**
- recordRepo.findBy...() được gọi với `start = LocalDate.now().minusDays(1)`, `end = LocalDate.now()`
- Summary được tạo thành công với periodStart/periodEnd đúng

**Expected Result (FAIL):**
- periodStart/periodEnd null khi lưu vào DB
- Date range tính sai

**Current Status:** 🔴 Not written

---

### HEALTH-TC-005 — summaryPeriod CONSULTATION dùng date range từ request

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthSummaryService.generateSummary()` — CONSULTATION period
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SUMMARY-002`

**Preconditions:**
- Request với `summaryPeriod="CONSULTATION"`, `periodStart="2026-06-01"`, `periodEnd="2026-06-26"`
- Mock recordRepo trả về 2 records

**Test Steps:**
1. Arrange: makeRequest(r -> { r.setSummaryPeriod("CONSULTATION"); r.setPeriodStart(LocalDate.of(2026,6,1)); r.setPeriodEnd(LocalDate.of(2026,6,26)); })
2. Act: Gọi generateSummary(request, SYNTHETIC_OWNER_ID)
3. Assert: recordRepo được gọi với start=2026-06-01 và end=2026-06-26

**Expected Result (PASS):**
- recordRepo được gọi với các date đúng theo request
- Summary được tạo với `summaryPeriod="CONSULTATION"`

**Current Status:** 🔴 Not written

---

### HEALTH-TC-006 — summary_json không chứa diagnosis (BR-SAFETY)

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthSummaryService.aggregateRecords()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-SAFETY`

**Preconditions:**
- Mock records chứa nhiều loại data đa dạng (FX-001)

**Test Steps:**
1. Arrange: List 3 SYNTHETIC records với recordType khác nhau
2. Act: Gọi `summaryService.generateSummary(request, SYNTHETIC_OWNER_ID)`
3. Assert: summaryJson không chứa các key: "diagnosis", "prescription", "medication", "treatment", "disease"

**Expected Result (PASS):**
- `summaryJson` chỉ chứa: `totalRecords`, `recordTypes`, `periodSummary`, `note`
- `note` chứa disclaimer: "Đây là thông tin tổng hợp, không phải chẩn đoán"
- Không có field "diagnosis", "prescription", "medication", "treatment"

**Expected Result (FAIL):**
- summaryJson chứa bất kỳ field y tế nhạy cảm nào vi phạm BR-SAFETY

**Current Status:** 🔴 Not written
**Implementation Note:** `aggregateRecords()` PHẢI có denylist kiểm tra output. Không dùng AI để generate nội dung diagnosis.

---

### HEALTH-TC-007 — GET summary không thuộc owner → 403/404

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthSummaryService.getSummary()` — RBAC cross-owner
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- SYNTHETIC Mother A: `00000000-0000-0000-0000-000000000001`
- SYNTHETIC Mother B: `00000000-0000-0000-0000-000000000002`
- Summary được tạo bởi Mother B (FX-006)
- Mock repository `findByIdAndOwnerUserId(summaryId, motherA_id)` trả về `Optional.empty()`

**Test Steps:**
1. Arrange: summaryId = UUID của summary thuộc Mother B, caller = Mother A
2. Act: Gọi `summaryService.getSummary(summaryId, SYNTHETIC_OWNER_ID /* Mother A */)`
3. Assert: Exception được ném với code "HEALTH-004" hoặc "HEALTH-005"

**Expected Result (PASS):**
- `HealthSummaryException` với code "HEALTH-004" (not found) hoặc "HEALTH-005" (access denied)
- Mother A KHÔNG nhận được data của Mother B

**Expected Result (FAIL):**
- Method trả về summary của Mother B (RBAC bypass)
- HTTP 200 với data của người khác

**Current Status:** 🔴 Not written
**Implementation Note:** Repository query PHẢI filter theo `owner_user_id`. Không dùng `findById()` đơn thuần — phải dùng `findByIdAndOwnerUserId()`.

---

### HEALTH-TC-008 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- `@WebMvcTest` setup với Security enabled
- Không có Authorization header

**Test Steps:**
1. Arrange: Request không có Authorization header
2. Act: `mockMvc.perform(post("/api/v1/health-summaries").content(...))`
3. Assert: HTTP 401, error.code = "IAM-001"

**Expected Result (PASS):**
- HTTP 401
- `IHealthSummaryService` KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### HEALTH-TC-SEC-001 — SQL Injection qua summaryPeriod field

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `GenerateHealthSummaryRequest` DTO + `IHealthSummaryRepository`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummarySecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Spring context với JPA + parameterized queries

**Test Steps (Attack Simulation):**
1. Arrange: Request với `summaryPeriod = "7D'; DROP TABLE health_summaries; --"`
2. Act: POST /api/v1/health-summaries với payload độc hại
3. Assert: HTTP 400 (validation fail), bảng health_summaries vẫn tồn tại

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 400 với HEALTH-001 (validation reject tại DTO layer)
- Bảng health_summaries không bị ảnh hưởng
- JPA parameterized queries ngăn injection

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP 500 với DB error
- Bảng bị xóa

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### HEALTH-TC-INT-001 — Luồng E2E tạo summary 7D với Testcontainers

**Severity:** `HIGH`
**Feature Under Test:** Full flow: Controller → Service → Repository → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/health/HealthSummaryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration V1__init_schema.sql applied tự động
- Seed: 3 SYNTHETIC health_records via JPA repository cho FX-001 owner

**Test Steps:**
1. Seed SYNTHETIC Mother user + 3 health_records trong 7 ngày qua
2. POST /api/v1/health-summaries với JWT ROLE_MOTHER, summaryPeriod="7D"
3. Assert HTTP 201 và extract summaryId từ response
4. GET /api/v1/health-summaries/{summaryId} để verify persistence
5. Assert DB state trực tiếp qua JdbcTemplate

**Expected Result (PASS):**
- HTTP 201 với summaryId hợp lệ
- DB: 1 record trong health_summaries, status='ACTIVE', summary_period='7D'
- DB: summary_json chứa totalRecords=3
- Audit log chứa HealthSummaryGenerated event

**Expected Result (FAIL):**
- HTTP 500 — service/repository error
- DB: record không được tạo
- summary_json null hoặc rỗng

**DB Assertion:**
```java
// Dùng JdbcTemplate để assert DB state
Map<String, Object> row = jdbcTemplate.queryForMap(
    "SELECT summary_period, status, summary_json::text FROM health_summaries WHERE summary_id = ?",
    savedSummaryId
);
assertThat(row.get("summary_period")).isEqualTo("7D");
assertThat(row.get("status")).isEqualTo("ACTIVE");
assertThat(row.get("summary_json").toString()).contains("totalRecords");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `HEALTH-TC-001` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-002` | `HealthSummaryControllerTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-003` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-004` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-005` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-006` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-007` | `HealthSummaryServiceTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-008` | `HealthSummaryControllerTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-SEC-001` | `HealthSummarySecurityTest.java` | `[ ]` | `___` | — |
| `HEALTH-TC-INT-001` | `HealthSummaryIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase stub — PHẢI throw UnsupportedOperationException
@Service
public class HealthSummaryService implements IHealthSummaryService {

    @Override
    public HealthSummaryResponse generateSummary(GenerateHealthSummaryRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public HealthSummaryResponse getSummary(UUID summaryId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<HealthSummaryResponse> listSummaries(UUID userId, String period) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `HEALTH-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `HEALTH-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `HEALTH-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `HEALTH-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `HEALTH-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `logs/red-gate-uc43-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-005` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] `health_summaries` table xác nhận tồn tại trong staging (V1__init_schema.sql)
- [ ] Index migration (nếu cần) đã approved và chạy thành công trên staging
- [ ] Test fixtures (FX-001 đến FX-006) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `HealthSummaryService`
- [ ] Không có business logic trong `HealthSummaryController`
- [ ] Không có PII plaintext trong application logs
- [ ] `summaryJson` không chứa field chẩn đoán/kê đơn (BR-SAFETY verified)
- [ ] Audit event `HealthSummaryGenerated` được emit sau mỗi lần tạo thành công

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 10 tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `HealthSummaryService` compile thành công:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — tất cả test instances tạo via factory method, không shared mutable state
- [ ] **Oracle Source** — tất cả expected values trong assert trace về BR/AC/ADR cụ thể

### Suspension Criteria

- Blocker: `health_summaries` table chưa có trong DB (migration chưa chạy)
- Phát hiện conflict về data model cần Principal Architect review
- CI pipeline broken bởi thay đổi khác trong `health` bounded context

---

## 7. Rollback Plan

```bash
# Revert index migration nếu cần (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_health_summaries_owner_period;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_health_summaries_owner_dates;"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/health/summary/
git checkout -- src/test/java/com/carebridge/backend/health/summary/

# Gap UC-43 vẫn OPEN → giữ entry trong backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — CB-HEALTH-IMP-005-TEST — UC-43 Generate Health Summary — Draft 2026-06-26*
