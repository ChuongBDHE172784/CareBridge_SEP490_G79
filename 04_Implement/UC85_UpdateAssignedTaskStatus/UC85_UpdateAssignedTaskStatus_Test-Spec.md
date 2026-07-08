# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-85 Update Assigned Task Status

**Document ID:** `CB-FAM-TDD-005`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tên] — Pending`
**DPO Sign-off:** `[ ] Not required — Data Classification: Internal (see TDS §1)`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC85_UpdateAssignedTaskStatus/UC85_UpdateAssignedTaskStatus_TDS.md` (`CB-FAM-IMP-005`)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.3.3 (lines 3275-3294)
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (lines 753-765, 1627)
- ADR-FAM-005 (FSM), ADR-FAM-006 (authorization), ADR-FAM-007 (idempotency) — all in TDS §3

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo TDD spec cho UC-85 Update Assigned Task Status — Status: Draft |

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
| **Feature / Gap ID** | `UC-85` |
| **Module** | `UpdateAssignedTaskStatus — family` |
| **Spec gốc** | `CB-FAM-IMP-005` |
| **Priority** | 🟡 P2 (SRS Priority: Medium) |
| **Sprint** | `S3 Cross-Domain Integration` |
| **Milestone** | Per `04_Implement/implement_artifacts/function-spec-task-allocation.md` lines 476-518 |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `care_tasks` rows created by UC-73 AssignFamilyTask (sibling); `care_group_members` membership state |
| **Downstream Consumers** | Notification module (Open Item), UC-74 ViewSharedCareCalendar (read-side) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-005 §17.1` (C1-C7), `ADR-FAM-005/006/007` |
| **Constraints Injected** | FSM correctness (C1), assignee-only authorization (C2), ACCEPTED membership (C3), Service-layer-only FSM/auth checks (C4), idempotent self-transition (C5), JWT-sourced callerId (C6), no new migration for v1 (C7) |
| **Model** | `Claude (Sonnet)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Bắt buộc điền trước khi viết test. Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS `3.3.3.3` chỉ nói "marks task as in progress, completed, or needing support" — không định nghĩa FSM đầy đủ, không nói `care_tasks.status` có `CHECK` constraint hay không | **Verified: `care_tasks.status varchar(20)` KHÔNG có `CHECK` constraint nào trong toàn bộ migration history** (chỉ có `idx_care_tasks_status` btree index, `V1__init_schema.sql` line 1627). Không giống `community_answers_status_check`, `intake_sessions_status_check`, `emergency_sessions_status_check` là các constraint CÓ tồn tại cho bảng khác. | Toàn bộ FSM enum + transition table trong test cases **KHÔNG được cite là "theo DB constraint"** — phải cite `ADR-FAM-005 (TDS này) — new design decision, not sourced from existing constraint`. Đây là **logic issue #1 quan trọng nhất** của toàn bộ spec này. |
| L2 | SRS không phân biệt rõ "assignee" vs "assigner/owner" khi nói "Family Member ... marks an assigned task" | Task allocation doc xác nhận `3.3.17.7 Update Family Task` là function RIÊNG cho việc sửa/reassign task — do đó UC-85 hẹp hơn, chỉ chuyển status, chỉ dành cho assignee | Test phải cover case group `OWNER` (không phải assignee) bị từ chối 403 — encode `ADR-FAM-006` scope decision, KHÔNG phải hành vi mặc định "group member nào cũng sửa được" |
| L3 | SRS E3 nói "no duplicate unsafe action" nhưng không nói rõ self-transition nên trả lỗi hay no-op | `ADR-FAM-007` quyết định: self-transition = idempotent no-op (200, `changed:false`), KHÔNG phải lỗi 409 | Test phải khẳng định self-transition trả `200`/`changed:false`, KHÔNG trả `409` |
| L4 | Schema có `completed_at` column nhưng SRS không nói khi nào set/không set | Suy luận từ tên cột + ADR-FAM-005: chỉ set khi transition thực sự đổi sang `COMPLETED` (không set lại nếu đã `COMPLETED → COMPLETED` no-op) | Test phải verify `completed_at` chỉ thay đổi khi có real transition vào `COMPLETED`, giữ nguyên khi self-transition |
| L5 | SRS không nói về concurrency/race condition khi 2 request cùng lúc sửa status | `TDS §4.2`: dùng conditional `UPDATE ... WHERE status = :expectedStatus`, không dùng `@Version` (tránh schema change collision với UC-73/74/84) | Test integration phải simulate 2 concurrent calls, verify 1 thành công + 1 nhận `FAM-024` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UpdateAssignedTaskStatus bao gồm các layer:
├── Domain (CareTaskStatus FSM — pure logic, no deps)
├── Services (CareTaskServiceImpl — mock JPA Repository + CareGroupAccessPolicy với Mockito)
├── Controller (CareTaskController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — conditional UPDATE + concurrency)

Mobile (Flutter):
├── Widget test — status update button/dropdown behavior per allowed transitions
└── Integration test — CareTaskService.updateTaskStatus() against mocked API client
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-85` (§3.3.3.3, lines 3275-3294) | Actor = Family Member; target statuses = in progress/completed/needing support; E1 access denied; E2 invalid/conflicting data rejected; E3 no duplicate unsafe action |
| `ADR-FAM-005` (TDS §3) | Full FSM transition table — NEW decision, not DB fact (L1) |
| `ADR-FAM-006` (TDS §3) | Assignee-only authorization; OWNER/assigner excluded; ACCEPTED membership required |
| `ADR-FAM-007` (TDS §3) | Self-transition idempotent no-op semantics |
| BR-RBAC / BR-PRIVACY | Access control tests, no cross-member data leak |
| `V1__init_schema.sql` lines 753-765, 1627 | `care_tasks` columns, absence of CHECK constraint (persistence oracle) |
| `CB-FAM-IMP-005` §9 (API contract) | Request/response schema, error codes FAM-020..026 |

### TDS-03 — Test Conditions and Coverage Items

> State Transition Testing là kỹ thuật CHÍNH của spec này (ISO 29119-4) do FSM combinatorics — mỗi transition (valid + invalid + self) là 1 condition riêng.

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | `OPEN → IN_PROGRESS` (valid) | `CareTaskStatus.canTransitionTo()` + Service | `FAM-UC85-TC-001` |
| TC-COND-002 | `OPEN → COMPLETED` (valid, direct completion) | same + `completed_at` set | `FAM-UC85-TC-002` |
| TC-COND-003 | `OPEN → NEEDS_SUPPORT` (valid) | same | `FAM-UC85-TC-003` |
| TC-COND-004 | `IN_PROGRESS → COMPLETED` (valid) | same + `completed_at` set | `FAM-UC85-TC-004` |
| TC-COND-005 | `IN_PROGRESS → NEEDS_SUPPORT` (valid) | same | `FAM-UC85-TC-005` |
| TC-COND-006 | `NEEDS_SUPPORT → IN_PROGRESS` (valid, recovery) | same | `FAM-UC85-TC-006` |
| TC-COND-007 | `NEEDS_SUPPORT → COMPLETED` (valid) | same + `completed_at` set | `FAM-UC85-TC-007` |
| TC-COND-008 | `COMPLETED → IN_PROGRESS` (invalid — terminal) | FSM rejection → `FAM-022` | `FAM-UC85-TC-008` |
| TC-COND-009 | `COMPLETED → OPEN` (invalid — terminal) | FSM rejection → `FAM-022` | `FAM-UC85-TC-009` |
| TC-COND-010 | `COMPLETED → NEEDS_SUPPORT` (invalid — terminal) | FSM rejection → `FAM-022` | `FAM-UC85-TC-010` |
| TC-COND-011 | `IN_PROGRESS → OPEN` (invalid — no revert-to-OPEN) | FSM rejection → `FAM-022` | `FAM-UC85-TC-011` |
| TC-COND-012 | `NEEDS_SUPPORT → OPEN` (invalid — no revert-to-OPEN) | FSM rejection → `FAM-022` | `FAM-UC85-TC-012` |
| TC-COND-013 | `OPEN → OPEN` (self, idempotent no-op) | `changed:false`, no event | `FAM-UC85-TC-013` |
| TC-COND-014 | `IN_PROGRESS → IN_PROGRESS` (self, idempotent no-op) | same | `FAM-UC85-TC-014` |
| TC-COND-015 | `NEEDS_SUPPORT → NEEDS_SUPPORT` (self, idempotent no-op) | same | `FAM-UC85-TC-015` |
| TC-COND-016 | `COMPLETED → COMPLETED` (self, idempotent no-op — NOT an FSM violation) | same, `completed_at` NOT re-set | `FAM-UC85-TC-016` |
| TC-COND-017 | Assignee + ACCEPTED member → authorized | `CareTaskAuthorizationPolicy.canUpdateStatus()` | `FAM-UC85-TC-017` (covered by TC-001) |
| TC-COND-018 | Non-assignee ACCEPTED member → 403 | same | `FAM-UC85-TC-018` |
| TC-COND-019 | Group OWNER but not assignee → 403 (notable narrowing, ADR-FAM-006) | same | `FAM-UC85-TC-019` |
| TC-COND-020 | Assignee but `invitation_status = PENDING` → 403 | `CareGroupAccessPolicy.isMember()` | `FAM-UC85-TC-020` |
| TC-COND-021 | Assignee but `invitation_status = REVOKED` → 403 | same | `FAM-UC85-TC-021` |
| TC-COND-022 | Task not found | Repository → `FAM-020` | `FAM-UC85-TC-022` |
| TC-COND-023 | Invalid `status` enum value in request body | DTO validation → `FAM-025` | `FAM-UC85-TC-023` |
| TC-COND-024 | Concurrent modification (conditional UPDATE 0 rows) | `ICareTaskRepository.updateStatusConditional()` → `FAM-024` | `FAM-UC85-TC-024` |
| TC-COND-025 | FSM check NOT in Controller (layer violation guard) | Controller unit test — Controller must not import `CareTaskStatus` FSM logic directly | `FAM-UC85-TC-025` |
| TC-COND-026 | `TaskStatusUpdated` event payload correctness (happy path) | Event publisher captured | `FAM-UC85-TC-026` |
| TC-COND-027 | No `TaskStatusUpdated` event on self-transition | Event publisher NOT invoked | `FAM-UC85-TC-027` |
| TC-COND-028 | Full E2E: PATCH via API with valid JWT (assignee) → 200 | E2E | `FAM-UC85-TC-E2E-001` |
| TC-COND-029 | Full E2E: PATCH without JWT → 401 | E2E/Security | `FAM-UC85-TC-E2E-002` |
| TC-COND-030 | Injection attempt in `status` field → 400, no DB side effect | Security | `FAM-UC85-TC-SEC-001` |

**Total conditions: 30** — largest of the three sibling FAM features due to FSM combinatorics (16 transition-oriented cases alone).

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| **State Transition Testing** (PRIMARY) | All 16 FSM transition conditions (TC-COND-001 to 016) | `care_tasks.status` FSM is the core risk surface of this UC (L1) — every valid, invalid, and self-transition must be individually verified since no DB constraint backs the enum |
| Equivalence Partitioning | Authorization roles (assignee / non-assignee / OWNER / PENDING / REVOKED) | Partitions the caller-identity input domain into authorized vs. rejected classes |
| Boundary Value Analysis | Request body `status` field (valid enum vs. unrecognized string vs. empty vs. null) | Boundary of acceptable input for FAM-025 validation |
| Error Guessing | Injection attempt in `status` (TC-SEC-001), concurrent write race (TC-024) | Security/attack vectors + concurrency edge case not explicitly enumerated by SRS |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareTask{status: OPEN, assignedTo: ACC-001, careGroupId: CG-001}` | Baseline for OPEN-origin transitions |
| `FX-002` | DB seed | `CareTask{status: IN_PROGRESS, assignedTo: ACC-001, careGroupId: CG-001}` | Baseline for IN_PROGRESS-origin transitions |
| `FX-003` | DB seed | `CareTask{status: NEEDS_SUPPORT, assignedTo: ACC-001, careGroupId: CG-001}` | Baseline for NEEDS_SUPPORT-origin transitions |
| `FX-004` | DB seed | `CareTask{status: COMPLETED, assignedTo: ACC-001, careGroupId: CG-001, completedAt: <fixed instant>}` | Baseline for terminal-state rejection tests |
| `FX-005` | DB seed | `CareGroupMember{userId: ACC-001, careGroupId: CG-001, invitationStatus: ACCEPTED, memberRole: MEMBER}` | Assignee, valid member |
| `FX-006` | DB seed | `CareGroupMember{userId: ACC-002, careGroupId: CG-001, invitationStatus: ACCEPTED, memberRole: MEMBER}` | Non-assignee ACCEPTED member |
| `FX-007` | DB seed | `CareGroupMember{userId: ACC-003, careGroupId: CG-001, invitationStatus: ACCEPTED, memberRole: OWNER}` | Group OWNER, not assignee |
| `FX-008` | DB seed | `CareGroupMember{userId: ACC-001, careGroupId: CG-001, invitationStatus: PENDING}` (override for TC-020) | Assignee whose membership is no longer ACCEPTED |
| `FX-009` | DB seed | `CareGroupMember{userId: ACC-001, careGroupId: CG-001, invitationStatus: REVOKED}` (override for TC-021) | Assignee whose membership was revoked |
| `FX-010` | JWT | `{sub: 'ACC-001', role: 'ROLE_FAMILY_MEMBER'}` | Auth context — assignee |
| `FX-011` | env | Fixed clock via `Clock.fixed(...)` injected into service | Deterministic `updatedAt`/`completedAt` assertions |

---

## 4. Test Case Specification

> **TC ID format:** `FAM-UC85-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written (all test cases in this document)

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeCareTask()
// ═══════════════════════════════════════════════════════════

// CareTaskTestFactory.java
class CareTaskTestFactory {

    static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final UUID ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000a01"); // ACC-001
    static final UUID NON_ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000a02"); // ACC-002
    static final UUID OWNER_NOT_ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000a03"); // ACC-003

    // Giá trị baseline hợp lệ — đồng bộ với FX-001 (§3 TDS-05)
    static CareTask makeCareTask(CareTaskStatus status) {
        CareTask task = new CareTask();
        task.setCareTaskId(TASK_ID);
        task.setCareGroupId(GROUP_ID);
        task.setAssignedTo(ASSIGNEE_ID);
        task.setAssignedBy(OWNER_NOT_ASSIGNEE_ID);
        task.setTitle("Buy prenatal vitamins");
        task.setStatus(status);
        task.setCompletedAt(status == CareTaskStatus.COMPLETED ? Instant.parse("2026-07-01T00:00:00Z") : null);
        task.setCreatedAt(Instant.parse("2026-06-30T00:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-06-30T00:00:00Z"));
        return task;
    }

    // Overload để override specific fields
    static CareTask makeCareTask(CareTaskStatus status, Consumer<CareTask> overrides) {
        CareTask task = makeCareTask(status);
        overrides.accept(task);
        return task;
    }

    static CareGroupMember makeAcceptedMember(UUID userId, GroupMemberRole role) {
        CareGroupMember m = new CareGroupMember();
        m.setCareGroupId(GROUP_ID);
        m.setUserId(userId);
        m.setMemberRole(role.name());
        m.setInvitationStatus(InviteStatus.ACCEPTED);
        return m;
    }

    static CareGroupMember makeMemberWithStatus(UUID userId, InviteStatus status) {
        CareGroupMember m = new CareGroupMember();
        m.setCareGroupId(GROUP_ID);
        m.setUserId(userId);
        m.setMemberRole("MEMBER");
        m.setInvitationStatus(status);
        return m;
    }

    static UpdateTaskStatusRequest makeRequest(String status) {
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
        req.setStatus(status);
        return req;
    }
}
```

---

### VALID TRANSITION TEST CASES (State Transition Testing — PRIMARY)

---

### FAM-UC85-TC-001 — OPEN → IN_PROGRESS (valid transition)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-FAM-005 (this TDS, CB-FAM-IMP-005 §3) — new design decision, not sourced from existing DB constraint` + `SRS UC-85 Description (line 3282)`

**Preconditions:**
- `FX-001`: `CareTask{status: OPEN, assignedTo: ASSIGNEE_ID}`
- `FX-005`: `ASSIGNEE_ID` is ACCEPTED member of `GROUP_ID`

**Test Steps:**
1. Arrange: `CareTaskTestFactory.makeCareTask(OPEN)`, mock repository `findById` returns it; mock `accessPolicy.isMember()` returns `true`
2. Act: call `service.updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Assert: response `changed=true`, `previousStatus=OPEN`, `newStatus=IN_PROGRESS`, `completedAt=null`

**Expected Result (PASS — hành vi đúng):**
- `updateStatusConditional(TASK_ID, "OPEN", "IN_PROGRESS")` called exactly once
- `TaskStatusUpdated` event published with `oldStatus=OPEN, newStatus=IN_PROGRESS`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or `changed=false`, or wrong repository method invoked

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-002 — OPEN → COMPLETED (valid, direct completion)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision` + `SRS UC-85 Description: "marks ... as ... completed"` (direct completion without an intermediate IN_PROGRESS marker is explicitly implied by this wording)

**Preconditions:** `FX-001` (OPEN), `FX-005` (ACCEPTED assignee)

**Test Steps:**
1. Arrange `makeCareTask(OPEN)`
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("COMPLETED"))`
3. Assert `completedAt` is set to fixed clock instant (FX-011)

**Expected Result (PASS):** `changed=true`, `completedAt != null`, `newStatus=COMPLETED`

**Expected Result (FAIL):** `completedAt` remains null, or transition rejected

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-003 — OPEN → NEEDS_SUPPORT (valid)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision` + `SRS UC-85 Description: "marks ... as ... needing support"`

**Preconditions:** `FX-001` (OPEN), `FX-005`

**Expected Result (PASS):** `changed=true`, `newStatus=NEEDS_SUPPORT`, `completedAt=null`

**Expected Result (FAIL):** transition rejected or wrong status persisted

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-004 — IN_PROGRESS → COMPLETED (valid)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-002` (IN_PROGRESS), `FX-005`

**Expected Result (PASS):** `changed=true`, `completedAt` set, `newStatus=COMPLETED`

**Expected Result (FAIL):** `completedAt` not set or transition rejected

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-005 — IN_PROGRESS → NEEDS_SUPPORT (valid)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-002` (IN_PROGRESS), `FX-005`

**Expected Result (PASS):** `changed=true`, `newStatus=NEEDS_SUPPORT`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-006 — NEEDS_SUPPORT → IN_PROGRESS (valid, recovery)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision (recovery transition)`

**Preconditions:** `FX-003` (NEEDS_SUPPORT), `FX-005`

**Expected Result (PASS):** `changed=true`, `newStatus=IN_PROGRESS`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-007 — NEEDS_SUPPORT → COMPLETED (valid)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-003` (NEEDS_SUPPORT), `FX-005`

**Expected Result (PASS):** `changed=true`, `completedAt` set, `newStatus=COMPLETED`

**Current Status:** 🔴 Not written

---

### INVALID TRANSITION TEST CASES (State Transition Testing — E2 oracle)

---

### FAM-UC85-TC-008 — COMPLETED → IN_PROGRESS (invalid — terminal state)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision, invariant: COMPLETED is terminal` + `SRS UC-85 E2: "Invalid ... or conflicting data is rejected"` (line 3287)

**Preconditions:** `FX-004` (COMPLETED), `FX-005`

**Test Steps:**
1. Arrange `makeCareTask(COMPLETED)`
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Assert `ConflictException` thrown with code `FAM-022`

**Expected Result (PASS):** `ConflictException` with `FAM-022`, no repository write called

**Expected Result (FAIL):** transition succeeds (invariant violated), or wrong error code

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-009 — COMPLETED → OPEN (invalid — terminal state)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-004` (COMPLETED), `FX-005`

**Expected Result (PASS):** `ConflictException` `FAM-022`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-010 — COMPLETED → NEEDS_SUPPORT (invalid — terminal state)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-004` (COMPLETED), `FX-005`

**Expected Result (PASS):** `ConflictException` `FAM-022`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-011 — IN_PROGRESS → OPEN (invalid — no revert-to-OPEN)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision (no SRS-defined "revert to OPEN" path)`

**Preconditions:** `FX-002` (IN_PROGRESS), `FX-005`

**Expected Result (PASS):** `ConflictException` `FAM-022`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-012 — NEEDS_SUPPORT → OPEN (invalid — no revert-to-OPEN)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-FAM-005 (this TDS) — new design decision`

**Preconditions:** `FX-003` (NEEDS_SUPPORT), `FX-005`

**Expected Result (PASS):** `ConflictException` `FAM-022`

**Current Status:** 🔴 Not written

---

### SELF-TRANSITION (IDEMPOTENCY) TEST CASES

---

### FAM-UC85-TC-013 — OPEN → OPEN (self, idempotent no-op)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-FAM-007 (this TDS) — new design decision` + `SRS UC-85 E3: "no duplicate unsafe action"` (line 3287)

**Preconditions:** `FX-001` (OPEN), `FX-005`

**Test Steps:**
1. Arrange `makeCareTask(OPEN)`
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("OPEN"))`
3. Assert response `changed=false`, `updateStatusConditional` NOT invoked, no event published

**Expected Result (PASS):** `200`-equivalent response, `changed=false`, no repository write, no event

**Expected Result (FAIL):** `ConflictException` thrown (treats self-transition as error — violates ADR-FAM-007), or duplicate write/event occurs

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-014 — IN_PROGRESS → IN_PROGRESS (self, idempotent no-op)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-FAM-007 (this TDS) — new design decision`

**Preconditions:** `FX-002` (IN_PROGRESS), `FX-005`

**Expected Result (PASS):** `changed=false`, no write, no event

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-015 — NEEDS_SUPPORT → NEEDS_SUPPORT (self, idempotent no-op)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `ADR-FAM-007 (this TDS) — new design decision`

**Preconditions:** `FX-003` (NEEDS_SUPPORT), `FX-005`

**Expected Result (PASS):** `changed=false`, no write, no event

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-016 — COMPLETED → COMPLETED (self, idempotent no-op — NOT an FSM violation)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-FAM-007 (this TDS) — new design decision — distinguishes self-transition-on-terminal-state (allowed no-op) from distinct-state-transition-out-of-terminal (rejected, see TC-008/009/010)`

**Preconditions:** `FX-004` (COMPLETED, `completedAt` = fixed instant `2026-07-01T00:00:00Z`), `FX-005`

**Test Steps:**
1. Arrange `makeCareTask(COMPLETED)` with `completedAt` fixed
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("COMPLETED"))`
3. Assert `changed=false`, `completedAt` UNCHANGED (still `2026-07-01T00:00:00Z`, not re-stamped to "now")

**Expected Result (PASS):** `changed=false`, `completedAt` identical to precondition value, no event published

**Expected Result (FAIL):** `ConflictException` thrown (this must NOT be treated as an FSM violation, since `this == target` is checked before the terminal-state rejection branch per `CareTaskStatus.canTransitionTo()` §8.1 of TDS), or `completedAt` incorrectly re-stamped

**Current Status:** 🔴 Not written

---

### AUTHORIZATION BOUNDARY TEST CASES

---

### FAM-UC85-TC-018 — Non-assignee ACCEPTED member → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskAuthorizationPolicy.canUpdateStatus()` via `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-FAM-006 (this TDS) — new scope decision` + `SRS UC-85 E1: "Access is denied when ... outside the permitted data scope"` (line 3287)

**Preconditions:** `FX-001` (OPEN, `assignedTo=ASSIGNEE_ID`), `FX-006` (`NON_ASSIGNEE_ID` is ACCEPTED member of same group, different account)

**Test Steps:**
1. Arrange task assigned to `ASSIGNEE_ID`; caller = `NON_ASSIGNEE_ID`
2. Act `updateStatus(TASK_ID, NON_ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Assert `ForbiddenException` with code `FAM-021`

**Expected Result (PASS):** `ForbiddenException` `FAM-021`, no repository write

**Expected Result (FAIL):** transition succeeds despite caller not being the assignee

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-019 — Group OWNER but not assignee → 403 (notable narrowing)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskAuthorizationPolicy.canUpdateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `ADR-FAM-006 (this TDS) — new scope decision: group OWNER role does NOT bypass the assignee check` (explicit design decision, not sourced from SRS, which does not mention OWNER override)

**Preconditions:** `FX-001` (OPEN, `assignedTo=ASSIGNEE_ID`), `FX-007` (`OWNER_NOT_ASSIGNEE_ID` is ACCEPTED `OWNER` of same group)

**Test Steps:**
1. Arrange caller = `OWNER_NOT_ASSIGNEE_ID` with `memberRole=OWNER`, `invitationStatus=ACCEPTED`
2. Act `updateStatus(TASK_ID, OWNER_NOT_ASSIGNEE_ID, makeRequest("COMPLETED"))`
3. Assert `ForbiddenException` `FAM-021` — even though caller IS a valid ACCEPTED group member with elevated role

**Expected Result (PASS):** `ForbiddenException` `FAM-021` despite OWNER role — proves authorization is narrower than plain group membership

**Expected Result (FAIL):** transition succeeds for OWNER (would silently expand scope beyond ADR-FAM-006, a regression against the explicit design decision)

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-020 — Assignee but invitation_status = PENDING → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()` via `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `ADR-FAM-006 (this TDS) — defense-in-depth decision` + `ADR-FAM-002 (UC-216 TDS, reused isMember() check requiring invitation_status=ACCEPTED)`

**Preconditions:** `FX-008` (`ASSIGNEE_ID` membership overridden to `PENDING`), task still has `assignedTo=ASSIGNEE_ID`

**Test Steps:**
1. Arrange `assignedTo=ASSIGNEE_ID` but caller's `care_group_members.invitation_status=PENDING`
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Assert `ForbiddenException` `FAM-023`

**Expected Result (PASS):** `ForbiddenException` `FAM-023` — even though `assigned_to` matches, membership is not ACCEPTED

**Expected Result (FAIL):** transition succeeds despite non-ACCEPTED membership

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-021 — Assignee but invitation_status = REVOKED → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()` via `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `ADR-FAM-006 (this TDS) — defense-in-depth decision`

**Preconditions:** `FX-009` (`ASSIGNEE_ID` membership overridden to `REVOKED`)

**Expected Result (PASS):** `ForbiddenException` `FAM-023`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-022 — Task not found → 404

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()`
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `CB-FAM-IMP-005 §10 Error Codes (FAM-020)`

**Preconditions:** `taskId` does not exist in mocked repository (`findById` returns `Optional.empty()`)

**Expected Result (PASS):** `NotFoundException` `FAM-020`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-023 — Invalid status enum value in request → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateTaskStatusRequest` validation / `CareTaskServiceImpl.updateStatus()` enum mapping
**Test File:** same as TC-001 (unit) + `CareTaskControllerTest.java` (validation layer)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `CB-FAM-IMP-005 §10 Error Codes (FAM-025)` + `SRS UC-85 E2`

**Preconditions:** request body `{"status": "DONE"}` (not a valid `CareTaskStatus` enum name)

**Test Steps:**
1. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("DONE"))`
2. Assert `ValidationException`/`IllegalArgumentException` mapped to `FAM-025`, no repository write

**Expected Result (PASS):** `400` response with `FAM-025`, `details` naming the `status` field

**Expected Result (FAIL):** `500` unhandled exception, or transition silently accepted with garbage status

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-024 — Concurrent modification detected → 409

**Severity:** `HIGH`
**Feature Under Test:** `ICareTaskRepository.updateStatusConditional()` via `CareTaskServiceImpl.updateStatus()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java` (unit, mocked 0-rows-affected) + `CareTaskIntegrationTest.java` (integration, real race)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-024`
**Oracle Source:** `CB-FAM-IMP-005 §4.2 (Concurrency strategy)` — new design decision, not sourced from a DB-level lock/version column (explicitly avoided per ADR-FAM-005 §5.2 to prevent schema collision with sibling UC-73/74/84)

**Preconditions:** `FX-001` (OPEN); mock `updateStatusConditional()` to return `0` (simulating that another request already changed status between read and write)

**Test Steps (Unit):**
1. Arrange `findById` returns `OPEN` task, but `updateStatusConditional(TASK_ID, "OPEN", "IN_PROGRESS")` mocked to return `0`
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Assert `ConflictException` `FAM-024`

**Test Steps (Integration — Testcontainers):**
1. Seed one `OPEN` task
2. Fire two concurrent `updateStatus()` calls targeting different statuses (`IN_PROGRESS` and `NEEDS_SUPPORT`) via parallel threads/`CompletableFuture`
3. Assert exactly one succeeds (`changed=true`), the other receives `FAM-024`
4. Assert final DB state matches whichever call's conditional UPDATE won (no lost update, no both-succeed anomaly)

**Expected Result (PASS):** exactly 1 winner, 1 `FAM-024` rejection, DB consistent with winner only

**Expected Result (FAIL):** both succeed (lost update bug), or both fail, or DB state inconsistent with either caller's intent

**Current Status:** 🔴 Not written

---

### LAYER-VIOLATION GUARD TEST CASE

---

### FAM-UC85-TC-025 — FSM validation must live in Service layer, not Controller

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskController` (architectural guard)
**Test File:** `src/test/java/com/carebridge/backend/family/controller/CareTaskControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-025`
**Oracle Source:** `CB-FAM-IMP-005 §17.1 C4 — ADR-FAM-005` + `CLAUDE.md (Controller: validation/mapping only, no business logic)`

**Preconditions:** `@WebMvcTest(CareTaskController.class)` with `ICareTaskService` mocked

**Test Steps:**
1. Mock `careTaskService.updateStatus(...)` to throw `ConflictException(FAM-022)` directly (simulating that FSM rejection originates from Service, not Controller)
2. Call controller endpoint via `MockMvc`
3. Assert Controller propagates the exception/response as-is (maps to `409`) WITHOUT itself invoking any `CareTaskStatus.canTransitionTo()` logic — verified by code inspection: `CareTaskController` source must not import `CareTaskStatus`

**Expected Result (PASS):** `409` response correctly surfaced from a Service-thrown exception; static check confirms `CareTaskController.java` has no direct dependency on `CareTaskStatus` FSM methods

**Expected Result (FAIL):** Controller contains its own `if (status == ...)` FSM branching (layer violation, AP-AI-004)

**Current Status:** 🔴 Not written

---

### DOMAIN EVENT TEST CASES

---

### FAM-UC85-TC-026 — TaskStatusUpdated event payload correctness (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()` event publishing
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-026`
**Oracle Source:** `CB-FAM-IMP-005 §7.3 (Payload Schema)`

**Preconditions:** `FX-001` (OPEN), `FX-005`

**Test Steps:**
1. Arrange `ApplicationEventPublisher` spy/mock
2. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))`
3. Capture published `TaskStatusUpdated` event

**Expected Result (PASS):** event payload has `taskId=TASK_ID`, `careGroupId=GROUP_ID`, `oldStatus=OPEN`, `newStatus=IN_PROGRESS`, `updatedBy=ASSIGNEE_ID`, `assignedBy=OWNER_NOT_ASSIGNEE_ID`

**Expected Result (FAIL):** event missing, or fields incorrect/swapped

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-027 — No TaskStatusUpdated event on self-transition

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.updateStatus()` event publishing
**Test File:** same as TC-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `ADR-FAM-007 (this TDS) — new design decision`

**Preconditions:** `FX-002` (IN_PROGRESS), `FX-005`

**Test Steps:**
1. Act `updateStatus(TASK_ID, ASSIGNEE_ID, makeRequest("IN_PROGRESS"))` (self-transition)
2. Assert `eventPublisher.publishEvent(any(TaskStatusUpdated.class))` NEVER invoked

**Expected Result (PASS):** zero interactions with event publisher for `TaskStatusUpdated`

**Expected Result (FAIL):** duplicate event published for a no-op (violates SRS E3 "no duplicate unsafe action")

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FAM-UC85-TC-INT-001 — Full flow: happy path status transition persisted to DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: CareTaskController → CareTaskServiceImpl → ICareTaskRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/family/CareTaskIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-004`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrated
- Seed: `care_tasks` row with `status='OPEN'`, `assigned_to=ASSIGNEE_ID`; `care_group_members` row ACCEPTED

**Test Steps:**
1. Seed `OPEN` task
2. Call `PATCH .../status` with `{"status":"IN_PROGRESS"}`
3. Call again with `{"status":"COMPLETED"}`
4. Assert DB state after each call

**Expected Result (PASS):**
- After step 2: DB row `status='IN_PROGRESS'`, `completed_at IS NULL`
- After step 3: DB row `status='COMPLETED'`, `completed_at IS NOT NULL`

**DB Assertion:**
```java
CareTask record = careTaskRepository.findById(TASK_ID).orElseThrow();
assertThat(record.getStatus()).isEqualTo(CareTaskStatus.COMPLETED);
assertThat(record.getCompletedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### FAM-UC85-TC-SEC-001 — Injection attempt in status field rejected safely

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection` (defense verification, not an actual vulnerability expectation)
**Legal:** `BR-PRIVACY — no unauthorized data manipulation`
**Feature Under Test:** `UpdateTaskStatusRequest` validation + `CareTaskServiceImpl` enum mapping
**Test File:** same as TC-023
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-030`

**Preconditions:** `FX-001` (OPEN), `FX-005`

**Test Steps (Attack Simulation):**
1. Send request body `{"status": "COMPLETED'; DROP TABLE care_tasks; --"}`
2. Call `updateStatus()`
3. Assert rejected with `400 FAM-025` (invalid enum value), no DB mutation, `care_tasks` table intact

**Expected Result (PASS = hệ thống an toàn):** `400 FAM-025`, table `care_tasks` row count unchanged, no raw SQL string concatenation anywhere in `updateStatusConditional` (parameterized native query per TDS §8.2)

**Expected Result (FAIL = lỗ hổng tồn tại):** exception other than validation error, or any DB schema/data corruption

**Current Status:** 🔴 Not written

---

### E2E TEST CASES

---

### FAM-UC85-TC-E2E-001 — Assignee PATCH via API with valid JWT → 200

**Severity:** `HIGH`
**Feature Under Test:** `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status`
**Test File:** `src/test/java/com/carebridge/backend/family/CareTaskE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-028`

**Preconditions:** Seeded `OPEN` task, valid JWT for `ASSIGNEE_ID` (FX-010)

**Test Steps:**
1. `PATCH` with `Authorization: Bearer <assignee JWT>`, body `{"status":"IN_PROGRESS"}`
2. Assert `200`, response body matches `CB-FAM-IMP-005 §9.2` happy-path schema

**Expected Result (PASS):** `200`, `changed=true`

**Current Status:** 🔴 Not written

---

### FAM-UC85-TC-E2E-002 — PATCH without JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** Spring Security filter chain on `CareTaskController`
**Test File:** same as E2E-001
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-029`
**Oracle Source:** `SRS UC-85 E1: "Access is denied when the actor is unauthenticated"` (line 3287)

**Test Steps:**
1. `PATCH` without `Authorization` header
2. Assert `401`

**Expected Result (PASS):** `401 Unauthorized`

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM-UC85-TC-001` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-002` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-003` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-004` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-005` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-006` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-007` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-008` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-009` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-010` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-011` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-012` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-013` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-014` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-015` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-016` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-018` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-019` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-020` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-021` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-022` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-023` | `CareTaskServiceImplTest.java` / `CareTaskControllerTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-024` | `CareTaskServiceImplTest.java` / `CareTaskIntegrationTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-025` | `CareTaskControllerTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-026` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-027` | `CareTaskServiceImplTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-INT-001` | `CareTaskIntegrationTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-SEC-001` | `CareTaskControllerTest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-E2E-001` | `CareTaskE2ETest.java` | `[ ]` | `___` | — |
| `FAM-UC85-TC-E2E-002` | `CareTaskE2ETest.java` | `[ ]` | `___` | — |

**Total test cases: 30** (16 FSM transition cases + 6 authorization boundary cases + 8 supporting cases [not-found, validation, concurrency, layer-guard, 2 event cases, integration, security] + 2 E2E). This is the largest test count of the three sibling FAM features (UC-70, UC-216) due to FSM combinatorics.

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CareTaskServiceImpl implements ICareTaskService {

    @Override
    public UpdateTaskStatusResponse updateStatus(UUID taskId, UUID callerId, UpdateTaskStatusRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class CareTaskAuthorizationPolicy {
    public boolean canUpdateStatus(CareTask task, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM-UC85-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM-UC85-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-UC85-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-UC85-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-UC85-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM-UC85-TC-024` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| *(remaining 24 TCs — same expected result, table abbreviated; full verification log recorded at Red Gate execution time)* | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___` (to be filled at implementation time)

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-005` đã được review và approve (Status hiện tại: `Draft`)
- [ ] Logic Issues (Section 2, đặc biệt L1 — absence of DB CHECK constraint) đã được confirm với Tech Lead
- [ ] UC-73 AssignFamilyTask đã tạo được ít nhất 1 seed row trong `care_tasks` để test integration
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `CareTaskServiceImpl`
- [ ] Không có business logic trong `CareTaskController` (chỉ validation + mapping) — verified bởi `FAM-UC85-TC-025`
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Mọi 16 FSM transition conditions (valid + invalid + self) có test case PASS

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 30 tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua `CareTaskTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-FAM-005/006/007 cho FSM/auth/idempotency, hoặc SRS E1/E2/E3, hoặc TDS §7/§8/§10 cho events/interfaces/error codes)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-73 chưa có seed data khả dụng cho integration tests
- Phát hiện lỗi kiến trúc mới cần Tech Lead review (vd: FSM table cần thay đổi)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới cho v1 — không cần revert DB
# Nếu optional migration V20260702100200 đã được apply (xem TDS §5.2):
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.care_tasks DROP CONSTRAINT IF EXISTS care_tasks_status_check;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260702100200';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTask.java
git checkout -- src/main/java/com/carebridge/backend/family/entity/CareTaskStatus.java
git checkout -- src/main/java/com/carebridge/backend/family/repository/ICareTaskRepository.java
git checkout -- src/main/java/com/carebridge/backend/family/service/ICareTaskService.java
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/CareTaskAuthorizationPolicy.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareTaskController.java
git checkout -- src/test/java/com/carebridge/backend/family/

# Gap vẫn OPEN → giữ nguyên entry trong sprint tracker
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (vd: cho phép OWNER update mà không cite ADR-FAM-006 override) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic FSM (kiểm tra bởi TC-025) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (vd: giả định `NotificationService` tồn tại — xem Open Item TDS §7.2) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Phát hiện lưu ý cần theo dõi khi implement → ghi vào bảng dưới

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-005` (potential) | N/A — Open Item | `TaskStatusUpdated` consumer (notification) chưa xác nhận class tồn tại trong codebase | Giữ nguyên là Open Item ở TDS §7.2 — KHÔNG implement consumer wiring trong scope UC-85; chỉ publish event | ☐ (deferred — out of UC-85 scope) |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending TDS `CB-FAM-IMP-005` approval trước khi bắt đầu Red Phase.*
