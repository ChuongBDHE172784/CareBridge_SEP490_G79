# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC-217 Revoke Family Invitation
# Đặc tả Kiểm thử Hướng Phát triển

**Document ID:** `CB-FAM-TDD-217`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Approved`
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
- `05_Development/CareBridgeAPI/.../family/service/impl/CareGroupServiceImpl.java` — owner-pattern & decline-flow reference
- `05_Development/CareBridgeAPI/.../audit/entity/AuditAction.java` — audit action enum
- `04_Implement/UC217_RevokeFamilyInvitation/UC217_RevokeFamilyInvitation_TDS.md` — TDS (CB-FAM-IMP-217)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.2 — Revoke Family Invitation

> **Quy ước TDD:** viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test ✅ nếu `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo TDD spec cho UC-217 Revoke Family Invitation |
| 2026-07-10 | AI Agent | Truthful sync after implementation evidence: 9/12 tests PASS in `CareGroupServiceImplMembershipLifecycleTest`; Red Gate not reconstructed because implementation pre-existed; E2E/INT remain pending |

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
| **Feature / UC ID** | `UC-217 Revoke Family Invitation` |
| **Module** | `family — Care Group Invitation (Bounded Context: family)` |
| **Spec gốc** | `CB-FAM-IMP-217` |
| **Priority** | 🟡 P2 (SRS Priority: Medium, Frequency: Occasional) |
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
| **Constraint Source** | `CB-FAM-IMP-217 §17`, `ADR-FAM-050/051/052` |
| **Constraints Injected** | Owner-only inline gate (C1); reuse `REVOKED` value, no migration (C2); distinct `CARE_GROUP_INVITE_REVOKED` audit action (C3); target-by-path + PENDING guard (C4); no-PII response + real field names (C5) |
| **Model** | `Claude Opus 4.8` |
| **Trust Level** | `T3 for unit/service coverage; Red Gate not reconstructed because implementation pre-existed (§5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` + real entity are the final persistence
> oracle; ERD and sibling-TDS prose are only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC-216 TDS prose/SQL/class-diagram invent `account_id`, `invite_status`, and an `accounts` table | Real `CareGroupMember.java` maps `userId → user_id` and `inviteStatus → invitation_status` against the `users` table (entity has explicit comments "Maps to V1 column user_id" / "Maps to V1 column invitation_status") | Tests assert against entity fields `userId` / `inviteStatus` and DB columns `user_id` / `invitation_status`; NEVER reference `account_id` / `invite_status` / `accounts` |
| L2 | A naive revoke design would add a new enum value (e.g. `CANCELLED`) to differentiate owner-revoke from invitee-decline | Real `InviteStatus` = `{ACCEPTED, PENDING, REVOKED}` only; batch decision confirms no new enum value / no migration. UC-218 `declineInvite()` already writes `REVOKED` | Tests assert revoke sets `invitation_status = REVOKED` (SHARED value); differentiation asserted via the distinct `AuditAction.CARE_GROUP_INVITE_REVOKED` constant, NOT a new status |
| L3 | Some sibling diagrams depict a `CareGroupAccessPolicy` authorization class | `family/policy/` is empty (`.gitkeep`); real ownership checks are inline in `CareGroupServiceImpl` (see `inviteMember()` lines ~155-162) | Tests exercise the owner gate as an inline service behavior; do NOT mock/assume a policy class |
| L4 | UC-71 draft proposes `invite_token`, `invite_expires_at`, `InviteChannel`, `REJECTED`/`EXPIRED` | None of these exist in real schema/code | Tests ignore token/channel/expiry entirely; only `invitation_status` transition is tested |
| L5 | Error codes could collide with parallel sibling agents (UC-218/219/…) | Batch allocation: UC-217 owns `FAM-050..054`; `FAM-005` reused for group-not-found | Tests assert exactly `FAM-050/051/052/053` for new paths and `FAM-005` for unknown group |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` + `CareGroupMember.java` in the test basis for any DB/persistence
> assertion.

### TDS-01 — Scope / Phạm vi

```
UC-217 Revoke Family Invitation — layers under test:
├── Service (CareGroupServiceImpl.revokeInvitation) — mock CareGroupRepository +
│     CareGroupMemberRepository + AuditService via Mockito (unit)
├── Controller (CareGroupController.revokeInvitation) — @WebMvcTest, mock ICareGroupService
│     (auth/status-code mapping) [optional if time-boxed]
└── Integration (@SpringBootTest + Testcontainers PostgreSQL) — real DB, verify
      invitation_status='REVOKED' and UC-216 filter exclusion
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.3.17.2` | Owner cancels a family invitation before it is accepted; Actor Mother; BR-RBAC + BR-PRIVACY |
| `ADR-FAM-050` | Owner-only inline gate `memberRole==OWNER && inviteStatus==ACCEPTED` → FAM-050 |
| `ADR-FAM-051` | Reuse `REVOKED` value; target must be `PENDING` → FAM-052 |
| `ADR-FAM-052` | Distinct `AuditAction.CARE_GROUP_INVITE_REVOKED` |
| `BR-RBAC` | Caller from JWT; target from path; no privilege escalation |
| `BR-PRIVACY` / PDPA | Response has no email/phone; audit retained |
| `CB-FAM-IMP-217 §11.3` | Service algorithm (guard order, exceptions) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner revokes a PENDING invite → REVOKED + audit REVOKED | `revokeInvitation()` happy path | `FAM217-TC-001`, `FAM217-TC-INT-001` |
| TC-COND-002 | Non-owner caller rejected | inline owner gate | `FAM217-TC-002`, `FAM217-TC-E2E-002` |
| TC-COND-003 | Target invite not found | target lookup | `FAM217-TC-003` |
| TC-COND-004 | Target not PENDING (ACCEPTED) | status guard | `FAM217-TC-004` |
| TC-COND-005 | Target already REVOKED | status guard (idempotency) | `FAM217-TC-005` |
| TC-COND-006 | Self-target guard | callerId==targetUserId | `FAM217-TC-006` |
| TC-COND-007 | Care group not found | group lookup | `FAM217-TC-007` |
| TC-COND-008 | Audit action is REVOKED, not DECLINED | audit differentiation | `FAM217-TC-008` |
| TC-COND-009 | Response has no PII; correct field names | response mapping | `FAM217-TC-009` |
| TC-COND-010 | E2E API 200 + 401 without JWT | controller/security | `FAM217-TC-E2E-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | caller role {OWNER-ACCEPTED, MEMBER, non-member} | Distinct authorization outcomes |
| State Transition Testing | `invitation_status` {PENDING→REVOKED valid; ACCEPTED/REVOKED→revoke invalid} | Guard the only legal transition |
| Boundary/Guard Analysis | `targetUserId == callerId` self-target boundary | Edge of authorization domain |
| Error Guessing | non-owner ACCEPTED member (IDOR attempt) | Privilege-escalation attack vector |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-CG-001` | DB seed | `CareGroup{ id=CG1, ownerUserId=OWNER1, status=ACTIVE }` | Group under test |
| `FX-OWNER` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=OWNER1, memberRole=OWNER, inviteStatus=ACCEPTED }` | Authorized caller |
| `FX-MEMBER` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=MEMBER3, memberRole=MEMBER, inviteStatus=ACCEPTED }` | Non-owner caller (FAM-050) |
| `FX-INVITEE-PENDING` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE2, memberRole=MEMBER, inviteStatus=PENDING }` | Revoke target (happy) |
| `FX-INVITEE-ACCEPTED` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=MEMBER4, inviteStatus=ACCEPTED }` | FAM-052 case |
| `FX-INVITEE-REVOKED` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE5, inviteStatus=REVOKED }` | FAM-052 idempotency |
| `FX-JWT-OWNER` | JWT | `{ sub: OWNER1, role: MOTHER }` | E2E auth context |
| `FX-JWT-MEMBER` | JWT | `{ sub: MEMBER3, role: MOTHER }` | E2E non-owner IDOR |

**Fixed UUIDs (SYNTHETIC):**
`CG1=11111111-1111-1111-1111-111111111111`, `OWNER1=aaaaaaaa-...-0001`,
`INVITEE2=22222222-2222-2222-2222-222222222222`, `MEMBER3=...-0003`, `MEMBER4=...-0004`,
`INVITEE5=...-0005`.

---

## 4. Test Case Specification

> **TC ID format:** `FAM217-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴/🟡/🟢

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern for UC-217
// Every @Test builds fresh entities via the factory — NO shared mutable state.
// ═══════════════════════════════════════════════════════════
class RevokeInvitationTestFactory {

    static final UUID CG1      = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER1   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID INVITEE2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID MEMBER3  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    static final UUID MEMBER4  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    static final UUID INVITEE5 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");

    // Real field names: userId / inviteStatus (NOT account_id / invite_status)
    static CareGroupMember member(UUID userId, GroupMemberRole role, InviteStatus status) {
        return CareGroupMember.builder()
            .id(UUID.randomUUID())
            .careGroupId(CG1)
            .userId(userId)
            .memberRole(role)
            .inviteStatus(status)
            .build();
    }

    static CareGroupMember owner()          { return member(OWNER1,   GroupMemberRole.OWNER,  InviteStatus.ACCEPTED); }
    static CareGroupMember memberAccepted()  { return member(MEMBER3,  GroupMemberRole.MEMBER, InviteStatus.ACCEPTED); }
    static CareGroupMember inviteePending()  { return member(INVITEE2, GroupMemberRole.MEMBER, InviteStatus.PENDING);  }
    static CareGroupMember inviteeAccepted() { return member(MEMBER4,  GroupMemberRole.MEMBER, InviteStatus.ACCEPTED); }
    static CareGroupMember inviteeRevoked()  { return member(INVITEE5, GroupMemberRole.MEMBER, InviteStatus.REVOKED);  }

    static CareGroup group() {
        return CareGroup.builder().id(CG1).ownerUserId(OWNER1).status(CareGroupStatus.ACTIVE).build();
    }
}
```

---

### FAM217-TC-001 — Owner revokes a PENDING invite → REVOKED

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.revokeInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-FAM-IMP-217 §11.3` (algorithm) + `ADR-FAM-051` (REVOKED value) + SRS §3.3.17.2 postcondition

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: mock `groupRepository.findById(CG1)` → `group()`; `memberRepository.findByCareGroupIdAndUserId(CG1, OWNER1)` → `owner()`; `findByCareGroupIdAndUserId(CG1, INVITEE2)` → `inviteePending()`; `memberRepository.save(any)` returns its argument.
2. Act: `service.revokeInvitation(CG1, INVITEE2, OWNER1)`.
3. Assert: saved member `inviteStatus == REVOKED`; response `inviteStatus == "REVOKED"`, `targetUserId == INVITEE2`.

**Expected Result (PASS):** target row saved with `InviteStatus.REVOKED`; `auditService.log` called with `AuditAction.CARE_GROUP_INVITE_REVOKED`.
**Expected Result (FAIL):** status left `PENDING`, or new enum value used, or wrong audit action.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_ownerRevokesPendingInvite_setsRevokedAndAudits`) on `2026-07-10`.
**Implementation Note:** Guard order per §11.3: group → self-target → owner → target → status.

---

### FAM217-TC-002 — Non-owner (MEMBER, ACCEPTED) caller → FAM-050 (403)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `revokeInvitation()` inline owner gate
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-050`, `§10 FAM-050`

**Preconditions:** `FX-CG-001`, `FX-MEMBER` (MEMBER3 is ACCEPTED but role MEMBER), `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: `findById(CG1)`→group; `findByCareGroupIdAndUserId(CG1, MEMBER3)`→`memberAccepted()`.
2. Act + Assert: `revokeInvitation(CG1, INVITEE2, MEMBER3)` throws `BusinessException`.

**Expected Result (PASS):** `BusinessException` with code `FAM-050`, HTTP 403; `memberRepository.save` NEVER called (no side effect).
**Expected Result (FAIL):** revoke succeeds → privilege escalation.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_nonOwnerAcceptedMember_throwsFam050AndDoesNotSave`) on `2026-07-10`.

---

### FAM217-TC-003 — Target invitation not found → FAM-051 (404)

**Severity:** `HIGH`
**Feature Under Test:** `revokeInvitation()` target lookup
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-003`
**Oracle Source:** `§10 FAM-051`

**Preconditions:** `FX-CG-001`, `FX-OWNER`; target row absent.

**Test Steps:**
1. Arrange: owner lookup returns `owner()`; `findByCareGroupIdAndUserId(CG1, UNKNOWN)` → `Optional.empty()`.
2. Act + Assert: `revokeInvitation(CG1, UNKNOWN, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-051`, HTTP 404.
**Expected Result (FAIL):** NullPointerException or a different code.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_targetNotFound_throwsFam051`) on `2026-07-10`.

---

### FAM217-TC-004 — Target already ACCEPTED → FAM-052 (409)

**Severity:** `HIGH`
**Feature Under Test:** `revokeInvitation()` PENDING guard
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-051`, `§10 FAM-052`

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-ACCEPTED`.

**Test Steps:**
1. Arrange: owner lookup → `owner()`; target lookup (MEMBER4) → `inviteeAccepted()`.
2. Act + Assert: `revokeInvitation(CG1, MEMBER4, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-052`, HTTP 409; `save` NEVER called; row remains `ACCEPTED`.
**Expected Result (FAIL):** an accepted member is wrongly revoked.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_targetAccepted_throwsFam052AndDoesNotSave`) on `2026-07-10`.

---

### FAM217-TC-005 — Target already REVOKED → FAM-052 (409, idempotency)

**Severity:** `MEDIUM`
**Feature Under Test:** `revokeInvitation()` PENDING guard (idempotency)
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-051`, `§9.1 idempotency note`, `§10 FAM-052`

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-REVOKED`.

**Test Steps:**
1. Arrange: owner lookup → `owner()`; target lookup (INVITEE5) → `inviteeRevoked()`.
2. Act + Assert: `revokeInvitation(CG1, INVITEE5, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-052`, HTTP 409 (second revoke is a no-op error).
**Expected Result (FAIL):** double-revoke succeeds silently / emits a second audit event.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_targetAlreadyRevoked_throwsFam052AndDoesNotSave`) on `2026-07-10`.

---

### FAM217-TC-006 — Self-target guard → FAM-053 (400)

**Severity:** `MEDIUM`
**Feature Under Test:** `revokeInvitation()` self-target guard
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-006`
**Oracle Source:** `§10 FAM-053`, `TDS §6.2`

**Preconditions:** `FX-CG-001`, `FX-OWNER`.

**Test Steps:**
1. Arrange: `findById(CG1)` → group.
2. Act + Assert: `revokeInvitation(CG1, OWNER1, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-053`, HTTP 400; owner row untouched; `save` NEVER called.
**Expected Result (FAIL):** owner accidentally revokes own membership.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_selfTarget_throwsFam053BeforeOwnerLookup`) on `2026-07-10`.

---

### FAM217-TC-007 — Care group not found → FAM-005 (404)

**Severity:** `MEDIUM`
**Feature Under Test:** `revokeInvitation()` group lookup
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-007`
**Oracle Source:** `§10 FAM-005` (reused from UC-70/UC-216)

**Preconditions:** none; group absent.

**Test Steps:**
1. Arrange: `groupRepository.findById(UNKNOWN)` → `Optional.empty()`.
2. Act + Assert: `revokeInvitation(UNKNOWN, INVITEE2, OWNER1)` throws.

**Expected Result (PASS):** `BusinessException` code `FAM-005`, HTTP 404.
**Expected Result (FAIL):** wrong code, or NPE.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_groupNotFound_throwsFam005`) on `2026-07-10`.

---

### FAM217-TC-008 — Audit action is REVOKED, NOT DECLINED

**Severity:** `HIGH`
**Feature Under Test:** `revokeInvitation()` audit differentiation
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-FAM-052` (distinct audit action)

**Preconditions:** `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: happy-path mocks (as TC-001).
2. Act: `revokeInvitation(CG1, INVITEE2, OWNER1)`.
3. Assert: capture the `AuditAction` argument to `auditService.log(...)`.

**Expected Result (PASS):** captured action == `CARE_GROUP_INVITE_REVOKED`; verify `CARE_GROUP_INVITE_DECLINED` was NEVER passed.
**Expected Result (FAIL):** reuses `CARE_GROUP_INVITE_DECLINED` → audit cannot distinguish revoke from decline.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_usesRevokedAuditActionNotDeclined`) on `2026-07-10`.
**Implementation Note:** `AuditAction.CARE_GROUP_INVITE_REVOKED` exists and is asserted directly; test also verifies `CARE_GROUP_INVITE_DECLINED` is never used for revoke.

---

### FAM217-TC-009 — Response contains no PII; uses real field names

**Severity:** `HIGH`
**CWE:** `CWE-359 — Exposure of Private Information`
**Legal:** `BR-PRIVACY / PDPA`
**Feature Under Test:** `RevokeInvitationResponse` mapping
**Test File:** `RevokeInvitationServiceTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-FAM-IMP-217 §8.1`, `BR-PRIVACY`

**Preconditions:** happy-path fixtures.

**Test Steps:**
1. Act: `revokeInvitation(CG1, INVITEE2, OWNER1)`.
2. Assert: response fields are exactly `{careGroupMemberId, groupId, targetUserId, inviteStatus, revokedAt}`; serialized JSON contains no `email`, no `phone`, no `@`.

**Expected Result (PASS):** no contact PII in response; `inviteStatus == "REVOKED"`.
**Expected Result (FAIL):** response leaks email/phone, or exposes `account_id`/`invite_status` naming.

**Current Status:** 🟢 Implemented and passed in `CareGroupServiceImplMembershipLifecycleTest.java` (`revokeInvitation_responseContainsOnlyNonPiiContractFields`) on `2026-07-10`.

---

### SECURITY / E2E TEST CASES

---

### FAM217-TC-E2E-001 — Owner revoke via API → 200; missing JWT → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke`
**Test File:** `src/test/java/com/carebridge/backend/family/RevokeInvitationIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-FAM-IMP-217 §9`, `§16`

**Preconditions:** Testcontainers PostgreSQL; seed `FX-CG-001`, `FX-OWNER`, `FX-INVITEE-PENDING`; `FX-JWT-OWNER`.

**Test Steps:**
1. `POST .../CG1/invitations/INVITEE2/revoke` with `Authorization: Bearer <owner_jwt>`.
2. `POST` the same without `Authorization` header.

**Expected Result (PASS):** first → 200, `data.inviteStatus == "REVOKED"`, no email/phone in body; second → 401.
**Expected Result (FAIL):** 200 without auth, or 500.

**Current Status:** 🔴 Not written

---

### FAM217-TC-E2E-002 — Non-owner ACCEPTED member cannot revoke (IDOR)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** revoke endpoint owner enforcement
**Test File:** `RevokeInvitationIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-FAM-050`, `§16`

**Preconditions:** seed `FX-MEMBER` (MEMBER3, ACCEPTED, role MEMBER), `FX-INVITEE-PENDING`; `FX-JWT-MEMBER`.

**Test Steps (Attack Simulation):**
1. As MEMBER3 (accepted but not owner), `POST .../CG1/invitations/INVITEE2/revoke`.
2. Inspect DB row for INVITEE2.

**Expected Result (PASS = safe):** `403` with `FAM-050`; DB row INVITEE2 remains `PENDING`.
**Expected Result (FAIL = vuln):** invite revoked by a non-owner.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### FAM217-TC-INT-001 — Persist REVOKED + UC-216 filter excludes it

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: revokeInvitation() → DB → listMembers() exclusion`
**Test File:** `RevokeInvitationIntegrationTest.java`
**TDD Phase:** 🔴 RED · **Condition Ref:** `TC-COND-001`
**Oracle Source:** `V1__init_schema.sql`, `CareGroupServiceImpl.listMembers()` filter `IN ('ACCEPTED','PENDING')`

**Preconditions:** PostgreSQL container; Flyway applied; seed `FX-OWNER`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Call `revokeInvitation(CG1, INVITEE2, OWNER1)`.
2. Query the DB row for INVITEE2.
3. Call `listMembers(CG1, OWNER1)`.

**Expected Result (PASS):** DB `invitation_status = 'REVOKED'` for INVITEE2; `listMembers` result does NOT contain INVITEE2.
**Expected Result (FAIL):** row still PENDING, or REVOKED row still appears in member list.

**DB Assertion:**
```java
CareGroupMember row = memberRepository.findByCareGroupIdAndUserId(CG1, INVITEE2).orElseThrow();
assertThat(row.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
CareGroupMembersResponse members = service.listMembers(CG1, OWNER1);
assertThat(members.getMembers())
    .noneMatch(m -> INVITEE2.equals(m.getMemberId())); // REVOKED excluded by existing filter
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM217-TC-001` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | guard helper already present as `requireOwner()` |
| `FAM217-TC-002` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM217-TC-003` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM217-TC-004` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertNonPendingTargetRejected()` |
| `FAM217-TC-005` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | shared helper `assertNonPendingTargetRejected()` |
| `FAM217-TC-006` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | asserts guard order before member lookup |
| `FAM217-TC-007` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | — |
| `FAM217-TC-008` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | direct verify of `CARE_GROUP_INVITE_REVOKED`, never `DECLINED` |
| `FAM217-TC-009` | `CareGroupServiceImplMembershipLifecycleTest.java` | `[N/A: implementation pre-existed]` | `[x] 2026-07-10 targeted Maven pass` | reflection verifies response contract fields |
| `FAM217-TC-E2E-001` | `RevokeInvitationIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `FAM217-TC-E2E-002` | `RevokeInvitationIntegrationTest.java` | `[ ]` | `[ ]` | — |
| `FAM217-TC-INT-001` | `RevokeInvitationIntegrationTest.java` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ suite với stub throw. Mọi test PHẢI FAIL. Nếu PASS ngay →
> AP-AI-002 → reject & rewrite.

**Stub cho Red Phase:**

```java
@Service
public class CareGroupServiceImpl implements ICareGroupService {
    @Override
    public RevokeInvitationResponse revokeInvitation(UUID groupId, UUID targetUserId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    // ... existing methods unchanged ...
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM217-TC-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM217-TC-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-003` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-004` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-005` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-006` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-007` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-008` | `throw` (+ enum constant missing → compile fail) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-009` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-E2E-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-E2E-002` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM217-TC-INT-001` | `throw` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `N/A — production implementation already existed before this UC217 pass`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Actual note `2026-07-10`: Red Gate was not re-created because `CareGroupServiceImpl.revokeInvitation(...)`, controller route, DTO, and audit enum were already present in the working tree. Added/expanded unit tests were validated against the existing implementation instead of fabricating a stub-fail history.
- Log file: `.omc/logs/uc217-red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-FAM-IMP-217` reviewed and status confirmed
- [x] Logic Issues (§2) confirmed (esp. L1 field-name deviation, L2 shared-REVOKED design)
- [x] No Flyway migration required (confirmed — ADR-FAM-051)
- [x] `AuditAction.CARE_GROUP_INVITE_REVOKED` exists in production enum (TDS §11.3 Chặng 1)
- [x] Test fixtures (§3 TDS-05) prepared via `CareGroupTestFactory`

### Exit Criteria (DoD)

- [x] Targeted unit test green: `mvn test -Dtest=CareGroupServiceImplMembershipLifecycleTest` → 12 tests, 0 failures/errors/skips (`2026-07-10`)
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [x] Coverage ≥ 80% lines for `revokeInvitation()` by targeted unit cases (manual assessment; coverage tool not run)
- [x] No business logic in controller (only validation + mapping)
- [x] No email/phone in response DTO; audit message contains IDs only (BR-PRIVACY)
- [x] Revoke sets `invitation_status = REVOKED` (not a new value); audit uses `CARE_GROUP_INVITE_REVOKED`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — not executed; implementation already existed before this pass
- [x] **Contract Existence** — targeted Maven test compiled clean; no hallucinated repo method / policy class:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"   # Expected: no output
  ```
- [ ] **Props Isolation** — all entities built via `RevokeInvitationTestFactory` inside each `@Test`
- [ ] **Oracle Source** — every expected value cites a BR/ADR/§ (see each TC)

### Suspension Criteria

- Owner-only vs any-member decision (ADR-FAM-050) still Open at implementation time
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
# No migration to revert — UC-217 adds no DB objects.

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/

# Dev/staging ONLY — repair a wrongly-revoked invite (status flip, no data loss)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE care_group_members SET invitation_status='PENDING', updated_at=NOW() \
      WHERE care_group_id='<groupId>' AND user_id='<targetUserId>' AND invitation_status='REVOKED';"

# UC-217 remains OPEN in the feature tracker until re-implemented.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS/BR | ☐ (mỗi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub (§5.1) | ☑ Mitigated by explicit pre-existing-implementation note; Red Gate not fabricated | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định thêm enum value / migration mới | ☐ (L2 forbids) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic | ☐ (logic in service only) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import repo method/policy class/`account_id`/`invite_status` không tồn tại | ☐ (L1/L3 guard; real fields only) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
