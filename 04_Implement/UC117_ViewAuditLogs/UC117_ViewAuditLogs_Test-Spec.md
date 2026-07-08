# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC117 — View Audit Logs — Test Specification

**Document ID:** `CB-AUDIT-TDD-117`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Implemented — 2026-07-04 (approved by user; AuditControllerTest/AuditLogMapperTest/AuditEligibilityPolicyTest all PASS, verified independently)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC117_ViewAuditLogs/UC117_ViewAuditLogs_TDS.md` — companion TDS (this spec implements §6/§8/§9/§10/§16/§17 of it)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — schema source (`audit_logs` L28-42, `audit_logs_action_check` constraint)
- `04_Implement/UC114_ManageUserAccounts/UC114_ManageUserAccounts_Test-Spec.md` — sibling Admin Governance cluster spec (structure/convention reference)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.19 — UC-117 functional requirements
- `CLAUDE.md` — RBAC/audit/least-scope delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code (đối với phần code MỚI — xem §2.1 lưu ý về phần code đã tồn tại).
> Thứ tự bắt buộc cho phần mới: viết test (`.java`/`.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC117 |
| `2026-07-02` | `AI Agent` | Đóng OI-117-5: Product xác nhận MODERATOR không được quyền truy cập scoped — cập nhật Suspension Criteria |

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
| **Feature / Gap ID** | `GAP-UC117` |
| **Module** | `Audit — View Audit Logs (Admin Portal, Admin Governance cluster)` |
| **Spec gốc** | `CB-AUDIT-IMP-117` |
| **Priority** | 🔴 P0 (meta-audit compliance gap, PII bulk-read surface) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Milestone** | Admin Portal governance tabs |
| **Data Classification** | `PII` (`actorUserId`/`userId`, `entityId`, `ipAddress`, free-form `details` diff payloads) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, GDPR Art. 30 |
| **Upstream Dependencies** | `audit.controller.AuditController` (existing), `audit.service.AuditServiceImpl` (existing), `audit.repository.AuditLogRepository` (existing), `audit.policy.AuditEligibilityPolicy` (existing, one-line change), `audit.entity.AuditAction` (existing enum, `VIEW_AUDIT_LOG` value already present) |
| **Downstream Consumers** | None — terminal read surface for compliance/complaint/moderation review |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUDIT-IMP-117 §17` (ADR-AUDIT-001 Accepted, ADR-AUDIT-002 Open) |
| **Constraints Injected** | Backend change strictly limited to `AuditEligibilityPolicy.SENSITIVE_ACTIONS` + one `auditService.log(...)` call in `AuditController.search(...)`; no `AuditServiceImpl`/`AuditLogRepository`/`AuditLog`/`AuditLogMapper` changes; frontend does zero client-side filtering/redaction; meta-audit write must be fail-soft; caller identity via `SecurityUtils.requireCurrentUserId(principal)` only; no `AuditQueryRequest` resurrection; no new Flyway migration |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

### 1.2 Scope Note — Backend read path already exists (carried from TDS §1.1)

This is the key structural difference from a typical new-feature Test-Spec: `AuditController.GET /api/v1/admin/audit-logs`, `AuditServiceImpl.search(...)`, `AuditLogRepository.search(...)`, and `AuditLogMapper` are **already implemented and in production use** (confirmed by TDS §1.1 repository inspection). This Test-Spec therefore splits into two tracks:

- **Track A — Characterization tests for existing, working code** (search/filter correctness, pagination, authorization on the read path). These are **not** Red-Phase in the classical TDD sense — the implementation already exists and (per TDS) already passes manually-verified curl samples (§15 of TDS). These tests are written to *lock down* current behavior and prevent regression, not to drive not-yet-written code. They are still written test-first relative to *this test file* (the test file itself doesn't exist yet), but the RED failure they exhibit before being added is "test file doesn't compile/exist," not "feature doesn't work."
- **Track B — Genuine Red→Green TDD for the NEW meta-audit wiring** (ADR-AUDIT-001: `VIEW_AUDIT_LOG` added to `SENSITIVE_ACTIONS`, controller emits the meta-audit call). This is actually-new code. These tests must fail against an unmodified `AuditEligibilityPolicy`/`AuditController` (genuine Red), and only pass once the ADR-AUDIT-001 wiring is added (genuine Green). This distinction is made explicit per-TC below and again in §5.1 Red Gate Protocol.

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | `AuditAction.VIEW_AUDIT_LOG` enum value exists but `AuditEligibilityPolicy.SENSITIVE_ACTIONS` does not include it, and `AuditController.search(...)` never calls `auditService.log(...)` — the meta-audit gap named in ADR-AUDIT-001 | TDS §3 ADR-AUDIT-001 (Accepted) mandates wiring this exact gap: add to `SENSITIVE_ACTIONS`, add one `log(...)` call after `search()` succeeds, payload = filter params only | `UC117-TC-001` through `UC117-TC-004` (Track B) assert the wiring exists, fires exactly once per call, carries filter-params-only payload, and is fail-soft |
| L2 | Java `AuditAction` enum has 52 values; DB `audit_logs_action_check` CHECK constraint array has only 14 values — a pre-existing drift (`OI-117-3`, TDS §5.2) that would cause `@Enumerated(EnumType.STRING)` insert failures for ~38 enum values | `VIEW_AUDIT_LOG` **is** one of the 14 values present in the constraint array (TDS §5.2 confirms), so ADR-AUDIT-001's insert will succeed — but this is fragile and worth locking down explicitly, not assumed | `UC117-TC-INT-002` explicitly asserts the `VIEW_AUDIT_LOG` insert succeeds against the real DB CHECK constraint (not mocked), so a future accidental narrowing of the constraint array is caught by CI rather than discovered in production |
| L3 | SRS text for UC-117 gives no concrete filter field list (generic AF3 template text only) | TDS §1.1/ADR-AUDIT-002 (Open) confirms the **active** contract is exactly 3 filters: `userId`, `action`, `fromDate`/`toDate` — `AuditQueryRequest` DTO is dead code (TDS §1.2), not part of the contract | `UC117-TC-005`–`TC-007` test only the 3 Accepted filters; no test exercises `entityType`/`entityId`/free-text search (Open Item `OI-117-1`, not implemented) — explicitly not invented |
| L4 | `AuditController` performs no `fromDate > toDate` validation — TDS §10 marks this `OI-117-4` as **Open**, not Accepted, and the current behavior degrades to an empty result set rather than erroring | Per `implement-flow.md`/CASE 2.0, tests must not assert behavior for an undecided requirement | `UC117-TC-008` tests the **current, observed** behavior (empty result, HTTP 200, no error) as a locked-down characterization test, explicitly annotated "Open Item — do not change without a new ADR/decision," rather than asserting a 400 that was never approved |
| L5 | `AuditLogResponse.details` is the raw, unredacted `new_value_json` string (TDS §3 ADR-AUDIT-002, `OI-117-2`, Open — DPO review pending) | No redaction/masking logic exists or is Accepted; TDS's only Accepted interim mitigation is frontend collapsed/expandable rendering (not backend redaction) | `UC117-WEB-TC-003` tests only the Accepted interim mitigation (collapsed/expandable UI, no inline plaintext table cell) — no test asserts field-level redaction, since that logic does not exist and is not decided (CASE 2.0 C2 compliance) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Audit.ViewAuditLogs module bao gồm các layer:
├── Track A — Characterization (existing, already-working code)
│   ├── Repository/Service (AuditServiceImpl.search — already implemented, locked down)
│   └── Controller (AuditController.search — @WebMvcTest, RBAC + filter param binding)
├── Track B — New TDD (ADR-AUDIT-001 meta-audit wiring)
│   ├── Policy (AuditEligibilityPolicy.shouldAudit — unit test)
│   └── Controller (AuditController.search — meta-audit call site, fail-soft wrapper)
├── Integration (real DB — confirms meta-audit row persisted, confirms VIEW_AUDIT_LOG
│                passes the audit_logs_action_check CHECK constraint per L2)
└── Web Frontend (React Testing Library + Vitest — AuditLogFilterBar, AuditLogTable, AuditLogsPage)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-117 §3.2.2.19` | Happy path search/filter, empty state (AF2), generic exception template |
| `ADR-AUDIT-001` (Accepted) | Meta-audit: every `search()` call emits `VIEW_AUDIT_LOG` with filter-params-only payload; fail-soft |
| `ADR-AUDIT-002` (Open) | Frontend filter scope = exactly 3 existing filters; `details` redaction undecided — test only Accepted interim mitigation |
| `BR-RBAC` | Only `SYSTEM_ADMIN` may call `GET /api/v1/admin/audit-logs` |
| `V1__init_schema.sql` | `audit_logs` columns, `audit_logs_action_check` CHECK constraint values (L2) |
| `CB-AUDIT-IMP-117 §6/§8/§9/§10/§16` | Sequence diagrams, service/API contracts, error codes, authorization matrix |
| `OI-117-3, OI-117-4` | Explicitly-Open items — tested as characterization only, never as asserted-correct new behavior |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Track | Test Cases |
|-------------|---------------|---------------|-------|-----------|
| TC-COND-001 | `search()` call emits exactly one `VIEW_AUDIT_LOG` audit entry | `AuditController.search()` meta-audit call | B | `UC117-TC-001` |
| TC-COND-002 | Meta-audit payload contains filter params only, never result rows | `AuditController.search()` payload construction | B | `UC117-TC-002` |
| TC-COND-003 | Meta-audit logging failure does not block the read response (fail-soft) | `AuditController.search()` try/catch wrapper | B | `UC117-TC-003` |
| TC-COND-004 | `AuditEligibilityPolicy.shouldAudit(VIEW_AUDIT_LOG)` returns true | `AuditEligibilityPolicy.SENSITIVE_ACTIONS` | B | `UC117-TC-004` |
| TC-COND-005 | SYSTEM_ADMIN filters by `userId` and receives correct paginated results | `AuditController.search()` / `AuditServiceImpl.search()` | A | `UC117-TC-005` |
| TC-COND-006 | SYSTEM_ADMIN filters by `action` enum value | `AuditController.search()` / `AuditServiceImpl.search()` | A | `UC117-TC-006` |
| TC-COND-007 | SYSTEM_ADMIN filters by `fromDate`/`toDate` range | `AuditController.search()` / `AuditServiceImpl.search()` | A | `UC117-TC-007` |
| TC-COND-008 | `fromDate > toDate` degrades to empty result (current behavior, `OI-117-4` Open) | `AuditController.search()` | A | `UC117-TC-008` |
| TC-COND-009 | Non-SYSTEM_ADMIN role is rejected with 403 `AUDIT-004` | `AuditController` `@PreAuthorize` | A | `UC117-TC-009` |
| TC-COND-010 | Unauthenticated caller is rejected with 401 `IAM-001` | `AuditController` JWT filter chain | A | `UC117-TC-010` |
| TC-COND-011 | Empty result set (no matches) returns 200 with `data:[]`, `totalElements:0` | `AuditController.search()` | A | `UC117-TC-011` |
| TC-COND-012 | Page size request above `AppConstants.MAX_PAGE_SIZE=100` is capped | `AuditController` pagination binding | A | `UC117-TC-012` |
| TC-COND-013 | Large result set pagination — page N returns correct slice, `totalPages` correct | `AuditServiceImpl.search()` | A | `UC117-TC-013` |
| TC-COND-014 | `AuditLogResponse` never exposes fields beyond §8.2 contract (no accidental entity leak) | `AuditLogMapper` | A | `UC117-TC-014` |
| TC-COND-015 | Full search + meta-audit round trip against real DB — meta-audit row persisted with correct actor/action/payload | Full flow | B (integration) | `UC117-TC-INT-001` |
| TC-COND-016 | `VIEW_AUDIT_LOG` insert succeeds against real `audit_logs_action_check` CHECK constraint (L2, `OI-117-3`) | DB constraint | B (integration) | `UC117-TC-INT-002` |
| TC-COND-017 | Web: `AuditLogFilterBar` submits the 3 Accepted filters (userId/action/date range) only | React component | Web | `UC117-WEB-TC-001` |
| TC-COND-018 | Web: `AuditLogTable` renders paginated rows + empty state (AF2) with "Clear filters" action | React component | Web | `UC117-WEB-TC-002` |
| TC-COND-019 | Web: `AuditLogTable` renders `details` in a collapsed/expandable viewer, not inline plaintext (ADR-AUDIT-002 interim mitigation) | React component | Web | `UC117-WEB-TC-003` |
| TC-COND-020 | Web: `AuditLogsPage` renders access-denied UI on 403 | React component | Web | `UC117-WEB-TC-004` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role partitions (SYSTEM_ADMIN vs. every other role); filter-present vs. filter-absent partitions | Binary authorization outcome per §16 matrix; null-safe optional filters per TDS §1.1 |
| Boundary Value Analysis | Page `size` (100/101); `fromDate == toDate` vs. `fromDate > toDate` | TDS §4.1 pagination SLA; `OI-117-4` boundary |
| State Transition Testing | Not applicable — `AuditLog` has no state machine (TDS §6.4); N/A documented, not omitted |
| Error Guessing | Meta-audit write failure mid-request (fail-soft); malformed `action` enum string in query param; oversized filter payload | ADR-AUDIT-001 Consequences (§12.2 fail-soft mandate); `AUDIT-001` error trigger |
| Characterization Testing | Track A — all existing `AuditController.search()` behavior (search/filter/pagination/auth) | Locks down already-working, already-manually-verified (TDS §15) production behavior against regression |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-117-001` | JWT | `{ sub: admin-user-id, role: 'SYSTEM_ADMIN' }` | Auth context for admin requests |
| `FX-117-002` | JWT | `{ sub: mother-user-id, role: 'MOTHER' }` (also parameterized: `FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, PARTNER`) | Non-admin rejection |
| `FX-117-003` | DB seed | 10 `AuditLog` rows spanning distinct `action` values, `actorUserId`, and a 30-day `createdAt` spread | Search/filter/pagination coverage |
| `FX-117-004` | DB seed | 1 `AuditLog` row with `action='VIEW_AUDIT_LOG'` pre-seeded to prove the CHECK constraint accepts it independent of the app-level insert path (control case for `UC117-TC-INT-002`) | DB constraint sanity |
| `FX-117-005` | Request | `GET ...?fromDate=2026-08-01T00:00:00Z&toDate=2026-01-01T00:00:00Z` (`fromDate` after `toDate`) | `OI-117-4` characterization |
| `FX-117-006` | Request | `GET ...?page=0&size=101` | `MAX_PAGE_SIZE` boundary |
| `FX-117-007` | Mock | `AuditService.log(...)` mocked to throw `RuntimeException("simulated audit write failure")` | Fail-soft verification (`UC117-TC-003`) |
| `FX-117-008` | Web mock | Fetch mock: `GET /api/v1/admin/audit-logs` happy-path fixture matching TDS §9.2 sample | Web component rendering |
| `FX-117-009` | Web mock | Fetch mock: 403 `AUDIT-004` error body | Web access-denied rendering |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> New factory for the Audit module — `AuditTestFactory`. Does not reuse `AdminGovernanceTestFactory` (UC114/115/116) since `AuditLog`/`AuditAction` are distinct aggregates from `User`; kept separate per bounded-context boundary (TDS §1 `Bounded Context: Audit / Admin Governance`, sibling not merged context).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// AuditTestFactory.java — shared factory for UC117 audit-log tests
// mỗi @Test dùng factory method, không shared state
// ═══════════════════════════════════════════════════════════
class AuditTestFactory {

    static AuditLog makeAuditLog(Consumer<AuditLog> overrides) {
        AuditLog log = new AuditLog();
        log.setAuditLogId(UUID.randomUUID());
        log.setCreatedAt(Instant.now());
        log.setActorUserId(UUID.randomUUID());
        log.setAction(AuditAction.MODERATION_ACTION);
        log.setEntityType("CommunityAnswer");
        log.setEntityId(UUID.randomUUID());
        log.setNewValueJson("{\"decision\":\"REMOVED\"}");
        log.setOldValueJson(null);
        log.setIpAddress("203.0.113.10");
        overrides.accept(log);
        return log;
    }

    static AuditLog makeViewAuditLogEntry(UUID actorId, String filterSnapshotJson) {
        return makeAuditLog(log -> {
            log.setActorUserId(actorId);
            log.setAction(AuditAction.VIEW_AUDIT_LOG);
            log.setEntityType("AuditLog");
            log.setEntityId(null);
            log.setNewValueJson(filterSnapshotJson);
        });
    }

    static List<AuditLog> makeAuditLogBatch(int count, Consumer<AuditLog> commonOverrides) {
        List<AuditLog> logs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logs.add(makeAuditLog(commonOverrides));
        }
        return logs;
    }

    // UC117-specific: search query params bundle
    static AuditSearchParams makeSearchParams(Consumer<AuditSearchParams> overrides) {
        AuditSearchParams p = new AuditSearchParams();
        overrides.accept(p);
        return p;
    }
}
```

---

### TRACK B — NEW: ADR-AUDIT-001 Meta-Audit Wiring (genuine Red → Green)

---

### UC117-TC-001 — `search()` call emits exactly one VIEW_AUDIT_LOG audit entry

**Severity:** `CRITICAL`
**Feature Under Test:** `AuditController.search()` (new meta-audit call site)
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/AuditControllerTest.java`
**TDD Phase:** 🔴 RED — genuinely not implemented (Track B)
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-AUDIT-IMP-117 ADR-AUDIT-001 Decision`, `§6.1 Sequence Diagram — Happy Path`

**Preconditions:** `AuditService` mocked (`@MockBean`); FX-117-001 (SYSTEM_ADMIN JWT).

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/audit-logs?action=MODERATION_ACTION&page=0&size=10").with(jwt SYSTEM_ADMIN))`.
2. Assert HTTP 200.
3. Verify `AuditService.log(eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("AuditLog"), isNull(), any())` invoked **exactly once** (Mockito `verify(times(1))`).

**Expected Result (PASS):** Meta-audit call fires exactly once per `search()` invocation.
**Expected Result (FAIL):** No call (gap remains unwired), or the mocked/unwired baseline shows zero interactions.

**Current Status:** 🔴 Not written
**Implementation Note:** Must fire after `search()` succeeds, per TDS §6.1 sequence diagram ordering (search first, then meta-audit).

---

### UC117-TC-002 — Meta-audit payload contains filter params only, never result rows

**Severity:** `CRITICAL`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File` (inverse check — asserting it does NOT happen)
**Feature Under Test:** `AuditController.search()` payload construction
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🔴 RED (Track B)
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-AUDIT-IMP-117 ADR-AUDIT-001 Decision` ("records the filter parameters only ... never the result rows themselves")

**Preconditions:** `AuditService` mocked; `AuditServiceImpl.search(...)` (also mocked at this layer) returns a `Page<AuditLogResponse>` with 5 rows containing distinct `userId`/`details` values.

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/audit-logs?userId={id}&fromDate=2026-01-01T00:00:00Z&toDate=2026-02-01T00:00:00Z&page=0&size=20").with(jwt SYSTEM_ADMIN))`.
2. Capture the `details`/payload argument passed to `AuditService.log(...)` via `ArgumentCaptor<Object>`.
3. Deserialize the captured payload and assert its keys are exactly `{userId, action, fromDate, toDate, page, size}` (or a subset thereof, per null-safe optional filters).
4. Assert none of the 5 mocked result rows' `id`/`details`/`userId` values appear anywhere in the captured payload.

**Expected Result (PASS):** Payload is a filter snapshot only; zero result-row data present.
**Expected Result (FAIL):** Payload includes serialized result rows (quadratic PII duplication risk named in the ADR).

**Current Status:** 🔴 Not written

---

### UC117-TC-003 — Meta-audit logging failure does not block the read response (fail-soft)

**Severity:** `CRITICAL`
**Feature Under Test:** `AuditController.search()` fail-soft wrapper
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🔴 RED (Track B)
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-AUDIT-IMP-117 §12.2 Rollback & Incident Runbook` ("the meta-audit write ... must not be allowed to fail the read request"), `§17 C6`

**Preconditions:** FX-117-007 — `AuditService.log(...)` mocked to `doThrow(new RuntimeException("simulated audit write failure"))`. `AuditServiceImpl.search(...)` mocked to return a valid non-empty page.

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=10").with(jwt SYSTEM_ADMIN))`.
2. Assert HTTP 200 (the read response is NOT affected by the meta-audit failure).
3. Assert the response body still contains the expected `data[]` from the mocked search result.
4. (If a logger is injectable/spyable) assert a warning-level log line was emitted for the swallowed exception — otherwise assert only on the non-propagation behavior.

**Expected Result (PASS):** 200 returned despite meta-audit throwing; exception is swallowed with a warning, never propagated.
**Expected Result (FAIL):** 500 returned, or the exception propagates and the read fails — violates availability > meta-audit completeness per §12.2.

**Current Status:** 🔴 Not written
**Implementation Note:** Mirrors `AuditServiceImpl.toJson(...)`'s existing fail-soft pattern (TDS §12.2) — same try/catch + warn-log idiom, not a new pattern invented for this UC.

---

### UC117-TC-004 — AuditEligibilityPolicy.shouldAudit(VIEW_AUDIT_LOG) returns true

**Severity:** `HIGH`
**Feature Under Test:** `AuditEligibilityPolicy.SENSITIVE_ACTIONS`
**Test File:** `src/test/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicyTest.java`
**TDD Phase:** 🔴 RED (Track B) — genuinely fails against unmodified `SENSITIVE_ACTIONS`
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-AUDIT-IMP-117 ADR-AUDIT-001 Decision`, `§11.3 Chặng 1`

**Test Steps:**
1. Call `auditEligibilityPolicy.shouldAudit(AuditAction.VIEW_AUDIT_LOG)`.
2. Assert result is `true`.
3. Regression guard: assert all previously-sensitive actions (e.g., `SECURITY_EVENT`) still return `true` (no accidental removal while adding the new entry).

**Expected Result (PASS):** `VIEW_AUDIT_LOG` is in `SENSITIVE_ACTIONS`; pre-existing entries untouched.
**Expected Result (FAIL):** Returns `false` (unwired gap still present), or a pre-existing sensitive action was accidentally dropped.

**Current Status:** 🔴 Not written

---

### TRACK A — CHARACTERIZATION: Existing Search/Filter/Auth Behavior (locked down, not newly driven)

---

### UC117-TC-005 — SYSTEM_ADMIN filters by userId and receives correct paginated results

**Severity:** `HIGH`
**Feature Under Test:** `AuditController.search()` / `AuditServiceImpl.search()` (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION — existing, working code; test file is new, feature is not (see §1.2 Track A note)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-AUDIT-IMP-117 §6.1 Sequence Diagram`, `§9.2 happy-path sample`, `§15.1 Mẫu thử thực tế`

**Preconditions:** FX-117-003 (10 seeded logs); FX-117-001 JWT.

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?userId={knownActorId}&page=0&size=20` with SYSTEM_ADMIN JWT.
2. Assert HTTP 200; `data[]` contains only rows where `userId == knownActorId`.
3. Assert response shape matches TDS §9.2 happy-path JSON sample field-for-field (`id, timestamp, userId, action, resourceType, resourceId, details`).

**Expected Result (PASS):** Filter applied correctly; DTO shape matches contract exactly.
**Expected Result (FAIL):** Unfiltered/wrong rows returned, or DTO fields drift from §8.2 contract.

**Current Status:** 🔴 Not written (test file new) — feature itself already implemented per TDS §1.1

---

### UC117-TC-006 — SYSTEM_ADMIN filters by action enum value

**Severity:** `HIGH`
**Feature Under Test:** `AuditController.search()` (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-AUDIT-IMP-117 §1.1`, `§9.1 Endpoints Table`

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?action=MODERATION_ACTION&page=0&size=20` with SYSTEM_ADMIN JWT.
2. Assert all returned rows have `action == "MODERATION_ACTION"`.
3. Repeat with an invalid/unknown action string (e.g. `action=NOT_A_REAL_ACTION`) → assert HTTP 400 `AUDIT-001` (§10).

**Expected Result (PASS):** Valid action filters correctly; invalid action string rejected with `AUDIT-001`.
**Expected Result (FAIL):** Invalid enum string silently ignored/500s instead of a clean 400.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-007 — SYSTEM_ADMIN filters by fromDate/toDate range

**Severity:** `HIGH`
**Feature Under Test:** `AuditController.search()` (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-AUDIT-IMP-117 §1.1`, `§6.1`

**Preconditions:** FX-117-003 (`createdAt` spread over 30 days).

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?fromDate=2026-06-01T00:00:00Z&toDate=2026-06-15T00:00:00Z&page=0&size=20`.
2. Assert all returned rows have `timestamp` within `[fromDate, toDate]` inclusive-consistent with the existing JPQL range semantics.
3. Assert rows sorted `createdAt DESC` (TDS §1.1).

**Expected Result (PASS):** Correct date-range filtering, correct sort order.
**Expected Result (FAIL):** Rows outside range included, or sort order violated.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-008 — fromDate > toDate degrades to empty result (current behavior, OI-117-4 Open)

**Severity:** `LOW`
**Feature Under Test:** `AuditController.search()` (existing, unvalidated)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION — locks down current (Open, not Accepted) behavior
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-AUDIT-IMP-117 §10 Error Codes Table` (`AUDIT-002` row, "validation not currently implemented"), `Phụ lục B OI-117-4`

**Preconditions:** FX-117-005 (`fromDate` after `toDate`).

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?fromDate=2026-08-01T00:00:00Z&toDate=2026-01-01T00:00:00Z&page=0&size=20`.
2. Assert HTTP **200** (not 400) with `data:[]`, `totalElements:0` — this is the currently-observed, TDS-documented behavior.
3. Test is annotated with a code comment: `// OI-117-4 (Open): current behavior is empty-result, not 400. Do NOT change this assertion without a new ADR/Product decision — see TDS §10.`

**Expected Result (PASS = matches documented current behavior):** 200, empty result, no error.
**Expected Result (FAIL = unexpected drift):** 400/500 thrown, or a crash — would indicate undocumented behavior change requiring TDS update.

**Current Status:** 🔴 Not written (test file new)
**Note:** This TC intentionally does NOT assert the "better" behavior (400) because that is an Open Item, not an Accepted decision (TDS §10, `OI-117-4`, "No — current behavior does not error"). Per CASE 2.0 C-discipline, tests must not encode undecided requirements as if they were accepted.

---

### UC117-TC-009 — Non-SYSTEM_ADMIN role is rejected with 403 AUDIT-004

**Severity:** `CRITICAL`
**Feature Under Test:** `AuditController` `@PreAuthorize` (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-AUDIT-IMP-117 §16 Authorization Matrix`, `§10 AUDIT-004`

**Preconditions:** FX-117-002, parameterized across `MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, PARTNER`.

**Test Steps:**
1. `MockMvc.perform(get("/api/v1/admin/audit-logs").with(jwt of each non-admin role))`.
2. Assert HTTP 403, `error.code == "AUDIT-004"`.
3. Assert `AuditService.search(...)` and `AuditService.log(...)` are both never invoked (`verifyNoInteractions`).

**Expected Result (PASS):** 403 for all six non-admin roles; service layer untouched (including no meta-audit call for a rejected request).
**Expected Result (FAIL):** Any non-admin role receives 200, or a meta-audit row is written despite the rejection.

**Current Status:** 🔴 Not written (test file new)
**Note:** Explicitly covers `OI-117-5` (MODERATOR not granted access) — `MODERATOR` is included in the parameterized rejection set, matching TDS §16 exactly (no scoped access implemented).

---

### UC117-TC-010 — Unauthenticated caller is rejected with 401 IAM-001

**Severity:** `HIGH`
**Feature Under Test:** JWT filter chain (platform-wide, existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-AUDIT-IMP-117 §9.2 401 sample`, `§15.2`

**Test Steps:**
1. `GET /api/v1/admin/audit-logs` with no `Authorization` header.
2. Assert HTTP 401, `error.code == "IAM-001"`.

**Expected Result (PASS):** 401 for missing JWT.
**Expected Result (FAIL):** Any other status, or a 500.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-011 — Empty result set returns 200 with data:[], totalElements:0 (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditController.search()` (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-AUDIT-IMP-117 §6.2 Sequence Diagram — Empty State`, `§9.2 Empty State sample`

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?action=LOGIN&fromDate=2099-01-01T00:00:00Z&toDate=2099-01-02T00:00:00Z` (guaranteed no matches).
2. Assert HTTP 200, body matches TDS §9.2 empty-state sample exactly (`data:[], totalElements:0, totalPages:0`).
3. Assert meta-audit (`VIEW_AUDIT_LOG`) is still recorded even on empty result (per §6.2 sequence diagram note — cross-reference with `UC117-TC-001`, Track B).

**Expected Result (PASS):** Correct empty-state shape; meta-audit still fires.
**Expected Result (FAIL):** Non-200 on empty result, or meta-audit skipped when result is empty.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-012 — Page size above MAX_PAGE_SIZE is capped

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditController` pagination binding (existing)
**Test File:** `AuditControllerTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-AUDIT-IMP-117 §4.1 NFR`, `§6.3 Pagination Sequence Diagram note` ("size is capped server-side at 100 ... a client requesting size=10000 silently receives size=100")

**Preconditions:** FX-117-006 (`size=101`).

**Test Steps:**
1. `GET /api/v1/admin/audit-logs?page=0&size=101`.
2. Assert response `size` field == 100 (silently capped, per TDS §6.3 note — NOT a 400, per the documented existing behavior).

**Expected Result (PASS):** Capped at 100, consistent with `AppConstants.MAX_PAGE_SIZE` and TDS §6.3's documented note.
**Expected Result (FAIL):** Unbounded size accepted (risking full-table PII dump), or an undocumented 400 contradicting §6.3.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-013 — Large result set pagination returns correct slice and totalPages

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditServiceImpl.search()` (existing)
**Test File:** `src/test/java/com/carebridge/backend/audit/service/AuditServiceImplTest.java` (extends existing `AuditServiceImplTest.java`)
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-AUDIT-IMP-117 §6.3 Sequence Diagram — Large-Result Pagination`

**Preconditions:** Mock `AuditLogRepository.search(...)` to return `Page<AuditLog>{content: 20 rows, totalElements: 4213, totalPages: 211}` for page 0, and a different 20-row slice for page 1.

**Test Steps:**
1. Call `auditServiceImpl.search(null, null, null, null, PageRequest.of(0, 20, Sort.by("createdAt").descending()))`.
2. Assert returned `Page` metadata matches (`totalElements=4213, totalPages=211`).
3. Call again with `PageRequest.of(1, 20, ...)`; assert distinct row content is returned (page 2 ≠ page 1 content).

**Expected Result (PASS):** Pagination metadata and content slice both correct across pages.
**Expected Result (FAIL):** Off-by-one page boundary, or duplicate/missing rows between pages.

**Current Status:** 🔴 Not written (test file new)

---

### UC117-TC-014 — AuditLogResponse never exposes fields beyond the §8.2 contract

**Severity:** `HIGH`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Feature Under Test:** `AuditLogMapper` (existing)
**Test File:** `src/test/java/com/carebridge/backend/audit/mapper/AuditLogMapperTest.java`
**TDD Phase:** 🟡 CHARACTERIZATION
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-AUDIT-IMP-117 §8.2 Frontend API Client Contract`, `§4.3 Security — PII minimization`

**Preconditions:** `AuditTestFactory.makeAuditLog(...)` with all fields populated, including `ipAddress` and `oldValueJson`.

**Test Steps:**
1. Map via `AuditLogMapper.toResponse(auditLog)`.
2. Reflectively enumerate `AuditLogResponse` fields; assert the set is exactly `{id, timestamp, userId, action, resourceType, resourceId, details}` per TDS §8.2 — no `ipAddress`, no `oldValueJson` field present on the DTO (even though present on the entity).

**Expected Result (PASS):** DTO shape matches §8.2 contract exactly, no extra entity fields leaked.
**Expected Result (FAIL):** DTO exposes `ipAddress`/`oldValueJson` or any field not in the documented contract.

**Current Status:** 🔴 Not written (test file new)

---

### INTEGRATION TEST CASES

---

### UC117-TC-INT-001 — Full search + meta-audit round trip against real DB with audit row verification

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: HTTP request -> AuditController -> AuditServiceImpl -> AuditLogRepository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/audit/integration/AuditLogsIntegrationTest.java` (new — follows existing convention at `security/integration/RegistrationIntegrationTest.java`, `content/integration/ContentIntegrationTest.java`)
**TDD Phase:** 🔴 RED (Track B — meta-audit persistence is genuinely new)
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- `@SpringBootTest` against the project's existing integration-test DB configuration (matching the pattern in `RegistrationIntegrationTest.java` — no Testcontainers usage currently found elsewhere in this backend test suite, so this test follows the same existing convention rather than introducing a new one; see Suspension note in §6).
- Seed FX-117-003 (10 `AuditLog` rows) via repository before each test (fresh instance per Props Isolation rule).

**Test Steps:**
1. Authenticate as SYSTEM_ADMIN (FX-117-001).
2. `GET /api/v1/admin/audit-logs?action=MODERATION_ACTION&page=0&size=10` → assert HTTP 200, expected rows returned.
3. Query `audit_logs` table directly: assert exactly one **new** row exists with `action='VIEW_AUDIT_LOG'`, `actor_user_id = <admin id>`, `entity_type='AuditLog'`, `entity_id IS NULL`.
4. Deserialize that row's `new_value_json` and assert it contains `{action: "MODERATION_ACTION", page: 0, size: 10}` (filter snapshot, no result-row data — cross-check with `UC117-TC-002`).

**Expected Result (PASS):** Meta-audit row is actually persisted with correct actor/action/payload — proves ADR-AUDIT-001's wiring end-to-end, not just at the mocked-controller-unit level.
**Expected Result (FAIL):** No meta-audit row written, or written with wrong actor/payload.

**DB Assertion:**
```java
List<AuditLog> metaAuditRows = auditLogRepository.findByActionAndActorUserId(
        AuditAction.VIEW_AUDIT_LOG, adminId);
assertThat(metaAuditRows).hasSize(1);
JsonNode payload = objectMapper.readTree(metaAuditRows.get(0).getNewValueJson());
assertThat(payload.get("action").asText()).isEqualTo("MODERATION_ACTION");
```

**Current Status:** 🔴 Not written

---

### UC117-TC-INT-002 — VIEW_AUDIT_LOG insert succeeds against the real audit_logs_action_check CHECK constraint

**Severity:** `HIGH`
**Feature Under Test:** DB constraint compatibility (`audit_logs_action_check`)
**Test File:** `AuditLogsIntegrationTest.java`
**TDD Phase:** 🔴 RED (Track B — proves L2/OI-117-3 does not block this specific value)
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CB-AUDIT-IMP-117 §5.2 Data Structure`, `Phụ lục B OI-117-3`

**Preconditions:** Real PostgreSQL schema with Flyway migrations applied (same DB as `UC117-TC-INT-001`).

**Test Steps:**
1. Directly persist an `AuditLog` entity with `action = AuditAction.VIEW_AUDIT_LOG` via `auditLogRepository.save(...)` (bypassing the controller, isolating this test to the DB-constraint question only).
2. Assert the save succeeds without a `DataIntegrityViolationException` (which would occur if `VIEW_AUDIT_LOG` were absent from the `audit_logs_action_check` CHECK constraint array).
3. Regression guard: assert a **known-absent** enum value (e.g., pick one of the ~38 enum values documented in TDS §5.2 as outside the 14-value constraint array, if identifiable) DOES throw `DataIntegrityViolationException` — proving the test is actually exercising the CHECK constraint and not silently no-op'ing.

**Expected Result (PASS):** `VIEW_AUDIT_LOG` insert succeeds; a genuinely out-of-constraint enum value fails — proves the constraint is live and `VIEW_AUDIT_LOG` specifically is safe.
**Expected Result (FAIL):** `VIEW_AUDIT_LOG` insert throws `DataIntegrityViolationException` (would mean ADR-AUDIT-001 cannot ship as designed — critical finding requiring TDS §5.2 correction), or the regression-guard step 3 also passes (indicating the CHECK constraint isn't actually being tested).

**Current Status:** 🔴 Not written
**Implementation Note:** This test exists specifically because TDS §5.2 flags `OI-117-3` as a real, pre-existing risk (52 enum values vs. 14 DB-constraint values) that directly touches this UC's new write path. It is not speculative — it is the one concrete DB-level assumption ADR-AUDIT-001 depends on.

---

### WEB TEST CASES

---

### UC117-WEB-TC-001 — AuditLogFilterBar submits exactly the 3 Accepted filters

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditLogFilterBar.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/audit/components/AuditLogFilterBar.test.tsx`
**TDD Phase:** 🔴 RED (new frontend)
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `CB-AUDIT-IMP-117 §8.2 AuditLogFilters interface`, `ADR-AUDIT-002 Decision (Option A)`

**Test Steps:**
1. Render `<AuditLogFilterBar onChange={mockOnChange} />`.
2. Fill in `userId`, select an `action`, set `fromDate`/`toDate`; submit.
3. Assert `mockOnChange` called with an object containing exactly `{userId, action, fromDate, toDate}` keys — no `entityType`/`entityId`/free-text search field present in the emitted filter object (asserts `OI-117-1` is NOT implemented, per ADR-AUDIT-002 Option A scope).

**Expected Result (PASS):** Filter bar UI matches the Accepted 3-filter contract exactly.
**Expected Result (FAIL):** UI exposes an `entityType`/free-text field not backed by the API (hallucinated contract, CASE 2.0 AP-AI-005).

**Current Status:** 🔴 Not written

---

### UC117-WEB-TC-002 — AuditLogTable renders paginated rows and empty state with Clear filters action

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditLogTable.tsx` / `AuditLogsPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/audit/pages/AuditLogsPage.test.tsx`
**TDD Phase:** 🔴 RED (new frontend)
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `CB-AUDIT-IMP-117 §6.2 Sequence Diagram — Empty State`, SRS AF2

**Test Steps:**
1. Mock `GET /api/v1/admin/audit-logs` returning FX-117-008 (happy-path, non-empty).
2. Render `<AuditLogsPage />`; assert table renders one row per fixture item.
3. Re-mock the same endpoint to return an empty-state response (§9.2 sample); trigger a re-search.
4. Assert empty-state message renders ("No audit log entries match the selected filters") and a "Clear filters" control is present and functional (resets filters and re-fetches).

**Expected Result (PASS):** Both populated and empty states render correctly per SRS AF2.
**Expected Result (FAIL):** Empty state shows a blank screen or stale data; "Clear filters" missing/non-functional.

**Current Status:** 🔴 Not written

---

### UC117-WEB-TC-003 — AuditLogTable renders details in a collapsed/expandable viewer, not inline plaintext

**Severity:** `HIGH`
**Feature Under Test:** `AuditLogTable.tsx`
**Test File:** `AuditLogsPage.test.tsx`
**TDD Phase:** 🔴 RED (new frontend)
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CB-AUDIT-IMP-117 ADR-AUDIT-002 §Hệ quả — Interim mitigation`, `§17 C2`

**Preconditions:** FX-117-008 includes a row with a `details` JSON string containing a synthetic PII-shaped value (e.g., `{"email":"synthetic@example.test"}` — SYNTHETIC only, per doc header rule).

**Test Steps:**
1. Render `<AuditLogTable rows={...} />` with the fixture row.
2. Assert the `details` value is **not** present as visible text in the initial table cell render (collapsed by default).
3. Simulate expand interaction (click/toggle); assert the JSON becomes visible only after explicit user action.
4. Assert no client-side transformation/enrichment is applied to `details` beyond raw JSON pretty-printing (CASE 2.0 C2 — "zero client-side filtering/redaction/enrichment").

**Expected Result (PASS):** `details` is not exposed at a glance; requires explicit expand; content is unmodified raw JSON when expanded (no invented redaction).
**Expected Result (FAIL):** `details` rendered inline in the table by default (over-exposure), or the frontend attempts to mask/redact fields itself (violates C2 — that decision belongs to ADR-AUDIT-002, still Open, and must not be pre-empted by the frontend).

**Current Status:** 🔴 Not written
**Note:** This TC deliberately does NOT test field-level redaction logic, since `OI-117-2`/ADR-AUDIT-002 is Open — only the Accepted interim UI mitigation (collapsed/expandable rendering) is tested.

---

### UC117-WEB-TC-004 — AuditLogsPage renders access-denied UI on 403 response

**Severity:** `MEDIUM`
**Feature Under Test:** `AuditLogsPage.tsx`
**Test File:** `AuditLogsPage.test.tsx`
**TDD Phase:** 🔴 RED (new frontend)
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `CB-AUDIT-IMP-117 §9.2 403 AUDIT-004 sample`, `§16 Authorization Matrix`

**Test Steps:**
1. Mock API returning 403 `AUDIT-004` error body.
2. Render `<AuditLogsPage />`.
3. Assert an access-denied message renders, not a crash or blank screen/unhandled promise rejection.

**Expected Result (PASS):** Graceful error UI.
**Expected Result (FAIL):** Unhandled promise rejection or blank page.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Track | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-------|-----------|-----------------|-------------------|------------------|
| `UC117-TC-001` | B (new) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-002` | B (new) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-003` | B (new) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-004` | B (new) | `AuditEligibilityPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-005` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-006` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-007` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-008` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-009` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-010` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-011` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-012` | A (characterization) | `AuditControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-013` | A (characterization) | `AuditServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-014` | A (characterization) | `AuditLogMapperTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-INT-001` | B (new) | `AuditLogsIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-TC-INT-002` | B (new) | `AuditLogsIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC117-WEB-TC-001` | Web (new) | `AuditLogFilterBar.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC117-WEB-TC-002` | Web (new) | `AuditLogsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC117-WEB-TC-003` | Web (new) | `AuditLogsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `UC117-WEB-TC-004` | Web (new) | `AuditLogsPage.test.tsx:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Important distinction (carried from §1.2):** Unlike a typical Red Gate where the *entire* production class is stubbed to throw, `AuditController.search(...)` and `AuditServiceImpl.search(...)` **already have real, working implementations in production**. Re-stubbing the whole controller to throw would be destructive to already-shipped, already-manually-verified (TDS §15) behavior and is explicitly forbidden by TDS §17 C1/C5 (backend change is scoped to exactly two call sites). Therefore:

- **Track A tests (`UC117-TC-005` through `UC117-TC-014`)** are NOT run against a throwing stub. They are run against the **current, unmodified** `AuditController`/`AuditServiceImpl` and are expected to **PASS immediately** (this is the "characterization" designation in §1.2/§3 TDS-01) — they exist to catch regressions introduced while implementing Track B, not to drive new implementation. Their Red Gate row is therefore N/A by design, not a gate failure.
- **Track B tests (`UC117-TC-001`–`004`, `UC117-TC-INT-001`, `UC117-TC-INT-002`)** are the only tests subject to a genuine Red Gate: they must FAIL against the current, unmodified `AuditEligibilityPolicy`/`AuditController` (meta-audit wiring absent), confirming the gap ADR-AUDIT-001 describes actually exists before the fix is applied.
- **Web tests** are all genuinely new (no existing frontend page, per TDS §1.1) and follow a standard full Red Gate.

**Stub cho Red Phase (Track B only — NOT applied to the whole AuditController):**

```java
// AuditEligibilityPolicy — Red Phase baseline is simply the CURRENT unmodified code
// (SENSITIVE_ACTIONS without VIEW_AUDIT_LOG). No stub needed — the absence IS the red state.
// UC117-TC-004 must fail against this as-is.

// AuditController.search(...) — Red Phase baseline is the CURRENT unmodified method body
// (no auditService.log(VIEW_AUDIT_LOG, ...) call present). No stub needed — the absence IS
// the red state. UC117-TC-001/002/003 must fail (verify(times(1)) sees zero invocations)
// against this as-is.

// If a placeholder call site is scaffolded early (e.g. during pairing), it must use the
// standard Red Phase stub idiom for the NEW logic only, isolated from the existing search():
private void recordMetaAudit(UUID adminId, AuditSearchParams params) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Baseline | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|----------|--------|----------------------------------|
| `UC117-TC-001` | Unmodified `AuditController` (no log call) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC117-TC-004` | Unmodified `AuditEligibilityPolicy` (no VIEW_AUDIT_LOG in set) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC117-TC-INT-001` | Unmodified backend | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC117-TC-005` (Track A control) | Unmodified backend | 🟢 PASS (by design — characterization, not Red) | ☐ FAIL ☐ PASS | If FAIL: existing production behavior differs from TDS §9.2 sample — escalate to Tech Lead, do NOT silently "fix" the test to match |
| `UC117-WEB-TC-001` | No frontend files exist | 🔴 FAIL (compile/module error) | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub/baseline commit hash: `___` (to be filled at implementation time)
- Track B tests all FAIL, Track A control (`UC117-TC-005`) PASSES against unmodified baseline? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-AUDIT-IMP-117` — ADR-AUDIT-001 confirmed `Accepted` (currently Accepted per TDS §3); ADR-AUDIT-002 remains `Open`/`Proposed` and does NOT block entry (TDS §11.1 explicitly states this)
- [ ] Logic Issues (§2, L1-L5) confirmed with Tech Lead
- [ ] No migration required (TDS §5.2) — nothing to wait on for schema
- [ ] Test fixtures (§3 TDS-05) prepared as seed builders/factories (`AuditTestFactory`)
- [ ] DPO review of `details` field exposure (TDS header Sign-off row) — recommended before production, explicitly non-blocking for Draft-to-dev per TDS §11.1

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green (Track A + Track B)
- [ ] `./mvnw verify` / integration profile — `UC117-TC-INT-001`, `UC117-TC-INT-002` green
- [ ] Web: `npm run test:run` — all Vitest/Testing Library tests green (pending package.json setup — see Suspension Criteria)
- [ ] Test coverage ≥ 80% lines for the new code only (`AuditEligibilityPolicy` delta, `AuditController.search()` meta-audit call site, all new frontend files) — coverage of pre-existing `AuditServiceImpl`/`AuditLogRepository` is a stretch goal, not a gate, since that code predates this UC
- [ ] No business/authorization logic added to `AuditController` beyond the existing `@PreAuthorize` (TDS §17 C5)
- [ ] `UC117-TC-003` (fail-soft) green — mandatory gate, matches TDS §17 C6
- [ ] `UC117-TC-009` (non-admin rejection, including MODERATOR) green — mandatory gate, matches TDS §16
- [ ] `UC117-TC-INT-002` (CHECK constraint compatibility) green — mandatory gate given `OI-117-3` is a live, documented risk touching this exact write path

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — Track B tests FAIL against unmodified baseline; Track A control test (`UC117-TC-005`) PASSES against unmodified baseline (proves it is genuinely characterizing existing behavior, not accidentally testing unimplemented code)
- [ ] **Contract Existence**: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation**: every test instance created via `AuditTestFactory`, no shared mutable fixtures across `@Test` methods
- [ ] **Oracle Source**: every assertion traces to an SRS/ADR/BR/schema/Open-Item citation (§4 "Oracle Source" fields)
- [ ] **No invented redaction logic**: `UC117-WEB-TC-003` confirmed to test only the Accepted interim mitigation, not field-level redaction (ADR-AUDIT-002 remains Open)

### Suspension Criteria

- Web test infrastructure (Vitest + Testing Library) not yet confirmed present in `CareBridgeWebApp/package.json` (verified during spec authoring: no `vitest`/`@testing-library` dependency currently listed) — `UC117-WEB-TC-*` suspended until added, same open item precedent as `UC114_ManageUserAccounts_Test-Spec.md` §6 Suspension Criteria (UC97 precedent referenced there)
- `UC117-TC-INT-001`/`UC117-TC-INT-002` (integration tests) assume the project's existing `@SpringBootTest`-based integration convention (as used in `security/integration/RegistrationIntegrationTest.java`) rather than Testcontainers, since no Testcontainers usage was found anywhere in the current backend test suite during spec authoring — flagged as an environment assumption, not silently guessed; confirm against the actual test-profile DB config at implementation time
- `OI-117-1` (`entityType`/`entityId` filters) and `OI-117-2` (`details` redaction) remain Open per ADR-AUDIT-002 — no test case in this spec asserts behavior for either; both are explicitly out of scope pending Product/Architect/DPO decision
- `OI-117-4` (date-range validation) remains Open — `UC117-TC-008` characterizes current behavior only, does not assert a decision that hasn't been made
- ~~`OI-117-5` (MODERATOR scoped access) remains Open~~ — **RESOLVED 2026-07-02**: Product confirmed no scoped access for MODERATOR. `UC117-TC-009` asserts MODERATOR is rejected, matching both code and the confirmed decision — no longer a suspension condition

---

## 7. Rollback Plan

```bash
# No migration to revert — UC117 introduces no schema change (TDS §5.2).

# Revert the two Track B backend changes (ADR-AUDIT-001)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java

# Revert new backend tests
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/controller/AuditControllerTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicyTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/mapper/AuditLogMapperTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/integration/AuditLogsIntegrationTest.java

# Revert new frontend (entire new AuditLogsPage feature — no existing page to preserve, per TDS §1.1)
git checkout -- 05_Development/CareBridgeWebApp/src/features/audit/

# NOTE: AuditServiceImpl, AuditLogRepository, AuditLog entity, AuditLogMapper are NOT touched
# by this UC (TDS §17 C1) and therefore require no rollback action — reverting the two files
# above fully restores pre-UC117 backend behavior.

# Gap vẫn OPEN → giữ nguyên entry trong tracking doc (ADR-AUDIT-001 meta-audit gap reopens)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Track B test PASS against unmodified baseline (should be RED) | ☐ Chờ Red Gate thực thi khi implement (§5.1) — Track A control test intentionally PASSES by design, do not confuse with this anti-pattern | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes MODERATOR scoped access, or `entityType`/`entityId` filtering, or `details` redaction — none of which are Accepted | ☑ Không phát hiện — `UC117-TC-009` explicitly asserts MODERATOR rejection (matches current code); `UC117-WEB-TC-001` explicitly asserts no `entityType` field; `UC117-WEB-TC-003` explicitly tests only Accepted interim mitigation, not redaction | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `AuditController` contains business/authorization logic beyond `@PreAuthorize` | ☑ Không phát hiện — controller tests (`TC-001`–`003`, `TC-009`/`010`) only assert RBAC/orchestration/meta-audit call-forwarding, no business logic asserted in controller | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports/uses `AuditQueryRequest`, or invents new query params (`entityType`, free-text search) not in TDS §9.1 | ☑ Không phát hiện — all types/params match TDS §8/§9 exactly; `UC117-WEB-TC-001` explicitly guards against this | G-3 |
| AP-CB-AUDIT-001 | **Destructive Red Gate on Existing Code** | Stubbing the entire already-working `AuditController.search()`/`AuditServiceImpl.search()` to throw, destroying manually-verified production behavior | ☑ Không phát hiện — §5.1 explicitly restricts the Red Gate stub to the NEW meta-audit call site only; Track A tests run against the unmodified, working implementation | **Release-blocking** |
| AP-CB-AUDIT-002 | **Meta-Audit Payload Over-Collection** | Test/implementation logs result rows (PII) instead of filter params only in the `VIEW_AUDIT_LOG` payload | ☑ Không phát hiện — `UC117-TC-002`/`UC117-TC-INT-001` explicitly assert payload excludes result-row data | **Release-blocking** |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (Red Gate execution pending at implementation time)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*Test-Spec based on TDD Template v2.0 + CASE 2.0. Status: Draft — pending Tech Lead review, ADR-AUDIT-002 Open items resolution, DPO sign-off, and Red Gate execution at implementation time.*
