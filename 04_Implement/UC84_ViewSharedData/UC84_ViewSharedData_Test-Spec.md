# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-84 View Shared Data

**Document ID:** `CB-FAM-TDD-004`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC84_ViewSharedData/UC84_ViewSharedData_TDS.md` (`CB-FAM-IMP-004`)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.3.2 (L3254-3273)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000004__create_family_alert_log.sql`
- Sibling reference (pattern only, not copied): `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_Test-Spec.md` (`CB-FAM-TDD-002`)

> **TDD convention:** This document describes test cases BEFORE production code is written. Mandatory order: write test → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Do not mark a test ✅ until `./mvnw test` is green.
> No real PII in test data — SYNTHETIC data only.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent` | Initial draft — TDD spec for UC-84 View Shared Data |

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
| **Feature / Gap ID** | `UC-84` |
| **Module** | `ViewSharedData — family bounded context` |
| **Spec gốc** | `CB-FAM-IMP-004` |
| **Priority** | 🟡 P2 (SRS Priority: Medium) |
| **Sprint** | `S3 Cross-Domain Integration` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `care_groups, care_group_members (permission_json), care_tasks, baby_daily_logs, postpartum_logs, safety_alerts, family_alert_log` |
| **Downstream Consumers** | `Family Sync hub UI (mobile), (adjacent, out of scope) FCM notification module` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-004 §17.1 (C1-C7)` |
| **Constraints Injected** | C1 (membership-before-permission ordering), C2 (deny-by-default), C3 (checklists rejected as 400), C4 (accountId from JWT only), C5 (no PII in DTOs), C6 (no FCM sending logic), C7 (alerts join unconfirmed — Open Item OI-4) |
| **Model** | `Claude (Sonnet 5)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` and approved migrations are the final persistence oracle; ERD/SRS prose is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-84 description lists 4 categories: "calendar, logs, checklists, or alerts" | `checklist_items`/`checklist_templates` (V1 L49/65) have **no visible FK** to `care_group_id` or `baby_id` — no confirmed join path exists for care-group-scoped checklist sharing | Test scope explicitly **excludes** `checklists`. Tests assert `category=checklists` returns `400 FAM-012`, not a data result. No test asserts checklist data retrieval. |
| L2 | `care_tasks.status` might be assumed to have a fixed enum via a DB CHECK constraint | `care_tasks.status varchar(20) NOT NULL DEFAULT 'OPEN'` has **no CHECK constraint** in `V1__init_schema.sql` — only an index `idx_care_tasks_status`. Status validity is an app-level (Java enum) decision only | Tests treat `status` as an opaque string passthrough from DB; do NOT assert DB-level enum enforcement. Any status-value assertions use values the test itself seeds, not a schema-derived exhaustive enum list. |
| L3 | `permission_json` might be assumed to have a confirmed, documented JSON shape | `permission_json` (jsonb) has **zero consumers/producers** anywhere in the codebase as of this draft — shape is an assumption pending sibling UC-72 | Tests use the ASSUMED shape `{"calendar": bool, "logs": bool, "checklists": bool, "alerts": bool}` explicitly labeled as an assumption in fixtures (`FX-002`). Any test asserting a specific key name is marked `Oracle Source: ADR-FAM-003 (assumed, Open Item OI-2)`, not treated as a confirmed contract. |
| L4 | "Alerts" category might be assumed backed by a single dedicated care-group-scoped table | Neither `safety_alerts` (`recipient_user_id`-keyed) nor `family_alert_log` (session-scoped via `emergency_sessions.user_id`, aggregate-only columns, no per-recipient row) is care-group-scoped | ALERTS-category test cases are marked `Oracle Source: ADR-FAM-005 §OI-4 (assumed join, pending Principal Architect review)`. Test suite includes a explicit "join path unconfirmed" marker test that fails loudly (not silently) if implementation ships without sign-off tracking. |
| L5 | SRS description implies FCM push might be part of this UC's implementation | FCM is a secondary actor only; per TDS §7.2, UC-84 is read-only and does not send notifications — that is owned by separate "Receive X Notification" UCs | No test asserts FCM send calls from `SharedDataServiceImpl`. A negative test confirms no notification-service mock is invoked during `getSharedData()`. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewSharedData (UC-84) bao gồm các layer:
├── Domain (PermissionCategory resolution — pure logic, no deps)
├── Application / Services (SharedDataServiceImpl — mock JPA Repositories với Mockito)
├── Policy (CareGroupAccessPolicy.hasPermission() — mock repository với Mockito)
├── Controller (SharedDataController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest) — calendar + logs only in v1;
    alerts integration tests are conditional on Open Item OI-4 resolution (see §6 Suspension Criteria)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-84 (§3.3.3.2, L3254-3273)` | Normal flow (validate access → apply rules → display result), AF1 (cancel/safe state), AF2 (empty state), E1 (access denied), E2 (invalid data rejected), E3 (external failure handling) |
| `ADR-FAM-002` (reused from UC-216) | `isMember()` gating — ACCEPTED only |
| `ADR-FAM-003` | Permission-filtered query per category; deny-by-default on NULL/missing flag |
| `ADR-FAM-004` | On-demand query design — no caching/staleness test needed |
| `ADR-FAM-005` | Category scope: calendar/logs/alerts in v1; checklists → 400 |
| `BR-RBAC, BR-PRIVACY (SRS L3269)` | Auth ordering (membership before permission); no PII in response DTOs |
| PDPA | No email/phone in `SharedDataItemDto.summary` |
| `CB-FAM-IMP-004 §9, §10, §16` | API contract, error codes, authorization matrix — every row/field must map to ≥1 TC below |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | ACCEPTED member, category flag=true → 200 with data | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-001`, `FAM-UC84-TC-002`, `FAM-UC84-TC-003` |
| TC-COND-002 | ACCEPTED member, category flag=false → 403 FAM-011 | `CareGroupAccessPolicy.hasPermission()` | `FAM-UC84-TC-004` |
| TC-COND-003 | ACCEPTED member, `permission_json`=NULL → 403 FAM-011 (deny-by-default) | `CareGroupAccessPolicy.hasPermission()` | `FAM-UC84-TC-005` |
| TC-COND-004 | ACCEPTED member, category key missing from JSON → 403 FAM-011 | `CareGroupAccessPolicy.hasPermission()` | `FAM-UC84-TC-006` |
| TC-COND-005 | ACCEPTED member, all flags=true → 200 for every v1 category | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-007` |
| TC-COND-006 | ACCEPTED member, all flags=false → 403 for every v1 category | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-008` |
| TC-COND-007 | ACCEPTED member, mixed flags (calendar=true, logs=false, alerts=true) → per-category correct result | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-009` |
| TC-COND-008 | PENDING member → 403 FAM-003, regardless of `permission_json` | `CareGroupAccessPolicy.isMember()` | `FAM-UC84-TC-010` |
| TC-COND-009 | REVOKED member → 403 FAM-003 | `CareGroupAccessPolicy.isMember()` | `FAM-UC84-TC-011` |
| TC-COND-010 | Non-member → 403 FAM-003 | `CareGroupAccessPolicy.isMember()` | `FAM-UC84-TC-012` |
| TC-COND-011 | `category=checklists` → 400 FAM-012 (deferred, no data leak) | `SharedDataController` validation | `FAM-UC84-TC-013` |
| TC-COND-012 | `category=` invalid string (e.g. `"xyz"`) → 400 FAM-012 | `SharedDataController` validation | `FAM-UC84-TC-014` |
| TC-COND-013 | `groupId` not found → 404 FAM-010 | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-015` |
| TC-COND-014 | ACCEPTED + calendar=true, but zero `care_tasks` rows → 200 empty items (AF2) | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-016` |
| TC-COND-015 | Response DTO never contains email/phone | Response mapping | `FAM-UC84-TC-017` |
| TC-COND-016 | No authentication (missing JWT) → 401 | Spring Security filter chain | `FAM-UC84-TC-018` |
| TC-COND-017 | `care_tasks.status` treated as opaque string (no CHECK constraint assumption) | `SharedDataItemDto` mapping | `FAM-UC84-TC-019` |
| TC-COND-018 | ALERTS category join — marker test flags unconfirmed join (Open Item OI-4) | `SharedDataServiceImpl` (alerts path) | `FAM-UC84-TC-020` |
| TC-COND-019 | No FCM/notification service is invoked during a read call | `SharedDataServiceImpl.getSharedData()` | `FAM-UC84-TC-021` |
| TC-COND-020 | Full E2E happy path via API for each v1 category | `SharedDataController` + DB | `FAM-UC84-TC-022`, `FAM-UC84-TC-023`, `FAM-UC84-TC-024` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `category` param: valid v1 set vs. `checklists` vs. arbitrary invalid string | Three distinct classes must be tested (valid/deferred/garbage) |
| Boundary Value Analysis | `permission_json` combinations: all-true, all-false, mixed, NULL, missing-key | Boundary of the deny-by-default rule is the highest-risk area in this feature (see PERMISSION-FILTERED VISIBILITY section below) |
| State Transition Testing | `invitation_status`: ACCEPTED / PENDING / REVOKED | Confirms UC-216's `isMember()` gate is correctly reused, not weakened |
| Error Guessing | Attempt to bypass permission check via query manipulation, invalid category injection, missing JWT | Security-focused coverage for RBAC/PDPA compliance |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `care_group_members { invitation_status: 'ACCEPTED', permission_json: {"calendar":true,"logs":true,"checklists":false,"alerts":true} }` | Happy path, all-relevant-true |
| `FX-002` | DB seed | `care_group_members { invitation_status: 'ACCEPTED', permission_json: {"calendar":false,"logs":false,"alerts":false} }` | All-false combo — assumed shape per Open Item OI-2 |
| `FX-003` | DB seed | `care_group_members { invitation_status: 'ACCEPTED', permission_json: NULL }` | Deny-by-default on NULL |
| `FX-004` | DB seed | `care_group_members { invitation_status: 'ACCEPTED', permission_json: {"calendar":true} }` (missing `logs`/`alerts` keys) | Deny-by-default on missing key |
| `FX-005` | DB seed | `care_group_members { invitation_status: 'PENDING', permission_json: {"calendar":true,...} }` | Membership gate takes precedence over permission flags |
| `FX-006` | DB seed | `care_group_members { invitation_status: 'REVOKED' }` | Membership gate — revoked |
| `FX-007` | DB seed | `care_tasks { care_group_id: CG-001, status: 'OPEN' }` × 2 rows | Calendar category data |
| `FX-008` | DB seed | `baby_daily_logs { baby_id: linked-to-CG-001 }` × 1 row | Logs category data |
| `FX-009` | DB seed | `safety_alerts { recipient_user_id: member-of-CG-001 }` × 1 row | Alerts category data (Open Item OI-4 assumption) |
| `FX-010` | JWT | `{ sub: 'acc-001', role: 'FAMILY' }` | Auth context for E2E tests |
| `FX-011` | env | none required (no HMAC/secret dependency for this read-only module) | — |

---

## PERMISSION-FILTERED VISIBILITY — CORE RISK

> This is the highest-risk area of UC-84: a single incorrect default (e.g., treating missing/NULL `permission_json` as "allow") would leak private family health/safety data. Every flag combination below MUST have a dedicated test — no combination may be skipped or assumed-covered by another.

| Combo | calendar | logs | alerts | Expected for calendar req | Expected for logs req | Expected for alerts req | Test Case |
|-------|----------|------|--------|---------------------------|------------------------|---------------------------|-----------|
| All true | true | true | true | 200 | 200 | 200 | `FAM-UC84-TC-007` |
| All false | false | false | false | 403 FAM-011 | 403 FAM-011 | 403 FAM-011 | `FAM-UC84-TC-008` |
| Mixed | true | false | true | 200 | 403 FAM-011 | 200 | `FAM-UC84-TC-009` |
| NULL permission_json | — | — | — | 403 FAM-011 | 403 FAM-011 | 403 FAM-011 | `FAM-UC84-TC-005` |
| Missing key | true (present) | (absent) | (absent) | 200 | 403 FAM-011 | 403 FAM-011 | `FAM-UC84-TC-006` |
| Calendar-only | true | false | false | 200 | 403 FAM-011 | 403 FAM-011 | `FAM-UC84-TC-004` (logs=false variant) |
| Logs-only | false | true | false | 403 FAM-011 | 200 | 403 FAM-011 | Covered by `FAM-UC84-TC-009` variant (see Implementation Note in TC-009) |

---

## 4. Test Case Specification

> **TC ID format:** `FAM-UC84-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written (all cases in this draft)

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// SharedDataTestFactory.java — every @Test uses a fresh instance
// ═══════════════════════════════════════════════════════════

class SharedDataTestFactory {

    static CareGroup makeCareGroup() {
        CareGroup group = new CareGroup();
        group.setCareGroupId(UUID.fromString("00000000-0000-0000-0000-0000000000C1"));
        group.setGroupName("Test Care Group");
        group.setStatus("ACTIVE");
        return group;
    }

    static CareGroupMember makeMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = new CareGroupMember();
        member.setCareGroupMemberId(UUID.randomUUID());
        member.setCareGroupId(makeCareGroup().getCareGroupId());
        member.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000A1"));
        member.setInvitationStatus("ACCEPTED"); // FX-001 baseline
        member.setPermissionJson("{\"calendar\":true,\"logs\":true,\"checklists\":false,\"alerts\":true}");
        overrides.accept(member);
        return member;
    }

    static CareTask makeCareTask(Consumer<CareTask> overrides) {
        CareTask task = new CareTask();
        task.setCareTaskId(UUID.randomUUID());
        task.setCareGroupId(makeCareGroup().getCareGroupId());
        task.setTitle("Prenatal checkup");
        task.setStatus("OPEN"); // no DB CHECK constraint — app-level only, see Logic Issue L2
        overrides.accept(task);
        return task;
    }

    static SafetyAlert makeSafetyAlert(Consumer<SafetyAlert> overrides) {
        SafetyAlert alert = new SafetyAlert();
        alert.setSafetyAlertId(UUID.randomUUID());
        alert.setRecipientUserId(makeMember(m -> {}).getUserId());
        alert.setAlertReason("FALL_DETECTED");
        overrides.accept(alert);
        return alert;
    }
}
```

---

### FAM-UC84-TC-001 — ACCEPTED member with calendar=true views calendar data

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData(groupId, accountId, CALENDAR, page, size)`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED — not implemented
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-003 §Decision`, `CB-FAM-IMP-004 §9.2 happy path example`

**Preconditions:**
- `FX-001` seeded (ACCEPTED member, `permission_json.calendar=true`)
- `FX-007` seeded (2 `care_tasks` rows scoped to the group)

**Test Steps:**
1. Arrange: `SharedDataTestFactory.makeMember(m -> {})`, `SharedDataTestFactory.makeCareTask(t -> {})` × 2
2. Act: call `getSharedData(CG-001, ACC-001, CALENDAR, 0, 20)`
3. Assert: response `items` contains exactly 2 entries, `category=CALENDAR`, `sourceType="CARE_TASK"`

**Expected Result (PASS):** `SharedDataResponse.totalItems == 2`, all items map from seeded `care_tasks` rows.
**Expected Result (FAIL):** Empty list, wrong category filter, or thrown exception.

**Current Status:** 🔴 Not written
**Implementation Note:** Ensure `findByCareGroupId` is called with the exact `groupId`, not a global unscoped query.

---

### FAM-UC84-TC-002 — ACCEPTED member with logs=true views logs data

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData(groupId, accountId, LOGS, page, size)`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-005 §Decision (logs join via care_groups.baby_id/owner_user_id)`

**Preconditions:**
- `FX-001` seeded (`permission_json.logs=true`)
- `FX-008` seeded (1 `baby_daily_logs` row linked via `care_groups.baby_id`)

**Test Steps:**
1. Arrange: seed member + baby daily log linked to the same care group's `baby_id`
2. Act: call `getSharedData(CG-001, ACC-001, LOGS, 0, 20)`
3. Assert: response contains 1 item, `sourceType="BABY_DAILY_LOG"`

**Expected Result (PASS):** 1 item returned, correctly mapped.
**Expected Result (FAIL):** 0 items (join broken) or items from unrelated baby.

**Current Status:** 🔴 Not written
**Implementation Note:** Join must go through `care_groups.baby_id`, not directly `care_group_members`.

---

### FAM-UC84-TC-003 — ACCEPTED member with alerts=true views alerts data

**Severity:** `HIGH`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData(groupId, accountId, ALERTS, page, size)`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-005 §OI-4 (assumed join, pending Principal Architect review)` — **Open Item, not a confirmed contract**

**Preconditions:**
- `FX-001` seeded (`permission_json.alerts=true`)
- `FX-009` seeded (1 `safety_alerts` row with `recipient_user_id` = a member of the care group)

**Test Steps:**
1. Arrange: seed member + safety alert addressed to a group member
2. Act: call `getSharedData(CG-001, ACC-001, ALERTS, 0, 20)`
3. Assert: response contains 1 item, `sourceType="SAFETY_ALERT"`

**Expected Result (PASS):** 1 item returned.
**Expected Result (FAIL):** 0 items, or exception, or item from a `recipient_user_id` outside the group.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ **This test encodes an UNCONFIRMED join (Open Item OI-4).** If Principal Architect review changes the join predicate, this test's fixture/assertions must be updated — do not treat as final until OI-4 is closed.

---

### FAM-UC84-TC-004 — ACCEPTED member with logs=false → 403 FAM-011

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-003 §Decision`

**Preconditions:** `permission_json = {"calendar":true,"logs":false,"alerts":true}`, `invitation_status=ACCEPTED`

**Test Steps:**
1. Arrange: seed member with `logs=false`
2. Act: call `getSharedData(CG-001, ACC-001, LOGS, 0, 20)`
3. Assert: `ForbiddenException` thrown with code `FAM-011`

**Expected Result (PASS):** Exception thrown, code = `FAM-011`.
**Expected Result (FAIL):** 200 response returned (permission bypass — CRITICAL security failure).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the core RBAC boundary test — must never regress.

---

### FAM-UC84-TC-005 — permission_json = NULL → deny-by-default, 403 FAM-011

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-003 §Hệ quả (deny-by-default decision)`

**Preconditions:** `FX-003` — `permission_json = NULL`, `invitation_status=ACCEPTED`

**Test Steps:**
1. Arrange: seed member with `permission_json=NULL`
2. Act: call `hasPermission(CG-001, ACC-001, CALENDAR)` (and repeat for LOGS, ALERTS)
3. Assert: returns `false` for every category

**Expected Result (PASS):** `false` for all 3 categories.
**Expected Result (FAIL):** `true` for any category (would mean a pre-UC-72 row silently grants full access — CRITICAL data leak).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important regression guard in this module — a row created by the existing UC-70 CreateCareGroup flow (which does not populate `permission_json`) must never grant implicit access.

---

### FAM-UC84-TC-006 — permission_json missing category key → deny-by-default, 403 FAM-011

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.hasPermission()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-003 §Hệ quả`

**Preconditions:** `FX-004` — `permission_json = {"calendar":true}` (no `logs`/`alerts` keys)

**Test Steps:**
1. Act: `hasPermission(CG-001, ACC-001, LOGS)` and `hasPermission(CG-001, ACC-001, ALERTS)`
2. Assert: both return `false`; `hasPermission(..., CALENDAR)` returns `true`

**Expected Result (PASS):** `calendar=true`, `logs=false`, `alerts=false`.
**Expected Result (FAIL):** Any missing key resolves to `true`.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-007 — All flags true → 200 for every v1 category

**Severity:** `HIGH`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()` (all 3 v1 categories)
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-FAM-IMP-004 §16 Authorization Matrix`

**Preconditions:** `FX-001` (all-true combo)

**Test Steps:** Call `getSharedData` for `CALENDAR`, `LOGS`, `ALERTS` sequentially.
**Expected Result (PASS):** All three calls return 200-equivalent (`SharedDataResponse`, no exception).
**Expected Result (FAIL):** Any category throws `ForbiddenException`.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-008 — All flags false → 403 for every v1 category

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()` (all 3 v1 categories)
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-FAM-IMP-004 §16 Authorization Matrix`

**Preconditions:** `FX-002` (all-false combo)

**Test Steps:** Call `getSharedData` for `CALENDAR`, `LOGS`, `ALERTS`.
**Expected Result (PASS):** All three throw `ForbiddenException(FAM-011)`.
**Expected Result (FAIL):** Any category returns 200.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-009 — Mixed flags → per-category correct result (calendar=true, logs=false, alerts=true)

**Severity:** `CRITICAL`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `PERMISSION-FILTERED VISIBILITY table (this doc)` row "Mixed"

**Preconditions:** member with `{"calendar":true,"logs":false,"alerts":true}`

**Test Steps:**
1. Call `getSharedData` for `CALENDAR` → expect 200
2. Call `getSharedData` for `LOGS` → expect 403 FAM-011
3. Call `getSharedData` for `ALERTS` → expect 200

**Expected Result (PASS):** Exactly matches per-category expectation above.
**Expected Result (FAIL):** Any category's result flips (e.g., logs unexpectedly allowed).

**Current Status:** 🔴 Not written
**Implementation Note:** Also covers the "Logs-only" row (invert flags: `calendar=false,logs=true,alerts=false`) — implementer should parametrize this test over both mixed combos rather than duplicate boilerplate.

---

### FAM-UC84-TC-010 — PENDING member → 403 FAM-003 regardless of permission_json

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-002 (reused from UC-216)`

**Preconditions:** `FX-005` — `invitation_status=PENDING`, `permission_json.calendar=true`

**Test Steps:** Call `getSharedData(CG-001, PENDING_ACC, CALENDAR, 0, 20)`
**Expected Result (PASS):** `ForbiddenException(FAM-003)` — membership gate runs BEFORE permission gate (C1).
**Expected Result (FAIL):** 200 returned, or `FAM-011` thrown instead of `FAM-003` (wrong error code = wrong gate ordering).

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-011 — REVOKED member → 403 FAM-003

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-002 (reused from UC-216)`

**Preconditions:** `FX-006` — `invitation_status=REVOKED`

**Test Steps:** Call `getSharedData(CG-001, REVOKED_ACC, CALENDAR, 0, 20)`
**Expected Result (PASS):** `ForbiddenException(FAM-003)`.
**Expected Result (FAIL):** 200 returned.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-012 — Non-member → 403 FAM-003

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-002 (reused from UC-216)`

**Preconditions:** account has zero `care_group_members` rows for `CG-001`

**Test Steps:** Call `getSharedData(CG-001, RANDOM_ACC, CALENDAR, 0, 20)`
**Expected Result (PASS):** `ForbiddenException(FAM-003)`.
**Expected Result (FAIL):** Any non-403 result.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-013 — category=checklists → 400 FAM-012 (deferred, no data leak)

**Severity:** `HIGH`
**Feature Under Test:** `SharedDataController` request validation
**Test File:** `src/test/java/com/carebridge/backend/family/controller/SharedDataControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-FAM-005 §Decision (Phương án B)`, `CB-FAM-IMP-004 §10 Error Codes`

**Preconditions:** valid ACCEPTED member JWT

**Test Steps:** `GET /api/v1/care-groups/CG-001/shared-data?category=checklists`
**Expected Result (PASS):** HTTP 400, body `{"error":{"code":"FAM-012", ...}}`.
**Expected Result (FAIL):** 200 (checklist data somehow returned) or 403 (would imply permission check ran on an unsupported category — wrong error path per Logic Issue L1).

**Current Status:** 🔴 Not written
**Implementation Note:** Validation must occur before any DB/service call — this test must also assert `SharedDataServiceImpl.getSharedData` is never invoked (Mockito `verifyNoInteractions`).

---

### FAM-UC84-TC-014 — category=xyz (arbitrary invalid) → 400 FAM-012

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedDataController` request validation
**Test File:** `src/test/java/com/carebridge/backend/family/controller/SharedDataControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-FAM-IMP-004 §10 Error Codes`

**Test Steps:** `GET /api/v1/care-groups/CG-001/shared-data?category=xyz`
**Expected Result (PASS):** HTTP 400, code `FAM-012`.
**Expected Result (FAIL):** 500 (unhandled enum parse exception) — indicates missing input validation.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-015 — groupId not found → 404 FAM-010

**Severity:** `HIGH`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-FAM-IMP-004 §10 Error Codes`

**Test Steps:** Call `getSharedData(NONEXISTENT_GROUP, ACC-001, CALENDAR, 0, 20)`
**Expected Result (PASS):** `NotFoundException(FAM-010)`.
**Expected Result (FAIL):** `NullPointerException` or wrong error code.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-016 — Calendar permission granted, zero care_tasks rows → 200 empty (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `SRS UC-84 AF2 (empty state)`

**Preconditions:** `FX-001` member, but no `care_tasks` rows exist for the group

**Test Steps:** Call `getSharedData(CG-001, ACC-001, CALENDAR, 0, 20)`
**Expected Result (PASS):** `200`-equivalent response, `totalItems=0`, `items=[]` — NOT an exception, NOT `FAM-011`.
**Expected Result (FAIL):** Exception thrown, or `403` returned (would incorrectly conflate "no data" with "no permission").

**Current Status:** 🔴 Not written
**Implementation Note:** This distinguishes AF2 (empty data) from the permission-denied path (FAM-011) — the two must never be confused in the implementation.

---

### FAM-UC84-TC-017 — Response DTO never contains email/phone

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Legal:** `PDPA, BR-PRIVACY`
**Feature Under Test:** `SharedDataItemDto` mapping (all categories)
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `BR-PRIVACY (SRS L3269)`, `CB-FAM-IMP-004 §17.1 C5`

**Preconditions:** seeded account has `email="test@example.com"`, `phone="0901234567"`

**Test Steps:**
1. Call `getSharedData` for each v1 category with full permission
2. Serialize response to JSON

**Expected Result (PASS):** JSON does not contain `"@"`, `"phone"`, or `"email"` substrings; contains `"summary"`/`"title"` fields only.
**Expected Result (FAIL):** Any PII substring present in serialized response.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-018 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** Spring Security filter chain in front of `SharedDataController`
**Test File:** `src/test/java/com/carebridge/backend/family/controller/SharedDataControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `CB-FAM-IMP-004 §9.1 Auth Level: JWT Bearer`

**Test Steps:** `GET /api/v1/care-groups/CG-001/shared-data?category=calendar` with no `Authorization` header
**Expected Result (PASS):** HTTP 401.
**Expected Result (FAIL):** Any status other than 401 (e.g., 403 or 200 — indicates the endpoint is not actually behind authentication).

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-019 — care_tasks.status treated as opaque string (no DB-level enum assumption)

**Severity:** `LOW`
**Feature Under Test:** `SharedDataItemDto` mapping for CALENDAR category
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `V1__init_schema.sql` (no CHECK constraint on `care_tasks.status`) — Logic Issue L2

**Test Steps:**
1. Seed a `care_task` with an arbitrary non-standard status string (e.g., `"CUSTOM_STATE_X"`) — permitted since no DB CHECK exists
2. Call `getSharedData(..., CALENDAR, ...)`

**Expected Result (PASS):** The arbitrary status string is passed through in `SharedDataItemDto.status` unchanged, no exception thrown.
**Expected Result (FAIL):** Exception thrown assuming a fixed enum, or the value is silently dropped/mapped to a default.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-020 — ALERTS category join marker test (Open Item OI-4 tracking)

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedDataServiceImpl` ALERTS mapping path
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplAlertsOpenItemTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-FAM-005 §OI-4` — explicitly an Open Item, not a confirmed oracle

**Preconditions:** none beyond standard ALERTS fixtures

**Test Steps:**
1. Assert that the implementation's alerts-join predicate matches the documented assumption in TDS §5.2/§ADR-FAM-005 (join via `safety_alerts.recipient_user_id` intersected with care-group membership `user_id` set)
2. If the implementer changes the join predicate, this test must be updated in the same PR alongside a note that OI-4 has been resolved

**Expected Result (PASS):** Join predicate matches documented assumption, OR the PR updating it also updates this test + closes OI-4 in the TDS Open Items table.
**Expected Result (FAIL):** Silent divergence between code and documented assumption (test not updated) — this is a process-guard test, not a pure behavior test.

**Current Status:** 🔴 Not written
**Implementation Note:** This test exists to prevent OI-4 from being "resolved" implicitly in code without a corresponding TDS/Test-Spec update. Mark as `Open` until Principal Architect confirms the join.

---

### FAM-UC84-TC-021 — No FCM/notification service invoked during read

**Severity:** `MEDIUM`
**Feature Under Test:** `SharedDataServiceImpl.getSharedData()` — negative interaction test
**Test File:** `src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CB-FAM-IMP-004 §7.2 (FCM downstream/adjacent note)`, `§17.1 C6`

**Test Steps:**
1. Mock any injectable notification/FCM service bean
2. Call `getSharedData` for each v1 category

**Expected Result (PASS):** `Mockito.verifyNoInteractions(notificationServiceMock)` succeeds.
**Expected Result (FAIL):** Any interaction recorded — indicates scope creep into notification-sending (C6 violation).

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-022 — E2E happy path: calendar category

**Severity:** `HIGH`
**Feature Under Test:** Full flow: JWT auth → controller → service → policy → repository → DB
**Test File:** `src/test/java/com/carebridge/backend/family/SharedDataE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migrations applied automatically
- `FX-001`, `FX-007`, `FX-010` seeded

**Test Steps:**
1. `GET /api/v1/care-groups/CG-001/shared-data?category=calendar` with valid JWT
2. Assert HTTP 200
3. Assert DB state unchanged (read-only — no side effects)

**Expected Result (PASS):** 200, `items` matches seeded `care_tasks`.
**Expected Result (FAIL):** Non-200, or a write occurred (verify via row count comparison before/after).

**DB Assertion:**
```java
long taskCountBefore = careTaskRepository.count();
// ... perform API call ...
long taskCountAfter = careTaskRepository.count();
assertThat(taskCountAfter).isEqualTo(taskCountBefore); // read-only invariant
```

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-023 — E2E happy path: logs category

**Severity:** `HIGH`
**Feature Under Test:** Full flow for LOGS category
**Test File:** `src/test/java/com/carebridge/backend/family/SharedDataE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:** `FX-001`, `FX-008`, `FX-010` seeded

**Test Steps:** `GET /api/v1/care-groups/CG-001/shared-data?category=logs` with valid JWT
**Expected Result (PASS):** 200, item reflects seeded `baby_daily_logs` row.
**Expected Result (FAIL):** Non-200 or empty when data exists.

**Current Status:** 🔴 Not written

---

### FAM-UC84-TC-024 — E2E: alerts category (conditional on OI-4 resolution)

**Severity:** `MEDIUM`
**Feature Under Test:** Full flow for ALERTS category
**Test File:** `src/test/java/com/carebridge/backend/family/SharedDataE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`

**Preconditions:** `FX-001`, `FX-009`, `FX-010` seeded; **Open Item OI-4 must be at least provisionally accepted** for this test to be meaningful — if OI-4 is unresolved at implementation time per TDS §11.1, this test may be marked `Suspended` rather than `Not written` (see §6 Suspension Criteria)

**Test Steps:** `GET /api/v1/care-groups/CG-001/shared-data?category=alerts` with valid JWT
**Expected Result (PASS):** 200, item reflects seeded `safety_alerts` row scoped to the care group's members.
**Expected Result (FAIL):** Non-200, or cross-group data leak (alert from a different group's member appears).

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM-UC84-TC-001` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-002` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-003` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | Depends on OI-4 |
| `FAM-UC84-TC-004` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-005` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | CRITICAL regression guard |
| `FAM-UC84-TC-006` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-007` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-008` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-009` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | Parametrize both mixed combos |
| `FAM-UC84-TC-010` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-011` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-012` | `CareGroupAccessPolicyTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-013` | `SharedDataControllerTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-014` | `SharedDataControllerTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-015` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-016` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-017` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-018` | `SharedDataControllerTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-019` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-020` | `SharedDataServiceImplAlertsOpenItemTest.java` | `[ ]` | `[ ]` | Process-guard, tied to OI-4 |
| `FAM-UC84-TC-021` | `SharedDataServiceImplTest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-022` | `SharedDataE2ETest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-023` | `SharedDataE2ETest.java` | `[ ]` | `[ ]` | |
| `FAM-UC84-TC-024` | `SharedDataE2ETest.java` | `[ ]` | `[ ]` | Conditional — see Suspension Criteria |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (MUST throw)
@Service
public class SharedDataServiceImpl implements ISharedDataService {

    @Override
    public SharedDataResponse getSharedData(UUID groupId, UUID accountId, SharedDataCategory category, int page, int size) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class CareGroupAccessPolicyStub {
    public boolean hasPermission(UUID groupId, UUID accountId, PermissionCategory category) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM-UC84-TC-001` … `FAM-UC84-TC-024` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` (to be created at implementation time)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-FAM-IMP-004` reviewed and approved (currently `Draft`)
- [ ] Logic Issues (§2) confirmed with Principal Architect, especially L1 (checklists gap), L4 (alerts join, OI-4)
- [ ] No Flyway migration required for v1 scope — confirmed in TDS §5.2
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green (no skips), including all 24 test cases above
- [ ] `./mvnw verify` — integration tests green (Testcontainers) for calendar + logs categories
- [ ] Test coverage ≥ 80% lines for `SharedDataServiceImpl` and `CareGroupAccessPolicy.hasPermission()`
- [ ] No business logic in `SharedDataController` (validation + mapping only)
- [ ] No PII/secret appears in plaintext logs
- [ ] Every permission-flag combination in the "PERMISSION-FILTERED VISIBILITY" table has a passing test

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL with empty/throw stub before implementation
- [ ] **Contract Existence** — every injected class exists in codebase (`./mvnw compile` clean)
- [ ] **Props Isolation** — no shared mutable state between tests (fresh instances via `SharedDataTestFactory`)
- [ ] **Oracle Source** — every expected value in an assert cites a BR/AC/ADR or is marked Open (see `FAM-UC84-TC-003`, `FAM-UC84-TC-020` — both explicitly marked as encoding an unconfirmed/Open assumption)

### Suspension Criteria

- Open Item **OI-4** (alerts join predicate) unresolved at implementation start → per TDS §11.1, ship `CALENDAR` + `LOGS` categories first; `FAM-UC84-TC-003`, `FAM-UC84-TC-020`, `FAM-UC84-TC-024` (ALERTS-specific) may be marked `Suspended` rather than `Not written` until Principal Architect sign-off lands. All other test cases proceed independently of OI-4.
- Open Item **OI-2** (`permission_json` shape) is later confirmed by UC-72 with a DIFFERENT shape than assumed here → all tests referencing `permission_json` fixtures (`FX-001` through `FX-004`, and `FAM-UC84-TC-004` through `FAM-UC84-TC-009`) must be revised before Exit Criteria can be met.
- CI pipeline broken by unrelated changes.

---

## 7. Rollback Plan

```bash
# No DB migration exists for this module — rollback is code/test-only.

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/SharedDataServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/SharedDataController.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/CareGroupAccessPolicy.java

# Revert test files
git checkout -- src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java
git checkout -- src/test/java/com/carebridge/backend/family/controller/SharedDataControllerTest.java
git checkout -- src/test/java/com/carebridge/backend/family/SharedDataE2ETest.java

# Feature remains OPEN → keep entry in sprint tracking / PHASE_GAP_ANALYSIS.md if applicable
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume checklist join hoặc alerts join không có ADR xác nhận, mà không được flag Open | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (e.g., permission check trong controller thay vì service/policy) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (e.g., a repository method not declared in TDS §8.2) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Phát hiện AP đã biết trước, được flag chủ động trong spec này (không phải lỗi, mà là Open Item minh bạch) → xem bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-003` | `FAM-UC84-TC-003`, `FAM-UC84-TC-020`, `FAM-UC84-TC-024` | Alerts category join is an ASSUMED, unconfirmed design (Open Item OI-4) — test cases explicitly flag this rather than silently treating it as confirmed | Await Principal Architect review; TC-020 exists specifically to prevent silent divergence | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Companion to TDS `CB-FAM-IMP-004` (Status: Draft).*
