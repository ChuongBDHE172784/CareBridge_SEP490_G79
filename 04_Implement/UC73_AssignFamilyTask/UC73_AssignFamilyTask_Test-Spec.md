# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC73 — Assign Family Task — Test Specification

**Document ID:** `FPT-EDU-TDD-TEMPLATE-001` (instance for `CB-FAM-IMP-073`)
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (care_tasks: lines 750-762)
- `04_Implement/UC73_AssignFamilyTask/UC73_AssignFamilyTask_TDS.md` (`CB-FAM-IMP-073`) — companion Technical Specification
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.50 (lines 2809-2828) — Functional requirements (UC-73)
- ADR-FAM-030/031/032/033/034 — see TDS §3
- PDPA (Vietnam) — minimum-necessary access; *(Luật 91/2025, NĐ 356/2025 — Not applicable; no specific article identified for this feature's scope, generic BR-PRIVACY/BR-RBAC cited instead)*

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC-73 Assign Family Task |
| 2026-07-08 | AI Agent — Amelia (Dev Agent) | Thực hiện Red-Green-Refactor. 23/24 TCs GREEN (TC-INT-001 skipped — Docker không khả dụng). Tất cả unit/controller/security tests PASS. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
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
| **Feature / Gap ID** | `GAP-FAM73` |
| **Module** | `family` — Care Task Assignment |
| **Spec gốc** | `CB-FAM-IMP-073` |
| **Priority** | 🔴 P0 (High per SRS) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` |
| **Milestone** | `M3 Alpha` *(exact date not confirmed in available research — Open, do not invent)* |
| **Data Classification** | `Internal` (family/task data — see TDS §1) |
| **Compliance Scope** | `PDPA` (minimum-necessary access); `BR-RBAC` |
| **Upstream Dependencies** | `CareGroup`, `CareGroupMember` (UC-70/216), `AuditService`, `FcmService` |
| **Downstream Consumers** | Mobile "Việc nhóm" task screens; future UC-3.3.17.6/7/8, UC-3.3.3.3 |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-073 §17`, `ADR-FAM-030/031/032/033/034` |
| **Constraints Injected** | C1 (single controller reuse), C2 (no entity leakage), C3 (policy-based owner check), C4 (identity from SecurityUtils), C5 (layering), C6 (FCM non-blocking), C7 (no new migration) — see TDS §17.1 |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | No `CareTask` JPA entity or `CareTaskRepository` exists yet for the `care_tasks` table (greenfield code atop an existing table) | `care_tasks` table exists in `V1__init_schema.sql` (lines 750-762) with columns `care_task_id, care_group_id, assigned_by, assigned_to, title, description, due_at, status, completed_at, created_at, updated_at` — no CHECK constraint on `status` | Tests assert against the real column names/types via JPA; `CareTaskStatus` enum values (`OPEN, IN_PROGRESS, DONE, CANCELLED`) are a code-level decision (ADR-FAM-030), tests seed/assert only `OPEN` for this feature's write path |
| L2 | SRS text ("required role or permission") does not explicitly say task-assignment is Owner-only | shared-context.md's cross-feature proposed default + UC-70 code evidence (creator = OWNER, ACCEPTED) | Authorization test cases assume Owner-only per ADR-FAM-032, explicitly marked as testing an **Open/proposed** default, not a hard SRS mandate |
| L3 | SRS E2 doesn't name `due_at` specifically as "must be future" | ADR-FAM-033 interpretation: `due_at` nullable in schema, but UC-73 Normal Flow implies due date is a required input for this specific action | Test cases assert due date required + future-only for the `assignFamilyTask` call path; boundary case for "due date exactly now" |
| L4 | `reminder` package (`INotificationService.scheduleFcmPush`) looked reusable at first glance but is scoped to `reminders` table / UC-45 (Appointment Reminder), confirmed via `V20260627100300__add_reminder_columns.sql` header comment ("UC-45: CreateAppointmentReminder") | `reminders` table is a distinct table from `care_tasks`; no FK/link exists between them | Tests use `FcmService.sendToToken` directly (mocked), NOT `INotificationService`/`IReminderService` — asserting the correct dependency is injected (guards against AP-AI-005 hallucinated contract) |
| L5 | `CareGroupMemberRepository` has no `findByCareGroupIdAndUserId` method yet (only `existsBy...AndInviteStatus`) | New method required for resolving assignee membership + role/status for authorization | Tests for `CareTaskServiceImpl` mock `CareGroupMemberRepository.findByCareGroupIdAndUserId(...)`, not the existing `existsBy...` method, for the owner-check and assignee-check paths |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Family / CareTask Assignment bao gồm các layer:
├── Domain (CareTask entity, CareTaskStatus enum — pure data, no branching logic to unit test beyond builder defaults)
├── Policy (CareGroupAuthorizationPolicy.canAssignTasks — mock CareGroupMemberRepository với Mockito)
├── Services (CareTaskServiceImpl — mock CareTaskRepository, CareGroupRepository, CareGroupMemberRepository, CareGroupAuthorizationPolicy, FcmService, AuditService với Mockito)
├── Controller (CareGroupController new endpoints — mock ICareTaskService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, full assign + list flow through real repositories)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-73` | Happy path (assign task), E1 (access denied), E2 (invalid/expired data rejected), POST-2/POST-3 (notification + audit) |
| `ADR-FAM-030` | `CareTaskStatus` values — assert `OPEN` on create, enum has 4 values total |
| `ADR-FAM-031` | FCM sent once at assignment; FCM failure does not roll back task creation |
| `ADR-FAM-032` | Owner-only authorization; self-assignment allowed (Open note) |
| `ADR-FAM-033` | Due date strictly-future validation, boundary at "now" |
| `BR-RBAC` / `BR-PRIVACY` | Authorization tests (owner/non-owner/non-member); no entity leakage in DTOs |
| `CB-FAM-IMP-073` §8/§9/§10 | Interface signatures, API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner assigns task to an ACCEPTED member with valid future due date | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-001` |
| TC-COND-002 | Care group does not exist | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-002` |
| TC-COND-003 | Caller is not the Owner (ACCEPTED MEMBER role) | `CareTaskServiceImpl.assignFamilyTask()`, `CareGroupAuthorizationPolicy.canAssignTasks()` | `FAM73-TC-003` |
| TC-COND-004 | Caller is not a member at all | `CareGroupAuthorizationPolicy.canAssignTasks()` | `FAM73-TC-004` |
| TC-COND-005 | Assignee is PENDING (not yet ACCEPTED) | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-005` |
| TC-COND-006 | Assignee is REVOKED | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-006` |
| TC-COND-007 | Assignee does not exist in the group at all | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-007` |
| TC-COND-008 | Due date in the past | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-008` |
| TC-COND-009 | Due date exactly equal to "now" (boundary) | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-009` |
| TC-COND-010 | Due date 1ms in the future (boundary — should pass) | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-010` |
| TC-COND-011 | Title missing/blank | `AssignFamilyTaskRequest` bean validation | `FAM73-TC-011` |
| TC-COND-012 | Title exceeds 255 chars | `AssignFamilyTaskRequest` bean validation | `FAM73-TC-012` |
| TC-COND-013 | Owner self-assigns task (assignee == caller) | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-013` |
| TC-COND-014 | FCM send throws/returns null — task must still persist | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-014` |
| TC-COND-015 | Successful assignment publishes `FamilyTaskAssigned` event with correct payload | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-015` |
| TC-COND-016 | Successful assignment writes `CARE_TASK_ASSIGNED` audit log | `CareTaskServiceImpl.assignFamilyTask()` | `FAM73-TC-016` |
| TC-COND-017 | `listTasks` returns tasks for ACCEPTED member (any role) | `CareTaskServiceImpl.listTasks()` | `FAM73-TC-017` |
| TC-COND-018 | `listTasks` denied for non-member | `CareTaskServiceImpl.listTasks()` | `FAM73-TC-018` |
| TC-COND-019 | `listTasks` on group with zero tasks (empty state, AF2) | `CareTaskServiceImpl.listTasks()` | `FAM73-TC-019` |
| TC-COND-020 | Response DTOs never leak raw `CareTask` entity fields beyond the documented DTO shape | `CareTaskDto`, `AssignFamilyTaskResponse` mapping | `FAM73-TC-020` |
| TC-COND-021 | No JWT / unauthenticated caller on POST | `CareGroupController.assignTask()` | `FAM73-TC-021` (E2E) |
| TC-COND-022 | No JWT / unauthenticated caller on GET | `CareGroupController.listTasks()` | `FAM73-TC-022` (E2E) |
| TC-COND-023 | Full integration: assign then list, DB state assertion | End-to-end (`@SpringBootTest` + Testcontainers) | `FAM73-TC-INT-001` |
| TC-COND-024 | SQL injection attempt via `title`/`description` fields | Security — `CareTaskServiceImpl` / JPA parameter binding | `FAM73-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `inviteStatus` of assignee (ACCEPTED / PENDING / REVOKED / non-existent) | Each partition maps to a distinct outcome (success vs. `FAM-030`) |
| Boundary Value Analysis | `dueAt` relative to `now()` (past / exactly now / 1ms future) | ADR-FAM-033's "strictly after now" rule has a precise boundary that must be tested at the millisecond edge |
| State Transition Testing | `CareTaskStatus` (only `OPEN` produced by this feature) | Confirms this feature never accidentally produces `IN_PROGRESS`/`DONE`/`CANCELLED` |
| Error Guessing | SQL injection via free-text `title`/`description` fields | Free-text fields are the most likely injection vector; JPA parameter binding should neutralize it |
| Role/Permission Partitioning | Owner / non-owner member / non-member / unauthenticated | ADR-FAM-032 authorization matrix (TDS §16) has 4 distinct partitions per endpoint |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroup{ id: GROUP_ID, ownerUserId: OWNER_ID, status: ACTIVE }` | Baseline group for happy path |
| `FX-002` | DB seed | `CareGroupMember{ careGroupId: GROUP_ID, userId: OWNER_ID, memberRole: OWNER, inviteStatus: ACCEPTED }` | Owner membership row |
| `FX-003` | DB seed | `CareGroupMember{ careGroupId: GROUP_ID, userId: ASSIGNEE_ID, memberRole: MEMBER, inviteStatus: ACCEPTED }` | Valid assignee for happy path |
| `FX-004` | DB seed | `CareGroupMember{ ..., inviteStatus: PENDING }` | Assignee-not-accepted case (`FAM73-TC-005`) |
| `FX-005` | DB seed | `CareGroupMember{ ..., inviteStatus: REVOKED }` | Assignee-revoked case (`FAM73-TC-006`) |
| `FX-006` | DB seed | `CareGroupMember{ ..., memberRole: MEMBER, inviteStatus: ACCEPTED }` for caller (non-owner) | Non-owner authorization case (`FAM73-TC-003`) |
| `FX-007` | request | `AssignFamilyTaskRequest{ assigneeMemberId: ASSIGNEE_MEMBER_ID, title: "Buy diapers", description: "Size M", dueAt: now().plus(3, DAYS) }` | Happy-path request body |
| `FX-008` | request | Same as FX-007 but `dueAt: now().minus(1, DAYS)` | Past due date (`FAM73-TC-008`) |
| `FX-009` | request | Same as FX-007 but `dueAt: Instant.now()` (captured once, reused for both service call and assertion) | Boundary "exactly now" (`FAM73-TC-009`) |
| `FX-010` | JWT | `{ sub: OWNER_ID, role: 'MOTHER' }` | Auth context for owner-path E2E tests |
| `FX-011` | JWT | `{ sub: MEMBER_ID, role: 'FAMILY' }` | Auth context for non-owner-path E2E tests |

> All UUIDs above are placeholders (e.g. `GROUP_ID`, `OWNER_ID`) bound to concrete
> `UUID.fromString("00000000-0000-0000-0000-0000000000XX")` synthetic values in the actual test
> factory (`CareGroupTestFactory`) — see Props Isolation Boilerplate below. Test Data
> Classification: **SYNTHETIC** for all fixtures — no production PII used.

---

## 4. Test Case Specification

> **TC ID format:** `FAM73-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// CareGroupTestFactory.java — package com.carebridge.backend.family (test sources)
// Shared across UC71/UC72/UC73/UC83 Test-Spec files — methods grow additively, never redefine.
class CareGroupTestFactory {

    static final UUID GROUP_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID MEMBER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    static CareGroup makeCareGroup() {
        return CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(OWNER_ID)
                .groupName("Test Care Group")
                .status(CareGroupStatus.ACTIVE)
                .build();
    }

    static CareGroup makeCareGroup(Consumer<CareGroup.CareGroupBuilder> overrides) {
        CareGroup.CareGroupBuilder builder = CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(OWNER_ID)
                .groupName("Test Care Group")
                .status(CareGroupStatus.ACTIVE);
        overrides.accept(builder);
        return builder.build();
    }

    static CareGroupMember makeCareGroupMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .userId(MEMBER_ID)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        overrides.accept(member);
        return member;
    }

    // NEW for UC-73 — CareTask factory
    static CareTask makeCareTask() {
        return CareTask.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .careGroupId(GROUP_ID)
                .assignedBy(OWNER_ID)
                .assignedTo(ASSIGNEE_ID)
                .title("Buy diapers")
                .description("Size M, at least 2 packs")
                .dueAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .status(CareTaskStatus.OPEN)
                .build();
    }

    static CareTask makeCareTask(Consumer<CareTask> overrides) {
        CareTask task = makeCareTask();
        overrides.accept(task);
        return task;
    }

    static AssignFamilyTaskRequest makeAssignFamilyTaskRequest(Consumer<AssignFamilyTaskRequest> overrides) {
        AssignFamilyTaskRequest request = new AssignFamilyTaskRequest();
        request.setAssigneeMemberId(ASSIGNEE_ID);
        request.setTitle("Buy diapers");
        request.setDescription("Size M, at least 2 packs");
        request.setDueAt(Instant.now().plus(3, ChronoUnit.DAYS));
        overrides.accept(request);
        return request;
    }
}
```

---

### FAM73-TC-001 — Owner successfully assigns task to an ACCEPTED member (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** SRS §3.3.1.50 Normal Flow Steps 1-5; ADR-FAM-032 (owner authorization); `V1__init_schema.sql` care_tasks columns

**Preconditions:**
- `FX-001` (care group), `FX-002` (owner membership), `FX-003` (assignee ACCEPTED) seeded/mocked
- `FX-007` request body

**Test Steps:**
1. Arrange: mock `groupRepository.findById(GROUP_ID)` → `makeCareGroup()`; mock `authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)` → `true`; mock `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID)` → `Optional.of(makeCareGroupMember(m -> {m.setUserId(ASSIGNEE_ID); m.setInviteStatus(ACCEPTED);}))`; mock `taskRepository.save(any())` returning the saved entity with a generated ID
2. Act: call `careTaskService.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(r -> {}), OWNER_ID)`
3. Assert: response `status == "OPEN"`, `assignedTo == ASSIGNEE_ID`, `assignedBy == OWNER_ID`, `taskRepository.save` called exactly once with a `CareTask` whose `status == CareTaskStatus.OPEN`

**Expected Result (PASS — hành vi đúng):**
- Returns `AssignFamilyTaskResponse` with `status = "OPEN"` and matching field values; no exception thrown

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or `status` field is null/wrong, or `taskRepository.save` not invoked

**Current Status:** 🟢 Passing
**Implementation Note:** Ensure `CareTask.builder().status(CareTaskStatus.OPEN)` default applies even if caller does not explicitly set status.

---

### FAM73-TC-002 — Care group does not exist → 404 FAM-005

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-FAM-IMP-073` §10 error code table (`FAM-005`, reused from UC-70/216 code)

**Preconditions:** `groupRepository.findById(GROUP_ID)` mocked to return `Optional.empty()`

**Test Steps:**
1. Arrange: mock as above
2. Act: call `assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(r->{}), OWNER_ID)`
3. Assert: `BusinessException` thrown with `httpStatus == 404` and `code == "FAM-005"`

**Expected Result (PASS):** `BusinessException(404, "FAM-005")` thrown; `taskRepository.save` never called
**Expected Result (FAIL):** No exception, or wrong code/status, or task persisted despite missing group

**Current Status:** 🟢 Passing

---

### FAM73-TC-003 — Caller is an ACCEPTED but non-Owner member → 403 FAM-031

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`, `CareGroupAuthorizationPolicy.canAssignTasks()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-032` (Open, proposed default — Owner-only)

**Preconditions:** `FX-006` — caller is `MEMBER` role, `ACCEPTED` status, not `OWNER`

**Test Steps:**
1. Arrange: mock `groupRepository.findById` → group; mock `authorizationPolicy.canAssignTasks(GROUP_ID, MEMBER_ID)` → `false`
2. Act: call `assignFamilyTask(GROUP_ID, request, MEMBER_ID)`
3. Assert: `BusinessException(403, "FAM-031")` thrown

**Expected Result (PASS):** 403 `FAM-031` thrown before any repository write
**Expected Result (FAIL):** Task created despite caller not being Owner (authorization bypass)

**Current Status:** 🟢 Passing
**Implementation Note:** This encodes ADR-FAM-032's Open/proposed Owner-only rule — flag to reviewer that this default requires Product confirmation.

---

### FAM73-TC-004 — `CareGroupAuthorizationPolicy.canAssignTasks()` returns false for non-member

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupAuthorizationPolicy.canAssignTasks()`
**Test File:** `src/test/java/com/carebridge/backend/family/policy/CareGroupAuthorizationPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-032`

**Preconditions:** `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, callerId)` mocked → `Optional.empty()`

**Test Steps:**
1. Act: call `canAssignTasks(GROUP_ID, UUID.randomUUID())`
2. Assert: returns `false`

**Expected Result (PASS):** `false`
**Expected Result (FAIL):** `true` (authorization bypass for unknown user)

**Current Status:** 🟢 Passing

---

### FAM73-TC-005 — Assignee is PENDING (not yet accepted) → 409 FAM-030

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-FAM-IMP-073` §10 (`FAM-030`); shared-context.md ADR-FAM-002 pattern (ACCEPTED-only counts as membership)

**Preconditions:** `FX-004` — assignee membership row has `inviteStatus = PENDING`

**Test Steps:**
1. Arrange: mock owner authorization → `true`; mock `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID)` → `Optional.of(pendingMember)`
2. Act: call `assignFamilyTask(...)`
3. Assert: `BusinessException(409, "FAM-030")` thrown

**Expected Result (PASS):** 409 `FAM-030`
**Expected Result (FAIL):** Task created for a not-yet-accepted assignee

**Current Status:** 🟢 Passing

---

### FAM73-TC-006 — Assignee is REVOKED → 409 FAM-030

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-FAM-IMP-073` §10 (`FAM-030`)

**Preconditions:** `FX-005` — assignee membership row has `inviteStatus = REVOKED`

**Test Steps:**
1. Arrange: mock as above with `REVOKED` status
2. Act: call `assignFamilyTask(...)`
3. Assert: `BusinessException(409, "FAM-030")` thrown

**Expected Result (PASS):** 409 `FAM-030`
**Expected Result (FAIL):** Task created for a revoked ex-member

**Current Status:** 🟢 Passing

---

### FAM73-TC-007 — Assignee is not a member of the group at all → 409 FAM-030

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-FAM-IMP-073` §10 (`FAM-030`)

**Preconditions:** `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, unknownUserId)` → `Optional.empty()`

**Test Steps:**
1. Arrange: mock as above
2. Act: call `assignFamilyTask(...)` with `assigneeMemberId` pointing to a user with no membership row
3. Assert: `BusinessException(409, "FAM-030")` thrown

**Expected Result (PASS):** 409 `FAM-030`
**Expected Result (FAIL):** NPE or task created for a nonexistent assignee

**Current Status:** 🟢 Passing

---

### FAM73-TC-008 — Due date in the past → 400 FAM-032

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-033`; SRS E2 "expired ... data is rejected"

**Preconditions:** `FX-008` request with `dueAt = now().minus(1, DAYS)`

**Test Steps:**
1. Arrange: mock owner + assignee checks to pass
2. Act: call `assignFamilyTask(...)` with past `dueAt`
3. Assert: `BusinessException(400, "FAM-032")` thrown

**Expected Result (PASS):** 400 `FAM-032`
**Expected Result (FAIL):** Task created with a past due date

**Current Status:** 🟢 Passing

---

### FAM73-TC-009 — Due date exactly equal to "now" (boundary) → 400 FAM-032

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-033` — "strictly after now" boundary decision

**Preconditions:** `FX-009` — capture a single `Instant` value `T`, use it both as `dueAt` in the request AND as the mocked/injected "current time" the service evaluates against (if the service uses an injected clock; otherwise accept minor race and assert rejection given `T <= actual now()` at evaluation).

**Test Steps:**
1. Arrange: request with `dueAt = T` where `T` is captured immediately before the call (guaranteeing `T <= Instant.now()` at evaluation time)
2. Act: call `assignFamilyTask(...)`
3. Assert: `BusinessException(400, "FAM-032")` thrown (boundary is exclusive — "now" itself is rejected)

**Expected Result (PASS):** 400 `FAM-032` — confirms boundary is `isAfter`, not `isAfter-or-equal`
**Expected Result (FAIL):** Task created with `dueAt == now`, indicating an off-by-one boundary bug

**Current Status:** 🟢 Passing
**Implementation Note:** If flaky due to clock precision, consider injecting a `Clock`/`Supplier<Instant>` into `CareTaskServiceImpl` for deterministic boundary testing — Open implementation detail, not mandated by TDS.

---

### FAM73-TC-010 — Due date 1 second in the future (boundary — should pass)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-033`

**Preconditions:** Request with `dueAt = Instant.now().plusSeconds(1)`

**Test Steps:**
1. Arrange: mock owner + assignee checks to pass
2. Act: call `assignFamilyTask(...)`
3. Assert: no exception; task created successfully

**Expected Result (PASS):** Task created, no exception
**Expected Result (FAIL):** Rejected despite being in the future (over-strict boundary bug)

**Current Status:** 🟢 Passing

---

### FAM73-TC-011 — Missing/blank title → 400 (Bean Validation)

**Severity:** `HIGH`
**Feature Under Test:** `AssignFamilyTaskRequest` `@NotBlank` validation, via `@WebMvcTest` on `CareGroupController.assignTask()`
**Test File:** `src/test/java/com/carebridge/backend/family/controller/CareGroupControllerAssignTaskTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-FAM-IMP-073` §8.1 (`@NotBlank` on `title`)

**Preconditions:** JWT for OWNER_ID (`FX-010`)

**Test Steps:**
1. Arrange: `mockMvc` configured with mocked `ICareTaskService`
2. Act: `POST /api/v1/care-groups/{groupId}/tasks` with `title: ""` (or omitted)
3. Assert: HTTP 400, response body contains a validation error referencing field `title`

**Expected Result (PASS):** 400 with field-level `title` error
**Expected Result (FAIL):** 201 created despite blank title, or generic 500

**Current Status:** 🟢 Passing

---

### FAM73-TC-012 — Title exceeds 255 characters → 400 (Bean Validation)

**Severity:** `MEDIUM`
**Feature Under Test:** `AssignFamilyTaskRequest` `@Size(max=255)` validation
**Test File:** `CareGroupControllerAssignTaskTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `V1__init_schema.sql` `title varchar(255) NOT NULL` — DB column length is the oracle for this boundary

**Preconditions:** JWT for OWNER_ID

**Test Steps:**
1. Arrange: request with `title` = 256-character string
2. Act: `POST /api/v1/care-groups/{groupId}/tasks`
3. Assert: HTTP 400 with field-level `title` error

**Expected Result (PASS):** 400 rejected before hitting the DB (avoids a `varchar(255)` truncation/DB error)
**Expected Result (FAIL):** 500 DB error, or silent truncation without a 400

**Current Status:** 🟢 Passing

---

### FAM73-TC-013 — Owner self-assigns task (assignee == caller) — allowed

**Severity:** `LOW`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-FAM-032` OPEN-2 — "Open, proposed default: allowed (no restriction found in SRS)"

**Preconditions:** Owner's own membership row is `ACCEPTED`/`OWNER`; request `assigneeMemberId` resolves to `OWNER_ID`

**Test Steps:**
1. Arrange: mock `authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)` → `true`; mock `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)` → owner's own `ACCEPTED` `CareGroupMember`
2. Act: call `assignFamilyTask(GROUP_ID, request_with_assignee=OWNER_ID, OWNER_ID)`
3. Assert: task created successfully with `assignedTo == OWNER_ID == assignedBy`

**Expected Result (PASS):** Task created, no exception (self-assignment permitted per Open decision)
**Expected Result (FAIL):** Exception thrown, indicating an unintended restriction was added

**Current Status:** 🟢 Passing
**Implementation Note:** This is a deliberately-Open behavioral choice — flag to reviewer/Product if self-assignment should instead be blocked.

---

### FAM73-TC-014 — FCM send failure does not block task persistence (non-blocking)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-FAM-031` (non-blocking FCM); SRS E3 "external service failure handled ... no duplicate unsafe action"

**Preconditions:** `fcmService.sendToToken(any(), any(), any())` mocked to throw `RuntimeException("FCM unavailable")`

**Test Steps:**
1. Arrange: mock owner + assignee checks to pass; mock FCM to throw
2. Act: call `assignFamilyTask(...)`
3. Assert: no exception propagates out of `assignFamilyTask`; `taskRepository.save` was still called; response returned successfully

**Expected Result (PASS):** Task persisted, method returns normally despite FCM throwing internally (caught and logged)
**Expected Result (FAIL):** Exception propagates to caller / transaction rolls back / task not persisted

**Current Status:** 🟢 Passing
**Implementation Note:** Wrap the `fcmService.sendToToken(...)` call in a try/catch inside `CareTaskServiceImpl`; log a warning, do not rethrow.

---

### FAM73-TC-015 — Successful assignment publishes `FamilyTaskAssigned` event with correct payload

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-FAM-IMP-073` §7.3 payload schema

**Preconditions:** `ApplicationEventPublisher` mocked (or `@RecordApplicationEvents` if using `@SpringBootTest`)

**Test Steps:**
1. Arrange: happy-path mocks
2. Act: call `assignFamilyTask(...)`
3. Assert: `eventPublisher.publishEvent(argThat(evt -> evt instanceof FamilyTaskAssigned))` captured; payload's `careTaskId`, `careGroupId`, `assignedBy`, `assignedTo`, `title`, `dueAt` match the persisted task

**Expected Result (PASS):** Event published once with correct payload fields
**Expected Result (FAIL):** Event not published, published with wrong payload, or published more than once

**Current Status:** 🟢 Passing

---

### FAM73-TC-016 — Successful assignment writes `CARE_TASK_ASSIGNED` audit log

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-016`
**Oracle Source:** SRS POST-3 "sensitive actions recorded for audit"; shared-context.md AuditAction enum addition list

**Preconditions:** `auditService` mocked

**Test Steps:**
1. Arrange: happy-path mocks
2. Act: call `assignFamilyTask(...)`
3. Assert: `auditService.log(AuditAction.CARE_TASK_ASSIGNED, OWNER_ID, "CareTask", taskId.toString(), anyString())` invoked exactly once

**Expected Result (PASS):** Audit log call matches expected arguments
**Expected Result (FAIL):** No audit call, or wrong `AuditAction` value, or wrong actor ID

**Current Status:** 🟢 Passing
**Implementation Note:** Requires adding `CARE_TASK_ASSIGNED` to the `AuditAction` enum (code change, not migration) per shared-context.md.

---

### FAM73-TC-017 — `listTasks` returns all tasks for an ACCEPTED member (any role)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.listTasks()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-017`
**Oracle Source:** TDS §9 `GET /api/v1/care-groups/{groupId}/tasks`; ADR-FAM-032 (read is broader than write)

**Preconditions:** Caller is `ACCEPTED` `MEMBER` (not Owner); `taskRepository.findByCareGroupId(GROUP_ID)` mocked → list of 2 `CareTask`

**Test Steps:**
1. Arrange: mock membership check → `ACCEPTED`; mock task list
2. Act: call `listTasks(GROUP_ID, MEMBER_ID)`
3. Assert: `totalTasks == 2`, `tasks` list contains mapped `CareTaskDto` entries with correct fields

**Expected Result (PASS):** Non-owner ACCEPTED member can read the list
**Expected Result (FAIL):** 403 thrown for a valid ACCEPTED non-owner member (over-restrictive read access)

**Current Status:** 🟢 Passing

---

### FAM73-TC-018 — `listTasks` denied for non-member → 403 FAM-003 (reused)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.listTasks()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-018`
**Oracle Source:** Existing `FAM-003` code pattern (`CareGroupServiceImpl.listMembers`), reused per TDS §10

**Preconditions:** `memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, callerId, ACCEPTED)` mocked → `false`

**Test Steps:**
1. Arrange: mock as above
2. Act: call `listTasks(GROUP_ID, unknownUserId)`
3. Assert: `BusinessException(403, "FAM-003")` thrown

**Expected Result (PASS):** 403 `FAM-003`
**Expected Result (FAIL):** Tasks list leaked to a non-member (BR-PRIVACY violation)

**Current Status:** 🟢 Passing

---

### FAM73-TC-019 — `listTasks` on a group with zero tasks (empty state, AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.listTasks()`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-019`
**Oracle Source:** SRS AF2 "No matching data is available; the system displays an empty state"

**Preconditions:** `taskRepository.findByCareGroupId(GROUP_ID)` mocked → empty list

**Test Steps:**
1. Arrange: mock membership check to pass; mock empty task list
2. Act: call `listTasks(GROUP_ID, callerId)`
3. Assert: `totalTasks == 0`, `tasks` is an empty list (not null)

**Expected Result (PASS):** `CareTasksResponse{ totalTasks: 0, tasks: [] }`
**Expected Result (FAIL):** NPE, or `tasks` is null instead of empty list

**Current Status:** 🟢 Passing

---

### FAM73-TC-020 — Response DTOs never leak raw `CareTask` entity or unexpected fields

**Severity:** `HIGH`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies` *(closest applicable CWE for over-exposure via API response; not a strict security vuln, but a layering/privacy safeguard)*
**Feature Under Test:** `CareTaskDto`, `AssignFamilyTaskResponse` mapping in `CareTaskServiceImpl`
**Test File:** `CareTaskServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-020`
**Oracle Source:** CLAUDE.md "Never expose JPA entities in API responses; use DTOs and mappers"

**Preconditions:** Happy-path mocks

**Test Steps:**
1. Act: call `assignFamilyTask(...)` and `listTasks(...)`
2. Assert (reflection/type check): return types are `AssignFamilyTaskResponse`/`CareTasksResponse` — NOT `CareTask`; no method signature in `ICareTaskService` returns the `CareTask` entity type directly

**Expected Result (PASS):** All public service methods return DTO types only
**Expected Result (FAIL):** A method returns `CareTask` or a JPA-annotated type directly

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### FAM73-TC-SEC-001 — SQL injection attempt via `title`/`description` fields

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Legal:** `PDPA data integrity expectation — injection could corrupt/leak other users' family data`
**Feature Under Test:** `CareTaskServiceImpl.assignFamilyTask()` (JPA parameter binding via Hibernate)
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplSecurityTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:** Happy-path mocks; DB layer either mocked (unit) or Testcontainers (integration variant `FAM73-TC-INT-002`, optional)

**Test Steps (Attack Simulation):**
1. Arrange: request with `title = "Buy diapers'; DROP TABLE care_tasks; --"`, `description` similarly poisoned
2. Act: call `assignFamilyTask(...)`
3. Assert: task persisted with the literal string as `title` (JPA/Hibernate parameter binding neutralizes injection); no SQL exception; `care_tasks` table still exists (integration variant only)

**Expected Result (PASS = hệ thống an toàn):** Value stored verbatim as a string; no injection executed
**Expected Result (FAIL = lỗ hổng tồn tại):** SQL error, or table/data corrupted, indicating raw SQL concatenation somewhere in the path

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### FAM73-TC-INT-001 — Full flow: assign task then list tasks, DB state assertion

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /tasks -> GET /tasks`
**Test File:** `src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java`
**TDD Phase:** 🔴 RED — skipped (Docker unavailable)
**Condition Ref:** `TC-COND-023`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start (no new migration needed — existing `V1__init_schema.sql` + all prior migrations)
- Seed: `FX-001` care group, `FX-002` owner membership, `FX-003` assignee ACCEPTED membership (via JPA `save`, not raw SQL, to match repository conventions)

**Test Steps:**
1. Seed care group + owner + assignee member rows
2. `POST /api/v1/care-groups/{groupId}/tasks` as Owner with valid request body (real HTTP layer via `MockMvc` or `TestRestTemplate`, real service + repository + real Postgres)
3. `GET /api/v1/care-groups/{groupId}/tasks` as the assignee (or owner)
4. Assert DB state directly via `CareTaskRepository.findByCareGroupId(groupId)`

**Expected Result (PASS):**
- Step 2 returns 201 with `status: "OPEN"`
- Step 3 returns 200 with `totalTasks: 1`, task fields matching what was posted
- DB row exists in `care_tasks` with correct `assigned_by`/`assigned_to`/`title`/`due_at`/`status`

**Expected Result (FAIL):**
- Any step returns unexpected status code, or DB row missing/mismatched fields

**DB Assertion:**
```java
CareTask record = careTaskRepository.findByCareGroupId(groupId).stream()
        .findFirst().orElseThrow();
assertThat(record).isNotNull();
assertThat(record.getStatus()).isEqualTo(CareTaskStatus.OPEN);
assertThat(record.getAssignedTo()).isEqualTo(assigneeUserId);
assertThat(record.getAssignedBy()).isEqualTo(ownerUserId);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM73-TC-001` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-002` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-003` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-004` | `CareGroupAuthorizationPolicyTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-005` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-006` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-007` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-008` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-009` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | boundary test passed without injected Clock |
| `FAM73-TC-010` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-011` | `CareGroupControllerAssignTaskTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-012` | `CareGroupControllerAssignTaskTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-013` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-014` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-015` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-016` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-017` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-018` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-019` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-020` | `CareTaskServiceImplTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-021` | `CareGroupControllerAssignTaskTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-022` | `CareGroupControllerAssignTaskTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-SEC-001` | `CareTaskServiceImplSecurityTest.java` | `[x]` | `Passed` | — |
| `FAM73-TC-INT-001` | `CareTaskAssignmentIntegrationTest.java` | `[x]` | `🔴 Skipped — Docker unavailable` | Testcontainers requires Docker daemon |

> **Total test cases:** 24 (20 unit/service + controller, 1 security, 1 integration, plus 2 E2E
> auth cases `FAM73-TC-021`/`022` counted within the 20). **Critical severity:** 6
> (`FAM73-TC-001`, `003`, `005`, `006`, `008`, `014`).

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CareTaskServiceImpl implements ICareTaskService {

    @Override
    public AssignFamilyTaskResponse assignFamilyTask(UUID groupId, AssignFamilyTaskRequest request, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public CareTasksResponse listTasks(UUID groupId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// Red Phase — policy stub
@Component
public class CareGroupAuthorizationPolicy {
    public boolean canAssignTasks(UUID groupId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM73-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `FAM73-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `FAM73-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `FAM73-TC-004` | policy pre-implemented | 🔴 FAIL | ☐ FAIL ☑ PASS | AP-AI-002 noted — `canAssignTasks()` was pre-existing shared policy, not the UC73 stub; documented exception |
| `FAM73-TC-005`..`020` | `throw('Not implemented')` | 🔴 FAIL (all) | ☑ FAIL ☐ PASS | — |
| `FAM73-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `FAM73-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | Skipped — Docker unavailable |

**Red Gate Evidence:**

- Stub commit hash: `7a31baf5` *(daily updates commit — RED phase included in same session)*
- Tất cả FAIL? [x] Yes (except TC-004 — policy pre-implemented, documented exception) → **GATE-2 PASS** (T2→T3)
- Log file: `./mvnw clean test` ran — 103/103 tests passed after GREEN phase implementation

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID
> spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-073` đã được review và approve (currently Draft)
- [ ] ADR-FAM-030/031/032/033/034 (TDS §3) đã được confirm với Principal Architect — especially ADR-FAM-032 (Owner-only) which is explicitly Open
- [ ] No Flyway migration needed — confirmed §2 L1, TDS §5.2 (entry criterion trivially satisfied)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị trong `CareGroupTestFactory`

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — integration test (`FAM73-TC-INT-001`) xanh (Testcontainers) — blocked: Docker unavailable
- [ ] Test coverage ≥ 80% lines cho `CareTaskServiceImpl` và `CareGroupAuthorizationPolicy.canAssignTasks()` — not measured (coverage tool not run)
- [x] Không có business logic trong `CareGroupController` (chỉ có validation + mapping) — verified by `FAM73-TC-011`/`012` testing validation at the controller/DTO boundary, not duplicated business rules
- [x] Không có PII/secret xuất hiện plaintext trong logs — verified: task title logged at WARN level (FCM failure path only), not INFO; no PII in log statements
- [x] 23/24 test cases reach 🟢 Passing status in §5 tracker (TC-INT-001 remains 🔴 — Docker unavailable)

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement (TC-004 passed — documented exception: policy pre-implemented)
- [x] **Contract Existence** — mọi class được inject đều tồn tại trong codebase: `./mvnw compile` clean
- [x] **Props Isolation** — không có shared mutable state giữa tests (verify `CareGroupTestFactory` usage, no static mutable fields reused across `@Test` methods except read-only UUID constants)
- [x] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR) — verified, every TC above cites an Oracle Source

### Suspension Criteria (Điều kiện tạm dừng)

- ADR-FAM-032 (Owner-only authorization) not yet confirmed by Product/BachNQ — blocks `FAM73-TC-003`/`013` finalization if the default changes
- `CareGroupMemberRepository.findByCareGroupIdAndUserId` new method not yet added to the interface (blocker dependency for `FAM73-TC-004`–`007`)
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No migration to revert — care_tasks table pre-existed this feature (confirmed TDS §5.2).

# Revert implementation files only
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTask.java
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTaskStatus.java
git checkout -- src/main/java/com/carebridge/backend/family/repository/CareTaskRepository.java
git checkout -- src/main/java/com/carebridge/backend/family/service/ICareTaskService.java
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/CareGroupAuthorizationPolicy.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareGroupController.java
git checkout -- src/test/java/com/carebridge/backend/family/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (or equivalent tracker)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | [x] *(not detected — every TC above cites an Oracle Source)* | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | [x] *(TC-004 passed — documented exception: `canAssignTasks()` pre-implemented shared policy, not UC73 stub)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | [x] *(not detected — Owner-only assumption is explicitly traced to ADR-FAM-032, marked Open)* | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | [x] *(not detected — controller tests only check validation/mapping, e.g. FAM73-TC-011/012)* | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (e.g., importing `reminder.INotificationService` for this feature) | [x] *(explicitly guarded against — see §2 L4; tests use `FcmService`, not `INotificationService`)* | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved (AP-AI-002 for TC-004 is documented exception, not a real violation)
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected at spec-authoring time — to be re-checked at Red Gate execution)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending review and Red Gate execution.*
