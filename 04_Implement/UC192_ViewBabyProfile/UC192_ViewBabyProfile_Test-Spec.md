# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-192 View Baby Profile

**Document ID:** `CB-BABY-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` (CB-BABY-IMP-002)
- SRS: §3.3.12.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-192 |

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
| **Feature / Gap ID** | `UC-192` |
| **Module** | `ViewBabyProfile — baby` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays basic information" — không rõ who can see | ADR-BABY-003: owner + care group members | Test both paths |
| L2 | SRS: không rõ archived profile handling | BR-BABY-011: archived → 200 với status ARCHIVED (not 404) | Test archived → 200 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner views profile | `BabyService.getBabyProfile()` | `BABY-VIEW-TC-001` |
| TC-COND-002 | Care group member views profile | `BabyAccessPolicy.canView()` | `BABY-VIEW-TC-002` |
| TC-COND-003 | Unrelated user → 403 | `BabyAccessPolicy.canView()` | `BABY-VIEW-TC-003` |
| TC-COND-004 | Non-existent profile → 404 | repo lookup | `BABY-VIEW-TC-004` |
| TC-COND-005 | Archived profile → 200 | status in response | `BABY-VIEW-TC-005` |

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

### BABY-VIEW-TC-001 — Owner views own profile → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyService.getBabyProfile()`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock `babyRepository.findById(BABY-001)` → active profile (accountId=ACC-001)
2. Mock `babyAccessPolicy.canView(BABY-001, ACC-001)` → true
3. Call `getBabyProfile(BABY-001, ACC-001)`

**Expected Result:** Response with nickname, birthDate, gender, status=ACTIVE

**Current Status:** 🔴 Not written

---

### BABY-VIEW-TC-002 — Care group member views profile → 200

**Severity:** `HIGH`
**Feature Under Test:** `BabyAccessPolicy.canView()` — group member path
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-BABY-003`

**Test Steps:**
1. ACC-002 is ACCEPTED member of care group linked to BABY-001's account
2. Call `getBabyProfile(BABY-001, ACC-002)`
3. Mock `careGroupMemberRepo.existsByGroupIdAndAccountIdAndStatus(groupId, ACC-002, ACCEPTED)` → true

**Expected Result:** 200

**Current Status:** 🔴 Not written

---

### BABY-VIEW-TC-003 — Unrelated user → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canView()`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (BABY-002)

**Current Status:** 🔴 Not written

---

### BABY-VIEW-TC-004 — Non-existent profile → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** 404, error BABY-004

**Current Status:** 🔴 Not written

---

### BABY-VIEW-TC-005 — Archived profile → 200 (not 404)

**Severity:** `HIGH`
**Feature Under Test:** archived profile handling
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-BABY-011`

**Test Steps:**
1. Profile has status=ARCHIVED, owned by ACC-001
2. Call `getBabyProfile(BABY-001, ACC-001)`

**Expected Result:**
- Response 200 with `status = "ARCHIVED"` (NOT 404)
- Profile data still returned

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `BABY-VIEW-TC-001` | `[ ]` | `___` | — |
| `BABY-VIEW-TC-002` | `[ ]` | `___` | — |
| `BABY-VIEW-TC-003` | `[ ]` | `___` | — |
| `BABY-VIEW-TC-005` | `[ ]` | `___` | — |

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
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ BabyAccessPolicy has ADR backing | G-1 |
