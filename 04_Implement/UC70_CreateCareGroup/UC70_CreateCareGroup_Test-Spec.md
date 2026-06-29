# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-70 Create Care Group

**Document ID:** `CB-FAM-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC70_CreateCareGroup/UC70_CreateCareGroup_TDS.md` (CB-FAM-IMP-001)
- SRS: §3.3.1.47

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-27 | AI Agent — Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-70 |

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
| **Feature / Gap ID** | `UC-70` |
| **Module** | `CreateCareGroup — family` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "creates a care group" — không rõ limit | ADR-FAM-001: max 5 ACTIVE groups | Test encode limit check → 409 |
| L2 | SRS: không rõ creator role | ADR-FAM-001: creator auto-added as OWNER | Test encode member creation |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid group creation | `CareGroupService.createCareGroup()` | `FAM-TC-001` |
| TC-COND-002 | Creator added as OWNER | `MemberRepository.save()` | `FAM-TC-002` |
| TC-COND-003 | Max 5 groups reached | `validateMaxActiveGroups()` | `FAM-TC-003` |
| TC-COND-004 | Empty group name | DTO validation | `FAM-TC-004` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path |
| `FX-002` | Input | `{groupName: "My Team", description: "Support"}` | Happy path |
| `FX-003` | DB | 5 existing ACTIVE groups for ACC-001 | Quota test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class CareGroupTestFactory {
    static CareGroup makeGroup() {
        CareGroup g = new CareGroup();
        g.setId(UUID.fromString("00000000-0000-0000-0000-000000000040"));
        g.setOwnerAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        g.setGroupName("My Team");
        g.setStatus(CareGroupStatus.ACTIVE);
        return g;
    }

    static CareGroupMember makeOwnerMember(UUID groupId, UUID accountId) {
        CareGroupMember m = new CareGroupMember();
        m.setGroupId(groupId);
        m.setAccountId(accountId);
        m.setMemberRole(GroupMemberRole.OWNER);
        m.setInviteStatus(InviteStatus.ACCEPTED);
        return m;
    }
}
```

---

### FAM-TC-001 — Happy path

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupService.createCareGroup()`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC-70 Normal Flow`

**Test Steps:**
1. Mock `careGroupRepository.countByOwnerAndStatus()` → 0
2. Mock `careGroupRepository.save()` → saved group
3. Mock `memberRepository.save()` → OWNER member

**Expected Result (PASS):**
- Returns 201 response
- `memberRepository.save()` called with `memberRole = OWNER`, `inviteStatus = ACCEPTED`
- `auditService.emit(CareGroupCreated)` called

**Current Status:** 🟢 Passing

---

### FAM-TC-002 — Creator is OWNER in DB (integration verification)

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupService` → `care_group_members`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FAM-001`

**Test Steps:**
1. Call `createCareGroup(FX-002, ACC-001)` with real DB
2. Assert `care_group_members` has 1 row with `member_role='OWNER'`, `invite_status='ACCEPTED'`

```java
List<CareGroupMember> members = memberRepo.findByGroupId(savedGroupId);
assertThat(members).hasSize(1);
assertThat(members.get(0).getMemberRole()).isEqualTo(GroupMemberRole.OWNER);
```

**Current Status:** 🔴 Not written

---

### FAM-TC-003 — Max 5 groups → 409

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupService.validateMaxActiveGroups()`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `ADR-FAM-001`

**Test Steps:**
1. Seed FX-003 (5 ACTIVE groups for ACC-001)
2. POST `/api/v1/care-groups`

**Expected Result:** 409, error FAM-002

**Current Status:** 🟢 Passing

---

### FAM-TC-004 — Empty group name → 400

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** 400

**Current Status:** 🔴 Not written

---

### FAM-TC-INT-001 — Full DB flow

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. POST with FX-001 JWT + FX-002 body
2. Assert 201
3. Assert care_groups count = 1, care_group_members count = 1 (OWNER)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `FAM-TC-001` | `[ ]` | `___` | — |
| `FAM-TC-002` | `[ ]` | `___` | — |
| `FAM-TC-003` | `[ ]` | `___` | — |
| `FAM-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] Creator automatically added as OWNER member
- [ ] Max 5 active groups enforced
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/family/
git checkout -- src/main/resources/db/migration/V24__create_care_groups.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ Tests FAIL with stub | G-2 ★ |
| AP-AI-004 | ☐ No business logic in controller | G-4 |
