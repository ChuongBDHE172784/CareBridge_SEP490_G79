# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-216 View Care Group Members

**Document ID:** `CB-FAM-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md` (CB-FAM-IMP-002)
- SRS: §3.3.17.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-216 |

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
| **Feature / Gap ID** | `UC-216` |
| **Module** | `ViewCareGroupMembers — family` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays members, roles, permissions, invitation status" — không rõ who can view | ADR-FAM-002: ACCEPTED members only (not PENDING) | Test PENDING invitee → 403 |
| L2 | SRS: không rõ REVOKED member visibility | BR-FAM-011: REVOKED not in list | Test REVOKED not in results |
| L3 | SRS: không rõ PII exposure | BR-PRIVACY-002: displayName only, no email/phone | Test response has no email |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | ACCEPTED member lists group | `CareGroupService.listMembers()` | `CGM-TC-001` |
| TC-COND-002 | PENDING invitee blocked | `CareGroupAccessPolicy.isMember()` | `CGM-TC-002` |
| TC-COND-003 | REVOKED not in results | status filter | `CGM-TC-003` |
| TC-COND-004 | Non-member → 403 | access policy | `CGM-TC-004` |
| TC-COND-005 | Response has no email/phone | Response mapping | `CGM-TC-005` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB | care group GRP-001 with 3 members: ACC-001 (OWNER/ACCEPTED), ACC-002 (MEMBER/ACCEPTED), ACC-003 (MEMBER/PENDING) | Happy path |
| `FX-002` | DB | ACC-004 not in group | Non-member |
| `FX-003` | DB | ACC-005 as MEMBER/REVOKED in group | Revoked |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class CareGroupMemberTestFactory {
    static CareGroupMember makeOwner(UUID groupId, UUID accountId) {
        CareGroupMember m = new CareGroupMember();
        m.setGroupId(groupId);
        m.setAccountId(accountId);
        m.setMemberRole(GroupMemberRole.OWNER);
        m.setInviteStatus(InviteStatus.ACCEPTED);
        m.setJoinedAt(Instant.now());
        return m;
    }

    static CareGroupMember makePendingMember(UUID groupId, UUID accountId) {
        CareGroupMember m = new CareGroupMember();
        m.setGroupId(groupId);
        m.setAccountId(accountId);
        m.setMemberRole(GroupMemberRole.MEMBER);
        m.setInviteStatus(InviteStatus.PENDING);
        return m;
    }
}
```

---

### CGM-TC-001 — ACCEPTED member lists group → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupService.listMembers()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `UC-216 Normal Flow`

**Test Steps:**
1. ACC-001 is ACCEPTED OWNER of GRP-001
2. FX-001: GRP-001 has 2 ACCEPTED + 1 PENDING members
3. Call `listMembers(GRP-001, ACC-001)`

**Expected Result:**
- Returns `CareGroupMembersResponse` with `totalMembers = 3` (2 ACCEPTED + 1 PENDING displayed)
- Contains member with `memberRole = OWNER`
- Contains PENDING member in list (visible but marked PENDING)

**Current Status:** 🔴 Not written

---

### CGM-TC-002 — PENDING invitee cannot list members → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupAccessPolicy.isMember()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-FAM-002`

**Test Steps:**
1. ACC-003 is PENDING in GRP-001
2. Call `listMembers(GRP-001, ACC-003)`

**Expected Result:**
- Throws ForbiddenException (FAM-003)
- `isMember()` checks invite_status = 'ACCEPTED' specifically

```java
// Verify policy checks ACCEPTED status
verify(memberRepo).existsByGroupIdAndAccountIdAndInviteStatus(
    GRP_001, ACC_003, InviteStatus.ACCEPTED
);
// → false → throw ForbiddenException
```

**Current Status:** 🔴 Not written

---

### CGM-TC-003 — REVOKED member not in results

**Severity:** `HIGH`
**Feature Under Test:** status filter in `findByGroupIdAndStatusIn()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-FAM-011`

**Test Steps:**
1. GRP-001 has ACC-005 with invite_status=REVOKED
2. Call `listMembers(GRP-001, ACC-001)` (OWNER)

**Expected Result:**
- Response does NOT contain ACC-005
- `memberRepository.findByGroupIdAndInviteStatusIn(GRP-001, [ACCEPTED, PENDING])` called (not REVOKED)

**Current Status:** 🔴 Not written

---

### CGM-TC-004 — Non-member → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (FAM-003)

**Current Status:** 🔴 Not written

---

### CGM-TC-005 — Response has no email/phone

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupMemberDto` mapping
**Oracle Source:** `BR-PRIVACY-002`
**TDD Phase:** 🔴 RED

```java
CareGroupMembersResponse resp = service.listMembers(GRP_001, ACC_001);
String json = objectMapper.writeValueAsString(resp);
assertThat(json).doesNotContain("@");      // no email
assertThat(json).doesNotContain("phone");
assertThat(json).doesNotContain("email");
// Members only expose displayName
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `CGM-TC-001` | `[ ]` | `___` | — |
| `CGM-TC-002` | `[ ]` | `___` | — |
| `CGM-TC-003` | `[ ]` | `___` | — |
| `CGM-TC-004` | `[ ]` | `___` | — |
| `CGM-TC-005` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] CareGroupAccessPolicy checks ACCEPTED (not PENDING)
- [ ] REVOKED members filtered from response
- [ ] Response has no email/phone data
- [ ] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS care_group_members CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '040';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/family/
git checkout -- src/test/java/com/carebridge/backend/family/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ isMember() checks ACCEPTED status — has ADR backing | G-1 |
