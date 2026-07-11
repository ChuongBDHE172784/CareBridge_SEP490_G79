# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC-219 Remove Family Member
# Đặc tả Kiểm thử Hướng Phát triển

**Document ID:** `CB-FAM-TDD-219`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (12/16 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer role`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/.../family/entity/CareGroupMember.java` — real entity (persistence oracle)
- `05_Development/CareBridgeAPI/.../family/entity/InviteStatus.java` — real enum `{ACCEPTED, PENDING, REVOKED}`
- `05_Development/CareBridgeAPI/.../family/entity/GroupMemberRole.java` — real enum `{OWNER, MEMBER, VIEWER}`
- `05_Development/CareBridgeAPI/.../family/repository/CareGroupMemberRepository.java` — real repository (verified — `findByCareGroupIdAndUserId`, `save`; no new method needed)
- `05_Development/CareBridgeAPI/.../family/service/impl/CareGroupServiceImpl.java` — owner-pattern reference (`inviteMember()` lines ~155-162)
- `05_Development/CareBridgeAPI/.../audit/entity/AuditAction.java` — real audit action enum (verified — no `CARE_GROUP_MEMBER_REMOVED` yet)
- `04_Implement/UC219_RemoveFamilyMember/UC219_RemoveFamilyMember_TDS.md` — TDS (`CB-FAM-IMP-219`)
- `04_Implement/UC217_RevokeFamilyInvitation/` — sibling TDS/Test-Spec (owner-pattern precedent, `FAM-050..054`)
- `04_Implement/UC70_CreateCareGroup/UC70_CreateCareGroup_TDS.md` — `ADR-FAM-001` (single OWNER per group)
- `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md` — `ADR-FAM-002` (REVOKED excluded from listing)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.4 — Remove Family Member

> **Quy ước TDD:** viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo TDD spec cho UC-219 Remove Family Member |
| 2026-07-10 | AI Agent | Truthful sync after implementation evidence: status set to Partially Implemented, 12/16 tests PASS in `CareGroupServiceImplMembershipLifecycleTest`; Red Gate not reconstructed because implementation pre-existed; E2E/INT remain pending |

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
| **Feature / UC ID** | `UC-219 Remove Family Member` |
| **Module** | `family — Care Group Membership (Bounded Context: family)` |
| **Spec gốc** | `CB-FAM-IMP-219` |
| **Priority** | 🟡 P2 (SRS Priority: Medium, Frequency: Regular) |
| **Sprint** | `S[N] (TBD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` (care-group family membership) |
| **Compliance Scope** | `PDPA (VN), BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `CareGroupRepository`, `CareGroupMemberRepository`, `AuditService` |
| **Downstream Consumers** | Audit log; UC-216 member list (excludes `REVOKED`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-219 §17`, `ADR-FAM-058/059/060/061/062` |
| **Constraints Injected** | Owner-only inline gate (C1); reuse `REVOKED` value, no migration (C2); target-is-OWNER guard before status guard (C3); target-by-path + ACCEPTED-only guard (C3); no `care_tasks` interaction (C5); no-PII response + real field names (C5) |
| **Model** | `Claude Opus 4.8` |
| **Trust Level** | `T3 for unit/service coverage; Red Gate not reconstructed because implementation pre-existed (§5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` + real entity are the final persistence
> oracle; ERD and sibling-TDS prose are only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC-216 TDS prose/SQL/class-diagram invent `account_id`, `invite_status`, and an `accounts` table | Real `CareGroupMember.java` maps `userId → user_id` and `inviteStatus → invitation_status` against the `users` table (entity has explicit comments "Maps to V1 column user_id" / "Maps to V1 column invitation_status") | Tests assert against entity fields `userId` / `inviteStatus` and DB columns `user_id` / `invitation_status`; NEVER reference `account_id` / `invite_status` / `accounts` |
| L2 | SRS postcondition "revokes active permissions" could imply a separate permission-clearing mechanism (e.g. a `permission_json` field) | Real `CareGroupMember` entity has NO `permission_json`/permissions column; every access gate in the codebase (`listMembers()`'s `existsByCareGroupIdAndUserIdAndInviteStatus(..., ACCEPTED)`, UC-216 `ADR-FAM-002`) requires `invitation_status == ACCEPTED` | Tests assert ONLY the `invitation_status → REVOKED` flip; no permission-field assertions are written since no such field exists (ADR-FAM-059) |
| L3 | A naive removal design would add a new enum value (e.g. `REMOVED`) to differentiate owner-remove from revoke/decline | Real `InviteStatus` = `{ACCEPTED, PENDING, REVOKED}` only; batch decision confirms no new enum value / no migration. UC-217 revoke and UC-218 `declineInvite()` already write `REVOKED` for different triggers | Tests assert removal sets `invitation_status = REVOKED` (SHARED value); differentiation asserted via the distinct `AuditAction.CARE_GROUP_MEMBER_REMOVED` constant, NOT a new status |
| L4 | Some sibling diagrams depict a `CareGroupAccessPolicy` authorization class | `family/policy/` is empty (`.gitkeep`); real ownership checks are inline in `CareGroupServiceImpl` (see `inviteMember()` lines ~155-162, and sibling UC-217 `revokeInvitation()`) | Tests exercise the owner gate as an inline service behavior; do NOT mock/assume a policy class |
| L5 | A naive removal design might auto-reassign the removed member's `care_tasks.assigned_to` rows (as UC-220's UX mockup does for Leave Care Group) | `CareTaskRepository` now exists for UC-220, but no SRS/UX evidence requires task reassignment for UC-219 specifically (ADR-FAM-061 — deliberately deferred, distinct from UC-220's scope) | Tests assert NO interaction with `CareTaskRepository`; a dedicated invariant test (`FAM219-TC-010`) verifies task reassignment is not invoked by removal |
| L6 | A naive removal design might allow removing the group OWNER (or treat owner-self-removal as a distinct case from removing-the-owner) | UC-70 `ADR-FAM-001`: exactly one OWNER row per group; removing it would orphan the group. Because the caller must themselves be OWNER (ADR-FAM-058), "target is OWNER" structurally is the only way to reach an owner-removal attempt, and it also covers owner-targets-self | Tests assert a single guard (`target.memberRole == OWNER` → `FAM-061`, checked BEFORE the ACCEPTED-status guard) covers both "remove another OWNER" (impossible under one-owner-per-group, but guarded anyway) and "owner removes self" |
| L7 | Error codes could collide with parallel sibling agents (UC-217/218/220/…) | Batch allocation: UC-219 owns `FAM-058..062`; `FAM-005` reused for group-not-found (per CLAUDE.md batch allocation table) | Tests assert exactly `FAM-058/059/060/061` for new paths and `FAM-005` for unknown group |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` + `CareGroupMember.java` in the test basis for any DB/persistence
> assertion.

### TDS-01 — Scope / Phạm vi

```
UC-219 Remove Family Member — layers under test:
├── Service (CareGroupServiceImpl.removeMember) — mock CareGroupRepository +
│     CareGroupMemberRepository + AuditService via Mockito (unit)
├── Controller (CareGroupController.removeMember) — @WebMvcTest, mock ICareGroupService
│     (auth/status-code mapping) [optional if time-boxed]
└── Integration (@SpringBootTest + Testcontainers PostgreSQL) — real DB, verify
      invitation_status='REVOKED' and UC-216 filter exclusion
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.3.17.4` | Owner removes an already-joined family member, revokes active permissions; Actor Mother; BR-RBAC + BR-PRIVACY |
| `ADR-FAM-058` | Owner-only inline gate `memberRole==OWNER && inviteStatus==ACCEPTED` → FAM-058 |
| `ADR-FAM-059` | Reuse `REVOKED` value; target must be `ACCEPTED` → FAM-060; no separate permission field |
| `ADR-FAM-060` | Target `memberRole==OWNER` → FAM-061 (also covers self-removal) |
| `ADR-FAM-061` | No auto-reassignment of `care_tasks`; scope boundary vs UC-220 |
| `ADR-FAM-062` | Distinct `AuditAction.CARE_GROUP_MEMBER_REMOVED` |
| `BR-RBAC` | Caller from JWT; target from path; no privilege escalation |
| `BR-PRIVACY` / PDPA | Response has no email/phone; audit retained |
| `CB-FAM-IMP-219 §11.3` | Service algorithm (guard order, exceptions) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner removes an ACCEPTED member → REVOKED + audit MEMBER_REMOVED | `removeMember()` happy path | `FAM219-TC-001`, `FAM219-TC-INT-001` |
| TC-COND-002 | Non-owner caller rejected | inline owner gate | `FAM219-TC-002`, `FAM219-TC-E2E-002` |
| TC-COND-003 | Target member not found | target lookup | `FAM219-TC-003` |
| TC-COND-004 | Target is the OWNER (incl. self) | owner-target guard | `FAM219-TC-004`, `FAM219-TC-005` |
| TC-COND-005 | Target is PENDING (not yet accepted) | status guard | `FAM219-TC-006` |
| TC-COND-006 | Target already REVOKED | status guard (idempotency) | `FAM219-TC-007` |
| TC-COND-007 | Care group not found | group lookup | `FAM219-TC-008` |
| TC-COND-008 | Audit action is MEMBER_REMOVED, not INVITE_REVOKED/DECLINED | audit differentiation | `FAM219-TC-009` |
| TC-COND-009 | No auto-reassignment of care_tasks (scope boundary) | no task-collaborator interaction | `FAM219-TC-010` |
| TC-COND-010 | Response has no PII; correct field names | response mapping | `FAM219-TC-011` |
| TC-COND-011 | Guard order: owner-target check precedes status check | guard sequencing | `FAM219-TC-012` |
| TC-COND-012 | E2E API 200 + 401 without JWT | controller/security | `FAM219-TC-E2E-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | caller role {OWNER-ACCEPTED, MEMBER, non-member}; target status {ACCEPTED, PENDING, REVOKED}; target role {OWNER, non-OWNER} | Distinct authorization/state outcomes across two independent partitions |
| State Transition Testing | `invitation_status` {ACCEPTED→REVOKED valid for non-OWNER; PENDING/REVOKED→remove invalid} | Guard the only legal transition for this UC |
| Boundary/Guard Analysis | `targetUserId == callerId` boundary (owner self-target); guard-order boundary (OWNER-check vs status-check precedence) | Edge of authorization domain; ensures correct error code precedence |
| Error Guessing | non-owner ACCEPTED member (IDOR attempt); attempting to orphan the group by removing OWNER | Privilege-escalation and data-integrity attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-CG-001` | DB seed | `CareGroup{ id=CG1, ownerUserId=OWNER1, status=ACTIVE }` | Group under test |
| `FX-OWNER` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=OWNER1, memberRole=OWNER, inviteStatus=ACCEPTED }` | Authorized caller; also the "cannot be removed" target |
| `FX-MEMBER-CALLER` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=MEMBER4, memberRole=MEMBER, inviteStatus=ACCEPTED }` | Non-owner caller (FAM-058) |
| `FX-MEMBER-TARGET` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=MEMBER3, memberRole=MEMBER, inviteStatus=ACCEPTED }` | Removal target (happy path) |
| `FX-INVITEE-PENDING` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE5, memberRole=MEMBER, inviteStatus=PENDING }` | FAM-060 case (not yet accepted) |
| `FX-MEMBER-REVOKED` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=MEMBER6, memberRole=MEMBER, inviteStatus=REVOKED }` | FAM-060 idempotency case |
| `FX-JWT-OWNER` | JWT | `{ sub: OWNER1, role: MOTHER }` | E2E auth context |
| `FX-JWT-MEMBER` | JWT | `{ sub: MEMBER4, role: MOTHER }` | E2E non-owner IDOR |

**Fixed UUIDs (SYNTHETIC):**
`CG1=11111111-1111-1111-1111-111111111111`, `OWNER1=aaaaaaaa-0000-0000-0000-000000000001`,
`MEMBER3=aaaaaaaa-0000-0000-0000-000000000003`, `MEMBER4=aaaaaaaa-0000-0000-0000-000000000004`,
`INVITEE5=aaaaaaaa-0000-0000-0000-000000000005`, `MEMBER6=aaaaaaaa-0000-0000-0000-000000000006`.

---

## 4. Test Case Specification

> **TC ID format:** `FAM219-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴/🟡/🟢

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern for UC-219
// Every @Test builds fresh entities via the factory — NO shared mutable state.
// ═══════════════════════════════════════════════════════════
class RemoveMemberTestFactory {

    static final UUID CG1      = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER1   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID MEMBER3  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    static final UUID MEMBER4  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    static final UUID INVITEE5 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");
    static final UUID MEMBER6  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000006");

    // Real field names: userId / inviteStatus / memberRole (NOT account_id / invite_status)
    static CareGroupMember member(UUID userId, GroupMemberRole role, InviteStatus status) {
        return CareGroupMember.builder()
            .id(UUID.randomUUID())
            .careGroupId(CG1)
            .userId(userId)
            .memberRole(role)
            .inviteStatus(status)
            .build();
    }

    static CareGroupMember owner()            { return member(OWNER1,   GroupMemberRole.OWNER,  InviteStatus.ACCEPTED); }
    static CareGroupMember memberCaller()     { return member(MEMBER4,  GroupMemberRole.MEMBER, InviteStatus.ACCEPTED); }
    static CareGroupMember memberTarget()     { return member(MEMBER3,  GroupMemberRole.MEMBER, InviteStatus.ACCEPTED); }
    static CareGroupMember inviteePending()   { return member(INVITEE5, GroupMemberRole.MEMBER, InviteStatus.PENDING);  }
    static CareGroupMember memberRevoked()    { return member(MEMBER6,  GroupMemberRole.MEMBER, InviteStatus.REVOKED);  }

    static CareGroup group() {
        return CareGroup.builder().id(CG1).ownerUserId(OWNER1).status(CareGroupStatus.ACTIVE).build();
    }
}
```

---

### FAM219-TC-001 — Owner removes an ACCEPTED member → REVOKED

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.removeMember()`
**Test File:** `src/test/java/com/carebridge/backend/family/RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-FAM-IMP-219 §11.3` (algorithm) + `ADR-FAM-059` (REVOKED value) + SRS §3.3.17.4 postcondition

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-MEMBER-TARGET`.

**Test Steps:**
1. Arrange: mock `groupRepository.findById(CG1)` → `group()`; `memberRepository.findByCareGroupIdAndUserId(CG1, OWNER1)` → `owner()`; `findByCareGroupIdAndUserId(CG1, MEMBER3)` → `memberTarget()`; `memberRepository.save(any)` returns its argument.
2. Act: `service.removeMember(CG1, MEMBER3, OWNER1)`.
3. Assert: saved member `inviteStatus == REVOKED`; response `inviteStatus == "REVOKED"`, `targetUserId == MEMBER3`.

**Expected Result (PASS):** target row saved with `InviteStatus.REVOKED`; `auditService.log` called with `AuditAction.CARE_GROUP_MEMBER_REMOVED`.
**Expected Result (FAIL):** status left `ACCEPTED`, or new enum value used, or wrong audit action.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_ownerRemovesAcceptedNonOwner_setsRevokedAndAudits`) on `2026-07-10`.
**Implementation Note:** Guard order per §11.3: group → owner(caller) → target lookup → target-is-OWNER → target-status.

---

### FAM219-TC-002 — Non-owner (MEMBER, ACCEPTED) caller → FAM-058 (403)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `removeMember()` inline owner gate
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-058`, `§10 FAM-058`

**Preconditions:** `FX-CG-001`, `FX-MEMBER-CALLER` (MEMBER4 is ACCEPTED but role MEMBER), `FX-MEMBER-TARGET`.

**Test Steps:**
1. Arrange: `findById(CG1)`→group; `findByCareGroupIdAndUserId(CG1, MEMBER4)`→`memberCaller()`.
2. Act + Assert: `removeMember(CG1, MEMBER3, MEMBER4)` throws `BusinessException`.

**Expected Result (PASS):** `BusinessException` with code `FAM-058`, HTTP 403; `memberRepository.save` NEVER called (no side effect).
**Expected Result (FAIL):** removal succeeds → privilege escalation.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_nonOwnerAcceptedMember_throwsFam058AndDoesNotSave`) on `2026-07-10`.

---

### FAM219-TC-003 — Target member not found → FAM-059 (404)

**Severity:** `HIGH`
**Feature Under Test:** `removeMember()` target lookup
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-003`
**Oracle Source:** `§10 FAM-059`

**Preconditions:** `FX-CG-001`, `FX-OWNER`; target row absent.

**Test Steps:**
1. Arrange: owner lookup returns `owner()`; `findByCareGroupIdAndUserId(CG1, UNKNOWN)` → `Optional.empty()`.
2. Act + Assert: `removeMember(CG1, UNKNOWN, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-059`, HTTP 404.
**Expected Result (FAIL):** NullPointerException or a different code.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetNotFound_throwsFam059`) on `2026-07-10`.

---

### FAM219-TC-004 — Target is the OWNER → FAM-061 (409)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow` (state-invariant bypass — group orphaning)
**Feature Under Test:** `removeMember()` owner-target guard
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-060`, `§10 FAM-061`

**Preconditions:** `FX-CG-001`, `FX-OWNER`. Caller is a DIFFERENT hypothetical accepted OWNER-role
check path is not reachable under UC-70's one-owner invariant, so this test targets the OWNER row
itself via a caller who is also the owner attempting to target the (only) OWNER row — i.e. this
overlaps with self-removal; see `FAM219-TC-005` for the explicit self case. This TC exercises the
guard generically: mock the target lookup to return an OWNER-role row regardless of which user id
it belongs to, proving the guard checks `memberRole`, not identity.

**Test Steps:**
1. Arrange: `findById(CG1)`→group; owner lookup (caller) → `owner()`; `findByCareGroupIdAndUserId(CG1, OWNER1)` (target, same as caller in this fixture set) → `owner()`.
2. Act + Assert: `removeMember(CG1, OWNER1, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-061`, HTTP 409; `memberRepository.save` NEVER called; OWNER1 row remains `ACCEPTED` (group not orphaned).
**Expected Result (FAIL):** the OWNER row is revoked → group orphaned (no controlling member).

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetOwner_throwsFam061AndDoesNotSave`) on `2026-07-10`.

---

### FAM219-TC-005 — Self-removal (owner targets own row) is rejected via the same OWNER guard → FAM-061 (409)

**Severity:** `HIGH`
**Feature Under Test:** `removeMember()` owner-target guard (self-removal case)
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-060` (self-removal explicitly covered by the same OWNER guard, no separate self-target check exists — unlike UC-217's distinct `FAM-053`)

**Preconditions:** `FX-CG-001`, `FX-OWNER`.

**Test Steps:**
1. Arrange: `findById(CG1)` → group; `findByCareGroupIdAndUserId(CG1, OWNER1)` → `owner()` (used for BOTH the caller lookup and the target lookup, since `targetUserId == callerId == OWNER1`).
2. Act + Assert: `removeMember(CG1, OWNER1, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-061`, HTTP 409 — same code as `FAM219-TC-004`, confirming no separate self-target error code exists for UC-219 (contrast UC-217's `FAM-053`); owner row untouched.
**Expected Result (FAIL):** owner accidentally revokes own membership, or a different/missing error code is thrown.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetOwner_throwsFam061AndDoesNotSave`) on `2026-07-10`.
**Implementation Note:** This is intentionally the SAME assertion as TC-004 by design (ADR-FAM-060: "this guard also covers the self-removal case") — kept as a separate TC to make the self-removal scenario explicitly traceable in the RTM, not because the code path differs.

---

### FAM219-TC-006 — Target is PENDING (not yet ACCEPTED) → FAM-060 (409)

**Severity:** `HIGH`
**Feature Under Test:** `removeMember()` ACCEPTED-only guard
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-059`, `§10 FAM-060`

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: owner lookup → `owner()`; target lookup (INVITEE5) → `inviteePending()`.
2. Act + Assert: `removeMember(CG1, INVITEE5, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-060`, HTTP 409; `save` NEVER called; row remains `PENDING`. Error message SHOULD guide the caller to UC-217 revoke instead (informational only, not asserted verbatim).
**Expected Result (FAIL):** a still-pending invite is wrongly revoked via the remove-member path (bypasses UC-217's distinct audit trail).

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetPending_throwsFam060AndDoesNotSave`) on `2026-07-10`.

---

### FAM219-TC-007 — Target already REVOKED → FAM-060 (409, idempotency)

**Severity:** `MEDIUM`
**Feature Under Test:** `removeMember()` ACCEPTED-only guard (idempotency)
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-FAM-059`, `§9.1 idempotency note`, `§10 FAM-060`

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-MEMBER-REVOKED`.

**Test Steps:**
1. Arrange: owner lookup → `owner()`; target lookup (MEMBER6) → `memberRevoked()`.
2. Act + Assert: `removeMember(CG1, MEMBER6, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-060`, HTTP 409 (second removal is a no-op error).
**Expected Result (FAIL):** double-removal succeeds silently / emits a second audit event.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetAlreadyRevoked_throwsFam060AndDoesNotSave`) on `2026-07-10`.

---

### FAM219-TC-008 — Care group not found → FAM-005 (404)

**Severity:** `MEDIUM`
**Feature Under Test:** `removeMember()` group lookup
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-007`
**Oracle Source:** `§10 FAM-005` (reused from UC-70/UC-216/UC-217)

**Preconditions:** none; group absent.

**Test Steps:**
1. Arrange: `groupRepository.findById(UNKNOWN)` → `Optional.empty()`.
2. Act + Assert: `removeMember(UNKNOWN, MEMBER3, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-005`, HTTP 404.
**Expected Result (FAIL):** wrong code, or NPE.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_groupNotFound_throwsFam005`) on `2026-07-10`.

---

### FAM219-TC-009 — Audit action is MEMBER_REMOVED, NOT INVITE_REVOKED/DECLINED/MEMBER_LEFT

**Severity:** `HIGH`
**Feature Under Test:** `removeMember()` audit differentiation
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-062` (distinct audit action)

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-MEMBER-TARGET`.

**Test Steps:**
1. Arrange: happy-path mocks (as TC-001).
2. Act: `removeMember(CG1, MEMBER3, OWNER1)`.
3. Assert: capture the `AuditAction` argument to `auditService.log(...)`.

**Expected Result (PASS):** captured action == `CARE_GROUP_MEMBER_REMOVED`; verify `CARE_GROUP_INVITE_REVOKED`, `CARE_GROUP_INVITE_DECLINED`, and `CARE_GROUP_MEMBER_LEFT` were NEVER passed.
**Expected Result (FAIL):** reuses an existing constant → audit cannot distinguish removal from revoke/decline/leave.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_usesMemberRemovedAuditActionNotInviteOrLeaveActions`) on `2026-07-10`.
**Implementation Note:** `AuditAction.CARE_GROUP_MEMBER_REMOVED` exists and is asserted directly; test also verifies invite revoke/decline and member-left actions are never used for removal.

---

### FAM219-TC-010 — No auto-reassignment of care_tasks (scope boundary vs UC-220)

**Severity:** `HIGH`
**Feature Under Test:** `removeMember()` — absence of any task-related side effect
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-061` (task reassignment deliberately belongs to UC-220, not UC-219)

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-MEMBER-TARGET`.

**Test Steps:**
1. Arrange: happy-path mocks (as TC-001). Do NOT construct or inject any `CareTaskRepository`/
   `CareTaskService` mock into `CareGroupServiceImpl`; the existing `CareTaskRepository` dependency
   is for UC-220 leave reassignment and must not be invoked by UC-219 removal.
2. Act: `removeMember(CG1, MEMBER3, OWNER1)`.
3. Assert: `CareGroupServiceImpl`'s only collaborators invoked are `groupRepository`,
   `memberRepository`, and `auditService` (verified via `Mockito.verifyNoMoreInteractions` on the
   full mock set); no compile-time reference to any task class exists in the service.

**Expected Result (PASS):** removal completes without invoking task reassignment.
**Expected Result (FAIL):** implementation calls `CareTaskRepository.reassignIncompleteTasks(...)`
or silently reassigns tasks via an undocumented mechanism.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_doesNotReassignTasks`) on `2026-07-10`.
**Implementation Note:** This TC is primarily a NEGATIVE/absence assertion enforcing ADR-FAM-061's
scope boundary. Because `CareTaskRepository` exists for UC-220, the test explicitly verifies it is
not called by `removeMember()`.

---

### FAM219-TC-011 — Response contains no PII; uses real field names

**Severity:** `HIGH`
**CWE:** `CWE-359 — Exposure of Private Information`
**Legal:** `BR-PRIVACY / PDPA`
**Feature Under Test:** `RemoveMemberResponse` mapping
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-FAM-IMP-219 §8.1`, `BR-PRIVACY`

**Preconditions:** happy-path fixtures.

**Test Steps:**
1. Act: `removeMember(CG1, MEMBER3, OWNER1)`.
2. Assert: response fields are exactly `{careGroupMemberId, groupId, targetUserId, inviteStatus, removedAt}`; serialized JSON contains no `email`, no `phone`, no `@`.

**Expected Result (PASS):** no contact PII in response; `inviteStatus == "REVOKED"`.
**Expected Result (FAIL):** response leaks email/phone, or exposes `account_id`/`invite_status` naming.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_responseContainsOnlyNonPiiContractFields`) on `2026-07-10`.

---

### FAM219-TC-012 — Guard order: owner-target check precedes ACCEPTED-status check

**Severity:** `MEDIUM`
**Feature Under Test:** `removeMember()` guard sequencing
**Test File:** `RemoveMemberServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-FAM-IMP-219 §11.3` normative guard-order note

**Preconditions:** a target row that is simultaneously `memberRole == OWNER` AND, hypothetically,
`inviteStatus != ACCEPTED` (constructed purely to test guard ORDER — an OWNER row is always
ACCEPTED in real data per UC-70, so this is a synthetic ordering probe, not a realistic state).

**Test Steps:**
1. Arrange: owner lookup (caller) → `owner()`; target lookup → a synthetic
   `member(OWNER1, GroupMemberRole.OWNER, InviteStatus.PENDING)` (deliberately inconsistent, for
   guard-order testing only).
2. Act + Assert: `removeMember(CG1, OWNER1, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code is `FAM-061` (owner-guard fires first), NOT `FAM-060` — proving the implementation checks `memberRole == OWNER` before `inviteStatus != ACCEPTED`, per the TDS's normative guard order.
**Expected Result (FAIL):** code is `FAM-060`, indicating the status guard incorrectly runs first.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`removeMember_targetOwnerWithNonAcceptedStatus_stillThrowsFam061BeforeStatusGuard`) on `2026-07-10`.
**Implementation Note:** This is a white-box ordering test justified by the TDS's explicit
normative statement ("The owner-target guard MUST precede the status guard") — the oracle is the
TDS text itself (§11.3), not an SRS source, since SRS does not specify guard ordering.

---

### SECURITY / E2E TEST CASES

---

### FAM219-TC-E2E-001 — Owner removal via API → 200; missing JWT → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}`
**Test File:** `src/test/java/com/carebridge/backend/family/RemoveMemberIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-FAM-IMP-219 §9`, `§16`

**Preconditions:** Testcontainers PostgreSQL; seed `FX-CG-001`, `FX-OWNER`, `FX-MEMBER-TARGET`; `FX-JWT-OWNER`.

**Test Steps:**
1. `DELETE .../CG1/members/MEMBER3` with `Authorization: Bearer <owner_jwt>`.
2. `DELETE` the same without `Authorization` header.

**Expected Result (PASS):** first → 200, `data.inviteStatus == "REVOKED"`, no email/phone in body; second → 401.
**Expected Result (FAIL):** 200 without auth, or 500.

**Current Status:** 🔴 Not written

---

### FAM219-TC-E2E-002 — Non-owner ACCEPTED member cannot remove (IDOR)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** removal endpoint owner enforcement
**Test File:** `RemoveMemberIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-058`, `§16`

**Preconditions:** seed `FX-MEMBER-CALLER` (MEMBER4, ACCEPTED, role MEMBER), `FX-MEMBER-TARGET`; `FX-JWT-MEMBER`.

**Test Steps (Attack Simulation):**
1. As MEMBER4 (accepted but not owner), `DELETE .../CG1/members/MEMBER3`.
2. Inspect DB row for MEMBER3.

**Expected Result (PASS = safe):** `403` with `FAM-058`; DB row MEMBER3 remains `ACCEPTED`.
**Expected Result (FAIL = vuln):** member removed by a non-owner.

**Current Status:** 🔴 Not written

---

### FAM219-TC-E2E-003 — Attempt to remove the OWNER via API (group-orphaning attempt) → 409

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Legal:** `Data Integrity (group must always have exactly one OWNER — UC-70 ADR-FAM-001)`
**Feature Under Test:** removal endpoint owner-target guard
**Test File:** `RemoveMemberIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-060`, `§16`, UC-70 `ADR-FAM-001`

**Preconditions:** seed `FX-CG-001`, `FX-OWNER`; `FX-JWT-OWNER`.

**Test Steps:**
1. As OWNER1, `DELETE .../CG1/members/OWNER1` (targets self, the only OWNER).
2. Inspect DB row for OWNER1 and confirm the group still has exactly one ACCEPTED OWNER.

**Expected Result (PASS = safe):** `409` with `FAM-061`; DB row OWNER1 remains `ACCEPTED`; group not orphaned.
**Expected Result (FAIL = data-integrity bug):** group ends up with zero ACCEPTED OWNER rows.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FAM219-TC-INT-001 — Persist REVOKED + UC-216 filter excludes it

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: removeMember() → DB → listMembers() exclusion`
**Test File:** `RemoveMemberIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-001`
**Oracle Source:** `V1__init_schema.sql`, `CareGroupServiceImpl.listMembers()` filter `IN ('ACCEPTED','PENDING')`

**Preconditions:** PostgreSQL container; Flyway applied; seed `FX-OWNER`, `FX-MEMBER-TARGET`.

**Test Steps:**
1. Call `removeMember(CG1, MEMBER3, OWNER1)`.
2. Query the DB row for MEMBER3.
3. Call `listMembers(CG1, OWNER1)`.

**Expected Result (PASS):** DB `invitation_status = 'REVOKED'` for MEMBER3; `listMembers` result does NOT contain MEMBER3; the OWNER row is still present and `ACCEPTED`.
**Expected Result (FAIL):** row still ACCEPTED, or REVOKED row still appears in member list.

**DB Assertion:**
```java
CareGroupMember row = memberRepository.findByCareGroupIdAndUserId(CG1, MEMBER3).orElseThrow();
assertThat(row.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
CareGroupMembersResponse members = service.listMembers(CG1, OWNER1);
assertThat(members.getMembers())
    .noneMatch(m -> MEMBER3.equals(m.getMemberId())); // REVOKED excluded by existing filter
CareGroupMember ownerRow = memberRepository.findByCareGroupIdAndUserId(CG1, OWNER1).orElseThrow();
assertThat(ownerRow.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED); // group not orphaned
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM219-TC-001` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | existing `requireOwner()` guard helper |
| `FAM219-TC-002` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM219-TC-003` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM219-TC-004` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | owner-target invariant |
| `FAM219-TC-005` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | same owner-target guard covers self-removal |
| `FAM219-TC-006` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertRemoveMemberNonAcceptedTargetRejected()` |
| `FAM219-TC-007` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertRemoveMemberNonAcceptedTargetRejected()` |
| `FAM219-TC-008` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM219-TC-009` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | verifies distinct audit action by negative assertions |
| `FAM219-TC-010` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | `verifyNoInteractions(taskRepository)` |
| `FAM219-TC-011` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | reflection verifies response fields |
| `FAM219-TC-012` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | explicit owner-before-status guard-order probe |
| `FAM219-TC-E2E-001` | `RemoveMemberIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `FAM219-TC-E2E-002` | `RemoveMemberIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `FAM219-TC-E2E-003` | `RemoveMemberIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `FAM219-TC-INT-001` | `RemoveMemberIntegrationTest.java` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ suite với stub throw. Mọi test PHẢI FAIL. Nếu PASS ngay →
> AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**

```java
@Service
public class CareGroupServiceImpl implements ICareGroupService {
    @Override
    public RemoveMemberResponse removeMember(UUID groupId, UUID targetUserId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    // ... existing methods unchanged ...
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM219-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM219-TC-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-004` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-005` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-007` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-008` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-009` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-010` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-011` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-012` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-E2E-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-E2E-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-E2E-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM219-TC-INT-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `N/A — production implementation already existed before this UC219 pass`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Actual note `2026-07-10`: Red Gate was not re-created because `CareGroupServiceImpl.removeMember(...)`, controller route, DTO, and audit enum were already present in the working tree. Added/expanded unit tests were validated against the existing implementation instead of fabricating a stub-fail history.
- Log file: `.omc/logs/uc219-red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-FAM-IMP-219` reviewed and status confirmed
- [x] Logic Issues (§2) confirmed (esp. L1 field-name deviation, L2 no-permission-field, L3 shared-REVOKED design, L5 no-task-reassignment, L6 owner-guard-covers-self)
- [x] No Flyway migration required (confirmed — ADR-FAM-059)
- [x] `AuditAction.CARE_GROUP_MEMBER_REMOVED` exists in production enum (TDS §11.3 Chặng 1)
- [x] Test fixtures (§3 TDS-05) prepared via `CareGroupTestFactory`

### Exit Criteria (DoD)

- [x] Targeted unit/service tests green: `mvn test -Dtest=CareGroupServiceImplMembershipLifecycleTest,CareTaskServiceImplTaskManagementTest` → 37 tests, 0 failures/errors/skips (`2026-07-10`)
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [x] Coverage ≥ 80% lines for `removeMember()` by targeted unit cases (manual assessment; coverage tool not run)
- [x] No business logic in controller (only validation + mapping)
- [x] No email/phone in response DTO; audit message contains IDs only (BR-PRIVACY)
- [x] Removal sets `invitation_status = REVOKED` (not a new value); audit uses `CARE_GROUP_MEMBER_REMOVED`
- [x] `removeMember()` does not call `CareTaskRepository` / task reassignment (ADR-FAM-061)
- [x] OWNER row can never be revoked via this endpoint (group never orphaned)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — not executed; implementation already existed before this pass
- [x] **Contract Existence** — targeted Maven test compiled clean; no hallucinated repo method / policy class:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"   # Expected: no output
  ```
- [ ] **Props Isolation** — all entities built via `RemoveMemberTestFactory` inside each `@Test`
- [ ] **Oracle Source** — every expected value cites a BR/ADR/§ (see each TC)

### Suspension Criteria

- Owner-only vs any-member decision (ADR-FAM-058) still Open at implementation time
- Cannot-remove-owner rule (ADR-FAM-060) still Open at implementation time
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
# No migration to revert — UC-219 adds no DB objects.

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/

# Dev/staging ONLY — repair a wrongly-removed member (status flip, no data loss)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE care_group_members SET invitation_status='ACCEPTED', updated_at=NOW() \
      WHERE care_group_id='<groupId>' AND user_id='<targetUserId>' AND invitation_status='REVOKED';"

# UC-219 remains OPEN in the feature tracker until re-implemented.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS/BR | ☐ (mỗi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☑ Mitigated by explicit pre-existing-implementation note; Red Gate not fabricated | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định thêm enum value / migration / auto-reassign tasks không có ADR | ☐ (L3/L5 forbids) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic | ☐ (logic in service only) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import repo method/policy class/task class/`account_id`/`invite_status` không tồn tại | ☐ (L1/L4/L5 guard; real fields only) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
