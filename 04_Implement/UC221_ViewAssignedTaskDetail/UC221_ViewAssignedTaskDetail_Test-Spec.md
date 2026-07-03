# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC-221 View Assigned Task Detail

**Document ID:** `CB-FAM-TDD-221`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (lines 753-765) — primary schema source for `care_tasks`
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.6 (lines ~4752-4771) — UC-221 functional requirements
- `04_Implement/UC221_ViewAssignedTaskDetail/UC221_ViewAssignedTaskDetail_TDS.md` (`CB-FAM-IMP-221`) — companion Technical Design Specification
- `04_Implement/UC73_AssignFamilyTask/UC73_AssignFamilyTask_TDS.md` — `ADR-FAM-030` (`CareTaskStatus` enum), `ADR-FAM-033` (`FAM-033` reservation)
- `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md` — `ADR-FAM-002` (member-visibility pattern, reused)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/repository/CareGroupMemberRepository.java` — existing `existsByCareGroupIdAndUserIdAndInviteStatus` method (verified, reused as-is)
- PDPA (Vietnam) — minimum-necessary access; BR-RBAC, BR-PRIVACY, BR-SAFETY (SRS §3.3.17.6)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent — Test Designer` | Khởi tạo tài liệu — TDD spec cho UC-221 View Assigned Task Detail |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0 — GATE-2)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-221` |
| **Module** | `ViewAssignedTaskDetail — family bounded context` |
| **Spec gốc** | `CB-FAM-IMP-221` (companion TDS) |
| **Priority** | 🟠 P1 (Frequency of Use: **Frequent** per SRS) |
| **Sprint** | `S[N] — TBD (Open, no sprint assigned yet)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` — response exposes assigner/assignee display names + free-text task title/description ("notes") |
| **Compliance Scope** | `PDPA` (Vietnam) — BR-RBAC, BR-PRIVACY; BR-SAFETY cited by SRS but no enforcement mechanism exists for this read path (see TDS ADR-FAM-072, Open) |
| **Upstream Dependencies** | `CareGroupRepository`, `CareGroupMemberRepository` (existing/implemented), new `CareTask` entity + `CareTaskRepository` (co-designed with UC-73), `UserRepository` (existing) |
| **Downstream Consumers** | Mobile app "Chi tiết công việc" screen (CB-170) — read-only consumer |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-221 §17` (Constraint Injection Block C1-C9), `ADR-FAM-002`, `ADR-FAM-030`, `ADR-FAM-068..072` |
| **Constraints Injected** | Reuse existing `CareGroupController`; never expose JPA entity; membership gate via `existsByCareGroupIdAndUserIdAndInviteStatus`; gate order group→membership→task; `findByIdAndCareGroupId` scoping; response = real columns only; `CareTaskStatus{OPEN,IN_PROGRESS,DONE,CANCELLED}`; read-only transaction |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate — §5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` and approved migrations are the final
> persistence oracle; ERD/mockups are only supporting evidence. Test cases below encode the
> **corrected** behaviour, not any conflicting spec text.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Parent orchestration prompt suggested a default entity class name `CareTaskEntity` if sibling naming were unreadable | `UC73_AssignFamilyTask_TDS.md` (the batch's primary `care_tasks` entity-design reference, ADR-FAM-030) already names the entity `CareTask` (no suffix), matching CareBridge's existing no-suffix convention (`CareGroup`, `CareGroupMember`) | All test factories and assertions in this spec use `CareTask` (per this UC's own `ADR-FAM-070` decision), **not** `CareTaskEntity`. If implementation lands as `CareTaskEntity`, tests must be updated to match the actually-agreed cross-batch name — flagged as `OPEN-NAMING` in the TDS. |
| L2 | `UC85_UpdateAssignedTaskStatus_TDS.md` (Draft) proposes a conflicting `CareTaskStatus{OPEN, IN_PROGRESS, COMPLETED, NEEDS_SUPPORT}` enum | `UC73_AssignFamilyTask_TDS.md`'s `ADR-FAM-030` (this batch's confirmed canonical enum) defines `CareTaskStatus{OPEN, IN_PROGRESS, DONE, CANCELLED}`; `care_tasks.status` has no CHECK constraint (verified `V1__init_schema.sql` lines 753-765), so the enum is a pure code-level decision | Tests assert the response `status` field only ever takes values `OPEN`/`IN_PROGRESS`/`DONE`/`CANCELLED` (FAM221-TC-018); no test references `COMPLETED` or `NEEDS_SUPPORT`. This is a **project-wide reconciliation item** (OPEN-RECON in the TDS) — not resolved here, only encoded consistently. |
| L3 | CB-170 mockup (shared UC-221/222/223) renders priority ("MỨC ĐỘ"), a checklist ("Hạng mục chi tiết"), and an activity-history timeline ("LỊCH SỬ HOẠT ĐỘNG") | None of these are columns/tables in `care_tasks` (verified `V1__init_schema.sql` lines 753-765) — confirmed no `priority` column, no sub-item table, no task-history table exists anywhere in the schema | Tests assert the JSON response body does **not** contain `priority`, `checklist`, `subItems`, or `activityHistory` keys (FAM221-TC-013/014/015) — encodes `ADR-FAM-071`. |
| L4 | `UC216_ViewCareGroupMembers_TDS.md`'s prose mistakenly uses field names `account_id`/`invite_status`/`accounts` | Real code (`CareGroupMember.java`) uses `userId`/`inviteStatus` (Java) mapping to DB columns `user_id`/`invitation_status`, and there is no `accounts` table — the entity is `User` (`security` module, `users` table) | All test factories reference `CareGroupMember.userId`/`inviteStatus` and `User` (not `Account`/`accountId`/`invite_status`). |
| L5 | Naive design might allocate a brand-new "task not found" error code for UC-221 | `ADR-FAM-033` (UC-73) explicitly **pre-reserves** `FAM-033` for "care task not found" for UC-221/222/223 | Tests assert the not-found-task error code is exactly `FAM-033` (reused, not a newly minted code) — FAM221-TC-010/011. |
| L6 | A naive implementation might fetch the task before checking membership (task-first ordering), which would let a non-member distinguish "task exists in another group" (404-with-generic-message vs some other signal) via timing/response-shape side channels | `ADR-FAM-069` mandates gate order: group-exists → membership → task-fetch, so a non-member always gets `403 FAM-068` regardless of whether the requested task id exists anywhere | FAM221-TC-021 asserts membership is checked (and 403 raised) even when the requested `taskId` does not exist at all — proving the service never reaches the task-repository call for non-members. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewAssignedTaskDetail (UC-221) bao gồm các layer:
├── Domain (CareTaskStatus enum — pure, no deps)
├── Services (CareTaskServiceImpl.getTaskDetail — mock CareTaskRepository/CareGroupRepository/
│             CareGroupMemberRepository/UserRepository với Mockito)
├── Controller (CareGroupController.getTaskDetail — mock ICareTaskService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full DB flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-221 §3.3.17.6` | Actors (Mother, Family Member); Description (content, due date, assigner, status, notes); E1/E2 exception handling; POST-1 clear result state |
| `ADR-FAM-068` (this TDS) | Read visibility = any ACCEPTED member, not assignee-only |
| `ADR-FAM-069` (this TDS) | Gate ordering group→membership→task; cross-group `404` (not leaked) |
| `ADR-FAM-070` (this TDS) | Entity/service naming (`CareTask`/`ICareTaskService`/`CareTaskRepository`) |
| `ADR-FAM-071` (this TDS) | Priority/checklist/activity-history excluded from response |
| `ADR-FAM-030` (UC-73, reused) | `CareTaskStatus{OPEN,IN_PROGRESS,DONE,CANCELLED}` |
| `ADR-FAM-002` (UC-216, reused) | Member-only (`ACCEPTED`) visibility pattern |
| BR-RBAC / BR-PRIVACY | Minimum-necessary access; no email/phone in response |
| BR-SAFETY (Open) | No enforcement mechanism exists for care-task notes at read time — not tested as a control (nothing to assert), only documented |
| `CB-FAM-IMP-221` §9/§10/§16 | API request/response schema, error codes, authorization matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | ACCEPTED member (any role) can view task detail | `CareTaskServiceImpl.getTaskDetail()` | `FAM221-TC-001..005` |
| TC-COND-002 | Non-member / PENDING / REVOKED caller denied | `CareTaskServiceImpl.getTaskDetail()` | `FAM221-TC-006..008, 021` |
| TC-COND-003 | Care group or task not found handled distinctly | `CareTaskServiceImpl.getTaskDetail()` | `FAM221-TC-009..011` |
| TC-COND-004 | Unauthenticated request rejected before controller logic | `CareGroupController.getTaskDetail()` / Spring Security filter | `FAM221-TC-012, FAM221-TC-E2E-002` |
| TC-COND-005 | Response contains only real schema-backed fields | `CareTaskDetailResponse` mapping | `FAM221-TC-013..015` |
| TC-COND-006 | Status/completedAt/name-resolution logic correctness | `CareTaskServiceImpl.getTaskDetail()` | `FAM221-TC-016..020, 022` |
| TC-COND-007 | End-to-end API + DB integration | Full stack | `FAM221-TC-INT-001, FAM221-TC-E2E-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Member role (`OWNER`/`MEMBER`/`VIEWER`) × relationship to task (assigner/assignee/neither) | All partitions must resolve identically to "allowed" per `ADR-FAM-068` — read is role-agnostic |
| Equivalence Partitioning | `inviteStatus` (`ACCEPTED` vs `PENDING`/`REVOKED`/absent) | Only `ACCEPTED` passes the gate |
| Boundary Value Analysis | `assigned_by`/`assigned_to` nullable columns | Both null and non-null must resolve without NPE (FAM221-TC-020) |
| State Transition Testing | `CareTaskStatus` (`OPEN`/`IN_PROGRESS`/`DONE`/`CANCELLED`) × `completedAt` presence | `completedAt` must be null unless `DONE` (data-consistency check, not a state machine test — read UC performs no transition) |
| Error Guessing / Negative Testing | Cross-group id substitution (IDOR-style probe) | Verifies `findByIdAndCareGroupId` scoping prevents cross-group disclosure (FAM221-TC-011) |
| Contract/Schema Testing | Response JSON shape vs `ADR-FAM-071` exclusion list | Prevents silent re-introduction of `priority`/`checklist`/`activityHistory` |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroup{ id: GROUP-1, groupName: "Nhóm Mẹ Hoa" }` | Baseline care group |
| `FX-002` | DB seed | `CareGroupMember{ careGroupId: GROUP-1, userId: OWNER-1, memberRole: OWNER, inviteStatus: ACCEPTED }` | Owner/creator membership |
| `FX-003` | DB seed | `CareGroupMember{ careGroupId: GROUP-1, userId: MEMBER-1, memberRole: MEMBER, inviteStatus: ACCEPTED }` | Non-owner accepted member (assignee) |
| `FX-004` | DB seed | `CareGroupMember{ careGroupId: GROUP-1, userId: VIEWER-1, memberRole: VIEWER, inviteStatus: ACCEPTED }` | Viewer-role accepted member |
| `FX-005` | DB seed | `CareGroupMember{ careGroupId: GROUP-1, userId: PENDING-1, memberRole: MEMBER, inviteStatus: PENDING }` | Not-yet-accepted invitee (must be denied) |
| `FX-006` | DB seed | `CareGroupMember{ careGroupId: GROUP-1, userId: REVOKED-1, memberRole: MEMBER, inviteStatus: REVOKED }` | Revoked member (must be denied) |
| `FX-007` | DB seed | `CareTask{ id: TASK-1, careGroupId: GROUP-1, assignedBy: OWNER-1, assignedTo: MEMBER-1, title: "Chuẩn bị đồ dùng đi sinh", description: "Mua tã bỉm...", dueAt: <future>, status: OPEN, completedAt: null }` | Happy-path task, status `OPEN` |
| `FX-008` | DB seed | `CareTask{ id: TASK-2, careGroupId: GROUP-1, ..., status: DONE, completedAt: <past-instant> }` | `DONE` task — `completedAt` must be populated |
| `FX-009` | DB seed | `CareGroup{ id: GROUP-2 }` + `CareTask{ id: TASK-3, careGroupId: GROUP-2, ... }` | Second, unrelated group+task — used to prove cross-group isolation (FAM221-TC-011) |
| `FX-010` | DB seed | `CareTask{ id: TASK-4, careGroupId: GROUP-1, assignedBy: null, assignedTo: null, ... }` | Task with null assigner/assignee — boundary case (FAM221-TC-020) |
| `FX-011` | Fixture | `User{ id: OWNER-1, name: "Mẹ Hoa", email: "hoa@example.test", phone: "0900000001" }`, `User{ id: MEMBER-1, name: "Bố Tuấn", email: "tuan@example.test", phone: "0900000002" }` | Name resolution + PII-leak-negative-assertion source |
| `FX-012` | JWT | `{ sub: "OWNER-1"/"MEMBER-1"/"NONMEMBER-1", role: "MOTHER"/"FAMILY_MEMBER" }` | Auth context for E2E tests |

---

## 4. Test Case Specification

> **TC ID format:** `FAM221-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing
> **Total test cases in this spec:** 24 (20 unit/service + 1 integration + 2 E2E + already counted; see §5 tracker) — **4 CRITICAL**, 8 HIGH, 10 MEDIUM, 2 LOW-equivalent(informational, folded into HIGH above where applicable)

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state
> giữa các test cases. Biện pháp chống AP-AI-002 (Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeX()
// ═══════════════════════════════════════════════════════════

// CareTaskDetailTestFactory.java
class CareTaskDetailTestFactory {

    static final UUID GROUP_1   = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000001");
    static final UUID GROUP_2   = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000099");
    static final UUID OWNER_1   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID MEMBER_1  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID VIEWER_1  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PENDING_1 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID REVOKED_1 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final UUID NONMEMBER_1 = UUID.fromString("00000000-0000-0000-0000-000000000006");
    static final UUID TASK_1    = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000010");

    // Fresh CareGroup — synced with FX-001
    static CareGroup makeGroup() {
        return CareGroup.builder()
                .id(GROUP_1)
                .groupName("Nhóm Mẹ Hoa")
                .build();
    }

    // Fresh CareGroupMember — overload to vary role/status; synced with FX-002..006
    static CareGroupMember makeMember(UUID userId, GroupMemberRole role, InviteStatus status) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_1)
                .userId(userId)
                .memberRole(role)
                .inviteStatus(status)
                .build();
    }

    // Fresh CareTask — baseline OPEN task; synced with FX-007
    static CareTask makeTask(Consumer<CareTask.CareTaskBuilder> overrides) {
        CareTask.CareTaskBuilder builder = CareTask.builder()
                .id(TASK_1)
                .careGroupId(GROUP_1)
                .assignedBy(OWNER_1)
                .assignedTo(MEMBER_1)
                .title("Chuẩn bị đồ dùng đi sinh cho mẹ và bé")
                .description("Mua tã bỉm, quần áo sơ sinh, giấy tờ tùy thân của mẹ")
                .dueAt(Instant.parse("2026-10-15T09:00:00Z"))
                .status(CareTaskStatus.OPEN)
                .completedAt(null);
        overrides.accept(builder);
        return builder.build();
    }

    // Fresh User — synced with FX-011; never include email/phone in DTO assertions source
    static User makeUser(UUID id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(name.toLowerCase().replace(" ", "") + "@example.test"); // present on entity, must NOT leak to response
        u.setPhone("0900000000");
        return u;
    }
}
```

---

### FAM221-TC-001 — OWNER member views task detail (happy path)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplGetDetailTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-221 §3.3.17.6` Normal Flow Step 5 + `CB-FAM-IMP-221` §9.2 (200 response schema)

**Preconditions:**
- `FX-001` (GROUP_1), `FX-002` (OWNER_1, `OWNER`, `ACCEPTED`), `FX-007` (TASK_1, `OPEN`)

**Test Steps:**
1. Arrange: mock `groupRepository.findById(GROUP_1)` → `Optional.of(makeGroup())`; mock
   `memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_1, OWNER_1, ACCEPTED)` → `true`;
   mock `taskRepository.findByIdAndCareGroupId(TASK_1, GROUP_1)` → `Optional.of(makeTask(b -> {}))`;
   mock `userRepository.findById(OWNER_1/MEMBER_1)` → respective `makeUser(...)`.
2. Act: call `service.getTaskDetail(GROUP_1, TASK_1, OWNER_1)`.
3. Assert: returned `CareTaskDetailResponse.careTaskId == TASK_1`, `.status == "OPEN"`,
   `.assignedByName == "Mẹ Hoa"`, `.assignedToName == "Bố Tuấn"`, `.completedAt == null`.

**Expected Result (PASS):** All fields populated per `CB-FAM-IMP-221` §9.2 happy-path JSON.
**Expected Result (FAIL):** `403`/`404` thrown, or a field silently null/mismatched.

**Current Status:** 🔴 Not written
**Implementation Note:** Owner is just one instance of "any ACCEPTED member" (ADR-FAM-068) — do not special-case OWNER in the service.

---

### FAM221-TC-002 — Non-owner MEMBER views task detail (happy path)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-068` Decision — "any ACCEPTED member (any memberRole)"

**Preconditions:** `FX-003` (MEMBER_1, `MEMBER`, `ACCEPTED`), `FX-007`

**Test Steps:**
1. Arrange as TC-001 but membership check uses `MEMBER_1`.
2. Act: `service.getTaskDetail(GROUP_1, TASK_1, MEMBER_1)`.
3. Assert: 200-equivalent response returned (no exception), full fields populated.

**Expected Result (PASS):** Same shape as TC-001 — role has no bearing on read outcome.
**Expected Result (FAIL):** Exception thrown for non-owner caller (would indicate an incorrect owner-only check leaked from UC-73's write-path logic).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the regression guard against accidentally reusing `CareGroupAuthorizationPolicy.canAssignTasks()` (owner-only, ADR-FAM-032) instead of the read-visibility check (ADR-FAM-068).

---

### FAM221-TC-003 — VIEWER-role member views task detail (happy path)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-068` Decision

**Preconditions:** `FX-004` (VIEWER_1, `VIEWER`, `ACCEPTED`), `FX-007`

**Test Steps:**
1. Arrange as TC-001 with `VIEWER_1`.
2. Act: `service.getTaskDetail(GROUP_1, TASK_1, VIEWER_1)`.
3. Assert: response returned successfully.

**Expected Result (PASS):** `VIEWER` role is sufficient for read (ADR-FAM-068 explicitly allows any role).
**Expected Result (FAIL):** Exception thrown — would indicate role filtering incorrectly excludes `VIEWER`.

**Current Status:** 🔴 Not written

---

### FAM221-TC-004 — Assignee views their own assigned task

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-221 §3.3.17.6` Primary Actor "Family Member"; `FX-007` (`assignedTo = MEMBER_1`)

**Preconditions:** `FX-003`, `FX-007`

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_1, MEMBER_1)` where `MEMBER_1 == task.assignedTo`.
2. Assert: response returned; `assignedTo == MEMBER_1`.

**Expected Result (PASS):** Assignee is a valid caller (subset of "any ACCEPTED member").
**Expected Result (FAIL):** N/A — covered by TC-002 logic; kept distinct to explicitly document the assignee persona from SRS.

**Current Status:** 🔴 Not written

---

### FAM221-TC-005 — Assigner (creator) views the task they created

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-221 §3.3.17.6` Primary Actor "Mother"; `ADR-FAM-068` Consequences ("assigner and assignee both can view")

**Preconditions:** `FX-002` (OWNER_1 == `task.assignedBy`), `FX-007`

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_1, OWNER_1)` where `OWNER_1 == task.assignedBy`.
2. Assert: response returned; `assignedBy == OWNER_1`.

**Expected Result (PASS):** Assigner (Mother) can view her own created task.
**Expected Result (FAIL):** N/A — distinct persona coverage of TC-001.

**Current Status:** 🔴 Not written

---

### FAM221-TC-006 — Non-member is denied (403 FAM-068)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC, PDPA minimum-necessary access`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-068` Decision; `CB-FAM-IMP-221` §10 error table (`FAM-068`, 403)

**Preconditions:** `FX-001`, `FX-007`; caller `NONMEMBER_1` has **no** `CareGroupMember` row for `GROUP_1`

**Test Steps:**
1. Arrange: `groupRepository.findById(GROUP_1)` → present; `memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_1, NONMEMBER_1, ACCEPTED)` → `false`.
2. Act: call `service.getTaskDetail(GROUP_1, TASK_1, NONMEMBER_1)`.
3. Assert: throws `BusinessException` with HTTP `403` and code `FAM-068`.

**Expected Result (PASS — hành vi đúng):** `BusinessException(403, "FAM-068", ...)` thrown; `taskRepository.findByIdAndCareGroupId(...)` is **never invoked** (verify via `Mockito.verifyNoInteractions(taskRepository)`).
**Expected Result (FAIL — dấu hiệu lỗi):** No exception thrown, or task data returned to a non-member, or wrong error code.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the core authorization control for the entire UC — treat as the highest-priority test.

---

### FAM221-TC-007 — PENDING invitee is denied (403 FAM-068)

**Severity:** `HIGH`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-002` (reused) — PENDING is not sufficient membership; `ADR-FAM-068`

**Preconditions:** `FX-005` (PENDING_1, `PENDING`)

**Test Steps:**
1. Arrange: `existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_1, PENDING_1, ACCEPTED)` → `false` (PENDING status never satisfies the `ACCEPTED`-only query).
2. Act: `service.getTaskDetail(GROUP_1, TASK_1, PENDING_1)`.
3. Assert: throws `BusinessException(403, "FAM-068")`.

**Expected Result (PASS):** Same as TC-006.
**Expected Result (FAIL):** PENDING invitee incorrectly allowed to read task detail before formally joining.

**Current Status:** 🔴 Not written

---

### FAM221-TC-008 — REVOKED member is denied (403 FAM-068)

**Severity:** `HIGH`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-002` (reused); `ADR-FAM-068`

**Preconditions:** `FX-006` (REVOKED_1, `REVOKED`)

**Test Steps:**
1. Arrange: `existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_1, REVOKED_1, ACCEPTED)` → `false`.
2. Act: `service.getTaskDetail(GROUP_1, TASK_1, REVOKED_1)`.
3. Assert: throws `BusinessException(403, "FAM-068")`.

**Expected Result (PASS):** A formerly-accepted-then-revoked member loses read access immediately.
**Expected Result (FAIL):** Stale membership grants continued access post-revocation.

**Current Status:** 🔴 Not written

---

### FAM221-TC-009 — Care group not found (404 FAM-005)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-FAM-IMP-221` §10 error table (`FAM-005`, reused from UC-70/216)

**Preconditions:** `groupRepository.findById(anyGroupId)` → `Optional.empty()`

**Test Steps:**
1. Act: `service.getTaskDetail(NONEXISTENT_GROUP, TASK_1, OWNER_1)`.
2. Assert: throws `BusinessException(404, "FAM-005")`.

**Expected Result (PASS):** Group-existence check runs first and fails cleanly.
**Expected Result (FAIL):** NPE, wrong status code, or wrong error code.

**Current Status:** 🔴 Not written

---

### FAM221-TC-010 — Task id does not exist anywhere (404 FAM-033)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-033` (UC-73, `FAM-033` reservation) + `CB-FAM-IMP-221` §10

**Preconditions:** `FX-001`, `FX-002` (OWNER_1 accepted); `taskRepository.findByIdAndCareGroupId(nonexistentTaskId, GROUP_1)` → `Optional.empty()`

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, NONEXISTENT_TASK, OWNER_1)` (caller IS an accepted member).
2. Assert: throws `BusinessException(404, "FAM-033")`.

**Expected Result (PASS):** Exactly `FAM-033` (the reserved code), not a newly invented code.
**Expected Result (FAIL):** Wrong/new error code, or `500` from an unhandled `Optional.get()`.

**Current Status:** 🔴 Not written

---

### FAM221-TC-011 — Task exists but belongs to a different group (IDOR — 404 FAM-033, no cross-group leak)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Legal:** `BR-PRIVACY — no cross-group data disclosure`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail(UUID, UUID, UUID)`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-069` Decision ("`findByIdAndCareGroupId` ... returning `FAM-033` on mismatch")

**Preconditions:** `FX-009` — `TASK-3` belongs to `GROUP_2`; caller `OWNER_1` is an `ACCEPTED` member of `GROUP_1` (not `GROUP_2`)

**Test Steps (Attack Simulation):**
1. Arrange: `groupRepository.findById(GROUP_1)` → present; membership of `OWNER_1` in `GROUP_1` → `true`;
   `taskRepository.findByIdAndCareGroupId(TASK_3, GROUP_1)` → `Optional.empty()` (because `TASK_3.careGroupId == GROUP_2`, not `GROUP_1`).
2. Act: caller requests `GET /api/v1/care-groups/{GROUP_1}/tasks/{TASK_3}` — i.e. `service.getTaskDetail(GROUP_1, TASK_3, OWNER_1)`.
3. Assert: throws `BusinessException(404, "FAM-033")` — **not** the actual `TASK_3` data from `GROUP_2`.

**Expected Result (PASS = hệ thống an toàn):** `404 FAM-033`; response body contains no field from the real `TASK_3` row (no title/description/assignee from `GROUP_2`).
**Expected Result (FAIL = lỗ hổng tồn tại):** `TASK_3`'s data (belonging to a group the caller is not a member of) is returned — a cross-group IDOR data leak.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the test that validates the entire rationale for `findByIdAndCareGroupId` (a group-scoped query) instead of a plain `findById(taskId)`.

---

### FAM221-TC-012 — Unauthenticated request rejected (401)

**Severity:** `HIGH`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `CareGroupController.getTaskDetail()` (Spring Security filter chain, `@PreAuthorize("isAuthenticated()")`)
**Test File:** `src/test/java/com/carebridge/backend/family/controller/CareGroupControllerGetTaskDetailTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-FAM-IMP-221` §9.2 (401 `IAM-001` response) + §16 Auth Matrix (`GUEST` row)

**Preconditions:** No `Authorization` header / invalid JWT

**Test Steps:**
1. Act: `mockMvc.perform(get("/api/v1/care-groups/{groupId}/tasks/{taskId}", GROUP_1, TASK_1))` — no `Authorization` header.
2. Assert: response status `401`.

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, controller/service method never invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request reaches the service layer without authentication.

**Current Status:** 🔴 Not written

---

### FAM221-TC-013 — Response never includes a `priority` field

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskDetailResponse` mapping in `CareTaskServiceImpl.getTaskDetail()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-071` Decision — "no `priority` column ... excluded from the response"; `V1__init_schema.sql` lines 753-765 (no `priority` column)

**Preconditions:** `FX-007`

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_1, OWNER_1)`, then serialize the response to JSON (or reflectively enumerate `CareTaskDetailResponse` fields).
2. Assert: the response object/JSON has no `priority` field/key.

**Expected Result (PASS):** No `priority` key anywhere in the response.
**Expected Result (FAIL):** A `priority` field is present (schema-drift regression re-introducing the CB-170 mockup's unsupported UI aspiration as a fabricated field).

**Current Status:** 🔴 Not written

---

### FAM221-TC-014 — Response never includes a checklist/sub-items field

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskDetailResponse` mapping
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-071` Decision — "no table/column supports sub-items"

**Test Steps:**
1. Act: serialize response to JSON.
2. Assert: no `checklist` / `subItems` / `items` key present.

**Expected Result (PASS):** Absent.
**Expected Result (FAIL):** Present — indicates an invented/hallucinated field not backed by schema.

**Current Status:** 🔴 Not written

---

### FAM221-TC-015 — Response never includes an activity-history field

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskDetailResponse` mapping
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-071` Decision — "no task-history table"

**Test Steps:**
1. Act: serialize response to JSON.
2. Assert: no `activityHistory` / `history` / `auditLog` key present.

**Expected Result (PASS):** Absent.
**Expected Result (FAIL):** Present — would require an `AuditAction`-style table that does not exist; a fabricated/mocked field would be a hallucinated contract (AP-AI-005).

**Current Status:** 🔴 Not written

---

### FAM221-TC-016 — `completedAt` is null when status is not `DONE`

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` mapping
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `V1__init_schema.sql` (`completed_at` nullable, no default); `CB-FAM-IMP-221` §9.2 sample (`"completedAt": null` for `OPEN`)

**Preconditions:** `FX-007` (`status = OPEN`, `completedAt = null`)

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_1, OWNER_1)`.
2. Assert: `response.completedAt == null`.

**Expected Result (PASS):** `null`, verbatim passthrough of the stored column — no fabricated timestamp.
**Expected Result (FAIL):** A non-null value synthesized for a non-`DONE` task.

**Current Status:** 🔴 Not written

---

### FAM221-TC-017 — `completedAt` is populated when status is `DONE`

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` mapping
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `FX-008` (`status = DONE`, `completedAt = <past-instant>`)

**Preconditions:** `FX-008` (`TASK-2`)

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_2, OWNER_1)`.
2. Assert: `response.completedAt` equals the seeded `Instant`; `response.status == "DONE"`.

**Expected Result (PASS):** Verbatim passthrough of the stored `completed_at`.
**Expected Result (FAIL):** `null` returned despite a stored value (mapping bug), or a re-computed/current timestamp instead of the stored one.

**Current Status:** 🔴 Not written

---

### FAM221-TC-018 — `status` maps only through the canonical `CareTaskStatus` enum (not the UC-85 variant)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskStatus` enum + `CareTaskServiceImpl.getTaskDetail()` mapping
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-030` (UC-73, reused) — `{OPEN, IN_PROGRESS, DONE, CANCELLED}`; §2 L2 (this document)

**Test Steps:**
1. Arrange: parametrize over all four seeded `CareTask.status` values (`OPEN`, `IN_PROGRESS`, `DONE`, `CANCELLED`).
2. Act: `service.getTaskDetail(...)` for each.
3. Assert: `response.status` equals the exact enum name for each case; additionally assert (via reflection or compile-time reference) that `CareTaskStatus.values()` contains exactly
   `{OPEN, IN_PROGRESS, DONE, CANCELLED}` and does **not** contain `COMPLETED` or `NEEDS_SUPPORT`.

**Expected Result (PASS):** All four values map verbatim; enum has exactly the UC-73/ADR-FAM-030 members.
**Expected Result (FAIL):** Enum accidentally compiled against the conflicting UC-85 variant (`COMPLETED`/`NEEDS_SUPPORT` present, `DONE`/`CANCELLED` absent) — a cross-batch reconciliation regression.

**Current Status:** 🔴 Not written

---

### FAM221-TC-019 — Response exposes only display names, never email/phone (PII minimization)

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Legal:** `BR-PRIVACY, PDPA minimum-necessary access`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` mapping (`assignedByName`/`assignedToName` resolution)
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-FAM-IMP-221` §4.3 Security NFR ("never email/phone of assigner/assignee"); `FX-011` (`User` fixtures carry email/phone that must NOT leak)

**Preconditions:** `FX-011` — `User` entities carry `email`/`phone` populated (to prove the mapping actively excludes them, not merely "happens to omit" them due to unset fields)

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_1, OWNER_1)`, serialize response to JSON.
2. Assert: JSON contains `"assignedByName": "Mẹ Hoa"` and `"assignedToName": "Bố Tuấn"`; JSON does **not** contain the substring `"@"`, the seeded phone digits, or any key named `email`/`phone`.

**Expected Result (PASS = an toàn):** Only display names present; no contact PII.
**Expected Result (FAIL = lỗ hổng):** `CareTaskDetailResponse` (or a nested `User`/`CareGroupMember` object) leaks email/phone — e.g. via accidentally serializing the whole `User` entity instead of mapping just `.getName()`.

**Current Status:** 🔴 Not written

---

### FAM221-TC-020 — Null `assigned_by`/`assigned_to` handled without NPE

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` — `resolveName()` helper
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `V1__init_schema.sql` lines 753-765 — `assigned_by`/`assigned_to` are nullable columns; `FX-010`

**Preconditions:** `FX-010` (`TASK-4`, `assignedBy = null`, `assignedTo = null`)

**Test Steps:**
1. Act: `service.getTaskDetail(GROUP_1, TASK_4, OWNER_1)`.
2. Assert: no exception thrown; `response.assignedBy == null`, `response.assignedByName == null`,
   `response.assignedTo == null`, `response.assignedToName == null`; `userRepository.findById(...)` is never called with a `null` argument.

**Expected Result (PASS):** Graceful `null` propagation.
**Expected Result (FAIL):** `NullPointerException`, or `userRepository.findById(null)` throws/misbehaves.

**Current Status:** 🔴 Not written

---

### FAM221-TC-021 — Gate ordering: membership denial happens even for a non-existent task (no task-existence side channel)

**Severity:** `CRITICAL`
**CWE:** `CWE-203 — Observable Discrepancy (information exposure through response differences)`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` gate order
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-069` "Gate ordering invariant: group-exists → membership → task-fetch" (TDS §6.2 note)

**Preconditions:** `NONMEMBER_1` not a member of `GROUP_1`; `NONEXISTENT_TASK` does not exist anywhere

**Test Steps:**
1. Arrange: `groupRepository.findById(GROUP_1)` → present; membership check for `NONMEMBER_1` → `false`.
2. Act: `service.getTaskDetail(GROUP_1, NONEXISTENT_TASK, NONMEMBER_1)`.
3. Assert: throws `BusinessException(403, "FAM-068")` — **not** `404 FAM-033` — and
   `Mockito.verifyNoInteractions(taskRepository)` (the task lookup is never attempted).

**Expected Result (PASS = an toàn):** Always `403` for non-members regardless of whether the requested task id is real; the task repository is never queried, so a non-member cannot use response-code differences (`403` vs `404`) to probe task existence indirectly.
**Expected Result (FAIL = lỗ hổng):** `404 FAM-033` returned instead of `403`, implying the task-existence check ran (or was reachable) before/independent of the membership gate.

**Current Status:** 🔴 Not written
**Implementation Note:** Directly enforces the TDS §6.2 gate-ordering invariant; also validated by mock-interaction verification, not just the returned status code.

---

### FAM221-TC-INT-001 — Full flow with real DB: seed → authorize → fetch → map

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.getTaskDetail()` + `CareTaskRepository` + `CareGroupMemberRepository` + `UserRepository` (real JPA, Testcontainers)
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskDetailIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically at Spring context start
- Seed: `FX-001, FX-002, FX-003, FX-007, FX-011` inserted via JPA repositories

**Test Steps:**
1. Seed `CareGroup`, two `CareGroupMember` rows (`OWNER_1` ACCEPTED-OWNER, `MEMBER_1` ACCEPTED-MEMBER), one `CareTask` (`assignedBy=OWNER_1`, `assignedTo=MEMBER_1`, `status=OPEN`), two `User` rows.
2. Call `careTaskService.getTaskDetail(GROUP_1, TASK_1, MEMBER_1)` against the real Spring context.
3. Assert response fields against the seeded row values (title/description/dueAt/status/names).
4. Repeat with a caller not in `care_group_members` for `GROUP_1` — assert `BusinessException(403, FAM-068)` is thrown, no exception swallowed.

**Expected Result (PASS):**
- Real DB round-trip returns the exact seeded field values.
- Non-member path throws the correct exception end-to-end (proves the Mockito-based unit tests reflect real repository behavior, not just mocked assumptions).

**Expected Result (FAIL):** DB assertion mismatch, or membership check passes incorrectly against real data.

**DB Assertion:**
```java
CareTask record = taskRepository.findByIdAndCareGroupId(TASK_1, GROUP_1).orElseThrow();
assertThat(record.getStatus()).isEqualTo(CareTaskStatus.OPEN);
assertThat(record.getCareGroupId()).isEqualTo(GROUP_1);
```

**Current Status:** 🔴 Not written

---

### FAM221-TC-E2E-001 — Full API happy path (ACCEPTED member)

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/care-groups/{groupId}/tasks/{taskId}` — full stack
**Test File:** `src/test/java/com/carebridge/backend/family/controller/CareGroupControllerGetTaskDetailE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:** Seed as in `FAM221-TC-INT-001`; valid JWT for `MEMBER_1` with role `FAMILY_MEMBER`

**Test Steps:**
1. `GET /api/v1/care-groups/{GROUP_1}/tasks/{TASK_1}` with:
   | Header          | Value              |
   | Authorization   | Bearer [MEMBER_1 JWT] |
   | X-Correlation-Id| [uuid]             |
2. Assert response status `200`.
3. Assert response body matches `CB-FAM-IMP-221` §9.2 happy-path JSON shape (field names, no extra fields).

**Expected Result (PASS):** `200` with correct body shape; no `priority`/`checklist`/`activityHistory` keys.
**Expected Result (FAIL):** Wrong status, missing fields, or leaked out-of-scope fields.

**Current Status:** 🔴 Not written

---

### FAM221-TC-E2E-002 — Unauthenticated API call denied

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `GET /api/v1/care-groups/{groupId}/tasks/{taskId}` — full stack
**Test File:** same as E2E-001
**TDD Phase:** 🔴 RED

**Preconditions:** No `Authorization` header

**Test Steps (Attack Simulation):**
1. `GET /api/v1/care-groups/{GROUP_1}/tasks/{TASK_1}` with no `Authorization` header.
2. Assert response status `401` with body `{ "error": { "code": "IAM-001", ... } }`.

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`.
**Expected Result (FAIL = lỗ hổng tồn tại):** Task data returned without authentication.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM221-TC-001` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-002` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-003` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-004` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-005` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-006` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-007` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-008` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-009` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-010` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-011` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-012` | `CareGroupControllerGetTaskDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-013` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-014` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-015` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-016` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-017` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-018` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-019` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-020` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-021` | `CareTaskServiceImplGetDetailTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-INT-001` | `CareTaskDetailIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-E2E-001` | `CareGroupControllerGetTaskDetailE2ETest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM221-TC-E2E-002` | `CareGroupControllerGetTaskDetailE2ETest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.
> Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CareTaskServiceImpl implements ICareTaskService {

    @Override
    public CareTaskDetailResponse getTaskDetail(UUID groupId, UUID taskId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    // (assignFamilyTask / listTasks from UC-73 stubbed identically if co-landing in the same PR —
    //  not this UC's concern if already implemented independently.)
}
```

```java
// Red Phase — controller stub (delegates to the stubbed service; also PHẢI throw/propagate)
@GetMapping("/{groupId}/tasks/{taskId}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<CareTaskDetailResponse>> getTaskDetail(
        @PathVariable UUID groupId, @PathVariable UUID taskId, Principal principal) {
    var callerId = SecurityUtils.requireCurrentUserId(principal);
    return ResponseEntity.ok(ApiResponse.success(
        careTaskService.getTaskDetail(groupId, taskId, callerId))); // throws UnsupportedOperationException
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM221-TC-001..005` (happy paths) | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM221-TC-006..008, 021` (auth denial) | `throw UnsupportedOperationException` (masks the intended `403`) | 🔴 FAIL | ☐ FAIL ☐ PASS | Must fail because the stub throws `UnsupportedOperationException`, not `BusinessException(403,...)` — test asserting exact exception type/code must not accidentally pass on a generic `RuntimeException` catch |
| `FAM221-TC-009..011` (not-found) | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | Same as above — code must assert the specific `FAM-005`/`FAM-033` code, not merely "an exception was thrown" |
| `FAM221-TC-012` (401) | Controller stub still requires auth (Spring Security runs first) | 🔴 FAIL *(expected 401 not reached — request never hits stub because there is no Authorization header at all, so this test should already read as intended pre-stub; verify it still fails for the RIGHT reason if service stub changes nothing)* | ☐ FAIL ☐ PASS | |
| `FAM221-TC-013..015` (field exclusion) | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | Must fail because no response object is produced to inspect |
| `FAM221-TC-016..020` (logic/mapping) | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM221-TC-INT-001, E2E-001` | Full stack — stub throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM221-TC-E2E-002` (401 unauth) | N/A — request rejected before controller | 🔴 FAIL *(must still be written as a real assertion, not vacuously true)* | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled when stub is committed)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` (to be filled during execution)

> **Nếu bất kỳ test PASS bất thường:** Dừng lại. Đặc biệt kiểm tra `FAM221-TC-006/007/008/021` — nếu
> chúng PASS với stub ném `UnsupportedOperationException` thay vì `BusinessException(403, FAM-068)`,
> khả năng cao test đang assert quá lỏng (chỉ `assertThrows(RuntimeException.class, ...)` thay vì
> assert đúng HTTP status + error code) — đây chính là dấu hiệu AP-AI-002 cần rewrite.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-221` đã được review và approve (currently `Draft`)
- [ ] `ADR-FAM-068/069/070/071/072` reviewed by Tech Lead (currently `Proposed`)
- [ ] `OPEN-RECON` (UC-73 vs UC-85 `CareTaskStatus` conflict) explicitly acknowledged by Tech Lead — this Test-Spec commits to the UC-73 variant regardless, but implementation must not start until Tech Lead confirms which enum ships
- [ ] Shared `CareTask` entity design (ADR-FAM-070) confirmed consistent with UC-73/UC-222 (if UC-222 lands with a different entity name, tests in this file must be updated per §2 L1)
- [ ] No Flyway migration needed — confirmed (`care_tasks` already exists)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — integration test (`FAM221-TC-INT-001`) xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `CareTaskServiceImpl.getTaskDetail()` and the new controller method
- [ ] Không có business logic trong `CareGroupController.getTaskDetail()` (chỉ validation + mapping)
- [ ] Không có PII (email/phone) xuất hiện plaintext trong logs hoặc response (FAM221-TC-019 green)
- [ ] Response never contains `priority`/`checklist`/`activityHistory` (FAM221-TC-013/014/015 green)
- [ ] Cross-group IDOR test (`FAM221-TC-011`) green
- [ ] Gate-ordering test (`FAM221-TC-021`) green

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 24 tests FAIL với `UnsupportedOperationException` stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua `CareTaskDetailTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (đã ghi trong mỗi TC ở §4)

### Suspension Criteria (Điều kiện tạm dừng)

- `CareTask` entity/repository not yet created by UC-73 (blocking dependency) — this Test-Spec's fixtures assume the entity exists; if UC-73 has not landed, entity/repository creation (TDS §11.3 Chặng 1) must happen first, shared across UC-73/221/222
- `OPEN-RECON` (enum conflict) unresolved and blocking a shared compile unit with UC-85's code (if UC-85 also lands in the same PR/module — not expected, but flagged)
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No DB migration to revert — this feature introduces no schema change (TDS §5.2/§11.2).

# Revert implementation files (if a partial/broken implementation was committed)
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareGroupController.java
git checkout -- src/main/java/com/carebridge/backend/family/dto/response/CareTaskDetailResponse.java
git checkout -- src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplGetDetailTest.java
git checkout -- src/test/java/com/carebridge/backend/family/controller/CareGroupControllerGetTaskDetailTest.java
git checkout -- src/test/java/com/carebridge/backend/family/service/CareTaskDetailIntegrationTest.java

# NOTE: if CareTask.java / CareTaskRepository.java are SHARED with UC-73/UC-222 (co-designed,
# ADR-FAM-070), do NOT blanket-revert those files if UC-73/UC-222 code already depends on them —
# coordinate with those specs before touching the shared entity/repository.

# Gap remains OPEN → this UC stays unimplemented; TDS/Test-Spec Status remains Draft.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ Checklist cho reviewer khi test cases được AI hỗ trợ generate (per §1.1, `AI Assisted: Yes`).

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ *(not observed — every TC cites an Oracle Source)* | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với `UnsupportedOperationException` stub (§5.1) | ☐ *(to be verified when Red Gate is executed — see §5.1 warning re: TC-006/007/008/021 loose-assertion risk)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes an architecture decision without an ADR (e.g., assignee-only visibility, or a new `priority` field) | ☐ *(not observed — visibility traced to ADR-FAM-068; field exclusion traced to ADR-FAM-071)* | G-1 |
| AP-AI-004 | Layer Violation | Test verifies business logic inside the controller test | ☐ *(not observed — controller tests (TC-012, E2E) assert HTTP status/auth only, not business branching)* | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a service/type/column not in the codebase (e.g., a `TaskHistoryRepository`, a `priority` field, `CareTaskEntity` name mismatch) | ☐ *(§2 L1 flags the `CareTaskEntity` vs `CareTask` naming risk explicitly — reviewer must re-check against whichever name UC-73/222 actually ship with)* | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Phát hiện các Open items cần theo dõi (không phải anti-pattern trong spec, mà là rủi ro cần re-xác nhận khi implement) → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-005` (risk, not confirmed) | `ALL` (entity references) | Spec assumes entity name `CareTask` per this UC's own `ADR-FAM-070`; if the actually-implemented shared entity is named differently (e.g. `CareTaskEntity`), every test factory/import in this file needs a rename | Re-verify entity class name against the real file in `family/entity/` before writing test code; update `CareTaskDetailTestFactory` accordingly | ☐ |
| `AP-AI-003` (risk, not confirmed) | `FAM221-TC-018` | Spec commits to `ADR-FAM-030`'s enum; `OPEN-RECON` with UC-85 is unresolved at the project level | Confirm with Tech Lead which enum ships before writing `CareTaskStatus` test assertions | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
*Status: Draft — pending review. Do not mark Approved without explicit user/Tech Lead confirmation.*
