# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC191 — Delete Postpartum Log

**Document ID:** `FPT-EDU-TDD-UC191-001`
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
- `04_Implement/UC191_DeletePostpartumLog/UC191_DeletePostpartumLog_TDS.md` (`CB-HEALTH-IMP-007`) — Technical Specification
- `04_Implement/UC189_ViewPostpartumLogs/UC189_ViewPostpartumLogs_TDS.md` — upstream entity/repository owner
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.11.5` — Functional requirements (UC-191)
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
| `2026-07-03` | `AI Agent — Technical Architect` | Khởi tạo tài liệu — TDD spec cho UC191 Delete Postpartum Log |

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
| **Feature / Gap ID** | `GAP-UC191` |
| **Module** | `Delete Postpartum Log — Bounded Context: health` |
| **Spec gốc** | `CB-HEALTH-IMP-007` |
| **Priority** | 🟡 P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 — Device Sync And Care Edge Cases` |
| **Milestone** | `TV2-Bách batch` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `Luật 91/2025 (PDPA)` |
| **Upstream Dependencies** | `UC189` (`PostpartumLog`, `PostpartumLogRepository`), `journey.MotherJourneyRepository`, `IAM (JWT)` |
| **Downstream Consumers** | `UC189` (list/detail phải loại trừ log sau delete) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-007 §17`, `ADR-PPLOG-006/007 (Accepted, mirrors ADR-HEALTH-004/005)` |
| **Constraints Injected** | C1 (ownership), C2 (soft-delete only), C3 (ACTIVE-only guard), C4 (identity), C5 (thin controller), C6 (event payload minimum-necessary) |
| **Model** | `Claude — Technical Architect Agent` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.11.5 không đặc tả behavior của DELETE gọi lần 2 | ADR-PPLOG-007 (Accepted): idempotent-read semantics — 404 lần 2, mirrors UC188 ADR-HEALTH-005 | `PPLOG-TC-D-004` verify lần DELETE thứ 2 trả 404 |
| L2 | SRS không đặc tả restore | Out-of-scope, đánh dấu Open (nhất quán UC188) — không có endpoint restore trong batch này | Không có test case cho restore; ghi rõ trong Test Design Scope là out-of-scope |
| L3 | Không có FK constraint nào tham chiếu `postpartum_log_id` trong `V1__init_schema.sql` | Xác nhận an toàn để soft-delete mà không cần cascade logic | `PPLOG-TC-D-INT-001` không cần verify cascade — chỉ verify row tồn tại với status mới |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Delete Postpartum Log bao gồm các layer:
├── Domain (PostpartumLog entity — reused from UC189, no changes)
├── Service (PostpartumLogServiceImpl.deleteLog() — mock JPA Repository với Mockito)
├── Controller (PostpartumLogController DELETE endpoint — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full DELETE flow + cross-UC189 read verification)

Out-of-scope: restore endpoint (không tồn tại), hard-delete, bulk-delete.
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-191 §3.3.11.5` | Soft-delete happy path, access denied (E1) |
| `ADR-PPLOG-006 (Accepted)` | Soft-delete-only semantics (no hard delete) |
| `ADR-PPLOG-007 (Accepted)` | Idempotent 404-on-repeat semantics |
| `BR-RBAC` | MOTHER-only role guard |
| `BR-PRIVACY` | ACTIVE-only guard, event payload minimum-necessary, physical row retention |
| `CB-HEALTH-IMP-007 §8/§9/§10` | Interface contract, API shape, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Delete happy path — owner deletes own ACTIVE log | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-001` |
| TC-COND-002 | Delete — log not found (never existed) | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-002` |
| TC-COND-003 | Delete — journey not found (data integrity) | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-003` |
| TC-COND-004 | Delete — repeated delete on already-DELETED log (idempotent 404) | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-004` |
| TC-COND-005 | Delete — IDOR: caller not journey owner | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-005` |
| TC-COND-006 | Delete never calls hard-delete repository method | `PostpartumLogServiceImpl.deleteLog()` (Mockito verify) | `PPLOG-TC-D-006` |
| TC-COND-007 | `PostpartumLogDeleted` event payload minimum-necessary (no health values) | `PostpartumLogServiceImpl.deleteLog()` | `PPLOG-TC-D-007` |
| TC-COND-008 | Controller — unauthenticated request | `PostpartumLogController` | `PPLOG-TC-D-008` |
| TC-COND-009 | Controller — non-MOTHER role rejected | `PostpartumLogController` | `PPLOG-TC-D-009` |
| TC-COND-010 | Integration: full DELETE flow, row physically retained with status=DELETED | Full stack | `PPLOG-TC-D-INT-001` |
| TC-COND-011 | Integration: post-delete, UC189 GET (list + detail) excludes the log | Full stack (cross-UC) | `PPLOG-TC-D-INT-002` |
| TC-COND-012 | Integration: post-delete, UC190 PATCH on same log rejected 404 | Full stack (cross-UC) | `PPLOG-TC-D-INT-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | ownership (owner/non-owner/journey-missing), log state (ACTIVE/DELETED/non-existent) | Full state × ownership matrix |
| State Transition Testing | `status` ACTIVE → DELETED (one-way) | FSM invariant enforcement |
| Error Guessing | IDOR via crafted `logId` of another Mother's journey; repeated DELETE (retry storm simulation) | Security + idempotency attack vectors |
| Interaction-based Testing | `Mockito.verify(logRepository, never()).deleteById(...)` | Prevent hard-delete regression (C2) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `MotherJourney { id: J1, ownerUserId: U1 }` | Owner journey |
| `FX-002` | DB seed | `MotherJourney { id: J2, ownerUserId: U2 }` | Non-owner journey |
| `FX-003` | DB seed | `PostpartumLog { id: L1, journeyId: J1, status: ACTIVE }` | Delete target — happy path |
| `FX-004` | DB seed | `PostpartumLog { id: L2, journeyId: J1, status: DELETED }` | Idempotency fixture (already deleted) |
| `FX-005` | JWT | `{ sub: U1, role: MOTHER }` | Auth context — owner |
| `FX-006` | JWT | `{ sub: U2, role: MOTHER }` | Auth context — non-owner (attacker) |
| `FX-007` | JWT | `{ sub: U1, role: FAMILY }` | Auth context — wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// PostpartumLogDeleteTestFactory.java
class PostpartumLogDeleteTestFactory {

    static final UUID JOURNEY_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID JOURNEY_OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A2");
    static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");
    static final UUID LOG_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C1");

    static MotherJourney makeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(JOURNEY_OWNER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    static PostpartumLog makeActiveLog() {
        return PostpartumLog.builder()
                .id(LOG_ID)
                .journeyId(JOURNEY_ID)
                .logDate(LocalDate.of(2026, 7, 1))
                .painLevel((short) 3)
                .bleedingLevel("LIGHT")
                .moodLevel((short) 6)
                .sleepHours(new BigDecimal("5.5"))
                .status(PostpartumLogStatus.ACTIVE)
                .build();
    }

    static PostpartumLog makeActiveLog(Consumer<PostpartumLog> overrides) {
        PostpartumLog log = makeActiveLog();
        overrides.accept(log);
        return log;
    }
}
```

---

### PPLOG-TC-D-001 — Delete happy path

**Severity:** `CRITICAL`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/health/unit/PostpartumLogServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-HEALTH-IMP-007 §8.1`

**Preconditions:** `FX-001` journey (owner `U1`), `FX-003` log (ACTIVE, journey `J1`).

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(LOG_ID, ACTIVE)` → `Optional.of(makeActiveLog())`
2. Mock `journeyRepository.findById(JOURNEY_ID)` → `Optional.of(makeJourney())`
3. Call `service.deleteLog(LOG_ID, JOURNEY_OWNER_ID)`

**Expected Result (PASS):** no exception; `save()` invoked once with entity where `status == DELETED`; `ApplicationEventPublisher.publishEvent()` called with `PostpartumLogDeleted`.
**Expected Result (FAIL):** exception thrown, or `save()` not invoked, or status not transitioned.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-002 — Delete — log not found (never existed)

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-HEALTH-IMP-007 §10 PPLOG-001`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(id, ACTIVE)` → `Optional.empty()`
2. Call `service.deleteLog(id, callerId)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-001", ...)`; `save()` never invoked.
**Expected Result (FAIL):** NPE, wrong code.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-003 — Delete — parent journey not found (data integrity)

**Severity:** `MEDIUM`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-HEALTH-IMP-007 §10 PPLOG-002`

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus` → present
2. Mock `journeyRepository.findById` → `Optional.empty()`
3. Call `service.deleteLog(...)`

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-002", ...)`.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-004 — Delete — repeated delete on already-DELETED log (idempotent 404)

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-HEALTH-IMP-007 §3 ADR-PPLOG-007 (Accepted)`

**Preconditions:** `FX-004` log already `DELETED`.

**Test Steps:**
1. Mock `logRepository.findByIdAndStatus(id, ACTIVE)` → `Optional.empty()` (because log is DELETED, filter excludes it)
2. Call `service.deleteLog(id, callerId)` a second time

**Expected Result (PASS):** throws `BusinessException(404, "PPLOG-001", ...)` — same as "not found" case, confirming idempotent-read semantics.
**Expected Result (FAIL):** exception different from first-time-not-found case, or `save()` invoked again (double-transition attempt).

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-005 — Delete — IDOR: caller not journey owner

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-HEALTH-IMP-007 §10 PPLOG-003`

**Preconditions:** `FX-003` owned by `U1`; attacker `U2`.

**Test Steps (Attack Simulation):**
1. Mock `logRepository.findByIdAndStatus(LOG_ID, ACTIVE)` → log of `U1`'s journey
2. Mock `journeyRepository.findById(JOURNEY_ID)` → owner `U1`
3. Call `service.deleteLog(LOG_ID, JOURNEY_OTHER_OWNER_ID)` (caller `U2`)

**Expected Result (PASS = hệ thống an toàn):** throws `BusinessException(403, "PPLOG-003", ...)`; `save()` never invoked — log remains ACTIVE.
**Expected Result (FAIL = lỗ hổng tồn tại):** `U1`'s log deleted by `U2` — unauthorized data destruction.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-006 — Delete never calls hard-delete repository method

**Severity:** `CRITICAL`
**CWE:** `CWE-404 — Improper Resource Shutdown or Release` *(analogized: improper permanent data destruction)*
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()` (interaction-based test)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-HEALTH-IMP-007 §17 C2`, `ADR-PPLOG-006`

**Test Steps:**
1. Execute happy-path delete (as in `PPLOG-TC-D-001`)
2. `Mockito.verify(logRepository, never()).deleteById(any())`
3. `Mockito.verify(logRepository, never()).delete(any(PostpartumLog.class))`

**Expected Result (PASS):** neither hard-delete method invoked — only `save()` used.
**Expected Result (FAIL):** any hard-delete method invoked — violates soft-delete-only invariant (BR-PRIVACY).

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-007 — PostpartumLogDeleted event payload minimum-necessary

**Severity:** `HIGH`
**Feature Under Test:** `PostpartumLogServiceImpl.deleteLog()` event publishing
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-HEALTH-IMP-007 §17 C6`, `BR-PRIVACY`

**Test Steps:**
1. Capture `ApplicationEventPublisher.publishEvent()` argument via `ArgumentCaptor`
2. Call `service.deleteLog(LOG_ID, JOURNEY_OWNER_ID)`
3. Inspect captured `PostpartumLogDeleted.payload()`

**Expected Result (PASS):** payload contains only `logId`, `journeyId`, `logDate` — no `painLevel`/`bleedingLevel`/`moodLevel`/`sleepHours`/notes fields present anywhere in the record.
**Expected Result (FAIL):** payload leaks raw health values.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-008 — Controller — unauthenticated request

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306`
**Feature Under Test:** `PostpartumLogController` DELETE (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/health/web/PostpartumLogControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. `DELETE /api/v1/postpartum-logs/{id}` with no `Authorization` header

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, `IAM-001`.
**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-009 — Controller — non-MOTHER role rejected

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `PostpartumLogController` `@PreAuthorize("hasRole('MOTHER')")`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Authenticate as `FAMILY` role (`FX-007`)
2. `DELETE /api/v1/postpartum-logs/{id}`

**Expected Result (PASS):** `403 Forbidden`.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### PPLOG-TC-D-INT-001 — Full flow: DELETE → verify DB row physically retained with status=DELETED

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: seed → DELETE /api/v1/postpartum-logs/{id} → assert DB row survives with status=DELETED`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied (incl. `V20260707091000`)
- Seed: `FX-001` (journey), `FX-003` (log, ACTIVE)

**Test Steps:**
1. `DELETE /api/v1/postpartum-logs/{logId}` with valid owner JWT
2. Query DB directly via `JdbcTemplate` / repository (bypassing status filter)

**Expected Result (PASS):**
- `204` response
- Row still exists: `SELECT COUNT(*) FROM postpartum_logs WHERE postpartum_log_id = ?` → `1`
- `status = 'DELETED'`

**DB Assertion:**
```java
PostpartumLog record = logRepository.findById(logId).orElseThrow(); // bypass status filter — direct findById
assertThat(record.getStatus()).isEqualTo(PostpartumLogStatus.DELETED);
```

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-INT-002 — Post-delete: UC189 GET (list + detail) excludes the log (cross-UC dependency)

**Severity:** `CRITICAL`
**Feature Under Test:** `Cross-UC integration: UC191 DELETE followed by UC189 GET`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. Seed and `DELETE` a log (as in `PPLOG-TC-D-INT-001`)
2. `GET /api/v1/postpartum-logs?journeyId={journeyId}` → assert deleted log absent from `data[]`
3. `GET /api/v1/postpartum-logs/{logId}` → assert `404 PPLOG-001`

**Expected Result (PASS):** both assertions hold — confirms UC189's `status=ACTIVE` filter correctly excludes UC191's soft-deleted record end-to-end.
**Expected Result (FAIL):** deleted log still visible via either UC189 endpoint — privacy/data-integrity regression.

**Current Status:** 🔴 Not written

---

### PPLOG-TC-D-INT-003 — Post-delete: UC190 PATCH on same log rejected (cross-UC dependency)

**Severity:** `HIGH`
**Feature Under Test:** `Cross-UC integration: UC191 DELETE followed by UC190 PATCH`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/PostpartumLogIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Test Steps:**
1. Seed and `DELETE` a log
2. `PATCH /api/v1/postpartum-logs/{logId}` with `{"painLevel": 5}`

**Expected Result (PASS):** `404 PPLOG-001`; DB row's `pain_level` unchanged (still original value, not `5`) — confirms UC190's `findByIdAndStatus(id, ACTIVE)` guard blocks writes to soft-deleted records.
**Expected Result (FAIL):** update succeeds on a DELETED log.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `PPLOG-TC-D-001` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-002` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-003` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-004` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-005` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-006` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-007` | `PostpartumLogServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-008` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-009` | `PostpartumLogControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-INT-001` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-INT-002` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `PPLOG-TC-D-INT-003` | `PostpartumLogIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Override
public void deleteLog(UUID logId, UUID callerId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PPLOG-TC-D-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-D-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-D-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `PPLOG-TC-D-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-007` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] UC189 code (entity/repository/migration `V20260707091000`) đã tồn tại và pass test
- [ ] UC190 code khuyến nghị đã tồn tại (để `PPLOG-TC-D-INT-003` chạy được) — nếu chưa, đánh dấu test này `Blocked`, không skip vĩnh viễn
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `PostpartumLogServiceImpl.deleteLog()`
- [ ] Không có business logic trong `PostpartumLogController`
- [ ] Không có PII/secret xuất hiện plaintext trong logs (đặc biệt event payload — `PPLOG-TC-D-007`)
- [ ] Mobile: `flutter test` xanh cho `postpartum_log_service_test.dart` (delete method)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence:** `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation:** không có shared mutable state giữa tests (factory pattern §4)
- [ ] **Oracle Source:** mọi expected value trong assert có ghi rõ nguồn (BR/ADR/TDS section)
- [ ] **Hard-delete regression guard:** `PPLOG-TC-D-006` xanh — xác nhận không có đường code nào gọi `deleteById()`/`delete()`

### Suspension Criteria

- UC189 chưa hoàn thành (blocker dependency)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới (tái sử dụng schema UC189) — revert chỉ cần code
git checkout -- src/main/java/com/carebridge/backend/health/service/
git checkout -- src/main/java/com/carebridge/backend/health/controller/PostpartumLogController.java
git checkout -- src/test/java/com/carebridge/backend/health/

# Nếu cần khôi phục dữ liệu bị soft-delete nhầm trong quá trình test trên staging (KHÔNG chạy trên production):
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE postpartum_logs SET status='ACTIVE' WHERE postpartum_log_id IN (...) AND updated_at > '[test_run_start_ts]';"

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
