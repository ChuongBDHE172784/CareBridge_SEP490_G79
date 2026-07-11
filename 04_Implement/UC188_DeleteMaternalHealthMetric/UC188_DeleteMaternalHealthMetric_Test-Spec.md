# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC188 Delete Maternal Health Metric

**Document ID:** `CB-HEALTH-TDD-188`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (targeted health backend tests PASS; full regression blocked by non-health Family/Exercise failures)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(bắt buộc — module PII sức khỏe thai sản)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627100200__add_maternal_metric_status.sql`
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.11.2`
- `04_Implement/UC188_DeleteMaternalHealthMetric/UC188_DeleteMaternalHealthMetric_TDS.md` (`CB-HEALTH-IMP-004`)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/IHealthMetricService.java` (existing, verified — only `getMetricDetail()` today)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/impl/HealthMetricServiceImpl.java` (existing, verified)

> **Quy ước TDD:** Test-first. Thứ tự: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data (UUID literals).

---

## CHANGELOG


| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted health backend test pass; full regression remains blocked outside health scope. |
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC188 Delete Maternal Health Metric (Draft) |

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
| **Feature / Gap ID** | `UC-188` |
| **Module** | Maternal Health — Delete Maternal Health Metric |
| **Spec gốc** | `CB-HEALTH-IMP-004` |
| **Priority** | 🔴 P0 (Sensitive-PII delete path) |
| **Sprint** | Sprint 4 — Device Sync And Care Edge Cases |
| **Milestone** | M3 Alpha |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | PDPA / Luật 91/2025, BR-RBAC, BR-PRIVACY |
| **Upstream Dependencies** | `MaternalHealthMetric`, `MetricStatus`, `MaternalHealthMetricRepository` (all existing — UC187), `MotherJourneyRepository` (ownership) |
| **Downstream Consumers** | UC69 ViewDeviceDataTrend (must exclude DELETED metrics), audit trail |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-HEALTH-IMP-004 §17`, ADR-HEALTH-004, ADR-HEALTH-005 |
| **Constraints Injected** | C1-C5 per TDS §17.1 |
| **Model** | Claude Sonnet 5 |
| **Trust Level** | T2 → T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|---------------------------|-------------------------------|------------------------------|
| L1 | A naive reading of "delete" could imply `metricRepository.delete()`/`deleteById()` (hard delete) | ADR-HEALTH-004 mandates soft-delete only — `status` column already exists (`V20260627100200`), reused, no new migration | Tests assert `verify(metricRepository, never()).delete(any())` / `never()).deleteById(any())` on every path, and assert `save()` is called with `status == DELETED` on the happy path |
| L2 | Naive design might treat a repeat-DELETE on an already-DELETED metric as a distinct "already deleted" 200/409 response | ADR-HEALTH-005 explicitly chose Option A: repeat delete returns the SAME `404 METRIC-001` as "never existed", for consistency with UC187's `findByIdAndStatus(id, ACTIVE)` semantics (no state leak) | Tests assert idempotent-repeat-delete yields `METRIC-001`/404, identical to non-existent-id case, NOT a distinct message |
| L3 | Ownership might be assumed to be a direct `owner_user_id` column on `maternal_health_metrics` | No such column exists — ownership is resolved via `metric.journeyId → MotherJourney.ownerUserId` (confirmed in shipped `HealthMetricServiceImpl.getMetricDetail()`, reused for delete per TDS §8.1) | Tests mock `journeyRepository.findById()` explicitly and assert the ownership comparison is against `journey.getOwnerUserId()`, never a field on `MaternalHealthMetric` itself |
| L4 | Class-level `@Transactional(readOnly = true)` on `HealthMetricServiceImpl` (confirmed in current code, line 19) would silently make a state-changing `deleteMetric()` a no-op / throw if left unchanged | TDS §11.3 Chặng 1 requires `deleteMetric()` to carry a method-level `@Transactional` override (not read-only), while `getMetricDetail()` keeps `readOnly = true` | Integration test (`TC-INT-001`) asserts the DB row is actually mutated after the call — a readOnly-transaction bug would surface as either an exception or a silently-unpersisted `status`, both caught by this assertion |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Maternal Health (Delete Metric, UC-188) bao gồm các layer:
├── Services (HealthMetricServiceImpl.deleteMetric() — mock MaternalHealthMetricRepository +
│             MotherJourneyRepository + ApplicationEventPublisher với Mockito)
├── Controller (HealthMetricController.deleteMetric() — mock IHealthMetricService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, full stack DELETE /api/v1/health-metrics/{id})

Lưu ý phạm vi: getMetricDetail() (UC187, đã Approved/shipped) KHÔNG nằm trong phạm vi Red Gate của
Test-Spec này — chỉ deleteMetric() (method mới) là 🔴 RED. Xem §5.1 Red Gate Protocol.
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|------------------|
| SRS §3.3.11.2 UC-188 | Owner-only soft-delete of a Mother-entered metric |
| ADR-HEALTH-004 | Soft-delete via reused `status` column; no hard delete; no new migration |
| ADR-HEALTH-005 | Idempotent repeat-delete → `404 METRIC-001` (not a distinct "already deleted" state) |
| BR-RBAC / PRE-3 | Only the journey owner may delete the metric |
| BR-PRIVACY | Physical row must never be removed; audit trail preserved |
| POST-3 (SRS Postcondition) | Sensitive action recorded for audit via `MaternalHealthMetricDeleted` |
| TDS §10 Error Codes | `METRIC-001` (404, not found/deleted), `METRIC-002` (404, orphan journey), `METRIC-003` (403, not owner) — all reused from UC187, no new codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|--------------------|--------------------|----------------|
| TC-COND-001 | Owner deletes own ACTIVE metric (happy path) | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-001` |
| TC-COND-002 | Metric already DELETED → 404 (idempotent, no state leak) | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-002` |
| TC-COND-003 | Metric never existed → 404 (identical code to TC-002) | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-003` |
| TC-COND-004 | Non-owner (different Mother) attempts delete → 403 (IDOR) | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-004` |
| TC-COND-005 | Parent journey missing (data integrity edge) → 404 `METRIC-002` | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-005` |
| TC-COND-006 | Repository is never called with `delete()`/`deleteById()` on any path | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-006` |
| TC-COND-007 | Happy-path delete emits `MaternalHealthMetricDeleted` exactly once | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-007` |
| TC-COND-008 | Denied/blocked paths never emit `MaternalHealthMetricDeleted` | `HealthMetricServiceImpl.deleteMetric()` | `METRIC-DEL-TC-008` |
| TC-COND-009 | No JWT → 401 (controller layer) | `HealthMetricController.deleteMetric()` | `METRIC-DEL-TC-009` |
| TC-COND-010 | Non-`MOTHER` role → 403 at controller role gate | `HealthMetricController.deleteMetric()` | `METRIC-DEL-TC-010` |
| TC-COND-011 | IDOR — Mother B deletes Mother A's metric via full auth chain | `HealthMetricController` + `HealthMetricServiceImpl` | `METRIC-DEL-TC-SEC-001` |
| TC-COND-012 | Full stack DELETE happy path, row preserved with `status=DELETED` | Integration | `METRIC-DEL-TC-INT-001` |
| TC-COND-013 | Full stack DELETE then GET → 404 (UC187 read path excludes DELETED) | Integration | `METRIC-DEL-TC-INT-002` |
| TC-COND-014 | Full stack DELETE 403 (non-owner), DB state unchanged | Integration | `METRIC-DEL-TC-INT-003` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|--------------|----------------|---------------|
| Equivalence Partitioning | Caller identity: owner / non-owner (IDOR) / unauthenticated / wrong role | 4 partitions, only "owner + MOTHER role" is in the accept partition |
| State Transition Testing | `MetricStatus`: ACTIVE → DELETED (once), DELETED → (no further transition) | Confirms delete is one-way; encodes ADR-HEALTH-004 invariant |
| Boundary/Idempotency | Repeat-delete on already-DELETED row (TC-002) vs never-existed row (TC-003) | ADR-HEALTH-005 mandates identical 404 response for both — a classic "same output, different internal cause" boundary |
| Error Guessing / Security | IDOR via guessed/known `metricId` belonging to another Mother | OWASP A01:2021 |
| Negative Testing | Explicit non-mutation assertions: `delete()`/`deleteById()` never called; row untouched on 403/404 paths; no audit event on denial | Directly encodes ADR-HEALTH-004 "soft-delete only" + BR-PRIVACY |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|--------------------|--------------|
| `FX-188-001` | In-memory | `MaternalHealthMetric{id=METRIC_ID, journeyId=JOURNEY_ID, status=ACTIVE}` | Happy path |
| `FX-188-002` | In-memory | Same metric, `status=DELETED` | 404 repeat-delete case |
| `FX-188-003` | In-memory | `MotherJourney{id=JOURNEY_ID, ownerUserId=OWNER_ID}` | Ownership resolution |
| `FX-188-004` | In-memory | `metricRepository.findByIdAndStatus(id, ACTIVE)` → `Optional.empty()` | 404 never-existed / already-deleted case |
| `FX-188-005` | In-memory | `journeyRepository.findById(JOURNEY_ID)` → `Optional.empty()` | 404 `METRIC-002` orphan-journey edge |
| `FX-188-006` | JWT/Auth | `{sub: OWNER_ID, roles: [ROLE_MOTHER]}` | Owner caller |
| `FX-188-007` | JWT/Auth | `{sub: OTHER_MOTHER_ID, roles: [ROLE_MOTHER]}` | Non-owner IDOR caller |
| `FX-188-008` | JWT/Auth | `{sub: caller-uuid, roles: [ROLE_FAMILY]}` | Wrong-role controller-gate rejection |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// HealthMetricDeleteTestFactory.java
class HealthMetricDeleteTestFactory {

    static final UUID OWNER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID JOURNEY_ID      = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID METRIC_ID       = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID NONEXISTENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    static MaternalHealthMetric makeActiveMetric() {
        return MaternalHealthMetric.builder()
                .id(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("62.5"))
                .unit("kg")
                .measuredAt(Instant.parse("2026-07-01T08:00:00Z"))
                .status(MetricStatus.ACTIVE)
                .build();
    }

    static MaternalHealthMetric makeDeletedMetric() {
        MaternalHealthMetric m = makeActiveMetric();
        m.setStatus(MetricStatus.DELETED);
        return m;
    }

    static MotherJourney makeOwnedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .build();
    }
}
```

---

### METRIC-DEL-TC-001 — Owner deletes own ACTIVE metric (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED — genuinely new method
**Condition Ref:** `TC-COND-001`
**Oracle Source:** ADR-HEALTH-004 (soft-delete), TDS §8.1 Service Interface, §6.1 Sequence Diagram

**Preconditions:** `FX-188-001` (active metric) mocked from `findByIdAndStatus(METRIC_ID, ACTIVE)`; `FX-188-003` (journey owned by `OWNER_ID`) mocked from `journeyRepository.findById(JOURNEY_ID)`.

**Test Steps:**
1. Arrange mocks as above.
2. Act: `healthMetricService.deleteMetric(METRIC_ID, OWNER_ID)`.
3. Assert: no exception thrown; `verify(metricRepository).save(argThat(m -> m.getStatus() == MetricStatus.DELETED))`.

**Expected Result (PASS):** Metric status transitioned to `DELETED` via `save()`, no exception, method returns `void`.
**Expected Result (FAIL):** Exception thrown, or `save()` not called, or status not `DELETED`.

**Current Status:** 🔴 Not written
**Implementation Note:** `deleteMetric()` must reuse `findByIdAndStatus(id, ACTIVE)` — same pattern as `getMetricDetail()`.

---

### METRIC-DEL-TC-002 — Metric already DELETED → 404 (idempotent, no distinct message)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** ADR-HEALTH-005 Decision (Option A — repeat delete = 404, not a distinct state)

**Preconditions:** `metricRepository.findByIdAndStatus(METRIC_ID, ACTIVE)` mocked to return `Optional.empty()` (matches real repo behavior for a row already `DELETED`).

**Test Steps:**
1. Act: `healthMetricService.deleteMetric(METRIC_ID, OWNER_ID)`.
2. Assert: throws `BusinessException` with `HttpStatus.NOT_FOUND` and code `METRIC-001`.
3. Assert: `verify(metricRepository, never()).save(any())`.

**Expected Result (PASS):** `BusinessException(404, METRIC-001)`; no side effects; message is the same generic "not found or deleted" text as TC-003 (no state leak).
**Expected Result (FAIL):** A distinct "already deleted" message/code is returned (violates ADR-HEALTH-005), or `save()` invoked.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-003 — Metric never existed → 404 (same code as TC-002)

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** ADR-HEALTH-005; TDS §10 Error Codes

**Test Steps:**
1. Mock `findByIdAndStatus(NONEXISTENT_ID, ACTIVE)` → `Optional.empty()`.
2. Act: `healthMetricService.deleteMetric(NONEXISTENT_ID, OWNER_ID)`.
3. Assert: throws `BusinessException(404, METRIC-001)` — identical error code/message shape to `METRIC-DEL-TC-002`.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-004 — Non-owner attempts delete → 403 (IDOR)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** ADR-HEALTH-004 constraint C1, TDS §16 Authorization Matrix

**Preconditions:** `FX-188-001` (metric, journeyId=`JOURNEY_ID`); `FX-188-003` (journey owned by `OWNER_ID`); caller is `OTHER_MOTHER_ID`.

**Test Steps:**
1. Act: `healthMetricService.deleteMetric(METRIC_ID, OTHER_MOTHER_ID)`.
2. Assert: throws `BusinessException(403, METRIC-003)`.
3. Assert: `verify(metricRepository, never()).save(any())`.

**Expected Result (PASS):** Exception thrown; row untouched.
**Expected Result (FAIL):** No exception (over-permissive access) — the exact IDOR risk ADR-HEALTH-004 exists to prevent.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-005 — Parent journey missing (data integrity edge) → 404 `METRIC-002`

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** TDS §10 Error Codes (`METRIC-002`), reused verbatim from `getMetricDetail()`'s existing logic

**Preconditions:** `FX-188-001` (metric present, `journeyId=JOURNEY_ID`); `FX-188-005` — `journeyRepository.findById(JOURNEY_ID)` → `Optional.empty()`.

**Test Steps:**
1. Act: `healthMetricService.deleteMetric(METRIC_ID, OWNER_ID)`.
2. Assert: throws `BusinessException(404, METRIC-002)`.
3. Assert: `verify(metricRepository, never()).save(any())`.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-006 — Repository `delete()`/`deleteById()` never invoked (soft-delete only)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** ADR-HEALTH-004 Option B decision, §17.4 AP-AI-005

**Test Steps:**
1. Act: `healthMetricService.deleteMetric(METRIC_ID, OWNER_ID)` (happy path).
2. Assert: `verify(metricRepository, never()).delete(any())`; `verify(metricRepository, never()).deleteById(any())`.
3. Repeat assertion on the 403 (`TC-004`) and 404 (`TC-002`/`TC-003`) paths — hard-delete must never fire on any path.

**Expected Result (PASS):** `delete()`/`deleteById()` never called anywhere.
**Expected Result (FAIL):** Hard-delete invoked — violates BR-PRIVACY audit-retention requirement.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-007 — Happy-path delete emits `MaternalHealthMetricDeleted` exactly once

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS §7.1 Domain Event Catalog, POST-3 (SRS)

**Test Steps:**
1. Act: `healthMetricService.deleteMetric(METRIC_ID, OWNER_ID)`.
2. Assert: `verify(eventPublisher, times(1)).publishEvent(argThat(evt -> evt instanceof MaternalHealthMetricDeleted && ((MaternalHealthMetricDeleted) evt).payload().metricId().equals(METRIC_ID)))`.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-008 — Denied/blocked paths never emit `MaternalHealthMetricDeleted`

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricServiceImpl.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricServiceDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** §7.1 Domain Event Catalog; consistency with BR-PRIVACY audit-trail integrity

**Test Steps:**
1. Trigger the 403 path (`TC-004`) — assert `verify(eventPublisher, never()).publishEvent(any(MaternalHealthMetricDeleted.class))`.
2. Trigger the 404 path (`TC-002`/`TC-003`) — assert same.
3. Trigger the 404 `METRIC-002` path (`TC-005`) — assert same.

**Expected Result (PASS):** Zero `MaternalHealthMetricDeleted` events on any non-2xx path.
**Expected Result (FAIL):** An event is published despite the operation being blocked (misleading audit trail).

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-009 — No JWT → 401 (controller layer)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricController.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Test Steps:**
1. `@WebMvcTest(HealthMetricController.class)`, no `Authorization` header.
2. `mockMvc.perform(delete("/api/v1/health-metrics/{id}", METRIC_ID))`.
3. Assert status 401.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-010 — Non-`MOTHER` role → 403 at controller role gate

**Severity:** `HIGH`
**Feature Under Test:** `HealthMetricController.deleteMetric()`
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricControllerDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** TDS §9.1 Endpoints Table (`@PreAuthorize("hasRole('MOTHER')")`), §16 Auth Matrix

**Test Steps:**
1. `@WebMvcTest(HealthMetricController.class)` with a valid JWT for role `FAMILY` (parametrized also for `EXPERT`, `SYSTEM_ADMIN`).
2. `mockMvc.perform(delete("/api/v1/health-metrics/{id}", METRIC_ID).header("Authorization", familyJwt))`.
3. Assert status 403; assert `healthMetricService.deleteMetric()` is NEVER invoked.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### METRIC-DEL-TC-SEC-001 — IDOR: Mother B deletes Mother A's metric (full auth chain)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Legal:** PDPA — unauthorized deletion of another data subject's maternal health PII
**Feature Under Test:** `HealthMetricController.deleteMetric()` + `HealthMetricServiceImpl.deleteMetric()` (full chain)
**Test File:** `src/test/java/com/carebridge/backend/health/HealthMetricControllerDeleteTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Authenticated as `OTHER_MOTHER_ID` (role `MOTHER`, passes controller role gate) targeting `METRIC_ID` owned (via journey) by `OWNER_ID`.

**Test Steps (Attack Simulation):**
1. Authenticate as `OTHER_MOTHER_ID`.
2. `DELETE /api/v1/health-metrics/{METRIC_ID}`.
3. Assert response is `403 METRIC-003` (service-layer ownership gate catches what the role gate could not).
4. Assert target metric's DB/mock state unchanged (`status` still `ACTIVE`, no `save()` invoked, no event published).

**Expected Result (PASS = safe):** `403`, no state mutation, no `MaternalHealthMetricDeleted` event (cross-check with TC-008).
**Expected Result (FAIL = vulnerability):** `204` returned, or the other Mother's metric is deleted.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### METRIC-DEL-TC-INT-001 — Full stack: owner DELETEs own metric, row preserved with status=DELETED

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `DELETE /api/v1/health-metrics/{id}` → `HealthMetricController` → `HealthMetricServiceImpl` → `MaternalHealthMetricRepository`/`MotherJourneyRepository` → PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/health/integration/HealthMetricDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** PostgreSQL Testcontainer running; Flyway migrated (includes `V20260627100200`); seed one `mother_journeys` row (`owner_user_id=OWNER_ID`) and one `maternal_health_metrics` row (`status=ACTIVE`, `journey_id` → seeded journey).

**Test Steps:**
1. Seed journey + metric via JPA.
2. `mockMvc.perform(delete("/api/v1/health-metrics/{id}", metricId).header("Authorization", ownerJwt))`.
3. Assert status 204.

**DB Assertion:**
```java
MaternalHealthMetric record = metricRepository.findById(metricId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(MetricStatus.DELETED); // transitioned, row preserved
assertThat(record.getMetricType()).isNotNull(); // physical row not hard-deleted (ADR-HEALTH-004)
```

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-INT-002 — Full stack: DELETE then GET → 404 (UC187 read path excludes DELETED)

**Severity:** `HIGH`
**Feature Under Test:** Full flow cross-check between `deleteMetric()` and existing `getMetricDetail()`
**Test File:** `src/test/java/com/carebridge/backend/health/integration/HealthMetricDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** TDS §11.3 Chặng 3 Verification sau deploy

**Test Steps:**
1. Seed journey + active metric.
2. `DELETE /api/v1/health-metrics/{id}` as owner → assert 204.
3. `GET /api/v1/health-metrics/{id}` as owner → assert 404 `METRIC-001`.

**Expected Result (PASS):** GET after DELETE consistently returns 404, proving the shared `findByIdAndStatus(id, ACTIVE)` guard is honored by both UC187 and UC188.

**Current Status:** 🔴 Not written

---

### METRIC-DEL-TC-INT-003 — Full stack: non-owner → 403, DB state unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, ownership denial path
**Test File:** `src/test/java/com/carebridge/backend/health/integration/HealthMetricDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Test Steps:**
1. Seed journey owned by `OWNER_ID` + active metric under it.
2. `DELETE /api/v1/health-metrics/{id}` authenticated as a different seeded `MOTHER`-role user.
3. Assert 403, error code `METRIC-003`.
4. DB assertion: `status` still `ACTIVE`.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|--------------|----------------------|------------------------|------------------------|
| `METRIC-DEL-TC-001` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-002` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-003` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-004` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-005` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-006` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-007` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-008` | `HealthMetricServiceDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-009` | `HealthMetricControllerDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-010` | `HealthMetricControllerDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-SEC-001` | `HealthMetricControllerDeleteTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-INT-001` | `HealthMetricDeleteIntegrationTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-INT-002` | `HealthMetricDeleteIntegrationTest.java` | `[ ]` | `[ ]` | |
| `METRIC-DEL-TC-INT-003` | `HealthMetricDeleteIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> **Scope note:** `HealthMetricServiceImpl.getMetricDetail()` and `IHealthMetricService.getMetricDetail()` are **pre-existing, shipped code from UC187** (confirmed by reading the real files — the interface currently declares only `getMetricDetail()`; no `deleteMetric()` exists anywhere in `com.carebridge.backend.health` yet, and no `HealthMetricController` file exists at all in the current codebase). The Red Gate below is scoped **exclusively to the new `deleteMetric()` method and its new `HealthMetricController`** — `getMetricDetail()` is NOT re-stubbed and its existing passing tests (if any) must remain green throughout.

**Stub cho Red Phase:**

```java
// IHealthMetricService.java — add new method signature (does not touch getMetricDetail())
public interface IHealthMetricService {
    MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId); // unchanged — UC187

    void deleteMetric(UUID metricId, UUID callerId); // NEW — UC188
}

// HealthMetricServiceImpl.java — add stub, getMetricDetail() body UNCHANGED
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements IHealthMetricService {
    // ... existing getMetricDetail() unchanged ...

    @Override
    @Transactional
    public void deleteMetric(UUID metricId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// HealthMetricController.java — NEW file (does not exist yet); only the delete endpoint is stubbed
// since this is the first controller file for this module.
@RestController
@RequestMapping("/api/v1/health-metrics")
@RequiredArgsConstructor
public class HealthMetricController {

    private final IHealthMetricService healthMetricService;

    @DeleteMapping("/{metricId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> deleteMetric(@PathVariable UUID metricId, Principal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-----------------|--------------|-------------|-----------------------------------------|
| `METRIC-DEL-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `METRIC-DEL-TC-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `METRIC-DEL-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `METRIC-DEL-TC-INT-001` | `throw` (via controller 500) | 🔴 FAIL | ☐ FAIL ☐ PASS | |

> Note: `HealthMetricController` tests fail at **compile time** initially since the class does not exist yet — this is an acceptable/expected Red Gate signal, to be confirmed once the stub above is added.

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled during implementation)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3)
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-HEALTH-IMP-004` reviewed and Approved
- [ ] ADR-HEALTH-004, ADR-HEALTH-005 confirmed Accepted (TDS §3)
- [ ] Logic Issues (§2) confirmed with Tech Lead
- [ ] No migration required for UC-188 (confirmed TDS §5.2) — no migration gate blocking
- [ ] Confirmed `getMetricDetail()` (UC187) tests remain untouched/green — this Test-Spec scopes ONLY `deleteMetric()`

### Exit Criteria
- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [ ] Test coverage ≥ 80% lines for new `deleteMetric()` code and new `HealthMetricController`
- [ ] No business logic in `HealthMetricController` (validation/mapping only)
- [ ] No PII in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — all new tests FAIL against throwing stub before implement
- [ ] Contract Existence — `./mvnw compile` clean, no hallucinated imports
- [ ] Props Isolation — all entities built via `HealthMetricDeleteTestFactory`, no shared mutable state
- [ ] Oracle Source — every assert traces to ADR-HEALTH-004/005 or existing schema fact
- [ ] Negative-mutation checks present: `delete()`/`deleteById()` never invoked; DB `status` unchanged on every denial path (403/404); no audit event on denial

### Suspension Criteria
- Tech Lead disagrees with ADR-HEALTH-005's idempotent-404 decision (would require TDS revision first)
- DPO sign-off (pending per TDS header) blocks production rollout — does not block test-writing/implementation itself

---

## 7. Rollback Plan

```bash
# No migration to revert for UC-188.
git checkout -- src/main/java/com/carebridge/backend/health/
git checkout -- src/test/java/com/carebridge/backend/health/
# NOTE: do NOT blanket-revert getMetricDetail() (UC187) if it was already merged independently —
# revert only the deleteMetric()/HealthMetricController additions specific to UC188.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu | Check | Gate chặn |
|-------|--------------|--------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-HEALTH-004/005 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throwing stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes `owner_user_id` exists directly on `maternal_health_metrics` (not in schema) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `HealthMetricController` doing ownership/soft-delete logic directly | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports repository method not declared in TDS §8.2, or asserts `metricRepository.deleteById()` IS called | ☐ | G-3 |
| AP-AI-006 (custom) | Idempotency-leak | Test asserts a distinct "already deleted" message/code differing from `METRIC-001` (violates ADR-HEALTH-005) | ☐ | G-1 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → Test-Spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec for UC188 Delete Maternal Health Metric — Status: Draft. Awaiting review before Approved.*
