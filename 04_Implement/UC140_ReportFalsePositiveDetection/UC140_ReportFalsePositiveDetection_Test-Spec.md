# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# UC140 — Report False Positive Detection

**Document ID:** `FPT-EDU-TDD-UC140-001`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000007__create_safety_events.sql` — base schema source (`imu_safety_events`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260705100000__add_false_positive_columns_to_imu_safety_events.sql` — NEW migration for this UC (see TDS §5.2)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.4.8`
- `04_Implement/UC140_ReportFalsePositiveDetection/UC140_ReportFalsePositiveDetection_TDS.md` (`CB-SAFETY-IMP-006`)
- `04_Implement/UC139_ViewSafetyEventHistory/UC139_ViewSafetyEventHistory_TDS.md` (shared ownership pattern)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent — Tech Lead` | Khởi tạo tài liệu — TDD spec cho UC140 Report False Positive Detection (Draft) |

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
| **Feature / Gap ID** | `UC140-FALSE-POSITIVE` |
| **Module** | `Report False Positive Detection — safety bounded context` |
| **Spec gốc** | `CB-SAFETY-IMP-006` |
| **Priority** | 🟡 P2 (Medium, per SRS) |
| **Sprint** | `S2 (per function-spec-task-allocation.md, TV5-Chương)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `imu_safety_events` (UC136), new migration `V20260705100000` |
| **Downstream Consumers** | UC139 read model; `FalsePositiveReported` event (no active subscriber yet, §18 RG-4) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC140_ReportFalsePositiveDetection_TDS.md §17`, `ADR-SAFETY-009`, `ADR-SAFETY-010` |
| **Constraints Injected** | Column-level mutation scope (C1), JWT-derived ownership (C2), status CHECK constraint (C3), no ML pipeline assumption (C4), optional reason (C5), idempotent overwrite (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC136 TDS established `imu_safety_events` as strictly append-only with table-level `REVOKE UPDATE, DELETE FROM PUBLIC`, no exceptions documented | UC140 genuinely requires mutation; original REVOKE remains at table level, but a NEW column-level GRANT (3 columns only) is added by `V20260705100000` | Tests must assert the GRANT is column-scoped, not table-scoped — a test asserting broad UPDATE capability on e.g. `event_type` must FAIL |
| L2 | SRS says "to improve rules" implying an ML/rule-tuning consumer exists | No such consumer exists anywhere in the codebase (confirmed via search — no `ai`/`triage`/`ml` package subscribes to safety events) | Tests only assert `FalsePositiveReported` event is published; do NOT assert any downstream rule-tuning side effect (would be a hallucinated contract, AP-AI-005) |
| L3 | Generic template implies mutation could target any field | Only `status`, `false_positive_reason`, `false_positive_reported_at` should ever change; `event_type`, `magnitude`, `detected_at`, `user_latitude`, `user_longitude` must remain byte-identical | Every happy-path test asserts forensic fields are unchanged before/after the PATCH |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Report False Positive Detection bao gồm các layer:
├── Domain (SafetyEventStatus enum, entity field mapping — pure logic)
├── Repository (ISafetyEventRepository.findByIdAndUserId, save — mock JPA với Mockito for unit; Testcontainers for integration)
├── Service (FalsePositiveReportService — mock Repository + EventPublisher với Mockito)
├── Controller (FalsePositiveReportController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest — verifies actual GRANT/CHECK constraint behavior)
└── Mobile (flutter_test widget tests for the false-positive report dialog/action)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-140` | Happy path labeling, optional reason, E1 access denial |
| `ADR-SAFETY-009` | Column-level mutation scope, immutable forensic fields |
| `ADR-SAFETY-010` | Ownership scoping (404 not 403), status CHECK constraint |
| `BR-RBAC` | Only `ROLE_MOTHER` can call this endpoint |
| `BR-SAFETY` | No diagnostic language leak in reason echo/response |
| `CB-SAFETY-IMP-006 §5.2, §8, §9, §10` | Migration DDL, DTO shape, endpoint contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother labels own UNREVIEWED event with a reason | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-001` |
| TC-COND-002 | Mother labels own event with no reason (optional field) | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-002` |
| TC-COND-003 | Forensic fields unchanged after labeling | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-003` |
| TC-COND-004 | Re-labeling an already-FALSE_POSITIVE event overwrites reason | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-004` |
| TC-COND-005 | reason exceeds 500 chars | `FalsePositiveReportController` validation | `FP-TC-005` |
| TC-COND-006 | User B attempts to label User A's event (IDOR) | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-006` |
| TC-COND-007 | Non-existent eventId | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-007` |
| TC-COND-008 | Unauthenticated request | `FalsePositiveReportController` security filter chain | `FP-TC-008` |
| TC-COND-009 | Non-MOTHER role (ROLE_EXPERT) | `FalsePositiveReportController` `@PreAuthorize` | `FP-TC-009` |
| TC-COND-010 | `FalsePositiveReported` event published exactly once | `FalsePositiveReportService.reportFalsePositive()` | `FP-TC-010` |
| TC-COND-011 | Column-level GRANT prevents mutation of `event_type`/`magnitude` via raw SQL as app role | DB-level constraint (Testcontainers) | `FP-TC-SEC-001` |
| TC-COND-012 | `status` CHECK constraint rejects invalid value | DB-level constraint (Testcontainers) | `FP-TC-SEC-002` |
| TC-COND-013 | Full flow via Testcontainers | End-to-end DB → API | `FP-TC-INT-001` |
| TC-COND-014 | Mobile confirmation dialog submits reason and shows success state | `SafetyEventDetailScreen` false-positive action | `FP-TC-MOB-001` |
| TC-COND-015 | Mobile dialog allows skipping the optional reason field | `SafetyEventDetailScreen` false-positive action | `FP-TC-MOB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `reason` (empty/null, valid, too-long) | Confirm optional-field and max-length validation partitions |
| Boundary Value Analysis | `reason` length (0, 500, 501 chars) | Confirm exact 500-char boundary enforced |
| State Transition Testing | `status` (UNREVIEWED → FALSE_POSITIVE → FALSE_POSITIVE) | Verify state machine per TDS §6.3, no invalid transitions possible |
| Error Guessing | IDOR via eventId enumeration, raw SQL privilege escalation attempt, missing JWT | Security-focused coverage for the mutation exception carved out by ADR-SAFETY-009 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `imu_safety_events` row: `{userId: MOTHER_A, status: 'UNREVIEWED', eventType: 'SUSPECTED_FALL', magnitude: 14.2, detectedAt: now-1h}` | Happy path base record |
| `FX-002` | DB seed | Same as FX-001 but `status: 'FALSE_POSITIVE'`, `falsePositiveReason: 'old reason'` | Re-labeling / idempotent overwrite test |
| `FX-003` | DB seed | `imu_safety_events` row owned by `MOTHER_B` | IDOR cross-user guard |
| `FX-004` | JWT | `{sub: MOTHER_A_uuid, role: 'MOTHER'}` | Auth context for owner |
| `FX-005` | JWT | `{sub: MOTHER_B_uuid, role: 'MOTHER'}` | Auth context for non-owner (IDOR attacker) |
| `FX-006` | JWT | `{sub: EXPERT_uuid, role: 'EXPERT'}` | Wrong-role rejection test |
| `FX-007` | String | 501-character string | Boundary test for max reason length |
| `FX-008` | env | Testcontainers PostgreSQL with `V20260705100000` applied | Integration/security DB tests |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// FalsePositiveTestFactory.java
class FalsePositiveTestFactory {

    static final UUID MOTHER_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID MOTHER_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");

    static SafetyEvent makeUnreviewedEvent() {
        return makeUnreviewedEvent(e -> {});
    }

    static SafetyEvent makeUnreviewedEvent(Consumer<SafetyEvent> overrides) {
        SafetyEvent event = SafetyEvent.builder()
                .id(UUID.randomUUID())
                .userId(MOTHER_A_ID)
                .imuSessionId(UUID.randomUUID())
                .eventType(SafetyEventType.SUSPECTED_FALL)
                .magnitude(new BigDecimal("14.2000"))
                .detectedAt(Instant.now().minusSeconds(3600))
                .status(SafetyEventStatus.UNREVIEWED)
                .createdBy("SYSTEM")
                .build();
        overrides.accept(event);
        return event;
    }

    static SafetyEvent makeAlreadyLabeledEvent() {
        return makeUnreviewedEvent(e -> {
            e.setStatus(SafetyEventStatus.FALSE_POSITIVE);
            e.setFalsePositiveReason("old reason");
            e.setFalsePositiveReportedAt(Instant.now().minusSeconds(1800));
        });
    }

    static String makeOverLengthReason() {
        return "a".repeat(501);
    }
}
```

---

### FP-TC-001 — Report false positive with reason — happy path

**Severity:** `HIGH`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS-3.3.4.8 Normal Flow / CB-SAFETY-IMP-006 §6.1`

**Preconditions:**
- FX-001 seeded (`status=UNREVIEWED`, owned by `MOTHER_A_ID`)

**Test Steps:**
1. Mock `safetyEventRepository.findByIdAndUserId(FX-001.id, MOTHER_A_ID)` returns FX-001
2. Call `service.reportFalsePositive(MOTHER_A_ID, FX-001.id, "Was just walking briskly")`
3. Assert `repository.save()` called with `status=FALSE_POSITIVE`, `falsePositiveReason` set, `falsePositiveReportedAt` non-null
4. Assert returned `FalsePositiveReportResponse` reflects the same

**Expected Result (PASS):**
- `status == "FALSE_POSITIVE"`, `falsePositiveReason == "Was just walking briskly"`

**Expected Result (FAIL):**
- Wrong status, missing reason, or exception thrown

**Current Status:** 🔴 Not written
**Implementation Note:** Reuses `findByIdAndUserId` already added for UC139.

---

### FP-TC-002 — Report false positive without reason (optional field)

**Severity:** `MEDIUM`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `SRS-3.3.4.8 ("optional reason")`

**Preconditions:** FX-001 seeded

**Test Steps:**
1. Call `service.reportFalsePositive(MOTHER_A_ID, FX-001.id, null)`
2. Assert no exception thrown
3. Assert `status=FALSE_POSITIVE`, `falsePositiveReason=null`

**Expected Result (PASS):** Label succeeds with null reason
**Expected Result (FAIL):** `NullPointerException` or validation rejects null

**Current Status:** 🔴 Not written

---

### FP-TC-003 — Forensic fields unchanged after labeling

**Severity:** `CRITICAL`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-009 (immutable core fields invariant)`

**Preconditions:** FX-001 seeded with known `eventType=SUSPECTED_FALL`, `magnitude=14.2`, `detectedAt=T0`, `userLatitude=10.76`, `userLongitude=106.66`

**Test Steps:**
1. Capture FX-001's `eventType`, `magnitude`, `detectedAt`, `userLatitude`, `userLongitude`, `notes`, `createdBy` before the call
2. Call `service.reportFalsePositive(MOTHER_A_ID, FX-001.id, "reason")`
3. Capture the same fields on the saved entity after the call
4. Assert all captured "before" values equal "after" values

**Expected Result (PASS):** All forensic fields byte-identical before/after
**Expected Result (FAIL):** Any forensic field mutated by the service (regression of ADR-SAFETY-006/009 invariant)

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary regression guard for AP-AI-004 (§17.4 TDS).

---

### FP-TC-004 — Re-labeling overwrites reason idempotently, no duplicate row

**Severity:** `MEDIUM`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SAFETY-009 (MVP overwrite trade-off)`

**Preconditions:** FX-002 seeded (`status=FALSE_POSITIVE`, `reason="old reason"`)

**Test Steps:**
1. Call `service.reportFalsePositive(MOTHER_A_ID, FX-002.id, "new reason")`
2. Assert `repository.save()` called once (not insert of a new row — same `id`)
3. Assert returned response has `falsePositiveReason == "new reason"`

**Expected Result (PASS):** Reason overwritten, same event `id`, single row
**Expected Result (FAIL):** New row created, or old reason retained, or exception thrown ("already labeled")

**Current Status:** 🔴 Not written

---

### FP-TC-005 — reason exceeding 500 characters rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `FalsePositiveReportController` request validation
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/FalsePositiveReportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-SAFETY-IMP-006 §8.1 (@Size max=500)`

**Preconditions:** FX-007 (501-char string), valid FX-004 JWT

**Test Steps:**
1. `PATCH /api/v1/safety/events/{FX-001.id}/false-positive` with `{"reason": "<501 chars>"}`
2. Assert response status and error code

**Expected Result (PASS):** `400 Bad Request`, code `SAFETY-011`
**Expected Result (FAIL):** Request accepted, oversized value persisted

**Current Status:** 🔴 Not written

---

### FP-TC-006 — IDOR guard: labeling another user's event returns 404, mutates nothing (SECURITY)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — unauthorized mutation of another user's health-safety record`
**Feature Under Test:** `FalsePositiveReportController` / `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/FalsePositiveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SAFETY-010`

**Preconditions:**
- FX-001 seeded (owned by `MOTHER_A_ID`, `status=UNREVIEWED`)
- FX-005 JWT (`MOTHER_B_ID`)

**Test Steps (Attack Simulation):**
1. Authenticate as `MOTHER_B_ID`
2. `PATCH /api/v1/safety/events/{FX-001.id}/false-positive` with `{"reason": "not mine"}`
3. Assert response status and body
4. Re-fetch FX-001 directly from repository, assert `status` is still `UNREVIEWED`

**Expected Result (PASS = hệ thống an toàn):**
- `404 Not Found`, code `SAFETY-009`; FX-001's `status` unchanged in DB

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `200 OK` with FX-001 mutated by `MOTHER_B_ID`, or `403 Forbidden` (confirms existence)

**Current Status:** 🔴 Not written

---

### FP-TC-007 — Non-existent eventId returns 404

**Severity:** `MEDIUM`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-SAFETY-IMP-006 §10`

**Preconditions:** Random UUID not present in DB

**Test Steps:**
1. Mock `repository.findByIdAndUserId(randomId, MOTHER_A_ID)` returns `Optional.empty()`
2. Call `service.reportFalsePositive(MOTHER_A_ID, randomId, "reason")`
3. Assert `SafetyException` thrown with code `SAFETY-009`, HTTP 404

**Expected Result (PASS):** Exception with correct code/status
**Expected Result (FAIL):** NPE or wrong error code

**Current Status:** 🔴 Not written

---

### FP-TC-008 — Unauthenticated request rejected

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `FalsePositiveReportController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/FalsePositiveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** No JWT header

**Test Steps (Attack Simulation):**
1. `PATCH /api/v1/safety/events/{anyId}/false-positive` with no `Authorization` header
2. Assert response status

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` or `500`

**Current Status:** 🔴 Not written

---

### FP-TC-009 — Wrong role (EXPERT) rejected with 403

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `FalsePositiveReportController` `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/FalsePositiveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-SAFETY-IMP-006 §16 Authorization Matrix`

**Preconditions:** FX-006 JWT (`ROLE_EXPERT`)

**Test Steps:**
1. `PATCH /api/v1/safety/events/{FX-001.id}/false-positive` with FX-006 JWT
2. Assert response status

**Expected Result (PASS):** `403 Forbidden`, code `SAFETY-004`
**Expected Result (FAIL):** `200 OK`

**Current Status:** 🔴 Not written

---

### FP-TC-010 — FalsePositiveReported event published exactly once, no downstream side effect assumed

**Severity:** `MEDIUM`
**Feature Under Test:** `FalsePositiveReportService.reportFalsePositive()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/FalsePositiveReportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-SAFETY-IMP-006 §7.1 / §18 RG-4`

**Preconditions:** FX-001 seeded, mocked `ApplicationEventPublisher`

**Test Steps:**
1. Call `service.reportFalsePositive(MOTHER_A_ID, FX-001.id, "reason")`
2. Verify `eventPublisher.publishEvent(any(FalsePositiveReported.class))` called exactly 1 time
3. Assert payload contains correct `safetyEventId`, `userId`, `reason`

**Expected Result (PASS):** Event published once with correct payload; test does NOT assert any ML/rule-tuning service was called (none exists)
**Expected Result (FAIL):** Event not published, published multiple times, or wrong payload

**Current Status:** 🔴 Not written
**Implementation Note:** Do not add a mocked `RuleTuningService`/`MLFeedbackService` to this test — that would be a hallucinated contract (AP-AI-005, RG-4 still Open for any real consumer).

---

### SECURITY TEST CASES (DB-level, Testcontainers)

---

### FP-TC-SEC-001 — Column-level GRANT prevents app role from mutating forensic columns

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-732 — Incorrect Permission Assignment for Critical Resource`
**Legal:** `PDPA — integrity of health-safety audit record`
**Feature Under Test:** `imu_safety_events` column-level GRANT (migration `V20260705100000`)
**Test File:** `src/test/java/com/carebridge/backend/safety/FalsePositiveMigrationSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Testcontainers PostgreSQL with all migrations applied including `V20260705100000`
- Connected as the `carebridge_app` role (not superuser)

**Test Steps (Attack Simulation):**
1. Seed one `imu_safety_events` row
2. Attempt raw SQL: `UPDATE imu_safety_events SET magnitude = 999.0 WHERE id = :id` using the app DB role
3. Attempt raw SQL: `UPDATE imu_safety_events SET status = 'FALSE_POSITIVE' WHERE id = :id` using the app DB role

**Expected Result (PASS = hệ thống an toàn):**
- Step 2 raises a PostgreSQL permission-denied error (`42501`)
- Step 3 succeeds (column is granted)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Step 2 succeeds — GRANT was applied too broadly (table-level instead of column-level)

**Current Status:** 🔴 Not written

---

### FP-TC-SEC-002 — status CHECK constraint rejects invalid value

**Severity:** `HIGH`
**CWE:** `CWE-1287 — Improper Validation of Specified Type of Input`
**Feature Under Test:** `chk_safety_event_status` CHECK constraint (migration `V20260705100000`)
**Test File:** `src/test/java/com/carebridge/backend/safety/FalsePositiveMigrationSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Testcontainers PostgreSQL with `V20260705100000` applied

**Test Steps:**
1. Attempt raw SQL `UPDATE imu_safety_events SET status = 'BOGUS_VALUE' WHERE id = :id`
2. Assert a constraint-violation exception is raised

**Expected Result (PASS):** DB rejects with CHECK constraint violation
**Expected Result (FAIL):** Invalid value silently accepted

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FP-TC-INT-001 — Full flow: seed UNREVIEWED event → PATCH false-positive → verify persisted + forensic fields intact

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: imu_safety_events row -> PATCH /api/v1/safety/events/{id}/false-positive -> DB state`
**Test File:** `src/test/java/com/carebridge/backend/safety/FalsePositiveReportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer running, all migrations applied including `V20260705100000`
- Seed 1 `imu_safety_events` row for `MOTHER_A_ID` via `ISafetyEventRepository.save()`, `status=UNREVIEWED`

**Test Steps:**
1. `PATCH /api/v1/safety/events/{eventId}/false-positive` as `MOTHER_A_ID` with `{"reason": "false alarm, was exercising"}`
2. Assert response 200, `status=FALSE_POSITIVE`
3. Re-fetch the row directly via repository
4. Assert `status=FALSE_POSITIVE`, `falsePositiveReason` matches, `falsePositiveReportedAt` is set
5. Assert `eventType`, `magnitude`, `detectedAt` are unchanged from the original seed values

**Expected Result (PASS):**
- All assertions in steps 2-5 hold

**Expected Result (FAIL):**
- Any forensic field changed, or status not persisted

**DB Assertion:**
```java
SafetyEvent updated = safetyEventRepository.findById(eventId).orElseThrow();
assertThat(updated.getStatus()).isEqualTo(SafetyEventStatus.FALSE_POSITIVE);
assertThat(updated.getFalsePositiveReason()).isEqualTo("false alarm, was exercising");
assertThat(updated.getEventType()).isEqualTo(originalEventType); // unchanged
assertThat(updated.getMagnitude()).isEqualByComparingTo(originalMagnitude); // unchanged
```

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES (Flutter)

---

### FP-TC-MOB-001 — Confirmation dialog submits reason and shows success state

**Severity:** `MEDIUM`
**Feature Under Test:** False-positive report action on `SafetyEventDetailScreen` (mobile)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/screens/safety_event_detail_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Mocked repository returns success response for `reportFalsePositive()`

**Test Steps:**
1. Pump `SafetyEventDetailScreen` for an `UNREVIEWED` event
2. Tap "Report false positive" action
3. Enter a reason in the dialog text field, tap "Submit"
4. Assert repository's `reportFalsePositive()` called with the entered reason
5. Assert UI shows a success confirmation (e.g. snackbar/badge updates to "False positive")

**Expected Result (PASS):** Dialog flow completes, UI reflects new status
**Expected Result (FAIL):** Dialog does not submit, or UI does not update

**Current Status:** 🔴 Not written

---

### FP-TC-MOB-002 — Dialog allows skipping the optional reason field

**Severity:** `LOW`
**Feature Under Test:** False-positive report dialog validation (mobile)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/screens/safety_event_detail_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:** Mocked repository returns success response

**Test Steps:**
1. Pump `SafetyEventDetailScreen`, open false-positive dialog
2. Leave reason field empty, tap "Submit"
3. Assert repository's `reportFalsePositive()` called with `null`/empty reason (not blocked by client-side "required" validation)

**Expected Result (PASS):** Submission succeeds without a reason
**Expected Result (FAIL):** Client blocks submission expecting a required field (contradicts SRS "optional reason")

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FP-TC-001` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-002` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-003` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-004` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-005` | `FalsePositiveReportControllerTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-006` | `FalsePositiveReportControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-007` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-008` | `FalsePositiveReportControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-009` | `FalsePositiveReportControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-010` | `FalsePositiveReportServiceTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-SEC-001` | `FalsePositiveMigrationSecurityTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-SEC-002` | `FalsePositiveMigrationSecurityTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-INT-001` | `FalsePositiveReportIntegrationTest.java` | `[ ]` | `[ ]` | |
| `FP-TC-MOB-001` | `safety_event_detail_screen_test.dart` | `[ ]` | `[ ]` | |
| `FP-TC-MOB-002` | `safety_event_detail_screen_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class FalsePositiveReportService implements IFalsePositiveReportService {

    @Override
    public FalsePositiveReportResponse reportFalsePositive(UUID userId, UUID eventId, String reason) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FP-TC-SEC-001` | N/A — DB-level test, independent of service stub | 🔴 FAIL until migration applied | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (pending implementation phase)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-SAFETY-IMP-006` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] Migration `V20260705100000` đã được review (column-level GRANT scope verified) và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration + security tests xanh (Testcontainers)
- [ ] `flutter test` — mobile widget tests xanh
- [ ] Test coverage ≥ 80% lines cho `FalsePositiveReportService`
- [ ] Không có business logic trong Controller
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `FP-TC-003` (forensic field immutability) và `FP-TC-SEC-001` (column-level GRANT) PASS trước khi merge — đây là 2 gate quan trọng nhất của UC140
- [ ] `FP-TC-006` (IDOR guard) PASS trước khi merge

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi
- [ ] **Props Isolation** — factory pattern dùng nhất quán
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn

### Suspension Criteria (Điều kiện tạm dừng)

- Migration `V20260705100000` conflicts with a sibling UC's migration in the same timestamp range (verify no other agent claimed `V20260705100000` before applying)
- DPO sign-off not obtained for the append-only exception (ADR-SAFETY-009)

---

## 7. Rollback Plan

```bash
# Revert migration (dev only — KHÔNG chạy trên production without DPO approval)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "REVOKE UPDATE (status, false_positive_reason, false_positive_reported_at) ON imu_safety_events FROM carebridge_app;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE imu_safety_events DROP COLUMN IF EXISTS status, DROP COLUMN IF EXISTS false_positive_reason, DROP COLUMN IF EXISTS false_positive_reported_at;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705100000';"

# Revert implementation files
git checkout -- src/main/resources/db/migration/V20260705100000__add_false_positive_columns_to_imu_safety_events.sql
git checkout -- src/main/java/com/carebridge/backend/safety/controller/FalsePositiveReportController.java
git checkout -- src/main/java/com/carebridge/backend/safety/service/FalsePositiveReportService.java
git checkout -- src/test/java/com/carebridge/backend/safety/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/safetyMonitoring/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/

# Gap vẫn OPEN → giữ nguyên entry trong TDS §18 Open Items
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (tất cả TC reference §17 TDS constraints) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(pending Red Gate run during implementation)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (e.g. table-level GRANT) | ☑ (FP-TC-SEC-001 explicitly asserts column-level, not table-level, per ADR-SAFETY-009) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check HTTP status/validation, not mutation logic) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import/assume ML/rule-tuning service that doesn't exist | ☑ (FP-TC-010 explicitly notes NOT asserting any such call — RG-4 remains Open for real consumer) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở mức spec (Draft) — TDD spec pending human approval
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
