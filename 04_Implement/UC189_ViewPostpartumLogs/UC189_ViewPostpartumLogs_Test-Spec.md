# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC189 — View Postpartum Logs

**Document ID:** `FPT-EDU-TDD-UC189-001`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC189_ViewPostpartumLogs/UC189_ViewPostpartumLogs_TDS.md` (`CB-HEALTH-IMP-005`) — Technical Specification
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.11.3` — Functional requirements (UC-189)
- `04_Implement/UC188_DeleteMaternalHealthMetric/UC188_DeleteMaternalHealthMetric_TDS.md` — sibling ADR pattern (ADR-HEALTH-004/005)
- Luật 91/2025 (PDPA Việt Nam) — Legal basis

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent — Technical Architect` | Khởi tạo tài liệu — TDD spec cho UC189 View Postpartum Logs |

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
| **Feature / Gap ID** | `GAP-UC189` |
| **Module** | `View Postpartum Logs — Bounded Context: health` |
| **Spec gốc** | `CB-HEALTH-IMP-005` |
| **Priority** | 🟡 P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 — Device Sync And Care Edge Cases` |
| **Milestone** | `TV2-Bách batch` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `Luật 91/2025 (PDPA)` |
| **Upstream Dependencies** | `journey.MotherJourneyRepository`, `IAM (JWT)` |
| **Downstream Consumers** | `UC190 UpdatePostpartumLog`, `UC191 DeletePostpartumLog` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-005 §17`, `ADR-PPLOG-001/002/003` |
| **Constraints Injected** | C1 (ownership via journey join), C2 (status=ACTIVE filter mandatory), C3 (no event on read), C4 (SecurityUtils identity), C5 (Controller thin), C6 (no diagnostic fields in DTO) |
| **Model** | `Claude — Technical Architect Agent` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS không có cột `status` trên `postpartum_logs` cho soft-delete filtering | `V1__init_schema.sql` dòng 588-600 xác nhận không có cột `status`; migration mới `V20260707091000` bổ sung | Test seed data PHẢI insert `status='ACTIVE'` tường minh sau migration; test phải verify `findByJourneyIdAndStatus` loại trừ `DELETED` |
| L2 | SRS không đặc tả pagination | ADR-PPLOG-002 (Proposed) — không phân trang | Test chỉ verify full-list + sort order, không test pagination params |
| L3 | SRS không đặc tả sort order cụ thể ("over time") | TDS quyết định `log_date DESC, created_at DESC` (tie-break) | `PPLOG-TC-003` verify sort order kèm tie-break case |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
View Postpartum Logs bao gồm các layer:
├── Domain (PostpartumLog entity — pure JPA, no business logic)
├── Service (PostpartumLogServiceImpl — mock JPA Repository với Mockito)
├── Controller (PostpartumLogController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full GET flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-189 §3.3.11.3` | List + detail view, empty state (AF2), access denied (E1) |
| `ADR-PPLOG-001` | Ownership via journey join, package placement |
| `ADR-PPLOG-002` | No-pagination behavior |
| `ADR-PPLOG-003` | No domain event on read |
| `BR-RBAC` | MOTHER-only role guard |
| `BR-PRIVACY` | status=ACTIVE filter mandatory |
| `BR-SAFETY` | No diagnostic fields in response |
| `CB-HEALTH-IMP-005 §8/§9/§10` | Interface contract, API shape, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother lists logs of own journey with ≥1 ACTIVE log | `PostpartumLogServiceImpl.listLogs()` | `PPLOG-TC-001` |
| TC-COND-002 | Mother lists logs of own journey with 0 logs | `PostpartumLogServiceImpl.listLogs()` | `PPLOG-TC-002` |
| TC-COND-003 | List sort order with tie-break on same log_date | `PostpartumLogServiceImpl.listLogs()` | `PPLOG-TC-003` |
| TC-COND-004 | List excludes DELETED logs | `PostpartumLogRepository.findByJourneyIdAndStatus...` | `PPLOG-TC-004` |
| TC-COND-005 | List — journeyId not found | `PostpartumLogServiceImpl.listLogs()` | `PPLOG-TC-005` |
| TC-COND-006 | List — caller not journey owner (IDOR) | `PostpartumLogServiceImpl.listLogs()` | `PPLOG-TC-006` |
| TC-COND-007 | Detail — happy path | `PostpartumLogServiceImpl.getLogDetail()` | `PPLOG-TC-007` |
| TC-COND-008 | Detail — log not found / already deleted | `PostpartumLogServiceImpl.getLogDetail()` | `PPLOG-TC-008` |
| TC-COND-009 | Detail — caller not owner (IDOR) | `PostpartumLogServiceImpl.getLogDetail()` | `PPLOG-TC-009` |
| TC-COND-010 | Controller — unauthenticated request | `PostpartumLogController` | `PPLOG-TC-010` |
| TC-COND-011 | Controller — non-MOTHER role rejected | `PostpartumLogController` | `PPLOG-TC-011` |
| TC-COND-012 | Response DTO excludes diagnostic fields | `PostpartumLogResponse` contract | `PPLOG-TC-012` |
| TC-COND-013 | End-to-end integration: seed → list → verify DB/API shape | Full stack | `PPLOG-TC-INT-001` |
| TC-COND-014 | End-to-end integration: DELETED log excluded from live API | Full stack | `PPLOG-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | journeyId (owned / not-owned / non-existent) | 3 partitions cover ownership matrix |
| Boundary Value Analysis | Empty list (0 records) vs single/multi record list | AF2 boundary |
| State Transition Testing | `status` ACTIVE vs DELETED filtering | FSM invariant (§6.4 TDS) |
| Error Guessing | IDOR via crafted `logId`/`journeyId` UUID of another Mother | Security attack vector |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `MotherJourney { id: J1, ownerUserId: U1 }` | Owner journey |
| `FX-002` | DB seed | `MotherJourney { id: J2, ownerUserId: U2 }` | Non-owner journey (IDOR victim) |
| `FX-003` | DB seed | `PostpartumLog { id: L1, journeyId: J1, logDate: 2026-07-01, status: ACTIVE }` | Happy path list item |
| `FX-004` | DB seed | `PostpartumLog { id: L2, journeyId: J1, logDate: 2026-06-30, status: DELETED }` | Excluded-from-list fixture |
| `FX-005` | DB seed | `PostpartumLog { id: L3, journeyId: J1, logDate: 2026-07-01, createdAt: earlier, status: ACTIVE }` | Tie-break sort fixture (same logDate as L1) |
| `FX-006` | JWT | `{ sub: U1, role: MOTHER }` | Auth context — owner |
| `FX-007` | JWT | `{ sub: U2, role: MOTHER }` | Auth context — non-owner (attacker) |
| `FX-008` | JWT | `{ sub: U1, role: FAMILY }` | Auth context — wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// PostpartumLogTestFactory.java
// ═══════════════════════════════════════════════════════════
class PostpartumLogTestFactory {

    static final UUID JOURNEY_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID JOURNEY_OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A2");
    static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");

    static MotherJourney makeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(JOURNEY_OWNER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    static MotherJourney makeJourney(Consumer<MotherJourney.MotherJourneyBuilder> overrides) {
        var builder = MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(JOURNEY_OWNER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE);
        overrides.accept(builder);
        return builder.build();
    }

    static PostpartumLog makeLog() {
        return PostpartumLog.builder()
                .id(UUID.randomUUID())
                .journeyId(JOURNEY_ID)
                .logDate(LocalDate.of(2026, 7, 1))
                .painLevel((short) 3)
                .bleedingLevel("LIGHT")
                .moodLevel((short) 6)
                .sleepHours(new BigDecimal("5.5"))
                .breastfeedingNote("Đau khi cho bú bên trái")
                .symptomNote("Không sốt")
                .status(PostpartumLogStatus.ACTIVE)
                .build();
    }

    static PostpartumLog makeLog(Consumer<PostpartumLog> overrides) {
        PostpartumLog log = makeLog();
        overrides.accept(log);
        return log;
    }
}
```

---

### PPLOG-TC-001 — List logs happy path (single ACTIVE log)

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.listLogs(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/health/unit/PostpartumLogServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-HEALTH-IMP-005 §8.1 IPostpartumLogService.listLogs`

**Preconditions:**
- `FX-001` journey seeded, `FX-003` log seeded (owner = caller)

**Test Steps:**
1. Mock `journeyRepository.findById(JOURNEY_ID)` → `Optional.of(makeJourney())`
2. Mock `logRepository.findByJourneyIdAndStatusOrderByLogDateDescCreatedAtDesc(JOURNEY_ID, ACTIVE)` → `List.of(makeLog())`
3. Call `service.listLogs(JOURNEY_ID, JOURNEY_OWNER_ID)`

**Expected Result (PASS):**
- Returns `List<PostpartumLogResponse>` size 1, all fields mapped correctly from entity.

**Expected Result (FAIL):**
- Empty list, wrong field mapping, or exception thrown.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-002 — List logs — empty result (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogServiceImpl.listLogs(UUID, UUID)`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `SRS AF2`

**Preconditions:** journey exists, owned, but 0 ACTIVE logs.

**Test Steps:**
1. Mock repository to return empty list
2. Call `service.listLogs(JOURNEY_ID, JOURNEY_OWNER_ID)`

**Expected Result (PASS):** returns empty `List` (not null, not exception).
**Expected Result (FAIL):** NPE, or exception thrown for empty state.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-003 — List logs sort order with tie-break

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogRepository.findByJourneyIdAndStatusOrderByLogDateDescCreatedAtDesc`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-HEALTH-IMP-005 §5.2 sort convention (log_date DESC, created_at DESC)`

**Preconditions:** `FX-003` (L1, logDate=2026-07-01, later createdAt) and `FX-005` (L3, same logDate, earlier createdAt) seeded.

**Test Steps:**
1. Query repository directly (integration-level) or via service
2. Assert order: `L1` (later createdAt) appears before `L3` when `log_date` ties.

**Expected Result (PASS):** `[L1, L3]` order.
**Expected Result (FAIL):** Non-deterministic or reversed order.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-004 — List excludes DELETED logs

**Severity:** `CRITICAL`
**CWE:** `CWE-668 — Exposure of Resource to Wrong Sphere` *(soft-deleted PII must not leak)*
**Feature Under Test:** `PostpartumLogServiceImpl.listLogs()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-PPLOG-001 §Decision`, `BR-PRIVACY`

**Preconditions:** `FX-003` (ACTIVE) + `FX-004` (DELETED) both belong to `JOURNEY_ID`.

**Test Steps:**
1. Seed both logs
2. Call `service.listLogs(JOURNEY_ID, JOURNEY_OWNER_ID)`

**Expected Result (PASS):** result contains only `FX-003`, `FX-004` absent.
**Expected Result (FAIL):** DELETED log appears in response — privacy violation.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-005 — List — journeyId does not exist

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.listLogs()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-HEALTH-IMP-005 §10 PPLOG-002`

**Test Steps:**
1. Mock `journeyRepository.findById(unknownId)` → `Optional.empty()`
2. Call `service.listLogs(unknownId, callerId)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-002", ...)`.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-006 — List — IDOR: caller not journey owner

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `PostpartumLogServiceImpl.listLogs()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-HEALTH-IMP-005 §10 PPLOG-003`, `BR-RBAC`

**Preconditions:** `FX-001` journey owned by `U1`; attacker is `U2` (`FX-007`).

**Test Steps (Attack Simulation):**
1. Mock `journeyRepository.findById(JOURNEY_ID)` → journey owned by `U1`
2. Call `service.listLogs(JOURNEY_ID, JOURNEY_OTHER_OWNER_ID)` (caller = `U2`)

**Expected Result (PASS = hệ thống an toàn):** throws `BusinessException(403, "PPLOG-003", ...)`.
**Expected Result (FAIL = lỗ hổng tồn tại):** returns log list belonging to `U1` — data leak.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-007 — Detail happy path

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.getLogDetail(UUID, UUID)`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-HEALTH-IMP-005 §8.1`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(L1, ACTIVE)` → `Optional.of(makeLog())`
2. Mock `journeyRepository.findById(JOURNEY_ID)` → owner journey
3. Call `service.getLogDetail(L1, JOURNEY_OWNER_ID)`

**Expected Result (PASS):** returns fully-mapped `PostpartumLogResponse`.
**Expected Result (FAIL):** exception or partial mapping.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-008 — Detail — not found / already deleted (idempotent-read semantics)

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.getLogDetail()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-HEALTH-IMP-005 §10 PPLOG-001`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(id, ACTIVE)` → `Optional.empty()`
2. Call `service.getLogDetail(id, callerId)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-001", ...)`.
**Expected Result (FAIL):** NPE or wrong code.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-009 — Detail — IDOR: caller not owner

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Feature Under Test:** `PostpartumLogServiceImpl.getLogDetail()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-HEALTH-IMP-005 §10 PPLOG-003`

**Test Steps (Attack Simulation):**
1. Mock `logRepository.findByIdAndStatus(L1, ACTIVE)` → log belongs to `JOURNEY_ID` (owner `U1`)
2. Mock `journeyRepository.findById(JOURNEY_ID)` → owner `U1`
3. Call `service.getLogDetail(L1, JOURNEY_OTHER_OWNER_ID)` (caller `U2`)

**Expected Result (PASS = hệ thống an toàn):** throws `BusinessException(403, "PPLOG-003", ...)`.
**Expected Result (FAIL = lỗ hổng tồn tại):** returns detail of `U1`'s log to `U2`.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-010 — Controller — unauthenticated request

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `PostpartumLogController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/health/web/PostpartumLogControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Call `GET /api/v1/postpartum-logs?journeyId=X` with no `Authorization` header

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, `IAM-001`.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200` returned without auth.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-011 — Controller — non-MOTHER role rejected

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `PostpartumLogController` `@PreAuthorize("hasRole('MOTHER')")`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Authenticate as `FAMILY` role (`FX-008`)
2. Call `GET /api/v1/postpartum-logs?journeyId=X`

**Expected Result (PASS):** `403 Forbidden`.
**Expected Result (FAIL):** `200` — role guard bypassed.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-012 — Response DTO contract excludes diagnostic fields

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogResponse` (contract/reflection test)
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-SAFETY`, `CB-HEALTH-IMP-005 §17 C6`

**Test Steps:**
1. Reflectively inspect `PostpartumLogResponse` fields
2. Assert no field named `riskLevel`, `diagnosis`, `recommendation`, `alertLevel`

**Expected Result (PASS):** assertion passes — no diagnostic field present.
**Expected Result (FAIL):** any such field found.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### PPLOG-TC-INT-001 — Full flow: seed → GET list → verify DB + API shape

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: seed DB → GET /api/v1/postpartum-logs?journeyId= → assert response`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migration `V20260707091000` applied automatically at context start
- Seed: `FX-001` (journey), `FX-003` (log)

**Test Steps:**
1. Seed journey + log via JPA repository
2. `GET /api/v1/postpartum-logs?journeyId={journeyId}` with valid JWT for owner
3. Assert response body shape and values

**Expected Result (PASS):**
- `200`, `data[0].id == L1`, all fields match seed values.

**Expected Result (FAIL):**
- Missing fields, wrong sort, 500 error (migration not applied).

**DB Assertion:**
```java
PostpartumLog record = logRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(PostpartumLogStatus.ACTIVE);
```

**Current Status:** 🔴 Not written

---

### PPLOG-TC-INT-002 — DELETED log excluded from live API (cross-UC dependency on UC191)

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: seed log with status=DELETED → GET list/detail → assert exclusion`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Seed log directly with `status='DELETED'` via JDBC (simulating post-UC191 state, since UC191 controller may not exist yet at UC189 implementation time).

**Test Steps:**
1. Insert log with `status='DELETED'` directly via `JdbcTemplate`
2. `GET /api/v1/postpartum-logs?journeyId=` → assert log absent from list
3. `GET /api/v1/postpartum-logs/{id}` → assert `404 PPLOG-001`

**Expected Result (PASS):** both assertions hold.
**Expected Result (FAIL):** DELETED log leaks through either endpoint.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PPLOG-TC-001` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-002` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-003` | `PostpartumLogRepositoryTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-004` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-005` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-006` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-007` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-008` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-009` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-010` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-011` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-012` | `PostpartumLogResponseContractTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-INT-001` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-INT-002` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class PostpartumLogServiceImpl implements IPostpartumLogService {

    @Override
    public List<PostpartumLogResponse> listLogs(UUID journeyId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public PostpartumLogResponse getLogDetail(UUID logId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PPLOG-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-005` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] Flyway migration `V20260707091000__add_postpartum_log_status.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `PostpartumLogServiceImpl`
- [ ] Không có business logic trong `PostpartumLogController`
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Mobile: `flutter test` xanh cho `postpartum_log_service_test.dart` (widget/service test cho list + detail screens)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence:** `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation:** không có shared mutable state giữa tests (factory pattern §4)
- [ ] **Oracle Source:** mọi expected value trong assert có ghi rõ nguồn (BR/ADR/TDS section)

### Suspension Criteria

- Migration `V20260707091000` chưa approved/chạy trên staging
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE postpartum_logs DROP COLUMN IF EXISTS status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707091000';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/health/entity/PostpartumLog.java
git checkout -- src/main/java/com/carebridge/backend/health/entity/PostpartumLogStatus.java
git checkout -- src/main/java/com/carebridge/backend/health/repository/PostpartumLogRepository.java
git checkout -- src/main/java/com/carebridge/backend/health/service/
git checkout -- src/main/java/com/carebridge/backend/health/controller/PostpartumLogController.java
git checkout -- src/main/resources/db/migration/V20260707091000__add_postpartum_log_status.sql
git checkout -- src/test/java/com/carebridge/backend/health/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu có)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending user review/approval per `.claude/rules/implement-flow.md`.*
