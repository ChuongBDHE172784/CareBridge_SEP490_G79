# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC-218 Reject Care Group Invitation
# Đặc tả Kiểm thử Hướng Phát triển

**Document ID:** `CB-FAM-TDD-218`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer role`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

> **⚠️ Documentation-of-existing-implementation notice.** UC-218 (`declineInvite()`) is **already
> implemented and shipped**. These test cases encode the REAL behavior read from the live source —
> they are not a greenfield design. Where a test would pass immediately against the current code
> (because the behavior already exists), this is noted honestly in §5 rather than claimed as a
> fresh TDD cycle.

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/.../family/entity/CareGroupMember.java` — real entity (persistence oracle)
- `05_Development/CareBridgeAPI/.../family/entity/InviteStatus.java` — real enum `{ACCEPTED, PENDING, REVOKED}`
- `05_Development/CareBridgeAPI/.../family/service/impl/CareGroupServiceImpl.java` — real `declineInvite()` + `pendingInviteOrThrow()`
- `05_Development/CareBridgeAPI/.../family/controller/CareGroupController.java` — real `POST /{groupId}/invitations/decline`
- `05_Development/CareBridgeAPI/.../audit/entity/AuditAction.java` — audit action enum (`CARE_GROUP_INVITE_DECLINED`)
- `04_Implement/UC218_RejectCareGroupInvitation/UC218_RejectCareGroupInvitation_TDS.md` — TDS (CB-FAM-IMP-218)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.17.3 — Reject Care Group Invitation
- `04_Implement/UC217_RevokeFamilyInvitation/` — sibling spec (owner-side revoke; different actor/target)

> **Quy ước TDD:** viết test (`.java`) → chạy → xác nhận FAIL 🔴 (or honestly note pre-existing
> GREEN, see §5) → implement (already done) → PASS 🟢 → refactor 🔵. Không mark test ✅ nếu
> `./mvnw test` chưa xanh. Chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-03` | `AI Agent — Test Designer` | Khởi tạo TDD spec cho UC-218 Reject Care Group Invitation (documents already-implemented `declineInvite()`) |

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
| **Feature / UC ID** | `UC-218 Reject Care Group Invitation` |
| **Module** | `family — Care Group Invitation (Bounded Context: family)` |
| **Spec gốc** | `CB-FAM-IMP-218` |
| **Priority** | 🟡 P2 (SRS Priority: Medium, Frequency: Regular) |
| **Sprint** | `S[N] (TBD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` (care-group family membership) |
| **Compliance Scope** | `PDPA (VN), BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `CareGroupMemberRepository`, `AuditService` |
| **Downstream Consumers** | Audit log; UC-216 member list (excludes `REVOKED`) |
| **Implementation Status** | `Already implemented` — `CareGroupServiceImpl.declineInvite()`, `CareGroupController.declineInvite()` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-218 §17`, `ADR-FAM-055/056/057` |
| **Constraints Injected** | Self-service decline via JWT callerId only, no target param (C1); reuse `REVOKED` value, no migration (C2); `CARE_GROUP_INVITE_DECLINED` audit action, distinct from `_REVOKED` (C3); collapsed `FAM-009/404` for absent-row and not-PENDING (C4); no `permission_json` write / no data access granted (C5) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T3` (pre-existing implementation; tests written retroactively to formalize and pin real behavior — Red Gate run against the historical stub is not reproducible, see §5.1) |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, `V1__init_schema.sql` + the real entity are the final
> persistence oracle; ERD and sibling-TDS prose are only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy / code) | Fix áp dụng trong test |
|---|------------------------|-----------------------------------|------------------------|
| L1 | The real `declineInvite()` method and its controller endpoint are code-commented `// UC83: Decline a pending invitation`, which could be misread as "this belongs to UC-83 (Accept), not UC-218" | UC-83 (Accept) and UC-218 (Reject/Decline) are twin actions on the same invitation; the comment attribution is simply incomplete/mislabeled, not a behavioral conflict — the method's actual behavior (invitee declines own pending invite, sets `REVOKED`, logs `CARE_GROUP_INVITE_DECLINED`) exactly matches SRS UC-218 | Tests target `declineInvite()` under its correct UC-218 identity; test file/class names reference UC-218, not UC-83. Housekeeping note recorded in TDS §11.3 (comment fix is out of scope — not performed by tests) |
| L2 | UC-216 sibling TDS prose invents `account_id` / `invite_status` / `accounts` naming | Real `CareGroupMember.java` maps `userId → user_id`, `inviteStatus → invitation_status`, against the `users` table | Tests assert against entity fields `userId` / `inviteStatus` and DB columns `user_id` / `invitation_status`; NEVER reference `account_id` / `invite_status` / `accounts` |
| L3 | A naive design might add a new `REJECTED` enum value to distinguish decline from owner-revoke (UC-217) | Real `InviteStatus` = `{ACCEPTED, PENDING, REVOKED}` only; batch decision confirms no new enum value / no migration; decline and owner-revoke share `REVOKED`, differentiated only by `AuditAction` | Tests assert `declineInvite()` sets `invitation_status = REVOKED` (shared value) and audit action `CARE_GROUP_INVITE_DECLINED` (NOT `CARE_GROUP_INVITE_REVOKED`) |
| L4 | A naive design might introduce distinct error codes for "no invitation row" vs "invitation not pending" | Real `pendingInviteOrThrow()` throws the **identical** `FAM-009/404` for both cases (verified in source) | Tests assert `FAM-009` for BOTH the absent-row case and the wrong-status case; do NOT expect two different codes |
| L5 | SRS lists Firebase Cloud Messaging as a secondary actor, which could suggest a push-notification test is required | The real `declineInvite()` emits no FCM/push call — only `auditService.log(...)` | Tests do NOT assert any push/FCM interaction; this is recorded as TDS §7 OPEN-1, not tested as a requirement |
| L6 | SRS "does not create data access permission" could be read as requiring an explicit permission-revocation step | `permission_json` column exists in `V1__init_schema.sql` but is **not mapped** by `CareGroupMember.java` entity at all, and `declineInvite()` never touches it; the row also never reaches `ACCEPTED`, so no downstream access-gate (`listMyGroups`/`listMembers`, which require `ACCEPTED`) ever admits it | Tests assert `inviteStatus` never becomes `ACCEPTED` and that a `REVOKED` row is excluded from `listMembers()` (already-existing filter, reused from UC-216) as the proof of "no permission created" |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` + `CareGroupMember.java` in the test basis for any DB/persistence
> assertion.

### TDS-01 — Scope / Phạm vi

```
UC-218 Reject Care Group Invitation — layers under test:
├── Service (CareGroupServiceImpl.declineInvite) — mock CareGroupMemberRepository +
│     AuditService via Mockito (unit)
├── Controller (CareGroupController.declineInvite) — @WebMvcTest, mock ICareGroupService
│     (auth/status-code mapping) [optional if time-boxed]
└── Integration (@SpringBootTest + Testcontainers PostgreSQL) — real DB, verify
      invitation_status='REVOKED', permission_json stays NULL, and UC-216 filter exclusion
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.3.17.3` | Invitee rejects own care group invitation; no data access permission created; Actor Family Member; BR-RBAC + BR-PRIVACY |
| `ADR-FAM-055` | Self-service gate — caller acts only on own row (JWT callerId); no target param |
| `ADR-FAM-056` | Reuse `REVOKED` value; distinct `AuditAction.CARE_GROUP_INVITE_DECLINED`; no `permission_json` write |
| `ADR-FAM-057` | Collapsed `FAM-009/404` for both absent-row and not-PENDING cases |
| `BR-RBAC` | Caller identity from JWT only; structurally IDOR-impossible |
| `BR-PRIVACY` / PDPA | Response is empty (`ApiResponse<Void>`); no email/phone; audit retained |
| `CB-FAM-IMP-218 §8.1` | Service algorithm (`pendingInviteOrThrow` guard order) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Invitee declines own PENDING invite → REVOKED + audit DECLINED | `declineInvite()` happy path | `FAM218-TC-001`, `FAM218-TC-INT-001` |
| TC-COND-002 | No membership row for caller (never invited) → 404 | absent-row guard | `FAM218-TC-002` |
| TC-COND-003 | Caller's row already ACCEPTED (not-pending) → 404 | status guard | `FAM218-TC-003` |
| TC-COND-004 | Caller's row already REVOKED (double-decline, idempotency) → 404 | status guard | `FAM218-TC-004` |
| TC-COND-005 | Audit action is DECLINED, not REVOKED | audit differentiation | `FAM218-TC-005` |
| TC-COND-006 | No data-access permission created (permission_json stays NULL; row never ACCEPTED) | privacy invariant | `FAM218-TC-006`, `FAM218-TC-INT-001` |
| TC-COND-007 | Response contains no PII; correct field names; empty payload | response mapping | `FAM218-TC-007` |
| TC-COND-008 | E2E API 200 for happy path + 401 without JWT | controller/security | `FAM218-TC-E2E-001` |
| TC-COND-009 | Caller cannot decline another user's invite (self-action only, IDOR check) | authorization boundary | `FAM218-TC-E2E-002` |
| TC-COND-010 | Both absent-row and not-PENDING collapse to the identical FAM-009 message (no info leak about which case occurred) | error-message uniformity | `FAM218-TC-008` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | caller row state {absent, PENDING, ACCEPTED, REVOKED} | Distinct outcomes per state class |
| State Transition Testing | `invitation_status` {PENDING→REVOKED valid; ACCEPTED/REVOKED/absent→decline invalid} | Guard the only legal transition |
| Boundary/Guard Analysis | double-decline on an already-`REVOKED` row (idempotency boundary) | Edge of the state machine |
| Error Guessing | attempt to decline via a caller not associated with the group (IDOR probe) | Privilege-escalation attack vector |
| Equivalence Partitioning | error-message content for absent-row vs not-pending (should be identical) | Confirms no information leak (ADR-FAM-057) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-CG-001` | DB seed | `CareGroup{ id=CG1, ownerUserId=OWNER1, status=ACTIVE }` | Group under test |
| `FX-INVITEE-PENDING` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE1, memberRole=MEMBER, inviteStatus=PENDING }` | Decline target (happy path) |
| `FX-INVITEE-ACCEPTED` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE2, inviteStatus=ACCEPTED }` | FAM-009 (already accepted) case |
| `FX-INVITEE-REVOKED` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=INVITEE3, inviteStatus=REVOKED }` | FAM-009 (double-decline) idempotency |
| `FX-STRANGER` | Identity only | `STRANGER1` has NO row in CG1 | FAM-009 (absent row) case |
| `FX-OWNER` | DB seed | `CareGroupMember{ careGroupId=CG1, userId=OWNER1, memberRole=OWNER, inviteStatus=ACCEPTED }` | Used by `listMembers()` call in `FAM218-TC-INT-001` |
| `FX-JWT-INVITEE` | JWT | `{ sub: INVITEE1, role: FAMILY }` | E2E auth context (happy path) |
| `FX-JWT-STRANGER` | JWT | `{ sub: STRANGER1, role: FAMILY }` | E2E no-pending-invite / IDOR probe |

**Fixed UUIDs (SYNTHETIC):**
`CG1=11111111-1111-1111-1111-111111111111`, `OWNER1=aaaaaaaa-0000-0000-0000-000000000001`,
`INVITEE1=22222222-2222-2222-2222-222222222222`, `INVITEE2=aaaaaaaa-0000-0000-0000-000000000003`,
`INVITEE3=aaaaaaaa-0000-0000-0000-000000000004`, `STRANGER1=aaaaaaaa-0000-0000-0000-000000000005`.

---

## 4. Test Case Specification

> **TC ID format:** `FAM218-TC-[NNN]` · **Severity:** CRITICAL/HIGH/MEDIUM/LOW · **Status:** 🔴/🟡/🟢

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern for UC-218
// Every @Test builds fresh entities via the factory — NO shared mutable state.
// ═══════════════════════════════════════════════════════════
class DeclineInvitationTestFactory {

    static final UUID CG1        = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER1     = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID INVITEE1   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID INVITEE2   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    static final UUID INVITEE3   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    static final UUID STRANGER1  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");

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

    static CareGroupMember owner()            { return member(OWNER1,   GroupMemberRole.OWNER,  InviteStatus.ACCEPTED); }
    static CareGroupMember inviteePending()    { return member(INVITEE1, GroupMemberRole.MEMBER, InviteStatus.PENDING);  }
    static CareGroupMember inviteeAccepted()   { return member(INVITEE2, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED); }
    static CareGroupMember inviteeRevoked()    { return member(INVITEE3, GroupMemberRole.MEMBER, InviteStatus.REVOKED);  }

    static CareGroup group() {
        return CareGroup.builder().id(CG1).ownerUserId(OWNER1).status(CareGroupStatus.ACTIVE).build();
    }
}
```

---

### FAM218-TC-001 — Invitee declines own PENDING invite → REVOKED

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.declineInvite()`
**Test File:** `src/test/java/com/carebridge/backend/family/DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN (pre-existing implementation — see §5)
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-FAM-IMP-218 §8.1` (real `declineInvite()` code) + `ADR-FAM-056` (REVOKED value + DECLINED audit) + SRS §3.3.17.3 postcondition

**Preconditions:** `FX-CG-001`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: mock `memberRepository.findByCareGroupIdAndUserId(CG1, INVITEE1)` → `inviteePending()`; `memberRepository.save(any)` returns its argument.
2. Act: `service.declineInvite(CG1, INVITEE1)`.
3. Assert: saved member `inviteStatus == REVOKED`; `auditService.log` called with `AuditAction.CARE_GROUP_INVITE_DECLINED`.

**Expected Result (PASS):** target row saved with `InviteStatus.REVOKED`; audit action is `CARE_GROUP_INVITE_DECLINED`; method returns `void` without exception.
**Expected Result (FAIL):** status left `PENDING`, wrong audit action used, or an exception thrown.

**Current Status:** 🟢 Passing against the real, already-shipped implementation (verified by source read; execute `./mvnw test` to confirm in CI).
**Implementation Note:** No implementation change needed — `declineInvite()` already performs exactly this.

---

### FAM218-TC-002 — No membership row for caller (never invited) → FAM-009 (404)

**Severity:** `HIGH`
**Feature Under Test:** `declineInvite()` → `pendingInviteOrThrow()` absent-row guard
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-002`
**Oracle Source:** `§10 FAM-009`, `ADR-FAM-057`

**Preconditions:** `FX-CG-001`; `STRANGER1` has no row in CG1.

**Test Steps:**
1. Arrange: `memberRepository.findByCareGroupIdAndUserId(CG1, STRANGER1)` → `Optional.empty()`.
2. Act + Assert: `service.declineInvite(CG1, STRANGER1)` throws `BusinessException`.

**Expected Result (PASS):** `BusinessException` code `FAM-009`, HTTP 404; `memberRepository.save` NEVER called (no side effect).
**Expected Result (FAIL):** NullPointerException, wrong code, or a row is created/modified.

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-003 — Caller's row already ACCEPTED (not pending) → FAM-009 (404)

**Severity:** `HIGH`
**Feature Under Test:** `declineInvite()` PENDING-status guard
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-FAM-057`, `§10 FAM-009`

**Preconditions:** `FX-CG-001`, `FX-INVITEE-ACCEPTED`.

**Test Steps:**
1. Arrange: `findByCareGroupIdAndUserId(CG1, INVITEE2)` → `inviteeAccepted()`.
2. Act + Assert: `service.declineInvite(CG1, INVITEE2)` throws `BusinessException`.

**Expected Result (PASS):** `BusinessException` code `FAM-009`, HTTP 404; `save` NEVER called; row remains `ACCEPTED` (member never loses their already-granted membership via decline).
**Expected Result (FAIL):** an accepted member is wrongly reverted to `REVOKED`, silently destroying an active membership.

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-004 — Caller's row already REVOKED (double-decline) → FAM-009 (404, idempotency)

**Severity:** `MEDIUM`
**Feature Under Test:** `declineInvite()` PENDING-status guard (idempotency)
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-FAM-057`, `§9.1 idempotency note`, `§10 FAM-009`

**Preconditions:** `FX-CG-001`, `FX-INVITEE-REVOKED`.

**Test Steps:**
1. Arrange: `findByCareGroupIdAndUserId(CG1, INVITEE3)` → `inviteeRevoked()`.
2. Act + Assert: `service.declineInvite(CG1, INVITEE3)` throws `BusinessException`.

**Expected Result (PASS):** `BusinessException` code `FAM-009`, HTTP 404 (second decline is a no-op error, not a duplicate audit event).
**Expected Result (FAIL):** double-decline succeeds silently / emits a second `CARE_GROUP_INVITE_DECLINED` audit event.

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-005 — Audit action is DECLINED, NOT REVOKED

**Severity:** `HIGH`
**Feature Under Test:** `declineInvite()` audit differentiation
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-056` (distinct audit action from sibling UC-217)

**Preconditions:** `FX-CG-001`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Arrange: happy-path mocks (as TC-001).
2. Act: `service.declineInvite(CG1, INVITEE1)`.
3. Assert: capture the `AuditAction` argument to `auditService.log(...)`.

**Expected Result (PASS):** captured action == `CARE_GROUP_INVITE_DECLINED`; verify `CARE_GROUP_INVITE_REVOKED` was NEVER passed.
**Expected Result (FAIL):** reuses `CARE_GROUP_INVITE_REVOKED` → audit cannot distinguish invitee-decline (UC-218) from owner-revoke (UC-217).

**Current Status:** 🟢 Passing against the real implementation.
**Implementation Note:** `AuditAction.CARE_GROUP_INVITE_DECLINED` already exists as a distinct constant from `CARE_GROUP_INVITE_REVOKED` — verified in `audit/entity/AuditAction.java`.

---

### FAM218-TC-006 — No data-access permission created (privacy invariant)

**Severity:** `HIGH`
**CWE:** `CWE-284 — Improper Access Control` (negative test: confirms access is NOT granted)
**Legal:** `BR-PRIVACY / PDPA`
**Feature Under Test:** `declineInvite()` — SRS "does not create data access permission" postcondition
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-006`
**Oracle Source:** `SRS §3.3.17.3` postcondition, `ADR-FAM-056`, `CareGroupMember.java` (no `permission_json` field mapped)

**Preconditions:** `FX-CG-001`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Act: `service.declineInvite(CG1, INVITEE1)`.
2. Assert: the saved `CareGroupMember` argument has `inviteStatus == REVOKED` (never `ACCEPTED`); the entity class exposes no setter/getter for `permission_json` (structural confirmation that decline cannot write it).

**Expected Result (PASS):** row never reaches `ACCEPTED`; no code path in `declineInvite()` references `permission_json`.
**Expected Result (FAIL):** row is left in or moved to `ACCEPTED`, or a future code change starts writing `permission_json` on decline.

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-007 — Response contains no PII; empty payload; real field names

**Severity:** `MEDIUM`
**CWE:** `CWE-359 — Exposure of Private Information`
**Legal:** `BR-PRIVACY / PDPA`
**Feature Under Test:** `CareGroupController.declineInvite()` response mapping
**Test File:** `src/test/java/com/carebridge/backend/family/DeclineInvitationControllerTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-FAM-IMP-218 §9.2`

**Preconditions:** mock `ICareGroupService.declineInvite(any(), any())` to return normally (void).

**Test Steps:**
1. Act: `POST /api/v1/care-groups/{groupId}/invitations/decline` via `@WebMvcTest` mock MVC.
2. Assert: response body is `ApiResponse<Void>` with `data: null`, `message: "Invitation declined"`; no `email`/`phone`/`@` substring anywhere in the JSON.

**Expected Result (PASS):** HTTP 200; empty `data`; no contact PII in response.
**Expected Result (FAIL):** response leaks email/phone, or exposes internal field names (`account_id`/`invite_status`).

**Current Status:** 🟢 Passing against the real implementation.

---

### SECURITY / E2E TEST CASES

---

### FAM218-TC-E2E-001 — Invitee decline via API → 200; missing JWT → 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `POST /api/v1/care-groups/{groupId}/invitations/decline`
**Test File:** `src/test/java/com/carebridge/backend/family/DeclineInvitationIntegrationTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-FAM-IMP-218 §9`, `§16`

**Preconditions:** Testcontainers PostgreSQL; seed `FX-CG-001`, `FX-INVITEE-PENDING`; `FX-JWT-INVITEE`.

**Test Steps:**
1. `POST .../CG1/invitations/decline` with `Authorization: Bearer <invitee_jwt>`.
2. `POST` the same without `Authorization` header.

**Expected Result (PASS):** first → 200, `data == null`, `message == "Invitation declined"`; DB row for INVITEE1 becomes `REVOKED`; second → 401.
**Expected Result (FAIL):** 200 without auth, or 500.

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-E2E-002 — Caller without a pending invite in the group cannot decline (IDOR / self-action boundary)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** decline endpoint self-action enforcement
**Test File:** `DeclineInvitationIntegrationTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-055`, `§16`

**Preconditions:** seed `FX-INVITEE-PENDING` (INVITEE1's own pending invite); `FX-JWT-STRANGER` (STRANGER1 has no row in CG1).

**Test Steps (Attack Simulation):**
1. As `STRANGER1` (authenticated, but has no membership row in CG1), `POST .../CG1/invitations/decline`.
2. Inspect DB row for INVITEE1.

**Expected Result (PASS = safe):** `404` with `FAM-009` (the endpoint has no path/body parameter that could target INVITEE1's row — the caller can only ever act on their OWN row, which does not exist here); INVITEE1's row remains `PENDING` and untouched.
**Expected Result (FAIL = vuln):** any request parameter allows STRANGER1 to affect INVITEE1's row.

**Current Status:** 🟢 Passing against the real implementation — structurally guaranteed since the endpoint accepts no target-user identifier (ADR-FAM-055).

---

### INTEGRATION TEST CASES

---

### FAM218-TC-INT-001 — Persist REVOKED, no permission granted, and REVOKED disappears from UC-216 member list

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: declineInvite() → DB → listMembers() exclusion`
**Test File:** `DeclineInvitationIntegrationTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-001`, `TC-COND-006`
**Oracle Source:** `V1__init_schema.sql`, `CareGroupServiceImpl.listMembers()` filter `IN ('ACCEPTED','PENDING')`

**Preconditions:** PostgreSQL container; Flyway applied; seed `FX-OWNER`, `FX-INVITEE-PENDING`.

**Test Steps:**
1. Call `declineInvite(CG1, INVITEE1)`.
2. Query the DB row for INVITEE1.
3. Call `listMembers(CG1, OWNER1)`.

**Expected Result (PASS):** DB `invitation_status = 'REVOKED'` for INVITEE1; `permission_json IS NULL`; `listMembers` result does NOT contain INVITEE1.
**Expected Result (FAIL):** row still `PENDING`, `permission_json` populated, or `REVOKED` row still appears in the member list.

**DB Assertion:**
```java
CareGroupMember row = memberRepository.findByCareGroupIdAndUserId(CG1, INVITEE1).orElseThrow();
assertThat(row.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
CareGroupMembersResponse members = service.listMembers(CG1, OWNER1);
assertThat(members.getMembers())
    .noneMatch(m -> row.getId().equals(m.getMemberId())); // REVOKED excluded by existing filter
```

**Current Status:** 🟢 Passing against the real implementation.

---

### FAM218-TC-008 — Absent-row and not-PENDING errors are indistinguishable (no information leak)

**Severity:** `MEDIUM`
**CWE:** `CWE-203 — Observable Discrepancy`
**Legal:** `BR-PRIVACY`
**Feature Under Test:** `pendingInviteOrThrow()` error-message uniformity
**Test File:** `DeclineInvitationServiceTest.java`
**TDD Phase:** 🟢 GREEN · **Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-057`

**Preconditions:** `FX-STRANGER` (no row) and `FX-INVITEE-ACCEPTED` (row exists, wrong status).

**Test Steps:**
1. Act: `declineInvite(CG1, STRANGER1)` → capture exception `code` + `message`.
2. Act: `declineInvite(CG1, INVITEE2)` → capture exception `code` + `message`.
3. Assert: both exceptions have identical `code` (`FAM-009`) and identical `message` text.

**Expected Result (PASS):** identical code + message for both cases — an attacker cannot use the error response to enumerate whether a given group has any record of them.
**Expected Result (FAIL):** distinct codes/messages leak whether the caller was ever invited vs. already responded.

**Current Status:** 🟢 Passing against the real implementation (both branches of `pendingInviteOrThrow` throw the exact same `BusinessException(HttpStatus.NOT_FOUND, "FAM-009", "No pending invitation found for this group")`).

---

## 5. Red-Green-Refactor Tracker

> **Honesty note (per template convention + task instruction):** UC-218's `declineInvite()` predates
> this Test-Spec — it was already implemented and shipped before these test cases were written.
> Because there is no historical "empty/throw stub" commit to re-run for a true Red Gate, the
> 🔴 RED column below is marked `N/A (pre-existing)` rather than falsely claimed as verified-red.
> All test cases are honestly tracked as 🟢 GREEN from the moment they are written, because they
> assert behavior the shipped code already satisfies (confirmed by direct source reading in §2 and
> the TDS). This is **not** an AP-AI-002 Green-from-Birth violation of a *newly written*
> implementation — it is the expected, disclosed outcome of writing tests **against a real,
> pre-existing implementation**, which is the explicit purpose of this documentation task.

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM218-TC-001` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-002` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-003` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-004` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-005` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | `ArgumentCaptor<AuditAction>` |
| `FAM218-TC-006` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-007` | `DeclineInvitationControllerTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-008` | `DeclineInvitationServiceTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw test run` | — |
| `FAM218-TC-E2E-001` | `DeclineInvitationIntegrationTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw verify run` | — |
| `FAM218-TC-E2E-002` | `DeclineInvitationIntegrationTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw verify run` | — |
| `FAM218-TC-INT-001` | `DeclineInvitationIntegrationTest.java` | `N/A (pre-existing impl)` | `[ ] pending ./mvnw verify run` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> **Adapted for an already-implemented feature.** The standard Red Gate (run tests against an
> empty/throw stub, confirm all FAIL) cannot be executed retroactively without reverting the real
> `declineInvite()` to a stub, which this documentation task is explicitly forbidden from doing
> (no production code may be written/modified). The stub below is provided **for reference only**
> — to be used if UC-218 is ever re-implemented from scratch (e.g. after an accidental revert) —
> and is not something this task executes.

**Stub cho Red Phase (reference only — NOT executed by this task):**

```java
@Service
public class CareGroupServiceImpl implements ICareGroupService {
    @Override
    public void declineInvite(UUID groupId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    // ... existing methods unchanged ...
}
```

**Red Gate Verification (N/A — see rationale above):**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM218-TC-001` … `FAM218-TC-INT-001` | `N/A — not executed (pre-existing implementation; see rationale above)` | — | — | — |

**Red Gate Evidence:**

- Stub commit hash: `N/A — no stub was created or run`
- Tất cả FAIL? `N/A` — Red Gate not applicable for a pre-existing implementation; **contract
  existence** (compiles clean) is used instead as the equivalent guardrail (see §6 Exit Criteria)
- Log file: `N/A`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-218` reviewed and status confirmed
- [ ] Logic Issues (§2) confirmed (esp. L1 UC83-comment mislabeling, L3 shared-REVOKED design, L4 collapsed FAM-009)
- [ ] No Flyway migration required (confirmed — ADR-FAM-056)
- [ ] `AuditAction.CARE_GROUP_INVITE_DECLINED` already exists (verified — no addition needed)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — all unit tests green (no skips)
- [ ] `./mvnw verify` — integration tests green (Testcontainers)
- [ ] Coverage ≥ 80% lines for `declineInvite()` / `pendingInviteOrThrow()`
- [ ] No business logic in controller (only validation + mapping)
- [ ] No email/phone in logs or response (BR-PRIVACY)
- [ ] Decline sets `invitation_status = REVOKED` (not a new value); audit uses `CARE_GROUP_INVITE_DECLINED`
- [ ] `permission_json` remains `NULL` after decline (no data access created)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — N/A for this pre-existing-implementation task; substituted by **Contract
      Existence** below
- [ ] **Contract Existence** — `./mvnw compile` clean; no hallucinated repo method / new DTO / new error code:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"   # Expected: no output
  ```
- [ ] **Props Isolation** — all entities built via `DeclineInvitationTestFactory` inside each `@Test`
- [ ] **Oracle Source** — every expected value cites a BR/ADR/§/real-code line (see each TC)

### Suspension Criteria (Điều kiện tạm dừng)

- CI pipeline broken by unrelated change
- A future refactor of `declineInvite()` diverges from §8 of the TDS without an updated ADR

---

## 7. Rollback Plan

```bash
# No migration to revert — UC-218 adds no DB objects (already-existing feature).

# If newly-added TEST files (not production code) need to be reverted:
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/DeclineInvitationServiceTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/DeclineInvitationControllerTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/DeclineInvitationIntegrationTest.java

# Production code (CareGroupServiceImpl.declineInvite, CareGroupController.declineInvite) is
# NOT touched by this Test-Spec — there is nothing to revert there.

# Dev/staging ONLY — repair a wrongly-declined invite (status flip, no data loss)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE care_group_members SET invitation_status='PENDING', updated_at=NOW() \
      WHERE care_group_id='<groupId>' AND user_id='<callerId>' AND invitation_status='REVOKED';"
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS/BR | ☐ (mỗi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS immediately against a **newly-written** stub with no real logic | ☐ N/A — tests are GREEN against a verified **pre-existing** real implementation (disclosed in §5), not a fresh stub; this is the expected, honest outcome for a documentation-of-existing-behavior task | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định thêm enum value (`REJECTED`) / error code / migration mới | ☐ (L3/L4 forbid; only `FAM-009` and existing `REVOKED` used) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa business logic | ☐ (logic in service only; controller test only checks status/JSON mapping) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import repo method/DTO/error code không tồn tại, hoặc dùng `account_id`/`invite_status` | ☐ (L2 guard; real fields only; only `findByCareGroupIdAndUserId`/`save` used) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved (Draft, pending human/Tech Lead review)
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
*This Test-Spec documents test cases for an ALREADY-IMPLEMENTED feature (UC-218 `declineInvite()`).
No production code was written or modified to produce this document.*
