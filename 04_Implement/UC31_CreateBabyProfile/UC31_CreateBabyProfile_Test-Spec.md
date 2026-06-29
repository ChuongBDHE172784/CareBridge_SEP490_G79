# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-31 Create Baby Profile

**Document ID:** `CB-BABY-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC31_CreateBabyProfile/UC31_CreateBabyProfile_TDS.md` (CB-BABY-IMP-001)
- SRS: §3.3.1.8

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-27 | AI Agent — Amelia (Dev Agent) | RED Gate verified, GREEN Gate PASS (45/45 unit tests) |
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-31 |

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
| **Feature / Gap ID** | `UC-31` |
| **Module** | `CreateBabyProfile — baby` |
| **Spec gốc** | `CB-BABY-IMP-001` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `Sensitive-PII` |
| **Upstream Dependencies** | `auth, accounts` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "birth weight or length" — không rõ range | ADR-BABY-002: weight 0.5–8.0 kg, length 20–60 cm | Test encode range validation |
| L2 | SRS: không giới hạn số profiles | ADR-BABY-001: nhiều profiles allowed | No uniqueness constraint |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid baby profile creation | `BabyService.createBabyProfile()` | `BABY-TC-001` |
| TC-COND-002 | Birth weight out of range | `validateBirthMeasurements()` | `BABY-TC-002` |
| TC-COND-003 | Birth length out of range | `validateBirthMeasurements()` | `BABY-TC-003` |
| TC-COND-004 | Missing nickname | DTO validation | `BABY-TC-004` |
| TC-COND-005 | Future birthDate | `@PastOrPresent` | `BABY-TC-005` |
| TC-COND-006 | EXPERT role blocked | `@PreAuthorize` | `BABY-TC-006` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path |
| `FX-002` | Input | `{nickname: "Bean", birthDate: "2026-01-15", gender: "MALE", birthWeightKg: 3.2}` | Happy path |
| `FX-003` | Input | `{birthWeightKg: 12.0}` | Out of range |
| `FX-004` | Input | `{birthDate: "2030-01-01"}` | Future date |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class BabyProfileTestFactory {
    static BabyProfile makeProfile() {
        BabyProfile p = new BabyProfile();
        p.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        p.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        p.setNickname("Bean");
        p.setBirthDate(LocalDate.of(2026, 1, 15));
        p.setGender(Gender.MALE);
        p.setBirthWeightKg(new BigDecimal("3.2"));
        p.setStatus(BabyProfileStatus.ACTIVE);
        return p;
    }

    static CreateBabyProfileRequest makeRequest() {
        CreateBabyProfileRequest r = new CreateBabyProfileRequest();
        r.setNickname("Bean");
        r.setBirthDate(LocalDate.of(2026, 1, 15));
        r.setGender(Gender.MALE);
        r.setBirthWeightKg(new BigDecimal("3.2"));
        return r;
    }
}
```

---

### BABY-TC-001 — Happy path

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyService.createBabyProfile()`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC-31 Normal Flow`

**Preconditions:** Mother authenticated FX-001

**Test Steps:**
1. Mock `babyRepository.save()` → saved profile
2. Call `babyService.createBabyProfile(FX-002, ACC-001)`

**Expected Result (PASS):**
- Returns response with `status = "ACTIVE"`, `nickname = "Bean"`
- `auditService.emit(BabyProfileCreated)` called once

**Current Status:** 🟢 Passing

---

### BABY-TC-002 — Birth weight out of range (too heavy) → 400

**Severity:** `HIGH`
**Feature Under Test:** `BabyService.validateBirthMeasurements()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-BABY-002`

**Test Steps:**
1. Call `createBabyProfile()` with `birthWeightKg = 12.0`

**Expected Result (PASS):**
- Throws `InvalidMeasurementException` with code `BABY-003`
- `babyRepository.save()` NOT called

**Current Status:** 🔴 Not written

---

### BABY-TC-003 — Birth length out of range → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyService.validateBirthMeasurements()`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Call with `birthLengthCm = 100.0`

**Expected Result:** throws BABY-003

**Current Status:** 🔴 Not written

---

### BABY-TC-004 — Missing nickname → 400

**Severity:** `HIGH`
**Feature Under Test:** `@NotBlank` DTO validation
**TDD Phase:** 🔴 RED

**Test Steps:** POST with `{"nickname": ""}` via @WebMvcTest

**Expected Result:** 400

**Current Status:** 🔴 Not written

---

### BABY-TC-005 — Future birthDate → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@PastOrPresent` DTO validation
**TDD Phase:** 🔴 RED

**Test Steps:** POST with FX-004 (birthDate 2030-01-01)

**Expected Result:** 400

**Current Status:** 🔴 Not written

---

### BABY-TC-006 — EXPERT role → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**TDD Phase:** 🔴 RED

**Expected Result:** 403

**Current Status:** 🔴 Not written

---

### BABY-TC-INT-001 — Full DB persistence

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. POST with FX-001 JWT + FX-002 body
2. Assert 201
3. `SELECT COUNT(*) FROM baby_profiles WHERE account_id='ACC-001'` = 1
4. `status = 'ACTIVE'`

```java
BabyProfile saved = babyRepo.findByAccountId(ACC_001).get(0);
assertThat(saved.getNickname()).isEqualTo("Bean");
assertThat(saved.getStatus()).isEqualTo(BabyProfileStatus.ACTIVE);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `BABY-TC-001` | `BabyServiceImplTest.java` | `Passed` | — |
| `BABY-TC-002` | `[ ]` | `___` | — |
| `BABY-TC-003` | `[ ]` | `___` | — |
| `BABY-TC-004` | `[ ]` | `___` | — |
| `BABY-TC-005` | `[ ]` | `___` | — |
| `BABY-TC-006` | `[ ]` | `___` | — |
| `BABY-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Entry / Exit Criteria

### Exit Criteria
- [ ] `./mvnw test` green
- [ ] Coverage ≥ 80% BabyService
- [ ] Birth measurements validated (weight 0.5–8.0 kg, length 20–60 cm)
- [ ] No medical interpretation in response
- [ ] Red Gate confirmed

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/baby/
git checkout -- src/main/resources/db/migration/V21__create_baby_profiles.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-001 | ☐ All TCs reference ADR/BR | G-0 |
| AP-AI-002 | ☐ Tests FAIL with stub | G-2 ★ |
| AP-AI-004 | ☐ No business logic in controller | G-4 |
