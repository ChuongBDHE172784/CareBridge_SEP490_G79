# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-192 View Baby Profile

**Document ID:** `CB-BABY-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Partially Implemented — 2026-07-10 (targeted baby/carejourney backend tests PASS; full regression blocked by non-baby Family/Exercise/Auth/Triage failures)`
**Author:** `AI Agent`
**Classification:** `Internal â€” Confidential`

**References:**
- TDS: `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` (CB-BABY-IMP-002)
- SRS: Â§3.3.12.1

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-06-27 | AI Agent â€” Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khá»Ÿi táº¡o TDD spec cho UC-192 |

---

## Má»¤C Lá»¤C

1. [ThÃ´ng tin Module](#1-thÃ´ng-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. ThÃ´ng tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-192` |
| **Module** | `ViewBabyProfile â€” baby` |
| **Priority** | ðŸŸ  P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c | Thá»±c táº¿ | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays basic information" â€” khÃ´ng rÃµ who can see | ADR-BABY-003: owner + care group members | Test both paths |
| L2 | SRS: khÃ´ng rÃµ archived profile handling | BR-BABY-011: archived â†’ 200 vá»›i status ARCHIVED (not 404) | Test archived â†’ 200 |

---

## 3. Test Design

### TDS-03 â€” Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner views profile | `BabyService.getBabyProfile()` | `BABY-VIEW-TC-001` |
| TC-COND-002 | Care group member views profile | `BabyAccessPolicy.canView()` | `BABY-VIEW-TC-002` |
| TC-COND-003 | Unrelated user â†’ 403 | `BabyAccessPolicy.canView()` | `BABY-VIEW-TC-003` |
| TC-COND-004 | Non-existent profile â†’ 404 | repo lookup | `BABY-VIEW-TC-004` |
| TC-COND-005 | Archived profile â†’ 200 | status in response | `BABY-VIEW-TC-005` |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class BabyViewTestFactory {
    static BabyProfile makeActiveProfile() {
        BabyProfile p = new BabyProfile();
        p.setId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        p.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        p.setNickname("Bean");
        p.setBirthDate(LocalDate.of(2026, 1, 15));
        p.setGender(Gender.MALE);
        p.setStatus(BabyProfileStatus.ACTIVE);
        return p;
    }

    static BabyProfile makeArchivedProfile() {
        BabyProfile p = makeActiveProfile();
        p.setStatus(BabyProfileStatus.ARCHIVED);
        return p;
    }
}
```

---

### BABY-VIEW-TC-001 â€” Owner views own profile â†’ 200

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyService.getBabyProfile()`
**TDD Phase:** ðŸŸ¢ GREEN

**Test Steps:**
1. Mock `babyRepository.findById(BABY-001)` â†’ active profile (accountId=ACC-001)
2. Mock `babyAccessPolicy.canView(BABY-001, ACC-001)` â†’ true
3. Call `getBabyProfile(BABY-001, ACC-001)`

**Expected Result:** Response with nickname, birthDate, gender, status=ACTIVE

**Current Status:** ðŸŸ¢ Passing

---

### BABY-VIEW-TC-002 â€” Care group member views profile â†’ 200

**Severity:** `HIGH`
**Feature Under Test:** `BabyAccessPolicy.canView()` â€” group member path
**TDD Phase:** ðŸŸ¢ GREEN
**Oracle Source:** `ADR-BABY-003`

**Test Steps:**
1. ACC-002 is ACCEPTED member of care group linked to BABY-001's account
2. Call `getBabyProfile(BABY-001, ACC-002)`
3. Mock `careGroupMemberRepo.existsByGroupIdAndAccountIdAndStatus(groupId, ACC-002, ACCEPTED)` â†’ true

**Expected Result:** 200

**Current Status:** ðŸŸ¢ Passing

---

### BABY-VIEW-TC-003 â€” Unrelated user â†’ 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canView()`
**TDD Phase:** ðŸŸ¢ GREEN

**Expected Result:** throws ForbiddenException (BABY-002)

**Current Status:** ðŸŸ¢ Passing

---

### BABY-VIEW-TC-004 â€” Non-existent profile â†’ 404

**Severity:** `MEDIUM`
**TDD Phase:** ðŸŸ¢ GREEN

**Expected Result:** 404, error BABY-004

**Current Status:** ðŸŸ¢ Passing

---

### BABY-VIEW-TC-005 â€” Archived profile â†’ 200 (not 404)

**Severity:** `HIGH`
**Feature Under Test:** archived profile handling
**TDD Phase:** ðŸŸ¢ GREEN
**Oracle Source:** `BR-BABY-011`

**Test Steps:**
1. Profile has status=ARCHIVED, owned by ACC-001
2. Call `getBabyProfile(BABY-001, ACC-001)`

**Expected Result:**
- Response 200 with `status = "ARCHIVED"` (NOT 404)
- Profile data still returned

**Current Status:** ðŸŸ¢ Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | ðŸ”´ RED | ðŸŸ¢ GREEN | ðŸ”µ REFACTOR |
|-------|--------|----------|------------|
| `BABY-VIEW-TC-001` | `[ ]` | `___` | â€” |
| `BABY-VIEW-TC-002` | `[ ]` | `___` | â€” |
| `BABY-VIEW-TC-003` | `[ ]` | `___` | â€” |
| `BABY-VIEW-TC-005` | `[ ]` | `___` | â€” |

---

## 6. Exit Criteria

- [ ] BabyAccessPolicy tested (owner path + care group member path)
- [ ] Archived profiles return 200, not 404
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/baby/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | â˜ | G-2 â˜… |
| AP-AI-003 | â˜ BabyAccessPolicy has ADR backing | G-1 |
