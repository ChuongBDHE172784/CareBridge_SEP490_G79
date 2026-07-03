# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC190 — Update Postpartum Log

**Document ID:** `FPT-EDU-TDD-UC190-001`
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
- `04_Implement/UC190_UpdatePostpartumLog/UC190_UpdatePostpartumLog_TDS.md` (`CB-HEALTH-IMP-006`) — Technical Specification
- `04_Implement/UC189_ViewPostpartumLogs/UC189_ViewPostpartumLogs_TDS.md` — upstream entity/repository owner
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.11.4` — Functional requirements (UC-190)
- Luật 91/2025 (PDPA Việt Nam) — Legal basis

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent — Technical Architect` | Khởi tạo tài liệu — TDD spec cho UC190 Update Postpartum Log |

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
| **Feature / Gap ID** | `GAP-UC190` |
| **Module** | `Update Postpartum Log — Bounded Context: health` |
| **Spec gốc** | `CB-HEALTH-IMP-006` |
| **Priority** | 🟡 P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 — Device Sync And Care Edge Cases` |
| **Milestone** | `TV2-Bách batch` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `Luật 91/2025 (PDPA)` |
| **Upstream Dependencies** | `UC189` (`PostpartumLog`, `PostpartumLogRepository`), `journey.MotherJourneyRepository`, `IAM (JWT)` |
| **Downstream Consumers** | `UC189` (read-after-write consistency) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-006 §17`, `ADR-PPLOG-004 (Accepted 2026-07-03)`, `ADR-PPLOG-005 (Accepted)` |
| **Constraints Injected** | C1 (ownership), C2 (partial-update), C3 (ACTIVE-only guard), C4 (identity), C5 (immutable fields), C6 (event payload minimum-necessary), C7 (thin controller) |
| **Model** | `Claude — Technical Architect Agent` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.11.4 không liệt kê field nào mutable | TDS §3 ADR-PPLOG-004 (Accepted 2026-07-03 — Product Owner xác nhận): 6 field content mutable, `log_date` immutable | Test cases include an explicit boundary test (`PPLOG-TC-U-009`) asserting `logDate` cannot be changed via the DTO surface |
| L2 | SRS không đặc tả validation range cho `pain_level`/`mood_level`/`sleep_hours` | TDS §4.3 quyết định: painLevel/moodLevel ∈ [0,10], sleepHours ∈ [0,24] | Boundary Value Analysis test cases (`PPLOG-TC-U-006/007/008`) |
| L3 | SRS không đặc tả PATCH vs PUT semantics | ADR-PPLOG-005: PATCH partial-update, field vắng mặt giữ nguyên | `PPLOG-TC-U-002` verify field không gửi giữ nguyên giá trị cũ |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Update Postpartum Log bao gồm các layer:
├── Domain (PostpartumLog entity — reused from UC189, no changes)
├── Service (PostpartumLogServiceImpl.updateLog() — mock JPA Repository với Mockito)
├── Controller (PostpartumLogController PATCH endpoint — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full PATCH flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-190 §3.3.11.4` | Update content, access denied (E1), invalid data rejected (E2) |
| `ADR-PPLOG-004 (Accepted)` | Immutable-field boundary (logDate) |
| `ADR-PPLOG-005 (Accepted)` | Partial-update semantics |
| `BR-RBAC` | MOTHER-only role guard |
| `BR-PRIVACY` | ACTIVE-only guard, event payload minimum-necessary |
| `CB-HEALTH-IMP-006 §8/§9/§10` | Interface contract, API shape, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Update happy path — single field | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-001` |
| TC-COND-002 | Update happy path — multi field, unspecified fields unchanged | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-002` |
| TC-COND-003 | Update — log not found / already deleted | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-003` |
| TC-COND-004 | Update — journey not found (data integrity) | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-004` |
| TC-COND-005 | Update — IDOR: caller not journey owner | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-005` |
| TC-COND-006 | painLevel boundary [0,10] | `UpdatePostpartumLogRequest` validation | `PPLOG-TC-U-006` |
| TC-COND-007 | moodLevel boundary [0,10] | `UpdatePostpartumLogRequest` validation | `PPLOG-TC-U-007` |
| TC-COND-008 | sleepHours boundary [0,24] | `UpdatePostpartumLogRequest` validation | `PPLOG-TC-U-008` |
| TC-COND-009 | logDate immutability — request body with `logDate` ignored/rejected | `PostpartumLogController` / DTO contract | `PPLOG-TC-U-009` |
| TC-COND-010 | bleedingLevel invalid enum value rejected | `UpdatePostpartumLogRequest` validation | `PPLOG-TC-U-010` |
| TC-COND-011 | `updated_at` refreshed, `created_at` unchanged | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-011` |
| TC-COND-012 | `PostpartumLogUpdated` event payload contains only changed field names | `PostpartumLogServiceImpl.updateLog()` | `PPLOG-TC-U-012` |
| TC-COND-013 | Controller — unauthenticated request | `PostpartumLogController` | `PPLOG-TC-U-013` |
| TC-COND-014 | Controller — non-MOTHER role rejected | `PostpartumLogController` | `PPLOG-TC-U-014` |
| TC-COND-015 | Integration: full PATCH flow, DB verifies partial-update | Full stack | `PPLOG-TC-U-INT-001` |
| TC-COND-016 | Integration: update DELETED log rejected (404) | Full stack | `PPLOG-TC-U-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | ownership (owner/non-owner/journey-missing) | 3 partitions |
| Boundary Value Analysis | `painLevel`/`moodLevel` (0, 10, -1, 11), `sleepHours` (0, 24, -0.1, 24.1) | Validation range enforcement |
| State Transition Testing | ACTIVE-only guard (reject update on DELETED) | FSM invariant |
| Error Guessing | IDOR, immutable-field injection via extra JSON keys | Security/data-integrity attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `MotherJourney { id: J1, ownerUserId: U1 }` | Owner journey |
| `FX-002` | DB seed | `MotherJourney { id: J2, ownerUserId: U2 }` | Non-owner journey |
| `FX-003` | DB seed | `PostpartumLog { id: L1, journeyId: J1, painLevel: 3, moodLevel: 6, sleepHours: 5.5, status: ACTIVE }` | Update target — happy path |
| `FX-004` | DB seed | `PostpartumLog { id: L2, journeyId: J1, status: DELETED }` | Update-rejected fixture |
| `FX-005` | Request | `{ "painLevel": 5 }` | Single-field partial update |
| `FX-006` | Request | `{ "painLevel": 99 }` | Boundary violation (painLevel) |
| `FX-007` | Request | `{ "sleepHours": 25.0 }` | Boundary violation (sleepHours) |
| `FX-008` | Request | `{ "bleedingLevel": "SEVERE" }` | Invalid enum value (not in NONE\|LIGHT\|MODERATE\|HEAVY) |
| `FX-009` | JWT | `{ sub: U1, role: MOTHER }` | Auth context — owner |
| `FX-010` | JWT | `{ sub: U2, role: MOTHER }` | Auth context — non-owner (attacker) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// PostpartumLogUpdateTestFactory.java
class PostpartumLogUpdateTestFactory {

    static final UUID JOURNEY_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID JOURNEY_OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A2");
    static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");
    static final UUID LOG_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C1");

    static PostpartumLog makeActiveLog() {
        return PostpartumLog.builder()
                .id(LOG_ID)
                .journeyId(JOURNEY_ID)
                .logDate(LocalDate.of(2026, 7, 1))
                .painLevel((short) 3)
                .bleedingLevel("LIGHT")
                .moodLevel((short) 6)
                .sleepHours(new BigDecimal("5.5"))
                .breastfeedingNote("original note")
                .symptomNote("original symptom")
                .status(PostpartumLogStatus.ACTIVE)
                .build();
    }

    static PostpartumLog makeActiveLog(Consumer<PostpartumLog> overrides) {
        PostpartumLog log = makeActiveLog();
        overrides.accept(log);
        return log;
    }

    static UpdatePostpartumLogRequest makeRequest(Consumer<UpdatePostpartumLogRequestBuilder> overrides) {
        var builder = UpdatePostpartumLogRequest.builder();
        overrides.accept(builder);
        return builder.build();
    }
}
```

---

### PPLOG-TC-U-001 — Update happy path — single field

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog(UUID, UUID, UpdatePostpartumLogRequest)`
**Test File:** `src/test/java/com/carebridge/backend/health/unit/PostpartumLogServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-HEALTH-IMP-006 §8.1`

**Preconditions:** `FX-003` seeded (ACTIVE), caller = owner `U1`.

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(LOG_ID, ACTIVE)` → `Optional.of(makeActiveLog())`
2. Mock `journeyRepository.findById(JOURNEY_ID)` → owner journey
3. Call `service.updateLog(LOG_ID, JOURNEY_OWNER_ID, request{painLevel: 5})`

**Expected Result (PASS):** returned `painLevel == 5`; `save()` called once with entity where only `painLevel` changed.
**Expected Result (FAIL):** other fields altered, or `save()` not invoked.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-002 — Update happy path — unspecified fields unchanged (partial-update)

**Severity:** `CRITICAL`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-PPLOG-005 §Decision`

**Preconditions:** `FX-003` seeded with `moodLevel=6`, `sleepHours=5.5`.

**Test Steps:**
1. Call `service.updateLog(LOG_ID, JOURNEY_OWNER_ID, request{painLevel: 5})` — only `painLevel` sent
2. Capture entity passed to `save()`

**Expected Result (PASS):** `moodLevel` still `6`, `sleepHours` still `5.5` — NOT nulled out.
**Expected Result (FAIL):** `moodLevel`/`sleepHours` overwritten to `null` — data-loss bug (violates ADR-PPLOG-005).

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-003 — Update — log not found or already deleted

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-HEALTH-IMP-006 §10 PPLOG-001`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(id, ACTIVE)` → `Optional.empty()`
2. Call `service.updateLog(id, callerId, request)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-001", ...)`.
**Expected Result (FAIL):** NPE, wrong code, or update silently no-ops.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-004 — Update — parent journey not found (data integrity)

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-HEALTH-IMP-006 §10 PPLOG-002`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus` → present
2. Mock `journeyRepository.findById(journeyId)` → `Optional.empty()`
3. Call `service.updateLog(...)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-002", ...)`.
**Expected Result (FAIL):** NPE.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-005 — Update — IDOR: caller not journey owner

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-HEALTH-IMP-006 §10 PPLOG-003`

**Preconditions:** `FX-003` owned by `U1`; attacker `U2`.

**Test Steps (Attack Simulation):**
1. Mock `logRepository.findByIdAndStatus(LOG_ID, ACTIVE)` → log of `U1`'s journey
2. Mock `journeyRepository.findById(JOURNEY_ID)` → owner `U1`
3. Call `service.updateLog(LOG_ID, JOURNEY_OTHER_OWNER_ID, request{painLevel: 1})`

**Expected Result (PASS = hệ thống an toàn):** throws `BusinessException(403, "PPLOG-003", ...)`; `save()` never invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** `U1`'s data modified by `U2`.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-006 — painLevel boundary [0,10]

**Severity:** `HIGH`
**Feature Under Test:** `UpdatePostpartumLogRequest` Bean Validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-HEALTH-IMP-006 §4.3 NFR — Security/Input validation`

**Test Steps (Boundary Value Analysis):**
1. Validate `painLevel = -1` → expect violation
2. Validate `painLevel = 0` → expect valid
3. Validate `painLevel = 10` → expect valid
4. Validate `painLevel = 11` → expect violation

**Expected Result (PASS):** violations exactly at `-1` and `11`; `0`/`10` pass.
**Expected Result (FAIL):** off-by-one boundary error (e.g. `10` rejected).

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-007 — moodLevel boundary [0,10]

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdatePostpartumLogRequest` Bean Validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-HEALTH-IMP-006 §4.3`

**Test Steps:** same BVA pattern as `PPLOG-TC-U-006` applied to `moodLevel`.

**Expected Result (PASS):** violations exactly at `-1` and `11`.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-008 — sleepHours boundary [0,24]

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdatePostpartumLogRequest` Bean Validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-HEALTH-IMP-006 §4.3`

**Test Steps (Boundary Value Analysis):**
1. `sleepHours = -0.1` → violation
2. `sleepHours = 0` → valid
3. `sleepHours = 24` → valid
4. `sleepHours = 24.1` → violation

**Expected Result (PASS):** boundaries enforced exactly.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-009 — logDate immutability boundary (⚠️ depends on ADR-PPLOG-004 confirmation)

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogController` PATCH endpoint — DTO contract surface
**Test File:** `src/test/java/com/carebridge/backend/health/web/PostpartumLogControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-HEALTH-IMP-006 §3 ADR-PPLOG-004 (Accepted 2026-07-03)`

**Preconditions:** ⚠️ This test case's expected behavior is contingent on Product Owner confirming ADR-PPLOG-004. If Product Owner decides `logDate` SHOULD be mutable, this test case must be rewritten (remove assertion, add positive-path test instead) before implementation proceeds.

**Test Steps:**
1. Send `PATCH /api/v1/postpartum-logs/{logId}` with body `{"logDate": "2026-08-01"}`
2. Since `UpdatePostpartumLogRequest` has no `logDate` field, Jackson either silently ignores the unknown property (default) or rejects with 400 if `FAIL_ON_UNKNOWN_PROPERTIES` is enabled at the `ObjectMapper` level.

**Expected Result (PASS — per current ADR-PPLOG-004 decision):** the persisted `PostpartumLog.logDate` is unchanged after the call (verify via DB assertion in integration variant `PPLOG-TC-U-INT-001`), regardless of whether the unknown JSON key causes 400 or is silently dropped.
**Expected Result (FAIL):** `logDate` is modified in the database — immutability invariant violated.

**Current Status:** 🔴 Not written
**Implementation Note:** Decide and document explicitly in code review whether unknown-property strict mode is enabled project-wide (affects error code returned, not the invariant itself).

---

### PPLOG-TC-U-010 — bleedingLevel invalid enum value rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdatePostpartumLogRequest` Bean Validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-HEALTH-IMP-006 §8.1 @Pattern(NONE|LIGHT|MODERATE|HEAVY)`

**Test Steps:**
1. Validate `bleedingLevel = "SEVERE"` (not in allowed set) → expect violation
2. Validate `bleedingLevel = "HEAVY"` → expect valid

**Expected Result (PASS):** `"SEVERE"` rejected with `PPLOG-004`, `"HEAVY"` accepted.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-011 — updated_at refreshed, created_at unchanged

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()` (integration-level, relies on `@UpdateTimestamp`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-HEALTH-IMP-006 §4.2 NFR — Accuracy`

**Test Steps:**
1. Seed log with known `createdAt`/`updatedAt`
2. Perform update
3. Reload from DB

**Expected Result (PASS):** `updatedAt` > original `updatedAt`; `createdAt` unchanged.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-012 — PostpartumLogUpdated event payload minimum-necessary

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.updateLog()` event publishing
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-HEALTH-IMP-006 §17 C6`, `BR-PRIVACY`

**Test Steps:**
1. Capture `ApplicationEventPublisher.publishEvent()` argument via `ArgumentCaptor`
2. Call `service.updateLog(LOG_ID, JOURNEY_OWNER_ID, request{painLevel: 5, symptomNote: "x"})`
3. Inspect captured `PostpartumLogUpdated.payload()`

**Expected Result (PASS):** `payload.changedFields()` contains exactly `["painLevel", "symptomNote"]` (order-insensitive); payload record has NO field carrying the actual numeric/text values.
**Expected Result (FAIL):** payload contains raw health values — BR-PRIVACY violation.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-013 — Controller — unauthenticated request

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306`
**Feature Under Test:** `PostpartumLogController` PATCH (Spring Security filter chain)
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. `PATCH /api/v1/postpartum-logs/{id}` with no `Authorization` header

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, `IAM-001`.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-014 — Controller — non-MOTHER role rejected

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `PostpartumLogController` `@PreAuthorize("hasRole('MOTHER')")`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Authenticate as `FAMILY` role
2. `PATCH /api/v1/postpartum-logs/{id}` with valid body

**Expected Result (PASS):** `403 Forbidden`.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### PPLOG-TC-U-INT-001 — Full flow: seed → PATCH → verify DB partial-update

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: seed DB → PATCH /api/v1/postpartum-logs/{id} → assert DB state`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied (incl. `V20260707091000`)
- Seed: `FX-001` (journey), `FX-003` (log with `painLevel=3, moodLevel=6, sleepHours=5.5`)

**Test Steps:**
1. `PATCH /api/v1/postpartum-logs/{logId}` with `{"painLevel": 5}` and valid owner JWT
2. Reload entity from DB via `PostpartumLogRepository`

**Expected Result (PASS):**
- `200`, response `data.painLevel == 5`
- DB: `pain_level = 5`, `mood_level = 6` (unchanged), `sleep_hours = 5.5` (unchanged), `updated_at` refreshed, `log_date` unchanged.

**DB Assertion:**
```java
PostpartumLog record = logRepository.findById(logId).orElseThrow();
assertThat(record.getPainLevel()).isEqualTo((short) 5);
assertThat(record.getMoodLevel()).isEqualTo((short) 6);
assertThat(record.getLogDate()).isEqualTo(LocalDate.of(2026, 7, 1));
```

**Current Status:** 🔴 Not written

---

### PPLOG-TC-U-INT-002 — Update rejected for DELETED log (404, cross-UC dependency on UC191)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: seed log status=DELETED → PATCH → assert 404`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** Insert log directly with `status='DELETED'` via `JdbcTemplate` (simulating post-UC191 state).

**Test Steps:**
1. `PATCH /api/v1/postpartum-logs/{deletedLogId}` with `{"painLevel": 5}`

**Expected Result (PASS):** `404`, `code == "PPLOG-001"`; DB row unchanged (`pain_level` not modified).
**Expected Result (FAIL):** update succeeds on a DELETED log — soft-delete invariant broken.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PPLOG-TC-U-001` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-002` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-003` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-004` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-005` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-006` | `UpdatePostpartumLogRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-007` | `UpdatePostpartumLogRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-008` | `UpdatePostpartumLogRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-009` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | ⚠️ re-verify after ADR-PPLOG-004 confirmation |
| `PPLOG-TC-U-010` | `UpdatePostpartumLogRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-011` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-012` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-013` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-014` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-INT-001` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-U-INT-002` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Override
public PostpartumLogResponse updateLog(UUID logId, UUID callerId, UpdatePostpartumLogRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PPLOG-TC-U-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-U-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-U-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-U-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-006` đã được review và approve
- [x] **ADR-PPLOG-004 đã được Product Owner xác nhận** (Accepted 2026-07-03) — không còn block `PPLOG-TC-U-009`
- [ ] UC189 code đã tồn tại và pass test (entity/repository dependency)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `PostpartumLogServiceImpl.updateLog()`
- [ ] Không có business logic trong `PostpartumLogController`
- [ ] Không có PII/secret xuất hiện plaintext trong logs (đặc biệt event payload — `PPLOG-TC-U-012`)
- [ ] Mobile: `flutter test` xanh cho `postpartum_log_service_test.dart` (update method)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence:** `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation:** không có shared mutable state giữa tests (factory pattern §4)
- [ ] **Oracle Source:** mọi expected value trong assert có ghi rõ nguồn (BR/ADR/TDS section)

### Suspension Criteria

- ADR-PPLOG-004 chưa được Product Owner xác nhận
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới (tái sử dụng schema UC189) — revert chỉ cần code
git checkout -- src/main/java/com/carebridge/backend/health/dto/UpdatePostpartumLogRequest.java
git checkout -- src/main/java/com/carebridge/backend/health/service/
git checkout -- src/main/java/com/carebridge/backend/health/controller/PostpartumLogController.java
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
*Status: Draft — pending user review/approval per `.claude/rules/implement-flow.md`. ⚠️ `PPLOG-TC-U-009` contingent on ADR-PPLOG-004 confirmation.*
