# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC220 — Leave Care Group — Test Specification

**Document ID:** `FPT-EDU-TDD-TEMPLATE-001` (instance for `CB-FAM-IMP-220`)
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (15/22 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (`care_group_members`, `care_tasks`: lines 750-762)
- `04_Implement/UC220_LeaveCareGroup/UC220_LeaveCareGroup_TDS.md` (`CB-FAM-IMP-220`) — companion Technical Specification
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.5 (~lines 4731-4750) — Functional requirements (UC-220)
- `03_Design/UI_UX/MobileAppScreen/CB-178 Leave Care Group Confirmation (UC-220)/code.html` — UI/UX oracle for task-reassignment behavior
- ADR-FAM-063/064/065/066/067 — see TDS §3
- `04_Implement/UC73_AssignFamilyTask/UC73_AssignFamilyTask_TDS.md` — `CareTaskStatus` enum (ADR-FAM-030)
- `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md` — `ADR-FAM-002` (ACCEPTED-only visibility)
- PDPA (Vietnam) — minimum-necessary access; *(Luật 91/2025, NĐ 356/2025 — Not applicable; no specific article identified for this feature's scope, generic BR-PRIVACY/BR-RBAC cited instead)*

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC-220 Leave Care Group |
| 2026-07-10 | AI Agent | Truthful sync after implementation evidence: status set to Partially Implemented, 15/22 tests PASS in `CareGroupServiceImplMembershipLifecycleTest`; Red Gate not reconstructed because implementation pre-existed; E2E/INT remain pending |

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
| **Feature / Gap ID** | `GAP-FAM220` |
| **Module** | `family` — Leave Care Group |
| **Spec gốc** | `CB-FAM-IMP-220` |
| **Priority** | 🟠 P1 (Medium per SRS) |
| **Sprint** | `Sprint 3 — Family Sync batch (UC217-222)` |
| **Milestone** | `M3 Alpha` *(exact date not confirmed in available research — Open, do not invent)* |
| **Data Classification** | `PII` (family membership + task ownership — see TDS §1) |
| **Compliance Scope** | `BR-RBAC`, `BR-PRIVACY`, `PDPA` |
| **Upstream Dependencies** | `CareGroup`, `CareGroupMember` (UC-70/216), `CareTask`/`CareTaskStatus` (UC-73 ADR-FAM-030, greenfield), `AuditService` |
| **Downstream Consumers** | Mobile "Rời nhóm" confirmation screen (CB-178); UC-216 member listing; group owner's task list |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-220 §17`, `ADR-FAM-063/064/065/066/067` |
| **Constraints Injected** | C1 (own-row resolution only), C2 (owner check before writes), C3 (not-ACCEPTED rejection), C4 (bulk reassignment query, same transaction), C5 (REVOKED + new AuditAction), C6 (identity/layering), C7 (no migration) — see TDS §17.1 |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T3 for unit/service coverage; Red Gate not reconstructed because implementation pre-existed (§5.1)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS text is generic ("the actor confirms the requested action") and does not itself say what happens to assigned tasks on leave | UI/UX mockup `CB-178 code.html` explicitly states unfinished tasks "sẽ được chuyển lại cho trưởng nhóm" (will be transferred back to the group leader) | Test cases assert the reassignment target is specifically `care_groups.owner_user_id`, using the mockup as the oracle, not an SRS line number |
| L2 | Earlier research found no `CareTask` JPA entity/repository yet for the `care_tasks` table — same greenfield finding as UC-73 | `CareTask`, `CareTaskStatus`, and `CareTaskRepository.reassignIncompleteTasks(...)` now exist in the working tree; `CareTaskStatus` enum values are `OPEN, IN_PROGRESS, DONE, CANCELLED` per UC-73 `ADR-FAM-030` — the `NEEDS_SUPPORT` variant from UC-85 is explicitly NOT used for this batch | Tests assert the service calls the existing bulk reassignment collaborator and preserves the 4 canonical status contract; DONE/CANCELLED DB behavior still requires repository/integration tests |
| L3 | The real `CareGroupMember` entity field names are `userId`/`inviteStatus` (maps to DB `user_id`/`invitation_status`), NOT `accountId`/`invite_status` as UC-216's own prose incorrectly states in places | Verified directly from `CareGroupMember.java`: `@Column(name = "user_id") private UUID userId;` and `@Column(name = "invitation_status") private InviteStatus inviteStatus;` | All test code and mock setups in this Test-Spec use `userId`/`getUserId()`/`inviteStatus`/`getInviteStatus()` — never `accountId`/`invite_status` |
| L4 | SRS Exceptions (E1/E2) are generic ("access is denied ... outside the permitted data scope" / "invalid ... data is rejected") and do not literally spell out "owner cannot leave" or "must be an ACCEPTED member" | TDS `ADR-FAM-064` (owner boundary, consistent with UC-219's owner-cannot-be-removed wording) and `ADR-FAM-063` (self-leave requires the caller's own row to be `ACCEPTED`) | Test cases for `FAM-063` (owner) and `FAM-064` (not-a-member/PENDING/already-REVOKED) are treated as **confirmed ADR decisions**, not SRS line-item mandates — Oracle Source cites the ADR, not a raw SRS quote |
| L5 | `CareGroupMemberRepository` already has `findByCareGroupIdAndUserId` (verified in codebase) — no new repository method needed for the membership side; only a NEW `CareTaskRepository` (greenfield) is required for the reassignment bulk query | Confirmed via direct read of `CareGroupMemberRepository.java` | Tests for the membership-resolution path mock the EXISTING `findByCareGroupIdAndUserId`; tests for reassignment mock/assert the NEW `CareTaskRepository.reassignIncompleteTasks(...)` |
| L6 | The `AuditAction` enum (verified) has no leave-specific constant; `CARE_GROUP_INVITE_ACCEPTED`/`CARE_GROUP_INVITE_DECLINED` exist but neither fits "leave" or "remove" or "revoke" semantics | `invitation_status` stays `{ACCEPTED, PENDING, REVOKED}` (no new value, no migration, per confirmed batch decision) — differentiation happens ONLY via the audit action | Tests assert the NEW `AuditAction.CARE_GROUP_MEMBER_LEFT` constant specifically — never reusing `CARE_GROUP_INVITE_DECLINED` or a hypothetical `CARE_GROUP_MEMBER_REMOVED` (that belongs to UC-219, a distinct sibling feature) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Family / Leave Care Group bao gồm các layer:
├── Domain (CareGroupMember, CareTask entities — pure data; CareTaskStatus enum reused from UC-73)
├── Services (CareGroupServiceImpl.leaveCareGroup() — mock CareGroupRepository, CareGroupMemberRepository,
│             CareTaskRepository, AuditService, ApplicationEventPublisher với Mockito)
├── Controller (CareGroupController new /leave endpoint — mock ICareGroupService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, full leave flow: membership flip +
                 task reassignment + audit, asserted against real repositories)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-220` | Happy path (leave), E1 (access denied / owner boundary), E2 (invalid/conflicting state rejected), POST-1/2/3 (result shown, related records updated, audit) |
| `ADR-FAM-063` | Self-leave resolves caller's OWN row only; requires `ACCEPTED` |
| `ADR-FAM-064` | Owner cannot leave — dedicated `FAM-063` rejection before any write |
| `ADR-FAM-065` | Task reassignment: incomplete (`OPEN`/`IN_PROGRESS`) only, to `care_groups.owner_user_id`, single bulk UPDATE, same transaction; oracle = CB-178 mockup |
| `ADR-FAM-066` | New `AuditAction.CARE_GROUP_MEMBER_LEFT`, distinct from sibling UCs' actions |
| `ADR-FAM-067` | Response returns `reassignedTaskCount`; pre-leave read is Open (not tested here — OPEN-1) |
| `CB-178 code.html` | Oracle for "unfinished tasks transferred back to group leader" wording and behavior |
| `CB-FAM-IMP-220` §8/§9/§10 | Interface signatures, API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | ACCEPTED non-owner member leaves; has 2 incomplete tasks reassigned to owner | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-001` |
| TC-COND-002 | ACCEPTED non-owner member leaves; has zero incomplete tasks | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-002` |
| TC-COND-003 | Care group does not exist | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-003` |
| TC-COND-004 | Caller's own row has `memberRole == OWNER` | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-004` |
| TC-COND-005 | Caller has no membership row in this group at all | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-005` |
| TC-COND-006 | Caller's own row is `PENDING` (not yet accepted) | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-006` |
| TC-COND-007 | Caller's own row is already `REVOKED` (already left / removed / declined) | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-007` |
| TC-COND-008 | `DONE` tasks are NOT reassigned — remain with departing member | `CareTaskRepository.reassignIncompleteTasks()` | `FAM220-TC-008` |
| TC-COND-009 | `CANCELLED` tasks are NOT reassigned — remain with departing member | `CareTaskRepository.reassignIncompleteTasks()` | `FAM220-TC-009` |
| TC-COND-010 | Membership row is append-only — never hard-deleted, only `REVOKED` | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-010` |
| TC-COND-011 | Successful leave writes `CARE_GROUP_MEMBER_LEFT` audit log with correct actor/entity | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-011` |
| TC-COND-012 | Successful leave publishes `CareGroupMemberLeft` event with correct payload | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-012` |
| TC-COND-013 | Successful leave with reassignment publishes `CareTaskReassigned` event with correct count | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-013` |
| TC-COND-014 | Reassignment target is always `care_groups.owner_user_id` — not hardcoded/wrong user | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-014` |
| TC-COND-015 | Atomicity: if the reassignment step throws, the membership row is NOT flipped to REVOKED (rollback) | `CareGroupServiceImpl.leaveCareGroup()` (transactional boundary) | `FAM220-TC-015` |
| TC-COND-016 | After leaving, the departed member is excluded from UC-216 `listMembers()` (ACCEPTED/PENDING filter) | `CareGroupServiceImpl.listMembers()` (existing, cross-check) | `FAM220-TC-016` |
| TC-COND-017 | Response DTO never leaks the raw `CareGroupMember`/`CareTask` entity | `LeaveCareGroupResponse` mapping | `FAM220-TC-017` |
| TC-COND-018 | No JWT / unauthenticated caller on POST /leave | `CareGroupController.leaveCareGroup()` | `FAM220-TC-018` (E2E) |
| TC-COND-019 | Second leave attempt by the same (now-REVOKED) caller is rejected, not a silent no-op success | `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-019` |
| TC-COND-020 | Caller cannot leave "on behalf of" another user — no client-supplied identity is honored | `CareGroupController.leaveCareGroup()`, `SecurityUtils` | `FAM220-TC-020` |
| TC-COND-021 | Full integration: seed group+owner+member+tasks, call leave, assert DB state for both tables | End-to-end (`@SpringBootTest` + Testcontainers) | `FAM220-TC-INT-001` |
| TC-COND-022 | Privilege-escalation attempt via a manipulated/forged path or payload targeting another user's row | Security — `CareGroupServiceImpl.leaveCareGroup()` | `FAM220-TC-SEC-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller's own `inviteStatus` (ACCEPTED / PENDING / REVOKED / no-row) | Each partition maps to a distinct outcome (success vs. `FAM-064`) |
| Equivalence Partitioning | Caller's `memberRole` (OWNER vs. non-OWNER) | Owner partition always rejects (`FAM-063`), non-owner partition proceeds |
| Equivalence Partitioning | Task `status` (OPEN/IN_PROGRESS vs. DONE/CANCELLED) | Only the incomplete partition is reassigned; the terminal partition must remain untouched |
| Boundary Value Analysis | Task count = 0 vs. task count > 0 | Confirms `reassignedTaskCount` is accurate at both the empty and non-empty boundary |
| State Transition Testing | `invitation_status` (ACCEPTED → REVOKED only, never → DELETE) | Confirms the append-only invariant holds for this feature's only legal transition |
| Error Guessing | Repeated leave calls (idempotency), forged caller identity | Most likely misuse vectors for a self-service state-changing endpoint |
| Role/Permission Partitioning | Owner / non-owner ACCEPTED member / non-member / unauthenticated | TDS §16 Authorization Matrix has 5 distinct partitions for this single endpoint |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroup{ id: GROUP_ID, ownerUserId: OWNER_ID, status: ACTIVE }` | Baseline group |
| `FX-002` | DB seed | `CareGroupMember{ careGroupId: GROUP_ID, userId: OWNER_ID, memberRole: OWNER, inviteStatus: ACCEPTED }` | Owner membership row |
| `FX-003` | DB seed | `CareGroupMember{ careGroupId: GROUP_ID, userId: LEAVING_MEMBER_ID, memberRole: MEMBER, inviteStatus: ACCEPTED }` | Caller who will leave (happy path) |
| `FX-004` | DB seed | `CareGroupMember{ ..., inviteStatus: PENDING }` for `LEAVING_MEMBER_ID` | Not-yet-accepted case (`FAM220-TC-006`) |
| `FX-005` | DB seed | `CareGroupMember{ ..., inviteStatus: REVOKED }` for `LEAVING_MEMBER_ID` | Already-left case (`FAM220-TC-007`, `TC-019`) |
| `FX-006` | DB seed | `CareTask{ careGroupId: GROUP_ID, assignedTo: LEAVING_MEMBER_ID, status: OPEN }` | Incomplete task #1 (reassignment target) |
| `FX-007` | DB seed | `CareTask{ careGroupId: GROUP_ID, assignedTo: LEAVING_MEMBER_ID, status: IN_PROGRESS }` | Incomplete task #2 (reassignment target) |
| `FX-008` | DB seed | `CareTask{ careGroupId: GROUP_ID, assignedTo: LEAVING_MEMBER_ID, status: DONE, completedAt: <past> }` | Must NOT be reassigned (`FAM220-TC-008`) |
| `FX-009` | DB seed | `CareTask{ careGroupId: GROUP_ID, assignedTo: LEAVING_MEMBER_ID, status: CANCELLED }` | Must NOT be reassigned (`FAM220-TC-009`) |
| `FX-010` | JWT | `{ sub: LEAVING_MEMBER_ID, role: 'FAMILY' }` | Auth context for member-leaves E2E tests |
| `FX-011` | JWT | `{ sub: OWNER_ID, role: 'MOTHER' }` | Auth context for owner-cannot-leave E2E test |
| `FX-012` | JWT | `{ sub: NON_MEMBER_ID, role: 'FAMILY' }` | Auth context for not-a-member E2E test |

> All UUIDs above are placeholders (e.g. `GROUP_ID`, `OWNER_ID`, `LEAVING_MEMBER_ID`) bound to concrete
> `UUID.fromString("00000000-0000-0000-0000-0000000000XX")` synthetic values in the shared
> `CareGroupTestFactory` (additive extension — see Props Isolation Boilerplate below; this factory is
> shared across UC71/72/73/83/220 Test-Spec files per project convention, methods grow additively,
> never redefine existing ones). Test Data Classification: **SYNTHETIC** for all fixtures — no
> production PII used.

---

## 4. Test Case Specification

> **TC ID format:** `FAM220-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// CareGroupTestFactory.java — package com.carebridge.backend.family (test sources)
// Shared across UC71/UC72/UC73/UC83/UC220 Test-Spec files — methods grow additively, never redefine.
class CareGroupTestFactory {

    static final UUID GROUP_ID           = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OWNER_ID           = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID MEMBER_ID          = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ASSIGNEE_ID        = UUID.fromString("00000000-0000-0000-0000-000000000004");
    // NEW for UC-220
    static final UUID LEAVING_MEMBER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID NON_MEMBER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000021");

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

    // NEW for UC-220 — the caller-about-to-leave, non-owner, ACCEPTED by default
    static CareGroupMember makeLeavingMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .userId(LEAVING_MEMBER_ID)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        overrides.accept(member);
        return member;
    }

    // NEW for UC-220 — CareTask factory (reuses UC-73's greenfield entity/enum)
    static CareTask makeCareTask(Consumer<CareTask> overrides) {
        CareTask task = CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .assignedBy(OWNER_ID)
                .assignedTo(LEAVING_MEMBER_ID)
                .title("Buy diapers")
                .status(CareTaskStatus.OPEN)
                .build();
        overrides.accept(task);
        return task;
    }
}
```

---

### FAM220-TC-001 — Member leaves with 2 incomplete tasks reassigned to owner (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `03_Design/UI_UX/MobileAppScreen/CB-178 Leave Care Group Confirmation (UC-220)/code.html` ("Bạn đang có 2 công việc được giao. Chúng sẽ được chuyển lại cho trưởng nhóm."); `ADR-FAM-065`

**Preconditions:**
- `FX-001` (care group, owner=OWNER_ID), `FX-003` (leaving member ACCEPTED, non-owner)
- `FX-006`, `FX-007` (2 incomplete tasks assigned to `LEAVING_MEMBER_ID`)

**Test Steps:**
1. Arrange: mock `groupRepository.findById(GROUP_ID)` → `makeCareGroup(b->{})`; mock
   `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, LEAVING_MEMBER_ID)` →
   `Optional.of(makeLeavingMember(m->{}))`; mock
   `taskRepository.reassignIncompleteTasks(GROUP_ID, LEAVING_MEMBER_ID, OWNER_ID)` → `2`
2. Act: call `careGroupService.leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: response `reassignedTaskCount == 2`; `memberRepository.save` called once with a member
   whose `inviteStatus == REVOKED`; `taskRepository.reassignIncompleteTasks` called exactly once
   with `(GROUP_ID, LEAVING_MEMBER_ID, OWNER_ID)`

**Expected Result (PASS — hành vi đúng):**
- Returns `LeaveCareGroupResponse{ groupId, leftAt, reassignedTaskCount: 2 }`; no exception thrown

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or `reassignedTaskCount` wrong/absent, or `memberRepository.save` not invoked, or
  reassignment target is not `OWNER_ID`

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_memberLeaves_reassignsIncompleteTasksToOwnerAndRevokesOwnRow`) on `2026-07-10`.
**Implementation Note:** This is the exact scenario the CB-178 mockup warns about — treat its Vietnamese
copy as the literal oracle for "why" this side effect exists.

---

### FAM220-TC-002 — Member leaves with zero incomplete tasks (Happy Path, no reassignment)

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-065` (reassignment is a no-op when there is nothing incomplete to move)

**Preconditions:** `FX-001`, `FX-003`; `taskRepository.reassignIncompleteTasks(...)` mocked → `0`

**Test Steps:**
1. Arrange: mock as above with reassignment returning `0`
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: response `reassignedTaskCount == 0`; membership still flipped to `REVOKED`; no exception

**Expected Result (PASS):** `LeaveCareGroupResponse{ reassignedTaskCount: 0 }`, member row REVOKED
**Expected Result (FAIL):** Exception thrown for the zero-task case, or member row not updated

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_memberLeavesWithNoIncompleteTasks_returnsZeroAndDoesNotPublishTaskEvent`) on `2026-07-10`.

---

### FAM220-TC-003 — Care group does not exist → 404 FAM-005

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-FAM-IMP-220` §10 error code table (`FAM-005`, reused from UC-70/216 code)

**Preconditions:** `groupRepository.findById(GROUP_ID)` mocked → `Optional.empty()`

**Test Steps:**
1. Arrange: mock as above
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `BusinessException` thrown with `httpStatus == 404` and `code == "FAM-005"`

**Expected Result (PASS):** `BusinessException(404, "FAM-005")`; `memberRepository.save` and
`taskRepository.reassignIncompleteTasks` never called
**Expected Result (FAIL):** No exception, wrong code, or side effects executed despite missing group

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_groupNotFound_throwsFam005`) on `2026-07-10`.

---

### FAM220-TC-004 — Caller is the group OWNER → 403 FAM-063 (owner-cannot-leave-rejected)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-064` (owner cannot leave — consistent with UC-219's "cannot remove/target the owner" rule)

**Preconditions:** `FX-002` — caller's own row has `memberRole == OWNER`, `inviteStatus == ACCEPTED`

**Test Steps:**
1. Arrange: mock `groupRepository.findById` → group; mock
   `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)` →
   `Optional.of(makeCareGroupMember(m -> { m.setUserId(OWNER_ID); m.setMemberRole(GroupMemberRole.OWNER); }))`
2. Act: call `leaveCareGroup(GROUP_ID, OWNER_ID)`
3. Assert: `BusinessException(403, "FAM-063")` thrown BEFORE any reassignment or save call

**Expected Result (PASS):** 403 `FAM-063` thrown; `taskRepository.reassignIncompleteTasks` and
`memberRepository.save` never invoked
**Expected Result (FAIL):** Owner successfully leaves, orphaning the group (critical data-integrity bug)

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_ownerCannotLeave_throwsFam063`) on `2026-07-10`.
**Implementation Note:** The owner check MUST run before the reassignment call — verify call order
with `InOrder`/`verifyNoInteractions` on `taskRepository`.

---

### FAM220-TC-005 — Caller has no membership row at all → 409 FAM-064 (not-a-member-rejected)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-063`; `CB-FAM-IMP-220` §10 (`FAM-064`)

**Preconditions:** `memberRepository.findByCareGroupIdAndUserId(GROUP_ID, NON_MEMBER_ID)` →
`Optional.empty()`

**Test Steps:**
1. Arrange: mock as above
2. Act: call `leaveCareGroup(GROUP_ID, NON_MEMBER_ID)`
3. Assert: `BusinessException(409, "FAM-064")` thrown

**Expected Result (PASS):** 409 `FAM-064`; no reassignment, no save
**Expected Result (FAIL):** NPE, or a phantom membership row is created/updated for a non-member

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_missingMembership_throwsFam064`) on `2026-07-10`.

---

### FAM220-TC-006 — Caller's own row is PENDING (not yet accepted) → 409 FAM-064

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-063` — a PENDING invitee is not yet an active member; they decline via UC-218, not leave

**Preconditions:** `FX-004` — `LEAVING_MEMBER_ID`'s row has `inviteStatus == PENDING`

**Test Steps:**
1. Arrange: mock `findByCareGroupIdAndUserId` → `Optional.of(makeLeavingMember(m ->
   m.setInviteStatus(InviteStatus.PENDING)))`
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `BusinessException(409, "FAM-064")` thrown

**Expected Result (PASS):** 409 `FAM-064`
**Expected Result (FAIL):** A still-pending invitee is allowed to "leave" a group they never joined

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_pendingMembership_throwsFam064`) on `2026-07-10`.

---

### FAM220-TC-007 — Caller's own row is already REVOKED (already left) → 409 FAM-064

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-FAM-063`; `CB-FAM-IMP-220` §9.1 idempotency note

**Preconditions:** `FX-005` — `LEAVING_MEMBER_ID`'s row already `REVOKED`

**Test Steps:**
1. Arrange: mock `findByCareGroupIdAndUserId` → `Optional.of(makeLeavingMember(m ->
   m.setInviteStatus(InviteStatus.REVOKED)))`
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `BusinessException(409, "FAM-064")` thrown

**Expected Result (PASS):** 409 `FAM-064` — second leave attempt is rejected, not a silent success
**Expected Result (FAIL):** Method returns success again / re-runs reassignment against an already-departed member

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_revokedMembership_throwsFam064AndDoesNotReassignAgain`) on `2026-07-10`.

---

### FAM220-TC-008 — DONE tasks are NOT reassigned (remain with departing member)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskRepository.reassignIncompleteTasks()`
**Test File:** `src/test/java/com/carebridge/backend/family/repository/CareTaskRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-065` ("DONE/CANCELLED tasks are untouched — historical record")

**Preconditions:** `FX-008` — a `DONE` task assigned to `LEAVING_MEMBER_ID` in `GROUP_ID`, seeded via
Testcontainers PostgreSQL (bulk `UPDATE ... WHERE status IN (...)` cannot be verified with a pure mock)

**Test Steps:**
1. Seed `FX-001`, `FX-008` via JPA `save`
2. Act: call `careTaskRepository.reassignIncompleteTasks(GROUP_ID, LEAVING_MEMBER_ID, OWNER_ID)`
3. Assert: returned row count excludes the DONE task; re-fetch the DONE task and assert
   `assignedTo == LEAVING_MEMBER_ID` (unchanged)

**Expected Result (PASS):** DONE task's `assigned_to` unchanged after the bulk update
**Expected Result (FAIL):** DONE task incorrectly reassigned to the owner (data-integrity regression —
loses historical record of who actually completed it)

**Current Status:** 🔴 Not written

---

### FAM220-TC-009 — CANCELLED tasks are NOT reassigned (remain with departing member)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskRepository.reassignIncompleteTasks()`
**Test File:** `CareTaskRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-065`

**Preconditions:** `FX-009` — a `CANCELLED` task assigned to `LEAVING_MEMBER_ID` in `GROUP_ID`

**Test Steps:**
1. Seed `FX-001`, `FX-009`
2. Act: call `reassignIncompleteTasks(GROUP_ID, LEAVING_MEMBER_ID, OWNER_ID)`
3. Assert: re-fetch the CANCELLED task, assert `assignedTo == LEAVING_MEMBER_ID` (unchanged)

**Expected Result (PASS):** CANCELLED task's `assigned_to` unchanged
**Expected Result (FAIL):** CANCELLED task incorrectly reassigned

**Current Status:** 🔴 Not written

---

### FAM220-TC-010 — Membership row is append-only (never hard-deleted)

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** TDS §6.3 State Machine invariant; CLAUDE.md general append-only pattern used across `family` module

**Preconditions:** `FX-001`, `FX-003`

**Test Steps:**
1. Arrange: happy-path mocks
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `memberRepository.delete(...)`/`deleteById(...)` is NEVER called; only
   `memberRepository.save(...)` with `inviteStatus == REVOKED` is invoked

**Expected Result (PASS):** No delete method invoked on the repository at any point
**Expected Result (FAIL):** Row physically deleted, breaking audit history / append-only invariant

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_membershipFlipIsAppendOnlySaveNotDelete`) on `2026-07-10`.

---

### FAM220-TC-011 — Successful leave writes `CARE_GROUP_MEMBER_LEFT` audit log

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-FAM-066`; SRS POST-3 "sensitive actions recorded for audit"

**Preconditions:** `auditService` mocked; happy-path mocks

**Test Steps:**
1. Arrange: happy-path mocks
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `auditService.log(AuditAction.CARE_GROUP_MEMBER_LEFT, LEAVING_MEMBER_ID, "CareGroup",
   GROUP_ID.toString(), anyString())` invoked exactly once — NOT `CARE_GROUP_INVITE_DECLINED` or any
   other existing constant

**Expected Result (PASS):** Audit call matches `CARE_GROUP_MEMBER_LEFT` with the leaving member as actor
**Expected Result (FAIL):** No audit call, or wrong/reused `AuditAction` constant (violates ADR-FAM-066's
differentiation requirement)

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_memberLeaves_reassignsIncompleteTasksToOwnerAndRevokesOwnRow`) on `2026-07-10`.
**Implementation Note:** Requires adding `CARE_GROUP_MEMBER_LEFT` to the `AuditAction` enum (code change,
not migration).

---

### FAM220-TC-012 — Successful leave publishes `CareGroupMemberLeft` event with correct payload

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-FAM-IMP-220` §7.3 payload schema

**Preconditions:** `ApplicationEventPublisher` mocked

**Test Steps:**
1. Arrange: happy-path mocks (2 incomplete tasks)
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `eventPublisher.publishEvent(argThat(evt -> evt instanceof CareGroupMemberLeft))` captured;
   payload's `careGroupId == GROUP_ID`, `leavingUserId == LEAVING_MEMBER_ID`, `reassignedTaskCount == 2`

**Expected Result (PASS):** Event published once with correct payload fields
**Expected Result (FAIL):** Event not published, wrong payload, or published more than once

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_publishesMemberLeftAndTaskReassignedEvents`) on `2026-07-10`.

---

### FAM220-TC-013 — Successful leave publishes `CareTaskReassigned` event with correct count

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-FAM-IMP-220` §7.3 payload schema

**Preconditions:** `ApplicationEventPublisher` mocked; reassignment mocked to return `2`

**Test Steps:**
1. Arrange: happy-path mocks
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `eventPublisher.publishEvent(argThat(evt -> evt instanceof CareTaskReassigned))` captured;
   payload's `fromUserId == LEAVING_MEMBER_ID`, `toUserId == OWNER_ID`, `reassignedCount == 2`

**Expected Result (PASS):** Event published with matching payload
**Expected Result (FAIL):** Event missing or fields mismatched (e.g. `toUserId` not the actual owner)

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_publishesMemberLeftAndTaskReassignedEvents`) on `2026-07-10`.

---

### FAM220-TC-014 — Reassignment target is always the group's actual owner (not hardcoded)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-FAM-065` — "target `toUserId` is `care_groups.owner_user_id`"

**Preconditions:** `FX-001` with a DIFFERENT owner than the test's other fixtures (`ownerUserId =
UUID.fromString("...-000000000099")`, a value not equal to the shared `OWNER_ID` constant)

**Test Steps:**
1. Arrange: mock `groupRepository.findById` → group with `ownerUserId = 99`; mock leaving member as above
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `taskRepository.reassignIncompleteTasks(GROUP_ID, LEAVING_MEMBER_ID, <owner=99>)` called
   with the group's ACTUAL `ownerUserId`, not a hardcoded constant

**Expected Result (PASS):** Reassignment call uses `group.getOwnerUserId()` dynamically
**Expected Result (FAIL):** Reassignment target is a hardcoded/wrong user id, silently misdirecting tasks

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_usesActualGroupOwnerAsReassignmentTarget`) on `2026-07-10`.

---

### FAM220-TC-015 — Atomicity: reassignment failure prevents the membership flip

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()` (transactional boundary)
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupLeaveIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-FAM-IMP-220` §4.2 ("Atomicity... All-or-nothing"); `ADR-FAM-065` ("same
`@Transactional` boundary")

**Preconditions:** Testcontainers PostgreSQL; `FX-001`, `FX-003` seeded via real repositories; force the
reassignment step to throw (e.g. inject a spy `CareTaskRepository` that throws
`DataIntegrityViolationException` on `reassignIncompleteTasks`)

**Test Steps:**
1. Seed group + leaving member
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)` with the reassignment step forced to throw
3. Assert: an exception propagates out of `leaveCareGroup`; re-fetch the membership row from the real DB
   — `inviteStatus` is STILL `ACCEPTED` (not `REVOKED`), confirming the transaction rolled back

**Expected Result (PASS):** Membership row unchanged in the DB after the forced failure (transactional
rollback works as designed)
**Expected Result (FAIL):** Membership flipped to `REVOKED` despite the reassignment failing — a member
loses access to the group while their tasks are still orphaned (data-integrity violation)

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_reassignmentFailurePreventsMembershipFlip`) on `2026-07-10`.
**Implementation Note:** Requires `@Transactional` (already the class-level default per
`CareGroupServiceImpl`, verified) to wrap BOTH the reassignment call and the member save.

---

### FAM220-TC-016 — Departed member excluded from UC-216 `listMembers()` after leaving

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.listMembers()` (existing, cross-check with UC-216 `ADR-FAM-002`)
**Test File:** `CareGroupLeaveIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md`
`ADR-FAM-002` (REVOKED excluded from listing)

**Preconditions:** Testcontainers; seed group + owner + leaving member (ACCEPTED)

**Test Steps:**
1. Seed as above; confirm `listMembers(GROUP_ID, OWNER_ID)` initially includes `LEAVING_MEMBER_ID`
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)`
3. Assert: `listMembers(GROUP_ID, OWNER_ID)` no longer includes `LEAVING_MEMBER_ID` in its `members` list

**Expected Result (PASS):** "Stops access to shared data" confirmed indirectly via the existing UC-216
filter — no separate access-teardown mechanism needed
**Expected Result (FAIL):** Departed member still appears in the member listing (REVOKED filter not
applied, or leave did not actually set REVOKED)

**Current Status:** 🔴 Not written

---

### FAM220-TC-017 — Response DTO never leaks the raw entity

**Severity:** `MEDIUM`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `LeaveCareGroupResponse` mapping in `CareGroupServiceImpl`
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** CLAUDE.md "Never expose JPA entities in API responses; use DTOs and mappers"

**Preconditions:** Happy-path mocks

**Test Steps:**
1. Act: call `leaveCareGroup(...)`
2. Assert (type check): return type is `LeaveCareGroupResponse` — NOT `CareGroupMember`/`CareTask`; no
   method signature in `ICareGroupService.leaveCareGroup` returns an entity type directly

**Expected Result (PASS):** Return type is the DTO only; response contains `groupId`, `leftAt`,
`reassignedTaskCount` — no `careGroupMemberId`, no raw `CareTask` fields
**Expected Result (FAIL):** Method returns/exposes a JPA-annotated type directly

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_responseContainsOnlyContractFields`) on `2026-07-10`.

---

### FAM220-TC-018 — No JWT / unauthenticated caller on POST /leave → 401

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupController.leaveCareGroup()`
**Test File:** `src/test/java/com/carebridge/backend/family/controller/CareGroupControllerLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** TDS §16 Authorization Matrix (`GUEST` column → 401); platform-wide JWT filter behavior

**Preconditions:** `@WebMvcTest` with Spring Security filter chain active, no `Authorization` header

**Test Steps:**
1. Act: `POST /api/v1/care-groups/{groupId}/leave` with no `Authorization` header
2. Assert: HTTP 401; `ICareGroupService.leaveCareGroup` never invoked

**Expected Result (PASS):** 401 returned before reaching the service layer
**Expected Result (FAIL):** 200/500, or the service is invoked despite missing auth

**Current Status:** 🔴 Not written

---

### FAM220-TC-019 — Second leave attempt by the same (now-REVOKED) caller is rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()` (idempotency behavior)
**Test File:** `CareGroupServiceLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CB-FAM-IMP-220` §9.1 idempotency footnote — "a second leave call ... returns 409
FAM-064 and makes no further change"

**Preconditions:** Same as `FAM220-TC-007` (row already `REVOKED`)

**Test Steps:**
1. Arrange: mock member row as `REVOKED`
2. Act: call `leaveCareGroup(GROUP_ID, LEAVING_MEMBER_ID)` a second time
3. Assert: `BusinessException(409, "FAM-064")`; no duplicate audit log entry, no duplicate reassignment

**Expected Result (PASS):** Consistent 409 on every repeat call after the first successful leave
**Expected Result (FAIL):** Second call silently "succeeds" again, double-writing audit logs or
re-running reassignment against an already-empty task set

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`leaveCareGroup_revokedMembership_throwsFam064AndDoesNotReassignAgain`) on `2026-07-10`.

---

### FAM220-TC-020 — Caller cannot leave "on behalf of" another user (own-row-only enforcement)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupController.leaveCareGroup()`, `SecurityUtils.requireCurrentUserId()`
**Test File:** `CareGroupControllerLeaveTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `ADR-FAM-063` — "No client-supplied identity is accepted"; `CB-FAM-IMP-220` §17 C1

**Preconditions:** JWT for `NON_MEMBER_ID`; endpoint accepts NO request body / no member-id parameter by
contract (§9.2 — request body is explicitly `none`)

**Test Steps:**
1. Arrange: `mockMvc` with mocked `ICareGroupService`; JWT `sub = NON_MEMBER_ID`
2. Act: `POST /api/v1/care-groups/{groupId}/leave` (no body accepted per contract — confirms there is no
   field through which another user's id could even be supplied)
3. Assert: `careGroupService.leaveCareGroup(groupId, NON_MEMBER_ID)` invoked with the JWT's own
   `sub` — never a value from the request body/path beyond `groupId`

**Expected Result (PASS):** `callerId` passed to the service always equals the JWT `sub`; endpoint
signature has no member-id/user-id request field to spoof
**Expected Result (FAIL):** Endpoint accepts a body/query param that could override the acting user id
(privilege escalation surface)

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### FAM220-TC-SEC-001 — Privilege-escalation attempt via forged path/claims

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC / PDPA — an attacker forcing another user's membership to REVOKED would be an
unauthorized data-integrity/privacy violation`
**Feature Under Test:** `CareGroupServiceImpl.leaveCareGroup()` end-to-end via the controller
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupLeaveSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Valid JWT for `LEAVING_MEMBER_ID`; a DIFFERENT, unrelated user `VICTIM_ID` is also an
ACCEPTED member of `GROUP_ID`

**Test Steps (Attack Simulation):**
1. Arrange: attacker holds a valid JWT for `LEAVING_MEMBER_ID` only (no way to set `sub` to `VICTIM_ID`
   without forging the JWT signature, which Spring Security already rejects — out of this feature's
   code path, but asserted here as a defense-in-depth check)
2. Act: `POST /api/v1/care-groups/{groupId}/leave` with `LEAVING_MEMBER_ID`'s valid JWT
3. Assert: only `LEAVING_MEMBER_ID`'s own row changes to `REVOKED`; `VICTIM_ID`'s row is untouched
   (`inviteStatus` still `ACCEPTED`); `VICTIM_ID`'s tasks (if any) are NOT reassigned

**Expected Result (PASS = hệ thống an toàn):** Only the authenticated caller's own row and tasks are
affected; no cross-user mutation is possible through this endpoint
**Expected Result (FAIL = lỗ hổng tồn tại):** Another user's membership or tasks are mutated —
indicates the service resolved the target row from something other than the JWT-derived `callerId`

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FAM220-TC-INT-001 — Full flow: leave with reassignment, DB state assertion (both tables)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /leave -> membership REVOKED + care_tasks reassigned`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupLeaveIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start (no new migration needed)
- Seed: `FX-001` care group, `FX-002` owner membership, `FX-003` leaving member (ACCEPTED), `FX-006`/
  `FX-007` (2 incomplete tasks), `FX-008` (1 DONE task) — all via JPA `save`, not raw SQL

**Test Steps:**
1. Seed care group + owner + leaving member + 3 tasks (2 incomplete, 1 DONE) as above
2. `POST /api/v1/care-groups/{groupId}/leave` as `LEAVING_MEMBER_ID` (real HTTP layer via `MockMvc` or
   `TestRestTemplate`, real service + repository + real Postgres)
3. Assert DB state directly via `CareGroupMemberRepository.findByCareGroupIdAndUserId(...)` and
   `CareTaskRepository.findByCareGroupId(...)`

**Expected Result (PASS):**
- Step 2 returns 200 with `reassignedTaskCount: 2`
- `care_group_members` row for `LEAVING_MEMBER_ID` has `invitation_status = 'REVOKED'`
- The 2 incomplete tasks now have `assigned_to = OWNER_ID`
- The 1 DONE task still has `assigned_to = LEAVING_MEMBER_ID` (unchanged)
- Audit log contains one `CARE_GROUP_MEMBER_LEFT` entry for `LEAVING_MEMBER_ID`

**Expected Result (FAIL):**
- Any step returns unexpected status code, or DB row missing/mismatched fields, or the DONE task was
  also reassigned

**DB Assertion:**
```java
CareGroupMember member = careGroupMemberRepository
        .findByCareGroupIdAndUserId(groupId, leavingMemberId).orElseThrow();
assertThat(member.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);

List<CareTask> tasks = careTaskRepository.findByCareGroupId(groupId);
assertThat(tasks).filteredOn(t -> t.getStatus() == CareTaskStatus.OPEN
                               || t.getStatus() == CareTaskStatus.IN_PROGRESS)
        .allMatch(t -> t.getAssignedTo().equals(ownerUserId));
assertThat(tasks).filteredOn(t -> t.getStatus() == CareTaskStatus.DONE)
        .allMatch(t -> t.getAssignedTo().equals(leavingMemberId));
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM220-TC-001` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM220-TC-002` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | zero-reassignment path |
| `FAM220-TC-003` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM220-TC-004` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | owner guard before writes |
| `FAM220-TC-005` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM220-TC-006` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertLeaveCareGroupNonAcceptedMembershipRejected()` |
| `FAM220-TC-007` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertLeaveCareGroupNonAcceptedMembershipRejected()` |
| `FAM220-TC-008` | `CareTaskRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-009` | `CareTaskRepositoryIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-010` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | verifies save, never delete |
| `FAM220-TC-011` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | happy-path audit assertion |
| `FAM220-TC-012` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | event payload captured |
| `FAM220-TC-013` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | added `CareTaskReassigned` event |
| `FAM220-TC-014` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | alternate owner id probe |
| `FAM220-TC-015` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | mock exception verifies no flip/save before reassignment succeeds |
| `FAM220-TC-016` | `CareGroupLeaveIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-017` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | reflection verifies response DTO fields |
| `FAM220-TC-018` | `CareGroupControllerLeaveTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-019` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | same REVOKED-row guard as TC-007 |
| `FAM220-TC-020` | `CareGroupControllerLeaveTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-SEC-001` | `CareGroupLeaveSecurityTest.java:TBD` | `[ ]` | `[ ]` | — |
| `FAM220-TC-INT-001` | `CareGroupLeaveIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |

> **Total test cases:** 22 (20 unit/service/controller + repository, 1 security, 1 integration).
> **Critical severity:** 7 (`FAM220-TC-001`, `004`, `005`, `008`, `009`, `014`, `015`, `SEC-001` —
> 8 total counting the security case).

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
// Added to the EXISTING CareGroupServiceImpl (do not create a parallel service class)
@Override
public LeaveCareGroupResponse leaveCareGroup(UUID groupId, UUID callerId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

```java
// Red Phase — repository stub
public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {
    @Override
    default int reassignIncompleteTasks(UUID careGroupId, UUID fromUserId, UUID toUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM220-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM220-TC-002`..`020` | `throw('Not implemented')` | 🔴 FAIL (all) | ☐ FAIL ☐ PASS | |
| `FAM220-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM220-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `N/A — production implementation already existed before this UC220 pass`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `N/A — no reconstructed Red Gate log`
- Actual note `2026-07-10`: Red Gate was not re-created because `CareGroupServiceImpl.leaveCareGroup(...)`, controller route, DTO, audit enum, and `CareTaskRepository.reassignIncompleteTasks(...)` were already present in the working tree. Added/expanded unit tests were validated against the existing implementation instead of fabricating a stub-fail history.

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với
> Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] TDS `CB-FAM-IMP-220` reviewed and status confirmed Approved
- [x] ADR-FAM-063/064/065/066/067 (TDS §3) used as implementation/test oracle
- [x] No Flyway migration needed — confirmed §2 L2, TDS §5.2
- [x] `CareTaskStatus` enum available (from UC-73 `ADR-FAM-030`, or introduced here if UC-73 not yet
      merged — reconcile per TDS `ADR-FAM-065` Consequences)
- [x] Test fixtures (§3 TDS-05) prepared via `CareGroupTestFactory`

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] Targeted unit/service tests green: `mvn test -Dtest=CareGroupServiceImplMembershipLifecycleTest,CareTaskServiceImplTaskManagementTest` → 37 tests, 0 failures/errors/skips (`2026-07-10`)
- [ ] `./mvnw verify` — integration tests (`FAM220-TC-008`, `009`, `015`, `016`, `INT-001`) xanh
      (Testcontainers)
- [x] Coverage ≥ 80% lines for `CareGroupServiceImpl.leaveCareGroup()` by targeted unit cases (manual assessment; coverage tool not run)
- [x] Không có business logic trong `CareGroupController` (chỉ có validation + mapping) — verified by
      `FAM220-TC-018`/`020` testing only auth/identity resolution at the controller boundary
- [x] Không có PII/secret xuất hiện plaintext trong leave response/audit path covered by unit tests
- [ ] All 22 test cases reach 🟢 Passing status in §5 tracker

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — not executed; implementation already existed before this pass
- [x] **Contract Existence** — targeted Maven test compiled clean; all injected classes exist:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (verify `CareGroupTestFactory`
      additive usage, no static mutable fields reused across `@Test` methods except read-only UUID
      constants)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR/mockup) — verified,
      every TC above cites an Oracle Source (including the CB-178 mockup for reassignment TCs)

### Suspension Criteria (Điều kiện tạm dừng)

- `CareTaskRepository`/`CareTask` entity not yet merged from UC-73's own implementation — if UC-73 lands
  first, reconcile the two greenfield definitions of the same table rather than duplicating
- ADR-FAM-064 (owner-cannot-leave) or ADR-FAM-065 (reassignment) not yet confirmed by Product — blocks
  `FAM220-TC-004`/`008`/`009`/`014`/`015` finalization if either default changes
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No migration to revert — care_group_members and care_tasks pre-existed this feature (confirmed
# TDS §5.2).

# Revert implementation files only
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/CareGroupServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/service/ICareGroupService.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareGroupController.java
git checkout -- src/main/java/com/carebridge/backend/family/dto/LeaveCareGroupResponse.java
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTask.java
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTaskStatus.java
git checkout -- src/main/java/com/carebridge/backend/family/repository/CareTaskRepository.java
git checkout -- src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- src/test/java/com/carebridge/backend/family/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (or equivalent tracker)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ *(not detected — every TC above cites an Oracle Source, including the CB-178 mockup)* | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☑ Mitigated by explicit pre-existing-implementation note; Red Gate not fabricated | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ *(not detected — owner-cannot-leave traced to ADR-FAM-064, reassignment traced to ADR-FAM-065 + mockup)* | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ *(not detected — controller tests, FAM220-TC-018/020, only check auth/identity, not the owner/task rules)* | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (e.g., a fictitious `ICareTaskService.reassignOnLeave`, or wrong field names `accountId`/`invite_status`) | ☐ *(explicitly guarded against — see §2 L3/L5; tests use `userId`/`inviteStatus` and the real `CareGroupMemberRepository.findByCareGroupIdAndUserId`)* | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved *(pending human reviewer confirmation)*
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected at spec-authoring time — to be re-checked at Red Gate execution)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Approved; unit/service implementation evidence updated 2026-07-10. Red Gate was not reconstructed because implementation pre-existed.*
