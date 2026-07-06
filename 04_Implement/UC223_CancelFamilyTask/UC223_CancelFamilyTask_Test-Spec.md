# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC223 — Cancel Family Task — Test Specification

**Document ID:** `FPT-EDU-TDD-TEMPLATE-001` (instance for `CB-FAM-IMP-223`)
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (`care_tasks` table, no `cancelled_at`/`cancellation_reason` column, no CHECK constraint on `status`)
- `04_Implement/UC223_CancelFamilyTask/UC223_CancelFamilyTask_TDS.md` (`CB-FAM-IMP-223`) — companion Technical Specification
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.8 (Table 245) — Functional requirements (UC-223): "Cancels a task and notifies related members."
- `04_Implement/UC73_AssignFamilyTask/UC73_AssignFamilyTask_TDS.md` — ADR-FAM-030 (`CareTaskStatus` enum), ADR-FAM-032 (owner-only), FAM-033 reservation
- `04_Implement/UC221_ViewAssignedTaskDetail/UC221_ViewAssignedTaskDetail_TDS.md` — ADR-FAM-070 (`CareTask` naming, superseded by ADR-FAM-077 for this batch)
- `04_Implement/UC222_UpdateFamilyTask/UC222_UpdateFamilyTask_TDS.md` — ADR-FAM-072 (owner-only update predicate, reused), ADR-FAM-073 (incomplete-task precedent for the already-terminal-rejected pattern), ADR-FAM-074 (content-only boundary — the reciprocal of this UC), ADR-FAM-077 (`CareTaskEntity` naming)
- ADR-FAM-079/080/081/082/083 — see TDS §3
- PDPA (Vietnam) — minimum-necessary access; *(Luật 91/2025, NĐ 356/2025 — Not applicable; no specific article identified, generic BR-RBAC/BR-PRIVACY cited instead)*

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC-223 Cancel Family Task |

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
| **Feature / Gap ID** | `GAP-FAM223` |
| **Module** | `family` — Care Task Cancellation (status-transition-only) |
| **Spec gốc** | `CB-FAM-IMP-223` |
| **Priority** | 🟠 P1 (Medium per SRS) |
| **Sprint** | `Sprint 3 — Cross-Domain Integration` *(mirrors sibling UC-73/UC-222 batch; exact date not confirmed — Open, do not invent)* |
| **Milestone** | `M3 Alpha` *(not confirmed in available sources — Open)* |
| **Data Classification** | `Internal` (family/task identity + notification trigger — see TDS §1) |
| **Compliance Scope** | `PDPA`; `BR-RBAC`, `BR-PRIVACY` |
| **Upstream Dependencies** | `CareGroup`, `CareGroupMember` (UC-70/216, implemented), `care_tasks` rows created by UC-73 (sibling workstream, not created here), notification module (FCM consumer of `CareTaskCancelled`) |
| **Downstream Consumers** | Notification module (member cancel-notice, recipient policy `Open`); read-side sibling UC-221; shared task-list/calendar readers of `care_tasks.status` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-223 §17`, `ADR-FAM-079/080/081/082/083` |
| **Constraints Injected** | C1 (reuse existing controller), C2 (no entity leakage), C3 (reuse `CareTaskStatus` enum, no redefinition), C4 (write ONLY `status`, never content fields), C5 (owner/precondition/idempotency checks in Service/Policy layer), C6 (identity from `SecurityUtils`), C7 (event published only on success; no hard-coded recipients), C8 (no migration, no hard delete) — see TDS §17.1 |
| **Model** | `Claude (Technical Architect + Test Designer agent)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | No `CareTask` JPA entity/repository/service exists yet for `care_tasks` (greenfield); UC-221's draft informally used `CareTask` (ADR-FAM-070) while UC-222 chose `CareTaskEntity` (ADR-FAM-077) | This TDS's ADR-FAM-083 reuses `CareTaskEntity` (the latest sibling decision) explicitly | Tests reference `CareTaskEntity` / `CareTaskRepository` / `ICareTaskService` per TDS §8; a rename note is left in the Red Gate stub if a sibling lands first with a different name |
| L2 | SRS §3.3.17.8 names only "Mother" and BR-RBAC generically — does not literally say "owner-only" | UC-73's ADR-FAM-032 and UC-222's ADR-FAM-072 already established owner-only mutation for the same aggregate; ADR-FAM-079 extends this symmetrically to cancel | Authorization test cases assert Owner-only (`memberRole==OWNER && inviteStatus==ACCEPTED`); non-owner ACCEPTED members (MEMBER/VIEWER) and non-members are both rejected with `FAM-079`, tested as distinct cases |
| L3 | SRS Description does not enumerate which statuses are cancellable | Canonical enum (ADR-FAM-030, reused not redefined): `{OPEN, IN_PROGRESS, DONE, CANCELLED}`. Cancellable = `OPEN` ∪ `IN_PROGRESS` per ADR-FAM-080, mirroring UC-222's "incomplete" precedent (ADR-FAM-073) | Test cases cover all 4 statuses as the *starting* state: OPEN/IN_PROGRESS succeed, DONE rejected `FAM-080`, CANCELLED rejected `FAM-081` — full state-transition-testing coverage per ISO 29119-4 |
| L4 | Ambiguous whether re-cancelling an already-`CANCELLED` task should be a silent no-op (200) or an error | ADR-FAM-081 decision: **error** (409 `FAM-081`), by precedent from UC-222's ADR-FAM-073 (which rejects any mutation of a terminal-state task rather than silently succeeding); chosen specifically to avoid a duplicate `CareTaskCancelled` notification | `FAM223-TC-004` asserts a re-cancel on `CANCELLED` returns 409 `FAM-081`; `FAM223-TC-012` asserts **no second** `CareTaskCancelled` event is published on that rejected path |
| L5 | Ambiguous whether "cancel" means a hard `DELETE` of the row or a soft `status` transition | ADR-FAM-080 decision: **soft transition only** — `status` set to `CANCELLED`, row retained, no `DELETE`; no `cancelled_at` column exists (verified against `V1__init_schema.sql`) | `FAM223-TC-016` asserts the row still exists (`findById` succeeds) after a successful cancel — proves soft-transition, not hard-delete |
| L6 | SRS says "notifies related members" but does not enumerate the recipient set (assignee only? assigner? whole group?) | ADR-FAM-082 marks the exact recipient list **Open (OPEN-2)** — confirmed genuinely unresolved: UC-73 (assignee-only, but a *different* trigger), UC-85 §7.2 (Open Item, guesses `assigned_by`), and UC-222 §7.2 (its own OPEN-2, also unresolved) show **no established batch convention to reuse**; the `CareTaskCancelled` payload is designed to carry `assignedTo`/`assignedBy`/`careGroupId`/`cancelledBy` so a future consumer can resolve the audience | `FAM223-TC-010` asserts the event payload shape/fields are present and correct — it does **not** assert a specific recipient list (**NEEDS-DECISION**: Product/Tech Lead must pick assignee-only / assignee+assigner / whole-group before a recipient-specific test can be written) |
| L7 | The three UI mockups tagged UC-223 in their folder names (`CB-021`, `CB-029`, `CB-170`) contain **zero** cancel affordance (`cancel`/`hủy`/`huỷ`/`lý do`/`reason` search returned no matches in any of the three, re-verified 2026-07-04 by both keyword search and manual button/icon inspection — see TDS ADR-FAM-083) | No confirmation-dialog copy oracle exists; the API is designed for a generic confirm-then-call pattern | No test case asserts specific confirmation-dialog UI copy (there is none to assert against); this is a confirmed **design-backlog item for UI/UX** (OPEN-3), not a spec ambiguity or a coverage gap — the API-level tests below are unaffected since they operate below the UI layer |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Family / CareTask Cancel (UC-223) bao gồm các layer:
├── Domain (CareTaskEntity — pure data; CareTaskStatus reused from UC-73, no new logic)
├── Policy (CareTaskAuthorizationPolicy.canCancelTask — mock CareGroupMemberRepository với Mockito)
├── Services (CareTaskServiceImpl.cancelFamilyTask — mock CareTaskRepository, CareTaskAuthorizationPolicy, AuditService, ApplicationEventPublisher với Mockito)
├── Controller (CareGroupController.cancelTask — mock ICareTaskService với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, full cancel flow through real repositories, asserting status/content invariant + audit + event end-to-end)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-223` (§3.3.17.8) | Happy path (cancel an incomplete task, notify related members), E1 (access denied), E2 (invalid/conflicting data rejected), POST-1/POST-2/POST-3 (result state, notification, audit) |
| `ADR-FAM-079` | Owner-only authorization — assert MEMBER/VIEWER/non-member all rejected `FAM-079` |
| `ADR-FAM-080` | Cancellable-status gate — assert OPEN/IN_PROGRESS accepted, DONE rejected `FAM-080`; soft transition (no hard delete) |
| `ADR-FAM-081` | Idempotency-as-error — assert re-cancelling an already-`CANCELLED` task is rejected `FAM-081`, and no duplicate event is published |
| `ADR-FAM-082` | `CareTaskCancelled` event design — assert payload shape/fields; recipient list explicitly **not** asserted (Open) |
| `ADR-FAM-083` | Content-immutability — assert `title`/`description`/`due_at`/`assigned_to`/`completed_at` never change; entity-name reuse (`CareTaskEntity`) |
| `ADR-FAM-030`/`032`/`072` (reused) | `CareTaskStatus` enum values; owner-only predicate identical to `canUpdateTask`/`canAssignTasks` |
| `BR-RBAC` / `BR-PRIVACY` | Authorization tests; no entity leakage in `CancelFamilyTaskResponse`; event payload carries IDs + title only, no free-text description |
| `CB-FAM-IMP-223` §8/§9/§10 | Interface signatures, API contract, error codes `FAM-033/079/080/081` (082/083 have no HTTP mapping — ADR-only) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner cancels an OPEN task (happy path) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-001` |
| TC-COND-002 | Owner cancels an IN_PROGRESS task (happy path) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-002` |
| TC-COND-003 | Cancel rejected when task status = DONE | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-003` |
| TC-COND-004 | Re-cancel rejected when task status already = CANCELLED (idempotency) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-004` |
| TC-COND-005 | Non-owner ACCEPTED MEMBER rejected | `CareTaskAuthorizationPolicy.canCancelTask()` | `FAM223-TC-005` |
| TC-COND-006 | Non-owner ACCEPTED VIEWER rejected | `CareTaskAuthorizationPolicy.canCancelTask()` | `FAM223-TC-006` |
| TC-COND-007 | Non-member (no CareGroupMember row) rejected | `CareTaskAuthorizationPolicy.canCancelTask()` | `FAM223-TC-007` |
| TC-COND-008 | Task id not found in target group | `CareTaskRepository.findByIdAndCareGroupId()` | `FAM223-TC-008` |
| TC-COND-009 | Task exists but under a different group id (cross-group) | `CareTaskRepository.findByIdAndCareGroupId()` | `FAM223-TC-009` |
| TC-COND-010 | `CareTaskCancelled` event payload correctness on successful cancel | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-010` |
| TC-COND-011 | No event published when cancel rejected (DONE / `FAM-080`) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-011` |
| TC-COND-012 | No second event published on rejected re-cancel (CANCELLED / `FAM-081`) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-012` |
| TC-COND-013 | Content fields + `completedAt` unchanged after successful cancel (invariant) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-013` |
| TC-COND-014 | `updatedAt` timestamp changes after a successful cancel | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-014` |
| TC-COND-015 | Authorization check precedes task load (gate ordering) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-015` |
| TC-COND-016 | Row is retained (not hard-deleted) after a successful cancel | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-016` |
| TC-COND-017 | Response DTO contains only `CancelFamilyTaskResponse` fields (no entity leakage) | `CareTaskMapper.toCancelResponse()` | `FAM223-TC-017` |
| TC-COND-018 | Unauthenticated request rejected | `CareGroupController.cancelTask()` (Spring Security filter chain) | `FAM223-TC-018` |
| TC-COND-019 | Cross-group task id manipulation (IDOR attempt) rejected without leaking existence | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-019` |
| TC-COND-020 | Full flow via real repositories (Testcontainers) | `CareTaskServiceImpl` + `CareTaskRepository` + `CareGroupMemberRepository` | `FAM223-TC-INT-001` |
| TC-COND-021 | Concurrent double-cancel (one succeeds, one rejected, exactly one event) | `CareTaskServiceImpl.cancelFamilyTask()` | `FAM223-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Role classes (OWNER / MEMBER / VIEWER / non-member) | Each partition has one representative and the identical expected error code `FAM-079` |
| State Transition Testing | `CareTaskStatus` gate: OPEN/IN_PROGRESS (cancellable) vs DONE (rejected `FAM-080`) vs CANCELLED (rejected `FAM-081`) | Full 4-state coverage of the starting-state partition, matching ADR-FAM-080/081's FSM-adjacent precondition |
| Error Guessing | Cross-group task id (IDOR); re-cancel double-submission; event emission on rejected paths | Security + contract-boundary defense per E1/E2 and ADR-FAM-081/082 |
| Contract/Schema Testing | `CancelFamilyTaskResponse` shape (no entity leakage); `CareTaskCancelled` payload shape | Prevents silent re-introduction of entity fields or a malformed event contract |
| Invariant/Non-Regression Testing | Content fields (`title`/`description`/`due_at`/`assigned_to`/`completed_at`) asserted unchanged after every successful cancel | Directly encodes ADR-FAM-083's disjoint-mutation-boundary decision with UC-222 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroup{id=GROUP_A, ownerUserId=OWNER_USER}` | Base group for all scenarios |
| `FX-002` | DB seed | `CareGroupMember{careGroupId=GROUP_A, userId=OWNER_USER, memberRole=OWNER, inviteStatus=ACCEPTED}` | Owner membership row |
| `FX-003` | DB seed | `CareGroupMember{careGroupId=GROUP_A, userId=MEMBER_USER, memberRole=MEMBER, inviteStatus=ACCEPTED}` | Non-owner ACCEPTED member (negative auth case) |
| `FX-004` | DB seed | `CareGroupMember{careGroupId=GROUP_A, userId=VIEWER_USER, memberRole=VIEWER, inviteStatus=ACCEPTED}` | Non-owner ACCEPTED viewer (negative auth case) |
| `FX-005` | DB seed | `CareTaskEntity{id=TASK_OPEN, careGroupId=GROUP_A, assignedTo=MEMBER_USER, assignedBy=OWNER_USER, status=OPEN, title="Mua thuốc định kỳ"}` | Happy-path base task |
| `FX-006` | DB seed | `CareTaskEntity{id=TASK_IN_PROGRESS, careGroupId=GROUP_A, status=IN_PROGRESS}` | Cancellable-but-started task |
| `FX-007` | DB seed | `CareTaskEntity{id=TASK_DONE, careGroupId=GROUP_A, status=DONE, completedAt=<past>}` | Terminal state — must reject cancel (`FAM-080`) |
| `FX-008` | DB seed | `CareTaskEntity{id=TASK_CANCELLED, careGroupId=GROUP_A, status=CANCELLED}` | Terminal state — must reject re-cancel (`FAM-081`) |
| `FX-009` | DB seed | `CareGroup{id=GROUP_B}` + `CareTaskEntity{id=TASK_OTHER_GROUP, careGroupId=GROUP_B, status=OPEN}` | Cross-group task (path `groupId=GROUP_A`, real group `GROUP_B`) |
| `FX-010` | JWT | `{sub: OWNER_USER, role: 'MOTHER'}` | Auth context — happy path |
| `FX-011` | JWT | `{sub: MEMBER_USER, role: 'FAMILY'}` | Auth context — non-owner negative case |
| `FX-012` | env | N/A — no HMAC/external service dependency for this feature | — |

---

## 4. Test Case Specification

> **TC ID format:** `FAM223-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state
> giữa các test cases. Đây là biện pháp chống AP-AI-002 (Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// CareTaskCancelTestFactory.java
class CareTaskCancelTestFactory {

    static final UUID GROUP_A = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000001");
    static final UUID GROUP_B = UUID.fromString("b0b0b0b0-0000-4b1b-9a3d-000000000099");
    static final UUID OWNER_USER   = UUID.fromString("11111111-0000-4b1b-9a3d-000000000001");
    static final UUID MEMBER_USER  = UUID.fromString("22222222-0000-4b1b-9a3d-000000000002");
    static final UUID VIEWER_USER  = UUID.fromString("33333333-0000-4b1b-9a3d-000000000003");
    static final UUID STRANGER_USER = UUID.fromString("55555555-0000-4b1b-9a3d-000000000005");
    static final UUID TASK_OPEN    = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000010");

    // Baseline OPEN task — synced with FX-005 (§3 TDS-05)
    static CareTaskEntity makeOpenTask() {
        return CareTaskEntity.builder()
                .id(TASK_OPEN)
                .careGroupId(GROUP_A)
                .assignedBy(OWNER_USER)
                .assignedTo(MEMBER_USER)
                .title("Mua thuốc định kỳ")
                .description("Original description")
                .dueAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .status(CareTaskStatus.OPEN)
                .completedAt(null)
                .build();
    }

    // Overload để override specific fields (e.g. status = DONE/CANCELLED/IN_PROGRESS)
    static CareTaskEntity makeTask(Consumer<CareTaskEntity.CareTaskEntityBuilder> overrides) {
        CareTaskEntity.CareTaskEntityBuilder b = CareTaskEntity.builder()
                .id(TASK_OPEN)
                .careGroupId(GROUP_A)
                .assignedBy(OWNER_USER)
                .assignedTo(MEMBER_USER)
                .title("Mua thuốc định kỳ")
                .description("Original description")
                .dueAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .status(CareTaskStatus.OPEN)
                .completedAt(null);
        overrides.accept(b);
        return b.build();
    }

    static CareGroupMember makeMember(UUID userId, GroupMemberRole role, InviteStatus status) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_A)
                .userId(userId)
                .memberRole(role)
                .inviteStatus(status)
                .build();
    }
}
```

---

### FAM223-TC-001 — Owner cancels an OPEN task (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.3.17.8` Normal Flow ("Cancels a task and notifies related members") + `TDS §8.1 CancelFamilyTaskResponse` + `ADR-FAM-080`

**Preconditions:**
- FX-001 (GROUP_A), FX-002 (OWNER_USER as OWNER/ACCEPTED), FX-005 (TASK_OPEN)

**Test Steps:**
1. Arrange: `CareTaskEntity task = CareTaskCancelTestFactory.makeOpenTask()`; mock repo `findByIdAndCareGroupId` returns it; mock policy `canCancelTask` returns true
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: response `status == "CANCELLED"`; repository `save()` invoked once with `status = CareTaskStatus.CANCELLED`; `auditService.log(CARE_TASK_CANCELLED, ...)` invoked once; `eventPublisher.publishEvent(...)` invoked once with a `CareTaskCancelled`

**Expected Result (PASS):**
- `CancelFamilyTaskResponse.status` = `"CANCELLED"`; exactly one `save()`, one audit log, one event publish

**Expected Result (FAIL):**
- Exception thrown, `status` not transitioned, or `save()`/audit/event not invoked

**Current Status:** 🔴 Not written
**Implementation Note:** Base happy path — verifies the full success chain (auth → load → transition → persist → audit → event).

---

### FAM223-TC-002 — Owner cancels an IN_PROGRESS task (still cancellable)

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS ADR-FAM-080` ("cancellable = OPEN ∪ IN_PROGRESS")

**Preconditions:** FX-001, FX-002, FX-006 (`TASK_IN_PROGRESS`)

**Test Steps:**
1. Arrange: `makeTask(b -> b.status(CareTaskStatus.IN_PROGRESS))`
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: success, `status` becomes `CANCELLED`; no `FAM-080` thrown

**Expected Result (PASS):** 200-equivalent success, `status = CANCELLED`
**Expected Result (FAIL):** `FAM-080` incorrectly thrown for `IN_PROGRESS`
**Current Status:** 🔴 Not written

---

### FAM223-TC-003 — Reject cancel when task status = DONE

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-FAM-080` — error `FAM-080` per `TDS §10`

**Preconditions:** FX-001, FX-002, FX-007 (`TASK_DONE`)

**Test Steps:**
1. Arrange: `makeTask(b -> b.status(CareTaskStatus.DONE).completedAt(Instant.now().minus(1, DAYS)))`
2. Act: attempt `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: `BusinessException` code `FAM-080`, HTTP 409; `save()` never invoked; `eventPublisher.publishEvent()` never invoked

**Expected Result (PASS):** Exception `FAM-080`/409; no persistence or event side effect
**Expected Result (FAIL):** A completed task is silently cancelled
**Current Status:** 🔴 Not written

---

### FAM223-TC-004 — Reject re-cancel when task status already = CANCELLED (idempotency-as-error)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-FAM-081` Decision ("re-cancelling an already-CANCELLED task throws `BusinessException(409, FAM-081)`") — a deliberate design choice for consistency with UC-222's ADR-FAM-073 precedent

**Preconditions:** FX-001, FX-002, FX-008 (`TASK_CANCELLED`)

**Test Steps:**
1. Arrange: `makeTask(b -> b.status(CareTaskStatus.CANCELLED))`
2. Act: attempt `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: `BusinessException` code `FAM-081` (distinct from `FAM-080`), HTTP 409; `save()` never invoked

**Expected Result (PASS):** Exception `FAM-081`/409 — distinct code from the DONE case
**Expected Result (FAIL):** Re-cancel silently succeeds (200 no-op), or reuses `FAM-080` instead of the distinct `FAM-081`
**Current Status:** 🔴 Not written
**Implementation Note:** This is the **idempotency-as-error** anti-regression test — the core behavioural distinction of ADR-FAM-081 from a naive idempotent-200 design.

---

### FAM223-TC-005 — Non-owner ACCEPTED MEMBER rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskAuthorizationPolicy.canCancelTask()` (via `CareTaskServiceImpl.cancelFamilyTask()`)
**Test File:** `CareTaskAuthorizationPolicyTest.java`, `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-FAM-079` (owner-only, reuses ADR-FAM-032/ADR-FAM-072 predicate); error `FAM-079` per `TDS §10`

**Preconditions:** FX-001, FX-003 (MEMBER_USER, `memberRole=MEMBER`, `inviteStatus=ACCEPTED`), FX-005

**Test Steps:**
1. Arrange: mock `memberRepository.findByCareGroupIdAndUserId(GROUP_A, MEMBER_USER)` returns FX-003
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, MEMBER_USER)`
3. Assert: `BusinessException` `FAM-079`/403; `taskRepository.findByIdAndCareGroupId` never reached (auth check precedes task load — see also `FAM223-TC-015`)

**Expected Result (PASS):** 403 `FAM-079`
**Expected Result (FAIL):** MEMBER role permitted to cancel — privilege escalation
**Current Status:** 🔴 Not written

---

### FAM223-TC-006 — Non-owner ACCEPTED VIEWER rejected

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskAuthorizationPolicy.canCancelTask()`
**Test File:** `CareTaskAuthorizationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-FAM-079`; error `FAM-079`

**Preconditions:** FX-001, FX-004 (VIEWER_USER)

**Test Steps:**
1. Arrange: mock membership lookup returns FX-004 (`memberRole=VIEWER`, `inviteStatus=ACCEPTED`)
2. Act: call `canCancelTask(GROUP_A, VIEWER_USER)`
3. Assert: returns `false`

**Expected Result (PASS):** `false` → service throws `FAM-079`/403
**Expected Result (FAIL):** `true` returned for VIEWER
**Current Status:** 🔴 Not written

---

### FAM223-TC-007 — Non-member (no CareGroupMember row) rejected

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskAuthorizationPolicy.canCancelTask()`
**Test File:** `CareTaskAuthorizationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-FAM-079`; error `FAM-079`

**Preconditions:** FX-001; caller `userId` (STRANGER_USER) has no `CareGroupMember` row in `GROUP_A`

**Test Steps:**
1. Arrange: mock `findByCareGroupIdAndUserId` returns `Optional.empty()`
2. Act: call `canCancelTask(GROUP_A, STRANGER_USER)`
3. Assert: returns `false`

**Expected Result (PASS):** `false` → 403 `FAM-079`
**Expected Result (FAIL):** `NullPointerException` or `true`
**Current Status:** 🔴 Not written

---

### FAM223-TC-008 — Task id not found in target group → rejected

**Severity:** `HIGH`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10` — `FAM-033` reused from UC-73's reservation for "care task not found"

**Preconditions:** FX-001, FX-002; `taskId` = random UUID with no matching row

**Test Steps:**
1. Arrange: mock `taskRepository.findByIdAndCareGroupId(randomId, GROUP_A)` returns `Optional.empty()`
2. Act: call `cancelFamilyTask(GROUP_A, randomId, OWNER_USER)`
3. Assert: `BusinessException` `FAM-033`/404

**Expected Result (PASS):** 404 `FAM-033`
**Expected Result (FAIL):** `NoSuchElementException`/500, or wrong error code
**Current Status:** 🔴 Not written

---

### FAM223-TC-009 — Task exists but under a different group id (cross-group) → rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §8.2` (`findByIdAndCareGroupId` — scoped lookup); error `FAM-033`

**Preconditions:** FX-001, FX-002, FX-009 (task actually belongs to `GROUP_B`), caller passes path `groupId = GROUP_A`

**Test Steps:**
1. Arrange: mock `findByIdAndCareGroupId(TASK_OTHER_GROUP, GROUP_A)` returns `Optional.empty()` (the compound key does not match since the task's real group is `GROUP_B`)
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OTHER_GROUP, OWNER_USER)`
3. Assert: `BusinessException` `FAM-033`/404 (not a 403 — the group-scoped lookup itself returns nothing, preventing enumeration of tasks across groups)

**Expected Result (PASS):** 404 `FAM-033` — no cross-group task info leaked
**Expected Result (FAIL):** Task from another group is loaded and cancelled — cross-tenant data leak
**Current Status:** 🔴 Not written

---

### FAM223-TC-010 — `CareTaskCancelled` event payload correctness on successful cancel

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`, `CareTaskCancelled` construction
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §7.3` Payload Schema + `ADR-FAM-082` Decision ("payload carries `careGroupId`, `assignedTo`, `assignedBy`, `cancelledBy`, `title`")

**Preconditions:** FX-001, FX-002, FX-005 (`TASK_OPEN`, `assignedTo=MEMBER_USER`, `assignedBy=OWNER_USER`)

**Test Steps:**
1. Arrange: `ArgumentCaptor<CareTaskCancelled>` on `eventPublisher.publishEvent(...)`
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: captured event's `payload.careTaskId == TASK_OPEN`; `payload.careGroupId == GROUP_A`; `payload.assignedTo == MEMBER_USER`; `payload.assignedBy == OWNER_USER`; `payload.cancelledBy == OWNER_USER`; `payload.title == "Mua thuốc định kỳ"`; `eventType == "CareTaskCancelled"`

**Expected Result (PASS):** All payload fields present and correctly populated per §7.3
**Expected Result (FAIL):** A field is missing, null when it shouldn't be, or the event carries the full `description` free-text (BR-PRIVACY violation)
**Current Status:** 🔴 Not written
**Implementation Note:** This test does **not** assert a recipient list — that remains `Open` (OPEN-2) by design; it verifies the payload is *sufficient* for a future consumer to decide.

---

### FAM223-TC-011 — No event published when cancel rejected (DONE / `FAM-080`)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §4.2` Data Integrity & Concurrency ("Notification exactly-once-ish... never on a rejected re-cancel") + `ADR-FAM-081`/`082`

**Preconditions:** FX-001, FX-002, FX-007 (`TASK_DONE`)

**Test Steps:**
1. Arrange: task with `status = DONE`
2. Act: attempt `cancelFamilyTask(...)`, expect `BusinessException`
3. Assert: `Mockito.verifyNoInteractions(eventPublisher)`

**Expected Result (PASS):** No event published on the rejected path
**Expected Result (FAIL):** An event is published even though the cancel was rejected
**Current Status:** 🔴 Not written

---

### FAM223-TC-012 — No second event published on rejected re-cancel (CANCELLED / `FAM-081`)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS ADR-FAM-081` Consequences ("protects the notification contract... a rejected re-cancel does NOT re-emit `CareTaskCancelled`, so members are never double-notified")

**Preconditions:** FX-001, FX-002, FX-008 (`TASK_CANCELLED`)

**Test Steps:**
1. Arrange: task with `status = CANCELLED`
2. Act: attempt `cancelFamilyTask(...)`, expect `BusinessException(409, FAM-081)`
3. Assert: `Mockito.verifyNoInteractions(eventPublisher)`

**Expected Result (PASS):** No event published; members are not double-notified
**Expected Result (FAIL):** A second `CareTaskCancelled` fires on a stale re-cancel — a duplicate-notification bug
**Current Status:** 🔴 Not written
**Implementation Note:** This is the **core anti-regression test** for the idempotency-as-error design — flagged CRITICAL because a failure here directly causes duplicate member notifications in production.

---

### FAM223-TC-013 — Content fields + `completedAt` unchanged after successful cancel (invariant)

**Severity:** `CRITICAL`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS ADR-FAM-083` invariant #1 (§6.3): "writes ONLY `status`... content fields and `completed_at` are unchanged"

**Preconditions:** FX-001, FX-002, FX-005 (`TASK_OPEN` with known `title`/`description`/`dueAt`/`assignedTo`)

**Test Steps:**
1. Arrange: `CareTaskEntity task = makeOpenTask()` with fixed content field values
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, OWNER_USER)`
3. Assert: captured saved entity's `title`/`description`/`dueAt`/`assignedTo`/`completedAt` are **identical** to the pre-cancel values; only `status` (→ `CANCELLED`) and `updatedAt` differ

**Expected Result (PASS):** Only `status`/`updatedAt` change; every content field bit-identical to input
**Expected Result (FAIL):** Any content field mutated as a side effect — violates the UC-222/UC-223 disjoint-mutation boundary (ADR-FAM-083)
**Current Status:** 🔴 Not written
**Implementation Note:** This is the **reciprocal** of UC-222's `FAM222-TC-020`/`023` (which assert `status`/`completedAt` unchanged on *update*) — together the two TCs prove the two UCs partition `care_tasks`'s mutable surface cleanly.

---

### FAM223-TC-014 — `updatedAt` timestamp changes after a successful cancel

**Severity:** `LOW`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §5.2` (`@UpdateTimestamp` on `updated_at`) + `TDS ADR-FAM-080` ("no `cancelled_at` column... only `updated_at` reflects the transition time")

**Preconditions:** FX-001, FX-002, FX-005

**Test Steps:**
1. Arrange: task with a known, older `updatedAt`
2. Act: call `cancelFamilyTask(...)`
3. Assert: response `updatedAt` is present and (in the integration test, §4 `FAM223-TC-INT-001`) strictly after the pre-cancel value; at the unit level, assert the field is populated in the mapped response (the JPA `@UpdateTimestamp` behaviour itself is exercised at the integration layer)

**Expected Result (PASS):** `updatedAt` is populated in the response and reflects the transition
**Expected Result (FAIL):** `updatedAt` is null or stale in the response
**Current Status:** 🔴 Not written

---

### FAM223-TC-015 — Authorization check precedes task load (gate ordering)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §6.1` Happy Path sequence diagram (policy check precedes `TaskRepo.findByIdAndCareGroupId`) — mirrors UC-222's `FAM222-TC-009` gate-ordering pattern

**Preconditions:** FX-001, FX-003 (MEMBER_USER, non-owner)

**Test Steps:**
1. Arrange: mock `canCancelTask` returns `false`
2. Act: call `cancelFamilyTask(GROUP_A, TASK_OPEN, MEMBER_USER)`
3. Assert: `BusinessException(403, FAM-079)` thrown; `Mockito.verifyNoInteractions(taskRepository)` — the task is **never fetched** for a caller who fails the owner check

**Expected Result (PASS):** 403 `FAM-079`; no task-repository interaction — non-owners cannot even infer whether a `taskId` exists
**Expected Result (FAIL):** Task is loaded before the authorization check — minor information-disclosure risk (existence oracle) and wasted work
**Current Status:** 🔴 Not written

---

### FAM223-TC-016 — Row is retained (not hard-deleted) after a successful cancel

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()`
**Test File:** `CareTaskServiceImplCancelTest.java` (unit-level via mock verification), reconfirmed at `FAM223-TC-INT-001` (integration, real DB)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS ADR-FAM-080` Decision ("a soft lifecycle transition; the row and all its content columns are retained — NO hard delete")

**Preconditions:** FX-001, FX-002, FX-005

**Test Steps:**
1. Arrange: mock `taskRepository` — verify `delete()`/`deleteById()` are never called anywhere in the service
2. Act: call `cancelFamilyTask(...)`
3. Assert: `Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any())`; `Mockito.verify(taskRepository, Mockito.never()).deleteById(Mockito.any())`; `save()` is invoked exactly once (the only persistence call)

**Expected Result (PASS):** Only `save()` is used; no delete method is ever invoked
**Expected Result (FAIL):** The service calls a delete method — a hard-delete regression against ADR-FAM-080
**Current Status:** 🔴 Not written

---

### FAM223-TC-017 — Response DTO contains only `CancelFamilyTaskResponse` fields (no entity leakage)

**Severity:** `LOW`
**Feature Under Test:** `CareTaskMapper.toCancelResponse()`
**Test File:** `CareTaskMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `TDS §8.1` `CancelFamilyTaskResponse` field list; CLAUDE.md "Never expose JPA entities in API responses; use DTOs and mappers"

**Preconditions:** None (pure mapper unit test)

**Test Steps:**
1. Arrange: `CareTaskEntity` with `status = CANCELLED`
2. Act: call `careTaskMapper.toCancelResponse(entity)`
3. Assert: returned object is `CancelFamilyTaskResponse` with exactly the fields `careTaskId`/`careGroupId`/`assignedTo`/`assignedBy`/`title`/`status`/`updatedAt` — reflection-based field-set check confirms no additional entity-only fields (e.g. `description`, `dueAt`) leak through

**Expected Result (PASS):** Response shape matches `TDS §8.1` exactly
**Expected Result (FAIL):** Response accidentally includes `description`/`dueAt` or exposes the raw entity
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

> Test cases kiểm tra attack vectors — điền thêm field OWASP và CWE.

---

### FAM223-TC-018 — Unauthenticated request rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `CareGroupController.cancelTask()` (Spring Security filter chain)
**Test File:** `CareGroupControllerCancelTaskTest.java` (`@WebMvcTest` + Spring Security test support)
**TDD Phase:** 🔴 RED
**Oracle Source:** `SRS §3.3.17.8` Exception E1 ("Access is denied when the actor is unauthenticated...") / `BR-RBAC`

**Preconditions:** No `Authorization` header supplied

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel` with no JWT
2. Observe response status
3. Verify no service method was invoked

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`; `ICareTaskService.cancelFamilyTask()` never called

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request reaches the service layer without authentication

**Current Status:** 🔴 Not written

---

### FAM223-TC-019 — Cross-group task id manipulation (IDOR attempt) rejected without leaking existence

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-PRIVACY`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()` (via `CareGroupControllerCancelTaskTest`, full path)
**Test File:** `CareTaskServiceImplIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Oracle Source:** `TDS §8.2` (`findByIdAndCareGroupId` scoped lookup) + `ADR-FAM-080`/`§10` (`FAM-033`, not a 403, to avoid an existence oracle)

**Preconditions:** FX-001, FX-002, FX-009 (task real group = `GROUP_B`)

**Test Steps (Attack Simulation):**
1. Owner of `GROUP_A` (who is not a member of `GROUP_B`) sends `POST /api/v1/care-groups/{GROUP_A}/tasks/{TASK_OTHER_GROUP}/cancel`
2. Observe response
3. Verify `GROUP_B`'s task's `status` is unaffected

**Expected Result (PASS = hệ thống an toàn):**
- `404 FAM-033` (not `403`, not `200`); `care_tasks` row for `TASK_OTHER_GROUP` still `status = OPEN`, unaffected

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Task in a foreign group is cancelled, or its existence is confirmed via a distinguishable error code — either is a cross-tenant data exposure/mutation

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### FAM223-TC-INT-001 — Full cancel flow via real repositories (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /cancel endpoint → CareTaskServiceImpl → CareTaskRepository/CareGroupMemberRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/family/CareTaskCancelIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `TDS §6.1` Happy Path sequence diagram + `TDS ADR-FAM-080`/`083` (status/content invariant) + `V1__init_schema.sql` (`care_tasks` column set)

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: FX-001, FX-002, FX-005 via JPA `save()` in test setup

**Test Steps:**
1. Seed group + owner + OPEN task per FX-001/002/005
2. `POST /api/v1/care-groups/{GROUP_A}/tasks/{TASK_OPEN}/cancel` with owner JWT
3. Assert HTTP 200 and response shape
4. Re-query `care_tasks` row directly via `CareTaskRepository.findById(TASK_OPEN)`

**Expected Result (PASS):**
- DB row `status = CANCELLED`; `title`/`description`/`due_at`/`assigned_to`/`completed_at` unchanged from pre-cancel; `updated_at` advanced; audit table (if wired) contains a `CARE_TASK_CANCELLED` entry

**Expected Result (FAIL):**
- DB row not updated, content fields mutated, or transaction partially applied

**DB Assertion:**
```java
CareTaskEntity record = careTaskRepository.findById(TASK_OPEN).orElseThrow();
assertThat(record.getStatus()).isEqualTo(CareTaskStatus.CANCELLED);
assertThat(record.getTitle()).isEqualTo("Mua thuốc định kỳ");
assertThat(record.getAssignedTo()).isEqualTo(MEMBER_USER);
assertThat(record.getCompletedAt()).isNull();
```

**Current Status:** 🔴 Not written

---

### FAM223-TC-INT-002 — Concurrent double-cancel (one succeeds, one rejected, exactly one event)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareTaskServiceImpl.cancelFamilyTask()` under concurrent invocation
**Test File:** `CareTaskCancelIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §4.2` Data Integrity & Concurrency ("the second committed cancel observes `CANCELLED` and is rejected `FAM-081`... Optimistic `@Version` locking is OUT of scope for v1") + `ADR-FAM-081`/`082`

**Preconditions:** FX-001, FX-002, FX-005; PostgreSQL Testcontainer running

**Test Steps:**
1. Seed OPEN task
2. Fire two near-simultaneous `cancelFamilyTask()` calls from two threads against the same `taskId`
3. Assert exactly one call completes successfully (`status = CANCELLED`, 200-equivalent) and the other throws `BusinessException(409, FAM-081)` (having observed the already-`CANCELLED` state after the first transaction committed)
4. Assert exactly **one** `CareTaskCancelled` event was published across both calls (no duplicate notification)

**Expected Result (PASS):** Exactly one success + one `FAM-081` rejection; exactly one event; final DB state is `CANCELLED`, not corrupted
**Expected Result (FAIL):** Both calls succeed (2 events published — duplicate notification), or a deadlock/exception occurs, or the final state is inconsistent
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM223-TC-001` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-002` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-003` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-004` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-005` | `CareTaskAuthorizationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-006` | `CareTaskAuthorizationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-007` | `CareTaskAuthorizationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-008` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-009` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-010` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-011` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-012` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-013` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-014` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-015` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-016` | `CareTaskServiceImplCancelTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-017` | `CareTaskMapperTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-018` | `CareGroupControllerCancelTaskTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-019` | `CareTaskServiceImplIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-INT-001` | `CareTaskCancelIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `FAM223-TC-INT-002` | `CareTaskCancelIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CareTaskServiceImpl implements ICareTaskService {

    @Override
    public CancelFamilyTaskResponse cancelFamilyTask(UUID groupId, UUID taskId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class CareTaskAuthorizationPolicy {
    public boolean canCancelTask(UUID groupId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM223-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM223-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM223-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| *(remaining TCs follow the same stub-must-fail pattern — full table populated at Red Gate execution time)* | | | | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(to be filled at implementation time)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(TBD)*

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec
> với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-223` đã được review và approve (currently `Draft`)
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] Không có migration mới cần thiết (§5.2 TDS decision) — xác nhận trước khi bắt đầu code
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị
- [ ] `CareTaskEntity`/`CareTaskStatus`/`CareTaskRepository` naming reconciled with sibling UC-73/UC-221/UC-222 (ADR-FAM-083) — if a sibling already landed with a different entity name, rename before writing tests
- [x] `AuditAction.CARE_TASK_CANCELLED` — **RESOLVED**: verified the real enum at
  `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java`
  contains 63 constants and **zero** `CARE_TASK_*` values today — adding `CARE_TASK_CANCELLED` is a
  routine, non-blocking one-line addition (full citation in companion TDS §11.1), not an open question
- [ ] Notification consumer wiring for `CareTaskCancelled` acknowledged as a separate, non-blocking dependency (OPEN-2)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `CareTaskServiceImpl.cancelFamilyTask()` và `CareTaskAuthorizationPolicy.canCancelTask()`
- [ ] Không có business logic trong `CareGroupController.cancelTask()` (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs/events (only `title` + IDs, never full `description`)
- [ ] Content-immutability invariant (ADR-FAM-083) verified by `FAM223-TC-013`/`016`/`INT-001` all green
- [ ] Idempotency-as-error invariant (ADR-FAM-081) verified by `FAM223-TC-004`/`012`/`INT-002` all green
- [ ] All error codes with an HTTP mapping (`FAM-033/079/080/081`) each covered by ≥1 passing test

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory pattern, §4)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/SRS) — done above per TC

### Suspension Criteria (Điều kiện tạm dừng)

- Sibling UC-73/UC-221/UC-222 lands first with a conflicting `CareTaskEntity`/`CareTaskStatus`/`CareTaskRepository` definition that requires renaming before these tests can compile
- Blocker dependency chưa sẵn sàng (e.g. `AuditService`/`AuditAction.CARE_TASK_CANCELLED` not yet defined)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No migration introduced by UC-223 — rollback is code-only (dev only)

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/CareTaskAuthorizationPolicy.java
git checkout -- src/main/java/com/carebridge/backend/family/dto/request/CancelFamilyTaskRequest.java
git checkout -- src/main/java/com/carebridge/backend/family/dto/response/CancelFamilyTaskResponse.java
git checkout -- src/main/java/com/carebridge/backend/family/event/CareTaskCancelled.java
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareGroupController.java
git checkout -- src/test/java/com/carebridge/backend/family/

# Note: do NOT blanket-revert CareTaskEntity/CareTaskRepository if sibling UC-73/221/222 code
# already depends on them — only revert this UC's own additions (cancelFamilyTask method,
# canCancelTask method, cancelTask endpoint, CancelFamilyTask* DTOs, CareTaskCancelled event).

# Gap vẫn OPEN → giữ nguyên entry trong sprint tracking
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes an architecture decision without an ADR (e.g., treating re-cancel as a no-op 200 instead of `FAM-081`, or asserting a hard `DELETE`) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller có business logic (owner check/status gate/idempotency) instead of Service/Policy | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a service/type not in codebase (e.g. `NEEDS_SUPPORT` enum value, a `cancelled_at`/`cancellation_reason` column, or a `CareTask` name diverging from `CareTaskEntity` without reconciliation) | ☐ | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved for Red Gate execution
  - AP-AI-001 check: every TC above cites an ADR/BR/SRS oracle in its "Oracle Source" field — none generic.
  - AP-AI-003 check: `FAM223-TC-004`/`012` explicitly assert re-cancel is an **error** (`FAM-081`), not a no-op, and `FAM223-TC-016` explicitly guards against a hard-delete regression; no TC assumes an undocumented decision.
  - AP-AI-004 check: authorization (`canCancelTask`) and the status/idempotency gate are tested at the Service/Policy layer (`CareTaskServiceImplCancelTest`, `CareTaskAuthorizationPolicyTest`), never asserted against controller internals.
  - AP-AI-005 check: all referenced types (`CareTaskEntity`, `CareTaskRepository`, `ICareTaskService`, `CareTaskAuthorizationPolicy`, `CareTaskCancelled`, `CareTaskStatus{OPEN,IN_PROGRESS,DONE,CANCELLED}`) trace to TDS §5/§7/§8; `NEEDS_SUPPORT` (UC-85's variant) and any `cancelled_at`/`cancellation_reason` column are never referenced.

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(none detected at spec-authoring time — re-run this table after Red Gate execution once stub code exists)* | | | | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: `Draft`. Companion to `04_Implement/UC223_CancelFamilyTask/UC223_CancelFamilyTask_TDS.md` (`CB-FAM-IMP-223`).*
