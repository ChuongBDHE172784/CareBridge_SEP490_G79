# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-228 View Vaccination Schedule

**Document ID:** `CB-VAC-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC228_ViewVaccinationSchedule/UC228_ViewVaccinationSchedule_TDS.md` (CB-VAC-IMP-001)
- SRS: §3.3.19.1

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-228 |

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
| **Feature / Gap ID** | `UC-228` |
| **Module** | `ViewVaccinationSchedule — vaccination` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "doses by status and expected time" — không rõ status compute logic | ADR-VAC-001: derived from birthDate + offsetDays, OVERDUE if expected < today | Test OVERDUE compute |
| L2 | SRS: không rõ who can access | ADR-VAC-001: owner + care group members (re-use BabyAccessPolicy) | Test non-owner → 403 |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | SCHEDULED dose computed correctly | `VaccinationService.deriveSchedule()` | `VAC-TC-001` |
| TC-COND-002 | OVERDUE dose computed correctly | `computeStatus()` | `VAC-TC-002` |
| TC-COND-003 | COMPLETED dose from record | merge logic | `VAC-TC-003` |
| TC-COND-004 | Non-owner → 403 | `BabyAccessPolicy.canView()` | `VAC-TC-004` |
| TC-COND-005 | Baby not found → 404 | repo lookup | `VAC-TC-005` |
| TC-COND-006 | No medical recommendation in response | Response mapping | `VAC-TC-006` |

### TDS-05 — Test Data

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-001` | DB | `baby: {id: BABY-001, accountId: ACC-001, birthDate: 2026-01-01}` | Baby profile |
| `FX-002` | DB | `reference: {vaccineName: "BCG", doseNumber: 1, offsetDays: 0}` | BCG at birth |
| `FX-003` | DB | `reference: {vaccineName: "Hepatitis B", doseNumber: 2, offsetDays: 30}` | HepB at 1 month |
| `FX-004` | DB | `vaccinationRecord: {babyId: BABY-001, refId: FX-002, status: COMPLETED, administeredDate: 2026-01-02}` | Completed BCG |
| `FX-005` | Config | `today = 2026-06-26` | Current date for OVERDUE compute |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class VaccinationTestFactory {
    static final LocalDate BABY_BIRTH_DATE = LocalDate.of(2026, 1, 1);
    static final LocalDate TODAY = LocalDate.of(2026, 6, 26);

    static BabyProfile makeBaby() {
        BabyProfile b = new BabyProfile();
        b.setId(UUID.fromString("00000000-0000-0000-0000-000000000090"));
        b.setAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        b.setBirthDate(BABY_BIRTH_DATE);
        b.setNickname("Bean");
        return b;
    }

    static VaccinationReferenceSchedule makeBcgRef() {
        VaccinationReferenceSchedule r = new VaccinationReferenceSchedule();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000091"));
        r.setVaccineName("BCG");
        r.setDoseNumber(1);
        r.setOffsetDays(0);
        return r;
    }
}
```

---

### VAC-TC-001 — SCHEDULED dose computed correctly

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationService.deriveSchedule()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-VAC-001`

**Test Steps:**
1. Baby birthDate = 2026-01-01
2. Reference: HepB dose 2, offsetDays=30 → expectedDate = 2026-01-31
3. No vaccination record for this dose
4. today = 2026-06-26

**Expected Result:**
- HepB dose 2 shows `status = "OVERDUE"` (expectedDate 2026-01-31 < today 2026-06-26)
- `expectedDate = 2026-01-31`
- `administeredDate = null`

**Current Status:** 🔴 Not written

---

### VAC-TC-002 — OVERDUE status computed when expected < today

**Severity:** `CRITICAL`
**Feature Under Test:** `VaccinationService.computeStatus()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `BR-VAC-003`

**Test Steps:**
1. Baby birthDate = 2026-01-01
2. Reference: BCG offsetDays=0 → expected = 2026-01-01
3. No completed record for BCG
4. today = 2026-06-26 (much later)

**Expected Result:**
- BCG dose status = "OVERDUE"
- (not "SCHEDULED", because expected date has passed)

```java
// Compute status logic
VaccinationDoseDto bcgDose = doses.stream()
    .filter(d -> d.getVaccineName().equals("BCG"))
    .findFirst().orElseThrow();
assertThat(bcgDose.getStatus()).isEqualTo("OVERDUE");
```

**Current Status:** 🔴 Not written

---

### VAC-TC-003 — COMPLETED dose from vaccination record

**Severity:** `CRITICAL`
**Feature Under Test:** merge logic in `deriveSchedule()`
**TDD Phase:** 🔴 RED
**Oracle Source:** `ADR-VAC-001`

**Test Steps:**
1. FX-004: BCG record with status=COMPLETED, administeredDate=2026-01-02
2. Call `getVaccinationSchedule(BABY-001, ACC-001)`

**Expected Result:**
- BCG dose shows `status = "COMPLETED"`, `administeredDate = 2026-01-02`

```java
VaccinationDoseDto bcg = doses.get(0);
assertThat(bcg.getStatus()).isEqualTo("COMPLETED");
assertThat(bcg.getAdministeredDate()).isEqualTo(LocalDate.of(2026, 1, 2));
```

**Current Status:** 🔴 Not written

---

### VAC-TC-004 — Non-owner → 403

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-VAC-001`
**TDD Phase:** 🔴 RED

**Expected Result:** ForbiddenException (VAC-002)

**Current Status:** 🔴 Not written

---

### VAC-TC-005 — Baby not found → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** NotFoundException (VAC-001)

**Current Status:** 🔴 Not written

---

### VAC-TC-006 — No vaccine recommendations in response

**Severity:** `HIGH`
**Oracle Source:** `BR-SAFETY-003`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("recommendation");
assertThat(json).doesNotContain("shouldTake");
assertThat(json).doesNotContain("medicalAdvice");
// Response only shows reference schedule — no AI recommendations
```

**Current Status:** 🔴 Not written

---

### VAC-TC-INT-001 — Full schedule derived from reference + records

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed: baby BABY-001, 2 reference vaccines (BCG at day 0, HepB at day 30)
2. Seed: 1 vaccination record (BCG COMPLETED)
3. Call GET `/api/v1/baby-profiles/BABY-001/vaccination-schedule`

**Expected Result:**
- BCG: COMPLETED
- HepB: OVERDUE (no record, expected date in past)

```java
VaccinationScheduleResponse resp = /* API call */;
assertThat(resp.getDoses()).hasSize(2);
assertThat(resp.getDoses().get(0).getStatus()).isEqualTo("COMPLETED");
assertThat(resp.getDoses().get(1).getStatus()).isEqualTo("OVERDUE");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-------|--------|----------|------------|
| `VAC-TC-001` | `[ ]` | `___` | — |
| `VAC-TC-002` | `[ ]` | `___` | — |
| `VAC-TC-003` | `[ ]` | `___` | — |
| `VAC-TC-004` | `[ ]` | `___` | — |
| `VAC-TC-006` | `[ ]` | `___` | — |
| `VAC-TC-INT-001` | `[ ]` | `___` | — |

---

## 6. Exit Criteria

- [ ] SCHEDULED/OVERDUE/COMPLETED/POSTPONED status computed correctly
- [ ] Status derived from reference schedule + birthDate — not hardcoded
- [ ] BabyAccessPolicy re-used (owner + care group members)
- [ ] Response has no vaccine recommendations beyond reference schedule
- [ ] Red Gate confirmed

---

## 7. Rollback

```bash
git checkout -- src/main/java/com/carebridge/backend/vaccination/
git checkout -- src/main/resources/db/migration/V27__create_vaccination_tables.sql
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-001 | ☐ Status compute logic traceable to ADR-VAC-001 | G-0 |
| AP-AI-002 | ☐ Tests FAIL with stub | G-2 ★ |
| AP-AI-003 | ☐ BabyAccessPolicy re-used — has ADR backing | G-1 |
